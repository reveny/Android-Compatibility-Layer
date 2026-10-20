#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
tier="${2:-t0}"
SRC_DIR="$PROJ_DIR/tests/guestlib/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
binary="$buildDir/src/GuestLibHostCallTest"

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

contains() {
    grep -c -- "$2" "$1" 2>/dev/null | head -1
}

GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libhello.so -o "$work/libhello.so" "$SRC_DIR/hello.c" || exit 1

"$binary" --guest "$work/libhello.so" --tier "$tier" > "$work/guest.txt" 2> "$work/guest.log"
if [ $? -ne 0 ]; then
    cat "$work/guest.log" >&2
    echo "gate FAILED: the guest run aborted" >&2
    exit 1
fi

recompiled=$(sed -n 's/.*guest: \([0-9]*\) blocks recompiled.*/\1/p' "$work/guest.log" | head -1)
recompiled="${recompiled:-0}"

native=$(sed -n 's/.*, \([0-9]*\) block entries executed natively.*/\1/p' "$work/guest.log" | head -1)
native="${native:-0}"

if [ "$tier" = "t1" ]; then
    if [ "$recompiled" -eq 0 ]; then
        echo "gate FAILED: --tier t1 was accepted but nothing was recompiled - the tier did nothing" >&2
        exit 1
    fi
    if [ "$native" -eq 0 ]; then
        echo "gate FAILED: $recompiled blocks were recompiled but none executed natively - the dispatcher never used T1" >&2
        exit 1
    fi
    echo "  tier t1: $recompiled blocks recompiled, $native executed natively"
elif [ "$recompiled" -ne 0 ]; then
    echo "gate FAILED: no tier was requested but $recompiled blocks were recompiled" >&2
    exit 1
fi

"$binary" --host > "$work/host.txt" || exit 1

if ! diff -u "$work/host.txt" "$work/guest.txt"; then
    echo "gate FAILED: guest output differs from the same C compiled for the host" >&2
    exit 1
fi

echo "  $(wc -l < "$work/guest.txt") lines identical to the host build"

if [ "$(contains "$work/guest.txt" 'guest: main counter 1000')" -ne 1 ]; then
    echo "gate FAILED: the main thread lost its thread-local across the guest threads" >&2
    exit 1
fi

if [ "$(contains "$work/guest.txt" 'counter 1000')" -ne 1 ]; then
    echo "gate FAILED: a guest thread saw the main thread's thread-local - TLS is not per-thread" >&2
    exit 1
fi

if [ "$(contains "$work/guest.txt" 'guest: thread ')" -ne 4 ]; then
    echo "gate FAILED: not every guest thread reported" >&2
    exit 1
fi

echo "gate PASSED: an ARM64 .so called printf, malloc and pthread_create under $tier"

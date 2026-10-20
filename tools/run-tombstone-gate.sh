#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"

CLANG="$SDK_DIR/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
harness="$buildDir/src/TombstoneHarness"
source="$PROJ_DIR/tests/guestlib/src/hello.c"

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit 77
fi

for file in "$harness" "$source"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing" >&2
        exit 1
    fi
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

if ! "$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libhello.so -o "$work/libhello.so" "$source"; then
    echo "gate FAILED: the guest fixture did not compile" >&2
    exit 1
fi

output=$(timeout 300 "$harness" "$work/libhello.so" 0x10 "$work/tombstone.txt" 2>&1)
status=$?

echo "$output" | tail -20

if [ "$status" -eq 0 ]; then
    echo "gate FAILED: the harness exited cleanly, so nothing faulted and this proves nothing" >&2
    exit 1
fi

if printf '%s' "$output" | grep -qF "the guest returned rather than faulting"; then
    echo "gate FAILED: the guest read an unmapped address without faulting" >&2
    exit 1
fi

for expected in \
    "translated guest code for" \
    "libhello.so" \
    "FaultWitness" \
    "the guest modules loaded at the fault" \
    "wrote the tombstone at"
do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: the fault report is missing '$expected'" >&2
        exit 1
    fi
done

if [ ! -s "$work/tombstone.txt" ]; then
    echo "gate FAILED: no tombstone was written" >&2
    exit 1
fi

echo "--- the tombstone"
head -8 "$work/tombstone.txt"

if ! grep -qF "acl tombstone, written " "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone has no header" >&2
    exit 1
fi

if ! grep -qF -- "--- the fault begins here ---" "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone does not separate the fault from the context before it" >&2
    exit 1
fi

if ! grep -qF "about to read guest address" "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone kept no context from before the fault" >&2
    exit 1
fi

if ! grep -qF "FaultWitness" "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone does not name the guest function that faulted" >&2
    exit 1
fi

if ! grep -qE "x0 =[0-9a-f]{16}.*x3 =[0-9a-f]{16}" "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone does not carry the guest register file" >&2
    exit 1
fi

if ! grep -qE "touching 0{14}10" "$work/tombstone.txt"; then
    echo "gate FAILED: the tombstone does not record the address that was touched" >&2
    exit 1
fi

echo "gate PASSED: a fault in translated code names the guest function, and the tombstone carries the registers, the modules and the lines before it"

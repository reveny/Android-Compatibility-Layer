#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
tier="${2:-t0}"
SRC_DIR="$PROJ_DIR/tests/ordering/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"
ITERATIONS="${ORDERING_ITERATIONS:-150000}"
ATTEMPTS="${ORDERING_ATTEMPTS:-4}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
binary="$buildDir/src/OrderingGateTest"

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libordering.so \
         -o "$work/libordering.so" "$SRC_DIR/ordering.c" || exit 1

attempt=1
iterations="$ITERATIONS"
status=$SKIP_EXIT_CODE

while [ "$attempt" -le "$ATTEMPTS" ]; do
    if [ "$attempt" -gt 1 ]; then
        echo "  no reachable counterexample at $((iterations / 4)) iterations - escalating to $iterations" >&2
    fi

    "$binary" "$work/libordering.so" "$iterations" --tier "$tier"
    status=$?

    if [ "$status" -ne $SKIP_EXIT_CODE ]; then
        break
    fi

    attempt=$((attempt + 1))
    iterations=$((iterations * 4))
done

if [ "$status" -eq $SKIP_EXIT_CODE ]; then
    if [ "$tier" = "t1" ]; then
        echo "gate FAILED: under T1 the guest's accesses become adjacent host instructions, so store buffering must be" >&2
        echo "reachable. After $ATTEMPTS escalating attempts it was not - either the litmus blocks were not recompiled" >&2
        echo "or the harness stopped interleaving, and either way this gate is blind." >&2
        exit 1
    fi

    echo "gate SKIPPED: no reachable counterexample under the current tier - see the diagnostic above" >&2
    exit $SKIP_EXIT_CODE
fi

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: a guest memory-ordering guarantee was violated under acl" >&2
    exit 1
fi

echo "gate PASSED: guest memory ordering holds under acl"

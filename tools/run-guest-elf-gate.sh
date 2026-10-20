#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
SRC_DIR="$PROJ_DIR/tests/guestlib/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
binary="$buildDir/src/GuestLibGateTest"

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

GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libhello.so -o "$work/libhello.so" "$SRC_DIR/hello.c" || exit 1

if [ ! -s "$work/libhello.so" ]; then
    echo "gate FAILED: the guest library did not link" >&2
    exit 1
fi

"$binary" "$work/libhello.so"
status=$?

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the guest library did not reproduce the host build under T0" >&2
    exit 1
fi

echo "gate PASSED: an ARM64 .so loaded, relocated and executed under T0"

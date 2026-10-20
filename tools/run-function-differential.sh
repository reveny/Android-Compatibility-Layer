#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
SRC_DIR="$PROJ_DIR/tests/tiers/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
binary="$buildDir/src/FunctionDifferentialTest"

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

"$CLANG" -O1 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libfunctions.so \
         -o "$work/libfunctions.so" "$SRC_DIR/functions.c" || exit 1

"$binary" "$work/libfunctions.so"

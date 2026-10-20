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
TOOL_DIR="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64"
CLANG="$TOOL_DIR/bin/aarch64-linux-android${API_LEVEL}-clang"
READELF="$TOOL_DIR/bin/llvm-readelf"
SYSROOT_LIB="$TOOL_DIR/sysroot/usr/lib/aarch64-linux-android"
binary="$buildDir/src/ElfReadelfDifferentialTest"

if [ ! -x "$CLANG" ] || [ ! -x "$READELF" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

libraries=()

GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libhello.so -o "$work/libhello.so" "$SRC_DIR/hello.c" || exit 1
libraries+=("$work/libhello.so")

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libhellonorelr.so -Wl,--pack-dyn-relocs=none \
         -o "$work/libhellonorelr.so" "$SRC_DIR/hello.c" || exit 1
libraries+=("$work/libhellonorelr.so")

if [ -f "$SYSROOT_LIB/libc++_shared.so" ]; then
    libraries+=("$SYSROOT_LIB/libc++_shared.so")
fi

apk="$PROJ_DIR/tests/app-debug.apk"
if [ -f "$apk" ] && command -v unzip >/dev/null 2>&1; then
    if unzip -o -q -j "$apk" 'lib/arm64-v8a/*.so' -d "$work/apklibs" 2>/dev/null; then
        while IFS= read -r extracted; do
            libraries+=("$extracted")
        done < <(find "$work/apklibs" -name '*.so' | sort)
    fi
fi

echo "comparing ${#libraries[@]} libraries against llvm-readelf"

"$binary" "$READELF" "${libraries[@]}"
status=$?

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the loader and llvm-readelf disagree about a real ARM64 library" >&2
    exit 1
fi

echo "gate PASSED: the loader agrees with llvm-readelf on every library"

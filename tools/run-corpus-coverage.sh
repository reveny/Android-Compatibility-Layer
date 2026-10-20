#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
SRC_DIR="$PROJ_DIR/tests/guest/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
BUDGET="${CORPUS_UNIMPLEMENTED_BUDGET:-0.0}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
OBJCOPY="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy"
binary="$buildDir/src/CorpusCoverageTest"

if [ ! -x "$CLANG" ] || [ ! -x "$OBJCOPY" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

: > "$work/text.bin"
for source in $(find "$SRC_DIR" -name '*.c' | sort); do
    name=$(basename "$source" .c)
    "$CLANG" -O2 -c -march=armv8.2-a+lse+rcpc -o "$work/$name.o" "$source" || exit 1
    "$OBJCOPY" -O binary --only-section=.text "$work/$name.o" "$work/$name.text" || exit 1
    cat "$work/$name.text" >> "$work/text.bin"
done

if [ ! -s "$work/text.bin" ]; then
    echo "gate FAILED: the corpus produced no .text bytes" >&2
    exit 1
fi

"$binary" "$work/text.bin" "$BUDGET"
status=$?

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: decoder coverage over the corpus regressed" >&2
    exit 1
fi

echo "gate PASSED: corpus coverage within the ${BUDGET} unimplemented budget"

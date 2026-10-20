#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
SRC_DIR="$PROJ_DIR/tests/bench/src"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"
ITERATIONS="${BENCH_ITERATIONS:-200000}"
REPEATS="${BENCH_REPEATS:-5}"
SKIP_EXIT_CODE=77

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
binary="$buildDir/src/BenchTiersTest"

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

"$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libbench.so \
         -o "$work/libbench.so" "$SRC_DIR/bench.c" || exit 1

runner=("$binary" "$work/libbench.so" "$ITERATIONS" "$REPEATS")
if command -v taskset >/dev/null 2>&1; then
    runner=(taskset -c 0 "${runner[@]}")
fi

"${runner[@]}"
status=$?

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the recompiler did not clear the throughput floor" >&2
    exit 1
fi

echo "gate PASSED: the recompiler clears the throughput floor"

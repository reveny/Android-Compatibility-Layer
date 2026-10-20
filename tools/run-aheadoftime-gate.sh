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
binary="$buildDir/src/AheadOfTimeGateTest"
linker="${ACL_LINKER:-ld.lld}"

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

if ! command -v "$linker" >/dev/null; then
    echo "gate SKIPPED: no linker '$linker' - set ACL_LINKER to one that accepts -shared" >&2
    exit $SKIP_EXIT_CODE
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/cache"

"$CLANG" -O1 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,libfunctions.so \
         -o "$work/libfunctions.so" "$SRC_DIR/functions.c" || exit 1

run()
{
    "$binary" "$work/libfunctions.so" --cache-root "$work/cache" 2>&1
}

cold=$(run)
coldStatus=$?

echo "$cold"

if [ "$coldStatus" -ne 0 ]; then
    echo "gate FAILED: the cold translation run exited $coldStatus" >&2
    exit 1
fi

warm=$(run)
warmStatus=$?

echo "$warm"

if [ "$warmStatus" -ne 0 ]; then
    echo "gate FAILED: the cached run exited $warmStatus" >&2
    exit 1
fi

contains()
{
    if [ "$(printf '%s\n' "$2" | grep -c -- "$1")" -eq 0 ]; then
        echo "gate FAILED: expected to see \"$1\"" >&2
        return 1
    fi
    return 0
}

contains "cache miss" "$cold" || exit 1
contains "cache hit" "$warm" || exit 1
contains "PASS nothing was interpreted at run time" "$cold" || exit 1
contains "PASS nothing was interpreted at run time" "$warm" || exit 1
contains "PASS nothing went through the run-time block JIT either" "$cold" || exit 1
contains "PASS translated code actually ran" "$cold" || exit 1
contains "PASS no untranslatable instruction was reached" "$cold" || exit 1
contains "aheadoftime-gate PASSED" "$cold" || exit 1
contains "aheadoftime-gate PASSED" "$warm" || exit 1

skipped=$("$binary" "$work/libfunctions.so" --cache-root "$work/cache" --skip-translation 2>&1)
skippedStatus=$?

if [ "$skippedStatus" -eq 0 ]; then
    echo "$skipped"
    echo "gate FAILED: with translation skipped the run still passed, so this gate cannot tell native from interpreted" >&2
    exit 1
fi

if [ "$(printf '%s\n' "$skipped" | grep -c -- "PASS nothing was interpreted at run time")" -ne 0 ]; then
    echo "gate FAILED: skipping translation did not make the interpreter counter move, so the counter proves nothing" >&2
    exit 1
fi

interpreted=$(printf '%s\n' "$skipped" | sed -n 's/.*, \([0-9]*\) interpreted.*/\1/p')
echo "red drive: with translation skipped the same work took $interpreted interpreted blocks instead of 5 translated ones"

echo "gate PASSED: the library was translated to native code before running, cached, and executed with no interpreter"

#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="$PROJ_DIR/tests/app-debug.apk"
binary="$buildDir/src/CodeMapUnitTest"

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

if [ ! -f "$apk" ]; then
    echo "gate FAILED: $apk is missing - it is the fixture this gate reads" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

if ! unzip -o -q -j "$apk" 'lib/arm64-v8a/libhelloworld.so' -d "$work"; then
    echo "gate FAILED: $apk does not carry lib/arm64-v8a/libhelloworld.so" >&2
    exit 1
fi

if ! file -b "$work/libhelloworld.so" | grep -q "ARM aarch64"; then
    echo "gate FAILED: the extracted library is not an aarch64 ELF, so this gate would prove nothing" >&2
    exit 1
fi

"$binary" "$work/libhelloworld.so"

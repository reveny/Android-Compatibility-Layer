#!/usr/bin/env bash
set -euo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$PROJ_DIR/tests/vectors/src"
OUT_DIR="$PROJ_DIR/tests/vectors"
SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
DEVICE_DIR=/data/local/tmp/acl
GOLDEN="$OUT_DIR/golden-arm64.txt"
PROVENANCE="$OUT_DIR/golden-arm64.provenance"

SKIP_EXIT_CODE=77

mode=capture
case "${1:-}" in
    "") echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2 ;;
    --check) mode=check ;;
    --regenerate) mode=regenerate ;;
    *) echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2 ;;
esac
[ "$#" -le 1 ] || { echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2; }

if [ "$mode" = "check" ] && [ ! -d "$SDK_DIR/ndk/$NDK_VERSION" ]; then
    echo "skip: NDK $NDK_VERSION not installed" >&2
    exit $SKIP_EXIT_CODE
fi

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
if [ ! -d "$NDK_DIR" ]; then
    echo "error: NDK $NDK_VERSION not found at $NDK_DIR" >&2
    echo "installed: $(ls "$SDK_DIR/ndk" 2>/dev/null | tr '\n' ' ')" >&2
    echo "override with NDK_VERSION=<version>" >&2
    exit 1
fi

CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang++"
[ -x "$CLANG" ] || { echo "error: $CLANG not found" >&2; exit 1; }

if ! command -v adb >/dev/null; then
    echo "skip: adb not installed" >&2
    exit $SKIP_EXIT_CODE
fi

if [ -z "${ANDROID_SERIAL:-}" ]; then
    transports=$(adb devices | tail -n +2 | grep -c . || true)
    if [ "$transports" -eq 0 ]; then
        echo "skip: no arm64 device attached" >&2
        exit $SKIP_EXIT_CODE
    fi
    if [ "$transports" -ne 1 ]; then
        echo "error: set ANDROID_SERIAL - adb reports $transports transports" >&2
        adb devices >&2
        exit 1
    fi
fi

state=$(adb get-state 2>&1 || true)
if [ "$state" != "device" ]; then
    if [ -n "${ANDROID_SERIAL:-}" ]; then
        echo "error: ANDROID_SERIAL=$ANDROID_SERIAL is set but not usable: $state" >&2
        exit 1
    fi
    echo "skip: adb device not ready: $state" >&2
    exit $SKIP_EXIT_CODE
fi

model=$(adb shell getprop ro.product.model | tr -d '\r' || true)
abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r' || true)
sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r' || true)
features=$(adb shell 'grep -m1 Features /proc/cpuinfo' 2>/dev/null | tr -d '\r' | sed 's/^Features[[:space:]]*:[[:space:]]*//' || true)
[ -n "$model" ] && [ -n "$abi" ] || { echo "error: could not read device properties" >&2; exit 1; }
[ "$abi" = "arm64-v8a" ] || { echo "error: device abi is $abi, expected arm64-v8a" >&2; exit 1; }

echo "mode   : $mode"
echo "ndk    : $NDK_VERSION  api=$API_LEVEL"
echo "device : ${ANDROID_SERIAL:-<single>}  $model  sdk=$sdk"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

"$CLANG" -std=c++20 -O0 -static -Wall -Wextra -Werror \
    -march=armv8.2-a+lse+rcpc \
    -I"$SRC_DIR" \
    -o "$work/acl-vectors-arm64" \
    "$SRC_DIR/VectorHarness.cpp" "$SRC_DIR/VectorTests.cpp"

adb shell "mkdir -p $DEVICE_DIR"
adb push "$work/acl-vectors-arm64" "$DEVICE_DIR/" >/dev/null
adb shell "chmod 755 $DEVICE_DIR/acl-vectors-arm64"

set +e
adb shell "$DEVICE_DIR/acl-vectors-arm64; echo EXIT=\$?" | tr -d '\r' > "$work/raw"
set -e

remoteExit=$(grep -o '^EXIT=[0-9]*$' "$work/raw" | tail -1 | cut -d= -f2 || true)
if [ -z "$remoteExit" ]; then
    echo "error: harness produced no exit marker - device output follows" >&2
    sed -n '1,20p' "$work/raw" >&2
    exit 1
fi
if [ "$remoteExit" != "0" ]; then
    echo "error: harness exited $remoteExit on device" >&2
    sed -n '1,20p' "$work/raw" >&2
    exit 1
fi

grep -v '^EXIT=' "$work/raw" | grep -v '^#' > "$work/clean" || true
actual=$(wc -l < "$work/clean")
[ "$actual" -gt 0 ] || { echo "error: harness produced no vectors" >&2; exit 1; }

if [ -f "$GOLDEN" ]; then
    expected=$(wc -l < "$GOLDEN")
    if [ "$actual" -ne "$expected" ] && [ "$mode" != "regenerate" ]; then
        echo "error: captured $actual vectors, committed golden has $expected" >&2
        echo "the suite changed size; re-run with --regenerate if that is intended" >&2
        exit 1
    fi
fi

if [ "$mode" = "check" ]; then
    if diff -u "$GOLDEN" "$work/clean"; then
        echo "OK: device reproduces all $actual golden vectors"
        exit 0
    fi
    echo "FAIL: device output differs from $GOLDEN" >&2
    exit 1
fi

cp "$work/clean" "$GOLDEN"

{
    echo "device_model=$model"
    echo "device_abi=$abi"
    echo "android_sdk=$sdk"
    echo "cpu_features=$features"
    echo "ndk=$NDK_VERSION"
    echo "api_level=$API_LEVEL"
} > "$PROVENANCE"

echo "captured $actual vectors to $GOLDEN"

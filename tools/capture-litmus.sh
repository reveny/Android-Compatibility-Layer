#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$PROJ_DIR/tests/litmus/src"
GOLDEN="$PROJ_DIR/tests/litmus/golden-litmus.txt"
PROVENANCE="$PROJ_DIR/tests/litmus/golden-litmus.provenance"
OBSERVED="$PROJ_DIR/tests/litmus/observed-outcomes.txt"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
ITERATIONS="${LITMUS_ITERATIONS:-500000}"
DEVICE_DIR=/data/local/tmp/acl
SKIP_EXIT_CODE=77

mode=""
case "${1:-}" in
    --check) mode=check ;;
    --regenerate) mode=regenerate ;;
    *) echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2 ;;
esac
[ "$#" -le 1 ] || { echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2; }

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang++"

if [ ! -x "$CLANG" ]; then
    if [ "$mode" = "check" ]; then
        echo "skip: NDK $NDK_VERSION not installed" >&2
        exit $SKIP_EXIT_CODE
    fi
    echo "error: NDK $NDK_VERSION not found at $NDK_DIR" >&2
    exit 1
fi

if ! command -v adb > /dev/null 2>&1; then
    echo "skip: adb not installed" >&2
    exit $SKIP_EXIT_CODE
fi

transports=$(adb devices | tail -n +2 | grep -c . || true)
if [ "$transports" -eq 0 ]; then
    echo "skip: no device attached" >&2
    exit $SKIP_EXIT_CODE
fi

if [ "$transports" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then
    echo "error: adb reports $transports transports - set ANDROID_SERIAL" >&2
    adb devices >&2
    exit 1
fi

state=$(adb get-state 2>&1 || true)
if [ "$state" != "device" ]; then
    echo "error: adb device not usable: $state" >&2
    exit 1
fi

abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
if [ "$abi" != "arm64-v8a" ]; then
    echo "error: device abi is $abi, expected arm64-v8a" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

"$CLANG" -std=c++20 -O2 -static -Wall -Wextra -Werror \
    -march=armv8.2-a+lse+rcpc \
    -o "$work/acl-litmus" \
    "$SRC_DIR/LitmusHarness.cpp" || exit 1

adb shell "mkdir -p $DEVICE_DIR" > /dev/null
adb push "$work/acl-litmus" "$DEVICE_DIR/" > /dev/null
adb shell "chmod 755 $DEVICE_DIR/acl-litmus"

adb shell "$DEVICE_DIR/acl-litmus $ITERATIONS 2> $DEVICE_DIR/litmus-observed.txt; echo EXIT=\$?" | tr -d '\r' > "$work/raw"

remoteExit=$(grep -o '^EXIT=[0-9]*$' "$work/raw" | tail -1 | cut -d= -f2 || true)
if [ -z "$remoteExit" ]; then
    echo "error: harness produced no exit marker - device output follows" >&2
    cat "$work/raw" >&2
    exit 1
fi

if [ "$remoteExit" -ne 0 ]; then
    echo "error: harness exited $remoteExit on device" >&2
    cat "$work/raw" >&2
    exit 1
fi

grep -v '^EXIT=' "$work/raw" > "$work/clean" || true
actual=$(wc -l < "$work/clean")
[ "$actual" -gt 0 ] || { echo "error: harness produced no verdicts" >&2; exit 1; }

if grep -qv ' forbidden=0 ' "$work/clean"; then
    echo "FAIL: a forbidden outcome was observed on real hardware" >&2
    cat "$work/clean" >&2
    exit 1
fi

if grep -q 'interleaved=no' "$work/clean"; then
    echo "FAIL: a litmus test never interleaved, so its forbidden=0 proves nothing" >&2
    cat "$work/clean" >&2
    exit 1
fi

if [ -f "$GOLDEN" ]; then
    expected=$(wc -l < "$GOLDEN")
    if [ "$actual" -ne "$expected" ] && [ "$mode" != "regenerate" ]; then
        echo "error: captured $actual verdicts, committed golden has $expected" >&2
        echo "the suite changed size; re-run with --regenerate if that is intended" >&2
        exit 1
    fi
fi

if [ "$mode" = "check" ]; then
    if diff -u "$GOLDEN" "$work/clean"; then
        echo "OK: all $actual litmus verdicts reproduce, no forbidden outcome observed"
        exit 0
    fi
    echo "FAIL: litmus verdicts differ from $GOLDEN" >&2
    exit 1
fi

cp "$work/clean" "$GOLDEN"
adb shell "cat $DEVICE_DIR/litmus-observed.txt" | tr -d '\r' > "$OBSERVED"

model=$(adb shell getprop ro.product.model | tr -d '\r')
sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
features=$(adb shell 'grep -m1 Features /proc/cpuinfo' | tr -d '\r' | sed 's/^Features[[:space:]]*:[[:space:]]*//')

{
    echo "device_model=$model"
    echo "device_abi=$abi"
    echo "android_sdk=$sdk"
    echo "cpu_features=$features"
    echo "ndk=$NDK_VERSION"
    echo "iterations=$ITERATIONS"
    echo "writer_cpu=0"
    echo "reader_cpu=4"
} > "$PROVENANCE"

echo "captured $actual litmus verdicts to $GOLDEN"

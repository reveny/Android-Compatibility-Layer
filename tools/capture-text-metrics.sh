#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GOLDEN="$PROJ_DIR/tests/text/golden-device-metrics.txt"
PROVENANCE="$PROJ_DIR/tests/text/golden-device-metrics.provenance"
FONT="$PROJ_DIR/third_party/roboto/Roboto-Regular.ttf"
HARNESS="$PROJ_DIR/tests/text/TextMetricsHarness.java"
REMOTE_DIR=/data/local/tmp/acl
SDK_JAR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar"
D8_JAR="${RUNTIME_HOST_ROOT:-$HOME/aosp/master-art/out/host/linux-x86}/framework/d8.jar"
SKIP=77

mode="${1:-}"
if [ "$#" -gt 1 ]; then
    echo "usage: $0 --check | --regenerate" >&2
    exit 2
fi

if [ "$mode" != "--check" ] && [ "$mode" != "--regenerate" ]; then
    echo "usage: $0 --check | --regenerate" >&2
    echo "  --check       compare the device against the committed golden" >&2
    echo "  --regenerate  overwrite the committed golden from the device" >&2
    exit 2
fi

if ! command -v adb > /dev/null; then
    echo "capture SKIPPED: adb is not installed" >&2
    exit $SKIP
fi

if [ ! -e "$SDK_JAR" ]; then
    echo "capture SKIPPED: android.jar not found at $SDK_JAR" >&2
    exit $SKIP
fi

if [ ! -e "$D8_JAR" ]; then
    echo "capture SKIPPED: d8 not found at $D8_JAR" >&2
    exit $SKIP
fi

devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
deviceCount="$(printf '%s\n' "$devices" | grep -c . || true)"

if [ "$deviceCount" -eq 0 ]; then
    echo "capture SKIPPED: no device attached" >&2
    exit $SKIP
fi

if [ "$deviceCount" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then
    echo "capture FAILED: $deviceCount devices attached and ANDROID_SERIAL is unset" >&2
    echo "                choosing one at random would silently capture from the wrong phone" >&2
    printf '%s\n' "$devices" >&2
    exit 1
fi

if [ -z "${ANDROID_SERIAL:-}" ]; then
    ANDROID_SERIAL="$(printf '%s\n' "$devices" | head -1)"
    export ANDROID_SERIAL
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/classes"
if ! javac --release 11 -nowarn -classpath "$SDK_JAR" -d "$work/classes" "$HARNESS" > "$work/javac.log" 2>&1; then
    echo "capture FAILED: the harness did not compile" >&2
    sed -n '1,10p' "$work/javac.log" >&2
    exit 1
fi

if ! java -cp "$D8_JAR" com.android.tools.r8.D8 --min-api 34 --lib "$SDK_JAR" --output "$work" "$work"/classes/acl/*.class \
     > "$work/d8.log" 2>&1; then
    echo "capture FAILED: d8 did not run" >&2
    sed -n '1,10p' "$work/d8.log" >&2
    exit 1
fi

(cd "$work" && zip -q metrics.jar classes.dex)

adb shell "mkdir -p $REMOTE_DIR/fonts" > /dev/null 2>&1
if ! adb push "$work/metrics.jar" "$REMOTE_DIR/metrics.jar" > /dev/null 2>&1; then
    echo "capture FAILED: could not push the harness" >&2
    exit 1
fi

if ! adb push "$FONT" "$REMOTE_DIR/fonts/Roboto-Regular.ttf" > /dev/null 2>&1; then
    echo "capture FAILED: could not push the font" >&2
    exit 1
fi

captured="$(adb shell "CLASSPATH=$REMOTE_DIR/metrics.jar app_process /system/bin acl.TextMetricsHarness 2>/dev/null; echo EXIT=\$?")"
status="$(printf '%s\n' "$captured" | sed -n 's/^EXIT=//p' | tail -1)"

if [ "$status" != "0" ]; then
    echo "capture FAILED: the on-device harness exited $status" >&2
    printf '%s\n' "$captured" | sed -n '1,10p' >&2
    exit 1
fi

printf '%s\n' "$captured" | sed 's/\r$//' | grep -v '^EXIT=' > "$work/device.txt"

if ! grep -q '^# end$' "$work/device.txt"; then
    echo "capture FAILED: the harness output has no end marker - it was truncated" >&2
    exit 1
fi

capturedLines="$(grep -c . "$work/device.txt")"
if [ "$capturedLines" -lt 100 ]; then
    echo "capture FAILED: only $capturedLines lines captured, expected hundreds" >&2
    exit 1
fi

if [ "$mode" = "--regenerate" ]; then
    cp "$work/device.txt" "$GOLDEN"
    echo "regenerated $GOLDEN ($capturedLines lines)"
    echo "review the diff before committing, and update $PROVENANCE"
    exit 0
fi

if [ ! -s "$GOLDEN" ]; then
    echo "capture FAILED: no committed golden at $GOLDEN (run with --regenerate)" >&2
    exit 1
fi

goldenLines="$(grep -c . "$GOLDEN")"
if [ "$capturedLines" -ne "$goldenLines" ]; then
    echo "capture FAILED: captured $capturedLines rows, the committed golden has $goldenLines" >&2
    echo "                changing the suite size needs --regenerate" >&2
    exit 1
fi

if ! diff -u "$GOLDEN" "$work/device.txt" > "$work/diff.txt"; then
    echo "capture FAILED: the device no longer produces the committed golden" >&2
    sed -n '1,30p' "$work/diff.txt" >&2
    exit 1
fi

echo "capture PASSED: the device still produces the committed golden ($goldenLines lines)"

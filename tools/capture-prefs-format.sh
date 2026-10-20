#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE=com.reveny.helloworld
PREFS=m3b_prefs.xml
REFERENCE="$PROJ_DIR/tests/m3b-prefs-device.xml"
SKIP=77

mode="${1:-}"
if [ "$#" -gt 1 ] || { [ "$mode" != "--check" ] && [ "$mode" != "--regenerate" ]; }; then
    echo "usage: $0 --check | --regenerate" >&2
    echo "  the device must be installed with $PACKAGE, awake and unlocked -" >&2
    echo "  before first unlock its credential-encrypted data directory does not exist" >&2
    exit 2
fi

if ! command -v adb > /dev/null; then
    echo "capture SKIPPED: adb is not installed" >&2
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
    printf '%s\n' "$devices" >&2
    exit 1
fi

if [ -z "${ANDROID_SERIAL:-}" ]; then
    ANDROID_SERIAL="$(printf '%s\n' "$devices" | head -1)"
    export ANDROID_SERIAL
fi

if ! adb shell "pm list packages" 2>/dev/null | grep -q "^package:$PACKAGE$"; then
    echo "capture SKIPPED: $PACKAGE is not installed on the device" >&2
    exit $SKIP
fi

locked="$(adb shell 'dumpsys user | grep -c RUNNING_LOCKED' 2>/dev/null | tr -d '\r')"
if [ "${locked:-0}" != "0" ]; then
    echo "capture SKIPPED: the device is in RUNNING_LOCKED - unlock it once so the app's" >&2
    echo "                 credential-encrypted data directory exists, then retry" >&2
    exit $SKIP
fi

adb shell "monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1" > /dev/null 2>&1
sleep 3

captured="$(adb shell "run-as $PACKAGE cat shared_prefs/$PREFS" 2>/dev/null | sed 's/\r$//')"

if [ -z "$captured" ]; then
    echo "capture FAILED: no preferences file on the device at shared_prefs/$PREFS" >&2
    exit 1
fi

if [ "$mode" = "--regenerate" ]; then
    printf '%s\n' "$captured" > "$REFERENCE"
    echo "regenerated $REFERENCE"
    exit 0
fi

if [ ! -s "$REFERENCE" ]; then
    echo "capture FAILED: no committed reference at $REFERENCE (run with --regenerate)" >&2
    exit 1
fi

if ! printf '%s\n' "$captured" | diff -u "$REFERENCE" - > /dev/null; then
    echo "capture FAILED: the device no longer writes the committed reference format" >&2
    printf '%s\n' "$captured" | diff -u "$REFERENCE" - >&2
    exit 1
fi

echo "capture PASSED: the device still writes the committed preferences format"

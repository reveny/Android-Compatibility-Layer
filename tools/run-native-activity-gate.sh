#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

activityClass="android.app.NativeActivity"
windowWidth=1080
windowHeight=2400

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ ! -f "$frameworkRes" ]; then
    echo "gate SKIPPED: no SDK platform android.jar at '$frameworkRes'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$apk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

listing=$(unzip -l "$apk")

if ! printf '%s' "$listing" | grep -E ' lib/arm64-v8a/libtriangle\.so$' > /dev/null; then
    echo "gate FAILED: $apk does not carry the arm64 native activity fixture" >&2
    exit 1
fi

if printf '%s' "$listing" | grep -E ' lib/(x86|x86_64|armeabi-v7a)/' > /dev/null; then
    echo "gate FAILED: $apk carries a library this gate must not be able to run natively" >&2
    exit 1
fi

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

output=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --activity "$activityClass" \
    --window "${windowWidth}x${windowHeight}" \
    --max-frames 2 \
    --guest-env "TRIANGLE_FRAMES=3" \
    "$apk" 2>&1)

echo "$output"

contains()
{
    printf '%s' "$2" | grep -qF -- "$1"
}

if ! contains "activity selected by --activity: $activityClass" "$output"; then
    echo "gate FAILED: $activityClass was not the activity that launched" >&2
    exit 1
fi

if ! contains "I/NativeActivity: native library: triangle" "$output"; then
    echo "gate FAILED: the android.app.lib_name meta-data did not reach the framework" >&2
    exit 1
fi

if ! contains "[nativeactivity] ANativeActivity_onCreate ran and registered" "$output"; then
    echo "gate FAILED: the guest entry point did not run" >&2
    exit 1
fi

registered=$(printf '%s' "$output" | sed -n 's/.*ANativeActivity_onCreate ran and registered \([0-9]*\) callback.*/\1/p' | head -1)
if [ -z "$registered" ] || [ "$registered" -lt 1 ]; then
    echo "gate FAILED: the guest registered no callbacks, so nothing could be dispatched back into it" >&2
    exit 1
fi

for callback in onStart onResume onNativeWindowCreated; do
    if ! contains "[nativeactivity] $callback" "$output"; then
        echo "gate FAILED: $callback was never dispatched into the guest" >&2
        exit 1
    fi
done

if ! contains "[nativeactivity] the guest window is ${windowWidth}x${windowHeight}" "$output"; then
    echo "gate FAILED: the activity was not handed a window" >&2
    exit 1
fi

if ! contains "[guest triangle] native window created ${windowWidth}x${windowHeight}" "$output"; then
    echo "gate FAILED: the guest did not read the window back at the size we published" >&2
    exit 1
fi

blocks=$(printf '%s' "$output" | grep -o '\[launcher\] guest blocks: .*' | head -1)
if [ -z "$blocks" ]; then
    echo "gate FAILED: the launcher did not report how the guest ran" >&2
    exit 1
fi

translated=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\2/p')
interpreted=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\3/p')
traps=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\4/p')

if [ -z "$translated" ] || [ "$translated" -lt 1 ]; then
    echo "gate FAILED: nothing was translated, so the activity cannot have run as translated arm64" >&2
    exit 1
fi

if [ "$interpreted" != "0" ]; then
    echo "gate FAILED: $interpreted block(s) were interpreted - this milestone claims translation, not interpretation" >&2
    exit 1
fi

if [ "$traps" != "0" ]; then
    echo "gate FAILED: $traps trap(s) were hit while the activity ran" >&2
    exit 1
fi

if contains "Exception in thread" "$output" || contains "NoSuchMethodError" "$output"; then
    echo "gate FAILED: the guest or the framework threw" >&2
    exit 1
fi

echo "gate PASSED: $activityClass ran $translated translated block(s), registered $registered callback(s), and drew on a ${windowWidth}x${windowHeight} window"

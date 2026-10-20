#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ -z "$frameworkRes" ]; then
    echo "gate FAILED: RUNTIME_FRAMEWORK_RES is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -f "$frameworkRes" ]; then
    echo "gate SKIPPED: no SDK platform android.jar at '$frameworkRes'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

if [ ! -f "$apk" ]; then
    echo "gate FAILED: $apk is missing" >&2
    exit 1
fi

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

matches()
{
    printf '%s' "$2" | grep -cE "$1" > /dev/null
}

listing=$(unzip -l "$apk" 2>/dev/null)
if [ -z "$listing" ]; then
    echo "gate FAILED: $apk could not be listed as a zip archive" >&2
    exit 1
fi

if ! contains "lib/arm64-v8a/libhelloworld.so" "$listing"; then
    echo "gate FAILED: $apk carries no lib/arm64-v8a/*.so, so the gate would prove nothing" >&2
    exit 1
fi

if contains "lib/x86_64/" "$listing"; then
    echo "gate FAILED: $apk carries an x86_64 library - it is meant to be a real, arm64-only Android APK" >&2
    exit 1
fi

entryCount=$(printf '%s' "$listing" | tail -1 | awk '{print $2}')
dexCount=$(printf '%s' "$listing" | grep -cE ' classes[0-9]*\.dex$')
libCount=$(printf '%s' "$listing" | grep -cE ' lib/arm64-v8a/[^/]+\.so$')

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

output=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --no-activity \
    --trace jni \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

if [ "$entryCount" -lt 900 ]; then
    echo "gate FAILED: $apk has only $entryCount entries - this gate exists to run the enumerator over a large real archive" >&2
    exit 1
fi

if ! contains "[jni] $entryCount archive entries, $dexCount dex, 1 native abi" "$output"; then
    echo "gate FAILED: the enumerator did not report $entryCount entries and $dexCount dex files" >&2
    exit 1
fi

if [ "$libCount" -lt 1 ]; then
    echo "gate FAILED: $apk carries no arm64-v8a library - this gate exists to watch an arm64-only archive reach the translator" >&2
    exit 1
fi

if ! contains "[launcher] extracted $libCount arm64-v8a guest librar" "$output"; then
    echo "$output"
    echo "gate FAILED: an arm64-only archive did not fall through to the translator with all $libCount of its libraries" >&2
    exit 1
fi

if ! contains "guest library to $dataRoot/com.reveny.helloworld/lib/arm64-v8a" "$output" &&
   ! contains "guest libraries to $dataRoot/com.reveny.helloworld/lib/arm64-v8a" "$output"; then
    echo "gate FAILED: the guest libraries were not staged under the package's own arm64-v8a directory" >&2
    exit 1
fi

if ! contains "[launcher] the guest bridge is live" "$output"; then
    echo "gate FAILED: the guest bridge was not started for an arm64-only archive" >&2
    exit 1
fi

if matches 'unimplemented|Exception in thread|NoSuchMethodError|NoClassDefFoundError' "$output"; then
    echo "gate FAILED: a strict guard fired or the guest threw" >&2
    exit 1
fi

if [ ! -f "$dataRoot/com.reveny.helloworld/lib/libhelloworld.so" ]; then
    echo "gate FAILED: the translator shim was not staged under the guest library's own soname" >&2
    exit 1
fi

if file -b "$dataRoot/com.reveny.helloworld/lib/libhelloworld.so" | grep -q "ARM aarch64"; then
    echo "gate FAILED: the staged shim is an aarch64 ELF - host ART must be able to dlopen it" >&2
    exit 1
fi

if ! file -b "$dataRoot/com.reveny.helloworld/lib/arm64-v8a/libhelloworld.so" | grep -q "ARM aarch64"; then
    echo "gate FAILED: the real guest library beneath the shim is not an aarch64 ELF" >&2
    exit 1
fi

absent=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$(mktemp -d)" \
    --no-activity \
    --abi armeabi-v7a \
    "$apk" 2>&1)

if ! contains "has no armeabi-v7a native library (it ships: arm64-v8a)" "$absent"; then
    echo "$absent"
    echo "gate FAILED: an ABI the archive genuinely does not ship was not refused by name" >&2
    exit 1
fi

selected=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --no-activity \
    --abi arm64-v8a \
    --trace jni \
    "$apk" 2>&1)
selectedStatus=$?

if [ "$selectedStatus" -ne 0 ]; then
    echo "$selected"
    echo "gate FAILED: selecting arm64-v8a exited $selectedStatus" >&2
    exit 1
fi

if ! contains "[launcher] extracted $libCount arm64-v8a native librar" "$selected"; then
    echo "$selected"
    echo "gate FAILED: the stored arm64 library was not extracted when its ABI was selected" >&2
    exit 1
fi

extracted="$dataRoot/com.reveny.helloworld/lib/libhelloworld.so"
if [ ! -f "$extracted" ]; then
    echo "gate FAILED: $extracted was not written" >&2
    exit 1
fi

described=$(file -b "$extracted")
if ! contains "ELF 64-bit LSB shared object, ARM aarch64" "$described"; then
    echo "gate FAILED: $extracted is not an arm64 ELF: $described" >&2
    exit 1
fi

echo "gate PASSED: the zip layer enumerated $entryCount real entries, extracted a stored arm64 library, and refused the ABI we cannot run"

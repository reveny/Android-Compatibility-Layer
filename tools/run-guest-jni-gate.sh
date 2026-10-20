#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
tier="${2:-t0}"
apk="$PROJ_DIR/tests/app-debug.apk"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
SKIP_EXIT_CODE=77

binary="$buildDir/src/JniBridgeGateTest"
shim="$buildDir/src/libaclguest.so"

for artifact in "$binary" "$shim"; do
    if [ ! -e "$artifact" ]; then
        echo "gate FAILED: $artifact is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

if [ ! -f "$apk" ]; then
    echo "gate FAILED: $apk is missing - it is the milestone's fixture" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit $SKIP_EXIT_CODE
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/guest" "$work/shim"

if ! unzip -o -q -j "$apk" 'lib/arm64-v8a/libhelloworld.so' -d "$work/guest"; then
    echo "gate FAILED: $apk does not carry lib/arm64-v8a/libhelloworld.so" >&2
    exit 1
fi

if ! file -b "$work/guest/libhelloworld.so" | grep -q "ARM aarch64"; then
    echo "gate FAILED: the extracted library is not an aarch64 ELF, so this gate would prove nothing" >&2
    file -b "$work/guest/libhelloworld.so" >&2
    exit 1
fi

cp "$shim" "$work/shim/libhelloworld.so"

if file -b "$work/shim/libhelloworld.so" | grep -q "ARM aarch64"; then
    echo "gate FAILED: the staged shim is an aarch64 ELF - host ART must be able to dlopen it" >&2
    exit 1
fi

output=$("$binary" "$hostRoot" "$apk" "$work/guest" "$work/shim" --tier "$tier" 2>&1)
status=$?

echo "$output"

contains()
{
    if [ "$(printf '%s\n' "$output" | grep -c -- "$1")" -eq 0 ]; then
        echo "gate FAILED: expected to see \"$1\"" >&2
        return 1
    fi
    return 0
}

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the harness exited $status" >&2
    exit 1
fi

contains "PASS System.loadLibrary routed one arm64 library into the guest runtime" || exit 1
contains "PASS guest JNI_OnLoad ran exactly once, driven by System.loadLibrary" || exit 1
contains "PASS guest JNI_OnLoad returned JNI_VERSION_1_6" || exit 1
contains "PASS __sF resolved to guest data, not to an executable stub" || exit 1
contains "PASS RegisterNatives bound exactly one method into host ART" || exit 1
contains "PASS the guest reached host JNI through the guest-visible vtable" || exit 1
contains "PASS the whole round trip touched no unimplemented JNI slot" || exit 1
contains 'stringFromJNI() returned "Hello World from C++"' || exit 1
contains "PASS arm64 guest code called back into Java" || exit 1
contains "guest-jni-gate PASSED" || exit 1

echo "gate PASSED: an arm64 .so from an APK did JNI in both directions and returned Hello World from C++"

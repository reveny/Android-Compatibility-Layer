#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/minimal/minimal.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$apk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

output=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$dataRoot" \
    --no-activity \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Minimal: MinimalApplication.onCreate package=com.example.minimal$'; then
    echo "gate FAILED: Application.onCreate did not report the manifest package" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Minimal: applicationContext=true$'; then
    echo "gate FAILED: getApplicationContext() was null" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Minimal: hello from the guest$'; then
    echo "gate FAILED: guest never reached the end of onCreate" >&2
    exit 1
fi

if [ ! -d "$dataRoot/com.example.minimal" ]; then
    echo "gate FAILED: per-package data directory was not created under the data root" >&2
    exit 1
fi

echo "gate PASSED: the APK booted to Application.onCreate on real ART"

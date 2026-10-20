#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

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
    --trace resources \
    --no-activity \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^\[resources\] 0x7f0f001b -> "androidx.startup"$'; then
    echo 'gate FAILED: getString(0x7f0f001b) did not resolve to "androidx.startup"' >&2
    echo 'this is the lookup discoverAndInitialize matches meta-data against; a wrong value' >&2
    echo 'discovers no initializers at all and reports no error' >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Bootstrap: factory: androidx.core.app.CoreComponentFactory$'; then
    echo "gate FAILED: the manifest's appComponentFactory was not instantiated from guest dex" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Bootstrap: provider ready: androidx.startup.InitializationProvider '; then
    echo "gate FAILED: InitializationProvider.onCreate did not complete" >&2
    exit 1
fi

for initializer in \
    androidx.lifecycle.ProcessLifecycleInitializer \
    androidx.emoji2.text.EmojiCompatInitializer \
    androidx.profileinstaller.ProfileInstallerInitializer
do
    if ! printf '%s' "$output" | grep -q "^I/StartupProbe: initializer $initializer initialized=true$"; then
        echo "gate FAILED: $initializer did not run" >&2
        exit 1
    fi
done

if ! printf '%s' "$output" | grep -q '^I/Bootstrap: Application.onCreate returned: '; then
    echo "gate FAILED: Application.onCreate did not return" >&2
    exit 1
fi

echo "gate PASSED: factory honoured, providers ran, initializers discovered, Application.onCreate returned"

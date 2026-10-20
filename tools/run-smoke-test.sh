#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

smokeTest="$buildDir/src/ArtRuntimeSmokeTest"
dex="$buildDir/tests/probe/probe.dex"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ -z "$hostRoot" ]; then
    echo "smoke FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "smoke SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$smokeTest" "$dex"; do
    if [ ! -e "$file" ]; then
        echo "smoke FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

output=$("$smokeTest" "$hostRoot" "$dex" Probe 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "smoke FAILED: exited $status" >&2
    exit 1
fi

for expected in "icu configured" "vm started" "found class Probe" "main returned cleanly"; do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "smoke FAILED: missing '$expected'" >&2
        exit 1
    fi
done

echo "smoke PASSED: ART started in-process and ran the probe"

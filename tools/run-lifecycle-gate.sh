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

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

run()
{
    "$launcher" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --data-root "$dataRoot" \
        --window 1080x2400 \
        --density 440 \
        "$@" \
        "$apk" 2>&1
}

echo "=== first run: cold create, a configuration change, then a stop that saves"

first="$(run --max-frames 200 --reconfigure "120:2400x1080@440")"
firstStatus=$?

if [ "$firstStatus" -ne 0 ]; then
    echo "gate FAILED: the first run exited $firstStatus" >&2
    printf '%s\n' "$first" | tail -30 >&2
    exit 1
fi

for expected in \
    "I/LifecycleProbe: no retained instance, this is a cold create" \
    "I/LifecycleProbe: created with no saved state, this is the first visit" \
    "I/Configurations: the configuration changed: orientation screenSize" \
    "I/LifecycleProbe: handled the configuration change at 872x392 dp, orientation 2" \
    "I/LifecycleProbe: saving visits=1" \
    "I/Activities: the state of com.reveny.helloworld.MainActivity is on disk, so it survives the process"
do
    if ! contains "$expected" "$first"; then
        echo "gate FAILED: the first run is missing '$expected'" >&2
        printf '%s\n' "$first" | grep -E "LifecycleProbe|Configurations|Activities" >&2
        exit 1
    fi
done

if contains "I/Configurations: 0 activity(ies) declared they handle it" "$first"; then
    echo "gate FAILED: MainActivity declares configChanges, so the change must not have forced a recreation" >&2
    exit 1
fi

echo "=== the same run, recreated on request: the instance is retained and the state comes back"

for expected in \
    "I/LifecycleProbe: asking to be recreated now that the configuration settled" \
    "I/LifecycleProbe: retaining generation 1 across the recreation" \
    "I/Activities: recreated com.reveny.helloworld.MainActivity with" \
    "I/LifecycleProbe: the instance survived the recreation, generation 1" \
    "I/LifecycleProbe: restored visits=1 note=carried-across-the-process ratio=0.625 tags=alpha,beta"
do
    if ! contains "$expected" "$first"; then
        echo "gate FAILED: the recreation is missing '$expected'" >&2
        printf '%s\n' "$first" | grep -E "LifecycleProbe|Configurations|Activities" >&2
        exit 1
    fi
done

stateFile="$(find "$dataRoot" -name 'saved-state-com.reveny.helloworld.MainActivity.bin' -print -quit)"
if [ -z "$stateFile" ] || [ ! -s "$stateFile" ]; then
    echo "gate FAILED: no saved state was written under $dataRoot" >&2
    find "$dataRoot" -name 'saved-state*' >&2
    exit 1
fi

firstStateMarker="$(cksum < "$stateFile") $(stat -c %s "$stateFile")"

echo "=== second run: the process is gone, the state is not"

second="$(run --max-frames 60)"
secondStatus=$?

if [ "$secondStatus" -ne 0 ]; then
    echo "gate FAILED: the second run exited $secondStatus" >&2
    printf '%s\n' "$second" | tail -30 >&2
    exit 1
fi

for expected in \
    "comes back with the state it left behind" \
    "I/LifecycleProbe: restored visits=1 note=carried-across-the-process ratio=0.625 tags=alpha,beta"
do
    if ! contains "$expected" "$second"; then
        echo "gate FAILED: the second run is missing '$expected'" >&2
        printf '%s\n' "$second" | grep -E "LifecycleProbe|Activities" >&2
        exit 1
    fi
done

if contains "I/LifecycleProbe: created with no saved state" "$second"; then
    echo "gate FAILED: the second run created cold, so the state did not survive" >&2
    exit 1
fi

restores="$(printf '%s\n' "$second" | grep -c "comes back with the state it left behind")"
if [ "$restores" -ne 1 ]; then
    echo "gate FAILED: the state was restored $restores times, expected exactly once - restoring must consume the file" >&2
    exit 1
fi

if [ ! -s "$stateFile" ]; then
    echo "gate FAILED: the second run consumed the state but did not write its own, so the cycle does not repeat" >&2
    exit 1
fi

if [ "$(cksum < "$stateFile") $(stat -c %s "$stateFile")" = "$firstStateMarker" ]; then
    echo "note: the second run wrote a byte-identical state, which is expected for an idempotent save"
fi

echo "note: the state crossed a real process boundary - the first launcher had exited before the second started"

echo "gate PASSED: state is written at stop and restored on the next launch, and a declared configuration change is handled in place"

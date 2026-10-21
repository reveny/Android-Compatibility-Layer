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
guestFrames=3

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

if ! unzip -l "$apk" | grep -E ' lib/arm64-v8a/libtriangle\.so$' > /dev/null; then
    echo "gate FAILED: $apk does not carry the arm64 native activity fixture" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

contains()
{
    printf '%s' "$2" | grep -qF -- "$1"
}

run()
{
    local name="$1"
    shift

    mkdir -p "$work/$name/data" "$work/$name/cache"

    "$launcher" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --framework-res "$frameworkRes" \
        --data-root "$work/$name/data" \
        --cache-root "$work/$name/cache" \
        --activity "$activityClass" \
        --max-frames 2 \
        --guest-env "TRIANGLE_FRAMES=$guestFrames" \
        --no-strict \
        --jit \
        "$@" \
        "$apk" 2>&1
}

field()
{
    printf '%s' "$2" | grep -o "$1" | head -1 | grep -oE '[0-9]+' | head -1
}

check_common()
{
    local name="$1"
    local output="$2"
    local status="$3"

    if [ "$status" -ne 0 ]; then
        echo "$output"
        echo "gate FAILED: $name: the launcher exited $status" >&2
        exit 1
    fi

    if contains "ahead of time at" "$output" || contains "[acl] translating" "$output" || contains "translated in" "$output"; then
        echo "$output"
        echo "gate FAILED: $name: a library was translated ahead of time under --jit" >&2
        exit 1
    fi

    if [ -n "$(find "$work/$name/cache" -name '*.o' -o -name '*.so' 2>/dev/null | head -1)" ]; then
        echo "$output"
        echo "gate FAILED: $name: translated objects were written to the cache under --jit" >&2
        exit 1
    fi

    if ! contains "nothing is translated ahead of time" "$output"; then
        echo "$output"
        echo "gate FAILED: $name: the launcher did not announce the run-time compiler" >&2
        exit 1
    fi

    if ! contains "[guest triangle] presented frame $((guestFrames - 1))" "$output"; then
        echo "$output"
        echo "gate FAILED: $name: the guest did not present all $guestFrames frames" >&2
        exit 1
    fi

    if contains "Exception in thread" "$output" || contains "NoSuchMethodError" "$output"; then
        echo "$output"
        echo "gate FAILED: $name: the guest or the framework threw" >&2
        exit 1
    fi

    if [ "$(field '[0-9][0-9]* translated, ' "$output")" != "0" ]; then
        echo "$output"
        echo "gate FAILED: $name: blocks ran from ahead-of-time translations under --jit" >&2
        exit 1
    fi

    if [ "$(field '[0-9][0-9]* traps hit' "$output")" != "0" ]; then
        echo "$output"
        echo "gate FAILED: $name: traps were hit while the activity ran" >&2
        exit 1
    fi
}

hot=$(run hot --setting jit-threshold=0)
hotStatus=$?
check_common hot "$hot" "$hotStatus"

if ! contains "compiled on a background thread" "$hot"; then
    echo "$hot"
    echo "gate FAILED: hot: the run-time compiler did not default to a background thread" >&2
    exit 1
fi

hotRegions=$(field 'jit: [0-9][0-9]* region' "$hot")
hotNative=$(field '[0-9][0-9]* entries ran compiled' "$hot")

if [ -z "$hotRegions" ] || [ "$hotRegions" -lt 1 ] || [ -z "$hotNative" ] || [ "$hotNative" -lt 1 ]; then
    echo "$hot"
    echo "gate FAILED: hot: with a threshold of 0 nothing was compiled at run time, or nothing ran compiled" >&2
    exit 1
fi

inline=$(run inline --setting jit-threshold=0 --setting jit-background=0)
inlineStatus=$?
check_common inline "$inline" "$inlineStatus"

inlineRegions=$(field 'jit: [0-9][0-9]* region' "$inline")
inlineBlocks=$(field 'covering [0-9][0-9]* block' "$inline")
inlineInterpreted=$(field '[0-9][0-9]* interpreted' "$inline")

if [ -z "$inlineRegions" ] || [ "$inlineRegions" -lt 1 ] || [ "$inlineInterpreted" != "0" ]; then
    echo "$inline"
    echo "gate FAILED: inline: compiling on first use still left blocks to the interpreter" >&2
    exit 1
fi

if [ -z "$inlineBlocks" ] || [ "$inlineBlocks" -le "$inlineRegions" ]; then
    echo "$inline"
    echo "gate FAILED: inline: each region covered a single block, so regions are not being formed" >&2
    exit 1
fi

cold=$(run cold)
coldStatus=$?
check_common cold "$cold" "$coldStatus"

coldInterpreted=$(field '[0-9][0-9]* interpreted' "$cold")

if [ -z "$coldInterpreted" ] || [ "$coldInterpreted" -lt 1 ]; then
    echo "$cold"
    echo "gate FAILED: cold: with the default threshold no cold block was interpreted" >&2
    exit 1
fi

refused=$("$launcher" --jit --require-translated "$apk" 2>&1)
refusedStatus=$?

if [ "$refusedStatus" -eq 0 ] || ! contains "cannot also require every block to be translated" "$refused"; then
    echo "$refused"
    echo "gate FAILED: --jit with --require-translated was not refused" >&2
    exit 1
fi

echo "gate PASSED: --jit translated nothing ahead of time, interpreted cold code, and compiled $inlineRegions region(s) covering $inlineBlocks block(s) at run time"

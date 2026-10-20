#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/strict/strict.apk}"

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
        echo "gate FAILED: $file is missing" >&2
        exit 1
    fi
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

run()
{
    timeout 120 "$launcher" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --data-root "$work/data" \
        --no-activity \
        "$@" \
        "$apk" > "$work/out.txt" 2>&1
    echo $?
}

strictStatus="$(run --strict)"
strictOutput="$(cat "$work/out.txt")"

if [ "$strictStatus" -ne 134 ]; then
    echo "gate FAILED: with --strict a guarded call exited $strictStatus, expected 134 (SIGABRT)" >&2
    printf '%s\n' "$strictOutput" | tail -15 >&2
    exit 1
fi

if ! printf '%s' "$strictOutput" | grep -q '\[framework\] unimplemented:'; then
    echo "gate FAILED: strict mode aborted without naming the unimplemented method" >&2
    printf '%s\n' "$strictOutput" | tail -15 >&2
    exit 1
fi

if printf '%s' "$strictOutput" | grep -q 'survived the guarded call'; then
    echo "gate FAILED: execution continued past a strict guard" >&2
    exit 1
fi

relaxedStatus="$(run --no-strict)"
relaxedOutput="$(cat "$work/out.txt")"

if [ "$relaxedStatus" -ne 0 ]; then
    echo "gate FAILED: with --no-strict the run exited $relaxedStatus, expected 0" >&2
    printf '%s\n' "$relaxedOutput" | tail -15 >&2
    exit 1
fi

if ! printf '%s' "$relaxedOutput" | grep -q 'survived the guarded call'; then
    echo "gate FAILED: with --no-strict execution did not continue past the guard" >&2
    printf '%s\n' "$relaxedOutput" | tail -15 >&2
    exit 1
fi

if ! printf '%s' "$relaxedOutput" | grep -q '\[framework\] unimplemented:'; then
    echo "gate FAILED: with --no-strict the guard did not even report" >&2
    exit 1
fi

for expected in \
    "I/Permissions: the manifest declares 2 permission(s), 1 of them granted" \
    "I/Strict: INTERNET is granted" \
    "I/Strict: CAMERA is denied" \
    "I/Strict: READ_SMS is denied" \
    "I/Strict: the package reports version 1.4-strict code 7 with 2 requested permission(s)" \
    "I/Strict: a touchscreen is true and a telephone is false"
do
    if ! printf '%s' "$relaxedOutput" | grep -qF "$expected"; then
        echo "gate FAILED: missing '$expected'" >&2
        printf '%s\n' "$relaxedOutput" | grep -E "Strict|Permissions" >&2
        exit 1
    fi
done

echo "note: a normal permission is granted, a dangerous one declared is not, and one never declared is refused outright"

echo "gate PASSED: strict aborts with 134 and names the method, strict=0 reports and continues, and permissions and package facts are the manifest's"

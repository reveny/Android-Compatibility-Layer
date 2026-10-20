#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/window/window.apk}"

launcher="$buildDir/src/acl"
harness="$buildDir/src/HostedWindowHarness"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$harness" "$frameworkJar" "$apk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing" >&2
        exit 1
    fi
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

output=$(timeout 180 env -u WAYLAND_DISPLAY -u WAYLAND_SOCKET \
    "$harness" "$launcher" "$apk" "$work/frame.raw" 400 300 0 \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/data" \
    --max-frames 3 \
    --log "$work/run.log" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the run exited $status" >&2
    exit 1
fi

if [ ! -s "$work/run.log" ]; then
    echo "gate FAILED: no log was written" >&2
    exit 1
fi

echo "--- first four captured lines"
head -4 "$work/run.log"

if ! grep -qE '^[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3} [VDIWEF]/[^:]+: ' "$work/run.log"; then
    echo "gate FAILED: the log is not in the timestamped priority-and-tag shape" >&2
    exit 1
fi

for expected in \
    "I/Pattern: PatternActivity.onCreate package=com.example.window" \
    "I/Pattern: PatternView.onDraw 400x300" \
    "I/Bootstrap: the activity is destroyed" \
    "I/hostedwindow: hosted by a shell"
do
    if ! grep -qF "$expected" "$work/run.log"; then
        echo "gate FAILED: the log is missing '$expected'" >&2
        exit 1
    fi
done

appLines=$(grep -c "I/Pattern: " "$work/run.log")
if [ "$appLines" -lt 5 ]; then
    echo "gate FAILED: only $appLines line(s) from the app itself were captured" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -qE "\[launcher\] log: [0-9]+ line\(s\) kept of [0-9]+, 0 dropped"; then
    echo "gate FAILED: the run dropped log lines, or did not report the buffer at all" >&2
    exit 1
fi

if grep -qE '^[0-9]{2}-[0-9]{2} [0-9:.]+ I/stderr: \[' "$work/run.log"; then
    echo "gate FAILED: a module line was filed under stderr rather than given its own tag" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -qF "[diagnostics] this app asked for nothing this runtime does not have"; then
    echo "gate FAILED: a run that touches no gap should say so plainly" >&2
    exit 1
fi

strictApk="$buildDir/tests/strict/strict.apk"

if [ ! -e "$strictApk" ]; then
    echo "gate FAILED: $strictApk is missing, so the gap report cannot be exercised" >&2
    exit 1
fi

gaps=$(timeout 180 env -u WAYLAND_DISPLAY -u WAYLAND_SOCKET \
    "$launcher" \
    --no-strict \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/gap-data" \
    --no-activity \
    "$strictApk" 2>&1)
status=$?

echo "--- the gap report"
printf '%s' "$gaps" | grep "\[diagnostics\]"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the gap run exited $status" >&2
    exit 1
fi

if ! printf '%s' "$gaps" | grep -qE "\[diagnostics\] this app asked for 1 thing\(s\) this runtime does not have"; then
    echo "gate FAILED: the report did not count the one thing the fixture asks for" >&2
    exit 1
fi

if ! printf '%s' "$gaps" | grep -qE "\[diagnostics\] +framework +android.content.Context.getSystemService\(no-such-service-at-all\) x1"; then
    echo "gate FAILED: the report did not name the missing call, its kind and its count" >&2
    exit 1
fi

echo "gate PASSED: the whole run is captured with priorities and tags, nothing was dropped, and the exit report names what the app asked for and did not get"

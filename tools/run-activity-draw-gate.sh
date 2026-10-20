#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/window/window.apk}"
golden="${3:-$PROJ_DIR/tests/window/pattern-golden.raw}"

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

for file in "$launcher" "$frameworkJar" "$apk" "$golden"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing" >&2
        exit 1
    fi
done

export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
if [ ! -d "$XDG_RUNTIME_DIR" ]; then
    echo "gate SKIPPED: no XDG_RUNTIME_DIR at '$XDG_RUNTIME_DIR'" >&2
    exit 77
fi

work="$(mktemp -d)"
westonPid=""

cleanup()
{
    if [ -n "$westonPid" ]; then kill "$westonPid" 2>/dev/null; wait "$westonPid" 2>/dev/null; fi
    rm -rf "$work"
}
trap cleanup EXIT

if command -v weston >/dev/null; then
    socket="acl-m2a-$$"
    weston --backend=headless --socket="$socket" --width=640 --height=480 --idle-time=0 \
        > "$work/weston.log" 2>&1 &
    westonPid=$!

    for _ in $(seq 1 50); do
        [ -S "$XDG_RUNTIME_DIR/$socket" ] && break
        sleep 0.1
    done

    if [ -S "$XDG_RUNTIME_DIR/$socket" ]; then
        export WAYLAND_DISPLAY="$socket"
    else
        echo "note: headless weston did not come up" >&2
        sed -n '1,10p' "$work/weston.log" >&2
        kill "$westonPid" 2>/dev/null
        westonPid=""

        if [ -z "${WAYLAND_DISPLAY:-}" ]; then
            echo "gate SKIPPED: weston failed and there is no session compositor" >&2
            exit 77
        fi
        echo "note: falling back to the session compositor $WAYLAND_DISPLAY" >&2
    fi
elif [ -z "${WAYLAND_DISPLAY:-}" ]; then
    echo "gate SKIPPED: no weston and no WAYLAND_DISPLAY" >&2
    exit 77
fi

output=$(timeout 120 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/data" \
    --max-frames 3 \
    --window 400x300 \
    --frame-dump "$work/frame.raw" \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

for expected in \
    "I/Pattern: WindowApplication.onCreate package=com.example.window" \
    "I/Bootstrap: activity: com.example.window.PatternActivity" \
    "I/Pattern: PatternActivity.onCreate package=com.example.window" \
    "I/Pattern: findViewById=true" \
    "I/Pattern: decorView=true" \
    "I/Pattern: PatternView.onDraw 400x300" \
    "I/ViewRootImpl: frame limit reached: 3" \
    "I/Bootstrap: main loop finished after 3 frames"
do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

drawn=$(printf '%s' "$output" | grep -cF "I/Pattern: PatternView.onDraw 400x300")
if [ "$drawn" -ne 3 ]; then
    echo "gate FAILED: onDraw ran $drawn times, expected 3" >&2
    exit 1
fi

if [ ! -s "$work/frame.raw" ]; then
    echo "gate FAILED: no frame was dumped" >&2
    exit 1
fi

if ! cmp -s "$work/frame.raw" "$golden"; then
    echo "gate FAILED: the guest-drawn frame differs from $golden" >&2
    cmp "$work/frame.raw" "$golden" >&2
    exit 1
fi

echo "gate PASSED: the APK's Activity drew three frames and the pixels match the golden"

#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
golden="${2:-$PROJ_DIR/tests/window/frame-golden.raw}"

smokeTest="$buildDir/src/WindowSmokeTest"

if [ ! -e "$smokeTest" ]; then
    echo "gate FAILED: $smokeTest is missing - it is an output of the build under test" >&2
    exit 1
fi

if [ ! -e "$golden" ]; then
    echo "gate FAILED: golden frame missing: $golden" >&2
    exit 1
fi

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
    socket="acl-gate-$$"
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
elif [ -n "${WAYLAND_DISPLAY:-}" ]; then
    echo "note: no weston, falling back to the session compositor $WAYLAND_DISPLAY" >&2
else
    echo "gate SKIPPED: no weston and no WAYLAND_DISPLAY" >&2
    exit 77
fi

output=$(timeout 60 "$smokeTest" 3 "$work/frame.raw" 2>&1)
status=$?

echo "$output"

if [ "$status" -eq 77 ]; then
    echo "gate SKIPPED: no compositor reachable" >&2
    exit 77
fi

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: window smoke test exited $status" >&2
    exit 1
fi

for expected in "surface created 320x240 configured=1" "presented frame 2" "window smoke test finished"; do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

if ! printf '%s' "$output" | grep -qE '^connected: .*presentation=0x[0-9a-f]+'; then
    echo "gate FAILED: wp_presentation was not bound" >&2
    exit 1
fi

if [ ! -s "$work/frame.raw" ]; then
    echo "gate FAILED: no frame was dumped" >&2
    exit 1
fi

if ! cmp -s "$work/frame.raw" "$golden"; then
    echo "gate FAILED: presented frame differs from $golden" >&2
    cmp "$work/frame.raw" "$golden" >&2
    exit 1
fi

echo "gate PASSED: a real compositor accepted three frames and the pixels match the golden"

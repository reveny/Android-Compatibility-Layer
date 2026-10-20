#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/graphics/graphics.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

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
    socket="acl-interaction-$$"
    weston --backend=headless --socket="$socket" --width=640 --height=480 --idle-time=0 \
        > "$work/weston.log" 2>&1 &
    westonPid=$!

    for _ in $(seq 1 50); do
        [ -S "$XDG_RUNTIME_DIR/$socket" ] && break
        sleep 0.1
    done

    if [ -S "$XDG_RUNTIME_DIR/$socket" ]; then
        export WAYLAND_DISPLAY="$socket"
    elif [ -z "${WAYLAND_DISPLAY:-}" ]; then
        echo "gate SKIPPED: weston failed and there is no session compositor" >&2
        exit 77
    fi
elif [ -z "${WAYLAND_DISPLAY:-}" ]; then
    echo "gate SKIPPED: no weston and no WAYLAND_DISPLAY" >&2
    exit 77
fi

output=$(timeout 120 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/data" \
    --activity com.example.graphics.InteractionActivity \
    --no-continuous-frames \
    --max-frames 400 \
    --window 400x300 \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

for expected in \
    "I/Interaction: InteractionActivity.onCreate package=com.example.graphics" \
    "I/Interaction: draws before any input: 3" \
    "I/Interaction: first target touched=true focused=true" \
    "I/Interaction: second target focused=true first still focused=false" \
    "I/Interaction: the panel saw 2 pointers at once" \
    "I/Interaction: the panel saw a cancel=true" \
    "I/Interaction: the field is a text editor=true focused=true" \
    "I/Interaction: input started for the field=true" \
    "I/Interaction: the field now reads 'hi 5'" \
    "I/Interaction: after a delete it reads 'hi '" \
    "I/Interaction: moving focus away left the field focused=false" \
    "I/Interaction: the animation started running=true" \
    "I/Interaction: the animation stepped more than once=true and finished at 100" \
    "I/Interaction: the animation is still running=false" \
    "I/Interaction: an invalidate produced 1 more draw" \
    "I/Interaction: final draw count 4" \
    "I/ViewRootImpl: nothing is dirty"
do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

if printf '%s' "$output" | grep -qF "frame limit reached"; then
    echo "gate FAILED: the loop kept drawing until the frame limit rather than going idle" >&2
    exit 1
fi

echo "gate PASSED: drawing follows invalidation, touches reach a view, focus moves, and the loop idles"

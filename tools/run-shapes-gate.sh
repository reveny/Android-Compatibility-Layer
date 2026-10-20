#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/graphics/graphics.apk}"
golden="${3:-$PROJ_DIR/tests/graphics/shapes-golden.raw}"

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
    socket="acl-shapes-$$"
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
    --max-frames 2 \
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
    "I/Shapes: ShapesActivity.onCreate package=com.example.graphics" \
    "I/Shapes: ShapesView.onDraw 400x300" \
    "I/Shapes: layer balanced=true" \
    "I/Shapes: saveCount balanced=true" \
    "I/Shapes: renderNode recorded=true" \
    "I/Shapes: renderNode identity=false mapped=transformed" \
    "I/Shapes: shadow and dashes drawn" \
    "I/Shapes: bitmap made 8x8 alpha=true" \
    "I/Shapes: bitmap scaled to 32x32" \
    "I/Shapes: png decoded=true" \
    "I/Shapes: png size 24x24 alpha=true" \
    "I/Shapes: png measured 24x24 type=image/png"
do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

if [ ! -s "$work/frame.raw" ]; then
    echo "gate FAILED: no frame was dumped" >&2
    exit 1
fi

if [ "${RUNTIME_REGENERATE_GOLDEN:-0}" = "1" ]; then
    cp "$work/frame.raw" "$golden"
    echo "gate NOTE: regenerated $golden"
    exit 0
fi

if [ ! -e "$golden" ]; then
    echo "gate FAILED: $golden is missing; run with RUNTIME_REGENERATE_GOLDEN=1 to record it" >&2
    exit 1
fi

if ! cmp -s "$work/frame.raw" "$golden"; then
    echo "gate FAILED: the drawn frame differs from $golden" >&2
    cmp "$work/frame.raw" "$golden" >&2
    exit 1
fi

echo "gate PASSED: paths, gradients, a clip, a layer and a stroke reached the screen and match the golden"

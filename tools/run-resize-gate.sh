#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

resizeTest="$buildDir/src/WindowResizeTest"

if [ ! -e "$resizeTest" ]; then
    echo "gate FAILED: $resizeTest is missing - it is an output of the build under test" >&2
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
    socket="acl-resize-$$"
    weston --backend=headless --socket="$socket" --width=640 --height=480 --idle-time=0 \
        > "$work/weston.log" 2>&1 &
    westonPid=$!

    for _ in $(seq 1 50); do
        [ -S "$XDG_RUNTIME_DIR/$socket" ] && break
        sleep 0.1
    done

    if [ ! -S "$XDG_RUNTIME_DIR/$socket" ]; then
        echo "gate SKIPPED: headless weston never came up" >&2
        exit 77
    fi

    export WAYLAND_DISPLAY="$socket"
elif [ -z "${WAYLAND_DISPLAY:-}" ]; then
    echo "gate SKIPPED: no weston to start and no compositor in the environment" >&2
    exit 77
fi

output=$("$resizeTest" 2>&1)
status=$?

printf '%s\n' "$output"

if [ "$status" -eq 77 ]; then
    echo "gate SKIPPED: the resize test could not reach a compositor" >&2
    exit 77
fi

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the shm pool did not survive being resized" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -qF "window-resize PASSED"; then
    echo "gate FAILED: the resize test did not report a pass" >&2
    exit 1
fi

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
apk="$PROJ_DIR/tests/app-debug.apk"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

if [ ! -d "$hostRoot" ] || [ ! -f "$frameworkRes" ] || [ ! -e "$launcher" ] || [ ! -e "$apk" ]; then
    echo "gate PASSED (pool only): the shm pool is reallocated on resize; no host ART or fixture for the whole-chain phase"
    exit 0
fi

dataRoot="$work/data"
mkdir -p "$dataRoot"

appOutput=$(timeout 120 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --max-frames 40 \
    --fullscreen "$apk" 2>&1)

printf '%s\n' "$appOutput" | grep -E "window is now|laid out again|configuration changed|whole output"

appContains()
{
    printf '%s' "$appOutput" | grep -qF "$1"
}

if ! appContains "asked the compositor for the whole output"; then
    echo "gate FAILED: --fullscreen never reached the compositor" >&2
    exit 1
fi

if ! appContains "the window is now"; then
    echo "gate FAILED: the compositor configured a size and the buffers never followed" >&2
    exit 1
fi

if ! appContains "so everything is laid out again"; then
    echo "gate FAILED: the window resized and the view tree was never laid out again" >&2
    exit 1
fi

if ! appContains "the configuration changed"; then
    echo "gate FAILED: the window resized and no configuration change was delivered" >&2
    exit 1
fi

echo "gate PASSED: the shm pool is reallocated on resize, and a compositor configure reaches layout and the configuration"

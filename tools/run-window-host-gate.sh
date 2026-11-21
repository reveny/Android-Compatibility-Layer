#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
windowApk="${2:-$buildDir/tests/window/window.apk}"
graphicsApk="${3:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ ! -f "$frameworkRes" ]; then
    echo "gate SKIPPED: no SDK platform android.jar at '$frameworkRes'" >&2
    exit 77
fi

if ! command -v weston >/dev/null; then
    echo "gate SKIPPED: no weston to host the window on" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$windowApk" "$graphicsApk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
if [ ! -d "$XDG_RUNTIME_DIR" ]; then
    echo "gate SKIPPED: no XDG_RUNTIME_DIR at '$XDG_RUNTIME_DIR'" >&2
    exit 77
fi

work="$(mktemp -d)"
socket="acl-host-gate-$$"
weston --backend=headless --socket="$socket" --width=1080 --height=2400 --idle-time=0 > "$work/weston.log" 2>&1 &
westonPid=$!

cleanup()
{
    kill "$westonPid" 2>/dev/null
    wait "$westonPid" 2>/dev/null
    rm -rf "$work"
}
trap cleanup EXIT

for _ in $(seq 1 50); do
    [ -S "$XDG_RUNTIME_DIR/$socket" ] && break
    sleep 0.1
done

if [ ! -S "$XDG_RUNTIME_DIR/$socket" ]; then
    echo "gate SKIPPED: headless weston did not come up" >&2
    exit 77
fi

run()
{
    local name="$1"
    shift

    timeout 180 env WAYLAND_DISPLAY="$socket" "$launcher" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --framework-res "$frameworkRes" \
        --data-root "$work/data-$name" \
        --jit --no-strict \
        --window 540x1200 \
        "$@" > "$work/$name.log" 2>&1
    echo $?
}

expect()
{
    local name="$1"
    local wanted="$2"
    local reason="$3"

    if ! grep -qF -- "$wanted" "$work/$name.log"; then
        cat "$work/$name.log"
        echo "gate FAILED: $reason" >&2
        exit 1
    fi
}

expectFramesReached()
{
    local name="$1"
    local api="$2"

    local reached
    reached=$(sed -n "s/^\[launcher\] the window host got \([0-9]*\) $api frame(s) as shared gpu [a-z]* and \([0-9]*\) as copies$/\1 \2/p" \
        "$work/$name.log" | tail -1)

    if [ -z "$reached" ]; then
        cat "$work/$name.log"
        echo "gate FAILED: $name: no $api frame reached the window host" >&2
        exit 1
    fi

    set -- $reached
    if [ $(($1 + $2)) -lt 1 ]; then
        echo "gate FAILED: $name: the window host got no $api frame" >&2
        exit 1
    fi

    echo "$name: the window host got $1 shared and $2 copied $api frame(s)"
}

status=$(run window --max-frames 3 "$windowApk")
expect window "[hostedwindow] hosted by a shell" "the framework window was not handed to the window host"
expect window "the app is on screen after" "the framework window never reached the screen"
if [ "$status" -ne 0 ]; then
    cat "$work/window.log"
    echo "gate FAILED: the framework window run exited $status, so the window host did not pass the app's status through" >&2
    exit 1
fi

for mode in shared copied; do
    extra=()
    [ "$mode" = copied ] && extra=(--setting window-host-dmabuf=0)

    run "gles-$mode" "${extra[@]}" --activity com.reveny.helloworld.GlesActivity --guest-env GLES_FRAMES=30 "$graphicsApk" > /dev/null
    expect "gles-$mode" "the colour it cleared to is the colour it read back" "gles-$mode: the guest did not read back what it drew"
    expectFramesReached "gles-$mode" "OpenGL ES"

    run "vulkan-$mode" "${extra[@]}" --activity android.app.NativeActivity --guest-env TRIANGLE_FRAMES=30 "$graphicsApk" > /dev/null
    expect "vulkan-$mode" "[guest triangle] pipeline created" "vulkan-$mode: the guest Vulkan pipeline was never created"
    expectFramesReached "vulkan-$mode" "Vulkan"
done

if grep -q "as shared gpu" "$work/gles-copied.log" && ! grep -q "and 30 as copies" "$work/gles-copied.log"; then
    echo "gate FAILED: window-host-dmabuf=0 did not make OpenGL ES frames go through copies" >&2
    exit 1
fi

echo "gate PASSED: framework, OpenGL ES and Vulkan frames all reach the window host, shared or copied"

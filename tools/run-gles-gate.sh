#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

activityClass="com.reveny.helloworld.GlesActivity"
frames=30

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

if [ -z "${WAYLAND_DISPLAY:-}" ] && [ -z "${WAYLAND_SOCKET:-}" ]; then
    echo "gate SKIPPED: no Wayland compositor in the environment, so nothing can be presented to" >&2
    exit 77
fi

if ! ldconfig -p 2>/dev/null | grep -q "libEGL\.so\.1"; then
    echo "gate SKIPPED: no host libEGL.so.1, so there is no driver to forward to" >&2
    exit 77
fi

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

output=$(timeout 180 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --activity "$activityClass" \
    --guest-env "GLES_FRAMES=$frames" \
    "$apk" 2>&1)

printf '%s\n' "$output"

contains()
{
    printf '%s' "$output" | grep -qF "$1"
}

if ! contains "OpenGL ES entry points forwarded to the host driver"; then
    echo "gate FAILED: no entry points were forwarded, so the guest had no driver to call" >&2
    exit 1
fi

if ! contains "ignoring the window it passed in"; then
    echo "gate FAILED: no window surface was made on the compositor surface" >&2
    exit 1
fi

if contains "gles FAILED"; then
    echo "gate FAILED: the guest reported a failure of its own" >&2
    exit 1
fi

if ! contains "the colour it cleared to is the colour it read back"; then
    echo "gate FAILED: glReadPixels did not return the colour the guest cleared to, so nothing was really drawn" >&2
    exit 1
fi

hostFrames=$(printf '%s' "$output" | sed -n 's/^\[launcher\] the host driver swapped \([0-9]*\) guest frame(s).*$/\1/p' | tail -1)
guestFrames=$(printf '%s' "$output" | sed -n 's/^.*gles: presented \([0-9]*\) frame(s).*$/\1/p' | tail -1)

if [ -z "$hostFrames" ]; then
    echo "gate FAILED: the host driver reported no swap count at all" >&2
    exit 1
fi

if [ "$hostFrames" -lt "$frames" ]; then
    echo "gate FAILED: the host driver swapped $hostFrames frame(s), fewer than the $frames asked for" >&2
    exit 1
fi

if [ "$guestFrames" != "$hostFrames" ]; then
    echo "gate FAILED: the guest counted $guestFrames frame(s) but the host swapped $hostFrames" >&2
    exit 1
fi

if ! contains "renderer ACL Graphics"; then
    echo "gate FAILED: the renderer string the guest read was not the one this runtime reports" >&2
    exit 1
fi

if contains "unimplemented symbol"; then
    echo "gate FAILED: the guest called a symbol this runtime does not have" >&2
    exit 1
fi

if contains "without a known prototype"; then
    echo "gate FAILED: an entry point was forwarded with a guessed signature, which misreads floating point arguments" >&2
    exit 1
fi

blocks=$(printf '%s' "$output" | sed -n 's/^\[launcher\] guest blocks: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps hit$/\1 \2 \3 \4/p' | tail -1)
if [ -z "$blocks" ]; then
    echo "gate FAILED: the launcher reported no translation summary" >&2
    exit 1
fi

set -- $blocks
totalBlocks="$1"
translatedBlocks="$2"
interpretedBlocks="$3"
trapsHit="$4"

if [ "$totalBlocks" -lt 1 ] || [ "$translatedBlocks" -lt 1 ]; then
    echo "gate FAILED: no guest code was translated, so nothing arm64 ran" >&2
    exit 1
fi

if [ "$interpretedBlocks" -ne 0 ]; then
    echo "gate FAILED: $interpretedBlocks block(s) fell back to the interpreter" >&2
    exit 1
fi

if [ "$trapsHit" -ne 0 ]; then
    echo "gate FAILED: $trapsHit trap(s) were hit in translated code" >&2
    exit 1
fi

echo "gate PASSED: the guest cleared and read back its own colour and swapped $hostFrames frames through the host OpenGL ES driver in $translatedBlocks translated block(s), 0 interpreted"

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

if [ ! -e "/usr/lib/libvulkan.so.1" ] && [ ! -e "/usr/lib64/libvulkan.so.1" ] && ! ldconfig -p 2>/dev/null | grep -q "libvulkan.so.1"; then
    echo "gate SKIPPED: no host Vulkan loader, so the guest's calls have nowhere to go" >&2
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
    --guest-env "TRIANGLE_FRAMES=$frames" \
    "$apk" 2>&1)

printf '%s' "$output" | grep -vE 'presented frame [0-9]+$'

contains()
{
    printf '%s' "$2" | grep -F -- "$1" > /dev/null
}

if ! contains "[vulkanwsi] the guest asked for VK_KHR_android_surface; the host is being asked for VK_KHR_wayland_surface" "$output"; then
    echo "gate FAILED: the surface extension was not translated, so the guest asked the host for an Android extension" >&2
    exit 1
fi

if ! contains "[vulkanwsi] the guest window surface is a real compositor surface" "$output"; then
    echo "gate FAILED: vkCreateAndroidSurfaceKHR was not translated into a real compositor surface" >&2
    exit 1
fi

if ! contains "[guest triangle] pipeline created" "$output"; then
    echo "gate FAILED: the guest never built a graphics pipeline, so nothing was rasterised" >&2
    exit 1
fi

presented=$(printf '%s' "$output" | grep -cE '^\[guest triangle\] presented frame [0-9]+$')
if [ "$presented" -ne "$frames" ]; then
    echo "gate FAILED: the guest presented $presented frames, not $frames" >&2
    exit 1
fi

hostPresented=$(printf '%s' "$output" | sed -n 's/.*the host driver presented \([0-9]*\) guest frame.*/\1/p' | head -1)
if [ -z "$hostPresented" ]; then
    echo "gate FAILED: the host side did not report how many frames it presented" >&2
    exit 1
fi

if [ "$hostPresented" -ne "$frames" ]; then
    echo "gate FAILED: the guest claims $frames frames but the host driver presented $hostPresented - the guest's own count is not evidence" >&2
    exit 1
fi

if ! contains "[guest triangle] presented frame $((frames - 1))" "$output"; then
    echo "gate FAILED: the guest did not reach the last frame it was asked for" >&2
    exit 1
fi

if ! contains "[guest triangle] triangle finished" "$output"; then
    echo "gate FAILED: the guest did not finish cleanly" >&2
    exit 1
fi

if ! contains "I/Bootstrap: the guest finished presenting" "$output"; then
    echo "gate FAILED: the framework did not observe the guest finishing; it timed out or the window closed first" >&2
    exit 1
fi

for callback in onNativeWindowDestroyed onPause onStop onDestroy; do
    if ! contains "[nativeactivity] $callback" "$output"; then
        echo "gate FAILED: $callback never reached the guest, so it could not tear down" >&2
        exit 1
    fi
done

blocks=$(printf '%s' "$output" | grep -o '\[launcher\] guest blocks: .*' | head -1)
if [ -z "$blocks" ]; then
    echo "gate FAILED: the launcher did not report how the guest ran" >&2
    exit 1
fi

translated=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\2/p')
interpreted=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\3/p')
traps=$(printf '%s' "$blocks" | sed -n 's/.*: \([0-9]*\) total, \([0-9]*\) translated, \([0-9]*\) interpreted, \([0-9]*\) traps.*/\4/p')

if [ -z "$translated" ] || [ "$translated" -lt 1 ]; then
    echo "gate FAILED: nothing was translated, so the guest cannot have run as translated arm64" >&2
    exit 1
fi

if [ "$interpreted" != "0" ]; then
    echo "gate FAILED: $interpreted block(s) were interpreted - this milestone claims translated arm64, not interpretation" >&2
    exit 1
fi

if [ "$traps" != "0" ]; then
    echo "gate FAILED: $traps trap(s) were hit, so the guest ran into something we cannot translate" >&2
    exit 1
fi

if contains "unimplemented symbol" "$output"; then
    echo "gate FAILED: the guest called a symbol we do not implement" >&2
    exit 1
fi

if contains "guest allocation callbacks" "$output"; then
    echo "gate FAILED: the guest supplied Vulkan allocation callbacks, which this path refuses rather than mishandles" >&2
    exit 1
fi

echo "gate PASSED: the guest presented $frames frames through the host Vulkan driver in $translated translated block(s), 0 interpreted"

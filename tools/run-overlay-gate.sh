#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

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

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

width=2400

pixelAt()
{
    local file="$1"
    local x="$2"
    local y="$3"
    local offset=$(((y * width + x) * 4))

    od -An -tu1 -j "$offset" -N 4 "$file" | tr -s ' ' | sed 's/^ //;s/ $//'
}

insideY=15
aboveY=2

rowBytes()
{
    local file="$1"
    local y="$2"
    local offset=$(((y * width) * 4))

    od -An -tu1 -j "$offset" -N $((300 * 4)) "$file" | tr -s ' \n' ' '
}

checkPanel()
{
    local file="$1"
    local what="$2"
    local background="$3"
    local textPixel="$4"

    if [ ! -s "$file" ]; then
        echo "gate FAILED: $what wrote no frame to $file" >&2
        exit 1
    fi

    local inside
    inside="$(pixelAt "$file" 8 "$insideY")"

    if [ "$inside" != "$background" ]; then
        echo "gate FAILED: $what has no counter panel at 8,$insideY - got '$inside', expected '$background'" >&2
        exit 1
    fi

    local found=0
    local y
    for y in $(seq 12 1 30); do
        if rowBytes "$file" "$y" | grep -qF " $textPixel "; then
            found=1
            break
        fi
    done

    if [ "$found" -ne 1 ]; then
        echo "gate FAILED: $what drew a panel with no glyph pixels in it, so the text never rendered" >&2
        exit 1
    fi

    echo "note: $what has the panel at 8,$insideY and inked glyph pixels on row $y"
}

echo "=== the framework path: the counter is drawn over the view tree"

"$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/framework" \
    --max-frames 150 \
    --frame-dump "$work/framework.raw" \
    --landscape --fps "$apk" > "$work/framework.log" 2>&1

if [ ! -s "$work/framework.raw" ]; then
    echo "gate FAILED: the framework run produced no frame" >&2
    tail -20 "$work/framework.log" >&2
    exit 1
fi

checkPanel "$work/framework.raw" "the framework frame" "56 53 54 255" "244 240 236 255"

above="$(pixelAt "$work/framework.raw" 8 "$aboveY")"
if [ "$above" = "56 53 54 255" ]; then
    echo "gate FAILED: the panel reaches above its own margin, so it is not positioned where it claims" >&2
    exit 1
fi

echo "=== the same app without --fps: nothing is drawn over it"

"$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/plain" \
    --max-frames 150 \
    --frame-dump "$work/plain.raw" \
    --landscape "$apk" > "$work/plain.log" 2>&1

if [ ! -s "$work/plain.raw" ]; then
    echo "gate FAILED: the run without --fps produced no frame" >&2
    exit 1
fi

plainInside="$(pixelAt "$work/plain.raw" 8 "$insideY")"
if [ "$plainInside" = "56 53 54 255" ]; then
    echo "gate FAILED: the counter was drawn without --fps being asked for" >&2
    exit 1
fi

echo "note: without --fps that pixel is '$plainInside', the app's own"

echo "=== the guest Vulkan path: the counter is copied into the guest's own swapchain image"

"$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/guest" \
    --activity android.app.NativeActivity \
    --guest-frame-dump "$work/guest.raw" \
    --landscape --fps "$apk" > "$work/guest.log" 2>&1

if ! grep -qF "the frame counter draws into the guest's own frames" "$work/guest.log"; then
    echo "gate SKIPPED: this driver would not take the counter into the guest's frames" >&2
    grep -i overlay "$work/guest.log" >&2
    exit 77
fi

if [ ! -s "$work/guest.raw" ]; then
    echo "gate FAILED: the guest run captured no frame" >&2
    tail -20 "$work/guest.log" >&2
    exit 1
fi

checkPanel "$work/guest.raw" "the guest frame" "12 12 14 255" "236 240 244 255"

guestAbove="$(pixelAt "$work/guest.raw" 8 "$aboveY")"
if [ "$guestAbove" = "12 12 14 255" ]; then
    echo "gate FAILED: the panel overwrote the guest's frame above its own margin" >&2
    exit 1
fi

echo "note: above the panel the guest's own pixels are '$guestAbove', so its frame survived the copy"

echo "gate PASSED: --fps draws a real counter into the framework's frames and into the guest's own Vulkan frames"

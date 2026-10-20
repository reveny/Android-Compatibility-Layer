#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/viewtransform/viewtransform.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

width=300
height=240

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$apk"; do
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
westonPid=""

cleanup()
{
    if [ -n "$westonPid" ]; then kill "$westonPid" 2>/dev/null; wait "$westonPid" 2>/dev/null; fi
    rm -rf "$work"
}
trap cleanup EXIT

if command -v weston >/dev/null; then
    socket="acl-transform-$$"
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

frame="$work/frame.raw"

output=$(timeout 180 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/data" \
    --max-frames 2 \
    --window "${width}x${height}" \
    --frame-dump "$frame" \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: acl exited $status" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -qF "transform fixture laid out"; then
    echo "gate FAILED: the fixture never finished laying out" >&2
    exit 1
fi

if [ ! -s "$frame" ]; then
    echo "gate FAILED: no frame was dumped" >&2
    exit 1
fi

pixelAt()
{
    local x="$1"
    local y="$2"
    local offset=$(((y * width + x) * 4))

    od -An -tu1 -j "$offset" -N 4 "$frame" | tr -s ' ' | sed 's/^ //;s/ $//'
}

blueOf()
{
    printf '%s' "$1" | cut -d' ' -f1
}

greenOf()
{
    printf '%s' "$1" | cut -d' ' -f2
}

redOf()
{
    printf '%s' "$1" | cut -d' ' -f3
}

failures=0

expectNear()
{
    local what="$1"
    local actual="$2"
    local wanted="$3"
    local slack="$4"

    local low=$((wanted - slack))
    local high=$((wanted + slack))

    if [ "$actual" -lt "$low" ] || [ "$actual" -gt "$high" ]; then
        echo "gate FAILED: $what is $actual, expected $wanted +/- $slack" >&2
        failures=$((failures + 1))
        return 1
    fi

    echo "note: $what is $actual, within $slack of $wanted"
    return 0
}

plain="$(pixelAt 40 40)"
echo "note: the plain box centre reads '$plain' as b g r a"

if [ "$(redOf "$plain")" -lt 200 ]; then
    echo "gate FAILED: the plain box did not draw at all, so nothing below is meaningful" >&2
    exit 1
fi

faded="$(pixelAt 110 40)"
fadedGreen="$(greenOf "$faded")"

expectNear "the 50% box's green channel" "$fadedGreen" 128 24

if [ "$fadedGreen" -gt 220 ]; then
    echo "note: a fully saturated green here means View.draw ignored mAlpha" >&2
fi

rotatedCorner="$(pixelAt 163 23)"
rotatedCentre="$(pixelAt 180 40)"

expectNear "the rotated box's centre blue channel" "$(blueOf "$rotatedCentre")" 255 40

cornerBlue="$(blueOf "$rotatedCorner")"
if [ "$cornerBlue" -gt 80 ]; then
    echo "gate FAILED: the rotated box still covers its unrotated corner at 163,23 (blue $cornerBlue), so the rotation never reached the canvas" >&2
    failures=$((failures + 1))
else
    echo "note: the unrotated corner at 163,23 is clear (blue $cornerBlue), so the box really turned"
fi

scaledOutside="$(pixelAt 14 110)"
scaledRed="$(redOf "$scaledOutside")"
scaledGreen="$(greenOf "$scaledOutside")"

if [ "$scaledRed" -lt 150 ] || [ "$scaledGreen" -lt 150 ]; then
    echo "gate FAILED: the scaled box does not reach 14,110 (r $scaledRed g $scaledGreen), so setScaleX/Y drew nothing" >&2
    failures=$((failures + 1))
else
    echo "note: the scaled box reaches 14,110 (r $scaledRed g $scaledGreen), outside its unscaled bounds"
fi

before="$(pixelAt 102 100)"
after="$(pixelAt 103 100)"

if [ "$(greenOf "$after")" -lt 200 ]; then
    echo "gate FAILED: the translated box is absent at 103,100, so setTranslationX/Y never moved it" >&2
    failures=$((failures + 1))
elif [ "$(greenOf "$before")" -gt 60 ]; then
    echo "gate FAILED: the translated box already inks 102,100, so the 12.5px offset was rounded down" >&2
    failures=$((failures + 1))
else
    echo "note: the 12.5px translation puts the edge between 102 and 103, so the half pixel survived the matrix"
fi

untouched="$(pixelAt 95 100)"
if [ "$(greenOf "$untouched")" -gt 60 ]; then
    echo "gate FAILED: the box still inks its untranslated position at 95,100" >&2
    failures=$((failures + 1))
fi

if [ "$failures" -ne 0 ]; then
    echo "gate FAILED: $failures transform(s) did not reach the pixels" >&2
    exit 1
fi

echo "gate PASSED: alpha, rotation and scale all reached the dumped frame"

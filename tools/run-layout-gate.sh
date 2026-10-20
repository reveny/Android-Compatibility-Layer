#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"
golden="${3:-$PROJ_DIR/tests/window/appdebug-golden.raw}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ -z "$frameworkRes" ]; then
    echo "gate FAILED: RUNTIME_FRAMEWORK_RES is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -e "$frameworkRes" ]; then
    echo "gate SKIPPED: no framework resources at '$frameworkRes'" >&2
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
    socket="acl-m2b-$$"
    weston --backend=headless --socket="$socket" --width=640 --height=960 --idle-time=0 > "$work/weston.log" 2>&1 &
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

output=$(timeout 180 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$work/data" \
    --max-frames 1 \
    --window 420x880 \
    --trace views,resources \
    --require-translated \
    --frame-dump "$work/frame.raw" \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^\[resources\] framework package: '; then
    echo "gate FAILED: framework resources were not loaded" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^\[resources\] 0x7f0f001b -> "androidx.startup"$'; then
    echo 'gate FAILED: getString(0x7f0f001b) did not resolve to "androidx.startup"' >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^I/Bootstrap: activity theme: 0x7f1002ae$'; then
    echo "gate FAILED: the manifest's application theme was not applied" >&2
    exit 1
fi

for expected in \
    "I/ViewRootImpl: android.view.DecorView id=0xffffffff bounds=0,0-420,880" \
    "androidx.appcompat.widget.FitWindowsLinearLayout id=0x7f080037 bounds=0,0-420,880" \
    "androidx.appcompat.widget.ContentFrameLayout id=0x1020002 bounds=0,0-420,880" \
    "androidx.constraintlayout.widget.ConstraintLayout id=0x7f0800ff bounds=0,0-420,880" \
    "androidx.appcompat.widget.AppCompatTextView id=0x7f08013f bounds=145,432-276,448"
do
    if ! printf '%s' "$output" | grep -qF "$expected"; then
        echo "gate FAILED: hierarchy missing '$expected'" >&2
        echo "the hierarchy it did report:" >&2
        printf '%s' "$output" | grep -F "I/ViewRootImpl:" >&2
        exit 1
    fi
done

if ! printf '%s' "$output" | grep -q '^I/Bootstrap: main loop finished after 1 frames$'; then
    echo "gate FAILED: no frame was drawn" >&2
    exit 1
fi

if [ ! -s "$work/frame.raw" ]; then
    echo "gate FAILED: no frame was dumped" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -qE '^\[launcher\] guest blocks: [0-9]+ total, [0-9]+ translated, 0 interpreted, 0 traps hit$'; then
    echo "gate FAILED: the guest library did not run fully ahead-of-time translated" >&2
    printf '%s' "$output" | grep -F "[launcher] guest blocks:" >&2
    exit 1
fi

guestBlocks=$(printf '%s' "$output" | sed -n 's/^\[launcher\] guest blocks: \([0-9]*\) total.*/\1/p')
if [ "${guestBlocks:-0}" -lt 10 ]; then
    echo "gate FAILED: only ${guestBlocks:-0} guest blocks ran, so the translation assertion above proves nothing" >&2
    exit 1
fi

if [ "${RUNTIME_REGENERATE_GOLDEN:-0}" = "1" ]; then
    cp "$work/frame.raw" "$golden"
    echo "regenerated $golden - review the diff, it should be confined to the nativeText run of pixels"
    exit 0
fi

if ! cmp -s "$work/frame.raw" "$golden"; then
    echo "gate FAILED: the rendered frame differs from $golden" >&2
    echo "note: this golden contains anti-aliased glyphs and is only reproducible against the" >&2
    echo "      FreeType and font recorded in tests/text/text-golden.raw.provenance" >&2
    cmp "$work/frame.raw" "$golden" >&2
    exit 1
fi

echo "gate PASSED: app-debug.apk inflated its ConstraintLayout and rendered it"

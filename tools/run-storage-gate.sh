#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

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

if [ ! -f "$frameworkRes" ]; then
    echo "gate SKIPPED: no framework resources at '$frameworkRes'" >&2
    exit 77
fi

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

matches()
{
    printf '%s' "$2" | grep -cE "$1" > /dev/null
}

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
    socket="acl-m3b-$$"
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

dataRoot="$work/data"
mkdir -p "$dataRoot"

output=$(timeout 180 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --max-frames 600 \
    --window 420x880 \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: acl exited $status" >&2
    exit 1
fi

for expected in \
    "I/M3bStorage: asset greeting.txt bytes=32 firstLine=hello from an asset" \
    "I/M3bStorage: missing asset threw FileNotFoundException as it should" \
    "I/M3bStorage: openFd payload.bin length=20 offsetIntoApk=true bytes=20" \
    "I/M3bStorage: openFd on a deflated asset threw FileNotFoundException as it should" \
    "I/M3bStorage: prefs committed=true" \
    "I/M3bStorage: prefs name=acl count=7 enabled=true stamp=1234567890123 ratio=0.5 tags=alpha|beta" \
    "I/M3bStorage: prefs contains(name)=true size=6" \
    "I/M3bStorage: file round trip matched=true bytes=22" \
    "I/M3bStorage: fileList count=1" \
    "I/M3bStorage: filesDir exists=true cacheDir exists=true filesNamedFiles=true" \
    "I/Bootstrap: main loop finished after 600 frames"
do
    if ! contains "$expected" "$output"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

if ! matches '^I/M3bStorage: asset list count=[0-9]+ names=.*greeting\.txt' "$output"; then
    echo "gate FAILED: the asset listing did not include the app's own assets" >&2
    exit 1
fi

if contains 'unimplemented' "$output"; then
    echo "gate FAILED: a strict guard fired on the storage path" >&2
    printf '%s' "$output" | grep 'unimplemented' >&2
    exit 1
fi

if matches 'Exception in thread|NoSuchMethodError|NoClassDefFoundError' "$output"; then
    echo "gate FAILED: a background thread threw - this run is long enough for androidx's delayed" >&2
    echo "             work to fire, which a one-frame run never reaches" >&2
    printf '%s' "$output" | grep -E 'Exception in thread|NoSuchMethodError|NoClassDefFoundError' >&2
    exit 1
fi

if ! contains 'D/ProfileInstaller: Installing profile' "$output"; then
    echo "gate FAILED: androidx.profileinstaller's delayed task never ran - the frame count is too" >&2
    echo "             low to exercise background work, so this gate would not catch a regression there" >&2
    exit 1
fi

prefsFile="$dataRoot/com.reveny.helloworld/shared_prefs/m3b_prefs.xml"
if [ ! -s "$prefsFile" ]; then
    echo "gate FAILED: no preferences file was written at $prefsFile" >&2
    exit 1
fi

if ! head -1 "$prefsFile" | grep -qF "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"; then
    echo "gate FAILED: the preferences file does not carry Android's XML declaration" >&2
    head -3 "$prefsFile" >&2
    exit 1
fi

for element in \
    '<int name="count" value="7" />' \
    '<boolean name="enabled" value="true" />' \
    '<string name="name">acl</string>' \
    '<float name="ratio" value="0.5" />' \
    '<long name="stamp" value="1234567890123" />' \
    '<set name="tags">'
do
    if ! grep -qF "$element" "$prefsFile"; then
        echo "gate FAILED: the preferences file is missing '$element'" >&2
        cat "$prefsFile" >&2
        exit 1
    fi
done

deviceReference="$PROJ_DIR/tests/m3b-prefs-device.xml"
if [ ! -s "$deviceReference" ]; then
    echo "gate FAILED: the device preferences reference is missing or empty: $deviceReference" >&2
    echo "             re-capture it with tools/capture-prefs-format.sh --regenerate against a device" >&2
    exit 1
fi

if ! diff -u <(sort "$deviceReference") <(sort "$prefsFile") > "$work/prefs.diff"; then
    echo "gate FAILED: our preferences file differs from the device reference" >&2
    echo "             (compared order-insensitively; we sort for determinism, Android does not)" >&2
    cat "$work/prefs.diff" >&2
    exit 1
fi

for directory in files cache shared_prefs; do
    if [ ! -d "$dataRoot/com.reveny.helloworld/$directory" ]; then
        echo "gate FAILED: $directory was not created under the package data root" >&2
        exit 1
    fi
done

if [ ! -s "$dataRoot/com.reveny.helloworld/files/m3b_file.txt" ]; then
    echo "gate FAILED: the fixture's file was not written" >&2
    exit 1
fi

echo "gate PASSED: app-debug.apk read an asset, round-tripped a preference and a file, and rendered"

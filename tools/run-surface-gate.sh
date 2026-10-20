#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
CLANG="$SDK_DIR/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed, so no guest library can be built" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar"; do
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
    socket="acl-surface-$$"
    weston --backend=headless --socket="$socket" --width=640 --height=480 --idle-time=0 \
        > "$work/weston.log" 2>&1 &
    westonPid=$!

    for _ in $(seq 1 50); do
        [ -S "$XDG_RUNTIME_DIR/$socket" ] && break
        sleep 0.1
    done

    if [ ! -S "$XDG_RUNTIME_DIR/$socket" ]; then
        echo "gate SKIPPED: headless weston did not come up" >&2
        exit 77
    fi

    export WAYLAND_DISPLAY="$socket"
elif [ -z "${WAYLAND_DISPLAY:-}" ]; then
    echo "gate SKIPPED: no weston and no WAYLAND_DISPLAY" >&2
    exit 77
fi

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

mkdir -p "$work/stage/arm64-v8a"
printf 'int SurfacePresence(int a) { return a + 1; }\n' > "$work/presence.c"

if ! "$CLANG" -O2 -fPIC -shared -Wl,-soname,libpresence.so -o "$work/stage/arm64-v8a/libpresence.so" "$work/presence.c"; then
    echo "gate FAILED: the guest presence library did not compile" >&2
    exit 1
fi

apk="$work/surface.apk"
if ! env FIXTURE_NATIVE_LIBS="$work/stage" "$PROJ_DIR/tools/build-fixture-apk.sh" "$PROJ_DIR/tests/surface" "$apk" > "$work/apk.log" 2>&1; then
    echo "gate FAILED: the surface fixture apk did not build" >&2
    tail -20 "$work/apk.log" >&2
    exit 1
fi

output=$(timeout 180 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/data" \
    --landscape "$apk" 2>&1)
status=$?

printf '%s\n' "$output" | grep -E "SurfaceProbe|ViewRootImpl|Bootstrap: entering" || true

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the launcher exited $status" >&2
    printf '%s\n' "$output" | tail -25 >&2
    exit 1
fi

if ! contains "I/Bootstrap: entering the main loop, frameLimit=0" "$output"; then
    echo "gate FAILED: this gate is only meaningful with no frame limit, and one was set" >&2
    exit 1
fi

for expected in \
    "I/SurfaceProbe: the content view is a SurfaceView and the callback is registered" \
    "I/SurfaceProbe: surfaceCreated" \
    "I/SurfaceProbe: surfaceChanged"
do
    if ! contains "$expected" "$output"; then
        echo "gate FAILED: missing '$expected'" >&2
        echo "             a SurfaceView that never receives its surface is a black screen for any app" >&2
        echo "             that draws its own frames, which is every game" >&2
        exit 1
    fi
done

echo "note: the surface arrived with frameLimit=0, so it did not depend on continuous layout"

echo "gate PASSED: a SurfaceView receives its surface without a frame limit"

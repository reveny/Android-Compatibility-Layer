#!/usr/bin/env bash
set -euo pipefail

AOSP_DIR="${AOSP_DIR:-$HOME/aosp/master-art}"
PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${BUILD_DIR:-$PROJ_DIR/build}"
HOST="$AOSP_DIR/out/host/linux-x86"
FW="$HOST/framework"
PRELOAD="$BUILD_DIR/tools/icu-preload/libicu_preload.so"

dex="${1:-}"
mainClass="${2:-}"
if [ -z "$dex" ] || [ -z "$mainClass" ]; then
    echo "usage: $(basename "$0") <dex> <MainClass> [args...]" >&2
    exit 2
fi
shift 2

[ -f "$dex" ] || { echo "error: dex not found: $dex" >&2; exit 1; }
[ -x "$HOST/bin/dalvikvm64" ] || { echo "error: no dalvikvm64 - run tools/build-art-host.sh" >&2; exit 1; }
[ -f "$PRELOAD" ] || { echo "error: ICU shim missing - run: cmake -S $PROJ_DIR -B $BUILD_DIR -G Ninja && cmake --build $BUILD_DIR" >&2; exit 1; }
[ -d "$HOST/com.android.i18n/etc/icu" ] || { echo "error: no ICU data - run tools/extract-icu-data.sh" >&2; exit 1; }

bcp=""
for jar in core-oj core-libart okhttp bouncycastle apache-xml conscrypt core-icu4j; do
    [ -f "$FW/$jar-hostdex.jar" ] || { echo "error: missing boot jar $jar-hostdex.jar - run tools/build-art-host.sh" >&2; exit 1; }
    bcp="${bcp:+$bcp:}$FW/$jar-hostdex.jar"
done

preload="$PRELOAD"
[ -n "${LD_PRELOAD:-}" ] && preload="$PRELOAD:$LD_PRELOAD"

exec env \
    ANDROID_ROOT="$HOST" \
    ANDROID_ART_ROOT="$HOST" \
    ANDROID_I18N_ROOT="$HOST/com.android.i18n" \
    ANDROID_TZDATA_ROOT="$HOST/com.android.tzdata" \
    LD_LIBRARY_PATH="$HOST/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" \
    LD_PRELOAD="$preload" \
    "$HOST/bin/dalvikvm64" \
        -Xbootclasspath:"$bcp" \
        -Xnoimage-dex2oat \
        -Xusejit:false \
        -cp "$dex" \
        "$mainClass" "$@"

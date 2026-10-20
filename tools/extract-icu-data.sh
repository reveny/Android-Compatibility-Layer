#!/usr/bin/env bash
set -euo pipefail

AOSP_DIR="${AOSP_DIR:-$HOME/aosp/master-art}"
APEX="$AOSP_DIR/prebuilts/runtime/mainline/i18n/apex/com.android.i18n-x86_64.apex"
HOST_LIB="$AOSP_DIR/out/host/linux-x86/lib64/libicuuc-host.so"
I18N_ROOT="$AOSP_DIR/out/host/linux-x86/com.android.i18n"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

case "$AOSP_DIR" in
    *[[:space:]]*) echo "error: AOSP_DIR contains whitespace; debugfs -R cannot be quoted safely" >&2; exit 1 ;;
esac

for tool in debugfs strings python3 od; do
    command -v "$tool" >/dev/null || { echo "error: $tool not found" >&2; exit 1; }
done
[ -f "$APEX" ] || { echo "error: apex not found at $APEX" >&2; exit 1; }
[ -f "$HOST_LIB" ] || { echo "error: $HOST_LIB not found - run tools/build-art-host.sh first" >&2; exit 1; }

python3 -c '
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
open(sys.argv[2], "wb").write(z.read("apex_payload.img"))
' "$APEX" "$WORK/payload.img"

dat=$(debugfs -R "ls -l /etc/icu" "$WORK/payload.img" 2>/dev/null | grep -oE "icudt[0-9]+[lbe]\.dat" | head -1 || true)
[ -n "$dat" ] || { echo "error: no icudt*.dat inside $APEX" >&2; exit 1; }

expected=$(strings "$HOST_LIB" | grep -oE "icudt[0-9]+[lbe]" | sort -u || true)
[ -n "$expected" ] || { echo "error: $HOST_LIB references no ICU data version" >&2; exit 1; }

expectedCount=$(printf '%s\n' "$expected" | grep -c . || true)
if [ "$expectedCount" -ne 1 ]; then
    echo "error: $HOST_LIB references $expectedCount ICU data versions: $(printf '%s' "$expected" | tr '\n' ' ')" >&2
    exit 1
fi

echo "apex provides : $dat"
echo "host lib wants: $expected"
[ "${dat%.dat}" = "$expected" ] || { echo "error: ICU data version mismatch" >&2; exit 1; }

mkdir -p "$I18N_ROOT/etc/icu"
dest="$I18N_ROOT/etc/icu/$dat"
debugfs -R "dump /etc/icu/$dat $dest" "$WORK/payload.img" 2>/dev/null || true

[ -s "$dest" ] || { echo "error: extraction produced an empty $dest" >&2; rm -f "$dest"; exit 1; }

magic=$(head -c 16 "$dest" | od -An -c | tr -s ' ' | grep -o "C m n D" || true)
[ -n "$magic" ] || { echo "error: $dest is not ICU common data (missing CmnD magic)" >&2; rm -f "$dest"; exit 1; }

ls -l "$dest"
echo "ANDROID_I18N_ROOT=$I18N_ROOT"

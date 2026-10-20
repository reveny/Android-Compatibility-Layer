#!/usr/bin/env bash
set -euo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROBE_DIR="$PROJ_DIR/tests/probe"
SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
OUTPUT_DEX="${OUTPUT_DEX:-$PROBE_DIR/probe.dex}"

D8="$(ls -d "$SDK_DIR"/build-tools/*/d8 2>/dev/null | sort -V | tail -1)"
if [ -z "$D8" ]; then
    echo "error: d8 not found under $SDK_DIR/build-tools" >&2
    exit 1
fi
echo "using $D8"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

mkdir -p "$WORK_DIR/classes" "$WORK_DIR/dex"

javac --release 11 -d "$WORK_DIR/classes" "$PROBE_DIR/src/Probe.java"

mapfile -t CLASS_FILES < <(find "$WORK_DIR/classes" -name '*.class')

"$D8" --release --min-api 21 \
    --output "$WORK_DIR/dex" \
    "${CLASS_FILES[@]}"

mkdir -p "$(dirname "$OUTPUT_DEX")"
mv -f "$WORK_DIR/dex/classes.dex" "$OUTPUT_DEX"
ls -l "$OUTPUT_DEX"

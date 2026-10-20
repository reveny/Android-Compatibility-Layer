#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$PROJ_DIR/tests/guest/src"
HISTOGRAM="$PROJ_DIR/tests/guest/histogram-arm64.txt"

SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
SKIP_EXIT_CODE=77

mode=""
case "${1:-}" in
    --check) mode=check ;;
    --regenerate) mode=regenerate ;;
    *) echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2 ;;
esac
[ "$#" -le 1 ] || { echo "usage: $(basename "$0") [--check | --regenerate]" >&2; exit 2; }

NDK_DIR="$SDK_DIR/ndk/$NDK_VERSION"
CLANG="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
OBJDUMP="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump"

if [ ! -x "$CLANG" ] || [ ! -x "$OBJDUMP" ]; then
    if [ "$mode" = "check" ]; then
        echo "skip: NDK $NDK_VERSION not installed" >&2
        exit $SKIP_EXIT_CODE
    fi
    echo "error: NDK $NDK_VERSION not found at $NDK_DIR" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

sources=$(find "$SRC_DIR" -name '*.c' | sort)
if [ -z "$sources" ]; then
    echo "error: no sources under $SRC_DIR" >&2
    exit 1
fi

for source in $sources; do
    "$CLANG" -O2 -c -march=armv8.2-a+lse+rcpc -o "$work/$(basename "$source" .c).o" "$source" || exit 1
done

"$OBJDUMP" -d --no-show-raw-insn "$work"/*.o 2>/dev/null \
    | awk -F'\t' 'NF > 1 { split($2, parts, " "); if (parts[1] != "") print parts[1] }' \
    | sort | uniq -c | sort -rn -k1,1 -k2,2 \
    | awk '{ printf "%s %s\n", $2, $1 }' > "$work/histogram.txt"

total=$(awk '{ total += $2 } END { print total + 0 }' "$work/histogram.txt")
distinct=$(wc -l < "$work/histogram.txt")

if [ "$total" -eq 0 ]; then
    echo "error: the corpus produced no instructions" >&2
    exit 1
fi

{
    echo "# mnemonic count - regenerate with tools/histogram-arm64.sh --regenerate"
    echo "# corpus: tests/guest/src, NDK $NDK_VERSION, -O2 -march=armv8.2-a+lse+rcpc"
    echo "# total $total distinct $distinct"
    cat "$work/histogram.txt"
} > "$work/final.txt"

if [ "$mode" = "check" ]; then
    if [ ! -f "$HISTOGRAM" ]; then
        echo "error: $HISTOGRAM is missing; run --regenerate" >&2
        exit 1
    fi
    if diff -u "$HISTOGRAM" "$work/final.txt"; then
        echo "OK: histogram matches - $total instructions, $distinct distinct mnemonics"
        exit 0
    fi
    echo "FAIL: the corpus histogram changed" >&2
    exit 1
fi

cp "$work/final.txt" "$HISTOGRAM"
echo "wrote $HISTOGRAM - $total instructions, $distinct distinct mnemonics"

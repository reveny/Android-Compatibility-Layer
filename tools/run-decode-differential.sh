#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
count="${DECODE_DIFFERENTIAL_COUNT:-20000}"
seed="${DECODE_DIFFERENTIAL_SEED:-20260821}"
features="${DECODE_DIFFERENTIAL_FEATURES:-+lse,+rcpc,+rcpc-immo,+pauth,+bti,+flagm,+altnzcv,+jsconv,+complxnum,+dotprod,+fullfp16,+i8mm,+bf16,+sha3,+sm4}"

binary="$buildDir/src/DecodeDifferentialTest"

if ! command -v llvm-mc > /dev/null 2>&1; then
    echo "gate SKIPPED: llvm-mc is not installed" >&2
    exit 77
fi

if ! llvm-mc --version 2>/dev/null | grep -c 'aarch64' > /dev/null; then
    echo "gate FAILED: llvm-mc is installed but has no aarch64 target - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -x "$binary" ]; then
    echo "gate FAILED: $binary is missing - it is an output of the build under test" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

"$binary" --generate "$count" "$seed" > "$work/encodings.txt"
if [ ! -s "$work/encodings.txt" ]; then
    echo "gate FAILED: the generator produced no encodings" >&2
    exit 1
fi

awk '{ printf "0x%s 0x%s 0x%s 0x%s\n", substr($1,7,2), substr($1,5,2), substr($1,3,2), substr($1,1,2) }' "$work/encodings.txt" \
    > "$work/bytes.txt"

llvm-mc -triple=aarch64 -mattr="$features" -disassemble < "$work/bytes.txt" > "$work/valid.txt" 2> "$work/stderr.txt"

grep -vE '^\s*\.text|^\s*$' "$work/valid.txt" > "$work/decoded.txt"

grep -E 'warning: invalid instruction encoding' "$work/stderr.txt" \
    | grep -oE '^<stdin>:[0-9]+' | cut -d: -f2 | sort -un > "$work/invalid.txt"

grep -E 'warning: potentially undefined instruction encoding' "$work/stderr.txt" \
    | grep -oE '^<stdin>:[0-9]+' | cut -d: -f2 | sort -un > "$work/ambiguous.txt"

decodedLines=$(wc -l < "$work/decoded.txt")
invalidLines=$(wc -l < "$work/invalid.txt")
encodingLines=$(wc -l < "$work/encodings.txt")

if [ $((decodedLines + invalidLines)) -ne "$encodingLines" ]; then
    echo "gate FAILED: llvm-mc accounted for $decodedLines decoded + $invalidLines invalid, but there were $encodingLines encodings" >&2
    exit 1
fi

awk -v invalidFile="$work/invalid.txt" -v ambiguousFile="$work/ambiguous.txt" -v decodedFile="$work/decoded.txt" '
BEGIN {
    while ((getline entry < invalidFile) > 0) rejected[entry + 0] = 1
    while ((getline entry < ambiguousFile) > 0) ambiguous[entry + 0] = 1
    while ((getline entry < decodedFile) > 0) accepted[++acceptedCount] = entry
    taken = 0
}
{
    if (FNR in rejected) { print $1 " INVALID"; next }
    taken += 1
    if (FNR in ambiguous) print $1 " AMBIGUOUS"
    else print $1 " " accepted[taken]
}
' "$work/encodings.txt" > "$work/reference.txt"

"$binary" "$work/reference.txt"
status=$?

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: the decoder disagrees with llvm-mc" >&2
    exit 1
fi

echo "gate PASSED: $count encodings, seed $seed"

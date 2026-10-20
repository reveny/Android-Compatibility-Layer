#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
golden="${2:-$PROJ_DIR/tests/text/golden-device-metrics.txt}"

harness="$buildDir/src/TextMetricsHarness"

if [ ! -e "$harness" ]; then
    echo "gate FAILED: $harness is missing - it is an output of the build under test" >&2
    exit 1
fi

if [ ! -s "$golden" ]; then
    echo "gate FAILED: device metrics golden missing: $golden" >&2
    echo "             capture it with tools/capture-text-metrics.sh --regenerate against a device" >&2
    exit 1
fi

if [ ! -s "$golden.provenance" ] && [ ! -s "${golden%.txt}.provenance" ]; then
    echo "gate FAILED: the golden has no provenance record beside it" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

if ! "$harness" > "$work/ours.txt" 2>"$work/harness.err"; then
    echo "gate FAILED: the metrics harness did not run" >&2
    sed -n '1,10p' "$work/harness.err" >&2
    exit 1
fi

if [ ! -s "$work/ours.txt" ]; then
    echo "gate FAILED: the metrics harness produced no rows" >&2
    exit 1
fi

"$PROJ_DIR/tools/compare-text-metrics.sh" "$work/ours.txt" "$golden"

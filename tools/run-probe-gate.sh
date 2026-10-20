#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dex="${1:-$PROJ_DIR/build/tests/probe/probe.dex}"

if [ ! -e "$dex" ]; then
    echo "gate FAILED: $dex is missing - it is an output of the build under test" >&2
    exit 1
fi

output=$("$PROJ_DIR/tools/run-dalvikvm.sh" "$dex" Probe 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: runtime exited $status" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q '^hello$'; then
    echo "gate FAILED: probe did not print hello" >&2
    exit 1
fi

if ! printf '%s' "$output" | grep -q 'vm\.name    = Dalvik'; then
    echo "gate FAILED: runtime is not ART (expected vm.name = Dalvik)" >&2
    exit 1
fi

echo "gate PASSED: real ART executed the dex and exited 0"

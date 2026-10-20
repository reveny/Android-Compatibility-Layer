#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

classes="$buildDir/java/classes"
if [ ! -d "$classes" ]; then
    echo "framework tests FAILED: $classes is missing - it is an output of the build under test" >&2
    exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mapfile -t sources < <(find "$PROJ_DIR/tests/framework/src" -name '*.java')

if ! javac -nowarn -cp "$classes" -d "$work" "${sources[@]}"; then
    echo "framework tests FAILED: could not compile the tests against the framework classes" >&2
    exit 1
fi

status=0
for source in "${sources[@]}"; do
    name="$(basename "$source" .java)"
    echo "--- $name ---"
    if ! timeout 120 java -cp "$classes:$work" "$name"; then
        echo "framework tests FAILED: $name" >&2
        status=1
    fi
done

if [ "$status" -ne 0 ]; then exit 1; fi

echo "framework tests PASSED"

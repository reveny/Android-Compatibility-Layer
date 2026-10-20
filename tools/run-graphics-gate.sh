#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

if ! command -v javac >/dev/null || ! command -v java >/dev/null; then
    echo "gate SKIPPED: no javac or java on the path" >&2
    exit 77
fi

work="$buildDir/tests/graphics-unit"
rm -rf "$work"
mkdir -p "$work"

sources=(
    "$PROJ_DIR/java/android/graphics/Matrix.java"
    "$PROJ_DIR/java/android/graphics/RectF.java"
    "$PROJ_DIR/java/android/graphics/Rect.java"
    "$PROJ_DIR/java/android/graphics/Path.java"
    "$PROJ_DIR/java/android/util/PathParser.java"
    "$PROJ_DIR/java/android/view/InputEvent.java"
    "$PROJ_DIR/java/android/view/MotionEvent.java"
    "$PROJ_DIR/java/runtime/PointerTracker.java"
    "$PROJ_DIR/tests/graphics/host/GraphicsUnit.java"
    "$PROJ_DIR/tests/graphics/host/PathDataUnit.java"
    "$PROJ_DIR/tests/graphics/host/PointerUnit.java"
)

for source in "${sources[@]}"; do
    if [ ! -f "$source" ]; then
        echo "gate FAILED: $source is missing" >&2
        exit 1
    fi
done

if ! javac --release 11 -nowarn -d "$work" "${sources[@]}" > "$work/javac.log" 2>&1; then
    echo "gate FAILED: the graphics sources did not compile" >&2
    cat "$work/javac.log" >&2
    exit 1
fi

if ! java -cp "$work" GraphicsUnit; then
    echo "gate FAILED: the geometry checks did not pass" >&2
    exit 1
fi

if ! java -cp "$work" android.util.PathDataUnit; then
    echo "gate FAILED: the path data checks did not pass" >&2
    exit 1
fi

if ! java -cp "$work" runtime.PointerUnit; then
    echo "gate FAILED: the pointer tracking checks did not pass" >&2
    exit 1
fi

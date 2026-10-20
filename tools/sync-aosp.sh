#!/usr/bin/env bash
set -u

AOSP_DIR="${AOSP_DIR:-$HOME/aosp/master-art}"
JOBS="${JOBS:-4}"

export PATH="$HOME/.local/bin:$PATH"

if [ ! -d "$AOSP_DIR/.repo" ]; then
    echo "error: no repo checkout at $AOSP_DIR (run repo init first)" >&2
    exit 1
fi

cd "$AOSP_DIR" || exit 1

echo "=== before ==="
df -h . | tail -1
du -sh . 2>/dev/null

repo sync -c -j"$JOBS" --no-tags --optimized-fetch --prune --retry-fetches=3
status=$?

echo "=== after ==="
df -h . | tail -1
du -sh . 2>/dev/null

if [ $status -ne 0 ]; then
    echo
    echo "sync incomplete (exit $status) - just run this script again, it resumes."
fi
exit $status

#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"
frameworkRes="${RUNTIME_FRAMEWORK_RES-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/platforms/android-34/android.jar}"

frames="${RUNTIME_PACING_FRAMES:-240}"
fpsFloor="${RUNTIME_PACING_FPS_FLOOR:-50}"
stallThreshold="${RUNTIME_PACING_STALL_MILLIS:-33.3}"
stallFraction="${RUNTIME_PACING_STALL_FRACTION:-2}"

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

if [ ! -f "$frameworkRes" ]; then
    echo "gate SKIPPED: no SDK platform android.jar at '$frameworkRes'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$apk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

if [ -z "${WAYLAND_DISPLAY:-}" ] && [ -z "${WAYLAND_SOCKET:-}" ]; then
    echo "gate SKIPPED: no Wayland compositor in the environment, so there is no vsync to be paced by" >&2
    exit 77
fi

if ! awk 'BEGIN { values[1] = 1; asort(values); exit 0 }' > /dev/null 2>&1; then
    echo "gate SKIPPED: this awk has no asort(), so the pacing report cannot be summarised" >&2
    exit 77
fi

loaders=$(ldconfig -p 2>/dev/null || true)
if [ ! -e "/usr/lib/libvulkan.so.1" ] && [ ! -e "/usr/lib64/libvulkan.so.1" ] &&
   ! printf '%s' "$loaders" | grep -F "libvulkan.so.1" > /dev/null; then
    echo "gate SKIPPED: no host Vulkan loader, so the guest's calls have nowhere to go" >&2
    exit 77
fi

dataRoot="$(mktemp -d)"
report="$buildDir/pacing.txt"
rm -f "$report"
trap 'rm -rf "$dataRoot"' EXIT

output=$(timeout 240 "$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --framework-res "$frameworkRes" \
    --data-root "$dataRoot" \
    --activity android.app.NativeActivity \
    --pacing-report "$report" \
    --guest-env "TRIANGLE_FRAMES=$frames" \
    "$apk" 2>&1)

printf '%s' "$output" | grep -vE 'presented frame [0-9]+$'

if ! printf '%s' "$output" | grep -F "[guest triangle] triangle finished" > /dev/null; then
    echo "gate FAILED: the guest did not finish presenting, so there is no pacing to judge" >&2
    exit 1
fi

if [ ! -f "$report" ]; then
    echo "gate FAILED: no pacing report was written to $report" >&2
    exit 1
fi

hostPresented=$(printf '%s' "$output" | sed -n 's/.*the host driver presented \([0-9]*\) guest frame.*/\1/p' | head -1)
if [ "$hostPresented" != "$frames" ]; then
    echo "gate FAILED: the host driver presented $hostPresented frames, not the $frames asked for" >&2
    exit 1
fi

summary=$(awk -v stallMillis="$stallThreshold" '
    /^interval/ { value = $3 / 1000000.0; intervals[++count] = value; total += value }
    END {
        if (count < 30) { printf "TOOFEW %d\n", count; exit }

        asort(intervals)
        median = intervals[int(count * 0.5)]
        p95 = intervals[int(count * 0.95)]
        worst = intervals[count]

        stalls = 0
        for (position = 1; position <= count; position++) if (intervals[position] > stallMillis) stalls++

        sustained = count / (total / 1000.0)
        stallPercent = stalls * 100.0 / count
        printf "OK %d %.3f %.3f %.3f %.1f %.1f %d\n", count, median, p95, worst, sustained, stallPercent, stalls
    }
' "$report")

if [ -z "$summary" ]; then
    echo "gate FAILED: the pacing report could not be summarised at all" >&2
    exit 1
fi

if [ "${summary%% *}" = "TOOFEW" ]; then
    echo "gate FAILED: only ${summary##* } interval(s) recorded; too few to judge pacing" >&2
    exit 1
fi

read -r _ count median p95 worst achieved stallPercent stalls <<< "$summary"

echo "pacing: $count intervals, sustained ${achieved} fps, median ${median}ms, p95 ${p95}ms, worst ${worst}ms"
echo "pacing: ${stalls} stall(s) over ${stallThreshold}ms (${stallPercent}%)"
echo "pacing: raw intervals in $report"

if awk -v m="$median" 'BEGIN { exit !(m < 1.0) }'; then
    echo "gate FAILED: the median interval is ${median}ms - presentation is free-running, not paced by a compositor" >&2
    exit 1
fi

if awk -v a="$achieved" -v f="$fpsFloor" 'BEGIN { exit !(a < f) }'; then
    echo "gate FAILED: sustained ${achieved} fps is below the ${fpsFloor} fps floor" >&2
    exit 1
fi

if awk -v s="$stallPercent" -v a="$stallFraction" 'BEGIN { exit !(s > a) }'; then
    echo "gate FAILED: ${stallPercent}% of frames stalled past ${stallThreshold}ms, above the ${stallFraction}% allowed" >&2
    exit 1
fi

echo "gate PASSED: $frames frames sustained ${achieved} fps (median ${median}ms), ${stallPercent}% stalled, floor ${fpsFloor} fps"

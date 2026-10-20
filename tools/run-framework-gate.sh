#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$apk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

output=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$dataRoot" \
    --max-frames 200 \
    "$apk" 2>&1)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

for expected in \
    "declares 4 activities, 2 service(s), 2 receiver(s), 2 provider(s)" \
    "I/Activities: started com.reveny.helloworld.SecondActivity in task 1, stack is now 2 deep" \
    "I/FrameworkProbe: second activity created with note=carried" \
    "I/Activities: finished com.reveny.helloworld.SecondActivity, back to com.reveny.helloworld.MainActivity" \
    "I/FrameworkProbe: activity result request=42 result=-1 answer=returned-carried" \
    "I/FrameworkProbe: service started with command=warm-up startId=1" \
    "I/FrameworkProbe: binder answered echo:through-the-binder" \
    "I/FrameworkProbe: service unbound" \
    "I/Services: destroyed com.reveny.helloworld.EchoService" \
    "I/FrameworkProbe: registered receiver got com.reveny.helloworld.PING payload=hello" \
    "I/FrameworkProbe: manifest receiver got com.reveny.helloworld.PING payload=hello" \
    "I/FrameworkProbe: provider inserted first-note" \
    "I/FrameworkProbe: provider round trip read first-note" \
    "I/Notifications: posted 7 Notification{channel=probe title=probe title text=probe text}" \
    "I/Notifications: cancelled 7" \
    "I/AlarmManager: the alarm for" \
    "I/FrameworkProbe: registered receiver got com.reveny.helloworld.PING payload=from-the-alarm" \
    "I/FrameworkProbe: job ran with id 11" \
    "I/Jobs: job 11 finished"
do
    if ! contains "$expected" "$output"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

settled=$(printf '%s\n' "$output" | grep -c "I/Jobs: job 11 finished")
if [ "$settled" -ne 1 ]; then
    echo "gate FAILED: job 11 settled $settled times, expected exactly once" >&2
    exit 1
fi

echo "framework gate PASSED: intents, services, broadcasts, providers, notifications, alarms and jobs all reached their component"

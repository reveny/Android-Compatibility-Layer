#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
apk="${2:-$buildDir/tests/jni/jni.apk}"

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

if ! unzip -l "$apk" 2>/dev/null | grep -c 'lib/x86_64/libacljni\.so' > /dev/null; then
    echo "gate FAILED: $apk does not carry lib/x86_64/libacljni.so - the gate would prove nothing" >&2
    exit 1
fi

dataRoot="$(mktemp -d)"
trap 'rm -rf "$dataRoot"' EXIT

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

matches()
{
    printf '%s' "$2" | grep -cE "$1" > /dev/null
}

run()
{
    "$launcher" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --data-root "$dataRoot" \
        --no-activity \
        "$@" \
        "$apk" 2>&1
}

output=$(run)
status=$?

echo "$output"

if [ "$status" -ne 0 ]; then
    echo "gate FAILED: launcher exited $status" >&2
    exit 1
fi

libraryDir="$dataRoot/com.example.jni/lib"
if [ ! -f "$libraryDir/libacljni.so" ]; then
    echo "gate FAILED: the native library was not extracted to $libraryDir" >&2
    exit 1
fi

for expected in \
    "[launcher] extracted 1 x86_64 native library to $libraryDir" \
    "I/Jni: nativeLibraryDir=/data/app/~~" \
    "/com.example.jni-" \
    "[acljni] JNI_OnLoad registered 8 methods" \
    "I/Jni: loadLibrary acljni returned" \
    "I/Jni: registered upcall = 42" \
    "I/Jni: mangled upcall = 101" \
    "I/Jni: string round trip = [acl-native]" \
    "I/Jni: instance upcall = 42" \
    "I/Jni: array sum = 10 mutated = [11, 12, 13, 14]" \
    "I/Jni: field bump = 7 field = 12" \
    "I/Jni: caught java exception = refused 9" \
    "I/Jni: caught native exception = native refused 3" \
    "I/Jni: attached thread upcall = 22" \
    "I/Jni: openFd stored.bin length=38 offset=true read=38 text=stored payload for the descriptor path" \
    "I/Jni: openFd deflated.txt refused: deflated.txt is compressed in the archive, so it has no file descriptor" \
    "I/Jni: openFd absent.bin refused" \
    "I/Jni: descriptor fd balance = 0" \
    "I/Jni: conformance complete"
do
    if ! contains "$expected" "$output"; then
        echo "gate FAILED: missing '$expected'" >&2
        exit 1
    fi
done

if matches 'unimplemented|Exception in thread|NoSuchMethodError|NoClassDefFoundError' "$output"; then
    echo "gate FAILED: a strict guard fired or the guest threw" >&2
    exit 1
fi

checked=$(run --trace jni)
checkedStatus=$?

if [ "$checkedStatus" -ne 0 ]; then
    echo "$checked"
    echo "gate FAILED: the conformance harness exited $checkedStatus under -Xcheck:jni" >&2
    exit 1
fi

if ! contains "I/Jni: conformance complete" "$checked"; then
    echo "$checked"
    echo "gate FAILED: the conformance harness did not complete under -Xcheck:jni" >&2
    exit 1
fi

if matches 'JNI DETECTED ERROR|WARNING in native method' "$checked"; then
    echo "$checked"
    echo "gate FAILED: -Xcheck:jni reported a JNI misuse" >&2
    exit 1
fi

if ! contains "[jni] extracted libacljni.so" "$checked"; then
    echo "gate FAILED: --trace jni did not report the extraction" >&2
    exit 1
fi

if ! contains "[jni] vm option -Xcheck:jni" "$checked"; then
    echo "gate FAILED: the second pass did not actually pass -Xcheck:jni to the VM" >&2
    exit 1
fi

if contains "[jni] vm option -Xcheck:jni" "$output"; then
    echo "gate FAILED: the first pass passed -Xcheck:jni, so the two passes are not distinct" >&2
    exit 1
fi

printf 'corrupted' | dd of="$libraryDir/libacljni.so" bs=1 seek=64 conv=notrunc status=none

repaired=$(run)
repairedStatus=$?
if [ "$repairedStatus" -ne 0 ] || ! contains "I/Jni: conformance complete" "$repaired"; then
    echo "$repaired"
    echo "gate FAILED: a corrupted native library was not re-extracted" >&2
    exit 1
fi

if ! cmp -s "$libraryDir/libacljni.so" "$buildDir/tests/jni/jnilibs/x86_64/libacljni.so"; then
    echo "gate FAILED: the re-extracted library does not match the one that was packaged" >&2
    exit 1
fi

refusedRoot="$(mktemp -d)"
refused=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$refusedRoot" \
    --no-activity \
    --abi arm64-v8a \
    "$apk" 2>&1)
refusedStatus=$?
rm -rf "$refusedRoot"

if [ "$refusedStatus" -eq 0 ]; then
    echo "$refused"
    echo "gate FAILED: an unrunnable ABI was reported as success" >&2
    exit 1
fi

if [ "$refusedStatus" -ge 128 ]; then
    echo "$refused"
    echo "gate FAILED: an unrunnable ABI killed the process with signal $((refusedStatus - 128)) instead of being refused" >&2
    exit 1
fi

if ! contains "has no arm64-v8a native library (it ships: x86_64)" "$refused"; then
    echo "$refused"
    echo "gate FAILED: the refusal did not name the ABI the archive actually ships" >&2
    exit 1
fi

if ! contains "java.lang.UnsatisfiedLinkError" "$refused"; then
    echo "$refused"
    echo "gate FAILED: the guest did not see an UnsatisfiedLinkError" >&2
    exit 1
fi

echo "gate PASSED: an x86_64 .so out of an APK did JNI in both directions on real ART"

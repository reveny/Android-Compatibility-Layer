#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"
signedApk="${2:-$PROJ_DIR/tests/app-debug.apk}"

launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
strictApk="$buildDir/tests/strict/strict.apk"
splitApk="$buildDir/tests/split/split_config.extra.apk"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ -z "$hostRoot" ]; then
    echo "gate FAILED: RUNTIME_HOST_ROOT is set but empty - that is a misconfiguration, not an absent prerequisite" >&2
    exit 1
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$signedApk" "$strictApk" "$splitApk"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing - it is an output of the build under test" >&2
        exit 1
    fi
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

contains()
{
    case "$2" in
        *"$1"*) return 0 ;;
        *) return 1 ;;
    esac
}

echo "=== a v2 signed package: the guest sees the real certificate"

signedOutput=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/signed" \
    --max-frames 200 \
    "$signedApk" 2>&1)
signedStatus=$?

if [ "$signedStatus" -ne 0 ]; then
    echo "gate FAILED: the signed package exited $signedStatus" >&2
    printf '%s\n' "$signedOutput" | tail -20 >&2
    exit 1
fi

for expected in \
    "I/PackagingProbe: SecureRandom produced 8 bytes" \
    "I/PackagingProbe: UUID.randomUUID produced a value of length 36" \
    "[launcher] the package signature verifies with v2:" \
    "[launcher] the signer is CN=" \
    "I/PackagingProbe: signed by 1 signer(s), certificate is" \
    "I/PackagingProbe: the certificate is DER: true" \
    "I/PackagingProbe: the signature round trips: true" \
    "I/PackagingProbe: the fingerprint is 64 hex characters" \
    "I/PackagingProbe: signing info reports 1 apk contents signer(s), multiple signers: false" \
    "I/PackagingProbe: signing info agrees with the signature array: true" \
    "I/PackagingProbe: the installed list holds 1 package(s)" \
    "I/PackagingProbe: installed: com.reveny.helloworld version"
do
    if ! contains "$expected" "$signedOutput"; then
        echo "gate FAILED: the signed run is missing '$expected'" >&2
        printf '%s\n' "$signedOutput" | grep -E "PackagingProbe|framework\] the package" >&2
        exit 1
    fi
done

if contains "Fatal signal" "$signedOutput"; then
    echo "gate FAILED: the signed run took a fatal signal, so the guest crypto stack is broken" >&2
    printf '%s\n' "$signedOutput" | grep -B5 "Fatal signal" >&2
    exit 1
fi

echo "note: the guest computed its own SHA-256 over the certificate, so nothing in this runtime displaced conscrypt"

echo "=== a tampered package: the digest no longer matches"

cp "$signedApk" "$work/tampered.apk"
printf '\xff' | dd of="$work/tampered.apk" bs=1 seek=64 count=1 conv=notrunc status=none

tamperedOutput=$("$launcher" \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/tampered-data" \
    --max-frames 60 \
    "$work/tampered.apk" 2>&1)

if ! contains "the package signature did not verify: the contents do not match the signed digest" "$tamperedOutput"; then
    echo "gate FAILED: a tampered package was not reported as a digest mismatch" >&2
    printf '%s\n' "$tamperedOutput" | grep -E "signature" >&2
    exit 1
fi

echo "=== an unsigned package: reported as unsigned, never faked"

unsignedOutput=$("$launcher" \
    --no-strict \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/unsigned" \
    --no-activity \
    "$strictApk" 2>&1)

for expected in \
    "the package signature did not verify: the archive carries no signing block" \
    "I/Strict: the package carries no signature"
do
    if ! contains "$expected" "$unsignedOutput"; then
        echo "gate FAILED: the unsigned run is missing '$expected'" >&2
        printf '%s\n' "$unsignedOutput" | grep -E "Strict|signature" >&2
        exit 1
    fi
done

echo "=== a base plus a split: one resource table, one asset chain"

mkdir -p "$work/installed"
cp "$strictApk" "$work/installed/base.apk"
cp "$splitApk" "$work/installed/split_config.extra.apk"

splitOutput=$("$launcher" \
    --no-strict \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/split-data" \
    --no-activity \
    "$work/installed/base.apk" 2>&1)
splitStatus=$?

if [ "$splitStatus" -ne 0 ]; then
    echo "gate FAILED: the split run exited $splitStatus" >&2
    printf '%s\n' "$splitOutput" | tail -20 >&2
    exit 1
fi

for expected in \
    "[launcher] the base apk is installed beside 1 split(s), which load into the same resource table" \
    "I/Bootstrap: the package is installed as a base plus 1 split(s)" \
    "I/Strict: the package is installed with 1 split(s)" \
    "I/Strict: split: config.extra" \
    "I/Strict: an asset that only the split carries reads back as this line lives only in the split" \
    "I/Strict: a string that only the split carries resolves to the split supplied this string"
do
    if ! contains "$expected" "$splitOutput"; then
        echo "gate FAILED: the split run is missing '$expected'" >&2
        printf '%s\n' "$splitOutput" | grep -E "Strict|split|beside" >&2
        exit 1
    fi
done

echo "note: the split was read through the sandbox, so its path was attached before confinement, not after"

echo "=== the base alone: no split is invented"

baseOutput=$("$launcher" \
    --no-strict \
    --host-root "$hostRoot" \
    --framework-jar "$frameworkJar" \
    --data-root "$work/base-data" \
    --no-activity \
    "$strictApk" 2>&1)

if ! contains "I/Strict: the package is installed with 0 split(s)" "$baseOutput"; then
    echo "gate FAILED: a package with no split beside it did not report zero splits" >&2
    printf '%s\n' "$baseOutput" | grep -E "Strict|split" >&2
    exit 1
fi

if contains "no asset from a split" "$splitOutput"; then
    echo "gate FAILED: the split asset was not reachable in the split run" >&2
    exit 1
fi

if ! contains "no asset from a split" "$baseOutput"; then
    echo "gate FAILED: the base alone found a split asset, so the two runs are not isolated" >&2
    exit 1
fi

echo "gate PASSED: a v2 signature verifies and reaches the guest, tampering is caught, an unsigned package says so, and splits load"

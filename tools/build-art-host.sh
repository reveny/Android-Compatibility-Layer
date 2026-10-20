#!/usr/bin/env bash

AOSP_DIR="${AOSP_DIR:-$HOME/aosp/master-art}"
BANCHAN_ARCH="${BANCHAN_ARCH:-x86_64}"
NINJA_FILE="out/build-module_${BANCHAN_ARCH}.ninja"
SOONG_NINJA_GLOB="out/soong/build.module_${BANCHAN_ARCH}*.ninja"
MINIMUM_EXPECTED_TARGETS=16

HOST_PREFIX="out/host/linux-x86"
REQUIRED_HOST_TARGETS=(
    "$HOST_PREFIX/bin/dalvikvm"
    "$HOST_PREFIX/bin/dex2oat"
    "$HOST_PREFIX/lib64/libart.so"
    "$HOST_PREFIX/lib64/libartbase.so"
    "$HOST_PREFIX/lib64/libartpalette.so"
    "$HOST_PREFIX/lib64/libopenjdk.so"
    "$HOST_PREFIX/lib64/libnativehelper.so"
    "$HOST_PREFIX/lib64/libbase.so"
    "$HOST_PREFIX/lib64/liblog.so"
    "$HOST_PREFIX/lib64/libutils.so"
    "$HOST_PREFIX/lib64/libcutils.so"
    "$HOST_PREFIX/lib64/libziparchive.so"
    "$HOST_PREFIX/lib64/libc++.so"
    "$HOST_PREFIX/lib64/libjavacrypto.so"
    "$HOST_PREFIX/lib64/libjavacore.so"
    "$HOST_PREFIX/lib64/libicu_jni.so"
    "$HOST_PREFIX/lib64/libopenjdkjvm.so"
    "$HOST_PREFIX/lib64/libopenjdkjvmti.so"
    "$HOST_PREFIX/lib64/libandroidio.so"
    "$HOST_PREFIX/lib64/libnativebridge.so"
    "$HOST_PREFIX/lib64/libnativeloader.so"
    "$HOST_PREFIX/lib64/libsigchain.so"
    "$HOST_PREFIX/lib64/libdexfile.so"
    "$HOST_PREFIX/lib64/libprofile.so"
    "$HOST_PREFIX/lib64/libartservice.so"
    "$HOST_PREFIX/lib64/libnpt.so"
    "$HOST_PREFIX/framework/d8.jar"
    "$HOST_PREFIX/framework/core-oj-hostdex.jar"
    "$HOST_PREFIX/framework/core-libart-hostdex.jar"
    "$HOST_PREFIX/framework/okhttp-hostdex.jar"
    "$HOST_PREFIX/framework/bouncycastle-hostdex.jar"
    "$HOST_PREFIX/framework/apache-xml-hostdex.jar"
    "$HOST_PREFIX/framework/conscrypt-hostdex.jar"
    "$HOST_PREFIX/framework/core-icu4j-hostdex.jar"
)

cd "$AOSP_DIR" || exit 1

echo "=== disk before ==="
df -h . | tail -1

source build/envsetup.sh >/dev/null 2>&1
banchan com.android.art "$BANCHAN_ARCH" >/dev/null 2>&1

if [ -z "${TARGET_BUILD_APPS:-}" ]; then
    echo "error: banchan did not take - start a fresh shell" >&2
    exit 1
fi
if [ -z "${ANDROID_HOST_OUT:-}" ]; then
    echo "error: ANDROID_HOST_OUT unset after envsetup" >&2
    exit 1
fi

export SOONG_ONLY=true

if [ ! -f "$NINJA_FILE" ] && ! compgen -G "$SOONG_NINJA_GLOB" >/dev/null; then
    echo "note: no build graph yet, running 'm nothing' to generate it"
    if ! m nothing; then
        echo "error: 'm nothing' failed, so the build graph was never generated" >&2
        exit 1
    fi
fi

targets=()

if [ -f "$NINJA_FILE" ]; then
    mapfile -t targets < <(
        sed -e ':join' -e '/\$$/{N;s/\$\n[[:space:]]*//;bjoin' -e '}' "$NINJA_FILE" \
        | grep -m1 "^build build-art-host:" \
        | tr ' ' '\n' \
        | grep "^out/host/" \
        | grep -v "/apex/" \
        | grep -v "timestamp" \
        | grep -v "^out/host/linux-x86/lib/" \
        | grep -v "^out/host/linux-x86/bin/art$" \
        | sort -u
    )
fi

if [ ${#targets[@]} -lt "$MINIMUM_EXPECTED_TARGETS" ]; then
    echo "note: no usable build-art-host phony, using the explicit host target list"

    missingFromGraph=()
    for target in "${REQUIRED_HOST_TARGETS[@]}"; do
        if ! grep -qhF "build $target:" $SOONG_NINJA_GLOB 2>/dev/null; then
            missingFromGraph+=("$target")
        fi
    done

    if [ ${#missingFromGraph[@]} -gt 0 ]; then
        echo "error: the build graph does not produce these host targets:" >&2
        printf '  %s\n' "${missingFromGraph[@]}" >&2
        exit 1
    fi

    targets=("${REQUIRED_HOST_TARGETS[@]}")
fi

derivedCount=${#targets[@]}
if [ "$derivedCount" -lt "$MINIMUM_EXPECTED_TARGETS" ]; then
    echo "error: derived only $derivedCount host targets from $NINJA_FILE (expected >= $MINIMUM_EXPECTED_TARGETS)" >&2
    echo "the build-art-host edge may have changed shape upstream; check docs/build-notes.md" >&2
    printf '  %s\n' "${targets[@]}" >&2
    exit 1
fi

targets+=(out/host/linux-x86/bin/dalvikvm out/host/linux-x86/bin/dex2oat)
mapfile -t targets < <(printf '%s\n' "${targets[@]}" | sort -u)

echo "=== building ${#targets[@]} host targets ==="
printf '  %s\n' "${targets[@]}"

time m "${targets[@]}"
status=$?

echo "=== disk after ==="
df -h . | tail -1
du -sh out 2>/dev/null

echo "=== result ==="
if [ $status -ne 0 ]; then
    echo "BUILD FAILED (exit $status)"
    exit $status
fi

missing=0
for target in "${targets[@]}"; do
    [ -e "$target" ] || { echo "MISSING: $target" >&2; missing=$((missing + 1)); }
done
[ "$missing" -eq 0 ] || { echo "error: $missing declared targets absent after a successful build" >&2; exit 1; }

echo "libart       : $(ls -l "$ANDROID_HOST_OUT"/lib64/libart.so 2>/dev/null || echo MISSING)"
echo "hostdex jars :"
ls "$ANDROID_HOST_OUT"/framework/ 2>/dev/null | grep hostdex | sed 's/^/  /' || true

exit 0

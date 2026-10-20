#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "${SCRIPT_DIR}")"

FIXTURE_DIR="${1:-}"
if [[ -z "${FIXTURE_DIR}" ]]; then
    echo "usage: $(basename "$0") <fixture-dir> [output.apk]" >&2
    exit 2
fi

FIXTURE_DIR="$(cd "${FIXTURE_DIR}" && pwd)"
OUTPUT_APK="${2:-${FIXTURE_DIR}/$(basename "${FIXTURE_DIR}").apk}"

SDK_ROOT="${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-34.0.0}"
PLATFORM_VERSION="${PLATFORM_VERSION:-android-34}"

AAPT2="${SDK_ROOT}/build-tools/${BUILD_TOOLS_VERSION}/aapt2"
ANDROID_JAR="${SDK_ROOT}/platforms/${PLATFORM_VERSION}/android.jar"
D8_JAR="${RUNTIME_D8_JAR:-${HOME}/aosp/master-art/out/host/linux-x86/framework/d8.jar}"

for tool in "${AAPT2}" "${ANDROID_JAR}" "${D8_JAR}" "${FIXTURE_DIR}/AndroidManifest.xml"; do
    if [[ ! -e "${tool}" ]]; then
        echo "missing: ${tool}" >&2
        exit 1
    fi
done

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT

mkdir -p "${WORK_DIR}/classes" "${WORK_DIR}/dex"

ASSET_ARGS=()
if [[ -d "${FIXTURE_DIR}/assets" ]]; then
    ASSET_ARGS=(-A "${FIXTURE_DIR}/assets")
fi

RESOURCE_ARGS=()
if [[ -d "${FIXTURE_DIR}/res" ]]; then
    "${AAPT2}" compile --dir "${FIXTURE_DIR}/res" -o "${WORK_DIR}/res.zip"
    RESOURCE_ARGS=("${WORK_DIR}/res.zip" --java "${WORK_DIR}/gen")
    mkdir -p "${WORK_DIR}/gen"
fi

"${AAPT2}" link \
    --manifest "${FIXTURE_DIR}/AndroidManifest.xml" \
    -I "${ANDROID_JAR}" \
    "${ASSET_ARGS[@]}" \
    "${RESOURCE_ARGS[@]}" \
    -0 .bin \
    --min-sdk-version 21 \
    --target-sdk-version 34 \
    -o "${WORK_DIR}/base.apk"

SOURCES=()
if [[ -d "${FIXTURE_DIR}/src" ]]; then
    mapfile -t SOURCES < <(find "${FIXTURE_DIR}/src" -name '*.java')
fi

if [[ ${#SOURCES[@]} -eq 0 && ! -d "${FIXTURE_DIR}/src" ]]; then
    echo "no src directory under ${FIXTURE_DIR}, so this fixture carries resources only"
elif [[ ${#SOURCES[@]} -eq 0 ]]; then
    echo "no sources under ${FIXTURE_DIR}/src" >&2
    exit 1
fi

if [[ ${#SOURCES[@]} -gt 0 ]]; then
    if [[ -d "${WORK_DIR}/gen" ]]; then
        mapfile -t GENERATED < <(find "${WORK_DIR}/gen" -name '*.java')
        SOURCES+=("${GENERATED[@]}")
    fi

    COMPILE_PATH="${ANDROID_JAR}"
    EXTRA_LIB_ARGS=()

    if [[ -n "${FIXTURE_CLASSPATH:-}" ]]; then
        if [[ ! -e "${FIXTURE_CLASSPATH}" ]]; then
            echo "FIXTURE_CLASSPATH does not exist: ${FIXTURE_CLASSPATH}" >&2
            exit 1
        fi

        COMPILE_PATH="${ANDROID_JAR}:${FIXTURE_CLASSPATH}"
        EXTRA_LIB_ARGS=(--lib "${FIXTURE_CLASSPATH}")
    fi

    javac --release 11 -nowarn -cp "${COMPILE_PATH}" -d "${WORK_DIR}/classes" "${SOURCES[@]}"

    mapfile -t CLASS_FILES < <(find "${WORK_DIR}/classes" -name '*.class')

    java -cp "${D8_JAR}" com.android.tools.r8.D8 \
        --release --min-api 21 \
        --lib "${ANDROID_JAR}" \
        "${EXTRA_LIB_ARGS[@]}" \
        --output "${WORK_DIR}/dex" \
        "${CLASS_FILES[@]}"

    (cd "${WORK_DIR}/dex" && zip -q -X "${WORK_DIR}/base.apk" classes.dex)
fi

if [[ -n "${FIXTURE_NATIVE_LIBS:-}" ]]; then
    if [[ ! -d "${FIXTURE_NATIVE_LIBS}" ]]; then
        echo "FIXTURE_NATIVE_LIBS is not a directory: ${FIXTURE_NATIVE_LIBS}" >&2
        exit 1
    fi

    mapfile -t NATIVE_LIBS < <(find "${FIXTURE_NATIVE_LIBS}" -name '*.so')
    if [[ ${#NATIVE_LIBS[@]} -eq 0 ]]; then
        echo "no .so under ${FIXTURE_NATIVE_LIBS}" >&2
        exit 1
    fi

    mkdir -p "${WORK_DIR}/staging"
    cp -r "${FIXTURE_NATIVE_LIBS}" "${WORK_DIR}/staging/lib"
    (cd "${WORK_DIR}/staging" && zip -q -X -r "${WORK_DIR}/base.apk" lib)
fi

mkdir -p "$(dirname "${OUTPUT_APK}")"
mv "${WORK_DIR}/base.apk" "${OUTPUT_APK}"

echo "built ${OUTPUT_APK}"

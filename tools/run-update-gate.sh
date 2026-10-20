#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
buildDir="${1:-$PROJ_DIR/build}"

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
API_LEVEL="${API_LEVEL:-34}"
NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
GUEST_ARCH="${GUEST_ARCH:-armv8.2-a+lse+rcpc}"

CLANG="$SDK_DIR/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android${API_LEVEL}-clang"
launcher="$buildDir/src/acl"
frameworkJar="$buildDir/java/framework.jar"
hostRoot="${RUNTIME_HOST_ROOT-$HOME/aosp/master-art/out/host/linux-x86}"

if [ ! -x "$CLANG" ]; then
    echo "gate SKIPPED: NDK $NDK_VERSION not installed" >&2
    exit 77
fi

if [ ! -d "$hostRoot" ]; then
    echo "gate SKIPPED: host ART not built at '$hostRoot'" >&2
    exit 77
fi

for file in "$launcher" "$frameworkJar" "$PROJ_DIR/tools/build-fixture-apk.sh" "$PROJ_DIR/tests/minimal/AndroidManifest.xml"; do
    if [ ! -e "$file" ]; then
        echo "gate FAILED: $file is missing" >&2
        exit 1
    fi
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

cat > "$work/stable.c" <<'EOF'
int StableAdd(int left, int right)
{
    int total = 0;
    for (int index = 0; index < right; index++) total += left + index;
    return total;
}

int StableScale(int value)
{
    return StableAdd(value, 3) * 7;
}
EOF

cat > "$work/changing-first.c" <<'EOF'
int ChangingValue(int seed)
{
    return seed + 1;
}
EOF

cat > "$work/changing-second.c" <<'EOF'
const int ChangingTable[262144] = {1, 2, 3};

int ChangingValue(int seed)
{
    int total = seed;
    for (int index = 0; index < 9; index++) total = total * 3 + index;
    return total + ChangingTable[seed & 0xffff];
}

int ChangingExtra(int seed)
{
    return ChangingValue(seed) ^ 0x5a5a;
}
EOF

build()
{
    local source="$1"
    local soname="$2"
    local output="$3"

    if ! "$CLANG" -O2 -fPIC -shared -march="$GUEST_ARCH" -Wl,-soname,"$soname" -o "$output" "$source"; then
        echo "gate FAILED: $source did not compile for the guest" >&2
        exit 1
    fi
}

package()
{
    local stage="$1"
    local apk="$2"

    if ! env FIXTURE_NATIVE_LIBS="$stage" "$PROJ_DIR/tools/build-fixture-apk.sh" "$PROJ_DIR/tests/minimal" "$apk" > /dev/null; then
        echo "gate FAILED: the fixture apk did not build" >&2
        exit 1
    fi
}

translate()
{
    local apk="$1"
    local cacheRoot="$2"
    local placement="${3:-1}"

    local imagePlacement=()
    [ "$placement" = "0" ] && imagePlacement=(--no-image-placement)

    "$launcher" \
        --cache-root "$cacheRoot/.cache/acl" \
        "${imagePlacement[@]}" \
        --host-root "$hostRoot" \
        --framework-jar "$frameworkJar" \
        --data-root "$cacheRoot/data" \
        --translate-only "$apk" 2>&1
}

cacheState()
{
    printf '%s\n' "$1" | grep -oE "translated [^ ]*lib(stable|changing)\.so ahead of time.*cache (hit|miss)" \
        | sed -E 's#.*/(lib[a-z]+\.so) ahead of time.*cache (hit|miss)#\1 \2#'
}

mkdir -p "$work/stage/arm64-v8a" "$work/cache"

build "$work/stable.c" libstable.so "$work/stage/arm64-v8a/libstable.so"
build "$work/changing-first.c" libchanging.so "$work/stage/arm64-v8a/libchanging.so"
package "$work/stage" "$work/first.apk"

echo "=== the first install: nothing is cached yet"

first="$(translate "$work/first.apk" "$work/cache")"
firstState="$(cacheState "$first")"

printf '%s\n' "$firstState"

if [ -z "$firstState" ]; then
    echo "gate FAILED: neither library was translated on the first run" >&2
    printf '%s\n' "$first" | tail -25 >&2
    exit 1
fi

if printf '%s\n' "$firstState" | grep -q "hit"; then
    echo "gate FAILED: something was a cache hit on a cold cache" >&2
    exit 1
fi

echo "=== the same install again: both libraries come back from the cache"

again="$(translate "$work/first.apk" "$work/cache")"
againState="$(cacheState "$again")"

printf '%s\n' "$againState"

if printf '%s\n' "$againState" | grep -q "miss"; then
    echo "gate FAILED: an unchanged package retranslated something" >&2
    printf '%s\n' "$again" | grep -E "ahead of time" >&2
    exit 1
fi

echo "=== the update: one library changes and grows, the other does not"

build "$work/changing-second.c" libchanging.so "$work/stage/arm64-v8a/libchanging.so"
package "$work/stage" "$work/second.apk"

updated="$(translate "$work/second.apk" "$work/cache")"
updatedState="$(cacheState "$updated")"

printf '%s\n' "$updatedState"

if ! printf '%s\n' "$updatedState" | grep -q "^libchanging.so miss$"; then
    echo "gate FAILED: the library that changed was not retranslated" >&2
    printf '%s\n' "$updated" | grep -E "ahead of time" >&2
    exit 1
fi

if ! printf '%s\n' "$updatedState" | grep -q "^libstable.so hit$"; then
    echo "gate FAILED: the library that did not change was retranslated, so an update still costs the whole package" >&2
    printf '%s\n' "$updated" | grep -E "ahead of time" >&2
    exit 1
fi

echo "=== the same update against a bump allocator, which is what this change replaced"

mkdir -p "$work/bump"

translate "$work/first.apk" "$work/bump" 0 > /dev/null
bumpUpdated="$(translate "$work/second.apk" "$work/bump" 0)"
bumpState="$(cacheState "$bumpUpdated")"

printf '%s\n' "$bumpState"

if printf '%s\n' "$bumpState" | grep -q "^libstable.so hit$"; then
    echo "gate FAILED: the bump allocator also kept libstable.so, so this fixture does not exercise the problem" >&2
    echo "             pick a library ordering where growth actually shifts the one that did not change" >&2
    exit 1
fi

echo "note: with placement by load order the unchanged library missed too, which is the cost this removes"

echo "gate PASSED: an update retranslates only the library that changed"

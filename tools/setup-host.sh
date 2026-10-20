#!/usr/bin/env bash
set -uo pipefail

PROJ_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLCHAIN_DIR="${ACL_TOOLCHAIN_DIR:-$PROJ_DIR/.toolchain}"

AOSP_DIR="$TOOLCHAIN_DIR/aosp/master-art"
SDK_DIR="$TOOLCHAIN_DIR/android-sdk"
ENV_FILE="$TOOLCHAIN_DIR/env.sh"

AOSP_MANIFEST="${ACL_AOSP_MANIFEST:-https://android.googlesource.com/platform/manifest}"
AOSP_BRANCH="${ACL_AOSP_BRANCH:-master-art}"
SDK_PLATFORM="${ACL_SDK_PLATFORM:-android-34}"
SDK_BUILD_TOOLS="${ACL_SDK_BUILD_TOOLS:-34.0.0}"
SDK_TOOLS_URL="${ACL_SDK_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip}"

JOBS="${JOBS:-16}"

checkOnly=0
phases=()

for argument in "$@"; do
    case "$argument" in
        --check) checkOnly=1 ;;
        --help|-h)
            echo "setup-host.sh [--check] [phase ...]"
            echo "  phases: packages aosp art sdk env"
            echo "  everything lands in .toolchain/ rather than spread over the home directory"
            exit 0
            ;;
        packages|aosp|art|sdk|env) phases+=("$argument") ;;
        *)
            echo "unknown argument: $argument" >&2
            exit 2
            ;;
    esac
done

if [ ${#phases[@]} -eq 0 ]; then
    phases=(packages aosp art sdk env)
fi

wants()
{
    local wanted="$1"
    local phase
    for phase in "${phases[@]}"; do
        [ "$phase" = "$wanted" ] && return 0
    done
    return 1
}

say()
{
    printf '\n=== %s ===\n' "$1"
}


REQUIRED_BINARIES=(clang clang++ cmake ninja javac java ld.lld objdump unzip curl git python3 repo)
OPTIONAL_BINARIES=(weston llvm-mc ccache adb)
REQUIRED_MODULES=(zlib sqlite3 freetype2 harfbuzz libpng libjpeg libwebp wayland-client wayland-scanner
                  wayland-protocols xkbcommon egl glesv2 wayland-egl vulkan libpipewire-0.3 sdl3)

missingBinaries=()
missingModules=()

surveyPackages()
{
    missingBinaries=()
    missingModules=()

    local binary
    for binary in "${REQUIRED_BINARIES[@]}"; do
        command -v "$binary" >/dev/null 2>&1 || missingBinaries+=("$binary")
    done

    if command -v pkg-config >/dev/null 2>&1; then
        local module
        for module in "${REQUIRED_MODULES[@]}"; do
            pkg-config --exists "$module" 2>/dev/null || missingModules+=("$module")
        done
    else
        missingBinaries+=(pkg-config)
    fi
}

llvmVersionOk()
{
    command -v llvm-config >/dev/null 2>&1 || return 1
    local major
    major="$(llvm-config --version 2>/dev/null | cut -d. -f1)"
    [ -n "$major" ] && [ "$major" -ge 20 ] 2>/dev/null
}

installHint()
{
    if command -v dnf >/dev/null 2>&1; then
        echo "sudo dnf install -y clang lld llvm llvm-devel cmake ninja-build pkgconf-pkg-config \\"
        echo "  binutils java-25-openjdk-devel weston unzip android-tools ccache \\"
        echo "  'pkgconfig(zlib)' 'pkgconfig(sqlite3)' 'pkgconfig(freetype2)' 'pkgconfig(harfbuzz)' \\"
        echo "  'pkgconfig(libpng)' 'pkgconfig(libjpeg)' 'pkgconfig(libwebp)' \\"
        echo "  'pkgconfig(wayland-client)' 'pkgconfig(wayland-scanner)' \\"
        echo "  'pkgconfig(wayland-protocols)' 'pkgconfig(xkbcommon)' \\"
        echo "  'pkgconfig(egl)' 'pkgconfig(glesv2)' 'pkgconfig(wayland-egl)' \\"
        echo "  'pkgconfig(vulkan)' vulkan-headers 'pkgconfig(libpipewire-0.3)' 'pkgconfig(sdl3)'"
        return
    fi

    if command -v pacman >/dev/null 2>&1; then
        echo "sudo pacman -S --needed clang lld llvm cmake ninja pkgconf binutils jdk-openjdk \\"
        echo "  weston unzip android-tools ccache zlib sqlite freetype2 harfbuzz libpng libjpeg-turbo \\"
        echo "  libwebp wayland wayland-protocols libxkbcommon mesa vulkan-headers vulkan-icd-loader \\"
        echo "  libpipewire sdl3"
        return
    fi

    if command -v apt-get >/dev/null 2>&1; then
        echo "sudo apt install -y clang lld llvm-dev cmake ninja-build pkg-config binutils \\"
        echo "  default-jdk weston unzip adb ccache zlib1g-dev libsqlite3-dev libfreetype-dev \\"
        echo "  libharfbuzz-dev libpng-dev libjpeg-dev libwebp-dev libwayland-dev \\"
        echo "  wayland-protocols libxkbcommon-dev libegl-dev libgles-dev libvulkan-dev \\"
        echo "  libpipewire-0.3-dev libsdl3-dev"
        return
    fi

    echo "(unrecognised package manager - install the missing items above by hand)"
}

phasePackages()
{
    say "packages"
    surveyPackages

    local binary
    for binary in "${OPTIONAL_BINARIES[@]}"; do
        command -v "$binary" >/dev/null 2>&1 || echo "optional, not installed: $binary"
    done

    if ! llvmVersionOk; then
        echo "LLVM 20 or newer is required; llvm-config reports $(llvm-config --version 2>/dev/null || echo 'nothing')"
    fi

    if [ ${#missingBinaries[@]} -eq 0 ] && [ ${#missingModules[@]} -eq 0 ] && llvmVersionOk; then
        echo "all system packages present"
        return 0
    fi

    [ ${#missingBinaries[@]} -gt 0 ] && echo "missing programs: ${missingBinaries[*]}"
    [ ${#missingModules[@]} -gt 0 ] && echo "missing pkg-config modules: ${missingModules[*]}"

    echo
    echo "install them with:"
    echo
    installHint
    echo
    echo "this script does not run sudo for you; re-run it once the above has finished"
    return 1
}


phaseAosp()
{
    say "aosp"

    if [ -d "$AOSP_DIR/.repo" ]; then
        echo "repo checkout already at $AOSP_DIR"
    else
        mkdir -p "$AOSP_DIR" || return 1
        echo "initialising $AOSP_BRANCH into $AOSP_DIR"

        (cd "$AOSP_DIR" && repo init -u "$AOSP_MANIFEST" -b "$AOSP_BRANCH" \
            --depth=1 --partial-clone --clone-filter=blob:limit=10M) || return 1
    fi

    local projects
    projects="$(cd "$AOSP_DIR" && repo manifest 2>/dev/null | grep -c "<project")"
    echo "manifest resolves to $projects projects"

    if [ "$projects" -gt 400 ]; then
        echo "that is far more than the reduced art manifest - check ACL_AOSP_BRANCH" >&2
        return 1
    fi

    echo "syncing; this is the long part and is safe to interrupt and re-run"
    AOSP_DIR="$AOSP_DIR" JOBS="$JOBS" "$PROJ_DIR/tools/sync-aosp.sh"
}


phaseArt()
{
    say "art"

    if [ ! -d "$AOSP_DIR/.repo" ]; then
        echo "no aosp checkout at $AOSP_DIR - run the aosp phase first" >&2
        return 1
    fi

    AOSP_DIR="$AOSP_DIR" "$PROJ_DIR/tools/build-art-host.sh" || return 1
    AOSP_DIR="$AOSP_DIR" "$PROJ_DIR/tools/extract-icu-data.sh"
}


phaseSdk()
{
    say "sdk"

    local manager="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

    if [ ! -x "$manager" ]; then
        mkdir -p "$SDK_DIR/cmdline-tools" || return 1

        local archive="$TOOLCHAIN_DIR/cmdline-tools.zip"
        echo "fetching the command line tools"

        if ! curl -fL --retry 3 -o "$archive" "$SDK_TOOLS_URL"; then
            echo "could not fetch $SDK_TOOLS_URL" >&2
            echo "that url carries a build number and goes stale; pass a current one in ACL_SDK_TOOLS_URL" >&2
            return 1
        fi

        unzip -q -o "$archive" -d "$SDK_DIR/cmdline-tools" || return 1
        rm -f "$archive"

        if [ -d "$SDK_DIR/cmdline-tools/cmdline-tools" ]; then
            rm -rf "$SDK_DIR/cmdline-tools/latest"
            mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest" || return 1
        fi
    fi

    echo "installing platform $SDK_PLATFORM and build-tools $SDK_BUILD_TOOLS"

    yes 2>/dev/null | "$manager" --sdk_root="$SDK_DIR" --licenses >/dev/null 2>&1
    "$manager" --sdk_root="$SDK_DIR" "platforms;$SDK_PLATFORM" "build-tools;$SDK_BUILD_TOOLS"
}


phaseEnv()
{
    say "env"

    mkdir -p "$TOOLCHAIN_DIR" || return 1

    cat > "$ENV_FILE" <<ENVEOF
export AOSP_DIR="$AOSP_DIR"
export RUNTIME_HOST_ROOT="$AOSP_DIR/out/host/linux-x86"
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export RUNTIME_FRAMEWORK_RES="$SDK_DIR/platforms/$SDK_PLATFORM/android.jar"
export PATH="\$HOME/.local/bin:\$PATH"
ENVEOF

    echo "wrote $ENV_FILE"
}


report()
{
    say "status"

    surveyPackages
    local packagesOk="yes"
    { [ ${#missingBinaries[@]} -gt 0 ] || [ ${#missingModules[@]} -gt 0 ] || ! llvmVersionOk; } && packagesOk="NO"

    local aospOk="NO"
    [ -d "$AOSP_DIR/.repo" ] && [ -d "$AOSP_DIR/art" ] && aospOk="yes"

    local artOk="NO"
    [ -f "$AOSP_DIR/out/host/linux-x86/lib64/libart.so" ] && artOk="yes"

    local d8Ok="NO"
    [ -f "$AOSP_DIR/out/host/linux-x86/framework/d8.jar" ] && d8Ok="yes"

    local icuOk="NO"
    ls "$AOSP_DIR/out/host/linux-x86/com.android.i18n/etc/icu/"*.dat >/dev/null 2>&1 && icuOk="yes"

    local sdkOk="NO"
    [ -f "$SDK_DIR/platforms/$SDK_PLATFORM/android.jar" ] && sdkOk="yes"

    local buildToolsOk="NO"
    [ -d "$SDK_DIR/build-tools/$SDK_BUILD_TOOLS" ] && buildToolsOk="yes"

    local envOk="NO"
    [ -f "$ENV_FILE" ] && envOk="yes"

    printf '  %-22s %s\n' "system packages" "$packagesOk"
    printf '  %-22s %s\n' "aosp checkout" "$aospOk"
    printf '  %-22s %s\n' "host art built" "$artOk"
    printf '  %-22s %s\n' "d8.jar" "$d8Ok"
    printf '  %-22s %s\n' "icu data" "$icuOk"
    printf '  %-22s %s\n' "sdk platform" "$sdkOk"
    printf '  %-22s %s\n' "sdk build-tools" "$buildToolsOk"
    printf '  %-22s %s\n' "env.sh" "$envOk"
    printf '\n  toolchain root: %s\n' "$TOOLCHAIN_DIR"

    [ "$packagesOk" = "yes" ] && [ "$artOk" = "yes" ] && [ "$d8Ok" = "yes" ] && [ "$sdkOk" = "yes" ]
}


if [ "$checkOnly" -eq 1 ]; then
    report
    exit $?
fi

mkdir -p "$TOOLCHAIN_DIR" || exit 1

wants packages && { phasePackages || exit 1; }
wants aosp && { phaseAosp || exit 1; }
wants art && { phaseArt || exit 1; }
wants sdk && { phaseSdk || exit 1; }
wants env && { phaseEnv || exit 1; }

report
status=$?

if [ $status -eq 0 ]; then
    cat <<'DONEEOF'

setup complete. build with:

  cmake -B build -G Ninja
  cmake --build build -j16
  ctest --test-dir build

CMake picks .toolchain up on its own. For the gates and for running an apk, source the env first:

  source .toolchain/env.sh
DONEEOF
fi

exit $status

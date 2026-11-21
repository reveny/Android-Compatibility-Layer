#!/usr/bin/env python3
import os
import shutil
import subprocess
import sys
from shutil import which

PROJ_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOLCHAIN_DIR = os.path.join(PROJ_DIR, '.toolchain')

AOSP_DIR = f"{TOOLCHAIN_DIR}/aosp/master-art"
SDK_DIR = f"{TOOLCHAIN_DIR}/android-sdk"
ENV_FILE = f"{TOOLCHAIN_DIR}/env.sh"
AOSP_MANIFEST = "https://android.googlesource.com/platform/manifest"
AOSP_BRANCH = "master-art"
SDK_PLATFORM = "android-34"
SDK_BUILD_TOOLS = '34.0.0'
SDK_TOOLS_URL = 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'
JOBS = 16
REQUIRED_BINARIES = ["clang", "clang++", "cmake", "ninja", "javac", "java", "ld.lld", "objdump", "unzip", "curl", "git",
                     "python3", "repo"]
OPTIONAL_BINARIES = ["weston", "llvm-mc", "ccache", "adb"]
REQUIRED_MODULES = ["zlib", "sqlite3", "freetype2", "harfbuzz", "libpng", "libjpeg", "libwebp", "wayland-client",
                    "wayland-scanner", "wayland-protocols", "xkbcommon", "egl", "glesv2", "wayland-egl", "vulkan",
                    "libpipewire-0.3", "sdl3"]

missingBinaries = []

missingModules = []


def say(phase: str = 'main'):
    def decorator(func):
        def wrapper(*args, **kwargs):
            print(f'\n=== {phase} ===\n')
            func(*args, **kwargs)

        return wrapper

    return decorator


def format_size(size):
    for unit in ("B", "K", "M", "G", "T"):
        if size < 1024 or unit == "T":
            return f"{size:.1f}{unit}".replace(".0", "")
        size /= 1024


def show_disk_usage(path="/"):
    usage = shutil.disk_usage(path)
    print("FILESYSTEM\tSIZE\tUSED\tAVAILABLE\tUSED%\tMOUNT")
    print(
        f'{os.path.realpath(path)}\t{format_size(usage.total)}\t{format_size(usage.used)}\t{format_size(usage.free)}\t{usage.used / usage.total * 100:.0f}%')


def surveyPackages():
    for binary in REQUIRED_BINARIES:
        if not which(binary):
            missingBinaries.append(binary)
    if which("pkg-config"):
        for module in REQUIRED_MODULES:
            if subprocess.run(["pkg-config", '--exists', module]).returncode != 0:
                missingModules.append(module)
    else:
        missingBinaries.append("pkg-config")


def installHint():
    if which("dnf"):
        print('''
sudo dnf install -y clang lld llvm llvm-devel cmake ninja-build pkgconf-pkg-config \
  binutils java-25-openjdk-devel weston unzip android-tools ccache \
  'pkgconfig(zlib)' 'pkgconfig(sqlite3)' 'pkgconfig(freetype2)' 'pkgconfig(harfbuzz)' \
  'pkgconfig(libpng)' 'pkgconfig(libjpeg)' 'pkgconfig(libwebp)' \
  'pkgconfig(wayland-client)' 'pkgconfig(wayland-scanner)' \
  'pkgconfig(wayland-protocols)' 'pkgconfig(xkbcommon)' \
  'pkgconfig(egl)' 'pkgconfig(glesv2)' 'pkgconfig(wayland-egl)' \
  'pkgconfig(vulkan)' vulkan-headers 'pkgconfig(libpipewire-0.3)' 'pkgconfig(sdl3)'
        ''')
        return 1
    if which("pacman"):
        print('''
sudo pacman -S --needed clang lld llvm cmake ninja pkgconf binutils jdk-openjdk \
  weston unzip android-tools ccache zlib sqlite freetype2 harfbuzz libpng libjpeg-turbo \
  libwebp wayland wayland-protocols libxkbcommon mesa vulkan-headers vulkan-icd-loader \
  libpipewire sdl3''')
        return 1
    if which("apt-get"):
        print('''
sudo apt install -y clang lld llvm-dev cmake ninja-build pkg-config binutils \
  default-jdk weston unzip adb ccache zlib1g-dev libsqlite3-dev libfreetype-dev \
  libharfbuzz-dev libpng-dev libjpeg-dev libwebp-dev libwayland-dev \
  wayland-protocols libxkbcommon-dev libegl-dev libgles-dev libvulkan-dev \
  libpipewire-0.3-dev libsdl3-dev
''')
        return 1
    print("(unrecognised package manager - install the missing items above by hand)")


@say("packages")
def phasePackages():
    surveyPackages()
    for binary in OPTIONAL_BINARIES:
        if not which(binary): print(f"optional, not installed: {binary}")
    llvmOk, llvmVersion = llvmVersionOk()
    if not llvmOk:
        print(f"LLVM 20 or newer is required; llvm-config reports {llvmVersion if llvmVersion else 'Not Installed'}")
    if not missingBinaries and not missingModules and llvmOk:
        print("all system packages present")
        return 0
    if missingBinaries:
        print("missing binaries", missingBinaries)
    if missingModules:
        print("missing pkg-config modules", missingModules)
    print()
    print("install them with:")
    print()
    installHint()
    print()
    print("this script does not run sudo for you; re-run it once the above has finished")
    return 1

@say("aosp")
def phaseAosp():
    local_dir = os.getcwd()
    os.chdir(AOSP_DIR)
    if os.path.exists(os.path.join(AOSP_DIR, '.repo')):
        print(f"repo checkout already at {AOSP_DIR}")
    else:
        os.makedirs(AOSP_DIR, exist_ok=True)
        print(f"initialising {AOSP_BRANCH} into {AOSP_DIR}")
        subprocess.run(
            ['repo', 'init', '-u', AOSP_MANIFEST, '-b', AOSP_BRANCH, "--depth=1", "--partial-clone",
             '--clone-filter=blob:limit=10M'])
    projects = subprocess.run(
        ["repo", "manifest"],
        capture_output=True,
        text=True,
        check=True,
    )
    projects = [i for i in projects.stdout.split('\n') if i.startswith('<project')]
    print(f"manifest resolves to {len(projects)} projects")
    if len(projects) >= 400:
        print("that is far more than the reduced art manifest - check ACL_AOSP_BRANCH")
        exit(1)
    print("syncing; this is the long part and is safe to interrupt and re-run")
    os.environ["PATH"] = os.environ["PATH"] + ":" + os.environ['HOME'] + "/.local/bin"
    if not os.path.exists(f"{AOSP_DIR}/.repo"):
        print(f"error: no repo checkout at {AOSP_DIR} (run repo init first)")
        exit(1)
    print("=== before ===")
    show_disk_usage()
    p = subprocess.run(
        ["repo", "sync", "-c", f"-j{JOBS}", "--no-tags", "--optimized-fetch", "--prune", "--retry-fetches=3"])
    print("=== after ===")
    show_disk_usage()
    os.chdir(local_dir)
    if p != 0:
        print(f"sync incomplete (exit {p.returncode}) - just run this script again, it resumes.")
        exit(p.returncode)

@say("art")
def phaseArt():
    if not os.path.exists(f"{AOSP_DIR}/.repo"):
        print(f"no aosp checkout at {AOSP_DIR} - run the aosp phase first")
        exit(1)
    os.environ['AOSP_DIR'] = AOSP_DIR
    p = subprocess.run([f"{PROJ_DIR}/tools/build-art-host.sh"])
    if p.returncode != 0:
        exit(p.returncode)
    p = subprocess.run([f"{PROJ_DIR}/tools/extract-icu-data.sh"])
    if p.returncode != 0:
        exit(p.returncode)
@say("sdk")
def phaseSdk():
    manager = f"{SDK_DIR}/cmdline-tools/latest/bin/sdkmanager"
    if not os.access(manager, os.X_OK):
        os.makedirs(f"{SDK_DIR}/cmdline-tools", exist_ok=True)
        archive = f"{TOOLCHAIN_DIR}/cmdline-tools.zip"
        print("fetching the command line tools")
        p = subprocess.run(['curl', '-fL', '--retry', '3', '-o', archive, SDK_TOOLS_URL])
        if p.returncode != 0:
            print(f"could not fetch {SDK_TOOLS_URL}")
            print("that url carries a build number and goes stale; pass a current one in ACL_SDK_TOOLS_URL")
            exit(1)
        p =  subprocess.run(['unzip', '-q', '-o', archive, '-d', f"{SDK_DIR}/cmdline-tools"])
        if p.returncode != 0:
            exit(p.returncode)
        os.remove(archive)
        if os.path.isdir(f"{SDK_DIR}/cmdline-tools/cmdline-tools"):
            shutil.rmtree(f"{SDK_DIR}/cmdline-tools/latest")
            shutil.copytree(f"{SDK_DIR}/cmdline-tools/cmdline-tools", f"{SDK_DIR}/cmdline-tools/latest")
    print(f"installing platform {SDK_PLATFORM} and build-tools {SDK_BUILD_TOOLS}")
    yes_input = "y\n" * 100

    subprocess.run(
        [manager, f"--sdk_root={SDK_DIR}", "--licenses"],
        input=yes_input,
        text=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    subprocess.run([manager, f'--sdk_root={SDK_DIR}', f"platforms;{SDK_PLATFORM}", f"build-tools;{SDK_BUILD_TOOLS}"])

@say("env")
def phaseEnv():
    os.makedirs(TOOLCHAIN_DIR, exist_ok=True)
    with open(ENV_FILE,'w', encoding="utf-8", newline='\n') as f:
        f.write(f'''export AOSP_DIR="{AOSP_DIR}"
export RUNTIME_HOST_ROOT="{AOSP_DIR}/out/host/linux-x86"
export ANDROID_SDK_ROOT="{SDK_DIR}"
export ANDROID_HOME="{SDK_DIR}"
export RUNTIME_FRAMEWORK_RES="{SDK_DIR}/platforms/{SDK_PLATFORM}/android.jar"
export PATH="{os.environ['HOME']}/.local/bin:{os.environ['PATH']}"''')
    print(f"wrote {ENV_FILE}")

@say("status")
def report():
    surveyPackages()
    packagesOk = True
    if missingBinaries or missingModules or not llvmVersionOk()[0]:packagesOk = False
    aospOk = os.path.exists(f"{AOSP_DIR}/.repo") and os.path.exists(f"{AOSP_DIR}/art")
    artOk = os.path.exists(f"{AOSP_DIR}/out/host/linux-x86/lib64/libart.so")
    d8Ok = os.path.exists(f"{AOSP_DIR}/out/host/linux-x86/framework/d8.jar")
    icuOk = len([i for i in os.listdir(f"{AOSP_DIR}/out/host/linux-x86/com.android.i18n/etc/icu/") if i.endswith('.dat')]) > 0
    sdkOk = os.path.exists(f"{SDK_DIR}/platforms/$SDK_PLATFORM/android.jar")
    buildToolsOk = os.path.exists(f"{SDK_DIR}/build-tools/{SDK_BUILD_TOOLS}")
    envOk = os.path.exists(ENV_FILE)
    print('  %-22s %s\n' % ("system packages", packagesOk))
    print('  %-22s %s\n' % ("aosp checkout", aospOk))
    print('  %-22s %s\n' % ("host art built", artOk))
    print('  %-22s %s\n' % ("d8.jar", d8Ok))
    print('  %-22s %s\n' % ("icu data", icuOk))
    print('  %-22s %s\n' % ("sdk platform", sdkOk))
    print('  %-22s %s\n' % ("sdk build-tools", buildToolsOk))
    print('  %-22s %s\n' % ("env.sh", envOk))
    print('\n  toolchain root: %s\n' % TOOLCHAIN_DIR)
    return all([packagesOk, artOk, d8Ok, sdkOk])

def llvmVersionOk():
    if which("llvm-config"):
        result = subprocess.run(
            ["llvm-config", "--version"],
            capture_output=True,
            text=True,
            check=True,
        )
        major = result.stdout.strip().split(".")[0]
        major = int(major)
        if major >= 20:
            return True, major
        return False, major
    else:
        missingBinaries.append("llvm-config")
        return False, 0


def main():
    phases = []
    checkOnly = False
    for argument in sys.argv[1:]:
        if argument == '--check':
            checkOnly = True
        if argument in ['--help', '-h']:
            print("setup-host.sh [--check] [phase ...]")
            print("  phases: packages aosp art sdk env")
            print("  everything lands in .toolchain/ rather than spread over the home directory")
            exit(0)
        if argument in ['packages', 'aosp', 'art', 'sdk', 'env']:
            phases.append(argument)
        else:
            print(f"unknown argument: {argument}")
            exit(2)
    if not phases:
        phases = ['packages', 'aosp', 'art', 'sdk', 'env']
    if checkOnly:
        return report()
    os.makedirs(TOOLCHAIN_DIR, exist_ok=True)
    if 'packages' in phases:phasePackages()
    if 'aosp' in phases:phaseAosp()
    if 'art' in phases:phaseArt()
    if 'sdk' in phases:phaseSdk()
    if 'env' in phases:phaseEnv()
    if report():
        print('''
setup complete. build with:
  cmake -B build -G Ninja
  cmake --build build -j16
  ctest --test-dir build

CMake picks .toolchain up on its own. For the gates and for running an apk, source the env first:
  source .toolchain/env.sh
        ''')
    exit(0)

if __name__ == '__main__':
    main()

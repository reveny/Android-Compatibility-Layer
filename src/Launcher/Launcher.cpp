#include "Launcher/Launcher.hpp"

#include <cctype>
#include <climits>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <iterator>
#include <set>
#include <atomic>
#include <chrono>
#include <dirent.h>
#include <mutex>
#include <fcntl.h>
#include <strings.h>
#include <sys/prctl.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <thread>
#include <unistd.h>

#include "Apk/Apk.hpp"
#include "PackageRegistry/PackageRegistry.hpp"
#include "ControlChannel/ControlChannel.hpp"
#include "Diagnostics/Diagnostics.hpp"
#include "LlvmBackend/LlvmBackend.hpp"
#include "Overlay/FrameRate.hpp"
#include "Overlay/Overlay.hpp"
#include "ArtRuntime/ArtRuntime.hpp"
#include "ArtRuntime/IcuSetup.hpp"
#include "AheadOfTime/AheadOfTime.hpp"
#include "FrameworkJni/FrameworkJni.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestLibc/HostBridge.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "JniBridge/JniBridge.hpp"
#include "NativeActivity/NativeActivity.hpp"
#include "Sandbox/Sandbox.hpp"
#include "Settings/Settings.hpp"
#include "TranslationCache/TranslationCache.hpp"
#if defined(RUNTIME_HAVE_VULKAN)
#include "VulkanWsi/VulkanWsi.hpp"
#endif
#if defined(RUNTIME_HAVE_GLES)
#include "GlesWsi/GlesWsi.hpp"
#endif
#include "Platform/Platform.hpp"

static bool gVerboseConsole = false;
static bool gPretranslationDeclined = false;

static constexpr uint64_t PRETRANSLATE_BUDGET_BYTES = 64ull << 20;
static constexpr int PRETRANSLATION_DECLINED_STATUS = 3;

static constexpr int PERCENT_STEP = 10;

void Launcher::PrintUsage(const char* program)
{
    fprintf(stderr, "usage: %s [--aot|--jit] [--portrait|--landscape] [flags] <apk>\n", program);
    fprintf(stderr, "       %s --install <apk>\n", program);
    fprintf(stderr, "       %s --launch <package> [flags]\n", program);
    fprintf(stderr, "       %s --uninstall <package> [--keep-data]\n", program);
    fprintf(stderr, "       %s --list\n", program);
    fprintf(stderr, "  --portrait    a %dx%d window, which is the default\n", PORTRAIT_WINDOW_WIDTH, PORTRAIT_WINDOW_HEIGHT);
    fprintf(stderr, "  --landscape   the same window turned on its side\n");
    fprintf(stderr, "  --no-vsync    present as fast as the guest can draw, to measure the frame rate\n");
    fprintf(stderr, "  --fps         draw the frame rate onto the app's own frames\n");
    fprintf(stderr, "  --verbose     print everything the runtime does; the default when output is not a terminal\n");
    fprintf(stderr, "  --quiet       print only the headlines; the default at a terminal\n");
    fprintf(stderr, "  --decorated   ask the compositor for a title bar and window frame\n");
    fprintf(stderr, "  --fullscreen  ask the compositor for the whole output\n");
    fprintf(stderr, "  --aot         translate every arm64 image before the app starts, the default\n");
    fprintf(stderr, "  --jit         translate nothing ahead of time: interpret guest code, then compile hot regions as it runs\n");
    fprintf(stderr, "                tune with --setting %s=N (default %llu), %s=N, %s=baseline|optimized, %s=0|1\n",
            JIT_THRESHOLD_SETTING, (unsigned long long)DEFAULT_JIT_THRESHOLD, JIT_REGION_BYTES_SETTING, JIT_TIER_SETTING,
            JIT_BACKGROUND_SETTING);
    fprintf(stderr, "  --install     copy the apk into the data root and record it, then exit\n");
    fprintf(stderr, "  --launch <package>  run a package installed earlier, with its own data directory\n");
    fprintf(stderr, "  --uninstall <package>  forget a package and delete its data\n");
    fprintf(stderr, "  --keep-data   with --uninstall, remove the apk but keep the data directory\n");
    fprintf(stderr, "  --list        print the installed packages, then exit\n");
    fprintf(stderr, "  --network     let the guest reach the network, which it otherwise cannot\n");
    fprintf(stderr, "  --window WxH  a window of exactly this many pixels\n");
    fprintf(stderr, "  --density N   the dots per inch the guest is told the screen has\n");
    fprintf(stderr, "  --strict      abort on any unimplemented framework method instead of returning a default, the default\n");
    fprintf(stderr, "  --no-strict   return a default from unimplemented framework methods and keep running\n");
    fprintf(stderr, "  --trace AREAS log every crossing of the named boundaries: jni,ndk,syscall,guest,sync,audio,all\n");
    fprintf(stderr, "  --log FILE    write the runtime log to a file as well as the terminal\n");
    fprintf(stderr, "  --frame-dump FILE        write the composed frames to a file as raw BGRA\n");
    fprintf(stderr, "  --guest-frame-dump FILE  write the frames the guest itself presented\n");
    fprintf(stderr, "  --max-frames N           stop after this many frames, which is how the gates run\n");
    fprintf(stderr, "  --guest-env K=V          put a variable in the guest's own environment, repeatable\n");
    fprintf(stderr, "  --data-root DIR          where per-package data directories live (default ~/%s)\n", DATA_ROOT_LEAF);
    fprintf(stderr, "  --abi NAME               native ABI to extract and load (default %s)\n", Apk::HOST_NATIVE_ABI);
    fprintf(stderr, "  --host-root DIR          the Android runtime libraries, found beside this binary by default\n");
    fprintf(stderr, "  --framework-jar FILE     framework.jar, found beside this binary by default\n");
    fprintf(stderr, "  --framework-res FILE     android.jar carrying the framework resources\n");
    fprintf(stderr, "  --font-dir DIR           where the fonts live, found beside this binary by default\n");
    fprintf(stderr, "  --cache-root DIR         where translated images are cached (default ~/.cache/acl)\n");
    fprintf(stderr, "  --linker PATH            the linker that joins translated objects, bundled by default\n");
    fprintf(stderr, "  --no-sandbox             do not confine the guest, which is for debugging the runtime only\n");
}

const char* Launcher::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::UsageError: return "UsageError";
        case Result::HostRootMissing: return "HostRootMissing";
        case Result::FrameworkJarMissing: return "FrameworkJarMissing";
        case Result::ApkNotFound: return "ApkNotFound";
        case Result::ApkOpenFailed: return "ApkOpenFailed";
        case Result::FrameworkResourcesUnusable: return "FrameworkResourcesUnusable";
        case Result::IcuSetupFailed: return "IcuSetupFailed";
        case Result::VmStartFailed: return "VmStartFailed";
        case Result::NativeRegistrationFailed: return "NativeRegistrationFailed";
        case Result::BootstrapClassMissing: return "BootstrapClassMissing";
        case Result::BootstrapMethodMissing: return "BootstrapMethodMissing";
        case Result::BootstrapFailed: return "BootstrapFailed";
        case Result::NativeLibraryExtractionFailed: return "NativeLibraryExtractionFailed";
    }
    return "Unknown";
}

const char* Launcher::ResultAdvice(Result result)
{
    switch (result)
    {
        case Result::Ok: return "";
        case Result::UsageError: return "that is not a combination of flags this runtime understands";
        case Result::HostRootMissing:
            return "the Android runtime libraries are missing; a release bundle carries them in art/ beside the binary, "
                   "and a build from source puts them under .toolchain";
        case Result::FrameworkJarMissing:
            return "framework.jar is missing; a release bundle carries it in java/ beside the binary, and a build from "
                   "source leaves it in the build directory";
        case Result::ApkNotFound: return "no file of that name is readable here";
        case Result::ApkOpenFailed: return "the file is not an Android package this runtime can read";
        case Result::FrameworkResourcesUnusable: return "the file named as the framework resource archive cannot be read as one";
        case Result::IcuSetupFailed: return "the bundled ICU data could not be loaded, so text would not render";
        case Result::VmStartFailed: return "the managed runtime refused to start; run again with --verbose for its own diagnosis";
        case Result::NativeRegistrationFailed: return "the framework's native methods could not be registered against this build";
        case Result::BootstrapClassMissing:
        case Result::BootstrapMethodMissing: return "framework.jar does not match this binary; both come from the same build";
        case Result::BootstrapFailed: return "the app stopped during start-up; run again with --verbose to see where";
        case Result::NativeLibraryExtractionFailed: return "the package's native libraries could not be unpacked into its data directory";
    }
    return "";
}

std::string Launcher::DefaultDataRoot()
{
    std::string home = Settings::HomeDirectory();
    if (home.empty()) return std::string("/tmp/") + DATA_ROOT_LEAF;
    return home + "/" + DATA_ROOT_LEAF;
}

static std::string DirectoryOfSelf()
{
    char path[PATH_MAX];
    ssize_t length = readlink("/proc/self/exe", path, sizeof(path) - 1);

    if (length <= 0) return std::string();

    path[length] = '\0';

    std::string full(path);
    size_t slash = full.find_last_of('/');

    return slash == std::string::npos ? std::string() : full.substr(0, slash);
}

static std::string ParentOf(const std::string& directory)
{
    size_t slash = directory.find_last_of('/');
    if (slash == std::string::npos || slash == 0) return std::string();

    return directory.substr(0, slash);
}

static constexpr int SELF_SEARCH_DEPTH = 6;
static constexpr long MAXIMUM_WINDOW_EDGE = 16384;

static constexpr const char* HOST_ROOT_RELATIVES[] = {"art", "runtime/art", ".toolchain/aosp/master-art/out/host/linux-x86",
                                                      "aosp/master-art/out/host/linux-x86"};

static bool LooksLikeHostRoot(const std::string& directory)
{
    return ArtRuntime::PathExists(directory + "/lib64/" + ArtRuntime::RUNTIME_LIBRARY);
}

static std::string FindBesideSelf(const std::string& start, const char* const* relatives, size_t count,
                                  bool (*acceptable)(const std::string&))
{
    std::string directory = start;

    for (int depth = 0; depth < SELF_SEARCH_DEPTH && !directory.empty(); ++depth)
    {
        for (size_t index = 0; index < count; ++index)
        {
            std::string candidate = directory + "/" + relatives[index];
            if (acceptable(candidate)) return candidate;
        }

        directory = ParentOf(directory);
    }

    return std::string();
}

std::string Launcher::DefaultHostRoot()
{
    std::string directory = DirectoryOfSelf();

    if (!directory.empty())
    {
        std::string bundled = FindBesideSelf(directory, HOST_ROOT_RELATIVES, std::size(HOST_ROOT_RELATIVES), LooksLikeHostRoot);
        if (!bundled.empty()) return bundled;
    }

    std::string home = Settings::HomeDirectory();
    if (home.empty()) return std::string();

    std::string fallback = home + "/aosp/master-art/out/host/linux-x86";
    return LooksLikeHostRoot(fallback) ? fallback : std::string();
}

std::string Launcher::DefaultFrameworkJar()
{
    std::string directory = DirectoryOfSelf();
    if (directory.empty()) return std::string();

    const char* relatives[] = {"/../java/framework.jar", "/java/framework.jar", "/framework.jar"};

    for (const char* relative : relatives)
    {
        std::string candidate = directory + relative;
        if (ArtRuntime::PathExists(candidate)) return candidate;
    }

    return std::string();
}

static constexpr const char* FONT_DIR_RELATIVES[] = {"fonts", "third_party/roboto"};

static bool LooksLikeFontDirectory(const std::string& directory)
{
    return ArtRuntime::PathExists(directory + "/Roboto-Regular.ttf");
}

std::string Launcher::DefaultFontDirectory()
{
    std::string directory = DirectoryOfSelf();
    if (directory.empty()) return std::string();

    return FindBesideSelf(directory, FONT_DIR_RELATIVES, std::size(FONT_DIR_RELATIVES), LooksLikeFontDirectory);
}

static void SetUnlessPresent(const char* name, const std::string& value)
{
    if (Settings::Has(name)) return;

    Settings::Set(name, value);
}

static bool UnsignedSetting(const char* name, uint64_t fallback, uint64_t& value)
{
    value = fallback;

    const char* text = Settings::Lookup(name);
    if (!text || !*text) return true;
    if (!isdigit((unsigned char)text[0])) return false;

    char* end = nullptr;
    unsigned long long parsed = strtoull(text, &end, 10);
    if (!end || *end != '\0' || parsed == ULLONG_MAX) return false;

    value = (uint64_t)parsed;
    return true;
}

static bool JitTier(LlvmBackend::Tier& tier)
{
    tier = LlvmBackend::Tier::Baseline;

    const char* text = Settings::Lookup(Launcher::JIT_TIER_SETTING);
    if (!text || !*text || strcasecmp(text, "baseline") == 0) return true;

    if (strcasecmp(text, "optimized") != 0) return false;

    tier = LlvmBackend::Tier::Optimized;
    return true;
}

static bool JitSettingsValid()
{
    uint64_t threshold = 0;
    uint64_t regionBytes = 0;
    LlvmBackend::Tier tier = LlvmBackend::Tier::Baseline;

    return UnsignedSetting(Launcher::JIT_THRESHOLD_SETTING, Launcher::DEFAULT_JIT_THRESHOLD, threshold) &&
           UnsignedSetting(Launcher::JIT_REGION_BYTES_SETTING, GuestRuntime::DEFAULT_RECOMPILE_REGION_BYTES, regionBytes) &&
           JitTier(tier);
}

static bool ParseWindowSize(const char* text, Launcher::Options& options)
{
    if (!text || !*text) return false;

    char* end = nullptr;
    long width = strtol(text, &end, 10);

    if (!end || (*end != 'x' && *end != 'X') || width <= 0 || width > MAXIMUM_WINDOW_EDGE) return false;

    long height = strtol(end + 1, &end, 10);
    if (!end || *end != '\0' || height <= 0 || height > MAXIMUM_WINDOW_EDGE) return false;

    options.windowWidth = (int)width;
    options.windowHeight = (int)height;

    Settings::Set(Launcher::WINDOW_WIDTH_SETTING, std::to_string(width));
    Settings::Set(Launcher::WINDOW_HEIGHT_SETTING, std::to_string(height));

    return true;
}

static void AppendGuestEnvironment(const char* assignment)
{
    if (!assignment || !*assignment) return;

    const char* existing = Settings::Lookup(GuestLibc::GUEST_ENVIRONMENT_SETTING);

    std::string merged = existing && *existing ? std::string(existing) + ";" : std::string();
    merged += assignment;

    Settings::Set(GuestLibc::GUEST_ENVIRONMENT_SETTING, merged);
}

static void AdoptBundledLinker()
{
    std::string directory = DirectoryOfSelf();
    if (directory.empty()) return;

    const char* configured = Settings::Lookup(AheadOfTime::LINKER_SETTING);

    if (!configured || !*configured)
    {
        std::string bundled = directory + "/" + AheadOfTime::DEFAULT_LINKER;
        if (!ArtRuntime::PathExists(bundled)) return;

        setenv(AheadOfTime::LINKER_SETTING, bundled.c_str(), 1);
    }

    std::string libraries = ParentOf(directory) + "/lib";
    if (ArtRuntime::PathExists(libraries)) setenv(AheadOfTime::LINKER_LIBRARY_PATH_SETTING, libraries.c_str(), 0);
}

static std::string SdkPlatformLeaf()
{
    return std::string("platforms/android-") + std::to_string(Platform::DECLARED_SDK_VERSION) + "/android.jar";
}

static bool IsReadableFile(const std::string& path)
{
    return ArtRuntime::PathExists(path);
}

std::string Launcher::DefaultFrameworkResources()
{
    std::string directory = DirectoryOfSelf();

    if (!directory.empty())
    {
        std::string toolchainSdk = std::string(".toolchain/android-sdk/") + SdkPlatformLeaf();
        const char* relatives[] = {"android/android.jar", "android.jar", toolchainSdk.c_str()};

        std::string bundled = FindBesideSelf(directory, relatives, std::size(relatives), IsReadableFile);
        if (!bundled.empty()) return bundled;
    }

    std::string home = Settings::HomeDirectory();
    if (home.empty()) return std::string();

    std::string candidate = home + "/" + SDK_ROOT_LEAF + "/" + SdkPlatformLeaf();
    return ArtRuntime::PathExists(candidate) ? candidate : std::string();
}

bool Launcher::EnsureDirectory(const std::string& path)
{
    if (path.empty()) return false;

    std::string partial;
    for (size_t index = 0; index <= path.size(); index++)
    {
        if (index < path.size() && path[index] != '/')
        {
            partial += path[index];
            continue;
        }

        if (index < path.size())
        {
            partial += '/';
            if (partial == "/") continue;
        }

        std::string directory = partial;
        if (!directory.empty() && directory.back() == '/') directory.pop_back();
        if (directory.empty()) continue;

        if (mkdir(directory.c_str(), 0755) != 0)
        {
            struct stat info;
            if (stat(directory.c_str(), &info) != 0) return false;
            if (!S_ISDIR(info.st_mode)) return false;
        }
    }

    return true;
}

std::string Launcher::DataDirectoryFor(const Options& options, const std::string& packageName)
{
    if (!Apk::IsValidPackageName(packageName)) return std::string();
    return options.dataRoot + "/" + packageName;
}

static char** gArgumentValues = nullptr;
static int gArgumentCount = 0;
static size_t gArgumentBytes = 0;

void Launcher::RememberArguments(int argc, char** argv)
{
    if (argc <= 0 || !argv || !argv[0]) return;

    gArgumentCount = argc;
    gArgumentValues = argv;

    char* start = argv[0];
    char* end = argv[argc - 1] + strlen(argv[argc - 1]) + 1;

    gArgumentBytes = (size_t)(end - start);
}

void Launcher::NameProcessAfterPackage(const std::string& packageName)
{
    if (packageName.empty() || !gArgumentValues || gArgumentBytes == 0) return;

    prctl(PR_SET_NAME, packageName.substr(0, 15).c_str(), 0, 0, 0);

    memset(gArgumentValues[0], 0, gArgumentBytes);
    snprintf(gArgumentValues[0], gArgumentBytes, "%s", packageName.c_str());

    for (int index = 1; index < gArgumentCount; ++index) gArgumentValues[index] = nullptr;
}

static void MakeAbsolute(std::string& path)
{
    if (path.empty() || path[0] == '/') return;

    char resolved[PATH_MAX];
    if (realpath(path.c_str(), resolved)) path = resolved;
}

Launcher::Result Launcher::ManagePackages(const Options& options, int& exitStatus)
{
    exitStatus = 1;

    const std::string indexPath = PackageRegistry::IndexPath(options.dataRoot);

    std::vector<PackageRegistry::Entry> installed;
    PackageRegistry::Load(indexPath, installed);

    if (options.action == Options::Action::List)
    {
        if (installed.empty())
        {
            fprintf(stderr, "[acl] nothing is installed under %s\n", options.dataRoot.c_str());
            exitStatus = 0;
            return Result::Ok;
        }

        for (const PackageRegistry::Entry& entry : installed)
        {
            printf("%-44s %s\n", entry.packageName.c_str(), entry.label.c_str());
        }

        exitStatus = 0;
        return Result::Ok;
    }

    if (options.action == Options::Action::Uninstall)
    {
        const PackageRegistry::Result removed =
            PackageRegistry::Uninstall(options.packageName, options.dataRoot, installed, options.keepData);

        if (removed != PackageRegistry::Result::Ok)
        {
            fprintf(stderr, "[acl] cannot uninstall %s: %s\n", options.packageName.c_str(),
                    PackageRegistry::ResultName(removed));
            return Result::UsageError;
        }

        fprintf(stderr, "[acl] uninstalled %s%s\n", options.packageName.c_str(),
                options.keepData ? ", keeping its data" : " and its data");

        exitStatus = 0;
        return Result::Ok;
    }

    PackageRegistry::Entry entry;
    const PackageRegistry::Result added =
        PackageRegistry::Install(options.apkPath, options.dataRoot, options.frameworkResources, installed, entry);

    if (added != PackageRegistry::Result::Ok)
    {
        fprintf(stderr, "[acl] cannot install %s: %s\n", options.apkPath.c_str(), PackageRegistry::ResultName(added));
        return Result::UsageError;
    }

    fprintf(stderr, "[acl] installed %s (%s) at %s\n", entry.packageName.c_str(), entry.label.c_str(), entry.apkPath.c_str());
    fprintf(stderr, "[acl] its data lives at %s; run it with --launch %s\n", entry.dataPath.c_str(),
            entry.packageName.c_str());

    exitStatus = 0;
    return Result::Ok;
}

Launcher::Result Launcher::ParseArguments(int argc, char** argv, Options& options)
{
    const char* apkPath = nullptr;
    bool verbosityChosen = false;

    for (int index = 1; index < argc; ++index)
    {
        const char* argument = argv[index];

        if (strcmp(argument, "--landscape") == 0 || strcmp(argument, "--horizontal") == 0)
        {
            options.landscape = true;
            continue;
        }

        if (strcmp(argument, "--portrait") == 0 || strcmp(argument, "--vertical") == 0)
        {
            options.landscape = false;
            continue;
        }

        if (strcmp(argument, "--no-vsync") == 0)
        {
            options.presentWithoutWaiting = true;
            continue;
        }

        if (strcmp(argument, "--fps") == 0)
        {
            options.reportFrameRate = true;
            continue;
        }

        if (strcmp(argument, "--verbose") == 0)
        {
            options.verbose = true;
            verbosityChosen = true;
            continue;
        }

        if (strcmp(argument, "--quiet") == 0)
        {
            options.verbose = false;
            verbosityChosen = true;
            continue;
        }

        if (strcmp(argument, "--decorated") == 0)
        {
            options.decorated = true;
            continue;
        }

        if (strcmp(argument, "--fullscreen") == 0)
        {
            options.fullscreen = true;
            continue;
        }

        if (strcmp(argument, "--aot") == 0)
        {
            options.aheadOfTime = true;
            continue;
        }

        if (strcmp(argument, "--jit") == 0)
        {
            options.aheadOfTime = false;
            continue;
        }

        if (strcmp(argument, "--install") == 0)
        {
            options.action = Options::Action::Install;
            continue;
        }

        if (strcmp(argument, "--uninstall") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;

            options.action = Options::Action::Uninstall;
            options.packageName = argv[++index];
            continue;
        }

        if (strcmp(argument, "--launch") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;

            options.packageName = argv[++index];
            continue;
        }

        if (strcmp(argument, "--list") == 0)
        {
            options.action = Options::Action::List;
            continue;
        }

        if (strcmp(argument, "--keep-data") == 0)
        {
            options.keepData = true;
            continue;
        }

        if (strcmp(argument, "--strict") == 0)
        {
            Settings::Set(FrameworkJni::STRICT_SETTING, "1");
            continue;
        }

        if (strcmp(argument, "--no-strict") == 0)
        {
            Settings::Set(FrameworkJni::STRICT_SETTING, "0");
            continue;
        }

        if (strcmp(argument, "--require-translated") == 0)
        {
            Settings::Set(REQUIRE_TRANSLATED_SETTING, "1");
            continue;
        }

        if (strcmp(argument, "--no-sandbox") == 0)
        {
            Settings::Set(NO_SANDBOX_SETTING, "1");
            continue;
        }

        if (strcmp(argument, "--no-image-placement") == 0)
        {
            Settings::Set(GuestRuntime::IMAGE_PLACEMENT_SETTING, "0");
            continue;
        }

        if (strcmp(argument, "--window") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            if (!ParseWindowSize(argv[++index], options)) return Result::UsageError;
            continue;
        }

        if (strcmp(argument, "--guest-env") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            AppendGuestEnvironment(argv[++index]);
            continue;
        }

        if (strcmp(argument, "--host-root") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            options.hostRoot = argv[++index];
            continue;
        }

        if (strcmp(argument, "--framework-jar") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            options.frameworkJar = argv[++index];
            continue;
        }

        if (strcmp(argument, "--framework-res") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            options.frameworkResources = argv[++index];
            options.frameworkResourcesRequested = true;
            continue;
        }

        if (strcmp(argument, "--data-root") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            options.dataRoot = argv[++index];
            continue;
        }

        if (strcmp(argument, "--abi") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            options.nativeAbi = argv[++index];
            options.nativeAbiRequested = true;
            continue;
        }

        if (strcmp(argument, "--font-dir") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(FONT_DIR_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--cache-root") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(AheadOfTime::CACHE_ROOT_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--linker") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(AheadOfTime::LINKER_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--guest-shim") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(GUEST_SHIM_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--density") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(Platform::DENSITY_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--trace") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(FrameworkJni::TRACE_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--frame-dump") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(FRAME_DUMP_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--guest-frame-dump") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(Overlay::CAPTURE_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--max-frames") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(MAX_FRAMES_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--activity") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(ACTIVITY_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--no-activity") == 0)
        {
            Settings::Set(LAUNCH_ACTIVITY_SETTING, "0");
            continue;
        }

        if (strcmp(argument, "--reconfigure") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(RECONFIGURE_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--no-continuous-frames") == 0)
        {
            Settings::Set(CONTINUOUS_FRAMES_SETTING, "0");
            continue;
        }

        if (strcmp(argument, "--log") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(Diagnostics::FILE_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--tombstone") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(Diagnostics::TOMBSTONE_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--pacing-report") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            Settings::Set(PACING_REPORT_SETTING, argv[++index]);
            continue;
        }

        if (strcmp(argument, "--network") == 0)
        {
            options.network = true;
            continue;
        }

        if (strcmp(argument, "--translate-only") == 0)
        {
            options.translateOnly = true;
            continue;
        }

        if (strcmp(argument, "--setting") == 0)
        {
            if (index + 1 >= argc) return Result::UsageError;
            if (!Settings::Accept(argv[++index])) return Result::UsageError;
            continue;
        }

        if (argument[0] == '-' || apkPath) return Result::UsageError;

        apkPath = argument;
    }

    if (!verbosityChosen) options.verbose = isatty(STDERR_FILENO) == 0;

    if (apkPath) options.apkPath = apkPath;

    MakeAbsolute(options.hostRoot);
    MakeAbsolute(options.frameworkJar);
    MakeAbsolute(options.frameworkResources);
    MakeAbsolute(options.dataRoot);


    if (options.presentWithoutWaiting) SetUnlessPresent(PRESENT_WITHOUT_WAITING_SETTING, "1");
    if (options.reportFrameRate) SetUnlessPresent(FRAME_RATE_SETTING, "1");
    if (options.reportFrameRate) SetUnlessPresent(FrameRate::OVERLAY_SETTING, "1");
    if (options.decorated) SetUnlessPresent(DECORATED_SETTING, "1");
    if (options.fullscreen) SetUnlessPresent(FULLSCREEN_SETTING, "1");
    if (options.network) SetUnlessPresent(NETWORK_SETTING, "1");

    if (!options.aheadOfTime)
    {
        const char* required = Settings::Lookup(REQUIRE_TRANSLATED_SETTING);
        if (required && strcmp(required, "0") != 0)
        {
            fprintf(stderr, "[acl] --jit compiles guest code while it runs, so it cannot also require every block to be translated "
                            "ahead of time\n");
            return Result::UsageError;
        }

        if (!JitSettingsValid())
        {
            fprintf(stderr, "[acl] %s and %s take a whole number, and %s takes baseline or optimized\n", JIT_THRESHOLD_SETTING,
                    JIT_REGION_BYTES_SETTING, JIT_TIER_SETTING);
            return Result::UsageError;
        }
    }

    if (options.windowWidth <= 0 || options.windowHeight <= 0)
    {
        options.windowWidth = options.landscape ? PORTRAIT_WINDOW_HEIGHT : PORTRAIT_WINDOW_WIDTH;
        options.windowHeight = options.landscape ? PORTRAIT_WINDOW_WIDTH : PORTRAIT_WINDOW_HEIGHT;
    }

    if (options.hostRoot.empty()) options.hostRoot = DefaultHostRoot();
    if (options.hostRoot.empty()) return Result::HostRootMissing;

    if (options.frameworkJar.empty()) options.frameworkJar = DefaultFrameworkJar();
    if (options.frameworkJar.empty()) return Result::FrameworkJarMissing;

    if (options.dataRoot.empty()) options.dataRoot = DefaultDataRoot();
    if (options.frameworkResources.empty()) options.frameworkResources = DefaultFrameworkResources();

    if (!Settings::Has(FONT_DIR_SETTING))
    {
        std::string bundledFonts = DefaultFontDirectory();
        if (!bundledFonts.empty()) Settings::Set(FONT_DIR_SETTING, bundledFonts);
    }

    AdoptBundledLinker();

    options.traceJni = FrameworkJni::IsTracing("jni");

    if (options.action == Options::Action::List) return Result::Ok;

    if (options.action == Options::Action::Uninstall)
    {
        if (options.packageName.empty() || !options.apkPath.empty()) return Result::UsageError;

        return Result::Ok;
    }

    if (options.apkPath.empty())
    {
        if (options.packageName.empty() || options.action == Options::Action::Install) return Result::UsageError;

        std::vector<PackageRegistry::Entry> installed;
        PackageRegistry::Entry found;

        if (PackageRegistry::Load(PackageRegistry::IndexPath(options.dataRoot), installed) != PackageRegistry::Result::Ok
            || !PackageRegistry::Find(installed, options.packageName, found))
        {
            fprintf(stderr, "[acl] %s is not installed; run acl --install <apk> first\n", options.packageName.c_str());
            return Result::UsageError;
        }

        options.apkPath = found.apkPath;
    }

    MakeAbsolute(options.apkPath);
    Apk::DiscoverSplits(options.apkPath, options.splitPaths);

    return Result::Ok;
}

struct TranslationSet
{
    std::deque<AheadOfTime::Image> images;
    std::set<uint32_t> translated;
};

static bool TranslateGuestLibrary(void* context, GuestRuntime::Instance& runtime, uint32_t moduleId, const char* imagePath);

static void AddPathIfPresent(std::vector<std::string>& paths, const std::string& path)
{
    if (path.empty()) return;

    struct stat details;
    if (stat(path.c_str(), &details) != 0) return;

    paths.push_back(path);
}

static std::string SharedLibraryJar(const std::string& frameworkJar, const std::string& library)
{
    size_t slash = frameworkJar.find_last_of('/');
    if (slash == std::string::npos) return std::string();

    std::string candidate = frameworkJar.substr(0, slash + 1) + library + ".jar";
    return ArtRuntime::PathExists(candidate) ? candidate : std::string();
}

static bool UsesSharedLibrary(const Apk::Manifest& manifest, const std::string& library)
{
    for (const std::string& declared : manifest.usesLibraries)
    {
        if (declared == library) return true;
    }

    return library == Launcher::APACHE_HTTP_LIBRARY && manifest.targetSdkVersion != 0 &&
           manifest.targetSdkVersion < Launcher::APACHE_HTTP_IMPLICIT_BEFORE_SDK;
}

static void AddParentIfRequested(std::vector<std::string>& paths, const char* variable)
{
    const char* value = Settings::Lookup(variable);
    if (!value || !*value) return;

    std::string path = value;
    size_t slash = path.find_last_of('/');

    AddPathIfPresent(paths, slash == std::string::npos ? std::string(".") : path.substr(0, slash));
}

static bool PretranslationRequested(const Launcher::Options& options)
{
    if (!options.aheadOfTime) return false;

    const char* setting = Settings::Lookup(Launcher::PRETRANSLATE_SETTING);
    return !setting || strcmp(setting, "0") != 0;
}

static bool ConfinementDisabled()
{
    const char* disabled = Settings::Lookup(Launcher::NO_SANDBOX_SETTING);
    return disabled && strcmp(disabled, "0") != 0;
}

static constexpr const char* PACKAGE_DATA_FOLDERS[] = {"files", "cache", "code_cache", "databases", "shared_prefs", "no_backup"};

static constexpr const char* EXTERNAL_STORAGE_FOLDERS[] = {"Alarms",   "DCIM",      "Documents",     "Download",    "Movies",
                                                           "Music",    "Pictures",  "Podcasts",      "Ringtones",   "Notifications",
                                                           "Android",  "Android/data", "Android/media", "Android/obb"};

static constexpr mode_t PACKAGE_DATA_MODE = 0700;
static constexpr mode_t PACKAGE_FOLDER_MODE = 0771;
static constexpr mode_t EXTERNAL_FOLDER_MODE = 0770;

static void BuildAndroidDirectories(const std::string& dataDirectory, const std::string& externalDirectory,
                                    const std::string& packageName)
{
    chmod(dataDirectory.c_str(), PACKAGE_DATA_MODE);

    for (const char* leaf : PACKAGE_DATA_FOLDERS)
    {
        std::string folder = dataDirectory + "/" + leaf;

        Launcher::EnsureDirectory(folder);
        chmod(folder.c_str(), PACKAGE_FOLDER_MODE);
    }

    if (externalDirectory.empty()) return;

    Launcher::EnsureDirectory(externalDirectory);
    chmod(externalDirectory.c_str(), EXTERNAL_FOLDER_MODE);

    for (const char* leaf : EXTERNAL_STORAGE_FOLDERS)
    {
        std::string folder = externalDirectory + "/" + leaf;

        Launcher::EnsureDirectory(folder);
        chmod(folder.c_str(), EXTERNAL_FOLDER_MODE);
    }

    if (packageName.empty()) return;

    Launcher::EnsureDirectory(externalDirectory + "/Android/data/" + packageName);
    Launcher::EnsureDirectory(externalDirectory + "/Android/data/" + packageName + "/files");
    Launcher::EnsureDirectory(externalDirectory + "/Android/data/" + packageName + "/cache");
    Launcher::EnsureDirectory(externalDirectory + "/Android/obb/" + packageName);
}

static uint64_t ChosenMemoryCeiling()
{
    long pages = sysconf(_SC_PHYS_PAGES);
    long pageBytes = sysconf(_SC_PAGESIZE);
    if (pages <= 0 || pageBytes <= 0) return 0;

    return ((uint64_t)pages * (uint64_t)pageBytes) / 2;
}

static uint64_t ChosenProcessCeiling()
{
    return Launcher::DEFAULT_PROCESS_LIMIT;
}

static void IsolateThisProcess(const Launcher::Options& options)
{
    if (ConfinementDisabled()) return;

    const char* already = Settings::Lookup(Launcher::ISOLATED_SETTING);
    if (already && *already) return;

    if (!Sandbox::IsolationSupported())
    {
        fprintf(stderr, "[launcher] this kernel offers no unprivileged namespaces, so the host stays visible\n");
        return;
    }

    Sandbox::Isolation isolation;
    isolation.allowNetwork = options.network;
    isolation.memoryBytes = ChosenMemoryCeiling();
    isolation.processLimit = ChosenProcessCeiling();
    isolation.applicationUserId = Settings::Lookup(Launcher::APP_USER_ID_SETTING) ? Launcher::APPLICATION_USER_ID : 0;

    AddPathIfPresent(isolation.readablePaths, options.hostRoot);
    AddPathIfPresent(isolation.readablePaths, ArtRuntime::ConscryptPrebuiltDirectory(options.hostRoot));
    AddPathIfPresent(isolation.readablePaths, options.frameworkJar);
    AddPathIfPresent(isolation.readablePaths, SharedLibraryJar(options.frameworkJar, Launcher::APACHE_HTTP_LIBRARY));
    AddPathIfPresent(isolation.readablePaths, options.frameworkResources);
    AddPathIfPresent(isolation.readablePaths, options.apkPath);

    for (const std::string& split : options.splitPaths) AddPathIfPresent(isolation.readablePaths, split);

    AddPathIfPresent(isolation.readablePaths, "/usr");
    AddPathIfPresent(isolation.readablePaths, "/etc");
    AddPathIfPresent(isolation.readablePaths, "/sys");
    AddPathIfPresent(isolation.readablePaths, RUNTIME_DEFAULT_FONT_DIR);

    const char* fontDirectory = Settings::Lookup(Launcher::FONT_DIR_SETTING);
    if (fontDirectory) AddPathIfPresent(isolation.readablePaths, fontDirectory);

    char self[PATH_MAX];
    ssize_t selfLength = readlink("/proc/self/exe", self, sizeof(self) - 1);
    if (selfLength > 0)
    {
        self[selfLength] = '\0';
        std::string binary = self;
        size_t slash = binary.find_last_of('/');
        if (slash != std::string::npos) AddPathIfPresent(isolation.readablePaths, binary.substr(0, slash));
    }

    Launcher::EnsureDirectory(options.dataRoot);
    AddPathIfPresent(isolation.writablePaths, options.dataRoot);

    std::string cacheRoot;
    if (TranslationCache::DefaultRoot(cacheRoot) == TranslationCache::Result::Ok)
    {
        Launcher::EnsureDirectory(cacheRoot);
        AddPathIfPresent(isolation.writablePaths, cacheRoot);
    }

    std::string home = Settings::HomeDirectory();
    if (!home.empty()) AddPathIfPresent(isolation.writablePaths, home + "/.cache/nvidia");

    AddPathIfPresent(isolation.writablePaths, Settings::RuntimeDirectory());

    AddParentIfRequested(isolation.writablePaths, Launcher::FRAME_DUMP_SETTING);
    AddParentIfRequested(isolation.writablePaths, Overlay::CAPTURE_SETTING);
    AddParentIfRequested(isolation.writablePaths, Launcher::PACING_REPORT_SETTING);
    AddParentIfRequested(isolation.writablePaths, Diagnostics::FILE_SETTING);
    AddParentIfRequested(isolation.writablePaths, Diagnostics::TOMBSTONE_SETTING);

    AddPathIfPresent(isolation.devicePaths, "/dev/null");
    AddPathIfPresent(isolation.devicePaths, "/dev/zero");
    AddPathIfPresent(isolation.devicePaths, "/dev/full");
    AddPathIfPresent(isolation.devicePaths, "/dev/random");
    AddPathIfPresent(isolation.devicePaths, "/dev/urandom");
    AddPathIfPresent(isolation.devicePaths, "/dev/dri");
    AddPathIfPresent(isolation.devicePaths, "/dev/char");
    AddPathIfPresent(isolation.devicePaths, "/dev/nvidiactl");
    AddPathIfPresent(isolation.devicePaths, "/dev/nvidia0");
    AddPathIfPresent(isolation.devicePaths, "/dev/nvidia-modeset");

    Sandbox::IsolationReport report;
    Sandbox::Result isolated = Sandbox::Isolate(isolation, report);

    if (isolated != Sandbox::Result::Ok)
    {
        fprintf(stderr, "[launcher] the host filesystem stays visible: %s\n", Sandbox::ResultName(isolated));
        return;
    }

    if (gVerboseConsole)
    {
        fprintf(stderr, "[launcher] isolated: %llu paths attached, %llu absent, own root%s%s\n",
                (unsigned long long)report.pathsAttached, (unsigned long long)report.pathsMissing,
                report.networkNamespace ? ", no network" : "", report.processNamespace ? ", own process table" : "");
    }

    if (!gVerboseConsole) return;

    if (report.resourcesGrouped)
    {
        fprintf(stderr, "[launcher] capped at %llu MiB of memory and %llu processes\n",
                (unsigned long long)(report.memoryCeilingBytes >> 20), (unsigned long long)report.processCeiling);
    }
    else
    {
        fprintf(stderr, "[launcher] no resource group was available, so only the process limits apply\n");
    }
}

static bool RunTranslatorProcess(const Launcher::Options& options)
{
    char self[PATH_MAX];
    ssize_t length = readlink("/proc/self/exe", self, sizeof(self) - 1);
    if (length <= 0) return false;

    self[length] = '\0';

    std::vector<std::string> owned;
    owned.push_back(self);
    owned.push_back("--translate-only");
    owned.push_back(options.landscape ? "--landscape" : "--portrait");
    owned.push_back("--setting");
    owned.push_back(std::string(Launcher::ISOLATED_SETTING) + "=1");

    const std::pair<const char*, const std::string*> resolved[] = {
        {"--host-root", &options.hostRoot},
        {"--framework-jar", &options.frameworkJar},
        {"--framework-res", &options.frameworkResources},
        {"--data-root", &options.dataRoot},
        {"--abi", &options.nativeAbi},
    };

    for (const auto& entry : resolved)
    {
        if (entry.second->empty()) continue;

        owned.push_back(entry.first);
        owned.push_back(*entry.second);
    }

    for (size_t entry = 0; entry < Settings::Count(); ++entry)
    {
        std::string name;
        std::string value;
        if (!Settings::At(entry, name, value)) continue;
        if (name == Launcher::ISOLATED_SETTING) continue;

        owned.push_back("--setting");
        owned.push_back(name + "=" + value);
    }

    owned.push_back(options.apkPath);

    std::vector<char*> arguments;
    arguments.reserve(owned.size() + 1);
    for (std::string& text : owned) arguments.push_back((char*)text.c_str());
    arguments.push_back(nullptr);

    pid_t child = fork();
    if (child < 0) return false;

    if (child == 0)
    {
        execv(self, arguments.data());

        fprintf(stderr, "[launcher] cannot start the translator at %s: %s\n", self, strerror(errno));
        fflush(stderr);
        _exit(127);
    }

    int state = 0;
    if (waitpid(child, &state, 0) < 0) return false;

    if (WIFEXITED(state) && WEXITSTATUS(state) == PRETRANSLATION_DECLINED_STATUS) return false;

    if (!WIFEXITED(state) || WEXITSTATUS(state) != 0)
    {
        fprintf(stderr, "[launcher] the translator process did not finish cleanly\n");
        return false;
    }

    return true;
}

static bool StartsWith(const char* line, uint32_t length, const char* prefix)
{
    const size_t prefixLength = strlen(prefix);

    return length >= prefixLength && strncmp(line, prefix, prefixLength) == 0;
}

static bool KeepForConsole(const char* line, uint32_t length)
{
    static const char* HEADLINES[] = {"[acl]", "[overlay]", "[tombstone]", "Fatal signal", "E/", "W/"};
    static bool continuing = false;

    for (const char* prefix : HEADLINES)
    {
        if (!StartsWith(line, length, prefix)) continue;

        continuing = true;
        return true;
    }

    const bool indented = length > 0 && (line[0] == '\t' || line[0] == ' ');
    const bool throwable = StartsWith(line, length, "java.") || StartsWith(line, length, "Caused by:")
                           || StartsWith(line, length, "android.") || StartsWith(line, length, "kotlin.");

    if (continuing && (indented || throwable || length == 0)) return true;

    continuing = false;

    return false;
}

static void ConfineBeforeParsing(const Launcher::Options& options, bool mayExecutePrograms)
{
    if (ConfinementDisabled())
    {
        fprintf(stderr, "\n***********************************************************\n");
        fprintf(stderr, "  RUNNING WITHOUT A SANDBOX - the guest has your full access\n");
        fprintf(stderr, "***********************************************************\n\n");
        return;
    }

    Sandbox::Policy policy;
    policy.allowNetwork = options.network;
    policy.allowProcessExecution = mayExecutePrograms;
    policy.allowMountChanges = !options.translateOnly && Sandbox::RootWasReplaced();
    policy.addressSpaceHeadroomBytes = Sandbox::DEFAULT_ADDRESS_SPACE_HEADROOM_BYTES + GuestRuntime::DEFAULT_ARENA_LENGTH;

    Sandbox::Report report;

    Sandbox::Result resources = Sandbox::RestrictResources(policy, report);
    if (resources != Sandbox::Result::Ok)
        fprintf(stderr, "[launcher] resource limits were not applied: %s\n", Sandbox::ResultName(resources));

    Sandbox::Result syscalls = Sandbox::RestrictSyscalls(policy, report);
    if (syscalls != Sandbox::Result::Ok)
    {
        fprintf(stderr, "[launcher] the syscall filter was not installed before parsing: %s\n", Sandbox::ResultName(syscalls));
        return;
    }

    fprintf(stderr, "[launcher] %llu syscalls denied before the archive is opened, on every thread%s\n",
            (unsigned long long)report.syscallsDenied, mayExecutePrograms ? "" : ", and no program can be executed");

    if (report.addressSpaceCeilingBytes != 0)
    {
        fprintf(stderr, "[launcher] address space ceiling %llu MiB, %llu MiB already mapped, and the guest arena reserves %llu MiB of it\n",
                (unsigned long long)(report.addressSpaceCeilingBytes >> 20), (unsigned long long)(report.addressSpaceInUseBytes >> 20),
                (unsigned long long)(GuestRuntime::DEFAULT_ARENA_LENGTH >> 20));
    }
}

static void ConfineAfterTranslation(bool executionWasAllowed)
{
    if (ConfinementDisabled() || !executionWasAllowed) return;

    Sandbox::Policy policy;
    policy.allowNetwork = false;
    policy.allowProcessExecution = false;

    Sandbox::Report report;
    Sandbox::Result syscalls = Sandbox::RestrictSyscalls(policy, report);

    if (syscalls != Sandbox::Result::Ok)
    {
        fprintf(stderr, "[launcher] execution could not be revoked after translation: %s\n", Sandbox::ResultName(syscalls));
        return;
    }

    fprintf(stderr, "[launcher] translation is finished, so the process gave up executing programs\n");
}

static void ConfineThisProcess(const Launcher::Options& options, const std::string& dataDirectory, const std::string& libraryDirectory,
                               bool mayExecutePrograms)
{
    if (ConfinementDisabled()) return;

    if (!Sandbox::IsSupported())
    {
        fprintf(stderr, "[launcher] this kernel offers no landlock, so the guest cannot be confined\n");
        return;
    }

    Sandbox::Policy policy;
    policy.allowNetwork = options.network;
    policy.allowProcessExecution = mayExecutePrograms;
    policy.addressSpaceHeadroomBytes = Sandbox::DEFAULT_ADDRESS_SPACE_HEADROOM_BYTES + GuestRuntime::DEFAULT_ARENA_LENGTH;

    if (policy.allowNetwork) fprintf(stderr, "[launcher] the guest was given the network on request\n");

    AddPathIfPresent(policy.readablePaths, options.hostRoot);
    AddPathIfPresent(policy.readablePaths, ArtRuntime::ConscryptPrebuiltDirectory(options.hostRoot));
    AddPathIfPresent(policy.readablePaths, options.frameworkJar);
    AddPathIfPresent(policy.readablePaths, SharedLibraryJar(options.frameworkJar, Launcher::APACHE_HTTP_LIBRARY));
    AddPathIfPresent(policy.readablePaths, options.frameworkResources);
    AddPathIfPresent(policy.readablePaths, options.apkPath);
    AddPathIfPresent(policy.readablePaths, libraryDirectory);

    for (const std::string& split : options.splitPaths) AddPathIfPresent(policy.readablePaths, split);

    AddPathIfPresent(policy.readablePaths, "/usr");
    AddPathIfPresent(policy.readablePaths, "/etc");
    AddPathIfPresent(policy.readablePaths, "/sys/module");
    AddPathIfPresent(policy.readablePaths, "/sys/devices");
    AddPathIfPresent(policy.readablePaths, "/proc");

    char self[PATH_MAX];
    ssize_t selfLength = readlink("/proc/self/exe", self, sizeof(self) - 1);
    if (selfLength > 0)
    {
        self[selfLength] = '\0';
        std::string binary = self;
        size_t slash = binary.find_last_of('/');
        if (slash != std::string::npos) AddPathIfPresent(policy.readablePaths, binary.substr(0, slash));
    }
    AddPathIfPresent(policy.readablePaths, RUNTIME_DEFAULT_FONT_DIR);

    const char* fontDirectory = Settings::Lookup(Launcher::FONT_DIR_SETTING);
    if (fontDirectory) AddPathIfPresent(policy.readablePaths, fontDirectory);

    AddPathIfPresent(policy.writablePaths, dataDirectory);

    std::string cacheRoot;
    if (TranslationCache::DefaultRoot(cacheRoot) == TranslationCache::Result::Ok)
    {
        Launcher::EnsureDirectory(cacheRoot);
        AddPathIfPresent(policy.writablePaths, cacheRoot);
    }

    AddPathIfPresent(policy.devicePaths, "/dev/null");
    AddPathIfPresent(policy.devicePaths, "/dev/urandom");

    if (!options.translateOnly)
    {
        std::string home = Settings::HomeDirectory();
        if (!home.empty()) AddPathIfPresent(policy.writablePaths, home + "/.cache/nvidia");

        AddPathIfPresent(policy.writablePaths, Settings::RuntimeDirectory());

        AddParentIfRequested(policy.writablePaths, Launcher::FRAME_DUMP_SETTING);
        AddParentIfRequested(policy.writablePaths, Overlay::CAPTURE_SETTING);
        AddParentIfRequested(policy.writablePaths, Launcher::PACING_REPORT_SETTING);
        AddParentIfRequested(policy.writablePaths, Diagnostics::FILE_SETTING);
        AddParentIfRequested(policy.writablePaths, Diagnostics::TOMBSTONE_SETTING);

        AddPathIfPresent(policy.devicePaths, "/dev/dri");
        AddPathIfPresent(policy.devicePaths, "/dev/char");
        AddPathIfPresent(policy.devicePaths, "/dev/nvidiactl");
        AddPathIfPresent(policy.devicePaths, "/dev/nvidia0");
        AddPathIfPresent(policy.devicePaths, "/dev/nvidia-modeset");
    }

    Sandbox::Report report;

    Sandbox::Policy seal;
    seal.allowNetwork = true;
    seal.allowProcessExecution = true;
    seal.allowMountChanges = false;

    Sandbox::Result sealed = Sandbox::RestrictSyscalls(seal, report);
    if (sealed != Sandbox::Result::Ok)
    {
        fprintf(stderr, "[launcher] the mount table was not sealed: %s\n", Sandbox::ResultName(sealed));
    }

    Sandbox::Result filesystem = Sandbox::RestrictFilesystem(policy, report);

    if (filesystem != Sandbox::Result::Ok)
    {
        fprintf(stderr, "[launcher] the filesystem was not confined: %s\n", Sandbox::ResultName(filesystem));
        return;
    }

    fprintf(stderr, "[launcher] confined: landlock abi %d, %llu readable, %llu writable, %llu device, %llu syscalls denied with the "
                    "mount table sealed\n",
            report.filesystemAbi, (unsigned long long)report.readablePathsApplied, (unsigned long long)report.writablePathsApplied,
            (unsigned long long)report.devicePathsApplied, (unsigned long long)report.syscallsDenied);
}

static bool PretranslationWorthIt(const std::string& directory)
{
    DIR* handle = opendir(directory.c_str());
    if (!handle) return true;

    uint64_t total = 0;
    uint64_t count = 0;

    for (dirent* entry = readdir(handle); entry; entry = readdir(handle))
    {
        std::string leaf = entry->d_name;
        if (leaf.size() < 4 || leaf.compare(leaf.size() - 3, 3, ".so") != 0) continue;

        struct stat details = {};
        if (stat((directory + "/" + leaf).c_str(), &details) != 0) continue;

        total += (uint64_t)details.st_size;
        count += 1;
    }

    closedir(handle);

    if (total <= PRETRANSLATE_BUDGET_BYTES) return true;

    fprintf(stderr, "[acl] %llu guest libraries totalling %llu MiB is more than this would translate up front, so each one is\n",
            (unsigned long long)count, (unsigned long long)(total >> 20));
    fprintf(stderr, "[acl] translated when the app first loads it; pass %s=1 to translate them all before starting\n",
            "RUNTIME_PRETRANSLATE");

    return false;
}

static void PretranslateGuestLibraries(GuestRuntime::Instance& runtime, const std::string& directory, TranslationSet& translations)
{
    if (!PretranslationWorthIt(directory))
    {
        gPretranslationDeclined = true;
        return;
    }

    DIR* handle = opendir(directory.c_str());
    if (!handle) return;

    struct LoadedLibrary
    {
        std::string path;
        uint32_t moduleId;
    };

    std::vector<LoadedLibrary> loaded;

    for (dirent* entry = readdir(handle); entry; entry = readdir(handle))
    {
        std::string leaf = entry->d_name;
        if (leaf.size() < 4 || leaf.compare(leaf.size() - 3, 3, ".so") != 0) continue;

        std::string path = directory + "/" + leaf;

        uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
        if (GuestRuntime::Load(runtime, path, moduleId) != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[launcher] cannot pre-translate %s\n", path.c_str());
            continue;
        }

        loaded.push_back({path, moduleId});
    }

    closedir(handle);

    uint64_t prepared = 0;

    for (const LoadedLibrary& library : loaded)
    {
        if (TranslateGuestLibrary(&translations, runtime, library.moduleId, library.path.c_str())) prepared += 1;
    }

    fprintf(stderr, "[launcher] pre-translated %llu guest librar%s before giving up execution\n", (unsigned long long)prepared,
            prepared == 1 ? "y" : "ies");
}

static void WriteLogIfRequested()
{
    if (!Diagnostics::IsInitialized()) return;

    Diagnostics::ReportGaps();

    const char* path = Settings::Lookup(Diagnostics::FILE_SETTING);
    const Diagnostics::Statistics statistics = Diagnostics::GetStatistics();

    Diagnostics::ReleaseStandardError();

    if (path && path[0] != '\0' && !Diagnostics::Dump(path))
    {
        fprintf(stderr, "[launcher] the log could not be written to %s\n", path);
    }

    if (gVerboseConsole || (path && path[0] != '\0'))
    {
        fprintf(stderr, "[launcher] log: %llu line(s) kept of %llu, %llu dropped, %llu truncated%s%s\n",
                (unsigned long long)statistics.held, (unsigned long long)statistics.written, (unsigned long long)statistics.dropped,
                (unsigned long long)statistics.truncated, path && path[0] != '\0' ? ", written to " : "",
                path && path[0] != '\0' ? path : "");
    }

    Diagnostics::Shutdown();
}

struct LogGuard
{
    ~LogGuard()
    {
        WriteLogIfRequested();
    }
};

static ControlChannel::Endpoint gProgressChannel;
static bool gShellVanished = false;

static void OpenProgressChannel()
{
    if (!ControlChannel::IsConfigured()) return;

    ControlChannel::Endpoint borrowed;
    if (ControlChannel::AdoptFromEnvironment(borrowed) != ControlChannel::Result::Ok) return;

    const int duplicated = dup(borrowed.fileDescriptor);
    if (duplicated < 0) return;

    ControlChannel::Adopt(duplicated, gProgressChannel);
}

static void CloseProgressChannel()
{
    ControlChannel::Close(gProgressChannel);
}

static constexpr int WATCHDOG_INTERVAL_MILLIS = 500;

static std::atomic_bool gWatchdogRunning{false};
static std::thread gWatchdog;

static void StartShellWatchdog()
{
    if (gProgressChannel.fileDescriptor == ControlChannel::INVALID_FILE_DESCRIPTOR) return;
    if (gWatchdogRunning.exchange(true)) return;

    gWatchdog = std::thread([]() {
        while (gWatchdogRunning.load(std::memory_order_relaxed))
        {
            if (ControlChannel::PeerHasGone(gProgressChannel))
            {
                fprintf(stderr, "[launcher] the shell went away while translating, so there is nobody left to run for\n");
                fflush(stderr);
                _exit(0);
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(WATCHDOG_INTERVAL_MILLIS));
        }
    });
}

static void StopShellWatchdog()
{
    if (!gWatchdogRunning.exchange(false)) return;
    if (gWatchdog.joinable()) gWatchdog.join();
}

static const char* LeafOf(const char* path)
{
    if (!path) return "";

    const char* separator = strrchr(path, '/');
    return separator ? separator + 1 : path;
}

static std::string gTranslatingLibrary;
static uint64_t gTranslatingBytes = 0;
static int gLastPercentShown = -1;

static std::mutex gPercentMutex;

static void ShowTranslationPercent(void*, const char*, uint64_t completed, uint64_t total)
{
    if (total == 0) return;

    std::lock_guard<std::mutex> guard(gPercentMutex);

    if (gLastPercentShown < 0)
    {
        if (gTranslatingBytes >= (1ull << 20))
            fprintf(stderr, "[acl] translating %s, %.1f MiB\n", gTranslatingLibrary.c_str(),
                    (double)gTranslatingBytes / (double)(1ull << 20));
        else
            fprintf(stderr, "[acl] translating %s, %llu KiB\n", gTranslatingLibrary.c_str(),
                    (unsigned long long)(gTranslatingBytes >> 10));

        gLastPercentShown = 0;
        fflush(stderr);
    }

    const int percent = (int)((completed * 100) / total);

    if (percent == gLastPercentShown) return;
    if (percent < gLastPercentShown + PERCENT_STEP && percent != 100) return;

    gLastPercentShown = percent;

    fprintf(stderr, "[acl]   %s %d%%\n", gTranslatingLibrary.c_str(), percent);
    fflush(stderr);
}

static void ReportTranslationProgress(const char* imagePath, const AheadOfTime::Statistics& statistics)
{
    if (gProgressChannel.fileDescriptor == ControlChannel::INVALID_FILE_DESCRIPTOR) return;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::Progress);

    ControlChannel::CopyText(message.payload.progress.libraryName, ControlChannel::MAX_NAME_BYTES, LeafOf(imagePath));
    message.payload.progress.functionsDiscovered = statistics.functionsDiscovered;
    message.payload.progress.functionsTranslated = statistics.functionsTranslated;
    message.payload.progress.blocksTranslated = statistics.blocksTranslated;
    message.payload.progress.cacheHit = statistics.cacheHit;
    message.payload.progress.memoryBytes = (uint64_t)LlvmBackend::ResidentMegabytes() * 1024ull * 1024ull;

    for (uint32_t phase = 0; phase < ControlChannel::PHASE_COUNT; ++phase) message.payload.progress.phases[phase] = 1.0f;

    if (ControlChannel::Send(gProgressChannel, message) != ControlChannel::Result::PeerClosed) return;

    gShellVanished = true;
    fprintf(stderr, "[launcher] the shell went away while translating, so there is nobody left to run for\n");
}

static bool TranslateGuestLibrary(void* context, GuestRuntime::Instance& runtime, uint32_t moduleId, const char* imagePath)
{
    TranslationSet* set = (TranslationSet*)context;

    if (!set->translated.insert(moduleId).second) return true;

    set->images.emplace_back();

    struct stat details = {};
    const uint64_t imageBytes = stat(imagePath, &details) == 0 ? (uint64_t)details.st_size : 0;

    gTranslatingLibrary = LeafOf(imagePath);
    gTranslatingBytes = imageBytes;
    gLastPercentShown = -1;
    AheadOfTime::SetProgressCallback(&ShowTranslationPercent, nullptr);

    const auto started = std::chrono::steady_clock::now();

    AheadOfTime::Statistics statistics;
    AheadOfTime::Result translated = AheadOfTime::Translate(runtime, moduleId, imagePath, set->images.back(), statistics);

    const double elapsed = std::chrono::duration<double>(std::chrono::steady_clock::now() - started).count();

    if (translated != AheadOfTime::Result::Ok)
    {
        fprintf(stderr, "[acl] %s could not be translated: %s\n", LeafOf(imagePath), AheadOfTime::ResultName(translated));
        set->images.pop_back();
        return false;
    }

    const ElfLoader::Library* placed = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(runtime), moduleId);

    fprintf(stderr,
            "[launcher] translated %s ahead of time at %016llx: %llu functions, %llu blocks, %llu direct calls, %llu traps, %llu ranges "
            "left uncovered, %llu undecodable, %llu unlowerable, cache %s\n",
            imagePath, (unsigned long long)(placed ? (uint64_t)placed->reservationAddress : 0),
            (unsigned long long)statistics.functionsTranslated, (unsigned long long)statistics.blocksTranslated,
            (unsigned long long)statistics.directCallsEmitted, (unsigned long long)statistics.trapsEmitted,
            (unsigned long long)statistics.rangesLeftUncovered, (unsigned long long)statistics.functionsUndecodable,
            (unsigned long long)statistics.functionsUnlowerable, statistics.cacheHit ? "hit" : "miss");

    if (statistics.cacheHit)
    {
        fprintf(stderr, "[acl] %s came back from the cache in %.1fs\n", LeafOf(imagePath), elapsed);
    }
    else
    {
        fprintf(stderr, "[acl] %s translated in %.1fs, %llu functions and %llu blocks\n", LeafOf(imagePath), elapsed,
                (unsigned long long)statistics.functionsTranslated, (unsigned long long)statistics.blocksTranslated);
    }

    fflush(stderr);

    ReportTranslationProgress(imagePath, statistics);

    return true;
}

std::string Launcher::GuestShimPath()
{
    const char* configured = Settings::Lookup(GUEST_SHIM_SETTING);
    if (configured && configured[0] != '\0') return configured;

    char self[4096];
    ssize_t length = readlink("/proc/self/exe", self, sizeof(self) - 1);
    if (length <= 0) return GUEST_SHIM_LEAF;

    self[length] = '\0';

    std::string path = self;
    size_t slash = path.find_last_of('/');
    if (slash == std::string::npos) return GUEST_SHIM_LEAF;

    return path.substr(0, slash) + "/" + GUEST_SHIM_LEAF;
}

static bool CopyFileContents(const std::string& source, const std::string& destination)
{
    FILE* input = fopen(source.c_str(), "rb");
    if (!input) return false;

    FILE* output = fopen(destination.c_str(), "wb");
    if (!output)
    {
        fclose(input);
        return false;
    }

    char buffer[65536];
    bool ok = true;

    while (true)
    {
        size_t read = fread(buffer, 1, sizeof(buffer), input);
        if (read == 0) break;

        if (fwrite(buffer, 1, read, output) != read)
        {
            ok = false;
            break;
        }
    }

    if (ferror(input)) ok = false;

    fclose(input);
    if (fclose(output) != 0) ok = false;

    if (ok) chmod(destination.c_str(), 0755);
    return ok;
}

static Launcher::Result PrepareGuestLibraries(const Apk::Package& package, const std::string& libraryDirectory, const std::string& abi,
                                              std::string& guestLibraryDirectory)
{
    guestLibraryDirectory = libraryDirectory + "/" + abi;
    if (!Launcher::EnsureDirectory(guestLibraryDirectory))
    {
        fprintf(stderr, "[launcher] cannot create guest library directory %s\n", guestLibraryDirectory.c_str());
        return Launcher::Result::NativeLibraryExtractionFailed;
    }

    std::vector<std::string> extracted;
    Apk::Result result = Apk::ExtractNativeLibraries(package, abi, guestLibraryDirectory, extracted);
    if (result != Apk::Result::Ok)
    {
        fprintf(stderr, "[launcher] cannot extract %s native libraries: %s\n", abi.c_str(), Apk::ResultName(result));
        return Launcher::Result::NativeLibraryExtractionFailed;
    }

    std::string shim = Launcher::GuestShimPath();
    if (!ArtRuntime::PathExists(shim))
    {
        fprintf(stderr, "[launcher] the guest shim %s is missing, so System.loadLibrary cannot reach the translator\n", shim.c_str());
        guestLibraryDirectory.clear();
        return Launcher::Result::NativeLibraryExtractionFailed;
    }

    for (const std::string& leaf : extracted)
    {
        std::string staged = libraryDirectory + "/" + leaf;
        if (CopyFileContents(shim, staged)) continue;

        fprintf(stderr, "[launcher] cannot stage the guest shim as %s\n", staged.c_str());
        guestLibraryDirectory.clear();
        return Launcher::Result::NativeLibraryExtractionFailed;
    }

    fprintf(stderr, "[launcher] extracted %d %s guest librar%s to %s, each fronted by the translator shim\n", (int)extracted.size(),
            abi.c_str(), extracted.size() == 1 ? "y" : "ies", guestLibraryDirectory.c_str());

    return Launcher::Result::Ok;
}

Launcher::Result Launcher::PrepareNativeLibraries(const Options& options, const Apk::Package& package, const std::string& dataDirectory,
                                                  std::string& libraryDirectory, std::string& guestLibraryDirectory)
{
    libraryDirectory = dataDirectory + "/" + NATIVE_LIBRARY_LEAF;
    guestLibraryDirectory.clear();
    if (!EnsureDirectory(libraryDirectory))
    {
        fprintf(stderr, "[launcher] cannot create native library directory %s\n", libraryDirectory.c_str());
        return Result::NativeLibraryExtractionFailed;
    }

    if (options.traceJni)
    {
        fprintf(stderr, "[jni] %d archive entries, %d dex, %d native abi\n", (int)package.entries.size(), (int)package.dexEntries.size(),
                (int)package.nativeAbis.size());

        if (!package.manifest.extractNativeLibs && !package.nativeAbis.empty())
        {
            fprintf(stderr, "[jni] the manifest asks for extractNativeLibs=false; extracting anyway, because host dlopen cannot map from a zip\n");
        }
    }

    if (!options.nativeAbiRequested)
    {
        std::string guestAbi;
        if (Apk::SelectAbi(package, Platform::GUEST_ABI, guestAbi) == Apk::Result::Ok && !guestAbi.empty())
        {
            fprintf(stderr, "[launcher] the package ships %s, which is what this runtime translates, so that is what it loads\n",
                    Platform::GUEST_ABI);

            return PrepareGuestLibraries(package, libraryDirectory, guestAbi, guestLibraryDirectory);
        }
    }

    std::string abi;
    Apk::Result selected = Apk::SelectAbi(package, options.nativeAbi, abi);

    if (selected != Apk::Result::Ok)
    {
        std::string available;
        for (const std::string& candidate : package.nativeAbis)
        {
            if (!available.empty()) available += ", ";
            available += candidate;
        }

        fprintf(stderr, "[launcher] %s has no %s native library (it ships: %s). System.loadLibrary will throw UnsatisfiedLinkError.\n",
                package.manifest.packageName.c_str(), options.nativeAbi.empty() ? Apk::HOST_NATIVE_ABI : options.nativeAbi.c_str(),
                available.c_str());
        return Result::Ok;
    }

    if (abi.empty())
    {
        if (options.traceJni) fprintf(stderr, "[jni] no native libraries in the archive\n");
        return Result::Ok;
    }

    std::vector<std::string> extracted;
    Apk::Result result = Apk::ExtractNativeLibraries(package, abi, libraryDirectory, extracted);
    if (result != Apk::Result::Ok)
    {
        fprintf(stderr, "[launcher] cannot extract %s native libraries: %s\n", abi.c_str(), Apk::ResultName(result));
        return Result::NativeLibraryExtractionFailed;
    }

    fprintf(stderr, "[launcher] extracted %d %s native librar%s to %s\n", (int)extracted.size(), abi.c_str(),
            extracted.size() == 1 ? "y" : "ies", libraryDirectory.c_str());

    if (options.traceJni)
    {
        for (const std::string& leaf : extracted) fprintf(stderr, "[jni] extracted %s\n", leaf.c_str());
    }

    return Result::Ok;
}

Launcher::Result Launcher::Run(const Options& options, int& exitStatus)
{
    exitStatus = 1;
    gVerboseConsole = options.verbose;

    if (options.windowWidth > 0 && options.windowHeight > 0)
    {
        Settings::Set(Launcher::WINDOW_WIDTH_SETTING, std::to_string(options.windowWidth));
        Settings::Set(Launcher::WINDOW_HEIGHT_SETTING, std::to_string(options.windowHeight));

        const int density = Platform::ConfiguredDensity((uint32_t)options.windowWidth, (uint32_t)options.windowHeight);
        Settings::Set(Platform::DENSITY_SETTING, std::to_string(density));

        if (gVerboseConsole)
        {
            fprintf(stderr, "[launcher] the guest gets a %s %dx%d window at %d dpi, so it measures %d by %d dp\n",
                    options.landscape ? "landscape" : "portrait", options.windowWidth, options.windowHeight, density,
                    options.windowWidth * Platform::DENSITY_DEFAULT / density,
                    options.windowHeight * Platform::DENSITY_DEFAULT / density);
        }
    }

    if (!ArtRuntime::PathExists(options.hostRoot)) return Result::HostRootMissing;
    if (!ArtRuntime::PathExists(options.frameworkJar)) return Result::FrameworkJarMissing;
    if (!ArtRuntime::PathExists(options.apkPath)) return Result::ApkNotFound;

    IsolateThisProcess(options);

    if (!options.translateOnly)
    {
        Diagnostics::Initialize(Diagnostics::DEFAULT_CAPACITY);

        if (!options.verbose) Diagnostics::SetConsoleFilter(&KeepForConsole);

        if (!Diagnostics::CaptureStandardError()) fprintf(stderr, "[launcher] the log buffer could not capture standard error\n");
    }

    LogGuard logGuard;

    if (!options.translateOnly)
    {
        fprintf(stderr, "[acl] %s, %s %dx%d at %d dpi\n", options.apkPath.c_str(), options.landscape ? "landscape" : "portrait",
                options.windowWidth, options.windowHeight,
                Platform::ConfiguredDensity((uint32_t)options.windowWidth, (uint32_t)options.windowHeight));
    }

    bool mayExecutePrograms = true;

    if (!options.translateOnly && PretranslationRequested(options))
    {
        fprintf(stderr, "[launcher] translating in a separate process, which gets no display, no device and no network\n");

        if (RunTranslatorProcess(options))
        {
            mayExecutePrograms = false;
        }
        else
        {
            fprintf(stderr, "[launcher] translation will happen here instead, so this process keeps the ability to link\n");
        }
    }

    if (!options.splitPaths.empty())
    {
        fprintf(stderr, "[launcher] the base apk is installed beside %zu split(s), which load into the same resource table\n",
                options.splitPaths.size());
    }

    ConfineBeforeParsing(options, mayExecutePrograms);

    ApkSignature::Info signingInfo;
    ApkSignature::Result signingResult = ApkSignature::Verify(options.apkPath, signingInfo);

    if (signingResult == ApkSignature::Result::Ok)
    {
        fprintf(stderr, "[launcher] the package signature verifies with %s: %s\n", ApkSignature::SchemeName(signingInfo.scheme),
                ApkSignature::DescribeInfo(signingInfo).c_str());

        if (!signingInfo.signers.empty() && !signingInfo.signers.front().subject.empty())
            fprintf(stderr, "[launcher] the signer is %s\n", signingInfo.signers.front().subject.c_str());
    }
    else
    {
        fprintf(stderr, "[launcher] the package signature did not verify: %s\n", ApkSignature::ResultName(signingResult));
    }

    FrameworkJni::SetSigningInfo(signingInfo);

    Apk::Package package;
    Apk::Result apkResult = Apk::OpenWithSplits(options.apkPath, options.splitPaths, options.frameworkResources, package);
    if (apkResult != Apk::Result::Ok)
    {
        fprintf(stderr, "[launcher] cannot open %s: %s\n", options.apkPath.c_str(), Apk::ResultName(apkResult));
        return Result::ApkOpenFailed;
    }

    if (!package.hasFramework)
    {
        if (options.frameworkResourcesRequested)
        {
            fprintf(stderr, "[launcher] --framework-res names '%s', which is not a loadable framework resource archive\n",
                    options.frameworkResources.c_str());
            Apk::Close(package);
            return Result::FrameworkResourcesUnusable;
        }

        fprintf(stderr,
                "[launcher] no framework resources, so the guest will read no layout attributes at all; a release bundle carries "
                "them in android/android.jar beside the binary, or pass --framework-res\n");
    }
    else if (options.traceJni || FrameworkJni::IsTracing("resources"))
    {
        fprintf(stderr, "[resources] framework package: %s\n", package.frameworkPath.c_str());
    }

    std::string dataDirectory = DataDirectoryFor(options, package.manifest.packageName);
    if (dataDirectory.empty())
    {
        fprintf(stderr, "[launcher] refusing a data directory from package name '%s'\n", package.manifest.packageName.c_str());
        Apk::Close(package);
        return Result::BootstrapFailed;
    }

    if (!EnsureDirectory(dataDirectory))
    {
        fprintf(stderr, "[launcher] cannot create data directory %s\n", dataDirectory.c_str());
        Apk::Close(package);
        return Result::BootstrapFailed;
    }

    std::string libraryDirectory;
    std::string guestLibraryDirectory;
    Result libraryResult = PrepareNativeLibraries(options, package, dataDirectory, libraryDirectory, guestLibraryDirectory);
    if (libraryResult != Result::Ok)
    {
        Apk::Close(package);
        return libraryResult;
    }

    IcuSetup::Result icuResult = IcuSetup::Configure(options.hostRoot);
    if (icuResult != IcuSetup::Result::Ok)
    {
        fprintf(stderr, "[launcher] ICU setup failed: %s\n", IcuSetup::ResultName(icuResult));
        Apk::Close(package);
        return Result::IcuSetupFailed;
    }

    ArtRuntime::Configuration configuration = ArtRuntime::DefaultConfiguration(options.hostRoot);
    configuration.classPath.push_back(options.frameworkJar);

    std::string apacheHttp = SharedLibraryJar(options.frameworkJar, Launcher::APACHE_HTTP_LIBRARY);
    if (!apacheHttp.empty() && UsesSharedLibrary(package.manifest, Launcher::APACHE_HTTP_LIBRARY))
    {
        fprintf(stderr, "[launcher] the app uses %s, so it joins the class path\n", Launcher::APACHE_HTTP_LIBRARY);
        configuration.classPath.push_back(apacheHttp);
    }

    configuration.classPath.push_back(options.apkPath);

    for (const std::string& split : package.splitPaths) configuration.classPath.push_back(split);
    configuration.nativeLibraryPath.insert(configuration.nativeLibraryPath.begin(), libraryDirectory);
    configuration.checkJni = options.traceJni || Settings::Tracing("checkjni");
    configuration.reportOptions = options.traceJni;

    ArtRuntime::Vm vm;
    NameProcessAfterPackage(package.manifest.packageName);

    fprintf(stderr, "[launcher] %s declares %zu activit%s, %zu service(s), %zu receiver(s), %zu provider(s)\n",
            package.manifest.packageName.c_str(), package.manifest.activities.size(),
            package.manifest.activities.size() == 1 ? "y" : "ies", package.manifest.services.size(),
            package.manifest.receivers.size(), package.manifest.providers.size());

    fprintf(stderr, "[launcher] the guest runs as uid %u, pid %d, named %s\n", (unsigned)getuid(), (int)getpid(),
            package.manifest.packageName.c_str());

    std::string androidDataDirectory = dataDirectory;
    std::string androidExternalDirectory = dataDirectory + "/external";

    BuildAndroidDirectories(dataDirectory, androidExternalDirectory, package.manifest.packageName);

    std::string androidArchivePath = options.apkPath;
    std::string androidLibraryDirectory = libraryDirectory;

    Sandbox::AndroidTree tree;
    tree.dataDirectory = dataDirectory;
    tree.externalDirectory = androidExternalDirectory;
    tree.packageName = package.manifest.packageName;
    tree.archivePath = options.apkPath;
    tree.libraryDirectory = libraryDirectory;

    Sandbox::AndroidPaths installed;

    if (Sandbox::AttachAndroidTree(tree, installed) == Sandbox::Result::Ok)
    {
        androidDataDirectory = "/data/data/" + package.manifest.packageName;
        androidExternalDirectory = GuestLibc::ANDROID_EXTERNAL_DIRECTORY;

        if (!installed.archivePath.empty()) androidArchivePath = installed.archivePath;
        if (!installed.libraryDirectory.empty()) androidLibraryDirectory = installed.libraryDirectory;

        fprintf(stderr, "[launcher] the guest sees its storage at %s and %s, as it would on a device\n",
                androidDataDirectory.c_str(), androidExternalDirectory.c_str());
        fprintf(stderr, "[launcher] and finds itself installed at %s\n", androidArchivePath.c_str());

        bool described = Sandbox::AttachSystemFile("/proc/cpuinfo", HostBridge::ProcessorDescription()) == Sandbox::Result::Ok &&
                         Sandbox::AttachSystemFile("/proc/meminfo", HostBridge::MemoryDescription()) == Sandbox::Result::Ok;

        if (described) fprintf(stderr, "[launcher] the whole process reads an arm64 /proc/cpuinfo and /proc/meminfo\n");
    }

    ConfineThisProcess(options, dataDirectory, libraryDirectory, mayExecutePrograms);

    unsetenv("LD_LIBRARY_PATH");
    unsetenv("LD_PRELOAD");



    ArtRuntime::Result vmResult = ArtRuntime::Start(configuration, vm);
    if (vmResult != ArtRuntime::Result::Ok)
    {
        fprintf(stderr, "[launcher] VM start failed: %s\n", ArtRuntime::ResultName(vmResult));
        Apk::Close(package);
        return Result::VmStartFailed;
    }

    FrameworkJni::SetPackage(&package);

    FrameworkJni::SetExternalStorageDirectory(androidExternalDirectory);
    FrameworkJni::SetArchivePath(androidArchivePath);

    GuestRuntime::Instance guest;
    JniBridge::Instance bridge;
    TranslationSet translations;
    bool guestStarted = false;

    if (!guestLibraryDirectory.empty())
    {
        GuestRuntime::Configuration guestConfiguration;
        guestConfiguration.searchPaths.push_back(guestLibraryDirectory);
        guestConfiguration.stubUnresolvedSymbols = true;
        guestConfiguration.traceGuest = options.traceJni;

        if (options.aheadOfTime)
        {
            const char* required = Settings::Lookup(REQUIRE_TRANSLATED_SETTING);
            guestConfiguration.requireTranslated = required == nullptr || strcmp(required, "0") != 0;
        }
        else
        {
            guestConfiguration.requireTranslated = false;
            guestConfiguration.recompile = true;
            UnsignedSetting(JIT_THRESHOLD_SETTING, DEFAULT_JIT_THRESHOLD, guestConfiguration.recompileThreshold);
            UnsignedSetting(JIT_REGION_BYTES_SETTING, GuestRuntime::DEFAULT_RECOMPILE_REGION_BYTES,
                            guestConfiguration.recompileRegionBytes);
            JitTier(guestConfiguration.tier);
            guestConfiguration.traceBlocks = Settings::Tracing("blocks");

            const char* background = Settings::Lookup(JIT_BACKGROUND_SETTING);
            guestConfiguration.recompileInBackground = background == nullptr || strcmp(background, "0") != 0;

            fprintf(stderr,
                    "[launcher] guest code is interpreted until a block has run %llu time(s), then compiled %s in regions of up to %llu "
                    "bytes at the %s tier; nothing is translated ahead of time\n",
                    (unsigned long long)guestConfiguration.recompileThreshold,
                    guestConfiguration.recompileInBackground ? "on a background thread" : "on the thread that reached it",
                    (unsigned long long)guestConfiguration.recompileRegionBytes, LlvmBackend::TierName(guestConfiguration.tier));
        }

        guestConfiguration.stepLimit = 0;

        GuestRuntime::Result started = GuestRuntime::Create(guestConfiguration, guest);
        if (started != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[launcher] cannot start the guest runtime: %s\n", GuestRuntime::ResultName(started));
            ArtRuntime::Stop(vm);
            Apk::Close(package);
            return Result::VmStartFailed;
        }

        GuestLibc::DescribeAndroidEnvironment(androidDataDirectory, androidExternalDirectory, package.manifest.packageName);

        if (GuestRuntime::RegisterPlatformProxies(guest) != GuestRuntime::Result::Ok ||
            GuestLibc::RegisterAll(guest) != GuestLibc::Result::Ok ||
            JniBridge::Create(guest, vm.javaVm, bridge) != JniBridge::Result::Ok ||
            JniBridge::InstallLibraryLoader(bridge, guestLibraryDirectory) != JniBridge::Result::Ok ||
            JniBridge::RegisterLoaderFunctions(bridge) != JniBridge::Result::Ok)
        {
            fprintf(stderr, "[launcher] cannot build the guest bridge\n");
            GuestRuntime::Destroy(guest);
            ArtRuntime::Stop(vm);
            Apk::Close(package);
            return Result::VmStartFailed;
        }

        if (options.aheadOfTime) JniBridge::InstallLibraryTranslator(bridge, &TranslateGuestLibrary, &translations);

        JniBridge::InstallRuntimeLoadHook(vm.jniEnv);

#if defined(RUNTIME_HAVE_VULKAN)
        VulkanWsi::Result vulkan = VulkanWsi::RegisterAll(guest);
        if (vulkan != VulkanWsi::Result::Ok)
        {
            fprintf(stderr, "[launcher] the guest will not be able to render with Vulkan: %s\n", VulkanWsi::ResultName(vulkan));
        }
#endif

#if defined(RUNTIME_HAVE_GLES)
        GlesWsi::Result gles = GlesWsi::RegisterAll(guest);
        if (gles != GlesWsi::Result::Ok)
        {
            fprintf(stderr, "[launcher] the guest will not be able to render with OpenGL ES: %s\n", GlesWsi::ResultName(gles));
        }
#endif

        NativeActivity::RegisterWindowFunctions(guest);
        NativeActivity::RegisterAudioFunctions(guest);
        NativeActivity::RegisterAssetFunctions(guest, package);
        OpenProgressChannel();
        StartShellWatchdog();

        if (PretranslationRequested(options)) PretranslateGuestLibraries(guest, guestLibraryDirectory, translations);

        StopShellWatchdog();
        CloseProgressChannel();

        if (gShellVanished)
        {
            GuestRuntime::Destroy(guest);
            ArtRuntime::Stop(vm);
            Apk::Close(package);
            exitStatus = 0;
            return Result::Ok;
        }

        guestStarted = true;
        fprintf(stderr, "[launcher] the guest bridge is live; System.loadLibrary will route %s libraries through the translator\n",
                Platform::GUEST_ABI);
    }

    if (options.translateOnly)
    {
        if (!gPretranslationDeclined) fprintf(stderr, "[launcher] the translation cache is warm; the translator process is done\n");

        fflush(stderr);
        _exit(gPretranslationDeclined ? PRETRANSLATION_DECLINED_STATUS : 0);
    }

    if (gPretranslationDeclined)
    {
        fprintf(stderr, "[launcher] the libraries are translated as the app loads them, so this process keeps the ability to link\n");
    }

    ConfineAfterTranslation(mayExecutePrograms && PretranslationRequested(options) && !gPretranslationDeclined);

    fprintf(stderr, "[acl] %s %s, signed by %s\n", package.manifest.packageName.c_str(),
            package.manifest.versionName.empty() ? "" : package.manifest.versionName.c_str(),
            signingInfo.signers.empty() || signingInfo.signers.front().subject.empty()
                ? (signingInfo.verified ? "an unnamed signer" : "nobody, it is unsigned")
                : signingInfo.signers.front().subject.c_str());
    fprintf(stderr, "[acl] running, close the window to stop\n");

    Result result = Result::Ok;
    ArtRuntime::Result registerResult = FrameworkJni::RegisterAll(vm);
    if (registerResult != ArtRuntime::Result::Ok)
    {
        fprintf(stderr, "[launcher] native registration failed: %s\n", ArtRuntime::ResultName(registerResult));
        ArtRuntime::DescribeAndClearException(vm);
        result = Result::NativeRegistrationFailed;
    }

    if (result == Result::Ok)
    {
        NativeActivity::Result installed = NativeActivity::InstallJni(vm.jniEnv, guest, bridge, guestStarted);
        if (installed != NativeActivity::Result::Ok)
        {
            fprintf(stderr, "[launcher] native activities are unavailable: %s\n", NativeActivity::ResultName(installed));
            result = Result::NativeRegistrationFailed;
        }
    }

    if (result == Result::Ok)
    {
        jclass bootstrap = nullptr;
        if (ArtRuntime::FindClass(vm, FrameworkJni::BOOTSTRAP_CLASS, bootstrap) != ArtRuntime::Result::Ok)
        {
            ArtRuntime::DescribeAndClearException(vm);
            result = Result::BootstrapClassMissing;
        }
        else
        {
            JNIEnv* env = vm.jniEnv;
            jmethodID start = env->GetStaticMethodID(bootstrap, FrameworkJni::BOOTSTRAP_METHOD, FrameworkJni::BOOTSTRAP_SIGNATURE);
            if (!start)
            {
                ArtRuntime::DescribeAndClearException(vm);
                result = Result::BootstrapMethodMissing;
            }
            else
            {
                jstring dataDir = env->NewStringUTF(androidDataDirectory.c_str());
                jstring libraryDir = env->NewStringUTF(androidLibraryDirectory.c_str());
                jint status = env->CallStaticIntMethod(bootstrap, start, dataDir, libraryDir);

                if (ArtRuntime::DescribeAndClearException(vm))
                {
                    result = Result::BootstrapFailed;
                }
                else if (status != 0)
                {
                    fprintf(stderr, "[launcher] bootstrap returned %d\n", static_cast<int>(status));
                    result = Result::BootstrapFailed;
                }
                else
                {
                    exitStatus = 0;
                }
            }
        }
    }

    if (guestStarted)
    {
        GuestRuntime::Statistics counters = GuestRuntime::GetStatistics(guest);
        fprintf(stderr, "[launcher] guest blocks: %llu total, %llu translated, %llu interpreted, %llu traps hit\n",
                (unsigned long long)counters.blocksExecuted, (unsigned long long)counters.blocksExecutedTranslated,
                (unsigned long long)counters.blocksInterpreted, (unsigned long long)counters.trapsHit);

        if (!options.aheadOfTime)
        {
            fprintf(stderr,
                    "[launcher] jit: %llu region(s) covering %llu block(s) compiled at run time, %llu entries ran compiled, %llu "
                    "region(s) left to the interpreter, %llu compile(s) discarded by %llu code invalidation(s)\n",
                    (unsigned long long)counters.regionsRecompiled, (unsigned long long)counters.blocksRecompiled,
                    (unsigned long long)counters.blocksExecutedNative, (unsigned long long)counters.recompileFailures,
                    (unsigned long long)counters.recompilesDiscarded, (unsigned long long)counters.codeInvalidations);
        }

#if defined(RUNTIME_HAVE_VULKAN)
        VulkanWsi::Statistics vulkanCounters = VulkanWsi::GetStatistics();
        if (vulkanCounters.entryPointsRegistered > 0)
        {
            fprintf(stderr, "[launcher] the host driver presented %llu guest frame(s) and refused %llu guest allocator(s)\n",
                    (unsigned long long)vulkanCounters.framesPresented,
                    (unsigned long long)vulkanCounters.guestAllocatorsRefused);
        }
        const char* pacingReport = Settings::Lookup(Launcher::PACING_REPORT_SETTING);
        if (pacingReport && pacingReport[0] != '\0') VulkanWsi::WritePacingReport(pacingReport);
#endif

#if defined(RUNTIME_HAVE_GLES)
        GlesWsi::Statistics glesCounters = GlesWsi::GetStatistics();
        if (glesCounters.entryPointsRegistered > 0)
        {
            fprintf(stderr, "[launcher] the host driver swapped %llu guest frame(s) and made %llu window surface(s) for OpenGL ES\n",
                    (unsigned long long)glesCounters.framesPresented, (unsigned long long)glesCounters.surfacesCreated);
        }
#endif
    }

    fprintf(stderr, "[launcher] the app's threads may still be running, so the process ends here instead of unloading what they use\n");
    return result;
}

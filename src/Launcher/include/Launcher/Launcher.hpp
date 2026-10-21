#pragma once

#include <cstdint>
#include <string>

#include "Apk/Apk.hpp"
#include "Platform/Platform.hpp"

namespace Launcher
{
    inline constexpr const char* DATA_ROOT_LEAF = ".local/share/acl";
    inline constexpr const char* SDK_ROOT_LEAF = "Android/Sdk";
    inline constexpr const char* NATIVE_LIBRARY_LEAF = "lib";
    inline constexpr const char* GUEST_SHIM_LEAF = "libaclguest.so";
    inline constexpr uint64_t DEFAULT_PROCESS_LIMIT = 4096;

    inline constexpr const char* REQUIRE_TRANSLATED_SETTING = "require-translated";
    inline constexpr const char* JIT_THRESHOLD_SETTING = "jit-threshold";
    inline constexpr const char* JIT_REGION_BYTES_SETTING = "jit-region-bytes";
    inline constexpr const char* JIT_TIER_SETTING = "jit-tier";
    inline constexpr const char* JIT_BACKGROUND_SETTING = "jit-background";
    inline constexpr uint64_t DEFAULT_JIT_THRESHOLD = 64;
    inline constexpr const char* NO_SANDBOX_SETTING = "no-sandbox";
    inline constexpr const char* NETWORK_SETTING = "network";
    inline constexpr const char* ISOLATED_SETTING = "isolated";
    inline constexpr const char* PRESENT_WITHOUT_WAITING_SETTING = "no-vsync";
    inline constexpr const char* FRAME_RATE_SETTING = "fps";
    inline constexpr const char* DECORATED_SETTING = "decorated";
    inline constexpr const char* FULLSCREEN_SETTING = "fullscreen";
    inline constexpr const char* FONT_DIR_SETTING = "font-dir";
    inline constexpr const char* GUEST_SHIM_SETTING = "guest-shim";
    inline constexpr const char* PRETRANSLATE_SETTING = "pretranslate";
    inline constexpr const char* FRAME_DUMP_SETTING = "frame-dump";
    inline constexpr const char* MAX_FRAMES_SETTING = "max-frames";
    inline constexpr const char* PACING_REPORT_SETTING = "pacing-report";
    inline constexpr const char* ACTIVITY_SETTING = "activity";
    inline constexpr const char* LAUNCH_ACTIVITY_SETTING = "launch-activity";
    inline constexpr const char* RECONFIGURE_SETTING = "reconfigure";
    inline constexpr const char* CONTINUOUS_FRAMES_SETTING = "continuous-frames";
    inline constexpr const char* APP_USER_ID_SETTING = "app-user-id";
    inline constexpr const char* WINDOW_WIDTH_SETTING = Platform::WINDOW_WIDTH_SETTING;
    inline constexpr const char* WINDOW_HEIGHT_SETTING = Platform::WINDOW_HEIGHT_SETTING;
    inline constexpr int PORTRAIT_WINDOW_WIDTH = 1080;
    inline constexpr int PORTRAIT_WINDOW_HEIGHT = 2400;

    enum class Result : uint8_t
    {
        Ok,
        UsageError,
        HostRootMissing,
        FrameworkJarMissing,
        ApkNotFound,
        ApkOpenFailed,
        FrameworkResourcesUnusable,
        IcuSetupFailed,
        VmStartFailed,
        NativeRegistrationFailed,
        BootstrapClassMissing,
        BootstrapMethodMissing,
        BootstrapFailed,
        NativeLibraryExtractionFailed
    };

    struct Options
    {
        bool verbose = false;
        std::string apkPath;
        std::vector<std::string> splitPaths;
        std::string hostRoot;
        std::string frameworkJar;
        std::string dataRoot;
        std::string frameworkResources;
        std::string nativeAbi;
        bool frameworkResourcesRequested = false;
        bool nativeAbiRequested = false;
        bool traceJni = false;
        bool landscape = false;
        bool presentWithoutWaiting = false;
        bool reportFrameRate = false;
        bool decorated = false;
        bool fullscreen = false;
        bool network = false;
        bool translateOnly = false;
        bool aheadOfTime = true;
        int windowWidth = 0;
        int windowHeight = 0;

        enum class Action : uint8_t
        {
            Run,
            Install,
            Uninstall,
            List
        };

        Action action = Action::Run;
        std::string packageName;
        bool keepData = false;
    };

    inline constexpr uint32_t APPLICATION_USER_ID = 10237;

    Result ManagePackages(const Options& options, int& exitStatus);

    void RememberArguments(int argc, char** argv);
    void NameProcessAfterPackage(const std::string& packageName);

    Result ParseArguments(int argc, char** argv, Options& options);
    Result Run(const Options& options, int& exitStatus);

    std::string DefaultDataRoot();
    std::string DefaultFontDirectory();
    std::string DefaultFrameworkResources();
    std::string DefaultHostRoot();
    std::string DefaultFrameworkJar();
    std::string DataDirectoryFor(const Options& options, const std::string& packageName);
    bool EnsureDirectory(const std::string& path);
    Result PrepareNativeLibraries(const Options& options, const Apk::Package& package, const std::string& dataDirectory,
                                  std::string& libraryDirectory, std::string& guestLibraryDirectory);

    std::string GuestShimPath();

    void PrintUsage(const char* program);
    const char* ResultName(Result result);
    const char* ResultAdvice(Result result);
}

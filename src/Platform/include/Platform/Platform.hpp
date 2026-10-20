#pragma once

#include <cstdint>

#ifndef RUNTIME_BUILD_SDK_VERSION
#error "RUNTIME_BUILD_SDK_VERSION must be defined by the build (see RUNTIME_DECLARED_SDK_VERSION in CMakeLists.txt)"
#endif

namespace Platform
{
    inline constexpr int DECLARED_SDK_VERSION = RUNTIME_BUILD_SDK_VERSION;
    inline constexpr int DECLARED_PREVIEW_SDK_VERSION = 0;
    inline constexpr const char* DECLARED_RELEASE = "14";
    inline constexpr const char* DECLARED_CODENAME = "REL";

    inline constexpr const char* GUEST_ABI = "arm64-v8a";
    inline constexpr int GUEST_POINTER_BYTES = 8;
    inline constexpr uint64_t GUEST_MIN_PAGE_BYTES = 4096;
    inline constexpr uint64_t GUEST_MAX_PAGE_BYTES = 16384;

    inline constexpr int DENSITY_DEFAULT = 160;
    inline constexpr int TARGET_SHORT_SIDE_DP = 393;
    inline constexpr const char* DENSITY_SETTING = "density";
    inline constexpr const char* WINDOW_WIDTH_SETTING = "window-width";
    inline constexpr const char* WINDOW_HEIGHT_SETTING = "window-height";

    bool SdkVersionMatchesBuildConfiguration(int buildConfiguredVersion);
    uint64_t HostPageBytes();

    int DensityForWindow(uint32_t widthPixels, uint32_t heightPixels);
    int NearestDensityBucket(int densityDpi);
    int ConfiguredDensity(uint32_t widthPixels, uint32_t heightPixels);

    inline constexpr int STATUS_BAR_HEIGHT_DP = 28;
    inline constexpr int NAVIGATION_BAR_HEIGHT_DP = 0;

    int StatusBarHeight(uint32_t widthPixels, uint32_t heightPixels);
    int NavigationBarHeight(uint32_t widthPixels, uint32_t heightPixels);
    void ConfiguredScreen(int& widthPixels, int& heightPixels, int& densityDpi);
}

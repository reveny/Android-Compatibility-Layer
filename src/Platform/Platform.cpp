#include "Platform/Platform.hpp"

#include <cstdlib>
#include <unistd.h>

#include "Settings/Settings.hpp"

static const int DENSITY_BUCKETS[] = {120, 160, 213, 240, 320, 400, 420, 440, 480, 560, 640};

bool Platform::SdkVersionMatchesBuildConfiguration(int buildConfiguredVersion)
{
    return buildConfiguredVersion == DECLARED_SDK_VERSION;
}

uint64_t Platform::HostPageBytes()
{
    long size = sysconf(_SC_PAGESIZE);
    if (size <= 0) return GUEST_MIN_PAGE_BYTES;
    return (uint64_t)size;
}

int Platform::NearestDensityBucket(int densityDpi)
{
    if (densityDpi <= DENSITY_BUCKETS[0]) return DENSITY_BUCKETS[0];

    const int count = (int)(sizeof(DENSITY_BUCKETS) / sizeof(DENSITY_BUCKETS[0]));
    if (densityDpi >= DENSITY_BUCKETS[count - 1]) return DENSITY_BUCKETS[count - 1];

    int best = DENSITY_BUCKETS[0];
    int bestDistance = densityDpi - DENSITY_BUCKETS[0];

    for (int index = 1; index < count; ++index)
    {
        const int distance = densityDpi > DENSITY_BUCKETS[index] ? densityDpi - DENSITY_BUCKETS[index]
                                                                 : DENSITY_BUCKETS[index] - densityDpi;

        if (distance >= bestDistance) continue;

        best = DENSITY_BUCKETS[index];
        bestDistance = distance;
    }

    return best;
}

int Platform::DensityForWindow(uint32_t widthPixels, uint32_t heightPixels)
{
    const uint32_t shortSide = widthPixels < heightPixels ? widthPixels : heightPixels;
    if (shortSide == 0) return DENSITY_DEFAULT;

    const int derived = (int)((shortSide * (uint32_t)DENSITY_DEFAULT + TARGET_SHORT_SIDE_DP / 2) / TARGET_SHORT_SIDE_DP);

    if (derived <= DENSITY_DEFAULT) return DENSITY_DEFAULT;

    return NearestDensityBucket(derived);
}

static int ConfiguredPixels(const char* name)
{
    const char* value = Settings::Lookup(name);
    if (!value || value[0] == '\0') return 0;

    char* end = nullptr;
    const long parsed = strtol(value, &end, 10);

    if (!end || *end != '\0' || parsed <= 0 || parsed > 16384) return 0;

    return (int)parsed;
}

void Platform::ConfiguredScreen(int& widthPixels, int& heightPixels, int& densityDpi)
{
    widthPixels = ConfiguredPixels(WINDOW_WIDTH_SETTING);
    heightPixels = ConfiguredPixels(WINDOW_HEIGHT_SETTING);
    densityDpi = ConfiguredDensity((uint32_t)widthPixels, (uint32_t)heightPixels);
}

int Platform::StatusBarHeight(uint32_t widthPixels, uint32_t heightPixels)
{
    const int density = ConfiguredDensity(widthPixels, heightPixels);
    return (STATUS_BAR_HEIGHT_DP * density + 80) / 160;
}

int Platform::NavigationBarHeight(uint32_t widthPixels, uint32_t heightPixels)
{
    const int density = ConfiguredDensity(widthPixels, heightPixels);
    return (NAVIGATION_BAR_HEIGHT_DP * density + 80) / 160;
}

int Platform::ConfiguredDensity(uint32_t widthPixels, uint32_t heightPixels)
{
    const char* configured = Settings::Lookup(DENSITY_SETTING);

    if (configured && configured[0] != '\0')
    {
        char* end = nullptr;
        const long parsed = strtol(configured, &end, 10);

        if (end && *end == '\0' && parsed >= 40 && parsed <= 1280) return (int)parsed;
    }

    return DensityForWindow(widthPixels, heightPixels);
}

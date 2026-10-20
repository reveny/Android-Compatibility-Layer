#include <cstdio>
#include <cstdlib>

#include "Platform/Platform.hpp"
#include "Settings/Settings.hpp"

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static void CheckDensity(uint32_t width, uint32_t height, int expected, const char* label)
{
    const int measured = Platform::DensityForWindow(width, height);

    if (measured == expected)
    {
        printf("PASS %s (%ux%u -> %d dpi)\n", label, width, height, measured);
        return;
    }

    printf("FAIL %s (%ux%u -> %d dpi, expected %d)\n", label, width, height, measured, expected);
    failures += 1;
}

static void CheckLogicalWidth(uint32_t width, uint32_t height, int lowest, int highest, const char* label)
{
    const int density = Platform::DensityForWindow(width, height);
    const uint32_t shortSide = width < height ? width : height;
    const int logical = (int)shortSide * Platform::DENSITY_DEFAULT / density;

    if (logical >= lowest && logical <= highest)
    {
        printf("PASS %s (%ux%u -> %d dp across)\n", label, width, height, logical);
        return;
    }

    printf("FAIL %s (%ux%u -> %d dp across, wanted %d to %d)\n", label, width, height, logical, lowest, highest);
    failures += 1;
}

int main()
{
    CheckDensity(1080, 2400, 440, "a 1080p phone is xxhdpi");
    CheckDensity(1080, 1920, 440, "an older 1080p phone is the same");
    CheckDensity(1440, 3120, 560, "a 1440p phone is xxxhdpi-ish");
    CheckDensity(720, 1280, 320, "a 720p phone is xhdpi");
    CheckDensity(2400, 1080, 440, "landscape measures the short side, not the long one");

    CheckDensity(400, 300, Platform::DENSITY_DEFAULT, "a small test window stays at the default");
    CheckDensity(420, 880, Platform::DENSITY_DEFAULT, "and so does the app-debug fixture");
    CheckDensity(320, 240, Platform::DENSITY_DEFAULT, "no window is ever reported as ldpi");
    CheckDensity(0, 0, Platform::DENSITY_DEFAULT, "a window with no size falls back rather than dividing by zero");

    CheckLogicalWidth(1080, 2400, 360, 420, "a 1080p phone measures like a phone");
    CheckLogicalWidth(1440, 3120, 360, 420, "and so does a 1440p one");
    CheckLogicalWidth(720, 1280, 340, 420, "and so does a 720p one");

    Check(Platform::NearestDensityBucket(1) == 120, "densities below the lowest bucket clamp up");
    Check(Platform::NearestDensityBucket(100000) == 640, "densities above the highest bucket clamp down");
    Check(Platform::NearestDensityBucket(435) == 440, "a density between buckets takes the nearer one");
    Check(Platform::NearestDensityBucket(425) == 420, "and the nearer one below, when that is nearer");
    Check(Platform::NearestDensityBucket(430) == 420, "an exact tie settles on the lower bucket rather than by accident");
    Check(Platform::NearestDensityBucket(240) == 240, "a density on a bucket stays put");

    Settings::Set(Platform::DENSITY_SETTING, "213");
    Check(Platform::ConfiguredDensity(1080, 2400) == 213, "an explicit density wins over the derived one");

    Settings::Set(Platform::DENSITY_SETTING, "not-a-number");
    Check(Platform::ConfiguredDensity(1080, 2400) == 440, "a malformed density is refused rather than believed");

    Settings::Set(Platform::DENSITY_SETTING, "3");
    Check(Platform::ConfiguredDensity(1080, 2400) == 440, "an implausible density is refused too");

    Settings::Unset(Platform::DENSITY_SETTING);
    Check(Platform::ConfiguredDensity(1080, 2400) == 440, "with nothing set the derived density is used");

    printf("%s: %d failure(s)\n", failures == 0 ? "platform" : "platform FAILED", failures);

    return failures == 0 ? 0 : 1;
}

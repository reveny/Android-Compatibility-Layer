#include "Overlay/FrameRate.hpp"

#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <mutex>

#include "Settings/Settings.hpp"

namespace FrameRate
{
    static std::mutex gMutex;
    static uint64_t gWindowStart = 0;
    static uint64_t gWindowFrames = 0;
    static std::atomic<uint64_t> gTotalFrames{0};
    static std::atomic<double> gPerSecond{0.0};
    static std::atomic<double> gMilliseconds{0.0};
}

static uint64_t MonotonicNanos()
{
    struct timespec now = {};
    clock_gettime(CLOCK_MONOTONIC, &now);

    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

bool FrameRate::OverlayRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(FrameRate::OVERLAY_SETTING);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

void FrameRate::Tick()
{
    const uint64_t now = MonotonicNanos();

    FrameRate::gTotalFrames.fetch_add(1, std::memory_order_relaxed);

    std::lock_guard<std::mutex> guard(FrameRate::gMutex);

    if (FrameRate::gWindowStart == 0)
    {
        FrameRate::gWindowStart = now;
        return;
    }

    FrameRate::gWindowFrames += 1;

    const uint64_t elapsed = now - FrameRate::gWindowStart;
    if (elapsed < FrameRate::WINDOW_NANOS) return;

    const double seconds = (double)elapsed / 1e9;
    const double perSecond = (double)FrameRate::gWindowFrames / seconds;

    FrameRate::gPerSecond.store(perSecond, std::memory_order_relaxed);
    FrameRate::gMilliseconds.store(perSecond > 0.0 ? 1000.0 / perSecond : 0.0, std::memory_order_relaxed);

    FrameRate::gWindowStart = now;
    FrameRate::gWindowFrames = 0;
}

double FrameRate::PerSecond()
{
    return FrameRate::gPerSecond.load(std::memory_order_relaxed);
}

double FrameRate::MillisecondsPerFrame()
{
    return FrameRate::gMilliseconds.load(std::memory_order_relaxed);
}

uint64_t FrameRate::TotalFrames()
{
    return FrameRate::gTotalFrames.load(std::memory_order_relaxed);
}

std::string FrameRate::Caption()
{
    const double perSecond = FrameRate::PerSecond();

    if (perSecond <= 0.0) return std::string("measuring");

    char text[64];
    snprintf(text, sizeof(text), "%.0f fps  %.1f ms", perSecond, FrameRate::MillisecondsPerFrame());

    return std::string(text);
}

std::string FrameRate::PaddedCaption()
{
    std::string caption = FrameRate::Caption();

    if ((int)caption.size() > FrameRate::CAPTION_CHARACTERS) caption.resize(FrameRate::CAPTION_CHARACTERS);
    while ((int)caption.size() < FrameRate::CAPTION_CHARACTERS) caption.push_back(' ');

    return caption;
}

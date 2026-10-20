#pragma once

#include <cstdint>
#include <string>

namespace FrameRate
{
    inline constexpr const char* OVERLAY_SETTING = "frame-overlay";
    inline constexpr uint64_t WINDOW_NANOS = 500000000ull;
    inline constexpr int OVERLAY_SCALE = 3;
    inline constexpr int CAPTION_CHARACTERS = 16;

    bool OverlayRequested();

    void Tick();

    double PerSecond();
    double MillisecondsPerFrame();
    uint64_t TotalFrames();

    std::string Caption();
    std::string PaddedCaption();
}

#pragma once

#include <cstdint>

namespace Overlay
{
    inline constexpr const char* CAPTURE_SETTING = "guest-frame-dump";

    bool Enabled();

    void DrawInto(void* pixels, int width, int height, int strideBytes);
}

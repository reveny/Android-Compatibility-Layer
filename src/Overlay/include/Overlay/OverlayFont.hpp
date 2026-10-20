#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace OverlayFont
{
    inline constexpr int GLYPH_WIDTH = 5;
    inline constexpr int GLYPH_HEIGHT = 7;
    inline constexpr int GLYPH_SPACING = 1;
    inline constexpr int MARGIN = 6;

    enum class Channels : uint8_t
    {
        Bgra,
        Rgba
    };

    struct Panel
    {
        std::vector<uint8_t> pixels;
        int width = 0;
        int height = 0;
    };

    int MeasureWidth(const std::string& text, int scale);
    int MeasureHeight(int scale);

    void Render(const std::string& text, int scale, Channels channels, Panel& panel);
    void Blend(const Panel& panel, void* destination, int destinationWidth, int destinationHeight, int destinationStride, int left,
               int top);
}

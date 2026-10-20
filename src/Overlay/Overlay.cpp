#include "Overlay/Overlay.hpp"

#include <string>

#include "Overlay/FrameRate.hpp"
#include "Overlay/OverlayFont.hpp"

bool Overlay::Enabled()
{
    return FrameRate::OverlayRequested();
}

void Overlay::DrawInto(void* pixels, int width, int height, int strideBytes)
{
    if (!pixels || width <= 0 || height <= 0) return;
    if (!FrameRate::OverlayRequested()) return;

    static OverlayFont::Panel panel;
    static std::string drawn;

    const std::string caption = FrameRate::Caption();

    if (caption != drawn || panel.pixels.empty())
    {
        drawn = caption;
        OverlayFont::Render(caption, FrameRate::OVERLAY_SCALE, OverlayFont::Channels::Bgra, panel);
    }

    OverlayFont::Blend(panel, pixels, width, height, strideBytes, OverlayFont::MARGIN, OverlayFont::MARGIN);
}

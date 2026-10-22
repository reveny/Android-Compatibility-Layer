#include "Rasterizer/Rasterizer.hpp"

#include <cmath>
#include <cstring>
#include <vector>

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_OUTLINE_H
#include FT_STROKER_H

bool RasterizerFeedStroker(const Rasterizer::Path& path, const Rasterizer::Transform& transform, FT_Stroker stroker);

bool RasterizerBuildOutline(const Rasterizer::Path& path, const Rasterizer::Transform& transform, FT_Outline& outline,
                            std::vector<FT_Vector>& points, std::vector<char>& tags, std::vector<short>& contours);

FT_Library RasterizerOutlineLibrary();

static constexpr int GRADIENT_RAMP_SIZE = 256;
static constexpr float MAXIMUM_STROKE_WIDTH = 8192.0f;
static constexpr float DEFAULT_MITER_LIMIT = 4.0f;
static constexpr float MAXIMUM_MITER_LIMIT = 1000.0f;

struct SpanTarget
{
    Rasterizer::Canvas* canvas;
    const Rasterizer::Source* source;
    Rasterizer::BlendMode blend;
    uint8_t alpha;
    Rasterizer::Rect clip;
    const Rasterizer::ClipMask* mask;
    int32_t height;
    uint32_t solid;
    bool isSolid;
    bool canCopy;
    bool directBlend;
    float axisX;
    float axisY;
    float axisScale;
    bool hasRamp;
    uint32_t ramp[GRADIENT_RAMP_SIZE];
};

static uint32_t DivideBy255(uint32_t value)
{
    return (value + 1 + (value >> 8)) >> 8;
}

static uint32_t ChannelOf(uint32_t color, int shift)
{
    return (color >> shift) & 0xFFu;
}

static uint32_t PackColor(uint32_t alpha, uint32_t red, uint32_t green, uint32_t blue)
{
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
}

static uint32_t Premultiply(uint32_t color)
{
    uint32_t alpha = ChannelOf(color, 24);
    if (alpha == 255) return color;

    return PackColor(alpha, DivideBy255(ChannelOf(color, 16) * alpha), DivideBy255(ChannelOf(color, 8) * alpha),
                     DivideBy255(ChannelOf(color, 0) * alpha));
}

static uint32_t Lerp(uint32_t first, uint32_t second, float amount)
{
    if (amount <= 0.0f) return first;
    if (amount >= 1.0f) return second;

    int32_t weight = static_cast<int32_t>(amount * 255.0f + 0.5f);

    uint32_t channels[4];
    for (int index = 0; index < 4; ++index)
    {
        int shift = index * 8;
        int32_t from = static_cast<int32_t>(ChannelOf(first, shift));
        int32_t to = static_cast<int32_t>(ChannelOf(second, shift));

        channels[index] = static_cast<uint32_t>(from + ((to - from) * weight) / 255);
    }

    return PackColor(channels[3], channels[2], channels[1], channels[0]);
}

static float ApplyTile(float value, Rasterizer::TileMode mode)
{
    if (mode == Rasterizer::TileMode::Clamp) return value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);

    float wrapped = value - std::floor(value);

    if (mode == Rasterizer::TileMode::Repeat) return wrapped;

    float doubled = value * 0.5f;
    float period = doubled - std::floor(doubled);

    return period < 0.5f ? period * 2.0f : 2.0f - period * 2.0f;
}

static uint32_t SampleStops(const Rasterizer::Source& source, float position)
{
    if (source.stopCount <= 0) return source.color;
    if (source.stopCount == 1) return source.stops[0].color;

    if (position <= source.stops[0].position) return source.stops[0].color;
    if (position >= source.stops[source.stopCount - 1].position) return source.stops[source.stopCount - 1].color;

    for (int32_t index = 1; index < source.stopCount; ++index)
    {
        const Rasterizer::GradientStop& upper = source.stops[index];
        if (position > upper.position) continue;

        const Rasterizer::GradientStop& lower = source.stops[index - 1];
        float span = upper.position - lower.position;
        float amount = span <= 0.0f ? 0.0f : (position - lower.position) / span;

        return Lerp(lower.color, upper.color, amount);
    }

    return source.stops[source.stopCount - 1].color;
}

static uint32_t TexelAt(const Rasterizer::Surface& image, int32_t x, int32_t y)
{
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x >= static_cast<int32_t>(image.width)) x = static_cast<int32_t>(image.width) - 1;
    if (y >= static_cast<int32_t>(image.height)) y = static_cast<int32_t>(image.height) - 1;

    const uint8_t* row = image.pixels + static_cast<size_t>(y) * image.stride;
    return reinterpret_cast<const uint32_t*>(row)[x];
}

static uint32_t SampleImage(const Rasterizer::Source& source, float x, float y)
{
    const Rasterizer::Surface* image = source.image;
    if (!image || !image->pixels || image->width == 0 || image->height == 0) return 0;

    float mappedX = ApplyTile(x / static_cast<float>(image->width), source.tileMode) * static_cast<float>(image->width);
    float mappedY = ApplyTile(y / static_cast<float>(image->height), source.tileMode) * static_cast<float>(image->height);

    if (!source.filter) return TexelAt(*image, static_cast<int32_t>(mappedX), static_cast<int32_t>(mappedY));

    float centredX = mappedX - 0.5f;
    float centredY = mappedY - 0.5f;

    float flooredX = std::floor(centredX);
    float flooredY = std::floor(centredY);

    int32_t leftColumn = static_cast<int32_t>(flooredX);
    int32_t topRow = static_cast<int32_t>(flooredY);

    float acrossAmount = centredX - flooredX;
    float downAmount = centredY - flooredY;

    uint32_t topLeft = TexelAt(*image, leftColumn, topRow);
    uint32_t topRight = TexelAt(*image, leftColumn + 1, topRow);
    uint32_t bottomLeft = TexelAt(*image, leftColumn, topRow + 1);
    uint32_t bottomRight = TexelAt(*image, leftColumn + 1, topRow + 1);

    return Lerp(Lerp(topLeft, topRight, acrossAmount), Lerp(bottomLeft, bottomRight, acrossAmount), downAmount);
}

static void BuildRamp(const Rasterizer::Source& source, uint32_t* ramp)
{
    for (int index = 0; index < GRADIENT_RAMP_SIZE; ++index)
    {
        float position = static_cast<float>(index) / static_cast<float>(GRADIENT_RAMP_SIZE - 1);
        ramp[index] = Premultiply(SampleStops(source, position));
    }
}

static uint32_t PremultipliedSourceAt(const SpanTarget& target, int32_t deviceX, int32_t deviceY)
{
    const Rasterizer::Source& source = *target.source;

    float x = static_cast<float>(deviceX) + 0.5f;
    float y = static_cast<float>(deviceY) + 0.5f;

    float localX = source.inverse.scaleX * x + source.inverse.skewX * y + source.inverse.translateX;
    float localY = source.inverse.skewY * x + source.inverse.scaleY * y + source.inverse.translateY;

    if (source.kind == Rasterizer::SourceKind::Image)
    {
        uint32_t sampled = SampleImage(source, localX, localY);
        if (!source.tinted) return sampled;

        uint32_t alpha = DivideBy255(ChannelOf(sampled, 24) * ChannelOf(source.tint, 24));

        return PackColor(alpha, DivideBy255(ChannelOf(source.tint, 16) * alpha),
                         DivideBy255(ChannelOf(source.tint, 8) * alpha),
                         DivideBy255(ChannelOf(source.tint, 0) * alpha));
    }

    float position = 0.0f;

    if (source.kind == Rasterizer::SourceKind::LinearGradient)
    {
        if (target.axisScale > 0.0f)
        {
            position = ((localX - source.startX) * target.axisX + (localY - source.startY) * target.axisY) * target.axisScale;
        }
    }
    else if (source.kind == Rasterizer::SourceKind::RadialGradient)
    {
        float deltaX = localX - source.startX;
        float deltaY = localY - source.startY;

        position = std::sqrt(deltaX * deltaX + deltaY * deltaY) * target.axisScale;
    }
    else
    {
        float angle = std::atan2(localY - source.startY, localX - source.startX);
        if (angle < 0.0f) angle += 6.28318530718f;

        position = angle / 6.28318530718f;
    }

    position = ApplyTile(position, source.tileMode);

    int index = static_cast<int>(position * static_cast<float>(GRADIENT_RAMP_SIZE - 1) + 0.5f);
    if (index < 0) index = 0;
    if (index >= GRADIENT_RAMP_SIZE) index = GRADIENT_RAMP_SIZE - 1;

    return target.ramp[index];
}

static uint32_t LerpChannel(uint32_t from, uint32_t to, uint32_t coverage)
{
    return DivideBy255(from * (255 - coverage) + to * coverage);
}

static uint32_t BlendDestinationModes(Rasterizer::BlendMode blend, uint32_t source, uint32_t destination, uint32_t coverage)
{
    uint32_t sourceAlpha = ChannelOf(source, 24);
    uint32_t destinationAlpha = ChannelOf(destination, 24);
    uint32_t sourceInverse = 255 - sourceAlpha;
    uint32_t destinationInverse = 255 - destinationAlpha;

    uint32_t blended[4];

    for (int channel = 0; channel < 4; ++channel)
    {
        int shift = 24 - channel * 8;
        uint32_t sourceValue = ChannelOf(source, shift);
        uint32_t destinationValue = ChannelOf(destination, shift);
        uint32_t result = destinationValue;

        switch (blend)
        {
            case Rasterizer::BlendMode::DestinationIn:
                result = DivideBy255(destinationValue * sourceAlpha);
                break;

            case Rasterizer::BlendMode::DestinationAtop:
                result = channel == 0 ? sourceAlpha
                                      : DivideBy255(destinationValue * sourceAlpha) + DivideBy255(sourceValue * destinationInverse);
                break;

            case Rasterizer::BlendMode::SourceOut:
                result = DivideBy255(sourceValue * destinationInverse);
                break;

            case Rasterizer::BlendMode::Xor:
                result = DivideBy255(sourceValue * destinationInverse) + DivideBy255(destinationValue * sourceInverse);
                break;

            default: break;
        }

        blended[channel] = LerpChannel(destinationValue, result > 255 ? 255 : result, coverage);
    }

    return PackColor(blended[0], blended[1], blended[2], blended[3]);
}

uint32_t RasterizerBlendPixel(Rasterizer::BlendMode blend, uint32_t premultiplied, uint32_t destination, uint32_t coverage)
{
    if (blend == Rasterizer::BlendMode::Destination) return destination;

    if (blend == Rasterizer::BlendMode::DestinationIn || blend == Rasterizer::BlendMode::DestinationAtop ||
        blend == Rasterizer::BlendMode::SourceOut || blend == Rasterizer::BlendMode::Xor)
    {
        return BlendDestinationModes(blend, premultiplied, destination, coverage);
    }

    if (blend == Rasterizer::BlendMode::Clear)
    {
        uint32_t kept = 255 - coverage;

        return PackColor(DivideBy255(ChannelOf(destination, 24) * kept), DivideBy255(ChannelOf(destination, 16) * kept),
                         DivideBy255(ChannelOf(destination, 8) * kept), DivideBy255(ChannelOf(destination, 0) * kept));
    }

    uint32_t sourceAlpha = DivideBy255(ChannelOf(premultiplied, 24) * coverage);
    if (sourceAlpha == 0 && blend != Rasterizer::BlendMode::Source) return destination;

    uint32_t sourceRed = DivideBy255(ChannelOf(premultiplied, 16) * coverage);
    uint32_t sourceGreen = DivideBy255(ChannelOf(premultiplied, 8) * coverage);
    uint32_t sourceBlue = DivideBy255(ChannelOf(premultiplied, 0) * coverage);

    uint32_t destinationAlpha = ChannelOf(destination, 24);
    uint32_t destinationRed = ChannelOf(destination, 16);
    uint32_t destinationGreen = ChannelOf(destination, 8);
    uint32_t destinationBlue = ChannelOf(destination, 0);

    uint32_t inverse = 255 - sourceAlpha;

    switch (blend)
    {
        case Rasterizer::BlendMode::Source:
            return PackColor(sourceAlpha, sourceRed, sourceGreen, sourceBlue);

        case Rasterizer::BlendMode::SourceIn:
            return PackColor(DivideBy255(sourceAlpha * destinationAlpha), DivideBy255(sourceRed * destinationAlpha),
                             DivideBy255(sourceGreen * destinationAlpha), DivideBy255(sourceBlue * destinationAlpha));

        case Rasterizer::BlendMode::SourceAtop:
            return PackColor(destinationAlpha, DivideBy255(sourceRed * destinationAlpha) + DivideBy255(destinationRed * inverse),
                             DivideBy255(sourceGreen * destinationAlpha) + DivideBy255(destinationGreen * inverse),
                             DivideBy255(sourceBlue * destinationAlpha) + DivideBy255(destinationBlue * inverse));

        case Rasterizer::BlendMode::DestinationOver:
        {
            uint32_t destinationInverse = 255 - destinationAlpha;
            return PackColor(destinationAlpha + DivideBy255(sourceAlpha * destinationInverse),
                             destinationRed + DivideBy255(sourceRed * destinationInverse),
                             destinationGreen + DivideBy255(sourceGreen * destinationInverse),
                             destinationBlue + DivideBy255(sourceBlue * destinationInverse));
        }

        case Rasterizer::BlendMode::DestinationOut:
            return PackColor(DivideBy255(destinationAlpha * inverse), DivideBy255(destinationRed * inverse),
                             DivideBy255(destinationGreen * inverse), DivideBy255(destinationBlue * inverse));

        case Rasterizer::BlendMode::Multiply:
            return PackColor(sourceAlpha + DivideBy255(destinationAlpha * inverse),
                             DivideBy255(sourceRed * destinationRed) + DivideBy255(destinationRed * inverse),
                             DivideBy255(sourceGreen * destinationGreen) + DivideBy255(destinationGreen * inverse),
                             DivideBy255(sourceBlue * destinationBlue) + DivideBy255(destinationBlue * inverse));

        case Rasterizer::BlendMode::Screen:
            return PackColor(sourceAlpha + DivideBy255(destinationAlpha * inverse),
                             sourceRed + destinationRed - DivideBy255(sourceRed * destinationRed),
                             sourceGreen + destinationGreen - DivideBy255(sourceGreen * destinationGreen),
                             sourceBlue + destinationBlue - DivideBy255(sourceBlue * destinationBlue));

        case Rasterizer::BlendMode::Plus:
        {
            uint32_t red = sourceRed + destinationRed;
            uint32_t green = sourceGreen + destinationGreen;
            uint32_t blue = sourceBlue + destinationBlue;
            uint32_t alpha = sourceAlpha + destinationAlpha;

            return PackColor(alpha > 255 ? 255 : alpha, red > 255 ? 255 : red, green > 255 ? 255 : green,
                             blue > 255 ? 255 : blue);
        }

        default:
            return PackColor(sourceAlpha + DivideBy255(destinationAlpha * inverse), sourceRed + DivideBy255(destinationRed * inverse),
                             sourceGreen + DivideBy255(destinationGreen * inverse),
                             sourceBlue + DivideBy255(destinationBlue * inverse));
    }
}

static void PaintSpans(int y, int count, const FT_Span* spans, void* user)
{
    SpanTarget* target = static_cast<SpanTarget*>(user);

    int32_t deviceY = y;
    if (deviceY < target->clip.top || deviceY >= target->clip.bottom) return;

    uint8_t* row = target->canvas->surface.pixels + static_cast<size_t>(deviceY) * target->canvas->surface.stride;

    for (int index = 0; index < count; ++index)
    {
        int32_t left = spans[index].x;
        int32_t right = left + spans[index].len;

        if (left < target->clip.left) left = target->clip.left;
        if (right > target->clip.right) right = target->clip.right;

        if (target->canCopy && spans[index].coverage == 255)
        {
            uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + left;
            for (int32_t deviceX = left; deviceX < right; ++deviceX) *pixel++ = target->solid;
            continue;
        }

        for (int32_t deviceX = left; deviceX < right; ++deviceX)
        {
            uint32_t coverage = spans[index].coverage;

            if (target->mask)
            {
                uint32_t clipping = target->mask->samples[static_cast<size_t>(deviceY) * target->mask->width + deviceX];
                if (clipping == 0) continue;

                coverage = DivideBy255(coverage * clipping);
            }

            if (target->alpha != 255) coverage = DivideBy255(coverage * target->alpha);
            if (coverage == 0) continue;

            uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + deviceX;
            uint32_t premultiplied = target->isSolid ? target->solid : PremultipliedSourceAt(*target, deviceX, deviceY);

            if (target->directBlend && coverage == 255 && ChannelOf(premultiplied, 24) == 255)
            {
                *pixel = premultiplied;
                continue;
            }

            *pixel = RasterizerBlendPixel(target->blend, premultiplied, *pixel, coverage);
        }
    }
}

Rasterizer::Result Rasterizer::FillCoverage(Canvas& canvas, const Coverage& mask, const Source& source, BlendMode blend,
                                            uint8_t alpha)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (!mask.samples) return Result::OutOfRange;

    const State& state = StateAt(canvas, canvas.depth - 1);

    Rect surfaceBounds{0, 0, static_cast<int32_t>(canvas.surface.width), static_cast<int32_t>(canvas.surface.height)};
    Rect band = Intersect(Intersect(state.clip, surfaceBounds), Rect{0, 0, mask.width, mask.height});

    if (IsEmpty(band)) return Result::Ok;

    SpanTarget target;
    target.canvas = &canvas;
    target.source = &source;
    target.blend = blend;
    target.alpha = alpha;
    target.clip = band;
    target.mask = state.mask;
    target.height = static_cast<int32_t>(canvas.surface.height);
    target.isSolid = source.kind == Rasterizer::SourceKind::Solid;
    target.solid = target.isSolid ? Premultiply(source.color) : 0;
    target.directBlend = false;
    target.canCopy = false;
    target.axisX = source.endX - source.startX;
    target.axisY = source.endY - source.startY;
    target.axisScale = 0.0f;

    if (source.kind == SourceKind::LinearGradient)
    {
        float lengthSquared = target.axisX * target.axisX + target.axisY * target.axisY;
        if (lengthSquared > 0.0f) target.axisScale = 1.0f / lengthSquared;
    }
    else if (source.kind == SourceKind::RadialGradient && source.radius > 0.0f)
    {
        target.axisScale = 1.0f / source.radius;
    }

    target.hasRamp = source.kind == SourceKind::LinearGradient || source.kind == SourceKind::RadialGradient
                     || source.kind == SourceKind::SweepGradient;

    if (target.hasRamp) BuildRamp(source, target.ramp);

    for (int32_t y = band.top; y < band.bottom; ++y)
    {
        const uint8_t* maskRow = mask.samples + static_cast<size_t>(y) * mask.pitch;
        uint8_t* row = canvas.surface.pixels + static_cast<size_t>(y) * canvas.surface.stride;

        for (int32_t x = band.left; x < band.right; ++x)
        {
            uint32_t coverage = maskRow[x];
            if (coverage == 0) continue;

            if (state.mask)
            {
                uint32_t clipping = state.mask->samples[static_cast<size_t>(y) * state.mask->width + x];
                if (clipping == 0) continue;

                coverage = DivideBy255(coverage * clipping);
            }

            if (alpha != 255) coverage = DivideBy255(coverage * alpha);
            if (coverage == 0) continue;

            uint32_t premultiplied = target.isSolid ? target.solid : PremultipliedSourceAt(target, x, y);
            uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + x;

            *pixel = RasterizerBlendPixel(blend, premultiplied, *pixel, coverage);
        }
    }

    return Result::Ok;
}

static Rasterizer::Result RenderOutline(Rasterizer::Canvas& canvas, FT_Outline& outline, const Rasterizer::Source& source,
                                        Rasterizer::FillRule rule, Rasterizer::BlendMode blend, uint8_t alpha)
{
    FT_Library library = RasterizerOutlineLibrary();
    if (!library) return Rasterizer::Result::AllocationFailed;

    if (rule == Rasterizer::FillRule::EvenOdd) outline.flags |= FT_OUTLINE_EVEN_ODD_FILL;
    else outline.flags &= ~FT_OUTLINE_EVEN_ODD_FILL;

    const Rasterizer::State& state = Rasterizer::StateAt(canvas, canvas.depth - 1);

    SpanTarget target;
    target.canvas = &canvas;
    target.source = &source;
    target.blend = blend;
    target.alpha = alpha;
    target.clip = state.clip;
    target.mask = state.mask;
    target.height = static_cast<int32_t>(canvas.surface.height);
    target.isSolid = source.kind == Rasterizer::SourceKind::Solid;
    target.solid = target.isSolid ? Premultiply(source.color) : 0;
    target.directBlend = !state.mask && alpha == 255 &&
                         (blend == Rasterizer::BlendMode::SourceOver || blend == Rasterizer::BlendMode::Source);
    target.canCopy = target.isSolid && target.directBlend && ChannelOf(source.color, 24) == 255;

    target.axisX = source.endX - source.startX;
    target.axisY = source.endY - source.startY;
    target.axisScale = 0.0f;

    if (source.kind == Rasterizer::SourceKind::LinearGradient)
    {
        float lengthSquared = target.axisX * target.axisX + target.axisY * target.axisY;
        if (lengthSquared > 0.0f) target.axisScale = 1.0f / lengthSquared;
    }
    else if (source.kind == Rasterizer::SourceKind::RadialGradient && source.radius > 0.0f)
    {
        target.axisScale = 1.0f / source.radius;
    }

    target.hasRamp = source.kind == Rasterizer::SourceKind::LinearGradient ||
                     source.kind == Rasterizer::SourceKind::RadialGradient ||
                     source.kind == Rasterizer::SourceKind::SweepGradient;

    if (target.hasRamp) BuildRamp(source, target.ramp);

    Rasterizer::Rect surfaceBounds{0, 0, static_cast<int32_t>(canvas.surface.width),
                                   static_cast<int32_t>(canvas.surface.height)};
    Rasterizer::Rect band = Rasterizer::Intersect(state.clip, surfaceBounds);

    if (Rasterizer::IsEmpty(band)) return Rasterizer::Result::Ok;

    FT_Raster_Params parameters;
    std::memset(&parameters, 0, sizeof(parameters));
    parameters.flags = FT_RASTER_FLAG_AA | FT_RASTER_FLAG_DIRECT | FT_RASTER_FLAG_CLIP;
    parameters.gray_spans = &PaintSpans;
    parameters.user = &target;
    parameters.clip_box.xMin = band.left;
    parameters.clip_box.yMin = band.top;
    parameters.clip_box.xMax = band.right;
    parameters.clip_box.yMax = band.bottom;

    if (FT_Outline_Render(library, &outline, &parameters) != 0) return Rasterizer::Result::OutOfRange;

    return Rasterizer::Result::Ok;
}

Rasterizer::Result Rasterizer::FillPath(Canvas& canvas, const Path& path, const Source& source, FillRule rule, BlendMode blend,
                                        uint8_t alpha)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (PathIsEmpty(path)) return Result::Ok;

    std::vector<FT_Vector> points;
    std::vector<char> tags;
    std::vector<short> contours;

    FT_Outline outline;
    if (!RasterizerBuildOutline(path, StateAt(canvas, canvas.depth - 1).transform, outline, points, tags, contours))
        return Result::Ok;

    return RenderOutline(canvas, outline, source, rule, blend, alpha);
}

Rasterizer::Result Rasterizer::StrokePath(Canvas& canvas, const Path& path, const Source& source, const StrokeStyle& style,
                                          BlendMode blend, uint8_t alpha)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (PathIsEmpty(path)) return Result::Ok;

    FT_Library library = RasterizerOutlineLibrary();
    if (!library) return Result::AllocationFailed;

    FT_Stroker stroker = nullptr;
    if (FT_Stroker_New(library, &stroker) != 0) return Result::AllocationFailed;

    FT_Stroker_LineCap cap = FT_STROKER_LINECAP_BUTT;
    if (style.cap == LineCap::Round) cap = FT_STROKER_LINECAP_ROUND;
    if (style.cap == LineCap::Square) cap = FT_STROKER_LINECAP_SQUARE;

    FT_Stroker_LineJoin join = FT_STROKER_LINEJOIN_MITER_VARIABLE;
    if (style.join == LineJoin::Round) join = FT_STROKER_LINEJOIN_ROUND;
    if (style.join == LineJoin::Bevel) join = FT_STROKER_LINEJOIN_BEVEL;

    const Transform& transform = StateAt(canvas, canvas.depth - 1).transform;
    float scale = std::sqrt(std::fabs(transform.scaleX * transform.scaleY - transform.skewX * transform.skewY));
    if (!std::isfinite(scale) || scale <= 0.0f) scale = 1.0f;

    float width = style.width * scale;
    if (!std::isfinite(width) || width < 1.0f) width = 1.0f;
    if (width > MAXIMUM_STROKE_WIDTH) width = MAXIMUM_STROKE_WIDTH;

    float miterLimit = style.miterLimit;
    if (!std::isfinite(miterLimit) || miterLimit < 1.0f) miterLimit = DEFAULT_MITER_LIMIT;
    if (miterLimit > MAXIMUM_MITER_LIMIT) miterLimit = MAXIMUM_MITER_LIMIT;

    FT_Stroker_Set(stroker, static_cast<FT_Fixed>(width * 32.0f), cap, join, static_cast<FT_Fixed>(miterLimit * 65536.0f));

    Result outcome = Result::Ok;

    if (RasterizerFeedStroker(path, transform, stroker))
    {
        FT_UInt strokedPoints = 0;
        FT_UInt strokedContours = 0;

        if (FT_Stroker_GetCounts(stroker, &strokedPoints, &strokedContours) == 0)
        {
            FT_Outline stroked;
            if (FT_Outline_New(library, strokedPoints, static_cast<FT_Int>(strokedContours), &stroked) == 0)
            {
                stroked.n_points = 0;
                stroked.n_contours = 0;

                FT_Stroker_Export(stroker, &stroked);
                outcome = RenderOutline(canvas, stroked, source, FillRule::NonZero, blend, alpha);

                FT_Outline_Done(library, &stroked);
            }
            else
            {
                outcome = Result::AllocationFailed;
            }
        }
    }

    FT_Stroker_Done(stroker);
    return outcome;
}

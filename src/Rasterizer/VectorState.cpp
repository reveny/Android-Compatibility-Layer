#include "Rasterizer/Rasterizer.hpp"

#include <cmath>
#include <cstdlib>
#include <cstring>
#include <vector>

uint32_t RasterizerBlendPixel(Rasterizer::BlendMode blend, uint32_t premultiplied, uint32_t destination, uint32_t coverage);
void RasterizerFlattenPath(const Rasterizer::Path& path, std::vector<float>& points, std::vector<int32_t>& starts);

Rasterizer::Transform Rasterizer::Multiply(const Transform& outer, const Transform& inner)
{
    Transform result;

    result.scaleX = outer.scaleX * inner.scaleX + outer.skewX * inner.skewY;
    result.skewY = outer.skewY * inner.scaleX + outer.scaleY * inner.skewY;
    result.skewX = outer.scaleX * inner.skewX + outer.skewX * inner.scaleY;
    result.scaleY = outer.skewY * inner.skewX + outer.scaleY * inner.scaleY;
    result.translateX = outer.scaleX * inner.translateX + outer.skewX * inner.translateY + outer.translateX;
    result.translateY = outer.skewY * inner.translateX + outer.scaleY * inner.translateY + outer.translateY;

    return result;
}

bool Rasterizer::Invert(const Transform& transform, Transform& inverse)
{
    float determinant = transform.scaleX * transform.scaleY - transform.skewX * transform.skewY;
    if (std::fabs(determinant) < 1e-9f) return false;

    float reciprocal = 1.0f / determinant;

    inverse.scaleX = transform.scaleY * reciprocal;
    inverse.skewY = -transform.skewY * reciprocal;
    inverse.skewX = -transform.skewX * reciprocal;
    inverse.scaleY = transform.scaleX * reciprocal;
    inverse.translateX = (transform.skewX * transform.translateY - transform.scaleY * transform.translateX) * reciprocal;
    inverse.translateY = (transform.skewY * transform.translateX - transform.scaleX * transform.translateY) * reciprocal;

    return true;
}

const Rasterizer::Transform& Rasterizer::CurrentTransform(const Canvas& canvas)
{
    static const Transform IDENTITY;
    if (canvas.depth < 1) return IDENTITY;

    return StateAt(canvas, canvas.depth - 1).transform;
}

Rasterizer::Result Rasterizer::Concat(Canvas& canvas, const Transform& transform)
{
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    State& state = TopState(canvas);
    state.transform = Multiply(state.transform, transform);

    return Result::Ok;
}

Rasterizer::Result Rasterizer::SetTransform(Canvas& canvas, const Transform& transform)
{
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    TopState(canvas).transform = transform;
    return Result::Ok;
}

static Rasterizer::ClipMask* AllocateMask(int32_t width, int32_t height)
{
    if (width <= 0 || height <= 0) return nullptr;

    Rasterizer::ClipMask* mask = new Rasterizer::ClipMask();
    mask->width = width;
    mask->height = height;
    mask->references = 1;
    mask->samples = static_cast<uint8_t*>(std::calloc(static_cast<size_t>(width) * height, 1));

    if (!mask->samples)
    {
        delete mask;
        return nullptr;
    }

    return mask;
}

void RasterizerRetainMask(Rasterizer::ClipMask* mask)
{
    if (mask) mask->references += 1;
}

void RasterizerReleaseMask(Rasterizer::ClipMask* mask)
{
    if (!mask) return;

    mask->references -= 1;
    if (mask->references > 0) return;

    std::free(mask->samples);
    delete mask;
}

Rasterizer::Result Rasterizer::ClipPath(Canvas& canvas, const Path& path, FillRule rule, bool subtract)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    int32_t width = static_cast<int32_t>(canvas.surface.width);
    int32_t height = static_cast<int32_t>(canvas.surface.height);

    ClipMask* built = AllocateMask(width, height);
    if (!built) return Result::AllocationFailed;

    Surface maskSurface;
    maskSurface.pixels = static_cast<uint8_t*>(std::calloc(static_cast<size_t>(width) * height * BYTES_PER_PIXEL, 1));
    maskSurface.width = static_cast<uint32_t>(width);
    maskSurface.height = static_cast<uint32_t>(height);
    maskSurface.stride = static_cast<uint32_t>(width) * BYTES_PER_PIXEL;

    if (!maskSurface.pixels)
    {
        RasterizerReleaseMask(built);
        return Result::AllocationFailed;
    }

    Canvas scratch;
    scratch.surface = maskSurface;
    scratch.depth = 1;
    scratch.stack[0].clip = Rect{0, 0, width, height};
    scratch.stack[0].transform = StateAt(canvas, canvas.depth - 1).transform;
    scratch.stack[0].mask = nullptr;

    Source white;
    white.kind = SourceKind::Solid;
    white.color = 0xFFFFFFFFu;

    Result drawn = FillPath(scratch, path, white, rule, BlendMode::Source, 255);

    if (drawn == Result::Ok)
    {
        State& state = TopState(canvas);

        for (int32_t y = 0; y < height; ++y)
        {
            const uint32_t* row = reinterpret_cast<const uint32_t*>(maskSurface.pixels + static_cast<size_t>(y) * maskSurface.stride);

            for (int32_t x = 0; x < width; ++x)
            {
                uint32_t coverage = (row[x] >> 24) & 0xFFu;
                if (subtract) coverage = 255 - coverage;

                uint32_t existing = 255;
                if (state.mask) existing = state.mask->samples[static_cast<size_t>(y) * state.mask->width + x];
                else if (x < state.clip.left || x >= state.clip.right || y < state.clip.top || y >= state.clip.bottom) existing = 0;

                built->samples[static_cast<size_t>(y) * width + x] =
                    static_cast<uint8_t>((coverage * existing + 127) / 255);
            }
        }

        RasterizerReleaseMask(state.mask);
        state.mask = built;
    }
    else
    {
        RasterizerReleaseMask(built);
    }

    std::free(maskSurface.pixels);
    return drawn;
}

static Rasterizer::Rect MapBounds(const Rasterizer::Transform& transform, const Rasterizer::Rect& bounds)
{
    float cornerX[4] = {static_cast<float>(bounds.left), static_cast<float>(bounds.right),
                        static_cast<float>(bounds.right), static_cast<float>(bounds.left)};
    float cornerY[4] = {static_cast<float>(bounds.top), static_cast<float>(bounds.top),
                        static_cast<float>(bounds.bottom), static_cast<float>(bounds.bottom)};

    float left = 0.0f;
    float top = 0.0f;
    float right = 0.0f;
    float bottom = 0.0f;

    for (int index = 0; index < 4; ++index)
    {
        float x = transform.scaleX * cornerX[index] + transform.skewX * cornerY[index] + transform.translateX;
        float y = transform.skewY * cornerX[index] + transform.scaleY * cornerY[index] + transform.translateY;

        if (index == 0)
        {
            left = right = x;
            top = bottom = y;
            continue;
        }

        if (x < left) left = x;
        if (x > right) right = x;
        if (y < top) top = y;
        if (y > bottom) bottom = y;
    }

    Rasterizer::Rect mapped;
    mapped.left = static_cast<int32_t>(std::floor(left));
    mapped.top = static_cast<int32_t>(std::floor(top));
    mapped.right = static_cast<int32_t>(std::ceil(right));
    mapped.bottom = static_cast<int32_t>(std::ceil(bottom));

    return mapped;
}

struct Rasterizer::Layer
{
    Surface parent;
    Surface offscreen;
    Rect bounds;
    int depth = 0;
    uint8_t alpha = 255;
    BlendMode blend = BlendMode::SourceOver;
};

Rasterizer::Result Rasterizer::SaveLayer(Canvas& canvas, const Rect& bounds, uint8_t alpha, BlendMode blend)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    Rect surfaceBounds{0, 0, static_cast<int32_t>(canvas.surface.width), static_cast<int32_t>(canvas.surface.height)};
    Rect area = Intersect(Intersect(MapBounds(TopState(canvas).transform, bounds), TopState(canvas).clip), surfaceBounds);

    if (canvas.layerCount >= canvas.layerCapacity)
    {
        int capacity = canvas.layerCapacity > 0 ? canvas.layerCapacity * 2 : 4;
        Layer* grown = static_cast<Layer*>(std::realloc(canvas.layers, sizeof(Layer) * capacity));
        if (!grown) return Result::AllocationFailed;

        canvas.layers = grown;
        canvas.layerCapacity = capacity;
    }

    Surface offscreen;
    if (CreateSurface(canvas.surface.width, canvas.surface.height, offscreen) != Result::Ok) return Result::AllocationFailed;

    std::memset(offscreen.pixels, 0, static_cast<size_t>(offscreen.stride) * offscreen.height);

    if (Save(canvas) != Result::Ok)
    {
        DestroySurface(offscreen);
        return Result::StateStackOverflow;
    }

    Layer& layer = canvas.layers[canvas.layerCount];
    layer.parent = canvas.surface;
    layer.offscreen = offscreen;
    layer.bounds = area;
    layer.depth = canvas.depth - 1;
    layer.alpha = alpha;
    layer.blend = blend;
    canvas.layerCount += 1;

    canvas.surface = offscreen;
    TopState(canvas).clip = area;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::RestoreLayer(Canvas& canvas)
{
    if (canvas.layerCount < 1) return Result::StateStackUnderflow;

    Layer layer = canvas.layers[canvas.layerCount - 1];
    canvas.layerCount -= 1;

    while (canvas.depth > layer.depth + 1) Restore(canvas);

    RasterizerReleaseMask(TopState(canvas).mask);
    TopState(canvas).mask = nullptr;

    canvas.surface = layer.parent;
    canvas.depth = layer.depth;

    for (int32_t y = layer.bounds.top; y < layer.bounds.bottom; ++y)
    {
        const uint32_t* source =
            reinterpret_cast<const uint32_t*>(layer.offscreen.pixels + static_cast<size_t>(y) * layer.offscreen.stride);
        uint32_t* destination = reinterpret_cast<uint32_t*>(layer.parent.pixels + static_cast<size_t>(y) * layer.parent.stride);

        for (int32_t x = layer.bounds.left; x < layer.bounds.right; ++x)
        {
            destination[x] = RasterizerBlendPixel(layer.blend, source[x], destination[x], layer.alpha);
        }
    }

    DestroySurface(layer.offscreen);
    return Result::Ok;
}

Rasterizer::Result Rasterizer::FillPathBlurred(Canvas& canvas, const Path& path, const Source& source, FillRule rule,
                                               BlendMode blend, uint8_t alpha, float radius)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (radius <= 0.0f) return FillPath(canvas, path, source, rule, blend, alpha);

    int32_t width = static_cast<int32_t>(canvas.surface.width);
    int32_t height = static_cast<int32_t>(canvas.surface.height);

    Surface scratch;
    if (CreateSurface(canvas.surface.width, canvas.surface.height, scratch) != Result::Ok) return Result::AllocationFailed;

    std::memset(scratch.pixels, 0, static_cast<size_t>(scratch.stride) * scratch.height);

    Canvas offscreen;
    offscreen.surface = scratch;
    offscreen.depth = 1;
    offscreen.stack[0].clip = Rect{0, 0, width, height};
    offscreen.stack[0].transform = TopState(canvas).transform;
    offscreen.stack[0].mask = nullptr;

    Source white;
    white.color = 0xFFFFFFFFu;

    Result drawn = FillPath(offscreen, path, white, rule, BlendMode::Source, 255);

    if (drawn != Result::Ok)
    {
        DestroySurface(scratch);
        return drawn;
    }

    std::vector<uint8_t> coverage(static_cast<size_t>(width) * height);

    for (int32_t y = 0; y < height; ++y)
    {
        const uint32_t* row = reinterpret_cast<const uint32_t*>(scratch.pixels + static_cast<size_t>(y) * scratch.stride);
        for (int32_t x = 0; x < width; ++x) coverage[static_cast<size_t>(y) * width + x] = (row[x] >> 24) & 0xFFu;
    }

    DestroySurface(scratch);
    BlurMask(coverage.data(), width, height, width, radius);

    Coverage mask;
    mask.samples = coverage.data();
    mask.width = width;
    mask.height = height;
    mask.pitch = width;

    return FillCoverage(canvas, mask, source, blend, alpha);
}

Rasterizer::Result Rasterizer::DashPath(const Path& source, Path& into, const float* intervals, int intervalCount,
                                        float phase)
{
    if (!intervals || intervalCount < 2) return Result::OutOfRange;

    float cycle = 0.0f;
    for (int index = 0; index < intervalCount; ++index)
    {
        if (intervals[index] < 0.0f) return Result::OutOfRange;
        cycle += intervals[index];
    }

    if (cycle <= 0.0f) return Result::OutOfRange;

    std::vector<float> points;
    std::vector<int32_t> starts;
    RasterizerFlattenPath(source, points, starts);

    int32_t slot = 0;
    float remaining = intervals[0];
    bool drawing = true;

    float walked = phase;
    while (walked > 0.0f)
    {
        if (walked < remaining)
        {
            remaining -= walked;
            break;
        }

        walked -= remaining;
        slot = (slot + 1) % intervalCount;
        remaining = intervals[slot];
        drawing = !drawing;
    }

    for (size_t contour = 0; contour + 1 < starts.size(); ++contour)
    {
        int32_t first = starts[contour];
        int32_t last = starts[contour + 1];
        bool penDown = false;

        for (int32_t index = first; index + 1 < last; ++index)
        {
            float startX = points[static_cast<size_t>(index) * 2];
            float startY = points[static_cast<size_t>(index) * 2 + 1];
            float endX = points[static_cast<size_t>(index + 1) * 2];
            float endY = points[static_cast<size_t>(index + 1) * 2 + 1];

            float deltaX = endX - startX;
            float deltaY = endY - startY;
            float length = std::sqrt(deltaX * deltaX + deltaY * deltaY);

            if (length <= 0.0f) continue;

            float travelled = 0.0f;
            while (travelled < length)
            {
                float step = length - travelled;
                if (step > remaining) step = remaining;

                float fromX = startX + deltaX * (travelled / length);
                float fromY = startY + deltaY * (travelled / length);
                float toX = startX + deltaX * ((travelled + step) / length);
                float toY = startY + deltaY * ((travelled + step) / length);

                if (drawing)
                {
                    if (!penDown)
                    {
                        MoveTo(into, fromX, fromY);
                        penDown = true;
                    }

                    LineTo(into, toX, toY);
                }
                else
                {
                    penDown = false;
                }

                travelled += step;
                remaining -= step;

                if (remaining > 0.0f) continue;

                slot = (slot + 1) % intervalCount;
                remaining = intervals[slot];
                drawing = !drawing;
                penDown = false;
            }
        }
    }

    return Result::Ok;
}

Rasterizer::Result Rasterizer::BlurMask(uint8_t* samples, int32_t width, int32_t height, int32_t pitch, float radius)
{
    if (!samples || width <= 0 || height <= 0) return Result::OutOfRange;

    int32_t span = static_cast<int32_t>(radius);
    if (span < 1) return Result::Ok;

    std::vector<uint8_t> scratch(static_cast<size_t>(width) * height);

    for (int pass = 0; pass < 2; ++pass)
    {
        for (int32_t y = 0; y < height; ++y)
        {
            uint8_t* row = samples + static_cast<size_t>(y) * pitch;

            for (int32_t x = 0; x < width; ++x)
            {
                int32_t total = 0;
                int32_t count = 0;

                for (int32_t offset = -span; offset <= span; ++offset)
                {
                    int32_t sample = x + offset;
                    if (sample < 0 || sample >= width) continue;

                    total += row[sample];
                    count += 1;
                }

                scratch[static_cast<size_t>(y) * width + x] = static_cast<uint8_t>(count == 0 ? 0 : total / count);
            }
        }

        for (int32_t x = 0; x < width; ++x)
        {
            for (int32_t y = 0; y < height; ++y)
            {
                int32_t total = 0;
                int32_t count = 0;

                for (int32_t offset = -span; offset <= span; ++offset)
                {
                    int32_t sample = y + offset;
                    if (sample < 0 || sample >= height) continue;

                    total += scratch[static_cast<size_t>(sample) * width + x];
                    count += 1;
                }

                samples[static_cast<size_t>(y) * pitch + x] = static_cast<uint8_t>(count == 0 ? 0 : total / count);
            }
        }
    }

    return Result::Ok;
}

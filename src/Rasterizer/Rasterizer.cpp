#include "Rasterizer/Rasterizer.hpp"

#include <cmath>
#include <cstdlib>
#include <cstring>

static constexpr float AXIS_TOLERANCE = 1.0f / (1 << 12);

void RasterizerRetainMask(Rasterizer::ClipMask* mask);
void RasterizerReleaseMask(Rasterizer::ClipMask* mask);

bool Rasterizer::IsEmpty(const Rect& rect)
{
    return rect.right <= rect.left || rect.bottom <= rect.top;
}

Rasterizer::Rect Rasterizer::Intersect(const Rect& first, const Rect& second)
{
    Rect result;
    result.left = first.left > second.left ? first.left : second.left;
    result.top = first.top > second.top ? first.top : second.top;
    result.right = first.right < second.right ? first.right : second.right;
    result.bottom = first.bottom < second.bottom ? first.bottom : second.bottom;

    if (IsEmpty(result)) return Rect{result.left, result.top, result.left, result.top};

    return result;
}

Rasterizer::Result Rasterizer::CreateSurface(uint32_t width, uint32_t height, Surface& surface)
{
    if (width < 1 || height < 1) return Result::InvalidGeometry;
    if (width > MAX_DIMENSION || height > MAX_DIMENSION) return Result::InvalidGeometry;

    uint32_t stride = width * BYTES_PER_PIXEL;
    size_t sizeBytes = static_cast<size_t>(stride) * height;

    uint8_t* pixels = static_cast<uint8_t*>(calloc(1, sizeBytes));
    if (!pixels) return Result::AllocationFailed;

    surface.pixels = pixels;
    surface.width = width;
    surface.height = height;
    surface.stride = stride;
    surface.ownsPixels = true;
    return Result::Ok;
}

Rasterizer::Result Rasterizer::WrapSurface(uint8_t* pixels, uint32_t width, uint32_t height, uint32_t stride, Surface& surface)
{
    if (!pixels) return Result::InvalidSurface;
    if (width < 1 || height < 1) return Result::InvalidGeometry;
    if (width > MAX_DIMENSION || height > MAX_DIMENSION) return Result::InvalidGeometry;
    if (stride < width * BYTES_PER_PIXEL) return Result::InvalidGeometry;

    surface.pixels = pixels;
    surface.width = width;
    surface.height = height;
    surface.stride = stride;
    surface.ownsPixels = false;
    return Result::Ok;
}

void Rasterizer::DestroySurface(Surface& surface)
{
    if (surface.ownsPixels) free(surface.pixels);

    surface.pixels = nullptr;
    surface.width = 0;
    surface.height = 0;
    surface.stride = 0;
    surface.ownsPixels = false;
}

Rasterizer::State& Rasterizer::StateAt(Canvas& canvas, int index)
{
    if (index < MAX_STATE_DEPTH) return canvas.stack[index];

    return canvas.deepStack[index - MAX_STATE_DEPTH];
}

const Rasterizer::State& Rasterizer::StateAt(const Canvas& canvas, int index)
{
    if (index < MAX_STATE_DEPTH) return canvas.stack[index];

    return canvas.deepStack[index - MAX_STATE_DEPTH];
}

Rasterizer::State& Rasterizer::TopState(Canvas& canvas)
{
    return StateAt(canvas, canvas.depth > 0 ? canvas.depth - 1 : 0);
}

static bool ReserveDepth(Rasterizer::Canvas& canvas, int depth)
{
    if (depth < Rasterizer::MAX_STATE_DEPTH) return true;

    int needed = depth - Rasterizer::MAX_STATE_DEPTH + 1;
    if (needed <= canvas.deepCapacity) return true;

    int capacity = canvas.deepCapacity > 0 ? canvas.deepCapacity * 2 : Rasterizer::MAX_STATE_DEPTH;
    while (capacity < needed) capacity *= 2;

    Rasterizer::State* grown = static_cast<Rasterizer::State*>(std::realloc(canvas.deepStack, sizeof(Rasterizer::State) * capacity));
    if (!grown) return false;

    canvas.deepStack = grown;
    canvas.deepCapacity = capacity;
    return true;
}

Rasterizer::Result Rasterizer::Begin(const Surface& surface, Canvas& canvas)
{
    if (!surface.pixels) return Result::InvalidSurface;

    End(canvas);

    canvas.surface = surface;
    canvas.depth = 1;
    canvas.stack[0] = State{};
    canvas.stack[0].clip = Rect{0, 0, static_cast<int32_t>(surface.width), static_cast<int32_t>(surface.height)};
    return Result::Ok;
}

void Rasterizer::End(Canvas& canvas)
{
    while (canvas.layerCount > 0) RestoreLayer(canvas);

    for (int index = 0; index < canvas.depth; ++index) RasterizerReleaseMask(StateAt(canvas, index).mask);

    canvas.depth = 0;

    std::free(canvas.deepStack);
    canvas.deepStack = nullptr;
    canvas.deepCapacity = 0;

    std::free(canvas.layers);
    canvas.layers = nullptr;
    canvas.layerCapacity = 0;
}

const Rasterizer::State& Rasterizer::CurrentState(const Canvas& canvas)
{
    return StateAt(canvas, canvas.depth > 0 ? canvas.depth - 1 : 0);
}

Rasterizer::Result Rasterizer::Save(Canvas& canvas)
{
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (!ReserveDepth(canvas, canvas.depth)) return Result::StateStackOverflow;

    StateAt(canvas, canvas.depth) = StateAt(canvas, canvas.depth - 1);
    RasterizerRetainMask(StateAt(canvas, canvas.depth).mask);
    canvas.depth++;
    return Result::Ok;
}

Rasterizer::Result Rasterizer::Restore(Canvas& canvas)
{
    if (canvas.depth <= 1) return Result::StateStackUnderflow;

    RasterizerReleaseMask(StateAt(canvas, canvas.depth - 1).mask);
    canvas.depth--;
    return Result::Ok;
}

Rasterizer::Result Rasterizer::Translate(Canvas& canvas, int32_t deltaX, int32_t deltaY)
{
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    State& state = TopState(canvas);
    state.translateX += deltaX;
    state.translateY += deltaY;

    Transform step;
    step.translateX = static_cast<float>(deltaX);
    step.translateY = static_cast<float>(deltaY);
    state.transform = Multiply(state.transform, step);

    return Result::Ok;
}

Rasterizer::Result Rasterizer::ClipRect(Canvas& canvas, const Rect& rect)
{
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    State& state = TopState(canvas);
    const Transform& transform = state.transform;

    bool axisAligned = std::fabs(transform.skewX) < AXIS_TOLERANCE && std::fabs(transform.skewY) < AXIS_TOLERANCE;

    if (!axisAligned)
    {
        Path* shape = CreatePath();
        if (!shape) return Result::AllocationFailed;

        AddRectangle(*shape, static_cast<float>(rect.left), static_cast<float>(rect.top), static_cast<float>(rect.right),
                     static_cast<float>(rect.bottom));

        Result outcome = ClipPath(canvas, *shape, FillRule::NonZero, false);
        DestroyPath(shape);

        return outcome;
    }

    float firstX = transform.scaleX * rect.left + transform.translateX;
    float secondX = transform.scaleX * rect.right + transform.translateX;
    float firstY = transform.scaleY * rect.top + transform.translateY;
    float secondY = transform.scaleY * rect.bottom + transform.translateY;

    Rect mapped;
    mapped.left = static_cast<int32_t>(std::lround(firstX < secondX ? firstX : secondX));
    mapped.right = static_cast<int32_t>(std::lround(firstX < secondX ? secondX : firstX));
    mapped.top = static_cast<int32_t>(std::lround(firstY < secondY ? firstY : secondY));
    mapped.bottom = static_cast<int32_t>(std::lround(firstY < secondY ? secondY : firstY));

    state.clip = Intersect(state.clip, mapped);
    return Result::Ok;
}

static void FillDeviceRect(const Rasterizer::Surface& surface, const Rasterizer::Rect& area, uint32_t color)
{
    for (int32_t y = area.top; y < area.bottom; ++y)
    {
        uint8_t* row = surface.pixels + static_cast<size_t>(y) * surface.stride;
        uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + area.left;
        for (int32_t x = area.left; x < area.right; ++x)
        {
            *pixel++ = color;
        }
    }
}

static Rasterizer::Rect ToDeviceRect(const Rasterizer::Canvas& canvas, const Rasterizer::Rect& rect)
{
    const Rasterizer::State& state = Rasterizer::CurrentState(canvas);

    Rasterizer::Rect device;
    device.left = rect.left + state.translateX;
    device.top = rect.top + state.translateY;
    device.right = rect.right + state.translateX;
    device.bottom = rect.bottom + state.translateY;

    Rasterizer::Rect bounds{0, 0, static_cast<int32_t>(canvas.surface.width), static_cast<int32_t>(canvas.surface.height)};
    return Rasterizer::Intersect(Rasterizer::Intersect(device, state.clip), bounds);
}

static uint32_t DivideBy255(uint32_t value)
{
    uint32_t sum = value + 128;
    return (sum + (sum >> 8)) >> 8;
}

static uint32_t BlendChannel(uint32_t source, uint32_t destination, uint32_t alpha)
{
    return DivideBy255(source * alpha + destination * (255u - alpha));
}

static uint32_t BlendPixel(uint32_t source, uint32_t destination, uint32_t coverage)
{
    uint32_t sourceAlpha = (source >> 24) & 0xFFu;
    uint32_t alpha = DivideBy255(coverage * sourceAlpha);
    if (alpha == 0) return destination;
    if (alpha == 255) return source | 0xFF000000u;

    uint32_t red = BlendChannel((source >> 16) & 0xFFu, (destination >> 16) & 0xFFu, alpha);
    uint32_t green = BlendChannel((source >> 8) & 0xFFu, (destination >> 8) & 0xFFu, alpha);
    uint32_t blue = BlendChannel(source & 0xFFu, destination & 0xFFu, alpha);

    return 0xFF000000u | (red << 16) | (green << 8) | blue;
}

Rasterizer::Result Rasterizer::DrawColor(Canvas& canvas, uint32_t color)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    Rect bounds{0, 0, static_cast<int32_t>(canvas.surface.width), static_cast<int32_t>(canvas.surface.height)};
    Rect area = Intersect(CurrentState(canvas).clip, bounds);
    if (IsEmpty(area)) return Result::Ok;

    FillDeviceRect(canvas.surface, area, color);
    return Result::Ok;
}

Rasterizer::Result Rasterizer::DrawRect(Canvas& canvas, const Rect& rect, uint32_t color)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    Rect area = ToDeviceRect(canvas, rect);
    if (IsEmpty(area)) return Result::Ok;

    FillDeviceRect(canvas.surface, area, color);
    return Result::Ok;
}

Rasterizer::Result Rasterizer::DrawRectOutline(Canvas& canvas, const Rect& rect, int32_t thickness, uint32_t color)
{
    if (thickness < 1) return Result::OutOfRange;

    int32_t limitX = rect.right - rect.left;
    int32_t limitY = rect.bottom - rect.top;
    if (limitX <= 0 || limitY <= 0) return Result::Ok;

    int32_t horizontal = thickness < limitY ? thickness : limitY;
    int32_t vertical = thickness < limitX ? thickness : limitX;

    DrawRect(canvas, Rect{rect.left, rect.top, rect.right, rect.top + horizontal}, color);
    DrawRect(canvas, Rect{rect.left, rect.bottom - horizontal, rect.right, rect.bottom}, color);
    DrawRect(canvas, Rect{rect.left, rect.top, rect.left + vertical, rect.bottom}, color);
    DrawRect(canvas, Rect{rect.right - vertical, rect.top, rect.right, rect.bottom}, color);
    return Result::Ok;
}

Rasterizer::Result Rasterizer::DrawHorizontalLine(Canvas& canvas, int32_t x, int32_t y, int32_t length, uint32_t color)
{
    if (length < 1) return Result::OutOfRange;
    return DrawRect(canvas, Rect{x, y, x + length, y + 1}, color);
}

Rasterizer::Result Rasterizer::DrawVerticalLine(Canvas& canvas, int32_t x, int32_t y, int32_t length, uint32_t color)
{
    if (length < 1) return Result::OutOfRange;
    return DrawRect(canvas, Rect{x, y, x + 1, y + length}, color);
}

Rasterizer::Result Rasterizer::DrawRectBlend(Canvas& canvas, const Rect& rect, uint32_t color)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    Rect area = ToDeviceRect(canvas, rect);
    if (IsEmpty(area)) return Result::Ok;

    for (int32_t y = area.top; y < area.bottom; ++y)
    {
        uint8_t* row = canvas.surface.pixels + static_cast<size_t>(y) * canvas.surface.stride;
        uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + area.left;
        for (int32_t x = area.left; x < area.right; ++x)
        {
            *pixel = BlendPixel(color, *pixel, 255u);
            ++pixel;
        }
    }

    return Result::Ok;
}

Rasterizer::Result Rasterizer::DrawCoverage(Canvas& canvas, const Coverage& mask, int32_t x, int32_t y, uint32_t color)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;
    if (!mask.samples) return Result::OutOfRange;
    if (mask.width <= 0 || mask.height <= 0) return Result::Ok;
    if (mask.pitch < mask.width) return Result::OutOfRange;

    Rect requested{x, y, x + mask.width, y + mask.height};
    Rect area = ToDeviceRect(canvas, requested);
    if (IsEmpty(area)) return Result::Ok;

    const State& state = CurrentState(canvas);
    int32_t originX = x + state.translateX;
    int32_t originY = y + state.translateY;

    for (int32_t deviceY = area.top; deviceY < area.bottom; ++deviceY)
    {
        const uint8_t* sampleRow = mask.samples + static_cast<std::ptrdiff_t>(deviceY - originY) * mask.pitch;
        uint8_t* row = canvas.surface.pixels + static_cast<size_t>(deviceY) * canvas.surface.stride;
        uint32_t* pixel = reinterpret_cast<uint32_t*>(row) + area.left;

        for (int32_t deviceX = area.left; deviceX < area.right; ++deviceX)
        {
            uint32_t coverage = sampleRow[deviceX - originX];
            if (coverage != 0) *pixel = BlendPixel(color, *pixel, coverage);
            ++pixel;
        }
    }

    return Result::Ok;
}

Rasterizer::Result Rasterizer::Blit(Canvas& canvas, const Surface& source, int32_t x, int32_t y)
{
    if (!canvas.surface.pixels) return Result::InvalidSurface;
    if (!source.pixels) return Result::InvalidSurface;
    if (canvas.depth < 1) return Result::StateStackUnderflow;

    Rect target{x, y, x + static_cast<int32_t>(source.width), y + static_cast<int32_t>(source.height)};
    Rect area = ToDeviceRect(canvas, target);
    if (IsEmpty(area)) return Result::Ok;

    const State& state = CurrentState(canvas);
    int32_t originX = x + state.translateX;
    int32_t originY = y + state.translateY;

    for (int32_t row = area.top; row < area.bottom; ++row)
    {
        const uint8_t* sourceRow = source.pixels + static_cast<size_t>(row - originY) * source.stride;
        uint8_t* targetRow = canvas.surface.pixels + static_cast<size_t>(row) * canvas.surface.stride;

        const uint32_t* sourcePixel = reinterpret_cast<const uint32_t*>(sourceRow) + (area.left - originX);
        uint32_t* targetPixel = reinterpret_cast<uint32_t*>(targetRow) + area.left;

        memcpy(targetPixel, sourcePixel, static_cast<size_t>(area.right - area.left) * BYTES_PER_PIXEL);
    }

    return Result::Ok;
}

uint32_t Rasterizer::PixelAt(const Surface& surface, uint32_t x, uint32_t y)
{
    if (!surface.pixels) return 0;
    if (x >= surface.width || y >= surface.height) return 0;

    const uint8_t* row = surface.pixels + static_cast<size_t>(y) * surface.stride;
    return reinterpret_cast<const uint32_t*>(row)[x];
}

const char* Rasterizer::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::InvalidGeometry: return "InvalidGeometry";
        case Result::InvalidSurface: return "InvalidSurface";
        case Result::AllocationFailed: return "AllocationFailed";
        case Result::StateStackOverflow: return "StateStackOverflow";
        case Result::StateStackUnderflow: return "StateStackUnderflow";
        case Result::OutOfRange: return "OutOfRange";
    }
    return "Unknown";
}

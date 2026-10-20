#pragma once

#include <cstdint>

namespace Rasterizer
{
    inline constexpr int BYTES_PER_PIXEL = 4;
    inline constexpr uint32_t MAX_DIMENSION = 16384;
    inline constexpr int MAX_STATE_DEPTH = 32;
    inline constexpr uint32_t OPAQUE_BLACK = 0xFF000000u;

    enum class Result : uint8_t
    {
        Ok,
        InvalidGeometry,
        InvalidSurface,
        AllocationFailed,
        StateStackOverflow,
        StateStackUnderflow,
        OutOfRange
    };

    struct Rect
    {
        int32_t left = 0;
        int32_t top = 0;
        int32_t right = 0;
        int32_t bottom = 0;
    };

    struct Coverage
    {
        const uint8_t* samples = nullptr;
        int32_t width = 0;
        int32_t height = 0;
        int32_t pitch = 0;
    };

    struct Surface
    {
        uint8_t* pixels = nullptr;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        bool ownsPixels = false;
    };

    struct Transform
    {
        float scaleX = 1.0f;
        float skewY = 0.0f;
        float skewX = 0.0f;
        float scaleY = 1.0f;
        float translateX = 0.0f;
        float translateY = 0.0f;
    };

    struct ClipMask
    {
        uint8_t* samples = nullptr;
        int32_t width = 0;
        int32_t height = 0;
        int32_t references = 0;
    };

    struct State
    {
        Rect clip;
        int32_t translateX = 0;
        int32_t translateY = 0;
        Transform transform;
        ClipMask* mask = nullptr;
    };

    struct Layer;

    struct Canvas
    {
        Surface surface;
        State stack[MAX_STATE_DEPTH];
        State* deepStack = nullptr;
        int deepCapacity = 0;
        Layer* layers = nullptr;
        int layerCount = 0;
        int layerCapacity = 0;
        int depth = 0;
    };

    enum class FillRule : uint8_t
    {
        NonZero,
        EvenOdd
    };

    enum class TileMode : uint8_t
    {
        Clamp,
        Repeat,
        Mirror
    };

    enum class SourceKind : uint8_t
    {
        Solid,
        LinearGradient,
        RadialGradient,
        SweepGradient,
        Image
    };

    enum class BlendMode : uint8_t
    {
        SourceOver,
        Source,
        SourceIn,
        SourceAtop,
        DestinationOver,
        DestinationOut,
        Multiply,
        Screen,
        Plus,
        Clear
    };

    enum class LineCap : uint8_t
    {
        Butt,
        Round,
        Square
    };

    enum class LineJoin : uint8_t
    {
        Miter,
        Round,
        Bevel
    };

    struct GradientStop
    {
        float position = 0.0f;
        uint32_t color = 0xFF000000u;
    };

    struct Source
    {
        SourceKind kind = SourceKind::Solid;
        uint32_t color = 0xFF000000u;
        float startX = 0.0f;
        float startY = 0.0f;
        float endX = 0.0f;
        float endY = 0.0f;
        float radius = 0.0f;
        const GradientStop* stops = nullptr;
        int32_t stopCount = 0;
        TileMode tileMode = TileMode::Clamp;
        const Surface* image = nullptr;
        Transform inverse;
        bool filter = true;
        bool tinted = false;
        uint32_t tint = 0xFF000000u;
    };

    struct StrokeStyle
    {
        float width = 1.0f;
        LineCap cap = LineCap::Butt;
        LineJoin join = LineJoin::Miter;
        float miterLimit = 4.0f;
    };

    struct Path;

    Path* CreatePath();
    void DestroyPath(Path* path);
    void ResetPath(Path& path);
    bool PathIsEmpty(const Path& path);

    Result MoveTo(Path& path, float x, float y);
    Result LineTo(Path& path, float x, float y);
    Result QuadraticTo(Path& path, float controlX, float controlY, float x, float y);
    Result CubicTo(Path& path, float firstX, float firstY, float secondX, float secondY, float x, float y);
    Result CloseContour(Path& path);
    Result AddRectangle(Path& path, float left, float top, float right, float bottom);
    Result AddRoundedRectangle(Path& path, float left, float top, float right, float bottom, float radiusX, float radiusY);
    Result AddOval(Path& path, float left, float top, float right, float bottom);
    Result AddArc(Path& path, float left, float top, float right, float bottom, float startDegrees, float sweepDegrees,
                  bool moveToStart);
    Result AddPath(Path& path, const Path& other, const Transform& transform);
    Result TransformPath(Path& path, const Transform& transform);
    bool PathBounds(const Path& path, float& left, float& top, float& right, float& bottom);
    void CurrentPoint(const Path& path, float& x, float& y);

    Result FillPath(Canvas& canvas, const Path& path, const Source& source, FillRule rule, BlendMode blend, uint8_t alpha);
    Result StrokePath(Canvas& canvas, const Path& path, const Source& source, const StrokeStyle& style, BlendMode blend,
                      uint8_t alpha);

    Result FillCoverage(Canvas& canvas, const Coverage& mask, const Source& source, BlendMode blend, uint8_t alpha);
    Result FillPathBlurred(Canvas& canvas, const Path& path, const Source& source, FillRule rule, BlendMode blend,
                           uint8_t alpha, float radius);
    Result DashPath(const Path& source, Path& into, const float* intervals, int intervalCount, float phase);

    Result ClipPath(Canvas& canvas, const Path& path, FillRule rule, bool subtract);

    Result Concat(Canvas& canvas, const Transform& transform);
    Result SetTransform(Canvas& canvas, const Transform& transform);
    const Transform& CurrentTransform(const Canvas& canvas);

    Result SaveLayer(Canvas& canvas, const Rect& bounds, uint8_t alpha, BlendMode blend);
    Result RestoreLayer(Canvas& canvas);

    Result BlurMask(uint8_t* samples, int32_t width, int32_t height, int32_t pitch, float radius);

    Transform Multiply(const Transform& outer, const Transform& inner);
    bool Invert(const Transform& transform, Transform& inverse);

    Result CreateSurface(uint32_t width, uint32_t height, Surface& surface);
    Result WrapSurface(uint8_t* pixels, uint32_t width, uint32_t height, uint32_t stride, Surface& surface);
    void DestroySurface(Surface& surface);

    Result Begin(const Surface& surface, Canvas& canvas);
    void End(Canvas& canvas);
    Result Save(Canvas& canvas);
    Result Restore(Canvas& canvas);
    State& StateAt(Canvas& canvas, int index);
    const State& StateAt(const Canvas& canvas, int index);
    State& TopState(Canvas& canvas);
    Result Translate(Canvas& canvas, int32_t deltaX, int32_t deltaY);
    Result ClipRect(Canvas& canvas, const Rect& rect);
    const State& CurrentState(const Canvas& canvas);

    Result DrawColor(Canvas& canvas, uint32_t color);
    Result DrawRect(Canvas& canvas, const Rect& rect, uint32_t color);
    Result DrawRectOutline(Canvas& canvas, const Rect& rect, int32_t thickness, uint32_t color);
    Result DrawHorizontalLine(Canvas& canvas, int32_t x, int32_t y, int32_t length, uint32_t color);
    Result DrawVerticalLine(Canvas& canvas, int32_t x, int32_t y, int32_t length, uint32_t color);
    Result DrawCoverage(Canvas& canvas, const Coverage& mask, int32_t x, int32_t y, uint32_t color);
    Result DrawRectBlend(Canvas& canvas, const Rect& rect, uint32_t color);

    Result Blit(Canvas& canvas, const Surface& source, int32_t x, int32_t y);

    uint32_t PixelAt(const Surface& surface, uint32_t x, uint32_t y);
    bool IsEmpty(const Rect& rect);
    Rect Intersect(const Rect& first, const Rect& second);

    const char* ResultName(Result result);
}

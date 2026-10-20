#include "Rasterizer/Rasterizer.hpp"

#include <cmath>
#include <cstring>
#include <vector>

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_OUTLINE_H
#include FT_STROKER_H

static constexpr float OVAL_CONTROL_RATIO = 0.5522847498f;
static constexpr float PI_VALUE = 3.14159265358979323846f;
static constexpr float PI_OVER_180 = PI_VALUE / 180.0f;
static constexpr int32_t FIXED_ONE = 64;
static constexpr int FLATTEN_STEPS = 16;

struct Rasterizer::Path
{
    std::vector<FT_Vector> points;
    std::vector<char> tags;
    std::vector<short> contours;
    std::vector<char> closed;
    int32_t contourStart = 0;
    bool open = false;
    float currentX = 0.0f;
    float currentY = 0.0f;
    float startX = 0.0f;
    float startY = 0.0f;
};

static FT_Library gLibrary = nullptr;

static FT_Library OutlineLibrary()
{
    if (!gLibrary && FT_Init_FreeType(&gLibrary) != 0) gLibrary = nullptr;

    return gLibrary;
}

static FT_Pos ToFixed(float value)
{
    return static_cast<FT_Pos>(std::lround(value * FIXED_ONE));
}

static void PushPoint(Rasterizer::Path& path, float x, float y, char tag)
{
    FT_Vector point;
    point.x = ToFixed(x);
    point.y = ToFixed(y);

    path.points.push_back(point);
    path.tags.push_back(tag);
}

static void FinishContour(Rasterizer::Path& path, bool closed)
{
    if (!path.open) return;

    path.contours.push_back(static_cast<short>(path.points.size() - 1));
    path.closed.push_back(closed ? 1 : 0);
    path.contourStart = static_cast<int32_t>(path.points.size());
    path.open = false;
}

Rasterizer::Path* Rasterizer::CreatePath()
{
    return new Path();
}

void Rasterizer::DestroyPath(Path* path)
{
    delete path;
}

void Rasterizer::ResetPath(Path& path)
{
    path.points.clear();
    path.tags.clear();
    path.contours.clear();
    path.closed.clear();
    path.contourStart = 0;
    path.open = false;
    path.currentX = 0.0f;
    path.currentY = 0.0f;
}

bool Rasterizer::PathIsEmpty(const Path& path)
{
    return path.points.empty();
}

Rasterizer::Result Rasterizer::MoveTo(Path& path, float x, float y)
{
    FinishContour(path, false);

    PushPoint(path, x, y, FT_CURVE_TAG_ON);
    path.open = true;
    path.currentX = x;
    path.currentY = y;
    path.startX = x;
    path.startY = y;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::LineTo(Path& path, float x, float y)
{
    if (!path.open) return MoveTo(path, x, y);

    PushPoint(path, x, y, FT_CURVE_TAG_ON);
    path.currentX = x;
    path.currentY = y;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::QuadraticTo(Path& path, float controlX, float controlY, float x, float y)
{
    if (!path.open) MoveTo(path, path.currentX, path.currentY);

    PushPoint(path, controlX, controlY, FT_CURVE_TAG_CONIC);
    PushPoint(path, x, y, FT_CURVE_TAG_ON);

    path.currentX = x;
    path.currentY = y;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::CubicTo(Path& path, float firstX, float firstY, float secondX, float secondY, float x, float y)
{
    if (!path.open) MoveTo(path, path.currentX, path.currentY);

    PushPoint(path, firstX, firstY, FT_CURVE_TAG_CUBIC);
    PushPoint(path, secondX, secondY, FT_CURVE_TAG_CUBIC);
    PushPoint(path, x, y, FT_CURVE_TAG_ON);

    path.currentX = x;
    path.currentY = y;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::CloseContour(Path& path)
{
    if (!path.open) return Result::Ok;

    FinishContour(path, true);
    path.currentX = path.startX;
    path.currentY = path.startY;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::AddRectangle(Path& path, float left, float top, float right, float bottom)
{
    MoveTo(path, left, top);
    LineTo(path, right, top);
    LineTo(path, right, bottom);
    LineTo(path, left, bottom);

    return CloseContour(path);
}

Rasterizer::Result Rasterizer::AddRoundedRectangle(Path& path, float left, float top, float right, float bottom, float radiusX,
                                                   float radiusY)
{
    float halfWidth = (right - left) * 0.5f;
    float halfHeight = (bottom - top) * 0.5f;

    if (radiusX > halfWidth) radiusX = halfWidth;
    if (radiusY > halfHeight) radiusY = halfHeight;

    if (radiusX <= 0.0f || radiusY <= 0.0f) return AddRectangle(path, left, top, right, bottom);

    float offsetX = radiusX * OVAL_CONTROL_RATIO;
    float offsetY = radiusY * OVAL_CONTROL_RATIO;

    MoveTo(path, left + radiusX, top);
    LineTo(path, right - radiusX, top);
    CubicTo(path, right - radiusX + offsetX, top, right, top + radiusY - offsetY, right, top + radiusY);
    LineTo(path, right, bottom - radiusY);
    CubicTo(path, right, bottom - radiusY + offsetY, right - radiusX + offsetX, bottom, right - radiusX, bottom);
    LineTo(path, left + radiusX, bottom);
    CubicTo(path, left + radiusX - offsetX, bottom, left, bottom - radiusY + offsetY, left, bottom - radiusY);
    LineTo(path, left, top + radiusY);
    CubicTo(path, left, top + radiusY - offsetY, left + radiusX - offsetX, top, left + radiusX, top);

    return CloseContour(path);
}

Rasterizer::Result Rasterizer::AddOval(Path& path, float left, float top, float right, float bottom)
{
    float centreX = (left + right) * 0.5f;
    float centreY = (top + bottom) * 0.5f;
    float radiusX = (right - left) * 0.5f;
    float radiusY = (bottom - top) * 0.5f;

    if (radiusX <= 0.0f || radiusY <= 0.0f) return Result::Ok;

    float offsetX = radiusX * OVAL_CONTROL_RATIO;
    float offsetY = radiusY * OVAL_CONTROL_RATIO;

    MoveTo(path, centreX, top);
    CubicTo(path, centreX + offsetX, top, right, centreY - offsetY, right, centreY);
    CubicTo(path, right, centreY + offsetY, centreX + offsetX, bottom, centreX, bottom);
    CubicTo(path, centreX - offsetX, bottom, left, centreY + offsetY, left, centreY);
    CubicTo(path, left, centreY - offsetY, centreX - offsetX, top, centreX, top);

    return CloseContour(path);
}

Rasterizer::Result Rasterizer::AddArc(Path& path, float left, float top, float right, float bottom, float startDegrees,
                                      float sweepDegrees, bool moveToStart)
{
    float centreX = (left + right) * 0.5f;
    float centreY = (top + bottom) * 0.5f;
    float radiusX = (right - left) * 0.5f;
    float radiusY = (bottom - top) * 0.5f;

    if (radiusX <= 0.0f || radiusY <= 0.0f) return Result::Ok;

    if (sweepDegrees > 360.0f) sweepDegrees = 360.0f;
    if (sweepDegrees < -360.0f) sweepDegrees = -360.0f;

    float start = startDegrees * PI_OVER_180;
    float sweep = sweepDegrees * PI_OVER_180;

    int segments = static_cast<int>(std::ceil(std::fabs(sweep) / (PI_VALUE * 0.5f)));
    if (segments < 1) segments = 1;

    float step = sweep / static_cast<float>(segments);
    float control = 4.0f / 3.0f * std::tan(step * 0.25f);

    float angle = start;
    float pointX = centreX + radiusX * std::cos(angle);
    float pointY = centreY + radiusY * std::sin(angle);

    if (moveToStart || !path.open) MoveTo(path, pointX, pointY);
    else LineTo(path, pointX, pointY);

    for (int segment = 0; segment < segments; ++segment)
    {
        float next = angle + step;

        float sineStart = std::sin(angle);
        float cosineStart = std::cos(angle);
        float sineEnd = std::sin(next);
        float cosineEnd = std::cos(next);

        float endX = centreX + radiusX * cosineEnd;
        float endY = centreY + radiusY * sineEnd;

        CubicTo(path, pointX - control * radiusX * sineStart, pointY + control * radiusY * cosineStart,
                endX + control * radiusX * sineEnd, endY - control * radiusY * cosineEnd, endX, endY);

        angle = next;
        pointX = endX;
        pointY = endY;
    }

    return Result::Ok;
}

Rasterizer::Result Rasterizer::AddPath(Path& path, const Path& other, const Transform& transform)
{
    FinishContour(path, false);

    size_t base = path.points.size();
    if (base + other.points.size() > 32767) return Result::OutOfRange;

    for (size_t index = 0; index < other.points.size(); ++index)
    {
        float x = static_cast<float>(other.points[index].x) / FIXED_ONE;
        float y = static_cast<float>(other.points[index].y) / FIXED_ONE;

        float mappedX = transform.scaleX * x + transform.skewX * y + transform.translateX;
        float mappedY = transform.skewY * x + transform.scaleY * y + transform.translateY;

        FT_Vector point;
        point.x = ToFixed(mappedX);
        point.y = ToFixed(mappedY);

        path.points.push_back(point);
        path.tags.push_back(other.tags[index]);
    }

    for (short contour : other.contours) path.contours.push_back(static_cast<short>(contour + base));
    for (char closed : other.closed) path.closed.push_back(closed);

    if (other.open && !other.points.empty())
    {
        path.contours.push_back(static_cast<short>(path.points.size() - 1));
        path.closed.push_back(0);
    }

    path.contourStart = static_cast<int32_t>(path.points.size());
    path.open = false;

    return Result::Ok;
}

Rasterizer::Result Rasterizer::TransformPath(Path& path, const Transform& transform)
{
    for (FT_Vector& point : path.points)
    {
        float x = static_cast<float>(point.x) / FIXED_ONE;
        float y = static_cast<float>(point.y) / FIXED_ONE;

        point.x = ToFixed(transform.scaleX * x + transform.skewX * y + transform.translateX);
        point.y = ToFixed(transform.skewY * x + transform.scaleY * y + transform.translateY);
    }

    return Result::Ok;
}

static void EmitPoint(std::vector<float>& points, float x, float y)
{
    points.push_back(x);
    points.push_back(y);
}

static void EmitQuadratic(std::vector<float>& points, float fromX, float fromY, float controlX, float controlY, float toX,
                          float toY)
{
    for (int step = 1; step <= FLATTEN_STEPS; ++step)
    {
        float amount = static_cast<float>(step) / FLATTEN_STEPS;
        float inverse = 1.0f - amount;

        float x = inverse * inverse * fromX + 2.0f * inverse * amount * controlX + amount * amount * toX;
        float y = inverse * inverse * fromY + 2.0f * inverse * amount * controlY + amount * amount * toY;

        EmitPoint(points, x, y);
    }
}

static void EmitCubic(std::vector<float>& points, float fromX, float fromY, float firstX, float firstY, float secondX,
                      float secondY, float toX, float toY)
{
    for (int step = 1; step <= FLATTEN_STEPS; ++step)
    {
        float amount = static_cast<float>(step) / FLATTEN_STEPS;
        float inverse = 1.0f - amount;

        float x = inverse * inverse * inverse * fromX + 3.0f * inverse * inverse * amount * firstX
                  + 3.0f * inverse * amount * amount * secondX + amount * amount * amount * toX;
        float y = inverse * inverse * inverse * fromY + 3.0f * inverse * inverse * amount * firstY
                  + 3.0f * inverse * amount * amount * secondY + amount * amount * amount * toY;

        EmitPoint(points, x, y);
    }
}

void RasterizerFlattenPath(const Rasterizer::Path& path, std::vector<float>& points, std::vector<int32_t>& starts)
{
    points.clear();
    starts.clear();

    std::vector<int32_t> ends(path.contours.begin(), path.contours.end());
    std::vector<char> closedFlags(path.closed.begin(), path.closed.end());

    if (path.open && !path.points.empty())
    {
        ends.push_back(static_cast<int32_t>(path.points.size()) - 1);
        closedFlags.push_back(0);
    }

    int32_t first = 0;

    for (size_t contour = 0; contour < ends.size(); ++contour)
    {
        int32_t last = ends[contour];
        if (last < first)
        {
            first = last + 1;
            continue;
        }

        starts.push_back(static_cast<int32_t>(points.size() / 2));

        float currentX = static_cast<float>(path.points[first].x) / FIXED_ONE;
        float currentY = static_cast<float>(path.points[first].y) / FIXED_ONE;
        EmitPoint(points, currentX, currentY);

        int32_t index = first + 1;
        while (index <= last)
        {
            char tag = static_cast<char>(path.tags[index] & 3);

            float x = static_cast<float>(path.points[index].x) / FIXED_ONE;
            float y = static_cast<float>(path.points[index].y) / FIXED_ONE;

            if (tag == FT_CURVE_TAG_ON)
            {
                EmitPoint(points, x, y);
                currentX = x;
                currentY = y;
                index += 1;
                continue;
            }

            if (tag == FT_CURVE_TAG_CONIC && index + 1 <= last)
            {
                float endX = static_cast<float>(path.points[index + 1].x) / FIXED_ONE;
                float endY = static_cast<float>(path.points[index + 1].y) / FIXED_ONE;

                EmitQuadratic(points, currentX, currentY, x, y, endX, endY);
                currentX = endX;
                currentY = endY;
                index += 2;
                continue;
            }

            if (index + 2 <= last)
            {
                float secondX = static_cast<float>(path.points[index + 1].x) / FIXED_ONE;
                float secondY = static_cast<float>(path.points[index + 1].y) / FIXED_ONE;
                float endX = static_cast<float>(path.points[index + 2].x) / FIXED_ONE;
                float endY = static_cast<float>(path.points[index + 2].y) / FIXED_ONE;

                EmitCubic(points, currentX, currentY, x, y, secondX, secondY, endX, endY);
                currentX = endX;
                currentY = endY;
                index += 3;
                continue;
            }

            break;
        }

        bool closed = contour < closedFlags.size() && closedFlags[contour] != 0;
        if (closed)
        {
            EmitPoint(points, static_cast<float>(path.points[first].x) / FIXED_ONE,
                      static_cast<float>(path.points[first].y) / FIXED_ONE);
        }

        first = last + 1;
    }

    starts.push_back(static_cast<int32_t>(points.size() / 2));
}

static FT_Vector MapPoint(const FT_Vector& point, const Rasterizer::Transform& transform)
{
    float x = static_cast<float>(point.x) / FIXED_ONE;
    float y = static_cast<float>(point.y) / FIXED_ONE;

    FT_Vector mapped;
    mapped.x = ToFixed(transform.scaleX * x + transform.skewX * y + transform.translateX);
    mapped.y = ToFixed(transform.skewY * x + transform.scaleY * y + transform.translateY);

    return mapped;
}

bool RasterizerFeedStroker(const Rasterizer::Path& path, const Rasterizer::Transform& transform, FT_Stroker stroker)
{
    std::vector<int32_t> ends(path.contours.begin(), path.contours.end());
    std::vector<char> closedFlags(path.closed.begin(), path.closed.end());

    if (path.open && !path.points.empty())
    {
        ends.push_back(static_cast<int32_t>(path.points.size()) - 1);
        closedFlags.push_back(0);
    }

    if (ends.empty()) return false;

    bool fed = false;
    int32_t first = 0;

    for (size_t contour = 0; contour < ends.size(); ++contour)
    {
        int32_t last = ends[contour];
        if (last <= first)
        {
            first = last + 1;
            continue;
        }

        bool closed = contour < closedFlags.size() && closedFlags[contour] != 0;

        FT_Vector start = MapPoint(path.points[first], transform);
        if (FT_Stroker_BeginSubPath(stroker, &start, closed ? 0 : 1) != 0)
        {
            first = last + 1;
            continue;
        }

        int32_t index = first + 1;
        while (index <= last)
        {
            char tag = static_cast<char>(path.tags[index] & 3);
            FT_Vector point = MapPoint(path.points[index], transform);

            if (tag == FT_CURVE_TAG_ON)
            {
                FT_Stroker_LineTo(stroker, &point);
                index += 1;
                continue;
            }

            if (tag == FT_CURVE_TAG_CONIC && index + 1 <= last)
            {
                FT_Vector end = MapPoint(path.points[index + 1], transform);
                FT_Stroker_ConicTo(stroker, &point, &end);
                index += 2;
                continue;
            }

            if (index + 2 <= last)
            {
                FT_Vector second = MapPoint(path.points[index + 1], transform);
                FT_Vector end = MapPoint(path.points[index + 2], transform);
                FT_Stroker_CubicTo(stroker, &point, &second, &end);
                index += 3;
                continue;
            }

            break;
        }

        FT_Stroker_EndSubPath(stroker);
        fed = true;
        first = last + 1;
    }

    return fed;
}

void Rasterizer::CurrentPoint(const Path& path, float& x, float& y)
{
    x = path.currentX;
    y = path.currentY;
}

bool Rasterizer::PathBounds(const Path& path, float& left, float& top, float& right, float& bottom)
{
    if (path.points.empty()) return false;

    left = right = static_cast<float>(path.points[0].x) / FIXED_ONE;
    top = bottom = static_cast<float>(path.points[0].y) / FIXED_ONE;

    for (const FT_Vector& point : path.points)
    {
        float x = static_cast<float>(point.x) / FIXED_ONE;
        float y = static_cast<float>(point.y) / FIXED_ONE;

        if (x < left) left = x;
        if (x > right) right = x;
        if (y < top) top = y;
        if (y > bottom) bottom = y;
    }

    return true;
}

bool RasterizerBuildOutline(const Rasterizer::Path& path, const Rasterizer::Transform& transform, FT_Outline& outline,
                            std::vector<FT_Vector>& points, std::vector<char>& tags, std::vector<short>& contours)
{
    points.clear();
    tags.clear();
    contours.clear();

    points.reserve(path.points.size());
    tags.reserve(path.tags.size());
    contours.reserve(path.contours.size() + 1);

    for (size_t index = 0; index < path.points.size(); ++index)
    {
        float x = static_cast<float>(path.points[index].x) / FIXED_ONE;
        float y = static_cast<float>(path.points[index].y) / FIXED_ONE;

        float mappedX = transform.scaleX * x + transform.skewX * y + transform.translateX;
        float mappedY = transform.skewY * x + transform.scaleY * y + transform.translateY;

        FT_Vector point;
        point.x = ToFixed(mappedX);
        point.y = ToFixed(mappedY);

        points.push_back(point);
        tags.push_back(path.tags[index]);
    }

    contours = path.contours;

    if (path.open && !points.empty()) contours.push_back(static_cast<short>(points.size() - 1));

    if (points.empty() || contours.empty()) return false;

    outline.n_points = static_cast<short>(points.size());
    outline.n_contours = static_cast<short>(contours.size());
    outline.points = points.data();
    outline.tags = reinterpret_cast<unsigned char*>(tags.data());
    outline.contours = reinterpret_cast<unsigned short*>(contours.data());
    outline.flags = FT_OUTLINE_NONE;

    return true;
}

FT_Library RasterizerOutlineLibrary()
{
    return OutlineLibrary();
}

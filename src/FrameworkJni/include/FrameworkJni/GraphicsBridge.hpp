#pragma once

#include <jni.h>

#include "ArtRuntime/ArtRuntime.hpp"

namespace GraphicsBridge
{
    inline constexpr const char* PATH_CLASS = "android/graphics/Path";
    inline constexpr const char* CANVAS_CLASS = "android/graphics/Canvas";
    inline constexpr const char* BITMAP_CLASS = "android/graphics/Bitmap";

    inline constexpr int PAINT_COLOR = 0;
    inline constexpr int PAINT_STYLE = 1;
    inline constexpr int PAINT_CAP = 2;
    inline constexpr int PAINT_JOIN = 3;
    inline constexpr int PAINT_BLEND = 4;
    inline constexpr int PAINT_ALPHA = 5;
    inline constexpr int PAINT_SHADER_KIND = 6;
    inline constexpr int PAINT_TILE_MODE = 7;
    inline constexpr int PAINT_FILTER = 8;
    inline constexpr int PAINT_TINT = 9;
    inline constexpr int PAINT_TINTED = 10;
    inline constexpr int PAINT_INT_COUNT = 11;

    inline constexpr int PAINT_STROKE_WIDTH = 0;
    inline constexpr int PAINT_MITER = 1;
    inline constexpr int PAINT_START_X = 2;
    inline constexpr int PAINT_START_Y = 3;
    inline constexpr int PAINT_END_X = 4;
    inline constexpr int PAINT_END_Y = 5;
    inline constexpr int PAINT_RADIUS = 6;
    inline constexpr int PAINT_LOCAL = 7;
    inline constexpr int PAINT_FLOAT_COUNT = 13;

    inline constexpr int STYLE_FILL = 0;
    inline constexpr int STYLE_STROKE = 1;
    inline constexpr int STYLE_FILL_AND_STROKE = 2;

    inline constexpr int SHADER_NONE = 0;
    inline constexpr int SHADER_LINEAR = 1;
    inline constexpr int SHADER_RADIAL = 2;
    inline constexpr int SHADER_SWEEP = 3;
    inline constexpr int SHADER_BITMAP = 4;

    const ArtRuntime::NativeMethodTable* Tables(int& count);
}

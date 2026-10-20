#pragma once

#include <jni.h>

#include "ArtRuntime/ArtRuntime.hpp"

namespace TextBridge
{
    inline constexpr const char* PAINT_CLASS = "android/graphics/Paint";
    inline constexpr const char* TYPEFACE_CLASS = "android/graphics/Typeface";

    inline constexpr int METRICS_FIELD_COUNT = 5;
    inline constexpr int BOUNDS_FIELD_COUNT = 4;
    inline constexpr int BREAK_RESULT_COUNT = 2;

    const ArtRuntime::NativeMethodTable* Tables(int& count);
}

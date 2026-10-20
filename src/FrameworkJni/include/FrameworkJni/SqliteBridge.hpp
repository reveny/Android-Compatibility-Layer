#pragma once

#include <jni.h>

#include "ArtRuntime/ArtRuntime.hpp"

namespace SqliteBridge
{
    inline constexpr const char* SQLITE_CLASS = "runtime/Sqlite";

    inline constexpr int STEP_ROW = 1;
    inline constexpr int STEP_DONE = 2;
    inline constexpr int STEP_ERROR = 3;

    const ArtRuntime::NativeMethodTable* Tables(int& count);
}

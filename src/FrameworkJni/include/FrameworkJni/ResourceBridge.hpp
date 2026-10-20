#pragma once

#include <jni.h>

#include "ArtRuntime/ArtRuntime.hpp"

namespace ResourceBridge
{
    inline constexpr const char* RESOURCES_CLASS = "android/content/res/Resources";
    inline constexpr const char* XML_PARSER_CLASS = "android/content/res/XmlBlock$Parser";
    inline constexpr const char* DESCRIPTOR_CLASS = "android/os/ParcelFileDescriptor";
    inline constexpr int MAX_RESOURCE_TEXT = 1 << 16;

    const ArtRuntime::NativeMethodTable* Tables(int& count);
}

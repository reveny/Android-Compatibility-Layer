#pragma once

#include <cstddef>
#include <cstdint>

namespace ImageCodec
{
    enum class Result : uint8_t
    {
        Ok,
        UnknownFormat,
        Corrupt,
        TooLarge,
        AllocationFailed
    };

    enum class Format : uint8_t
    {
        Unknown,
        Png,
        Jpeg,
        Webp
    };

    inline constexpr uint32_t MAX_DIMENSION = 16384;

    struct Image
    {
        uint8_t* pixels = nullptr;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        bool hasAlpha = false;
    };

    Format Detect(const uint8_t* bytes, size_t length);
    const char* FormatName(Format format);
    const char* ResultName(Result result);

    Result Decode(const uint8_t* bytes, size_t length, Image& image);
    Result MeasureOnly(const uint8_t* bytes, size_t length, uint32_t& width, uint32_t& height);
    void Release(Image& image);
}

#pragma once

#include <cstddef>
#include <cstdint>
#include <mutex>

namespace FontStack
{
    inline constexpr const char* DIRECTORY_SETTING = "font-dir";

    inline constexpr uint32_t INVALID_FONT = 0;
    inline constexpr uint64_t INVALID_RUN = 0;
    inline constexpr uint32_t INVALID_STYLE = 0;

    inline constexpr int WEIGHT_THIN = 100;
    inline constexpr int WEIGHT_LIGHT = 300;
    inline constexpr int WEIGHT_NORMAL = 400;
    inline constexpr int WEIGHT_MEDIUM = 500;
    inline constexpr int WEIGHT_BOLD = 700;
    inline constexpr int WEIGHT_BLACK = 900;

    inline constexpr float ADVANCE_EPSILON = 1.0e-4f;
    inline constexpr float MIN_TEXT_SIZE = 1.0f;
    inline constexpr float MAX_TEXT_SIZE = 2048.0f;
    inline constexpr int MAX_STYLE_COUNT = 4096;
    inline constexpr int MAX_FONT_COUNT = 256;
    inline constexpr size_t MAX_FONT_FILE_BYTES = 32u * 1024u * 1024u;
    inline constexpr int MAX_CACHED_GLYPH_DIMENSION = 128;
    inline constexpr size_t MAX_GLYPH_CACHE_BYTES = 4u * 1024u * 1024u;
    inline constexpr int MAX_SHAPE_CACHE_ENTRIES = 512;
    inline constexpr int MAX_RUN_LENGTH = 8192;

    inline constexpr int FLAG_FAKE_BOLD = 1 << 0;
    inline constexpr int FLAG_SYNTHETIC_ITALIC = 1 << 1;

    enum class Result : uint8_t
    {
        Ok,
        NotInitialized,
        AlreadyInitialized,
        FontDirectoryMissing,
        FontFileUnreadable,
        FontFileMalformed,
        FontFileTooLarge,
        UnknownFamily,
        InvalidFont,
        InvalidStyle,
        InvalidRun,
        InvalidArgument,
        TextTooLong,
        StyleTableFull,
        FontTableFull,
        ShapingFailed,
        RasterizationFailed
    };

    struct Metrics
    {
        float top = 0.0f;
        float ascent = 0.0f;
        float descent = 0.0f;
        float bottom = 0.0f;
        float leading = 0.0f;
    };

    struct Bounds
    {
        int32_t left = 0;
        int32_t top = 0;
        int32_t right = 0;
        int32_t bottom = 0;
    };

    struct GlyphImage
    {
        const uint8_t* samples = nullptr;
        int32_t width = 0;
        int32_t height = 0;
        int32_t pitch = 0;
        int32_t bearingX = 0;
        int32_t bearingY = 0;
    };

    struct GlyphPosition
    {
        uint32_t glyphId = 0;
        float x = 0.0f;
        float y = 0.0f;
        int32_t cluster = 0;
    };

    Result Initialize(const char* fontDirectory);
    void Shutdown();
    bool IsInitialized();
    std::recursive_mutex& Mutex();

    Result ResolveFamily(const char* familyName, int weight, bool italic, uint32_t& font);
    Result LoadFontFromMemory(const uint8_t* bytes, size_t byteCount, int weight, bool italic, uint32_t& font);
    Result FontWeight(uint32_t font, int& weight);
    Result FontIsItalic(uint32_t font, bool& italic);

    Result InternStyle(uint32_t font, float textSize, float scaleX, float skewX, float letterSpacing, int flags, uint32_t& style);

    Result MeasureText(uint32_t style, const uint16_t* text, int32_t count, float& advance);
    Result TextWidths(uint32_t style, const uint16_t* text, int32_t count, float* outWidths, int32_t& written);
    Result FontMetrics(uint32_t style, Metrics& metrics);
    Result TextBounds(uint32_t style, const uint16_t* text, int32_t count, Bounds& bounds);
    Result BreakText(uint32_t style, const uint16_t* text, int32_t count, float maxWidth, bool forwards, int32_t& breakAt,
                     float& measuredWidth);
    Result HasGlyph(uint32_t style, const uint16_t* text, int32_t count, bool& present);

    Result ShapeRun(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t& run);
    Result RunAdvance(uint64_t run, float& advance);
    Result RunGlyphCount(uint64_t run, int32_t& count);
    Result RunGlyph(uint64_t run, int32_t index, GlyphPosition& position);
    Result ReleaseRun(uint64_t run);

    Result RasterizeGlyph(uint32_t style, uint32_t glyphId, GlyphImage& image);

    const char* ResultName(Result result);
}

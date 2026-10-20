#include <cmath>
#include <cstdio>
#include <cstring>
#include <vector>

#include "FontStack/FontRegistry.hpp"
#include "FontStack/FontStack.hpp"
#include "FontStack/GlyphCache.hpp"

static int gFailures = 0;

static void Check(bool condition, const char* what)
{
    printf("%s %s\n", condition ? "PASS" : "FAIL", what);
    if (!condition) gFailures += 1;
}

static void ToUtf16(const char* ascii, std::vector<uint16_t>& out)
{
    out.clear();
    for (const char* cursor = ascii; *cursor; ++cursor)
    {
        out.push_back(static_cast<uint16_t>(static_cast<unsigned char>(*cursor)));
    }
}

static float MeasureAscii(uint32_t style, const char* ascii)
{
    std::vector<uint16_t> text;
    ToUtf16(ascii, text);

    float advance = 0.0f;
    FontStack::Result result = FontStack::MeasureText(style, text.data(), static_cast<int32_t>(text.size()), advance);
    if (result != FontStack::Result::Ok) return -1.0f;
    return advance;
}

static uint32_t StyleFor(const char* family, int weight, bool italic, float textSize)
{
    uint32_t font = FontStack::INVALID_FONT;
    if (FontStack::ResolveFamily(family, weight, italic, font) != FontStack::Result::Ok) return FontStack::INVALID_STYLE;

    uint32_t style = FontStack::INVALID_STYLE;
    if (FontStack::InternStyle(font, textSize, 1.0f, 0.0f, 0.0f, 0, style) != FontStack::Result::Ok) return FontStack::INVALID_STYLE;
    return style;
}

static void TestFamilyResolution()
{
    FontRegistry::Selection selection;

    Check(FontRegistry::Resolve("sans-serif", FontStack::WEIGHT_NORMAL, false, selection), "sans-serif resolves");
    Check(selection.weightAxis == 400.0f, "sans-serif is weight 400");

    Check(FontRegistry::Resolve("sans-serif-light", FontStack::WEIGHT_NORMAL, false, selection), "sans-serif-light resolves");
    Check(selection.weightAxis == 300.0f, "sans-serif-light pins weight 300 regardless of requested weight");

    Check(FontRegistry::Resolve("sans-serif-medium", FontStack::WEIGHT_NORMAL, false, selection), "sans-serif-medium resolves");
    Check(selection.weightAxis == 500.0f, "sans-serif-medium pins weight 500");

    FontRegistry::Resolve("sans-serif-condensed", FontStack::WEIGHT_NORMAL, false, selection);
    Check(selection.widthAxis == FontRegistry::AXIS_WIDTH_CONDENSED, "sans-serif-condensed narrows the width axis");

    FontRegistry::Resolve("no-such-family-at-all", FontStack::WEIGHT_NORMAL, false, selection);
    Check(selection.substitution == FontRegistry::Substitution::DefaultFamily, "an unknown family falls back and says so");

    FontRegistry::Resolve("serif", FontStack::WEIGHT_NORMAL, false, selection);
    Check(selection.substitution == FontRegistry::Substitution::FamilySubstituted, "serif reports itself as substituted");
}

static void TestWeightAffectsAdvance()
{
    uint32_t regular = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 48.0f);
    uint32_t medium = StyleFor("sans-serif-medium", FontStack::WEIGHT_NORMAL, false, 48.0f);
    uint32_t light = StyleFor("sans-serif-light", FontStack::WEIGHT_NORMAL, false, 48.0f);

    Check(regular != FontStack::INVALID_STYLE && medium != FontStack::INVALID_STYLE, "regular and medium styles intern");

    float regularWidth = MeasureAscii(regular, "Hello World!");
    float mediumWidth = MeasureAscii(medium, "Hello World!");
    float lightWidth = MeasureAscii(light, "Hello World!");

    Check(regularWidth == 259.0f, "regular at 48px measures 259, the value the device reports");
    Check(lightWidth == 255.0f, "light at 48px measures 255, the value the device reports");
    Check(mediumWidth == 261.0f, "medium at 48px measures 261, the value the device reports");
    Check(mediumWidth > regularWidth && regularWidth > lightWidth, "the variable weight axis orders the three faces");

    uint32_t regularSmall = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 14.0f);
    uint32_t lightSmall = StyleFor("sans-serif-light", FontStack::WEIGHT_NORMAL, false, 14.0f);

    Check(MeasureAscii(regularSmall, "Hello World!") == MeasureAscii(lightSmall, "Hello World!"),
          "at 14px light and regular collapse to the same advance, exactly as the device reports");
}

static void TestFreeTypeAgreesWithHarfBuzz()
{
    uint32_t style = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 32.0f);

    std::vector<uint16_t> text;
    ToUtf16("H", text);

    uint64_t run = FontStack::INVALID_RUN;
    Check(FontStack::ShapeRun(style, text.data(), 1, false, run) == FontStack::Result::Ok, "single glyph shapes");

    int32_t glyphCount = 0;
    FontStack::RunGlyphCount(run, glyphCount);
    Check(glyphCount == 1, "one character produces one glyph");

    FontStack::GlyphPosition position;
    FontStack::RunGlyph(run, 0, position);

    float shaperAdvance = 0.0f;
    FontStack::RunAdvance(run, shaperAdvance);

    FontStack::GlyphImage image;
    Check(FontStack::RasterizeGlyph(style, position.glyphId, image) == FontStack::Result::Ok, "the shaped glyph rasterizes");
    Check(image.width > 0 && image.height > 0, "the rasterized glyph has a non-empty bitmap");
    Check(shaperAdvance > static_cast<float>(image.width) * 0.5f, "the shaper advance is consistent with the raster width");
}

static void TestMetricsAreRealAndOrdered()
{
    uint32_t style = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 14.0f);

    FontStack::Metrics metrics;
    Check(FontStack::FontMetrics(style, metrics) == FontStack::Result::Ok, "font metrics resolve");
    Check(metrics.ascent < 0.0f, "ascent is negative, as Android defines it");
    Check(metrics.descent > 0.0f, "descent is positive");
    Check(metrics.top <= metrics.ascent, "top is at or above ascent");
    Check(metrics.bottom >= metrics.descent, "bottom is at or below descent");
}

static void TestAdvanceScalesWithSize()
{
    float small = MeasureAscii(StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 12.0f), "Hello World!");
    float large = MeasureAscii(StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 48.0f), "Hello World!");

    Check(large > small * 3.0f, "advance grows with text size");
}

static void TestCachesAreBounded()
{
    GlyphCache::Clear();

    uint32_t style = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 14.0f);
    std::vector<uint16_t> text;
    ToUtf16("Hello World!", text);

    uint64_t first = FontStack::INVALID_RUN;
    FontStack::ShapeRun(style, text.data(), static_cast<int32_t>(text.size()), false, first);

    uint64_t second = FontStack::INVALID_RUN;
    FontStack::ShapeRun(style, text.data(), static_cast<int32_t>(text.size()), false, second);
    Check(first == second, "shaping the same run twice hits the shape cache");

    for (int size = 8; size < 200; ++size)
    {
        uint32_t cycled = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, static_cast<float>(size));
        MeasureAscii(cycled, "Hello World!");
    }

    GlyphCache::Statistics stats = GlyphCache::Snapshot();
    Check(stats.bytesResident <= FontStack::MAX_GLYPH_CACHE_BYTES, "the glyph cache stays inside its byte budget");
}

static void TestHostileInputFailsCleanly()
{
    uint32_t style = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 14.0f);

    float advance = 0.0f;
    Check(FontStack::MeasureText(style, nullptr, 5, advance) == FontStack::Result::InvalidArgument, "a null string with a count is rejected");
    Check(FontStack::MeasureText(style, nullptr, -1, advance) == FontStack::Result::InvalidArgument, "a negative count is rejected");

    std::vector<uint16_t> huge(FontStack::MAX_RUN_LENGTH + 1, static_cast<uint16_t>('a'));
    Check(FontStack::MeasureText(style, huge.data(), static_cast<int32_t>(huge.size()), advance) == FontStack::Result::TextTooLong,
          "an over-long run is rejected rather than shaped");

    Check(FontStack::MeasureText(9999, huge.data(), 1, advance) == FontStack::Result::InvalidStyle, "a forged style id is rejected");

    uint32_t enormous = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 1.0e6f);
    FontStack::Metrics metrics;
    Check(FontStack::FontMetrics(enormous, metrics) == FontStack::Result::Ok, "an absurd text size is clamped, not passed to FreeType");

    uint8_t garbage[64];
    memset(garbage, 0xA5, sizeof(garbage));
    uint32_t font = FontStack::INVALID_FONT;
    Check(FontStack::LoadFontFromMemory(garbage, sizeof(garbage), 400, false, font) == FontStack::Result::FontFileMalformed,
          "a malformed font buffer is rejected");
    Check(FontStack::LoadFontFromMemory(garbage, FontStack::MAX_FONT_FILE_BYTES + 1, 400, false, font) == FontStack::Result::FontFileTooLarge,
          "an oversized font is refused before it is read");
}

static void TestRunHandlesAreValidated()
{
    uint32_t style = StyleFor("sans-serif", FontStack::WEIGHT_NORMAL, false, 14.0f);
    std::vector<uint16_t> text;
    ToUtf16("abc", text);

    uint64_t run = FontStack::INVALID_RUN;
    FontStack::ShapeRun(style, text.data(), 3, false, run);

    float advance = 0.0f;
    Check(FontStack::RunAdvance(run, advance) == FontStack::Result::Ok, "a live run reports its advance");
    Check(FontStack::RunAdvance(run ^ 0xFFFFFFFF00000000ull, advance) == FontStack::Result::InvalidRun,
          "a stale generation is rejected rather than dereferenced");
    Check(FontStack::RunAdvance(0, advance) == FontStack::Result::InvalidRun, "a zero handle is rejected");
    Check(FontStack::RunAdvance(0xFFFFFFFFull, advance) == FontStack::Result::InvalidRun, "an out-of-range slot is rejected");
}

int main()
{
    FontStack::Result started = FontStack::Initialize(nullptr);
    if (started != FontStack::Result::Ok)
    {
        fprintf(stderr, "font stack did not start: %s\n", FontStack::ResultName(started));
        return 1;
    }

    TestFamilyResolution();
    TestWeightAffectsAdvance();
    TestFreeTypeAgreesWithHarfBuzz();
    TestMetricsAreRealAndOrdered();
    TestAdvanceScalesWithSize();
    TestCachesAreBounded();
    TestHostileInputFailsCleanly();
    TestRunHandlesAreValidated();

    FontStack::Shutdown();

    printf("%s\n", gFailures == 0 ? "ALL PASS" : "FAILURES");
    return gFailures == 0 ? 0 : 1;
}

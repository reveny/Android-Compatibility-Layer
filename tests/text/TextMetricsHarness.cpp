#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include "FontStack/FontStack.hpp"

static const float SIZES[] = {12.0f, 14.0f, 16.0f, 20.0f, 24.0f, 32.0f, 48.0f};

static const char* CORPUS[] = {
    "Hello World!", "AV", "To", "Yo", "Ta", "0123456789", "iiiiiiiiii", "WWWWWWWWWW",
    "The quick brown fox jumps over the lazy dog",
};

static const char* FAMILIES[] = {"sans-serif", "sans-serif-light", "sans-serif-medium", "monospace"};

static void ToUtf16(const char* ascii, std::vector<uint16_t>& out)
{
    out.clear();
    for (const char* cursor = ascii; *cursor; ++cursor)
    {
        out.push_back(static_cast<uint16_t>(static_cast<unsigned char>(*cursor)));
    }
}

static void EmitFamily(const char* section, const char* label, const char* family)
{
    uint32_t font = FontStack::INVALID_FONT;
    if (FontStack::ResolveFamily(family, FontStack::WEIGHT_NORMAL, false, font) != FontStack::Result::Ok) return;

    for (size_t sizeIndex = 0; sizeIndex < sizeof(SIZES) / sizeof(SIZES[0]); ++sizeIndex)
    {
        float size = SIZES[sizeIndex];

        uint32_t style = FontStack::INVALID_STYLE;
        if (FontStack::InternStyle(font, size, 1.0f, 0.0f, 0.0f, 0, style) != FontStack::Result::Ok) continue;

        FontStack::Metrics metrics;
        FontStack::FontMetrics(style, metrics);

        printf("%s %s %.4f metrics %.4f %.4f %.4f %.4f %.4f\n", section, label, size, metrics.top, metrics.ascent, metrics.descent,
               metrics.bottom, metrics.leading);

        printf("%s %s %.4f metricsInt %d %d %d %d %d\n", section, label, size, static_cast<int>(std::floor(metrics.top)),
               static_cast<int>(std::lround(metrics.ascent)), static_cast<int>(std::lround(metrics.descent)),
               static_cast<int>(std::ceil(metrics.bottom)), static_cast<int>(std::lround(metrics.leading)));

        for (size_t textIndex = 0; textIndex < sizeof(CORPUS) / sizeof(CORPUS[0]); ++textIndex)
        {
            std::vector<uint16_t> text;
            ToUtf16(CORPUS[textIndex], text);

            float advance = 0.0f;
            FontStack::MeasureText(style, text.data(), static_cast<int32_t>(text.size()), advance);
            printf("%s %s %.4f measure %.4f \"%s\"\n", section, label, size, advance, CORPUS[textIndex]);

            FontStack::Bounds bounds;
            FontStack::TextBounds(style, text.data(), static_cast<int32_t>(text.size()), bounds);
            printf("%s %s %.4f bounds %d %d %d %d \"%s\"\n", section, label, size, bounds.left, bounds.top, bounds.right, bounds.bottom,
                   CORPUS[textIndex]);

            std::vector<float> widths(text.size(), 0.0f);
            int32_t written = 0;
            FontStack::TextWidths(style, text.data(), static_cast<int32_t>(text.size()), widths.data(), written);

            printf("%s %s %.4f widths", section, label, size);
            for (int32_t widthIndex = 0; widthIndex < written; ++widthIndex)
            {
                printf(" %.4f", widths[static_cast<size_t>(widthIndex)]);
            }
            printf(" \"%s\"\n", CORPUS[textIndex]);
        }
    }
}

int main(int argc, char** argv)
{
    if (argc > 1 && strcmp(argv[1], "--regenerate") == 0)
    {
        fprintf(stderr, "This harness has no regenerate mode by design.\n");
        fprintf(stderr, "The golden is ground truth captured from a device; regenerating it from the\n");
        fprintf(stderr, "implementation under test would turn the gate into a tautology.\n");
        fprintf(stderr, "Use tools/capture-text-metrics.sh --regenerate against a real device.\n");
        return 2;
    }

    FontStack::Result started = FontStack::Initialize(nullptr);
    if (started != FontStack::Result::Ok)
    {
        fprintf(stderr, "font stack did not start: %s\n", FontStack::ResultName(started));
        return 1;
    }

    EmitFamily("file", "Roboto-Regular", "sans-serif");

    for (size_t index = 0; index < sizeof(FAMILIES) / sizeof(FAMILIES[0]); ++index)
    {
        EmitFamily("family", FAMILIES[index], FAMILIES[index]);
    }

    FontStack::Shutdown();
    return 0;
}

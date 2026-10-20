#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#include <ft2build.h>
#include FT_FREETYPE_H

#include "FontStack/FontStack.hpp"
#include "Rasterizer/Rasterizer.hpp"

static const uint32_t SURFACE_WIDTH = 360;
static const uint32_t SURFACE_HEIGHT = 220;
static const uint32_t BACKGROUND = 0xFF101828u;
static const uint32_t INK = 0xFFF0F0F0u;

static const char* CORPUS[] = {"Hello World!", "AVATAR Two", "gjpqy 0123", "Wave"};
static const float SIZES[] = {12.0f, 16.0f, 24.0f, 40.0f};
static const char* FAMILIES[] = {"sans-serif", "sans-serif-light", "sans-serif-medium", "monospace"};

static void ToUtf16(const char* ascii, std::vector<uint16_t>& out)
{
    out.clear();
    for (const char* cursor = ascii; *cursor; ++cursor)
    {
        out.push_back(static_cast<uint16_t>(static_cast<unsigned char>(*cursor)));
    }
}

static bool DrawRun(Rasterizer::Canvas& canvas, const char* family, float textSize, const char* ascii, int32_t x, int32_t baseline)
{
    uint32_t font = FontStack::INVALID_FONT;
    if (FontStack::ResolveFamily(family, FontStack::WEIGHT_NORMAL, false, font) != FontStack::Result::Ok) return false;

    uint32_t style = FontStack::INVALID_STYLE;
    if (FontStack::InternStyle(font, textSize, 1.0f, 0.0f, 0.0f, 0, style) != FontStack::Result::Ok) return false;

    std::vector<uint16_t> text;
    ToUtf16(ascii, text);

    uint64_t run = FontStack::INVALID_RUN;
    if (FontStack::ShapeRun(style, text.data(), static_cast<int32_t>(text.size()), false, run) != FontStack::Result::Ok) return false;

    int32_t glyphCount = 0;
    FontStack::RunGlyphCount(run, glyphCount);

    for (int32_t index = 0; index < glyphCount; ++index)
    {
        FontStack::GlyphPosition position;
        if (FontStack::RunGlyph(run, index, position) != FontStack::Result::Ok) return false;

        FontStack::GlyphImage image;
        if (FontStack::RasterizeGlyph(style, position.glyphId, image) != FontStack::Result::Ok) continue;
        if (!image.samples || image.width <= 0 || image.height <= 0) continue;

        Rasterizer::Coverage mask;
        mask.samples = image.samples;
        mask.width = image.width;
        mask.height = image.height;
        mask.pitch = image.pitch;

        int32_t penX = x + static_cast<int32_t>(position.x) + image.bearingX;
        int32_t penY = baseline - image.bearingY;
        Rasterizer::DrawCoverage(canvas, mask, penX, penY, INK);
    }

    return true;
}

static bool PaintCorpus(Rasterizer::Canvas& canvas)
{
    Rasterizer::DrawColor(canvas, BACKGROUND);

    int32_t baseline = 20;
    for (size_t familyIndex = 0; familyIndex < sizeof(FAMILIES) / sizeof(FAMILIES[0]); ++familyIndex)
    {
        for (size_t sizeIndex = 0; sizeIndex < sizeof(SIZES) / sizeof(SIZES[0]); ++sizeIndex)
        {
            const char* text = CORPUS[(familyIndex + sizeIndex) % (sizeof(CORPUS) / sizeof(CORPUS[0]))];
            if (!DrawRun(canvas, FAMILIES[familyIndex], SIZES[sizeIndex], text, 8, baseline)) return false;
            baseline += static_cast<int32_t>(SIZES[sizeIndex]) / 2 + 6;
        }
    }

    return true;
}

static bool ReadProvenance(const char* path, std::string& freetypeVersion, std::string& fontHash)
{
    FILE* handle = fopen(path, "rb");
    if (!handle) return false;

    char line[512];
    while (fgets(line, sizeof(line), handle))
    {
        char* newline = strchr(line, '\n');
        if (newline) *newline = '\0';

        if (strncmp(line, "freetype=", 9) == 0) freetypeVersion = line + 9;
        else if (strncmp(line, "font_sha256=", 12) == 0) fontHash = line + 12;
    }

    fclose(handle);
    return !freetypeVersion.empty();
}

static bool FontDigest(const char* path, std::string& digest)
{
    std::string command = std::string("sha256sum ") + path + " 2>/dev/null";

    FILE* pipe = popen(command.c_str(), "r");
    if (!pipe) return false;

    char buffer[128];
    bool read = fgets(buffer, sizeof(buffer), pipe) != nullptr;
    pclose(pipe);

    if (!read) return false;

    digest.assign(buffer);
    size_t space = digest.find(' ');
    if (space != std::string::npos) digest.resize(space);

    return digest.size() == 64;
}

static std::string CurrentFreeTypeVersion()
{
    char buffer[64];
    snprintf(buffer, sizeof(buffer), "%d.%d.%d", FREETYPE_MAJOR, FREETYPE_MINOR, FREETYPE_PATCH);
    return buffer;
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: %s <golden.raw> [--regenerate]\n", argv[0]);
        return 2;
    }

    const char* goldenPath = argv[1];
    bool regenerate = argc > 2 && strcmp(argv[2], "--regenerate") == 0;
    if (argc > 2 && !regenerate)
    {
        fprintf(stderr, "unrecognised argument: %s\n", argv[2]);
        return 2;
    }

    std::string provenancePath = std::string(goldenPath) + ".provenance";
    std::string pinnedFreeType;
    std::string pinnedFontHash;

    if (!regenerate)
    {
        if (!ReadProvenance(provenancePath.c_str(), pinnedFreeType, pinnedFontHash))
        {
            fprintf(stderr, "text golden provenance missing or malformed: %s\n", provenancePath.c_str());
            return 1;
        }

        std::string current = CurrentFreeTypeVersion();
        if (current != pinnedFreeType)
        {
            fprintf(stderr, "text golden was captured with FreeType %s, this build has %s.\n", pinnedFreeType.c_str(), current.c_str());
            fprintf(stderr, "Anti-aliased glyph coverage is only reproducible for a fixed FreeType.\n");
            fprintf(stderr, "Re-capture deliberately with --regenerate after reviewing the diff.\n");
            return 1;
        }

        if (pinnedFontHash.empty())
        {
            fprintf(stderr, "text golden provenance has no font_sha256= line: %s\n", provenancePath.c_str());
            return 1;
        }

        std::string fontPath = std::string(RUNTIME_DEFAULT_FONT_DIR) + "/Roboto-Regular.ttf";

        std::string actualHash;
        if (!FontDigest(fontPath.c_str(), actualHash))
        {
            fprintf(stderr, "could not hash the font at %s\n", fontPath.c_str());
            return 1;
        }

        if (actualHash != pinnedFontHash)
        {
            fprintf(stderr, "text golden was captured with a font whose SHA-256 is %s,\n", pinnedFontHash.c_str());
            fprintf(stderr, "but %s hashes to %s.\n", fontPath.c_str(), actualHash.c_str());
            return 1;
        }
    }

    FontStack::Result started = FontStack::Initialize(nullptr);
    if (started != FontStack::Result::Ok)
    {
        fprintf(stderr, "font stack did not start: %s\n", FontStack::ResultName(started));
        return 1;
    }

    Rasterizer::Surface surface;
    if (Rasterizer::CreateSurface(SURFACE_WIDTH, SURFACE_HEIGHT, surface) != Rasterizer::Result::Ok)
    {
        fprintf(stderr, "could not create the surface\n");
        return 1;
    }

    Rasterizer::Canvas canvas;
    Rasterizer::Begin(surface, canvas);

    if (!PaintCorpus(canvas))
    {
        fprintf(stderr, "the corpus did not render\n");
        return 1;
    }

    size_t sizeBytes = static_cast<size_t>(SURFACE_WIDTH) * SURFACE_HEIGHT * Rasterizer::BYTES_PER_PIXEL;

    if (regenerate)
    {
        FILE* out = fopen(goldenPath, "wb");
        if (!out)
        {
            fprintf(stderr, "cannot write %s\n", goldenPath);
            return 1;
        }
        fwrite(surface.pixels, 1, sizeBytes, out);
        fclose(out);
        printf("regenerated %s (%zu bytes) with FreeType %s\n", goldenPath, sizeBytes, CurrentFreeTypeVersion().c_str());

        Rasterizer::DestroySurface(surface);
        FontStack::Shutdown();
        return 0;
    }

    FILE* golden = fopen(goldenPath, "rb");
    if (!golden)
    {
        fprintf(stderr, "text golden missing: %s (run with --regenerate)\n", goldenPath);
        return 1;
    }

    std::vector<uint8_t> expected(sizeBytes);
    size_t got = fread(expected.data(), 1, sizeBytes, golden);
    long extra = 0;
    if (fseek(golden, 0, SEEK_END) == 0) extra = ftell(golden);
    fclose(golden);

    if (got != sizeBytes || static_cast<size_t>(extra) != sizeBytes)
    {
        fprintf(stderr, "text golden is %ld bytes, expected %zu\n", extra, sizeBytes);
        return 1;
    }

    size_t differing = 0;
    for (size_t index = 0; index < sizeBytes; index += Rasterizer::BYTES_PER_PIXEL)
    {
        if (memcmp(surface.pixels + index, expected.data() + index, Rasterizer::BYTES_PER_PIXEL) != 0) differing += 1;
    }

    Rasterizer::DestroySurface(surface);
    FontStack::Shutdown();

    if (differing != 0)
    {
        fprintf(stderr, "%zu of %u pixels differ from %s\n", differing, SURFACE_WIDTH * SURFACE_HEIGHT, goldenPath);
        return 1;
    }

    printf("text golden matches: %ux%u, FreeType %s\n", SURFACE_WIDTH, SURFACE_HEIGHT, CurrentFreeTypeVersion().c_str());
    return 0;
}

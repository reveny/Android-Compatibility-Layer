#include "FontStack/FontStack.hpp"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <thread>
#include <vector>

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_MULTIPLE_MASTERS_H

#include <hb-ft.h>
#include <hb.h>

#include "FontStack/FontRegistry.hpp"
#include "FontStack/GlyphCache.hpp"
#include "Settings/Settings.hpp"

struct FontEntry
{
    std::string fileName;
    std::vector<uint8_t> bytes;
    FT_Face face = nullptr;
    hb_font_t* shaper = nullptr;
    float weightAxis = FontStack::WEIGHT_NORMAL;
    float widthAxis = FontRegistry::AXIS_WIDTH_NORMAL;
    float italicAxis = 0.0f;
    bool syntheticItalic = false;
    int weight = FontStack::WEIGHT_NORMAL;
    bool italic = false;
};

struct StyleEntry
{
    uint32_t font = FontStack::INVALID_FONT;
    float textSize = 0.0f;
    float scaleX = 1.0f;
    float skewX = 0.0f;
    float letterSpacing = 0.0f;
    int flags = 0;
};

struct RunEntry
{
    uint32_t generation = 0;
    uint32_t style = FontStack::INVALID_STYLE;
    bool live = false;
    float advance = 0.0f;
    std::vector<FontStack::GlyphPosition> glyphs;
};

static FT_Library gLibrary = nullptr;
static std::string gFontDirectory;
static std::vector<FontEntry> gFonts;
static std::vector<StyleEntry> gStyles;
static std::vector<RunEntry> gRuns;
static std::vector<size_t> gFreeRuns;
static std::thread::id gOwnerThread;
static bool gInitialized = false;

static bool OnOwnerThread()
{
    return std::this_thread::get_id() == gOwnerThread;
}

static float ClampTextSize(float textSize)
{
    if (!(textSize > 0.0f)) return FontStack::MIN_TEXT_SIZE;
    if (textSize < FontStack::MIN_TEXT_SIZE) return FontStack::MIN_TEXT_SIZE;
    if (textSize > FontStack::MAX_TEXT_SIZE) return FontStack::MAX_TEXT_SIZE;
    return textSize;
}

static FT_Fixed ToFixed(float value)
{
    return static_cast<FT_Fixed>(std::lround(static_cast<double>(value) * 65536.0));
}

static FontStack::Result ApplyVariationAxes(FontEntry& entry)
{
    FT_MM_Var* variations = nullptr;
    if (FT_Get_MM_Var(entry.face, &variations) != 0 || !variations) return FontStack::Result::Ok;

    std::vector<FT_Fixed> coordinates(variations->num_axis, 0);
    std::vector<hb_variation_t> shaperAxes;

    for (FT_UInt axis = 0; axis < variations->num_axis; ++axis)
    {
        uint32_t tag = static_cast<uint32_t>(variations->axis[axis].tag);
        float wanted = static_cast<float>(variations->axis[axis].def) / 65536.0f;

        if (tag == FT_MAKE_TAG('w', 'g', 'h', 't')) wanted = entry.weightAxis;
        else if (tag == FT_MAKE_TAG('w', 'd', 't', 'h')) wanted = entry.widthAxis;
        else if (tag == FT_MAKE_TAG('i', 't', 'a', 'l')) wanted = entry.italicAxis;

        float minimum = static_cast<float>(variations->axis[axis].minimum) / 65536.0f;
        float maximum = static_cast<float>(variations->axis[axis].maximum) / 65536.0f;
        if (wanted < minimum) wanted = minimum;
        if (wanted > maximum) wanted = maximum;

        coordinates[axis] = ToFixed(wanted);

        hb_variation_t shaperAxis{};
        shaperAxis.tag = tag;
        shaperAxis.value = wanted;
        shaperAxes.push_back(shaperAxis);
    }

    FT_Error setError = FT_Set_Var_Design_Coordinates(entry.face, variations->num_axis, coordinates.data());
    FT_Done_MM_Var(gLibrary, variations);

    if (setError != 0) return FontStack::Result::FontFileMalformed;

    if (entry.shaper && !shaperAxes.empty())
    {
        hb_font_set_variations(entry.shaper, shaperAxes.data(), static_cast<unsigned int>(shaperAxes.size()));
    }

    return FontStack::Result::Ok;
}

static FontStack::Result CreateFont(std::vector<uint8_t>&& bytes, const FontRegistry::Selection& selection, int weight, bool italic,
                                    uint32_t& font)
{
    if (gFonts.size() >= static_cast<size_t>(FontStack::MAX_FONT_COUNT)) return FontStack::Result::FontTableFull;

    FontEntry entry;
    entry.bytes = std::move(bytes);
    entry.fileName = selection.fileName ? selection.fileName : "";
    entry.weightAxis = selection.weightAxis;
    entry.widthAxis = selection.widthAxis;
    entry.italicAxis = selection.italicAxis;
    entry.syntheticItalic = selection.syntheticItalic;
    entry.weight = weight;
    entry.italic = italic;

    FT_Error error = FT_New_Memory_Face(gLibrary, entry.bytes.data(), static_cast<FT_Long>(entry.bytes.size()), 0, &entry.face);
    if (error != 0) return FontStack::Result::FontFileMalformed;

    if (entry.face->units_per_EM == 0)
    {
        FT_Done_Face(entry.face);
        return FontStack::Result::FontFileMalformed;
    }

    entry.shaper = hb_ft_font_create_referenced(entry.face);
    if (!entry.shaper)
    {
        FT_Done_Face(entry.face);
        return FontStack::Result::FontFileMalformed;
    }

    hb_ft_font_set_load_flags(entry.shaper, FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP);

    FontStack::Result applied = ApplyVariationAxes(entry);
    if (applied != FontStack::Result::Ok)
    {
        hb_font_destroy(entry.shaper);
        FT_Done_Face(entry.face);
        return applied;
    }

    gFonts.push_back(std::move(entry));
    font = static_cast<uint32_t>(gFonts.size());
    return FontStack::Result::Ok;
}

static FontEntry* FontFrom(uint32_t font)
{
    if (font == FontStack::INVALID_FONT || font > gFonts.size()) return nullptr;
    return &gFonts[font - 1];
}

static StyleEntry* StyleFrom(uint32_t style)
{
    if (style == FontStack::INVALID_STYLE || style > gStyles.size()) return nullptr;
    return &gStyles[style - 1];
}

static FontStack::Result PrepareFace(const StyleEntry& style, FontEntry*& entry)
{
    entry = FontFrom(style.font);
    if (!entry) return FontStack::Result::InvalidFont;

    FT_F26Dot6 size = static_cast<FT_F26Dot6>(std::lround(static_cast<double>(style.textSize) * 64.0));
    if (FT_Set_Char_Size(entry->face, 0, size, 72, 72) != 0) return FontStack::Result::RasterizationFailed;

    hb_ft_font_changed(entry->shaper);
    return FontStack::Result::Ok;
}

static bool ReadFile(const std::string& path, std::vector<uint8_t>& bytes)
{
    FILE* handle = fopen(path.c_str(), "rb");
    if (!handle) return false;

    if (fseek(handle, 0, SEEK_END) != 0)
    {
        fclose(handle);
        return false;
    }

    long length = ftell(handle);
    if (length <= 0 || static_cast<size_t>(length) > FontStack::MAX_FONT_FILE_BYTES)
    {
        fclose(handle);
        return false;
    }

    rewind(handle);
    bytes.resize(static_cast<size_t>(length));
    size_t got = fread(bytes.data(), 1, bytes.size(), handle);
    fclose(handle);

    return got == bytes.size();
}

FontStack::Result FontStack::Initialize(const char* fontDirectory)
{
    if (gInitialized) return Result::AlreadyInitialized;

    const char* directory = fontDirectory;
    if (!directory || !*directory) directory = Settings::Lookup(FontStack::DIRECTORY_SETTING);
    if (!directory || !*directory) directory = RUNTIME_DEFAULT_FONT_DIR;

    std::string probe = std::string(directory) + "/Roboto-Regular.ttf";
    std::vector<uint8_t> bytes;
    if (!ReadFile(probe, bytes)) return Result::FontDirectoryMissing;

    if (FT_Init_FreeType(&gLibrary) != 0) return Result::RasterizationFailed;

    gFontDirectory = directory;
    gOwnerThread = std::this_thread::get_id();
    gInitialized = true;
    return Result::Ok;
}

void FontStack::Shutdown()
{
    if (!gInitialized) return;

    ShapeCache::Clear();
    GlyphCache::Clear();

    for (FontEntry& entry : gFonts)
    {
        if (entry.shaper) hb_font_destroy(entry.shaper);
        if (entry.face) FT_Done_Face(entry.face);
    }

    gFonts.clear();
    gStyles.clear();
    gRuns.clear();
    gFreeRuns.clear();

    if (gLibrary) FT_Done_FreeType(gLibrary);
    gLibrary = nullptr;
    gInitialized = false;
}

bool FontStack::IsInitialized()
{
    return gInitialized;
}

FontStack::Result FontStack::ResolveFamily(const char* familyName, int weight, bool italic, uint32_t& font)
{
    if (!gInitialized) return Result::NotInitialized;
    if (!OnOwnerThread()) return Result::WrongThread;

    FontRegistry::Selection selection;
    if (!FontRegistry::Resolve(familyName, weight, italic, selection)) return Result::UnknownFamily;

    for (size_t index = 0; index < gFonts.size(); ++index)
    {
        const FontEntry& entry = gFonts[index];
        if (entry.fileName != selection.fileName) continue;
        if (entry.weightAxis != selection.weightAxis) continue;
        if (entry.widthAxis != selection.widthAxis) continue;
        if (entry.italicAxis != selection.italicAxis) continue;
        if (entry.syntheticItalic != selection.syntheticItalic) continue;

        font = static_cast<uint32_t>(index + 1);
        return Result::Ok;
    }

    std::vector<uint8_t> bytes;
    if (!ReadFile(gFontDirectory + "/" + selection.fileName, bytes)) return Result::FontFileUnreadable;

    return CreateFont(std::move(bytes), selection, weight, italic, font);
}

FontStack::Result FontStack::LoadFontFromMemory(const uint8_t* fontBytes, size_t byteCount, int weight, bool italic, uint32_t& font)
{
    if (!gInitialized) return Result::NotInitialized;
    if (!OnOwnerThread()) return Result::WrongThread;
    if (!fontBytes || byteCount == 0) return Result::InvalidArgument;
    if (byteCount > MAX_FONT_FILE_BYTES) return Result::FontFileTooLarge;

    FontRegistry::Selection selection;
    selection.fileName = "";
    selection.weightAxis = static_cast<float>(weight);
    selection.widthAxis = FontRegistry::AXIS_WIDTH_NORMAL;
    selection.italicAxis = italic ? 1.0f : 0.0f;

    std::vector<uint8_t> copied(fontBytes, fontBytes + byteCount);
    return CreateFont(std::move(copied), selection, weight, italic, font);
}

FontStack::Result FontStack::FontWeight(uint32_t font, int& weight)
{
    FontEntry* entry = FontFrom(font);
    if (!entry) return Result::InvalidFont;

    weight = entry->weight;
    return Result::Ok;
}

FontStack::Result FontStack::FontIsItalic(uint32_t font, bool& italic)
{
    FontEntry* entry = FontFrom(font);
    if (!entry) return Result::InvalidFont;

    italic = entry->italic;
    return Result::Ok;
}

FontStack::Result FontStack::InternStyle(uint32_t font, float textSize, float scaleX, float skewX, float letterSpacing, int flags,
                                         uint32_t& style)
{
    if (!gInitialized) return Result::NotInitialized;
    if (!OnOwnerThread()) return Result::WrongThread;
    if (!FontFrom(font)) return Result::InvalidFont;

    float clamped = ClampTextSize(textSize);

    for (size_t index = 0; index < gStyles.size(); ++index)
    {
        const StyleEntry& entry = gStyles[index];
        if (entry.font != font) continue;
        if (entry.textSize != clamped) continue;
        if (entry.scaleX != scaleX) continue;
        if (entry.skewX != skewX) continue;
        if (entry.letterSpacing != letterSpacing) continue;
        if (entry.flags != flags) continue;

        style = static_cast<uint32_t>(index + 1);
        return Result::Ok;
    }

    if (gStyles.size() >= static_cast<size_t>(MAX_STYLE_COUNT)) return Result::StyleTableFull;

    StyleEntry entry;
    entry.font = font;
    entry.textSize = clamped;
    entry.scaleX = scaleX;
    entry.skewX = skewX;
    entry.letterSpacing = letterSpacing;
    entry.flags = flags;

    gStyles.push_back(entry);
    style = static_cast<uint32_t>(gStyles.size());
    return Result::Ok;
}

static RunEntry* RunFrom(uint64_t run);

static FontStack::Result ShapeInternal(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft,
                                       std::vector<FontStack::GlyphPosition>& glyphs, float& advance)
{
    if (!gInitialized) return FontStack::Result::NotInitialized;
    if (!OnOwnerThread()) return FontStack::Result::WrongThread;
    if (!text && count > 0) return FontStack::Result::InvalidArgument;
    if (count < 0) return FontStack::Result::InvalidArgument;
    if (count > FontStack::MAX_RUN_LENGTH) return FontStack::Result::TextTooLong;

    StyleEntry* entry = StyleFrom(style);
    if (!entry) return FontStack::Result::InvalidStyle;

    FontEntry* font = nullptr;
    FontStack::Result prepared = PrepareFace(*entry, font);
    if (prepared != FontStack::Result::Ok) return prepared;

    glyphs.clear();
    advance = 0.0f;
    if (count == 0) return FontStack::Result::Ok;

    hb_buffer_t* buffer = hb_buffer_create();
    if (!buffer) return FontStack::Result::ShapingFailed;

    hb_buffer_add_utf16(buffer, text, count, 0, count);
    hb_buffer_guess_segment_properties(buffer);
    hb_buffer_set_direction(buffer, rightToLeft ? HB_DIRECTION_RTL : HB_DIRECTION_LTR);

    hb_shape(font->shaper, buffer, nullptr, 0);

    unsigned int produced = 0;
    hb_glyph_info_t* infos = hb_buffer_get_glyph_infos(buffer, &produced);
    hb_glyph_position_t* positions = hb_buffer_get_glyph_positions(buffer, &produced);

    if (!infos || !positions)
    {
        hb_buffer_destroy(buffer);
        return FontStack::Result::ShapingFailed;
    }

    float penX = 0.0f;
    float penY = 0.0f;
    glyphs.reserve(produced);

    for (unsigned int index = 0; index < produced; ++index)
    {
        FontStack::GlyphPosition position;
        position.glyphId = infos[index].codepoint;
        position.cluster = static_cast<int32_t>(infos[index].cluster);
        position.x = (penX + static_cast<float>(positions[index].x_offset) / 64.0f) * entry->scaleX;
        position.y = penY + static_cast<float>(positions[index].y_offset) / 64.0f;
        glyphs.push_back(position);

        float shaped = static_cast<float>(positions[index].x_advance) / 64.0f;
        float base = static_cast<float>(hb_font_get_glyph_h_advance(font->shaper, infos[index].codepoint)) / 64.0f;

        penX += std::round(base) + (shaped - base) + entry->letterSpacing;
        penY += static_cast<float>(positions[index].y_advance) / 64.0f;
    }

    hb_buffer_destroy(buffer);

    advance = penX * entry->scaleX;
    return FontStack::Result::Ok;
}

FontStack::Result FontStack::MeasureText(uint32_t style, const uint16_t* text, int32_t count, float& advance)
{
    std::vector<GlyphPosition> glyphs;
    Result shaped = ShapeInternal(style, text, count, false, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    advance = std::ceil(advance - ADVANCE_EPSILON);
    return Result::Ok;
}

FontStack::Result FontStack::TextWidths(uint32_t style, const uint16_t* text, int32_t count, float* outWidths, int32_t& written)
{
    written = 0;
    if (!outWidths && count > 0) return Result::InvalidArgument;

    std::vector<GlyphPosition> glyphs;
    float advance = 0.0f;
    Result shaped = ShapeInternal(style, text, count, false, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    for (int32_t index = 0; index < count; ++index)
    {
        outWidths[index] = 0.0f;
    }

    for (size_t index = 0; index < glyphs.size(); ++index)
    {
        float next = index + 1 < glyphs.size() ? glyphs[index + 1].x : advance;
        int32_t cluster = glyphs[index].cluster;
        if (cluster < 0 || cluster >= count) continue;
        outWidths[cluster] += next - glyphs[index].x;
    }

    written = count;
    return Result::Ok;
}

FontStack::Result FontStack::FontMetrics(uint32_t style, Metrics& metrics)
{
    if (!gInitialized) return Result::NotInitialized;
    if (!OnOwnerThread()) return Result::WrongThread;

    StyleEntry* entry = StyleFrom(style);
    if (!entry) return Result::InvalidStyle;

    FontEntry* font = nullptr;
    Result prepared = PrepareFace(*entry, font);
    if (prepared != Result::Ok) return prepared;

    const FT_Size_Metrics& sized = font->face->size->metrics;
    FT_Fixed verticalScale = sized.y_scale;

    metrics.ascent = -static_cast<float>(FT_MulFix(font->face->ascender, verticalScale)) / 64.0f;
    metrics.descent = -static_cast<float>(FT_MulFix(font->face->descender, verticalScale)) / 64.0f;
    metrics.top = -static_cast<float>(FT_MulFix(font->face->bbox.yMax, verticalScale)) / 64.0f;
    metrics.bottom = -static_cast<float>(FT_MulFix(font->face->bbox.yMin, verticalScale)) / 64.0f;
    metrics.leading = static_cast<float>(FT_MulFix(font->face->height, verticalScale)) / 64.0f - (metrics.descent - metrics.ascent);

    return Result::Ok;
}

FontStack::Result FontStack::TextBounds(uint32_t style, const uint16_t* text, int32_t count, Bounds& bounds)
{
    std::vector<GlyphPosition> glyphs;
    float advance = 0.0f;
    Result shaped = ShapeInternal(style, text, count, false, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    bounds = Bounds{};
    if (glyphs.empty()) return Result::Ok;

    bool first = true;
    for (const GlyphPosition& glyph : glyphs)
    {
        GlyphImage image;
        if (RasterizeGlyph(style, glyph.glyphId, image) != Result::Ok) continue;

        int32_t left = static_cast<int32_t>(std::lround(glyph.x)) + image.bearingX;
        int32_t top = -image.bearingY;
        int32_t right = left + image.width;
        int32_t bottom = top + image.height;

        if (first)
        {
            bounds.left = left;
            bounds.top = top;
            bounds.right = right;
            bounds.bottom = bottom;
            first = false;
            continue;
        }

        if (left < bounds.left) bounds.left = left;
        if (top < bounds.top) bounds.top = top;
        if (right > bounds.right) bounds.right = right;
        if (bottom > bounds.bottom) bounds.bottom = bottom;
    }

    return Result::Ok;
}

FontStack::Result FontStack::BreakText(uint32_t style, const uint16_t* text, int32_t count, float maxWidth, bool forwards,
                                       int32_t& breakAt, float& measuredWidth)
{
    breakAt = 0;
    measuredWidth = 0.0f;

    std::vector<GlyphPosition> glyphs;
    float advance = 0.0f;
    Result shaped = ShapeInternal(style, text, count, false, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    if (advance <= maxWidth)
    {
        breakAt = count;
        measuredWidth = advance;
        return Result::Ok;
    }

    if (forwards)
    {
        for (size_t index = 0; index < glyphs.size(); ++index)
        {
            float edge = index + 1 < glyphs.size() ? glyphs[index + 1].x : advance;
            if (edge > maxWidth) break;

            breakAt = index + 1 < glyphs.size() ? glyphs[index + 1].cluster : count;
            measuredWidth = edge;
        }
        return Result::Ok;
    }

    for (size_t index = glyphs.size(); index > 0; --index)
    {
        float consumed = advance - glyphs[index - 1].x;
        if (consumed > maxWidth) break;

        breakAt = count - glyphs[index - 1].cluster;
        measuredWidth = consumed;
    }

    return Result::Ok;
}

FontStack::Result FontStack::HasGlyph(uint32_t style, const uint16_t* text, int32_t count, bool& present)
{
    present = false;

    std::vector<GlyphPosition> glyphs;
    float advance = 0.0f;
    Result shaped = ShapeInternal(style, text, count, false, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    if (glyphs.size() != 1) return Result::Ok;

    present = glyphs[0].glyphId != 0;
    return Result::Ok;
}

FontStack::Result FontStack::ShapeRun(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t& run)
{
    run = INVALID_RUN;

    uint64_t cached = INVALID_RUN;
    if (ShapeCache::Lookup(style, text, count, rightToLeft, cached))
    {
        if (RunFrom(cached))
        {
            run = cached;
            return Result::Ok;
        }

        ShapeCache::Forget(cached);
    }

    std::vector<GlyphPosition> glyphs;
    float advance = 0.0f;
    Result shaped = ShapeInternal(style, text, count, rightToLeft, glyphs, advance);
    if (shaped != Result::Ok) return shaped;

    size_t slot = 0;
    if (!gFreeRuns.empty())
    {
        slot = gFreeRuns.back();
        gFreeRuns.pop_back();
    }
    else
    {
        gRuns.push_back(RunEntry{});
        slot = gRuns.size() - 1;
    }

    RunEntry& entry = gRuns[slot];
    entry.generation += 1;
    entry.style = style;
    entry.live = true;
    entry.advance = advance;
    entry.glyphs = std::move(glyphs);

    run = (static_cast<uint64_t>(entry.generation) << 32) | static_cast<uint64_t>(slot + 1);

    uint64_t evicted = INVALID_RUN;
    ShapeCache::Insert(style, text, count, rightToLeft, run, evicted);
    if (evicted != INVALID_RUN && evicted != run) ReleaseRun(evicted);

    return Result::Ok;
}

static RunEntry* RunFrom(uint64_t run)
{
    uint32_t slot = static_cast<uint32_t>(run & 0xFFFFFFFFull);
    uint32_t generation = static_cast<uint32_t>(run >> 32);
    if (slot == 0 || slot > gRuns.size()) return nullptr;

    RunEntry& entry = gRuns[slot - 1];
    if (!entry.live || entry.generation != generation) return nullptr;
    return &entry;
}

FontStack::Result FontStack::RunAdvance(uint64_t run, float& advance)
{
    RunEntry* entry = RunFrom(run);
    if (!entry) return Result::InvalidRun;

    advance = entry->advance;
    return Result::Ok;
}

FontStack::Result FontStack::RunGlyphCount(uint64_t run, int32_t& count)
{
    RunEntry* entry = RunFrom(run);
    if (!entry) return Result::InvalidRun;

    count = static_cast<int32_t>(entry->glyphs.size());
    return Result::Ok;
}

FontStack::Result FontStack::RunGlyph(uint64_t run, int32_t index, GlyphPosition& position)
{
    RunEntry* entry = RunFrom(run);
    if (!entry) return Result::InvalidRun;
    if (index < 0 || index >= static_cast<int32_t>(entry->glyphs.size())) return Result::InvalidArgument;

    position = entry->glyphs[static_cast<size_t>(index)];
    return Result::Ok;
}

FontStack::Result FontStack::ReleaseRun(uint64_t run)
{
    RunEntry* entry = RunFrom(run);
    if (!entry) return Result::InvalidRun;

    uint32_t slot = static_cast<uint32_t>(run & 0xFFFFFFFFull);
    entry->live = false;
    entry->glyphs.clear();
    gFreeRuns.push_back(slot - 1);
    return Result::Ok;
}

FontStack::Result FontStack::RasterizeGlyph(uint32_t style, uint32_t glyphId, GlyphImage& image)
{
    if (!gInitialized) return Result::NotInitialized;
    if (!OnOwnerThread()) return Result::WrongThread;

    StyleEntry* entry = StyleFrom(style);
    if (!entry) return Result::InvalidStyle;

    uint64_t key = GlyphCache::KeyFor(style, glyphId, entry->flags);
    if (GlyphCache::Lookup(key, image)) return Result::Ok;

    FontEntry* font = nullptr;
    Result prepared = PrepareFace(*entry, font);
    if (prepared != Result::Ok) return prepared;

    if (FT_Load_Glyph(font->face, glyphId, FT_LOAD_NO_HINTING | FT_LOAD_NO_BITMAP) != 0) return Result::RasterizationFailed;
    if (FT_Render_Glyph(font->face->glyph, FT_RENDER_MODE_NORMAL) != 0) return Result::RasterizationFailed;

    const FT_Bitmap& bitmap = font->face->glyph->bitmap;
    image.samples = bitmap.buffer;
    image.width = static_cast<int32_t>(bitmap.width);
    image.height = static_cast<int32_t>(bitmap.rows);
    image.pitch = bitmap.pitch;
    image.bearingX = font->face->glyph->bitmap_left;
    image.bearingY = font->face->glyph->bitmap_top;

    if (GlyphCache::ShouldCache(image.width, image.height)) GlyphCache::Insert(key, image);
    return Result::Ok;
}

const char* FontStack::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotInitialized: return "NotInitialized";
        case Result::AlreadyInitialized: return "AlreadyInitialized";
        case Result::FontDirectoryMissing: return "FontDirectoryMissing";
        case Result::FontFileUnreadable: return "FontFileUnreadable";
        case Result::FontFileMalformed: return "FontFileMalformed";
        case Result::FontFileTooLarge: return "FontFileTooLarge";
        case Result::UnknownFamily: return "UnknownFamily";
        case Result::InvalidFont: return "InvalidFont";
        case Result::InvalidStyle: return "InvalidStyle";
        case Result::InvalidRun: return "InvalidRun";
        case Result::InvalidArgument: return "InvalidArgument";
        case Result::TextTooLong: return "TextTooLong";
        case Result::StyleTableFull: return "StyleTableFull";
        case Result::FontTableFull: return "FontTableFull";
        case Result::ShapingFailed: return "ShapingFailed";
        case Result::RasterizationFailed: return "RasterizationFailed";
        case Result::WrongThread: return "WrongThread";
    }

    return "Unknown";
}

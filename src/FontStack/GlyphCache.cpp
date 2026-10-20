#include "FontStack/GlyphCache.hpp"

#include <cstring>
#include <unordered_map>
#include <vector>

struct CachedGlyph
{
    std::vector<uint8_t> samples;
    int32_t width = 0;
    int32_t height = 0;
    int32_t pitch = 0;
    int32_t bearingX = 0;
    int32_t bearingY = 0;
    uint64_t lastUsed = 0;
};

static std::unordered_map<uint64_t, CachedGlyph> gGlyphs;
static GlyphCache::Statistics gStatistics;
static uint64_t gClock = 0;

uint64_t GlyphCache::KeyFor(uint32_t style, uint32_t glyphId, int flags)
{
    uint64_t key = static_cast<uint64_t>(style) & 0xFFFFull;
    key |= (static_cast<uint64_t>(glyphId) & 0xFFFFFFull) << 16;
    key |= (static_cast<uint64_t>(static_cast<uint32_t>(flags)) & 0xFFull) << 40;
    return key;
}

bool GlyphCache::ShouldCache(int32_t width, int32_t height)
{
    if (width <= 0 || height <= 0) return false;
    return width <= FontStack::MAX_CACHED_GLYPH_DIMENSION && height <= FontStack::MAX_CACHED_GLYPH_DIMENSION;
}

bool GlyphCache::Lookup(uint64_t key, FontStack::GlyphImage& image)
{
    gStatistics.lookups += 1;

    auto found = gGlyphs.find(key);
    if (found == gGlyphs.end()) return false;

    found->second.lastUsed = ++gClock;
    image.samples = found->second.samples.data();
    image.width = found->second.width;
    image.height = found->second.height;
    image.pitch = found->second.pitch;
    image.bearingX = found->second.bearingX;
    image.bearingY = found->second.bearingY;

    gStatistics.hits += 1;
    return true;
}

static void EvictUntilUnder(size_t budget)
{
    while (gStatistics.bytesResident > budget && !gGlyphs.empty())
    {
        auto oldest = gGlyphs.begin();
        for (auto candidate = gGlyphs.begin(); candidate != gGlyphs.end(); ++candidate)
        {
            if (candidate->second.lastUsed < oldest->second.lastUsed) oldest = candidate;
        }

        gStatistics.bytesResident -= oldest->second.samples.size();
        gStatistics.entriesResident -= 1;
        gStatistics.evictions += 1;
        gGlyphs.erase(oldest);
    }
}

bool GlyphCache::Insert(uint64_t key, const FontStack::GlyphImage& image)
{
    if (!ShouldCache(image.width, image.height))
    {
        gStatistics.bypasses += 1;
        return false;
    }

    if (!image.samples) return false;

    size_t rowBytes = static_cast<size_t>(image.width);
    size_t total = rowBytes * static_cast<size_t>(image.height);

    CachedGlyph entry;
    entry.samples.resize(total);
    for (int32_t row = 0; row < image.height; ++row)
    {
        const uint8_t* source = image.samples + static_cast<ptrdiff_t>(row) * image.pitch;
        memcpy(entry.samples.data() + static_cast<size_t>(row) * rowBytes, source, rowBytes);
    }

    entry.width = image.width;
    entry.height = image.height;
    entry.pitch = image.width;
    entry.bearingX = image.bearingX;
    entry.bearingY = image.bearingY;
    entry.lastUsed = ++gClock;

    gStatistics.bytesResident += total;
    gStatistics.entriesResident += 1;
    gGlyphs[key] = std::move(entry);

    EvictUntilUnder(FontStack::MAX_GLYPH_CACHE_BYTES);
    return true;
}

void GlyphCache::Clear()
{
    gGlyphs.clear();
    gStatistics = Statistics{};
    gClock = 0;
}

GlyphCache::Statistics GlyphCache::Snapshot()
{
    return gStatistics;
}

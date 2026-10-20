#include "FontStack/GlyphCache.hpp"

#include <cstring>
#include <vector>

struct CachedShape
{
    uint32_t style = 0;
    bool rightToLeft = false;
    std::vector<uint16_t> text;
    uint64_t run = 0;
    uint64_t lastUsed = 0;
};

static std::vector<CachedShape> gShapes;
static ShapeCache::Statistics gStatistics;
static uint64_t gClock = 0;

static bool SameText(const CachedShape& entry, const uint16_t* text, int32_t count)
{
    if (entry.text.size() != static_cast<size_t>(count)) return false;
    if (count == 0) return true;
    return memcmp(entry.text.data(), text, static_cast<size_t>(count) * sizeof(uint16_t)) == 0;
}

bool ShapeCache::Lookup(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t& run)
{
    gStatistics.lookups += 1;
    if (count < 0) return false;

    for (CachedShape& entry : gShapes)
    {
        if (entry.style != style) continue;
        if (entry.rightToLeft != rightToLeft) continue;

        if (!SameText(entry, text, count))
        {
            gStatistics.collisionsRejected += 1;
            continue;
        }

        entry.lastUsed = ++gClock;
        run = entry.run;
        gStatistics.hits += 1;
        return true;
    }

    return false;
}

bool ShapeCache::Insert(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t run, uint64_t& evicted)
{
    evicted = FontStack::INVALID_RUN;
    if (count < 0) return false;

    while (gShapes.size() >= static_cast<size_t>(FontStack::MAX_SHAPE_CACHE_ENTRIES))
    {
        size_t oldest = 0;
        for (size_t index = 1; index < gShapes.size(); ++index)
        {
            if (gShapes[index].lastUsed < gShapes[oldest].lastUsed) oldest = index;
        }

        evicted = gShapes[oldest].run;
        gShapes.erase(gShapes.begin() + static_cast<ptrdiff_t>(oldest));
        gStatistics.evictions += 1;
    }

    CachedShape entry;
    entry.style = style;
    entry.rightToLeft = rightToLeft;
    entry.text.assign(text, text + count);
    entry.run = run;
    entry.lastUsed = ++gClock;

    gShapes.push_back(std::move(entry));
    gStatistics.entriesResident = static_cast<int32_t>(gShapes.size());
    return true;
}

void ShapeCache::Forget(uint64_t run)
{
    for (size_t index = 0; index < gShapes.size(); ++index)
    {
        if (gShapes[index].run != run) continue;

        gShapes.erase(gShapes.begin() + static_cast<ptrdiff_t>(index));
        gStatistics.entriesResident = static_cast<int32_t>(gShapes.size());
        return;
    }
}

void ShapeCache::DrainAll(uint64_t* out, int32_t capacity, int32_t& written)
{
    written = 0;
    if (!out || capacity <= 0) return;

    for (const CachedShape& entry : gShapes)
    {
        if (written >= capacity) break;
        out[written] = entry.run;
        written += 1;
    }
}

void ShapeCache::Clear()
{
    gShapes.clear();
    gStatistics = Statistics{};
    gClock = 0;
}

ShapeCache::Statistics ShapeCache::Snapshot()
{
    return gStatistics;
}

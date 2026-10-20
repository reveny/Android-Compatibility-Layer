#pragma once

#include <cstddef>
#include <cstdint>

#include "FontStack/FontStack.hpp"

namespace GlyphCache
{
    struct Statistics
    {
        uint64_t lookups = 0;
        uint64_t hits = 0;
        uint64_t evictions = 0;
        uint64_t bypasses = 0;
        size_t bytesResident = 0;
        int32_t entriesResident = 0;
    };

    uint64_t KeyFor(uint32_t style, uint32_t glyphId, int flags);

    bool Lookup(uint64_t key, FontStack::GlyphImage& image);
    bool Insert(uint64_t key, const FontStack::GlyphImage& image);
    bool ShouldCache(int32_t width, int32_t height);

    void Clear();
    Statistics Snapshot();
}

namespace ShapeCache
{
    struct Statistics
    {
        uint64_t lookups = 0;
        uint64_t hits = 0;
        uint64_t evictions = 0;
        uint64_t collisionsRejected = 0;
        int32_t entriesResident = 0;
    };

    bool Lookup(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t& run);
    bool Insert(uint32_t style, const uint16_t* text, int32_t count, bool rightToLeft, uint64_t run, uint64_t& evicted);

    void Forget(uint64_t run);
    void DrainAll(uint64_t* out, int32_t capacity, int32_t& written);

    void Clear();
    Statistics Snapshot();
}

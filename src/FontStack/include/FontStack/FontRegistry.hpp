#pragma once

#include <cstdint>

namespace FontRegistry
{
    inline constexpr int MAX_FAMILY_COUNT = 64;
    inline constexpr float AXIS_WEIGHT_MIN = 100.0f;
    inline constexpr float AXIS_WEIGHT_MAX = 900.0f;
    inline constexpr float AXIS_WIDTH_CONDENSED = 75.0f;
    inline constexpr float AXIS_WIDTH_NORMAL = 100.0f;

    enum class Substitution : uint8_t
    {
        Exact,
        WeightApproximated,
        FamilySubstituted,
        DefaultFamily
    };

    struct Selection
    {
        const char* fileName = nullptr;
        float weightAxis = AXIS_WEIGHT_MIN;
        float widthAxis = AXIS_WIDTH_NORMAL;
        float italicAxis = 0.0f;
        bool syntheticItalic = false;
        Substitution substitution = Substitution::Exact;
    };

    bool Resolve(const char* familyName, int weight, bool italic, Selection& selection);
    const char* SubstitutionName(Substitution substitution);
    int FamilyCount();
    const char* FamilyNameAt(int index);
}

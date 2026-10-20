#include "FontStack/FontRegistry.hpp"

#include <cstring>

#include "FontStack/FontStack.hpp"

struct FamilyEntry
{
    const char* name;
    const char* fileName;
    float weightAxis;
    float widthAxis;
    bool exactWeight;
    FontRegistry::Substitution substitution;
};

static const char* ROBOTO = "Roboto-Regular.ttf";
static const char* DROID_SANS_MONO = "DroidSansMono.ttf";

static const FamilyEntry FAMILIES[] = {
    {"sans-serif", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::Exact},
    {"arial", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"helvetica", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"tahoma", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"verdana", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"sans-serif-thin", ROBOTO, FontStack::WEIGHT_THIN, FontRegistry::AXIS_WIDTH_NORMAL, true, FontRegistry::Substitution::Exact},
    {"sans-serif-light", ROBOTO, FontStack::WEIGHT_LIGHT, FontRegistry::AXIS_WIDTH_NORMAL, true, FontRegistry::Substitution::Exact},
    {"sans-serif-medium", ROBOTO, FontStack::WEIGHT_MEDIUM, FontRegistry::AXIS_WIDTH_NORMAL, true, FontRegistry::Substitution::Exact},
    {"sans-serif-black", ROBOTO, FontStack::WEIGHT_BLACK, FontRegistry::AXIS_WIDTH_NORMAL, true, FontRegistry::Substitution::Exact},
    {"sans-serif-condensed", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_CONDENSED, false, FontRegistry::Substitution::Exact},
    {"sans-serif-condensed-light", ROBOTO, FontStack::WEIGHT_LIGHT, FontRegistry::AXIS_WIDTH_CONDENSED, true,
     FontRegistry::Substitution::Exact},
    {"sans-serif-condensed-medium", ROBOTO, FontStack::WEIGHT_MEDIUM, FontRegistry::AXIS_WIDTH_CONDENSED, true,
     FontRegistry::Substitution::Exact},
    {"monospace", DROID_SANS_MONO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::Exact},
    {"courier", DROID_SANS_MONO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false,
     FontRegistry::Substitution::FamilySubstituted},
    {"serif", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"times", ROBOTO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false, FontRegistry::Substitution::FamilySubstituted},
    {"serif-monospace", DROID_SANS_MONO, FontStack::WEIGHT_NORMAL, FontRegistry::AXIS_WIDTH_NORMAL, false,
     FontRegistry::Substitution::FamilySubstituted},
};

static const int FAMILY_COUNT = static_cast<int>(sizeof(FAMILIES) / sizeof(FAMILIES[0]));

static float ClampWeight(int weight)
{
    float value = static_cast<float>(weight);
    if (value < FontRegistry::AXIS_WEIGHT_MIN) return FontRegistry::AXIS_WEIGHT_MIN;
    if (value > FontRegistry::AXIS_WEIGHT_MAX) return FontRegistry::AXIS_WEIGHT_MAX;
    return value;
}

static bool SupportsVariations(const char* fileName)
{
    return strcmp(fileName, ROBOTO) == 0;
}

bool FontRegistry::Resolve(const char* familyName, int weight, bool italic, Selection& selection)
{
    const FamilyEntry* entry = &FAMILIES[0];
    Substitution substitution = Substitution::DefaultFamily;

    if (familyName && *familyName)
    {
        for (int index = 0; index < FAMILY_COUNT; ++index)
        {
            if (strcmp(FAMILIES[index].name, familyName) != 0) continue;

            entry = &FAMILIES[index];
            substitution = entry->substitution;
            break;
        }
    }

    selection.fileName = entry->fileName;
    selection.widthAxis = entry->widthAxis;
    selection.weightAxis = entry->exactWeight ? entry->weightAxis : ClampWeight(weight);

    if (!SupportsVariations(entry->fileName))
    {
        selection.weightAxis = FontStack::WEIGHT_NORMAL;
        selection.italicAxis = 0.0f;
        selection.syntheticItalic = italic;
        if (substitution == Substitution::Exact) substitution = Substitution::WeightApproximated;
        selection.substitution = substitution;
        return true;
    }

    selection.italicAxis = italic ? 1.0f : 0.0f;
    selection.syntheticItalic = false;
    selection.substitution = substitution;
    return true;
}

const char* FontRegistry::SubstitutionName(Substitution substitution)
{
    switch (substitution)
    {
        case Substitution::Exact: return "Exact";
        case Substitution::WeightApproximated: return "WeightApproximated";
        case Substitution::FamilySubstituted: return "FamilySubstituted";
        case Substitution::DefaultFamily: return "DefaultFamily";
    }

    return "Unknown";
}

int FontRegistry::FamilyCount()
{
    return FAMILY_COUNT;
}

const char* FontRegistry::FamilyNameAt(int index)
{
    if (index < 0 || index >= FAMILY_COUNT) return nullptr;
    return FAMILIES[index].name;
}

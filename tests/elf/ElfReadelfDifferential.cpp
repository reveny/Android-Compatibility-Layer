#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include "ElfLoader/ElfLoader.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

struct ReferenceImage
{
    uint64_t loadSegments = 0;
    uint64_t dynamicSymbols = 0;
    uint64_t relative = 0;
    uint64_t relrRelative = 0;
    uint64_t globalData = 0;
    uint64_t jumpSlot = 0;
    uint64_t absolute = 0;
    uint64_t threadLocal = 0;
    uint64_t unsupportedRelocations = 0;
    std::string soname;
    std::vector<std::string> needed;
};

static constexpr int MINIMUM_COMPARED_LIBRARIES = 3;

static int failures = 0;
static int compared = 0;
static int skipped = 0;

static void Check(bool condition, const std::string& label)
{
    if (condition)
    {
        printf("PASS %s\n", label.c_str());
        return;
    }

    printf("FAIL %s\n", label.c_str());
    failures += 1;
}

static void CheckEqual(uint64_t produced, uint64_t expected, const std::string& label)
{
    if (produced == expected)
    {
        printf("PASS %s (%llu)\n", label.c_str(), (unsigned long long)expected);
        return;
    }

    printf("FAIL %s: llvm-readelf says %llu, the loader says %llu\n", label.c_str(), (unsigned long long)expected,
           (unsigned long long)produced);
    failures += 1;
}

static bool RunReadelf(const std::string& readelf, const std::string& path, std::string& output)
{
    std::string command = readelf + " --program-headers --dynamic --relocs --dyn-syms '" + path + "' 2>/dev/null";

    FILE* pipe = popen(command.c_str(), "r");
    if (!pipe) return false;

    char chunk[4096];
    output.clear();
    while (fgets(chunk, sizeof(chunk), pipe)) output += chunk;

    return pclose(pipe) == 0 && !output.empty();
}

static std::string Trimmed(const std::string& text)
{
    size_t first = text.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) return std::string();

    size_t last = text.find_last_not_of(" \t\r\n");
    return text.substr(first, last - first + 1);
}

static uint64_t CountOccurrences(const std::string& text, const std::string& needle)
{
    uint64_t count = 0;
    size_t cursor = 0;

    for (;;)
    {
        size_t found = text.find(needle, cursor);
        if (found == std::string::npos) break;

        count += 1;
        cursor = found + needle.size();
    }

    return count;
}

static void ParseReference(const std::string& output, ReferenceImage& reference)
{
    size_t start = 0;
    bool inDynamicSymbols = false;

    while (start < output.size())
    {
        size_t end = output.find('\n', start);
        if (end == std::string::npos) end = output.size();

        std::string line = output.substr(start, end - start);
        std::string trimmed = Trimmed(line);
        start = end + 1;

        if (trimmed.rfind("Symbol table '.dynsym'", 0) == 0)
        {
            inDynamicSymbols = true;
            continue;
        }

        if (trimmed.empty())
        {
            inDynamicSymbols = false;
            continue;
        }

        if (inDynamicSymbols)
        {
            size_t colon = trimmed.find(':');
            if (colon != std::string::npos && colon > 0 && trimmed[0] >= '0' && trimmed[0] <= '9') reference.dynamicSymbols += 1;
            continue;
        }

        if (trimmed.rfind("LOAD ", 0) == 0) reference.loadSegments += 1;

        if (trimmed.find("(SONAME)") != std::string::npos)
        {
            size_t open = trimmed.find('[');
            size_t close = trimmed.find(']');
            if (open != std::string::npos && close != std::string::npos && close > open)
                reference.soname = trimmed.substr(open + 1, close - open - 1);
        }

        if (trimmed.find("(NEEDED)") != std::string::npos)
        {
            size_t open = trimmed.find('[');
            size_t close = trimmed.find(']');
            if (open != std::string::npos && close != std::string::npos && close > open)
                reference.needed.push_back(trimmed.substr(open + 1, close - open - 1));
        }

        if (trimmed.rfind("Relocation section '.relr.dyn'", 0) == 0)
        {
            size_t contains = trimmed.find("contains ");
            if (contains != std::string::npos) reference.relrRelative = strtoull(trimmed.c_str() + contains + 9, nullptr, 10);
        }
    }

    reference.relative = CountOccurrences(output, "R_AARCH64_RELATIVE");
    reference.globalData = CountOccurrences(output, "R_AARCH64_GLOB_DAT");
    reference.jumpSlot = CountOccurrences(output, "R_AARCH64_JUMP_SLOT");
    reference.absolute = CountOccurrences(output, "R_AARCH64_ABS64");

    reference.threadLocal = CountOccurrences(output, "R_AARCH64_TLSDESC") + CountOccurrences(output, "R_AARCH64_TLS_");

    uint64_t named = reference.relative + reference.globalData + reference.jumpSlot + reference.absolute + reference.threadLocal;
    uint64_t allAarch64 = CountOccurrences(output, "R_AARCH64_");
    reference.unsupportedRelocations = allAarch64 > named ? allAarch64 - named : 0;
}

static void CompareOne(const std::string& readelf, const std::string& path)
{
    printf("\n--- %s\n", path.c_str());

    std::string output;
    if (!RunReadelf(readelf, path, output))
    {
        Check(false, "llvm-readelf produced output for " + path);
        return;
    }

    ReferenceImage reference;
    ParseReference(output, reference);

    if (reference.unsupportedRelocations != 0)
    {
        printf("SKIP %s: it uses %llu relocations of types the loader does not implement yet\n", path.c_str(),
               (unsigned long long)reference.unsupportedRelocations);
        skipped += 1;
        return;
    }

    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.stubUnresolvedSymbols = true;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        Check(false, "the guest runtime started for " + path);
        return;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    GuestRuntime::Result loaded = GuestRuntime::Load(instance, path, moduleId);

    if (loaded != GuestRuntime::Result::Ok)
    {
        Check(false, "the loader accepted " + path);
        GuestRuntime::Destroy(instance);
        return;
    }

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(instance), moduleId);
    if (!library)
    {
        Check(false, "the loaded module has a handle for " + path);
        GuestRuntime::Destroy(instance);
        return;
    }

    CheckEqual(library->segments.size(), reference.loadSegments, "PT_LOAD segments");
    CheckEqual(library->exports.size(), reference.dynamicSymbols, "dynamic symbols");
    CheckEqual(library->relocationCounts.relative, reference.relative, "R_AARCH64_RELATIVE");
    CheckEqual(library->relocationCounts.relrRelative, reference.relrRelative, "RELR-expanded relative");
    CheckEqual(library->relocationCounts.globalData, reference.globalData, "R_AARCH64_GLOB_DAT");
    CheckEqual(library->relocationCounts.jumpSlot, reference.jumpSlot, "R_AARCH64_JUMP_SLOT");
    CheckEqual(library->relocationCounts.absolute, reference.absolute, "R_AARCH64_ABS64");
    CheckEqual(library->relocationCounts.threadLocal, reference.threadLocal, "TLS relocations");

    if (!reference.soname.empty()) Check(library->soname == reference.soname, "DT_SONAME is " + reference.soname);

    CheckEqual(library->dependencies.size(), reference.needed.size(), "DT_NEEDED entries");

    for (size_t index = 0; index < reference.needed.size() && index < library->dependencies.size(); ++index)
        Check(library->dependencies[index] == reference.needed[index], "DT_NEEDED[" + std::to_string(index) + "] is " + reference.needed[index]);

    compared += 1;
    GuestRuntime::Destroy(instance);
}

int main(int argc, char** argv)
{
    if (argc < 3)
    {
        fprintf(stderr, "usage: ElfReadelfDifferentialTest <llvm-readelf> <library.so>...\n");
        return 2;
    }

    std::string readelf = argv[1];

    for (int index = 2; index < argc; ++index) CompareOne(readelf, argv[index]);

    printf("\n%d libraries compared, %d skipped\n", compared, skipped);

    if (compared < MINIMUM_COMPARED_LIBRARIES)
    {
        printf("FAIL only %d libraries were fully compared, which is below the floor of %d - the differential is not proving anything\n",
               compared, MINIMUM_COMPARED_LIBRARIES);
        failures += 1;
    }

    printf("%s: %d checks failed\n", failures == 0 ? "elf-readelf-differential PASSED" : "elf-readelf-differential FAILED", failures);
    return failures == 0 ? 0 : 1;
}

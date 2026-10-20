#include "AheadOfTime/AheadOfTime.hpp"

#include <algorithm>
#include <cerrno>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <set>
#include <sys/stat.h>
#include <spawn.h>
#include <sys/wait.h>
#include <unistd.h>
#include <unordered_set>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "CodeMap/CodeMap.hpp"
#include "LlvmBackend/LlvmBackend.hpp"
#include "Settings/Settings.hpp"
#include "TranslationCache/TranslationCache.hpp"

extern char** environ;

static AheadOfTime::ProgressCallback gProgressCallback = nullptr;
static void* gProgressContext = nullptr;

void AheadOfTime::SetProgressCallback(AheadOfTime::ProgressCallback callback, void* context)
{
    gProgressCallback = callback;
    gProgressContext = context;
}

static void ReportProgress(const char* imagePath, uint64_t completed, uint64_t total)
{
    if (!gProgressCallback) return;

    gProgressCallback(gProgressContext, imagePath, completed, total);
}

struct BatchProgress
{
    const char* imagePath;
    uint64_t rangesBefore;
    uint64_t rangesInBatch;
    uint64_t rangesTotal;
};

static void ForwardChunkProgress(void* context, uint64_t completedChunks, uint64_t totalChunks)
{
    const BatchProgress* batch = (const BatchProgress*)context;
    if (!batch || totalChunks == 0) return;

    const uint64_t within = (batch->rangesInBatch * completedChunks) / totalChunks;

    ReportProgress(batch->imagePath, batch->rangesBefore + within, batch->rangesTotal);
}

struct AheadOfTimeImage
{
    void* handle = nullptr;
    std::string path;
};

static constexpr const char* LIBRARY_PATH_PREFIX = "LD_LIBRARY_PATH=";

static std::vector<std::string> LinkerEnvironment()
{
    std::vector<std::string> entries;

    const char* bundled = Settings::Lookup(AheadOfTime::LINKER_LIBRARY_PATH_SETTING);
    const bool overriding = bundled && bundled[0] != '\0';

    for (char** entry = environ; *entry; ++entry)
    {
        if (overriding && strncmp(*entry, LIBRARY_PATH_PREFIX, strlen(LIBRARY_PATH_PREFIX)) == 0) continue;
        entries.push_back(*entry);
    }

    if (!overriding) return entries;

    entries.push_back(std::string(LIBRARY_PATH_PREFIX) + bundled);

    return entries;
}

static bool RunLinker(const std::vector<std::string>& objectPaths, const std::string& libraryPath)
{
    if (objectPaths.empty()) return false;

    const char* configured = Settings::Lookup(AheadOfTime::LINKER_SETTING);
    std::string linker = configured && configured[0] != '\0' ? configured : AheadOfTime::DEFAULT_LINKER;

    std::string shared = "-shared";
    std::string flag = "-o";

    std::vector<char*> arguments;
    arguments.push_back((char*)linker.c_str());
    arguments.push_back((char*)shared.c_str());
    arguments.push_back((char*)flag.c_str());
    arguments.push_back((char*)libraryPath.c_str());
    for (const std::string& object : objectPaths) arguments.push_back((char*)object.c_str());
    arguments.push_back(nullptr);

    std::vector<std::string> environment = LinkerEnvironment();

    std::vector<char*> environmentPointers;
    environmentPointers.reserve(environment.size() + 1);
    for (std::string& entry : environment) environmentPointers.push_back((char*)entry.c_str());
    environmentPointers.push_back(nullptr);

    pid_t child = 0;
    int spawned = posix_spawnp(&child, arguments[0], nullptr, nullptr, arguments.data(), environmentPointers.data());
    if (spawned != 0)
    {
        fprintf(stderr, "[aheadoftime] cannot run %s: %s; pass --linker with one that accepts -shared\n", arguments[0],
                strerror(spawned));
        return false;
    }

    int state = 0;
    if (waitpid(child, &state, 0) < 0) return false;

    return WIFEXITED(state) && WEXITSTATUS(state) == 0;
}

static void RebuildRanges(const std::set<uint64_t>& starts, uint64_t executableEnd, std::vector<CodeMap::FunctionRange>& ranges)
{
    ranges.clear();
    ranges.reserve(starts.size());

    for (uint64_t start : starts)
    {
        CodeMap::FunctionRange range;
        range.startAddress = (GuestState::GuestAddress)start;
        ranges.push_back(range);
    }

    for (size_t index = 0; index < ranges.size(); ++index)
    {
        uint64_t end = index + 1 < ranges.size() ? (uint64_t)ranges[index + 1].startAddress : executableEnd;
        ranges[index].endAddress = (GuestState::GuestAddress)end;
    }
}

static constexpr uint64_t REPORTED_UNLOWERABLE = 8;

static void FillUncoveredCode(Arm64Decoder::Context& decoder, const CodeMap::FunctionRange& range, Ir::Function& function,
                              uint64_t& leftUncovered);

static AheadOfTime::Result DiscoverRanges(GuestRuntime::Instance& runtime, uint32_t moduleId, std::vector<CodeMap::FunctionRange>& ranges,
                                          std::vector<GuestState::GuestAddress>& translatable, uint64_t& trapsEmitted,
                                          uint64_t& unlowerable)
{
    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(runtime), moduleId);
    if (!library) return AheadOfTime::Result::DiscoveryFailed;

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(runtime);

    CodeMap::Statistics discovery;
    if (CodeMap::Discover(*library, memory, ranges, discovery) != CodeMap::Result::Ok) return AheadOfTime::Result::DiscoveryFailed;

    GuestState::GuestAddress executableStart{};
    GuestState::GuestAddress executableEnd{};
    if (CodeMap::ExecutableRange(*library, executableStart, executableEnd) != CodeMap::Result::Ok)
        return AheadOfTime::Result::DiscoveryFailed;

    std::set<uint64_t> starts;
    for (const CodeMap::FunctionRange& range : ranges) starts.insert((uint64_t)range.startAddress);

    Arm64Decoder::Context decoder;
    decoder.memory = memory;
    decoder.trapOnUndecodable = true;

    for (int round = 0; round < AheadOfTime::MAX_DISCOVERY_ROUNDS; ++round)
    {
        std::set<uint64_t> discovered;

        for (const CodeMap::FunctionRange& range : ranges)
        {
            Ir::Function function;
            if (Arm64Decoder::DecodeFunction(decoder, range.startAddress, range.endAddress, function) != Arm64Decoder::Result::Ok) continue;

            for (const Ir::Block& block : function.blocks)
            {
                for (const Ir::Instruction& instruction : block.instructions)
                {
                    if (instruction.opcode != Ir::Opcode::Call) continue;
                    if (instruction.immediate < (uint64_t)executableStart || instruction.immediate >= (uint64_t)executableEnd) continue;
                    if (starts.count(instruction.immediate) != 0) continue;

                    discovered.insert(instruction.immediate);
                }

                uint64_t successors[2] = {(uint64_t)block.successorTaken, (uint64_t)block.successorFallthrough};

                for (uint64_t successor : successors)
                {
                    if (successor == (uint64_t)Ir::NO_SUCCESSOR) continue;
                    if (successor >= (uint64_t)range.startAddress && successor < (uint64_t)range.endAddress) continue;
                    if (successor < (uint64_t)executableStart || successor >= (uint64_t)executableEnd) continue;
                    if (starts.count(successor) != 0) continue;

                    discovered.insert(successor);
                }
            }
        }

        if (discovered.empty()) break;

        starts.insert(discovered.begin(), discovered.end());
        RebuildRanges(starts, (uint64_t)executableEnd, ranges);
    }

    LlvmBackend::ReturnFreedMemory();

    translatable.clear();
    translatable.reserve(ranges.size());

    for (const CodeMap::FunctionRange& range : ranges)
    {
        Ir::Function function;
        if (Arm64Decoder::DecodeFunction(decoder, range.startAddress, range.endAddress, function) != Arm64Decoder::Result::Ok) continue;

        uint64_t ignoredUncovered = 0;
        FillUncoveredCode(decoder, range, function, ignoredUncovered);

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!LlvmBackend::CanLower(function, unsupported))
        {
            unlowerable += 1;
            if (unlowerable <= REPORTED_UNLOWERABLE)
                fprintf(stderr, "[aheadoftime] %016llx cannot be lowered because of %s, so it would be a hole in translation\n",
                        (unsigned long long)range.startAddress, Ir::OpcodeName(unsupported));
            continue;
        }

        translatable.push_back(range.startAddress);
    }

    trapsEmitted = decoder.statistics.trapsEmitted;
    LlvmBackend::ReturnFreedMemory();

    return translatable.empty() ? AheadOfTime::Result::NothingTranslated : AheadOfTime::Result::Ok;
}

static constexpr uint64_t MAX_GAP_DECODES = 65536;
static constexpr uint64_t MAX_TABLE_TARGETS = 256;

static bool FirstUncoveredAddress(const Ir::Function& function, uint64_t start, uint64_t end, uint64_t& uncovered)
{
    std::vector<std::pair<uint64_t, uint64_t>> covered;
    covered.reserve(function.blocks.size());

    for (const Ir::Block& block : function.blocks)
        covered.push_back(std::make_pair((uint64_t)block.guestAddress, (uint64_t)block.guestEndAddress));

    std::sort(covered.begin(), covered.end());

    uint64_t cursor = start;
    for (const std::pair<uint64_t, uint64_t>& span : covered)
    {
        if (span.first > cursor) break;
        if (span.second > cursor) cursor = span.second;
    }

    if (cursor >= end) return false;

    uncovered = cursor;
    return true;
}

static void RememberBase(const Arm64Decoder::Context& decoder, uint64_t candidate, uint64_t rangeStart, uint64_t rangeEnd,
                         std::unordered_set<uint64_t>& leaders)
{
    if ((candidate & 3) != 0) return;

    if (candidate >= rangeStart && candidate < rangeEnd)
    {
        for (uint64_t step = 0; step < MAX_TABLE_TARGETS; ++step)
        {
            uint64_t target = candidate + step * 4;
            if (target >= rangeEnd) break;

            leaders.insert(target);
        }

        return;
    }

    if (!decoder.memory.Read) return;

    for (uint64_t index = 0; index < MAX_TABLE_TARGETS; ++index)
    {
        int32_t entry = 0;
        if (!decoder.memory.Read(decoder.memory.context, (GuestState::GuestAddress)(candidate + index * 4), &entry, sizeof(entry))) return;

        uint64_t target = candidate + (uint64_t)(int64_t)entry;
        if ((target & 3) != 0) return;
        if (target < rangeStart || target >= rangeEnd) return;

        leaders.insert(target);
    }
}

static void CollectBranchTableLeaders(const Arm64Decoder::Context& decoder, const Ir::Block& block, uint64_t rangeStart, uint64_t rangeEnd,
                                      std::unordered_set<uint64_t>& leaders)
{
    uint64_t materialised[32] = {};
    bool known[32] = {};

    for (uint64_t address = (uint64_t)block.guestAddress; address < (uint64_t)block.guestEndAddress; address += 4)
    {
        uint32_t encoding = 0;
        if (!decoder.memory.Read ||
            !decoder.memory.Read(decoder.memory.context, (GuestState::GuestAddress)address, &encoding, sizeof(encoding)))
            break;

        uint32_t destination = encoding & 0x1f;

        if ((encoding & 0x1f000000) == 0x10000000)
        {
            uint64_t packed = ((uint64_t)((encoding >> 5) & 0x7ffff) << 2) | (uint64_t)((encoding >> 29) & 3);
            int64_t offset = (int64_t)(packed << 43) >> 43;

            if ((encoding >> 31) & 1) materialised[destination] = (address & ~0xfffull) + (uint64_t)(offset * 4096);
            else
                materialised[destination] = address + (uint64_t)offset;

            known[destination] = true;
            RememberBase(decoder, materialised[destination], rangeStart, rangeEnd, leaders);
            continue;
        }

        if ((encoding & 0xff800000) == 0x91000000)
        {
            uint32_t source = (encoding >> 5) & 0x1f;
            uint64_t immediate = (encoding >> 10) & 0xfff;
            if ((encoding >> 22) & 1) immediate <<= 12;

            if (known[source])
            {
                materialised[destination] = materialised[source] + immediate;
                known[destination] = true;
                RememberBase(decoder, materialised[destination], rangeStart, rangeEnd, leaders);
                continue;
            }
        }

        known[destination] = false;
    }
}

static void FillUncoveredCode(Arm64Decoder::Context& decoder, const CodeMap::FunctionRange& range, Ir::Function& function,
                              uint64_t& leftUncovered)
{
    bool indirect = false;
    for (const Ir::Block& block : function.blocks)
    {
        if (!block.endsWithIndirectBranch) continue;

        indirect = true;
        break;
    }

    std::unordered_set<uint64_t> leaders;
    if (indirect)
    {
        for (const Ir::Block& block : function.blocks)
            CollectBranchTableLeaders(decoder, block, (uint64_t)range.startAddress, (uint64_t)range.endAddress, leaders);
    }

    const std::unordered_set<uint64_t>* restored = decoder.blockLeaders;

    if (!leaders.empty())
    {
        decoder.blockLeaders = &leaders;

        Ir::Function split;
        if (Arm64Decoder::DecodeFunction(decoder, range.startAddress, range.endAddress, split) == Arm64Decoder::Result::Ok)
            function = std::move(split);
    }

    std::set<uint64_t> present;
    for (const Ir::Block& block : function.blocks) present.insert((uint64_t)block.guestAddress);

    uint64_t searchFrom = (uint64_t)range.startAddress;

    for (uint64_t attempt = 0; attempt < MAX_GAP_DECODES; ++attempt)
    {
        uint64_t uncovered = 0;
        if (!FirstUncoveredAddress(function, searchFrom, (uint64_t)range.endAddress, uncovered)) break;

        Ir::Function reached;
        if (Arm64Decoder::DecodeFunction(decoder, (GuestState::GuestAddress)uncovered, range.endAddress, reached) !=
            Arm64Decoder::Result::Ok)
        {
            searchFrom = uncovered + 4;
            continue;
        }

        bool grew = false;
        for (Ir::Block& block : reached.blocks)
        {
            if (!present.insert((uint64_t)block.guestAddress).second) continue;

            function.blocks.push_back(std::move(block));
            grew = true;
        }

        if (!grew) searchFrom = uncovered + 4;
    }

    uint64_t remaining = 0;
    if (FirstUncoveredAddress(function, (uint64_t)range.startAddress, (uint64_t)range.endAddress, remaining)) leftUncovered += 1;

    decoder.blockLeaders = restored;
}

static AheadOfTime::Result DecodeBatch(Arm64Decoder::Context& decoder, const std::vector<CodeMap::FunctionRange>& ranges, size_t from,
                                       size_t upTo, std::vector<Ir::Function>& functions, uint64_t& instructions, uint64_t& leftUncovered,
                                       uint64_t& undecodable)
{
    functions.clear();
    functions.reserve(upTo - from);
    instructions = 0;

    for (size_t index = from; index < upTo; ++index)
    {
        Ir::Function function;
        if (Arm64Decoder::DecodeFunction(decoder, ranges[index].startAddress, ranges[index].endAddress, function) !=
            Arm64Decoder::Result::Ok)
        {
            undecodable += 1;
            continue;
        }

        FillUncoveredCode(decoder, ranges[index], function, leftUncovered);

        for (const Ir::Block& block : function.blocks) instructions += block.instructions.size();

        functions.push_back(std::move(function));
    }

    return AheadOfTime::Result::Ok;
}

static uint64_t DecodeBudgetInstructions()
{
    uint64_t budget = AheadOfTime::DECODE_BUDGET_MEGABYTES;

    uint64_t instructions = (budget << 20) / AheadOfTime::BYTES_PER_GUEST_INSTRUCTION;
    return instructions < AheadOfTime::MINIMUM_BATCH_INSTRUCTIONS ? AheadOfTime::MINIMUM_BATCH_INSTRUCTIONS : instructions;
}

static AheadOfTime::Result BuildLibrary(GuestRuntime::Instance& runtime, const std::vector<CodeMap::FunctionRange>& ranges,
                                        const std::vector<GuestState::GuestAddress>& translatable, const std::string& libraryPath,
                                        AheadOfTime::Statistics& statistics)
{
    LlvmBackend::Configuration backendConfiguration;
    backendConfiguration.directMemoryAccess = true;

    GuestState::GuestAddress configuredArenaBase{};
    uint64_t configuredArenaLength = 0;
    GuestRuntime::ArenaBounds(runtime, configuredArenaBase, configuredArenaLength);
    backendConfiguration.arenaBase = (uint64_t)configuredArenaBase;
    backendConfiguration.arenaLength = configuredArenaLength;

    LlvmBackend::Session session;
    if (LlvmBackend::Initialize(backendConfiguration, session) != LlvmBackend::Result::Ok) return AheadOfTime::Result::EmitFailed;

    Arm64Decoder::Context decoder;
    decoder.memory = GuestRuntime::Memory(runtime);
    decoder.trapOnUndecodable = true;

    uint64_t batchBudget = DecodeBudgetInstructions();

    std::vector<std::string> objectPaths;
    std::vector<Ir::Function> functions;
    AheadOfTime::Result outcome = AheadOfTime::Result::Ok;

    size_t from = 0;
    uint64_t batch = 0;

    ReportProgress(libraryPath.c_str(), 0, ranges.size());

    while (from < ranges.size() && outcome == AheadOfTime::Result::Ok)
    {
        size_t upTo = from;
        uint64_t carried = 0;

        while (upTo < ranges.size())
        {
            uint64_t weight = ((uint64_t)ranges[upTo].endAddress - (uint64_t)ranges[upTo].startAddress) / 4;
            if (carried != 0 && carried + weight > batchBudget) break;

            carried += weight;
            upTo += 1;
        }

        uint64_t instructions = 0;
        DecodeBatch(decoder, ranges, from, upTo, functions, instructions, statistics.rangesLeftUncovered, statistics.functionsUndecodable);

        for (const Ir::Function& function : functions) statistics.blocksTranslated += function.blocks.size();

        if (!functions.empty())
        {
            BatchProgress progress = {libraryPath.c_str(), (uint64_t)from, (uint64_t)(upTo - from), (uint64_t)ranges.size()};
            LlvmBackend::SetChunkProgressCallback(&ForwardChunkProgress, &progress);

            std::string batchPath = libraryPath + ".b" + std::to_string((unsigned long long)batch);
            LlvmBackend::Result emitted = LlvmBackend::EmitObjectFiles(session, functions.data(), functions.size(), translatable.data(),
                                                                      translatable.size(), batchPath, objectPaths);

            LlvmBackend::SetChunkProgressCallback(nullptr, nullptr);

            if (emitted != LlvmBackend::Result::Ok)
            {
                fprintf(stderr, "[aheadoftime] cannot emit an object file: %s\n", LlvmBackend::ResultName(emitted));
                outcome = AheadOfTime::Result::EmitFailed;
            }
        }

        functions.clear();
        functions.shrink_to_fit();
        LlvmBackend::ReturnFreedMemory();

        from = upTo;
        batch += 1;

        ReportProgress(libraryPath.c_str(), from, ranges.size());
    }

    LlvmBackend::Statistics backendStatistics = LlvmBackend::GetStatistics(session);
    LlvmBackend::Shutdown(session);

    if (outcome != AheadOfTime::Result::Ok)
    {
        for (const std::string& object : objectPaths) remove(object.c_str());
        return outcome;
    }

    statistics.functionsDiscovered = ranges.size();
    statistics.functionsTranslated = backendStatistics.functionsCompiled;
    statistics.directCallsEmitted = backendStatistics.directCallsEmitted;

    bool linked = RunLinker(objectPaths, libraryPath);

    for (const std::string& object : objectPaths) remove(object.c_str());
    return linked ? AheadOfTime::Result::Ok : AheadOfTime::Result::LinkFailed;
}

AheadOfTime::Result AheadOfTime::Translate(GuestRuntime::Instance& runtime, uint32_t moduleId, const std::string& imagePath, Image& image,
                                           Statistics& statistics)
{
    statistics = Statistics();

    TranslationCache::Configuration cacheConfiguration;
    if (TranslationCache::DefaultRoot(cacheConfiguration.root) != TranslationCache::Result::Ok) return Result::CacheFailed;

    static std::once_flag sweepOnce;
    std::call_once(sweepOnce, [&cacheConfiguration]() {
        uint64_t removed = 0;
        if (TranslationCache::SweepAbandoned(cacheConfiguration, removed) == TranslationCache::Result::Ok && removed != 0)
            fprintf(stderr, "[aheadoftime] removed %llu object(s) left behind by translations that did not finish\n",
                    (unsigned long long)removed);
    });

    GuestState::GuestAddress arenaBase{};
    uint64_t arenaLength = 0;
    GuestRuntime::ArenaBounds(runtime, arenaBase, arenaLength);

    const ElfLoader::Library* placed = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(runtime), moduleId);
    uint64_t loadBias = placed ? placed->loadBias : 0;

    GuestState::GuestAddress executableStart{};
    GuestState::GuestAddress executableEnd{};
    if (placed) CodeMap::ExecutableRange(*placed, executableStart, executableEnd);

    char version[256];
    snprintf(version, sizeof(version), "%s-%s-%016llx-%016llx", TRANSLATOR_VERSION, LlvmBackend::HostCpuName().c_str(),
             (unsigned long long)arenaBase, (unsigned long long)loadBias);
    cacheConfiguration.runtimeVersion = version;

    std::string key;
    if (TranslationCache::KeyForFile(imagePath, cacheConfiguration.runtimeVersion, key) != TranslationCache::Result::Ok)
        return Result::CacheFailed;

    TranslationCache::Entry entry;
    bool cached = TranslationCache::Lookup(cacheConfiguration, key, entry) == TranslationCache::Result::Ok;

    std::vector<CodeMap::FunctionRange> ranges;
    std::vector<GuestState::GuestAddress> translatable;

    Result discovered = DiscoverRanges(runtime, moduleId, ranges, translatable, statistics.trapsEmitted, statistics.functionsUnlowerable);
    if (discovered != Result::Ok) return discovered;

    if (!cached)
    {
        if (mkdir(cacheConfiguration.root.c_str(), 0755) != 0 && errno != EEXIST)
        {
            fprintf(stderr, "[aheadoftime] cannot create the translation cache at %s\n", cacheConfiguration.root.c_str());
            return Result::CacheFailed;
        }

        std::string scratch = cacheConfiguration.root + "/" + key + ".building." + std::to_string((unsigned long long)getpid());

        Result built = BuildLibrary(runtime, ranges, translatable, scratch, statistics);
        LlvmBackend::ReturnFreedMemory();

        if (built != Result::Ok)
        {
            remove(scratch.c_str());
            return built;
        }

        TranslationCache::Result stored = TranslationCache::Adopt(cacheConfiguration, key, scratch, entry);
        if (stored != TranslationCache::Result::Ok)
        {
            remove(scratch.c_str());
            return Result::CacheFailed;
        }
    }

    statistics.cacheHit = cached;

    void* handle = dlopen(entry.path.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (!handle)
    {
        fprintf(stderr, "[aheadoftime] cannot load the translated image %s: %s\n", entry.path.c_str(), dlerror());
        return Result::LoadFailed;
    }

    AheadOfTimeImage* state = new AheadOfTimeImage();
    state->handle = handle;
    state->path = entry.path;


    Arm64Decoder::Context decoder;
    decoder.memory = GuestRuntime::Memory(runtime);
    decoder.trapOnUndecodable = true;

    for (GuestState::GuestAddress start : translatable)
    {
        std::string symbol = LlvmBackend::FunctionSymbol(start);

        void* hostEntry = dlsym(handle, symbol.c_str());
        if (!hostEntry) continue;

        auto found = std::lower_bound(ranges.begin(), ranges.end(), start,
                                      [](const CodeMap::FunctionRange& candidate, GuestState::GuestAddress wanted) {
                                          return candidate.startAddress < wanted;
                                      });

        if (found == ranges.end() || found->startAddress != start) continue;

        Ir::Function function;
        if (Arm64Decoder::DecodeFunction(decoder, found->startAddress, found->endAddress, function) != Arm64Decoder::Result::Ok) continue;

        uint64_t ignoredUncovered = 0;
        FillUncoveredCode(decoder, *found, function, ignoredUncovered);

        for (uint64_t index = 0; index < function.blocks.size(); ++index)
        {
            if (GuestRuntime::RegisterTranslatedBlock(runtime, function.blocks[index].guestAddress, hostEntry, (uint32_t)index) !=
                GuestRuntime::Result::Ok)
                continue;

            statistics.entriesResolved += 1;
        }
    }

    ranges.clear();
    ranges.shrink_to_fit();
    translatable.clear();
    translatable.shrink_to_fit();
    LlvmBackend::ReturnFreedMemory();

    if (statistics.entriesResolved == 0)
    {
        dlclose(handle);
        delete state;
        return Result::LoadFailed;
    }

    image.implementation = state;
    return Result::Ok;
}

void AheadOfTime::Release(Image& image)
{
    AheadOfTimeImage* state = (AheadOfTimeImage*)image.implementation;
    if (!state) return;

    if (state->handle) dlclose(state->handle);

    delete state;
    image.implementation = nullptr;
}

const char* AheadOfTime::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotInitialized: return "NotInitialized";
        case Result::DiscoveryFailed: return "DiscoveryFailed";
        case Result::NothingTranslated: return "NothingTranslated";
        case Result::EmitFailed: return "EmitFailed";
        case Result::LinkFailed: return "LinkFailed";
        case Result::CacheFailed: return "CacheFailed";
        case Result::LoadFailed: return "LoadFailed";
    }

    return "Unknown";
}

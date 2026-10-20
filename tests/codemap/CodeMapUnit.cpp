#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>
#include <unordered_set>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "CodeMap/CodeMap.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "LlvmBackend/LlvmBackend.hpp"

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: CodeMapUnitTest <libhelloworld.so>\n");
        return 2;
    }

    std::string path = argv[1];
    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.stubUnresolvedSymbols = true;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("codemap-unit FAILED: cannot create the guest runtime\n");
        return 1;
    }

    if (GuestRuntime::RegisterPlatformProxies(instance) != GuestRuntime::Result::Ok ||
        GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        printf("codemap-unit FAILED: cannot register the guest platform surface\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        printf("codemap-unit FAILED: cannot load %s\n", path.c_str());
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(instance), moduleId);
    if (!library)
    {
        printf("codemap-unit FAILED: the loaded module vanished\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);

    std::vector<CodeMap::FunctionRange> functions;
    CodeMap::Statistics statistics;
    CodeMap::Result discovered = CodeMap::Discover(*library, memory, functions, statistics);

    printf("discovery: %s, %llu functions (%llu from the exception table, %llu from symbols, %llu discarded) over %llu executable bytes\n",
           CodeMap::ResultName(discovered), (unsigned long long)functions.size(), (unsigned long long)statistics.fromExceptionTable,
           (unsigned long long)statistics.fromSymbols, (unsigned long long)statistics.discarded,
           (unsigned long long)statistics.executableBytes);

    Check(discovered == CodeMap::Result::Ok, "the executable range and function table were discovered");
    Check(statistics.exceptionTableUsed, "the exception table was parsed rather than silently skipped");
    Check(statistics.fromExceptionTable > statistics.fromSymbols,
          "the exception table found more functions than the dynamic symbol table, which is why it is the primary source");
    Check(functions.size() > 400, "a real library yields hundreds of function ranges");

    bool sorted = true;
    bool nonEmpty = true;
    bool contiguous = true;

    for (size_t index = 0; index < functions.size(); ++index)
    {
        if ((uint64_t)functions[index].endAddress <= (uint64_t)functions[index].startAddress) nonEmpty = false;
        if (index == 0) continue;

        if (functions[index].startAddress <= functions[index - 1].startAddress) sorted = false;
        if (functions[index - 1].endAddress != functions[index].startAddress) contiguous = false;
    }

    Check(sorted, "the ranges are sorted and free of duplicate starts");
    Check(nonEmpty, "no range is empty or inverted");
    Check(contiguous, "the ranges tile the executable image without gaps or overlaps");

    Arm64Decoder::Context context;
    context.memory = memory;
    context.trapOnUndecodable = true;

    std::map<std::string, uint64_t> rejectionCounts;
    std::map<std::string, uint64_t> unsupportedCounts;
    uint64_t lowerable = 0;
    uint64_t decoded = 0;
    uint64_t rejected = 0;
    uint64_t blocks = 0;
    uint64_t multiBlock = 0;
    bool blocksSorted = true;
    bool blocksOverlap = false;
    bool successorsResolve = true;

    for (const CodeMap::FunctionRange& range : functions)
    {
        Ir::Function function;
        Arm64Decoder::Result outcome = Arm64Decoder::DecodeFunction(context, range.startAddress, range.endAddress, function);

        if (outcome != Arm64Decoder::Result::Ok)
        {
            rejected += 1;
            rejectionCounts[Arm64Decoder::ResultName(outcome)] += 1;
            continue;
        }

        decoded += 1;
        blocks += function.blocks.size();
        if (function.blocks.size() > 1) multiBlock += 1;

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (LlvmBackend::CanLower(function, unsupported)) lowerable += 1;
        else unsupportedCounts[Ir::OpcodeName(unsupported)] += 1;

        std::unordered_set<uint64_t> starts;
        for (const Ir::Block& block : function.blocks) starts.insert((uint64_t)block.guestAddress);

        for (size_t index = 0; index < function.blocks.size(); ++index)
        {
            const Ir::Block& block = function.blocks[index];

            if (index > 0 && block.guestAddress <= function.blocks[index - 1].guestAddress) blocksSorted = false;
            if (index > 0 && function.blocks[index - 1].guestEndAddress > block.guestAddress) blocksOverlap = true;

            const GuestState::GuestAddress successors[2] = {block.successorTaken, block.successorFallthrough};
            for (GuestState::GuestAddress successor : successors)
            {
                if (successor == Ir::NO_SUCCESSOR) continue;

                uint64_t target = (uint64_t)successor;
                if (target < (uint64_t)range.startAddress || target >= (uint64_t)range.endAddress) continue;
                if (starts.count(target) == 0) successorsResolve = false;
            }
        }
    }

    printf("decode: %llu functions decoded, %llu rejected, %llu blocks total, %llu functions with internal control flow\n",
           (unsigned long long)decoded, (unsigned long long)rejected, (unsigned long long)blocks, (unsigned long long)multiBlock);

    for (const auto& entry : rejectionCounts)
        printf("  rejected %llu function(s) with %s\n", (unsigned long long)entry.second, entry.first.c_str());

    printf("  %llu trap(s) emitted for undecodable instructions\n", (unsigned long long)context.statistics.trapsEmitted);
    printf("lowering: %llu of %llu functions translate whole\n", (unsigned long long)lowerable, (unsigned long long)decoded);

    for (const auto& entry : unsupportedCounts)
        printf("  %llu function(s) blocked on %s\n", (unsigned long long)entry.second, entry.first.c_str());

    Check(decoded == functions.size(),
          "every discovered function decodes once undecodable instructions become traps rather than whole-function failures");
    Check(context.statistics.trapsEmitted > 0, "the trap path is exercised, so this library really does contain encodings we lack");
    Check(decoded > 0, "whole functions decode through the control-flow walker");
    Check(multiBlock > 0, "functions with internal control flow produce more than one block");
    Check(blocksSorted, "a decoded function's blocks come back sorted by guest address");
    Check(!blocksOverlap, "no two blocks in a function cover the same instruction, so a branch target really does split its block");
    Check(successorsResolve,
          "every in-range successor lands on a block start, so no branch targets the middle of another block");

    GuestRuntime::Destroy(instance);

    printf("%s: %d checks failed\n", failures == 0 ? "codemap-unit PASSED" : "codemap-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}

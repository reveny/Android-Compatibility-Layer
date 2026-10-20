#pragma once

#include <cstdint>
#include <vector>
#include <string>

#include "GuestState/GuestState.hpp"
#include "Interpreter/Interpreter.hpp"
#include "Ir/Ir.hpp"

namespace LlvmBackend
{
    inline constexpr uint64_t MINIMUM_FUNCTIONS_PER_CHUNK = 64;
    inline constexpr uint64_t TRACED_CHUNK_MULTIPLIER = 8;
    inline constexpr uint64_t BYTES_PER_LOWERED_INSTRUCTION = 16384;
    inline constexpr uint64_t DEFAULT_MEMORY_BUDGET_MEGABYTES = 4096;
    inline constexpr uint64_t MINIMUM_INSTRUCTIONS_PER_CHUNK = 512;
    enum class Tier : uint8_t
    {
        Baseline,
        Optimized
    };

    enum class Result : uint8_t
    {
        Ok,
        NotInitialized,
        LoweringFailed,
        VerificationFailed,
        CodegenFailed,
        OutOfMemory,
        IoFailed,
        UnsupportedOpcode,
        BadDescriptor
    };

    inline constexpr uint64_t BLOCK_TRACE_ENTRIES = 4096;

    struct RuntimeContext
    {
        GuestState::MemoryAccessor memory;
        void* hostCallContext = nullptr;
        uint64_t* blockTraceEntries = nullptr;
        uint64_t* blockTraceCursor = nullptr;
    };

    using EntryPoint = void (*)(GuestState::Registers* registers, RuntimeContext* context, Interpreter::ExitStatus* status,
                                uint32_t entryBlock);

    struct CompiledFunction
    {
        GuestState::GuestAddress guestEntryAddress{};
        void* hostEntryPoint = nullptr;
        uint64_t hostCodeSize = 0;
        Tier tier = Tier::Baseline;
        Result result = Result::Ok;
        void* release = nullptr;
    };

    using JniEnterGuest = uint64_t (*)(void* context, uint64_t guestFunction, const char* descriptor, const uint64_t* slots);

    struct JniThunkRequest
    {
        const char* descriptor = nullptr;
        uint64_t guestFunction = 0;
        JniEnterGuest enterGuest = nullptr;
        void* context = nullptr;
    };

    struct Configuration
    {
        Tier tier = Tier::Baseline;
        uint32_t threadCount = 1;
        bool emitIrToStderr = false;
        bool verifyModule = true;
        bool directMemoryAccess = false;
        bool traceBlocks = false;
        bool reportForeignAccess = false;
        uint64_t arenaBase = 0;
        uint64_t arenaLength = 0;
    };

    struct Statistics
    {
        uint64_t functionsCompiled = 0;
        uint64_t functionsRejected = 0;
        uint64_t functionsReleased = 0;
        uint64_t blocksLowered = 0;
        uint64_t instructionsLowered = 0;
        uint64_t directCallsEmitted = 0;
    };

    struct Session
    {
        void* implementation = nullptr;

        Session() = default;
        Session(const Session&) = delete;
        Session& operator=(const Session&) = delete;
    };

    Result Initialize(const Configuration& configuration, Session& session);
    void Shutdown(Session& session);
    void ReturnFreedMemory();
    uint64_t PeakResidentMegabytes();
    uint64_t ResidentMegabytes();

    Result CompileFunction(Session& session, const Ir::Function& function, Tier tier, CompiledFunction& compiled);
    Result CompileFunctions(Session& session, const Ir::Function* functions, uint64_t count, Tier tier, CompiledFunction* compiled,
                            uint64_t& failureCount);

    Result EmitJniThunk(Session& session, const JniThunkRequest& request, CompiledFunction& compiled);

    void ReleaseFunction(Session& session, CompiledFunction& compiled);

    const char* CodeGenLevelTag();

    using ChunkProgressCallback = void (*)(void* context, uint64_t completedChunks, uint64_t totalChunks);

    void SetChunkProgressCallback(ChunkProgressCallback callback, void* context);

    Result EmitObjectFile(Session& session, const Ir::Function* functions, uint64_t count, const std::string& outputPath);
    Result EmitObjectFiles(Session& session, const Ir::Function* functions, uint64_t count, const GuestState::GuestAddress* knownEntries,
                           uint64_t knownEntryCount, const std::string& basePath, std::vector<std::string>& objectPaths);
    std::string FunctionSymbol(GuestState::GuestAddress entryAddress);
    const std::string& HostCpuName();
    uint64_t ForeignAccessCount();

    bool CanLower(const Ir::Function& function, Ir::Opcode& firstUnsupported);
    bool CanLowerInstruction(const Ir::Instruction& instruction);
    Statistics GetStatistics(const Session& session);

    const char* ResultName(Result result);
    const char* TierName(Tier tier);
}

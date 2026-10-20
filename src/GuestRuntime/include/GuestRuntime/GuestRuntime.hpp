#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "AbiBridge/AbiBridge.hpp"
#include "ElfLoader/ElfLoader.hpp"
#include "GuestState/GuestState.hpp"
#include "LlvmBackend/LlvmBackend.hpp"

namespace GuestRuntime
{
    inline constexpr uint64_t DEFAULT_ARENA_BASE = 0x0000700000000000ull;
    inline constexpr const char* IMAGE_PLACEMENT_SETTING = "image-placement";
    inline constexpr uint64_t IMAGE_SLOT_BYTES = 0x0000000004000000ull;
    inline constexpr uint64_t IMAGE_SLOT_COUNT = 128;
    inline constexpr uint64_t IMAGE_REGION_LENGTH = IMAGE_SLOT_BYTES * IMAGE_SLOT_COUNT;
    inline constexpr uint64_t DEFAULT_ARENA_LENGTH = IMAGE_REGION_LENGTH + 0x0000000100000000ull;
    inline constexpr uint64_t DEFAULT_STUB_REGION_LENGTH = 0x0000000000100000ull;
    inline constexpr uint64_t DEFAULT_STACK_BYTES = 8ull * 1024 * 1024;
    inline constexpr uint64_t DEFAULT_STEP_LIMIT = 2000000000ull;
    inline constexpr uint64_t SENTINEL_RETURN_ADDRESS = 0x00000000DEAD0000ull;

    enum class Result : uint8_t
    {
        Ok,
        ArenaUnavailable,
        OutOfGuestMemory,
        LoadFailed,
        DecodeFailed,
        GuestFaulted,
        HostCallFailed,
        StepLimitReached,
        NoSuchSymbol,
        ThreadCreationFailed
    };

    struct Configuration
    {
        std::vector<std::string> searchPaths;
        uint64_t arenaBase = DEFAULT_ARENA_BASE;
        uint64_t arenaLength = DEFAULT_ARENA_LENGTH;
        uint64_t stubRegionLength = DEFAULT_STUB_REGION_LENGTH;
        uint64_t stackBytes = DEFAULT_STACK_BYTES;
        uint64_t stepLimit = DEFAULT_STEP_LIMIT;
        bool traceGuest = false;
        bool requireTranslated = false;
        bool traceBlocks = false;
        bool stubUnresolvedSymbols = false;
        bool recompile = false;
        LlvmBackend::Tier tier = LlvmBackend::Tier::Baseline;
    };

    struct Statistics
    {
        uint64_t blocksDecoded = 0;
        uint64_t blocksExecuted = 0;
        uint64_t hostCalls = 0;
        uint64_t threadsStarted = 0;
        uint64_t syscalls = 0;
        uint64_t blocksRecompiled = 0;
        uint64_t blocksExecutedNative = 0;
        uint64_t codeWritesObserved = 0;
        uint64_t blocksInterpreted = 0;
        uint64_t blocksExecutedTranslated = 0;
        uint64_t trapsHit = 0;
    };

    struct Instance
    {
        void* implementation = nullptr;

        Instance() = default;
        Instance(const Instance&) = delete;
        Instance& operator=(const Instance&) = delete;
    };

    Result Create(const Configuration& configuration, Instance& instance);
    void Destroy(Instance& instance);

    ElfLoader::Namespace& LibraryNamespace(Instance& instance);
    AbiBridge::Registry& BridgeRegistry(Instance& instance);
    const GuestState::MemoryAccessor& Memory(const Instance& instance);
    GuestState::GuestCaller Caller(Instance& instance);
    const Statistics& GetStatistics(const Instance& instance);

    Result Reserve(Instance& instance, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base);
    Result ReserveScratch(Instance& instance, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base);
    Result Commit(Instance& instance, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable);
    Result Protect(Instance& instance, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable);
    void* HostPointer(Instance& instance, GuestState::GuestAddress address, uint64_t length);
    void ArenaBounds(const Instance& instance, GuestState::GuestAddress& base, uint64_t& length);

    Result RegisterPlatformProxies(Instance& instance);
    Result RegisterDataObject(Instance& instance, const std::string& symbolName, uint64_t sizeInBytes, GuestState::GuestAddress& address);

    struct Description
    {
        std::string soname;
        std::string path;
        std::string symbolName;
        uint64_t moduleBase = 0;
        uint64_t symbolAddress = 0;
    };

    Result DescribeAddress(Instance& instance, uint64_t address, Description& description);

    struct Module
    {
        std::string path;
        uint64_t loadBias = 0;
        uint64_t programHeaderAddress = 0;
        uint64_t programHeaderCount = 0;
        uint64_t threadLocalModuleId = 0;
        uint64_t loadEventCount = 0;
    };

    void DescribeModules(Instance& instance, std::vector<Module>& modules);

    Result Load(Instance& instance, const std::string& sonameOrPath, uint32_t& moduleId);
    Result RunInitializers(Instance& instance, uint32_t moduleId);
    Result Lookup(Instance& instance, const std::string& symbolName, GuestState::GuestAddress& address);

    Result CallGuest(Instance& instance, GuestState::GuestAddress entryAddress, GuestState::Registers& registers);

    Result CallGuestNested(Instance& instance, const GuestState::Registers& caller, GuestState::GuestAddress entryAddress,
                           const uint64_t* arguments, uint32_t argumentCount, uint64_t& returned);

    bool OnGuestThread();

    bool CurrentGuestStack(uint64_t& base, uint64_t& bytes);

    using FaultHandler = bool (*)(void* context, int signalNumber, void* information);

    void SetFaultHandler(FaultHandler handler, void* context);

    Result CallGuestOnCurrentThread(GuestState::GuestAddress entryAddress, const uint64_t* arguments, uint32_t argumentCount,
                                    uint64_t& returned);

    Result AllocateStack(Instance& instance, uint64_t bytes, GuestState::GuestAddress& stackTop);
    Result AllocateThreadPointer(Instance& instance, GuestState::GuestAddress& threadPointer);

    uint64_t CurrentCallToken();

    using ThreadExitHandler = void (*)(void* context);

    void SetThreadExitHandler(Instance& instance, ThreadExitHandler handler, void* context);

    Result StartThread(Instance& instance, GuestState::GuestAddress entryAddress, uint64_t argument, uint64_t& handle);
    Result JoinThread(Instance& instance, uint64_t handle, uint64_t& returnValue);

    Result RegisterTranslatedBlock(Instance& instance, GuestState::GuestAddress blockAddress, void* hostEntry, uint32_t entryBlock);

    void InvalidateBlocks(Instance& instance, GuestState::GuestAddress address, uint64_t length);

    const char* ResultName(Result result);
}

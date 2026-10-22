#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "GuestState/GuestState.hpp"

namespace ElfLoader
{
    inline constexpr uint32_t INVALID_MODULE_ID = 0xFFFFFFFFu;
    inline constexpr uint64_t NO_TLS_OFFSET = 0xFFFFFFFFFFFFFFFFull;
    inline constexpr uint32_t MAX_MODULES = 256;
    inline constexpr uint64_t STATIC_TLS_RESERVED_BYTES = 16;
    inline constexpr uint64_t MINIMUM_ENTRY_BYTES = 4;
    inline constexpr const char* THREAD_LOCAL_RESOLVER_SYMBOL = "__tlsdesc_resolve";

    enum class Result : uint8_t
    {
        Ok,
        FileNotFound,
        NotAnElf,
        WrongMachine,
        WrongClass,
        MalformedElf,
        UnsupportedRelocation,
        DependencyNotFound,
        SymbolNotFound,
        MappingFailed,
        InitializerFaulted,
        TooManyModules,
        UnknownModule,
        UnresolvedDataObject
    };

    struct Segment
    {
        GuestState::GuestAddress guestAddress{};
        uint64_t length = 0;
        bool readable = false;
        bool writable = false;
        bool executable = false;
    };

    struct StubRegion
    {
        GuestState::GuestAddress guestAddress{};
        uint64_t length = 0;
        uint64_t entryBytes = 0;
    };

    struct Symbol
    {
        std::string name;
        GuestState::GuestAddress guestAddress{};
        uint64_t size = 0;
        bool isFunction = false;
        bool isObject = false;
        bool isWeak = false;
        bool isLocal = false;
        bool isDefined = false;
    };

    struct ThreadLocalStorage
    {
        GuestState::GuestAddress imageAddress{};
        uint64_t imageSize = 0;
        uint64_t blockSize = 0;
        uint64_t alignment = 0;
        uint64_t staticOffsetFromThreadPointer = NO_TLS_OFFSET;
        uint32_t moduleId = INVALID_MODULE_ID;
        bool present = false;
    };

    struct RelocationCounts
    {
        uint64_t relative = 0;
        uint64_t relrRelative = 0;
        uint64_t globalData = 0;
        uint64_t jumpSlot = 0;
        uint64_t absolute = 0;
        uint64_t threadLocal = 0;
        uint64_t resolvedToHostStub = 0;
        uint64_t resolvedToHostData = 0;
        uint64_t resolvedWeakToZero = 0;
        uint64_t packed = 0;
    };

    struct Library
    {
        std::string soname;
        std::string path;
        uint32_t moduleId = INVALID_MODULE_ID;
        uint64_t loadBias = 0;
        GuestState::GuestAddress reservationAddress{};
        uint64_t reservationLength = 0;
        std::vector<Segment> segments;
        std::vector<Symbol> exports;
        std::vector<std::string> dependencies;
        std::vector<uint32_t> dependencyModuleIds;
        GuestState::GuestAddress preinitArrayAddress{};
        uint64_t preinitArrayCount = 0;
        GuestState::GuestAddress initAddress{};
        GuestState::GuestAddress initArrayAddress{};
        uint64_t initArrayCount = 0;
        GuestState::GuestAddress finiAddress{};
        GuestState::GuestAddress finiArrayAddress{};
        uint64_t finiArrayCount = 0;
        GuestState::GuestAddress ehFrameAddress{};
        uint64_t ehFrameLength = 0;
        std::vector<StubRegion> stubRegions;
        GuestState::GuestAddress programHeaderAddress{};
        uint64_t programHeaderCount = 0;
        ThreadLocalStorage threadLocalStorage;
        RelocationCounts relocationCounts;
        bool initializersRun = false;
    };

    struct HostResolver
    {
        bool (*ResolveToGuestStub)(void* context, const char* name, GuestState::GuestAddress& guestStubAddress) = nullptr;
        bool (*LookupDefinedStub)(void* context, const char* name, GuestState::GuestAddress& guestStubAddress) = nullptr;
        bool (*ResolveDataObject)(void* context, const char* name, GuestState::GuestAddress& guestAddress) = nullptr;
        bool (*IsProxyLibrary)(void* context, const char* soname) = nullptr;
        void* context = nullptr;
    };

    struct AddressSpace
    {
        bool (*Reserve)(void* context, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base) = nullptr;
        bool (*ReserveForImage)(void* context, const char* name, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base) =
            nullptr;
        bool (*Commit)(void* context, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable) = nullptr;
        bool (*Protect)(void* context, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable) = nullptr;
        bool (*Release)(void* context, GuestState::GuestAddress address, uint64_t length) = nullptr;
        void* context = nullptr;
    };

    struct Configuration
    {
        std::vector<std::string> searchPaths;
        HostResolver hostResolver;
        AddressSpace addressSpace;
        GuestState::MemoryAccessor memory;
    };

    struct Namespace
    {
        Configuration configuration;
        std::vector<Library> loaded;
        uint64_t staticTlsBytes = STATIC_TLS_RESERVED_BYTES;
        uint32_t nextModuleId = 0;
    };

    Result CreateNamespace(const Configuration& configuration, Namespace& libraryNamespace);
    void DestroyNamespace(Namespace& libraryNamespace);

    Result LoadLibrary(Namespace& libraryNamespace, const std::string& sonameOrPath, uint32_t& moduleId);
    Result LoadFromMemory(Namespace& libraryNamespace, const void* data, uint64_t length, const std::string& soname, uint32_t& moduleId);
    Result UnloadLibrary(Namespace& libraryNamespace, uint32_t moduleId);

    Library* FindLibrary(Namespace& libraryNamespace, uint32_t moduleId);
    const Library* FindLibrary(const Namespace& libraryNamespace, uint32_t moduleId);

    Result RunInitializers(Namespace& libraryNamespace, uint32_t moduleId, const GuestState::GuestCaller& caller);
    Result RunFinalizers(Namespace& libraryNamespace, uint32_t moduleId, const GuestState::GuestCaller& caller);

    Result LookupExport(const Namespace& libraryNamespace, const std::string& name, GuestState::GuestAddress& address);
    Result LookupExportIn(const Namespace& libraryNamespace, uint32_t moduleId, const std::string& name, GuestState::GuestAddress& address);

    const char* ResultName(Result result);
}

#pragma once

#include <cstdint>
#include <string>

#include "GuestState/GuestState.hpp"

namespace AbiBridge
{
    inline constexpr int MAX_ARGUMENTS = 32;
    inline constexpr uint32_t ARGUMENT_SLOT_BYTES = 16;

    static_assert(MAX_ARGUMENTS <= 255, "totalArgumentCount is a uint8_t and must be able to hold MAX_ARGUMENTS");

    enum class ArgumentClass : uint8_t
    {
        Invalid,
        Integer,
        Pointer,
        Float,
        Double,
        LongDouble,
        Vector,
        Aggregate,
        HomogeneousFloatAggregate,
        HomogeneousVectorAggregate
    };

    enum class EightbyteClass : uint8_t
    {
        None,
        Integer,
        SseFloat,
        X87,
        X87Up,
        Memory
    };

    enum class Result : uint8_t
    {
        Ok,
        UnknownSymbol,
        UnsupportedSignature,
        LibraryNotFound,
        LibraryUnreadable,
        AlreadyRegistered,
        GuestFaulted,
        StubRegionExhausted,
        MalformedArguments,
        NotAFunction,
        NotDataSymbol
    };

    struct Argument
    {
        ArgumentClass argumentClass = ArgumentClass::Invalid;
        uint32_t sizeInBytes = 0;
        uint32_t alignmentInBytes = 0;
        uint8_t memberCount = 0;
        EightbyteClass eightbytes[2] = {EightbyteClass::None, EightbyteClass::None};
    };

    struct Signature
    {
        Argument returnValue;
        Argument arguments[MAX_ARGUMENTS];
        uint8_t namedArgumentCount = 0;
        uint8_t totalArgumentCount = 0;
        bool variadic = false;
    };

    struct ArgumentBuffer
    {
        const void* slots = nullptr;
        uint32_t slotCount = 0;
        uint32_t slotStrideBytes = ARGUMENT_SLOT_BYTES;
    };

    using GuestCallHandler = Result (*)(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory);

    struct Trampoline
    {
        std::string symbolName;
        Signature signature;
        void* hostFunction = nullptr;
        GuestCallHandler customHandler = nullptr;
        void* customContext = nullptr;
        GuestState::GuestAddress guestStubAddress{};
    };

    struct DataSymbol
    {
        std::string symbolName;
        GuestState::GuestAddress guestAddress{};
        uint64_t sizeInBytes = 0;
    };

    struct RegistryConfiguration
    {
        GuestState::GuestAddress stubRegionBase{};
        uint64_t stubRegionLength = 0;
    };

    struct Registry
    {
        void* implementation = nullptr;

        Registry() = default;
        Registry(const Registry&) = delete;
        Registry& operator=(const Registry&) = delete;
    };

    Result CreateRegistry(const RegistryConfiguration& configuration, Registry& registry);
    void DestroyRegistry(Registry& registry);

    Result RegisterHostFunction(Registry& registry, const std::string& symbolName, const Signature& signature, void* hostFunction);
    Result RegisterHostHandler(Registry& registry, const std::string& symbolName, GuestCallHandler handler, void* context);
    Result RegisterHostData(Registry& registry, const std::string& symbolName, GuestState::GuestAddress guestAddress, uint64_t sizeInBytes);
    Result RegisterProxyLibrary(Registry& registry, const std::string& soname);

    bool IsProxyLibrary(const Registry& registry, const std::string& soname);
    bool IsGuestStub(const Registry& registry, GuestState::GuestAddress address);

    Result LookupGuestStub(const Registry& registry, const std::string& symbolName, GuestState::GuestAddress& guestStubAddress);
    Result LookupGuestData(const Registry& registry, const std::string& symbolName, GuestState::GuestAddress& guestAddress);
    const Trampoline* LookupTrampoline(const Registry& registry, GuestState::GuestAddress guestStubAddress);

    Result CallHostFromGuest(const Registry& registry, GuestState::GuestAddress guestStubAddress, GuestState::Registers& registers,
                             const GuestState::MemoryAccessor& memory);
    Result CallGuestFromHost(const GuestState::GuestCaller& caller, GuestState::GuestAddress guestAddress, const Signature& signature,
                             const ArgumentBuffer& arguments, void* returnValue, uint32_t returnValueBytes);

    const char* ResultName(Result result);
}

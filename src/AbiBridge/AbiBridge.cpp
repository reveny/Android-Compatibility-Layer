#include "AbiBridge/AbiBridge.hpp"

#include <cstring>
#include <deque>
#include <string>
#include <unordered_map>
#include <vector>

static constexpr uint64_t STUB_STRIDE_BYTES = 16;
static constexpr int UNIVERSAL_INTEGER_SLOTS = 16;
static constexpr int UNIVERSAL_VECTOR_SLOTS = 8;
static constexpr int GENERAL_ARGUMENT_REGISTERS = 8;
static constexpr uint64_t STACK_ARGUMENT_SLOT_BYTES = 8;

struct AbiBridgeRegistry
{
    std::deque<AbiBridge::Trampoline> trampolines;
    std::deque<AbiBridge::DataSymbol> dataSymbols;
    std::unordered_map<std::string, size_t> trampolineByName;
    std::unordered_map<std::string, size_t> dataByName;
    std::vector<std::string> proxySonames;
    uint64_t stubBase = 0;
    uint64_t stubLength = 0;
    uint64_t stubCursor = 0;
};

using HostUniversalInteger = uint64_t (*)(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t,
                                          uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, double, double, double, double, double,
                                          double, double, double);
using HostUniversalDouble = double (*)(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t,
                                       uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, double, double, double, double, double,
                                       double, double, double);
using HostUniversalFloat = float (*)(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t,
                                     uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, double, double, double, double, double,
                                     double, double, double);

static AbiBridgeRegistry* Implementation(const AbiBridge::Registry& registry)
{
    return (AbiBridgeRegistry*)registry.implementation;
}

static bool IsIntegerClass(AbiBridge::ArgumentClass argumentClass)
{
    return argumentClass == AbiBridge::ArgumentClass::Integer || argumentClass == AbiBridge::ArgumentClass::Pointer;
}

static bool IsFloatClass(AbiBridge::ArgumentClass argumentClass)
{
    return argumentClass == AbiBridge::ArgumentClass::Float || argumentClass == AbiBridge::ArgumentClass::Double;
}

static bool ClassifyArguments(const AbiBridge::Signature& signature, int& integerCount, int& floatCount)
{
    integerCount = 0;
    floatCount = 0;

    if (signature.totalArgumentCount > AbiBridge::MAX_ARGUMENTS) return false;
    if (signature.namedArgumentCount > signature.totalArgumentCount) return false;

    for (uint8_t index = 0; index < signature.totalArgumentCount; ++index)
    {
        const AbiBridge::Argument& argument = signature.arguments[index];
        if (IsIntegerClass(argument.argumentClass))
        {
            integerCount += 1;
            continue;
        }
        if (IsFloatClass(argument.argumentClass))
        {
            floatCount += 1;
            continue;
        }
        return false;
    }

    AbiBridge::ArgumentClass returnClass = signature.returnValue.argumentClass;
    if (returnClass == AbiBridge::ArgumentClass::Invalid) return true;

    return IsIntegerClass(returnClass) || IsFloatClass(returnClass);
}

static bool SignatureIsSupported(const AbiBridge::Signature& signature)
{
    int integerCount = 0;
    int floatCount = 0;
    return ClassifyArguments(signature, integerCount, floatCount);
}

static bool SignatureIsDirectlyCallable(const AbiBridge::Signature& signature)
{
    int integerCount = 0;
    int floatCount = 0;
    if (!ClassifyArguments(signature, integerCount, floatCount)) return false;
    if (signature.variadic) return false;
    if (integerCount > UNIVERSAL_INTEGER_SLOTS) return false;
    if (floatCount > UNIVERSAL_VECTOR_SLOTS) return false;

    return true;
}

static double DoubleFromRawBits(uint64_t raw)
{
    double value = 0.0;
    memcpy(&value, &raw, sizeof(value));
    return value;
}

AbiBridge::Result AbiBridge::CreateRegistry(const RegistryConfiguration& configuration, Registry& registry)
{
    AbiBridgeRegistry* implementation = new AbiBridgeRegistry();
    implementation->stubBase = (uint64_t)configuration.stubRegionBase;
    implementation->stubLength = configuration.stubRegionLength;
    implementation->stubCursor = (uint64_t)configuration.stubRegionBase;

    registry.implementation = implementation;
    return Result::Ok;
}

void AbiBridge::DestroyRegistry(Registry& registry)
{
    delete Implementation(registry);
    registry.implementation = nullptr;
}

AbiBridge::Result AbiBridge::RegisterHostFunction(Registry& registry, const std::string& symbolName, const Signature& signature,
                                                  void* hostFunction)
{
    AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::UnknownSymbol;
    if (!hostFunction) return Result::UnknownSymbol;
    if (!SignatureIsDirectlyCallable(signature)) return Result::UnsupportedSignature;

    if (implementation->trampolineByName.count(symbolName) != 0) return Result::AlreadyRegistered;
    if (implementation->dataByName.count(symbolName) != 0) return Result::AlreadyRegistered;

    if (implementation->stubCursor + STUB_STRIDE_BYTES > implementation->stubBase + implementation->stubLength)
        return Result::StubRegionExhausted;

    Trampoline trampoline;
    trampoline.symbolName = symbolName;
    trampoline.signature = signature;
    trampoline.hostFunction = hostFunction;
    trampoline.guestStubAddress = (GuestState::GuestAddress)implementation->stubCursor;

    implementation->stubCursor += STUB_STRIDE_BYTES;
    implementation->trampolineByName.emplace(symbolName, implementation->trampolines.size());
    implementation->trampolines.push_back(std::move(trampoline));

    return Result::Ok;
}

AbiBridge::Result AbiBridge::RegisterHostHandler(Registry& registry, const std::string& symbolName, GuestCallHandler handler, void* context)
{
    AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::UnknownSymbol;
    if (!handler) return Result::UnknownSymbol;

    if (implementation->trampolineByName.count(symbolName) != 0) return Result::AlreadyRegistered;
    if (implementation->dataByName.count(symbolName) != 0) return Result::AlreadyRegistered;

    if (implementation->stubCursor + STUB_STRIDE_BYTES > implementation->stubBase + implementation->stubLength)
        return Result::StubRegionExhausted;

    Trampoline trampoline;
    trampoline.symbolName = symbolName;
    trampoline.customHandler = handler;
    trampoline.customContext = context;
    trampoline.guestStubAddress = (GuestState::GuestAddress)implementation->stubCursor;

    implementation->stubCursor += STUB_STRIDE_BYTES;
    implementation->trampolineByName.emplace(symbolName, implementation->trampolines.size());
    implementation->trampolines.push_back(std::move(trampoline));

    return Result::Ok;
}

AbiBridge::Result AbiBridge::RegisterHostData(Registry& registry, const std::string& symbolName, GuestState::GuestAddress guestAddress,
                                              uint64_t sizeInBytes)
{
    AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::UnknownSymbol;
    if (sizeInBytes == 0) return Result::MalformedArguments;

    if (implementation->trampolineByName.count(symbolName) != 0) return Result::AlreadyRegistered;
    if (implementation->dataByName.count(symbolName) != 0) return Result::AlreadyRegistered;

    DataSymbol symbol;
    symbol.symbolName = symbolName;
    symbol.guestAddress = guestAddress;
    symbol.sizeInBytes = sizeInBytes;

    implementation->dataByName.emplace(symbolName, implementation->dataSymbols.size());
    implementation->dataSymbols.push_back(std::move(symbol));

    return Result::Ok;
}

AbiBridge::Result AbiBridge::RegisterProxyLibrary(Registry& registry, const std::string& soname)
{
    AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::LibraryNotFound;

    for (const std::string& existing : implementation->proxySonames)
    {
        if (existing == soname) return Result::AlreadyRegistered;
    }

    implementation->proxySonames.push_back(soname);
    return Result::Ok;
}

bool AbiBridge::IsProxyLibrary(const Registry& registry, const std::string& soname)
{
    const AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return false;

    for (const std::string& existing : implementation->proxySonames)
    {
        if (existing == soname) return true;
    }

    return false;
}

bool AbiBridge::IsGuestStub(const Registry& registry, GuestState::GuestAddress address)
{
    const AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return false;

    uint64_t raw = (uint64_t)address;
    return raw >= implementation->stubBase && raw < implementation->stubBase + implementation->stubLength;
}

AbiBridge::Result AbiBridge::LookupGuestStub(const Registry& registry, const std::string& symbolName, GuestState::GuestAddress& guestStubAddress)
{
    const AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::UnknownSymbol;

    auto found = implementation->trampolineByName.find(symbolName);
    if (found == implementation->trampolineByName.end())
    {
        if (implementation->dataByName.count(symbolName) != 0) return Result::NotAFunction;
        return Result::UnknownSymbol;
    }

    guestStubAddress = implementation->trampolines[found->second].guestStubAddress;
    return Result::Ok;
}

AbiBridge::Result AbiBridge::LookupGuestData(const Registry& registry, const std::string& symbolName, GuestState::GuestAddress& guestAddress)
{
    const AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return Result::UnknownSymbol;

    auto found = implementation->dataByName.find(symbolName);
    if (found == implementation->dataByName.end())
    {
        if (implementation->trampolineByName.count(symbolName) != 0) return Result::NotDataSymbol;
        return Result::UnknownSymbol;
    }

    guestAddress = implementation->dataSymbols[found->second].guestAddress;
    return Result::Ok;
}

const AbiBridge::Trampoline* AbiBridge::LookupTrampoline(const Registry& registry, GuestState::GuestAddress guestStubAddress)
{
    const AbiBridgeRegistry* implementation = Implementation(registry);
    if (!implementation) return nullptr;

    uint64_t raw = (uint64_t)guestStubAddress;
    if (raw < implementation->stubBase) return nullptr;

    uint64_t offset = raw - implementation->stubBase;
    if (offset % STUB_STRIDE_BYTES != 0) return nullptr;

    uint64_t index = offset / STUB_STRIDE_BYTES;
    if (index >= implementation->trampolines.size()) return nullptr;

    return &implementation->trampolines[index];
}

static bool ReadStackArgument(const GuestState::Registers& registers, const GuestState::MemoryAccessor& memory, uint64_t& stackOffset,
                              uint64_t& value)
{
    if (!memory.Read) return false;

    GuestState::GuestAddress address = (GuestState::GuestAddress)(registers.stackPointer + stackOffset);
    if (!memory.Read(memory.context, address, &value, sizeof(value))) return false;

    stackOffset += STACK_ARGUMENT_SLOT_BYTES;
    return true;
}

AbiBridge::Result AbiBridge::CallHostFromGuest(const Registry& registry, GuestState::GuestAddress guestStubAddress,
                                               GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    const Trampoline* trampoline = LookupTrampoline(registry, guestStubAddress);
    if (!trampoline) return Result::UnknownSymbol;

    if (trampoline->customHandler) return trampoline->customHandler(trampoline->customContext, registers, memory);

    const Signature& signature = trampoline->signature;
    if (!SignatureIsDirectlyCallable(signature)) return Result::UnsupportedSignature;
    if (!trampoline->hostFunction) return Result::NotAFunction;

    uint64_t integers[UNIVERSAL_INTEGER_SLOTS] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    double vectors[UNIVERSAL_VECTOR_SLOTS] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    int integerCount = 0;
    int vectorCount = 0;

    uint64_t stackOffset = 0;

    for (uint8_t index = 0; index < signature.totalArgumentCount; ++index)
    {
        const Argument& argument = signature.arguments[index];

        if (IsFloatClass(argument.argumentClass))
        {
            uint64_t raw = 0;

            if (vectorCount < GuestState::VECTOR_REGISTER_COUNT && vectorCount < UNIVERSAL_VECTOR_SLOTS)
            {
                raw = registers.vector[vectorCount].low;
            }
            else if (!ReadStackArgument(registers, memory, stackOffset, raw))
            {
                return Result::MalformedArguments;
            }

            if (vectorCount >= UNIVERSAL_VECTOR_SLOTS) return Result::UnsupportedSignature;

            vectors[vectorCount] = DoubleFromRawBits(raw);
            vectorCount += 1;
            continue;
        }

        uint64_t value = 0;

        if (integerCount < GENERAL_ARGUMENT_REGISTERS)
        {
            value = registers.general[integerCount];
        }
        else if (!ReadStackArgument(registers, memory, stackOffset, value))
        {
            return Result::MalformedArguments;
        }

        if (argument.sizeInBytes == 4 && argument.argumentClass == ArgumentClass::Integer) value = (uint64_t)(uint32_t)value;

        integers[integerCount] = value;
        integerCount += 1;
    }

    ArgumentClass returnClass = signature.returnValue.argumentClass;

    if (returnClass == ArgumentClass::Double)
    {
        HostUniversalDouble function = (HostUniversalDouble)trampoline->hostFunction;
        double returned = function(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5], integers[6], integers[7],
                                   integers[8], integers[9], integers[10], integers[11], integers[12], integers[13], integers[14],
                                   integers[15], vectors[0], vectors[1], vectors[2], vectors[3], vectors[4], vectors[5], vectors[6],
                                   vectors[7]);

        uint64_t raw = 0;
        memcpy(&raw, &returned, sizeof(raw));
        registers.vector[0].low = raw;
        registers.vector[0].high = 0;
        return Result::Ok;
    }

    if (returnClass == ArgumentClass::Float)
    {
        HostUniversalFloat function = (HostUniversalFloat)trampoline->hostFunction;
        float returned = function(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5], integers[6], integers[7],
                                  integers[8], integers[9], integers[10], integers[11], integers[12], integers[13], integers[14],
                                  integers[15], vectors[0], vectors[1], vectors[2], vectors[3], vectors[4], vectors[5], vectors[6],
                                  vectors[7]);

        uint32_t raw = 0;
        memcpy(&raw, &returned, sizeof(raw));
        registers.vector[0].low = raw;
        registers.vector[0].high = 0;
        return Result::Ok;
    }

    HostUniversalInteger function = (HostUniversalInteger)trampoline->hostFunction;
    uint64_t returned = function(integers[0], integers[1], integers[2], integers[3], integers[4], integers[5], integers[6], integers[7],
                                 integers[8], integers[9], integers[10], integers[11], integers[12], integers[13], integers[14],
                                 integers[15], vectors[0], vectors[1], vectors[2], vectors[3], vectors[4], vectors[5], vectors[6],
                                 vectors[7]);

    if (IsIntegerClass(returnClass))
    {
        if (signature.returnValue.sizeInBytes == 4) returned = (uint64_t)(uint32_t)returned;
        registers.general[0] = returned;
    }

    return Result::Ok;
}

AbiBridge::Result AbiBridge::CallGuestFromHost(const GuestState::GuestCaller& caller, GuestState::GuestAddress guestAddress,
                                               const Signature& signature, const ArgumentBuffer& arguments, void* returnValue,
                                               uint32_t returnValueBytes)
{
    if (!caller.EnterGuest) return Result::GuestFaulted;
    if (!SignatureIsSupported(signature)) return Result::UnsupportedSignature;
    if (signature.totalArgumentCount != 0 && !arguments.slots) return Result::MalformedArguments;
    if (arguments.slotCount < signature.totalArgumentCount) return Result::MalformedArguments;
    if (arguments.slotStrideBytes < sizeof(uint64_t)) return Result::MalformedArguments;

    GuestState::Registers registers;
    GuestState::Reset(registers);

    int integerCount = 0;
    int floatCount = 0;
    std::vector<uint64_t> stackWords;

    for (uint8_t index = 0; index < signature.totalArgumentCount; ++index)
    {
        const Argument& argument = signature.arguments[index];
        const uint8_t* slot = (const uint8_t*)arguments.slots + (uint64_t)index * arguments.slotStrideBytes;

        uint64_t raw = 0;
        memcpy(&raw, slot, sizeof(raw));

        if (IsIntegerClass(argument.argumentClass))
        {
            if (integerCount < UNIVERSAL_INTEGER_SLOTS)
            {
                registers.general[integerCount] = raw;
                integerCount += 1;
                continue;
            }

            stackWords.push_back(raw);
            continue;
        }

        if (floatCount < UNIVERSAL_VECTOR_SLOTS)
        {
            registers.vector[floatCount].low = raw;
            registers.vector[floatCount].high = 0;
            floatCount += 1;
            continue;
        }

        stackWords.push_back(raw);
    }

    if (!stackWords.empty())
    {
        if (!caller.EnterGuestWithStack) return Result::UnsupportedSignature;
        if (!caller.EnterGuestWithStack(caller.context, guestAddress, registers, stackWords.data(), (uint32_t)stackWords.size()))
            return Result::GuestFaulted;
    }
    else if (!caller.EnterGuest(caller.context, guestAddress, registers))
        return Result::GuestFaulted;

    if (returnValue && returnValueBytes != 0)
    {
        ArgumentClass returnClass = signature.returnValue.argumentClass;
        uint64_t raw = IsFloatClass(returnClass) ? registers.vector[0].low : registers.general[0];

        uint32_t bytes = returnValueBytes < sizeof(raw) ? returnValueBytes : (uint32_t)sizeof(raw);
        memcpy(returnValue, &raw, bytes);
    }

    return Result::Ok;
}

const char* AbiBridge::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::UnknownSymbol: return "UnknownSymbol";
        case Result::UnsupportedSignature: return "UnsupportedSignature";
        case Result::LibraryNotFound: return "LibraryNotFound";
        case Result::LibraryUnreadable: return "LibraryUnreadable";
        case Result::AlreadyRegistered: return "AlreadyRegistered";
        case Result::GuestFaulted: return "GuestFaulted";
        case Result::StubRegionExhausted: return "StubRegionExhausted";
        case Result::MalformedArguments: return "MalformedArguments";
        case Result::NotAFunction: return "NotAFunction";
        case Result::NotDataSymbol: return "NotDataSymbol";
    }

    return "Unknown";
}

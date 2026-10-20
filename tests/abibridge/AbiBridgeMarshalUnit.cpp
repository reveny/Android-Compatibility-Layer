#include <cstdio>
#include <cstring>
#include <vector>

#include "AbiBridge/AbiBridge.hpp"

static int failures = 0;

static void Expect(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static constexpr uint64_t STUB_BASE = 0x0000700000000000ull;
static constexpr uint64_t STUB_LENGTH = 0x10000;
static constexpr uint64_t STACK_BASE = 0x0000700000100000ull;
static constexpr uint64_t STACK_BYTES = 4096;

static std::vector<uint8_t> gStack(STACK_BYTES, 0);

static bool ReadStack(void*, GuestState::GuestAddress address, void* destination, uint64_t length)
{
    uint64_t offset = (uint64_t)address - STACK_BASE;
    if (offset + length > STACK_BYTES) return false;

    memcpy(destination, gStack.data() + offset, length);
    return true;
}

static bool WriteStack(void*, GuestState::GuestAddress address, const void* source, uint64_t length)
{
    uint64_t offset = (uint64_t)address - STACK_BASE;
    if (offset + length > STACK_BYTES) return false;

    memcpy(gStack.data() + offset, source, length);
    return true;
}

static float gClearRed = 0.0f;
static float gClearGreen = 0.0f;
static float gClearBlue = 0.0f;
static float gClearAlpha = 0.0f;

static void ClearColour(float red, float green, float blue, float alpha)
{
    gClearRed = red;
    gClearGreen = green;
    gClearBlue = blue;
    gClearAlpha = alpha;
}

static int gBlit[10] = {};

static void BlitFramebuffer(int a, int b, int c, int d, int e, int f, unsigned int g, unsigned int h, int i, int j)
{
    gBlit[0] = a;
    gBlit[1] = b;
    gBlit[2] = c;
    gBlit[3] = d;
    gBlit[4] = e;
    gBlit[5] = f;
    gBlit[6] = (int)g;
    gBlit[7] = (int)h;
    gBlit[8] = i;
    gBlit[9] = j;
}

static float gMixedFloat = 0.0f;
static uint64_t gMixedPointer = 0;

static void MixedArguments(uint64_t p0, uint64_t p1, uint64_t p2, uint64_t p3, uint64_t p4, uint64_t p5, uint64_t p6, uint64_t p7,
                           uint64_t p8, float value)
{
    (void)p0;
    (void)p1;
    (void)p2;
    (void)p3;
    (void)p4;
    (void)p5;
    (void)p6;
    (void)p7;

    gMixedPointer = p8;
    gMixedFloat = value;
}

static AbiBridge::Argument IntegerArgument()
{
    AbiBridge::Argument argument;
    argument.argumentClass = AbiBridge::ArgumentClass::Integer;
    argument.sizeInBytes = sizeof(uint64_t);
    argument.alignmentInBytes = sizeof(uint64_t);
    return argument;
}

static AbiBridge::Argument FloatArgument()
{
    AbiBridge::Argument argument;
    argument.argumentClass = AbiBridge::ArgumentClass::Float;
    argument.sizeInBytes = sizeof(float);
    argument.alignmentInBytes = sizeof(float);
    return argument;
}

static uint64_t FloatBits(float value)
{
    uint32_t raw = 0;
    memcpy(&raw, &value, sizeof(raw));
    return (uint64_t)raw;
}

static GuestState::MemoryAccessor StackAccessor()
{
    GuestState::MemoryAccessor memory;
    memory.Read = &ReadStack;
    memory.Write = &WriteStack;
    memory.context = nullptr;
    return memory;
}

static void CheckFloatArgumentsSurvive(AbiBridge::Registry& registry)
{
    AbiBridge::Signature signature;
    for (int index = 0; index < 4; ++index) signature.arguments[index] = FloatArgument();
    signature.totalArgumentCount = 4;
    signature.namedArgumentCount = 4;

    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, "ClearColour", signature, (void*)&ClearColour);
    Expect(registered == AbiBridge::Result::Ok, "a four float entry point registers");

    GuestState::GuestAddress stub{};
    Expect(AbiBridge::LookupGuestStub(registry, "ClearColour", stub) == AbiBridge::Result::Ok, "the float entry point has a stub");

    GuestState::Registers registers;
    registers.stackPointer = STACK_BASE;
    registers.vector[0].low = FloatBits(0.25f);
    registers.vector[1].low = FloatBits(-1.5f);
    registers.vector[2].low = FloatBits(3.75f);
    registers.vector[3].low = FloatBits(1.0f);

    GuestState::MemoryAccessor memory = StackAccessor();
    AbiBridge::Result called = AbiBridge::CallHostFromGuest(registry, stub, registers, memory);

    Expect(called == AbiBridge::Result::Ok, "the float entry point is callable");
    Expect(gClearRed == 0.25f, "the first float arrives unchanged");
    Expect(gClearGreen == -1.5f, "a negative float arrives unchanged");
    Expect(gClearBlue == 3.75f, "the third float arrives unchanged");
    Expect(gClearAlpha == 1.0f, "the fourth float arrives unchanged");
}

static void CheckStackArgumentsAreRead(AbiBridge::Registry& registry)
{
    AbiBridge::Signature signature;
    for (int index = 0; index < 10; ++index) signature.arguments[index] = IntegerArgument();
    signature.totalArgumentCount = 10;
    signature.namedArgumentCount = 10;

    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, "BlitFramebuffer", signature, (void*)&BlitFramebuffer);
    Expect(registered == AbiBridge::Result::Ok, "a ten integer entry point registers");

    GuestState::GuestAddress stub{};
    Expect(AbiBridge::LookupGuestStub(registry, "BlitFramebuffer", stub) == AbiBridge::Result::Ok, "the ten integer entry point has a stub");

    GuestState::Registers registers;
    registers.stackPointer = STACK_BASE;
    for (int index = 0; index < 8; ++index) registers.general[index] = (uint64_t)(index + 1);

    uint64_t ninth = 9;
    uint64_t tenth = 10;
    memcpy(gStack.data(), &ninth, sizeof(ninth));
    memcpy(gStack.data() + 8, &tenth, sizeof(tenth));

    GuestState::MemoryAccessor memory = StackAccessor();
    AbiBridge::Result called = AbiBridge::CallHostFromGuest(registry, stub, registers, memory);

    Expect(called == AbiBridge::Result::Ok, "the ten integer entry point is callable");

    bool registerArgumentsArrived = true;
    for (int index = 0; index < 8; ++index) registerArgumentsArrived = registerArgumentsArrived && gBlit[index] == index + 1;

    Expect(registerArgumentsArrived, "the first eight integers arrive from the register file");
    Expect(gBlit[8] == 9, "the ninth integer is read off the guest stack");
    Expect(gBlit[9] == 10, "the tenth integer is read off the guest stack");
}

static void CheckMixedArgumentsKeepTheirFiles(AbiBridge::Registry& registry)
{
    AbiBridge::Signature signature;
    for (int index = 0; index < 9; ++index) signature.arguments[index] = IntegerArgument();
    signature.arguments[9] = FloatArgument();
    signature.totalArgumentCount = 10;
    signature.namedArgumentCount = 10;

    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, "MixedArguments", signature, (void*)&MixedArguments);
    Expect(registered == AbiBridge::Result::Ok, "a mixed entry point registers");

    GuestState::GuestAddress stub{};
    Expect(AbiBridge::LookupGuestStub(registry, "MixedArguments", stub) == AbiBridge::Result::Ok, "the mixed entry point has a stub");

    GuestState::Registers registers;
    registers.stackPointer = STACK_BASE;
    for (int index = 0; index < 8; ++index) registers.general[index] = (uint64_t)(index + 1);
    registers.vector[0].low = FloatBits(0.5f);

    uint64_t ninth = 0xfeedfaceull;
    memcpy(gStack.data(), &ninth, sizeof(ninth));

    GuestState::MemoryAccessor memory = StackAccessor();
    AbiBridge::Result called = AbiBridge::CallHostFromGuest(registry, stub, registers, memory);

    Expect(called == AbiBridge::Result::Ok, "the mixed entry point is callable");
    Expect(gMixedPointer == 0xfeedfaceull, "the ninth integer comes off the stack while a float stays in the vector file");
    Expect(gMixedFloat == 0.5f, "the float argument is not displaced by the stacked integer");
}

int main()
{
    AbiBridge::RegistryConfiguration configuration;
    configuration.stubRegionBase = (GuestState::GuestAddress)STUB_BASE;
    configuration.stubRegionLength = STUB_LENGTH;

    AbiBridge::Registry registry;
    if (AbiBridge::CreateRegistry(configuration, registry) != AbiBridge::Result::Ok)
    {
        printf("FAIL the registry could not be created\n");
        return 1;
    }

    CheckFloatArgumentsSurvive(registry);
    CheckStackArgumentsAreRead(registry);
    CheckMixedArgumentsKeepTheirFiles(registry);

    AbiBridge::DestroyRegistry(registry);

    printf("%s: %d failure(s)\n", failures == 0 ? "abibridge-marshal PASSED" : "abibridge-marshal FAILED", failures);
    return failures == 0 ? 0 : 1;
}

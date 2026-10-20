#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "LlvmBackend/LlvmBackend.hpp"

struct FunctionCase
{
    const char* name;
    uint64_t arguments[4];
    int argumentCount;
};

static const FunctionCase FUNCTION_CASES[] = {
    {"SumTo", {0}, 1},
    {"SumTo", {1}, 1},
    {"SumTo", {97}, 1},
    {"CollatzSteps", {27}, 1},
    {"CollatzSteps", {1}, 1},
    {"CollatzSteps", {97}, 1},
    {"GreatestCommonDivisor", {1071, 462}, 2},
    {"GreatestCommonDivisor", {17, 0}, 2},
    {"GreatestCommonDivisor", {0, 13}, 2},
    {"Branchy", {5, 9}, 2},
    {"Branchy", {9, 5}, 2},
    {"Branchy", {7, 7}, 2},
    {"NestedLoops", {13}, 1},
    {"NestedLoops", {0}, 1},
    {"BitWeights", {0x0123456789abcdefull}, 1},
    {"BitWeights", {0}, 1},
    {"BitWeights", {0xffffffffffffffffull}, 1},
    {"ShiftMix", {0x123456789abcdefull, 0x2b}, 2},
    {"ShiftMix", {0xffffffffffffffffull, 3}, 2},
    {"SelectChain", {5, 19, 2}, 3},
    {"SelectChain", {0xfffffffffffffff0ull, 4, 4}, 3},
    {"CountdownDivide", {1000000}, 1},
    {"CountdownDivide", {0}, 1},
    {"CountdownDivide", {0xffffffffffffffffull}, 1},
};

static constexpr int FUNCTION_CASE_COUNT = (int)(sizeof(FUNCTION_CASES) / sizeof(FUNCTION_CASES[0]));
static constexpr uint64_t STACK_BYTES = 1u << 20;
static constexpr int SKIP_EXIT_CODE = 77;

static int failures = 0;
static int compared = 0;

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

static void ReportDifference(const GuestState::Registers& expected, const GuestState::Registers& produced)
{
    for (int index = 0; index < 31; ++index)
    {
        if (expected.general[index] == produced.general[index]) continue;
        printf("       x%-2d interpreted=%016llx translated=%016llx\n", index, (unsigned long long)expected.general[index],
               (unsigned long long)produced.general[index]);
    }

    if (expected.stackPointer != produced.stackPointer)
        printf("       sp  interpreted=%016llx translated=%016llx\n", (unsigned long long)expected.stackPointer,
               (unsigned long long)produced.stackPointer);

    if (expected.flagsNzcv != produced.flagsNzcv)
        printf("       nzcv interpreted=%08x translated=%08x\n", expected.flagsNzcv, produced.flagsNzcv);

    const uint8_t* left = (const uint8_t*)&expected;
    const uint8_t* right = (const uint8_t*)&produced;

    for (size_t offset = 0; offset < sizeof(GuestState::Registers); ++offset)
    {
        if (left[offset] == right[offset]) continue;
        printf("       byte %zu interpreted=%02x translated=%02x\n", offset, left[offset], right[offset]);
    }
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: FunctionDifferentialTest <libfunctions.so>\n");
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
        printf("function-differential FAILED: cannot create the guest runtime\n");
        return 1;
    }

    if (GuestRuntime::RegisterPlatformProxies(instance) != GuestRuntime::Result::Ok ||
        GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        printf("function-differential FAILED: cannot register the guest platform surface\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        printf("function-differential FAILED: cannot load %s\n", path.c_str());
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::GuestAddress stackTop{};
    if (GuestRuntime::AllocateStack(instance, STACK_BYTES, stackTop) != GuestRuntime::Result::Ok)
    {
        printf("function-differential FAILED: cannot allocate a guest stack\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::GuestAddress threadPointer{};
    if (GuestRuntime::AllocateThreadPointer(instance, threadPointer) != GuestRuntime::Result::Ok)
    {
        printf("function-differential FAILED: cannot allocate a guest thread pointer\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    LlvmBackend::Configuration backendConfiguration;
    backendConfiguration.directMemoryAccess = true;

    LlvmBackend::Session session;
    if (LlvmBackend::Initialize(backendConfiguration, session) != LlvmBackend::Result::Ok)
    {
        printf("function-differential FAILED: cannot start the translator\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    Arm64Decoder::Context decoder;
    decoder.memory = GuestRuntime::Memory(instance);
    decoder.trapOnUndecodable = true;

    uint64_t multiBlock = 0;
    uint64_t totalBlocks = 0;

    for (int index = 0; index < FUNCTION_CASE_COUNT; ++index)
    {
        const FunctionCase& testCase = FUNCTION_CASES[index];

        GuestState::GuestAddress entry{};
        if (GuestRuntime::Lookup(instance, testCase.name, entry) != GuestRuntime::Result::Ok)
        {
            printf("FAIL the fixture exports %s\n", testCase.name);
            failures += 1;
            continue;
        }

        GuestState::Registers interpreted;
        GuestState::Reset(interpreted);
        interpreted.stackPointer = (uint64_t)stackTop;
        interpreted.threadPointer = (uint64_t)threadPointer;
        for (int argument = 0; argument < testCase.argumentCount; ++argument)
            interpreted.general[argument] = testCase.arguments[argument];

        GuestState::Registers translated = interpreted;

        if (GuestRuntime::CallGuest(instance, entry, interpreted) != GuestRuntime::Result::Ok)
        {
            printf("FAIL %s interpreted cleanly\n", testCase.name);
            failures += 1;
            continue;
        }

        Ir::Function function;
        if (Arm64Decoder::DecodeFunction(decoder, entry, (GuestState::GuestAddress)((uint64_t)entry + 4096), function) !=
            Arm64Decoder::Result::Ok)
        {
            printf("FAIL %s decodes as a whole function\n", testCase.name);
            failures += 1;
            continue;
        }

        totalBlocks += function.blocks.size();
        if (function.blocks.size() > 1) multiBlock += 1;

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!LlvmBackend::CanLower(function, unsupported))
        {
            printf("FAIL %s is lowerable (first unsupported opcode %s)\n", testCase.name, Ir::OpcodeName(unsupported));
            failures += 1;
            continue;
        }

        LlvmBackend::CompiledFunction compiled;
        LlvmBackend::Result built = LlvmBackend::CompileFunction(session, function, LlvmBackend::Tier::Baseline, compiled);
        if (built != LlvmBackend::Result::Ok)
        {
            printf("FAIL %s translates (%s)\n", testCase.name, LlvmBackend::ResultName(built));
            failures += 1;
            continue;
        }

        translated.general[30] = GuestRuntime::SENTINEL_RETURN_ADDRESS;

        LlvmBackend::RuntimeContext runtime;
        runtime.memory = GuestRuntime::Memory(instance);

        Interpreter::ExitStatus status;
        memset(&status, 0, sizeof(status));

        ((LlvmBackend::EntryPoint)compiled.hostEntryPoint)(&translated, &runtime, &status, 0);
        LlvmBackend::ReleaseFunction(session, compiled);

        std::string label = std::string(testCase.name) + " case " + std::to_string(index);

        if (status.reason != Interpreter::ExitReason::Returned)
        {
            printf("FAIL %s left translated code with %s rather than a return\n", label.c_str(),
                   Interpreter::ExitReasonName(status.reason));
            failures += 1;
            continue;
        }

        if (!GuestState::Equals(interpreted, translated))
        {
            printf("FAIL %s: the register file differs between the interpreter and the translator\n", label.c_str());
            ReportDifference(interpreted, translated);
            failures += 1;
            continue;
        }

        compared += 1;
        printf("PASS %s -> x0=%016llx over %llu block(s)\n", label.c_str(), (unsigned long long)translated.general[0],
               (unsigned long long)function.blocks.size());
    }

    printf("\n%llu of %d functions had internal control flow, %llu blocks decoded in total\n", (unsigned long long)multiBlock,
           FUNCTION_CASE_COUNT, (unsigned long long)totalBlocks);

    Check(multiBlock * 2 > (uint64_t)FUNCTION_CASE_COUNT,
          "most cases exercise a function with internal control flow, so this really is a whole-function differential");
    Check(totalBlocks >= 3 * (uint64_t)FUNCTION_CASE_COUNT,
          "the cases average several blocks each, so loops and branches stayed inside translated code");
    Check(compared == FUNCTION_CASE_COUNT, "every case was compared rather than skipped");

    LlvmBackend::Shutdown(session);
    GuestRuntime::Destroy(instance);

    if (compared == 0)
    {
        printf("function-differential CANNOT DISCRIMINATE: nothing was compared\n");
        return SKIP_EXIT_CODE;
    }

    printf("%s: %d checks failed\n", failures == 0 ? "function-differential PASSED" : "function-differential FAILED", failures);
    return failures == 0 ? 0 : 1;
}

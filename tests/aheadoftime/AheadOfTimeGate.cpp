#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

#include "AheadOfTime/AheadOfTime.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "Settings/Settings.hpp"

struct FunctionCase
{
    const char* name;
    uint64_t argument;
    uint64_t expected;
};

static const FunctionCase FUNCTION_CASES[] = {
    {"SumTo", 97, 4656},
    {"CollatzSteps", 27, 111},
    {"NestedLoops", 13, 606},
    {"BitWeights", 0xffffffffffffffffull, 2016},
    {"CountdownDivide", 1000000, 13},
    {"CallChain", 50, 451},
};

static constexpr int FUNCTION_CASE_COUNT = (int)(sizeof(FUNCTION_CASES) / sizeof(FUNCTION_CASES[0]));
static constexpr uint64_t STACK_BYTES = 1u << 20;

static int failures = 0;

static bool HasFlag(int argc, char** argv, const char* flag)
{
    for (int index = 1; index < argc; ++index)
    {
        if (strcmp(argv[index], flag) == 0) return true;
    }

    return false;
}

static const char* FlagValue(int argc, char** argv, const char* flag)
{
    for (int index = 1; index + 1 < argc; ++index)
    {
        if (strcmp(argv[index], flag) == 0) return argv[index + 1];
    }

    return nullptr;
}

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
        fprintf(stderr, "usage: AheadOfTimeGateTest <libfunctions.so>\n");
        return 2;
    }

    std::string path = argv[1];
    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.stubUnresolvedSymbols = true;
    configuration.requireTranslated = !HasFlag(argc, argv, "--skip-translation");

    const char* cacheRoot = FlagValue(argc, argv, "--cache-root");
    if (cacheRoot) Settings::Set(AheadOfTime::CACHE_ROOT_SETTING, cacheRoot);

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("aheadoftime-gate FAILED: cannot create the guest runtime\n");
        return 1;
    }

    if (GuestRuntime::RegisterPlatformProxies(instance) != GuestRuntime::Result::Ok ||
        GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        printf("aheadoftime-gate FAILED: cannot register the guest platform surface\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        printf("aheadoftime-gate FAILED: cannot load %s\n", path.c_str());
        GuestRuntime::Destroy(instance);
        return 1;
    }

    AheadOfTime::Image image;
    AheadOfTime::Statistics statistics;
    AheadOfTime::Result translated = AheadOfTime::Result::Ok;
    if (!HasFlag(argc, argv, "--skip-translation"))
        translated = AheadOfTime::Translate(instance, moduleId, path, image, statistics);

    printf("translate: %s, %llu discovered, %llu translated, %llu blocks, %llu traps, %llu direct calls, %llu entries, cache %s\n",
           AheadOfTime::ResultName(translated), (unsigned long long)statistics.functionsDiscovered,
           (unsigned long long)statistics.functionsTranslated, (unsigned long long)statistics.blocksTranslated,
           (unsigned long long)statistics.trapsEmitted, (unsigned long long)statistics.directCallsEmitted,
           (unsigned long long)statistics.entriesResolved, statistics.cacheHit ? "hit" : "miss");

    Check(translated == AheadOfTime::Result::Ok, "the whole library was translated to a native shared object before running");
    Check(statistics.cacheHit || statistics.functionsTranslated > 0, "a cache miss emitted functions into the object file");
    Check(statistics.entriesResolved > 0,
          "every decoded block resolved to a translated entry point, so the dispatcher can enter anywhere in a function");

    if (translated != AheadOfTime::Result::Ok)
    {
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::GuestAddress stackTop{};
    GuestState::GuestAddress threadPointer{};
    GuestRuntime::AllocateStack(instance, STACK_BYTES, stackTop);
    GuestRuntime::AllocateThreadPointer(instance, threadPointer);

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

        GuestState::Registers registers;
        GuestState::Reset(registers);
        registers.stackPointer = (uint64_t)stackTop;
        registers.threadPointer = (uint64_t)threadPointer;
        registers.general[0] = testCase.argument;

        GuestRuntime::Result ran = GuestRuntime::CallGuest(instance, entry, registers);

        if (ran != GuestRuntime::Result::Ok)
        {
            printf("FAIL %s ran natively: %s\n", testCase.name, GuestRuntime::ResultName(ran));
            failures += 1;
            continue;
        }

        if (registers.general[0] != testCase.expected)
        {
            printf("FAIL %s returned %llu, expected %llu\n", testCase.name, (unsigned long long)registers.general[0],
                   (unsigned long long)testCase.expected);
            failures += 1;
            continue;
        }

        printf("PASS %s(%llu) = %llu, run as translated x86-64\n", testCase.name, (unsigned long long)testCase.argument,
               (unsigned long long)registers.general[0]);
    }

    GuestRuntime::Statistics counters = GuestRuntime::GetStatistics(instance);
    printf("execution: %llu blocks total, %llu translated, %llu interpreted, %llu through the block JIT, %llu traps hit\n",
           (unsigned long long)counters.blocksExecuted, (unsigned long long)counters.blocksExecutedTranslated,
           (unsigned long long)counters.blocksInterpreted, (unsigned long long)counters.blocksExecutedNative,
           (unsigned long long)counters.trapsHit);

    Check(statistics.cacheHit || statistics.directCallsEmitted > 0,
          "a guest call to a known function became a direct host call rather than a dispatcher round trip");
    Check(counters.blocksExecutedTranslated > 0, "translated code actually ran");
    Check(counters.blocksInterpreted == 0, "nothing was interpreted at run time");
    Check(counters.blocksExecutedNative == 0, "nothing went through the run-time block JIT either");
    Check(counters.trapsHit == 0, "no untranslatable instruction was reached");

    AheadOfTime::Release(image);
    GuestRuntime::Destroy(instance);

    printf("%s: %d checks failed\n", failures == 0 ? "aheadoftime-gate PASSED" : "aheadoftime-gate FAILED", failures);
    return failures == 0 ? 0 : 1;
}

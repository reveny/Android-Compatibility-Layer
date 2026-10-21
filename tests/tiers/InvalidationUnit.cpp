#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <thread>

#include "GuestRuntime/GuestRuntime.hpp"

static constexpr uint32_t MOVZ_X0_ONE = 0xD2800020u;
static constexpr uint32_t MOVZ_X0_TWO = 0xD2800040u;
static constexpr uint32_t MOVZ_X0_THREE = 0xD2800060u;
static constexpr uint32_t RET_X30 = 0xD65F03C0u;
static constexpr uint32_t MOVZ_X0_ZERO = 0xD2800000u;
static constexpr uint32_t MOVZ_X0_SEVEN = 0xD28000E0u;
static constexpr uint32_t MOVZ_X1_FIVE = 0xD28000A1u;
static constexpr uint32_t ADD_X0_X0_TWO = 0x91000800u;
static constexpr uint32_t SUBS_X1_X1_ONE = 0xF1000421u;
static constexpr uint32_t BNE_BACK_TWO = 0x54FFFFC1u;
static constexpr uint32_t MOVZ_X8_GETPID = 0xD2801588u;
static constexpr uint32_t SVC_ZERO = 0xD4000001u;
static constexpr uint32_t DC_CVAU_X0 = 0xD50B7B20u;
static constexpr uint32_t IC_IVAU_X0 = 0xD50B7520u;
static constexpr uint32_t DSB_ISH = 0xD5033B9Fu;
static constexpr uint32_t ISB = 0xD5033FDFu;
static constexpr uint32_t MRS_X0_CNTVCT = 0xD53BE040u;
static constexpr uint32_t MRS_X0_CNTFRQ = 0xD53BE000u;
static constexpr uint64_t HOT_THRESHOLD = 4;
static constexpr uint64_t LOOP_OFFSET = 0x100;
static constexpr uint64_t SYSCALL_OFFSET = 0x200;
static constexpr uint64_t PATCHED_OFFSET = 0x300;
static constexpr uint64_t FLUSH_OFFSET = 0x400;
static constexpr uint64_t COUNTER_OFFSET = 0x500;
static constexpr uint64_t FREQUENCY_OFFSET = 0x600;
static constexpr uint64_t EXPECTED_COUNTER_FREQUENCY = 24000000;
static constexpr uint64_t BACKGROUND_ATTEMPTS = 500;
static constexpr uint64_t BACKGROUND_PAUSE_MILLISECONDS = 10;

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

static bool CallWith(GuestRuntime::Instance& instance, GuestState::GuestAddress entry, uint64_t argument, uint64_t& returned)
{
    GuestState::Registers registers;
    GuestState::Reset(registers);
    registers.general[0] = argument;

    if (GuestRuntime::CallGuest(instance, entry, registers) != GuestRuntime::Result::Ok) return false;

    returned = registers.general[0];
    return true;
}

static bool CallAt(GuestRuntime::Instance& instance, GuestState::GuestAddress entry, uint64_t& returned)
{
    return CallWith(instance, entry, 0, returned);
}

static int RunTier(bool recompile)
{
    printf("--- %s ---\n", recompile ? "T1 (recompiling)" : "T0 (interpreting)");

    GuestRuntime::Configuration configuration;
    configuration.recompile = recompile;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: the guest runtime did not start\n");
        return 1;
    }

    GuestState::GuestAddress code{};
    if (GuestRuntime::Reserve(instance, 0x4000, 0x4000, code) != GuestRuntime::Result::Ok ||
        GuestRuntime::Commit(instance, code, 0x4000, true, true, true) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: cannot reserve an executable guest page\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);

    uint32_t first[2] = {MOVZ_X0_ONE, RET_X30};
    memory.Write(memory.context, code, first, sizeof(first));

    uint64_t returned = 0;
    Check(CallAt(instance, code, returned) && returned == 1, "the guest page executes and returns 1");

    uint64_t decodedAfterFirst = GuestRuntime::GetStatistics(instance).blocksDecoded;

    Check(CallAt(instance, code, returned) && returned == 1, "a second call still returns 1");
    Check(GuestRuntime::GetStatistics(instance).blocksDecoded == decodedAfterFirst,
          "the second call was served from the block cache without re-decoding");

    uint32_t second[2] = {MOVZ_X0_TWO, RET_X30};
    memory.Write(memory.context, code, second, sizeof(second));

    Check(GuestRuntime::GetStatistics(instance).codeWritesObserved > 0, "a write to an executable guest page is observed");

    Check(CallAt(instance, code, returned) && returned == 2,
          "after the code page was rewritten the guest returns the NEW value, so the stale block was invalidated");

    Check(GuestRuntime::GetStatistics(instance).blocksDecoded > decodedAfterFirst,
          "the rewritten page was re-decoded rather than served stale from the cache");

    if (recompile)
    {
        Check(GuestRuntime::GetStatistics(instance).blocksRecompiled > 0,
              "T1 actually recompiled the block, so the native-entry invalidation path was exercised");
    }

    uint32_t third[2] = {MOVZ_X0_THREE, RET_X30};
    GuestRuntime::Protect(instance, code, 0x4000, true, true, false);
    memory.Write(memory.context, code, third, sizeof(third));
    GuestRuntime::Protect(instance, code, 0x4000, true, false, true);

    Check(CallAt(instance, code, returned) && returned == 3,
          "the write-enable / patch / re-protect cycle a real guest JIT uses also invalidates");

    GuestRuntime::Destroy(instance);
    return failures;
}

static int RunHotRegions()
{
    printf("--- T1 with a hot threshold of %llu and run-time regions ---\n", (unsigned long long)HOT_THRESHOLD);

    GuestRuntime::Configuration configuration;
    configuration.recompile = true;
    configuration.recompileThreshold = HOT_THRESHOLD;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: the guest runtime did not start\n");
        return 1;
    }

    GuestState::GuestAddress code{};
    if (GuestRuntime::Reserve(instance, 0x4000, 0x4000, code) != GuestRuntime::Result::Ok ||
        GuestRuntime::Commit(instance, code, 0x4000, true, true, true) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: cannot reserve an executable guest page\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);

    uint32_t straight[2] = {MOVZ_X0_ONE, RET_X30};
    memory.Write(memory.context, code, straight, sizeof(straight));

    uint64_t returned = 0;
    bool coldCallsReturned = true;

    for (uint64_t call = 1; call < HOT_THRESHOLD; ++call)
        coldCallsReturned = coldCallsReturned && CallAt(instance, code, returned) && returned == 1;

    Check(coldCallsReturned, "the cold calls below the threshold return 1");
    Check(GuestRuntime::GetStatistics(instance).regionsRecompiled == 0, "nothing is compiled before a block reaches the threshold");
    Check(GuestRuntime::GetStatistics(instance).blocksInterpreted >= HOT_THRESHOLD - 1, "the cold calls were interpreted");

    Check(CallAt(instance, code, returned) && returned == 1, "the call that reaches the threshold still returns 1");
    Check(GuestRuntime::GetStatistics(instance).regionsRecompiled == 1, "the block is compiled once it reaches the threshold");
    Check(GuestRuntime::GetStatistics(instance).blocksExecutedNative > 0, "the hot block then runs compiled");

    GuestState::GuestAddress loop = (GuestState::GuestAddress)((uint64_t)code + LOOP_OFFSET);
    uint32_t looping[7] = {MOVZ_X0_ZERO, MOVZ_X1_FIVE, ADD_X0_X0_TWO, SUBS_X1_X1_ONE, BNE_BACK_TWO, RET_X30, RET_X30};
    memory.Write(memory.context, loop, looping, sizeof(looping));

    bool loopReturned = true;
    for (uint64_t call = 0; call < HOT_THRESHOLD; ++call) loopReturned = loopReturned && CallAt(instance, loop, returned) && returned == 10;

    Check(loopReturned, "a counted loop returns 10 while it warms up");

    uint64_t regionsBefore = GuestRuntime::GetStatistics(instance).regionsRecompiled;
    uint64_t blocksBefore = GuestRuntime::GetStatistics(instance).blocksRecompiled;

    Check(regionsBefore >= 2, "the loop was compiled at run time");
    Check(blocksBefore >= regionsBefore + 2, "a region spans several guest blocks rather than one");

    uint32_t patched = MOVZ_X0_SEVEN;
    memory.Write(memory.context, (GuestState::GuestAddress)((uint64_t)loop + 5 * sizeof(uint32_t)), &patched, sizeof(patched));

    Check(CallAt(instance, loop, returned) && returned == 7,
          "a write to a block that only ever ran inside a compiled region invalidates the whole region");

    GuestState::GuestAddress system = (GuestState::GuestAddress)((uint64_t)code + SYSCALL_OFFSET);
    uint32_t calling[3] = {MOVZ_X8_GETPID, SVC_ZERO, RET_X30};
    memory.Write(memory.context, system, calling, sizeof(calling));

    uint64_t failuresBefore = GuestRuntime::GetStatistics(instance).recompileFailures;
    bool systemReturned = true;

    for (uint64_t call = 0; call < HOT_THRESHOLD * 3; ++call)
        systemReturned = systemReturned && CallAt(instance, system, returned) && returned != 0;

    Check(systemReturned, "a block that makes a system call keeps working past the threshold");
    Check(GuestRuntime::GetStatistics(instance).recompileFailures == failuresBefore + 1,
          "a block the recompiler refuses is tried once and then left to the interpreter");

    GuestState::GuestAddress patchedEntry = (GuestState::GuestAddress)((uint64_t)code + PATCHED_OFFSET);
    memory.Write(memory.context, patchedEntry, straight, sizeof(straight));

    bool patchedReturned = true;
    for (uint64_t call = 0; call <= HOT_THRESHOLD; ++call)
        patchedReturned = patchedReturned && CallAt(instance, patchedEntry, returned) && returned == 1;

    Check(patchedReturned, "the function a guest JIT will patch runs hot and returns 1");

    GuestState::GuestAddress flush = (GuestState::GuestAddress)((uint64_t)code + FLUSH_OFFSET);
    uint32_t flushing[6] = {DC_CVAU_X0, DSB_ISH, IC_IVAU_X0, DSB_ISH, ISB, RET_X30};
    memory.Write(memory.context, flush, flushing, sizeof(flushing));

    uint32_t replacement = MOVZ_X0_TWO;
    memcpy((void*)(uintptr_t)(uint64_t)patchedEntry, &replacement, sizeof(replacement));

    Check(CallAt(instance, patchedEntry, returned) && returned == 1,
          "a host-side patch is invisible to the runtime until the guest flushes, so the old code still runs");

    uint64_t flushed = 0;
    Check(CallWith(instance, flush, (uint64_t)patchedEntry, flushed), "dc cvau, dsb, ic ivau and isb all decode and run");
    Check(CallAt(instance, patchedEntry, returned) && returned == 2,
          "after a host-side patch and an ic ivau the guest runs the new code, not the compiled copy of the old");

    GuestState::GuestAddress counter = (GuestState::GuestAddress)((uint64_t)code + COUNTER_OFFSET);
    uint32_t counting[2] = {MRS_X0_CNTVCT, RET_X30};
    memory.Write(memory.context, counter, counting, sizeof(counting));

    uint64_t previous = 0;
    bool counterAdvances = true;

    for (uint64_t call = 0; call <= HOT_THRESHOLD + 1; ++call)
    {
        uint64_t ticks = 0;
        counterAdvances = counterAdvances && CallAt(instance, counter, ticks) && ticks != 0 && ticks >= previous;
        previous = ticks;
    }

    Check(counterAdvances, "cntvct_el0 reads a nonzero counter that never goes backwards, interpreted and compiled");

    GuestState::GuestAddress frequency = (GuestState::GuestAddress)((uint64_t)code + FREQUENCY_OFFSET);
    uint32_t asking[2] = {MRS_X0_CNTFRQ, RET_X30};
    memory.Write(memory.context, frequency, asking, sizeof(asking));

    Check(CallAt(instance, frequency, returned) && returned == EXPECTED_COUNTER_FREQUENCY, "cntfrq_el0 reports the counter frequency");

    GuestRuntime::Destroy(instance);
    return failures;
}

static int RunBackgroundCompiler()
{
    printf("--- T1 compiling on a background thread ---\n");

    GuestRuntime::Configuration configuration;
    configuration.recompile = true;
    configuration.recompileThreshold = 1;
    configuration.recompileInBackground = true;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: the guest runtime did not start\n");
        return 1;
    }

    GuestState::GuestAddress code{};
    if (GuestRuntime::Reserve(instance, 0x4000, 0x4000, code) != GuestRuntime::Result::Ok ||
        GuestRuntime::Commit(instance, code, 0x4000, true, true, true) != GuestRuntime::Result::Ok)
    {
        printf("invalidation-unit FAILED: cannot reserve an executable guest page\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);

    uint32_t looping[6] = {MOVZ_X0_ZERO, MOVZ_X1_FIVE, ADD_X0_X0_TWO, SUBS_X1_X1_ONE, BNE_BACK_TWO, RET_X30};
    memory.Write(memory.context, code, looping, sizeof(looping));

    uint64_t returned = 0;
    bool alwaysCorrect = true;
    bool ranCompiled = false;

    for (uint64_t attempt = 0; attempt < BACKGROUND_ATTEMPTS && !ranCompiled; ++attempt)
    {
        alwaysCorrect = alwaysCorrect && CallAt(instance, code, returned) && returned == 10;
        ranCompiled = GuestRuntime::GetStatistics(instance).blocksExecutedNative > 0;

        if (!ranCompiled) std::this_thread::sleep_for(std::chrono::milliseconds(BACKGROUND_PAUSE_MILLISECONDS));
    }

    Check(alwaysCorrect, "the loop returns 10 on every call while the background thread compiles it");
    Check(ranCompiled, "the region the background thread compiled is picked up by later calls");

    GuestRuntime::Destroy(instance);
    Check(true, "the runtime shuts down with the background compiler running");
    return failures;
}

int main()
{
    RunTier(false);
    RunTier(true);
    RunHotRegions();
    RunBackgroundCompiler();

    printf("%s: %d checks failed\n", failures == 0 ? "invalidation-unit PASSED" : "invalidation-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}

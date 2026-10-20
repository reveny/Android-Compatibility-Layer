#include <cstdint>
#include <cstdio>
#include <cstring>

#include "GuestRuntime/GuestRuntime.hpp"

static constexpr uint32_t MOVZ_X0_ONE = 0xD2800020u;
static constexpr uint32_t MOVZ_X0_TWO = 0xD2800040u;
static constexpr uint32_t MOVZ_X0_THREE = 0xD2800060u;
static constexpr uint32_t RET_X30 = 0xD65F03C0u;

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

static bool CallAt(GuestRuntime::Instance& instance, GuestState::GuestAddress entry, uint64_t& returned)
{
    GuestState::Registers registers;
    GuestState::Reset(registers);

    if (GuestRuntime::CallGuest(instance, entry, registers) != GuestRuntime::Result::Ok) return false;

    returned = registers.general[0];
    return true;
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

int main()
{
    RunTier(false);
    RunTier(true);

    printf("%s: %d checks failed\n", failures == 0 ? "invalidation-unit PASSED" : "invalidation-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}

#include "VectorHarness.hpp"

#include <cstdio>
#include <sys/auxv.h>

#ifndef HWCAP_ATOMICS
#define HWCAP_ATOMICS (1 << 8)
#endif
#ifndef HWCAP_LRCPC
#define HWCAP_LRCPC (1 << 15)
#endif

bool VectorHarness::HasRequiredCpuFeatures()
{
    unsigned long hwcap = getauxval(AT_HWCAP);
    bool ok = true;

    if ((hwcap & HWCAP_ATOMICS) == 0)
    {
        fprintf(stderr, "error: CPU lacks FEAT_LSE (atomics); CAS/CASP/LDADD/SWP would SIGILL\n");
        ok = false;
    }
    if ((hwcap & HWCAP_LRCPC) == 0)
    {
        fprintf(stderr, "error: CPU lacks FEAT_LRCPC; LDAPR would SIGILL\n");
        ok = false;
    }

    return ok;
}

void VectorHarness::SeedState(State& state, uint64_t seed)
{
    (void)seed;
    for (int index = 0; index < GENERAL_REGISTER_COUNT; ++index)
    {
        state.general[index] = UNWRITTEN_REGISTER;
    }

    state.nzcv = 0;
    state.floatingPointStatus = 0;

    for (int index = 0; index < SCRATCH_WORD_COUNT; ++index)
    {
        state.scratch[index] = 0;
    }

    state.vectorLow = 0;
    state.vectorHigh = 0;
}

void VectorHarness::PrintState(const char* name, const State& state)
{
    printf("%s", name);

    for (int index = 0; index < GENERAL_REGISTER_COUNT; ++index)
    {
        printf(" x%d=%016llx", index, (unsigned long long)state.general[index]);
    }

    printf(" nzcv=%08llx", (unsigned long long)(state.nzcv & 0xF0000000ull));
    printf(" fpsr=%08llx", (unsigned long long)(state.floatingPointStatus & FPSR_CAPTURED_BITS));

    for (int index = 0; index < SCRATCH_WORD_COUNT; ++index)
    {
        printf(" m%d=%016llx", index, (unsigned long long)state.scratch[index]);
    }

    printf(" vlo=%016llx vhi=%016llx\n", (unsigned long long)state.vectorLow, (unsigned long long)state.vectorHigh);
    fflush(stdout);
}

int VectorHarness::RunAll()
{
    if (!HasRequiredCpuFeatures()) return 2;

    for (int index = 0; index < TEST_CASE_COUNT; ++index)
    {
        State state;
        SeedState(state, 0x5DEECE66Dull + (uint64_t)index);
        TEST_CASES[index].Run(state);
        PrintState(TEST_CASES[index].name, state);
    }

    return 0;
}

int main()
{
    return VectorHarness::RunAll();
}

#pragma once

#include <cstdint>

namespace VectorHarness
{
    inline constexpr int GENERAL_REGISTER_COUNT = 31;
    inline constexpr int SCRATCH_WORD_COUNT = 8;
    inline constexpr int EXCLUSIVE_RETRY_LIMIT = 100;
    inline constexpr uint64_t FPSR_CAPTURED_BITS = 0x0800009Full;
    inline constexpr uint64_t UNWRITTEN_REGISTER = 0xBAADF00DBAADF00Dull;

    struct alignas(16) State
    {
        uint64_t general[GENERAL_REGISTER_COUNT];
        uint64_t nzcv;
        uint64_t floatingPointStatus;
        alignas(16) uint64_t scratch[SCRATCH_WORD_COUNT];
        uint64_t vectorLow;
        uint64_t vectorHigh;
    };

    struct TestCase
    {
        const char* name;
        void (*Run)(State& state);
    };

    bool HasRequiredCpuFeatures();
    void SeedState(State& state, uint64_t seed);
    void PrintState(const char* name, const State& state);
    int RunAll();

    extern const TestCase TEST_CASES[];
    extern const int TEST_CASE_COUNT;
}

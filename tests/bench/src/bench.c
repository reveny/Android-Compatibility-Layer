#include <stdint.h>

static uint64_t table[1024];

uint64_t BenchmarkMemoryKernel(uint64_t iterations, uint64_t seed)
{
    uint64_t index = seed | 1;
    uint64_t total = 0;

    for (uint64_t step = 0; step < iterations; ++step)
    {
        index = index * 6364136223846793005ull + 1442695040888963407ull;
        uint64_t slot = (index >> 33) & 1023;

        total += table[slot];
        table[slot] = total ^ step;
    }

    return total;
}

uint64_t BenchmarkKernel(uint64_t iterations, uint64_t seed)
{
    uint64_t accumulator = seed;
    uint64_t alternate = seed ^ 0x9E3779B97F4A7C15ull;

    for (uint64_t index = 0; index < iterations; ++index)
    {
        accumulator += alternate;
        accumulator ^= accumulator >> 29;
        accumulator *= 0xBF58476D1CE4E5B9ull;
        accumulator ^= accumulator >> 32;

        alternate -= accumulator;
        alternate ^= alternate << 17;
        alternate += 0x94D049BB133111EBull;
        alternate ^= alternate >> 13;
    }

    return accumulator ^ alternate;
}

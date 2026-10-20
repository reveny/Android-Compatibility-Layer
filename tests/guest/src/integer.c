#include <stdint.h>

uint64_t MixBits(uint64_t value, uint64_t salt)
{
    value ^= value >> 33;
    value *= 0xff51afd7ed558ccdull;
    value ^= value >> 29;
    value *= salt | 1;
    value ^= value >> 32;
    return value;
}

int64_t SaturatingScale(int64_t value, int32_t numerator, int32_t denominator)
{
    if (denominator == 0) return value < 0 ? INT64_MIN : INT64_MAX;

    int64_t scaled = value * (int64_t)numerator;
    if (numerator != 0 && scaled / numerator != value) return value < 0 ? INT64_MIN : INT64_MAX;
    return scaled / denominator;
}

uint32_t PopulationCount(uint64_t value)
{
    uint32_t total = 0;
    while (value)
    {
        value &= value - 1;
        total += 1;
    }
    return total;
}

uint64_t RotateAndMask(uint64_t value, unsigned amount, uint64_t mask)
{
    amount &= 63;
    uint64_t rotated = amount == 0 ? value : ((value >> amount) | (value << (64 - amount)));
    return (rotated & mask) | ((~rotated) & ~mask);
}

int CompareChain(int64_t a, int64_t b, int64_t c)
{
    if (a < b) return a < c ? -1 : 1;
    if (a > b) return b < c ? 2 : -2;
    return c == a ? 0 : 3;
}

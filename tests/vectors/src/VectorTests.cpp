#include "VectorTests.hpp"

void VectorTests::AddCarryOverflow(VectorHarness::State& state)
{
    uint64_t result = 0;
    uint64_t flags = 0;
    asm volatile("adds %0, %2, %3\n\t"
                 "mrs %1, nzcv"
                 : "=&r"(result), "=&r"(flags)
                 : "r"(0x7FFFFFFFFFFFFFFFull), "r"(1ull)
                 : "cc");
    state.general[0] = result;
    state.nzcv = flags;
}

void VectorTests::SubtractFlags(VectorHarness::State& state)
{
    uint64_t result = 0;
    uint64_t flags = 0;
    asm volatile("subs %0, %2, %3\n\t"
                 "mrs %1, nzcv"
                 : "=&r"(result), "=&r"(flags)
                 : "r"(5ull), "r"(9ull)
                 : "cc");
    state.general[0] = result;
    state.nzcv = flags;
}

void VectorTests::AddsZeroResult(VectorHarness::State& state)
{
    uint64_t result = 0;
    uint64_t flags = 0;
    asm volatile("adds %0, %2, %3\n\t"
                 "mrs %1, nzcv"
                 : "=&r"(result), "=&r"(flags)
                 : "r"(0xFFFFFFFFFFFFFFFFull), "r"(1ull)
                 : "cc");
    state.general[0] = result;
    state.nzcv = flags;
}

void VectorTests::ShiftFamily(VectorHarness::State& state)
{
    uint64_t left = 0;
    uint64_t logical = 0;
    uint64_t arithmetic = 0;
    uint64_t rotate = 0;
    asm volatile("lsl %0, %4, %5\n\t"
                 "lsr %1, %4, %5\n\t"
                 "asr %2, %4, %5\n\t"
                 "ror %3, %4, %5"
                 : "=&r"(left), "=&r"(logical), "=&r"(arithmetic), "=&r"(rotate)
                 : "r"(0x8000000000000123ull), "r"(9ull));
    state.general[0] = left;
    state.general[1] = logical;
    state.general[2] = arithmetic;
    state.general[3] = rotate;
}

void VectorTests::BitCounting(VectorHarness::State& state)
{
    uint64_t leadingZeros = 0;
    uint64_t reversedBits = 0;
    asm volatile("clz %0, %2\n\t"
                 "rbit %1, %2"
                 : "=&r"(leadingZeros), "=&r"(reversedBits)
                 : "r"(0x0000000000FF0000ull));
    state.general[0] = leadingZeros;
    state.general[1] = reversedBits;
}

void VectorTests::ByteReversal(VectorHarness::State& state)
{
    uint64_t rev64 = 0;
    uint64_t rev32 = 0;
    uint64_t rev16 = 0;
    asm volatile("rev %0, %3\n\t"
                 "rev32 %1, %3\n\t"
                 "rev16 %2, %3"
                 : "=&r"(rev64), "=&r"(rev32), "=&r"(rev16)
                 : "r"(0x0123456789ABCDEFull));
    state.general[0] = rev64;
    state.general[1] = rev32;
    state.general[2] = rev16;
}

void VectorTests::ConditionalSelect(VectorHarness::State& state)
{
    uint64_t selected = 0;
    uint64_t incremented = 0;
    uint64_t negated = 0;
    asm volatile("cmp %3, %4\n\t"
                 "csel %0, %3, %4, hi\n\t"
                 "csinc %1, %3, %4, eq\n\t"
                 "csneg %2, %3, %4, lt"
                 : "=&r"(selected), "=&r"(incremented), "=&r"(negated)
                 : "r"(0x10ull), "r"(0x20ull)
                 : "cc");
    state.general[0] = selected;
    state.general[1] = incremented;
    state.general[2] = negated;
}

void VectorTests::MultiplyHigh(VectorHarness::State& state)
{
    uint64_t unsignedHigh = 0;
    uint64_t signedHigh = 0;
    uint64_t low = 0;
    asm volatile("umulh %0, %3, %4\n\t"
                 "smulh %1, %3, %4\n\t"
                 "mul %2, %3, %4"
                 : "=&r"(unsignedHigh), "=&r"(signedHigh), "=&r"(low)
                 : "r"(0xDEADBEEFCAFEBABEull), "r"(0xFFFFFFFFFFFFFFF0ull));
    state.general[0] = unsignedHigh;
    state.general[1] = signedHigh;
    state.general[2] = low;
}

void VectorTests::DivideByZero(VectorHarness::State& state)
{
    uint64_t signedResult = 0;
    uint64_t unsignedResult = 0;
    asm volatile("sdiv %0, %2, %3\n\t"
                 "udiv %1, %2, %3"
                 : "=&r"(signedResult), "=&r"(unsignedResult)
                 : "r"(0x1234ull), "r"(0ull));
    state.general[0] = signedResult;
    state.general[1] = unsignedResult;
}

void VectorTests::DivideSignedOverflow(VectorHarness::State& state)
{
    uint64_t result = 0;
    asm volatile("sdiv %0, %1, %2" : "=&r"(result) : "r"(0x8000000000000000ull), "r"(0xFFFFFFFFFFFFFFFFull));
    state.general[0] = result;
}

void VectorTests::ExtendAndTruncate(VectorHarness::State& state)
{
    uint64_t signExtendByte = 0;
    uint64_t signExtendHalf = 0;
    uint64_t signExtendWord = 0;
    uint64_t zeroExtendByte = 0;
    asm volatile("sxtb %0, %w4\n\t"
                 "sxth %1, %w4\n\t"
                 "sxtw %2, %w4\n\t"
                 "uxtb %w3, %w4"
                 : "=&r"(signExtendByte), "=&r"(signExtendHalf), "=&r"(signExtendWord), "=&r"(zeroExtendByte)
                 : "r"(0xFFFFFFFF87654321ull));
    state.general[0] = signExtendByte;
    state.general[1] = signExtendHalf;
    state.general[2] = signExtendWord;
    state.general[3] = zeroExtendByte;
}

void VectorTests::BitfieldExtract(VectorHarness::State& state)
{
    uint64_t unsignedField = 0;
    uint64_t signedField = 0;
    uint64_t inserted = 0x1111111111111111ull;
    asm volatile("ubfx %0, %3, #8, #16\n\t"
                 "sbfx %1, %3, #8, #16\n\t"
                 "bfi %2, %3, #4, #8"
                 : "=&r"(unsignedField), "=&r"(signedField), "+r"(inserted)
                 : "r"(0x0123456789ABCDEFull));
    state.general[0] = unsignedField;
    state.general[1] = signedField;
    state.general[2] = inserted;
}

void VectorTests::LoadAcquireStoreRelease(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t loaded = 0;
    *cell = 0;
    asm volatile("stlr %2, [%1]\n\t"
                 "ldar %0, [%1]"
                 : "=&r"(loaded)
                 : "r"(cell), "r"(0xA5A5A5A5DEADBEEFull)
                 : "memory");
    state.general[0] = loaded;
}

void VectorTests::ExclusiveStoreSuccess(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t loaded = 0;
    uint64_t status = 1;

    for (int attempt = 0; attempt < VectorHarness::EXCLUSIVE_RETRY_LIMIT && status != 0; ++attempt)
    {
        *cell = 0x1122334455667788ull;
        asm volatile("ldxr %0, [%2]\n\t"
                     "stxr %w1, %3, [%2]"
                     : "=&r"(loaded), "=&r"(status)
                     : "r"(cell), "r"(0x99AABBCCDDEEFF00ull)
                     : "memory");
    }

    state.general[0] = loaded;
    state.general[1] = status;
}

void VectorTests::ExclusiveClearedByClrex(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t loaded = 0;
    uint64_t status = 0;
    *cell = 0x1122334455667788ull;
    asm volatile("ldxr %0, [%2]\n\t"
                 "clrex\n\t"
                 "stxr %w1, %3, [%2]"
                 : "=&r"(loaded), "=&r"(status)
                 : "r"(cell), "r"(0xFFFFFFFFFFFFFFFFull)
                 : "memory");
    state.general[0] = loaded;
    state.general[1] = status;
}

void VectorTests::CompareAndSwap(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t expected = 0x0F0F0F0F0F0F0F0Full;
    *cell = 0x0F0F0F0F0F0F0F0Full;
    asm volatile("cas %0, %2, [%1]" : "+r"(expected) : "r"(cell), "r"(0xC0FFEEC0FFEEC0FFull) : "memory");
    state.general[0] = expected;
}

void VectorTests::CompareAndSwapFailure(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t expected = 0xDEADDEADDEADDEADull;
    *cell = 0x0F0F0F0F0F0F0F0Full;
    asm volatile("cas %0, %2, [%1]" : "+r"(expected) : "r"(cell), "r"(0xC0FFEEC0FFEEC0FFull) : "memory");
    state.general[0] = expected;
}

void VectorTests::AtomicAdd(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t previous = 0;
    *cell = 0x1000ull;
    asm volatile("ldadd %2, %0, [%1]" : "=&r"(previous) : "r"(cell), "r"(0x234ull) : "memory");
    state.general[0] = previous;
}

void VectorTests::AtomicSwap(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t previous = 0;
    *cell = 0xAAAABBBBCCCCDDDDull;
    asm volatile("swp %2, %0, [%1]" : "=&r"(previous) : "r"(cell), "r"(0x1111222233334444ull) : "memory");
    state.general[0] = previous;
}

void VectorTests::CompareAndSwapPair(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    cell[0] = 0x1111111111111111ull;
    cell[1] = 0x2222222222222222ull;

    register uint64_t expectedLow asm("x2") = 0x1111111111111111ull;
    register uint64_t expectedHigh asm("x3") = 0x2222222222222222ull;
    register uint64_t newLow asm("x4") = 0xAAAAAAAAAAAAAAAAull;
    register uint64_t newHigh asm("x5") = 0xBBBBBBBBBBBBBBBBull;

    asm volatile("casp %0, %1, %2, %3, [%4]" : "+r"(expectedLow), "+r"(expectedHigh) : "r"(newLow), "r"(newHigh), "r"(cell) : "memory");

    state.general[0] = expectedLow;
    state.general[1] = expectedHigh;
    state.scratch[2] = cell[0];
    state.scratch[3] = cell[1];
}

void VectorTests::LoadAcquireOrdered(VectorHarness::State& state)
{
    volatile uint64_t* cell = &state.scratch[0];
    uint64_t loaded = 0;
    *cell = 0x5566778899AABBCCull;
    asm volatile("ldapr %0, [%1]" : "=&r"(loaded) : "r"(cell) : "memory");
    state.general[0] = loaded;
}

void VectorTests::VectorAddBytes(VectorHarness::State& state)
{
    uint64_t low = 0;
    uint64_t high = 0;
    asm volatile("movi v0.16b, #0x11\n\t"
                 "movi v1.16b, #0x22\n\t"
                 "add v2.16b, v0.16b, v1.16b\n\t"
                 "umov %0, v2.d[0]\n\t"
                 "umov %1, v2.d[1]"
                 : "=&r"(low), "=&r"(high)
                 :
                 : "v0", "v1", "v2");
    state.vectorLow = low;
    state.vectorHigh = high;
}

void VectorTests::FloatCompareQuietNan(VectorHarness::State& state)
{
    uint64_t flags = 0;
    uint64_t status = 0;
    double nanValue = __builtin_nan("");
    double one = 1.0;
    asm volatile("msr fpsr, xzr\n\t"
                 "fcmp %d2, %d3\n\t"
                 "mrs %0, nzcv\n\t"
                 "mrs %1, fpsr"
                 : "=&r"(flags), "=&r"(status)
                 : "w"(nanValue), "w"(one)
                 : "cc");
    state.nzcv = flags;
    state.floatingPointStatus = status;
}

void VectorTests::FloatCompareSignallingNan(VectorHarness::State& state)
{
    uint64_t flags = 0;
    uint64_t status = 0;
    double nanValue = __builtin_nans("");
    double one = 1.0;
    asm volatile("msr fpsr, xzr\n\t"
                 "fcmp %d2, %d3\n\t"
                 "mrs %0, nzcv\n\t"
                 "mrs %1, fpsr"
                 : "=&r"(flags), "=&r"(status)
                 : "w"(nanValue), "w"(one)
                 : "cc");
    state.nzcv = flags;
    state.floatingPointStatus = status;
}

void VectorTests::FloatCompareExceptionalQuietNan(VectorHarness::State& state)
{
    uint64_t flags = 0;
    uint64_t status = 0;
    double nanValue = __builtin_nan("");
    double one = 1.0;
    asm volatile("msr fpsr, xzr\n\t"
                 "fcmpe %d2, %d3\n\t"
                 "mrs %0, nzcv\n\t"
                 "mrs %1, fpsr"
                 : "=&r"(flags), "=&r"(status)
                 : "w"(nanValue), "w"(one)
                 : "cc");
    state.nzcv = flags;
    state.floatingPointStatus = status;
}

void VectorTests::FloatDivideZeroByZero(VectorHarness::State& state)
{
    uint64_t status = 0;
    double zero = 0.0;
    double result = 0.0;
    asm volatile("msr fpsr, xzr\n\t"
                 "fdiv %d0, %d2, %d2\n\t"
                 "mrs %1, fpsr"
                 : "=&w"(result), "=&r"(status)
                 : "w"(zero));
    state.floatingPointStatus = status;
    __builtin_memcpy(&state.general[0], &result, sizeof(uint64_t));
}

void VectorTests::FloatOverflowAndInexact(VectorHarness::State& state)
{
    uint64_t status = 0;
    double large = 1.0e308;
    double result = 0.0;
    asm volatile("msr fpsr, xzr\n\t"
                 "fmul %d0, %d2, %d2\n\t"
                 "mrs %1, fpsr"
                 : "=&w"(result), "=&r"(status)
                 : "w"(large));
    state.floatingPointStatus = status;
    __builtin_memcpy(&state.general[0], &result, sizeof(uint64_t));
}

void VectorTests::VectorSaturatingAdd(VectorHarness::State& state)
{
    uint64_t low = 0;
    uint64_t high = 0;
    uint64_t status = 0;
    asm volatile("msr fpsr, xzr\n\t"
                 "movi v0.16b, #0x7f\n\t"
                 "movi v1.16b, #0x7f\n\t"
                 "sqadd v2.16b, v0.16b, v1.16b\n\t"
                 "umov %0, v2.d[0]\n\t"
                 "umov %1, v2.d[1]\n\t"
                 "mrs %2, fpsr"
                 : "=&r"(low), "=&r"(high), "=&r"(status)
                 :
                 : "v0", "v1", "v2");
    state.vectorLow = low;
    state.vectorHigh = high;
    state.floatingPointStatus = status;
}

const VectorHarness::TestCase VectorHarness::TEST_CASES[] = {
    {"add_carry_overflow", VectorTests::AddCarryOverflow},
    {"subtract_flags", VectorTests::SubtractFlags},
    {"adds_zero_result", VectorTests::AddsZeroResult},
    {"shift_family", VectorTests::ShiftFamily},
    {"bit_counting", VectorTests::BitCounting},
    {"byte_reversal", VectorTests::ByteReversal},
    {"conditional_select", VectorTests::ConditionalSelect},
    {"multiply_high", VectorTests::MultiplyHigh},
    {"divide_by_zero", VectorTests::DivideByZero},
    {"divide_signed_overflow", VectorTests::DivideSignedOverflow},
    {"extend_and_truncate", VectorTests::ExtendAndTruncate},
    {"bitfield_extract", VectorTests::BitfieldExtract},
    {"load_acquire_store_release", VectorTests::LoadAcquireStoreRelease},
    {"exclusive_store_success", VectorTests::ExclusiveStoreSuccess},
    {"exclusive_cleared_by_clrex", VectorTests::ExclusiveClearedByClrex},
    {"compare_and_swap", VectorTests::CompareAndSwap},
    {"compare_and_swap_failure", VectorTests::CompareAndSwapFailure},
    {"atomic_add", VectorTests::AtomicAdd},
    {"atomic_swap", VectorTests::AtomicSwap},
    {"compare_and_swap_pair", VectorTests::CompareAndSwapPair},
    {"load_acquire_rcpc", VectorTests::LoadAcquireOrdered},
    {"neon_add_bytes", VectorTests::VectorAddBytes},
    {"float_compare_quiet_nan", VectorTests::FloatCompareQuietNan},
    {"float_compare_signalling_nan", VectorTests::FloatCompareSignallingNan},
    {"float_compare_exceptional_quiet_nan", VectorTests::FloatCompareExceptionalQuietNan},
    {"float_divide_zero_by_zero", VectorTests::FloatDivideZeroByZero},
    {"float_overflow_and_inexact", VectorTests::FloatOverflowAndInexact},
    {"neon_saturating_add", VectorTests::VectorSaturatingAdd},
};

const int VectorHarness::TEST_CASE_COUNT = sizeof(VectorHarness::TEST_CASES) / sizeof(VectorHarness::TEST_CASES[0]);

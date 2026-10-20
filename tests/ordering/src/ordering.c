#include <pthread.h>
#include <stdint.h>

#define CACHE_LINE 128
#define OUTCOME_SLOTS 4

struct Cells
{
    _Alignas(CACHE_LINE) uint64_t x;
    _Alignas(CACHE_LINE) uint64_t y;
};

static struct Cells cells;
static uint64_t outcomes[OUTCOME_SLOTS];
static uint64_t iterationCount;
static int selectedTest;

static uint64_t firstRead;
static uint64_t secondRead;

_Alignas(CACHE_LINE) static uint32_t arrived;
_Alignas(CACHE_LINE) static uint32_t generation;

static void Barrier(void)
{
    uint32_t seen = __atomic_load_n(&generation, __ATOMIC_ACQUIRE);

    if (__atomic_add_fetch(&arrived, 1u, __ATOMIC_ACQ_REL) == 2u)
    {
        __atomic_store_n(&arrived, 0u, __ATOMIC_RELAXED);
        __atomic_add_fetch(&generation, 1u, __ATOMIC_ACQ_REL);
        return;
    }

    while (__atomic_load_n(&generation, __ATOMIC_ACQUIRE) == seen)
    {
    }
}

static void StoreBufferFirstRelaxed(void)
{
    asm volatile("str %1, [%0]" : : "r"(&cells.x), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(firstRead) : "r"(&cells.y) : "memory");
}

static void StoreBufferSecondRelaxed(void)
{
    asm volatile("str %1, [%0]" : : "r"(&cells.y), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(secondRead) : "r"(&cells.x) : "memory");
}

static void StoreBufferFirstReleaseAcquire(void)
{
    asm volatile("stlr %1, [%0]" : : "r"(&cells.x), "r"(1ull) : "memory");
    asm volatile("ldar %0, [%1]" : "=r"(firstRead) : "r"(&cells.y) : "memory");
}

static void StoreBufferSecondReleaseAcquire(void)
{
    asm volatile("stlr %1, [%0]" : : "r"(&cells.y), "r"(1ull) : "memory");
    asm volatile("ldar %0, [%1]" : "=r"(secondRead) : "r"(&cells.x) : "memory");
}

static void StoreBufferFirstBarrier(void)
{
    asm volatile("str %1, [%0]\n\tdmb ish" : : "r"(&cells.x), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(firstRead) : "r"(&cells.y) : "memory");
}

static void StoreBufferSecondBarrier(void)
{
    asm volatile("str %1, [%0]\n\tdmb ish" : : "r"(&cells.y), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(secondRead) : "r"(&cells.x) : "memory");
}

static void MessagePassingWriterRelaxed(void)
{
    asm volatile("str %1, [%0]" : : "r"(&cells.x), "r"(1ull) : "memory");
    asm volatile("str %1, [%0]" : : "r"(&cells.y), "r"(1ull) : "memory");
}

static void MessagePassingReaderRelaxed(void)
{
    asm volatile("ldr %0, [%1]" : "=r"(firstRead) : "r"(&cells.y) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(secondRead) : "r"(&cells.x) : "memory");
}

static void MessagePassingWriterRelease(void)
{
    asm volatile("str %1, [%0]" : : "r"(&cells.x), "r"(1ull) : "memory");
    asm volatile("stlr %1, [%0]" : : "r"(&cells.y), "r"(1ull) : "memory");
}

static void MessagePassingReaderAcquire(void)
{
    asm volatile("ldar %0, [%1]" : "=r"(firstRead) : "r"(&cells.y) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(secondRead) : "r"(&cells.x) : "memory");
}

struct OrderingTest
{
    void (*First)(void);
    void (*Second)(void);
    int forbiddenSlot;
};

static const struct OrderingTest ORDERING_TESTS[] = {
    {StoreBufferFirstRelaxed, StoreBufferSecondRelaxed, -1},
    {StoreBufferFirstReleaseAcquire, StoreBufferSecondReleaseAcquire, 0},
    {StoreBufferFirstBarrier, StoreBufferSecondBarrier, 0},
    {MessagePassingWriterRelaxed, MessagePassingReaderRelaxed, -1},
    {MessagePassingWriterRelease, MessagePassingReaderAcquire, 2},
};

static void* SecondThread(void* raw)
{
    (void)raw;
    const struct OrderingTest* test = &ORDERING_TESTS[selectedTest];

    for (uint64_t index = 0; index < iterationCount; ++index)
    {
        Barrier();
        test->Second();
        Barrier();
    }

    return 0;
}

uint64_t RunOrdering(uint64_t testIndex, uint64_t iterations)
{
    selectedTest = (int)testIndex;
    iterationCount = iterations;

    for (int slot = 0; slot < OUTCOME_SLOTS; ++slot) outcomes[slot] = 0;

    const struct OrderingTest* test = &ORDERING_TESTS[selectedTest];

    pthread_t second;
    if (pthread_create(&second, 0, SecondThread, 0) != 0) return ~0ull;

    for (uint64_t index = 0; index < iterationCount; ++index)
    {
        cells.x = 0;
        cells.y = 0;
        firstRead = ~0ull;
        secondRead = ~0ull;

        Barrier();
        test->First();
        Barrier();

        int slot = ((firstRead != 0) << 1) | (secondRead != 0);
        outcomes[slot] += 1;
    }

    pthread_join(second, 0);

    if (test->forbiddenSlot < 0) return 0;
    return outcomes[test->forbiddenSlot];
}

uint64_t OrderingOutcome(uint64_t slot)
{
    if (slot >= OUTCOME_SLOTS) return 0;
    return outcomes[slot];
}

uint64_t OrderingTestCount(void)
{
    return sizeof(ORDERING_TESTS) / sizeof(ORDERING_TESTS[0]);
}

#include <pthread.h>
#include <sched.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define CACHE_LINE 128
#define DEFAULT_ITERATIONS 200000

struct Cells
{
    alignas(CACHE_LINE) uint64_t x;
    alignas(CACHE_LINE) uint64_t y;
};

struct LitmusTest
{
    const char* name;
    void (*Writer)(struct Cells* cells, uint64_t* observed);
    void (*Reader)(struct Cells* cells, uint64_t* first, uint64_t* second);
    int forbiddenFirst;
    int forbiddenSecond;
    bool hasForbidden;
    int requiredSlot;
};

static void MessagePassingWriterRelaxed(struct Cells* cells, uint64_t* observed)
{
    (void)observed;
    asm volatile("str %1, [%0]" : : "r"(&cells->x), "r"(1ull) : "memory");
    asm volatile("str %1, [%0]" : : "r"(&cells->y), "r"(1ull) : "memory");
}

static void MessagePassingWriterBarrier(struct Cells* cells, uint64_t* observed)
{
    (void)observed;
    asm volatile("str %1, [%0]\n\tdmb ish" : : "r"(&cells->x), "r"(1ull) : "memory");
    asm volatile("str %1, [%0]" : : "r"(&cells->y), "r"(1ull) : "memory");
}

static void MessagePassingWriterRelease(struct Cells* cells, uint64_t* observed)
{
    (void)observed;
    asm volatile("str %1, [%0]" : : "r"(&cells->x), "r"(1ull) : "memory");
    asm volatile("stlr %1, [%0]" : : "r"(&cells->y), "r"(1ull) : "memory");
}

static void MessagePassingReaderRelaxed(struct Cells* cells, uint64_t* first, uint64_t* second)
{
    asm volatile("ldr %0, [%1]" : "=r"(*first) : "r"(&cells->y) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(*second) : "r"(&cells->x) : "memory");
}

static void MessagePassingReaderBarrier(struct Cells* cells, uint64_t* first, uint64_t* second)
{
    asm volatile("ldr %0, [%1]\n\tdmb ish" : "=r"(*first) : "r"(&cells->y) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(*second) : "r"(&cells->x) : "memory");
}

static void MessagePassingReaderAcquire(struct Cells* cells, uint64_t* first, uint64_t* second)
{
    asm volatile("ldar %0, [%1]" : "=r"(*first) : "r"(&cells->y) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(*second) : "r"(&cells->x) : "memory");
}

static void StoreBufferWriter(struct Cells* cells, uint64_t* observed)
{
    asm volatile("str %1, [%0]" : : "r"(&cells->x), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(*observed) : "r"(&cells->y) : "memory");
}

static void StoreBufferReader(struct Cells* cells, uint64_t* first, uint64_t* second)
{
    (void)first;
    asm volatile("str %1, [%0]" : : "r"(&cells->y), "r"(1ull) : "memory");
    asm volatile("ldr %0, [%1]" : "=r"(*second) : "r"(&cells->x) : "memory");
}

static const struct LitmusTest LITMUS_TESTS[] = {
    {"mp_relaxed", MessagePassingWriterRelaxed, MessagePassingReaderRelaxed, 0, 0, false, 3},
    {"mp_dmb_ish", MessagePassingWriterBarrier, MessagePassingReaderBarrier, 1, 0, true, 3},
    {"mp_release_acquire", MessagePassingWriterRelease, MessagePassingReaderAcquire, 1, 0, true, 3},
    {"sb_relaxed", StoreBufferWriter, StoreBufferReader, 0, 0, false, 0},
};

#define LITMUS_TEST_COUNT ((int)(sizeof(LITMUS_TESTS) / sizeof(LITMUS_TESTS[0])))

static struct Cells gCells;
static const struct LitmusTest* gTest;
static uint64_t gWriterObserved;
static volatile int gWriterRound;
static volatile int gStop;
static int gWriterCpu;
alignas(CACHE_LINE) static int gArrived;

static void Barrier(int round)
{
    __atomic_add_fetch(&gArrived, 1, __ATOMIC_SEQ_CST);
    while (__atomic_load_n(&gArrived, __ATOMIC_SEQ_CST) < 2 * round) { }
}

static void PinTo(int cpu)
{
    cpu_set_t set;
    CPU_ZERO(&set);
    CPU_SET(cpu, &set);
    sched_setaffinity(0, sizeof(set), &set);
}

static void* WriterThread(void* argument)
{
    (void)argument;
    PinTo(gWriterCpu);

    for (int round = 1;; ++round)
    {
        if (gStop) return NULL;

        Barrier(round);
        if (gStop) return NULL;

        gTest->Writer(&gCells, &gWriterObserved);
        gWriterRound = round;
    }
}

int main(int argc, char** argv)
{
    int iterations = argc > 1 ? atoi(argv[1]) : DEFAULT_ITERATIONS;
    if (iterations <= 0) iterations = DEFAULT_ITERATIONS;

    gWriterCpu = 0;
    PinTo(4);

    for (int index = 0; index < LITMUS_TEST_COUNT; ++index)
    {
        const struct LitmusTest* test = &LITMUS_TESTS[index];

        gTest = test;
        gWriterRound = 0;
        gStop = 0;
        gArrived = 0;

        pthread_t writer;
        if (pthread_create(&writer, NULL, WriterThread, NULL) != 0)
        {
            fprintf(stderr, "error: could not create the writer thread\n");
            return 2;
        }

        uint64_t forbidden = 0;
        uint64_t outcomes[4] = {0, 0, 0, 0};

        for (int round = 1; round <= iterations; ++round)
        {
            memset(&gCells, 0, sizeof(gCells));
            gWriterObserved = 0;

            uint64_t first = 0;
            uint64_t second = 0;

            unsigned delay = (unsigned)(round * 2654435761u) % 96u;

            Barrier(round);
            for (unsigned tick = 0; tick < delay; ++tick) asm volatile("" ::: "memory");
            test->Reader(&gCells, &first, &second);

            while (gWriterRound != round) { }

            if (test->Writer == StoreBufferWriter) first = gWriterObserved;

            int slot = ((first != 0) ? 2 : 0) | ((second != 0) ? 1 : 0);
            outcomes[slot] += 1;

            if (test->hasForbidden && (int)first == test->forbiddenFirst && (int)second == test->forbiddenSecond) forbidden += 1;
        }

        gStop = 1;
        __atomic_add_fetch(&gArrived, 2, __ATOMIC_SEQ_CST);
        pthread_join(writer, NULL);

        bool interleaved = outcomes[test->requiredSlot] > 0;

        printf("%s iterations=%d forbidden=%llu interleaved=%s\n", test->name, iterations, (unsigned long long)forbidden,
               interleaved ? "yes" : "no");
        fprintf(stderr, "%s outcomes 00=%llu 01=%llu 10=%llu 11=%llu\n", test->name, (unsigned long long)outcomes[0],
                (unsigned long long)outcomes[1], (unsigned long long)outcomes[2], (unsigned long long)outcomes[3]);
        fflush(stdout);
        fflush(stderr);
    }

    return 0;
}

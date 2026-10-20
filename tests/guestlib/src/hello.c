#include <pthread.h>
#include <setjmp.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static const char* const NAMES[4] = {"alpha", "bravo", "charlie", "delta"};

static uint64_t Square(uint64_t value)
{
    return value * value;
}

static uint64_t Triple(uint64_t value)
{
    return value * 3;
}

static uint64_t (*const OPERATIONS[2])(uint64_t) = {Square, Triple};

static uint64_t TouchThrough(volatile uint64_t* where)
{
    return *where;
}

uint64_t FaultWitness(uint64_t offset)
{
    volatile uint64_t* target = (volatile uint64_t*)offset;
    return TouchThrough(target);
}

uint64_t SelfContained(uint64_t seed)
{
    uint64_t accumulator = 0;

    for (uint64_t index = 0; index < 16; ++index)
    {
        uint64_t value = seed + index;
        accumulator += OPERATIONS[index & 1](value);
        accumulator ^= accumulator >> 7;
        accumulator *= 0x100000001B3ull;
    }

    return accumulator;
}

uint64_t NameLengthSum(uint64_t rotation)
{
    uint64_t total = 0;

    for (uint64_t index = 0; index < 4; ++index)
    {
        const char* cursor = NAMES[(index + rotation) & 3];
        while (*cursor != '\0')
        {
            total = total * 31 + (uint64_t)(unsigned char)*cursor;
            cursor += 1;
        }
    }

    return total;
}

uint64_t RelocationWitness(uint64_t seed)
{
    return SelfContained(seed) ^ NameLengthSum(seed);
}

static __thread uint64_t threadCounter;

struct WorkerArguments
{
    uint64_t seed;
    uint64_t result;
    uint64_t observedCounter;
};

static void* Worker(void* raw)
{
    struct WorkerArguments* arguments = (struct WorkerArguments*)raw;

    for (uint64_t index = 0; index < arguments->seed + 1; ++index) threadCounter += index + 1;

    arguments->result = SelfContained(arguments->seed) ^ NameLengthSum(arguments->seed);
    arguments->observedCounter = threadCounter;

    return (void*)(uintptr_t)arguments->result;
}

#define CONTENDER_COUNT 4
#define CONTENDER_ITERATIONS 2000

static uint64_t sharedCounter;
static uint64_t exchangedCounter;

static void* Contender(void* raw)
{
    (void)raw;

    for (int index = 0; index < CONTENDER_ITERATIONS; ++index)
    {
        __atomic_fetch_add(&sharedCounter, 1, __ATOMIC_RELAXED);

        uint64_t previous = __atomic_load_n(&exchangedCounter, __ATOMIC_RELAXED);
        while (!__atomic_compare_exchange_n(&exchangedCounter, &previous, previous + 1, 1, __ATOMIC_ACQ_REL, __ATOMIC_RELAXED))
        {
        }
    }

    return NULL;
}

static void* Inner(void* raw)
{
    return (void*)(uintptr_t)(((uint64_t)(uintptr_t)raw) * 2 + 1);
}

static void* Outer(void* raw)
{
    pthread_t inner;
    if (pthread_create(&inner, NULL, Inner, raw) != 0) return NULL;

    void* returned = NULL;
    if (pthread_join(inner, &returned) != 0) return NULL;

    return (void*)(uintptr_t)(((uint64_t)(uintptr_t)returned) * 3);
}

uint64_t ConcurrencyWitness(void)
{
    sharedCounter = 0;
    exchangedCounter = 0;

    pthread_t threads[CONTENDER_COUNT];
    for (int index = 0; index < CONTENDER_COUNT; ++index)
    {
        if (pthread_create(&threads[index], NULL, Contender, NULL) != 0) return 1;
    }

    for (int index = 0; index < CONTENDER_COUNT; ++index)
    {
        if (pthread_join(threads[index], NULL) != 0) return 2;
    }

    printf("guest: atomic %llu\n", (unsigned long long)sharedCounter);
    printf("guest: exchanged %llu\n", (unsigned long long)exchangedCounter);

    pthread_t outer;
    if (pthread_create(&outer, NULL, Outer, (void*)(uintptr_t)7) != 0) return 3;

    void* nested = NULL;
    if (pthread_join(outer, &nested) != 0) return 4;

    printf("guest: nested %llu\n", (unsigned long long)(uintptr_t)nested);

    return sharedCounter + exchangedCounter + (uint64_t)(uintptr_t)nested;
}

static jmp_buf recoveryPoint;
static uint64_t recoveryDepth;

static void FailDeeply(uint64_t depth, uint64_t code)
{
    recoveryDepth = depth;

    if (depth == 0) longjmp(recoveryPoint, (int)code);

    FailDeeply(depth - 1, code);
}

uint64_t JumpWitness(void)
{
    uint64_t accumulator = 0;

    volatile uint64_t preserved = 0x5a5a5a5a5a5a5a5aull;
    volatile double preservedDouble = 2.5;

    int taken = setjmp(recoveryPoint);

    if (taken == 0)
    {
        accumulator = accumulator * 31 + 1;
        FailDeeply(6, 42);
        return 0xdead;
    }

    accumulator = accumulator * 31 + (uint64_t)(unsigned int)taken;
    accumulator = accumulator * 31 + recoveryDepth;
    accumulator = accumulator * 31 + (preserved == 0x5a5a5a5a5a5a5a5aull ? 7u : 0u);
    accumulator = accumulator * 31 + (preservedDouble == 2.5 ? 9u : 0u);

    jmp_buf second;
    int again = setjmp(second);

    if (again == 0) longjmp(second, 0);

    accumulator = accumulator * 31 + (uint64_t)(unsigned int)again;

    uint64_t rounds = 0;
    jmp_buf loop;
    int state = setjmp(loop);

    if (state < 3)
    {
        rounds += 1;
        longjmp(loop, state + 1);
    }

    accumulator = accumulator * 31 + rounds;
    accumulator = accumulator * 31 + (uint64_t)(unsigned int)state;

    printf("guest: jump %llu\n", (unsigned long long)accumulator);
    return accumulator;
}

uint64_t HostCallWitness(void)
{
    printf("guest: %s %d %.4f %s\n", NAMES[0], 7, 1.5, NAMES[2]);

    uint64_t* buffer = (uint64_t*)malloc(64 * sizeof(uint64_t));
    if (!buffer) return 1;

    for (uint64_t index = 0; index < 64; ++index) buffer[index] = SelfContained(index);

    uint64_t checksum = 0;
    for (uint64_t index = 0; index < 64; ++index) checksum = checksum * 131 + buffer[index];

    free(buffer);
    printf("guest: checksum %llu\n", (unsigned long long)checksum);

    char scratch[64];
    snprintf(scratch, sizeof(scratch), "%s-%d-%.2f", NAMES[3], 42, 2.25);
    printf("guest: formatted %s\n", scratch);

    threadCounter = 1000;

    pthread_t threads[4];
    struct WorkerArguments arguments[4];

    for (uint64_t index = 0; index < 4; ++index)
    {
        arguments[index].seed = index + 1;
        arguments[index].result = 0;
        arguments[index].observedCounter = 0;

        if (pthread_create(&threads[index], NULL, Worker, &arguments[index]) != 0) return 2;
    }

    uint64_t combined = 0;
    for (uint64_t index = 0; index < 4; ++index)
    {
        void* returned = NULL;
        if (pthread_join(threads[index], &returned) != 0) return 3;

        combined = combined * 7 + (uint64_t)(uintptr_t)returned;
        printf("guest: thread %llu counter %llu\n", (unsigned long long)index, (unsigned long long)arguments[index].observedCounter);
    }

    printf("guest: main counter %llu\n", (unsigned long long)threadCounter);
    printf("guest: combined %llu\n", (unsigned long long)combined);

    return checksum ^ combined;
}

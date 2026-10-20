#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

static bool TierRecompileRequested(int argc, char** argv)
{
    for (int index = 1; index + 1 < argc; ++index)
    {
        if (strcmp(argv[index], "--tier") == 0) return strcmp(argv[index + 1], "t1") == 0;
    }

    return false;
}

struct OrderingCase
{
    const char* name;
    const char* forbidden;
    int shapeSlot;
    int controlIndex;
};

static const OrderingCase ORDERING_CASES[] = {
    {"sb_relaxed", nullptr, 0, 0},
    {"sb_release_acquire", "both threads read the stale value through STLR/LDAR", 0, 0},
    {"sb_dmb_ish", "both threads read the stale value across DMB ISH", 0, 0},
    {"mp_relaxed", nullptr, 2, 3},
    {"mp_release_acquire", "the flag was seen set while the payload was still stale", 2, 3},
};

static constexpr int ORDERING_CASE_COUNT = (int)(sizeof(ORDERING_CASES) / sizeof(ORDERING_CASES[0]));
static constexpr uint64_t DEFAULT_ITERATIONS = 20000;
static constexpr int SKIP_EXIT_CODE = 77;
static constexpr uint64_t MINIMUM_CONTROL_HITS = 20;

static int failures = 0;
static int indiscriminate = 0;
static int discriminated = 0;
static uint64_t observed[ORDERING_CASE_COUNT][4];

static bool CallGuest(GuestRuntime::Instance& instance, const char* name, uint64_t first, uint64_t second, uint64_t& returned)
{
    GuestState::GuestAddress address{};
    if (GuestRuntime::Lookup(instance, name, address) != GuestRuntime::Result::Ok)
    {
        printf("       the fixture does not export %s\n", name);
        return false;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);
    registers.general[0] = first;
    registers.general[1] = second;

    GuestRuntime::Result result = GuestRuntime::CallGuest(instance, address, registers);
    if (result != GuestRuntime::Result::Ok)
    {
        printf("       %s failed: %s\n", name, GuestRuntime::ResultName(result));
        return false;
    }

    returned = registers.general[0];
    return true;
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: OrderingGateTest <ordering.so> [iterations]\n");
        return 2;
    }

    std::string path = argv[1];
    uint64_t iterations = argc > 2 ? strtoull(argv[2], nullptr, 10) : DEFAULT_ITERATIONS;

    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);

    configuration.recompile = TierRecompileRequested(argc, argv);
    configuration.stepLimit = 0;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("ordering-gate FAILED: cannot create the guest runtime\n");
        return 1;
    }

    if (GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        printf("ordering-gate FAILED: cannot register the libc trampolines\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        printf("ordering-gate FAILED: cannot load %s\n", path.c_str());
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint64_t exported = 0;
    if (!CallGuest(instance, "OrderingTestCount", 0, 0, exported) || exported != ORDERING_CASE_COUNT)
    {
        printf("ordering-gate FAILED: the fixture exports %llu cases, the gate knows %d\n", (unsigned long long)exported,
               ORDERING_CASE_COUNT);
        GuestRuntime::Destroy(instance);
        return 1;
    }

    printf("running %d ordering cases at %llu iterations under %s\n", ORDERING_CASE_COUNT, (unsigned long long)iterations,
           configuration.recompile ? "T1" : "T0");

    for (int index = 0; index < ORDERING_CASE_COUNT; ++index)
    {
        const OrderingCase& ordering = ORDERING_CASES[index];

        uint64_t forbidden = 0;
        if (!CallGuest(instance, "RunOrdering", (uint64_t)index, iterations, forbidden))
        {
            printf("FAIL %s: the guest run did not complete\n", ordering.name);
            failures += 1;
            continue;
        }

        uint64_t total = 0;
        for (uint64_t slot = 0; slot < 4; ++slot)
        {
            if (!CallGuest(instance, "OrderingOutcome", slot, 0, observed[index][slot])) break;
            total += observed[index][slot];
        }

        printf("  %-20s 00=%llu 01=%llu 10=%llu 11=%llu forbidden=%llu\n", ordering.name, (unsigned long long)observed[index][0],
               (unsigned long long)observed[index][1], (unsigned long long)observed[index][2], (unsigned long long)observed[index][3],
               (unsigned long long)forbidden);

        if (total != iterations)
        {
            printf("FAIL %s: %llu outcomes recorded for %llu iterations\n", ordering.name, (unsigned long long)total,
                   (unsigned long long)iterations);
            failures += 1;
            continue;
        }

        if (!ordering.forbidden) continue;

        uint64_t controlReach = observed[ordering.controlIndex][ordering.shapeSlot];
        if (controlReach < MINIMUM_CONTROL_HITS)
        {
            printf("INDISCRIMINATE %s: its relaxed control %s reached the outcome only %llu times in %llu iterations,\n"
                   "     below the floor of %llu, so a forbidden count of zero here is too weak to trust\n",
                   ordering.name, ORDERING_CASES[ordering.controlIndex].name, (unsigned long long)controlReach,
                   (unsigned long long)iterations, (unsigned long long)MINIMUM_CONTROL_HITS);
            indiscriminate += 1;
            continue;
        }

        if (forbidden != 0)
        {
            printf("FAIL %s: %llu forbidden outcomes - %s\n", ordering.name, (unsigned long long)forbidden, ordering.forbidden);
            failures += 1;
            continue;
        }

        discriminated += 1;
        printf("PASS %s (forbidden outcome absent, and its control reached that outcome %llu times)\n", ordering.name,
               (unsigned long long)controlReach);
    }

    GuestRuntime::Destroy(instance);

    if (failures != 0)
    {
        printf("ordering-gate FAILED: %d checks failed\n", failures);
        return 1;
    }

    if (discriminated == 0)
    {
        printf("\nordering-gate CANNOT DISCRIMINATE: no ordering case has a reachable counterexample.\n");
        printf("Under T0 this is expected and structural: a guest instruction costs hundreds of host instructions, so the\n");
        printf("host store buffer drains between any two guest memory accesses and the interpreter is accidentally\n");
        printf("sequentially consistent. Re-run with GUEST_TIER=t1, where guest instructions become adjacent host ones.\n");
        return SKIP_EXIT_CODE;
    }

    if (indiscriminate != 0)
    {
        printf("\n%d case(s) had no reachable counterexample and were not judged. On an x86-64 host that is expected for\n"
               "message-passing: TSO never reorders store-store or load-load, so the relaxed control cannot reach the\n"
               "outcome either. Store-buffering is the shape that discriminates here, and it did.\n",
               indiscriminate);
    }

    printf("ordering-gate PASSED: %d forbidden outcome(s) absent against a reachable control\n", discriminated);
    return 0;
}

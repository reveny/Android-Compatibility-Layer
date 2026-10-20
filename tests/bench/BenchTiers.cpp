#include <algorithm>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#include "AheadOfTime/AheadOfTime.hpp"
#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

static constexpr int DEFAULT_REPEATS = 5;
static constexpr uint64_t DEFAULT_ITERATIONS = 200000;
static constexpr double REQUIRED_SPEEDUP = 25.0;
static constexpr double REQUIRED_MEMORY_SPEEDUP = 20.0;

struct Measurement
{
    double seconds = 0.0;
    uint64_t result = 0;
    uint64_t recompiled = 0;
    bool ok = false;
};

static Measurement RunTier(const std::string& path, bool translate, uint64_t iterations, bool includeTranslateTime, const char* kernel)
{
    Measurement measurement;

    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.requireTranslated = translate;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok) return measurement;

    if (GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        GuestRuntime::Destroy(instance);
        return measurement;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        GuestRuntime::Destroy(instance);
        return measurement;
    }

    AheadOfTime::Image image;
    auto translateStarted = std::chrono::steady_clock::now();

    if (translate)
    {
        AheadOfTime::Statistics translation;
        if (AheadOfTime::Translate(instance, moduleId, path, image, translation) != AheadOfTime::Result::Ok)
        {
            GuestRuntime::Destroy(instance);
            return measurement;
        }
    }

    auto translateFinished = std::chrono::steady_clock::now();

    GuestState::GuestAddress entry{};
    if (GuestRuntime::Lookup(instance, kernel, entry) != GuestRuntime::Result::Ok)
    {
        AheadOfTime::Release(image);
        GuestRuntime::Destroy(instance);
        return measurement;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);
    registers.general[0] = iterations;
    registers.general[1] = 0x0123456789ABCDEFull;

    auto started = std::chrono::steady_clock::now();
    GuestRuntime::Result called = GuestRuntime::CallGuest(instance, entry, registers);
    auto finished = std::chrono::steady_clock::now();

    if (called == GuestRuntime::Result::Ok)
    {
        measurement.seconds = std::chrono::duration<double>(finished - started).count();
        if (includeTranslateTime) measurement.seconds += std::chrono::duration<double>(translateFinished - translateStarted).count();

        measurement.result = registers.general[0];
        measurement.recompiled = GuestRuntime::GetStatistics(instance).blocksExecutedTranslated;
        measurement.ok = true;
    }

    AheadOfTime::Release(image);
    GuestRuntime::Destroy(instance);

    return measurement;
}

static double Median(std::vector<double>& samples)
{
    std::sort(samples.begin(), samples.end());
    return samples[samples.size() / 2];
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: BenchTiersTest <bench.so> [iterations] [repeats]\n");
        return 2;
    }

    std::string path = argv[1];
    uint64_t iterations = argc > 2 ? strtoull(argv[2], nullptr, 10) : DEFAULT_ITERATIONS;
    int repeats = argc > 3 ? atoi(argv[3]) : DEFAULT_REPEATS;

    static const char* const KERNELS[] = {"BenchmarkKernel", "BenchmarkMemoryKernel"};
    static const double FLOORS[] = {REQUIRED_SPEEDUP, REQUIRED_MEMORY_SPEEDUP};

    printf("%llu iterations, median of %d\n", (unsigned long long)iterations, repeats);

    bool belowFloor = false;

    for (int kernel = 0; kernel < 2; ++kernel)
    {
        std::vector<double> interpreted;
        std::vector<double> translated;
        uint64_t interpretedResult = 0;
        uint64_t translatedResult = 0;
        uint64_t blocksTranslated = 0;

        for (int repeat = 0; repeat < repeats; ++repeat)
        {
            Measurement slow = RunTier(path, false, iterations, false, KERNELS[kernel]);
            Measurement fast = RunTier(path, true, iterations, false, KERNELS[kernel]);

            if (!slow.ok || !fast.ok)
            {
                printf("bench-tiers FAILED: a guest run of %s did not complete\n", KERNELS[kernel]);
                return 1;
            }

            if (repeat != 0 && (slow.result != interpretedResult || fast.result != translatedResult))
            {
                printf("bench-tiers FAILED: %s produced a different checksum between repeats\n", KERNELS[kernel]);
                return 1;
            }

            interpreted.push_back(slow.seconds);
            translated.push_back(fast.seconds);
            interpretedResult = slow.result;
            translatedResult = fast.result;
            blocksTranslated = fast.recompiled;
        }

        if (interpretedResult != translatedResult)
        {
            printf("bench-tiers FAILED: %s returned %llu interpreted and %llu translated - a faster wrong answer is not a speedup\n",
                   KERNELS[kernel], (unsigned long long)interpretedResult, (unsigned long long)translatedResult);
            return 1;
        }

        if (blocksTranslated == 0)
        {
            printf("bench-tiers FAILED: %s executed no translated blocks, so its measurement is the interpreter twice\n",
                   KERNELS[kernel]);
            return 1;
        }

        double slowMedian = Median(interpreted);
        double fastMedian = Median(translated);
        double speedup = fastMedian > 0.0 ? slowMedian / fastMedian : 0.0;

        Measurement cold = RunTier(path, true, iterations, true, KERNELS[kernel]);

        printf("%s checksum %llu\n", KERNELS[kernel], (unsigned long long)interpretedResult);
        printf("  interpreted        %8.4f s\n", slowMedian);
        printf("  translated         %8.4f s   (%llu blocks entered)\n", fastMedian, (unsigned long long)blocksTranslated);
        printf("  incl. translation  %8.4f s\n", cold.ok ? cold.seconds : 0.0);
        printf("  speedup            %8.2fx   (floor %.1fx)\n", speedup, FLOORS[kernel]);

        if (speedup < FLOORS[kernel])
        {
            printf("bench-tiers FAILED: %s at %.2fx is below the %.1fx floor\n", KERNELS[kernel], speedup, FLOORS[kernel]);
            belowFloor = true;
        }
    }

    if (belowFloor) return 1;

    printf("bench-tiers PASSED: translated code clears the floor on a register kernel and a memory kernel\n");
    return 0;
}

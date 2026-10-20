#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

static bool gRecompile = false;

static bool TierRecompileRequested(int argc, char** argv)
{
    for (int index = 1; index + 1 < argc; ++index)
    {
        if (strcmp(argv[index], "--tier") == 0) return strcmp(argv[index + 1], "t1") == 0;
    }

    return false;
}

extern "C" uint64_t HostCallWitness(void);
extern "C" uint64_t ConcurrencyWitness(void);
extern "C" uint64_t JumpWitness(void);

static int RunHost()
{
    uint64_t returned = HostCallWitness();
    printf("result %llu\n", (unsigned long long)returned);

    uint64_t concurrency = ConcurrencyWitness();
    printf("concurrency %llu\n", (unsigned long long)concurrency);

    uint64_t jump = JumpWitness();
    printf("jump %llu\n", (unsigned long long)jump);

    return 0;
}

static int RunGuest(const std::string& path)
{
    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);

    configuration.recompile = gRecompile;

    GuestRuntime::Instance instance;
    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "cannot create the guest runtime\n");
        return 1;
    }

    if (GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        fprintf(stderr, "cannot register the libc trampolines\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    GuestRuntime::Result loaded = GuestRuntime::Load(instance, path, moduleId);
    if (loaded != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "cannot load %s: %s\n", path.c_str(), GuestRuntime::ResultName(loaded));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    if (GuestRuntime::RunInitializers(instance, moduleId) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest initializers faulted\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::GuestAddress entry{};
    if (GuestRuntime::Lookup(instance, "HostCallWitness", entry) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest library does not export HostCallWitness\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);

    GuestRuntime::Result called = GuestRuntime::CallGuest(instance, entry, registers);
    if (called != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest call failed: %s\n", GuestRuntime::ResultName(called));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    printf("result %llu\n", (unsigned long long)registers.general[0]);
    fflush(stdout);

    GuestState::GuestAddress concurrencyEntry{};
    if (GuestRuntime::Lookup(instance, "ConcurrencyWitness", concurrencyEntry) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest library does not export ConcurrencyWitness\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::Registers concurrencyRegisters;
    GuestState::Reset(concurrencyRegisters);

    GuestRuntime::Result concurrency = GuestRuntime::CallGuest(instance, concurrencyEntry, concurrencyRegisters);
    if (concurrency != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest concurrency call failed: %s\n", GuestRuntime::ResultName(concurrency));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    printf("concurrency %llu\n", (unsigned long long)concurrencyRegisters.general[0]);
    fflush(stdout);

    GuestState::GuestAddress jumpEntry{};
    if (GuestRuntime::Lookup(instance, "JumpWitness", jumpEntry) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest library does not export JumpWitness\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    GuestState::Registers jumpRegisters;
    GuestState::Reset(jumpRegisters);

    GuestRuntime::Result jumped = GuestRuntime::CallGuest(instance, jumpEntry, jumpRegisters);
    if (jumped != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "the guest jump call failed: %s\n", GuestRuntime::ResultName(jumped));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    printf("jump %llu\n", (unsigned long long)jumpRegisters.general[0]);
    fflush(stdout);

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(instance), moduleId);
    const GuestRuntime::Statistics& statistics = GuestRuntime::GetStatistics(instance);

    fprintf(stderr, "guest: %llu blocks decoded, %llu executed, %llu host calls, %llu threads, %llu TLS relocations\n",
            (unsigned long long)statistics.blocksDecoded, (unsigned long long)statistics.blocksExecuted,
            (unsigned long long)statistics.hostCalls, (unsigned long long)statistics.threadsStarted,
            (unsigned long long)(library ? library->relocationCounts.threadLocal : 0));

    fprintf(stderr, "guest: %llu blocks recompiled, %llu block entries executed natively\n",
            (unsigned long long)statistics.blocksRecompiled, (unsigned long long)statistics.blocksExecutedNative);

    GuestRuntime::Destroy(instance);
    return 0;
}

int main(int argc, char** argv)
{
    gRecompile = TierRecompileRequested(argc, argv);

    if (argc >= 2 && strcmp(argv[1], "--host") == 0) return RunHost();
    if (argc >= 3 && strcmp(argv[1], "--guest") == 0) return RunGuest(argv[2]);

    fprintf(stderr, "usage: GuestLibHostCallTest --host | --guest <hello.so>\n");
    return 2;
}

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <string>

#include "AheadOfTime/AheadOfTime.hpp"
#include "Diagnostics/Diagnostics.hpp"
#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "Settings/Settings.hpp"

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: TombstoneHarness <hello.so> [bad-address] [tombstone-file]\n");
        return 2;
    }

    const std::string path = argv[1];
    const uint64_t target = argc > 2 ? strtoull(argv[2], nullptr, 0) : 0x10;

    if (argc > 3) Settings::Set(Diagnostics::TOMBSTONE_SETTING, argv[3]);

    std::string directory = ".";
    const size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    Diagnostics::Initialize(Diagnostics::DEFAULT_CAPACITY);

    if (!Diagnostics::CaptureStandardError())
    {
        printf("harness: standard error could not be captured, so no tombstone is possible\n");
        return 1;
    }

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.stubUnresolvedSymbols = true;
    configuration.requireTranslated = true;

    GuestRuntime::Instance instance;

    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        Diagnostics::ReleaseStandardError();
        printf("harness: cannot create the guest runtime\n");
        return 1;
    }

    if (GuestRuntime::RegisterPlatformProxies(instance) != GuestRuntime::Result::Ok ||
        GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        Diagnostics::ReleaseStandardError();
        printf("harness: cannot register the guest platform surface\n");
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;

    if (GuestRuntime::Load(instance, path, moduleId) != GuestRuntime::Result::Ok)
    {
        Diagnostics::ReleaseStandardError();
        printf("harness: cannot load %s\n", path.c_str());
        return 1;
    }

    AheadOfTime::Image image;
    AheadOfTime::Statistics statistics;

    if (AheadOfTime::Translate(instance, moduleId, path, image, statistics) != AheadOfTime::Result::Ok)
    {
        Diagnostics::ReleaseStandardError();
        printf("harness: cannot translate %s ahead of time\n", path.c_str());
        return 1;
    }

    GuestState::GuestAddress entry{};

    if (GuestRuntime::Lookup(instance, "FaultWitness", entry) != GuestRuntime::Result::Ok)
    {
        Diagnostics::ReleaseStandardError();
        printf("harness: the fixture does not export FaultWitness\n");
        return 1;
    }

    fprintf(stderr, "[harness] about to read guest address %016llx, which is not mapped\n", (unsigned long long)target);
    fflush(stderr);

    GuestState::Registers registers{};
    registers.general[0] = target;

    GuestRuntime::CallGuest(instance, entry, registers);

    fprintf(stderr, "[harness] the guest returned rather than faulting, so this proves nothing\n");
    fflush(stderr);

    Diagnostics::ReleaseStandardError();
    GuestRuntime::Destroy(instance);

    return 3;
}

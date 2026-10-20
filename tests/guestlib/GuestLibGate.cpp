#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>

#include "AbiBridge/AbiBridge.hpp"
#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

extern "C" uint64_t SelfContained(uint64_t seed);
extern "C" uint64_t NameLengthSum(uint64_t rotation);
extern "C" uint64_t RelocationWitness(uint64_t seed);

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static bool CallGuestFunction(GuestRuntime::Instance& instance, const char* name, uint64_t argument, uint64_t& returned)
{
    GuestState::GuestAddress address{};
    if (GuestRuntime::Lookup(instance, name, address) != GuestRuntime::Result::Ok)
    {
        printf("       cannot find guest symbol %s\n", name);
        return false;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);
    registers.general[0] = argument;

    GuestRuntime::Result result = GuestRuntime::CallGuest(instance, address, registers);
    if (result != GuestRuntime::Result::Ok)
    {
        printf("       guest call to %s failed: %s\n", name, GuestRuntime::ResultName(result));
        return false;
    }

    returned = registers.general[0];
    return true;
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: GuestLibGateTest <hello.so>\n");
        return 2;
    }

    std::string path = argv[1];
    std::string directory = ".";
    size_t slash = path.find_last_of('/');
    if (slash != std::string::npos) directory = path.substr(0, slash);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);

    GuestRuntime::Instance instance;
    GuestRuntime::Result created = GuestRuntime::Create(configuration, instance);
    if (created != GuestRuntime::Result::Ok)
    {
        printf("guest-elf-gate FAILED: cannot create guest runtime: %s\n", GuestRuntime::ResultName(created));
        return 1;
    }

    GuestLibc::Result registered = GuestLibc::RegisterAll(instance);
    if (registered != GuestLibc::Result::Ok)
    {
        printf("guest-elf-gate FAILED: cannot register libc trampolines: %s\n", GuestLibc::ResultName(registered));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    GuestRuntime::Result loaded = GuestRuntime::Load(instance, path, moduleId);
    if (loaded != GuestRuntime::Result::Ok)
    {
        printf("guest-elf-gate FAILED: cannot load %s: %s\n", path.c_str(), GuestRuntime::ResultName(loaded));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(instance), moduleId);
    if (!library)
    {
        printf("guest-elf-gate FAILED: the loaded module has no handle\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    printf("loaded %s: bias %016llx, %zu segments, %zu dynamic symbols\n", library->soname.c_str(), (unsigned long long)library->loadBias,
           library->segments.size(), library->exports.size());
    printf("relocations: relative %llu, relr %llu, glob_dat %llu, jump_slot %llu, abs64 %llu\n",
           (unsigned long long)library->relocationCounts.relative, (unsigned long long)library->relocationCounts.relrRelative,
           (unsigned long long)library->relocationCounts.globalData, (unsigned long long)library->relocationCounts.jumpSlot,
           (unsigned long long)library->relocationCounts.absolute);

    Check(library->segments.size() >= 2, "the image mapped at least two segments");
    Check(library->loadBias != 0, "the image was relocated away from its link address");

    uint64_t relativeTotal = library->relocationCounts.relative + library->relocationCounts.relrRelative;
    Check(relativeTotal >= 4, "the pointer tables produced relative relocations");

    for (uint64_t seed = 0; seed < 8; ++seed)
    {
        uint64_t guest = 0;
        char label[64];
        snprintf(label, sizeof(label), "SelfContained(%llu) matches the host build", (unsigned long long)seed);

        if (!CallGuestFunction(instance, "SelfContained", seed, guest))
        {
            Check(false, label);
            continue;
        }

        Check(guest == SelfContained(seed), label);
    }

    for (uint64_t rotation = 0; rotation < 4; ++rotation)
    {
        uint64_t guest = 0;
        char label[64];
        snprintf(label, sizeof(label), "NameLengthSum(%llu) matches the host build", (unsigned long long)rotation);

        if (CallGuestFunction(instance, "NameLengthSum", rotation, guest))
            Check(guest == NameLengthSum(rotation), label);
        else
            Check(false, label);
    }

    uint64_t guestWitness = 0;
    if (CallGuestFunction(instance, "RelocationWitness", 3, guestWitness))
        Check(guestWitness == RelocationWitness(3), "RelocationWitness matches the host build");
    else
        Check(false, "RelocationWitness matches the host build");

    GuestState::GuestAddress selfContained{};
    if (GuestRuntime::Lookup(instance, "SelfContained", selfContained) == GuestRuntime::Result::Ok)
    {
        AbiBridge::Argument word;
        word.argumentClass = AbiBridge::ArgumentClass::Integer;
        word.sizeInBytes = 8;
        word.alignmentInBytes = 8;

        AbiBridge::Signature signature;
        signature.returnValue = word;
        signature.arguments[0] = word;
        signature.totalArgumentCount = 1;
        signature.namedArgumentCount = 1;

        uint64_t slots[2] = {5, 0};
        AbiBridge::ArgumentBuffer buffer;
        buffer.slots = slots;
        buffer.slotCount = 1;
        buffer.slotStrideBytes = sizeof(uint64_t) * 2;

        GuestState::GuestCaller caller = GuestRuntime::Caller(instance);
        uint64_t returned = 0;

        AbiBridge::Result bridged =
            AbiBridge::CallGuestFromHost(caller, selfContained, signature, buffer, &returned, sizeof(returned));

        Check(bridged == AbiBridge::Result::Ok && returned == SelfContained(5),
              "AbiBridge::CallGuestFromHost enters guest code from a host thread");
    }
    else
    {
        Check(false, "AbiBridge::CallGuestFromHost enters guest code from a host thread");
    }

    const GuestRuntime::Statistics& statistics = GuestRuntime::GetStatistics(instance);
    printf("decoded %llu blocks, executed %llu, %llu host calls\n", (unsigned long long)statistics.blocksDecoded,
           (unsigned long long)statistics.blocksExecuted, (unsigned long long)statistics.hostCalls);

    Check(statistics.blocksDecoded > 0, "the dispatch loop decoded guest blocks on demand");
    Check(statistics.blocksExecuted > statistics.blocksDecoded, "the block cache served repeat visits");

    GuestRuntime::Destroy(instance);

    printf("%s: %d checks failed\n", failures == 0 ? "guest-elf-gate PASSED" : "guest-elf-gate FAILED", failures);
    return failures == 0 ? 0 : 1;
}

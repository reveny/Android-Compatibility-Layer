#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

#include "ArtRuntime/ArtRuntime.hpp"
#include "ArtRuntime/IcuSetup.hpp"
#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/GuestLibc.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "JniBridge/JniBridge.hpp"

static bool TierRecompileRequested(int argc, char** argv)
{
    for (int index = 1; index + 1 < argc; ++index)
    {
        if (strcmp(argv[index], "--tier") == 0) return strcmp(argv[index + 1], "t1") == 0;
    }

    return false;
}

static constexpr uint64_t EXPECTED_JNI_VERSION = 0x00010006;

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

int main(int argc, char** argv)
{
    if (argc < 5)
    {
        fprintf(stderr, "usage: JniBridgeGateTest <host-root> <apk> <guest-lib-dir> <shim-dir>\n");
        return 2;
    }

    std::string hostRoot = argv[1];
    std::string apkPath = argv[2];
    std::string directory = argv[3];
    std::string shimDirectory = argv[4];
    std::string libraryPath = directory + "/libhelloworld.so";

    if (IcuSetup::Configure(hostRoot) != IcuSetup::Result::Ok)
    {
        printf("guest-jni-gate FAILED: cannot configure ICU\n");
        return 1;
    }

    ArtRuntime::Configuration artConfiguration = ArtRuntime::DefaultConfiguration(hostRoot);
    artConfiguration.classPath.push_back(apkPath);
    artConfiguration.nativeLibraryPath.insert(artConfiguration.nativeLibraryPath.begin(), shimDirectory);

    ArtRuntime::Vm vm;
    if (ArtRuntime::Start(artConfiguration, vm) != ArtRuntime::Result::Ok)
    {
        printf("m8a-gate FAILED: cannot start host ART\n");
        return 1;
    }

    printf("host ART started, JavaVM at %p\n", (void*)vm.javaVm);

    GuestRuntime::Configuration configuration;
    configuration.searchPaths.push_back(directory);
    configuration.stubUnresolvedSymbols = true;

    configuration.recompile = TierRecompileRequested(argc, argv);

    GuestRuntime::Instance instance;
    GuestRuntime::Result created = GuestRuntime::Create(configuration, instance);
    Check(created == GuestRuntime::Result::Ok, "the guest arena maps in a process that already hosts a Java heap");

    if (created != GuestRuntime::Result::Ok)
    {
        printf("m8a-gate FAILED: %s\n", GuestRuntime::ResultName(created));
        return 1;
    }

    if (GuestRuntime::RegisterPlatformProxies(instance) != GuestRuntime::Result::Ok ||
        GuestLibc::RegisterAll(instance) != GuestLibc::Result::Ok)
    {
        printf("m8a-gate FAILED: cannot register the guest platform surface\n");
        GuestRuntime::Destroy(instance);
        return 1;
    }

    JniBridge::Instance bridge;
    JniBridge::Result bridged = JniBridge::Create(instance, vm.javaVm, bridge);
    Check(bridged == JniBridge::Result::Ok, "the guest-visible JavaVM and JNIEnv objects are built in the arena");

    if (bridged != JniBridge::Result::Ok)
    {
        printf("m8a-gate FAILED: %s\n", JniBridge::ResultName(bridged));
        GuestRuntime::Destroy(instance);
        return 1;
    }

    Check(JniBridge::InstallLibraryLoader(bridge, directory) == JniBridge::Result::Ok, "the guest library loader is installed");

    jclass owner = vm.jniEnv->FindClass("com/reveny/helloworld/NativeLib");
    Check(owner != nullptr, "host ART can load the fixture's NativeLib class");

    if (owner)
    {
        jmethodID stringFromJni = vm.jniEnv->GetStaticMethodID(owner, "stringFromJNI", "()Ljava/lang/String;");
        if (!stringFromJni) ArtRuntime::DescribeAndClearException(vm);

        Check(stringFromJni != nullptr, "stringFromJNI is bound in host ART after the guest registered it");

        if (stringFromJni)
        {
            jstring produced = (jstring)vm.jniEnv->CallStaticObjectMethod(owner, stringFromJni);

            if (ArtRuntime::DescribeAndClearException(vm))
            {
                Check(false, "calling stringFromJNI from Java did not throw");
            }
            else if (!produced)
            {
                Check(false, "stringFromJNI returned a string");
            }
            else
            {
                const char* text = vm.jniEnv->GetStringUTFChars(produced, nullptr);
                printf("  stringFromJNI() returned \"%s\"\n", text ? text : "(null)");

                Check(text != nullptr && strcmp(text, "Hello World from C++") == 0,
                      "arm64 guest code called back into Java and returned \"Hello World from C++\"");

                if (text) vm.jniEnv->ReleaseStringUTFChars(produced, text);
            }
        }
    }

    JniBridge::Statistics statistics = JniBridge::GetStatistics(bridge);
    printf("  guest libraries=%llu JNI_OnLoad calls=%llu version=%08llx slot calls=%llu unimplemented=%llu natives=%llu\n",
           (unsigned long long)statistics.guestLibrariesLoaded, (unsigned long long)statistics.guestJniOnLoadCalls,
           (unsigned long long)statistics.lastJniOnLoadVersion, (unsigned long long)statistics.slotCalls,
           (unsigned long long)statistics.unimplementedSlotCalls, (unsigned long long)statistics.registeredNatives);

    Check(statistics.guestLibrariesLoaded == 1, "System.loadLibrary routed one arm64 library into the guest runtime");
    Check(statistics.guestJniOnLoadCalls == 1, "guest JNI_OnLoad ran exactly once, driven by System.loadLibrary");
    Check(statistics.lastJniOnLoadVersion == EXPECTED_JNI_VERSION, "guest JNI_OnLoad returned JNI_VERSION_1_6");
    Check(statistics.slotCalls > 0, "the guest reached host JNI through the guest-visible vtable");
    Check(statistics.unimplementedSlotCalls == 0, "the whole round trip touched no unimplemented JNI slot");
    Check(statistics.registeredNatives == 1, "RegisterNatives bound exactly one method into host ART");

    const std::vector<JniBridge::RegisteredNative>& natives = JniBridge::RegisteredNatives(bridge);
    if (!natives.empty())
    {
        const JniBridge::RegisteredNative& native = natives.front();
        Check(native.methodName == "stringFromJNI", "the registered native is stringFromJNI");
        Check(native.descriptor == "()Ljava/lang/String;", "its descriptor survived the guest-memory read");
        Check((uint64_t)native.guestFunction != 0, "its function pointer is a guest address");
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    const ElfLoader::Namespace& libraries = GuestRuntime::LibraryNamespace(instance);
    for (const ElfLoader::Library& candidate : libraries.loaded)
    {
        if (candidate.soname.find("helloworld") != std::string::npos) moduleId = candidate.moduleId;
    }

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(instance), moduleId);
    Check(library != nullptr, "the APK's own arm64 libhelloworld.so is loaded in the guest namespace");

    if (library)
    {
        const ElfLoader::RelocationCounts& counts = library->relocationCounts;
        printf("  relocations: relative=%llu globalData=%llu jumpSlot=%llu absolute=%llu stub=%llu data=%llu\n",
               (unsigned long long)counts.relative, (unsigned long long)counts.globalData, (unsigned long long)counts.jumpSlot,
               (unsigned long long)counts.absolute, (unsigned long long)counts.resolvedToHostStub,
               (unsigned long long)counts.resolvedToHostData);

        Check(counts.resolvedToHostData == 1, "__sF resolved to guest data, not to an executable stub");
    }

    (void)libraryPath;

    JniBridge::Destroy(bridge);
    GuestRuntime::Destroy(instance);

    printf("%s: %d checks failed\n", failures == 0 ? "guest-jni-gate PASSED" : "guest-jni-gate FAILED", failures);
    return failures == 0 ? 0 : 1;
}

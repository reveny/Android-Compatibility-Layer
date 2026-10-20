#include "ArtRuntime/ArtRuntime.hpp"
#include "ArtRuntime/IcuSetup.hpp"

#include <cstdio>
#include <cstdlib>

int main(int argc, char** argv)
{
    if (argc < 3)
    {
        fprintf(stderr, "usage: %s <host-root> <dex-or-apk> [MainClass]\n", argv[0]);
        return 2;
    }

    std::string hostRoot = argv[1];
    std::string dexPath = argv[2];
    const char* mainClassName = argc > 3 ? argv[3] : "Probe";

    IcuSetup::Result icuResult = IcuSetup::Configure(hostRoot);
    if (icuResult != IcuSetup::Result::Ok)
    {
        fprintf(stderr, "icu setup failed: %s\n", IcuSetup::ResultName(icuResult));
        return 1;
    }
    printf("icu configured\n");

    ArtRuntime::Configuration configuration = ArtRuntime::DefaultConfiguration(hostRoot);
    configuration.classPath.push_back(dexPath);

    ArtRuntime::Vm vm;
    ArtRuntime::Result result = ArtRuntime::Start(configuration, vm);
    if (result != ArtRuntime::Result::Ok)
    {
        fprintf(stderr, "vm start failed: %s\n", ArtRuntime::ResultName(result));
        return 1;
    }
    printf("vm started\n");

    jclass mainClass = nullptr;
    result = ArtRuntime::FindClass(vm, mainClassName, mainClass);
    if (result != ArtRuntime::Result::Ok)
    {
        fprintf(stderr, "find class failed: %s\n", ArtRuntime::ResultName(result));
        return 1;
    }
    printf("found class %s\n", mainClassName);

    jmethodID main = vm.jniEnv->GetStaticMethodID(mainClass, "main", "([Ljava/lang/String;)V");
    if (!main)
    {
        ArtRuntime::DescribeAndClearException(vm);
        fprintf(stderr, "no main method\n");
        return 1;
    }

    jclass stringClass = vm.jniEnv->FindClass("java/lang/String");
    jobjectArray args = vm.jniEnv->NewObjectArray(0, stringClass, nullptr);
    vm.jniEnv->CallStaticVoidMethod(mainClass, main, args);

    if (ArtRuntime::DescribeAndClearException(vm))
    {
        fprintf(stderr, "main threw\n");
        return 1;
    }

    printf("main returned cleanly\n");
    ArtRuntime::Stop(vm);
    return 0;
}

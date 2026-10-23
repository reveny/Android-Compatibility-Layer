#include "ArtRuntime/ArtRuntime.hpp"

#include <cstdio>
#include <cstdlib>
#include <dlfcn.h>
#include <sys/stat.h>

bool ArtRuntime::PathExists(const std::string& path)
{
    struct stat info;
    return stat(path.c_str(), &info) == 0;
}

std::string ArtRuntime::Join(const std::vector<std::string>& parts, char separator)
{
    std::string result;
    for (const std::string& part : parts)
    {
        if (!result.empty()) result += separator;
        result += part;
    }
    return result;
}

static const char* CRYPTO_LIBRARY = "libjavacrypto.so";

std::string ArtRuntime::ConscryptPrebuiltDirectory(const std::string& hostRoot)
{
    if (PathExists(hostRoot + "/lib64/" + CRYPTO_LIBRARY)) return std::string();

    std::string tree = hostRoot;

    size_t out = tree.find("/out/host/");
    if (out == std::string::npos) return std::string();

    tree.resize(out);

    std::string directory = tree + "/prebuilts/module_sdk/conscrypt/current/test-exports/linux_glibc/x86_64/lib";
    if (!PathExists(directory + "/" + CRYPTO_LIBRARY)) return std::string();

    return directory;
}

static void PreloadConscrypt(const std::string& hostRoot)
{
    std::string directory = ArtRuntime::ConscryptPrebuiltDirectory(hostRoot);
    if (directory.empty()) return;

    std::string file = directory + "/" + CRYPTO_LIBRARY;

    dlerror();
    if (dlopen(file.c_str(), RTLD_NOW | RTLD_LOCAL)) return;

    const char* error = dlerror();
    fprintf(stderr, "[art] %s could not be preloaded: %s\n", file.c_str(), error ? error : "unknown");
}

static void DescribeAbsentCrypto(const std::string& hostRoot)
{
    fprintf(stderr,
            "[art] %s/lib64/%s is absent and no module sdk prebuilt stands in for it, so conscrypt has its\n"
            "[art] java half and not its native one. The first SecureRandom an app asks for will take a link\n"
            "[art] error inside the provider list and never recover. Dropping the jar from the boot class path\n"
            "[art] instead does not work: the boot image is compiled against that exact list, and changing it\n"
            "[art] makes the runtime recompile the world on every start.\n",
            hostRoot.c_str(), CRYPTO_LIBRARY);
}

ArtRuntime::Configuration ArtRuntime::DefaultConfiguration(const std::string& hostRoot)
{
    Configuration configuration;
    configuration.hostRoot = hostRoot;

    const char* bootJars[] = {"core-oj", "core-libart", "okhttp", "bouncycastle", "apache-xml", "conscrypt", "core-icu4j"};
    for (const char* jar : bootJars)
    {
        configuration.bootClassPath.push_back(hostRoot + "/framework/" + jar + "-hostdex.jar");
    }

    configuration.nativeLibraryPath.push_back(hostRoot + "/lib64");

    if (!PathExists(hostRoot + "/lib64/" + CRYPTO_LIBRARY))
    {
        std::string prebuilt = ConscryptPrebuiltDirectory(hostRoot);

        if (prebuilt.empty())
        {
            DescribeAbsentCrypto(hostRoot);
        }
        else
        {
            configuration.nativeLibraryPath.push_back(prebuilt);

            fprintf(stderr,
                    "[art] %s was never built here, so conscrypt takes its native half from the module sdk\n"
                    "[art] prebuilt this manifest already checked out: %s\n",
                    CRYPTO_LIBRARY, prebuilt.c_str());
        }
    }

    return configuration;
}

ArtRuntime::Result ArtRuntime::Start(const Configuration& configuration, Vm& vm)
{
    if (vm.javaVm) return Result::AlreadyStarted;
    if (configuration.hostRoot.empty() || !PathExists(configuration.hostRoot)) return Result::HostRootMissing;
    if (configuration.bootClassPath.empty()) return Result::BootClassPathEmpty;

    for (const std::string& jar : configuration.bootClassPath)
    {
        if (PathExists(jar)) continue;
        fprintf(stderr, "[art] boot jar missing: %s\n", jar.c_str());
        return Result::BootJarMissing;
    }

    setenv("ANDROID_ROOT", configuration.hostRoot.c_str(), 1);
    setenv("ANDROID_ART_ROOT", configuration.hostRoot.c_str(), 1);
    setenv("ANDROID_I18N_ROOT", (configuration.hostRoot + "/com.android.i18n").c_str(), 1);
    setenv("ANDROID_TZDATA_ROOT", (configuration.hostRoot + "/com.android.tzdata").c_str(), 1);

    std::string libraryFile = configuration.hostRoot + "/lib64/" + RUNTIME_LIBRARY;
    dlerror();
    vm.libraryHandle = dlopen(libraryFile.c_str(), RTLD_NOW | RTLD_GLOBAL | RTLD_DEEPBIND);
    if (!vm.libraryHandle)
    {
        const char* error = dlerror();
        fprintf(stderr, "[art] dlopen %s failed: %s\n", libraryFile.c_str(), error ? error : "unknown");
        return Result::LibraryNotFound;
    }

    PreloadConscrypt(configuration.hostRoot);

    dlerror();
    CreateJavaVmFn createVm = (CreateJavaVmFn)dlsym(vm.libraryHandle, "JNI_CreateJavaVM");
    if (!createVm)
    {
        const char* error = dlerror();
        fprintf(stderr, "[art] dlsym JNI_CreateJavaVM failed: %s\n", error ? error : "unknown");
        return Result::SymbolNotFound;
    }

    std::vector<std::string> strings;
    strings.push_back("-Xbootclasspath:" + Join(configuration.bootClassPath, ':'));
    strings.push_back("-Xnoimage-dex2oat");
    strings.push_back(configuration.useJitCompilation ? "-Xusejit:true" : "-Xusejit:false");

    if (!configuration.classPath.empty())
    {
        strings.push_back("-classpath");
        strings.push_back(Join(configuration.classPath, ':'));
    }
    if (!configuration.nativeLibraryPath.empty())
    {
        strings.push_back("-Djava.library.path=" + Join(configuration.nativeLibraryPath, ':'));
    }
    if (configuration.checkJni) strings.push_back("-Xcheck:jni");
    if (!configuration.verboseTags.empty()) strings.push_back("-verbose:" + Join(configuration.verboseTags, ','));

    if (configuration.reportOptions)
    {
        for (const std::string& option : strings) fprintf(stderr, "[jni] vm option %s\n", option.c_str());
    }

    std::vector<JavaVMOption> options;
    options.reserve(strings.size());
    for (const std::string& string : strings)
    {
        JavaVMOption option;
        option.optionString = const_cast<char*>(string.c_str());
        option.extraInfo = nullptr;
        options.push_back(option);
    }

    JavaVMInitArgs args;
    args.version = JNI_VERSION;
    args.nOptions = (jint)options.size();
    args.options = options.data();
    args.ignoreUnrecognized = JNI_FALSE;

    if (createVm(&vm.javaVm, &vm.jniEnv, &args) != JNI_OK)
    {
        vm.javaVm = nullptr;
        vm.jniEnv = nullptr;
        return Result::VmCreationFailed;
    }

    return Result::Ok;
}

void ArtRuntime::Stop(Vm& vm)
{
    if (!vm.javaVm) return;
    vm.javaVm->DetachCurrentThread();
    vm.javaVm->DestroyJavaVM();
    vm.javaVm = nullptr;
    vm.jniEnv = nullptr;
}

ArtRuntime::Result ArtRuntime::FindClass(const Vm& vm, const char* className, jclass& classOut)
{
    if (!vm.jniEnv) return Result::NotStarted;

    classOut = vm.jniEnv->FindClass(className);
    if (classOut) return Result::Ok;

    DescribeAndClearException(vm);
    fprintf(stderr, "[art] class not found: %s\n", className);
    return Result::ClassNotFound;
}

ArtRuntime::Result ArtRuntime::RegisterNatives(const Vm& vm, const NativeMethodTable& table)
{
    jclass registeredClass = nullptr;
    Result result = FindClass(vm, table.className, registeredClass);
    if (result != Result::Ok) return result;

    jint status = vm.jniEnv->RegisterNatives(registeredClass, table.methods, table.methodCount);
    vm.jniEnv->DeleteLocalRef(registeredClass);

    if (status == JNI_OK) return Result::Ok;

    DescribeAndClearException(vm);
    fprintf(stderr, "[art] RegisterNatives failed for %s\n", table.className);
    return Result::RegistrationFailed;
}

ArtRuntime::Result ArtRuntime::RegisterAll(const Vm& vm, const NativeMethodTable* tables, int tableCount)
{
    for (int index = 0; index < tableCount; ++index)
    {
        Result result = RegisterNatives(vm, tables[index]);
        if (result != Result::Ok) return result;
    }
    return Result::Ok;
}

bool ArtRuntime::DescribeAndClearException(const Vm& vm)
{
    if (!vm.jniEnv) return false;
    if (!vm.jniEnv->ExceptionCheck()) return false;

    vm.jniEnv->ExceptionDescribe();
    vm.jniEnv->ExceptionClear();
    return true;
}

const char* ArtRuntime::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::HostRootMissing: return "HostRootMissing";
        case Result::LibraryNotFound: return "LibraryNotFound";
        case Result::SymbolNotFound: return "SymbolNotFound";
        case Result::BootClassPathEmpty: return "BootClassPathEmpty";
        case Result::BootJarMissing: return "BootJarMissing";
        case Result::VmCreationFailed: return "VmCreationFailed";
        case Result::AlreadyStarted: return "AlreadyStarted";
        case Result::NotStarted: return "NotStarted";
        case Result::ClassNotFound: return "ClassNotFound";
        case Result::RegistrationFailed: return "RegistrationFailed";
    }
    return "Unknown";
}

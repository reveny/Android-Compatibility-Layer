#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <jni.h>

namespace ArtRuntime
{
    inline constexpr const char* RUNTIME_LIBRARY = "libart.so";
    inline constexpr int JNI_VERSION = JNI_VERSION_1_6;

    using CreateJavaVmFn = jint (*)(JavaVM**, JNIEnv**, void*);

    enum class Result : uint8_t
    {
        Ok,
        HostRootMissing,
        LibraryNotFound,
        SymbolNotFound,
        BootClassPathEmpty,
        BootJarMissing,
        VmCreationFailed,
        AlreadyStarted,
        NotStarted,
        ClassNotFound,
        RegistrationFailed
    };

    struct Configuration
    {
        std::string hostRoot;
        std::vector<std::string> bootClassPath;
        std::vector<std::string> classPath;
        std::vector<std::string> nativeLibraryPath;
        std::vector<std::string> verboseTags;
        bool useJitCompilation = false;
        bool checkJni = false;
        bool reportOptions = false;
    };

    struct Vm
    {
        void* libraryHandle = nullptr;
        JavaVM* javaVm = nullptr;
        JNIEnv* jniEnv = nullptr;
    };

    struct NativeMethodTable
    {
        const char* className = nullptr;
        const JNINativeMethod* methods = nullptr;
        int methodCount = 0;
    };

    Result Start(const Configuration& configuration, Vm& vm);
    void Stop(Vm& vm);

    Result RegisterNatives(const Vm& vm, const NativeMethodTable& table);
    Result RegisterAll(const Vm& vm, const NativeMethodTable* tables, int tableCount);

    Result FindClass(const Vm& vm, const char* className, jclass& classOut);
    bool DescribeAndClearException(const Vm& vm);

    Configuration DefaultConfiguration(const std::string& hostRoot);
    std::string ConscryptPrebuiltDirectory(const std::string& hostRoot);

    bool PathExists(const std::string& path);
    std::string Join(const std::vector<std::string>& parts, char separator);

    const char* ResultName(Result result);
}

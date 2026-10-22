#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <jni.h>

#include "GuestRuntime/GuestRuntime.hpp"

namespace JniBridge
{
    inline constexpr int NATIVE_INTERFACE_SLOTS = (int)(sizeof(JNINativeInterface) / sizeof(void*));
    inline constexpr int INVOKE_INTERFACE_SLOTS = (int)(sizeof(JNIInvokeInterface) / sizeof(void*));

    enum class Result : uint8_t
    {
        Ok,
        NotInitialized,
        OutOfGuestMemory,
        RegistrationFailed,
        BadDescriptor,
        GuestFaulted,
        NoJavaVm,
        NoLibraryLoader,
        LibraryLoadFailed
    };

    struct RegisteredNative
    {
        std::string methodName;
        std::string descriptor;
        GuestState::GuestAddress guestFunction{};
        void* declaringClass = nullptr;
    };

    struct Statistics
    {
        uint64_t slotCalls = 0;
        uint64_t unimplementedSlotCalls = 0;
        uint64_t registeredNatives = 0;
        uint64_t guestLibrariesLoaded = 0;
        uint64_t guestJniOnLoadCalls = 0;
        uint64_t lastJniOnLoadVersion = 0;
    };

    struct Instance
    {
        void* implementation = nullptr;

        Instance() = default;
        Instance(const Instance&) = delete;
        Instance& operator=(const Instance&) = delete;
    };

    Result Create(GuestRuntime::Instance& runtime, JavaVM* javaVm, Instance& bridge);
    void Destroy(Instance& bridge);

    using LibraryTranslator = bool (*)(void* context, GuestRuntime::Instance& runtime, uint32_t moduleId, const char* imagePath);

    Result InstallLibraryLoader(Instance& bridge, const std::string& guestLibraryDirectory);
    void InstallLibraryTranslator(Instance& bridge, LibraryTranslator translator, void* context);
    void RemoveLibraryLoader(Instance& bridge);
    Result LoadGuestLibrary(Instance& bridge, const std::string& soname, uint32_t& moduleId);
    Result RegisterLoaderFunctions(Instance& bridge);
    Result InstallRuntimeLoadHook(JNIEnv* env);

    Result GuestJavaVm(const Instance& bridge, GuestState::GuestAddress& address);
    Result GuestJniEnv(const Instance& bridge, GuestState::GuestAddress& address);

    const std::vector<RegisteredNative>& RegisteredNatives(const Instance& bridge);
    Statistics GetStatistics(const Instance& bridge);

    const char* SlotName(int slot);
    const char* ResultName(Result result);
}

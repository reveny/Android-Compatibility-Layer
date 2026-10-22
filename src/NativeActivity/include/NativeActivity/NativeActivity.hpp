#pragma once

#include <cstdint>
#include <string>

#include "Apk/Apk.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "JniBridge/JniBridge.hpp"

namespace NativeActivity
{
    inline constexpr const char* ENTRY_POINT_SYMBOL = "ANativeActivity_onCreate";

    enum class Result : uint8_t
    {
        Ok,
        NotInitialized,
        NoEntryPoint,
        OutOfGuestMemory,
        GuestFaulted,
        NoCallback,
        LibraryMissing,
        ClassMissing,
        JniRegistrationFailed
    };

    enum class Callback : uint8_t
    {
        Start,
        Resume,
        SaveInstanceState,
        Pause,
        Stop,
        Destroy,
        WindowFocusChanged,
        NativeWindowCreated,
        NativeWindowResized,
        NativeWindowRedrawNeeded,
        NativeWindowDestroyed,
        InputQueueCreated,
        InputQueueDestroyed,
        ContentRectChanged,
        ConfigurationChanged,
        LowMemory,
        Count
    };

    struct Statistics
    {
        uint64_t callbacksRegistered = 0;
        uint64_t callbacksInvoked = 0;
        uint64_t callbacksAbsent = 0;
        bool entryPointRan = false;
    };

    struct Instance
    {
        void* implementation = nullptr;

        Instance() = default;
        Instance(const Instance&) = delete;
        Instance& operator=(const Instance&) = delete;
    };

    Result Create(GuestRuntime::Instance& runtime, JniBridge::Instance& bridge, uint32_t moduleId, const std::string& internalDataPath,
                  void* activityObject, Instance& activity);
    void Destroy(Instance& activity);

    Result Invoke(Instance& activity, Callback callback, uint64_t argument);
    bool HasCallback(const Instance& activity, Callback callback);

    Statistics GetStatistics(const Instance& activity);

    inline constexpr const char* ACTIVITY_CLASS = "android/app/NativeActivity";
    inline constexpr const char* SURFACE_BRIDGE_CLASS = "runtime/SurfaceBridge";

    Result InstallJni(JNIEnv* env, GuestRuntime::Instance& runtime, JniBridge::Instance& bridge, bool guestBridgeLive);
    void RemoveJni();

    Result RegisterWindowFunctions(GuestRuntime::Instance& runtime);

    Result RegisterAudioFunctions(GuestRuntime::Instance& runtime);
    Result RegisterAssetFunctions(GuestRuntime::Instance& runtime, const Apk::Package& package);
    void PrepareLooper();
    bool ServiceLooper();
    Result CreateWindow(GuestRuntime::Instance& runtime, int32_t width, int32_t height, GuestState::GuestAddress& window);
    bool FinishRequested();

    const char* CallbackName(Callback callback);
    const char* ResultName(Result result);
}

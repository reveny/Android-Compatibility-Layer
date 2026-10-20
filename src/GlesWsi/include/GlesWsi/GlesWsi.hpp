#pragma once

#include <cstdint>

#include "GuestRuntime/GuestRuntime.hpp"

struct wl_display;
struct wl_surface;

namespace GlesWsi
{
    inline constexpr const char* EGL_SONAME = "libEGL.so.1";
    inline constexpr const char* GLES_SONAME = "libGLESv2.so.2";
    inline constexpr const char* WAYLAND_EGL_SONAME = "libwayland-egl.so.1";

    inline constexpr const char* FRAME_TRACE_SETTING = "trace-frames";

    inline constexpr uint64_t MAX_ENTRY_POINT_NAME = 128;
    inline constexpr uint64_t STRING_ARENA_BYTES = 65536;

    enum class Result : uint8_t
    {
        Ok,
        NoDriver,
        MissingEntryPoint,
        RegistrationFailed,
        NoSurfaceTarget,
        NoStringArena
    };

    struct Statistics
    {
        uint64_t entryPointsRegistered = 0;
        uint64_t surfacesCreated = 0;
        uint64_t framesPresented = 0;
        uint64_t stringsCopied = 0;
        uint64_t guestCallbacksRefused = 0;
    };

    Result RegisterAll(GuestRuntime::Instance& runtime);
    void Shutdown();

    void AttachSurfaceTarget(wl_display* display, wl_surface* surface, uint32_t width, uint32_t height);
    void ResizeSurfaceTarget(uint32_t width, uint32_t height);
    bool HasSurfaceTarget();

    Statistics GetStatistics();
    const char* ResultName(Result result);
}

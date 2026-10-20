#pragma once

#include <cstdint>
#include <string>

#include "GuestRuntime/GuestRuntime.hpp"

struct wl_display;
struct wl_surface;

namespace VulkanWsi
{
    inline constexpr const char* DRIVER_SONAME = "libvulkan.so.1";
    inline constexpr const char* GUEST_SURFACE_EXTENSION = "VK_KHR_android_surface";
    inline constexpr const char* HOST_SURFACE_EXTENSION = "VK_KHR_wayland_surface";

    inline constexpr uint64_t MAX_PACING_SAMPLES = 4096;
    inline constexpr const char* FRAME_TRACE_SETTING = "trace-frames";
    inline constexpr const char* PRESENT_WITHOUT_WAITING_SETTING = "no-vsync";
    inline constexpr uint64_t FRAME_REPORT_NANOS = 1000000000ull;

    enum class Result : uint8_t
    {
        Ok,
        NoDriver,
        MissingEntryPoint,
        RegistrationFailed,
        NoSurfaceTarget,
        GuestAllocatorRefused,
        ReportUnwritable
    };

    struct Statistics
    {
        uint64_t callsForwarded = 0;
        uint64_t entryPointsRegistered = 0;
        uint64_t surfaceExtensionsRewritten = 0;
        uint64_t surfacesCreated = 0;
        uint64_t guestAllocatorsRefused = 0;
        uint64_t framesPresented = 0;
    };

    Result RegisterAll(GuestRuntime::Instance& runtime);
    void Shutdown();

    void AttachSurfaceTarget(wl_display* display, wl_surface* surface);
    bool HasSurfaceTarget();

    Statistics GetStatistics();
    Result WritePacingReport(const std::string& path);

    const char* ResultName(Result result);
}

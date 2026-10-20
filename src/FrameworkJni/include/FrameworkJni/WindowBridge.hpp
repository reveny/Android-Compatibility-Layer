#pragma once

#include <cstdint>

#include "FrameworkJni/HostedWindow.hpp"
#include "Rasterizer/Rasterizer.hpp"
#include "WaylandClient/WaylandClient.hpp"

namespace WindowBridge
{
    inline constexpr const char* FRAME_DUMP_SETTING = "frame-dump";

    inline constexpr int POLL_EVENT_CAPACITY = 8;
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;

    enum class Result : uint8_t
    {
        Ok,
        AlreadyOpen,
        NotOpen,
        NoCompositor,
        SurfaceFailed,
        NoFreeBuffer,
        PollFailed
    };

    struct Session
    {
        WaylandClient::Connection connection;
        WaylandClient::Surface surface;
        WaylandClient::Buffer* current = nullptr;
        HostedWindow::Session hosted;
        HostedWindow::Buffer* hostedCurrent = nullptr;
        Rasterizer::Surface target;
        Rasterizer::Canvas canvas;
        int epollFileDescriptor = INVALID_FILE_DESCRIPTOR;
        int wakeFileDescriptor = INVALID_FILE_DESCRIPTOR;
        uint64_t lastFrameTimeNanos = 0;
        uint32_t resizedWidth = 0;
        uint32_t resizedHeight = 0;
        bool resizePending = false;
        bool framePending = false;
        bool frameArrived = false;
        bool isHosted = false;
        bool open = false;
        bool transparent = false;
    };

    Result Open(uint32_t width, uint32_t height, const char* title, Session& session);
    void Close(Session& session);

    Result BeginFrame(Session& session);
    Result EndFrame(Session& session);
    Result RequestFrame(Session& session);

    Result Poll(Session& session, int timeoutMillis);
    void Wake(Session& session);

    bool ConsumeFrame(Session& session);
    bool ConsumeResize(Session& session, uint32_t& width, uint32_t& height);
    Result SetFullscreen(Session& session, bool fullscreen);
    bool NextInputEvent(Session& session, WaylandClient::InputEvent& event);
    bool IsCloseRequested(const Session& session);

    const char* ResultName(Result result);
}

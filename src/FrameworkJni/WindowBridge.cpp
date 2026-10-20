#include "FrameworkJni/WindowBridge.hpp"

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <unistd.h>

#include "Overlay/FrameRate.hpp"
#include "Overlay/Overlay.hpp"

#if defined(RUNTIME_HAVE_VULKAN)
#include "Settings/Settings.hpp"
#include "VulkanWsi/VulkanWsi.hpp"
#endif

#if defined(RUNTIME_HAVE_GLES)
#include "GlesWsi/GlesWsi.hpp"
#endif

static void HandleFrame(void* context, uint64_t frameTimeNanos)
{
    WindowBridge::Session* session = static_cast<WindowBridge::Session*>(context);
    session->frameArrived = true;
    session->framePending = false;
    session->lastFrameTimeNanos = frameTimeNanos;
}

static int WatchedDescriptor(const WindowBridge::Session& session)
{
    if (session.isHosted) return session.hosted.channel.fileDescriptor;

    return session.connection.fileDescriptor;
}

WindowBridge::Result WindowBridge::Open(uint32_t width, uint32_t height, const char* title, Session& session)
{
    if (session.open) return Result::AlreadyOpen;

    session.isHosted = HostedWindow::IsRequested();

    if (session.isHosted)
    {
        if (HostedWindow::Open(width, height, title, session.hosted) != HostedWindow::Result::Ok)
        {
            session.isHosted = false;
            return Result::SurfaceFailed;
        }
    }
    else
    {
        if (WaylandClient::Connect(session.connection) != WaylandClient::Result::Ok) return Result::NoCompositor;

        if (WaylandClient::CreateSurface(session.connection, width, height, title, session.surface) != WaylandClient::Result::Ok)
        {
            WaylandClient::Disconnect(session.connection);
            return Result::SurfaceFailed;
        }
    }

    session.wakeFileDescriptor = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
    session.epollFileDescriptor = epoll_create1(EPOLL_CLOEXEC);

    if (session.wakeFileDescriptor < 0 || session.epollFileDescriptor < 0)
    {
        Close(session);
        return Result::PollFailed;
    }

    epoll_event displayEvent{};
    displayEvent.events = EPOLLIN;
    displayEvent.data.fd = WatchedDescriptor(session);
    epoll_ctl(session.epollFileDescriptor, EPOLL_CTL_ADD, displayEvent.data.fd, &displayEvent);

    epoll_event wakeEvent{};
    wakeEvent.events = EPOLLIN;
    wakeEvent.data.fd = session.wakeFileDescriptor;
    epoll_ctl(session.epollFileDescriptor, EPOLL_CTL_ADD, session.wakeFileDescriptor, &wakeEvent);

    session.open = true;

#if defined(RUNTIME_HAVE_VULKAN)
    if (!session.isHosted) VulkanWsi::AttachSurfaceTarget(session.connection.display, session.surface.surface);
#endif

#if defined(RUNTIME_HAVE_GLES)
    if (!session.isHosted) GlesWsi::AttachSurfaceTarget(session.connection.display, session.surface.surface, width, height);
#endif

    return Result::Ok;
}

void WindowBridge::Close(Session& session)
{
    if (session.epollFileDescriptor >= 0) close(session.epollFileDescriptor);
    if (session.wakeFileDescriptor >= 0) close(session.wakeFileDescriptor);

    session.epollFileDescriptor = INVALID_FILE_DESCRIPTOR;
    session.wakeFileDescriptor = INVALID_FILE_DESCRIPTOR;

    if (session.isHosted)
    {
        HostedWindow::SendExit(session.hosted, 0);
        HostedWindow::Close(session.hosted);
        session.hostedCurrent = nullptr;
        session.open = false;
        return;
    }

    WaylandClient::DestroySurface(session.surface);
    WaylandClient::Disconnect(session.connection);

    session.current = nullptr;
    session.open = false;
}

static void ClearToTransparent(Rasterizer::Surface& surface)
{
    if (!surface.pixels) return;

    for (uint32_t row = 0; row < surface.height; row++)
    {
        std::memset(surface.pixels + static_cast<size_t>(row) * surface.stride, 0, static_cast<size_t>(surface.width) * 4);
    }
}

WindowBridge::Result WindowBridge::BeginFrame(Session& session)
{
    if (!session.open) return Result::NotOpen;

    if (session.isHosted)
    {
        HostedWindow::Buffer* hostedBuffer = nullptr;
        if (HostedWindow::AcquireBuffer(session.hosted, hostedBuffer) != HostedWindow::Result::Ok) return Result::NoFreeBuffer;

        session.hostedCurrent = hostedBuffer;
        Rasterizer::WrapSurface(hostedBuffer->pixels, session.hosted.width, session.hosted.height, session.hosted.stride, session.target);
        Rasterizer::Begin(session.target, session.canvas);

        if (session.transparent) ClearToTransparent(session.target);

        return Result::Ok;
    }

    WaylandClient::Buffer* buffer = nullptr;
    if (WaylandClient::AcquireBuffer(session.surface, buffer) != WaylandClient::Result::Ok)
    {
        WaylandClient::Roundtrip(session.connection);
        if (WaylandClient::AcquireBuffer(session.surface, buffer) != WaylandClient::Result::Ok) return Result::NoFreeBuffer;
    }

    session.current = buffer;
    Rasterizer::WrapSurface(buffer->pixels, buffer->width, buffer->height, buffer->stride, session.target);
    Rasterizer::Begin(session.target, session.canvas);

    if (session.transparent) ClearToTransparent(session.target);

    return Result::Ok;
}

static void DumpFrame(const uint8_t* pixels, size_t sizeBytes)
{
    const char* path = Settings::Lookup(WindowBridge::FRAME_DUMP_SETTING);
    if (!path || !*path) return;

    FILE* out = fopen(path, "wb");
    if (!out) return;

    fwrite(pixels, 1, sizeBytes, out);
    fclose(out);
}

WindowBridge::Result WindowBridge::EndFrame(Session& session)
{
    if (!session.open) return Result::NotOpen;

    FrameRate::Tick();

    if (session.isHosted)
    {
        if (!session.hostedCurrent) return Result::NoFreeBuffer;

        Overlay::DrawInto(session.hostedCurrent->pixels, session.hosted.width, session.hosted.height, session.hosted.stride);
        DumpFrame(session.hostedCurrent->pixels, session.hostedCurrent->sizeBytes);
        HostedWindow::Present(session.hosted, session.hosted.lastFrameTimeNanos);
        session.hostedCurrent = nullptr;
        return Result::Ok;
    }

    if (!session.current) return Result::NoFreeBuffer;

    Overlay::DrawInto(session.current->pixels, session.current->width, session.current->height, session.current->stride);
    DumpFrame(session.current->pixels, session.current->sizeBytes);
    WaylandClient::Present(session.connection, session.surface, *session.current);
    session.current = nullptr;
    return Result::Ok;
}

WindowBridge::Result WindowBridge::RequestFrame(Session& session)
{
    if (!session.open) return Result::NotOpen;

    if (session.isHosted)
    {
        HostedWindow::RequestFrame(session.hosted);
        Wake(session);
        return Result::Ok;
    }

    if (session.framePending) return Result::Ok;

    if (WaylandClient::RequestFrame(session.connection, session.surface, &HandleFrame, &session) != WaylandClient::Result::Ok)
    {
        return Result::PollFailed;
    }

    session.framePending = true;
    Wake(session);
    return Result::Ok;
}

static void ApplyPendingResize(WindowBridge::Session& session)
{
    if (session.isHosted) return;

    WaylandClient::Surface& surface = session.surface;

    if (surface.pendingWidth == 0 || surface.pendingHeight == 0) return;
    if (surface.pendingWidth == surface.width && surface.pendingHeight == surface.height) return;

    uint32_t width = surface.pendingWidth;
    uint32_t height = surface.pendingHeight;

    if (session.current != nullptr) return;

    WaylandClient::Result resized = WaylandClient::Resize(session.connection, surface, width, height);

    if (resized != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "[windowbridge] the compositor asked for %ux%u and the buffers could not follow: %s\n", width, height,
                WaylandClient::ResultName(resized));
        surface.pendingWidth = surface.width;
        surface.pendingHeight = surface.height;
        return;
    }

    fprintf(stderr, "[windowbridge] the window is now %ux%u\n", width, height);

    session.resizedWidth = width;
    session.resizedHeight = height;
    session.resizePending = true;

#if defined(RUNTIME_HAVE_GLES)
    GlesWsi::ResizeSurfaceTarget(width, height);
#endif
}

WindowBridge::Result WindowBridge::Poll(Session& session, int timeoutMillis)
{
    if (!session.open) return Result::NotOpen;

    if (!session.isHosted) WaylandClient::Flush(session.connection);

    epoll_event events[POLL_EVENT_CAPACITY];
    int ready = epoll_wait(session.epollFileDescriptor, events, POLL_EVENT_CAPACITY, timeoutMillis);
    if (ready < 0) return errno == EINTR ? Result::Ok : Result::PollFailed;

    for (int index = 0; index < ready; ++index)
    {
        if (events[index].data.fd == session.wakeFileDescriptor)
        {
            uint64_t drained = 0;
            ssize_t got = read(session.wakeFileDescriptor, &drained, sizeof(drained));
            (void)got;
            continue;
        }

        if (events[index].data.fd != WatchedDescriptor(session)) continue;

        if (session.isHosted)
        {
            if (HostedWindow::Pump(session.hosted, 0) == HostedWindow::Result::PeerClosed)
            {
                fprintf(stderr, "[windowbridge] the shell closed the control channel\n");
                return Result::NoCompositor;
            }

            continue;
        }

        if ((events[index].events & (EPOLLHUP | EPOLLERR)) != 0)
        {
            fprintf(stderr, "[windowbridge] the compositor hung up on the connection\n");
            session.surface.closeRequested = true;
            return Result::NoCompositor;
        }

        if (WaylandClient::DispatchPending(session.connection) != WaylandClient::Result::Ok)
        {
            WaylandClient::ReportConnectionError(session.connection);
            session.surface.closeRequested = true;
            return Result::NoCompositor;
        }
    }

    ApplyPendingResize(session);

    return Result::Ok;
}

void WindowBridge::Wake(Session& session)
{
    if (session.wakeFileDescriptor < 0) return;

    uint64_t one = 1;
    ssize_t written = write(session.wakeFileDescriptor, &one, sizeof(one));
    (void)written;
}

bool WindowBridge::NextInputEvent(Session& session, WaylandClient::InputEvent& event)
{
    if (session.isHosted) return HostedWindow::NextInputEvent(session.hosted, event);

    return WaylandClient::NextInputEvent(session.connection, event);
}

bool WindowBridge::ConsumeResize(Session& session, uint32_t& width, uint32_t& height)
{
    if (!session.resizePending) return false;

    session.resizePending = false;
    width = session.resizedWidth;
    height = session.resizedHeight;
    return true;
}

WindowBridge::Result WindowBridge::SetFullscreen(Session& session, bool fullscreen)
{
    if (!session.open) return Result::NotOpen;
    if (session.isHosted) return Result::Ok;

    return WaylandClient::SetFullscreen(session.surface, fullscreen) == WaylandClient::Result::Ok ? Result::Ok : Result::NoCompositor;
}

bool WindowBridge::ConsumeFrame(Session& session)
{
    if (session.isHosted)
    {
        if (!session.hosted.frameArrived) return false;

        session.hosted.frameArrived = false;
        return true;
    }

    if (!session.frameArrived) return false;

    session.frameArrived = false;
    return true;
}

bool WindowBridge::IsCloseRequested(const Session& session)
{
    if (session.isHosted) return session.hosted.stopRequested;

    return session.surface.closeRequested;
}

const char* WindowBridge::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::AlreadyOpen: return "AlreadyOpen";
        case Result::NotOpen: return "NotOpen";
        case Result::NoCompositor: return "NoCompositor";
        case Result::SurfaceFailed: return "SurfaceFailed";
        case Result::NoFreeBuffer: return "NoFreeBuffer";
        case Result::PollFailed: return "PollFailed";
    }
    return "Unknown";
}

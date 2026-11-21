#include "WindowHost/WindowHost.hpp"

#include <cerrno>
#include <chrono>
#include <csignal>
#include <cstdio>
#include <cstring>
#include <memory>
#include <string>
#include <vector>

#include <dirent.h>
#include <fcntl.h>
#include <poll.h>
#include <sys/epoll.h>
#include <sys/mman.h>
#include <sys/signalfd.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <sys/wait.h>
#include <unistd.h>

#include "Settings/Settings.hpp"
#include "WaylandClient/WaylandClient.hpp"

struct HostState;

struct HostedBuffer
{
    HostState* owner = nullptr;
    wl_buffer* buffer = nullptr;
    uint32_t generation = 0;
    uint32_t index = 0;
    uint32_t width = 0;
    uint32_t height = 0;
    bool attached = false;
    bool forgotten = false;
};

struct HostState
{
    ControlChannel::Endpoint* channel = nullptr;
    pid_t app = -1;
    int epollFileDescriptor = -1;
    int signalFileDescriptor = -1;
    WaylandClient::Connection connection;
    WaylandClient::Surface surface;
    std::vector<std::unique_ptr<HostedBuffer>> buffers;
    HostedBuffer* pendingBuffer = nullptr;
    int pendingFence = -1;
    uint32_t sentWidth = 0;
    uint32_t sentHeight = 0;
    bool sentFullscreen = false;
    bool sentMaximized = false;
    bool windowOpen = false;
    bool helloSeen = false;
    bool channelOpen = true;
    bool stopSent = false;
    bool attachedWarned = false;
    std::chrono::steady_clock::time_point stopDeadline{};
    int exitStatus = 1;
    bool appExited = false;
};

static uint64_t MonotonicNanos()
{
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static void SendToApp(HostState& state, const ControlChannel::Message& message)
{
    if (!state.channelOpen) return;

    ControlChannel::Send(*state.channel, message);
}

static void SendReleased(HostState& state, const HostedBuffer& buffer)
{
    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::BufferReleased);
    message.payload.bufferReleased.generation = buffer.generation;
    message.payload.bufferReleased.index = buffer.index;
    SendToApp(state, message);
}

static void DropBuffer(HostState& state, HostedBuffer* buffer)
{
    if (state.pendingBuffer == buffer)
    {
        if (state.pendingFence >= 0) close(state.pendingFence);
        state.pendingBuffer = nullptr;
        state.pendingFence = -1;
    }

    WaylandClient::DestroyExternalBuffer(buffer->buffer);

    for (size_t position = 0; position < state.buffers.size(); ++position)
    {
        if (state.buffers[position].get() != buffer) continue;

        state.buffers.erase(state.buffers.begin() + (long)position);
        return;
    }
}

static HostedBuffer* FindBuffer(HostState& state, uint32_t generation, uint32_t index)
{
    for (const std::unique_ptr<HostedBuffer>& buffer : state.buffers)
    {
        if (buffer->generation == generation && buffer->index == index && !buffer->forgotten) return buffer.get();
    }

    return nullptr;
}

static void HandleBufferRelease(void* context)
{
    HostedBuffer* buffer = static_cast<HostedBuffer*>(context);
    HostState& state = *buffer->owner;

    buffer->attached = false;

    if (buffer->forgotten)
    {
        DropBuffer(state, buffer);
        return;
    }

    SendReleased(state, *buffer);
}

static void CommitFrame(HostState& state, HostedBuffer* buffer, int fence);

static void HandleFrameDone(void* context, uint64_t frameTimeNanos)
{
    HostState& state = *static_cast<HostState*>(context);

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::PresentDone);
    message.payload.presentDone.frameTimeNanos = frameTimeNanos != 0 ? frameTimeNanos : MonotonicNanos();
    SendToApp(state, message);

    if (!state.pendingBuffer) return;

    HostedBuffer* buffer = state.pendingBuffer;
    int fence = state.pendingFence;
    state.pendingBuffer = nullptr;
    state.pendingFence = -1;

    CommitFrame(state, buffer, fence);
}

static void WaitForFence(int fence)
{
    if (fence < 0) return;

    pollfd watched{};
    watched.fd = fence;
    watched.events = POLLIN;

    while (poll(&watched, 1, WindowHost::FENCE_TIMEOUT_MILLIS) < 0 && errno == EINTR)
    {
    }

    close(fence);
}

static void CommitFrame(HostState& state, HostedBuffer* buffer, int fence)
{
    WaitForFence(fence);

    if (!state.windowOpen) return;

    WaylandClient::PresentExternal(state.connection, state.surface, buffer->buffer, buffer->width, buffer->height, state.sentWidth,
                                   state.sentHeight);
    buffer->attached = true;

    WaylandClient::RequestFrame(state.connection, state.surface, &HandleFrameDone, &state);
}

static uint64_t RenderNodeFor(uint64_t device)
{
    if (device == 0) return 0;

    char path[128];
    snprintf(path, sizeof(path), "/sys/dev/char/%u:%u/device/drm", major((dev_t)device), minor((dev_t)device));

    DIR* directory = opendir(path);
    if (!directory) return device;

    uint64_t found = device;

    while (dirent* entry = readdir(directory))
    {
        if (strncmp(entry->d_name, "renderD", 7) != 0) continue;

        std::string node = std::string("/dev/dri/") + entry->d_name;
        struct stat described{};

        if (stat(node.c_str(), &described) == 0 && S_ISCHR(described.st_mode)) found = (uint64_t)described.st_rdev;
        break;
    }

    closedir(directory);
    return found;
}

static int CreateFormatTable(const std::vector<WaylandClient::DmabufFormat>& formats, uint32_t& count)
{
    count = (uint32_t)(formats.size() < ControlChannel::MAX_FORMAT_ENTRIES ? formats.size() : ControlChannel::MAX_FORMAT_ENTRIES);
    if (count == 0) return -1;

    size_t sizeBytes = (size_t)count * sizeof(ControlChannel::FormatEntry);

    int descriptor = memfd_create("acl-formats", MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (descriptor < 0) return -1;

    std::vector<ControlChannel::FormatEntry> entries(count);
    for (uint32_t position = 0; position < count; ++position)
    {
        entries[position].format = formats[position].format;
        entries[position].padding = 0;
        entries[position].modifier = formats[position].modifier;
    }

    if (write(descriptor, entries.data(), sizeBytes) != (ssize_t)sizeBytes)
    {
        close(descriptor);
        return -1;
    }

    fcntl(descriptor, F_ADD_SEALS, F_SEAL_SHRINK | F_SEAL_GROW | F_SEAL_WRITE | F_SEAL_SEAL);
    return descriptor;
}

static void DescribeDisplay(HostState& state)
{
    WaylandClient::DisplayMode mode = WaylandClient::CurrentDisplayMode(state.connection);

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::Display);

    ControlChannel::DisplayPayload& payload = message.payload.display;
    payload.width = mode.width;
    payload.height = mode.height;
    payload.refreshMilliHertz = mode.refreshMilliHertz;
    payload.scale = mode.scale;
    payload.physicalWidthMm = mode.physicalWidthMm;
    payload.physicalHeightMm = mode.physicalHeightMm;
    payload.renderDevice = RenderNodeFor(state.connection.dmabufMainDevice);

    uint32_t count = 0;
    int table = state.connection.dmabuf ? CreateFormatTable(state.connection.dmabufFormats, count) : -1;

    payload.formatCount = table >= 0 ? count : 0;
    payload.dmabufAvailable = table >= 0;

    if (table >= 0)
    {
        if (state.channelOpen) ControlChannel::SendWithDescriptors(*state.channel, message, &table, 1);
        close(table);
        return;
    }

    SendToApp(state, message);
}

static void SendConfigureIfChanged(HostState& state)
{
    if (!state.windowOpen) return;

    uint32_t width = state.surface.pendingWidth > 0 ? state.surface.pendingWidth : state.surface.width;
    uint32_t height = state.surface.pendingHeight > 0 ? state.surface.pendingHeight : state.surface.height;

    if (width == 0 || height == 0) return;
    if (width == state.sentWidth && height == state.sentHeight && state.surface.fullscreen == state.sentFullscreen &&
        state.surface.maximized == state.sentMaximized)
    {
        return;
    }

    state.sentWidth = width;
    state.sentHeight = height;
    state.sentFullscreen = state.surface.fullscreen;
    state.sentMaximized = state.surface.maximized;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::Configure);
    message.payload.configure.width = width;
    message.payload.configure.height = height;
    message.payload.configure.fullscreen = state.surface.fullscreen;
    message.payload.configure.maximized = state.surface.maximized;
    SendToApp(state, message);
}

static void ForwardInput(HostState& state)
{
    WaylandClient::InputEvent event{};

    while (WaylandClient::NextInputEvent(state.connection, event))
    {
        ControlChannel::Message message{};
        ControlChannel::Prepare(message, ControlChannel::MessageKind::Input);

        ControlChannel::InputPayload& payload = message.payload.input;
        payload.kind = event.kind == WaylandClient::InputKind::Key ? ControlChannel::InputKind::Key : ControlChannel::InputKind::Touch;
        payload.action = event.action;
        payload.pointerId = event.pointerId;
        payload.keyCode = event.keyCode;
        payload.unicode = event.unicode;
        payload.metaState = event.metaState;
        payload.x = event.x;
        payload.y = event.y;
        payload.timeNanos = event.timeNanos;
        SendToApp(state, message);
    }
}

static void CloseWindow(HostState& state)
{
    if (!state.windowOpen) return;

    if (state.pendingFence >= 0) close(state.pendingFence);
    state.pendingBuffer = nullptr;
    state.pendingFence = -1;

    for (const std::unique_ptr<HostedBuffer>& buffer : state.buffers) WaylandClient::DestroyExternalBuffer(buffer->buffer);
    state.buffers.clear();

    epoll_ctl(state.epollFileDescriptor, EPOLL_CTL_DEL, state.connection.fileDescriptor, nullptr);

    WaylandClient::DestroySurface(state.surface);
    WaylandClient::Disconnect(state.connection);
    state.windowOpen = false;
}

static void OpenWindow(HostState& state, const ControlChannel::HelloPayload& hello)
{
    if (state.helloSeen) return;
    state.helloSeen = true;

    if (hello.version != ControlChannel::PROTOCOL_VERSION)
    {
        fprintf(stderr, "[windowhost] the app speaks window protocol %u and this host speaks %u, so no window opens\n", hello.version,
                ControlChannel::PROTOCOL_VERSION);
        return;
    }

    if (WaylandClient::Connect(state.connection) != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "[windowhost] cannot reach the compositor, so no window opens\n");
        return;
    }

    const char* title = hello.title[0] != '\0' ? hello.title : hello.packageName;

    WaylandClient::Result created = WaylandClient::CreateHostedSurface(state.connection, hello.width, hello.height, title, state.surface);

    if (created != WaylandClient::Result::Ok)
    {
        fprintf(stderr, "[windowhost] the compositor did not give the app a window: %s\n", WaylandClient::ResultName(created));
        WaylandClient::Disconnect(state.connection);
        return;
    }

    state.windowOpen = true;
    state.sentWidth = hello.width;
    state.sentHeight = hello.height;

    epoll_event event{};
    event.events = EPOLLIN;
    event.data.fd = state.connection.fileDescriptor;
    epoll_ctl(state.epollFileDescriptor, EPOLL_CTL_ADD, state.connection.fileDescriptor, &event);

    DescribeDisplay(state);
    SendConfigureIfChanged(state);
    WaylandClient::Flush(state.connection);
}

static bool PlausibleBuffer(const ControlChannel::RegisterBufferPayload& payload, uint32_t descriptorCount)
{
    if (payload.generation == 0 || payload.index >= ControlChannel::MAX_FRAME_BUFFERS) return false;
    if (payload.width == 0 || payload.height == 0) return false;
    if (payload.width > WaylandClient::MAX_DIMENSION || payload.height > WaylandClient::MAX_DIMENSION) return false;
    if (payload.planeCount == 0 || payload.planeCount > ControlChannel::MAX_PLANES || payload.planeCount != descriptorCount) return false;
    if (payload.kind == ControlChannel::BufferKind::Shared && payload.planeCount != 1) return false;

    return payload.kind == ControlChannel::BufferKind::Shared || payload.kind == ControlChannel::BufferKind::Dmabuf;
}

static void RegisterBuffer(HostState& state, const ControlChannel::RegisterBufferPayload& payload, const int* descriptors,
                           uint32_t descriptorCount)
{
    if (!state.windowOpen) return;

    bool acceptable = PlausibleBuffer(payload, descriptorCount) && state.buffers.size() < WindowHost::MAX_HOSTED_BUFFERS &&
                      !FindBuffer(state, payload.generation, payload.index);

    std::unique_ptr<HostedBuffer> hosted = std::make_unique<HostedBuffer>();
    hosted->owner = &state;
    hosted->generation = payload.generation;
    hosted->index = payload.index;
    hosted->width = payload.width;
    hosted->height = payload.height;

    if (!acceptable)
    {
        hosted->buffer = nullptr;
    }
    else if (payload.kind == ControlChannel::BufferKind::Shared)
    {
        hosted->buffer = WaylandClient::ImportSharedBuffer(state.connection, descriptors[0], payload.width, payload.height, payload.strides[0],
                                                           payload.format, &HandleBufferRelease, hosted.get());
    }
    else
    {
        hosted->buffer = WaylandClient::ImportDmabuf(state.connection, descriptors, payload.offsets, payload.strides, payload.planeCount,
                                                     payload.width, payload.height, payload.format, payload.modifier, &HandleBufferRelease,
                                                     hosted.get());
    }

    if (!hosted->buffer)
    {
        ControlChannel::Message refusal{};
        ControlChannel::Prepare(refusal, ControlChannel::MessageKind::ForgetBuffers);
        refusal.payload.forgetBuffers.generation = payload.generation;
        SendToApp(state, refusal);
        return;
    }

    state.buffers.push_back(std::move(hosted));
}

static void ForgetBuffers(HostState& state, uint32_t generation)
{
    std::vector<HostedBuffer*> retired;

    for (const std::unique_ptr<HostedBuffer>& buffer : state.buffers)
    {
        if (buffer->generation == generation) retired.push_back(buffer.get());
    }

    for (HostedBuffer* buffer : retired)
    {
        if (buffer->attached)
        {
            buffer->forgotten = true;
            if (state.pendingBuffer == buffer) DropBuffer(state, buffer);
            continue;
        }

        DropBuffer(state, buffer);
    }
}

static void PresentFrame(HostState& state, const ControlChannel::FrameReadyPayload& payload, int fence)
{
    HostedBuffer* buffer = state.windowOpen ? FindBuffer(state, payload.generation, payload.index) : nullptr;

    if (!buffer || buffer->attached)
    {
        if (fence >= 0) close(fence);
        if (buffer && !state.attachedWarned)
        {
            fprintf(stderr, "[windowhost] the app presented a buffer the compositor still holds, so that frame is skipped\n");
            state.attachedWarned = true;
        }
        return;
    }

    if (state.surface.frameRequested)
    {
        if (state.pendingBuffer && state.pendingBuffer != buffer)
        {
            SendReleased(state, *state.pendingBuffer);
            if (state.pendingFence >= 0) close(state.pendingFence);
        }

        state.pendingBuffer = buffer;
        state.pendingFence = fence;
        return;
    }

    CommitFrame(state, buffer, fence);
}

static void HandleMessage(HostState& state, const ControlChannel::Message& message, int* descriptors, uint32_t& descriptorCount)
{
    switch (message.kind)
    {
        case ControlChannel::MessageKind::Hello:
            OpenWindow(state, message.payload.hello);
            break;

        case ControlChannel::MessageKind::RegisterBuffer:
            RegisterBuffer(state, message.payload.registerBuffer, descriptors, descriptorCount);
            break;

        case ControlChannel::MessageKind::ForgetBuffers:
            ForgetBuffers(state, message.payload.forgetBuffers.generation);
            break;

        case ControlChannel::MessageKind::FrameReady:
        {
            int fence = -1;

            if (message.payload.frameReady.hasFence && descriptorCount > 0)
            {
                fence = descriptors[0];
                descriptors[0] = -1;
            }

            PresentFrame(state, message.payload.frameReady, fence);
            break;
        }

        case ControlChannel::MessageKind::RequestFrame:
            if (state.windowOpen) WaylandClient::RequestFrame(state.connection, state.surface, &HandleFrameDone, &state);
            break;

        case ControlChannel::MessageKind::SetTitle:
            if (state.windowOpen) WaylandClient::SetTitle(state.surface, message.payload.title.title);
            break;

        case ControlChannel::MessageKind::SetFullscreen:
            if (state.windowOpen) WaylandClient::SetFullscreen(state.surface, message.payload.fullscreen.fullscreen);
            break;

        default:
            break;
    }
}

static void DrainChannel(HostState& state)
{
    for (;;)
    {
        ControlChannel::Message message{};
        int descriptors[ControlChannel::MAX_DESCRIPTORS];
        uint32_t descriptorCount = ControlChannel::MAX_DESCRIPTORS;

        ControlChannel::Result received = ControlChannel::ReceiveWithDescriptors(*state.channel, message, descriptors, descriptorCount, 0);

        if (received == ControlChannel::Result::Ok) HandleMessage(state, message, descriptors, descriptorCount);

        for (uint32_t index = 0; index < descriptorCount; ++index)
        {
            if (descriptors[index] >= 0) close(descriptors[index]);
        }

        if (received == ControlChannel::Result::Ok || received == ControlChannel::Result::Malformed) continue;
        if (received == ControlChannel::Result::WouldBlock) return;

        state.channelOpen = false;
        epoll_ctl(state.epollFileDescriptor, EPOLL_CTL_DEL, state.channel->fileDescriptor, nullptr);
        CloseWindow(state);
        return;
    }
}

static void ServiceCompositor(HostState& state, uint32_t events)
{
    if ((events & (EPOLLHUP | EPOLLERR)) != 0 || WaylandClient::DispatchPending(state.connection) != WaylandClient::Result::Ok)
    {
        WaylandClient::ReportConnectionError(state.connection);
        fprintf(stderr, "[windowhost] the compositor connection ended, so the app is asked to stop\n");
        CloseWindow(state);
        state.surface.closeRequested = true;
    }

    if (state.windowOpen)
    {
        ForwardInput(state);
        SendConfigureIfChanged(state);
    }
}

static void RequestStopIfClosed(HostState& state)
{
    if (state.stopSent || !state.surface.closeRequested) return;

    state.stopSent = true;
    state.stopDeadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(WindowHost::STOP_GRACE_MILLIS);

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::Stop);
    SendToApp(state, message);
}

static bool ReapApp(HostState& state)
{
    int status = 0;
    pid_t reaped = waitpid(state.app, &status, WNOHANG);

    if (reaped != state.app) return false;

    state.appExited = true;
    state.exitStatus = WIFEXITED(status) ? WEXITSTATUS(status) : 128 + WTERMSIG(status);
    return true;
}

static int NextTimeout(const HostState& state)
{
    if (!state.stopSent) return -1;

    auto remaining = std::chrono::duration_cast<std::chrono::milliseconds>(state.stopDeadline - std::chrono::steady_clock::now()).count();
    return remaining > 0 ? (int)remaining : 0;
}

bool WindowHost::IsWanted()
{
    const char* value = Settings::Lookup(WINDOW_HOST_SETTING);
    if (value && strcmp(value, "0") == 0) return false;

    return WaylandClient::IsAvailable();
}

int WindowHost::Run(ControlChannel::Endpoint& channel, pid_t app)
{
    HostState state;
    state.channel = &channel;
    state.app = app;

    sigset_t watched;
    sigemptyset(&watched);
    sigaddset(&watched, SIGCHLD);
    sigprocmask(SIG_BLOCK, &watched, nullptr);

    state.signalFileDescriptor = signalfd(-1, &watched, SFD_CLOEXEC | SFD_NONBLOCK);
    state.epollFileDescriptor = epoll_create1(EPOLL_CLOEXEC);

    epoll_event channelEvent{};
    channelEvent.events = EPOLLIN;
    channelEvent.data.fd = channel.fileDescriptor;
    epoll_ctl(state.epollFileDescriptor, EPOLL_CTL_ADD, channel.fileDescriptor, &channelEvent);

    epoll_event signalEvent{};
    signalEvent.events = EPOLLIN;
    signalEvent.data.fd = state.signalFileDescriptor;
    epoll_ctl(state.epollFileDescriptor, EPOLL_CTL_ADD, state.signalFileDescriptor, &signalEvent);

    while (!ReapApp(state))
    {
        if (state.windowOpen) WaylandClient::Flush(state.connection);

        epoll_event events[POLL_EVENT_CAPACITY];
        int ready = epoll_wait(state.epollFileDescriptor, events, POLL_EVENT_CAPACITY, NextTimeout(state));

        if (ready < 0 && errno != EINTR) break;

        for (int index = 0; index < ready; ++index)
        {
            int descriptor = events[index].data.fd;

            if (descriptor == state.signalFileDescriptor)
            {
                signalfd_siginfo information{};
                while (read(state.signalFileDescriptor, &information, sizeof(information)) == (ssize_t)sizeof(information))
                {
                }
                continue;
            }

            if (state.channelOpen && descriptor == channel.fileDescriptor)
            {
                DrainChannel(state);
                continue;
            }

            if (state.windowOpen && descriptor == state.connection.fileDescriptor) ServiceCompositor(state, events[index].events);
        }

        RequestStopIfClosed(state);

        if (state.stopSent && std::chrono::steady_clock::now() >= state.stopDeadline && !state.appExited)
        {
            kill(state.app, SIGKILL);
            state.stopDeadline = std::chrono::steady_clock::now() + std::chrono::hours(1);
        }
    }

    if (!state.appExited)
    {
        kill(state.app, SIGKILL);
        int status = 0;
        waitpid(state.app, &status, 0);
        state.exitStatus = WIFEXITED(status) ? WEXITSTATUS(status) : 128 + WTERMSIG(status);
    }

    CloseWindow(state);
    ControlChannel::Close(channel);

    if (state.signalFileDescriptor >= 0) close(state.signalFileDescriptor);
    if (state.epollFileDescriptor >= 0) close(state.epollFileDescriptor);

    return state.exitStatus;
}

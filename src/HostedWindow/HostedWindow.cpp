#include "HostedWindow/HostedWindow.hpp"

#include <chrono>
#include <cstdio>
#include <cstring>

#include <fcntl.h>
#include <sys/eventfd.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>

#include "Settings/Settings.hpp"

static void Signal(HostedWindow::Session& session)
{
    if (session.signalFileDescriptor < 0) return;

    uint64_t one = 1;
    ssize_t written = write(session.signalFileDescriptor, &one, sizeof(one));
    (void)written;
}

static void QueueInput(HostedWindow::Session& session, const WaylandClient::InputEvent& event)
{
    if (session.queueCount >= HostedWindow::MAX_QUEUED_INPUT)
    {
        session.queueDropped += 1;
        return;
    }

    int slot = (session.queueHead + session.queueCount) % HostedWindow::MAX_QUEUED_INPUT;
    session.queued[slot] = event;
    session.queueCount += 1;
}

static void ReleaseSharedBuffers(HostedWindow::Session& session)
{
    for (uint32_t index = 0; index < session.bufferCount; ++index)
    {
        HostedWindow::Buffer& buffer = session.buffers[index];

        if (buffer.pixels) munmap(buffer.pixels, buffer.sizeBytes);
        if (buffer.fileDescriptor >= 0) close(buffer.fileDescriptor);

        buffer = HostedWindow::Buffer{};
    }

    session.bufferCount = 0;
}

static bool ReadFormatTable(HostedWindow::Display& display, uint32_t formatCount, int descriptor)
{
    display.formats.clear();

    if (formatCount == 0 || formatCount > ControlChannel::MAX_FORMAT_ENTRIES) return false;

    struct stat described{};
    size_t sizeBytes = (size_t)formatCount * sizeof(ControlChannel::FormatEntry);

    if (fstat(descriptor, &described) != 0 || (size_t)described.st_size < sizeBytes) return false;

    void* mapped = mmap(nullptr, sizeBytes, PROT_READ, MAP_PRIVATE, descriptor, 0);
    if (mapped == MAP_FAILED) return false;

    const ControlChannel::FormatEntry* entries = (const ControlChannel::FormatEntry*)mapped;
    display.formats.assign(entries, entries + formatCount);
    munmap(mapped, sizeBytes);

    return true;
}

static void AdoptDisplay(HostedWindow::Session& session, const ControlChannel::DisplayPayload& payload, const int* descriptors,
                         uint32_t descriptorCount)
{
    HostedWindow::Display& display = session.display;
    display.width = payload.width;
    display.height = payload.height;
    display.refreshMilliHertz = payload.refreshMilliHertz;
    display.scale = payload.scale == 0 ? 1 : payload.scale;
    display.physicalWidthMm = payload.physicalWidthMm;
    display.physicalHeightMm = payload.physicalHeightMm;
    display.renderDevice = payload.renderDevice;
    display.dmabufAvailable = payload.dmabufAvailable;
    display.described = true;

    if (descriptorCount > 0 && !ReadFormatTable(display, payload.formatCount, descriptors[0])) display.dmabufAvailable = false;
    if (descriptorCount == 0 && display.formats.empty()) display.dmabufAvailable = false;
}

static WaylandClient::InputEvent TranslateInput(const ControlChannel::InputPayload& source)
{
    WaylandClient::InputEvent event{};
    event.kind = source.kind == ControlChannel::InputKind::Key ? WaylandClient::InputKind::Key : WaylandClient::InputKind::Touch;
    event.action = source.action;
    event.pointerId = source.pointerId;
    event.keyCode = source.keyCode;
    event.unicode = source.unicode;
    event.metaState = source.metaState;
    event.x = source.x;
    event.y = source.y;
    event.timeNanos = source.timeNanos;

    return event;
}

static void HandleMessage(HostedWindow::Session& session, const ControlChannel::Message& message, const int* descriptors,
                          uint32_t descriptorCount)
{
    HostedWindow::ReleaseListener listener = nullptr;
    void* listenerContext = nullptr;
    uint32_t releasedGeneration = 0;
    uint32_t releasedIndex = 0;
    bool refused = false;

    {
        std::lock_guard guard(session.mutex);

        switch (message.kind)
        {
            case ControlChannel::MessageKind::Display:
                AdoptDisplay(session, message.payload.display, descriptors, descriptorCount);
                break;

            case ControlChannel::MessageKind::Configure:
                if (message.payload.configure.width == 0 || message.payload.configure.height == 0) break;
                if (message.payload.configure.width > WaylandClient::MAX_DIMENSION) break;
                if (message.payload.configure.height > WaylandClient::MAX_DIMENSION) break;

                session.configuredWidth = message.payload.configure.width;
                session.configuredHeight = message.payload.configure.height;
                session.configurePending = true;
                break;

            case ControlChannel::MessageKind::PresentDone:
                session.frameArrived = true;
                session.lastFrameTimeNanos = message.payload.presentDone.frameTimeNanos;
                session.presentsCompleted += 1;
                break;

            case ControlChannel::MessageKind::BufferReleased:
                releasedGeneration = message.payload.bufferReleased.generation;
                releasedIndex = message.payload.bufferReleased.index;

                if (releasedGeneration == session.generation)
                {
                    if (releasedIndex < session.bufferCount) session.buffers[releasedIndex].heldByShell = false;
                    break;
                }

                listener = session.releaseListener;
                listenerContext = session.releaseContext;
                break;

            case ControlChannel::MessageKind::ForgetBuffers:
                releasedGeneration = message.payload.forgetBuffers.generation;
                releasedIndex = HostedWindow::INVALID_BUFFER_INDEX;
                refused = true;
                listener = session.releaseListener;
                listenerContext = session.releaseContext;
                break;

            case ControlChannel::MessageKind::Input:
                QueueInput(session, TranslateInput(message.payload.input));
                break;

            case ControlChannel::MessageKind::Stop:
                session.stopRequested = true;
                break;

            default:
                break;
        }

        if (listener) listener(listenerContext, releasedGeneration, releasedIndex, refused);
    }

    session.changed.notify_all();
    Signal(session);
}

static void Receive(HostedWindow::Session* session)
{
    for (;;)
    {
        ControlChannel::Message message{};
        int descriptors[ControlChannel::MAX_DESCRIPTORS];
        uint32_t descriptorCount = ControlChannel::MAX_DESCRIPTORS;

        ControlChannel::Result received = ControlChannel::ReceiveWithDescriptors(session->channel, message, descriptors, descriptorCount, -1);

        if (received == ControlChannel::Result::Ok) HandleMessage(*session, message, descriptors, descriptorCount);

        for (uint32_t index = 0; index < descriptorCount; ++index) close(descriptors[index]);

        if (received == ControlChannel::Result::Ok || received == ControlChannel::Result::Malformed) continue;
        if (received == ControlChannel::Result::WouldBlock) continue;

        {
            std::lock_guard<std::mutex> guard(session->mutex);
            session->stopRequested = true;
        }

        session->changed.notify_all();
        Signal(*session);
        return;
    }
}

static HostedWindow::Result SendRegistration(HostedWindow::Session& session, const HostedWindow::BufferDescription& description,
                                             const int* descriptors, uint32_t descriptorCount)
{
    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::RegisterBuffer);

    ControlChannel::RegisterBufferPayload& payload = message.payload.registerBuffer;
    payload.generation = description.generation;
    payload.index = description.index;
    payload.kind = description.kind;
    payload.width = description.width;
    payload.height = description.height;
    payload.format = description.format;
    payload.modifier = description.modifier;
    payload.planeCount = description.planeCount;

    for (uint32_t plane = 0; plane < ControlChannel::MAX_PLANES; ++plane)
    {
        payload.offsets[plane] = description.offsets[plane];
        payload.strides[plane] = description.strides[plane];
    }

    ControlChannel::Result sent = ControlChannel::SendWithDescriptors(session.channel, message, descriptors, descriptorCount);

    if (sent == ControlChannel::Result::Ok) return HostedWindow::Result::Ok;
    if (sent == ControlChannel::Result::InvalidArgument) return HostedWindow::Result::InvalidArgument;

    return HostedWindow::Result::PeerClosed;
}

static HostedWindow::Result AllocateSharedBuffers(HostedWindow::Session& session, uint32_t width, uint32_t height)
{
    uint32_t stride = width * 4;
    size_t sizeBytes = (size_t)stride * height;
    uint32_t generation = session.nextGeneration++;

    for (uint32_t index = 0; index < ControlChannel::FRAME_BUFFER_COUNT; ++index)
    {
        int descriptor = memfd_create("acl-frame", MFD_CLOEXEC | MFD_ALLOW_SEALING);
        if (descriptor < 0) return HostedWindow::Result::MappingFailed;

        if (ftruncate(descriptor, (off_t)sizeBytes) != 0 || fcntl(descriptor, F_ADD_SEALS, F_SEAL_SHRINK | F_SEAL_GROW | F_SEAL_SEAL) != 0)
        {
            close(descriptor);
            return HostedWindow::Result::MappingFailed;
        }

        void* mapped = mmap(nullptr, sizeBytes, PROT_READ | PROT_WRITE, MAP_SHARED, descriptor, 0);
        if (mapped == MAP_FAILED)
        {
            close(descriptor);
            return HostedWindow::Result::MappingFailed;
        }

        session.buffers[index].pixels = (uint8_t*)mapped;
        session.buffers[index].sizeBytes = sizeBytes;
        session.buffers[index].fileDescriptor = descriptor;
        session.buffers[index].heldByShell = false;
        session.buffers[index].acquired = false;
        session.bufferCount = index + 1;
    }

    session.generation = generation;
    session.width = width;
    session.height = height;
    session.stride = stride;

    for (uint32_t index = 0; index < session.bufferCount; ++index)
    {
        HostedWindow::BufferDescription description{};
        description.generation = generation;
        description.index = index;
        description.kind = ControlChannel::BufferKind::Shared;
        description.width = width;
        description.height = height;
        description.format = ControlChannel::FORMAT_ARGB8888;
        description.planeCount = 1;
        description.strides[0] = stride;

        HostedWindow::Result registered = SendRegistration(session, description, &session.buffers[index].fileDescriptor, 1);
        if (registered != HostedWindow::Result::Ok) return registered;
    }

    return HostedWindow::Result::Ok;
}

bool HostedWindow::IsRequested()
{
    return ControlChannel::IsConfigured();
}

bool HostedWindow::DmabufWanted()
{
    const char* value = Settings::Lookup(NO_DMABUF_SETTING);
    return !value || strcmp(value, "0") != 0;
}

HostedWindow::Result HostedWindow::Open(uint32_t width, uint32_t height, const char* title, const char* packageName, Session& session)
{
    if (session.open) return Result::AlreadyOpen;

    if (ControlChannel::AdoptFromEnvironment(session.channel) != ControlChannel::Result::Ok) return Result::NoChannel;

    ControlChannel::Message hello{};
    ControlChannel::Prepare(hello, ControlChannel::MessageKind::Hello);
    hello.payload.hello.version = ControlChannel::PROTOCOL_VERSION;
    hello.payload.hello.width = width;
    hello.payload.hello.height = height;
    ControlChannel::CopyText(hello.payload.hello.title, ControlChannel::MAX_NAME_BYTES, title);
    ControlChannel::CopyText(hello.payload.hello.packageName, ControlChannel::MAX_NAME_BYTES, packageName);

    if (ControlChannel::Send(session.channel, hello) != ControlChannel::Result::Ok)
    {
        ControlChannel::Close(session.channel);
        return Result::HandshakeFailed;
    }

    auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(HANDSHAKE_TIMEOUT_MILLIS);

    while (!session.display.described)
    {
        int remaining = (int)std::chrono::duration_cast<std::chrono::milliseconds>(deadline - std::chrono::steady_clock::now()).count();

        ControlChannel::Message reply{};
        int descriptors[ControlChannel::MAX_DESCRIPTORS];
        uint32_t descriptorCount = ControlChannel::MAX_DESCRIPTORS;

        ControlChannel::Result received = remaining <= 0 ? ControlChannel::Result::WouldBlock
                                                         : ControlChannel::ReceiveWithDescriptors(session.channel, reply, descriptors,
                                                                                                  descriptorCount, remaining);

        if (received == ControlChannel::Result::Ok) HandleMessage(session, reply, descriptors, descriptorCount);
        for (uint32_t index = 0; index < descriptorCount; ++index) close(descriptors[index]);

        if (received == ControlChannel::Result::Ok || received == ControlChannel::Result::Malformed) continue;

        fprintf(stderr, "[hostedwindow] the window host did not describe its window: %s\n", ControlChannel::ResultName(received));
        ControlChannel::Close(session.channel);
        session.display = Display{};
        return Result::HandshakeFailed;
    }

    uint32_t bufferWidth = session.configurePending ? session.configuredWidth : width;
    uint32_t bufferHeight = session.configurePending ? session.configuredHeight : height;
    session.configurePending = false;

    Result allocated = AllocateSharedBuffers(session, bufferWidth, bufferHeight);

    if (allocated != Result::Ok)
    {
        fprintf(stderr, "[hostedwindow] the frame buffers cannot be shared with the window host: %s\n", ResultName(allocated));
        ReleaseSharedBuffers(session);
        ControlChannel::Close(session.channel);
        return allocated;
    }

    session.signalFileDescriptor = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);
    session.open = true;
    session.receiver = std::thread(Receive, &session);

    fprintf(stderr, "[hostedwindow] hosted by a shell: %u buffer(s) of %ux%u, stride %u, %s\n", session.bufferCount, session.width,
            session.height, session.stride, session.display.dmabufAvailable ? "gpu buffers can be shared" : "gpu buffers cannot be shared");

    return Result::Ok;
}

void HostedWindow::Close(Session& session)
{
    if (!session.open && session.channel.fileDescriptor == ControlChannel::INVALID_FILE_DESCRIPTOR) return;

    if (session.channel.fileDescriptor >= 0) shutdown(session.channel.fileDescriptor, SHUT_RDWR);
    if (session.receiver.joinable()) session.receiver.join();

    std::lock_guard<std::mutex> guard(session.mutex);

    ReleaseSharedBuffers(session);
    ControlChannel::Close(session.channel);

    if (session.signalFileDescriptor >= 0) close(session.signalFileDescriptor);
    session.signalFileDescriptor = INVALID_FILE_DESCRIPTOR;

    session.open = false;
}

HostedWindow::Result HostedWindow::AcquireBuffer(Session& session, Buffer*& buffer, uint32_t& index)
{
    if (!session.open) return Result::NotOpen;

    std::lock_guard<std::mutex> guard(session.mutex);

    for (uint32_t candidate = 0; candidate < session.bufferCount; ++candidate)
    {
        if (session.buffers[candidate].heldByShell || session.buffers[candidate].acquired) continue;

        session.buffers[candidate].acquired = true;
        buffer = &session.buffers[candidate];
        index = candidate;
        return Result::Ok;
    }

    return Result::NoFreeBuffer;
}

HostedWindow::Result HostedWindow::Present(Session& session, uint32_t index, uint64_t frameTimeNanos)
{
    if (!session.open) return Result::NotOpen;

    uint32_t generation = 0;
    uint64_t sequence = 0;

    {
        std::lock_guard<std::mutex> guard(session.mutex);

        if (index >= session.bufferCount || !session.buffers[index].acquired) return Result::NoFreeBuffer;

        generation = session.generation;
        sequence = session.sequence++;
        session.buffers[index].acquired = false;
        session.buffers[index].heldByShell = true;
    }

    ControlChannel::Message ready{};
    ControlChannel::Prepare(ready, ControlChannel::MessageKind::FrameReady);
    ready.payload.frameReady.generation = generation;
    ready.payload.frameReady.index = index;
    ready.payload.frameReady.sequence = sequence;
    ready.payload.frameReady.frameTimeNanos = frameTimeNanos;

    return ControlChannel::Send(session.channel, ready) == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

HostedWindow::Result HostedWindow::RequestFrame(Session& session)
{
    if (!session.open) return Result::NotOpen;

    ControlChannel::Message request{};
    ControlChannel::Prepare(request, ControlChannel::MessageKind::RequestFrame);

    return ControlChannel::Send(session.channel, request) == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

HostedWindow::Result HostedWindow::Resize(Session& session, uint32_t width, uint32_t height)
{
    if (!session.open) return Result::NotOpen;
    if (width == 0 || height == 0 || width > WaylandClient::MAX_DIMENSION || height > WaylandClient::MAX_DIMENSION) return Result::InvalidArgument;

    uint32_t retired = 0;

    {
        std::lock_guard<std::mutex> guard(session.mutex);

        for (uint32_t index = 0; index < session.bufferCount; ++index)
        {
            if (session.buffers[index].acquired) return Result::NoFreeBuffer;
        }

        retired = session.generation;
        ReleaseSharedBuffers(session);

        Result allocated = AllocateSharedBuffers(session, width, height);
        if (allocated != Result::Ok)
        {
            ReleaseSharedBuffers(session);
            return allocated;
        }
    }

    return ForgetBuffers(session, retired);
}

bool HostedWindow::ConsumeConfigure(Session& session, uint32_t& width, uint32_t& height)
{
    std::lock_guard<std::mutex> guard(session.mutex);

    if (!session.configurePending) return false;

    session.configurePending = false;
    width = session.configuredWidth;
    height = session.configuredHeight;

    return true;
}

bool HostedWindow::ConsumeFrame(Session& session)
{
    std::lock_guard<std::mutex> guard(session.mutex);

    if (!session.frameArrived) return false;

    session.frameArrived = false;
    return true;
}

bool HostedWindow::IsStopRequested(Session& session)
{
    std::lock_guard<std::mutex> guard(session.mutex);
    return session.stopRequested;
}

bool HostedWindow::NextInputEvent(Session& session, WaylandClient::InputEvent& event)
{
    std::lock_guard<std::mutex> guard(session.mutex);

    if (session.queueCount <= 0) return false;

    event = session.queued[session.queueHead];
    session.queueHead = (session.queueHead + 1) % MAX_QUEUED_INPUT;
    session.queueCount -= 1;

    return true;
}

void HostedWindow::DrainSignal(Session& session)
{
    if (session.signalFileDescriptor < 0) return;

    uint64_t drained = 0;
    ssize_t got = read(session.signalFileDescriptor, &drained, sizeof(drained));
    (void)got;
}

HostedWindow::Display HostedWindow::CurrentDisplay(Session& session)
{
    std::lock_guard<std::mutex> guard(session.mutex);
    return session.display;
}

void HostedWindow::CurrentSize(Session& session, uint32_t& width, uint32_t& height)
{
    std::lock_guard<std::mutex> guard(session.mutex);

    width = session.configuredWidth != 0 ? session.configuredWidth : session.width;
    height = session.configuredHeight != 0 ? session.configuredHeight : session.height;
}

uint32_t HostedWindow::ReserveGeneration(Session& session)
{
    std::lock_guard<std::mutex> guard(session.mutex);
    return session.nextGeneration++;
}

void HostedWindow::SetReleaseListener(Session& session, ReleaseListener listener, void* context)
{
    std::lock_guard<std::mutex> guard(session.mutex);

    session.releaseListener = listener;
    session.releaseContext = context;
}

HostedWindow::Result HostedWindow::RegisterBuffer(Session& session, const BufferDescription& description, const int* descriptors,
                                                  uint32_t descriptorCount)
{
    if (!session.open) return Result::NotOpen;
    if (description.planeCount == 0 || description.planeCount > ControlChannel::MAX_PLANES) return Result::InvalidArgument;
    if (descriptorCount != description.planeCount || description.index >= ControlChannel::MAX_FRAME_BUFFERS) return Result::InvalidArgument;

    return SendRegistration(session, description, descriptors, descriptorCount);
}

HostedWindow::Result HostedWindow::ForgetBuffers(Session& session, uint32_t generation)
{
    if (!session.open) return Result::NotOpen;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::ForgetBuffers);
    message.payload.forgetBuffers.generation = generation;

    return ControlChannel::Send(session.channel, message) == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

HostedWindow::Result HostedWindow::PresentBuffer(Session& session, uint32_t generation, uint32_t index, int fenceDescriptor)
{
    if (!session.open) return Result::NotOpen;

    uint64_t sequence = 0;

    {
        std::lock_guard<std::mutex> guard(session.mutex);
        sequence = session.sequence++;
    }

    ControlChannel::Message ready{};
    ControlChannel::Prepare(ready, ControlChannel::MessageKind::FrameReady);
    ready.payload.frameReady.generation = generation;
    ready.payload.frameReady.index = index;
    ready.payload.frameReady.sequence = sequence;
    ready.payload.frameReady.hasFence = fenceDescriptor >= 0;

    ControlChannel::Result sent = fenceDescriptor >= 0 ? ControlChannel::SendWithDescriptors(session.channel, ready, &fenceDescriptor, 1)
                                                       : ControlChannel::Send(session.channel, ready);

    return sent == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

uint64_t HostedWindow::PresentsCompleted(Session& session)
{
    std::lock_guard<std::mutex> guard(session.mutex);
    return session.presentsCompleted;
}

bool HostedWindow::WaitForPresentAfter(Session& session, uint64_t completed, int timeoutMillis)
{
    std::unique_lock<std::mutex> guard(session.mutex);

    return session.changed.wait_for(guard, std::chrono::milliseconds(timeoutMillis),
                                    [&] { return session.presentsCompleted > completed || session.stopRequested; });
}

HostedWindow::Result HostedWindow::SetFullscreen(Session& session, bool fullscreen)
{
    if (!session.open) return Result::NotOpen;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::SetFullscreen);
    message.payload.fullscreen.fullscreen = fullscreen;

    return ControlChannel::Send(session.channel, message) == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

HostedWindow::Result HostedWindow::SetTitle(Session& session, const char* title)
{
    if (!session.open) return Result::NotOpen;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::SetTitle);
    ControlChannel::CopyText(message.payload.title.title, ControlChannel::MAX_NAME_BYTES, title);

    return ControlChannel::Send(session.channel, message) == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

void HostedWindow::ReportGuestPresents(Session& session)
{
    if (!session.open) return;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::GuestPresents);
    ControlChannel::Send(session.channel, message);
}

bool HostedWindow::RequestLaunch(Session& session, const char* packageName)
{
    if (!session.open || !packageName || packageName[0] == '\0') return false;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::LaunchPackage);

    if (!ControlChannel::CopyText(message.payload.launchPackage.packageName, ControlChannel::MAX_PACKAGE_BYTES, packageName)) return false;

    return ControlChannel::Send(session.channel, message) == ControlChannel::Result::Ok;
}

void HostedWindow::SendExit(Session& session, int32_t status)
{
    if (session.channel.fileDescriptor == ControlChannel::INVALID_FILE_DESCRIPTOR) return;

    ControlChannel::Message message{};
    ControlChannel::Prepare(message, ControlChannel::MessageKind::Exit);
    message.payload.exit.status = status;

    ControlChannel::Send(session.channel, message);
}

const char* HostedWindow::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::AlreadyOpen: return "AlreadyOpen";
        case Result::NotOpen: return "NotOpen";
        case Result::NoChannel: return "NoChannel";
        case Result::HandshakeFailed: return "HandshakeFailed";
        case Result::MappingFailed: return "MappingFailed";
        case Result::NoFreeBuffer: return "NoFreeBuffer";
        case Result::PeerClosed: return "PeerClosed";
        case Result::InvalidArgument: return "InvalidArgument";
        case Result::TimedOut: return "TimedOut";
    }

    return "Unknown";
}

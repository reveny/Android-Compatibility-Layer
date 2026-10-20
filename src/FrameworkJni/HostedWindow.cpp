#include "FrameworkJni/HostedWindow.hpp"

#include <cstdio>
#include <cstring>

#include <sys/mman.h>
#include <unistd.h>

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

static void ReleaseBuffers(HostedWindow::Session& session)
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

static bool AdoptBuffers(HostedWindow::Session& session, const ControlChannel::BuffersPayload& payload, const int* descriptors,
                         uint32_t descriptorCount)
{
    if (payload.count == 0 || payload.count > ControlChannel::MAX_FRAME_BUFFERS) return false;
    if (payload.count != descriptorCount) return false;
    if (payload.width == 0 || payload.height == 0) return false;
    if (payload.stride < payload.width * 4) return false;

    size_t sizeBytes = (size_t)payload.stride * payload.height;

    for (uint32_t index = 0; index < descriptorCount; ++index)
    {
        void* mapped = mmap(nullptr, sizeBytes, PROT_READ | PROT_WRITE, MAP_SHARED, descriptors[index], 0);
        if (mapped == MAP_FAILED)
        {
            for (uint32_t taken = 0; taken < descriptorCount; ++taken) close(descriptors[taken]);
            ReleaseBuffers(session);
            return false;
        }

        session.buffers[index].pixels = (uint8_t*)mapped;
        session.buffers[index].sizeBytes = sizeBytes;
        session.buffers[index].fileDescriptor = descriptors[index];
        session.buffers[index].heldByShell = false;
    }

    session.bufferCount = payload.count;
    session.width = payload.width;
    session.height = payload.height;
    session.stride = payload.stride;

    return true;
}

static void HandleMessage(HostedWindow::Session& session, const ControlChannel::Message& message)
{
    if (message.kind == ControlChannel::MessageKind::PresentDone)
    {
        session.frameArrived = true;
        session.framePending = false;
        session.lastFrameTimeNanos = message.payload.presentDone.frameTimeNanos;
        return;
    }

    if (message.kind == ControlChannel::MessageKind::BufferReleased)
    {
        uint32_t index = message.payload.bufferReleased.index;
        if (index < session.bufferCount) session.buffers[index].heldByShell = false;
        return;
    }

    if (message.kind == ControlChannel::MessageKind::Input)
    {
        const ControlChannel::InputPayload& source = message.payload.input;

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

        QueueInput(session, event);
        return;
    }

    if (message.kind == ControlChannel::MessageKind::Stop)
    {
        session.stopRequested = true;
    }
}

bool HostedWindow::IsRequested()
{
    return ControlChannel::IsConfigured();
}

HostedWindow::Result HostedWindow::Open(uint32_t width, uint32_t height, const char* packageName, Session& session)
{
    if (session.open) return Result::AlreadyOpen;

    if (ControlChannel::AdoptFromEnvironment(session.channel) != ControlChannel::Result::Ok) return Result::NoChannel;

    ControlChannel::Message hello{};
    ControlChannel::Prepare(hello, ControlChannel::MessageKind::Hello);
    hello.payload.hello.version = ControlChannel::PROTOCOL_VERSION;
    hello.payload.hello.width = width;
    hello.payload.hello.height = height;
    ControlChannel::CopyText(hello.payload.hello.packageName, ControlChannel::MAX_NAME_BYTES, packageName);

    if (ControlChannel::Send(session.channel, hello) != ControlChannel::Result::Ok)
    {
        ControlChannel::Close(session.channel);
        return Result::HandshakeFailed;
    }

    ControlChannel::Message reply{};
    int descriptors[ControlChannel::MAX_DESCRIPTORS];
    uint32_t descriptorCount = ControlChannel::MAX_DESCRIPTORS;

    ControlChannel::Result received =
        ControlChannel::ReceiveWithDescriptors(session.channel, reply, descriptors, descriptorCount, HANDSHAKE_TIMEOUT_MILLIS);

    if (received != ControlChannel::Result::Ok || reply.kind != ControlChannel::MessageKind::Buffers)
    {
        for (uint32_t index = 0; index < descriptorCount; ++index) close(descriptors[index]);
        fprintf(stderr, "[hostedwindow] the shell did not hand over frame buffers\n");
        ControlChannel::Close(session.channel);
        return Result::HandshakeFailed;
    }

    if (!AdoptBuffers(session, reply.payload.buffers, descriptors, descriptorCount))
    {
        fprintf(stderr, "[hostedwindow] the frame buffers the shell offered cannot be mapped\n");
        ControlChannel::Close(session.channel);
        return Result::MappingFailed;
    }

    session.open = true;

    fprintf(stderr, "[hostedwindow] hosted by a shell: %u buffer(s) of %ux%u, stride %u\n", session.bufferCount, session.width,
            session.height, session.stride);

    return Result::Ok;
}

void HostedWindow::Close(Session& session)
{
    if (!session.open && session.channel.fileDescriptor == ControlChannel::INVALID_FILE_DESCRIPTOR) return;

    ReleaseBuffers(session);
    ControlChannel::Close(session.channel);

    session.currentIndex = INVALID_BUFFER_INDEX;
    session.open = false;
}

HostedWindow::Result HostedWindow::AcquireBuffer(Session& session, Buffer*& buffer)
{
    if (!session.open) return Result::NotOpen;

    for (int attempt = 0; attempt < 2; ++attempt)
    {
        for (uint32_t index = 0; index < session.bufferCount; ++index)
        {
            if (session.buffers[index].heldByShell) continue;

            session.currentIndex = index;
            buffer = &session.buffers[index];
            return Result::Ok;
        }

        if (attempt == 0) Pump(session, 0);
    }

    return Result::NoFreeBuffer;
}

HostedWindow::Result HostedWindow::Present(Session& session, uint64_t frameTimeNanos)
{
    if (!session.open) return Result::NotOpen;
    if (session.currentIndex >= session.bufferCount) return Result::NoFreeBuffer;

    ControlChannel::Message ready{};
    ControlChannel::Prepare(ready, ControlChannel::MessageKind::FrameReady);
    ready.payload.frameReady.index = session.currentIndex;
    ready.payload.frameReady.sequence = session.sequence;
    ready.payload.frameReady.frameTimeNanos = frameTimeNanos;

    ControlChannel::Result sent = ControlChannel::Send(session.channel, ready);

    if (sent == ControlChannel::Result::PeerClosed)
    {
        session.stopRequested = true;
        return Result::PeerClosed;
    }

    session.buffers[session.currentIndex].heldByShell = true;
    session.currentIndex = INVALID_BUFFER_INDEX;
    session.sequence += 1;

    return sent == ControlChannel::Result::Ok ? Result::Ok : Result::PeerClosed;
}

HostedWindow::Result HostedWindow::RequestFrame(Session& session)
{
    if (!session.open) return Result::NotOpen;

    session.framePending = true;
    return Result::Ok;
}

HostedWindow::Result HostedWindow::Pump(Session& session, int timeoutMillis)
{
    if (!session.open) return Result::NotOpen;

    int remaining = timeoutMillis;

    for (;;)
    {
        ControlChannel::Message message{};
        ControlChannel::Result received = ControlChannel::Receive(session.channel, message, remaining);

        if (received == ControlChannel::Result::WouldBlock) return Result::Ok;

        if (received == ControlChannel::Result::PeerClosed)
        {
            session.stopRequested = true;
            return Result::PeerClosed;
        }

        if (received == ControlChannel::Result::Ok) HandleMessage(session, message);

        remaining = 0;
    }
}

bool HostedWindow::NextInputEvent(Session& session, WaylandClient::InputEvent& event)
{
    if (session.queueCount <= 0) return false;

    event = session.queued[session.queueHead];
    session.queueHead = (session.queueHead + 1) % MAX_QUEUED_INPUT;
    session.queueCount -= 1;

    return true;
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
    }

    return "Unknown";
}

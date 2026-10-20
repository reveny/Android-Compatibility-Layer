#include "ControlChannel/ControlChannel.hpp"

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <fcntl.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

#include "Settings/Settings.hpp"

static bool KindIsKnown(ControlChannel::MessageKind kind)
{
    return kind > ControlChannel::MessageKind::Invalid && kind <= ControlChannel::MessageKind::Exit;
}

static void TerminateText(char* text, uint32_t capacity)
{
    if (!text || capacity == 0) return;

    text[capacity - 1] = '\0';
}

static void SanitiseMessage(ControlChannel::Message& message)
{
    if (message.kind == ControlChannel::MessageKind::Hello)
    {
        TerminateText(message.payload.hello.packageName, ControlChannel::MAX_NAME_BYTES);
        return;
    }

    if (message.kind == ControlChannel::MessageKind::Progress)
    {
        TerminateText(message.payload.progress.libraryName, ControlChannel::MAX_NAME_BYTES);
        return;
    }

    if (message.kind == ControlChannel::MessageKind::Log)
    {
        TerminateText(message.payload.log.tag, ControlChannel::MAX_TAG_BYTES);
        TerminateText(message.payload.log.text, ControlChannel::MAX_TEXT_BYTES);
        return;
    }

    if (message.kind == ControlChannel::MessageKind::LaunchPackage)
    {
        TerminateText(message.payload.launchPackage.packageName, ControlChannel::MAX_PACKAGE_BYTES);
    }
}

static ControlChannel::Result WaitReadable(int fileDescriptor, int timeoutMillis)
{
    pollfd watched{};
    watched.fd = fileDescriptor;
    watched.events = POLLIN;

    for (;;)
    {
        int ready = poll(&watched, 1, timeoutMillis);
        if (ready > 0) return ControlChannel::Result::Ok;
        if (ready == 0) return ControlChannel::Result::WouldBlock;
        if (errno == EINTR) continue;

        return ControlChannel::Result::ReceiveFailed;
    }
}

ControlChannel::Result ControlChannel::CreatePair(Endpoint& endpoint, int& peerDescriptor)
{
    if (endpoint.fileDescriptor != INVALID_FILE_DESCRIPTOR) return Result::AlreadyOpen;

    int pair[2] = {INVALID_FILE_DESCRIPTOR, INVALID_FILE_DESCRIPTOR};

    if (socketpair(AF_UNIX, SOCK_SEQPACKET | SOCK_CLOEXEC, 0, pair) != 0) return Result::SendFailed;

    int flags = fcntl(pair[1], F_GETFD);
    if (flags < 0 || fcntl(pair[1], F_SETFD, flags & ~FD_CLOEXEC) != 0)
    {
        close(pair[0]);
        close(pair[1]);
        return Result::SendFailed;
    }

    endpoint = Endpoint{};
    endpoint.fileDescriptor = pair[0];
    peerDescriptor = pair[1];

    return Result::Ok;
}

ControlChannel::Result ControlChannel::Adopt(int fileDescriptor, Endpoint& endpoint)
{
    if (fileDescriptor < 0) return Result::InvalidArgument;
    if (fcntl(fileDescriptor, F_GETFD) < 0) return Result::InvalidArgument;

    endpoint = Endpoint{};
    endpoint.fileDescriptor = fileDescriptor;

    return Result::Ok;
}

ControlChannel::Result ControlChannel::AdoptFromEnvironment(Endpoint& endpoint)
{
    const char* value = Settings::Lookup(DESCRIPTOR_SETTING);
    if (!value || value[0] == '\0') return Result::NotConfigured;

    char* end = nullptr;
    long parsed = strtol(value, &end, 10);

    if (!end || *end != '\0' || parsed < 0 || parsed > 65535) return Result::InvalidArgument;

    return Adopt((int)parsed, endpoint);
}

void ControlChannel::Close(Endpoint& endpoint)
{
    if (endpoint.fileDescriptor == INVALID_FILE_DESCRIPTOR) return;

    close(endpoint.fileDescriptor);
    endpoint.fileDescriptor = INVALID_FILE_DESCRIPTOR;
}

ControlChannel::Result ControlChannel::Send(Endpoint& endpoint, const Message& message)
{
    return SendWithDescriptors(endpoint, message, nullptr, 0);
}

ControlChannel::Result ControlChannel::SendWithDescriptors(Endpoint& endpoint, const Message& message, const int* descriptors,
                                                           uint32_t descriptorCount)
{
    if (endpoint.fileDescriptor == INVALID_FILE_DESCRIPTOR) return Result::NotOpen;
    if (!KindIsKnown(message.kind)) return Result::InvalidArgument;
    if (descriptorCount > MAX_DESCRIPTORS) return Result::InvalidArgument;
    if (descriptorCount > 0 && !descriptors) return Result::InvalidArgument;

    iovec span{};
    span.iov_base = (void*)&message;
    span.iov_len = sizeof(Message);

    alignas(cmsghdr) char control[CMSG_SPACE(sizeof(int) * MAX_DESCRIPTORS)];
    memset(control, 0, sizeof(control));

    msghdr header{};
    header.msg_iov = &span;
    header.msg_iovlen = 1;

    if (descriptorCount > 0)
    {
        header.msg_control = control;
        header.msg_controllen = CMSG_SPACE(sizeof(int) * descriptorCount);

        cmsghdr* entry = CMSG_FIRSTHDR(&header);
        entry->cmsg_level = SOL_SOCKET;
        entry->cmsg_type = SCM_RIGHTS;
        entry->cmsg_len = CMSG_LEN(sizeof(int) * descriptorCount);
        memcpy(CMSG_DATA(entry), descriptors, sizeof(int) * descriptorCount);
    }

    for (;;)
    {
        ssize_t written = sendmsg(endpoint.fileDescriptor, &header, MSG_NOSIGNAL);

        if (written == (ssize_t)sizeof(Message))
        {
            endpoint.messagesSent += 1;
            return Result::Ok;
        }

        if (written < 0 && errno == EINTR) continue;
        if (written < 0 && (errno == EPIPE || errno == ECONNRESET))
        {
            endpoint.peerClosed = true;
            return Result::PeerClosed;
        }

        return Result::SendFailed;
    }
}

ControlChannel::Result ControlChannel::Receive(Endpoint& endpoint, Message& message, int timeoutMillis)
{
    uint32_t descriptorCount = 0;
    return ReceiveWithDescriptors(endpoint, message, nullptr, descriptorCount, timeoutMillis);
}

ControlChannel::Result ControlChannel::ReceiveWithDescriptors(Endpoint& endpoint, Message& message, int* descriptors,
                                                              uint32_t& descriptorCount, int timeoutMillis)
{
    uint32_t capacity = descriptors ? descriptorCount : 0;
    descriptorCount = 0;

    if (endpoint.fileDescriptor == INVALID_FILE_DESCRIPTOR) return Result::NotOpen;
    if (capacity > MAX_DESCRIPTORS) return Result::InvalidArgument;

    Result ready = WaitReadable(endpoint.fileDescriptor, timeoutMillis);
    if (ready != Result::Ok) return ready;

    memset(&message, 0, sizeof(Message));

    iovec span{};
    span.iov_base = &message;
    span.iov_len = sizeof(Message);

    alignas(cmsghdr) char control[CMSG_SPACE(sizeof(int) * MAX_DESCRIPTORS)];
    memset(control, 0, sizeof(control));

    msghdr header{};
    header.msg_iov = &span;
    header.msg_iovlen = 1;
    header.msg_control = control;
    header.msg_controllen = sizeof(control);

    ssize_t read = 0;

    for (;;)
    {
        read = recvmsg(endpoint.fileDescriptor, &header, MSG_CMSG_CLOEXEC);
        if (read >= 0) break;
        if (errno == EINTR) continue;

        if (errno == ECONNRESET)
        {
            endpoint.peerClosed = true;
            return Result::PeerClosed;
        }

        return Result::ReceiveFailed;
    }

    if (read == 0)
    {
        endpoint.peerClosed = true;
        return Result::PeerClosed;
    }

    for (cmsghdr* entry = CMSG_FIRSTHDR(&header); entry; entry = CMSG_NXTHDR(&header, entry))
    {
        if (entry->cmsg_level != SOL_SOCKET || entry->cmsg_type != SCM_RIGHTS) continue;

        uint32_t carried = (uint32_t)((entry->cmsg_len - CMSG_LEN(0)) / sizeof(int));

        for (uint32_t index = 0; index < carried; ++index)
        {
            int carriedDescriptor = INVALID_FILE_DESCRIPTOR;
            memcpy(&carriedDescriptor, CMSG_DATA(entry) + index * sizeof(int), sizeof(int));

            if (descriptorCount < capacity)
            {
                descriptors[descriptorCount] = carriedDescriptor;
                descriptorCount += 1;
                continue;
            }

            close(carriedDescriptor);
        }
    }

    if (read != (ssize_t)sizeof(Message) || (header.msg_flags & MSG_TRUNC) != 0 || !KindIsKnown(message.kind))
    {
        endpoint.malformedReceived += 1;
        message.kind = MessageKind::Invalid;
        return Result::Malformed;
    }

    SanitiseMessage(message);
    endpoint.messagesReceived += 1;

    return Result::Ok;
}

void ControlChannel::Prepare(Message& message, MessageKind kind)
{
    memset(&message, 0, sizeof(Message));
    message.kind = kind;
}

bool ControlChannel::CopyText(char* destination, uint32_t capacity, const char* source)
{
    if (!destination || capacity == 0) return false;

    destination[0] = '\0';
    if (!source) return true;

    size_t length = strnlen(source, capacity);
    if (length >= capacity) length = capacity - 1;

    memcpy(destination, source, length);
    destination[length] = '\0';

    return source[length] == '\0';
}

bool ControlChannel::PeerHasGone(Endpoint& endpoint)
{
    if (endpoint.fileDescriptor == INVALID_FILE_DESCRIPTOR) return true;
    if (endpoint.peerClosed) return true;

    pollfd watched{};
    watched.fd = endpoint.fileDescriptor;
    watched.events = 0;

    for (;;)
    {
        const int ready = poll(&watched, 1, 0);

        if (ready < 0 && errno == EINTR) continue;
        if (ready < 0) return false;

        break;
    }

    if ((watched.revents & (POLLHUP | POLLERR | POLLNVAL)) == 0) return false;

    endpoint.peerClosed = true;
    return true;
}

bool ControlChannel::IsConfigured()
{
    const char* value = Settings::Lookup(DESCRIPTOR_SETTING);
    return value && value[0] != '\0';
}

bool ControlChannel::PublishDescriptor(int fileDescriptor)
{
    if (fileDescriptor < 0) return false;

    Settings::Set(DESCRIPTOR_SETTING, std::to_string(fileDescriptor));

    return true;
}

const char* ControlChannel::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotOpen: return "NotOpen";
        case Result::AlreadyOpen: return "AlreadyOpen";
        case Result::InvalidArgument: return "InvalidArgument";
        case Result::NotConfigured: return "NotConfigured";
        case Result::PeerClosed: return "PeerClosed";
        case Result::WouldBlock: return "WouldBlock";
        case Result::SendFailed: return "SendFailed";
        case Result::ReceiveFailed: return "ReceiveFailed";
        case Result::Malformed: return "Malformed";
        case Result::VersionMismatch: return "VersionMismatch";
    }

    return "Unknown";
}

const char* ControlChannel::MessageKindName(MessageKind kind)
{
    switch (kind)
    {
        case MessageKind::Invalid: return "Invalid";
        case MessageKind::Hello: return "Hello";
        case MessageKind::Buffers: return "Buffers";
        case MessageKind::FrameReady: return "FrameReady";
        case MessageKind::BufferReleased: return "BufferReleased";
        case MessageKind::PresentDone: return "PresentDone";
        case MessageKind::Input: return "Input";
        case MessageKind::Stop: return "Stop";
        case MessageKind::Progress: return "Progress";
        case MessageKind::Statistics: return "Statistics";
        case MessageKind::Log: return "Log";
        case MessageKind::GuestPresents: return "GuestPresents";
        case MessageKind::LaunchPackage: return "LaunchPackage";
        case MessageKind::Exit: return "Exit";
    }

    return "Unknown";
}

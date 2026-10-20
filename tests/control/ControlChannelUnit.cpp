#include <cstdio>
#include <cstring>

#include <fcntl.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <unistd.h>

#include "ControlChannel/ControlChannel.hpp"

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static int MakeSealedBuffer(const char* contents)
{
    int descriptor = memfd_create("acl-control-unit", MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (descriptor < 0) return -1;

    size_t length = strlen(contents);

    if (ftruncate(descriptor, (off_t)length) != 0 || write(descriptor, contents, length) != (ssize_t)length)
    {
        close(descriptor);
        return -1;
    }

    return descriptor;
}

static void CheckPairAndRoundTrip()
{
    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;

    Check(ControlChannel::CreatePair(shell, playerDescriptor) == ControlChannel::Result::Ok, "a pair is created");

    int flags = fcntl(playerDescriptor, F_GETFD);
    Check(flags >= 0 && (flags & FD_CLOEXEC) == 0, "the inherited end survives an exec");
    Check((fcntl(shell.fileDescriptor, F_GETFD) & FD_CLOEXEC) != 0, "the owning end does not");

    ControlChannel::Endpoint player{};
    Check(ControlChannel::Adopt(playerDescriptor, player) == ControlChannel::Result::Ok, "the player adopts its end");

    ControlChannel::Message sent{};
    ControlChannel::Prepare(sent, ControlChannel::MessageKind::Hello);
    sent.payload.hello.version = ControlChannel::PROTOCOL_VERSION;
    sent.payload.hello.width = 1080;
    sent.payload.hello.height = 2400;
    ControlChannel::CopyText(sent.payload.hello.packageName, ControlChannel::MAX_NAME_BYTES, "com.example.graphics");

    Check(ControlChannel::Send(player, sent) == ControlChannel::Result::Ok, "the player sends hello");

    ControlChannel::Message received{};
    Check(ControlChannel::Receive(shell, received, 1000) == ControlChannel::Result::Ok, "the shell receives it");
    Check(received.kind == ControlChannel::MessageKind::Hello, "the kind survives");
    Check(received.payload.hello.width == 1080 && received.payload.hello.height == 2400, "the geometry survives");
    Check(strcmp(received.payload.hello.packageName, "com.example.graphics") == 0, "the package name survives");
    Check(shell.messagesReceived == 1 && player.messagesSent == 1, "both ends count the message");

    ControlChannel::Close(shell);
    ControlChannel::Close(player);
}

static void CheckDescriptorPassing()
{
    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;
    ControlChannel::CreatePair(shell, playerDescriptor);

    ControlChannel::Endpoint player{};
    ControlChannel::Adopt(playerDescriptor, player);

    int buffers[2] = {MakeSealedBuffer("first"), MakeSealedBuffer("second")};
    Check(buffers[0] >= 0 && buffers[1] >= 0, "two shared buffers exist");

    ControlChannel::Message sent{};
    ControlChannel::Prepare(sent, ControlChannel::MessageKind::Buffers);
    sent.payload.buffers.count = 2;
    sent.payload.buffers.width = 4;
    sent.payload.buffers.height = 1;
    sent.payload.buffers.stride = 16;

    Check(ControlChannel::SendWithDescriptors(shell, sent, buffers, 2) == ControlChannel::Result::Ok, "the shell sends two descriptors");

    close(buffers[0]);
    close(buffers[1]);

    ControlChannel::Message received{};
    int carried[ControlChannel::MAX_DESCRIPTORS];
    uint32_t carriedCount = ControlChannel::MAX_DESCRIPTORS;

    Check(ControlChannel::ReceiveWithDescriptors(player, received, carried, carriedCount, 1000) == ControlChannel::Result::Ok,
          "the player receives them");
    Check(carriedCount == 2, "both descriptors arrive");
    Check(received.payload.buffers.stride == 16, "the payload arrives with them");

    char text[8];
    memset(text, 0, sizeof(text));
    Check(pread(carried[1], text, 6, 0) == 6 && strcmp(text, "second") == 0, "the second descriptor reads the right bytes");

    close(carried[0]);
    close(carried[1]);
    ControlChannel::Close(shell);
    ControlChannel::Close(player);
}

static void CheckDescriptorOverflow()
{
    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;
    ControlChannel::CreatePair(shell, playerDescriptor);

    ControlChannel::Endpoint player{};
    ControlChannel::Adopt(playerDescriptor, player);

    int buffers[3] = {MakeSealedBuffer("a"), MakeSealedBuffer("b"), MakeSealedBuffer("c")};

    ControlChannel::Message sent{};
    ControlChannel::Prepare(sent, ControlChannel::MessageKind::Buffers);
    sent.payload.buffers.count = 3;
    ControlChannel::SendWithDescriptors(shell, sent, buffers, 3);

    close(buffers[0]);
    close(buffers[1]);
    close(buffers[2]);

    ControlChannel::Message received{};
    int carried[1];
    uint32_t carriedCount = 1;

    ControlChannel::ReceiveWithDescriptors(player, received, carried, carriedCount, 1000);
    Check(carriedCount == 1, "a receiver with room for one keeps one");

    int probe = MakeSealedBuffer("probe");
    Check(probe >= 0 && probe < 64, "the descriptors it could not keep were closed rather than leaked");

    close(probe);
    close(carried[0]);
    ControlChannel::Close(shell);
    ControlChannel::Close(player);
}

static void CheckMalformed()
{
    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;
    ControlChannel::CreatePair(shell, playerDescriptor);

    ControlChannel::Endpoint player{};
    ControlChannel::Adopt(playerDescriptor, player);

    ControlChannel::Message crafted{};
    ControlChannel::Prepare(crafted, ControlChannel::MessageKind::Hello);
    crafted.kind = (ControlChannel::MessageKind)9999;

    Check(send(shell.fileDescriptor, &crafted, sizeof(crafted), MSG_NOSIGNAL) == (ssize_t)sizeof(crafted), "an unknown kind is written raw");

    ControlChannel::Message received{};
    Check(ControlChannel::Receive(player, received, 1000) == ControlChannel::Result::Malformed, "an unknown kind is refused");
    Check(player.malformedReceived == 1, "and counted");

    char shortMessage[8];
    memset(shortMessage, 0, sizeof(shortMessage));
    send(shell.fileDescriptor, shortMessage, sizeof(shortMessage), MSG_NOSIGNAL);
    Check(ControlChannel::Receive(player, received, 1000) == ControlChannel::Result::Malformed, "a truncated datagram is refused");

    ControlChannel::Message unterminated{};
    ControlChannel::Prepare(unterminated, ControlChannel::MessageKind::Progress);
    memset(unterminated.payload.progress.libraryName, 'x', ControlChannel::MAX_NAME_BYTES);
    send(shell.fileDescriptor, &unterminated, sizeof(unterminated), MSG_NOSIGNAL);

    Check(ControlChannel::Receive(player, received, 1000) == ControlChannel::Result::Ok, "an unterminated name is still accepted");
    Check(strlen(received.payload.progress.libraryName) == ControlChannel::MAX_NAME_BYTES - 1, "and is terminated for the reader");

    ControlChannel::Close(shell);
    ControlChannel::Close(player);
}

static void CheckTimeoutAndClose()
{
    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;
    ControlChannel::CreatePair(shell, playerDescriptor);

    ControlChannel::Endpoint player{};
    ControlChannel::Adopt(playerDescriptor, player);

    ControlChannel::Message received{};
    Check(ControlChannel::Receive(player, received, 0) == ControlChannel::Result::WouldBlock, "an empty channel does not block");
    Check(!ControlChannel::PeerHasGone(player), "a live peer is not reported as gone");

    ControlChannel::Message pending{};
    ControlChannel::Prepare(pending, ControlChannel::MessageKind::Stop);
    ControlChannel::Send(shell, pending);
    Check(!ControlChannel::PeerHasGone(player), "and a waiting message is not mistaken for one");
    ControlChannel::Receive(player, received, 100);

    ControlChannel::Close(shell);

    Check(ControlChannel::PeerHasGone(player), "a closed peer is noticed without reading anything");

    Check(ControlChannel::Receive(player, received, 1000) == ControlChannel::Result::PeerClosed, "closing one end reports the other");
    Check(player.peerClosed, "and the endpoint remembers it");

    ControlChannel::Message stop{};
    ControlChannel::Prepare(stop, ControlChannel::MessageKind::Stop);
    ControlChannel::Result sent = ControlChannel::Send(player, stop);
    Check(sent == ControlChannel::Result::PeerClosed || sent == ControlChannel::Result::SendFailed, "sending to a closed peer fails loudly");

    ControlChannel::Close(player);
}

static void CheckText()
{
    char destination[8];

    Check(ControlChannel::CopyText(destination, sizeof(destination), "short") && strcmp(destination, "short") == 0, "text that fits is copied");
    Check(!ControlChannel::CopyText(destination, sizeof(destination), "far too long to fit"), "text that does not fit says so");
    Check(strlen(destination) == sizeof(destination) - 1, "and is truncated with a terminator");
    Check(ControlChannel::CopyText(destination, sizeof(destination), nullptr) && destination[0] == '\0', "a null source is an empty string");
}

int main()
{
    CheckPairAndRoundTrip();
    CheckDescriptorPassing();
    CheckDescriptorOverflow();
    CheckMalformed();
    CheckTimeoutAndClose();
    CheckText();

    printf("%s: %d failure(s)\n", failures == 0 ? "control channel" : "control channel FAILED", failures);

    return failures == 0 ? 0 : 1;
}

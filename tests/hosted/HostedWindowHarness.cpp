#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>

#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

#include "ControlChannel/ControlChannel.hpp"

static constexpr int POLL_SLICE_MILLIS = 50;
static constexpr int DEADLINE_SECONDS = 120;
static constexpr int STOP_TIMEOUT_SECONDS = 20;

struct SharedBuffer
{
    int fileDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;
    uint8_t* pixels = nullptr;
    size_t sizeBytes = 0;
};

static uint64_t MonotonicNanos()
{
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static bool AllocateBuffers(SharedBuffer* buffers, uint32_t count, uint32_t stride, uint32_t height)
{
    size_t sizeBytes = (size_t)stride * height;

    for (uint32_t index = 0; index < count; ++index)
    {
        int descriptor = memfd_create("acl-hosted-frame", MFD_CLOEXEC | MFD_ALLOW_SEALING);
        if (descriptor < 0) return false;

        if (ftruncate(descriptor, (off_t)sizeBytes) != 0)
        {
            close(descriptor);
            return false;
        }

        void* mapped = mmap(nullptr, sizeBytes, PROT_READ | PROT_WRITE, MAP_SHARED, descriptor, 0);
        if (mapped == MAP_FAILED)
        {
            close(descriptor);
            return false;
        }

        buffers[index].fileDescriptor = descriptor;
        buffers[index].pixels = (uint8_t*)mapped;
        buffers[index].sizeBytes = sizeBytes;
    }

    return true;
}

static bool WriteRaw(const char* path, const uint8_t* pixels, size_t sizeBytes)
{
    FILE* out = fopen(path, "wb");
    if (!out) return false;

    size_t written = fwrite(pixels, 1, sizeBytes, out);
    fclose(out);

    return written == sizeBytes;
}

int main(int argc, char** argv)
{
    if (argc < 6)
    {
        fprintf(stderr, "usage: HostedWindowHarness <acl> <apk> <output.raw> <width> <height> [stop-after-frames] [player flags...]\n");
        return 2;
    }

    const char* playerPath = argv[1];
    const char* apkPath = argv[2];
    const char* outputPath = argv[3];
    uint32_t width = (uint32_t)strtoul(argv[4], nullptr, 10);
    uint32_t height = (uint32_t)strtoul(argv[5], nullptr, 10);
    uint32_t stride = width * 4;
    uint32_t stopAfterFrames = argc > 6 ? (uint32_t)strtoul(argv[6], nullptr, 10) : 0;

    ControlChannel::Endpoint shell{};
    int playerDescriptor = ControlChannel::INVALID_FILE_DESCRIPTOR;

    if (ControlChannel::CreatePair(shell, playerDescriptor) != ControlChannel::Result::Ok)
    {
        fprintf(stderr, "harness: cannot create the control channel\n");
        return 1;
    }

    char descriptorSetting[48];
    snprintf(descriptorSetting, sizeof(descriptorSetting), "control-fd=%d", playerDescriptor);

    char windowSetting[48];
    snprintf(windowSetting, sizeof(windowSetting), "%ux%u", width, height);

    std::vector<char*> arguments;
    arguments.push_back((char*)playerPath);
    arguments.push_back((char*)"--setting");
    arguments.push_back(descriptorSetting);
    arguments.push_back((char*)"--window");
    arguments.push_back(windowSetting);

    for (int extra = 7; extra < argc; ++extra) arguments.push_back(argv[extra]);

    arguments.push_back((char*)apkPath);
    arguments.push_back(nullptr);

    pid_t child = fork();

    if (child < 0)
    {
        fprintf(stderr, "harness: cannot fork the player\n");
        return 1;
    }

    if (child == 0)
    {
        close(shell.fileDescriptor);
        execv(playerPath, arguments.data());
        _exit(127);
    }

    close(playerDescriptor);

    SharedBuffer buffers[ControlChannel::FRAME_BUFFER_COUNT];
    bool buffersReady = false;
    bool helloSeen = false;
    bool guestPresents = false;
    uint32_t framesReceived = 0;
    uint32_t tapsSent = 0;
    uint32_t progressSeen = 0;
    uint64_t deadline = MonotonicNanos() + (uint64_t)DEADLINE_SECONDS * 1000000000ull;

    for (;;)
    {
        if (MonotonicNanos() > deadline)
        {
            fprintf(stderr, "harness: the player did not finish within %d seconds\n", DEADLINE_SECONDS);
            kill(child, SIGKILL);
            break;
        }

        ControlChannel::Message message{};
        ControlChannel::Result received = ControlChannel::Receive(shell, message, POLL_SLICE_MILLIS);

        if (received == ControlChannel::Result::PeerClosed) break;

        if (received == ControlChannel::Result::WouldBlock)
        {
            int state = 0;
            if (waitpid(child, &state, WNOHANG) == child)
            {
                child = -1;
                break;
            }
            continue;
        }

        if (received != ControlChannel::Result::Ok) continue;

        if (message.kind == ControlChannel::MessageKind::Hello)
        {
            helloSeen = true;
            printf("harness: hello from %s, version %u, %ux%u\n", message.payload.hello.packageName, message.payload.hello.version,
                   message.payload.hello.width, message.payload.hello.height);

            if (message.payload.hello.version != ControlChannel::PROTOCOL_VERSION)
            {
                fprintf(stderr, "harness: protocol version mismatch\n");
                break;
            }

            if (!AllocateBuffers(buffers, ControlChannel::FRAME_BUFFER_COUNT, stride, height))
            {
                fprintf(stderr, "harness: cannot allocate shared frame buffers\n");
                break;
            }

            int descriptors[ControlChannel::FRAME_BUFFER_COUNT];
            for (uint32_t index = 0; index < ControlChannel::FRAME_BUFFER_COUNT; ++index) descriptors[index] = buffers[index].fileDescriptor;

            ControlChannel::Message offer{};
            ControlChannel::Prepare(offer, ControlChannel::MessageKind::Buffers);
            offer.payload.buffers.count = ControlChannel::FRAME_BUFFER_COUNT;
            offer.payload.buffers.width = width;
            offer.payload.buffers.height = height;
            offer.payload.buffers.stride = stride;

            ControlChannel::SendWithDescriptors(shell, offer, descriptors, ControlChannel::FRAME_BUFFER_COUNT);
            buffersReady = true;
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::FrameReady)
        {
            uint32_t index = message.payload.frameReady.index;
            if (!buffersReady || index >= ControlChannel::FRAME_BUFFER_COUNT) continue;

            framesReceived += 1;
            WriteRaw(outputPath, buffers[index].pixels, buffers[index].sizeBytes);

            ControlChannel::Message release{};
            ControlChannel::Prepare(release, ControlChannel::MessageKind::BufferReleased);
            release.payload.bufferReleased.index = index;
            ControlChannel::Send(shell, release);

            ControlChannel::Message present{};
            ControlChannel::Prepare(present, ControlChannel::MessageKind::PresentDone);
            present.payload.presentDone.frameTimeNanos = MonotonicNanos();
            ControlChannel::Send(shell, present);

            if (framesReceived == 1 && tapsSent == 0)
            {
                ControlChannel::Message down{};
                ControlChannel::Prepare(down, ControlChannel::MessageKind::Input);
                down.payload.input.kind = ControlChannel::InputKind::Touch;
                down.payload.input.action = 0;
                down.payload.input.x = (float)width * 0.5f;
                down.payload.input.y = (float)height * 0.5f;
                down.payload.input.timeNanos = MonotonicNanos();
                ControlChannel::Send(shell, down);

                ControlChannel::Message up = down;
                up.payload.input.action = 1;
                up.payload.input.timeNanos = MonotonicNanos();
                ControlChannel::Send(shell, up);

                tapsSent = 1;
            }

            if (stopAfterFrames > 0 && framesReceived >= stopAfterFrames) break;

            continue;
        }

        if (message.kind == ControlChannel::MessageKind::Progress)
        {
            progressSeen += 1;
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::GuestPresents)
        {
            guestPresents = true;
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::Exit) break;
    }

    uint64_t closedAt = MonotonicNanos();
    ControlChannel::Close(shell);

    int state = 0;
    int exitStatus = -1;
    bool stoppedOnItsOwn = true;
    double stopSeconds = 0.0;

    if (child > 0)
    {
        uint64_t stopDeadline = closedAt + (uint64_t)STOP_TIMEOUT_SECONDS * 1000000000ull;

        for (;;)
        {
            pid_t reaped = waitpid(child, &state, WNOHANG);

            if (reaped == child) break;

            if (reaped < 0)
            {
                child = -1;
                break;
            }

            if (MonotonicNanos() > stopDeadline)
            {
                stoppedOnItsOwn = false;
                fprintf(stderr, "harness: the player ignored the closed channel for %d seconds\n", STOP_TIMEOUT_SECONDS);
                kill(child, SIGKILL);
                waitpid(child, &state, 0);
                break;
            }

            usleep(1000);
        }

        stopSeconds = (double)(MonotonicNanos() - closedAt) / 1e9;
        if (child > 0) exitStatus = WIFEXITED(state) ? WEXITSTATUS(state) : 128 + WTERMSIG(state);
    }

    for (uint32_t index = 0; index < ControlChannel::FRAME_BUFFER_COUNT; ++index)
    {
        if (buffers[index].pixels) munmap(buffers[index].pixels, buffers[index].sizeBytes);
        if (buffers[index].fileDescriptor >= 0) close(buffers[index].fileDescriptor);
    }

    printf("harness: hello=%d frames=%u taps=%u progress=%u guestPresents=%d exit=%d\n", helloSeen ? 1 : 0, framesReceived, tapsSent,
           progressSeen, guestPresents ? 1 : 0, exitStatus);
    printf("harness: the player stopped %.2fs after the channel closed, on its own=%d\n", stopSeconds, stoppedOnItsOwn ? 1 : 0);

    if (!helloSeen)
    {
        fprintf(stderr, "harness: the player never introduced itself\n");
        return 1;
    }

    if (framesReceived == 0)
    {
        fprintf(stderr, "harness: the player never handed over a frame\n");
        return 1;
    }

    if (!stoppedOnItsOwn) return 1;

    return exitStatus == 0 ? 0 : 1;
}

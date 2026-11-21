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
    uint32_t generation = 0;
    uint32_t index = 0;
    uint8_t* pixels = nullptr;
    size_t sizeBytes = 0;
};

static uint64_t MonotonicNanos()
{
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static void AdoptBuffer(std::vector<SharedBuffer>& buffers, const ControlChannel::RegisterBufferPayload& payload, int descriptor)
{
    if (payload.kind != ControlChannel::BufferKind::Shared || payload.planeCount != 1) return;

    size_t sizeBytes = (size_t)payload.strides[0] * payload.height;
    void* mapped = mmap(nullptr, sizeBytes, PROT_READ, MAP_SHARED, descriptor, 0);
    if (mapped == MAP_FAILED) return;

    SharedBuffer buffer{};
    buffer.generation = payload.generation;
    buffer.index = payload.index;
    buffer.pixels = (uint8_t*)mapped;
    buffer.sizeBytes = sizeBytes;
    buffers.push_back(buffer);
}

static void ForgetGeneration(std::vector<SharedBuffer>& buffers, uint32_t generation)
{
    for (size_t position = buffers.size(); position > 0; --position)
    {
        if (buffers[position - 1].generation != generation) continue;

        munmap(buffers[position - 1].pixels, buffers[position - 1].sizeBytes);
        buffers.erase(buffers.begin() + (long)(position - 1));
    }
}

static const SharedBuffer* FindBuffer(const std::vector<SharedBuffer>& buffers, uint32_t generation, uint32_t index)
{
    for (const SharedBuffer& buffer : buffers)
    {
        if (buffer.generation == generation && buffer.index == index) return &buffer;
    }

    return nullptr;
}

static void SendPresentDone(ControlChannel::Endpoint& shell)
{
    ControlChannel::Message present{};
    ControlChannel::Prepare(present, ControlChannel::MessageKind::PresentDone);
    present.payload.presentDone.frameTimeNanos = MonotonicNanos();
    ControlChannel::Send(shell, present);
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

    std::vector<SharedBuffer> buffers;
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
        int descriptors[ControlChannel::MAX_DESCRIPTORS];
        uint32_t descriptorCount = ControlChannel::MAX_DESCRIPTORS;
        ControlChannel::Result received = ControlChannel::ReceiveWithDescriptors(shell, message, descriptors, descriptorCount, POLL_SLICE_MILLIS);

        if (received == ControlChannel::Result::Ok && message.kind == ControlChannel::MessageKind::RegisterBuffer && descriptorCount > 0)
        {
            AdoptBuffer(buffers, message.payload.registerBuffer, descriptors[0]);
        }

        for (uint32_t index = 0; index < descriptorCount; ++index) close(descriptors[index]);

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
            printf("harness: hello from %s titled %s, version %u, %ux%u\n", message.payload.hello.packageName, message.payload.hello.title,
                   message.payload.hello.version, message.payload.hello.width, message.payload.hello.height);

            if (message.payload.hello.version != ControlChannel::PROTOCOL_VERSION)
            {
                fprintf(stderr, "harness: protocol version mismatch\n");
                break;
            }

            ControlChannel::Message display{};
            ControlChannel::Prepare(display, ControlChannel::MessageKind::Display);
            display.payload.display.width = width;
            display.payload.display.height = height;
            display.payload.display.refreshMilliHertz = 60000;
            display.payload.display.scale = 1;
            ControlChannel::Send(shell, display);
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::ForgetBuffers)
        {
            ForgetGeneration(buffers, message.payload.forgetBuffers.generation);
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::RequestFrame)
        {
            SendPresentDone(shell);
            continue;
        }

        if (message.kind == ControlChannel::MessageKind::FrameReady)
        {
            const SharedBuffer* buffer = FindBuffer(buffers, message.payload.frameReady.generation, message.payload.frameReady.index);
            if (!buffer) continue;

            framesReceived += 1;
            WriteRaw(outputPath, buffer->pixels, buffer->sizeBytes);

            ControlChannel::Message release{};
            ControlChannel::Prepare(release, ControlChannel::MessageKind::BufferReleased);
            release.payload.bufferReleased.generation = buffer->generation;
            release.payload.bufferReleased.index = buffer->index;
            ControlChannel::Send(shell, release);

            SendPresentDone(shell);

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

    for (const SharedBuffer& buffer : buffers) munmap(buffer.pixels, buffer.sizeBytes);

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

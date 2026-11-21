#pragma once

#include <condition_variable>
#include <cstddef>
#include <cstdint>
#include <mutex>
#include <thread>
#include <vector>

#include "ControlChannel/ControlChannel.hpp"
#include "WaylandClient/WaylandClient.hpp"

namespace HostedWindow
{
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;
    inline constexpr uint32_t INVALID_BUFFER_INDEX = 0xFFFFFFFFu;
    inline constexpr int HANDSHAKE_TIMEOUT_MILLIS = 15000;
    inline constexpr int MAX_QUEUED_INPUT = 256;
    inline constexpr const char* NO_DMABUF_SETTING = "window-host-dmabuf";

    enum class Result : uint8_t
    {
        Ok,
        AlreadyOpen,
        NotOpen,
        NoChannel,
        HandshakeFailed,
        MappingFailed,
        NoFreeBuffer,
        PeerClosed,
        InvalidArgument,
        TimedOut
    };

    struct Buffer
    {
        uint8_t* pixels = nullptr;
        size_t sizeBytes = 0;
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        bool heldByShell = false;
        bool acquired = false;
    };

    struct Display
    {
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t refreshMilliHertz = 0;
        uint32_t scale = 1;
        int32_t physicalWidthMm = 0;
        int32_t physicalHeightMm = 0;
        uint64_t renderDevice = 0;
        bool dmabufAvailable = false;
        bool described = false;
        std::vector<ControlChannel::FormatEntry> formats;
    };

    struct BufferDescription
    {
        uint32_t generation = 0;
        uint32_t index = 0;
        ControlChannel::BufferKind kind = ControlChannel::BufferKind::Shared;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t format = ControlChannel::FORMAT_ARGB8888;
        uint64_t modifier = ControlChannel::MODIFIER_LINEAR;
        uint32_t planeCount = 0;
        uint32_t offsets[ControlChannel::MAX_PLANES] = {};
        uint32_t strides[ControlChannel::MAX_PLANES] = {};
    };

    using ReleaseListener = void (*)(void* context, uint32_t generation, uint32_t index, bool refused);

    struct Session
    {
        ControlChannel::Endpoint channel;
        std::mutex mutex;
        std::condition_variable changed;
        std::thread receiver;
        int signalFileDescriptor = INVALID_FILE_DESCRIPTOR;
        Buffer buffers[ControlChannel::FRAME_BUFFER_COUNT];
        uint32_t bufferCount = 0;
        uint32_t generation = 0;
        uint32_t nextGeneration = 1;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        uint64_t sequence = 0;
        uint64_t lastFrameTimeNanos = 0;
        uint64_t presentsCompleted = 0;
        uint32_t configuredWidth = 0;
        uint32_t configuredHeight = 0;
        bool configurePending = false;
        Display display;
        ReleaseListener releaseListener = nullptr;
        void* releaseContext = nullptr;
        WaylandClient::InputEvent queued[MAX_QUEUED_INPUT];
        int queueHead = 0;
        int queueCount = 0;
        uint64_t queueDropped = 0;
        bool frameArrived = false;
        bool stopRequested = false;
        bool open = false;
    };

    bool IsRequested();

    Result Open(uint32_t width, uint32_t height, const char* title, const char* packageName, Session& session);
    void Close(Session& session);

    Result AcquireBuffer(Session& session, Buffer*& buffer, uint32_t& index);
    Result Present(Session& session, uint32_t index, uint64_t frameTimeNanos);
    Result RequestFrame(Session& session);
    Result Resize(Session& session, uint32_t width, uint32_t height);

    bool ConsumeConfigure(Session& session, uint32_t& width, uint32_t& height);
    bool ConsumeFrame(Session& session);
    bool IsStopRequested(Session& session);
    bool NextInputEvent(Session& session, WaylandClient::InputEvent& event);
    void DrainSignal(Session& session);
    Display CurrentDisplay(Session& session);
    void CurrentSize(Session& session, uint32_t& width, uint32_t& height);

    uint32_t ReserveGeneration(Session& session);
    void SetReleaseListener(Session& session, ReleaseListener listener, void* context);
    Result RegisterBuffer(Session& session, const BufferDescription& description, const int* descriptors, uint32_t descriptorCount);
    Result ForgetBuffers(Session& session, uint32_t generation);
    Result PresentBuffer(Session& session, uint32_t generation, uint32_t index, int fenceDescriptor);
    uint64_t PresentsCompleted(Session& session);
    bool WaitForPresentAfter(Session& session, uint64_t completed, int timeoutMillis);

    Result SetFullscreen(Session& session, bool fullscreen);
    Result SetTitle(Session& session, const char* title);
    void ReportGuestPresents(Session& session);
    bool RequestLaunch(Session& session, const char* packageName);
    void SendExit(Session& session, int32_t status);

    bool DmabufWanted();

    const char* ResultName(Result result);
}

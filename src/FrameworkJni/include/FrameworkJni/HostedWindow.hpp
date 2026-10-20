#pragma once

#include <cstddef>
#include <cstdint>

#include "ControlChannel/ControlChannel.hpp"
#include "WaylandClient/WaylandClient.hpp"

namespace HostedWindow
{
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;
    inline constexpr uint32_t INVALID_BUFFER_INDEX = 0xFFFFFFFFu;
    inline constexpr int HANDSHAKE_TIMEOUT_MILLIS = 5000;
    inline constexpr int MAX_QUEUED_INPUT = 256;

    enum class Result : uint8_t
    {
        Ok,
        AlreadyOpen,
        NotOpen,
        NoChannel,
        HandshakeFailed,
        MappingFailed,
        NoFreeBuffer,
        PeerClosed
    };

    struct Buffer
    {
        uint8_t* pixels = nullptr;
        size_t sizeBytes = 0;
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        bool heldByShell = false;
    };

    struct Session
    {
        ControlChannel::Endpoint channel;
        Buffer buffers[ControlChannel::MAX_FRAME_BUFFERS];
        uint32_t bufferCount = 0;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        uint32_t currentIndex = INVALID_BUFFER_INDEX;
        uint64_t sequence = 0;
        uint64_t lastFrameTimeNanos = 0;
        WaylandClient::InputEvent queued[MAX_QUEUED_INPUT];
        int queueHead = 0;
        int queueCount = 0;
        uint64_t queueDropped = 0;
        bool framePending = false;
        bool frameArrived = false;
        bool stopRequested = false;
        bool open = false;
    };

    bool IsRequested();

    Result Open(uint32_t width, uint32_t height, const char* packageName, Session& session);
    void Close(Session& session);

    Result AcquireBuffer(Session& session, Buffer*& buffer);
    Result Present(Session& session, uint64_t frameTimeNanos);
    Result RequestFrame(Session& session);

    Result Pump(Session& session, int timeoutMillis);
    bool NextInputEvent(Session& session, WaylandClient::InputEvent& event);

    void ReportGuestPresents(Session& session);
    bool RequestLaunch(Session& session, const char* packageName);
    void SendExit(Session& session, int32_t status);

    const char* ResultName(Result result);
}

#pragma once

#include <cstdint>

namespace ControlChannel
{
    inline constexpr const char* DESCRIPTOR_SETTING = "control-fd";
    inline constexpr uint32_t PROTOCOL_VERSION = 1;
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;
    inline constexpr uint32_t FRAME_BUFFER_COUNT = 3;
    inline constexpr uint32_t MAX_FRAME_BUFFERS = 8;
    inline constexpr uint32_t MAX_DESCRIPTORS = MAX_FRAME_BUFFERS;
    inline constexpr uint32_t MAX_NAME_BYTES = 256;
    inline constexpr uint32_t MAX_TEXT_BYTES = 512;
    inline constexpr uint32_t MAX_TAG_BYTES = 64;
    inline constexpr uint32_t PHASE_COUNT = 4;

    enum class Result : uint8_t
    {
        Ok,
        NotOpen,
        AlreadyOpen,
        InvalidArgument,
        NotConfigured,
        PeerClosed,
        WouldBlock,
        SendFailed,
        ReceiveFailed,
        Malformed,
        VersionMismatch
    };

    enum class MessageKind : uint16_t
    {
        Invalid = 0,
        Hello,
        Buffers,
        FrameReady,
        BufferReleased,
        PresentDone,
        Input,
        Stop,
        Progress,
        Statistics,
        Log,
        GuestPresents,
        LaunchPackage,
        Exit
    };

    enum class InputKind : uint8_t
    {
        Touch,
        Key
    };

    struct HelloPayload
    {
        uint32_t version;
        uint32_t width;
        uint32_t height;
        char packageName[MAX_NAME_BYTES];
    };

    struct BuffersPayload
    {
        uint32_t count;
        uint32_t width;
        uint32_t height;
        uint32_t stride;
    };

    struct FrameReadyPayload
    {
        uint32_t index;
        uint64_t sequence;
        uint64_t frameTimeNanos;
    };

    struct BufferReleasedPayload
    {
        uint32_t index;
    };

    struct PresentDonePayload
    {
        uint64_t frameTimeNanos;
    };

    struct InputPayload
    {
        InputKind kind;
        int32_t action;
        int32_t pointerId;
        int32_t keyCode;
        uint32_t unicode;
        int32_t metaState;
        float x;
        float y;
        uint64_t timeNanos;
    };

    struct ProgressPayload
    {
        char libraryName[MAX_NAME_BYTES];
        uint64_t functionsDiscovered;
        uint64_t functionsTranslated;
        uint64_t blocksTranslated;
        uint64_t sizeBytes;
        uint64_t memoryBytes;
        uint64_t memoryBudgetBytes;
        double elapsedSeconds;
        float phases[PHASE_COUNT];
        uint32_t translatorVersion;
        bool cacheHit;
    };

    struct StatisticsPayload
    {
        uint64_t blocksExecuted;
        uint64_t blocksTranslated;
        uint64_t blocksInterpreted;
        uint64_t trapsHit;
        uint64_t framesPresented;
    };

    struct LogPayload
    {
        uint8_t priority;
        char tag[MAX_TAG_BYTES];
        char text[MAX_TEXT_BYTES];
    };

    inline constexpr uint32_t MAX_PACKAGE_BYTES = 256;

    struct LaunchPackagePayload
    {
        char packageName[MAX_PACKAGE_BYTES];
    };

    struct ExitPayload
    {
        int32_t status;
    };

    union Payload
    {
        HelloPayload hello;
        BuffersPayload buffers;
        FrameReadyPayload frameReady;
        BufferReleasedPayload bufferReleased;
        PresentDonePayload presentDone;
        InputPayload input;
        ProgressPayload progress;
        StatisticsPayload statistics;
        LogPayload log;
        LaunchPackagePayload launchPackage;
        ExitPayload exit;
    };

    struct Message
    {
        MessageKind kind;
        Payload payload;
    };

    struct Endpoint
    {
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        bool peerClosed = false;
        uint64_t messagesSent = 0;
        uint64_t messagesReceived = 0;
        uint64_t malformedReceived = 0;
    };

    Result CreatePair(Endpoint& endpoint, int& peerDescriptor);
    Result Adopt(int fileDescriptor, Endpoint& endpoint);
    Result AdoptFromEnvironment(Endpoint& endpoint);
    void Close(Endpoint& endpoint);

    Result Send(Endpoint& endpoint, const Message& message);
    Result SendWithDescriptors(Endpoint& endpoint, const Message& message, const int* descriptors, uint32_t descriptorCount);

    Result Receive(Endpoint& endpoint, Message& message, int timeoutMillis);
    Result ReceiveWithDescriptors(Endpoint& endpoint, Message& message, int* descriptors, uint32_t& descriptorCount, int timeoutMillis);

    void Prepare(Message& message, MessageKind kind);
    bool CopyText(char* destination, uint32_t capacity, const char* source);

    bool PeerHasGone(Endpoint& endpoint);

    bool IsConfigured();
    bool PublishDescriptor(int fileDescriptor);

    const char* ResultName(Result result);
    const char* MessageKindName(MessageKind kind);
}

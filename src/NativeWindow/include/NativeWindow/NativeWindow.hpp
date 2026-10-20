#pragma once

#include <cstdint>

namespace NativeWindow
{
    inline constexpr uint32_t DEFAULT_BUFFER_COUNT = 3;
    inline constexpr uint32_t MAX_BUFFER_COUNT = 16;
    inline constexpr uint32_t INVALID_BUFFER_INDEX = 0xFFFFFFFFu;
    inline constexpr int INVALID_FENCE_FD = -1;
    inline constexpr int INVALID_DMABUF_FD = -1;

    enum class Result : uint8_t
    {
        Ok,
        NoCompositor,
        AllocationFailed,
        NoFreeBuffer,
        InvalidGeometry,
        InvalidBufferCount,
        InvalidBufferIndex,
        InvalidArgumentCount,
        UnsupportedOperation
    };

    enum class PixelFormat : uint32_t
    {
        ImplementationDefined = 0x22,
        Rgba8888 = 1,
        Rgbx8888 = 2,
        Rgb888 = 3,
        Rgb565 = 4,
        Bgra8888 = 5,
        RgbaFp16 = 22,
        Rgba1010102 = 43
    };

    enum class Operation : uint32_t
    {
        SetBufferCount,
        SetBuffersDimensions,
        SetBuffersFormat,
        SetBuffersTransform,
        SetBuffersTimestamp,
        SetBuffersDataSpace,
        SetScalingMode,
        SetCrop,
        SetUsage,
        SetUsage64,
        SetSwapInterval,
        SetSharedBufferMode,
        SetAutoRefresh,
        SetSurfaceDamage,
        ApiConnect,
        ApiDisconnect
    };

    enum class Attribute : uint32_t
    {
        Width,
        Height,
        Format,
        MinUndequeuedBuffers,
        MaxBufferCount,
        DefaultWidth,
        DefaultHeight,
        TransformHint,
        BufferAge,
        ConsumerUsageBits,
        ConsumerRunningBehind,
        ConcreteType
    };

    struct Buffer
    {
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        PixelFormat format = PixelFormat::Rgba8888;
        uint64_t modifier = 0;
        void* mappedAddress = nullptr;
        int dmabufFd = INVALID_DMABUF_FD;
        int releaseFenceFd = INVALID_FENCE_FD;
        bool inUseByCompositor = false;
    };

    struct ReferenceCount
    {
        uint32_t magic = 0;
        uint32_t version = 0;
        int32_t count = 0;
    };

    struct Window
    {
        ReferenceCount base;
        uint32_t width = 0;
        uint32_t height = 0;
        PixelFormat format = PixelFormat::Rgba8888;
        uint32_t bufferCount = DEFAULT_BUFFER_COUNT;
        Buffer buffers[MAX_BUFFER_COUNT];
        void* waylandSurface = nullptr;
        void* gbmDevice = nullptr;
        int32_t transform = 0;
        int32_t swapInterval = 1;
    };

    Result Create(uint32_t width, uint32_t height, PixelFormat format, Window& window);
    void Destroy(Window& window);

    Result SetBuffersGeometry(Window& window, uint32_t width, uint32_t height, PixelFormat format);
    Result SetBufferCount(Window& window, uint32_t bufferCount);
    Result DequeueBuffer(Window& window, uint32_t& bufferIndex, int& acquireFenceFd);
    Result QueueBuffer(Window& window, uint32_t bufferIndex, int releaseFenceFd);
    Result CancelBuffer(Window& window, uint32_t bufferIndex, int releaseFenceFd);
    Result Query(const Window& window, Attribute attribute, int32_t& value);
    Result Perform(Window& window, Operation operation, const int64_t* arguments, uint32_t argumentCount);

    Result BufferAt(Window& window, uint32_t bufferIndex, Buffer*& buffer);

    void* ToAndroidNativeWindow(Window& window);
    Window* FromAndroidNativeWindow(void* androidNativeWindow);

    const char* ResultName(Result result);
}

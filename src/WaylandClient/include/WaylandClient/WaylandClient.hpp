#pragma once

#include <cstddef>
#include <cstdint>

struct wl_buffer;
struct wl_compositor;
struct wl_display;
struct wl_registry;
struct wl_output;
struct wl_shm;
struct wl_shm_pool;
struct wl_surface;
struct wl_seat;
struct wl_pointer;
struct wl_keyboard;
struct wl_touch;
struct xdg_surface;
struct xdg_toplevel;
struct xdg_wm_base;
struct wp_presentation;
struct zxdg_decoration_manager_v1;
struct zxdg_toplevel_decoration_v1;

namespace WaylandClient
{
    inline constexpr const char* DEFAULT_TITLE = "acl";
    inline constexpr const char* DEFAULT_APPLICATION_ID = "acl";
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;
    inline constexpr int BUFFER_COUNT = 2;
    inline constexpr int BYTES_PER_PIXEL = 4;
    inline constexpr uint32_t MIN_DIMENSION = 1;
    inline constexpr uint32_t MAX_DIMENSION = 16384;
    inline constexpr uint32_t DEFAULT_WIDTH = 1080;
    inline constexpr uint32_t DEFAULT_HEIGHT = 2400;
    inline constexpr int CONNECT_TIMEOUT_MILLIS = 5000;

    enum class Result : uint8_t
    {
        Ok,
        NoDisplay,
        NoCompositor,
        NoShell,
        NoSharedMemory,
        AlreadyConnected,
        NotConnected,
        InvalidGeometry,
        MappingFailed,
        NoFreeBuffer,
        DispatchFailed,
        FlushFailed
    };

    struct Buffer
    {
        wl_buffer* buffer = nullptr;
        uint8_t* pixels = nullptr;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t stride = 0;
        size_t sizeBytes = 0;
        bool heldByCompositor = false;
    };

    inline constexpr int MAX_RETIRED_POOLS = 4;

    struct RetiredPool
    {
        wl_shm_pool* pool = nullptr;
        uint8_t* base = nullptr;
        size_t sizeBytes = 0;
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        Buffer buffers[BUFFER_COUNT];
        bool active = false;
    };

    struct Surface
    {
        wl_surface* surface = nullptr;
        xdg_surface* shellSurface = nullptr;
        xdg_toplevel* toplevel = nullptr;
        zxdg_toplevel_decoration_v1* decoration = nullptr;
        bool decorated = false;
        wl_shm_pool* pool = nullptr;
        int poolFileDescriptor = INVALID_FILE_DESCRIPTOR;
        uint8_t* poolBase = nullptr;
        size_t poolSizeBytes = 0;
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t pendingWidth = 0;
        uint32_t pendingHeight = 0;
        uint32_t boundsWidth = 0;
        uint32_t boundsHeight = 0;
        Buffer buffers[BUFFER_COUNT];
        RetiredPool retired[MAX_RETIRED_POOLS];
        bool configured = false;
        bool closeRequested = false;
        bool frameRequested = false;
        bool fullscreen = false;
        bool resized = false;
    };

    inline constexpr int MAX_QUEUED_INPUT = 256;

    enum class InputKind : uint8_t
    {
        Touch,
        Key
    };

    struct InputEvent
    {
        InputKind kind = InputKind::Touch;
        int32_t action = 0;
        int32_t pointerId = 0;
        int32_t keyCode = 0;
        uint32_t unicode = 0;
        int32_t metaState = 0;
        float x = 0.0f;
        float y = 0.0f;
        uint64_t timeNanos = 0;
    };

    struct Connection
    {
        wl_display* display = nullptr;
        wl_registry* registry = nullptr;
        wl_compositor* compositor = nullptr;
        wl_shm* sharedMemory = nullptr;
        xdg_wm_base* shell = nullptr;
        wp_presentation* presentation = nullptr;
        zxdg_decoration_manager_v1* decorations = nullptr;
        wl_seat* seat = nullptr;
        wl_pointer* pointer = nullptr;
        wl_keyboard* keyboard = nullptr;
        wl_touch* touch = nullptr;
        void* keyboardState = nullptr;
        void* keyboardMap = nullptr;
        void* keyboardContext = nullptr;
        InputEvent queued[MAX_QUEUED_INPUT];
        int queueHead = 0;
        int queueCount = 0;
        uint64_t queueDropped = 0;
        float pointerX = 0.0f;
        float pointerY = 0.0f;
        bool pointerDown = false;
        wl_output* output = nullptr;
        uint32_t outputWidth = 0;
        uint32_t outputHeight = 0;
        uint32_t outputRefreshMilliHertz = 0;
        uint32_t outputScale = 1;
        int32_t outputPhysicalWidthMm = 0;
        int32_t outputPhysicalHeightMm = 0;
        int32_t outputTransform = 0;
        bool outputDescribed = false;
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        uint32_t clockId = 0;
    };

    struct DisplayMode
    {
        uint32_t width = 0;
        uint32_t height = 0;
        uint32_t refreshMilliHertz = 0;
        uint32_t scale = 1;
        int32_t physicalWidthMm = 0;
        int32_t physicalHeightMm = 0;
        bool described = false;
    };

    using FrameCallback = void (*)(void* context, uint64_t frameTimeNanos);

    Result Connect(Connection& connection);
    void Disconnect(Connection& connection);

    Result CreateSurface(Connection& connection, uint32_t width, uint32_t height, const char* title, Surface& surface);
    void DestroySurface(Surface& surface);

    DisplayMode CurrentDisplayMode(const Connection& connection);

    Result Resize(Connection& connection, Surface& surface, uint32_t width, uint32_t height);
    Result SetFullscreen(Surface& surface, bool fullscreen);
    bool ConsumeResize(Surface& surface, uint32_t& width, uint32_t& height);

    Result AcquireBuffer(Surface& surface, Buffer*& buffer);
    Result Present(Connection& connection, Surface& surface, Buffer& buffer);

    Result RequestFrame(Connection& connection, Surface& surface, FrameCallback callback, void* context);

    inline constexpr const char* SOCKET_SETTING = "wayland-socket";
    inline constexpr const char* DECORATION_SETTING = "decorated";
    inline constexpr const char* FULLSCREEN_SETTING = "fullscreen";

    bool NextInputEvent(Connection& connection, InputEvent& event);
    void QueueInputEvent(Connection& connection, const InputEvent& event);

    Result Dispatch(Connection& connection);
    Result DispatchPending(Connection& connection);
    Result Flush(Connection& connection);
    void ReportConnectionError(Connection& connection);
    Result Roundtrip(Connection& connection);

    bool IsAvailable();
    const char* ResultName(Result result);
}

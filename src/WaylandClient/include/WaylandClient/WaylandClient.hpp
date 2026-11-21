#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

struct wl_buffer;
struct wl_compositor;
struct wl_display;
struct wl_registry;
struct wl_output;
struct wl_shm;
struct wl_shm_pool;
struct wl_surface;
struct wl_cursor_theme;
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
struct zwp_linux_dmabuf_v1;
struct wp_viewporter;
struct wp_viewport;
struct libdecor;
struct libdecor_frame;

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
    inline constexpr const char* FRAME_LIBRARY = "libdecor-0.so.0";
    inline constexpr const char* DEFAULT_CURSOR = "default";
    inline constexpr const char* FALLBACK_CURSOR = "left_ptr";
    inline constexpr int DEFAULT_CURSOR_SIZE = 24;
    inline constexpr int MAX_TRACKED_TOUCHES = 32;
    inline constexpr uint32_t MAX_BUFFER_PLANES = 4;
    inline constexpr uint32_t FOURCC_ARGB8888 = 0x34325241;
    inline constexpr uint32_t FOURCC_XRGB8888 = 0x34325258;

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
        wp_viewport* viewport = nullptr;
        libdecor_frame* frame = nullptr;
        bool decorated = false;
        bool maximized = false;
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
        uint32_t floatingWidth = 0;
        uint32_t floatingHeight = 0;
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

    struct DmabufFormat
    {
        uint32_t format = 0;
        uint64_t modifier = 0;
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
        zwp_linux_dmabuf_v1* dmabuf = nullptr;
        wp_viewporter* viewporter = nullptr;
        void* dmabufFeedback = nullptr;
        std::vector<DmabufFormat> dmabufFormats;
        uint64_t dmabufMainDevice = 0;
        bool dmabufDescribed = false;
        libdecor* frames = nullptr;
        wl_surface* contentSurface = nullptr;
        wl_surface* pointerSurface = nullptr;
        wl_cursor_theme* cursorTheme = nullptr;
        wl_surface* cursorSurface = nullptr;
        uint32_t foreignTouches = 0;
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
    using ReleaseCallback = void (*)(void* context);

    Result Connect(Connection& connection);
    void Disconnect(Connection& connection);

    Result CreateSurface(Connection& connection, uint32_t width, uint32_t height, const char* title, Surface& surface);
    Result CreateHostedSurface(Connection& connection, uint32_t width, uint32_t height, const char* title, Surface& surface);
    void DestroySurface(Surface& surface);
    Result SetTitle(Surface& surface, const char* title);

    wl_buffer* ImportSharedBuffer(Connection& connection, int fileDescriptor, uint32_t width, uint32_t height, uint32_t stride,
                                  uint32_t format, ReleaseCallback callback, void* context);
    wl_buffer* ImportDmabuf(Connection& connection, const int* fileDescriptors, const uint32_t* offsets, const uint32_t* strides,
                            uint32_t planeCount, uint32_t width, uint32_t height, uint32_t format, uint64_t modifier,
                            ReleaseCallback callback, void* context);
    void DestroyExternalBuffer(wl_buffer* buffer);
    Result PresentExternal(Connection& connection, Surface& surface, wl_buffer* buffer, uint32_t width, uint32_t height, uint32_t shownWidth,
                           uint32_t shownHeight);

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
    inline constexpr const char* TITLE_SETTING = "window-title";
    inline constexpr const char* FRAMES_SETTING = "window-frames";

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

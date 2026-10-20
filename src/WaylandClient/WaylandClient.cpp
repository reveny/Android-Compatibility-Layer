#include "WaylandClient/WaylandClient.hpp"

#include <cerrno>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <poll.h>
#include <string>
#include <sys/mman.h>
#include <unistd.h>

#include <cstdlib>
#include <ctime>
#include <thread>
#include <wayland-client.h>
#include <xkbcommon/xkbcommon.h>
#include <xkbcommon/xkbcommon-keysyms.h>

#include "presentation-time-client-protocol.h"
#include "xdg-decoration-unstable-v1-client-protocol.h"
#include "xdg-shell-client-protocol.h"

#include <sys/socket.h>
#include <sys/un.h>

#include "Settings/Settings.hpp"

struct SurfaceListener
{
    WaylandClient::Surface* surface = nullptr;
    WaylandClient::FrameCallback callback = nullptr;
    void* context = nullptr;
};

static uint32_t gCompositorVersion = 0;

static constexpr int32_t TOUCH_ACTION_DOWN = 0;
static constexpr int32_t TOUCH_ACTION_UP = 1;
static constexpr int32_t TOUCH_ACTION_MOVE = 2;
static constexpr int32_t TOUCH_ACTION_CANCEL = 3;
static constexpr int32_t TOUCH_ACTION_POINTER_DOWN = 5;
static constexpr int32_t TOUCH_ACTION_POINTER_UP = 6;

static constexpr int32_t KEY_ACTION_DOWN = 0;
static constexpr int32_t KEY_ACTION_UP = 1;

static constexpr uint32_t POINTER_BUTTON_LEFT = 0x110;
static constexpr int32_t POINTER_TOUCH_ID = 0;

static uint64_t InputNanos()
{
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static bool InputTracingRequested()
{
    static const bool requested = Settings::Tracing("input");

    return requested;
}

void WaylandClient::QueueInputEvent(Connection& connection, const InputEvent& event)
{
    if (InputTracingRequested())
    {
        fprintf(stderr, "[waylandclient] queued %s action %d at %.1f,%.1f (pointer %d, key %d)\n",
                event.kind == InputKind::Touch ? "touch" : "key", event.action, event.x, event.y, event.pointerId, event.keyCode);
    }

    if (connection.queueCount == MAX_QUEUED_INPUT)
    {
        connection.queueHead = (connection.queueHead + 1) % MAX_QUEUED_INPUT;
        connection.queueCount -= 1;

        if (connection.queueDropped == 0)
        {
            fprintf(stderr, "[waylandclient] the input queue is full, so the oldest events are being dropped\n");
        }

        connection.queueDropped += 1;
    }

    int slot = (connection.queueHead + connection.queueCount) % MAX_QUEUED_INPUT;
    connection.queued[slot] = event;
    connection.queueCount += 1;
}

bool WaylandClient::NextInputEvent(Connection& connection, InputEvent& event)
{
    if (connection.queueCount == 0) return false;

    event = connection.queued[connection.queueHead];
    connection.queueHead = (connection.queueHead + 1) % MAX_QUEUED_INPUT;
    connection.queueCount -= 1;

    return true;
}

static void QueueTouch(WaylandClient::Connection* connection, int32_t action, int32_t pointerId, float x, float y)
{
    WaylandClient::InputEvent event;
    event.kind = WaylandClient::InputKind::Touch;
    event.action = action;
    event.pointerId = pointerId;
    event.x = x;
    event.y = y;
    event.timeNanos = InputNanos();

    WaylandClient::QueueInputEvent(*connection, event);
}

static void HandlePointerEnter(void* data, wl_pointer*, uint32_t, wl_surface*, wl_fixed_t x, wl_fixed_t y)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    connection->pointerX = (float)wl_fixed_to_double(x);
    connection->pointerY = (float)wl_fixed_to_double(y);

    if (InputTracingRequested()) fprintf(stderr, "[waylandclient] the pointer entered at %.1f,%.1f\n", connection->pointerX, connection->pointerY);
}

static void HandlePointerLeave(void* data, wl_pointer*, uint32_t, wl_surface*)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;

    if (connection->pointerDown)
    {
        connection->pointerDown = false;
        QueueTouch(connection, TOUCH_ACTION_CANCEL, POINTER_TOUCH_ID, connection->pointerX, connection->pointerY);
    }
}

static void HandlePointerMotion(void* data, wl_pointer*, uint32_t, wl_fixed_t x, wl_fixed_t y)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    connection->pointerX = (float)wl_fixed_to_double(x);
    connection->pointerY = (float)wl_fixed_to_double(y);

    if (!connection->pointerDown) return;

    QueueTouch(connection, TOUCH_ACTION_MOVE, POINTER_TOUCH_ID, connection->pointerX, connection->pointerY);
}

static void HandlePointerButton(void* data, wl_pointer*, uint32_t, uint32_t, uint32_t button, uint32_t state)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;

    if (InputTracingRequested()) fprintf(stderr, "[waylandclient] pointer button %u state %u\n", button, state);

    if (button != POINTER_BUTTON_LEFT) return;

    bool pressed = state != 0;
    if (pressed == connection->pointerDown) return;

    connection->pointerDown = pressed;
    QueueTouch(connection, pressed ? TOUCH_ACTION_DOWN : TOUCH_ACTION_UP, POINTER_TOUCH_ID, connection->pointerX, connection->pointerY);
}

static void HandlePointerAxis(void*, wl_pointer*, uint32_t, uint32_t, wl_fixed_t)
{
}

static void HandlePointerFrame(void*, wl_pointer*)
{
}

static void HandlePointerAxisSource(void*, wl_pointer*, uint32_t)
{
}

static void HandlePointerAxisStop(void*, wl_pointer*, uint32_t, uint32_t)
{
}

static void HandlePointerAxisDiscrete(void*, wl_pointer*, uint32_t, int32_t)
{
}

static void HandlePointerAxisValue120(void*, wl_pointer*, uint32_t, int32_t)
{
}

static void HandlePointerAxisRelativeDirection(void*, wl_pointer*, uint32_t, uint32_t)
{
}

static void HandlePointerWarp(void*, wl_pointer*, wl_fixed_t, wl_fixed_t)
{
}

static const wl_pointer_listener POINTER_LISTENER = {
    HandlePointerEnter,        HandlePointerLeave,        HandlePointerMotion,     HandlePointerButton,
    HandlePointerAxis,         HandlePointerFrame,        HandlePointerAxisSource, HandlePointerAxisStop,
    HandlePointerAxisDiscrete, HandlePointerAxisValue120, HandlePointerAxisRelativeDirection,
    HandlePointerWarp,
};

static void HandleTouchDown(void* data, wl_touch*, uint32_t, uint32_t, wl_surface*, int32_t id, wl_fixed_t x, wl_fixed_t y)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    QueueTouch(connection, id == 0 ? TOUCH_ACTION_DOWN : TOUCH_ACTION_POINTER_DOWN, id, (float)wl_fixed_to_double(x),
               (float)wl_fixed_to_double(y));
}

static void HandleTouchUp(void* data, wl_touch*, uint32_t, uint32_t, int32_t id)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    QueueTouch(connection, id == 0 ? TOUCH_ACTION_UP : TOUCH_ACTION_POINTER_UP, id, connection->pointerX, connection->pointerY);
}

static void HandleTouchMotion(void* data, wl_touch*, uint32_t, int32_t id, wl_fixed_t x, wl_fixed_t y)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    QueueTouch(connection, TOUCH_ACTION_MOVE, id, (float)wl_fixed_to_double(x), (float)wl_fixed_to_double(y));
}

static void HandleTouchFrame(void*, wl_touch*)
{
}

static void HandleTouchCancel(void* data, wl_touch*)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    QueueTouch(connection, TOUCH_ACTION_CANCEL, POINTER_TOUCH_ID, connection->pointerX, connection->pointerY);
}

static void HandleTouchShape(void*, wl_touch*, int32_t, wl_fixed_t, wl_fixed_t)
{
}

static void HandleTouchOrientation(void*, wl_touch*, int32_t, wl_fixed_t)
{
}

static const wl_touch_listener TOUCH_LISTENER = {
    HandleTouchDown, HandleTouchUp, HandleTouchMotion, HandleTouchFrame, HandleTouchCancel, HandleTouchShape, HandleTouchOrientation,
};

struct KeySymbolMapping
{
    uint32_t symbol;
    int32_t keyCode;
};

static const KeySymbolMapping KEY_SYMBOLS[] = {
    {XKB_KEY_Escape, 4},      {XKB_KEY_Return, 66},       {XKB_KEY_KP_Enter, 66},   {XKB_KEY_BackSpace, 67},
    {XKB_KEY_Delete, 112},    {XKB_KEY_Tab, 61},          {XKB_KEY_space, 62},      {XKB_KEY_Up, 19},
    {XKB_KEY_Down, 20},       {XKB_KEY_Left, 21},         {XKB_KEY_Right, 22},      {XKB_KEY_Home, 122},
    {XKB_KEY_End, 123},       {XKB_KEY_Page_Up, 92},      {XKB_KEY_Page_Down, 93},  {XKB_KEY_Shift_L, 59},
    {XKB_KEY_Shift_R, 60},    {XKB_KEY_Control_L, 113},   {XKB_KEY_Control_R, 114}, {XKB_KEY_Alt_L, 57},
    {XKB_KEY_Alt_R, 58},      {XKB_KEY_minus, 69},        {XKB_KEY_equal, 70},      {XKB_KEY_bracketleft, 71},
    {XKB_KEY_bracketright, 72}, {XKB_KEY_backslash, 73},  {XKB_KEY_semicolon, 74},  {XKB_KEY_apostrophe, 75},
    {XKB_KEY_grave, 68},      {XKB_KEY_comma, 55},        {XKB_KEY_period, 56},     {XKB_KEY_slash, 76},
};

static constexpr int32_t ANDROID_KEYCODE_A = 29;
static constexpr int32_t ANDROID_KEYCODE_0 = 7;
static constexpr int32_t ANDROID_META_SHIFT = 0x00000001;
static constexpr int32_t ANDROID_META_ALT = 0x00000002;
static constexpr int32_t ANDROID_META_CTRL = 0x00001000;

static int32_t AndroidKeyCodeFor(uint32_t symbol)
{
    if (symbol >= XKB_KEY_a && symbol <= XKB_KEY_z) return ANDROID_KEYCODE_A + (int32_t)(symbol - XKB_KEY_a);
    if (symbol >= XKB_KEY_A && symbol <= XKB_KEY_Z) return ANDROID_KEYCODE_A + (int32_t)(symbol - XKB_KEY_A);
    if (symbol >= XKB_KEY_0 && symbol <= XKB_KEY_9) return ANDROID_KEYCODE_0 + (int32_t)(symbol - XKB_KEY_0);

    for (const KeySymbolMapping& mapping : KEY_SYMBOLS)
    {
        if (mapping.symbol == symbol) return mapping.keyCode;
    }

    return 0;
}

static int32_t AndroidMetaStateFor(void* keyboardState)
{
    xkb_state* state = (xkb_state*)keyboardState;
    if (!state) return 0;

    int32_t meta = 0;

    if (xkb_state_mod_name_is_active(state, XKB_MOD_NAME_SHIFT, XKB_STATE_MODS_EFFECTIVE) > 0) meta |= ANDROID_META_SHIFT;
    if (xkb_state_mod_name_is_active(state, XKB_MOD_NAME_ALT, XKB_STATE_MODS_EFFECTIVE) > 0) meta |= ANDROID_META_ALT;
    if (xkb_state_mod_name_is_active(state, XKB_MOD_NAME_CTRL, XKB_STATE_MODS_EFFECTIVE) > 0) meta |= ANDROID_META_CTRL;

    return meta;
}

static void HandleKeyboardKeymap(void* data, wl_keyboard*, uint32_t format, int32_t descriptor, uint32_t size)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;

    if (format != WL_KEYBOARD_KEYMAP_FORMAT_XKB_V1)
    {
        close(descriptor);
        return;
    }

    char* text = (char*)mmap(nullptr, size, PROT_READ, MAP_PRIVATE, descriptor, 0);
    close(descriptor);

    if (text == MAP_FAILED) return;

    if (!connection->keyboardContext) connection->keyboardContext = xkb_context_new(XKB_CONTEXT_NO_FLAGS);

    xkb_context* context = (xkb_context*)connection->keyboardContext;
    if (!context)
    {
        munmap(text, size);
        return;
    }

    xkb_keymap* keymap = xkb_keymap_new_from_string(context, text, XKB_KEYMAP_FORMAT_TEXT_V1, XKB_KEYMAP_COMPILE_NO_FLAGS);
    munmap(text, size);

    if (!keymap) return;

    if (connection->keyboardState) xkb_state_unref((xkb_state*)connection->keyboardState);
    if (connection->keyboardMap) xkb_keymap_unref((xkb_keymap*)connection->keyboardMap);

    connection->keyboardMap = keymap;
    connection->keyboardState = xkb_state_new(keymap);
}

static void HandleKeyboardEnter(void*, wl_keyboard*, uint32_t, wl_surface*, wl_array*)
{
}

static void HandleKeyboardLeave(void*, wl_keyboard*, uint32_t, wl_surface*)
{
}

static void HandleKeyboardKey(void* data, wl_keyboard*, uint32_t, uint32_t, uint32_t key, uint32_t state)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;

    xkb_state* keyboardState = (xkb_state*)connection->keyboardState;
    if (!keyboardState) return;

    xkb_keycode_t code = key + 8;
    xkb_keysym_t symbol = xkb_state_key_get_one_sym(keyboardState, code);

    WaylandClient::InputEvent event;
    event.kind = WaylandClient::InputKind::Key;
    event.action = state != 0 ? KEY_ACTION_DOWN : KEY_ACTION_UP;
    event.keyCode = AndroidKeyCodeFor((uint32_t)symbol);
    event.unicode = state != 0 ? xkb_state_key_get_utf32(keyboardState, code) : 0;
    event.metaState = AndroidMetaStateFor(connection->keyboardState);
    event.timeNanos = InputNanos();

    if (event.keyCode == 0 && event.unicode == 0) return;

    WaylandClient::QueueInputEvent(*connection, event);
}

static void HandleKeyboardModifiers(void* data, wl_keyboard*, uint32_t, uint32_t depressed, uint32_t latched, uint32_t locked,
                                    uint32_t group)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;
    if (!connection->keyboardState) return;

    xkb_state_update_mask((xkb_state*)connection->keyboardState, depressed, latched, locked, 0, 0, group);
}

static void HandleKeyboardRepeat(void*, wl_keyboard*, int32_t, int32_t)
{
}

static const wl_keyboard_listener KEYBOARD_LISTENER = {
    HandleKeyboardKeymap, HandleKeyboardEnter, HandleKeyboardLeave, HandleKeyboardKey, HandleKeyboardModifiers, HandleKeyboardRepeat,
};

static void HandleSeatCapabilities(void* data, wl_seat* seat, uint32_t capabilities)
{
    WaylandClient::Connection* connection = (WaylandClient::Connection*)data;

    bool hasPointer = (capabilities & WL_SEAT_CAPABILITY_POINTER) != 0;
    bool hasKeyboard = (capabilities & WL_SEAT_CAPABILITY_KEYBOARD) != 0;
    bool hasTouch = (capabilities & WL_SEAT_CAPABILITY_TOUCH) != 0;

    fprintf(stderr, "[waylandclient] the seat offers%s%s%s\n", hasPointer ? " a pointer" : "", hasKeyboard ? " a keyboard" : "",
            hasTouch ? " touch" : "");

    if (hasPointer && !connection->pointer)
    {
        connection->pointer = wl_seat_get_pointer(seat);
        wl_pointer_add_listener(connection->pointer, &POINTER_LISTENER, connection);
    }

    if (hasKeyboard && !connection->keyboard)
    {
        connection->keyboard = wl_seat_get_keyboard(seat);
        wl_keyboard_add_listener(connection->keyboard, &KEYBOARD_LISTENER, connection);
    }

    if (hasTouch && !connection->touch)
    {
        connection->touch = wl_seat_get_touch(seat);
        wl_touch_add_listener(connection->touch, &TOUCH_LISTENER, connection);
    }
}

static void HandleSeatName(void*, wl_seat*, const char*)
{
}

static const wl_seat_listener SEAT_LISTENER = {HandleSeatCapabilities, HandleSeatName};

static void HandleShellPing(void*, xdg_wm_base* shell, uint32_t serial)
{
    xdg_wm_base_pong(shell, serial);
}

static const xdg_wm_base_listener SHELL_LISTENER = {HandleShellPing};

static bool FullscreenRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(WaylandClient::FULLSCREEN_SETTING);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

static bool DecorationRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(WaylandClient::DECORATION_SETTING);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

static void HandleDecorationConfigure(void* data, zxdg_toplevel_decoration_v1*, uint32_t mode)
{
    WaylandClient::Surface* surface = (WaylandClient::Surface*)data;
    surface->decorated = mode == ZXDG_TOPLEVEL_DECORATION_V1_MODE_SERVER_SIDE;

    fprintf(stderr, "[waylandclient] the compositor %s the window frame\n", surface->decorated ? "draws" : "leaves the client to draw");
}

static const zxdg_toplevel_decoration_v1_listener DECORATION_LISTENER = {&HandleDecorationConfigure};

static void HandleOutputGeometry(void* data, wl_output*, int32_t, int32_t, int32_t physicalWidth, int32_t physicalHeight, int32_t,
                                 const char*, const char*, int32_t transform)
{
    WaylandClient::Connection* connection = static_cast<WaylandClient::Connection*>(data);

    connection->outputPhysicalWidthMm = physicalWidth;
    connection->outputPhysicalHeightMm = physicalHeight;
    connection->outputTransform = transform;
}

static void HandleOutputMode(void* data, wl_output*, uint32_t flags, int32_t width, int32_t height, int32_t refresh)
{
    WaylandClient::Connection* connection = static_cast<WaylandClient::Connection*>(data);

    if ((flags & WL_OUTPUT_MODE_CURRENT) == 0) return;
    if (width <= 0 || height <= 0) return;

    connection->outputWidth = static_cast<uint32_t>(width);
    connection->outputHeight = static_cast<uint32_t>(height);
    connection->outputRefreshMilliHertz = refresh > 0 ? static_cast<uint32_t>(refresh) : 0;
}

static void HandleOutputDone(void* data, wl_output*)
{
    WaylandClient::Connection* connection = static_cast<WaylandClient::Connection*>(data);
    connection->outputDescribed = true;
}

static void HandleOutputScale(void* data, wl_output*, int32_t factor)
{
    WaylandClient::Connection* connection = static_cast<WaylandClient::Connection*>(data);
    if (factor > 0) connection->outputScale = static_cast<uint32_t>(factor);
}

static void HandleOutputName(void*, wl_output*, const char*)
{
}

static void HandleOutputDescription(void*, wl_output*, const char*)
{
}

static const wl_output_listener OUTPUT_LISTENER = {HandleOutputGeometry, HandleOutputMode,        HandleOutputDone,
                                                   HandleOutputScale,    HandleOutputName, HandleOutputDescription};

static void HandleRegistryGlobal(void* data, wl_registry* registry, uint32_t name, const char* interface, uint32_t version)
{
    WaylandClient::Connection* connection = static_cast<WaylandClient::Connection*>(data);

    if (strcmp(interface, wl_output_interface.name) == 0)
    {
        if (connection->output != nullptr) return;

        uint32_t bound = version < 4 ? version : 4;
        connection->output = static_cast<wl_output*>(wl_registry_bind(registry, name, &wl_output_interface, bound));
        wl_output_add_listener(connection->output, &OUTPUT_LISTENER, connection);
        return;
    }

    if (strcmp(interface, wl_compositor_interface.name) == 0)
    {
        uint32_t bound = version < 4 ? version : 4;
        connection->compositor = static_cast<wl_compositor*>(wl_registry_bind(registry, name, &wl_compositor_interface, bound));
        gCompositorVersion = bound;
        return;
    }

    if (strcmp(interface, wl_shm_interface.name) == 0)
    {
        connection->sharedMemory = static_cast<wl_shm*>(wl_registry_bind(registry, name, &wl_shm_interface, 1));
        return;
    }

    if (strcmp(interface, xdg_wm_base_interface.name) == 0)
    {
        uint32_t bound = version < 3 ? version : 3;
        connection->shell = static_cast<xdg_wm_base*>(wl_registry_bind(registry, name, &xdg_wm_base_interface, bound));
        xdg_wm_base_add_listener(connection->shell, &SHELL_LISTENER, connection);
        return;
    }

    if (strcmp(interface, wl_seat_interface.name) == 0)
    {
        uint32_t bound = version < 5 ? version : 5;
        connection->seat = static_cast<wl_seat*>(wl_registry_bind(registry, name, &wl_seat_interface, bound));
        wl_seat_add_listener(connection->seat, &SEAT_LISTENER, connection);
        return;
    }

    if (strcmp(interface, wp_presentation_interface.name) == 0)
    {
        connection->presentation = static_cast<wp_presentation*>(wl_registry_bind(registry, name, &wp_presentation_interface, 1));
        return;
    }

    if (strcmp(interface, zxdg_decoration_manager_v1_interface.name) == 0)
    {
        connection->decorations =
            static_cast<zxdg_decoration_manager_v1*>(wl_registry_bind(registry, name, &zxdg_decoration_manager_v1_interface, 1));
    }
}

static void HandleRegistryRemove(void*, wl_registry*, uint32_t)
{
}

static const wl_registry_listener REGISTRY_LISTENER = {HandleRegistryGlobal, HandleRegistryRemove};

static void HandleBufferRelease(void* data, wl_buffer*)
{
    WaylandClient::Buffer* buffer = static_cast<WaylandClient::Buffer*>(data);
    buffer->heldByCompositor = false;
}

static const wl_buffer_listener BUFFER_LISTENER = {HandleBufferRelease};

static void HandleShellSurfaceConfigure(void* data, xdg_surface* shellSurface, uint32_t serial)
{
    WaylandClient::Surface* surface = static_cast<WaylandClient::Surface*>(data);
    xdg_surface_ack_configure(shellSurface, serial);
    surface->configured = true;
}

static const xdg_surface_listener SHELL_SURFACE_LISTENER = {HandleShellSurfaceConfigure};

static void HandleToplevelConfigure(void* data, xdg_toplevel*, int32_t width, int32_t height, wl_array* states)
{
    WaylandClient::Surface* surface = static_cast<WaylandClient::Surface*>(data);

    surface->fullscreen = false;

    if (states)
    {
        const uint32_t* cursor = static_cast<const uint32_t*>(states->data);
        size_t count = states->size / sizeof(uint32_t);

        for (size_t index = 0; index < count; ++index)
        {
            if (cursor[index] == XDG_TOPLEVEL_STATE_FULLSCREEN) surface->fullscreen = true;
        }
    }

    if (width <= 0 || height <= 0) return;

    surface->pendingWidth = static_cast<uint32_t>(width);
    surface->pendingHeight = static_cast<uint32_t>(height);
}

static void HandleToplevelClose(void* data, xdg_toplevel*)
{
    WaylandClient::Surface* surface = static_cast<WaylandClient::Surface*>(data);
    surface->closeRequested = true;
}

static void HandleToplevelConfigureBounds(void* data, xdg_toplevel*, int32_t width, int32_t height)
{
    WaylandClient::Surface* surface = static_cast<WaylandClient::Surface*>(data);

    if (width <= 0 || height <= 0) return;

    surface->boundsWidth = static_cast<uint32_t>(width);
    surface->boundsHeight = static_cast<uint32_t>(height);
}

static void HandleToplevelCapabilities(void*, xdg_toplevel*, wl_array*)
{
}

static const xdg_toplevel_listener TOPLEVEL_LISTENER = {HandleToplevelConfigure, HandleToplevelClose, HandleToplevelConfigureBounds,
                                                        HandleToplevelCapabilities};

static void HandleFrameDone(void* data, wl_callback* callback, uint32_t milliseconds)
{
    wl_callback_destroy(callback);

    SurfaceListener* listener = static_cast<SurfaceListener*>(data);
    if (listener->surface) listener->surface->frameRequested = false;
    if (listener->callback) listener->callback(listener->context, static_cast<uint64_t>(milliseconds) * 1000000ull);

    delete listener;
}

static const wl_callback_listener FRAME_LISTENER = {HandleFrameDone};

static int CreateAnonymousFile(size_t sizeBytes)
{
    int fileDescriptor = memfd_create("acl-shm", MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (fileDescriptor < 0) return -1;

    if (ftruncate(fileDescriptor, static_cast<off_t>(sizeBytes)) != 0)
    {
        close(fileDescriptor);
        return -1;
    }

    return fileDescriptor;
}

static std::string CompositorSocketPath()
{
    const char* configured = Settings::Lookup(WaylandClient::SOCKET_SETTING);

    if (configured && *configured)
    {
        if (configured[0] == '/') return configured;
        return Settings::RuntimeDirectory() + "/" + configured;
    }

    std::string runtimeDirectory = Settings::RuntimeDirectory();

    for (const char* name : {"wayland-0", "wayland-1"})
    {
        std::string candidate = runtimeDirectory + "/" + name;
        if (access(candidate.c_str(), R_OK | W_OK) == 0) return candidate;
    }

    return std::string();
}

bool WaylandClient::IsAvailable()
{
    std::string socketPath = CompositorSocketPath();
    if (socketPath.empty()) return false;

    return access(socketPath.c_str(), R_OK | W_OK) == 0;
}

static int ConnectToCompositor()
{
    std::string socketPath = CompositorSocketPath();
    if (socketPath.empty()) return -1;

    sockaddr_un address{};
    address.sun_family = AF_UNIX;

    if (socketPath.size() >= sizeof(address.sun_path)) return -1;
    memcpy(address.sun_path, socketPath.c_str(), socketPath.size());

    int socketHandle = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (socketHandle < 0) return -1;

    if (connect(socketHandle, (const sockaddr*)&address, sizeof(address)) == 0) return socketHandle;

    close(socketHandle);

    return -1;
}

static void HandleSyncDone(void* data, wl_callback* callback, uint32_t)
{
    wl_callback_destroy(callback);
    *static_cast<bool*>(data) = true;
}

static const wl_callback_listener SYNC_LISTENER = {HandleSyncDone};

static bool RoundtripWithin(wl_display* display, int timeoutMillis)
{
    wl_callback* sync = wl_display_sync(display);
    if (!sync) return false;

    bool done = false;
    wl_callback_add_listener(sync, &SYNC_LISTENER, &done);

    int fileDescriptor = wl_display_get_fd(display);
    int remaining = timeoutMillis;

    while (!done)
    {
        while (wl_display_prepare_read(display) != 0)
        {
            if (wl_display_dispatch_pending(display) < 0) return false;
            if (done) return true;
        }

        if (wl_display_flush(display) < 0 && errno != EAGAIN)
        {
            wl_display_cancel_read(display);
            return false;
        }

        pollfd waiting{};
        waiting.fd = fileDescriptor;
        waiting.events = POLLIN;

        int ready = poll(&waiting, 1, remaining);
        if (ready < 0 && errno == EINTR)
        {
            wl_display_cancel_read(display);
            continue;
        }

        if (ready <= 0)
        {
            wl_display_cancel_read(display);
            return false;
        }

        if (wl_display_read_events(display) < 0) return false;
        if (wl_display_dispatch_pending(display) < 0) return false;
    }

    return true;
}

WaylandClient::Result WaylandClient::Connect(Connection& connection)
{
    if (connection.display) return Result::AlreadyConnected;

    int socketHandle = ConnectToCompositor();
    if (socketHandle < 0) return Result::NoDisplay;

    connection.display = wl_display_connect_to_fd(socketHandle);

    if (!connection.display)
    {
        close(socketHandle);
        return Result::NoDisplay;
    }

    connection.registry = wl_display_get_registry(connection.display);
    wl_registry_add_listener(connection.registry, &REGISTRY_LISTENER, &connection);

    if (!RoundtripWithin(connection.display, CONNECT_TIMEOUT_MILLIS))
    {
        Disconnect(connection);
        return Result::NoDisplay;
    }

    if (!connection.compositor)
    {
        Disconnect(connection);
        return Result::NoCompositor;
    }
    if (!connection.sharedMemory)
    {
        Disconnect(connection);
        return Result::NoSharedMemory;
    }
    if (!connection.shell)
    {
        Disconnect(connection);
        return Result::NoShell;
    }

    connection.fileDescriptor = wl_display_get_fd(connection.display);

    return Result::Ok;
}

void WaylandClient::Disconnect(Connection& connection)
{
    if (connection.pointer) wl_pointer_destroy(connection.pointer);
    if (connection.keyboard) wl_keyboard_destroy(connection.keyboard);
    if (connection.touch) wl_touch_destroy(connection.touch);
    if (connection.seat) wl_seat_destroy(connection.seat);
    if (connection.keyboardState) xkb_state_unref((xkb_state*)connection.keyboardState);
    if (connection.keyboardMap) xkb_keymap_unref((xkb_keymap*)connection.keyboardMap);
    if (connection.keyboardContext) xkb_context_unref((xkb_context*)connection.keyboardContext);
    if (connection.presentation) wp_presentation_destroy(connection.presentation);
    if (connection.shell) xdg_wm_base_destroy(connection.shell);
    if (connection.sharedMemory) wl_shm_destroy(connection.sharedMemory);
    if (connection.compositor) wl_compositor_destroy(connection.compositor);
    if (connection.registry) wl_registry_destroy(connection.registry);
    if (connection.display) wl_display_disconnect(connection.display);

    gCompositorVersion = 0;
    connection = Connection{};
}

static void FreeRetiredPool(WaylandClient::RetiredPool& retired)
{
    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        if (retired.buffers[index].buffer) wl_buffer_destroy(retired.buffers[index].buffer);
        retired.buffers[index] = WaylandClient::Buffer{};
    }

    if (retired.pool) wl_shm_pool_destroy(retired.pool);
    if (retired.base) munmap(retired.base, retired.sizeBytes);
    if (retired.fileDescriptor >= 0) close(retired.fileDescriptor);

    retired = WaylandClient::RetiredPool{};
}

static bool RetiredPoolIsIdle(const WaylandClient::RetiredPool& retired)
{
    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        if (retired.buffers[index].buffer && retired.buffers[index].heldByCompositor) return false;
    }

    return true;
}

static void ReapRetiredPools(WaylandClient::Surface& surface)
{
    for (int slot = 0; slot < WaylandClient::MAX_RETIRED_POOLS; ++slot)
    {
        WaylandClient::RetiredPool& retired = surface.retired[slot];

        if (!retired.active) continue;
        if (!RetiredPoolIsIdle(retired)) continue;

        FreeRetiredPool(retired);
    }
}

static bool RetireBufferPool(WaylandClient::Surface& surface)
{
    ReapRetiredPools(surface);

    int slot = 0;
    while (slot < WaylandClient::MAX_RETIRED_POOLS && surface.retired[slot].active) slot += 1;

    if (slot == WaylandClient::MAX_RETIRED_POOLS) return false;

    WaylandClient::RetiredPool& retired = surface.retired[slot];

    retired.pool = surface.pool;
    retired.base = surface.poolBase;
    retired.sizeBytes = surface.poolSizeBytes;
    retired.fileDescriptor = surface.poolFileDescriptor;
    retired.active = true;

    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        retired.buffers[index] = surface.buffers[index];
        surface.buffers[index] = WaylandClient::Buffer{};

        if (retired.buffers[index].buffer) wl_buffer_set_user_data(retired.buffers[index].buffer, &retired.buffers[index]);
    }

    surface.pool = nullptr;
    surface.poolBase = nullptr;
    surface.poolSizeBytes = 0;
    surface.poolFileDescriptor = WaylandClient::INVALID_FILE_DESCRIPTOR;

    if (RetiredPoolIsIdle(retired)) FreeRetiredPool(retired);

    return true;
}

static WaylandClient::Result AllocateBufferPool(WaylandClient::Connection& connection, WaylandClient::Surface& surface, uint32_t width,
                                                uint32_t height)
{
    uint32_t stride = width * WaylandClient::BYTES_PER_PIXEL;
    size_t bufferBytes = static_cast<size_t>(stride) * height;
    size_t poolBytes = bufferBytes * WaylandClient::BUFFER_COUNT;

    if (poolBytes > static_cast<size_t>(INT32_MAX)) return WaylandClient::Result::InvalidGeometry;

    int fileDescriptor = CreateAnonymousFile(poolBytes);
    if (fileDescriptor < 0) return WaylandClient::Result::MappingFailed;

    void* base = mmap(nullptr, poolBytes, PROT_READ | PROT_WRITE, MAP_SHARED, fileDescriptor, 0);
    if (base == MAP_FAILED)
    {
        close(fileDescriptor);
        return WaylandClient::Result::MappingFailed;
    }

    surface.poolFileDescriptor = fileDescriptor;
    surface.poolBase = static_cast<uint8_t*>(base);
    surface.poolSizeBytes = poolBytes;
    surface.width = width;
    surface.height = height;

    surface.pool = wl_shm_create_pool(connection.sharedMemory, fileDescriptor, static_cast<int32_t>(poolBytes));

    for (int index = 0; index < WaylandClient::BUFFER_COUNT; ++index)
    {
        WaylandClient::Buffer& buffer = surface.buffers[index];
        buffer.pixels = surface.poolBase + static_cast<size_t>(index) * bufferBytes;
        buffer.width = width;
        buffer.height = height;
        buffer.stride = stride;
        buffer.sizeBytes = bufferBytes;
        buffer.heldByCompositor = false;
        buffer.buffer = wl_shm_pool_create_buffer(surface.pool, static_cast<int32_t>(index * bufferBytes), static_cast<int32_t>(width),
                                                  static_cast<int32_t>(height), static_cast<int32_t>(stride), WL_SHM_FORMAT_ARGB8888);
        wl_buffer_add_listener(buffer.buffer, &BUFFER_LISTENER, &buffer);
    }

    return WaylandClient::Result::Ok;
}

WaylandClient::DisplayMode WaylandClient::CurrentDisplayMode(const Connection& connection)
{
    DisplayMode mode;

    mode.width = connection.outputWidth;
    mode.height = connection.outputHeight;
    mode.refreshMilliHertz = connection.outputRefreshMilliHertz;
    mode.scale = connection.outputScale;
    mode.physicalWidthMm = connection.outputPhysicalWidthMm;
    mode.physicalHeightMm = connection.outputPhysicalHeightMm;
    mode.described = connection.outputDescribed && connection.outputWidth > 0 && connection.outputRefreshMilliHertz > 0;

    return mode;
}

WaylandClient::Result WaylandClient::Resize(Connection& connection, Surface& surface, uint32_t width, uint32_t height)
{
    if (!connection.display) return Result::NotConnected;
    if (width < MIN_DIMENSION || height < MIN_DIMENSION) return Result::InvalidGeometry;
    if (width > MAX_DIMENSION || height > MAX_DIMENSION) return Result::InvalidGeometry;
    if (width == surface.width && height == surface.height) return Result::Ok;

    if (!RetireBufferPool(surface))
    {
        fprintf(stderr, "[waylandclient] too many buffer pools are still held by the compositor to resize now\n");
        return Result::NoFreeBuffer;
    }

    Result allocated = AllocateBufferPool(connection, surface, width, height);
    if (allocated != Result::Ok) return allocated;

    surface.resized = true;
    return Result::Ok;
}

WaylandClient::Result WaylandClient::SetFullscreen(Surface& surface, bool fullscreen)
{
    if (!surface.toplevel) return Result::NoShell;

    if (fullscreen) xdg_toplevel_set_fullscreen(surface.toplevel, nullptr);
    else xdg_toplevel_unset_fullscreen(surface.toplevel);

    return Result::Ok;
}

bool WaylandClient::ConsumeResize(Surface& surface, uint32_t& width, uint32_t& height)
{
    if (!surface.resized) return false;

    surface.resized = false;
    width = surface.width;
    height = surface.height;
    return true;
}

WaylandClient::Result WaylandClient::CreateSurface(Connection& connection, uint32_t width, uint32_t height, const char* title, Surface& surface)
{
    if (!connection.display) return Result::NotConnected;
    if (width < MIN_DIMENSION || height < MIN_DIMENSION) return Result::InvalidGeometry;
    if (width > MAX_DIMENSION || height > MAX_DIMENSION) return Result::InvalidGeometry;

    Result allocated = AllocateBufferPool(connection, surface, width, height);
    if (allocated != Result::Ok) return allocated;

    surface.surface = wl_compositor_create_surface(connection.compositor);
    surface.shellSurface = xdg_wm_base_get_xdg_surface(connection.shell, surface.surface);
    xdg_surface_add_listener(surface.shellSurface, &SHELL_SURFACE_LISTENER, &surface);

    surface.toplevel = xdg_surface_get_toplevel(surface.shellSurface);
    xdg_toplevel_add_listener(surface.toplevel, &TOPLEVEL_LISTENER, &surface);
    xdg_toplevel_set_title(surface.toplevel, title ? title : DEFAULT_TITLE);
    xdg_toplevel_set_app_id(surface.toplevel, DEFAULT_APPLICATION_ID);

    if (DecorationRequested())
    {
        if (!connection.decorations)
            fprintf(stderr, "[waylandclient] this compositor does not offer window decorations, so the window has none\n");
        else
        {
            surface.decoration = zxdg_decoration_manager_v1_get_toplevel_decoration(connection.decorations, surface.toplevel);
            zxdg_toplevel_decoration_v1_add_listener(surface.decoration, &DECORATION_LISTENER, &surface);
            zxdg_toplevel_decoration_v1_set_mode(surface.decoration, ZXDG_TOPLEVEL_DECORATION_V1_MODE_SERVER_SIDE);
        }
    }

    if (FullscreenRequested())
    {
        xdg_toplevel_set_fullscreen(surface.toplevel, nullptr);
        fprintf(stderr, "[waylandclient] the window asked the compositor for the whole output\n");
    }

    wl_surface_commit(surface.surface);
    wl_display_roundtrip(connection.display);

    if (!surface.configured)
    {
        DestroySurface(surface);
        return Result::NoShell;
    }

    return Result::Ok;
}

void WaylandClient::DestroySurface(Surface& surface)
{
    for (int slot = 0; slot < MAX_RETIRED_POOLS; ++slot)
    {
        if (surface.retired[slot].active) FreeRetiredPool(surface.retired[slot]);
    }

    for (int index = 0; index < BUFFER_COUNT; ++index)
    {
        if (surface.buffers[index].buffer) wl_buffer_destroy(surface.buffers[index].buffer);
        surface.buffers[index] = Buffer{};
    }

    if (surface.pool) wl_shm_pool_destroy(surface.pool);
    if (surface.poolBase) munmap(surface.poolBase, surface.poolSizeBytes);
    if (surface.poolFileDescriptor >= 0) close(surface.poolFileDescriptor);

    if (surface.decoration) zxdg_toplevel_decoration_v1_destroy(surface.decoration);
    if (surface.toplevel) xdg_toplevel_destroy(surface.toplevel);
    if (surface.shellSurface) xdg_surface_destroy(surface.shellSurface);
    if (surface.surface) wl_surface_destroy(surface.surface);

    surface = Surface{};
}

WaylandClient::Result WaylandClient::AcquireBuffer(Surface& surface, Buffer*& buffer)
{
    ReapRetiredPools(surface);

    for (int index = 0; index < BUFFER_COUNT; ++index)
    {
        if (surface.buffers[index].heldByCompositor) continue;

        buffer = &surface.buffers[index];
        return Result::Ok;
    }

    buffer = nullptr;
    return Result::NoFreeBuffer;
}

WaylandClient::Result WaylandClient::Present(Connection& connection, Surface& surface, Buffer& buffer)
{
    if (!connection.display) return Result::NotConnected;
    if (!surface.surface) return Result::InvalidGeometry;

    wl_surface_attach(surface.surface, buffer.buffer, 0, 0);

    if (gCompositorVersion >= WL_SURFACE_DAMAGE_BUFFER_SINCE_VERSION)
    {
        wl_surface_damage_buffer(surface.surface, 0, 0, static_cast<int32_t>(surface.width), static_cast<int32_t>(surface.height));
    }
    else
    {
        wl_surface_damage(surface.surface, 0, 0, static_cast<int32_t>(surface.width), static_cast<int32_t>(surface.height));
    }

    wl_surface_commit(surface.surface);

    buffer.heldByCompositor = true;
    return Flush(connection);
}

WaylandClient::Result WaylandClient::RequestFrame(Connection& connection, Surface& surface, FrameCallback callback, void* context)
{
    if (!connection.display) return Result::NotConnected;
    if (!surface.surface) return Result::InvalidGeometry;
    if (surface.frameRequested) return Result::Ok;

    SurfaceListener* listener = new SurfaceListener;
    listener->surface = &surface;
    listener->callback = callback;
    listener->context = context;

    wl_callback* frame = wl_surface_frame(surface.surface);
    if (!frame)
    {
        delete listener;
        return Result::DispatchFailed;
    }

    wl_callback_add_listener(frame, &FRAME_LISTENER, listener);
    wl_surface_commit(surface.surface);

    surface.frameRequested = true;
    return Flush(connection);
}

WaylandClient::Result WaylandClient::Dispatch(Connection& connection)
{
    if (!connection.display) return Result::NotConnected;
    return wl_display_dispatch(connection.display) < 0 ? Result::DispatchFailed : Result::Ok;
}

WaylandClient::Result WaylandClient::DispatchPending(Connection& connection)
{
    if (!connection.display) return Result::NotConnected;

    if (wl_display_prepare_read(connection.display) == 0)
    {
        wl_display_read_events(connection.display);
    }

    return wl_display_dispatch_pending(connection.display) < 0 ? Result::DispatchFailed : Result::Ok;
}

WaylandClient::Result WaylandClient::Flush(Connection& connection)
{
    if (!connection.display) return Result::NotConnected;
    return wl_display_flush(connection.display) < 0 ? Result::FlushFailed : Result::Ok;
}

void WaylandClient::ReportConnectionError(Connection& connection)
{
    if (!connection.display) return;

    int failure = wl_display_get_error(connection.display);

    if (failure == EPROTO)
    {
        const wl_interface* interface = nullptr;
        uint32_t object = 0;
        uint32_t code = wl_display_get_protocol_error(connection.display, &interface, &object);

        fprintf(stderr, "[waylandclient] the compositor reported protocol error %u on %s object %u\n", code,
                interface && interface->name ? interface->name : "an unnamed interface", object);
        return;
    }

    fprintf(stderr, "[waylandclient] the connection failed with errno %d (%s)\n", failure, strerror(failure));
}

WaylandClient::Result WaylandClient::Roundtrip(Connection& connection)
{
    if (!connection.display) return Result::NotConnected;
    return wl_display_roundtrip(connection.display) < 0 ? Result::DispatchFailed : Result::Ok;
}

const char* WaylandClient::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NoDisplay: return "NoDisplay";
        case Result::NoCompositor: return "NoCompositor";
        case Result::NoShell: return "NoShell";
        case Result::NoSharedMemory: return "NoSharedMemory";
        case Result::AlreadyConnected: return "AlreadyConnected";
        case Result::NotConnected: return "NotConnected";
        case Result::InvalidGeometry: return "InvalidGeometry";
        case Result::MappingFailed: return "MappingFailed";
        case Result::NoFreeBuffer: return "NoFreeBuffer";
        case Result::DispatchFailed: return "DispatchFailed";
        case Result::FlushFailed: return "FlushFailed";
    }
    return "Unknown";
}

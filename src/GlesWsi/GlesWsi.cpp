#include "GlesWsi/GlesWsi.hpp"

#define EGL_NO_X11 1

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl32.h>

#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <mutex>
#include <string>
#include <sys/stat.h>
#include <type_traits>
#include <unistd.h>
#include <unordered_map>
#include <vector>

#include "AbiBridge/AbiBridge.hpp"
#include "Overlay/FrameRate.hpp"
#include "Platform/DeviceIdentity.hpp"
#include "Settings/Settings.hpp"

#include "GlesEntryPointList.hpp"

using WaylandEglWindowCreate = void* (*)(wl_surface*, int, int);
using WaylandEglWindowResize = void (*)(void*, int, int, int, int);
using WaylandEglWindowDestroy = void (*)(void*);

static void* gEglDriver = nullptr;
static void* gGlesDriver = nullptr;
static void* gWaylandEgl = nullptr;

static AbiBridge::Registry* gRegistry = nullptr;
static GuestRuntime::Instance* gRuntime = nullptr;
static GlesWsi::Statistics gStatistics;

static wl_display* gDisplay = nullptr;
static wl_surface* gSurface = nullptr;
static uint32_t gSurfaceWidth = 0;
static uint32_t gSurfaceHeight = 0;

static void* gEglWindow = nullptr;
static EGLDisplay gHostDisplay = EGL_NO_DISPLAY;

static WaylandEglWindowCreate gWindowCreate = nullptr;
static WaylandEglWindowResize gWindowResize = nullptr;
static WaylandEglWindowDestroy gWindowDestroy = nullptr;

static EGLDisplay (*gHostGetPlatformDisplay)(EGLenum, void*, const EGLAttrib*) = nullptr;
static EGLDisplay (*gHostGetDisplay)(EGLNativeDisplayType) = nullptr;
static EGLSurface (*gHostCreateWindowSurface)(EGLDisplay, EGLConfig, EGLNativeWindowType, const EGLint*) = nullptr;
static EGLBoolean (*gHostSwapBuffers)(EGLDisplay, EGLSurface) = nullptr;
static void* (*gHostGetProcAddress)(const char*) = nullptr;
static const GLubyte* (*gHostGetString)(GLenum) = nullptr;
static const GLubyte* (*gHostGetStringi)(GLenum, GLuint) = nullptr;

static GuestState::GuestAddress gStringArena{};
static uint64_t gStringArenaUsed = 0;
static std::unordered_map<std::string, uint64_t> gStringAddresses;

static EGLBoolean (*gHostGetConfigAttrib)(EGLDisplay, EGLConfig, EGLint, EGLint*) = nullptr;
static EGLBoolean (*gHostDestroySurface)(EGLDisplay, EGLSurface) = nullptr;
static EGLBoolean (*gHostSwapInterval)(EGLDisplay, EGLint) = nullptr;
static const char* (*gHostQueryString)(EGLDisplay, EGLint) = nullptr;
static EGLSyncKHR (*gHostCreateSync)(EGLDisplay, EGLenum, const EGLint*) = nullptr;
static EGLBoolean (*gHostDestroySync)(EGLDisplay, EGLSyncKHR) = nullptr;
static EGLint (*gHostDupNativeFence)(EGLDisplay, EGLSyncKHR) = nullptr;

struct gbm_device;
struct gbm_surface;
struct gbm_bo;

static constexpr uint32_t GBM_USE_RENDERING = 1u << 2;
static constexpr uint32_t GBM_USE_LINEAR = 1u << 4;
static constexpr uint32_t GBM_TRANSFER_READ = 1u << 0;
static constexpr int MAX_ATTRIBUTE_PAIRS = 32;

struct GbmLibrary
{
    void* handle = nullptr;
    bool attempted = false;
    gbm_device* (*createDevice)(int) = nullptr;
    void (*destroyDevice)(gbm_device*) = nullptr;
    gbm_surface* (*createSurface)(gbm_device*, uint32_t, uint32_t, uint32_t, uint32_t) = nullptr;
    gbm_surface* (*createSurfaceWithModifiers)(gbm_device*, uint32_t, uint32_t, uint32_t, const uint64_t*, unsigned int, uint32_t) = nullptr;
    void (*destroySurface)(gbm_surface*) = nullptr;
    gbm_bo* (*lockFrontBuffer)(gbm_surface*) = nullptr;
    void (*releaseBuffer)(gbm_surface*, gbm_bo*) = nullptr;
    int (*hasFreeBuffers)(gbm_surface*) = nullptr;
    int (*planeCount)(gbm_bo*) = nullptr;
    int (*descriptorForPlane)(gbm_bo*, int) = nullptr;
    uint32_t (*offset)(gbm_bo*, int) = nullptr;
    uint32_t (*strideForPlane)(gbm_bo*, int) = nullptr;
    uint64_t (*modifier)(gbm_bo*) = nullptr;
    uint32_t (*width)(gbm_bo*) = nullptr;
    uint32_t (*height)(gbm_bo*) = nullptr;
    uint32_t (*format)(gbm_bo*) = nullptr;
    void* (*map)(gbm_bo*, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t, uint32_t*, void**) = nullptr;
    void (*unmap)(gbm_bo*, void*) = nullptr;
};

struct HostedSlot
{
    gbm_bo* bo = nullptr;
    bool held = false;
};

struct HostedTarget
{
    HostedWindow::Session* session = nullptr;
    uint32_t width = 0;
    uint32_t height = 0;
    int renderNode = -1;
    gbm_device* device = nullptr;
    gbm_surface* surface = nullptr;
    EGLSurface eglSurface = EGL_NO_SURFACE;
    EGLDisplay eglDisplay = EGL_NO_DISPLAY;
    uint32_t format = 0;
    uint32_t generation = 0;
    HostedSlot slots[ControlChannel::MAX_FRAME_BUFFERS];
    uint32_t slotCount = 0;
    bool sharing = false;
    bool nativeFences = false;
    EGLint swapInterval = 1;
    std::mutex mutex;
    std::condition_variable released;
    std::vector<uint32_t> releasedIndices;
    bool refused = false;
};

static GbmLibrary gGbm;
static HostedTarget gHosted;

template <typename Function> static bool ResolveGbmFunction(const char* name, Function& function)
{
    function = reinterpret_cast<Function>(dlsym(gGbm.handle, name));
    return function != nullptr;
}

static bool LoadGbm()
{
    if (gGbm.attempted) return gGbm.handle != nullptr;
    gGbm.attempted = true;

    gGbm.handle = dlopen(GlesWsi::GBM_SONAME, RTLD_NOW | RTLD_LOCAL);
    if (!gGbm.handle) return false;

    ResolveGbmFunction("gbm_surface_create_with_modifiers2", gGbm.createSurfaceWithModifiers);

    bool resolved = ResolveGbmFunction("gbm_create_device", gGbm.createDevice) && ResolveGbmFunction("gbm_device_destroy", gGbm.destroyDevice) &&
                    ResolveGbmFunction("gbm_surface_create", gGbm.createSurface) &&
                    ResolveGbmFunction("gbm_surface_destroy", gGbm.destroySurface) &&
                    ResolveGbmFunction("gbm_surface_lock_front_buffer", gGbm.lockFrontBuffer) &&
                    ResolveGbmFunction("gbm_surface_release_buffer", gGbm.releaseBuffer) &&
                    ResolveGbmFunction("gbm_surface_has_free_buffers", gGbm.hasFreeBuffers) &&
                    ResolveGbmFunction("gbm_bo_get_plane_count", gGbm.planeCount) &&
                    ResolveGbmFunction("gbm_bo_get_fd_for_plane", gGbm.descriptorForPlane) &&
                    ResolveGbmFunction("gbm_bo_get_offset", gGbm.offset) &&
                    ResolveGbmFunction("gbm_bo_get_stride_for_plane", gGbm.strideForPlane) &&
                    ResolveGbmFunction("gbm_bo_get_modifier", gGbm.modifier) && ResolveGbmFunction("gbm_bo_get_width", gGbm.width) &&
                    ResolveGbmFunction("gbm_bo_get_height", gGbm.height) && ResolveGbmFunction("gbm_bo_get_format", gGbm.format) &&
                    ResolveGbmFunction("gbm_bo_map", gGbm.map) && ResolveGbmFunction("gbm_bo_unmap", gGbm.unmap);

    if (resolved) return true;

    fprintf(stderr, "[gleswsi] %s lacks functions this runtime needs, so OpenGL ES cannot render for a hosted window\n", GlesWsi::GBM_SONAME);
    dlclose(gGbm.handle);
    gGbm.handle = nullptr;
    return false;
}

static int OpenRenderNode(uint64_t wanted)
{
    DIR* directory = opendir(GlesWsi::RENDER_NODE_DIRECTORY);
    if (!directory) return -1;

    int first = -1;
    int chosen = -1;

    while (dirent* entry = readdir(directory))
    {
        if (strncmp(entry->d_name, "renderD", 7) != 0) continue;

        std::string path = std::string(GlesWsi::RENDER_NODE_DIRECTORY) + "/" + entry->d_name;
        int descriptor = open(path.c_str(), O_RDWR | O_CLOEXEC);
        if (descriptor < 0) continue;

        struct stat described{};
        if (wanted != 0 && fstat(descriptor, &described) == 0 && (uint64_t)described.st_rdev == wanted)
        {
            chosen = descriptor;
            break;
        }

        if (first < 0)
        {
            first = descriptor;
            continue;
        }

        close(descriptor);
    }

    closedir(directory);

    if (chosen >= 0)
    {
        if (first >= 0) close(first);
        return chosen;
    }

    return first;
}

static bool HasExtension(const char* extensions, const char* name)
{
    if (!extensions) return false;

    size_t length = strlen(name);

    for (const char* cursor = strstr(extensions, name); cursor; cursor = strstr(cursor + length, name))
    {
        bool startsWord = cursor == extensions || cursor[-1] == ' ';
        bool endsWord = cursor[length] == ' ' || cursor[length] == '\0';
        if (startsWord && endsWord) return true;
    }

    return false;
}

static EGLDisplay HostedDisplay()
{
    if (!LoadGbm() || !gHostGetPlatformDisplay)
    {
        fprintf(stderr, "[gleswsi] the host has no gbm, so OpenGL ES cannot render for a hosted window\n");
        return EGL_NO_DISPLAY;
    }

    uint64_t wanted = 0;
    if (gHosted.session) wanted = HostedWindow::CurrentDisplay(*gHosted.session).renderDevice;

    gHosted.renderNode = OpenRenderNode(wanted);
    if (gHosted.renderNode < 0)
    {
        fprintf(stderr, "[gleswsi] no gpu render node can be opened, so OpenGL ES cannot render for a hosted window\n");
        return EGL_NO_DISPLAY;
    }

    gHosted.device = gGbm.createDevice(gHosted.renderNode);
    if (!gHosted.device)
    {
        fprintf(stderr, "[gleswsi] gbm cannot drive the gpu render node, so OpenGL ES cannot render for a hosted window\n");
        close(gHosted.renderNode);
        gHosted.renderNode = -1;
        return EGL_NO_DISPLAY;
    }

    EGLDisplay display = gHostGetPlatformDisplay(EGL_PLATFORM_GBM_KHR, gHosted.device, nullptr);
    if (display == EGL_NO_DISPLAY)
    {
        fprintf(stderr, "[gleswsi] the host driver has no display for gbm, so OpenGL ES cannot render for a hosted window\n");
        return EGL_NO_DISPLAY;
    }

    gHosted.eglDisplay = display;
    fprintf(stderr, "[gleswsi] the guest renders on the gpu through gbm, and its frames are handed to the window host\n");

    return display;
}

static void LoadFenceFunctions(EGLDisplay display)
{
    if (!gHostQueryString || !gHostGetProcAddress) return;
    if (!HasExtension(gHostQueryString(display, EGL_EXTENSIONS), "EGL_ANDROID_native_fence_sync")) return;

    gHostCreateSync = (EGLSyncKHR(*)(EGLDisplay, EGLenum, const EGLint*))gHostGetProcAddress("eglCreateSyncKHR");
    gHostDestroySync = (EGLBoolean(*)(EGLDisplay, EGLSyncKHR))gHostGetProcAddress("eglDestroySyncKHR");
    gHostDupNativeFence = (EGLint(*)(EGLDisplay, EGLSyncKHR))gHostGetProcAddress("eglDupNativeFenceFDANDROID");

    gHosted.nativeFences = gHostCreateSync && gHostDestroySync && gHostDupNativeFence;
}

static void HandleHostedRelease(void*, uint32_t generation, uint32_t index, bool refused)
{
    {
        std::lock_guard<std::mutex> guard(gHosted.mutex);

        if (generation != gHosted.generation) return;

        if (refused) gHosted.refused = true;
        else gHosted.releasedIndices.push_back(index);
    }

    gHosted.released.notify_all();
}

static void ReclaimReleasedSlots()
{
    std::vector<uint32_t> indices;
    bool refused = false;

    {
        std::lock_guard<std::mutex> guard(gHosted.mutex);
        indices.swap(gHosted.releasedIndices);
        refused = gHosted.refused;
        gHosted.refused = false;
    }

    for (uint32_t index : indices)
    {
        if (index >= gHosted.slotCount || !gHosted.slots[index].held) continue;

        gGbm.releaseBuffer(gHosted.surface, gHosted.slots[index].bo);
        gHosted.slots[index].held = false;
    }

    if (!refused || !gHosted.sharing) return;

    fprintf(stderr, "[gleswsi] the window host cannot show the gpu buffers, so each frame is copied to it instead\n");
    gHosted.sharing = false;

    for (uint32_t index = 0; index < gHosted.slotCount; ++index)
    {
        if (!gHosted.slots[index].held) continue;

        gGbm.releaseBuffer(gHosted.surface, gHosted.slots[index].bo);
        gHosted.slots[index].held = false;
    }

    HostedWindow::ForgetBuffers(*gHosted.session, gHosted.generation);
}

static void TeardownHostedSurface(bool destroyEglSurface)
{
    if (!gHosted.surface) return;

    if (gHosted.session)
    {
        HostedWindow::SetReleaseListener(*gHosted.session, nullptr, nullptr);
        HostedWindow::ForgetBuffers(*gHosted.session, gHosted.generation);
    }

    if (destroyEglSurface && gHosted.eglSurface != EGL_NO_SURFACE && gHostDestroySurface) gHostDestroySurface(gHosted.eglDisplay, gHosted.eglSurface);

    gGbm.destroySurface(gHosted.surface);

    std::lock_guard<std::mutex> guard(gHosted.mutex);
    gHosted.surface = nullptr;
    gHosted.eglSurface = EGL_NO_SURFACE;
    gHosted.slotCount = 0;
    gHosted.generation = 0;
    gHosted.releasedIndices.clear();
    gHosted.refused = false;

    for (HostedSlot& slot : gHosted.slots) slot = HostedSlot{};
}

static std::vector<uint64_t> SharableModifiers(const HostedWindow::Display& display, uint32_t format)
{
    std::vector<uint64_t> modifiers;

    for (const ControlChannel::FormatEntry& entry : display.formats)
    {
        if (entry.format != format || entry.modifier == ControlChannel::MODIFIER_INVALID) continue;
        modifiers.push_back(entry.modifier);
    }

    return modifiers;
}

static EGLSurface CreateHostedSurface(EGLDisplay display, EGLConfig config, const EGLint* attributes)
{
    if (!gHosted.session)
    {
        fprintf(stderr, "[gleswsi] the guest wants a window surface before the hosted window exists\n");
        return EGL_NO_SURFACE;
    }

    TeardownHostedSurface(true);

    EGLint visual = 0;
    if (!gHostGetConfigAttrib || !gHostGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &visual) || visual == 0)
    {
        visual = (EGLint)ControlChannel::FORMAT_XRGB8888;
    }

    uint32_t width = 0;
    uint32_t height = 0;
    HostedWindow::CurrentSize(*gHosted.session, width, height);

    HostedWindow::Display hostDisplay = HostedWindow::CurrentDisplay(*gHosted.session);
    std::vector<uint64_t> modifiers = SharableModifiers(hostDisplay, (uint32_t)visual);

    bool sharing = hostDisplay.dmabufAvailable && HostedWindow::DmabufWanted() && !modifiers.empty();
    gbm_surface* surface = nullptr;

    if (sharing && gGbm.createSurfaceWithModifiers)
    {
        surface = gGbm.createSurfaceWithModifiers(gHosted.device, width, height, (uint32_t)visual, modifiers.data(), (unsigned int)modifiers.size(),
                                                  GBM_USE_RENDERING);
    }

    if (!surface)
    {
        uint32_t flags = GBM_USE_RENDERING;
        if (sharing) flags |= GBM_USE_LINEAR;

        surface = gGbm.createSurface(gHosted.device, width, height, (uint32_t)visual, flags);
    }

    if (!surface)
    {
        fprintf(stderr, "[gleswsi] gbm cannot make a %ux%u surface of format %08x for the guest\n", width, height, (uint32_t)visual);
        return EGL_NO_SURFACE;
    }

    EGLSurface created = gHostCreateWindowSurface(display, config, (EGLNativeWindowType)surface, attributes);
    if (created == EGL_NO_SURFACE)
    {
        gGbm.destroySurface(surface);
        return EGL_NO_SURFACE;
    }

    uint32_t generation = HostedWindow::ReserveGeneration(*gHosted.session);

    {
        std::lock_guard<std::mutex> guard(gHosted.mutex);
        gHosted.surface = surface;
        gHosted.eglSurface = created;
        gHosted.eglDisplay = display;
        gHosted.width = width;
        gHosted.height = height;
        gHosted.format = (uint32_t)visual;
        gHosted.sharing = sharing;
        gHosted.generation = generation;
    }

    HostedWindow::SetReleaseListener(*gHosted.session, &HandleHostedRelease, nullptr);
    LoadFenceFunctions(display);

    gStatistics.surfacesCreated += 1;
    fprintf(stderr, "[gleswsi] the guest draws a %ux%u gpu surface whose frames are %s the window host\n", width, height,
            sharing ? "shared with" : "copied to");

    return created;
}

static uint32_t SlotFor(gbm_bo* bo)
{
    for (uint32_t index = 0; index < gHosted.slotCount; ++index)
    {
        if (gHosted.slots[index].bo == bo) return index;
    }

    if (gHosted.slotCount >= ControlChannel::MAX_FRAME_BUFFERS) return HostedWindow::INVALID_BUFFER_INDEX;

    int planes = gGbm.planeCount(bo);
    if (planes <= 0 || planes > (int)ControlChannel::MAX_PLANES) return HostedWindow::INVALID_BUFFER_INDEX;

    HostedWindow::BufferDescription description{};
    description.generation = gHosted.generation;
    description.index = gHosted.slotCount;
    description.kind = ControlChannel::BufferKind::Dmabuf;
    description.width = gGbm.width(bo);
    description.height = gGbm.height(bo);
    description.format = gGbm.format(bo);
    description.modifier = gGbm.modifier(bo);
    description.planeCount = (uint32_t)planes;

    int descriptors[ControlChannel::MAX_PLANES] = {-1, -1, -1, -1};
    bool exported = true;

    for (int plane = 0; plane < planes; ++plane)
    {
        descriptors[plane] = gGbm.descriptorForPlane(bo, plane);
        description.offsets[plane] = gGbm.offset(bo, plane);
        description.strides[plane] = gGbm.strideForPlane(bo, plane);
        if (descriptors[plane] < 0) exported = false;
    }

    HostedWindow::Result registered = exported ? HostedWindow::RegisterBuffer(*gHosted.session, description, descriptors, (uint32_t)planes)
                                               : HostedWindow::Result::InvalidArgument;

    for (int plane = 0; plane < planes; ++plane)
    {
        if (descriptors[plane] >= 0) close(descriptors[plane]);
    }

    if (registered != HostedWindow::Result::Ok) return HostedWindow::INVALID_BUFFER_INDEX;

    gHosted.slots[gHosted.slotCount].bo = bo;
    gHosted.slots[gHosted.slotCount].held = false;
    gHosted.slotCount += 1;

    return gHosted.slotCount - 1;
}

static bool ShareFrame(gbm_bo* bo, int fence)
{
    uint32_t index = SlotFor(bo);

    if (index == HostedWindow::INVALID_BUFFER_INDEX)
    {
        if (fence >= 0) close(fence);
        return false;
    }

    gHosted.slots[index].held = true;
    HostedWindow::PresentBuffer(*gHosted.session, gHosted.generation, index, fence);

    if (fence >= 0) close(fence);

    gStatistics.framesShared += 1;
    return true;
}

static void CopyRow(uint8_t* destination, const uint8_t* source, uint32_t pixels, bool swapRedBlue)
{
    if (!swapRedBlue)
    {
        memcpy(destination, source, (size_t)pixels * 4);
        return;
    }

    for (uint32_t pixel = 0; pixel < pixels; ++pixel)
    {
        destination[pixel * 4 + 0] = source[pixel * 4 + 2];
        destination[pixel * 4 + 1] = source[pixel * 4 + 1];
        destination[pixel * 4 + 2] = source[pixel * 4 + 0];
        destination[pixel * 4 + 3] = source[pixel * 4 + 3];
    }
}

static void CopyFrame(gbm_bo* bo)
{
    HostedWindow::Session& session = *gHosted.session;
    HostedWindow::Buffer* buffer = nullptr;
    uint32_t index = HostedWindow::INVALID_BUFFER_INDEX;

    if (HostedWindow::AcquireBuffer(session, buffer, index) != HostedWindow::Result::Ok) return;

    uint32_t sourceStride = 0;
    void* mapping = nullptr;
    uint32_t width = gGbm.width(bo);
    uint32_t height = gGbm.height(bo);
    const uint8_t* source = (const uint8_t*)gGbm.map(bo, 0, 0, width, height, GBM_TRANSFER_READ, &sourceStride, &mapping);

    if (source)
    {
        uint32_t rows = height < session.height ? height : session.height;
        uint32_t columns = width < session.width ? width : session.width;
        uint32_t format = gGbm.format(bo);
        bool swapRedBlue = format == ControlChannel::FORMAT_ABGR8888 || format == ControlChannel::FORMAT_XBGR8888;

        for (uint32_t row = 0; row < rows; ++row)
        {
            CopyRow(buffer->pixels + (size_t)row * session.stride, source + (size_t)row * sourceStride, columns, swapRedBlue);
        }

        gGbm.unmap(bo, mapping);
        gStatistics.framesCopied += 1;
    }

    HostedWindow::Present(session, index, 0);
}

static void WaitForFreeBuffer()
{
    while (!gGbm.hasFreeBuffers(gHosted.surface))
    {
        bool woke = false;

        {
            std::unique_lock<std::mutex> guard(gHosted.mutex);
            woke = gHosted.released.wait_for(guard, std::chrono::milliseconds(GlesWsi::RELEASE_TIMEOUT_MILLIS),
                                             [] { return !gHosted.releasedIndices.empty() || gHosted.refused; });
        }

        ReclaimReleasedSlots();

        if (!woke) return;
    }
}

static bool PacingWanted()
{
    static const bool unpaced = []() {
        const char* setting = Settings::Lookup(GlesWsi::NO_VSYNC_SETTING);
        return setting && strcmp(setting, "0") != 0;
    }();

    return !unpaced && gHosted.swapInterval > 0;
}

static EGLBoolean SwapHostedBuffers(EGLDisplay display, EGLSurface surface)
{
    FrameRate::Tick();

    EGLSyncKHR sync = EGL_NO_SYNC_KHR;

    if (gHosted.sharing && gHosted.nativeFences)
    {
        const EGLint attributes[] = {EGL_SYNC_NATIVE_FENCE_FD_ANDROID, EGL_NO_NATIVE_FENCE_FD_ANDROID, EGL_NONE};
        sync = gHostCreateSync(display, EGL_SYNC_NATIVE_FENCE_ANDROID, attributes);
    }

    EGLBoolean swapped = gHostSwapBuffers(display, surface);

    int fence = -1;
    if (sync != EGL_NO_SYNC_KHR)
    {
        if (swapped == EGL_TRUE) fence = gHostDupNativeFence(display, sync);
        gHostDestroySync(display, sync);
    }

    if (swapped != EGL_TRUE)
    {
        if (fence >= 0) close(fence);
        return swapped;
    }

    gStatistics.framesPresented += 1;
    ReclaimReleasedSlots();

    gbm_bo* bo = gGbm.lockFrontBuffer(gHosted.surface);
    if (!bo)
    {
        if (fence >= 0) close(fence);
        return swapped;
    }

    uint64_t completed = HostedWindow::PresentsCompleted(*gHosted.session);

    if (!gHosted.sharing || !ShareFrame(bo, fence))
    {
        CopyFrame(bo);
        gGbm.releaseBuffer(gHosted.surface, bo);
    }

    WaitForFreeBuffer();

    if (PacingWanted()) HostedWindow::WaitForPresentAfter(*gHosted.session, completed, GlesWsi::PACING_TIMEOUT_MILLIS);

    return swapped;
}

static bool FrameTraceRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(GlesWsi::FRAME_TRACE_SETTING);
        return setting && strcmp(setting, "0") != 0;
    }();

    return requested;
}

template <typename Type>
static AbiBridge::Argument ArgumentFor()
{
    AbiBridge::Argument argument;

    if constexpr (std::is_floating_point<Type>::value)
    {
        argument.argumentClass = sizeof(Type) == sizeof(float) ? AbiBridge::ArgumentClass::Float : AbiBridge::ArgumentClass::Double;
        argument.sizeInBytes = (uint32_t)sizeof(Type);
        argument.alignmentInBytes = (uint32_t)sizeof(Type);
        return argument;
    }
    else
    {
        argument.argumentClass = std::is_pointer<Type>::value ? AbiBridge::ArgumentClass::Pointer : AbiBridge::ArgumentClass::Integer;
        argument.sizeInBytes = (uint32_t)sizeof(uint64_t);
        argument.alignmentInBytes = (uint32_t)sizeof(uint64_t);
        return argument;
    }
}

template <typename Function>
struct SignatureBuilder;

template <typename Returned, typename... Arguments>
struct SignatureBuilder<Returned (*)(Arguments...)>
{
    static constexpr int argumentCount = (int)sizeof...(Arguments);
    static constexpr bool argumentsFitRegisters = (... && (sizeof(Arguments) <= sizeof(uint64_t)));
    static constexpr int floatCount = (0 + ... + (std::is_floating_point<Arguments>::value ? 1 : 0));
    static constexpr int integerCount = argumentCount - floatCount;

    static AbiBridge::Signature Build()
    {
        AbiBridge::Signature signature;
        int index = 0;

        ((signature.arguments[index++] = ArgumentFor<Arguments>()), ...);

        signature.totalArgumentCount = (uint8_t)argumentCount;
        signature.namedArgumentCount = (uint8_t)argumentCount;

        if constexpr (!std::is_void<Returned>::value) signature.returnValue = ArgumentFor<Returned>();

        return signature;
    }
};

static bool RegisterDerivedEntryPoint(AbiBridge::Registry& registry, const char* name, void* hostFunction,
                                      const AbiBridge::Signature& signature)
{
    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, name, signature, hostFunction);

    if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
    {
        fprintf(stderr, "[gleswsi] cannot forward %s: %s\n", name, AbiBridge::ResultName(registered));
        return false;
    }

    if (registered == AbiBridge::Result::Ok) gStatistics.entryPointsRegistered += 1;
    return true;
}

static AbiBridge::Signature GenericForwardingSignature(int argumentCount)
{
    AbiBridge::Argument word;
    word.argumentClass = AbiBridge::ArgumentClass::Integer;
    word.sizeInBytes = sizeof(uint64_t);
    word.alignmentInBytes = sizeof(uint64_t);

    AbiBridge::Signature signature;
    for (int index = 0; index < argumentCount; ++index) signature.arguments[index] = word;

    signature.totalArgumentCount = (uint8_t)argumentCount;
    signature.namedArgumentCount = (uint8_t)argumentCount;
    signature.returnValue = word;

    return signature;
}

static void* ResolveFromDrivers(const char* name)
{
    void* found = nullptr;

    if (gGlesDriver) found = dlsym(gGlesDriver, name);
    if (!found && gEglDriver) found = dlsym(gEglDriver, name);
    if (!found && gHostGetProcAddress) found = gHostGetProcAddress(name);

    return found;
}

static EGLDisplay CurrentHostDisplay()
{
    if (gHostDisplay != EGL_NO_DISPLAY) return gHostDisplay;

    if (HostedWindow::IsRequested())
    {
        gHostDisplay = HostedDisplay();
        return gHostDisplay;
    }

    if (!gDisplay)
    {
        fprintf(stderr, "[gleswsi] the guest asked for a display before one was attached, so there is nothing to draw on\n");
        return EGL_NO_DISPLAY;
    }

    if (gHostGetPlatformDisplay) gHostDisplay = gHostGetPlatformDisplay(EGL_PLATFORM_WAYLAND_KHR, gDisplay, nullptr);
    if (gHostDisplay == EGL_NO_DISPLAY && gHostGetDisplay) gHostDisplay = gHostGetDisplay((EGLNativeDisplayType)gDisplay);

    if (gHostDisplay == EGL_NO_DISPLAY) fprintf(stderr, "[gleswsi] the host driver has no display for this compositor connection\n");

    return gHostDisplay;
}

static EGLDisplay GetDisplayForGuest(EGLNativeDisplayType)
{
    return CurrentHostDisplay();
}

static EGLDisplay GetPlatformDisplayForGuest(EGLenum, void*, const EGLAttrib*)
{
    return CurrentHostDisplay();
}

static EGLSurface CreateWindowSurfaceForGuest(EGLDisplay display, EGLConfig config, EGLNativeWindowType, const EGLint* attributes)
{
    if (!gHostCreateWindowSurface) return EGL_NO_SURFACE;

    if (gHosted.device) return CreateHostedSurface(display, config, attributes);

    if (!gSurface || !gWindowCreate)
    {
        fprintf(stderr, "[gleswsi] there is no compositor surface to make a window surface from\n");
        return EGL_NO_SURFACE;
    }

    if (!gEglWindow)
    {
        gEglWindow = gWindowCreate(gSurface, (int)gSurfaceWidth, (int)gSurfaceHeight);

        if (!gEglWindow)
        {
            fprintf(stderr, "[gleswsi] the compositor surface could not be made into a window the driver can draw into\n");
            return EGL_NO_SURFACE;
        }

        fprintf(stderr, "[gleswsi] the guest draws into the compositor surface at %ux%u, ignoring the window it passed in\n",
                gSurfaceWidth, gSurfaceHeight);
    }

    EGLSurface created = gHostCreateWindowSurface(display, config, (EGLNativeWindowType)gEglWindow, attributes);
    if (created != EGL_NO_SURFACE) gStatistics.surfacesCreated += 1;

    return created;
}

static EGLSurface CreatePlatformWindowSurfaceForGuest(EGLDisplay display, EGLConfig config, void* window, const EGLAttrib* attributes)
{
    EGLint converted[MAX_ATTRIBUTE_PAIRS * 2 + 1];
    int count = 0;

    for (int pair = 0; attributes && attributes[pair * 2] != EGL_NONE && pair < MAX_ATTRIBUTE_PAIRS; ++pair)
    {
        converted[count++] = (EGLint)attributes[pair * 2];
        converted[count++] = (EGLint)attributes[pair * 2 + 1];
    }

    converted[count] = EGL_NONE;

    return CreateWindowSurfaceForGuest(display, config, (EGLNativeWindowType)window, converted);
}

static EGLBoolean DestroySurfaceForGuest(EGLDisplay display, EGLSurface surface)
{
    if (!gHostDestroySurface) return EGL_FALSE;

    EGLBoolean destroyed = gHostDestroySurface(display, surface);

    if (gHosted.surface && surface == gHosted.eglSurface) TeardownHostedSurface(false);

    return destroyed;
}

static EGLBoolean SwapIntervalForGuest(EGLDisplay display, EGLint interval)
{
    gHosted.swapInterval = interval;

    return gHostSwapInterval ? gHostSwapInterval(display, interval) : EGL_TRUE;
}

static uint64_t MonotonicNanos()
{
    struct timespec now = {};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static EGLBoolean SwapBuffersForGuest(EGLDisplay display, EGLSurface surface)
{
    if (!gHostSwapBuffers) return EGL_FALSE;

    EGLBoolean swapped = EGL_FALSE;

    if (gHosted.surface && surface == gHosted.eglSurface)
    {
        swapped = SwapHostedBuffers(display, surface);
    }
    else
    {
        FrameRate::Tick();
        swapped = gHostSwapBuffers(display, surface);
        if (swapped == EGL_TRUE) gStatistics.framesPresented += 1;
    }

    if (swapped != EGL_TRUE || !FrameTraceRequested()) return swapped;

    static uint64_t reportedAt = 0;
    static uint64_t reportedFrames = 0;

    uint64_t now = MonotonicNanos();
    if (reportedAt == 0) reportedAt = now;

    if (now - reportedAt >= 1000000000ull)
    {
        fprintf(stderr, "[gleswsi] %llu frame(s) in the last second\n", (unsigned long long)(gStatistics.framesPresented - reportedFrames));
        reportedFrames = gStatistics.framesPresented;
        reportedAt = now;
    }

    return swapped;
}

static const GLubyte* CopyStringForGuest(const char* text)
{
    if (!text) return nullptr;
    if ((uint64_t)gStringArena == 0) return (const GLubyte*)text;

    std::string key(text);

    auto found = gStringAddresses.find(key);
    if (found != gStringAddresses.end()) return (const GLubyte*)found->second;

    uint64_t needed = key.size() + 1;
    if (gStringArenaUsed + needed > GlesWsi::STRING_ARENA_BYTES)
    {
        fprintf(stderr, "[gleswsi] no room left to hand the guest the string \"%s\"\n", text);
        return nullptr;
    }

    uint64_t address = (uint64_t)gStringArena + gStringArenaUsed;
    memcpy((void*)address, key.c_str(), needed);
    gStringArenaUsed += needed;

    gStringAddresses.emplace(std::move(key), address);
    gStatistics.stringsCopied += 1;

    return (const GLubyte*)address;
}

static const GLubyte* GetStringForGuest(GLenum name)
{
    if (!gHostGetString) return nullptr;

    const GLubyte* reported = gHostGetString(name);

    if (name == GL_RENDERER) return CopyStringForGuest(Platform::DEVICE_GPU);
    if (name == GL_VENDOR) return CopyStringForGuest(Platform::DEVICE_GPU_VENDOR);

    return CopyStringForGuest((const char*)reported);
}

static const GLubyte* GetStringIndexedForGuest(GLenum name, GLuint index)
{
    if (!gHostGetStringi) return nullptr;

    return CopyStringForGuest((const char*)gHostGetStringi(name, index));
}

static AbiBridge::Result RefuseGuestCallback(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    static std::once_flag reported;
    std::call_once(reported, []() {
        fprintf(stderr, "[gleswsi] the guest asked to be called back from the driver, which cannot reach guest code, so it is refused\n");
    });

    gStatistics.guestCallbacksRefused += 1;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ProcAddressForGuest(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t nameAddress = registers.general[0];
    registers.general[0] = 0;

    if (nameAddress == 0 || !gRegistry) return AbiBridge::Result::Ok;

    char name[GlesWsi::MAX_ENTRY_POINT_NAME];
    uint64_t length = 0;

    for (;;)
    {
        char character = 0;
        if (!memory.Read(memory.context, (GuestState::GuestAddress)(nameAddress + length), &character, sizeof(character)))
            return AbiBridge::Result::MalformedArguments;

        name[length] = character;
        if (character == '\0') break;

        length += 1;
        if (length + 1 >= sizeof(name)) return AbiBridge::Result::MalformedArguments;
    }

    GuestState::GuestAddress stub{};
    if (AbiBridge::LookupGuestStub(*gRegistry, name, stub) == AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)stub;
        return AbiBridge::Result::Ok;
    }

    void* forwarded = ResolveFromDrivers(name);

    if (!forwarded)
    {
        fprintf(stderr, "[gleswsi] the guest asked for %s, which the host driver does not have\n", name);
        return AbiBridge::Result::Ok;
    }

    fprintf(stderr, "[gleswsi] %s is forwarded without a known prototype, so a floating point argument would be misread\n", name);

    if (!RegisterDerivedEntryPoint(*gRegistry, name, forwarded, GenericForwardingSignature(8))) return AbiBridge::Result::Ok;
    if (AbiBridge::LookupGuestStub(*gRegistry, name, stub) != AbiBridge::Result::Ok) return AbiBridge::Result::Ok;

    registers.general[0] = (uint64_t)stub;
    return AbiBridge::Result::Ok;
}

static bool LoadHostDrivers()
{
    gEglDriver = dlopen(GlesWsi::EGL_SONAME, RTLD_NOW | RTLD_LOCAL);
    if (!gEglDriver) return false;

    gGlesDriver = dlopen(GlesWsi::GLES_SONAME, RTLD_NOW | RTLD_LOCAL);
    if (!gGlesDriver) return false;

    gWaylandEgl = dlopen(GlesWsi::WAYLAND_EGL_SONAME, RTLD_NOW | RTLD_LOCAL);
    if (!gWaylandEgl) return false;

    gWindowCreate = (WaylandEglWindowCreate)dlsym(gWaylandEgl, "wl_egl_window_create");
    gWindowResize = (WaylandEglWindowResize)dlsym(gWaylandEgl, "wl_egl_window_resize");
    gWindowDestroy = (WaylandEglWindowDestroy)dlsym(gWaylandEgl, "wl_egl_window_destroy");

    gHostGetPlatformDisplay = (EGLDisplay(*)(EGLenum, void*, const EGLAttrib*))dlsym(gEglDriver, "eglGetPlatformDisplay");
    gHostGetDisplay = (EGLDisplay(*)(EGLNativeDisplayType))dlsym(gEglDriver, "eglGetDisplay");
    gHostCreateWindowSurface =
        (EGLSurface(*)(EGLDisplay, EGLConfig, EGLNativeWindowType, const EGLint*))dlsym(gEglDriver, "eglCreateWindowSurface");
    gHostSwapBuffers = (EGLBoolean(*)(EGLDisplay, EGLSurface))dlsym(gEglDriver, "eglSwapBuffers");
    gHostGetProcAddress = (void* (*)(const char*))dlsym(gEglDriver, "eglGetProcAddress");
    gHostGetString = (const GLubyte* (*)(GLenum))dlsym(gGlesDriver, "glGetString");
    gHostGetStringi = (const GLubyte* (*)(GLenum, GLuint))dlsym(gGlesDriver, "glGetStringi");
    gHostGetConfigAttrib = (EGLBoolean(*)(EGLDisplay, EGLConfig, EGLint, EGLint*))dlsym(gEglDriver, "eglGetConfigAttrib");
    gHostDestroySurface = (EGLBoolean(*)(EGLDisplay, EGLSurface))dlsym(gEglDriver, "eglDestroySurface");
    gHostSwapInterval = (EGLBoolean(*)(EGLDisplay, EGLint))dlsym(gEglDriver, "eglSwapInterval");
    gHostQueryString = (const char* (*)(EGLDisplay, EGLint))dlsym(gEglDriver, "eglQueryString");

    return gWindowCreate && gHostCreateWindowSurface && gHostSwapBuffers && gHostGetString;
}

static void RegisterInterposedEntryPoints(AbiBridge::Registry& registry)
{
    RegisterDerivedEntryPoint(registry, "eglGetDisplay", (void*)&GetDisplayForGuest, SignatureBuilder<decltype(&GetDisplayForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglGetPlatformDisplay", (void*)&GetPlatformDisplayForGuest,
                              SignatureBuilder<decltype(&GetPlatformDisplayForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglCreateWindowSurface", (void*)&CreateWindowSurfaceForGuest,
                              SignatureBuilder<decltype(&CreateWindowSurfaceForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglCreatePlatformWindowSurface", (void*)&CreatePlatformWindowSurfaceForGuest,
                              SignatureBuilder<decltype(&CreatePlatformWindowSurfaceForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglDestroySurface", (void*)&DestroySurfaceForGuest,
                              SignatureBuilder<decltype(&DestroySurfaceForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglSwapInterval", (void*)&SwapIntervalForGuest, SignatureBuilder<decltype(&SwapIntervalForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglSwapBuffers", (void*)&SwapBuffersForGuest, SignatureBuilder<decltype(&SwapBuffersForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "glGetString", (void*)&GetStringForGuest, SignatureBuilder<decltype(&GetStringForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "glGetStringi", (void*)&GetStringIndexedForGuest,
                              SignatureBuilder<decltype(&GetStringIndexedForGuest)>::Build());

    AbiBridge::RegisterHostHandler(registry, "eglGetProcAddress", &ProcAddressForGuest, nullptr);
    AbiBridge::RegisterHostHandler(registry, "glDebugMessageCallback", &RefuseGuestCallback, nullptr);
}

GlesWsi::Result GlesWsi::RegisterAll(GuestRuntime::Instance& runtime)
{
    if (!LoadHostDrivers())
    {
        fprintf(stderr, "[gleswsi] no host OpenGL ES driver, so the guest cannot render with it\n");
        return Result::NoDriver;
    }

    gRuntime = &runtime;
    gRegistry = &GuestRuntime::BridgeRegistry(runtime);
    AbiBridge::Registry& registry = *gRegistry;

    if (GuestRuntime::RegisterDataObject(runtime, "acl:glstrings", STRING_ARENA_BYTES, gStringArena) != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "[gleswsi] no guest memory for the strings the driver reports, so they cannot be handed over\n");
        return Result::NoStringArena;
    }

    RegisterInterposedEntryPoints(registry);

#define FORWARD_ENTRY_POINT(name)                                                                                                     \
    {                                                                                                                                 \
        using FunctionType = decltype(&name);                                                                                         \
        static_assert(SignatureBuilder<FunctionType>::argumentsFitRegisters, #name " passes an argument wider than a register");       \
        static_assert(SignatureBuilder<FunctionType>::integerCount <= 16, #name " passes more integers than the bridge can carry");    \
        static_assert(SignatureBuilder<FunctionType>::floatCount <= 8, #name " passes more floats than the bridge can carry");         \
        void* hostFunction = ResolveFromDrivers(#name);                                                                               \
        if (hostFunction) RegisterDerivedEntryPoint(registry, #name, hostFunction, SignatureBuilder<FunctionType>::Build());           \
    }

    RUNTIME_GLES_ENTRY_POINTS(FORWARD_ENTRY_POINT)
    RUNTIME_EGL_ENTRY_POINTS(FORWARD_ENTRY_POINT)

#undef FORWARD_ENTRY_POINT

    fprintf(stderr, "[gleswsi] %llu OpenGL ES entry points forwarded to the host driver\n",
            (unsigned long long)gStatistics.entryPointsRegistered);

    return Result::Ok;
}

void GlesWsi::Shutdown()
{
    if (gEglWindow && gWindowDestroy) gWindowDestroy(gEglWindow);

    TeardownHostedSurface(true);
    gHosted.session = nullptr;

    gEglWindow = nullptr;
    gHostDisplay = EGL_NO_DISPLAY;
    gDisplay = nullptr;
    gSurface = nullptr;
    gRegistry = nullptr;
    gRuntime = nullptr;
}

void GlesWsi::AttachSurfaceTarget(wl_display* display, wl_surface* surface, uint32_t width, uint32_t height)
{
    gDisplay = display;
    gSurface = surface;
    gSurfaceWidth = width;
    gSurfaceHeight = height;
}

void GlesWsi::AttachHostedTarget(HostedWindow::Session* session, uint32_t width, uint32_t height)
{
    gHosted.session = session;
    gHosted.width = width;
    gHosted.height = height;
}

void GlesWsi::ResizeSurfaceTarget(uint32_t width, uint32_t height)
{
    gSurfaceWidth = width;
    gSurfaceHeight = height;

    if (gEglWindow && gWindowResize) gWindowResize(gEglWindow, (int)width, (int)height, 0, 0);
}

bool GlesWsi::HasSurfaceTarget()
{
    return (gDisplay != nullptr && gSurface != nullptr) || gHosted.session != nullptr;
}

GlesWsi::Statistics GlesWsi::GetStatistics()
{
    return gStatistics;
}

const char* GlesWsi::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok:
            return "Ok";
        case Result::NoDriver:
            return "NoDriver";
        case Result::MissingEntryPoint:
            return "MissingEntryPoint";
        case Result::RegistrationFailed:
            return "RegistrationFailed";
        case Result::NoSurfaceTarget:
            return "NoSurfaceTarget";
        case Result::NoStringArena:
            return "NoStringArena";
    }

    return "Unknown";
}

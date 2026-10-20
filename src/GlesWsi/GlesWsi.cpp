#include "GlesWsi/GlesWsi.hpp"

#define EGL_NO_X11 1

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl32.h>

#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <string>
#include <type_traits>
#include <unordered_map>

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

static uint64_t MonotonicNanos()
{
    struct timespec now = {};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static EGLBoolean SwapBuffersForGuest(EGLDisplay display, EGLSurface surface)
{
    if (!gHostSwapBuffers) return EGL_FALSE;

    FrameRate::Tick();

    EGLBoolean swapped = gHostSwapBuffers(display, surface);
    if (swapped != EGL_TRUE) return swapped;

    gStatistics.framesPresented += 1;

    if (!FrameTraceRequested()) return swapped;

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

    return gWindowCreate && gHostCreateWindowSurface && gHostSwapBuffers && gHostGetString;
}

static void RegisterInterposedEntryPoints(AbiBridge::Registry& registry)
{
    RegisterDerivedEntryPoint(registry, "eglGetDisplay", (void*)&GetDisplayForGuest, SignatureBuilder<decltype(&GetDisplayForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglGetPlatformDisplay", (void*)&GetPlatformDisplayForGuest,
                              SignatureBuilder<decltype(&GetPlatformDisplayForGuest)>::Build());
    RegisterDerivedEntryPoint(registry, "eglCreateWindowSurface", (void*)&CreateWindowSurfaceForGuest,
                              SignatureBuilder<decltype(&CreateWindowSurfaceForGuest)>::Build());
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

void GlesWsi::ResizeSurfaceTarget(uint32_t width, uint32_t height)
{
    gSurfaceWidth = width;
    gSurfaceHeight = height;

    if (gEglWindow && gWindowResize) gWindowResize(gEglWindow, (int)width, (int)height, 0, 0);
}

bool GlesWsi::HasSurfaceTarget()
{
    return gDisplay != nullptr && gSurface != nullptr;
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

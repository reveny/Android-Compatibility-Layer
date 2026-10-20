#include "VulkanWsi/VulkanWsi.hpp"

#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <type_traits>
#include <vector>

#define VK_USE_PLATFORM_WAYLAND_KHR
#include <vulkan/vulkan.h>

#include "AbiBridge/AbiBridge.hpp"
#include "Overlay/FrameRate.hpp"
#include "Overlay/OverlayVulkan.hpp"
#include "Platform/DeviceIdentity.hpp"
#include "Settings/Settings.hpp"

struct GuestAndroidSurfaceCreateInfo
{
    VkStructureType sType;
    const void* pNext;
    uint32_t flags;
    uint64_t window;
};

static void* gDriver = nullptr;
static wl_display* gDisplay = nullptr;
static wl_surface* gSurface = nullptr;
static std::mutex gStatisticsMutex;
static VulkanWsi::Statistics gStatistics;
static std::vector<uint64_t> gPresentTimestamps;

static PFN_vkCreateInstance gHostCreateInstance = nullptr;
static PFN_vkGetPhysicalDeviceProperties gHostGetPhysicalDeviceProperties = nullptr;
static PFN_vkCreateWaylandSurfaceKHR gHostCreateWaylandSurface = nullptr;
static PFN_vkGetInstanceProcAddr gHostGetInstanceProcAddr = nullptr;
static PFN_vkGetDeviceProcAddr gHostGetDeviceProcAddr = nullptr;
static VkInstance gHostInstance = VK_NULL_HANDLE;
static PFN_vkCreateSwapchainKHR gHostCreateSwapchain = nullptr;
static PFN_vkGetPhysicalDeviceSurfaceFormatsKHR gHostGetSurfaceFormats = nullptr;
static PFN_vkDestroySurfaceKHR gHostDestroySurface = nullptr;
static VkSurfaceKHR gSharedSurface = VK_NULL_HANDLE;
static uint32_t gSharedSurfaceHolders = 0;
static PFN_vkQueuePresentKHR gHostQueuePresent = nullptr;
static PFN_vkCreateDevice gHostCreateDevice = nullptr;
static PFN_vkGetDeviceQueue gHostGetDeviceQueue = nullptr;
static PFN_vkDestroySwapchainKHR gHostDestroySwapchain = nullptr;

static uint64_t gFrameWindowStart = 0;
static uint64_t gFrameWindowCount = 0;

static void CountForwardedCall()
{
    std::lock_guard<std::mutex> guard(gStatisticsMutex);
    gStatistics.callsForwarded += 1;
}

static bool PresentWithoutWaitingRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(VulkanWsi::PRESENT_WITHOUT_WAITING_SETTING);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

static bool FrameTracingRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(VulkanWsi::FRAME_TRACE_SETTING);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

template <typename Signature>
struct EntryPointTraits;

template <typename Returned, typename... Arguments>
struct EntryPointTraits<Returned (*)(Arguments...)>
{
    static constexpr int argumentCount = (int)sizeof...(Arguments);
    static constexpr bool returnsValue = !std::is_void<Returned>::value;
    static constexpr bool argumentsFitRegisters = (... && (sizeof(Arguments) <= sizeof(uint64_t)));
    static constexpr bool argumentsAreIntegral = (... && !std::is_floating_point<Arguments>::value);
};

static AbiBridge::Signature MakeForwardingSignature(int argumentCount, bool returnsValue)
{
    AbiBridge::Argument word;
    word.argumentClass = AbiBridge::ArgumentClass::Integer;
    word.sizeInBytes = sizeof(uint64_t);
    word.alignmentInBytes = sizeof(uint64_t);

    AbiBridge::Signature signature;
    for (int index = 0; index < argumentCount; ++index) signature.arguments[index] = word;

    signature.totalArgumentCount = (uint8_t)argumentCount;
    signature.namedArgumentCount = (uint8_t)argumentCount;

    if (returnsValue)
    {
        signature.returnValue.argumentClass = AbiBridge::ArgumentClass::Integer;
        signature.returnValue.sizeInBytes = sizeof(uint64_t);
        signature.returnValue.alignmentInBytes = sizeof(uint64_t);
    }

    return signature;
}

static constexpr uint32_t GUEST_RENDERER_VENDOR = 0x00010f00u;
static constexpr uint32_t GUEST_RENDERER_DEVICE = 0x00000001u;
static constexpr uint32_t GUEST_RENDERER_DRIVER_VERSION = (1u << 22) | (0u << 12) | 0u;

static constexpr uint64_t MAX_ENTRY_POINT_NAME = 128;
static constexpr int FORWARDED_ARGUMENT_SLOTS = 8;

static AbiBridge::Registry* gRegistry = nullptr;

struct FloatingPointEntryPoint
{
    const char* name;
    int leadingIntegerArguments;
    int floatArguments;
};

static bool DescribeFloatingPointEntryPoint(const char* name, FloatingPointEntryPoint& described)
{
    static const FloatingPointEntryPoint KNOWN[] = {
        {"vkCmdSetLineWidth", 1, 1},   {"vkCmdSetDepthBias", 1, 3},    {"vkCmdSetDepthBiasEXT", 1, 3},
        {"vkCmdSetDepthBounds", 1, 2}, {"vkCmdSetBlendConstants", 1, 0},
    };

    for (const FloatingPointEntryPoint& entry : KNOWN)
    {
        if (strcmp(entry.name, name) != 0) continue;
        if (entry.floatArguments == 0) return false;

        described = entry;
        return true;
    }

    return false;
}

static bool RegisterForwardedEntryPoint(AbiBridge::Registry& registry, const char* name, void* hostFunction, int argumentCount,
                                        bool returnsValue);

static bool RegisterFloatingPointEntryPoint(AbiBridge::Registry& registry, const char* name, void* hostFunction,
                                            const FloatingPointEntryPoint& described)
{
    AbiBridge::Argument word;
    word.argumentClass = AbiBridge::ArgumentClass::Integer;
    word.sizeInBytes = sizeof(uint64_t);
    word.alignmentInBytes = sizeof(uint64_t);

    AbiBridge::Argument single;
    single.argumentClass = AbiBridge::ArgumentClass::Float;
    single.sizeInBytes = sizeof(float);
    single.alignmentInBytes = sizeof(float);

    AbiBridge::Signature signature;
    int index = 0;

    for (int leading = 0; leading < described.leadingIntegerArguments; ++leading) signature.arguments[index++] = word;
    for (int floating = 0; floating < described.floatArguments; ++floating) signature.arguments[index++] = single;

    signature.totalArgumentCount = (uint8_t)index;
    signature.namedArgumentCount = (uint8_t)index;

    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, name, signature, hostFunction);

    if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
    {
        fprintf(stderr, "[vulkanwsi] cannot forward %s: %s\n", name, AbiBridge::ResultName(registered));
        return false;
    }

    return true;
}

static void* ResolveFromDriver(const char* name, bool forDevice, uint64_t handle)
{
    void* found = nullptr;

    if (forDevice && gHostGetDeviceProcAddr) found = (void*)gHostGetDeviceProcAddr((VkDevice)handle, name);
    if (!found && !forDevice && gHostGetInstanceProcAddr) found = (void*)gHostGetInstanceProcAddr((VkInstance)handle, name);

    if (!found && forDevice && gHostGetInstanceProcAddr && gHostInstance != VK_NULL_HANDLE)
        found = (void*)gHostGetInstanceProcAddr(gHostInstance, name);

    if (!found) found = dlsym(gDriver, name);

    return found;
}

static bool WithoutPromotedSuffix(const char* name, char* promoted)
{
    size_t length = strlen(name);
    if (length < 4 || length >= MAX_ENTRY_POINT_NAME) return false;

    if (strcmp(name + length - 3, "KHR") != 0) return false;

    memcpy(promoted, name, length - 3);
    promoted[length - 3] = '\0';
    return true;
}

static AbiBridge::Result ProcAddressForGuest(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    bool forDevice = context != nullptr;
    uint64_t dispatchHandle = registers.general[0];
    uint64_t nameAddress = registers.general[1];
    registers.general[0] = 0;

    if (nameAddress == 0 || !gRegistry) return AbiBridge::Result::Ok;

    char name[MAX_ENTRY_POINT_NAME];
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

    void* forwarded = ResolveFromDriver(name, forDevice, dispatchHandle);

    if (!forwarded)
    {
        char promoted[MAX_ENTRY_POINT_NAME];
        if (WithoutPromotedSuffix(name, promoted)) forwarded = ResolveFromDriver(promoted, forDevice, dispatchHandle);
    }

    if (!forwarded)
    {
        fprintf(stderr, "[vulkanwsi] the guest asked for %s, which the host driver does not have\n", name);
        return AbiBridge::Result::Ok;
    }

    FloatingPointEntryPoint described{};

    if (DescribeFloatingPointEntryPoint(name, described))
    {
        if (!RegisterFloatingPointEntryPoint(*gRegistry, name, forwarded, described)) return AbiBridge::Result::Ok;
    }
    else if (!RegisterForwardedEntryPoint(*gRegistry, name, forwarded, FORWARDED_ARGUMENT_SLOTS, true))
    {
        return AbiBridge::Result::Ok;
    }

    if (AbiBridge::LookupGuestStub(*gRegistry, name, stub) != AbiBridge::Result::Ok) return AbiBridge::Result::Ok;

    registers.general[0] = (uint64_t)stub;
    return AbiBridge::Result::Ok;
}

static bool RegisterForwardedEntryPoint(AbiBridge::Registry& registry, const char* name, void* hostFunction, int argumentCount,
                                        bool returnsValue)
{
    AbiBridge::Signature signature = MakeForwardingSignature(argumentCount, returnsValue);
    AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, name, signature, hostFunction);

    if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
    {
        fprintf(stderr, "[vulkanwsi] cannot register %s: %s\n", name, AbiBridge::ResultName(registered));
        return false;
    }

    gStatistics.entryPointsRegistered += 1;
    return true;
}

static void DescribeRendererForGuest(VkPhysicalDeviceProperties& properties)
{
    static std::once_flag announced;
    std::call_once(announced, [&properties]() {
        fprintf(stderr, "[vulkanwsi] the guest is told it draws with %s, not %s\n", Platform::DEVICE_GPU, properties.deviceName);
    });

    snprintf(properties.deviceName, sizeof(properties.deviceName), "%s", Platform::DEVICE_GPU);

    properties.vendorID = GUEST_RENDERER_VENDOR;
    properties.deviceID = GUEST_RENDERER_DEVICE;
    properties.deviceType = VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU;
    properties.driverVersion = GUEST_RENDERER_DRIVER_VERSION;

    for (uint32_t index = 0; index < VK_UUID_SIZE; ++index)
    {
        properties.pipelineCacheUUID[index] = (uint8_t)(GUEST_RENDERER_VENDOR + index * 7u);
    }
}

static void PropertiesForGuest(VkPhysicalDevice device, VkPhysicalDeviceProperties* properties)
{
    if (gHostGetPhysicalDeviceProperties) gHostGetPhysicalDeviceProperties(device, properties);
    if (!properties) return;

    DescribeRendererForGuest(*properties);
}

static VkResult CreateInstanceForGuest(const VkInstanceCreateInfo* createInfo, const VkAllocationCallbacks* allocator,
                                       VkInstance* instance)
{
    CountForwardedCall();

    if (allocator)
    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        gStatistics.guestAllocatorsRefused += 1;
        fprintf(stderr, "[vulkanwsi] vkCreateInstance was given guest allocation callbacks; the host cannot call back into the "
                        "guest, so they are refused\n");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    if (!createInfo) return VK_ERROR_INITIALIZATION_FAILED;

    std::vector<const char*> extensions;
    bool rewrote = false;

    for (uint32_t index = 0; index < createInfo->enabledExtensionCount; ++index)
    {
        const char* requested = createInfo->ppEnabledExtensionNames[index];

        if (requested && strcmp(requested, VulkanWsi::GUEST_SURFACE_EXTENSION) == 0)
        {
            extensions.push_back(VulkanWsi::HOST_SURFACE_EXTENSION);
            rewrote = true;
            continue;
        }

        extensions.push_back(requested);
    }

    if (rewrote)
    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        gStatistics.surfaceExtensionsRewritten += 1;
        fprintf(stderr, "[vulkanwsi] the guest asked for %s; the host is being asked for %s instead\n",
                VulkanWsi::GUEST_SURFACE_EXTENSION, VulkanWsi::HOST_SURFACE_EXTENSION);
    }

    VkInstanceCreateInfo rewritten = *createInfo;
    rewritten.enabledExtensionCount = (uint32_t)extensions.size();
    rewritten.ppEnabledExtensionNames = extensions.empty() ? nullptr : extensions.data();

    return gHostCreateInstance(&rewritten, nullptr, instance);
}

static VkResult SurfaceFormatsForGuest(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, uint32_t* count,
                                       VkSurfaceFormatKHR* formats)
{
    CountForwardedCall();

    if (!gHostGetSurfaceFormats && gHostGetInstanceProcAddr && gHostInstance != VK_NULL_HANDLE)
    {
        gHostGetSurfaceFormats = (PFN_vkGetPhysicalDeviceSurfaceFormatsKHR)gHostGetInstanceProcAddr(
            gHostInstance, "vkGetPhysicalDeviceSurfaceFormatsKHR");
    }

    if (!gHostGetSurfaceFormats) return VK_ERROR_INITIALIZATION_FAILED;

    VkResult asked = gHostGetSurfaceFormats(physicalDevice, surface, count, formats);

    fprintf(stderr, "[vulkanwsi] the guest asked this surface for formats: result %d, count %u, buffer %p\n", (int)asked,
            count ? (unsigned)*count : 0u, (const void*)formats);

    if (asked == VK_SUCCESS && formats && count)
    {
        for (uint32_t index = 0; index < *count; ++index)
        {
            fprintf(stderr, "[vulkanwsi]   format %u in colour space %u\n", (unsigned)formats[index].format,
                    (unsigned)formats[index].colorSpace);
        }
    }

    return asked;
}

static VkResult CreateSwapchainForGuest(VkDevice device, const VkSwapchainCreateInfoKHR* createInfo,
                                        const VkAllocationCallbacks*, VkSwapchainKHR* swapchain)
{
    CountForwardedCall();

    if (!createInfo || !swapchain) return VK_ERROR_INITIALIZATION_FAILED;

    if (!gHostCreateSwapchain && gHostGetDeviceProcAddr)
        gHostCreateSwapchain = (PFN_vkCreateSwapchainKHR)gHostGetDeviceProcAddr(device, "vkCreateSwapchainKHR");

    if (!gHostCreateSwapchain)
    {
        fprintf(stderr, "[vulkanwsi] the host driver does not expose vkCreateSwapchainKHR\n");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    VkSwapchainCreateInfoKHR rewritten = *createInfo;

    const VkImageUsageFlags requestedUsage = rewritten.imageUsage;
    rewritten.imageUsage = OverlayVulkan::AddCopyUsage(rewritten.imageUsage);

    if (PresentWithoutWaitingRequested() && rewritten.presentMode != VK_PRESENT_MODE_IMMEDIATE_KHR)
    {
        VkPresentModeKHR asked = rewritten.presentMode;
        rewritten.presentMode = VK_PRESENT_MODE_IMMEDIATE_KHR;

        fprintf(stderr, "[vulkanwsi] presenting without waiting for the display, so the guest asked for mode %u and gets %u\n",
                (unsigned)asked, (unsigned)rewritten.presentMode);
    }

    VkResult created = gHostCreateSwapchain(device, &rewritten, nullptr, swapchain);

    if (created != VK_SUCCESS && rewritten.presentMode != createInfo->presentMode)
    {
        fprintf(stderr, "[vulkanwsi] the host driver refused the unthrottled present mode, so the guest keeps its own\n");

        rewritten.presentMode = createInfo->presentMode;
        created = gHostCreateSwapchain(device, &rewritten, nullptr, swapchain);
    }

    if (created != VK_SUCCESS && rewritten.imageUsage != requestedUsage)
    {
        fprintf(stderr, "[overlay] the driver refused the extra transfer usage, so the counter stays off the guest's frames\n");

        rewritten.imageUsage = requestedUsage;
        created = gHostCreateSwapchain(device, &rewritten, nullptr, swapchain);
    }

    if (created == VK_SUCCESS)
    {
        OverlayVulkan::NoteSwapchain(device, *swapchain, rewritten.imageFormat, rewritten.imageExtent,
                                     (rewritten.imageUsage & VK_IMAGE_USAGE_TRANSFER_DST_BIT) != 0);
    }

    fprintf(stderr,
            "[vulkanwsi] the swapchain the guest asked for came back %d: type %u, next %p, flags %u, format %u, colour space %u, "
            "%ux%u, layers %u, %u images, present mode %u, usage %u, alpha %u, transform %u, sharing %u, clipped %u, old %p\n",
            (int)created, (unsigned)rewritten.sType, (const void*)rewritten.pNext, (unsigned)rewritten.flags,
            (unsigned)rewritten.imageFormat, (unsigned)rewritten.imageColorSpace, rewritten.imageExtent.width,
            rewritten.imageExtent.height, rewritten.imageArrayLayers, rewritten.minImageCount, (unsigned)rewritten.presentMode,
            (unsigned)rewritten.imageUsage, (unsigned)rewritten.compositeAlpha, (unsigned)rewritten.preTransform,
            (unsigned)rewritten.imageSharingMode, (unsigned)rewritten.clipped, (const void*)rewritten.oldSwapchain);

    return created;
}

static VkResult CreateSurfaceForGuest(VkInstance instance, const GuestAndroidSurfaceCreateInfo* createInfo,
                                      const VkAllocationCallbacks* allocator, VkSurfaceKHR* surface)
{
    CountForwardedCall();

    if (allocator)
    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        gStatistics.guestAllocatorsRefused += 1;
        fprintf(stderr, "[vulkanwsi] vkCreateAndroidSurfaceKHR was given guest allocation callbacks; they are refused\n");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    if (!createInfo || !surface) return VK_ERROR_INITIALIZATION_FAILED;

    if (!gDisplay || !gSurface)
    {
        fprintf(stderr, "[vulkanwsi] the guest asked for a window surface but no compositor surface is attached\n");
        return VK_ERROR_INITIALIZATION_FAILED;
    }

    gHostInstance = instance;

    if (!gHostCreateWaylandSurface)
    {
        gHostCreateWaylandSurface =
            (PFN_vkCreateWaylandSurfaceKHR)gHostGetInstanceProcAddr(instance, "vkCreateWaylandSurfaceKHR");
    }

    if (!gHostCreateWaylandSurface)
    {
        fprintf(stderr, "[vulkanwsi] the host driver does not expose vkCreateWaylandSurfaceKHR, so %s cannot be translated\n",
                VulkanWsi::GUEST_SURFACE_EXTENSION);
        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    if (gSharedSurface != VK_NULL_HANDLE)
    {
        gSharedSurfaceHolders += 1;
        *surface = gSharedSurface;
        fprintf(stderr, "[vulkanwsi] the guest asked for a second window surface; the one compositor surface is shared instead\n");
        return VK_SUCCESS;
    }

    VkWaylandSurfaceCreateInfoKHR translated{};
    translated.sType = VK_STRUCTURE_TYPE_WAYLAND_SURFACE_CREATE_INFO_KHR;
    translated.display = gDisplay;
    translated.surface = gSurface;

    VkResult created = gHostCreateWaylandSurface(instance, &translated, nullptr, surface);
    if (created != VK_SUCCESS)
    {
        fprintf(stderr, "[vulkanwsi] vkCreateWaylandSurfaceKHR failed with %d\n", (int)created);
        return created;
    }

    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        gStatistics.surfacesCreated += 1;
    }

    gSharedSurface = *surface;
    gSharedSurfaceHolders = 1;

    fprintf(stderr, "[vulkanwsi] the guest window surface is a real compositor surface\n");
    return VK_SUCCESS;
}

static void DestroySurfaceForGuest(VkInstance instance, VkSurfaceKHR surface, const VkAllocationCallbacks*)
{
    CountForwardedCall();

    if (surface == VK_NULL_HANDLE) return;

    if (surface == gSharedSurface)
    {
        if (gSharedSurfaceHolders > 0) gSharedSurfaceHolders -= 1;

        fprintf(stderr, "[vulkanwsi] the guest released the window surface; it is kept because a compositor surface may take its role "
                        "only once\n");
        return;
    }

    if (!gHostDestroySurface && gHostGetInstanceProcAddr)
        gHostDestroySurface = (PFN_vkDestroySurfaceKHR)gHostGetInstanceProcAddr(instance, "vkDestroySurfaceKHR");

    if (gHostDestroySurface) gHostDestroySurface(instance, surface, nullptr);
}

static uint64_t MonotonicNanos()
{
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static VkResult CreateDeviceForGuest(VkPhysicalDevice physicalDevice, const VkDeviceCreateInfo* createInfo,
                                     const VkAllocationCallbacks*, VkDevice* device)
{
    CountForwardedCall();

    VkResult created = gHostCreateDevice(physicalDevice, createInfo, nullptr, device);

    if (created == VK_SUCCESS && device) OverlayVulkan::NoteDevice(physicalDevice, *device);

    return created;
}

static void GetDeviceQueueForGuest(VkDevice device, uint32_t queueFamilyIndex, uint32_t queueIndex, VkQueue* queue)
{
    CountForwardedCall();

    gHostGetDeviceQueue(device, queueFamilyIndex, queueIndex, queue);

    if (queue) OverlayVulkan::NoteQueue(device, queueFamilyIndex, *queue);
}

static void DestroySwapchainForGuest(VkDevice device, VkSwapchainKHR swapchain, const VkAllocationCallbacks*)
{
    CountForwardedCall();

    OverlayVulkan::ForgetSwapchain(swapchain);
    gHostDestroySwapchain(device, swapchain, nullptr);
}

static VkResult QueuePresentForGuest(VkQueue queue, const VkPresentInfoKHR* presentInfo)
{
    FrameRate::Tick();

    VkPresentInfoKHR rewrittenPresent = {};
    VkSemaphore overlaySemaphore = VK_NULL_HANDLE;

    if (presentInfo && presentInfo->swapchainCount == 1 && presentInfo->pSwapchains && presentInfo->pImageIndices
        && OverlayVulkan::Draw(queue, presentInfo->pSwapchains[0], presentInfo->pImageIndices[0], presentInfo->pWaitSemaphores,
                               presentInfo->waitSemaphoreCount, overlaySemaphore))
    {
        rewrittenPresent = *presentInfo;
        rewrittenPresent.waitSemaphoreCount = 1;
        rewrittenPresent.pWaitSemaphores = &overlaySemaphore;
        presentInfo = &rewrittenPresent;
    }

    VkResult presented = gHostQueuePresent(queue, presentInfo);
    uint64_t completed = MonotonicNanos();

    if (presented == VK_SUCCESS || presented == VK_SUBOPTIMAL_KHR)
    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        gStatistics.framesPresented += presentInfo ? presentInfo->swapchainCount : 1;
        if (gPresentTimestamps.size() < VulkanWsi::MAX_PACING_SAMPLES) gPresentTimestamps.push_back(completed);

        if (FrameTracingRequested())
        {
            if (gFrameWindowStart == 0) gFrameWindowStart = completed;
            gFrameWindowCount += 1;

            uint64_t elapsed = completed - gFrameWindowStart;
            if (elapsed >= VulkanWsi::FRAME_REPORT_NANOS)
            {
                fprintf(stderr, "[vulkanwsi] %llu frames presented in the last %.2fs, %.1f per second, %llu in total\n",
                        (unsigned long long)gFrameWindowCount, (double)elapsed / 1e9,
                        (double)gFrameWindowCount * 1e9 / (double)elapsed, (unsigned long long)gStatistics.framesPresented);

                gFrameWindowStart = completed;
                gFrameWindowCount = 0;
            }
        }
    }

    return presented;
}

VulkanWsi::Result VulkanWsi::RegisterAll(GuestRuntime::Instance& runtime)
{
    if (!gDriver) gDriver = dlopen(DRIVER_SONAME, RTLD_NOW | RTLD_LOCAL);

    if (!gDriver)
    {
        fprintf(stderr, "[vulkanwsi] no host Vulkan driver: %s\n", dlerror());
        return Result::NoDriver;
    }

    if (FrameRate::OverlayRequested()) OverlayVulkan::Enable([](const char* name) { return dlsym(gDriver, name); });

    gHostGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)dlsym(gDriver, "vkGetInstanceProcAddr");
    gHostGetDeviceProcAddr = (PFN_vkGetDeviceProcAddr)dlsym(gDriver, "vkGetDeviceProcAddr");
    gHostCreateInstance = (PFN_vkCreateInstance)dlsym(gDriver, "vkCreateInstance");

    if (!gHostGetInstanceProcAddr || !gHostCreateInstance)
    {
        fprintf(stderr, "[vulkanwsi] %s does not expose the loader entry points\n", DRIVER_SONAME);
        return Result::MissingEntryPoint;
    }

    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(runtime);
    gRegistry = &registry;

    if (AbiBridge::RegisterHostHandler(registry, "vkGetInstanceProcAddr", &ProcAddressForGuest, nullptr) !=
            AbiBridge::Result::Ok ||
        AbiBridge::RegisterHostHandler(registry, "vkGetDeviceProcAddr", &ProcAddressForGuest, (void*)1) != AbiBridge::Result::Ok)
    {
        return Result::RegistrationFailed;
    }

    gStatistics.entryPointsRegistered += 2;

    if (!RegisterForwardedEntryPoint(registry, "vkCreateInstance", (void*)&CreateInstanceForGuest,
                                     EntryPointTraits<PFN_vkCreateInstance>::argumentCount, true) ||
        !RegisterForwardedEntryPoint(registry, "vkCreateAndroidSurfaceKHR", (void*)&CreateSurfaceForGuest, 4, true) ||
        !RegisterForwardedEntryPoint(registry, "vkCreateSwapchainKHR", (void*)&CreateSwapchainForGuest, 4, true) ||
        !RegisterForwardedEntryPoint(registry, "vkGetPhysicalDeviceSurfaceFormatsKHR", (void*)&SurfaceFormatsForGuest, 4, true) ||
        !RegisterForwardedEntryPoint(registry, "vkDestroySurfaceKHR", (void*)&DestroySurfaceForGuest, 3, false))
    {
        return Result::RegistrationFailed;
    }

#define FORWARD_ENTRY_POINT(name)                                                                                       \
    do                                                                                                                  \
    {                                                                                                                   \
        static_assert(EntryPointTraits<PFN_##name>::argumentsFitRegisters, #name " passes an argument wider than a register"); \
        static_assert(EntryPointTraits<PFN_##name>::argumentsAreIntegral, #name " passes a floating point argument");    \
        void* symbol = dlsym(gDriver, #name);                                                                           \
        if (!symbol)                                                                                                    \
        {                                                                                                               \
            fprintf(stderr, "[vulkanwsi] the host driver does not expose %s\n", #name);                                  \
            return Result::MissingEntryPoint;                                                                           \
        }                                                                                                               \
        if (!RegisterForwardedEntryPoint(registry, #name, symbol, EntryPointTraits<PFN_##name>::argumentCount,           \
                                         EntryPointTraits<PFN_##name>::returnsValue))                                   \
            return Result::RegistrationFailed;                                                                          \
    } while (false)

    FORWARD_ENTRY_POINT(vkDestroyInstance);
    FORWARD_ENTRY_POINT(vkEnumeratePhysicalDevices);
    gHostGetPhysicalDeviceProperties = (PFN_vkGetPhysicalDeviceProperties)dlsym(gDriver, "vkGetPhysicalDeviceProperties");
    if (!gHostGetPhysicalDeviceProperties)
    {
        fprintf(stderr, "[vulkanwsi] the host driver does not expose vkGetPhysicalDeviceProperties\n");
        return Result::MissingEntryPoint;
    }

    if (!RegisterForwardedEntryPoint(registry, "vkGetPhysicalDeviceProperties", (void*)&PropertiesForGuest, 2, false))
        return Result::RegistrationFailed;

    FORWARD_ENTRY_POINT(vkGetPhysicalDeviceQueueFamilyProperties);

    gHostCreateDevice = (PFN_vkCreateDevice)dlsym(gDriver, "vkCreateDevice");
    gHostGetDeviceQueue = (PFN_vkGetDeviceQueue)dlsym(gDriver, "vkGetDeviceQueue");
    gHostDestroySwapchain = (PFN_vkDestroySwapchainKHR)dlsym(gDriver, "vkDestroySwapchainKHR");

    if (!gHostCreateDevice || !gHostGetDeviceQueue || !gHostDestroySwapchain)
    {
        fprintf(stderr, "[vulkanwsi] the host driver does not expose vkCreateDevice, vkGetDeviceQueue or vkDestroySwapchainKHR\n");
        return Result::MissingEntryPoint;
    }

    if (!RegisterForwardedEntryPoint(registry, "vkCreateDevice", (void*)&CreateDeviceForGuest, 4, true) ||
        !RegisterForwardedEntryPoint(registry, "vkGetDeviceQueue", (void*)&GetDeviceQueueForGuest, 4, false) ||
        !RegisterForwardedEntryPoint(registry, "vkDestroySwapchainKHR", (void*)&DestroySwapchainForGuest, 3, false))
    {
        return Result::RegistrationFailed;
    }

    FORWARD_ENTRY_POINT(vkDestroyDevice);
    FORWARD_ENTRY_POINT(vkDeviceWaitIdle);

    FORWARD_ENTRY_POINT(vkDestroySurfaceKHR);
    FORWARD_ENTRY_POINT(vkGetPhysicalDeviceSurfaceSupportKHR);
    FORWARD_ENTRY_POINT(vkGetPhysicalDeviceSurfaceCapabilitiesKHR);
    FORWARD_ENTRY_POINT(vkGetPhysicalDeviceSurfaceFormatsKHR);
    FORWARD_ENTRY_POINT(vkGetPhysicalDeviceSurfacePresentModesKHR);

    FORWARD_ENTRY_POINT(vkCreateSwapchainKHR);
    FORWARD_ENTRY_POINT(vkDestroySwapchainKHR);
    FORWARD_ENTRY_POINT(vkGetSwapchainImagesKHR);
    FORWARD_ENTRY_POINT(vkAcquireNextImageKHR);

    gHostQueuePresent = (PFN_vkQueuePresentKHR)dlsym(gDriver, "vkQueuePresentKHR");
    if (!gHostQueuePresent)
    {
        fprintf(stderr, "[vulkanwsi] the host driver does not expose vkQueuePresentKHR\n");
        return Result::MissingEntryPoint;
    }

    if (!RegisterForwardedEntryPoint(registry, "vkQueuePresentKHR", (void*)&QueuePresentForGuest,
                                     EntryPointTraits<PFN_vkQueuePresentKHR>::argumentCount, true))
    {
        return Result::RegistrationFailed;
    }

    FORWARD_ENTRY_POINT(vkCreateImageView);
    FORWARD_ENTRY_POINT(vkDestroyImageView);
    FORWARD_ENTRY_POINT(vkCreateRenderPass);
    FORWARD_ENTRY_POINT(vkDestroyRenderPass);
    FORWARD_ENTRY_POINT(vkCreateFramebuffer);
    FORWARD_ENTRY_POINT(vkDestroyFramebuffer);
    FORWARD_ENTRY_POINT(vkCreateShaderModule);
    FORWARD_ENTRY_POINT(vkDestroyShaderModule);
    FORWARD_ENTRY_POINT(vkCreatePipelineLayout);
    FORWARD_ENTRY_POINT(vkDestroyPipelineLayout);
    FORWARD_ENTRY_POINT(vkCreateGraphicsPipelines);
    FORWARD_ENTRY_POINT(vkDestroyPipeline);

    FORWARD_ENTRY_POINT(vkCreateCommandPool);
    FORWARD_ENTRY_POINT(vkDestroyCommandPool);
    FORWARD_ENTRY_POINT(vkAllocateCommandBuffers);
    FORWARD_ENTRY_POINT(vkBeginCommandBuffer);
    FORWARD_ENTRY_POINT(vkEndCommandBuffer);
    FORWARD_ENTRY_POINT(vkResetCommandBuffer);

    FORWARD_ENTRY_POINT(vkCmdBeginRenderPass);
    FORWARD_ENTRY_POINT(vkCmdEndRenderPass);
    FORWARD_ENTRY_POINT(vkCmdBindPipeline);
    FORWARD_ENTRY_POINT(vkCmdPushConstants);
    FORWARD_ENTRY_POINT(vkCmdDraw);

    FORWARD_ENTRY_POINT(vkCreateSemaphore);
    FORWARD_ENTRY_POINT(vkDestroySemaphore);
    FORWARD_ENTRY_POINT(vkCreateFence);
    FORWARD_ENTRY_POINT(vkDestroyFence);
    FORWARD_ENTRY_POINT(vkResetFences);
    FORWARD_ENTRY_POINT(vkWaitForFences);
    FORWARD_ENTRY_POINT(vkQueueSubmit);

#undef FORWARD_ENTRY_POINT

    fprintf(stderr, "[vulkanwsi] %llu Vulkan entry points forward to the host driver\n",
            (unsigned long long)gStatistics.entryPointsRegistered);

    return Result::Ok;
}

void VulkanWsi::Shutdown()
{
    gDisplay = nullptr;
    gSurface = nullptr;
}

void VulkanWsi::AttachSurfaceTarget(wl_display* display, wl_surface* surface)
{
    gDisplay = display;
    gSurface = surface;
}

bool VulkanWsi::HasSurfaceTarget()
{
    return gDisplay != nullptr && gSurface != nullptr;
}

VulkanWsi::Result VulkanWsi::WritePacingReport(const std::string& path)
{
    std::vector<uint64_t> samples;
    {
        std::lock_guard<std::mutex> guard(gStatisticsMutex);
        samples = gPresentTimestamps;
    }

    FILE* report = fopen(path.c_str(), "w");
    if (!report)
    {
        fprintf(stderr, "[vulkanwsi] cannot write the pacing report to %s\n", path.c_str());
        return Result::ReportUnwritable;
    }

    fprintf(report, "presents %zu\n", samples.size());
    fprintf(report, "capacity %llu\n", (unsigned long long)MAX_PACING_SAMPLES);

    for (size_t index = 1; index < samples.size(); ++index)
    {
        fprintf(report, "interval %zu %llu\n", index, (unsigned long long)(samples[index] - samples[index - 1]));
    }

    fclose(report);

    fprintf(stderr, "[vulkanwsi] wrote %zu present interval(s) to %s\n", samples.empty() ? 0 : samples.size() - 1, path.c_str());
    return Result::Ok;
}

VulkanWsi::Statistics VulkanWsi::GetStatistics()
{
    std::lock_guard<std::mutex> guard(gStatisticsMutex);
    return gStatistics;
}

const char* VulkanWsi::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NoDriver: return "NoDriver";
        case Result::MissingEntryPoint: return "MissingEntryPoint";
        case Result::RegistrationFailed: return "RegistrationFailed";
        case Result::NoSurfaceTarget: return "NoSurfaceTarget";
        case Result::GuestAllocatorRefused: return "GuestAllocatorRefused";
        case Result::ReportUnwritable: return "ReportUnwritable";
    }

    return "Unknown";
}

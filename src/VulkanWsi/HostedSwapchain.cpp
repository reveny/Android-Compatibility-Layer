#include "HostedSwapchain.hpp"

#include <algorithm>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstring>
#include <memory>
#include <mutex>
#include <set>
#include <unordered_map>

#include <sys/sysmacros.h>
#include <unistd.h>

#include "Settings/Settings.hpp"
#include "VulkanWsi/VulkanWsi.hpp"

struct DeviceFunctions
{
    PFN_vkCreateImage createImage = nullptr;
    PFN_vkDestroyImage destroyImage = nullptr;
    PFN_vkGetImageMemoryRequirements imageRequirements = nullptr;
    PFN_vkAllocateMemory allocateMemory = nullptr;
    PFN_vkFreeMemory freeMemory = nullptr;
    PFN_vkBindImageMemory bindImageMemory = nullptr;
    PFN_vkGetMemoryFdKHR memoryDescriptor = nullptr;
    PFN_vkGetImageDrmFormatModifierPropertiesEXT modifierProperties = nullptr;
    PFN_vkGetImageSubresourceLayout subresourceLayout = nullptr;
    PFN_vkCreateSemaphore createSemaphore = nullptr;
    PFN_vkDestroySemaphore destroySemaphore = nullptr;
    PFN_vkGetSemaphoreFdKHR semaphoreDescriptor = nullptr;
    PFN_vkImportSemaphoreFdKHR importSemaphore = nullptr;
    PFN_vkImportFenceFdKHR importFence = nullptr;
    PFN_vkQueueSubmit queueSubmit = nullptr;
    PFN_vkQueueWaitIdle queueWaitIdle = nullptr;
    PFN_vkCreateBuffer createBuffer = nullptr;
    PFN_vkDestroyBuffer destroyBuffer = nullptr;
    PFN_vkGetBufferMemoryRequirements bufferRequirements = nullptr;
    PFN_vkBindBufferMemory bindBufferMemory = nullptr;
    PFN_vkMapMemory mapMemory = nullptr;
    PFN_vkUnmapMemory unmapMemory = nullptr;
    PFN_vkInvalidateMappedMemoryRanges invalidateRanges = nullptr;
    PFN_vkCreateCommandPool createCommandPool = nullptr;
    PFN_vkDestroyCommandPool destroyCommandPool = nullptr;
    PFN_vkAllocateCommandBuffers allocateCommandBuffers = nullptr;
    PFN_vkBeginCommandBuffer beginCommandBuffer = nullptr;
    PFN_vkEndCommandBuffer endCommandBuffer = nullptr;
    PFN_vkCmdPipelineBarrier pipelineBarrier = nullptr;
    PFN_vkCmdCopyImageToBuffer copyImageToBuffer = nullptr;
    PFN_vkCreateFence createFence = nullptr;
    PFN_vkDestroyFence destroyFence = nullptr;
    PFN_vkWaitForFences waitForFences = nullptr;
    PFN_vkResetFences resetFences = nullptr;
    PFN_vkDeviceWaitIdle deviceWaitIdle = nullptr;
};

struct DeviceRecord
{
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkQueue queue = VK_NULL_HANDLE;
    uint32_t queueFamily = 0;
    bool sharing = false;
    bool semaphoreDescriptors = false;
    bool fenceDescriptors = false;
    DeviceFunctions functions;
};

struct HostedImage
{
    VkImage image = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    VkSemaphore exportSemaphore = VK_NULL_HANDLE;
    VkCommandBuffer copyCommands = VK_NULL_HANDLE;
    bool held = false;
    bool acquired = false;
};

struct Swapchain
{
    VkDevice device = VK_NULL_HANDLE;
    DeviceRecord* record = nullptr;
    VkFormat format = VK_FORMAT_UNDEFINED;
    uint32_t drmFormat = 0;
    VkExtent2D extent{};
    VkImageUsageFlags usage = 0;
    VkPresentModeKHR presentMode = VK_PRESENT_MODE_FIFO_KHR;
    std::vector<HostedImage> images;
    uint32_t generation = 0;
    bool sharing = false;
    VkBuffer staging = VK_NULL_HANDLE;
    VkDeviceMemory stagingMemory = VK_NULL_HANDLE;
    void* stagingPixels = nullptr;
    bool stagingCoherent = true;
    VkCommandPool commandPool = VK_NULL_HANDLE;
    VkFence copyFence = VK_NULL_HANDLE;
    std::mutex mutex;
    std::condition_variable released;
    std::vector<uint32_t> releasedIndices;
    bool refused = false;
};

struct FakeSurface
{
    uint32_t marker = 0;
};

static PFN_vkGetInstanceProcAddr gGetInstanceProcAddr = nullptr;
static PFN_vkGetDeviceProcAddr gGetDeviceProcAddr = nullptr;
static VkInstance gInstance = VK_NULL_HANDLE;
static HostedWindow::Session* gSession = nullptr;
static uint32_t gWidth = 0;
static uint32_t gHeight = 0;
static std::mutex gMutex;
static std::unordered_map<VkDevice, std::unique_ptr<DeviceRecord>> gDevices;
static std::unordered_map<VkQueue, uint32_t> gQueueFamilies;
static std::set<VkSurfaceKHR> gSurfaces;
static std::set<VkSwapchainKHR> gSwapchains;
static Swapchain* gListening = nullptr;
static HostedSwapchain::Statistics gStatistics;

static const char* const WANTED_DEVICE_EXTENSIONS[] = {
    "VK_KHR_external_memory",         "VK_KHR_external_memory_fd",       "VK_EXT_external_memory_dma_buf",
    "VK_KHR_image_format_list",       "VK_KHR_bind_memory2",             "VK_KHR_get_memory_requirements2",
    "VK_KHR_sampler_ycbcr_conversion", "VK_KHR_maintenance1",            "VK_KHR_dedicated_allocation",
    "VK_EXT_image_drm_format_modifier", "VK_KHR_external_semaphore",     "VK_KHR_external_semaphore_fd",
    "VK_KHR_external_fence",          "VK_KHR_external_fence_fd",
};

template <typename Function> static Function InstanceFunction(const char* name)
{
    if (!gGetInstanceProcAddr || gInstance == VK_NULL_HANDLE) return nullptr;
    return reinterpret_cast<Function>(gGetInstanceProcAddr(gInstance, name));
}

template <typename Function> static void DeviceFunction(VkDevice device, const char* name, Function& function)
{
    function = reinterpret_cast<Function>(gGetDeviceProcAddr(device, name));
}

static bool Enabled(const std::vector<const char*>& extensions, const char* name)
{
    for (const char* extension : extensions)
    {
        if (extension && strcmp(extension, name) == 0) return true;
    }

    return false;
}

static void LoadDeviceFunctions(VkDevice device, DeviceFunctions& functions)
{
    DeviceFunction(device, "vkCreateImage", functions.createImage);
    DeviceFunction(device, "vkDestroyImage", functions.destroyImage);
    DeviceFunction(device, "vkGetImageMemoryRequirements", functions.imageRequirements);
    DeviceFunction(device, "vkAllocateMemory", functions.allocateMemory);
    DeviceFunction(device, "vkFreeMemory", functions.freeMemory);
    DeviceFunction(device, "vkBindImageMemory", functions.bindImageMemory);
    DeviceFunction(device, "vkGetMemoryFdKHR", functions.memoryDescriptor);
    DeviceFunction(device, "vkGetImageDrmFormatModifierPropertiesEXT", functions.modifierProperties);
    DeviceFunction(device, "vkGetImageSubresourceLayout", functions.subresourceLayout);
    DeviceFunction(device, "vkCreateSemaphore", functions.createSemaphore);
    DeviceFunction(device, "vkDestroySemaphore", functions.destroySemaphore);
    DeviceFunction(device, "vkGetSemaphoreFdKHR", functions.semaphoreDescriptor);
    DeviceFunction(device, "vkImportSemaphoreFdKHR", functions.importSemaphore);
    DeviceFunction(device, "vkImportFenceFdKHR", functions.importFence);
    DeviceFunction(device, "vkQueueSubmit", functions.queueSubmit);
    DeviceFunction(device, "vkQueueWaitIdle", functions.queueWaitIdle);
    DeviceFunction(device, "vkCreateBuffer", functions.createBuffer);
    DeviceFunction(device, "vkDestroyBuffer", functions.destroyBuffer);
    DeviceFunction(device, "vkGetBufferMemoryRequirements", functions.bufferRequirements);
    DeviceFunction(device, "vkBindBufferMemory", functions.bindBufferMemory);
    DeviceFunction(device, "vkMapMemory", functions.mapMemory);
    DeviceFunction(device, "vkUnmapMemory", functions.unmapMemory);
    DeviceFunction(device, "vkInvalidateMappedMemoryRanges", functions.invalidateRanges);
    DeviceFunction(device, "vkCreateCommandPool", functions.createCommandPool);
    DeviceFunction(device, "vkDestroyCommandPool", functions.destroyCommandPool);
    DeviceFunction(device, "vkAllocateCommandBuffers", functions.allocateCommandBuffers);
    DeviceFunction(device, "vkBeginCommandBuffer", functions.beginCommandBuffer);
    DeviceFunction(device, "vkEndCommandBuffer", functions.endCommandBuffer);
    DeviceFunction(device, "vkCmdPipelineBarrier", functions.pipelineBarrier);
    DeviceFunction(device, "vkCmdCopyImageToBuffer", functions.copyImageToBuffer);
    DeviceFunction(device, "vkCreateFence", functions.createFence);
    DeviceFunction(device, "vkDestroyFence", functions.destroyFence);
    DeviceFunction(device, "vkWaitForFences", functions.waitForFences);
    DeviceFunction(device, "vkResetFences", functions.resetFences);
    DeviceFunction(device, "vkDeviceWaitIdle", functions.deviceWaitIdle);
}

static DeviceRecord* RecordFor(VkDevice device)
{
    std::lock_guard<std::mutex> guard(gMutex);

    auto found = gDevices.find(device);
    return found == gDevices.end() ? nullptr : found->second.get();
}

static uint32_t DrmFormatFor(VkFormat format, bool opaque)
{
    switch (format)
    {
        case VK_FORMAT_B8G8R8A8_UNORM:
        case VK_FORMAT_B8G8R8A8_SRGB: return opaque ? ControlChannel::FORMAT_XRGB8888 : ControlChannel::FORMAT_ARGB8888;
        case VK_FORMAT_R8G8B8A8_UNORM:
        case VK_FORMAT_R8G8B8A8_SRGB: return opaque ? ControlChannel::FORMAT_XBGR8888 : ControlChannel::FORMAT_ABGR8888;
        default: return 0;
    }
}

static bool SameGpuAsCompositor(VkPhysicalDevice physicalDevice, uint64_t renderDevice)
{
    if (renderDevice == 0) return true;

    auto getProperties = InstanceFunction<PFN_vkGetPhysicalDeviceProperties2>("vkGetPhysicalDeviceProperties2");
    if (!getProperties) return true;

    VkPhysicalDeviceDrmPropertiesEXT drm{};
    drm.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRM_PROPERTIES_EXT;

    VkPhysicalDeviceProperties2 properties{};
    properties.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;
    properties.pNext = &drm;
    getProperties(physicalDevice, &properties);

    if (!drm.hasRender) return true;

    return makedev((unsigned)drm.renderMajor, (unsigned)drm.renderMinor) == (dev_t)renderDevice;
}

static std::vector<VkDrmFormatModifierPropertiesEXT> DriverModifiers(VkPhysicalDevice physicalDevice, VkFormat format)
{
    std::vector<VkDrmFormatModifierPropertiesEXT> modifiers;

    auto getFormatProperties = InstanceFunction<PFN_vkGetPhysicalDeviceFormatProperties2>("vkGetPhysicalDeviceFormatProperties2");
    if (!getFormatProperties) return modifiers;

    VkDrmFormatModifierPropertiesListEXT list{};
    list.sType = VK_STRUCTURE_TYPE_DRM_FORMAT_MODIFIER_PROPERTIES_LIST_EXT;

    VkFormatProperties2 properties{};
    properties.sType = VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2;
    properties.pNext = &list;
    getFormatProperties(physicalDevice, format, &properties);

    if (list.drmFormatModifierCount == 0) return modifiers;

    modifiers.resize(list.drmFormatModifierCount);
    list.pDrmFormatModifierProperties = modifiers.data();
    getFormatProperties(physicalDevice, format, &properties);
    modifiers.resize(list.drmFormatModifierCount);

    return modifiers;
}

static std::vector<uint64_t> SharableModifiers(const DeviceRecord& record, VkFormat format, uint32_t drmFormat,
                                               std::unordered_map<uint64_t, uint32_t>& planeCounts)
{
    std::vector<uint64_t> chosen;

    if (!gSession || !record.sharing || !HostedWindow::DmabufWanted()) return chosen;

    HostedWindow::Display display = HostedWindow::CurrentDisplay(*gSession);
    if (!display.dmabufAvailable) return chosen;

    bool sameGpu = SameGpuAsCompositor(record.physicalDevice, display.renderDevice);
    std::vector<VkDrmFormatModifierPropertiesEXT> driver = DriverModifiers(record.physicalDevice, format);

    for (const VkDrmFormatModifierPropertiesEXT& offered : driver)
    {
        if ((offered.drmFormatModifierTilingFeatures & VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT) == 0) continue;
        if (!sameGpu && offered.drmFormatModifier != ControlChannel::MODIFIER_LINEAR) continue;

        for (const ControlChannel::FormatEntry& entry : display.formats)
        {
            if (entry.format != drmFormat || entry.modifier != offered.drmFormatModifier) continue;

            chosen.push_back(offered.drmFormatModifier);
            planeCounts[offered.drmFormatModifier] = offered.drmFormatModifierPlaneCount;
            break;
        }
    }

    return chosen;
}

static bool MemoryTypeFor(VkPhysicalDevice physicalDevice, uint32_t typeBits, VkMemoryPropertyFlags wanted, uint32_t& typeIndex,
                          VkMemoryPropertyFlags* found)
{
    auto getMemoryProperties = InstanceFunction<PFN_vkGetPhysicalDeviceMemoryProperties>("vkGetPhysicalDeviceMemoryProperties");
    if (!getMemoryProperties) return false;

    VkPhysicalDeviceMemoryProperties properties{};
    getMemoryProperties(physicalDevice, &properties);

    for (uint32_t index = 0; index < properties.memoryTypeCount; ++index)
    {
        if ((typeBits & (1u << index)) == 0) continue;
        if ((properties.memoryTypes[index].propertyFlags & wanted) != wanted) continue;

        typeIndex = index;
        if (found) *found = properties.memoryTypes[index].propertyFlags;
        return true;
    }

    return false;
}

static void DestroyImages(Swapchain& swapchain)
{
    const DeviceFunctions& functions = swapchain.record->functions;

    for (HostedImage& image : swapchain.images)
    {
        if (image.exportSemaphore != VK_NULL_HANDLE) functions.destroySemaphore(swapchain.device, image.exportSemaphore, nullptr);
        if (image.image != VK_NULL_HANDLE) functions.destroyImage(swapchain.device, image.image, nullptr);
        if (image.memory != VK_NULL_HANDLE) functions.freeMemory(swapchain.device, image.memory, nullptr);
    }

    swapchain.images.clear();
}

static bool CreateImages(Swapchain& swapchain, uint32_t count, const std::vector<uint64_t>& modifiers)
{
    const DeviceRecord& record = *swapchain.record;
    const DeviceFunctions& functions = record.functions;
    bool sharing = !modifiers.empty();

    swapchain.images.resize(count);

    for (HostedImage& hosted : swapchain.images)
    {
        VkImageDrmFormatModifierListCreateInfoEXT modifierList{};
        modifierList.sType = VK_STRUCTURE_TYPE_IMAGE_DRM_FORMAT_MODIFIER_LIST_CREATE_INFO_EXT;
        modifierList.drmFormatModifierCount = (uint32_t)modifiers.size();
        modifierList.pDrmFormatModifiers = modifiers.data();

        VkExternalMemoryImageCreateInfo external{};
        external.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO;
        external.pNext = &modifierList;
        external.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT;

        VkImageCreateInfo info{};
        info.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
        info.pNext = sharing ? &external : nullptr;
        info.imageType = VK_IMAGE_TYPE_2D;
        info.format = swapchain.format;
        info.extent = {swapchain.extent.width, swapchain.extent.height, 1};
        info.mipLevels = 1;
        info.arrayLayers = 1;
        info.samples = VK_SAMPLE_COUNT_1_BIT;
        info.tiling = sharing ? VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT : VK_IMAGE_TILING_OPTIMAL;
        info.usage = swapchain.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        info.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
        info.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;

        if (functions.createImage(swapchain.device, &info, nullptr, &hosted.image) != VK_SUCCESS) return false;

        VkMemoryRequirements requirements{};
        functions.imageRequirements(swapchain.device, hosted.image, &requirements);

        uint32_t typeIndex = 0;
        if (!MemoryTypeFor(record.physicalDevice, requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, typeIndex, nullptr) &&
            !MemoryTypeFor(record.physicalDevice, requirements.memoryTypeBits, 0, typeIndex, nullptr))
        {
            return false;
        }

        VkMemoryDedicatedAllocateInfo dedicated{};
        dedicated.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO;
        dedicated.image = hosted.image;

        VkExportMemoryAllocateInfo exported{};
        exported.sType = VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO;
        exported.pNext = &dedicated;
        exported.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT;

        VkMemoryAllocateInfo allocation{};
        allocation.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
        allocation.pNext = sharing ? (const void*)&exported : (const void*)&dedicated;
        allocation.allocationSize = requirements.size;
        allocation.memoryTypeIndex = typeIndex;

        if (functions.allocateMemory(swapchain.device, &allocation, nullptr, &hosted.memory) != VK_SUCCESS) return false;
        if (functions.bindImageMemory(swapchain.device, hosted.image, hosted.memory, 0) != VK_SUCCESS) return false;

        if (!sharing || !record.semaphoreDescriptors) continue;

        VkExportSemaphoreCreateInfo exportSemaphore{};
        exportSemaphore.sType = VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO;
        exportSemaphore.handleTypes = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT;

        VkSemaphoreCreateInfo semaphoreInfo{};
        semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
        semaphoreInfo.pNext = &exportSemaphore;

        if (functions.createSemaphore(swapchain.device, &semaphoreInfo, nullptr, &hosted.exportSemaphore) != VK_SUCCESS)
        {
            hosted.exportSemaphore = VK_NULL_HANDLE;
        }
    }

    return true;
}

static bool RegisterImages(Swapchain& swapchain, const std::unordered_map<uint64_t, uint32_t>& planeCounts)
{
    const DeviceFunctions& functions = swapchain.record->functions;

    for (uint32_t index = 0; index < swapchain.images.size(); ++index)
    {
        HostedImage& hosted = swapchain.images[index];

        VkImageDrmFormatModifierPropertiesEXT modifier{};
        modifier.sType = VK_STRUCTURE_TYPE_IMAGE_DRM_FORMAT_MODIFIER_PROPERTIES_EXT;
        if (functions.modifierProperties(swapchain.device, hosted.image, &modifier) != VK_SUCCESS) return false;

        auto counted = planeCounts.find(modifier.drmFormatModifier);
        if (counted == planeCounts.end() || counted->second == 0 || counted->second > ControlChannel::MAX_PLANES) return false;

        uint32_t planeCount = counted->second;

        VkMemoryGetFdInfoKHR request{};
        request.sType = VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR;
        request.memory = hosted.memory;
        request.handleType = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT;

        int descriptor = -1;
        if (functions.memoryDescriptor(swapchain.device, &request, &descriptor) != VK_SUCCESS || descriptor < 0) return false;

        HostedWindow::BufferDescription description{};
        description.generation = swapchain.generation;
        description.index = index;
        description.kind = ControlChannel::BufferKind::Dmabuf;
        description.width = swapchain.extent.width;
        description.height = swapchain.extent.height;
        description.format = swapchain.drmFormat;
        description.modifier = modifier.drmFormatModifier;
        description.planeCount = planeCount;

        int descriptors[ControlChannel::MAX_PLANES] = {-1, -1, -1, -1};

        for (uint32_t plane = 0; plane < planeCount; ++plane)
        {
            VkImageSubresource subresource{};
            subresource.aspectMask = (VkImageAspectFlags)(VK_IMAGE_ASPECT_MEMORY_PLANE_0_BIT_EXT << plane);

            VkSubresourceLayout layout{};
            functions.subresourceLayout(swapchain.device, hosted.image, &subresource, &layout);

            description.offsets[plane] = (uint32_t)layout.offset;
            description.strides[plane] = (uint32_t)layout.rowPitch;
            descriptors[plane] = plane == 0 ? descriptor : dup(descriptor);
        }

        HostedWindow::Result registered = HostedWindow::RegisterBuffer(*gSession, description, descriptors, planeCount);

        for (uint32_t plane = 0; plane < planeCount; ++plane)
        {
            if (descriptors[plane] >= 0) close(descriptors[plane]);
        }

        if (registered != HostedWindow::Result::Ok) return false;
    }

    return true;
}

static void HandleRelease(void* context, uint32_t generation, uint32_t index, bool refused)
{
    Swapchain* swapchain = static_cast<Swapchain*>(context);

    {
        std::lock_guard<std::mutex> guard(swapchain->mutex);

        if (generation != swapchain->generation) return;

        if (refused) swapchain->refused = true;
        else swapchain->releasedIndices.push_back(index);
    }

    swapchain->released.notify_all();
}

static void ReclaimReleased(Swapchain& swapchain)
{
    std::lock_guard<std::mutex> guard(swapchain.mutex);

    for (uint32_t index : swapchain.releasedIndices)
    {
        if (index < swapchain.images.size()) swapchain.images[index].held = false;
    }

    swapchain.releasedIndices.clear();

    if (!swapchain.refused || !swapchain.sharing) return;

    fprintf(stderr, "[vulkanwsi] the window host cannot show the gpu images, so each frame is copied to it instead\n");
    swapchain.sharing = false;
    swapchain.refused = false;

    for (HostedImage& image : swapchain.images) image.held = false;
}

static bool PrepareCopy(Swapchain& swapchain, VkQueue queue)
{
    if (swapchain.commandPool != VK_NULL_HANDLE) return true;

    const DeviceRecord& record = *swapchain.record;
    const DeviceFunctions& functions = record.functions;

    uint32_t family = record.queueFamily;

    {
        std::lock_guard<std::mutex> guard(gMutex);
        auto found = gQueueFamilies.find(queue);
        if (found != gQueueFamilies.end()) family = found->second;
    }

    VkDeviceSize sizeBytes = (VkDeviceSize)swapchain.extent.width * swapchain.extent.height * 4;

    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = sizeBytes;
    bufferInfo.usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if (functions.createBuffer(swapchain.device, &bufferInfo, nullptr, &swapchain.staging) != VK_SUCCESS) return false;

    VkMemoryRequirements requirements{};
    functions.bufferRequirements(swapchain.device, swapchain.staging, &requirements);

    uint32_t typeIndex = 0;
    VkMemoryPropertyFlags flags = 0;

    if (!MemoryTypeFor(record.physicalDevice, requirements.memoryTypeBits,
                       VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT, typeIndex, &flags) &&
        !MemoryTypeFor(record.physicalDevice, requirements.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, typeIndex, &flags))
    {
        return false;
    }

    VkMemoryAllocateInfo allocation{};
    allocation.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocation.allocationSize = requirements.size;
    allocation.memoryTypeIndex = typeIndex;

    if (functions.allocateMemory(swapchain.device, &allocation, nullptr, &swapchain.stagingMemory) != VK_SUCCESS) return false;
    if (functions.bindBufferMemory(swapchain.device, swapchain.staging, swapchain.stagingMemory, 0) != VK_SUCCESS) return false;
    if (functions.mapMemory(swapchain.device, swapchain.stagingMemory, 0, VK_WHOLE_SIZE, 0, &swapchain.stagingPixels) != VK_SUCCESS) return false;

    swapchain.stagingCoherent = (flags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0;

    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = family;

    if (functions.createCommandPool(swapchain.device, &poolInfo, nullptr, &swapchain.commandPool) != VK_SUCCESS) return false;

    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    if (functions.createFence(swapchain.device, &fenceInfo, nullptr, &swapchain.copyFence) != VK_SUCCESS) return false;

    for (HostedImage& hosted : swapchain.images)
    {
        VkCommandBufferAllocateInfo allocate{};
        allocate.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
        allocate.commandPool = swapchain.commandPool;
        allocate.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        allocate.commandBufferCount = 1;

        if (functions.allocateCommandBuffers(swapchain.device, &allocate, &hosted.copyCommands) != VK_SUCCESS) return false;

        VkCommandBufferBeginInfo begin{};
        begin.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
        functions.beginCommandBuffer(hosted.copyCommands, &begin);

        VkImageMemoryBarrier barrier{};
        barrier.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        barrier.srcAccessMask = VK_ACCESS_MEMORY_WRITE_BIT;
        barrier.dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
        barrier.oldLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
        barrier.newLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
        barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        barrier.image = hosted.image;
        barrier.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};

        functions.pipelineBarrier(hosted.copyCommands, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0,
                                  nullptr, 1, &barrier);

        VkBufferImageCopy region{};
        region.imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        region.imageExtent = {swapchain.extent.width, swapchain.extent.height, 1};
        functions.copyImageToBuffer(hosted.copyCommands, hosted.image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, swapchain.staging, 1, &region);

        barrier.srcAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
        barrier.dstAccessMask = VK_ACCESS_MEMORY_READ_BIT;
        barrier.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
        barrier.newLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
        functions.pipelineBarrier(hosted.copyCommands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, 0, nullptr, 0,
                                  nullptr, 1, &barrier);

        functions.endCommandBuffer(hosted.copyCommands);
    }

    return true;
}

static void ReleaseCopyResources(Swapchain& swapchain)
{
    const DeviceFunctions& functions = swapchain.record->functions;

    if (swapchain.commandPool != VK_NULL_HANDLE) functions.destroyCommandPool(swapchain.device, swapchain.commandPool, nullptr);
    if (swapchain.copyFence != VK_NULL_HANDLE) functions.destroyFence(swapchain.device, swapchain.copyFence, nullptr);
    if (swapchain.stagingPixels) functions.unmapMemory(swapchain.device, swapchain.stagingMemory);
    if (swapchain.staging != VK_NULL_HANDLE) functions.destroyBuffer(swapchain.device, swapchain.staging, nullptr);
    if (swapchain.stagingMemory != VK_NULL_HANDLE) functions.freeMemory(swapchain.device, swapchain.stagingMemory, nullptr);

    swapchain.commandPool = VK_NULL_HANDLE;
    swapchain.copyFence = VK_NULL_HANDLE;
    swapchain.stagingPixels = nullptr;
    swapchain.staging = VK_NULL_HANDLE;
    swapchain.stagingMemory = VK_NULL_HANDLE;
}

static bool PacingWanted(const Swapchain& swapchain)
{
    static const bool unpaced = []() {
        const char* setting = Settings::Lookup(VulkanWsi::PRESENT_WITHOUT_WAITING_SETTING);
        return setting && strcmp(setting, "0") != 0;
    }();

    return !unpaced && (swapchain.presentMode == VK_PRESENT_MODE_FIFO_KHR || swapchain.presentMode == VK_PRESENT_MODE_FIFO_RELAXED_KHR);
}

static void SignalAcquired(Swapchain& swapchain, VkSemaphore semaphore, VkFence fence)
{
    const DeviceRecord& record = *swapchain.record;
    const DeviceFunctions& functions = record.functions;

    bool semaphoreDone = semaphore == VK_NULL_HANDLE;
    bool fenceDone = fence == VK_NULL_HANDLE;

    if (!semaphoreDone && record.semaphoreDescriptors && functions.importSemaphore)
    {
        VkImportSemaphoreFdInfoKHR import{};
        import.sType = VK_STRUCTURE_TYPE_IMPORT_SEMAPHORE_FD_INFO_KHR;
        import.semaphore = semaphore;
        import.flags = VK_SEMAPHORE_IMPORT_TEMPORARY_BIT;
        import.handleType = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT;
        import.fd = -1;
        semaphoreDone = functions.importSemaphore(swapchain.device, &import) == VK_SUCCESS;
    }

    if (!fenceDone && record.fenceDescriptors && functions.importFence)
    {
        VkImportFenceFdInfoKHR import{};
        import.sType = VK_STRUCTURE_TYPE_IMPORT_FENCE_FD_INFO_KHR;
        import.fence = fence;
        import.flags = VK_FENCE_IMPORT_TEMPORARY_BIT;
        import.handleType = VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT;
        import.fd = -1;
        fenceDone = functions.importFence(swapchain.device, &import) == VK_SUCCESS;
    }

    if (semaphoreDone && fenceDone) return;
    if (record.queue == VK_NULL_HANDLE) return;

    VkSubmitInfo submit{};
    submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit.signalSemaphoreCount = semaphoreDone ? 0 : 1;
    submit.pSignalSemaphores = &semaphore;

    functions.queueSubmit(record.queue, 1, &submit, fenceDone ? VK_NULL_HANDLE : fence);
}

void HostedSwapchain::Initialize(PFN_vkGetInstanceProcAddr getInstanceProcAddr, PFN_vkGetDeviceProcAddr getDeviceProcAddr)
{
    gGetInstanceProcAddr = getInstanceProcAddr;
    gGetDeviceProcAddr = getDeviceProcAddr;
}

void HostedSwapchain::Attach(HostedWindow::Session* session, uint32_t width, uint32_t height)
{
    gSession = session;
    gWidth = width;
    gHeight = height;
}

bool HostedSwapchain::IsActive()
{
    return HostedWindow::IsRequested();
}

void HostedSwapchain::NoteInstance(VkInstance instance)
{
    gInstance = instance;
}

void HostedSwapchain::AddDeviceExtensions(VkPhysicalDevice physicalDevice, std::vector<const char*>& extensions)
{
    auto enumerate = InstanceFunction<PFN_vkEnumerateDeviceExtensionProperties>("vkEnumerateDeviceExtensionProperties");
    if (!enumerate) return;

    uint32_t count = 0;
    if (enumerate(physicalDevice, nullptr, &count, nullptr) != VK_SUCCESS || count == 0) return;

    std::vector<VkExtensionProperties> supported(count);
    if (enumerate(physicalDevice, nullptr, &count, supported.data()) != VK_SUCCESS) return;

    for (const char* wanted : WANTED_DEVICE_EXTENSIONS)
    {
        if (Enabled(extensions, wanted)) continue;

        for (const VkExtensionProperties& offered : supported)
        {
            if (strcmp(offered.extensionName, wanted) != 0) continue;

            extensions.push_back(wanted);
            break;
        }
    }
}

void HostedSwapchain::NoteDevice(VkPhysicalDevice physicalDevice, VkDevice device, const std::vector<const char*>& extensions)
{
    std::unique_ptr<DeviceRecord> record = std::make_unique<DeviceRecord>();
    record->physicalDevice = physicalDevice;
    record->sharing = Enabled(extensions, "VK_KHR_external_memory_fd") && Enabled(extensions, "VK_EXT_external_memory_dma_buf") &&
                      Enabled(extensions, "VK_EXT_image_drm_format_modifier");
    record->semaphoreDescriptors = Enabled(extensions, "VK_KHR_external_semaphore_fd");
    record->fenceDescriptors = Enabled(extensions, "VK_KHR_external_fence_fd");
    LoadDeviceFunctions(device, record->functions);

    std::lock_guard<std::mutex> guard(gMutex);
    gDevices[device] = std::move(record);
}

void HostedSwapchain::NoteQueue(VkDevice device, uint32_t family, VkQueue queue)
{
    std::lock_guard<std::mutex> guard(gMutex);

    gQueueFamilies[queue] = family;

    auto found = gDevices.find(device);
    if (found == gDevices.end() || found->second->queue != VK_NULL_HANDLE) return;

    found->second->queue = queue;
    found->second->queueFamily = family;
}

VkResult HostedSwapchain::CreateSurface(VkSurfaceKHR* surface)
{
    if (!surface) return VK_ERROR_INITIALIZATION_FAILED;

    FakeSurface* created = new FakeSurface;
    *surface = reinterpret_cast<VkSurfaceKHR>(created);

    std::lock_guard<std::mutex> guard(gMutex);
    gSurfaces.insert(*surface);

    fprintf(stderr, "[vulkanwsi] the guest window surface is backed by the window host\n");
    return VK_SUCCESS;
}

bool HostedSwapchain::OwnsSurface(VkSurfaceKHR surface)
{
    std::lock_guard<std::mutex> guard(gMutex);
    return gSurfaces.count(surface) != 0;
}

void HostedSwapchain::DestroySurface(VkSurfaceKHR surface)
{
    {
        std::lock_guard<std::mutex> guard(gMutex);
        if (gSurfaces.erase(surface) == 0) return;
    }

    delete reinterpret_cast<FakeSurface*>(surface);
}

VkResult HostedSwapchain::SurfaceSupport(VkBool32* supported)
{
    if (supported) *supported = VK_TRUE;
    return VK_SUCCESS;
}

VkResult HostedSwapchain::SurfaceCapabilities(VkSurfaceCapabilitiesKHR* capabilities)
{
    if (!capabilities) return VK_ERROR_INITIALIZATION_FAILED;

    uint32_t width = gWidth;
    uint32_t height = gHeight;
    if (gSession) HostedWindow::CurrentSize(*gSession, width, height);

    *capabilities = VkSurfaceCapabilitiesKHR{};
    capabilities->minImageCount = MIN_IMAGES;
    capabilities->maxImageCount = ControlChannel::MAX_FRAME_BUFFERS;
    capabilities->currentExtent = {width, height};
    capabilities->minImageExtent = {1, 1};
    capabilities->maxImageExtent = {WaylandClient::MAX_DIMENSION, WaylandClient::MAX_DIMENSION};
    capabilities->maxImageArrayLayers = 1;
    capabilities->supportedTransforms = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    capabilities->currentTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    capabilities->supportedCompositeAlpha =
        VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR | VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR | VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR;
    capabilities->supportedUsageFlags = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT |
                                        VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT;

    return VK_SUCCESS;
}

VkResult HostedSwapchain::SurfaceFormats(uint32_t* count, VkSurfaceFormatKHR* formats)
{
    static const VkFormat OFFERED[] = {VK_FORMAT_B8G8R8A8_UNORM, VK_FORMAT_B8G8R8A8_SRGB, VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_R8G8B8A8_SRGB};
    const uint32_t offered = (uint32_t)(sizeof(OFFERED) / sizeof(OFFERED[0]));

    if (!count) return VK_ERROR_INITIALIZATION_FAILED;

    if (!formats)
    {
        *count = offered;
        return VK_SUCCESS;
    }

    uint32_t written = std::min(*count, offered);
    for (uint32_t index = 0; index < written; ++index)
    {
        formats[index].format = OFFERED[index];
        formats[index].colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    }

    *count = written;
    return written < offered ? VK_INCOMPLETE : VK_SUCCESS;
}

VkResult HostedSwapchain::PresentModes(uint32_t* count, VkPresentModeKHR* modes)
{
    static const VkPresentModeKHR OFFERED[] = {VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_IMMEDIATE_KHR};
    const uint32_t offered = (uint32_t)(sizeof(OFFERED) / sizeof(OFFERED[0]));

    if (!count) return VK_ERROR_INITIALIZATION_FAILED;

    if (!modes)
    {
        *count = offered;
        return VK_SUCCESS;
    }

    uint32_t written = std::min(*count, offered);
    for (uint32_t index = 0; index < written; ++index) modes[index] = OFFERED[index];

    *count = written;
    return written < offered ? VK_INCOMPLETE : VK_SUCCESS;
}

bool HostedSwapchain::OwnsSwapchain(VkSwapchainKHR swapchain)
{
    std::lock_guard<std::mutex> guard(gMutex);
    return gSwapchains.count(swapchain) != 0;
}

VkResult HostedSwapchain::CreateSwapchain(VkDevice device, const VkSwapchainCreateInfoKHR* createInfo, VkSwapchainKHR* handle)
{
    DeviceRecord* record = RecordFor(device);

    if (!createInfo || !handle || !record || !gSession) return VK_ERROR_INITIALIZATION_FAILED;

    bool opaque = createInfo->compositeAlpha != VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR &&
                  createInfo->compositeAlpha != VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR;
    uint32_t drmFormat = DrmFormatFor(createInfo->imageFormat, opaque);

    if (drmFormat == 0)
    {
        fprintf(stderr, "[vulkanwsi] the guest wants swapchain format %u, which the window host cannot take\n", (unsigned)createInfo->imageFormat);
        return VK_ERROR_FORMAT_NOT_SUPPORTED;
    }

    Swapchain* swapchain = new Swapchain;
    swapchain->device = device;
    swapchain->record = record;
    swapchain->format = createInfo->imageFormat;
    swapchain->drmFormat = drmFormat;
    swapchain->extent = createInfo->imageExtent;
    swapchain->usage = createInfo->imageUsage;
    swapchain->presentMode = createInfo->presentMode;
    swapchain->generation = HostedWindow::ReserveGeneration(*gSession);

    uint32_t count = std::max(createInfo->minImageCount, PREFERRED_IMAGES);
    count = std::min(std::max(count, MIN_IMAGES), ControlChannel::MAX_FRAME_BUFFERS);

    std::unordered_map<uint64_t, uint32_t> planeCounts;
    std::vector<uint64_t> modifiers = SharableModifiers(*record, createInfo->imageFormat, drmFormat, planeCounts);

    bool sharing = !modifiers.empty() && CreateImages(*swapchain, count, modifiers) && RegisterImages(*swapchain, planeCounts);

    if (!sharing)
    {
        DestroyImages(*swapchain);
        HostedWindow::ForgetBuffers(*gSession, swapchain->generation);

        if (!CreateImages(*swapchain, count, std::vector<uint64_t>()))
        {
            DestroyImages(*swapchain);
            delete swapchain;
            return VK_ERROR_OUT_OF_DEVICE_MEMORY;
        }
    }

    swapchain->sharing = sharing;
    *handle = reinterpret_cast<VkSwapchainKHR>(swapchain);

    {
        std::lock_guard<std::mutex> guard(gMutex);
        gSwapchains.insert(*handle);
        gStatistics.swapchainsCreated += 1;
        gListening = swapchain;
    }

    HostedWindow::SetReleaseListener(*gSession, &HandleRelease, swapchain);

    fprintf(stderr, "[vulkanwsi] the guest swapchain has %u %ux%u image(s) whose frames are %s the window host\n", count, swapchain->extent.width,
            swapchain->extent.height, sharing ? "shared with" : "copied to");

    return VK_SUCCESS;
}

void HostedSwapchain::DestroySwapchain(VkDevice, VkSwapchainKHR handle)
{
    Swapchain* swapchain = reinterpret_cast<Swapchain*>(handle);
    bool listening = false;

    {
        std::lock_guard<std::mutex> guard(gMutex);
        if (gSwapchains.erase(handle) == 0) return;

        listening = gListening == swapchain;
        if (listening) gListening = nullptr;
    }

    if (gSession)
    {
        if (listening) HostedWindow::SetReleaseListener(*gSession, nullptr, nullptr);
        HostedWindow::ForgetBuffers(*gSession, swapchain->generation);
    }

    swapchain->record->functions.deviceWaitIdle(swapchain->device);
    ReleaseCopyResources(*swapchain);
    DestroyImages(*swapchain);

    delete swapchain;
}

VkResult HostedSwapchain::GetImages(VkSwapchainKHR handle, uint32_t* count, VkImage* images)
{
    if (!count) return VK_ERROR_INITIALIZATION_FAILED;

    Swapchain* swapchain = reinterpret_cast<Swapchain*>(handle);
    uint32_t available = (uint32_t)swapchain->images.size();

    if (!images)
    {
        *count = available;
        return VK_SUCCESS;
    }

    uint32_t written = std::min(*count, available);
    for (uint32_t index = 0; index < written; ++index) images[index] = swapchain->images[index].image;

    *count = written;
    return written < available ? VK_INCOMPLETE : VK_SUCCESS;
}

VkResult HostedSwapchain::Acquire(VkDevice, VkSwapchainKHR handle, uint64_t timeout, VkSemaphore semaphore, VkFence fence, uint32_t* index)
{
    Swapchain* swapchain = reinterpret_cast<Swapchain*>(handle);
    if (!index) return VK_ERROR_INITIALIZATION_FAILED;

    auto deadline = std::chrono::steady_clock::now() +
                    std::chrono::nanoseconds(timeout > (uint64_t)INT64_MAX / 2 ? (uint64_t)INT64_MAX / 2 : timeout);

    for (;;)
    {
        ReclaimReleased(*swapchain);

        for (uint32_t candidate = 0; candidate < swapchain->images.size(); ++candidate)
        {
            HostedImage& image = swapchain->images[candidate];
            if (image.held || image.acquired) continue;

            image.acquired = true;
            *index = candidate;
            SignalAcquired(*swapchain, semaphore, fence);
            return VK_SUCCESS;
        }

        if (timeout == 0) return VK_NOT_READY;
        if (gSession && HostedWindow::IsStopRequested(*gSession)) return VK_ERROR_SURFACE_LOST_KHR;

        std::unique_lock<std::mutex> guard(swapchain->mutex);
        bool woke = swapchain->released.wait_until(guard, deadline, [swapchain] { return !swapchain->releasedIndices.empty() || swapchain->refused; });

        if (!woke) return VK_TIMEOUT;
    }
}

static void CopyRows(Swapchain& swapchain, HostedWindow::Buffer& buffer)
{
    const uint8_t* source = (const uint8_t*)swapchain.stagingPixels;
    uint32_t sourceStride = swapchain.extent.width * 4;
    uint32_t rows = std::min(swapchain.extent.height, gSession->height);
    uint32_t columns = std::min(swapchain.extent.width, gSession->width);
    bool swapRedBlue = swapchain.drmFormat == ControlChannel::FORMAT_ABGR8888 || swapchain.drmFormat == ControlChannel::FORMAT_XBGR8888;

    for (uint32_t row = 0; row < rows; ++row)
    {
        uint8_t* destination = buffer.pixels + (size_t)row * gSession->stride;
        const uint8_t* line = source + (size_t)row * sourceStride;

        if (!swapRedBlue)
        {
            memcpy(destination, line, (size_t)columns * 4);
            continue;
        }

        for (uint32_t pixel = 0; pixel < columns; ++pixel)
        {
            destination[pixel * 4 + 0] = line[pixel * 4 + 2];
            destination[pixel * 4 + 1] = line[pixel * 4 + 1];
            destination[pixel * 4 + 2] = line[pixel * 4 + 0];
            destination[pixel * 4 + 3] = line[pixel * 4 + 3];
        }
    }
}

static VkResult CopyToHost(Swapchain& swapchain, VkQueue queue, uint32_t index, const VkSemaphore* waits, uint32_t waitCount,
                           const VkPipelineStageFlags* stages)
{
    const DeviceFunctions& functions = swapchain.record->functions;

    if (!PrepareCopy(swapchain, queue))
    {
        fprintf(stderr, "[vulkanwsi] the frame cannot be copied for the window host\n");
        ReleaseCopyResources(swapchain);
        return VK_ERROR_OUT_OF_DEVICE_MEMORY;
    }

    VkSubmitInfo submit{};
    submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit.waitSemaphoreCount = waitCount;
    submit.pWaitSemaphores = waits;
    submit.pWaitDstStageMask = stages;
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &swapchain.images[index].copyCommands;

    VkResult submitted = functions.queueSubmit(queue, 1, &submit, swapchain.copyFence);
    if (submitted != VK_SUCCESS) return submitted;

    functions.waitForFences(swapchain.device, 1, &swapchain.copyFence, VK_TRUE, UINT64_MAX);
    functions.resetFences(swapchain.device, 1, &swapchain.copyFence);

    if (!swapchain.stagingCoherent)
    {
        VkMappedMemoryRange range{};
        range.sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE;
        range.memory = swapchain.stagingMemory;
        range.size = VK_WHOLE_SIZE;
        functions.invalidateRanges(swapchain.device, 1, &range);
    }

    HostedWindow::Buffer* buffer = nullptr;
    uint32_t bufferIndex = HostedWindow::INVALID_BUFFER_INDEX;

    if (HostedWindow::AcquireBuffer(*gSession, buffer, bufferIndex) == HostedWindow::Result::Ok)
    {
        CopyRows(swapchain, *buffer);
        HostedWindow::Present(*gSession, bufferIndex, 0);

        std::lock_guard<std::mutex> guard(gMutex);
        gStatistics.framesCopied += 1;
    }

    return VK_SUCCESS;
}

static VkResult ShareWithHost(Swapchain& swapchain, VkQueue queue, uint32_t index, const VkSemaphore* waits, uint32_t waitCount,
                              const VkPipelineStageFlags* stages)
{
    const DeviceFunctions& functions = swapchain.record->functions;
    HostedImage& image = swapchain.images[index];

    VkSubmitInfo submit{};
    submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit.waitSemaphoreCount = waitCount;
    submit.pWaitSemaphores = waits;
    submit.pWaitDstStageMask = stages;
    submit.signalSemaphoreCount = image.exportSemaphore != VK_NULL_HANDLE ? 1 : 0;
    submit.pSignalSemaphores = &image.exportSemaphore;

    VkResult submitted = functions.queueSubmit(queue, 1, &submit, VK_NULL_HANDLE);
    if (submitted != VK_SUCCESS) return submitted;

    int fence = -1;

    if (image.exportSemaphore != VK_NULL_HANDLE)
    {
        VkSemaphoreGetFdInfoKHR request{};
        request.sType = VK_STRUCTURE_TYPE_SEMAPHORE_GET_FD_INFO_KHR;
        request.semaphore = image.exportSemaphore;
        request.handleType = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT;

        if (functions.semaphoreDescriptor(swapchain.device, &request, &fence) != VK_SUCCESS) fence = -1;
    }

    if (fence < 0) functions.queueWaitIdle(queue);

    {
        std::lock_guard<std::mutex> guard(swapchain.mutex);
        image.held = true;
    }

    HostedWindow::PresentBuffer(*gSession, swapchain.generation, index, fence);
    if (fence >= 0) close(fence);

    std::lock_guard<std::mutex> guard(gMutex);
    gStatistics.framesShared += 1;

    return VK_SUCCESS;
}

VkResult HostedSwapchain::Present(VkQueue queue, VkSwapchainKHR handle, uint32_t index, const VkSemaphore* waits, uint32_t waitCount)
{
    Swapchain* swapchain = reinterpret_cast<Swapchain*>(handle);

    if (!gSession || index >= swapchain->images.size() || !swapchain->images[index].acquired) return VK_ERROR_OUT_OF_DATE_KHR;

    std::vector<VkPipelineStageFlags> stages(waitCount, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
    uint64_t completed = HostedWindow::PresentsCompleted(*gSession);

    ReclaimReleased(*swapchain);

    VkResult presented = swapchain->sharing ? ShareWithHost(*swapchain, queue, index, waits, waitCount, stages.data())
                                            : CopyToHost(*swapchain, queue, index, waits, waitCount, stages.data());

    swapchain->images[index].acquired = false;

    if (presented != VK_SUCCESS) return presented;

    if (PacingWanted(*swapchain)) HostedWindow::WaitForPresentAfter(*gSession, completed, PACING_TIMEOUT_MILLIS);
    if (HostedWindow::IsStopRequested(*gSession)) return VK_ERROR_SURFACE_LOST_KHR;

    uint32_t width = 0;
    uint32_t height = 0;
    HostedWindow::CurrentSize(*gSession, width, height);

    return width == swapchain->extent.width && height == swapchain->extent.height ? VK_SUCCESS : VK_SUBOPTIMAL_KHR;
}

HostedSwapchain::Statistics HostedSwapchain::GetStatistics()
{
    std::lock_guard<std::mutex> guard(gMutex);
    return gStatistics;
}

#include "Overlay/OverlayVulkan.hpp"

#include <cstdio>
#include <cstring>
#include <map>
#include <mutex>
#include <vector>

#include "Overlay/FrameRate.hpp"
#include "Overlay/Overlay.hpp"
#include "Overlay/OverlayFont.hpp"
#include "Settings/Settings.hpp"

namespace OverlayVulkan
{
    struct Api
    {
        PFN_vkGetPhysicalDeviceMemoryProperties GetMemoryProperties = nullptr;
        PFN_vkGetSwapchainImagesKHR GetSwapchainImages = nullptr;
        PFN_vkCreateCommandPool CreateCommandPool = nullptr;
        PFN_vkDestroyCommandPool DestroyCommandPool = nullptr;
        PFN_vkAllocateCommandBuffers AllocateCommandBuffers = nullptr;
        PFN_vkBeginCommandBuffer BeginCommandBuffer = nullptr;
        PFN_vkEndCommandBuffer EndCommandBuffer = nullptr;
        PFN_vkResetCommandBuffer ResetCommandBuffer = nullptr;
        PFN_vkCmdPipelineBarrier CmdPipelineBarrier = nullptr;
        PFN_vkCmdCopyBufferToImage CmdCopyBufferToImage = nullptr;
        PFN_vkCmdCopyImageToBuffer CmdCopyImageToBuffer = nullptr;
        PFN_vkCreateBuffer CreateBuffer = nullptr;
        PFN_vkDestroyBuffer DestroyBuffer = nullptr;
        PFN_vkGetBufferMemoryRequirements GetBufferMemoryRequirements = nullptr;
        PFN_vkAllocateMemory AllocateMemory = nullptr;
        PFN_vkFreeMemory FreeMemory = nullptr;
        PFN_vkBindBufferMemory BindBufferMemory = nullptr;
        PFN_vkMapMemory MapMemory = nullptr;
        PFN_vkUnmapMemory UnmapMemory = nullptr;
        PFN_vkCreateSemaphore CreateSemaphore = nullptr;
        PFN_vkDestroySemaphore DestroySemaphore = nullptr;
        PFN_vkCreateFence CreateFence = nullptr;
        PFN_vkDestroyFence DestroyFence = nullptr;
        PFN_vkWaitForFences WaitForFences = nullptr;
        PFN_vkResetFences ResetFences = nullptr;
        PFN_vkQueueSubmit QueueSubmit = nullptr;
    };

    struct Slot
    {
        VkCommandBuffer commands = VK_NULL_HANDLE;
        VkSemaphore finished = VK_NULL_HANDLE;
        VkFence fence = VK_NULL_HANDLE;
        bool submitted = false;
    };

    struct Target
    {
        VkDevice device = VK_NULL_HANDLE;
        VkFormat format = VK_FORMAT_UNDEFINED;
        VkExtent2D extent = {};
        bool copyable = false;
        bool ready = false;
        bool broken = false;
        std::vector<VkImage> images;
        std::vector<Slot> slots;
        VkCommandPool pool = VK_NULL_HANDLE;
        VkBuffer staging = VK_NULL_HANDLE;
        VkDeviceMemory stagingMemory = VK_NULL_HANDLE;
        void* stagingMapping = nullptr;
        uint64_t stagingBytes = 0;
        VkBuffer readback = VK_NULL_HANDLE;
        VkDeviceMemory readbackMemory = VK_NULL_HANDLE;
        void* readbackMapping = nullptr;
        uint64_t readbackBytes = 0;
        bool captured = false;
        int panelWidth = 0;
        int panelHeight = 0;
        std::string caption;
    };

    static std::mutex gMutex;
    static Api gApi;
    static bool gEnabled = false;
    static const char* gFailure = nullptr;
    static VkPhysicalDevice gPhysicalDevice = VK_NULL_HANDLE;
    static std::map<VkQueue, uint32_t> gQueueFamilies;
    static std::map<VkQueue, VkDevice> gQueueDevices;
    static std::map<VkSwapchainKHR, Target> gTargets;
}

static void Fail(const char* reason)
{
    if (OverlayVulkan::gFailure) return;

    OverlayVulkan::gFailure = reason;
    fprintf(stderr, "[overlay] the frame counter cannot be drawn into the guest's frames: %s\n", reason);
}

bool OverlayVulkan::Enable(OverlayVulkan::Resolver resolve)
{
    if (!resolve) return false;

    OverlayVulkan::Api& api = OverlayVulkan::gApi;

#define RESOLVE_OVERLAY(field, name)                                          \
    do                                                                        \
    {                                                                         \
        api.field = (PFN_##name)resolve(#name);                               \
        if (!api.field)                                                       \
        {                                                                     \
            Fail("the host driver does not expose " #name);                   \
            return false;                                                     \
        }                                                                     \
    } while (false)

    RESOLVE_OVERLAY(GetMemoryProperties, vkGetPhysicalDeviceMemoryProperties);
    RESOLVE_OVERLAY(GetSwapchainImages, vkGetSwapchainImagesKHR);
    RESOLVE_OVERLAY(CreateCommandPool, vkCreateCommandPool);
    RESOLVE_OVERLAY(DestroyCommandPool, vkDestroyCommandPool);
    RESOLVE_OVERLAY(AllocateCommandBuffers, vkAllocateCommandBuffers);
    RESOLVE_OVERLAY(BeginCommandBuffer, vkBeginCommandBuffer);
    RESOLVE_OVERLAY(EndCommandBuffer, vkEndCommandBuffer);
    RESOLVE_OVERLAY(ResetCommandBuffer, vkResetCommandBuffer);
    RESOLVE_OVERLAY(CmdPipelineBarrier, vkCmdPipelineBarrier);
    RESOLVE_OVERLAY(CmdCopyBufferToImage, vkCmdCopyBufferToImage);
    RESOLVE_OVERLAY(CmdCopyImageToBuffer, vkCmdCopyImageToBuffer);
    RESOLVE_OVERLAY(CreateBuffer, vkCreateBuffer);
    RESOLVE_OVERLAY(DestroyBuffer, vkDestroyBuffer);
    RESOLVE_OVERLAY(GetBufferMemoryRequirements, vkGetBufferMemoryRequirements);
    RESOLVE_OVERLAY(AllocateMemory, vkAllocateMemory);
    RESOLVE_OVERLAY(FreeMemory, vkFreeMemory);
    RESOLVE_OVERLAY(BindBufferMemory, vkBindBufferMemory);
    RESOLVE_OVERLAY(MapMemory, vkMapMemory);
    RESOLVE_OVERLAY(UnmapMemory, vkUnmapMemory);
    RESOLVE_OVERLAY(CreateSemaphore, vkCreateSemaphore);
    RESOLVE_OVERLAY(DestroySemaphore, vkDestroySemaphore);
    RESOLVE_OVERLAY(CreateFence, vkCreateFence);
    RESOLVE_OVERLAY(DestroyFence, vkDestroyFence);
    RESOLVE_OVERLAY(WaitForFences, vkWaitForFences);
    RESOLVE_OVERLAY(ResetFences, vkResetFences);
    RESOLVE_OVERLAY(QueueSubmit, vkQueueSubmit);

#undef RESOLVE_OVERLAY

    OverlayVulkan::gEnabled = true;

    return true;
}

bool OverlayVulkan::IsEnabled()
{
    return OverlayVulkan::gEnabled;
}

VkImageUsageFlags OverlayVulkan::AddCopyUsage(VkImageUsageFlags requested)
{
    if (!OverlayVulkan::gEnabled) return requested;

    return requested | VK_IMAGE_USAGE_TRANSFER_DST_BIT;
}

void OverlayVulkan::NoteDevice(VkPhysicalDevice physicalDevice, VkDevice)
{
    std::lock_guard<std::mutex> guard(OverlayVulkan::gMutex);
    OverlayVulkan::gPhysicalDevice = physicalDevice;
}

void OverlayVulkan::NoteQueue(VkDevice device, uint32_t queueFamilyIndex, VkQueue queue)
{
    if (queue == VK_NULL_HANDLE) return;

    std::lock_guard<std::mutex> guard(OverlayVulkan::gMutex);
    OverlayVulkan::gQueueFamilies[queue] = queueFamilyIndex;
    OverlayVulkan::gQueueDevices[queue] = device;
}

void OverlayVulkan::NoteSwapchain(VkDevice device, VkSwapchainKHR swapchain, VkFormat format, VkExtent2D extent, bool copyable)
{
    if (swapchain == VK_NULL_HANDLE) return;

    std::lock_guard<std::mutex> guard(OverlayVulkan::gMutex);

    OverlayVulkan::Target target;
    target.device = device;
    target.format = format;
    target.extent = extent;
    target.copyable = copyable;

    OverlayVulkan::gTargets[swapchain] = target;
}

void OverlayVulkan::ForgetSwapchain(VkSwapchainKHR swapchain)
{
    std::lock_guard<std::mutex> guard(OverlayVulkan::gMutex);
    OverlayVulkan::gTargets.erase(swapchain);
}

static const char* CapturePath()
{
    static const char* path = Settings::Lookup(Overlay::CAPTURE_SETTING);

    return path && *path ? path : nullptr;
}

static bool FormatIsBlueFirst(VkFormat format)
{
    return format == VK_FORMAT_B8G8R8A8_UNORM || format == VK_FORMAT_B8G8R8A8_SRGB;
}

static bool FormatIsCopyable(VkFormat format)
{
    switch (format)
    {
        case VK_FORMAT_B8G8R8A8_UNORM:
        case VK_FORMAT_B8G8R8A8_SRGB:
        case VK_FORMAT_R8G8B8A8_UNORM:
        case VK_FORMAT_R8G8B8A8_SRGB:
            return true;
        default:
            return false;
    }
}

static bool FindMemoryType(uint32_t allowed, VkMemoryPropertyFlags wanted, uint32_t& chosen)
{
    VkPhysicalDeviceMemoryProperties properties = {};
    OverlayVulkan::gApi.GetMemoryProperties(OverlayVulkan::gPhysicalDevice, &properties);

    for (uint32_t index = 0; index < properties.memoryTypeCount; ++index)
    {
        if ((allowed & (1u << index)) == 0) continue;
        if ((properties.memoryTypes[index].propertyFlags & wanted) != wanted) continue;

        chosen = index;
        return true;
    }

    return false;
}

static bool PrepareTarget(OverlayVulkan::Target& target, VkSwapchainKHR swapchain, uint32_t queueFamilyIndex)
{
    OverlayVulkan::Api& api = OverlayVulkan::gApi;

    uint32_t imageCount = 0;
    if (api.GetSwapchainImages(target.device, swapchain, &imageCount, nullptr) != VK_SUCCESS || imageCount == 0)
    {
        Fail("the swapchain images could not be counted");
        return false;
    }

    target.images.assign(imageCount, VK_NULL_HANDLE);
    if (api.GetSwapchainImages(target.device, swapchain, &imageCount, target.images.data()) != VK_SUCCESS)
    {
        Fail("the swapchain images could not be read");
        return false;
    }

    VkCommandPoolCreateInfo poolInfo = {};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    poolInfo.queueFamilyIndex = queueFamilyIndex;

    if (api.CreateCommandPool(target.device, &poolInfo, nullptr, &target.pool) != VK_SUCCESS)
    {
        Fail("a command pool could not be created");
        return false;
    }

    target.slots.assign(imageCount, OverlayVulkan::Slot());

    std::vector<VkCommandBuffer> buffers(imageCount, VK_NULL_HANDLE);

    VkCommandBufferAllocateInfo allocateInfo = {};
    allocateInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocateInfo.commandPool = target.pool;
    allocateInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocateInfo.commandBufferCount = imageCount;

    if (api.AllocateCommandBuffers(target.device, &allocateInfo, buffers.data()) != VK_SUCCESS)
    {
        Fail("the command buffers could not be allocated");
        return false;
    }

    for (uint32_t index = 0; index < imageCount; ++index)
    {
        target.slots[index].commands = buffers[index];

        VkSemaphoreCreateInfo semaphoreInfo = {};
        semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;

        if (api.CreateSemaphore(target.device, &semaphoreInfo, nullptr, &target.slots[index].finished) != VK_SUCCESS)
        {
            Fail("a semaphore could not be created");
            return false;
        }

        VkFenceCreateInfo fenceInfo = {};
        fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;

        if (api.CreateFence(target.device, &fenceInfo, nullptr, &target.slots[index].fence) != VK_SUCCESS)
        {
            Fail("a fence could not be created");
            return false;
        }
    }

    target.panelWidth = OverlayFont::MeasureWidth(std::string(FrameRate::CAPTION_CHARACTERS, '0'), FrameRate::OVERLAY_SCALE);
    target.panelHeight = OverlayFont::MeasureHeight(FrameRate::OVERLAY_SCALE);
    target.stagingBytes = (uint64_t)target.panelWidth * (uint64_t)target.panelHeight * 4;

    VkBufferCreateInfo bufferInfo = {};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = target.stagingBytes;
    bufferInfo.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    if (api.CreateBuffer(target.device, &bufferInfo, nullptr, &target.staging) != VK_SUCCESS)
    {
        Fail("the staging buffer could not be created");
        return false;
    }

    VkMemoryRequirements requirements = {};
    api.GetBufferMemoryRequirements(target.device, target.staging, &requirements);

    uint32_t memoryType = 0;
    if (!FindMemoryType(requirements.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        memoryType))
    {
        Fail("no host visible memory type accepts the staging buffer");
        return false;
    }

    VkMemoryAllocateInfo memoryInfo = {};
    memoryInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    memoryInfo.allocationSize = requirements.size;
    memoryInfo.memoryTypeIndex = memoryType;

    if (api.AllocateMemory(target.device, &memoryInfo, nullptr, &target.stagingMemory) != VK_SUCCESS)
    {
        Fail("the staging memory could not be allocated");
        return false;
    }

    if (api.BindBufferMemory(target.device, target.staging, target.stagingMemory, 0) != VK_SUCCESS)
    {
        Fail("the staging memory could not be bound");
        return false;
    }

    if (api.MapMemory(target.device, target.stagingMemory, 0, VK_WHOLE_SIZE, 0, &target.stagingMapping) != VK_SUCCESS)
    {
        Fail("the staging memory could not be mapped");
        return false;
    }

    if (CapturePath())
    {
        target.readbackBytes = (uint64_t)target.extent.width * (uint64_t)target.extent.height * 4;

        VkBufferCreateInfo readbackInfo = {};
        readbackInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
        readbackInfo.size = target.readbackBytes;
        readbackInfo.usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        readbackInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

        VkMemoryRequirements readbackRequirements = {};
        uint32_t readbackType = 0;

        if (api.CreateBuffer(target.device, &readbackInfo, nullptr, &target.readback) == VK_SUCCESS)
        {
            api.GetBufferMemoryRequirements(target.device, target.readback, &readbackRequirements);

            if (FindMemoryType(readbackRequirements.memoryTypeBits,
                               VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, readbackType))
            {
                VkMemoryAllocateInfo readbackMemoryInfo = {};
                readbackMemoryInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
                readbackMemoryInfo.allocationSize = readbackRequirements.size;
                readbackMemoryInfo.memoryTypeIndex = readbackType;

                if (api.AllocateMemory(target.device, &readbackMemoryInfo, nullptr, &target.readbackMemory) == VK_SUCCESS
                    && api.BindBufferMemory(target.device, target.readback, target.readbackMemory, 0) == VK_SUCCESS)
                {
                    api.MapMemory(target.device, target.readbackMemory, 0, VK_WHOLE_SIZE, 0, &target.readbackMapping);
                }
            }
        }

        if (!target.readbackMapping) fprintf(stderr, "[overlay] a guest frame cannot be captured: no readable buffer\n");
    }

    target.ready = true;

    fprintf(stderr, "[overlay] the frame counter draws into the guest's own frames, %dx%d at the top left\n", target.panelWidth,
            target.panelHeight);

    return true;
}

static void RecordAndSubmit(OverlayVulkan::Target& target, OverlayVulkan::Slot& slot, VkQueue queue, VkImage image,
                            const VkSemaphore* waitSemaphores, uint32_t waitCount, bool capture)
{
    OverlayVulkan::Api& api = OverlayVulkan::gApi;

    api.ResetCommandBuffer(slot.commands, 0);

    VkCommandBufferBeginInfo beginInfo = {};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    api.BeginCommandBuffer(slot.commands, &beginInfo);

    VkImageMemoryBarrier toTransfer = {};
    toTransfer.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
    toTransfer.srcAccessMask = 0;
    toTransfer.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    toTransfer.oldLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    toTransfer.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    toTransfer.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    toTransfer.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    toTransfer.image = image;
    toTransfer.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    toTransfer.subresourceRange.levelCount = 1;
    toTransfer.subresourceRange.layerCount = 1;

    api.CmdPipelineBarrier(slot.commands, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0,
                           nullptr, 1, &toTransfer);

    VkBufferImageCopy region = {};
    region.bufferOffset = 0;
    region.bufferRowLength = (uint32_t)target.panelWidth;
    region.bufferImageHeight = (uint32_t)target.panelHeight;
    region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    region.imageSubresource.layerCount = 1;
    region.imageOffset.x = OverlayFont::MARGIN;
    region.imageOffset.y = OverlayFont::MARGIN;
    region.imageExtent.width = (uint32_t)target.panelWidth;
    region.imageExtent.height = (uint32_t)target.panelHeight;
    region.imageExtent.depth = 1;

    api.CmdCopyBufferToImage(slot.commands, target.staging, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);

    VkImageLayout beforePresent = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;

    if (capture)
    {
        VkImageMemoryBarrier toRead = toTransfer;
        toRead.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        toRead.dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
        toRead.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        toRead.newLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;

        api.CmdPipelineBarrier(slot.commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0,
                               nullptr, 1, &toRead);

        VkBufferImageCopy whole = {};
        whole.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        whole.imageSubresource.layerCount = 1;
        whole.imageExtent.width = target.extent.width;
        whole.imageExtent.height = target.extent.height;
        whole.imageExtent.depth = 1;

        api.CmdCopyImageToBuffer(slot.commands, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, target.readback, 1, &whole);

        beforePresent = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
    }

    VkImageMemoryBarrier toPresent = toTransfer;
    toPresent.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    toPresent.dstAccessMask = 0;
    toPresent.oldLayout = beforePresent;
    toPresent.newLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

    api.CmdPipelineBarrier(slot.commands, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, 0, nullptr, 0,
                           nullptr, 1, &toPresent);

    api.EndCommandBuffer(slot.commands);

    std::vector<VkPipelineStageFlags> stages(waitCount, VK_PIPELINE_STAGE_TRANSFER_BIT);

    VkSubmitInfo submit = {};
    submit.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submit.waitSemaphoreCount = waitCount;
    submit.pWaitSemaphores = waitCount > 0 ? waitSemaphores : nullptr;
    submit.pWaitDstStageMask = waitCount > 0 ? stages.data() : nullptr;
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &slot.commands;
    submit.signalSemaphoreCount = 1;
    submit.pSignalSemaphores = &slot.finished;

    api.ResetFences(target.device, 1, &slot.fence);

    if (api.QueueSubmit(queue, 1, &submit, slot.fence) != VK_SUCCESS)
    {
        Fail("the overlay copy could not be submitted");
        target.broken = true;
        return;
    }

    slot.submitted = true;
}

bool OverlayVulkan::Draw(VkQueue queue, VkSwapchainKHR swapchain, uint32_t imageIndex, const VkSemaphore* waitSemaphores,
                         uint32_t waitCount, VkSemaphore& signalled)
{
    if (!OverlayVulkan::gEnabled) return false;

    std::lock_guard<std::mutex> guard(OverlayVulkan::gMutex);

    auto found = OverlayVulkan::gTargets.find(swapchain);
    if (found == OverlayVulkan::gTargets.end())
    {
        Fail("this swapchain was not created through the interposed vkCreateSwapchainKHR");
        return false;
    }

    OverlayVulkan::Target& target = found->second;
    if (target.broken) return false;

    if (!target.copyable)
    {
        Fail("the swapchain images cannot be copied into");
        return false;
    }

    if (!FormatIsCopyable(target.format))
    {
        Fail("the swapchain format is not one of the four eight bit formats this can write");
        return false;
    }

    auto family = OverlayVulkan::gQueueFamilies.find(queue);
    if (family == OverlayVulkan::gQueueFamilies.end())
    {
        Fail("the presenting queue never came from the interposed vkGetDeviceQueue");
        return false;
    }

    if (OverlayVulkan::gPhysicalDevice == VK_NULL_HANDLE)
    {
        Fail("the physical device never came from the interposed vkCreateDevice");
        return false;
    }

    if (!target.ready && !PrepareTarget(target, swapchain, family->second))
    {
        target.broken = true;
        return false;
    }

    if (imageIndex >= target.images.size() || imageIndex >= target.slots.size()) return false;

    OverlayVulkan::Slot& slot = target.slots[imageIndex];

    if (slot.submitted)
    {
        OverlayVulkan::gApi.WaitForFences(target.device, 1, &slot.fence, VK_TRUE, UINT64_MAX);
        slot.submitted = false;
    }

    const std::string caption = FrameRate::PaddedCaption();

    if (caption != target.caption)
    {
        target.caption = caption;

        OverlayFont::Panel panel;
        OverlayFont::Render(caption, FrameRate::OVERLAY_SCALE,
                            FormatIsBlueFirst(target.format) ? OverlayFont::Channels::Bgra : OverlayFont::Channels::Rgba, panel);

        for (size_t at = 3; at < panel.pixels.size(); at += 4) panel.pixels[at] = 255;

        if (panel.width == target.panelWidth && panel.height == target.panelHeight
            && panel.pixels.size() == (size_t)target.stagingBytes)
        {
            memcpy(target.stagingMapping, panel.pixels.data(), panel.pixels.size());
        }
        else
        {
            Fail("the overlay panel came out a different size than the staging buffer");
            target.broken = true;
            return false;
        }
    }

    const bool capture = CapturePath() && target.readbackMapping && !target.captured
                         && FrameRate::TotalFrames() >= OverlayVulkan::CAPTURE_AFTER_FRAMES;

    RecordAndSubmit(target, slot, queue, target.images[imageIndex], waitSemaphores, waitCount, capture);

    if (target.broken) return false;

    if (capture)
    {
        target.captured = true;

        OverlayVulkan::gApi.WaitForFences(target.device, 1, &slot.fence, VK_TRUE, UINT64_MAX);

        FILE* out = fopen(CapturePath(), "wb");

        if (out)
        {
            fwrite(target.readbackMapping, 1, (size_t)target.readbackBytes, out);
            fclose(out);

            fprintf(stderr, "[overlay] a guest frame with the counter on it is at %s, %ux%u\n", CapturePath(), target.extent.width,
                    target.extent.height);
        }
        else
        {
            fprintf(stderr, "[overlay] a guest frame could not be written to %s\n", CapturePath());
        }
    }

    signalled = slot.finished;

    return true;
}

const char* OverlayVulkan::LastFailure()
{
    return OverlayVulkan::gFailure;
}

#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

namespace OverlayVulkan
{
    using Resolver = void* (*)(const char* name);

    bool Enable(Resolver resolve);
    bool IsEnabled();

    VkImageUsageFlags AddCopyUsage(VkImageUsageFlags requested);

    void NoteDevice(VkPhysicalDevice physicalDevice, VkDevice device);
    void NoteQueue(VkDevice device, uint32_t queueFamilyIndex, VkQueue queue);
    void NoteSwapchain(VkDevice device, VkSwapchainKHR swapchain, VkFormat format, VkExtent2D extent, bool copyable);
    void ForgetSwapchain(VkSwapchainKHR swapchain);

    bool Draw(VkQueue queue, VkSwapchainKHR swapchain, uint32_t imageIndex, const VkSemaphore* waitSemaphores, uint32_t waitCount,
              VkSemaphore& signalled);

    const char* LastFailure();

    inline constexpr uint64_t CAPTURE_AFTER_FRAMES = 90;
}

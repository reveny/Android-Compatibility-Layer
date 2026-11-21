#pragma once

#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

#include "HostedWindow/HostedWindow.hpp"

namespace HostedSwapchain
{
    inline constexpr uint32_t MIN_IMAGES = 2;
    inline constexpr uint32_t PREFERRED_IMAGES = 3;
    inline constexpr int PACING_TIMEOUT_MILLIS = 50;
    inline constexpr uint64_t NANOS_PER_MILLI = 1000000ull;

    struct Statistics
    {
        uint64_t framesShared = 0;
        uint64_t framesCopied = 0;
        uint64_t swapchainsCreated = 0;
    };

    void Initialize(PFN_vkGetInstanceProcAddr getInstanceProcAddr, PFN_vkGetDeviceProcAddr getDeviceProcAddr);
    void Attach(HostedWindow::Session* session, uint32_t width, uint32_t height);
    bool IsActive();

    void NoteInstance(VkInstance instance);
    void AddDeviceExtensions(VkPhysicalDevice physicalDevice, std::vector<const char*>& extensions);
    void NoteDevice(VkPhysicalDevice physicalDevice, VkDevice device, const std::vector<const char*>& extensions);
    void NoteQueue(VkDevice device, uint32_t family, VkQueue queue);

    VkResult CreateSurface(VkSurfaceKHR* surface);
    bool OwnsSurface(VkSurfaceKHR surface);
    void DestroySurface(VkSurfaceKHR surface);

    VkResult SurfaceSupport(VkBool32* supported);
    VkResult SurfaceCapabilities(VkSurfaceCapabilitiesKHR* capabilities);
    VkResult SurfaceFormats(uint32_t* count, VkSurfaceFormatKHR* formats);
    VkResult PresentModes(uint32_t* count, VkPresentModeKHR* modes);

    bool OwnsSwapchain(VkSwapchainKHR swapchain);
    VkResult CreateSwapchain(VkDevice device, const VkSwapchainCreateInfoKHR* createInfo, VkSwapchainKHR* swapchain);
    void DestroySwapchain(VkDevice device, VkSwapchainKHR swapchain);
    VkResult GetImages(VkSwapchainKHR swapchain, uint32_t* count, VkImage* images);
    VkResult Acquire(VkDevice device, VkSwapchainKHR swapchain, uint64_t timeout, VkSemaphore semaphore, VkFence fence, uint32_t* index);
    VkResult Present(VkQueue queue, VkSwapchainKHR swapchain, uint32_t index, const VkSemaphore* waits, uint32_t waitCount);

    Statistics GetStatistics();
}

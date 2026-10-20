#pragma once

#include <cstdint>
#include <string>

#include "GuestRuntime/GuestRuntime.hpp"
#include "GuestState/GuestState.hpp"

namespace AheadOfTime
{
    inline constexpr const char* CACHE_ROOT_SETTING = "cache-root";
    inline constexpr const char* LINKER_SETTING = "linker";
    inline constexpr const char* LINKER_LIBRARY_PATH_SETTING = "linker-library-path";
    inline constexpr const char* DEFAULT_LINKER = "ld.lld";
    inline constexpr const char* TRANSLATOR_VERSION = "acl-translator-1";

    inline constexpr uint64_t DECODE_BUDGET_MEGABYTES = 1536;
    inline constexpr uint64_t BYTES_PER_GUEST_INSTRUCTION = 1024;
    inline constexpr uint64_t MINIMUM_BATCH_INSTRUCTIONS = 65536;

    inline constexpr int MAX_DISCOVERY_ROUNDS = 8;

    enum class Result : uint8_t
    {
        Ok,
        NotInitialized,
        DiscoveryFailed,
        NothingTranslated,
        EmitFailed,
        LinkFailed,
        CacheFailed,
        LoadFailed
    };

    struct Statistics
    {
        uint64_t functionsDiscovered = 0;
        uint64_t functionsTranslated = 0;
        uint64_t blocksTranslated = 0;
        uint64_t trapsEmitted = 0;
        uint64_t entriesResolved = 0;
        uint64_t directCallsEmitted = 0;
        uint64_t rangesLeftUncovered = 0;
        uint64_t functionsUndecodable = 0;
        uint64_t functionsUnlowerable = 0;
        bool cacheHit = false;
    };

    struct Image
    {
        void* implementation = nullptr;

        Image() = default;
        Image(const Image&) = delete;
        Image& operator=(const Image&) = delete;
    };

    using ProgressCallback = void (*)(void* context, const char* imagePath, uint64_t completed, uint64_t total);

    void SetProgressCallback(ProgressCallback callback, void* context);

    Result Translate(GuestRuntime::Instance& runtime, uint32_t moduleId, const std::string& imagePath, Image& image, Statistics& statistics);
    void Release(Image& image);

    const char* ResultName(Result result);
}

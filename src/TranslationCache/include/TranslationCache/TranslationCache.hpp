#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace TranslationCache
{
    inline constexpr const char* ROOT_SETTING = "cache-root";

    inline constexpr uint32_t KEY_CHARACTERS = 64;
    inline constexpr uint64_t MAX_OBJECT_BYTES = 1ull << 30;

    enum class Result : uint8_t
    {
        Ok,
        NotFound,
        Unreadable,
        Unwritable,
        KeyMismatch,
        Disabled
    };

    struct Configuration
    {
        std::string root;
        std::string runtimeVersion;
        bool enabled = true;
    };

    struct Entry
    {
        std::string key;
        std::string path;
        uint64_t objectBytes = 0;
    };

    Result DefaultRoot(std::string& root);
    Result KeyForFile(const std::string& imagePath, const std::string& runtimeVersion, std::string& key);

    Result Lookup(const Configuration& configuration, const std::string& key, Entry& entry);
    Result Store(const Configuration& configuration, const std::string& key, const void* object, uint64_t length, Entry& entry);
    Result Adopt(const Configuration& configuration, const std::string& key, const std::string& objectPath, Entry& entry);
    Result SweepAbandoned(const Configuration& configuration, uint64_t& removed);
    Result Load(const Entry& entry, std::vector<uint8_t>& object);

    const char* ResultName(Result result);
}

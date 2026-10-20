#pragma once

#include <cstdint>
#include <string>

namespace IcuSetup
{
    inline constexpr const char* ICU_LIBRARY = "libicuuc-host.so";
    inline constexpr const char* ICU_DATA_SUBDIR = "/etc/icu";
    inline constexpr int ICU_VERSION_MIN = 60;
    inline constexpr int ICU_VERSION_MAX = 99;
    inline constexpr int MAX_SYMBOL_LENGTH = 64;

    using SetDataDirectoryFn = void (*)(const char* path);

    enum class Result : uint8_t
    {
        Ok,
        LibraryNotFound,
        SymbolNotFound,
        DataDirectoryMissing
    };

    Result Configure(const std::string& hostRoot);

    const char* ResultName(Result result);
}

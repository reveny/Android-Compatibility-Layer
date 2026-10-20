#pragma once

namespace IcuPreload
{
    using SetDataDirectoryFn = void (*)(const char* path);

    inline constexpr const char* ICU_LIBRARY = "libicuuc-host.so";
    inline constexpr const char* ICU_DATA_SUBDIR = "/etc/icu";
    inline constexpr const char* LOG_PREFIX = "[icu-preload] ";
    inline constexpr int ICU_VERSION_MIN = 60;
    inline constexpr int ICU_VERSION_MAX = 99;
    inline constexpr int MAX_PATH_LENGTH = 4096;
    inline constexpr int MAX_SYMBOL_LENGTH = 64;

    bool IsVerbose();
    void Report(const char* format, ...) __attribute__((format(printf, 1, 2)));
    void* OpenIcuLibrary();
    SetDataDirectoryFn ResolveSetter(void* handle);
    bool Configure();
}

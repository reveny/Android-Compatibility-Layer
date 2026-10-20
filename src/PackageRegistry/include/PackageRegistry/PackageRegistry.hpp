#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace PackageRegistry
{
    inline constexpr const char* INDEX_LEAF = "packages.conf";
    inline constexpr const char* ICON_DIRECTORY_LEAF = "icons";
    inline constexpr const char* SECTION_PREFIX = "package.";
    inline constexpr uint32_t MAX_PACKAGES = 512;
    inline constexpr uint32_t MAX_LINE_BYTES = 4096;

    enum class Result : uint8_t
    {
        Ok,
        NotFound,
        Unreadable,
        Malformed,
        NotAnArchive,
        NoPackageName,
        CopyFailed,
        Unwritable,
        TooMany
    };

    struct Entry
    {
        std::string packageName;
        std::string label;
        std::string apkPath;
        std::string iconPath;
        std::string dataPath;
        int minSdkVersion = 0;
        int targetSdkVersion = 0;
        int activityCount = 0;
    };

    std::string IndexPath(const std::string& dataRoot);
    std::string IconDirectory(const std::string& dataRoot);
    std::string InstalledApkPath(const std::string& dataRoot, const std::string& packageName);

    Result Load(const std::string& indexPath, std::vector<Entry>& entries);
    bool Find(const std::vector<Entry>& entries, const std::string& packageName, Entry& found);

    Result Save(const std::string& indexPath, const std::vector<Entry>& entries);

    Result Install(const std::string& apkPath, const std::string& dataRoot, const std::string& frameworkResources,
                   std::vector<Entry>& entries, Entry& installed);

    Result Uninstall(const std::string& packageName, const std::string& dataRoot, std::vector<Entry>& entries, bool keepData);

    const char* ResultName(Result result);
}

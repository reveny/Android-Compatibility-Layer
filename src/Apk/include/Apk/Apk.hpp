#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "Platform/Platform.hpp"

namespace Apk
{
    inline constexpr int INVALID_FILE_DESCRIPTOR = -1;
    inline constexpr uint16_t COMPRESSION_STORED = 0;
    inline constexpr uint16_t COMPRESSION_DEFLATE = 8;
    inline constexpr int MAX_ATTRIBUTE_LENGTH = 1024;
    inline constexpr int MAX_STRING_LENGTH = 4096;
    inline constexpr int MAX_VALUE_LENGTH = 1 << 20;
    inline constexpr int MAX_ELEMENT_DEPTH = 32;
    inline constexpr int MAX_PACKAGE_NAME_LENGTH = 255;
    inline constexpr int MAX_ENTRY_NAME_LENGTH = 4095;
    inline constexpr int MAX_ENTRIES = 1 << 20;
    inline constexpr uint64_t MAX_EXTRACT_BYTES = 1ull << 31;
    inline constexpr uint64_t MAX_EXTRACT_TOTAL_BYTES = 1ull << 31;
    inline constexpr int MAX_NATIVE_LIBRARIES = 256;
    inline constexpr int MAX_DEX_ENTRIES = 4096;
    inline constexpr const char* DEFAULT_APPLICATION_CLASS = "android.app.Application";
    inline constexpr const char* NATIVE_LIBRARY_PREFIX = "lib/";
    inline constexpr const char* NATIVE_LIBRARY_SUFFIX = ".so";
    inline constexpr const char* HOST_NATIVE_ABI = "x86_64";

    inline constexpr int32_t VALUE_TYPE_REFERENCE = 0x01;

    inline constexpr uint32_t ATTRIBUTE_THEME = 0x01010000;
    inline constexpr uint32_t ATTRIBUTE_LABEL = 0x01010001;
    inline constexpr uint32_t ATTRIBUTE_ICON = 0x01010002;
    inline constexpr uint32_t ATTRIBUTE_NAME = 0x01010003;
    inline constexpr uint32_t ATTRIBUTE_ROUND_ICON = 0x0101052c;
    inline constexpr int ADAPTIVE_ICON_SDK_VERSION = 26;
    inline constexpr uint32_t ATTRIBUTE_VERSION_CODE = 0x0101021b;
    inline constexpr uint32_t ATTRIBUTE_VERSION_NAME = 0x0101021c;
    inline constexpr uint32_t ATTRIBUTE_VERSION_CODE_MAJOR = 0x01010576;
    inline constexpr uint32_t ATTRIBUTE_TASK_AFFINITY = 0x01010012;
    inline constexpr uint32_t ATTRIBUTE_GRANT_URI_PERMISSIONS = 0x0101001b;
    inline constexpr uint32_t ATTRIBUTE_ENABLED = 0x0101000e;
    inline constexpr uint32_t ATTRIBUTE_PROCESS = 0x01010011;
    inline constexpr uint32_t ATTRIBUTE_LAUNCH_MODE = 0x0101001d;
    inline constexpr uint32_t ATTRIBUTE_CONFIG_CHANGES = 0x0101001f;
    inline constexpr uint32_t ATTRIBUTE_DEBUGGABLE = 0x0101000f;
    inline constexpr uint32_t ATTRIBUTE_EXPORTED = 0x01010010;
    inline constexpr uint32_t ATTRIBUTE_AUTHORITIES = 0x01010018;
    inline constexpr uint32_t ATTRIBUTE_PRIORITY = 0x0101001c;
    inline constexpr uint32_t ATTRIBUTE_VALUE = 0x01010024;
    inline constexpr uint32_t ATTRIBUTE_RESOURCE = 0x01010025;
    inline constexpr uint32_t ATTRIBUTE_MIME_TYPE = 0x01010026;
    inline constexpr uint32_t ATTRIBUTE_SCHEME = 0x01010027;
    inline constexpr uint32_t ATTRIBUTE_TARGET_ACTIVITY = 0x01010202;
    inline constexpr uint32_t ATTRIBUTE_MIN_SDK_VERSION = 0x0101020c;
    inline constexpr uint32_t ATTRIBUTE_TARGET_SDK_VERSION = 0x01010270;
    inline constexpr uint32_t ATTRIBUTE_EXTRACT_NATIVE_LIBS = 0x010104ea;
    inline constexpr uint32_t ATTRIBUTE_DIRECT_BOOT_AWARE = 0x01010505;
    inline constexpr uint32_t ATTRIBUTE_APP_COMPONENT_FACTORY = 0x0101057a;

    enum class Result : uint8_t
    {
        Ok,
        FileNotFound,
        NotAZipArchive,
        MalformedArchive,
        EntryNotFound,
        MalformedManifest,
        UnsupportedCompression,
        UnsupportedAbi,
        MappingFailed
    };

    struct Entry
    {
        std::string name;
        uint64_t dataOffset = 0;
        uint64_t compressedSize = 0;
        uint64_t uncompressedSize = 0;
        uint32_t crc32 = 0;
        uint16_t compressionMethod = COMPRESSION_STORED;
    };

    inline constexpr int32_t META_DATA_NO_VALUE = -1;
    inline constexpr int32_t META_DATA_RESOURCE = -2;

    struct MetaDataEntry
    {
        std::string name;
        std::string value;
        int32_t valueType = META_DATA_NO_VALUE;
        uint32_t valueData = 0;
        uint32_t resource = 0;
    };

    struct IntentFilterInfo
    {
        std::vector<std::string> actions;
        std::vector<std::string> categories;
        std::vector<std::string> schemes;
        std::vector<std::string> mimeTypes;
        int priority = 0;
    };

    struct ProviderInfo
    {
        std::string className;
        std::string authorities;
        std::vector<MetaDataEntry> metaData;
        bool exported = false;
        bool directBootAware = false;
        bool grantUriPermissions = false;
        bool enabled = true;
        std::string process;
    };

    struct ActivityInfo
    {
        std::string className;
        std::string targetActivity;
        std::string taskAffinity;
        uint32_t themeResource = 0;
        int launchMode = 0;
        int configChanges = 0;
        bool isLauncher = false;
        bool isHome = false;
        bool isSystemUi = false;
        std::vector<MetaDataEntry> metaData;
        std::vector<IntentFilterInfo> filters;
    };

    struct ServiceInfo
    {
        std::string className;
        std::vector<MetaDataEntry> metaData;
        std::vector<IntentFilterInfo> filters;
        bool exported = false;
    };

    struct ReceiverInfo
    {
        std::string className;
        std::vector<MetaDataEntry> metaData;
        std::vector<IntentFilterInfo> filters;
        bool exported = false;
    };

    struct Manifest
    {
        std::string packageName;
        std::string splitName;
        std::string versionName;
        uint64_t versionCode = 0;
        std::string applicationLabel;
        uint32_t labelResource = 0;
        uint32_t iconResource = 0;
        uint32_t roundIconResource = 0;
        std::string applicationClassName;
        std::string appComponentFactory;
        uint32_t themeResource = 0;
        int minSdkVersion = 0;
        int targetSdkVersion = 0;
        bool debuggable = false;
        bool extractNativeLibs = true;
        std::vector<MetaDataEntry> applicationMetaData;
        std::vector<ActivityInfo> activities;
        std::vector<ProviderInfo> providers;
        std::vector<ServiceInfo> services;
        std::vector<ReceiverInfo> receivers;
        std::vector<std::string> permissions;
        std::vector<std::string> usesLibraries;
    };

    struct Package
    {
        std::string path;
        std::string frameworkPath;
        void* assetsHandle = nullptr;
        void* archiveHandle = nullptr;
        bool hasFramework = false;
        int fileDescriptor = INVALID_FILE_DESCRIPTOR;
        const void* mappedBase = nullptr;
        uint64_t mappedLength = 0;
        std::vector<Entry> entries;
        std::vector<std::string> dexEntries;
        std::vector<std::string> nativeAbis;
        std::vector<std::string> splitPaths;
        std::vector<std::string> splitNames;
        Manifest manifest;
    };

    struct Asset
    {
        void* handle = nullptr;
        int64_t length = 0;
    };

    Result Open(const std::string& path, const std::string& frameworkPath, Package& package);
    Result OpenWithSplits(const std::string& path, const std::vector<std::string>& splitPaths, const std::string& frameworkPath,
                          Package& package);
    Result ReadSplitName(const std::string& path, std::string& splitName);
    Result DiscoverSplits(const std::string& basePath, std::vector<std::string>& splitPaths);
    void Close(Package& package);

    Result OpenAsset(const Package& package, const std::string& path, Asset& asset);
    Result OpenNonAsset(const Package& package, const std::string& path, Asset& asset);
    int ReadAsset(Asset& asset, void* buffer, int byteCount);
    Result AssetFileDescriptor(Asset& asset, int& descriptor, int64_t& offset, int64_t& length);
    void CloseAsset(Asset& asset);
    Result ListAssets(const Package& package, const std::string& directory, std::vector<std::string>& names);

    std::string ResolveString(const Package& package, uint32_t resourceId);
    std::string ResolveLabel(const Package& package);
    std::string ResolveRasterIconEntry(const Package& package);

    bool IsSafeEntryName(const std::string& name);
    int FindEntry(const Package& package, const std::string& name);
    Result EnumerateArchive(Package& package);
    std::string NativeLibraryAbi(const std::string& entryName);
    int DexOrdinal(const std::string& entryName);
    Result SelectAbi(const Package& package, const std::string& preferred, std::string& selected);
    bool MatchesExtracted(const std::string& path, const Entry& entry);
    Result ExtractEntry(const Package& package, int entryIndex, const std::string& destinationPath);
    Result ExtractNativeLibraries(const Package& package, const std::string& abi, const std::string& destinationDirectory,
                                  std::vector<std::string>& extracted);

    Result GetString(const Package& package, uint32_t resourceId, std::string& out);
    Result LauncherActivity(const Package& package, std::string& className);

    Result ParseManifest(void* apkHandle, Manifest& manifest);
    std::string AttributeString(void* xml, int index);
    std::string AttributeName(void* xml, int index);
    std::string ElementName(void* xml);
    uint32_t AttributeReference(void* xml, int index);
    uint32_t ThemeFor(const Package& package, const std::string& activityClassName);

    bool IsValidPackageName(const std::string& packageName);

    const char* ResultName(Result result);
}

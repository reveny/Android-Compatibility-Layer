#include "Apk/Apk.hpp"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <unordered_set>
#include <zlib.h>

#include "AndroidfwC.h"

bool Apk::IsValidPackageName(const std::string& packageName)
{
    if (packageName.empty() || packageName.size() > MAX_PACKAGE_NAME_LENGTH) return false;

    bool startOfSegment = true;
    for (char character : packageName)
    {
        if (character == '.')
        {
            if (startOfSegment) return false;
            startOfSegment = true;
            continue;
        }

        bool isLetter = (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
        bool isDigit = character >= '0' && character <= '9';

        if (startOfSegment && !isLetter && character != '_') return false;
        if (!isLetter && !isDigit && character != '_') return false;

        startOfSegment = false;
    }

    return !startOfSegment;
}

std::string Apk::AttributeString(void* xml, int index)
{
    std::string value(MAX_ATTRIBUTE_LENGTH, '\0');

    int needed = AndroidfwXmlAttributeString((AndroidfwXml*)xml, index, value.data(), (int)value.size());
    if (needed == ANDROIDFW_ABSENT) return std::string();

    if (needed >= (int)value.size())
    {
        if (needed >= MAX_VALUE_LENGTH) return std::string();
        value.assign((size_t)needed + 1, '\0');
        needed = AndroidfwXmlAttributeString((AndroidfwXml*)xml, index, value.data(), (int)value.size());
        if (needed == ANDROIDFW_ABSENT || needed >= (int)value.size()) return std::string();
    }

    value.resize((size_t)needed);
    return value;
}

std::string Apk::AttributeName(void* xml, int index)
{
    std::string value(MAX_ATTRIBUTE_LENGTH, '\0');

    int needed = AndroidfwXmlAttributeName((AndroidfwXml*)xml, index, value.data(), (int)value.size());
    if (needed == ANDROIDFW_ABSENT || needed >= (int)value.size()) return std::string();

    value.resize((size_t)needed);
    return value;
}

uint32_t Apk::AttributeReference(void* xml, int index)
{
    int32_t type = 0;
    uint32_t data = 0;
    if (!AndroidfwXmlAttributeValue((AndroidfwXml*)xml, index, &type, &data)) return 0;

    if (type != VALUE_TYPE_REFERENCE) return 0;
    return data;
}

Apk::Result Apk::OpenNonAsset(const Package& package, const std::string& path, Asset& asset)
{
    asset = Asset{};
    if (!package.assetsHandle) return Result::FileNotFound;
    if (!IsSafeEntryName(path)) return Result::EntryNotFound;

    AndroidfwAsset* opened = AndroidfwAssetOpenNonAsset((AndroidfwApk*)package.assetsHandle, path.c_str());
    if (!opened) return Result::EntryNotFound;

    asset.handle = opened;
    asset.length = AndroidfwAssetLength(opened);
    return Result::Ok;
}

Apk::Result Apk::OpenAsset(const Package& package, const std::string& path, Asset& asset)
{
    asset = Asset{};
    if (!package.assetsHandle) return Result::FileNotFound;

    AndroidfwAsset* opened = AndroidfwAssetOpen((AndroidfwApk*)package.assetsHandle, path.c_str());
    if (!opened) return Result::EntryNotFound;

    asset.handle = opened;
    asset.length = AndroidfwAssetLength(opened);
    return Result::Ok;
}

int Apk::ReadAsset(Asset& asset, void* buffer, int byteCount)
{
    if (!asset.handle) return -1;
    return AndroidfwAssetRead((AndroidfwAsset*)asset.handle, buffer, byteCount);
}

Apk::Result Apk::AssetFileDescriptor(Asset& asset, int& descriptor, int64_t& offset, int64_t& length)
{
    descriptor = INVALID_FILE_DESCRIPTOR;
    offset = 0;
    length = 0;

    if (!asset.handle) return Result::EntryNotFound;

    long start = 0;
    long extent = 0;
    int opened = AndroidfwAssetFileDescriptor((AndroidfwAsset*)asset.handle, &start, &extent);
    if (opened < 0) return Result::UnsupportedCompression;

    descriptor = opened;
    offset = start;
    length = extent;
    return Result::Ok;
}

void Apk::CloseAsset(Asset& asset)
{
    if (!asset.handle) return;

    AndroidfwAssetClose((AndroidfwAsset*)asset.handle);
    asset = Asset{};
}

Apk::Result Apk::ListAssets(const Package& package, const std::string& directory, std::vector<std::string>& names)
{
    names.clear();
    if (!package.assetsHandle) return Result::FileNotFound;

    std::string buffer(MAX_VALUE_LENGTH, '\0');
    int needed = AndroidfwAssetList((AndroidfwApk*)package.assetsHandle, directory.c_str(), buffer.data(), (int)buffer.size());
    if (needed == ANDROIDFW_ABSENT) return Result::EntryNotFound;
    if (needed >= (int)buffer.size()) return Result::MalformedArchive;

    buffer.resize((size_t)needed);

    size_t start = 0;
    while (start < buffer.size())
    {
        size_t separator = buffer.find('\n', start);
        if (separator == std::string::npos) separator = buffer.size();

        if (separator > start) names.push_back(buffer.substr(start, separator - start));
        start = separator + 1;
    }

    return Result::Ok;
}

uint32_t Apk::ThemeFor(const Package& package, const std::string& activityClassName)
{
    for (const ActivityInfo& activity : package.manifest.activities)
    {
        if (activity.className != activityClassName) continue;
        if (activity.themeResource != 0) return activity.themeResource;
        break;
    }

    return package.manifest.themeResource;
}

std::string Apk::ElementName(void* xml)
{
    std::string value(MAX_ATTRIBUTE_LENGTH, '\0');

    int needed = AndroidfwXmlElementName((AndroidfwXml*)xml, value.data(), (int)value.size());
    if (needed == ANDROIDFW_ABSENT || needed >= (int)value.size()) return std::string();

    value.resize((size_t)needed);
    return value;
}

std::string Apk::ResolveString(const Package& package, uint32_t resourceId)
{
    if (!package.assetsHandle || resourceId == 0) return std::string();

    std::string value(MAX_ATTRIBUTE_LENGTH, '\0');

    int needed = AndroidfwApkGetString((AndroidfwApk*)package.assetsHandle, resourceId, value.data(), (int)value.size());
    if (needed == ANDROIDFW_ABSENT || needed >= (int)value.size()) return std::string();

    value.resize((size_t)needed);
    return value;
}

std::string Apk::ResolveLabel(const Package& package)
{
    if (!package.manifest.applicationLabel.empty()) return package.manifest.applicationLabel;

    const std::string resolved = ResolveString(package, package.manifest.labelResource);
    if (!resolved.empty() && resolved[0] != '@' && resolved[0] != 'r') return resolved;
    if (!resolved.empty() && resolved.rfind("res/", 0) != 0) return resolved;

    return std::string();
}

static bool NameLooksRaster(const std::string& name)
{
    const char* const SUFFIXES[] = {".png", ".webp", ".jpg", ".jpeg"};

    for (const char* suffix : SUFFIXES)
    {
        const size_t length = strlen(suffix);
        if (name.size() > length && name.compare(name.size() - length, length, suffix) == 0) return true;
    }

    return false;
}

std::string Apk::ResolveRasterIconEntry(const Package& package)
{
    const uint32_t candidates[] = {package.manifest.iconResource, package.manifest.roundIconResource};

    for (uint32_t candidate : candidates)
    {
        if (candidate == 0) continue;

        const std::string direct = ResolveString(package, candidate);
        if (NameLooksRaster(direct)) return direct;
    }

    int windowWidth = 0;
    int windowHeight = 0;
    int density = Platform::DENSITY_DEFAULT;
    Platform::ConfiguredScreen(windowWidth, windowHeight, density);

    AndroidfwApkSetConfiguration((AndroidfwApk*)package.assetsHandle, ADAPTIVE_ICON_SDK_VERSION - 1, density, 0, 0);

    std::string found;

    for (uint32_t candidate : candidates)
    {
        if (candidate == 0) continue;

        const std::string legacy = ResolveString(package, candidate);
        if (!NameLooksRaster(legacy)) continue;

        found = legacy;
        break;
    }

    const int screenWidthDp = windowWidth > 0 ? windowWidth * Platform::DENSITY_DEFAULT / density : 0;
    const int screenHeightDp = windowHeight > 0 ? windowHeight * Platform::DENSITY_DEFAULT / density : 0;
    AndroidfwApkSetConfiguration((AndroidfwApk*)package.assetsHandle, Platform::DECLARED_SDK_VERSION, density, screenWidthDp,
                                 screenHeightDp);

    return found;
}

Apk::Result Apk::ParseManifest(void* apkHandle, Manifest& manifest)
{
    AndroidfwXml* xml = AndroidfwXmlOpen((AndroidfwApk*)apkHandle, "AndroidManifest.xml");
    if (!xml) return Result::MalformedManifest;

    std::string elementStack[MAX_ELEMENT_DEPTH];
    int depth = 0;
    bool inIntentFilter = false;
    bool intentFilterOwnsActivity = false;
    bool sawMainAction = false;
    bool sawLauncherCategory = false;
    bool sawHomeCategory = false;
    bool sawSystemUiCategory = false;

    IntentFilterInfo currentFilter;
    std::string filterOwner;

    for (;;)
    {
        AndroidfwXmlEvent event = AndroidfwXmlNext(xml);
        if (event == ANDROIDFW_XML_END_DOCUMENT) break;

        if (event == ANDROIDFW_XML_ERROR)
        {
            AndroidfwXmlClose(xml);
            return Result::MalformedManifest;
        }

        if (event == ANDROIDFW_XML_END_ELEMENT)
        {
            if (depth > 0) --depth;
            if (depth < MAX_ELEMENT_DEPTH && elementStack[depth] == "intent-filter")
            {
                if (sawMainAction && sawLauncherCategory && intentFilterOwnsActivity && !manifest.activities.empty())
                {
                    manifest.activities.back().isLauncher = true;
                }

                if (sawMainAction && sawHomeCategory && intentFilterOwnsActivity && !manifest.activities.empty())
                {
                    manifest.activities.back().isHome = true;
                }

                if (sawMainAction && sawSystemUiCategory && intentFilterOwnsActivity && !manifest.activities.empty())
                {
                    manifest.activities.back().isSystemUi = true;
                }

                if (filterOwner == "activity" && !manifest.activities.empty())
                    manifest.activities.back().filters.push_back(currentFilter);
                else if (filterOwner == "service" && !manifest.services.empty())
                    manifest.services.back().filters.push_back(currentFilter);
                else if (filterOwner == "receiver" && !manifest.receivers.empty())
                    manifest.receivers.back().filters.push_back(currentFilter);

                currentFilter = IntentFilterInfo();
                filterOwner.clear();
                inIntentFilter = false;
                intentFilterOwnsActivity = false;
                sawMainAction = false;
                sawLauncherCategory = false;
                sawHomeCategory = false;
                sawSystemUiCategory = false;
            }
            continue;
        }

        if (event != ANDROIDFW_XML_START_ELEMENT) continue;

        if (depth >= MAX_ELEMENT_DEPTH)
        {
            AndroidfwXmlClose(xml);
            return Result::MalformedManifest;
        }

        std::string name = ElementName(xml);
        elementStack[depth] = name;
        ++depth;

        int attributeCount = AndroidfwXmlAttributeCount(xml);

        if (name == "manifest")
        {
            uint64_t versionCodeMajor = 0;

            for (int index = 0; index < attributeCount; ++index)
            {
                const uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);

                if (id == ATTRIBUTE_VERSION_CODE) manifest.versionCode = (uint32_t)AndroidfwXmlAttributeInt(xml, index, 0);
                if (id == ATTRIBUTE_VERSION_CODE_MAJOR) versionCodeMajor = (uint32_t)AndroidfwXmlAttributeInt(xml, index, 0);
                if (id == ATTRIBUTE_VERSION_NAME) manifest.versionName = AttributeString(xml, index);

                if (id != 0) continue;

                const std::string attribute = AttributeName(xml, index);

                if (attribute == "package") manifest.packageName = AttributeString(xml, index);
                if (attribute == "split") manifest.splitName = AttributeString(xml, index);
            }

            manifest.versionCode |= versionCodeMajor << 32;
        }
        else if (name == "uses-sdk")
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_MIN_SDK_VERSION) manifest.minSdkVersion = AndroidfwXmlAttributeInt(xml, index, 0);
                if (id == ATTRIBUTE_TARGET_SDK_VERSION) manifest.targetSdkVersion = AndroidfwXmlAttributeInt(xml, index, 0);
            }
        }
        else if (name == "application")
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) manifest.applicationClassName = AttributeString(xml, index);
                if (id == ATTRIBUTE_APP_COMPONENT_FACTORY) manifest.appComponentFactory = AttributeString(xml, index);
                if (id == ATTRIBUTE_THEME) manifest.themeResource = AttributeReference(xml, index);
                if (id == ATTRIBUTE_ICON) manifest.iconResource = AttributeReference(xml, index);
                if (id == ATTRIBUTE_ROUND_ICON) manifest.roundIconResource = AttributeReference(xml, index);

                if (id == ATTRIBUTE_LABEL)
                {
                    manifest.labelResource = AttributeReference(xml, index);
                    if (manifest.labelResource == 0) manifest.applicationLabel = AttributeString(xml, index);
                }

                if (id == ATTRIBUTE_DEBUGGABLE) manifest.debuggable = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
                if (id == ATTRIBUTE_EXTRACT_NATIVE_LIBS) manifest.extractNativeLibs = AndroidfwXmlAttributeInt(xml, index, 1) != 0;
            }
        }
        else if (name == "activity" || name == "activity-alias")
        {
            ActivityInfo activity;
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) activity.className = AttributeString(xml, index);
                if (id == ATTRIBUTE_TARGET_ACTIVITY) activity.targetActivity = AttributeString(xml, index);
                if (id == ATTRIBUTE_THEME) activity.themeResource = AttributeReference(xml, index);
                if (id == ATTRIBUTE_LAUNCH_MODE) activity.launchMode = AndroidfwXmlAttributeInt(xml, index, 0);
                if (id == ATTRIBUTE_CONFIG_CHANGES) activity.configChanges = AndroidfwXmlAttributeInt(xml, index, 0);
                if (id == ATTRIBUTE_TASK_AFFINITY) activity.taskAffinity = AttributeString(xml, index);
            }
            manifest.activities.push_back(activity);
        }
        else if (name == "service")
        {
            ServiceInfo service;
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) service.className = AttributeString(xml, index);
                if (id == ATTRIBUTE_EXPORTED) service.exported = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
            }
            manifest.services.push_back(service);
        }
        else if (name == "receiver")
        {
            ReceiverInfo receiver;
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) receiver.className = AttributeString(xml, index);
                if (id == ATTRIBUTE_EXPORTED) receiver.exported = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
            }
            manifest.receivers.push_back(receiver);
        }
        else if (name == "provider")
        {
            ProviderInfo provider;
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) provider.className = AttributeString(xml, index);
                if (id == ATTRIBUTE_AUTHORITIES) provider.authorities = AttributeString(xml, index);
                if (id == ATTRIBUTE_EXPORTED) provider.exported = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
                if (id == ATTRIBUTE_DIRECT_BOOT_AWARE) provider.directBootAware = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
                if (id == ATTRIBUTE_GRANT_URI_PERMISSIONS)
                    provider.grantUriPermissions = AndroidfwXmlAttributeInt(xml, index, 0) != 0;
            }
            manifest.providers.push_back(provider);
        }
        else if (name == "meta-data")
        {
            MetaDataEntry entry;
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);
                if (id == ATTRIBUTE_NAME) entry.name = AttributeString(xml, index);
                if (id == ATTRIBUTE_VALUE) entry.value = AttributeString(xml, index);
            }
            bool nested = depth >= 2 && depth - 2 < MAX_ELEMENT_DEPTH;
            const std::string& parent = nested ? elementStack[depth - 2] : entry.name;

            if (nested && parent == "provider" && !manifest.providers.empty())
            {
                manifest.providers.back().metaData.push_back(entry);
            }
            else if (nested && (parent == "activity" || parent == "activity-alias") && !manifest.activities.empty())
            {
                manifest.activities.back().metaData.push_back(entry);
            }
            else if (nested && parent == "service" && !manifest.services.empty())
            {
                manifest.services.back().metaData.push_back(entry);
            }
            else if (nested && parent == "receiver" && !manifest.receivers.empty())
            {
                manifest.receivers.back().metaData.push_back(entry);
            }
        }
        else if (name == "uses-permission")
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                if (AndroidfwXmlAttributeResourceId(xml, index) != ATTRIBUTE_NAME) continue;
                manifest.permissions.push_back(AttributeString(xml, index));
            }
        }
        else if (name == "action" && inIntentFilter)
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                if (AndroidfwXmlAttributeResourceId(xml, index) != ATTRIBUTE_NAME) continue;

                std::string action = AttributeString(xml, index);
                if (action == "android.intent.action.MAIN") sawMainAction = true;
                if (!action.empty()) currentFilter.actions.push_back(action);
            }
        }
        else if (name == "category" && inIntentFilter)
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                if (AndroidfwXmlAttributeResourceId(xml, index) != ATTRIBUTE_NAME) continue;

                std::string category = AttributeString(xml, index);
                if (category == "android.intent.category.LAUNCHER") sawLauncherCategory = true;
                if (category == "android.intent.category.HOME") sawHomeCategory = true;
                if (category == "android.intent.category.SYSTEM_UI") sawSystemUiCategory = true;
                if (!category.empty()) currentFilter.categories.push_back(category);
            }
        }
        else if (name == "data" && inIntentFilter)
        {
            for (int index = 0; index < attributeCount; ++index)
            {
                uint32_t id = AndroidfwXmlAttributeResourceId(xml, index);

                if (id == ATTRIBUTE_SCHEME)
                {
                    std::string scheme = AttributeString(xml, index);
                    if (!scheme.empty()) currentFilter.schemes.push_back(scheme);
                }

                if (id == ATTRIBUTE_MIME_TYPE)
                {
                    std::string mimeType = AttributeString(xml, index);
                    if (!mimeType.empty()) currentFilter.mimeTypes.push_back(mimeType);
                }
            }
        }
        else if (name == "intent-filter")
        {
            inIntentFilter = true;
            intentFilterOwnsActivity = depth >= 2 && (elementStack[depth - 2] == "activity" || elementStack[depth - 2] == "activity-alias");

            currentFilter = IntentFilterInfo();
            filterOwner = depth >= 2 ? elementStack[depth - 2] : std::string();
            if (filterOwner == "activity-alias") filterOwner = "activity";

            for (int index = 0; index < attributeCount; ++index)
            {
                if (AndroidfwXmlAttributeResourceId(xml, index) != ATTRIBUTE_PRIORITY) continue;
                currentFilter.priority = AndroidfwXmlAttributeInt(xml, index, 0);
            }
        }
    }

    AndroidfwXmlClose(xml);

    if (!IsValidPackageName(manifest.packageName)) return Result::MalformedManifest;
    if (manifest.applicationClassName.empty()) manifest.applicationClassName = DEFAULT_APPLICATION_CLASS;
    return Result::Ok;
}

bool Apk::IsSafeEntryName(const std::string& name)
{
    if (name.empty() || name.size() > MAX_ENTRY_NAME_LENGTH) return false;
    if (name.front() == '/') return false;
    if (name.back() == '/') return false;

    size_t segmentStart = 0;
    for (size_t index = 0; index <= name.size(); ++index)
    {
        if (index != name.size())
        {
            char character = name[index];
            if (character == '\0' || character == '\\') return false;
            if (character != '/') continue;
        }

        size_t length = index - segmentStart;
        if (length == 0) return false;
        if (length == 1 && name[segmentStart] == '.') return false;
        if (length == 2 && name[segmentStart] == '.' && name[segmentStart + 1] == '.') return false;

        segmentStart = index + 1;
    }

    return true;
}

int Apk::FindEntry(const Package& package, const std::string& name)
{
    for (size_t index = 0; index < package.entries.size(); ++index)
    {
        if (package.entries[index].name == name) return (int)index;
    }
    return -1;
}

int Apk::DexOrdinal(const std::string& entryName)
{
    if (entryName == "classes.dex") return 1;

    size_t prefixLength = strlen("classes");
    size_t suffixLength = strlen(".dex");

    if (entryName.size() <= prefixLength + suffixLength) return -1;
    if (entryName.compare(0, prefixLength, "classes") != 0) return -1;
    if (entryName.compare(entryName.size() - suffixLength, suffixLength, ".dex") != 0) return -1;

    size_t digitCount = entryName.size() - prefixLength - suffixLength;
    if (digitCount > 6) return -1;
    if (entryName[prefixLength] == '0') return -1;

    int ordinal = 0;
    for (size_t index = prefixLength; index < prefixLength + digitCount; ++index)
    {
        char character = entryName[index];
        if (character < '0' || character > '9') return -1;
        ordinal = ordinal * 10 + (character - '0');
    }

    if (ordinal < 2) return -1;
    return ordinal;
}

Apk::Result Apk::EnumerateArchive(Package& package)
{
    if (package.archiveHandle)
    {
        AndroidfwZipClose((AndroidfwZip*)package.archiveHandle);
        package.archiveHandle = nullptr;
    }
    package.entries.clear();
    package.dexEntries.clear();
    package.nativeAbis.clear();

    AndroidfwZip* zip = AndroidfwZipOpen(package.path.c_str());
    if (!zip) return Result::NotAZipArchive;

    package.archiveHandle = zip;

    int count = AndroidfwZipCount(zip);
    if (count < 0 || count > MAX_ENTRIES) return Result::MalformedArchive;

    std::string name(MAX_ENTRY_NAME_LENGTH + 1, '\0');
    package.entries.reserve((size_t)count);

    for (int index = 0; index < count; ++index)
    {
        int needed = AndroidfwZipEntryName(zip, index, name.data(), (int)name.size());
        if (needed < 0 || needed > MAX_ENTRY_NAME_LENGTH) return Result::MalformedArchive;

        AndroidfwZipEntry info;
        if (!AndroidfwZipEntryAt(zip, index, &info)) return Result::MalformedArchive;

        Entry entry;
        entry.name.assign(name.data(), (size_t)needed);
        entry.dataOffset = info.dataOffset;
        entry.compressedSize = info.compressedSize;
        entry.uncompressedSize = info.uncompressedSize;
        entry.crc32 = info.crc32;
        entry.compressionMethod = info.compressionMethod;

        if (entry.name.empty()) return Result::MalformedArchive;
        if (entry.name.back() == '/') continue;
        if (!IsSafeEntryName(entry.name)) return Result::MalformedArchive;

        package.entries.push_back(std::move(entry));
    }

    for (const Entry& entry : package.entries)
    {
        std::string abi = NativeLibraryAbi(entry.name);
        if (abi.empty()) continue;

        bool known = false;
        for (const std::string& seen : package.nativeAbis) known = known || seen == abi;
        if (!known) package.nativeAbis.push_back(abi);
    }

    std::unordered_set<std::string> rootNames;
    for (const Entry& entry : package.entries)
    {
        if (entry.name.find('/') == std::string::npos) rootNames.insert(entry.name);
    }

    for (int ordinal = 1; ordinal <= MAX_DEX_ENTRIES; ++ordinal)
    {
        std::string dexName = ordinal == 1 ? "classes.dex" : "classes" + std::to_string(ordinal) + ".dex";
        if (rootNames.find(dexName) == rootNames.end()) break;
        package.dexEntries.push_back(dexName);
    }

    for (const Entry& entry : package.entries)
    {
        int ordinal = DexOrdinal(entry.name);
        if (ordinal > (int)package.dexEntries.size()) return Result::MalformedArchive;
    }

    return Result::Ok;
}

std::string Apk::NativeLibraryAbi(const std::string& entryName)
{
    size_t prefixLength = strlen(NATIVE_LIBRARY_PREFIX);
    size_t suffixLength = strlen(NATIVE_LIBRARY_SUFFIX);

    if (entryName.compare(0, prefixLength, NATIVE_LIBRARY_PREFIX) != 0) return std::string();
    if (entryName.size() < prefixLength + suffixLength) return std::string();
    if (entryName.compare(entryName.size() - suffixLength, suffixLength, NATIVE_LIBRARY_SUFFIX) != 0) return std::string();

    size_t separator = entryName.find('/', prefixLength);
    if (separator == std::string::npos) return std::string();
    if (separator == prefixLength) return std::string();
    if (entryName.find('/', separator + 1) != std::string::npos) return std::string();
    if (separator + 1 + suffixLength > entryName.size()) return std::string();

    return entryName.substr(prefixLength, separator - prefixLength);
}

Apk::Result Apk::SelectAbi(const Package& package, const std::string& preferred, std::string& selected)
{
    selected.clear();
    if (package.nativeAbis.empty()) return Result::Ok;

    std::string wanted = preferred.empty() ? std::string(HOST_NATIVE_ABI) : preferred;
    for (const std::string& abi : package.nativeAbis)
    {
        if (abi != wanted) continue;
        selected = abi;
        return Result::Ok;
    }

    return Result::UnsupportedAbi;
}

Apk::Result Apk::Open(const std::string& path, const std::string& frameworkPath, Package& package)
{
    return OpenWithSplits(path, std::vector<std::string>(), frameworkPath, package);
}

Apk::Result Apk::ReadSplitName(const std::string& path, std::string& splitName)
{
    splitName.clear();

    AndroidfwApk* handle = AndroidfwApkLoad(nullptr, path.c_str());
    if (!handle) return Result::FileNotFound;

    Manifest manifest;
    Result result = ParseManifest(handle, manifest);

    AndroidfwApkUnload(handle);

    if (result != Result::Ok) return result;

    splitName = manifest.splitName;

    return Result::Ok;
}

Apk::Result Apk::DiscoverSplits(const std::string& basePath, std::vector<std::string>& splitPaths)
{
    splitPaths.clear();

    const size_t slash = basePath.find_last_of('/');
    if (slash == std::string::npos) return Result::Ok;

    const std::string directory = basePath.substr(0, slash);
    const std::string baseName = basePath.substr(slash + 1);

    DIR* opened = opendir(directory.c_str());
    if (!opened) return Result::Ok;

    std::vector<std::string> found;

    for (struct dirent* entry = readdir(opened); entry; entry = readdir(opened))
    {
        const std::string name = entry->d_name;

        if (name == baseName) continue;
        if (name.size() < 5 || name.compare(name.size() - 4, 4, ".apk") != 0) continue;
        if (name.compare(0, 6, "split_") != 0) continue;

        found.push_back(directory + "/" + name);
    }

    closedir(opened);
    std::sort(found.begin(), found.end());
    splitPaths = found;

    return Result::Ok;
}

Apk::Result Apk::OpenWithSplits(const std::string& path, const std::vector<std::string>& splitPaths, const std::string& frameworkPath,
                                Package& package)
{
    Close(package);

    std::vector<const char*> splits;
    splits.reserve(splitPaths.size());

    for (const std::string& split : splitPaths) splits.push_back(split.c_str());

    AndroidfwApk* handle = AndroidfwApkLoadWithSplits(frameworkPath.empty() ? nullptr : frameworkPath.c_str(), path.c_str(),
                                                      splits.empty() ? nullptr : splits.data(), (int)splits.size());
    if (!handle) return Result::FileNotFound;

    int windowWidth = 0;
    int windowHeight = 0;
    int density = Platform::DENSITY_DEFAULT;
    Platform::ConfiguredScreen(windowWidth, windowHeight, density);

    const int screenWidthDp = windowWidth > 0 ? windowWidth * Platform::DENSITY_DEFAULT / density : 0;
    const int screenHeightDp = windowHeight > 0 ? windowHeight * Platform::DENSITY_DEFAULT / density : 0;

    AndroidfwApkSetConfiguration(handle, Platform::DECLARED_SDK_VERSION, density, screenWidthDp, screenHeightDp);

    package.path = path;
    package.frameworkPath = frameworkPath;
    package.assetsHandle = handle;
    package.hasFramework = AndroidfwApkHasFramework(handle);
    package.splitPaths = splitPaths;

    for (const std::string& split : splitPaths)
    {
        std::string splitName;
        if (ReadSplitName(split, splitName) != Result::Ok || splitName.empty()) splitName = "unnamed";

        package.splitNames.push_back(splitName);
    }

    Result result = EnumerateArchive(package);
    if (result != Result::Ok)
    {
        Close(package);
        return result;
    }

    result = ParseManifest(handle, package.manifest);
    if (result != Result::Ok)
    {
        Close(package);
        return result;
    }

    return Result::Ok;
}

void Apk::Close(Package& package)
{
    if (package.assetsHandle)
    {
        AndroidfwApkUnload((AndroidfwApk*)package.assetsHandle);
        package.assetsHandle = nullptr;
    }
    if (package.archiveHandle)
    {
        AndroidfwZipClose((AndroidfwZip*)package.archiveHandle);
        package.archiveHandle = nullptr;
    }
    package.path.clear();
    package.frameworkPath.clear();
    package.hasFramework = false;
    package.fileDescriptor = INVALID_FILE_DESCRIPTOR;
    package.mappedBase = nullptr;
    package.mappedLength = 0;
    package.entries.clear();
    package.dexEntries.clear();
    package.nativeAbis.clear();
    package.manifest = Manifest();
}

bool Apk::MatchesExtracted(const std::string& path, const Entry& entry)
{
    struct stat status;
    if (lstat(path.c_str(), &status) != 0) return false;
    if (!S_ISREG(status.st_mode)) return false;
    if ((uint64_t)status.st_size != entry.uncompressedSize) return false;

    int descriptor = open(path.c_str(), O_RDONLY | O_NOFOLLOW | O_CLOEXEC);
    if (descriptor < 0) return false;

    FILE* file = fdopen(descriptor, "rb");
    if (!file)
    {
        close(descriptor);
        return false;
    }

    unsigned long running = crc32(0, nullptr, 0);
    unsigned char buffer[65536];
    for (;;)
    {
        size_t got = fread(buffer, 1, sizeof(buffer), file);
        if (got == 0) break;
        running = crc32(running, buffer, (unsigned int)got);
    }

    bool complete = feof(file) != 0;
    fclose(file);

    return complete && (uint32_t)running == entry.crc32;
}

Apk::Result Apk::ExtractEntry(const Package& package, int entryIndex, const std::string& destinationPath)
{
    if (!package.archiveHandle) return Result::NotAZipArchive;
    if (entryIndex < 0 || entryIndex >= (int)package.entries.size()) return Result::EntryNotFound;

    const Entry& entry = package.entries[(size_t)entryIndex];
    if (entry.compressionMethod != COMPRESSION_STORED && entry.compressionMethod != COMPRESSION_DEFLATE) return Result::UnsupportedCompression;
    if (entry.uncompressedSize > MAX_EXTRACT_BYTES) return Result::MalformedArchive;

    if (MatchesExtracted(destinationPath, entry)) return Result::Ok;

    std::string temporaryPath = destinationPath + ".partial." + std::to_string((long)getpid());
    unlink(temporaryPath.c_str());

    int descriptor = open(temporaryPath.c_str(), O_RDWR | O_CREAT | O_EXCL | O_NOFOLLOW | O_CLOEXEC, 0755);
    if (descriptor < 0) return Result::MappingFailed;

    bool extracted = AndroidfwZipExtract((AndroidfwZip*)package.archiveHandle, entry.name.c_str(), descriptor);
    bool closed = close(descriptor) == 0;

    if (!extracted || !closed)
    {
        unlink(temporaryPath.c_str());
        return Result::MalformedArchive;
    }

    if (!MatchesExtracted(temporaryPath, entry))
    {
        fprintf(stderr, "[apk] %s did not extract to its recorded size %llu and crc32 %u\n", entry.name.c_str(),
                (unsigned long long)entry.uncompressedSize, entry.crc32);
        unlink(temporaryPath.c_str());
        return Result::MalformedArchive;
    }

    if (rename(temporaryPath.c_str(), destinationPath.c_str()) != 0)
    {
        unlink(temporaryPath.c_str());
        return Result::MappingFailed;
    }

    return Result::Ok;
}

Apk::Result Apk::ExtractNativeLibraries(const Package& package, const std::string& abi, const std::string& destinationDirectory,
                                        std::vector<std::string>& extracted)
{
    extracted.clear();
    if (abi.empty()) return Result::Ok;

    std::string prefix = std::string(NATIVE_LIBRARY_PREFIX) + abi + "/";

    std::vector<std::string> names;
    uint64_t budget = 0;

    for (const Entry& entry : package.entries)
    {
        if (NativeLibraryAbi(entry.name) != abi) continue;
        if (entry.name.compare(0, prefix.size(), prefix) != 0) continue;

        if ((int)names.size() >= MAX_NATIVE_LIBRARIES) return Result::MalformedArchive;
        if (entry.uncompressedSize > MAX_EXTRACT_TOTAL_BYTES - budget) return Result::MalformedArchive;

        budget += entry.uncompressedSize;
        names.push_back(entry.name);
    }

    for (const std::string& name : names)
    {
        std::string leaf = name.substr(prefix.size());
        if (leaf.empty() || leaf.find('/') != std::string::npos) return Result::MalformedArchive;

        Result result = ExtractEntry(package, FindEntry(package, name), destinationDirectory + "/" + leaf);
        if (result != Result::Ok) return result;

        extracted.push_back(leaf);
    }

    return Result::Ok;
}

Apk::Result Apk::GetString(const Package& package, uint32_t resourceId, std::string& out)
{
    if (!package.assetsHandle) return Result::FileNotFound;

    std::string value(MAX_STRING_LENGTH, '\0');

    int needed = AndroidfwApkGetString((AndroidfwApk*)package.assetsHandle, resourceId, value.data(), (int)value.size());
    if (needed == ANDROIDFW_ABSENT) return Result::EntryNotFound;

    if (needed >= (int)value.size())
    {
        if (needed >= MAX_VALUE_LENGTH) return Result::EntryNotFound;
        value.assign((size_t)needed + 1, '\0');
        needed = AndroidfwApkGetString((AndroidfwApk*)package.assetsHandle, resourceId, value.data(), (int)value.size());
        if (needed == ANDROIDFW_ABSENT || needed >= (int)value.size()) return Result::EntryNotFound;
    }

    value.resize((size_t)needed);
    out = value;
    return Result::Ok;
}

Apk::Result Apk::LauncherActivity(const Package& package, std::string& className)
{
    for (const ActivityInfo& activity : package.manifest.activities)
    {
        if (!activity.isLauncher) continue;
        className = activity.targetActivity.empty() ? activity.className : activity.targetActivity;
        return Result::Ok;
    }

    for (const ActivityInfo& activity : package.manifest.activities)
    {
        if (!activity.isHome && !activity.isSystemUi) continue;
        className = activity.targetActivity.empty() ? activity.className : activity.targetActivity;
        return Result::Ok;
    }

    return Result::EntryNotFound;
}

const char* Apk::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::FileNotFound: return "FileNotFound";
        case Result::NotAZipArchive: return "NotAZipArchive";
        case Result::MalformedArchive: return "MalformedArchive";
        case Result::EntryNotFound: return "EntryNotFound";
        case Result::MalformedManifest: return "MalformedManifest";
        case Result::UnsupportedCompression: return "UnsupportedCompression";
        case Result::UnsupportedAbi: return "UnsupportedAbi";
        case Result::MappingFailed: return "MappingFailed";
    }
    return "Unknown";
}

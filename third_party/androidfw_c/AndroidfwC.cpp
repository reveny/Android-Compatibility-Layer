#include "AndroidfwC.h"

#include <memory>
#include <string>
#include <vector>

#include <androidfw/ApkAssets.h>
#include <androidfw/AssetManager2.h>
#include <androidfw/AttributeResolution.h>
#include <androidfw/ResourceTypes.h>
#include <androidfw/ZipFileRO.h>
#include <utils/Unicode.h>

struct AndroidfwApk
{
    android::AssetManager2 assetManager;
    std::vector<android::AssetManager2::ApkAssetsPtr> assets;
    android::ApkAssetsCookie applicationCookie = android::kInvalidCookie;
    bool hasFramework = false;
};

struct AndroidfwXml
{
    std::unique_ptr<android::Asset> asset;
    android::ResXMLTree tree;
    int depth = 0;
    bool leaving = false;

    explicit AndroidfwXml(std::shared_ptr<const android::DynamicRefTable> table) : tree(std::move(table))
    {
    }
};

struct AndroidfwTheme
{
    AndroidfwApk* apk = nullptr;
    std::unique_ptr<android::Theme> theme;
};

static int CopyOut(const std::string& value, char* out, int outSize)
{
    if (!out || outSize <= 0) return ANDROIDFW_ABSENT;

    size_t limit = (size_t)(outSize - 1);
    size_t copied = value.size() < limit ? value.size() : limit;

    memcpy(out, value.c_str(), copied);
    out[copied] = '\0';

    return (int)value.size();
}

static std::string Utf16ToUtf8(const char16_t* text, size_t length)
{
    if (!text || length == 0) return std::string();

    ssize_t needed = utf16_to_utf8_length(text, length);
    if (needed <= 0) return std::string();

    std::string result;
    result.resize((size_t)needed);
    utf16_to_utf8(text, length, result.data(), (size_t)needed + 1);
    return result;
}

static bool StringAt(const android::ResStringPool& pool, uint32_t index, std::string& value)
{
    if (index > (uint32_t)INT32_MAX) return false;

    auto utf8 = pool.string8At((int32_t)index);
    if (utf8.ok())
    {
        value.assign(utf8->data(), utf8->size());
        return true;
    }

    auto utf16 = pool.stringAt((int32_t)index);
    if (!utf16.ok()) return false;

    value = Utf16ToUtf8(utf16->data(), utf16->size());
    return true;
}

AndroidfwApk* AndroidfwApkLoad(const char* frameworkPath, const char* path)
{
    return AndroidfwApkLoadWithSplits(frameworkPath, path, nullptr, 0);
}

AndroidfwApk* AndroidfwApkLoadWithSplits(const char* frameworkPath, const char* path, const char* const* splitPaths, int splitCount)
{
    if (!path) return nullptr;

    auto* apk = new AndroidfwApk();

    if (frameworkPath && *frameworkPath)
    {
        auto framework = android::ApkAssets::Load(frameworkPath, android::PROPERTY_SYSTEM);
        if (framework != nullptr)
        {
            apk->assets.push_back(std::move(framework));
            apk->hasFramework = true;
        }
    }

    auto loaded = android::ApkAssets::Load(path);
    if (loaded == nullptr)
    {
        delete apk;
        return nullptr;
    }

    apk->assets.push_back(std::move(loaded));
    apk->applicationCookie = static_cast<android::ApkAssetsCookie>(apk->assets.size() - 1);

    for (int index = 0; index < splitCount; index++)
    {
        if (!splitPaths || !splitPaths[index] || !*splitPaths[index]) continue;

        auto split = android::ApkAssets::Load(splitPaths[index]);
        if (split == nullptr)
        {
            delete apk;
            return nullptr;
        }

        apk->assets.push_back(std::move(split));
    }

    apk->assetManager.SetApkAssets(apk->assets);
    return apk;
}

bool AndroidfwApkHasFramework(AndroidfwApk* apk)
{
    return apk && apk->hasFramework;
}

int AndroidfwApkGetResourceName(AndroidfwApk* apk, uint32_t resourceId, char* out, int outSize)
{
    if (!apk) return ANDROIDFW_ABSENT;

    auto name = apk->assetManager.GetResourceName(resourceId);
    if (!name.has_value()) return ANDROIDFW_ABSENT;

    std::string text;
    if (name->package) text.assign(name->package, name->package_len);

    text += ":";
    if (name->type)
        text.append(name->type, name->type_len);
    else if (name->type16)
        text += Utf16ToUtf8(name->type16, name->type_len);

    text += "/";
    if (name->entry)
        text.append(name->entry, name->entry_len);
    else if (name->entry16)
        text += Utf16ToUtf8(name->entry16, name->entry_len);

    return CopyOut(text, out, outSize);
}

bool AndroidfwApkGetResourceValue(AndroidfwApk* apk, uint32_t resourceId, int32_t* outType, uint32_t* outData, uint32_t* outResourceId)
{
    if (!apk || !outType || !outData) return false;

    auto selected = apk->assetManager.GetResource(resourceId, true);
    if (!selected.has_value()) return false;

    auto resolved = apk->assetManager.ResolveReference(selected.value());
    if (!resolved.has_value()) return false;

    *outType = selected->type;
    *outData = selected->data;
    if (outResourceId) *outResourceId = selected->resid;
    return true;
}

int AndroidfwApkGetArraySize(AndroidfwApk* apk, uint32_t resourceId)
{
    if (!apk) return -1;

    auto bag = apk->assetManager.GetBag(resourceId);
    if (!bag.has_value()) return -1;

    return (int)(*bag)->entry_count;
}

bool AndroidfwApkGetArrayEntry(AndroidfwApk* apk, uint32_t resourceId, int index, int32_t* outType, uint32_t* outData,
                               uint32_t* outResourceId)
{
    if (!apk || index < 0 || !outType || !outData) return false;

    auto bag = apk->assetManager.GetBag(resourceId);
    if (!bag.has_value()) return false;
    if (index >= (int)(*bag)->entry_count) return false;

    const android::ResolvedBag::Entry& entry = (*bag)->entries[index];

    android::AssetManager2::SelectedValue value{};
    value.cookie = entry.cookie;
    value.type = entry.value.dataType;
    value.data = entry.value.data;
    value.resid = 0;

    if (apk->assetManager.ResolveReference(value).ok())
    {
        *outType = value.type;
        *outData = value.data;
        if (outResourceId) *outResourceId = value.resid;

        return true;
    }

    *outType = entry.value.dataType;
    *outData = entry.value.data;
    if (outResourceId) *outResourceId = 0;

    return true;
}

AndroidfwTheme* AndroidfwThemeCreate(AndroidfwApk* apk)
{
    if (!apk) return nullptr;

    auto* theme = new AndroidfwTheme();
    theme->apk = apk;
    theme->theme = apk->assetManager.NewTheme();
    return theme;
}

void AndroidfwThemeDestroy(AndroidfwTheme* theme)
{
    delete theme;
}

bool AndroidfwThemeApplyStyle(AndroidfwTheme* theme, uint32_t resourceId, bool force)
{
    if (!theme || !theme->theme) return false;
    return theme->theme->ApplyStyle(resourceId, force).has_value();
}

bool AndroidfwThemeSetTo(AndroidfwTheme* target, AndroidfwTheme* source)
{
    if (!target || !target->theme || !source || !source->theme) return false;
    return target->theme->SetTo(*source->theme).has_value();
}

void AndroidfwThemeClear(AndroidfwTheme* theme)
{
    if (theme && theme->theme) theme->theme->Clear();
}

bool AndroidfwThemeResolveAttribute(AndroidfwTheme* theme, uint32_t attributeId, int32_t* outType, uint32_t* outData, uint32_t* outResourceId)
{
    if (!theme || !theme->theme || !outType || !outData) return false;

    auto value = theme->theme->GetAttribute(attributeId);
    if (!value.has_value()) return false;

    auto resolved = theme->theme->ResolveAttributeReference(value.value());
    if (!resolved.has_value()) return false;

    *outType = value->type;
    *outData = value->data;
    if (outResourceId) *outResourceId = value->resid;
    return true;
}

bool AndroidfwThemeObtainStyled(AndroidfwTheme* theme, AndroidfwXml* xml, uint32_t defStyleAttr, uint32_t defStyleResource,
                                const uint32_t* attributes, int attributeCount, uint32_t* outValues, uint32_t* outIndices)
{
    if (!theme || !theme->theme || !attributes || attributeCount <= 0 || !outValues) return false;

    if (xml)
    {
        if (!outIndices) return false;
        return android::ApplyStyle(theme->theme.get(), &xml->tree, defStyleAttr, defStyleResource, attributes, (size_t)attributeCount, outValues,
                                   outIndices)
            .has_value();
    }

    return android::ResolveAttrs(theme->theme.get(), defStyleAttr, defStyleResource, nullptr, 0, const_cast<uint32_t*>(attributes),
                                 (size_t)attributeCount, outValues, outIndices)
        .has_value();
}

bool AndroidfwObtainStyledNoTheme(AndroidfwApk* apk, AndroidfwXml* xml, const uint32_t* attributes, int attributeCount, uint32_t* outValues,
                                  uint32_t* outIndices)
{
    if (!apk || !xml || !attributes || attributeCount <= 0 || !outValues) return false;

    return android::RetrieveAttributes(&apk->assetManager, &xml->tree, const_cast<uint32_t*>(attributes), (size_t)attributeCount, outValues,
                                       outIndices)
        .has_value();
}

void AndroidfwApkUnload(AndroidfwApk* apk)
{
    delete apk;
}

bool AndroidfwApkSetSdkVersion(AndroidfwApk* apk, int sdkVersion)
{
    return AndroidfwApkSetConfiguration(apk, sdkVersion, 0, 0, 0);
}

bool AndroidfwApkSetConfiguration(AndroidfwApk* apk, int sdkVersion, int densityDpi, int screenWidthDp, int screenHeightDp)
{
    if (!apk) return false;

    android::ResTable_config config;
    memset(&config, 0, sizeof(config));
    config.sdkVersion = (uint16_t)sdkVersion;

    if (densityDpi > 0) config.density = (uint16_t)densityDpi;

    if (screenWidthDp > 0 && screenHeightDp > 0)
    {
        config.screenWidthDp = (uint16_t)screenWidthDp;
        config.screenHeightDp = (uint16_t)screenHeightDp;
        config.smallestScreenWidthDp = (uint16_t)(screenWidthDp < screenHeightDp ? screenWidthDp : screenHeightDp);
        config.orientation = screenWidthDp > screenHeightDp ? android::ResTable_config::ORIENTATION_LAND
                                                            : android::ResTable_config::ORIENTATION_PORT;
    }

    std::vector<android::ResTable_config> configs;
    configs.push_back(config);
    apk->assetManager.SetConfigurations(std::move(configs));
    return true;
}

int AndroidfwApkGetString(AndroidfwApk* apk, uint32_t resourceId, char* out, int outSize)
{
    if (!apk) return ANDROIDFW_ABSENT;

    auto selected = apk->assetManager.GetResource(resourceId, true);
    if (!selected.has_value()) return ANDROIDFW_ABSENT;

    auto resolved = apk->assetManager.ResolveReference(selected.value());
    if (!resolved.has_value()) return ANDROIDFW_ABSENT;

    if (selected->type != android::Res_value::TYPE_STRING) return ANDROIDFW_ABSENT;

    const android::ResStringPool* pool = apk->assetManager.GetStringPoolForCookie(selected->cookie);
    if (!pool) return ANDROIDFW_ABSENT;

    std::string value;
    if (!StringAt(*pool, selected->data, value)) return ANDROIDFW_ABSENT;

    return CopyOut(value, out, outSize);
}

static AndroidfwXml* OpenTree(std::unique_ptr<android::Asset> asset, std::shared_ptr<const android::DynamicRefTable> table)
{
    if (!asset) return nullptr;

    auto* xml = new AndroidfwXml(std::move(table));
    if (xml->tree.setTo(asset->getBuffer(true), asset->getLength(), true) != android::NO_ERROR)
    {
        delete xml;
        return nullptr;
    }

    xml->asset = std::move(asset);
    return xml;
}

struct AndroidfwZipRecord
{
    std::string name;
    AndroidfwZipEntry info = {};
};

struct AndroidfwZip
{
    android::ZipFileRO* archive = nullptr;
    std::vector<AndroidfwZipRecord> records;
};

AndroidfwZip* AndroidfwZipOpen(const char* path)
{
    if (!path || !*path) return nullptr;

    android::ZipFileRO* archive = android::ZipFileRO::open(path);
    if (!archive) return nullptr;

    auto zip = std::make_unique<AndroidfwZip>();
    zip->archive = archive;

    void* cookie = nullptr;
    if (!archive->startIteration(&cookie))
    {
        delete archive;
        return nullptr;
    }

    bool malformed = false;
    for (;;)
    {
        android::base::expected<android::ZipEntryRO, int32_t> next = archive->nextEntryOrError(cookie);
        if (!next.ok())
        {
            malformed = true;
            break;
        }

        android::ZipEntryRO entry = next.value();
        if (!entry) break;

        char name[ANDROIDFW_ZIP_MAX_NAME + 1];
        int length = archive->getEntryFileName(entry, name, sizeof(name));
        if (length != 0)
        {
            malformed = true;
            break;
        }

        uint16_t method = 0;
        uint32_t uncompressedSize = 0;
        uint32_t compressedSize = 0;
        off64_t dataOffset = 0;
        uint32_t crc = 0;
        if (!archive->getEntryInfo(entry, &method, &uncompressedSize, &compressedSize, &dataOffset, nullptr, &crc, nullptr))
        {
            malformed = true;
            break;
        }

        AndroidfwZipRecord record;
        record.name = name;
        record.info.dataOffset = dataOffset < 0 ? 0 : static_cast<uint64_t>(dataOffset);
        record.info.compressedSize = compressedSize;
        record.info.uncompressedSize = uncompressedSize;
        record.info.crc32 = crc;
        record.info.compressionMethod = method;
        zip->records.push_back(std::move(record));
    }

    archive->endIteration(cookie);

    if (malformed)
    {
        delete archive;
        return nullptr;
    }

    return zip.release();
}

void AndroidfwZipClose(AndroidfwZip* zip)
{
    if (!zip) return;

    delete zip->archive;
    delete zip;
}

int AndroidfwZipCount(AndroidfwZip* zip)
{
    if (!zip) return ANDROIDFW_ABSENT;
    return static_cast<int>(zip->records.size());
}

int AndroidfwZipEntryName(AndroidfwZip* zip, int index, char* out, int outSize)
{
    if (!zip || index < 0 || index >= static_cast<int>(zip->records.size())) return ANDROIDFW_ABSENT;
    return CopyOut(zip->records[static_cast<size_t>(index)].name, out, outSize);
}

bool AndroidfwZipEntryAt(AndroidfwZip* zip, int index, AndroidfwZipEntry* out)
{
    if (!zip || !out || index < 0 || index >= static_cast<int>(zip->records.size())) return false;

    *out = zip->records[static_cast<size_t>(index)].info;
    return true;
}

bool AndroidfwZipExtract(AndroidfwZip* zip, const char* name, int destinationDescriptor)
{
    if (!zip || !zip->archive || !name || destinationDescriptor < 0) return false;

    android::ZipEntryRO entry = zip->archive->findEntryByName(name);
    if (!entry) return false;

    bool extracted = zip->archive->uncompressEntry(entry, destinationDescriptor);
    zip->archive->releaseEntry(entry);

    return extracted;
}

struct AndroidfwAsset
{
    std::unique_ptr<android::Asset> asset;
};

AndroidfwAsset* AndroidfwAssetOpen(AndroidfwApk* apk, const char* path)
{
    if (!apk || !path) return nullptr;
    if (apk->applicationCookie == android::kInvalidCookie) return nullptr;

    std::unique_ptr<android::Asset> opened =
        apk->assetManager.Open(path, apk->applicationCookie, android::Asset::ACCESS_RANDOM);

    if (!opened) opened = apk->assetManager.Open(path, android::Asset::ACCESS_RANDOM);
    if (!opened) return nullptr;

    auto* wrapper = new AndroidfwAsset();
    wrapper->asset = std::move(opened);
    return wrapper;
}

AndroidfwAsset* AndroidfwAssetOpenNonAsset(AndroidfwApk* apk, const char* path)
{
    if (!apk || !path) return nullptr;
    if (apk->applicationCookie == android::kInvalidCookie) return nullptr;

    std::unique_ptr<android::Asset> opened =
        apk->assetManager.OpenNonAsset(path, apk->applicationCookie, android::Asset::ACCESS_RANDOM);
    if (!opened) return nullptr;

    auto* wrapper = new AndroidfwAsset();
    wrapper->asset = std::move(opened);
    return wrapper;
}

void AndroidfwAssetClose(AndroidfwAsset* asset)
{
    delete asset;
}

long AndroidfwAssetLength(AndroidfwAsset* asset)
{
    if (!asset || !asset->asset) return ANDROIDFW_ABSENT;
    return static_cast<long>(asset->asset->getLength());
}

int AndroidfwAssetRead(AndroidfwAsset* asset, void* buffer, int byteCount)
{
    if (!asset || !asset->asset || !buffer || byteCount < 0) return ANDROIDFW_ABSENT;

    ssize_t got = asset->asset->read(buffer, static_cast<size_t>(byteCount));
    if (got < 0) return ANDROIDFW_ABSENT;
    return static_cast<int>(got);
}

int AndroidfwAssetFileDescriptor(AndroidfwAsset* asset, long* outOffset, long* outLength)
{
    if (!asset || !asset->asset) return ANDROIDFW_ABSENT;

    off64_t start = 0;
    off64_t length = 0;
    int descriptor = asset->asset->openFileDescriptor(&start, &length);
    if (descriptor < 0) return ANDROIDFW_ABSENT;

    if (outOffset) *outOffset = static_cast<long>(start);
    if (outLength) *outLength = static_cast<long>(length);
    return descriptor;
}

int AndroidfwAssetList(AndroidfwApk* apk, const char* directory, char* out, int outSize)
{
    if (!apk || !directory) return ANDROIDFW_ABSENT;

    if (apk->applicationCookie == android::kInvalidCookie) return ANDROIDFW_ABSENT;

    std::unique_ptr<android::AssetDir> listing = apk->assetManager.OpenDir(directory);
    if (!listing) return ANDROIDFW_ABSENT;

    std::string prefix = directory[0] == '\0' ? std::string() : std::string(directory) + "/";

    std::string joined;
    for (size_t index = 0; index < listing->getFileCount(); ++index)
    {
        std::string name = listing->getFileName(index).c_str();

        std::unique_ptr<android::Asset> probe =
            apk->assetManager.Open(prefix + name, apk->applicationCookie, android::Asset::ACCESS_RANDOM);

        if (!probe) probe = apk->assetManager.Open(prefix + name, android::Asset::ACCESS_RANDOM);
        if (!probe) continue;

        if (!joined.empty()) joined.push_back('\n');
        joined.append(name);
    }

    return CopyOut(joined, out, outSize);
}

AndroidfwXml* AndroidfwXmlOpen(AndroidfwApk* apk, const char* assetPath)
{
    if (!apk || !assetPath) return nullptr;

    android::ApkAssetsCookie cookie = apk->applicationCookie;
    if (cookie == android::kInvalidCookie) return nullptr;

    std::unique_ptr<android::Asset> asset = apk->assetManager.OpenNonAsset(assetPath, cookie, android::Asset::ACCESS_BUFFER);
    if (!asset) return nullptr;

    return OpenTree(std::move(asset), apk->assetManager.GetDynamicRefTableForCookie(cookie));
}

AndroidfwXml* AndroidfwXmlOpenResource(AndroidfwApk* apk, uint32_t resourceId)
{
    if (!apk) return nullptr;

    auto selected = apk->assetManager.GetResource(resourceId, true);
    if (!selected.has_value()) return nullptr;

    auto resolved = apk->assetManager.ResolveReference(selected.value());
    if (!resolved.has_value()) return nullptr;

    if (selected->type != android::Res_value::TYPE_STRING) return nullptr;

    const android::ResStringPool* pool = apk->assetManager.GetStringPoolForCookie(selected->cookie);
    if (!pool) return nullptr;

    std::string path;
    if (!StringAt(*pool, selected->data, path)) return nullptr;

    std::unique_ptr<android::Asset> asset = apk->assetManager.OpenNonAsset(path, selected->cookie, android::Asset::ACCESS_BUFFER);
    if (!asset) return nullptr;

    return OpenTree(std::move(asset), apk->assetManager.GetDynamicRefTableForCookie(selected->cookie));
}

void AndroidfwXmlRestart(AndroidfwXml* xml)
{
    if (!xml) return;

    xml->tree.restart();
    xml->depth = 0;
    xml->leaving = false;
}

int AndroidfwXmlDepth(AndroidfwXml* xml)
{
    return xml ? xml->depth : 0;
}

int AndroidfwXmlAttributeIndexOfName(AndroidfwXml* xml, const char* xmlNamespace, const char* name)
{
    if (!xml || !name) return -1;

    size_t count = xml->tree.getAttributeCount();
    for (size_t index = 0; index < count; ++index)
    {
        size_t length = 0;
        const char16_t* attributeName = xml->tree.getAttributeName(index, &length);
        if (!attributeName) continue;
        if (Utf16ToUtf8(attributeName, length) != name) continue;

        if (!xmlNamespace || !*xmlNamespace) return (int)index;

        size_t namespaceLength = 0;
        const char16_t* attributeNamespace = xml->tree.getAttributeNamespace(index, &namespaceLength);
        if (!attributeNamespace) continue;
        if (Utf16ToUtf8(attributeNamespace, namespaceLength) == xmlNamespace) return (int)index;
    }

    return -1;
}

int AndroidfwXmlPooledString(AndroidfwXml* xml, uint32_t stringIndex, char* out, int outSize)
{
    if (!xml) return ANDROIDFW_ABSENT;

    std::string value;
    if (!StringAt(xml->tree.getStrings(), stringIndex, value)) return ANDROIDFW_ABSENT;

    return CopyOut(value, out, outSize);
}

bool AndroidfwXmlAttributeValue(AndroidfwXml* xml, int index, int32_t* outType, uint32_t* outData)
{
    if (!xml || !outType || !outData) return false;

    android::Res_value value;
    if (xml->tree.getAttributeValue(index, &value) < 0) return false;

    *outType = value.dataType;
    *outData = value.data;
    return true;
}

int AndroidfwApkGetPooledString(AndroidfwApk* apk, int cookie, uint32_t stringIndex, char* out, int outSize)
{
    if (!apk) return ANDROIDFW_ABSENT;

    const android::ResStringPool* pool = apk->assetManager.GetStringPoolForCookie(cookie);
    if (!pool) return ANDROIDFW_ABSENT;

    std::string value;
    if (!StringAt(*pool, stringIndex, value)) return ANDROIDFW_ABSENT;

    return CopyOut(value, out, outSize);
}

uint32_t AndroidfwApkGetIdentifier(AndroidfwApk* apk, const char* name, const char* defType, const char* defPackage)
{
    if (!apk || !name) return 0;

    auto identifier = apk->assetManager.GetResourceId(name, defType ? defType : "", defPackage ? defPackage : "");
    return identifier.has_value() ? identifier.value() : 0;
}

int AndroidfwXmlLineNumber(AndroidfwXml* xml)
{
    return xml ? (int)xml->tree.getLineNumber() : 0;
}

int AndroidfwXmlAttributeIndexOf(AndroidfwXml* xml, uint32_t resourceId)
{
    if (!xml) return -1;

    size_t count = xml->tree.getAttributeCount();
    for (size_t index = 0; index < count; ++index)
    {
        if (xml->tree.getAttributeNameResID(index) == resourceId) return (int)index;
    }

    return -1;
}

void AndroidfwXmlClose(AndroidfwXml* xml)
{
    delete xml;
}

AndroidfwXmlEvent AndroidfwXmlNext(AndroidfwXml* xml)
{
    if (!xml) return ANDROIDFW_XML_ERROR;

    if (xml->leaving)
    {
        if (xml->depth > 0) xml->depth--;
        xml->leaving = false;
    }

    for (;;)
    {
        switch (xml->tree.next())
        {
            case android::ResXMLParser::START_DOCUMENT: return ANDROIDFW_XML_START_DOCUMENT;
            case android::ResXMLParser::END_DOCUMENT: return ANDROIDFW_XML_END_DOCUMENT;
            case android::ResXMLParser::START_TAG: xml->depth++; return ANDROIDFW_XML_START_ELEMENT;
            case android::ResXMLParser::END_TAG:
                xml->leaving = true;
                return ANDROIDFW_XML_END_ELEMENT;
            case android::ResXMLParser::TEXT: return ANDROIDFW_XML_TEXT;
            case android::ResXMLParser::START_NAMESPACE:
            case android::ResXMLParser::END_NAMESPACE: continue;
            default: return ANDROIDFW_XML_ERROR;
        }
    }
}

int AndroidfwXmlElementName(AndroidfwXml* xml, char* out, int outSize)
{
    if (!xml) return ANDROIDFW_ABSENT;

    size_t length = 0;
    const char16_t* name = xml->tree.getElementName(&length);
    if (!name) return ANDROIDFW_ABSENT;
    return CopyOut(Utf16ToUtf8(name, length), out, outSize);
}

int AndroidfwXmlAttributeCount(AndroidfwXml* xml)
{
    if (!xml) return 0;
    return (int)xml->tree.getAttributeCount();
}

uint32_t AndroidfwXmlAttributeResourceId(AndroidfwXml* xml, int index)
{
    if (!xml) return 0;
    return xml->tree.getAttributeNameResID(index);
}

int AndroidfwXmlAttributeName(AndroidfwXml* xml, int index, char* out, int outSize)
{
    if (!xml) return ANDROIDFW_ABSENT;

    size_t length = 0;
    const char16_t* name = xml->tree.getAttributeName(index, &length);
    if (!name) return ANDROIDFW_ABSENT;
    return CopyOut(Utf16ToUtf8(name, length), out, outSize);
}

AndroidfwValueKind AndroidfwXmlAttributeKind(AndroidfwXml* xml, int index)
{
    if (!xml) return ANDROIDFW_VALUE_NULL;

    android::Res_value value;
    if (xml->tree.getAttributeValue(index, &value) < 0) return ANDROIDFW_VALUE_NULL;

    switch (value.dataType)
    {
        case android::Res_value::TYPE_NULL: return ANDROIDFW_VALUE_NULL;
        case android::Res_value::TYPE_REFERENCE: return ANDROIDFW_VALUE_REFERENCE;
        case android::Res_value::TYPE_STRING: return ANDROIDFW_VALUE_STRING;
        case android::Res_value::TYPE_INT_BOOLEAN: return ANDROIDFW_VALUE_BOOL;
        default: break;
    }

    if (value.dataType >= android::Res_value::TYPE_FIRST_INT && value.dataType <= android::Res_value::TYPE_LAST_INT)
    {
        return ANDROIDFW_VALUE_INT;
    }
    return ANDROIDFW_VALUE_OTHER;
}

int AndroidfwXmlAttributeString(AndroidfwXml* xml, int index, char* out, int outSize)
{
    if (!xml) return ANDROIDFW_ABSENT;

    size_t length = 0;
    const char16_t* value = xml->tree.getAttributeStringValue(index, &length);
    if (!value) return ANDROIDFW_ABSENT;
    return CopyOut(Utf16ToUtf8(value, length), out, outSize);
}

int32_t AndroidfwXmlAttributeInt(AndroidfwXml* xml, int index, int32_t fallback)
{
    if (!xml) return fallback;

    android::Res_value value;
    if (xml->tree.getAttributeValue(index, &value) < 0) return fallback;

    if (value.dataType < android::Res_value::TYPE_FIRST_INT) return fallback;
    if (value.dataType > android::Res_value::TYPE_LAST_INT) return fallback;

    return (int32_t)value.data;
}

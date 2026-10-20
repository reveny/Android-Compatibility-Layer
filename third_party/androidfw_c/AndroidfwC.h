#pragma once

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

    typedef struct AndroidfwApk AndroidfwApk;
    typedef struct AndroidfwXml AndroidfwXml;
    typedef struct AndroidfwTheme AndroidfwTheme;

#define ANDROIDFW_STYLE_NUM_ENTRIES 7
#define ANDROIDFW_STYLE_TYPE 0
#define ANDROIDFW_STYLE_DATA 1
#define ANDROIDFW_STYLE_ASSET_COOKIE 2
#define ANDROIDFW_STYLE_RESOURCE_ID 3
#define ANDROIDFW_STYLE_CHANGING_CONFIGURATIONS 4
#define ANDROIDFW_STYLE_DENSITY 5
#define ANDROIDFW_STYLE_SOURCE_RESOURCE_ID 6

#define ANDROIDFW_ABSENT (-1)

#define ANDROIDFW_ZIP_MAX_NAME 4095

    typedef enum
    {
        ANDROIDFW_XML_START_DOCUMENT = 0,
        ANDROIDFW_XML_END_DOCUMENT = 1,
        ANDROIDFW_XML_START_ELEMENT = 2,
        ANDROIDFW_XML_END_ELEMENT = 3,
        ANDROIDFW_XML_TEXT = 4,
        ANDROIDFW_XML_ERROR = 5
    } AndroidfwXmlEvent;

    typedef enum
    {
        ANDROIDFW_VALUE_NULL = 0,
        ANDROIDFW_VALUE_REFERENCE = 1,
        ANDROIDFW_VALUE_STRING = 2,
        ANDROIDFW_VALUE_INT = 3,
        ANDROIDFW_VALUE_BOOL = 4,
        ANDROIDFW_VALUE_OTHER = 5
    } AndroidfwValueKind;

    AndroidfwApk* AndroidfwApkLoad(const char* frameworkPath, const char* path);
    AndroidfwApk* AndroidfwApkLoadWithSplits(const char* frameworkPath, const char* path, const char* const* splitPaths, int splitCount);
    void AndroidfwApkUnload(AndroidfwApk* apk);
    bool AndroidfwApkSetSdkVersion(AndroidfwApk* apk, int sdkVersion);
    bool AndroidfwApkSetConfiguration(AndroidfwApk* apk, int sdkVersion, int densityDpi, int screenWidthDp, int screenHeightDp);
    bool AndroidfwApkHasFramework(AndroidfwApk* apk);

    int AndroidfwApkGetString(AndroidfwApk* apk, uint32_t resourceId, char* out, int outSize);
    int AndroidfwApkGetPooledString(AndroidfwApk* apk, int cookie, uint32_t stringIndex, char* out, int outSize);
    int AndroidfwApkGetResourceName(AndroidfwApk* apk, uint32_t resourceId, char* out, int outSize);
    uint32_t AndroidfwApkGetIdentifier(AndroidfwApk* apk, const char* name, const char* defType, const char* defPackage);
    bool AndroidfwApkGetResourceValue(AndroidfwApk* apk, uint32_t resourceId, int32_t* outType, uint32_t* outData, uint32_t* outResourceId);
    int AndroidfwApkGetArraySize(AndroidfwApk* apk, uint32_t resourceId);
    bool AndroidfwApkGetArrayEntry(AndroidfwApk* apk, uint32_t resourceId, int index, int32_t* outType, uint32_t* outData,
                                   uint32_t* outResourceId);

    typedef struct AndroidfwZip AndroidfwZip;

    typedef struct
    {
        uint64_t dataOffset;
        uint64_t compressedSize;
        uint64_t uncompressedSize;
        uint32_t crc32;
        uint16_t compressionMethod;
    } AndroidfwZipEntry;

    AndroidfwZip* AndroidfwZipOpen(const char* path);
    void AndroidfwZipClose(AndroidfwZip* zip);
    int AndroidfwZipCount(AndroidfwZip* zip);
    int AndroidfwZipEntryName(AndroidfwZip* zip, int index, char* out, int outSize);
    bool AndroidfwZipEntryAt(AndroidfwZip* zip, int index, AndroidfwZipEntry* out);
    bool AndroidfwZipExtract(AndroidfwZip* zip, const char* name, int destinationDescriptor);

    typedef struct AndroidfwAsset AndroidfwAsset;

    AndroidfwAsset* AndroidfwAssetOpen(AndroidfwApk* apk, const char* path);
    AndroidfwAsset* AndroidfwAssetOpenNonAsset(AndroidfwApk* apk, const char* path);
    void AndroidfwAssetClose(AndroidfwAsset* asset);
    long AndroidfwAssetLength(AndroidfwAsset* asset);
    int AndroidfwAssetRead(AndroidfwAsset* asset, void* buffer, int byteCount);
    int AndroidfwAssetFileDescriptor(AndroidfwAsset* asset, long* outOffset, long* outLength);
    int AndroidfwAssetList(AndroidfwApk* apk, const char* directory, char* out, int outSize);

    AndroidfwTheme* AndroidfwThemeCreate(AndroidfwApk* apk);
    void AndroidfwThemeDestroy(AndroidfwTheme* theme);
    bool AndroidfwThemeApplyStyle(AndroidfwTheme* theme, uint32_t resourceId, bool force);
    bool AndroidfwThemeSetTo(AndroidfwTheme* target, AndroidfwTheme* source);
    void AndroidfwThemeClear(AndroidfwTheme* theme);
    bool AndroidfwThemeResolveAttribute(AndroidfwTheme* theme, uint32_t attributeId, int32_t* outType, uint32_t* outData, uint32_t* outResourceId);

    bool AndroidfwThemeObtainStyled(AndroidfwTheme* theme, AndroidfwXml* xml, uint32_t defStyleAttr, uint32_t defStyleResource,
                                    const uint32_t* attributes, int attributeCount, uint32_t* outValues, uint32_t* outIndices);
    bool AndroidfwObtainStyledNoTheme(AndroidfwApk* apk, AndroidfwXml* xml, const uint32_t* attributes, int attributeCount, uint32_t* outValues,
                                      uint32_t* outIndices);

    AndroidfwXml* AndroidfwXmlOpen(AndroidfwApk* apk, const char* assetPath);
    AndroidfwXml* AndroidfwXmlOpenResource(AndroidfwApk* apk, uint32_t resourceId);
    void AndroidfwXmlClose(AndroidfwXml* xml);
    void AndroidfwXmlRestart(AndroidfwXml* xml);
    int AndroidfwXmlLineNumber(AndroidfwXml* xml);
    int AndroidfwXmlDepth(AndroidfwXml* xml);
    int AndroidfwXmlAttributeIndexOf(AndroidfwXml* xml, uint32_t resourceId);
    int AndroidfwXmlAttributeIndexOfName(AndroidfwXml* xml, const char* xmlNamespace, const char* name);
    int AndroidfwXmlPooledString(AndroidfwXml* xml, uint32_t stringIndex, char* out, int outSize);
    bool AndroidfwXmlAttributeValue(AndroidfwXml* xml, int index, int32_t* outType, uint32_t* outData);

    AndroidfwXmlEvent AndroidfwXmlNext(AndroidfwXml* xml);
    int AndroidfwXmlElementName(AndroidfwXml* xml, char* out, int outSize);

    int AndroidfwXmlAttributeCount(AndroidfwXml* xml);
    uint32_t AndroidfwXmlAttributeResourceId(AndroidfwXml* xml, int index);
    int AndroidfwXmlAttributeName(AndroidfwXml* xml, int index, char* out, int outSize);
    AndroidfwValueKind AndroidfwXmlAttributeKind(AndroidfwXml* xml, int index);
    int AndroidfwXmlAttributeString(AndroidfwXml* xml, int index, char* out, int outSize);
    int32_t AndroidfwXmlAttributeInt(AndroidfwXml* xml, int index, int32_t fallback);

#ifdef __cplusplus
}
#endif

#include "FrameworkJni/ResourceBridge.hpp"

#include <cstdio>
#include <mutex>
#include <set>
#include <string>
#include <unistd.h>
#include <vector>

#include "AndroidfwC.h"
#include "FrameworkJni/FrameworkJni.hpp"

static std::mutex gHandleLock;
static std::set<jlong> gLiveThemes;
static std::set<jlong> gLiveXml;

static AndroidfwApk* ApkFrom(jlong handle)
{
    const Apk::Package* package = FrameworkJni::GetPackage();
    if (!package || package->assetsHandle != reinterpret_cast<void*>(handle)) return nullptr;
    return reinterpret_cast<AndroidfwApk*>(handle);
}

static AndroidfwTheme* ThemeFrom(jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    if (handle == 0 || gLiveThemes.find(handle) == gLiveThemes.end()) return nullptr;
    return reinterpret_cast<AndroidfwTheme*>(handle);
}

static AndroidfwXml* XmlFrom(jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    if (handle == 0 || gLiveXml.find(handle) == gLiveXml.end()) return nullptr;
    return reinterpret_cast<AndroidfwXml*>(handle);
}

static void RegisterHandle(std::set<jlong>& registry, jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    registry.insert(handle);
}

static void ForgetHandle(std::set<jlong>& registry, jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    registry.erase(handle);
}

static std::string TakeString(JNIEnv* env, jstring value)
{
    if (!value) return std::string();

    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return std::string();

    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

static bool GrownText(int (*fetch)(void*, char*, int), void* context, std::string& out)
{
    std::string buffer(1024, '\0');

    int needed = fetch(context, buffer.data(), static_cast<int>(buffer.size()));
    if (needed == ANDROIDFW_ABSENT) return false;

    if (needed >= static_cast<int>(buffer.size()))
    {
        if (needed >= ResourceBridge::MAX_RESOURCE_TEXT) return false;
        buffer.assign(static_cast<size_t>(needed) + 1, '\0');
        needed = fetch(context, buffer.data(), static_cast<int>(buffer.size()));
        if (needed == ANDROIDFW_ABSENT || needed >= static_cast<int>(buffer.size())) return false;
    }

    buffer.resize(static_cast<size_t>(needed));
    out = buffer;
    return true;
}

static jstring Grown(JNIEnv* env, int (*fetch)(void*, char*, int), void* context)
{
    std::string text;
    if (!GrownText(fetch, context, text)) return nullptr;
    return env->NewStringUTF(text.c_str());
}

struct ResourceRequest
{
    AndroidfwApk* apk = nullptr;
    uint32_t resourceId = 0;
    int cookie = 0;
};

static int FetchString(void* context, char* out, int outSize)
{
    ResourceRequest* request = static_cast<ResourceRequest*>(context);
    return AndroidfwApkGetString(request->apk, request->resourceId, out, outSize);
}

static int FetchPooled(void* context, char* out, int outSize)
{
    ResourceRequest* request = static_cast<ResourceRequest*>(context);
    return AndroidfwApkGetPooledString(request->apk, request->cookie, request->resourceId, out, outSize);
}

static int FetchResourceName(void* context, char* out, int outSize)
{
    ResourceRequest* request = static_cast<ResourceRequest*>(context);
    return AndroidfwApkGetResourceName(request->apk, request->resourceId, out, outSize);
}

static jstring ResourcesGetString(JNIEnv* env, jclass, jlong handle, jint resourceId)
{
    bool trace = FrameworkJni::IsTracing("resources");

    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk)
    {
        if (trace) fprintf(stderr, "[resources] 0x%08x -> <no package>\n", static_cast<unsigned>(resourceId));
        return nullptr;
    }

    ResourceRequest request{apk, static_cast<uint32_t>(resourceId), 0};

    std::string value;
    if (!GrownText(&FetchString, &request, value))
    {
        if (trace) fprintf(stderr, "[resources] 0x%08x -> EntryNotFound\n", static_cast<unsigned>(resourceId));
        return nullptr;
    }

    if (trace)
    {
        fprintf(stderr, "[resources] 0x%08x -> \"%s\"\n", static_cast<unsigned>(resourceId), value.c_str());
        fflush(stderr);
    }

    return env->NewStringUTF(value.c_str());
}

static jstring ResourcesGetPooledString(JNIEnv* env, jclass, jlong handle, jint cookie, jint stringIndex)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return nullptr;

    ResourceRequest request{apk, static_cast<uint32_t>(stringIndex), cookie};
    return Grown(env, &FetchPooled, &request);
}

static jstring ResourcesGetResourceName(JNIEnv* env, jclass, jlong handle, jint resourceId)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return nullptr;

    ResourceRequest request{apk, static_cast<uint32_t>(resourceId), 0};
    return Grown(env, &FetchResourceName, &request);
}

static jint ResourcesGetIdentifier(JNIEnv* env, jclass, jlong handle, jstring name, jstring defType, jstring defPackage)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return 0;

    std::string nameText = TakeString(env, name);
    std::string typeText = TakeString(env, defType);
    std::string packageText = TakeString(env, defPackage);

    return static_cast<jint>(AndroidfwApkGetIdentifier(apk, nameText.c_str(), typeText.c_str(), packageText.c_str()));
}

static jintArray PackTriple(JNIEnv* env, int32_t type, uint32_t data, uint32_t resourceId)
{
    jintArray array = env->NewIntArray(3);
    if (!array) return nullptr;

    jint values[3] = {static_cast<jint>(type), static_cast<jint>(data), static_cast<jint>(resourceId)};
    env->SetIntArrayRegion(array, 0, 3, values);
    return array;
}

static jintArray ResourcesGetResourceValue(JNIEnv* env, jclass, jlong handle, jint resourceId)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return nullptr;

    int32_t type = 0;
    uint32_t data = 0;
    uint32_t resolved = 0;
    if (!AndroidfwApkGetResourceValue(apk, static_cast<uint32_t>(resourceId), &type, &data, &resolved)) return nullptr;

    return PackTriple(env, type, data, resolved);
}

static jint ResourcesGetArraySize(JNIEnv*, jclass, jlong handle, jint resourceId)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return -1;

    return static_cast<jint>(AndroidfwApkGetArraySize(apk, static_cast<uint32_t>(resourceId)));
}

static jintArray ResourcesGetArrayEntry(JNIEnv* env, jclass, jlong handle, jint resourceId, jint index)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return nullptr;

    int32_t type = 0;
    uint32_t data = 0;
    uint32_t resolved = 0;

    if (!AndroidfwApkGetArrayEntry(apk, static_cast<uint32_t>(resourceId), index, &type, &data, &resolved)) return nullptr;

    return PackTriple(env, type, data, resolved);
}

static jlong ResourcesOpenXmlResource(JNIEnv*, jclass, jlong handle, jint resourceId)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return 0;

    AndroidfwXml* xml = AndroidfwXmlOpenResource(apk, static_cast<uint32_t>(resourceId));
    if (!xml) return 0;

    jlong xmlHandle = reinterpret_cast<jlong>(xml);
    RegisterHandle(gLiveXml, xmlHandle);
    return xmlHandle;
}

static jlong ResourcesOpenXmlBuffer(JNIEnv* env, jclass, jlong handle, jbyteArray data)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk || !data) return 0;

    jsize length = env->GetArrayLength(data);
    if (length <= 0) return 0;

    std::vector<jbyte> bytes(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, 0, length, bytes.data());

    AndroidfwXml* xml = AndroidfwXmlOpenBuffer(apk, bytes.data(), static_cast<uint32_t>(length));
    if (!xml) return 0;

    jlong xmlHandle = reinterpret_cast<jlong>(xml);
    RegisterHandle(gLiveXml, xmlHandle);
    return xmlHandle;
}

static bool ReadAttributes(JNIEnv* env, jintArray attributes, std::vector<uint32_t>& out)
{
    jsize count = env->GetArrayLength(attributes);
    if (count <= 0) return false;

    out.resize(static_cast<size_t>(count));
    env->GetIntArrayRegion(attributes, 0, count, reinterpret_cast<jint*>(out.data()));
    return true;
}

static void WriteResults(JNIEnv* env, jintArray outValues, jintArray outIndices, const std::vector<uint32_t>& values,
                         const std::vector<uint32_t>& indices)
{
    env->SetIntArrayRegion(outValues, 0, static_cast<jsize>(values.size()), reinterpret_cast<const jint*>(values.data()));
    if (outIndices) env->SetIntArrayRegion(outIndices, 0, static_cast<jsize>(indices.size()), reinterpret_cast<const jint*>(indices.data()));
}

static void ResourcesObtainNoTheme(JNIEnv* env, jclass, jlong handle, jlong xmlHandle, jintArray attributes, jintArray outValues,
                                   jintArray outIndices)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return;

    std::vector<uint32_t> wanted;
    if (!ReadAttributes(env, attributes, wanted)) return;

    std::vector<uint32_t> values(wanted.size() * ANDROIDFW_STYLE_NUM_ENTRIES, 0);
    std::vector<uint32_t> indices(wanted.size() + 1, 0);

    AndroidfwObtainStyledNoTheme(apk, XmlFrom(xmlHandle), wanted.data(), static_cast<int>(wanted.size()), values.data(),
                                 indices.data());

    WriteResults(env, outValues, outIndices, values, indices);
}

static std::set<jlong> gLiveAssets;
static std::set<int> gLiveDescriptors;

static jlong AssetsOpen(JNIEnv* env, jclass, jlong handle, jstring path)
{
    const Apk::Package* package = FrameworkJni::GetPackage();
    if (!package || !ApkFrom(handle) || !path) return 0;

    std::string wanted = TakeString(env, path);
    if (wanted.empty()) return 0;

    Apk::Asset* asset = new Apk::Asset();
    if (Apk::OpenAsset(*package, wanted, *asset) != Apk::Result::Ok)
    {
        delete asset;
        return 0;
    }

    jlong assetHandle = reinterpret_cast<jlong>(asset);
    RegisterHandle(gLiveAssets, assetHandle);
    return assetHandle;
}

static jlong AssetsOpenNonAsset(JNIEnv* env, jclass, jlong handle, jstring path)
{
    const Apk::Package* package = FrameworkJni::GetPackage();
    if (!package || !ApkFrom(handle) || !path) return 0;

    std::string wanted = TakeString(env, path);
    if (wanted.empty()) return 0;

    Apk::Asset* asset = new Apk::Asset();
    if (Apk::OpenNonAsset(*package, wanted, *asset) != Apk::Result::Ok)
    {
        delete asset;
        return 0;
    }

    jlong assetHandle = reinterpret_cast<jlong>(asset);
    RegisterHandle(gLiveAssets, assetHandle);
    return assetHandle;
}

static Apk::Asset* AssetFrom(jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    if (handle == 0 || gLiveAssets.find(handle) == gLiveAssets.end()) return nullptr;
    return reinterpret_cast<Apk::Asset*>(handle);
}

static Apk::Asset* ClaimAsset(jlong handle)
{
    std::lock_guard<std::mutex> held(gHandleLock);
    if (handle == 0 || gLiveAssets.erase(handle) == 0) return nullptr;
    return reinterpret_cast<Apk::Asset*>(handle);
}

static jobject AssetsOpenFd(JNIEnv* env, jclass, jlong assetHandle, jlongArray outExtent)
{
    Apk::Asset* asset = AssetFrom(assetHandle);
    if (!asset || !outExtent) return nullptr;
    if (env->GetArrayLength(outExtent) < 3) return nullptr;

    int descriptor = -1;
    int64_t offset = 0;
    int64_t length = 0;
    if (Apk::AssetFileDescriptor(*asset, descriptor, offset, length) != Apk::Result::Ok) return nullptr;

    jclass descriptorClass = env->FindClass("java/io/FileDescriptor");
    if (!descriptorClass)
    {
        env->ExceptionClear();
        close(descriptor);
        FrameworkJni::Unimplemented(env, "java.io.FileDescriptor is missing");
        return nullptr;
    }

    jmethodID init = env->GetMethodID(descriptorClass, "<init>", "()V");
    if (!init)
    {
        env->ExceptionClear();
        close(descriptor);
        FrameworkJni::Unimplemented(env, "java.io.FileDescriptor has no no-argument constructor");
        return nullptr;
    }

    jmethodID setInt = env->GetMethodID(descriptorClass, "setInt$", "(I)V");
    if (!setInt)
    {
        env->ExceptionClear();
        close(descriptor);
        FrameworkJni::Unimplemented(env, "java.io.FileDescriptor has no setInt$(int)");
        return nullptr;
    }

    jobject produced = env->NewObject(descriptorClass, init);
    if (!produced)
    {
        close(descriptor);
        return nullptr;
    }

    env->CallVoidMethod(produced, setInt, static_cast<jint>(descriptor));

    {
        std::lock_guard<std::mutex> held(gHandleLock);
        gLiveDescriptors.insert(descriptor);
    }

    jlong extent[3] = {static_cast<jlong>(descriptor), static_cast<jlong>(offset), static_cast<jlong>(length)};
    env->SetLongArrayRegion(outExtent, 0, 3, extent);

    return produced;
}

static void InvalidateDescriptor(JNIEnv* env, jobject descriptor)
{
    if (!descriptor) return;

    jclass descriptorClass = env->GetObjectClass(descriptor);
    jmethodID setInt = descriptorClass ? env->GetMethodID(descriptorClass, "setInt$", "(I)V") : nullptr;
    if (!setInt)
    {
        env->ExceptionClear();
        return;
    }

    env->CallVoidMethod(descriptor, setInt, static_cast<jint>(-1));
}

static jboolean DescriptorClose(JNIEnv* env, jclass, jobject descriptor, jint fileDescriptor)
{
    InvalidateDescriptor(env, descriptor);

    if (fileDescriptor < 0) return JNI_FALSE;

    {
        std::lock_guard<std::mutex> held(gHandleLock);
        if (gLiveDescriptors.erase(fileDescriptor) == 0)
        {
            fprintf(stderr, "[resources] refusing to close descriptor %d, which this runtime did not open\n", fileDescriptor);
            return JNI_FALSE;
        }
    }

    return close(fileDescriptor) == 0 ? JNI_TRUE : JNI_FALSE;
}

static void DescriptorInvalidate(JNIEnv* env, jclass, jobject descriptor)
{
    if (descriptor)
    {
        jclass descriptorClass = env->GetObjectClass(descriptor);
        jmethodID getInt = descriptorClass ? env->GetMethodID(descriptorClass, "getInt$", "()I") : nullptr;

        if (getInt)
        {
            jint detached = env->CallIntMethod(descriptor, getInt);
            if (detached >= 0)
            {
                std::lock_guard<std::mutex> held(gHandleLock);
                gLiveDescriptors.erase(detached);
            }
        }
        else
        {
            env->ExceptionClear();
        }
    }

    InvalidateDescriptor(env, descriptor);
}

static jlong AssetsLength(JNIEnv*, jclass, jlong assetHandle)
{
    Apk::Asset* asset = AssetFrom(assetHandle);
    return asset ? static_cast<jlong>(asset->length) : -1;
}

static jint AssetsRead(JNIEnv* env, jclass, jlong assetHandle, jbyteArray buffer, jint offset, jint count)
{
    Apk::Asset* asset = AssetFrom(assetHandle);
    if (!asset || !buffer || offset < 0 || count < 0) return -1;

    jsize length = env->GetArrayLength(buffer);
    if (offset > length || count > length - offset) return -1;
    if (count == 0) return 0;

    std::vector<uint8_t> scratch(static_cast<size_t>(count));
    int got = Apk::ReadAsset(*asset, scratch.data(), count);
    if (got == 0) return -1;
    if (got < 0) return -2;

    env->SetByteArrayRegion(buffer, offset, got, reinterpret_cast<const jbyte*>(scratch.data()));
    return got;
}

static void AssetsClose(JNIEnv*, jclass, jlong assetHandle)
{
    Apk::Asset* asset = ClaimAsset(assetHandle);
    if (!asset) return;

    Apk::CloseAsset(*asset);
    delete asset;
}

static jobjectArray AssetsList(JNIEnv* env, jclass, jlong handle, jstring directory)
{
    const Apk::Package* package = FrameworkJni::GetPackage();
    if (!package || !ApkFrom(handle)) return nullptr;

    std::string wanted = directory ? TakeString(env, directory) : std::string();

    std::vector<std::string> names;
    if (Apk::ListAssets(*package, wanted, names) != Apk::Result::Ok) return nullptr;

    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) return nullptr;

    jobjectArray result = env->NewObjectArray(static_cast<jsize>(names.size()), stringClass, nullptr);
    if (!result) return nullptr;

    for (size_t index = 0; index < names.size(); ++index)
    {
        jstring name = env->NewStringUTF(names[index].c_str());
        env->SetObjectArrayElement(result, static_cast<jsize>(index), name);
        env->DeleteLocalRef(name);
    }

    return result;
}

static jlong ResourcesCreateTheme(JNIEnv*, jclass, jlong handle)
{
    AndroidfwApk* apk = ApkFrom(handle);
    if (!apk) return 0;

    AndroidfwTheme* theme = AndroidfwThemeCreate(apk);
    if (!theme) return 0;

    jlong themeHandle = reinterpret_cast<jlong>(theme);
    RegisterHandle(gLiveThemes, themeHandle);
    return themeHandle;
}

static void ResourcesDestroyTheme(JNIEnv*, jclass, jlong themeHandle)
{
    AndroidfwTheme* theme = ThemeFrom(themeHandle);
    if (!theme) return;

    ForgetHandle(gLiveThemes, themeHandle);
    AndroidfwThemeDestroy(theme);
}

static void ResourcesClearTheme(JNIEnv*, jclass, jlong themeHandle)
{
    AndroidfwTheme* theme = ThemeFrom(themeHandle);
    if (!theme) return;

    AndroidfwThemeClear(theme);
}

static void ResourcesApplyStyle(JNIEnv*, jclass, jlong themeHandle, jint resourceId, jboolean force)
{
    AndroidfwTheme* theme = ThemeFrom(themeHandle);
    if (!theme) return;

    bool applied = AndroidfwThemeApplyStyle(theme, static_cast<uint32_t>(resourceId), force == JNI_TRUE);

    if (FrameworkJni::IsTracing("styled") || (!applied && FrameworkJni::IsTracing("resources")))
    {
        fprintf(stderr, "[styled] applyStyle theme=%p style=0x%08x applied=%d\n", reinterpret_cast<void*>(themeHandle),
                static_cast<unsigned>(resourceId), applied ? 1 : 0);
        fflush(stderr);
    }
}

static void ResourcesSetTo(JNIEnv*, jclass, jlong targetHandle, jlong sourceHandle)
{
    AndroidfwTheme* target = ThemeFrom(targetHandle);
    AndroidfwTheme* source = ThemeFrom(sourceHandle);
    if (!target || !source) return;

    bool copied = AndroidfwThemeSetTo(target, source);
    if (FrameworkJni::IsTracing("styled"))
    {
        fprintf(stderr, "[styled] setTo target=%p source=%p copied=%d\n", (void*)target, (void*)source, copied ? 1 : 0);
        fflush(stderr);
    }
}

static jintArray ResourcesResolveAttribute(JNIEnv* env, jclass, jlong themeHandle, jint attributeId)
{
    AndroidfwTheme* theme = ThemeFrom(themeHandle);
    if (!theme) return nullptr;

    int32_t type = 0;
    uint32_t data = 0;
    uint32_t resourceId = 0;
    if (!AndroidfwThemeResolveAttribute(theme, static_cast<uint32_t>(attributeId), &type, &data, &resourceId)) return nullptr;

    return PackTriple(env, type, data, resourceId);
}

static void ResourcesObtainStyled(JNIEnv* env, jclass, jlong themeHandle, jlong xmlHandle, jint defStyleAttr, jint defStyleRes, jintArray attributes,
                                  jintArray outValues, jintArray outIndices)
{
    AndroidfwTheme* theme = ThemeFrom(themeHandle);
    if (!theme) return;

    std::vector<uint32_t> wanted;
    if (!ReadAttributes(env, attributes, wanted)) return;

    std::vector<uint32_t> values(wanted.size() * ANDROIDFW_STYLE_NUM_ENTRIES, 0);
    std::vector<uint32_t> indices(wanted.size() + 1, 0);

    bool ok =
        AndroidfwThemeObtainStyled(theme, XmlFrom(xmlHandle), static_cast<uint32_t>(defStyleAttr),
                                   static_cast<uint32_t>(defStyleRes), wanted.data(), static_cast<int>(wanted.size()), values.data(), indices.data());

    if (FrameworkJni::IsTracing("styled"))
    {
        fprintf(stderr, "[styled] theme=%p attrs=%zu ok=%d resolved=%u first=0x%08x\n", reinterpret_cast<void*>(themeHandle), wanted.size(),
                ok ? 1 : 0, indices[0], wanted.empty() ? 0u : wanted[0]);
        fflush(stderr);
    }

    WriteResults(env, outValues, outIndices, values, indices);
}

static const JNINativeMethod RESOURCE_METHODS[] = {
    {"nativeGetString", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&ResourcesGetString)},
    {"nativeGetPooledString", "(JII)Ljava/lang/String;", reinterpret_cast<void*>(&ResourcesGetPooledString)},
    {"nativeGetResourceName", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&ResourcesGetResourceName)},
    {"nativeGetIdentifier", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", reinterpret_cast<void*>(&ResourcesGetIdentifier)},
    {"nativeGetResourceValue", "(JI)[I", reinterpret_cast<void*>(&ResourcesGetResourceValue)},
    {"nativeGetArraySize", "(JI)I", reinterpret_cast<void*>(&ResourcesGetArraySize)},
    {"nativeGetArrayEntry", "(JII)[I", reinterpret_cast<void*>(&ResourcesGetArrayEntry)},
    {"nativeOpenXmlResource", "(JI)J", reinterpret_cast<void*>(&ResourcesOpenXmlResource)},
    {"nativeOpenXmlBuffer", "(J[B)J", reinterpret_cast<void*>(&ResourcesOpenXmlBuffer)},
    {"nativeObtainNoTheme", "(JJ[I[I[I)V", reinterpret_cast<void*>(&ResourcesObtainNoTheme)},
    {"nativeCreateTheme", "(J)J", reinterpret_cast<void*>(&ResourcesCreateTheme)},
    {"nativeApplyStyle", "(JIZ)V", reinterpret_cast<void*>(&ResourcesApplyStyle)},
    {"nativeSetTo", "(JJ)V", reinterpret_cast<void*>(&ResourcesSetTo)},
    {"nativeResolveAttribute", "(JI)[I", reinterpret_cast<void*>(&ResourcesResolveAttribute)},
    {"nativeObtainStyled", "(JJII[I[I[I)V", reinterpret_cast<void*>(&ResourcesObtainStyled)},
    {"nativeClearTheme", "(J)V", reinterpret_cast<void*>(&ResourcesClearTheme)},
    {"nativeDestroyTheme", "(J)V", reinterpret_cast<void*>(&ResourcesDestroyTheme)},
    {"nativeAssetOpen", "(JLjava/lang/String;)J", reinterpret_cast<void*>(&AssetsOpen)},
    {"nativeAssetOpenNonAsset", "(JLjava/lang/String;)J", reinterpret_cast<void*>(&AssetsOpenNonAsset)},
    {"nativeAssetOpenFd", "(J[J)Ljava/io/FileDescriptor;", reinterpret_cast<void*>(&AssetsOpenFd)},
    {"nativeAssetLength", "(J)J", reinterpret_cast<void*>(&AssetsLength)},
    {"nativeAssetRead", "(J[BII)I", reinterpret_cast<void*>(&AssetsRead)},
    {"nativeAssetClose", "(J)V", reinterpret_cast<void*>(&AssetsClose)},
    {"nativeAssetList", "(JLjava/lang/String;)[Ljava/lang/String;", reinterpret_cast<void*>(&AssetsList)},
};

static jint XmlNext(JNIEnv*, jclass, jlong handle)
{
    AndroidfwXml* xml = XmlFrom(handle);
    if (!xml) return ANDROIDFW_XML_END_DOCUMENT;

    AndroidfwXmlEvent event = AndroidfwXmlNext(xml);
    if (event == ANDROIDFW_XML_ERROR) return ANDROIDFW_XML_END_DOCUMENT;
    return static_cast<jint>(event);
}

static jint XmlDepth(JNIEnv*, jclass, jlong handle)
{
    return AndroidfwXmlDepth(XmlFrom(handle));
}

static void XmlClose(JNIEnv*, jclass, jlong handle)
{
    AndroidfwXml* xml = XmlFrom(handle);
    if (!xml) return;

    ForgetHandle(gLiveXml, handle);
    AndroidfwXmlClose(xml);
}

static jint XmlLineNumber(JNIEnv*, jclass, jlong handle)
{
    return AndroidfwXmlLineNumber(XmlFrom(handle));
}

static jint XmlAttributeCount(JNIEnv*, jclass, jlong handle)
{
    return AndroidfwXmlAttributeCount(XmlFrom(handle));
}

static jint XmlAttributeNameResource(JNIEnv*, jclass, jlong handle, jint index)
{
    return static_cast<jint>(AndroidfwXmlAttributeResourceId(XmlFrom(handle), index));
}

static jint XmlIndexOfResource(JNIEnv*, jclass, jlong handle, jint resourceId)
{
    return AndroidfwXmlAttributeIndexOf(XmlFrom(handle), static_cast<uint32_t>(resourceId));
}

static jint XmlIndexOfName(JNIEnv* env, jclass, jlong handle, jstring xmlNamespace, jstring name)
{
    std::string namespaceText = TakeString(env, xmlNamespace);
    std::string nameText = TakeString(env, name);

    return AndroidfwXmlAttributeIndexOfName(XmlFrom(handle), namespaceText.empty() ? nullptr : namespaceText.c_str(), nameText.c_str());
}

static jintArray XmlAttributeValue(JNIEnv* env, jclass, jlong handle, jint index)
{
    int32_t type = 0;
    uint32_t data = 0;
    if (!AndroidfwXmlAttributeValue(XmlFrom(handle), index, &type, &data)) return nullptr;

    jintArray array = env->NewIntArray(2);
    if (!array) return nullptr;

    jint values[2] = {static_cast<jint>(type), static_cast<jint>(data)};
    env->SetIntArrayRegion(array, 0, 2, values);
    return array;
}

struct XmlRequest
{
    AndroidfwXml* xml = nullptr;
    int index = 0;
};

static int FetchPooledXmlString(void* context, char* out, int outSize)
{
    XmlRequest* request = static_cast<XmlRequest*>(context);
    return AndroidfwXmlPooledString(request->xml, static_cast<uint32_t>(request->index), out, outSize);
}

static jstring XmlPooledString(JNIEnv* env, jclass, jlong handle, jint stringIndex)
{
    XmlRequest request{XmlFrom(handle), stringIndex};
    return Grown(env, &FetchPooledXmlString, &request);
}

static int FetchElementName(void* context, char* out, int outSize)
{
    return AndroidfwXmlElementName(static_cast<XmlRequest*>(context)->xml, out, outSize);
}

static int FetchAttributeName(void* context, char* out, int outSize)
{
    XmlRequest* request = static_cast<XmlRequest*>(context);
    return AndroidfwXmlAttributeName(request->xml, request->index, out, outSize);
}

static int FetchAttributeString(void* context, char* out, int outSize)
{
    XmlRequest* request = static_cast<XmlRequest*>(context);
    return AndroidfwXmlAttributeString(request->xml, request->index, out, outSize);
}

static jstring XmlElementName(JNIEnv* env, jclass, jlong handle)
{
    XmlRequest request{XmlFrom(handle), 0};
    return Grown(env, &FetchElementName, &request);
}

static jstring XmlAttributeName(JNIEnv* env, jclass, jlong handle, jint index)
{
    XmlRequest request{XmlFrom(handle), index};
    return Grown(env, &FetchAttributeName, &request);
}

static jstring XmlAttributeStringValue(JNIEnv* env, jclass, jlong handle, jint index)
{
    XmlRequest request{XmlFrom(handle), index};
    return Grown(env, &FetchAttributeString, &request);
}

static const JNINativeMethod XML_METHODS[] = {
    {"nativeNext", "(J)I", reinterpret_cast<void*>(&XmlNext)},
    {"nativeDepth", "(J)I", reinterpret_cast<void*>(&XmlDepth)},
    {"nativeClose", "(J)V", reinterpret_cast<void*>(&XmlClose)},
    {"nativeElementName", "(J)Ljava/lang/String;", reinterpret_cast<void*>(&XmlElementName)},
    {"nativeLineNumber", "(J)I", reinterpret_cast<void*>(&XmlLineNumber)},
    {"nativeAttributeCount", "(J)I", reinterpret_cast<void*>(&XmlAttributeCount)},
    {"nativeAttributeName", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&XmlAttributeName)},
    {"nativeAttributeStringValue", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&XmlAttributeStringValue)},
    {"nativeAttributeNameResource", "(JI)I", reinterpret_cast<void*>(&XmlAttributeNameResource)},
    {"nativeAttributeValue", "(JI)[I", reinterpret_cast<void*>(&XmlAttributeValue)},
    {"nativeIndexOfName", "(JLjava/lang/String;Ljava/lang/String;)I", reinterpret_cast<void*>(&XmlIndexOfName)},
    {"nativeIndexOfResource", "(JI)I", reinterpret_cast<void*>(&XmlIndexOfResource)},
    {"nativePooledString", "(JI)Ljava/lang/String;", reinterpret_cast<void*>(&XmlPooledString)},
};

static const JNINativeMethod DESCRIPTOR_METHODS[] = {
    {"nativeClose", "(Ljava/io/FileDescriptor;I)Z", reinterpret_cast<void*>(&DescriptorClose)},
    {"nativeInvalidate", "(Ljava/io/FileDescriptor;)V", reinterpret_cast<void*>(&DescriptorInvalidate)},
};

static const ArtRuntime::NativeMethodTable TABLES[] = {
    {ResourceBridge::RESOURCES_CLASS, RESOURCE_METHODS, static_cast<int>(sizeof(RESOURCE_METHODS) / sizeof(RESOURCE_METHODS[0]))},
    {ResourceBridge::XML_PARSER_CLASS, XML_METHODS, static_cast<int>(sizeof(XML_METHODS) / sizeof(XML_METHODS[0]))},
    {ResourceBridge::DESCRIPTOR_CLASS, DESCRIPTOR_METHODS, static_cast<int>(sizeof(DESCRIPTOR_METHODS) / sizeof(DESCRIPTOR_METHODS[0]))},
};

const ArtRuntime::NativeMethodTable* ResourceBridge::Tables(int& count)
{
    count = static_cast<int>(sizeof(TABLES) / sizeof(TABLES[0]));
    return TABLES;
}

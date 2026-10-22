#include "FrameworkJni/FrameworkJni.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sys/syscall.h>
#include <unistd.h>
#include <vector>

#include "ApkSignature/ApkSignature.hpp"
#include "Diagnostics/Diagnostics.hpp"
#include "FontStack/FontStack.hpp"
#include "FrameworkJni/ResourceBridge.hpp"
#include "FrameworkJni/GraphicsBridge.hpp"
#include "FrameworkJni/SqliteBridge.hpp"
#include "FrameworkJni/TextBridge.hpp"
#include "FrameworkJni/WindowBridge.hpp"
#include "Settings/Settings.hpp"

static const Apk::Package* gPackage = nullptr;
static const std::vector<PackageRegistry::Entry>* gInstalledPackages = nullptr;

static std::string CopyJavaString(JNIEnv* env, jstring value)
{
    if (!value) return std::string();

    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return std::string();

    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

static jint LogPrintlnNative(JNIEnv* env, jclass, jint, jint priority, jstring tag, jstring message)
{
    std::string tagText = CopyJavaString(env, tag);
    std::string messageText = CopyJavaString(env, message);

    fprintf(stderr, "%c/%s: %s\n", FrameworkJni::PriorityLetter(priority), tagText.c_str(), messageText.c_str());
    fflush(stderr);

    return static_cast<jint>(messageText.size());
}

static jstring MakeString(JNIEnv* env, const std::string& value)
{
    return env->NewStringUTF(value.c_str());
}

static const Apk::ProviderInfo* ProviderAt(jint index)
{
    if (!gPackage) return nullptr;
    if (index < 0 || static_cast<size_t>(index) >= gPackage->manifest.providers.size()) return nullptr;
    return &gPackage->manifest.providers[static_cast<size_t>(index)];
}

static constexpr jint COMPONENT_ACTIVITY = 0;
static constexpr jint COMPONENT_SERVICE = 1;
static constexpr jint COMPONENT_RECEIVER = 2;
static constexpr jint COMPONENT_PROVIDER = 3;
static constexpr jint COMPONENT_APPLICATION = 4;

static const std::vector<Apk::IntentFilterInfo>* FiltersFor(jint kind, jint index)
{
    if (!gPackage) return nullptr;

    size_t at = static_cast<size_t>(index);
    if (index < 0) return nullptr;

    if (kind == COMPONENT_ACTIVITY && at < gPackage->manifest.activities.size())
        return &gPackage->manifest.activities[at].filters;
    if (kind == COMPONENT_SERVICE && at < gPackage->manifest.services.size())
        return &gPackage->manifest.services[at].filters;
    if (kind == COMPONENT_RECEIVER && at < gPackage->manifest.receivers.size())
        return &gPackage->manifest.receivers[at].filters;

    return nullptr;
}

static const Apk::IntentFilterInfo* FilterAt(jint kind, jint index, jint filterIndex)
{
    const std::vector<Apk::IntentFilterInfo>* filters = FiltersFor(kind, index);
    if (!filters) return nullptr;
    if (filterIndex < 0 || static_cast<size_t>(filterIndex) >= filters->size()) return nullptr;

    return &(*filters)[static_cast<size_t>(filterIndex)];
}

static jstring JoinedLines(JNIEnv* env, const std::vector<std::string>& values)
{
    std::string joined;
    for (const std::string& value : values)
    {
        if (!joined.empty()) joined += '\n';
        joined += value;
    }

    return MakeString(env, joined);
}

static jint BootstrapComponentCount(JNIEnv*, jclass, jint kind)
{
    if (!gPackage) return 0;

    if (kind == COMPONENT_ACTIVITY) return static_cast<jint>(gPackage->manifest.activities.size());
    if (kind == COMPONENT_SERVICE) return static_cast<jint>(gPackage->manifest.services.size());
    if (kind == COMPONENT_RECEIVER) return static_cast<jint>(gPackage->manifest.receivers.size());

    return 0;
}

static jint BootstrapPermissionCount(JNIEnv*, jclass)
{
    if (!gPackage) return 0;

    return static_cast<jint>(gPackage->manifest.permissions.size());
}

static jstring BootstrapPermissionAt(JNIEnv* env, jclass, jint index)
{
    if (!gPackage || index < 0) return nullptr;

    const size_t at = static_cast<size_t>(index);
    if (at >= gPackage->manifest.permissions.size()) return nullptr;

    return MakeString(env, gPackage->manifest.permissions[at]);
}

static jint BootstrapActivityLaunchMode(JNIEnv* env, jclass, jstring className)
{
    if (!gPackage) return 0;

    const std::string wanted = CopyJavaString(env, className);

    for (const Apk::ActivityInfo& activity : gPackage->manifest.activities)
    {
        if (activity.className != wanted && activity.targetActivity != wanted) continue;

        return static_cast<jint>(activity.launchMode);
    }

    return 0;
}

static jint BootstrapActivityConfigChanges(JNIEnv* env, jclass, jstring className)
{
    if (!gPackage) return 0;

    const std::string wanted = CopyJavaString(env, className);

    for (const Apk::ActivityInfo& activity : gPackage->manifest.activities)
    {
        if (activity.className != wanted && activity.targetActivity != wanted) continue;

        return static_cast<jint>(activity.configChanges);
    }

    return 0;
}

static jlong BootstrapVersionCode(JNIEnv*, jclass)
{
    if (!gPackage) return 0;

    return static_cast<jlong>(gPackage->manifest.versionCode);
}

static jstring BootstrapVersionName(JNIEnv* env, jclass)
{
    if (!gPackage) return nullptr;

    return MakeString(env, gPackage->manifest.versionName);
}

static jstring BootstrapComponentClassName(JNIEnv* env, jclass, jint kind, jint index)
{
    if (!gPackage || index < 0) return nullptr;

    size_t at = static_cast<size_t>(index);

    if (kind == COMPONENT_ACTIVITY && at < gPackage->manifest.activities.size())
    {
        const Apk::ActivityInfo& activity = gPackage->manifest.activities[at];
        return MakeString(env, activity.targetActivity.empty() ? activity.className : activity.targetActivity);
    }

    if (kind == COMPONENT_SERVICE && at < gPackage->manifest.services.size())
        return MakeString(env, gPackage->manifest.services[at].className);

    if (kind == COMPONENT_RECEIVER && at < gPackage->manifest.receivers.size())
        return MakeString(env, gPackage->manifest.receivers[at].className);

    return nullptr;
}

static const std::vector<Apk::MetaDataEntry>* ComponentMetaDataAt(jint kind, jint index)
{
    if (!gPackage || index < 0) return nullptr;

    size_t at = static_cast<size_t>(index);

    if (kind == COMPONENT_ACTIVITY && at < gPackage->manifest.activities.size())
        return &gPackage->manifest.activities[at].metaData;

    if (kind == COMPONENT_SERVICE && at < gPackage->manifest.services.size())
        return &gPackage->manifest.services[at].metaData;

    if (kind == COMPONENT_RECEIVER && at < gPackage->manifest.receivers.size())
        return &gPackage->manifest.receivers[at].metaData;

    if (kind == COMPONENT_PROVIDER && at < gPackage->manifest.providers.size())
        return &gPackage->manifest.providers[at].metaData;

    if (kind == COMPONENT_APPLICATION && at == 0) return &gPackage->manifest.applicationMetaData;

    return nullptr;
}

static const Apk::MetaDataEntry* ComponentMetaDataEntry(jint kind, jint index, jint entry)
{
    const std::vector<Apk::MetaDataEntry>* entries = ComponentMetaDataAt(kind, index);
    if (!entries || entry < 0 || static_cast<size_t>(entry) >= entries->size()) return nullptr;

    return &(*entries)[static_cast<size_t>(entry)];
}

static jint BootstrapComponentMetaDataType(JNIEnv*, jclass, jint kind, jint index, jint entry)
{
    const Apk::MetaDataEntry* found = ComponentMetaDataEntry(kind, index, entry);
    if (!found) return Apk::META_DATA_NO_VALUE;
    if (found->resource != 0) return Apk::META_DATA_RESOURCE;

    return found->valueType;
}

static jint BootstrapComponentMetaDataData(JNIEnv*, jclass, jint kind, jint index, jint entry)
{
    const Apk::MetaDataEntry* found = ComponentMetaDataEntry(kind, index, entry);
    if (!found) return 0;

    return static_cast<jint>(found->resource != 0 ? found->resource : found->valueData);
}

static jint BootstrapComponentMetaDataCount(JNIEnv*, jclass, jint kind, jint index)
{
    const std::vector<Apk::MetaDataEntry>* entries = ComponentMetaDataAt(kind, index);
    return entries ? static_cast<jint>(entries->size()) : 0;
}

static jstring BootstrapComponentMetaDataName(JNIEnv* env, jclass, jint kind, jint index, jint entry)
{
    const std::vector<Apk::MetaDataEntry>* entries = ComponentMetaDataAt(kind, index);
    if (!entries || entry < 0 || static_cast<size_t>(entry) >= entries->size()) return nullptr;

    return MakeString(env, (*entries)[static_cast<size_t>(entry)].name);
}

static jstring BootstrapComponentMetaDataValue(JNIEnv* env, jclass, jint kind, jint index, jint entry)
{
    const std::vector<Apk::MetaDataEntry>* entries = ComponentMetaDataAt(kind, index);
    if (!entries || entry < 0 || static_cast<size_t>(entry) >= entries->size()) return nullptr;

    return MakeString(env, (*entries)[static_cast<size_t>(entry)].value);
}

static jint BootstrapComponentFilterCount(JNIEnv*, jclass, jint kind, jint index)
{
    const std::vector<Apk::IntentFilterInfo>* filters = FiltersFor(kind, index);
    return filters ? static_cast<jint>(filters->size()) : 0;
}

static jstring BootstrapFilterActions(JNIEnv* env, jclass, jint kind, jint index, jint filterIndex)
{
    const Apk::IntentFilterInfo* filter = FilterAt(kind, index, filterIndex);
    return filter ? JoinedLines(env, filter->actions) : nullptr;
}

static jstring BootstrapFilterCategories(JNIEnv* env, jclass, jint kind, jint index, jint filterIndex)
{
    const Apk::IntentFilterInfo* filter = FilterAt(kind, index, filterIndex);
    return filter ? JoinedLines(env, filter->categories) : nullptr;
}

static jstring BootstrapFilterSchemes(JNIEnv* env, jclass, jint kind, jint index, jint filterIndex)
{
    const Apk::IntentFilterInfo* filter = FilterAt(kind, index, filterIndex);
    return filter ? JoinedLines(env, filter->schemes) : nullptr;
}

static jstring BootstrapFilterTypes(JNIEnv* env, jclass, jint kind, jint index, jint filterIndex)
{
    const Apk::IntentFilterInfo* filter = FilterAt(kind, index, filterIndex);
    return filter ? JoinedLines(env, filter->mimeTypes) : nullptr;
}

static jint BootstrapFilterPriority(JNIEnv*, jclass, jint kind, jint index, jint filterIndex)
{
    const Apk::IntentFilterInfo* filter = FilterAt(kind, index, filterIndex);
    return filter ? static_cast<jint>(filter->priority) : 0;
}

static jstring BootstrapPackageName(JNIEnv* env, jclass)
{
    if (!gPackage) return nullptr;
    return MakeString(env, gPackage->manifest.packageName);
}

static jstring BootstrapApplicationClassName(JNIEnv* env, jclass)
{
    if (!gPackage) return nullptr;
    return MakeString(env, gPackage->manifest.applicationClassName);
}

static jstring BootstrapAppComponentFactory(JNIEnv* env, jclass)
{
    if (!gPackage) return nullptr;
    return MakeString(env, gPackage->manifest.appComponentFactory);
}

static jstring BootstrapLauncherActivity(JNIEnv* env, jclass)
{
    if (!gPackage) return nullptr;

    std::string className;
    if (Apk::LauncherActivity(*gPackage, className) != Apk::Result::Ok) return nullptr;

    return MakeString(env, className);
}

static jint BootstrapActivityTheme(JNIEnv* env, jclass, jstring className)
{
    if (!gPackage) return 0;

    std::string name = CopyJavaString(env, className);
    return static_cast<jint>(Apk::ThemeFor(*gPackage, name));
}

static std::string gArchivePath;

void FrameworkJni::SetArchivePath(const std::string& path)
{
    gArchivePath = path;
}

static ApkSignature::Info gSigningInfo;

void FrameworkJni::SetSigningInfo(const ApkSignature::Info& info)
{
    gSigningInfo = info;
}

static jint BootstrapSignatureCount(JNIEnv*, jclass)
{
    return static_cast<jint>(gSigningInfo.signers.size());
}

static jbyteArray BootstrapSignatureAt(JNIEnv* env, jclass, jint index)
{
    const ApkSignature::Info& info = gSigningInfo;
    if (index < 0 || static_cast<size_t>(index) >= info.signers.size()) return nullptr;

    const std::vector<uint8_t>& certificate = info.signers[static_cast<size_t>(index)].certificate;
    jbyteArray array = env->NewByteArray(static_cast<jsize>(certificate.size()));
    if (array == nullptr) return nullptr;

    env->SetByteArrayRegion(array, 0, static_cast<jsize>(certificate.size()), reinterpret_cast<const jbyte*>(certificate.data()));

    return array;
}

static jint BootstrapSignatureScheme(JNIEnv*, jclass)
{
    return static_cast<jint>(gSigningInfo.scheme);
}

static jboolean BootstrapSignatureVerified(JNIEnv*, jclass)
{
    return gSigningInfo.verified ? JNI_TRUE : JNI_FALSE;
}

static jint BootstrapSplitCount(JNIEnv*, jclass)
{
    if (!gPackage) return 0;

    return static_cast<jint>(gPackage->splitNames.size());
}

static jstring BootstrapSplitName(JNIEnv* env, jclass, jint index)
{
    if (!gPackage) return nullptr;
    if (index < 0 || static_cast<size_t>(index) >= gPackage->splitNames.size()) return nullptr;

    return MakeString(env, gPackage->splitNames[static_cast<size_t>(index)]);
}

static jstring BootstrapSplitPath(JNIEnv* env, jclass, jint index)
{
    if (!gPackage) return nullptr;
    if (index < 0 || static_cast<size_t>(index) >= gPackage->splitPaths.size()) return nullptr;

    return MakeString(env, gPackage->splitPaths[static_cast<size_t>(index)]);
}

static jstring BootstrapSourceDir(JNIEnv* env, jclass)
{
    if (!gArchivePath.empty()) return MakeString(env, gArchivePath);
    if (!gPackage) return nullptr;

    return MakeString(env, gPackage->path);
}

static std::string gExternalStorageDirectory;

void FrameworkJni::SetExternalStorageDirectory(const std::string& path)
{
    gExternalStorageDirectory = path;
}

static jstring BootstrapExternalStorageDir(JNIEnv* env, jclass)
{
    if (gExternalStorageDirectory.empty()) return nullptr;
    return MakeString(env, gExternalStorageDirectory);
}

static jint BootstrapMinSdkVersion(JNIEnv*, jclass)
{
    return gPackage ? static_cast<jint>(gPackage->manifest.minSdkVersion) : 0;
}

static jint BootstrapTargetSdkVersion(JNIEnv*, jclass)
{
    return gPackage ? static_cast<jint>(gPackage->manifest.targetSdkVersion) : 0;
}

static jboolean BootstrapDebuggable(JNIEnv*, jclass)
{
    return gPackage && gPackage->manifest.debuggable ? JNI_TRUE : JNI_FALSE;
}

static jboolean BootstrapExtractNativeLibs(JNIEnv*, jclass)
{
    return gPackage && gPackage->manifest.extractNativeLibs ? JNI_TRUE : JNI_FALSE;
}

static jlong BootstrapAssetsHandle(JNIEnv*, jclass)
{
    return gPackage ? reinterpret_cast<jlong>(gPackage->assetsHandle) : 0;
}

static jint BootstrapProviderCount(JNIEnv*, jclass)
{
    return gPackage ? static_cast<jint>(gPackage->manifest.providers.size()) : 0;
}

static jstring BootstrapProviderClassName(JNIEnv* env, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    if (!provider) return nullptr;
    return MakeString(env, provider->className);
}

static jstring BootstrapProviderAuthority(JNIEnv* env, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    if (!provider) return nullptr;
    return MakeString(env, provider->authorities);
}

static jboolean BootstrapProviderDirectBootAware(JNIEnv*, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    return provider && provider->directBootAware ? JNI_TRUE : JNI_FALSE;
}

static jboolean BootstrapProviderGrantUriPermissions(JNIEnv*, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    return provider && provider->grantUriPermissions ? JNI_TRUE : JNI_FALSE;
}

static jboolean BootstrapProviderEnabled(JNIEnv*, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    return provider && provider->enabled ? JNI_TRUE : JNI_FALSE;
}

static jstring BootstrapProviderProcess(JNIEnv* env, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    if (!provider || provider->process.empty()) return nullptr;
    return MakeString(env, provider->process);
}

static jint BootstrapProviderMetaDataCount(JNIEnv*, jclass, jint index)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    return provider ? static_cast<jint>(provider->metaData.size()) : 0;
}

static const Apk::MetaDataEntry* MetaDataAt(jint index, jint entry)
{
    const Apk::ProviderInfo* provider = ProviderAt(index);
    if (!provider) return nullptr;
    if (entry < 0 || static_cast<size_t>(entry) >= provider->metaData.size()) return nullptr;
    return &provider->metaData[static_cast<size_t>(entry)];
}

static jstring BootstrapProviderMetaDataName(JNIEnv* env, jclass, jint index, jint entry)
{
    const Apk::MetaDataEntry* entryData = MetaDataAt(index, entry);
    if (!entryData) return nullptr;
    return MakeString(env, entryData->name);
}

static jstring BootstrapProviderMetaDataValue(JNIEnv* env, jclass, jint index, jint entry)
{
    const Apk::MetaDataEntry* entryData = MetaDataAt(index, entry);
    if (!entryData) return nullptr;
    return MakeString(env, entryData->value);
}

static const Apk::ActivityInfo* ActivityNamed(JNIEnv* env, jstring className)
{
    if (!gPackage) return nullptr;

    std::string name = CopyJavaString(env, className);
    for (const Apk::ActivityInfo& activity : gPackage->manifest.activities)
    {
        if (activity.className == name) return &activity;
    }
    return nullptr;
}

static jint BootstrapActivityMetaDataCount(JNIEnv* env, jclass, jstring className)
{
    const Apk::ActivityInfo* activity = ActivityNamed(env, className);
    return activity ? static_cast<jint>(activity->metaData.size()) : 0;
}

static const Apk::MetaDataEntry* ActivityMetaDataAt(JNIEnv* env, jstring className, jint entry)
{
    const Apk::ActivityInfo* activity = ActivityNamed(env, className);
    if (!activity) return nullptr;
    if (entry < 0 || static_cast<size_t>(entry) >= activity->metaData.size()) return nullptr;
    return &activity->metaData[static_cast<size_t>(entry)];
}

static jstring BootstrapActivityMetaDataName(JNIEnv* env, jclass, jstring className, jint entry)
{
    const Apk::MetaDataEntry* entryData = ActivityMetaDataAt(env, className, entry);
    if (!entryData) return nullptr;
    return MakeString(env, entryData->name);
}

static jstring BootstrapActivityMetaDataValue(JNIEnv* env, jclass, jstring className, jint entry)
{
    const Apk::MetaDataEntry* entryData = ActivityMetaDataAt(env, className, entry);
    if (!entryData) return nullptr;
    return MakeString(env, entryData->value);
}

static const PackageRegistry::Entry* InstalledPackageAt(jint slot)
{
    if (!gInstalledPackages) return nullptr;
    if (slot < 0 || (size_t)slot >= gInstalledPackages->size()) return nullptr;

    return &(*gInstalledPackages)[(size_t)slot];
}

static jint PackagesCount(JNIEnv*, jclass)
{
    return gInstalledPackages ? (jint)gInstalledPackages->size() : 0;
}

static jstring PackagesNameAt(JNIEnv* env, jclass, jint slot)
{
    const PackageRegistry::Entry* entry = InstalledPackageAt(slot);
    return entry ? MakeString(env, entry->packageName) : nullptr;
}

static jstring PackagesLabelAt(JNIEnv* env, jclass, jint slot)
{
    const PackageRegistry::Entry* entry = InstalledPackageAt(slot);
    return entry ? MakeString(env, entry->label) : nullptr;
}

static jstring PackagesArchiveAt(JNIEnv* env, jclass, jint slot)
{
    const PackageRegistry::Entry* entry = InstalledPackageAt(slot);
    return entry ? MakeString(env, entry->apkPath) : nullptr;
}

static jstring PackagesIconAt(JNIEnv* env, jclass, jint slot)
{
    const PackageRegistry::Entry* entry = InstalledPackageAt(slot);
    return entry ? MakeString(env, entry->iconPath) : nullptr;
}

static jint PackagesTargetSdkAt(JNIEnv*, jclass, jint slot)
{
    const PackageRegistry::Entry* entry = InstalledPackageAt(slot);
    return entry ? (jint)entry->targetSdkVersion : 0;
}

jboolean PackagesLaunch(JNIEnv* env, jclass, jstring packageName);

static const JNINativeMethod PACKAGES_METHODS[] = {
    {"nativeLaunch", "(Ljava/lang/String;)Z", reinterpret_cast<void*>(&PackagesLaunch)},
    {"nativeCount", "()I", reinterpret_cast<void*>(&PackagesCount)},
    {"nativeNameAt", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&PackagesNameAt)},
    {"nativeLabelAt", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&PackagesLabelAt)},
    {"nativeArchiveAt", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&PackagesArchiveAt)},
    {"nativeIconAt", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&PackagesIconAt)},
    {"nativeTargetSdkAt", "(I)I", reinterpret_cast<void*>(&PackagesTargetSdkAt)},
};

void NotificationsPost(JNIEnv* env, jclass, jstring tag, jint id, jstring channelId, jstring title, jstring text,
                       jlong whenMillis, jint priority, jint importance, jint flags);
void NotificationsCancel(JNIEnv* env, jclass, jstring tag, jint id);
void NotificationsCancelAll(JNIEnv* env, jclass);

static const JNINativeMethod NOTIFICATION_STORE_METHODS[] = {
    {"nativePost", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;JIII)V",
     reinterpret_cast<void*>(&NotificationsPost)},
    {"nativeCancel", "(Ljava/lang/String;I)V", reinterpret_cast<void*>(&NotificationsCancel)},
    {"nativeCancelAll", "()V", reinterpret_cast<void*>(&NotificationsCancelAll)},
};

static const JNINativeMethod BOOTSTRAP_METHODS[] = {
    {"nativePackageName", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapPackageName)},
    {"nativeApplicationClassName", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapApplicationClassName)},
    {"nativeAppComponentFactory", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapAppComponentFactory)},
    {"nativeSignatureCount", "()I", reinterpret_cast<void*>(&BootstrapSignatureCount)},
    {"nativeSignatureAt", "(I)[B", reinterpret_cast<void*>(&BootstrapSignatureAt)},
    {"nativeSignatureScheme", "()I", reinterpret_cast<void*>(&BootstrapSignatureScheme)},
    {"nativeSignatureVerified", "()Z", reinterpret_cast<void*>(&BootstrapSignatureVerified)},
    {"nativeSplitCount", "()I", reinterpret_cast<void*>(&BootstrapSplitCount)},
    {"nativeSplitName", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapSplitName)},
    {"nativeSplitPath", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapSplitPath)},
    {"nativeSourceDir", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapSourceDir)},
    {"nativeExternalStorageDir", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapExternalStorageDir)},
    {"nativeLauncherActivity", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapLauncherActivity)},
    {"nativeActivityTheme", "(Ljava/lang/String;)I", reinterpret_cast<void*>(&BootstrapActivityTheme)},
    {"nativeMinSdkVersion", "()I", reinterpret_cast<void*>(&BootstrapMinSdkVersion)},
    {"nativeTargetSdkVersion", "()I", reinterpret_cast<void*>(&BootstrapTargetSdkVersion)},
    {"nativeDebuggable", "()Z", reinterpret_cast<void*>(&BootstrapDebuggable)},
    {"nativeExtractNativeLibs", "()Z", reinterpret_cast<void*>(&BootstrapExtractNativeLibs)},
    {"nativeAssetsHandle", "()J", reinterpret_cast<void*>(&BootstrapAssetsHandle)},
    {"nativeProviderCount", "()I", reinterpret_cast<void*>(&BootstrapProviderCount)},
    {"nativeProviderClassName", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapProviderClassName)},
    {"nativeProviderAuthority", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapProviderAuthority)},
    {"nativeProviderDirectBootAware", "(I)Z", reinterpret_cast<void*>(&BootstrapProviderDirectBootAware)},
    {"nativeProviderGrantUriPermissions", "(I)Z", reinterpret_cast<void*>(&BootstrapProviderGrantUriPermissions)},
    {"nativeProviderEnabled", "(I)Z", reinterpret_cast<void*>(&BootstrapProviderEnabled)},
    {"nativeProviderProcess", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapProviderProcess)},
    {"nativeProviderMetaDataCount", "(I)I", reinterpret_cast<void*>(&BootstrapProviderMetaDataCount)},
    {"nativeProviderMetaDataName", "(II)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapProviderMetaDataName)},
    {"nativeProviderMetaDataValue", "(II)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapProviderMetaDataValue)},
    {"nativeComponentCount", "(I)I", reinterpret_cast<void*>(&BootstrapComponentCount)},
    {"nativePermissionCount", "()I", reinterpret_cast<void*>(&BootstrapPermissionCount)},
    {"nativePermissionAt", "(I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapPermissionAt)},
    {"nativeActivityLaunchMode", "(Ljava/lang/String;)I", reinterpret_cast<void*>(&BootstrapActivityLaunchMode)},
    {"nativeActivityConfigChanges", "(Ljava/lang/String;)I", reinterpret_cast<void*>(&BootstrapActivityConfigChanges)},
    {"nativeVersionCode", "()J", reinterpret_cast<void*>(&BootstrapVersionCode)},
    {"nativeVersionName", "()Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapVersionName)},
    {"nativeComponentClassName", "(II)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapComponentClassName)},
    {"nativeComponentFilterCount", "(II)I", reinterpret_cast<void*>(&BootstrapComponentFilterCount)},
    {"nativeComponentMetaDataCount", "(II)I", reinterpret_cast<void*>(&BootstrapComponentMetaDataCount)},
    {"nativeComponentMetaDataName", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapComponentMetaDataName)},
    {"nativeComponentMetaDataValue", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapComponentMetaDataValue)},
    {"nativeComponentMetaDataType", "(III)I", reinterpret_cast<void*>(&BootstrapComponentMetaDataType)},
    {"nativeComponentMetaDataData", "(III)I", reinterpret_cast<void*>(&BootstrapComponentMetaDataData)},
    {"nativeFilterActions", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapFilterActions)},
    {"nativeFilterCategories", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapFilterCategories)},
    {"nativeFilterSchemes", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapFilterSchemes)},
    {"nativeFilterTypes", "(III)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapFilterTypes)},
    {"nativeFilterPriority", "(III)I", reinterpret_cast<void*>(&BootstrapFilterPriority)},
    {"nativeActivityMetaDataCount", "(Ljava/lang/String;)I", reinterpret_cast<void*>(&BootstrapActivityMetaDataCount)},
    {"nativeActivityMetaDataName", "(Ljava/lang/String;I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapActivityMetaDataName)},
    {"nativeActivityMetaDataValue", "(Ljava/lang/String;I)Ljava/lang/String;", reinterpret_cast<void*>(&BootstrapActivityMetaDataValue)},
};

static WindowBridge::Session gWindowSession;

static constexpr jlong CANVAS_TOKEN = 1;

static Rasterizer::Canvas* CanvasFrom(jlong handle)
{
    if (handle == 0) return nullptr;

    if (handle == CANVAS_TOKEN) return gWindowSession.open ? &gWindowSession.canvas : nullptr;

    return reinterpret_cast<Rasterizer::Canvas*>(static_cast<intptr_t>(handle));
}

Rasterizer::Canvas* FrameworkJniCanvasFrom(jlong handle)
{
    return CanvasFrom(handle);
}

static jint CanvasSave(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (!canvas) return 0;

    int before = canvas->depth;
    if (Rasterizer::Save(*canvas) != Rasterizer::Result::Ok) return -1;

    return before;
}

static jboolean CanvasRestore(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (!canvas) return JNI_FALSE;

    return Rasterizer::Restore(*canvas) == Rasterizer::Result::Ok ? JNI_TRUE : JNI_FALSE;
}

static jint CanvasGetSaveCount(JNIEnv*, jclass, jlong handle)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    return canvas ? canvas->depth : 0;
}

static void CanvasTranslate(JNIEnv*, jclass, jlong handle, jint deltaX, jint deltaY)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (canvas) Rasterizer::Translate(*canvas, deltaX, deltaY);
}

static jboolean CanvasClipRect(JNIEnv*, jclass, jlong handle, jint left, jint top, jint right, jint bottom)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (!canvas) return JNI_FALSE;

    Rasterizer::ClipRect(*canvas, Rasterizer::Rect{left, top, right, bottom});
    return Rasterizer::IsEmpty(Rasterizer::CurrentState(*canvas).clip) ? JNI_FALSE : JNI_TRUE;
}

jboolean PackagesLaunch(JNIEnv* env, jclass, jstring packageName)
{
    const std::string name = CopyJavaString(env, packageName);
    if (name.empty()) return JNI_FALSE;

    if (!gWindowSession.open || !gWindowSession.isHosted)
    {
        fprintf(stderr, "[framework] %s cannot be started: only a player the shell is hosting can ask for a launch\n", name.c_str());
        return JNI_FALSE;
    }

    return HostedWindow::RequestLaunch(gWindowSession.hosted, name.c_str()) ? JNI_TRUE : JNI_FALSE;
}

void NotificationsPost(JNIEnv*, jclass, jstring, jint, jstring, jstring, jstring, jlong, jint, jint, jint)
{
}

void NotificationsCancel(JNIEnv*, jclass, jstring, jint)
{
}

void NotificationsCancelAll(JNIEnv*, jclass)
{
}

static void CanvasDrawColor(JNIEnv*, jclass, jlong handle, jint color)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (canvas) Rasterizer::DrawColor(*canvas, static_cast<uint32_t>(color));
}

static void CanvasDrawRect(JNIEnv*, jclass, jlong handle, jint left, jint top, jint right, jint bottom, jint color)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (canvas) Rasterizer::DrawRect(*canvas, Rasterizer::Rect{left, top, right, bottom}, static_cast<uint32_t>(color));
}

static void CanvasDrawLine(JNIEnv*, jclass, jlong handle, jint startX, jint startY, jint stopX, jint stopY, jint color)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (!canvas) return;

    if (startY == stopY)
    {
        int32_t left = startX < stopX ? startX : stopX;
        int32_t length = startX < stopX ? stopX - startX : startX - stopX;
        if (length > 0) Rasterizer::DrawHorizontalLine(*canvas, left, startY, length, static_cast<uint32_t>(color));
        return;
    }

    if (startX == stopX)
    {
        int32_t top = startY < stopY ? startY : stopY;
        int32_t length = startY < stopY ? stopY - startY : startY - stopY;
        if (length > 0) Rasterizer::DrawVerticalLine(*canvas, startX, top, length, static_cast<uint32_t>(color));
    }
}

static void CanvasDrawTextRun(JNIEnv* env, jclass, jlong handle, jint style, jcharArray text, jint index, jint count, jfloat x, jfloat y,
                              jint color, jboolean rightToLeft)
{
    Rasterizer::Canvas* canvas = CanvasFrom(handle);
    if (!canvas || !text || count <= 0 || index < 0) return;

    jsize length = env->GetArrayLength(text);
    if (index + count > length) return;

    std::vector<uint16_t> chars(static_cast<size_t>(count));
    env->GetCharArrayRegion(text, index, count, reinterpret_cast<jchar*>(chars.data()));

    uint64_t run = FontStack::INVALID_RUN;
    if (FontStack::ShapeRun(static_cast<uint32_t>(style), chars.data(), count, rightToLeft == JNI_TRUE, run) != FontStack::Result::Ok)
    {
        return;
    }

    int32_t glyphCount = 0;
    FontStack::RunGlyphCount(run, glyphCount);

    int32_t originX = static_cast<int32_t>(x);
    int32_t baseline = static_cast<int32_t>(y);

    for (int32_t glyph = 0; glyph < glyphCount; ++glyph)
    {
        FontStack::GlyphPosition position;
        if (FontStack::RunGlyph(run, glyph, position) != FontStack::Result::Ok) break;

        FontStack::GlyphImage image;
        if (FontStack::RasterizeGlyph(static_cast<uint32_t>(style), position.glyphId, image) != FontStack::Result::Ok) continue;
        if (!image.samples || image.width <= 0 || image.height <= 0) continue;

        Rasterizer::Coverage mask;
        mask.samples = image.samples;
        mask.width = image.width;
        mask.height = image.height;
        mask.pitch = image.pitch;

        Rasterizer::DrawCoverage(*canvas, mask, originX + static_cast<int32_t>(position.x) + image.bearingX,
                                 baseline - image.bearingY, static_cast<uint32_t>(color));
    }
}

static const JNINativeMethod CANVAS_METHODS[] = {
    {"nativeSave", "(J)I", reinterpret_cast<void*>(&CanvasSave)},
    {"nativeRestore", "(J)Z", reinterpret_cast<void*>(&CanvasRestore)},
    {"nativeGetSaveCount", "(J)I", reinterpret_cast<void*>(&CanvasGetSaveCount)},
    {"nativeTranslate", "(JII)V", reinterpret_cast<void*>(&CanvasTranslate)},
    {"nativeClipRect", "(JIIII)Z", reinterpret_cast<void*>(&CanvasClipRect)},
    {"nativeDrawColor", "(JI)V", reinterpret_cast<void*>(&CanvasDrawColor)},
    {"nativeDrawRect", "(JIIIII)V", reinterpret_cast<void*>(&CanvasDrawRect)},
    {"nativeDrawLine", "(JIIIII)V", reinterpret_cast<void*>(&CanvasDrawLine)},
    {"nativeDrawTextRun", "(JI[CIIFFIZ)V", reinterpret_cast<void*>(&CanvasDrawTextRun)},
};

static jlong WindowOpen(JNIEnv* env, jclass, jint width, jint height, jstring title)
{
    if (gWindowSession.open) return reinterpret_cast<jlong>(&gWindowSession);

    std::string titleText = CopyJavaString(env, title);
    WindowBridge::Result result = WindowBridge::Open(static_cast<uint32_t>(width), static_cast<uint32_t>(height), titleText.c_str(), gWindowSession);

    if (result != WindowBridge::Result::Ok)
    {
        fprintf(stderr, "[window] cannot open: %s\n", WindowBridge::ResultName(result));
        return 0;
    }

    return reinterpret_cast<jlong>(&gWindowSession);
}

static WindowBridge::Session* SessionFrom(jlong handle)
{
    WindowBridge::Session* session = reinterpret_cast<WindowBridge::Session*>(handle);
    return session == &gWindowSession ? session : nullptr;
}

static void WindowClose(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (session) WindowBridge::Close(*session);
}

static jlong WindowBeginFrame(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session) return 0;

    if (WindowBridge::BeginFrame(*session) != WindowBridge::Result::Ok) return 0;
    return CANVAS_TOKEN;
}

static void WindowEndFrame(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (session) WindowBridge::EndFrame(*session);
}

static void WindowRequestFrame(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (session) WindowBridge::RequestFrame(*session);
}

static jboolean WindowPoll(JNIEnv*, jclass, jlong handle, jint timeoutMillis)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session) return JNI_FALSE;

    return WindowBridge::Poll(*session, timeoutMillis) == WindowBridge::Result::Ok ? JNI_TRUE : JNI_FALSE;
}

static void WindowWake(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (session) WindowBridge::Wake(*session);
}

static constexpr jsize INPUT_EVENT_WORDS = 8;

static jboolean WindowConsumeFrame(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    return session && WindowBridge::ConsumeFrame(*session) ? JNI_TRUE : JNI_FALSE;
}

static jboolean WindowNextInputEvent(JNIEnv* env, jclass, jlong handle, jlongArray output)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session || !output || env->GetArrayLength(output) < INPUT_EVENT_WORDS) return JNI_FALSE;

    WaylandClient::InputEvent event;
    if (!WindowBridge::NextInputEvent(*session, event)) return JNI_FALSE;

    uint32_t xBits = 0;
    uint32_t yBits = 0;
    memcpy(&xBits, &event.x, sizeof(xBits));
    memcpy(&yBits, &event.y, sizeof(yBits));

    jlong words[INPUT_EVENT_WORDS];
    words[0] = static_cast<jlong>(event.kind);
    words[1] = event.action;
    words[2] = event.pointerId;
    words[3] = event.keyCode;
    words[4] = static_cast<jlong>(event.unicode);
    words[5] = event.metaState;
    words[6] = static_cast<jlong>(event.timeNanos);
    words[7] = (static_cast<jlong>(xBits) << 32) | static_cast<jlong>(yBits);

    env->SetLongArrayRegion(output, 0, INPUT_EVENT_WORDS, words);
    return JNI_TRUE;
}

static jlong WindowLastFrameTimeNanos(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    return session ? static_cast<jlong>(session->lastFrameTimeNanos) : 0;
}

static jboolean WindowIsCloseRequested(JNIEnv*, jclass, jlong handle)
{
    WindowBridge::Session* session = SessionFrom(handle);
    return session && WindowBridge::IsCloseRequested(*session) ? JNI_TRUE : JNI_FALSE;
}

static void WindowSetGuestPresents(JNIEnv*, jclass, jlong handle, jboolean guestPresents)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session || !guestPresents) return;
    if (!session->isHosted) return;

    HostedWindow::ReportGuestPresents(session->hosted);
}

static jboolean WindowConsumeResize(JNIEnv* env, jclass, jlong handle, jintArray sizeOut)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session || !sizeOut) return JNI_FALSE;
    if (env->GetArrayLength(sizeOut) < 2) return JNI_FALSE;

    uint32_t width = 0;
    uint32_t height = 0;
    if (!WindowBridge::ConsumeResize(*session, width, height)) return JNI_FALSE;

    jint values[2] = {(jint)width, (jint)height};
    env->SetIntArrayRegion(sizeOut, 0, 2, values);

    return JNI_TRUE;
}

static void WindowSetFullscreen(JNIEnv*, jclass, jlong handle, jboolean fullscreen)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session) return;

    WindowBridge::SetFullscreen(*session, fullscreen == JNI_TRUE);
}

static jboolean WindowDisplayMode(JNIEnv* env, jclass, jlong handle, jintArray modeOut)
{
    WindowBridge::Session* session = SessionFrom(handle);
    if (!session || !modeOut) return JNI_FALSE;
    if (env->GetArrayLength(modeOut) < 6) return JNI_FALSE;
    if (session->isHosted) return JNI_FALSE;

    WaylandClient::DisplayMode mode = WaylandClient::CurrentDisplayMode(session->connection);
    if (!mode.described) return JNI_FALSE;

    jint values[6] = {(jint)mode.width,           (jint)mode.height,          (jint)mode.refreshMilliHertz,
                      (jint)mode.scale,           (jint)mode.physicalWidthMm, (jint)mode.physicalHeightMm};
    env->SetIntArrayRegion(modeOut, 0, 6, values);

    return JNI_TRUE;
}

static const JNINativeMethod WINDOW_METHODS[] = {
    {"nativeOpen", "(IILjava/lang/String;)J", reinterpret_cast<void*>(&WindowOpen)},
    {"nativeDisplayMode", "(J[I)Z", reinterpret_cast<void*>(&WindowDisplayMode)},
    {"nativeConsumeResize", "(J[I)Z", reinterpret_cast<void*>(&WindowConsumeResize)},
    {"nativeSetFullscreen", "(JZ)V", reinterpret_cast<void*>(&WindowSetFullscreen)},
    {"nativeSetGuestPresents", "(JZ)V", reinterpret_cast<void*>(&WindowSetGuestPresents)},
    {"nativeClose", "(J)V", reinterpret_cast<void*>(&WindowClose)},
    {"nativeBeginFrame", "(J)J", reinterpret_cast<void*>(&WindowBeginFrame)},
    {"nativeEndFrame", "(J)V", reinterpret_cast<void*>(&WindowEndFrame)},
    {"nativeRequestFrame", "(J)V", reinterpret_cast<void*>(&WindowRequestFrame)},
    {"nativePoll", "(JI)Z", reinterpret_cast<void*>(&WindowPoll)},
    {"nativeWake", "(J)V", reinterpret_cast<void*>(&WindowWake)},
    {"nativeConsumeFrame", "(J)Z", reinterpret_cast<void*>(&WindowConsumeFrame)},
    {"nativeNextInputEvent", "(J[J)Z", reinterpret_cast<void*>(&WindowNextInputEvent)},
    {"nativeLastFrameTimeNanos", "(J)J", reinterpret_cast<void*>(&WindowLastFrameTimeNanos)},
    {"nativeIsCloseRequested", "(J)Z", reinterpret_cast<void*>(&WindowIsCloseRequested)},
};

static void StrictUnimplemented(JNIEnv* env, jclass, jstring method)
{
    std::string name = CopyJavaString(env, method);
    FrameworkJni::Unimplemented(env, name.c_str());
}

static jboolean StrictIsTracing(JNIEnv* env, jclass, jstring topic)
{
    std::string name = CopyJavaString(env, topic);
    return FrameworkJni::IsTracing(name.c_str()) ? JNI_TRUE : JNI_FALSE;
}

static const JNINativeMethod STRICT_METHODS[] = {
    {"nativeUnimplemented", "(Ljava/lang/String;)V", reinterpret_cast<void*>(&StrictUnimplemented)},
    {"nativeIsTracing", "(Ljava/lang/String;)Z", reinterpret_cast<void*>(&StrictIsTracing)},
};

static jstring SettingsLookup(JNIEnv* env, jclass, jstring name)
{
    std::string wanted = CopyJavaString(env, name);

    const char* value = Settings::Lookup(wanted.c_str());
    if (!value) return nullptr;

    return env->NewStringUTF(value);
}

static const JNINativeMethod SETTINGS_METHODS[] = {
    {"nativeLookup", "(Ljava/lang/String;)Ljava/lang/String;", reinterpret_cast<void*>(&SettingsLookup)},
};

static const JNINativeMethod LOG_METHODS[] = {
    {"println_native", "(IILjava/lang/String;Ljava/lang/String;)I", reinterpret_cast<void*>(&LogPrintlnNative)},
};

static jint ProcessId(JNIEnv*, jclass)
{
    return static_cast<jint>(getpid());
}

static jint ThreadId(JNIEnv*, jclass)
{
    return static_cast<jint>(syscall(SYS_gettid));
}

static jint UserId(JNIEnv*, jclass)
{
    return static_cast<jint>(getuid());
}

static const JNINativeMethod PROCESS_METHODS[] = {
    {"nativeProcessId", "()I", reinterpret_cast<void*>(&ProcessId)},
    {"nativeThreadId", "()I", reinterpret_cast<void*>(&ThreadId)},
    {"nativeUserId", "()I", reinterpret_cast<void*>(&UserId)},
};

void FrameworkJni::SetPackage(const Apk::Package* package)
{
    gPackage = package;
}

void FrameworkJni::SetInstalledPackages(const std::vector<PackageRegistry::Entry>* entries)
{
    gInstalledPackages = entries;
}

const Apk::Package* FrameworkJni::GetPackage()
{
    return gPackage;
}

char FrameworkJni::PriorityLetter(int priority)
{
    switch (priority)
    {
        case PRIORITY_VERBOSE: return 'V';
        case PRIORITY_DEBUG: return 'D';
        case PRIORITY_INFO: return 'I';
        case PRIORITY_WARN: return 'W';
        case PRIORITY_ERROR: return 'E';
        case PRIORITY_ASSERT: return 'A';
        default: return '?';
    }
}

bool FrameworkJni::IsStrict()
{
    const char* value = Settings::Lookup(STRICT_SETTING);
    if (!value) return true;
    return strcmp(value, "0") != 0;
}

bool FrameworkJni::IsTracing(const char* topic)
{
    const char* value = Settings::Lookup(TRACE_SETTING);
    if (!value || !*value) return false;

    size_t topicLength = strlen(topic);
    const char* cursor = value;

    while (*cursor)
    {
        const char* separator = strchr(cursor, ',');
        size_t length = separator ? static_cast<size_t>(separator - cursor) : strlen(cursor);

        if (length == 3 && strncmp(cursor, "all", 3) == 0) return true;
        if (length == topicLength && strncmp(cursor, topic, topicLength) == 0) return true;

        if (!separator) break;
        cursor = separator + 1;
    }

    return false;
}

void FrameworkJni::Unimplemented(JNIEnv* env, const char* method)
{
    Diagnostics::NoteGap(Diagnostics::Gap::FrameworkMethod, method);

    fprintf(stderr, "[framework] unimplemented: %s\n", method);

    if (env)
    {
        jclass throwable = env->FindClass("java/lang/Throwable");
        if (throwable)
        {
            jmethodID init = env->GetMethodID(throwable, "<init>", "(Ljava/lang/String;)V");
            jmethodID print = env->GetMethodID(throwable, "printStackTrace", "()V");
            jstring label = env->NewStringUTF(method);
            if (init && print && label)
            {
                jobject trace = env->NewObject(throwable, init, label);
                if (trace) env->CallVoidMethod(trace, print);
            }
        }
        env->ExceptionClear();
    }

    fflush(stderr);
    if (IsStrict()) abort();
}

ArtRuntime::Result FrameworkJni::RegisterAll(const ArtRuntime::Vm& vm)
{
    const ArtRuntime::NativeMethodTable tables[] = {
        {LOG_CLASS, LOG_METHODS, static_cast<int>(sizeof(LOG_METHODS) / sizeof(LOG_METHODS[0]))},
        {BOOTSTRAP_CLASS, BOOTSTRAP_METHODS, static_cast<int>(sizeof(BOOTSTRAP_METHODS) / sizeof(BOOTSTRAP_METHODS[0]))},
        {STRICT_CLASS, STRICT_METHODS, static_cast<int>(sizeof(STRICT_METHODS) / sizeof(STRICT_METHODS[0]))},
        {SETTINGS_CLASS, SETTINGS_METHODS, static_cast<int>(sizeof(SETTINGS_METHODS) / sizeof(SETTINGS_METHODS[0]))},
        {PACKAGES_CLASS, PACKAGES_METHODS, static_cast<int>(sizeof(PACKAGES_METHODS) / sizeof(PACKAGES_METHODS[0]))},
        {NOTIFICATION_STORE_CLASS, NOTIFICATION_STORE_METHODS,
         static_cast<int>(sizeof(NOTIFICATION_STORE_METHODS) / sizeof(NOTIFICATION_STORE_METHODS[0]))},
        {CANVAS_CLASS, CANVAS_METHODS, static_cast<int>(sizeof(CANVAS_METHODS) / sizeof(CANVAS_METHODS[0]))},
        {WINDOW_CLASS, WINDOW_METHODS, static_cast<int>(sizeof(WINDOW_METHODS) / sizeof(WINDOW_METHODS[0]))},
        {PROCESS_CLASS, PROCESS_METHODS, static_cast<int>(sizeof(PROCESS_METHODS) / sizeof(PROCESS_METHODS[0]))},
    };

    ArtRuntime::Result result = ArtRuntime::RegisterAll(vm, tables, static_cast<int>(sizeof(tables) / sizeof(tables[0])));
    if (result != ArtRuntime::Result::Ok) return result;

    int resourceTableCount = 0;
    const ArtRuntime::NativeMethodTable* resourceTables = ResourceBridge::Tables(resourceTableCount);
    result = ArtRuntime::RegisterAll(vm, resourceTables, resourceTableCount);
    if (result != ArtRuntime::Result::Ok) return result;

    int textTableCount = 0;
    const ArtRuntime::NativeMethodTable* textTables = TextBridge::Tables(textTableCount);
    result = ArtRuntime::RegisterAll(vm, textTables, textTableCount);
    if (result != ArtRuntime::Result::Ok) return result;

    int sqliteTableCount = 0;
    const ArtRuntime::NativeMethodTable* sqliteTables = SqliteBridge::Tables(sqliteTableCount);
    result = ArtRuntime::RegisterAll(vm, sqliteTables, sqliteTableCount);
    if (result != ArtRuntime::Result::Ok) return result;

    int graphicsTableCount = 0;
    const ArtRuntime::NativeMethodTable* graphicsTables = GraphicsBridge::Tables(graphicsTableCount);
    return ArtRuntime::RegisterAll(vm, graphicsTables, graphicsTableCount);
}

#include "NativeActivity/NativeActivity.hpp"

#include <cstdio>
#include <mutex>
#include <set>
#include <string>

static GuestRuntime::Instance* gRuntime = nullptr;
static JniBridge::Instance* gBridge = nullptr;
static std::mutex gActivityLock;
static std::set<jlong> gLiveActivities;

static std::string CopyJavaString(JNIEnv* env, jstring value)
{
    if (!value) return std::string();

    const char* raw = env->GetStringUTFChars(value, nullptr);
    if (!raw) return std::string();

    std::string copied(raw);
    env->ReleaseStringUTFChars(value, raw);
    return copied;
}

static jlong NativeActivityCreate(JNIEnv* env, jobject self, jstring libraryName, jstring internalDataPath)
{
    if (!gRuntime || !gBridge)
    {
        fprintf(stderr, "[nativeactivity] no guest runtime is installed, so %s cannot start\n", NativeActivity::ACTIVITY_CLASS);
        return 0;
    }

    std::string name = CopyJavaString(env, libraryName);
    std::string dataPath = CopyJavaString(env, internalDataPath);
    std::string soname = "lib" + name + ".so";

    uint32_t moduleId = 0;
    JniBridge::Result loaded = JniBridge::LoadGuestLibrary(*gBridge, soname, moduleId);
    if (loaded != JniBridge::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] cannot load %s: %s\n", soname.c_str(), JniBridge::ResultName(loaded));
        return 0;
    }

    NativeActivity::Instance* activity = new NativeActivity::Instance();
    jobject reference = env->NewGlobalRef(self);

    NativeActivity::Result created = NativeActivity::Create(*gRuntime, *gBridge, moduleId, dataPath, reference, *activity);
    if (created != NativeActivity::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] %s did not start: %s\n", soname.c_str(), NativeActivity::ResultName(created));
        env->DeleteGlobalRef(reference);
        delete activity;
        return 0;
    }

    fprintf(stderr, "[nativeactivity] %s is live\n", soname.c_str());
    jlong handle = (jlong)(uintptr_t)activity;
    {
        std::lock_guard<std::mutex> held(gActivityLock);
        gLiveActivities.insert(handle);
    }

    return handle;
}

static NativeActivity::Instance* ActivityFrom(jlong handle)
{
    std::lock_guard<std::mutex> held(gActivityLock);
    if (handle == 0 || gLiveActivities.find(handle) == gLiveActivities.end()) return nullptr;

    return (NativeActivity::Instance*)(uintptr_t)handle;
}

static NativeActivity::Instance* ClaimActivity(jlong handle)
{
    std::lock_guard<std::mutex> held(gActivityLock);
    if (handle == 0 || gLiveActivities.erase(handle) == 0) return nullptr;

    return (NativeActivity::Instance*)(uintptr_t)handle;
}

static void NativeActivityDispatch(JNIEnv*, jobject, jlong handle, jint callback)
{
    NativeActivity::Instance* activity = ActivityFrom(handle);
    if (!activity || callback < 0 || callback >= (jint)NativeActivity::Callback::Count) return;

    NativeActivity::Callback selected = (NativeActivity::Callback)callback;
    NativeActivity::Result invoked = NativeActivity::Invoke(*activity, selected, 0);

    if (invoked == NativeActivity::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] %s\n", NativeActivity::CallbackName(selected));
    }
}

static void NativeActivitySetWindow(JNIEnv*, jobject, jlong handle, jlong window)
{
    NativeActivity::Instance* activity = ActivityFrom(handle);
    if (!activity) return;

    NativeActivity::Callback callback =
        window != 0 ? NativeActivity::Callback::NativeWindowCreated : NativeActivity::Callback::NativeWindowDestroyed;

    if (NativeActivity::Invoke(*activity, callback, (uint64_t)window) == NativeActivity::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] %s\n", NativeActivity::CallbackName(callback));
    }
}

static void NativeActivityDestroy(JNIEnv*, jobject, jlong handle)
{
    NativeActivity::Instance* activity = ClaimActivity(handle);
    if (!activity) return;

    NativeActivity::Statistics statistics = NativeActivity::GetStatistics(*activity);
    fprintf(stderr, "[nativeactivity] finished: %llu registered, %llu invoked, %llu absent\n",
            (unsigned long long)statistics.callbacksRegistered, (unsigned long long)statistics.callbacksInvoked,
            (unsigned long long)statistics.callbacksAbsent);

    NativeActivity::Destroy(*activity);
    delete activity;
}

static jlong NativeActivityCreateWindow(JNIEnv*, jobject, jint width, jint height)
{
    if (!gRuntime) return 0;

    GuestState::GuestAddress window{};
    if (NativeActivity::CreateWindow(*gRuntime, (int32_t)width, (int32_t)height, window) != NativeActivity::Result::Ok) return 0;

    return (jlong)(uint64_t)window;
}

static jboolean NativeActivityFinishRequested(JNIEnv*, jobject)
{
    return NativeActivity::FinishRequested() ? JNI_TRUE : JNI_FALSE;
}

static const JNINativeMethod ACTIVITY_METHODS[] = {
    {"nativeCreate", "(Ljava/lang/String;Ljava/lang/String;)J", (void*)&NativeActivityCreate},
    {"nativeDispatch", "(JI)V", (void*)&NativeActivityDispatch},
    {"nativeSetWindow", "(JJ)V", (void*)&NativeActivitySetWindow},
    {"nativeDestroy", "(J)V", (void*)&NativeActivityDestroy},
    {"nativeCreateWindow", "(II)J", (void*)&NativeActivityCreateWindow},
    {"nativeFinishRequested", "()Z", (void*)&NativeActivityFinishRequested},
};

static jboolean NativeActivityServiceLooper(JNIEnv*, jobject)
{
    return NativeActivity::ServiceLooper() ? JNI_TRUE : JNI_FALSE;
}

static void NativeActivityPrepareLooper(JNIEnv*, jobject)
{
    NativeActivity::PrepareLooper();
}

static const JNINativeMethod SURFACE_METHODS[] = {
    {"nativeServiceLooper", "()Z", (void*)&NativeActivityServiceLooper},
    {"nativePrepareLooper", "()V", (void*)&NativeActivityPrepareLooper},
    {"nativeAcquireWindow", "(II)J", (void*)&NativeActivityCreateWindow},
};

NativeActivity::Result NativeActivity::InstallJni(JNIEnv* env, GuestRuntime::Instance& runtime, JniBridge::Instance& bridge,
                                                 bool guestBridgeLive)
{
    jclass activityClass = env->FindClass(ACTIVITY_CLASS);
    if (!activityClass)
    {
        env->ExceptionClear();
        fprintf(stderr, "[nativeactivity] %s is missing from the framework classes\n", ACTIVITY_CLASS);
        return Result::ClassMissing;
    }

    jint registered = env->RegisterNatives(activityClass, ACTIVITY_METHODS, (jint)(sizeof(ACTIVITY_METHODS) / sizeof(ACTIVITY_METHODS[0])));
    env->DeleteLocalRef(activityClass);

    if (registered != JNI_OK)
    {
        env->ExceptionClear();
        fprintf(stderr, "[nativeactivity] cannot register the natives of %s\n", ACTIVITY_CLASS);
        return Result::JniRegistrationFailed;
    }

    jclass surfaceClass = env->FindClass(SURFACE_BRIDGE_CLASS);
    if (!surfaceClass)
    {
        env->ExceptionClear();
        fprintf(stderr, "[nativeactivity] %s is missing from the framework classes\n", SURFACE_BRIDGE_CLASS);
        return Result::ClassMissing;
    }

    jint surfaceRegistered =
        env->RegisterNatives(surfaceClass, SURFACE_METHODS, (jint)(sizeof(SURFACE_METHODS) / sizeof(SURFACE_METHODS[0])));
    env->DeleteLocalRef(surfaceClass);

    if (surfaceRegistered != JNI_OK)
    {
        env->ExceptionClear();
        fprintf(stderr, "[nativeactivity] cannot register the natives of %s\n", SURFACE_BRIDGE_CLASS);
        return Result::JniRegistrationFailed;
    }

    if (guestBridgeLive)
    {
        Result functions = RegisterWindowFunctions(runtime);
        if (functions != Result::Ok) return functions;
    }

    gRuntime = &runtime;
    gBridge = &bridge;
    return Result::Ok;
}

void NativeActivity::RemoveJni()
{
    gRuntime = nullptr;
    gBridge = nullptr;
}

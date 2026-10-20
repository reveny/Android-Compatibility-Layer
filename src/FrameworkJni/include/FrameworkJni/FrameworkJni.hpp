#pragma once

#include <cstdint>
#include <string>

#include <jni.h>

#include "Apk/Apk.hpp"
#include "ApkSignature/ApkSignature.hpp"
#include "ArtRuntime/ArtRuntime.hpp"
#include "PackageRegistry/PackageRegistry.hpp"

namespace FrameworkJni
{
    inline constexpr const char* LOG_CLASS = "android/util/Log";
    inline constexpr const char* RESOURCES_CLASS = "android/content/res/Resources";
    inline constexpr const char* BOOTSTRAP_CLASS = "runtime/Bootstrap";
    inline constexpr const char* STRICT_CLASS = "runtime/Strict";
    inline constexpr const char* SETTINGS_CLASS = "runtime/Settings";
    inline constexpr const char* PACKAGES_CLASS = "runtime/Packages";
    inline constexpr const char* NOTIFICATION_STORE_CLASS = "runtime/NotificationStore";
    inline constexpr const char* CANVAS_CLASS = "android/graphics/Canvas";
    inline constexpr const char* WINDOW_CLASS = "runtime/WindowBridge";
    inline constexpr const char* PROCESS_CLASS = "android/os/Process";
    inline constexpr const char* BOOTSTRAP_METHOD = "start";
    inline constexpr const char* BOOTSTRAP_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;)I";
    inline constexpr const char* STRICT_SETTING = "strict";
    inline constexpr const char* TRACE_SETTING = "trace";

    inline constexpr int PRIORITY_VERBOSE = 2;
    inline constexpr int PRIORITY_DEBUG = 3;
    inline constexpr int PRIORITY_INFO = 4;
    inline constexpr int PRIORITY_WARN = 5;
    inline constexpr int PRIORITY_ERROR = 6;
    inline constexpr int PRIORITY_ASSERT = 7;

    void SetPackage(const Apk::Package* package);
    void SetInstalledPackages(const std::vector<PackageRegistry::Entry>* entries);
    void SetSigningInfo(const ApkSignature::Info& info);
    void SetExternalStorageDirectory(const std::string& path);
    void SetArchivePath(const std::string& path);
    const Apk::Package* GetPackage();

    ArtRuntime::Result RegisterAll(const ArtRuntime::Vm& vm);

    bool IsStrict();
    bool IsTracing(const char* topic);
    void Unimplemented(JNIEnv* env, const char* method);

    char PriorityLetter(int priority);
}

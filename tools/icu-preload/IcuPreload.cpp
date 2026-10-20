#include "IcuPreload.hpp"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>

bool IcuPreload::IsVerbose()
{
    return false;
}

void IcuPreload::Report(const char* format, ...)
{
    va_list arguments;
    va_start(arguments, format);
    fputs(LOG_PREFIX, stderr);
    vfprintf(stderr, format, arguments);
    fputc('\n', stderr);
    va_end(arguments);
}

void* IcuPreload::OpenIcuLibrary()
{
    dlerror();
    void* handle = dlopen(ICU_LIBRARY, RTLD_NOW | RTLD_LOCAL);
    if (handle) return handle;

    char error[MAX_PATH_LENGTH];
    const char* message = dlerror();
    snprintf(error, sizeof(error), "%s", message ? message : "unknown");

    Report("dlopen %s failed: %s", ICU_LIBRARY, error);
    return nullptr;
}

IcuPreload::SetDataDirectoryFn IcuPreload::ResolveSetter(void* handle)
{
    dlerror();
    SetDataDirectoryFn plain = (SetDataDirectoryFn)dlsym(handle, "u_setDataDirectory");
    if (plain) return plain;

    for (int version = ICU_VERSION_MIN; version <= ICU_VERSION_MAX; ++version)
    {
        char name[MAX_SYMBOL_LENGTH];
        int written = snprintf(name, sizeof(name), "u_setDataDirectory_%d", version);
        if (written < 0 || (size_t)written >= sizeof(name)) continue;

        dlerror();
        SetDataDirectoryFn found = (SetDataDirectoryFn)dlsym(handle, name);
        if (!found) continue;

        if (IsVerbose()) Report("resolved %s", name);
        return found;
    }

    return nullptr;
}

bool IcuPreload::Configure()
{
    const char* i18nRoot = getenv("ANDROID_I18N_ROOT");
    if (!i18nRoot)
    {
        Report("ANDROID_I18N_ROOT is unset; ART will abort in libicu_jni JNI_OnLoad");
        return false;
    }

    void* handle = OpenIcuLibrary();
    if (!handle) return false;

    SetDataDirectoryFn setter = ResolveSetter(handle);
    if (!setter)
    {
        Report("no u_setDataDirectory symbol in %s (probed unversioned and _%d.._%d)", ICU_LIBRARY, ICU_VERSION_MIN, ICU_VERSION_MAX);
        return false;
    }

    char dataDir[MAX_PATH_LENGTH];
    int written = snprintf(dataDir, sizeof(dataDir), "%s%s", i18nRoot, ICU_DATA_SUBDIR);
    if (written < 0 || (size_t)written >= sizeof(dataDir))
    {
        Report("ANDROID_I18N_ROOT is too long; data directory would be truncated");
        return false;
    }

    setter(dataDir);

    if (IsVerbose()) Report("data directory set to %s", dataDir);
    return true;
}

extern "C" __attribute__((constructor)) void IcuPreloadInit()
{
    if (IcuPreload::Configure()) return;
    IcuPreload::Report("ICU was not configured; the runtime is expected to fail");
}

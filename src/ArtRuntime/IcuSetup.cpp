#include "ArtRuntime/IcuSetup.hpp"

#include <cstdio>
#include <dlfcn.h>
#include <sys/stat.h>

IcuSetup::Result IcuSetup::Configure(const std::string& hostRoot)
{
    std::string dataDirectory = hostRoot + "/com.android.i18n" + ICU_DATA_SUBDIR;

    struct stat info;
    if (stat(dataDirectory.c_str(), &info) != 0)
    {
        fprintf(stderr, "[icu] data directory missing: %s (run tools/extract-icu-data.sh)\n", dataDirectory.c_str());
        return Result::DataDirectoryMissing;
    }

    std::string libraryFile = hostRoot + "/lib64/" + ICU_LIBRARY;
    dlerror();
    void* handle = dlopen(libraryFile.c_str(), RTLD_NOW | RTLD_GLOBAL);
    if (!handle)
    {
        const char* error = dlerror();
        fprintf(stderr, "[icu] dlopen %s failed: %s\n", libraryFile.c_str(), error ? error : "unknown");
        return Result::LibraryNotFound;
    }

    SetDataDirectoryFn setter = (SetDataDirectoryFn)dlsym(handle, "u_setDataDirectory");
    for (int version = ICU_VERSION_MIN; !setter && version <= ICU_VERSION_MAX; ++version)
    {
        char name[MAX_SYMBOL_LENGTH];
        int written = snprintf(name, sizeof(name), "u_setDataDirectory_%d", version);
        if (written < 0 || (size_t)written >= sizeof(name)) continue;
        setter = (SetDataDirectoryFn)dlsym(handle, name);
    }

    if (!setter)
    {
        fprintf(stderr, "[icu] no u_setDataDirectory symbol in %s (probed unversioned and _%d.._%d)\n", ICU_LIBRARY, ICU_VERSION_MIN,
                ICU_VERSION_MAX);
        return Result::SymbolNotFound;
    }

    setter(dataDirectory.c_str());
    return Result::Ok;
}

const char* IcuSetup::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::LibraryNotFound: return "LibraryNotFound";
        case Result::SymbolNotFound: return "SymbolNotFound";
        case Result::DataDirectoryMissing: return "DataDirectoryMissing";
    }
    return "Unknown";
}

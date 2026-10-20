#include "TranslationCache/TranslationCache.hpp"

#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dirent.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/stat.h>
#include <unistd.h>

#include "Settings/Settings.hpp"

static constexpr uint64_t HASH_OFFSET = 1469598103934665603ull;
static constexpr uint64_t HASH_PRIME = 1099511628211ull;
static constexpr uint64_t READ_CHUNK_BYTES = 1u << 16;

static void MixHash(uint64_t& low, uint64_t& high, const uint8_t* data, uint64_t length)
{
    for (uint64_t index = 0; index < length; ++index)
    {
        low = (low ^ data[index]) * HASH_PRIME;
        high = (high + low) * HASH_PRIME;
        high ^= high >> 29;
    }
}

static bool EnsureDirectory(const std::string& path)
{
    if (path.empty()) return false;
    if (mkdir(path.c_str(), 0755) == 0) return true;

    struct stat information;
    if (stat(path.c_str(), &information) != 0) return false;
    return S_ISDIR(information.st_mode);
}

static std::string HexOf(uint64_t low, uint64_t high)
{
    char text[33];
    snprintf(text, sizeof(text), "%016llx%016llx", (unsigned long long)high, (unsigned long long)low);
    return std::string(text);
}

TranslationCache::Result TranslationCache::DefaultRoot(std::string& root)
{
    const char* explicitRoot = Settings::Lookup(TranslationCache::ROOT_SETTING);
    if (explicitRoot && *explicitRoot)
    {
        root = explicitRoot;
        return Result::Ok;
    }

    std::string home = Settings::HomeDirectory();
    if (home.empty()) return Result::Unwritable;

    root = home + "/.cache/acl";
    return Result::Ok;
}

TranslationCache::Result TranslationCache::KeyForFile(const std::string& imagePath, const std::string& runtimeVersion, std::string& key)
{
    int descriptor = open(imagePath.c_str(), O_RDONLY | O_CLOEXEC);
    if (descriptor < 0) return Result::NotFound;

    uint64_t low = HASH_OFFSET;
    uint64_t high = HASH_OFFSET;

    std::vector<uint8_t> chunk((size_t)READ_CHUNK_BYTES);
    for (;;)
    {
        ssize_t taken = read(descriptor, chunk.data(), chunk.size());
        if (taken < 0)
        {
            close(descriptor);
            return Result::Unreadable;
        }
        if (taken == 0) break;

        MixHash(low, high, chunk.data(), (uint64_t)taken);
    }

    close(descriptor);

    MixHash(low, high, (const uint8_t*)runtimeVersion.data(), runtimeVersion.size());
    key = HexOf(low, high);

    return Result::Ok;
}

TranslationCache::Result TranslationCache::Lookup(const Configuration& configuration, const std::string& key, Entry& entry)
{
    if (!configuration.enabled) return Result::Disabled;
    if (key.size() > KEY_CHARACTERS) return Result::KeyMismatch;

    entry.key = key;
    entry.path = configuration.root + "/" + key + ".o";

    struct stat information;
    if (stat(entry.path.c_str(), &information) != 0) return Result::NotFound;
    if (!S_ISREG(information.st_mode)) return Result::Unreadable;
    if ((uint64_t)information.st_size > MAX_OBJECT_BYTES) return Result::Unreadable;

    entry.objectBytes = (uint64_t)information.st_size;
    return Result::Ok;
}

static bool IsSafeKey(const std::string& key)
{
    if (key.empty() || key.size() > TranslationCache::KEY_CHARACTERS) return false;

    for (char character : key)
    {
        bool digit = character >= '0' && character <= '9';
        bool lower = character >= 'a' && character <= 'z';
        bool upper = character >= 'A' && character <= 'Z';

        if (!digit && !lower && !upper) return false;
    }

    return true;
}

TranslationCache::Result TranslationCache::Store(const Configuration& configuration, const std::string& key, const void* object,
                                                 uint64_t length, Entry& entry)
{
    if (!configuration.enabled) return Result::Disabled;
    if (length > MAX_OBJECT_BYTES) return Result::Unwritable;
    if (!IsSafeKey(key)) return Result::KeyMismatch;
    if (!EnsureDirectory(configuration.root)) return Result::Unwritable;

    entry.key = key;
    entry.path = configuration.root + "/" + key + ".o";
    entry.objectBytes = length;

    std::string partial = entry.path + ".partial." + std::to_string((long)getpid());

    int descriptor = open(partial.c_str(), O_WRONLY | O_CREAT | O_EXCL | O_NOFOLLOW | O_CLOEXEC, 0644);
    if (descriptor < 0) return Result::Unwritable;

    const uint8_t* cursor = (const uint8_t*)object;
    uint64_t remaining = length;

    while (remaining != 0)
    {
        ssize_t written = write(descriptor, cursor, (size_t)remaining);
        if (written <= 0)
        {
            close(descriptor);
            unlink(partial.c_str());
            return Result::Unwritable;
        }

        cursor += written;
        remaining -= (uint64_t)written;
    }

    close(descriptor);

    if (rename(partial.c_str(), entry.path.c_str()) != 0)
    {
        unlink(partial.c_str());
        return Result::Unwritable;
    }

    return Result::Ok;
}

TranslationCache::Result TranslationCache::Adopt(const Configuration& configuration, const std::string& key, const std::string& objectPath,
                                                 Entry& entry)
{
    if (!configuration.enabled) return Result::Disabled;
    if (!IsSafeKey(key)) return Result::KeyMismatch;
    if (!EnsureDirectory(configuration.root)) return Result::Unwritable;

    struct stat described{};
    if (stat(objectPath.c_str(), &described) != 0 || !S_ISREG(described.st_mode)) return Result::Unwritable;
    if (described.st_size <= 0 || (uint64_t)described.st_size > MAX_OBJECT_BYTES) return Result::Unwritable;

    entry.key = key;
    entry.path = configuration.root + "/" + key + ".o";
    entry.objectBytes = (uint64_t)described.st_size;

    if (rename(objectPath.c_str(), entry.path.c_str()) != 0) return Result::Unwritable;

    return Result::Ok;
}

static bool WriterHasGone(const std::string& name, const char* marker)
{
    size_t at = name.find(marker);
    if (at == std::string::npos) return false;

    const char* digits = name.c_str() + at + strlen(marker);
    char* end = nullptr;
    long writer = strtol(digits, &end, 10);

    if (end == digits || writer <= 0) return false;
    if (*end != '\0' && *end != '.') return false;

    return kill((pid_t)writer, 0) != 0 && errno == ESRCH;
}

TranslationCache::Result TranslationCache::SweepAbandoned(const Configuration& configuration, uint64_t& removed)
{
    removed = 0;
    if (!configuration.enabled) return Result::Disabled;

    DIR* directory = opendir(configuration.root.c_str());
    if (!directory) return Result::NotFound;

    for (;;)
    {
        struct dirent* entry = readdir(directory);
        if (!entry) break;

        std::string name = entry->d_name;
        if (!WriterHasGone(name, ".building.") && !WriterHasGone(name, ".partial.")) continue;

        if (unlink((configuration.root + "/" + name).c_str()) == 0) removed += 1;
    }

    closedir(directory);
    return Result::Ok;
}

TranslationCache::Result TranslationCache::Load(const Entry& entry, std::vector<uint8_t>& object)
{
    int descriptor = open(entry.path.c_str(), O_RDONLY | O_CLOEXEC);
    if (descriptor < 0) return Result::NotFound;

    object.assign((size_t)entry.objectBytes, 0);

    uint64_t filled = 0;
    while (filled < entry.objectBytes)
    {
        ssize_t taken = read(descriptor, object.data() + filled, (size_t)(entry.objectBytes - filled));
        if (taken <= 0)
        {
            close(descriptor);
            return Result::Unreadable;
        }

        filled += (uint64_t)taken;
    }

    close(descriptor);
    return Result::Ok;
}

const char* TranslationCache::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotFound: return "NotFound";
        case Result::Unreadable: return "Unreadable";
        case Result::Unwritable: return "Unwritable";
        case Result::KeyMismatch: return "KeyMismatch";
        case Result::Disabled: return "Disabled";
    }

    return "Unknown";
}

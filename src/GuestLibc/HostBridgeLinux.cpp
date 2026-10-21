#include "GuestLibc/HostBridge.hpp"

#include <cctype>
#include <clocale>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <cwchar>
#include <cwctype>
#include <cerrno>
#include <dirent.h>
#include <fcntl.h>
#include <fnmatch.h>
#include <libgen.h>
#include <locale.h>
#include <poll.h>
#include <pthread.h>
#include <sched.h>
#include <semaphore.h>
#include <sys/file.h>
#include <signal.h>
#include <strings.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/sendfile.h>
#include <sys/statvfs.h>
#include <sys/vfs.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <pwd.h>
#include <unistd.h>
#include <utime.h>
#include <zlib.h>

#include <atomic>
#include <mutex>
#include <string>
#include <vector>

#include "Platform/DeviceIdentity.hpp"
#include "Settings/Settings.hpp"

static constexpr int64_t SYNTHETIC_PROCESSOR_COUNT = 8;
static constexpr uint64_t SYNTHETIC_MEMORY_KILOBYTES = Platform::DEVICE_MEMORY_KILOBYTES;
static constexpr const char* SYNTHETIC_PROCESSOR_FEATURES = "fp asimd atomics";

struct GuestMapping
{
    uint64_t base;
    uint64_t length;
};

static std::mutex guestMappingMutex;
static uint64_t guestMemoryBase = 0;
static uint64_t guestMemoryLength = 0;
static std::vector<GuestMapping> guestMappings;

static bool RangeContains(uint64_t base, uint64_t length, uint64_t address, uint64_t span)
{
    if (length == 0) return false;
    if (address < base) return false;

    uint64_t offset = address - base;
    if (offset >= length) return false;

    return span <= length - offset;
}

static bool GuestOwns(uint64_t address, uint64_t length)
{
    if (length == 0) return true;
    if (address + length < address) return false;

    std::lock_guard<std::mutex> guard(guestMappingMutex);

    if (RangeContains(guestMemoryBase, guestMemoryLength, address, length)) return true;

    for (const GuestMapping& mapping : guestMappings)
    {
        if (RangeContains(mapping.base, mapping.length, address, length)) return true;
    }

    return false;
}

static void RememberGuestMapping(uint64_t address, uint64_t length)
{
    if (length == 0) return;

    std::lock_guard<std::mutex> guard(guestMappingMutex);

    if (RangeContains(guestMemoryBase, guestMemoryLength, address, length)) return;

    guestMappings.push_back({address, length});
}

static void ForgetGuestMapping(uint64_t address, uint64_t length)
{
    if (length == 0) return;

    std::lock_guard<std::mutex> guard(guestMappingMutex);

    std::vector<GuestMapping> kept;
    kept.reserve(guestMappings.size() + 1);

    uint64_t end = address + length;

    for (const GuestMapping& mapping : guestMappings)
    {
        uint64_t mappingEnd = mapping.base + mapping.length;

        if (mappingEnd <= address || mapping.base >= end)
        {
            kept.push_back(mapping);
            continue;
        }

        if (mapping.base < address) kept.push_back({mapping.base, address - mapping.base});
        if (mappingEnd > end) kept.push_back({end, mappingEnd - end});
    }

    guestMappings.swap(kept);
}

static void RefuseGuestMemoryCall(const char* name, uint64_t address, uint64_t length)
{
    fprintf(stderr, "[hostbridge] refused %s(%016llx, %llu): the guest does not own that memory\n", name,
            (unsigned long long)address, (unsigned long long)length);
    fflush(stderr);
}

static std::string BuildProcessorDescription()
{
    int64_t total = HostBridge::ProcessorCount();
    int64_t efficient = total > 2 ? total / 2 : total;

    std::string described;

    for (int64_t index = 0; index < total; ++index)
    {
        bool little = index < efficient;

        described += "processor\t: " + std::to_string(index) + "\n";
        described += "BogoMIPS\t: 38.40\n";
        described += std::string("Features\t: ") + SYNTHETIC_PROCESSOR_FEATURES + "\n";
        described += "CPU implementer\t: 0x41\n";
        described += "CPU architecture: 8\n";
        described += std::string("CPU variant\t: ") + (little ? "0x0" : "0x1") + "\n";
        described += std::string("CPU part\t: ") + (little ? "0xd05" : "0xd0b") + "\n";
        described += "CPU revision\t: 0\n\n";
    }

    described += "Hardware\t: " + std::string(Platform::DEVICE_BOARD) + "\n";

    return described;
}

static void AppendMemoryLine(std::string& described, const char* name, uint64_t kilobytes)
{
    std::string label = name;
    label += ":";

    while (label.size() < 16) label += ' ';

    std::string amount = std::to_string(kilobytes);
    while (amount.size() < 8) amount.insert(amount.begin(), ' ');

    described += label + amount + " kB\n";
}

static std::string BuildMemoryDescription()
{
    uint64_t total = SYNTHETIC_MEMORY_KILOBYTES;
    uint64_t free = total / 8;
    uint64_t cached = total / 3;
    uint64_t available = free + cached;
    uint64_t anonymous = total / 6;

    std::string described;

    AppendMemoryLine(described, "MemTotal", total);
    AppendMemoryLine(described, "MemFree", free);
    AppendMemoryLine(described, "MemAvailable", available);
    AppendMemoryLine(described, "Buffers", total / 512);
    AppendMemoryLine(described, "Cached", cached);
    AppendMemoryLine(described, "SwapCached", 0);
    AppendMemoryLine(described, "Active", anonymous + cached / 3);
    AppendMemoryLine(described, "Inactive", cached / 2);
    AppendMemoryLine(described, "Active(anon)", anonymous / 2);
    AppendMemoryLine(described, "Inactive(anon)", anonymous / 4);
    AppendMemoryLine(described, "Active(file)", cached / 3);
    AppendMemoryLine(described, "Inactive(file)", cached / 2);
    AppendMemoryLine(described, "Unevictable", 0);
    AppendMemoryLine(described, "Mlocked", 0);
    AppendMemoryLine(described, "SwapTotal", 0);
    AppendMemoryLine(described, "SwapFree", 0);
    AppendMemoryLine(described, "Dirty", 0);
    AppendMemoryLine(described, "Writeback", 0);
    AppendMemoryLine(described, "AnonPages", anonymous);
    AppendMemoryLine(described, "Mapped", total / 12);
    AppendMemoryLine(described, "Shmem", total / 64);
    AppendMemoryLine(described, "Slab", total / 32);
    AppendMemoryLine(described, "SReclaimable", total / 64);
    AppendMemoryLine(described, "SUnreclaim", total / 64);
    AppendMemoryLine(described, "KernelStack", total / 512);
    AppendMemoryLine(described, "PageTables", total / 128);
    AppendMemoryLine(described, "CommitLimit", total / 2);
    AppendMemoryLine(described, "Committed_AS", total / 3);
    AppendMemoryLine(described, "VmallocTotal", 34359738367ull);
    AppendMemoryLine(described, "VmallocUsed", total / 64);
    AppendMemoryLine(described, "VmallocChunk", 0);

    return described;
}

const std::string& HostBridge::ProcessorDescription()
{
    static const std::string processors = BuildProcessorDescription();
    return processors;
}

const std::string& HostBridge::MemoryDescription()
{
    static const std::string memory = BuildMemoryDescription();
    return memory;
}

static const std::string* SyntheticSystemFile(const char* path)
{
    if (!path) return nullptr;

    if (strcmp(path, "/proc/cpuinfo") == 0) return &HostBridge::ProcessorDescription();
    if (strcmp(path, "/proc/meminfo") == 0) return &HostBridge::MemoryDescription();

    return nullptr;
}

struct AndroidRedirect
{
    std::string guestPrefix;
    std::string hostPrefix;
};

static std::vector<AndroidRedirect> androidTree;

static void AddAndroidRedirect(const std::string& guestPrefix, const std::string& hostPrefix)
{
    if (guestPrefix.empty() || hostPrefix.empty()) return;

    androidTree.push_back({guestPrefix, hostPrefix});
}

void HostBridge::MapAndroidTree(const char* dataDirectory, const char* externalDirectory, const char* systemDirectory,
                                const char* packageName)
{
    androidTree.clear();

    std::string data = dataDirectory ? dataDirectory : "";
    std::string external = externalDirectory ? externalDirectory : "";
    std::string system = systemDirectory ? systemDirectory : "";
    std::string package = packageName ? packageName : "";

    if (!package.empty() && !data.empty())
    {
        AddAndroidRedirect("/data/data/" + package, data);
        AddAndroidRedirect("/data/user/0/" + package, data);
        AddAndroidRedirect("/data/user_de/0/" + package, data);
    }

    AddAndroidRedirect("/data/local/tmp", data.empty() ? std::string() : data + "/cache");

    if (!external.empty())
    {
        AddAndroidRedirect("/sdcard", external);
        AddAndroidRedirect("/storage/emulated/0", external);
        AddAndroidRedirect("/storage/self/primary", external);
        AddAndroidRedirect("/mnt/sdcard", external);
    }

    AddAndroidRedirect("/system", system);
}

static const char* TranslateGuestPath(const char* path, std::string& storage)
{
    if (!path || path[0] != '/' || androidTree.empty()) return path;

    for (const AndroidRedirect& redirect : androidTree)
    {
        size_t length = redirect.guestPrefix.size();
        if (strncmp(path, redirect.guestPrefix.c_str(), length) != 0) continue;

        char following = path[length];
        if (following != '\0' && following != '/') continue;

        storage = redirect.hostPrefix;
        storage.append(path + length);
        return storage.c_str();
    }

    return path;
}

struct GuestPath
{
    std::string storage;
    const char* value;

    explicit GuestPath(const char* path) : value(TranslateGuestPath(path, storage)) {}

    operator const char*() const { return value; }
};

static int OpenSyntheticDescriptor(const std::string& content)
{
    int descriptor = memfd_create("acl-proc", MFD_CLOEXEC);
    if (descriptor < 0) return -1;

    if (write(descriptor, content.data(), content.size()) != (ssize_t)content.size() || lseek(descriptor, 0, SEEK_SET) != 0)
    {
        close(descriptor);
        return -1;
    }

    return descriptor;
}

static locale_t AsLocale(void* locale)
{
    return (locale_t)locale;
}

template <typename Callable>
static auto WithLocale(void* locale, Callable&& body) -> decltype(body())
{
    if (!locale) return body();

    locale_t previous = uselocale(AsLocale(locale));
    auto produced = body();
    uselocale(previous);
    return produced;
}

void* HostBridge::AllocateBytes(uint64_t size)
{
    return malloc((size_t)size);
}

void* HostBridge::AllocateZeroed(uint64_t count, uint64_t size)
{
    return calloc((size_t)count, (size_t)size);
}

void* HostBridge::ResizeAllocation(void* block, uint64_t size)
{
    return realloc(block, (size_t)size);
}

void HostBridge::ReleaseAllocation(void* block)
{
    free(block);
}

void* HostBridge::AllocateAligned(uint64_t alignment, uint64_t size)
{
    void* block = nullptr;
    if (alignment < sizeof(void*)) alignment = sizeof(void*);
    if (posix_memalign(&block, (size_t)alignment, (size_t)size) != 0) return nullptr;

    return block;
}

int32_t HostBridge::AllocatePosixAligned(void** out, uint64_t alignment, uint64_t size)
{
    return posix_memalign(out, (size_t)alignment, (size_t)size);
}

void* HostBridge::CopyBytes(void* destination, const void* source, uint64_t count)
{
    return memcpy(destination, source, (size_t)count);
}

void* HostBridge::MoveBytes(void* destination, const void* source, uint64_t count)
{
    return memmove(destination, source, (size_t)count);
}

void* HostBridge::FillBytes(void* destination, int32_t value, uint64_t count)
{
    return memset(destination, value, (size_t)count);
}

int32_t HostBridge::CompareBytes(const void* left, const void* right, uint64_t count)
{
    return memcmp(left, right, (size_t)count);
}

void* HostBridge::FindByte(const void* block, int32_t value, uint64_t count)
{
    return (void*)memchr(block, value, (size_t)count);
}

void* HostBridge::FindLastByte(const void* block, int32_t value, uint64_t count)
{
    const uint8_t* bytes = (const uint8_t*)block;

    for (uint64_t index = count; index > 0; --index)
    {
        if (bytes[index - 1] == (uint8_t)value) return (void*)(bytes + index - 1);
    }

    return nullptr;
}

uint64_t HostBridge::StringLength(const char* text)
{
    return strlen(text);
}

uint64_t HostBridge::StringLengthBounded(const char* text, uint64_t limit)
{
    return strnlen(text, (size_t)limit);
}

int32_t HostBridge::StringCompare(const char* left, const char* right)
{
    return strcmp(left, right);
}

int32_t HostBridge::StringCompareBounded(const char* left, const char* right, uint64_t count)
{
    return strncmp(left, right, (size_t)count);
}

int32_t HostBridge::StringCompareIgnoringCase(const char* left, const char* right)
{
    return strcasecmp(left, right);
}

int32_t HostBridge::StringCompareIgnoringCaseBounded(const char* left, const char* right, uint64_t count)
{
    return strncasecmp(left, right, (size_t)count);
}

char* HostBridge::StringCopy(char* destination, const char* source)
{
    return strcpy(destination, source);
}

char* HostBridge::StringCopyBounded(char* destination, const char* source, uint64_t count)
{
    return strncpy(destination, source, (size_t)count);
}

uint64_t HostBridge::StringCopyTerminated(char* destination, const char* source, uint64_t size)
{
    uint64_t length = strlen(source);

    if (size != 0)
    {
        uint64_t copied = length < size - 1 ? length : size - 1;
        memcpy(destination, source, (size_t)copied);
        destination[copied] = '\0';
    }

    return length;
}

char* HostBridge::StringCopyReturningEnd(char* destination, const char* source)
{
    uint64_t length = strlen(source);
    memcpy(destination, source, (size_t)length + 1);
    return destination + length;
}

char* HostBridge::StringAppend(char* destination, const char* source)
{
    return strcat(destination, source);
}

char* HostBridge::StringAppendBounded(char* destination, const char* source, uint64_t count)
{
    return strncat(destination, source, (size_t)count);
}

char* HostBridge::FindCharacter(const char* text, int32_t value)
{
    return (char*)strchr(text, value);
}

char* HostBridge::FindLastCharacter(const char* text, int32_t value)
{
    return (char*)strrchr(text, value);
}

char* HostBridge::FindSubstring(const char* text, const char* pattern)
{
    return (char*)strstr(text, pattern);
}

char* HostBridge::FindAnyCharacter(const char* text, const char* set)
{
    return (char*)strpbrk(text, set);
}

uint64_t HostBridge::SpanOfCharacters(const char* text, const char* set)
{
    return strspn(text, set);
}

uint64_t HostBridge::SpanExcludingCharacters(const char* text, const char* set)
{
    return strcspn(text, set);
}

char* HostBridge::SplitString(char* text, const char* separators, char** state)
{
    return strtok_r(text, separators, state);
}

char* HostBridge::DuplicateString(const char* text)
{
    return strdup(text);
}

char* HostBridge::DuplicateStringBounded(const char* text, uint64_t limit)
{
    return strndup(text, (size_t)limit);
}

char* HostBridge::DescribeErrorNumber(int32_t number)
{
    return strerror(number);
}

int32_t HostBridge::DescribeErrorInto(int32_t number, char* buffer, uint64_t size)
{
    const char* text = strerror(number);
    if (!buffer || size == 0) return 0;

    StringCopyTerminated(buffer, text, size);
    return 0;
}

int32_t HostBridge::StringCollate(const char* left, const char* right)
{
    return strcoll(left, right);
}

uint64_t HostBridge::StringTransform(char* destination, const char* source, uint64_t count)
{
    return strxfrm(destination, source, (size_t)count);
}

uint64_t HostBridge::WideLength(const void* text)
{
    return wcslen((const wchar_t*)text);
}

int32_t HostBridge::WideCompare(const void* left, const void* right)
{
    return wcscmp((const wchar_t*)left, (const wchar_t*)right);
}

int32_t HostBridge::WideCompareBounded(const void* left, const void* right, uint64_t count)
{
    return wcsncmp((const wchar_t*)left, (const wchar_t*)right, (size_t)count);
}

void* HostBridge::WideCopy(void* destination, const void* source)
{
    return wcscpy((wchar_t*)destination, (const wchar_t*)source);
}

void* HostBridge::WideCopyBounded(void* destination, const void* source, uint64_t count)
{
    return wcsncpy((wchar_t*)destination, (const wchar_t*)source, (size_t)count);
}

void* HostBridge::WideFindCharacter(const void* text, int32_t value)
{
    return (void*)wcschr((const wchar_t*)text, (wchar_t)value);
}

void* HostBridge::WideFindLastCharacter(const void* text, int32_t value)
{
    return (void*)wcsrchr((const wchar_t*)text, (wchar_t)value);
}

void* HostBridge::WideFindSubstring(const void* text, const void* pattern)
{
    return (void*)wcsstr((const wchar_t*)text, (const wchar_t*)pattern);
}

void* HostBridge::WideCopyBytes(void* destination, const void* source, uint64_t count)
{
    return wmemcpy((wchar_t*)destination, (const wchar_t*)source, (size_t)count);
}

void* HostBridge::WideMoveBytes(void* destination, const void* source, uint64_t count)
{
    return wmemmove((wchar_t*)destination, (const wchar_t*)source, (size_t)count);
}

void* HostBridge::WideFill(void* destination, int32_t value, uint64_t count)
{
    return wmemset((wchar_t*)destination, (wchar_t)value, (size_t)count);
}

void* HostBridge::WideFindValue(const void* block, int32_t value, uint64_t count)
{
    return (void*)wmemchr((const wchar_t*)block, (wchar_t)value, (size_t)count);
}

int32_t HostBridge::WideCompareValues(const void* left, const void* right, uint64_t count)
{
    return wmemcmp((const wchar_t*)left, (const wchar_t*)right, (size_t)count);
}

int32_t HostBridge::WideCollate(const void* left, const void* right)
{
    return wcscoll((const wchar_t*)left, (const wchar_t*)right);
}

uint64_t HostBridge::WideTransform(void* destination, const void* source, uint64_t count)
{
    return wcsxfrm((wchar_t*)destination, (const wchar_t*)source, (size_t)count);
}

uint64_t HostBridge::DecodeCharacter(void* wide, const char* text, uint64_t limit, void* state)
{
    return mbrtowc((wchar_t*)wide, text, (size_t)limit, (mbstate_t*)state);
}

uint64_t HostBridge::EncodeCharacter(char* text, int32_t wide, void* state)
{
    return wcrtomb(text, (wchar_t)wide, (mbstate_t*)state);
}

uint64_t HostBridge::DecodeString(void* wide, const char** text, uint64_t limit, void* state)
{
    return mbsrtowcs((wchar_t*)wide, text, (size_t)limit, (mbstate_t*)state);
}

uint64_t HostBridge::EncodeString(char* text, const void** wide, uint64_t limit, void* state)
{
    return wcsrtombs(text, (const wchar_t**)wide, (size_t)limit, (mbstate_t*)state);
}

uint64_t HostBridge::DecodeStringBounded(void* wide, const char** text, uint64_t available, uint64_t limit, void* state)
{
    return mbsnrtowcs((wchar_t*)wide, text, (size_t)available, (size_t)limit, (mbstate_t*)state);
}

uint64_t HostBridge::EncodeStringBounded(char* text, const void** wide, uint64_t available, uint64_t limit, void* state)
{
    return wcsnrtombs(text, (const wchar_t**)wide, (size_t)available, (size_t)limit, (mbstate_t*)state);
}

uint64_t HostBridge::DecodedLength(const char* text, uint64_t limit, void* state)
{
    return mbrlen(text, (size_t)limit, (mbstate_t*)state);
}

int32_t HostBridge::DecodeSingle(void* wide, const char* text, uint64_t limit)
{
    return mbtowc((wchar_t*)wide, text, (size_t)limit);
}

int32_t HostBridge::EncodeSingle(char* text, int32_t wide)
{
    return wctomb(text, (wchar_t)wide);
}

uint64_t HostBridge::DecodeWholeString(void* wide, const char* text, uint64_t limit)
{
    return mbstowcs((wchar_t*)wide, text, (size_t)limit);
}

uint64_t HostBridge::EncodeWholeString(char* text, const void* wide, uint64_t limit)
{
    return wcstombs(text, (const wchar_t*)wide, (size_t)limit);
}

int32_t HostBridge::IsDecodingComplete(const void* state)
{
    return mbsinit((const mbstate_t*)state);
}

int32_t HostBridge::ByteToWide(int32_t value)
{
    return btowc(value);
}

int32_t HostBridge::WideToByte(int32_t value)
{
    return wctob(value);
}

uint64_t HostBridge::MaximumCharacterBytes()
{
    return (uint64_t)MB_CUR_MAX;
}

void* HostBridge::CreateLocale(int32_t mask, const char* name, void* base)
{
    return (void*)newlocale(mask, name, AsLocale(base));
}

void HostBridge::ReleaseLocale(void* locale)
{
    if (locale) freelocale(AsLocale(locale));
}

void* HostBridge::DuplicateLocale(void* locale)
{
    return (void*)duplocale(AsLocale(locale));
}

void* HostBridge::UseLocale(void* locale)
{
    return (void*)uselocale(AsLocale(locale));
}

void* HostBridge::LocaleConventions()
{
    return (void*)localeconv();
}

char* HostBridge::SelectLocale(int32_t category, const char* name)
{
    return setlocale(category, name);
}

int32_t HostBridge::ClassifyWide(int32_t value, uint64_t classification)
{
    return iswctype((wint_t)value, (wctype_t)classification);
}

uint64_t HostBridge::LookupWideClass(const char* name)
{
    return (uint64_t)wctype(name);
}

int32_t HostBridge::WideToUpper(int32_t value)
{
    return (int32_t)towupper((wint_t)value);
}

int32_t HostBridge::WideToLower(int32_t value)
{
    return (int32_t)towlower((wint_t)value);
}

int32_t HostBridge::ToUpper(int32_t value)
{
    return toupper(value);
}

int32_t HostBridge::IsUpper(int32_t value)
{
    return isupper(value);
}

int32_t HostBridge::IsLower(int32_t value)
{
    return islower(value);
}

int32_t HostBridge::IsAlpha(int32_t value)
{
    return isalpha(value);
}

int32_t HostBridge::IsDigit(int32_t value)
{
    return isdigit(value);
}

int32_t HostBridge::IsAlphaNumeric(int32_t value)
{
    return isalnum(value);
}

int32_t HostBridge::IsPunctuation(int32_t value)
{
    return ispunct(value);
}

int32_t HostBridge::IsHexDigit(int32_t value)
{
    return isxdigit(value);
}

int32_t HostBridge::IsControl(int32_t value)
{
    return iscntrl(value);
}

int32_t HostBridge::IsPrintable(int32_t value)
{
    return isprint(value);
}

int32_t HostBridge::IsGraphical(int32_t value)
{
    return isgraph(value);
}

int32_t HostBridge::IsBlank(int32_t value)
{
    return isblank(value);
}

int32_t HostBridge::ToLower(int32_t value)
{
    return tolower(value);
}

int32_t HostBridge::IsSpace(int32_t value)
{
    return isspace(value);
}

int32_t HostBridge::IsWideAlphabetic(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswalpha((wint_t)value); });
}

int32_t HostBridge::IsWideBlank(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswblank((wint_t)value); });
}

int32_t HostBridge::IsWideControl(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswcntrl((wint_t)value); });
}

int32_t HostBridge::IsWideDigit(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswdigit((wint_t)value); });
}

int32_t HostBridge::IsWideLower(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswlower((wint_t)value); });
}

int32_t HostBridge::IsWidePrintable(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswprint((wint_t)value); });
}

int32_t HostBridge::IsWidePunctuation(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswpunct((wint_t)value); });
}

int32_t HostBridge::IsWideSpace(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswspace((wint_t)value); });
}

int32_t HostBridge::IsWideUpper(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswupper((wint_t)value); });
}

int32_t HostBridge::IsWideHexDigit(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)iswxdigit((wint_t)value); });
}

int32_t HostBridge::WideToUpperIn(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)towupper((wint_t)value); });
}

int32_t HostBridge::WideToLowerIn(int32_t value, void* locale)
{
    return WithLocale(locale, [&] { return (int32_t)towlower((wint_t)value); });
}

int32_t HostBridge::StringCollateIn(const char* left, const char* right, void* locale)
{
    return WithLocale(locale, [&] { return strcoll(left, right); });
}

uint64_t HostBridge::StringTransformIn(char* destination, const char* source, uint64_t count, void* locale)
{
    return WithLocale(locale, [&] { return (uint64_t)strxfrm(destination, source, (size_t)count); });
}

int32_t HostBridge::WideCollateIn(const void* left, const void* right, void* locale)
{
    return WithLocale(locale, [&] { return wcscoll((const wchar_t*)left, (const wchar_t*)right); });
}

uint64_t HostBridge::WideTransformIn(void* destination, const void* source, uint64_t count, void* locale)
{
    return WithLocale(locale, [&] { return (uint64_t)wcsxfrm((wchar_t*)destination, (const wchar_t*)source, (size_t)count); });
}

int64_t HostBridge::ParseSigned(const char* text, char** end, int32_t base)
{
    return strtoll(text, end, base);
}

uint64_t HostBridge::ParseUnsigned(const char* text, char** end, int32_t base)
{
    return strtoull(text, end, base);
}

double HostBridge::ParseDouble(const char* text, char** end)
{
    return strtod(text, end);
}

float HostBridge::ParseFloat(const char* text, char** end)
{
    return strtof(text, end);
}

double HostBridge::ParseExtended(const char* text, char** end)
{
    return (double)strtold(text, end);
}

int64_t HostBridge::ParseSignedIn(const char* text, char** end, int32_t base, void* locale)
{
    return WithLocale(locale, [&] { return (int64_t)strtoll(text, end, base); });
}

uint64_t HostBridge::ParseUnsignedIn(const char* text, char** end, int32_t base, void* locale)
{
    return WithLocale(locale, [&] { return (uint64_t)strtoull(text, end, base); });
}

double HostBridge::ParseExtendedIn(const char* text, char** end, void* locale)
{
    return WithLocale(locale, [&] { return (double)strtold(text, end); });
}

int64_t HostBridge::ParseWideSigned(const void* text, void** end, int32_t base)
{
    return wcstoll((const wchar_t*)text, (wchar_t**)end, base);
}

uint64_t HostBridge::ParseWideUnsigned(const void* text, void** end, int32_t base)
{
    return wcstoull((const wchar_t*)text, (wchar_t**)end, base);
}

double HostBridge::ParseWideDouble(const void* text, void** end)
{
    return wcstod((const wchar_t*)text, (wchar_t**)end);
}

float HostBridge::ParseWideFloat(const void* text, void** end)
{
    return wcstof((const wchar_t*)text, (wchar_t**)end);
}

double HostBridge::ParseWideExtended(const void* text, void** end)
{
    return (double)wcstold((const wchar_t*)text, (wchar_t**)end);
}

int32_t HostBridge::ParseInteger(const char* text)
{
    return atoi(text);
}

int64_t HostBridge::ParseLong(const char* text)
{
    return atol(text);
}

double HostBridge::ParseNumber(const char* text)
{
    return atof(text);
}

double HostBridge::SineOf(double value)
{
    return sin(value);
}

double HostBridge::CosineOf(double value)
{
    return cos(value);
}

double HostBridge::TangentOf(double value)
{
    return tan(value);
}

double HostBridge::ArcSineOf(double value)
{
    return asin(value);
}

double HostBridge::ArcCosineOf(double value)
{
    return acos(value);
}

double HostBridge::ArcTangentOf(double value)
{
    return atan(value);
}

double HostBridge::ArcTangentOfRatio(double y, double x)
{
    return atan2(y, x);
}

double HostBridge::ExponentialOf(double value)
{
    return exp(value);
}

double HostBridge::LogarithmOf(double value)
{
    return log(value);
}

double HostBridge::BinaryLogarithmOf(double value)
{
    return log2(value);
}

double HostBridge::DecimalLogarithmOf(double value)
{
    return log10(value);
}

double HostBridge::PowerOf(double base, double exponent)
{
    return pow(base, exponent);
}

double HostBridge::SquareRootOf(double value)
{
    return sqrt(value);
}

double HostBridge::CubeRootOf(double value)
{
    return cbrt(value);
}

double HostBridge::RoundUp(double value)
{
    return ceil(value);
}

double HostBridge::RoundDown(double value)
{
    return floor(value);
}

double HostBridge::RoundNearest(double value)
{
    return round(value);
}

double HostBridge::RoundTowardZero(double value)
{
    return trunc(value);
}

double HostBridge::RoundToInteger(double value)
{
    return nearbyint(value);
}

double HostBridge::Remainder(double left, double right)
{
    return fmod(left, right);
}

double HostBridge::Magnitude(double x, double y)
{
    return hypot(x, y);
}

double HostBridge::HyperbolicSine(double value)
{
    return sinh(value);
}

double HostBridge::HyperbolicCosine(double value)
{
    return cosh(value);
}

double HostBridge::HyperbolicTangent(double value)
{
    return tanh(value);
}

double HostBridge::WithSignOf(double value, double sign)
{
    return copysign(value, sign);
}

double HostBridge::ScaleByPowerOfTwo(double value, int32_t exponent)
{
    return ldexp(value, exponent);
}

double HostBridge::SplitExponent(double value, int32_t* exponent)
{
    int produced = 0;
    double fraction = frexp(value, &produced);
    if (exponent) *exponent = produced;

    return fraction;
}

double HostBridge::SplitFraction(double value, double* whole)
{
    return modf(value, whole);
}

double HostBridge::UnbiasedExponent(double value)
{
    return logb(value);
}

void HostBridge::SineAndCosine(double value, double* sine, double* cosine)
{
    if (sine) *sine = sin(value);
    if (cosine) *cosine = cos(value);
}

float HostBridge::SineOfNarrow(float value)
{
    return sinf(value);
}

float HostBridge::CosineOfNarrow(float value)
{
    return cosf(value);
}

float HostBridge::TangentOfNarrow(float value)
{
    return tanf(value);
}

float HostBridge::ArcSineOfNarrow(float value)
{
    return asinf(value);
}

float HostBridge::ArcCosineOfNarrow(float value)
{
    return acosf(value);
}

float HostBridge::ArcTangentOfNarrow(float value)
{
    return atanf(value);
}

float HostBridge::ArcTangentOfRatioNarrow(float y, float x)
{
    return atan2f(y, x);
}

float HostBridge::ExponentialOfNarrow(float value)
{
    return expf(value);
}

float HostBridge::BinaryExponentialNarrow(float value)
{
    return exp2f(value);
}

float HostBridge::LogarithmOfNarrow(float value)
{
    return logf(value);
}

float HostBridge::BinaryLogarithmNarrow(float value)
{
    return log2f(value);
}

float HostBridge::DecimalLogarithmNarrow(float value)
{
    return log10f(value);
}

float HostBridge::PowerOfNarrow(float base, float exponent)
{
    return powf(base, exponent);
}

float HostBridge::SquareRootOfNarrow(float value)
{
    return sqrtf(value);
}

float HostBridge::CubeRootOfNarrow(float value)
{
    return cbrtf(value);
}

float HostBridge::RemainderNarrow(float left, float right)
{
    return fmodf(left, right);
}

float HostBridge::MagnitudeNarrow(float x, float y)
{
    return hypotf(x, y);
}

float HostBridge::HyperbolicTangentNarrow(float value)
{
    return tanhf(value);
}

float HostBridge::ScaleByPowerOfTwoNarrow(float value, int32_t exponent)
{
    return ldexpf(value, exponent);
}

float HostBridge::SplitFractionNarrow(float value, float* whole)
{
    return modff(value, whole);
}

float HostBridge::RoundToEvenNarrow(float value)
{
    return nearbyintf(value);
}

void HostBridge::SineAndCosineNarrow(float value, float* sine, float* cosine)
{
    if (sine) *sine = sinf(value);
    if (cosine) *cosine = cosf(value);
}

int32_t HostBridge::AbsoluteInteger(int32_t value)
{
    return value < 0 ? -value : value;
}

int64_t HostBridge::AbsoluteLong(int64_t value)
{
    return value < 0 ? -value : value;
}

void HostBridge::SortArray(void* base, uint64_t count, uint64_t width, void* comparator)
{
    if (!base || !comparator || count < 2 || width == 0) return;

    qsort(base, (size_t)count, (size_t)width, (int (*)(const void*, const void*))comparator);
}

void* HostBridge::SearchArray(const void* key, const void* base, uint64_t count, uint64_t width, void* comparator)
{
    if (!key || !base || !comparator || width == 0) return nullptr;

    return bsearch(key, base, (size_t)count, (size_t)width, (int (*)(const void*, const void*))comparator);
}

void HostBridge::SeedRandom(int64_t seed)
{
    srand48((long)seed);
}

int64_t HostBridge::NextRandom()
{
    return (int64_t)lrand48();
}

int32_t HostBridge::OpenFile(const char* path, int32_t flags, int32_t mode)
{
    const std::string* synthetic = SyntheticSystemFile(path);
    if (synthetic) return OpenSyntheticDescriptor(*synthetic);

    return open(GuestPath(path), flags, (mode_t)mode);
}

int32_t HostBridge::CloseDescriptor(int32_t descriptor)
{
    return close(descriptor);
}

int64_t HostBridge::ReadDescriptor(int32_t descriptor, void* buffer, uint64_t count)
{
    return read(descriptor, buffer, (size_t)count);
}

int64_t HostBridge::WriteDescriptor(int32_t descriptor, const void* buffer, uint64_t count)
{
    return write(descriptor, buffer, (size_t)count);
}

int64_t HostBridge::SeekDescriptor(int32_t descriptor, int64_t offset, int32_t origin)
{
    return lseek(descriptor, (off_t)offset, origin);
}

int32_t HostBridge::CheckAccess(const char* path, int32_t mode)
{
    return access(GuestPath(path), mode);
}

int32_t HostBridge::DuplicateDescriptor(int32_t descriptor)
{
    return dup(descriptor);
}

int32_t HostBridge::CreatePipe(int32_t* descriptors)
{
    return pipe(descriptors);
}

struct GuestStatus
{
    uint64_t device;
    uint64_t inode;
    uint32_t mode;
    uint32_t linkCount;
    uint32_t userId;
    uint32_t groupId;
    uint64_t representedDevice;
    uint64_t firstPadding;
    int64_t size;
    int32_t blockSize;
    int32_t secondPadding;
    int64_t blockCount;
    int64_t accessedSeconds;
    uint64_t accessedNanoseconds;
    int64_t modifiedSeconds;
    uint64_t modifiedNanoseconds;
    int64_t changedSeconds;
    uint64_t changedNanoseconds;
    uint32_t firstUnused;
    uint32_t secondUnused;
};

static_assert(sizeof(GuestStatus) == 128, "the guest struct stat is 128 bytes on arm64");

static void StoreGuestStatus(const struct stat& source, void* out)
{
    GuestStatus described;
    memset(&described, 0, sizeof(described));

    described.device = (uint64_t)source.st_dev;
    described.inode = (uint64_t)source.st_ino;
    described.mode = (uint32_t)source.st_mode;
    described.linkCount = (uint32_t)source.st_nlink;
    described.userId = (uint32_t)source.st_uid;
    described.groupId = (uint32_t)source.st_gid;
    described.representedDevice = (uint64_t)source.st_rdev;
    described.size = (int64_t)source.st_size;
    described.blockSize = (int32_t)source.st_blksize;
    described.blockCount = (int64_t)source.st_blocks;
    described.accessedSeconds = (int64_t)source.st_atim.tv_sec;
    described.accessedNanoseconds = (uint64_t)source.st_atim.tv_nsec;
    described.modifiedSeconds = (int64_t)source.st_mtim.tv_sec;
    described.modifiedNanoseconds = (uint64_t)source.st_mtim.tv_nsec;
    described.changedSeconds = (int64_t)source.st_ctim.tv_sec;
    described.changedNanoseconds = (uint64_t)source.st_ctim.tv_nsec;

    memcpy(out, &described, sizeof(described));
}

int32_t HostBridge::DescribePath(const char* path, void* out)
{
    struct stat described;
    int32_t result = stat(GuestPath(path), &described);
    if (result == 0 && out) StoreGuestStatus(described, out);

    return result;
}

int32_t HostBridge::DescribeLink(const char* path, void* out)
{
    struct stat described;
    int32_t result = lstat(GuestPath(path), &described);
    if (result == 0 && out) StoreGuestStatus(described, out);

    return result;
}

int32_t HostBridge::DescribeDescriptor(int32_t descriptor, void* out)
{
    struct stat described;
    int32_t result = fstat(descriptor, &described);
    if (result == 0 && out) StoreGuestStatus(described, out);

    return result;
}

int64_t HostBridge::ReadLinkTarget(const char* path, char* buffer, uint64_t size)
{
    return readlink(GuestPath(path), buffer, (size_t)size);
}

char* HostBridge::ResolvePath(const char* path, char* resolved)
{
    return realpath(GuestPath(path), resolved);
}

char* HostBridge::WorkingDirectory(char* buffer, uint64_t size)
{
    return getcwd(buffer, (size_t)size);
}

int32_t HostBridge::RemovePath(const char* path)
{
    return unlink(GuestPath(path));
}

int32_t HostBridge::RemoveEntry(const char* path)
{
    return remove(GuestPath(path));
}

int32_t HostBridge::RenamePath(const char* from, const char* to)
{
    return rename(GuestPath(from), GuestPath(to));
}

int32_t HostBridge::CreateDirectory(const char* path, int32_t mode)
{
    return mkdir(GuestPath(path), (mode_t)mode);
}

int32_t HostBridge::RemoveDirectory(const char* path)
{
    return rmdir(GuestPath(path));
}

void* HostBridge::OpenDirectory(const char* path)
{
    return (void*)opendir(GuestPath(path));
}

void* HostBridge::ReadDirectory(void* directory)
{
    return (void*)readdir((DIR*)directory);
}

int32_t HostBridge::CloseDirectory(void* directory)
{
    return closedir((DIR*)directory);
}

int32_t HostBridge::TruncatePath(const char* path, int64_t length)
{
    return truncate(GuestPath(path), (off_t)length);
}

int32_t HostBridge::TruncateDescriptor(int32_t descriptor, int64_t length)
{
    return ftruncate(descriptor, (off_t)length);
}

int32_t HostBridge::ChangeMode(const char* path, int32_t mode)
{
    return chmod(GuestPath(path), (mode_t)mode);
}

int32_t HostBridge::ChangeDescriptorMode(int32_t descriptor, int32_t mode)
{
    return fchmod(descriptor, (mode_t)mode);
}

int32_t HostBridge::CreateSymbolicLink(const char* target, const char* path)
{
    return symlink(GuestPath(target), GuestPath(path));
}

int32_t HostBridge::CreateHardLink(const char* target, const char* path)
{
    return link(GuestPath(target), GuestPath(path));
}

int32_t HostBridge::IsTerminal(int32_t descriptor)
{
    return isatty(descriptor);
}

char* HostBridge::BaseName(char* path)
{
    return basename(path);
}

int32_t HostBridge::MatchPattern(const char* pattern, const char* text, int32_t flags)
{
    return fnmatch(pattern, text, flags);
}

int32_t HostBridge::DescribeFileSystem(const char* path, void* out)
{
    return statfs(GuestPath(path), (struct statfs*)out);
}

int32_t HostBridge::SetTimestamps(const char* path, const void* times)
{
    return utimes(GuestPath(path), (const struct timeval*)times);
}

int32_t HostBridge::LockDescriptor(int32_t descriptor, int32_t operation)
{
    return (int32_t)syscall(SYS_flock, descriptor, operation);
}

int32_t HostBridge::PollDescriptors(void* descriptors, uint64_t count, int32_t timeout)
{
    return poll((struct pollfd*)descriptors, (nfds_t)count, timeout);
}

void HostBridge::SetGuestMemoryRange(uint64_t base, uint64_t length)
{
    std::lock_guard<std::mutex> guard(guestMappingMutex);

    guestMemoryBase = base;
    guestMemoryLength = length;
}

static std::atomic<HostBridge::CodeInvalidator> codeInvalidator{nullptr};
static std::atomic<void*> codeInvalidatorContext{nullptr};

void HostBridge::SetCodeInvalidator(CodeInvalidator invalidator, void* context)
{
    codeInvalidatorContext.store(context, std::memory_order_relaxed);
    codeInvalidator.store(invalidator, std::memory_order_release);
}

static void InvalidateGuestCode(uint64_t address, uint64_t length)
{
    HostBridge::CodeInvalidator invalidator = codeInvalidator.load(std::memory_order_acquire);
    if (invalidator) invalidator(codeInvalidatorContext.load(std::memory_order_relaxed), address, length);
}

void* HostBridge::MapMemory(void* address, uint64_t length, int32_t protection, int32_t flags, int32_t descriptor, int64_t offset)
{
    bool placed = address != nullptr && (flags & (MAP_FIXED | MAP_FIXED_NOREPLACE)) != 0;

    if (placed && !GuestOwns((uint64_t)address, length))
    {
        RefuseGuestMemoryCall("mmap", (uint64_t)address, length);
        errno = EINVAL;
        return MAP_FAILED;
    }

    void* mapped = mmap(address, (size_t)length, protection, flags, descriptor, (off_t)offset);
    if (mapped == MAP_FAILED) return mapped;

    RememberGuestMapping((uint64_t)mapped, length);
    return mapped;
}

int32_t HostBridge::UnmapMemory(void* address, uint64_t length)
{
    if (!GuestOwns((uint64_t)address, length))
    {
        RefuseGuestMemoryCall("munmap", (uint64_t)address, length);
        errno = EINVAL;
        return -1;
    }

    int32_t released = munmap(address, (size_t)length);
    if (released == 0) ForgetGuestMapping((uint64_t)address, length);
    if (released == 0) InvalidateGuestCode((uint64_t)address, length);

    return released;
}

int32_t HostBridge::ProtectMemory(void* address, uint64_t length, int32_t protection)
{
    if (!GuestOwns((uint64_t)address, length))
    {
        RefuseGuestMemoryCall("mprotect", (uint64_t)address, length);
        errno = EINVAL;
        return -1;
    }

    int32_t changed = mprotect(address, (size_t)length, protection);
    if (changed == 0 && (protection & PROT_EXEC) != 0) InvalidateGuestCode((uint64_t)address, length);

    return changed;
}

int32_t HostBridge::AdviseMemory(void* address, uint64_t length, int32_t advice)
{
    if (!GuestOwns((uint64_t)address, length))
    {
        RefuseGuestMemoryCall("madvise", (uint64_t)address, length);
        errno = EINVAL;
        return -1;
    }

    return madvise(address, (size_t)length, advice);
}

int32_t HostBridge::ProcessControl(int32_t option, uint64_t first, uint64_t second, uint64_t third)
{
    return prctl(option, (unsigned long)first, (unsigned long)second, (unsigned long)third, 0ul);
}

void* HostBridge::OpenStream(const char* path, const char* mode)
{
    const std::string* synthetic = SyntheticSystemFile(path);
    if (synthetic) return (void*)fmemopen((void*)synthetic->data(), synthetic->size(), "r");

    return (void*)fopen(GuestPath(path), mode);
}

void* HostBridge::AdoptDescriptor(int32_t descriptor, const char* mode)
{
    return (void*)fdopen(descriptor, mode);
}

int32_t HostBridge::CloseStream(void* stream)
{
    return fclose((FILE*)stream);
}

uint64_t HostBridge::ReadStream(void* buffer, uint64_t size, uint64_t count, void* stream)
{
    return fread(buffer, (size_t)size, (size_t)count, (FILE*)stream);
}

uint64_t HostBridge::WriteStream(const void* buffer, uint64_t size, uint64_t count, void* stream)
{
    return fwrite(buffer, (size_t)size, (size_t)count, (FILE*)stream);
}

int32_t HostBridge::SeekStream(void* stream, int64_t offset, int32_t origin)
{
    return fseeko((FILE*)stream, (off_t)offset, origin);
}

int64_t HostBridge::StreamPosition(void* stream)
{
    return (int64_t)ftello((FILE*)stream);
}

char* HostBridge::ReadStreamLine(char* buffer, int32_t size, void* stream)
{
    return fgets(buffer, size, (FILE*)stream);
}

int32_t HostBridge::ReadStreamCharacter(void* stream)
{
    return fgetc((FILE*)stream);
}

int32_t HostBridge::WriteStreamCharacter(int32_t value, void* stream)
{
    return fputc(value, (FILE*)stream);
}

int32_t HostBridge::WriteStreamText(const char* text, void* stream)
{
    return fputs(text, (FILE*)stream);
}

int32_t HostBridge::StreamAtEnd(void* stream)
{
    return feof((FILE*)stream);
}

int32_t HostBridge::StreamHasError(void* stream)
{
    return ferror((FILE*)stream);
}

void HostBridge::ClearStreamError(void* stream)
{
    clearerr((FILE*)stream);
}

int32_t HostBridge::StreamDescriptor(void* stream)
{
    return fileno((FILE*)stream);
}

void HostBridge::SetStreamBuffer(void* stream, char* buffer)
{
    setbuf((FILE*)stream, buffer);
}

int32_t HostBridge::FlushStream(void* stream)
{
    return fflush((FILE*)stream);
}

int64_t HostBridge::CurrentTime(int64_t* out)
{
    time_t produced = time(nullptr);
    if (out) *out = (int64_t)produced;

    return (int64_t)produced;
}

int64_t HostBridge::ProcessorTime()
{
    return (int64_t)clock();
}

double HostBridge::TimeDifference(int64_t later, int64_t earlier)
{
    return difftime((time_t)later, (time_t)earlier);
}

int64_t HostBridge::ComposeTime(void* parts)
{
    return (int64_t)mktime((struct tm*)parts);
}

void* HostBridge::SplitUniversalTime(const int64_t* value)
{
    time_t seconds = value ? (time_t)*value : 0;
    return (void*)gmtime(&seconds);
}

void* HostBridge::SplitUniversalTimeInto(const int64_t* value, void* parts)
{
    time_t seconds = value ? (time_t)*value : 0;
    return (void*)gmtime_r(&seconds, (struct tm*)parts);
}

void* HostBridge::SplitLocalTime(const int64_t* value)
{
    time_t seconds = value ? (time_t)*value : 0;
    return (void*)localtime(&seconds);
}

void* HostBridge::SplitLocalTimeInto(const int64_t* value, void* parts)
{
    time_t seconds = value ? (time_t)*value : 0;
    return (void*)localtime_r(&seconds, (struct tm*)parts);
}

uint64_t HostBridge::FormatTime(char* buffer, uint64_t size, const char* format, const void* parts)
{
    return strftime(buffer, (size_t)size, format, (const struct tm*)parts);
}

int32_t HostBridge::ReadClock(int32_t clock, void* out)
{
    return clock_gettime((clockid_t)clock, (struct timespec*)out);
}

int32_t HostBridge::ReadClockResolution(int32_t clock, void* out)
{
    return clock_getres((clockid_t)clock, (struct timespec*)out);
}

int32_t HostBridge::ReadWallClock(void* out, void* zone)
{
    return gettimeofday((struct timeval*)out, (struct timezone*)zone);
}

int32_t HostBridge::SleepPrecisely(const void* span, void* remaining)
{
    return nanosleep((const struct timespec*)span, (struct timespec*)remaining);
}

int32_t HostBridge::SleepMicroseconds(uint64_t span)
{
    return usleep((useconds_t)span);
}

int32_t HostBridge::YieldProcessor()
{
    return sched_yield();
}

int32_t HostBridge::SetPathTimes(const char* path, const void* times)
{
    return utime(GuestPath(path), (const struct utimbuf*)times);
}

int32_t HostBridge::ThreadId()
{
    return (int32_t)syscall(SYS_gettid);
}

int32_t HostBridge::ProcessId()
{
    return (int32_t)getpid();
}

int32_t HostBridge::UserId()
{
    return (int32_t)getuid();
}

int32_t HostBridge::EffectiveUserId()
{
    return (int32_t)geteuid();
}

int32_t HostBridge::EffectiveGroupId()
{
    return (int32_t)getegid();
}

uint64_t HostBridge::CurrentThread()
{
    return (uint64_t)pthread_self();
}

int32_t HostBridge::ThreadsEqual(uint64_t left, uint64_t right)
{
    return left == right ? 1 : 0;
}

int32_t HostBridge::ReadPriority(int32_t which, int32_t who)
{
    return getpriority((__priority_which_t)which, (id_t)who);
}

int32_t HostBridge::WritePriority(int32_t which, int32_t who, int32_t priority)
{
    return setpriority((__priority_which_t)which, (id_t)who, priority);
}

int32_t HostBridge::PageSize()
{
    return (int32_t)sysconf(_SC_PAGESIZE);
}

struct ConfigurationName
{
    int32_t guest;
    int32_t host;
};

static const ConfigurationName CONFIGURATION_NAMES[] = {
    {0x0000, _SC_ARG_MAX},
    {0x0005, _SC_CHILD_MAX},
    {0x0006, _SC_CLK_TCK},
    {0x000a, _SC_NGROUPS_MAX},
    {0x000b, _SC_OPEN_MAX},
    {0x0017, _SC_JOB_CONTROL},
    {0x0018, _SC_SAVED_IDS},
    {0x0019, _SC_VERSION},
    {0x001b, _SC_STREAM_MAX},
    {0x001c, _SC_TZNAME_MAX},
    {0x0025, _SC_ATEXIT_MAX},
    {0x0026, _SC_IOV_MAX},
    {0x0027, _SC_PAGESIZE},
    {0x0028, _SC_PAGESIZE},
    {0x0036, _SC_SEM_VALUE_MAX},
    {0x0047, _SC_GETGR_R_SIZE_MAX},
    {0x0048, _SC_GETPW_R_SIZE_MAX},
    {0x0049, _SC_LOGIN_NAME_MAX},
    {0x004a, _SC_THREAD_DESTRUCTOR_ITERATIONS},
    {0x004b, _SC_THREAD_KEYS_MAX},
    {0x004c, _SC_THREAD_STACK_MIN},
    {0x004d, _SC_THREAD_THREADS_MAX},
    {0x004e, _SC_TTY_NAME_MAX},
    {0x004f, _SC_THREADS},
    {0x0060, _SC_NPROCESSORS_CONF},
    {0x0061, _SC_NPROCESSORS_ONLN},
    {0x0062, _SC_PHYS_PAGES},
    {0x0063, _SC_AVPHYS_PAGES},
    {0x0064, _SC_MONOTONIC_CLOCK},
    {0x006e, _SC_CPUTIME},
    {0x008f, _SC_LEVEL1_ICACHE_SIZE},
    {0x0090, _SC_LEVEL1_ICACHE_ASSOC},
    {0x0091, _SC_LEVEL1_ICACHE_LINESIZE},
    {0x0092, _SC_LEVEL1_DCACHE_SIZE},
    {0x0093, _SC_LEVEL1_DCACHE_ASSOC},
    {0x0094, _SC_LEVEL1_DCACHE_LINESIZE},
};

int64_t HostBridge::ProcessorCount()
{
    static const int64_t reported = (int64_t)SYNTHETIC_PROCESSOR_COUNT;

    return reported;
}

int64_t HostBridge::SystemConfiguration(int32_t name)
{
    if (name == 0x0060 || name == 0x0061) return ProcessorCount();

    for (const ConfigurationName& entry : CONFIGURATION_NAMES)
    {
        if (entry.guest != name) continue;

        return (int64_t)sysconf(entry.host);
    }

    errno = EINVAL;
    return -1;
}

int32_t HostBridge::CreateSemaphore(void* semaphore, int32_t shared, uint32_t value)
{
    return sem_init((sem_t*)semaphore, shared, value);
}

int32_t HostBridge::DestroySemaphore(void* semaphore)
{
    return sem_destroy((sem_t*)semaphore);
}

int32_t HostBridge::PostSemaphore(void* semaphore)
{
    return sem_post((sem_t*)semaphore);
}

int32_t HostBridge::WaitSemaphore(void* semaphore)
{
    return sem_wait((sem_t*)semaphore);
}

int32_t HostBridge::WaitSemaphoreUntil(void* semaphore, const void* deadline)
{
    return sem_timedwait((sem_t*)semaphore, (const struct timespec*)deadline);
}

int32_t HostBridge::ReadSemaphore(void* semaphore, int32_t* value)
{
    return sem_getvalue((sem_t*)semaphore, value);
}

int32_t HostBridge::ClearSignalSet(void* set)
{
    return sigemptyset((sigset_t*)set);
}

int32_t HostBridge::FillSignalSet(void* set)
{
    return sigfillset((sigset_t*)set);
}

int32_t HostBridge::AddSignal(void* set, int32_t number)
{
    return sigaddset((sigset_t*)set, number);
}

int32_t HostBridge::RemoveSignal(void* set, int32_t number)
{
    return sigdelset((sigset_t*)set, number);
}

int32_t HostBridge::SetEnvironmentValue(const char* name, const char* value, int32_t overwrite)
{
    return setenv(name, value, overwrite);
}

int32_t HostBridge::ClearEnvironmentValue(const char* name)
{
    return unsetenv(name);
}

int32_t HostBridge::ControlDescriptor(int32_t descriptor, int32_t command, int64_t argument)
{
    return fcntl(descriptor, command, (long)argument);
}

int32_t HostBridge::WriteLine(const char* text)
{
    return puts(text);
}

void* HostBridge::ErrorLocation()
{
    return (void*)__errno_location();
}

int32_t HostBridge::OpenFileAt(int32_t directory, const char* path, int32_t flags, int32_t mode)
{
    return openat(directory, GuestPath(path), flags, (mode_t)mode);
}

int32_t HostBridge::RemovePathAt(int32_t directory, const char* path, int32_t flags)
{
    return unlinkat(directory, GuestPath(path), flags);
}

int32_t HostBridge::ChangeModeAt(int32_t directory, const char* path, int32_t mode, int32_t flags)
{
    return fchmodat(directory, GuestPath(path), (mode_t)mode, flags);
}

int32_t HostBridge::SetTimesAt(int32_t directory, const char* path, const void* times, int32_t flags)
{
    return utimensat(directory, GuestPath(path), (const struct timespec*)times, flags);
}

int32_t HostBridge::SetDescriptorTimes(int32_t descriptor, const void* times)
{
    return futimens(descriptor, (const struct timespec*)times);
}

void* HostBridge::OpenDirectoryDescriptor(int32_t descriptor)
{
    return fdopendir(descriptor);
}

int32_t HostBridge::ChangeWorkingDirectory(const char* path)
{
    return chdir(GuestPath(path));
}

int32_t HostBridge::DescribeVolume(const char* path, void* out)
{
    return statvfs(GuestPath(path), (struct statvfs*)out);
}

int64_t HostBridge::PathLimit(const char* path, int32_t name)
{
    return pathconf(GuestPath(path), name);
}

int64_t HostBridge::TransferFile(int32_t destination, int32_t source, void* offset, uint64_t count)
{
    return sendfile(destination, source, (off_t*)offset, (size_t)count);
}

int32_t HostBridge::WaitForDescriptors(int32_t limit, void* readable, void* writable, void* failing, void* timeout)
{
    return select(limit, (fd_set*)readable, (fd_set*)writable, (fd_set*)failing, (struct timeval*)timeout);
}

int32_t HostBridge::RaiseSignal(int32_t number)
{
    return raise(number);
}

int32_t HostBridge::SuspendForSignals(const void* mask)
{
    return sigsuspend((const sigset_t*)mask);
}

uint64_t HostBridge::DivideIntegers(int32_t numerator, int32_t denominator)
{
    div_t divided = div(numerator, denominator);
    return (uint64_t)(uint32_t)divided.quot | ((uint64_t)(uint32_t)divided.rem << 32);
}

char* HostBridge::DescribeErrorReturning(int32_t number, char* buffer, uint64_t size)
{
    if (!buffer || size == 0) return nullptr;

    const char* text = strerror(number);
    if (!text) text = "Unknown error";

    size_t length = strlen(text);
    if (length >= size) length = size - 1;

    memcpy(buffer, text, length);
    buffer[length] = '\0';
    return buffer;
}

uint64_t HostBridge::FormatTimeIn(char* buffer, uint64_t size, const char* format, const void* parts, void* locale)
{
    return WithLocale(locale, [&]() { return (uint64_t)strftime(buffer, (size_t)size, format, (const struct tm*)parts); });
}

void* HostBridge::PasswordEntry(uint32_t user)
{
    return getpwuid((uid_t)user);
}

int32_t HostBridge::PasswordEntryInto(uint32_t user, void* entry, char* buffer, uint64_t size, void** result)
{
    return getpwuid_r((uid_t)user, (struct passwd*)entry, buffer, (size_t)size, (struct passwd**)result);
}

int32_t HostBridge::StreamPushBack(int32_t value, void* stream)
{
    return ungetc(value, (FILE*)stream);
}

int32_t HostBridge::WriteWideCharacter(int32_t value, void* stream)
{
    return (int32_t)fputwc((wchar_t)value, (FILE*)stream);
}

int32_t HostBridge::ReadWideCharacter(void* stream)
{
    return (int32_t)getwc((FILE*)stream);
}

int32_t HostBridge::WidePushBack(int32_t value, void* stream)
{
    return (int32_t)ungetwc((wint_t)value, (FILE*)stream);
}

void HostBridge::MarkDescriptorSet(int32_t descriptor, void* set, uint64_t size)
{
    if (!set || descriptor < 0) return;

    uint64_t index = (uint64_t)descriptor / 64;
    if ((index + 1) * sizeof(uint64_t) > size) return;

    ((uint64_t*)set)[index] |= 1ull << ((uint64_t)descriptor % 64);
}

int32_t HostBridge::DescriptorIsSet(int32_t descriptor, const void* set, uint64_t size)
{
    if (!set || descriptor < 0) return 0;

    uint64_t index = (uint64_t)descriptor / 64;
    if ((index + 1) * sizeof(uint64_t) > size) return 0;

    return (((const uint64_t*)set)[index] >> ((uint64_t)descriptor % 64)) & 1ull ? 1 : 0;
}

uint32_t HostBridge::AddressFromText(const char* text)
{
    return (uint32_t)inet_addr(text);
}

const char* HostBridge::AddressToText(int32_t family, const void* address, char* buffer, uint32_t size)
{
    return inet_ntop(family, address, buffer, (socklen_t)size);
}

int32_t HostBridge::AddressFromTextInto(int32_t family, const char* text, void* address)
{
    return inet_pton(family, text, address);
}

uint32_t HostBridge::InterfaceIndex(const char* name)
{
    return if_nametoindex(name);
}

int32_t HostBridge::DecompressInitialize(void* stream, int32_t windowBits, const char* version, int32_t size)
{
    return inflateInit2_((z_streamp)stream, windowBits, version, size);
}

int32_t HostBridge::Decompress(void* stream, int32_t flush)
{
    return inflate((z_streamp)stream, flush);
}

int32_t HostBridge::DecompressEnd(void* stream)
{
    return inflateEnd((z_streamp)stream);
}

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <string>
#include <sys/stat.h>
#include <unistd.h>

#include "GuestLibc/GuestLibc.hpp"
#include "GuestLibc/HostBridge.hpp"
#include "Platform/DeviceIdentity.hpp"

static int failures = 0;

static void Expect(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static constexpr const char* DEVICE_FINGERPRINT = "samsung/dm2qxxx/dm2q:14/UP1A.231005.007/S916BXXS6CXI4:user/release-keys";

static std::string Separators(const std::string& text)
{
    std::string shape;
    for (char letter : text)
    {
        if (letter == '/' || letter == ':') shape += letter;
    }

    return shape;
}

static void CheckDeviceIdentity()
{
    std::string fingerprint = Platform::BUILD_FINGERPRINT;

    Expect(fingerprint.find(' ') == std::string::npos, "the fingerprint carries no spaces");

    Expect(Separators(fingerprint) == Separators(DEVICE_FINGERPRINT),
           "the fingerprint separates its parts exactly as a real device does");

    std::string expected = std::string(Platform::DEVICE_BRAND) + "/" + Platform::DEVICE_PRODUCT + "/" + Platform::DEVICE_NAME + ":" +
                           Platform::DECLARED_RELEASE + "/" + Platform::BUILD_IDENTIFIER + "/" + Platform::BUILD_INCREMENTAL + ":" +
                           Platform::BUILD_TYPE + "/" + Platform::BUILD_TAGS;

    Expect(fingerprint == expected, "the fingerprint is built from the same parts it reports separately");

    Expect(std::string(Platform::DEVICE_BRAND).find("acl") != std::string::npos, "the brand is this runtime, not a real vendor");
    Expect(std::string(Platform::BUILD_TAGS) == "release-keys", "the build is tagged the way a shipped device is");
    Expect(std::string(Platform::BUILD_TYPE) == "user", "the build type is the one a shipped device reports");

    std::string patch = Platform::SECURITY_PATCH;
    bool dated = patch.size() == 10 && patch[4] == '-' && patch[7] == '-';
    Expect(dated, "the security patch level is a full date, which is the only form that parses");

    std::string identifier = Platform::BUILD_IDENTIFIER;
    Expect(identifier.find('.') != std::string::npos && identifier.find(' ') == std::string::npos,
           "the build identifier is dotted and unspaced, as the platform writes it");
}

static void CheckAndroidTree()
{
    char directoryTemplate[] = "/tmp/acl-tree-XXXXXX";
    const char* base = mkdtemp(directoryTemplate);
    if (!base)
    {
        printf("FAIL the android tree needs a work directory\n");
        failures += 1;
        return;
    }

    std::string data = std::string(base) + "/data";
    std::string external = std::string(base) + "/external";
    mkdir(data.c_str(), 0755);
    mkdir(external.c_str(), 0755);

    HostBridge::MapAndroidTree(data.c_str(), external.c_str(), "/usr/share", "com.example.probe");

    int written = HostBridge::OpenFile("/data/data/com.example.probe/saved.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644);
    Expect(written >= 0, "the guest can create a file under its android data directory");
    if (written >= 0) close(written);

    struct stat details;
    Expect(stat((data + "/saved.txt").c_str(), &details) == 0, "the file landed in the host directory behind it");

    Expect(HostBridge::CheckAccess("/data/user/0/com.example.probe/saved.txt", F_OK) == 0,
           "the same file is reachable through the other android name for it");

    unsigned char described[512];
    Expect(HostBridge::DescribePath("/sdcard", described) == 0, "external storage resolves rather than reading as absent");

    Expect(HostBridge::OpenFile("/data/data/com.other.app/saved.txt", O_RDONLY, 0) < 0,
           "another package's data directory does not resolve");

    HostBridge::RemovePath("/data/data/com.example.probe/saved.txt");
    Expect(stat((data + "/saved.txt").c_str(), &details) != 0, "removing through the android name removed the host file");

    HostBridge::MapAndroidTree("", "", "", "");
    Expect(HostBridge::OpenFile("/data/data/com.example.probe/saved.txt", O_RDONLY, 0) < 0,
           "with no tree mapped the android path is not invented");

    rmdir(data.c_str());
    rmdir(external.c_str());
    rmdir(base);
}

static void ExpectPlan(const char* format, int conversions, int integers, int floats, const char* label)
{
    GuestLibc::FormatPlan plan;
    GuestLibc::Result parsed = GuestLibc::ParseFormat(format, plan);

    if (parsed != GuestLibc::Result::Ok)
    {
        printf("FAIL %s: ParseFormat returned %s\n", label, GuestLibc::ResultName(parsed));
        failures += 1;
        return;
    }

    if (plan.conversionCount != conversions || plan.integerCount != integers || plan.floatCount != floats)
    {
        printf("FAIL %s: expected %d conversions (%d integer, %d float), got %d (%d integer, %d float)\n", label, conversions, integers,
               floats, plan.conversionCount, plan.integerCount, plan.floatCount);
        failures += 1;
        return;
    }

    printf("PASS %s\n", label);
}

static void ExpectClasses(const char* format, const GuestLibc::ConversionClass* expected, int count, const char* label)
{
    GuestLibc::FormatPlan plan;
    GuestLibc::Result parsed = GuestLibc::ParseFormat(format, plan);

    if (parsed != GuestLibc::Result::Ok || plan.conversionCount != count)
    {
        printf("FAIL %s: ParseFormat returned %s with %d conversions\n", label, GuestLibc::ResultName(parsed), plan.conversionCount);
        failures += 1;
        return;
    }

    for (int index = 0; index < count; ++index)
    {
        if (plan.conversions[index] == expected[index]) continue;

        printf("FAIL %s: conversion %d is %s, expected %s\n", label, index, GuestLibc::ConversionClassName(plan.conversions[index]),
               GuestLibc::ConversionClassName(expected[index]));
        failures += 1;
        return;
    }

    printf("PASS %s\n", label);
}

static void ExpectRejected(const char* format, GuestLibc::Result expected, const char* label)
{
    GuestLibc::FormatPlan plan;
    GuestLibc::Result parsed = GuestLibc::ParseFormat(format, plan);

    if (parsed == expected)
    {
        printf("PASS %s -> %s\n", label, GuestLibc::ResultName(parsed));
        return;
    }

    printf("FAIL %s: expected %s, got %s\n", label, GuestLibc::ResultName(expected), GuestLibc::ResultName(parsed));
    failures += 1;
}

static constexpr int32_t GUEST_OPEN_DIRECTORY = 0x4000;
static constexpr int32_t GUEST_OPEN_NO_FOLLOW = 0x8000;
static constexpr int32_t GUEST_CLASS_NOT_A_NUMBER = 0x02;
static constexpr int32_t GUEST_CLASS_ZERO = 0x10;

struct GuestPollEvent
{
    uint32_t events;
    uint32_t padding;
    uint64_t data;
};

static void CheckOpenFlags()
{
    int32_t guest = GUEST_OPEN_DIRECTORY | GUEST_OPEN_NO_FOLLOW | O_RDONLY | O_CLOEXEC;
    int32_t host = HostBridge::TranslateOpenFlagsToHost(guest);

    Expect((host & O_DIRECTORY) != 0, "the guest's O_DIRECTORY reaches the host as O_DIRECTORY");
    Expect((host & O_NOFOLLOW) != 0, "the guest's O_NOFOLLOW reaches the host as O_NOFOLLOW");
    Expect((host & O_DIRECT) == 0, "the guest's O_DIRECTORY is not mistaken for the host's O_DIRECT");
    Expect((host & O_CLOEXEC) != 0, "flags both ABIs share pass through unchanged");
    Expect(HostBridge::TranslateOpenFlagsToGuest(host) == guest, "flags read back by F_GETFL use the guest's numbering");

    char directoryTemplate[] = "/tmp/acl-flags-XXXXXX";
    const char* base = mkdtemp(directoryTemplate);
    if (!base)
    {
        printf("FAIL the open flag check needs a work directory\n");
        failures += 1;
        return;
    }

    int descriptor = HostBridge::OpenFile(base, GUEST_OPEN_DIRECTORY | O_RDONLY, 0);
    Expect(descriptor >= 0, "a guest can open a directory with its own O_DIRECTORY value");
    if (descriptor >= 0) close(descriptor);

    rmdir(base);
}

static void CheckEventPoll()
{
    int poll = HostBridge::CreateEventPoll(O_CLOEXEC);
    int event = HostBridge::CreateEventDescriptor(1, 0);

    Expect(poll >= 0 && event >= 0, "an event poll and an event descriptor can be created");
    if (poll < 0 || event < 0) return;

    GuestPollEvent wanted{};
    wanted.events = 0x001;
    wanted.data = 0x1122334455667788ull;

    Expect(HostBridge::ControlEventPoll(poll, 1, event, &wanted) == 0, "a descriptor is added with the guest's event layout");

    GuestPollEvent delivered[2]{};
    int produced = HostBridge::WaitEventPoll(poll, delivered, 2, 0);

    Expect(produced == 1, "the ready descriptor is reported once");
    Expect(delivered[0].data == wanted.data, "the user data comes back at the guest's 8 byte offset, not the host's packed one");

    close(event);
    close(poll);
}

static void CheckSystemDescription()
{
    char described[6 * 65] = {};
    Expect(HostBridge::DescribeSystem(described) == 0, "uname succeeds");
    Expect(strcmp(described + 4 * 65, "aarch64") == 0, "uname reports the guest's machine, not the host's");

    char name[64] = {};
    Expect(HostBridge::HostName(name, sizeof(name)) == 0 && strcmp(name, "localhost") == 0, "the host name does not leak the host's");
    Expect(HostBridge::HostName(name, 4) != 0, "a host name buffer that is too small is refused");
}

static void CheckClassification()
{
    Expect(HostBridge::ClassifyDouble(0.0) == GUEST_CLASS_ZERO, "zero classifies with bionic's FP_ZERO value");
    Expect(HostBridge::ClassifyNarrow(__builtin_nanf("")) == GUEST_CLASS_NOT_A_NUMBER, "NaN classifies with bionic's FP_NAN value");
    Expect(HostBridge::IsInfinite(-__builtin_inf()) == -1, "negative infinity reports its sign");
}

int main()
{
    ExpectPlan("", 0, 0, 0, "an empty format takes no arguments");
    ExpectPlan("no conversions here", 0, 0, 0, "a literal format takes no arguments");
    ExpectPlan("100%% done", 0, 0, 0, "a doubled percent is not a conversion");
    ExpectPlan("%d", 1, 1, 0, "%d is one integer");
    ExpectPlan("%s", 1, 1, 0, "%s passes a pointer in the integer sequence");
    ExpectPlan("%f", 1, 0, 1, "%f is one float");
    ExpectPlan("%.3f", 1, 0, 1, "a fixed precision consumes no argument");
    ExpectPlan("%-+ #0'12.5s", 1, 1, 0, "flags and a fixed width consume no arguments");
    ExpectPlan("%*d", 2, 2, 0, "a star width consumes an extra integer");

    ExpectPlan("%*.*f", 3, 2, 1, "a star width AND a star precision consume two integers");

    ExpectPlan("%ld %zu %ju", 3, 3, 0, "length modifiers stay in the integer sequence");
    ExpectPlan("%lf", 1, 0, 1, "%lf is still a double");
    ExpectPlan("%p", 1, 1, 0, "%p is a pointer");

    const GuestLibc::ConversionClass gateFormat[] = {GuestLibc::ConversionClass::String, GuestLibc::ConversionClass::Integer,
                                                     GuestLibc::ConversionClass::Double, GuestLibc::ConversionClass::String};
    ExpectClasses("guest: %s %d %.4f %s\n", gateFormat, 4, "the gate format classifies as string/integer/double/string");

    const GuestLibc::ConversionClass mixed[] = {GuestLibc::ConversionClass::Integer, GuestLibc::ConversionClass::Integer,
                                                GuestLibc::ConversionClass::Double, GuestLibc::ConversionClass::LongInteger};
    ExpectClasses("%*.*f%ld", mixed, 4, "the two star arguments precede the float they describe");

    const GuestLibc::ConversionClass logFormat[] = {GuestLibc::ConversionClass::String, GuestLibc::ConversionClass::Integer,
                                                    GuestLibc::ConversionClass::Integer, GuestLibc::ConversionClass::Integer,
                                                    GuestLibc::ConversionClass::Integer};
    ExpectClasses("%s %d %d %d %d", logFormat, 5, "__android_log_print's everyday shape classifies");

    ExpectRejected("%", GuestLibc::Result::BadFormat, "a trailing percent is rejected");
    ExpectRejected("%y", GuestLibc::Result::BadFormat, "an unknown conversion is rejected, not guessed");
    ExpectRejected("%Lf", GuestLibc::Result::BadFormat, "long double is rejected rather than mis-marshalled");
    ExpectRejected(nullptr, GuestLibc::Result::BadFormat, "a null format is rejected");

    std::string many;
    for (int index = 0; index < GuestLibc::MAX_FORMAT_ARGUMENTS + 4; ++index) many += "%d";
    ExpectRejected(many.c_str(), GuestLibc::Result::TooManyArguments, "more conversions than the plan can hold is refused, not truncated");

    CheckDeviceIdentity();
    CheckAndroidTree();
    CheckOpenFlags();
    CheckEventPoll();
    CheckSystemDescription();
    CheckClassification();

    printf("%s: %d checks failed\n", failures == 0 ? "guestlibc-unit PASSED" : "guestlibc-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}

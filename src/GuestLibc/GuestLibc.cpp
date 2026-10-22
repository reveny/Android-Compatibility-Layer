#include "GuestLibc/GuestLibc.hpp"

#include "ElfLoader/ElfLoader.hpp"
#include "GuestLibc/HostBridge.hpp"
#include "GuestSync/GuestSync.hpp"
#include "GuestSyscall/GuestSyscall.hpp"

#include <atomic>
#include <cerrno>
#include <chrono>
#include <condition_variable>
#include <cstdarg>
#include <cstdint>
#include <memory>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cmath>
#include <ctime>
#include <cwchar>
#include <cwctype>
#include <locale.h>
#include <map>
#include <sched.h>
#include <signal.h>
#include <strings.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/random.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <unistd.h>
#include <mutex>
#include <pthread.h>
#include <vector>

#include "Platform/DeviceIdentity.hpp"
#include "Settings/Settings.hpp"

static constexpr int HOST_INTEGER_ARGUMENT_SLOTS = 6;
static constexpr int HOST_VECTOR_ARGUMENT_SLOTS = 8;
static constexpr int GUEST_INTEGER_ARGUMENT_SLOTS = 8;
static constexpr int GUEST_VECTOR_ARGUMENT_SLOTS = 8;
static constexpr uint64_t FORMAT_BUFFER_BYTES = 8192;
static constexpr uint64_t GUEST_FILE_STRIDE_BYTES = 152;
static constexpr uint64_t GUEST_STANDARD_STREAM_COUNT = 3;

struct HostRegisterSave
{
    uint64_t general[HOST_INTEGER_ARGUMENT_SLOTS];
    uint64_t vector[HOST_VECTOR_ARGUMENT_SLOTS * 2];
};

struct GuestVarargs
{
    const GuestState::Registers* registers = nullptr;
    const GuestState::MemoryAccessor* memory = nullptr;
    int integerIndex = 0;
    int vectorIndex = 0;
    uint64_t stackCursor = 0;
    bool faulted = false;

    bool fromArgumentList = false;
    uint64_t generalTop = 0;
    uint64_t vectorTop = 0;
    int32_t generalOffset = 0;
    int32_t vectorOffset = 0;
};

struct MarshalledCall
{
    HostRegisterSave save = {};
    std::vector<uint64_t> overflow;
    uint32_t generalOffset = 0;
    uint32_t vectorOffset = sizeof(uint64_t) * HOST_INTEGER_ARGUMENT_SLOTS;
};

static uint64_t NextStackSlot(GuestVarargs& source)
{
    uint64_t value = 0;
    if (!source.memory->Read(source.memory->context, (GuestState::GuestAddress)source.stackCursor, &value, sizeof(value)))
        source.faulted = true;

    source.stackCursor += sizeof(uint64_t);
    return value;
}

static uint64_t ReadGuestWord(GuestVarargs& source, uint64_t address)
{
    uint64_t value = 0;
    if (!source.memory->Read(source.memory->context, (GuestState::GuestAddress)address, &value, sizeof(value)))
        source.faulted = true;

    return value;
}

static uint64_t NextInteger(GuestVarargs& source)
{
    if (source.fromArgumentList)
    {
        if (source.generalOffset < 0)
        {
            uint64_t value = ReadGuestWord(source, source.generalTop + (uint64_t)(int64_t)source.generalOffset);
            source.generalOffset += 8;
            return value;
        }

        return NextStackSlot(source);
    }

    if (source.integerIndex < GUEST_INTEGER_ARGUMENT_SLOTS)
    {
        uint64_t value = source.registers->general[source.integerIndex];
        source.integerIndex += 1;
        return value;
    }

    return NextStackSlot(source);
}

static uint64_t NextDoubleBits(GuestVarargs& source)
{
    if (source.fromArgumentList)
    {
        if (source.vectorOffset < 0)
        {
            uint64_t value = ReadGuestWord(source, source.vectorTop + (uint64_t)(int64_t)source.vectorOffset);
            source.vectorOffset += 16;
            return value;
        }

        return NextStackSlot(source);
    }

    if (source.vectorIndex < GUEST_VECTOR_ARGUMENT_SLOTS)
    {
        uint64_t value = source.registers->vector[source.vectorIndex].low;
        source.vectorIndex += 1;
        return value;
    }

    return NextStackSlot(source);
}

static bool ReadGuestString(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t limit, std::string& text)
{
    text.clear();

    for (uint64_t index = 0; index < limit; ++index)
    {
        char character = 0;
        if (!memory.Read(memory.context, (GuestState::GuestAddress)(address + index), &character, 1)) return false;
        if (character == '\0') return true;

        text.push_back(character);
    }

    return false;
}

static GuestLibc::Result MarshalVarargs(const GuestLibc::FormatPlan& plan, GuestVarargs& source, MarshalledCall& call)
{
    int generalUsed = 0;
    int vectorUsed = 0;

    for (int index = 0; index < plan.conversionCount; ++index)
    {
        GuestLibc::ConversionClass conversionClass = plan.conversions[index];

        if (conversionClass == GuestLibc::ConversionClass::Double)
        {
            uint64_t bits = NextDoubleBits(source);
            if (vectorUsed < HOST_VECTOR_ARGUMENT_SLOTS)
            {
                call.save.vector[vectorUsed * 2] = bits;
                call.save.vector[vectorUsed * 2 + 1] = 0;
                vectorUsed += 1;
                continue;
            }

            call.overflow.push_back(bits);
            continue;
        }

        uint64_t value = NextInteger(source);
        if (generalUsed < HOST_INTEGER_ARGUMENT_SLOTS)
        {
            call.save.general[generalUsed] = value;
            generalUsed += 1;
            continue;
        }

        call.overflow.push_back(value);
    }

    if (source.faulted) return GuestLibc::Result::GuestMemoryFault;

    call.overflow.push_back(0);
    return GuestLibc::Result::Ok;
}

#if defined(__x86_64__)
static void BuildVaList(MarshalledCall& call, va_list& arguments)
{
    struct HostVaList
    {
        unsigned int generalOffset;
        unsigned int vectorOffset;
        void* overflowArgumentArea;
        void* registerSaveArea;
    };

    HostVaList* view = (HostVaList*)&arguments;
    view->generalOffset = call.generalOffset;
    view->vectorOffset = call.vectorOffset;
    view->overflowArgumentArea = call.overflow.data();
    view->registerSaveArea = &call.save;
}
#endif

static AbiBridge::Argument MakeArgument(AbiBridge::ArgumentClass argumentClass, uint32_t sizeInBytes)
{
    AbiBridge::Argument argument;
    argument.argumentClass = argumentClass;
    argument.sizeInBytes = sizeInBytes;
    argument.alignmentInBytes = sizeInBytes;
    return argument;
}

static AbiBridge::Argument PointerArgument()
{
    return MakeArgument(AbiBridge::ArgumentClass::Pointer, 8);
}

static AbiBridge::Argument WordArgument()
{
    return MakeArgument(AbiBridge::ArgumentClass::Integer, 8);
}

static AbiBridge::Argument IntArgument()
{
    return MakeArgument(AbiBridge::ArgumentClass::Integer, 4);
}

static AbiBridge::Argument DoubleArgument()
{
    return MakeArgument(AbiBridge::ArgumentClass::Double, 8);
}

static AbiBridge::Argument FloatArgument()
{
    return MakeArgument(AbiBridge::ArgumentClass::Float, 4);
}

static AbiBridge::Argument VoidArgument()
{
    return AbiBridge::Argument();
}

static AbiBridge::Signature MakeSignature(AbiBridge::Argument returnValue, const AbiBridge::Argument* arguments, int count)
{
    AbiBridge::Signature signature;
    signature.returnValue = returnValue;
    signature.totalArgumentCount = (uint8_t)count;
    signature.namedArgumentCount = (uint8_t)count;

    for (int index = 0; index < count; ++index) signature.arguments[index] = arguments[index];

    return signature;
}

static uint64_t IgnoredRegistration(uint64_t first, uint64_t second, uint64_t third)
{
    (void)first;
    (void)second;
    (void)third;
    return 0;
}

static void GuestAbort(void)
{
    fprintf(stderr, "[guestlibc] the guest called abort()\n");
    abort();
}

static void GuestStackCheckFail(void)
{
    fprintf(stderr, "[guestlibc] the guest tripped its stack protector\n");
    abort();
}

static void GuestSetAbortMessage(const char* message)
{
    fprintf(stderr, "[guestlibc] android_set_abort_message: %s\n", message ? message : "(null)");
}

GuestLibc::Result GuestLibc::ParseFormat(const char* format, FormatPlan& plan)
{
    plan.conversionCount = 0;
    plan.integerCount = 0;
    plan.floatCount = 0;

    if (!format) return Result::BadFormat;

    for (const char* cursor = format; *cursor != '\0'; ++cursor)
    {
        if (*cursor != '%') continue;

        cursor += 1;
        if (*cursor == '%') continue;

        int lengthModifier = 0;
        int starCount = 0;

        for (;;)
        {
            char character = *cursor;
            if (character == '\0') return Result::BadFormat;

            if (character == '-' || character == '+' || character == ' ' || character == '#' || character == '\'' || character == '0')
            {
                cursor += 1;
                continue;
            }
            break;
        }

        while ((*cursor >= '0' && *cursor <= '9') || *cursor == '*')
        {
            if (*cursor == '*') starCount += 1;
            cursor += 1;
        }

        if (*cursor == '.')
        {
            cursor += 1;
            while ((*cursor >= '0' && *cursor <= '9') || *cursor == '*')
            {
                if (*cursor == '*') starCount += 1;
                cursor += 1;
            }
        }

        for (int star = 0; star < starCount; ++star)
        {
            if (plan.conversionCount >= MAX_FORMAT_ARGUMENTS) return Result::TooManyArguments;
            plan.conversions[plan.conversionCount] = ConversionClass::Integer;
            plan.conversionCount += 1;
            plan.integerCount += 1;
        }

        while (*cursor == 'h' || *cursor == 'l' || *cursor == 'L' || *cursor == 'z' || *cursor == 'j' || *cursor == 't' || *cursor == 'q')
        {
            if (*cursor == 'l' || *cursor == 'z' || *cursor == 'j' || *cursor == 't' || *cursor == 'q') lengthModifier += 1;
            if (*cursor == 'L') lengthModifier = 3;
            cursor += 1;
        }

        if (*cursor == '\0') return Result::BadFormat;
        if (plan.conversionCount >= MAX_FORMAT_ARGUMENTS) return Result::TooManyArguments;

        ConversionClass conversionClass = ConversionClass::Invalid;

        switch (*cursor)
        {
            case 'd':
            case 'i':
            case 'u':
            case 'o':
            case 'x':
            case 'X':
            case 'c':
                conversionClass = lengthModifier > 0 ? ConversionClass::LongInteger : ConversionClass::Integer;
                break;
            case 'e':
            case 'E':
            case 'f':
            case 'F':
            case 'g':
            case 'G':
            case 'a':
            case 'A':
                conversionClass = lengthModifier == 3 ? ConversionClass::LongDouble : ConversionClass::Double;
                break;
            case 's': conversionClass = lengthModifier > 0 ? ConversionClass::WideString : ConversionClass::String; break;
            case 'p': conversionClass = ConversionClass::Pointer; break;
            case 'n': conversionClass = ConversionClass::CountOut; break;
            default: return Result::BadFormat;
        }

        if (conversionClass == ConversionClass::LongDouble) return Result::BadFormat;

        plan.conversions[plan.conversionCount] = conversionClass;
        plan.conversionCount += 1;

        if (conversionClass == ConversionClass::Double)
            plan.floatCount += 1;
        else
            plan.integerCount += 1;
    }

    return Result::Ok;
}

#if defined(__x86_64__)
static AbiBridge::Result FormatCall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory, int fixedArgumentCount,
                                    int formatArgumentIndex, std::string& output, int& produced)
{
    uint64_t formatAddress = registers.general[formatArgumentIndex];

    std::string format;
    if (!ReadGuestString(memory, formatAddress, GuestLibc::MAX_FORMAT_LENGTH, format))
    {
        fprintf(stderr, "[guestlibc] cannot read the format string at %016llx\n", (unsigned long long)formatAddress);
        return AbiBridge::Result::MalformedArguments;
    }

    GuestLibc::FormatPlan plan;
    GuestLibc::Result parsed = GuestLibc::ParseFormat(format.c_str(), plan);
    if (parsed != GuestLibc::Result::Ok)
    {
        fprintf(stderr, "[guestlibc] cannot marshal the format \"%s\": %s\n", format.c_str(), GuestLibc::ResultName(parsed));
        return AbiBridge::Result::UnsupportedSignature;
    }

    GuestVarargs source;
    source.registers = &registers;
    source.memory = &memory;
    source.integerIndex = fixedArgumentCount;
    source.vectorIndex = 0;
    source.stackCursor = registers.stackPointer;

    if (fixedArgumentCount < 0)
    {
        uint64_t list = registers.general[-fixedArgumentCount - 1];

        uint64_t stack = 0;
        uint64_t generalTop = 0;
        uint64_t vectorTop = 0;
        int32_t generalOffset = 0;
        int32_t vectorOffset = 0;

        if (!memory.Read(memory.context, (GuestState::GuestAddress)list, &stack, sizeof(stack)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 8), &generalTop, sizeof(generalTop)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 16), &vectorTop, sizeof(vectorTop)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 24), &generalOffset, sizeof(generalOffset)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 28), &vectorOffset, sizeof(vectorOffset)))
        {
            return AbiBridge::Result::MalformedArguments;
        }

        source.fromArgumentList = true;
        source.stackCursor = stack;
        source.generalTop = generalTop;
        source.vectorTop = vectorTop;
        source.generalOffset = generalOffset;
        source.vectorOffset = vectorOffset;
    }

    MarshalledCall call;
    GuestLibc::Result marshalled = MarshalVarargs(plan, source, call);
    if (marshalled != GuestLibc::Result::Ok) return AbiBridge::Result::MalformedArguments;

    std::vector<char> buffer((size_t)FORMAT_BUFFER_BYTES);

    for (int attempt = 0; attempt < 2; ++attempt)
    {
        va_list arguments;
        BuildVaList(call, arguments);

        produced = vsnprintf(buffer.data(), buffer.size(), format.c_str(), arguments);
        if (produced < 0) return AbiBridge::Result::MalformedArguments;
        if ((size_t)produced < buffer.size()) break;

        buffer.assign((size_t)produced + 1, 0);
    }

    output.assign(buffer.data(), (size_t)produced < buffer.size() ? (size_t)produced : buffer.size() - 1);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result PrintfHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, 1, 0, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fwrite(output.data(), 1, output.size(), stdout);
    registers.general[0] = (uint64_t)(uint32_t)produced;

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SnprintfHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t destination = registers.general[0];
    uint64_t limit = registers.general[1];

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, 3, 2, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (limit != 0 && destination != 0)
    {
        uint64_t copied = output.size() + 1 < limit ? output.size() + 1 : limit;
        std::vector<char> terminated(output.begin(), output.begin() + (long)(copied - 1));
        terminated.push_back('\0');

        if (!memory.Write(memory.context, (GuestState::GuestAddress)destination, terminated.data(), copied))
            return AbiBridge::Result::MalformedArguments;
    }

    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WriteFormattedInto(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory,
                                            uint64_t destination, uint64_t limit, int fixedArgumentCount, int formatArgumentIndex)
{
    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, fixedArgumentCount, formatArgumentIndex, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (limit != 0 && destination != 0)
    {
        uint64_t copied = output.size() + 1 < limit ? output.size() + 1 : limit;
        std::vector<char> terminated(output.begin(), output.begin() + (long)(copied - 1));
        terminated.push_back('\0');

        if (!memory.Write(memory.context, (GuestState::GuestAddress)destination, terminated.data(), copied))
            return AbiBridge::Result::MalformedArguments;
    }

    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result VsnprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    return WriteFormattedInto(registers, memory, registers.general[0], registers.general[1], -4, 2);
}

static AbiBridge::Result VsprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    return WriteFormattedInto(registers, memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, -3, 1);
}

static AbiBridge::Result SprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    return WriteFormattedInto(registers, memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, 2, 1);
}

static uint64_t CheckedFormatLimit(uint64_t limit, uint64_t capacity)
{
    return capacity < limit ? capacity : limit;
}

static AbiBridge::Result CheckedVsnprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t limit = CheckedFormatLimit(registers.general[1], registers.general[3]);
    return WriteFormattedInto(registers, memory, registers.general[0], limit, -6, 4);
}

static AbiBridge::Result CheckedSnprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t limit = CheckedFormatLimit(registers.general[1], registers.general[3]);
    return WriteFormattedInto(registers, memory, registers.general[0], limit, 5, 4);
}

static AbiBridge::Result CheckedVsprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t limit = CheckedFormatLimit(GuestLibc::MAX_FORMAT_LENGTH, registers.general[2]);
    return WriteFormattedInto(registers, memory, registers.general[0], limit, -5, 3);
}

static AbiBridge::Result CheckedSprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t limit = CheckedFormatLimit(GuestLibc::MAX_FORMAT_LENGTH, registers.general[2]);
    return WriteFormattedInto(registers, memory, registers.general[0], limit, 4, 3);
}

static AbiBridge::Result AndroidLogBufferPrintHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    std::string tag;
    if (!ReadGuestString(memory, registers.general[2], GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, 4, 3, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fprintf(stderr, "[guest %s] %s\n", tag.c_str(), output.c_str());
    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result VprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, -2, 0, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fwrite(output.data(), 1, output.size(), stdout);
    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result VfprintfHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, -3, 1, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fwrite(output.data(), 1, output.size(), stderr);
    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AndroidLogVprintHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    std::string tag;
    if (!ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, -4, 2, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fprintf(stderr, "[guest %s] %s\n", tag.c_str(), output.c_str());
    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AndroidLogPrintHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t tagAddress = registers.general[1];

    std::string tag;
    if (!ReadGuestString(memory, tagAddress, GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, 3, 2, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fprintf(stderr, "[guest %s] %s\n", tag.c_str(), output.c_str());
    registers.general[0] = (uint64_t)(uint32_t)produced;

    return AbiBridge::Result::Ok;
}
#endif

static constexpr uint64_t AT_GUEST_PAGESZ = 6;
static constexpr uint64_t AT_GUEST_HWCAP = 16;
static constexpr uint64_t AT_GUEST_CLKTCK = 17;
static constexpr uint64_t AT_GUEST_SECURE = 23;
static constexpr uint64_t AT_GUEST_HWCAP2 = 26;

static constexpr uint64_t GUEST_HWCAP_FP = 1ull << 0;
static constexpr uint64_t GUEST_HWCAP_ASIMD = 1ull << 1;
static constexpr uint64_t GUEST_HWCAP_ATOMICS = 1ull << 8;

struct PropertyEntry
{
    const char* name;
    const char* value;
};

static const PropertyEntry gSystemProperties[] = {
        {"ro.build.version.sdk", Platform::DECLARED_SDK},
        {"ro.build.version.release", Platform::DECLARED_RELEASE},
        {"ro.build.version.release_or_codename", Platform::DECLARED_RELEASE},
        {"ro.build.version.security_patch", Platform::SECURITY_PATCH},
        {"ro.build.version.incremental", Platform::BUILD_INCREMENTAL},
        {"ro.build.version.codename", "REL"},
        {"ro.build.version.preview_sdk", "0"},
        {"ro.build.version.base_os", ""},
        {"ro.build.fingerprint", Platform::BUILD_FINGERPRINT},
        {"ro.build.id", Platform::BUILD_IDENTIFIER},
        {"ro.build.display.id", Platform::BUILD_IDENTIFIER},
        {"ro.build.type", Platform::BUILD_TYPE},
        {"ro.build.tags", Platform::BUILD_TAGS},
        {"ro.build.host", Platform::BUILD_HOST},
        {"ro.build.user", Platform::BUILD_USER},
        {"ro.build.characteristics", "default"},
        {"ro.product.model", Platform::DEVICE_MODEL},
        {"ro.product.manufacturer", Platform::DEVICE_MANUFACTURER},
        {"ro.product.brand", Platform::DEVICE_BRAND},
        {"ro.product.name", Platform::DEVICE_PRODUCT},
        {"ro.product.device", Platform::DEVICE_NAME},
        {"ro.product.board", Platform::DEVICE_BOARD},
        {"ro.product.cpu.abi", "arm64-v8a"},
        {"ro.product.cpu.abilist", "arm64-v8a"},
        {"ro.product.cpu.abilist64", "arm64-v8a"},
        {"ro.product.cpu.abilist32", ""},
        {"ro.soc.manufacturer", Platform::DEVICE_SOC_MANUFACTURER},
        {"ro.soc.model", Platform::DEVICE_SOC_MODEL},
        {"ro.hardware", Platform::DEVICE_HARDWARE},
        {"ro.board.platform", Platform::DEVICE_BOARD},
        {"ro.bootloader", "unknown"},
        {"ro.debuggable", "0"},
        {"ro.secure", "1"},
};

static constexpr uint64_t SYSTEM_PROPERTY_COUNT = sizeof(gSystemProperties) / sizeof(gSystemProperties[0]);

static AbiBridge::Result SystemPropertyGetHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string requested;
    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, requested)) requested.clear();

    const char* answer = "";
    for (const PropertyEntry& entry : gSystemProperties)
    {
        if (requested != entry.name) continue;

        answer = entry.value;
        break;
    }

    uint64_t destination = registers.general[1];
    uint64_t length = strlen(answer);

    if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, answer, length + 1))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(uint32_t)length;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SystemPropertyFindHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string requested;
    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, requested)) requested.clear();

    registers.general[0] = 0;
    for (uint64_t index = 0; index < SYSTEM_PROPERTY_COUNT; ++index)
    {
        if (requested != gSystemProperties[index].name) continue;

        registers.general[0] = index + 1;
        break;
    }

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SystemPropertyReadHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t handle = registers.general[0];
    uint64_t nameOut = registers.general[1];
    uint64_t valueOut = registers.general[2];

    if (handle == 0 || handle > SYSTEM_PROPERTY_COUNT)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    const PropertyEntry& entry = gSystemProperties[handle - 1];
    uint64_t length = strlen(entry.value);

    if (nameOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)nameOut, entry.name, strlen(entry.name) + 1))
        return AbiBridge::Result::MalformedArguments;

    if (valueOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)valueOut, entry.value, length + 1))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(uint32_t)length;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result GetAuxiliaryValueHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    switch (registers.general[0])
    {
        case AT_GUEST_HWCAP: registers.general[0] = GUEST_HWCAP_FP | GUEST_HWCAP_ASIMD | GUEST_HWCAP_ATOMICS; break;
        case AT_GUEST_HWCAP2: registers.general[0] = 0; break;
        case AT_GUEST_PAGESZ: registers.general[0] = 4096; break;
        case AT_GUEST_CLKTCK: registers.general[0] = 100; break;
        case AT_GUEST_SECURE: registers.general[0] = 0; break;
        default: registers.general[0] = 0; break;
    }

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadLocalResolveHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t descriptor = registers.general[0];
    uint64_t offset = 0;

    if (!memory.Read(memory.context, (GuestState::GuestAddress)(descriptor + sizeof(uint64_t)), &offset, sizeof(offset)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = offset;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result TlsGetAddrHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance* instance = (GuestRuntime::Instance*)context;

    uint64_t descriptor = registers.general[0];
    uint64_t pair[2] = {0, 0};

    if (!memory.Read(memory.context, (GuestState::GuestAddress)descriptor, pair, sizeof(pair))) return AbiBridge::Result::MalformedArguments;

    const ElfLoader::Library* library = ElfLoader::FindLibrary(GuestRuntime::LibraryNamespace(*instance), (uint32_t)pair[0]);
    if (!library || !library->threadLocalStorage.present)
    {
        fprintf(stderr, "[guestlibc] __tls_get_addr for unknown module %llu\n", (unsigned long long)pair[0]);
        return AbiBridge::Result::UnknownSymbol;
    }

    registers.general[0] = registers.threadPointer + library->threadLocalStorage.staticOffsetFromThreadPointer + pair[1];
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result PthreadCreateHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance* instance = (GuestRuntime::Instance*)context;

    uint64_t threadOut = registers.general[0];
    uint64_t startRoutine = registers.general[2];
    uint64_t argument = registers.general[3];

    uint64_t handle = 0;
    GuestRuntime::Result started = GuestRuntime::StartThread(*instance, (GuestState::GuestAddress)startRoutine, argument, handle);

    fprintf(stderr, "[guestlibc] the guest started a thread at %016llx: %s\n", (unsigned long long)startRoutine,
            GuestRuntime::ResultName(started));

    if (started != GuestRuntime::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)EAGAIN;
        return AbiBridge::Result::Ok;
    }

    if (threadOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)threadOut, &handle, sizeof(handle)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static std::mutex gGuardLock;

static int GuestGuardAcquire(uint8_t* guard)
{
    if (!guard) return 0;

    std::lock_guard<std::mutex> held(gGuardLock);
    if (*guard != 0) return 0;

    return 1;
}

static void GuestGuardRelease(uint8_t* guard)
{
    if (!guard) return;

    std::lock_guard<std::mutex> held(gGuardLock);
    *guard = 1;
}

static void GuestPureVirtual()
{
    fprintf(stderr, "[guestlibc] the guest called a pure virtual function\n");
    abort();
}

static void GuestExitProcess(int status)
{
    fprintf(stderr, "[guestlibc] the guest called exit(%d)\n", status);
    _exit(status);
}

static void GuestAssertionFailed(const char* file, int line, const char* function, const char* message)
{
    fprintf(stderr, "[guestlibc] the guest failed an assertion at %s:%d in %s: %s\n", file ? file : "?", line,
            function ? function : "?", message ? message : "?");
    abort();
}

static constexpr int64_t EPOCH_DEADLINE_SECONDS = 1000000000ll;

struct GuestMutex
{
    std::recursive_mutex lock;
    std::atomic<int> depth{0};
};

struct GuestCondition
{
    std::condition_variable_any wait;
};

static std::mutex gShadowLock;
static std::map<uint64_t, std::unique_ptr<GuestMutex>> gGuestMutexes;
static std::map<uint64_t, std::unique_ptr<GuestCondition>> gGuestConditions;
static thread_local std::map<uint64_t, uint64_t> gThreadLocalValues;
static std::atomic<uint64_t> gNextThreadLocalKey{1};

static GuestMutex& ShadowMutex(uint64_t address)
{
    std::lock_guard<std::mutex> guard(gShadowLock);

    std::unique_ptr<GuestMutex>& slot = gGuestMutexes[address];
    if (!slot) slot = std::make_unique<GuestMutex>();

    return *slot;
}

static GuestCondition& ShadowCondition(uint64_t address)
{
    std::lock_guard<std::mutex> guard(gShadowLock);

    std::unique_ptr<GuestCondition>& slot = gGuestConditions[address];
    if (!slot) slot = std::make_unique<GuestCondition>();

    return *slot;
}

static AbiBridge::Result MutexLockHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint64_t object = registers.general[0];
    GuestMutex& guarded = ShadowMutex(object);

    if (!guarded.lock.try_lock())
    {
        GuestSync::BeginWait(GuestSync::Operation::MutexLock, object, registers.general[30]);
        guarded.lock.lock();
        GuestSync::EndWait(0);
    }

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result MutexTryLockHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    bool taken = ShadowMutex(registers.general[0]).lock.try_lock();
    registers.general[0] = taken ? 0 : (uint64_t)(uint32_t)EBUSY;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result MutexUnlockHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint64_t object = registers.general[0];

    GuestSync::NoteWake(GuestSync::Operation::MutexUnlock, object, 1);
    ShadowMutex(object).lock.unlock();

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result MutexDestroyHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::lock_guard<std::mutex> guard(gShadowLock);
    gGuestMutexes.erase(registers.general[0]);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConditionWaitHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint64_t object = registers.general[0];
    GuestCondition& condition = ShadowCondition(object);
    GuestMutex& guarded = ShadowMutex(registers.general[1]);

    GuestSync::BeginWait(GuestSync::Operation::ConditionWait, object, registers.general[30]);
    condition.wait.wait(guarded.lock);
    GuestSync::EndWait(0);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConditionTimedWaitHandler(void* context, GuestState::Registers& registers,
                                                   const GuestState::MemoryAccessor& memory)
{
    (void)context;

    GuestCondition& condition = ShadowCondition(registers.general[0]);
    GuestMutex& guarded = ShadowMutex(registers.general[1]);
    uint64_t deadline = registers.general[2];

    int64_t seconds = 0;
    int64_t nanoseconds = 0;

    if (deadline != 0)
    {
        if (!memory.Read(memory.context, (GuestState::GuestAddress)deadline, &seconds, sizeof(seconds)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(deadline + 8), &nanoseconds, sizeof(nanoseconds)))
        {
            return AbiBridge::Result::MalformedArguments;
        }
    }

    struct timespec now;
    clock_gettime(seconds >= EPOCH_DEADLINE_SECONDS ? CLOCK_REALTIME : CLOCK_MONOTONIC, &now);

    int64_t remaining = (seconds - (int64_t)now.tv_sec) * 1000000000ll + (nanoseconds - (int64_t)now.tv_nsec);
    if (remaining < 0) remaining = 0;

    GuestSync::BeginWait(GuestSync::Operation::ConditionTimedWait, registers.general[0], registers.general[30]);
    std::cv_status status = condition.wait.wait_for(guarded.lock, std::chrono::nanoseconds(remaining));

    registers.general[0] = status == std::cv_status::timeout ? (uint64_t)(uint32_t)ETIMEDOUT : 0;
    GuestSync::EndWait(registers.general[0]);

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConditionSignalHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSync::NoteWake(GuestSync::Operation::ConditionSignal, registers.general[0], 1);
    ShadowCondition(registers.general[0]).wait.notify_one();

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConditionBroadcastHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSync::NoteWake(GuestSync::Operation::ConditionBroadcast, registers.general[0], 0);
    ShadowCondition(registers.general[0]).wait.notify_all();

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConditionDestroyHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::lock_guard<std::mutex> guard(gShadowLock);
    gGuestConditions.erase(registers.general[0]);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SucceedingStubHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

struct GuestSemaphore
{
    std::mutex lock;
    std::condition_variable available;
    uint32_t count = 0;
};

static std::map<uint64_t, std::unique_ptr<GuestSemaphore>> gGuestSemaphores;

static GuestSemaphore& ShadowSemaphore(uint64_t address)
{
    std::lock_guard<std::mutex> guard(gShadowLock);

    std::unique_ptr<GuestSemaphore>& slot = gGuestSemaphores[address];
    if (!slot) slot = std::make_unique<GuestSemaphore>();

    return *slot;
}

static AbiBridge::Result SemaphoreCreateHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);

    std::lock_guard<std::mutex> held(semaphore.lock);
    semaphore.count = (uint32_t)registers.general[2];

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphoreDestroyHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::lock_guard<std::mutex> guard(gShadowLock);
    gGuestSemaphores.erase(registers.general[0]);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphorePostHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);

    {
        std::lock_guard<std::mutex> held(semaphore.lock);
        semaphore.count += 1;
    }

    GuestSync::NoteWake(GuestSync::Operation::SemaphorePost, registers.general[0], 1);
    semaphore.available.notify_one();

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphoreWaitHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);

    GuestSync::BeginWait(GuestSync::Operation::SemaphoreWait, registers.general[0], registers.general[30]);

    std::unique_lock<std::mutex> held(semaphore.lock);
    semaphore.available.wait(held, [&semaphore]() { return semaphore.count != 0; });

    GuestSync::EndWait(0);
    semaphore.count -= 1;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphoreTryWaitHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);

    std::lock_guard<std::mutex> held(semaphore.lock);
    if (semaphore.count == 0)
    {
        errno = EAGAIN;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    semaphore.count -= 1;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphoreWaitUntilHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);
    uint64_t deadline = registers.general[1];

    int64_t seconds = 0;
    int64_t nanoseconds = 0;

    if (deadline != 0 &&
        (!memory.Read(memory.context, (GuestState::GuestAddress)deadline, &seconds, sizeof(seconds)) ||
         !memory.Read(memory.context, (GuestState::GuestAddress)(deadline + 8), &nanoseconds, sizeof(nanoseconds))))
    {
        return AbiBridge::Result::MalformedArguments;
    }

    std::chrono::nanoseconds span(seconds * 1000000000ll + nanoseconds);
    std::chrono::time_point<std::chrono::system_clock> when(std::chrono::duration_cast<std::chrono::system_clock::duration>(span));

    GuestSync::BeginWait(GuestSync::Operation::SemaphoreTimedWait, registers.general[0], registers.general[30]);

    std::unique_lock<std::mutex> held(semaphore.lock);
    bool ready = semaphore.available.wait_until(held, when, [&semaphore]() { return semaphore.count != 0; });

    GuestSync::EndWait(ready ? 0 : (uint64_t)(uint32_t)ETIMEDOUT);

    if (!ready)
    {
        errno = ETIMEDOUT;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    semaphore.count -= 1;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SemaphoreReadHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    GuestSemaphore& semaphore = ShadowSemaphore(registers.general[0]);
    uint64_t destination = registers.general[1];

    int32_t value = 0;
    {
        std::lock_guard<std::mutex> held(semaphore.lock);
        value = (int32_t)semaphore.count;
    }

    if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &value, sizeof(value)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

struct GuestOnce
{
    std::mutex lock;
    bool done = false;
};

static std::map<uint64_t, std::unique_ptr<GuestOnce>> gGuestOnceFlags;

static GuestOnce& ShadowOnce(uint64_t address)
{
    std::lock_guard<std::mutex> guard(gShadowLock);

    std::unique_ptr<GuestOnce>& slot = gGuestOnceFlags[address];
    if (!slot) slot = std::make_unique<GuestOnce>();

    return *slot;
}

static AbiBridge::Result ThreadOnceHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)memory;
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t control = registers.general[0];
    uint64_t routine = registers.general[1];

    if (control == 0 || routine == 0)
    {
        registers.general[0] = (uint64_t)(uint32_t)EINVAL;
        return AbiBridge::Result::Ok;
    }

    GuestOnce& once = ShadowOnce(control);

    std::lock_guard<std::mutex> held(once.lock);
    if (!once.done)
    {
        uint64_t returned = 0;
        GuestRuntime::Result called =
            GuestRuntime::CallGuestNested(instance, registers, (GuestState::GuestAddress)routine, nullptr, 0, returned);

        if (called != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[guestlibc] the pthread_once initializer at %016llx failed: %s\n", (unsigned long long)routine,
                    GuestRuntime::ResultName(called));
            return AbiBridge::Result::GuestFaulted;
        }

        once.done = true;
    }

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadExitHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[30] = GuestRuntime::SENTINEL_RETURN_ADDRESS;
    return AbiBridge::Result::Ok;
}

static constexpr int GUEST_ATTRIBUTES_STACK_BASE = 0;
static constexpr int GUEST_ATTRIBUTES_STACK_BYTES = 8;
static constexpr uint64_t GUEST_ATTRIBUTES_RECORD_BYTES = 16;

static AbiBridge::Result ThreadAttributesHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t attributes = registers.general[1];
    if (attributes == 0) return AbiBridge::Result::MalformedArguments;

    uint64_t base = 0;
    uint64_t bytes = 0;

    if (!GuestRuntime::CurrentGuestStack(base, bytes))
    {
        bytes = GuestRuntime::DEFAULT_STACK_BYTES;
        base = registers.stackPointer > bytes ? registers.stackPointer - bytes : 0;
    }

    uint8_t described[GUEST_ATTRIBUTES_RECORD_BYTES];
    memcpy(described + GUEST_ATTRIBUTES_STACK_BASE, &base, sizeof(base));
    memcpy(described + GUEST_ATTRIBUTES_STACK_BYTES, &bytes, sizeof(bytes));

    if (!memory.Write(memory.context, (GuestState::GuestAddress)attributes, described, sizeof(described)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadStackBoundsHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t attributes = registers.general[0];
    uint64_t baseOut = registers.general[1];
    uint64_t sizeOut = registers.general[2];

    uint64_t base = 0;
    uint64_t bytes = 0;

    uint8_t described[GUEST_ATTRIBUTES_RECORD_BYTES];
    if (attributes != 0 && memory.Read(memory.context, (GuestState::GuestAddress)attributes, described, sizeof(described)))
    {
        memcpy(&base, described + GUEST_ATTRIBUTES_STACK_BASE, sizeof(base));
        memcpy(&bytes, described + GUEST_ATTRIBUTES_STACK_BYTES, sizeof(bytes));
    }

    if (bytes == 0 && !GuestRuntime::CurrentGuestStack(base, bytes))
    {
        bytes = GuestRuntime::DEFAULT_STACK_BYTES;
        base = registers.stackPointer > bytes ? registers.stackPointer - bytes : 0;
    }

    if (baseOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)baseOut, &base, sizeof(base)))
        return AbiBridge::Result::MalformedArguments;

    if (sizeOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)sizeOut, &bytes, sizeof(bytes)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static constexpr uint64_t JUMP_CONTEXT_COOKIE = 0x6a756d70636e7478ull;

static constexpr int JUMP_SLOT_COOKIE = 0;
static constexpr int JUMP_SLOT_TOKEN = 1;
static constexpr int JUMP_SLOT_RETURN = 2;
static constexpr int JUMP_SLOT_STACK = 3;
static constexpr int JUMP_SLOT_FRAME = 4;
static constexpr int JUMP_SLOT_FIRST_GENERAL = 5;
static constexpr int JUMP_SLOT_FIRST_VECTOR = 15;
static constexpr int JUMP_SLOT_COUNT = 23;

static constexpr int JUMP_FIRST_SAVED_GENERAL = 19;
static constexpr int JUMP_SAVED_GENERAL_COUNT = 10;
static constexpr int JUMP_FIRST_SAVED_VECTOR = 8;
static constexpr int JUMP_SAVED_VECTOR_COUNT = 8;

static bool WriteJumpSlot(const GuestState::MemoryAccessor& memory, uint64_t buffer, int slot, uint64_t value)
{
    return memory.Write(memory.context, (GuestState::GuestAddress)(buffer + (uint64_t)slot * 8), &value, sizeof(value));
}

static bool ReadJumpSlot(const GuestState::MemoryAccessor& memory, uint64_t buffer, int slot, uint64_t& value)
{
    return memory.Read(memory.context, (GuestState::GuestAddress)(buffer + (uint64_t)slot * 8), &value, sizeof(value));
}

static AbiBridge::Result SaveJumpContextHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t buffer = registers.general[0];
    if (buffer == 0) return AbiBridge::Result::MalformedArguments;

    bool written = WriteJumpSlot(memory, buffer, JUMP_SLOT_COOKIE, JUMP_CONTEXT_COOKIE) &&
                   WriteJumpSlot(memory, buffer, JUMP_SLOT_TOKEN, GuestRuntime::CurrentCallToken()) &&
                   WriteJumpSlot(memory, buffer, JUMP_SLOT_RETURN, registers.general[30]) &&
                   WriteJumpSlot(memory, buffer, JUMP_SLOT_STACK, registers.stackPointer) &&
                   WriteJumpSlot(memory, buffer, JUMP_SLOT_FRAME, registers.general[29]);

    for (int index = 0; written && index < JUMP_SAVED_GENERAL_COUNT; ++index)
    {
        written = WriteJumpSlot(memory, buffer, JUMP_SLOT_FIRST_GENERAL + index, registers.general[JUMP_FIRST_SAVED_GENERAL + index]);
    }

    for (int index = 0; written && index < JUMP_SAVED_VECTOR_COUNT; ++index)
    {
        written = WriteJumpSlot(memory, buffer, JUMP_SLOT_FIRST_VECTOR + index, registers.vector[JUMP_FIRST_SAVED_VECTOR + index].low);
    }

    if (!written) return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result RestoreJumpContextHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t buffer = registers.general[0];
    int32_t value = (int32_t)(uint32_t)registers.general[1];

    if (buffer == 0) return AbiBridge::Result::MalformedArguments;

    uint64_t slots[JUMP_SLOT_COUNT];
    for (int index = 0; index < JUMP_SLOT_COUNT; ++index)
    {
        if (!ReadJumpSlot(memory, buffer, index, slots[index])) return AbiBridge::Result::MalformedArguments;
    }

    if (slots[JUMP_SLOT_COOKIE] != JUMP_CONTEXT_COOKIE)
    {
        fprintf(stderr, "[guestlibc] the guest jumped to a context at %016llx that setjmp never wrote\n",
                (unsigned long long)buffer);
        return AbiBridge::Result::MalformedArguments;
    }

    if (slots[JUMP_SLOT_TOKEN] != GuestRuntime::CurrentCallToken())
    {
        fprintf(stderr, "[guestlibc] the guest jumped to a context saved in a different guest call, which would leave host frames "
                        "stranded; the jump was refused\n");
        return AbiBridge::Result::MalformedArguments;
    }

    for (int index = 0; index < JUMP_SAVED_GENERAL_COUNT; ++index)
    {
        registers.general[JUMP_FIRST_SAVED_GENERAL + index] = slots[JUMP_SLOT_FIRST_GENERAL + index];
    }

    for (int index = 0; index < JUMP_SAVED_VECTOR_COUNT; ++index)
    {
        registers.vector[JUMP_FIRST_SAVED_VECTOR + index].low = slots[JUMP_SLOT_FIRST_VECTOR + index];
        registers.vector[JUMP_FIRST_SAVED_VECTOR + index].high = 0;
    }

    registers.general[29] = slots[JUMP_SLOT_FRAME];
    registers.stackPointer = slots[JUMP_SLOT_STACK];
    registers.general[30] = slots[JUMP_SLOT_RETURN];
    registers.general[0] = (uint64_t)(uint32_t)(value == 0 ? 1 : value);

    return AbiBridge::Result::Ok;
}

static constexpr const char* GUEST_FAULT_SIGNAL_VARIABLE = "RUNTIME_GUEST_FAULT_SIGNALS";
static constexpr int GUEST_SIGNAL_COUNT = 65;
static constexpr uint64_t GUEST_SIGNAL_DEFAULT = 0;
static constexpr uint64_t GUEST_SIGNAL_IGNORE = 1;

static constexpr int GUEST_ACTION_FLAGS_OFFSET = 0;
static constexpr int GUEST_ACTION_HANDLER_OFFSET = 8;
static constexpr int GUEST_ACTION_BYTES = 32;

static std::atomic<uint64_t> gGuestSignalHandlers[GUEST_SIGNAL_COUNT];
static std::atomic<uint32_t> gGuestSignalFlags[GUEST_SIGNAL_COUNT];
static std::atomic<bool> gGuestFaultHandlersWanted[GUEST_SIGNAL_COUNT];

static bool SignalBelongsToTheRuntime(int number)
{
    return number == SIGSEGV || number == SIGBUS || number == SIGILL || number == SIGFPE || number == SIGTRAP || number == SIGABRT ||
           number == SIGSYS;
}

static void DeliverGuestSignal(int number, siginfo_t* information, void* context)
{
    (void)context;

    if (number < 0 || number >= GUEST_SIGNAL_COUNT) return;

    uint64_t handler = gGuestSignalHandlers[number].load(std::memory_order_acquire);
    if (handler == GUEST_SIGNAL_DEFAULT || handler == GUEST_SIGNAL_IGNORE) return;
    if (!GuestRuntime::OnGuestThread()) return;

    uint64_t arguments[3] = {(uint64_t)(uint32_t)number, (uint64_t)(uintptr_t)information, 0};
    uint64_t returned = 0;

    GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)handler, arguments, 3, returned);
}

static thread_local int gFaultDeliveryDepth = 0;

static bool DeliverGuestFault(void* context, int number, void* information)
{
    (void)context;

    if (number < 0 || number >= GUEST_SIGNAL_COUNT) return false;
    if (gFaultDeliveryDepth != 0) return false;
    if (!gGuestFaultHandlersWanted[number].load(std::memory_order_acquire)) return false;

    uint64_t handler = gGuestSignalHandlers[number].load(std::memory_order_acquire);
    if (handler == GUEST_SIGNAL_DEFAULT || handler == GUEST_SIGNAL_IGNORE) return false;
    if (!GuestRuntime::OnGuestThread()) return false;

    uint64_t arguments[3] = {(uint64_t)(uint32_t)number, (uint64_t)(uintptr_t)information, 0};
    uint64_t returned = 0;

    gFaultDeliveryDepth += 1;
    GuestRuntime::Result delivered =
        GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)handler, arguments, 3, returned);
    gFaultDeliveryDepth -= 1;

    return delivered == GuestRuntime::Result::Ok;
}

static bool InstallHostSignalHandler(int number)
{
    struct sigaction action;
    memset(&action, 0, sizeof(action));

    action.sa_sigaction = &DeliverGuestSignal;
    action.sa_flags = SA_SIGINFO | SA_RESTART;
    sigemptyset(&action.sa_mask);

    return sigaction(number, &action, nullptr) == 0;
}

static AbiBridge::Result InstallSignalActionHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    int number = (int)(int32_t)(uint32_t)registers.general[0];
    uint64_t requested = registers.general[1];
    uint64_t previous = registers.general[2];

    if (number <= 0 || number >= GUEST_SIGNAL_COUNT)
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (previous != 0)
    {
        uint8_t described[GUEST_ACTION_BYTES];
        memset(described, 0, sizeof(described));

        uint64_t handler = gGuestSignalHandlers[number].load(std::memory_order_acquire);
        uint32_t flags = gGuestSignalFlags[number].load(std::memory_order_acquire);

        memcpy(described + GUEST_ACTION_HANDLER_OFFSET, &handler, sizeof(handler));
        memcpy(described + GUEST_ACTION_FLAGS_OFFSET, &flags, sizeof(flags));

        if (!memory.Write(memory.context, (GuestState::GuestAddress)previous, described, sizeof(described)))
            return AbiBridge::Result::MalformedArguments;
    }

    if (requested == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint8_t wanted[GUEST_ACTION_BYTES];
    if (!memory.Read(memory.context, (GuestState::GuestAddress)requested, wanted, sizeof(wanted)))
        return AbiBridge::Result::MalformedArguments;

    uint64_t handler = 0;
    uint32_t flags = 0;
    memcpy(&handler, wanted + GUEST_ACTION_HANDLER_OFFSET, sizeof(handler));
    memcpy(&flags, wanted + GUEST_ACTION_FLAGS_OFFSET, sizeof(flags));

    gGuestSignalHandlers[number].store(handler, std::memory_order_release);
    gGuestSignalFlags[number].store(flags, std::memory_order_release);

    if (SignalBelongsToTheRuntime(number))
    {
        bool wanted = Settings::Lookup(GUEST_FAULT_SIGNAL_VARIABLE) != nullptr;
        gGuestFaultHandlersWanted[number].store(wanted, std::memory_order_release);

        fprintf(stderr,
                "[guestlibc] the guest asked to handle signal %d; the runtime keeps that signal so that faults in translated code are "
                "still reported, and %s\n",
                number,
                wanted ? "will offer the guest arena faults first, because RUNTIME_GUEST_FAULT_SIGNALS is set"
                       : "will not pass faults to the guest");

        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    if (handler != GUEST_SIGNAL_DEFAULT && handler != GUEST_SIGNAL_IGNORE && !InstallHostSignalHandler(number))
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result InstallSignalHandlerHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    int number = (int)(int32_t)(uint32_t)registers.general[0];
    uint64_t handler = registers.general[1];

    if (number <= 0 || number >= GUEST_SIGNAL_COUNT)
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    uint64_t established = gGuestSignalHandlers[number].exchange(handler, std::memory_order_acq_rel);

    if (!SignalBelongsToTheRuntime(number) && handler != GUEST_SIGNAL_DEFAULT && handler != GUEST_SIGNAL_IGNORE)
        InstallHostSignalHandler(number);

    registers.general[0] = established;
    return AbiBridge::Result::Ok;
}

static bool ReadGuestSignalSet(const GuestState::MemoryAccessor& memory, uint64_t address, sigset_t& wanted)
{
    sigemptyset(&wanted);

    uint64_t bits = 0;
    if (!memory.Read(memory.context, (GuestState::GuestAddress)address, &bits, sizeof(bits))) return false;

    for (int number = 1; number < GUEST_SIGNAL_COUNT; ++number)
    {
        if ((bits & (1ull << (number - 1))) != 0) sigaddset(&wanted, number);
    }

    return true;
}

static bool WriteGuestSignalSet(const GuestState::MemoryAccessor& memory, uint64_t address, const sigset_t& established)
{
    uint64_t bits = 0;
    for (int number = 1; number < GUEST_SIGNAL_COUNT; ++number)
    {
        if (sigismember(&established, number) == 1) bits |= 1ull << (number - 1);
    }

    return memory.Write(memory.context, (GuestState::GuestAddress)address, &bits, sizeof(bits));
}

static AbiBridge::Result SignalSetFillHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t bits = context != nullptr ? ~0ull : 0ull;
    if (registers.general[0] != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)registers.general[0], &bits, sizeof(bits)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SignalSetChangeHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t address = registers.general[0];
    int number = (int)(int32_t)(uint32_t)registers.general[1];
    bool adding = context != nullptr;

    if (address == 0 || number <= 0 || number >= GUEST_SIGNAL_COUNT)
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    uint64_t bits = 0;
    if (!memory.Read(memory.context, (GuestState::GuestAddress)address, &bits, sizeof(bits))) return AbiBridge::Result::MalformedArguments;

    if (adding) bits |= 1ull << (number - 1);
    else bits &= ~(1ull << (number - 1));

    if (!memory.Write(memory.context, (GuestState::GuestAddress)address, &bits, sizeof(bits)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SignalMaskHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    int how = (int)(int32_t)(uint32_t)registers.general[0];
    uint64_t requested = registers.general[1];
    uint64_t previous = registers.general[2];

    sigset_t wanted;
    sigemptyset(&wanted);
    if (requested != 0 && !ReadGuestSignalSet(memory, requested, wanted)) return AbiBridge::Result::MalformedArguments;

    sigset_t established;
    sigemptyset(&established);

    int result = pthread_sigmask(how, requested != 0 ? &wanted : nullptr, &established);

    if (previous != 0 && !WriteGuestSignalSet(memory, previous, established)) return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(uint32_t)result;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SignalSuspendHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    sigset_t wanted;
    sigemptyset(&wanted);

    if (registers.general[0] != 0 && !ReadGuestSignalSet(memory, registers.general[0], wanted))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(int64_t)sigsuspend(&wanted);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadSignalSendHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    pthread_t thread = (pthread_t)registers.general[0];
    int number = (int)(int32_t)(uint32_t)registers.general[1];

    registers.general[0] = (uint64_t)(uint32_t)pthread_kill(thread, number);
    return AbiBridge::Result::Ok;
}

static GuestSyscall::Statistics gLibcSyscallStatistics;

static AbiBridge::Result SyscallHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestState::Registers shifted = registers;
    shifted.general[8] = registers.general[0];

    for (int index = 0; index < 5; ++index) shifted.general[index] = registers.general[index + 1];

    GuestSyscall::Result serviced = GuestSyscall::Dispatch(shifted, memory, gLibcSyscallStatistics);

    if (serviced == GuestSyscall::Result::GuestMemoryFault) return AbiBridge::Result::MalformedArguments;

    if (serviced != GuestSyscall::Result::Ok)
    {
        registers.general[0] = (uint64_t)(-(int64_t)ENOSYS);
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = shifted.general[0];
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadLocalCreateHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t keyOut = registers.general[0];
    uint32_t key = (uint32_t)gNextThreadLocalKey.fetch_add(1);

    if (keyOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)keyOut, &key, sizeof(key)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadLocalSetHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    gThreadLocalValues[(uint32_t)registers.general[0]] = registers.general[1];

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadLocalGetHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    auto found = gThreadLocalValues.find((uint32_t)registers.general[0]);
    registers.general[0] = found == gThreadLocalValues.end() ? 0 : found->second;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result PthreadJoinHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance* instance = (GuestRuntime::Instance*)context;

    uint64_t handle = registers.general[0];
    uint64_t valueOut = registers.general[1];

    uint64_t returned = 0;
    GuestRuntime::Result joined = GuestRuntime::JoinThread(*instance, handle, returned);

    if (joined != GuestRuntime::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)ESRCH;
        return AbiBridge::Result::Ok;
    }

    if (valueOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)valueOut, &returned, sizeof(returned)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static void* GuestMemsetChecked(void* destination, int value, size_t length, size_t capacity)
{
    if (length > capacity)
    {
        fprintf(stderr, "[guestlibc] __memset_chk caught a %zu byte write into an object of %zu bytes\n", length, capacity);
        abort();
    }

    return memset(destination, value, length);
}

static void* GuestMemcpyChecked(void* destination, const void* source, size_t length, size_t capacity)
{
    if (length > capacity)
    {
        fprintf(stderr, "[guestlibc] __memcpy_chk caught a %zu byte write into an object of %zu bytes\n", length, capacity);
        abort();
    }

    return memcpy(destination, source, length);
}

[[noreturn]] static void GuestFortifyFailure(const char* name, size_t length, size_t capacity)
{
    fprintf(stderr, "[guestlibc] %s caught a %zu byte access to an object of %zu bytes\n", name, length, capacity);
    abort();
}

static void* GuestMemmoveChecked(void* destination, const void* source, size_t length, size_t capacity)
{
    if (length > capacity) GuestFortifyFailure("__memmove_chk", length, capacity);

    return memmove(destination, source, length);
}

static size_t GuestStrlenChecked(const char* text, size_t capacity)
{
    size_t length = strlen(text);
    if (length >= capacity) GuestFortifyFailure("__strlen_chk", length + 1, capacity);

    return length;
}

static char* GuestStrchrChecked(const char* text, int character, size_t capacity)
{
    (void)capacity;
    return (char*)strchr(text, character);
}

static char* GuestStrrchrChecked(const char* text, int character, size_t capacity)
{
    (void)capacity;
    return (char*)strrchr(text, character);
}

static char* GuestStrncpyChecked(char* destination, const char* source, size_t length, size_t capacity)
{
    if (length > capacity) GuestFortifyFailure("__strncpy_chk", length, capacity);

    return strncpy(destination, source, length);
}

static char* GuestStrncpyCheckedBoth(char* destination, const char* source, size_t length, size_t destinationCapacity,
                                     size_t sourceCapacity)
{
    (void)sourceCapacity;
    if (length > destinationCapacity) GuestFortifyFailure("__strncpy_chk2", length, destinationCapacity);

    return strncpy(destination, source, length);
}

static char* GuestStrcpyChecked(char* destination, const char* source, size_t capacity)
{
    size_t length = strlen(source) + 1;
    if (length > capacity) GuestFortifyFailure("__strcpy_chk", length, capacity);

    return (char*)memcpy(destination, source, length);
}

static char* GuestStpcpyChecked(char* destination, const char* source, size_t capacity)
{
    size_t length = strlen(source);
    if (length + 1 > capacity) GuestFortifyFailure("__stpcpy_chk", length + 1, capacity);

    memcpy(destination, source, length + 1);
    return destination + length;
}

static char* GuestStrcatChecked(char* destination, const char* source, size_t capacity)
{
    size_t existing = strlen(destination);
    size_t appended = strlen(source);
    if (existing + appended + 1 > capacity) GuestFortifyFailure("__strcat_chk", existing + appended + 1, capacity);

    memcpy(destination + existing, source, appended + 1);
    return destination;
}

static char* GuestStrncatChecked(char* destination, const char* source, size_t length, size_t capacity)
{
    size_t existing = strlen(destination);
    size_t appended = strnlen(source, length);
    if (existing + appended + 1 > capacity) GuestFortifyFailure("__strncat_chk", existing + appended + 1, capacity);

    memcpy(destination + existing, source, appended);
    destination[existing + appended] = '\0';
    return destination;
}

static size_t GuestStrlcpyChecked(char* destination, const char* source, size_t size, size_t capacity)
{
    if (size > capacity) GuestFortifyFailure("__strlcpy_chk", size, capacity);

    return (size_t)HostBridge::StringCopyTerminated(destination, source, size);
}

static int64_t GuestReadChecked(int descriptor, void* buffer, size_t count, size_t capacity)
{
    if (count > capacity) GuestFortifyFailure("__read_chk", count, capacity);

    return HostBridge::ReadDescriptor(descriptor, buffer, count);
}

static int64_t GuestWriteChecked(int descriptor, const void* buffer, size_t count, size_t capacity)
{
    if (count > capacity) GuestFortifyFailure("__write_chk", count, capacity);

    return HostBridge::WriteDescriptor(descriptor, buffer, count);
}

static int64_t GuestPreadChecked(int descriptor, void* buffer, size_t count, int64_t offset, size_t capacity)
{
    if (count > capacity) GuestFortifyFailure("__pread_chk", count, capacity);

    return HostBridge::ReadDescriptorAt(descriptor, buffer, count, offset);
}

static int64_t GuestReadlinkChecked(const char* path, char* buffer, size_t size, size_t capacity)
{
    if (size > capacity) GuestFortifyFailure("__readlink_chk", size, capacity);

    return HostBridge::ReadLinkTarget(path, buffer, size);
}

static char* GuestFgetsChecked(char* buffer, int size, void* stream, size_t capacity)
{
    if (size > 0 && (size_t)size > capacity) GuestFortifyFailure("__fgets_chk", (size_t)size, capacity);

    return HostBridge::ReadStreamLine(buffer, size, stream);
}

static size_t GuestFreadChecked(void* buffer, size_t size, size_t count, void* stream, size_t capacity)
{
    if (size != 0 && count > capacity / size) GuestFortifyFailure("__fread_chk", size * count, capacity);

    return (size_t)HostBridge::ReadStream(buffer, size, count, stream);
}

static int GuestOpenChecked(const char* path, int flags)
{
    return HostBridge::OpenFile(path, flags, 0);
}

static int GuestOpenAtChecked(int directory, const char* path, int flags)
{
    return HostBridge::OpenFileAt(directory, path, flags, 0);
}

static uint32_t GuestUmaskChecked(uint32_t mask)
{
    return HostBridge::ChangeCreationMask(mask & 0777);
}

static std::mutex environmentMutex;
static std::map<std::string, uint64_t> publishedEnvironment;
static std::map<std::string, std::string> guestEnvironment;

void GuestLibc::SetEnvironmentValue(const std::string& name, const std::string& value)
{
    std::lock_guard<std::mutex> guard(environmentMutex);
    guestEnvironment[name] = value;
    publishedEnvironment.erase(name);
}

void GuestLibc::DescribeAndroidEnvironment(const std::string& dataDirectory, const std::string& externalDirectory,
                                           const std::string& packageName)
{
    HostBridge::MapAndroidTree(dataDirectory.c_str(), externalDirectory.c_str(), ANDROID_SYSTEM_DIRECTORY, packageName.c_str());
    if (!packageName.empty()) HostBridge::SetProgramName(packageName.c_str());

    SetEnvironmentValue("ANDROID_BOOTLOGO", "1");
    SetEnvironmentValue("ANDROID_ROOT", "/system");
    SetEnvironmentValue("ANDROID_ASSETS", "/system/app");
    SetEnvironmentValue("ANDROID_ART_ROOT", "/apex/com.android.art");
    SetEnvironmentValue("ANDROID_I18N_ROOT", "/apex/com.android.i18n");
    SetEnvironmentValue("ANDROID_TZDATA_ROOT", "/apex/com.android.tzdata");
    SetEnvironmentValue("ASEC_MOUNTPOINT", "/mnt/asec");
    SetEnvironmentValue("MEMTAG_OPTIONS", "off");
    SetEnvironmentValue("PATH", "/system/bin:/system/xbin");

    if (!dataDirectory.empty() && !packageName.empty())
    {
        SetEnvironmentValue("ANDROID_DATA", "/data");
        SetEnvironmentValue("DOWNLOAD_CACHE", "/data/data/" + packageName + "/cache");
    }

    if (!externalDirectory.empty())
    {
        SetEnvironmentValue("EXTERNAL_STORAGE", ANDROID_EXTERNAL_DIRECTORY);
        SetEnvironmentValue("ANDROID_STORAGE", "/storage");
    }

    const char* injected = Settings::Lookup(GUEST_ENVIRONMENT_SETTING);
    if (!injected) return;

    std::string pending;
    for (const char* cursor = injected;; ++cursor)
    {
        if (*cursor != '\0' && *cursor != ';')
        {
            pending.push_back(*cursor);
            continue;
        }

        size_t equals = pending.find('=');
        if (equals != std::string::npos && equals > 0)
            SetEnvironmentValue(pending.substr(0, equals), pending.substr(equals + 1));

        pending.clear();
        if (*cursor == '\0') break;
    }
}

static AbiBridge::Result GetenvHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    std::string name;
    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, name))
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::lock_guard<std::mutex> guard(environmentMutex);

    std::map<std::string, uint64_t>::const_iterator cached = publishedEnvironment.find(name);
    if (cached != publishedEnvironment.end())
    {
        registers.general[0] = cached->second;
        return AbiBridge::Result::Ok;
    }

    std::map<std::string, std::string>::const_iterator entry = guestEnvironment.find(name);
    if (entry == guestEnvironment.end())
    {
        publishedEnvironment[name] = 0;
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    const char* value = entry->second.c_str();
    uint64_t bytes = strlen(value) + 1;
    GuestState::GuestAddress address{};

    if (GuestRuntime::ReserveScratch(instance, bytes, sizeof(uint64_t), address) != GuestRuntime::Result::Ok ||
        GuestRuntime::Commit(instance, address, bytes, true, true, false) != GuestRuntime::Result::Ok ||
        !memory.Write(memory.context, address, value, bytes))
    {
        fprintf(stderr, "[guestlibc] cannot publish the value of %s into guest memory\n", name.c_str());
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    publishedEnvironment[name] = (uint64_t)address;
    registers.general[0] = (uint64_t)address;
    return AbiBridge::Result::Ok;
}

static bool HostStreamFor(GuestRuntime::Instance& instance, uint64_t guestPointer, FILE*& stream)
{
    if (guestPointer == 0) return false;

    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(instance);

    GuestState::GuestAddress base{};
    if (AbiBridge::LookupGuestData(registry, "__sF", base) == AbiBridge::Result::Ok)
    {
        uint64_t rawBase = (uint64_t)base;
        uint64_t span = GUEST_STANDARD_STREAM_COUNT * GUEST_FILE_STRIDE_BYTES;

        if (guestPointer >= rawBase && guestPointer < rawBase + span)
        {
            uint64_t index = (guestPointer - rawBase) / GUEST_FILE_STRIDE_BYTES;

            if (index == 0) stream = stdin;
            else if (index == 1) stream = stdout;
            else stream = stderr;

            return true;
        }
    }

    stream = (FILE*)guestPointer;
    return true;
}

static bool UnknownStream(uint64_t guestPointer, const char* name)
{
    fprintf(stderr,
            "[guestlibc] %s was called on FILE* %016llx, which is not one of the three __sF streams. Guest-opened streams are not "
            "modelled.\n",
            name, (unsigned long long)guestPointer);
    return false;
}

#if defined(__x86_64__)
static AbiBridge::Result FprintfHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t guestStream = registers.general[0];
    FILE* stream = nullptr;
    if (!HostStreamFor(instance, guestStream, stream))
    {
        UnknownStream(guestStream, "fprintf");
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, 2, 1, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    fwrite(output.data(), 1, output.size(), stream);
    registers.general[0] = (uint64_t)(uint32_t)produced;

    return AbiBridge::Result::Ok;
}
#endif

static AbiBridge::Result FwriteHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t source = registers.general[0];
    uint64_t itemBytes = registers.general[1];
    uint64_t itemCount = registers.general[2];
    uint64_t guestStream = registers.general[3];

    FILE* stream = nullptr;
    if (!HostStreamFor(instance, guestStream, stream))
    {
        UnknownStream(guestStream, "fwrite");
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    if (itemBytes == 0 || itemCount == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t total = itemBytes * itemCount;
    if (total / itemBytes != itemCount) return AbiBridge::Result::MalformedArguments;

    std::vector<uint8_t> bytes((size_t)total);
    if (!memory.Read(memory.context, (GuestState::GuestAddress)source, bytes.data(), total)) return AbiBridge::Result::MalformedArguments;

    registers.general[0] = fwrite(bytes.data(), (size_t)itemBytes, (size_t)itemCount, stream);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result FputcHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)memory;
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    int character = (int)(int32_t)(uint32_t)registers.general[0];
    uint64_t guestStream = registers.general[1];

    FILE* stream = nullptr;
    if (!HostStreamFor(instance, guestStream, stream))
    {
        UnknownStream(guestStream, "fputc");
        registers.general[0] = (uint64_t)(uint32_t)EOF;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)(uint32_t)fputc(character, stream);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result PutcharHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    int character = (int)(int32_t)(uint32_t)registers.general[0];
    registers.general[0] = (uint64_t)(uint32_t)fputc(character, stdout);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result RewindHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    FILE* stream = nullptr;
    if (HostStreamFor(instance, registers.general[0], stream)) rewind(stream);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamGetPositionHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    FILE* stream = nullptr;
    uint64_t positionOut = registers.general[1];
    if (positionOut == 0 || !HostStreamFor(instance, registers.general[0], stream))
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    int64_t position = (int64_t)ftello(stream);
    if (position < 0)
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (!memory.Write(memory.context, (GuestState::GuestAddress)positionOut, &position, sizeof(position)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamSetPositionHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    FILE* stream = nullptr;
    uint64_t positionIn = registers.general[1];
    if (positionIn == 0 || !HostStreamFor(instance, registers.general[0], stream))
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    int64_t position = 0;
    if (!memory.Read(memory.context, (GuestState::GuestAddress)positionIn, &position, sizeof(position)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(int64_t)fseeko(stream, (off_t)position, SEEK_SET);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AndroidLogBufferWriteHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    std::string tag;
    std::string text;

    if (!ReadGuestString(memory, registers.general[2], GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";
    if (!ReadGuestString(memory, registers.general[3], GuestLibc::MAX_FORMAT_LENGTH, text)) text.clear();

    fprintf(stderr, "[guest %s] %s\n", tag.c_str(), text.c_str());
    registers.general[0] = (uint64_t)(uint32_t)text.size();
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LongDivideHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    int64_t numerator = (int64_t)registers.general[0];
    int64_t denominator = (int64_t)registers.general[1];

    if (denominator == 0 || (denominator == -1 && numerator == INT64_MIN))
    {
        registers.general[0] = 0;
        registers.general[1] = 0;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)(numerator / denominator);
    registers.general[1] = (uint64_t)(numerator % denominator);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ZeroedIntegerOutHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t valueOut = registers.general[1];
    int32_t zero = 0;

    if (valueOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)valueOut, &zero, sizeof(zero)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ThreadSchedulingHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t policyOut = registers.general[1];
    uint64_t parameterOut = registers.general[2];
    int32_t zero = 0;

    if (policyOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)policyOut, &zero, sizeof(zero)))
        return AbiBridge::Result::MalformedArguments;

    if (parameterOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)parameterOut, &zero, sizeof(zero)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static void FillRandom(void* destination, uint64_t length)
{
    uint8_t* cursor = (uint8_t*)destination;

    while (length != 0)
    {
        ssize_t got = getrandom(cursor, (size_t)length, 0);
        if (got <= 0) continue;

        cursor += got;
        length -= (uint64_t)got;
    }
}

static AbiBridge::Result RandomWordHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint32_t value = 0;
    FillRandom(&value, sizeof(value));

    registers.general[0] = value;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result RandomUniformHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint32_t upper = (uint32_t)registers.general[0];

    if (upper < 2)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint32_t floor = (0u - upper) % upper;
    uint32_t value = 0;

    do
    {
        FillRandom(&value, sizeof(value));
    } while (value < floor);

    registers.general[0] = value % upper;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result RandomBufferHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t destination = registers.general[0];
    uint64_t length = registers.general[1];

    if (destination == 0 || length == 0) return AbiBridge::Result::Ok;

    std::vector<uint8_t> bytes((size_t)length);
    FillRandom(bytes.data(), length);

    if (!memory.Write(memory.context, (GuestState::GuestAddress)destination, bytes.data(), length)) return AbiBridge::Result::MalformedArguments;

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SignalSetMemberHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t address = registers.general[0];
    int number = (int)(int32_t)(uint32_t)registers.general[1];

    if (address == 0 || number <= 0 || number >= GUEST_SIGNAL_COUNT)
    {
        errno = EINVAL;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    uint64_t bits = 0;
    if (!memory.Read(memory.context, (GuestState::GuestAddress)address, &bits, sizeof(bits))) return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (bits >> (number - 1)) & 1;
    return AbiBridge::Result::Ok;
}

static constexpr uint32_t GUEST_ROUNDING_MASK = 0x00c00000u;
static constexpr uint32_t GUEST_EXCEPTION_MASK = 0x1fu;

struct GuestFloatingEnvironment
{
    uint32_t control;
    uint32_t status;
};

static AbiBridge::Result RoundingGetHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = registers.floatingPointControl & GUEST_ROUNDING_MASK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result RoundingSetHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    uint32_t wanted = (uint32_t)registers.general[0];
    if ((wanted & ~GUEST_ROUNDING_MASK) != 0)
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    registers.floatingPointControl = (registers.floatingPointControl & ~GUEST_ROUNDING_MASK) | wanted;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result EnvironmentGetHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    bool holding = context != nullptr;
    uint64_t environmentOut = registers.general[0];

    GuestFloatingEnvironment saved{registers.floatingPointControl, registers.floatingPointStatus};
    if (environmentOut == 0 || !memory.Write(memory.context, (GuestState::GuestAddress)environmentOut, &saved, sizeof(saved)))
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    if (holding) registers.floatingPointStatus &= ~GUEST_EXCEPTION_MASK;

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result EnvironmentSetHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    bool updating = context != nullptr;
    uint64_t environmentIn = registers.general[0];

    GuestFloatingEnvironment wanted{0, 0};
    if (environmentIn != 0 && !memory.Read(memory.context, (GuestState::GuestAddress)environmentIn, &wanted, sizeof(wanted)))
    {
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    uint32_t raised = registers.floatingPointStatus & GUEST_EXCEPTION_MASK;
    registers.floatingPointControl = wanted.control;
    registers.floatingPointStatus = wanted.status | (updating ? raised : 0);

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ExceptionClearHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.floatingPointStatus &= ~((uint32_t)registers.general[0] & GUEST_EXCEPTION_MASK);
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ExceptionRaiseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.floatingPointStatus |= (uint32_t)registers.general[0] & GUEST_EXCEPTION_MASK;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ExceptionTestHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = registers.floatingPointStatus & (uint32_t)registers.general[0] & GUEST_EXCEPTION_MASK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result CompressStartDetailedHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    int32_t produced = HostBridge::CompressStartDetailed(
        (void*)registers.general[0], (int32_t)registers.general[1], (int32_t)registers.general[2], (int32_t)registers.general[3],
        (int32_t)registers.general[4], (int32_t)registers.general[5], (const char*)registers.general[6], (int32_t)registers.general[7]);

    registers.general[0] = (uint64_t)(int64_t)produced;
    return AbiBridge::Result::Ok;
}

struct GuestStreamCookie
{
    uint64_t cookie;
    uint64_t readFunction;
    uint64_t writeFunction;
    uint64_t seekFunction;
    uint64_t closeFunction;
};

static constexpr size_t GUEST_STREAM_CHUNK_BYTES = 1u << 30;

static ssize_t GuestStreamRead(void* context, char* buffer, size_t size)
{
    GuestStreamCookie* stream = (GuestStreamCookie*)context;
    if (stream->readFunction == 0)
    {
        errno = EBADF;
        return -1;
    }

    uint64_t arguments[3] = {stream->cookie, (uint64_t)buffer, (uint64_t)(size < GUEST_STREAM_CHUNK_BYTES ? size : GUEST_STREAM_CHUNK_BYTES)};
    uint64_t returned = 0;

    if (GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)stream->readFunction, arguments, 3, returned) !=
        GuestRuntime::Result::Ok)
    {
        errno = EIO;
        return -1;
    }

    return (ssize_t)(int32_t)(uint32_t)returned;
}

static ssize_t GuestStreamWrite(void* context, const char* buffer, size_t size)
{
    GuestStreamCookie* stream = (GuestStreamCookie*)context;
    if (stream->writeFunction == 0)
    {
        errno = EBADF;
        return -1;
    }

    uint64_t arguments[3] = {stream->cookie, (uint64_t)buffer, (uint64_t)(size < GUEST_STREAM_CHUNK_BYTES ? size : GUEST_STREAM_CHUNK_BYTES)};
    uint64_t returned = 0;

    if (GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)stream->writeFunction, arguments, 3, returned) !=
        GuestRuntime::Result::Ok)
    {
        errno = EIO;
        return -1;
    }

    int32_t written = (int32_t)(uint32_t)returned;
    return written < 0 ? -1 : (ssize_t)written;
}

static int GuestStreamSeek(void* context, off64_t* offset, int whence)
{
    GuestStreamCookie* stream = (GuestStreamCookie*)context;
    if (stream->seekFunction == 0 || !offset)
    {
        errno = ESPIPE;
        return -1;
    }

    uint64_t arguments[3] = {stream->cookie, (uint64_t)*offset, (uint64_t)(int64_t)whence};
    uint64_t returned = 0;

    if (GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)stream->seekFunction, arguments, 3, returned) !=
        GuestRuntime::Result::Ok)
    {
        errno = EIO;
        return -1;
    }

    int64_t position = (int64_t)returned;
    if (position < 0) return -1;

    *offset = position;
    return 0;
}

static int GuestStreamClose(void* context)
{
    GuestStreamCookie* stream = (GuestStreamCookie*)context;
    int status = 0;

    if (stream->closeFunction != 0)
    {
        uint64_t arguments[1] = {stream->cookie};
        uint64_t returned = 0;

        if (GuestRuntime::CallGuestOnCurrentThread((GuestState::GuestAddress)stream->closeFunction, arguments, 1, returned) !=
            GuestRuntime::Result::Ok)
        {
            status = -1;
        }
        else
        {
            status = (int32_t)(uint32_t)returned;
        }
    }

    delete stream;
    return status;
}

static AbiBridge::Result GuestStreamOpenHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestStreamCookie* stream = new GuestStreamCookie{registers.general[0], registers.general[1], registers.general[2],
                                                      registers.general[3], registers.general[4]};

    if (stream->readFunction == 0 && stream->writeFunction == 0)
    {
        delete stream;
        errno = EINVAL;
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    const char* mode = stream->readFunction != 0 && stream->writeFunction != 0 ? "r+" : (stream->readFunction != 0 ? "r" : "w");

    cookie_io_functions_t functions{};
    functions.read = &GuestStreamRead;
    functions.write = &GuestStreamWrite;
    functions.seek = &GuestStreamSeek;
    functions.close = &GuestStreamClose;

    FILE* opened = fopencookie(stream, mode, functions);
    if (!opened) delete stream;

    registers.general[0] = (uint64_t)opened;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result FflushHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)memory;
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t guestStream = registers.general[0];
    if (guestStream == 0)
    {
        registers.general[0] = (uint64_t)(uint32_t)fflush(nullptr);
        return AbiBridge::Result::Ok;
    }

    FILE* stream = nullptr;
    if (!HostStreamFor(instance, guestStream, stream))
    {
        UnknownStream(guestStream, "fflush");
        registers.general[0] = (uint64_t)(uint32_t)EOF;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)(uint32_t)fflush(stream);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DeniedCallHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    errno = EACCES;
    registers.general[0] = (uint64_t)(int64_t)-1;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ProtectMemoryHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)memory;

    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t address = registers.general[0];
    uint64_t length = registers.general[1];
    int32_t protection = (int32_t)registers.general[2];

    GuestState::GuestAddress arenaBase{};
    uint64_t arenaLength = 0;
    GuestRuntime::ArenaBounds(instance, arenaBase, arenaLength);

    uint64_t base = (uint64_t)arenaBase;
    bool insideArena = length != 0 && address >= base && address - base < arenaLength;

    if (!insideArena)
    {
        registers.general[0] = (uint64_t)(int64_t)HostBridge::ProtectMemory((void*)address, length, protection);
        return AbiBridge::Result::Ok;
    }

    const AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(instance);
    if (AbiBridge::IsGuestStub(registry, (GuestState::GuestAddress)address) ||
        AbiBridge::IsGuestStub(registry, (GuestState::GuestAddress)(address + length - 1)))
    {
        errno = EACCES;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    GuestRuntime::Result changed = GuestRuntime::Protect(instance, (GuestState::GuestAddress)address, length, (protection & PROT_READ) != 0,
                                                         (protection & PROT_WRITE) != 0, (protection & PROT_EXEC) != 0);

    if (changed != GuestRuntime::Result::Ok)
    {
        errno = ENOMEM;
        registers.general[0] = (uint64_t)(int64_t)-1;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DeniedPointerHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    errno = EACCES;
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AndroidLogWriteHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string tag;
    std::string text;

    if (!ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";
    if (!ReadGuestString(memory, registers.general[2], GuestLibc::MAX_FORMAT_LENGTH, text)) text.clear();

    fprintf(stderr, "[guest %s] %s\n", tag.c_str(), text.c_str());
    registers.general[0] = (uint64_t)(uint32_t)text.size();
    return AbiBridge::Result::Ok;
}

#if defined(__x86_64__)
static AbiBridge::Result AndroidLogAssertHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string condition;
    std::string tag;

    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, condition)) condition = "?";
    if (!ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, tag)) tag = "?";

    std::string output;
    int produced = 0;

    if (registers.general[2] != 0 && FormatCall(registers, memory, 3, 2, output, produced) != AbiBridge::Result::Ok) output.clear();

    fprintf(stderr, "[guest %s] assertion failed: %s: %s\n", tag.c_str(), condition.c_str(), output.c_str());
    abort();
}

static GuestLibc::Result PlanScanFormat(const char* format, GuestLibc::FormatPlan& plan)
{
    plan.conversionCount = 0;
    plan.integerCount = 0;
    plan.floatCount = 0;

    if (!format) return GuestLibc::Result::BadFormat;

    for (const char* cursor = format; *cursor != '\0'; ++cursor)
    {
        if (*cursor != '%') continue;

        cursor += 1;
        if (*cursor == '%') continue;

        bool suppressed = false;
        if (*cursor == '*')
        {
            suppressed = true;
            cursor += 1;
        }

        while (*cursor >= '0' && *cursor <= '9') cursor += 1;
        while (*cursor == 'h' || *cursor == 'l' || *cursor == 'L' || *cursor == 'j' || *cursor == 'z' || *cursor == 't' || *cursor == 'q')
            cursor += 1;

        if (*cursor == '\0') return GuestLibc::Result::BadFormat;

        if (*cursor == '[')
        {
            cursor += 1;
            if (*cursor == '^') cursor += 1;
            if (*cursor == ']') cursor += 1;
            while (*cursor != '\0' && *cursor != ']') cursor += 1;
            if (*cursor == '\0') return GuestLibc::Result::BadFormat;
        }

        if (suppressed) continue;

        if (plan.conversionCount >= GuestLibc::MAX_FORMAT_ARGUMENTS) return GuestLibc::Result::TooManyArguments;

        plan.conversions[plan.conversionCount] = GuestLibc::ConversionClass::Pointer;
        plan.conversionCount += 1;
        plan.integerCount += 1;
    }

    return GuestLibc::Result::Ok;
}

static AbiBridge::Result ScanCall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory, int fixedArgumentCount,
                                  int formatArgumentIndex, const std::string& format, MarshalledCall& call)
{
    GuestLibc::FormatPlan plan;
    GuestLibc::Result parsed = PlanScanFormat(format.c_str(), plan);
    if (parsed != GuestLibc::Result::Ok)
    {
        fprintf(stderr, "[guestlibc] cannot marshal the scan format \"%s\": %s\n", format.c_str(), GuestLibc::ResultName(parsed));
        return AbiBridge::Result::UnsupportedSignature;
    }

    (void)formatArgumentIndex;

    GuestVarargs source;
    source.registers = &registers;
    source.memory = &memory;
    source.integerIndex = fixedArgumentCount;
    source.vectorIndex = 0;
    source.stackCursor = registers.stackPointer;

    if (fixedArgumentCount < 0)
    {
        uint64_t list = registers.general[-fixedArgumentCount - 1];

        uint64_t stack = 0;
        uint64_t generalTop = 0;
        uint64_t vectorTop = 0;
        int32_t generalOffset = 0;
        int32_t vectorOffset = 0;

        if (!memory.Read(memory.context, (GuestState::GuestAddress)list, &stack, sizeof(stack)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 8), &generalTop, sizeof(generalTop)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 16), &vectorTop, sizeof(vectorTop)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 24), &generalOffset, sizeof(generalOffset)) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(list + 28), &vectorOffset, sizeof(vectorOffset)))
        {
            return AbiBridge::Result::MalformedArguments;
        }

        source.fromArgumentList = true;
        source.stackCursor = stack;
        source.generalTop = generalTop;
        source.vectorTop = vectorTop;
        source.generalOffset = generalOffset;
        source.vectorOffset = vectorOffset;
    }

    if (MarshalVarargs(plan, source, call) != GuestLibc::Result::Ok) return AbiBridge::Result::MalformedArguments;

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ScanStringHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string input;
    std::string format;

    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, input) ||
        !ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, format))
    {
        return AbiBridge::Result::MalformedArguments;
    }

    MarshalledCall call;
    AbiBridge::Result marshalled = ScanCall(registers, memory, 2, 1, format, call);
    if (marshalled != AbiBridge::Result::Ok) return marshalled;

    va_list arguments;
    BuildVaList(call, arguments);

    registers.general[0] = (uint64_t)(uint32_t)vsscanf(input.c_str(), format.c_str(), arguments);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ScanArgumentListHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    std::string input;
    std::string format;

    if (!ReadGuestString(memory, registers.general[0], GuestLibc::MAX_FORMAT_LENGTH, input) ||
        !ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, format))
    {
        return AbiBridge::Result::MalformedArguments;
    }

    MarshalledCall call;
    AbiBridge::Result marshalled = ScanCall(registers, memory, -3, 1, format, call);
    if (marshalled != AbiBridge::Result::Ok) return marshalled;

    va_list arguments;
    BuildVaList(call, arguments);

    registers.general[0] = (uint64_t)(uint32_t)vsscanf(input.c_str(), format.c_str(), arguments);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ScanStreamHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    FILE* stream = nullptr;
    if (!HostStreamFor(instance, registers.general[0], stream))
    {
        UnknownStream(registers.general[0], "fscanf");
        registers.general[0] = (uint64_t)(uint32_t)EOF;
        return AbiBridge::Result::Ok;
    }

    std::string format;
    if (!ReadGuestString(memory, registers.general[1], GuestLibc::MAX_FORMAT_LENGTH, format)) return AbiBridge::Result::MalformedArguments;

    MarshalledCall call;
    AbiBridge::Result marshalled = ScanCall(registers, memory, 2, 1, format, call);
    if (marshalled != AbiBridge::Result::Ok) return marshalled;

    va_list arguments;
    BuildVaList(call, arguments);

    registers.general[0] = (uint64_t)(uint32_t)vfscanf(stream, format.c_str(), arguments);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AllocateFormattedHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t destination = registers.general[0];

    std::string output;
    int produced = 0;

    AbiBridge::Result result = FormatCall(registers, memory, -3, 1, output, produced);
    if (result != AbiBridge::Result::Ok)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    char* allocated = (char*)HostBridge::AllocateBytes(output.size() + 1);
    if (!allocated)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    memcpy(allocated, output.data(), output.size());
    allocated[output.size()] = '\0';

    uint64_t value = (uint64_t)allocated;
    if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, &value, sizeof(value)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WideFormatHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;

    uint64_t destination = registers.general[0];
    uint64_t limit = registers.general[1];
    uint64_t formatAddress = registers.general[2];

    std::vector<wchar_t> format;
    for (uint64_t offset = 0;; offset += sizeof(wchar_t))
    {
        wchar_t character = 0;
        if (!memory.Read(memory.context, (GuestState::GuestAddress)(formatAddress + offset), &character, sizeof(character)))
            return AbiBridge::Result::MalformedArguments;

        format.push_back(character);
        if (character == 0) break;
        if (format.size() > GuestLibc::MAX_FORMAT_LENGTH) return AbiBridge::Result::MalformedArguments;
    }

    std::string narrowed;
    for (wchar_t character : format) narrowed.push_back(character < 128 ? (char)character : '?');

    GuestLibc::FormatPlan plan;
    if (GuestLibc::ParseFormat(narrowed.c_str(), plan) != GuestLibc::Result::Ok) return AbiBridge::Result::UnsupportedSignature;

    GuestVarargs source;
    source.registers = &registers;
    source.memory = &memory;
    source.integerIndex = 3;
    source.vectorIndex = 0;
    source.stackCursor = registers.stackPointer;

    MarshalledCall call;
    if (MarshalVarargs(plan, source, call) != GuestLibc::Result::Ok) return AbiBridge::Result::MalformedArguments;

    if (limit == 0 || limit > GuestLibc::MAX_FORMAT_LENGTH) limit = GuestLibc::MAX_FORMAT_LENGTH;
    std::vector<wchar_t> buffer((size_t)limit);

    va_list arguments;
    BuildVaList(call, arguments);

    int produced = vswprintf(buffer.data(), buffer.size(), format.data(), arguments);
    if (produced < 0)
    {
        registers.general[0] = (uint64_t)(uint32_t)-1;
        return AbiBridge::Result::Ok;
    }

    uint64_t bytes = ((uint64_t)produced + 1) * sizeof(wchar_t);
    if (destination != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)destination, buffer.data(), bytes))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(uint32_t)produced;
    return AbiBridge::Result::Ok;
}
#endif

static AbiBridge::Result DescriptorControlHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)context;
    (void)memory;

    int descriptor = (int)(int32_t)(uint32_t)registers.general[0];
    unsigned long request = (unsigned long)registers.general[1];
    unsigned long argument = (unsigned long)registers.general[2];

    registers.general[0] = (uint64_t)(int64_t)ioctl(descriptor, request, argument);
    return AbiBridge::Result::Ok;
}

static uint64_t PublishGuestString(GuestRuntime::Instance& instance, const std::string& text);

static constexpr int MODULE_INFORMATION_ADDRESS = 0;
static constexpr int MODULE_INFORMATION_NAME = 8;
static constexpr int MODULE_INFORMATION_HEADERS = 16;
static constexpr int MODULE_INFORMATION_COUNT = 24;
static constexpr int MODULE_INFORMATION_ADDS = 32;
static constexpr int MODULE_INFORMATION_SUBS = 40;
static constexpr int MODULE_INFORMATION_TLS_MODULE = 48;
static constexpr int MODULE_INFORMATION_TLS_DATA = 56;
static constexpr uint64_t MODULE_INFORMATION_BYTES = 64;
static constexpr uint64_t MAX_SORT_ELEMENT_BYTES = 4096;
static constexpr uint64_t MAX_SORT_ELEMENTS = 1u << 24;

static AbiBridge::Result IteratePhdrHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t callback = registers.general[0];
    uint64_t opaque = registers.general[1];

    if (callback == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    std::vector<GuestRuntime::Module> modules;
    GuestRuntime::DescribeModules(instance, modules);

    GuestState::GuestAddress described{};
    if (GuestRuntime::ReserveScratch(instance, MODULE_INFORMATION_BYTES, 16, described) != GuestRuntime::Result::Ok ||
        GuestRuntime::Commit(instance, described, MODULE_INFORMATION_BYTES, true, true, false) != GuestRuntime::Result::Ok)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t answered = 0;

    for (const GuestRuntime::Module& module : modules)
    {
        if (module.programHeaderCount == 0) continue;

        uint8_t information[MODULE_INFORMATION_BYTES];
        memset(information, 0, sizeof(information));

        uint64_t name = PublishGuestString(instance, module.path);
        uint16_t count = (uint16_t)module.programHeaderCount;

        memcpy(information + MODULE_INFORMATION_ADDRESS, &module.loadBias, sizeof(module.loadBias));
        memcpy(information + MODULE_INFORMATION_NAME, &name, sizeof(name));
        memcpy(information + MODULE_INFORMATION_HEADERS, &module.programHeaderAddress, sizeof(module.programHeaderAddress));
        memcpy(information + MODULE_INFORMATION_COUNT, &count, sizeof(count));
        memcpy(information + MODULE_INFORMATION_ADDS, &module.loadEventCount, sizeof(module.loadEventCount));

        uint64_t zero = 0;
        memcpy(information + MODULE_INFORMATION_SUBS, &zero, sizeof(zero));
        memcpy(information + MODULE_INFORMATION_TLS_MODULE, &module.threadLocalModuleId, sizeof(module.threadLocalModuleId));
        memcpy(information + MODULE_INFORMATION_TLS_DATA, &zero, sizeof(zero));

        if (!memory.Write(memory.context, described, information, sizeof(information))) return AbiBridge::Result::MalformedArguments;

        uint64_t arguments[3] = {(uint64_t)described, MODULE_INFORMATION_BYTES, opaque};
        uint64_t returned = 0;

        GuestRuntime::Result called =
            GuestRuntime::CallGuestNested(instance, registers, (GuestState::GuestAddress)callback, arguments, 3, returned);

        if (called != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[guestlibc] the dl_iterate_phdr callback at %016llx failed: %s\n", (unsigned long long)callback,
                    GuestRuntime::ResultName(called));
            return AbiBridge::Result::GuestFaulted;
        }

        answered = (uint64_t)(uint32_t)returned;
        if (answered != 0) break;
    }

    registers.general[0] = answered;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SearchArrayHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)memory;
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t key = registers.general[0];
    uint64_t base = registers.general[1];
    uint64_t count = registers.general[2];
    uint64_t width = registers.general[3];
    uint64_t comparator = registers.general[4];

    if (base == 0 || width == 0 || comparator == 0 || count == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t low = 0;
    uint64_t high = count;
    uint64_t found = 0;

    while (low < high)
    {
        uint64_t middle = low + (high - low) / 2;
        uint64_t element = base + middle * width;

        uint64_t arguments[2] = {key, element};
        uint64_t returned = 0;

        if (GuestRuntime::CallGuestNested(instance, registers, (GuestState::GuestAddress)comparator, arguments, 2, returned) !=
            GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[guestlibc] the bsearch comparator at %016llx faulted\n", (unsigned long long)comparator);
            return AbiBridge::Result::GuestFaulted;
        }

        int32_t ordering = (int32_t)(uint32_t)returned;
        if (ordering == 0)
        {
            found = element;
            break;
        }

        if (ordering < 0) high = middle;
        else low = middle + 1;
    }

    registers.general[0] = found;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SortArrayHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t base = registers.general[0];
    uint64_t count = registers.general[1];
    uint64_t width = registers.general[2];
    uint64_t comparator = registers.general[3];

    registers.general[0] = 0;

    if (base == 0 || width == 0 || comparator == 0 || count < 2) return AbiBridge::Result::Ok;
    if (width > MAX_SORT_ELEMENT_BYTES || count > MAX_SORT_ELEMENTS) return AbiBridge::Result::MalformedArguments;

    bool faulted = false;
    auto precedes = [&](uint64_t left, uint64_t right) {
        if (faulted) return false;

        uint64_t arguments[2] = {base + left * width, base + right * width};
        uint64_t returned = 0;

        if (GuestRuntime::CallGuestNested(instance, registers, (GuestState::GuestAddress)comparator, arguments, 2, returned) !=
            GuestRuntime::Result::Ok)
        {
            faulted = true;
            return false;
        }

        return (int32_t)(uint32_t)returned < 0;
    };

    std::vector<uint64_t> order(count);
    for (uint64_t index = 0; index < count; ++index) order[index] = index;

    std::vector<uint64_t> merged(count);

    for (uint64_t run = 1; run < count && !faulted; run *= 2)
    {
        for (uint64_t start = 0; start < count; start += run * 2)
        {
            uint64_t middle = start + run < count ? start + run : count;
            uint64_t end = start + run * 2 < count ? start + run * 2 : count;

            uint64_t left = start;
            uint64_t right = middle;
            uint64_t cursor = start;

            while (left < middle && right < end) merged[cursor++] = precedes(order[right], order[left]) ? order[right++] : order[left++];
            while (left < middle) merged[cursor++] = order[left++];
            while (right < end) merged[cursor++] = order[right++];
        }

        order.swap(merged);
    }

    if (faulted)
    {
        fprintf(stderr, "[guestlibc] the qsort comparator at %016llx faulted\n", (unsigned long long)comparator);
        return AbiBridge::Result::GuestFaulted;
    }

    std::vector<uint8_t> original(count * width);
    if (!memory.Read(memory.context, (GuestState::GuestAddress)base, original.data(), original.size()))
        return AbiBridge::Result::MalformedArguments;

    std::vector<uint8_t> sorted(count * width);
    for (uint64_t index = 0; index < count; ++index)
        memcpy(sorted.data() + index * width, original.data() + order[index] * width, (size_t)width);

    if (!memory.Write(memory.context, (GuestState::GuestAddress)base, sorted.data(), sorted.size()))
        return AbiBridge::Result::MalformedArguments;

    return AbiBridge::Result::Ok;
}

static std::mutex gGuestStringLock;
static std::map<std::string, uint64_t> gGuestStrings;

static uint64_t PublishGuestString(GuestRuntime::Instance& instance, const std::string& text)
{
    std::lock_guard<std::mutex> guard(gGuestStringLock);

    auto found = gGuestStrings.find(text);
    if (found != gGuestStrings.end()) return found->second;

    GuestState::GuestAddress address{};
    if (GuestRuntime::ReserveScratch(instance, text.size() + 1, sizeof(uint64_t), address) != GuestRuntime::Result::Ok) return 0;
    if (GuestRuntime::Commit(instance, address, text.size() + 1, true, true, false) != GuestRuntime::Result::Ok) return 0;

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);
    if (!memory.Write(memory.context, address, text.c_str(), text.size() + 1)) return 0;

    gGuestStrings[text] = (uint64_t)address;
    return (uint64_t)address;
}

static AbiBridge::Result DescribeAddressHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    GuestRuntime::Instance& instance = *(GuestRuntime::Instance*)context;

    uint64_t address = registers.general[0];
    uint64_t out = registers.general[1];

    if (out == 0)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    GuestRuntime::Description description;
    if (GuestRuntime::DescribeAddress(instance, address, description) != GuestRuntime::Result::Ok)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t entry[4] = {PublishGuestString(instance, description.path), description.moduleBase, 0, 0};

    if (!description.symbolName.empty())
    {
        entry[2] = PublishGuestString(instance, description.symbolName);
        entry[3] = description.symbolAddress;
    }

    if (!memory.Write(memory.context, (GuestState::GuestAddress)out, entry, sizeof(entry))) return AbiBridge::Result::MalformedArguments;

    registers.general[0] = 1;
    return AbiBridge::Result::Ok;
}

static void InvalidateGuestCode(void* context, uint64_t address, uint64_t length)
{
    GuestRuntime::InvalidateBlocks(*(GuestRuntime::Instance*)context, (GuestState::GuestAddress)address, length);
}

static constexpr uint8_t GUEST_CTYPE_UPPER = 0x01;
static constexpr uint8_t GUEST_CTYPE_LOWER = 0x02;
static constexpr uint8_t GUEST_CTYPE_DIGIT = 0x04;
static constexpr uint8_t GUEST_CTYPE_SPACE = 0x08;
static constexpr uint8_t GUEST_CTYPE_PUNCTUATION = 0x10;
static constexpr uint8_t GUEST_CTYPE_CONTROL = 0x20;
static constexpr uint8_t GUEST_CTYPE_HEX = 0x40;
static constexpr uint8_t GUEST_CTYPE_BLANK = 0x80;
static constexpr uint64_t GUEST_CTYPE_TABLE_BYTES = 257;
static constexpr uint64_t GUEST_TIME_ZONE_NAME_BYTES = 8;
static constexpr uint64_t GUEST_ENVIRONMENT_SLOTS = 256;

static uint8_t GuestCharacterClass(int character)
{
    uint8_t flags = 0;

    if (character >= 'A' && character <= 'Z') flags |= GUEST_CTYPE_UPPER;
    if (character >= 'a' && character <= 'z') flags |= GUEST_CTYPE_LOWER;
    if (character >= '0' && character <= '9') flags |= GUEST_CTYPE_DIGIT;
    if (character == ' ' || (character >= '\t' && character <= '\r')) flags |= GUEST_CTYPE_SPACE;
    if (character > ' ' && character < 0x7f && !isalnum(character)) flags |= GUEST_CTYPE_PUNCTUATION;
    if (character < ' ' || character == 0x7f) flags |= GUEST_CTYPE_CONTROL;
    if ((character >= 'A' && character <= 'F') || (character >= 'a' && character <= 'f')) flags |= GUEST_CTYPE_HEX;
    if (character == ' ') flags |= GUEST_CTYPE_BLANK;

    return flags;
}

static bool PublishDataObject(GuestRuntime::Instance& instance, const char* name, const void* content, uint64_t size,
                              GuestState::GuestAddress& address)
{
    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(instance);
    if (AbiBridge::LookupGuestData(registry, name, address) == AbiBridge::Result::Ok) return true;

    GuestRuntime::Result reserved = GuestRuntime::RegisterDataObject(instance, name, size, address);
    if (reserved != GuestRuntime::Result::Ok)
    {
        fprintf(stderr, "[guestlibc] cannot allocate %s: %s\n", name, GuestRuntime::ResultName(reserved));
        return false;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);
    if (content && !memory.Write(memory.context, address, content, size))
    {
        fprintf(stderr, "[guestlibc] cannot publish %s\n", name);
        return false;
    }

    return true;
}

static bool PublishPointerTo(GuestRuntime::Instance& instance, const char* name, GuestState::GuestAddress target)
{
    uint64_t value = (uint64_t)target;
    GuestState::GuestAddress address{};
    return PublishDataObject(instance, name, &value, sizeof(value), address);
}

static bool PublishCharacterTable(GuestRuntime::Instance& instance)
{
    uint8_t table[GUEST_CTYPE_TABLE_BYTES] = {};
    for (int character = 0; character < 128; ++character) table[character + 1] = GuestCharacterClass(character);

    GuestState::GuestAddress tableAddress{};
    if (!PublishDataObject(instance, "acl:ctype", table, sizeof(table), tableAddress)) return false;

    return PublishPointerTo(instance, "_ctype_", tableAddress);
}

static bool PublishTimeZone(GuestRuntime::Instance& instance)
{
    char names[2 * GUEST_TIME_ZONE_NAME_BYTES] = "UTC";
    memcpy(names + GUEST_TIME_ZONE_NAME_BYTES, "UTC", 4);

    GuestState::GuestAddress namesAddress{};
    if (!PublishDataObject(instance, "acl:tzname", names, sizeof(names), namesAddress)) return false;

    uint64_t pointers[2] = {(uint64_t)namesAddress, (uint64_t)namesAddress + GUEST_TIME_ZONE_NAME_BYTES};
    int32_t daylight = 0;
    int64_t offset = 0;

    GuestState::GuestAddress address{};
    return PublishDataObject(instance, "tzname", pointers, sizeof(pointers), address) &&
           PublishDataObject(instance, "daylight", &daylight, sizeof(daylight), address) &&
           PublishDataObject(instance, "timezone", &offset, sizeof(offset), address);
}

static bool PublishOptionParserState(GuestRuntime::Instance& instance)
{
    int32_t first = 1;
    int32_t zero = 0;
    uint64_t none = 0;

    GuestState::GuestAddress address{};
    return PublishDataObject(instance, "optind", &first, sizeof(first), address) &&
           PublishDataObject(instance, "opterr", &first, sizeof(first), address) &&
           PublishDataObject(instance, "optopt", &zero, sizeof(zero), address) &&
           PublishDataObject(instance, "optreset", &zero, sizeof(zero), address) &&
           PublishDataObject(instance, "optarg", &none, sizeof(none), address);
}

static bool PublishStackGuard(GuestRuntime::Instance& instance)
{
    uint64_t guard = 0;
    if (getrandom(&guard, sizeof(guard), 0) != (ssize_t)sizeof(guard)) guard = 0x5a17c0de9e3779b9ull;
    guard &= ~0xffull;

    GuestState::GuestAddress address{};
    return PublishDataObject(instance, "__stack_chk_guard", &guard, sizeof(guard), address);
}

static bool PublishEnvironment(GuestRuntime::Instance& instance)
{
    std::vector<std::string> entries;
    {
        std::lock_guard<std::mutex> guard(environmentMutex);
        for (const auto& variable : guestEnvironment)
        {
            if (entries.size() + 1 >= GUEST_ENVIRONMENT_SLOTS) break;
            entries.push_back(variable.first + "=" + variable.second);
        }
    }

    uint64_t textBytes = 0;
    for (const std::string& entry : entries) textBytes += entry.size() + 1;

    std::vector<uint8_t> text(textBytes == 0 ? 1 : textBytes, 0);
    GuestState::GuestAddress textAddress{};
    if (!PublishDataObject(instance, "acl:environment", nullptr, text.size(), textAddress)) return false;

    std::vector<uint64_t> pointers(GUEST_ENVIRONMENT_SLOTS, 0);
    uint64_t cursor = 0;

    for (size_t index = 0; index < entries.size(); ++index)
    {
        memcpy(text.data() + cursor, entries[index].c_str(), entries[index].size() + 1);
        pointers[index] = (uint64_t)textAddress + cursor;
        cursor += entries[index].size() + 1;
    }

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);
    if (!memory.Write(memory.context, textAddress, text.data(), text.size())) return false;

    GuestState::GuestAddress pointersAddress{};
    if (!PublishDataObject(instance, "acl:environ", pointers.data(), pointers.size() * sizeof(uint64_t), pointersAddress)) return false;

    return PublishPointerTo(instance, "environ", pointersAddress) && PublishPointerTo(instance, "__environ", pointersAddress);
}

static bool PublishLibraryData(GuestRuntime::Instance& instance)
{
    return PublishCharacterTable(instance) && PublishTimeZone(instance) && PublishOptionParserState(instance) &&
           PublishStackGuard(instance) && PublishEnvironment(instance);
}

GuestLibc::Result GuestLibc::RegisterAll(GuestRuntime::Instance& instance)
{
    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(instance);

    GuestState::GuestAddress guestMemoryBase{};
    uint64_t guestMemoryLength = 0;
    GuestRuntime::ArenaBounds(instance, guestMemoryBase, guestMemoryLength);
    HostBridge::SetGuestMemoryRange((uint64_t)guestMemoryBase, guestMemoryLength);
    HostBridge::SetCodeInvalidator(&InvalidateGuestCode, &instance);

    GuestState::GuestAddress streams{};
    if (AbiBridge::LookupGuestData(registry, "__sF", streams) != AbiBridge::Result::Ok)
    {
        GuestRuntime::Result reserved =
            GuestRuntime::RegisterDataObject(instance, "__sF", GUEST_STANDARD_STREAM_COUNT * GUEST_FILE_STRIDE_BYTES, streams);

        if (reserved != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[guestlibc] cannot allocate the __sF stream array: %s\n", GuestRuntime::ResultName(reserved));
            return Result::RegistrationFailed;
        }
    }

    GuestRuntime::SetFaultHandler(&DeliverGuestFault, nullptr);
    HostBridge::SetStandardStreams((uint64_t)streams, GUEST_FILE_STRIDE_BYTES);

    const char* streamNames[GUEST_STANDARD_STREAM_COUNT] = {"stdin", "stdout", "stderr"};
    for (uint64_t index = 0; index < GUEST_STANDARD_STREAM_COUNT; ++index)
    {
        GuestState::GuestAddress slot{};
        if (AbiBridge::LookupGuestData(registry, streamNames[index], slot) == AbiBridge::Result::Ok) continue;

        GuestRuntime::Result reserved = GuestRuntime::RegisterDataObject(instance, streamNames[index], sizeof(uint64_t), slot);
        if (reserved != GuestRuntime::Result::Ok)
        {
            fprintf(stderr, "[guestlibc] cannot allocate the %s pointer: %s\n", streamNames[index], GuestRuntime::ResultName(reserved));
            return Result::RegistrationFailed;
        }

        uint64_t value = (uint64_t)streams + index * GUEST_FILE_STRIDE_BYTES;
        const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(instance);
        if (!memory.Write(memory.context, slot, &value, sizeof(value)))
        {
            fprintf(stderr, "[guestlibc] cannot publish the %s pointer\n", streamNames[index]);
            return Result::RegistrationFailed;
        }
    }

    if (!PublishLibraryData(instance)) return Result::RegistrationFailed;

    struct Entry
    {
        const char* name;
        void* function;
        AbiBridge::Argument returnValue;
        AbiBridge::Argument arguments[6];
        int argumentCount;
    };

    AbiBridge::Argument pointer = PointerArgument();
    AbiBridge::Argument word = WordArgument();
    AbiBridge::Argument integer = IntArgument();

    const Entry entries[] = {
        {"malloc", (void*)&HostBridge::AllocateBytes, pointer, {word}, 1},
        {"calloc", (void*)&HostBridge::AllocateZeroed, pointer, {word, word}, 2},
        {"realloc", (void*)&HostBridge::ResizeAllocation, pointer, {pointer, word}, 2},
        {"free", (void*)&HostBridge::ReleaseAllocation, VoidArgument(), {pointer}, 1},
        {"memcpy", (void*)&HostBridge::CopyBytes, pointer, {pointer, pointer, word}, 3},
        {"memmove", (void*)&HostBridge::MoveBytes, pointer, {pointer, pointer, word}, 3},
        {"memset", (void*)&HostBridge::FillBytes, pointer, {pointer, integer, word}, 3},
        {"__memset_chk", (void*)&GuestMemsetChecked, pointer, {pointer, integer, word, word}, 4},
        {"__memcpy_chk", (void*)&GuestMemcpyChecked, pointer, {pointer, pointer, word, word}, 4},
        {"memcmp", (void*)&HostBridge::CompareBytes, integer, {pointer, pointer, word}, 3},
        {"strlen", (void*)&HostBridge::StringLength, word, {pointer}, 1},
        {"strcmp", (void*)&HostBridge::StringCompare, integer, {pointer, pointer}, 2},
        {"strncmp", (void*)&HostBridge::StringCompareBounded, integer, {pointer, pointer, word}, 3},
        {"strcpy", (void*)&HostBridge::StringCopy, pointer, {pointer, pointer}, 2},
        {"strncpy", (void*)&HostBridge::StringCopyBounded, pointer, {pointer, pointer, word}, 3},
        {"strchr", (void*)&HostBridge::FindCharacter, pointer, {pointer, integer}, 2},
        {"puts", (void*)&HostBridge::WriteLine, integer, {pointer}, 1},
        {"abort", (void*)&GuestAbort, VoidArgument(), {}, 0},
        {"__cxa_atexit", (void*)&IgnoredRegistration, integer, {pointer, pointer, pointer}, 3},
        {"__cxa_finalize", (void*)&IgnoredRegistration, VoidArgument(), {pointer}, 1},
        {"__register_atfork", (void*)&IgnoredRegistration, integer, {pointer, pointer, pointer}, 3},
        {"memchr", (void*)&HostBridge::FindByte, pointer, {pointer, integer, word}, 3},
        {"posix_memalign", (void*)&HostBridge::AllocatePosixAligned, integer, {pointer, word, word}, 3},
        {"strtol", (void*)&HostBridge::ParseSigned, word, {pointer, pointer, integer}, 3},
        {"strtoll", (void*)&HostBridge::ParseSigned, word, {pointer, pointer, integer}, 3},
        {"strtoul", (void*)&HostBridge::ParseUnsigned, word, {pointer, pointer, integer}, 3},
        {"strtoull", (void*)&HostBridge::ParseUnsigned, word, {pointer, pointer, integer}, 3},
        {"strtod", (void*)&HostBridge::ParseDouble, DoubleArgument(), {pointer, pointer}, 2},
        {"strtof", (void*)&HostBridge::ParseFloat, FloatArgument(), {pointer, pointer}, 2},
        {"wcslen", (void*)&HostBridge::WideLength, word, {pointer}, 1},
        {"wcstol", (void*)&HostBridge::ParseWideSigned, word, {pointer, pointer, integer}, 3},
        {"wcstoll", (void*)&HostBridge::ParseWideSigned, word, {pointer, pointer, integer}, 3},
        {"wcstoul", (void*)&HostBridge::ParseWideUnsigned, word, {pointer, pointer, integer}, 3},
        {"wcstoull", (void*)&HostBridge::ParseWideUnsigned, word, {pointer, pointer, integer}, 3},
        {"wcstod", (void*)&HostBridge::ParseWideDouble, DoubleArgument(), {pointer, pointer}, 2},
        {"wcstof", (void*)&HostBridge::ParseWideFloat, FloatArgument(), {pointer, pointer}, 2},
        {"wmemchr", (void*)&HostBridge::WideFindValue, pointer, {pointer, integer, word}, 3},
        {"wmemcmp", (void*)&HostBridge::WideCompareValues, integer, {pointer, pointer, word}, 3},
        {"__errno", (void*)&HostBridge::ErrorLocation, pointer, {}, 0},
        {"__stack_chk_fail", (void*)&GuestStackCheckFail, VoidArgument(), {}, 0},
        {"android_set_abort_message", (void*)&GuestSetAbortMessage, VoidArgument(), {pointer}, 1},

        {"newlocale", (void*)&HostBridge::CreateLocale, pointer, {integer, pointer, pointer}, 3},
        {"freelocale", (void*)&HostBridge::ReleaseLocale, VoidArgument(), {pointer}, 1},
        {"duplocale", (void*)&HostBridge::DuplicateLocale, pointer, {pointer}, 1},
        {"uselocale", (void*)&HostBridge::UseLocale, pointer, {pointer}, 1},
        {"localeconv", (void*)&HostBridge::LocaleConventions, pointer, {}, 0},
        {"setlocale", (void*)&HostBridge::SelectLocale, pointer, {integer, pointer}, 2},

        {"strcat", (void*)&HostBridge::StringAppend, pointer, {pointer, pointer}, 2},
        {"strncat", (void*)&HostBridge::StringAppendBounded, pointer, {pointer, pointer, word}, 3},
        {"strstr", (void*)&HostBridge::FindSubstring, pointer, {pointer, pointer}, 2},
        {"strrchr", (void*)&HostBridge::FindLastCharacter, pointer, {pointer, integer}, 2},
        {"strdup", (void*)&HostBridge::DuplicateString, pointer, {pointer}, 1},
        {"strndup", (void*)&HostBridge::DuplicateStringBounded, pointer, {pointer, word}, 2},
        {"strcasecmp", (void*)&HostBridge::StringCompareIgnoringCase, integer, {pointer, pointer}, 2},
        {"strncasecmp", (void*)&HostBridge::StringCompareIgnoringCaseBounded, integer, {pointer, pointer, word}, 3},
        {"strspn", (void*)&HostBridge::SpanOfCharacters, word, {pointer, pointer}, 2},
        {"strcspn", (void*)&HostBridge::SpanExcludingCharacters, word, {pointer, pointer}, 2},
        {"strpbrk", (void*)&HostBridge::FindAnyCharacter, pointer, {pointer, pointer}, 2},
        {"strerror", (void*)&HostBridge::DescribeErrorNumber, pointer, {integer}, 1},
        {"strtok_r", (void*)&HostBridge::SplitString, pointer, {pointer, pointer, pointer}, 3},
        {"stpcpy", (void*)&HostBridge::StringCopyReturningEnd, pointer, {pointer, pointer}, 2},
        {"strcoll", (void*)&HostBridge::StringCollate, integer, {pointer, pointer}, 2},
        {"strxfrm", (void*)&HostBridge::StringTransform, word, {pointer, pointer, word}, 3},

        {"wcscmp", (void*)&HostBridge::WideCompare, integer, {pointer, pointer}, 2},
        {"wcsncmp", (void*)&HostBridge::WideCompareBounded, integer, {pointer, pointer, word}, 3},
        {"wcscpy", (void*)&HostBridge::WideCopy, pointer, {pointer, pointer}, 2},
        {"wcsncpy", (void*)&HostBridge::WideCopyBounded, pointer, {pointer, pointer, word}, 3},
        {"wcschr", (void*)&HostBridge::WideFindCharacter, pointer, {pointer, integer}, 2},
        {"wcsrchr", (void*)&HostBridge::WideFindLastCharacter, pointer, {pointer, integer}, 2},
        {"wcsstr", (void*)&HostBridge::WideFindSubstring, pointer, {pointer, pointer}, 2},
        {"wmemcpy", (void*)&HostBridge::WideCopyBytes, pointer, {pointer, pointer, word}, 3},
        {"wmemmove", (void*)&HostBridge::WideMoveBytes, pointer, {pointer, pointer, word}, 3},
        {"wmemset", (void*)&HostBridge::WideFill, pointer, {pointer, integer, word}, 3},
        {"wcscoll", (void*)&HostBridge::WideCollate, integer, {pointer, pointer}, 2},
        {"wcsxfrm", (void*)&HostBridge::WideTransform, word, {pointer, pointer, word}, 3},
        {"mbrtowc", (void*)&HostBridge::DecodeCharacter, word, {pointer, pointer, word, pointer}, 4},
        {"wcrtomb", (void*)&HostBridge::EncodeCharacter, word, {pointer, integer, pointer}, 3},
        {"mbsrtowcs", (void*)&HostBridge::DecodeString, word, {pointer, pointer, word, pointer}, 4},
        {"wcsrtombs", (void*)&HostBridge::EncodeString, word, {pointer, pointer, word, pointer}, 4},
        {"mbrlen", (void*)&HostBridge::DecodedLength, word, {pointer, word, pointer}, 3},
        {"btowc", (void*)&HostBridge::ByteToWide, integer, {integer}, 1},
        {"wctob", (void*)&HostBridge::WideToByte, integer, {integer}, 1},
        {"mbsinit", (void*)&HostBridge::IsDecodingComplete, integer, {pointer}, 1},

        {"towupper", (void*)&HostBridge::WideToUpper, integer, {integer}, 1},
        {"towlower", (void*)&HostBridge::WideToLower, integer, {integer}, 1},
        {"iswctype", (void*)&HostBridge::ClassifyWide, integer, {integer, word}, 2},
        {"wctype", (void*)&HostBridge::LookupWideClass, word, {pointer}, 1},
        {"toupper", (void*)&HostBridge::ToUpper, integer, {integer}, 1},
        {"tolower", (void*)&HostBridge::ToLower, integer, {integer}, 1},
        {"isupper", (void*)&HostBridge::IsUpper, integer, {integer}, 1},
        {"islower", (void*)&HostBridge::IsLower, integer, {integer}, 1},
        {"isalpha", (void*)&HostBridge::IsAlpha, integer, {integer}, 1},
        {"isdigit", (void*)&HostBridge::IsDigit, integer, {integer}, 1},
        {"isalnum", (void*)&HostBridge::IsAlphaNumeric, integer, {integer}, 1},
        {"isspace", (void*)&HostBridge::IsSpace, integer, {integer}, 1},
        {"ispunct", (void*)&HostBridge::IsPunctuation, integer, {integer}, 1},
        {"isxdigit", (void*)&HostBridge::IsHexDigit, integer, {integer}, 1},
        {"iscntrl", (void*)&HostBridge::IsControl, integer, {integer}, 1},
        {"isprint", (void*)&HostBridge::IsPrintable, integer, {integer}, 1},
        {"isgraph", (void*)&HostBridge::IsGraphical, integer, {integer}, 1},
        {"isblank", (void*)&HostBridge::IsBlank, integer, {integer}, 1},

        {"sin", (void*)&HostBridge::SineOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"cos", (void*)&HostBridge::CosineOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"tan", (void*)&HostBridge::TangentOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"asin", (void*)&HostBridge::ArcSineOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"acos", (void*)&HostBridge::ArcCosineOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"atan", (void*)&HostBridge::ArcTangentOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"atan2", (void*)&HostBridge::ArcTangentOfRatio, DoubleArgument(), {DoubleArgument(), DoubleArgument()}, 2},
        {"exp", (void*)&HostBridge::ExponentialOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"log", (void*)&HostBridge::LogarithmOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"log2", (void*)&HostBridge::BinaryLogarithmOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"log10", (void*)&HostBridge::DecimalLogarithmOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"pow", (void*)&HostBridge::PowerOf, DoubleArgument(), {DoubleArgument(), DoubleArgument()}, 2},
        {"sqrt", (void*)&HostBridge::SquareRootOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"cbrt", (void*)&HostBridge::CubeRootOf, DoubleArgument(), {DoubleArgument()}, 1},
        {"ceil", (void*)&HostBridge::RoundUp, DoubleArgument(), {DoubleArgument()}, 1},
        {"floor", (void*)&HostBridge::RoundDown, DoubleArgument(), {DoubleArgument()}, 1},
        {"fmod", (void*)&HostBridge::Remainder, DoubleArgument(), {DoubleArgument(), DoubleArgument()}, 2},
        {"round", (void*)&HostBridge::RoundNearest, DoubleArgument(), {DoubleArgument()}, 1},
        {"trunc", (void*)&HostBridge::RoundTowardZero, DoubleArgument(), {DoubleArgument()}, 1},
        {"hypot", (void*)&HostBridge::Magnitude, DoubleArgument(), {DoubleArgument(), DoubleArgument()}, 2},
        {"sinh", (void*)&HostBridge::HyperbolicSine, DoubleArgument(), {DoubleArgument()}, 1},
        {"cosh", (void*)&HostBridge::HyperbolicCosine, DoubleArgument(), {DoubleArgument()}, 1},
        {"tanh", (void*)&HostBridge::HyperbolicTangent, DoubleArgument(), {DoubleArgument()}, 1},
        {"copysign", (void*)&HostBridge::WithSignOf, DoubleArgument(), {DoubleArgument(), DoubleArgument()}, 2},
        {"ldexp", (void*)&HostBridge::ScaleByPowerOfTwo, DoubleArgument(), {DoubleArgument(), integer}, 2},
        {"frexp", (void*)&HostBridge::SplitExponent, DoubleArgument(), {DoubleArgument(), pointer}, 2},
        {"modf", (void*)&HostBridge::SplitFraction, DoubleArgument(), {DoubleArgument(), pointer}, 2},

        {"sinf", (void*)&HostBridge::SineOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"cosf", (void*)&HostBridge::CosineOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"tanf", (void*)&HostBridge::TangentOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"atan2f", (void*)&HostBridge::ArcTangentOfRatioNarrow, FloatArgument(), {FloatArgument(), FloatArgument()}, 2},
        {"expf", (void*)&HostBridge::ExponentialOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"logf", (void*)&HostBridge::LogarithmOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"powf", (void*)&HostBridge::PowerOfNarrow, FloatArgument(), {FloatArgument(), FloatArgument()}, 2},
        {"sqrtf", (void*)&HostBridge::SquareRootOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"fmodf", (void*)&HostBridge::RemainderNarrow, FloatArgument(), {FloatArgument(), FloatArgument()}, 2},

        {"atoi", (void*)&HostBridge::ParseInteger, integer, {pointer}, 1},
        {"atol", (void*)&HostBridge::ParseLong, word, {pointer}, 1},
        {"atof", (void*)&HostBridge::ParseNumber, DoubleArgument(), {pointer}, 1},
        {"abs", (void*)&HostBridge::AbsoluteInteger, integer, {integer}, 1},
        {"labs", (void*)&HostBridge::AbsoluteLong, word, {word}, 1},
        {"llabs", (void*)&HostBridge::AbsoluteLong, word, {word}, 1},
        {"getpagesize", (void*)&HostBridge::PageSize, integer, {}, 0},
        {"sysconf", (void*)&HostBridge::SystemConfiguration, word, {integer}, 1},
        {"nanosleep", (void*)&HostBridge::SleepPrecisely, integer, {pointer, pointer}, 2},
        {"usleep", (void*)&HostBridge::SleepMicroseconds, integer, {word}, 1},
        {"sched_yield", (void*)&HostBridge::YieldProcessor, integer, {}, 0},
        {"gettid", (void*)&HostBridge::ThreadId, integer, {}, 0},
        {"time", (void*)&HostBridge::CurrentTime, word, {pointer}, 1},
        {"clock_gettime", (void*)&HostBridge::ReadClock, integer, {integer, pointer}, 2},
        {"gettimeofday", (void*)&HostBridge::ReadWallClock, integer, {pointer, pointer}, 2},

        {"__cxa_guard_acquire", (void*)&GuestGuardAcquire, integer, {pointer}, 1},
        {"__cxa_guard_release", (void*)&GuestGuardRelease, VoidArgument(), {pointer}, 1},
        {"__cxa_guard_abort", (void*)&GuestGuardRelease, VoidArgument(), {pointer}, 1},
        {"__cxa_pure_virtual", (void*)&GuestPureVirtual, VoidArgument(), {}, 0},

        {"mbtowc", (void*)&HostBridge::DecodeSingle, integer, {pointer, pointer, word}, 3},
        {"wctomb", (void*)&HostBridge::EncodeSingle, integer, {pointer, integer}, 2},
        {"mbstowcs", (void*)&HostBridge::DecodeWholeString, word, {pointer, pointer, word}, 3},
        {"wcstombs", (void*)&HostBridge::EncodeWholeString, word, {pointer, pointer, word}, 3},
        {"mbsnrtowcs", (void*)&HostBridge::DecodeStringBounded, word, {pointer, pointer, word, word, pointer}, 5},
        {"wcsnrtombs", (void*)&HostBridge::EncodeStringBounded, word, {pointer, pointer, word, word, pointer}, 5},
        {"__ctype_get_mb_cur_max", (void*)&HostBridge::MaximumCharacterBytes, word, {}, 0},
        {"strnlen", (void*)&HostBridge::StringLengthBounded, word, {pointer, word}, 2},
        {"memrchr", (void*)&HostBridge::FindLastByte, pointer, {pointer, integer, word}, 3},
        {"strlcpy", (void*)&HostBridge::StringCopyTerminated, word, {pointer, pointer, word}, 3},
        {"strerror_r", (void*)&HostBridge::DescribeErrorInto, integer, {integer, pointer, word}, 3},
        {"strtold", (void*)&HostBridge::ParseExtended, DoubleArgument(), {pointer, pointer}, 2},
        {"memalign", (void*)&HostBridge::AllocateAligned, pointer, {word, word}, 2},

        {"iswalpha_l", (void*)&HostBridge::IsWideAlphabetic, integer, {integer, pointer}, 2},
        {"iswblank_l", (void*)&HostBridge::IsWideBlank, integer, {integer, pointer}, 2},
        {"iswcntrl_l", (void*)&HostBridge::IsWideControl, integer, {integer, pointer}, 2},
        {"iswdigit_l", (void*)&HostBridge::IsWideDigit, integer, {integer, pointer}, 2},
        {"iswlower_l", (void*)&HostBridge::IsWideLower, integer, {integer, pointer}, 2},
        {"iswprint_l", (void*)&HostBridge::IsWidePrintable, integer, {integer, pointer}, 2},
        {"iswpunct_l", (void*)&HostBridge::IsWidePunctuation, integer, {integer, pointer}, 2},
        {"iswspace_l", (void*)&HostBridge::IsWideSpace, integer, {integer, pointer}, 2},
        {"iswupper_l", (void*)&HostBridge::IsWideUpper, integer, {integer, pointer}, 2},
        {"iswxdigit_l", (void*)&HostBridge::IsWideHexDigit, integer, {integer, pointer}, 2},
        {"towupper_l", (void*)&HostBridge::WideToUpperIn, integer, {integer, pointer}, 2},
        {"towlower_l", (void*)&HostBridge::WideToLowerIn, integer, {integer, pointer}, 2},
        {"strcoll_l", (void*)&HostBridge::StringCollateIn, integer, {pointer, pointer, pointer}, 3},
        {"strxfrm_l", (void*)&HostBridge::StringTransformIn, word, {pointer, pointer, word, pointer}, 4},
        {"wcscoll_l", (void*)&HostBridge::WideCollateIn, integer, {pointer, pointer, pointer}, 3},
        {"wcsxfrm_l", (void*)&HostBridge::WideTransformIn, word, {pointer, pointer, word, pointer}, 4},
        {"strtoll_l", (void*)&HostBridge::ParseSignedIn, word, {pointer, pointer, integer, pointer}, 4},
        {"strtoull_l", (void*)&HostBridge::ParseUnsignedIn, word, {pointer, pointer, integer, pointer}, 4},
        {"strtold_l", (void*)&HostBridge::ParseExtendedIn, DoubleArgument(), {pointer, pointer, pointer}, 3},
        {"isspace", (void*)&HostBridge::IsSpace, integer, {integer}, 1},

        {"asinf", (void*)&HostBridge::ArcSineOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"acosf", (void*)&HostBridge::ArcCosineOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"atanf", (void*)&HostBridge::ArcTangentOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"cbrtf", (void*)&HostBridge::CubeRootOfNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"exp2f", (void*)&HostBridge::BinaryExponentialNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"log2f", (void*)&HostBridge::BinaryLogarithmNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"log10f", (void*)&HostBridge::DecimalLogarithmNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"hypotf", (void*)&HostBridge::MagnitudeNarrow, FloatArgument(), {FloatArgument(), FloatArgument()}, 2},
        {"tanhf", (void*)&HostBridge::HyperbolicTangentNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"ldexpf", (void*)&HostBridge::ScaleByPowerOfTwoNarrow, FloatArgument(), {FloatArgument(), integer}, 2},
        {"modff", (void*)&HostBridge::SplitFractionNarrow, FloatArgument(), {FloatArgument(), pointer}, 2},
        {"nearbyintf", (void*)&HostBridge::RoundToEvenNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"scalbn", (void*)&HostBridge::ScaleByPowerOfTwo, DoubleArgument(), {DoubleArgument(), integer}, 2},
        {"logb", (void*)&HostBridge::UnbiasedExponent, DoubleArgument(), {DoubleArgument()}, 1},
        {"sincos", (void*)&HostBridge::SineAndCosine, VoidArgument(), {DoubleArgument(), pointer, pointer}, 3},
        {"sincosf", (void*)&HostBridge::SineAndCosineNarrow, VoidArgument(), {FloatArgument(), pointer, pointer}, 3},

        {"open", (void*)&HostBridge::OpenFile, integer, {pointer, integer, integer}, 3},
        {"fcntl", (void*)&HostBridge::ControlDescriptor, integer, {integer, integer, word}, 3},
        {"close", (void*)&HostBridge::CloseDescriptor, integer, {integer}, 1},
        {"read", (void*)&HostBridge::ReadDescriptor, word, {integer, pointer, word}, 3},
        {"write", (void*)&HostBridge::WriteDescriptor, word, {integer, pointer, word}, 3},
        {"lseek", (void*)&HostBridge::SeekDescriptor, word, {integer, word, integer}, 3},
        {"lseek64", (void*)&HostBridge::SeekDescriptor, word, {integer, word, integer}, 3},
        {"access", (void*)&HostBridge::CheckAccess, integer, {pointer, integer}, 2},
        {"dup", (void*)&HostBridge::DuplicateDescriptor, integer, {integer}, 1},
        {"pipe", (void*)&HostBridge::CreatePipe, integer, {pointer}, 1},
        {"stat", (void*)&HostBridge::DescribePath, integer, {pointer, pointer}, 2},
        {"lstat", (void*)&HostBridge::DescribeLink, integer, {pointer, pointer}, 2},
        {"fstat", (void*)&HostBridge::DescribeDescriptor, integer, {integer, pointer}, 2},
        {"readlink", (void*)&HostBridge::ReadLinkTarget, word, {pointer, pointer, word}, 3},
        {"realpath", (void*)&HostBridge::ResolvePath, pointer, {pointer, pointer}, 2},
        {"getcwd", (void*)&HostBridge::WorkingDirectory, pointer, {pointer, word}, 2},
        {"unlink", (void*)&HostBridge::RemovePath, integer, {pointer}, 1},
        {"remove", (void*)&HostBridge::RemoveEntry, integer, {pointer}, 1},
        {"rename", (void*)&HostBridge::RenamePath, integer, {pointer, pointer}, 2},
        {"mkdir", (void*)&HostBridge::CreateDirectory, integer, {pointer, integer}, 2},
        {"rmdir", (void*)&HostBridge::RemoveDirectory, integer, {pointer}, 1},
        {"opendir", (void*)&HostBridge::OpenDirectory, pointer, {pointer}, 1},
        {"readdir", (void*)&HostBridge::ReadDirectory, pointer, {pointer}, 1},
        {"closedir", (void*)&HostBridge::CloseDirectory, integer, {pointer}, 1},
        {"truncate", (void*)&HostBridge::TruncatePath, integer, {pointer, word}, 2},
        {"ftruncate", (void*)&HostBridge::TruncateDescriptor, integer, {integer, word}, 2},
        {"chmod", (void*)&HostBridge::ChangeMode, integer, {pointer, integer}, 2},
        {"fchmod", (void*)&HostBridge::ChangeDescriptorMode, integer, {integer, integer}, 2},
        {"symlink", (void*)&HostBridge::CreateSymbolicLink, integer, {pointer, pointer}, 2},
        {"link", (void*)&HostBridge::CreateHardLink, integer, {pointer, pointer}, 2},
        {"isatty", (void*)&HostBridge::IsTerminal, integer, {integer}, 1},
        {"getpid", (void*)&HostBridge::ProcessId, integer, {}, 0},
        {"getuid", (void*)&HostBridge::UserId, integer, {}, 0},
        {"geteuid", (void*)&HostBridge::EffectiveUserId, integer, {}, 0},
        {"getgid", (void*)&HostBridge::GroupId, integer, {}, 0},
        {"getegid", (void*)&HostBridge::EffectiveGroupId, integer, {}, 0},
        {"basename", (void*)&HostBridge::BaseName, pointer, {pointer}, 1},
        {"fnmatch", (void*)&HostBridge::MatchPattern, integer, {pointer, pointer, integer}, 3},
        {"statfs", (void*)&HostBridge::DescribeFileSystem, integer, {pointer, pointer}, 2},
        {"utimes", (void*)&HostBridge::SetTimestamps, integer, {pointer, pointer}, 2},
        {"flock", (void*)&HostBridge::LockDescriptor, integer, {integer, integer}, 2},
        {"poll", (void*)&HostBridge::PollDescriptors, integer, {pointer, word, integer}, 3},
        {"madvise", (void*)&HostBridge::AdviseMemory, integer, {pointer, word, integer}, 3},
        {"munmap", (void*)&HostBridge::UnmapMemory, integer, {pointer, word}, 2},
        {"mmap", (void*)&HostBridge::MapMemory, pointer, {pointer, word, integer, integer, integer, word}, 6},
        {"prctl", (void*)&HostBridge::ProcessControl, integer, {integer, word, word, word}, 4},

        {"fopen", (void*)&HostBridge::OpenStream, pointer, {pointer, pointer}, 2},
        {"fdopen", (void*)&HostBridge::AdoptDescriptor, pointer, {integer, pointer}, 2},
        {"fclose", (void*)&HostBridge::CloseStream, integer, {pointer}, 1},
        {"fread", (void*)&HostBridge::ReadStream, word, {pointer, word, word, pointer}, 4},
        {"fseek", (void*)&HostBridge::SeekStream, integer, {pointer, word, integer}, 3},
        {"fseeko", (void*)&HostBridge::SeekStream, integer, {pointer, word, integer}, 3},
        {"ftell", (void*)&HostBridge::StreamPosition, word, {pointer}, 1},
        {"ftello", (void*)&HostBridge::StreamPosition, word, {pointer}, 1},
        {"fgets", (void*)&HostBridge::ReadStreamLine, pointer, {pointer, integer, pointer}, 3},
        {"fgetc", (void*)&HostBridge::ReadStreamCharacter, integer, {pointer}, 1},
        {"fputs", (void*)&HostBridge::WriteStreamText, integer, {pointer, pointer}, 2},
        {"feof", (void*)&HostBridge::StreamAtEnd, integer, {pointer}, 1},
        {"ferror", (void*)&HostBridge::StreamHasError, integer, {pointer}, 1},
        {"clearerr", (void*)&HostBridge::ClearStreamError, VoidArgument(), {pointer}, 1},
        {"fileno", (void*)&HostBridge::StreamDescriptor, integer, {pointer}, 1},
        {"setbuf", (void*)&HostBridge::SetStreamBuffer, VoidArgument(), {pointer, pointer}, 2},

        {"clock", (void*)&HostBridge::ProcessorTime, word, {}, 0},
        {"difftime", (void*)&HostBridge::TimeDifference, DoubleArgument(), {word, word}, 2},
        {"mktime", (void*)&HostBridge::ComposeTime, word, {pointer}, 1},
        {"gmtime", (void*)&HostBridge::SplitUniversalTime, pointer, {pointer}, 1},
        {"gmtime_r", (void*)&HostBridge::SplitUniversalTimeInto, pointer, {pointer, pointer}, 2},
        {"localtime", (void*)&HostBridge::SplitLocalTime, pointer, {pointer}, 1},
        {"localtime_r", (void*)&HostBridge::SplitLocalTimeInto, pointer, {pointer, pointer}, 2},
        {"strftime", (void*)&HostBridge::FormatTime, word, {pointer, word, pointer, pointer}, 4},
        {"clock_getres", (void*)&HostBridge::ReadClockResolution, integer, {integer, pointer}, 2},
        {"utime", (void*)&HostBridge::SetPathTimes, integer, {pointer, pointer}, 2},

        {"setenv", (void*)&HostBridge::SetEnvironmentValue, integer, {pointer, pointer, integer}, 3},
        {"unsetenv", (void*)&HostBridge::ClearEnvironmentValue, integer, {pointer}, 1},
        {"srand48", (void*)&HostBridge::SeedRandom, VoidArgument(), {word}, 1},
        {"lrand48", (void*)&HostBridge::NextRandom, word, {}, 0},
        {"exit", (void*)&GuestExitProcess, VoidArgument(), {integer}, 1},
        {"_exit", (void*)&GuestExitProcess, VoidArgument(), {integer}, 1},
        {"__assert2", (void*)&GuestAssertionFailed, VoidArgument(), {pointer, integer, pointer, pointer}, 4},

        {"pthread_self", (void*)&HostBridge::CurrentThread, word, {}, 0},
        {"pthread_equal", (void*)&HostBridge::ThreadsEqual, integer, {word, word}, 2},

        {"getpriority", (void*)&HostBridge::ReadPriority, integer, {integer, integer}, 2},
        {"setpriority", (void*)&HostBridge::WritePriority, integer, {integer, integer, integer}, 3},
        {"openat", (void*)&HostBridge::OpenFileAt, integer, {integer, pointer, integer, integer}, 4},
        {"unlinkat", (void*)&HostBridge::RemovePathAt, integer, {integer, pointer, integer}, 3},
        {"fchmodat", (void*)&HostBridge::ChangeModeAt, integer, {integer, pointer, integer, integer}, 4},
        {"utimensat", (void*)&HostBridge::SetTimesAt, integer, {integer, pointer, pointer, integer}, 4},
        {"futimens", (void*)&HostBridge::SetDescriptorTimes, integer, {integer, pointer}, 2},
        {"fdopendir", (void*)&HostBridge::OpenDirectoryDescriptor, pointer, {integer}, 1},
        {"chdir", (void*)&HostBridge::ChangeWorkingDirectory, integer, {pointer}, 1},
        {"statvfs", (void*)&HostBridge::DescribeVolume, integer, {pointer, pointer}, 2},
        {"pathconf", (void*)&HostBridge::PathLimit, word, {pointer, integer}, 2},
        {"sendfile", (void*)&HostBridge::TransferFile, word, {integer, integer, pointer, word}, 4},
        {"select", (void*)&HostBridge::WaitForDescriptors, integer, {integer, pointer, pointer, pointer, pointer}, 5},
        {"raise", (void*)&HostBridge::RaiseSignal, integer, {integer}, 1},
        {"div", (void*)&HostBridge::DivideIntegers, word, {integer, integer}, 2},
        {"__gnu_strerror_r", (void*)&HostBridge::DescribeErrorReturning, pointer, {integer, pointer, word}, 3},
        {"strftime_l", (void*)&HostBridge::FormatTimeIn, word, {pointer, word, pointer, pointer, pointer}, 5},
        {"getpwuid", (void*)&HostBridge::PasswordEntry, pointer, {integer}, 1},
        {"getpwuid_r", (void*)&HostBridge::PasswordEntryInto, integer, {integer, pointer, pointer, word, pointer}, 5},
        {"getc", (void*)&HostBridge::ReadStreamCharacter, integer, {pointer}, 1},
        {"ungetc", (void*)&HostBridge::StreamPushBack, integer, {integer, pointer}, 2},
        {"fputwc", (void*)&HostBridge::WriteWideCharacter, integer, {integer, pointer}, 2},
        {"getwc", (void*)&HostBridge::ReadWideCharacter, integer, {pointer}, 1},
        {"ungetwc", (void*)&HostBridge::WidePushBack, integer, {integer, pointer}, 2},
        {"wcstold", (void*)&HostBridge::ParseWideExtended, DoubleArgument(), {pointer, pointer}, 2},
        {"__FD_SET_chk", (void*)&HostBridge::MarkDescriptorSet, VoidArgument(), {integer, pointer, word}, 3},
        {"__FD_ISSET_chk", (void*)&HostBridge::DescriptorIsSet, integer, {integer, pointer, word}, 3},
        {"inet_addr", (void*)&HostBridge::AddressFromText, integer, {pointer}, 1},
        {"inet_ntop", (void*)&HostBridge::AddressToText, pointer, {integer, pointer, pointer, integer}, 4},
        {"inet_pton", (void*)&HostBridge::AddressFromTextInto, integer, {integer, pointer, pointer}, 3},
        {"if_nametoindex", (void*)&HostBridge::InterfaceIndex, integer, {pointer}, 1},
        {"inflateInit2_", (void*)&HostBridge::DecompressInitialize, integer, {pointer, integer, pointer, integer}, 4},
        {"inflate", (void*)&HostBridge::Decompress, integer, {pointer, integer}, 2},
        {"inflateEnd", (void*)&HostBridge::DecompressEnd, integer, {pointer}, 1},
        {"__cxa_thread_atexit_impl", (void*)&IgnoredRegistration, integer, {pointer, pointer, pointer}, 3},

        {"__memmove_chk", (void*)&GuestMemmoveChecked, pointer, {pointer, pointer, word, word}, 4},
        {"__strlen_chk", (void*)&GuestStrlenChecked, word, {pointer, word}, 2},
        {"__strchr_chk", (void*)&GuestStrchrChecked, pointer, {pointer, integer, word}, 3},
        {"__strrchr_chk", (void*)&GuestStrrchrChecked, pointer, {pointer, integer, word}, 3},
        {"__strncpy_chk", (void*)&GuestStrncpyChecked, pointer, {pointer, pointer, word, word}, 4},
        {"__strncpy_chk2", (void*)&GuestStrncpyCheckedBoth, pointer, {pointer, pointer, word, word, word}, 5},
        {"__strcpy_chk", (void*)&GuestStrcpyChecked, pointer, {pointer, pointer, word}, 3},
        {"__stpcpy_chk", (void*)&GuestStpcpyChecked, pointer, {pointer, pointer, word}, 3},
        {"__strcat_chk", (void*)&GuestStrcatChecked, pointer, {pointer, pointer, word}, 3},
        {"__strncat_chk", (void*)&GuestStrncatChecked, pointer, {pointer, pointer, word, word}, 4},
        {"__strlcpy_chk", (void*)&GuestStrlcpyChecked, word, {pointer, pointer, word, word}, 4},
        {"__read_chk", (void*)&GuestReadChecked, word, {integer, pointer, word, word}, 4},
        {"__write_chk", (void*)&GuestWriteChecked, word, {integer, pointer, word, word}, 4},
        {"__pread_chk", (void*)&GuestPreadChecked, word, {integer, pointer, word, word, word}, 5},
        {"__pread64_chk", (void*)&GuestPreadChecked, word, {integer, pointer, word, word, word}, 5},
        {"__readlink_chk", (void*)&GuestReadlinkChecked, word, {pointer, pointer, word, word}, 4},
        {"__fgets_chk", (void*)&GuestFgetsChecked, pointer, {pointer, integer, pointer, word}, 4},
        {"__fread_chk", (void*)&GuestFreadChecked, word, {pointer, word, word, pointer, word}, 5},
        {"__open_2", (void*)&GuestOpenChecked, integer, {pointer, integer}, 2},
        {"__openat_2", (void*)&GuestOpenAtChecked, integer, {integer, pointer, integer}, 3},
        {"__umask_chk", (void*)&GuestUmaskChecked, integer, {integer}, 1},
        {"umask", (void*)&HostBridge::ChangeCreationMask, integer, {integer}, 1},

        {"isalpha_l", (void*)&HostBridge::IsAlpha, integer, {integer}, 1},
        {"isalnum_l", (void*)&HostBridge::IsAlphaNumeric, integer, {integer}, 1},
        {"isdigit_l", (void*)&HostBridge::IsDigit, integer, {integer}, 1},
        {"isxdigit_l", (void*)&HostBridge::IsHexDigit, integer, {integer}, 1},
        {"islower_l", (void*)&HostBridge::IsLower, integer, {integer}, 1},
        {"isupper_l", (void*)&HostBridge::IsUpper, integer, {integer}, 1},
        {"isspace_l", (void*)&HostBridge::IsSpace, integer, {integer}, 1},
        {"ispunct_l", (void*)&HostBridge::IsPunctuation, integer, {integer}, 1},
        {"iscntrl_l", (void*)&HostBridge::IsControl, integer, {integer}, 1},
        {"isprint_l", (void*)&HostBridge::IsPrintable, integer, {integer}, 1},
        {"isgraph_l", (void*)&HostBridge::IsGraphical, integer, {integer}, 1},
        {"isblank_l", (void*)&HostBridge::IsBlank, integer, {integer}, 1},
        {"tolower_l", (void*)&HostBridge::ToLower, integer, {integer}, 1},
        {"toupper_l", (void*)&HostBridge::ToUpper, integer, {integer}, 1},
        {"iswalpha", (void*)&HostBridge::IsWideAlphabeticAnywhere, integer, {integer}, 1},
        {"iswblank", (void*)&HostBridge::IsWideBlankAnywhere, integer, {integer}, 1},
        {"iswcntrl", (void*)&HostBridge::IsWideControlAnywhere, integer, {integer}, 1},
        {"iswdigit", (void*)&HostBridge::IsWideDigitAnywhere, integer, {integer}, 1},
        {"iswlower", (void*)&HostBridge::IsWideLowerAnywhere, integer, {integer}, 1},
        {"iswprint", (void*)&HostBridge::IsWidePrintableAnywhere, integer, {integer}, 1},
        {"iswpunct", (void*)&HostBridge::IsWidePunctuationAnywhere, integer, {integer}, 1},
        {"iswspace", (void*)&HostBridge::IsWideSpaceAnywhere, integer, {integer}, 1},
        {"iswupper", (void*)&HostBridge::IsWideUpperAnywhere, integer, {integer}, 1},
        {"iswxdigit", (void*)&HostBridge::IsWideHexDigitAnywhere, integer, {integer}, 1},
        {"iswalnum", (void*)&HostBridge::IsWideAlphaNumericAnywhere, integer, {integer}, 1},
        {"iswgraph", (void*)&HostBridge::IsWideGraphicalAnywhere, integer, {integer}, 1},
        {"strtof_l", (void*)&HostBridge::ParseFloat, FloatArgument(), {pointer, pointer}, 2},
        {"strtod_l", (void*)&HostBridge::ParseDouble, DoubleArgument(), {pointer, pointer}, 2},
        {"strtol_l", (void*)&HostBridge::ParseSigned, word, {pointer, pointer, integer}, 3},
        {"strtoul_l", (void*)&HostBridge::ParseUnsigned, word, {pointer, pointer, integer}, 3},

        {"rand", (void*)&HostBridge::RandomInteger, integer, {}, 0},
        {"srand", (void*)&HostBridge::SeedRandomInteger, VoidArgument(), {integer}, 1},
        {"random", (void*)&HostBridge::NextRandom, word, {}, 0},
        {"srandom", (void*)&HostBridge::SeedRandom, VoidArgument(), {word}, 1},
        {"atoll", (void*)&HostBridge::ParseLongLong, word, {pointer}, 1},
        {"strtok", (void*)&HostBridge::SplitStringShared, pointer, {pointer, pointer}, 2},
        {"strcasestr", (void*)&HostBridge::FindSubstringIgnoringCase, pointer, {pointer, pointer}, 2},
        {"strsignal", (void*)&HostBridge::DescribeSignal, pointer, {integer}, 1},
        {"perror", (void*)&HostBridge::PrintError, VoidArgument(), {pointer}, 1},
        {"gai_strerror", (void*)&HostBridge::DescribeAddressError, pointer, {integer}, 1},
        {"inet_aton", (void*)&HostBridge::AddressFromTextChecked, integer, {pointer, pointer}, 2},
        {"__cmsg_nxthdr", (void*)&HostBridge::NextControlMessage, pointer, {pointer, pointer}, 2},
        {"wcsftime", (void*)&HostBridge::FormatWideTime, word, {pointer, word, pointer, pointer}, 4},
        {"tzset", (void*)&HostBridge::UpdateTimeZone, VoidArgument(), {}, 0},
        {"aligned_alloc", (void*)&HostBridge::AllocateAligned, pointer, {word, word}, 2},
        {"_Znwm", (void*)&HostBridge::AllocateBytes, pointer, {word}, 1},
        {"_Znam", (void*)&HostBridge::AllocateBytes, pointer, {word}, 1},
        {"_ZdlPv", (void*)&HostBridge::ReleaseAllocation, VoidArgument(), {pointer}, 1},
        {"_ZdaPv", (void*)&HostBridge::ReleaseAllocation, VoidArgument(), {pointer}, 1},
        {"getprogname", (void*)&HostBridge::ProgramName, pointer, {}, 0},
        {"putwc", (void*)&HostBridge::WriteWideCharacter, integer, {integer, pointer}, 2},

        {"frexpf", (void*)&HostBridge::SplitExponentNarrow, FloatArgument(), {FloatArgument(), pointer}, 2},
        {"exp2", (void*)&HostBridge::BinaryExponential, DoubleArgument(), {DoubleArgument()}, 1},
        {"sinhf", (void*)&HostBridge::HyperbolicSineNarrow, FloatArgument(), {FloatArgument()}, 1},
        {"lrintf", (void*)&HostBridge::RoundToLongNarrow, word, {FloatArgument()}, 1},
        {"lrint", (void*)&HostBridge::RoundToLong, word, {DoubleArgument()}, 1},
        {"ilogbf", (void*)&HostBridge::BinaryExponentNarrow, integer, {FloatArgument()}, 1},
        {"__fpclassifyd", (void*)&HostBridge::ClassifyDouble, integer, {DoubleArgument()}, 1},
        {"__fpclassify", (void*)&HostBridge::ClassifyDouble, integer, {DoubleArgument()}, 1},
        {"__fpclassifyf", (void*)&HostBridge::ClassifyNarrow, integer, {FloatArgument()}, 1},
        {"__isfinitef", (void*)&HostBridge::IsFiniteNarrow, integer, {FloatArgument()}, 1},
        {"__isfinite", (void*)&HostBridge::IsFinite, integer, {DoubleArgument()}, 1},
        {"isnan", (void*)&HostBridge::IsNotANumber, integer, {DoubleArgument()}, 1},
        {"__isnan", (void*)&HostBridge::IsNotANumber, integer, {DoubleArgument()}, 1},
        {"isinf", (void*)&HostBridge::IsInfinite, integer, {DoubleArgument()}, 1},
        {"__isinf", (void*)&HostBridge::IsInfinite, integer, {DoubleArgument()}, 1},

        {"writev", (void*)&HostBridge::WriteGathered, word, {integer, pointer, integer}, 3},
        {"readv", (void*)&HostBridge::ReadScattered, word, {integer, pointer, integer}, 3},
        {"pread", (void*)&HostBridge::ReadDescriptorAt, word, {integer, pointer, word, word}, 4},
        {"pread64", (void*)&HostBridge::ReadDescriptorAt, word, {integer, pointer, word, word}, 4},
        {"pwrite", (void*)&HostBridge::WriteDescriptorAt, word, {integer, pointer, word, word}, 4},
        {"pwrite64", (void*)&HostBridge::WriteDescriptorAt, word, {integer, pointer, word, word}, 4},
        {"fsync", (void*)&HostBridge::SynchronizeDescriptor, integer, {integer}, 1},
        {"fdatasync", (void*)&HostBridge::SynchronizeDescriptorData, integer, {integer}, 1},
        {"dup2", (void*)&HostBridge::ReplaceDescriptor, integer, {integer, integer}, 2},
        {"dup3", (void*)&HostBridge::ReplaceDescriptorWithFlags, integer, {integer, integer, integer}, 3},
        {"pipe2", (void*)&HostBridge::CreatePipeWithFlags, integer, {pointer, integer}, 2},
        {"eventfd", (void*)&HostBridge::CreateEventDescriptor, integer, {integer, integer}, 2},
        {"mkstemp", (void*)&HostBridge::CreateTemporaryFile, integer, {pointer}, 1},
        {"posix_fadvise", (void*)&HostBridge::AdviseFileAccess, integer, {integer, word, word, integer}, 4},
        {"posix_fadvise64", (void*)&HostBridge::AdviseFileAccess, integer, {integer, word, word, integer}, 4},
        {"readahead", (void*)&HostBridge::ReadAhead, word, {integer, word, word}, 3},
        {"msync", (void*)&HostBridge::SynchronizeMemory, integer, {pointer, word, integer}, 3},
        {"mlock", (void*)&HostBridge::LockMemory, integer, {pointer, word}, 2},
        {"munlock", (void*)&HostBridge::UnlockMemory, integer, {pointer, word}, 2},
        {"epoll_create1", (void*)&HostBridge::CreateEventPoll, integer, {integer}, 1},
        {"epoll_create", (void*)&HostBridge::CreateEventPollSized, integer, {integer}, 1},
        {"epoll_ctl", (void*)&HostBridge::ControlEventPoll, integer, {integer, integer, integer, pointer}, 4},
        {"epoll_wait", (void*)&HostBridge::WaitEventPoll, integer, {integer, pointer, integer, integer}, 4},
        {"epoll_pwait", (void*)&HostBridge::WaitEventPollMasked, integer, {integer, pointer, integer, integer, pointer}, 5},
        {"inotify_init", (void*)&HostBridge::CreateFileWatch, integer, {}, 0},
        {"inotify_init1", (void*)&HostBridge::CreateFileWatchWithFlags, integer, {integer}, 1},
        {"inotify_add_watch", (void*)&HostBridge::AddFileWatch, integer, {integer, pointer, integer}, 3},
        {"inotify_rm_watch", (void*)&HostBridge::RemoveFileWatch, integer, {integer, integer}, 2},

        {"getppid", (void*)&HostBridge::ParentProcessId, integer, {}, 0},
        {"waitpid", (void*)&HostBridge::WaitForChild, integer, {integer, pointer, integer}, 3},
        {"wait", (void*)&HostBridge::WaitForAnyChild, integer, {pointer}, 1},
        {"uname", (void*)&HostBridge::DescribeSystem, integer, {pointer}, 1},
        {"sysinfo", (void*)&HostBridge::DescribeSystemMemory, integer, {pointer}, 1},
        {"gethostname", (void*)&HostBridge::HostName, integer, {pointer, word}, 2},
        {"getrlimit", (void*)&HostBridge::ReadResourceLimit, integer, {integer, pointer}, 2},
        {"getrlimit64", (void*)&HostBridge::ReadResourceLimit, integer, {integer, pointer}, 2},
        {"setrlimit", (void*)&HostBridge::WriteResourceLimit, integer, {integer, pointer}, 2},
        {"setrlimit64", (void*)&HostBridge::WriteResourceLimit, integer, {integer, pointer}, 2},
        {"getrusage", (void*)&HostBridge::ReadResourceUsage, integer, {integer, pointer}, 2},
        {"alarm", (void*)&HostBridge::ScheduleAlarm, integer, {integer}, 1},
        {"sleep", (void*)&HostBridge::SleepSeconds, integer, {integer}, 1},
        {"sched_getscheduler", (void*)&HostBridge::SchedulingPolicy, integer, {integer}, 1},
        {"sched_get_priority_min", (void*)&HostBridge::SchedulingPriorityMinimum, integer, {integer}, 1},
        {"sched_get_priority_max", (void*)&HostBridge::SchedulingPriorityMaximum, integer, {integer}, 1},
        {"sched_getparam", (void*)&HostBridge::ReadSchedulingParameters, integer, {integer, pointer}, 2},
        {"sched_getaffinity", (void*)&HostBridge::ReadProcessorAffinity, integer, {integer, word, pointer}, 3},
        {"sched_setaffinity", (void*)&HostBridge::WriteProcessorAffinity, integer, {integer, word, pointer}, 3},
        {"sched_getcpu", (void*)&HostBridge::CurrentProcessor, integer, {}, 0},

        {"inflateInit_", (void*)&HostBridge::DecompressStart, integer, {pointer, pointer, integer}, 3},
        {"inflateReset", (void*)&HostBridge::DecompressReset, integer, {pointer}, 1},
        {"deflateInit_", (void*)&HostBridge::CompressStart, integer, {pointer, integer, pointer, integer}, 4},
        {"deflate", (void*)&HostBridge::Compress, integer, {pointer, integer}, 2},
        {"deflateEnd", (void*)&HostBridge::CompressEnd, integer, {pointer}, 1},
        {"deflateReset", (void*)&HostBridge::CompressReset, integer, {pointer}, 1},
        {"deflateBound", (void*)&HostBridge::CompressBound, word, {pointer, word}, 2},
        {"zError", (void*)&HostBridge::CompressionErrorText, pointer, {integer}, 1},
        {"crc32", (void*)&HostBridge::ChecksumCrc32, word, {word, pointer, integer}, 3},
        {"adler32", (void*)&HostBridge::ChecksumAdler32, word, {word, pointer, integer}, 3},

    };

    for (const Entry& entry : entries)
    {
        AbiBridge::Signature signature = MakeSignature(entry.returnValue, entry.arguments, entry.argumentCount);
        AbiBridge::Result registered = AbiBridge::RegisterHostFunction(registry, entry.name, signature, entry.function);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[guestlibc] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::RegistrationFailed;
        }
    }

    struct HandlerEntry
    {
        const char* name;
        AbiBridge::GuestCallHandler handler;
        void* context;
    };

    const HandlerEntry handlers[] = {
#if defined(__x86_64__)
        {"printf", &PrintfHandler, nullptr},
        {"snprintf", &SnprintfHandler, nullptr},
        {"sprintf", &SprintfHandler, nullptr},
        {"vsnprintf", &VsnprintfHandler, nullptr},
        {"vsprintf", &VsprintfHandler, nullptr},
        {"vprintf", &VprintfHandler, nullptr},
        {"vfprintf", &VfprintfHandler, nullptr},
        {"__android_log_vprint", &AndroidLogVprintHandler, nullptr},
        {"__android_log_print", &AndroidLogPrintHandler, nullptr},
        {"__android_log_assert", &AndroidLogAssertHandler, nullptr},
        {"fprintf", &FprintfHandler, &instance},
        {"sscanf", &ScanStringHandler, nullptr},
        {"vsscanf", &ScanArgumentListHandler, nullptr},
        {"fscanf", &ScanStreamHandler, &instance},
        {"vasprintf", &AllocateFormattedHandler, nullptr},
        {"swprintf", &WideFormatHandler, nullptr},
        {"__vsnprintf_chk", &CheckedVsnprintfHandler, nullptr},
        {"__snprintf_chk", &CheckedSnprintfHandler, nullptr},
        {"__vsprintf_chk", &CheckedVsprintfHandler, nullptr},
        {"__sprintf_chk", &CheckedSprintfHandler, nullptr},
        {"__android_log_buf_print", &AndroidLogBufferPrintHandler, nullptr},
#endif
        {"__android_log_buf_write", &AndroidLogBufferWriteHandler, nullptr},
        {"putc", &FputcHandler, &instance},
        {"putchar", &PutcharHandler, nullptr},
        {"__fwrite_chk", &FwriteHandler, &instance},
        {"rewind", &RewindHandler, &instance},
        {"funopen", &GuestStreamOpenHandler, nullptr},
        {"funopen64", &GuestStreamOpenHandler, nullptr},
        {"fgetpos", &StreamGetPositionHandler, &instance},
        {"fsetpos", &StreamSetPositionHandler, &instance},
        {"setvbuf", &SucceedingStubHandler, nullptr},
        {"lldiv", &LongDivideHandler, nullptr},
        {"ldiv", &LongDivideHandler, nullptr},
        {"imaxdiv", &LongDivideHandler, nullptr},
        {"sigismember", &SignalSetMemberHandler, nullptr},
        {"deflateInit2_", &CompressStartDetailedHandler, nullptr},
        {"fegetround", &RoundingGetHandler, nullptr},
        {"fesetround", &RoundingSetHandler, nullptr},
        {"fegetenv", &EnvironmentGetHandler, nullptr},
        {"feholdexcept", &EnvironmentGetHandler, (void*)1},
        {"fesetenv", &EnvironmentSetHandler, nullptr},
        {"feupdateenv", &EnvironmentSetHandler, (void*)1},
        {"feclearexcept", &ExceptionClearHandler, nullptr},
        {"feraiseexcept", &ExceptionRaiseHandler, nullptr},
        {"fetestexcept", &ExceptionTestHandler, nullptr},
        {"sem_close", &SucceedingStubHandler, nullptr},
        {"sem_unlink", &SucceedingStubHandler, nullptr},
        {"pthread_atfork", &SucceedingStubHandler, nullptr},
        {"pthread_attr_setschedpolicy", &SucceedingStubHandler, nullptr},
        {"pthread_attr_setschedparam", &SucceedingStubHandler, nullptr},
        {"pthread_attr_getschedparam", &ZeroedIntegerOutHandler, nullptr},
        {"pthread_attr_getschedpolicy", &ZeroedIntegerOutHandler, nullptr},
        {"pthread_attr_getdetachstate", &ZeroedIntegerOutHandler, nullptr},
        {"pthread_attr_setguardsize", &SucceedingStubHandler, nullptr},
        {"pthread_attr_setscope", &SucceedingStubHandler, nullptr},
        {"pthread_setschedparam", &SucceedingStubHandler, nullptr},
        {"pthread_getschedparam", &ThreadSchedulingHandler, nullptr},
        {"arc4random", &RandomWordHandler, nullptr},
        {"arc4random_uniform", &RandomUniformHandler, nullptr},
        {"arc4random_buf", &RandomBufferHandler, nullptr},
        {"pthread_setschedprio", &SucceedingStubHandler, nullptr},
        {"sched_setscheduler", &SucceedingStubHandler, nullptr},
        {"sched_setparam", &SucceedingStubHandler, nullptr},
        {"fork", &DeniedCallHandler, nullptr},
        {"vfork", &DeniedCallHandler, nullptr},
        {"clone", &DeniedCallHandler, nullptr},
        {"execv", &DeniedCallHandler, nullptr},
        {"execve", &DeniedCallHandler, nullptr},
        {"execvp", &DeniedCallHandler, nullptr},
        {"execl", &DeniedCallHandler, nullptr},
        {"execlp", &DeniedCallHandler, nullptr},
        {"popen", &DeniedPointerHandler, nullptr},
        {"pclose", &DeniedCallHandler, nullptr},
        {"kill", &DeniedCallHandler, nullptr},
        {"tgkill", &DeniedCallHandler, nullptr},
        {"timer_create", &DeniedCallHandler, nullptr},
        {"timer_settime", &DeniedCallHandler, nullptr},
        {"timer_delete", &DeniedCallHandler, nullptr},
        {"mremap", &DeniedCallHandler, nullptr},
        {"sendmsg", &DeniedCallHandler, nullptr},
        {"recvmsg", &DeniedCallHandler, nullptr},
        {"sendto", &DeniedCallHandler, nullptr},
        {"recvfrom", &DeniedCallHandler, nullptr},
        {"sendmmsg", &DeniedCallHandler, nullptr},
        {"recvmmsg", &DeniedCallHandler, nullptr},
        {"getnameinfo", &DeniedCallHandler, nullptr},
        {"__android_log_write", &AndroidLogWriteHandler, nullptr},
        {"ioctl", &DescriptorControlHandler, nullptr},
        {"dl_iterate_phdr", &IteratePhdrHandler, &instance},
        {"qsort", &SortArrayHandler, &instance},
        {"bsearch", &SearchArrayHandler, &instance},
        {"dladdr", &DescribeAddressHandler, &instance},
        {"__loader_dladdr", &DescribeAddressHandler, &instance},
        {"__loader_dl_iterate_phdr", &IteratePhdrHandler, &instance},
        {"mprotect", &ProtectMemoryHandler, &instance},
        {"sem_init", &SemaphoreCreateHandler, nullptr},
        {"sem_destroy", &SemaphoreDestroyHandler, nullptr},
        {"sem_post", &SemaphorePostHandler, nullptr},
        {"sem_wait", &SemaphoreWaitHandler, nullptr},
        {"sem_trywait", &SemaphoreTryWaitHandler, nullptr},
        {"sem_timedwait", &SemaphoreWaitUntilHandler, nullptr},
        {"sem_getvalue", &SemaphoreReadHandler, nullptr},
        {"pthread_once", &ThreadOnceHandler, &instance},
        {"pthread_exit", &ThreadExitHandler, nullptr},
        {"pthread_kill", &ThreadSignalSendHandler, nullptr},
        {"pthread_getattr_np", &ThreadAttributesHandler, nullptr},
        {"pthread_attr_getstack", &ThreadStackBoundsHandler, nullptr},
        {"socket", &DeniedCallHandler, nullptr},
        {"socketpair", &DeniedCallHandler, nullptr},
        {"connect", &DeniedCallHandler, nullptr},
        {"bind", &DeniedCallHandler, nullptr},
        {"listen", &DeniedCallHandler, nullptr},
        {"accept", &DeniedCallHandler, nullptr},
        {"send", &DeniedCallHandler, nullptr},
        {"recv", &DeniedCallHandler, nullptr},
        {"shutdown", &DeniedCallHandler, nullptr},
        {"getsockopt", &DeniedCallHandler, nullptr},
        {"setsockopt", &DeniedCallHandler, nullptr},
        {"getsockname", &DeniedCallHandler, nullptr},
        {"getpeername", &DeniedCallHandler, nullptr},
        {"getaddrinfo", &DeniedCallHandler, nullptr},
        {"freeaddrinfo", &SucceedingStubHandler, nullptr},
        {"gethostbyname", &DeniedPointerHandler, nullptr},
        {"gethostbyaddr", &DeniedPointerHandler, nullptr},
        {"ptrace", &DeniedCallHandler, nullptr},
        {"process_vm_readv", &DeniedCallHandler, nullptr},
        {"openlog", &SucceedingStubHandler, nullptr},
        {"closelog", &SucceedingStubHandler, nullptr},
        {"syslog", &SucceedingStubHandler, nullptr},
        {"getenv", &GetenvHandler, &instance},
        {"fwrite", &FwriteHandler, &instance},
        {"fputc", &FputcHandler, &instance},
        {"fflush", &FflushHandler, &instance},
        {"pthread_create", &PthreadCreateHandler, &instance},
        {"pthread_join", &PthreadJoinHandler, &instance},
        {"pthread_mutex_init", &SucceedingStubHandler, nullptr},
        {"pthread_mutex_lock", &MutexLockHandler, nullptr},
        {"pthread_mutex_trylock", &MutexTryLockHandler, nullptr},
        {"pthread_mutex_unlock", &MutexUnlockHandler, nullptr},
        {"pthread_mutex_destroy", &MutexDestroyHandler, nullptr},
        {"pthread_mutexattr_init", &SucceedingStubHandler, nullptr},
        {"pthread_mutexattr_settype", &SucceedingStubHandler, nullptr},
        {"pthread_mutexattr_destroy", &SucceedingStubHandler, nullptr},
        {"pthread_cond_init", &SucceedingStubHandler, nullptr},
        {"pthread_cond_wait", &ConditionWaitHandler, nullptr},
        {"pthread_cond_timedwait", &ConditionTimedWaitHandler, nullptr},
        {"pthread_cond_signal", &ConditionSignalHandler, nullptr},
        {"pthread_cond_broadcast", &ConditionBroadcastHandler, nullptr},
        {"pthread_cond_destroy", &ConditionDestroyHandler, nullptr},
        {"pthread_condattr_init", &SucceedingStubHandler, nullptr},
        {"pthread_condattr_setclock", &SucceedingStubHandler, nullptr},
        {"pthread_condattr_destroy", &SucceedingStubHandler, nullptr},
        {"pthread_rwlock_init", &SucceedingStubHandler, nullptr},
        {"pthread_rwlock_rdlock", &MutexLockHandler, nullptr},
        {"pthread_rwlock_wrlock", &MutexLockHandler, nullptr},
        {"pthread_rwlock_unlock", &MutexUnlockHandler, nullptr},
        {"pthread_rwlock_destroy", &MutexDestroyHandler, nullptr},
        {"pthread_key_create", &ThreadLocalCreateHandler, nullptr},
        {"pthread_key_delete", &SucceedingStubHandler, nullptr},
        {"pthread_setspecific", &ThreadLocalSetHandler, nullptr},
        {"pthread_getspecific", &ThreadLocalGetHandler, nullptr},
        {"pthread_attr_init", &SucceedingStubHandler, nullptr},
        {"pthread_attr_setdetachstate", &SucceedingStubHandler, nullptr},
        {"pthread_attr_setstacksize", &SucceedingStubHandler, nullptr},
        {"pthread_attr_destroy", &SucceedingStubHandler, nullptr},
        {"pthread_detach", &SucceedingStubHandler, nullptr},
        {"pthread_setname_np", &SucceedingStubHandler, nullptr},
        {"syscall", &SyscallHandler, nullptr},
        {"setjmp", &SaveJumpContextHandler, nullptr},
        {"_setjmp", &SaveJumpContextHandler, nullptr},
        {"sigsetjmp", &SaveJumpContextHandler, nullptr},
        {"__sigsetjmp", &SaveJumpContextHandler, nullptr},
        {"longjmp", &RestoreJumpContextHandler, nullptr},
        {"_longjmp", &RestoreJumpContextHandler, nullptr},
        {"siglongjmp", &RestoreJumpContextHandler, nullptr},
        {"sigaction", &InstallSignalActionHandler, nullptr},
        {"signal", &InstallSignalHandlerHandler, nullptr},
        {"bsd_signal", &InstallSignalHandlerHandler, nullptr},
        {"sigaltstack", &SucceedingStubHandler, nullptr},
        {"pthread_sigmask", &SignalMaskHandler, nullptr},
        {"sigprocmask", &SignalMaskHandler, nullptr},
        {"sigsuspend", &SignalSuspendHandler, nullptr},
        {"sigemptyset", &SignalSetFillHandler, nullptr},
        {"sigfillset", &SignalSetFillHandler, (void*)1},
        {"sigaddset", &SignalSetChangeHandler, (void*)1},
        {"sigdelset", &SignalSetChangeHandler, nullptr},
        {"getauxval", &GetAuxiliaryValueHandler, nullptr},
        {"__system_property_get", &SystemPropertyGetHandler, nullptr},
        {"__system_property_find", &SystemPropertyFindHandler, nullptr},
        {"__system_property_read", &SystemPropertyReadHandler, nullptr},
        {ElfLoader::THREAD_LOCAL_RESOLVER_SYMBOL, &ThreadLocalResolveHandler, nullptr},
        {"__tls_get_addr", &TlsGetAddrHandler, &instance},
    };

    for (const HandlerEntry& entry : handlers)
    {
        AbiBridge::Result registered = AbiBridge::RegisterHostHandler(registry, entry.name, entry.handler, entry.context);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[guestlibc] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::RegistrationFailed;
        }
    }

    return Result::Ok;
}

const char* GuestLibc::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::RegistrationFailed: return "RegistrationFailed";
        case Result::BadFormat: return "BadFormat";
        case Result::TooManyArguments: return "TooManyArguments";
        case Result::GuestMemoryFault: return "GuestMemoryFault";
    }

    return "Unknown";
}

const char* GuestLibc::ConversionClassName(ConversionClass conversionClass)
{
    switch (conversionClass)
    {
        case ConversionClass::Invalid: return "Invalid";
        case ConversionClass::Integer: return "Integer";
        case ConversionClass::LongInteger: return "LongInteger";
        case ConversionClass::Pointer: return "Pointer";
        case ConversionClass::Double: return "Double";
        case ConversionClass::LongDouble: return "LongDouble";
        case ConversionClass::String: return "String";
        case ConversionClass::WideString: return "WideString";
        case ConversionClass::CountOut: return "CountOut";
    }

    return "Unknown";
}

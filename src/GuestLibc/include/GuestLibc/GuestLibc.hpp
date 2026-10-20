#pragma once

#include <cstdint>

#include "AbiBridge/AbiBridge.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

namespace GuestLibc
{
    inline constexpr int MAX_FORMAT_ARGUMENTS = 24;
    inline constexpr uint64_t MAX_FORMAT_LENGTH = 4096;
    inline constexpr const char* GUEST_ENVIRONMENT_SETTING = "guest-env";

    enum class Result : uint8_t
    {
        Ok,
        RegistrationFailed,
        BadFormat,
        TooManyArguments,
        GuestMemoryFault
    };

    enum class ConversionClass : uint8_t
    {
        Invalid,
        Integer,
        LongInteger,
        Pointer,
        Double,
        LongDouble,
        String,
        WideString,
        CountOut
    };

    struct FormatPlan
    {
        ConversionClass conversions[MAX_FORMAT_ARGUMENTS];
        int conversionCount = 0;
        int integerCount = 0;
        int floatCount = 0;
    };

    Result ParseFormat(const char* format, FormatPlan& plan);

    Result RegisterAll(GuestRuntime::Instance& instance);

    void SetEnvironmentValue(const std::string& name, const std::string& value);

    inline constexpr const char* ANDROID_SYSTEM_DIRECTORY = "/system";
    inline constexpr const char* ANDROID_EXTERNAL_DIRECTORY = "/storage/emulated/0";

    void DescribeAndroidEnvironment(const std::string& dataDirectory, const std::string& externalDirectory,
                                    const std::string& packageName);

    const char* ResultName(Result result);
    const char* ConversionClassName(ConversionClass conversionClass);
}

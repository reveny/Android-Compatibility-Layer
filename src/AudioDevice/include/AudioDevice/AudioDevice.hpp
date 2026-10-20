#pragma once

#include <cstdint>

namespace AudioDevice
{
    inline constexpr uint32_t DEFAULT_SAMPLE_RATE = 48000;
    inline constexpr uint32_t DEFAULT_CHANNELS = 2;
    inline constexpr uint32_t DEFAULT_FRAMES_PER_BURST = 256;
    inline constexpr uint32_t MAXIMUM_CHANNELS = 8;

    enum class Result : uint8_t
    {
        Ok,
        Unsupported,
        InvalidFormat,
        ConnectionFailed,
        StreamFailed,
        NotOpen
    };

    enum class SampleFormat : uint8_t
    {
        Signed16,
        Float32
    };

    struct Format
    {
        uint32_t sampleRate = DEFAULT_SAMPLE_RATE;
        uint32_t channels = DEFAULT_CHANNELS;
        uint32_t framesPerBurst = DEFAULT_FRAMES_PER_BURST;
        SampleFormat sampleFormat = SampleFormat::Float32;
    };

    typedef uint32_t (*FillFunction)(void* context, void* frames, uint32_t frameCount);

    struct Stream;

    bool IsSupported();

    Result Open(const Format& requested, FillFunction fill, void* context, Stream*& stream);
    Result Start(Stream& stream);
    Result Stop(Stream& stream);
    void Close(Stream*& stream);

    Format DescribeFormat(const Stream& stream);
    uint32_t BufferCapacityInFrames(const Stream& stream);
    uint64_t UnderrunCount(const Stream& stream);
    uint32_t SampleBytes(SampleFormat sampleFormat);

    const char* ResultName(Result result);
}

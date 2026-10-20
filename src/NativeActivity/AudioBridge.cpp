#include "NativeActivity/NativeActivity.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <mutex>
#include <set>

#include "AbiBridge/AbiBridge.hpp"
#include "AudioDevice/AudioDevice.hpp"
#include "GuestRuntime/GuestRuntime.hpp"
#include "Settings/Settings.hpp"

static constexpr int32_t AUDIO_OK = 0;
static constexpr int32_t AUDIO_ERROR_INVALID_STATE = -895;
static constexpr int32_t AUDIO_ERROR_INVALID_HANDLE = -892;
static constexpr int32_t AUDIO_ERROR_UNAVAILABLE = -889;
static constexpr int32_t AUDIO_ERROR_NULL = -886;

static constexpr int32_t AUDIO_FORMAT_UNSPECIFIED = 0;
static constexpr int32_t AUDIO_FORMAT_PCM_I16 = 1;
static constexpr int32_t AUDIO_FORMAT_PCM_FLOAT = 2;

static constexpr int32_t AUDIO_DIRECTION_OUTPUT = 0;

static constexpr int32_t AUDIO_CALLBACK_RESULT_STOP = 1;

static constexpr int32_t AUDIO_ERROR_TIMEOUT = -899;

static constexpr int32_t AUDIO_STREAM_STATE_OPEN = 1;
static constexpr int32_t AUDIO_STREAM_STATE_STARTED = 3;
static constexpr int32_t AUDIO_STREAM_STATE_PAUSED = 5;
static constexpr int32_t AUDIO_STREAM_STATE_FLUSHED = 7;
static constexpr int32_t AUDIO_STREAM_STATE_STOPPED = 9;
static constexpr int32_t AUDIO_STREAM_STATE_CLOSED = 11;

static constexpr uint64_t AUDIO_TRACE_INTERVAL = 256;

static constexpr int32_t UNSPECIFIED = 0;

struct AudioBuilder
{
    int32_t deviceId = UNSPECIFIED;
    int32_t sampleRate = UNSPECIFIED;
    int32_t channelCount = UNSPECIFIED;
    int32_t format = AUDIO_FORMAT_UNSPECIFIED;
    int32_t direction = AUDIO_DIRECTION_OUTPUT;
    int32_t sharingMode = UNSPECIFIED;
    int32_t performanceMode = UNSPECIFIED;
    int32_t bufferCapacityInFrames = UNSPECIFIED;
    int32_t framesPerDataCallback = UNSPECIFIED;
    int32_t usage = UNSPECIFIED;
    int32_t contentType = UNSPECIFIED;
    int32_t sessionId = UNSPECIFIED;
    uint64_t dataCallback = 0;
    uint64_t dataUserData = 0;
    uint64_t errorCallback = 0;
    uint64_t errorUserData = 0;
};

struct AudioStream
{
    AudioBuilder settings;
    AudioDevice::Stream* device = nullptr;
    AudioDevice::Format format;
    std::atomic<int32_t> state{AUDIO_STREAM_STATE_OPEN};
    int32_t bufferSizeInFrames = 0;
    std::atomic<uint64_t> framesWritten{0};
    std::atomic<uint64_t> presentedNanos{0};
    std::atomic<uint64_t> callbacks{0};
    std::atomic<uint64_t> soundingSamples{0};
    std::mutex stateLock;
    std::condition_variable stateChanged;
};

static bool AudioTracingRequested()
{
    static const bool requested = Settings::Tracing("audio");

    return requested;
}

static uint64_t AudioMonotonicNanos()
{
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static void EnterState(AudioStream* stream, int32_t state)
{
    {
        std::lock_guard<std::mutex> held(stream->stateLock);
        stream->state.store(state, std::memory_order_release);
    }

    stream->stateChanged.notify_all();
}

static std::mutex gAudioLock;
static std::set<AudioBuilder*> gBuilders;
static std::set<AudioStream*> gStreams;
static GuestRuntime::Instance* gAudioRuntime = nullptr;
static uint64_t gAudioThreadPointer = 0;

static AudioBuilder* LookupBuilder(uint64_t handle)
{
    std::lock_guard<std::mutex> guard(gAudioLock);
    AudioBuilder* builder = (AudioBuilder*)handle;
    return gBuilders.count(builder) != 0 ? builder : nullptr;
}

static AudioStream* LookupStream(uint64_t handle)
{
    std::lock_guard<std::mutex> guard(gAudioLock);
    AudioStream* stream = (AudioStream*)handle;
    return gStreams.count(stream) != 0 ? stream : nullptr;
}

static uint64_t AudioThreadPointer()
{
    if (gAudioThreadPointer != 0) return gAudioThreadPointer;
    if (!gAudioRuntime) return 0;

    GuestState::GuestAddress pointer{};
    if (GuestRuntime::AllocateThreadPointer(*gAudioRuntime, pointer) != GuestRuntime::Result::Ok) return 0;

    gAudioThreadPointer = (uint64_t)pointer;
    return gAudioThreadPointer;
}

static uint64_t PresentedFrames(const AudioStream& stream, uint64_t written)
{
    uint64_t buffered = (uint64_t)stream.format.framesPerBurst;
    return written > buffered ? written - buffered : 0;
}

static const char* DescribeAudioResult(int32_t result)
{
    switch (result)
    {
        case AUDIO_OK: return "AAUDIO_OK";
        case AUDIO_ERROR_INVALID_STATE: return "AAUDIO_ERROR_INVALID_STATE";
        case AUDIO_ERROR_INVALID_HANDLE: return "AAUDIO_ERROR_INVALID_HANDLE";
        case AUDIO_ERROR_UNAVAILABLE: return "AAUDIO_ERROR_UNAVAILABLE";
        case AUDIO_ERROR_NULL: return "AAUDIO_ERROR_NULL";
        case AUDIO_ERROR_TIMEOUT: return "AAUDIO_ERROR_TIMEOUT";
        default: return "AAUDIO_ERROR";
    }
}

static const char* DescribeStreamState(int32_t state)
{
    switch (state)
    {
        case AUDIO_STREAM_STATE_OPEN: return "OPEN";
        case AUDIO_STREAM_STATE_STARTED: return "STARTED";
        case AUDIO_STREAM_STATE_STOPPED: return "STOPPED";
        case AUDIO_STREAM_STATE_PAUSED: return "PAUSED";
        case AUDIO_STREAM_STATE_FLUSHED: return "FLUSHED";
        case AUDIO_STREAM_STATE_CLOSED: return "CLOSED";
        default: return "UNKNOWN";
    }
}

static uint64_t CountSoundingSamples(const AudioDevice::Format& format, const void* frames, uint32_t frameCount)
{
    if (!frames || frameCount == 0) return 0;

    uint64_t samples = (uint64_t)frameCount * format.channels;
    uint64_t sounding = 0;

    if (format.sampleFormat == AudioDevice::SampleFormat::Signed16)
    {
        const int16_t* narrow = (const int16_t*)frames;
        for (uint64_t index = 0; index < samples; ++index)
        {
            if (narrow[index] != 0) sounding += 1;
        }
    }
    else
    {
        const float* wide = (const float*)frames;
        for (uint64_t index = 0; index < samples; ++index)
        {
            if (wide[index] != 0.0f) sounding += 1;
        }
    }

    return sounding;
}

static uint32_t FillFromGuest(void* context, void* frames, uint32_t frameCount)
{
    AudioStream* stream = (AudioStream*)context;
    if (!stream || !gAudioRuntime || stream->settings.dataCallback == 0) return 0;

    GuestState::Registers registers;
    GuestState::Reset(registers);

    registers.general[0] = (uint64_t)stream;
    registers.general[1] = stream->settings.dataUserData;
    registers.general[2] = (uint64_t)frames;
    registers.general[3] = frameCount;
    registers.threadPointer = AudioThreadPointer();

    GuestRuntime::Result called =
        GuestRuntime::CallGuest(*gAudioRuntime, (GuestState::GuestAddress)stream->settings.dataCallback, registers);

    if (called != GuestRuntime::Result::Ok) return 0;
    if ((int32_t)(uint32_t)registers.general[0] == AUDIO_CALLBACK_RESULT_STOP) EnterState(stream, AUDIO_STREAM_STATE_STOPPED);

    stream->framesWritten.fetch_add(frameCount, std::memory_order_relaxed);
    stream->presentedNanos.store(AudioMonotonicNanos(), std::memory_order_relaxed);

    uint64_t sounding = CountSoundingSamples(stream->format, frames, frameCount);
    stream->soundingSamples.fetch_add(sounding, std::memory_order_relaxed);

    uint64_t seen = stream->callbacks.fetch_add(1, std::memory_order_relaxed) + 1;

    if (AudioTracingRequested() && (seen <= 4 || seen % AUDIO_TRACE_INTERVAL == 0))
    {
        fprintf(stderr, "[audiobridge] callback %llu filled %u frame(s) holding %llu sounding sample(s); %llu frames written in total\n",
                (unsigned long long)seen, frameCount, (unsigned long long)sounding,
                (unsigned long long)stream->framesWritten.load(std::memory_order_relaxed));
    }

    return frameCount;
}

static AbiBridge::Result CreateBuilderHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t output = registers.general[0];

    if (output == 0)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_NULL;
        return AbiBridge::Result::Ok;
    }

    AudioBuilder* builder = new AudioBuilder();
    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gBuilders.insert(builder);
    }

    uint64_t handle = (uint64_t)builder;
    if (!memory.Write(memory.context, (GuestState::GuestAddress)output, &handle, sizeof(handle)))
    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gBuilders.erase(builder);
        delete builder;
        return AbiBridge::Result::MalformedArguments;
    }

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result DeleteBuilderHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioBuilder* builder = LookupBuilder(registers.general[0]);

    if (builder)
    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gBuilders.erase(builder);
        delete builder;
    }

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result BuilderSetHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioBuilder* builder = LookupBuilder(registers.general[0]);
    if (!builder) return AbiBridge::Result::Ok;

    int32_t value = (int32_t)(uint32_t)registers.general[1];
    uint64_t field = (uint64_t)(uintptr_t)context;

    if (field == 0) builder->deviceId = value;
    else if (field == 1) builder->sampleRate = value;
    else if (field == 2) builder->channelCount = value;
    else if (field == 3) builder->format = value;
    else if (field == 4) builder->direction = value;
    else if (field == 5) builder->sharingMode = value;
    else if (field == 6) builder->performanceMode = value;
    else if (field == 7) builder->bufferCapacityInFrames = value;
    else if (field == 8) builder->framesPerDataCallback = value;
    else if (field == 9) builder->usage = value;
    else if (field == 10) builder->contentType = value;
    else if (field == 11) builder->sessionId = value;

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result BuilderSetDataCallbackHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioBuilder* builder = LookupBuilder(registers.general[0]);
    if (!builder) return AbiBridge::Result::Ok;

    builder->dataCallback = registers.general[1];
    builder->dataUserData = registers.general[2];
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result BuilderSetErrorCallbackHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioBuilder* builder = LookupBuilder(registers.general[0]);
    if (!builder) return AbiBridge::Result::Ok;

    builder->errorCallback = registers.general[1];
    builder->errorUserData = registers.general[2];
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result OpenStreamHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    AudioBuilder* builder = LookupBuilder(registers.general[0]);
    uint64_t output = registers.general[1];

    if (!builder || output == 0)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_NULL;
        return AbiBridge::Result::Ok;
    }

    if (builder->direction != AUDIO_DIRECTION_OUTPUT || !AudioDevice::IsSupported())
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_UNAVAILABLE;
        return AbiBridge::Result::Ok;
    }

    AudioStream* stream = new AudioStream();
    stream->settings = *builder;

    AudioDevice::Format wanted;
    wanted.sampleRate = builder->sampleRate > 0 ? (uint32_t)builder->sampleRate : AudioDevice::DEFAULT_SAMPLE_RATE;
    wanted.channels = builder->channelCount > 0 ? (uint32_t)builder->channelCount : AudioDevice::DEFAULT_CHANNELS;
    wanted.framesPerBurst =
        builder->framesPerDataCallback > 0 ? (uint32_t)builder->framesPerDataCallback : AudioDevice::DEFAULT_FRAMES_PER_BURST;
    wanted.sampleFormat =
        builder->format == AUDIO_FORMAT_PCM_I16 ? AudioDevice::SampleFormat::Signed16 : AudioDevice::SampleFormat::Float32;

    AudioDevice::Result opened = AudioDevice::Open(wanted, &FillFromGuest, stream, stream->device);

    if (opened != AudioDevice::Result::Ok)
    {
        fprintf(stderr, "[audiobridge] the host device did not open: %s\n", AudioDevice::ResultName(opened));
        delete stream;
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_UNAVAILABLE;
        return AbiBridge::Result::Ok;
    }

    stream->format = AudioDevice::DescribeFormat(*stream->device);
    stream->bufferSizeInFrames = (int32_t)AudioDevice::BufferCapacityInFrames(*stream->device);
    stream->state = AUDIO_STREAM_STATE_OPEN;

    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gStreams.insert(stream);
    }

    uint64_t handle = (uint64_t)stream;
    if (!memory.Write(memory.context, (GuestState::GuestAddress)output, &handle, sizeof(handle)))
    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gStreams.erase(stream);
        AudioDevice::Close(stream->device);
        delete stream;
        return AbiBridge::Result::MalformedArguments;
    }

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamStartHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    AudioDevice::Result started = AudioDevice::Start(*stream->device);
    if (started != AudioDevice::Result::Ok)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_STATE;
        return AbiBridge::Result::Ok;
    }

    EnterState(stream, AUDIO_STREAM_STATE_STARTED);
    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamStopHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    AudioDevice::Stop(*stream->device);
    EnterState(stream, AUDIO_STREAM_STATE_STOPPED);
    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamPauseHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    bool flushing = context != nullptr;

    if (flushing)
    {
        if (stream->state.load(std::memory_order_acquire) == AUDIO_STREAM_STATE_STARTED)
        {
            registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_STATE;
            return AbiBridge::Result::Ok;
        }

        EnterState(stream, AUDIO_STREAM_STATE_FLUSHED);
    }
    else
    {
        AudioDevice::Stop(*stream->device);
        EnterState(stream, AUDIO_STREAM_STATE_PAUSED);
    }

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamCloseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    {
        std::lock_guard<std::mutex> guard(gAudioLock);
        gStreams.erase(stream);
    }

    AudioDevice::Close(stream->device);
    EnterState(stream, AUDIO_STREAM_STATE_CLOSED);
    delete stream;

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamQueryHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);
    uint64_t field = (uint64_t)(uintptr_t)context;

    if (!stream)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    int32_t answer = 0;

    if (field == 0) answer = (int32_t)stream->format.sampleRate;
    else if (field == 1) answer = (int32_t)stream->format.channels;
    else if (field == 2)
        answer = stream->format.sampleFormat == AudioDevice::SampleFormat::Signed16 ? AUDIO_FORMAT_PCM_I16 : AUDIO_FORMAT_PCM_FLOAT;
    else if (field == 3) answer = stream->bufferSizeInFrames;
    else if (field == 4) answer = (int32_t)AudioDevice::BufferCapacityInFrames(*stream->device);
    else if (field == 5) answer = (int32_t)stream->format.framesPerBurst;
    else if (field == 6) answer = (int32_t)AudioDevice::UnderrunCount(*stream->device);
    else if (field == 7) answer = stream->settings.deviceId;
    else if (field == 8) answer = 0;
    else if (field == 9) answer = stream->settings.direction;
    else if (field == 10) answer = stream->settings.sharingMode;
    else if (field == 11) answer = stream->settings.performanceMode;
    else if (field == 12) answer = stream->settings.usage;
    else if (field == 13) answer = stream->settings.contentType;
    else if (field == 14) answer = stream->settings.sessionId;

    registers.general[0] = (uint64_t)(int64_t)answer;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamSetBufferSizeHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    int32_t wanted = (int32_t)(uint32_t)registers.general[1];
    int32_t capacity = (int32_t)AudioDevice::BufferCapacityInFrames(*stream->device);

    if (wanted < 1) wanted = 1;
    if (capacity > 0 && wanted > capacity) wanted = capacity;

    stream->bufferSizeInFrames = wanted;
    registers.general[0] = (uint64_t)(int64_t)wanted;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamWaitForStateHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    AudioStream* stream = LookupStream(registers.general[0]);
    int32_t wanted = (int32_t)(uint32_t)registers.general[1];
    uint64_t output = registers.general[2];
    int64_t timeoutNanos = (int64_t)registers.general[3];

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    int32_t reached = stream->state.load(std::memory_order_acquire);

    if (reached == wanted && timeoutNanos > 0)
    {
        std::unique_lock<std::mutex> held(stream->stateLock);
        stream->stateChanged.wait_for(held, std::chrono::nanoseconds(timeoutNanos),
                                      [stream, wanted]() { return stream->state.load(std::memory_order_acquire) != wanted; });

        reached = stream->state.load(std::memory_order_acquire);
    }

    if (output != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)output, &reached, sizeof(reached)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(int64_t)(reached == wanted ? AUDIO_ERROR_TIMEOUT : AUDIO_OK);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamStateHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);
    registers.general[0] = (uint64_t)(int64_t)(stream ? stream->state.load(std::memory_order_acquire) : AUDIO_STREAM_STATE_CLOSED);

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamFramePositionHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    uint64_t written = stream->framesWritten.load(std::memory_order_relaxed);
    bool wantRead = context != nullptr;

    registers.general[0] = wantRead ? PresentedFrames(*stream, written) : written;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamTimestampHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    AudioStream* stream = LookupStream(registers.general[0]);
    uint64_t framesOut = registers.general[2];
    uint64_t nanosOut = registers.general[3];

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    if (stream->state.load(std::memory_order_acquire) != AUDIO_STREAM_STATE_STARTED)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_STATE;
        return AbiBridge::Result::Ok;
    }

    int64_t written = (int64_t)stream->framesWritten.load(std::memory_order_relaxed);
    int64_t position = (int64_t)PresentedFrames(*stream, (uint64_t)written);
    int64_t when = (int64_t)stream->presentedNanos.load(std::memory_order_relaxed);

    if (when == 0) when = (int64_t)AudioMonotonicNanos();

    if (framesOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)framesOut, &position, sizeof(position)))
        return AbiBridge::Result::MalformedArguments;

    if (nanosOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)nanosOut, &when, sizeof(when)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(int64_t)AUDIO_OK;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamWriteHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    AudioStream* stream = LookupStream(registers.general[0]);

    if (!stream)
    {
        registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_HANDLE;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)(int64_t)AUDIO_ERROR_INVALID_STATE;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result StreamTextHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = (uint64_t)(uintptr_t)(context != nullptr ? DescribeStreamState((int32_t)(uint32_t)registers.general[0])
                                                                   : DescribeAudioResult((int32_t)(uint32_t)registers.general[0]));

    return AbiBridge::Result::Ok;
}

NativeActivity::Result NativeActivity::RegisterAudioFunctions(GuestRuntime::Instance& runtime)
{
    struct HandlerEntry
    {
        const char* name;
        AbiBridge::GuestCallHandler handler;
        void* context;
    };

    const HandlerEntry handlers[] = {
        {"AAudio_createStreamBuilder", &CreateBuilderHandler, nullptr},
        {"AAudioStreamBuilder_delete", &DeleteBuilderHandler, nullptr},
        {"AAudioStreamBuilder_setDeviceId", &BuilderSetHandler, (void*)0},
        {"AAudioStreamBuilder_setSampleRate", &BuilderSetHandler, (void*)1},
        {"AAudioStreamBuilder_setChannelCount", &BuilderSetHandler, (void*)2},
        {"AAudioStreamBuilder_setFormat", &BuilderSetHandler, (void*)3},
        {"AAudioStreamBuilder_setDirection", &BuilderSetHandler, (void*)4},
        {"AAudioStreamBuilder_setSharingMode", &BuilderSetHandler, (void*)5},
        {"AAudioStreamBuilder_setPerformanceMode", &BuilderSetHandler, (void*)6},
        {"AAudioStreamBuilder_setBufferCapacityInFrames", &BuilderSetHandler, (void*)7},
        {"AAudioStreamBuilder_setFramesPerDataCallback", &BuilderSetHandler, (void*)8},
        {"AAudioStreamBuilder_setDataCallback", &BuilderSetDataCallbackHandler, nullptr},
        {"AAudioStreamBuilder_setErrorCallback", &BuilderSetErrorCallbackHandler, nullptr},
        {"AAudioStreamBuilder_openStream", &OpenStreamHandler, nullptr},
        {"AAudioStream_requestStart", &StreamStartHandler, nullptr},
        {"AAudioStream_requestStop", &StreamStopHandler, nullptr},
        {"AAudioStream_close", &StreamCloseHandler, nullptr},
        {"AAudioStream_getSampleRate", &StreamQueryHandler, (void*)0},
        {"AAudioStream_getChannelCount", &StreamQueryHandler, (void*)1},
        {"AAudioStream_getFormat", &StreamQueryHandler, (void*)2},
        {"AAudioStream_getBufferSizeInFrames", &StreamQueryHandler, (void*)3},
        {"AAudioStream_getBufferCapacityInFrames", &StreamQueryHandler, (void*)4},
        {"AAudioStream_getFramesPerBurst", &StreamQueryHandler, (void*)5},
        {"AAudioStream_getXRunCount", &StreamQueryHandler, (void*)6},
        {"AAudioStream_getDeviceId", &StreamQueryHandler, (void*)7},
        {"AAudioStream_isMMapUsed", &StreamQueryHandler, (void*)8},
        {"AAudioStream_setBufferSizeInFrames", &StreamSetBufferSizeHandler, nullptr},
        {"AAudioStream_waitForStateChange", &StreamWaitForStateHandler, nullptr},
        {"AAudioStream_getState", &StreamStateHandler, nullptr},
        {"AAudioStream_getFramesWritten", &StreamFramePositionHandler, nullptr},
        {"AAudioStream_getFramesRead", &StreamFramePositionHandler, (void*)1},
        {"AAudioStream_getTimestamp", &StreamTimestampHandler, nullptr},
        {"AAudioStream_requestPause", &StreamPauseHandler, nullptr},
        {"AAudioStream_requestFlush", &StreamPauseHandler, (void*)1},
        {"AAudioStream_release", &StreamStopHandler, nullptr},
        {"AAudioStream_write", &StreamWriteHandler, nullptr},
        {"AAudioStream_getDirection", &StreamQueryHandler, (void*)9},
        {"AAudioStream_getSharingMode", &StreamQueryHandler, (void*)10},
        {"AAudioStream_getPerformanceMode", &StreamQueryHandler, (void*)11},
        {"AAudioStream_getSamplesPerFrame", &StreamQueryHandler, (void*)1},
        {"AAudioStream_getUsage", &StreamQueryHandler, (void*)12},
        {"AAudioStream_getContentType", &StreamQueryHandler, (void*)13},
        {"AAudioStream_getSessionId", &StreamQueryHandler, (void*)14},
        {"AAudioStreamBuilder_setUsage", &BuilderSetHandler, (void*)9},
        {"AAudioStreamBuilder_setContentType", &BuilderSetHandler, (void*)10},
        {"AAudioStreamBuilder_setSessionId", &BuilderSetHandler, (void*)11},
        {"AAudio_convertResultToText", &StreamTextHandler, nullptr},
        {"AAudio_convertStreamStateToText", &StreamTextHandler, (void*)1},
    };

    gAudioRuntime = &runtime;
    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(runtime);

    for (const HandlerEntry& entry : handlers)
    {
        AbiBridge::Result registered = AbiBridge::RegisterHostHandler(registry, entry.name, entry.handler, entry.context);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[audiobridge] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::JniRegistrationFailed;
        }
    }

    return Result::Ok;
}

#include "AudioDevice/AudioDevice.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#if RUNTIME_HAVE_AUDIO
#include <pipewire/pipewire.h>
#include <spa/param/audio/format-utils.h>

#include "Settings/Settings.hpp"
#endif

uint32_t AudioDevice::SampleBytes(SampleFormat sampleFormat)
{
    return sampleFormat == SampleFormat::Signed16 ? 2u : 4u;
}

const char* AudioDevice::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::Unsupported: return "Unsupported";
        case Result::InvalidFormat: return "InvalidFormat";
        case Result::ConnectionFailed: return "ConnectionFailed";
        case Result::StreamFailed: return "StreamFailed";
        case Result::NotOpen: return "NotOpen";
    }
    return "Unknown";
}

#if !RUNTIME_HAVE_AUDIO

struct AudioDevice::Stream
{
    Format format;
};

bool AudioDevice::IsSupported()
{
    return false;
}

AudioDevice::Result AudioDevice::Open(const Format&, FillFunction, void*, Stream*& stream)
{
    stream = nullptr;
    return Result::Unsupported;
}

AudioDevice::Result AudioDevice::Start(Stream&)
{
    return Result::Unsupported;
}

AudioDevice::Result AudioDevice::Stop(Stream&)
{
    return Result::Unsupported;
}

void AudioDevice::Close(Stream*& stream)
{
    stream = nullptr;
}

AudioDevice::Format AudioDevice::DescribeFormat(const Stream& stream)
{
    return stream.format;
}

uint32_t AudioDevice::BufferCapacityInFrames(const Stream&)
{
    return 0;
}

uint64_t AudioDevice::UnderrunCount(const Stream&)
{
    return 0;
}

#else

struct AudioDevice::Stream
{
    Format format;
    FillFunction fill = nullptr;
    void* context = nullptr;
    pw_thread_loop* loop = nullptr;
    pw_stream* stream = nullptr;
    spa_hook listener{};
    uint32_t sampleBytes = 4;
    uint32_t capacityInFrames = 0;
    uint64_t underruns = 0;
    bool running = false;
    bool streaming = false;
};

static bool gLibraryReady = false;

static void EnsureLibrary()
{
    if (gLibraryReady) return;

    pw_init(nullptr, nullptr);
    gLibraryReady = true;
}

static void OnStreamStateChanged(void* userData, enum pw_stream_state previous, enum pw_stream_state current, const char* error)
{
    AudioDevice::Stream* stream = (AudioDevice::Stream*)userData;

    if (current == PW_STREAM_STATE_ERROR)
    {
        fprintf(stderr, "[audiodevice] the stream went from %s to %s%s%s\n", pw_stream_state_as_string(previous),
                pw_stream_state_as_string(current), error ? ": " : "", error ? error : "");
    }

    if (stream) stream->streaming = current == PW_STREAM_STATE_STREAMING;
}

static void OnStreamProcess(void* userData)
{
    AudioDevice::Stream* stream = (AudioDevice::Stream*)userData;
    if (!stream || !stream->stream) return;

    pw_buffer* held = pw_stream_dequeue_buffer(stream->stream);
    if (!held)
    {
        stream->underruns += 1;
        return;
    }

    spa_buffer* buffer = held->buffer;
    uint32_t stride = stream->format.channels * stream->sampleBytes;

    if (!buffer->datas[0].data || stride == 0)
    {
        pw_stream_queue_buffer(stream->stream, held);
        return;
    }

    uint32_t capacity = buffer->datas[0].maxsize / stride;
    uint32_t wanted = held->requested != 0 ? (uint32_t)held->requested : capacity;
    if (wanted > capacity) wanted = capacity;

    uint32_t produced = 0;
    if (wanted != 0 && stream->fill) produced = stream->fill(stream->context, buffer->datas[0].data, wanted);
    if (produced > wanted) produced = wanted;

    if (produced < wanted)
    {
        uint8_t* silence = (uint8_t*)buffer->datas[0].data + (size_t)produced * stride;
        memset(silence, 0, (size_t)(wanted - produced) * stride);
        stream->underruns += 1;
    }

    buffer->datas[0].chunk->offset = 0;
    buffer->datas[0].chunk->stride = (int32_t)stride;
    buffer->datas[0].chunk->size = wanted * stride;

    pw_stream_queue_buffer(stream->stream, held);
}

static const pw_stream_events STREAM_EVENTS = {
    PW_VERSION_STREAM_EVENTS, nullptr, &OnStreamStateChanged, nullptr, nullptr, nullptr,     nullptr,
    nullptr,                  &OnStreamProcess, nullptr,      nullptr, nullptr,
};

bool AudioDevice::IsSupported()
{
    return true;
}

AudioDevice::Result AudioDevice::Open(const Format& requested, FillFunction fill, void* context, Stream*& stream)
{
    stream = nullptr;

    if (requested.channels == 0 || requested.channels > MAXIMUM_CHANNELS) return Result::InvalidFormat;
    if (requested.sampleRate == 0) return Result::InvalidFormat;

    EnsureLibrary();

    Stream* opened = new Stream();
    opened->format = requested;
    opened->fill = fill;
    opened->context = context;
    opened->sampleBytes = SampleBytes(requested.sampleFormat);

    opened->loop = pw_thread_loop_new("acl-audio", nullptr);
    if (!opened->loop)
    {
        delete opened;
        return Result::ConnectionFailed;
    }

    pw_properties* properties = pw_properties_new(PW_KEY_MEDIA_TYPE, "Audio", PW_KEY_MEDIA_CATEGORY, "Playback", PW_KEY_MEDIA_ROLE,
                                                  "Game", PW_KEY_NODE_NAME, "acl", nullptr);

    char latency[64];
    snprintf(latency, sizeof(latency), "%u/%u", requested.framesPerBurst, requested.sampleRate);
    pw_properties_set(properties, PW_KEY_NODE_LATENCY, latency);

    pw_thread_loop_lock(opened->loop);

    opened->stream = pw_stream_new_simple(pw_thread_loop_get_loop(opened->loop), "acl", properties, &STREAM_EVENTS, opened);
    if (!opened->stream)
    {
        pw_thread_loop_unlock(opened->loop);
        pw_thread_loop_destroy(opened->loop);
        delete opened;
        return Result::StreamFailed;
    }

    uint8_t podStorage[1024];
    spa_pod_builder builder{};
    builder.data = podStorage;
    builder.size = sizeof(podStorage);

    spa_audio_info_raw info{};
    info.format = requested.sampleFormat == SampleFormat::Signed16 ? SPA_AUDIO_FORMAT_S16 : SPA_AUDIO_FORMAT_F32;
    info.rate = requested.sampleRate;
    info.channels = requested.channels;

    const spa_pod* params[1];
    params[0] = spa_format_audio_raw_build(&builder, SPA_PARAM_EnumFormat, &info);

    int connected = pw_stream_connect(opened->stream, PW_DIRECTION_OUTPUT, PW_ID_ANY,
                                      (pw_stream_flags)(PW_STREAM_FLAG_AUTOCONNECT | PW_STREAM_FLAG_MAP_BUFFERS), params, 1);

    pw_thread_loop_unlock(opened->loop);

    if (connected < 0)
    {
        pw_stream_destroy(opened->stream);
        pw_thread_loop_destroy(opened->loop);
        delete opened;
        return Result::ConnectionFailed;
    }

    opened->capacityInFrames = requested.framesPerBurst * 4;
    stream = opened;

    return Result::Ok;
}

AudioDevice::Result AudioDevice::Start(Stream& stream)
{
    if (!stream.loop || !stream.stream) return Result::NotOpen;
    if (stream.running) return Result::Ok;

    if (pw_thread_loop_start(stream.loop) < 0) return Result::ConnectionFailed;

    pw_thread_loop_lock(stream.loop);
    pw_stream_set_active(stream.stream, true);
    pw_thread_loop_unlock(stream.loop);

    stream.running = true;

    return Result::Ok;
}

AudioDevice::Result AudioDevice::Stop(Stream& stream)
{
    if (!stream.loop || !stream.stream) return Result::NotOpen;
    if (!stream.running) return Result::Ok;

    pw_thread_loop_lock(stream.loop);
    pw_stream_set_active(stream.stream, false);
    pw_thread_loop_unlock(stream.loop);

    pw_thread_loop_stop(stream.loop);
    stream.running = false;
    return Result::Ok;
}

void AudioDevice::Close(Stream*& stream)
{
    if (!stream) return;

    if (stream->running) Stop(*stream);
    if (stream->stream) pw_stream_destroy(stream->stream);
    if (stream->loop) pw_thread_loop_destroy(stream->loop);

    delete stream;
    stream = nullptr;
}

AudioDevice::Format AudioDevice::DescribeFormat(const Stream& stream)
{
    return stream.format;
}

uint32_t AudioDevice::BufferCapacityInFrames(const Stream& stream)
{
    return stream.capacityInFrames;
}

uint64_t AudioDevice::UnderrunCount(const Stream& stream)
{
    return stream.underruns;
}

#endif

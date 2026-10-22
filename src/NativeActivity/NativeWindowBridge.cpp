#include "NativeActivity/NativeActivity.hpp"

#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <map>
#include <memory>
#include <mutex>
#include <poll.h>
#include <sys/eventfd.h>
#include <string>
#include <unistd.h>
#include <vector>

#include "AbiBridge/AbiBridge.hpp"
#include "Settings/Settings.hpp"

static constexpr uint64_t GUEST_WINDOW_BYTES = 64;

struct GuestWindowRecord
{
    GuestState::GuestAddress address{};
    int32_t width = 0;
    int32_t height = 0;
    std::atomic<int32_t> references{0};
};

static GuestWindowRecord gWindow;
static std::atomic<bool> gFinishRequested{false};

static AbiBridge::Result WindowAcquireHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    if (registers.general[0] == (uint64_t)gWindow.address) gWindow.references.fetch_add(1);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowReleaseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    if (registers.general[0] == (uint64_t)gWindow.address) gWindow.references.fetch_sub(1);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowWidthHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = registers.general[0] == (uint64_t)gWindow.address ? (uint64_t)(uint32_t)gWindow.width : 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowHeightHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = registers.general[0] == (uint64_t)gWindow.address ? (uint64_t)(uint32_t)gWindow.height : 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowFromSurfaceHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = (uint64_t)gWindow.address;
    if ((uint64_t)gWindow.address != 0) gWindow.references.fetch_add(1);

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowFormatHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = registers.general[0] == (uint64_t)gWindow.address ? 1u : 0u;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ActivityFinishHandler(void*, GuestState::Registers&, const GuestState::MemoryAccessor&)
{
    gFinishRequested.store(true);
    fprintf(stderr, "[nativeactivity] the guest asked the activity to finish\n");
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ActivitySetWindowFlagsHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    static bool reported = false;

    if (!reported)
    {
        reported = true;
        fprintf(stderr, "[nativeactivity] the guest set window flags add %llu clear %llu, which this runtime does not act on\n",
                (unsigned long long)registers.general[1], (unsigned long long)registers.general[2]);
    }

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ActivityShowSoftInputHandler(void*, GuestState::Registers&, const GuestState::MemoryAccessor&)
{
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ActivityHideSoftInputHandler(void*, GuestState::Registers&, const GuestState::MemoryAccessor&)
{
    return AbiBridge::Result::Ok;
}

static constexpr uint64_t GUEST_LOOPER_BYTES = 64;
static constexpr int IDLE_POLL_MILLISECONDS = 4;
static constexpr uint64_t GUEST_CONFIGURATION_BYTES = 64;

struct LooperWatch
{
    int descriptor = -1;
    int identifier = 0;
    int events = 0;
    uint64_t callback = 0;
    uint64_t data = 0;
};

struct FrameCallbackRequest
{
    uint64_t function = 0;
    uint64_t data = 0;
    bool wide = false;
};

struct GuestLooperRecord
{
    GuestState::GuestAddress address{};
    std::mutex lock;
    std::vector<LooperWatch> watches;
    std::vector<FrameCallbackRequest> pendingFrames;
    std::atomic<int32_t> references{0};
    int wakeDescriptor = -1;

    ~GuestLooperRecord()
    {
        if (wakeDescriptor >= 0) close(wakeDescriptor);
    }
};

static constexpr const char* FRAME_TRACE_VARIABLE = "RUNTIME_TRACE_FRAME_CALLBACKS";

static std::atomic<uint64_t> gFramesPosted{0};
static std::atomic<uint64_t> gFramesDelivered{0};

static bool FrameTracingRequested()
{
    static const bool requested = []() {
        const char* setting = Settings::Lookup(FRAME_TRACE_VARIABLE);
        return setting != nullptr && strcmp(setting, "0") != 0;
    }();

    return requested;
}

static void WakeLooper(const std::shared_ptr<GuestLooperRecord>& looper)
{
    if (!looper || looper->wakeDescriptor < 0) return;

    uint64_t one = 1;
    ssize_t written = write(looper->wakeDescriptor, &one, sizeof(one));
    (void)written;
}

static std::mutex gLooperRegistryLock;
static std::map<uint64_t, std::shared_ptr<GuestLooperRecord>> gLoopersByThread;
static std::map<uint64_t, std::shared_ptr<GuestLooperRecord>> gLoopersByAddress;
static uint64_t gNextLooperName = 0;

static std::shared_ptr<GuestLooperRecord> LooperForCallingThread()
{
    std::lock_guard<std::mutex> guard(gLooperRegistryLock);

    auto found = gLoopersByThread.find((uint64_t)gettid());
    return found == gLoopersByThread.end() ? nullptr : found->second;
}

static std::shared_ptr<GuestLooperRecord> LooperForAddress(uint64_t address)
{
    if (address == 0) return nullptr;

    std::lock_guard<std::mutex> guard(gLooperRegistryLock);

    auto found = gLoopersByAddress.find(address);
    return found == gLoopersByAddress.end() ? nullptr : found->second;
}

static std::shared_ptr<GuestLooperRecord> PrepareLooperForCallingThread(GuestRuntime::Instance& runtime)
{
    std::shared_ptr<GuestLooperRecord> existing = LooperForCallingThread();
    if (existing) return existing;

    std::shared_ptr<GuestLooperRecord> prepared = std::make_shared<GuestLooperRecord>();
    prepared->wakeDescriptor = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);

    std::string name;
    {
        std::lock_guard<std::mutex> guard(gLooperRegistryLock);
        name = "acl:looper:" + std::to_string(gNextLooperName++);
    }

    if (GuestRuntime::RegisterDataObject(runtime, name, GUEST_LOOPER_BYTES, prepared->address) != GuestRuntime::Result::Ok)
        return nullptr;

    std::lock_guard<std::mutex> guard(gLooperRegistryLock);
    gLoopersByThread[(uint64_t)gettid()] = prepared;
    gLoopersByAddress[(uint64_t)prepared->address] = prepared;

    return prepared;
}
static GuestState::GuestAddress gConfiguration{};

static constexpr int32_t POLL_WAKE = -1;
static constexpr int32_t POLL_CALLBACK = -2;
static constexpr int32_t POLL_TIMEOUT = -3;
static constexpr int32_t POLL_ERROR = -4;

static void DeliverPendingFrames();
static void RunLooperCallback(const GuestState::Registers& caller, uint64_t callback, int descriptor, int events, uint64_t data);

static AbiBridge::Result LooperPrepareHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestRuntime::Instance* runtime = (GuestRuntime::Instance*)context;

    std::shared_ptr<GuestLooperRecord> prepared = PrepareLooperForCallingThread(*runtime);
    registers.general[0] = prepared ? (uint64_t)prepared->address : 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperForThreadHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::shared_ptr<GuestLooperRecord> existing = LooperForCallingThread();
    registers.general[0] = existing ? (uint64_t)existing->address : 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperAcquireHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::shared_ptr<GuestLooperRecord> looper = LooperForAddress(registers.general[0]);
    if (looper) looper->references.fetch_add(1);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperReleaseHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    std::shared_ptr<GuestLooperRecord> looper = LooperForAddress(registers.general[0]);
    if (looper) looper->references.fetch_sub(1);
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperAddDescriptorHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    LooperWatch watch;
    watch.descriptor = (int)(int32_t)(uint32_t)registers.general[1];
    watch.identifier = (int)(int32_t)(uint32_t)registers.general[2];
    watch.events = (int)(int32_t)(uint32_t)registers.general[3];
    watch.callback = registers.general[4];
    watch.data = registers.general[5];

    std::shared_ptr<GuestLooperRecord> looper = LooperForAddress(registers.general[0]);

    if (!looper)
    {
        registers.general[0] = (uint64_t)(uint32_t)(int32_t)-1;
        return AbiBridge::Result::Ok;
    }

    {
        std::lock_guard<std::mutex> guard(looper->lock);

        bool replaced = false;
        for (LooperWatch& existing : looper->watches)
        {
            if (existing.descriptor != watch.descriptor) continue;

            existing = watch;
            replaced = true;
            break;
        }

        if (!replaced) looper->watches.push_back(watch);
    }

    WakeLooper(looper);

    registers.general[0] = 1;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperRemoveDescriptorHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    int descriptor = (int)(int32_t)(uint32_t)registers.general[1];

    std::shared_ptr<GuestLooperRecord> looper = LooperForAddress(registers.general[0]);

    if (looper)
    {
        std::lock_guard<std::mutex> guard(looper->lock);

        for (size_t index = 0; index < looper->watches.size(); ++index)
        {
            if (looper->watches[index].descriptor != descriptor) continue;

            looper->watches.erase(looper->watches.begin() + (long)index);
            break;
        }
    }

    WakeLooper(looper);

    registers.general[0] = 1;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result LooperPollHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    int timeout = (int)(int32_t)(uint32_t)registers.general[0];
    uint64_t descriptorOut = registers.general[1];
    uint64_t eventsOut = registers.general[2];
    uint64_t dataOut = registers.general[3];

    DeliverPendingFrames();

    std::shared_ptr<GuestLooperRecord> looper = LooperForCallingThread();

    std::vector<LooperWatch> watches;
    if (looper)
    {
        std::lock_guard<std::mutex> guard(looper->lock);
        watches = looper->watches;
    }

    std::vector<pollfd> descriptors;
    descriptors.reserve(watches.size() + 1);

    for (const LooperWatch& watch : watches)
    {
        pollfd entry{};
        entry.fd = watch.descriptor;
        entry.events = (short)(watch.events & (POLLIN | POLLOUT));
        descriptors.push_back(entry);
    }

    size_t wakeIndex = descriptors.size();
    if (looper && looper->wakeDescriptor >= 0)
    {
        pollfd entry{};
        entry.fd = looper->wakeDescriptor;
        entry.events = POLLIN;
        descriptors.push_back(entry);
    }

    int ready = 0;

    if (!descriptors.empty())
    {
        ready = poll(descriptors.data(), (nfds_t)descriptors.size(), timeout);
    }
    else if (timeout != 0)
    {
        int waited = timeout < 0 || timeout > IDLE_POLL_MILLISECONDS ? IDLE_POLL_MILLISECONDS : timeout;

        struct timespec span;
        span.tv_sec = waited / 1000;
        span.tv_nsec = (long)(waited % 1000) * 1000000l;
        nanosleep(&span, nullptr);
    }

    if (ready <= 0)
    {
        registers.general[0] = (uint64_t)(uint32_t)(int32_t)(ready < 0 ? POLL_ERROR : POLL_TIMEOUT);
        return AbiBridge::Result::Ok;
    }

    if (wakeIndex < descriptors.size() && descriptors[wakeIndex].revents != 0)
    {
        uint64_t drained = 0;
        ssize_t read = ::read(looper->wakeDescriptor, &drained, sizeof(drained));
        (void)read;

        DeliverPendingFrames();

        if (ready == 1)
        {
            registers.general[0] = (uint64_t)(uint32_t)(int32_t)POLL_WAKE;
            return AbiBridge::Result::Ok;
        }
    }

    for (size_t index = 0; index < descriptors.size(); ++index)
    {
        if (descriptors[index].revents == 0) continue;

        int32_t descriptor = descriptors[index].fd;
        int32_t events = descriptors[index].revents;

        if (descriptorOut != 0)
            memory.Write(memory.context, (GuestState::GuestAddress)descriptorOut, &descriptor, sizeof(descriptor));
        if (eventsOut != 0) memory.Write(memory.context, (GuestState::GuestAddress)eventsOut, &events, sizeof(events));
        if (dataOut != 0)
        {
            uint64_t data = watches[index].data;
            memory.Write(memory.context, (GuestState::GuestAddress)dataOut, &data, sizeof(data));
        }

        if (watches[index].callback != 0)
        {
            RunLooperCallback(registers, watches[index].callback, descriptor, events, watches[index].data);
            registers.general[0] = (uint64_t)(uint32_t)(int32_t)POLL_CALLBACK;
            return AbiBridge::Result::Ok;
        }

        registers.general[0] = (uint64_t)(uint32_t)watches[index].identifier;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)(uint32_t)(int32_t)-3;
    return AbiBridge::Result::Ok;
}

static GuestState::GuestAddress gChoreographer{};
static GuestState::GuestAddress gAssetManager{};
static GuestRuntime::Instance* gLooperRuntime = nullptr;

static AbiBridge::Result ChoreographerInstanceHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestRuntime::Instance* runtime = (GuestRuntime::Instance*)context;

    if ((uint64_t)gChoreographer == 0 &&
        GuestRuntime::RegisterDataObject(*runtime, "acl:choreographer", GUEST_LOOPER_BYTES, gChoreographer) !=
            GuestRuntime::Result::Ok)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)gChoreographer;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ChoreographerPostFrameHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    FrameCallbackRequest pending;
    pending.function = registers.general[1];
    pending.data = registers.general[2];
    pending.wide = context != nullptr;

    std::shared_ptr<GuestLooperRecord> looper = LooperForCallingThread();

    if (pending.function != 0 && looper)
    {
        {
            std::lock_guard<std::mutex> guard(looper->lock);
            looper->pendingFrames.push_back(pending);
        }

        WakeLooper(looper);
    }

    if (FrameTracingRequested())
    {
        uint64_t posted = gFramesPosted.fetch_add(1) + 1;
        if (posted <= 8 || posted % 256 == 0)
            fprintf(stderr, "[nativeactivity] frame callback %llu posted on thread %d, looper %s\n", (unsigned long long)posted,
                    (int)gettid(), looper ? "present" : "missing");
    }

    return AbiBridge::Result::Ok;
}

static void RunLooperCallback(const GuestState::Registers& caller, uint64_t callback, int descriptor, int events, uint64_t data)
{
    if (!gLooperRuntime || callback == 0) return;

    uint64_t arguments[3];
    arguments[0] = (uint64_t)(uint32_t)descriptor;
    arguments[1] = (uint64_t)(uint32_t)events;
    arguments[2] = data;

    uint64_t returned = 0;
    GuestRuntime::CallGuestNested(*gLooperRuntime, caller, (GuestState::GuestAddress)callback, arguments, 3, returned);
}

static void DeliverPendingFrames()
{
    if (!gLooperRuntime) return;

    std::shared_ptr<GuestLooperRecord> looper = LooperForCallingThread();
    if (!looper) return;

    std::vector<FrameCallbackRequest> due;
    {
        std::lock_guard<std::mutex> guard(looper->lock);
        due.swap(looper->pendingFrames);
    }

    if (!due.empty() && FrameTracingRequested())
    {
        uint64_t delivered = gFramesDelivered.fetch_add(due.size()) + due.size();
        if (delivered <= 8 || delivered % 256 == 0)
            fprintf(stderr, "[nativeactivity] frame callback %llu delivered on thread %d\n", (unsigned long long)delivered,
                    (int)gettid());
    }

    for (const FrameCallbackRequest& pending : due)
    {
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        uint64_t nanoseconds = (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;

        GuestState::Registers registers;
        GuestState::Reset(registers);
        registers.general[0] = nanoseconds;
        registers.general[1] = pending.data;

        GuestRuntime::CallGuest(*gLooperRuntime, (GuestState::GuestAddress)pending.function, registers);
    }
}

static AbiBridge::Result LooperWakeHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    WakeLooper(LooperForAddress(registers.general[0]));
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result WindowSetGeometryHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result AssetManagerFromJavaHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestRuntime::Instance* runtime = (GuestRuntime::Instance*)context;

    if ((uint64_t)gAssetManager == 0 &&
        GuestRuntime::RegisterDataObject(*runtime, "acl:assets", GUEST_LOOPER_BYTES, gAssetManager) != GuestRuntime::Result::Ok)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)gAssetManager;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result SensorAbsentHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result InputQueueIdleHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result InputQueueEmptyHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t eventOut = registers.general[1];
    uint64_t none = 0;

    if (eventOut != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)eventOut, &none, sizeof(none)))
        return AbiBridge::Result::MalformedArguments;

    registers.general[0] = (uint64_t)(int64_t)-1;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConfigurationNewHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    GuestRuntime::Instance* runtime = (GuestRuntime::Instance*)context;

    if ((uint64_t)gConfiguration == 0 &&
        GuestRuntime::RegisterDataObject(*runtime, "acl:configuration", GUEST_CONFIGURATION_BYTES, gConfiguration) !=
            GuestRuntime::Result::Ok)
    {
        registers.general[0] = 0;
        return AbiBridge::Result::Ok;
    }

    registers.general[0] = (uint64_t)gConfiguration;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConfigurationIgnoredHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 0;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConfigurationSdkVersionHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor&)
{
    registers.general[0] = 34;
    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConfigurationLanguageHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t out = registers.general[1];
    const char letters[2] = {'e', 'n'};

    if (out != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)out, letters, sizeof(letters)))
        return AbiBridge::Result::MalformedArguments;

    return AbiBridge::Result::Ok;
}

static AbiBridge::Result ConfigurationCountryHandler(void*, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t out = registers.general[1];
    const char letters[2] = {'U', 'S'};

    if (out != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)out, letters, sizeof(letters)))
        return AbiBridge::Result::MalformedArguments;

    return AbiBridge::Result::Ok;
}

NativeActivity::Result NativeActivity::RegisterWindowFunctions(GuestRuntime::Instance& runtime)
{
    struct HandlerEntry
    {
        const char* name;
        AbiBridge::GuestCallHandler handler;
        void* context;
    };

    const HandlerEntry handlers[] = {
        {"ALooper_prepare", &LooperPrepareHandler, &runtime},
        {"ALooper_forThread", &LooperForThreadHandler, &runtime},
        {"ALooper_acquire", &LooperAcquireHandler, nullptr},
        {"ALooper_release", &LooperReleaseHandler, nullptr},
        {"ALooper_addFd", &LooperAddDescriptorHandler, nullptr},
        {"ALooper_removeFd", &LooperRemoveDescriptorHandler, nullptr},
        {"ALooper_pollOnce", &LooperPollHandler, nullptr},
        {"ALooper_pollAll", &LooperPollHandler, nullptr},
        {"ALooper_wake", &LooperWakeHandler, nullptr},
        {"AChoreographer_getInstance", &ChoreographerInstanceHandler, &runtime},
        {"AChoreographer_postFrameCallback", &ChoreographerPostFrameHandler, nullptr},
        {"AChoreographer_postFrameCallback64", &ChoreographerPostFrameHandler, &runtime},
        {"AAssetManager_fromJava", &AssetManagerFromJavaHandler, &runtime},
        {"ANativeWindow_setBuffersGeometry", &WindowSetGeometryHandler, nullptr},
        {"ASensorManager_getInstance", &SensorAbsentHandler, nullptr},
        {"ASensorManager_getDefaultSensor", &SensorAbsentHandler, nullptr},
        {"ASensorManager_getSensorList", &SensorAbsentHandler, nullptr},
        {"ASensorManager_createEventQueue", &SensorAbsentHandler, nullptr},
        {"ASensorManager_destroyEventQueue", &SensorAbsentHandler, nullptr},
        {"ASensorEventQueue_enableSensor", &SensorAbsentHandler, nullptr},
        {"ASensorEventQueue_disableSensor", &SensorAbsentHandler, nullptr},
        {"ASensorEventQueue_setEventRate", &SensorAbsentHandler, nullptr},
        {"ASensorEventQueue_hasEvents", &SensorAbsentHandler, nullptr},
        {"ASensorEventQueue_getEvents", &SensorAbsentHandler, nullptr},
        {"ASensor_getName", &SensorAbsentHandler, nullptr},
        {"ASensor_getVendor", &SensorAbsentHandler, nullptr},
        {"ASensor_getType", &SensorAbsentHandler, nullptr},
        {"ASensor_getResolution", &SensorAbsentHandler, nullptr},
        {"ASensor_getMinDelay", &SensorAbsentHandler, nullptr},
        {"AConfiguration_new", &ConfigurationNewHandler, &runtime},
        {"AConfiguration_delete", &ConfigurationIgnoredHandler, nullptr},
        {"AConfiguration_fromAssetManager", &ConfigurationIgnoredHandler, nullptr},
        {"AConfiguration_getSdkVersion", &ConfigurationSdkVersionHandler, nullptr},
        {"AConfiguration_getLanguage", &ConfigurationLanguageHandler, nullptr},
        {"AConfiguration_getCountry", &ConfigurationCountryHandler, nullptr},
        {"ANativeWindow_acquire", &WindowAcquireHandler, nullptr},
        {"ANativeWindow_release", &WindowReleaseHandler, nullptr},
        {"ANativeWindow_getWidth", &WindowWidthHandler, nullptr},
        {"ANativeWindow_getHeight", &WindowHeightHandler, nullptr},
        {"ANativeActivity_finish", &ActivityFinishHandler, nullptr},
        {"ANativeActivity_setWindowFlags", &ActivitySetWindowFlagsHandler, nullptr},
        {"ANativeActivity_showSoftInput", &ActivityShowSoftInputHandler, nullptr},
        {"ANativeActivity_hideSoftInput", &ActivityHideSoftInputHandler, nullptr},
        {"ANativeWindow_fromSurface", &WindowFromSurfaceHandler, nullptr},
        {"ANativeWindow_getFormat", &WindowFormatHandler, nullptr},
        {"AInputQueue_attachLooper", &InputQueueIdleHandler, nullptr},
        {"AInputQueue_detachLooper", &InputQueueIdleHandler, nullptr},
        {"AInputQueue_hasEvents", &InputQueueIdleHandler, nullptr},
        {"AInputQueue_getEvent", &InputQueueEmptyHandler, nullptr},
        {"AInputQueue_preDispatchEvent", &InputQueueIdleHandler, nullptr},
        {"AInputQueue_finishEvent", &InputQueueIdleHandler, nullptr},
    };

    gLooperRuntime = &runtime;
    AbiBridge::Registry& registry = GuestRuntime::BridgeRegistry(runtime);

    for (const HandlerEntry& entry : handlers)
    {
        AbiBridge::Result registered = AbiBridge::RegisterHostHandler(registry, entry.name, entry.handler, entry.context);

        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered)
        {
            fprintf(stderr, "[nativeactivity] cannot register %s: %s\n", entry.name, AbiBridge::ResultName(registered));
            return Result::JniRegistrationFailed;
        }
    }

    return Result::Ok;
}

void NativeActivity::PrepareLooper()
{
    if (!gLooperRuntime) return;
    PrepareLooperForCallingThread(*gLooperRuntime);
}

bool NativeActivity::ServiceLooper()
{
    if (!gLooperRuntime) return false;

    DeliverPendingFrames();

    std::shared_ptr<GuestLooperRecord> looper = LooperForCallingThread();
    if (!looper) return false;

    std::vector<LooperWatch> watches;
    {
        std::lock_guard<std::mutex> guard(looper->lock);
        watches = looper->watches;
    }

    if (watches.empty()) return false;

    std::vector<pollfd> descriptors;
    descriptors.reserve(watches.size());

    for (const LooperWatch& watch : watches)
    {
        pollfd entry{};
        entry.fd = watch.descriptor;
        entry.events = (short)(watch.events & (POLLIN | POLLOUT));
        descriptors.push_back(entry);
    }

    if (poll(descriptors.data(), (nfds_t)descriptors.size(), 0) <= 0) return true;

    for (size_t index = 0; index < descriptors.size(); ++index)
    {
        if (descriptors[index].revents == 0) continue;
        if (watches[index].callback == 0) continue;

        GuestState::Registers registers;
        GuestState::Reset(registers);
        registers.general[0] = (uint64_t)(uint32_t)descriptors[index].fd;
        registers.general[1] = (uint64_t)(uint32_t)descriptors[index].revents;
        registers.general[2] = watches[index].data;

        GuestRuntime::CallGuest(*gLooperRuntime, (GuestState::GuestAddress)watches[index].callback, registers);
    }

    return true;
}

NativeActivity::Result NativeActivity::CreateWindow(GuestRuntime::Instance& runtime, int32_t width, int32_t height,
                                                    GuestState::GuestAddress& window)
{
    if ((uint64_t)gWindow.address == 0 &&
        GuestRuntime::RegisterDataObject(runtime, "acl:window", GUEST_WINDOW_BYTES, gWindow.address) != GuestRuntime::Result::Ok)
    {
        return Result::OutOfGuestMemory;
    }

    gWindow.width = width;
    gWindow.height = height;
    window = gWindow.address;

    fprintf(stderr, "[nativeactivity] the guest window is %dx%d\n", width, height);
    return Result::Ok;
}

bool NativeActivity::FinishRequested()
{
    return gFinishRequested.load();
}

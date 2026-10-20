#include "NativeActivity/NativeActivity.hpp"

#include <cstddef>
#include <cstdio>
#include <cstring>

#include <android/native_activity.h>

#include "AbiBridge/AbiBridge.hpp"
#include "ElfLoader/ElfLoader.hpp"

static constexpr int CALLBACK_COUNT = (int)NativeActivity::Callback::Count;
static constexpr int32_t GUEST_SDK_VERSION = RUNTIME_BUILD_SDK_VERSION;

struct NativeActivityState
{
    GuestRuntime::Instance* runtime = nullptr;
    GuestState::GuestAddress activityAddress{};
    GuestState::GuestAddress callbacksAddress{};
    GuestState::GuestAddress dataPathAddress{};
    uint64_t callbacks[CALLBACK_COUNT] = {};
    NativeActivity::Statistics statistics;
};

static NativeActivityState* Implementation(const NativeActivity::Instance& activity)
{
    return (NativeActivityState*)activity.implementation;
}

static uint64_t CallbackOffset(NativeActivity::Callback callback)
{
    switch (callback)
    {
        case NativeActivity::Callback::Start: return offsetof(ANativeActivityCallbacks, onStart);
        case NativeActivity::Callback::Resume: return offsetof(ANativeActivityCallbacks, onResume);
        case NativeActivity::Callback::SaveInstanceState: return offsetof(ANativeActivityCallbacks, onSaveInstanceState);
        case NativeActivity::Callback::Pause: return offsetof(ANativeActivityCallbacks, onPause);
        case NativeActivity::Callback::Stop: return offsetof(ANativeActivityCallbacks, onStop);
        case NativeActivity::Callback::Destroy: return offsetof(ANativeActivityCallbacks, onDestroy);
        case NativeActivity::Callback::WindowFocusChanged: return offsetof(ANativeActivityCallbacks, onWindowFocusChanged);
        case NativeActivity::Callback::NativeWindowCreated: return offsetof(ANativeActivityCallbacks, onNativeWindowCreated);
        case NativeActivity::Callback::NativeWindowResized: return offsetof(ANativeActivityCallbacks, onNativeWindowResized);
        case NativeActivity::Callback::NativeWindowRedrawNeeded: return offsetof(ANativeActivityCallbacks, onNativeWindowRedrawNeeded);
        case NativeActivity::Callback::NativeWindowDestroyed: return offsetof(ANativeActivityCallbacks, onNativeWindowDestroyed);
        case NativeActivity::Callback::InputQueueCreated: return offsetof(ANativeActivityCallbacks, onInputQueueCreated);
        case NativeActivity::Callback::InputQueueDestroyed: return offsetof(ANativeActivityCallbacks, onInputQueueDestroyed);
        case NativeActivity::Callback::ContentRectChanged: return offsetof(ANativeActivityCallbacks, onContentRectChanged);
        case NativeActivity::Callback::ConfigurationChanged: return offsetof(ANativeActivityCallbacks, onConfigurationChanged);
        case NativeActivity::Callback::LowMemory: return offsetof(ANativeActivityCallbacks, onLowMemory);
        default: break;
    }

    return 0;
}

static bool WriteGuestWord(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t value)
{
    return memory.Write(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static bool ReadGuestWord(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t& value)
{
    return memory.Read(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static NativeActivity::Result PublishDataPath(GuestRuntime::Instance& runtime, const std::string& path, GuestState::GuestAddress& address)
{
    uint64_t bytes = path.size() + 1;

    if (GuestRuntime::Reserve(runtime, bytes, sizeof(uint64_t), address) != GuestRuntime::Result::Ok)
        return NativeActivity::Result::OutOfGuestMemory;

    if (GuestRuntime::Commit(runtime, address, bytes, true, true, false) != GuestRuntime::Result::Ok)
        return NativeActivity::Result::OutOfGuestMemory;

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(runtime);
    if (!memory.Write(memory.context, address, path.c_str(), bytes)) return NativeActivity::Result::OutOfGuestMemory;

    return NativeActivity::Result::Ok;
}

NativeActivity::Result NativeActivity::Create(GuestRuntime::Instance& runtime, JniBridge::Instance& bridge, uint32_t moduleId,
                                              const std::string& internalDataPath, void* activityObject, Instance& activity)
{
    GuestState::GuestAddress entry{};
    if (ElfLoader::LookupExportIn(GuestRuntime::LibraryNamespace(runtime), moduleId, ENTRY_POINT_SYMBOL, entry) != ElfLoader::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] the guest library does not export %s\n", ENTRY_POINT_SYMBOL);
        return Result::NoEntryPoint;
    }

    NativeActivityState* state = new NativeActivityState();
    state->runtime = &runtime;

    Result published = PublishDataPath(runtime, internalDataPath, state->dataPathAddress);
    if (published != Result::Ok)
    {
        delete state;
        return published;
    }

    if (GuestRuntime::RegisterDataObject(runtime, "acl:activity", sizeof(ANativeActivity), state->activityAddress) !=
            GuestRuntime::Result::Ok ||
        GuestRuntime::RegisterDataObject(runtime, "acl:activity:callbacks", sizeof(ANativeActivityCallbacks), state->callbacksAddress) !=
            GuestRuntime::Result::Ok)
    {
        delete state;
        return Result::OutOfGuestMemory;
    }

    GuestState::GuestAddress guestVm{};
    GuestState::GuestAddress guestEnv{};
    JniBridge::GuestJavaVm(bridge, guestVm);
    JniBridge::GuestJniEnv(bridge, guestEnv);

    const GuestState::MemoryAccessor& memory = GuestRuntime::Memory(runtime);
    uint64_t base = (uint64_t)state->activityAddress;

    bool written = WriteGuestWord(memory, base + offsetof(ANativeActivity, callbacks), (uint64_t)state->callbacksAddress) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, vm), (uint64_t)guestVm) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, env), (uint64_t)guestEnv) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, clazz), (uint64_t)activityObject) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, internalDataPath), (uint64_t)state->dataPathAddress) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, externalDataPath), (uint64_t)state->dataPathAddress) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, instance), 0) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, assetManager), 0) &&
                   WriteGuestWord(memory, base + offsetof(ANativeActivity, obbPath), (uint64_t)state->dataPathAddress);

    int32_t sdkVersion = GUEST_SDK_VERSION;
    written = written && memory.Write(memory.context, (GuestState::GuestAddress)(base + offsetof(ANativeActivity, sdkVersion)),
                                      &sdkVersion, sizeof(sdkVersion));

    if (!written)
    {
        delete state;
        return Result::OutOfGuestMemory;
    }

    for (uint64_t offset = 0; offset < sizeof(ANativeActivityCallbacks); offset += sizeof(uint64_t))
    {
        if (WriteGuestWord(memory, (uint64_t)state->callbacksAddress + offset, 0)) continue;

        delete state;
        return Result::OutOfGuestMemory;
    }

    AbiBridge::Argument pointer;
    pointer.argumentClass = AbiBridge::ArgumentClass::Pointer;
    pointer.sizeInBytes = sizeof(void*);
    pointer.alignmentInBytes = sizeof(void*);

    AbiBridge::Signature signature;
    signature.arguments[0] = pointer;
    signature.arguments[1] = pointer;
    signature.arguments[2] = pointer;
    signature.totalArgumentCount = 3;
    signature.namedArgumentCount = 3;

    uint64_t slots[3] = {(uint64_t)state->activityAddress, 0, 0};

    AbiBridge::ArgumentBuffer arguments;
    arguments.slots = slots;
    arguments.slotCount = 3;
    arguments.slotStrideBytes = sizeof(uint64_t);

    AbiBridge::Result entered = AbiBridge::CallGuestFromHost(GuestRuntime::Caller(runtime), entry, signature, arguments, nullptr, 0);
    if (entered != AbiBridge::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] %s failed: %s\n", ENTRY_POINT_SYMBOL, AbiBridge::ResultName(entered));
        delete state;
        return Result::GuestFaulted;
    }

    state->statistics.entryPointRan = true;

    for (int index = 0; index < CALLBACK_COUNT; ++index)
    {
        uint64_t slot = 0;
        if (!ReadGuestWord(memory, (uint64_t)state->callbacksAddress + CallbackOffset((Callback)index), slot)) continue;

        state->callbacks[index] = slot;
        if (slot != 0) state->statistics.callbacksRegistered += 1;
    }

    fprintf(stderr, "[nativeactivity] %s ran and registered %llu callback(s)\n", ENTRY_POINT_SYMBOL,
            (unsigned long long)state->statistics.callbacksRegistered);

    activity.implementation = state;
    return Result::Ok;
}

void NativeActivity::Destroy(Instance& activity)
{
    delete Implementation(activity);
    activity.implementation = nullptr;
}

bool NativeActivity::HasCallback(const Instance& activity, Callback callback)
{
    const NativeActivityState* state = Implementation(activity);
    if (!state || callback >= Callback::Count) return false;

    return state->callbacks[(int)callback] != 0;
}

NativeActivity::Result NativeActivity::Invoke(Instance& activity, Callback callback, uint64_t argument)
{
    NativeActivityState* state = Implementation(activity);
    if (!state) return Result::NotInitialized;
    if (callback >= Callback::Count) return Result::NoCallback;

    uint64_t target = state->callbacks[(int)callback];
    if (target == 0)
    {
        state->statistics.callbacksAbsent += 1;
        return Result::NoCallback;
    }

    AbiBridge::Argument pointer;
    pointer.argumentClass = AbiBridge::ArgumentClass::Pointer;
    pointer.sizeInBytes = sizeof(void*);
    pointer.alignmentInBytes = sizeof(void*);

    AbiBridge::Signature signature;
    signature.arguments[0] = pointer;
    signature.arguments[1] = pointer;
    signature.totalArgumentCount = 2;
    signature.namedArgumentCount = 2;

    uint64_t slots[2] = {(uint64_t)state->activityAddress, argument};

    AbiBridge::ArgumentBuffer arguments;
    arguments.slots = slots;
    arguments.slotCount = 2;
    arguments.slotStrideBytes = sizeof(uint64_t);

    AbiBridge::Result entered = AbiBridge::CallGuestFromHost(GuestRuntime::Caller(*state->runtime),
                                                             (GuestState::GuestAddress)target, signature, arguments, nullptr, 0);

    if (entered != AbiBridge::Result::Ok)
    {
        fprintf(stderr, "[nativeactivity] %s failed: %s\n", CallbackName(callback), AbiBridge::ResultName(entered));
        return Result::GuestFaulted;
    }

    state->statistics.callbacksInvoked += 1;
    return Result::Ok;
}

NativeActivity::Statistics NativeActivity::GetStatistics(const Instance& activity)
{
    const NativeActivityState* state = Implementation(activity);
    if (!state) return Statistics();

    return state->statistics;
}

const char* NativeActivity::CallbackName(Callback callback)
{
    switch (callback)
    {
        case Callback::Start: return "onStart";
        case Callback::Resume: return "onResume";
        case Callback::SaveInstanceState: return "onSaveInstanceState";
        case Callback::Pause: return "onPause";
        case Callback::Stop: return "onStop";
        case Callback::Destroy: return "onDestroy";
        case Callback::WindowFocusChanged: return "onWindowFocusChanged";
        case Callback::NativeWindowCreated: return "onNativeWindowCreated";
        case Callback::NativeWindowResized: return "onNativeWindowResized";
        case Callback::NativeWindowRedrawNeeded: return "onNativeWindowRedrawNeeded";
        case Callback::NativeWindowDestroyed: return "onNativeWindowDestroyed";
        case Callback::InputQueueCreated: return "onInputQueueCreated";
        case Callback::InputQueueDestroyed: return "onInputQueueDestroyed";
        case Callback::ContentRectChanged: return "onContentRectChanged";
        case Callback::ConfigurationChanged: return "onConfigurationChanged";
        case Callback::LowMemory: return "onLowMemory";
        default: break;
    }

    return "unknown";
}

const char* NativeActivity::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NotInitialized: return "NotInitialized";
        case Result::NoEntryPoint: return "NoEntryPoint";
        case Result::OutOfGuestMemory: return "OutOfGuestMemory";
        case Result::GuestFaulted: return "GuestFaulted";
        case Result::NoCallback: return "NoCallback";
        case Result::LibraryMissing: return "LibraryMissing";
        case Result::ClassMissing: return "ClassMissing";
        case Result::JniRegistrationFailed: return "JniRegistrationFailed";
    }

    return "Unknown";
}

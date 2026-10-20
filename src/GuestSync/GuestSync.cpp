#include "GuestSync/GuestSync.hpp"

#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <mutex>
#include <thread>
#include <unistd.h>
#include <unordered_map>
#include <vector>

#include "Settings/Settings.hpp"

struct WaitRecord
{
    std::atomic<uint64_t> object{0};
    std::atomic<uint64_t> caller{0};
    std::atomic<uint64_t> startedNanos{0};
    std::atomic<uint64_t> wakesAtStart{0};
    std::atomic<int> threadId{0};
    std::atomic<uint8_t> operation{(uint8_t)GuestSync::Operation::Count};
    std::atomic<bool> active{false};
    std::atomic<bool> reported{false};
};

static std::mutex gRegistryLock;
static std::vector<WaitRecord*> gWaiters;
static std::mutex gWakeLock;
static std::unordered_map<uint64_t, uint64_t> gWakes;
static std::atomic<GuestSync::AddressDescriber> gDescriber{nullptr};
static std::atomic<void*> gDescriberContext{nullptr};
static std::atomic<bool> gDumpedOnStall{false};

static void DescribeAddress(uint64_t address, char* buffer, uint64_t bytes)
{
    GuestSync::AddressDescriber describer = gDescriber.load(std::memory_order_acquire);

    if (!describer || address == 0)
    {
        snprintf(buffer, (size_t)bytes, "%016llx", (unsigned long long)address);
        return;
    }

    describer(gDescriberContext.load(std::memory_order_relaxed), address, buffer, bytes);
}

void GuestSync::SetAddressDescriber(AddressDescriber describer, void* context)
{
    gDescriberContext.store(context, std::memory_order_relaxed);
    gDescriber.store(describer, std::memory_order_release);
}

static uint64_t MonotonicNanos()
{
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

const char* GuestSync::OperationName(Operation operation)
{
    switch (operation)
    {
        case Operation::MutexLock: return "mutex lock";
        case Operation::MutexTryLock: return "mutex trylock";
        case Operation::MutexUnlock: return "mutex unlock";
        case Operation::MutexDestroy: return "mutex destroy";
        case Operation::ConditionWait: return "condition wait";
        case Operation::ConditionTimedWait: return "condition timed wait";
        case Operation::ConditionSignal: return "condition signal";
        case Operation::ConditionBroadcast: return "condition broadcast";
        case Operation::ConditionDestroy: return "condition destroy";
        case Operation::SemaphoreWait: return "semaphore wait";
        case Operation::SemaphoreTimedWait: return "semaphore timed wait";
        case Operation::SemaphorePost: return "semaphore post";
        case Operation::OnceEnter: return "once";
        case Operation::ThreadJoin: return "thread join";
        default: return "?";
    }
}

bool GuestSync::TracingRequested()
{
    static const bool requested = Settings::Tracing("sync");

    return requested;
}

static uint64_t StallThresholdNanos()
{
    if (!GuestSync::TracingRequested()) return 0;

    return GuestSync::DEFAULT_STALL_SECONDS * 1000000000ull;
}

static bool ReportStalledWaiters(uint64_t threshold)
{
    uint64_t now = MonotonicNanos();
    bool stalled = false;

    std::lock_guard<std::mutex> guard(gRegistryLock);

    for (WaitRecord* record : gWaiters)
    {
        if (!record->active.load(std::memory_order_acquire)) continue;
        if (record->reported.load(std::memory_order_relaxed)) continue;

        uint64_t started = record->startedNanos.load(std::memory_order_relaxed);
        if (started == 0 || now - started < threshold) continue;

        uint64_t object = record->object.load(std::memory_order_relaxed);
        uint64_t wakesNow = GuestSync::WakesFor(object);
        uint64_t wakesThen = record->wakesAtStart.load(std::memory_order_relaxed);

        record->reported.store(true, std::memory_order_relaxed);

        char described[256];
        DescribeAddress(record->caller.load(std::memory_order_relaxed), described, sizeof(described));

        fprintf(stderr,
                "[guestsync] thread %d has been in %s on %016llx for %llu ms, called from %s; that object has been woken %llu time(s) "
                "since the wait began, so this is %s\n",
                record->threadId.load(std::memory_order_relaxed),
                GuestSync::OperationName((GuestSync::Operation)record->operation.load(std::memory_order_relaxed)),
                (unsigned long long)object, (unsigned long long)((now - started) / 1000000ull), described,
                (unsigned long long)(wakesNow - wakesThen), wakesNow == wakesThen ? "a wake that never came" : "a wake that was missed");

        stalled = true;
    }

    return stalled;
}

static void StartWatchdog()
{
    static std::once_flag started;

    std::call_once(started, []() {
        uint64_t threshold = StallThresholdNanos();
        if (threshold == 0) return;

        std::thread watcher([threshold]() {
            for (;;)
            {
                struct timespec span;
                span.tv_sec = (time_t)(GuestSync::WATCHDOG_INTERVAL_NANOS / 1000000000ull);
                span.tv_nsec = (long)(GuestSync::WATCHDOG_INTERVAL_NANOS % 1000000000ull);
                nanosleep(&span, nullptr);

                if (!ReportStalledWaiters(threshold)) continue;
                if (gDumpedOnStall.exchange(true)) continue;

                GuestSync::DescribeWaiters();
            }
        });

        watcher.detach();
    });
}

static WaitRecord& RecordForCallingThread()
{
    static thread_local WaitRecord* record = []() {
        WaitRecord* created = new WaitRecord();
        created->threadId.store((int)gettid(), std::memory_order_relaxed);

        std::lock_guard<std::mutex> guard(gRegistryLock);
        gWaiters.push_back(created);

        return created;
    }();

    return *record;
}

bool GuestSync::StallReportingRequested()
{
    return StallThresholdNanos() != 0;
}

uint64_t GuestSync::WakesFor(uint64_t object)
{
    std::lock_guard<std::mutex> guard(gWakeLock);

    auto found = gWakes.find(object);
    return found == gWakes.end() ? 0 : found->second;
}

void GuestSync::Record(Operation operation, uint64_t object, uint64_t detail)
{
    if (!TracingRequested()) return;

    fprintf(stderr, "[guestsync %d] %s %016llx (%llu)\n", (int)gettid(), OperationName(operation), (unsigned long long)object,
            (unsigned long long)detail);
}

void GuestSync::BeginWait(Operation operation, uint64_t object, uint64_t caller)
{
    if (!TracingRequested() && !StallReportingRequested()) return;

    StartWatchdog();

    WaitRecord& record = RecordForCallingThread();
    record.object.store(object, std::memory_order_relaxed);
    record.caller.store(caller, std::memory_order_relaxed);
    record.operation.store((uint8_t)operation, std::memory_order_relaxed);
    record.startedNanos.store(MonotonicNanos(), std::memory_order_relaxed);
    record.wakesAtStart.store(WakesFor(object), std::memory_order_relaxed);
    record.reported.store(false, std::memory_order_relaxed);
    record.active.store(true, std::memory_order_release);

    Record(operation, object, 0);
}

void GuestSync::EndWait(uint64_t outcome)
{
    if (!TracingRequested() && !StallReportingRequested()) return;

    WaitRecord& record = RecordForCallingThread();
    record.active.store(false, std::memory_order_release);

    if (!TracingRequested()) return;

    fprintf(stderr, "[guestsync %d] %s %016llx returned %llu after %llu ms\n", (int)gettid(),
            OperationName((Operation)record.operation.load(std::memory_order_relaxed)),
            (unsigned long long)record.object.load(std::memory_order_relaxed), (unsigned long long)outcome,
            (unsigned long long)((MonotonicNanos() - record.startedNanos.load(std::memory_order_relaxed)) / 1000000ull));
}

void GuestSync::NoteWake(Operation operation, uint64_t object, uint64_t woken)
{
    if (!TracingRequested() && !StallReportingRequested()) return;

    {
        std::lock_guard<std::mutex> guard(gWakeLock);
        gWakes[object] += 1;
    }

    Record(operation, object, woken);
}

void GuestSync::DescribeWaiters()
{
    uint64_t now = MonotonicNanos();

    std::lock_guard<std::mutex> guard(gRegistryLock);

    uint64_t waiting = 0;
    for (WaitRecord* record : gWaiters)
    {
        if (record->active.load(std::memory_order_acquire)) waiting += 1;
    }

    if (waiting == 0)
    {
        fprintf(stderr, "[guestsync] no guest thread is inside a synchronisation wait\n");
        return;
    }

    fprintf(stderr, "[guestsync] %llu guest thread(s) are inside a synchronisation wait:\n", (unsigned long long)waiting);

    for (WaitRecord* record : gWaiters)
    {
        if (!record->active.load(std::memory_order_acquire)) continue;

        uint64_t object = record->object.load(std::memory_order_relaxed);
        uint64_t started = record->startedNanos.load(std::memory_order_relaxed);

        char described[256];
        DescribeAddress(record->caller.load(std::memory_order_relaxed), described, sizeof(described));

        fprintf(stderr, "[guestsync]   thread %d in %s on %016llx for %llu ms, woken %llu time(s) since, called from %s\n",
                record->threadId.load(std::memory_order_relaxed),
                OperationName((Operation)record->operation.load(std::memory_order_relaxed)), (unsigned long long)object,
                (unsigned long long)((now - started) / 1000000ull),
                (unsigned long long)(WakesFor(object) - record->wakesAtStart.load(std::memory_order_relaxed)), described);
    }
}

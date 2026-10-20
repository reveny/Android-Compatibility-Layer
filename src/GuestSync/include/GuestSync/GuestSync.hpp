#pragma once

#include <cstdint>

namespace GuestSync
{
    inline constexpr uint64_t DEFAULT_STALL_SECONDS = 5;
    inline constexpr uint64_t WATCHDOG_INTERVAL_NANOS = 1000000000ull;

    enum class Operation : uint8_t
    {
        MutexLock,
        MutexTryLock,
        MutexUnlock,
        MutexDestroy,
        ConditionWait,
        ConditionTimedWait,
        ConditionSignal,
        ConditionBroadcast,
        ConditionDestroy,
        SemaphoreWait,
        SemaphoreTimedWait,
        SemaphorePost,
        OnceEnter,
        ThreadJoin,
        Count
    };

    const char* OperationName(Operation operation);

    bool TracingRequested();
    bool StallReportingRequested();

    using AddressDescriber = void (*)(void* context, uint64_t address, char* buffer, uint64_t bytes);

    void SetAddressDescriber(AddressDescriber describer, void* context);

    void Record(Operation operation, uint64_t object, uint64_t detail);
    void BeginWait(Operation operation, uint64_t object, uint64_t caller);
    void EndWait(uint64_t outcome);
    void NoteWake(Operation operation, uint64_t object, uint64_t woken);

    void DescribeWaiters();
    uint64_t WakesFor(uint64_t object);
}

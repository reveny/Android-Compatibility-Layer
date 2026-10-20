#include "GuestSyscall/GuestSyscall.hpp"

#include "Diagnostics/Diagnostics.hpp"

#include <atomic>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <linux/futex.h>
#include <sched.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <vector>

#include "Settings/Settings.hpp"

static constexpr uint64_t MAX_TRANSFER_BYTES = 1u << 20;

static bool TracingRequested()
{
    static const bool wanted = Settings::Tracing("syscall");

    return wanted;
}

static uint64_t ErrnoResult(long returned)
{
    if (returned >= 0) return (uint64_t)returned;
    return (uint64_t)(-(int64_t)errno);
}

static bool FutexTakesDeadline(uint32_t command)
{
    return command == FUTEX_WAIT || command == FUTEX_WAIT_BITSET || command == FUTEX_LOCK_PI || command == FUTEX_WAIT_REQUEUE_PI;
}

static GuestSyscall::Result FutexSyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t address = registers.general[0];
    uint32_t operation = (uint32_t)registers.general[1];
    uint32_t expected = (uint32_t)registers.general[2];
    uint64_t fourth = registers.general[3];
    uint64_t secondAddress = registers.general[4];
    uint32_t third = (uint32_t)registers.general[5];

    uint32_t command = operation & FUTEX_CMD_MASK;

    struct timespec span;
    const void* deadline = nullptr;

    if (FutexTakesDeadline(command))
    {
        if (fourth != 0)
        {
            int64_t seconds = 0;
            int64_t nanoseconds = 0;

            if (!memory.Read(memory.context, (GuestState::GuestAddress)fourth, &seconds, sizeof(seconds)) ||
                !memory.Read(memory.context, (GuestState::GuestAddress)(fourth + 8), &nanoseconds, sizeof(nanoseconds)))
            {
                return GuestSyscall::Result::GuestMemoryFault;
            }

            span.tv_sec = (time_t)seconds;
            span.tv_nsec = (long)nanoseconds;
            deadline = &span;
        }
    }
    else
    {
        deadline = (const void*)(uintptr_t)fourth;
    }

    long returned = syscall(SYS_futex, (void*)(uintptr_t)address, (int)operation, expected, deadline,
                            (void*)(uintptr_t)secondAddress, third);

    registers.general[0] = ErrnoResult(returned);
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result GetRandomSyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t address = registers.general[0];
    uint64_t length = registers.general[1];

    if (length > MAX_TRANSFER_BYTES) length = MAX_TRANSFER_BYTES;

    std::vector<uint8_t> buffer((size_t)length);
    long produced = syscall(SYS_getrandom, buffer.data(), (size_t)length, 0);

    if (produced < 0)
    {
        registers.general[0] = ErrnoResult(produced);
        return GuestSyscall::Result::Ok;
    }

    if (produced != 0 && !memory.Write(memory.context, (GuestState::GuestAddress)address, buffer.data(), (uint64_t)produced))
        return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = (uint64_t)produced;
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result WriteSyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    int descriptor = (int)registers.general[0];
    uint64_t address = registers.general[1];
    uint64_t length = registers.general[2];

    if (length > MAX_TRANSFER_BYTES) length = MAX_TRANSFER_BYTES;

    std::vector<uint8_t> buffer((size_t)length);
    if (length != 0 && !memory.Read(memory.context, (GuestState::GuestAddress)address, buffer.data(), length))
        return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = ErrnoResult(write(descriptor, buffer.data(), (size_t)length));
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result ReadSyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    int descriptor = (int)registers.general[0];
    uint64_t address = registers.general[1];
    uint64_t length = registers.general[2];

    if (length > MAX_TRANSFER_BYTES) length = MAX_TRANSFER_BYTES;

    std::vector<uint8_t> buffer((size_t)length);
    long returned = read(descriptor, buffer.data(), (size_t)length);

    if (returned > 0 && !memory.Write(memory.context, (GuestState::GuestAddress)address, buffer.data(), (uint64_t)returned))
        return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = ErrnoResult(returned);
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result ClockGetTimeSyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    clockid_t clock = (clockid_t)registers.general[0];
    uint64_t address = registers.general[1];

    struct timespec value = {};
    if (clock_gettime(clock, &value) != 0)
    {
        registers.general[0] = (uint64_t)(-(int64_t)errno);
        return GuestSyscall::Result::Ok;
    }

    uint64_t packed[2] = {(uint64_t)value.tv_sec, (uint64_t)value.tv_nsec};
    if (!memory.Write(memory.context, (GuestState::GuestAddress)address, packed, sizeof(packed))) return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = 0;
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result GetTimeOfDaySyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t address = registers.general[0];

    struct timeval value = {};
    if (gettimeofday(&value, nullptr) != 0)
    {
        registers.general[0] = (uint64_t)(-(int64_t)errno);
        return GuestSyscall::Result::Ok;
    }

    if (address != 0)
    {
        uint64_t packed[2] = {(uint64_t)value.tv_sec, (uint64_t)value.tv_usec};
        if (!memory.Write(memory.context, (GuestState::GuestAddress)address, packed, sizeof(packed)))
            return GuestSyscall::Result::GuestMemoryFault;
    }

    registers.general[0] = 0;
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result AffinitySyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t requested = registers.general[1];
    uint64_t destination = registers.general[2];

    cpu_set_t allowed;
    CPU_ZERO(&allowed);

    if (sched_getaffinity(0, sizeof(allowed), &allowed) != 0)
    {
        registers.general[0] = (uint64_t)(-(int64_t)errno);
        return GuestSyscall::Result::Ok;
    }

    uint64_t copied = requested < sizeof(allowed) ? requested : sizeof(allowed);
    if (copied == 0 || destination == 0)
    {
        registers.general[0] = (uint64_t)(-(int64_t)EINVAL);
        return GuestSyscall::Result::Ok;
    }

    if (!memory.Write(memory.context, (GuestState::GuestAddress)destination, &allowed, copied))
        return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = copied;
    return GuestSyscall::Result::Ok;
}

static GuestSyscall::Result SetAffinitySyscall(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    uint64_t requested = registers.general[1];
    uint64_t source = registers.general[2];

    cpu_set_t wanted;
    CPU_ZERO(&wanted);

    uint64_t copied = requested < sizeof(wanted) ? requested : sizeof(wanted);
    if (copied == 0 || source == 0)
    {
        registers.general[0] = (uint64_t)(-(int64_t)EINVAL);
        return GuestSyscall::Result::Ok;
    }

    if (!memory.Read(memory.context, (GuestState::GuestAddress)source, &wanted, copied)) return GuestSyscall::Result::GuestMemoryFault;

    registers.general[0] = ErrnoResult(sched_setaffinity(0, sizeof(wanted), &wanted));
    return GuestSyscall::Result::Ok;
}

GuestSyscall::Result GuestSyscall::Dispatch(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory, Statistics& statistics)
{
    if (!memory.Read || !memory.Write) return Result::GuestMemoryFault;

    std::atomic_ref<uint64_t>(statistics.dispatched).fetch_add(1, std::memory_order_relaxed);
    uint32_t number = (uint32_t)registers.general[8];

    if (TracingRequested())
    {
        fprintf(stderr, "[syscall %d] %u(%016llx, %016llx, %016llx, %016llx) from %016llx\n", (int)gettid(), number,
                (unsigned long long)registers.general[0], (unsigned long long)registers.general[1],
                (unsigned long long)registers.general[2], (unsigned long long)registers.general[3],
                (unsigned long long)registers.general[30]);
    }

    switch (number)
    {
        case SYSCALL_GET_THREAD_ID: registers.general[0] = (uint64_t)gettid(); return Result::Ok;
        case SYSCALL_GET_PROCESS_ID: registers.general[0] = (uint64_t)getpid(); return Result::Ok;
        case SYSCALL_GET_USER_ID: registers.general[0] = (uint64_t)getuid(); return Result::Ok;
        case SYSCALL_YIELD: registers.general[0] = ErrnoResult(sched_yield()); return Result::Ok;
        case SYSCALL_EXIT_GROUP:
            fprintf(stderr, "[guestsyscall] the guest called exit_group(%d)\n", (int)registers.general[0]);
            return Result::ProcessExit;
        case SYSCALL_ADVISE_MEMORY: registers.general[0] = 0; return Result::Ok;
        case SYSCALL_WRITE: return WriteSyscall(registers, memory);
        case SYSCALL_READ: return ReadSyscall(registers, memory);
        case SYSCALL_CLOCK_GET_TIME: return ClockGetTimeSyscall(registers, memory);
        case SYSCALL_GET_TIME_OF_DAY: return GetTimeOfDaySyscall(registers, memory);
        case SYSCALL_FUTEX: return FutexSyscall(registers, memory);
        case SYSCALL_GET_RANDOM: return GetRandomSyscall(registers, memory);
        case SYSCALL_PROTECT_MEMORY: registers.general[0] = 0; return Result::Ok;
        case SYSCALL_SCHED_GET_AFFINITY: return AffinitySyscall(registers, memory);
        case SYSCALL_SCHED_SET_AFFINITY: return SetAffinitySyscall(registers, memory);
        default: break;
    }

    std::atomic_ref<uint64_t>(statistics.unimplemented).fetch_add(1, std::memory_order_relaxed);

    static std::atomic<uint64_t> announced[4] = {};
    bool seen = false;
    for (std::atomic<uint64_t>& slot : announced)
    {
        uint64_t held = slot.load(std::memory_order_relaxed);
        if (held == (uint64_t)number + 1)
        {
            seen = true;
            break;
        }

        if (held != 0) continue;

        uint64_t empty = 0;
        if (slot.compare_exchange_strong(empty, (uint64_t)number + 1, std::memory_order_relaxed)) break;
    }

    char described[48];
    snprintf(described, sizeof(described), "syscall %u", number);
    Diagnostics::NoteGap(Diagnostics::Gap::Syscall, described);

    if (!seen)
    {
        fprintf(stderr, "[guestsyscall] the guest made syscall %u, which is not implemented; it was told ENOSYS\n", number);
        fflush(stderr);
    }

    registers.general[0] = (uint64_t)(-(int64_t)ENOSYS);
    return Result::Unimplemented;
}

const char* GuestSyscall::SyscallName(uint32_t number)
{
    switch (number)
    {
        case SYSCALL_GET_THREAD_ID: return "gettid";
        case SYSCALL_GET_PROCESS_ID: return "getpid";
        case SYSCALL_GET_USER_ID: return "getuid";
        case SYSCALL_WRITE: return "write";
        case SYSCALL_READ: return "read";
        case SYSCALL_CLOCK_GET_TIME: return "clock_gettime";
        case SYSCALL_GET_TIME_OF_DAY: return "gettimeofday";
        case SYSCALL_EXIT_GROUP: return "exit_group";
        case SYSCALL_ADVISE_MEMORY: return "madvise";
        case SYSCALL_YIELD: return "sched_yield";
        default: break;
    }

    return "unknown";
}

const char* GuestSyscall::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::ProcessExit: return "ProcessExit";
        case Result::Unimplemented: return "Unimplemented";
        case Result::GuestMemoryFault: return "GuestMemoryFault";
    }

    return "Unknown";
}

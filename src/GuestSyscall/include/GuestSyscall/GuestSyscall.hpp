#pragma once

#include <cstdint>

#include "GuestState/GuestState.hpp"

namespace GuestSyscall
{
    inline constexpr uint32_t SYSCALL_GET_THREAD_ID = 178;
    inline constexpr uint32_t SYSCALL_GET_PROCESS_ID = 172;
    inline constexpr uint32_t SYSCALL_GET_USER_ID = 174;
    inline constexpr uint32_t SYSCALL_WRITE = 64;
    inline constexpr uint32_t SYSCALL_READ = 63;
    inline constexpr uint32_t SYSCALL_CLOCK_GET_TIME = 113;
    inline constexpr uint32_t SYSCALL_GET_TIME_OF_DAY = 169;
    inline constexpr uint32_t SYSCALL_EXIT_GROUP = 94;
    inline constexpr uint32_t SYSCALL_ADVISE_MEMORY = 233;
    inline constexpr uint32_t SYSCALL_YIELD = 124;
    inline constexpr uint32_t SYSCALL_FUTEX = 98;
    inline constexpr uint32_t SYSCALL_GET_RANDOM = 278;
    inline constexpr uint32_t SYSCALL_SCHED_GET_AFFINITY = 123;
    inline constexpr uint32_t SYSCALL_SCHED_SET_AFFINITY = 122;
    inline constexpr uint32_t SYSCALL_PROTECT_MEMORY = 226;
    inline constexpr uint32_t SYSCALL_RESERVED_TIMER = 115;
    inline constexpr uint32_t SYSCALL_TRUNCATE_DESCRIPTOR = 46;
    inline constexpr uint32_t SYSCALL_ALLOCATE_FILE_SPACE = 47;
    inline constexpr uint32_t SYSCALL_SYNCHRONIZE_DESCRIPTOR = 82;
    inline constexpr uint32_t SYSCALL_SYNCHRONIZE_DESCRIPTOR_DATA = 83;
    inline constexpr uint32_t SYSCALL_GET_CPU = 168;
    inline constexpr uint32_t SYSCALL_GET_PARENT_PROCESS_ID = 173;
    inline constexpr uint32_t SYSCALL_GET_EFFECTIVE_USER_ID = 175;
    inline constexpr uint32_t SYSCALL_GET_GROUP_ID = 176;
    inline constexpr uint32_t SYSCALL_GET_EFFECTIVE_GROUP_ID = 177;
    inline constexpr uint32_t SYSCALL_READ_OTHER_PROCESS = 270;
    inline constexpr uint32_t SYSCALL_WRITE_OTHER_PROCESS = 271;

    enum class Result : uint8_t
    {
        Ok,
        ProcessExit,
        Unimplemented,
        GuestMemoryFault
    };

    struct Statistics
    {
        uint64_t dispatched = 0;
        uint64_t unimplemented = 0;
    };

    Result Dispatch(GuestState::Registers& registers, const GuestState::MemoryAccessor& memory, Statistics& statistics);

    const char* SyscallName(uint32_t number);
    const char* ResultName(Result result);
}

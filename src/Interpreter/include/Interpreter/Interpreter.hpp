#pragma once

#include <cstdint>

#include "GuestState/GuestState.hpp"
#include "Ir/Ir.hpp"

namespace Interpreter
{
    enum class ExitReason : uint8_t
    {
        Returned,
        IndirectBranch,
        HostCall,
        Syscall,
        BlockExhausted,
        Undefined,
        MemoryFault,
        StepLimitReached,
        InstructionCacheInvalidated
    };

    struct ExitStatus
    {
        ExitReason reason = ExitReason::Returned;
        GuestState::GuestAddress targetAddress{};
        GuestState::GuestAddress faultAddress{};
        uint64_t stepsExecuted = 0;
        bool hasTargetAddress = false;
        bool hasFaultAddress = false;
    };

    inline constexpr uint64_t UNLIMITED_STEPS = 0;

    struct Configuration
    {
        uint64_t stepLimit = UNLIMITED_STEPS;
        bool traceInstructions = false;
    };

    ExitStatus RunFunction(const Ir::Function& function, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory,
                           const Configuration& configuration);

    ExitStatus RunBlock(const Ir::Block& block, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory,
                        const Configuration& configuration);

    const char* ExitReasonName(ExitReason reason);
}

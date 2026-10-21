#include "Interpreter/Interpreter.hpp"

#include "Interpreter/IrSemantics.hpp"

#include <cstdio>
#include <cstring>
#include <mutex>
#include <vector>

using IrSemantics::BitsInType;
using IrSemantics::BitsToDouble;
using IrSemantics::BitsToFloat;
using IrSemantics::ClassifyDoubleResult;
using IrSemantics::ComputeAddFlags;
using IrSemantics::DEFAULT_DOUBLE_NAN;
using IrSemantics::DEFAULT_FLOAT_NAN;
using IrSemantics::DoubleToBits;
using IrSemantics::FloatToBits;
using IrSemantics::IsSignallingDouble;
using IrSemantics::IsSignallingFloat;
using IrSemantics::MaskLane;
using IrSemantics::MaskToType;
using IrSemantics::ReadLane;
using IrSemantics::ReverseBitsIn;
using IrSemantics::ReverseBytesInLanes;
using IrSemantics::SignExtendTo64;
using IrSemantics::WriteLane;


Interpreter::ExitStatus Interpreter::RunBlock(const Ir::Block& block, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory,
                                              const Configuration& configuration)
{
    ExitStatus status;

    std::vector<GuestState::VectorRegister> values;

    for (const Ir::Instruction& instruction : block.instructions)
    {
        if (configuration.stepLimit != UNLIMITED_STEPS && status.stepsExecuted >= configuration.stepLimit)
        {
            status.reason = ExitReason::StepLimitReached;
            return status;
        }
        status.stepsExecuted += 1;

        if (configuration.traceInstructions)
        {
            fprintf(stderr, "[t0] %016llx %s\n", (unsigned long long)instruction.guestAddress, Ir::OpcodeName(instruction.opcode));
        }

        uint32_t resultId = instruction.result.id;
        if (resultId != Ir::INVALID_VALUE_ID && resultId >= values.size()) values.resize(resultId + 1);

        GuestState::VectorRegister leftWide;
        GuestState::VectorRegister rightWide;
        GuestState::VectorRegister thirdWide;
        if (instruction.operandCount > 0 && instruction.operands[0].id < values.size()) leftWide = values[instruction.operands[0].id];
        if (instruction.operandCount > 1 && instruction.operands[1].id < values.size()) rightWide = values[instruction.operands[1].id];
        if (instruction.operandCount > 2 && instruction.operands[2].id < values.size()) thirdWide = values[instruction.operands[2].id];

        int bits = BitsInType(instruction.operandType);
        uint64_t left = leftWide.low;
        uint64_t right = rightWide.low;
        uint64_t third = instruction.operandCount > 2 && instruction.operands[2].id < values.size() ? values[instruction.operands[2].id].low : 0;
        uint64_t produced = 0;
        GuestState::VectorRegister producedWide;
        bool producedIsWide = false;

        switch (instruction.opcode)
        {
            case Ir::Opcode::Constant: produced = instruction.immediate; break;

            case Ir::Opcode::ReadGeneral: produced = registers.general[instruction.immediate]; break;
            case Ir::Opcode::WriteGeneral: registers.general[instruction.immediate] = MaskToType(left, instruction.operandType); break;
            case Ir::Opcode::ReadStackPointer: produced = registers.stackPointer; break;
            case Ir::Opcode::WriteStackPointer: registers.stackPointer = left; break;
            case Ir::Opcode::ReadFlags: produced = registers.flagsNzcv; break;
            case Ir::Opcode::WriteFlags: registers.flagsNzcv = (uint32_t)left & 0xf0000000u; break;

            case Ir::Opcode::Add: produced = left + right; break;
            case Ir::Opcode::Subtract: produced = left - right; break;
            case Ir::Opcode::Multiply: produced = left * right; break;
            case Ir::Opcode::And: produced = left & right; break;
            case Ir::Opcode::Or: produced = left | right; break;
            case Ir::Opcode::Xor: produced = left ^ right; break;
            case Ir::Opcode::Not: produced = ~left; break;

            case Ir::Opcode::AddWithCarry: produced = left + right + ((third & GuestState::FLAG_CARRY) ? 1 : 0); break;
            case Ir::Opcode::SubtractWithBorrow: produced = left + ~right + ((third & GuestState::FLAG_CARRY) ? 1 : 0); break;

            case Ir::Opcode::MultiplyHighUnsigned: produced = (uint64_t)(((__uint128_t)left * (__uint128_t)right) >> 64); break;
            case Ir::Opcode::MultiplyHighSigned:
                produced = (uint64_t)(((__int128_t)(int64_t)left * (__int128_t)(int64_t)right) >> 64);
                break;

            case Ir::Opcode::DivideUnsigned:
            {
                uint64_t divisor = MaskToType(right, instruction.operandType);
                uint64_t dividend = MaskToType(left, instruction.operandType);
                produced = divisor == 0 ? 0 : dividend / divisor;
                break;
            }
            case Ir::Opcode::DivideSigned:
            {
                int64_t dividend = (int64_t)SignExtendTo64(left, bits);
                int64_t divisor = (int64_t)SignExtendTo64(right, bits);
                if (divisor == 0) produced = 0;
                else if (divisor == -1) produced = (uint64_t)(0 - (uint64_t)dividend);
                else produced = (uint64_t)(dividend / divisor);
                break;
            }

            case Ir::Opcode::ShiftLeft: produced = (right % bits) == 0 ? left : (left << (right % bits)); break;
            case Ir::Opcode::ShiftRightLogical: produced = MaskToType(left, instruction.operandType) >> (right % bits); break;
            case Ir::Opcode::ShiftRightArithmetic:
                produced = (uint64_t)((int64_t)SignExtendTo64(left, bits) >> (right % bits));
                break;
            case Ir::Opcode::RotateRight:
            {
                uint64_t amount = right % bits;
                uint64_t masked = MaskToType(left, instruction.operandType);
                produced = amount == 0 ? masked : ((masked >> amount) | (masked << (bits - amount)));
                break;
            }

            case Ir::Opcode::CountLeadingZeros:
            {
                uint64_t masked = MaskToType(left, instruction.operandType);
                if (masked == 0) produced = (uint64_t)bits;
                else produced = (uint64_t)(__builtin_clzll(masked) - (64 - bits));
                break;
            }
            case Ir::Opcode::ReverseBits: produced = ReverseBitsIn(left, bits); break;
            case Ir::Opcode::ReverseBytes: produced = ReverseBytesInLanes(left, instruction.laneWidth, bits); break;

            case Ir::Opcode::ComputeNzcvAdd: produced = ComputeAddFlags(left, right, third & 1, bits); break;
            case Ir::Opcode::ComputeNzcvSubtract: produced = ComputeAddFlags(left, ~right, third & 1, bits); break;
            case Ir::Opcode::ComputeNzcvLogical:
            {
                uint64_t masked = MaskToType(left, instruction.operandType);
                uint32_t flags = 0;
                if ((masked >> (bits - 1)) & 1) flags |= GuestState::FLAG_NEGATIVE;
                if (masked == 0) flags |= GuestState::FLAG_ZERO;
                produced = flags;
                break;
            }

            case Ir::Opcode::Compare:
            {
                uint64_t a = MaskToType(left, instruction.operandType);
                uint64_t b = MaskToType(right, instruction.operandType);
                bool outcome = false;
                switch (instruction.predicate)
                {
                    case Ir::ComparePredicate::Equal: outcome = a == b; break;
                    case Ir::ComparePredicate::NotEqual: outcome = a != b; break;
                    case Ir::ComparePredicate::LessUnsigned: outcome = a < b; break;
                    case Ir::ComparePredicate::LessEqualUnsigned: outcome = a <= b; break;
                    case Ir::ComparePredicate::GreaterUnsigned: outcome = a > b; break;
                    case Ir::ComparePredicate::GreaterEqualUnsigned: outcome = a >= b; break;
                    case Ir::ComparePredicate::LessSigned: outcome = (int64_t)SignExtendTo64(a, bits) < (int64_t)SignExtendTo64(b, bits); break;
                    case Ir::ComparePredicate::LessEqualSigned: outcome = (int64_t)SignExtendTo64(a, bits) <= (int64_t)SignExtendTo64(b, bits); break;
                    case Ir::ComparePredicate::GreaterSigned: outcome = (int64_t)SignExtendTo64(a, bits) > (int64_t)SignExtendTo64(b, bits); break;
                    case Ir::ComparePredicate::GreaterEqualSigned:
                        outcome = (int64_t)SignExtendTo64(a, bits) >= (int64_t)SignExtendTo64(b, bits);
                        break;
                }
                produced = outcome ? 1 : 0;
                break;
            }

            case Ir::Opcode::Select:
                produced = GuestState::ConditionHolds(registers.flagsNzcv, (uint8_t)instruction.condition) ? left : right;
                break;

            case Ir::Opcode::ExtendSigned: produced = SignExtendTo64(left, (int)instruction.immediate); break;
            case Ir::Opcode::ExtendUnsigned: produced = left & ((instruction.immediate >= 64) ? ~0ull : ((1ull << instruction.immediate) - 1)); break;
            case Ir::Opcode::Truncate: produced = MaskToType(left, instruction.operandType); break;

            case Ir::Opcode::ClearExclusive: registers.exclusive = GuestState::ExclusiveMonitor(); break;
            case Ir::Opcode::MemoryBarrier: break;
            case Ir::Opcode::Hint: break;
            case Ir::Opcode::PointerAuthSign:
            case Ir::Opcode::PointerAuthAuthenticate:
            case Ir::Opcode::PointerAuthStrip: produced = left; break;

            case Ir::Opcode::Load:
            {
                int width = Ir::SizeInBytes(instruction.operandType);
                GuestState::VectorRegister loaded;
                if (!memory.Read || !memory.Read(memory.context, static_cast<GuestState::GuestAddress>(left), &loaded, (uint64_t)width))
                {
                    status.reason = ExitReason::MemoryFault;
                    status.faultAddress = static_cast<GuestState::GuestAddress>(left);
                    status.hasFaultAddress = true;
                    return status;
                }

                if (width > 8)
                {
                    producedWide = loaded;
                    producedIsWide = true;
                }
                else produced = loaded.low;
                break;
            }
            case Ir::Opcode::Store:
            {
                int width = Ir::SizeInBytes(instruction.operandType);
                GuestState::VectorRegister payload = rightWide;
                if (width <= 8) payload.high = 0;

                if (!memory.Write || !memory.Write(memory.context, static_cast<GuestState::GuestAddress>(left), &payload, (uint64_t)width))
                {
                    status.reason = ExitReason::MemoryFault;
                    status.faultAddress = static_cast<GuestState::GuestAddress>(left);
                    status.hasFaultAddress = true;
                    return status;
                }

                break;
            }

            case Ir::Opcode::ReadVector:
                producedWide = registers.vector[instruction.immediate];
                if (instruction.operandType != Ir::ValueType::Vector128 && instruction.operandType != Ir::ValueType::Int128)
                {
                    producedWide.high = 0;
                    producedWide.low = MaskToType(producedWide.low, instruction.operandType);
                }
                producedIsWide = true;
                break;

            case Ir::Opcode::WriteVector:
                registers.vector[instruction.immediate].low = leftWide.low;
                registers.vector[instruction.immediate].high =
                    (instruction.operandType == Ir::ValueType::Vector128 || instruction.operandType == Ir::ValueType::Int128) ? leftWide.high : 0;
                break;

            case Ir::Opcode::VectorShiftLeftSigned:
            case Ir::Opcode::VectorShiftLeftUnsigned:
            case Ir::Opcode::VectorAdd:
            case Ir::Opcode::VectorSubtract:
            case Ir::Opcode::VectorMultiply:
            case Ir::Opcode::VectorAnd:
            case Ir::Opcode::VectorOr:
            case Ir::Opcode::VectorXor:
            case Ir::Opcode::VectorDuplicate:
            case Ir::Opcode::VectorExtractLane:
            case Ir::Opcode::VectorExtract:
            case Ir::Opcode::VectorInsertLane:
            case Ir::Opcode::VectorReduceAdd:
            case Ir::Opcode::VectorReduceAddLong:
            case Ir::Opcode::VectorFloatCompare:
            case Ir::Opcode::VectorFloatMinimum:
            case Ir::Opcode::VectorFloatMaximum:
            case Ir::Opcode::VectorFloatPairwiseAdd:
            case Ir::Opcode::VectorFloatEstimate:
            case Ir::Opcode::VectorNarrow:
            case Ir::Opcode::VectorPermute:
            case Ir::Opcode::VectorMinMax:
            case Ir::Opcode::VectorPairwise:
            case Ir::Opcode::VectorAbsoluteDifference:
            case Ir::Opcode::VectorAbsolute:
            case Ir::Opcode::VectorCountLeadingZeros:
            case Ir::Opcode::VectorPairwiseAddLong:
            case Ir::Opcode::VectorFloatConvertWidth:
            case Ir::Opcode::VectorTableLookup:
            case Ir::Opcode::VectorReduceMinMax:
            case Ir::Opcode::VectorCompare:
            case Ir::Opcode::VectorReverseBytes:
            case Ir::Opcode::VectorFloatAdd:
            case Ir::Opcode::VectorFloatSubtract:
            case Ir::Opcode::VectorFloatMultiply:
            case Ir::Opcode::VectorFloatMultiplyAdd:
            case Ir::Opcode::VectorFloatRound:
            case Ir::Opcode::VectorFloatDivide:
            case Ir::Opcode::VectorFloatSquareRoot:
            case Ir::Opcode::VectorFloatNegate:
            case Ir::Opcode::VectorFloatAbsolute:
            case Ir::Opcode::VectorConvertFloat:
            case Ir::Opcode::VectorCountBits:
            case Ir::Opcode::VectorWiden:
            {
                IrSemantics::VectorRequest request;
                request.opcode = instruction.opcode;
                request.laneWidth = instruction.laneWidth;
                request.laneCount = instruction.laneCount;
                request.laneIndex = instruction.laneIndex;
                request.immediate = instruction.immediate;

                bool wide = false;
                if (!IrSemantics::ApplyVectorOperation(request, leftWide, rightWide, thirdWide, producedWide, wide,
                                                       registers.floatingPointStatus))
                {
                    status.reason = ExitReason::Undefined;
                    status.faultAddress = instruction.guestAddress;
                    status.hasFaultAddress = true;
                    return status;
                }

                if (wide) producedIsWide = true;
                else produced = producedWide.low;

                break;
            }

            case Ir::Opcode::FloatAdd:
            case Ir::Opcode::FloatSubtract:
            case Ir::Opcode::FloatMultiply:
            case Ir::Opcode::FloatDivide:
            case Ir::Opcode::FloatCompare:
            case Ir::Opcode::FloatCompareSignalling:
            case Ir::Opcode::FloatConvert:
            case Ir::Opcode::FloatSquareRoot:
            case Ir::Opcode::FloatMultiplyAdd:
            case Ir::Opcode::FloatMinimum:
            case Ir::Opcode::FloatMaximum:
            case Ir::Opcode::FloatRound:
            case Ir::Opcode::FloatConvertRounded:
            {
                IrSemantics::FloatRequest request;
                request.opcode = instruction.opcode;
                request.operandType = instruction.operandType;
                request.resultType = instruction.result.type;
                request.sourceType = instruction.operands[0].type;
                request.immediate = instruction.immediate;

                if (!IrSemantics::ApplyFloatOperation(request, left, right, third, produced, registers.floatingPointStatus))
                {
                    status.reason = ExitReason::Undefined;
                    status.faultAddress = instruction.guestAddress;
                    status.hasFaultAddress = true;
                    return status;
                }

                break;
            }

            case Ir::Opcode::ReadSystemRegister:
                if (Ir::DescribeSystemRegister(instruction.systemRegister, produced)) break;
                if (instruction.systemRegister == Ir::SystemRegister::ThreadPointer) produced = registers.threadPointer;
                else if (instruction.systemRegister == Ir::SystemRegister::ThreadPointerReadOnly) produced = registers.threadPointer;
                else if (instruction.systemRegister == Ir::SystemRegister::FloatingPointControl) produced = registers.floatingPointControl;
                else if (instruction.systemRegister == Ir::SystemRegister::CounterVirtual) produced = Ir::ReadVirtualCounter();
                else produced = registers.floatingPointStatus;
                break;

            case Ir::Opcode::WriteSystemRegister:
                if (instruction.systemRegister == Ir::SystemRegister::ThreadPointer) registers.threadPointer = left;
                else if (instruction.systemRegister == Ir::SystemRegister::FloatingPointControl) registers.floatingPointControl = (uint32_t)left;
                else if (instruction.systemRegister == Ir::SystemRegister::FloatingPointStatus) registers.floatingPointStatus = (uint32_t)left;
                break;

            case Ir::Opcode::ReadExclusiveHigh:
                produced = registers.exclusive.valueHigh;
                break;

            case Ir::Opcode::SetExclusiveExpected:
                registers.exclusive.valueLow = left;
                registers.exclusive.valueHigh = right;
                break;

            case Ir::Opcode::LoadExclusive:
            case Ir::Opcode::StoreExclusive:
            case Ir::Opcode::LoadExclusivePair:
            case Ir::Opcode::StoreExclusivePair:
            case Ir::Opcode::AtomicCompareExchangePair:
            case Ir::Opcode::AtomicCompareExchange:
            case Ir::Opcode::AtomicReadModifyWrite:
            {
                IrSemantics::AtomicRequest request;
                request.opcode = instruction.opcode;
                request.operandType = instruction.operandType;
                request.atomicOperation = instruction.atomicOperation;

                IrSemantics::AtomicOutcome outcome =
                    IrSemantics::ApplyAtomicOperation(request, memory, registers.exclusive, left, right, third, produced);

                if (outcome == IrSemantics::AtomicOutcome::MemoryFault)
                {
                    status.reason = ExitReason::MemoryFault;
                    status.faultAddress = static_cast<GuestState::GuestAddress>(left);
                    status.hasFaultAddress = true;
                    return status;
                }

                if (outcome == IrSemantics::AtomicOutcome::Undefined)
                {
                    status.reason = ExitReason::Undefined;
                    status.faultAddress = instruction.guestAddress;
                    status.hasFaultAddress = true;
                    return status;
                }

                break;
            }

            case Ir::Opcode::Branch:
            case Ir::Opcode::Call:
                status.reason = ExitReason::IndirectBranch;
                status.targetAddress = static_cast<GuestState::GuestAddress>(instruction.immediate);
                status.hasTargetAddress = true;
                return status;

            case Ir::Opcode::BranchConditional:
            {
                bool taken = instruction.operandCount > 0 ? (left != 0)
                                                          : GuestState::ConditionHolds(registers.flagsNzcv, (uint8_t)instruction.condition);
                status.reason = ExitReason::IndirectBranch;
                status.targetAddress = static_cast<GuestState::GuestAddress>(
                    taken ? instruction.immediate : (uint64_t)block.successorFallthrough);
                status.hasTargetAddress = true;
                return status;
            }

            case Ir::Opcode::BranchIndirect:
            case Ir::Opcode::Return:
                status.reason = instruction.opcode == Ir::Opcode::Return ? ExitReason::Returned : ExitReason::IndirectBranch;
                status.targetAddress = static_cast<GuestState::GuestAddress>(left);
                status.hasTargetAddress = true;
                return status;

            case Ir::Opcode::HostCall:
                status.reason = ExitReason::HostCall;
                status.targetAddress = static_cast<GuestState::GuestAddress>(instruction.immediate);
                status.hasTargetAddress = true;
                return status;

            case Ir::Opcode::Syscall:
                status.reason = ExitReason::Syscall;
                status.targetAddress = static_cast<GuestState::GuestAddress>(instruction.guestAddress);
                status.hasTargetAddress = true;
                return status;

            case Ir::Opcode::InvalidateInstructionCache:
                status.reason = ExitReason::InstructionCacheInvalidated;
                status.targetAddress = static_cast<GuestState::GuestAddress>(instruction.guestAddress);
                status.hasTargetAddress = true;
                status.faultAddress = static_cast<GuestState::GuestAddress>(left);
                status.hasFaultAddress = true;
                return status;

            case Ir::Opcode::Undefined:
                status.reason = ExitReason::Undefined;
                status.faultAddress = instruction.guestAddress;
                status.hasFaultAddress = true;
                return status;

            default:
                status.reason = ExitReason::Undefined;
                status.faultAddress = instruction.guestAddress;
                status.hasFaultAddress = true;
                return status;
        }

        if (resultId != Ir::INVALID_VALUE_ID)
        {
            if (producedIsWide) values[resultId] = producedWide;
            else
            {
                values[resultId].low = MaskToType(produced, instruction.result.type);
                values[resultId].high = 0;
            }
        }
    }

    status.reason = ExitReason::BlockExhausted;
    return status;
}

Interpreter::ExitStatus Interpreter::RunFunction(const Ir::Function& function, GuestState::Registers& registers,
                                                 const GuestState::MemoryAccessor& memory, const Configuration& configuration)
{
    ExitStatus status;

    if (function.blocks.empty())
    {
        status.reason = ExitReason::Undefined;
        return status;
    }

    uint64_t cursor = static_cast<uint64_t>(function.entryAddress);
    Configuration remaining = configuration;

    for (;;)
    {
        const Ir::Block* selected = nullptr;
        for (const Ir::Block& block : function.blocks)
        {
            if (static_cast<uint64_t>(block.guestAddress) == cursor) selected = &block;
        }

        if (!selected)
        {
            status.reason = ExitReason::IndirectBranch;
            status.targetAddress = static_cast<GuestState::GuestAddress>(cursor);
            status.hasTargetAddress = true;
            return status;
        }

        ExitStatus blockStatus = RunBlock(*selected, registers, memory, remaining);
        status.stepsExecuted += blockStatus.stepsExecuted;

        if (remaining.stepLimit != UNLIMITED_STEPS)
        {
            if (blockStatus.stepsExecuted > remaining.stepLimit)
            {
                status.reason = ExitReason::StepLimitReached;
                return status;
            }

            remaining.stepLimit -= blockStatus.stepsExecuted;
            if (remaining.stepLimit == 0 && blockStatus.reason == ExitReason::IndirectBranch)
            {
                status.reason = ExitReason::StepLimitReached;
                return status;
            }
        }

        if (blockStatus.reason != ExitReason::IndirectBranch)
        {
            status.reason = blockStatus.reason;
            status.targetAddress = blockStatus.targetAddress;
            status.faultAddress = blockStatus.faultAddress;
            status.hasTargetAddress = blockStatus.hasTargetAddress;
            status.hasFaultAddress = blockStatus.hasFaultAddress;
            return status;
        }

        uint64_t target = static_cast<uint64_t>(blockStatus.targetAddress);
        if (target < static_cast<uint64_t>(function.entryAddress) || target >= static_cast<uint64_t>(function.endAddress))
        {
            status.reason = ExitReason::IndirectBranch;
            status.targetAddress = blockStatus.targetAddress;
            status.hasTargetAddress = true;
            return status;
        }

        cursor = target;
    }
}

const char* Interpreter::ExitReasonName(ExitReason reason)
{
    switch (reason)
    {
        case ExitReason::Returned: return "Returned";
        case ExitReason::IndirectBranch: return "IndirectBranch";
        case ExitReason::HostCall: return "HostCall";
        case ExitReason::Syscall: return "Syscall";
        case ExitReason::BlockExhausted: return "BlockExhausted";
        case ExitReason::Undefined: return "Undefined";
        case ExitReason::MemoryFault: return "MemoryFault";
        case ExitReason::StepLimitReached: return "StepLimitReached";
        case ExitReason::InstructionCacheInvalidated: return "InstructionCacheInvalidated";
    }
    return "Unknown";
}

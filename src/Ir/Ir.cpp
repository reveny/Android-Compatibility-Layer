#include "Ir/Ir.hpp"

Ir::Value Ir::MakeValue(uint32_t id, ValueType type)
{
    Value value;
    value.id = id;
    value.type = type;
    return value;
}

bool Ir::IsTerminator(Opcode opcode)
{
    switch (opcode)
    {
        case Opcode::Branch:
        case Opcode::BranchConditional:
        case Opcode::BranchIndirect:
        case Opcode::Call:
        case Opcode::Return:
        case Opcode::HostCall:
        case Opcode::Syscall:
        case Opcode::Undefined: return true;
        default: return false;
    }
}

bool Ir::IsMemoryAccess(Opcode opcode)
{
    switch (opcode)
    {
        case Opcode::Load:
        case Opcode::Store:
        case Opcode::LoadExclusive:
        case Opcode::StoreExclusive:
        case Opcode::LoadExclusivePair:
        case Opcode::StoreExclusivePair:
        case Opcode::AtomicCompareExchangePair:
        case Opcode::AtomicCompareExchange:
        case Opcode::AtomicReadModifyWrite: return true;
        default: return false;
    }
}

bool Ir::UpdatesFlags(Opcode opcode)
{
    switch (opcode)
    {
        case Opcode::ComputeNzcvAdd:
        case Opcode::ComputeNzcvSubtract:
        case Opcode::ComputeNzcvLogical:
        case Opcode::FloatCompare:
        case Opcode::FloatCompareSignalling:
        case Opcode::WriteFlags: return true;
        default: return false;
    }
}

int Ir::SizeInBytes(ValueType type)
{
    switch (type)
    {
        case ValueType::Bool: return 1;
        case ValueType::Int8: return 1;
        case ValueType::Int16: return 2;
        case ValueType::Int32: return 4;
        case ValueType::Int64: return 8;
        case ValueType::Int128: return 16;
        case ValueType::Float16: return 2;
        case ValueType::Float32: return 4;
        case ValueType::Float64: return 8;
        case ValueType::Vector128: return 16;
        case ValueType::Invalid: return 0;
    }
    return 0;
}

int Ir::LaneSizeInBytes(LaneWidth laneWidth)
{
    switch (laneWidth)
    {
        case LaneWidth::Byte: return 1;
        case LaneWidth::Half: return 2;
        case LaneWidth::Word: return 4;
        case LaneWidth::Double: return 8;
        case LaneWidth::None: return 0;
    }
    return 0;
}

static constexpr uint64_t PROCESSOR_FEATURES_LOWEST_LEVELS = 0x11ull;
static constexpr uint64_t ISA_FEATURES_LARGE_ATOMICS = 0x200000ull;
static constexpr uint64_t MAIN_IDENTIFIER = 0x410FD080ull;
static constexpr uint64_t CACHE_GEOMETRY = 0x8444C004ull;
static constexpr uint64_t CACHE_ZERO_PROHIBITED = 0x10ull;

bool Ir::DescribeSystemRegister(SystemRegister systemRegister, uint64_t& value)
{
    switch (systemRegister)
    {
        case SystemRegister::MainIdRegister: value = MAIN_IDENTIFIER; return true;
        case SystemRegister::CacheTypeRegister: value = CACHE_GEOMETRY; return true;
        case SystemRegister::CacheZeroIdRegister: value = CACHE_ZERO_PROHIBITED; return true;
        case SystemRegister::ProcessorFeatureRegister0: value = PROCESSOR_FEATURES_LOWEST_LEVELS; return true;
        case SystemRegister::ProcessorFeatureRegister1: value = 0; return true;
        case SystemRegister::VectorFeatureRegister0: value = 0; return true;
        case SystemRegister::IsaFeatureRegister0: value = ISA_FEATURES_LARGE_ATOMICS; return true;
        case SystemRegister::IsaFeatureRegister1: value = 0; return true;
        default: return false;
    }
}

const char* Ir::OpcodeName(Opcode opcode)
{
    switch (opcode)
    {
        case Opcode::Invalid: return "Invalid";
        case Opcode::ReadGeneral: return "ReadGeneral";
        case Opcode::WriteGeneral: return "WriteGeneral";
        case Opcode::ReadVector: return "ReadVector";
        case Opcode::WriteVector: return "WriteVector";
        case Opcode::ReadFlags: return "ReadFlags";
        case Opcode::WriteFlags: return "WriteFlags";
        case Opcode::ReadStackPointer: return "ReadStackPointer";
        case Opcode::WriteStackPointer: return "WriteStackPointer";
        case Opcode::ReadSystemRegister: return "ReadSystemRegister";
        case Opcode::WriteSystemRegister: return "WriteSystemRegister";
        case Opcode::Constant: return "Constant";
        case Opcode::Add: return "Add";
        case Opcode::AddWithCarry: return "AddWithCarry";
        case Opcode::Subtract: return "Subtract";
        case Opcode::SubtractWithBorrow: return "SubtractWithBorrow";
        case Opcode::Multiply: return "Multiply";
        case Opcode::MultiplyHighSigned: return "MultiplyHighSigned";
        case Opcode::MultiplyHighUnsigned: return "MultiplyHighUnsigned";
        case Opcode::DivideSigned: return "DivideSigned";
        case Opcode::DivideUnsigned: return "DivideUnsigned";
        case Opcode::And: return "And";
        case Opcode::Or: return "Or";
        case Opcode::Xor: return "Xor";
        case Opcode::Not: return "Not";
        case Opcode::ShiftLeft: return "ShiftLeft";
        case Opcode::ShiftRightLogical: return "ShiftRightLogical";
        case Opcode::ShiftRightArithmetic: return "ShiftRightArithmetic";
        case Opcode::RotateRight: return "RotateRight";
        case Opcode::CountLeadingZeros: return "CountLeadingZeros";
        case Opcode::ReverseBits: return "ReverseBits";
        case Opcode::ReverseBytes: return "ReverseBytes";
        case Opcode::ExtendSigned: return "ExtendSigned";
        case Opcode::ExtendUnsigned: return "ExtendUnsigned";
        case Opcode::Truncate: return "Truncate";
        case Opcode::Select: return "Select";
        case Opcode::Compare: return "Compare";
        case Opcode::ComputeNzcvAdd: return "ComputeNzcvAdd";
        case Opcode::ComputeNzcvSubtract: return "ComputeNzcvSubtract";
        case Opcode::ComputeNzcvLogical: return "ComputeNzcvLogical";
        case Opcode::ConcatToWide: return "ConcatToWide";
        case Opcode::ExtractLow: return "ExtractLow";
        case Opcode::ExtractHigh: return "ExtractHigh";
        case Opcode::FloatAdd: return "FloatAdd";
        case Opcode::FloatSubtract: return "FloatSubtract";
        case Opcode::FloatMultiply: return "FloatMultiply";
        case Opcode::FloatDivide: return "FloatDivide";
        case Opcode::FloatCompare: return "FloatCompare";
        case Opcode::FloatCompareSignalling: return "FloatCompareSignalling";
        case Opcode::FloatConvert: return "FloatConvert";
        case Opcode::FloatSquareRoot: return "FloatSquareRoot";
        case Opcode::FloatMultiplyAdd: return "FloatMultiplyAdd";
        case Opcode::FloatMinimum: return "FloatMinimum";
        case Opcode::FloatMaximum: return "FloatMaximum";
        case Opcode::FloatRound: return "FloatRound";
        case Opcode::FloatConvertRounded: return "FloatConvertRounded";
        case Opcode::VectorAdd: return "VectorAdd";
        case Opcode::VectorSubtract: return "VectorSubtract";
        case Opcode::VectorMultiply: return "VectorMultiply";
        case Opcode::VectorAnd: return "VectorAnd";
        case Opcode::VectorOr: return "VectorOr";
        case Opcode::VectorXor: return "VectorXor";
        case Opcode::VectorCompare: return "VectorCompare";
        case Opcode::VectorShiftLeftSigned: return "VectorShiftLeftSigned";
        case Opcode::VectorShiftLeftUnsigned: return "VectorShiftLeftUnsigned";
        case Opcode::VectorExtractLane: return "VectorExtractLane";
        case Opcode::VectorExtract: return "VectorExtract";
        case Opcode::VectorInsertLane: return "VectorInsertLane";
        case Opcode::VectorDuplicate: return "VectorDuplicate";
        case Opcode::VectorReduceAdd: return "VectorReduceAdd";
        case Opcode::VectorCountBits: return "VectorCountBits";
        case Opcode::VectorWiden: return "VectorWiden";
        case Opcode::VectorSaturatingAddSigned: return "VectorSaturatingAddSigned";
        case Opcode::VectorSaturatingAddUnsigned: return "VectorSaturatingAddUnsigned";
        case Opcode::VectorSaturatingSubtractSigned: return "VectorSaturatingSubtractSigned";
        case Opcode::VectorSaturatingSubtractUnsigned: return "VectorSaturatingSubtractUnsigned";
        case Opcode::VectorFloatAdd: return "VectorFloatAdd";
        case Opcode::VectorFloatSubtract: return "VectorFloatSubtract";
        case Opcode::VectorFloatMultiply: return "VectorFloatMultiply";
        case Opcode::VectorFloatMultiplyAdd: return "VectorFloatMultiplyAdd";
        case Opcode::VectorFloatRound: return "VectorFloatRound";
        case Opcode::VectorFloatDivide: return "VectorFloatDivide";
        case Opcode::VectorFloatSquareRoot: return "VectorFloatSquareRoot";
        case Opcode::VectorFloatNegate: return "VectorFloatNegate";
        case Opcode::VectorFloatAbsolute: return "VectorFloatAbsolute";
        case Opcode::VectorConvertFloat: return "VectorConvertFloat";
        case Opcode::VectorReverseBytes: return "VectorReverseBytes";
        case Opcode::VectorReduceAddLong: return "VectorReduceAddLong";
        case Opcode::VectorFloatCompare: return "VectorFloatCompare";
        case Opcode::VectorFloatMinimum: return "VectorFloatMinimum";
        case Opcode::VectorFloatMaximum: return "VectorFloatMaximum";
        case Opcode::VectorFloatPairwiseAdd: return "VectorFloatPairwiseAdd";
        case Opcode::VectorFloatEstimate: return "VectorFloatEstimate";
        case Opcode::VectorNarrow: return "VectorNarrow";
        case Opcode::VectorPermute: return "VectorPermute";
        case Opcode::VectorMinMax: return "VectorMinMax";
        case Opcode::VectorPairwise: return "VectorPairwise";
        case Opcode::VectorAbsoluteDifference: return "VectorAbsoluteDifference";
        case Opcode::VectorAbsolute: return "VectorAbsolute";
        case Opcode::VectorCountLeadingZeros: return "VectorCountLeadingZeros";
        case Opcode::VectorPairwiseAddLong: return "VectorPairwiseAddLong";
        case Opcode::VectorFloatConvertWidth: return "VectorFloatConvertWidth";
        case Opcode::VectorTableLookup: return "VectorTableLookup";
        case Opcode::VectorReduceMinMax: return "VectorReduceMinMax";
        case Opcode::PointerAuthSign: return "PointerAuthSign";
        case Opcode::PointerAuthAuthenticate: return "PointerAuthAuthenticate";
        case Opcode::PointerAuthStrip: return "PointerAuthStrip";
        case Opcode::Hint: return "Hint";
        case Opcode::Load: return "Load";
        case Opcode::Store: return "Store";
        case Opcode::LoadExclusive: return "LoadExclusive";
        case Opcode::StoreExclusive: return "StoreExclusive";
        case Opcode::LoadExclusivePair: return "LoadExclusivePair";
        case Opcode::ReadExclusiveHigh: return "ReadExclusiveHigh";
        case Opcode::SetExclusiveExpected: return "SetExclusiveExpected";
        case Opcode::StoreExclusivePair: return "StoreExclusivePair";
        case Opcode::AtomicCompareExchangePair: return "AtomicCompareExchangePair";
        case Opcode::ClearExclusive: return "ClearExclusive";
        case Opcode::AtomicCompareExchange: return "AtomicCompareExchange";
        case Opcode::AtomicReadModifyWrite: return "AtomicReadModifyWrite";
        case Opcode::MemoryBarrier: return "MemoryBarrier";
        case Opcode::Branch: return "Branch";
        case Opcode::BranchConditional: return "BranchConditional";
        case Opcode::BranchIndirect: return "BranchIndirect";
        case Opcode::Call: return "Call";
        case Opcode::Return: return "Return";
        case Opcode::HostCall: return "HostCall";
        case Opcode::Syscall: return "Syscall";
        case Opcode::Undefined: return "Undefined";
    }
    return "Unknown";
}

const char* Ir::ConditionName(Condition condition)
{
    switch (condition)
    {
        case Condition::Equal: return "eq";
        case Condition::NotEqual: return "ne";
        case Condition::CarrySet: return "cs";
        case Condition::CarryClear: return "cc";
        case Condition::Minus: return "mi";
        case Condition::Plus: return "pl";
        case Condition::OverflowSet: return "vs";
        case Condition::OverflowClear: return "vc";
        case Condition::UnsignedHigher: return "hi";
        case Condition::UnsignedLowerOrSame: return "ls";
        case Condition::SignedGreaterEqual: return "ge";
        case Condition::SignedLess: return "lt";
        case Condition::SignedGreater: return "gt";
        case Condition::SignedLessEqual: return "le";
        case Condition::Always: return "al";
        case Condition::Never: return "nv";
    }
    return "??";
}

bool Ir::Verify(const Function& function)
{
    if (function.blocks.empty()) return false;

    for (const Block& block : function.blocks)
    {
        if (block.instructions.empty()) return false;

        std::vector<bool> defined(function.nextValueId, false);

        for (size_t index = 0; index < block.instructions.size(); ++index)
        {
            const Instruction& instruction = block.instructions[index];

            if (instruction.opcode == Opcode::Invalid) return false;
            if (instruction.operandCount > MAX_OPERANDS) return false;

            for (uint8_t operand = 0; operand < instruction.operandCount; ++operand)
            {
                if (instruction.operands[operand].id == INVALID_VALUE_ID) return false;
                if (instruction.operands[operand].type == ValueType::Invalid) return false;
                if (instruction.operands[operand].id >= function.nextValueId) return false;
                if (!defined[instruction.operands[operand].id]) return false;
            }

            if (instruction.result.id != INVALID_VALUE_ID)
            {
                if (instruction.result.id >= function.nextValueId) return false;
                if (defined[instruction.result.id]) return false;
                defined[instruction.result.id] = true;
            }

            bool terminator = IsTerminator(instruction.opcode);
            bool last = index + 1 == block.instructions.size();
            if (terminator && !last) return false;

            if (instruction.opcode == Opcode::AtomicReadModifyWrite && instruction.atomicOperation == AtomicOperation::Invalid) return false;
            if (instruction.opcode == Opcode::BranchConditional && instruction.condition == Condition::Always) return false;
        }
    }

    return true;
}

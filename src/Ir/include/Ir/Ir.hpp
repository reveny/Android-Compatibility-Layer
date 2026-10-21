#pragma once

#include <cstdint>
#include <vector>

#include "GuestState/GuestState.hpp"

namespace Ir
{
    inline constexpr int MAX_OPERANDS = 4;
    inline constexpr uint32_t INVALID_VALUE_ID = 0xFFFFFFFFu;
    inline constexpr GuestState::GuestAddress NO_SUCCESSOR = static_cast<GuestState::GuestAddress>(0xFFFFFFFFFFFFFFFFull);
    inline constexpr uint64_t COUNTER_FREQUENCY_HZ = 24000000;
    inline constexpr uint64_t INSTRUCTION_CACHE_LINE_BYTES = 64;

    static_assert(MAX_OPERANDS <= 255, "operandCount is a uint8_t and must be able to hold MAX_OPERANDS");

    enum class ValueType : uint8_t
    {
        Invalid,
        Bool,
        Int8,
        Int16,
        Int32,
        Int64,
        Int128,
        Float16,
        Float32,
        Float64,
        Vector128
    };

    enum class LaneWidth : uint8_t
    {
        None,
        Byte,
        Half,
        Word,
        Double
    };

    enum class Opcode : uint16_t
    {
        Invalid,

        ReadGeneral,
        WriteGeneral,
        ReadVector,
        WriteVector,
        ReadFlags,
        WriteFlags,
        ReadStackPointer,
        WriteStackPointer,
        ReadSystemRegister,
        WriteSystemRegister,

        Constant,

        Add,
        AddWithCarry,
        Subtract,
        SubtractWithBorrow,
        Multiply,
        MultiplyHighSigned,
        MultiplyHighUnsigned,
        DivideSigned,
        DivideUnsigned,
        And,
        Or,
        Xor,
        Not,
        ShiftLeft,
        ShiftRightLogical,
        ShiftRightArithmetic,
        RotateRight,
        CountLeadingZeros,
        ReverseBits,
        ReverseBytes,
        ExtendSigned,
        ExtendUnsigned,
        Truncate,
        Select,
        Compare,
        ComputeNzcvAdd,
        ComputeNzcvSubtract,
        ComputeNzcvLogical,

        ConcatToWide,
        ExtractLow,
        ExtractHigh,

        FloatAdd,
        FloatSubtract,
        FloatMultiply,
        FloatDivide,
        FloatCompare,
        FloatCompareSignalling,
        FloatConvert,
        FloatSquareRoot,
        FloatMultiplyAdd,
        FloatMinimum,
        FloatMaximum,
        FloatRound,
        FloatConvertRounded,

        VectorAdd,
        VectorSubtract,
        VectorMultiply,
        VectorAnd,
        VectorOr,
        VectorXor,
        VectorCompare,
        VectorShiftLeftSigned,
        VectorShiftLeftUnsigned,
        VectorExtractLane,
        VectorInsertLane,
        VectorExtract,
        VectorDuplicate,
        VectorReduceAdd,
        VectorCountBits,
        VectorWiden,
        VectorSaturatingAddSigned,
        VectorSaturatingAddUnsigned,
        VectorSaturatingSubtractSigned,
        VectorSaturatingSubtractUnsigned,
        VectorFloatAdd,
        VectorFloatSubtract,
        VectorFloatMultiply,
        VectorFloatMultiplyAdd,
        VectorFloatDivide,
        VectorFloatSquareRoot,
        VectorFloatRound,
        VectorFloatNegate,
        VectorFloatAbsolute,
        VectorConvertFloat,
        VectorReverseBytes,
        VectorReduceAddLong,
        VectorFloatCompare,
        VectorFloatMinimum,
        VectorFloatMaximum,
        VectorFloatPairwiseAdd,
        VectorFloatEstimate,
        VectorNarrow,
        VectorPermute,
        VectorMinMax,
        VectorPairwise,
        VectorAbsoluteDifference,
        VectorAbsolute,
        VectorCountLeadingZeros,
        VectorPairwiseAddLong,
        VectorFloatConvertWidth,
        VectorTableLookup,
        VectorReduceMinMax,

        PointerAuthSign,
        PointerAuthAuthenticate,
        PointerAuthStrip,
        Hint,

        Load,
        Store,
        LoadExclusive,
        StoreExclusive,
        LoadExclusivePair,
        ReadExclusiveHigh,
        SetExclusiveExpected,
        StoreExclusivePair,
        AtomicCompareExchangePair,
        ClearExclusive,
        AtomicCompareExchange,
        AtomicReadModifyWrite,
        MemoryBarrier,

        Branch,
        BranchConditional,
        BranchIndirect,
        Call,
        Return,
        HostCall,
        Syscall,
        Undefined,
        InvalidateInstructionCache
    };

    enum class Condition : uint8_t
    {
        Equal = 0,
        NotEqual = 1,
        CarrySet = 2,
        CarryClear = 3,
        Minus = 4,
        Plus = 5,
        OverflowSet = 6,
        OverflowClear = 7,
        UnsignedHigher = 8,
        UnsignedLowerOrSame = 9,
        SignedGreaterEqual = 10,
        SignedLess = 11,
        SignedGreater = 12,
        SignedLessEqual = 13,
        Always = 14,
        Never = 15
    };

    enum class AtomicOperation : uint8_t
    {
        Invalid,
        Swap,
        Add,
        Clear,
        SetBits,
        ExclusiveOr,
        MaxSigned,
        MaxUnsigned,
        MinSigned,
        MinUnsigned
    };

    enum class SystemRegister : uint16_t
    {
        Invalid,
        ThreadPointer,
        ThreadPointerReadOnly,
        FloatingPointControl,
        FloatingPointStatus,
        CacheTypeRegister,
        CacheZeroIdRegister,
        MainIdRegister,
        IsaFeatureRegister0,
        IsaFeatureRegister1,
        ProcessorFeatureRegister0,
        ProcessorFeatureRegister1,
        VectorFeatureRegister0,
        CounterVirtual,
        CounterFrequency
    };

    enum class ComparePredicate : uint8_t
    {
        Equal,
        NotEqual,
        LessSigned,
        LessEqualSigned,
        GreaterSigned,
        GreaterEqualSigned,
        LessUnsigned,
        LessEqualUnsigned,
        GreaterUnsigned,
        GreaterEqualUnsigned
    };

    enum class FloatPredicate : uint8_t
    {
        OrderedEqual,
        OrderedNotEqual,
        OrderedLess,
        OrderedLessEqual,
        OrderedGreater,
        OrderedGreaterEqual,
        Ordered,
        Unordered,
        UnorderedEqual,
        UnorderedNotEqual,
        UnorderedLess,
        UnorderedLessEqual,
        UnorderedGreater,
        UnorderedGreaterEqual
    };

    enum class MemoryOrder : uint8_t
    {
        Relaxed,
        Acquire,
        AcquireReadOnly,
        Release,
        AcquireRelease,
        SequentiallyConsistent
    };

    struct Value
    {
        uint32_t id = INVALID_VALUE_ID;
        ValueType type = ValueType::Invalid;
    };

    struct Instruction
    {
        GuestState::GuestAddress guestAddress{};
        uint64_t immediate = 0;
        Value result;
        Value operands[MAX_OPERANDS];
        Opcode opcode = Opcode::Invalid;
        SystemRegister systemRegister = SystemRegister::Invalid;
        ValueType operandType = ValueType::Invalid;
        LaneWidth laneWidth = LaneWidth::None;
        AtomicOperation atomicOperation = AtomicOperation::Invalid;
        MemoryOrder order = MemoryOrder::Relaxed;
        ComparePredicate predicate = ComparePredicate::Equal;
        FloatPredicate floatPredicate = FloatPredicate::OrderedEqual;
        Condition condition = Condition::Always;
        uint8_t operandCount = 0;
        uint8_t laneIndex = 0;
        uint8_t laneCount = 0;
        bool updatesFloatingPointStatus = false;
    };

    struct Block
    {
        GuestState::GuestAddress guestAddress{};
        GuestState::GuestAddress guestEndAddress{};
        std::vector<Instruction> instructions;
        GuestState::GuestAddress successorTaken = NO_SUCCESSOR;
        GuestState::GuestAddress successorFallthrough = NO_SUCCESSOR;
        bool endsWithIndirectBranch = false;
    };

    struct Function
    {
        GuestState::GuestAddress entryAddress{};
        GuestState::GuestAddress endAddress{};
        std::vector<Block> blocks;
        uint32_t nextValueId = 0;
    };

    Value MakeValue(uint32_t id, ValueType type);
    bool IsTerminator(Opcode opcode);
    bool IsMemoryAccess(Opcode opcode);
    bool UpdatesFlags(Opcode opcode);
    int SizeInBytes(ValueType type);
    int LaneSizeInBytes(LaneWidth laneWidth);
    bool DescribeSystemRegister(SystemRegister systemRegister, uint64_t& value);
    uint64_t ReadVirtualCounter();
    const char* OpcodeName(Opcode opcode);
    const char* ConditionName(Condition condition);
    bool Verify(const Function& function);
}

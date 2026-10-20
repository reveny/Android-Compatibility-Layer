#pragma once

#include <cstdint>

#include "GuestState/GuestState.hpp"
#include "Ir/Ir.hpp"

namespace IrSemantics
{
    inline constexpr uint64_t DEFAULT_DOUBLE_NAN = 0x7FF8000000000000ull;
    inline constexpr uint64_t DEFAULT_FLOAT_NAN = 0x7FC00000ull;

    uint64_t MaskToType(uint64_t value, Ir::ValueType type);
    int BitsInType(Ir::ValueType type);
    uint64_t SignExtendTo64(uint64_t value, int bits);

    uint32_t ComputeAddFlags(uint64_t left, uint64_t right, uint64_t carryIn, int bits);

    uint64_t ReverseBytesInLanes(uint64_t value, Ir::LaneWidth laneWidth, int bits);
    uint64_t ReverseBitsIn(uint64_t value, int bits);

    double BitsToDouble(uint64_t bits);
    uint64_t DoubleToBits(double value);
    float BitsToFloat(uint32_t bits);
    uint64_t FloatToBits(float value);

    bool IsSignallingDouble(uint64_t bits);
    bool IsSignallingFloat(uint32_t bits);

    uint32_t ClassifyDoubleResult(double left, double right, double outcome, Ir::Opcode opcode);

    uint64_t MaskLane(uint64_t value, int laneBytes);
    uint64_t ReadLane(const GuestState::VectorRegister& source, int lane, int laneBytes);
    void WriteLane(GuestState::VectorRegister& destination, int lane, int laneBytes, uint64_t value);

    struct VectorRequest
    {
        Ir::Opcode opcode = Ir::Opcode::Invalid;
        Ir::LaneWidth laneWidth = Ir::LaneWidth::Byte;
        int laneCount = 0;
        int laneIndex = 0;
        uint64_t immediate = 0;
    };

    bool ApplyVectorOperation(const VectorRequest& request, const GuestState::VectorRegister& left,
                              const GuestState::VectorRegister& right, const GuestState::VectorRegister& third,
                              GuestState::VectorRegister& produced, bool& producedIsWide, uint32_t& floatingPointStatus);

    uint32_t CaptureHostFloatExceptions();
    void ClearHostFloatExceptions();

    struct FloatRequest
    {
        Ir::Opcode opcode = Ir::Opcode::Invalid;
        Ir::ValueType operandType = Ir::ValueType::Invalid;
        Ir::ValueType resultType = Ir::ValueType::Invalid;
        Ir::ValueType sourceType = Ir::ValueType::Invalid;
        uint64_t immediate = 0;
    };

    bool ApplyFloatOperation(const FloatRequest& request, uint64_t left, uint64_t right, uint64_t third, uint64_t& produced,
                             uint32_t& floatingPointStatus);

    enum class AtomicOutcome : uint8_t
    {
        Ok,
        MemoryFault,
        Undefined
    };

    struct AtomicRequest
    {
        Ir::Opcode opcode = Ir::Opcode::Invalid;
        Ir::ValueType operandType = Ir::ValueType::Invalid;
        Ir::AtomicOperation atomicOperation = Ir::AtomicOperation::Invalid;
    };

    AtomicOutcome ApplyAtomicOperation(const AtomicRequest& request, const GuestState::MemoryAccessor& memory,
                                       GuestState::ExclusiveMonitor& exclusive, uint64_t address, uint64_t operand, uint64_t third,
                                       uint64_t& produced);
}

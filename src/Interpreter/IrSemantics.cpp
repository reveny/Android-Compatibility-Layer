#include "Interpreter/IrSemantics.hpp"

#include <cfenv>
#include <cstring>
#include <mutex>

static std::mutex& GuestAtomicMutex()
{
    static std::mutex mutex;
    return mutex;
}

static void* AtomicCell(const GuestState::MemoryAccessor& memory, uint64_t address, int width)
{
    if (!memory.TranslateForAtomic) return nullptr;
    if (width != 1 && width != 2 && width != 4 && width != 8) return nullptr;
    if (address % (uint64_t)width != 0) return nullptr;

    return memory.TranslateForAtomic(memory.context, (GuestState::GuestAddress)address, (uint64_t)width);
}

static uint64_t AtomicLoadCell(void* cell, int width)
{
    if (width == 1) return __atomic_load_n((uint8_t*)cell, __ATOMIC_SEQ_CST);
    if (width == 2) return __atomic_load_n((uint16_t*)cell, __ATOMIC_SEQ_CST);
    if (width == 4) return __atomic_load_n((uint32_t*)cell, __ATOMIC_SEQ_CST);
    return __atomic_load_n((uint64_t*)cell, __ATOMIC_SEQ_CST);
}

static bool AtomicCompareExchangeCell(void* cell, int width, uint64_t& expected, uint64_t desired)
{
    if (width == 1)
    {
        uint8_t narrow = (uint8_t)expected;
        bool swapped = __atomic_compare_exchange_n((uint8_t*)cell, &narrow, (uint8_t)desired, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        expected = narrow;
        return swapped;
    }

    if (width == 2)
    {
        uint16_t narrow = (uint16_t)expected;
        bool swapped = __atomic_compare_exchange_n((uint16_t*)cell, &narrow, (uint16_t)desired, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        expected = narrow;
        return swapped;
    }

    if (width == 4)
    {
        uint32_t narrow = (uint32_t)expected;
        bool swapped = __atomic_compare_exchange_n((uint32_t*)cell, &narrow, (uint32_t)desired, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        expected = narrow;
        return swapped;
    }

    return __atomic_compare_exchange_n((uint64_t*)cell, &expected, desired, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
}

uint64_t IrSemantics::MaskToType(uint64_t value, Ir::ValueType type)
{
    switch (type)
    {
        case Ir::ValueType::Bool: return value & 1;
        case Ir::ValueType::Int8: return value & 0xffull;
        case Ir::ValueType::Int16: return value & 0xffffull;
        case Ir::ValueType::Int32: return value & 0xffffffffull;
        default: return value;
    }
}

int IrSemantics::BitsInType(Ir::ValueType type)
{
    return Ir::SizeInBytes(type) * 8;
}

uint64_t IrSemantics::SignExtendTo64(uint64_t value, int bits)
{
    if (bits >= 64 || bits <= 0) return value;

    uint64_t masked = value & ((1ull << bits) - 1);
    uint64_t sign = 1ull << (bits - 1);
    return (masked ^ sign) - sign;
}

uint32_t IrSemantics::ComputeAddFlags(uint64_t left, uint64_t right, uint64_t carryIn, int bits)
{
    uint64_t mask = (bits >= 64) ? ~0ull : ((1ull << bits) - 1);
    uint64_t maskedLeft = left & mask;
    uint64_t maskedRight = right & mask;

    uint64_t sum = (maskedLeft + maskedRight + carryIn) & mask;

    bool carry = false;
    if (bits >= 64)
    {
        unsigned long long partial = 0;
        carry = __builtin_add_overflow((unsigned long long)maskedLeft, (unsigned long long)maskedRight, &partial);
        unsigned long long total = 0;
        carry = __builtin_add_overflow(partial, (unsigned long long)carryIn, &total) || carry;
    }
    else
    {
        carry = (maskedLeft + maskedRight + carryIn) > mask;
    }

    uint64_t signBit = 1ull << (bits - 1);
    bool leftNegative = (maskedLeft & signBit) != 0;
    bool rightNegative = (maskedRight & signBit) != 0;
    bool resultNegative = (sum & signBit) != 0;
    bool overflow = (leftNegative == rightNegative) && (resultNegative != leftNegative);

    uint32_t flags = 0;
    if (resultNegative) flags |= GuestState::FLAG_NEGATIVE;
    if (sum == 0) flags |= GuestState::FLAG_ZERO;
    if (carry) flags |= GuestState::FLAG_CARRY;
    if (overflow) flags |= GuestState::FLAG_OVERFLOW;
    return flags;
}

uint64_t IrSemantics::ReverseBytesInLanes(uint64_t value, Ir::LaneWidth laneWidth, int bits)
{
    int laneBytes = Ir::LaneSizeInBytes(laneWidth);
    if (laneBytes == 0) laneBytes = bits / 8;

    uint64_t result = 0;
    int totalBytes = bits / 8;
    for (int lane = 0; lane < totalBytes; lane += laneBytes)
    {
        for (int byte = 0; byte < laneBytes; ++byte)
        {
            uint64_t source = (value >> ((lane + byte) * 8)) & 0xffull;
            result |= source << ((lane + laneBytes - 1 - byte) * 8);
        }
    }
    return result;
}

uint64_t IrSemantics::ReverseBitsIn(uint64_t value, int bits)
{
    uint64_t result = 0;
    for (int index = 0; index < bits; ++index)
    {
        if ((value >> index) & 1) result |= 1ull << (bits - 1 - index);
    }
    return result;
}

double IrSemantics::BitsToDouble(uint64_t bits)
{
    double value = 0.0;
    memcpy(&value, &bits, sizeof(value));
    return value;
}

uint64_t IrSemantics::DoubleToBits(double value)
{
    uint64_t bits = 0;
    memcpy(&bits, &value, sizeof(bits));
    return bits;
}

float IrSemantics::BitsToFloat(uint32_t bits)
{
    float value = 0.0f;
    memcpy(&value, &bits, sizeof(value));
    return value;
}

uint64_t IrSemantics::FloatToBits(float value)
{
    uint32_t bits = 0;
    memcpy(&bits, &value, sizeof(bits));
    return bits;
}

bool IrSemantics::IsSignallingDouble(uint64_t bits)
{
    uint64_t exponent = (bits >> 52) & 0x7ff;
    uint64_t mantissa = bits & 0xfffffffffffffull;
    return exponent == 0x7ff && mantissa != 0 && ((mantissa >> 51) & 1) == 0;
}

bool IrSemantics::IsSignallingFloat(uint32_t bits)
{
    uint32_t exponent = (bits >> 23) & 0xff;
    uint32_t mantissa = bits & 0x7fffff;
    return exponent == 0xff && mantissa != 0 && ((mantissa >> 22) & 1) == 0;
}

uint32_t IrSemantics::ClassifyDoubleResult(double left, double right, double outcome, Ir::Opcode opcode)
{
    uint32_t raised = 0;

    bool leftNan = left != left;
    bool rightNan = right != right;

    if (opcode == Ir::Opcode::FloatDivide && right == 0.0 && !leftNan && left != 0.0) raised |= GuestState::FPSR_DIVIDE_BY_ZERO;
    if (opcode == Ir::Opcode::FloatDivide && right == 0.0 && left == 0.0) raised |= GuestState::FPSR_INVALID_OPERATION;

    if (IsSignallingDouble(DoubleToBits(left)) || IsSignallingDouble(DoubleToBits(right))) raised |= GuestState::FPSR_INVALID_OPERATION;

    if (outcome != outcome && !leftNan && !rightNan && (raised & GuestState::FPSR_DIVIDE_BY_ZERO) == 0)
    {
        raised |= GuestState::FPSR_INVALID_OPERATION;
    }

    bool infinite = outcome > 1.7976931348623157e308 || outcome < -1.7976931348623157e308;
    bool operandInfinite = left > 1.7976931348623157e308 || left < -1.7976931348623157e308 || right > 1.7976931348623157e308 ||
                           right < -1.7976931348623157e308;

    if (infinite && !operandInfinite && (raised & GuestState::FPSR_DIVIDE_BY_ZERO) == 0)
    {
        raised |= GuestState::FPSR_OVERFLOW | GuestState::FPSR_INEXACT;
    }

    return raised;
}

uint64_t IrSemantics::MaskLane(uint64_t value, int laneBytes)
{
    if (laneBytes >= 8) return value;
    return value & ((1ull << (laneBytes * 8)) - 1);
}

uint64_t IrSemantics::ReadLane(const GuestState::VectorRegister& source, int lane, int laneBytes)
{
    int bitOffset = lane * laneBytes * 8;
    uint64_t half = bitOffset >= 64 ? source.high : source.low;
    int shift = bitOffset & 63;
    return MaskLane(half >> shift, laneBytes);
}

void IrSemantics::WriteLane(GuestState::VectorRegister& destination, int lane, int laneBytes, uint64_t value)
{
    int bitOffset = lane * laneBytes * 8;
    uint64_t& half = bitOffset >= 64 ? destination.high : destination.low;
    int shift = bitOffset & 63;

    uint64_t mask = laneBytes >= 8 ? ~0ull : (((1ull << (laneBytes * 8)) - 1) << shift);
    half = (half & ~mask) | ((MaskLane(value, laneBytes) << shift) & mask);
}

static double SquareRootDouble(double value)
{
    return __builtin_sqrt(value);
}

static float SquareRootFloat(float value)
{
    return __builtin_sqrtf(value);
}

static bool SignedZeroIsNegative(uint64_t bits, bool isDouble)
{
    return isDouble ? (bits >> 63) != 0 : ((bits >> 31) & 1) != 0;
}

static double RoundToMode(double value, uint32_t mode)
{
    if (value != value) return value;

    if (mode == 1) return __builtin_ceil(value);
    if (mode == 2) return __builtin_floor(value);
    if (mode == 3) return __builtin_trunc(value);
    if (mode == 4) return __builtin_round(value);
    if (mode == 5) return __builtin_rint(value);

    return __builtin_nearbyint(value);
}

static uint64_t SaturateToInteger(double value, bool isSigned, int bits)
{
    if (value != value) return 0;

    if (isSigned)
    {
        double lowest = -__builtin_ldexp(1.0, bits - 1);
        double highest = __builtin_ldexp(1.0, bits - 1);
        if (value <= lowest) return (uint64_t)1 << (bits - 1);
        if (value >= highest) return (~(uint64_t)0) >> (65 - bits);
        return (uint64_t)(int64_t)value;
    }

    if (value <= 0.0) return 0;
    if (value >= __builtin_ldexp(1.0, bits)) return bits >= 64 ? ~(uint64_t)0 : ((uint64_t)1 << bits) - 1;
    return (uint64_t)value;
}

static float HalfToFloat(uint16_t bits)
{
    uint32_t sign = (uint32_t)(bits & 0x8000) << 16;
    uint32_t exponent = (bits >> 10) & 0x1f;
    uint32_t fraction = bits & 0x3ff;

    if (exponent == 0x1f) return IrSemantics::BitsToFloat(sign | 0x7f800000u | (fraction << 13));

    if (exponent == 0)
    {
        if (fraction == 0) return IrSemantics::BitsToFloat(sign);

        while ((fraction & 0x400) == 0)
        {
            fraction <<= 1;
            exponent -= 1;
        }

        exponent += 1;
        fraction &= 0x3ff;
    }

    return IrSemantics::BitsToFloat(sign | ((exponent + 112) << 23) | (fraction << 13));
}

struct StatusOnScopeExit
{
    uint32_t& status;

    explicit StatusOnScopeExit(uint32_t& tracked) : status(tracked) {}
    ~StatusOnScopeExit() { status |= IrSemantics::CaptureHostFloatExceptions(); }
};

static uint64_t FloatToHalf(float value)
{
    _Float16 narrowed = (_Float16)value;
    uint16_t bits = 0;
    memcpy(&bits, &narrowed, sizeof(bits));

    return bits;
}

static uint32_t ReciprocalEstimateEntry(uint32_t scaled)
{
    uint32_t a = scaled * 2 + 1;
    uint32_t b = (1u << 19) / a;
    return (b + 1) / 2;
}

static uint32_t ReciprocalSquareRootEstimateEntry(uint32_t scaled)
{
    uint32_t a = scaled;

    if (a < 256)
    {
        a = a * 2 + 1;
    }
    else
    {
        a = (a >> 1) << 1;
        a = (a + 1) * 2;
    }

    uint32_t b = 512;
    while ((uint64_t)a * (uint64_t)(b + 1) * (uint64_t)(b + 1) < (1ull << 28)) b += 1;

    return (b + 1) / 2;
}

static uint64_t FloatEstimateBits(uint64_t bits, bool isDouble, bool squareRoot, uint32_t& status)
{
    int fractionBits = isDouble ? 52 : 23;
    int exponentBits = isDouble ? 11 : 8;
    int bias = (1 << (exponentBits - 1)) - 1;

    uint64_t signMask = 1ull << (fractionBits + exponentBits);
    uint64_t fractionMask = (1ull << fractionBits) - 1;
    uint32_t topExponent = (1u << exponentBits) - 1;

    uint64_t sign = bits & signMask;
    int exponent = (int)((bits >> fractionBits) & topExponent);
    uint64_t fraction = bits & fractionMask;
    uint64_t quietNan = isDouble ? IrSemantics::DEFAULT_DOUBLE_NAN : IrSemantics::DEFAULT_FLOAT_NAN;

    if (exponent == (int)topExponent && fraction != 0)
    {
        if (isDouble ? IrSemantics::IsSignallingDouble(bits) : IrSemantics::IsSignallingFloat((uint32_t)bits))
        {
            status |= GuestState::FPSR_INVALID_OPERATION;
        }

        return quietNan;
    }

    if (squareRoot && sign != 0 && !(exponent == 0 && fraction == 0))
    {
        status |= GuestState::FPSR_INVALID_OPERATION;
        return quietNan;
    }

    if (exponent == (int)topExponent) return squareRoot ? 0 : sign;

    if (exponent == 0 && fraction == 0)
    {
        status |= GuestState::FPSR_DIVIDE_BY_ZERO;
        return sign | ((uint64_t)topExponent << fractionBits);
    }

    if (exponent == 0)
    {
        exponent = 1;

        while ((fraction & (1ull << (fractionBits - 1))) == 0)
        {
            fraction <<= 1;
            exponent -= 1;
        }

        fraction = (fraction << 1) & fractionMask;
    }

    if (!squareRoot)
    {
        uint32_t scaled = 0x100u | (uint32_t)(fraction >> (fractionBits - 8));
        int resultExponent = 2 * bias - 1 - exponent;
        uint32_t estimate = ReciprocalEstimateEntry(scaled);

        if (resultExponent < 1)
        {
            status |= GuestState::FPSR_UNDERFLOW | GuestState::FPSR_INEXACT;
            return sign;
        }

        if (resultExponent >= (int)topExponent)
        {
            status |= GuestState::FPSR_OVERFLOW | GuestState::FPSR_INEXACT;
            return sign | ((uint64_t)topExponent << fractionBits);
        }

        return sign | ((uint64_t)resultExponent << fractionBits) | ((uint64_t)(estimate & 0xff) << (fractionBits - 8));
    }

    uint32_t scaled = (exponent & 1) == 0 ? (0x100u | (uint32_t)(fraction >> (fractionBits - 8)))
                                          : (0x080u | (uint32_t)(fraction >> (fractionBits - 7)));

    int resultExponent = (3 * bias - 1 - exponent) / 2;
    uint32_t estimate = ReciprocalSquareRootEstimateEntry(scaled);

    return sign | ((uint64_t)resultExponent << fractionBits) | ((uint64_t)(estimate & 0xff) << (fractionBits - 8));
}

static uint64_t ConvertLaneDouble(uint64_t bits, uint64_t mode)
{
    bool toFloat = (mode & 1) != 0;
    bool isSigned = (mode & 2) != 0;

    if (!toFloat) return SaturateToInteger(IrSemantics::BitsToDouble(bits), isSigned, 64);
    if (isSigned) return IrSemantics::DoubleToBits((double)(int64_t)bits);
    return IrSemantics::DoubleToBits((double)bits);
}

static uint64_t ConvertLaneFloat(uint32_t bits, uint64_t mode)
{
    bool toFloat = (mode & 1) != 0;
    bool isSigned = (mode & 2) != 0;

    if (!toFloat) return SaturateToInteger((double)IrSemantics::BitsToFloat(bits), isSigned, 32);
    if (isSigned) return IrSemantics::FloatToBits((float)(int32_t)bits);
    return IrSemantics::FloatToBits((float)bits);
}

void IrSemantics::ClearHostFloatExceptions()
{
    feclearexcept(FE_ALL_EXCEPT);
}

uint32_t IrSemantics::CaptureHostFloatExceptions()
{
    int raised = fetestexcept(FE_ALL_EXCEPT);
    uint32_t status = 0;

    if (raised & FE_INVALID) status |= GuestState::FPSR_INVALID_OPERATION;
    if (raised & FE_DIVBYZERO) status |= GuestState::FPSR_DIVIDE_BY_ZERO;
    if (raised & FE_OVERFLOW) status |= GuestState::FPSR_OVERFLOW;
    if (raised & FE_UNDERFLOW) status |= GuestState::FPSR_UNDERFLOW;
    if (raised & FE_INEXACT) status |= GuestState::FPSR_INEXACT;

    return status;
}

bool IrSemantics::ApplyVectorOperation(const VectorRequest& request, const GuestState::VectorRegister& left,
                                       const GuestState::VectorRegister& right, const GuestState::VectorRegister& third,
                                       GuestState::VectorRegister& produced, bool& producedIsWide, uint32_t& floatingPointStatus)
{
    produced = GuestState::VectorRegister();
    producedIsWide = true;

    int laneBytes = Ir::LaneSizeInBytes(request.laneWidth);
    if (laneBytes == 0) laneBytes = 1;

    switch (request.opcode)
    {
        case Ir::Opcode::VectorShiftLeftSigned:
        case Ir::Opcode::VectorShiftLeftUnsigned:
        {
            bool arithmetic = request.opcode == Ir::Opcode::VectorShiftLeftSigned;
            int laneBits = laneBytes * 8;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t value = ReadLane(left, lane, laneBytes);
                int8_t amount = (int8_t)(ReadLane(right, lane, laneBytes) & 0xff);
                uint64_t outcome = 0;

                if (amount >= 0)
                {
                    outcome = amount >= laneBits ? 0 : value << amount;
                }
                else if (arithmetic)
                {
                    int shift = -amount >= laneBits ? laneBits - 1 : -amount;
                    int64_t signedValue = (int64_t)SignExtendTo64(value, laneBits);
                    outcome = (uint64_t)(signedValue >> shift);
                }
                else
                {
                    outcome = -amount >= laneBits ? 0 : value >> (-amount);
                }

                WriteLane(produced, lane, laneBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorAdd:
        case Ir::Opcode::VectorSubtract:
        case Ir::Opcode::VectorMultiply:
        case Ir::Opcode::VectorAnd:
        case Ir::Opcode::VectorOr:
        case Ir::Opcode::VectorXor:
        {
            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = ReadLane(right, lane, laneBytes);
                uint64_t outcome = 0;

                if (request.opcode == Ir::Opcode::VectorAdd) outcome = a + b;
                if (request.opcode == Ir::Opcode::VectorSubtract) outcome = a - b;
                if (request.opcode == Ir::Opcode::VectorMultiply) outcome = a * b;
                if (request.opcode == Ir::Opcode::VectorAnd) outcome = a & b;
                if (request.opcode == Ir::Opcode::VectorOr) outcome = a | b;
                if (request.opcode == Ir::Opcode::VectorXor) outcome = a ^ b;

                WriteLane(produced, lane, laneBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorDuplicate:
        {
            for (int lane = 0; lane < request.laneCount; ++lane) WriteLane(produced, lane, laneBytes, left.low);
            return true;
        }

        case Ir::Opcode::VectorExtractLane:
        {
            produced.low = ReadLane(left, request.laneIndex, laneBytes);
            produced.high = 0;
            producedIsWide = false;
            return true;
        }

        case Ir::Opcode::VectorExtract:
        {
            int totalBytes = request.laneCount;
            if (totalBytes != 8 && totalBytes != 16) totalBytes = 16;

            int index = request.laneIndex;
            if (index < 0 || index >= totalBytes) index = 0;

            for (int byte = 0; byte < totalBytes; ++byte)
            {
                int source = index + byte;
                uint64_t value = source < totalBytes ? ReadLane(left, source, 1) : ReadLane(right, source - totalBytes, 1);

                WriteLane(produced, byte, 1, value);
            }

            return true;
        }

        case Ir::Opcode::VectorInsertLane:
        {
            produced = left;
            WriteLane(produced, request.laneIndex, laneBytes, right.low);
            return true;
        }

        case Ir::Opcode::VectorReduceAdd:
        {
            uint64_t total = 0;
            for (int lane = 0; lane < request.laneCount; ++lane) total += ReadLane(left, lane, laneBytes);

            produced.low = MaskLane(total, laneBytes);
            produced.high = 0;
            return true;
        }

        case Ir::Opcode::VectorReduceAddLong:
        {
            bool signedSource = (request.immediate & 1) != 0;
            uint64_t total = 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t value = ReadLane(left, lane, laneBytes);
                if (signedSource) value = SignExtendTo64(value, laneBytes * 8);
                total += value;
            }

            produced.low = laneBytes >= 4 ? total : MaskLane(total, laneBytes * 2);
            produced.high = 0;
            return true;
        }

        case Ir::Opcode::VectorCompare:
        {
            uint64_t ones = MaskLane(~(uint64_t)0, laneBytes);
            uint64_t mode = request.immediate & 0x7;
            bool againstZero = (request.immediate & 0x8) != 0;
            bool unsignedForm = (request.immediate & 0x10) != 0;
            bool testBits = (request.immediate & 0x20) != 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = againstZero ? 0 : ReadLane(right, lane, laneBytes);
                bool outcome = false;

                if (testBits)
                {
                    outcome = (a & b) != 0;
                }
                else if (unsignedForm)
                {
                    if (mode == 0) outcome = a == b;
                    else if (mode == 1) outcome = a >= b;
                    else if (mode == 2) outcome = a > b;
                    else if (mode == 3) outcome = a <= b;
                    else outcome = a < b;
                }
                else
                {
                    int64_t sa = (int64_t)SignExtendTo64(a, laneBytes * 8);
                    int64_t sb = (int64_t)SignExtendTo64(b, laneBytes * 8);

                    if (mode == 0) outcome = sa == sb;
                    else if (mode == 1) outcome = sa >= sb;
                    else if (mode == 2) outcome = sa > sb;
                    else if (mode == 3) outcome = sa <= sb;
                    else outcome = sa < sb;
                }

                WriteLane(produced, lane, laneBytes, outcome ? ones : 0);
            }

            return true;
        }

        case Ir::Opcode::VectorReverseBytes:
        {
            int containerBytes = (int)request.immediate;
            if (containerBytes < 2 || containerBytes > 8) return false;

            int perContainer = containerBytes / laneBytes;
            if (perContainer < 2) return false;

            for (int lane = 0; lane < request.laneCount; lane += perContainer)
            {
                for (int offset = 0; offset < perContainer / 2; ++offset)
                {
                    int low = lane + offset;
                    int high = lane + perContainer - 1 - offset;
                    uint64_t a = ReadLane(left, low, laneBytes);
                    uint64_t b = ReadLane(left, high, laneBytes);
                    WriteLane(produced, low, laneBytes, b);
                    WriteLane(produced, high, laneBytes, a);
                }

                if (perContainer & 1)
                {
                    int middle = lane + perContainer / 2;
                    WriteLane(produced, middle, laneBytes, ReadLane(left, middle, laneBytes));
                }
            }

            return true;
        }

        case Ir::Opcode::VectorPermute:
        {
            bool takeOdd = (request.immediate & 1) != 0;
            uint64_t kind = request.immediate >> 1;
            int total = request.laneCount;

            for (int lane = 0; lane < total; ++lane)
            {
                int source = 0;

                if (kind == 0)
                {
                    source = lane * 2 + (takeOdd ? 1 : 0);
                }
                else if (kind == 1)
                {
                    source = (lane & ~1) + (takeOdd ? 1 : 0) + (lane & 1) * total;
                }
                else
                {
                    source = lane / 2 + (takeOdd ? total / 2 : 0) + (lane & 1) * total;
                }

                uint64_t value = source < total ? ReadLane(left, source, laneBytes) : ReadLane(right, source - total, laneBytes);
                WriteLane(produced, lane, laneBytes, value);
            }

            return true;
        }

        case Ir::Opcode::VectorMinMax:
        case Ir::Opcode::VectorAbsoluteDifference:
        {
            bool unsignedForm = (request.immediate & 1) != 0;
            bool minimum = (request.immediate & 2) != 0;
            bool difference = request.opcode == Ir::Opcode::VectorAbsoluteDifference;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = ReadLane(right, lane, laneBytes);
                uint64_t outcome = 0;

                if (unsignedForm)
                {
                    if (difference) outcome = a > b ? a - b : b - a;
                    else outcome = minimum ? (a < b ? a : b) : (a > b ? a : b);
                }
                else
                {
                    int64_t sa = (int64_t)SignExtendTo64(a, laneBytes * 8);
                    int64_t sb = (int64_t)SignExtendTo64(b, laneBytes * 8);

                    if (difference) outcome = (uint64_t)(sa > sb ? sa - sb : sb - sa);
                    else outcome = (uint64_t)(minimum ? (sa < sb ? sa : sb) : (sa > sb ? sa : sb));
                }

                WriteLane(produced, lane, laneBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorPairwise:
        {
            bool unsignedForm = (request.immediate & 1) != 0;
            uint64_t kind = (request.immediate >> 1) & 0x3;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                bool fromRight = lane >= request.laneCount / 2;
                const GuestState::VectorRegister& source = fromRight ? right : left;
                int base = (fromRight ? lane - request.laneCount / 2 : lane) * 2;

                uint64_t a = ReadLane(source, base, laneBytes);
                uint64_t b = ReadLane(source, base + 1, laneBytes);
                uint64_t outcome = 0;

                if (kind == 0)
                {
                    outcome = a + b;
                }
                else if (unsignedForm)
                {
                    outcome = kind == 1 ? (a > b ? a : b) : (a < b ? a : b);
                }
                else
                {
                    int64_t sa = (int64_t)SignExtendTo64(a, laneBytes * 8);
                    int64_t sb = (int64_t)SignExtendTo64(b, laneBytes * 8);
                    outcome = (uint64_t)(kind == 1 ? (sa > sb ? sa : sb) : (sa < sb ? sa : sb));
                }

                WriteLane(produced, lane, laneBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorPairwiseAddLong:
        {
            bool unsignedForm = (request.immediate & 1) != 0;
            bool accumulate = (request.immediate & 2) != 0;

            for (int lane = 0; lane < request.laneCount / 2; ++lane)
            {
                uint64_t a = ReadLane(left, lane * 2, laneBytes);
                uint64_t b = ReadLane(left, lane * 2 + 1, laneBytes);

                if (!unsignedForm)
                {
                    a = SignExtendTo64(a, laneBytes * 8);
                    b = SignExtendTo64(b, laneBytes * 8);
                }

                uint64_t outcome = a + b;
                if (accumulate) outcome += ReadLane(right, lane, laneBytes * 2);
                WriteLane(produced, lane, laneBytes * 2, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorAbsolute:
        {
            bool negate = (request.immediate & 1) != 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                int64_t value = (int64_t)SignExtendTo64(ReadLane(left, lane, laneBytes), laneBytes * 8);
                int64_t outcome = negate ? -value : (value < 0 ? -value : value);
                WriteLane(produced, lane, laneBytes, (uint64_t)outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorCountLeadingZeros:
        {
            bool signedForm = (request.immediate & 1) != 0;
            int laneBits = laneBytes * 8;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t value = MaskLane(ReadLane(left, lane, laneBytes), laneBytes);
                if (signedForm && (value & (1ull << (laneBits - 1))) != 0) value = MaskLane(~value, laneBytes);

                int count = 0;
                for (int bit = laneBits - 1; bit >= 0; --bit)
                {
                    if ((value >> bit) & 1) break;
                    count += 1;
                }

                WriteLane(produced, lane, laneBytes, (uint64_t)(signedForm ? count - 1 : count));
            }

            return true;
        }

        case Ir::Opcode::VectorReduceMinMax:
        {
            bool unsignedForm = (request.immediate & 1) != 0;
            bool minimum = (request.immediate & 2) != 0;

            uint64_t best = ReadLane(left, 0, laneBytes);

            for (int lane = 1; lane < request.laneCount; ++lane)
            {
                uint64_t value = ReadLane(left, lane, laneBytes);

                if (unsignedForm)
                {
                    if (minimum ? (value < best) : (value > best)) best = value;
                }
                else
                {
                    int64_t candidate = (int64_t)SignExtendTo64(value, laneBytes * 8);
                    int64_t current = (int64_t)SignExtendTo64(best, laneBytes * 8);
                    if (minimum ? (candidate < current) : (candidate > current)) best = value;
                }
            }

            produced.low = MaskLane(best, laneBytes);
            produced.high = 0;
            return true;
        }

        case Ir::Opcode::VectorTableLookup:
        {
            uint64_t base = request.immediate;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t selector = ReadLane(right, lane, 1);
                if (selector < base || selector >= base + 16) continue;

                WriteLane(produced, lane, 1, ReadLane(left, (int)(selector - base), 1));
            }

            return true;
        }

        case Ir::Opcode::VectorFloatConvertWidth:
        {
            bool widen = (request.immediate & 1) != 0;
            bool highHalf = (request.immediate & 2) != 0;
            bool halfPrecision = (request.immediate & 4) != 0;

            int narrowBytes = halfPrecision ? 2 : 4;
            int wideBytes = halfPrecision ? 4 : 8;
            int offset = highHalf ? request.laneCount : 0;

            ClearHostFloatExceptions();
            StatusOnScopeExit narrowing(floatingPointStatus);

            if (widen)
            {
                for (int lane = 0; lane < request.laneCount; ++lane)
                {
                    uint64_t source = ReadLane(left, lane + offset, narrowBytes);
                    uint64_t outcome = halfPrecision ? (uint64_t)FloatToBits(HalfToFloat((uint16_t)source))
                                                     : DoubleToBits((double)BitsToFloat((uint32_t)source));
                    WriteLane(produced, lane, wideBytes, outcome);
                }

                return true;
            }

            if (highHalf)
            {
                for (int lane = 0; lane < request.laneCount; ++lane)
                {
                    WriteLane(produced, lane, narrowBytes, ReadLane(right, lane, narrowBytes));
                }
            }

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t source = ReadLane(left, lane, wideBytes);
                uint64_t outcome = halfPrecision ? FloatToHalf(BitsToFloat((uint32_t)source))
                                                 : FloatToBits((float)BitsToDouble(source));
                WriteLane(produced, lane + offset, narrowBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorNarrow:
        {
            bool highHalf = (request.immediate & 1) != 0;
            int offset = highHalf ? request.laneCount : 0;

            if (highHalf)
            {
                for (int lane = 0; lane < request.laneCount; ++lane)
                {
                    WriteLane(produced, lane, laneBytes, ReadLane(right, lane, laneBytes));
                }
            }

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t value = ReadLane(left, lane, laneBytes * 2);
                WriteLane(produced, lane + offset, laneBytes, value);
            }

            return true;
        }

        case Ir::Opcode::VectorFloatPairwiseAdd:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            ClearHostFloatExceptions();

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                bool fromRight = lane >= request.laneCount / 2;
                const GuestState::VectorRegister& source = fromRight ? right : left;
                int base = (fromRight ? lane - request.laneCount / 2 : lane) * 2;

                uint64_t a = ReadLane(source, base, laneBytes);
                uint64_t b = ReadLane(source, base + 1, laneBytes);
                uint64_t outcome = 0;

                uint64_t kind = request.immediate & 0x3;

                if (laneBytes == 8)
                {
                    double x = BitsToDouble(a);
                    double y = BitsToDouble(b);
                    double value = kind == 0 ? x + y : (kind == 1 ? (x > y ? x : y) : (x < y ? x : y));
                    outcome = DoubleToBits(value);
                }
                else
                {
                    float x = BitsToFloat((uint32_t)a);
                    float y = BitsToFloat((uint32_t)b);
                    float value = kind == 0 ? x + y : (kind == 1 ? (x > y ? x : y) : (x < y ? x : y));
                    outcome = FloatToBits(value);
                }

                WriteLane(produced, lane, laneBytes, outcome);
            }

            floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::VectorFloatCompare:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            uint64_t ones = MaskLane(~(uint64_t)0, laneBytes);
            uint64_t mode = request.immediate & 0x7;
            bool againstZero = (request.immediate & 0x8) != 0;
            bool absolute = (request.immediate & 0x10) != 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = againstZero ? 0 : ReadLane(right, lane, laneBytes);

                double x = laneBytes == 8 ? BitsToDouble(a) : (double)BitsToFloat((uint32_t)a);
                double y = laneBytes == 8 ? BitsToDouble(b) : (double)BitsToFloat((uint32_t)b);

                if (absolute)
                {
                    x = x < 0.0 ? -x : x;
                    y = y < 0.0 ? -y : y;
                }

                bool outcome = false;
                if (mode == 0) outcome = x == y;
                else if (mode == 1) outcome = x >= y;
                else if (mode == 2) outcome = x > y;
                else if (mode == 3) outcome = x <= y;
                else if (mode == 4) outcome = x < y;

                WriteLane(produced, lane, laneBytes, outcome ? ones : 0);
            }

            return true;
        }

        case Ir::Opcode::VectorFloatEstimate:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            bool squareRoot = (request.immediate & 1) != 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t outcome = FloatEstimateBits(a, laneBytes == 8, squareRoot, floatingPointStatus);
                WriteLane(produced, lane, laneBytes, outcome);
            }

            return true;
        }

        case Ir::Opcode::VectorFloatRound:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            ClearHostFloatExceptions();

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);

                if (laneBytes == 8)
                {
                    double value = RoundToMode(BitsToDouble(a), (uint32_t)request.immediate);
                    uint64_t outcome = DoubleToBits(value);
                    if (value != value) outcome = DEFAULT_DOUBLE_NAN;
                    WriteLane(produced, lane, laneBytes, outcome);
                }
                else
                {
                    float value = (float)RoundToMode((double)BitsToFloat((uint32_t)a), (uint32_t)request.immediate);
                    uint64_t outcome = FloatToBits(value);
                    if (value != value) outcome = DEFAULT_FLOAT_NAN;
                    WriteLane(produced, lane, laneBytes, outcome);
                }
            }

            floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::VectorFloatMultiplyAdd:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            ClearHostFloatExceptions();

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = ReadLane(right, lane, laneBytes);
                uint64_t c = ReadLane(third, lane, laneBytes);

                if (laneBytes == 8)
                {
                    double x = BitsToDouble(a);
                    double y = BitsToDouble(b);
                    double z = BitsToDouble(c);
                    if (request.immediate != 0) x = -x;

                    double value = __builtin_fma(x, y, z);
                    uint64_t outcome = DoubleToBits(value);
                    if (value != value && x == x && y == y && z == z) outcome = DEFAULT_DOUBLE_NAN;
                    WriteLane(produced, lane, laneBytes, outcome);
                }
                else
                {
                    float x = BitsToFloat((uint32_t)a);
                    float y = BitsToFloat((uint32_t)b);
                    float z = BitsToFloat((uint32_t)c);
                    if (request.immediate != 0) x = -x;

                    float value = __builtin_fmaf(x, y, z);
                    uint64_t outcome = FloatToBits(value);
                    if (value != value && x == x && y == y && z == z) outcome = DEFAULT_FLOAT_NAN;
                    WriteLane(produced, lane, laneBytes, outcome);
                }
            }

            floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::VectorFloatMinimum:
        case Ir::Opcode::VectorFloatMaximum:
        case Ir::Opcode::VectorFloatAdd:
        case Ir::Opcode::VectorFloatSubtract:
        case Ir::Opcode::VectorFloatMultiply:
        case Ir::Opcode::VectorFloatDivide:
        case Ir::Opcode::VectorFloatSquareRoot:
        case Ir::Opcode::VectorFloatNegate:
        case Ir::Opcode::VectorFloatAbsolute:
        case Ir::Opcode::VectorConvertFloat:
        {
            if (laneBytes != 4 && laneBytes != 8) return false;

            bool signalling = request.opcode != Ir::Opcode::VectorFloatNegate && request.opcode != Ir::Opcode::VectorFloatAbsolute;
            if (signalling) ClearHostFloatExceptions();

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t a = ReadLane(left, lane, laneBytes);
                uint64_t b = ReadLane(right, lane, laneBytes);
                uint64_t outcome = 0;

                if (laneBytes == 8)
                {
                    double x = BitsToDouble(a);
                    double y = BitsToDouble(b);
                    double value = 0.0;

                    switch (request.opcode)
                    {
                        case Ir::Opcode::VectorFloatAdd: value = x + y; break;
                        case Ir::Opcode::VectorFloatSubtract: value = x - y; break;
                        case Ir::Opcode::VectorFloatMultiply: value = x * y; break;
                        case Ir::Opcode::VectorFloatDivide: value = x / y; break;
                        case Ir::Opcode::VectorFloatMinimum: value = x < y ? x : y; break;
                        case Ir::Opcode::VectorFloatMaximum: value = x > y ? x : y; break;
                        case Ir::Opcode::VectorFloatSquareRoot: value = SquareRootDouble(x); break;
                        case Ir::Opcode::VectorFloatNegate: outcome = a ^ 0x8000000000000000ull; break;
                        case Ir::Opcode::VectorFloatAbsolute: outcome = a & 0x7fffffffffffffffull; break;
                        default: break;
                    }

                    if (request.opcode == Ir::Opcode::VectorConvertFloat)
                    {
                        outcome = ConvertLaneDouble(a, request.immediate);
                    }
                    else if (request.opcode != Ir::Opcode::VectorFloatNegate &&
                             request.opcode != Ir::Opcode::VectorFloatAbsolute)
                    {
                        outcome = DoubleToBits(value);
                        if (value != value && x == x && y == y) outcome = DEFAULT_DOUBLE_NAN;
                    }
                }
                else
                {
                    float x = BitsToFloat((uint32_t)a);
                    float y = BitsToFloat((uint32_t)b);
                    float value = 0.0f;

                    switch (request.opcode)
                    {
                        case Ir::Opcode::VectorFloatAdd: value = x + y; break;
                        case Ir::Opcode::VectorFloatSubtract: value = x - y; break;
                        case Ir::Opcode::VectorFloatMultiply: value = x * y; break;
                        case Ir::Opcode::VectorFloatDivide: value = x / y; break;
                        case Ir::Opcode::VectorFloatMinimum: value = x < y ? x : y; break;
                        case Ir::Opcode::VectorFloatMaximum: value = x > y ? x : y; break;
                        case Ir::Opcode::VectorFloatSquareRoot: value = SquareRootFloat(x); break;
                        case Ir::Opcode::VectorFloatNegate: outcome = (a ^ 0x80000000ull) & 0xffffffffull; break;
                        case Ir::Opcode::VectorFloatAbsolute: outcome = a & 0x7fffffffull; break;
                        default: break;
                    }

                    if (request.opcode == Ir::Opcode::VectorConvertFloat)
                    {
                        outcome = ConvertLaneFloat((uint32_t)a, request.immediate);
                    }
                    else if (request.opcode != Ir::Opcode::VectorFloatNegate &&
                             request.opcode != Ir::Opcode::VectorFloatAbsolute)
                    {
                        outcome = FloatToBits(value);
                        if (value != value && x == x && y == y) outcome = DEFAULT_FLOAT_NAN;
                    }
                }

                WriteLane(produced, lane, laneBytes, outcome);
            }

            if (signalling) floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::VectorCountBits:
        {
            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t byte = ReadLane(left, lane, 1);
                WriteLane(produced, lane, 1, (uint64_t)__builtin_popcountll(byte));
            }

            return true;
        }

        case Ir::Opcode::VectorWiden:
        {
            bool signedWiden = (request.immediate & 1) != 0;
            bool highHalf = (request.immediate & 2) != 0;
            int offset = highHalf ? request.laneCount : 0;

            for (int lane = 0; lane < request.laneCount; ++lane)
            {
                uint64_t value = ReadLane(left, lane + offset, laneBytes);
                if (signedWiden) value = SignExtendTo64(value, laneBytes * 8);
                WriteLane(produced, lane, laneBytes * 2, value);
            }

            return true;
        }

        default: break;
    }

    return false;
}

bool IrSemantics::ApplyFloatOperation(const FloatRequest& request, uint64_t left, uint64_t right, uint64_t third, uint64_t& produced,
                                      uint32_t& floatingPointStatus)
{
    produced = 0;

    switch (request.opcode)
    {
        case Ir::Opcode::FloatMultiplyAdd:
        {
            bool negateProduct = (request.immediate & 1) != 0;
            bool negateAddend = (request.immediate & 2) != 0;

            ClearHostFloatExceptions();

            if (request.operandType == Ir::ValueType::Float64)
            {
                double factor = BitsToDouble(left);
                double other = BitsToDouble(right);
                double addend = BitsToDouble(third);

                if (negateProduct) factor = -factor;
                if (negateAddend) addend = -addend;

                double outcome = __builtin_fma(factor, other, addend);
                produced = DoubleToBits(outcome);

                if (outcome != outcome && factor == factor && other == other && addend == addend) produced = DEFAULT_DOUBLE_NAN;
            }
            else
            {
                float factor = BitsToFloat((uint32_t)left);
                float other = BitsToFloat((uint32_t)right);
                float addend = BitsToFloat((uint32_t)third);

                if (negateProduct) factor = -factor;
                if (negateAddend) addend = -addend;

                float outcome = __builtin_fmaf(factor, other, addend);
                produced = FloatToBits(outcome);

                if (outcome != outcome && factor == factor && other == other && addend == addend) produced = DEFAULT_FLOAT_NAN;
            }

            floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::FloatMinimum:
        case Ir::Opcode::FloatMaximum:
        {
            bool minimum = request.opcode == Ir::Opcode::FloatMinimum;
            bool numeric = (request.immediate & 1) != 0;
            bool isDouble = request.operandType == Ir::ValueType::Float64;

            double x = isDouble ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
            double y = isDouble ? BitsToDouble(right) : (double)BitsToFloat((uint32_t)right);

            bool leftNan = x != x;
            bool rightNan = y != y;
            double outcome = 0.0;

            if (leftNan && rightNan) outcome = x;
            else if (leftNan) outcome = numeric ? y : x;
            else if (rightNan) outcome = numeric ? x : y;
            else if (x == 0.0 && y == 0.0) outcome = minimum ? (SignedZeroIsNegative(left, isDouble) ? x : y)
                                                             : (SignedZeroIsNegative(left, isDouble) ? y : x);
            else outcome = minimum ? (x < y ? x : y) : (x > y ? x : y);

            if ((leftNan || rightNan) && !(leftNan && rightNan) && !numeric)
            {
                produced = isDouble ? DEFAULT_DOUBLE_NAN : DEFAULT_FLOAT_NAN;
                if (IsSignallingDouble(DoubleToBits(x)) || IsSignallingFloat((uint32_t)left) ||
                    IsSignallingFloat((uint32_t)right))
                {
                    floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
                }
                return true;
            }

            if (leftNan && rightNan)
            {
                produced = isDouble ? DEFAULT_DOUBLE_NAN : DEFAULT_FLOAT_NAN;
                return true;
            }

            produced = isDouble ? DoubleToBits(outcome) : FloatToBits((float)outcome);
            return true;
        }

        case Ir::Opcode::FloatRound:
        {
            bool isDouble = request.operandType == Ir::ValueType::Float64;
            ClearHostFloatExceptions();

            double value = isDouble ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
            double outcome = RoundToMode(value, (uint32_t)request.immediate);

            produced = isDouble ? DoubleToBits(outcome) : FloatToBits((float)outcome);
            if (request.immediate == 5) floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::FloatConvertRounded:
        {
            bool unsignedForm = (request.immediate & 1) != 0;
            uint32_t mode = (uint32_t)((request.immediate >> 1) & 0x7);
            bool toFloat = (request.immediate & 0x10) != 0;
            uint32_t fractionBits = (uint32_t)((request.immediate >> 8) & 0xff);
            double scale = __builtin_ldexp(1.0, (int)fractionBits);

            if (toFloat)
            {
                int sourceBits = request.sourceType == Ir::ValueType::Int64 ? 64 : 32;
                double source = unsignedForm ? (double)MaskToType(left, request.sourceType)
                                             : (double)(int64_t)SignExtendTo64(left, sourceBits);

                double outcome = source / scale;
                produced = request.resultType == Ir::ValueType::Float64 ? DoubleToBits(outcome) : FloatToBits((float)outcome);
                return true;
            }

            bool isDouble = request.sourceType == Ir::ValueType::Float64;
            int bits = request.resultType == Ir::ValueType::Int64 ? 64 : 32;

            double value = isDouble ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
            value *= scale;
            double rounded = RoundToMode(value, mode);

            produced = SaturateToInteger(rounded, !unsignedForm, bits);
            if (bits == 32) produced &= 0xffffffffull;

            if (value != value) floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
            else if (rounded != value) floatingPointStatus |= GuestState::FPSR_INEXACT;

            return true;
        }

        case Ir::Opcode::FloatSquareRoot:
        {
            ClearHostFloatExceptions();

            if (request.operandType == Ir::ValueType::Float64)
            {
                double a = BitsToDouble(left);
                double outcome = SquareRootDouble(a);

                produced = DoubleToBits(outcome);
                if (outcome != outcome && a == a)
                {
                    produced = DEFAULT_DOUBLE_NAN;
                    floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
                }
            }
            else
            {
                float a = BitsToFloat((uint32_t)left);
                float outcome = SquareRootFloat(a);

                produced = FloatToBits(outcome);
                if (outcome != outcome && a == a)
                {
                    produced = DEFAULT_FLOAT_NAN;
                    floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
                }
            }

            floatingPointStatus |= CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::FloatAdd:
        case Ir::Opcode::FloatSubtract:
        case Ir::Opcode::FloatMultiply:
        case Ir::Opcode::FloatDivide:
        {
            uint32_t raised = 0;

            ClearHostFloatExceptions();

            if (request.operandType == Ir::ValueType::Float64)
            {
                double a = BitsToDouble(left);
                double b = BitsToDouble(right);
                double outcome = 0.0;

                if (request.opcode == Ir::Opcode::FloatAdd) outcome = a + b;
                if (request.opcode == Ir::Opcode::FloatSubtract) outcome = a - b;
                if (request.opcode == Ir::Opcode::FloatMultiply) outcome = a * b;
                if (request.opcode == Ir::Opcode::FloatDivide) outcome = a / b;

                raised = ClassifyDoubleResult(a, b, outcome, request.opcode);
                produced = DoubleToBits(outcome);
                if (outcome != outcome && a == a && b == b) produced = DEFAULT_DOUBLE_NAN;
            }
            else
            {
                float a = BitsToFloat((uint32_t)left);
                float b = BitsToFloat((uint32_t)right);
                float outcome = 0.0f;

                if (request.opcode == Ir::Opcode::FloatAdd) outcome = a + b;
                if (request.opcode == Ir::Opcode::FloatSubtract) outcome = a - b;
                if (request.opcode == Ir::Opcode::FloatMultiply) outcome = a * b;
                if (request.opcode == Ir::Opcode::FloatDivide) outcome = a / b;

                raised = ClassifyDoubleResult((double)a, (double)b, (double)outcome, request.opcode);
                produced = FloatToBits(outcome);
                if (outcome != outcome && a == a && b == b) produced = DEFAULT_FLOAT_NAN;
            }

            floatingPointStatus |= raised | CaptureHostFloatExceptions();
            return true;
        }

        case Ir::Opcode::FloatCompare:
        case Ir::Opcode::FloatCompareSignalling:
        {
            bool signalling = request.opcode == Ir::Opcode::FloatCompareSignalling;
            bool isDouble = request.operandType == Ir::ValueType::Float64;

            double a = isDouble ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
            double b = isDouble ? BitsToDouble(right) : (double)BitsToFloat((uint32_t)right);

            bool aNan = a != a;
            bool bNan = b != b;
            bool aSignalling = isDouble ? IsSignallingDouble(left) : IsSignallingFloat((uint32_t)left);
            bool bSignalling = isDouble ? IsSignallingDouble(right) : IsSignallingFloat((uint32_t)right);

            uint32_t flags = 0;
            if (aNan || bNan)
            {
                flags = GuestState::FLAG_CARRY | GuestState::FLAG_OVERFLOW;
                if (signalling || aSignalling || bSignalling) floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
            }
            else if (a == b) flags = GuestState::FLAG_ZERO | GuestState::FLAG_CARRY;
            else if (a < b) flags = GuestState::FLAG_NEGATIVE;
            else flags = GuestState::FLAG_CARRY;

            produced = flags;
            return true;
        }

        case Ir::Opcode::FloatConvert:
        {
            if (request.immediate == 1)
            {
                produced = left;
                return true;
            }

            if (request.immediate == 2 || request.immediate == 3)
            {
                int sourceBits = request.sourceType == Ir::ValueType::Int64 ? 64 : 32;

                ClearHostFloatExceptions();

                if (request.resultType == Ir::ValueType::Float64)
                {
                    double outcome = request.immediate == 3 ? (double)MaskToType(left, request.sourceType)
                                                           : (double)(int64_t)SignExtendTo64(left, sourceBits);
                    produced = DoubleToBits(outcome);
                }
                else
                {
                    float outcome = request.immediate == 3 ? (float)MaskToType(left, request.sourceType)
                                                          : (float)(int64_t)SignExtendTo64(left, sourceBits);
                    produced = FloatToBits(outcome);
                }

                floatingPointStatus |= CaptureHostFloatExceptions();
                return true;
            }

            if (request.immediate == 6)
            {
                ClearHostFloatExceptions();

                if (request.resultType == Ir::ValueType::Float64)
                {
                    double widened =
                        request.sourceType == Ir::ValueType::Float64 ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
                    produced = DoubleToBits(widened);
                }
                else
                {
                    float narrowed = request.sourceType == Ir::ValueType::Float64 ? (float)BitsToDouble(left) : BitsToFloat((uint32_t)left);
                    produced = FloatToBits(narrowed);
                }

                floatingPointStatus |= CaptureHostFloatExceptions();
                return true;
            }

            double value = request.sourceType == Ir::ValueType::Float64 ? BitsToDouble(left) : (double)BitsToFloat((uint32_t)left);
            int resultBits = request.resultType == Ir::ValueType::Int64 ? 64 : 32;
            bool signedForm = request.immediate == 4;

            double truncated = __builtin_trunc(value);
            bool outOfRange = value != value;

            if (!outOfRange && signedForm)
            {
                outOfRange = truncated < -__builtin_ldexp(1.0, resultBits - 1) || truncated >= __builtin_ldexp(1.0, resultBits - 1);
            }
            else if (!outOfRange)
            {
                outOfRange = truncated < 0.0 || truncated >= __builtin_ldexp(1.0, resultBits);
            }

            produced = SaturateToInteger(value, signedForm, resultBits);
            if (resultBits == 32) produced &= 0xffffffffull;

            if (outOfRange) floatingPointStatus |= GuestState::FPSR_INVALID_OPERATION;
            else if (truncated != value) floatingPointStatus |= GuestState::FPSR_INEXACT;

            return true;
        }

        default: break;
    }

    return false;
}

IrSemantics::AtomicOutcome IrSemantics::ApplyAtomicOperation(const AtomicRequest& request, const GuestState::MemoryAccessor& memory,
                                                             GuestState::ExclusiveMonitor& exclusive, uint64_t address, uint64_t operand,
                                                             uint64_t third, uint64_t& produced)
{
    produced = 0;

    int width = Ir::SizeInBytes(request.operandType);
    int bits = BitsInType(request.operandType);

    if (request.opcode == Ir::Opcode::ClearExclusive)
    {
        exclusive = GuestState::ExclusiveMonitor();
        return AtomicOutcome::Ok;
    }

    void* cell = AtomicCell(memory, address, width);

    if (request.opcode == Ir::Opcode::LoadExclusive)
    {
        std::unique_lock<std::mutex> atomicGuard(GuestAtomicMutex(), std::defer_lock);
        if (!cell) atomicGuard.lock();

        uint64_t loaded = 0;
        if (cell) loaded = AtomicLoadCell(cell, width);
        else if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &loaded, (uint64_t)width))
            return AtomicOutcome::MemoryFault;

        exclusive.address = address;
        exclusive.valueLow = loaded;
        exclusive.valueHigh = 0;
        exclusive.widthBytes = (uint32_t)width;
        exclusive.valid = 1;

        produced = loaded;
        return AtomicOutcome::Ok;
    }

    if (request.opcode == Ir::Opcode::LoadExclusivePair)
    {
        std::lock_guard<std::mutex> held(GuestAtomicMutex());

        uint64_t low = 0;
        uint64_t high = 0;

        if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &low, (uint64_t)width) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(address + (uint64_t)width), &high, (uint64_t)width))
        {
            return AtomicOutcome::MemoryFault;
        }

        exclusive.address = address;
        exclusive.valueLow = low;
        exclusive.valueHigh = high;
        exclusive.widthBytes = (uint32_t)(2 * width);
        exclusive.valid = 1;

        produced = low;
        return AtomicOutcome::Ok;
    }

    if (request.opcode == Ir::Opcode::StoreExclusivePair)
    {
        std::lock_guard<std::mutex> held(GuestAtomicMutex());

        if (!exclusive.valid || exclusive.address != address || exclusive.widthBytes != (uint32_t)(2 * width))
        {
            exclusive = GuestState::ExclusiveMonitor();
            produced = 1;
            return AtomicOutcome::Ok;
        }

        uint64_t low = 0;
        uint64_t high = 0;

        if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &low, (uint64_t)width) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(address + (uint64_t)width), &high, (uint64_t)width))
        {
            return AtomicOutcome::MemoryFault;
        }

        if (low != exclusive.valueLow || high != exclusive.valueHigh)
        {
            exclusive = GuestState::ExclusiveMonitor();
            produced = 1;
            return AtomicOutcome::Ok;
        }

        uint64_t desiredLow = MaskToType(operand, request.operandType);
        uint64_t desiredHigh = MaskToType(third, request.operandType);

        if (!memory.Write || !memory.Write(memory.context, (GuestState::GuestAddress)address, &desiredLow, (uint64_t)width) ||
            !memory.Write(memory.context, (GuestState::GuestAddress)(address + (uint64_t)width), &desiredHigh, (uint64_t)width))
        {
            return AtomicOutcome::MemoryFault;
        }

        exclusive = GuestState::ExclusiveMonitor();
        produced = 0;
        return AtomicOutcome::Ok;
    }

    if (request.opcode == Ir::Opcode::AtomicCompareExchangePair)
    {
        std::lock_guard<std::mutex> held(GuestAtomicMutex());

        uint64_t expectedLow = exclusive.valueLow;
        uint64_t expectedHigh = exclusive.valueHigh;

        uint64_t low = 0;
        uint64_t high = 0;

        if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &low, (uint64_t)width) ||
            !memory.Read(memory.context, (GuestState::GuestAddress)(address + (uint64_t)width), &high, (uint64_t)width))
        {
            return AtomicOutcome::MemoryFault;
        }

        if (low == expectedLow && high == expectedHigh)
        {
            uint64_t desiredLow = MaskToType(operand, request.operandType);
            uint64_t desiredHigh = MaskToType(third, request.operandType);

            if (!memory.Write || !memory.Write(memory.context, (GuestState::GuestAddress)address, &desiredLow, (uint64_t)width) ||
                !memory.Write(memory.context, (GuestState::GuestAddress)(address + (uint64_t)width), &desiredHigh, (uint64_t)width))
            {
                return AtomicOutcome::MemoryFault;
            }
        }

        exclusive.valueLow = low;
        exclusive.valueHigh = high;

        produced = low;
        return AtomicOutcome::Ok;
    }

    if (request.opcode == Ir::Opcode::StoreExclusive)
    {
        if (cell && exclusive.valid && exclusive.address == address && exclusive.widthBytes == (uint32_t)width)
        {
            uint64_t expected = exclusive.valueLow;
            bool swapped = AtomicCompareExchangeCell(cell, width, expected, MaskToType(operand, request.operandType));

            exclusive = GuestState::ExclusiveMonitor();
            produced = swapped ? 0 : 1;
            return AtomicOutcome::Ok;
        }

        std::unique_lock<std::mutex> atomicGuard(GuestAtomicMutex(), std::defer_lock);
        if (!cell) atomicGuard.lock();

        if (!exclusive.valid || exclusive.address != address || exclusive.widthBytes != (uint32_t)width)
        {
            exclusive = GuestState::ExclusiveMonitor();
            produced = 1;
            return AtomicOutcome::Ok;
        }

        uint64_t observed = 0;
        if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &observed, (uint64_t)width))
            return AtomicOutcome::MemoryFault;

        if (observed != exclusive.valueLow)
        {
            exclusive = GuestState::ExclusiveMonitor();
            produced = 1;
            return AtomicOutcome::Ok;
        }

        uint64_t payload = operand;
        if (!memory.Write || !memory.Write(memory.context, (GuestState::GuestAddress)address, &payload, (uint64_t)width))
            return AtomicOutcome::MemoryFault;

        exclusive = GuestState::ExclusiveMonitor();
        produced = 0;
        return AtomicOutcome::Ok;
    }

    if (request.opcode == Ir::Opcode::AtomicCompareExchange)
    {
        if (cell)
        {
            uint64_t expected = MaskToType(operand, request.operandType);
            AtomicCompareExchangeCell(cell, width, expected, MaskToType(third, request.operandType));
            produced = expected;
            return AtomicOutcome::Ok;
        }

        std::lock_guard<std::mutex> atomicGuard(GuestAtomicMutex());

        uint64_t observed = 0;
        if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &observed, (uint64_t)width))
            return AtomicOutcome::MemoryFault;

        if (observed == MaskToType(operand, request.operandType))
        {
            uint64_t payload = third;
            if (!memory.Write || !memory.Write(memory.context, (GuestState::GuestAddress)address, &payload, (uint64_t)width))
                return AtomicOutcome::MemoryFault;
        }

        produced = observed;
        return AtomicOutcome::Ok;
    }

    if (request.opcode != Ir::Opcode::AtomicReadModifyWrite) return AtomicOutcome::Undefined;

    std::unique_lock<std::mutex> atomicGuard(GuestAtomicMutex(), std::defer_lock);
    if (!cell) atomicGuard.lock();

    uint64_t observed = 0;
    if (cell) observed = AtomicLoadCell(cell, width);
    else if (!memory.Read || !memory.Read(memory.context, (GuestState::GuestAddress)address, &observed, (uint64_t)width))
        return AtomicOutcome::MemoryFault;

    uint64_t masked = MaskToType(operand, request.operandType);

    for (;;)
    {
        uint64_t updated = masked;

        switch (request.atomicOperation)
        {
            case Ir::AtomicOperation::Swap: updated = masked; break;
            case Ir::AtomicOperation::Add: updated = observed + masked; break;
            case Ir::AtomicOperation::Clear: updated = observed & ~masked; break;
            case Ir::AtomicOperation::SetBits: updated = observed | masked; break;
            case Ir::AtomicOperation::ExclusiveOr: updated = observed ^ masked; break;
            case Ir::AtomicOperation::MaxUnsigned: updated = observed > masked ? observed : masked; break;
            case Ir::AtomicOperation::MinUnsigned: updated = observed < masked ? observed : masked; break;
            case Ir::AtomicOperation::MaxSigned:
                updated = (int64_t)SignExtendTo64(observed, bits) > (int64_t)SignExtendTo64(masked, bits) ? observed : masked;
                break;
            case Ir::AtomicOperation::MinSigned:
                updated = (int64_t)SignExtendTo64(observed, bits) < (int64_t)SignExtendTo64(masked, bits) ? observed : masked;
                break;
            case Ir::AtomicOperation::Invalid: return AtomicOutcome::Undefined;
        }

        uint64_t payload = MaskToType(updated, request.operandType);

        if (cell)
        {
            uint64_t expected = observed;
            if (AtomicCompareExchangeCell(cell, width, expected, payload)) break;

            observed = expected;
            continue;
        }

        if (!memory.Write || !memory.Write(memory.context, (GuestState::GuestAddress)address, &payload, (uint64_t)width))
            return AtomicOutcome::MemoryFault;

        break;
    }

    produced = observed;
    return AtomicOutcome::Ok;
}

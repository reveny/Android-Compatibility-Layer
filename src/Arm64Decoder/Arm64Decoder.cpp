#include "Arm64Decoder/Arm64Decoder.hpp"

#include <algorithm>
#include <cstdio>

static uint32_t AllocateValue(Ir::Function& function)
{
    return function.nextValueId++;
}

static Ir::ValueType WidthType(bool is64Bit)
{
    return is64Bit ? Ir::ValueType::Int64 : Ir::ValueType::Int32;
}

static Ir::Instruction& Append(Ir::Block& block, Ir::Opcode opcode, GuestState::GuestAddress address, Ir::ValueType type)
{
    Ir::Instruction instruction;
    instruction.opcode = opcode;
    instruction.guestAddress = address;
    instruction.operandType = type;
    block.instructions.push_back(instruction);
    return block.instructions.back();
}

static Ir::Value EmitConstant(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint64_t value, Ir::ValueType type)
{
    Ir::Instruction& instruction = Append(block, Ir::Opcode::Constant, address, type);
    instruction.immediate = value;
    instruction.result = Ir::MakeValue(AllocateValue(function), type);
    return instruction.result;
}

static Ir::Value EmitReadGeneral(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint32_t number, Ir::ValueType type,
                                 bool thirtyOneIsStackPointer)
{
    if (number == 31 && !thirtyOneIsStackPointer) return EmitConstant(function, block, address, 0, type);

    Ir::Opcode opcode = (number == 31) ? Ir::Opcode::ReadStackPointer : Ir::Opcode::ReadGeneral;
    Ir::Instruction& instruction = Append(block, opcode, address, type);
    instruction.immediate = number;
    instruction.result = Ir::MakeValue(AllocateValue(function), type);
    return instruction.result;
}

static void EmitWriteGeneral(Ir::Block& block, GuestState::GuestAddress address, uint32_t number, Ir::Value value, Ir::ValueType type,
                             bool thirtyOneIsStackPointer)
{
    if (number == 31 && !thirtyOneIsStackPointer) return;

    Ir::Opcode opcode = (number == 31) ? Ir::Opcode::WriteStackPointer : Ir::Opcode::WriteGeneral;
    Ir::Instruction& instruction = Append(block, opcode, address, type);
    instruction.immediate = number;
    instruction.operands[0] = value;
    instruction.operandCount = 1;
}

static Ir::Value EmitBinary(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, Ir::Opcode opcode, Ir::Value left,
                            Ir::Value right, Ir::ValueType type)
{
    Ir::Instruction& instruction = Append(block, opcode, address, type);
    instruction.operands[0] = left;
    instruction.operands[1] = right;
    instruction.operandCount = 2;
    instruction.result = Ir::MakeValue(AllocateValue(function), type);
    return instruction.result;
}

static Ir::Value EmitUnary(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, Ir::Opcode opcode, Ir::Value operand,
                           Ir::ValueType type)
{
    Ir::Instruction& instruction = Append(block, opcode, address, type);
    instruction.operands[0] = operand;
    instruction.operandCount = 1;
    instruction.result = Ir::MakeValue(AllocateValue(function), type);
    return instruction.result;
}

static void EmitFlagsFrom(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, Ir::Opcode nzcvOpcode, Ir::Value left,
                          Ir::Value right, Ir::Value carryIn, Ir::ValueType type)
{
    Ir::Instruction& compute = Append(block, nzcvOpcode, address, type);
    compute.operands[0] = left;
    compute.operands[1] = right;
    compute.operands[2] = carryIn;
    compute.operandCount = 3;
    compute.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

    Ir::Instruction& write = Append(block, Ir::Opcode::WriteFlags, address, Ir::ValueType::Int32);
    write.operands[0] = compute.result;
    write.operandCount = 1;
}

static uint64_t SignExtend(uint64_t value, int bits)
{
    uint64_t sign = 1ull << (bits - 1);
    return (value ^ sign) - sign;
}

static bool DecodeBitMask(bool is64Bit, uint32_t immN, uint32_t imms, uint32_t immr, uint64_t& result)
{
    uint32_t combined = (immN << 6) | (~imms & 0x3f);
    if (combined == 0) return false;

    int length = 31 - __builtin_clz(combined);
    if (length < 1) return false;
    if (!is64Bit && immN != 0) return false;

    uint32_t size = 1u << length;
    uint32_t levels = size - 1;
    uint32_t s = imms & levels;
    uint32_t r = immr & levels;
    if (s == levels) return false;

    uint64_t welem = (s + 1 >= 64) ? ~0ull : ((1ull << (s + 1)) - 1);
    uint64_t pattern = welem;
    if (r != 0) pattern = ((welem >> r) | (welem << (size - r)));
    if (size < 64) pattern &= ((1ull << size) - 1);

    uint64_t replicated = 0;
    for (uint32_t filled = 0; filled < 64; filled += size)
    {
        replicated |= pattern << filled;
    }

    result = is64Bit ? replicated : (replicated & 0xFFFFFFFFull);
    return true;
}

static bool DecodeBitMasks(bool is64Bit, uint32_t immN, uint32_t imms, uint32_t immr, uint64_t& wmask, uint64_t& tmask)
{
    uint32_t combined = (immN << 6) | (~imms & 0x3f);
    if (combined == 0) return false;

    int length = 31 - __builtin_clz(combined);
    if (length < 1) return false;
    if (!is64Bit && immN != 0) return false;

    uint32_t size = 1u << length;
    uint32_t levels = size - 1;
    uint32_t s = imms & levels;
    uint32_t r = immr & levels;
    uint32_t diff = (s - r) & levels;

    uint64_t welem = (s + 1 >= 64) ? ~0ull : ((1ull << (s + 1)) - 1);
    uint64_t telem = (diff + 1 >= 64) ? ~0ull : ((1ull << (diff + 1)) - 1);

    uint64_t rotated = welem;
    if (r != 0 && size < 64) rotated = ((welem >> r) | (welem << (size - r)));
    if (r != 0 && size == 64) rotated = ((welem >> r) | (welem << (64 - r)));
    if (size < 64) rotated &= ((1ull << size) - 1);

    wmask = 0;
    tmask = 0;
    for (uint32_t filled = 0; filled < 64; filled += size)
    {
        wmask |= rotated << filled;
        tmask |= telem << filled;
    }

    if (!is64Bit)
    {
        wmask &= 0xFFFFFFFFull;
        tmask &= 0xFFFFFFFFull;
    }

    return true;
}

static Ir::Value EmitShiftedRegister(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint32_t number, uint32_t shiftType,
                                     uint32_t amount, Ir::ValueType type)
{
    Ir::Value value = EmitReadGeneral(function, block, address, number, type, false);
    if (amount == 0 && shiftType != 3) return value;

    Ir::Opcode opcode = Ir::Opcode::ShiftLeft;
    if (shiftType == 1) opcode = Ir::Opcode::ShiftRightLogical;
    if (shiftType == 2) opcode = Ir::Opcode::ShiftRightArithmetic;
    if (shiftType == 3) opcode = Ir::Opcode::RotateRight;

    Ir::Value shift = EmitConstant(function, block, address, amount, type);
    return EmitBinary(function, block, address, opcode, value, shift, type);
}

static Arm64Decoder::Result DecodeDataProcessingImmediate(Ir::Function& function, Ir::Block& block, uint32_t encoding,
                                                          GuestState::GuestAddress address)
{
    bool is64Bit = (encoding >> 31) & 1;
    Ir::ValueType type = WidthType(is64Bit);
    uint32_t op0 = (encoding >> 23) & 0x7;
    uint32_t rd = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;

    if (op0 == 0 || op0 == 1)
    {
        uint32_t immlo = (encoding >> 29) & 0x3;
        uint32_t immhi = (encoding >> 5) & 0x7ffff;
        uint64_t offset = SignExtend((immhi << 2) | immlo, 21);
        uint64_t base = static_cast<uint64_t>(address);

        if ((encoding >> 31) & 1)
        {
            offset <<= 12;
            base &= ~0xfffull;
        }

        Ir::Value value = EmitConstant(function, block, address, base + offset, Ir::ValueType::Int64);
        EmitWriteGeneral(block, address, rd, value, Ir::ValueType::Int64, false);
        return Arm64Decoder::Result::Ok;
    }

    if (op0 == 2 || op0 == 3)
    {
        bool subtract = (encoding >> 30) & 1;
        bool setFlags = (encoding >> 29) & 1;
        uint32_t shift = (encoding >> 22) & 0x3;
        uint64_t immediate = (encoding >> 10) & 0xfff;
        if (shift == 1) immediate <<= 12;
        if (shift > 1) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, !setFlags);
        Ir::Value right = EmitConstant(function, block, address, immediate, type);
        Ir::Value result = EmitBinary(function, block, address, subtract ? Ir::Opcode::Subtract : Ir::Opcode::Add, left, right, type);

        if (setFlags)
        {
            Ir::Value carry = EmitConstant(function, block, address, subtract ? 1 : 0, Ir::ValueType::Int32);
            Ir::Opcode nzcv = subtract ? Ir::Opcode::ComputeNzcvSubtract : Ir::Opcode::ComputeNzcvAdd;
            EmitFlagsFrom(function, block, address, nzcv, left, right, carry, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, !setFlags);
        return Arm64Decoder::Result::Ok;
    }

    if (op0 == 4)
    {
        uint32_t opc = (encoding >> 29) & 0x3;
        uint32_t immN = (encoding >> 22) & 1;
        uint32_t immr = (encoding >> 16) & 0x3f;
        uint32_t imms = (encoding >> 10) & 0x3f;

        uint64_t mask = 0;
        if (!DecodeBitMask(is64Bit, immN, imms, immr, mask)) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitConstant(function, block, address, mask, type);

        Ir::Opcode opcode = Ir::Opcode::And;
        if (opc == 1) opcode = Ir::Opcode::Or;
        if (opc == 2) opcode = Ir::Opcode::Xor;

        Ir::Value result = EmitBinary(function, block, address, opcode, left, right, type);

        if (opc == 3)
        {
            Ir::Value zero = EmitConstant(function, block, address, 0, Ir::ValueType::Int32);
            EmitFlagsFrom(function, block, address, Ir::Opcode::ComputeNzcvLogical, result, zero, zero, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, opc == 3 ? false : true);
        return Arm64Decoder::Result::Ok;
    }

    if (op0 == 5)
    {
        uint32_t opc = (encoding >> 29) & 0x3;
        uint32_t hw = (encoding >> 21) & 0x3;
        uint64_t immediate = (encoding >> 5) & 0xffff;
        uint32_t shift = hw * 16;
        if (!is64Bit && hw > 1) return Arm64Decoder::Result::Unallocated;

        if (opc == 0)
        {
            uint64_t value = ~(immediate << shift);
            if (!is64Bit) value &= 0xFFFFFFFFull;
            Ir::Value constant = EmitConstant(function, block, address, value, type);
            EmitWriteGeneral(block, address, rd, constant, type, false);
            return Arm64Decoder::Result::Ok;
        }

        if (opc == 2)
        {
            Ir::Value constant = EmitConstant(function, block, address, immediate << shift, type);
            EmitWriteGeneral(block, address, rd, constant, type, false);
            return Arm64Decoder::Result::Ok;
        }

        if (opc == 3)
        {
            uint64_t clearMask = ~(0xffffull << shift);
            Ir::Value previous = EmitReadGeneral(function, block, address, rd, type, false);
            Ir::Value keep = EmitBinary(function, block, address, Ir::Opcode::And, previous,
                                        EmitConstant(function, block, address, clearMask, type), type);
            Ir::Value merged = EmitBinary(function, block, address, Ir::Opcode::Or, keep,
                                          EmitConstant(function, block, address, immediate << shift, type), type);
            EmitWriteGeneral(block, address, rd, merged, type, false);
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unallocated;
    }

    if (op0 == 6)
    {
        uint32_t opc = (encoding >> 29) & 0x3;
        uint32_t immN = (encoding >> 22) & 1;
        uint32_t immr = (encoding >> 16) & 0x3f;
        uint32_t imms = (encoding >> 10) & 0x3f;

        if (opc == 3) return Arm64Decoder::Result::Unallocated;
        if (is64Bit != (immN == 1)) return Arm64Decoder::Result::Unallocated;
        if (!is64Bit && ((immr | imms) & 0x20) != 0) return Arm64Decoder::Result::Unallocated;

        uint64_t wmask = 0;
        uint64_t tmask = 0;
        if (!DecodeBitMasks(is64Bit, immN, imms, immr, wmask, tmask)) return Arm64Decoder::Result::Unallocated;

        Ir::Value source = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value rotateAmount = EmitConstant(function, block, address, immr, type);
        Ir::Value rotated = immr == 0 ? source : EmitBinary(function, block, address, Ir::Opcode::RotateRight, source, rotateAmount, type);

        Ir::Value bottom = EmitBinary(function, block, address, Ir::Opcode::And, rotated, EmitConstant(function, block, address, wmask, type), type);

        if (opc == 1)
        {
            Ir::Value previous = EmitReadGeneral(function, block, address, rd, type, false);
            Ir::Value keptWithin =
                EmitBinary(function, block, address, Ir::Opcode::And, previous, EmitConstant(function, block, address, ~wmask, type), type);
            bottom = EmitBinary(function, block, address, Ir::Opcode::Or, bottom, keptWithin, type);

            Ir::Value keptOutside =
                EmitBinary(function, block, address, Ir::Opcode::And, previous, EmitConstant(function, block, address, ~tmask, type), type);
            Ir::Value inside =
                EmitBinary(function, block, address, Ir::Opcode::And, bottom, EmitConstant(function, block, address, tmask, type), type);
            Ir::Value merged = EmitBinary(function, block, address, Ir::Opcode::Or, keptOutside, inside, type);
            EmitWriteGeneral(block, address, rd, merged, type, false);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value inside = EmitBinary(function, block, address, Ir::Opcode::And, bottom, EmitConstant(function, block, address, tmask, type), type);

        if (opc == 2)
        {
            EmitWriteGeneral(block, address, rd, inside, type, false);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value signShift = EmitConstant(function, block, address, imms, type);
        Ir::Value signBit = EmitBinary(function, block, address, Ir::Opcode::ShiftRightLogical, source, signShift, type);
        Ir::Value isolated =
            EmitBinary(function, block, address, Ir::Opcode::And, signBit, EmitConstant(function, block, address, 1, type), type);
        Ir::Value zero = EmitConstant(function, block, address, 0, type);
        Ir::Value replicated = EmitBinary(function, block, address, Ir::Opcode::Subtract, zero, isolated, type);
        Ir::Value top =
            EmitBinary(function, block, address, Ir::Opcode::And, replicated, EmitConstant(function, block, address, ~tmask, type), type);
        Ir::Value merged = EmitBinary(function, block, address, Ir::Opcode::Or, top, inside, type);

        EmitWriteGeneral(block, address, rd, merged, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if (op0 == 7)
    {
        uint32_t rm = (encoding >> 16) & 0x1f;
        uint32_t imms = (encoding >> 10) & 0x3f;
        uint32_t immN = (encoding >> 22) & 1;

        if (((encoding >> 29) & 0x3) != 0) return Arm64Decoder::Result::Unallocated;
        if (((encoding >> 21) & 1) != 0) return Arm64Decoder::Result::Unallocated;
        if (is64Bit != (immN == 1)) return Arm64Decoder::Result::Unallocated;
        if (!is64Bit && (imms & 0x20) != 0) return Arm64Decoder::Result::Unallocated;

        Ir::Value low = EmitReadGeneral(function, block, address, rm, type, false);
        Ir::Value high = EmitReadGeneral(function, block, address, rn, type, false);

        int bits = is64Bit ? 64 : 32;
        Ir::Value lowPart =
            EmitBinary(function, block, address, Ir::Opcode::ShiftRightLogical, low, EmitConstant(function, block, address, imms, type), type);

        Ir::Value result = lowPart;
        if (imms != 0)
        {
            Ir::Value highPart = EmitBinary(function, block, address, Ir::Opcode::ShiftLeft, high,
                                            EmitConstant(function, block, address, (uint64_t)(bits - (int)imms), type), type);
            result = EmitBinary(function, block, address, Ir::Opcode::Or, lowPart, highPart, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static Arm64Decoder::Result DecodeBranch(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    uint32_t op0 = (encoding >> 29) & 0x7;

    if ((encoding & 0x7c000000u) == 0x14000000u)
    {
        bool link = (encoding >> 31) & 1;
        uint64_t offset = SignExtend(encoding & 0x3ffffff, 26) << 2;
        uint64_t target = static_cast<uint64_t>(address) + offset;

        if (link)
        {
            Ir::Value returnAddress = EmitConstant(function, block, address, static_cast<uint64_t>(address) + 4, Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, 30, returnAddress, Ir::ValueType::Int64, false);
        }

        Ir::Instruction& instruction = Append(block, link ? Ir::Opcode::Call : Ir::Opcode::Branch, address, Ir::ValueType::Int64);
        instruction.immediate = target;
        block.successorTaken = static_cast<GuestState::GuestAddress>(target);

        if (link) block.successorFallthrough = static_cast<GuestState::GuestAddress>(static_cast<uint64_t>(address) + 4);

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xff000010u) == 0x54000000u)
    {
        uint64_t offset = SignExtend((encoding >> 5) & 0x7ffff, 19) << 2;
        uint64_t target = static_cast<uint64_t>(address) + offset;

        Ir::Instruction& instruction = Append(block, Ir::Opcode::BranchConditional, address, Ir::ValueType::Int64);
        instruction.immediate = target;
        instruction.condition = static_cast<Ir::Condition>(encoding & 0xf);
        block.successorTaken = static_cast<GuestState::GuestAddress>(target);
        block.successorFallthrough = static_cast<GuestState::GuestAddress>(static_cast<uint64_t>(address) + 4);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x7e000000u) == 0x34000000u)
    {
        bool is64Bit = (encoding >> 31) & 1;
        bool notZero = (encoding >> 24) & 1;
        uint64_t offset = SignExtend((encoding >> 5) & 0x7ffff, 19) << 2;
        uint64_t target = static_cast<uint64_t>(address) + offset;
        Ir::ValueType type = WidthType(is64Bit);

        Ir::Value value = EmitReadGeneral(function, block, address, encoding & 0x1f, type, false);
        Ir::Value zero = EmitConstant(function, block, address, 0, type);
        Ir::Instruction& compare = Append(block, Ir::Opcode::Compare, address, type);
        compare.operands[0] = value;
        compare.operands[1] = zero;
        compare.operandCount = 2;
        compare.predicate = notZero ? Ir::ComparePredicate::NotEqual : Ir::ComparePredicate::Equal;
        compare.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Bool);

        Ir::Instruction& branch = Append(block, Ir::Opcode::BranchConditional, address, Ir::ValueType::Int64);
        branch.operands[0] = compare.result;
        branch.operandCount = 1;
        branch.immediate = target;
        branch.condition = Ir::Condition::NotEqual;
        block.successorTaken = static_cast<GuestState::GuestAddress>(target);
        block.successorFallthrough = static_cast<GuestState::GuestAddress>(static_cast<uint64_t>(address) + 4);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x7e000000u) == 0x36000000u)
    {
        bool notZero = (encoding >> 24) & 1;
        uint32_t bitPosition = (((encoding >> 31) & 1) << 5) | ((encoding >> 19) & 0x1f);
        uint64_t offset = SignExtend((encoding >> 5) & 0x3fff, 14) << 2;
        uint64_t target = static_cast<uint64_t>(address) + offset;
        Ir::ValueType type = bitPosition >= 32 ? Ir::ValueType::Int64 : Ir::ValueType::Int32;

        Ir::Value value = EmitReadGeneral(function, block, address, encoding & 0x1f, type, false);
        Ir::Value mask = EmitConstant(function, block, address, 1ull << bitPosition, type);
        Ir::Value masked = EmitBinary(function, block, address, Ir::Opcode::And, value, mask, type);
        Ir::Value zero = EmitConstant(function, block, address, 0, type);

        Ir::Instruction& compare = Append(block, Ir::Opcode::Compare, address, type);
        compare.operands[0] = masked;
        compare.operands[1] = zero;
        compare.operandCount = 2;
        compare.predicate = notZero ? Ir::ComparePredicate::NotEqual : Ir::ComparePredicate::Equal;
        compare.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Bool);

        Ir::Instruction& branch = Append(block, Ir::Opcode::BranchConditional, address, Ir::ValueType::Int64);
        branch.operands[0] = compare.result;
        branch.operandCount = 1;
        branch.immediate = target;
        branch.condition = Ir::Condition::NotEqual;
        block.successorTaken = static_cast<GuestState::GuestAddress>(target);
        block.successorFallthrough = static_cast<GuestState::GuestAddress>(static_cast<uint64_t>(address) + 4);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xfffffc1fu) == 0xd65f0000u)
    {
        Ir::Value target = EmitReadGeneral(function, block, address, (encoding >> 5) & 0x1f, Ir::ValueType::Int64, false);

        Ir::Instruction& instruction = Append(block, Ir::Opcode::Return, address, Ir::ValueType::Int64);
        instruction.operands[0] = target;
        instruction.operandCount = 1;
        block.endsWithIndirectBranch = true;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xffdffc1fu) == 0xd61f0000u)
    {
        bool link = (encoding >> 21) & 1;
        if (link)
        {
            Ir::Value returnAddress = EmitConstant(function, block, address, static_cast<uint64_t>(address) + 4, Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, 30, returnAddress, Ir::ValueType::Int64, false);
        }

        Ir::Value target = EmitReadGeneral(function, block, address, (encoding >> 5) & 0x1f, Ir::ValueType::Int64, false);

        Ir::Instruction& instruction = Append(block, Ir::Opcode::BranchIndirect, address, Ir::ValueType::Int64);
        instruction.operands[0] = target;
        instruction.operandCount = 1;
        block.endsWithIndirectBranch = true;

        if (link) block.successorFallthrough = static_cast<GuestState::GuestAddress>(static_cast<uint64_t>(address) + 4);

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xfffff01fu) == 0xd503201fu)
    {
        uint32_t hint = (encoding >> 5) & 0x7f;

        Ir::Opcode opcode = Ir::Opcode::Hint;
        if (hint == 8 || hint == 24 || hint == 25) opcode = Ir::Opcode::PointerAuthSign;
        if (hint == 12 || hint == 28 || hint == 29) opcode = Ir::Opcode::PointerAuthAuthenticate;

        Append(block, opcode, address, Ir::ValueType::Int64).immediate = hint;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xffd00000u) == 0xd5100000u)
    {
        bool read = (encoding >> 21) & 1;
        uint32_t identifier = (encoding >> 5) & 0x7fff;
        uint32_t rt = encoding & 0x1f;

        if (identifier == 0x5A10)
        {
            if (read)
            {
                Ir::Instruction& flags = Append(block, Ir::Opcode::ReadFlags, address, Ir::ValueType::Int64);
                flags.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
                EmitWriteGeneral(block, address, rt, flags.result, Ir::ValueType::Int64, false);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value written = EmitReadGeneral(function, block, address, rt, Ir::ValueType::Int64, false);

            Ir::Instruction& flags = Append(block, Ir::Opcode::WriteFlags, address, Ir::ValueType::Int64);
            flags.operands[0] = written;
            flags.operandCount = 1;
            return Arm64Decoder::Result::Ok;
        }

        Ir::SystemRegister systemRegister = Ir::SystemRegister::Invalid;
        if (identifier == 0x5E82) systemRegister = Ir::SystemRegister::ThreadPointer;
        if (identifier == 0x5E83) systemRegister = Ir::SystemRegister::ThreadPointerReadOnly;
        if (identifier == 0x5A20) systemRegister = Ir::SystemRegister::FloatingPointControl;
        if (identifier == 0x5A21) systemRegister = Ir::SystemRegister::FloatingPointStatus;
        if (identifier == 0x4000) systemRegister = Ir::SystemRegister::MainIdRegister;
        if (identifier == 0x5801) systemRegister = Ir::SystemRegister::CacheTypeRegister;
        if (identifier == 0x5807) systemRegister = Ir::SystemRegister::CacheZeroIdRegister;
        if (identifier == 0x4020) systemRegister = Ir::SystemRegister::ProcessorFeatureRegister0;
        if (identifier == 0x4021) systemRegister = Ir::SystemRegister::ProcessorFeatureRegister1;
        if (identifier == 0x4024) systemRegister = Ir::SystemRegister::VectorFeatureRegister0;
        if (identifier == 0x4030) systemRegister = Ir::SystemRegister::IsaFeatureRegister0;
        if (identifier == 0x4031) systemRegister = Ir::SystemRegister::IsaFeatureRegister1;
        if (identifier == 0x5F00) systemRegister = Ir::SystemRegister::CounterFrequency;
        if (identifier == 0x5F01 || identifier == 0x5F02 || identifier == 0x5F05 || identifier == 0x5F06)
            systemRegister = Ir::SystemRegister::CounterVirtual;
        if (systemRegister == Ir::SystemRegister::Invalid) return Arm64Decoder::Result::Unimplemented;

        uint64_t constantValue = 0;
        if (!read && Ir::DescribeSystemRegister(systemRegister, constantValue)) return Arm64Decoder::Result::Ok;

        if (read)
        {
            Ir::Instruction& instruction = Append(block, Ir::Opcode::ReadSystemRegister, address, Ir::ValueType::Int64);
            instruction.systemRegister = systemRegister;
            instruction.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, rt, instruction.result, Ir::ValueType::Int64, false);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value value = EmitReadGeneral(function, block, address, rt, Ir::ValueType::Int64, false);

        Ir::Instruction& instruction = Append(block, Ir::Opcode::WriteSystemRegister, address, Ir::ValueType::Int64);
        instruction.systemRegister = systemRegister;
        instruction.operands[0] = value;
        instruction.operandCount = 1;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xffffff1fu) == 0xd503211fu || (encoding & 0xfffffc1fu) == 0xd503241fu)
    {
        Append(block, Ir::Opcode::Hint, address, Ir::ValueType::Int64).immediate = (encoding >> 5) & 0x7f;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xfffff0ffu) == 0xd503305fu)
    {
        Append(block, Ir::Opcode::ClearExclusive, address, Ir::ValueType::Int64);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xfffff09fu) == 0xd503309fu)
    {
        Ir::Instruction& instruction = Append(block, Ir::Opcode::MemoryBarrier, address, Ir::ValueType::Int64);
        instruction.order = Ir::MemoryOrder::SequentiallyConsistent;
        instruction.immediate = (encoding >> 8) & 0xf;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xfff80000u) == 0xd5080000u)
    {
        uint32_t op1 = (encoding >> 16) & 0x7;
        uint32_t crn = (encoding >> 12) & 0xf;
        uint32_t crm = (encoding >> 8) & 0xf;
        uint32_t op2 = (encoding >> 5) & 0x7;
        uint32_t rt = encoding & 0x1f;

        if (op1 != 3 || crn != 7 || op2 != 1) return Arm64Decoder::Result::Unimplemented;

        if (crm == 5)
        {
            Ir::Value line = EmitReadGeneral(function, block, address, rt, Ir::ValueType::Int64, false);

            Ir::Instruction& instruction = Append(block, Ir::Opcode::InvalidateInstructionCache, address, Ir::ValueType::Int64);
            instruction.operands[0] = line;
            instruction.operandCount = 1;
            return Arm64Decoder::Result::Ok;
        }

        if (crm == 10 || crm == 11 || crm == 12 || crm == 13 || crm == 14)
        {
            Append(block, Ir::Opcode::Hint, address, Ir::ValueType::Int64).immediate = 0;
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unimplemented;
    }

    (void)op0;
    return Arm64Decoder::Result::Unimplemented;
}

static Arm64Decoder::Result DecodeDataProcessingRegister(Ir::Function& function, Ir::Block& block, uint32_t encoding,
                                                         GuestState::GuestAddress address)
{
    bool is64Bit = (encoding >> 31) & 1;
    Ir::ValueType type = WidthType(is64Bit);
    uint32_t rd = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;
    uint32_t rm = (encoding >> 16) & 0x1f;

    if ((encoding & 0x1f000000u) == 0x0a000000u)
    {
        uint32_t opc = (encoding >> 29) & 0x3;
        uint32_t shiftType = (encoding >> 22) & 0x3;
        uint32_t amount = (encoding >> 10) & 0x3f;
        bool negate = (encoding >> 21) & 1;

        if (!is64Bit && (amount & 0x20) != 0) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitShiftedRegister(function, block, address, rm, shiftType, amount, type);
        if (negate) right = EmitUnary(function, block, address, Ir::Opcode::Not, right, type);

        Ir::Opcode opcode = Ir::Opcode::And;
        if (opc == 1) opcode = Ir::Opcode::Or;
        if (opc == 2) opcode = Ir::Opcode::Xor;

        Ir::Value result = EmitBinary(function, block, address, opcode, left, right, type);

        if (opc == 3)
        {
            Ir::Value zero = EmitConstant(function, block, address, 0, Ir::ValueType::Int32);
            EmitFlagsFrom(function, block, address, Ir::Opcode::ComputeNzcvLogical, result, zero, zero, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x1fe00000u) == 0x0b200000u)
    {
        bool subtract = (encoding >> 30) & 1;
        bool setFlags = (encoding >> 29) & 1;
        uint32_t option = (encoding >> 13) & 0x7;
        uint32_t amount = (encoding >> 10) & 0x7;

        if (amount > 4) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, !setFlags);
        Ir::Value raw = EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

        static const int EXTEND_BITS[] = {8, 16, 32, 64, 8, 16, 32, 64};
        bool signedExtend = (option & 0x4) != 0;
        int width = EXTEND_BITS[option];

        Ir::Value extended = raw;
        if (width < 64)
        {
            Ir::Instruction& instruction = Append(block, signedExtend ? Ir::Opcode::ExtendSigned : Ir::Opcode::ExtendUnsigned, address, type);
            instruction.operands[0] = raw;
            instruction.operandCount = 1;
            instruction.immediate = (uint64_t)width;
            instruction.result = Ir::MakeValue(AllocateValue(function), type);
            extended = instruction.result;
        }

        Ir::Value right = extended;
        if (amount != 0)
        {
            right = EmitBinary(function, block, address, Ir::Opcode::ShiftLeft, extended, EmitConstant(function, block, address, amount, type), type);
        }

        Ir::Value result = EmitBinary(function, block, address, subtract ? Ir::Opcode::Subtract : Ir::Opcode::Add, left, right, type);

        if (setFlags)
        {
            Ir::Value carry = EmitConstant(function, block, address, subtract ? 1 : 0, Ir::ValueType::Int32);
            Ir::Opcode nzcv = subtract ? Ir::Opcode::ComputeNzcvSubtract : Ir::Opcode::ComputeNzcvAdd;
            EmitFlagsFrom(function, block, address, nzcv, left, right, carry, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, !setFlags);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x1f200000u) == 0x0b000000u)
    {
        bool subtract = (encoding >> 30) & 1;
        bool setFlags = (encoding >> 29) & 1;
        uint32_t shiftType = (encoding >> 22) & 0x3;
        uint32_t amount = (encoding >> 10) & 0x3f;
        if (shiftType == 3) return Arm64Decoder::Result::Unallocated;
        if (!is64Bit && (amount & 0x20) != 0) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitShiftedRegister(function, block, address, rm, shiftType, amount, type);
        Ir::Value result = EmitBinary(function, block, address, subtract ? Ir::Opcode::Subtract : Ir::Opcode::Add, left, right, type);

        if (setFlags)
        {
            Ir::Value carry = EmitConstant(function, block, address, subtract ? 1 : 0, Ir::ValueType::Int32);
            Ir::Opcode nzcv = subtract ? Ir::Opcode::ComputeNzcvSubtract : Ir::Opcode::ComputeNzcvAdd;
            EmitFlagsFrom(function, block, address, nzcv, left, right, carry, type);
        }

        EmitWriteGeneral(block, address, rd, result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x1fe00000u) == 0x1a000000u)
    {
        bool subtract = (encoding >> 30) & 1;
        bool setFlags = (encoding >> 29) & 1;

        if (((encoding >> 10) & 0x3f) != 0) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitReadGeneral(function, block, address, rm, type, false);
        Ir::Instruction& flags = Append(block, Ir::Opcode::ReadFlags, address, Ir::ValueType::Int32);
        flags.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Instruction& compute = Append(block, subtract ? Ir::Opcode::SubtractWithBorrow : Ir::Opcode::AddWithCarry, address, type);
        compute.operands[0] = left;
        compute.operands[1] = right;
        compute.operands[2] = flags.result;
        compute.operandCount = 3;
        compute.result = Ir::MakeValue(AllocateValue(function), type);

        if (setFlags)
        {
            EmitFlagsFrom(function, block, address, subtract ? Ir::Opcode::ComputeNzcvSubtract : Ir::Opcode::ComputeNzcvAdd, left, right,
                          flags.result, type);
        }

        EmitWriteGeneral(block, address, rd, compute.result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3fe00410u) == 0x3a400000u)
    {
        bool subtract = (encoding >> 30) & 1;
        bool immediateForm = (encoding >> 11) & 1;
        Ir::Condition condition = static_cast<Ir::Condition>((encoding >> 12) & 0xf);
        uint64_t flags = (uint64_t)(encoding & 0xf) << 28;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = immediateForm ? EmitConstant(function, block, address, (encoding >> 16) & 0x1f, type)
                                        : EmitReadGeneral(function, block, address, rm, type, false);

        Ir::Value carry = EmitConstant(function, block, address, subtract ? 1 : 0, Ir::ValueType::Int32);
        Ir::Opcode nzcv = subtract ? Ir::Opcode::ComputeNzcvSubtract : Ir::Opcode::ComputeNzcvAdd;

        Ir::Instruction& compute = Append(block, nzcv, address, type);
        compute.operands[0] = left;
        compute.operands[1] = right;
        compute.operands[2] = carry;
        compute.operandCount = 3;
        compute.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Value computed = compute.result;
        Ir::Value fallback = EmitConstant(function, block, address, flags, Ir::ValueType::Int32);

        Ir::Instruction& select = Append(block, Ir::Opcode::Select, address, Ir::ValueType::Int32);
        select.operands[0] = computed;
        select.operands[1] = fallback;
        select.operandCount = 2;
        select.condition = condition;
        select.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Instruction& write = Append(block, Ir::Opcode::WriteFlags, address, Ir::ValueType::Int32);
        write.operands[0] = select.result;
        write.operandCount = 1;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x1fe00000u) == 0x1a800000u)
    {
        uint32_t op2 = (encoding >> 10) & 0x3;
        bool invert = (encoding >> 30) & 1;
        Ir::Condition condition = static_cast<Ir::Condition>((encoding >> 12) & 0xf);

        if (((encoding >> 29) & 1) != 0) return Arm64Decoder::Result::Unallocated;
        if (op2 > 1) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitReadGeneral(function, block, address, rm, type, false);

        if (invert) right = EmitUnary(function, block, address, Ir::Opcode::Not, right, type);
        if (op2 == 1) right = EmitBinary(function, block, address, Ir::Opcode::Add, right, EmitConstant(function, block, address, 1, type), type);

        Ir::Instruction& select = Append(block, Ir::Opcode::Select, address, type);
        select.operands[0] = left;
        select.operands[1] = right;
        select.operandCount = 2;
        select.condition = condition;
        select.result = Ir::MakeValue(AllocateValue(function), type);

        EmitWriteGeneral(block, address, rd, select.result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x5fe00000u) == 0x5ac00000u)
    {
        uint32_t opcode2 = (encoding >> 10) & 0x3f;

        if (((encoding >> 29) & 1) != 0) return Arm64Decoder::Result::Unallocated;
        if (((encoding >> 16) & 0x1f) != 0) return Arm64Decoder::Result::Unallocated;
        if (opcode2 > 5) return Arm64Decoder::Result::Unallocated;
        if (!is64Bit && opcode2 == 3) return Arm64Decoder::Result::Unallocated;

        Ir::Value operand = EmitReadGeneral(function, block, address, rn, type, false);

        Ir::Opcode opcode = Ir::Opcode::Invalid;
        if (opcode2 == 0) opcode = Ir::Opcode::ReverseBits;
        if (opcode2 == 4) opcode = Ir::Opcode::CountLeadingZeros;
        if (opcode2 == 1 || opcode2 == 2 || opcode2 == 3) opcode = Ir::Opcode::ReverseBytes;
        if (opcode == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;

        Ir::Instruction& instruction = Append(block, opcode, address, type);
        instruction.operands[0] = operand;
        instruction.operandCount = 1;
        instruction.result = Ir::MakeValue(AllocateValue(function), type);

        if (opcode == Ir::Opcode::ReverseBytes)
        {
            if (opcode2 == 1) instruction.laneWidth = Ir::LaneWidth::Half;
            if (opcode2 == 2) instruction.laneWidth = Ir::LaneWidth::Word;
            if (opcode2 == 3) instruction.laneWidth = Ir::LaneWidth::Double;
        }

        EmitWriteGeneral(block, address, rd, instruction.result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x5fe00000u) == 0x1ac00000u)
    {
        uint32_t opcode2 = (encoding >> 10) & 0x3f;
        Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
        Ir::Value right = EmitReadGeneral(function, block, address, rm, type, false);

        Ir::Opcode opcode = Ir::Opcode::Invalid;
        if (opcode2 == 2) opcode = Ir::Opcode::DivideUnsigned;
        if (opcode2 == 3) opcode = Ir::Opcode::DivideSigned;
        if (opcode2 == 8) opcode = Ir::Opcode::ShiftLeft;
        if (opcode2 == 9) opcode = Ir::Opcode::ShiftRightLogical;
        if (opcode2 == 10) opcode = Ir::Opcode::ShiftRightArithmetic;
        if (opcode2 == 11) opcode = Ir::Opcode::RotateRight;
        if (opcode == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;

        Ir::Value result = EmitBinary(function, block, address, opcode, left, right, type);
        EmitWriteGeneral(block, address, rd, result, type, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x1f000000u) == 0x1b000000u)
    {
        uint32_t op31 = (encoding >> 21) & 0x7;
        uint32_t ra = (encoding >> 10) & 0x1f;
        bool subtract = (encoding >> 15) & 1;

        if (((encoding >> 29) & 0x3) != 0) return Arm64Decoder::Result::Unallocated;
        if (!is64Bit && op31 != 0) return Arm64Decoder::Result::Unallocated;
        if (op31 == 3 || op31 == 4 || op31 == 7) return Arm64Decoder::Result::Unallocated;
        if ((op31 == 2 || op31 == 6) && (subtract || ra != 31)) return Arm64Decoder::Result::Unallocated;

        if (op31 == 2 || op31 == 6)
        {
            Ir::Value left = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, false);
            Ir::Value right = EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);
            Ir::Value result = EmitBinary(function, block, address, op31 == 2 ? Ir::Opcode::MultiplyHighSigned : Ir::Opcode::MultiplyHighUnsigned,
                                          left, right, Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, rd, result, Ir::ValueType::Int64, false);
            return Arm64Decoder::Result::Ok;
        }

        if (op31 == 0)
        {
            Ir::Value left = EmitReadGeneral(function, block, address, rn, type, false);
            Ir::Value right = EmitReadGeneral(function, block, address, rm, type, false);
            Ir::Value product = EmitBinary(function, block, address, Ir::Opcode::Multiply, left, right, type);
            Ir::Value addend = EmitReadGeneral(function, block, address, ra, type, false);
            Ir::Value result = EmitBinary(function, block, address, subtract ? Ir::Opcode::Subtract : Ir::Opcode::Add, addend, product, type);
            EmitWriteGeneral(block, address, rd, result, type, false);
            return Arm64Decoder::Result::Ok;
        }

        if (op31 == 1 || op31 == 5)
        {
            Ir::Opcode widen = op31 == 1 ? Ir::Opcode::ExtendSigned : Ir::Opcode::ExtendUnsigned;
            Ir::Value narrowLeft = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int32, false);
            Ir::Value narrowRight = EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int32, false);

            Ir::Instruction& extendLeft = Append(block, widen, address, Ir::ValueType::Int64);
            extendLeft.operands[0] = narrowLeft;
            extendLeft.operandCount = 1;
            extendLeft.immediate = 32;
            extendLeft.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

            Ir::Instruction& extendRight = Append(block, widen, address, Ir::ValueType::Int64);
            extendRight.operands[0] = narrowRight;
            extendRight.operandCount = 1;
            extendRight.immediate = 32;
            extendRight.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

            Ir::Value product =
                EmitBinary(function, block, address, Ir::Opcode::Multiply, extendLeft.result, extendRight.result, Ir::ValueType::Int64);
            Ir::Value addend = EmitReadGeneral(function, block, address, ra, Ir::ValueType::Int64, false);
            Ir::Value result = EmitBinary(function, block, address, subtract ? Ir::Opcode::Subtract : Ir::Opcode::Add, addend, product,
                                          Ir::ValueType::Int64);

            EmitWriteGeneral(block, address, rd, result, Ir::ValueType::Int64, false);
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unimplemented;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static Ir::ValueType AccessType(uint32_t size)
{
    if (size == 0) return Ir::ValueType::Int8;
    if (size == 1) return Ir::ValueType::Int16;
    if (size == 2) return Ir::ValueType::Int32;
    return Ir::ValueType::Int64;
}

static Ir::Value EmitLoad(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, Ir::Value pointer, Ir::ValueType accessType,
                          Ir::MemoryOrder order)
{
    Ir::Instruction& instruction = Append(block, Ir::Opcode::Load, address, accessType);
    instruction.operands[0] = pointer;
    instruction.operandCount = 1;
    instruction.order = order;
    instruction.result = Ir::MakeValue(AllocateValue(function), accessType);
    return instruction.result;
}

static void EmitStore(Ir::Block& block, GuestState::GuestAddress address, Ir::Value pointer, Ir::Value value, Ir::ValueType accessType,
                      Ir::MemoryOrder order)
{
    Ir::Instruction& instruction = Append(block, Ir::Opcode::Store, address, accessType);
    instruction.operands[0] = pointer;
    instruction.operands[1] = value;
    instruction.operandCount = 2;
    instruction.order = order;
}

static Ir::Value EmitAddress(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint32_t base, int64_t offset)
{
    Ir::Value pointer = EmitReadGeneral(function, block, address, base, Ir::ValueType::Int64, true);
    if (offset == 0) return pointer;

    Ir::Value displacement = EmitConstant(function, block, address, (uint64_t)offset, Ir::ValueType::Int64);
    return EmitBinary(function, block, address, Ir::Opcode::Add, pointer, displacement, Ir::ValueType::Int64);
}

static void EmitExtendedWrite(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint32_t rt, Ir::Value loaded,
                              uint32_t size, uint32_t opc, bool is64BitResult)
{
    Ir::ValueType destination = is64BitResult ? Ir::ValueType::Int64 : Ir::ValueType::Int32;

    if (opc >= 2)
    {
        Ir::Instruction& extend = Append(block, Ir::Opcode::ExtendSigned, address, destination);
        extend.operands[0] = loaded;
        extend.operandCount = 1;
        extend.immediate = (uint64_t)(8 << size);
        extend.result = Ir::MakeValue(AllocateValue(function), destination);
        EmitWriteGeneral(block, address, rt, extend.result, destination, false);
        return;
    }

    EmitWriteGeneral(block, address, rt, loaded, destination, false);
}

static Ir::Value EmitReadVector(Ir::Function& function, Ir::Block& block, GuestState::GuestAddress address, uint32_t number, Ir::ValueType type)
{
    Ir::Instruction& instruction = Append(block, Ir::Opcode::ReadVector, address, type);
    instruction.immediate = number;
    instruction.result = Ir::MakeValue(AllocateValue(function), type);
    return instruction.result;
}

static void EmitWriteVector(Ir::Block& block, GuestState::GuestAddress address, uint32_t number, Ir::Value value, Ir::ValueType type)
{
    Ir::Instruction& instruction = Append(block, Ir::Opcode::WriteVector, address, type);
    instruction.immediate = number;
    instruction.operands[0] = value;
    instruction.operandCount = 1;
}

static Ir::LaneWidth LaneWidthFor(uint32_t size)
{
    if (size == 0) return Ir::LaneWidth::Byte;
    if (size == 1) return Ir::LaneWidth::Half;
    if (size == 2) return Ir::LaneWidth::Word;
    return Ir::LaneWidth::Double;
}

static uint64_t ExpandFloatImmediate(uint32_t immediate, bool isDouble);

static uint64_t ExpandSimdImmediate(uint32_t op, uint32_t cmode, uint64_t abcdefgh)
{
    uint64_t immediate = 0;

    switch (cmode >> 1)
    {
        case 0: immediate = abcdefgh; break;
        case 1: immediate = abcdefgh << 8; break;
        case 2: immediate = abcdefgh << 16; break;
        case 3: immediate = abcdefgh << 24; break;
        case 4: immediate = abcdefgh; break;
        case 5: immediate = abcdefgh << 8; break;
        case 6: immediate = (cmode & 1) ? ((abcdefgh << 16) | 0xffff) : ((abcdefgh << 8) | 0xff); break;
        default: immediate = abcdefgh; break;
    }

    if ((cmode >> 1) <= 3) immediate |= immediate << 32;
    else if ((cmode >> 1) == 4 || (cmode >> 1) == 5) immediate |= immediate << 16;
    else if ((cmode >> 1) == 6) immediate |= immediate << 32;

    if ((cmode >> 1) <= 5 && (cmode >> 1) >= 4) immediate |= immediate << 32;

    if (cmode == 14 && op == 0)
    {
        immediate = 0;
        for (int index = 0; index < 8; ++index) immediate |= ((abcdefgh >> index) & 1) ? (0xffull << (index * 8)) : 0;
    }

    if (cmode == 14 && op == 1)
    {
        immediate = 0;
        for (int index = 0; index < 8; ++index) immediate |= ((abcdefgh >> index) & 1) ? (0xffull << (index * 8)) : 0;
    }

    return immediate;
}

static Arm64Decoder::Result DecodeVectorDataProcessing(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    uint32_t rd = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;
    uint32_t rm = (encoding >> 16) & 0x1f;
    bool wide = (encoding >> 30) & 1;
    Ir::ValueType vectorType = Ir::ValueType::Vector128;

    if ((encoding & 0xffe0fc00u) == 0x5e000400u)
    {
        uint32_t imm5 = (encoding >> 16) & 0x1f;

        uint32_t size = 0;
        if ((imm5 & 1) != 0) size = 0;
        else if ((imm5 & 2) != 0) size = 1;
        else if ((imm5 & 4) != 0) size = 2;
        else if ((imm5 & 8) != 0) size = 3;
        else return Arm64Decoder::Result::Unallocated;

        Ir::ValueType elementType = Ir::ValueType::Int8;
        if (size == 1) elementType = Ir::ValueType::Int16;
        if (size == 2) elementType = Ir::ValueType::Int32;
        if (size == 3) elementType = Ir::ValueType::Int64;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = source;
        extract.operandCount = 1;
        extract.laneWidth = LaneWidthFor(size);
        extract.laneIndex = (uint8_t)(imm5 >> (size + 1));
        extract.result = Ir::MakeValue(AllocateValue(function), elementType);

        EmitWriteVector(block, address, rd, extract.result, elementType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xdf80f400u) == 0x5f809000u)
    {
        bool doubleLanes = ((encoding >> 22) & 1) != 0;
        uint32_t high = (encoding >> 11) & 1;
        uint32_t low = (encoding >> 21) & 1;
        uint32_t extra = (encoding >> 20) & 1;

        uint32_t element = ((encoding >> 16) & 0xf) | (extra << 4);
        uint32_t index = doubleLanes ? high : ((high << 1) | low);

        if (doubleLanes && low != 0) return Arm64Decoder::Result::Unallocated;

        Ir::ValueType scalarType = doubleLanes ? Ir::ValueType::Float64 : Ir::ValueType::Float32;
        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;

        Ir::Value source = EmitReadVector(function, block, address, element, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = source;
        extract.operandCount = 1;
        extract.laneWidth = laneWidth;
        extract.laneIndex = (uint8_t)index;
        extract.result = Ir::MakeValue(AllocateValue(function), scalarType);
        Ir::Value chosen = extract.result;

        Ir::Value left = EmitReadVector(function, block, address, rn, scalarType);

        Ir::Instruction& product = Append(block, Ir::Opcode::FloatMultiply, address, scalarType);
        product.operands[0] = left;
        product.operands[1] = chosen;
        product.operandCount = 2;
        product.operandType = scalarType;
        product.updatesFloatingPointStatus = true;
        product.result = Ir::MakeValue(AllocateValue(function), scalarType);

        EmitWriteVector(block, address, rd, product.result, scalarType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xffbffc00u) == 0x7e30d800u)
    {
        bool doubleLanes = ((encoding >> 22) & 1) != 0;
        Ir::ValueType scalarType = doubleLanes ? Ir::ValueType::Float64 : Ir::ValueType::Float32;
        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& lower = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        lower.operands[0] = source;
        lower.operandCount = 1;
        lower.laneWidth = laneWidth;
        lower.laneIndex = 0;
        lower.result = Ir::MakeValue(AllocateValue(function), scalarType);
        Ir::Value first = lower.result;

        Ir::Instruction& upper = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        upper.operands[0] = source;
        upper.operandCount = 1;
        upper.laneWidth = laneWidth;
        upper.laneIndex = 1;
        upper.result = Ir::MakeValue(AllocateValue(function), scalarType);
        Ir::Value second = upper.result;

        Ir::Instruction& total = Append(block, Ir::Opcode::FloatAdd, address, scalarType);
        total.operands[0] = first;
        total.operands[1] = second;
        total.operandCount = 2;
        total.operandType = scalarType;
        total.updatesFloatingPointStatus = true;
        total.result = Ir::MakeValue(AllocateValue(function), scalarType);

        EmitWriteVector(block, address, rd, total.result, scalarType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xdf3e0c00u) == 0x5e300800u)
    {
        uint32_t pairwiseOpcode = (encoding >> 12) & 0x1f;
        uint32_t pairwiseSize = (encoding >> 22) & 0x3;
        bool unsignedPairwise = ((encoding >> 29) & 1) != 0;

        if (pairwiseOpcode == 0x1b && !unsignedPairwise && pairwiseSize == 3)
        {
            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& low = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
            low.operands[0] = source;
            low.operandCount = 1;
            low.laneWidth = Ir::LaneWidth::Double;
            low.laneIndex = 0;
            low.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

            Ir::Instruction& high = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
            high.operands[0] = source;
            high.operandCount = 1;
            high.laneWidth = Ir::LaneWidth::Double;
            high.laneIndex = 1;
            high.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

            Ir::Instruction& sum = Append(block, Ir::Opcode::Add, address, Ir::ValueType::Int64);
            sum.operands[0] = low.result;
            sum.operands[1] = high.result;
            sum.operandCount = 2;
            sum.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

            EmitWriteVector(block, address, rd, sum.result, Ir::ValueType::Int64);
            return Arm64Decoder::Result::Ok;
        }
    }

    if ((encoding & 0xdf3e0c00u) == 0x5e200800u)
    {
        uint32_t scalarOpcode = (encoding >> 12) & 0x1f;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool doublePrecision = ((encoding >> 22) & 1) != 0;
        bool sourceIsFloat = ((encoding >> 23) & 1) != 0;

        Ir::ValueType scalarInteger = doublePrecision ? Ir::ValueType::Int64 : Ir::ValueType::Int32;
        Ir::ValueType scalarFloat = doublePrecision ? Ir::ValueType::Float64 : Ir::ValueType::Float32;

        if (scalarOpcode == 0x1d && !sourceIsFloat)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, scalarInteger);
            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvert, address, scalarFloat);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = unsignedForm ? 3 : 2;
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), scalarFloat);
            EmitWriteVector(block, address, rd, convert.result, scalarFloat);
            return Arm64Decoder::Result::Ok;
        }

        if (scalarOpcode == 0x1b && sourceIsFloat)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, scalarFloat);
            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvert, address, scalarInteger);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = unsignedForm ? 5 : 4;
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), scalarInteger);
            EmitWriteVector(block, address, rd, convert.result, scalarInteger);
            return Arm64Decoder::Result::Ok;
        }
    }

    if ((encoding & 0x9ff80c00u) == 0x0f000400u)
    {
        uint32_t op = (encoding >> 29) & 1;
        uint32_t cmode = (encoding >> 12) & 0xf;
        uint64_t abcdefgh = (((encoding >> 16) & 0x7) << 5) | ((encoding >> 5) & 0x1f);

        bool merging = cmode < 14 && (cmode & 1) != 0;
        uint64_t immediate = 0;

        if (cmode == 15)
        {
            if (op == 1 && !wide) return Arm64Decoder::Result::Unallocated;

            if (op == 1)
            {
                immediate = ExpandFloatImmediate((uint32_t)abcdefgh, true);
            }
            else
            {
                uint64_t single = ExpandFloatImmediate((uint32_t)abcdefgh, false) & 0xffffffffull;
                immediate = single | (single << 32);
            }
        }
        else if (cmode == 14)
        {
            if (op == 0)
            {
                uint64_t byte = abcdefgh & 0xff;
                for (int index = 0; index < 8; ++index) immediate |= byte << (index * 8);
            }
            else
            {
                for (int index = 0; index < 8; ++index) immediate |= ((abcdefgh >> index) & 1) ? (0xffull << (index * 8)) : 0;
            }
        }
        else
        {
            immediate = ExpandSimdImmediate(0, cmode, abcdefgh);
            if (op == 1 && !merging) immediate = ~immediate;
        }

        Ir::Value constant = EmitConstant(function, block, address, immediate, Ir::ValueType::Int64);
        Ir::Instruction& duplicate = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
        duplicate.operands[0] = constant;
        duplicate.operandCount = 1;
        duplicate.laneWidth = Ir::LaneWidth::Double;
        duplicate.laneCount = wide ? 2 : 1;
        duplicate.result = Ir::MakeValue(AllocateValue(function), vectorType);

        Ir::Value produced = duplicate.result;

        if (merging)
        {
            Ir::Value existing = EmitReadVector(function, block, address, rd, vectorType);

            if (op == 1)
            {
                Ir::Value complement = EmitConstant(function, block, address, ~immediate, Ir::ValueType::Int64);

                Ir::Instruction& inverted = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
                inverted.operands[0] = complement;
                inverted.operandCount = 1;
                inverted.laneWidth = Ir::LaneWidth::Double;
                inverted.laneCount = wide ? 2 : 1;
                inverted.result = Ir::MakeValue(AllocateValue(function), vectorType);
                produced = inverted.result;
            }

            Ir::Instruction& combined = Append(block, op == 1 ? Ir::Opcode::VectorAnd : Ir::Opcode::VectorOr, address, vectorType);
            combined.operands[0] = existing;
            combined.operands[1] = produced;
            combined.operandCount = 2;
            combined.laneWidth = Ir::LaneWidth::Byte;
            combined.laneCount = wide ? 16 : 8;
            combined.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = combined.result;
        }

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f800400u) == 0x0f000400u && ((encoding >> 19) & 0xf) != 0)
    {
        uint32_t immh = (encoding >> 19) & 0xf;
        uint32_t immb = (encoding >> 16) & 0x7;
        uint32_t opcode = (encoding >> 11) & 0x1f;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        uint32_t sizeIndex = 3;
        if (immh == 1) sizeIndex = 0;
        else if (immh < 4) sizeIndex = 1;
        else if (immh < 8) sizeIndex = 2;

        uint32_t elementBits = 8u << sizeIndex;
        uint32_t encoded = (immh << 3) | immb;

        Ir::LaneWidth narrowWidth = LaneWidthFor(sizeIndex);
        int narrowBytes = Ir::LaneSizeInBytes(narrowWidth);

        if (opcode == 0x14)
        {
            if (sizeIndex == 3) return Arm64Decoder::Result::Unallocated;

            int amount = (int)encoded - (int)elementBits;
            Ir::LaneWidth wideWidth = LaneWidthFor(sizeIndex + 1);
            uint8_t lanes = (uint8_t)(8 / narrowBytes);

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& widen = Append(block, Ir::Opcode::VectorWiden, address, vectorType);
            widen.operands[0] = source;
            widen.operandCount = 1;
            widen.laneWidth = narrowWidth;
            widen.laneCount = lanes;
            widen.immediate = (unsignedForm ? 0u : 1u) | (wide ? 2u : 0u);
            widen.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value widened = widen.result;

            if (amount == 0)
            {
                EmitWriteVector(block, address, rd, widened, vectorType);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value spread = EmitConstant(function, block, address, (uint64_t)(uint8_t)(int8_t)amount, Ir::ValueType::Int64);

            Ir::Instruction& amounts = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            amounts.operands[0] = spread;
            amounts.operandCount = 1;
            amounts.laneWidth = wideWidth;
            amounts.laneCount = lanes;
            amounts.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value counts = amounts.result;

            Ir::Instruction& shifted = Append(block, Ir::Opcode::VectorShiftLeftUnsigned, address, vectorType);
            shifted.operands[0] = widened;
            shifted.operands[1] = counts;
            shifted.operandCount = 2;
            shifted.laneWidth = wideWidth;
            shifted.laneCount = lanes;
            shifted.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, shifted.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x10)
        {
            if (sizeIndex == 3 || unsignedForm) return Arm64Decoder::Result::Unimplemented;

            int amount = (int)(2 * elementBits) - (int)encoded;
            Ir::LaneWidth wideWidth = LaneWidthFor(sizeIndex + 1);
            uint8_t lanes = (uint8_t)(8 / narrowBytes);

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value spread = EmitConstant(function, block, address, (uint64_t)(uint8_t)(int8_t)(-amount), Ir::ValueType::Int64);

            Ir::Instruction& amounts = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            amounts.operands[0] = spread;
            amounts.operandCount = 1;
            amounts.laneWidth = wideWidth;
            amounts.laneCount = lanes;
            amounts.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value counts = amounts.result;

            Ir::Instruction& shifted = Append(block, Ir::Opcode::VectorShiftLeftUnsigned, address, vectorType);
            shifted.operands[0] = source;
            shifted.operands[1] = counts;
            shifted.operandCount = 2;
            shifted.laneWidth = wideWidth;
            shifted.laneCount = lanes;
            shifted.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value reduced = shifted.result;

            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& narrowed = Append(block, Ir::Opcode::VectorNarrow, address, vectorType);
            narrowed.operands[0] = reduced;
            narrowed.operands[1] = previous;
            narrowed.operandCount = 2;
            narrowed.laneWidth = narrowWidth;
            narrowed.laneCount = lanes;
            narrowed.immediate = wide ? 1u : 0u;
            narrowed.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, narrowed.result, wide ? vectorType : Ir::ValueType::Int64);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode != 0x00 && opcode != 0x02 && opcode != 0x0a) return Arm64Decoder::Result::Unimplemented;
        if (opcode == 0x0a && unsignedForm) return Arm64Decoder::Result::Unimplemented;
        if (sizeIndex == 3 && !wide) return Arm64Decoder::Result::Unallocated;

        bool leftShift = opcode == 0x0a;
        int amount = leftShift ? (int)encoded - (int)elementBits : (int)(2 * elementBits) - (int)encoded;
        int applied = leftShift ? amount : -amount;

        uint8_t lanes = (uint8_t)((wide ? 16 : 8) / narrowBytes);
        Ir::Opcode shiftKind = (!leftShift && !unsignedForm) ? Ir::Opcode::VectorShiftLeftSigned : Ir::Opcode::VectorShiftLeftUnsigned;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value spread = EmitConstant(function, block, address, (uint64_t)(uint8_t)(int8_t)applied, Ir::ValueType::Int64);

        Ir::Instruction& amounts = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
        amounts.operands[0] = spread;
        amounts.operandCount = 1;
        amounts.laneWidth = narrowWidth;
        amounts.laneCount = lanes;
        amounts.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value counts = amounts.result;

        Ir::Instruction& shifted = Append(block, shiftKind, address, vectorType);
        shifted.operands[0] = source;
        shifted.operands[1] = counts;
        shifted.operandCount = 2;
        shifted.laneWidth = narrowWidth;
        shifted.laneCount = lanes;
        shifted.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = shifted.result;

        if (opcode == 0x02)
        {
            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& accumulated = Append(block, Ir::Opcode::VectorAdd, address, vectorType);
            accumulated.operands[0] = previous;
            accumulated.operands[1] = produced;
            accumulated.operandCount = 2;
            accumulated.laneWidth = narrowWidth;
            accumulated.laneCount = lanes;
            accumulated.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = accumulated.result;
        }

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbfe08400u) == 0x2e000000u)
    {
        uint32_t index = (encoding >> 11) & 0xf;
        uint8_t totalBytes = (uint8_t)(wide ? 16 : 8);

        if (!wide && (index & 0x8) != 0) return Arm64Decoder::Result::Unimplemented;

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& instruction = Append(block, Ir::Opcode::VectorExtract, address, vectorType);
        instruction.operands[0] = left;
        instruction.operands[1] = right;
        instruction.operandCount = 2;
        instruction.laneWidth = Ir::LaneWidth::Byte;
        instruction.laneCount = totalBytes;
        instruction.laneIndex = (uint8_t)index;
        instruction.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, instruction.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20fc00u) == 0x0e201c00u)
    {
        uint32_t selector = ((encoding >> 29) & 1) << 2 | ((encoding >> 22) & 0x3);

        uint8_t totalBytes = (uint8_t)(wide ? 16 : 8);
        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        if (selector == 5 || selector == 6 || selector == 7)
        {
            Ir::Value destination = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Value first = selector == 7 ? destination : left;
            Ir::Value second = selector == 5 ? right : (selector == 6 ? destination : left);
            Ir::Value mask = selector == 5 ? destination : right;

            Ir::Instruction& difference = Append(block, Ir::Opcode::VectorXor, address, vectorType);
            difference.operands[0] = first;
            difference.operands[1] = second;
            difference.operandCount = 2;
            difference.laneWidth = Ir::LaneWidth::Byte;
            difference.laneCount = totalBytes;
            difference.result = Ir::MakeValue(AllocateValue(function), vectorType);

            Ir::Instruction& gated = Append(block, Ir::Opcode::VectorAnd, address, vectorType);
            gated.operands[0] = difference.result;
            gated.operands[1] = mask;
            gated.operandCount = 2;
            gated.laneWidth = Ir::LaneWidth::Byte;
            gated.laneCount = totalBytes;
            gated.result = Ir::MakeValue(AllocateValue(function), vectorType);

            Ir::Instruction& merged = Append(block, Ir::Opcode::VectorXor, address, vectorType);
            merged.operands[0] = gated.result;
            merged.operands[1] = second;
            merged.operandCount = 2;
            merged.laneWidth = Ir::LaneWidth::Byte;
            merged.laneCount = totalBytes;
            merged.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, merged.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Opcode kind = Ir::Opcode::Invalid;
        if (selector == 0 || selector == 1) kind = Ir::Opcode::VectorAnd;
        if (selector == 2 || selector == 3) kind = Ir::Opcode::VectorOr;
        if (selector == 4) kind = Ir::Opcode::VectorXor;
        if (kind == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;

        if (selector == 1 || selector == 3)
        {
            Ir::Value saturated = EmitConstant(function, block, address, ~(uint64_t)0, Ir::ValueType::Int64);

            Ir::Instruction& ones = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            ones.operands[0] = saturated;
            ones.operandCount = 1;
            ones.laneWidth = Ir::LaneWidth::Double;
            ones.laneCount = wide ? 2 : 1;
            ones.result = Ir::MakeValue(AllocateValue(function), vectorType);

            Ir::Instruction& inverted = Append(block, Ir::Opcode::VectorXor, address, vectorType);
            inverted.operands[0] = right;
            inverted.operands[1] = ones.result;
            inverted.operandCount = 2;
            inverted.laneWidth = Ir::LaneWidth::Byte;
            inverted.laneCount = totalBytes;
            inverted.result = Ir::MakeValue(AllocateValue(function), vectorType);
            right = inverted.result;
        }

        Ir::Instruction& instruction = Append(block, kind, address, vectorType);
        instruction.operands[0] = left;
        instruction.operands[1] = right;
        instruction.operandCount = 2;
        instruction.laneWidth = Ir::LaneWidth::Byte;
        instruction.laneCount = totalBytes;
        instruction.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, instruction.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20fc00u) == 0x0e204400u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedShift = (encoding >> 29) & 1;
        if (!wide && size == 3) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Opcode kind = unsignedShift ? Ir::Opcode::VectorShiftLeftUnsigned : Ir::Opcode::VectorShiftLeftSigned;
        Ir::Instruction& instruction = Append(block, kind, address, vectorType);
        instruction.operands[0] = left;
        instruction.operands[1] = right;
        instruction.operandCount = 2;
        instruction.laneWidth = LaneWidthFor(size);
        instruction.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(size)));
        instruction.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, instruction.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20fc00u) == 0x0e208400u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool subtract = (encoding >> 29) & 1;
        if (!wide && size == 3) return Arm64Decoder::Result::Unallocated;

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& instruction = Append(block, subtract ? Ir::Opcode::VectorSubtract : Ir::Opcode::VectorAdd, address, vectorType);
        instruction.operands[0] = left;
        instruction.operands[1] = right;
        instruction.operandCount = 2;
        instruction.laneWidth = LaneWidthFor(size);
        instruction.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(size)));
        instruction.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, instruction.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20fc00u) == 0x0e201000u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool signedWiden = ((encoding >> 29) & 1) == 0;
        if (size == 3) return Arm64Decoder::Result::Unallocated;

        int sourceBytes = Ir::LaneSizeInBytes(LaneWidthFor(size));
        uint8_t lanes = (uint8_t)(8 / sourceBytes);

        Ir::Value accumulator = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value narrow = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& widen = Append(block, Ir::Opcode::VectorWiden, address, vectorType);
        widen.operands[0] = narrow;
        widen.operandCount = 1;
        widen.laneWidth = LaneWidthFor(size);
        widen.laneCount = lanes;
        widen.immediate = (signedWiden ? 1u : 0u) | (wide ? 2u : 0u);
        widen.result = Ir::MakeValue(AllocateValue(function), vectorType);

        Ir::Instruction& add = Append(block, Ir::Opcode::VectorAdd, address, vectorType);
        add.operands[0] = accumulator;
        add.operands[1] = widen.result;
        add.operandCount = 2;
        add.laneWidth = LaneWidthFor(size + 1);
        add.laneCount = lanes;
        add.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, add.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f200c00u) == 0x0e200000u)
    {
        uint32_t opcode = (encoding >> 12) & 0xf;
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        if (size == 3) return Arm64Decoder::Result::Unallocated;

        Ir::Opcode combine = Ir::Opcode::Invalid;
        bool accumulate = false;
        bool subtracting = false;

        if (opcode == 0x0) combine = Ir::Opcode::VectorAdd;
        else if (opcode == 0x2) combine = Ir::Opcode::VectorSubtract;
        else if (opcode == 0x7) combine = Ir::Opcode::VectorAbsoluteDifference;
        else if (opcode == 0xc) combine = Ir::Opcode::VectorMultiply;
        else if (opcode == 0x8 || opcode == 0xa)
        {
            combine = Ir::Opcode::VectorMultiply;
            accumulate = true;
            subtracting = opcode == 0xa;
        }
        else if (opcode == 0x5)
        {
            combine = Ir::Opcode::VectorAbsoluteDifference;
            accumulate = true;
        }
        else return Arm64Decoder::Result::Unimplemented;

        Ir::LaneWidth narrowWidth = LaneWidthFor(size);
        Ir::LaneWidth wideWidth = LaneWidthFor(size + 1);
        uint8_t lanes = (uint8_t)(8 / Ir::LaneSizeInBytes(narrowWidth));
        uint64_t widenMode = (unsignedForm ? 0u : 1u) | (wide ? 2u : 0u);

        Ir::Value leftNarrow = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value rightNarrow = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& leftWide = Append(block, Ir::Opcode::VectorWiden, address, vectorType);
        leftWide.operands[0] = leftNarrow;
        leftWide.operandCount = 1;
        leftWide.laneWidth = narrowWidth;
        leftWide.laneCount = lanes;
        leftWide.immediate = widenMode;
        leftWide.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value extendedLeft = leftWide.result;

        Ir::Instruction& rightWide = Append(block, Ir::Opcode::VectorWiden, address, vectorType);
        rightWide.operands[0] = rightNarrow;
        rightWide.operandCount = 1;
        rightWide.laneWidth = narrowWidth;
        rightWide.laneCount = lanes;
        rightWide.immediate = widenMode;
        rightWide.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value extendedRight = rightWide.result;

        Ir::Instruction& merged = Append(block, combine, address, vectorType);
        merged.operands[0] = extendedLeft;
        merged.operands[1] = extendedRight;
        merged.operandCount = 2;
        merged.laneWidth = wideWidth;
        merged.laneCount = lanes;
        merged.immediate = combine == Ir::Opcode::VectorAbsoluteDifference ? (unsignedForm ? 1u : 0u) : 0u;
        merged.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = merged.result;

        if (accumulate)
        {
            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& total =
                Append(block, subtracting ? Ir::Opcode::VectorSubtract : Ir::Opcode::VectorAdd, address, vectorType);
            total.operands[0] = previous;
            total.operands[1] = produced;
            total.operandCount = 2;
            total.laneWidth = wideWidth;
            total.laneCount = lanes;
            total.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = total.result;
        }

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f3efc00u) == 0x0e30a800u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool maximum = ((encoding >> 16) & 1) == 0;

        if (size == 3) return Arm64Decoder::Result::Unallocated;
        if (!wide && size == 2) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = LaneWidthFor(size);
        Ir::ValueType resultType = Ir::ValueType::Int8;
        if (size == 1) resultType = Ir::ValueType::Int16;
        if (size == 2) resultType = Ir::ValueType::Int32;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& reduce = Append(block, Ir::Opcode::VectorReduceMinMax, address, vectorType);
        reduce.operands[0] = source;
        reduce.operandCount = 1;
        reduce.laneWidth = laneWidth;
        reduce.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        reduce.immediate = (unsignedForm ? 1u : 0u) | (maximum ? 0u : 2u);
        reduce.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, reduce.result, resultType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf3ffc00u) == 0x0e31b800u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        if (size == 3) return Arm64Decoder::Result::Unallocated;
        if (!wide && size == 2) return Arm64Decoder::Result::Unallocated;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& reduce = Append(block, Ir::Opcode::VectorReduceAdd, address, vectorType);
        reduce.operands[0] = source;
        reduce.operandCount = 1;
        reduce.laneWidth = LaneWidthFor(size);
        reduce.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(size)));
        reduce.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, reduce.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbffffc00u) == 0x0e205800u)
    {
        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& count = Append(block, Ir::Opcode::VectorCountBits, address, vectorType);
        count.operands[0] = source;
        count.operandCount = 1;
        count.laneWidth = Ir::LaneWidth::Byte;
        count.laneCount = wide ? 16 : 8;
        count.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, count.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf00fc00u) == 0x0f008000u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        if (size != 2 && size != 1) return Arm64Decoder::Result::Unimplemented;

        uint32_t element = size == 2 ? (((encoding >> 11) & 1) << 1) | ((encoding >> 21) & 1)
                                     : (((encoding >> 11) & 1) << 2) | (((encoding >> 21) & 1) << 1) | ((encoding >> 20) & 1);
        uint32_t source = size == 2 ? ((encoding >> 16) & 0x1f) : ((encoding >> 16) & 0xf);

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value elementSource = EmitReadVector(function, block, address, source, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = elementSource;
        extract.operandCount = 1;
        extract.laneWidth = LaneWidthFor(size);
        extract.laneIndex = (uint8_t)element;
        extract.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

        Ir::Instruction& duplicate = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
        duplicate.operands[0] = extract.result;
        duplicate.operandCount = 1;
        duplicate.laneWidth = LaneWidthFor(size);
        duplicate.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(size)));
        duplicate.result = Ir::MakeValue(AllocateValue(function), vectorType);

        Ir::Instruction& multiply = Append(block, Ir::Opcode::VectorMultiply, address, vectorType);
        multiply.operands[0] = left;
        multiply.operands[1] = duplicate.result;
        multiply.operandCount = 2;
        multiply.laneWidth = LaneWidthFor(size);
        multiply.laneCount = duplicate.laneCount;
        multiply.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, multiply.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbfe0fc00u) == 0x0e000400u || (encoding & 0xbfe0fc00u) == 0x0e000c00u)
    {
        bool fromGeneral = (encoding & 0x0000fc00u) == 0x00000c00u;
        uint32_t imm5 = (encoding >> 16) & 0x1f;

        uint32_t size = 0;
        if ((imm5 & 1) != 0) size = 0;
        else if ((imm5 & 2) != 0) size = 1;
        else if ((imm5 & 4) != 0) size = 2;
        else if ((imm5 & 8) != 0) size = 3;
        else return Arm64Decoder::Result::Unallocated;

        if (!wide && size == 3) return Arm64Decoder::Result::Unallocated;

        Ir::Value scalar;
        if (fromGeneral)
        {
            scalar = EmitReadGeneral(function, block, address, rn, size == 3 ? Ir::ValueType::Int64 : Ir::ValueType::Int32, false);
        }
        else
        {
            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
            extract.operands[0] = source;
            extract.operandCount = 1;
            extract.laneWidth = LaneWidthFor(size);
            extract.laneIndex = (uint8_t)(imm5 >> (size + 1));
            extract.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
            scalar = extract.result;
        }

        Ir::Instruction& duplicate = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
        duplicate.operands[0] = scalar;
        duplicate.operandCount = 1;
        duplicate.laneWidth = LaneWidthFor(size);
        duplicate.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(size)));
        duplicate.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, duplicate.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbfe0fc00u) == 0x0e001c00u)
    {
        uint32_t imm5 = (encoding >> 16) & 0x1f;

        uint32_t size = 0;
        if ((imm5 & 1) != 0) size = 0;
        else if ((imm5 & 2) != 0) size = 1;
        else if ((imm5 & 4) != 0) size = 2;
        else if ((imm5 & 8) != 0) size = 3;
        else return Arm64Decoder::Result::Unallocated;

        uint32_t index = imm5 >> (size + 1);
        Ir::ValueType sourceType = size == 3 ? Ir::ValueType::Int64 : Ir::ValueType::Int32;

        Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);
        Ir::Value value = EmitReadGeneral(function, block, address, rn, sourceType, false);

        Ir::Instruction& insert = Append(block, Ir::Opcode::VectorInsertLane, address, vectorType);
        insert.operands[0] = previous;
        insert.operands[1] = value;
        insert.operandCount = 2;
        insert.laneWidth = LaneWidthFor(size);
        insert.laneIndex = (uint8_t)index;
        insert.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, insert.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xffe08400u) == 0x6e000400u)
    {
        uint32_t imm5 = (encoding >> 16) & 0x1f;
        uint32_t imm4 = (encoding >> 11) & 0xf;

        uint32_t size = 0;
        if ((imm5 & 1) != 0) size = 0;
        else if ((imm5 & 2) != 0) size = 1;
        else if ((imm5 & 4) != 0) size = 2;
        else if ((imm5 & 8) != 0) size = 3;
        else return Arm64Decoder::Result::Unallocated;

        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = source;
        extract.operandCount = 1;
        extract.laneWidth = LaneWidthFor(size);
        extract.laneIndex = (uint8_t)(imm4 >> size);
        extract.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);

        Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

        Ir::Instruction& insert = Append(block, Ir::Opcode::VectorInsertLane, address, vectorType);
        insert.operands[0] = previous;
        insert.operands[1] = extract.result;
        insert.operandCount = 2;
        insert.laneWidth = LaneWidthFor(size);
        insert.laneIndex = (uint8_t)(imm5 >> (size + 1));
        insert.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, insert.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbfe0fc00u) == 0x0e003c00u || (encoding & 0xbfe0fc00u) == 0x0e002c00u)
    {
        bool signedMove = (encoding & 0x0000fc00u) == 0x00002c00u;
        uint32_t imm5 = (encoding >> 16) & 0x1f;

        uint32_t size = 0;
        if ((imm5 & 1) != 0) size = 0;
        else if ((imm5 & 2) != 0) size = 1;
        else if ((imm5 & 4) != 0) size = 2;
        else if ((imm5 & 8) != 0) size = 3;
        else return Arm64Decoder::Result::Unallocated;

        if (size == 3 && (signedMove || !wide)) return Arm64Decoder::Result::Unallocated;
        if (signedMove && size == 2 && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::ValueType resultType = wide ? Ir::ValueType::Int64 : Ir::ValueType::Int32;
        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = source;
        extract.operandCount = 1;
        extract.laneWidth = LaneWidthFor(size);
        extract.laneIndex = (uint8_t)(imm5 >> (size + 1));
        extract.result = Ir::MakeValue(AllocateValue(function), resultType);

        Ir::Value value = extract.result;

        if (signedMove)
        {
            Ir::Instruction& extend = Append(block, Ir::Opcode::ExtendSigned, address, resultType);
            extend.operands[0] = value;
            extend.operandCount = 1;
            extend.immediate = Ir::LaneSizeInBytes(LaneWidthFor(size)) * 8;
            extend.result = Ir::MakeValue(AllocateValue(function), resultType);
            value = extend.result;
        }

        EmitWriteGeneral(block, address, rd, value, resultType, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f3ffc00u) == 0x0e303800u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        if (size == 3) return Arm64Decoder::Result::Unallocated;
        if (!wide && size == 2) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth sourceWidth = LaneWidthFor(size);
        int sourceBytes = Ir::LaneSizeInBytes(sourceWidth);
        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& reduce = Append(block, Ir::Opcode::VectorReduceAddLong, address, vectorType);
        reduce.operands[0] = source;
        reduce.operandCount = 1;
        reduce.laneWidth = sourceWidth;
        reduce.laneCount = (uint8_t)((wide ? 16 : 8) / sourceBytes);
        reduce.immediate = unsignedForm ? 0u : 1u;
        reduce.result = Ir::MakeValue(AllocateValue(function), vectorType);

        Ir::ValueType resultType = Ir::ValueType::Int64;
        if (sourceBytes == 1) resultType = Ir::ValueType::Int16;
        if (sourceBytes == 2) resultType = Ir::ValueType::Int32;

        EmitWriteVector(block, address, rd, reduce.result, resultType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20c400u) == 0x0e20c400u)
    {
        uint32_t opcode = (encoding >> 11) & 0x1f;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool negated = ((encoding >> 23) & 1) != 0;
        bool doubleLanes = ((encoding >> 22) & 1) != 0;

        Ir::Opcode operation = Ir::Opcode::Invalid;
        uint64_t mode = 0;

        if (opcode == 0x1a) operation = unsignedForm ? Ir::Opcode::VectorFloatPairwiseAdd
                                                     : (negated ? Ir::Opcode::VectorFloatSubtract : Ir::Opcode::VectorFloatAdd);
        if (opcode == 0x1b && unsignedForm && !negated) operation = Ir::Opcode::VectorFloatMultiply;
        if (opcode == 0x1f && unsignedForm && !negated) operation = Ir::Opcode::VectorFloatDivide;
        if (opcode == 0x1e && !unsignedForm) operation = negated ? Ir::Opcode::VectorFloatMinimum : Ir::Opcode::VectorFloatMaximum;
        if (opcode == 0x18 && !unsignedForm) operation = negated ? Ir::Opcode::VectorFloatMinimum : Ir::Opcode::VectorFloatMaximum;

        if ((opcode == 0x1e || opcode == 0x18) && unsignedForm)
        {
            operation = Ir::Opcode::VectorFloatPairwiseAdd;
            mode = negated ? 2u : 1u;
        }

        bool absoluteAfter = opcode == 0x1a && unsignedForm && negated;
        if (absoluteAfter) operation = Ir::Opcode::VectorFloatSubtract;

        if (opcode == 0x1c)
        {
            operation = Ir::Opcode::VectorFloatCompare;
            if (!unsignedForm && !negated) mode = 0;
            else if (unsignedForm && !negated) mode = 1;
            else if (unsignedForm && negated) mode = 2;
            else operation = Ir::Opcode::Invalid;
        }

        if (opcode == 0x1d && unsignedForm)
        {
            operation = Ir::Opcode::VectorFloatCompare;
            mode = negated ? 0x12 : 0x11;
        }

        if (opcode == 0x1f && !unsignedForm)
        {
            if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

            Ir::LaneWidth stepWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
            uint8_t stepLanes = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(stepWidth));

            Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

            Ir::Instruction& product = Append(block, Ir::Opcode::VectorFloatMultiply, address, vectorType);
            product.operands[0] = left;
            product.operands[1] = right;
            product.operandCount = 2;
            product.laneWidth = stepWidth;
            product.laneCount = stepLanes;
            product.result = Ir::MakeValue(AllocateValue(function), vectorType);

            uint64_t base = negated ? (doubleLanes ? 0x4008000000000000ull : 0x40400000ull)
                                    : (doubleLanes ? 0x4000000000000000ull : 0x40000000ull);
            if (!doubleLanes) base |= base << 32;

            Ir::Value seed = EmitConstant(function, block, address, base, Ir::ValueType::Int64);

            Ir::Instruction& constant = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            constant.operands[0] = seed;
            constant.operandCount = 1;
            constant.laneWidth = Ir::LaneWidth::Double;
            constant.laneCount = wide ? 2 : 1;
            constant.result = Ir::MakeValue(AllocateValue(function), vectorType);

            Ir::Instruction& difference = Append(block, Ir::Opcode::VectorFloatSubtract, address, vectorType);
            difference.operands[0] = constant.result;
            difference.operands[1] = product.result;
            difference.operandCount = 2;
            difference.laneWidth = stepWidth;
            difference.laneCount = stepLanes;
            difference.result = Ir::MakeValue(AllocateValue(function), vectorType);

            Ir::Value stepped = difference.result;

            if (negated)
            {
                uint64_t half = doubleLanes ? 0x3fe0000000000000ull : 0x3f000000ull;
                if (!doubleLanes) half |= half << 32;

                Ir::Value factor = EmitConstant(function, block, address, half, Ir::ValueType::Int64);

                Ir::Instruction& scale = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
                scale.operands[0] = factor;
                scale.operandCount = 1;
                scale.laneWidth = Ir::LaneWidth::Double;
                scale.laneCount = wide ? 2 : 1;
                scale.result = Ir::MakeValue(AllocateValue(function), vectorType);

                Ir::Instruction& halved = Append(block, Ir::Opcode::VectorFloatMultiply, address, vectorType);
                halved.operands[0] = stepped;
                halved.operands[1] = scale.result;
                halved.operandCount = 2;
                halved.laneWidth = stepWidth;
                halved.laneCount = stepLanes;
                halved.result = Ir::MakeValue(AllocateValue(function), vectorType);
                stepped = halved.result;
            }

            EmitWriteVector(block, address, rd, stepped, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x19 && !unsignedForm)
        {
            if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

            Ir::LaneWidth stepWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
            Ir::Value factor = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value other = EmitReadVector(function, block, address, rm, vectorType);
            Ir::Value accumulator = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& fused = Append(block, Ir::Opcode::VectorFloatMultiplyAdd, address, vectorType);
            fused.operands[0] = factor;
            fused.operands[1] = other;
            fused.operands[2] = accumulator;
            fused.operandCount = 3;
            fused.laneWidth = stepWidth;
            fused.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(stepWidth));
            fused.immediate = negated ? 1u : 0u;
            fused.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value produced = fused.result;

            EmitWriteVector(block, address, rd, produced, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (operation == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;
        if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& combined = Append(block, operation, address, vectorType);
        combined.operands[0] = left;
        combined.operands[1] = right;
        combined.operandCount = 2;
        combined.laneWidth = laneWidth;
        combined.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        combined.immediate = mode;
        combined.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = combined.result;

        if (absoluteAfter)
        {
            Ir::Instruction& magnitude = Append(block, Ir::Opcode::VectorFloatAbsolute, address, vectorType);
            magnitude.operands[0] = produced;
            magnitude.operandCount = 1;
            magnitude.laneWidth = laneWidth;
            magnitude.laneCount = combined.laneCount;
            magnitude.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = magnitude.result;
        }

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xdf20c400u) == 0x5e20c400u)
    {
        uint32_t opcode = (encoding >> 11) & 0x1f;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool negated = ((encoding >> 23) & 1) != 0;
        bool doubleLanes = ((encoding >> 22) & 1) != 0;

        Ir::ValueType scalarType = doubleLanes ? Ir::ValueType::Float64 : Ir::ValueType::Float32;
        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;

        Ir::Opcode operation = Ir::Opcode::Invalid;
        uint64_t mode = 0;
        bool absoluteAfter = false;

        if (opcode == 0x1a && unsignedForm && negated)
        {
            operation = Ir::Opcode::VectorFloatSubtract;
            absoluteAfter = true;
        }

        if (opcode == 0x1c && unsignedForm)
        {
            operation = Ir::Opcode::VectorFloatCompare;
            mode = negated ? 2u : 1u;
        }

        if (opcode == 0x1c && !unsignedForm && !negated) operation = Ir::Opcode::VectorFloatCompare;

        if (opcode == 0x1d && unsignedForm)
        {
            operation = Ir::Opcode::VectorFloatCompare;
            mode = negated ? 0x12u : 0x11u;
        }

        if (operation == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& combined = Append(block, operation, address, vectorType);
        combined.operands[0] = left;
        combined.operands[1] = right;
        combined.operandCount = 2;
        combined.laneWidth = laneWidth;
        combined.laneCount = 1;
        combined.immediate = mode;
        combined.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = combined.result;

        if (absoluteAfter)
        {
            Ir::Instruction& magnitude = Append(block, Ir::Opcode::VectorFloatAbsolute, address, vectorType);
            magnitude.operands[0] = produced;
            magnitude.operandCount = 1;
            magnitude.laneWidth = laneWidth;
            magnitude.laneCount = 1;
            magnitude.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = magnitude.result;
        }

        EmitWriteVector(block, address, rd, produced, scalarType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20fc00u) == 0x0e208c00u)
    {
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        if (size == 3 && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = LaneWidthFor(size);
        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& compare = Append(block, Ir::Opcode::VectorCompare, address, vectorType);
        compare.operands[0] = left;
        compare.operands[1] = right;
        compare.operandCount = 2;
        compare.laneWidth = laneWidth;
        compare.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        compare.immediate = unsignedForm ? 0u : 0x20u;
        compare.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, compare.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f20f400u) == 0x0e203400u || (encoding & 0xbf20fc00u) == 0x2e208c00u)
    {
        uint32_t opcode = (encoding >> 11) & 0x1f;
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        uint64_t mode = 0;
        if (opcode == 0x11) mode = unsignedForm ? 0u : 0x20u;
        else if (opcode == 0x06) mode = 2u | (unsignedForm ? 0x10u : 0u);
        else if (opcode == 0x07) mode = 1u | (unsignedForm ? 0x10u : 0u);
        else return Arm64Decoder::Result::Unimplemented;

        if (size == 3 && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = LaneWidthFor(size);
        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& compare = Append(block, Ir::Opcode::VectorCompare, address, vectorType);
        compare.operands[0] = left;
        compare.operands[1] = right;
        compare.operandCount = 2;
        compare.laneWidth = laneWidth;
        compare.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        compare.immediate = mode;
        compare.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, compare.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f200400u) == 0x0e200400u)
    {
        uint32_t opcode = (encoding >> 11) & 0x1f;
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;

        Ir::Opcode operation = Ir::Opcode::Invalid;
        uint64_t mode = 0;
        bool accumulate = false;
        bool subtracting = false;

        if (opcode == 0x13 && !unsignedForm) operation = Ir::Opcode::VectorMultiply;

        if (opcode == 0x12)
        {
            operation = Ir::Opcode::VectorMultiply;
            accumulate = true;
            subtracting = unsignedForm;
        }

        if (opcode == 0x0c || opcode == 0x0d)
        {
            operation = Ir::Opcode::VectorMinMax;
            mode = (unsignedForm ? 1u : 0u) | (opcode == 0x0d ? 2u : 0u);
        }

        if (opcode == 0x0e)
        {
            operation = Ir::Opcode::VectorAbsoluteDifference;
            mode = unsignedForm ? 1u : 0u;
        }

        if (opcode == 0x14 || opcode == 0x15 || opcode == 0x17)
        {
            operation = Ir::Opcode::VectorPairwise;
            uint64_t kind = opcode == 0x17 ? 0u : (opcode == 0x14 ? 1u : 2u);
            mode = (unsignedForm ? 1u : 0u) | (kind << 1);
            if (opcode == 0x17 && unsignedForm) operation = Ir::Opcode::Invalid;
        }

        if (operation == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;
        if (size == 3 && operation != Ir::Opcode::VectorPairwise) return Arm64Decoder::Result::Unallocated;
        if (size == 3 && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = LaneWidthFor(size);
        uint8_t lanes = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& combined = Append(block, operation, address, vectorType);
        combined.operands[0] = left;
        combined.operands[1] = right;
        combined.operandCount = 2;
        combined.laneWidth = laneWidth;
        combined.laneCount = lanes;
        combined.immediate = mode;
        combined.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = combined.result;

        if (accumulate)
        {
            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& merged =
                Append(block, subtracting ? Ir::Opcode::VectorSubtract : Ir::Opcode::VectorAdd, address, vectorType);
            merged.operands[0] = previous;
            merged.operands[1] = produced;
            merged.operandCount = 2;
            merged.laneWidth = laneWidth;
            merged.laneCount = lanes;
            merged.result = Ir::MakeValue(AllocateValue(function), vectorType);
            produced = merged.result;
        }

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f3e0c00u) == 0x0e200800u)
    {
        uint32_t opcode = (encoding >> 12) & 0x1f;
        uint32_t size = (encoding >> 22) & 0x3;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool floatFamily = (size & 0x2) != 0;
        bool doubleLanes = (size & 0x1) != 0;

        Ir::Opcode operation = Ir::Opcode::Invalid;
        uint64_t mode = 0;

        if (opcode == 0x00 || opcode == 0x01)
        {
            uint32_t containerBytes = 0;
            if (opcode == 0x00 && !unsignedForm) containerBytes = 8;
            if (opcode == 0x00 && unsignedForm) containerBytes = 4;
            if (opcode == 0x01 && !unsignedForm) containerBytes = 2;
            if (containerBytes == 0) return Arm64Decoder::Result::Unimplemented;

            Ir::LaneWidth laneWidth = LaneWidthFor(size);
            int laneBytes = Ir::LaneSizeInBytes(laneWidth);
            if (laneBytes >= (int)containerBytes) return Arm64Decoder::Result::Unallocated;

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& reversed = Append(block, Ir::Opcode::VectorReverseBytes, address, vectorType);
            reversed.operands[0] = source;
            reversed.operandCount = 1;
            reversed.laneWidth = laneWidth;
            reversed.laneCount = (uint8_t)((wide ? 16 : 8) / laneBytes);
            reversed.immediate = containerBytes;
            reversed.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, reversed.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x08 || opcode == 0x09 || opcode == 0x0a)
        {
            {
                uint64_t compareMode = 0x8;
                if (opcode == 0x08) compareMode |= unsignedForm ? 1u : 2u;
                else if (opcode == 0x09) compareMode |= unsignedForm ? 3u : 0u;
                else if (!unsignedForm) compareMode |= 4u;
                else return Arm64Decoder::Result::Unimplemented;

                Ir::LaneWidth compareWidth = LaneWidthFor(size);
                if (size == 3 && !wide) return Arm64Decoder::Result::Unallocated;

                Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

                Ir::Instruction& compare = Append(block, Ir::Opcode::VectorCompare, address, vectorType);
                compare.operands[0] = source;
                compare.operands[1] = source;
                compare.operandCount = 2;
                compare.laneWidth = compareWidth;
                compare.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(compareWidth));
                compare.immediate = compareMode;
                compare.result = Ir::MakeValue(AllocateValue(function), vectorType);

                EmitWriteVector(block, address, rd, compare.result, vectorType);
                return Arm64Decoder::Result::Ok;
            }
        }

        if (opcode == 0x05 && unsignedForm && size == 0)
        {
            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value saturated = EmitConstant(function, block, address, ~(uint64_t)0, Ir::ValueType::Int64);

            Ir::Instruction& ones = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            ones.operands[0] = saturated;
            ones.operandCount = 1;
            ones.laneWidth = Ir::LaneWidth::Double;
            ones.laneCount = wide ? 2 : 1;
            ones.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value mask = ones.result;

            Ir::Instruction& inverted = Append(block, Ir::Opcode::VectorXor, address, vectorType);
            inverted.operands[0] = source;
            inverted.operands[1] = mask;
            inverted.operandCount = 2;
            inverted.laneWidth = Ir::LaneWidth::Byte;
            inverted.laneCount = wide ? 16 : 8;
            inverted.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, inverted.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x13 && unsignedForm)
        {
            if (size == 3) return Arm64Decoder::Result::Unallocated;

            Ir::LaneWidth narrowWidth = LaneWidthFor(size);
            Ir::LaneWidth wideWidth = LaneWidthFor(size + 1);
            uint8_t lanes = (uint8_t)(8 / Ir::LaneSizeInBytes(narrowWidth));
            uint32_t shift = 8u << size;

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& widen = Append(block, Ir::Opcode::VectorWiden, address, vectorType);
            widen.operands[0] = source;
            widen.operandCount = 1;
            widen.laneWidth = narrowWidth;
            widen.laneCount = lanes;
            widen.immediate = wide ? 2u : 0u;
            widen.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value widened = widen.result;

            Ir::Value spread = EmitConstant(function, block, address, shift, Ir::ValueType::Int64);

            Ir::Instruction& amounts = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            amounts.operands[0] = spread;
            amounts.operandCount = 1;
            amounts.laneWidth = wideWidth;
            amounts.laneCount = lanes;
            amounts.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value counts = amounts.result;

            Ir::Instruction& shifted = Append(block, Ir::Opcode::VectorShiftLeftUnsigned, address, vectorType);
            shifted.operands[0] = widened;
            shifted.operands[1] = counts;
            shifted.operandCount = 2;
            shifted.laneWidth = wideWidth;
            shifted.laneCount = lanes;
            shifted.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, shifted.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x0b || opcode == 0x04 || opcode == 0x02 || opcode == 0x06)
        {
            Ir::LaneWidth elementWidth = LaneWidthFor(size);
            if (size == 3 && !wide) return Arm64Decoder::Result::Unallocated;

            uint8_t lanes = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(elementWidth));
            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            if (opcode == 0x02 || opcode == 0x06)
            {
                if (size == 3) return Arm64Decoder::Result::Unallocated;
                Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

                Ir::Instruction& folded = Append(block, Ir::Opcode::VectorPairwiseAddLong, address, vectorType);
                folded.operands[0] = source;
                folded.operands[1] = previous;
                folded.operandCount = 2;
                folded.laneWidth = elementWidth;
                folded.laneCount = lanes;
                folded.immediate = (unsignedForm ? 1u : 0u) | (opcode == 0x06 ? 2u : 0u);
                folded.result = Ir::MakeValue(AllocateValue(function), vectorType);

                EmitWriteVector(block, address, rd, folded.result, vectorType);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Opcode unary = opcode == 0x0b ? Ir::Opcode::VectorAbsolute : Ir::Opcode::VectorCountLeadingZeros;

            Ir::Instruction& applied = Append(block, unary, address, vectorType);
            applied.operands[0] = source;
            applied.operandCount = 1;
            applied.laneWidth = elementWidth;
            applied.laneCount = lanes;
            applied.immediate = opcode == 0x0b ? (unsignedForm ? 1u : 0u) : (unsignedForm ? 0u : 1u);
            applied.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, applied.result, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if ((opcode == 0x16 || opcode == 0x17) && !unsignedForm)
        {
            bool widen = opcode == 0x17;
            bool halfPrecision = (size & 1) == 0;

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& converted = Append(block, Ir::Opcode::VectorFloatConvertWidth, address, vectorType);
            converted.operands[0] = source;
            converted.operands[1] = previous;
            converted.operandCount = 2;
            converted.laneWidth = halfPrecision ? Ir::LaneWidth::Half : Ir::LaneWidth::Word;
            converted.laneCount = halfPrecision ? 4 : 2;
            converted.immediate = (widen ? 1u : 0u) | (wide ? 2u : 0u) | (halfPrecision ? 4u : 0u);
            converted.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, converted.result, widen ? vectorType : (wide ? vectorType : Ir::ValueType::Int64));
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x18 || opcode == 0x19)
        {
            bool towardsLimit = floatFamily;
            uint64_t rounding = 0;

            if (opcode == 0x18 && !unsignedForm) rounding = towardsLimit ? 1u : 0u;
            else if (opcode == 0x19 && !unsignedForm) rounding = towardsLimit ? 3u : 2u;
            else if (opcode == 0x18 && !towardsLimit) rounding = 4u;
            else if (opcode == 0x19) rounding = towardsLimit ? 0u : 5u;
            else return Arm64Decoder::Result::Unimplemented;

            if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

            Ir::LaneWidth stepWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

            Ir::Instruction& rounded = Append(block, Ir::Opcode::VectorFloatRound, address, vectorType);
            rounded.operands[0] = source;
            rounded.operandCount = 1;
            rounded.laneWidth = stepWidth;
            rounded.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(stepWidth));
            rounded.immediate = rounding;
            rounded.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value produced = rounded.result;

            EmitWriteVector(block, address, rd, produced, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x0f && floatFamily) operation = unsignedForm ? Ir::Opcode::VectorFloatNegate : Ir::Opcode::VectorFloatAbsolute;
        if (opcode == 0x1f && floatFamily && unsignedForm) operation = Ir::Opcode::VectorFloatSquareRoot;

        if (opcode == 0x1d && floatFamily)
        {
            operation = Ir::Opcode::VectorFloatEstimate;
            mode = unsignedForm ? 1u : 0u;
        }

        if (floatFamily && (opcode == 0x0c || opcode == 0x0d || opcode == 0x0e))
        {
            operation = Ir::Opcode::VectorFloatCompare;

            if (opcode == 0x0c) mode = unsignedForm ? 0x9u : 0xau;
            else if (opcode == 0x0d) mode = unsignedForm ? 0xbu : 0x8u;
            else if (!unsignedForm) mode = 0xcu;
            else operation = Ir::Opcode::Invalid;
        }

        if (opcode == 0x12 && !unsignedForm)
        {
            Ir::LaneWidth narrowWidth = LaneWidthFor(size);
            int narrowBytes = Ir::LaneSizeInBytes(narrowWidth);
            if (size == 3) return Arm64Decoder::Result::Unallocated;

            Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);
            Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

            Ir::Instruction& narrowed = Append(block, Ir::Opcode::VectorNarrow, address, vectorType);
            narrowed.operands[0] = source;
            narrowed.operands[1] = previous;
            narrowed.operandCount = 2;
            narrowed.laneWidth = narrowWidth;
            narrowed.laneCount = (uint8_t)(8 / narrowBytes);
            narrowed.immediate = wide ? 1u : 0u;
            narrowed.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rd, narrowed.result, wide ? vectorType : Ir::ValueType::Int64);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 0x1d && !floatFamily)
        {
            operation = Ir::Opcode::VectorConvertFloat;
            mode = unsignedForm ? 1u : 3u;
        }

        if (opcode == 0x1b && floatFamily)
        {
            operation = Ir::Opcode::VectorConvertFloat;
            mode = unsignedForm ? 0u : 2u;
        }

        if (operation == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;
        if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
        Ir::Value source = EmitReadVector(function, block, address, rn, vectorType);

        Ir::Instruction& applied = Append(block, operation, address, vectorType);
        applied.operands[0] = source;
        applied.operandCount = 1;
        applied.laneWidth = laneWidth;
        applied.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        applied.immediate = mode;
        applied.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, applied.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf208c00u) == 0x0e000000u)
    {
        uint32_t length = (encoding >> 13) & 0x3;
        bool extend = ((encoding >> 12) & 1) != 0;

        if (extend) return Arm64Decoder::Result::Unimplemented;

        Ir::Value selectors = EmitReadVector(function, block, address, rm, vectorType);
        Ir::Value gathered;

        for (uint32_t part = 0; part <= length; ++part)
        {
            Ir::Value table = EmitReadVector(function, block, address, (rn + part) & 0x1f, vectorType);

            Ir::Instruction& lookup = Append(block, Ir::Opcode::VectorTableLookup, address, vectorType);
            lookup.operands[0] = table;
            lookup.operands[1] = selectors;
            lookup.operandCount = 2;
            lookup.laneWidth = Ir::LaneWidth::Byte;
            lookup.laneCount = wide ? 16 : 8;
            lookup.immediate = (uint64_t)part * 16;
            lookup.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value slice = lookup.result;

            if (part == 0)
            {
                gathered = slice;
                continue;
            }

            Ir::Instruction& merged = Append(block, Ir::Opcode::VectorOr, address, vectorType);
            merged.operands[0] = gathered;
            merged.operands[1] = slice;
            merged.operandCount = 2;
            merged.laneWidth = Ir::LaneWidth::Byte;
            merged.laneCount = wide ? 16 : 8;
            merged.result = Ir::MakeValue(AllocateValue(function), vectorType);
            gathered = merged.result;
        }

        EmitWriteVector(block, address, rd, gathered, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf208c00u) == 0x0e000800u)
    {
        uint32_t opcode = (encoding >> 12) & 0x7;
        uint32_t size = (encoding >> 22) & 0x3;

        if (opcode == 0 || opcode == 4) return Arm64Decoder::Result::Unallocated;
        if (!wide && size == 3) return Arm64Decoder::Result::Unallocated;

        uint64_t kind = (uint64_t)((opcode & 0x3) - 1);
        uint64_t mode = (kind << 1) | (uint64_t)((opcode >> 2) & 1);

        Ir::LaneWidth laneWidth = LaneWidthFor(size);
        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);
        Ir::Value right = EmitReadVector(function, block, address, rm, vectorType);

        Ir::Instruction& permuted = Append(block, Ir::Opcode::VectorPermute, address, vectorType);
        permuted.operands[0] = left;
        permuted.operands[1] = right;
        permuted.operandCount = 2;
        permuted.laneWidth = laneWidth;
        permuted.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));
        permuted.immediate = mode;
        permuted.result = Ir::MakeValue(AllocateValue(function), vectorType);

        EmitWriteVector(block, address, rd, permuted.result, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x9f800400u) == 0x0f800000u)
    {
        uint32_t opcode = (encoding >> 12) & 0xf;
        bool unsignedForm = ((encoding >> 29) & 1) != 0;
        bool doubleLanes = ((encoding >> 22) & 1) != 0;

        if (opcode != 0x9 && opcode != 0x1 && opcode != 0x5) return Arm64Decoder::Result::Unimplemented;
        if (unsignedForm) return Arm64Decoder::Result::Unimplemented;
        if (doubleLanes && !wide) return Arm64Decoder::Result::Unallocated;

        uint32_t high = (encoding >> 11) & 1;
        uint32_t low = (encoding >> 21) & 1;
        uint32_t extra = (encoding >> 20) & 1;

        uint32_t element = ((encoding >> 16) & 0xf) | (extra << 4);
        uint32_t index = doubleLanes ? high : ((high << 1) | low);

        if (doubleLanes && low != 0) return Arm64Decoder::Result::Unallocated;

        Ir::LaneWidth laneWidth = doubleLanes ? Ir::LaneWidth::Double : Ir::LaneWidth::Word;
        uint8_t lanes = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(laneWidth));

        Ir::Value source = EmitReadVector(function, block, address, element, vectorType);

        Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
        extract.operands[0] = source;
        extract.operandCount = 1;
        extract.laneWidth = laneWidth;
        extract.laneIndex = (uint8_t)index;
        extract.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
        Ir::Value lane = extract.result;

        Ir::Instruction& spread = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
        spread.operands[0] = lane;
        spread.operandCount = 1;
        spread.laneWidth = laneWidth;
        spread.laneCount = lanes;
        spread.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value broadcast = spread.result;

        Ir::Value left = EmitReadVector(function, block, address, rn, vectorType);

        if (opcode == 0x9)
        {
            Ir::Instruction& combined = Append(block, Ir::Opcode::VectorFloatMultiply, address, vectorType);
            combined.operands[0] = left;
            combined.operands[1] = broadcast;
            combined.operandCount = 2;
            combined.laneWidth = laneWidth;
            combined.laneCount = lanes;
            combined.result = Ir::MakeValue(AllocateValue(function), vectorType);
            Ir::Value scaled = combined.result;

            EmitWriteVector(block, address, rd, scaled, vectorType);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value previous = EmitReadVector(function, block, address, rd, vectorType);

        Ir::Instruction& fused = Append(block, Ir::Opcode::VectorFloatMultiplyAdd, address, vectorType);
        fused.operands[0] = left;
        fused.operands[1] = broadcast;
        fused.operands[2] = previous;
        fused.operandCount = 3;
        fused.laneWidth = laneWidth;
        fused.laneCount = lanes;
        fused.immediate = opcode == 0x1 ? 0u : 1u;
        fused.result = Ir::MakeValue(AllocateValue(function), vectorType);
        Ir::Value produced = fused.result;

        EmitWriteVector(block, address, rd, produced, vectorType);
        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static uint64_t ExpandFloatImmediate(uint32_t immediate, bool isDouble)
{
    uint64_t sign = (immediate >> 7) & 1;
    uint64_t topExponent = (immediate >> 6) & 1;
    uint64_t lowExponent = (immediate >> 4) & 3;
    uint64_t fraction = immediate & 0xf;

    uint32_t exponentBits = isDouble ? 11 : 8;
    uint32_t fractionBits = isDouble ? 52 : 23;

    uint64_t replicated = topExponent ? ((1ull << (exponentBits - 3)) - 1) : 0;
    uint64_t exponent = ((topExponent ^ 1ull) << (exponentBits - 1)) | (replicated << 2) | lowExponent;

    return (sign << (exponentBits + fractionBits)) | (exponent << fractionBits) | (fraction << (fractionBits - 4));
}

static Arm64Decoder::Result DecodeFloatingPoint(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    uint32_t rd = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;
    uint32_t rm = (encoding >> 16) & 0x1f;
    uint32_t type = (encoding >> 22) & 0x3;

    uint32_t family = (encoding >> 24) & 0x1f;

    if ((family != 0x1e && family != 0x1f) || ((encoding >> 30) & 1) != 0 || ((encoding >> 29) & 1) != 0)
    {
        return DecodeVectorDataProcessing(function, block, encoding, address);
    }
    if (type == 2) return Arm64Decoder::Result::Unallocated;
    if (type == 3) return Arm64Decoder::Result::Unimplemented;

    Ir::ValueType floatType = type == 0 ? Ir::ValueType::Float32 : Ir::ValueType::Float64;
    bool is64BitInteger = (encoding >> 31) & 1;
    Ir::ValueType integerType = WidthType(is64BitInteger);

    if ((encoding & 0xff200c00u) == 0x1e200c00u)
    {
        Ir::ValueType bitType = type == 0 ? Ir::ValueType::Int32 : Ir::ValueType::Int64;
        Ir::Condition condition = static_cast<Ir::Condition>((encoding >> 12) & 0xf);

        Ir::Value taken = EmitReadVector(function, block, address, rn, bitType);
        Ir::Value other = EmitReadVector(function, block, address, rm, bitType);

        Ir::Instruction& select = Append(block, Ir::Opcode::Select, address, bitType);
        select.operands[0] = taken;
        select.operands[1] = other;
        select.operandCount = 2;
        select.condition = condition;
        select.result = Ir::MakeValue(AllocateValue(function), bitType);

        EmitWriteVector(block, address, rd, select.result, bitType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xff207c00u) == 0x1e204000u)
    {
        uint32_t singleSource = (encoding >> 15) & 0x3f;
        Ir::ValueType bitType = type == 0 ? Ir::ValueType::Int32 : Ir::ValueType::Int64;
        uint64_t signBit = type == 0 ? 0x80000000ull : 0x8000000000000000ull;

        if (singleSource == 0)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);
            EmitWriteVector(block, address, rd, value, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (singleSource >= 8 && singleSource <= 15)
        {
            uint32_t rounding = 0;
            if (singleSource == 8) rounding = 0;
            else if (singleSource == 9) rounding = 1;
            else if (singleSource == 10) rounding = 2;
            else if (singleSource == 11) rounding = 3;
            else if (singleSource == 12) rounding = 4;
            else if (singleSource == 14) rounding = 5;
            else if (singleSource == 15) rounding = 0;
            else return Arm64Decoder::Result::Unallocated;

            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);

            Ir::Instruction& rounded = Append(block, Ir::Opcode::FloatRound, address, floatType);
            rounded.operands[0] = value;
            rounded.operandCount = 1;
            rounded.operandType = floatType;
            rounded.immediate = rounding;
            rounded.updatesFloatingPointStatus = true;
            rounded.result = Ir::MakeValue(AllocateValue(function), floatType);

            EmitWriteVector(block, address, rd, rounded.result, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (singleSource == 3)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);

            Ir::Instruction& root = Append(block, Ir::Opcode::FloatSquareRoot, address, floatType);
            root.operands[0] = value;
            root.operandCount = 1;
            root.operandType = floatType;
            root.updatesFloatingPointStatus = true;
            root.result = Ir::MakeValue(AllocateValue(function), floatType);

            EmitWriteVector(block, address, rd, root.result, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (singleSource == 4 || singleSource == 5)
        {
            Ir::ValueType targetType = singleSource == 4 ? Ir::ValueType::Float32 : Ir::ValueType::Float64;
            if (targetType == floatType) return Arm64Decoder::Result::Unallocated;

            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);

            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvert, address, targetType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = 6;
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), targetType);

            EmitWriteVector(block, address, rd, convert.result, targetType);
            return Arm64Decoder::Result::Ok;
        }

        if (singleSource == 1 || singleSource == 2)
        {
            Ir::Value bits = EmitReadVector(function, block, address, rn, bitType);
            Ir::Value mask = EmitConstant(function, block, address, singleSource == 1 ? ~signBit : signBit, bitType);

            Ir::Instruction& combined =
                Append(block, singleSource == 1 ? Ir::Opcode::And : Ir::Opcode::Xor, address, bitType);
            combined.operands[0] = bits;
            combined.operands[1] = mask;
            combined.operandCount = 2;
            combined.result = Ir::MakeValue(AllocateValue(function), bitType);

            EmitWriteVector(block, address, rd, combined.result, bitType);
            return Arm64Decoder::Result::Ok;
        }
    }

    if ((encoding & 0x5f20fc00u) == 0x1e200000u)
    {
        uint32_t opcode = (encoding >> 16) & 0x1f;
        uint32_t sf = (encoding >> 31) & 1;

        if (opcode == 6 || opcode == 7)
        {
            if (sf == 0 && type == 1) return Arm64Decoder::Result::Unallocated;

            if (opcode == 6)
            {
                Ir::Value value = EmitReadVector(function, block, address, rn, floatType);
                Ir::Instruction& move = Append(block, Ir::Opcode::FloatConvert, address, integerType);
                move.operands[0] = value;
                move.operandCount = 1;
                move.result = Ir::MakeValue(AllocateValue(function), integerType);
                move.immediate = 1;
                EmitWriteGeneral(block, address, rd, move.result, integerType, false);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value value = EmitReadGeneral(function, block, address, rn, integerType, false);
            Ir::Instruction& move = Append(block, Ir::Opcode::FloatConvert, address, floatType);
            move.operands[0] = value;
            move.operandCount = 1;
            move.result = Ir::MakeValue(AllocateValue(function), floatType);
            move.immediate = 1;
            EmitWriteVector(block, address, rd, move.result, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 2 || opcode == 3)
        {
            Ir::Value value = EmitReadGeneral(function, block, address, rn, integerType, false);
            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvert, address, floatType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = opcode == 2 ? 2 : 3;
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), floatType);
            EmitWriteVector(block, address, rd, convert.result, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode == 24 || opcode == 25)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);
            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvert, address, integerType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = opcode == 24 ? 4 : 5;
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), integerType);
            EmitWriteGeneral(block, address, rd, convert.result, integerType, false);
            return Arm64Decoder::Result::Ok;
        }

        if (opcode <= 1 || opcode == 4 || opcode == 5 || opcode == 8 || opcode == 9 || opcode == 16 || opcode == 17)
        {
            uint32_t rounding = 0;
            if (opcode == 4 || opcode == 5) rounding = 4;
            else if (opcode == 8 || opcode == 9) rounding = 1;
            else if (opcode == 16 || opcode == 17) rounding = 2;

            bool unsignedForm = (opcode & 1) != 0;
            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);

            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvertRounded, address, integerType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = ((uint64_t)rounding << 1) | (unsignedForm ? 1u : 0u);
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), integerType);

            EmitWriteGeneral(block, address, rd, convert.result, integerType, false);
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unimplemented;
    }

    if ((encoding & 0x5f200000u) == 0x1e000000u)
    {
        uint32_t opcode = (encoding >> 16) & 0x7;
        uint32_t rmode = (encoding >> 19) & 0x3;
        uint32_t scale = (encoding >> 10) & 0x3f;
        uint32_t fractionBits = 64 - scale;

        if (!is64BitInteger && scale < 32) return Arm64Decoder::Result::Unallocated;
        if (fractionBits > 64) return Arm64Decoder::Result::Unallocated;

        bool unsignedForm = (opcode & 1) != 0;

        if (rmode == 0 && (opcode == 2 || opcode == 3))
        {
            Ir::Value value = EmitReadGeneral(function, block, address, rn, integerType, false);

            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvertRounded, address, floatType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = (unsignedForm ? 1u : 0u) | 0x10u | ((uint64_t)fractionBits << 8);
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), floatType);

            EmitWriteVector(block, address, rd, convert.result, floatType);
            return Arm64Decoder::Result::Ok;
        }

        if (rmode == 3 && opcode <= 1)
        {
            Ir::Value value = EmitReadVector(function, block, address, rn, floatType);

            Ir::Instruction& convert = Append(block, Ir::Opcode::FloatConvertRounded, address, integerType);
            convert.operands[0] = value;
            convert.operandCount = 1;
            convert.immediate = (unsignedForm ? 1u : 0u) | (3ull << 1) | ((uint64_t)fractionBits << 8);
            convert.updatesFloatingPointStatus = true;
            convert.result = Ir::MakeValue(AllocateValue(function), integerType);

            EmitWriteGeneral(block, address, rd, convert.result, integerType, false);
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unimplemented;
    }

    if ((encoding & 0xff200c00u) == 0x1e200400u)
    {
        Ir::Condition condition = static_cast<Ir::Condition>((encoding >> 12) & 0xf);
        uint64_t flags = (uint64_t)(encoding & 0xf) << 28;
        bool signalling = ((encoding >> 4) & 1) != 0;

        Ir::Value left = EmitReadVector(function, block, address, rn, floatType);
        Ir::Value right = EmitReadVector(function, block, address, rm, floatType);

        Ir::Instruction& compare =
            Append(block, signalling ? Ir::Opcode::FloatCompareSignalling : Ir::Opcode::FloatCompare, address, floatType);
        compare.operands[0] = left;
        compare.operands[1] = right;
        compare.operandCount = 2;
        compare.operandType = floatType;
        compare.updatesFloatingPointStatus = true;
        compare.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Value computed = compare.result;
        Ir::Value fallback = EmitConstant(function, block, address, flags, Ir::ValueType::Int32);

        Ir::Instruction& select = Append(block, Ir::Opcode::Select, address, Ir::ValueType::Int32);
        select.operands[0] = computed;
        select.operands[1] = fallback;
        select.operandCount = 2;
        select.condition = condition;
        select.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Instruction& write = Append(block, Ir::Opcode::WriteFlags, address, Ir::ValueType::Int32);
        write.operands[0] = select.result;
        write.operandCount = 1;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf201fe0u) == 0x1e201000u)
    {
        uint64_t bits = ExpandFloatImmediate((encoding >> 13) & 0xff, type == 1);
        Ir::Value value = EmitConstant(function, block, address, bits, floatType);
        EmitWriteVector(block, address, rd, value, floatType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xff20fc07u) == 0x1e202000u)
    {
        bool signalling = (encoding >> 4) & 1;
        bool againstZero = (encoding >> 3) & 1;

        Ir::Value left = EmitReadVector(function, block, address, rn, floatType);
        Ir::Value right = againstZero ? EmitConstant(function, block, address, 0, floatType)
                                      : EmitReadVector(function, block, address, rm, floatType);

        Ir::Instruction& compare = Append(block, signalling ? Ir::Opcode::FloatCompareSignalling : Ir::Opcode::FloatCompare, address, floatType);
        compare.operands[0] = left;
        compare.operands[1] = right;
        compare.operandCount = 2;
        compare.updatesFloatingPointStatus = true;
        compare.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

        Ir::Instruction& write = Append(block, Ir::Opcode::WriteFlags, address, Ir::ValueType::Int32);
        write.operands[0] = compare.result;
        write.operandCount = 1;
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xff000000u) == 0x1f000000u)
    {
        uint32_t addend = (encoding >> 10) & 0x1f;
        bool subtracting = ((encoding >> 15) & 1) != 0;
        bool negateAddend = ((encoding >> 21) & 1) != 0;
        bool negateProduct = subtracting != negateAddend;

        if (((encoding >> 29) & 0x3) != 0) return Arm64Decoder::Result::Unallocated;

        Ir::Value factor = EmitReadVector(function, block, address, rn, floatType);
        Ir::Value other = EmitReadVector(function, block, address, rm, floatType);
        Ir::Value accumulator = EmitReadVector(function, block, address, addend, floatType);

        Ir::Instruction& fused = Append(block, Ir::Opcode::FloatMultiplyAdd, address, floatType);
        fused.operands[0] = factor;
        fused.operands[1] = other;
        fused.operands[2] = accumulator;
        fused.operandCount = 3;
        fused.operandType = floatType;
        fused.immediate = (negateProduct ? 1u : 0u) | (negateAddend ? 2u : 0u);
        fused.updatesFloatingPointStatus = true;
        fused.result = Ir::MakeValue(AllocateValue(function), floatType);

        EmitWriteVector(block, address, rd, fused.result, floatType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xff200c00u) == 0x1e200800u)
    {
        uint32_t opcode = (encoding >> 12) & 0xf;

        Ir::Opcode kind = Ir::Opcode::Invalid;
        uint64_t mode = 0;

        if (opcode == 2) kind = Ir::Opcode::FloatAdd;
        if (opcode == 3) kind = Ir::Opcode::FloatSubtract;
        if (opcode == 0 || opcode == 8) kind = Ir::Opcode::FloatMultiply;
        if (opcode == 1) kind = Ir::Opcode::FloatDivide;

        if (opcode == 4 || opcode == 6)
        {
            kind = Ir::Opcode::FloatMaximum;
            mode = opcode == 6 ? 1u : 0u;
        }

        if (opcode == 5 || opcode == 7)
        {
            kind = Ir::Opcode::FloatMinimum;
            mode = opcode == 7 ? 1u : 0u;
        }

        if (kind == Ir::Opcode::Invalid) return Arm64Decoder::Result::Unimplemented;

        Ir::Value left = EmitReadVector(function, block, address, rn, floatType);
        Ir::Value right = EmitReadVector(function, block, address, rm, floatType);

        Ir::Instruction& instruction = Append(block, kind, address, floatType);
        instruction.operands[0] = left;
        instruction.operands[1] = right;
        instruction.operandCount = 2;
        instruction.operandType = floatType;
        instruction.immediate = mode;
        instruction.updatesFloatingPointStatus = true;
        instruction.result = Ir::MakeValue(AllocateValue(function), floatType);
        Ir::Value produced = instruction.result;

        if (opcode == 8)
        {
            Ir::ValueType bitType = type == 0 ? Ir::ValueType::Int32 : Ir::ValueType::Int64;
            uint64_t signBit = type == 0 ? 0x80000000ull : 0x8000000000000000ull;

            Ir::Value flipped = EmitConstant(function, block, address, signBit, bitType);

            Ir::Instruction& negated = Append(block, Ir::Opcode::Xor, address, bitType);
            negated.operands[0] = produced;
            negated.operands[1] = flipped;
            negated.operandCount = 2;
            negated.result = Ir::MakeValue(AllocateValue(function), bitType);

            EmitWriteVector(block, address, rd, negated.result, bitType);
            return Arm64Decoder::Result::Ok;
        }

        EmitWriteVector(block, address, rd, produced, floatType);
        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static Ir::ValueType VectorAccessType(uint32_t scale)
{
    if (scale == 0) return Ir::ValueType::Int8;
    if (scale == 1) return Ir::ValueType::Int16;
    if (scale == 2) return Ir::ValueType::Int32;
    if (scale == 3) return Ir::ValueType::Int64;
    return Ir::ValueType::Vector128;
}

static Arm64Decoder::Result DecodeVectorLoadStore(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    uint32_t rt = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;

    if ((encoding & 0xbf000000u) == 0x0c000000u)
    {
        bool postIndex = (encoding >> 23) & 1;
        bool load = (encoding >> 22) & 1;
        uint32_t opcode = (encoding >> 12) & 0xf;
        uint32_t size = (encoding >> 10) & 0x3;
        uint32_t rm = (encoding >> 16) & 0x1f;
        bool wide = (encoding >> 30) & 1;

        if (((encoding >> 21) & 1) != 0) return Arm64Decoder::Result::Unallocated;
        if (!postIndex && rm != 0) return Arm64Decoder::Result::Unallocated;

        uint32_t registers = 0;
        bool interleaved = false;

        if (opcode == 0x7) registers = 1;
        else if (opcode == 0xa) registers = 2;
        else if (opcode == 0x6) registers = 3;
        else if (opcode == 0x2) registers = 4;
        else if (opcode == 0x8)
        {
            registers = 2;
            interleaved = true;
        }
        else if (opcode == 0x0)
        {
            registers = 4;
            interleaved = true;
        }
        else if (opcode == 0x4)
        {
            registers = 3;
            interleaved = true;
        }
        else return Arm64Decoder::Result::Unimplemented;

        if (!wide && size == 3 && registers != 1) return Arm64Decoder::Result::Unallocated;

        int lanes = wide ? 16 : 8;
        Ir::ValueType accessType = wide ? Ir::ValueType::Vector128 : Ir::ValueType::Int64;

        if (interleaved)
        {
            if (!wide && size == 3) return Arm64Decoder::Result::Unallocated;

            Ir::ValueType vectorType = Ir::ValueType::Vector128;
            Ir::LaneWidth laneWidth = LaneWidthFor(size);
            uint8_t laneTotal = (uint8_t)(lanes / Ir::LaneSizeInBytes(laneWidth));

            uint32_t slots[4];
            for (uint32_t step = 0; step < registers; ++step) slots[step] = (rt + step) & 0x1f;

            Ir::Value staged[4];
            Ir::Value folded[4];

            if (registers == 3)
            {
                Ir::ValueType elementType = VectorAccessType(size);
                uint32_t elementBytes = 1u << size;

                if (load)
                {
                    Ir::Value zero = EmitConstant(function, block, address, 0, Ir::ValueType::Int64);

                    Ir::Instruction& blank = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
                    blank.operands[0] = zero;
                    blank.operandCount = 1;
                    blank.laneWidth = Ir::LaneWidth::Double;
                    blank.laneCount = 2;
                    blank.result = Ir::MakeValue(AllocateValue(function), vectorType);

                    Ir::Value built[3] = {blank.result, blank.result, blank.result};

                    for (uint32_t lane = 0; lane < laneTotal; ++lane)
                    {
                        for (uint32_t part = 0; part < 3; ++part)
                        {
                            Ir::Value pointer =
                                EmitAddress(function, block, address, rn, (int64_t)(lane * 3 + part) * elementBytes);
                            Ir::Value loaded =
                                EmitLoad(function, block, address, pointer, elementType, Ir::MemoryOrder::Relaxed);

                            Ir::Instruction& insert = Append(block, Ir::Opcode::VectorInsertLane, address, vectorType);
                            insert.operands[0] = built[part];
                            insert.operands[1] = loaded;
                            insert.operandCount = 2;
                            insert.laneWidth = laneWidth;
                            insert.laneIndex = (uint8_t)lane;
                            insert.result = Ir::MakeValue(AllocateValue(function), vectorType);
                            built[part] = insert.result;
                        }
                    }

                    for (uint32_t part = 0; part < 3; ++part)
                        EmitWriteVector(block, address, slots[part], built[part], vectorType);
                }
                else
                {
                    Ir::Value source[3];
                    for (uint32_t part = 0; part < 3; ++part)
                        source[part] = EmitReadVector(function, block, address, slots[part], vectorType);

                    for (uint32_t lane = 0; lane < laneTotal; ++lane)
                    {
                        for (uint32_t part = 0; part < 3; ++part)
                        {
                            Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
                            extract.operands[0] = source[part];
                            extract.operandCount = 1;
                            extract.laneWidth = laneWidth;
                            extract.laneIndex = (uint8_t)lane;
                            extract.result = Ir::MakeValue(AllocateValue(function), elementType);

                            Ir::Value pointer =
                                EmitAddress(function, block, address, rn, (int64_t)(lane * 3 + part) * elementBytes);
                            EmitStore(block, address, pointer, extract.result, elementType, Ir::MemoryOrder::Relaxed);
                        }
                    }
                }
            }
            else if (load)
            {
                for (uint32_t step = 0; step < registers; ++step)
                {
                    Ir::Value target = EmitAddress(function, block, address, rn, (int64_t)step * lanes);
                    staged[step] = EmitLoad(function, block, address, target, accessType, Ir::MemoryOrder::Relaxed);
                }

                for (uint32_t pass = 0; pass < registers / 2; ++pass)
                {
                    for (uint32_t part = 0; part < 2; ++part)
                    {
                        Ir::Instruction& split = Append(block, Ir::Opcode::VectorPermute, address, vectorType);
                        split.operands[0] = staged[pass * 2];
                        split.operands[1] = staged[pass * 2 + 1];
                        split.operandCount = 2;
                        split.laneWidth = laneWidth;
                        split.laneCount = laneTotal;
                        split.immediate = part;
                        split.result = Ir::MakeValue(AllocateValue(function), vectorType);
                        folded[part * (registers / 2) + pass] = split.result;
                    }
                }

                if (registers == 2)
                {
                    EmitWriteVector(block, address, slots[0], folded[0], accessType);
                    EmitWriteVector(block, address, slots[1], folded[1], accessType);
                }
                else
                {
                    for (uint32_t pass = 0; pass < 2; ++pass)
                    {
                        for (uint32_t part = 0; part < 2; ++part)
                        {
                            Ir::Instruction& split = Append(block, Ir::Opcode::VectorPermute, address, vectorType);
                            split.operands[0] = folded[pass * 2];
                            split.operands[1] = folded[pass * 2 + 1];
                            split.operandCount = 2;
                            split.laneWidth = laneWidth;
                            split.laneCount = laneTotal;
                            split.immediate = part;
                            split.result = Ir::MakeValue(AllocateValue(function), vectorType);

                            EmitWriteVector(block, address, slots[pass + part * 2], split.result, accessType);
                        }
                    }
                }
            }
            else
            {
                for (uint32_t step = 0; step < registers; ++step)
                {
                    staged[step] = EmitReadVector(function, block, address, slots[step], vectorType);
                }

                if (registers == 4)
                {
                    for (uint32_t pass = 0; pass < 2; ++pass)
                    {
                        for (uint32_t part = 0; part < 2; ++part)
                        {
                            Ir::Instruction& weave = Append(block, Ir::Opcode::VectorPermute, address, vectorType);
                            weave.operands[0] = staged[pass];
                            weave.operands[1] = staged[pass + 2];
                            weave.operandCount = 2;
                            weave.laneWidth = laneWidth;
                            weave.laneCount = laneTotal;
                            weave.immediate = 4u | part;
                            weave.result = Ir::MakeValue(AllocateValue(function), vectorType);
                            folded[pass * 2 + part] = weave.result;
                        }
                    }
                }
                else
                {
                    folded[0] = staged[0];
                    folded[1] = staged[1];
                }

                for (uint32_t pass = 0; pass < registers / 2; ++pass)
                {
                    for (uint32_t part = 0; part < 2; ++part)
                    {
                        Ir::Instruction& weave = Append(block, Ir::Opcode::VectorPermute, address, vectorType);
                        weave.operands[0] = registers == 4 ? folded[pass] : folded[0];
                        weave.operands[1] = registers == 4 ? folded[pass + 2] : folded[1];
                        weave.operandCount = 2;
                        weave.laneWidth = laneWidth;
                        weave.laneCount = laneTotal;
                        weave.immediate = 4u | part;
                        weave.result = Ir::MakeValue(AllocateValue(function), vectorType);
                        Ir::Value woven = weave.result;

                        Ir::Value target = EmitAddress(function, block, address, rn, (int64_t)(pass * 2 + part) * lanes);
                        EmitStore(block, address, target, woven, accessType, Ir::MemoryOrder::Relaxed);
                    }
                }
            }

            if (postIndex)
            {
                Ir::Value base = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, true);
                Ir::Value step = rm == 31 ? EmitConstant(function, block, address, (uint64_t)(registers * lanes), Ir::ValueType::Int64)
                                          : EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

                Ir::Value updated = EmitBinary(function, block, address, Ir::Opcode::Add, base, step, Ir::ValueType::Int64);
                EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
            }

            return Arm64Decoder::Result::Ok;
        }

        for (uint32_t step = 0; step < registers; ++step)
        {
            uint32_t element = (rt + step) & 0x1f;
            Ir::Value pointer = EmitAddress(function, block, address, rn, (int64_t)step * lanes);

            if (load)
            {
                Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
                EmitWriteVector(block, address, element, loaded, accessType);
            }
            else
            {
                Ir::Value value = EmitReadVector(function, block, address, element, accessType);
                EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
            }
        }

        if (postIndex)
        {
            Ir::Value base = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, true);
            Ir::Value step = rm == 31 ? EmitConstant(function, block, address, (uint64_t)(registers * lanes), Ir::ValueType::Int64)
                                      : EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

            Ir::Value updated = EmitBinary(function, block, address, Ir::Opcode::Add, base, step, Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0xbf000000u) == 0x0d000000u)
    {
        bool postIndex = (encoding >> 23) & 1;
        bool load = (encoding >> 22) & 1;
        uint32_t replicate = (encoding >> 21) & 1;
        uint32_t opcode = (encoding >> 13) & 0x7;
        uint32_t selectBit = (encoding >> 12) & 1;
        uint32_t size = (encoding >> 10) & 0x3;
        uint32_t rm = (encoding >> 16) & 0x1f;
        bool wide = (encoding >> 30) & 1;

        if (!postIndex && rm != 0) return Arm64Decoder::Result::Unallocated;

        uint32_t registers = ((opcode & 1) << 1 | replicate) + 1;
        if (registers != 1) return Arm64Decoder::Result::Unimplemented;

        uint32_t scale = (opcode >> 1) & 0x3;
        uint32_t elementSize = 0;
        uint32_t index = 0;

        if (scale == 0)
        {
            elementSize = 0;
            index = ((wide ? 1u : 0u) << 3) | (selectBit << 2) | size;
        }
        else if (scale == 1)
        {
            if ((size & 1) != 0) return Arm64Decoder::Result::Unallocated;
            elementSize = 1;
            index = ((wide ? 1u : 0u) << 2) | (selectBit << 1) | (size >> 1);
        }
        else if (scale == 2)
        {
            if ((size & 1) == 0)
            {
                elementSize = 2;
                index = ((wide ? 1u : 0u) << 1) | selectBit;
            }
            else
            {
                if (selectBit != 0) return Arm64Decoder::Result::Unallocated;
                elementSize = 3;
                index = wide ? 1u : 0u;
            }
        }
        else
        {
            if (!load || selectBit != 0) return Arm64Decoder::Result::Unallocated;
            elementSize = size;
        }

        Ir::ValueType accessType = VectorAccessType(elementSize);
        Ir::Value pointer = EmitAddress(function, block, address, rn, 0);
        Ir::ValueType vectorType = Ir::ValueType::Vector128;

        if (scale == 3)
        {
            Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);

            Ir::Instruction& spread = Append(block, Ir::Opcode::VectorDuplicate, address, vectorType);
            spread.operands[0] = loaded;
            spread.operandCount = 1;
            spread.laneWidth = LaneWidthFor(elementSize);
            spread.laneCount = (uint8_t)((wide ? 16 : 8) / Ir::LaneSizeInBytes(LaneWidthFor(elementSize)));
            spread.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rt, spread.result, vectorType);
        }
        else if (load)
        {
            Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
            Ir::Value previous = EmitReadVector(function, block, address, rt, vectorType);

            Ir::Instruction& insert = Append(block, Ir::Opcode::VectorInsertLane, address, vectorType);
            insert.operands[0] = previous;
            insert.operands[1] = loaded;
            insert.operandCount = 2;
            insert.laneWidth = LaneWidthFor(elementSize);
            insert.laneIndex = (uint8_t)index;
            insert.result = Ir::MakeValue(AllocateValue(function), vectorType);

            EmitWriteVector(block, address, rt, insert.result, vectorType);
        }
        else
        {
            Ir::Value source = EmitReadVector(function, block, address, rt, vectorType);

            Ir::Instruction& extract = Append(block, Ir::Opcode::VectorExtractLane, address, vectorType);
            extract.operands[0] = source;
            extract.operandCount = 1;
            extract.laneWidth = LaneWidthFor(elementSize);
            extract.laneIndex = (uint8_t)index;
            extract.result = Ir::MakeValue(AllocateValue(function), accessType);

            EmitStore(block, address, pointer, extract.result, accessType, Ir::MemoryOrder::Relaxed);
        }

        if (postIndex)
        {
            int64_t stride = (int64_t)1 << elementSize;
            Ir::Value base = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, true);
            Ir::Value step = rm == 31 ? EmitConstant(function, block, address, (uint64_t)stride, Ir::ValueType::Int64)
                                      : EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

            Ir::Value updated = EmitBinary(function, block, address, Ir::Opcode::Add, base, step, Ir::ValueType::Int64);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3f000000u) == 0x3d000000u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint32_t scale = ((opc & 0x2) << 1) | size;
        if (scale > 4) return Arm64Decoder::Result::Unallocated;

        Ir::ValueType accessType = VectorAccessType(scale);
        uint64_t immediate = ((encoding >> 10) & 0xfff) << scale;
        Ir::Value pointer = EmitAddress(function, block, address, rn, (int64_t)immediate);

        if ((opc & 1) == 0)
        {
            Ir::Value value = EmitReadVector(function, block, address, rt, accessType);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
        EmitWriteVector(block, address, rt, loaded, accessType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3f200c00u) == 0x3c200800u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint32_t rm = (encoding >> 16) & 0x1f;
        uint32_t option = (encoding >> 13) & 0x7;
        bool scaled = (encoding >> 12) & 1;
        uint32_t scale = ((opc & 0x2) << 1) | size;

        if (scale > 4) return Arm64Decoder::Result::Unallocated;
        if ((option & 0x2) == 0) return Arm64Decoder::Result::Unallocated;

        Ir::ValueType accessType = VectorAccessType(scale);
        Ir::Value base = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, true);
        Ir::Value index = EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

        if (option != 3 && option != 7)
        {
            Ir::Opcode extend = (option & 0x4) ? Ir::Opcode::ExtendSigned : Ir::Opcode::ExtendUnsigned;
            Ir::Instruction& instruction = Append(block, extend, address, Ir::ValueType::Int64);
            instruction.operands[0] = index;
            instruction.operandCount = 1;
            instruction.immediate = 32;
            instruction.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
            index = instruction.result;
        }

        if (scaled && scale != 0)
        {
            Ir::Value amount = EmitConstant(function, block, address, scale, Ir::ValueType::Int64);
            index = EmitBinary(function, block, address, Ir::Opcode::ShiftLeft, index, amount, Ir::ValueType::Int64);
        }

        Ir::Value pointer = EmitBinary(function, block, address, Ir::Opcode::Add, base, index, Ir::ValueType::Int64);

        if ((opc & 1) == 0)
        {
            Ir::Value value = EmitReadVector(function, block, address, rt, accessType);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
        EmitWriteVector(block, address, rt, loaded, accessType);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3f200000u) == 0x3c000000u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint32_t mode = (encoding >> 10) & 0x3;
        uint32_t scale = ((opc & 0x2) << 1) | size;
        if (scale > 4) return Arm64Decoder::Result::Unallocated;
        if (mode == 2) return Arm64Decoder::Result::Unimplemented;

        Ir::ValueType accessType = VectorAccessType(scale);
        int64_t immediate = (int64_t)SignExtend((encoding >> 12) & 0x1ff, 9);

        bool preIndex = mode == 3;
        bool postIndex = mode == 1;
        int64_t applied = postIndex ? 0 : immediate;

        Ir::Value pointer = EmitAddress(function, block, address, rn, applied);

        if ((opc & 1) == 0)
        {
            Ir::Value value = EmitReadVector(function, block, address, rt, accessType);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
        }
        else
        {
            Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
            EmitWriteVector(block, address, rt, loaded, accessType);
        }

        if (preIndex || postIndex)
        {
            Ir::Value updated = EmitAddress(function, block, address, rn, immediate);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3e000000u) == 0x2c000000u)
    {
        uint32_t opc = (encoding >> 30) & 0x3;
        uint32_t mode = (encoding >> 23) & 0x3;
        bool load = (encoding >> 22) & 1;
        uint32_t rt2 = (encoding >> 10) & 0x1f;

        if (opc == 3) return Arm64Decoder::Result::Unallocated;
        if (mode == 0) return Arm64Decoder::Result::Unimplemented;

        uint32_t scale = 2 + opc;
        Ir::ValueType accessType = VectorAccessType(scale);
        int width = Ir::SizeInBytes(accessType);
        int64_t immediate = (int64_t)SignExtend((encoding >> 15) & 0x7f, 7) * width;

        bool preIndex = mode == 3;
        bool postIndex = mode == 1;
        int64_t applied = postIndex ? 0 : immediate;

        Ir::Value pointer = EmitAddress(function, block, address, rn, applied);
        Ir::Value second = EmitAddress(function, block, address, rn, applied + width);

        if (load)
        {
            Ir::Value first = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
            Ir::Value other = EmitLoad(function, block, address, second, accessType, Ir::MemoryOrder::Relaxed);
            EmitWriteVector(block, address, rt, first, accessType);
            EmitWriteVector(block, address, rt2, other, accessType);
        }
        else
        {
            Ir::Value first = EmitReadVector(function, block, address, rt, accessType);
            Ir::Value other = EmitReadVector(function, block, address, rt2, accessType);
            EmitStore(block, address, pointer, first, accessType, Ir::MemoryOrder::Relaxed);
            EmitStore(block, address, second, other, accessType, Ir::MemoryOrder::Relaxed);
        }

        if (preIndex || postIndex)
        {
            Ir::Value updated = EmitAddress(function, block, address, rn, immediate);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static Arm64Decoder::Result LoadStoreWidthCheck(uint32_t size, uint32_t opc)
{
    if (size == 3 && opc == 2) return Arm64Decoder::Result::Unimplemented;
    if (opc == 3 && size >= 2) return Arm64Decoder::Result::Unallocated;
    return Arm64Decoder::Result::Ok;
}

static Arm64Decoder::Result DecodeLoadStore(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    uint32_t rt = encoding & 0x1f;
    uint32_t rn = (encoding >> 5) & 0x1f;

    if ((encoding & 0xffc00000u) == 0xf9800000u || (encoding & 0xffe00c00u) == 0xf8a00800u ||
        (encoding & 0xffe00c00u) == 0xf8800000u || (encoding & 0xff000000u) == 0xd8000000u)
    {
        return Arm64Decoder::Result::Ok;
    }

    if (((encoding >> 26) & 1) != 0) return DecodeVectorLoadStore(function, block, encoding, address);

    if ((encoding & 0x3f000000u) == 0x08000000u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        bool second = (encoding >> 23) & 1;
        bool load = (encoding >> 22) & 1;
        bool pair = (encoding >> 21) & 1;
        uint32_t rs = (encoding >> 16) & 0x1f;
        bool ordered = (encoding >> 15) & 1;
        uint32_t rt2 = (encoding >> 10) & 0x1f;
        Ir::ValueType accessType = AccessType(size);

        if (second && pair)
        {
            if (rt2 != 31) return Arm64Decoder::Result::Unallocated;

            Ir::Value pointer = EmitAddress(function, block, address, rn, 0);
            Ir::Value expected = EmitReadGeneral(function, block, address, rs, accessType, false);
            Ir::Value desired = EmitReadGeneral(function, block, address, rt, accessType, false);

            Ir::Instruction& compare = Append(block, Ir::Opcode::AtomicCompareExchange, address, accessType);
            compare.operands[0] = pointer;
            compare.operands[1] = expected;
            compare.operands[2] = desired;
            compare.operandCount = 3;
            compare.order = ordered ? Ir::MemoryOrder::AcquireRelease : Ir::MemoryOrder::Relaxed;
            compare.result = Ir::MakeValue(AllocateValue(function), accessType);

            EmitWriteGeneral(block, address, rs, compare.result, accessType, false);
            return Arm64Decoder::Result::Ok;
        }

        if (second && !pair && rs == 31 && rt2 == 31)
        {
            Ir::Value pointer = EmitAddress(function, block, address, rn, 0);

            if (load)
            {
                Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Acquire);
                EmitWriteGeneral(block, address, rt, loaded, accessType, false);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value value = EmitReadGeneral(function, block, address, rt, accessType, false);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Release);
            return Arm64Decoder::Result::Ok;
        }

        if (!second && pair && size < 2)
        {
            if (rt2 != 31) return Arm64Decoder::Result::Unallocated;

            Ir::ValueType pairType = size == 0 ? Ir::ValueType::Int32 : Ir::ValueType::Int64;

            Ir::Value expectedLow = EmitReadGeneral(function, block, address, rs, pairType, false);
            Ir::Value expectedHigh = EmitReadGeneral(function, block, address, rs + 1, pairType, false);

            Ir::Instruction& expected = Append(block, Ir::Opcode::SetExclusiveExpected, address, pairType);
            expected.operands[0] = expectedLow;
            expected.operands[1] = expectedHigh;
            expected.operandCount = 2;

            Ir::Value pointer = EmitAddress(function, block, address, rn, 0);
            Ir::Value desiredLow = EmitReadGeneral(function, block, address, rt, pairType, false);
            Ir::Value desiredHigh = EmitReadGeneral(function, block, address, rt + 1, pairType, false);

            Ir::Instruction& swap = Append(block, Ir::Opcode::AtomicCompareExchangePair, address, pairType);
            swap.operands[0] = pointer;
            swap.operands[1] = desiredLow;
            swap.operands[2] = desiredHigh;
            swap.operandCount = 3;
            swap.order = ordered ? Ir::MemoryOrder::AcquireRelease : Ir::MemoryOrder::Relaxed;
            swap.result = Ir::MakeValue(AllocateValue(function), pairType);
            Ir::Value swappedLow = swap.result;

            Ir::Instruction& high = Append(block, Ir::Opcode::ReadExclusiveHigh, address, pairType);
            high.operandCount = 0;
            high.result = Ir::MakeValue(AllocateValue(function), pairType);
            Ir::Value swappedHigh = high.result;

            EmitWriteGeneral(block, address, rs, swappedLow, pairType, false);
            EmitWriteGeneral(block, address, rs + 1, swappedHigh, pairType, false);
            return Arm64Decoder::Result::Ok;
        }

        if (!second && pair && size >= 2)
        {
            Ir::ValueType pairType = size == 2 ? Ir::ValueType::Int32 : Ir::ValueType::Int64;
            Ir::Value pointer = EmitAddress(function, block, address, rn, 0);

            if (load)
            {
                Ir::Instruction& low = Append(block, Ir::Opcode::LoadExclusivePair, address, pairType);
                low.operands[0] = pointer;
                low.operandCount = 1;
                low.order = ordered ? Ir::MemoryOrder::Acquire : Ir::MemoryOrder::Relaxed;
                low.result = Ir::MakeValue(AllocateValue(function), pairType);
                Ir::Value loadedLow = low.result;

                Ir::Instruction& high = Append(block, Ir::Opcode::ReadExclusiveHigh, address, pairType);
                high.operandCount = 0;
                high.result = Ir::MakeValue(AllocateValue(function), pairType);
                Ir::Value loadedHigh = high.result;

                EmitWriteGeneral(block, address, rt, loadedLow, pairType, false);
                EmitWriteGeneral(block, address, rt2, loadedHigh, pairType, false);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value low = EmitReadGeneral(function, block, address, rt, pairType, false);
            Ir::Value high = EmitReadGeneral(function, block, address, rt2, pairType, false);

            Ir::Instruction& instruction = Append(block, Ir::Opcode::StoreExclusivePair, address, pairType);
            instruction.operands[0] = pointer;
            instruction.operands[1] = low;
            instruction.operands[2] = high;
            instruction.operandCount = 3;
            instruction.order = ordered ? Ir::MemoryOrder::Release : Ir::MemoryOrder::Relaxed;
            instruction.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);

            EmitWriteGeneral(block, address, rs, instruction.result, Ir::ValueType::Int32, false);
            return Arm64Decoder::Result::Ok;
        }

        if (!second && !pair && rt2 == 31)
        {
            Ir::Value pointer = EmitAddress(function, block, address, rn, 0);

            if (load)
            {
                Ir::Instruction& instruction = Append(block, Ir::Opcode::LoadExclusive, address, accessType);
                instruction.operands[0] = pointer;
                instruction.operandCount = 1;
                instruction.order = ordered ? Ir::MemoryOrder::Acquire : Ir::MemoryOrder::Relaxed;
                instruction.result = Ir::MakeValue(AllocateValue(function), accessType);
                EmitWriteGeneral(block, address, rt, instruction.result, accessType, false);
                return Arm64Decoder::Result::Ok;
            }

            Ir::Value value = EmitReadGeneral(function, block, address, rt, accessType, false);
            Ir::Instruction& instruction = Append(block, Ir::Opcode::StoreExclusive, address, accessType);
            instruction.operands[0] = pointer;
            instruction.operands[1] = value;
            instruction.operandCount = 2;
            instruction.order = ordered ? Ir::MemoryOrder::Release : Ir::MemoryOrder::Relaxed;
            instruction.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int32);
            EmitWriteGeneral(block, address, rs, instruction.result, Ir::ValueType::Int32, false);
            return Arm64Decoder::Result::Ok;
        }

        return Arm64Decoder::Result::Unimplemented;
    }

    if ((encoding & 0x3f200000u) == 0x38200000u && ((encoding >> 10) & 0x3) == 0)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        bool acquire = (encoding >> 23) & 1;
        bool release = (encoding >> 22) & 1;
        uint32_t rs = (encoding >> 16) & 0x1f;
        uint32_t operation = (encoding >> 12) & 0x7;
        bool swap = (encoding >> 15) & 1;
        Ir::ValueType accessType = AccessType(size);

        Ir::Value pointer = EmitAddress(function, block, address, rn, 0);
        Ir::Value operand = EmitReadGeneral(function, block, address, rs, accessType, false);

        if (swap && operation == 4)
        {
            if (rs != 31) return Arm64Decoder::Result::Unallocated;
            Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::AcquireReadOnly);
            EmitWriteGeneral(block, address, rt, loaded, accessType, false);
            return Arm64Decoder::Result::Ok;
        }

        Ir::AtomicOperation kind = Ir::AtomicOperation::Swap;
        if (!swap)
        {
            switch (operation)
            {
                case 0: kind = Ir::AtomicOperation::Add; break;
                case 1: kind = Ir::AtomicOperation::Clear; break;
                case 2: kind = Ir::AtomicOperation::ExclusiveOr; break;
                case 3: kind = Ir::AtomicOperation::SetBits; break;
                case 4: kind = Ir::AtomicOperation::MaxSigned; break;
                case 5: kind = Ir::AtomicOperation::MinSigned; break;
                case 6: kind = Ir::AtomicOperation::MaxUnsigned; break;
                default: kind = Ir::AtomicOperation::MinUnsigned; break;
            }
        }
        else if (operation != 0)
        {
            return Arm64Decoder::Result::Unallocated;
        }

        Ir::Instruction& instruction = Append(block, Ir::Opcode::AtomicReadModifyWrite, address, accessType);
        instruction.operands[0] = pointer;
        instruction.operands[1] = operand;
        instruction.operandCount = 2;
        instruction.atomicOperation = kind;
        Ir::MemoryOrder order = Ir::MemoryOrder::Relaxed;
        if (acquire && release) order = Ir::MemoryOrder::AcquireRelease;
        else if (acquire) order = Ir::MemoryOrder::Acquire;
        else if (release) order = Ir::MemoryOrder::Release;
        instruction.order = order;
        instruction.result = Ir::MakeValue(AllocateValue(function), accessType);

        EmitWriteGeneral(block, address, rt, instruction.result, accessType, false);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3b000000u) == 0x39000000u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint64_t immediate = ((encoding >> 10) & 0xfff) << size;
        Ir::ValueType accessType = AccessType(size);

        Arm64Decoder::Result widthCheck = LoadStoreWidthCheck(size, opc);
        if (widthCheck != Arm64Decoder::Result::Ok) return widthCheck;

        Ir::Value pointer = EmitAddress(function, block, address, rn, (int64_t)immediate);

        if (opc == 0)
        {
            Ir::Value value = EmitReadGeneral(function, block, address, rt, accessType, false);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
        EmitExtendedWrite(function, block, address, rt, loaded, size, opc, opc != 3);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3b200000u) == 0x38000000u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint32_t mode = (encoding >> 10) & 0x3;
        int64_t immediate = (int64_t)SignExtend((encoding >> 12) & 0x1ff, 9);
        Ir::ValueType accessType = AccessType(size);

        if (mode == 2) return Arm64Decoder::Result::Unimplemented;
        Arm64Decoder::Result widthCheck = LoadStoreWidthCheck(size, opc);
        if (widthCheck != Arm64Decoder::Result::Ok) return widthCheck;

        bool preIndex = mode == 3;
        bool postIndex = mode == 1;
        int64_t applied = postIndex ? 0 : immediate;

        Ir::Value pointer = EmitAddress(function, block, address, rn, applied);

        if (opc == 0)
        {
            Ir::Value value = EmitReadGeneral(function, block, address, rt, accessType, false);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
        }
        else
        {
            Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
            EmitExtendedWrite(function, block, address, rt, loaded, size, opc, opc != 3);
        }

        if (preIndex || postIndex)
        {
            Ir::Value updated = EmitAddress(function, block, address, rn, immediate);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3b200c00u) == 0x38200800u)
    {
        uint32_t size = (encoding >> 30) & 0x3;
        uint32_t opc = (encoding >> 22) & 0x3;
        uint32_t rm = (encoding >> 16) & 0x1f;
        uint32_t option = (encoding >> 13) & 0x7;
        bool scaled = (encoding >> 12) & 1;
        Ir::ValueType accessType = AccessType(size);

        if ((option & 0x2) == 0) return Arm64Decoder::Result::Unallocated;
        Arm64Decoder::Result widthCheck = LoadStoreWidthCheck(size, opc);
        if (widthCheck != Arm64Decoder::Result::Ok) return widthCheck;

        Ir::Value base = EmitReadGeneral(function, block, address, rn, Ir::ValueType::Int64, true);
        Ir::Value index = EmitReadGeneral(function, block, address, rm, Ir::ValueType::Int64, false);

        if (option != 3 && option != 7)
        {
            Ir::Opcode extend = (option & 0x4) ? Ir::Opcode::ExtendSigned : Ir::Opcode::ExtendUnsigned;
            Ir::Instruction& instruction = Append(block, extend, address, Ir::ValueType::Int64);
            instruction.operands[0] = index;
            instruction.operandCount = 1;
            instruction.immediate = 32;
            instruction.result = Ir::MakeValue(AllocateValue(function), Ir::ValueType::Int64);
            index = instruction.result;
        }

        if (scaled && size != 0)
        {
            Ir::Value amount = EmitConstant(function, block, address, size, Ir::ValueType::Int64);
            index = EmitBinary(function, block, address, Ir::Opcode::ShiftLeft, index, amount, Ir::ValueType::Int64);
        }

        Ir::Value pointer = EmitBinary(function, block, address, Ir::Opcode::Add, base, index, Ir::ValueType::Int64);

        if (opc == 0)
        {
            Ir::Value value = EmitReadGeneral(function, block, address, rt, accessType, false);
            EmitStore(block, address, pointer, value, accessType, Ir::MemoryOrder::Relaxed);
            return Arm64Decoder::Result::Ok;
        }

        Ir::Value loaded = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
        EmitExtendedWrite(function, block, address, rt, loaded, size, opc, opc != 3);
        return Arm64Decoder::Result::Ok;
    }

    if ((encoding & 0x3a000000u) == 0x28000000u)
    {
        uint32_t opc = (encoding >> 30) & 0x3;
        uint32_t mode = (encoding >> 23) & 0x3;
        bool load = (encoding >> 22) & 1;
        uint32_t rt2 = (encoding >> 10) & 0x1f;
        bool signedWord = opc == 1;

        if (opc == 3) return Arm64Decoder::Result::Unallocated;
        if (signedWord && !load) return Arm64Decoder::Result::Unallocated;
        if (mode == 0) return Arm64Decoder::Result::Unimplemented;

        uint32_t scale = (opc == 2 || signedWord) ? 3 : 2;
        if (signedWord) scale = 2;
        Ir::ValueType accessType = (opc == 2) ? Ir::ValueType::Int64 : Ir::ValueType::Int32;
        int64_t immediate = (int64_t)SignExtend((encoding >> 15) & 0x7f, 7) * (1ll << scale);

        bool preIndex = mode == 3;
        bool postIndex = mode == 1;
        int64_t applied = postIndex ? 0 : immediate;

        Ir::Value pointer = EmitAddress(function, block, address, rn, applied);
        int width = Ir::SizeInBytes(accessType);
        Ir::Value second = EmitAddress(function, block, address, rn, applied + width);

        if (load)
        {
            Ir::Value first = EmitLoad(function, block, address, pointer, accessType, Ir::MemoryOrder::Relaxed);
            Ir::Value other = EmitLoad(function, block, address, second, accessType, Ir::MemoryOrder::Relaxed);
            EmitExtendedWrite(function, block, address, rt, first, scale, signedWord ? 2 : 1, opc == 2 || signedWord);
            EmitExtendedWrite(function, block, address, rt2, other, scale, signedWord ? 2 : 1, opc == 2 || signedWord);
        }
        else
        {
            Ir::Value first = EmitReadGeneral(function, block, address, rt, accessType, false);
            Ir::Value other = EmitReadGeneral(function, block, address, rt2, accessType, false);
            EmitStore(block, address, pointer, first, accessType, Ir::MemoryOrder::Relaxed);
            EmitStore(block, address, second, other, accessType, Ir::MemoryOrder::Relaxed);
        }

        if (preIndex || postIndex)
        {
            Ir::Value updated = EmitAddress(function, block, address, rn, immediate);
            EmitWriteGeneral(block, address, rn, updated, Ir::ValueType::Int64, true);
        }

        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unimplemented;
}

static Arm64Decoder::Result DecodeException(Ir::Function& function, Ir::Block& block, uint32_t encoding, GuestState::GuestAddress address)
{
    (void)function;

    uint32_t opc = (encoding >> 21) & 0x7;
    uint32_t op2 = (encoding >> 2) & 0x7;
    uint32_t ll = encoding & 0x3;
    uint32_t immediate = (encoding >> 5) & 0xFFFF;

    if (op2 != 0) return Arm64Decoder::Result::Unallocated;

    if (opc == 0 && ll == 1)
    {
        Ir::Instruction& call = Append(block, Ir::Opcode::Syscall, address, Ir::ValueType::Int64);
        call.immediate = immediate;
        return Arm64Decoder::Result::Ok;
    }

    if ((opc == 1 || opc == 2) && ll == 0)
    {
        Ir::Instruction& trap = Append(block, Ir::Opcode::Undefined, address, Ir::ValueType::Int64);
        trap.immediate = immediate;
        return Arm64Decoder::Result::Ok;
    }

    return Arm64Decoder::Result::Unallocated;
}

Arm64Decoder::Result Arm64Decoder::DecodeInstruction(Context& context, uint32_t encoding, GuestState::GuestAddress address, Ir::Function& function,
                                                     Ir::Block& block)
{
    context.statistics.instructionsDecoded += 1;

    if (block.instructions.capacity() - block.instructions.size() < MAX_EMITTED_PER_INSTRUCTION)
    {
        block.instructions.reserve(block.instructions.size() + MAX_EMITTED_PER_INSTRUCTION * 8);
    }

    uint32_t op0 = (encoding >> 25) & 0xf;
    Result result = Result::Unimplemented;

    if (encoding == 0)
    {
        Append(block, Ir::Opcode::Undefined, address, Ir::ValueType::Int64);
        context.statistics.unallocatedEncountered += 1;
        return Result::Unallocated;
    }

    if ((encoding & 0xFF000000u) == 0xD4000000u) result = DecodeException(function, block, encoding, address);
    else if ((op0 & 0xe) == 0x8) result = DecodeDataProcessingImmediate(function, block, encoding, address);
    else if ((op0 & 0xe) == 0xa) result = DecodeBranch(function, block, encoding, address);
    else if ((op0 & 0x5) == 0x4) result = DecodeLoadStore(function, block, encoding, address);
    else if ((op0 & 0x7) == 0x7) result = DecodeFloatingPoint(function, block, encoding, address);
    else if ((op0 & 0x5) == 0x5) result = DecodeDataProcessingRegister(function, block, encoding, address);

    if (result == Result::Unallocated) context.statistics.unallocatedEncountered += 1;
    if (result == Result::Unimplemented) context.statistics.unimplementedEncountered += 1;

    return result;
}

Arm64Decoder::Result Arm64Decoder::DecodeBlock(Context& context, GuestState::GuestAddress address, Ir::Function& function, Ir::Block& block)
{
    block.guestAddress = address;
    uint64_t cursor = static_cast<uint64_t>(address);
    uint64_t fetched = 0;

    for (;;)
    {
        if (context.maxBlockInstructions != 0 && fetched >= context.maxBlockInstructions) return Result::BlockTooLong;
        fetched += 1;

        uint32_t encoding = 0;
        if (!context.memory.Read || !context.memory.Read(context.memory.context, static_cast<GuestState::GuestAddress>(cursor), &encoding,
                                                         sizeof(encoding)))
        {
            return Result::FetchFailed;
        }

        size_t emittedBefore = block.instructions.size();
        Result result = DecodeInstruction(context, encoding, static_cast<GuestState::GuestAddress>(cursor), function, block);

        if (result != Result::Ok && context.trapOnUndecodable)
        {
            block.instructions.resize(emittedBefore);

            Ir::Instruction& trap = Append(block, Ir::Opcode::Undefined, static_cast<GuestState::GuestAddress>(cursor),
                                           Ir::ValueType::Int64);
            trap.immediate = encoding;

            if (context.reportUndecodable)
            {
                fprintf(stderr, "[arm64decoder] cannot decode %08x at %016llx\n", encoding, (unsigned long long)cursor);
            }

            context.statistics.trapsEmitted += 1;
            block.guestEndAddress = static_cast<GuestState::GuestAddress>(cursor + INSTRUCTION_SIZE_BYTES);

            return Result::Ok;
        }

        if (result != Result::Ok) return result;

        cursor += INSTRUCTION_SIZE_BYTES;
        block.guestEndAddress = static_cast<GuestState::GuestAddress>(cursor);

        if (!block.instructions.empty() && Ir::IsTerminator(block.instructions.back().opcode)) return Result::Ok;

        if (context.regionEndAddress != 0 && cursor >= context.regionEndAddress)
        {
            block.successorFallthrough = static_cast<GuestState::GuestAddress>(cursor);
            return Result::Ok;
        }

        if (context.blockLeaders && context.blockLeaders->count(cursor) != 0)
        {
            block.successorFallthrough = static_cast<GuestState::GuestAddress>(cursor);
            return Result::Ok;
        }
    }
}

static bool WithinRegion(uint64_t address, uint64_t entry, uint64_t end)
{
    return address >= entry && address < end;
}

static void CollectSuccessors(const Ir::Block& block, uint64_t entry, uint64_t end, std::unordered_set<uint64_t>& leaders,
                              std::vector<uint64_t>& pending)
{
    const GuestState::GuestAddress successors[2] = {block.successorTaken, block.successorFallthrough};

    for (GuestState::GuestAddress successor : successors)
    {
        if (successor == Ir::NO_SUCCESSOR) continue;

        uint64_t target = static_cast<uint64_t>(successor);
        if (!WithinRegion(target, entry, end)) continue;
        if (!leaders.insert(target).second) continue;

        pending.push_back(target);
    }
}

Arm64Decoder::Result Arm64Decoder::DecodeFunction(Context& context, GuestState::GuestAddress entryAddress, GuestState::GuestAddress endAddress,
                                                  Ir::Function& function)
{
    uint64_t entry = static_cast<uint64_t>(entryAddress);
    uint64_t end = static_cast<uint64_t>(endAddress);
    if (end <= entry) return Result::FetchFailed;

    const std::unordered_set<uint64_t>* outerLeaders = context.blockLeaders;
    uint64_t outerEnd = context.regionEndAddress;

    context.regionEndAddress = end;

    std::unordered_set<uint64_t> leaders;
    context.blockLeaders = &leaders;
    std::unordered_set<uint64_t> scanned;
    std::vector<uint64_t> pending;

    leaders.insert(entry);
    pending.push_back(entry);

    if (outerLeaders)
    {
        for (uint64_t requested : *outerLeaders)
        {
            if (requested < entry || requested >= end) continue;
            if ((requested & 3) != 0) continue;
            if (!leaders.insert(requested).second) continue;

            pending.push_back(requested);
        }
    }

    while (!pending.empty())
    {
        uint64_t start = pending.back();
        pending.pop_back();
        if (!scanned.insert(start).second) continue;

        Ir::Function scratch;
        Ir::Block block;

        Result result = DecodeBlock(context, static_cast<GuestState::GuestAddress>(start), scratch, block);
        if (result != Result::Ok)
        {
            context.blockLeaders = outerLeaders;
            context.regionEndAddress = outerEnd;
            return result;
        }

        CollectSuccessors(block, entry, end, leaders, pending);
    }

    std::vector<uint64_t> starts(leaders.begin(), leaders.end());
    std::sort(starts.begin(), starts.end());

    context.blockLeaders = &leaders;

    function.entryAddress = entryAddress;
    function.endAddress = endAddress;
    function.blocks.clear();
    function.blocks.reserve(starts.size());

    Result outcome = Result::Ok;

    for (uint64_t start : starts)
    {
        Ir::Block block;

        outcome = DecodeBlock(context, static_cast<GuestState::GuestAddress>(start), function, block);
        if (outcome != Result::Ok) break;

        function.blocks.push_back(std::move(block));
    }

    context.blockLeaders = outerLeaders;
    context.regionEndAddress = outerEnd;

    return outcome;
}

void Arm64Decoder::ResetStatistics(Context& context)
{
    context.statistics = Statistics();
}

const char* Arm64Decoder::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::Unallocated: return "Unallocated";
        case Result::Unimplemented: return "Unimplemented";
        case Result::FetchFailed: return "FetchFailed";
        case Result::BlockTooLong: return "BlockTooLong";
    }
    return "Unknown";
}

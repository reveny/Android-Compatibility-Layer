#include "GuestState/GuestState.hpp"

#include <cstdio>
#include <cstring>
#include <type_traits>

static_assert(std::has_unique_object_representations<GuestState::Registers>::value,
              "Registers must have no padding holes; Equals compares it with memcmp");

void GuestState::Reset(Registers& registers)
{
    registers = Registers();
}

bool GuestState::Equals(const Registers& left, const Registers& right)
{
    return memcmp(&left, &right, sizeof(Registers)) == 0;
}

void GuestState::Dump(const Registers& registers)
{
    for (int index = 0; index < GENERAL_REGISTER_COUNT; ++index)
    {
        fprintf(stderr, "x%-2d = %016llx%s", index, (unsigned long long)registers.general[index], (index % 4) == 3 ? "\n" : "  ");
    }

    fprintf(stderr, "\nsp  = %016llx  pc  = %016llx  tp  = %016llx\n", (unsigned long long)registers.stackPointer,
            (unsigned long long)registers.programCounter, (unsigned long long)registers.threadPointer);
    fprintf(stderr, "nzcv= %08x  fpcr= %08x  fpsr= %08x\n", registers.flagsNzcv, registers.floatingPointControl,
            registers.floatingPointStatus);

    for (int index = 0; index < VECTOR_REGISTER_COUNT; ++index)
    {
        fprintf(stderr, "v%-2d = %016llx:%016llx%s", index, (unsigned long long)registers.vector[index].high,
                (unsigned long long)registers.vector[index].low, (index % 2) == 1 ? "\n" : "  ");
    }

    if (registers.exclusive.valid)
    {
        fprintf(stderr, "monitor address=%016llx width=%u\n", (unsigned long long)registers.exclusive.address, registers.exclusive.widthBytes);
    }

    fflush(stderr);
}

bool GuestState::ConditionHolds(uint32_t flagsNzcv, uint8_t condition)
{
    bool negative = (flagsNzcv & FLAG_NEGATIVE) != 0;
    bool zero = (flagsNzcv & FLAG_ZERO) != 0;
    bool carry = (flagsNzcv & FLAG_CARRY) != 0;
    bool overflow = (flagsNzcv & FLAG_OVERFLOW) != 0;

    bool result = false;
    switch ((condition >> 1) & 0x7)
    {
        case 0: result = zero; break;
        case 1: result = carry; break;
        case 2: result = negative; break;
        case 3: result = overflow; break;
        case 4: result = carry && !zero; break;
        case 5: result = negative == overflow; break;
        case 6: result = (negative == overflow) && !zero; break;
        case 7: result = true; break;
        default: result = false; break;
    }

    if ((condition & 1) != 0 && (condition & 0xF) != 0xF) result = !result;
    return result;
}

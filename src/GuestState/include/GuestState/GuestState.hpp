#pragma once

#include <cstdint>

namespace GuestState
{
    inline constexpr int GENERAL_REGISTER_COUNT = 31;
    inline constexpr int VECTOR_REGISTER_COUNT = 32;

    inline constexpr uint32_t FLAG_NEGATIVE = 1u << 31;
    inline constexpr uint32_t FLAG_ZERO = 1u << 30;
    inline constexpr uint32_t FLAG_CARRY = 1u << 29;
    inline constexpr uint32_t FLAG_OVERFLOW = 1u << 28;

    inline constexpr uint32_t FPSR_INVALID_OPERATION = 1u << 0;
    inline constexpr uint32_t FPSR_DIVIDE_BY_ZERO = 1u << 1;
    inline constexpr uint32_t FPSR_OVERFLOW = 1u << 2;
    inline constexpr uint32_t FPSR_UNDERFLOW = 1u << 3;
    inline constexpr uint32_t FPSR_INEXACT = 1u << 4;
    inline constexpr uint32_t FPSR_INPUT_DENORMAL = 1u << 7;
    inline constexpr uint32_t FPSR_SATURATION = 1u << 27;
    inline constexpr uint32_t FPSR_CAPTURED_BITS = 0x0800009Fu;

    enum class GuestAddress : uint64_t;

    struct VectorRegister
    {
        uint64_t low = 0;
        uint64_t high = 0;
    };

    struct ExclusiveMonitor
    {
        uint64_t address = 0;
        uint64_t valueLow = 0;
        uint64_t valueHigh = 0;
        uint32_t widthBytes = 0;
        uint8_t valid = 0;
        uint8_t reservedPadding[3] = {0, 0, 0};
    };

    struct Registers
    {
        uint64_t general[GENERAL_REGISTER_COUNT] = {};
        uint64_t stackPointer = 0;
        uint64_t programCounter = 0;
        uint64_t threadPointer = 0;
        VectorRegister vector[VECTOR_REGISTER_COUNT] = {};
        ExclusiveMonitor exclusive;
        uint32_t flagsNzcv = 0;
        uint32_t floatingPointControl = 0;
        uint32_t floatingPointStatus = 0;
        uint32_t reservedPadding = 0;
    };

    struct MemoryAccessor
    {
        bool (*Read)(void* context, GuestAddress address, void* destination, uint64_t length) = nullptr;
        bool (*Write)(void* context, GuestAddress address, const void* source, uint64_t length) = nullptr;
        void* (*TranslateForAtomic)(void* context, GuestAddress address, uint64_t length) = nullptr;
        void* context = nullptr;
    };

    struct GuestCaller
    {
        bool (*EnterGuest)(void* context, GuestAddress guestAddress, Registers& registers) = nullptr;
        bool (*EnterGuestWithStack)(void* context, GuestAddress guestAddress, Registers& registers, const uint64_t* stackWords,
                                    uint32_t stackWordCount) = nullptr;
        void* context = nullptr;
    };

    void Reset(Registers& registers);
    bool Equals(const Registers& left, const Registers& right);
    void Dump(const Registers& registers);
    bool ConditionHolds(uint32_t flagsNzcv, uint8_t condition);
}

#pragma once

#include <cstddef>
#include <cstdint>
#include <unordered_set>

#include "GuestState/GuestState.hpp"
#include "Ir/Ir.hpp"

namespace Arm64Decoder
{
    inline constexpr int INSTRUCTION_SIZE_BYTES = 4;
    inline constexpr size_t MAX_EMITTED_PER_INSTRUCTION = 64;
    inline constexpr uint64_t DEFAULT_MAX_BLOCK_INSTRUCTIONS = 4096;

    enum class Result : uint8_t
    {
        Ok,
        Unallocated,
        Unimplemented,
        FetchFailed,
        BlockTooLong
    };

    struct Statistics
    {
        uint64_t instructionsDecoded = 0;
        uint64_t unallocatedEncountered = 0;
        uint64_t unimplementedEncountered = 0;
        uint64_t trapsEmitted = 0;
    };

    struct Context
    {
        GuestState::MemoryAccessor memory;
        Statistics statistics;
        uint64_t maxBlockInstructions = DEFAULT_MAX_BLOCK_INSTRUCTIONS;
        const std::unordered_set<uint64_t>* blockLeaders = nullptr;
        uint64_t regionEndAddress = 0;
        bool trapOnUndecodable = false;
        bool reportUndecodable = false;
    };

    void ResetStatistics(Context& context);

    Result DecodeInstruction(Context& context, uint32_t encoding, GuestState::GuestAddress address, Ir::Function& function, Ir::Block& block);
    Result DecodeBlock(Context& context, GuestState::GuestAddress address, Ir::Function& function, Ir::Block& block);
    Result DecodeFunction(Context& context, GuestState::GuestAddress entryAddress, GuestState::GuestAddress endAddress, Ir::Function& function);

    const char* ResultName(Result result);
}

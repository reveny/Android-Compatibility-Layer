#pragma once

#include <cstdint>
#include <vector>

#include "ElfLoader/ElfLoader.hpp"
#include "GuestState/GuestState.hpp"

namespace CodeMap
{
    inline constexpr uint64_t MAX_FUNCTIONS = 1ull << 20;
    inline constexpr uint8_t EXCEPTION_TABLE_VERSION = 1;
    inline constexpr uint8_t ENCODING_UNSIGNED_WORD = 0x03;
    inline constexpr uint8_t ENCODING_PROGRAM_RELATIVE_SIGNED_WORD = 0x1b;
    inline constexpr uint8_t ENCODING_TABLE_RELATIVE_SIGNED_WORD = 0x3b;
    inline constexpr uint8_t ENCODING_OMITTED = 0xff;

    enum class Result : uint8_t
    {
        Ok,
        NoExecutableSegment,
        UnreadableExceptionTable,
        UnsupportedExceptionTable,
        TooManyFunctions
    };

    struct FunctionRange
    {
        GuestState::GuestAddress startAddress{};
        GuestState::GuestAddress endAddress{};
    };

    struct Statistics
    {
        uint64_t fromExceptionTable = 0;
        uint64_t fromSymbols = 0;
        uint64_t fromStubs = 0;
        uint64_t discarded = 0;
        uint64_t executableBytes = 0;
        bool exceptionTableUsed = false;
    };

    Result ExecutableRange(const ElfLoader::Library& library, GuestState::GuestAddress& start, GuestState::GuestAddress& end);

    Result Discover(const ElfLoader::Library& library, const GuestState::MemoryAccessor& memory, std::vector<FunctionRange>& functions,
                    Statistics& statistics);

    void AddEntryPoint(GuestState::GuestAddress address, GuestState::GuestAddress executableStart, GuestState::GuestAddress executableEnd,
                       std::vector<FunctionRange>& functions);

    const char* ResultName(Result result);
}

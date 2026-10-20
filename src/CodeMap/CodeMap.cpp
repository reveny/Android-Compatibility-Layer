#include "CodeMap/CodeMap.hpp"

#include <algorithm>
#include <cstdio>
#include <cstring>

static bool ReadGuest(const GuestState::MemoryAccessor& memory, uint64_t address, void* destination, uint64_t length)
{
    if (!memory.Read) return false;
    return memory.Read(memory.context, (GuestState::GuestAddress)address, destination, length);
}

static bool EncodedSize(uint8_t encoding, uint64_t& size)
{
    if (encoding == CodeMap::ENCODING_OMITTED)
    {
        size = 0;
        return true;
    }

    switch (encoding & 0x0f)
    {
        case 0x02:
        case 0x03: size = 4; return true;
        case 0x04: size = 8; return true;
        case 0x09:
        case 0x0a:
        case 0x0b: size = 4; return true;
        case 0x0c: size = 8; return true;
        default: break;
    }

    return false;
}

CodeMap::Result CodeMap::ExecutableRange(const ElfLoader::Library& library, GuestState::GuestAddress& start, GuestState::GuestAddress& end)
{
    uint64_t lowest = 0;
    uint64_t highest = 0;
    bool found = false;

    for (const ElfLoader::Segment& segment : library.segments)
    {
        if (!segment.executable) continue;
        if (segment.length == 0) continue;

        uint64_t segmentStart = (uint64_t)segment.guestAddress;
        uint64_t segmentEnd = segmentStart + segment.length;

        if (!found || segmentStart < lowest) lowest = segmentStart;
        if (!found || segmentEnd > highest) highest = segmentEnd;

        found = true;
    }

    if (!found) return Result::NoExecutableSegment;

    start = (GuestState::GuestAddress)lowest;
    end = (GuestState::GuestAddress)highest;

    return Result::Ok;
}

void CodeMap::AddEntryPoint(GuestState::GuestAddress address, GuestState::GuestAddress executableStart,
                            GuestState::GuestAddress executableEnd, std::vector<FunctionRange>& functions)
{
    uint64_t raw = (uint64_t)address;
    if (raw < (uint64_t)executableStart || raw >= (uint64_t)executableEnd) return;

    FunctionRange range;
    range.startAddress = address;
    range.endAddress = executableEnd;

    functions.push_back(range);
}

static CodeMap::Result CollectFromExceptionTable(const ElfLoader::Library& library, const GuestState::MemoryAccessor& memory,
                                                 GuestState::GuestAddress executableStart, GuestState::GuestAddress executableEnd,
                                                 std::vector<CodeMap::FunctionRange>& functions, CodeMap::Statistics& statistics)
{
    if (library.ehFrameAddress == GuestState::GuestAddress{} || library.ehFrameLength < 12) return CodeMap::Result::Ok;

    uint64_t base = (uint64_t)library.ehFrameAddress;

    uint8_t header[4] = {0, 0, 0, 0};
    if (!ReadGuest(memory, base, header, sizeof(header))) return CodeMap::Result::UnreadableExceptionTable;

    if (header[0] != CodeMap::EXCEPTION_TABLE_VERSION) return CodeMap::Result::UnsupportedExceptionTable;
    if (header[3] != CodeMap::ENCODING_TABLE_RELATIVE_SIGNED_WORD) return CodeMap::Result::UnsupportedExceptionTable;

    uint64_t frameSize = 0;
    uint64_t countSize = 0;
    if (!EncodedSize(header[1], frameSize)) return CodeMap::Result::UnsupportedExceptionTable;
    if (!EncodedSize(header[2], countSize)) return CodeMap::Result::UnsupportedExceptionTable;
    if (countSize != 4) return CodeMap::Result::UnsupportedExceptionTable;

    uint64_t cursor = base + sizeof(header) + frameSize;

    uint32_t count = 0;
    if (!ReadGuest(memory, cursor, &count, sizeof(count))) return CodeMap::Result::UnreadableExceptionTable;
    cursor += countSize;

    if (count > CodeMap::MAX_FUNCTIONS) return CodeMap::Result::TooManyFunctions;
    if (cursor + (uint64_t)count * 8 > base + library.ehFrameLength) return CodeMap::Result::UnreadableExceptionTable;

    for (uint32_t index = 0; index < count; ++index)
    {
        int32_t delta = 0;
        if (!ReadGuest(memory, cursor + (uint64_t)index * 8, &delta, sizeof(delta))) return CodeMap::Result::UnreadableExceptionTable;

        uint64_t address = base + (uint64_t)(int64_t)delta;
        if (address < (uint64_t)executableStart || address >= (uint64_t)executableEnd)
        {
            statistics.discarded += 1;
            continue;
        }

        CodeMap::FunctionRange range;
        range.startAddress = (GuestState::GuestAddress)address;
        range.endAddress = executableEnd;

        functions.push_back(range);
        statistics.fromExceptionTable += 1;
    }

    statistics.exceptionTableUsed = true;
    return CodeMap::Result::Ok;
}

CodeMap::Result CodeMap::Discover(const ElfLoader::Library& library, const GuestState::MemoryAccessor& memory,
                                  std::vector<FunctionRange>& functions, Statistics& statistics)
{
    functions.clear();
    statistics = Statistics();

    GuestState::GuestAddress executableStart{};
    GuestState::GuestAddress executableEnd{};

    Result ranged = ExecutableRange(library, executableStart, executableEnd);
    if (ranged != Result::Ok) return ranged;

    statistics.executableBytes = (uint64_t)executableEnd - (uint64_t)executableStart;

    Result collected = CollectFromExceptionTable(library, memory, executableStart, executableEnd, functions, statistics);
    if (collected != Result::Ok && collected != Result::UnsupportedExceptionTable) return collected;

    for (const ElfLoader::Symbol& symbol : library.exports)
    {
        if (!symbol.isDefined || !symbol.isFunction) continue;

        uint64_t address = (uint64_t)symbol.guestAddress;
        if (address < (uint64_t)executableStart || address >= (uint64_t)executableEnd)
        {
            statistics.discarded += 1;
            continue;
        }

        FunctionRange range;
        range.startAddress = symbol.guestAddress;
        range.endAddress = executableEnd;

        functions.push_back(range);
        statistics.fromSymbols += 1;
    }

    for (const ElfLoader::StubRegion& region : library.stubRegions)
    {
        if (region.entryBytes == 0) continue;

        for (uint64_t offset = 0; offset + region.entryBytes <= region.length; offset += region.entryBytes)
        {
            uint64_t address = (uint64_t)region.guestAddress + offset;
            if (address < (uint64_t)executableStart || address >= (uint64_t)executableEnd) continue;

            FunctionRange range;
            range.startAddress = (GuestState::GuestAddress)address;
            range.endAddress = (GuestState::GuestAddress)(address + region.entryBytes);

            functions.push_back(range);
            statistics.fromStubs += 1;
        }
    }

    AddEntryPoint(library.initAddress, executableStart, executableEnd, functions);
    AddEntryPoint(library.finiAddress, executableStart, executableEnd, functions);

    if (functions.size() > MAX_FUNCTIONS) return Result::TooManyFunctions;

    std::sort(functions.begin(), functions.end(),
              [](const FunctionRange& left, const FunctionRange& right) { return left.startAddress < right.startAddress; });

    functions.erase(std::unique(functions.begin(), functions.end(),
                                [](const FunctionRange& left, const FunctionRange& right)
                                { return left.startAddress == right.startAddress; }),
                    functions.end());

    for (size_t index = 0; index + 1 < functions.size(); ++index) functions[index].endAddress = functions[index + 1].startAddress;
    if (!functions.empty()) functions.back().endAddress = executableEnd;

    return Result::Ok;
}

const char* CodeMap::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::NoExecutableSegment: return "NoExecutableSegment";
        case Result::UnreadableExceptionTable: return "UnreadableExceptionTable";
        case Result::UnsupportedExceptionTable: return "UnsupportedExceptionTable";
        case Result::TooManyFunctions: return "TooManyFunctions";
    }

    return "Unknown";
}

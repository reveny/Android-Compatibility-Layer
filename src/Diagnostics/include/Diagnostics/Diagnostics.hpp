#pragma once

#include <cstdint>

namespace Diagnostics
{
    inline constexpr uint32_t MAX_TAG_BYTES = 48;
    inline constexpr uint32_t MAX_TEXT_BYTES = 512;
    inline constexpr uint32_t DEFAULT_CAPACITY = 4096;
    inline constexpr uint32_t MAX_CAPACITY = 262144;

    inline constexpr const char* FILE_SETTING = "log";
    inline constexpr const char* TOMBSTONE_SETTING = "tombstone";
    inline constexpr uint32_t TOMBSTONE_CONTEXT_LINES = 200;
    inline constexpr int TOMBSTONE_SETTLE_MILLIS = 400;

    enum class Priority : uint8_t
    {
        Verbose,
        Debug,
        Info,
        Warn,
        Error,
        Fatal
    };

    struct Record
    {
        uint64_t sequence;
        uint64_t realtimeNanos;
        Priority priority;
        char tag[MAX_TAG_BYTES];
        char text[MAX_TEXT_BYTES];
    };

    struct Statistics
    {
        uint64_t written = 0;
        uint64_t dropped = 0;
        uint64_t truncated = 0;
        uint32_t capacity = 0;
        uint32_t held = 0;
    };

    bool Initialize(uint32_t capacity);
    void Shutdown();
    bool IsInitialized();

    void Write(Priority priority, const char* tag, const char* text);
    void WriteLine(const char* line, uint32_t length);

    using ConsoleFilter = bool (*)(const char* line, uint32_t length);

    void SetConsoleFilter(ConsoleFilter filter);

    bool CaptureStandardError();
    void ReleaseStandardError();

    uint32_t Read(uint64_t& cursor, Record* records, uint32_t capacity);
    uint64_t NextSequence();

    bool Dump(const char* path);
    bool WriteTombstone(const char* path, uint64_t faultSequence, uint32_t contextLines);
    Statistics GetStatistics();

    inline constexpr uint32_t MAX_GAPS = 512;
    inline constexpr uint32_t MAX_GAP_NAME_BYTES = 160;

    enum class Gap : uint8_t
    {
        FrameworkMethod,
        JniSlot,
        GuestSymbol,
        Syscall,
        Instruction,
        Other
    };

    struct GapRecord
    {
        Gap kind;
        uint64_t count;
        char name[MAX_GAP_NAME_BYTES];
    };

    void NoteGap(Gap kind, const char* name);
    uint32_t ReadGaps(GapRecord* records, uint32_t capacity);
    uint32_t DistinctGaps();
    uint64_t TotalGapHits();
    void ReportGaps();

    const char* GapName(Gap kind);

    char PriorityLetter(Priority priority);
    const char* PriorityName(Priority priority);
}

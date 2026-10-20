#include "Diagnostics/Diagnostics.hpp"

#include <algorithm>
#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <mutex>
#include <thread>
#include <vector>

#include <cerrno>
#include <fcntl.h>
#include <poll.h>
#include <sys/eventfd.h>
#include <unistd.h>

static std::mutex gGuard;
static std::vector<Diagnostics::Record> gRecords;
static uint64_t gNextSequence = 1;
static uint64_t gWritten = 0;
static uint64_t gDropped = 0;
static uint64_t gTruncated = 0;

static int gOriginalStandardError = -1;
static Diagnostics::ConsoleFilter gConsoleFilter = nullptr;

static void ForwardToConsole(const char* line, uint32_t length)
{
    if (gOriginalStandardError < 0) return;
    if (gConsoleFilter && !gConsoleFilter(line, length)) return;

    uint32_t offset = 0;

    while (offset < length)
    {
        const ssize_t written = ::write(gOriginalStandardError, line + offset, (size_t)(length - offset));
        if (written <= 0) break;

        offset += (uint32_t)written;
    }

    const char newline = '\n';
    const ssize_t ended = ::write(gOriginalStandardError, &newline, 1);
    (void)ended;
}

void Diagnostics::SetConsoleFilter(Diagnostics::ConsoleFilter filter)
{
    gConsoleFilter = filter;
}
static int gCaptureRead = -1;
static int gCaptureWake = -1;
static std::thread gReader;
static std::atomic_bool gReaderRunning{false};

static uint64_t RealtimeNanos()
{
    timespec now{};
    clock_gettime(CLOCK_REALTIME, &now);
    return (uint64_t)now.tv_sec * 1000000000ull + (uint64_t)now.tv_nsec;
}

static bool PriorityFromLetter(char letter, Diagnostics::Priority& priority)
{
    switch (letter)
    {
        case 'V': priority = Diagnostics::Priority::Verbose; return true;
        case 'D': priority = Diagnostics::Priority::Debug; return true;
        case 'I': priority = Diagnostics::Priority::Info; return true;
        case 'W': priority = Diagnostics::Priority::Warn; return true;
        case 'E': priority = Diagnostics::Priority::Error; return true;
        case 'F': priority = Diagnostics::Priority::Fatal; return true;
        default: return false;
    }
}

static void CopyBounded(char* destination, uint32_t capacity, const char* source, uint32_t length)
{
    if (capacity == 0) return;

    if (length >= capacity)
    {
        length = capacity - 1;
        gTruncated += 1;
    }

    if (source && length > 0) memcpy(destination, source, length);

    destination[length] = '\0';
}

static bool ParseShortForm(const char* line, uint32_t length, Diagnostics::Priority& priority, const char*& tag, uint32_t& tagLength,
                           const char*& text, uint32_t& textLength)
{
    if (length < 4 || line[1] != '/') return false;
    if (!PriorityFromLetter(line[0], priority)) return false;

    for (uint32_t index = 2; index + 1 < length; ++index)
    {
        if (line[index] != ':' || line[index + 1] != ' ') continue;

        tag = line + 2;
        tagLength = index - 2;
        text = line + index + 2;
        textLength = length - index - 2;
        return tagLength > 0;
    }

    return false;
}

static bool ParseModuleForm(const char* line, uint32_t length, const char*& tag, uint32_t& tagLength, const char*& text,
                            uint32_t& textLength)
{
    if (length < 3 || line[0] != '[') return false;

    for (uint32_t index = 1; index < length; ++index)
    {
        if (line[index] != ']') continue;

        tag = line + 1;
        tagLength = index - 1;

        uint32_t start = index + 1;
        while (start < length && line[start] == ' ') start += 1;

        text = line + start;
        textLength = length - start;
        return tagLength > 0;
    }

    return false;
}

static bool ParseLongForm(const char* line, uint32_t length, Diagnostics::Priority& priority, const char*& tag, uint32_t& tagLength,
                          const char*& text, uint32_t& textLength)
{
    if (length < 20) return false;
    if (line[0] < '0' || line[0] > '9' || line[2] != '-') return false;

    uint32_t index = 0;

    while (index + 2 < length)
    {
        const bool isolated = line[index] == ' ' && line[index + 2] == ' ';

        Diagnostics::Priority parsed = Diagnostics::Priority::Info;
        if (isolated && PriorityFromLetter(line[index + 1], parsed))
        {
            priority = parsed;
            index += 3;
            break;
        }

        index += 1;
    }

    if (index + 2 >= length) return false;

    const uint32_t tagStart = index;

    while (index < length && line[index] != ':') index += 1;
    if (index >= length) return false;

    uint32_t tagEnd = index;
    while (tagEnd > tagStart && line[tagEnd - 1] == ' ') tagEnd -= 1;

    uint32_t textStart = index + 1;
    while (textStart < length && line[textStart] == ' ') textStart += 1;

    tag = line + tagStart;
    tagLength = tagEnd - tagStart;
    text = line + textStart;
    textLength = length - textStart;

    return tagLength > 0;
}

bool Diagnostics::Initialize(uint32_t capacity)
{
    std::lock_guard<std::mutex> held(gGuard);

    if (!gRecords.empty()) return true;
    if (capacity == 0 || capacity > MAX_CAPACITY) capacity = DEFAULT_CAPACITY;

    gRecords.assign(capacity, Record{});
    gNextSequence = 1;
    gWritten = 0;
    gDropped = 0;
    gTruncated = 0;

    return true;
}

void Diagnostics::Shutdown()
{
    ReleaseStandardError();

    std::lock_guard<std::mutex> held(gGuard);
    gRecords.clear();
}

bool Diagnostics::IsInitialized()
{
    std::lock_guard<std::mutex> held(gGuard);
    return !gRecords.empty();
}

void Diagnostics::Write(Priority priority, const char* tag, const char* text)
{
    std::lock_guard<std::mutex> held(gGuard);

    if (gRecords.empty()) return;

    Record& record = gRecords[(size_t)((gNextSequence - 1) % gRecords.size())];

    if (record.sequence != 0) gDropped += 1;

    record.sequence = gNextSequence;
    record.realtimeNanos = RealtimeNanos();
    record.priority = priority;

    CopyBounded(record.tag, MAX_TAG_BYTES, tag, tag ? (uint32_t)strlen(tag) : 0);
    CopyBounded(record.text, MAX_TEXT_BYTES, text, text ? (uint32_t)strlen(text) : 0);

    gNextSequence += 1;
    gWritten += 1;
}

void Diagnostics::WriteLine(const char* line, uint32_t length)
{
    if (!line || length == 0) return;

    Priority priority = Priority::Info;
    const char* tag = nullptr;
    uint32_t tagLength = 0;
    const char* text = nullptr;
    uint32_t textLength = 0;

    if (!ParseShortForm(line, length, priority, tag, tagLength, text, textLength) &&
        !ParseLongForm(line, length, priority, tag, tagLength, text, textLength) &&
        !ParseModuleForm(line, length, tag, tagLength, text, textLength))
    {
        priority = Priority::Info;
        tag = "stderr";
        tagLength = 6;
        text = line;
        textLength = length;
    }

    std::lock_guard<std::mutex> held(gGuard);

    if (gRecords.empty()) return;

    Record& record = gRecords[(size_t)((gNextSequence - 1) % gRecords.size())];

    if (record.sequence != 0) gDropped += 1;

    record.sequence = gNextSequence;
    record.realtimeNanos = RealtimeNanos();
    record.priority = priority;

    CopyBounded(record.tag, MAX_TAG_BYTES, tag, tagLength);
    CopyBounded(record.text, MAX_TEXT_BYTES, text, textLength);

    gNextSequence += 1;
    gWritten += 1;
}

static void ReadCapturedOutput()
{
    std::vector<char> pending;
    char buffer[4096];
    bool stopping = false;

    for (;;)
    {
        pollfd watched[2]{};
        watched[0].fd = gCaptureRead;
        watched[0].events = POLLIN;
        watched[1].fd = gCaptureWake;
        watched[1].events = POLLIN;

        const int ready = poll(watched, 2, -1);

        if (ready < 0 && errno == EINTR) continue;
        if (ready < 0) break;

        if ((watched[1].revents & POLLIN) != 0) stopping = true;

        if ((watched[0].revents & (POLLIN | POLLHUP)) == 0)
        {
            if (stopping) break;
            continue;
        }

        const ssize_t read = ::read(gCaptureRead, buffer, sizeof(buffer));

        if (read == 0) break;

        if (read < 0)
        {
            if (errno == EINTR) continue;
            if (errno == EAGAIN || errno == EWOULDBLOCK)
            {
                if (stopping) break;
                continue;
            }

            break;
        }

        for (ssize_t index = 0; index < read; ++index)
        {
            if (buffer[index] != '\n')
            {
                if (pending.size() < Diagnostics::MAX_TEXT_BYTES * 4) pending.push_back(buffer[index]);
                continue;
            }

            ForwardToConsole(pending.data(), (uint32_t)pending.size());
            Diagnostics::WriteLine(pending.data(), (uint32_t)pending.size());
            pending.clear();
        }
    }

    if (!pending.empty())
    {
        ForwardToConsole(pending.data(), (uint32_t)pending.size());
        Diagnostics::WriteLine(pending.data(), (uint32_t)pending.size());
    }

    gReaderRunning = false;
}

bool Diagnostics::CaptureStandardError()
{
    if (gCaptureRead >= 0) return true;
    if (!IsInitialized()) return false;

    int channel[2] = {-1, -1};
    if (pipe(channel) != 0) return false;

    gCaptureWake = eventfd(0, EFD_CLOEXEC | EFD_NONBLOCK);

    gOriginalStandardError = dup(STDERR_FILENO);

    if (gCaptureWake < 0 || gOriginalStandardError < 0 || dup2(channel[1], STDERR_FILENO) < 0)
    {
        if (gCaptureWake >= 0) close(gCaptureWake);
        close(channel[0]);
        close(channel[1]);
        gCaptureWake = -1;
        return false;
    }

    close(channel[1]);
    gCaptureRead = channel[0];

    setvbuf(stderr, nullptr, _IOLBF, 0);

    gReaderRunning = true;
    gReader = std::thread(&ReadCapturedOutput);

    return true;
}

void Diagnostics::ReleaseStandardError()
{
    if (gCaptureRead < 0) return;

    if (gOriginalStandardError >= 0)
    {
        fflush(stderr);
        dup2(gOriginalStandardError, STDERR_FILENO);
    }

    if (gCaptureWake >= 0)
    {
        const uint64_t one = 1;
        const ssize_t woke = write(gCaptureWake, &one, sizeof(one));
        (void)woke;
    }

    if (gReader.joinable()) gReader.join();

    close(gCaptureRead);
    gCaptureRead = -1;

    if (gCaptureWake >= 0)
    {
        close(gCaptureWake);
        gCaptureWake = -1;
    }

    if (gOriginalStandardError >= 0)
    {
        close(gOriginalStandardError);
        gOriginalStandardError = -1;
    }
}

uint32_t Diagnostics::Read(uint64_t& cursor, Record* records, uint32_t capacity)
{
    if (!records || capacity == 0) return 0;

    std::lock_guard<std::mutex> held(gGuard);

    if (gRecords.empty()) return 0;

    const uint64_t oldest = gNextSequence > gRecords.size() ? gNextSequence - gRecords.size() : 1;
    if (cursor < oldest) cursor = oldest;

    uint32_t produced = 0;

    while (cursor < gNextSequence && produced < capacity)
    {
        const Record& record = gRecords[(size_t)((cursor - 1) % gRecords.size())];

        if (record.sequence == cursor)
        {
            records[produced] = record;
            produced += 1;
        }

        cursor += 1;
    }

    return produced;
}

uint64_t Diagnostics::NextSequence()
{
    std::lock_guard<std::mutex> held(gGuard);
    return gNextSequence;
}

bool Diagnostics::Dump(const char* path)
{
    if (!path || path[0] == '\0') return false;

    FILE* out = fopen(path, "wb");
    if (!out) return false;

    uint64_t cursor = 0;
    Record batch[64];

    for (;;)
    {
        const uint32_t produced = Read(cursor, batch, 64);
        if (produced == 0) break;

        for (uint32_t index = 0; index < produced; ++index)
        {
            const Record& record = batch[index];
            const time_t seconds = (time_t)(record.realtimeNanos / 1000000000ull);
            const unsigned millis = (unsigned)((record.realtimeNanos / 1000000ull) % 1000ull);

            tm parts{};
            char stamp[32] = "00-00 00:00:00";

            if (localtime_r(&seconds, &parts)) strftime(stamp, sizeof(stamp), "%m-%d %H:%M:%S", &parts);

            fprintf(out, "%s.%03u %c/%s: %s\n", stamp, millis, PriorityLetter(record.priority), record.tag, record.text);
        }
    }

    fclose(out);
    return true;
}

bool Diagnostics::WriteTombstone(const char* path, uint64_t faultSequence, uint32_t contextLines)
{
    if (!path || path[0] == '\0') return false;
    if (!IsInitialized()) return false;

    uint64_t settled = NextSequence();

    for (int waited = 0; waited < TOMBSTONE_SETTLE_MILLIS; waited += 20)
    {
        timespec pause{};
        pause.tv_nsec = 20 * 1000 * 1000;
        nanosleep(&pause, nullptr);

        const uint64_t now = NextSequence();
        if (now == settled) break;

        settled = now;
    }

    FILE* out = fopen(path, "wb");
    if (!out) return false;

    const time_t seconds = (time_t)(RealtimeNanos() / 1000000000ull);
    tm parts{};
    char stamp[64] = "unknown time";

    if (localtime_r(&seconds, &parts)) strftime(stamp, sizeof(stamp), "%Y-%m-%d %H:%M:%S", &parts);

    fprintf(out, "acl tombstone, written %s\n", stamp);
    fprintf(out, "the fault is at log line %llu; the %u line(s) before it are kept for context\n\n",
            (unsigned long long)faultSequence, contextLines);

    uint64_t cursor = faultSequence > contextLines ? faultSequence - contextLines : 0;
    Record batch[64];
    uint64_t written = 0;

    for (;;)
    {
        const bool crossing = cursor <= faultSequence;
        const uint32_t produced = Read(cursor, batch, 64);
        if (produced == 0) break;

        for (uint32_t index = 0; index < produced; ++index)
        {
            const Record& record = batch[index];

            if (crossing && record.sequence == faultSequence) fprintf(out, "--- the fault begins here ---\n");

            const time_t line = (time_t)(record.realtimeNanos / 1000000000ull);
            const unsigned millis = (unsigned)((record.realtimeNanos / 1000000ull) % 1000ull);

            tm lineParts{};
            char lineStamp[32] = "00-00 00:00:00";

            if (localtime_r(&line, &lineParts)) strftime(lineStamp, sizeof(lineStamp), "%m-%d %H:%M:%S", &lineParts);

            fprintf(out, "%s.%03u %c/%s: %s\n", lineStamp, millis, PriorityLetter(record.priority), record.tag, record.text);
            written += 1;
        }
    }

    fprintf(out, "\n%llu line(s) written\n", (unsigned long long)written);
    fclose(out);

    return written > 0;
}

Diagnostics::Statistics Diagnostics::GetStatistics()
{
    std::lock_guard<std::mutex> held(gGuard);

    Statistics statistics;
    statistics.written = gWritten;
    statistics.dropped = gDropped;
    statistics.truncated = gTruncated;
    statistics.capacity = (uint32_t)gRecords.size();
    statistics.held = gRecords.empty() ? 0 : (uint32_t)(gNextSequence - 1 < gRecords.size() ? gNextSequence - 1 : gRecords.size());

    return statistics;
}

static std::mutex gGapGuard;
static std::vector<Diagnostics::GapRecord> gGaps;
static uint64_t gGapHits = 0;
static uint64_t gGapsBeyondCapacity = 0;

void Diagnostics::NoteGap(Gap kind, const char* name)
{
    if (!name || name[0] == '\0') return;

    std::lock_guard<std::mutex> held(gGapGuard);

    gGapHits += 1;

    for (GapRecord& record : gGaps)
    {
        if (record.kind != kind || strncmp(record.name, name, MAX_GAP_NAME_BYTES - 1) != 0) continue;

        record.count += 1;
        return;
    }

    if (gGaps.size() >= MAX_GAPS)
    {
        gGapsBeyondCapacity += 1;
        return;
    }

    GapRecord created{};
    created.kind = kind;
    created.count = 1;

    const size_t length = strnlen(name, MAX_GAP_NAME_BYTES - 1);
    memcpy(created.name, name, length);
    created.name[length] = '\0';

    gGaps.push_back(created);
}

uint32_t Diagnostics::ReadGaps(GapRecord* records, uint32_t capacity)
{
    if (!records || capacity == 0) return 0;

    std::lock_guard<std::mutex> held(gGapGuard);

    uint32_t produced = 0;

    for (const GapRecord& record : gGaps)
    {
        if (produced >= capacity) break;

        records[produced] = record;
        produced += 1;
    }

    return produced;
}

uint32_t Diagnostics::DistinctGaps()
{
    std::lock_guard<std::mutex> held(gGapGuard);
    return (uint32_t)gGaps.size();
}

uint64_t Diagnostics::TotalGapHits()
{
    std::lock_guard<std::mutex> held(gGapGuard);
    return gGapHits;
}

void Diagnostics::ReportGaps()
{
    std::vector<GapRecord> snapshot;
    uint64_t hits = 0;
    uint64_t beyond = 0;

    {
        std::lock_guard<std::mutex> held(gGapGuard);
        snapshot = gGaps;
        hits = gGapHits;
        beyond = gGapsBeyondCapacity;
    }

    if (snapshot.empty())
    {
        fprintf(stderr, "[diagnostics] this app asked for nothing this runtime does not have\n");
        return;
    }

    std::sort(snapshot.begin(), snapshot.end(), [](const GapRecord& left, const GapRecord& right) {
        if (left.kind != right.kind) return left.kind < right.kind;
        return left.count > right.count;
    });

    fprintf(stderr, "[diagnostics] this app asked for %zu thing(s) this runtime does not have, %llu time(s) in total:\n",
            snapshot.size(), (unsigned long long)hits);

    for (const GapRecord& record : snapshot)
    {
        fprintf(stderr, "[diagnostics]   %-11s %-64s x%llu\n", GapName(record.kind), record.name, (unsigned long long)record.count);
    }

    if (beyond > 0)
    {
        fprintf(stderr, "[diagnostics]   and %llu more that did not fit in the report\n", (unsigned long long)beyond);
    }
}

const char* Diagnostics::GapName(Gap kind)
{
    switch (kind)
    {
        case Gap::FrameworkMethod: return "framework";
        case Gap::JniSlot: return "jni";
        case Gap::GuestSymbol: return "guest";
        case Gap::Syscall: return "syscall";
        case Gap::Instruction: return "instruction";
        case Gap::Other: return "other";
    }

    return "unknown";
}

char Diagnostics::PriorityLetter(Priority priority)
{
    switch (priority)
    {
        case Priority::Verbose: return 'V';
        case Priority::Debug: return 'D';
        case Priority::Info: return 'I';
        case Priority::Warn: return 'W';
        case Priority::Error: return 'E';
        case Priority::Fatal: return 'F';
    }

    return '?';
}

const char* Diagnostics::PriorityName(Priority priority)
{
    switch (priority)
    {
        case Priority::Verbose: return "verbose";
        case Priority::Debug: return "debug";
        case Priority::Info: return "info";
        case Priority::Warn: return "warn";
        case Priority::Error: return "error";
        case Priority::Fatal: return "fatal";
    }

    return "unknown";
}

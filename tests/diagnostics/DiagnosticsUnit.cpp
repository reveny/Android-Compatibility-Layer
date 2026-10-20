#include <cerrno>
#include <cstdio>
#include <cstring>
#include <string>

#include <csignal>
#include <ctime>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

#include "Diagnostics/Diagnostics.hpp"

static int failures = 0;

static void Check(bool condition, const char* label)
{
    if (condition)
    {
        printf("PASS %s\n", label);
        return;
    }

    printf("FAIL %s\n", label);
    failures += 1;
}

static bool LastRecord(Diagnostics::Record& out)
{
    uint64_t cursor = 0;
    Diagnostics::Record batch[64];
    bool any = false;

    for (;;)
    {
        const uint32_t produced = Diagnostics::Read(cursor, batch, 64);
        if (produced == 0) break;

        out = batch[produced - 1];
        any = true;
    }

    return any;
}

static void CheckLine(const char* line, Diagnostics::Priority priority, const char* tag, const char* text, const char* label)
{
    Diagnostics::WriteLine(line, (uint32_t)strlen(line));

    Diagnostics::Record record{};
    if (!LastRecord(record))
    {
        printf("FAIL %s (nothing was recorded)\n", label);
        failures += 1;
        return;
    }

    const bool ok = record.priority == priority && strcmp(record.tag, tag) == 0 && strcmp(record.text, text) == 0;

    if (ok)
    {
        printf("PASS %s (%c/%s: %s)\n", label, Diagnostics::PriorityLetter(record.priority), record.tag, record.text);
        return;
    }

    printf("FAIL %s (got %c/%s: %s)\n", label, Diagnostics::PriorityLetter(record.priority), record.tag, record.text);
    failures += 1;
}

static void CheckParsing()
{
    Diagnostics::Initialize(64);

    CheckLine("I/Bootstrap: entering the main loop", Diagnostics::Priority::Info, "Bootstrap", "entering the main loop",
              "the framework's own format is understood");
    CheckLine("E/WindowBridge: the window connection ended", Diagnostics::Priority::Error, "WindowBridge",
              "the window connection ended", "and its priority survives");
    CheckLine("[launcher] the guest gets a portrait window", Diagnostics::Priority::Info, "launcher",
              "the guest gets a portrait window", "a native module line becomes a tag");
    CheckLine("08-26 12:00:35.199     2     2 I window  : heap.cc:440 Using CollectorTypeCC GC.", Diagnostics::Priority::Info,
              "window", "heap.cc:440 Using CollectorTypeCC GC.", "the runtime's long format is understood");
    CheckLine("something with no shape at all", Diagnostics::Priority::Info, "stderr", "something with no shape at all",
              "anything else is kept rather than dropped");

    Diagnostics::Shutdown();
}

static void CheckRing()
{
    Diagnostics::Initialize(8);

    for (int index = 0; index < 20; ++index)
    {
        char text[32];
        snprintf(text, sizeof(text), "line %d", index);
        Diagnostics::Write(Diagnostics::Priority::Info, "ring", text);
    }

    const Diagnostics::Statistics statistics = Diagnostics::GetStatistics();
    Check(statistics.written == 20, "every write is counted");
    Check(statistics.dropped == 12, "and the overwritten ones are counted as dropped");
    Check(statistics.held == 8, "the ring holds exactly its capacity");

    uint64_t cursor = 0;
    Diagnostics::Record batch[32];
    const uint32_t produced = Diagnostics::Read(cursor, batch, 32);

    Check(produced == 8, "a reader sees only what survives");
    Check(strcmp(batch[0].text, "line 12") == 0, "starting at the oldest survivor, not the oldest ever");
    Check(strcmp(batch[7].text, "line 19") == 0, "and ending at the newest");

    const uint32_t again = Diagnostics::Read(cursor, batch, 32);
    Check(again == 0, "a cursor that has caught up reads nothing");

    Diagnostics::Write(Diagnostics::Priority::Warn, "ring", "after");
    Check(Diagnostics::Read(cursor, batch, 32) == 1, "and picks up only what arrived since");

    Diagnostics::Shutdown();
}

static void CheckTruncation()
{
    Diagnostics::Initialize(8);

    const std::string huge(Diagnostics::MAX_TEXT_BYTES * 2, 'x');
    Diagnostics::Write(Diagnostics::Priority::Info, "long", huge.c_str());

    Diagnostics::Record record{};
    Check(LastRecord(record), "an over-long line is still recorded");
    Check(strlen(record.text) == Diagnostics::MAX_TEXT_BYTES - 1, "truncated to the record size");
    Check(Diagnostics::GetStatistics().truncated > 0, "and the truncation is counted rather than hidden");

    Diagnostics::Shutdown();
}

static void CheckCapture(const char* scratch)
{
    Diagnostics::Initialize(64);

    Check(Diagnostics::CaptureStandardError(), "standard error can be captured");

    fprintf(stderr, "[capture] a native module line\n");
    fprintf(stderr, "W/Probe: a framework line\n");
    fflush(stderr);

    for (int attempt = 0; attempt < 200 && Diagnostics::GetStatistics().written < 2; ++attempt) usleep(1000);

    Diagnostics::ReleaseStandardError();

    const Diagnostics::Statistics statistics = Diagnostics::GetStatistics();
    Check(statistics.written >= 2, "and what was written to it arrives in the buffer");

    uint64_t cursor = 0;
    Diagnostics::Record batch[16];
    const uint32_t produced = Diagnostics::Read(cursor, batch, 16);

    bool sawModule = false;
    bool sawFramework = false;

    for (uint32_t index = 0; index < produced; ++index)
    {
        if (strcmp(batch[index].tag, "capture") == 0) sawModule = true;
        if (strcmp(batch[index].tag, "Probe") == 0 && batch[index].priority == Diagnostics::Priority::Warn) sawFramework = true;
    }

    Check(sawModule, "a captured module line keeps its tag");
    Check(sawFramework, "a captured framework line keeps its tag and priority");

    std::string path = std::string(scratch) + "/diagnostics.log";
    Check(Diagnostics::Dump(path.c_str()), "the buffer dumps to a file");

    FILE* file = fopen(path.c_str(), "rb");
    Check(file != nullptr, "and that file can be read back");

    if (file)
    {
        char line[1024];
        bool found = false;

        while (fgets(line, sizeof(line), file))
        {
            if (strstr(line, "W/Probe: a framework line")) found = true;
        }

        fclose(file);
        Check(found, "with the priority and tag in the logcat shape");
    }

    Diagnostics::Shutdown();
}

static void CheckReleaseWithAnotherHolder()
{
    Diagnostics::Initialize(64);

    if (!Diagnostics::CaptureStandardError())
    {
        printf("FAIL standard error can be captured for the release test\n");
        failures += 1;
        return;
    }

    const pid_t child = fork();

    if (child == 0)
    {
        sleep(30);
        _exit(0);
    }

    if (child < 0)
    {
        printf("FAIL a child could not be forked to hold the write end\n");
        failures += 1;
        Diagnostics::ReleaseStandardError();
        Diagnostics::Shutdown();
        return;
    }

    const time_t started = time(nullptr);
    Diagnostics::ReleaseStandardError();
    const time_t took = time(nullptr) - started;

    Check(took < 5, "releasing does not wait for a forked child that still holds the write end");

    kill(child, SIGKILL);
    int state = 0;
    waitpid(child, &state, 0);

    Diagnostics::Shutdown();
}

int main(int argc, char** argv)
{
    const char* scratch = argc > 1 ? argv[1] : "/tmp";

    if (mkdir(scratch, 0755) != 0 && errno != EEXIST)
    {
        fprintf(stderr, "diagnostics: cannot create the scratch directory %s\n", scratch);
        return 2;
    }

    CheckParsing();
    CheckRing();
    CheckTruncation();
    CheckCapture(scratch);
    CheckReleaseWithAnotherHolder();

    printf("%s: %d failure(s)\n", failures == 0 ? "diagnostics" : "diagnostics FAILED", failures);

    return failures == 0 ? 0 : 1;
}

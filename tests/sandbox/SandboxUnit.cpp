#include <arpa/inet.h>
#include <atomic>
#include <cerrno>
#include <dirent.h>
#include <netinet/in.h>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <sched.h>
#include <string>
#include <thread>
#include <sys/ptrace.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <unistd.h>

#include "Sandbox/Sandbox.hpp"
#include "Settings/Settings.hpp"

static constexpr int EXEC_REFUSED_CODE = 42;
static constexpr int EXEC_FAILED_CODE = 43;
static constexpr long X32_SYSCALL_BIT = 0x40000000;

static int failures = 0;
static int checks = 0;

static void Expect(bool condition, const char* description)
{
    checks += 1;
    if (condition)
    {
        printf("  ok    %s\n", description);
        return;
    }

    printf("  FAIL  %s\n", description);
    failures += 1;
}

static bool CanRead(const std::string& path)
{
    int descriptor = open(path.c_str(), O_RDONLY | O_CLOEXEC);
    if (descriptor < 0) return false;

    close(descriptor);
    return true;
}

static bool ExecIsRefused()
{
    pid_t child = fork();
    if (child == 0)
    {
        char* arguments[] = {(char*)"/usr/bin/true", nullptr};
        execv("/usr/bin/true", arguments);
        _exit(errno == EPERM ? EXEC_REFUSED_CODE : EXEC_FAILED_CODE);
    }

    if (child < 0) return false;

    int state = 0;
    if (waitpid(child, &state, 0) < 0) return false;

    return WIFEXITED(state) && WEXITSTATUS(state) == EXEC_REFUSED_CODE;
}

static bool NonNativeAbiIsRefused()
{
    pid_t child = fork();
    if (child == 0)
    {
        syscall(X32_SYSCALL_BIT | __NR_unshare, CLONE_NEWUSER);
        _exit(EXEC_FAILED_CODE);
    }

    if (child < 0) return false;

    int state = 0;
    if (waitpid(child, &state, 0) < 0) return false;

    return WIFSIGNALED(state) && WTERMSIG(state) == SIGSYS;
}

static std::atomic<bool> filterInstalled{false};
static std::atomic<bool> siblingWasRefused{false};

static void ProbeFromSiblingThread()
{
    while (!filterInstalled.load(std::memory_order_acquire)) std::this_thread::yield();

    int probe = socket(AF_INET, SOCK_STREAM, 0);
    siblingWasRefused.store(probe < 0 && errno == EPERM, std::memory_order_relaxed);

    if (probe >= 0) close(probe);
}

static constexpr int ISOLATION_REFUSED = 10;
static constexpr int HOME_STILL_VISIBLE = 11;
static constexpr int UNLISTED_STILL_VISIBLE = 12;
static constexpr int LISTED_PATH_LOST = 13;
static constexpr int PROCESS_TABLE_SHARED = 14;
static constexpr int NEIGHBOURS_STILL_VISIBLE = 15;
static constexpr int NETWORK_STILL_REACHABLE = 16;

static uint64_t CountProcesses()
{
    DIR* handle = opendir("/proc");
    if (!handle) return 0;

    uint64_t seen = 0;
    for (dirent* entry = readdir(handle); entry; entry = readdir(handle))
    {
        if (entry->d_name[0] >= '1' && entry->d_name[0] <= '9') seen += 1;
    }

    closedir(handle);
    return seen;
}

static int ProbeFromInsideIsolation()
{
    Sandbox::Isolation isolation;
    isolation.readablePaths.push_back("/usr");
    isolation.allowNetwork = false;
    isolation.separateProcessTable = true;

    Sandbox::IsolationReport report;
    if (Sandbox::Isolate(isolation, report) != Sandbox::Result::Ok) return ISOLATION_REFUSED;

    struct stat details;

    const std::string home = Settings::HomeDirectory();
    if (!home.empty() && stat(home.c_str(), &details) == 0) return HOME_STILL_VISIBLE;
    if (stat("/etc", &details) == 0) return UNLISTED_STILL_VISIBLE;
    if (stat("/usr/lib", &details) != 0) return LISTED_PATH_LOST;
    if (getpid() > 2) return PROCESS_TABLE_SHARED;
    if (CountProcesses() > 2) return NEIGHBOURS_STILL_VISIBLE;

    int probe = socket(AF_INET, SOCK_DGRAM, 0);
    if (probe >= 0)
    {
        sockaddr_in elsewhere{};
        elsewhere.sin_family = AF_INET;
        elsewhere.sin_port = htons(53);
        elsewhere.sin_addr.s_addr = inet_addr("1.1.1.1");

        bool reached = connect(probe, (sockaddr*)&elsewhere, sizeof(elsewhere)) == 0;
        close(probe);

        if (reached) return NETWORK_STILL_REACHABLE;
    }

    return 0;
}

static bool CanCreate(const std::string& path)
{
    int descriptor = open(path.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (descriptor < 0) return false;

    close(descriptor);
    unlink(path.c_str());
    return true;
}

int main()
{
    if (!Sandbox::IsSupported())
    {
        printf("sandbox-unit SKIPPED: this kernel does not offer landlock\n");
        return 77;
    }

    char directoryTemplate[] = "/tmp/acl-sandbox-XXXXXX";
    const char* workDirectory = mkdtemp(directoryTemplate);
    if (!workDirectory)
    {
        printf("sandbox-unit FAILED: cannot create a work directory\n");
        return 1;
    }

    std::string allowed = std::string(workDirectory) + "/allowed.txt";
    const std::string home = Settings::HomeDirectory();
    std::string outsideHome = home.empty() ? std::string("/etc/hostname") : home + "/.bashrc";

    if (Sandbox::IsolationSupported())
    {
        pid_t isolated = fork();
        if (isolated == 0) _exit(ProbeFromInsideIsolation());

        int isolationState = 0;
        waitpid(isolated, &isolationState, 0);

        int outcome = WIFEXITED(isolationState) ? WEXITSTATUS(isolationState) : -1;

        printf("inside an isolated process:\n");
        Expect(outcome != ISOLATION_REFUSED, "the process can be isolated at all");
        Expect(outcome != HOME_STILL_VISIBLE, "$HOME is gone, not merely unreadable");
        Expect(outcome != UNLISTED_STILL_VISIBLE, "an unlisted directory does not exist");
        Expect(outcome != LISTED_PATH_LOST, "a listed directory survived the new root");
        Expect(outcome != PROCESS_TABLE_SHARED, "the process has its own process table, behind a reaper");
        Expect(outcome != NEIGHBOURS_STILL_VISIBLE, "no process outside the sandbox is visible in /proc");
        Expect(outcome != NETWORK_STILL_REACHABLE, "the network cannot be reached");
        Expect(outcome == 0, "every isolation check passed");
    }
    else
    {
        printf("isolation SKIPPED: this kernel offers no unprivileged namespaces\n");
    }

    printf("before the sandbox is applied:\n");
    Expect(CanRead("/etc/passwd"), "/etc/passwd is readable");
    Expect(CanCreate(allowed), "the work directory is writable");

    int probe = socket(AF_INET, SOCK_STREAM, 0);
    Expect(probe >= 0, "a socket can be created");
    if (probe >= 0) close(probe);

    std::thread sibling(&ProbeFromSiblingThread);

    Sandbox::Policy policy;
    policy.readablePaths.push_back("/usr");
    policy.writablePaths.push_back(workDirectory);
    policy.allowNetwork = false;

    Sandbox::Report report;
    Sandbox::Result applied = Sandbox::Apply(policy, report);

    filterInstalled.store(true, std::memory_order_release);
    sibling.join();

    if (applied != Sandbox::Result::Ok)
    {
        printf("sandbox-unit FAILED: Apply returned %s\n", Sandbox::ResultName(applied));
        return 1;
    }

    printf("applied: landlock abi %d, %llu readable, %llu writable, %llu syscalls denied\n", report.filesystemAbi,
           (unsigned long long)report.readablePathsApplied, (unsigned long long)report.writablePathsApplied,
           (unsigned long long)report.syscallsDenied);

    printf("after the sandbox is applied:\n");
    Expect(!CanRead("/etc/passwd"), "/etc/passwd is no longer readable");
    Expect(!CanRead(outsideHome), "a file outside the policy is not readable");
    Expect(!CanRead("/etc"), "an unlisted directory is not readable");
    Expect(CanCreate(allowed), "the work directory is still writable");
    Expect(CanRead("/usr/lib"), "an allowed directory is still readable");

    Expect(socket(AF_INET, SOCK_STREAM, 0) < 0 && errno == EPERM, "socket(AF_INET) is refused with EPERM");
    Expect(socket(AF_INET6, SOCK_STREAM, 0) < 0 && errno == EPERM, "socket(AF_INET6) is refused with EPERM");

    int local = socket(AF_UNIX, SOCK_STREAM, 0);
    Expect(local >= 0, "socket(AF_UNIX) is permitted, because a compositor connection needs it");
    if (local >= 0) close(local);

    Expect(ptrace(PTRACE_TRACEME, 0, nullptr, nullptr) < 0 && errno == EPERM, "ptrace is refused with EPERM");

    Expect(siblingWasRefused.load(std::memory_order_relaxed),
           "a thread that already existed when the filter was installed is refused too");

    Expect(ExecIsRefused(), "execv is refused, observed from a child so success cannot pass");
    Expect(report.syscallsDenied > 40, "the filter denies the expected number of syscalls");
    Expect(NonNativeAbiIsRefused(), "an x32 syscall is killed rather than falling through the filter");

    Expect(report.filesystemRestricted, "the report says the filesystem was restricted");
    Expect(report.syscallsRestricted, "the report says syscalls were restricted");
    Expect(report.resourcesRestricted, "the report says resources were restricted");
    Expect(report.readablePathsApplied == 1, "one readable path was applied");
    Expect(report.writablePathsApplied == 1, "one writable path was applied");
    Expect(report.syscallsDenied > 0, "the filter denies at least one syscall");

    rmdir(workDirectory);

    if (failures != 0)
    {
        printf("sandbox-unit FAILED: %d of %d checks failed\n", failures, checks);
        return 1;
    }

    printf("sandbox-unit PASSED: %d checks, the process cannot read the host, network, or exec\n", checks);
    return 0;
}

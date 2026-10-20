#include "Sandbox/Sandbox.hpp"

#include <cerrno>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <linux/landlock.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>

static constexpr uint64_t READ_ACCESS =
    LANDLOCK_ACCESS_FS_READ_FILE | LANDLOCK_ACCESS_FS_READ_DIR | LANDLOCK_ACCESS_FS_EXECUTE;

static constexpr uint64_t WRITE_ACCESS = READ_ACCESS | LANDLOCK_ACCESS_FS_WRITE_FILE | LANDLOCK_ACCESS_FS_MAKE_REG |
                                         LANDLOCK_ACCESS_FS_MAKE_DIR | LANDLOCK_ACCESS_FS_REMOVE_FILE |
                                         LANDLOCK_ACCESS_FS_REMOVE_DIR | LANDLOCK_ACCESS_FS_MAKE_SYM |
                                         LANDLOCK_ACCESS_FS_TRUNCATE | LANDLOCK_ACCESS_FS_REFER;

static constexpr uint64_t DEVICE_ACCESS =
    LANDLOCK_ACCESS_FS_READ_FILE | LANDLOCK_ACCESS_FS_WRITE_FILE | LANDLOCK_ACCESS_FS_READ_DIR;

static constexpr uint64_t FILE_ONLY_ACCESS =
    LANDLOCK_ACCESS_FS_READ_FILE | LANDLOCK_ACCESS_FS_WRITE_FILE | LANDLOCK_ACCESS_FS_EXECUTE;

static int CreateRuleset(const landlock_ruleset_attr* attribute, size_t size, uint32_t flags)
{
    return (int)syscall(__NR_landlock_create_ruleset, attribute, size, flags);
}

static int AddRule(int rulesetFileDescriptor, landlock_rule_type type, const void* attribute, uint32_t flags)
{
    return (int)syscall(__NR_landlock_add_rule, rulesetFileDescriptor, type, attribute, flags);
}

static int RestrictSelf(int rulesetFileDescriptor, uint32_t flags)
{
    return (int)syscall(__NR_landlock_restrict_self, rulesetFileDescriptor, flags);
}

static uint64_t HandledAccessFor(int abi)
{
    uint64_t handled = LANDLOCK_ACCESS_FS_EXECUTE | LANDLOCK_ACCESS_FS_WRITE_FILE | LANDLOCK_ACCESS_FS_READ_FILE |
                       LANDLOCK_ACCESS_FS_READ_DIR | LANDLOCK_ACCESS_FS_REMOVE_DIR | LANDLOCK_ACCESS_FS_REMOVE_FILE |
                       LANDLOCK_ACCESS_FS_MAKE_CHAR | LANDLOCK_ACCESS_FS_MAKE_DIR | LANDLOCK_ACCESS_FS_MAKE_REG |
                       LANDLOCK_ACCESS_FS_MAKE_SOCK | LANDLOCK_ACCESS_FS_MAKE_FIFO | LANDLOCK_ACCESS_FS_MAKE_BLOCK |
                       LANDLOCK_ACCESS_FS_MAKE_SYM;

    if (abi >= 2) handled |= LANDLOCK_ACCESS_FS_REFER;
    if (abi >= 3) handled |= LANDLOCK_ACCESS_FS_TRUNCATE;

    return handled;
}

static bool AllowPath(int rulesetFileDescriptor, const std::string& path, uint64_t access, Sandbox::Report& report, bool& applied)
{
    applied = false;

    int target = open(path.c_str(), O_PATH | O_CLOEXEC);
    if (target < 0)
    {
        report.pathsMissing += 1;
        return true;
    }

    struct stat details;
    bool directory = fstat(target, &details) == 0 && S_ISDIR(details.st_mode);

    landlock_path_beneath_attr beneath{};
    beneath.allowed_access = directory ? access : (access & FILE_ONLY_ACCESS);
    beneath.parent_fd = target;

    bool added = AddRule(rulesetFileDescriptor, LANDLOCK_RULE_PATH_BENEATH, &beneath, 0) == 0;
    close(target);

    if (!added) fprintf(stderr, "[sandbox] cannot allow %s: %s\n", path.c_str(), strerror(errno));

    applied = added;
    return added;
}

bool Sandbox::IsSupported()
{
    return FilesystemAbi() > 0;
}

int Sandbox::FilesystemAbi()
{
    int abi = CreateRuleset(nullptr, 0, LANDLOCK_CREATE_RULESET_VERSION);
    return abi < 0 ? 0 : abi;
}

Sandbox::Result Sandbox::RestrictFilesystem(const Policy& policy, Report& report)
{
    int abi = FilesystemAbi();
    if (abi <= 0) return Result::Unsupported;

    report.filesystemAbi = abi;

    if (policy.readablePaths.empty() && policy.writablePaths.empty()) return Result::NoPaths;

    landlock_ruleset_attr attribute{};
    attribute.handled_access_fs = HandledAccessFor(abi);

    int ruleset = CreateRuleset(&attribute, sizeof(attribute), 0);
    if (ruleset < 0)
    {
        fprintf(stderr, "[sandbox] cannot create a landlock ruleset: %s\n", strerror(errno));
        return Result::RulesetFailed;
    }

    uint64_t readAccess = READ_ACCESS & attribute.handled_access_fs;
    uint64_t writeAccess = WRITE_ACCESS & attribute.handled_access_fs;
    uint64_t deviceAccess = DEVICE_ACCESS & attribute.handled_access_fs;

    bool applied = false;

    for (const std::string& path : policy.readablePaths)
    {
        if (!AllowPath(ruleset, path, readAccess, report, applied))
        {
            close(ruleset);
            return Result::RuleFailed;
        }
        if (applied) report.readablePathsApplied += 1;
    }

    for (const std::string& path : policy.writablePaths)
    {
        if (!AllowPath(ruleset, path, writeAccess, report, applied))
        {
            close(ruleset);
            return Result::RuleFailed;
        }
        if (applied) report.writablePathsApplied += 1;
    }

    for (const std::string& path : policy.devicePaths)
    {
        if (!AllowPath(ruleset, path, deviceAccess, report, applied))
        {
            close(ruleset);
            return Result::RuleFailed;
        }
        if (applied) report.devicePathsApplied += 1;
    }

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0)
    {
        close(ruleset);
        return Result::NoNewPrivilegesFailed;
    }

    if (RestrictSelf(ruleset, 0) != 0)
    {
        fprintf(stderr, "[sandbox] cannot restrict this process: %s\n", strerror(errno));
        close(ruleset);
        return Result::RestrictFailed;
    }

    close(ruleset);
    report.filesystemRestricted = true;
    return Result::Ok;
}

static uint64_t CommittedAddressSpace()
{
    FILE* status = fopen("/proc/self/statm", "r");
    if (!status) return 0;

    unsigned long long pages = 0;
    int scanned = fscanf(status, "%llu", &pages);
    fclose(status);

    if (scanned != 1) return 0;

    long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) return 0;

    return (uint64_t)pages * (uint64_t)pageSize;
}

Sandbox::Result Sandbox::RestrictResources(const Policy& policy, Report& report)
{
    rlimit core{};
    core.rlim_cur = 0;
    core.rlim_max = 0;
    if (setrlimit(RLIMIT_CORE, &core) != 0) return Result::LimitsFailed;

    rlimit fileSize{};
    fileSize.rlim_cur = policy.fileSizeBytes;
    fileSize.rlim_max = policy.fileSizeBytes;
    if (setrlimit(RLIMIT_FSIZE, &fileSize) != 0) return Result::LimitsFailed;

    if (policy.threadLimit != 0)
    {
        rlimit threads{};
        threads.rlim_cur = policy.threadLimit;
        threads.rlim_max = policy.threadLimit;
        if (setrlimit(RLIMIT_NPROC, &threads) != 0) return Result::LimitsFailed;
    }

    if (policy.addressSpaceHeadroomBytes != 0)
    {
        uint64_t committed = CommittedAddressSpace();
        uint64_t ceiling = committed + policy.addressSpaceHeadroomBytes;
        if (ceiling < committed) ceiling = policy.addressSpaceHeadroomBytes;

        rlimit addressSpace{};
        addressSpace.rlim_cur = ceiling;
        addressSpace.rlim_max = ceiling;
        if (setrlimit(RLIMIT_AS, &addressSpace) != 0) return Result::LimitsFailed;

        report.addressSpaceCeilingBytes = ceiling;
        report.addressSpaceInUseBytes = committed;
    }

    report.resourcesRestricted = true;
    return Result::Ok;
}

Sandbox::Result Sandbox::Apply(const Policy& policy, Report& report)
{
    Result filesystem = RestrictFilesystem(policy, report);
    if (filesystem != Result::Ok)
    {
        fprintf(stderr, "[sandbox] the filesystem was not confined: %s\n", ResultName(filesystem));
    }

    Result resources = RestrictResources(policy, report);
    if (resources != Result::Ok)
    {
        fprintf(stderr, "[sandbox] resource limits were not applied: %s\n", ResultName(resources));
    }

    Result syscalls = RestrictSyscalls(policy, report);
    if (syscalls != Result::Ok)
    {
        fprintf(stderr, "[sandbox] the syscall filter was not installed: %s\n", ResultName(syscalls));
    }

    if (filesystem != Result::Ok) return filesystem;
    if (syscalls != Result::Ok) return syscalls;

    return resources;
}

const char* Sandbox::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::Unsupported: return "Unsupported";
        case Result::NoPaths: return "NoPaths";
        case Result::RulesetFailed: return "RulesetFailed";
        case Result::RuleFailed: return "RuleFailed";
        case Result::NoNewPrivilegesFailed: return "NoNewPrivilegesFailed";
        case Result::RestrictFailed: return "RestrictFailed";
        case Result::FilterFailed: return "FilterFailed";
        case Result::LimitsFailed: return "LimitsFailed";
        case Result::NamespaceFailed: return "NamespaceFailed";
        case Result::IdentityMapFailed: return "IdentityMapFailed";
        case Result::RootBuildFailed: return "RootBuildFailed";
        case Result::RootPivotFailed: return "RootPivotFailed";
    }

    return "Unknown";
}

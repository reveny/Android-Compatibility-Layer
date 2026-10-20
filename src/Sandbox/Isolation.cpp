#include "Sandbox/Sandbox.hpp"

#include <cerrno>
#include <climits>
#include <cstdio>
#include <dirent.h>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <sched.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/statvfs.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

#ifndef OPEN_TREE_CLONE
#define OPEN_TREE_CLONE 1
#endif

#ifndef MOVE_MOUNT_F_EMPTY_PATH
#define MOVE_MOUNT_F_EMPTY_PATH 0x00000004
#endif

static constexpr const char* MERGED_DIRECTORIES[] = {"/lib", "/lib64", "/bin", "/sbin"};
static constexpr const char* WORK_DIRECTORY = "/tmp";
static constexpr const char* REPLACEMENT_ROOT_LEAF = "root";
static constexpr const char* DISPLACED_ROOT_LEAF = ".displaced";
static constexpr const char* RESOURCE_GROUP_ROOT = "/sys/fs/cgroup";
static constexpr const char* EXTERNAL_STORAGE_ROOT = "/storage/emulated/0";

struct AttachRequest
{
    int descriptor;
    std::string path;
    bool readOnly;
    bool directory;
};

struct MergedLink
{
    std::string path;
    std::string destination;
};

static bool WriteControlFile(const std::string& path, const std::string& contents)
{
    int descriptor = open(path.c_str(), O_WRONLY | O_CLOEXEC);
    if (descriptor < 0) return false;

    bool written = write(descriptor, contents.data(), contents.size()) == (ssize_t)contents.size();

    close(descriptor);
    return written;
}

static std::string OwnResourceGroup()
{
    FILE* handle = fopen("/proc/self/cgroup", "r");
    if (!handle) return std::string();

    char line[PATH_MAX];
    if (!fgets(line, sizeof(line), handle))
    {
        fclose(handle);
        return std::string();
    }

    fclose(handle);

    std::string described = line;
    while (!described.empty() && (described.back() == '\n' || described.back() == '\r')) described.pop_back();

    size_t marker = described.find("::");
    if (marker == std::string::npos) return std::string();

    return described.substr(marker + 2);
}

static void SweepAbandonedResourceGroups(const std::string& parent)
{
    DIR* handle = opendir(parent.c_str());
    if (!handle) return;

    for (dirent* entry = readdir(handle); entry; entry = readdir(handle))
    {
        if (strncmp(entry->d_name, "acl-", 4) != 0) continue;

        rmdir((parent + "/" + entry->d_name).c_str());
    }

    closedir(handle);
}

static std::string CreateResourceGroup(const Sandbox::Isolation& isolation, Sandbox::IsolationReport& report, int& parentDescriptor,
                                       std::string& leafName)
{
    parentDescriptor = -1;
    leafName.clear();

    if (isolation.memoryBytes == 0 && isolation.processLimit == 0) return std::string();

    std::string relative = OwnResourceGroup();
    if (relative.empty()) return std::string();

    std::string leaf = "/acl-" + std::to_string((unsigned long long)getpid());

    while (!relative.empty() && relative != "/")
    {
        SweepAbandonedResourceGroups(RESOURCE_GROUP_ROOT + relative);

        std::string candidate = RESOURCE_GROUP_ROOT + relative + leaf;

        if (mkdir(candidate.c_str(), 0755) == 0 || errno == EEXIST)
        {
            bool capped = false;

            if (isolation.memoryBytes != 0 &&
                WriteControlFile(candidate + "/memory.max", std::to_string(isolation.memoryBytes)))
            {
                report.memoryCeilingBytes = isolation.memoryBytes;
                capped = true;
            }

            if (isolation.processLimit != 0 &&
                WriteControlFile(candidate + "/pids.max", std::to_string(isolation.processLimit)))
            {
                report.processCeiling = isolation.processLimit;
                capped = true;
            }

            if (capped)
            {
                parentDescriptor = open((RESOURCE_GROUP_ROOT + relative).c_str(), O_DIRECTORY | O_CLOEXEC);
                leafName = leaf.substr(1);
                return candidate;
            }

            rmdir(candidate.c_str());
        }

        size_t slash = relative.find_last_of('/');
        if (slash == std::string::npos) break;

        relative = relative.substr(0, slash);
    }

    return std::string();
}

static void EnterResourceGroup(const std::string& group, Sandbox::IsolationReport& report)
{
    if (group.empty()) return;

    if (!WriteControlFile(group + "/cgroup.procs", "0"))
    {
        fprintf(stderr, "[sandbox] cannot join the resource group: %s\n", strerror(errno));
        report.memoryCeilingBytes = 0;
        report.processCeiling = 0;
        return;
    }

    report.resourcesGrouped = true;
}

static bool WriteIdentityFile(const char* path, const char* contents)
{
    int descriptor = open(path, O_WRONLY | O_CLOEXEC);
    if (descriptor < 0) return false;

    size_t length = strlen(contents);
    bool written = write(descriptor, contents, length) == (ssize_t)length;

    close(descriptor);
    return written;
}

static bool MakeDirectories(const std::string& path)
{
    if (path.empty() || path == "/") return true;

    std::string built;
    size_t index = 0;

    while (index < path.size())
    {
        size_t slash = path.find('/', index + 1);
        if (slash == std::string::npos) slash = path.size();

        built = path.substr(0, slash);
        if (!built.empty() && mkdir(built.c_str(), 0755) != 0 && errno != EEXIST) return false;

        index = slash;
    }

    return true;
}

static bool MakeParentDirectories(const std::string& path)
{
    size_t slash = path.find_last_of('/');
    if (slash == std::string::npos || slash == 0) return true;

    return MakeDirectories(path.substr(0, slash));
}

static void RememberSource(std::vector<AttachRequest>& requests, const std::string& path, bool readOnly,
                           Sandbox::IsolationReport& report)
{
    struct stat details;
    if (stat(path.c_str(), &details) != 0)
    {
        report.pathsMissing += 1;
        return;
    }

    requests.push_back({-1, path, readOnly, S_ISDIR(details.st_mode) != 0});
}

static void CaptureSources(std::vector<AttachRequest>& requests)
{
    for (AttachRequest& request : requests)
    {
        request.descriptor =
            (int)syscall(SYS_open_tree, AT_FDCWD, request.path.c_str(), OPEN_TREE_CLONE | AT_RECURSIVE | O_CLOEXEC);
    }
}

static void RestrictToReadOnly(const std::string& target, const std::string& path)
{
    unsigned long carried = MS_BIND | MS_REMOUNT | MS_RDONLY;

    struct statvfs attached;
    if (statvfs(target.c_str(), &attached) == 0)
    {
        if (attached.f_flag & ST_NOSUID) carried |= MS_NOSUID;
        if (attached.f_flag & ST_NODEV) carried |= MS_NODEV;
        if (attached.f_flag & ST_NOEXEC) carried |= MS_NOEXEC;
        if (attached.f_flag & ST_NOATIME) carried |= MS_NOATIME;
        if (attached.f_flag & ST_NODIRATIME) carried |= MS_NODIRATIME;
        if (attached.f_flag & ST_RELATIME) carried |= MS_RELATIME;
    }

    if (mount(nullptr, target.c_str(), nullptr, carried, nullptr) != 0)
        fprintf(stderr, "[sandbox] %s stays writable: %s\n", path.c_str(), strerror(errno));
}

static bool AttachRemembered(const std::string& newRoot, const AttachRequest& request, Sandbox::IsolationReport& report)
{
    std::string target = newRoot + request.path;

    if (request.directory)
    {
        if (!MakeDirectories(target)) return false;
    }
    else
    {
        if (!MakeParentDirectories(target)) return false;

        int placeholder = open(target.c_str(), O_WRONLY | O_CREAT | O_CLOEXEC, 0644);
        if (placeholder < 0 && errno != EEXIST) return false;
        if (placeholder >= 0) close(placeholder);
    }

    bool attached = false;

    if (request.descriptor >= 0)
    {
        attached = syscall(SYS_move_mount, request.descriptor, "", AT_FDCWD, target.c_str(), MOVE_MOUNT_F_EMPTY_PATH) == 0;
        if (!attached)
            fprintf(stderr, "[sandbox] cannot place the captured %s: %s\n", request.path.c_str(), strerror(errno));
    }

    if (!attached && mount(request.path.c_str(), target.c_str(), nullptr, MS_BIND | MS_REC, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot attach %s: %s\n", request.path.c_str(), strerror(errno));
        return false;
    }

    if (request.readOnly) RestrictToReadOnly(target, request.path);

    report.pathsAttached += 1;
    return true;
}

static bool rootWasReplaced = false;

static bool AttachOver(const std::string& source, const std::string& target)
{
    if (!MakeDirectories(target)) return false;

    if (mount(source.c_str(), target.c_str(), nullptr, MS_BIND | MS_REC, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot place %s at %s: %s\n", source.c_str(), target.c_str(), strerror(errno));
        return false;
    }

    return true;
}

bool Sandbox::RootWasReplaced()
{
    return rootWasReplaced;
}

static std::string ScrambledName(const std::string& seed, uint64_t salt)
{
    static const char ALPHABET[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    uint64_t state = 1469598103934665603ull ^ salt;
    for (char letter : seed)
    {
        state ^= (uint64_t)(unsigned char)letter;
        state *= 1099511628211ull;
    }

    std::string scrambled;
    for (int index = 0; index < 22; ++index)
    {
        scrambled += ALPHABET[state & 63];
        state = state * 6364136223846793005ull + 1442695040888963407ull;
    }

    return scrambled + "==";
}

static bool AttachFileOver(const std::string& source, const std::string& target)
{
    if (!MakeParentDirectories(target)) return false;

    int placeholder = open(target.c_str(), O_WRONLY | O_CREAT | O_CLOEXEC, 0644);
    if (placeholder < 0 && errno != EEXIST) return false;
    if (placeholder >= 0) close(placeholder);

    if (mount(source.c_str(), target.c_str(), nullptr, MS_BIND, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot place %s at %s: %s\n", source.c_str(), target.c_str(), strerror(errno));
        return false;
    }

    return true;
}

Sandbox::Result Sandbox::AttachSystemFile(const std::string& path, const std::string& contents)
{
    if (!rootWasReplaced) return Result::Unsupported;

    std::string backing = std::string(WORK_DIRECTORY) + "/described";
    if (!MakeDirectories(backing)) return Result::RootBuildFailed;

    size_t slash = path.find_last_of('/');
    backing += slash == std::string::npos ? path : path.substr(slash);

    int descriptor = open(backing.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0444);
    if (descriptor < 0) return Result::RootBuildFailed;

    bool written = write(descriptor, contents.data(), contents.size()) == (ssize_t)contents.size();
    close(descriptor);

    if (!written) return Result::RootBuildFailed;

    if (mount(backing.c_str(), path.c_str(), nullptr, MS_BIND, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot describe %s to the guest: %s\n", path.c_str(), strerror(errno));
        return Result::RootBuildFailed;
    }

    return Result::Ok;
}

Sandbox::Result Sandbox::AttachAndroidTree(const AndroidTree& tree, AndroidPaths& paths)
{
    if (!rootWasReplaced) return Result::Unsupported;
    if (tree.packageName.empty() || tree.dataDirectory.empty()) return Result::NoPaths;

    std::string installed = "/data/app/~~" + ScrambledName(tree.packageName, 1) + "/" + tree.packageName + "-" +
                            ScrambledName(tree.packageName, 2);

    if (!tree.archivePath.empty() && AttachFileOver(tree.archivePath, installed + "/base.apk"))
    {
        paths.archivePath = installed + "/base.apk";
    }

    if (!tree.libraryDirectory.empty() && AttachOver(tree.libraryDirectory, installed + "/lib/arm64"))
    {
        paths.libraryDirectory = installed + "/lib/arm64";
    }

    std::string packageData = "/data/data/" + tree.packageName;

    if (!AttachOver(tree.dataDirectory, packageData)) return Result::RootBuildFailed;
    if (!AttachOver(tree.dataDirectory, "/data/user/0/" + tree.packageName)) return Result::RootBuildFailed;

    if (!tree.externalDirectory.empty())
    {
        if (!AttachOver(tree.externalDirectory, EXTERNAL_STORAGE_ROOT)) return Result::RootBuildFailed;

        MakeDirectories("/storage/self");
        MakeDirectories("/mnt");

        if (symlink(EXTERNAL_STORAGE_ROOT, "/storage/self/primary") != 0 && errno != EEXIST)
            fprintf(stderr, "[sandbox] the primary storage link is missing: %s\n", strerror(errno));

        if (symlink("/storage/self/primary", "/sdcard") != 0 && errno != EEXIST)
            fprintf(stderr, "[sandbox] the sdcard link is missing: %s\n", strerror(errno));

        if (symlink(EXTERNAL_STORAGE_ROOT, "/mnt/sdcard") != 0 && errno != EEXIST)
            fprintf(stderr, "[sandbox] the mounted sdcard link is missing: %s\n", strerror(errno));
    }

    return Result::Ok;
}

bool Sandbox::IsolationSupported()
{
    int probe = open("/proc/self/uid_map", O_WRONLY | O_CLOEXEC);
    if (probe < 0) return false;

    close(probe);
    return true;
}

Sandbox::Result Sandbox::Isolate(const Isolation& isolation, IsolationReport& report)
{
    uid_t user = getuid();
    gid_t group = getgid();

    std::vector<AttachRequest> requests;
    std::vector<MergedLink> links;

    for (const std::string& path : isolation.readablePaths) RememberSource(requests, path, true, report);
    for (const std::string& path : isolation.writablePaths) RememberSource(requests, path, false, report);

    for (const char* path : MERGED_DIRECTORIES)
    {
        char destination[PATH_MAX];
        ssize_t length = readlink(path, destination, sizeof(destination) - 1);

        if (length > 0)
        {
            destination[length] = '\0';
            links.push_back({path, destination});
            continue;
        }

        RememberSource(requests, path, true, report);
    }

    size_t deviceFirst = requests.size();
    for (const std::string& path : isolation.devicePaths) RememberSource(requests, path, false, report);

    int resourceGroupParent = -1;
    std::string resourceGroupLeaf;
    std::string resourceGroup = CreateResourceGroup(isolation, report, resourceGroupParent, resourceGroupLeaf);

    int wanted = CLONE_NEWUSER | CLONE_NEWNS | CLONE_NEWIPC | CLONE_NEWUTS;
    if (!isolation.allowNetwork) wanted |= CLONE_NEWNET;
    if (isolation.separateProcessTable) wanted |= CLONE_NEWPID;

    if (unshare(wanted) != 0)
    {
        fprintf(stderr, "[sandbox] cannot create namespaces: %s\n", strerror(errno));
        return Result::NamespaceFailed;
    }

    report.userNamespace = true;
    report.mountNamespace = true;
    report.networkNamespace = !isolation.allowNetwork;
    report.processNamespace = isolation.separateProcessTable;

    char identity[64];

    unsigned insideUser = isolation.applicationUserId != 0 ? isolation.applicationUserId : (unsigned)user;
    unsigned insideGroup = isolation.applicationUserId != 0 ? isolation.applicationUserId : (unsigned)group;

    WriteIdentityFile("/proc/self/setgroups", "deny");

    snprintf(identity, sizeof(identity), "%u %u 1", insideUser, (unsigned)user);
    if (!WriteIdentityFile("/proc/self/uid_map", identity)) return Result::IdentityMapFailed;

    snprintf(identity, sizeof(identity), "%u %u 1", insideGroup, (unsigned)group);
    if (!WriteIdentityFile("/proc/self/gid_map", identity)) return Result::IdentityMapFailed;

    if (isolation.separateProcessTable)
    {
        pid_t player = fork();
        if (player < 0)
        {
            fprintf(stderr, "[sandbox] cannot enter the process table: %s\n", strerror(errno));
            return Result::NamespaceFailed;
        }

        if (player != 0)
        {
            int state = 0;
            while (waitpid(player, &state, 0) < 0 && errno == EINTR) continue;

            if (resourceGroupParent >= 0 && unlinkat(resourceGroupParent, resourceGroupLeaf.c_str(), AT_REMOVEDIR) != 0)
                fprintf(stderr, "[sandbox] the resource group %s outlived the run: %s\n", resourceGroupLeaf.c_str(), strerror(errno));

            if (WIFSIGNALED(state)) _exit(128 + WTERMSIG(state));
            _exit(WIFEXITED(state) ? WEXITSTATUS(state) : 1);
        }
    }

    EnterResourceGroup(resourceGroup, report);
    unshare(CLONE_NEWCGROUP);

    if (mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot detach the mount tree: %s\n", strerror(errno));
        return Result::RootBuildFailed;
    }

    CaptureSources(requests);

    if (mount("acl",WORK_DIRECTORY, "tmpfs", MS_NOSUID | MS_NODEV, "mode=755") != 0)
    {
        fprintf(stderr, "[sandbox] cannot create a private work directory: %s\n", strerror(errno));
        return Result::RootBuildFailed;
    }

    std::string root = std::string(WORK_DIRECTORY) + "/" + REPLACEMENT_ROOT_LEAF;

    if (mkdir(root.c_str(), 0755) != 0 || mount("acl",root.c_str(), "tmpfs", MS_NOSUID | MS_NODEV, "mode=755") != 0)
    {
        fprintf(stderr, "[sandbox] cannot create the replacement root: %s\n", strerror(errno));
        return Result::RootBuildFailed;
    }

    for (size_t index = 0; index < deviceFirst; ++index)
    {
        if (!AttachRemembered(root, requests[index], report)) return Result::RootBuildFailed;
    }

    for (const MergedLink& link : links)
    {
        if (symlink(link.destination.c_str(), (root + link.path).c_str()) != 0 && errno != EEXIST)
        {
            fprintf(stderr, "[sandbox] cannot recreate the link %s: %s\n", link.path.c_str(), strerror(errno));
            return Result::RootBuildFailed;
        }
    }

    if (!MakeDirectories(root + "/dev") || mount("acl",(root + "/dev").c_str(), "tmpfs", MS_NOSUID, "mode=755") != 0)
        return Result::RootBuildFailed;

    for (size_t index = deviceFirst; index < requests.size(); ++index)
    {
        if (!AttachRemembered(root, requests[index], report)) return Result::RootBuildFailed;
    }

    if (MakeDirectories(root + "/dev/shm")) mount("acl",(root + "/dev/shm").c_str(), "tmpfs", MS_NOSUID | MS_NODEV, nullptr);
    if (!MakeDirectories(root + "/tmp")) return Result::RootBuildFailed;

    if (!MakeDirectories(root + "/proc")) return Result::RootBuildFailed;

    if (mount("proc", (root + "/proc").c_str(), "proc", MS_NOSUID | MS_NODEV | MS_NOEXEC, nullptr) != 0)
    {
        fprintf(stderr, "[sandbox] cannot give the process its own view of /proc: %s\n", strerror(errno));
        return Result::RootBuildFailed;
    }

    std::string displaced = root + "/" + DISPLACED_ROOT_LEAF;
    if (!MakeDirectories(displaced)) return Result::RootBuildFailed;

    if (syscall(SYS_pivot_root, root.c_str(), displaced.c_str()) != 0)
    {
        fprintf(stderr, "[sandbox] cannot replace the root: %s\n", strerror(errno));
        return Result::RootPivotFailed;
    }

    if (chdir("/") != 0) return Result::RootPivotFailed;

    std::string oldRoot = std::string("/") + DISPLACED_ROOT_LEAF;
    umount2(oldRoot.c_str(), MNT_DETACH);
    rmdir(oldRoot.c_str());

    for (const AttachRequest& request : requests)
    {
        if (request.descriptor >= 0) close(request.descriptor);
    }

    report.rootReplaced = true;
    rootWasReplaced = true;

    if (!isolation.separateProcessTable) return Result::Ok;

    pid_t worker = fork();
    if (worker < 0)
    {
        fprintf(stderr, "[sandbox] cannot step out of the first process slot: %s\n", strerror(errno));
        return Result::NamespaceFailed;
    }

    if (worker == 0) return Result::Ok;

    for (;;)
    {
        int state = 0;
        pid_t finished = waitpid(-1, &state, 0);

        if (finished < 0)
        {
            if (errno == EINTR) continue;
            _exit(1);
        }

        if (finished != worker) continue;

        if (WIFSIGNALED(state)) _exit(128 + WTERMSIG(state));
        _exit(WIFEXITED(state) ? WEXITSTATUS(state) : 1);
    }
}

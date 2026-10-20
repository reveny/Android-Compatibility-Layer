#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace Sandbox
{
    inline constexpr uint64_t DEFAULT_ADDRESS_SPACE_HEADROOM_BYTES = 32ull * 1024ull * 1024ull * 1024ull;
    inline constexpr uint64_t DEFAULT_FILE_SIZE_BYTES = 4ull * 1024ull * 1024ull * 1024ull;
    inline constexpr uint64_t DEFAULT_THREAD_LIMIT = 0;

    enum class Result : uint8_t
    {
        Ok,
        Unsupported,
        NoPaths,
        RulesetFailed,
        RuleFailed,
        NoNewPrivilegesFailed,
        RestrictFailed,
        FilterFailed,
        LimitsFailed,
        NamespaceFailed,
        IdentityMapFailed,
        RootBuildFailed,
        RootPivotFailed
    };

    struct Policy
    {
        std::vector<std::string> readablePaths;
        std::vector<std::string> writablePaths;
        std::vector<std::string> devicePaths;
        bool allowNetwork = false;
        bool allowProcessExecution = false;
        bool allowMountChanges = false;
        uint64_t addressSpaceHeadroomBytes = DEFAULT_ADDRESS_SPACE_HEADROOM_BYTES;
        uint64_t fileSizeBytes = DEFAULT_FILE_SIZE_BYTES;
        uint64_t threadLimit = DEFAULT_THREAD_LIMIT;
    };

    struct Report
    {
        int filesystemAbi = 0;
        uint64_t readablePathsApplied = 0;
        uint64_t writablePathsApplied = 0;
        uint64_t devicePathsApplied = 0;
        uint64_t pathsMissing = 0;
        uint64_t syscallsDenied = 0;
        uint64_t addressSpaceInUseBytes = 0;
        uint64_t addressSpaceCeilingBytes = 0;
        bool filesystemRestricted = false;
        bool syscallsRestricted = false;
        bool resourcesRestricted = false;
    };

    struct Isolation
    {
        std::vector<std::string> readablePaths;
        std::vector<std::string> writablePaths;
        std::vector<std::string> devicePaths;
        bool allowNetwork = false;
        bool separateProcessTable = true;
        uint64_t memoryBytes = 0;
        uint64_t processLimit = 0;
        uint32_t applicationUserId = 0;
    };

    struct IsolationReport
    {
        uint64_t pathsAttached = 0;
        uint64_t pathsMissing = 0;
        uint64_t memoryCeilingBytes = 0;
        uint64_t processCeiling = 0;
        bool userNamespace = false;
        bool mountNamespace = false;
        bool networkNamespace = false;
        bool processNamespace = false;
        bool rootReplaced = false;
        bool resourcesGrouped = false;
    };

    struct AndroidTree
    {
        std::string dataDirectory;
        std::string externalDirectory;
        std::string packageName;
        std::string archivePath;
        std::string libraryDirectory;
    };

    struct AndroidPaths
    {
        std::string archivePath;
        std::string libraryDirectory;
    };

    bool IsolationSupported();
    Result Isolate(const Isolation& isolation, IsolationReport& report);
    Result AttachAndroidTree(const AndroidTree& tree, AndroidPaths& paths);
    Result AttachSystemFile(const std::string& path, const std::string& contents);
    bool RootWasReplaced();

    bool IsSupported();
    int FilesystemAbi();

    Result RestrictFilesystem(const Policy& policy, Report& report);
    Result RestrictSyscalls(const Policy& policy, Report& report);
    Result RestrictResources(const Policy& policy, Report& report);
    Result Apply(const Policy& policy, Report& report);

    const char* ResultName(Result result);
}

#include "Sandbox/Sandbox.hpp"

#include <cerrno>
#include <cstdio>
#include <cstring>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <stddef.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <sys/syscall.h>
#include <unistd.h>

#ifndef SECCOMP_SET_MODE_FILTER
#define SECCOMP_SET_MODE_FILTER 1
#endif

#ifndef SECCOMP_FILTER_FLAG_TSYNC
#define SECCOMP_FILTER_FLAG_TSYNC 1
#endif

#ifndef __X32_SYSCALL_BIT
#define __X32_SYSCALL_BIT 0x40000000
#endif

struct DeniedSyscall
{
    int number;
    const char* name;
};

static const DeniedSyscall LOCAL_ONLY_SYSCALLS[] = {
    {__NR_socket, "socket"},
    {__NR_socketpair, "socketpair"},
};

static const DeniedSyscall EXECUTION_SYSCALLS[] = {
    {__NR_execve, "execve"},
    {__NR_execveat, "execveat"},
};

static const DeniedSyscall MOUNT_SYSCALLS[] = {
    {__NR_mount, "mount"},
    {__NR_umount2, "umount2"},
};

static const DeniedSyscall ALWAYS_DENIED[] = {
    {__NR_ptrace, "ptrace"},
    {__NR_process_vm_readv, "process_vm_readv"},
    {__NR_process_vm_writev, "process_vm_writev"},
    {__NR_kcmp, "kcmp"},
    {__NR_unshare, "unshare"},
    {__NR_setns, "setns"},
    {__NR_pivot_root, "pivot_root"},
    {__NR_chroot, "chroot"},
    {__NR_open_by_handle_at, "open_by_handle_at"},
    {__NR_name_to_handle_at, "name_to_handle_at"},
    {__NR_fsopen, "fsopen"},
    {__NR_fsconfig, "fsconfig"},
    {__NR_fsmount, "fsmount"},
    {__NR_move_mount, "move_mount"},
    {__NR_open_tree, "open_tree"},
    {__NR_io_uring_setup, "io_uring_setup"},
    {__NR_io_uring_enter, "io_uring_enter"},
    {__NR_io_uring_register, "io_uring_register"},
    {__NR_bpf, "bpf"},
    {__NR_perf_event_open, "perf_event_open"},
    {__NR_userfaultfd, "userfaultfd"},
    {__NR_keyctl, "keyctl"},
    {__NR_add_key, "add_key"},
    {__NR_request_key, "request_key"},
    {__NR_kexec_load, "kexec_load"},
    {__NR_kexec_file_load, "kexec_file_load"},
    {__NR_init_module, "init_module"},
    {__NR_finit_module, "finit_module"},
    {__NR_delete_module, "delete_module"},
    {__NR_modify_ldt, "modify_ldt"},
    {__NR_swapon, "swapon"},
    {__NR_swapoff, "swapoff"},
    {__NR_reboot, "reboot"},
    {__NR_settimeofday, "settimeofday"},
    {__NR_clock_settime, "clock_settime"},
    {__NR_quotactl, "quotactl"},
    {__NR_acct, "acct"},
    {__NR_setuid, "setuid"},
    {__NR_setgid, "setgid"},
    {__NR_setreuid, "setreuid"},
    {__NR_setregid, "setregid"},
    {__NR_setresuid, "setresuid"},
    {__NR_setresgid, "setresgid"},
    {__NR_capset, "capset"},
};

static constexpr size_t ALWAYS_DENIED_COUNT = sizeof(ALWAYS_DENIED) / sizeof(ALWAYS_DENIED[0]);
static constexpr size_t EXECUTION_COUNT = sizeof(EXECUTION_SYSCALLS) / sizeof(EXECUTION_SYSCALLS[0]);
static constexpr size_t LOCAL_ONLY_COUNT = sizeof(LOCAL_ONLY_SYSCALLS) / sizeof(LOCAL_ONLY_SYSCALLS[0]);

Sandbox::Result Sandbox::RestrictSyscalls(const Policy& policy, Report& report)
{
    std::vector<sock_filter> program;

    program.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (uint32_t)offsetof(seccomp_data, arch)));
    program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AUDIT_ARCH_X86_64, 1, 0));
    program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_KILL_PROCESS));

    program.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (uint32_t)offsetof(seccomp_data, nr)));

    program.push_back(BPF_JUMP(BPF_JMP | BPF_JGE | BPF_K, __X32_SYSCALL_BIT, 0, 1));
    program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_KILL_PROCESS));

    uint64_t denied = 0;

    for (size_t index = 0; index < ALWAYS_DENIED_COUNT; ++index)
    {
        program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, (uint32_t)ALWAYS_DENIED[index].number, 0, 1));
        program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA)));
        denied += 1;
    }

    if (!policy.allowProcessExecution)
    {
        for (size_t index = 0; index < EXECUTION_COUNT; ++index)
        {
            program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, (uint32_t)EXECUTION_SYSCALLS[index].number, 0, 1));
            program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA)));
            denied += 1;
        }
    }

    if (!policy.allowMountChanges)
    {
        for (const DeniedSyscall& entry : MOUNT_SYSCALLS)
        {
            program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, (uint32_t)entry.number, 0, 1));
            program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA)));
            denied += 1;
        }
    }

    if (!policy.allowNetwork)
    {
        for (size_t index = 0; index < LOCAL_ONLY_COUNT; ++index)
        {
            program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, (uint32_t)LOCAL_ONLY_SYSCALLS[index].number, 0, 4));
            program.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (uint32_t)offsetof(seccomp_data, args[0])));
            program.push_back(BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, AF_UNIX, 1, 0));
            program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA)));
            program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));
            program.push_back(BPF_STMT(BPF_LD | BPF_W | BPF_ABS, (uint32_t)offsetof(seccomp_data, nr)));
            denied += 1;
        }
    }

    program.push_back(BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW));

    sock_fprog filter{};
    filter.len = (unsigned short)program.size();
    filter.filter = program.data();

    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) return Result::NoNewPrivilegesFailed;

    long installed = syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, SECCOMP_FILTER_FLAG_TSYNC, &filter);

    if (installed > 0)
    {
        fprintf(stderr, "[sandbox] thread %ld would not take the syscall filter, so no thread has it\n", installed);
        return Result::FilterFailed;
    }

    if (installed != 0)
    {
        fprintf(stderr, "[sandbox] cannot install the syscall filter: %s\n", strerror(errno));
        return Result::FilterFailed;
    }

    report.syscallsDenied = denied;
    report.syscallsRestricted = true;
    return Result::Ok;
}

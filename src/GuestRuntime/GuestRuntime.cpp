#include "GuestRuntime/GuestRuntime.hpp"

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "Diagnostics/Diagnostics.hpp"
#include "GuestSync/GuestSync.hpp"
#include "GuestSyscall/GuestSyscall.hpp"
#include "Interpreter/Interpreter.hpp"
#include "Ir/Ir.hpp"
#include "Platform/Platform.hpp"

#include <atomic>
#include <condition_variable>
#include <csignal>
#include <dlfcn.h>
#include <sys/syscall.h>
#include <ucontext.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <deque>
#include <mutex>
#include <sys/mman.h>
#include <thread>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "Settings/Settings.hpp"

static constexpr uint32_t BRK_ENCODING = 0xD4200000u;
static constexpr uint64_t HOST_ADDRESS_FLOOR = 0x10000;
static constexpr int GUEST_BACKTRACE_FRAMES = 14;
static constexpr uint64_t GUEST_FAULT_SCAN_BYTES = 4096;
static constexpr int GUEST_FAULT_SCAN_FRAMES = 40;
static constexpr uint64_t SIGNAL_FRAME_GAP_BYTES = 512;
static constexpr uint64_t RECENT_ENTRY_COUNT = 32;
static constexpr uint64_t FAULT_STACK_BYTES = 256 * 1024;
static constexpr uint64_t BLOCK_TRACE_REPORTED = 64;
static constexpr uint64_t SHUTDOWN_GRACE_MILLISECONDS = 2000;
static constexpr uint64_t SHUTDOWN_POLL_MILLISECONDS = 20;
static constexpr uint64_t UNRESOLVED_DATA_OBJECT_BYTES = 4096;
static constexpr int FAULT_MESSAGE_BYTES = 256;

static std::atomic<uint64_t> faultReportArenaBase{0};
static std::atomic<uint64_t> faultReportArenaLength{0};

static char* AppendText(char* cursor, char* end, const char* text)
{
    while (*text && cursor < end) *cursor++ = *text++;
    return cursor;
}

static char* AppendHex(char* cursor, char* end, uint64_t value)
{
    static const char DIGITS[] = "0123456789abcdef";

    for (int shift = 60; shift >= 0 && cursor < end; shift -= 4) *cursor++ = DIGITS[(value >> shift) & 0xf];
    return cursor;
}

static struct sigaction previousSegmentationFault;
static struct sigaction previousBusError;

static void ChainToPrevious(int signalNumber, siginfo_t* information, void* context)
{
    const struct sigaction& previous = signalNumber == SIGBUS ? previousBusError : previousSegmentationFault;

    if ((previous.sa_flags & SA_SIGINFO) != 0 && previous.sa_sigaction)
    {
        previous.sa_sigaction(signalNumber, information, context);
        return;
    }

    if (previous.sa_handler && previous.sa_handler != SIG_DFL && previous.sa_handler != SIG_IGN)
    {
        previous.sa_handler(signalNumber);
        return;
    }

    struct sigaction restored;
    memset(&restored, 0, sizeof(restored));
    restored.sa_handler = SIG_DFL;
    sigaction(signalNumber, &restored, nullptr);
}

static void ReportGuestRegisters(uint64_t faultAddress);
static void ReportLoadedModules();
static bool DescribeTranslatedEntry(uint64_t entryAddress, char* out, size_t capacity);

static thread_local uint64_t gRecompiledEntry = 0;

static std::atomic<GuestRuntime::FaultHandler> gFaultHandler{nullptr};
static std::atomic<void*> gFaultHandlerContext{nullptr};

void GuestRuntime::SetFaultHandler(FaultHandler handler, void* context)
{
    gFaultHandlerContext.store(context, std::memory_order_relaxed);
    gFaultHandler.store(handler, std::memory_order_release);
}

static void ReportHostRegisters(void* context)
{
    ucontext_t* machine = (ucontext_t*)context;
    if (!machine) return;

    static const struct
    {
        const char* name;
        int slot;
    } HOST_REGISTERS[] = {{"rax", REG_RAX}, {"rbx", REG_RBX}, {"rcx", REG_RCX}, {"rdx", REG_RDX}, {"rsi", REG_RSI}, {"rdi", REG_RDI},
                          {"rbp", REG_RBP}, {"rsp", REG_RSP}, {"r8 ", REG_R8},  {"r9 ", REG_R9},  {"r10", REG_R10}, {"r11", REG_R11},
                          {"r12", REG_R12}, {"r13", REG_R13}, {"r14", REG_R14}, {"r15", REG_R15}};

    static constexpr int HOST_REGISTER_COUNT = (int)(sizeof(HOST_REGISTERS) / sizeof(HOST_REGISTERS[0]));

    fprintf(stderr, "[guestruntime]   the host registers at the fault, which is where translated code keeps the guest ones:\n");

    for (int row = 0; row < HOST_REGISTER_COUNT; row += 4)
    {
        fprintf(stderr, "[guestruntime]  ");

        for (int column = 0; column < 4 && row + column < HOST_REGISTER_COUNT; ++column)
        {
            fprintf(stderr, " %s=%016llx", HOST_REGISTERS[row + column].name,
                    (unsigned long long)machine->uc_mcontext.gregs[HOST_REGISTERS[row + column].slot]);
        }

        fprintf(stderr, "\n");
    }
}

static bool TranslatedSymbolEntry(const char* symbolName, uint64_t& entryAddress)
{
    static const char PREFIX[] = "acl_fn_";
    static const size_t PREFIX_LENGTH = sizeof(PREFIX) - 1;

    if (!symbolName || strncmp(symbolName, PREFIX, PREFIX_LENGTH) != 0) return false;

    char* end = nullptr;
    const unsigned long long parsed = strtoull(symbolName + PREFIX_LENGTH, &end, 16);

    if (!end || *end != '\0' || end == symbolName + PREFIX_LENGTH) return false;

    entryAddress = (uint64_t)parsed;
    return true;
}

static void ReportFaultingInstruction(void* context)
{
    ucontext_t* machine = (ucontext_t*)context;
    if (!machine) return;

    uint64_t instruction = (uint64_t)machine->uc_mcontext.gregs[REG_RIP];

    Dl_info described{};
    if (dladdr((void*)instruction, &described) != 0 && described.dli_sname)
    {
        uint64_t guestEntry = 0;

        if (TranslatedSymbolEntry(described.dli_sname, guestEntry))
        {
            char where[256];
            const bool placed = DescribeTranslatedEntry(guestEntry, where, sizeof(where));

            fprintf(stderr, "[guestruntime]   the faulting instruction is %llu bytes into translated guest code for %016llx%s%s\n",
                    (unsigned long long)(instruction - (uint64_t)described.dli_saddr), (unsigned long long)guestEntry,
                    placed ? ", which is " : "", placed ? where : "");
            return;
        }

        fprintf(stderr, "[guestruntime]   the faulting instruction is at %016llx, %llu bytes into %s\n",
                (unsigned long long)instruction, (unsigned long long)(instruction - (uint64_t)described.dli_saddr),
                described.dli_sname);
        return;
    }

    if (described.dli_fname)
    {
        fprintf(stderr, "[guestruntime]   the faulting instruction is at %016llx, %llu bytes into %s\n",
                (unsigned long long)instruction, (unsigned long long)(instruction - (uint64_t)described.dli_fbase),
                described.dli_fname);
        return;
    }

    if (gRecompiledEntry != 0)
    {
        char where[256];
        const bool placed = DescribeTranslatedEntry(gRecompiledEntry, where, sizeof(where));

        fprintf(stderr,
                "[guestruntime]   the faulting instruction is at %016llx, in code the recompiler built at run time for the guest region "
                "entered at %016llx%s%s\n",
                (unsigned long long)instruction, (unsigned long long)gRecompiledEntry, placed ? ", which is " : "", placed ? where : "");
        return;
    }

    fprintf(stderr, "[guestruntime]   the faulting instruction is at %016llx, which belongs to no known image\n",
            (unsigned long long)instruction);
}

static void ReportGuestFault(int signalNumber, siginfo_t* information, void* context)
{
    uint64_t address = (uint64_t)information->si_addr;
    uint64_t base = faultReportArenaBase.load(std::memory_order_relaxed);
    uint64_t length = faultReportArenaLength.load(std::memory_order_relaxed);

    bool insideArena = length != 0 && address >= base && address < base + length;

    if (insideArena)
    {
        GuestRuntime::FaultHandler handler = gFaultHandler.load(std::memory_order_acquire);
        if (handler && handler(gFaultHandlerContext.load(std::memory_order_relaxed), signalNumber, information)) return;
    }

    if (insideArena)
    {
        char message[FAULT_MESSAGE_BYTES];
        char* cursor = message;
        char* end = message + sizeof(message);

        cursor = AppendText(cursor, end, "[guestruntime] fault at ");
        cursor = AppendHex(cursor, end, address);
        cursor = AppendText(cursor, end, ", inside the guest arena: translated code dereferenced a guest pointer it may not touch\n");

        ssize_t ignored = write(2, message, (size_t)(cursor - message));
        (void)ignored;
    }

    const uint64_t faultSequence = Diagnostics::NextSequence();

    ReportGuestRegisters(address);
    ReportFaultingInstruction(context);
    ReportHostRegisters(context);
    ReportLoadedModules();

    fflush(stderr);

    const char* tombstone = Settings::Lookup(Diagnostics::TOMBSTONE_SETTING);

    if (tombstone && tombstone[0] != '\0')
    {
        const bool wrote = Diagnostics::WriteTombstone(tombstone, faultSequence, Diagnostics::TOMBSTONE_CONTEXT_LINES);
        fprintf(stderr, "[guestruntime] %s the tombstone at %s\n", wrote ? "wrote" : "could not write", tombstone);
        fflush(stderr);
    }

    ChainToPrevious(signalNumber, information, context);
}

static void InstallFaultReporter(uint64_t arenaBase, uint64_t arenaLength)
{
    faultReportArenaBase.store(arenaBase, std::memory_order_relaxed);
    faultReportArenaLength.store(arenaLength, std::memory_order_relaxed);

    static std::once_flag installed;
    std::call_once(installed, []() {
        struct sigaction action;
        memset(&action, 0, sizeof(action));

        action.sa_sigaction = &ReportGuestFault;
        action.sa_flags = SA_SIGINFO | SA_ONSTACK;
        sigemptyset(&action.sa_mask);

        sigaction(SIGSEGV, &action, &previousSegmentationFault);
        sigaction(SIGBUS, &action, &previousBusError);
    });
}
static constexpr uint8_t PAGE_COMMITTED = 1u << 0;
static constexpr uint8_t PAGE_READABLE = 1u << 1;
static constexpr uint8_t PAGE_WRITABLE = 1u << 2;
static constexpr uint8_t PAGE_EXECUTABLE = 1u << 3;

struct GuestThread
{
    std::thread worker;
    uint64_t returnValue = 0;
    std::atomic<bool> finished{false};
    bool joined = false;
    bool faulted = false;
};

struct TranslatedBlock
{
    void* hostEntry = nullptr;
    uint32_t entryBlock = 0;
};

struct NativeEntry
{
    void* hostEntry = nullptr;
    uint32_t entryBlock = 0;
    uint64_t regionAddress = 0;
};

struct RecompiledRegion
{
    LlvmBackend::CompiledFunction compiled;
    uint64_t startAddress = 0;
    uint64_t endAddress = 0;
    std::vector<uint64_t> blockAddresses;
};

struct GuestRuntimeState
{
    GuestRuntime::Configuration configuration;
    GuestRuntime::Statistics statistics;

    uint8_t* arena = nullptr;
    uint64_t arenaBase = 0;
    uint64_t arenaLength = 0;
    uint64_t arenaCursor = 0;
    uint64_t imageRegionBase = 0;
    bool imageSlotTaken[GuestRuntime::IMAGE_SLOT_COUNT] = {};
    uint64_t scratchCursor = 0;
    uint64_t pageBytes = 4096;
    bool traceHostCalls = false;
    bool traceHostCallStacks = false;
    std::vector<uint8_t> pageFlags;

    uint64_t stubBase = 0;
    uint64_t stubLength = 0;
    uint64_t mainThreadPointer = 0;

    ElfLoader::Namespace libraryNamespace;
    std::mutex libraryMutex;
    AbiBridge::Registry registry;
    std::deque<std::string> missingSymbolNames;

    std::unordered_map<uint64_t, Ir::Function> blocks;
    std::unordered_map<uint64_t, NativeEntry> nativeEntries;
    std::unordered_map<uint64_t, RecompiledRegion> recompiledRegions;
    std::unordered_map<uint64_t, uint64_t> blockHits;
    std::unordered_set<uint64_t> unrecompilable;
    std::vector<LlvmBackend::CompiledFunction> retiredCode;
    uint64_t codeLowAddress = UINT64_MAX;
    uint64_t codeHighAddress = 0;
    std::thread compiler;
    std::mutex compileMutex;
    std::condition_variable compileWake;
    std::deque<uint64_t> compileQueue;
    std::unordered_set<uint64_t> compileQueued;
    bool compileStopping = false;
    std::unordered_map<uint64_t, TranslatedBlock> translatedBlocks;
    std::mutex translatedMutex;
    std::deque<Ir::Function> retired;
    uint64_t invalidationEpoch = 0;
    LlvmBackend::Session backend;
    bool backendReady = false;
    Arm64Decoder::Context decoder;
    GuestState::MemoryAccessor memory;
    GuestSyscall::Statistics syscallStatistics;

    std::mutex arenaMutex;
    std::mutex blockMutex;
    std::mutex threadMutex;

    std::deque<GuestThread> threads;
    std::mutex stackMutex;
    std::vector<GuestState::GuestAddress> freeStacks;
    GuestRuntime::ThreadExitHandler threadExitHandler = nullptr;
    void* threadExitContext = nullptr;
};

static GuestRuntimeState* Implementation(GuestRuntime::Instance& instance)
{
    return (GuestRuntimeState*)instance.implementation;
}

static const GuestRuntimeState* Implementation(const GuestRuntime::Instance& instance)
{
    return (const GuestRuntimeState*)instance.implementation;
}

static void Bump(uint64_t& counter)
{
    std::atomic_ref<uint64_t>(counter).fetch_add(1, std::memory_order_relaxed);
}

static uint64_t RoundUpTo(uint64_t value, uint64_t alignment)
{
    if (alignment <= 1) return value;
    uint64_t remainder = value % alignment;
    if (remainder == 0) return value;
    return value + (alignment - remainder);
}

static uint64_t RoundDownTo(uint64_t value, uint64_t alignment)
{
    if (alignment <= 1) return value;
    return value - (value % alignment);
}

static bool WithinArena(const GuestRuntimeState* state, uint64_t address, uint64_t length)
{
    if (address < state->arenaBase) return false;
    uint64_t offset = address - state->arenaBase;
    if (offset > state->arenaLength) return false;
    return length <= state->arenaLength - offset;
}

static uint8_t* HostAddress(GuestRuntimeState* state, uint64_t address)
{
    return state->arena + (address - state->arenaBase);
}

static bool TouchesArena(const GuestRuntimeState* state, uint64_t address, uint64_t length)
{
    uint64_t span = length == 0 ? 1 : length;
    if (address + span < address) return true;
    if (address + span <= state->arenaBase) return false;

    return address < state->arenaBase + state->arenaLength;
}

static bool PagesAllow(const GuestRuntimeState* state, uint64_t address, uint64_t length, uint8_t required)
{
    if (!WithinArena(state, address, length)) return false;
    if (length == 0) return true;

    uint64_t first = (address - state->arenaBase) / state->pageBytes;
    uint64_t last = (address - state->arenaBase + length - 1) / state->pageBytes;

    for (uint64_t page = first; page <= last; ++page)
    {
        if (page >= state->pageFlags.size()) return false;
        if ((state->pageFlags[(size_t)page] & required) != required) return false;
    }

    return true;
}

static bool PagesTouch(const GuestRuntimeState* state, uint64_t address, uint64_t length, uint8_t wanted)
{
    if (length == 0) return false;
    if (!WithinArena(state, address, length)) return false;

    uint64_t first = (address - state->arenaBase) / state->pageBytes;
    uint64_t last = (address - state->arenaBase + length - 1) / state->pageBytes;

    for (uint64_t page = first; page <= last; ++page)
    {
        if (page >= state->pageFlags.size()) return false;
        if ((state->pageFlags[(size_t)page] & wanted) != 0) return true;
    }

    return false;
}

static uint64_t InvalidateRange(GuestRuntimeState* state, uint64_t start, uint64_t end);
static std::string DescribeGuestAddress(GuestRuntimeState* state, uint64_t address);

static bool MemoryRead(void* context, GuestState::GuestAddress address, void* destination, uint64_t length)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    uint64_t raw = (uint64_t)address;

    if (TouchesArena(state, raw, length))
    {
        if (!PagesAllow(state, raw, length, PAGE_COMMITTED | PAGE_READABLE)) return false;
        if (length != 0) memcpy(destination, HostAddress(state, raw), (size_t)length);
        return true;
    }

    if (raw < HOST_ADDRESS_FLOOR) return false;
    if (length != 0) memcpy(destination, (const void*)raw, (size_t)length);

    return true;
}

static bool MemoryWrite(void* context, GuestState::GuestAddress address, const void* source, uint64_t length)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    uint64_t raw = (uint64_t)address;

    if (TouchesArena(state, raw, length))
    {
        if (!PagesAllow(state, raw, length, PAGE_COMMITTED | PAGE_WRITABLE)) return false;

        if (length != 0 && PagesTouch(state, raw, length, PAGE_EXECUTABLE))
        {
            Bump(state->statistics.codeWritesObserved);
            InvalidateRange(state, raw, raw + length);
        }

        if (length != 0) memcpy(HostAddress(state, raw), source, (size_t)length);
        return true;
    }

    if (raw < HOST_ADDRESS_FLOOR) return false;
    if (length != 0) memcpy((void*)raw, source, (size_t)length);

    return true;
}

static void* MemoryTranslateForAtomic(void* context, GuestState::GuestAddress address, uint64_t length)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    uint64_t raw = (uint64_t)address;

    if (TouchesArena(state, raw, length))
    {
        if (!PagesAllow(state, raw, length, PAGE_COMMITTED | PAGE_READABLE | PAGE_WRITABLE)) return nullptr;
        return HostAddress(state, raw);
    }

    if (raw < HOST_ADDRESS_FLOOR) return nullptr;
    return (void*)raw;
}

static GuestRuntime::Result ReserveIn(GuestRuntimeState* state, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base)
{
    if (!state) return GuestRuntime::Result::ArenaUnavailable;
    if (length == 0) return GuestRuntime::Result::OutOfGuestMemory;

    std::lock_guard<std::mutex> guard(state->arenaMutex);

    if (length == 0) return GuestRuntime::Result::OutOfGuestMemory;

    uint64_t granularity = alignment < state->pageBytes ? state->pageBytes : alignment;
    uint64_t candidate = RoundUpTo(state->arenaCursor, granularity);
    uint64_t rounded = RoundUpTo(length, state->pageBytes);

    if (rounded < length) return GuestRuntime::Result::OutOfGuestMemory;
    if (candidate < state->arenaCursor) return GuestRuntime::Result::OutOfGuestMemory;

    if (!WithinArena(state, candidate, rounded))
    {
        fprintf(stderr,
                "[guestruntime] the arena cannot place %llu bytes at %016llx: it spans %016llx..%016llx and %llu MiB are already "
                "handed out\n",
                (unsigned long long)rounded, (unsigned long long)candidate, (unsigned long long)state->configuration.arenaBase,
                (unsigned long long)(state->configuration.arenaBase + state->configuration.arenaLength),
                (unsigned long long)((state->arenaCursor - state->configuration.arenaBase) >> 20));

        return GuestRuntime::Result::OutOfGuestMemory;
    }

    state->arenaCursor = candidate + rounded + state->pageBytes;
    base = (GuestState::GuestAddress)candidate;

    if (Settings::Tracing("arena") && rounded >= (1ull << 20))
    {
        fprintf(stderr, "[guestruntime] arena handed out %llu MiB at %016llx, %llu MiB used in total\n",
                (unsigned long long)(rounded >> 20), (unsigned long long)candidate,
                (unsigned long long)((state->arenaCursor - state->configuration.arenaBase) >> 20));
    }

    return GuestRuntime::Result::Ok;
}

static int ProtectionBits(bool readable, bool writable, bool executable)
{
    int protection = 0;
    if (readable || executable) protection |= PROT_READ;
    if (writable) protection |= PROT_WRITE;
    return protection;
}

static GuestRuntime::Result ProtectIn(GuestRuntimeState* state, uint64_t address, uint64_t length, bool readable, bool writable,
                                      bool executable)
{
    if (!state) return GuestRuntime::Result::ArenaUnavailable;
    if (length == 0) return GuestRuntime::Result::Ok;

    uint64_t start = RoundDownTo(address, state->pageBytes);
    uint64_t end = RoundUpTo(address + length, state->pageBytes);
    if (!WithinArena(state, start, end - start)) return GuestRuntime::Result::OutOfGuestMemory;

    bool leavesExecutable = false;

    {
        std::lock_guard<std::mutex> guard(state->arenaMutex);

        for (uint64_t page = (start - state->arenaBase) / state->pageBytes; page < (end - state->arenaBase) / state->pageBytes; ++page)
        {
            if ((state->pageFlags[(size_t)page] & PAGE_COMMITTED) == 0) return GuestRuntime::Result::OutOfGuestMemory;
            if ((state->pageFlags[(size_t)page] & PAGE_EXECUTABLE) != 0) leavesExecutable = true;
        }

        if (mprotect(HostAddress(state, start), (size_t)(end - start), ProtectionBits(readable, writable, executable)) != 0)
            return GuestRuntime::Result::OutOfGuestMemory;

        uint8_t flags = PAGE_COMMITTED;
        if (readable || executable) flags |= PAGE_READABLE;
        if (writable) flags |= PAGE_WRITABLE;
        if (executable) flags |= PAGE_EXECUTABLE;

        for (uint64_t page = (start - state->arenaBase) / state->pageBytes; page < (end - state->arenaBase) / state->pageBytes; ++page)
            state->pageFlags[(size_t)page] = flags;
    }

    if (leavesExecutable && writable)
    {
        Bump(state->statistics.codeWritesObserved);
        InvalidateRange(state, start, end);
    }

    return GuestRuntime::Result::Ok;
}

static GuestRuntime::Result ReserveScratchIn(GuestRuntimeState* state, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base)
{
    if (!state) return GuestRuntime::Result::ArenaUnavailable;
    if (length == 0) return GuestRuntime::Result::OutOfGuestMemory;

    std::lock_guard<std::mutex> guard(state->arenaMutex);

    uint64_t granularity = alignment < state->pageBytes ? state->pageBytes : alignment;
    uint64_t rounded = RoundUpTo(length, state->pageBytes);
    if (rounded < length) return GuestRuntime::Result::OutOfGuestMemory;

    uint64_t needed = rounded + state->pageBytes;
    if (state->scratchCursor < state->arenaBase + needed) return GuestRuntime::Result::OutOfGuestMemory;

    uint64_t candidate = (state->scratchCursor - needed) & ~(granularity - 1);
    if (candidate < state->arenaCursor) return GuestRuntime::Result::OutOfGuestMemory;

    state->scratchCursor = candidate;
    base = (GuestState::GuestAddress)candidate;

    if (Settings::Tracing("arena") && rounded >= (1ull << 20))
    {
        fprintf(stderr, "[guestruntime] the arena handed out %llu scratch MiB at %016llx\n", (unsigned long long)(rounded >> 20),
                (unsigned long long)candidate);
    }

    return GuestRuntime::Result::Ok;
}

static GuestRuntime::Result CommitIn(GuestRuntimeState* state, uint64_t address, uint64_t length, bool readable, bool writable, bool executable)
{
    if (!state) return GuestRuntime::Result::ArenaUnavailable;
    if (length == 0) return GuestRuntime::Result::Ok;

    uint64_t start = RoundDownTo(address, state->pageBytes);
    uint64_t end = RoundUpTo(address + length, state->pageBytes);
    if (!WithinArena(state, start, end - start)) return GuestRuntime::Result::OutOfGuestMemory;

    uint64_t firstPage = (start - state->arenaBase) / state->pageBytes;
    uint64_t lastPage = (end - state->arenaBase) / state->pageBytes;

    uint64_t runStart = 0;
    bool inRun = false;

    std::unique_lock<std::mutex> guard(state->arenaMutex);

    for (uint64_t page = firstPage; page <= lastPage; ++page)
    {
        bool fresh = page < lastPage && (state->pageFlags[(size_t)page] & PAGE_COMMITTED) == 0;

        if (fresh && !inRun)
        {
            runStart = page;
            inRun = true;
        }

        if (fresh || !inRun) continue;

        uint64_t runAddress = state->arenaBase + runStart * state->pageBytes;
        uint64_t runLength = (page - runStart) * state->pageBytes;

        void* mapped = mmap(HostAddress(state, runAddress), (size_t)runLength, PROT_READ | PROT_WRITE,
                            MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0);
        if (mapped == MAP_FAILED) return GuestRuntime::Result::OutOfGuestMemory;

        for (uint64_t marked = runStart; marked < page; ++marked) state->pageFlags[(size_t)marked] = PAGE_COMMITTED;

        inRun = false;
    }

    guard.unlock();

    return ProtectIn(state, start, end - start, readable, writable, executable);
}

static bool ReserveCallback(void* context, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base)
{
    return ReserveIn((GuestRuntimeState*)context, length, alignment, base) == GuestRuntime::Result::Ok;
}

static bool DeterministicPlacementDisabled()
{
    const char* disabled = Settings::Lookup(GuestRuntime::IMAGE_PLACEMENT_SETTING);
    return disabled && strcmp(disabled, "0") == 0;
}

static uint64_t ImageSlotHash(const char* name)
{
    uint64_t hash = 0xcbf29ce484222325ull;

    for (const char* cursor = name; cursor && *cursor; ++cursor)
    {
        hash ^= (uint64_t)(unsigned char)*cursor;
        hash *= 0x100000001b3ull;
    }

    return hash;
}

static bool ReserveForImageCallback(void* context, const char* name, uint64_t length, uint64_t alignment,
                                    GuestState::GuestAddress& base)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    if (!state || !name || !*name) return false;

    if (DeterministicPlacementDisabled()) return ReserveIn(state, length, alignment, base) == GuestRuntime::Result::Ok;

    if (length > GuestRuntime::IMAGE_SLOT_BYTES || alignment > GuestRuntime::IMAGE_SLOT_BYTES)
    {
        fprintf(stderr, "[guestruntime] %s needs %llu MiB, more than one image slot, so it is placed after the others\n", name,
                (unsigned long long)(length >> 20));

        return ReserveIn(state, length, alignment, base) == GuestRuntime::Result::Ok;
    }

    std::lock_guard<std::mutex> guard(state->arenaMutex);

    const uint64_t start = ImageSlotHash(name) % GuestRuntime::IMAGE_SLOT_COUNT;

    for (uint64_t probe = 0; probe < GuestRuntime::IMAGE_SLOT_COUNT; ++probe)
    {
        const uint64_t slot = (start + probe) % GuestRuntime::IMAGE_SLOT_COUNT;
        if (state->imageSlotTaken[slot]) continue;

        state->imageSlotTaken[slot] = true;
        base = (GuestState::GuestAddress)(state->imageRegionBase + slot * GuestRuntime::IMAGE_SLOT_BYTES);

        if (probe != 0)
        {
            fprintf(stderr, "[guestruntime] %s wanted slot %llu but it was taken, so it sits at %llu and its cache key moved\n", name,
                    (unsigned long long)start, (unsigned long long)slot);
        }

        return true;
    }

    return false;
}

static bool CommitCallback(void* context, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable)
{
    return CommitIn((GuestRuntimeState*)context, (uint64_t)address, length, readable, writable, executable) == GuestRuntime::Result::Ok;
}

static bool ProtectCallback(void* context, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable, bool executable)
{
    return ProtectIn((GuestRuntimeState*)context, (uint64_t)address, length, readable, writable, executable) == GuestRuntime::Result::Ok;
}

static bool ReleaseCallback(void* context, GuestState::GuestAddress address, uint64_t length)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    uint64_t raw = (uint64_t)address;

    if (!WithinArena(state, raw, length)) return false;

    uint64_t start = RoundDownTo(raw, state->pageBytes);
    uint64_t end = RoundUpTo(raw + length, state->pageBytes);

    std::lock_guard<std::mutex> guard(state->arenaMutex);

    if (mmap(HostAddress(state, start), (size_t)(end - start), PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0) == MAP_FAILED)
        return false;

    for (uint64_t page = (start - state->arenaBase) / state->pageBytes; page < (end - state->arenaBase) / state->pageBytes; ++page)
        state->pageFlags[(size_t)page] = 0;

    return true;
}

static AbiBridge::Result MissingSymbolHandler(void* context, GuestState::Registers& registers, const GuestState::MemoryAccessor& memory)
{
    (void)registers;
    (void)memory;

    Diagnostics::NoteGap(Diagnostics::Gap::GuestSymbol, (const char*)context);

    fprintf(stderr, "[guestruntime] the guest called unimplemented symbol %s\n", (const char*)context);
    return AbiBridge::Result::UnknownSymbol;
}

static bool ResolveToGuestStubCallback(void* context, const char* name, GuestState::GuestAddress& guestStubAddress)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    if (AbiBridge::LookupGuestStub(state->registry, name, guestStubAddress) == AbiBridge::Result::Ok) return true;
    if (!state->configuration.stubUnresolvedSymbols) return false;

    state->missingSymbolNames.push_back(name);
    const char* stored = state->missingSymbolNames.back().c_str();

    if (AbiBridge::RegisterHostHandler(state->registry, name, &MissingSymbolHandler, (void*)stored) != AbiBridge::Result::Ok) return false;

    return AbiBridge::LookupGuestStub(state->registry, name, guestStubAddress) == AbiBridge::Result::Ok;
}

static bool LookupDefinedStubCallback(void* context, const char* name, GuestState::GuestAddress& guestStubAddress)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    return AbiBridge::LookupGuestStub(state->registry, name, guestStubAddress) == AbiBridge::Result::Ok;
}

static bool ResolveDataObjectCallback(void* context, const char* name, GuestState::GuestAddress& guestAddress)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    if (AbiBridge::LookupGuestData(state->registry, name, guestAddress) == AbiBridge::Result::Ok) return true;
    if (!state->configuration.stubUnresolvedSymbols) return false;

    GuestState::GuestAddress base{};
    if (ReserveScratchIn(state, UNRESOLVED_DATA_OBJECT_BYTES, sizeof(uint64_t), base) != GuestRuntime::Result::Ok) return false;
    if (CommitIn(state, (uint64_t)base, UNRESOLVED_DATA_OBJECT_BYTES, true, true, false) != GuestRuntime::Result::Ok) return false;

    if (AbiBridge::RegisterHostData(state->registry, name, base, UNRESOLVED_DATA_OBJECT_BYTES) != AbiBridge::Result::Ok) return false;

    fprintf(stderr, "[guestruntime] unimplemented data object %s was given %llu zeroed guest bytes at %016llx\n", name,
            (unsigned long long)UNRESOLVED_DATA_OBJECT_BYTES, (unsigned long long)base);

    guestAddress = base;
    return true;
}

static bool IsProxyLibraryCallback(void* context, const char* soname)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    return AbiBridge::IsProxyLibrary(state->registry, soname);
}

static const Ir::Block* BlockAt(GuestRuntimeState* state, uint64_t address, GuestRuntime::Result& failure)
{
    std::lock_guard<std::mutex> guard(state->blockMutex);

    auto existing = state->blocks.find(address);
    if (existing != state->blocks.end()) return &existing->second.blocks.front();

    Ir::Function function;
    function.entryAddress = (GuestState::GuestAddress)address;

    if (AbiBridge::IsGuestStub(state->registry, (GuestState::GuestAddress)address))
    {
        Ir::Block stub;
        stub.guestAddress = (GuestState::GuestAddress)address;
        stub.guestEndAddress = (GuestState::GuestAddress)address;
        stub.endsWithIndirectBranch = true;

        Ir::Instruction call;
        call.opcode = Ir::Opcode::HostCall;
        call.guestAddress = (GuestState::GuestAddress)address;
        call.immediate = address;
        stub.instructions.push_back(call);

        function.endAddress = (GuestState::GuestAddress)address;
        function.blocks.push_back(std::move(stub));
    }
    else
    {
        Ir::Block block;
        Arm64Decoder::Result decoded = Arm64Decoder::DecodeBlock(state->decoder, (GuestState::GuestAddress)address, function, block);

        if (decoded == Arm64Decoder::Result::BlockTooLong && !block.instructions.empty()) decoded = Arm64Decoder::Result::Ok;

        if (decoded != Arm64Decoder::Result::Ok)
        {
            uint64_t failingAddress = block.instructions.empty() ? address : (uint64_t)block.guestEndAddress;
            uint32_t encoding = 0;
            state->memory.Read(state->memory.context, (GuestState::GuestAddress)failingAddress, &encoding, sizeof(encoding));

            fprintf(stderr, "[guestruntime] decode failed in the block at %016llx: %s at %016llx (encoding %08x)\n",
                    (unsigned long long)address, Arm64Decoder::ResultName(decoded), (unsigned long long)failingAddress, encoding);

            failure = GuestRuntime::Result::DecodeFailed;
            return nullptr;
        }

        function.endAddress = block.guestEndAddress;
        function.blocks.push_back(std::move(block));
    }

    if (!Ir::Verify(function))
    {
        fprintf(stderr, "[guestruntime] decoded IR at %016llx does not verify\n", (unsigned long long)address);
        failure = GuestRuntime::Result::DecodeFailed;
        return nullptr;
    }

    uint64_t reach = (uint64_t)function.endAddress + state->configuration.recompileRegionBytes + Arm64Decoder::INSTRUCTION_SIZE_BYTES;
    if (reach < (uint64_t)function.endAddress) reach = UINT64_MAX;

    if (address < state->codeLowAddress) state->codeLowAddress = address;
    if (reach > state->codeHighAddress) state->codeHighAddress = reach;

    Bump(state->statistics.blocksDecoded);
    auto inserted = state->blocks.emplace(address, std::move(function));

    return &inserted.first->second.blocks.front();
}

static uint64_t ExecutableRunEnd(GuestRuntimeState* state, uint64_t address, uint64_t limit)
{
    std::lock_guard<std::mutex> guard(state->arenaMutex);

    if (!WithinArena(state, address, 1)) return address;

    uint64_t end = RoundDownTo(address, state->pageBytes);

    while (end < limit)
    {
        uint64_t page = (end - state->arenaBase) / state->pageBytes;
        if (page >= state->pageFlags.size()) break;
        if ((state->pageFlags[(size_t)page] & (PAGE_COMMITTED | PAGE_EXECUTABLE)) != (PAGE_COMMITTED | PAGE_EXECUTABLE)) break;

        end += state->pageBytes;
    }

    return end < limit ? end : limit;
}

static bool DecodeRegion(GuestRuntimeState* state, uint64_t address, Ir::Function& region)
{
    uint64_t limit = address + state->configuration.recompileRegionBytes;
    if (limit <= address) return false;

    uint64_t end = ExecutableRunEnd(state, address, limit);
    if (end <= address + Arm64Decoder::INSTRUCTION_SIZE_BYTES) return false;

    Arm64Decoder::Context decoder;
    decoder.memory = state->memory;
    decoder.trapOnUndecodable = true;

    if (Arm64Decoder::DecodeFunction(decoder, (GuestState::GuestAddress)address, (GuestState::GuestAddress)end, region) !=
        Arm64Decoder::Result::Ok)
        return false;

    if (region.blocks.empty() || (uint64_t)region.blocks.front().guestAddress != address) return false;

    std::vector<Ir::Block> lowerable;
    lowerable.reserve(region.blocks.size());

    for (Ir::Block& block : region.blocks)
    {
        bool keep = (uint64_t)block.guestAddress == address;

        if (!keep)
        {
            keep = true;
            for (const Ir::Instruction& instruction : block.instructions)
            {
                if (LlvmBackend::CanLowerInstruction(instruction)) continue;

                keep = false;
                break;
            }
        }

        if (keep) lowerable.push_back(std::move(block));
    }

    region.blocks = std::move(lowerable);
    return Ir::Verify(region);
}

static bool CompileRecompiledUnit(GuestRuntimeState* state, const Ir::Function& function, LlvmBackend::CompiledFunction& compiled,
                                  Ir::Opcode& unsupported)
{
    if (!LlvmBackend::CanLower(function, unsupported)) return false;

    if (LlvmBackend::CompileFunction(state->backend, function, state->configuration.tier, compiled) != LlvmBackend::Result::Ok)
        return false;

    return compiled.hostEntryPoint != nullptr;
}

static NativeEntry RecompileAt(GuestRuntimeState* state, uint64_t address)
{
    Ir::Function single;
    uint64_t epoch = 0;

    {
        std::lock_guard<std::mutex> guard(state->blockMutex);

        auto existing = state->nativeEntries.find(address);
        if (existing != state->nativeEntries.end()) return existing->second;

        if (state->unrecompilable.count(address) != 0) return NativeEntry{};

        auto found = state->blocks.find(address);
        if (found == state->blocks.end()) return NativeEntry{};

        single = found->second;
        epoch = state->invalidationEpoch;
    }

    Ir::Function region;
    Ir::Opcode unsupported = Ir::Opcode::Invalid;
    LlvmBackend::CompiledFunction compiled;

    bool built = DecodeRegion(state, address, region) && CompileRecompiledUnit(state, region, compiled, unsupported);

    if (!built)
    {
        region = std::move(single);
        unsupported = Ir::Opcode::Invalid;
        built = CompileRecompiledUnit(state, region, compiled, unsupported);
    }

    NativeEntry entry;
    bool refused = false;

    {
        std::lock_guard<std::mutex> guard(state->blockMutex);

        auto raced = state->nativeEntries.find(address);

        if (epoch != state->invalidationEpoch || raced != state->nativeEntries.end())
        {
            if (built) LlvmBackend::ReleaseFunction(state->backend, compiled);
            return raced != state->nativeEntries.end() ? raced->second : NativeEntry{};
        }

        if (!built)
        {
            state->unrecompilable.insert(address);
            state->blockHits.erase(address);
            Bump(state->statistics.recompileFailures);
            refused = true;
        }
        else
        {
            RecompiledRegion record;
            record.compiled = compiled;
            record.startAddress = address;
            record.endAddress = address;

            for (uint64_t index = 0; index < region.blocks.size(); ++index)
            {
                const Ir::Block& block = region.blocks[index];
                uint64_t blockAddress = (uint64_t)block.guestAddress;
                uint64_t blockEnd = (uint64_t)block.guestEndAddress;

                if (blockAddress < record.startAddress) record.startAddress = blockAddress;
                if (blockEnd > record.endAddress) record.endAddress = blockEnd;

                record.blockAddresses.push_back(blockAddress);

                if (state->nativeEntries.count(blockAddress) != 0) continue;

                NativeEntry published;
                published.hostEntry = compiled.hostEntryPoint;
                published.entryBlock = (uint32_t)index;
                published.regionAddress = address;
                state->nativeEntries[blockAddress] = published;
            }

            if (record.endAddress <= record.startAddress) record.endAddress = record.startAddress + Arm64Decoder::INSTRUCTION_SIZE_BYTES;

            state->recompiledRegions[address] = std::move(record);
            state->blockHits.erase(address);
            std::atomic_ref<uint64_t>(state->statistics.blocksRecompiled).fetch_add(region.blocks.size(), std::memory_order_relaxed);
            Bump(state->statistics.regionsRecompiled);

            entry = state->nativeEntries[address];
        }
    }

    if (refused && Settings::Tracing("blocks"))
    {
        const bool named = unsupported != Ir::Opcode::Invalid;

        fprintf(stderr, "[guestruntime] %016llx (%s) stays interpreted: the recompiler could not compile it%s%s\n",
                (unsigned long long)address, DescribeGuestAddress(state, address).c_str(), named ? ", it cannot lower " : "",
                named ? Ir::OpcodeName(unsupported) : "");
    }

    return entry;
}

static NativeEntry NativeEntryFor(GuestRuntimeState* state, uint64_t address)
{
    if (!state->backendReady) return NativeEntry{};

    bool queued = false;

    {
        std::lock_guard<std::mutex> guard(state->blockMutex);

        auto existing = state->nativeEntries.find(address);
        if (existing != state->nativeEntries.end()) return existing->second;

        if (state->unrecompilable.count(address) != 0) return NativeEntry{};
        if (state->blocks.count(address) == 0) return NativeEntry{};

        uint64_t hits = ++state->blockHits[address];
        if (hits < state->configuration.recompileThreshold) return NativeEntry{};

        if (state->configuration.recompileInBackground)
        {
            std::lock_guard<std::mutex> compileGuard(state->compileMutex);
            if (!state->compileQueued.insert(address).second) return NativeEntry{};

            state->compileQueue.push_back(address);
            queued = true;
        }
    }

    if (queued)
    {
        state->compileWake.notify_one();
        return NativeEntry{};
    }

    return RecompileAt(state, address);
}

static void RunBackgroundCompiler(GuestRuntimeState* state)
{
    for (;;)
    {
        uint64_t address = 0;

        {
            std::unique_lock<std::mutex> guard(state->compileMutex);
            state->compileWake.wait(guard, [state]() { return state->compileStopping || !state->compileQueue.empty(); });
            if (state->compileStopping) return;

            address = state->compileQueue.front();
            state->compileQueue.pop_front();
        }

        RecompileAt(state, address);

        std::lock_guard<std::mutex> guard(state->compileMutex);
        state->compileQueued.erase(address);
    }
}

static void StopBackgroundCompiler(GuestRuntimeState* state)
{
    if (!state->compiler.joinable()) return;

    {
        std::lock_guard<std::mutex> guard(state->compileMutex);
        state->compileStopping = true;
    }

    state->compileWake.notify_all();
    state->compiler.join();
}

static GuestRuntime::Result AllocateStackIn(GuestRuntimeState* state, uint64_t bytes, GuestState::GuestAddress& stackTop)
{
    GuestState::GuestAddress base{};
    GuestRuntime::Result reserved = ReserveScratchIn(state, bytes, Platform::GUEST_MAX_PAGE_BYTES, base);
    if (reserved != GuestRuntime::Result::Ok) return reserved;

    GuestRuntime::Result committed = CommitIn(state, (uint64_t)base, bytes, true, true, false);
    if (committed != GuestRuntime::Result::Ok) return committed;

    stackTop = (GuestState::GuestAddress)((uint64_t)base + bytes - 16);
    return GuestRuntime::Result::Ok;
}

static GuestRuntime::Result TakeStack(GuestRuntimeState* state, GuestState::GuestAddress& stackTop)
{
    {
        std::lock_guard<std::mutex> guard(state->stackMutex);

        if (!state->freeStacks.empty())
        {
            stackTop = state->freeStacks.back();
            state->freeStacks.pop_back();
            return GuestRuntime::Result::Ok;
        }
    }

    return AllocateStackIn(state, state->configuration.stackBytes, stackTop);
}

static void ReturnStack(GuestRuntimeState* state, GuestState::GuestAddress stackTop)
{
    std::lock_guard<std::mutex> guard(state->stackMutex);
    state->freeStacks.push_back(stackTop);
}

struct BorrowedStack
{
    GuestRuntimeState* state = nullptr;
    GuestState::GuestAddress top{};
    bool held = false;

    ~BorrowedStack()
    {
        if (held) ReturnStack(state, top);
    }
};

static std::atomic<uint64_t> gNextCallToken{1};
static thread_local uint64_t gCurrentCallToken = 0;
static thread_local GuestState::Registers* gActiveRegisters = nullptr;
static thread_local GuestRuntimeState* gActiveState = nullptr;
static thread_local uint64_t gRecentEntries[RECENT_ENTRY_COUNT] = {};
static thread_local uint64_t gRecentEntryCursor = 0;
static thread_local uint64_t gGuestStackTop = 0;
static thread_local uint64_t gGuestStackBytes = 0;
static thread_local uint64_t gBlockTrace[LlvmBackend::BLOCK_TRACE_ENTRIES] = {};
static thread_local uint64_t gBlockTraceCursor = 0;

static void AttachBlockTrace(LlvmBackend::RuntimeContext& runtime)
{
    runtime.blockTraceEntries = gBlockTrace;
    runtime.blockTraceCursor = &gBlockTraceCursor;
}

static void EnsureFaultStack()
{
    static thread_local bool considered = false;
    static thread_local std::vector<uint8_t> reserved;

    if (considered) return;
    considered = true;

    stack_t established;
    memset(&established, 0, sizeof(established));

    if (sigaltstack(nullptr, &established) != 0) return;
    if ((established.ss_flags & SS_DISABLE) == 0) return;

    reserved.assign(FAULT_STACK_BYTES, 0);

    stack_t described;
    memset(&described, 0, sizeof(described));
    described.ss_sp = reserved.data();
    described.ss_size = reserved.size();

    sigaltstack(&described, nullptr);
}

struct ActiveRegisterScope
{
    GuestState::Registers* previousRegisters = nullptr;
    GuestRuntimeState* previousState = nullptr;

    ActiveRegisterScope(GuestRuntimeState* state, GuestState::Registers& registers)
    {
        previousRegisters = gActiveRegisters;
        previousState = gActiveState;
        gActiveRegisters = &registers;
        gActiveState = state;
    }

    ~ActiveRegisterScope()
    {
        gActiveRegisters = previousRegisters;
        gActiveState = previousState;
    }
};

struct CallTokenScope
{
    uint64_t previous = 0;

    CallTokenScope()
    {
        previous = gCurrentCallToken;
        gCurrentCallToken = gNextCallToken.fetch_add(1, std::memory_order_relaxed);
    }

    ~CallTokenScope()
    {
        gCurrentCallToken = previous;
    }
};

uint64_t GuestRuntime::CurrentCallToken()
{
    return gCurrentCallToken;
}

static bool DescribeAddressIn(GuestRuntimeState* state, uint64_t address, GuestRuntime::Description& description)
{
    for (const ElfLoader::Library& library : state->libraryNamespace.loaded)
    {
        uint64_t base = (uint64_t)library.reservationAddress;
        if (address < base || address >= base + library.reservationLength) continue;

        description.soname = library.soname;
        description.path = library.path;
        description.moduleBase = base;

        const ElfLoader::Symbol* nearest = nullptr;
        for (const ElfLoader::Symbol& symbol : library.exports)
        {
            if (!symbol.isDefined || !symbol.isFunction) continue;

            uint64_t value = (uint64_t)symbol.guestAddress;
            if (value > address) continue;
            if (nearest && value <= (uint64_t)nearest->guestAddress) continue;

            nearest = &symbol;
        }

        if (nearest)
        {
            description.symbolName = nearest->name;
            description.symbolAddress = (uint64_t)nearest->guestAddress;
        }

        return true;
    }

    return false;
}

static std::string DescribeGuestAddress(GuestRuntimeState* state, uint64_t address)
{
    GuestRuntime::Description description;

    {
        std::lock_guard<std::mutex> guard(state->libraryMutex);
        if (!DescribeAddressIn(state, address, description)) return std::string("?");
    }

    char described[512];
    if (!description.symbolName.empty())
    {
        snprintf(described, sizeof(described), "%s+0x%llx (%s+0x%llx)", description.soname.c_str(),
                 (unsigned long long)(address - description.moduleBase), description.symbolName.c_str(),
                 (unsigned long long)(address - description.symbolAddress));
    }
    else
    {
        snprintf(described, sizeof(described), "%s+0x%llx", description.soname.c_str(),
                 (unsigned long long)(address - description.moduleBase));
    }

    return std::string(described);
}

static constexpr int INTERPRETED_SAMPLE_SLOTS = 24;
static constexpr uint64_t INTERPRETED_SAMPLE_STRIDE = 4096;

static std::mutex gInterpretedLock;
static uint64_t gInterpretedAddress[INTERPRETED_SAMPLE_SLOTS];
static uint64_t gInterpretedCount[INTERPRETED_SAMPLE_SLOTS];
static std::atomic<uint64_t> gInterpretedSeen{0};
static bool gInterpretedWanted = false;

static void NoteInterpretedBlock(uint64_t address)
{
    if (!gInterpretedWanted) return;
    if ((gInterpretedSeen.fetch_add(1, std::memory_order_relaxed) % INTERPRETED_SAMPLE_STRIDE) != 0) return;

    std::lock_guard<std::mutex> guard(gInterpretedLock);

    int weakest = 0;
    for (int slot = 0; slot < INTERPRETED_SAMPLE_SLOTS; ++slot)
    {
        if (gInterpretedAddress[slot] == address)
        {
            gInterpretedCount[slot] += 1;
            return;
        }

        if (gInterpretedCount[slot] < gInterpretedCount[weakest]) weakest = slot;
    }

    gInterpretedAddress[weakest] = address;
    gInterpretedCount[weakest] = 1;
}

static void ReportInterpretedBlocks(GuestRuntimeState* state)
{
    std::lock_guard<std::mutex> guard(gInterpretedLock);

    for (int slot = 0; slot < INTERPRETED_SAMPLE_SLOTS; ++slot)
    {
        if (gInterpretedCount[slot] < 4) continue;

        fprintf(stderr, "[guestruntime]   %llu samples at %016llx %s\n", (unsigned long long)gInterpretedCount[slot],
                (unsigned long long)gInterpretedAddress[slot], DescribeGuestAddress(state, gInterpretedAddress[slot]).c_str());

        gInterpretedCount[slot] = 0;
        gInterpretedAddress[slot] = 0;
    }
}

static void StartBlockReporter(GuestRuntimeState* state)
{
    if (!Settings::Tracing("blocks")) return;

    uint64_t seconds = 5;

    gInterpretedWanted = true;

    std::thread reporter([state, seconds]() {
        uint64_t previousTranslated = 0;
        uint64_t previousInterpreted = 0;
        uint64_t previousRecompiled = 0;

        for (;;)
        {
            struct timespec span;
            span.tv_sec = (time_t)seconds;
            span.tv_nsec = 0;
            nanosleep(&span, nullptr);

            uint64_t translated = state->statistics.blocksExecutedTranslated + state->statistics.blocksExecutedNative;
            uint64_t interpreted = state->statistics.blocksInterpreted;

            uint64_t translatedNow = translated - previousTranslated;
            uint64_t interpretedNow = interpreted - previousInterpreted;
            uint64_t total = translatedNow + interpretedNow;

            previousTranslated = translated;
            previousInterpreted = interpreted;

            uint64_t recompiled = state->statistics.regionsRecompiled;
            uint64_t recompiledNow = recompiled - previousRecompiled;
            previousRecompiled = recompiled;

            if (total == 0 && recompiledNow == 0) continue;

            fprintf(stderr,
                    "[guestruntime] %llu blocks in the last %llus: %llu translated, %llu interpreted (%llu%% interpreted), %llu compiled on "
                    "demand\n",
                    (unsigned long long)total, (unsigned long long)seconds, (unsigned long long)translatedNow,
                    (unsigned long long)interpretedNow, (unsigned long long)(total == 0 ? 0 : interpretedNow * 100 / total),
                    (unsigned long long)recompiledNow);

            if (interpretedNow * 2 > total) ReportInterpretedBlocks(state);
        }
    });

    reporter.detach();
}

static void DescribeAddressForSync(void* context, uint64_t address, char* buffer, uint64_t bytes)
{
    GuestRuntimeState* state = (GuestRuntimeState*)context;
    if (!state || bytes == 0) return;

    std::string described = DescribeGuestAddress(state, address);
    snprintf(buffer, (size_t)bytes, "%016llx %s", (unsigned long long)address, described.c_str());
}

static bool DescribeTranslatedEntry(uint64_t entryAddress, char* out, size_t capacity)
{
    GuestRuntimeState* state = gActiveState;
    if (!state || !out || capacity == 0) return false;

    const std::string described = DescribeGuestAddress(state, entryAddress);
    if (described.empty()) return false;

    snprintf(out, capacity, "%s", described.c_str());
    return true;
}

static void ReportLoadedModules()
{
    GuestRuntimeState* state = gActiveState;
    if (!state) return;

    if (!state->libraryMutex.try_lock())
    {
        fprintf(stderr, "[guestruntime]   the module list is held by the faulting thread, so it is not listed here\n");
        return;
    }

    fprintf(stderr, "[guestruntime]   the guest modules loaded at the fault, so an address above can be placed:\n");

    for (const ElfLoader::Library& library : state->libraryNamespace.loaded)
    {
        fprintf(stderr, "[guestruntime]     %016llx %s\n", (unsigned long long)library.loadBias, library.path.c_str());
    }

    state->libraryMutex.unlock();
}

static void ReportGuestRegisters(uint64_t faultAddress)
{
    GuestState::Registers* registers = gActiveRegisters;
    GuestRuntimeState* state = gActiveState;
    if (!registers || !state) return;

    fprintf(stderr,
            "[guestruntime] the faulting guest thread was at sp=%016llx tp=%016llx x0=%016llx x1=%016llx x2=%016llx x30=%016llx, "
            "touching %016llx\n",
            (unsigned long long)registers->stackPointer, (unsigned long long)registers->threadPointer,
            (unsigned long long)registers->general[0], (unsigned long long)registers->general[1],
            (unsigned long long)registers->general[2], (unsigned long long)registers->general[30],
            (unsigned long long)faultAddress);

    fprintf(stderr, "[guestruntime]   these guest registers are as of the last write-back, not as of the faulting instruction, so a\n");
    fprintf(stderr, "[guestruntime]   fault inside translated code will show values the faulting instruction never saw:\n");

    for (int row = 0; row < GuestState::GENERAL_REGISTER_COUNT; row += 4)
    {
        fprintf(stderr, "[guestruntime]  ");

        for (int column = 0; column < 4 && row + column < GuestState::GENERAL_REGISTER_COUNT; ++column)
        {
            fprintf(stderr, " x%-2d=%016llx", row + column, (unsigned long long)registers->general[row + column]);
        }

        fprintf(stderr, "\n");
    }

    if (faultAddress >= state->arenaBase && faultAddress < state->arenaBase + state->arenaLength)
    {
        uint64_t page = (faultAddress - state->arenaBase) / state->pageBytes;
        uint8_t flags = page < state->pageFlags.size() ? state->pageFlags[(size_t)page] : 0;

        GuestRuntime::Description owner;
        bool known = DescribeAddressIn(state, faultAddress, owner);

        fprintf(stderr, "[guestruntime]   the runtime has that page as%s%s%s%s, and it belongs to %s\n",
                (flags & PAGE_COMMITTED) != 0 ? " committed" : " uncommitted", (flags & PAGE_READABLE) != 0 ? " readable" : "",
                (flags & PAGE_WRITABLE) != 0 ? " writable" : "", (flags & PAGE_EXECUTABLE) != 0 ? " executable" : "",
                known ? owner.soname.c_str() : "no loaded module");
    }

    GuestRuntime::Description caller;
    if (DescribeAddressIn(state, registers->general[30], caller))
    {
        fprintf(stderr, "[guestruntime]   x30 is %s+0x%llx\n", caller.soname.c_str(),
                (unsigned long long)(registers->general[30] - caller.moduleBase));
    }

    if (gBlockTraceCursor != 0)
    {
        uint64_t traced = gBlockTraceCursor < BLOCK_TRACE_REPORTED ? gBlockTraceCursor : BLOCK_TRACE_REPORTED;

        fprintf(stderr, "[guestruntime]   the last %llu guest blocks this thread ran, oldest first:\n", (unsigned long long)traced);

        for (uint64_t index = 0; index < traced; ++index)
        {
            uint64_t at = gBlockTrace[(gBlockTraceCursor - traced + index) % LlvmBackend::BLOCK_TRACE_ENTRIES];

            GuestRuntime::Description described;
            if (DescribeAddressIn(state, at, described))
            {
                fprintf(stderr, "[guestruntime]     %016llx  %s+0x%llx\n", (unsigned long long)at, described.soname.c_str(),
                        (unsigned long long)(at - described.moduleBase));
            }
            else
            {
                fprintf(stderr, "[guestruntime]     %016llx\n", (unsigned long long)at);
            }
        }
    }

    fprintf(stderr, "[guestruntime]   the runtime last entered these guest addresses, oldest first:\n");

    uint64_t seen = gRecentEntryCursor < RECENT_ENTRY_COUNT ? gRecentEntryCursor : RECENT_ENTRY_COUNT;
    for (uint64_t index = 0; index < seen; ++index)
    {
        uint64_t entry = gRecentEntries[(gRecentEntryCursor - seen + index) % RECENT_ENTRY_COUNT];

        GuestRuntime::Description described;
        if (DescribeAddressIn(state, entry, described))
        {
            fprintf(stderr, "[guestruntime]     %016llx  %s+0x%llx\n", (unsigned long long)entry, described.soname.c_str(),
                    (unsigned long long)(entry - described.moduleBase));
        }
        else
        {
            fprintf(stderr, "[guestruntime]     %016llx\n", (unsigned long long)entry);
        }
    }

    fprintf(stderr, "[guestruntime]   guest stack words that land in a loaded module:\n");

    int printed = 0;
    for (uint64_t offset = 0; offset < GUEST_FAULT_SCAN_BYTES && printed < GUEST_FAULT_SCAN_FRAMES; offset += sizeof(uint64_t))
    {
        uint64_t slot = 0;
        uint64_t at = registers->stackPointer + offset;

        if (!PagesAllow(state, at, sizeof(slot), PAGE_COMMITTED | PAGE_READABLE)) break;
        if (!MemoryRead(state, (GuestState::GuestAddress)at, &slot, sizeof(slot))) break;

        GuestRuntime::Description frame;
        if (!DescribeAddressIn(state, slot, frame)) continue;

        fprintf(stderr, "[guestruntime]     %016llx  %s+0x%llx\n", (unsigned long long)slot, frame.soname.c_str(),
                (unsigned long long)(slot - frame.moduleBase));
        printed += 1;
    }

    fflush(stderr);
}

static std::string DescribeGuestBacktrace(GuestRuntimeState* state, const GuestState::Registers& registers)
{
    std::string described;
    uint64_t frame = registers.general[29];

    for (int depth = 0; depth < GUEST_BACKTRACE_FRAMES; ++depth)
    {
        if (!PagesAllow(state, frame, 16, PAGE_COMMITTED | PAGE_READABLE)) break;

        uint64_t pair[2] = {0, 0};
        if (!MemoryRead(state, (GuestState::GuestAddress)frame, pair, sizeof(pair))) break;
        if (pair[1] == 0 || pair[1] == GuestRuntime::SENTINEL_RETURN_ADDRESS) break;

        described += "\n              ";
        described += DescribeGuestAddress(state, pair[1]);

        if (pair[0] <= frame) break;
        frame = pair[0];
    }

    return described;
}

static std::string DescribeGuestText(GuestRuntimeState* state, uint64_t address)
{
    if (address == 0) return std::string();

    char text[80];
    uint64_t length = 0;

    while (length + 1 < sizeof(text))
    {
        if (!PagesAllow(state, address + length, 1, PAGE_COMMITTED | PAGE_READABLE)) return std::string();

        char character = 0;
        if (!MemoryRead(state, (GuestState::GuestAddress)(address + length), &character, sizeof(character))) return std::string();
        if (character == '\0') break;
        if (character < 0x20 || character > 0x7e) return std::string();

        text[length] = character;
        length += 1;
    }

    if (length < 2) return std::string();

    text[length] = '\0';
    return std::string(" \"") + text + "\"";
}

static GuestRuntime::Result CallGuestIn(GuestRuntimeState* state, GuestState::GuestAddress entryAddress, GuestState::Registers& registers,
                                        const uint64_t* stackWords = nullptr, uint32_t stackWordCount = 0)
{
    if (!state) return GuestRuntime::Result::ArenaUnavailable;

    BorrowedStack borrowed;
    CallTokenScope token;
    ActiveRegisterScope active(state, registers);

    EnsureFaultStack();

    if (registers.stackPointer == 0)
    {
        GuestState::GuestAddress stackTop{};
        GuestRuntime::Result allocated = TakeStack(state, stackTop);
        if (allocated != GuestRuntime::Result::Ok) return allocated;

        borrowed.state = state;
        borrowed.top = stackTop;
        borrowed.held = true;

        registers.stackPointer = (uint64_t)stackTop;

        gGuestStackTop = (uint64_t)stackTop;
        gGuestStackBytes = state->configuration.stackBytes;
    }

    if (stackWordCount != 0)
    {
        uint64_t bytes = RoundUpTo((uint64_t)stackWordCount * sizeof(uint64_t), 16);
        registers.stackPointer -= bytes;

        for (uint32_t index = 0; index < stackWordCount; ++index)
            memcpy(HostAddress(state, registers.stackPointer + (uint64_t)index * sizeof(uint64_t)), &stackWords[index], sizeof(uint64_t));
    }

    if (registers.threadPointer == 0)
    {
        std::lock_guard<std::mutex> guard(state->threadMutex);
        if (state->mainThreadPointer == 0)
        {
            GuestRuntime::Instance instance;
            instance.implementation = state;

            GuestState::GuestAddress pointer{};
            GuestRuntime::Result allocated = GuestRuntime::AllocateThreadPointer(instance, pointer);
            instance.implementation = nullptr;

            if (allocated != GuestRuntime::Result::Ok) return allocated;
            state->mainThreadPointer = (uint64_t)pointer;
        }

        registers.threadPointer = state->mainThreadPointer;
    }

    registers.general[30] = GuestRuntime::SENTINEL_RETURN_ADDRESS;

    uint64_t cursor = (uint64_t)entryAddress;
    uint64_t remaining = state->configuration.stepLimit;

    Interpreter::Configuration interpreterConfiguration;
    interpreterConfiguration.stepLimit = Interpreter::UNLIMITED_STEPS;

    for (;;)
    {
        if (cursor == GuestRuntime::SENTINEL_RETURN_ADDRESS) return GuestRuntime::Result::Ok;

        if (AbiBridge::IsGuestStub(state->registry, (GuestState::GuestAddress)cursor))
        {
            Bump(state->statistics.hostCalls);

            std::string traced;
            if (state->traceHostCalls)
            {
                const AbiBridge::Trampoline* trampoline = AbiBridge::LookupTrampoline(state->registry, (GuestState::GuestAddress)cursor);

                char described[1024];
                snprintf(described, sizeof(described), "[hostcall %d] %s(%016llx%s, %016llx%s, %016llx%s) from %s",
                         (int)syscall(SYS_gettid), trampoline ? trampoline->symbolName.c_str() : "?",
                         (unsigned long long)registers.general[0], DescribeGuestText(state, registers.general[0]).c_str(),
                         (unsigned long long)registers.general[1], DescribeGuestText(state, registers.general[1]).c_str(),
                         (unsigned long long)registers.general[2], DescribeGuestText(state, registers.general[2]).c_str(),
                         DescribeGuestAddress(state, registers.general[30]).c_str());

                traced = described;
                if (state->traceHostCallStacks) traced += DescribeGuestBacktrace(state, registers);
            }

            AbiBridge::Result called =
                AbiBridge::CallHostFromGuest(state->registry, (GuestState::GuestAddress)cursor, registers, state->memory);

            if (state->traceHostCalls)
            {
                fprintf(stderr, "%s = %016llx\n", traced.c_str(), (unsigned long long)registers.general[0]);
                fflush(stderr);
            }

            if (called != AbiBridge::Result::Ok)
            {
                const AbiBridge::Trampoline* trampoline = AbiBridge::LookupTrampoline(state->registry, (GuestState::GuestAddress)cursor);
                fprintf(stderr, "[guestruntime] host call to %s failed: %s\n", trampoline ? trampoline->symbolName.c_str() : "?",
                        AbiBridge::ResultName(called));
                return GuestRuntime::Result::HostCallFailed;
            }

            cursor = registers.general[30];
            continue;
        }

        gRecentEntries[gRecentEntryCursor % RECENT_ENTRY_COUNT] = cursor;
        gRecentEntryCursor += 1;

        Interpreter::ExitStatus status;
        TranslatedBlock translated;

        {
            std::lock_guard<std::mutex> guard(state->translatedMutex);
            auto found = state->translatedBlocks.find(cursor);
            if (found != state->translatedBlocks.end()) translated = found->second;
        }

        const Ir::Block* block = nullptr;

        if (translated.hostEntry)
        {
            LlvmBackend::RuntimeContext runtime;
            runtime.memory = state->memory;
            AttachBlockTrace(runtime);

            memset(&status, 0, sizeof(status));
            ((LlvmBackend::EntryPoint)translated.hostEntry)(&registers, &runtime, &status, translated.entryBlock);

            Bump(state->statistics.blocksExecutedTranslated);
            if (status.reason == Interpreter::ExitReason::Undefined) Bump(state->statistics.trapsHit);
        }
        else
        {
            if (state->configuration.requireTranslated)
            {
                fprintf(stderr,
                        "[guestruntime] %016llx (%s) was reached with no translated entry, and everything is meant to be translated "
                        "ahead of time, so this run cannot continue; it is a hole in ahead-of-time discovery, not something to "
                        "interpret around\n",
                        (unsigned long long)cursor, DescribeGuestAddress(state, cursor).c_str());
                return GuestRuntime::Result::GuestFaulted;
            }

            GuestRuntime::Result failure = GuestRuntime::Result::Ok;
            block = BlockAt(state, cursor, failure);
            if (!block)
            {
                fprintf(stderr,
                        "[guestruntime %d] the guest branched to %016llx, which holds no code; it would return to %016llx (%s), and this "
                        "call entered the guest at %016llx (%s)\n",
                        (int)syscall(SYS_gettid), (unsigned long long)cursor, (unsigned long long)registers.general[30],
                        DescribeGuestAddress(state, registers.general[30]).c_str(), (unsigned long long)entryAddress,
                        DescribeGuestAddress(state, (uint64_t)entryAddress).c_str());
                return failure;
            }

            NativeEntry native = state->configuration.recompile ? NativeEntryFor(state, cursor) : NativeEntry{};

            if (native.hostEntry)
            {
                LlvmBackend::RuntimeContext runtime;
                runtime.memory = state->memory;
                AttachBlockTrace(runtime);

                memset(&status, 0, sizeof(status));

                const uint64_t enclosingEntry = gRecompiledEntry;
                gRecompiledEntry = native.regionAddress;
                ((LlvmBackend::EntryPoint)native.hostEntry)(&registers, &runtime, &status, native.entryBlock);
                gRecompiledEntry = enclosingEntry;

                Bump(state->statistics.blocksExecutedNative);
                if (status.reason == Interpreter::ExitReason::Undefined) Bump(state->statistics.trapsHit);
            }
            else
            {
                status = Interpreter::RunBlock(*block, registers, state->memory, interpreterConfiguration);
                Bump(state->statistics.blocksInterpreted);
                NoteInterpretedBlock(cursor);
            }
        }

        Bump(state->statistics.blocksExecuted);

        if (state->configuration.stepLimit != 0)
        {
            if (status.stepsExecuted > remaining) return GuestRuntime::Result::StepLimitReached;
            remaining -= status.stepsExecuted;
        }

        if (state->configuration.traceGuest)
        {
            fprintf(stderr, "[guest] %016llx -> %s %016llx\n", (unsigned long long)cursor, Interpreter::ExitReasonName(status.reason),
                    (unsigned long long)status.targetAddress);
        }

        if (status.reason == Interpreter::ExitReason::HostCall)
        {
            Bump(state->statistics.hostCalls);

            AbiBridge::Result called = AbiBridge::CallHostFromGuest(state->registry, status.targetAddress, registers, state->memory);
            if (called != AbiBridge::Result::Ok)
            {
                const AbiBridge::Trampoline* trampoline = AbiBridge::LookupTrampoline(state->registry, status.targetAddress);
                fprintf(stderr, "[guestruntime] host call to %s failed: %s\n", trampoline ? trampoline->symbolName.c_str() : "?",
                        AbiBridge::ResultName(called));
                return GuestRuntime::Result::HostCallFailed;
            }

            cursor = registers.general[30];
            continue;
        }

        if (status.reason == Interpreter::ExitReason::Syscall)
        {
            Bump(state->statistics.syscalls);

            GuestSyscall::Result serviced = GuestSyscall::Dispatch(registers, state->memory, state->syscallStatistics);
            if (serviced == GuestSyscall::Result::GuestMemoryFault) return GuestRuntime::Result::GuestFaulted;
            if (serviced == GuestSyscall::Result::ProcessExit) return GuestRuntime::Result::Ok;

            if (state->configuration.traceGuest || serviced == GuestSyscall::Result::Unimplemented)
            {
                fprintf(stderr, "[syscall] %s (%llu) -> %s\n", GuestSyscall::SyscallName((uint32_t)registers.general[8]),
                        (unsigned long long)registers.general[8], GuestSyscall::ResultName(serviced));
            }

            cursor = (uint64_t)status.targetAddress + 4;
            continue;
        }

        if (status.reason == Interpreter::ExitReason::InstructionCacheInvalidated)
        {
            uint64_t line = RoundDownTo((uint64_t)status.faultAddress, Ir::INSTRUCTION_CACHE_LINE_BYTES);
            InvalidateRange(state, line, line + Ir::INSTRUCTION_CACHE_LINE_BYTES);

            cursor = (uint64_t)status.targetAddress + Arm64Decoder::INSTRUCTION_SIZE_BYTES;
            continue;
        }

        if (status.reason == Interpreter::ExitReason::BlockExhausted)
        {
            if (status.hasTargetAddress) cursor = (uint64_t)status.targetAddress;
            else if (block) cursor = (uint64_t)block->guestEndAddress;
            else return GuestRuntime::Result::GuestFaulted;

            continue;
        }

        if (status.reason == Interpreter::ExitReason::IndirectBranch || status.reason == Interpreter::ExitReason::Returned)
        {
            if (!status.hasTargetAddress) return GuestRuntime::Result::GuestFaulted;
            cursor = (uint64_t)status.targetAddress;
            continue;
        }

        uint32_t encoding = 0;
        bool readable = status.hasFaultAddress &&
                        state->memory.Read(state->memory.context, status.faultAddress, &encoding, sizeof(encoding));

        if (readable)
        {
            fprintf(stderr, "[guestruntime] guest exited at %016llx with %s (fault %016llx, encoding %08x)\n",
                    (unsigned long long)cursor, Interpreter::ExitReasonName(status.reason),
                    (unsigned long long)status.faultAddress, encoding);
        }
        else
        {
            fprintf(stderr, "[guestruntime] guest exited at %016llx with %s (fault %016llx)\n", (unsigned long long)cursor,
                    Interpreter::ExitReasonName(status.reason), (unsigned long long)status.faultAddress);
        }

        return GuestRuntime::Result::GuestFaulted;
    }
}

static bool EnterGuestCallback(void* context, GuestState::GuestAddress guestAddress, GuestState::Registers& registers)
{
    return CallGuestIn((GuestRuntimeState*)context, guestAddress, registers) == GuestRuntime::Result::Ok;
}

static bool EnterGuestWithStackCallback(void* context, GuestState::GuestAddress guestAddress, GuestState::Registers& registers,
                                        const uint64_t* stackWords, uint32_t stackWordCount)
{
    return CallGuestIn((GuestRuntimeState*)context, guestAddress, registers, stackWords, stackWordCount) == GuestRuntime::Result::Ok;
}

GuestRuntime::Result GuestRuntime::Create(const Configuration& configuration, Instance& instance)
{
    GuestRuntimeState* state = new GuestRuntimeState();
    state->configuration = configuration;
    state->pageBytes = Platform::HostPageBytes();

    uint64_t length = RoundUpTo(configuration.arenaLength, state->pageBytes);

    void* mapped = mmap((void*)configuration.arenaBase, (size_t)length, PROT_NONE,
                        MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE | MAP_FIXED_NOREPLACE, -1, 0);
    if (mapped == MAP_FAILED)
    {
        delete state;
        return Result::ArenaUnavailable;
    }

    if ((uint64_t)mapped != configuration.arenaBase)
    {
        munmap(mapped, (size_t)length);
        delete state;
        return Result::ArenaUnavailable;
    }

    state->arena = (uint8_t*)mapped;
    state->arenaBase = configuration.arenaBase;
    InstallFaultReporter(configuration.arenaBase, length);
    GuestSync::SetAddressDescriber(&DescribeAddressForSync, state);
    StartBlockReporter(state);
    state->arenaLength = length;
    state->arenaCursor = configuration.arenaBase + GuestRuntime::IMAGE_REGION_LENGTH;
    state->imageRegionBase = configuration.arenaBase;
    state->scratchCursor = configuration.arenaBase + length;
    state->traceHostCalls = Settings::Tracing("hostcalls");
    state->traceHostCallStacks = Settings::Tracing("hostcall-stacks");
    state->pageFlags.assign((size_t)(length / state->pageBytes), 0);

    state->memory.Read = &MemoryRead;
    state->memory.Write = &MemoryWrite;
    state->memory.TranslateForAtomic = &MemoryTranslateForAtomic;
    state->memory.context = state;
    state->decoder.memory = state->memory;

    instance.implementation = state;

    GuestState::GuestAddress stubBase{};
    Result reserved = ReserveIn(state, configuration.stubRegionLength, Platform::GUEST_MAX_PAGE_BYTES, stubBase);
    if (reserved != Result::Ok)
    {
        Destroy(instance);
        return reserved;
    }

    Result committed = CommitIn(state, (uint64_t)stubBase, configuration.stubRegionLength, true, true, false);
    if (committed != Result::Ok)
    {
        Destroy(instance);
        return committed;
    }

    state->stubBase = (uint64_t)stubBase;
    state->stubLength = configuration.stubRegionLength;

    for (uint64_t offset = 0; offset + sizeof(BRK_ENCODING) <= state->stubLength; offset += sizeof(BRK_ENCODING))
        memcpy(HostAddress(state, state->stubBase + offset), &BRK_ENCODING, sizeof(BRK_ENCODING));

    ProtectIn(state, state->stubBase, state->stubLength, true, false, false);

    AbiBridge::RegistryConfiguration registryConfiguration;
    registryConfiguration.stubRegionBase = stubBase;
    registryConfiguration.stubRegionLength = configuration.stubRegionLength;

    if (AbiBridge::CreateRegistry(registryConfiguration, state->registry) != AbiBridge::Result::Ok)
    {
        Destroy(instance);
        return Result::ArenaUnavailable;
    }

    ElfLoader::Configuration loaderConfiguration;
    loaderConfiguration.searchPaths = configuration.searchPaths;
    loaderConfiguration.memory = state->memory;
    loaderConfiguration.hostResolver.ResolveToGuestStub = &ResolveToGuestStubCallback;
    loaderConfiguration.hostResolver.LookupDefinedStub = &LookupDefinedStubCallback;
    loaderConfiguration.hostResolver.ResolveDataObject = &ResolveDataObjectCallback;
    loaderConfiguration.hostResolver.IsProxyLibrary = &IsProxyLibraryCallback;
    loaderConfiguration.hostResolver.context = state;
    loaderConfiguration.addressSpace.Reserve = &ReserveCallback;
    loaderConfiguration.addressSpace.ReserveForImage = &ReserveForImageCallback;
    loaderConfiguration.addressSpace.Commit = &CommitCallback;
    loaderConfiguration.addressSpace.Protect = &ProtectCallback;
    loaderConfiguration.addressSpace.Release = &ReleaseCallback;
    loaderConfiguration.addressSpace.context = state;

    if (ElfLoader::CreateNamespace(loaderConfiguration, state->libraryNamespace) != ElfLoader::Result::Ok)
    {
        Destroy(instance);
        return Result::ArenaUnavailable;
    }

    Result proxies = RegisterPlatformProxies(instance);
    if (proxies != Result::Ok)
    {
        Destroy(instance);
        return proxies;
    }

    if (configuration.recompile)
    {
        LlvmBackend::Configuration backendConfiguration;
        backendConfiguration.tier = configuration.tier;
        backendConfiguration.traceBlocks = configuration.traceBlocks;

        if (LlvmBackend::Initialize(backendConfiguration, state->backend) != LlvmBackend::Result::Ok)
        {
            Destroy(instance);
            return Result::ArenaUnavailable;
        }

        state->backendReady = true;

        if (configuration.recompileInBackground) state->compiler = std::thread(&RunBackgroundCompiler, state);
    }

    return Result::Ok;
}

void GuestRuntime::Destroy(Instance& instance)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return;

    uint64_t running = 0;
    for (uint64_t waited = 0;; waited += SHUTDOWN_POLL_MILLISECONDS)
    {
        running = 0;
        for (GuestThread& record : state->threads)
        {
            if (record.worker.joinable() && !record.finished.load(std::memory_order_acquire)) running += 1;
        }

        if (running == 0 || waited >= SHUTDOWN_GRACE_MILLISECONDS) break;
        std::this_thread::sleep_for(std::chrono::milliseconds(SHUTDOWN_POLL_MILLISECONDS));
    }

    if (running != 0)
    {
        fprintf(stderr,
                "[guestruntime] %llu guest thread(s) were still running %llu ms after shutdown began, so the arena stays mapped and "
                "the runtime is abandoned rather than pulled out from under them\n",
                (unsigned long long)running, (unsigned long long)SHUTDOWN_GRACE_MILLISECONDS);

        for (GuestThread& record : state->threads)
        {
            if (record.worker.joinable()) record.worker.detach();
        }

        instance.implementation = nullptr;
        return;
    }

    for (GuestThread& record : state->threads)
    {
        if (record.worker.joinable()) record.worker.join();
    }

    ElfLoader::DestroyNamespace(state->libraryNamespace);
    AbiBridge::DestroyRegistry(state->registry);

    if (state->backendReady)
    {
        StopBackgroundCompiler(state);

        for (auto& region : state->recompiledRegions) LlvmBackend::ReleaseFunction(state->backend, region.second.compiled);
        for (LlvmBackend::CompiledFunction& compiled : state->retiredCode) LlvmBackend::ReleaseFunction(state->backend, compiled);

        state->recompiledRegions.clear();
        state->retiredCode.clear();
        state->nativeEntries.clear();

        LlvmBackend::Shutdown(state->backend);
    }

    if (state->arena) munmap(state->arena, (size_t)state->arenaLength);

    delete state;
    instance.implementation = nullptr;
}

ElfLoader::Namespace& GuestRuntime::LibraryNamespace(Instance& instance)
{
    return Implementation(instance)->libraryNamespace;
}

AbiBridge::Registry& GuestRuntime::BridgeRegistry(Instance& instance)
{
    return Implementation(instance)->registry;
}

const GuestState::MemoryAccessor& GuestRuntime::Memory(const Instance& instance)
{
    return Implementation(instance)->memory;
}

GuestState::GuestCaller GuestRuntime::Caller(Instance& instance)
{
    GuestState::GuestCaller caller;
    caller.EnterGuest = &EnterGuestCallback;
    caller.EnterGuestWithStack = &EnterGuestWithStackCallback;
    caller.context = instance.implementation;
    return caller;
}

const GuestRuntime::Statistics& GuestRuntime::GetStatistics(const Instance& instance)
{
    return Implementation(instance)->statistics;
}

GuestRuntime::Result GuestRuntime::Reserve(Instance& instance, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base)
{
    return ReserveIn(Implementation(instance), length, alignment, base);
}

GuestRuntime::Result GuestRuntime::ReserveScratch(Instance& instance, uint64_t length, uint64_t alignment, GuestState::GuestAddress& base)
{
    return ReserveScratchIn(Implementation(instance), length, alignment, base);
}

GuestRuntime::Result GuestRuntime::Commit(Instance& instance, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable,
                                          bool executable)
{
    return CommitIn(Implementation(instance), (uint64_t)address, length, readable, writable, executable);
}

GuestRuntime::Result GuestRuntime::Protect(Instance& instance, GuestState::GuestAddress address, uint64_t length, bool readable, bool writable,
                                           bool executable)
{
    return ProtectIn(Implementation(instance), (uint64_t)address, length, readable, writable, executable);
}

void* GuestRuntime::HostPointer(Instance& instance, GuestState::GuestAddress address, uint64_t length)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return nullptr;
    if (length == 0) return nullptr;
    if (!PagesAllow(state, (uint64_t)address, length, PAGE_COMMITTED | PAGE_READABLE | PAGE_WRITABLE)) return nullptr;

    return HostAddress(state, (uint64_t)address);
}

void GuestRuntime::ArenaBounds(const Instance& instance, GuestState::GuestAddress& base, uint64_t& length)
{
    const GuestRuntimeState* state = Implementation(instance);
    if (!state)
    {
        base = GuestState::GuestAddress{};
        length = 0;
        return;
    }

    base = (GuestState::GuestAddress)state->arenaBase;
    length = state->arenaLength;
}

GuestRuntime::Result GuestRuntime::RegisterDataObject(Instance& instance, const std::string& symbolName, uint64_t sizeInBytes,
                                                      GuestState::GuestAddress& address)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;
    if (sizeInBytes == 0) return Result::OutOfGuestMemory;

    GuestState::GuestAddress base{};
    Result reserved = ReserveScratchIn(state, sizeInBytes, sizeof(uint64_t), base);
    if (reserved != Result::Ok) return reserved;

    Result committed = Commit(instance, base, sizeInBytes, true, true, false);
    if (committed != Result::Ok) return committed;

    if (AbiBridge::RegisterHostData(state->registry, symbolName, base, sizeInBytes) != AbiBridge::Result::Ok)
        return Result::OutOfGuestMemory;

    address = base;
    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::RegisterTranslatedBlock(Instance& instance, GuestState::GuestAddress blockAddress, void* hostEntry,
                                                           uint32_t entryBlock)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;
    if (!hostEntry) return Result::DecodeFailed;

    TranslatedBlock translated;
    translated.hostEntry = hostEntry;
    translated.entryBlock = entryBlock;

    std::lock_guard<std::mutex> guard(state->translatedMutex);
    state->translatedBlocks[(uint64_t)blockAddress] = translated;

    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::RegisterPlatformProxies(Instance& instance)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    static const char* const PLATFORM_SONAMES[] = {
        "libc.so",         "libm.so",           "libdl.so",       "liblog.so",         "libandroid.so",
        "libstdc++.so",    "libz.so",           "libEGL.so",      "libGLESv1_CM.so",   "libGLESv2.so",
        "libGLESv3.so",    "libvulkan.so",      "libjnigraphics.so", "libOpenSLES.so", "libaaudio.so",
        "libmediandk.so",  "libnativewindow.so", "libcamera2ndk.so", "libneuralnetworks.so", "libsync.so"};

    for (const char* soname : PLATFORM_SONAMES)
    {
        AbiBridge::Result registered = AbiBridge::RegisterProxyLibrary(state->registry, soname);
        if (registered != AbiBridge::Result::Ok && registered != AbiBridge::Result::AlreadyRegistered) return Result::ArenaUnavailable;
    }

    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::Load(Instance& instance, const std::string& sonameOrPath, uint32_t& moduleId)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    ElfLoader::Result result;
    {
        std::lock_guard<std::mutex> guard(state->libraryMutex);
        result = ElfLoader::LoadLibrary(state->libraryNamespace, sonameOrPath, moduleId);
    }

    if (result != ElfLoader::Result::Ok)
    {
        fprintf(stderr, "[guestruntime] cannot load %s: %s\n", sonameOrPath.c_str(), ElfLoader::ResultName(result));
        return Result::LoadFailed;
    }

    return Result::Ok;
}

void GuestRuntime::DescribeModules(Instance& instance, std::vector<Module>& modules)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return;

    std::lock_guard<std::mutex> guard(state->libraryMutex);

    uint64_t loaded = state->libraryNamespace.loaded.size();
    for (const ElfLoader::Library& library : state->libraryNamespace.loaded)
    {
        Module described;
        described.path = library.path;
        described.loadBias = library.loadBias;
        described.programHeaderAddress = (uint64_t)library.programHeaderAddress;
        described.programHeaderCount = library.programHeaderCount;
        described.threadLocalModuleId = library.threadLocalStorage.present ? library.threadLocalStorage.moduleId : 0;
        described.loadEventCount = loaded;

        modules.push_back(described);
    }
}

GuestRuntime::Result GuestRuntime::DescribeAddress(Instance& instance, uint64_t address, Description& description)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    description = Description();

    std::lock_guard<std::mutex> guard(state->libraryMutex);
    return DescribeAddressIn(state, address, description) ? Result::Ok : Result::NoSuchSymbol;
}

GuestRuntime::Result GuestRuntime::RunInitializers(Instance& instance, uint32_t moduleId)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    GuestState::GuestCaller caller = Caller(instance);
    if (ElfLoader::RunInitializers(state->libraryNamespace, moduleId, caller) != ElfLoader::Result::Ok) return Result::GuestFaulted;

    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::Lookup(Instance& instance, const std::string& symbolName, GuestState::GuestAddress& address)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    if (ElfLoader::LookupExport(state->libraryNamespace, symbolName, address) != ElfLoader::Result::Ok) return Result::NoSuchSymbol;

    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::CallGuest(Instance& instance, GuestState::GuestAddress entryAddress, GuestState::Registers& registers)
{
    return CallGuestIn(Implementation(instance), entryAddress, registers);
}

GuestRuntime::Result GuestRuntime::CallGuestNested(Instance& instance, const GuestState::Registers& caller,
                                                   GuestState::GuestAddress entryAddress, const uint64_t* arguments,
                                                   uint32_t argumentCount, uint64_t& returned)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    GuestState::Registers nested;
    GuestState::Reset(nested);

    uint32_t supplied = argumentCount;
    if (supplied > (uint32_t)GuestState::GENERAL_REGISTER_COUNT) supplied = (uint32_t)GuestState::GENERAL_REGISTER_COUNT;
    for (uint32_t index = 0; index < supplied; ++index) nested.general[index] = arguments[index];

    nested.stackPointer = caller.stackPointer;
    nested.threadPointer = caller.threadPointer;
    nested.floatingPointControl = caller.floatingPointControl;

    Result called = CallGuestIn(state, entryAddress, nested);
    returned = nested.general[0];
    return called;
}

bool GuestRuntime::OnGuestThread()
{
    return gActiveState != nullptr && gActiveRegisters != nullptr;
}

bool GuestRuntime::CurrentGuestStack(uint64_t& base, uint64_t& bytes)
{
    if (gGuestStackTop == 0 || gGuestStackBytes == 0) return false;

    bytes = gGuestStackBytes;
    base = gGuestStackTop > bytes ? gGuestStackTop - bytes : 0;
    return true;
}

GuestRuntime::Result GuestRuntime::CallGuestOnCurrentThread(GuestState::GuestAddress entryAddress, const uint64_t* arguments,
                                                            uint32_t argumentCount, uint64_t& returned)
{
    GuestRuntimeState* state = gActiveState;
    GuestState::Registers* interrupted = gActiveRegisters;
    if (!state || !interrupted) return Result::ArenaUnavailable;

    GuestState::Registers nested;
    GuestState::Reset(nested);

    uint32_t supplied = argumentCount;
    if (supplied > (uint32_t)GuestState::GENERAL_REGISTER_COUNT) supplied = (uint32_t)GuestState::GENERAL_REGISTER_COUNT;
    for (uint32_t index = 0; index < supplied; ++index) nested.general[index] = arguments[index];

    nested.stackPointer = (interrupted->stackPointer - SIGNAL_FRAME_GAP_BYTES) & ~(uint64_t)15;
    nested.threadPointer = interrupted->threadPointer;
    nested.floatingPointControl = interrupted->floatingPointControl;

    Result called = CallGuestIn(state, entryAddress, nested);
    returned = nested.general[0];
    return called;
}

GuestRuntime::Result GuestRuntime::AllocateStack(Instance& instance, uint64_t bytes, GuestState::GuestAddress& stackTop)
{
    return AllocateStackIn(Implementation(instance), bytes, stackTop);
}

GuestRuntime::Result GuestRuntime::AllocateThreadPointer(Instance& instance, GuestState::GuestAddress& threadPointer)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    uint64_t needed = state->libraryNamespace.staticTlsBytes;
    uint64_t bytes = RoundUpTo(needed < Platform::GUEST_MAX_PAGE_BYTES ? Platform::GUEST_MAX_PAGE_BYTES : needed, state->pageBytes);

    GuestState::GuestAddress base{};
    Result reserved = ReserveIn(state, bytes, Platform::GUEST_MAX_PAGE_BYTES, base);
    if (reserved != Result::Ok) return reserved;

    Result committed = CommitIn(state, (uint64_t)base, bytes, true, true, false);
    if (committed != Result::Ok) return committed;

    std::vector<uint8_t> zeros((size_t)bytes, 0);
    if (!state->memory.Write(state->memory.context, base, zeros.data(), bytes)) return Result::OutOfGuestMemory;

    for (const ElfLoader::Library& library : state->libraryNamespace.loaded)
    {
        const ElfLoader::ThreadLocalStorage& storage = library.threadLocalStorage;
        if (!storage.present || storage.imageSize == 0) continue;

        std::vector<uint8_t> image((size_t)storage.imageSize);
        if (!state->memory.Read(state->memory.context, storage.imageAddress, image.data(), storage.imageSize))
            return Result::OutOfGuestMemory;

        uint64_t destination = (uint64_t)base + storage.staticOffsetFromThreadPointer;
        if (!state->memory.Write(state->memory.context, (GuestState::GuestAddress)destination, image.data(), storage.imageSize))
            return Result::OutOfGuestMemory;
    }

    threadPointer = base;

    fprintf(stderr, "[guestruntime %d] thread pointer %016llx covers %llu bytes\n", (int)syscall(SYS_gettid),
            (unsigned long long)base, (unsigned long long)bytes);

    return Result::Ok;
}

void GuestRuntime::SetThreadExitHandler(Instance& instance, ThreadExitHandler handler, void* context)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return;

    state->threadExitHandler = handler;
    state->threadExitContext = context;
}

GuestRuntime::Result GuestRuntime::StartThread(Instance& instance, GuestState::GuestAddress entryAddress, uint64_t argument, uint64_t& handle)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    GuestState::GuestAddress stackTop{};
    Result stackReady = AllocateStackIn(state, state->configuration.stackBytes, stackTop);

    if (stackReady != Result::Ok)
    {
        fprintf(stderr, "[guestruntime] a guest thread needs %llu stack bytes, which the arena refused: %s\n",
                (unsigned long long)state->configuration.stackBytes, ResultName(stackReady));
        return stackReady;
    }

    GuestState::GuestAddress threadPointer{};
    Result pointerReady = AllocateThreadPointer(instance, threadPointer);

    if (pointerReady != Result::Ok)
    {
        fprintf(stderr, "[guestruntime] a guest thread needs a thread pointer, which the arena refused: %s\n",
                ResultName(pointerReady));
        return pointerReady;
    }

    std::lock_guard<std::mutex> guard(state->threadMutex);

    state->threads.emplace_back();
    GuestThread& record = state->threads.back();
    handle = state->threads.size();

    GuestThread* target = &record;
    uint64_t entry = (uint64_t)entryAddress;
    uint64_t stack = (uint64_t)stackTop;
    uint64_t pointer = (uint64_t)threadPointer;

    uint64_t stackBytes = state->configuration.stackBytes;

    target->worker = std::thread([state, target, entry, stack, pointer, argument, stackBytes]() {
        GuestState::Registers registers;
        GuestState::Reset(registers);
        registers.general[0] = argument;
        registers.stackPointer = stack;
        registers.threadPointer = pointer;

        gGuestStackTop = stack;
        gGuestStackBytes = stackBytes;

        GuestRuntime::Result result = CallGuestIn(state, (GuestState::GuestAddress)entry, registers);
        target->faulted = result != GuestRuntime::Result::Ok;
        target->returnValue = registers.general[0];

        fprintf(stderr, "[guestruntime] the guest thread that started at %016llx ended with %s, returning %016llx\n",
                (unsigned long long)entry, GuestRuntime::ResultName(result), (unsigned long long)registers.general[0]);

        if (state->threadExitHandler) state->threadExitHandler(state->threadExitContext);

        target->finished.store(true, std::memory_order_release);
    });

    Bump(state->statistics.threadsStarted);
    return Result::Ok;
}

GuestRuntime::Result GuestRuntime::JoinThread(Instance& instance, uint64_t handle, uint64_t& returnValue)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return Result::ArenaUnavailable;

    GuestThread* record = nullptr;

    {
        std::lock_guard<std::mutex> guard(state->threadMutex);
        if (handle == 0 || handle > state->threads.size()) return Result::ThreadCreationFailed;

        record = &state->threads[(size_t)handle - 1];
        if (record->joined) return Result::ThreadCreationFailed;
        record->joined = true;
    }

    if (record->worker.joinable()) record->worker.join();

    returnValue = record->returnValue;
    return record->faulted ? Result::GuestFaulted : Result::Ok;
}

static uint64_t InvalidateRange(GuestRuntimeState* state, uint64_t start, uint64_t end)
{
    std::lock_guard<std::mutex> guard(state->blockMutex);
    uint64_t dropped = 0;

    if (end <= state->codeLowAddress || start >= state->codeHighAddress) return dropped;

    state->invalidationEpoch += 1;

    for (auto entry = state->blocks.begin(); entry != state->blocks.end();)
    {
        const Ir::Function& function = entry->second;
        uint64_t blockStart = (uint64_t)function.entryAddress;
        uint64_t blockEnd = function.blocks.empty() ? blockStart + 4 : (uint64_t)function.blocks.front().guestEndAddress;

        if (blockEnd <= start || end <= blockStart)
        {
            ++entry;
            continue;
        }

        state->blockHits.erase(entry->first);
        state->unrecompilable.erase(entry->first);
        state->retired.push_back(std::move(entry->second));
        entry = state->blocks.erase(entry);
        dropped += 1;
    }

    for (auto region = state->recompiledRegions.begin(); region != state->recompiledRegions.end();)
    {
        const RecompiledRegion& record = region->second;

        if (record.endAddress <= start || end <= record.startAddress)
        {
            ++region;
            continue;
        }

        for (uint64_t blockAddress : record.blockAddresses)
        {
            auto native = state->nativeEntries.find(blockAddress);
            if (native != state->nativeEntries.end() && native->second.regionAddress == region->first) state->nativeEntries.erase(native);
        }

        state->retiredCode.push_back(record.compiled);
        region = state->recompiledRegions.erase(region);
        dropped += 1;
    }

    for (auto refused = state->unrecompilable.begin(); refused != state->unrecompilable.end();)
    {
        if (*refused >= start && *refused < end) refused = state->unrecompilable.erase(refused);
        else ++refused;
    }

    return dropped;
}

void GuestRuntime::InvalidateBlocks(Instance& instance, GuestState::GuestAddress address, uint64_t length)
{
    GuestRuntimeState* state = Implementation(instance);
    if (!state) return;

    InvalidateRange(state, (uint64_t)address, (uint64_t)address + length);
}

const char* GuestRuntime::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::ArenaUnavailable: return "ArenaUnavailable";
        case Result::OutOfGuestMemory: return "OutOfGuestMemory";
        case Result::LoadFailed: return "LoadFailed";
        case Result::DecodeFailed: return "DecodeFailed";
        case Result::GuestFaulted: return "GuestFaulted";
        case Result::HostCallFailed: return "HostCallFailed";
        case Result::StepLimitReached: return "StepLimitReached";
        case Result::NoSuchSymbol: return "NoSuchSymbol";
        case Result::ThreadCreationFailed: return "ThreadCreationFailed";
    }

    return "Unknown";
}

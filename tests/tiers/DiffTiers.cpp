#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "Interpreter/Interpreter.hpp"
#include "LlvmBackend/LlvmBackend.hpp"

#include "VectorCases.hpp"

static constexpr int MINIMUM_COMPILED_CASES = 24;
static constexpr int MINIMUM_LOWERING_CASES = 12;

struct LoweringCase
{
    const char* name;
    uint32_t code[6];
    int codeWords;
    uint64_t general[4];
};

static const LoweringCase LOWERING_CASES[] = {
    {"w_register_write_zeroes_upper", {0x92800002u, 0x0B020043u, 0xAA0303E0u}, 3, {0, 0, 0, 0}},
    {"w_add_overflows_32_bits", {0x0B010000u, 0xAA0003E0u}, 2, {0xFFFFFFFFull, 0xFFFFFFFFull, 0, 0}},
    {"w_sub_borrows_into_upper", {0x4B010000u, 0xAA0003E0u}, 2, {0, 1, 0, 0}},
    {"w_multiply_truncates", {0x1B017C00u, 0xAA0003E0u}, 2, {0x1234567890ABull, 0xFFFFFFFFull, 0, 0}},
    {"w_logical_keeps_upper_clear", {0x0A010000u, 0xAA0003E0u}, 2, {~0ull, ~0ull, 0, 0}},
    {"w_shift_left_past_32", {0x1AC12000u, 0xAA0003E0u}, 2, {0xFFFFFFFFull, 24, 0, 0}},
    {"w_shift_right_logical", {0x1AC12400u, 0xAA0003E0u}, 2, {0xFFFFFFFF80000000ull, 8, 0, 0}},
    {"w_shift_right_arithmetic", {0x1AC12800u, 0xAA0003E0u}, 2, {0x80000000ull, 8, 0, 0}},
    {"w_rotate_right", {0x1AC12C00u, 0xAA0003E0u}, 2, {0xF000000Full, 4, 0, 0}},
    {"x_shift_by_more_than_width", {0x9AC12000u, 0xAA0003E0u}, 2, {0xDEADBEEFull, 70, 0, 0}},
    {"x_divide_by_zero_is_zero", {0x9AC10C00u, 0xAA0003E0u}, 2, {0x1234ull, 0, 0, 0}},
    {"x_signed_divide_minus_one", {0x9AC10800u, 0xAA0003E0u}, 2, {0x8000000000000000ull, ~0ull, 0, 0}},
    {"w_divide_by_zero_is_zero", {0x1AC10C00u, 0xAA0003E0u}, 2, {0x1234ull, 0, 0, 0}},
    {"w_count_leading_zeros", {0x5AC01000u, 0xAA0003E0u}, 2, {0x00008000ull, 0, 0, 0}},
    {"x_count_leading_zeros_of_zero", {0xDAC01000u, 0xAA0003E0u}, 2, {0, 0, 0, 0}},
    {"w_reverse_bytes", {0x5AC00800u, 0xAA0003E0u}, 2, {0xDEADBEEFull, 0, 0, 0}},
    {"x_reverse_bits", {0xDAC00000u, 0xAA0003E0u}, 2, {0x0123456789ABCDEFull, 0, 0, 0}},
    {"w_adds_sets_flags_then_adc", {0x2B010000u, 0x1A0103E2u, 0xAA0203E0u}, 3, {0xFFFFFFFFull, 1, 0, 0}},
    {"x_subs_then_conditional_select", {0xEB010000u, 0x9A819002u, 0xAA0203E0u}, 3, {5, 9, 0, 0}},
    {"w_ands_clears_carry", {0x6A010000u, 0x1A9F37E2u, 0xAA0203E0u}, 3, {0xF0F0F0F0ull, 0x0F0F0F0Full, 0, 0}},
};

static constexpr int LOWERING_CASE_COUNT = (int)(sizeof(LOWERING_CASES) / sizeof(LOWERING_CASES[0]));

struct VectorMemory
{
    uint32_t code[VECTOR_MAX_CODE_WORDS];
    uint64_t scratch[VECTOR_SCRATCH_WORDS];
};

static bool MemoryRead(void* context, GuestState::GuestAddress address, void* destination, uint64_t length)
{
    VectorMemory* memory = (VectorMemory*)context;
    uint64_t raw = (uint64_t)address;

    if (raw >= VECTOR_CODE_BASE && raw + length <= VECTOR_CODE_BASE + sizeof(memory->code))
    {
        memcpy(destination, (const uint8_t*)memory->code + (raw - VECTOR_CODE_BASE), length);
        return true;
    }

    if (raw >= VECTOR_SCRATCH_BASE && raw + length <= VECTOR_SCRATCH_BASE + sizeof(memory->scratch))
    {
        memcpy(destination, (const uint8_t*)memory->scratch + (raw - VECTOR_SCRATCH_BASE), length);
        return true;
    }

    return false;
}

static bool MemoryWrite(void* context, GuestState::GuestAddress address, const void* source, uint64_t length)
{
    VectorMemory* memory = (VectorMemory*)context;
    uint64_t raw = (uint64_t)address;

    if (raw >= VECTOR_SCRATCH_BASE && raw + length <= VECTOR_SCRATCH_BASE + sizeof(memory->scratch))
    {
        memcpy((uint8_t*)memory->scratch + (raw - VECTOR_SCRATCH_BASE), source, length);
        return true;
    }

    return false;
}

static void SeedState(const VectorCase& testCase, GuestState::Registers& registers, VectorMemory& memory)
{
    memset(&memory, 0, sizeof(memory));
    memcpy(memory.code, testCase.code, sizeof(memory.code));
    memcpy(memory.scratch, testCase.scratch, sizeof(memory.scratch));

    GuestState::Reset(registers);
    for (int index = 0; index < VECTOR_GENERAL_COUNT; ++index) registers.general[index] = testCase.general[index];
    registers.stackPointer = VECTOR_SCRATCH_BASE + 0x1000;
    registers.flagsNzcv = testCase.nzcv;
}

static bool DecodeCase(const VectorCase& testCase, Ir::Function& function, VectorMemory& memory)
{
    Arm64Decoder::Context context;
    context.memory.Read = &MemoryRead;
    context.memory.Write = &MemoryWrite;
    context.memory.context = &memory;

    Ir::Block block;
    function.entryAddress = (GuestState::GuestAddress)VECTOR_CODE_BASE;
    function.endAddress = (GuestState::GuestAddress)(VECTOR_CODE_BASE + testCase.codeWords * 4);
    block.guestAddress = function.entryAddress;
    block.guestEndAddress = function.endAddress;

    for (int word = 0; word < testCase.codeWords; ++word)
    {
        GuestState::GuestAddress at = (GuestState::GuestAddress)(VECTOR_CODE_BASE + word * 4);
        if (Arm64Decoder::DecodeInstruction(context, testCase.code[word], at, function, block) != Arm64Decoder::Result::Ok) return false;
    }

    function.blocks.push_back(std::move(block));
    return Ir::Verify(function);
}

static int loweringCompiled = 0;

static void RunLoweringCases(LlvmBackend::Session& session, int& failures)
{
    for (int index = 0; index < LOWERING_CASE_COUNT; ++index)
    {
        const LoweringCase& loweringCase = LOWERING_CASES[index];

        VectorMemory memory;
        memset(&memory, 0, sizeof(memory));
        for (int word = 0; word < loweringCase.codeWords; ++word) memory.code[word] = loweringCase.code[word];

        Arm64Decoder::Context context;
        context.memory.Read = &MemoryRead;
        context.memory.Write = &MemoryWrite;
        context.memory.context = &memory;

        Ir::Function function;
        Ir::Block block;
        function.entryAddress = (GuestState::GuestAddress)VECTOR_CODE_BASE;
        function.endAddress = (GuestState::GuestAddress)(VECTOR_CODE_BASE + loweringCase.codeWords * 4);
        block.guestAddress = function.entryAddress;
        block.guestEndAddress = function.endAddress;

        bool decoded = true;
        for (int word = 0; word < loweringCase.codeWords; ++word)
        {
            GuestState::GuestAddress at = (GuestState::GuestAddress)(VECTOR_CODE_BASE + word * 4);
            if (Arm64Decoder::DecodeInstruction(context, loweringCase.code[word], at, function, block) != Arm64Decoder::Result::Ok)
            {
                printf("FAIL %s: word %d (%08x) did not decode\n", loweringCase.name, word, loweringCase.code[word]);
                failures += 1;
                decoded = false;
                break;
            }
        }
        if (!decoded) continue;

        function.blocks.push_back(std::move(block));
        if (!Ir::Verify(function))
        {
            printf("FAIL %s: the decoded IR does not verify\n", loweringCase.name);
            failures += 1;
            continue;
        }

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!LlvmBackend::CanLower(function, unsupported))
        {
            printf("FAIL %s: this case exists to exercise the lowering but T1 rejected it (%s)\n", loweringCase.name,
                   Ir::OpcodeName(unsupported));
            failures += 1;
            continue;
        }

        LlvmBackend::CompiledFunction target;
        if (LlvmBackend::CompileFunction(session, function, LlvmBackend::Tier::Baseline, target) != LlvmBackend::Result::Ok)
        {
            printf("FAIL %s: T1 compilation failed\n", loweringCase.name);
            failures += 1;
            continue;
        }

        GuestState::Registers interpreted;
        GuestState::Reset(interpreted);
        for (int reg = 0; reg < 4; ++reg) interpreted.general[reg] = loweringCase.general[reg];
        interpreted.stackPointer = VECTOR_SCRATCH_BASE + 0x1000;

        GuestState::Registers recompiled = interpreted;

        VectorMemory interpretedMemory = memory;
        VectorMemory recompiledMemory = memory;

        GuestState::MemoryAccessor accessor;
        accessor.Read = &MemoryRead;
        accessor.Write = &MemoryWrite;
        accessor.context = &interpretedMemory;

        Interpreter::Configuration interpreterConfiguration;
        Interpreter::RunBlock(function.blocks.front(), interpreted, accessor, interpreterConfiguration);

        LlvmBackend::RuntimeContext runtime;
        runtime.memory.Read = &MemoryRead;
        runtime.memory.Write = &MemoryWrite;
        runtime.memory.context = &recompiledMemory;

        Interpreter::ExitStatus status;
        ((LlvmBackend::EntryPoint)target.hostEntryPoint)(&recompiled, &runtime, &status, 0);

        loweringCompiled += 1;

        if (!GuestState::Equals(interpreted, recompiled))
        {
            printf("FAIL %s: T0 x0=%016llx x2=%016llx nzcv=%08x, T1 x0=%016llx x2=%016llx nzcv=%08x\n", loweringCase.name,
                   (unsigned long long)interpreted.general[0], (unsigned long long)interpreted.general[2], interpreted.flagsNzcv,
                   (unsigned long long)recompiled.general[0], (unsigned long long)recompiled.general[2], recompiled.flagsNzcv);
            failures += 1;
            continue;
        }

        printf("PASS %s\n", loweringCase.name);
    }
}

int main()
{
    LlvmBackend::Configuration configuration;
    LlvmBackend::Session session;

    if (LlvmBackend::Initialize(configuration, session) != LlvmBackend::Result::Ok)
    {
        printf("tier-differential FAILED: the LLVM backend did not initialise\n");
        return 1;
    }

    int compiled = 0;
    int skipped = 0;
    int failures = 0;

    for (int index = 0; index < VECTOR_CASE_COUNT; ++index)
    {
        const VectorCase& testCase = VECTOR_CASES[index];

        VectorMemory decodeMemory;
        memset(&decodeMemory, 0, sizeof(decodeMemory));
        memcpy(decodeMemory.code, testCase.code, sizeof(decodeMemory.code));

        Ir::Function function;
        if (!DecodeCase(testCase, function, decodeMemory))
        {
            printf("FAIL %s: the case did not decode or verify\n", testCase.name);
            failures += 1;
            continue;
        }

        Ir::Opcode unsupported = Ir::Opcode::Invalid;
        if (!LlvmBackend::CanLower(function, unsupported))
        {
            skipped += 1;
            continue;
        }

        LlvmBackend::CompiledFunction target;
        LlvmBackend::Result lowered = LlvmBackend::CompileFunction(session, function, LlvmBackend::Tier::Baseline, target);
        if (lowered != LlvmBackend::Result::Ok)
        {
            printf("FAIL %s: T1 compilation returned %s\n", testCase.name, LlvmBackend::ResultName(lowered));
            failures += 1;
            continue;
        }

        GuestState::Registers interpreted;
        VectorMemory interpretedMemory;
        SeedState(testCase, interpreted, interpretedMemory);

        GuestState::MemoryAccessor interpretedAccessor;
        interpretedAccessor.Read = &MemoryRead;
        interpretedAccessor.Write = &MemoryWrite;
        interpretedAccessor.context = &interpretedMemory;

        Interpreter::Configuration interpreterConfiguration;
        Interpreter::ExitStatus interpretedStatus =
            Interpreter::RunBlock(function.blocks.front(), interpreted, interpretedAccessor, interpreterConfiguration);

        GuestState::Registers recompiled;
        VectorMemory recompiledMemory;
        SeedState(testCase, recompiled, recompiledMemory);

        LlvmBackend::RuntimeContext runtime;
        runtime.memory.Read = &MemoryRead;
        runtime.memory.Write = &MemoryWrite;
        runtime.memory.context = &recompiledMemory;

        Interpreter::ExitStatus recompiledStatus;
        LlvmBackend::EntryPoint entry = (LlvmBackend::EntryPoint)target.hostEntryPoint;
        entry(&recompiled, &runtime, &recompiledStatus, 0);

        compiled += 1;

        if (!GuestState::Equals(interpreted, recompiled))
        {
            printf("FAIL %s: the register file differs between T0 and T1\n", testCase.name);
            failures += 1;
            continue;
        }

        if (memcmp(&interpretedMemory, &recompiledMemory, sizeof(VectorMemory)) != 0)
        {
            printf("FAIL %s: touched memory differs between T0 and T1\n", testCase.name);
            failures += 1;
            continue;
        }

        if (interpretedStatus.reason != recompiledStatus.reason)
        {
            printf("FAIL %s: T0 exited with %s, T1 with %s\n", testCase.name, Interpreter::ExitReasonName(interpretedStatus.reason),
                   Interpreter::ExitReasonName(recompiledStatus.reason));
            failures += 1;
            continue;
        }

        printf("PASS %s\n", testCase.name);
    }

    printf("\n--- directed lowering cases ---\n");
    RunLoweringCases(session, failures);

    LlvmBackend::Statistics statistics = LlvmBackend::GetStatistics(session);
    printf("\n%d silicon cases compiled and compared, %d skipped as not yet lowerable, %d directed cases, %d failed\n", compiled, skipped,
           loweringCompiled, failures);
    printf("backend: %llu functions compiled, %llu rejected, %llu IR instructions lowered\n",
           (unsigned long long)statistics.functionsCompiled, (unsigned long long)statistics.functionsRejected,
           (unsigned long long)statistics.instructionsLowered);

    LlvmBackend::Shutdown(session);

    if (failures != 0)
    {
        printf("tier-differential FAILED: %d cases diverged\n", failures);
        return 1;
    }

    if (compiled < MINIMUM_COMPILED_CASES)
    {
        printf("tier-differential FAILED: only %d silicon cases ran under T1, below the floor of %d - lowering coverage\n"
               "regressed\n",
               compiled, MINIMUM_COMPILED_CASES);
        return 1;
    }

    if (loweringCompiled < MINIMUM_LOWERING_CASES)
    {
        printf("tier-differential FAILED: only %d directed lowering cases ran, below the floor of %d\n", loweringCompiled,
               MINIMUM_LOWERING_CASES);
        return 1;
    }

    if (statistics.functionsCompiled == 0)
    {
        printf("tier-differential FAILED: the backend reports zero compiled functions, so T1 never ran\n");
        return 1;
    }

    printf("tier-differential PASSED: T0 and T1 agree on %d cases\n", compiled);
    return 0;
}

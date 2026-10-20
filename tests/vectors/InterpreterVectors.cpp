#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "Interpreter/Interpreter.hpp"

#include "VectorCases.hpp"

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

static std::string Project(const VectorCase& testCase, const GuestState::Registers& registers, const VectorMemory& memory)
{
    std::string line = testCase.name;
    char field[64];

    for (int index = 0; index < VECTOR_GENERAL_COUNT; ++index)
    {
        if (index >= VECTOR_RESERVED_LOW && index <= VECTOR_RESERVED_HIGH) continue;
        snprintf(field, sizeof(field), " x%d=%016llx", index, (unsigned long long)registers.general[index]);
        line += field;
    }

    for (int index = 0; index < VECTOR_REGISTER_COUNT; ++index)
    {
        snprintf(field, sizeof(field), " v%d=%016llx:%016llx", index, (unsigned long long)registers.vector[index].high,
                 (unsigned long long)registers.vector[index].low);
        line += field;
    }

    snprintf(field, sizeof(field), " nzcv=%08x", registers.flagsNzcv & 0xF0000000u);
    line += field;
    snprintf(field, sizeof(field), " fpsr=%08x", registers.floatingPointStatus & 0x0800009Fu);
    line += field;

    for (int index = 0; index < VECTOR_SCRATCH_WORDS - 1; ++index)
    {
        snprintf(field, sizeof(field), " m%d=%016llx", index, (unsigned long long)memory.scratch[index]);
        line += field;
    }

    return line;
}

int main(int argc, char** argv)
{
    const char* goldenPath = argc > 1 ? argv[1] : nullptr;
    if (!goldenPath)
    {
        fprintf(stderr, "usage: InterpreterVectorsTest <golden>\n");
        return 2;
    }

    FILE* golden = fopen(goldenPath, "rb");
    if (!golden)
    {
        fprintf(stderr, "cannot open %s\n", goldenPath);
        return 2;
    }

    std::vector<std::string> expected;
    char line[4096];
    while (fgets(line, sizeof(line), golden))
    {
        size_t length = strlen(line);
        while (length > 0 && (line[length - 1] == '\n' || line[length - 1] == '\r')) line[--length] = '\0';
        if (length > 0) expected.push_back(line);
    }
    fclose(golden);

    if ((int)expected.size() != VECTOR_CASE_COUNT)
    {
        printf("interpreter-vectors FAILED: the golden holds %d lines but there are %d cases\n", (int)expected.size(), VECTOR_CASE_COUNT);
        printf("the suite changed size; recapture with tools/capture-vectors.sh --regenerate\n");
        return 1;
    }

    int failures = 0;

    for (int index = 0; index < VECTOR_CASE_COUNT; ++index)
    {
        const VectorCase& testCase = VECTOR_CASES[index];

        VectorMemory memory;
        memset(&memory, 0, sizeof(memory));
        memcpy(memory.code, testCase.code, sizeof(memory.code));
        memcpy(memory.scratch, testCase.scratch, sizeof(memory.scratch));

        Arm64Decoder::Context context;
        context.memory.Read = &MemoryRead;
        context.memory.Write = &MemoryWrite;
        context.memory.context = &memory;

        Ir::Function function;
        Ir::Block block;
        function.entryAddress = static_cast<GuestState::GuestAddress>(VECTOR_CODE_BASE);
        function.endAddress = static_cast<GuestState::GuestAddress>(VECTOR_CODE_BASE + testCase.codeWords * 4);
        block.guestAddress = function.entryAddress;
        block.guestEndAddress = function.endAddress;

        bool decoded = true;
        for (int word = 0; word < testCase.codeWords; ++word)
        {
            GuestState::GuestAddress at = static_cast<GuestState::GuestAddress>(VECTOR_CODE_BASE + word * 4);
            Arm64Decoder::Result result = Arm64Decoder::DecodeInstruction(context, testCase.code[word], at, function, block);
            if (result != Arm64Decoder::Result::Ok)
            {
                printf("FAIL %s: word %d (%08x) decoded as %s\n", testCase.name, word, testCase.code[word],
                       Arm64Decoder::ResultName(result));
                decoded = false;
                failures += 1;
                break;
            }
        }
        if (!decoded) continue;

        Ir::Function verified = function;
        verified.blocks.push_back(block);
        if (!Ir::Verify(verified))
        {
            printf("FAIL %s: the decoded IR does not verify\n", testCase.name);
            failures += 1;
            continue;
        }

        GuestState::Registers registers;
        GuestState::Reset(registers);
        for (int reg = 0; reg < VECTOR_GENERAL_COUNT; ++reg) registers.general[reg] = testCase.general[reg];
        registers.stackPointer = VECTOR_SCRATCH_BASE + 0x1000;
        registers.flagsNzcv = testCase.nzcv;

        GuestState::MemoryAccessor accessor;
        accessor.Read = &MemoryRead;
        accessor.Write = &MemoryWrite;
        accessor.context = &memory;

        Interpreter::Configuration configuration;
        Interpreter::ExitStatus status = Interpreter::RunBlock(block, registers, accessor, configuration);

        if (status.reason != Interpreter::ExitReason::BlockExhausted)
        {
            printf("FAIL %s: T0 exited with %s\n", testCase.name, Interpreter::ExitReasonName(status.reason));
            failures += 1;
            continue;
        }

        std::string produced = Project(testCase, registers, memory);
        if (produced != expected[index])
        {
            printf("FAIL %s\n  device %s\n  t0     %s\n", testCase.name, expected[index].c_str(), produced.c_str());
            failures += 1;
            continue;
        }

        printf("PASS %s\n", testCase.name);
    }

    printf("%s: %d of %d cases reproduced under T0\n", failures == 0 ? "interpreter-vectors PASSED" : "interpreter-vectors FAILED",
           VECTOR_CASE_COUNT - failures, VECTOR_CASE_COUNT);

    return failures == 0 ? 0 : 1;
}

#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>

#include "VectorCases.hpp"

static uint32_t* MapFixed(uint64_t address, size_t length, int protection)
{
    void* mapped = mmap((void*)address, length, protection, MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0);
    if (mapped == MAP_FAILED) return nullptr;
    if ((uint64_t)mapped != address) return nullptr;
    return (uint32_t*)mapped;
}

static int EmitEpilogue(uint32_t* code, int at)
{
    static const uint32_t epilogue[] = {
        0xa9000640u, 0xa9010e42u, 0xa9021644u, 0xa9031e46u, 0xa9042648u, 0xa9052e4au, 0xa906364cu,
        0xa9073e4eu, 0xa909d253u, 0xa90ada55u, 0xa90be257u, 0xa90cea59u, 0xa90df25bu, 0xa90efa5du,
        0xad090640u, 0xad0a0e42u, 0xad0b1644u, 0xad0c1e46u, 0xad0d2648u, 0xad0e2e4au, 0xad0f364cu, 0xad103e4eu,
        0xad114650u, 0xad124e52u, 0xad135654u, 0xad145e56u, 0xad156658u, 0xad166e5au, 0xad17765cu, 0xad187e5eu,
        0xd53b4210u, 0xb9011250u, 0xd53b4430u, 0xb9011650u, 0xf9408250u, 0xd61f0200u,
    };

    for (size_t index = 0; index < sizeof(epilogue) / sizeof(epilogue[0]); ++index)
    {
        code[at + (int)index] = epilogue[index];
    }

    return at + (int)(sizeof(epilogue) / sizeof(epilogue[0]));
}

int main(void)
{
    size_t pageLength = 65536;

    uint32_t* code = MapFixed(VECTOR_CODE_BASE, pageLength, PROT_READ | PROT_WRITE);
    if (!code) { printf("error: could not map the code page at the fixed address\n"); return 2; }

    uint32_t* state = MapFixed(VECTOR_STATE_BASE, pageLength, PROT_READ | PROT_WRITE);
    if (!state) { printf("error: could not map the state page at the fixed address\n"); return 2; }

    uint32_t* scratchPage = MapFixed(VECTOR_SCRATCH_BASE, pageLength, PROT_READ | PROT_WRITE);
    if (!scratchPage) { printf("error: could not map the scratch page at the fixed address\n"); return 2; }

    VectorState* live = (VectorState*)state;

    for (int index = 0; index < VECTOR_CASE_COUNT; ++index)
    {
        const VectorCase& testCase = VECTOR_CASES[index];

        memset(live, 0, sizeof(VectorState));
        memset(scratchPage, 0, pageLength);
        for (int reg = 0; reg < VECTOR_GENERAL_COUNT; ++reg) live->general[reg] = testCase.general[reg];
        for (int word = 0; word < VECTOR_SCRATCH_WORDS; ++word) ((uint64_t*)scratchPage)[word] = testCase.scratch[word];
        live->nzcv = testCase.nzcv;
        live->stackPointer = VECTOR_SCRATCH_BASE + 0x1000;

        memset(code, 0, pageLength);
        for (int word = 0; word < testCase.codeWords; ++word) code[word] = testCase.code[word];
        EmitEpilogue(code, testCase.codeWords);

        if (mprotect(code, pageLength, PROT_READ | PROT_EXEC) != 0) { printf("error: mprotect failed\n"); return 2; }
        __builtin___clear_cache((char*)code, (char*)code + pageLength);

        register uint64_t statePointer asm("x18") = VECTOR_STATE_BASE;

        asm volatile(
            "str x29, [x18, %[savedFrameOffset]]\n\t"
            "adr x30, 1f\n\t"
            "str x30, [x18, %[returnOffset]]\n\t"
            "ldr w17, [x18, %[nzcvOffset]]\n\t"
            "msr nzcv, x17\n\t"
            "msr fpsr, xzr\n\t"
            "ldr x17, [x18, %[stackOffset]]\n\t"
            "mov x16, sp\n\t"
            "mov sp, x17\n\t"
            "str x16, [x18, %[savedStackOffset]]\n\t"
            "ldp q0, q1, [x18, #288]\n\t"
            "ldp q2, q3, [x18, #320]\n\t"
            "ldp q4, q5, [x18, #352]\n\t"
            "ldp q6, q7, [x18, #384]\n\t"
            "ldp q8, q9, [x18, #416]\n\t"
            "ldp q10, q11, [x18, #448]\n\t"
            "ldp q12, q13, [x18, #480]\n\t"
            "ldp q14, q15, [x18, #512]\n\t"
            "ldp q16, q17, [x18, #544]\n\t"
            "ldp q18, q19, [x18, #576]\n\t"
            "ldp q20, q21, [x18, #608]\n\t"
            "ldp q22, q23, [x18, #640]\n\t"
            "ldp q24, q25, [x18, #672]\n\t"
            "ldp q26, q27, [x18, #704]\n\t"
            "ldp q28, q29, [x18, #736]\n\t"
            "ldp q30, q31, [x18, #768]\n\t"
            "ldp x0, x1, [x18, #0]\n\t"
            "ldp x2, x3, [x18, #16]\n\t"
            "ldp x4, x5, [x18, #32]\n\t"
            "ldp x6, x7, [x18, #48]\n\t"
            "ldp x8, x9, [x18, #64]\n\t"
            "ldp x10, x11, [x18, #80]\n\t"
            "ldp x12, x13, [x18, #96]\n\t"
            "ldp x14, x15, [x18, #112]\n\t"
            "ldp x19, x20, [x18, #152]\n\t"
            "ldp x21, x22, [x18, #168]\n\t"
            "ldp x23, x24, [x18, #184]\n\t"
            "ldp x25, x26, [x18, #200]\n\t"
            "ldp x27, x28, [x18, #216]\n\t"
            "ldp x29, x30, [x18, #232]\n\t"
            "movz x17, #0x2000, lsl #16\n\t"
            "br x17\n\t"
            "1:\n\t"
            "ldr x16, [x18, %[savedStackOffset]]\n\t"
            "mov sp, x16\n\t"
            "ldr x29, [x18, %[savedFrameOffset]]\n\t"
            :
            : [returnOffset] "i"(VECTOR_OFFSET_RETURN_ADDRESS), [nzcvOffset] "i"(VECTOR_OFFSET_NZCV),
              [stackOffset] "i"(VECTOR_OFFSET_STACK_POINTER), [savedStackOffset] "i"(VECTOR_OFFSET_SAVED_STACK),
              [savedFrameOffset] "i"(VECTOR_OFFSET_SAVED_FRAME), "r"(statePointer)
            : "x0", "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13", "x14", "x15", "x16", "x17", "x19", "x20",
              "x21", "x22", "x23", "x24", "x25", "x26", "x27", "x28", "x30", "memory", "cc", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8",
              "v9", "v10", "v11", "v12", "v13", "v14", "v15", "v16", "v17", "v18", "v19", "v20", "v21", "v22", "v23", "v24", "v25", "v26", "v27",
              "v28", "v29", "v30", "v31");

        if (mprotect(code, pageLength, PROT_READ | PROT_WRITE) != 0) { printf("error: mprotect back failed\n"); return 2; }

        printf("%s", testCase.name);
        for (int reg = 0; reg < VECTOR_GENERAL_COUNT; ++reg)
        {
            if (reg >= VECTOR_RESERVED_LOW && reg <= VECTOR_RESERVED_HIGH) continue;
            printf(" x%d=%016llx", reg, (unsigned long long)live->general[reg]);
        }
        for (int reg = 0; reg < VECTOR_REGISTER_COUNT; ++reg)
        {
            printf(" v%d=%016llx:%016llx", reg, (unsigned long long)live->vector[reg * 2 + 1], (unsigned long long)live->vector[reg * 2]);
        }
        printf(" nzcv=%08x", live->nzcv & 0xF0000000u);
        printf(" fpsr=%08x", live->floatingPointStatus & 0x0800009Fu);
        for (int word = 0; word < VECTOR_SCRATCH_WORDS - 1; ++word)
        {
            printf(" m%d=%016llx", word, (unsigned long long)((uint64_t*)scratchPage)[word]);
        }
        printf("\n");
        fflush(stdout);
    }

    return 0;
}

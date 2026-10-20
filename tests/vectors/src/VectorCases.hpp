#pragma once

#include <stdint.h>

#define VECTOR_CODE_BASE 0x0000000020000000ull
#define VECTOR_STATE_BASE 0x0000000050000000ull
#define VECTOR_SCRATCH_BASE 0x0000000040000000ull

#define VECTOR_GENERAL_COUNT 31
#define VECTOR_REGISTER_COUNT 32
#define VECTOR_SCRATCH_WORDS 8
#define VECTOR_MAX_CODE_WORDS 24

#define VECTOR_RESERVED_LOW 16
#define VECTOR_RESERVED_HIGH 18

struct VectorState
{
    uint64_t general[VECTOR_GENERAL_COUNT];
    uint64_t stackPointer;
    uint64_t returnAddress;
    uint64_t savedStackPointer;
    uint32_t nzcv;
    uint32_t floatingPointStatus;
    uint64_t savedFramePointer;
    alignas(16) uint64_t vector[VECTOR_REGISTER_COUNT * 2];
};

#define VECTOR_OFFSET_GENERAL 0
#define VECTOR_OFFSET_STACK_POINTER 248
#define VECTOR_OFFSET_RETURN_ADDRESS 256
#define VECTOR_OFFSET_SAVED_STACK 264
#define VECTOR_OFFSET_NZCV 272
#define VECTOR_OFFSET_FPSR 276
#define VECTOR_OFFSET_SAVED_FRAME 280
#define VECTOR_OFFSET_VECTORS 288

struct VectorCase
{
    const char* name;
    uint64_t general[VECTOR_GENERAL_COUNT];
    uint64_t scratch[VECTOR_SCRATCH_WORDS];
    uint32_t nzcv;
    uint32_t code[VECTOR_MAX_CODE_WORDS];
    int codeWords;
};

#ifdef __cplusplus
extern "C"
{
#endif

    extern const VectorCase VECTOR_CASES[];
    extern const int VECTOR_CASE_COUNT;

#ifdef __cplusplus
}
#endif

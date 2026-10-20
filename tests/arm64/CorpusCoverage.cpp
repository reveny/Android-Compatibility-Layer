#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"

static bool ReadNothing(void*, GuestState::GuestAddress, void*, uint64_t)
{
    return false;
}

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: CorpusCoverageTest <text-blob> [maximum-unimplemented-fraction]\n");
        return 2;
    }

    double allowed = argc > 2 ? atof(argv[2]) : 1.0;

    FILE* blob = fopen(argv[1], "rb");
    if (!blob)
    {
        fprintf(stderr, "cannot open %s\n", argv[1]);
        return 2;
    }

    std::vector<uint32_t> words;
    uint32_t encoding = 0;
    while (fread(&encoding, sizeof(encoding), 1, blob) == 1) words.push_back(encoding);
    fclose(blob);

    if (words.empty())
    {
        printf("corpus-coverage FAILED: the blob held no instructions\n");
        return 1;
    }

    Arm64Decoder::Context context;
    context.memory.Read = &ReadNothing;

    int ok = 0;
    int unimplemented = 0;
    int unallocated = 0;

    for (size_t index = 0; index < words.size(); ++index)
    {
        Ir::Function function;
        Ir::Block block;

        Arm64Decoder::Result result =
            Arm64Decoder::DecodeInstruction(context, words[index], static_cast<GuestState::GuestAddress>(0x1000 + index * 4), function, block);

        if (result == Arm64Decoder::Result::Ok) ok += 1;
        else if (result == Arm64Decoder::Result::Unallocated) unallocated += 1;
        else
        {
            unimplemented += 1;
        }
    }

    int total = (int)words.size();
    double fraction = (double)unimplemented / (double)total;

    printf("corpus instructions=%d decoded=%d unimplemented=%d unallocated=%d coverage=%.1f%%\n", total, ok, unimplemented, unallocated,
           100.0 * (double)ok / (double)total);

    if (fraction > allowed)
    {
        printf("corpus-coverage FAILED: %.1f%% unimplemented exceeds the %.1f%% budget\n", 100.0 * fraction, 100.0 * allowed);
        return 1;
    }

    printf("corpus-coverage PASSED\n");
    return 0;
}

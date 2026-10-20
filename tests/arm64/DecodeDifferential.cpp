#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

#include "Arm64Decoder/Arm64Decoder.hpp"
#include "Interpreter/Interpreter.hpp"

static bool ReadNothing(void*, GuestState::GuestAddress, void*, uint64_t)
{
    return false;
}

static Arm64Decoder::Result DecodeOne(uint32_t encoding, Ir::Function& function, Ir::Block& block)
{
    Arm64Decoder::Context context;
    context.memory.Read = &ReadNothing;

    return Arm64Decoder::DecodeInstruction(context, encoding, static_cast<GuestState::GuestAddress>(0x1000), function, block);
}

static std::string FirstToken(const std::string& line)
{
    size_t start = line.find_first_not_of(" \t");
    if (start == std::string::npos) return std::string();

    size_t end = line.find_first_of(" \t", start);
    if (end == std::string::npos) return line.substr(start);
    return line.substr(start, end - start);
}

static uint32_t NextRandom(uint64_t& state)
{
    state = state * 6364136223846793005ull + 1442695040888963407ull;
    return (uint32_t)(state >> 32);
}

static int Generate(int count, uint64_t seed)
{
    uint64_t state = seed;

    for (int index = 0; index < count; ++index)
    {
        uint32_t encoding = NextRandom(state);

        if ((index % 4) == 1)
        {
            uint32_t shapes[] = {0x8b000000u, 0xcb000000u, 0xab000000u, 0xeb000000u, 0x91000000u, 0xd1000000u,
                                 0x0b000000u, 0x4b000000u, 0x8a000000u, 0xaa000000u, 0xca000000u, 0xea000000u};
            encoding = shapes[NextRandom(state) % (sizeof(shapes) / sizeof(shapes[0]))] | (NextRandom(state) & 0x00ffffffu);
        }
        else if ((index % 4) == 2)
        {
            uint32_t shapes[] = {0x14000000u, 0x94000000u, 0x54000000u, 0x34000000u, 0xb4000000u,
                                 0x36000000u, 0xd65f0000u, 0xd63f0000u, 0xd61f0000u, 0xd2800000u, 0xf2800000u, 0x92800000u};
            encoding = shapes[NextRandom(state) % (sizeof(shapes) / sizeof(shapes[0]))] | (NextRandom(state) & 0x00ffffffu);
        }

        printf("%08x\n", encoding);
    }

    return 0;
}

int main(int argc, char** argv)
{
    if (argc >= 4 && strcmp(argv[1], "--generate") == 0)
    {
        return Generate(atoi(argv[2]), strtoull(argv[3], nullptr, 10));
    }

    const char* referencePath = argc > 1 ? argv[1] : nullptr;
    if (!referencePath)
    {
        fprintf(stderr, "usage: DecodeDifferentialTest <llvm-mc-reference> | --generate <count> <seed>\n");
        return 2;
    }

    FILE* reference = fopen(referencePath, "rb");
    if (!reference)
    {
        fprintf(stderr, "cannot open %s\n", referencePath);
        return 2;
    }

    int total = 0;
    int agreedUndefined = 0;
    int weRejectedTheyAccepted = 0;
    int weAcceptedTheyRejected = 0;
    int agreedDecodable = 0;
    int ambiguous = 0;
    int notYetImplemented = 0;
    int notYetImplementedButUndefined = 0;
    std::vector<std::string> examples;

    char line[512];
    while (fgets(line, sizeof(line), reference))
    {
        unsigned encoding = 0;
        char mnemonic[128] = {0};
        if (sscanf(line, "%x %127[^\n]", &encoding, mnemonic) != 2) continue;

        total += 1;

        std::string reported = FirstToken(mnemonic);
        if (reported == "AMBIGUOUS")
        {
            ambiguous += 1;
            continue;
        }

        bool referenceUndefined = reported == "udf" || reported == "INVALID";

        Ir::Function function;
        Ir::Block block;
        Arm64Decoder::Result result = DecodeOne(encoding, function, block);

        bool weRejected = result == Arm64Decoder::Result::Unallocated;
        bool weDecoded = result == Arm64Decoder::Result::Ok;

        if (referenceUndefined && weRejected) agreedUndefined += 1;
        else if (referenceUndefined && weDecoded)
        {
            weAcceptedTheyRejected += 1;
            if (examples.size() < 10)
            {
                char buffer[128];
                snprintf(buffer, sizeof(buffer), "%08x we=Ok llvm=%s", encoding, reported.c_str());
                examples.push_back(buffer);
            }
        }
        else if (!referenceUndefined && weRejected)
        {
            weRejectedTheyAccepted += 1;
            if (examples.size() < 10)
            {
                char buffer[128];
                snprintf(buffer, sizeof(buffer), "%08x we=Unallocated llvm=%s", encoding, reported.c_str());
                examples.push_back(buffer);
            }
        }
        else if (!referenceUndefined && weDecoded) agreedDecodable += 1;
        else if (!referenceUndefined) notYetImplemented += 1;
        else notYetImplementedButUndefined += 1;
    }

    fclose(reference);

    printf("encodings=%d agreed_decodable=%d agreed_undefined=%d ambiguous_skipped=%d\n", total, agreedDecodable, agreedUndefined, ambiguous);
    printf("we_accepted_llvm_rejected=%d we_rejected_llvm_accepted=%d\n", weAcceptedTheyRejected, weRejectedTheyAccepted);
    printf("unimplemented_llvm_accepted=%d unimplemented_llvm_rejected=%d\n", notYetImplemented, notYetImplementedButUndefined);

    for (const std::string& example : examples) printf("example %s\n", example.c_str());

    if (total == 0)
    {
        printf("decode-differential FAILED: the reference file held no encodings\n");
        return 1;
    }

    if (weAcceptedTheyRejected != 0)
    {
        printf("decode-differential FAILED: %d encodings decoded that llvm-mc calls undefined\n", weAcceptedTheyRejected);
        return 1;
    }

    printf("decode-differential PASSED: no encoding was accepted that llvm-mc rejects\n");
    return 0;
}

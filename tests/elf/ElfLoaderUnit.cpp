#include <cstdint>
#include <cstdio>
#include <cstring>
#include <elf.h>
#include <string>
#include <vector>

#include "ElfLoader/ElfLoader.hpp"
#include "GuestRuntime/GuestRuntime.hpp"

static constexpr uint64_t IMAGE_BYTES = 0x400;
static constexpr uint64_t DYNSTR_OFFSET = 0x100;
static constexpr uint64_t DYNSTR_BYTES = 0x40;
static constexpr uint64_t DYNSYM_OFFSET = 0x140;
static constexpr uint64_t GNUHASH_OFFSET = 0x180;
static constexpr uint64_t RELA_OFFSET = 0x1C0;
static constexpr uint64_t DYNAMIC_OFFSET = 0x200;
static constexpr uint64_t TARGET_OFFSET = 0x300;

static constexpr uint64_t NAME_WIDGET = 1;
static constexpr uint64_t NAME_MISSING = 8;

static int failures = 0;

struct Blueprint
{
    uint8_t elfClass = ELFCLASS64;
    uint8_t elfData = ELFDATA2LSB;
    uint16_t machine = EM_AARCH64;
    uint16_t type = ET_DYN;
    uint16_t programHeaderCount = 2;
    bool includeLoad = true;
    bool includeDynamic = true;
    bool includeGnuHash = true;
    bool oversizeFileSize = false;
    bool offsetPastEnd = false;
    bool corruptStringTable = false;
    uint64_t symbolEntrySize = sizeof(Elf64_Sym);
    uint32_t relocationType = 0;
    bool needMissingLibrary = false;
    bool undefinedSymbol = false;
    bool weakSymbol = false;
    uint64_t relocationOffset = TARGET_OFFSET;
    bool unalignedSegmentAlign = false;
    bool overflowingSegment = false;
};

static void Place(std::vector<uint8_t>& image, uint64_t offset, const void* source, uint64_t bytes)
{
    memcpy(image.data() + offset, source, (size_t)bytes);
}

static std::vector<uint8_t> BuildImage(const Blueprint& blueprint)
{
    std::vector<uint8_t> image((size_t)IMAGE_BYTES, 0);

    const char strings[] = "\0widget\0libmissing.so";
    Place(image, DYNSTR_OFFSET, strings, sizeof(strings));

    Elf64_Sym symbols[2] = {};
    symbols[1].st_name = (Elf32_Word)NAME_WIDGET;
    symbols[1].st_info = (unsigned char)ELF64_ST_INFO(blueprint.weakSymbol ? STB_WEAK : STB_GLOBAL, STT_FUNC);
    symbols[1].st_shndx = blueprint.undefinedSymbol ? SHN_UNDEF : 1;
    symbols[1].st_value = blueprint.undefinedSymbol ? 0 : TARGET_OFFSET;
    Place(image, DYNSYM_OFFSET, symbols, sizeof(symbols));

    uint32_t hashHeader[4] = {1, 2, 1, 0};
    Place(image, GNUHASH_OFFSET, hashHeader, sizeof(hashHeader));

    if (blueprint.relocationType != 0)
    {
        Elf64_Rela relocation = {};
        relocation.r_offset = blueprint.relocationOffset;
        relocation.r_info = ELF64_R_INFO(1, blueprint.relocationType);
        relocation.r_addend = 0;
        Place(image, RELA_OFFSET, &relocation, sizeof(relocation));
    }

    std::vector<Elf64_Dyn> dynamic;
    auto add = [&dynamic](int64_t tag, uint64_t value) {
        Elf64_Dyn entry = {};
        entry.d_tag = tag;
        entry.d_un.d_val = value;
        dynamic.push_back(entry);
    };

    add(DT_STRTAB, blueprint.corruptStringTable ? IMAGE_BYTES + 0x1000 : DYNSTR_OFFSET);
    add(DT_STRSZ, DYNSTR_BYTES);
    add(DT_SYMTAB, DYNSYM_OFFSET);
    add(DT_SYMENT, blueprint.symbolEntrySize);
    if (blueprint.includeGnuHash) add(DT_GNU_HASH, GNUHASH_OFFSET);
    if (blueprint.needMissingLibrary) add(DT_NEEDED, NAME_MISSING);

    if (blueprint.relocationType != 0)
    {
        add(DT_RELA, RELA_OFFSET);
        add(DT_RELASZ, sizeof(Elf64_Rela));
        add(DT_RELAENT, sizeof(Elf64_Rela));
    }

    add(DT_NULL, 0);
    Place(image, DYNAMIC_OFFSET, dynamic.data(), dynamic.size() * sizeof(Elf64_Dyn));

    std::vector<Elf64_Phdr> programs;

    if (blueprint.includeLoad)
    {
        Elf64_Phdr load = {};
        load.p_type = PT_LOAD;
        load.p_flags = PF_R | PF_W;
        load.p_offset = blueprint.offsetPastEnd ? IMAGE_BYTES + 0x1000 : 0;
        load.p_vaddr = 0;
        load.p_filesz = blueprint.oversizeFileSize ? IMAGE_BYTES * 2 : IMAGE_BYTES;
        load.p_memsz = IMAGE_BYTES;
        load.p_align = blueprint.unalignedSegmentAlign ? 0x1001 : 0x1000;
        programs.push_back(load);
    }

    if (blueprint.overflowingSegment)
    {
        Elf64_Phdr overflowing = {};
        overflowing.p_type = PT_LOAD;
        overflowing.p_flags = PF_R;
        overflowing.p_offset = 0;
        overflowing.p_vaddr = 0xFFFFFFFFFFFFE000ull;
        overflowing.p_filesz = 0;
        overflowing.p_memsz = 0x1000;
        overflowing.p_align = 0x1000;
        programs.push_back(overflowing);
    }

    if (blueprint.includeDynamic)
    {
        Elf64_Phdr dynamicHeader = {};
        dynamicHeader.p_type = PT_DYNAMIC;
        dynamicHeader.p_flags = PF_R | PF_W;
        dynamicHeader.p_offset = DYNAMIC_OFFSET;
        dynamicHeader.p_vaddr = DYNAMIC_OFFSET;
        dynamicHeader.p_filesz = dynamic.size() * sizeof(Elf64_Dyn);
        dynamicHeader.p_memsz = dynamic.size() * sizeof(Elf64_Dyn);
        dynamicHeader.p_align = 8;
        programs.push_back(dynamicHeader);
    }

    Elf64_Ehdr header = {};
    memcpy(header.e_ident, ELFMAG, SELFMAG);
    header.e_ident[EI_CLASS] = blueprint.elfClass;
    header.e_ident[EI_DATA] = blueprint.elfData;
    header.e_ident[EI_VERSION] = EV_CURRENT;
    header.e_type = blueprint.type;
    header.e_machine = blueprint.machine;
    header.e_version = EV_CURRENT;
    header.e_ehsize = sizeof(Elf64_Ehdr);
    header.e_phoff = sizeof(Elf64_Ehdr);
    header.e_phentsize = sizeof(Elf64_Phdr);
    header.e_phnum = blueprint.programHeaderCount == 2 ? (uint16_t)programs.size() : blueprint.programHeaderCount;

    Place(image, sizeof(Elf64_Ehdr), programs.data(), programs.size() * sizeof(Elf64_Phdr));
    Place(image, 0, &header, sizeof(header));

    return image;
}

static void Expect(const Blueprint& blueprint, ElfLoader::Result expected, const char* label, uint64_t truncateTo = 0)
{
    std::vector<uint8_t> image = BuildImage(blueprint);
    if (truncateTo != 0 && truncateTo < image.size()) image.resize((size_t)truncateTo);

    GuestRuntime::Configuration configuration;
    GuestRuntime::Instance instance;

    if (GuestRuntime::Create(configuration, instance) != GuestRuntime::Result::Ok)
    {
        printf("FAIL %s: the guest runtime did not start\n", label);
        failures += 1;
        return;
    }

    uint32_t moduleId = ElfLoader::INVALID_MODULE_ID;
    ElfLoader::Result produced =
        ElfLoader::LoadFromMemory(GuestRuntime::LibraryNamespace(instance), image.data(), image.size(), "libcase.so", moduleId);

    GuestRuntime::Destroy(instance);

    if (produced == expected)
    {
        printf("PASS %s -> %s\n", label, ElfLoader::ResultName(produced));
        return;
    }

    printf("FAIL %s: expected %s, got %s\n", label, ElfLoader::ResultName(expected), ElfLoader::ResultName(produced));
    failures += 1;
}

int main()
{
    Blueprint valid;
    Expect(valid, ElfLoader::Result::Ok, "a minimal well-formed image loads");

    Blueprint truncated;
    Expect(truncated, ElfLoader::Result::NotAnElf, "a header shorter than Elf64_Ehdr is rejected", 32);

    Blueprint badMagic;
    badMagic.elfData = ELFDATA2MSB;
    Expect(badMagic, ElfLoader::Result::WrongClass, "big-endian is rejected");

    Blueprint wrongClass;
    wrongClass.elfClass = ELFCLASS32;
    Expect(wrongClass, ElfLoader::Result::WrongClass, "ELFCLASS32 is rejected");

    Blueprint wrongMachine;
    wrongMachine.machine = EM_X86_64;
    Expect(wrongMachine, ElfLoader::Result::WrongMachine, "a non-AArch64 machine is rejected");

    Blueprint executable;
    executable.type = ET_EXEC;
    Expect(executable, ElfLoader::Result::NotAnElf, "ET_EXEC is rejected");

    Blueprint noPrograms;
    noPrograms.programHeaderCount = 0;
    Expect(noPrograms, ElfLoader::Result::MalformedElf, "e_phnum of zero is rejected");

    Blueprint noLoad;
    noLoad.includeLoad = false;
    Expect(noLoad, ElfLoader::Result::MalformedElf, "an image with no PT_LOAD is rejected");

    Blueprint noDynamic;
    noDynamic.includeDynamic = false;
    Expect(noDynamic, ElfLoader::Result::MalformedElf, "an image with no PT_DYNAMIC is rejected");

    Blueprint oversize;
    oversize.oversizeFileSize = true;
    Expect(oversize, ElfLoader::Result::MalformedElf, "p_filesz larger than p_memsz is rejected");

    Blueprint pastEnd;
    pastEnd.offsetPastEnd = true;
    Expect(pastEnd, ElfLoader::Result::MalformedElf, "a segment whose contents run past the file is rejected");

    Blueprint unalignedAlign;
    unalignedAlign.unalignedSegmentAlign = true;
    Expect(unalignedAlign, ElfLoader::Result::MalformedElf, "a p_align that is not a power of two is rejected");

    Blueprint overflowing;
    overflowing.overflowingSegment = true;
    Expect(overflowing, ElfLoader::Result::MalformedElf, "a segment whose p_vaddr plus p_memsz wraps is rejected");

    Blueprint wrappingSpan;
    wrappingSpan.unalignedSegmentAlign = true;
    wrappingSpan.overflowingSegment = true;
    Expect(wrappingSpan, ElfLoader::Result::MalformedElf,
           "a wrapping reservation span cannot escape the module bounds check");

    Blueprint corruptStrings;
    corruptStrings.corruptStringTable = true;
    Expect(corruptStrings, ElfLoader::Result::MalformedElf, "a DT_STRTAB outside every segment is rejected");

    Blueprint noHash;
    noHash.includeGnuHash = false;
    Expect(noHash, ElfLoader::Result::MalformedElf, "an image with no DT_GNU_HASH is rejected");

    Blueprint badSymbolEntry;
    badSymbolEntry.symbolEntrySize = 16;
    Expect(badSymbolEntry, ElfLoader::Result::MalformedElf, "a DT_SYMENT that is not sizeof(Elf64_Sym) is rejected");

    Blueprint missingDependency;
    missingDependency.needMissingLibrary = true;
    Expect(missingDependency, ElfLoader::Result::DependencyNotFound, "an unproxied DT_NEEDED that is not on the search path is rejected");

    Blueprint unsupportedRelocation;
    unsupportedRelocation.relocationType = R_AARCH64_COPY;
    Expect(unsupportedRelocation, ElfLoader::Result::UnsupportedRelocation, "an unimplemented relocation type is named, not ignored");

    Blueprint threadLocalWithoutTls;
    threadLocalWithoutTls.relocationType = R_AARCH64_TLSDESC;
    Expect(threadLocalWithoutTls, ElfLoader::Result::UnsupportedRelocation, "a TLS relocation in an image with no PT_TLS is rejected");

    Blueprint unresolved;
    unresolved.relocationType = R_AARCH64_JUMP_SLOT;
    unresolved.undefinedSymbol = true;
    Expect(unresolved, ElfLoader::Result::SymbolNotFound, "an undefined strong symbol with no host stub is rejected");

    Blueprint weakUnresolved;
    weakUnresolved.relocationType = R_AARCH64_JUMP_SLOT;
    weakUnresolved.undefinedSymbol = true;
    weakUnresolved.weakSymbol = true;
    Expect(weakUnresolved, ElfLoader::Result::Ok, "an undefined weak symbol resolves to zero");

    Blueprint relocationOutside;
    relocationOutside.relocationType = R_AARCH64_RELATIVE;
    relocationOutside.relocationOffset = IMAGE_BYTES + 0x100000;
    Expect(relocationOutside, ElfLoader::Result::MalformedElf, "a relocation past the end of the module image is rejected");

    Blueprint relocationBelowArena;
    relocationBelowArena.relocationType = R_AARCH64_RELATIVE;
    relocationBelowArena.relocationOffset = 0ull - 0x100000000ull;
    Expect(relocationBelowArena, ElfLoader::Result::MalformedElf, "a relocation whose target wraps below the guest arena is rejected");

    Blueprint relocationIntoHostMemory;
    relocationIntoHostMemory.relocationType = R_AARCH64_RELATIVE;
    relocationIntoHostMemory.relocationOffset = 0xFFFF000000000000ull;
    Expect(relocationIntoHostMemory, ElfLoader::Result::MalformedElf, "a relocation targeting host memory outside the arena is rejected");

    Blueprint globalDataOutside;
    globalDataOutside.relocationType = R_AARCH64_GLOB_DAT;
    globalDataOutside.relocationOffset = 0ull - 0x100000000ull;
    Expect(globalDataOutside, ElfLoader::Result::MalformedElf, "a symbol relocation escaping the module image is rejected");

    Blueprint resolvedRelative;
    resolvedRelative.relocationType = R_AARCH64_RELATIVE;
    Expect(resolvedRelative, ElfLoader::Result::Ok, "a well-formed R_AARCH64_RELATIVE applies");

    printf("%s: %d checks failed\n", failures == 0 ? "elfloader-unit PASSED" : "elfloader-unit FAILED", failures);
    return failures == 0 ? 0 : 1;
}

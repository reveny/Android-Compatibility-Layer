#include "ElfLoader/ElfLoader.hpp"

#include "Platform/Platform.hpp"

#include <cstdio>
#include <cstring>
#include <elf.h>

static constexpr uint64_t DT_ANDROID_REL_TAG = 0x6000000full;
static constexpr uint64_t DT_ANDROID_RELSZ_TAG = 0x60000010ull;
static constexpr uint64_t DT_ANDROID_RELA_TAG = 0x60000011ull;
static constexpr uint64_t DT_ANDROID_RELASZ_TAG = 0x60000012ull;
static constexpr uint64_t DT_ANDROID_RELR_TAG = 0x6fffe000ull;
static constexpr uint64_t DT_ANDROID_RELRSZ_TAG = 0x6fffe001ull;
static constexpr uint64_t DT_ANDROID_RELRENT_TAG = 0x6fffe003ull;

static constexpr uint64_t STUB_ENTRY_BYTES = 16;
static constexpr uint64_t MAX_IMAGE_BYTES = 1ull << 31;
static constexpr uint16_t MAX_PROGRAM_HEADERS = 128;
static constexpr uint64_t MAX_DYNAMIC_ENTRIES = 4096;
static constexpr uint64_t MAX_DYNAMIC_SYMBOLS = 1u << 21;
static constexpr uint64_t MAX_RELOCATIONS = 1u << 24;

struct ElfDynamicInfo
{
    uint64_t stringTable = 0;
    uint64_t stringTableSize = 0;
    uint64_t symbolTable = 0;
    uint64_t symbolEntrySize = sizeof(Elf64_Sym);
    uint64_t gnuHash = 0;
    uint64_t rela = 0;
    uint64_t relaSize = 0;
    uint64_t relaEntrySize = sizeof(Elf64_Rela);
    uint64_t packedRela = 0;
    uint64_t packedRelaSize = 0;
    bool hasPackedRel = false;
    uint64_t jumpRelocations = 0;
    uint64_t pltRelocationSize = 0;
    uint64_t pltRelocationType = DT_RELA;
    uint64_t relr = 0;
    uint64_t relrSize = 0;
    uint64_t relrEntrySize = sizeof(uint64_t);
    uint64_t initAddress = 0;
    uint64_t initArray = 0;
    uint64_t initArraySize = 0;
    uint64_t preinitArray = 0;
    uint64_t preinitArraySize = 0;
    uint64_t finiAddress = 0;
    uint64_t finiArray = 0;
    uint64_t finiArraySize = 0;
    uint64_t sonameOffset = 0;
    bool hasSoname = false;
    bool hasGnuHash = false;
    std::vector<uint64_t> neededOffsets;
};

static uint64_t RoundDown(uint64_t value, uint64_t alignment)
{
    if (alignment <= 1) return value;
    return value - (value % alignment);
}

static uint64_t RoundUp(uint64_t value, uint64_t alignment)
{
    if (alignment <= 1) return value;
    uint64_t remainder = value % alignment;
    if (remainder == 0) return value;
    return value + (alignment - remainder);
}

static bool ReadImage(const uint8_t* data, uint64_t length, uint64_t offset, void* destination, uint64_t bytes)
{
    if (offset > length) return false;
    if (bytes > length - offset) return false;
    memcpy(destination, data + offset, (size_t)bytes);
    return true;
}

static void CollectStubRegions(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, ElfLoader::Library& library)
{
    if (header.e_shoff == 0 || header.e_shnum == 0 || header.e_shstrndx >= header.e_shnum) return;

    Elf64_Shdr names;
    if (!ReadImage(data, length, header.e_shoff + (uint64_t)header.e_shstrndx * header.e_shentsize, &names, sizeof(names))) return;
    if (names.sh_offset >= length) return;

    uint64_t nameLimit = length - names.sh_offset;
    if (names.sh_size < nameLimit) nameLimit = names.sh_size;

    for (uint16_t index = 0; index < header.e_shnum; ++index)
    {
        Elf64_Shdr section;
        if (!ReadImage(data, length, header.e_shoff + (uint64_t)index * header.e_shentsize, &section, sizeof(section))) continue;
        if ((section.sh_flags & SHF_EXECINSTR) == 0 || section.sh_size == 0 || section.sh_name >= nameLimit) continue;

        const char* name = (const char*)data + names.sh_offset + section.sh_name;
        if (strncmp(name, ".plt", 4) != 0 && strcmp(name, ".iplt") != 0) continue;

        ElfLoader::StubRegion region;
        region.guestAddress = (GuestState::GuestAddress)(library.loadBias + section.sh_addr);
        region.length = section.sh_size;
        region.entryBytes = section.sh_entsize != 0 ? section.sh_entsize : STUB_ENTRY_BYTES;

        library.stubRegions.push_back(region);
    }
}

static bool FileOffsetOf(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, uint64_t virtualAddress, uint64_t bytes,
                         uint64_t& fileOffset)
{
    for (uint16_t index = 0; index < header.e_phnum; ++index)
    {
        Elf64_Phdr program;
        uint64_t offset = header.e_phoff + (uint64_t)index * header.e_phentsize;
        if (!ReadImage(data, length, offset, &program, sizeof(program))) return false;
        if (program.p_type != PT_LOAD) continue;
        if (virtualAddress < program.p_vaddr) continue;

        uint64_t delta = virtualAddress - program.p_vaddr;
        if (delta >= program.p_filesz) continue;
        if (bytes > program.p_filesz - delta) continue;

        fileOffset = program.p_offset + delta;
        return fileOffset <= length && bytes <= length - fileOffset;
    }

    return false;
}

static const char* StringAt(const uint8_t* data, uint64_t length, uint64_t tableOffset, uint64_t tableSize, uint64_t index)
{
    if (index >= tableSize) return nullptr;
    if (tableOffset > length || tableSize > length - tableOffset) return nullptr;

    const char* start = (const char*)(data + tableOffset + index);
    uint64_t available = tableSize - index;
    if (memchr(start, '\0', (size_t)available) == nullptr) return nullptr;
    return start;
}

static bool WithinModule(const ElfLoader::Library& library, uint64_t address, uint64_t bytes)
{
    uint64_t base = (uint64_t)library.reservationAddress;
    if (library.reservationLength == 0) return false;
    if (address < base) return false;

    uint64_t offset = address - base;
    if (offset > library.reservationLength) return false;

    return bytes <= library.reservationLength - offset;
}

static bool ReadGuest64(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t& value)
{
    if (!memory.Read) return false;
    return memory.Read(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static bool WriteGuest64(const GuestState::MemoryAccessor& memory, uint64_t address, uint64_t value)
{
    if (!memory.Write) return false;
    return memory.Write(memory.context, (GuestState::GuestAddress)address, &value, sizeof(value));
}

static ElfLoader::Result ParseDynamic(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, uint64_t dynamicVirtualAddress,
                                      uint64_t dynamicSize, ElfDynamicInfo& info)
{
    uint64_t count = dynamicSize / sizeof(Elf64_Dyn);
    if (count == 0 || count > MAX_DYNAMIC_ENTRIES) return ElfLoader::Result::MalformedElf;

    uint64_t base = 0;
    if (!FileOffsetOf(data, length, header, dynamicVirtualAddress, dynamicSize, base)) return ElfLoader::Result::MalformedElf;

    for (uint64_t index = 0; index < count; ++index)
    {
        Elf64_Dyn entry;
        if (!ReadImage(data, length, base + index * sizeof(Elf64_Dyn), &entry, sizeof(entry))) return ElfLoader::Result::MalformedElf;
        if (entry.d_tag == DT_NULL) break;

        uint64_t value = entry.d_un.d_val;
        switch ((uint64_t)entry.d_tag)
        {
            case DT_STRTAB: info.stringTable = value; break;
            case DT_STRSZ: info.stringTableSize = value; break;
            case DT_SYMTAB: info.symbolTable = value; break;
            case DT_SYMENT: info.symbolEntrySize = value; break;
            case DT_GNU_HASH:
                info.gnuHash = value;
                info.hasGnuHash = true;
                break;
            case DT_RELA: info.rela = value; break;
            case DT_RELASZ: info.relaSize = value; break;
            case DT_RELAENT: info.relaEntrySize = value; break;
            case DT_ANDROID_RELA_TAG: info.packedRela = value; break;
            case DT_ANDROID_RELASZ_TAG: info.packedRelaSize = value; break;
            case DT_ANDROID_REL_TAG:
            case DT_ANDROID_RELSZ_TAG: info.hasPackedRel = true; break;
            case DT_JMPREL: info.jumpRelocations = value; break;
            case DT_PLTRELSZ: info.pltRelocationSize = value; break;
            case DT_PLTREL: info.pltRelocationType = value; break;
            case DT_RELR:
            case DT_ANDROID_RELR_TAG: info.relr = value; break;
            case DT_RELRSZ:
            case DT_ANDROID_RELRSZ_TAG: info.relrSize = value; break;
            case DT_RELRENT:
            case DT_ANDROID_RELRENT_TAG: info.relrEntrySize = value; break;
            case DT_INIT: info.initAddress = value; break;
            case DT_INIT_ARRAY: info.initArray = value; break;
            case DT_INIT_ARRAYSZ: info.initArraySize = value; break;
            case DT_PREINIT_ARRAY: info.preinitArray = value; break;
            case DT_PREINIT_ARRAYSZ: info.preinitArraySize = value; break;
            case DT_FINI: info.finiAddress = value; break;
            case DT_FINI_ARRAY: info.finiArray = value; break;
            case DT_FINI_ARRAYSZ: info.finiArraySize = value; break;
            case DT_SONAME:
                info.sonameOffset = value;
                info.hasSoname = true;
                break;
            case DT_NEEDED: info.neededOffsets.push_back(value); break;
            default: break;
        }
    }

    if (info.symbolEntrySize != sizeof(Elf64_Sym)) return ElfLoader::Result::MalformedElf;
    if (info.relaSize != 0 && info.relaEntrySize != sizeof(Elf64_Rela)) return ElfLoader::Result::MalformedElf;
    if (info.relrSize != 0 && info.relrEntrySize != sizeof(uint64_t)) return ElfLoader::Result::MalformedElf;
    if (info.pltRelocationSize != 0 && info.pltRelocationType != DT_RELA) return ElfLoader::Result::UnsupportedRelocation;

    return ElfLoader::Result::Ok;
}

static bool DeriveSymbolCount(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, uint64_t gnuHashAddress, uint64_t& symbolCount)
{
    uint64_t headerOffset = 0;
    if (!FileOffsetOf(data, length, header, gnuHashAddress, 16, headerOffset)) return false;

    uint32_t fields[4];
    if (!ReadImage(data, length, headerOffset, fields, sizeof(fields))) return false;

    uint32_t bucketCount = fields[0];
    uint32_t symbolOffset = fields[1];
    uint32_t bloomSize = fields[2];

    if (bucketCount == 0)
    {
        symbolCount = symbolOffset;
        return symbolOffset <= MAX_DYNAMIC_SYMBOLS;
    }

    if (bucketCount > MAX_DYNAMIC_SYMBOLS || bloomSize > MAX_DYNAMIC_SYMBOLS) return false;

    uint64_t bucketsAddress = gnuHashAddress + 16 + (uint64_t)bloomSize * sizeof(uint64_t);
    uint64_t bucketsOffset = 0;
    if (!FileOffsetOf(data, length, header, bucketsAddress, (uint64_t)bucketCount * sizeof(uint32_t), bucketsOffset)) return false;

    uint32_t last = symbolOffset;
    bool anyHashed = false;

    for (uint32_t bucket = 0; bucket < bucketCount; ++bucket)
    {
        uint32_t entry = 0;
        if (!ReadImage(data, length, bucketsOffset + (uint64_t)bucket * sizeof(uint32_t), &entry, sizeof(entry))) return false;
        if (entry == 0) continue;
        if (entry < symbolOffset) return false;

        anyHashed = true;
        if (entry > last) last = entry;
    }

    if (!anyHashed)
    {
        symbolCount = symbolOffset;
        return symbolOffset <= MAX_DYNAMIC_SYMBOLS;
    }

    if (last > MAX_DYNAMIC_SYMBOLS) return false;

    uint64_t chainAddress = bucketsAddress + (uint64_t)bucketCount * sizeof(uint32_t);
    for (;;)
    {
        uint64_t chainOffset = 0;
        uint64_t index = (uint64_t)last - symbolOffset;
        if (!FileOffsetOf(data, length, header, chainAddress + index * sizeof(uint32_t), sizeof(uint32_t), chainOffset)) return false;

        uint32_t word = 0;
        if (!ReadImage(data, length, chainOffset, &word, sizeof(word))) return false;
        if ((word & 1u) != 0) break;

        last += 1;
        if (last > MAX_DYNAMIC_SYMBOLS) return false;
    }

    symbolCount = (uint64_t)last + 1;
    return true;
}

static ElfLoader::Result CollectSymbols(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, const ElfDynamicInfo& info,
                                        uint64_t loadBias, std::vector<ElfLoader::Symbol>& symbols)
{
    if (!info.hasGnuHash) return ElfLoader::Result::MalformedElf;
    if (info.symbolTable == 0 || info.stringTable == 0) return ElfLoader::Result::MalformedElf;

    uint64_t symbolCount = 0;
    if (!DeriveSymbolCount(data, length, header, info.gnuHash, symbolCount)) return ElfLoader::Result::MalformedElf;

    uint64_t stringOffset = 0;
    if (!FileOffsetOf(data, length, header, info.stringTable, info.stringTableSize, stringOffset)) return ElfLoader::Result::MalformedElf;

    uint64_t tableOffset = 0;
    if (!FileOffsetOf(data, length, header, info.symbolTable, symbolCount * sizeof(Elf64_Sym), tableOffset)) return ElfLoader::Result::MalformedElf;

    symbols.clear();
    symbols.reserve((size_t)symbolCount);

    for (uint64_t index = 0; index < symbolCount; ++index)
    {
        Elf64_Sym entry;
        if (!ReadImage(data, length, tableOffset + index * sizeof(Elf64_Sym), &entry, sizeof(entry))) return ElfLoader::Result::MalformedElf;

        ElfLoader::Symbol symbol;
        const char* name = StringAt(data, length, stringOffset, info.stringTableSize, entry.st_name);
        if (!name) return ElfLoader::Result::MalformedElf;

        symbol.name = name;
        symbol.size = entry.st_size;
        symbol.isFunction = ELF64_ST_TYPE(entry.st_info) == STT_FUNC || ELF64_ST_TYPE(entry.st_info) == STT_GNU_IFUNC;
        symbol.isObject = ELF64_ST_TYPE(entry.st_info) == STT_OBJECT;
        symbol.isWeak = ELF64_ST_BIND(entry.st_info) == STB_WEAK;
        symbol.isLocal = ELF64_ST_BIND(entry.st_info) == STB_LOCAL;
        symbol.isDefined = entry.st_shndx != SHN_UNDEF;

        if (ELF64_ST_TYPE(entry.st_info) == STT_TLS)
            symbol.guestAddress = (GuestState::GuestAddress)entry.st_value;
        else if (symbol.isDefined)
            symbol.guestAddress = (GuestState::GuestAddress)(loadBias + entry.st_value);

        symbols.push_back(std::move(symbol));
    }

    return ElfLoader::Result::Ok;
}

static const ElfLoader::Symbol* FindDefinedSymbol(const ElfLoader::Library& library, const std::string& name)
{
    const ElfLoader::Symbol* weakMatch = nullptr;

    for (const ElfLoader::Symbol& symbol : library.exports)
    {
        if (!symbol.isDefined || symbol.isLocal) continue;
        if (symbol.name != name) continue;
        if (!symbol.isWeak) return &symbol;
        if (!weakMatch) weakMatch = &symbol;
    }

    return weakMatch;
}

static bool ResolveInGroup(const ElfLoader::Namespace& libraryNamespace, uint32_t rootModuleId, const std::string& name, uint64_t& address)
{
    std::vector<uint32_t> queue;
    std::vector<uint32_t> seen;
    queue.push_back(rootModuleId);

    for (size_t cursor = 0; cursor < queue.size(); ++cursor)
    {
        uint32_t moduleId = queue[cursor];

        bool alreadySeen = false;
        for (uint32_t entry : seen)
        {
            if (entry == moduleId) alreadySeen = true;
        }
        if (alreadySeen) continue;
        seen.push_back(moduleId);

        const ElfLoader::Library* library = ElfLoader::FindLibrary(libraryNamespace, moduleId);
        if (!library) continue;

        const ElfLoader::Symbol* symbol = FindDefinedSymbol(*library, name);
        if (symbol)
        {
            address = (uint64_t)symbol->guestAddress;
            return true;
        }

        for (uint32_t dependency : library->dependencyModuleIds) queue.push_back(dependency);
    }

    return false;
}

static ElfLoader::Result ApplyThreadLocalRelocation(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library, uint32_t type,
                                                    uint32_t symbolIndex, uint64_t target, int64_t addend)
{
    const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;
    const ElfLoader::Symbol& symbol = library.exports[symbolIndex];

    if (!library.threadLocalStorage.present) return ElfLoader::Result::UnsupportedRelocation;

    uint64_t withinBlock = (uint64_t)symbol.guestAddress + (uint64_t)addend;
    uint64_t fromThreadPointer = library.threadLocalStorage.staticOffsetFromThreadPointer + withinBlock;

    if (type == R_AARCH64_TLS_DTPMOD) return WriteGuest64(memory, target, library.moduleId) ? ElfLoader::Result::Ok
                                                                                              : ElfLoader::Result::MappingFailed;

    if (type == R_AARCH64_TLS_DTPREL) return WriteGuest64(memory, target, withinBlock) ? ElfLoader::Result::Ok
                                                                                         : ElfLoader::Result::MappingFailed;

    if (type == R_AARCH64_TLS_TPREL) return WriteGuest64(memory, target, fromThreadPointer) ? ElfLoader::Result::Ok
                                                                                              : ElfLoader::Result::MappingFailed;

    const ElfLoader::HostResolver& resolver = libraryNamespace.configuration.hostResolver;
    GuestState::GuestAddress stub{};

    if (!resolver.ResolveToGuestStub || !resolver.ResolveToGuestStub(resolver.context, ElfLoader::THREAD_LOCAL_RESOLVER_SYMBOL, stub))
    {
        fprintf(stderr, "[elfloader] %s: no %s trampoline for a TLSDESC relocation\n", library.soname.c_str(),
                ElfLoader::THREAD_LOCAL_RESOLVER_SYMBOL);
        return ElfLoader::Result::SymbolNotFound;
    }

    if (!WriteGuest64(memory, target, (uint64_t)stub)) return ElfLoader::Result::MappingFailed;
    if (!WriteGuest64(memory, target + sizeof(uint64_t), fromThreadPointer)) return ElfLoader::Result::MappingFailed;

    return ElfLoader::Result::Ok;
}

static ElfLoader::Result ApplyRelaEntries(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library,
                                          const std::vector<Elf64_Rela>& entries);

static ElfLoader::Result ApplyRelaTable(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library, const uint8_t* data,
                                        uint64_t length, const Elf64_Ehdr& header, const ElfDynamicInfo& info, uint64_t tableAddress,
                                        uint64_t tableSize)
{
    if (tableSize == 0) return ElfLoader::Result::Ok;

    uint64_t count = tableSize / sizeof(Elf64_Rela);
    if (count > MAX_RELOCATIONS) return ElfLoader::Result::MalformedElf;

    uint64_t tableOffset = 0;
    if (!FileOffsetOf(data, length, header, tableAddress, tableSize, tableOffset)) return ElfLoader::Result::MalformedElf;

    uint64_t stringOffset = 0;
    if (!FileOffsetOf(data, length, header, info.stringTable, info.stringTableSize, stringOffset)) return ElfLoader::Result::MalformedElf;

    std::vector<Elf64_Rela> entries((size_t)count);
    for (uint64_t index = 0; index < count; ++index)
    {
        if (!ReadImage(data, length, tableOffset + index * sizeof(Elf64_Rela), &entries[(size_t)index], sizeof(Elf64_Rela)))
            return ElfLoader::Result::MalformedElf;
    }

    return ApplyRelaEntries(libraryNamespace, library, entries);
}

static bool ReadSleb128(const uint8_t* data, uint64_t end, uint64_t& cursor, int64_t& value)
{
    uint64_t result = 0;
    uint32_t shift = 0;
    uint8_t byte = 0;

    do
    {
        if (cursor >= end || shift >= 64) return false;

        byte = data[cursor++];
        result |= (uint64_t)(byte & 0x7f) << shift;
        shift += 7;
    } while (byte & 0x80);

    if (shift < 64 && (byte & 0x40)) result |= ~0ull << shift;

    value = (int64_t)result;
    return true;
}

static constexpr int64_t PACKED_GROUPED_BY_INFO = 1;
static constexpr int64_t PACKED_GROUPED_BY_OFFSET_DELTA = 2;
static constexpr int64_t PACKED_GROUPED_BY_ADDEND = 4;
static constexpr int64_t PACKED_GROUP_HAS_ADDEND = 8;

static ElfLoader::Result DecodePackedRelocations(const uint8_t* data, uint64_t length, const Elf64_Ehdr& header, uint64_t tableAddress,
                                                 uint64_t tableSize, std::vector<Elf64_Rela>& entries)
{
    entries.clear();

    uint64_t tableOffset = 0;
    if (!FileOffsetOf(data, length, header, tableAddress, tableSize, tableOffset)) return ElfLoader::Result::MalformedElf;
    if (tableSize < 4 || memcmp(data + tableOffset, "APS2", 4) != 0) return ElfLoader::Result::UnsupportedRelocation;

    uint64_t cursor = tableOffset + 4;
    uint64_t end = tableOffset + tableSize;

    int64_t remaining = 0;
    int64_t offset = 0;
    if (!ReadSleb128(data, end, cursor, remaining) || !ReadSleb128(data, end, cursor, offset)) return ElfLoader::Result::MalformedElf;
    if (remaining < 0 || (uint64_t)remaining > MAX_RELOCATIONS) return ElfLoader::Result::MalformedElf;

    entries.reserve((size_t)remaining);

    int64_t info = 0;
    int64_t addend = 0;

    while (remaining > 0)
    {
        int64_t groupSize = 0;
        int64_t groupFlags = 0;
        if (!ReadSleb128(data, end, cursor, groupSize) || !ReadSleb128(data, end, cursor, groupFlags)) return ElfLoader::Result::MalformedElf;
        if (groupSize <= 0 || groupSize > remaining) return ElfLoader::Result::MalformedElf;

        bool byInfo = (groupFlags & PACKED_GROUPED_BY_INFO) != 0;
        bool byOffsetDelta = (groupFlags & PACKED_GROUPED_BY_OFFSET_DELTA) != 0;
        bool byAddend = (groupFlags & PACKED_GROUPED_BY_ADDEND) != 0;
        bool hasAddend = (groupFlags & PACKED_GROUP_HAS_ADDEND) != 0;

        int64_t offsetDelta = 0;
        if (byOffsetDelta && !ReadSleb128(data, end, cursor, offsetDelta)) return ElfLoader::Result::MalformedElf;
        if (byInfo && !ReadSleb128(data, end, cursor, info)) return ElfLoader::Result::MalformedElf;

        if (hasAddend && byAddend)
        {
            int64_t delta = 0;
            if (!ReadSleb128(data, end, cursor, delta)) return ElfLoader::Result::MalformedElf;
            addend += delta;
        }
        else if (!hasAddend)
        {
            addend = 0;
        }

        for (int64_t index = 0; index < groupSize; ++index)
        {
            int64_t step = offsetDelta;
            if (!byOffsetDelta && !ReadSleb128(data, end, cursor, step)) return ElfLoader::Result::MalformedElf;
            offset += step;

            if (!byInfo && !ReadSleb128(data, end, cursor, info)) return ElfLoader::Result::MalformedElf;

            if (hasAddend && !byAddend)
            {
                int64_t delta = 0;
                if (!ReadSleb128(data, end, cursor, delta)) return ElfLoader::Result::MalformedElf;
                addend += delta;
            }

            Elf64_Rela entry;
            entry.r_offset = (Elf64_Addr)offset;
            entry.r_info = (Elf64_Xword)info;
            entry.r_addend = (Elf64_Sxword)addend;
            entries.push_back(entry);
        }

        remaining -= groupSize;
    }

    return ElfLoader::Result::Ok;
}

static ElfLoader::Result ApplyPackedRelaTable(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library, const uint8_t* data,
                                              uint64_t length, const Elf64_Ehdr& header, uint64_t tableAddress, uint64_t tableSize)
{
    if (tableSize == 0) return ElfLoader::Result::Ok;

    std::vector<Elf64_Rela> entries;
    ElfLoader::Result decoded = DecodePackedRelocations(data, length, header, tableAddress, tableSize, entries);
    if (decoded != ElfLoader::Result::Ok)
    {
        fprintf(stderr, "[elfloader] %s: the packed relocation table could not be decoded: %s\n", library.soname.c_str(),
                ElfLoader::ResultName(decoded));
        return decoded;
    }

    library.relocationCounts.packed += entries.size();
    return ApplyRelaEntries(libraryNamespace, library, entries);
}

static ElfLoader::Result ApplyRelaEntries(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library,
                                          const std::vector<Elf64_Rela>& entries)
{
    const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;
    uint64_t count = entries.size();

    for (uint64_t index = 0; index < count; ++index)
    {
        const Elf64_Rela& entry = entries[(size_t)index];

        uint32_t type = (uint32_t)ELF64_R_TYPE(entry.r_info);
        uint32_t symbolIndex = (uint32_t)ELF64_R_SYM(entry.r_info);
        uint64_t target = library.loadBias + entry.r_offset;

        if (type == R_AARCH64_NONE) continue;

        uint64_t width = type == R_AARCH64_TLSDESC ? 2 * sizeof(uint64_t) : sizeof(uint64_t);
        if (!WithinModule(library, target, width))
        {
            fprintf(stderr, "[elfloader] %s: relocation %llu targets %016llx, outside the module image\n", library.soname.c_str(),
                    (unsigned long long)index, (unsigned long long)target);
            return ElfLoader::Result::MalformedElf;
        }

        if (type == R_AARCH64_RELATIVE)
        {
            if (!WriteGuest64(memory, target, library.loadBias + (uint64_t)entry.r_addend)) return ElfLoader::Result::MappingFailed;
            library.relocationCounts.relative += 1;
            continue;
        }

        bool threadLocal = type == R_AARCH64_TLS_DTPMOD || type == R_AARCH64_TLS_DTPREL || type == R_AARCH64_TLS_TPREL ||
                           type == R_AARCH64_TLSDESC;

        if (type != R_AARCH64_ABS64 && type != R_AARCH64_GLOB_DAT && type != R_AARCH64_JUMP_SLOT && !threadLocal)
            return ElfLoader::Result::UnsupportedRelocation;

        if (symbolIndex >= library.exports.size()) return ElfLoader::Result::MalformedElf;
        const ElfLoader::Symbol& symbol = library.exports[symbolIndex];

        if (threadLocal)
        {
            ElfLoader::Result applied = ApplyThreadLocalRelocation(libraryNamespace, library, type, symbolIndex, target, entry.r_addend);
            if (applied != ElfLoader::Result::Ok) return applied;

            library.relocationCounts.threadLocal += 1;
            continue;
        }

        uint64_t resolved = 0;

        if (symbol.isDefined)
        {
            resolved = (uint64_t)symbol.guestAddress;
        }
        else if (!ResolveInGroup(libraryNamespace, library.moduleId, symbol.name, resolved))
        {
            GuestState::GuestAddress stub{};
            const ElfLoader::HostResolver& resolver = libraryNamespace.configuration.hostResolver;

            if (symbol.isObject)
            {
                GuestState::GuestAddress object{};
                if (resolver.ResolveDataObject && resolver.ResolveDataObject(resolver.context, symbol.name.c_str(), object))
                {
                    resolved = (uint64_t)object;
                    library.relocationCounts.resolvedToHostData += 1;
                }
                else if (symbol.isWeak)
                {
                    resolved = 0;
                    library.relocationCounts.resolvedWeakToZero += 1;
                }
                else
                {
                    fprintf(stderr, "[elfloader] %s: unresolved data object %s. A host stub address is executable, not readable, so it "
                                    "must not be substituted here.\n",
                            library.soname.c_str(), symbol.name.c_str());
                    return ElfLoader::Result::UnresolvedDataObject;
                }
            }
            else if (symbol.isWeak)
            {
                if (resolver.LookupDefinedStub && resolver.LookupDefinedStub(resolver.context, symbol.name.c_str(), stub))
                {
                    resolved = (uint64_t)stub;
                    library.relocationCounts.resolvedToHostStub += 1;
                }
                else
                {
                    resolved = 0;
                    library.relocationCounts.resolvedWeakToZero += 1;

                    fprintf(stderr, "[elfloader] %s: the weak symbol %s is undefined here, so it reads as absent to the guest\n",
                            library.soname.c_str(), symbol.name.c_str());
                }
            }
            else if (resolver.ResolveToGuestStub && resolver.ResolveToGuestStub(resolver.context, symbol.name.c_str(), stub))
            {
                resolved = (uint64_t)stub;
                library.relocationCounts.resolvedToHostStub += 1;
            }
            else
            {
                fprintf(stderr, "[elfloader] %s: unresolved symbol %s\n", library.soname.c_str(), symbol.name.c_str());
                return ElfLoader::Result::SymbolNotFound;
            }
        }

        if (!WriteGuest64(memory, target, resolved + (uint64_t)entry.r_addend)) return ElfLoader::Result::MappingFailed;

        if (type == R_AARCH64_ABS64) library.relocationCounts.absolute += 1;
        if (type == R_AARCH64_GLOB_DAT) library.relocationCounts.globalData += 1;
        if (type == R_AARCH64_JUMP_SLOT) library.relocationCounts.jumpSlot += 1;
    }

    return ElfLoader::Result::Ok;
}

static ElfLoader::Result ApplyRelrTable(const ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library, const uint8_t* data,
                                        uint64_t length, const Elf64_Ehdr& header, uint64_t tableAddress, uint64_t tableSize)
{
    if (tableSize == 0) return ElfLoader::Result::Ok;

    uint64_t count = tableSize / sizeof(uint64_t);
    if (count > MAX_RELOCATIONS) return ElfLoader::Result::MalformedElf;

    uint64_t tableOffset = 0;
    if (!FileOffsetOf(data, length, header, tableAddress, tableSize, tableOffset)) return ElfLoader::Result::MalformedElf;

    const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;
    uint64_t cursor = 0;
    bool haveCursor = false;

    for (uint64_t index = 0; index < count; ++index)
    {
        uint64_t entry = 0;
        if (!ReadImage(data, length, tableOffset + index * sizeof(uint64_t), &entry, sizeof(entry))) return ElfLoader::Result::MalformedElf;

        if ((entry & 1ull) == 0)
        {
            cursor = library.loadBias + entry;
            haveCursor = true;

            if (!WithinModule(library, cursor, sizeof(uint64_t)))
            {
                fprintf(stderr, "[elfloader] %s: RELR entry targets %016llx, outside the module image\n", library.soname.c_str(),
                        (unsigned long long)cursor);
                return ElfLoader::Result::MalformedElf;
            }

            uint64_t existing = 0;
            if (!ReadGuest64(memory, cursor, existing)) return ElfLoader::Result::MalformedElf;
            if (!WriteGuest64(memory, cursor, existing + library.loadBias)) return ElfLoader::Result::MappingFailed;

            library.relocationCounts.relrRelative += 1;
            cursor += sizeof(uint64_t);
            continue;
        }

        if (!haveCursor) return ElfLoader::Result::MalformedElf;

        uint64_t bits = entry >> 1;
        for (uint64_t bit = 0; bit < 63; ++bit)
        {
            if ((bits & (1ull << bit)) == 0) continue;

            uint64_t target = cursor + bit * sizeof(uint64_t);
            if (!WithinModule(library, target, sizeof(uint64_t)))
            {
                fprintf(stderr, "[elfloader] %s: RELR bitmap targets %016llx, outside the module image\n", library.soname.c_str(),
                        (unsigned long long)target);
                return ElfLoader::Result::MalformedElf;
            }

            uint64_t existing = 0;
            if (!ReadGuest64(memory, target, existing)) return ElfLoader::Result::MalformedElf;
            if (!WriteGuest64(memory, target, existing + library.loadBias)) return ElfLoader::Result::MappingFailed;

            library.relocationCounts.relrRelative += 1;
        }

        cursor += 63 * sizeof(uint64_t);
    }

    return ElfLoader::Result::Ok;
}

static ElfLoader::Result MapSegments(ElfLoader::Namespace& libraryNamespace, ElfLoader::Library& library, const uint8_t* data, uint64_t length,
                                     const Elf64_Ehdr& header)
{
    uint64_t lowest = UINT64_MAX;
    uint64_t highest = 0;
    uint64_t maximumAlignment = Platform::GUEST_MIN_PAGE_BYTES;
    bool sawLoad = false;

    for (uint16_t index = 0; index < header.e_phnum; ++index)
    {
        Elf64_Phdr program;
        if (!ReadImage(data, length, header.e_phoff + (uint64_t)index * header.e_phentsize, &program, sizeof(program)))
            return ElfLoader::Result::MalformedElf;
        if (program.p_type != PT_LOAD) continue;
        if (program.p_filesz > program.p_memsz) return ElfLoader::Result::MalformedElf;
        if (program.p_offset > length || program.p_filesz > length - program.p_offset) return ElfLoader::Result::MalformedElf;

        if (program.p_align != 0 && (program.p_align & (program.p_align - 1)) != 0) return ElfLoader::Result::MalformedElf;
        if (program.p_memsz > MAX_IMAGE_BYTES) return ElfLoader::Result::MalformedElf;
        if (program.p_vaddr > MAX_IMAGE_BYTES - program.p_memsz) return ElfLoader::Result::MalformedElf;

        sawLoad = true;
        if (program.p_vaddr < lowest) lowest = program.p_vaddr;
        if (program.p_vaddr + program.p_memsz > highest) highest = program.p_vaddr + program.p_memsz;
        if (program.p_align > maximumAlignment) maximumAlignment = program.p_align;
    }

    if (!sawLoad) return ElfLoader::Result::MalformedElf;
    if (maximumAlignment > (1ull << 24)) return ElfLoader::Result::MalformedElf;

    lowest = RoundDown(lowest, maximumAlignment);
    highest = RoundUp(highest, maximumAlignment);

    if (highest < lowest) return ElfLoader::Result::MalformedElf;

    uint64_t span = highest - lowest;
    if (span == 0 || span > MAX_IMAGE_BYTES) return ElfLoader::Result::MalformedElf;
    const ElfLoader::AddressSpace& addressSpace = libraryNamespace.configuration.addressSpace;
    if (!addressSpace.Reserve || !addressSpace.Commit || !addressSpace.Protect) return ElfLoader::Result::MappingFailed;

    GuestState::GuestAddress base{};
    const bool placed = addressSpace.ReserveForImage && !library.soname.empty()
                            ? addressSpace.ReserveForImage(addressSpace.context, library.soname.c_str(), span, maximumAlignment, base)
                            : addressSpace.Reserve(addressSpace.context, span, maximumAlignment, base);

    if (!placed) return ElfLoader::Result::MappingFailed;

    library.reservationAddress = base;
    library.reservationLength = span;
    library.loadBias = (uint64_t)base - lowest;

    uint64_t hostPage = Platform::HostPageBytes();

    for (uint16_t index = 0; index < header.e_phnum; ++index)
    {
        Elf64_Phdr program;
        if (!ReadImage(data, length, header.e_phoff + (uint64_t)index * header.e_phentsize, &program, sizeof(program)))
            return ElfLoader::Result::MalformedElf;
        if (program.p_type != PT_LOAD) continue;

        uint64_t start = RoundDown(library.loadBias + program.p_vaddr, hostPage);
        uint64_t end = RoundUp(library.loadBias + program.p_vaddr + program.p_memsz, hostPage);
        if (end <= start) continue;

        if (!addressSpace.Commit(addressSpace.context, (GuestState::GuestAddress)start, end - start, true, true, false))
            return ElfLoader::Result::MappingFailed;

        if (program.p_filesz != 0)
        {
            const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;
            if (!memory.Write) return ElfLoader::Result::MappingFailed;
            if (!memory.Write(memory.context, (GuestState::GuestAddress)(library.loadBias + program.p_vaddr), data + program.p_offset,
                              program.p_filesz))
                return ElfLoader::Result::MappingFailed;
        }

        for (const ElfLoader::Segment& existing : library.segments)
        {
            uint64_t existingStart = (uint64_t)existing.guestAddress;
            if (start < existingStart + existing.length && existingStart < end) return ElfLoader::Result::MalformedElf;
        }

        ElfLoader::Segment segment;
        segment.guestAddress = (GuestState::GuestAddress)start;
        segment.length = end - start;
        segment.readable = (program.p_flags & PF_R) != 0;
        segment.writable = (program.p_flags & PF_W) != 0;
        segment.executable = (program.p_flags & PF_X) != 0;
        library.segments.push_back(segment);
    }

    return ElfLoader::Result::Ok;
}

static ElfLoader::Result FinalizeProtection(const ElfLoader::Namespace& libraryNamespace, const ElfLoader::Library& library,
                                            const uint8_t* data, uint64_t length, const Elf64_Ehdr& header)
{
    const ElfLoader::AddressSpace& addressSpace = libraryNamespace.configuration.addressSpace;

    for (const ElfLoader::Segment& segment : library.segments)
    {
        if (!addressSpace.Protect(addressSpace.context, segment.guestAddress, segment.length, segment.readable, segment.writable,
                                  segment.executable))
            return ElfLoader::Result::MappingFailed;
    }

    uint64_t hostPage = Platform::HostPageBytes();

    for (uint16_t index = 0; index < header.e_phnum; ++index)
    {
        Elf64_Phdr program;
        if (!ReadImage(data, length, header.e_phoff + (uint64_t)index * header.e_phentsize, &program, sizeof(program)))
            return ElfLoader::Result::MalformedElf;
        if (program.p_type != PT_GNU_RELRO) continue;

        uint64_t start = RoundDown(library.loadBias + program.p_vaddr, hostPage);
        uint64_t end = RoundDown(library.loadBias + program.p_vaddr + program.p_memsz, hostPage);
        if (end <= start) continue;

        if (!addressSpace.Protect(addressSpace.context, (GuestState::GuestAddress)start, end - start, true, false, false))
            return ElfLoader::Result::MappingFailed;
    }

    return ElfLoader::Result::Ok;
}

static bool EnterAt(const ElfLoader::Library& library, const GuestState::GuestCaller& caller, uint64_t address)
{
    if (!WithinModule(library, address, ElfLoader::MINIMUM_ENTRY_BYTES))
    {
        fprintf(stderr, "[elfloader] %s: an initializer entry points at %016llx, outside the module image\n", library.soname.c_str(),
                (unsigned long long)address);
        return false;
    }

    GuestState::Registers registers;
    GuestState::Reset(registers);
    return caller.EnterGuest(caller.context, (GuestState::GuestAddress)address, registers);
}

static bool RunInitializerArray(const ElfLoader::Library& library, const GuestState::MemoryAccessor& memory,
                                const GuestState::GuestCaller& caller, uint64_t arrayAddress, uint64_t count)
{
    if (count != 0 && !WithinModule(library, arrayAddress, count * sizeof(uint64_t))) return false;

    for (uint64_t index = 0; index < count; ++index)
    {
        uint64_t entry = 0;
        if (!ReadGuest64(memory, arrayAddress + index * sizeof(uint64_t), entry)) return false;
        if (entry == 0 || entry == UINT64_MAX) continue;
        if (!EnterAt(library, caller, entry)) return false;
    }

    return true;
}

static ElfLoader::Result LoadImage(ElfLoader::Namespace& libraryNamespace, const uint8_t* data, uint64_t length, const std::string& fallbackName,
                                   const std::string& path, uint32_t& moduleId)
{
    if (libraryNamespace.loaded.size() >= ElfLoader::MAX_MODULES) return ElfLoader::Result::TooManyModules;

    Elf64_Ehdr header;
    if (!ReadImage(data, length, 0, &header, sizeof(header))) return ElfLoader::Result::NotAnElf;
    if (memcmp(header.e_ident, ELFMAG, SELFMAG) != 0) return ElfLoader::Result::NotAnElf;
    if (header.e_ident[EI_CLASS] != ELFCLASS64) return ElfLoader::Result::WrongClass;
    if (header.e_ident[EI_DATA] != ELFDATA2LSB) return ElfLoader::Result::WrongClass;
    if (header.e_machine != EM_AARCH64) return ElfLoader::Result::WrongMachine;
    if (header.e_type != ET_DYN) return ElfLoader::Result::NotAnElf;
    if (header.e_phentsize != sizeof(Elf64_Phdr)) return ElfLoader::Result::MalformedElf;
    if (header.e_phnum == 0 || header.e_phnum > MAX_PROGRAM_HEADERS) return ElfLoader::Result::MalformedElf;

    ElfLoader::Library library;
    library.moduleId = libraryNamespace.nextModuleId;
    library.soname = fallbackName;
    library.path = path;

    ElfLoader::Result result = MapSegments(libraryNamespace, library, data, length, header);
    if (result != ElfLoader::Result::Ok) return result;

    uint64_t dynamicVirtualAddress = 0;
    uint64_t dynamicSize = 0;

    for (uint16_t index = 0; index < header.e_phnum; ++index)
    {
        Elf64_Phdr program;
        if (!ReadImage(data, length, header.e_phoff + (uint64_t)index * header.e_phentsize, &program, sizeof(program)))
            return ElfLoader::Result::MalformedElf;

        if (program.p_type == PT_DYNAMIC)
        {
            dynamicVirtualAddress = program.p_vaddr;
            dynamicSize = program.p_memsz;
        }
        else if (program.p_type == PT_TLS)
        {
            uint64_t alignment = program.p_align == 0 ? 1 : program.p_align;

            library.threadLocalStorage.present = true;
            library.threadLocalStorage.imageAddress = (GuestState::GuestAddress)(library.loadBias + program.p_vaddr);
            library.threadLocalStorage.imageSize = program.p_filesz;
            library.threadLocalStorage.blockSize = program.p_memsz;
            library.threadLocalStorage.alignment = alignment;
            library.threadLocalStorage.moduleId = library.moduleId;
            library.threadLocalStorage.staticOffsetFromThreadPointer = RoundUp(libraryNamespace.staticTlsBytes, alignment);

            libraryNamespace.staticTlsBytes = library.threadLocalStorage.staticOffsetFromThreadPointer + program.p_memsz;
        }
        else if (program.p_type == PT_GNU_EH_FRAME)
        {
            library.ehFrameAddress = (GuestState::GuestAddress)(library.loadBias + program.p_vaddr);
            library.ehFrameLength = program.p_memsz;
        }
        else if (program.p_type == PT_PHDR)
        {
            library.programHeaderAddress = (GuestState::GuestAddress)(library.loadBias + program.p_vaddr);
            library.programHeaderCount = header.e_phnum;
        }
    }

    CollectStubRegions(data, length, header, library);

    if (library.programHeaderCount == 0)
    {
        for (const ElfLoader::Segment& segment : library.segments)
        {
            uint64_t start = (uint64_t)segment.guestAddress;
            uint64_t wanted = library.loadBias + header.e_phoff;
            uint64_t span = (uint64_t)header.e_phnum * sizeof(Elf64_Phdr);

            if (wanted < start || wanted + span > start + segment.length) continue;

            library.programHeaderAddress = (GuestState::GuestAddress)wanted;
            library.programHeaderCount = header.e_phnum;
            break;
        }
    }

    if (dynamicSize == 0) return ElfLoader::Result::MalformedElf;

    ElfDynamicInfo info;
    result = ParseDynamic(data, length, header, dynamicVirtualAddress, dynamicSize, info);
    if (result != ElfLoader::Result::Ok) return result;

    uint64_t stringOffset = 0;
    if (!FileOffsetOf(data, length, header, info.stringTable, info.stringTableSize, stringOffset)) return ElfLoader::Result::MalformedElf;

    if (info.hasSoname)
    {
        const char* soname = StringAt(data, length, stringOffset, info.stringTableSize, info.sonameOffset);
        if (!soname) return ElfLoader::Result::MalformedElf;
        library.soname = soname;
    }

    for (uint64_t offset : info.neededOffsets)
    {
        const char* needed = StringAt(data, length, stringOffset, info.stringTableSize, offset);
        if (!needed) return ElfLoader::Result::MalformedElf;
        library.dependencies.push_back(needed);
    }

    result = CollectSymbols(data, length, header, info, library.loadBias, library.exports);
    if (result != ElfLoader::Result::Ok) return result;

    library.preinitArrayAddress = (GuestState::GuestAddress)(info.preinitArray == 0 ? 0 : library.loadBias + info.preinitArray);
    library.preinitArrayCount = info.preinitArraySize / sizeof(uint64_t);
    library.initAddress = (GuestState::GuestAddress)(info.initAddress == 0 ? 0 : library.loadBias + info.initAddress);
    library.initArrayAddress = (GuestState::GuestAddress)(info.initArray == 0 ? 0 : library.loadBias + info.initArray);
    library.initArrayCount = info.initArraySize / sizeof(uint64_t);
    library.finiAddress = (GuestState::GuestAddress)(info.finiAddress == 0 ? 0 : library.loadBias + info.finiAddress);
    library.finiArrayAddress = (GuestState::GuestAddress)(info.finiArray == 0 ? 0 : library.loadBias + info.finiArray);
    library.finiArrayCount = info.finiArraySize / sizeof(uint64_t);

    moduleId = library.moduleId;
    libraryNamespace.nextModuleId += 1;
    libraryNamespace.loaded.push_back(std::move(library));

    const ElfLoader::HostResolver& resolver = libraryNamespace.configuration.hostResolver;
    std::string owner = libraryNamespace.loaded.back().soname;
    std::vector<std::string> pending = libraryNamespace.loaded.back().dependencies;
    std::vector<uint32_t> dependencyIds;

    for (const std::string& dependency : pending)
    {
        if (resolver.IsProxyLibrary && resolver.IsProxyLibrary(resolver.context, dependency.c_str())) continue;

        uint32_t dependencyId = ElfLoader::INVALID_MODULE_ID;
        ElfLoader::Result dependencyResult = ElfLoader::LoadLibrary(libraryNamespace, dependency, dependencyId);
        if (dependencyResult != ElfLoader::Result::Ok)
        {
            fprintf(stderr, "[elfloader] %s: cannot satisfy DT_NEEDED %s (%s)\n", owner.c_str(), dependency.c_str(),
                    ElfLoader::ResultName(dependencyResult));
            return ElfLoader::Result::DependencyNotFound;
        }
        dependencyIds.push_back(dependencyId);
    }

    ElfLoader::Library* self = ElfLoader::FindLibrary(libraryNamespace, moduleId);
    if (!self) return ElfLoader::Result::UnknownModule;
    self->dependencyModuleIds = dependencyIds;

    result = ApplyRelaTable(libraryNamespace, *self, data, length, header, info, info.rela, info.relaSize);
    if (result != ElfLoader::Result::Ok) return result;

    self = ElfLoader::FindLibrary(libraryNamespace, moduleId);
    if (!self) return ElfLoader::Result::UnknownModule;

    if (info.hasPackedRel)
    {
        fprintf(stderr, "[elfloader] %s uses DT_ANDROID_REL, which arm64 never emits\n", self->soname.c_str());
        return ElfLoader::Result::UnsupportedRelocation;
    }

    result = ApplyPackedRelaTable(libraryNamespace, *self, data, length, header, info.packedRela, info.packedRelaSize);
    if (result != ElfLoader::Result::Ok) return result;

    self = ElfLoader::FindLibrary(libraryNamespace, moduleId);
    if (!self) return ElfLoader::Result::UnknownModule;

    result = ApplyRelaTable(libraryNamespace, *self, data, length, header, info, info.jumpRelocations, info.pltRelocationSize);
    if (result != ElfLoader::Result::Ok) return result;

    self = ElfLoader::FindLibrary(libraryNamespace, moduleId);
    if (!self) return ElfLoader::Result::UnknownModule;

    result = ApplyRelrTable(libraryNamespace, *self, data, length, header, info.relr, info.relrSize);
    if (result != ElfLoader::Result::Ok) return result;

    return FinalizeProtection(libraryNamespace, *self, data, length, header);
}

ElfLoader::Result ElfLoader::CreateNamespace(const Configuration& configuration, Namespace& libraryNamespace)
{
    libraryNamespace.configuration = configuration;
    libraryNamespace.loaded.clear();
    libraryNamespace.loaded.reserve(MAX_MODULES);
    libraryNamespace.nextModuleId = 0;

    if (!configuration.addressSpace.Reserve) return Result::MappingFailed;
    if (!configuration.memory.Read || !configuration.memory.Write) return Result::MappingFailed;

    return Result::Ok;
}

void ElfLoader::DestroyNamespace(Namespace& libraryNamespace)
{
    const AddressSpace& addressSpace = libraryNamespace.configuration.addressSpace;

    for (Library& library : libraryNamespace.loaded)
    {
        if (library.reservationLength == 0) continue;
        if (addressSpace.Release) addressSpace.Release(addressSpace.context, library.reservationAddress, library.reservationLength);
    }

    libraryNamespace.loaded.clear();
    libraryNamespace.nextModuleId = 0;
}

ElfLoader::Library* ElfLoader::FindLibrary(Namespace& libraryNamespace, uint32_t moduleId)
{
    for (Library& library : libraryNamespace.loaded)
    {
        if (library.moduleId == moduleId) return &library;
    }
    return nullptr;
}

const ElfLoader::Library* ElfLoader::FindLibrary(const Namespace& libraryNamespace, uint32_t moduleId)
{
    for (const Library& library : libraryNamespace.loaded)
    {
        if (library.moduleId == moduleId) return &library;
    }
    return nullptr;
}

ElfLoader::Result ElfLoader::LoadLibrary(Namespace& libraryNamespace, const std::string& sonameOrPath, uint32_t& moduleId)
{
    std::string leaf = sonameOrPath;
    size_t slash = leaf.find_last_of('/');
    if (slash != std::string::npos) leaf = leaf.substr(slash + 1);

    for (const Library& library : libraryNamespace.loaded)
    {
        if (library.soname == leaf || library.path == sonameOrPath)
        {
            moduleId = library.moduleId;
            return Result::Ok;
        }
    }

    std::vector<std::string> candidates;
    if (sonameOrPath.find('/') != std::string::npos) candidates.push_back(sonameOrPath);
    for (const std::string& directory : libraryNamespace.configuration.searchPaths) candidates.push_back(directory + "/" + leaf);

    for (const std::string& candidate : candidates)
    {
        FILE* file = fopen(candidate.c_str(), "rb");
        if (!file) continue;

        if (fseek(file, 0, SEEK_END) != 0)
        {
            fclose(file);
            continue;
        }

        long size = ftell(file);
        if (size <= 0 || (uint64_t)size > MAX_IMAGE_BYTES)
        {
            fclose(file);
            return Result::MalformedElf;
        }

        std::vector<uint8_t> image((size_t)size);
        rewind(file);
        size_t read = fread(image.data(), 1, image.size(), file);
        fclose(file);

        if (read != image.size()) return Result::MalformedElf;

        return LoadImage(libraryNamespace, image.data(), image.size(), leaf, candidate, moduleId);
    }

    return Result::FileNotFound;
}

ElfLoader::Result ElfLoader::LoadFromMemory(Namespace& libraryNamespace, const void* data, uint64_t length, const std::string& soname,
                                            uint32_t& moduleId)
{
    if (!data || length == 0 || length > MAX_IMAGE_BYTES) return Result::MalformedElf;

    for (const Library& library : libraryNamespace.loaded)
    {
        if (library.soname == soname)
        {
            moduleId = library.moduleId;
            return Result::Ok;
        }
    }

    return LoadImage(libraryNamespace, (const uint8_t*)data, length, soname, soname, moduleId);
}

ElfLoader::Result ElfLoader::UnloadLibrary(Namespace& libraryNamespace, uint32_t moduleId)
{
    for (size_t index = 0; index < libraryNamespace.loaded.size(); ++index)
    {
        if (libraryNamespace.loaded[index].moduleId != moduleId) continue;

        const AddressSpace& addressSpace = libraryNamespace.configuration.addressSpace;
        Library& library = libraryNamespace.loaded[index];

        if (library.reservationLength != 0 && addressSpace.Release)
            addressSpace.Release(addressSpace.context, library.reservationAddress, library.reservationLength);

        libraryNamespace.loaded.erase(libraryNamespace.loaded.begin() + (long)index);
        return Result::Ok;
    }

    return Result::UnknownModule;
}

ElfLoader::Result ElfLoader::RunInitializers(Namespace& libraryNamespace, uint32_t moduleId, const GuestState::GuestCaller& caller)
{
    Library* library = FindLibrary(libraryNamespace, moduleId);
    if (!library) return Result::UnknownModule;
    if (library->initializersRun) return Result::Ok;

    library->initializersRun = true;

    std::vector<uint32_t> dependencies = library->dependencyModuleIds;
    for (uint32_t dependency : dependencies)
    {
        Result result = RunInitializers(libraryNamespace, dependency, caller);
        if (result != Result::Ok) return result;
    }

    library = FindLibrary(libraryNamespace, moduleId);
    if (!library) return Result::UnknownModule;

    if (!caller.EnterGuest) return Result::InitializerFaulted;

    const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;

    if (!RunInitializerArray(*library, memory, caller, (uint64_t)library->preinitArrayAddress, library->preinitArrayCount))
        return Result::InitializerFaulted;

    if ((uint64_t)library->initAddress != 0 && !EnterAt(*library, caller, (uint64_t)library->initAddress))
        return Result::InitializerFaulted;

    if (!RunInitializerArray(*library, memory, caller, (uint64_t)library->initArrayAddress, library->initArrayCount))
        return Result::InitializerFaulted;

    return Result::Ok;
}

ElfLoader::Result ElfLoader::RunFinalizers(Namespace& libraryNamespace, uint32_t moduleId, const GuestState::GuestCaller& caller)
{
    Library* library = FindLibrary(libraryNamespace, moduleId);
    if (!library) return Result::UnknownModule;
    if (!caller.EnterGuest) return Result::InitializerFaulted;

    const GuestState::MemoryAccessor& memory = libraryNamespace.configuration.memory;
    uint64_t arrayAddress = (uint64_t)library->finiArrayAddress;
    uint64_t count = library->finiArrayCount;
    uint64_t directFini = (uint64_t)library->finiAddress;

    if (count != 0 && !WithinModule(*library, arrayAddress, count * sizeof(uint64_t))) return Result::InitializerFaulted;

    for (uint64_t index = count; index > 0; --index)
    {
        uint64_t entry = 0;
        if (!ReadGuest64(memory, arrayAddress + (index - 1) * sizeof(uint64_t), entry)) return Result::InitializerFaulted;
        if (entry == 0 || entry == UINT64_MAX) continue;
        if (!EnterAt(*library, caller, entry)) return Result::InitializerFaulted;
    }

    if (directFini != 0 && !EnterAt(*library, caller, directFini)) return Result::InitializerFaulted;

    return Result::Ok;
}

ElfLoader::Result ElfLoader::LookupExport(const Namespace& libraryNamespace, const std::string& name, GuestState::GuestAddress& address)
{
    for (const Library& library : libraryNamespace.loaded)
    {
        const Symbol* symbol = FindDefinedSymbol(library, name);
        if (!symbol) continue;

        address = symbol->guestAddress;
        return Result::Ok;
    }

    return Result::SymbolNotFound;
}

ElfLoader::Result ElfLoader::LookupExportIn(const Namespace& libraryNamespace, uint32_t moduleId, const std::string& name,
                                            GuestState::GuestAddress& address)
{
    const Library* library = FindLibrary(libraryNamespace, moduleId);
    if (!library) return Result::UnknownModule;

    const Symbol* symbol = FindDefinedSymbol(*library, name);
    if (!symbol) return Result::SymbolNotFound;

    address = symbol->guestAddress;
    return Result::Ok;
}

const char* ElfLoader::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::FileNotFound: return "FileNotFound";
        case Result::NotAnElf: return "NotAnElf";
        case Result::WrongMachine: return "WrongMachine";
        case Result::WrongClass: return "WrongClass";
        case Result::MalformedElf: return "MalformedElf";
        case Result::UnsupportedRelocation: return "UnsupportedRelocation";
        case Result::DependencyNotFound: return "DependencyNotFound";
        case Result::SymbolNotFound: return "SymbolNotFound";
        case Result::MappingFailed: return "MappingFailed";
        case Result::InitializerFaulted: return "InitializerFaulted";
        case Result::TooManyModules: return "TooManyModules";
        case Result::UnknownModule: return "UnknownModule";
        case Result::UnresolvedDataObject: return "UnresolvedDataObject";
    }

    return "Unknown";
}

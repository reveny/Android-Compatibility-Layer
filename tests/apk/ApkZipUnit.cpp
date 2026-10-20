#include <cstdio>
#include <cstdlib>
#include <dirent.h>
#include <cstring>
#include <string>
#include <sys/stat.h>
#include <unistd.h>
#include <vector>
#include <zlib.h>

#include "Apk/Apk.hpp"

static int gFailures = 0;
static std::string gWorkDirectory;

static void Check(bool condition, const char* what)
{
    printf("%s %s\n", condition ? "PASS" : "FAIL", what);
    if (!condition) gFailures += 1;
}

static void CheckResult(Apk::Result actual, Apk::Result expected, const char* what)
{
    bool matched = actual == expected;
    printf("%s %s (%s)\n", matched ? "PASS" : "FAIL", what, Apk::ResultName(actual));
    if (!matched) gFailures += 1;
}

struct ZipEntrySource
{
    std::string name;
    std::vector<uint8_t> stored;
    uint32_t crc = 0;
    uint32_t uncompressedSize = 0;
    uint16_t method = 0;
    uint32_t localHeaderOffset = 0;
};

static void PutShort(std::vector<uint8_t>& bytes, uint16_t value)
{
    bytes.push_back((uint8_t)(value & 0xff));
    bytes.push_back((uint8_t)((value >> 8) & 0xff));
}

static void PutLong(std::vector<uint8_t>& bytes, uint32_t value)
{
    bytes.push_back((uint8_t)(value & 0xff));
    bytes.push_back((uint8_t)((value >> 8) & 0xff));
    bytes.push_back((uint8_t)((value >> 16) & 0xff));
    bytes.push_back((uint8_t)((value >> 24) & 0xff));
}

static std::vector<uint8_t> Deflate(const std::vector<uint8_t>& raw)
{
    z_stream stream;
    memset(&stream, 0, sizeof(stream));
    deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED, -MAX_WBITS, 8, Z_DEFAULT_STRATEGY);

    std::vector<uint8_t> out(raw.size() + 128);
    stream.next_in = const_cast<uint8_t*>(raw.data());
    stream.avail_in = (unsigned int)raw.size();
    stream.next_out = out.data();
    stream.avail_out = (unsigned int)out.size();

    deflate(&stream, Z_FINISH);
    out.resize(out.size() - stream.avail_out);
    deflateEnd(&stream);

    return out;
}

static ZipEntrySource MakeEntry(const std::string& name, const std::string& text, bool compress)
{
    std::vector<uint8_t> raw(text.begin(), text.end());

    ZipEntrySource entry;
    entry.name = name;
    entry.crc = (uint32_t)crc32(crc32(0, nullptr, 0), raw.data(), (unsigned int)raw.size());
    entry.uncompressedSize = (uint32_t)raw.size();
    entry.method = compress ? 8 : 0;
    entry.stored = compress ? Deflate(raw) : raw;

    return entry;
}

static std::vector<uint8_t> BuildArchive(std::vector<ZipEntrySource>& entries)
{
    std::vector<uint8_t> bytes;

    for (ZipEntrySource& entry : entries)
    {
        entry.localHeaderOffset = (uint32_t)bytes.size();

        PutLong(bytes, 0x04034b50);
        PutShort(bytes, 20);
        PutShort(bytes, 0);
        PutShort(bytes, entry.method);
        PutShort(bytes, 0);
        PutShort(bytes, 0);
        PutLong(bytes, entry.crc);
        PutLong(bytes, (uint32_t)entry.stored.size());
        PutLong(bytes, entry.uncompressedSize);
        PutShort(bytes, (uint16_t)entry.name.size());
        PutShort(bytes, 0);
        bytes.insert(bytes.end(), entry.name.begin(), entry.name.end());
        bytes.insert(bytes.end(), entry.stored.begin(), entry.stored.end());
    }

    uint32_t directoryOffset = (uint32_t)bytes.size();

    for (const ZipEntrySource& entry : entries)
    {
        PutLong(bytes, 0x02014b50);
        PutShort(bytes, 20);
        PutShort(bytes, 20);
        PutShort(bytes, 0);
        PutShort(bytes, entry.method);
        PutShort(bytes, 0);
        PutShort(bytes, 0);
        PutLong(bytes, entry.crc);
        PutLong(bytes, (uint32_t)entry.stored.size());
        PutLong(bytes, entry.uncompressedSize);
        PutShort(bytes, (uint16_t)entry.name.size());
        PutShort(bytes, 0);
        PutShort(bytes, 0);
        PutShort(bytes, 0);
        PutShort(bytes, 0);
        PutLong(bytes, 0);
        PutLong(bytes, entry.localHeaderOffset);
        bytes.insert(bytes.end(), entry.name.begin(), entry.name.end());
    }

    uint32_t directorySize = (uint32_t)bytes.size() - directoryOffset;

    PutLong(bytes, 0x06054b50);
    PutShort(bytes, 0);
    PutShort(bytes, 0);
    PutShort(bytes, (uint16_t)entries.size());
    PutShort(bytes, (uint16_t)entries.size());
    PutLong(bytes, directorySize);
    PutLong(bytes, directoryOffset);
    PutShort(bytes, 0);

    return bytes;
}

static std::string WriteArchive(const char* leaf, const std::vector<uint8_t>& bytes)
{
    std::string path = gWorkDirectory + "/" + leaf;

    FILE* file = fopen(path.c_str(), "wb");
    if (!file) return std::string();

    fwrite(bytes.data(), 1, bytes.size(), file);
    fclose(file);

    return path;
}

static Apk::Result EnumerateOf(const char* leaf, std::vector<ZipEntrySource> entries, Apk::Package& package)
{
    std::vector<uint8_t> bytes = BuildArchive(entries);
    package.path = WriteArchive(leaf, bytes);
    if (package.path.empty()) return Apk::Result::FileNotFound;

    return Apk::EnumerateArchive(package);
}

static void TestSafeNames()
{
    Check(Apk::IsSafeEntryName("lib/x86_64/libacl.so"), "a normal entry name is accepted");
    Check(Apk::IsSafeEntryName("classes.dex"), "a root entry name is accepted");
    Check(!Apk::IsSafeEntryName("../evil.so"), "a leading parent segment is rejected");
    Check(!Apk::IsSafeEntryName("lib/../../evil.so"), "an interior parent segment is rejected");
    Check(!Apk::IsSafeEntryName("/etc/passwd"), "an absolute name is rejected");
    Check(!Apk::IsSafeEntryName("lib\\x86_64\\libacl.so"), "a backslash separator is rejected");
    Check(!Apk::IsSafeEntryName("lib//libacl.so"), "an empty segment is rejected");
    Check(!Apk::IsSafeEntryName("./libacl.so"), "a current-directory segment is rejected");
    Check(!Apk::IsSafeEntryName(""), "an empty name is rejected");
    Check(!Apk::IsSafeEntryName(std::string(Apk::MAX_ENTRY_NAME_LENGTH + 1, 'a')), "an over-long name is rejected");
    Check(Apk::IsSafeEntryName("assets/..hidden"), "a leading-dot leaf is not mistaken for a parent segment");
    Check(!Apk::IsSafeEntryName(std::string("lib/") + '\0' + "evil.so"), "an embedded NUL is rejected");
}

static void TestEnumeration()
{
    Apk::Package package;
    Apk::Result result = EnumerateOf("plain.zip",
                                     {MakeEntry("classes.dex", "dex-one", false), MakeEntry("classes2.dex", "dex-two", true),
                                      MakeEntry("lib/x86_64/libacl.so", "elf-bytes-here", false),
                                      MakeEntry("lib/arm64-v8a/libacl.so", "elf-bytes-here", true), MakeEntry("assets/greeting.txt", "hello", true)},
                                     package);

    CheckResult(result, Apk::Result::Ok, "a well-formed archive enumerates");
    Check(package.entries.size() == 5, "every entry is recorded");
    Check(package.dexEntries.size() == 2 && package.dexEntries[0] == "classes.dex" && package.dexEntries[1] == "classes2.dex",
          "dex entries are recorded in load order");
    Check(package.nativeAbis.size() == 2, "both native ABIs are recorded");
    Check(Apk::FindEntry(package, "assets/greeting.txt") >= 0, "an asset entry is findable by name");
    Check(Apk::FindEntry(package, "assets/absent.txt") < 0, "an absent name is not findable");

    const Apk::Entry& stored = package.entries[(size_t)Apk::FindEntry(package, "lib/x86_64/libacl.so")];
    Check(stored.compressionMethod == Apk::COMPRESSION_STORED, "a stored entry reports COMPRESSION_STORED");
    Check(stored.uncompressedSize == 14, "a stored entry reports its uncompressed size");

    const Apk::Entry& deflated = package.entries[(size_t)Apk::FindEntry(package, "lib/arm64-v8a/libacl.so")];
    Check(deflated.compressionMethod == Apk::COMPRESSION_DEFLATE, "a deflated entry reports COMPRESSION_DEFLATE");
    Check(deflated.uncompressedSize == 14, "a deflated entry reports its uncompressed size, not its compressed one");

    Apk::Close(package);
}

static void TestAbiSelection()
{
    Apk::Package package;
    CheckResult(EnumerateOf("abis.zip", {MakeEntry("lib/arm64-v8a/libacl.so", "elf", false)}, package), Apk::Result::Ok,
                "an arm64-only archive enumerates");

    std::string selected;
    CheckResult(Apk::SelectAbi(package, "", selected), Apk::Result::UnsupportedAbi, "an arm64-only archive has no host ABI");
    Check(selected.empty(), "a refused selection leaves the ABI empty");
    CheckResult(Apk::SelectAbi(package, "arm64-v8a", selected), Apk::Result::Ok, "arm64 can be selected explicitly");
    Check(selected == "arm64-v8a", "the explicit ABI is the one selected");
    Apk::Close(package);

    Apk::Package hostPackage;
    CheckResult(EnumerateOf("host-abi.zip", {MakeEntry("lib/x86_64/libacl.so", "elf", false)}, hostPackage), Apk::Result::Ok,
                "an x86_64 archive enumerates");
    CheckResult(Apk::SelectAbi(hostPackage, "", selected), Apk::Result::Ok, "the host ABI is selected by default");
    Check(selected == Apk::HOST_NATIVE_ABI, "the default selection is the host ABI");
    Apk::Close(hostPackage);

    Apk::Package bare;
    CheckResult(EnumerateOf("no-libs.zip", {MakeEntry("classes.dex", "dex", false)}, bare), Apk::Result::Ok, "an archive with no lib/ enumerates");
    CheckResult(Apk::SelectAbi(bare, "", selected), Apk::Result::Ok, "an archive with no native code is not an ABI failure");
    Check(selected.empty(), "an archive with no native code selects no ABI");
    Apk::Close(bare);
}

static void TestDexOrdinals()
{
    Check(Apk::DexOrdinal("classes.dex") == 1, "classes.dex is ordinal 1");
    Check(Apk::DexOrdinal("classes2.dex") == 2, "classes2.dex is ordinal 2");
    Check(Apk::DexOrdinal("classes10.dex") == 10, "classes10.dex is ordinal 10");
    Check(Apk::DexOrdinal("classes0.dex") == -1, "classes0.dex is not part of the multidex scheme");
    Check(Apk::DexOrdinal("classes1.dex") == -1, "classes1.dex is not part of the multidex scheme");
    Check(Apk::DexOrdinal("classes02.dex") == -1, "a leading zero is not the canonical spelling of classes2.dex");
    Check(Apk::DexOrdinal("classesfoo.dex") == -1, "a non-numeric suffix is not a multidex member");
    Check(Apk::DexOrdinal("classes2.dex.bak") == -1, "a trailing suffix is not a multidex member");
    Check(Apk::DexOrdinal("res/classes2.dex") == -1, "a nested classes2.dex is not a multidex member");
    Check(Apk::DexOrdinal("classes99999999.dex") == -1, "an absurd ordinal is refused rather than overflowing");
}

static void TestDataOffsets()
{
    std::string payload = "the-stored-payload-bytes";

    Apk::Package package;
    CheckResult(EnumerateOf("offsets.zip", {MakeEntry("assets/first.bin", "leading entry", false), MakeEntry("assets/second.bin", payload, false)},
                            package),
                Apk::Result::Ok, "the offset archive enumerates");

    int index = Apk::FindEntry(package, "assets/second.bin");
    Check(index >= 0, "the second stored entry is present");

    if (index >= 0)
    {
        const Apk::Entry& entry = package.entries[(size_t)index];
        Check(entry.dataOffset > 0, "a stored entry records a non-zero data offset");

        FILE* archive = fopen(package.path.c_str(), "rb");
        Check(archive != nullptr, "the archive reopens for a direct read at the recorded offset");

        if (archive)
        {
            std::vector<char> read(payload.size());
            bool sought = fseek(archive, (long)entry.dataOffset, SEEK_SET) == 0;
            size_t got = sought ? fread(read.data(), 1, read.size(), archive) : 0;
            fclose(archive);

            Check(got == payload.size() && std::string(read.data(), got) == payload,
                  "Entry::dataOffset points at the entry's data, which is what M12's MapEntry will rely on");
        }
    }

    Apk::Close(package);
}

static void TestHostileArchives()
{
    Apk::Package slip;
    CheckResult(EnumerateOf("slip.zip", {MakeEntry("classes.dex", "dex", false), MakeEntry("../../evil.so", "elf", false)}, slip),
                Apk::Result::MalformedArchive, "a zip-slip name is rejected");
    Apk::Close(slip);

    Apk::Package absolute;
    CheckResult(EnumerateOf("absolute.zip", {MakeEntry("/etc/passwd", "root", false)}, absolute), Apk::Result::MalformedArchive,
                "an absolute name is rejected");
    Apk::Close(absolute);

    Apk::Package backslash;
    CheckResult(EnumerateOf("backslash.zip", {MakeEntry("lib\\x86_64\\libacl.so", "elf", false)}, backslash), Apk::Result::MalformedArchive,
                "a backslash separator is rejected");
    Apk::Close(backslash);

    Apk::Package embedded;
    CheckResult(EnumerateOf("nul.zip", {MakeEntry(std::string("lib/") + '\0' + "evil.so", "elf", false)}, embedded), Apk::Result::NotAZipArchive,
                "an embedded NUL is rejected before we ever see the name");
    Apk::Close(embedded);

    Apk::Package empty;
    CheckResult(EnumerateOf("empty-name.zip", {MakeEntry("", "nameless", false)}, empty), Apk::Result::MalformedArchive,
                "a zero-length entry name is rejected");
    Apk::Close(empty);

    Apk::Package gap;
    CheckResult(EnumerateOf("dex-gap.zip", {MakeEntry("classes.dex", "one", false), MakeEntry("classes3.dex", "three", false)}, gap),
                Apk::Result::MalformedArchive, "a multi-dex gap is rejected");
    Apk::Close(gap);

    Apk::Package strays;
    CheckResult(EnumerateOf("dex-strays.zip",
                            {MakeEntry("classes.dex", "one", false), MakeEntry("classes0.dex", "zero", false),
                             MakeEntry("classes1.dex", "one-again", false), MakeEntry("classes02.dex", "padded", false)},
                            strays),
                Apk::Result::Ok, "names outside the multidex scheme are not mistaken for a gap");
    Check(strays.dexEntries.size() == 1, "only the canonical dex file is recorded alongside the strays");
    Apk::Close(strays);

    Apk::Package unordered;
    CheckResult(EnumerateOf("dex-order.zip",
                            {MakeEntry("classes.dex", "one", false), MakeEntry("classes2.dex", "two", false),
                             MakeEntry("classes4.dex", "four", false), MakeEntry("classes3.dex", "three", false)},
                            unordered),
                Apk::Result::Ok, "central-directory order does not decide dex contiguity");
    Check(unordered.dexEntries.size() == 4, "four contiguous dex files are recorded despite the archive order");
    Apk::Close(unordered);

    Apk::Package named;
    CheckResult(EnumerateOf("dex-named.zip", {MakeEntry("classes.dex", "one", false), MakeEntry("classesfoo.dex", "not-a-dex", false)}, named),
                Apk::Result::Ok, "a non-numeric classes*.dex is not mistaken for a multi-dex member");
    Check(named.dexEntries.size() == 1, "only the real dex file is recorded");
    Apk::Close(named);

    std::vector<ZipEntrySource> single = {MakeEntry("classes.dex", "dex", false)};
    std::vector<uint8_t> truncated = BuildArchive(single);
    truncated.resize(truncated.size() - 8);
    Apk::Package broken;
    broken.path = WriteArchive("truncated.zip", truncated);
    CheckResult(Apk::EnumerateArchive(broken), Apk::Result::NotAZipArchive, "a truncated central directory is rejected");
    Apk::Close(broken);

    Apk::Package missing;
    missing.path = gWorkDirectory + "/does-not-exist.zip";
    CheckResult(Apk::EnumerateArchive(missing), Apk::Result::NotAZipArchive, "an absent archive is rejected");
    Apk::Close(missing);
}

static void TestExtraction()
{
    Apk::Package package;
    CheckResult(EnumerateOf("extract.zip",
                            {MakeEntry("lib/x86_64/libstored.so", "stored-payload", false),
                             MakeEntry("lib/x86_64/libdeflated.so", "deflated-payload-which-compresses-aaaaaaaaaaaaaaaa", true)},
                            package),
                Apk::Result::Ok, "the extraction archive enumerates");

    std::string destination = gWorkDirectory + "/lib";
    Check(mkdir(destination.c_str(), 0755) == 0, "the destination directory is created");

    std::vector<std::string> extracted;
    CheckResult(Apk::ExtractNativeLibraries(package, "x86_64", destination, extracted), Apk::Result::Ok, "both native libraries extract");
    Check(extracted.size() == 2, "both libraries are reported");

    Check(Apk::MatchesExtracted(destination + "/libstored.so", package.entries[(size_t)Apk::FindEntry(package, "lib/x86_64/libstored.so")]),
          "the stored library matches its recorded crc32");
    Check(Apk::MatchesExtracted(destination + "/libdeflated.so", package.entries[(size_t)Apk::FindEntry(package, "lib/x86_64/libdeflated.so")]),
          "the deflated library inflates to its recorded crc32");

    FILE* corrupt = fopen((destination + "/libstored.so").c_str(), "r+b");
    Check(corrupt != nullptr, "the extracted library reopens for corruption");
    if (corrupt)
    {
        fputc('X', corrupt);
        fclose(corrupt);
    }
    Check(!Apk::MatchesExtracted(destination + "/libstored.so",
                                 package.entries[(size_t)Apk::FindEntry(package, "lib/x86_64/libstored.so")]),
          "a corrupted library no longer matches, so the next run re-extracts");

    CheckResult(Apk::ExtractNativeLibraries(package, "x86_64", destination, extracted), Apk::Result::Ok, "a corrupted library is re-extracted");
    Check(Apk::MatchesExtracted(destination + "/libstored.so", package.entries[(size_t)Apk::FindEntry(package, "lib/x86_64/libstored.so")]),
          "the re-extracted library matches again");

    CheckResult(Apk::ExtractNativeLibraries(package, "arm64-v8a", destination, extracted), Apk::Result::Ok, "an absent ABI extracts nothing");
    Check(extracted.empty(), "an absent ABI reports no libraries");

    CheckResult(Apk::ExtractEntry(package, -1, destination + "/out.so"), Apk::Result::EntryNotFound, "a negative entry index is refused");
    CheckResult(Apk::ExtractEntry(package, 9999, destination + "/out.so"), Apk::Result::EntryNotFound, "an out-of-range entry index is refused");

    Check(access((destination + "/libstored.so.partial").c_str(), F_OK) != 0, "no partial file survives a successful extraction");

    Apk::Close(package);
}

static void TestDeflateBomb()
{
    std::vector<ZipEntrySource> stored = {MakeEntry("lib/x86_64/libbomb.so", "small", false)};
    stored[0].uncompressedSize = 0xfffffff0;

    Apk::Package rejected;
    rejected.path = WriteArchive("bomb-stored.zip", BuildArchive(stored));
    CheckResult(Apk::EnumerateArchive(rejected), Apk::Result::NotAZipArchive,
                "a stored entry claiming more bytes than the archive holds is rejected at open");
    Apk::Close(rejected);

    std::vector<ZipEntrySource> deflated = {MakeEntry("lib/x86_64/libbomb.so", "small", true)};
    deflated[0].uncompressedSize = (uint32_t)(Apk::MAX_EXTRACT_BYTES + 1);

    Apk::Package package;
    package.path = WriteArchive("bomb-deflated.zip", BuildArchive(deflated));
    CheckResult(Apk::EnumerateArchive(package), Apk::Result::Ok, "a deflated entry with an absurd declared size still enumerates");

    int index = Apk::FindEntry(package, "lib/x86_64/libbomb.so");
    Check(index >= 0, "the bomb entry is present");

    if (index >= 0)
    {
        Check(package.entries[(size_t)index].uncompressedSize > Apk::MAX_EXTRACT_BYTES, "the declared size exceeds the per-entry extraction cap");
        CheckResult(Apk::ExtractEntry(package, index, gWorkDirectory + "/bomb.so"), Apk::Result::MalformedArchive,
                    "an absurd declared size is refused before anything is written");
        Check(access((gWorkDirectory + "/bomb.so").c_str(), F_OK) != 0, "a refused extraction writes no file");
    }

    Apk::Close(package);
}

static void TestAggregateBudget()
{
    std::string payload(4096, 'z');

    std::vector<ZipEntrySource> entries;
    for (int index = 0; index < 12; ++index)
    {
        std::string name = "lib/x86_64/libbomb" + std::to_string(index) + ".so";
        ZipEntrySource entry = MakeEntry(name, payload, true);
        entry.uncompressedSize = (uint32_t)(Apk::MAX_EXTRACT_TOTAL_BYTES / 4);
        entries.push_back(entry);
    }

    Apk::Package package;
    package.path = WriteArchive("budget.zip", BuildArchive(entries));
    CheckResult(Apk::EnumerateArchive(package), Apk::Result::Ok, "the aggregate-budget archive enumerates");

    std::string destination = gWorkDirectory + "/budget";
    Check(mkdir(destination.c_str(), 0755) == 0, "the aggregate-budget destination is created");

    std::vector<std::string> extracted;
    CheckResult(Apk::ExtractNativeLibraries(package, "x86_64", destination, extracted), Apk::Result::MalformedArchive,
                "twelve entries each a quarter of the total budget are refused in aggregate");
    Check(extracted.empty(), "an over-budget archive extracts nothing at all");

    Apk::Close(package);
}

static void TestSymlinkDestination()
{
    std::string payload = "replacement-content";

    Apk::Package package;
    CheckResult(EnumerateOf("symlink.zip", {MakeEntry("lib/x86_64/libtarget.so", payload, false)}, package), Apk::Result::Ok,
                "the symlink archive enumerates");

    std::string destination = gWorkDirectory + "/symlinkdir";
    Check(mkdir(destination.c_str(), 0755) == 0, "the symlink destination directory is created");

    std::string victim = gWorkDirectory + "/victim.txt";
    FILE* file = fopen(victim.c_str(), "wb");
    Check(file != nullptr, "the victim file is created");
    if (file)
    {
        fputs("ORIGINAL", file);
        fclose(file);
    }

    std::string partial = destination + "/libtarget.so.partial." + std::to_string((long)getpid());
    Check(symlink(victim.c_str(), partial.c_str()) == 0, "a symlink is planted where the partial file would go");

    std::vector<std::string> extracted;
    CheckResult(Apk::ExtractNativeLibraries(package, "x86_64", destination, extracted), Apk::Result::Ok,
                "a planted symlink is replaced rather than followed, so extraction still succeeds");

    char readBack[64] = {0};
    FILE* check = fopen(victim.c_str(), "rb");
    if (check)
    {
        size_t got = fread(readBack, 1, sizeof(readBack) - 1, check);
        readBack[got] = '\0';
        fclose(check);
    }

    Check(std::string(readBack) == "ORIGINAL", "a planted symlink cannot redirect extraction onto another file");

    struct stat status;
    Check(lstat((destination + "/libtarget.so").c_str(), &status) != 0 || S_ISREG(status.st_mode),
          "the extracted library is a regular file, never a symlink");

    Apk::Close(package);
}

static void TestIdempotentEnumeration()
{
    Apk::Package package;
    CheckResult(EnumerateOf("idempotent.zip",
                            {MakeEntry("classes.dex", "one", false), MakeEntry("lib/x86_64/libacl.so", "elf", false),
                             MakeEntry("assets/a.txt", "a", true)},
                            package),
                Apk::Result::Ok, "the idempotency archive enumerates");

    size_t entries = package.entries.size();
    size_t dex = package.dexEntries.size();
    size_t abis = package.nativeAbis.size();

    CheckResult(Apk::EnumerateArchive(package), Apk::Result::Ok, "a second enumeration of the same package succeeds");
    Check(package.entries.size() == entries && package.dexEntries.size() == dex && package.nativeAbis.size() == abis,
          "a second enumeration replaces the previous state rather than appending to it");

    Apk::Close(package);
}

static void TestOverlongEntryName()
{
    Apk::Package package;
    CheckResult(EnumerateOf("overlong.zip", {MakeEntry(std::string(65535, 'a'), "payload", false)}, package), Apk::Result::NotAZipArchive,
                "a 65535-byte entry name is refused rather than overflowing the name buffer");
    Apk::Close(package);

    Apk::Package boundary;
    CheckResult(EnumerateOf("boundary.zip", {MakeEntry(std::string(Apk::MAX_ENTRY_NAME_LENGTH + 1, 'a'), "payload", false)}, boundary),
                Apk::Result::NotAZipArchive, "a name one byte past our own cap is refused");
    Apk::Close(boundary);

    Apk::Package accepted;
    CheckResult(EnumerateOf("longest.zip", {MakeEntry(std::string(Apk::MAX_ENTRY_NAME_LENGTH, 'a'), "payload", false)}, accepted),
                Apk::Result::Ok, "a name at exactly our cap is still accepted");
    Check(accepted.entries.size() == 1 && accepted.entries[0].name.size() == (size_t)Apk::MAX_ENTRY_NAME_LENGTH,
          "the longest accepted name round-trips at full length");
    Apk::Close(accepted);
}

static void RemoveTree(const std::string& path)
{
    if (path.empty()) return;

    DIR* directory = opendir(path.c_str());
    if (!directory) return;

    for (dirent* entry = readdir(directory); entry; entry = readdir(directory))
    {
        std::string name = entry->d_name;
        if (name == "." || name == "..") continue;

        std::string child = path + "/" + name;
        struct stat status;
        if (stat(child.c_str(), &status) == 0 && S_ISDIR(status.st_mode))
        {
            RemoveTree(child);
            continue;
        }

        unlink(child.c_str());
    }

    closedir(directory);
    rmdir(path.c_str());
}

int main()
{
    std::string templatePath = "/tmp/acl-apk-zip-XXXXXX";

    std::vector<char> mutablePath(templatePath.begin(), templatePath.end());
    mutablePath.push_back('\0');

    if (!mkdtemp(mutablePath.data()))
    {
        printf("FAIL could not create a work directory\n");
        return 1;
    }
    gWorkDirectory = mutablePath.data();

    TestSafeNames();
    TestEnumeration();
    TestDexOrdinals();
    TestDataOffsets();
    TestAbiSelection();
    TestHostileArchives();
    TestExtraction();
    TestDeflateBomb();
    TestAggregateBudget();
    TestSymlinkDestination();
    TestIdempotentEnumeration();
    TestOverlongEntryName();

    RemoveTree(gWorkDirectory);

    printf("%s: %d failures\n", gFailures == 0 ? "apk-zip-unit PASSED" : "apk-zip-unit FAILED", gFailures);
    return gFailures == 0 ? 0 : 1;
}

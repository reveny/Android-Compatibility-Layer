#include "ApkSignature/ApkSignature.hpp"
#include "ApkSignature/ApkSignatureInternal.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <sys/stat.h>
#include <vector>

static int gChecks = 0;
static int gFailures = 0;

static void Check(bool condition, const std::string& what)
{
    gChecks += 1;

    if (condition) return;

    gFailures += 1;
    std::fprintf(stderr, "FAILED: %s\n", what.c_str());
}

static bool CopyFile(const std::string& from, const std::string& to)
{
    std::FILE* source = std::fopen(from.c_str(), "rb");
    if (source == nullptr) return false;

    std::FILE* target = std::fopen(to.c_str(), "wb");
    if (target == nullptr)
    {
        std::fclose(source);
        return false;
    }

    char buffer[65536];
    size_t read = 0;

    while ((read = std::fread(buffer, 1, sizeof(buffer), source)) > 0)
    {
        std::fwrite(buffer, 1, read, target);
    }

    std::fclose(source);
    std::fclose(target);

    return true;
}

static bool FlipByte(const std::string& path, long offset)
{
    std::FILE* file = std::fopen(path.c_str(), "r+b");
    if (file == nullptr) return false;

    if (std::fseek(file, offset, SEEK_SET) != 0)
    {
        std::fclose(file);
        return false;
    }

    int value = std::fgetc(file);
    if (value < 0)
    {
        std::fclose(file);
        return false;
    }

    std::fseek(file, offset, SEEK_SET);
    std::fputc(value ^ 0xff, file);
    std::fclose(file);

    return true;
}

static std::string Hex(const std::vector<uint8_t>& bytes)
{
    static const char* DIGITS = "0123456789abcdef";
    std::string text;

    for (uint8_t value : bytes)
    {
        text.push_back(DIGITS[value >> 4]);
        text.push_back(DIGITS[value & 0x0f]);
    }

    return text;
}

static std::string DigestOf(ApkSignatureInternal::DigestKind kind, const std::string& input)
{
    std::vector<uint8_t> out;
    ApkSignatureInternal::Digest(kind, input.data(), input.size(), out);

    return Hex(out);
}

static void CheckDigests()
{
    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha256, "")
              == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
          "SHA-256 of the empty string matches the published vector");

    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha256, "abc")
              == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
          "SHA-256 of \"abc\" matches the published vector");

    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha256,
                   "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
              == "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
          "SHA-256 of the 56 byte vector, which straddles the padding boundary, matches");

    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha512, "")
              == "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce"
                 "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
          "SHA-512 of the empty string matches the published vector");

    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha512, "abc")
              == "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                 "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
          "SHA-512 of \"abc\" matches the published vector");

    std::string long1000;
    long1000.assign(1000000, 'a');

    Check(DigestOf(ApkSignatureInternal::DigestKind::Sha256, long1000)
              == "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
          "SHA-256 of a million 'a' matches the published vector, so multi block streaming is right");
}

int main(int argc, char** argv)
{
    if (argc < 3)
    {
        std::fprintf(stderr, "usage: SignatureUnitTest <apk> <scratch directory>\n");
        return 2;
    }

    std::string apk = argv[1];
    std::string scratch = argv[2];

    ::mkdir(scratch.c_str(), 0755);

    CheckDigests();

    ApkSignature::Info info;
    ApkSignature::Result result = ApkSignature::Verify(apk, info);

    Check(result == ApkSignature::Result::Ok, std::string("the fixture verifies, got: ") + ApkSignature::ResultName(result));
    Check(info.verified, "the fixture reports itself verified");
    Check(info.scheme == ApkSignature::SCHEME_V2 || info.scheme == ApkSignature::SCHEME_V3,
          "the fixture is signed with a block scheme");
    Check(info.signers.size() == 1, "the fixture has exactly one signer");

    if (!info.signers.empty())
    {
        const ApkSignature::Signer& signer = info.signers.front();

        Check(!signer.certificate.empty(), "the signer carries a certificate");
        Check(signer.certificate.size() > 200, "the certificate is a plausible size");
        Check(signer.certificate[0] == 0x30, "the certificate is DER, so it opens with a SEQUENCE tag");
        Check(signer.certificateSha256.size() == 64, "the fingerprint is a full SHA-256 in hex");
        Check(signer.subject.find("Android") != std::string::npos || signer.subject.find("CN=") != std::string::npos,
              std::string("the subject reads as a name, got: ") + signer.subject);
        Check(signer.algorithm.find("SHA-") != std::string::npos, "the algorithm names its digest");
    }

    ApkSignature::Info read;
    ApkSignature::Result readResult = ApkSignature::Read(apk, read);

    Check(readResult == ApkSignature::Result::Ok, "reading without verifying succeeds");
    Check(!read.verified, "reading without verifying does not claim verification");
    Check(read.signers.size() == info.signers.size(), "reading finds the same signer count");

    if (!read.signers.empty() && !info.signers.empty())
    {
        Check(read.signers.front().certificate == info.signers.front().certificate,
              "reading and verifying return the same certificate bytes");
    }

    std::string tampered = scratch + "/tampered.apk";
    Check(CopyFile(apk, tampered), "the fixture can be copied for tampering");
    Check(FlipByte(tampered, 64), "a byte inside the first zip entry can be flipped");

    ApkSignature::Info tamperedInfo;
    ApkSignature::Result tamperedResult = ApkSignature::Verify(tampered, tamperedInfo);

    Check(tamperedResult == ApkSignature::Result::DigestMismatch,
          std::string("a tampered payload is rejected as a digest mismatch, got: ") + ApkSignature::ResultName(tamperedResult));
    Check(!tamperedInfo.verified, "a tampered payload is not reported verified");
    Check(!tamperedInfo.signers.empty(), "a tampered payload still names the signer it claims to be");

    std::string missing = scratch + "/there-is-no-such-file.apk";
    ApkSignature::Info missingInfo;

    Check(ApkSignature::Verify(missing, missingInfo) == ApkSignature::Result::Unreadable, "a missing file is unreadable");

    std::string plain = scratch + "/plain.bin";
    std::FILE* file = std::fopen(plain.c_str(), "wb");

    if (file != nullptr)
    {
        std::fwrite("this is not a zip archive at all", 1, 32, file);
        std::fclose(file);

        ApkSignature::Info plainInfo;
        ApkSignature::Result plainResult = ApkSignature::Verify(plain, plainInfo);

        Check(plainResult == ApkSignature::Result::Unsigned,
              std::string("a file that is not an archive reports unsigned, got: ") + ApkSignature::ResultName(plainResult));
        Check(plainInfo.scheme == ApkSignature::SCHEME_NONE, "a file that is not an archive names no scheme");
    }

    Check(std::strcmp(ApkSignature::SchemeName(ApkSignature::SCHEME_V2), "v2") == 0, "scheme 2 is named v2");
    Check(std::strcmp(ApkSignature::SchemeName(ApkSignature::SCHEME_NONE), "none") == 0, "scheme 0 is named none");
    Check(ApkSignature::DescribeInfo(info).find("signer") != std::string::npos, "the description mentions the signer");

    std::printf("%d check(s), %d failure(s)\n", gChecks, gFailures);

    return gFailures == 0 ? 0 : 1;
}

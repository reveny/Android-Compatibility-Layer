#include "ApkSignature/ApkSignature.hpp"

#include "ApkSignature/ApkSignatureInternal.hpp"

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

namespace ApkSignature
{
    static constexpr const char* BLOCK_MAGIC = "APK Sig Block 42";
    static constexpr size_t BLOCK_MAGIC_BYTES = 16;
    static constexpr uint32_t END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50;
    static constexpr size_t END_OF_CENTRAL_DIRECTORY_MINIMUM = 22;
    static constexpr size_t COMMENT_SEARCH_LIMIT = 65535 + END_OF_CENTRAL_DIRECTORY_MINIMUM;
    static constexpr uint32_t BLOCK_ID_V2 = 0x7109871a;
    static constexpr uint32_t BLOCK_ID_V3 = 0xf05368c0;
    static constexpr size_t CHUNK_BYTES = 1024 * 1024;
    static constexpr uint8_t CHUNK_PREFIX = 0xa5;
    static constexpr uint8_t ROOT_PREFIX = 0x5a;

    static constexpr uint32_t ALGORITHM_RSA_PSS_SHA256 = 0x0101;
    static constexpr uint32_t ALGORITHM_RSA_PSS_SHA512 = 0x0102;
    static constexpr uint32_t ALGORITHM_RSA_PKCS1_SHA256 = 0x0103;
    static constexpr uint32_t ALGORITHM_RSA_PKCS1_SHA512 = 0x0104;
    static constexpr uint32_t ALGORITHM_ECDSA_SHA256 = 0x0201;
    static constexpr uint32_t ALGORITHM_ECDSA_SHA512 = 0x0202;
    static constexpr uint32_t ALGORITHM_DSA_SHA256 = 0x0301;

    struct Mapping
    {
        const uint8_t* data;
        size_t size;
        int descriptor;
    };

    struct Reader
    {
        const uint8_t* data;
        size_t size;
        size_t at;
        bool bad;
    };

    struct Located
    {
        size_t blockStart;
        size_t blockEnd;
        size_t centralDirectoryOffset;
        size_t centralDirectorySize;
        size_t endOfCentralDirectory;
    };
}

static bool OpenMapping(const std::string& path, ApkSignature::Mapping& mapping)
{
    mapping.data = nullptr;
    mapping.size = 0;
    mapping.descriptor = -1;

    int descriptor = ::open(path.c_str(), O_RDONLY | O_CLOEXEC);
    if (descriptor < 0) return false;

    struct stat status = {};
    if (::fstat(descriptor, &status) != 0 || status.st_size <= 0)
    {
        ::close(descriptor);
        return false;
    }

    void* address = ::mmap(nullptr, static_cast<size_t>(status.st_size), PROT_READ, MAP_PRIVATE, descriptor, 0);
    if (address == MAP_FAILED)
    {
        ::close(descriptor);
        return false;
    }

    mapping.data = static_cast<const uint8_t*>(address);
    mapping.size = static_cast<size_t>(status.st_size);
    mapping.descriptor = descriptor;

    return true;
}

static void CloseMapping(ApkSignature::Mapping& mapping)
{
    if (mapping.data != nullptr) ::munmap(const_cast<uint8_t*>(mapping.data), mapping.size);
    if (mapping.descriptor >= 0) ::close(mapping.descriptor);

    mapping.data = nullptr;
    mapping.size = 0;
    mapping.descriptor = -1;
}

static uint32_t ReadUnsigned32(const uint8_t* data)
{
    return static_cast<uint32_t>(data[0]) | (static_cast<uint32_t>(data[1]) << 8) | (static_cast<uint32_t>(data[2]) << 16)
           | (static_cast<uint32_t>(data[3]) << 24);
}

static uint64_t ReadUnsigned64(const uint8_t* data)
{
    return static_cast<uint64_t>(ReadUnsigned32(data)) | (static_cast<uint64_t>(ReadUnsigned32(data + 4)) << 32);
}

static bool Remaining(const ApkSignature::Reader& reader, size_t bytes)
{
    return !reader.bad && reader.at + bytes <= reader.size;
}

static uint32_t TakeUnsigned32(ApkSignature::Reader& reader)
{
    if (!Remaining(reader, 4))
    {
        reader.bad = true;
        return 0;
    }

    uint32_t value = ReadUnsigned32(reader.data + reader.at);
    reader.at += 4;

    return value;
}

static bool TakeLengthPrefixed(ApkSignature::Reader& reader, ApkSignature::Reader& out)
{
    uint32_t length = TakeUnsigned32(reader);
    if (reader.bad) return false;

    if (!Remaining(reader, length))
    {
        reader.bad = true;
        return false;
    }

    out.data = reader.data + reader.at;
    out.size = length;
    out.at = 0;
    out.bad = false;

    reader.at += length;

    return true;
}

static bool LocateSigningBlock(const ApkSignature::Mapping& mapping, ApkSignature::Located& located)
{
    if (mapping.size < ApkSignature::END_OF_CENTRAL_DIRECTORY_MINIMUM) return false;

    size_t searchFrom = mapping.size > ApkSignature::COMMENT_SEARCH_LIMIT ? mapping.size - ApkSignature::COMMENT_SEARCH_LIMIT : 0;
    size_t found = 0;
    bool any = false;

    for (size_t at = mapping.size - ApkSignature::END_OF_CENTRAL_DIRECTORY_MINIMUM + 1; at-- > searchFrom;)
    {
        if (ReadUnsigned32(mapping.data + at) != ApkSignature::END_OF_CENTRAL_DIRECTORY_SIGNATURE) continue;

        found = at;
        any = true;
        break;
    }

    if (!any) return false;

    located.endOfCentralDirectory = found;
    located.centralDirectorySize = ReadUnsigned32(mapping.data + found + 12);
    located.centralDirectoryOffset = ReadUnsigned32(mapping.data + found + 16);

    if (located.centralDirectoryOffset > mapping.size) return false;
    if (located.centralDirectoryOffset < ApkSignature::BLOCK_MAGIC_BYTES + 8) return false;

    const uint8_t* magic = mapping.data + located.centralDirectoryOffset - ApkSignature::BLOCK_MAGIC_BYTES;
    if (::memcmp(magic, ApkSignature::BLOCK_MAGIC, ApkSignature::BLOCK_MAGIC_BYTES) != 0) return false;

    uint64_t trailingSize = ReadUnsigned64(mapping.data + located.centralDirectoryOffset - ApkSignature::BLOCK_MAGIC_BYTES - 8);
    if (trailingSize + 8 > located.centralDirectoryOffset) return false;

    size_t blockStart = located.centralDirectoryOffset - static_cast<size_t>(trailingSize) - 8;
    uint64_t leadingSize = ReadUnsigned64(mapping.data + blockStart);

    if (leadingSize != trailingSize) return false;

    located.blockStart = blockStart;
    located.blockEnd = located.centralDirectoryOffset - ApkSignature::BLOCK_MAGIC_BYTES - 8;

    return true;
}

static bool FindBlockValue(const ApkSignature::Mapping& mapping, const ApkSignature::Located& located, uint32_t wantedId,
                           ApkSignature::Reader& value)
{
    size_t at = located.blockStart + 8;

    while (at + 12 <= located.blockEnd)
    {
        uint64_t pairLength = ReadUnsigned64(mapping.data + at);
        if (pairLength < 4 || at + 8 + pairLength > located.blockEnd + 24) break;

        uint32_t id = ReadUnsigned32(mapping.data + at + 8);

        if (id == wantedId)
        {
            value.data = mapping.data + at + 12;
            value.size = static_cast<size_t>(pairLength) - 4;
            value.at = 0;
            value.bad = false;

            return true;
        }

        at += 8 + static_cast<size_t>(pairLength);
    }

    return false;
}

static ApkSignatureInternal::DigestKind DigestForAlgorithm(uint32_t algorithm)
{
    switch (algorithm)
    {
        case ApkSignature::ALGORITHM_RSA_PSS_SHA512:
        case ApkSignature::ALGORITHM_RSA_PKCS1_SHA512:
            return ApkSignatureInternal::DigestKind::Sha512;
        default:
            return ApkSignatureInternal::DigestKind::Sha256;
    }
}

static const char* AlgorithmName(uint32_t algorithm)
{
    switch (algorithm)
    {
        case ApkSignature::ALGORITHM_RSA_PSS_SHA256: return "RSASSA-PSS with SHA-256";
        case ApkSignature::ALGORITHM_RSA_PSS_SHA512: return "RSASSA-PSS with SHA-512";
        case ApkSignature::ALGORITHM_RSA_PKCS1_SHA256: return "RSASSA-PKCS1-v1_5 with SHA-256";
        case ApkSignature::ALGORITHM_RSA_PKCS1_SHA512: return "RSASSA-PKCS1-v1_5 with SHA-512";
        case ApkSignature::ALGORITHM_ECDSA_SHA256: return "ECDSA with SHA-256";
        case ApkSignature::ALGORITHM_ECDSA_SHA512: return "ECDSA with SHA-512";
        case ApkSignature::ALGORITHM_DSA_SHA256: return "DSA with SHA-256";
        default: return "an algorithm this runtime does not know";
    }
}

static bool IsProbabilisticSignature(uint32_t algorithm)
{
    return algorithm == ApkSignature::ALGORITHM_RSA_PSS_SHA256 || algorithm == ApkSignature::ALGORITHM_RSA_PSS_SHA512;
}

static int AlgorithmRank(uint32_t algorithm)
{
    switch (algorithm)
    {
        case ApkSignature::ALGORITHM_RSA_PSS_SHA512: return 6;
        case ApkSignature::ALGORITHM_RSA_PKCS1_SHA512: return 5;
        case ApkSignature::ALGORITHM_RSA_PSS_SHA256: return 3;
        case ApkSignature::ALGORITHM_RSA_PKCS1_SHA256: return 2;
        default: return -1;
    }
}

static void DigestBytes(ApkSignatureInternal::DigestKind kind, const uint8_t* data, size_t size, std::vector<uint8_t>& out)
{
    ApkSignatureInternal::Digest(kind, data, size, out);
}

static std::string ToHex(const std::vector<uint8_t>& bytes)
{
    static const char* DIGITS = "0123456789abcdef";
    std::string text;
    text.reserve(bytes.size() * 2);

    for (uint8_t value : bytes)
    {
        text.push_back(DIGITS[value >> 4]);
        text.push_back(DIGITS[value & 0x0f]);
    }

    return text;
}

static void ChunkDigest(ApkSignatureInternal::DigestKind kind, const uint8_t* data, size_t size, std::vector<uint8_t>& accumulated,
                        size_t& chunks)
{
    for (size_t at = 0; at < size; at += ApkSignature::CHUNK_BYTES)
    {
        size_t take = std::min(ApkSignature::CHUNK_BYTES, size - at);

        uint8_t header[5];
        header[0] = ApkSignature::CHUNK_PREFIX;
        header[1] = static_cast<uint8_t>(take & 0xff);
        header[2] = static_cast<uint8_t>((take >> 8) & 0xff);
        header[3] = static_cast<uint8_t>((take >> 16) & 0xff);
        header[4] = static_cast<uint8_t>((take >> 24) & 0xff);

        ApkSignatureInternal::DigestState state;
        ApkSignatureInternal::DigestBegin(state, kind);
        ApkSignatureInternal::DigestUpdate(state, header, sizeof(header));
        ApkSignatureInternal::DigestUpdate(state, data + at, take);

        std::vector<uint8_t> produced;
        ApkSignatureInternal::DigestEnd(state, produced);

        accumulated.insert(accumulated.end(), produced.begin(), produced.end());
        chunks += 1;
    }
}

static void ContentDigest(ApkSignatureInternal::DigestKind kind, const ApkSignature::Mapping& mapping,
                          const ApkSignature::Located& located, std::vector<uint8_t>& out)
{
    std::vector<uint8_t> accumulated;
    size_t chunks = 0;

    ChunkDigest(kind, mapping.data, located.blockStart, accumulated, chunks);
    ChunkDigest(kind, mapping.data + located.centralDirectoryOffset, located.centralDirectorySize, accumulated, chunks);

    size_t endSize = mapping.size - located.endOfCentralDirectory;
    std::vector<uint8_t> end(mapping.data + located.endOfCentralDirectory, mapping.data + mapping.size);

    if (end.size() >= 20)
    {
        uint32_t patched = static_cast<uint32_t>(located.blockStart);
        end[16] = static_cast<uint8_t>(patched & 0xff);
        end[17] = static_cast<uint8_t>((patched >> 8) & 0xff);
        end[18] = static_cast<uint8_t>((patched >> 16) & 0xff);
        end[19] = static_cast<uint8_t>((patched >> 24) & 0xff);
    }

    ChunkDigest(kind, end.data(), endSize, accumulated, chunks);

    uint8_t header[5];
    header[0] = ApkSignature::ROOT_PREFIX;
    header[1] = static_cast<uint8_t>(chunks & 0xff);
    header[2] = static_cast<uint8_t>((chunks >> 8) & 0xff);
    header[3] = static_cast<uint8_t>((chunks >> 16) & 0xff);
    header[4] = static_cast<uint8_t>((chunks >> 24) & 0xff);

    ApkSignatureInternal::DigestState state;
    ApkSignatureInternal::DigestBegin(state, kind);
    ApkSignatureInternal::DigestUpdate(state, header, sizeof(header));
    ApkSignatureInternal::DigestUpdate(state, accumulated.data(), accumulated.size());
    ApkSignatureInternal::DigestEnd(state, out);
}

static bool VerifySignature(uint32_t algorithm, const uint8_t* publicKey, size_t publicKeySize, const uint8_t* signedData,
                            size_t signedDataSize, const uint8_t* signature, size_t signatureSize)
{
    if (AlgorithmRank(algorithm) < 0) return false;

    const ApkSignatureInternal::DigestKind kind = DigestForAlgorithm(algorithm);

    std::vector<uint8_t> modulus;
    std::vector<uint8_t> exponent;

    if (!ApkSignatureInternal::RsaPublicKeyFromSubjectPublicKeyInfo(publicKey, publicKeySize, modulus, exponent)) return false;

    std::vector<uint8_t> recovered;
    if (!ApkSignatureInternal::RsaRecover(modulus, exponent, signature, signatureSize, recovered)) return false;

    std::vector<uint8_t> digest;
    ApkSignatureInternal::Digest(kind, signedData, signedDataSize, digest);

    if (!IsProbabilisticSignature(algorithm)) return ApkSignatureInternal::VerifyPkcs1(recovered, kind, digest);

    size_t modulusBits = modulus.size() * 8;
    uint8_t leading = modulus.front();

    while (modulusBits > 0 && (leading & 0x80) == 0)
    {
        modulusBits -= 1;
        leading = static_cast<uint8_t>(leading << 1);
    }

    return ApkSignatureInternal::VerifyProbabilistic(recovered, modulusBits, kind, digest);
}

static bool KeyMatchesCertificate(const uint8_t* publicKey, size_t publicKeySize, const std::vector<uint8_t>& certificate)
{
    std::vector<uint8_t> fromCertificate;
    if (!ApkSignatureInternal::CertificatePublicKey(certificate, fromCertificate)) return false;

    return fromCertificate.size() == publicKeySize && ::memcmp(fromCertificate.data(), publicKey, publicKeySize) == 0;
}

static std::string CertificateSubject(const std::vector<uint8_t>& certificate)
{
    return ApkSignatureInternal::CertificateSubject(certificate);
}

static ApkSignature::Result ParseSigners(ApkSignature::Reader signers, const ApkSignature::Mapping& mapping,
                                         const ApkSignature::Located& located, bool verify, ApkSignature::Info& info)
{
    while (signers.at < signers.size)
    {
        ApkSignature::Reader signer;
        if (!TakeLengthPrefixed(signers, signer)) return ApkSignature::Result::Malformed;

        ApkSignature::Reader signedData;
        if (!TakeLengthPrefixed(signer, signedData)) return ApkSignature::Result::Malformed;

        const uint8_t* signedDataBytes = signedData.data;
        size_t signedDataSize = signedData.size;

        if (info.scheme == ApkSignature::SCHEME_V3)
        {
            TakeUnsigned32(signer);
            TakeUnsigned32(signer);
            if (signer.bad) return ApkSignature::Result::Malformed;
        }

        ApkSignature::Reader signatures;
        if (!TakeLengthPrefixed(signer, signatures)) return ApkSignature::Result::Malformed;

        ApkSignature::Reader publicKey;
        if (!TakeLengthPrefixed(signer, publicKey)) return ApkSignature::Result::Malformed;

        uint32_t chosen = 0;
        const uint8_t* chosenSignature = nullptr;
        size_t chosenSignatureSize = 0;
        int chosenRank = -1;

        while (signatures.at < signatures.size)
        {
            ApkSignature::Reader entry;
            if (!TakeLengthPrefixed(signatures, entry)) return ApkSignature::Result::Malformed;

            uint32_t algorithm = TakeUnsigned32(entry);
            ApkSignature::Reader bytes;
            if (!TakeLengthPrefixed(entry, bytes)) return ApkSignature::Result::Malformed;

            int rank = AlgorithmRank(algorithm);
            if (rank <= chosenRank) continue;

            chosen = algorithm;
            chosenRank = rank;
            chosenSignature = bytes.data;
            chosenSignatureSize = bytes.size;
        }

        if (chosenRank < 0) return ApkSignature::Result::UnsupportedAlgorithm;

        ApkSignature::Reader digests;
        if (!TakeLengthPrefixed(signedData, digests)) return ApkSignature::Result::Malformed;

        ApkSignature::Reader certificates;
        if (!TakeLengthPrefixed(signedData, certificates)) return ApkSignature::Result::Malformed;

        std::vector<uint8_t> claimed;

        while (digests.at < digests.size)
        {
            ApkSignature::Reader entry;
            if (!TakeLengthPrefixed(digests, entry)) return ApkSignature::Result::Malformed;

            uint32_t algorithm = TakeUnsigned32(entry);
            ApkSignature::Reader bytes;
            if (!TakeLengthPrefixed(entry, bytes)) return ApkSignature::Result::Malformed;

            if (algorithm != chosen) continue;

            claimed.assign(bytes.data, bytes.data + bytes.size);
        }

        if (verify && claimed.empty()) return ApkSignature::Result::DigestMismatch;

        bool first = true;

        while (certificates.at < certificates.size)
        {
            ApkSignature::Reader entry;
            if (!TakeLengthPrefixed(certificates, entry)) return ApkSignature::Result::Malformed;

            ApkSignature::Signer record;
            record.certificate.assign(entry.data, entry.data + entry.size);
            record.subject = CertificateSubject(record.certificate);
            record.algorithm = AlgorithmName(chosen);

            std::vector<uint8_t> fingerprint;
            DigestBytes(ApkSignatureInternal::DigestKind::Sha256, record.certificate.data(), record.certificate.size(), fingerprint);
            record.certificateSha256 = ToHex(fingerprint);

            if (first && verify && !KeyMatchesCertificate(publicKey.data, publicKey.size, record.certificate))
            {
                return ApkSignature::Result::KeyMismatch;
            }

            info.signers.push_back(record);
            first = false;
        }

        if (!verify) continue;

        if (!VerifySignature(chosen, publicKey.data, publicKey.size, signedDataBytes, signedDataSize, chosenSignature,
                             chosenSignatureSize))
        {
            return ApkSignature::Result::SignatureMismatch;
        }

        std::vector<uint8_t> computed;
        ContentDigest(DigestForAlgorithm(chosen), mapping, located, computed);

        if (computed != claimed) return ApkSignature::Result::DigestMismatch;
    }

    if (info.signers.empty()) return ApkSignature::Result::NoSigners;

    return ApkSignature::Result::Ok;
}

static ApkSignature::Result Inspect(const std::string& path, bool verify, ApkSignature::Info& info)
{
    info.scheme = ApkSignature::SCHEME_NONE;
    info.verified = false;
    info.signers.clear();

    ApkSignature::Mapping mapping = {};
    if (!OpenMapping(path, mapping)) return ApkSignature::Result::Unreadable;

    ApkSignature::Located located = {};
    if (!LocateSigningBlock(mapping, located))
    {
        CloseMapping(mapping);
        return ApkSignature::Result::Unsigned;
    }

    ApkSignature::Reader value = {};
    int scheme = ApkSignature::SCHEME_NONE;

    if (FindBlockValue(mapping, located, ApkSignature::BLOCK_ID_V3, value))
    {
        scheme = ApkSignature::SCHEME_V3;
    }
    else if (FindBlockValue(mapping, located, ApkSignature::BLOCK_ID_V2, value))
    {
        scheme = ApkSignature::SCHEME_V2;
    }
    else
    {
        CloseMapping(mapping);
        return ApkSignature::Result::Unsigned;
    }

    info.scheme = scheme;

    ApkSignature::Reader signers;
    if (!TakeLengthPrefixed(value, signers))
    {
        CloseMapping(mapping);
        return ApkSignature::Result::Malformed;
    }

    ApkSignature::Result result = ParseSigners(signers, mapping, located, verify, info);
    info.verified = verify && result == ApkSignature::Result::Ok;

    CloseMapping(mapping);

    return result;
}

ApkSignature::Result ApkSignature::Verify(const std::string& path, ApkSignature::Info& info)
{
    return Inspect(path, true, info);
}

ApkSignature::Result ApkSignature::Read(const std::string& path, ApkSignature::Info& info)
{
    return Inspect(path, false, info);
}

std::string ApkSignature::DescribeInfo(const ApkSignature::Info& info)
{
    if (info.signers.empty()) return "no signer";

    std::string text = std::to_string(info.signers.size());
    text += info.signers.size() == 1 ? " signer, " : " signers, ";
    text += info.signers.front().algorithm;
    text += ", ";
    text += info.signers.front().certificateSha256.substr(0, 16);

    return text;
}

const char* ApkSignature::ResultName(ApkSignature::Result result)
{
    switch (result)
    {
        case ApkSignature::Result::Ok: return "ok";
        case ApkSignature::Result::Unreadable: return "the file could not be read";
        case ApkSignature::Result::NotAnArchive: return "the file is not a zip archive";
        case ApkSignature::Result::Unsigned: return "the archive carries no signing block";
        case ApkSignature::Result::Malformed: return "the signing block is malformed";
        case ApkSignature::Result::UnsupportedAlgorithm: return "no signature uses an algorithm this runtime knows";
        case ApkSignature::Result::DigestMismatch: return "the contents do not match the signed digest";
        case ApkSignature::Result::SignatureMismatch: return "the signature does not verify against the signer key";
        case ApkSignature::Result::KeyMismatch: return "the signing key does not match the leading certificate";
        case ApkSignature::Result::NoSigners: return "the signing block names no signer";
    }

    return "an unnamed failure";
}

const char* ApkSignature::SchemeName(int scheme)
{
    switch (scheme)
    {
        case ApkSignature::SCHEME_JAR: return "v1";
        case ApkSignature::SCHEME_V2: return "v2";
        case ApkSignature::SCHEME_V3: return "v3";
        default: return "none";
    }
}

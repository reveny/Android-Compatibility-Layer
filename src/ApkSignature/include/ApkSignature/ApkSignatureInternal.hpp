#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace ApkSignatureInternal
{
    inline constexpr size_t SHA256_BYTES = 32;
    inline constexpr size_t SHA512_BYTES = 64;
    inline constexpr size_t MAX_DIGEST_BYTES = SHA512_BYTES;

    enum class DigestKind : uint8_t
    {
        Sha256,
        Sha512
    };

    struct Sha256State
    {
        uint32_t words[8];
        uint64_t length;
        uint8_t buffer[64];
        size_t pending;
    };

    struct Sha512State
    {
        uint64_t words[8];
        uint64_t lengthLow;
        uint64_t lengthHigh;
        uint8_t buffer[128];
        size_t pending;
    };

    struct DigestState
    {
        DigestKind kind;
        Sha256State sha256;
        Sha512State sha512;
    };

    struct DerValue
    {
        uint8_t tag;
        const uint8_t* start;
        const uint8_t* content;
        size_t size;
        const uint8_t* next;
    };

    void DigestBegin(DigestState& state, DigestKind kind);
    void DigestUpdate(DigestState& state, const void* data, size_t size);
    void DigestEnd(DigestState& state, std::vector<uint8_t>& out);
    size_t DigestSize(DigestKind kind);

    void Digest(DigestKind kind, const void* data, size_t size, std::vector<uint8_t>& out);

    bool DerRead(const uint8_t* data, size_t size, DerValue& value);
    bool DerEnter(const DerValue& value, DerValue& child);
    bool DerNext(const DerValue& value, const uint8_t* end, DerValue& following);

    bool RsaPublicKeyFromSubjectPublicKeyInfo(const uint8_t* data, size_t size, std::vector<uint8_t>& modulus,
                                              std::vector<uint8_t>& exponent);
    bool RsaRecover(const std::vector<uint8_t>& modulus, const std::vector<uint8_t>& exponent, const uint8_t* signature,
                    size_t signatureSize, std::vector<uint8_t>& out);

    bool VerifyPkcs1(const std::vector<uint8_t>& recovered, DigestKind kind, const std::vector<uint8_t>& digest);
    bool VerifyProbabilistic(const std::vector<uint8_t>& recovered, size_t modulusBits, DigestKind kind,
                             const std::vector<uint8_t>& digest);

    bool CertificatePublicKey(const std::vector<uint8_t>& certificate, std::vector<uint8_t>& subjectPublicKeyInfo);
    std::string CertificateSubject(const std::vector<uint8_t>& certificate);
}

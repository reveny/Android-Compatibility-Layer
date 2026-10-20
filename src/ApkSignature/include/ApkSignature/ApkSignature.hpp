#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ApkSignature
{
    inline constexpr int SCHEME_NONE = 0;
    inline constexpr int SCHEME_JAR = 1;
    inline constexpr int SCHEME_V2 = 2;
    inline constexpr int SCHEME_V3 = 3;

    enum class Result
    {
        Ok,
        Unreadable,
        NotAnArchive,
        Unsigned,
        Malformed,
        UnsupportedAlgorithm,
        DigestMismatch,
        SignatureMismatch,
        KeyMismatch,
        NoSigners
    };

    struct Signer
    {
        std::vector<uint8_t> certificate;
        std::string subject;
        std::string algorithm;
        std::string certificateSha256;
    };

    struct Info
    {
        int scheme;
        bool verified;
        std::vector<Signer> signers;
    };

    Result Verify(const std::string& path, Info& info);
    Result Read(const std::string& path, Info& info);

    std::string DescribeInfo(const Info& info);
    const char* ResultName(Result result);
    const char* SchemeName(int scheme);
}

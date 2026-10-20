#include "ApkSignature/ApkSignatureInternal.hpp"

#include <cstring>

namespace ApkSignatureInternal
{
    static constexpr uint8_t TAG_INTEGER = 0x02;
    static constexpr uint8_t TAG_BIT_STRING = 0x03;
    static constexpr uint8_t TAG_OBJECT_IDENTIFIER = 0x06;
    static constexpr uint8_t TAG_UTF8_STRING = 0x0c;
    static constexpr uint8_t TAG_PRINTABLE_STRING = 0x13;
    static constexpr uint8_t TAG_IA5_STRING = 0x16;
    static constexpr uint8_t TAG_SEQUENCE = 0x30;
    static constexpr uint8_t TAG_SET = 0x31;
    static constexpr uint8_t TAG_CONSTRUCTED = 0x20;

    static constexpr int SEQUENCE_SUBJECT = 4;
    static constexpr int SEQUENCE_PUBLIC_KEY = 5;

    static constexpr uint8_t OID_COMMON_NAME[] = {0x55, 0x04, 0x03};
    static constexpr uint8_t OID_ORGANISATION[] = {0x55, 0x04, 0x0a};

    static constexpr uint8_t DIGEST_INFO_SHA256[] = {0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86, 0x48, 0x01, 0x65,
                                                     0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20};
    static constexpr uint8_t DIGEST_INFO_SHA512[] = {0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, 0x86, 0x48, 0x01, 0x65,
                                                     0x03, 0x04, 0x02, 0x03, 0x05, 0x00, 0x04, 0x40};

    static constexpr uint32_t SHA256_CONSTANTS[64] = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5, 0xd807aa98, 0x12835b01,
        0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147,
        0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116, 0x1e376c08,
        0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2};

    static constexpr uint64_t SHA512_CONSTANTS[80] = {
        0x428a2f98d728ae22ull, 0x7137449123ef65cdull, 0xb5c0fbcfec4d3b2full, 0xe9b5dba58189dbbcull, 0x3956c25bf348b538ull,
        0x59f111f1b605d019ull, 0x923f82a4af194f9bull, 0xab1c5ed5da6d8118ull, 0xd807aa98a3030242ull, 0x12835b0145706fbeull,
        0x243185be4ee4b28cull, 0x550c7dc3d5ffb4e2ull, 0x72be5d74f27b896full, 0x80deb1fe3b1696b1ull, 0x9bdc06a725c71235ull,
        0xc19bf174cf692694ull, 0xe49b69c19ef14ad2ull, 0xefbe4786384f25e3ull, 0x0fc19dc68b8cd5b5ull, 0x240ca1cc77ac9c65ull,
        0x2de92c6f592b0275ull, 0x4a7484aa6ea6e483ull, 0x5cb0a9dcbd41fbd4ull, 0x76f988da831153b5ull, 0x983e5152ee66dfabull,
        0xa831c66d2db43210ull, 0xb00327c898fb213full, 0xbf597fc7beef0ee4ull, 0xc6e00bf33da88fc2ull, 0xd5a79147930aa725ull,
        0x06ca6351e003826full, 0x142929670a0e6e70ull, 0x27b70a8546d22ffcull, 0x2e1b21385c26c926ull, 0x4d2c6dfc5ac42aedull,
        0x53380d139d95b3dfull, 0x650a73548baf63deull, 0x766a0abb3c77b2a8ull, 0x81c2c92e47edaee6ull, 0x92722c851482353bull,
        0xa2bfe8a14cf10364ull, 0xa81a664bbc423001ull, 0xc24b8b70d0f89791ull, 0xc76c51a30654be30ull, 0xd192e819d6ef5218ull,
        0xd69906245565a910ull, 0xf40e35855771202aull, 0x106aa07032bbd1b8ull, 0x19a4c116b8d2d0c8ull, 0x1e376c085141ab53ull,
        0x2748774cdf8eeb99ull, 0x34b0bcb5e19b48a8ull, 0x391c0cb3c5c95a63ull, 0x4ed8aa4ae3418acbull, 0x5b9cca4f7763e373ull,
        0x682e6ff3d6b2b8a3ull, 0x748f82ee5defb2fcull, 0x78a5636f43172f60ull, 0x84c87814a1f0ab72ull, 0x8cc702081a6439ecull,
        0x90befffa23631e28ull, 0xa4506cebde82bde9ull, 0xbef9a3f7b2c67915ull, 0xc67178f2e372532bull, 0xca273eceea26619cull,
        0xd186b8c721c0c207ull, 0xeada7dd6cde0eb1eull, 0xf57d4f7fee6ed178ull, 0x06f067aa72176fbaull, 0x0a637dc5a2c898a6ull,
        0x113f9804bef90daeull, 0x1b710b35131c471bull, 0x28db77f523047d84ull, 0x32caab7b40c72493ull, 0x3c9ebe0a15c9bebcull,
        0x431d67c49c100d4cull, 0x4cc5d4becb3e42b6ull, 0x597f299cfc657e2aull, 0x5fcb6fab3ad6faecull, 0x6c44198c4a475817ull};
}

static uint32_t RotateRight32(uint32_t value, int amount)
{
    return (value >> amount) | (value << (32 - amount));
}

static uint64_t RotateRight64(uint64_t value, int amount)
{
    return (value >> amount) | (value << (64 - amount));
}

static void Sha256Block(ApkSignatureInternal::Sha256State& state, const uint8_t* block)
{
    uint32_t schedule[64];

    for (int index = 0; index < 16; index++)
    {
        schedule[index] = (static_cast<uint32_t>(block[index * 4]) << 24) | (static_cast<uint32_t>(block[index * 4 + 1]) << 16)
                          | (static_cast<uint32_t>(block[index * 4 + 2]) << 8) | static_cast<uint32_t>(block[index * 4 + 3]);
    }

    for (int index = 16; index < 64; index++)
    {
        uint32_t previous = schedule[index - 15];
        uint32_t recent = schedule[index - 2];
        uint32_t lower = RotateRight32(previous, 7) ^ RotateRight32(previous, 18) ^ (previous >> 3);
        uint32_t upper = RotateRight32(recent, 17) ^ RotateRight32(recent, 19) ^ (recent >> 10);

        schedule[index] = schedule[index - 16] + lower + schedule[index - 7] + upper;
    }

    uint32_t a = state.words[0];
    uint32_t b = state.words[1];
    uint32_t c = state.words[2];
    uint32_t d = state.words[3];
    uint32_t e = state.words[4];
    uint32_t f = state.words[5];
    uint32_t g = state.words[6];
    uint32_t h = state.words[7];

    for (int index = 0; index < 64; index++)
    {
        uint32_t sigmaOne = RotateRight32(e, 6) ^ RotateRight32(e, 11) ^ RotateRight32(e, 25);
        uint32_t choice = (e & f) ^ (~e & g);
        uint32_t first = h + sigmaOne + choice + ApkSignatureInternal::SHA256_CONSTANTS[index] + schedule[index];
        uint32_t sigmaZero = RotateRight32(a, 2) ^ RotateRight32(a, 13) ^ RotateRight32(a, 22);
        uint32_t majority = (a & b) ^ (a & c) ^ (b & c);
        uint32_t second = sigmaZero + majority;

        h = g;
        g = f;
        f = e;
        e = d + first;
        d = c;
        c = b;
        b = a;
        a = first + second;
    }

    state.words[0] += a;
    state.words[1] += b;
    state.words[2] += c;
    state.words[3] += d;
    state.words[4] += e;
    state.words[5] += f;
    state.words[6] += g;
    state.words[7] += h;
}

static void Sha512Block(ApkSignatureInternal::Sha512State& state, const uint8_t* block)
{
    uint64_t schedule[80];

    for (int index = 0; index < 16; index++)
    {
        uint64_t value = 0;
        for (int byte = 0; byte < 8; byte++) value = (value << 8) | block[index * 8 + byte];

        schedule[index] = value;
    }

    for (int index = 16; index < 80; index++)
    {
        uint64_t previous = schedule[index - 15];
        uint64_t recent = schedule[index - 2];
        uint64_t lower = RotateRight64(previous, 1) ^ RotateRight64(previous, 8) ^ (previous >> 7);
        uint64_t upper = RotateRight64(recent, 19) ^ RotateRight64(recent, 61) ^ (recent >> 6);

        schedule[index] = schedule[index - 16] + lower + schedule[index - 7] + upper;
    }

    uint64_t a = state.words[0];
    uint64_t b = state.words[1];
    uint64_t c = state.words[2];
    uint64_t d = state.words[3];
    uint64_t e = state.words[4];
    uint64_t f = state.words[5];
    uint64_t g = state.words[6];
    uint64_t h = state.words[7];

    for (int index = 0; index < 80; index++)
    {
        uint64_t sigmaOne = RotateRight64(e, 14) ^ RotateRight64(e, 18) ^ RotateRight64(e, 41);
        uint64_t choice = (e & f) ^ (~e & g);
        uint64_t first = h + sigmaOne + choice + ApkSignatureInternal::SHA512_CONSTANTS[index] + schedule[index];
        uint64_t sigmaZero = RotateRight64(a, 28) ^ RotateRight64(a, 34) ^ RotateRight64(a, 39);
        uint64_t majority = (a & b) ^ (a & c) ^ (b & c);
        uint64_t second = sigmaZero + majority;

        h = g;
        g = f;
        f = e;
        e = d + first;
        d = c;
        c = b;
        b = a;
        a = first + second;
    }

    state.words[0] += a;
    state.words[1] += b;
    state.words[2] += c;
    state.words[3] += d;
    state.words[4] += e;
    state.words[5] += f;
    state.words[6] += g;
    state.words[7] += h;
}

size_t ApkSignatureInternal::DigestSize(ApkSignatureInternal::DigestKind kind)
{
    return kind == ApkSignatureInternal::DigestKind::Sha256 ? ApkSignatureInternal::SHA256_BYTES : ApkSignatureInternal::SHA512_BYTES;
}

void ApkSignatureInternal::DigestBegin(ApkSignatureInternal::DigestState& state, ApkSignatureInternal::DigestKind kind)
{
    state.kind = kind;

    if (kind == ApkSignatureInternal::DigestKind::Sha256)
    {
        state.sha256.words[0] = 0x6a09e667;
        state.sha256.words[1] = 0xbb67ae85;
        state.sha256.words[2] = 0x3c6ef372;
        state.sha256.words[3] = 0xa54ff53a;
        state.sha256.words[4] = 0x510e527f;
        state.sha256.words[5] = 0x9b05688c;
        state.sha256.words[6] = 0x1f83d9ab;
        state.sha256.words[7] = 0x5be0cd19;
        state.sha256.length = 0;
        state.sha256.pending = 0;

        return;
    }

    state.sha512.words[0] = 0x6a09e667f3bcc908ull;
    state.sha512.words[1] = 0xbb67ae8584caa73bull;
    state.sha512.words[2] = 0x3c6ef372fe94f82bull;
    state.sha512.words[3] = 0xa54ff53a5f1d36f1ull;
    state.sha512.words[4] = 0x510e527fade682d1ull;
    state.sha512.words[5] = 0x9b05688c2b3e6c1full;
    state.sha512.words[6] = 0x1f83d9abfb41bd6bull;
    state.sha512.words[7] = 0x5be0cd19137e2179ull;
    state.sha512.lengthLow = 0;
    state.sha512.lengthHigh = 0;
    state.sha512.pending = 0;
}

void ApkSignatureInternal::DigestUpdate(ApkSignatureInternal::DigestState& state, const void* data, size_t size)
{
    const uint8_t* bytes = static_cast<const uint8_t*>(data);

    if (state.kind == ApkSignatureInternal::DigestKind::Sha256)
    {
        state.sha256.length += size;

        while (size > 0)
        {
            size_t take = sizeof(state.sha256.buffer) - state.sha256.pending;
            if (take > size) take = size;

            ::memcpy(state.sha256.buffer + state.sha256.pending, bytes, take);
            state.sha256.pending += take;
            bytes += take;
            size -= take;

            if (state.sha256.pending < sizeof(state.sha256.buffer)) continue;

            Sha256Block(state.sha256, state.sha256.buffer);
            state.sha256.pending = 0;
        }

        return;
    }

    uint64_t before = state.sha512.lengthLow;
    state.sha512.lengthLow += size;
    if (state.sha512.lengthLow < before) state.sha512.lengthHigh += 1;

    while (size > 0)
    {
        size_t take = sizeof(state.sha512.buffer) - state.sha512.pending;
        if (take > size) take = size;

        ::memcpy(state.sha512.buffer + state.sha512.pending, bytes, take);
        state.sha512.pending += take;
        bytes += take;
        size -= take;

        if (state.sha512.pending < sizeof(state.sha512.buffer)) continue;

        Sha512Block(state.sha512, state.sha512.buffer);
        state.sha512.pending = 0;
    }
}

void ApkSignatureInternal::DigestEnd(ApkSignatureInternal::DigestState& state, std::vector<uint8_t>& out)
{
    if (state.kind == ApkSignatureInternal::DigestKind::Sha256)
    {
        uint64_t bits = state.sha256.length * 8;

        state.sha256.buffer[state.sha256.pending++] = 0x80;

        if (state.sha256.pending > 56)
        {
            ::memset(state.sha256.buffer + state.sha256.pending, 0, sizeof(state.sha256.buffer) - state.sha256.pending);
            Sha256Block(state.sha256, state.sha256.buffer);
            state.sha256.pending = 0;
        }

        ::memset(state.sha256.buffer + state.sha256.pending, 0, 56 - state.sha256.pending);

        for (int index = 0; index < 8; index++) state.sha256.buffer[56 + index] = static_cast<uint8_t>(bits >> (56 - index * 8));

        Sha256Block(state.sha256, state.sha256.buffer);

        out.assign(ApkSignatureInternal::SHA256_BYTES, 0);
        for (int index = 0; index < 8; index++)
        {
            out[index * 4] = static_cast<uint8_t>(state.sha256.words[index] >> 24);
            out[index * 4 + 1] = static_cast<uint8_t>(state.sha256.words[index] >> 16);
            out[index * 4 + 2] = static_cast<uint8_t>(state.sha256.words[index] >> 8);
            out[index * 4 + 3] = static_cast<uint8_t>(state.sha256.words[index]);
        }

        return;
    }

    uint64_t bitsLow = state.sha512.lengthLow << 3;
    uint64_t bitsHigh = (state.sha512.lengthHigh << 3) | (state.sha512.lengthLow >> 61);

    state.sha512.buffer[state.sha512.pending++] = 0x80;

    if (state.sha512.pending > 112)
    {
        ::memset(state.sha512.buffer + state.sha512.pending, 0, sizeof(state.sha512.buffer) - state.sha512.pending);
        Sha512Block(state.sha512, state.sha512.buffer);
        state.sha512.pending = 0;
    }

    ::memset(state.sha512.buffer + state.sha512.pending, 0, 112 - state.sha512.pending);

    for (int index = 0; index < 8; index++) state.sha512.buffer[112 + index] = static_cast<uint8_t>(bitsHigh >> (56 - index * 8));
    for (int index = 0; index < 8; index++) state.sha512.buffer[120 + index] = static_cast<uint8_t>(bitsLow >> (56 - index * 8));

    Sha512Block(state.sha512, state.sha512.buffer);

    out.assign(ApkSignatureInternal::SHA512_BYTES, 0);
    for (int index = 0; index < 8; index++)
    {
        for (int byte = 0; byte < 8; byte++)
        {
            out[index * 8 + byte] = static_cast<uint8_t>(state.sha512.words[index] >> (56 - byte * 8));
        }
    }
}

void ApkSignatureInternal::Digest(ApkSignatureInternal::DigestKind kind, const void* data, size_t size, std::vector<uint8_t>& out)
{
    ApkSignatureInternal::DigestState state;

    ApkSignatureInternal::DigestBegin(state, kind);
    ApkSignatureInternal::DigestUpdate(state, data, size);
    ApkSignatureInternal::DigestEnd(state, out);
}

bool ApkSignatureInternal::DerRead(const uint8_t* data, size_t size, ApkSignatureInternal::DerValue& value)
{
    if (data == nullptr || size < 2) return false;

    value.tag = data[0];
    value.start = data;

    size_t at = 1;
    size_t length = 0;

    if ((data[at] & 0x80) == 0)
    {
        length = data[at];
        at += 1;
    }
    else
    {
        size_t lengthBytes = data[at] & 0x7f;
        at += 1;

        if (lengthBytes == 0 || lengthBytes > 4 || at + lengthBytes > size) return false;

        for (size_t index = 0; index < lengthBytes; index++) length = (length << 8) | data[at + index];

        at += lengthBytes;
    }

    if (at + length > size) return false;

    value.content = data + at;
    value.size = length;
    value.next = data + at + length;

    return true;
}

bool ApkSignatureInternal::DerEnter(const ApkSignatureInternal::DerValue& value, ApkSignatureInternal::DerValue& child)
{
    if ((value.tag & ApkSignatureInternal::TAG_CONSTRUCTED) == 0) return false;

    return ApkSignatureInternal::DerRead(value.content, value.size, child);
}

bool ApkSignatureInternal::DerNext(const ApkSignatureInternal::DerValue& value, const uint8_t* end,
                                   ApkSignatureInternal::DerValue& following)
{
    if (value.next >= end) return false;

    return ApkSignatureInternal::DerRead(value.next, static_cast<size_t>(end - value.next), following);
}

bool ApkSignatureInternal::RsaPublicKeyFromSubjectPublicKeyInfo(const uint8_t* data, size_t size, std::vector<uint8_t>& modulus,
                                                                std::vector<uint8_t>& exponent)
{
    modulus.clear();
    exponent.clear();

    ApkSignatureInternal::DerValue outer;
    if (!ApkSignatureInternal::DerRead(data, size, outer) || outer.tag != ApkSignatureInternal::TAG_SEQUENCE) return false;

    ApkSignatureInternal::DerValue algorithm;
    if (!ApkSignatureInternal::DerEnter(outer, algorithm)) return false;

    ApkSignatureInternal::DerValue keyBits;
    if (!ApkSignatureInternal::DerNext(algorithm, outer.content + outer.size, keyBits)) return false;
    if (keyBits.tag != ApkSignatureInternal::TAG_BIT_STRING || keyBits.size < 2) return false;
    if (keyBits.content[0] != 0) return false;

    ApkSignatureInternal::DerValue key;
    if (!ApkSignatureInternal::DerRead(keyBits.content + 1, keyBits.size - 1, key)) return false;
    if (key.tag != ApkSignatureInternal::TAG_SEQUENCE) return false;

    ApkSignatureInternal::DerValue modulusValue;
    if (!ApkSignatureInternal::DerEnter(key, modulusValue) || modulusValue.tag != ApkSignatureInternal::TAG_INTEGER) return false;

    ApkSignatureInternal::DerValue exponentValue;
    if (!ApkSignatureInternal::DerNext(modulusValue, key.content + key.size, exponentValue)) return false;
    if (exponentValue.tag != ApkSignatureInternal::TAG_INTEGER) return false;

    const uint8_t* modulusBytes = modulusValue.content;
    size_t modulusSize = modulusValue.size;

    while (modulusSize > 1 && modulusBytes[0] == 0)
    {
        modulusBytes += 1;
        modulusSize -= 1;
    }

    modulus.assign(modulusBytes, modulusBytes + modulusSize);
    exponent.assign(exponentValue.content, exponentValue.content + exponentValue.size);

    return !modulus.empty() && !exponent.empty();
}

static void TrimLeading(std::vector<uint32_t>& value)
{
    while (value.size() > 1 && value.back() == 0) value.pop_back();
}

static std::vector<uint32_t> FromBytes(const uint8_t* data, size_t size)
{
    std::vector<uint32_t> value;
    value.reserve(size / 4 + 1);

    size_t at = size;

    while (at >= 4)
    {
        at -= 4;
        value.push_back((static_cast<uint32_t>(data[at]) << 24) | (static_cast<uint32_t>(data[at + 1]) << 16)
                        | (static_cast<uint32_t>(data[at + 2]) << 8) | static_cast<uint32_t>(data[at + 3]));
    }

    if (at > 0)
    {
        uint32_t remainder = 0;
        for (size_t index = 0; index < at; index++) remainder = (remainder << 8) | data[index];

        value.push_back(remainder);
    }

    if (value.empty()) value.push_back(0);

    TrimLeading(value);

    return value;
}

static int Compare(const std::vector<uint32_t>& left, const std::vector<uint32_t>& right)
{
    if (left.size() != right.size()) return left.size() < right.size() ? -1 : 1;

    for (size_t index = left.size(); index-- > 0;)
    {
        if (left[index] != right[index]) return left[index] < right[index] ? -1 : 1;
    }

    return 0;
}

static void ShiftLeftOne(std::vector<uint32_t>& value)
{
    uint32_t carry = 0;

    for (size_t index = 0; index < value.size(); index++)
    {
        uint32_t next = value[index] >> 31;
        value[index] = (value[index] << 1) | carry;
        carry = next;
    }

    if (carry != 0) value.push_back(carry);
}

static void Subtract(std::vector<uint32_t>& left, const std::vector<uint32_t>& right)
{
    uint64_t borrow = 0;

    for (size_t index = 0; index < left.size(); index++)
    {
        uint64_t rightWord = index < right.size() ? right[index] : 0;
        uint64_t difference = static_cast<uint64_t>(left[index]) - rightWord - borrow;

        left[index] = static_cast<uint32_t>(difference);
        borrow = (difference >> 32) != 0 ? 1 : 0;
    }

    TrimLeading(left);
}

static std::vector<uint32_t> Multiply(const std::vector<uint32_t>& left, const std::vector<uint32_t>& right)
{
    std::vector<uint32_t> product(left.size() + right.size(), 0);

    for (size_t outer = 0; outer < left.size(); outer++)
    {
        uint64_t carry = 0;

        for (size_t inner = 0; inner < right.size(); inner++)
        {
            uint64_t current = static_cast<uint64_t>(left[outer]) * right[inner] + product[outer + inner] + carry;
            product[outer + inner] = static_cast<uint32_t>(current);
            carry = current >> 32;
        }

        size_t at = outer + right.size();

        while (carry != 0 && at < product.size())
        {
            uint64_t current = product[at] + carry;
            product[at] = static_cast<uint32_t>(current);
            carry = current >> 32;
            at += 1;
        }
    }

    TrimLeading(product);

    return product;
}

static std::vector<uint32_t> Modulo(std::vector<uint32_t> value, const std::vector<uint32_t>& modulus)
{
    if (Compare(value, modulus) < 0) return value;

    const size_t bits = value.size() * 32;
    std::vector<uint32_t> remainder(1, 0);

    for (size_t index = bits; index-- > 0;)
    {
        ShiftLeftOne(remainder);

        uint32_t bit = (value[index / 32] >> (index % 32)) & 1u;
        if (bit != 0) remainder[0] |= 1u;

        if (Compare(remainder, modulus) >= 0) Subtract(remainder, modulus);
    }

    TrimLeading(remainder);

    return remainder;
}

bool ApkSignatureInternal::RsaRecover(const std::vector<uint8_t>& modulus, const std::vector<uint8_t>& exponent,
                                      const uint8_t* signature, size_t signatureSize, std::vector<uint8_t>& out)
{
    out.clear();

    if (modulus.empty() || exponent.empty() || signature == nullptr || signatureSize == 0) return false;
    if (signatureSize != modulus.size()) return false;
    if (modulus.size() > 1024) return false;

    std::vector<uint32_t> modulusWords = FromBytes(modulus.data(), modulus.size());
    std::vector<uint32_t> base = FromBytes(signature, signatureSize);

    if (Compare(base, modulusWords) >= 0) return false;

    std::vector<uint32_t> result(1, 1);
    std::vector<uint32_t> square = base;

    for (size_t byteIndex = exponent.size(); byteIndex-- > 0;)
    {
        uint8_t byte = exponent[byteIndex];

        for (int bit = 0; bit < 8; bit++)
        {
            if (((byte >> bit) & 1u) != 0) result = Modulo(Multiply(result, square), modulusWords);

            if (byteIndex == 0 && bit == 7) break;

            square = Modulo(Multiply(square, square), modulusWords);
        }
    }

    out.assign(modulus.size(), 0);

    for (size_t index = 0; index < out.size(); index++)
    {
        size_t fromEnd = out.size() - 1 - index;
        size_t word = fromEnd / 4;
        size_t shift = (fromEnd % 4) * 8;

        out[index] = word < result.size() ? static_cast<uint8_t>(result[word] >> shift) : 0;
    }

    return true;
}

bool ApkSignatureInternal::VerifyPkcs1(const std::vector<uint8_t>& recovered, ApkSignatureInternal::DigestKind kind,
                                       const std::vector<uint8_t>& digest)
{
    const uint8_t* prefix = kind == ApkSignatureInternal::DigestKind::Sha256 ? ApkSignatureInternal::DIGEST_INFO_SHA256
                                                                            : ApkSignatureInternal::DIGEST_INFO_SHA512;
    const size_t prefixSize = kind == ApkSignatureInternal::DigestKind::Sha256 ? sizeof(ApkSignatureInternal::DIGEST_INFO_SHA256)
                                                                              : sizeof(ApkSignatureInternal::DIGEST_INFO_SHA512);

    const size_t expected = prefixSize + digest.size();
    if (recovered.size() < expected + 11) return false;

    if (recovered[0] != 0x00 || recovered[1] != 0x01) return false;

    size_t at = 2;
    while (at < recovered.size() && recovered[at] == 0xff) at += 1;

    if (at < 10) return false;
    if (at >= recovered.size() || recovered[at] != 0x00) return false;

    at += 1;

    if (recovered.size() - at != expected) return false;
    if (::memcmp(recovered.data() + at, prefix, prefixSize) != 0) return false;

    return ::memcmp(recovered.data() + at + prefixSize, digest.data(), digest.size()) == 0;
}

static void MaskGenerate(ApkSignatureInternal::DigestKind kind, const uint8_t* seed, size_t seedSize, uint8_t* out, size_t outSize)
{
    const size_t digestSize = ApkSignatureInternal::DigestSize(kind);
    size_t produced = 0;
    uint32_t counter = 0;

    while (produced < outSize)
    {
        uint8_t suffix[4];
        suffix[0] = static_cast<uint8_t>(counter >> 24);
        suffix[1] = static_cast<uint8_t>(counter >> 16);
        suffix[2] = static_cast<uint8_t>(counter >> 8);
        suffix[3] = static_cast<uint8_t>(counter);

        ApkSignatureInternal::DigestState state;
        ApkSignatureInternal::DigestBegin(state, kind);
        ApkSignatureInternal::DigestUpdate(state, seed, seedSize);
        ApkSignatureInternal::DigestUpdate(state, suffix, sizeof(suffix));

        std::vector<uint8_t> block;
        ApkSignatureInternal::DigestEnd(state, block);

        size_t take = digestSize;
        if (take > outSize - produced) take = outSize - produced;

        ::memcpy(out + produced, block.data(), take);
        produced += take;
        counter += 1;
    }
}

bool ApkSignatureInternal::VerifyProbabilistic(const std::vector<uint8_t>& recovered, size_t modulusBits,
                                               ApkSignatureInternal::DigestKind kind, const std::vector<uint8_t>& digest)
{
    const size_t digestSize = ApkSignatureInternal::DigestSize(kind);
    const size_t saltSize = digestSize;
    const size_t emBits = modulusBits - 1;
    const size_t emSize = (emBits + 7) / 8;

    if (recovered.size() < emSize) return false;

    const uint8_t* encoded = recovered.data() + (recovered.size() - emSize);

    if (emSize < digestSize + saltSize + 2) return false;
    if (encoded[emSize - 1] != 0xbc) return false;

    const size_t maskedSize = emSize - digestSize - 1;
    const size_t leadingZeroBits = 8 * emSize - emBits;

    if (leadingZeroBits > 0 && (encoded[0] >> (8 - leadingZeroBits)) != 0) return false;

    std::vector<uint8_t> mask(maskedSize, 0);
    MaskGenerate(kind, encoded + maskedSize, digestSize, mask.data(), maskedSize);

    std::vector<uint8_t> recoveredSalt(maskedSize, 0);
    for (size_t index = 0; index < maskedSize; index++) recoveredSalt[index] = encoded[index] ^ mask[index];

    if (leadingZeroBits > 0) recoveredSalt[0] &= static_cast<uint8_t>(0xff >> leadingZeroBits);

    size_t at = 0;
    while (at < maskedSize && recoveredSalt[at] == 0) at += 1;

    if (at >= maskedSize || recoveredSalt[at] != 0x01) return false;

    at += 1;

    if (maskedSize - at != saltSize) return false;

    const uint8_t padding[8] = {0, 0, 0, 0, 0, 0, 0, 0};

    ApkSignatureInternal::DigestState state;
    ApkSignatureInternal::DigestBegin(state, kind);
    ApkSignatureInternal::DigestUpdate(state, padding, sizeof(padding));
    ApkSignatureInternal::DigestUpdate(state, digest.data(), digest.size());
    ApkSignatureInternal::DigestUpdate(state, recoveredSalt.data() + at, saltSize);

    std::vector<uint8_t> expected;
    ApkSignatureInternal::DigestEnd(state, expected);

    return ::memcmp(expected.data(), encoded + maskedSize, digestSize) == 0;
}

static bool FindTopLevelSequence(const std::vector<uint8_t>& certificate, int wanted, ApkSignatureInternal::DerValue& found)
{
    ApkSignatureInternal::DerValue outer;
    if (!ApkSignatureInternal::DerRead(certificate.data(), certificate.size(), outer)) return false;
    if (outer.tag != ApkSignatureInternal::TAG_SEQUENCE) return false;

    ApkSignatureInternal::DerValue toBeSigned;
    if (!ApkSignatureInternal::DerEnter(outer, toBeSigned) || toBeSigned.tag != ApkSignatureInternal::TAG_SEQUENCE) return false;

    const uint8_t* end = toBeSigned.content + toBeSigned.size;

    ApkSignatureInternal::DerValue field;
    if (!ApkSignatureInternal::DerEnter(toBeSigned, field)) return false;

    int sequences = 0;

    for (;;)
    {
        if (field.tag == ApkSignatureInternal::TAG_SEQUENCE)
        {
            sequences += 1;

            if (sequences == wanted)
            {
                found = field;
                return true;
            }
        }

        ApkSignatureInternal::DerValue following;
        if (!ApkSignatureInternal::DerNext(field, end, following)) return false;

        field = following;
    }
}

bool ApkSignatureInternal::CertificatePublicKey(const std::vector<uint8_t>& certificate, std::vector<uint8_t>& subjectPublicKeyInfo)
{
    subjectPublicKeyInfo.clear();

    ApkSignatureInternal::DerValue key;
    if (!FindTopLevelSequence(certificate, ApkSignatureInternal::SEQUENCE_PUBLIC_KEY, key)) return false;

    subjectPublicKeyInfo.assign(key.start, key.next);

    return !subjectPublicKeyInfo.empty();
}

std::string ApkSignatureInternal::CertificateSubject(const std::vector<uint8_t>& certificate)
{
    ApkSignatureInternal::DerValue name;
    if (!FindTopLevelSequence(certificate, ApkSignatureInternal::SEQUENCE_SUBJECT, name)) return std::string();

    ApkSignatureInternal::DerValue relative;
    if (!ApkSignatureInternal::DerEnter(name, relative)) return std::string();

    const uint8_t* end = name.content + name.size;
    std::string subject;

    for (;;)
    {
        ApkSignatureInternal::DerValue pair;

        if (relative.tag == ApkSignatureInternal::TAG_SET && ApkSignatureInternal::DerEnter(relative, pair))
        {
            ApkSignatureInternal::DerValue identifier;

            if (ApkSignatureInternal::DerEnter(pair, identifier) && identifier.tag == ApkSignatureInternal::TAG_OBJECT_IDENTIFIER)
            {
                ApkSignatureInternal::DerValue text;

                if (ApkSignatureInternal::DerNext(identifier, pair.content + pair.size, text))
                {
                    bool isName = identifier.size == sizeof(ApkSignatureInternal::OID_COMMON_NAME)
                                  && ::memcmp(identifier.content, ApkSignatureInternal::OID_COMMON_NAME, identifier.size) == 0;
                    bool isOrganisation = identifier.size == sizeof(ApkSignatureInternal::OID_ORGANISATION)
                                          && ::memcmp(identifier.content, ApkSignatureInternal::OID_ORGANISATION, identifier.size) == 0;
                    bool isText = text.tag == ApkSignatureInternal::TAG_UTF8_STRING
                                  || text.tag == ApkSignatureInternal::TAG_PRINTABLE_STRING
                                  || text.tag == ApkSignatureInternal::TAG_IA5_STRING;

                    if ((isName || isOrganisation) && isText)
                    {
                        if (!subject.empty()) subject += ", ";

                        subject += isName ? "CN=" : "O=";
                        subject.append(reinterpret_cast<const char*>(text.content), text.size);
                    }
                }
            }
        }

        ApkSignatureInternal::DerValue following;
        if (!ApkSignatureInternal::DerNext(relative, end, following)) break;

        relative = following;
    }

    return subject;
}

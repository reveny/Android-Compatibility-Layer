#include <stddef.h>
#include <stdint.h>

struct Node
{
    uint64_t key;
    uint64_t value;
    struct Node* next;
};

uint64_t SumList(const struct Node* head, int limit)
{
    uint64_t total = 0;
    for (int index = 0; head && index < limit; ++index)
    {
        total += head->key ^ head->value;
        head = head->next;
    }
    return total;
}

void CopyStrided(uint8_t* destination, const uint8_t* source, size_t count, size_t stride)
{
    for (size_t index = 0; index < count; ++index)
    {
        destination[index] = source[index * stride];
    }
}

int FindByte(const uint8_t* buffer, size_t length, uint8_t needle)
{
    for (size_t index = 0; index < length; ++index)
    {
        if (buffer[index] == needle) return (int)index;
    }
    return -1;
}

uint32_t Checksum16(const uint16_t* words, size_t count)
{
    uint32_t total = 0;
    for (size_t index = 0; index < count; ++index) total += words[index];
    while (total >> 16) total = (total & 0xffff) + (total >> 16);
    return ~total & 0xffff;
}

void ScaleInPlace(int32_t* values, size_t count, int32_t factor)
{
    for (size_t index = 0; index < count; ++index) values[index] *= factor;
}

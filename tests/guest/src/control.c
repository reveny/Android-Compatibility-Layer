#include <stdint.h>

typedef int (*Operation)(int, int);

static int AddOperation(int a, int b) { return a + b; }
static int SubtractOperation(int a, int b) { return a - b; }
static int MultiplyOperation(int a, int b) { return a * b; }

static Operation OPERATIONS[] = {AddOperation, SubtractOperation, MultiplyOperation};

int Dispatch(int selector, int a, int b)
{
    if (selector < 0 || selector >= 3) return 0;
    return OPERATIONS[selector](a, b);
}

int Classify(int value)
{
    switch (value)
    {
        case 0: return 10;
        case 1: return 20;
        case 2: return 30;
        case 3: return 40;
        case 7: return 70;
        case 11: return 110;
        case 13: return 130;
        default: return -1;
    }
}

uint64_t Fibonacci(int index)
{
    if (index < 2) return (uint64_t)index;
    return Fibonacci(index - 1) + Fibonacci(index - 2);
}

int NestedLoops(int width, int height)
{
    int total = 0;
    for (int y = 0; y < height; ++y)
    {
        for (int x = 0; x < width; ++x)
        {
            if (((x ^ y) & 3) == 0) total += x * y;
            else if (x > y) total -= x;
            else total += 1;
        }
    }
    return total;
}

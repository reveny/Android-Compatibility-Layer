long SumTo(long n)
{
    long total = 0;
    for (long index = 0; index < n; ++index) total += index;
    return total;
}

long CollatzSteps(long n)
{
    long steps = 0;
    while (n > 1)
    {
        n = (n & 1) ? 3 * n + 1 : n / 2;
        steps += 1;
    }
    return steps;
}

long GreatestCommonDivisor(long a, long b)
{
    while (b != 0)
    {
        long carried = b;
        b = a % b;
        a = carried;
    }
    return a;
}

long Branchy(long a, long b)
{
    if (a > b) return a - b;
    if (a == b) return 0;
    return b - a;
}

long NestedLoops(long n)
{
    long total = 0;
    for (long outer = 0; outer < n; ++outer)
    {
        for (long inner = 0; inner < outer; ++inner) total += (outer ^ inner);
    }
    return total;
}

long BitWeights(long value)
{
    long total = 0;
    for (int bit = 0; bit < 64; ++bit)
    {
        if (value & (1L << bit)) total += bit;
    }
    return total;
}

long ShiftMix(long a, long b)
{
    long total = a;
    for (int round = 0; round < 8; ++round)
    {
        total = (total << 3) ^ (total >> 5);
        total += (b >> (round & 7));
        if (total < 0) total = -total;
    }
    return total;
}

long SelectChain(long a, long b, long c)
{
    long best = a > b ? a : b;
    best = best > c ? best : c;
    long worst = a < b ? a : b;
    worst = worst < c ? worst : c;
    return best - worst;
}

long StackSpill(long a, long b, long c, long d, long e, long f, long g, long h)
{
    long values[8];
    values[0] = a;
    values[1] = b;
    values[2] = c;
    values[3] = d;
    values[4] = e;
    values[5] = f;
    values[6] = g;
    values[7] = h;

    long total = 0;
    for (int index = 0; index < 8; ++index)
    {
        total += values[index] * (index + 1);
        if (total > 1000000) total -= 1000000;
    }
    return total;
}

__attribute__((noinline)) static long InnerAccumulate(long n)
{
    long total = 0;
    for (long index = 1; index <= n; ++index) total += (index % 7);
    return total;
}

long CallChain(long n)
{
    long total = 0;
    for (long round = 0; round < 3; ++round) total += InnerAccumulate(n + round);
    return total;
}

long CountdownDivide(long n)
{
    long rounds = 0;
    while (n != 0)
    {
        n = n / 3;
        rounds += 1;
        if (rounds > 100) break;
    }
    return rounds;
}

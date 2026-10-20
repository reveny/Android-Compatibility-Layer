package android.util;

public class Pair<F, S>
{
    public final F first;
    public final S second;

    public Pair(F first, S second)
    {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> create(A first, B second)
    {
        return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Pair)) return false;

        Pair<?, ?> compared = (Pair<?, ?>)other;
        return java.util.Objects.equals(first, compared.first) && java.util.Objects.equals(second, compared.second);
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(first, second);
    }

    @Override
    public String toString()
    {
        return "Pair{" + first + " " + second + "}";
    }
}

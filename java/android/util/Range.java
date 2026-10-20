package android.util;

public final class Range<T extends Comparable<? super T>>
{
    private final T mLower;
    private final T mUpper;

    public Range(T lower, T upper)
    {
        if (lower == null || upper == null) throw new NullPointerException("bounds");
        if (lower.compareTo(upper) > 0) throw new IllegalArgumentException("lower must be less than or equal to upper");

        mLower = lower;
        mUpper = upper;
    }

    public static <T extends Comparable<? super T>> Range<T> create(T lower, T upper)
    {
        return new Range<>(lower, upper);
    }

    public T getLower()
    {
        return mLower;
    }

    public T getUpper()
    {
        return mUpper;
    }

    public boolean contains(T value)
    {
        return mLower.compareTo(value) <= 0 && mUpper.compareTo(value) >= 0;
    }

    public boolean contains(Range<T> other)
    {
        return contains(other.mLower) && contains(other.mUpper);
    }

    public T clamp(T value)
    {
        if (mLower.compareTo(value) > 0) return mLower;
        if (mUpper.compareTo(value) < 0) return mUpper;

        return value;
    }

    public Range<T> intersect(Range<T> other)
    {
        T lower = mLower.compareTo(other.mLower) >= 0 ? mLower : other.mLower;
        T upper = mUpper.compareTo(other.mUpper) <= 0 ? mUpper : other.mUpper;

        return new Range<>(lower, upper);
    }

    public Range<T> extend(Range<T> other)
    {
        T lower = mLower.compareTo(other.mLower) <= 0 ? mLower : other.mLower;
        T upper = mUpper.compareTo(other.mUpper) >= 0 ? mUpper : other.mUpper;

        return new Range<>(lower, upper);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Range)) return false;

        Range<?> candidate = (Range<?>)other;
        return mLower.equals(candidate.mLower) && mUpper.equals(candidate.mUpper);
    }

    @Override
    public int hashCode()
    {
        return mLower.hashCode() * 31 + mUpper.hashCode();
    }

    @Override
    public String toString()
    {
        return "[" + mLower + ", " + mUpper + "]";
    }
}

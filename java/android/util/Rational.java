package android.util;

public final class Rational extends Number implements Comparable<Rational>
{
    private static final long serialVersionUID = 1L;

    public static final Rational NaN = new Rational(0, 0);
    public static final Rational POSITIVE_INFINITY = new Rational(1, 0);
    public static final Rational NEGATIVE_INFINITY = new Rational(-1, 0);
    public static final Rational ZERO = new Rational(0, 1);

    private final int mNumerator;
    private final int mDenominator;

    public Rational(int numerator, int denominator)
    {
        if (denominator < 0)
        {
            numerator = -numerator;
            denominator = -denominator;
        }

        if (denominator == 0 && numerator > 0)
        {
            mNumerator = 1;
            mDenominator = 0;
        }
        else if (denominator == 0 && numerator < 0)
        {
            mNumerator = -1;
            mDenominator = 0;
        }
        else if (denominator == 0)
        {
            mNumerator = 0;
            mDenominator = 0;
        }
        else if (numerator == 0)
        {
            mNumerator = 0;
            mDenominator = 1;
        }
        else
        {
            int divisor = gcd(numerator, denominator);
            mNumerator = numerator / divisor;
            mDenominator = denominator / divisor;
        }
    }

    public int getNumerator()
    {
        return mNumerator;
    }

    public int getDenominator()
    {
        return mDenominator;
    }

    public boolean isNaN()
    {
        return mDenominator == 0 && mNumerator == 0;
    }

    public boolean isInfinite()
    {
        return mNumerator != 0 && mDenominator == 0;
    }

    public boolean isFinite()
    {
        return mDenominator != 0;
    }

    public boolean isZero()
    {
        return isFinite() && mNumerator == 0;
    }

    private boolean isPositiveInfinity()
    {
        return mDenominator == 0 && mNumerator > 0;
    }

    private boolean isNegativeInfinity()
    {
        return mDenominator == 0 && mNumerator < 0;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Rational)) return false;

        Rational rational = (Rational)other;
        return mNumerator == rational.mNumerator && mDenominator == rational.mDenominator;
    }

    @Override
    public String toString()
    {
        if (isNaN()) return "NaN";
        if (isPositiveInfinity()) return "Infinity";
        if (isNegativeInfinity()) return "-Infinity";

        return mNumerator + "/" + mDenominator;
    }

    @Override
    public int hashCode()
    {
        int numeratorFlipped = mNumerator << 16 | mNumerator >>> 16;
        return mDenominator ^ numeratorFlipped;
    }

    private static int gcd(int numerator, int denominator)
    {
        int first = numerator;
        int second = denominator;

        while (second != 0)
        {
            int previous = second;
            second = first % second;
            first = previous;
        }

        return Math.abs(first);
    }

    @Override
    public double doubleValue()
    {
        return (double)mNumerator / (double)mDenominator;
    }

    @Override
    public float floatValue()
    {
        return (float)mNumerator / (float)mDenominator;
    }

    @Override
    public int intValue()
    {
        if (isPositiveInfinity()) return Integer.MAX_VALUE;
        if (isNegativeInfinity()) return Integer.MIN_VALUE;
        if (isNaN()) return 0;

        return mNumerator / mDenominator;
    }

    @Override
    public long longValue()
    {
        if (isPositiveInfinity()) return Long.MAX_VALUE;
        if (isNegativeInfinity()) return Long.MIN_VALUE;
        if (isNaN()) return 0;

        return mNumerator / mDenominator;
    }

    @Override
    public short shortValue()
    {
        return (short)intValue();
    }

    @Override
    public int compareTo(Rational other)
    {
        if (other == null) throw new NullPointerException("another must not be null");

        if (equals(other)) return 0;
        if (isNaN()) return 1;
        if (other.isNaN()) return -1;
        if (isPositiveInfinity() || other.isNegativeInfinity()) return 1;
        if (isNegativeInfinity() || other.isPositiveInfinity()) return -1;

        long thisNumerator = (long)mNumerator * other.mDenominator;
        long otherNumerator = (long)other.mNumerator * mDenominator;

        if (thisNumerator < otherNumerator) return -1;
        if (thisNumerator > otherNumerator) return 1;
        return 0;
    }

    public static Rational parseRational(String string) throws NumberFormatException
    {
        if (string == null) throw new NullPointerException("string must not be null");

        if (string.equals("NaN")) return NaN;
        if (string.equals("Infinity")) return POSITIVE_INFINITY;
        if (string.equals("-Infinity")) return NEGATIVE_INFINITY;

        int separator = string.indexOf(':');
        if (separator < 0) separator = string.indexOf('/');
        if (separator < 0) throw invalidRational(string, "missing colon or slash");

        try
        {
            return new Rational(Integer.parseInt(string.substring(0, separator)), Integer.parseInt(string.substring(separator + 1)));
        }
        catch (NumberFormatException failure)
        {
            throw invalidRational(string, "numerator or denominator not an integer");
        }
    }

    private static NumberFormatException invalidRational(String string, String cause)
    {
        return new NumberFormatException("Invalid Rational: \"" + string + "\"");
    }
}

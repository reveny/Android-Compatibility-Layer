package android.util;

public final class Half extends Number implements Comparable<Half>
{
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 16;
    public static final short EPSILON = (short)0x1400;
    public static final short LOWEST_VALUE = (short)0xfbff;
    public static final int MAX_EXPONENT = 15;
    public static final short MAX_VALUE = (short)0x7bff;
    public static final int MIN_EXPONENT = -14;
    public static final short MIN_NORMAL = (short)0x0400;
    public static final short MIN_VALUE = (short)0x0001;
    public static final short NaN = (short)0x7e00;
    public static final short NEGATIVE_INFINITY = (short)0xfc00;
    public static final short NEGATIVE_ZERO = (short)0x8000;
    public static final short POSITIVE_INFINITY = (short)0x7c00;
    public static final short POSITIVE_ZERO = (short)0x0000;

    private static final int FP16_SIGN_SHIFT = 15;
    private static final int FP16_SIGN_MASK = 0x8000;
    private static final int FP16_EXPONENT_SHIFT = 10;
    private static final int FP16_EXPONENT_MASK = 0x1f;
    private static final int FP16_SIGNIFICAND_MASK = 0x3ff;
    private static final int FP16_EXPONENT_BIAS = 15;
    private static final int FP16_COMBINED = 0x7fff;
    private static final int FP16_EXPONENT_MAX = 0x7c00;

    private static final int FP32_SIGN_SHIFT = 31;
    private static final int FP32_EXPONENT_SHIFT = 23;
    private static final int FP32_EXPONENT_MASK = 0xff;
    private static final int FP32_SIGNIFICAND_MASK = 0x7fffff;
    private static final int FP32_EXPONENT_BIAS = 127;
    private static final int FP32_QNAN_MASK = 0x400000;
    private static final int FP32_DENORMAL_MAGIC = 126 << 23;
    private static final float FP32_DENORMAL_FLOAT = Float.intBitsToFloat(FP32_DENORMAL_MAGIC);

    private final short mValue;

    public Half(short value)
    {
        mValue = value;
    }

    public Half(float value)
    {
        mValue = toHalf(value);
    }

    public Half(double value)
    {
        mValue = toHalf((float)value);
    }

    public Half(String value) throws NumberFormatException
    {
        mValue = toHalf(Float.parseFloat(value));
    }

    public short halfValue()
    {
        return mValue;
    }

    @Override
    public byte byteValue()
    {
        return (byte)toFloat(mValue);
    }

    @Override
    public short shortValue()
    {
        return (short)toFloat(mValue);
    }

    @Override
    public int intValue()
    {
        return (int)toFloat(mValue);
    }

    @Override
    public long longValue()
    {
        return (long)toFloat(mValue);
    }

    @Override
    public float floatValue()
    {
        return toFloat(mValue);
    }

    @Override
    public double doubleValue()
    {
        return toFloat(mValue);
    }

    public boolean isNaN()
    {
        return isNaN(mValue);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Half && halfToIntBits(((Half)other).mValue) == halfToIntBits(mValue);
    }

    @Override
    public int hashCode()
    {
        return hashCode(mValue);
    }

    @Override
    public String toString()
    {
        return toString(mValue);
    }

    @Override
    public int compareTo(Half other)
    {
        return compare(mValue, other.mValue);
    }

    public static int hashCode(short half)
    {
        return halfToIntBits(half);
    }

    public static int compare(short first, short second)
    {
        if (less(first, second)) return -1;
        if (greater(first, second)) return 1;

        short firstBits = (first & FP16_COMBINED) > FP16_EXPONENT_MAX ? NaN : first;
        short secondBits = (second & FP16_COMBINED) > FP16_EXPONENT_MAX ? NaN : second;

        return firstBits == secondBits ? 0 : (firstBits < secondBits ? -1 : 1);
    }

    public static short halfToShortBits(short half)
    {
        return (half & FP16_COMBINED) > FP16_EXPONENT_MAX ? NaN : half;
    }

    public static int halfToIntBits(short half)
    {
        return (half & FP16_COMBINED) > FP16_EXPONENT_MAX ? NaN : half & 0xffff;
    }

    public static int halfToRawIntBits(short half)
    {
        return half & 0xffff;
    }

    public static short intBitsToHalf(int bits)
    {
        return (short)(bits & 0xffff);
    }

    public static short copySign(short magnitude, short sign)
    {
        return (short)((sign & FP16_SIGN_MASK) | (magnitude & FP16_COMBINED));
    }

    public static short abs(short half)
    {
        return (short)(half & FP16_COMBINED);
    }

    public static short round(short half)
    {
        int bits = half & 0xffff;
        int exponent = bits & 0x7fff;
        int result = bits;

        if (exponent < 0x3c00)
        {
            result &= FP16_SIGN_MASK;
            result |= 0x3c00 & (exponent >= 0x3800 ? 0xffff : 0x0);
        }
        else if (exponent < 0x6400)
        {
            exponent = 25 - (exponent >> 10);
            int mask = (1 << exponent) - 1;
            result += 1 << (exponent - 1);
            result &= ~mask;
        }

        return (short)result;
    }

    public static short ceil(short half)
    {
        int bits = half & 0xffff;
        int exponent = bits & 0x7fff;
        int result = bits;

        if (exponent < 0x3c00)
        {
            result &= FP16_SIGN_MASK;
            result |= 0x3c00 & -(~(bits >> 15) & (exponent != 0 ? 1 : 0));
        }
        else if (exponent < 0x6400)
        {
            exponent = 25 - (exponent >> 10);
            int mask = (1 << exponent) - 1;
            result += mask & ((bits >> 15) - 1);
            result &= ~mask;
        }

        return (short)result;
    }

    public static short floor(short half)
    {
        int bits = half & 0xffff;
        int exponent = bits & 0x7fff;
        int result = bits;

        if (exponent < 0x3c00)
        {
            result &= FP16_SIGN_MASK;
            result |= 0x3c00 & (bits > 0x8000 ? 0xffff : 0x0);
        }
        else if (exponent < 0x6400)
        {
            exponent = 25 - (exponent >> 10);
            int mask = (1 << exponent) - 1;
            result += mask & -(bits >> 15);
            result &= ~mask;
        }

        return (short)result;
    }

    public static short trunc(short half)
    {
        int bits = half & 0xffff;
        int exponent = bits & 0x7fff;
        int result = bits;

        if (exponent < 0x3c00)
        {
            result &= FP16_SIGN_MASK;
        }
        else if (exponent < 0x6400)
        {
            exponent = 25 - (exponent >> 10);
            int mask = (1 << exponent) - 1;
            result &= ~mask;
        }

        return (short)result;
    }

    public static short min(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return NaN;
        if ((first & FP16_COMBINED) == 0 && (second & FP16_COMBINED) == 0) return (first & FP16_SIGN_MASK) != 0 ? first : second;

        return toCompareValue(first) < toCompareValue(second) ? first : second;
    }

    public static short max(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return NaN;
        if ((first & FP16_COMBINED) == 0 && (second & FP16_COMBINED) == 0) return (first & FP16_SIGN_MASK) != 0 ? second : first;

        return toCompareValue(first) > toCompareValue(second) ? first : second;
    }

    public static boolean less(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return false;

        return toCompareValue(first) < toCompareValue(second);
    }

    public static boolean lessEquals(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return false;

        return toCompareValue(first) <= toCompareValue(second);
    }

    public static boolean greater(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return false;

        return toCompareValue(first) > toCompareValue(second);
    }

    public static boolean greaterEquals(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return false;

        return toCompareValue(first) >= toCompareValue(second);
    }

    public static boolean equals(short first, short second)
    {
        if (isNaN(first) || isNaN(second)) return false;

        return first == second || ((first | second) & FP16_COMBINED) == 0;
    }

    public static int getSign(short half)
    {
        return (half & FP16_SIGN_MASK) == 0 ? 1 : -1;
    }

    public static int getExponent(short half)
    {
        return ((half >>> FP16_EXPONENT_SHIFT) & FP16_EXPONENT_MASK) - FP16_EXPONENT_BIAS;
    }

    public static int getSignificand(short half)
    {
        return half & FP16_SIGNIFICAND_MASK;
    }

    public static boolean isInfinite(short half)
    {
        return (half & FP16_COMBINED) == FP16_EXPONENT_MAX;
    }

    public static boolean isNaN(short half)
    {
        return (half & FP16_COMBINED) > FP16_EXPONENT_MAX;
    }

    public static boolean isNormalized(short half)
    {
        return (half & FP16_EXPONENT_MAX) != 0 && (half & FP16_EXPONENT_MAX) != FP16_EXPONENT_MAX;
    }

    public static float toFloat(short half)
    {
        int bits = half & 0xffff;
        int sign = bits & FP16_SIGN_MASK;
        int exponent = (bits >>> FP16_EXPONENT_SHIFT) & FP16_EXPONENT_MASK;
        int significand = bits & FP16_SIGNIFICAND_MASK;

        int outExponent = 0;
        int outSignificand = 0;

        if (exponent == 0)
        {
            if (significand != 0)
            {
                float value = Float.intBitsToFloat(FP32_DENORMAL_MAGIC + significand);
                value -= FP32_DENORMAL_FLOAT;
                return sign == 0 ? value : -value;
            }
        }
        else
        {
            outSignificand = significand << 13;
            if (exponent == 0x1f)
            {
                outExponent = 0xff;
                if (outSignificand != 0) outSignificand |= FP32_QNAN_MASK;
            }
            else
            {
                outExponent = exponent - FP16_EXPONENT_BIAS + FP32_EXPONENT_BIAS;
            }
        }

        int out = (sign << 16) | (outExponent << FP32_EXPONENT_SHIFT) | outSignificand;
        return Float.intBitsToFloat(out);
    }

    public static short toHalf(float value)
    {
        int bits = Float.floatToRawIntBits(value);
        int sign = bits >>> FP32_SIGN_SHIFT;
        int exponent = (bits >>> FP32_EXPONENT_SHIFT) & FP32_EXPONENT_MASK;
        int significand = bits & FP32_SIGNIFICAND_MASK;

        int outExponent = 0;
        int outSignificand = 0;

        if (exponent == 0xff)
        {
            outExponent = 0x1f;
            outSignificand = significand != 0 ? 0x200 : 0;
        }
        else
        {
            exponent = exponent - FP32_EXPONENT_BIAS + FP16_EXPONENT_BIAS;
            if (exponent >= 0x1f)
            {
                outExponent = 0x1f;
            }
            else if (exponent <= 0)
            {
                if (exponent >= -10)
                {
                    significand = significand | 0x800000;
                    int shift = 14 - exponent;
                    outSignificand = significand >> shift;

                    int lowSignificand = significand & ((1 << shift) - 1);
                    int halfway = 1 << (shift - 1);
                    if (lowSignificand + (outSignificand & 1) > halfway) outSignificand++;
                }
            }
            else
            {
                outExponent = exponent;
                outSignificand = significand >> 13;
                if ((significand & 0x1fff) + (outSignificand & 0x1) > 0x1000) outSignificand++;
            }
        }

        return (short)((sign << FP16_SIGN_SHIFT) | (outExponent << FP16_EXPONENT_SHIFT) + outSignificand);
    }

    public static Half valueOf(short half)
    {
        return new Half(half);
    }

    public static Half valueOf(float value)
    {
        return new Half(value);
    }

    public static Half valueOf(String value)
    {
        return new Half(value);
    }

    public static short parseHalf(String value) throws NumberFormatException
    {
        return toHalf(Float.parseFloat(value));
    }

    public static String toString(short half)
    {
        return Float.toString(toFloat(half));
    }

    public static String toHexString(short half)
    {
        StringBuilder out = new StringBuilder();

        int bits = half & 0xffff;
        int sign = bits >>> FP16_SIGN_SHIFT;
        int exponent = (bits >>> FP16_EXPONENT_SHIFT) & FP16_EXPONENT_MASK;
        int significand = bits & FP16_SIGNIFICAND_MASK;

        if (exponent == 0x1f)
        {
            if (significand == 0)
            {
                if (sign != 0) out.append('-');
                out.append("Infinity");
            }
            else
            {
                out.append("NaN");
            }
            return out.toString();
        }

        if (sign == 1) out.append('-');

        if (exponent == 0)
        {
            if (significand == 0)
            {
                out.append("0x0.0p0");
            }
            else
            {
                out.append("0x0.");
                out.append(Integer.toHexString(significand).replaceFirst("0{2,}$", ""));
                out.append("p-14");
            }
        }
        else
        {
            out.append("0x1.");
            out.append(Integer.toHexString(significand).replaceFirst("0{2,}$", ""));
            out.append('p');
            out.append(exponent - FP16_EXPONENT_BIAS);
        }

        return out.toString();
    }

    private static int toCompareValue(short half)
    {
        return (half & FP16_SIGN_MASK) != 0 ? 0x8000 - (half & 0xffff) : half & 0xffff;
    }
}

package android.util;

public class TypedValue
{
    public static final int TYPE_NULL = 0x00;
    public static final int TYPE_REFERENCE = 0x01;
    public static final int TYPE_ATTRIBUTE = 0x02;
    public static final int TYPE_STRING = 0x03;
    public static final int TYPE_FLOAT = 0x04;
    public static final int TYPE_DIMENSION = 0x05;
    public static final int TYPE_FRACTION = 0x06;
    public static final int TYPE_FIRST_INT = 0x10;
    public static final int TYPE_INT_DEC = 0x10;
    public static final int TYPE_INT_HEX = 0x11;
    public static final int TYPE_INT_BOOLEAN = 0x12;
    public static final int TYPE_FIRST_COLOR_INT = 0x1c;
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;
    public static final int TYPE_LAST_COLOR_INT = 0x1f;
    public static final int TYPE_LAST_INT = 0x1f;

    public static final int COMPLEX_UNIT_PX = 0;
    public static final int COMPLEX_UNIT_DIP = 1;
    public static final int COMPLEX_UNIT_SP = 2;
    public static final int COMPLEX_UNIT_PT = 3;
    public static final int COMPLEX_UNIT_IN = 4;
    public static final int COMPLEX_UNIT_MM = 5;
    public static final int COMPLEX_UNIT_FRACTION = 0;
    public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

    public static final int COMPLEX_UNIT_SHIFT = 0;
    public static final int COMPLEX_UNIT_MASK = 0xf;
    public static final int COMPLEX_RADIX_SHIFT = 4;
    public static final int COMPLEX_RADIX_MASK = 0x3;
    public static final int COMPLEX_MANTISSA_SHIFT = 8;
    public static final int COMPLEX_MANTISSA_MASK = 0xffffff;

    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_NONE = 0xffff;

    private static final float MANTISSA_MULTIPLIER = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTIPLIERS = {MANTISSA_MULTIPLIER, MANTISSA_MULTIPLIER / (1 << 7),
                                                      MANTISSA_MULTIPLIER / (1 << 15), MANTISSA_MULTIPLIER / (1 << 23)};

    public int type;
    public CharSequence string;
    public int data;
    public int assetCookie;
    public int resourceId;
    public int changingConfigurations = -1;
    public int density;
    public int sourceResourceId;

    public final float getFloat()
    {
        return Float.intBitsToFloat(data);
    }

    public int getComplexUnit()
    {
        return (data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK;
    }

    public static float complexToFloat(int complex)
    {
        return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
                * RADIX_MULTIPLIERS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
    }

    public static float complexToDimension(int data, DisplayMetrics metrics)
    {
        return applyDimension((data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK, complexToFloat(data), metrics);
    }

    public static int complexToDimensionPixelOffset(int data, DisplayMetrics metrics)
    {
        return (int)complexToDimension(data, metrics);
    }

    public static int complexToDimensionPixelSize(int data, DisplayMetrics metrics)
    {
        float value = complexToDimension(data, metrics);
        int rounded = (int)(value >= 0 ? value + 0.5f : value - 0.5f);

        if (rounded != 0) return rounded;
        if (value == 0) return 0;
        return value > 0 ? 1 : -1;
    }

    public static float applyDimension(int unit, float value, DisplayMetrics metrics)
    {
        switch (unit)
        {
            case COMPLEX_UNIT_PX: return value;
            case COMPLEX_UNIT_DIP: return value * metrics.density;
            case COMPLEX_UNIT_SP: return value * metrics.scaledDensity;
            case COMPLEX_UNIT_PT: return value * metrics.xdpi * (1.0f / 72);
            case COMPLEX_UNIT_IN: return value * metrics.xdpi;
            case COMPLEX_UNIT_MM: return value * metrics.xdpi * (1.0f / 25.4f);
            default: return 0;
        }
    }

    public static float deriveDimension(int unitToConvertTo, float pixelValue, DisplayMetrics metrics)
    {
        float base = applyDimension(unitToConvertTo, 1.0f, metrics);
        return base == 0 ? 0 : pixelValue / base;
    }

    public final float getFraction(float base, float pbase)
    {
        return complexToFraction(data, base, pbase);
    }

    public static float complexToFraction(int data, float base, float pbase)
    {
        int unit = (data >> COMPLEX_UNIT_SHIFT) & COMPLEX_UNIT_MASK;
        float value = complexToFloat(data);
        return unit == COMPLEX_UNIT_FRACTION ? value * base : value * pbase;
    }

    public final CharSequence coerceToString()
    {
        if (type == TYPE_STRING) return string;
        return coerceToString(type, data);
    }

    public static String coerceToString(int type, int data)
    {
        switch (type)
        {
            case TYPE_NULL: return null;
            case TYPE_REFERENCE: return "@" + data;
            case TYPE_ATTRIBUTE: return "?" + data;
            case TYPE_FLOAT: return Float.toString(Float.intBitsToFloat(data));
            case TYPE_INT_HEX: return "0x" + Integer.toHexString(data);
            case TYPE_INT_BOOLEAN: return data != 0 ? "true" : "false";
            default: break;
        }

        if (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT) return "#" + Integer.toHexString(data);
        if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) return Integer.toString(data);
        return null;
    }

    public void setTo(TypedValue other)
    {
        type = other.type;
        string = other.string;
        data = other.data;
        assetCookie = other.assetCookie;
        resourceId = other.resourceId;
        changingConfigurations = other.changingConfigurations;
        density = other.density;
        sourceResourceId = other.sourceResourceId;
    }

    @Override
    public String toString()
    {
        return "TypedValue{t=0x" + Integer.toHexString(type) + "/d=0x" + Integer.toHexString(data) + "}";
    }
}

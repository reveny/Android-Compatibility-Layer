package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.util.AttributeSet;
import android.util.TypedValue;

final class AnimationAttributes
{
    private final Context mContext;
    private final AttributeSet mAttributes;

    AnimationAttributes(Context context, AttributeSet attributes)
    {
        mContext = context;
        mAttributes = attributes;
    }

    Context getContext()
    {
        return mContext;
    }

    private int indexOf(String name)
    {
        if (mAttributes == null) return -1;

        int count = mAttributes.getAttributeCount();
        for (int index = 0; index < count; index++)
        {
            String attribute = mAttributes.getAttributeName(index);
            if (attribute == null) continue;

            int colon = attribute.indexOf(':');
            if (colon >= 0) attribute = attribute.substring(colon + 1);

            if (attribute.equals(name)) return index;
        }

        return -1;
    }

    boolean has(String name)
    {
        return indexOf(name) >= 0;
    }

    TypedValue getValue(String name)
    {
        int index = indexOf(name);
        if (index < 0) return null;

        TypedValue value = new TypedValue();

        if (mAttributes instanceof XmlBlock.Parser)
        {
            int[] packed = ((XmlBlock.Parser)mAttributes).getAttributeTypedValue(index);
            if (packed != null)
            {
                value.type = packed[0];
                value.data = packed[1];

                if (value.type == TypedValue.TYPE_STRING) value.string = mAttributes.getAttributeValue(index);

                return resolve(value);
            }
        }

        return parse(mAttributes.getAttributeValue(index));
    }

    private TypedValue resolve(TypedValue value)
    {
        Resources resources = mContext == null ? null : mContext.getResources();

        for (int depth = 0; depth < 8 && value.type == TypedValue.TYPE_REFERENCE && resources != null; depth++)
        {
            int resourceId = value.data;
            if (resourceId == 0) return value;

            TypedValue resolved = new TypedValue();
            try
            {
                resources.getValue(resourceId, resolved, true);
            }
            catch (RuntimeException failure)
            {
                return value;
            }

            if (resolved.resourceId == 0) resolved.resourceId = resourceId;
            value = resolved;
        }

        return value;
    }

    static TypedValue parse(String text)
    {
        if (text == null) return null;

        String trimmed = text.trim();
        TypedValue value = new TypedValue();

        if (trimmed.startsWith("@"))
        {
            value.type = TypedValue.TYPE_STRING;
            value.string = trimmed;
            return value;
        }

        if (trimmed.equals("true") || trimmed.equals("false"))
        {
            value.type = TypedValue.TYPE_INT_BOOLEAN;
            value.data = trimmed.equals("true") ? 1 : 0;
            return value;
        }

        try
        {
            if (trimmed.endsWith("%p"))
            {
                value.type = TypedValue.TYPE_FRACTION;
                value.data = encodeFraction(Float.parseFloat(trimmed.substring(0, trimmed.length() - 2)) / 100.0f,
                                            TypedValue.COMPLEX_UNIT_FRACTION_PARENT);
                return value;
            }

            if (trimmed.endsWith("%"))
            {
                value.type = TypedValue.TYPE_FRACTION;
                value.data = encodeFraction(Float.parseFloat(trimmed.substring(0, trimmed.length() - 1)) / 100.0f,
                                            TypedValue.COMPLEX_UNIT_FRACTION);
                return value;
            }

            if (trimmed.startsWith("0x") || trimmed.startsWith("0X"))
            {
                value.type = TypedValue.TYPE_INT_HEX;
                value.data = (int)Long.parseLong(trimmed.substring(2), 16);
                return value;
            }

            String numeric = trimmed;
            if (numeric.endsWith("dip")) numeric = numeric.substring(0, numeric.length() - 3);
            else if (numeric.endsWith("px")) numeric = numeric.substring(0, numeric.length() - 2);
            else if (numeric.endsWith("dp") || numeric.endsWith("sp")) numeric = numeric.substring(0, numeric.length() - 2);

            if (numeric.indexOf('.') < 0 && numeric.indexOf('e') < 0 && numeric.indexOf('E') < 0)
            {
                value.type = TypedValue.TYPE_INT_DEC;
                value.data = Integer.parseInt(numeric);
                return value;
            }

            value.type = TypedValue.TYPE_FLOAT;
            value.data = Float.floatToIntBits(Float.parseFloat(numeric));
            return value;
        }
        catch (NumberFormatException failure)
        {
            value.type = TypedValue.TYPE_STRING;
            value.string = trimmed;
            return value;
        }
    }

    private static int encodeFraction(float fraction, int unit)
    {
        float magnitude = Math.abs(fraction);
        int radix;
        int shift;

        if (magnitude < 1.0f)
        {
            radix = 3;
            shift = 23;
        }
        else if (magnitude < 256.0f)
        {
            radix = 2;
            shift = 15;
        }
        else if (magnitude < 65536.0f)
        {
            radix = 1;
            shift = 7;
        }
        else
        {
            radix = 0;
            shift = 0;
        }

        int mantissa = Math.round(fraction * (1 << shift));
        return ((mantissa & TypedValue.COMPLEX_MANTISSA_MASK) << TypedValue.COMPLEX_MANTISSA_SHIFT)
               | (radix << TypedValue.COMPLEX_RADIX_SHIFT) | unit;
    }

    float getFloat(String name, float fallback)
    {
        TypedValue value = getValue(name);
        if (value == null) return fallback;

        if (value.type == TypedValue.TYPE_FLOAT) return value.getFloat();
        if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) return value.data;
        if (value.type == TypedValue.TYPE_DIMENSION) return value.getDimension(displayMetrics());
        if (value.type == TypedValue.TYPE_FRACTION) return TypedValue.complexToFloat(value.data);

        return fallback;
    }

    int getInt(String name, int fallback)
    {
        TypedValue value = getValue(name);
        if (value == null) return fallback;

        if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) return value.data;
        if (value.type == TypedValue.TYPE_FLOAT) return (int)value.getFloat();

        return fallback;
    }

    boolean getBoolean(String name, boolean fallback)
    {
        TypedValue value = getValue(name);
        if (value == null) return fallback;

        if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) return value.data != 0;

        return fallback;
    }

    int getEnum(String name, String[] options, int[] values, int fallback)
    {
        int index = indexOf(name);
        if (index < 0) return fallback;

        TypedValue value = getValue(name);
        if (value != null && value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) return value.data;

        String text = mAttributes.getAttributeValue(index);
        if (text == null) return fallback;

        for (int option = 0; option < options.length; option++)
        {
            if (options[option].equals(text.trim())) return values[option];
        }

        return fallback;
    }

    int getColor(String name, int fallback)
    {
        TypedValue value = getValue(name);
        if (value == null) return fallback;

        if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) return value.data;

        return fallback;
    }

    int getResourceId(String name, int fallback)
    {
        int index = indexOf(name);
        if (index < 0) return fallback;

        int resourceId = mAttributes.getAttributeResourceValue(index, 0);
        if (resourceId != 0) return resourceId;

        String text = mAttributes.getAttributeValue(index);
        if (text != null && text.startsWith("@"))
        {
            String reference = text.substring(1);

            try
            {
                return (int)Long.parseLong(reference);
            }
            catch (NumberFormatException failure)
            {
                if (mContext == null || mContext.getResources() == null) return fallback;

                int identifier = mContext.getResources().getIdentifier(reference, null, mContext.getPackageName());
                return identifier != 0 ? identifier : fallback;
            }
        }

        return fallback;
    }

    Animation.Description getDescription(String name)
    {
        Animation.Description description = new Animation.Description();
        TypedValue value = getValue(name);

        if (value == null)
        {
            description.type = Animation.ABSOLUTE;
            description.value = 0;
            return description;
        }

        if (value.type == TypedValue.TYPE_FRACTION)
        {
            description.type = (value.data & TypedValue.COMPLEX_UNIT_MASK) == TypedValue.COMPLEX_UNIT_FRACTION_PARENT
                                   ? Animation.RELATIVE_TO_PARENT : Animation.RELATIVE_TO_SELF;
            description.value = TypedValue.complexToFloat(value.data);
            return description;
        }

        description.type = Animation.ABSOLUTE;

        if (value.type == TypedValue.TYPE_FLOAT) description.value = value.getFloat();
        else if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) description.value = value.data;
        else if (value.type == TypedValue.TYPE_DIMENSION) description.value = value.getDimension(displayMetrics());
        else description.value = 0;

        return description;
    }

    private android.util.DisplayMetrics displayMetrics()
    {
        if (mContext != null && mContext.getResources() != null && mContext.getResources().getDisplayMetrics() != null)
        {
            return mContext.getResources().getDisplayMetrics();
        }

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        metrics.density = 1.0f;
        metrics.scaledDensity = 1.0f;
        metrics.xdpi = 160.0f;
        return metrics;
    }
}

package android.graphics.drawable;

import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.util.AttributeSet;
import android.util.TypedValue;

import org.xmlpull.v1.XmlPullParser;

public class DrawableInflater
{
    public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public static Drawable inflate(Resources resources, XmlPullParser parser, AttributeSet attributes)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        String name = parser.getName();
        if (name == null) return null;

        Drawable drawable = forTag(name);
        if (drawable == null) return null;

        drawable.inflate(resources, parser, attributes);
        return drawable;
    }

    public static Drawable forTag(String name)
    {
        switch (name)
        {
            case "vector": return new VectorDrawable();
            case "shape": return new GradientDrawable();
            case "color": return new ColorDrawable(0);
            case "bitmap": return new BitmapDrawable();
            case "layer-list": return new LayerDrawable();
            case "inset": return new InsetDrawable();
            case "selector": return new StateListDrawable();
            case "ripple": return new RippleDrawable();
            case "adaptive-icon": return new AdaptiveIconDrawable();
            default: return null;
        }
    }

    static int[] typedValue(AttributeSet attributes, String name)
    {
        if (!(attributes instanceof XmlBlock.Parser)) return null;

        return ((XmlBlock.Parser) attributes).getAttributeTypedValue(ANDROID_NAMESPACE, name);
    }

    static float dimension(Resources resources, AttributeSet attributes, String name, float fallback)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null && packed[0] == TypedValue.TYPE_DIMENSION)
        {
            return TypedValue.complexToDimension(packed[1], resources.getDisplayMetrics());
        }

        return dimensionOrFloat(resources, attributes, name, fallback);
    }

    static float dimensionOrFloat(Resources resources, AttributeSet attributes, String name, float fallback)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null)
        {
            switch (packed[0])
            {
                case TypedValue.TYPE_FLOAT:
                    return Float.intBitsToFloat(packed[1]);

                case TypedValue.TYPE_DIMENSION:
                    return TypedValue.complexToDimension(packed[1], resources.getDisplayMetrics());

                case TypedValue.TYPE_INT_DEC:
                case TypedValue.TYPE_INT_HEX:
                    return packed[1];

                case TypedValue.TYPE_REFERENCE:
                    return packed[1] == 0 ? fallback : resources.getDimension(packed[1]);

                default:
                    break;
            }
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        return parseNumber(text, fallback);
    }

    static float parseNumber(String text, float fallback)
    {
        int end = text.length();
        while (end > 0 && !isNumeric(text.charAt(end - 1))) end--;

        if (end == 0) return fallback;

        try
        {
            return Float.parseFloat(text.substring(0, end));
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }
    }

    private static boolean isNumeric(char value)
    {
        return (value >= '0' && value <= '9') || value == '.' || value == '-' || value == '+';
    }

    static int color(Resources resources, AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null)
        {
            if (packed[0] >= TypedValue.TYPE_FIRST_COLOR_INT && packed[0] <= TypedValue.TYPE_LAST_COLOR_INT) return packed[1];

            if (packed[0] == TypedValue.TYPE_REFERENCE && packed[1] != 0)
            {
                try
                {
                    return resources.getColor(packed[1]);
                }
                catch (RuntimeException failure)
                {
                    return fallback;
                }
            }
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null || !text.startsWith("#")) return fallback;

        return parseColor(text, fallback);
    }

    static int parseColor(String text, int fallback)
    {
        String digits = text.substring(1);

        try
        {
            if (digits.length() == 6) return 0xFF000000 | (int) Long.parseLong(digits, 16);
            if (digits.length() == 8) return (int) Long.parseLong(digits, 16);

            if (digits.length() == 3 || digits.length() == 4)
            {
                int value = 0;
                for (int index = 0; index < digits.length(); index++)
                {
                    int nibble = Character.digit(digits.charAt(index), 16);
                    if (nibble < 0) return fallback;

                    value = (value << 8) | (nibble << 4) | nibble;
                }

                return digits.length() == 3 ? 0xFF000000 | value : value;
            }
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }

        return fallback;
    }

    static int integer(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT) return packed[1];

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }
    }

    static boolean flag(AttributeSet attributes, String name, boolean fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] == TypedValue.TYPE_INT_BOOLEAN) return packed[1] != 0;

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        return "true".equals(text);
    }

    static String text(AttributeSet attributes, String name)
    {
        return attributes.getAttributeValue(ANDROID_NAMESPACE, name);
    }

    static int enumeration(AttributeSet attributes, String name, String[] labels, int fallback)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT)
        {
            return packed[1];
        }

        String value = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (value == null || labels == null) return fallback;

        for (int index = 0; index < labels.length; index++)
        {
            if (labels[index].equals(value)) return index;
        }

        return fallback;
    }

    static int reference(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] == TypedValue.TYPE_REFERENCE) return packed[1];

        return fallback;
    }
}

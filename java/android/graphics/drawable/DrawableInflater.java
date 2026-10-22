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
        return inflate(resources, parser, attributes, null);
    }

    public static Drawable inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        String name = parser.getName();
        if (name == null) return null;

        Drawable drawable = forTag(name);
        if (drawable == null) drawable = forClass(name, attributes);
        if (drawable == null) return null;

        drawable.inflate(resources, parser, attributes, theme);
        return drawable;
    }

    public static Drawable forTag(String name)
    {
        switch (name)
        {
            case "vector": return new VectorDrawable();
            case "animated-vector": return new AnimatedVectorDrawable();
            case "shape": return new GradientDrawable();
            case "color": return new ColorDrawable(0);
            case "bitmap": return new BitmapDrawable();
            case "nine-patch": return new NinePatchDrawable();
            case "layer-list": return new LayerDrawable();
            case "transition": return new TransitionDrawable();
            case "inset": return new InsetDrawable();
            case "clip": return new ClipDrawable();
            case "scale": return new ScaleDrawable();
            case "rotate": return new RotateDrawable();
            case "level-list": return new LevelListDrawable();
            case "selector": return new StateListDrawable();
            case "animated-selector": return new AnimatedStateListDrawable();
            case "animation-list": return new AnimationDrawable();
            case "animated-image": return new AnimatedImageDrawable();
            case "ripple": return new RippleDrawable();
            case "adaptive-icon": return new AdaptiveIconDrawable();
            default: return null;
        }
    }

    private static Drawable forClass(String name, AttributeSet attributes)
    {
        String className = "drawable".equals(name) && attributes != null ? attributes.getAttributeValue(null, "class") : name;
        if (className == null || className.indexOf('.') < 0) return null;

        try
        {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) loader = DrawableInflater.class.getClassLoader();

            Class<?> type = Class.forName(className, true, loader);
            if (!Drawable.class.isAssignableFrom(type)) return null;

            return (Drawable) type.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException | LinkageError failure)
        {
            return null;
        }
    }

    static Drawable itemDrawable(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        Drawable drawable = null;

        int reference = reference(attributes, "drawable", 0);
        if (reference != 0) drawable = resources.getDrawable(reference);

        int itemDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = inflate(resources, parser, attributes, theme);
            if (child != null) drawable = child;
        }

        return drawable;
    }

    static void skipElement(XmlPullParser parser) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int depth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= depth) return;
        }
    }

    static float fraction(AttributeSet attributes, String name, float fallback)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null)
        {
            if (packed[0] == TypedValue.TYPE_FRACTION) return TypedValue.complexToFraction(packed[1], 1, 1);
            if (packed[0] == TypedValue.TYPE_FLOAT) return Float.intBitsToFloat(packed[1]);
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        float value = parseNumber(text, fallback);
        return text.endsWith("%") || text.endsWith("%p") ? value / 100 : value;
    }

    static boolean isFraction(AttributeSet attributes, String name)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null) return packed[0] == TypedValue.TYPE_FRACTION;

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        return text != null && text.indexOf('%') >= 0;
    }

    static int gravity(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] >= TypedValue.TYPE_FIRST_INT && packed[0] <= TypedValue.TYPE_LAST_INT) return packed[1];

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null) return fallback;

        int result = 0;

        for (String part : text.split("\\|"))
        {
            switch (part.trim())
            {
                case "top": result |= android.view.Gravity.TOP; break;
                case "bottom": result |= android.view.Gravity.BOTTOM; break;
                case "left": result |= android.view.Gravity.LEFT; break;
                case "right": result |= android.view.Gravity.RIGHT; break;
                case "start": result |= android.view.Gravity.START; break;
                case "end": result |= android.view.Gravity.END; break;
                case "center_vertical": result |= android.view.Gravity.CENTER_VERTICAL; break;
                case "center_horizontal": result |= android.view.Gravity.CENTER_HORIZONTAL; break;
                case "center": result |= android.view.Gravity.CENTER; break;
                case "fill_vertical": result |= android.view.Gravity.FILL_VERTICAL; break;
                case "fill_horizontal": result |= android.view.Gravity.FILL_HORIZONTAL; break;
                case "fill": result |= android.view.Gravity.FILL; break;
                case "clip_vertical": result |= android.view.Gravity.CLIP_VERTICAL; break;
                case "clip_horizontal": result |= android.view.Gravity.CLIP_HORIZONTAL; break;
                default: break;
            }
        }

        return result;
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

    static android.content.res.ColorStateList colorStateList(Resources resources, AttributeSet attributes, String name)
    {
        int[] packed = typedValue(attributes, name);

        if (packed != null)
        {
            if (packed[0] >= TypedValue.TYPE_FIRST_COLOR_INT && packed[0] <= TypedValue.TYPE_LAST_COLOR_INT)
            {
                return android.content.res.ColorStateList.valueOf(packed[1]);
            }

            if (packed[0] == TypedValue.TYPE_REFERENCE && packed[1] != 0 && resources != null)
            {
                try
                {
                    return resources.getColorStateList(packed[1]);
                }
                catch (RuntimeException failure)
                {
                    return null;
                }
            }
        }

        String text = attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        if (text == null || !text.startsWith("#")) return null;

        return android.content.res.ColorStateList.valueOf(parseColor(text, 0));
    }

    static android.graphics.BlendMode tintMode(AttributeSet attributes, String name, android.graphics.BlendMode fallback)
    {
        int value = integer(attributes, name, -1);

        if (value < 0)
        {
            String text = text(attributes, name);
            if (text == null) return fallback;

            switch (text)
            {
                case "src_over": value = 3; break;
                case "src_in": value = 5; break;
                case "src_atop": value = 9; break;
                case "multiply": value = 14; break;
                case "screen": value = 15; break;
                case "add": value = 16; break;
                default: return fallback;
            }
        }

        switch (value)
        {
            case 3: return android.graphics.BlendMode.SRC_OVER;
            case 5: return android.graphics.BlendMode.SRC_IN;
            case 9: return android.graphics.BlendMode.SRC_ATOP;
            case 14: return android.graphics.BlendMode.MODULATE;
            case 15: return android.graphics.BlendMode.SCREEN;
            case 16: return android.graphics.BlendMode.PLUS;
            default: return fallback;
        }
    }

    static int reference(AttributeSet attributes, String name, int fallback)
    {
        int[] packed = typedValue(attributes, name);
        if (packed != null && packed[0] == TypedValue.TYPE_REFERENCE) return packed[1];

        return fallback;
    }
}

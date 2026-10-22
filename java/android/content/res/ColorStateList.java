package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.StateSet;
import android.util.TypedValue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ColorStateList implements Parcelable
{
    private static final int DEFAULT_COLOR = 0xFFFF0000;
    private static final int STATE_FOCUSED = 0x0101009c;
    private static final int[][] EMPTY = new int[][] {new int[0]};

    private final int[][] mStateSpecs;
    private final int[] mColors;
    private final int mDefaultColor;
    private final boolean mIsOpaque;

    public ColorStateList(int[][] states, int[] colors)
    {
        mStateSpecs = states;
        mColors = colors;

        int defaultColor = colors.length > 0 ? colors[0] : DEFAULT_COLOR;
        for (int index = 0; index < states.length && index < colors.length; index++)
        {
            if (states[index].length == 0)
            {
                defaultColor = colors[index];
                break;
            }
        }

        mDefaultColor = defaultColor;

        boolean opaque = colors.length > 0;
        for (int color : colors)
        {
            if ((color >>> 24) != 0xFF) opaque = false;
        }

        mIsOpaque = opaque;
    }

    public static ColorStateList valueOf(int color)
    {
        return new ColorStateList(EMPTY, new int[] {color});
    }

    public static ColorStateList createFromXml(Resources resources, XmlPullParser parser) throws XmlPullParserException, IOException
    {
        return createFromXml(resources, parser, null);
    }

    public static ColorStateList createFromXml(Resources resources, XmlPullParser parser, Resources.Theme theme)
        throws XmlPullParserException, IOException
    {
        int event;
        while ((event = parser.next()) != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT)
        {
        }

        if (event != XmlPullParser.START_TAG) throw new XmlPullParserException("No start tag found");

        return createFromXmlInner(resources, parser, theme);
    }

    static ColorStateList createFromXmlInner(Resources resources, XmlPullParser parser, Resources.Theme theme)
        throws XmlPullParserException, IOException
    {
        if (!"selector".equals(parser.getName()))
        {
            throw new XmlPullParserException(parser.getPositionDescription() + ": invalid color state list tag " + parser.getName());
        }

        ArrayList<int[]> states = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG || parser.getDepth() > outerDepth + 1) continue;
            if (!"item".equals(parser.getName())) continue;

            ArrayList<Integer> state = new ArrayList<>();
            int color = DEFAULT_COLOR;
            float alpha = 1.0f;

            for (int index = 0; index < parser.getAttributeCount(); index++)
            {
                String name = parser.getAttributeName(index);
                int[] typed = typedValueOf(parser, index);

                if ("color".equals(name))
                {
                    color = resolveColor(resources, theme, typed, parser.getAttributeValue(index), color);
                    continue;
                }

                if ("alpha".equals(name))
                {
                    alpha = resolveFloat(theme, typed, parser.getAttributeValue(index), alpha);
                    continue;
                }

                int nameResource = parser instanceof XmlResourceParser ? ((XmlResourceParser)parser).getAttributeNameResource(index) : 0;
                if (nameResource == 0 || !isBoolean(typed, parser.getAttributeValue(index))) continue;

                boolean wanted = typed != null ? typed[1] != 0 : "true".equals(parser.getAttributeValue(index));
                state.add(wanted ? nameResource : -nameResource);
            }

            int[] stateSpec = new int[state.size()];
            for (int index = 0; index < stateSpec.length; index++) stateSpec[index] = state.get(index);

            states.add(stateSpec);
            colors.add(modulateAlpha(color, alpha));
        }

        int[][] stateArray = states.toArray(new int[0][]);
        int[] colorArray = new int[colors.size()];
        for (int index = 0; index < colorArray.length; index++) colorArray[index] = colors.get(index);

        return new ColorStateList(stateArray, colorArray);
    }

    private static int[] typedValueOf(XmlPullParser parser, int index)
    {
        if (parser instanceof XmlBlock.Parser) return ((XmlBlock.Parser)parser).getAttributeTypedValue(index);
        return null;
    }

    private static boolean isBoolean(int[] typed, String text)
    {
        if (typed != null) return typed[0] == TypedValue.TYPE_INT_BOOLEAN;
        return "true".equals(text) || "false".equals(text);
    }

    private static boolean isColorType(int type)
    {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    private static int colorOfResource(Resources resources, Resources.Theme theme, int resourceId, int fallback)
    {
        if (resources == null || resourceId == 0) return fallback;

        try
        {
            ColorStateList nested = resources.getColorStateList(resourceId, theme);
            return nested != null ? nested.getDefaultColor() : fallback;
        }
        catch (RuntimeException unresolved)
        {
            return fallback;
        }
    }

    private static int resolveColor(Resources resources, Resources.Theme theme, int[] typed, String text, int fallback)
    {
        if (typed == null)
        {
            if (text == null || !text.startsWith("#")) return fallback;

            try
            {
                return android.graphics.Color.parseColor(text);
            }
            catch (IllegalArgumentException malformed)
            {
                return fallback;
            }
        }

        int type = typed[0];
        int data = typed[1];

        if (isColorType(type)) return data;
        if (type == TypedValue.TYPE_REFERENCE) return colorOfResource(resources, theme, data, fallback);

        if (type == TypedValue.TYPE_ATTRIBUTE && theme != null)
        {
            TypedValue value = new TypedValue();
            if (!theme.resolveAttribute(data, value, true)) return fallback;
            if (isColorType(value.type)) return value.data;

            return colorOfResource(resources, theme, value.resourceId, fallback);
        }

        return fallback;
    }

    private static float resolveFloat(Resources.Theme theme, int[] typed, String text, float fallback)
    {
        if (typed == null)
        {
            try
            {
                return text == null ? fallback : Float.parseFloat(text);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }

        if (typed[0] == TypedValue.TYPE_FLOAT) return Float.intBitsToFloat(typed[1]);

        if (typed[0] == TypedValue.TYPE_ATTRIBUTE && theme != null)
        {
            TypedValue value = new TypedValue();
            if (theme.resolveAttribute(typed[1], value, true) && value.type == TypedValue.TYPE_FLOAT)
            {
                return Float.intBitsToFloat(value.data);
            }
        }

        return fallback;
    }

    private static int modulateAlpha(int color, float alpha)
    {
        if (alpha >= 1.0f) return color;

        int base = color >>> 24;
        int scaled = Math.max(0, Math.min(255, Math.round(base * alpha)));
        return (color & 0x00FFFFFF) | (scaled << 24);
    }

    public int getDefaultColor()
    {
        return mDefaultColor;
    }

    public int getColorForState(int[] stateSet, int defaultColor)
    {
        for (int index = 0; index < mStateSpecs.length && index < mColors.length; index++)
        {
            if (StateSet.stateSetMatches(mStateSpecs[index], stateSet)) return mColors[index];
        }

        return defaultColor;
    }

    public boolean isStateful()
    {
        return mStateSpecs.length >= 1 && mStateSpecs[0].length > 0;
    }

    public boolean hasFocusStateSpecified()
    {
        for (int[] spec : mStateSpecs)
        {
            for (int state : spec)
            {
                if (state == STATE_FOCUSED || state == -STATE_FOCUSED) return true;
            }
        }

        return false;
    }

    public boolean isOpaque()
    {
        return mIsOpaque;
    }

    public ColorStateList withAlpha(int alpha)
    {
        int[] colors = new int[mColors.length];
        for (int index = 0; index < colors.length; index++) colors[index] = (mColors[index] & 0x00FFFFFF) | (alpha << 24);

        return new ColorStateList(mStateSpecs, colors);
    }

    public ColorStateList withLStar(float lStar)
    {
        return this;
    }

    public int getChangingConfigurations()
    {
        return 0;
    }

    public boolean canApplyTheme()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "ColorStateList{mStateSpecs=" + Arrays.deepToString(mStateSpecs) + "mColors=" + Arrays.toString(mColors) +
               "mDefaultColor=" + mDefaultColor + '}';
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mStateSpecs.length);
        for (int[] spec : mStateSpecs) destination.writeIntArray(spec);
        destination.writeIntArray(mColors);
    }

    public static final Parcelable.Creator<ColorStateList> CREATOR = new Parcelable.Creator<ColorStateList>()
    {
        @Override
        public ColorStateList createFromParcel(Parcel source)
        {
            int count = source.readInt();
            int[][] specs = new int[count][];
            for (int index = 0; index < count; index++) specs[index] = source.createIntArray();

            return new ColorStateList(specs, source.createIntArray());
        }

        @Override
        public ColorStateList[] newArray(int size)
        {
            return new ColorStateList[size];
        }
    };
}

package android.graphics.fonts;

import java.util.ArrayList;
import java.util.Objects;

public final class FontVariationAxis
{
    private final String mTag;
    private final int mTagValue;
    private final float mStyleValue;

    public FontVariationAxis(String tagString, float styleValue)
    {
        if (!isValidTag(tagString)) throw new IllegalArgumentException("Illegal tag pattern: " + tagString);

        mTag = tagString;
        mTagValue = makeTag(tagString);
        mStyleValue = styleValue;
    }

    public String getTag()
    {
        return mTag;
    }

    public float getStyleValue()
    {
        return mStyleValue;
    }

    @Override
    public String toString()
    {
        return "'" + mTag + "' " + Float.toString(mStyleValue);
    }

    private static boolean isValidTag(String tagString)
    {
        if (tagString == null || tagString.length() != 4) return false;

        for (int index = 0; index < 4; index++)
        {
            char character = tagString.charAt(index);
            if (character < 0x20 || character > 0x7E) return false;
        }
        return true;
    }

    private static int makeTag(String tagString)
    {
        return (tagString.charAt(0) << 24) | (tagString.charAt(1) << 16) | (tagString.charAt(2) << 8) | tagString.charAt(3);
    }

    public static FontVariationAxis[] fromFontVariationSettings(String settings)
    {
        if (settings == null || settings.isEmpty()) return null;

        ArrayList<FontVariationAxis> axes = new ArrayList<>();
        int length = settings.length();
        int index = 0;

        while (index < length)
        {
            char character = settings.charAt(index);
            if (Character.isWhitespace(character))
            {
                index++;
                continue;
            }

            if (!(character == '\'' || character == '"') || length < index + 6 || settings.charAt(index + 5) != character)
            {
                throw new IllegalArgumentException("Tag should be wrapped with double or single quote: " + settings);
            }

            String tagString = settings.substring(index + 1, index + 5);
            index += 6;

            int end = settings.indexOf(',', index);
            if (end == -1) end = length;

            float value;
            try
            {
                value = Float.parseFloat(settings.substring(index, end).trim());
            }
            catch (NumberFormatException failure)
            {
                throw new IllegalArgumentException("Failed to parse float string: " + failure.getMessage());
            }

            axes.add(new FontVariationAxis(tagString, value));
            index = end + 1;
        }

        return axes.toArray(new FontVariationAxis[0]);
    }

    public static String toFontVariationSettings(FontVariationAxis[] axes)
    {
        if (axes == null) return "";

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < axes.length; index++)
        {
            if (index > 0) builder.append(',');
            builder.append(axes[index]);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof FontVariationAxis)) return false;

        FontVariationAxis axis = (FontVariationAxis)other;
        return axis.mTagValue == mTagValue && Float.compare(axis.mStyleValue, mStyleValue) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTag, mStyleValue);
    }
}

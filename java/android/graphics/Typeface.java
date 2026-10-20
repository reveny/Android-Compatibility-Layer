package android.graphics;

public class Typeface
{
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    private static final int WEIGHT_NORMAL = 400;
    private static final int WEIGHT_BOLD = 700;

    public static final Typeface DEFAULT = create("sans-serif", NORMAL);
    public static final Typeface DEFAULT_BOLD = create("sans-serif", BOLD);
    public static final Typeface SANS_SERIF = create("sans-serif", NORMAL);
    public static final Typeface SERIF = create("serif", NORMAL);
    public static final Typeface MONOSPACE = create("monospace", NORMAL);

    private final String mFamily;
    private final int mStyle;
    private final int mWeight;
    private final boolean mItalic;
    private final int mNativeFont;

    private Typeface(String family, int style, int weight, boolean italic, int nativeFont)
    {
        mFamily = family;
        mStyle = style;
        mWeight = weight;
        mItalic = italic;
        mNativeFont = nativeFont;
    }

    public int getNativeFont()
    {
        return mNativeFont;
    }

    public String getFamily()
    {
        return mFamily;
    }

    public int getStyle()
    {
        return mStyle;
    }

    public boolean isBold()
    {
        return (mStyle & BOLD) != 0;
    }

    public boolean isItalic()
    {
        return mItalic;
    }

    public int getWeight()
    {
        return mWeight;
    }

    public static Typeface defaultFromStyle(int style)
    {
        return create("sans-serif", style);
    }

    public static Typeface create(Typeface family, int style)
    {
        String name = family == null ? "sans-serif" : family.mFamily;
        return create(name, style);
    }

    public static Typeface create(Typeface family, int weight, boolean italic)
    {
        String name = family == null ? "sans-serif" : family.mFamily;
        int style = (weight >= WEIGHT_BOLD ? BOLD : NORMAL) | (italic ? ITALIC : NORMAL);
        return build(name, style, weight, italic);
    }

    public static Typeface create(String familyName, int style)
    {
        int weight = (style & BOLD) != 0 ? WEIGHT_BOLD : WEIGHT_NORMAL;
        boolean italic = (style & ITALIC) != 0;
        return build(familyName, style, weight, italic);
    }

    private static Typeface build(String familyName, int style, int weight, boolean italic)
    {
        String family = familyName == null || familyName.isEmpty() ? "sans-serif" : familyName;
        int font = runtime.TextMeasurers.get().resolveFamily(family, weight, italic);
        return new Typeface(family, style, weight, italic, font);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Typeface)) return false;

        Typeface typeface = (Typeface)other;
        return mStyle == typeface.mStyle && mWeight == typeface.mWeight && mItalic == typeface.mItalic
               && mFamily.equals(typeface.mFamily);
    }

    @Override
    public int hashCode()
    {
        return mFamily.hashCode() * 31 + mStyle * 7 + mWeight + (mItalic ? 1 : 0);
    }

    public static final class CustomFallbackBuilder
    {
        private int mWeight = 400;
        private boolean mItalic;
        private String mSystemFallback = "sans-serif";

        public CustomFallbackBuilder(android.graphics.fonts.FontFamily family)
        {
        }

        public static int getMaxCustomFallbackCount()
        {
            return 64;
        }

        public CustomFallbackBuilder addCustomFallback(android.graphics.fonts.FontFamily family)
        {
            return this;
        }

        public CustomFallbackBuilder setSystemFallback(String familyName)
        {
            if (familyName != null) mSystemFallback = familyName;
            return this;
        }

        public CustomFallbackBuilder setStyle(android.graphics.fonts.FontStyle style)
        {
            if (style == null) return this;

            mWeight = style.getWeight();
            mItalic = style.getSlant() == android.graphics.fonts.FontStyle.FONT_SLANT_ITALIC;

            return this;
        }

        public Typeface build()
        {
            return Typeface.create(mSystemFallback, mItalic ? Typeface.ITALIC : Typeface.NORMAL);
        }
    }

    public static final class Builder
    {
        private String mFamily = "sans-serif";
        private int mWeight = 400;
        private boolean mItalic;

        public Builder(java.io.File path)
        {
        }

        public Builder(android.content.res.AssetManager assets, String path)
        {
        }

        public Builder(android.graphics.fonts.Font font)
        {
        }

        public Builder setWeight(int weight)
        {
            mWeight = weight;
            return this;
        }

        public Builder setItalic(boolean italic)
        {
            mItalic = italic;
            return this;
        }

        public Builder setFallback(String familyName)
        {
            if (familyName != null) mFamily = familyName;
            return this;
        }

        public Builder setTtcIndex(int index)
        {
            return this;
        }

        public Builder setFontVariationSettings(String settings)
        {
            return this;
        }

        public Typeface build()
        {
            return Typeface.create(mFamily, mItalic ? Typeface.ITALIC : Typeface.NORMAL);
        }
    }

    public static native int nativeResolveFamily(String familyName, int weight, boolean italic);

    public static native int nativeWeight(int font);

    public static native boolean nativeIsItalic(int font);
}

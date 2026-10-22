package android.graphics.fonts;

public final class FontStyle
{
    public static final int FONT_WEIGHT_THIN = 100;
    public static final int FONT_WEIGHT_EXTRA_LIGHT = 200;
    public static final int FONT_WEIGHT_LIGHT = 300;
    public static final int FONT_WEIGHT_NORMAL = 400;
    public static final int FONT_WEIGHT_MEDIUM = 500;
    public static final int FONT_WEIGHT_SEMI_BOLD = 600;
    public static final int FONT_WEIGHT_BOLD = 700;
    public static final int FONT_WEIGHT_EXTRA_BOLD = 800;
    public static final int FONT_WEIGHT_BLACK = 900;
    public static final int FONT_WEIGHT_MIN = 1;
    public static final int FONT_WEIGHT_MAX = 1000;
    public static final int FONT_WEIGHT_UNSPECIFIED = -1;

    public static final int FONT_SLANT_UPRIGHT = 0;
    public static final int FONT_SLANT_ITALIC = 1;

    private final int mWeight;
    private final int mSlant;

    public FontStyle()
    {
        this(FONT_WEIGHT_NORMAL, FONT_SLANT_UPRIGHT);
    }

    public FontStyle(int weight, int slant)
    {
        mWeight = weight;
        mSlant = slant;
    }

    public int getWeight()
    {
        return mWeight;
    }

    public int getSlant()
    {
        return mSlant;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof FontStyle)) return false;

        FontStyle that = (FontStyle)other;
        return mWeight == that.mWeight && mSlant == that.mSlant;
    }

    @Override
    public int hashCode()
    {
        return 31 * mWeight + mSlant;
    }

    @Override
    public String toString()
    {
        return "FontStyle{weight=" + mWeight + " slant=" + mSlant + "}";
    }
}

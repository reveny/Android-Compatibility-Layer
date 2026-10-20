package android.content.res;

public class ColorStateList
{
    private final int mDefaultColor;

    private ColorStateList(int defaultColor)
    {
        mDefaultColor = defaultColor;
    }

    public static ColorStateList valueOf(int color)
    {
        return new ColorStateList(color);
    }

    public int getDefaultColor()
    {
        return mDefaultColor;
    }

    public int getColorForState(int[] stateSet, int defaultColor)
    {
        return mDefaultColor;
    }

    public boolean isStateful()
    {
        return false;
    }

    public boolean isOpaque()
    {
        return (mDefaultColor >>> 24) == 0xFF;
    }

    public ColorStateList withAlpha(int alpha)
    {
        return new ColorStateList((mDefaultColor & 0x00FFFFFF) | (alpha << 24));
    }

    @Override
    public String toString()
    {
        return "ColorStateList{#" + Integer.toHexString(mDefaultColor) + "}";
    }
}

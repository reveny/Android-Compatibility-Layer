package android.graphics;

public class Color
{
    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int alpha(int color)
    {
        return color >>> 24;
    }

    public static int red(int color)
    {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color)
    {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color)
    {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue)
    {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static int argb(int alpha, int red, int green, int blue)
    {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private final float mRed;
    private final float mGreen;
    private final float mBlue;
    private final float mAlpha;

    public Color()
    {
        mRed = 0.0f;
        mGreen = 0.0f;
        mBlue = 0.0f;
        mAlpha = 1.0f;
    }

    private Color(float red, float green, float blue, float alpha)
    {
        mRed = red;
        mGreen = green;
        mBlue = blue;
        mAlpha = alpha;
    }

    public static Color valueOf(int color)
    {
        return new Color(red(color) / 255.0f, green(color) / 255.0f, blue(color) / 255.0f, alpha(color) / 255.0f);
    }

    public static Color valueOf(float red, float green, float blue)
    {
        return new Color(red, green, blue, 1.0f);
    }

    public static Color valueOf(float red, float green, float blue, float alpha)
    {
        return new Color(red, green, blue, alpha);
    }

    public int toArgb()
    {
        return argb(Math.round(mAlpha * 255.0f), Math.round(mRed * 255.0f), Math.round(mGreen * 255.0f),
                    Math.round(mBlue * 255.0f));
    }

    public float red()
    {
        return mRed;
    }

    public float green()
    {
        return mGreen;
    }

    public float blue()
    {
        return mBlue;
    }

    public float alpha()
    {
        return mAlpha;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Color)) return false;

        return toArgb() == ((Color)other).toArgb();
    }

    @Override
    public int hashCode()
    {
        return toArgb();
    }

    @Override
    public String toString()
    {
        return "Color(" + mRed + ", " + mGreen + ", " + mBlue + ", " + mAlpha + ")";
    }
}

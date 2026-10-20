package android.util;

public final class Size
{
    private final int mWidth;
    private final int mHeight;

    public Size(int width, int height)
    {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public static Size parseSize(String text)
    {
        if (text == null) throw new IllegalArgumentException("text");

        int separator = text.indexOf('*');
        if (separator < 0) separator = text.indexOf('x');
        if (separator < 0) throw new NumberFormatException("Invalid Size: \"" + text + "\"");

        return new Size(Integer.parseInt(text.substring(0, separator)), Integer.parseInt(text.substring(separator + 1)));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Size)) return false;

        Size candidate = (Size)other;
        return candidate.mWidth == mWidth && candidate.mHeight == mHeight;
    }

    @Override
    public int hashCode()
    {
        return mHeight ^ ((mWidth << 16) | (mWidth >>> 16));
    }

    @Override
    public String toString()
    {
        return mWidth + "x" + mHeight;
    }
}

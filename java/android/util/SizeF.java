package android.util;

public final class SizeF implements android.os.Parcelable
{
    public static final android.os.Parcelable.Creator<SizeF> CREATOR = new android.os.Parcelable.Creator<SizeF>()
    {
        @Override
        public SizeF createFromParcel(android.os.Parcel source)
        {
            float width = source.readFloat();
            float height = source.readFloat();
            return new SizeF(width, height);
        }

        @Override
        public SizeF[] newArray(int size)
        {
            return new SizeF[size];
        }
    };

    private final float mWidth;
    private final float mHeight;

    public SizeF(float width, float height)
    {
        mWidth = width;
        mHeight = height;
    }

    public float getWidth()
    {
        return mWidth;
    }

    public float getHeight()
    {
        return mHeight;
    }

    public static SizeF parseSizeF(String text)
    {
        if (text == null) throw new IllegalArgumentException("text");

        int separator = text.indexOf('*');
        if (separator < 0) separator = text.indexOf('x');
        if (separator < 0) throw new NumberFormatException("Invalid SizeF: \"" + text + "\"");

        return new SizeF(Float.parseFloat(text.substring(0, separator)), Float.parseFloat(text.substring(separator + 1)));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof SizeF)) return false;

        SizeF candidate = (SizeF)other;
        return Float.floatToIntBits(candidate.mWidth) == Float.floatToIntBits(mWidth)
                && Float.floatToIntBits(candidate.mHeight) == Float.floatToIntBits(mHeight);
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(mWidth) ^ Float.floatToIntBits(mHeight);
    }

    @Override
    public String toString()
    {
        return mWidth + "x" + mHeight;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel destination, int flags)
    {
        destination.writeFloat(mWidth);
        destination.writeFloat(mHeight);
    }
}

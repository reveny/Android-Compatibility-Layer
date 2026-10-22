package android.hardware.camera2.params;

public final class RggbChannelVector
{
    public static final int COUNT = 4;
    public static final int RED = 0;
    public static final int GREEN_EVEN = 1;
    public static final int GREEN_ODD = 2;
    public static final int BLUE = 3;

    private final float mRed;
    private final float mGreenEven;
    private final float mGreenOdd;
    private final float mBlue;

    public RggbChannelVector(float red, float greenEven, float greenOdd, float blue)
    {
        mRed = checkFinite(red, "red");
        mGreenEven = checkFinite(greenEven, "greenEven");
        mGreenOdd = checkFinite(greenOdd, "greenOdd");
        mBlue = checkFinite(blue, "blue");
    }

    private static float checkFinite(float value, String name)
    {
        if (Float.isNaN(value)) throw new IllegalArgumentException(name + " must not be NaN");
        if (Float.isInfinite(value)) throw new IllegalArgumentException(name + " must not be infinite");
        return value;
    }

    public float getRed()
    {
        return mRed;
    }

    public float getGreenEven()
    {
        return mGreenEven;
    }

    public float getGreenOdd()
    {
        return mGreenOdd;
    }

    public float getBlue()
    {
        return mBlue;
    }

    public float getComponent(int colorChannel)
    {
        if (colorChannel < 0 || colorChannel >= COUNT) throw new IllegalArgumentException("Color channel out of range");

        switch (colorChannel)
        {
            case RED: return mRed;
            case GREEN_EVEN: return mGreenEven;
            case GREEN_ODD: return mGreenOdd;
            default: return mBlue;
        }
    }

    public void copyTo(float[] destination, int offset)
    {
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (destination.length - offset < COUNT) throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");

        destination[offset + RED] = mRed;
        destination[offset + GREEN_EVEN] = mGreenEven;
        destination[offset + GREEN_ODD] = mGreenOdd;
        destination[offset + BLUE] = mBlue;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof RggbChannelVector)) return false;

        RggbChannelVector vector = (RggbChannelVector)other;
        return mRed == vector.mRed && mGreenEven == vector.mGreenEven && mGreenOdd == vector.mGreenOdd && mBlue == vector.mBlue;
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(mRed) ^ Float.floatToIntBits(mGreenEven) ^ Float.floatToIntBits(mGreenOdd)
                ^ Float.floatToIntBits(mBlue);
    }

    @Override
    public String toString()
    {
        return "RggbChannelVector{R:" + mRed + ", G_even:" + mGreenEven + ", G_odd:" + mGreenOdd + ", B:" + mBlue + "}";
    }
}

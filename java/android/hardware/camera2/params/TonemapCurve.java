package android.hardware.camera2.params;

import android.graphics.PointF;

import java.util.Arrays;

public final class TonemapCurve
{
    public static final float LEVEL_BLACK = 0.0f;
    public static final float LEVEL_WHITE = 1.0f;
    public static final int POINT_SIZE = 2;
    public static final int CHANNEL_RED = 0;
    public static final int CHANNEL_GREEN = 1;
    public static final int CHANNEL_BLUE = 2;

    private static final int OFFSET_POINT_IN = 0;
    private static final int OFFSET_POINT_OUT = 1;
    private static final int TONEMAP_MIN_CURVE_POINTS = 2;
    private static final int MIN_CURVE_LENGTH = TONEMAP_MIN_CURVE_POINTS * POINT_SIZE;

    private final float[] mRed;
    private final float[] mGreen;
    private final float[] mBlue;

    public TonemapCurve(float[] red, float[] green, float[] blue)
    {
        if (red == null) throw new NullPointerException("red must not be null");
        if (green == null) throw new NullPointerException("green must not be null");
        if (blue == null) throw new NullPointerException("blue must not be null");

        checkCurve(red, "red");
        checkCurve(green, "green");
        checkCurve(blue, "blue");

        mRed = Arrays.copyOf(red, red.length);
        mGreen = Arrays.copyOf(green, green.length);
        mBlue = Arrays.copyOf(blue, blue.length);
    }

    private static void checkCurve(float[] curve, String name)
    {
        if (curve.length % POINT_SIZE != 0) throw new IllegalArgumentException(name + " array length must be divisible by " + POINT_SIZE);
        if (curve.length < MIN_CURVE_LENGTH)
        {
            throw new IllegalArgumentException(name + " array length must be at least " + MIN_CURVE_LENGTH);
        }
        for (float value : curve)
        {
            if (value < LEVEL_BLACK || value > LEVEL_WHITE) throw new IllegalArgumentException(name + " values must be in [0.0, 1.0]");
        }
    }

    private float[] getCurve(int colorChannel)
    {
        switch (colorChannel)
        {
            case CHANNEL_RED: return mRed;
            case CHANNEL_GREEN: return mGreen;
            case CHANNEL_BLUE: return mBlue;
            default: throw new IllegalArgumentException("colorChannel out of range");
        }
    }

    public int getPointCount(int colorChannel)
    {
        return getCurve(colorChannel).length / POINT_SIZE;
    }

    public PointF getPoint(int colorChannel, int index)
    {
        float[] curve = getCurve(colorChannel);
        if (index < 0 || index >= curve.length / POINT_SIZE) throw new IllegalArgumentException("index out of range");

        return new PointF(curve[index * POINT_SIZE + OFFSET_POINT_IN], curve[index * POINT_SIZE + OFFSET_POINT_OUT]);
    }

    public void copyColorCurve(int colorChannel, float[] destination, int offset)
    {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (destination == null) throw new NullPointerException("destination must not be null");

        float[] curve = getCurve(colorChannel);
        if (destination.length + offset < curve.length) throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        System.arraycopy(curve, 0, destination, offset, curve.length);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TonemapCurve)) return false;

        TonemapCurve curve = (TonemapCurve)other;
        return Arrays.equals(mRed, curve.mRed) && Arrays.equals(mGreen, curve.mGreen) && Arrays.equals(mBlue, curve.mBlue);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * Arrays.hashCode(mRed) + Arrays.hashCode(mGreen)) + Arrays.hashCode(mBlue);
    }

    @Override
    public String toString()
    {
        return "TonemapCurve{R:" + curveToString(mRed) + ", G:" + curveToString(mGreen) + ", B:" + curveToString(mBlue) + "}";
    }

    private static String curveToString(float[] curve)
    {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < curve.length; index += POINT_SIZE)
        {
            if (index > 0) builder.append(", ");
            builder.append("(").append(curve[index]).append(", ").append(curve[index + 1]).append(")");
        }
        return builder.append("]").toString();
    }
}

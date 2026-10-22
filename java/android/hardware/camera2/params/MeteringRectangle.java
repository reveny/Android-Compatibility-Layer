package android.hardware.camera2.params;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Size;

public final class MeteringRectangle
{
    public static final int METERING_WEIGHT_MIN = 0;
    public static final int METERING_WEIGHT_MAX = 1000;
    public static final int METERING_WEIGHT_DONT_CARE = 0;

    private final int mX;
    private final int mY;
    private final int mWidth;
    private final int mHeight;
    private final int mWeight;

    public MeteringRectangle(int x, int y, int width, int height, int meteringWeight)
    {
        mX = checkNonNegative(x, "x");
        mY = checkNonNegative(y, "y");
        mWidth = checkNonNegative(width, "width");
        mHeight = checkNonNegative(height, "height");
        mWeight = checkWeight(meteringWeight);
    }

    public MeteringRectangle(Point xy, Size dimensions, int meteringWeight)
    {
        if (xy == null) throw new NullPointerException("xy must not be null");
        if (dimensions == null) throw new NullPointerException("dimensions must not be null");
        mX = checkNonNegative(xy.x, "x");
        mY = checkNonNegative(xy.y, "y");
        mWidth = checkNonNegative(dimensions.getWidth(), "width");
        mHeight = checkNonNegative(dimensions.getHeight(), "height");
        mWeight = checkWeight(meteringWeight);
    }

    public MeteringRectangle(Rect rect, int meteringWeight)
    {
        if (rect == null) throw new NullPointerException("rect must not be null");
        mX = checkNonNegative(rect.left, "rect.left");
        mY = checkNonNegative(rect.top, "rect.top");
        mWidth = checkNonNegative(rect.width(), "rect.width");
        mHeight = checkNonNegative(rect.height(), "rect.height");
        mWeight = checkWeight(meteringWeight);
    }

    private static int checkNonNegative(int value, String name)
    {
        if (value < 0) throw new IllegalArgumentException(name + " must not be negative");
        return value;
    }

    private static int checkWeight(int weight)
    {
        if (weight < METERING_WEIGHT_MIN || weight > METERING_WEIGHT_MAX)
        {
            throw new IllegalArgumentException("meteringWeight is out of range [" + METERING_WEIGHT_MIN + ", " + METERING_WEIGHT_MAX + "]");
        }
        return weight;
    }

    public int getX()
    {
        return mX;
    }

    public int getY()
    {
        return mY;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getMeteringWeight()
    {
        return mWeight;
    }

    public Point getUpperLeftPoint()
    {
        return new Point(mX, mY);
    }

    public Size getSize()
    {
        return new Size(mWidth, mHeight);
    }

    public Rect getRect()
    {
        return new Rect(mX, mY, mX + mWidth, mY + mHeight);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof MeteringRectangle && equals((MeteringRectangle)other);
    }

    public boolean equals(MeteringRectangle other)
    {
        if (other == null) return false;
        return mX == other.mX && mY == other.mY && mWidth == other.mWidth && mHeight == other.mHeight && mWeight == other.mWeight;
    }

    @Override
    public int hashCode()
    {
        int hash = mX;
        hash = 31 * hash + mY;
        hash = 31 * hash + mWidth;
        hash = 31 * hash + mHeight;
        return 31 * hash + mWeight;
    }

    @Override
    public String toString()
    {
        return String.format("(x:%d, y:%d, w:%d, h:%d, wt:%d)", mX, mY, mWidth, mHeight, mWeight);
    }
}

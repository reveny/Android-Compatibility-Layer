package android.hardware.camera2.params;

import java.util.Arrays;

public final class LensShadingMap
{
    public static final float MINIMUM_GAIN_FACTOR = 1.0f;

    private final int mRows;
    private final int mColumns;
    private final float[] mElements;

    LensShadingMap(float[] elements, int rows, int columns)
    {
        if (rows <= 0) throw new IllegalArgumentException("rows must be positive");
        if (columns <= 0) throw new IllegalArgumentException("columns must be positive");
        if (elements == null) throw new NullPointerException("elements must not be null");
        if (elements.length != rows * columns * RggbChannelVector.COUNT)
        {
            throw new IllegalArgumentException("elements must be " + rows * columns * RggbChannelVector.COUNT + " length");
        }
        mRows = rows;
        mColumns = columns;
        mElements = elements;
    }

    public int getRowCount()
    {
        return mRows;
    }

    public int getColumnCount()
    {
        return mColumns;
    }

    public int getGainFactorCount()
    {
        return mRows * mColumns * RggbChannelVector.COUNT;
    }

    public float getGainFactor(int colorChannel, int column, int row)
    {
        if (colorChannel < 0 || colorChannel >= RggbChannelVector.COUNT) throw new IllegalArgumentException("colorChannel out of range");
        if (column < 0 || column >= mColumns) throw new IllegalArgumentException("column out of range");
        if (row < 0 || row >= mRows) throw new IllegalArgumentException("row out of range");

        return mElements[colorChannel + (row * mColumns + column) * RggbChannelVector.COUNT];
    }

    public RggbChannelVector getGainFactorVector(int column, int row)
    {
        if (column < 0 || column >= mColumns) throw new IllegalArgumentException("column out of range");
        if (row < 0 || row >= mRows) throw new IllegalArgumentException("row out of range");

        int offset = (row * mColumns + column) * RggbChannelVector.COUNT;
        return new RggbChannelVector(mElements[offset + RggbChannelVector.RED], mElements[offset + RggbChannelVector.GREEN_EVEN],
                mElements[offset + RggbChannelVector.GREEN_ODD], mElements[offset + RggbChannelVector.BLUE]);
    }

    public void copyGainFactors(float[] destination, int offset)
    {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (destination.length + offset < getGainFactorCount())
        {
            throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        }
        System.arraycopy(mElements, 0, destination, offset, getGainFactorCount());
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LensShadingMap)) return false;

        LensShadingMap map = (LensShadingMap)other;
        return mRows == map.mRows && mColumns == map.mColumns && Arrays.equals(mElements, map.mElements);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * mRows + mColumns) + Arrays.hashCode(mElements);
    }

    @Override
    public String toString()
    {
        return "LensShadingMap{rows=" + mRows + ", columns=" + mColumns + ", gains=" + Arrays.toString(mElements) + "}";
    }
}

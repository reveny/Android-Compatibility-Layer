package android.hardware.camera2.params;

import java.util.Arrays;

public final class BlackLevelPattern
{
    public static final int COUNT = 4;

    private final int[] mCfaOffsets;

    public BlackLevelPattern(int[] offsets)
    {
        if (offsets == null) throw new NullPointerException("Null offsets array passed to constructor");
        if (offsets.length < COUNT) throw new IllegalArgumentException("Invalid offsets array length");
        mCfaOffsets = Arrays.copyOf(offsets, COUNT);
    }

    public int getOffsetForIndex(int column, int row)
    {
        if (row < 0 || column < 0) throw new IllegalArgumentException("column, row arguments must be positive");
        return mCfaOffsets[((row & 1) << 1) | (column & 1)];
    }

    public void copyTo(int[] destination, int offset)
    {
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (offset < 0) throw new IllegalArgumentException("Null offset passed to copyTo");
        if (destination.length - offset < COUNT) throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        System.arraycopy(mCfaOffsets, 0, destination, offset, COUNT);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof BlackLevelPattern && Arrays.equals(((BlackLevelPattern)other).mCfaOffsets, mCfaOffsets);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(mCfaOffsets);
    }

    @Override
    public String toString()
    {
        return String.format("BlackLevelPattern([%d, %d], [%d, %d])", mCfaOffsets[0], mCfaOffsets[1], mCfaOffsets[2], mCfaOffsets[3]);
    }
}

package android.hardware.camera2.params;

import android.util.Rational;

import java.util.Arrays;

public final class ColorSpaceTransform
{
    private static final int ROWS = 3;
    private static final int COLUMNS = 3;
    private static final int COUNT = ROWS * COLUMNS;
    private static final int RATIONAL_SIZE = 2;
    private static final int OFFSET_NUMERATOR = 0;
    private static final int OFFSET_DENOMINATOR = 1;

    private final int[] mElements;

    public ColorSpaceTransform(Rational[] elements)
    {
        if (elements == null) throw new NullPointerException("elements must not be null");
        if (elements.length != COUNT) throw new IllegalArgumentException("elements must be " + COUNT + " length");

        mElements = new int[COUNT * RATIONAL_SIZE];
        for (int index = 0; index < elements.length; ++index)
        {
            if (elements[index] == null) throw new NullPointerException("element[" + index + "] must not be null");
            mElements[index * RATIONAL_SIZE + OFFSET_NUMERATOR] = elements[index].getNumerator();
            mElements[index * RATIONAL_SIZE + OFFSET_DENOMINATOR] = elements[index].getDenominator();
        }
    }

    public ColorSpaceTransform(int[] elements)
    {
        if (elements == null) throw new NullPointerException("elements must not be null");
        if (elements.length != COUNT * RATIONAL_SIZE)
        {
            throw new IllegalArgumentException("elements must be " + COUNT * RATIONAL_SIZE + " length");
        }
        mElements = Arrays.copyOf(elements, elements.length);
    }

    public Rational getElement(int column, int row)
    {
        if (column < 0 || column >= COLUMNS) throw new IllegalArgumentException("column out of range");
        if (row < 0 || row >= ROWS) throw new IllegalArgumentException("row out of range");

        int index = (row * COLUMNS + column) * RATIONAL_SIZE;
        return new Rational(mElements[index + OFFSET_NUMERATOR], mElements[index + OFFSET_DENOMINATOR]);
    }

    public void copyElements(Rational[] destination, int offset)
    {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (destination.length - offset < COUNT) throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");

        for (int index = 0, element = 0; index < COUNT; ++index, element += RATIONAL_SIZE)
        {
            destination[index + offset] = new Rational(mElements[element + OFFSET_NUMERATOR], mElements[element + OFFSET_DENOMINATOR]);
        }
    }

    public void copyElements(int[] destination, int offset)
    {
        if (offset < 0) throw new IllegalArgumentException("offset must not be negative");
        if (destination == null) throw new NullPointerException("destination must not be null");
        if (destination.length - offset < COUNT * RATIONAL_SIZE)
        {
            throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        }
        System.arraycopy(mElements, 0, destination, offset, COUNT * RATIONAL_SIZE);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ColorSpaceTransform)) return false;

        ColorSpaceTransform transform = (ColorSpaceTransform)other;
        for (int index = 0, element = 0; index < COUNT; ++index, element += RATIONAL_SIZE)
        {
            Rational mine = new Rational(mElements[element + OFFSET_NUMERATOR], mElements[element + OFFSET_DENOMINATOR]);
            Rational theirs = new Rational(transform.mElements[element + OFFSET_NUMERATOR],
                    transform.mElements[element + OFFSET_DENOMINATOR]);
            if (!mine.equals(theirs)) return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(mElements);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("ColorSpace Transform");
        builder.append("(");
        for (int row = 0; row < ROWS; ++row)
        {
            builder.append("[");
            for (int column = 0; column < COLUMNS; ++column)
            {
                int index = (row * COLUMNS + column) * RATIONAL_SIZE;
                builder.append(mElements[index + OFFSET_NUMERATOR]).append("/").append(mElements[index + OFFSET_DENOMINATOR]);
                if (column < COLUMNS - 1) builder.append(", ");
            }
            builder.append("]");
            if (row < ROWS - 1) builder.append(", ");
        }
        builder.append(")");
        return builder.toString();
    }
}

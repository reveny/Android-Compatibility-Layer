package android.util;

import java.util.Arrays;

public class SparseBooleanArray implements Cloneable
{
    private int[] mKeys;
    private boolean[] mValues;
    private int mSize;

    public SparseBooleanArray()
    {
        this(10);
    }

    public SparseBooleanArray(int initialCapacity)
    {
        int capacity = Math.max(0, initialCapacity);
        mKeys = new int[capacity];
        mValues = new boolean[capacity];
    }

    public boolean get(int key)
    {
        return get(key, false);
    }

    public boolean get(int key, boolean valueIfKeyNotFound)
    {
        int index = Arrays.binarySearch(mKeys, 0, mSize, key);
        return index < 0 ? valueIfKeyNotFound : mValues[index];
    }

    public void put(int key, boolean value)
    {
        int index = Arrays.binarySearch(mKeys, 0, mSize, key);

        if (index >= 0)
        {
            mValues[index] = value;
            return;
        }

        insertAt(~index, key, value);
    }

    public void append(int key, boolean value)
    {
        if (mSize != 0 && key <= mKeys[mSize - 1])
        {
            put(key, value);
            return;
        }

        insertAt(mSize, key, value);
    }

    public void delete(int key)
    {
        int index = Arrays.binarySearch(mKeys, 0, mSize, key);
        if (index >= 0) removeAt(index);
    }

    public void removeAt(int index)
    {
        if (index >= mSize) throw new ArrayIndexOutOfBoundsException(index);

        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
        System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
        mSize--;
    }

    public int size()
    {
        return mSize;
    }

    public int keyAt(int index)
    {
        if (index >= mSize) throw new ArrayIndexOutOfBoundsException(index);

        return mKeys[index];
    }

    public boolean valueAt(int index)
    {
        if (index >= mSize) throw new ArrayIndexOutOfBoundsException(index);

        return mValues[index];
    }

    public void setValueAt(int index, boolean value)
    {
        if (index >= mSize) throw new ArrayIndexOutOfBoundsException(index);

        mValues[index] = value;
    }

    public void setKeyAt(int index, int key)
    {
        if (index >= mSize) throw new ArrayIndexOutOfBoundsException(index);

        mKeys[index] = key;
    }

    public int indexOfKey(int key)
    {
        return Arrays.binarySearch(mKeys, 0, mSize, key);
    }

    public int indexOfValue(boolean value)
    {
        for (int index = 0; index < mSize; index++)
        {
            if (mValues[index] == value) return index;
        }

        return -1;
    }

    public void clear()
    {
        mSize = 0;
    }

    @Override
    public SparseBooleanArray clone()
    {
        try
        {
            SparseBooleanArray copy = (SparseBooleanArray)super.clone();
            copy.mKeys = mKeys.clone();
            copy.mValues = mValues.clone();
            return copy;
        }
        catch (CloneNotSupportedException failure)
        {
            throw new AssertionError(failure);
        }
    }

    @Override
    public int hashCode()
    {
        int hash = mSize;

        for (int index = 0; index < mSize; index++) hash = 31 * hash + mKeys[index] | (mValues[index] ? 1 : 0);

        return hash;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SparseBooleanArray)) return false;

        SparseBooleanArray that = (SparseBooleanArray)other;
        if (mSize != that.mSize) return false;

        for (int index = 0; index < mSize; index++)
        {
            if (mKeys[index] != that.mKeys[index] || mValues[index] != that.mValues[index]) return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        if (mSize <= 0) return "{}";

        StringBuilder builder = new StringBuilder(mSize * 28);
        builder.append('{');

        for (int index = 0; index < mSize; index++)
        {
            if (index > 0) builder.append(", ");

            builder.append(mKeys[index]).append('=').append(mValues[index]);
        }

        return builder.append('}').toString();
    }

    private void insertAt(int index, int key, boolean value)
    {
        if (mSize == mKeys.length)
        {
            int capacity = mSize < 4 ? 8 : mSize * 2;
            mKeys = Arrays.copyOf(mKeys, capacity);
            mValues = Arrays.copyOf(mValues, capacity);
        }

        System.arraycopy(mKeys, index, mKeys, index + 1, mSize - index);
        System.arraycopy(mValues, index, mValues, index + 1, mSize - index);
        mKeys[index] = key;
        mValues[index] = value;
        mSize++;
    }
}

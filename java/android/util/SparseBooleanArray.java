package android.util;

import java.util.LinkedHashMap;

public class SparseBooleanArray implements Cloneable
{
    private final LinkedHashMap<Integer, Boolean> mValues = new LinkedHashMap<>();

    public SparseBooleanArray()
    {
    }

    public SparseBooleanArray(int initialCapacity)
    {
    }

    public boolean get(int key)
    {
        return get(key, false);
    }

    public boolean get(int key, boolean valueIfKeyNotFound)
    {
        Boolean value = mValues.get(key);
        return value == null ? valueIfKeyNotFound : value;
    }

    public void put(int key, boolean value)
    {
        mValues.put(key, value);
    }

    public void append(int key, boolean value)
    {
        mValues.put(key, value);
    }

    public void delete(int key)
    {
        mValues.remove(key);
    }

    public void removeAt(int index)
    {
        int key = keyAt(index);
        if (key != Integer.MIN_VALUE) mValues.remove(key);
    }

    public int size()
    {
        return mValues.size();
    }

    public int keyAt(int index)
    {
        int at = 0;
        for (Integer key : mValues.keySet())
        {
            if (at++ == index) return key;
        }

        return Integer.MIN_VALUE;
    }

    public boolean valueAt(int index)
    {
        int at = 0;
        for (Boolean value : mValues.values())
        {
            if (at++ == index) return value;
        }

        return false;
    }

    public int indexOfKey(int key)
    {
        int at = 0;
        for (Integer candidate : mValues.keySet())
        {
            if (candidate == key) return at;
            at++;
        }

        return -1;
    }

    public int indexOfValue(boolean value)
    {
        int at = 0;
        for (Boolean candidate : mValues.values())
        {
            if (candidate == value) return at;
            at++;
        }

        return -1;
    }

    public void clear()
    {
        mValues.clear();
    }

    @Override
    public SparseBooleanArray clone()
    {
        SparseBooleanArray copy = new SparseBooleanArray();
        copy.mValues.putAll(mValues);

        return copy;
    }

    @Override
    public String toString()
    {
        return mValues.toString();
    }
}

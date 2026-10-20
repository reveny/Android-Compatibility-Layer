package android.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class SparseLongArray implements Cloneable
{
    private final LinkedHashMap<Integer, Long> mValues = new LinkedHashMap<>();

    public SparseLongArray()
    {
    }

    public SparseLongArray(int initialCapacity)
    {
    }

    public long get(int key)
    {
        return get(key, 0);
    }

    public long get(int key, long valueIfKeyNotFound)
    {
        Long value = mValues.get(key);
        return value == null ? valueIfKeyNotFound : value;
    }

    public void put(int key, long value)
    {
        mValues.put(key, value);
    }

    public void append(int key, long value)
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

    public long valueAt(int index)
    {
        int at = 0;
        for (Long value : mValues.values())
        {
            if (at++ == index) return value;
        }

        return 0;
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

    public int indexOfValue(long value)
    {
        int at = 0;
        for (Long candidate : mValues.values())
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
    public SparseLongArray clone()
    {
        SparseLongArray copy = new SparseLongArray();
        copy.mValues.putAll(mValues);

        return copy;
    }

    @Override
    public String toString()
    {
        return mValues.toString();
    }
}

package android.util;

import java.util.LinkedHashMap;

public class LongSparseArray<E> implements Cloneable
{
    private final LinkedHashMap<Long, E> mValues = new LinkedHashMap<>();

    public LongSparseArray()
    {
    }

    public LongSparseArray(int initialCapacity)
    {
    }

    public E get(long key)
    {
        return mValues.get(key);
    }

    public E get(long key, E valueIfKeyNotFound)
    {
        E value = mValues.get(key);
        return value == null ? valueIfKeyNotFound : value;
    }

    public void put(long key, E value)
    {
        mValues.put(key, value);
    }

    public void append(long key, E value)
    {
        mValues.put(key, value);
    }

    public void delete(long key)
    {
        mValues.remove(key);
    }

    public void remove(long key)
    {
        mValues.remove(key);
    }

    public void removeAt(int index)
    {
        long key = keyAt(index);
        mValues.remove(key);
    }

    public int size()
    {
        return mValues.size();
    }

    public long keyAt(int index)
    {
        int at = 0;
        for (Long key : mValues.keySet())
        {
            if (at++ == index) return key;
        }

        return 0;
    }

    public E valueAt(int index)
    {
        int at = 0;
        for (E value : mValues.values())
        {
            if (at++ == index) return value;
        }

        return null;
    }

    public void setValueAt(int index, E value)
    {
        long key = keyAt(index);
        mValues.put(key, value);
    }

    public int indexOfKey(long key)
    {
        int at = 0;
        for (Long candidate : mValues.keySet())
        {
            if (candidate == key) return at;
            at++;
        }

        return -1;
    }

    public int indexOfValue(E value)
    {
        int at = 0;
        for (E candidate : mValues.values())
        {
            if (candidate == value) return at;
            at++;
        }

        return -1;
    }

    public boolean containsKey(long key)
    {
        return mValues.containsKey(key);
    }

    public void clear()
    {
        mValues.clear();
    }

    public boolean isEmpty()
    {
        return mValues.isEmpty();
    }

    @Override
    public LongSparseArray<E> clone()
    {
        LongSparseArray<E> copy = new LongSparseArray<>();
        copy.mValues.putAll(mValues);

        return copy;
    }

    @Override
    public String toString()
    {
        return mValues.toString();
    }
}

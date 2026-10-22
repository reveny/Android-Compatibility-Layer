package android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SparseIntArray implements Cloneable
{
    private final LinkedHashMap<Integer, Integer> mEntries = new LinkedHashMap<>();
    private ArrayList<Integer> mSortedKeys;

    public SparseIntArray()
    {
    }

    public SparseIntArray(int initialCapacity)
    {
    }

    public void put(int key, int value)
    {
        mEntries.put(key, value);
        mSortedKeys = null;
    }

    public int get(int key)
    {
        return get(key, 0);
    }

    public int get(int key, int valueIfKeyNotFound)
    {
        Integer value = mEntries.get(key);
        return value == null ? valueIfKeyNotFound : value;
    }

    public void delete(int key)
    {
        mEntries.remove(key);
        mSortedKeys = null;
    }

    public void removeAt(int index)
    {
        Integer key = keyAtOrNull(index);
        if (key != null) delete(key);
    }

    public void clear()
    {
        mEntries.clear();
        mSortedKeys = null;
    }

    public int size()
    {
        return mEntries.size();
    }

    public int keyAt(int index)
    {
        Integer key = keyAtOrNull(index);
        return key == null ? 0 : key;
    }

    public int valueAt(int index)
    {
        Integer key = keyAtOrNull(index);
        return key == null ? 0 : mEntries.get(key);
    }

    public int indexOfKey(int key)
    {
        return sortedKeys().indexOf(key);
    }

    public int indexOfValue(int value)
    {
        ArrayList<Integer> keys = sortedKeys();
        for (int index = 0; index < keys.size(); index++)
        {
            if (mEntries.get(keys.get(index)) == value) return index;
        }
        return -1;
    }

    public void setValueAt(int index, int value)
    {
        Integer key = keyAtOrNull(index);
        if (key != null) mEntries.put(key, value);
    }

    public void append(int key, int value)
    {
        put(key, value);
    }

    @Override
    public SparseIntArray clone()
    {
        SparseIntArray copy = new SparseIntArray();
        copy.mEntries.putAll(mEntries);
        return copy;
    }

    private Integer keyAtOrNull(int index)
    {
        ArrayList<Integer> keys = sortedKeys();
        return index >= 0 && index < keys.size() ? keys.get(index) : null;
    }

    private ArrayList<Integer> sortedKeys()
    {
        if (mSortedKeys == null)
        {
            mSortedKeys = new ArrayList<>(mEntries.keySet());
            java.util.Collections.sort(mSortedKeys);
        }
        return mSortedKeys;
    }

    @Override
    public String toString()
    {
        return "SparseIntArray" + mEntries;
    }
}

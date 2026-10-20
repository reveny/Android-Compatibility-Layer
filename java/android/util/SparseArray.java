package android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SparseArray<E> implements Cloneable
{
    private final LinkedHashMap<Integer, E> mEntries = new LinkedHashMap<>();
    private ArrayList<Integer> mSortedKeys;

    public SparseArray()
    {
    }

    public SparseArray(int initialCapacity)
    {
    }

    public void put(int key, E value)
    {
        mEntries.put(key, value);
        mSortedKeys = null;
    }

    public E get(int key)
    {
        return mEntries.get(key);
    }

    public E get(int key, E valueIfKeyNotFound)
    {
        E value = mEntries.get(key);
        return value == null ? valueIfKeyNotFound : value;
    }

    public void remove(int key)
    {
        delete(key);
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

    public E valueAt(int index)
    {
        Integer key = keyAtOrNull(index);
        return key == null ? null : mEntries.get(key);
    }

    public void setValueAt(int index, E value)
    {
        Integer key = keyAtOrNull(index);
        if (key != null) mEntries.put(key, value);
    }

    public int indexOfKey(int key)
    {
        return sortedKeys().indexOf(key);
    }

    public int indexOfValue(E value)
    {
        ArrayList<Integer> keys = sortedKeys();
        for (int index = 0; index < keys.size(); index++)
        {
            if (mEntries.get(keys.get(index)) == value) return index;
        }
        return -1;
    }

    public void append(int key, E value)
    {
        put(key, value);
    }

    public boolean contains(int key)
    {
        return mEntries.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SparseArray<E> clone()
    {
        SparseArray<E> copy = new SparseArray<>();
        for (Map.Entry<Integer, E> entry : mEntries.entrySet())
        {
            copy.mEntries.put(entry.getKey(), entry.getValue());
        }
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
        return "SparseArray" + mEntries;
    }
}

package android.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ArrayMap<K, V> implements Map<K, V>
{
    private final LinkedHashMap<K, V> mEntries;
    private final ArrayList<K> mOrder = new ArrayList<>();

    public ArrayMap()
    {
        mEntries = new LinkedHashMap<>();
    }

    public ArrayMap(int capacity)
    {
        mEntries = new LinkedHashMap<>(Math.max(1, capacity));
    }

    public ArrayMap(ArrayMap<K, V> source)
    {
        mEntries = new LinkedHashMap<>();
        if (source != null) putAll(source);
    }

    @Override
    public int size()
    {
        return mOrder.size();
    }

    @Override
    public boolean isEmpty()
    {
        return mOrder.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return mEntries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return mEntries.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return mEntries.get(key);
    }

    public V getOrDefault(Object key, V fallback)
    {
        V value = mEntries.get(key);

        return value == null && !mEntries.containsKey(key) ? fallback : value;
    }

    @Override
    public V put(K key, V value)
    {
        if (!mEntries.containsKey(key)) mOrder.add(key);

        return mEntries.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        if (mEntries.containsKey(key)) mOrder.remove(key);

        return mEntries.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> source)
    {
        if (source == null) return;

        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) put(entry.getKey(), entry.getValue());
    }

    @Override
    public void clear()
    {
        mEntries.clear();
        mOrder.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return mEntries.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return mEntries.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return mEntries.entrySet();
    }

    public K keyAt(int index)
    {
        return mOrder.get(index);
    }

    public V valueAt(int index)
    {
        return mEntries.get(mOrder.get(index));
    }

    public V setValueAt(int index, V value)
    {
        return mEntries.put(mOrder.get(index), value);
    }

    public int indexOfKey(Object key)
    {
        return mOrder.indexOf(key);
    }

    public int indexOfValue(Object value)
    {
        for (int index = 0; index < mOrder.size(); index++)
        {
            V candidate = mEntries.get(mOrder.get(index));

            if (candidate == null ? value == null : candidate.equals(value)) return index;
        }

        return -1;
    }

    public V removeAt(int index)
    {
        K key = mOrder.remove(index);

        return mEntries.remove(key);
    }

    public void putAll(ArrayMap<? extends K, ? extends V> array)
    {
        putAll((Map<? extends K, ? extends V>)array);
    }

    public void ensureCapacity(int capacity)
    {
        mOrder.ensureCapacity(capacity);
    }

    public boolean containsAll(Collection<?> keys)
    {
        if (keys == null) return true;

        for (Object key : keys)
        {
            if (!containsKey(key)) return false;
        }

        return true;
    }

    public boolean removeAll(Collection<?> keys)
    {
        if (keys == null) return false;

        boolean changed = false;
        for (Object key : keys)
        {
            if (containsKey(key))
            {
                remove(key);
                changed = true;
            }
        }

        return changed;
    }

    public boolean retainAll(Collection<?> keys)
    {
        boolean changed = false;

        for (int index = mOrder.size() - 1; index >= 0; index--)
        {
            if (keys != null && keys.contains(mOrder.get(index))) continue;

            removeAt(index);
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Map)) return false;

        return mEntries.equals(other);
    }

    @Override
    public int hashCode()
    {
        return mEntries.hashCode();
    }

    @Override
    public String toString()
    {
        return mEntries.toString();
    }
}

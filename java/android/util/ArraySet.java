package android.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ArraySet<E> implements Collection<E>, Set<E>
{
    private final LinkedHashSet<E> mEntries = new LinkedHashSet<>();
    private final ArrayList<E> mOrder = new ArrayList<>();

    public ArraySet()
    {
    }

    public ArraySet(int capacity)
    {
        mOrder.ensureCapacity(Math.max(1, capacity));
    }

    public ArraySet(Collection<? extends E> source)
    {
        if (source != null) addAll(source);
    }

    public ArraySet(ArraySet<E> source)
    {
        if (source != null) addAll((Collection<? extends E>)source);
    }

    public ArraySet(E[] source)
    {
        if (source == null) return;

        for (E value : source) add(value);
    }

    public void ensureCapacity(int capacity)
    {
        mOrder.ensureCapacity(capacity);
    }

    public void addAll(ArraySet<? extends E> source)
    {
        addAll((Collection<? extends E>)source);
    }

    public boolean removeAll(ArraySet<? extends E> source)
    {
        return removeAll((Collection<?>)source);
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
    public boolean contains(Object value)
    {
        return mEntries.contains(value);
    }

    @Override
    public Iterator<E> iterator()
    {
        return new ArrayList<>(mOrder).iterator();
    }

    @Override
    public Object[] toArray()
    {
        return mOrder.toArray();
    }

    @Override
    public <T> T[] toArray(T[] into)
    {
        return mOrder.toArray(into);
    }

    @Override
    public boolean add(E value)
    {
        if (!mEntries.add(value)) return false;

        mOrder.add(value);
        return true;
    }

    @Override
    public boolean remove(Object value)
    {
        if (!mEntries.remove(value)) return false;

        mOrder.remove(value);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> values)
    {
        return values == null || mEntries.containsAll(values);
    }

    @Override
    public boolean addAll(Collection<? extends E> values)
    {
        if (values == null) return false;

        boolean changed = false;
        for (E value : values) changed |= add(value);

        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> values)
    {
        if (values == null) return false;

        boolean changed = false;
        for (Object value : values) changed |= remove(value);

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> values)
    {
        boolean changed = false;

        for (int index = mOrder.size() - 1; index >= 0; index--)
        {
            if (values != null && values.contains(mOrder.get(index))) continue;

            removeAt(index);
            changed = true;
        }

        return changed;
    }

    @Override
    public void clear()
    {
        mEntries.clear();
        mOrder.clear();
    }

    public E valueAt(int index)
    {
        return mOrder.get(index);
    }

    public int indexOf(Object value)
    {
        return mOrder.indexOf(value);
    }

    public E removeAt(int index)
    {
        E value = mOrder.remove(index);
        mEntries.remove(value);

        return value;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Set)) return false;

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

package android.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V>
{
    private final LinkedHashMap<K, V> mMap;

    private int mSize;
    private int mMaxSize;
    private int mPutCount;
    private int mCreateCount;
    private int mEvictionCount;
    private int mHitCount;
    private int mMissCount;

    public LruCache(int maxSize)
    {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize <= 0");

        mMaxSize = maxSize;
        mMap = new LinkedHashMap<K, V>(0, 0.75f, true);
    }

    public void resize(int maxSize)
    {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize <= 0");

        synchronized (this)
        {
            mMaxSize = maxSize;
        }

        trimToSize(maxSize);
    }

    public final V get(K key)
    {
        if (key == null) throw new NullPointerException("key == null");

        V mapValue;
        synchronized (this)
        {
            mapValue = mMap.get(key);
            if (mapValue != null)
            {
                mHitCount++;
                return mapValue;
            }

            mMissCount++;
        }

        V createdValue = create(key);
        if (createdValue == null) return null;

        synchronized (this)
        {
            mCreateCount++;
            mapValue = mMap.put(key, createdValue);

            if (mapValue != null) mMap.put(key, mapValue);
            else mSize += safeSizeOf(key, createdValue);
        }

        if (mapValue != null)
        {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        }

        trimToSize(mMaxSize);
        return createdValue;
    }

    public final V put(K key, V value)
    {
        if (key == null || value == null) throw new NullPointerException("key == null || value == null");

        V previous;
        synchronized (this)
        {
            mPutCount++;
            mSize += safeSizeOf(key, value);
            previous = mMap.put(key, value);
            if (previous != null) mSize -= safeSizeOf(key, previous);
        }

        if (previous != null) entryRemoved(false, key, previous, value);

        trimToSize(mMaxSize);
        return previous;
    }

    public void trimToSize(int maxSize)
    {
        while (true)
        {
            K key;
            V value;

            synchronized (this)
            {
                if (mSize < 0 || (mMap.isEmpty() && mSize != 0))
                {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (mSize <= maxSize || mMap.isEmpty()) break;

                Map.Entry<K, V> toEvict = mMap.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                mMap.remove(key);
                mSize -= safeSizeOf(key, value);
                mEvictionCount++;
            }

            entryRemoved(true, key, value, null);
        }
    }

    public final V remove(K key)
    {
        if (key == null) throw new NullPointerException("key == null");

        V previous;
        synchronized (this)
        {
            previous = mMap.remove(key);
            if (previous != null) mSize -= safeSizeOf(key, previous);
        }

        if (previous != null) entryRemoved(false, key, previous, null);
        return previous;
    }

    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue)
    {
    }

    protected V create(K key)
    {
        return null;
    }

    private int safeSizeOf(K key, V value)
    {
        int result = sizeOf(key, value);
        if (result < 0) throw new IllegalStateException("Negative size: " + key + "=" + value);
        return result;
    }

    protected int sizeOf(K key, V value)
    {
        return 1;
    }

    public final void evictAll()
    {
        trimToSize(-1);
    }

    public final synchronized int size()
    {
        return mSize;
    }

    public final synchronized int maxSize()
    {
        return mMaxSize;
    }

    public final synchronized int hitCount()
    {
        return mHitCount;
    }

    public final synchronized int missCount()
    {
        return mMissCount;
    }

    public final synchronized int createCount()
    {
        return mCreateCount;
    }

    public final synchronized int putCount()
    {
        return mPutCount;
    }

    public final synchronized int evictionCount()
    {
        return mEvictionCount;
    }

    public final synchronized Map<K, V> snapshot()
    {
        return new LinkedHashMap<K, V>(mMap);
    }

    @Override
    public final synchronized String toString()
    {
        int accesses = mHitCount + mMissCount;
        int hitPercent = accesses != 0 ? (100 * mHitCount / accesses) : 0;
        return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]", mMaxSize, mHitCount, mMissCount, hitPercent);
    }
}

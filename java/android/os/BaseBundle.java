package android.os;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseBundle
{
    final Map<String, Object> mMap;

    BaseBundle()
    {
        mMap = new HashMap<>();
    }

    BaseBundle(Map<String, Object> map)
    {
        mMap = map;
    }

    BaseBundle(int capacity)
    {
        mMap = new HashMap<>(Math.max(1, capacity));
    }

    public int size()
    {
        return mMap.size();
    }

    public boolean isEmpty()
    {
        return mMap.isEmpty();
    }

    public void clear()
    {
        mMap.clear();
    }

    public boolean containsKey(String key)
    {
        return mMap.containsKey(key);
    }

    public Object get(String key)
    {
        return mMap.get(key);
    }

    public void remove(String key)
    {
        mMap.remove(key);
    }

    public void putAll(PersistableBundle other)
    {
        if (other != null) mMap.putAll(other.mMap);
    }

    public Set<String> keySet()
    {
        return new HashSet<>(mMap.keySet());
    }

    public void putBoolean(String key, boolean value)
    {
        mMap.put(key, value);
    }

    public void putInt(String key, int value)
    {
        mMap.put(key, value);
    }

    public void putLong(String key, long value)
    {
        mMap.put(key, value);
    }

    public void putDouble(String key, double value)
    {
        mMap.put(key, value);
    }

    public void putString(String key, String value)
    {
        mMap.put(key, value);
    }

    public void putBooleanArray(String key, boolean[] value)
    {
        mMap.put(key, value);
    }

    public void putIntArray(String key, int[] value)
    {
        mMap.put(key, value);
    }

    public void putLongArray(String key, long[] value)
    {
        mMap.put(key, value);
    }

    public void putDoubleArray(String key, double[] value)
    {
        mMap.put(key, value);
    }

    public void putStringArray(String key, String[] value)
    {
        mMap.put(key, value);
    }

    public boolean getBoolean(String key)
    {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Boolean ? (Boolean)value : defaultValue;
    }

    public int getInt(String key)
    {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Integer ? (Integer)value : defaultValue;
    }

    public long getLong(String key)
    {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Long ? (Long)value : defaultValue;
    }

    public double getDouble(String key)
    {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Double ? (Double)value : defaultValue;
    }

    public String getString(String key)
    {
        Object value = mMap.get(key);
        return value instanceof String ? (String)value : null;
    }

    public String getString(String key, String defaultValue)
    {
        String value = getString(key);
        return value == null ? defaultValue : value;
    }

    public boolean[] getBooleanArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof boolean[] ? (boolean[])value : null;
    }

    public int[] getIntArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof int[] ? (int[])value : null;
    }

    public long[] getLongArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof long[] ? (long[])value : null;
    }

    public double[] getDoubleArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof double[] ? (double[])value : null;
    }

    public String[] getStringArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof String[] ? (String[])value : null;
    }
}

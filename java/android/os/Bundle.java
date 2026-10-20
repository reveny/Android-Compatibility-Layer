package android.os;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Bundle implements Parcelable, Cloneable
{
    public static final Bundle EMPTY = new Bundle(true);

    public static final Parcelable.Creator<Bundle> CREATOR = new Parcelable.Creator<Bundle>()
    {
        @Override
        public Bundle createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof Bundle ? (Bundle)value : new Bundle();
        }

        @Override
        public Bundle[] newArray(int size)
        {
            return new Bundle[size];
        }
    };

    private final java.util.Map<String, Object> mMap;
    private ClassLoader mClassLoader;

    public Bundle()
    {
        mMap = new HashMap<>();
    }

    private Bundle(boolean immutable)
    {
        mMap = immutable ? java.util.Collections.<String, Object>unmodifiableMap(new HashMap<String, Object>()) : new HashMap<String, Object>();
    }

    public Bundle(Bundle source)
    {
        mMap = new HashMap<>();
        if (source != null) mMap.putAll(source.mMap);
    }

    public Bundle(int capacity)
    {
        mMap = new HashMap<>(Math.max(1, capacity));
    }

    public Bundle(ClassLoader loader)
    {
        mMap = new HashMap<>();
        mClassLoader = loader;
    }

    public void setClassLoader(ClassLoader loader)
    {
        mClassLoader = loader;
    }

    public ClassLoader getClassLoader()
    {
        return mClassLoader;
    }

    public Set<String> keySet()
    {
        return new HashSet<>(mMap.keySet());
    }

    public boolean isEmpty()
    {
        return mMap.isEmpty();
    }

    public int size()
    {
        return mMap.size();
    }

    public boolean containsKey(String key)
    {
        return mMap.containsKey(key);
    }

    public void remove(String key)
    {
        mMap.remove(key);
    }

    public void clear()
    {
        mMap.clear();
    }

    public void putAll(Bundle other)
    {
        if (other != null) mMap.putAll(other.mMap);
    }

    public Object get(String key)
    {
        return mMap.get(key);
    }

    public void putString(String key, String value)
    {
        mMap.put(key, value);
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

    public void putCharSequence(String key, CharSequence value)
    {
        mMap.put(key, value);
    }

    public CharSequence getCharSequence(String key)
    {
        Object value = mMap.get(key);
        return value instanceof CharSequence ? (CharSequence)value : null;
    }

    public void putInt(String key, int value)
    {
        mMap.put(key, value);
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

    public void putLong(String key, long value)
    {
        mMap.put(key, value);
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

    public void putFloat(String key, float value)
    {
        mMap.put(key, value);
    }

    public float getFloat(String key)
    {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Float ? (Float)value : defaultValue;
    }

    public void putDouble(String key, double value)
    {
        mMap.put(key, value);
    }

    public double getDouble(String key, double defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Double ? (Double)value : defaultValue;
    }

    public double getDouble(String key)
    {
        return getDouble(key, 0.0);
    }

    public void putByte(String key, byte value)
    {
        mMap.put(key, Byte.valueOf(value));
    }

    public byte getByte(String key)
    {
        return getByte(key, (byte)0);
    }

    public byte getByte(String key, byte defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Byte ? ((Byte)value).byteValue() : defaultValue;
    }

    public void putShort(String key, short value)
    {
        mMap.put(key, Short.valueOf(value));
    }

    public short getShort(String key)
    {
        return getShort(key, (short)0);
    }

    public short getShort(String key, short defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Short ? ((Short)value).shortValue() : defaultValue;
    }

    public void putChar(String key, char value)
    {
        mMap.put(key, Character.valueOf(value));
    }

    public char getChar(String key)
    {
        return getChar(key, '\0');
    }

    public char getChar(String key, char defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Character ? ((Character)value).charValue() : defaultValue;
    }

    public void putByteArray(String key, byte[] value)
    {
        mMap.put(key, value);
    }

    public byte[] getByteArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof byte[] ? (byte[])value : null;
    }

    public void putBooleanArray(String key, boolean[] value)
    {
        mMap.put(key, value);
    }

    public boolean[] getBooleanArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof boolean[] ? (boolean[])value : null;
    }

    public void putLongArray(String key, long[] value)
    {
        mMap.put(key, value);
    }

    public long[] getLongArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof long[] ? (long[])value : null;
    }

    public void putFloatArray(String key, float[] value)
    {
        mMap.put(key, value);
    }

    public float[] getFloatArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof float[] ? (float[])value : null;
    }

    public void putDoubleArray(String key, double[] value)
    {
        mMap.put(key, value);
    }

    public double[] getDoubleArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof double[] ? (double[])value : null;
    }

    public void putIntegerArrayList(String key, java.util.ArrayList<Integer> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public java.util.ArrayList<Integer> getIntegerArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof java.util.ArrayList ? (java.util.ArrayList<Integer>)value : null;
    }

    public void putCharSequenceArrayList(String key, java.util.ArrayList<CharSequence> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public java.util.ArrayList<CharSequence> getCharSequenceArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof java.util.ArrayList ? (java.util.ArrayList<CharSequence>)value : null;
    }

    public void putBoolean(String key, boolean value)
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

    public void putBundle(String key, Bundle value)
    {
        mMap.put(key, value);
    }

    public Bundle getBundle(String key)
    {
        Object value = mMap.get(key);
        return value instanceof Bundle ? (Bundle)value : null;
    }

    public void putIntArray(String key, int[] value)
    {
        mMap.put(key, value);
    }

    public int[] getIntArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof int[] ? (int[])value : null;
    }

    public void putStringArray(String key, String[] value)
    {
        mMap.put(key, value);
    }

    public String[] getStringArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof String[] ? (String[])value : null;
    }

    public void putStringArrayList(String key, ArrayList<String> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getStringArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<String>)value : null;
    }

    public void putSerializable(String key, java.io.Serializable value)
    {
        mMap.put(key, value);
    }

    public java.io.Serializable getSerializable(String key)
    {
        Object value = mMap.get(key);
        return value instanceof java.io.Serializable ? (java.io.Serializable)value : null;
    }

    public void putParcelable(String key, Parcelable value)
    {
        mMap.put(key, value);
    }

    public Parcelable getParcelable(String key)
    {
        Object value = mMap.get(key);
        return value instanceof Parcelable ? (Parcelable)value : null;
    }

    public <T> T getParcelable(String key, Class<T> type)
    {
        Object value = mMap.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public <T> ArrayList<T> getParcelableArrayList(String key, Class<T> type)
    {
        return getParcelableArrayList(key);
    }

    public <T extends java.io.Serializable> T getSerializable(String key, Class<T> type)
    {
        Object value = mMap.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void putParcelableArrayList(String key, ArrayList<?> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getParcelableArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<T>)value : null;
    }

    public <T extends Parcelable> void putSparseParcelableArray(String key, android.util.SparseArray<T> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> android.util.SparseArray<T> getSparseParcelableArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof android.util.SparseArray ? (android.util.SparseArray<T>)value : null;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this);
    }

    @Override
    public Object clone()
    {
        return new Bundle(this);
    }

    @Override
    public String toString()
    {
        return "Bundle[" + mMap.keySet() + "]";
    }
}

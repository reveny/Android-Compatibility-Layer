package android.os;

import java.util.ArrayList;
import java.util.HashMap;

public final class Bundle extends BaseBundle implements Cloneable, Parcelable
{
    public static final Bundle EMPTY = new Bundle(true);

    public static final Parcelable.Creator<Bundle> CREATOR = new Parcelable.Creator<Bundle>()
    {
        @Override
        public Bundle createFromParcel(Parcel source)
        {
            Bundle bundle = new Bundle();
            if (source != null) source.readBundleBody(bundle.mMap, null);
            return bundle;
        }

        @Override
        public Bundle[] newArray(int size)
        {
            return new Bundle[size];
        }
    };

    private ClassLoader mClassLoader;

    public Bundle()
    {
        super();
    }

    private Bundle(boolean immutable)
    {
        super(immutable ? java.util.Collections.<String, Object>unmodifiableMap(new HashMap<String, Object>())
                        : new HashMap<String, Object>());
    }

    public Bundle(Bundle source)
    {
        super();
        if (source != null) mMap.putAll(source.mMap);
    }

    public Bundle(PersistableBundle source)
    {
        super();
        if (source != null) mMap.putAll(source.mMap);
    }

    public Bundle(int capacity)
    {
        super(capacity);
    }

    public Bundle(ClassLoader loader)
    {
        super();
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

    public boolean hasFileDescriptors()
    {
        return false;
    }

    public void putAll(Bundle other)
    {
        if (other != null) mMap.putAll(other.mMap);
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

    public CharSequence getCharSequence(String key, CharSequence defaultValue)
    {
        CharSequence value = getCharSequence(key);
        return value == null ? defaultValue : value;
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

    public void putByte(String key, byte value)
    {
        mMap.put(key, Byte.valueOf(value));
    }

    public byte getByte(String key)
    {
        return getByte(key, (byte)0);
    }

    public Byte getByte(String key, byte defaultValue)
    {
        Object value = mMap.get(key);
        return value instanceof Byte ? (Byte)value : Byte.valueOf(defaultValue);
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

    public void putShortArray(String key, short[] value)
    {
        mMap.put(key, value);
    }

    public short[] getShortArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof short[] ? (short[])value : null;
    }

    public void putCharArray(String key, char[] value)
    {
        mMap.put(key, value);
    }

    public char[] getCharArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof char[] ? (char[])value : null;
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

    public void putCharSequenceArray(String key, CharSequence[] value)
    {
        mMap.put(key, value);
    }

    public CharSequence[] getCharSequenceArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof CharSequence[] ? (CharSequence[])value : null;
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Integer> getIntegerArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<Integer>)value : null;
    }

    public void putCharSequenceArrayList(String key, ArrayList<CharSequence> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<CharSequence> getCharSequenceArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<CharSequence>)value : null;
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

    public <T extends java.io.Serializable> T getSerializable(String key, Class<T> type)
    {
        Object value = mMap.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void putBinder(String key, IBinder value)
    {
        mMap.put(key, value);
    }

    public IBinder getBinder(String key)
    {
        Object value = mMap.get(key);
        return value instanceof IBinder ? (IBinder)value : null;
    }

    public void putParcelable(String key, Parcelable value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> T getParcelable(String key)
    {
        Object value = mMap.get(key);
        return value instanceof Parcelable ? (T)value : null;
    }

    public <T> T getParcelable(String key, Class<T> type)
    {
        Object value = mMap.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void putParcelableArray(String key, Parcelable[] value)
    {
        mMap.put(key, value);
    }

    public Parcelable[] getParcelableArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof Parcelable[] ? (Parcelable[])value : null;
    }

    public void putParcelableArrayList(String key, ArrayList<? extends Parcelable> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<T>)value : null;
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getParcelableArrayList(String key, Class<? extends T> type)
    {
        Object value = mMap.get(key);
        return value instanceof ArrayList ? (ArrayList<T>)value : null;
    }

    public void putSparseParcelableArray(String key, android.util.SparseArray<? extends Parcelable> value)
    {
        mMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends Parcelable> android.util.SparseArray<T> getSparseParcelableArray(String key)
    {
        Object value = mMap.get(key);
        return value instanceof android.util.SparseArray ? (android.util.SparseArray<T>)value : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getParcelableArray(String key, Class<T> type)
    {
        Object value = mMap.get(key);
        if (!(value instanceof Object[]) || type == null) return null;

        Object[] source = (Object[])value;
        T[] result = (T[])java.lang.reflect.Array.newInstance(type, source.length);
        for (int index = 0; index < source.length; index++)
        {
            if (source[index] != null && !type.isInstance(source[index])) return null;
            result[index] = (T)source[index];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> android.util.SparseArray<T> getSparseParcelableArray(String key, Class<? extends T> type)
    {
        Object value = mMap.get(key);
        return value instanceof android.util.SparseArray ? (android.util.SparseArray<T>)value : null;
    }

    public void putSize(String key, android.util.Size value)
    {
        mMap.put(key, value);
    }

    public void putSizeF(String key, android.util.SizeF value)
    {
        mMap.put(key, value);
    }

    public android.util.Size getSize(String key)
    {
        Object value = mMap.get(key);
        return value instanceof android.util.Size ? (android.util.Size)value : null;
    }

    public android.util.SizeF getSizeF(String key)
    {
        Object value = mMap.get(key);
        return value instanceof android.util.SizeF ? (android.util.SizeF)value : null;
    }

    public void readFromParcel(Parcel parcel)
    {
        if (parcel == null) return;

        mMap.clear();
        parcel.readBundleBody(mMap, mClassLoader);
    }

    public Bundle deepCopy()
    {
        return new Bundle(this);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBundleBody(this);
    }

    @Override
    public Object clone()
    {
        return new Bundle(this);
    }

    @Override
    public synchronized String toString()
    {
        return "Bundle[" + mMap.keySet() + "]";
    }
}

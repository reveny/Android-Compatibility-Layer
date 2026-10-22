package android.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ContentValues implements android.os.Parcelable
{
    public static final String TAG = "ContentValues";

    public static final android.os.Parcelable.Creator<ContentValues> CREATOR = new android.os.Parcelable.Creator<ContentValues>()
    {
        @Override
        public ContentValues createFromParcel(android.os.Parcel source)
        {
            ContentValues values = new ContentValues();
            int count = source.readInt();

            for (int index = 0; index < count; index++)
            {
                String key = source.readString();
                values.mValues.put(key, source.readValue(null));
            }

            return values;
        }

        @Override
        public ContentValues[] newArray(int size)
        {
            return new ContentValues[size];
        }
    };

    private final Map<String, Object> mValues;

    public ContentValues()
    {
        mValues = new LinkedHashMap<>();
    }

    public ContentValues(int size)
    {
        if (size < 0) throw new IllegalArgumentException("Initial size must be non-negative");

        mValues = new LinkedHashMap<>(size);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel destination, int flags)
    {
        destination.writeInt(mValues.size());

        for (Map.Entry<String, Object> entry : mValues.entrySet())
        {
            destination.writeString(entry.getKey());
            destination.writeValue(entry.getValue());
        }
    }

    public ContentValues(ContentValues from)
    {
        mValues = new LinkedHashMap<>();
        if (from != null) mValues.putAll(from.mValues);
    }

    public void put(String key, String value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Byte value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Short value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Integer value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Long value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Float value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Double value)
    {
        mValues.put(key, value);
    }

    public void put(String key, Boolean value)
    {
        mValues.put(key, value);
    }

    public void put(String key, byte[] value)
    {
        mValues.put(key, value);
    }

    public void putObject(String key, Object value)
    {
        mValues.put(key, value);
    }

    public void putNull(String key)
    {
        mValues.put(key, null);
    }

    public void putAll(ContentValues other)
    {
        if (other != null) mValues.putAll(other.mValues);
    }

    public Object get(String key)
    {
        return mValues.get(key);
    }

    public String getAsString(String key)
    {
        Object value = mValues.get(key);
        return value == null ? null : value.toString();
    }

    public Long getAsLong(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).longValue();
        if (value instanceof String) return Long.valueOf((String)value);

        return null;
    }

    public Integer getAsInteger(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).intValue();
        if (value instanceof String) return Integer.valueOf((String)value);

        return null;
    }

    public Short getAsShort(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).shortValue();

        return null;
    }

    public Byte getAsByte(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).byteValue();

        return null;
    }

    public Float getAsFloat(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).floatValue();

        return null;
    }

    public Double getAsDouble(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Number) return ((Number)value).doubleValue();

        return null;
    }

    public Boolean getAsBoolean(String key)
    {
        Object value = mValues.get(key);
        if (value instanceof Boolean) return (Boolean)value;
        if (value instanceof Number) return ((Number)value).intValue() != 0;
        if (value instanceof String) return Boolean.valueOf((String)value);

        return null;
    }

    public byte[] getAsByteArray(String key)
    {
        Object value = mValues.get(key);
        return value instanceof byte[] ? (byte[])value : null;
    }

    public boolean containsKey(String key)
    {
        return mValues.containsKey(key);
    }

    public void remove(String key)
    {
        mValues.remove(key);
    }

    public void clear()
    {
        mValues.clear();
    }

    public Set<String> keySet()
    {
        return mValues.keySet();
    }

    public Set<Map.Entry<String, Object>> valueSet()
    {
        return mValues.entrySet();
    }

    public int size()
    {
        return mValues.size();
    }

    public boolean isEmpty()
    {
        return mValues.isEmpty();
    }

    @Override
    public String toString()
    {
        return mValues.toString();
    }
}

package android.os;

import java.util.ArrayList;

public final class Parcel
{
    private final ArrayList<Object> mValues = new ArrayList<>();
    private int mPosition;

    private Parcel()
    {
    }

    public static Parcel obtain()
    {
        return new Parcel();
    }

    public void recycle()
    {
        mValues.clear();
        mPosition = 0;
    }

    public int dataPosition()
    {
        return mPosition;
    }

    public void setDataPosition(int position)
    {
        mPosition = position;
    }

    public int dataSize()
    {
        return mValues.size();
    }

    public void writeInt(int value)
    {
        mValues.add(value);
    }

    public int readInt()
    {
        Object value = read();
        return value instanceof Integer ? (Integer)value : 0;
    }

    public void writeLong(long value)
    {
        mValues.add(value);
    }

    public long readLong()
    {
        Object value = read();
        return value instanceof Long ? (Long)value : 0L;
    }

    public void writeFloat(float value)
    {
        mValues.add(value);
    }

    public float readFloat()
    {
        Object value = read();
        return value instanceof Float ? (Float)value : 0f;
    }

    public void writeString(String value)
    {
        mValues.add(value);
    }

    public String readString()
    {
        Object value = read();
        return value instanceof String ? (String)value : null;
    }

    public void writeBundle(Bundle value)
    {
        mValues.add(value);
    }

    public Bundle readBundle()
    {
        Object value = read();
        return value instanceof Bundle ? (Bundle)value : null;
    }

    public void writeParcelable(Parcelable value, int flags)
    {
        mValues.add(value);
    }

    public <T extends Parcelable> T readParcelable(ClassLoader loader)
    {
        Object value = read();
        return value == null ? null : castParcelable(value);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Parcelable> T castParcelable(Object value)
    {
        return (T)value;
    }

    public void writeValue(Object value)
    {
        mValues.add(value);
    }

    public Object readValue(ClassLoader loader)
    {
        return read();
    }

    private Object read()
    {
        if (mPosition < 0 || mPosition >= mValues.size()) return null;
        return mValues.get(mPosition++);
    }
}

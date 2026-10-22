package android.database;

import android.database.sqlite.SQLiteClosable;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class CursorWindow extends SQLiteClosable implements Parcelable
{
    private static final long DEFAULT_WINDOW_BYTES = 2 * 1024 * 1024;

    public static final Parcelable.Creator<CursorWindow> CREATOR = new Parcelable.Creator<CursorWindow>()
    {
        @Override
        public CursorWindow createFromParcel(Parcel source)
        {
            return newFromParcel(source);
        }

        @Override
        public CursorWindow[] newArray(int size)
        {
            return new CursorWindow[size];
        }
    };

    private final String mName;
    private final List<Object[]> mRows = new ArrayList<>();
    private int mColumnCount;
    private int mStartPosition;

    public CursorWindow(String name)
    {
        this(name, DEFAULT_WINDOW_BYTES);
    }

    public CursorWindow(String name, long windowSizeBytes)
    {
        mName = name == null || name.isEmpty() ? "<unnamed>" : name;
    }

    @Deprecated
    public CursorWindow(boolean localWindow)
    {
        this((String)null);
    }

    public static CursorWindow newFromParcel(Parcel parcel)
    {
        Object value = parcel == null ? null : parcel.readValue(null);
        return value instanceof CursorWindow ? (CursorWindow)value : new CursorWindow((String)null);
    }

    public synchronized void clear()
    {
        mRows.clear();
        mStartPosition = 0;
    }

    public int getStartPosition()
    {
        return mStartPosition;
    }

    public void setStartPosition(int position)
    {
        mStartPosition = position;
    }

    public synchronized int getNumRows()
    {
        return mRows.size();
    }

    public synchronized boolean setNumColumns(int columnCount)
    {
        if (!mRows.isEmpty() && columnCount != mColumnCount) return false;

        mColumnCount = columnCount;
        return true;
    }

    public synchronized boolean allocRow()
    {
        mRows.add(new Object[mColumnCount]);
        return true;
    }

    public synchronized void freeLastRow()
    {
        if (!mRows.isEmpty()) mRows.remove(mRows.size() - 1);
    }

    private synchronized Object valueAt(int row, int column)
    {
        int index = row - mStartPosition;
        if (index < 0 || index >= mRows.size() || column < 0 || column >= mColumnCount)
            throw new IllegalStateException("row " + row + " column " + column + " is outside the window " + mName);

        return mRows.get(index)[column];
    }

    private synchronized boolean store(Object value, int row, int column)
    {
        int index = row - mStartPosition;
        if (index < 0 || index >= mRows.size() || column < 0 || column >= mColumnCount) return false;

        mRows.get(index)[column] = value;
        return true;
    }

    public int getType(int row, int column)
    {
        Object value = valueAt(row, column);

        if (value == null) return Cursor.FIELD_TYPE_NULL;
        if (value instanceof byte[]) return Cursor.FIELD_TYPE_BLOB;
        if (value instanceof Double) return Cursor.FIELD_TYPE_FLOAT;
        if (value instanceof Long) return Cursor.FIELD_TYPE_INTEGER;

        return Cursor.FIELD_TYPE_STRING;
    }

    @Deprecated
    public boolean isNull(int row, int column)
    {
        return getType(row, column) == Cursor.FIELD_TYPE_NULL;
    }

    @Deprecated
    public boolean isBlob(int row, int column)
    {
        int type = getType(row, column);
        return type == Cursor.FIELD_TYPE_BLOB || type == Cursor.FIELD_TYPE_NULL;
    }

    @Deprecated
    public boolean isLong(int row, int column)
    {
        return getType(row, column) == Cursor.FIELD_TYPE_INTEGER;
    }

    @Deprecated
    public boolean isFloat(int row, int column)
    {
        return getType(row, column) == Cursor.FIELD_TYPE_FLOAT;
    }

    @Deprecated
    public boolean isString(int row, int column)
    {
        int type = getType(row, column);
        return type == Cursor.FIELD_TYPE_STRING || type == Cursor.FIELD_TYPE_NULL;
    }

    public byte[] getBlob(int row, int column)
    {
        Object value = valueAt(row, column);
        if (value == null || value instanceof byte[]) return (byte[])value;
        if (value instanceof String) return ((String)value).getBytes(java.nio.charset.StandardCharsets.UTF_8);

        throw new SQLiteException("unable to convert " + value.getClass().getSimpleName() + " to a blob");
    }

    public String getString(int row, int column)
    {
        Object value = valueAt(row, column);
        if (value == null || value instanceof String) return (String)value;
        if (value instanceof byte[]) throw new SQLiteException("unable to convert a blob to a string");

        return value.toString();
    }

    public void copyStringToBuffer(int row, int column, CharArrayBuffer buffer)
    {
        if (buffer == null) throw new IllegalArgumentException("the CharArrayBuffer must not be null");
        buffer.copyFrom(getString(row, column));
    }

    public long getLong(int row, int column)
    {
        Object value = valueAt(row, column);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number)value).longValue();
        if (value instanceof String) return parseLong((String)value);

        throw new SQLiteException("unable to convert a blob to a long");
    }

    public double getDouble(int row, int column)
    {
        Object value = valueAt(row, column);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number)value).doubleValue();
        if (value instanceof String) return parseDouble((String)value);

        throw new SQLiteException("unable to convert a blob to a double");
    }

    public short getShort(int row, int column)
    {
        return (short)getLong(row, column);
    }

    public int getInt(int row, int column)
    {
        return (int)getLong(row, column);
    }

    public float getFloat(int row, int column)
    {
        return (float)getDouble(row, column);
    }

    public boolean putBlob(byte[] value, int row, int column)
    {
        return store(value == null ? null : value.clone(), row, column);
    }

    public boolean putString(String value, int row, int column)
    {
        return store(value, row, column);
    }

    public boolean putLong(long value, int row, int column)
    {
        return store(Long.valueOf(value), row, column);
    }

    public boolean putDouble(double value, int row, int column)
    {
        return store(Double.valueOf(value), row, column);
    }

    public boolean putNull(int row, int column)
    {
        return store(null, row, column);
    }

    private static long parseLong(String text)
    {
        try
        {
            return Long.parseLong(text.trim());
        }
        catch (NumberFormatException malformed)
        {
            return (long)parseDouble(text);
        }
    }

    private static double parseDouble(String text)
    {
        try
        {
            return Double.parseDouble(text.trim());
        }
        catch (NumberFormatException malformed)
        {
            return 0.0;
        }
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
    protected void onAllReferencesReleased()
    {
        clear();
    }

    @Override
    public String toString()
    {
        return mName + " {" + getNumRows() + " rows}";
    }
}

package android.database;

import java.util.ArrayList;
import java.util.List;

public class MatrixCursor implements Cursor
{
    private final String[] mColumns;
    private final List<Object[]> mRows = new ArrayList<>();

    private int mPosition = -1;
    private boolean mClosed;

    public MatrixCursor(String[] columnNames)
    {
        mColumns = columnNames == null ? new String[0] : columnNames;
    }

    public MatrixCursor(String[] columnNames, int initialCapacity)
    {
        this(columnNames);
    }

    public void addRow(Object[] values)
    {
        if (values == null) return;

        Object[] row = new Object[mColumns.length];
        System.arraycopy(values, 0, row, 0, Math.min(values.length, mColumns.length));
        mRows.add(row);
    }

    public void addRow(Iterable<?> values)
    {
        if (values == null) return;

        Object[] row = new Object[mColumns.length];
        int index = 0;

        for (Object value : values)
        {
            if (index >= mColumns.length) break;
            row[index++] = value;
        }

        mRows.add(row);
    }

    public RowBuilder newRow()
    {
        Object[] row = new Object[mColumns.length];
        mRows.add(row);

        return new RowBuilder(row);
    }

    public static class RowBuilder
    {
        private final Object[] mRow;
        private int mIndex;

        RowBuilder(Object[] row)
        {
            mRow = row;
        }

        public RowBuilder add(Object value)
        {
            if (mIndex < mRow.length) mRow[mIndex++] = value;
            return this;
        }

        public RowBuilder add(String columnName, Object value)
        {
            return add(value);
        }
    }

    @Override
    public int getCount()
    {
        return mRows.size();
    }

    @Override
    public int getPosition()
    {
        return mPosition;
    }

    @Override
    public boolean move(int offset)
    {
        return moveToPosition(mPosition + offset);
    }

    @Override
    public boolean moveToPosition(int position)
    {
        if (position < -1 || position > mRows.size()) return false;

        mPosition = position;
        return position >= 0 && position < mRows.size();
    }

    @Override
    public boolean moveToFirst()
    {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast()
    {
        return moveToPosition(mRows.size() - 1);
    }

    @Override
    public boolean moveToNext()
    {
        return moveToPosition(mPosition + 1);
    }

    @Override
    public boolean moveToPrevious()
    {
        return moveToPosition(mPosition - 1);
    }

    @Override
    public boolean isFirst()
    {
        return mPosition == 0 && !mRows.isEmpty();
    }

    @Override
    public boolean isLast()
    {
        return !mRows.isEmpty() && mPosition == mRows.size() - 1;
    }

    @Override
    public boolean isBeforeFirst()
    {
        return mPosition < 0;
    }

    @Override
    public boolean isAfterLast()
    {
        return mPosition >= mRows.size();
    }

    @Override
    public int getColumnCount()
    {
        return mColumns.length;
    }

    @Override
    public int getColumnIndex(String columnName)
    {
        for (int index = 0; index < mColumns.length; index++)
        {
            if (mColumns[index].equals(columnName)) return index;
        }

        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName)
    {
        int index = getColumnIndex(columnName);
        if (index < 0) throw new IllegalArgumentException("no column named " + columnName);

        return index;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return mColumns[columnIndex];
    }

    @Override
    public String[] getColumnNames()
    {
        return mColumns.clone();
    }

    @Override
    public String getString(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value == null ? null : value.toString();
    }

    @Override
    public short getShort(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof Number ? ((Number)value).shortValue() : 0;
    }

    @Override
    public int getInt(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof Number ? ((Number)value).intValue() : 0;
    }

    @Override
    public long getLong(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof Number ? ((Number)value).longValue() : 0L;
    }

    @Override
    public float getFloat(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof Number ? ((Number)value).floatValue() : 0f;
    }

    @Override
    public double getDouble(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof Number ? ((Number)value).doubleValue() : 0d;
    }

    @Override
    public byte[] getBlob(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        return value instanceof byte[] ? (byte[])value : null;
    }

    @Override
    public int getType(int columnIndex)
    {
        Object value = valueAt(columnIndex);

        if (value == null) return FIELD_TYPE_NULL;
        if (value instanceof byte[]) return FIELD_TYPE_BLOB;
        if (value instanceof Float || value instanceof Double) return FIELD_TYPE_FLOAT;
        if (value instanceof Number) return FIELD_TYPE_INTEGER;

        return FIELD_TYPE_STRING;
    }

    @Override
    public boolean isNull(int columnIndex)
    {
        return valueAt(columnIndex) == null;
    }

    @Override
    public boolean isClosed()
    {
        return mClosed;
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    private Object valueAt(int columnIndex)
    {
        if (mPosition < 0 || mPosition >= mRows.size()) throw new IllegalStateException("the cursor is not on a row");
        if (columnIndex < 0 || columnIndex >= mColumns.length) throw new IllegalArgumentException("no column " + columnIndex);

        return mRows.get(mPosition)[columnIndex];
    }
}

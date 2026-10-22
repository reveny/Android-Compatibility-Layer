package android.database;

import java.util.ArrayList;
import java.util.List;

public class MatrixCursor extends AbstractCursor
{
    private final String[] mColumns;
    private final List<Object[]> mRows = new ArrayList<>();

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
        if (values.length != mColumns.length)
            throw new IllegalArgumentException("columnNames.length = " + mColumns.length + ", columnValues.length = " + values.length);

        mRows.add(values.clone());
    }

    public void addRow(Iterable<?> values)
    {
        if (values == null) return;

        Object[] row = new Object[mColumns.length];
        int index = 0;

        for (Object value : values)
        {
            if (index >= mColumns.length) throw new IllegalArgumentException("columnValues.size() > columnNames.length");
            row[index++] = value;
        }

        if (index != mColumns.length) throw new IllegalArgumentException("columnValues.size() < columnNames.length");

        mRows.add(row);
    }

    public RowBuilder newRow()
    {
        Object[] row = new Object[mColumns.length];
        mRows.add(row);

        return new RowBuilder(row, mColumns);
    }

    public class RowBuilder
    {
        private final Object[] mRow;
        private final String[] mNames;
        private int mIndex;

        RowBuilder(Object[] row, String[] names)
        {
            mRow = row;
            mNames = names;
        }

        public RowBuilder add(Object value)
        {
            if (mIndex >= mRow.length) throw new CursorIndexOutOfBoundsException("No more columns left.");

            mRow[mIndex++] = value;
            return this;
        }

        public RowBuilder add(String columnName, Object value)
        {
            for (int index = 0; index < mNames.length; index++)
            {
                if (mNames[index].equals(columnName)) mRow[index] = value;
            }

            return this;
        }
    }

    @Override
    public int getCount()
    {
        return mRows.size();
    }

    @Override
    public String[] getColumnNames()
    {
        return mColumns;
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
        if (value == null) return 0;
        if (value instanceof Number) return ((Number)value).shortValue();

        return Short.parseShort(value.toString());
    }

    @Override
    public int getInt(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number)value).intValue();

        return Integer.parseInt(value.toString());
    }

    @Override
    public long getLong(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number)value).longValue();

        return Long.parseLong(value.toString());
    }

    @Override
    public float getFloat(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        if (value == null) return 0f;
        if (value instanceof Number) return ((Number)value).floatValue();

        return Float.parseFloat(value.toString());
    }

    @Override
    public double getDouble(int columnIndex)
    {
        Object value = valueAt(columnIndex);
        if (value == null) return 0d;
        if (value instanceof Number) return ((Number)value).doubleValue();

        return Double.parseDouble(value.toString());
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

    private Object valueAt(int columnIndex)
    {
        if (columnIndex < 0 || columnIndex >= mColumns.length)
            throw new CursorIndexOutOfBoundsException("Requested column: " + columnIndex + ", # of columns: " + mColumns.length);
        if (mPos < 0) throw new CursorIndexOutOfBoundsException("Before first row.");
        if (mPos >= mRows.size()) throw new CursorIndexOutOfBoundsException("After last row.");

        return mRows.get(mPos)[columnIndex];
    }
}

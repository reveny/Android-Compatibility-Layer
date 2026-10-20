package android.database;

public class CursorWrapper implements Cursor
{
    protected final Cursor mCursor;

    public CursorWrapper(Cursor cursor)
    {
        mCursor = cursor;
    }

    public Cursor getWrappedCursor()
    {
        return mCursor;
    }

    @Override
    public int getCount()
    {
        return mCursor.getCount();
    }

    @Override
    public int getPosition()
    {
        return mCursor.getPosition();
    }

    @Override
    public boolean move(int offset)
    {
        return mCursor.move(offset);
    }

    @Override
    public boolean moveToPosition(int position)
    {
        return mCursor.moveToPosition(position);
    }

    @Override
    public boolean moveToFirst()
    {
        return mCursor.moveToFirst();
    }

    @Override
    public boolean moveToLast()
    {
        return mCursor.moveToLast();
    }

    @Override
    public boolean moveToNext()
    {
        return mCursor.moveToNext();
    }

    @Override
    public boolean moveToPrevious()
    {
        return mCursor.moveToPrevious();
    }

    @Override
    public boolean isFirst()
    {
        return mCursor.isFirst();
    }

    @Override
    public boolean isLast()
    {
        return mCursor.isLast();
    }

    @Override
    public boolean isBeforeFirst()
    {
        return mCursor.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast()
    {
        return mCursor.isAfterLast();
    }

    @Override
    public int getColumnIndex(String columnName)
    {
        return mCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName)
    {
        return mCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return mCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames()
    {
        return mCursor.getColumnNames();
    }

    @Override
    public int getColumnCount()
    {
        return mCursor.getColumnCount();
    }

    @Override
    public byte[] getBlob(int columnIndex)
    {
        return mCursor.getBlob(columnIndex);
    }

    @Override
    public String getString(int columnIndex)
    {
        return mCursor.getString(columnIndex);
    }

    @Override
    public short getShort(int columnIndex)
    {
        return mCursor.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex)
    {
        return mCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex)
    {
        return mCursor.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex)
    {
        return mCursor.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex)
    {
        return mCursor.getDouble(columnIndex);
    }

    @Override
    public int getType(int columnIndex)
    {
        return mCursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex)
    {
        return mCursor.isNull(columnIndex);
    }

    @Override
    public void close()
    {
        mCursor.close();
    }

    @Override
    public boolean isClosed()
    {
        return mCursor.isClosed();
    }

}

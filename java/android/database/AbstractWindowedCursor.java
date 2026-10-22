package android.database;

public abstract class AbstractWindowedCursor extends AbstractCursor
{
    protected CursorWindow mWindow;

    public AbstractWindowedCursor()
    {
    }

    @Override
    public byte[] getBlob(int columnIndex)
    {
        checkPosition();
        return mWindow.getBlob(mPos, columnIndex);
    }

    @Override
    public String getString(int columnIndex)
    {
        checkPosition();
        return mWindow.getString(mPos, columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer)
    {
        checkPosition();
        mWindow.copyStringToBuffer(mPos, columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex)
    {
        checkPosition();
        return mWindow.getShort(mPos, columnIndex);
    }

    @Override
    public int getInt(int columnIndex)
    {
        checkPosition();
        return mWindow.getInt(mPos, columnIndex);
    }

    @Override
    public long getLong(int columnIndex)
    {
        checkPosition();
        return mWindow.getLong(mPos, columnIndex);
    }

    @Override
    public float getFloat(int columnIndex)
    {
        checkPosition();
        return mWindow.getFloat(mPos, columnIndex);
    }

    @Override
    public double getDouble(int columnIndex)
    {
        checkPosition();
        return mWindow.getDouble(mPos, columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex)
    {
        checkPosition();
        return mWindow.getType(mPos, columnIndex) == FIELD_TYPE_NULL;
    }

    @Deprecated
    public boolean isBlob(int columnIndex)
    {
        return getType(columnIndex) == FIELD_TYPE_BLOB;
    }

    @Deprecated
    public boolean isString(int columnIndex)
    {
        return getType(columnIndex) == FIELD_TYPE_STRING;
    }

    @Deprecated
    public boolean isLong(int columnIndex)
    {
        return getType(columnIndex) == FIELD_TYPE_INTEGER;
    }

    @Deprecated
    public boolean isFloat(int columnIndex)
    {
        return getType(columnIndex) == FIELD_TYPE_FLOAT;
    }

    @Override
    public int getType(int columnIndex)
    {
        checkPosition();
        return mWindow.getType(mPos, columnIndex);
    }

    @Override
    protected void checkPosition()
    {
        super.checkPosition();
        if (mWindow == null) throw new StaleDataException("Attempting to access a closed CursorWindow.");
    }

    @Override
    public CursorWindow getWindow()
    {
        return mWindow;
    }

    public void setWindow(CursorWindow window)
    {
        if (window == mWindow) return;

        closeWindow();
        mWindow = window;
    }

    public boolean hasWindow()
    {
        return mWindow != null;
    }

    protected void closeWindow()
    {
        if (mWindow == null) return;

        mWindow.close();
        mWindow = null;
    }

    @Override
    public void close()
    {
        super.close();
        closeWindow();
    }
}

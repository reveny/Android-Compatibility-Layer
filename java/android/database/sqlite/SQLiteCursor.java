package android.database.sqlite;

import android.database.AbstractWindowedCursor;
import android.database.CursorWindow;

import java.util.HashMap;
import java.util.Map;

public class SQLiteCursor extends AbstractWindowedCursor
{
    private static final int NO_COUNT = -1;

    private final String mEditTable;
    private final String[] mColumns;
    private final SQLiteQuery mQuery;
    private final SQLiteCursorDriver mDriver;
    private int mCount = NO_COUNT;
    private Map<String, Integer> mColumnNameMap;

    @Deprecated
    public SQLiteCursor(SQLiteDatabase database, SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
    {
        this(driver, editTable, query);
    }

    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
    {
        if (query == null) throw new IllegalArgumentException("query object cannot be null");

        mDriver = driver;
        mEditTable = editTable;
        mQuery = query;
        mColumns = query.getColumnNames();
    }

    public SQLiteDatabase getDatabase()
    {
        return mQuery.getDatabase();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition)
    {
        if (mWindow == null || newPosition < mWindow.getStartPosition()
            || newPosition >= mWindow.getStartPosition() + mWindow.getNumRows())
        {
            fillWindow();
        }

        return true;
    }

    @Override
    public int getCount()
    {
        if (mCount == NO_COUNT) fillWindow();
        return mCount;
    }

    private void fillWindow()
    {
        if (mWindow == null) mWindow = new CursorWindow(mEditTable == null ? mQuery.toString() : mEditTable);
        mCount = mQuery.fillWindow(mWindow);
    }

    @Override
    public int getColumnIndex(String columnName)
    {
        if (mColumnNameMap == null)
        {
            Map<String, Integer> map = new HashMap<>();
            for (int index = 0; index < mColumns.length; index++) map.put(mColumns[index], index);
            mColumnNameMap = map;
        }

        if (columnName == null) return -1;

        final int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) columnName = columnName.substring(periodIndex + 1);

        Integer index = mColumnNameMap.get(columnName);
        if (index != null) return index;

        return super.getColumnIndex(columnName);
    }

    @Override
    public String[] getColumnNames()
    {
        return mColumns;
    }

    @Override
    public void deactivate()
    {
        super.deactivate();
        mDriver.cursorDeactivated();
    }

    @Override
    public void close()
    {
        super.close();

        synchronized (this)
        {
            mQuery.close();
            mDriver.cursorClosed();
        }
    }

    @Deprecated
    @Override
    public boolean requery()
    {
        if (isClosed()) return false;

        synchronized (this)
        {
            if (!mQuery.getDatabase().isOpen()) return false;

            if (mWindow != null) mWindow.clear();
            mPos = -1;
            mCount = NO_COUNT;

            mDriver.cursorRequeried(this);
        }

        try
        {
            return super.requery();
        }
        catch (IllegalStateException failure)
        {
            return false;
        }
    }

    @Override
    public void setWindow(CursorWindow window)
    {
        super.setWindow(window);
        mCount = NO_COUNT;
    }

    public void setSelectionArguments(String[] selectionArgs)
    {
        mDriver.setBindArguments(selectionArgs);
    }

    public void setFillWindowForwardOnly(boolean fillWindowForwardOnly)
    {
    }
}

package android.database.sqlite;

import android.database.Cursor;
import android.os.CancellationSignal;

final class SQLiteDirectCursorDriver implements SQLiteCursorDriver
{
    private final SQLiteDatabase mDatabase;
    private final String mEditTable;
    private final String mSql;
    private final CancellationSignal mCancellationSignal;
    private SQLiteQuery mQuery;

    SQLiteDirectCursorDriver(SQLiteDatabase database, String sql, String editTable, CancellationSignal cancellationSignal)
    {
        mDatabase = database;
        mEditTable = editTable;
        mSql = sql;
        mCancellationSignal = cancellationSignal;
    }

    @Override
    public Cursor query(SQLiteDatabase.CursorFactory factory, String[] selectionArgs)
    {
        SQLiteQuery query = mDatabase.compileQuery(mSql, mCancellationSignal);

        try
        {
            query.bindAllArgsAsStrings(selectionArgs);

            Cursor cursor = factory == null ? new SQLiteCursor(this, mEditTable, query) : factory.newCursor(mDatabase, this, mEditTable, query);
            mQuery = query;

            return cursor;
        }
        catch (RuntimeException failure)
        {
            query.close();
            throw failure;
        }
    }

    @Override
    public void cursorClosed()
    {
    }

    @Override
    public void setBindArguments(String[] bindArgs)
    {
        if (mQuery == null) return;

        mQuery.clearBindings();
        mQuery.bindAllArgsAsStrings(bindArgs);
    }

    @Override
    public void cursorDeactivated()
    {
    }

    @Override
    public void cursorRequeried(Cursor cursor)
    {
    }

    @Override
    public String toString()
    {
        return "SQLiteDirectCursorDriver: " + mSql;
    }
}

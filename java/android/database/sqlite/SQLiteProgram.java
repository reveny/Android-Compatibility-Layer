package android.database.sqlite;

import runtime.Sqlite;

public abstract class SQLiteProgram extends SQLiteClosable
{
    final SQLiteDatabase mDatabase;
    long mStatement;

    SQLiteProgram(SQLiteDatabase database, long statement)
    {
        mDatabase = database;
        mStatement = statement;
    }

    public final void bindNull(int index)
    {
        Sqlite.nativeBindNull(mStatement, index);
    }

    public final void bindLong(int index, long value)
    {
        Sqlite.nativeBindLong(mStatement, index, value);
    }

    public final void bindDouble(int index, double value)
    {
        Sqlite.nativeBindDouble(mStatement, index, value);
    }

    public final void bindString(int index, String value)
    {
        if (value == null) bindNull(index);
        else Sqlite.nativeBindString(mStatement, index, value);
    }

    public final void bindBlob(int index, byte[] value)
    {
        if (value == null) bindNull(index);
        else Sqlite.nativeBindBlob(mStatement, index, value);
    }

    public final void bindAllArgsAsStrings(String[] arguments)
    {
        if (arguments == null) return;

        for (int index = 0; index < arguments.length; index++) bindString(index + 1, arguments[index]);
    }

    public final void clearBindings()
    {
        Sqlite.nativeReset(mStatement);
    }

    final void bindAll(Object[] arguments)
    {
        if (arguments == null) return;

        for (int index = 0; index < arguments.length; index++)
        {
            Object value = arguments[index];
            int at = index + 1;

            if (value == null) bindNull(at);
            else if (value instanceof byte[]) bindBlob(at, (byte[])value);
            else if (value instanceof Float || value instanceof Double) bindDouble(at, ((Number)value).doubleValue());
            else if (value instanceof Boolean) bindLong(at, ((Boolean)value) ? 1 : 0);
            else if (value instanceof Number) bindLong(at, ((Number)value).longValue());
            else bindString(at, value.toString());
        }
    }

    @Override
    protected void onAllReferencesReleased()
    {
        if (mStatement == 0) return;

        Sqlite.nativeFinalize(mStatement);
        mStatement = 0;
    }
}

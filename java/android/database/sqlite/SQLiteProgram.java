package android.database.sqlite;

import java.util.Arrays;

import runtime.Sqlite;

public abstract class SQLiteProgram extends SQLiteClosable
{
    private static final Object UNBOUND = new Object();

    final SQLiteDatabase mDatabase;
    long mStatement;
    private Object[] mBindArgs;

    SQLiteProgram(SQLiteDatabase database, long statement)
    {
        mDatabase = database;
        mStatement = statement;

        int parameters = Sqlite.nativeParameterCount(statement);
        mBindArgs = new Object[Math.max(parameters, 0)];
        Arrays.fill(mBindArgs, UNBOUND);
    }

    public void bindNull(int index)
    {
        record(index, null);
        Sqlite.nativeBindNull(mStatement, index);
    }

    public void bindLong(int index, long value)
    {
        record(index, Long.valueOf(value));
        Sqlite.nativeBindLong(mStatement, index, value);
    }

    public void bindDouble(int index, double value)
    {
        record(index, Double.valueOf(value));
        Sqlite.nativeBindDouble(mStatement, index, value);
    }

    public void bindString(int index, String value)
    {
        if (value == null)
        {
            bindNull(index);
            return;
        }

        record(index, value);
        Sqlite.nativeBindString(mStatement, index, value);
    }

    public void bindBlob(int index, byte[] value)
    {
        if (value == null)
        {
            bindNull(index);
            return;
        }

        record(index, value);
        Sqlite.nativeBindBlob(mStatement, index, value);
    }

    public void bindAllArgsAsStrings(String[] arguments)
    {
        if (arguments == null) return;

        for (int index = arguments.length; index != 0; index--) bindString(index, arguments[index - 1]);
    }

    public void clearBindings()
    {
        Arrays.fill(mBindArgs, UNBOUND);
        Sqlite.nativeReset(mStatement);
    }

    public final int getUniqueId()
    {
        return -1;
    }

    SQLiteDatabase getDatabase()
    {
        return mDatabase;
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

    final void rebind()
    {
        checkOpen();
        Sqlite.nativeReset(mStatement);

        for (int index = 0; index < mBindArgs.length; index++)
        {
            Object value = mBindArgs[index];
            int at = index + 1;

            if (value == UNBOUND) continue;

            if (value == null) Sqlite.nativeBindNull(mStatement, at);
            else if (value instanceof byte[]) Sqlite.nativeBindBlob(mStatement, at, (byte[])value);
            else if (value instanceof Double) Sqlite.nativeBindDouble(mStatement, at, (Double)value);
            else if (value instanceof Long) Sqlite.nativeBindLong(mStatement, at, (Long)value);
            else Sqlite.nativeBindString(mStatement, at, (String)value);
        }
    }

    final void checkOpen()
    {
        if (mStatement == 0) throw new IllegalStateException("the program has already been closed");
    }

    private void record(int index, Object value)
    {
        checkOpen();

        if (index < 1 || index > mBindArgs.length)
        {
            throw new IllegalArgumentException("Cannot bind argument at index " + index + " because the index is out of range.  "
                + "The statement has " + mBindArgs.length + " parameters.");
        }

        mBindArgs[index - 1] = value;
    }

    @Override
    protected void onAllReferencesReleased()
    {
        if (mStatement == 0) return;

        Sqlite.nativeFinalize(mStatement);
        mStatement = 0;
    }
}

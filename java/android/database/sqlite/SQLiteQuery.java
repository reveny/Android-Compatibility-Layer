package android.database.sqlite;

import android.database.CursorWindow;
import android.os.CancellationSignal;

import runtime.Sqlite;

public final class SQLiteQuery extends SQLiteProgram
{
    private final String mSql;
    private final CancellationSignal mCancellationSignal;

    SQLiteQuery(SQLiteDatabase database, String sql, long statement, CancellationSignal cancellationSignal)
    {
        super(database, statement);
        mSql = sql;
        mCancellationSignal = cancellationSignal;
    }

    String[] getColumnNames()
    {
        checkOpen();

        int columns = Sqlite.nativeColumnCount(mStatement);
        String[] names = new String[columns];

        for (int index = 0; index < columns; index++) names[index] = Sqlite.nativeColumnName(mStatement, index);

        return names;
    }

    int fillWindow(CursorWindow window)
    {
        if (mCancellationSignal != null) mCancellationSignal.throwIfCanceled();

        rebind();

        int columns = Sqlite.nativeColumnCount(mStatement);

        window.acquireReference();
        try
        {
            window.clear();
            window.setStartPosition(0);
            window.setNumColumns(columns);

            int rows = 0;
            for (;;)
            {
                int outcome = Sqlite.nativeStep(mStatement);
                if (outcome == Sqlite.STEP_DONE) break;

                if (outcome == Sqlite.STEP_ERROR)
                {
                    SQLiteException failure = mDatabase.failure(mSql);
                    window.clear();
                    throw failure;
                }

                if (mCancellationSignal != null && (rows & 255) == 0) mCancellationSignal.throwIfCanceled();

                window.allocRow();
                for (int column = 0; column < columns; column++) put(window, rows, column);
                rows++;
            }

            return rows;
        }
        finally
        {
            Sqlite.nativeReset(mStatement);
            window.releaseReference();
        }
    }

    private void put(CursorWindow window, int row, int column)
    {
        switch (Sqlite.nativeColumnType(mStatement, column))
        {
            case Sqlite.TYPE_INTEGER:
                window.putLong(Sqlite.nativeColumnLong(mStatement, column), row, column);
                break;
            case Sqlite.TYPE_FLOAT:
                window.putDouble(Sqlite.nativeColumnDouble(mStatement, column), row, column);
                break;
            case Sqlite.TYPE_BLOB:
                window.putBlob(Sqlite.nativeColumnBlob(mStatement, column), row, column);
                break;
            case Sqlite.TYPE_NULL:
                window.putNull(row, column);
                break;
            default:
                window.putString(Sqlite.nativeColumnString(mStatement, column), row, column);
                break;
        }
    }

    @Override
    public String toString()
    {
        return "SQLiteQuery: " + mSql;
    }
}

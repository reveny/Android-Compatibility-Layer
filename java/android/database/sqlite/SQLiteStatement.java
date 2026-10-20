package android.database.sqlite;

import runtime.Sqlite;

public final class SQLiteStatement extends SQLiteProgram
{
    SQLiteStatement(SQLiteDatabase database, long statement)
    {
        super(database, statement);
    }

    public void execute()
    {
        step();
    }

    public long executeInsert()
    {
        step();
        return Sqlite.nativeLastInsertRowId(mDatabase.handle());
    }

    public int executeUpdateDelete()
    {
        step();
        return Sqlite.nativeChanges(mDatabase.handle());
    }

    public long simpleQueryForLong()
    {
        if (Sqlite.nativeStep(mStatement) != Sqlite.STEP_ROW) throw new SQLiteDoneException("the statement returned no rows");

        long value = Sqlite.nativeColumnLong(mStatement, 0);
        Sqlite.nativeReset(mStatement);

        return value;
    }

    public String simpleQueryForString()
    {
        if (Sqlite.nativeStep(mStatement) != Sqlite.STEP_ROW) throw new SQLiteDoneException("the statement returned no rows");

        String value = Sqlite.nativeColumnString(mStatement, 0);
        Sqlite.nativeReset(mStatement);

        return value;
    }

    private void step()
    {
        int outcome = Sqlite.nativeStep(mStatement);
        if (outcome == Sqlite.STEP_ERROR)
        {
            throw new SQLiteException(Sqlite.nativeErrorMessage(mDatabase.handle()));
        }

        Sqlite.nativeReset(mStatement);
    }
}

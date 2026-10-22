package android.database.sqlite;

public class SQLiteTableLockedException extends SQLiteException
{
    public SQLiteTableLockedException()
    {
    }

    public SQLiteTableLockedException(String message)
    {
        super(message);
    }
}

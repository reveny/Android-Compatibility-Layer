package android.database.sqlite;

public class SQLiteReadOnlyDatabaseException extends SQLiteException
{
    public SQLiteReadOnlyDatabaseException()
    {
    }

    public SQLiteReadOnlyDatabaseException(String message)
    {
        super(message);
    }
}

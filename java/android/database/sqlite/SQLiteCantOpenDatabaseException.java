package android.database.sqlite;

public class SQLiteCantOpenDatabaseException extends SQLiteException
{
    public SQLiteCantOpenDatabaseException()
    {
    }

    public SQLiteCantOpenDatabaseException(String message)
    {
        super(message);
    }
}

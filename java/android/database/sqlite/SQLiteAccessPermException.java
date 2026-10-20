package android.database.sqlite;

public class SQLiteAccessPermException extends SQLiteException
{
    public SQLiteAccessPermException()
    {
    }

    public SQLiteAccessPermException(String message)
    {
        super(message);
    }
}

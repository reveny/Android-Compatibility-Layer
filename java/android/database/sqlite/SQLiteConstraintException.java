package android.database.sqlite;

public class SQLiteConstraintException extends SQLiteException
{
    public SQLiteConstraintException()
    {
    }

    public SQLiteConstraintException(String message)
    {
        super(message);
    }
}

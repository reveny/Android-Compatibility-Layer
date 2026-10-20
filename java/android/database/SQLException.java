package android.database;

public class SQLException extends RuntimeException
{
    public SQLException()
    {
    }

    public SQLException(String message)
    {
        super(message);
    }

    public SQLException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

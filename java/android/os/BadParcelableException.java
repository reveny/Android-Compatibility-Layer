package android.os;

public class BadParcelableException extends android.util.AndroidRuntimeException
{
    public BadParcelableException(String message)
    {
        super(message);
    }

    public BadParcelableException(Exception cause)
    {
        super(cause);
    }

    public BadParcelableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

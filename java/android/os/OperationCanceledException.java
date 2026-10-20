package android.os;

public class OperationCanceledException extends RuntimeException
{
    public OperationCanceledException()
    {
        this("the operation was cancelled");
    }

    public OperationCanceledException(String message)
    {
        super(message);
    }
}

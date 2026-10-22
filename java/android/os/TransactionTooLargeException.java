package android.os;

public class TransactionTooLargeException extends RemoteException
{
    public TransactionTooLargeException()
    {
        super();
    }

    public TransactionTooLargeException(String message)
    {
        super(message);
    }
}

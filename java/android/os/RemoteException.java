package android.os;

public class RemoteException extends android.util.AndroidException
{
    public RemoteException()
    {
        super();
    }

    public RemoteException(String message)
    {
        super(message);
    }

    public RemoteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause);
    }

    public RemoteException(Throwable cause)
    {
        super(cause.getMessage(), cause);
    }

    public RuntimeException rethrowAsRuntimeException()
    {
        throw new RuntimeException(this);
    }

    public RuntimeException rethrowFromSystemServer()
    {
        throw new RuntimeException(this);
    }
}

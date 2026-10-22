package android.security.keystore;

import java.security.ProviderException;

public class StrongBoxUnavailableException extends ProviderException
{
    public StrongBoxUnavailableException()
    {
    }

    public StrongBoxUnavailableException(String message)
    {
        super(message);
    }

    public StrongBoxUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public StrongBoxUnavailableException(Throwable cause)
    {
        super(cause);
    }
}

package android.security.identity;

public class UnknownAuthenticationKeyException extends IdentityCredentialException
{
    public UnknownAuthenticationKeyException(String message)
    {
        super(message);
    }

    public UnknownAuthenticationKeyException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

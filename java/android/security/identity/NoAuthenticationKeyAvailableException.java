package android.security.identity;

public class NoAuthenticationKeyAvailableException extends IdentityCredentialException
{
    public NoAuthenticationKeyAvailableException(String message)
    {
        super(message);
    }

    public NoAuthenticationKeyAvailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

package android.security.identity;

public class AlreadyPersonalizedException extends IdentityCredentialException
{
    public AlreadyPersonalizedException(String message)
    {
        super(message);
    }

    public AlreadyPersonalizedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

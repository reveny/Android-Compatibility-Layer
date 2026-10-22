package android.security.identity;

public class IdentityCredentialException extends Exception
{
    public IdentityCredentialException(String message)
    {
        super(message);
    }

    public IdentityCredentialException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

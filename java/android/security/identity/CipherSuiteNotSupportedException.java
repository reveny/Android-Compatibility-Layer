package android.security.identity;

public class CipherSuiteNotSupportedException extends IdentityCredentialException
{
    public CipherSuiteNotSupportedException(String message)
    {
        super(message);
    }

    public CipherSuiteNotSupportedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

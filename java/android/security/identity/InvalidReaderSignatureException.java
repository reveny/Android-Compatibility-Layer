package android.security.identity;

public class InvalidReaderSignatureException extends IdentityCredentialException
{
    public InvalidReaderSignatureException(String message)
    {
        super(message);
    }

    public InvalidReaderSignatureException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

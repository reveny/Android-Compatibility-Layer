package android.security.identity;

public class MessageDecryptionException extends IdentityCredentialException
{
    public MessageDecryptionException(String message)
    {
        super(message);
    }

    public MessageDecryptionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

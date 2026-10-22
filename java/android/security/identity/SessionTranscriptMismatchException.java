package android.security.identity;

public class SessionTranscriptMismatchException extends IdentityCredentialException
{
    public SessionTranscriptMismatchException(String message)
    {
        super(message);
    }

    public SessionTranscriptMismatchException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

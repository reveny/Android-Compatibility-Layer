package android.credentials;

public class GetCredentialException extends Exception
{
    public static final String TYPE_INTERRUPTED = "android.credentials.GetCredentialException.TYPE_INTERRUPTED";
    public static final String TYPE_NO_CREDENTIAL = "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL";
    public static final String TYPE_UNKNOWN = "android.credentials.GetCredentialException.TYPE_UNKNOWN";
    public static final String TYPE_USER_CANCELED = "android.credentials.GetCredentialException.TYPE_USER_CANCELED";

    private final String mType;

    public GetCredentialException(String type, String message)
    {
        this(type, message, null);
    }

    public GetCredentialException(String type, String message, Throwable cause)
    {
        super(message, cause);
        mType = CredentialChecks.requireType(type);
    }

    public GetCredentialException(String type, Throwable cause)
    {
        this(type, null, cause);
    }

    public GetCredentialException(String type)
    {
        this(type, null, null);
    }

    public String getType()
    {
        return mType;
    }
}

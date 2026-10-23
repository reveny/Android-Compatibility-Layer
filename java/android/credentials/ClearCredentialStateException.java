package android.credentials;

public class ClearCredentialStateException extends Exception
{
    public static final String TYPE_UNKNOWN = "android.credentials.ClearCredentialStateException.TYPE_UNKNOWN";

    private final String mType;

    public ClearCredentialStateException(String type, String message)
    {
        this(type, message, null);
    }

    public ClearCredentialStateException(String type, String message, Throwable cause)
    {
        super(message, cause);
        mType = CredentialChecks.requireType(type);
    }

    public ClearCredentialStateException(String type, Throwable cause)
    {
        this(type, null, cause);
    }

    public ClearCredentialStateException(String type)
    {
        this(type, null, null);
    }

    public String getType()
    {
        return mType;
    }
}

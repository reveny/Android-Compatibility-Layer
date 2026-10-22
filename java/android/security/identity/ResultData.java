package android.security.identity;

import java.util.Collection;

public abstract class ResultData
{
    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_SUCH_ENTRY = 1;
    public static final int STATUS_NOT_REQUESTED = 2;
    public static final int STATUS_NOT_IN_REQUEST_MESSAGE = 3;
    public static final int STATUS_USER_AUTHENTICATION_FAILED = 4;
    public static final int STATUS_READER_AUTHENTICATION_FAILED = 5;
    public static final int STATUS_NO_ACCESS_CONTROL_PROFILES = 6;

    ResultData()
    {
    }

    public abstract byte[] getAuthenticatedData();

    public abstract byte[] getMessageAuthenticationCode();

    public abstract byte[] getStaticAuthenticationData();

    public abstract Collection<String> getNamespaces();

    public abstract Collection<String> getEntryNames(String namespaceName);

    public abstract Collection<String> getRetrievedEntryNames(String namespaceName);

    public abstract int getStatus(String namespaceName, String name);

    public abstract byte[] getEntry(String namespaceName, String name);
}

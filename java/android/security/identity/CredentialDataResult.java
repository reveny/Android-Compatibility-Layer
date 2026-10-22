package android.security.identity;

import java.util.Collection;

public abstract class CredentialDataResult
{
    CredentialDataResult()
    {
    }

    public abstract byte[] getDeviceNameSpaces();

    public abstract byte[] getDeviceMac();

    public byte[] getDeviceSignature()
    {
        throw new UnsupportedOperationException();
    }

    public abstract byte[] getStaticAuthenticationData();

    public abstract Entries getIssuerSignedEntries();

    public abstract Entries getDeviceSignedEntries();

    public interface Entries
    {
        int STATUS_OK = 0;
        int STATUS_NO_SUCH_ENTRY = 1;
        int STATUS_NOT_REQUESTED = 2;
        int STATUS_NOT_IN_REQUEST_MESSAGE = 3;
        int STATUS_USER_AUTHENTICATION_FAILED = 4;
        int STATUS_READER_AUTHENTICATION_FAILED = 5;
        int STATUS_NO_ACCESS_CONTROL_PROFILES = 6;

        Collection<String> getNamespaces();

        Collection<String> getEntryNames(String namespaceName);

        Collection<String> getRetrievedEntryNames(String namespaceName);

        int getStatus(String namespaceName, String name);

        byte[] getEntry(String namespaceName, String name);
    }
}

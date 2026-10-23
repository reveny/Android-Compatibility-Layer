package android.credentials;

public final class PrepareGetCredentialResponse
{
    private final PendingGetCredentialHandle mPendingGetCredentialHandle;

    PrepareGetCredentialResponse(GetCredentialRequest request)
    {
        mPendingGetCredentialHandle = new PendingGetCredentialHandle(request);
    }

    public boolean hasCredentialResults(String credentialType)
    {
        return false;
    }

    public boolean hasAuthenticationResults()
    {
        return false;
    }

    public boolean hasRemoteResults()
    {
        return false;
    }

    public PendingGetCredentialHandle getPendingGetCredentialHandle()
    {
        return mPendingGetCredentialHandle;
    }

    public static final class PendingGetCredentialHandle
    {
        private final GetCredentialRequest mRequest;

        PendingGetCredentialHandle(GetCredentialRequest request)
        {
            mRequest = request;
        }

        GetCredentialRequest getRequest()
        {
            return mRequest;
        }
    }
}

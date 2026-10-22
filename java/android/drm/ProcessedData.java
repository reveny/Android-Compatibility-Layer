package android.drm;

public class ProcessedData
{
    private final byte[] mData;
    private final String mAccountId;
    private final String mSubscriptionId;

    ProcessedData(byte[] data, String accountId, String subscriptionId)
    {
        mData = data;
        mAccountId = accountId == null ? "_NO_USER" : accountId;
        mSubscriptionId = subscriptionId == null ? "" : subscriptionId;
    }

    public String getAccountId()
    {
        return mAccountId;
    }

    public byte[] getData()
    {
        return mData;
    }

    public String getSubscriptionId()
    {
        return mSubscriptionId;
    }
}

package android.drm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DrmRights
{
    private static final String NO_USER = "_NO_USER";

    private byte[] mData;
    private String mMimeType;
    private String mAccountId;
    private String mSubscriptionId;

    public DrmRights(ProcessedData data, String mimeType)
    {
        if (data == null) throw new IllegalArgumentException("data is null");

        mData = data.getData();
        mAccountId = data.getAccountId();
        mSubscriptionId = data.getSubscriptionId();
        mMimeType = mimeType;

        if (!isValid()) throw new IllegalArgumentException("mimeType: " + mimeType + ", data: " + mData);
    }

    public DrmRights(File rightsFile, String mimeType)
    {
        instantiate(rightsFile, mimeType);
    }

    public DrmRights(String rightsFilePath, String mimeType)
    {
        this(new File(rightsFilePath), mimeType);
    }

    public DrmRights(String rightsFilePath, String mimeType, String accountId)
    {
        this(new File(rightsFilePath), mimeType);
        mAccountId = accountId;
    }

    public DrmRights(String rightsFilePath, String mimeType, String accountId, String subscriptionId)
    {
        this(new File(rightsFilePath), mimeType);
        mAccountId = accountId;
        mSubscriptionId = subscriptionId;
    }

    private void instantiate(File rightsFile, String mimeType)
    {
        try
        {
            mData = Files.readAllBytes(rightsFile.toPath());
        }
        catch (IOException exception)
        {
            mData = null;
        }

        mMimeType = mimeType;
        mAccountId = NO_USER;
        mSubscriptionId = "";

        if (!isValid()) throw new IllegalArgumentException("mimeType: " + mimeType + ", data: " + mData);
    }

    boolean isValid()
    {
        return mMimeType != null && !mMimeType.isEmpty() && mData != null && mData.length > 0;
    }

    public String getAccountId()
    {
        return mAccountId;
    }

    public byte[] getData()
    {
        return mData;
    }

    public String getMimeType()
    {
        return mMimeType;
    }

    public String getSubscriptionId()
    {
        return mSubscriptionId;
    }
}

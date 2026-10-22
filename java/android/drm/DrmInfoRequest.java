package android.drm;

import java.util.HashMap;
import java.util.Iterator;

public class DrmInfoRequest
{
    public static final String ACCOUNT_ID = "account_id";
    public static final String SUBSCRIPTION_ID = "subscription_id";
    public static final int TYPE_REGISTRATION_INFO = 1;
    public static final int TYPE_RIGHTS_ACQUISITION_INFO = 3;
    public static final int TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO = 4;
    public static final int TYPE_UNREGISTRATION_INFO = 2;

    private final int mInfoType;
    private final String mMimeType;
    private final HashMap<String, Object> mRequestInformation = new HashMap<>();

    public DrmInfoRequest(int infoType, String mimeType)
    {
        mInfoType = infoType;
        mMimeType = mimeType;

        if (!isValid()) throw new IllegalArgumentException("infoType: " + infoType + ", mimeType: " + mimeType);
    }

    boolean isValid()
    {
        return mMimeType != null && !mMimeType.isEmpty() && mInfoType >= TYPE_REGISTRATION_INFO
            && mInfoType <= TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO;
    }

    public Object get(String key)
    {
        return mRequestInformation.get(key);
    }

    public int getInfoType()
    {
        return mInfoType;
    }

    public String getMimeType()
    {
        return mMimeType;
    }

    public Iterator<Object> iterator()
    {
        return mRequestInformation.values().iterator();
    }

    public Iterator<String> keyIterator()
    {
        return mRequestInformation.keySet().iterator();
    }

    public void put(String key, Object value)
    {
        mRequestInformation.put(key, value);
    }
}

package android.drm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;

public class DrmInfo
{
    private final int mInfoType;
    private final String mMimeType;
    private final HashMap<String, Object> mAttributes = new HashMap<>();
    private byte[] mData;

    public DrmInfo(int infoType, byte[] data, String mimeType)
    {
        mInfoType = infoType;
        mMimeType = mimeType;
        mData = data;

        if (!isValid()) throw new IllegalArgumentException("infoType: " + infoType + ", mimeType: " + mimeType + ", data: " + data);
    }

    public DrmInfo(int infoType, String path, String mimeType)
    {
        mInfoType = infoType;
        mMimeType = mimeType;

        try
        {
            mData = Files.readAllBytes(new File(path).toPath());
        }
        catch (IOException exception)
        {
            mData = null;
        }

        if (!isValid()) throw new IllegalArgumentException("infoType: " + infoType + ", mimeType: " + mimeType + ", path: " + path);
    }

    boolean isValid()
    {
        return mMimeType != null && !mMimeType.isEmpty() && mData != null && mData.length > 0
            && mInfoType >= DrmInfoRequest.TYPE_REGISTRATION_INFO && mInfoType <= DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO;
    }

    public Object get(String key)
    {
        return mAttributes.get(key);
    }

    public byte[] getData()
    {
        return mData;
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
        return mAttributes.values().iterator();
    }

    public Iterator<String> keyIterator()
    {
        return mAttributes.keySet().iterator();
    }

    public void put(String key, Object value)
    {
        mAttributes.put(key, value);
    }
}

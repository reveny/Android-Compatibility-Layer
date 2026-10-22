package android.drm;

import java.util.HashMap;

public class DrmEvent
{
    public static final String DRM_INFO_OBJECT = "drm_info_object";
    public static final String DRM_INFO_STATUS_OBJECT = "drm_info_status_object";
    public static final int TYPE_ALL_RIGHTS_REMOVED = 1001;
    public static final int TYPE_DRM_INFO_PROCESSED = 1002;

    private final int mUniqueId;
    private final int mType;
    private final String mMessage;
    private final HashMap<String, Object> mAttributes = new HashMap<>();

    protected DrmEvent(int uniqueId, int type, String message)
    {
        mUniqueId = uniqueId;
        mType = type;
        mMessage = message == null ? "" : message;
    }

    protected DrmEvent(int uniqueId, int type, String message, HashMap<String, Object> attributes)
    {
        this(uniqueId, type, message);

        if (attributes != null) mAttributes.putAll(attributes);
    }

    public Object getAttribute(String key)
    {
        return mAttributes.get(key);
    }

    public String getMessage()
    {
        return mMessage;
    }

    public int getType()
    {
        return mType;
    }

    public int getUniqueId()
    {
        return mUniqueId;
    }
}

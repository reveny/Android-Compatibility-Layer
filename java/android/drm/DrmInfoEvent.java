package android.drm;

import java.util.HashMap;

public class DrmInfoEvent extends DrmEvent
{
    public static final int TYPE_ACCOUNT_ALREADY_REGISTERED = 5;
    public static final int TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT = 1;
    public static final int TYPE_REMOVE_RIGHTS = 2;
    public static final int TYPE_RIGHTS_INSTALLED = 3;
    public static final int TYPE_RIGHTS_REMOVED = 6;
    public static final int TYPE_WAIT_FOR_RIGHTS = 4;

    public DrmInfoEvent(int uniqueId, int type, String message)
    {
        super(uniqueId, type, message);
        checkTypeValidity(type);
    }

    public DrmInfoEvent(int uniqueId, int type, String message, HashMap<String, Object> attributes)
    {
        super(uniqueId, type, message, attributes);
        checkTypeValidity(type);
    }

    private static void checkTypeValidity(int type)
    {
        boolean infoType = type >= TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT && type <= TYPE_RIGHTS_REMOVED;
        boolean eventType = type == TYPE_ALL_RIGHTS_REMOVED || type == TYPE_DRM_INFO_PROCESSED;

        if (!infoType && !eventType) throw new IllegalArgumentException("Unsupported type: " + type);
    }
}

package android.view.contentcapture;

import java.util.Set;

public final class ContentCaptureManager
{
    public static final int DATA_SHARE_ERROR_UNKNOWN = 1;
    public static final int DATA_SHARE_ERROR_CONCURRENT_REQUEST = 2;
    public static final int DATA_SHARE_ERROR_TIMEOUT_INTERRUPTED = 3;

    private static final ContentCaptureManager INSTANCE = new ContentCaptureManager();

    private final ContentCaptureSession mMainSession = new RuntimeContentCaptureSession();

    public ContentCaptureManager()
    {
    }

    public static ContentCaptureManager getInstance()
    {
        return INSTANCE;
    }

    public android.content.ComponentName getServiceComponentName()
    {
        return null;
    }

    public boolean isContentCaptureEnabled()
    {
        return false;
    }

    public Set<ContentCaptureCondition> getContentCaptureConditions()
    {
        return null;
    }

    public void setContentCaptureEnabled(boolean enabled)
    {
    }

    public ContentCaptureSession getMainContentCaptureSession()
    {
        return mMainSession;
    }
}

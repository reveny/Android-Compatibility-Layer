package android.media.projection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public final class MediaProjectionManager
{
    private static final String EXTRA_MEDIA_PROJECTION = "android.media.projection.extra.EXTRA_MEDIA_PROJECTION";
    private static final String EXTRA_MEDIA_PROJECTION_CONFIG = "android.media.projection.extra.EXTRA_MEDIA_PROJECTION_CONFIG";
    private static final int RESULT_OK = -1;

    private final Context mContext;

    public MediaProjectionManager(Context context)
    {
        mContext = context;
    }

    public Intent createScreenCaptureIntent()
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui",
            "com.android.systemui.mediaprojection.permission.MediaProjectionPermissionActivity"));
        return intent;
    }

    public Intent createScreenCaptureIntent(MediaProjectionConfig config)
    {
        if (config == null) throw new NullPointerException("config must not be null");

        Intent intent = createScreenCaptureIntent();
        intent.putExtra(EXTRA_MEDIA_PROJECTION_CONFIG, config);
        return intent;
    }

    public MediaProjection getMediaProjection(int resultCode, Intent resultData)
    {
        if (resultData == null) throw new NullPointerException("resultData must not be null");
        if (resultCode != RESULT_OK) return null;
        if (!resultData.hasExtra(EXTRA_MEDIA_PROJECTION)) return null;

        return new MediaProjection();
    }
}

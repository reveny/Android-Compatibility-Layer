package android.media;

public final class SyncParams
{
    public static final int AUDIO_ADJUST_MODE_DEFAULT = 0;
    public static final int AUDIO_ADJUST_MODE_RESAMPLE = 2;
    public static final int AUDIO_ADJUST_MODE_STRETCH = 1;
    public static final int SYNC_SOURCE_AUDIO = 2;
    public static final int SYNC_SOURCE_DEFAULT = 0;
    public static final int SYNC_SOURCE_SYSTEM_CLOCK = 1;
    public static final int SYNC_SOURCE_VSYNC = 3;

    private static final int SET_SYNC_SOURCE = 1;
    private static final int SET_AUDIO_ADJUST_MODE = 2;
    private static final int SET_TOLERANCE = 4;
    private static final int SET_FRAME_RATE = 8;

    private int mSet;
    private int mSyncSource = SYNC_SOURCE_DEFAULT;
    private int mAudioAdjustMode = AUDIO_ADJUST_MODE_DEFAULT;
    private float mTolerance;
    private float mFrameRate;

    public SyncParams()
    {
    }

    public SyncParams allowDefaults()
    {
        mSet |= SET_SYNC_SOURCE | SET_AUDIO_ADJUST_MODE | SET_TOLERANCE;
        return this;
    }

    public int getAudioAdjustMode()
    {
        if ((mSet & SET_AUDIO_ADJUST_MODE) == 0) throw new IllegalStateException("audio adjust mode not set");
        return mAudioAdjustMode;
    }

    public float getFrameRate()
    {
        if ((mSet & SET_FRAME_RATE) == 0) throw new IllegalStateException("frame rate not set");
        return mFrameRate;
    }

    public int getSyncSource()
    {
        if ((mSet & SET_SYNC_SOURCE) == 0) throw new IllegalStateException("sync source not set");
        return mSyncSource;
    }

    public float getTolerance()
    {
        if ((mSet & SET_TOLERANCE) == 0) throw new IllegalStateException("tolerance not set");
        return mTolerance;
    }

    public SyncParams setAudioAdjustMode(int audioAdjustMode)
    {
        mAudioAdjustMode = audioAdjustMode;
        mSet |= SET_AUDIO_ADJUST_MODE;
        return this;
    }

    public SyncParams setFrameRate(float frameRate)
    {
        mFrameRate = frameRate;
        mSet |= SET_FRAME_RATE;
        return this;
    }

    public SyncParams setSyncSource(int syncSource)
    {
        mSyncSource = syncSource;
        mSet |= SET_SYNC_SOURCE;
        return this;
    }

    public SyncParams setTolerance(float tolerance)
    {
        if (tolerance < 0.0f || tolerance >= 1.0f) throw new IllegalArgumentException("tolerance must be in [0, 1)");
        mTolerance = tolerance;
        mSet |= SET_TOLERANCE;
        return this;
    }
}

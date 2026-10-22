package android.media;

public abstract class VolumeProvider
{
    public static final int VOLUME_CONTROL_ABSOLUTE = 2;
    public static final int VOLUME_CONTROL_FIXED = 0;
    public static final int VOLUME_CONTROL_RELATIVE = 1;

    private final int mControlType;
    private final int mMaxVolume;
    private final String mControlId;
    private int mCurrentVolume;
    private Callback mCallback;

    public VolumeProvider(int volumeControl, int maxVolume, int currentVolume)
    {
        this(volumeControl, maxVolume, currentVolume, null);
    }

    public VolumeProvider(int volumeControl, int maxVolume, int currentVolume, String volumeControlId)
    {
        mControlType = volumeControl;
        mMaxVolume = maxVolume;
        mCurrentVolume = currentVolume;
        mControlId = volumeControlId;
    }

    public final int getCurrentVolume()
    {
        return mCurrentVolume;
    }

    public final int getMaxVolume()
    {
        return mMaxVolume;
    }

    public final int getVolumeControl()
    {
        return mControlType;
    }

    public final String getVolumeControlId()
    {
        return mControlId;
    }

    public void onAdjustVolume(int direction)
    {
    }

    public void onSetVolumeTo(int volume)
    {
    }

    public final void setCurrentVolume(int currentVolume)
    {
        mCurrentVolume = currentVolume;

        Callback callback = mCallback;
        if (callback != null) callback.onVolumeChanged(this);
    }

    public void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    public abstract static class Callback
    {
        public abstract void onVolumeChanged(VolumeProvider volumeProvider);
    }
}

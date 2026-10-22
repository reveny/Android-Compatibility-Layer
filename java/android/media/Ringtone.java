package android.media;

import android.content.Context;
import android.net.Uri;

public class Ringtone
{
    private final Uri mUri;
    private AudioAttributes mAudioAttributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build();
    private float mVolume = 1.0f;
    private boolean mLooping;
    private boolean mPlaying;
    private boolean mHapticGeneratorEnabled;

    Ringtone(Uri uri)
    {
        mUri = uri;
    }

    @Override
    protected void finalize()
    {
        stop();
    }

    public AudioAttributes getAudioAttributes()
    {
        return mAudioAttributes;
    }

    @Deprecated
    public int getStreamType()
    {
        return mAudioAttributes.getVolumeControlStream();
    }

    public String getTitle(Context context)
    {
        if (mUri == null) return "Silent";
        if (RingtoneManager.isDefault(mUri)) return "Default ringtone";

        String segment = mUri.getLastPathSegment();
        return segment == null || segment.isEmpty() ? "Unknown ringtone" : segment;
    }

    public float getVolume()
    {
        return mVolume;
    }

    public boolean isHapticGeneratorEnabled()
    {
        return mHapticGeneratorEnabled;
    }

    public boolean isLooping()
    {
        return mLooping;
    }

    public synchronized boolean isPlaying()
    {
        return mPlaying;
    }

    public synchronized void play()
    {
        mPlaying = mUri != null;
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException
    {
        if (attributes == null) throw new IllegalArgumentException("Invalid null AudioAttributes for Ringtone");
        mAudioAttributes = attributes;
    }

    public boolean setHapticGeneratorEnabled(boolean enabled)
    {
        return false;
    }

    public void setLooping(boolean looping)
    {
        mLooping = looping;
    }

    @Deprecated
    public void setStreamType(int streamType)
    {
        setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
    }

    public void setVolume(float volume)
    {
        mVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public synchronized void stop()
    {
        mPlaying = false;
    }

    Uri getUri()
    {
        return mUri;
    }
}

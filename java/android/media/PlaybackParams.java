package android.media;

import android.os.Parcel;
import android.os.Parcelable;

public final class PlaybackParams implements Parcelable
{
    public static final int AUDIO_FALLBACK_MODE_DEFAULT = 0;
    public static final int AUDIO_FALLBACK_MODE_FAIL = 2;
    public static final int AUDIO_FALLBACK_MODE_MUTE = 1;

    private static final int SET_SPEED = 1;
    private static final int SET_PITCH = 2;
    private static final int SET_AUDIO_FALLBACK_MODE = 4;

    public static final Parcelable.Creator<PlaybackParams> CREATOR = new Parcelable.Creator<PlaybackParams>()
    {
        @Override
        public PlaybackParams createFromParcel(Parcel source)
        {
            PlaybackParams params = new PlaybackParams();
            params.mSet = source.readInt();
            params.mSpeed = source.readFloat();
            params.mPitch = source.readFloat();
            params.mAudioFallbackMode = source.readInt();
            return params;
        }

        @Override
        public PlaybackParams[] newArray(int size)
        {
            return new PlaybackParams[size];
        }
    };

    private int mSet;
    private float mSpeed = 1.0f;
    private float mPitch = 1.0f;
    private int mAudioFallbackMode = AUDIO_FALLBACK_MODE_DEFAULT;

    public PlaybackParams()
    {
    }

    public PlaybackParams allowDefaults()
    {
        mSet |= SET_SPEED | SET_PITCH | SET_AUDIO_FALLBACK_MODE;
        return this;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public int getAudioFallbackMode()
    {
        if ((mSet & SET_AUDIO_FALLBACK_MODE) == 0) throw new IllegalStateException("audio fallback mode not set");
        return mAudioFallbackMode;
    }

    public float getPitch()
    {
        if ((mSet & SET_PITCH) == 0) throw new IllegalStateException("pitch not set");
        return mPitch;
    }

    public float getSpeed()
    {
        if ((mSet & SET_SPEED) == 0) throw new IllegalStateException("speed not set");
        return mSpeed;
    }

    public PlaybackParams setAudioFallbackMode(int audioFallbackMode)
    {
        mAudioFallbackMode = audioFallbackMode;
        mSet |= SET_AUDIO_FALLBACK_MODE;
        return this;
    }

    public PlaybackParams setPitch(float pitch)
    {
        if (pitch < 0.0f) throw new IllegalArgumentException("pitch must not be negative");
        mPitch = pitch;
        mSet |= SET_PITCH;
        return this;
    }

    public PlaybackParams setSpeed(float speed)
    {
        mSpeed = speed;
        mSet |= SET_SPEED;
        return this;
    }

    float speedOrDefault()
    {
        return (mSet & SET_SPEED) == 0 ? 1.0f : mSpeed;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mSet);
        destination.writeFloat(mSpeed);
        destination.writeFloat(mPitch);
        destination.writeInt(mAudioFallbackMode);
    }
}

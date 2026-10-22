package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AudioPlaybackConfiguration implements Parcelable
{
    public static final Parcelable.Creator<AudioPlaybackConfiguration> CREATOR =
        new Parcelable.Creator<AudioPlaybackConfiguration>()
    {
        @Override
        public AudioPlaybackConfiguration createFromParcel(Parcel source)
        {
            int playerInterfaceId = source.readInt();
            int sessionId = source.readInt();
            AudioAttributes attributes = AudioAttributes.CREATOR.createFromParcel(source);
            return new AudioPlaybackConfiguration(playerInterfaceId, sessionId, attributes, null);
        }

        @Override
        public AudioPlaybackConfiguration[] newArray(int size)
        {
            return new AudioPlaybackConfiguration[size];
        }
    };

    private final int mPlayerInterfaceId;
    private final int mSessionId;
    private final AudioAttributes mAttributes;
    private final AudioDeviceInfo mDevice;

    AudioPlaybackConfiguration(int playerInterfaceId, int sessionId, AudioAttributes attributes, AudioDeviceInfo device)
    {
        mPlayerInterfaceId = playerInterfaceId;
        mSessionId = sessionId;
        mAttributes = attributes == null ? new AudioAttributes.Builder().build() : attributes;
        mDevice = device;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof AudioPlaybackConfiguration)) return false;

        AudioPlaybackConfiguration other = (AudioPlaybackConfiguration)object;
        return mPlayerInterfaceId == other.mPlayerInterfaceId && mSessionId == other.mSessionId;
    }

    public AudioAttributes getAudioAttributes()
    {
        return mAttributes;
    }

    public AudioDeviceInfo getAudioDeviceInfo()
    {
        return mDevice;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPlayerInterfaceId, mSessionId);
    }

    @Override
    public String toString()
    {
        return "AudioPlaybackConfiguration piid:" + mPlayerInterfaceId + " session:" + mSessionId + " " + mAttributes;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mPlayerInterfaceId);
        parcel.writeInt(mSessionId);
        mAttributes.writeToParcel(parcel, flags);
    }
}

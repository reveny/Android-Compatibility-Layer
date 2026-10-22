package android.media;

import android.media.audiofx.AudioEffect;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AudioRecordingConfiguration implements Parcelable
{
    public static final Parcelable.Creator<AudioRecordingConfiguration> CREATOR =
        new Parcelable.Creator<AudioRecordingConfiguration>()
    {
        @Override
        public AudioRecordingConfiguration createFromParcel(Parcel source)
        {
            int sessionId = source.readInt();
            int clientSource = source.readInt();
            int deviceSource = source.readInt();
            AudioFormat clientFormat = AudioFormat.CREATOR.createFromParcel(source);
            AudioFormat deviceFormat = AudioFormat.CREATOR.createFromParcel(source);
            boolean silenced = source.readBoolean();
            return new AudioRecordingConfiguration(sessionId, clientSource, deviceSource, clientFormat, deviceFormat, silenced,
                null);
        }

        @Override
        public AudioRecordingConfiguration[] newArray(int size)
        {
            return new AudioRecordingConfiguration[size];
        }
    };

    private final int mClientSessionId;
    private final int mClientSource;
    private final int mDeviceSource;
    private final AudioFormat mClientFormat;
    private final AudioFormat mDeviceFormat;
    private final boolean mClientSilenced;
    private final AudioDeviceInfo mDevice;

    AudioRecordingConfiguration(int clientSessionId, int clientSource, int deviceSource, AudioFormat clientFormat,
        AudioFormat deviceFormat, boolean clientSilenced, AudioDeviceInfo device)
    {
        mClientSessionId = clientSessionId;
        mClientSource = clientSource;
        mDeviceSource = deviceSource;
        mClientFormat = clientFormat;
        mDeviceFormat = deviceFormat;
        mClientSilenced = clientSilenced;
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
        if (!(object instanceof AudioRecordingConfiguration)) return false;

        AudioRecordingConfiguration other = (AudioRecordingConfiguration)object;
        return mClientSessionId == other.mClientSessionId && mClientSource == other.mClientSource
            && mDeviceSource == other.mDeviceSource && mClientSilenced == other.mClientSilenced
            && mClientFormat.equals(other.mClientFormat) && mDeviceFormat.equals(other.mDeviceFormat);
    }

    public AudioDeviceInfo getAudioDevice()
    {
        return mDevice;
    }

    public int getAudioSource()
    {
        return mDeviceSource;
    }

    public int getClientAudioSessionId()
    {
        return mClientSessionId;
    }

    public int getClientAudioSource()
    {
        return mClientSource;
    }

    public List<AudioEffect.Descriptor> getClientEffects()
    {
        return new ArrayList<>();
    }

    public AudioFormat getClientFormat()
    {
        return mClientFormat;
    }

    public List<AudioEffect.Descriptor> getEffects()
    {
        return new ArrayList<>();
    }

    public AudioFormat getFormat()
    {
        return mDeviceFormat;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mClientSessionId, mClientSource, mDeviceSource, mClientFormat, mDeviceFormat, mClientSilenced);
    }

    public boolean isClientSilenced()
    {
        return mClientSilenced;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mClientSessionId);
        parcel.writeInt(mClientSource);
        parcel.writeInt(mDeviceSource);
        mClientFormat.writeToParcel(parcel, flags);
        mDeviceFormat.writeToParcel(parcel, flags);
        parcel.writeBoolean(mClientSilenced);
    }
}

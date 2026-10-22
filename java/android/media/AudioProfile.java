package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

public class AudioProfile implements Parcelable
{
    public static final int AUDIO_ENCAPSULATION_TYPE_IEC61937 = 1;
    public static final int AUDIO_ENCAPSULATION_TYPE_NONE = 0;
    public static final int AUDIO_ENCAPSULATION_TYPE_PCM = 2;

    public static final Parcelable.Creator<AudioProfile> CREATOR = new Parcelable.Creator<AudioProfile>()
    {
        @Override
        public AudioProfile createFromParcel(Parcel source)
        {
            int format = source.readInt();
            int[] sampleRates = source.createIntArray();
            int[] channelMasks = source.createIntArray();
            int[] channelIndexMasks = source.createIntArray();
            int encapsulationType = source.readInt();
            return new AudioProfile(format, sampleRates, channelMasks, channelIndexMasks, encapsulationType);
        }

        @Override
        public AudioProfile[] newArray(int size)
        {
            return new AudioProfile[size];
        }
    };

    private final int mFormat;
    private final int[] mSampleRates;
    private final int[] mChannelMasks;
    private final int[] mChannelIndexMasks;
    private final int mEncapsulationType;

    AudioProfile(int format, int[] sampleRates, int[] channelMasks, int[] channelIndexMasks, int encapsulationType)
    {
        mFormat = format;
        mSampleRates = sampleRates == null ? new int[0] : sampleRates;
        mChannelMasks = channelMasks == null ? new int[0] : channelMasks;
        mChannelIndexMasks = channelIndexMasks == null ? new int[0] : channelIndexMasks;
        mEncapsulationType = encapsulationType;
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
        if (!(object instanceof AudioProfile)) return false;

        AudioProfile other = (AudioProfile)object;
        return mFormat == other.mFormat && mEncapsulationType == other.mEncapsulationType
            && Arrays.equals(mSampleRates, other.mSampleRates) && Arrays.equals(mChannelMasks, other.mChannelMasks)
            && Arrays.equals(mChannelIndexMasks, other.mChannelIndexMasks);
    }

    public int[] getChannelIndexMasks()
    {
        return mChannelIndexMasks;
    }

    public int[] getChannelMasks()
    {
        return mChannelMasks;
    }

    public int getEncapsulationType()
    {
        return mEncapsulationType;
    }

    public int getFormat()
    {
        return mFormat;
    }

    public int[] getSampleRates()
    {
        return mSampleRates;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mFormat, Arrays.hashCode(mSampleRates), Arrays.hashCode(mChannelMasks),
            Arrays.hashCode(mChannelIndexMasks), mEncapsulationType);
    }

    @Override
    public String toString()
    {
        return "{format=" + mFormat + ", sampling rates=" + Arrays.toString(mSampleRates) + ", channel masks="
            + Arrays.toString(mChannelMasks) + ", channel index masks=" + Arrays.toString(mChannelIndexMasks)
            + ", encapsulation type=" + mEncapsulationType + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mFormat);
        parcel.writeIntArray(mSampleRates);
        parcel.writeIntArray(mChannelMasks);
        parcel.writeIntArray(mChannelIndexMasks);
        parcel.writeInt(mEncapsulationType);
    }
}

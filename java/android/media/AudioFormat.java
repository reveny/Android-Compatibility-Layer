package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AudioFormat implements Parcelable
{
    public static final int CHANNEL_CONFIGURATION_DEFAULT = 1;
    public static final int CHANNEL_CONFIGURATION_INVALID = 0;
    public static final int CHANNEL_CONFIGURATION_MONO = 2;
    public static final int CHANNEL_CONFIGURATION_STEREO = 3;
    public static final int CHANNEL_INVALID = 0;
    public static final int CHANNEL_IN_BACK = 32;
    public static final int CHANNEL_IN_BACK_PROCESSED = 512;
    public static final int CHANNEL_IN_DEFAULT = 1;
    public static final int CHANNEL_IN_FRONT = 16;
    public static final int CHANNEL_IN_FRONT_PROCESSED = 256;
    public static final int CHANNEL_IN_LEFT = 4;
    public static final int CHANNEL_IN_LEFT_PROCESSED = 64;
    public static final int CHANNEL_IN_MONO = 16;
    public static final int CHANNEL_IN_PRESSURE = 1024;
    public static final int CHANNEL_IN_RIGHT = 8;
    public static final int CHANNEL_IN_RIGHT_PROCESSED = 128;
    public static final int CHANNEL_IN_STEREO = 12;
    public static final int CHANNEL_IN_VOICE_DNLINK = 32768;
    public static final int CHANNEL_IN_VOICE_UPLINK = 16384;
    public static final int CHANNEL_IN_X_AXIS = 2048;
    public static final int CHANNEL_IN_Y_AXIS = 4096;
    public static final int CHANNEL_IN_Z_AXIS = 8192;
    public static final int CHANNEL_OUT_5POINT1 = 252;
    public static final int CHANNEL_OUT_5POINT1POINT2 = 3145980;
    public static final int CHANNEL_OUT_5POINT1POINT4 = 737532;
    public static final int CHANNEL_OUT_6POINT1 = 1276;
    public static final int CHANNEL_OUT_7POINT1 = 1020;
    public static final int CHANNEL_OUT_7POINT1POINT2 = 3152124;
    public static final int CHANNEL_OUT_7POINT1POINT4 = 743676;
    public static final int CHANNEL_OUT_7POINT1_SURROUND = 6396;
    public static final int CHANNEL_OUT_9POINT1POINT4 = 202070268;
    public static final int CHANNEL_OUT_9POINT1POINT6 = 205215996;
    public static final int CHANNEL_OUT_BACK_CENTER = 1024;
    public static final int CHANNEL_OUT_BACK_LEFT = 64;
    public static final int CHANNEL_OUT_BACK_RIGHT = 128;
    public static final int CHANNEL_OUT_BOTTOM_FRONT_CENTER = 8388608;
    public static final int CHANNEL_OUT_BOTTOM_FRONT_LEFT = 4194304;
    public static final int CHANNEL_OUT_BOTTOM_FRONT_RIGHT = 16777216;
    public static final int CHANNEL_OUT_DEFAULT = 1;
    public static final int CHANNEL_OUT_FRONT_CENTER = 16;
    public static final int CHANNEL_OUT_FRONT_LEFT = 4;
    public static final int CHANNEL_OUT_FRONT_LEFT_OF_CENTER = 256;
    public static final int CHANNEL_OUT_FRONT_RIGHT = 8;
    public static final int CHANNEL_OUT_FRONT_RIGHT_OF_CENTER = 512;
    public static final int CHANNEL_OUT_FRONT_WIDE_LEFT = 67108864;
    public static final int CHANNEL_OUT_FRONT_WIDE_RIGHT = 134217728;
    public static final int CHANNEL_OUT_LOW_FREQUENCY = 32;
    public static final int CHANNEL_OUT_LOW_FREQUENCY_2 = 33554432;
    public static final int CHANNEL_OUT_MONO = 4;
    public static final int CHANNEL_OUT_QUAD = 204;
    public static final int CHANNEL_OUT_SIDE_LEFT = 2048;
    public static final int CHANNEL_OUT_SIDE_RIGHT = 4096;
    public static final int CHANNEL_OUT_STEREO = 12;
    public static final int CHANNEL_OUT_SURROUND = 1052;
    public static final int CHANNEL_OUT_TOP_BACK_CENTER = 262144;
    public static final int CHANNEL_OUT_TOP_BACK_LEFT = 131072;
    public static final int CHANNEL_OUT_TOP_BACK_RIGHT = 524288;
    public static final int CHANNEL_OUT_TOP_CENTER = 8192;
    public static final int CHANNEL_OUT_TOP_FRONT_CENTER = 32768;
    public static final int CHANNEL_OUT_TOP_FRONT_LEFT = 16384;
    public static final int CHANNEL_OUT_TOP_FRONT_RIGHT = 65536;
    public static final int CHANNEL_OUT_TOP_SIDE_LEFT = 1048576;
    public static final int CHANNEL_OUT_TOP_SIDE_RIGHT = 2097152;
    public static final int ENCODING_AAC_ELD = 15;
    public static final int ENCODING_AAC_HE_V1 = 11;
    public static final int ENCODING_AAC_HE_V2 = 12;
    public static final int ENCODING_AAC_LC = 10;
    public static final int ENCODING_AAC_XHE = 16;
    public static final int ENCODING_AC3 = 5;
    public static final int ENCODING_AC4 = 17;
    public static final int ENCODING_DEFAULT = 1;
    public static final int ENCODING_DOLBY_MAT = 19;
    public static final int ENCODING_DOLBY_TRUEHD = 14;
    public static final int ENCODING_DRA = 28;
    public static final int ENCODING_DSD = 31;
    public static final int ENCODING_DTS = 7;
    public static final int ENCODING_DTS_HD = 8;
    public static final int ENCODING_DTS_HD_MA = 29;
    public static final int ENCODING_DTS_UHD = 27;
    public static final int ENCODING_DTS_UHD_P1 = 27;
    public static final int ENCODING_DTS_UHD_P2 = 30;
    public static final int ENCODING_E_AC3 = 6;
    public static final int ENCODING_E_AC3_JOC = 18;
    public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_AAC = 42;
    public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_FLAC = 43;
    public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_OPUS = 41;
    public static final int ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM = 44;
    public static final int ENCODING_IAMF_BASE_PROFILE_AAC = 38;
    public static final int ENCODING_IAMF_BASE_PROFILE_FLAC = 39;
    public static final int ENCODING_IAMF_BASE_PROFILE_OPUS = 37;
    public static final int ENCODING_IAMF_BASE_PROFILE_PCM = 40;
    public static final int ENCODING_IAMF_SIMPLE_PROFILE_AAC = 34;
    public static final int ENCODING_IAMF_SIMPLE_PROFILE_FLAC = 35;
    public static final int ENCODING_IAMF_SIMPLE_PROFILE_OPUS = 33;
    public static final int ENCODING_IAMF_SIMPLE_PROFILE_PCM = 36;
    public static final int ENCODING_IEC61937 = 13;
    public static final int ENCODING_INVALID = 0;
    public static final int ENCODING_MP3 = 9;
    public static final int ENCODING_MPEGH_BL_L3 = 23;
    public static final int ENCODING_MPEGH_BL_L4 = 24;
    public static final int ENCODING_MPEGH_LC_L3 = 25;
    public static final int ENCODING_MPEGH_LC_L4 = 26;
    public static final int ENCODING_OPUS = 20;
    public static final int ENCODING_PCM_16BIT = 2;
    public static final int ENCODING_PCM_24BIT_PACKED = 21;
    public static final int ENCODING_PCM_32BIT = 22;
    public static final int ENCODING_PCM_8BIT = 3;
    public static final int ENCODING_PCM_FLOAT = 4;
    public static final int SAMPLE_RATE_UNSPECIFIED = 0;

    public static final int SAMPLE_RATE_HZ_MIN = 4000;
    public static final int SAMPLE_RATE_HZ_MAX = 1600000;

    private static final int PROPERTY_ENCODING = 1;
    private static final int PROPERTY_SAMPLE_RATE = 2;
    private static final int PROPERTY_CHANNEL_MASK = 4;
    private static final int PROPERTY_CHANNEL_INDEX_MASK = 8;
    private static final int CHANNEL_COUNT_MAX = 30;

    public static final Parcelable.Creator<AudioFormat> CREATOR = new Parcelable.Creator<AudioFormat>()
    {
        @Override
        public AudioFormat createFromParcel(Parcel source)
        {
            int propertySetMask = source.readInt();
            int encoding = source.readInt();
            int sampleRate = source.readInt();
            int channelMask = source.readInt();
            int channelIndexMask = source.readInt();
            return new AudioFormat(propertySetMask, encoding, sampleRate, channelMask, channelIndexMask);
        }

        @Override
        public AudioFormat[] newArray(int size)
        {
            return new AudioFormat[size];
        }
    };

    private final int mPropertySetMask;
    private final int mEncoding;
    private final int mSampleRate;
    private final int mChannelMask;
    private final int mChannelIndexMask;
    private final int mChannelCount;
    private final int mFrameSizeInBytes;

    private AudioFormat(int propertySetMask, int encoding, int sampleRate, int channelMask, int channelIndexMask)
    {
        mPropertySetMask = propertySetMask;
        mEncoding = (propertySetMask & PROPERTY_ENCODING) != 0 ? encoding : ENCODING_INVALID;
        mSampleRate = (propertySetMask & PROPERTY_SAMPLE_RATE) != 0 ? sampleRate : SAMPLE_RATE_UNSPECIFIED;
        mChannelMask = (propertySetMask & PROPERTY_CHANNEL_MASK) != 0 ? channelMask : CHANNEL_INVALID;
        mChannelIndexMask = (propertySetMask & PROPERTY_CHANNEL_INDEX_MASK) != 0 ? channelIndexMask : 0;

        int count = 0;
        if (mChannelIndexMask != 0)
        {
            count = Integer.bitCount(mChannelIndexMask);
        }
        else if (mChannelMask != CHANNEL_INVALID)
        {
            count = Integer.bitCount(mChannelMask);
        }

        mChannelCount = count;
        mFrameSizeInBytes = isEncodingLinearFrames(mEncoding) && count > 0 ? count * getBytesPerSample(mEncoding) : 1;
    }

    public static int getBytesPerSample(int encoding)
    {
        switch (encoding)
        {
            case ENCODING_PCM_8BIT:
                return 1;
            case ENCODING_PCM_16BIT:
            case ENCODING_IEC61937:
            case ENCODING_DEFAULT:
                return 2;
            case ENCODING_PCM_24BIT_PACKED:
                return 3;
            case ENCODING_PCM_FLOAT:
            case ENCODING_PCM_32BIT:
                return 4;
            default:
                throw new IllegalArgumentException("Bad audio format " + encoding);
        }
    }

    public static boolean isValidEncoding(int encoding)
    {
        return (encoding >= ENCODING_DEFAULT && encoding <= ENCODING_DSD)
            || (encoding >= ENCODING_IAMF_SIMPLE_PROFILE_OPUS && encoding <= ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM);
    }

    public static boolean isEncodingLinearPcm(int encoding)
    {
        switch (encoding)
        {
            case ENCODING_PCM_8BIT:
            case ENCODING_PCM_16BIT:
            case ENCODING_PCM_FLOAT:
            case ENCODING_PCM_24BIT_PACKED:
            case ENCODING_PCM_32BIT:
            case ENCODING_DEFAULT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isEncodingLinearFrames(int encoding)
    {
        return isEncodingLinearPcm(encoding) || encoding == ENCODING_IEC61937;
    }

    public static int channelCountFromOutChannelMask(int mask)
    {
        return Integer.bitCount(mask);
    }

    public static int channelCountFromInChannelMask(int mask)
    {
        return Integer.bitCount(mask);
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
        if (!(object instanceof AudioFormat)) return false;

        AudioFormat other = (AudioFormat)object;
        return mPropertySetMask == other.mPropertySetMask && mEncoding == other.mEncoding && mSampleRate == other.mSampleRate
            && mChannelMask == other.mChannelMask && mChannelIndexMask == other.mChannelIndexMask;
    }

    public int getChannelCount()
    {
        return mChannelCount;
    }

    public int getChannelIndexMask()
    {
        return mChannelIndexMask;
    }

    public int getChannelMask()
    {
        return mChannelMask;
    }

    public int getEncoding()
    {
        return mEncoding;
    }

    public int getFrameSizeInBytes()
    {
        return mFrameSizeInBytes;
    }

    public int getSampleRate()
    {
        return mSampleRate;
    }

    public int getPropertySetMask()
    {
        return mPropertySetMask;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPropertySetMask, mSampleRate, mEncoding, mChannelMask, mChannelIndexMask);
    }

    @Override
    public String toString()
    {
        return "AudioFormat: props=" + mPropertySetMask + " enc=" + mEncoding + " chan=0x" + Integer.toHexString(mChannelMask)
            .toUpperCase() + " chan_index=0x" + Integer.toHexString(mChannelIndexMask).toUpperCase() + " rate=" + mSampleRate;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mPropertySetMask);
        parcel.writeInt(mEncoding);
        parcel.writeInt(mSampleRate);
        parcel.writeInt(mChannelMask);
        parcel.writeInt(mChannelIndexMask);
    }

    public static class Builder
    {
        private int mPropertySetMask;
        private int mEncoding = ENCODING_INVALID;
        private int mSampleRate = SAMPLE_RATE_UNSPECIFIED;
        private int mChannelMask = CHANNEL_INVALID;
        private int mChannelIndexMask;

        public Builder()
        {
        }

        public Builder(AudioFormat format)
        {
            mPropertySetMask = format.mPropertySetMask;
            mEncoding = format.mEncoding;
            mSampleRate = format.mSampleRate;
            mChannelMask = format.mChannelMask;
            mChannelIndexMask = format.mChannelIndexMask;
        }

        public AudioFormat build()
        {
            return new AudioFormat(mPropertySetMask, mEncoding, mSampleRate, mChannelMask, mChannelIndexMask);
        }

        public Builder setChannelIndexMask(int channelIndexMask)
        {
            if (channelIndexMask == 0) throw new IllegalArgumentException("Invalid zero channel index mask");

            int count = Integer.bitCount(channelIndexMask);
            if (count > CHANNEL_COUNT_MAX)
            {
                throw new IllegalArgumentException("Channel index mask " + channelIndexMask + " has too many channels");
            }

            if (mChannelMask != CHANNEL_INVALID && Integer.bitCount(mChannelMask) != count)
            {
                throw new IllegalArgumentException("Mismatched channel count for index mask " + channelIndexMask);
            }

            mChannelIndexMask = channelIndexMask;
            mPropertySetMask |= PROPERTY_CHANNEL_INDEX_MASK;
            return this;
        }

        public Builder setChannelMask(int channelMask)
        {
            if (channelMask == CHANNEL_INVALID) throw new IllegalArgumentException("Invalid zero channel mask");

            if (mChannelIndexMask != 0 && Integer.bitCount(channelMask) != Integer.bitCount(mChannelIndexMask))
            {
                throw new IllegalArgumentException("Mismatched channel count for mask " + Integer.toHexString(channelMask));
            }

            mChannelMask = channelMask;
            mPropertySetMask |= PROPERTY_CHANNEL_MASK;
            return this;
        }

        public Builder setEncoding(int encoding) throws IllegalArgumentException
        {
            if (!isValidEncoding(encoding)) throw new IllegalArgumentException("Invalid encoding " + encoding);

            mEncoding = encoding == ENCODING_DEFAULT ? ENCODING_PCM_16BIT : encoding;
            mPropertySetMask |= PROPERTY_ENCODING;
            return this;
        }

        public Builder setSampleRate(int sampleRate) throws IllegalArgumentException
        {
            if ((sampleRate < SAMPLE_RATE_HZ_MIN || sampleRate > SAMPLE_RATE_HZ_MAX) && sampleRate != SAMPLE_RATE_UNSPECIFIED)
            {
                throw new IllegalArgumentException("Invalid sample rate " + sampleRate);
            }

            mSampleRate = sampleRate;
            mPropertySetMask |= PROPERTY_SAMPLE_RATE;
            return this;
        }
    }
}

package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class TrackChangeEvent extends Event implements Parcelable
{
    public static final int TRACK_CHANGE_REASON_ADAPTIVE = 4;
    public static final int TRACK_CHANGE_REASON_INITIAL = 2;
    public static final int TRACK_CHANGE_REASON_MANUAL = 3;
    public static final int TRACK_CHANGE_REASON_OTHER = 1;
    public static final int TRACK_CHANGE_REASON_UNKNOWN = 0;
    public static final int TRACK_STATE_OFF = 0;
    public static final int TRACK_STATE_ON = 1;
    public static final int TRACK_TYPE_AUDIO = 0;
    public static final int TRACK_TYPE_TEXT = 2;
    public static final int TRACK_TYPE_VIDEO = 1;

    public static final Parcelable.Creator<TrackChangeEvent> CREATOR = new Parcelable.Creator<TrackChangeEvent>()
    {
        @Override
        public TrackChangeEvent createFromParcel(Parcel source)
        {
            Builder builder = new Builder(source.readInt());
            builder.mTrackState = source.readInt();
            builder.mTrackChangeReason = source.readInt();
            builder.mContainerMimeType = source.readString();
            builder.mSampleMimeType = source.readString();
            builder.mCodecName = source.readString();
            builder.mBitrate = source.readInt();
            builder.mTimeSinceCreatedMillis = source.readLong();
            builder.mLanguage = source.readString();
            builder.mLanguageRegion = source.readString();
            builder.mChannelCount = source.readInt();
            builder.mAudioSampleRate = source.readInt();
            builder.mWidth = source.readInt();
            builder.mHeight = source.readInt();
            builder.mVideoFrameRate = source.readFloat();
            builder.mMetricsBundle = source.readBundle();
            return builder.build();
        }

        @Override
        public TrackChangeEvent[] newArray(int size)
        {
            return new TrackChangeEvent[size];
        }
    };

    private final int mTrackType;
    private final int mTrackState;
    private final int mTrackChangeReason;
    private final String mContainerMimeType;
    private final String mSampleMimeType;
    private final String mCodecName;
    private final int mBitrate;
    private final String mLanguage;
    private final String mLanguageRegion;
    private final int mChannelCount;
    private final int mAudioSampleRate;
    private final int mWidth;
    private final int mHeight;
    private final float mVideoFrameRate;

    private TrackChangeEvent(Builder builder)
    {
        super(builder.mTimeSinceCreatedMillis, builder.mMetricsBundle);
        mTrackType = builder.mTrackType;
        mTrackState = builder.mTrackState;
        mTrackChangeReason = builder.mTrackChangeReason;
        mContainerMimeType = builder.mContainerMimeType;
        mSampleMimeType = builder.mSampleMimeType;
        mCodecName = builder.mCodecName;
        mBitrate = builder.mBitrate;
        mLanguage = builder.mLanguage;
        mLanguageRegion = builder.mLanguageRegion;
        mChannelCount = builder.mChannelCount;
        mAudioSampleRate = builder.mAudioSampleRate;
        mWidth = builder.mWidth;
        mHeight = builder.mHeight;
        mVideoFrameRate = builder.mVideoFrameRate;
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
        if (!(object instanceof TrackChangeEvent)) return false;

        TrackChangeEvent other = (TrackChangeEvent)object;
        return mTrackType == other.mTrackType && mTrackState == other.mTrackState && mTrackChangeReason == other.mTrackChangeReason
            && Objects.equals(mContainerMimeType, other.mContainerMimeType) && Objects.equals(mSampleMimeType, other.mSampleMimeType)
            && Objects.equals(mCodecName, other.mCodecName) && mBitrate == other.mBitrate
            && getTimeSinceCreatedMillis() == other.getTimeSinceCreatedMillis() && Objects.equals(mLanguage, other.mLanguage)
            && Objects.equals(mLanguageRegion, other.mLanguageRegion) && mChannelCount == other.mChannelCount
            && mAudioSampleRate == other.mAudioSampleRate && mWidth == other.mWidth && mHeight == other.mHeight
            && Float.compare(mVideoFrameRate, other.mVideoFrameRate) == 0;
    }

    public int getAudioSampleRate()
    {
        return mAudioSampleRate;
    }

    public int getBitrate()
    {
        return mBitrate;
    }

    public int getChannelCount()
    {
        return mChannelCount;
    }

    public String getCodecName()
    {
        return mCodecName;
    }

    public String getContainerMimeType()
    {
        return mContainerMimeType;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public String getLanguage()
    {
        return mLanguage;
    }

    public String getLanguageRegion()
    {
        return mLanguageRegion;
    }

    @Override
    public Bundle getMetricsBundle()
    {
        return super.getMetricsBundle();
    }

    public String getSampleMimeType()
    {
        return mSampleMimeType;
    }

    @Override
    public long getTimeSinceCreatedMillis()
    {
        return super.getTimeSinceCreatedMillis();
    }

    public int getTrackChangeReason()
    {
        return mTrackChangeReason;
    }

    public int getTrackState()
    {
        return mTrackState;
    }

    public int getTrackType()
    {
        return mTrackType;
    }

    public float getVideoFrameRate()
    {
        return mVideoFrameRate;
    }

    public int getWidth()
    {
        return mWidth;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTrackType, mTrackState, mTrackChangeReason, mContainerMimeType, mSampleMimeType, mCodecName, mBitrate,
            getTimeSinceCreatedMillis(), mLanguage, mLanguageRegion, mChannelCount, mAudioSampleRate, mWidth, mHeight, mVideoFrameRate);
    }

    @Override
    public String toString()
    {
        return "TrackChangeEvent { trackType = " + mTrackType + ", trackState = " + mTrackState + ", trackChangeReason = "
            + mTrackChangeReason + ", containerMimeType = " + mContainerMimeType + ", sampleMimeType = " + mSampleMimeType
            + ", codecName = " + mCodecName + ", bitrate = " + mBitrate + ", timeSinceCreatedMillis = " + getTimeSinceCreatedMillis()
            + ", language = " + mLanguage + ", languageRegion = " + mLanguageRegion + ", channelCount = " + mChannelCount
            + ", audioSampleRate = " + mAudioSampleRate + ", width = " + mWidth + ", height = " + mHeight + ", videoFrameRate = "
            + mVideoFrameRate + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mTrackType);
        destination.writeInt(mTrackState);
        destination.writeInt(mTrackChangeReason);
        destination.writeString(mContainerMimeType);
        destination.writeString(mSampleMimeType);
        destination.writeString(mCodecName);
        destination.writeInt(mBitrate);
        destination.writeLong(getTimeSinceCreatedMillis());
        destination.writeString(mLanguage);
        destination.writeString(mLanguageRegion);
        destination.writeInt(mChannelCount);
        destination.writeInt(mAudioSampleRate);
        destination.writeInt(mWidth);
        destination.writeInt(mHeight);
        destination.writeFloat(mVideoFrameRate);
        destination.writeBundle(getMetricsBundle());
    }

    public static final class Builder
    {
        private final int mTrackType;
        private int mTrackState = TRACK_STATE_OFF;
        private int mTrackChangeReason = TRACK_CHANGE_REASON_UNKNOWN;
        private String mContainerMimeType;
        private String mSampleMimeType;
        private String mCodecName;
        private int mBitrate = -1;
        private long mTimeSinceCreatedMillis = -1;
        private String mLanguage;
        private String mLanguageRegion;
        private int mChannelCount = -1;
        private int mAudioSampleRate = -1;
        private int mWidth = -1;
        private int mHeight = -1;
        private float mVideoFrameRate = -1;
        private Bundle mMetricsBundle = new Bundle();

        public Builder(int trackType)
        {
            if (trackType != TRACK_TYPE_AUDIO && trackType != TRACK_TYPE_VIDEO && trackType != TRACK_TYPE_TEXT)
            {
                throw new IllegalArgumentException("track type must be one of TRACK_TYPE_AUDIO, TRACK_TYPE_VIDEO, TRACK_TYPE_TEXT.");
            }

            mTrackType = trackType;
        }

        public TrackChangeEvent build()
        {
            return new TrackChangeEvent(this);
        }

        public Builder setAudioSampleRate(int audioSampleRate)
        {
            mAudioSampleRate = audioSampleRate;
            return this;
        }

        public Builder setBitrate(int bitrate)
        {
            mBitrate = bitrate;
            return this;
        }

        public Builder setChannelCount(int channelCount)
        {
            mChannelCount = channelCount;
            return this;
        }

        public Builder setCodecName(String codecName)
        {
            mCodecName = Objects.requireNonNull(codecName);
            return this;
        }

        public Builder setContainerMimeType(String containerMimeType)
        {
            mContainerMimeType = Objects.requireNonNull(containerMimeType);
            return this;
        }

        public Builder setHeight(int height)
        {
            mHeight = height;
            return this;
        }

        public Builder setLanguage(String language)
        {
            mLanguage = Objects.requireNonNull(language);
            return this;
        }

        public Builder setLanguageRegion(String languageRegion)
        {
            mLanguageRegion = Objects.requireNonNull(languageRegion);
            return this;
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setSampleMimeType(String sampleMimeType)
        {
            mSampleMimeType = Objects.requireNonNull(sampleMimeType);
            return this;
        }

        public Builder setTimeSinceCreatedMillis(long timeSinceCreatedMillis)
        {
            if (timeSinceCreatedMillis < 0) throw new IllegalArgumentException("Negative value: " + timeSinceCreatedMillis);

            mTimeSinceCreatedMillis = timeSinceCreatedMillis;
            return this;
        }

        public Builder setTrackChangeReason(int trackChangeReason)
        {
            mTrackChangeReason = trackChangeReason;
            return this;
        }

        public Builder setTrackState(int trackState)
        {
            mTrackState = trackState;
            return this;
        }

        public Builder setVideoFrameRate(float videoFrameRate)
        {
            if (videoFrameRate < 0 && videoFrameRate != -1)
            {
                throw new IllegalArgumentException("Video frame rate must be non-negative or -1 (unknown)");
            }

            mVideoFrameRate = videoFrameRate;
            return this;
        }

        public Builder setWidth(int width)
        {
            mWidth = width;
            return this;
        }
    }
}

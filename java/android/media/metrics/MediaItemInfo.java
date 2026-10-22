package android.media.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MediaItemInfo implements Parcelable
{
    public static final long DATA_TYPE_AUDIO = 4L;
    public static final long DATA_TYPE_DEPTH = 16L;
    public static final long DATA_TYPE_GAIN_MAP = 32L;
    public static final long DATA_TYPE_GAPLESS = 256L;
    public static final long DATA_TYPE_HIGH_DYNAMIC_RANGE_VIDEO = 1024L;
    public static final long DATA_TYPE_HIGH_FRAME_RATE = 64L;
    public static final long DATA_TYPE_IMAGE = 1L;
    public static final long DATA_TYPE_METADATA = 8L;
    public static final long DATA_TYPE_SPATIAL_AUDIO = 512L;
    public static final long DATA_TYPE_SPEED_SETTING_CUE_POINTS = 128L;
    public static final long DATA_TYPE_VIDEO = 2L;
    public static final int SOURCE_TYPE_CAMERA = 2;
    public static final int SOURCE_TYPE_EDITING_SESSION = 3;
    public static final int SOURCE_TYPE_GALLERY = 1;
    public static final int SOURCE_TYPE_GENERATED = 7;
    public static final int SOURCE_TYPE_LOCAL_FILE = 4;
    public static final int SOURCE_TYPE_REMOTE_FILE = 5;
    public static final int SOURCE_TYPE_REMOTE_LIVE_STREAM = 6;
    public static final int SOURCE_TYPE_UNSPECIFIED = 0;
    public static final int VALUE_UNSPECIFIED = -1;

    public static final Parcelable.Creator<MediaItemInfo> CREATOR = new Parcelable.Creator<MediaItemInfo>()
    {
        @Override
        public MediaItemInfo createFromParcel(Parcel source)
        {
            Builder builder = new Builder();
            builder.mSourceType = source.readInt();
            builder.mDataTypes = source.readLong();
            builder.mDurationMillis = source.readLong();
            builder.mClipDurationMillis = source.readLong();
            builder.mContainerMimeType = source.readString();
            builder.mSampleMimeTypes.addAll(source.createStringArrayList());
            builder.mCodecNames.addAll(source.createStringArrayList());
            builder.mAudioSampleRateHz = source.readInt();
            builder.mAudioChannelCount = source.readInt();
            builder.mAudioSampleCount = source.readLong();
            int width = source.readInt();
            int height = source.readInt();
            builder.mVideoSize = new Size(width, height);
            builder.mVideoDataSpace = source.readInt();
            builder.mVideoFrameRate = source.readFloat();
            builder.mVideoSampleCount = source.readLong();
            return builder.build();
        }

        @Override
        public MediaItemInfo[] newArray(int size)
        {
            return new MediaItemInfo[size];
        }
    };

    private final int mSourceType;
    private final long mDataTypes;
    private final long mDurationMillis;
    private final long mClipDurationMillis;
    private final String mContainerMimeType;
    private final List<String> mSampleMimeTypes;
    private final List<String> mCodecNames;
    private final int mAudioSampleRateHz;
    private final int mAudioChannelCount;
    private final long mAudioSampleCount;
    private final Size mVideoSize;
    private final int mVideoDataSpace;
    private final float mVideoFrameRate;
    private final long mVideoSampleCount;

    private MediaItemInfo(Builder builder)
    {
        mSourceType = builder.mSourceType;
        mDataTypes = builder.mDataTypes;
        mDurationMillis = builder.mDurationMillis;
        mClipDurationMillis = builder.mClipDurationMillis;
        mContainerMimeType = builder.mContainerMimeType;
        mSampleMimeTypes = new ArrayList<>(builder.mSampleMimeTypes);
        mCodecNames = new ArrayList<>(builder.mCodecNames);
        mAudioSampleRateHz = builder.mAudioSampleRateHz;
        mAudioChannelCount = builder.mAudioChannelCount;
        mAudioSampleCount = builder.mAudioSampleCount;
        mVideoSize = builder.mVideoSize;
        mVideoDataSpace = builder.mVideoDataSpace;
        mVideoFrameRate = builder.mVideoFrameRate;
        mVideoSampleCount = builder.mVideoSampleCount;
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
        if (!(object instanceof MediaItemInfo)) return false;

        MediaItemInfo other = (MediaItemInfo)object;
        return mSourceType == other.mSourceType && mDataTypes == other.mDataTypes && mDurationMillis == other.mDurationMillis
            && mClipDurationMillis == other.mClipDurationMillis && Objects.equals(mContainerMimeType, other.mContainerMimeType)
            && mSampleMimeTypes.equals(other.mSampleMimeTypes) && mCodecNames.equals(other.mCodecNames)
            && mAudioSampleRateHz == other.mAudioSampleRateHz && mAudioChannelCount == other.mAudioChannelCount
            && mAudioSampleCount == other.mAudioSampleCount && mVideoSize.equals(other.mVideoSize)
            && mVideoDataSpace == other.mVideoDataSpace && Float.compare(mVideoFrameRate, other.mVideoFrameRate) == 0
            && mVideoSampleCount == other.mVideoSampleCount;
    }

    public int getAudioChannelCount()
    {
        return mAudioChannelCount;
    }

    public long getAudioSampleCount()
    {
        return mAudioSampleCount;
    }

    public int getAudioSampleRateHz()
    {
        return mAudioSampleRateHz;
    }

    public long getClipDurationMillis()
    {
        return mClipDurationMillis;
    }

    public List<String> getCodecNames()
    {
        return new ArrayList<>(mCodecNames);
    }

    public String getContainerMimeType()
    {
        return mContainerMimeType;
    }

    public long getDataTypes()
    {
        return mDataTypes;
    }

    public long getDurationMillis()
    {
        return mDurationMillis;
    }

    public List<String> getSampleMimeTypes()
    {
        return new ArrayList<>(mSampleMimeTypes);
    }

    public int getSourceType()
    {
        return mSourceType;
    }

    public int getVideoDataSpace()
    {
        return mVideoDataSpace;
    }

    public float getVideoFrameRate()
    {
        return mVideoFrameRate;
    }

    public long getVideoSampleCount()
    {
        return mVideoSampleCount;
    }

    public Size getVideoSize()
    {
        return mVideoSize;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mSourceType, mDataTypes, mDurationMillis, mClipDurationMillis, mContainerMimeType, mSampleMimeTypes,
            mCodecNames, mAudioSampleRateHz, mAudioChannelCount, mAudioSampleCount, mVideoSize, mVideoDataSpace, mVideoFrameRate,
            mVideoSampleCount);
    }

    @Override
    public String toString()
    {
        return "MediaItemInfo { sourceType = " + mSourceType + ", dataTypes = " + mDataTypes + ", durationMillis = "
            + mDurationMillis + ", clipDurationMillis = " + mClipDurationMillis + ", containerMimeType = " + mContainerMimeType
            + ", sampleMimeTypes = " + mSampleMimeTypes + ", codecNames = " + mCodecNames + ", audioSampleRateHz = "
            + mAudioSampleRateHz + ", audioChannelCount = " + mAudioChannelCount + ", audioSampleCount = " + mAudioSampleCount
            + ", videoSize = " + mVideoSize + ", videoDataSpace = " + mVideoDataSpace + ", videoFrameRate = " + mVideoFrameRate
            + ", videoSampleCount = " + mVideoSampleCount + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mSourceType);
        destination.writeLong(mDataTypes);
        destination.writeLong(mDurationMillis);
        destination.writeLong(mClipDurationMillis);
        destination.writeString(mContainerMimeType);
        destination.writeStringList(mSampleMimeTypes);
        destination.writeStringList(mCodecNames);
        destination.writeInt(mAudioSampleRateHz);
        destination.writeInt(mAudioChannelCount);
        destination.writeLong(mAudioSampleCount);
        destination.writeInt(mVideoSize.getWidth());
        destination.writeInt(mVideoSize.getHeight());
        destination.writeInt(mVideoDataSpace);
        destination.writeFloat(mVideoFrameRate);
        destination.writeLong(mVideoSampleCount);
    }

    public static final class Builder
    {
        private int mSourceType = SOURCE_TYPE_UNSPECIFIED;
        private long mDataTypes;
        private long mDurationMillis = VALUE_UNSPECIFIED;
        private long mClipDurationMillis = VALUE_UNSPECIFIED;
        private String mContainerMimeType;
        private final List<String> mSampleMimeTypes = new ArrayList<>();
        private final List<String> mCodecNames = new ArrayList<>();
        private int mAudioSampleRateHz = VALUE_UNSPECIFIED;
        private int mAudioChannelCount = VALUE_UNSPECIFIED;
        private long mAudioSampleCount = VALUE_UNSPECIFIED;
        private Size mVideoSize = new Size(VALUE_UNSPECIFIED, VALUE_UNSPECIFIED);
        private int mVideoDataSpace;
        private float mVideoFrameRate = VALUE_UNSPECIFIED;
        private long mVideoSampleCount = VALUE_UNSPECIFIED;

        public Builder()
        {
        }

        public Builder addCodecName(String codecName)
        {
            mCodecNames.add(Objects.requireNonNull(codecName));
            return this;
        }

        public Builder addDataType(long dataType)
        {
            mDataTypes |= dataType;
            return this;
        }

        public Builder addSampleMimeType(String sampleMimeType)
        {
            mSampleMimeTypes.add(Objects.requireNonNull(sampleMimeType));
            return this;
        }

        public MediaItemInfo build()
        {
            return new MediaItemInfo(this);
        }

        public Builder setAudioChannelCount(int audioChannelCount)
        {
            mAudioChannelCount = audioChannelCount;
            return this;
        }

        public Builder setAudioSampleCount(long audioSampleCount)
        {
            mAudioSampleCount = audioSampleCount;
            return this;
        }

        public Builder setAudioSampleRateHz(int audioSampleRateHz)
        {
            mAudioSampleRateHz = audioSampleRateHz;
            return this;
        }

        public Builder setClipDurationMillis(long clipDurationMillis)
        {
            mClipDurationMillis = clipDurationMillis;
            return this;
        }

        public Builder setContainerMimeType(String containerMimeType)
        {
            mContainerMimeType = Objects.requireNonNull(containerMimeType);
            return this;
        }

        public Builder setDurationMillis(long durationMillis)
        {
            mDurationMillis = durationMillis;
            return this;
        }

        public Builder setSourceType(int sourceType)
        {
            mSourceType = sourceType;
            return this;
        }

        public Builder setVideoDataSpace(int videoDataSpace)
        {
            mVideoDataSpace = videoDataSpace;
            return this;
        }

        public Builder setVideoFrameRate(float videoFrameRate)
        {
            mVideoFrameRate = videoFrameRate;
            return this;
        }

        public Builder setVideoSampleCount(long videoSampleCount)
        {
            mVideoSampleCount = videoSampleCount;
            return this;
        }

        public Builder setVideoSize(Size videoSize)
        {
            mVideoSize = Objects.requireNonNull(videoSize);
            return this;
        }
    }
}

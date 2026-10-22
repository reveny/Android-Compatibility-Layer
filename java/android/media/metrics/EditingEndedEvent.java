package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EditingEndedEvent extends Event implements Parcelable
{
    public static final int ERROR_CODE_AUDIO_PROCESSING_FAILED = 18;
    public static final int ERROR_CODE_DECODER_INIT_FAILED = 11;
    public static final int ERROR_CODE_DECODING_FAILED = 12;
    public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 13;
    public static final int ERROR_CODE_ENCODER_INIT_FAILED = 14;
    public static final int ERROR_CODE_ENCODING_FAILED = 15;
    public static final int ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED = 16;
    public static final int ERROR_CODE_FAILED_RUNTIME_CHECK = 2;
    public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 6;
    public static final int ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 9;
    public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 7;
    public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 4;
    public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 5;
    public static final int ERROR_CODE_IO_NO_PERMISSION = 8;
    public static final int ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 10;
    public static final int ERROR_CODE_IO_UNSPECIFIED = 3;
    public static final int ERROR_CODE_MUXING_FAILED = 19;
    public static final int ERROR_CODE_NONE = 1;
    public static final int ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED = 17;
    public static final int FINAL_STATE_CANCELED = 2;
    public static final int FINAL_STATE_ERROR = 3;
    public static final int FINAL_STATE_SUCCEEDED = 1;
    public static final long OPERATION_TYPE_AUDIO_EDIT = 8L;
    public static final long OPERATION_TYPE_AUDIO_TRANSCODE = 2L;
    public static final long OPERATION_TYPE_AUDIO_TRANSMUX = 32L;
    public static final long OPERATION_TYPE_PAUSED = 64L;
    public static final long OPERATION_TYPE_RESUMED = 128L;
    public static final long OPERATION_TYPE_VIDEO_EDIT = 4L;
    public static final long OPERATION_TYPE_VIDEO_TRANSCODE = 1L;
    public static final long OPERATION_TYPE_VIDEO_TRANSMUX = 16L;
    public static final int PROGRESS_PERCENT_UNKNOWN = -1;
    public static final int TIME_SINCE_CREATED_UNKNOWN = -1;

    public static final Parcelable.Creator<EditingEndedEvent> CREATOR = new Parcelable.Creator<EditingEndedEvent>()
    {
        @Override
        public EditingEndedEvent createFromParcel(Parcel source)
        {
            Builder builder = new Builder(source.readInt());
            builder.mFinalProgressPercent = source.readFloat();
            builder.mErrorCode = source.readInt();
            builder.mTimeSinceCreatedMillis = source.readLong();
            builder.mExporterName = source.readString();
            builder.mMuxerName = source.readString();
            builder.mOperationTypes = source.readLong();
            builder.mInputMediaItemInfos.addAll(source.createTypedArrayList(MediaItemInfo.CREATOR));
            builder.mOutputMediaItemInfo = source.readTypedObject(MediaItemInfo.CREATOR);
            builder.mMetricsBundle = source.readBundle();
            return builder.build();
        }

        @Override
        public EditingEndedEvent[] newArray(int size)
        {
            return new EditingEndedEvent[size];
        }
    };

    private final int mFinalState;
    private final float mFinalProgressPercent;
    private final int mErrorCode;
    private final String mExporterName;
    private final String mMuxerName;
    private final long mOperationTypes;
    private final List<MediaItemInfo> mInputMediaItemInfos;
    private final MediaItemInfo mOutputMediaItemInfo;

    private EditingEndedEvent(Builder builder)
    {
        super(builder.mTimeSinceCreatedMillis, builder.mMetricsBundle);
        mFinalState = builder.mFinalState;
        mFinalProgressPercent = builder.mFinalProgressPercent;
        mErrorCode = builder.mErrorCode;
        mExporterName = builder.mExporterName;
        mMuxerName = builder.mMuxerName;
        mOperationTypes = builder.mOperationTypes;
        mInputMediaItemInfos = new ArrayList<>(builder.mInputMediaItemInfos);
        mOutputMediaItemInfo = builder.mOutputMediaItemInfo;
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
        if (!(object instanceof EditingEndedEvent)) return false;

        EditingEndedEvent other = (EditingEndedEvent)object;
        return mFinalState == other.mFinalState && Float.compare(mFinalProgressPercent, other.mFinalProgressPercent) == 0
            && mErrorCode == other.mErrorCode && getTimeSinceCreatedMillis() == other.getTimeSinceCreatedMillis()
            && Objects.equals(mExporterName, other.mExporterName) && Objects.equals(mMuxerName, other.mMuxerName)
            && mOperationTypes == other.mOperationTypes && mInputMediaItemInfos.equals(other.mInputMediaItemInfos)
            && Objects.equals(mOutputMediaItemInfo, other.mOutputMediaItemInfo);
    }

    public int getErrorCode()
    {
        return mErrorCode;
    }

    public String getExporterName()
    {
        return mExporterName;
    }

    public float getFinalProgressPercent()
    {
        return mFinalProgressPercent;
    }

    public int getFinalState()
    {
        return mFinalState;
    }

    public List<MediaItemInfo> getInputMediaItemInfos()
    {
        return new ArrayList<>(mInputMediaItemInfos);
    }

    @Override
    public Bundle getMetricsBundle()
    {
        return super.getMetricsBundle();
    }

    public String getMuxerName()
    {
        return mMuxerName;
    }

    public long getOperationTypes()
    {
        return mOperationTypes;
    }

    public MediaItemInfo getOutputMediaItemInfo()
    {
        return mOutputMediaItemInfo;
    }

    @Override
    public long getTimeSinceCreatedMillis()
    {
        return super.getTimeSinceCreatedMillis();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mFinalState, mFinalProgressPercent, mErrorCode, getTimeSinceCreatedMillis(), mExporterName, mMuxerName,
            mOperationTypes, mInputMediaItemInfos, mOutputMediaItemInfo);
    }

    @Override
    public String toString()
    {
        return "EditingEndedEvent { finalState = " + mFinalState + ", finalProgressPercent = " + mFinalProgressPercent
            + ", errorCode = " + mErrorCode + ", timeSinceCreatedMillis = " + getTimeSinceCreatedMillis() + ", exporterName = "
            + mExporterName + ", muxerName = " + mMuxerName + ", operationTypes = " + mOperationTypes
            + ", inputMediaItemInfos = " + mInputMediaItemInfos + ", outputMediaItemInfo = " + mOutputMediaItemInfo + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mFinalState);
        destination.writeFloat(mFinalProgressPercent);
        destination.writeInt(mErrorCode);
        destination.writeLong(getTimeSinceCreatedMillis());
        destination.writeString(mExporterName);
        destination.writeString(mMuxerName);
        destination.writeLong(mOperationTypes);
        destination.writeTypedList(mInputMediaItemInfos);
        destination.writeTypedObject(mOutputMediaItemInfo, flags);
        destination.writeBundle(getMetricsBundle());
    }

    public static final class Builder
    {
        private final int mFinalState;
        private float mFinalProgressPercent = PROGRESS_PERCENT_UNKNOWN;
        private int mErrorCode = ERROR_CODE_NONE;
        private long mTimeSinceCreatedMillis = TIME_SINCE_CREATED_UNKNOWN;
        private String mExporterName;
        private String mMuxerName;
        private long mOperationTypes;
        private final List<MediaItemInfo> mInputMediaItemInfos = new ArrayList<>();
        private MediaItemInfo mOutputMediaItemInfo;
        private Bundle mMetricsBundle = new Bundle();

        public Builder(int finalState)
        {
            mFinalState = finalState;
        }

        public Builder addInputMediaItemInfo(MediaItemInfo mediaItemInfo)
        {
            mInputMediaItemInfos.add(Objects.requireNonNull(mediaItemInfo));
            return this;
        }

        public Builder addOperationType(long operationType)
        {
            mOperationTypes |= operationType;
            return this;
        }

        public EditingEndedEvent build()
        {
            return new EditingEndedEvent(this);
        }

        public Builder setErrorCode(int errorCode)
        {
            mErrorCode = errorCode;
            return this;
        }

        public Builder setExporterName(String exporterName)
        {
            mExporterName = Objects.requireNonNull(exporterName);
            return this;
        }

        public Builder setFinalProgressPercent(float finalProgressPercent)
        {
            mFinalProgressPercent = finalProgressPercent;
            return this;
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setMuxerName(String muxerName)
        {
            mMuxerName = Objects.requireNonNull(muxerName);
            return this;
        }

        public Builder setOutputMediaItemInfo(MediaItemInfo mediaItemInfo)
        {
            mOutputMediaItemInfo = Objects.requireNonNull(mediaItemInfo);
            return this;
        }

        public Builder setTimeSinceCreatedMillis(long timeSinceCreatedMillis)
        {
            if (timeSinceCreatedMillis < 0) throw new IllegalArgumentException("Negative value: " + timeSinceCreatedMillis);

            mTimeSinceCreatedMillis = timeSinceCreatedMillis;
            return this;
        }
    }
}

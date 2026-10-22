package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public final class PlaybackErrorEvent extends Event implements Parcelable
{
    public static final int ERROR_AUDIO_TRACK_INIT_FAILED = 17;
    public static final int ERROR_AUDIO_TRACK_OTHER = 19;
    public static final int ERROR_AUDIO_TRACK_WRITE_FAILED = 18;
    public static final int ERROR_DECODER_INIT_FAILED = 13;
    public static final int ERROR_DECODING_FAILED = 14;
    public static final int ERROR_DECODING_FORMAT_EXCEEDS_CAPABILITIES = 15;
    public static final int ERROR_DECODING_FORMAT_UNSUPPORTED = 35;
    public static final int ERROR_DECODING_OTHER = 16;
    public static final int ERROR_DRM_CONTENT_ERROR = 28;
    public static final int ERROR_DRM_DEVICE_REVOKED = 29;
    public static final int ERROR_DRM_DISALLOWED_OPERATION = 26;
    public static final int ERROR_DRM_LICENSE_ACQUISITION_FAILED = 25;
    public static final int ERROR_DRM_OTHER = 30;
    public static final int ERROR_DRM_PROVISIONING_FAILED = 24;
    public static final int ERROR_DRM_SCHEME_UNSUPPORTED = 23;
    public static final int ERROR_DRM_SYSTEM_ERROR = 27;
    public static final int ERROR_IO_BAD_HTTP_STATUS = 5;
    public static final int ERROR_IO_CONNECTION_CLOSED = 8;
    public static final int ERROR_IO_CONNECTION_TIMEOUT = 7;
    public static final int ERROR_IO_DNS_FAILED = 6;
    public static final int ERROR_IO_FILE_NOT_FOUND = 31;
    public static final int ERROR_IO_NETWORK_CONNECTION_FAILED = 4;
    public static final int ERROR_IO_NETWORK_UNAVAILABLE = 3;
    public static final int ERROR_IO_NO_PERMISSION = 32;
    public static final int ERROR_IO_OTHER = 9;
    public static final int ERROR_OTHER = 1;
    public static final int ERROR_PARSING_CONTAINER_MALFORMED = 11;
    public static final int ERROR_PARSING_CONTAINER_UNSUPPORTED = 34;
    public static final int ERROR_PARSING_MANIFEST_MALFORMED = 10;
    public static final int ERROR_PARSING_MANIFEST_UNSUPPORTED = 33;
    public static final int ERROR_PARSING_OTHER = 12;
    public static final int ERROR_PLAYER_BEHIND_LIVE_WINDOW = 21;
    public static final int ERROR_PLAYER_OTHER = 22;
    public static final int ERROR_PLAYER_REMOTE = 20;
    public static final int ERROR_RUNTIME = 2;
    public static final int ERROR_UNKNOWN = 0;

    public static final Parcelable.Creator<PlaybackErrorEvent> CREATOR = new Parcelable.Creator<PlaybackErrorEvent>()
    {
        @Override
        public PlaybackErrorEvent createFromParcel(Parcel source)
        {
            String exceptionStack = source.readString();
            int errorCode = source.readInt();
            int subErrorCode = source.readInt();
            long timeSinceCreatedMillis = source.readLong();
            Bundle metricsBundle = source.readBundle();
            return new PlaybackErrorEvent(exceptionStack, errorCode, subErrorCode, timeSinceCreatedMillis, metricsBundle);
        }

        @Override
        public PlaybackErrorEvent[] newArray(int size)
        {
            return new PlaybackErrorEvent[size];
        }
    };

    private final String mExceptionStack;
    private final int mErrorCode;
    private final int mSubErrorCode;

    PlaybackErrorEvent(String exceptionStack, int errorCode, int subErrorCode, long timeSinceCreatedMillis, Bundle metricsBundle)
    {
        super(timeSinceCreatedMillis, metricsBundle);
        mExceptionStack = exceptionStack;
        mErrorCode = errorCode;
        mSubErrorCode = subErrorCode;
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
        if (!(object instanceof PlaybackErrorEvent)) return false;

        PlaybackErrorEvent other = (PlaybackErrorEvent)object;
        return Objects.equals(mExceptionStack, other.mExceptionStack) && mErrorCode == other.mErrorCode
            && mSubErrorCode == other.mSubErrorCode && getTimeSinceCreatedMillis() == other.getTimeSinceCreatedMillis();
    }

    public int getErrorCode()
    {
        return mErrorCode;
    }

    @Override
    public Bundle getMetricsBundle()
    {
        return super.getMetricsBundle();
    }

    public int getSubErrorCode()
    {
        return mSubErrorCode;
    }

    @Override
    public long getTimeSinceCreatedMillis()
    {
        return super.getTimeSinceCreatedMillis();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mExceptionStack, mErrorCode, mSubErrorCode, getTimeSinceCreatedMillis());
    }

    @Override
    public String toString()
    {
        return "PlaybackErrorEvent { errorCode = " + mErrorCode + ", subErrorCode = " + mSubErrorCode
            + ", timeSinceCreatedMillis = " + getTimeSinceCreatedMillis() + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mExceptionStack);
        destination.writeInt(mErrorCode);
        destination.writeInt(mSubErrorCode);
        destination.writeLong(getTimeSinceCreatedMillis());
        destination.writeBundle(getMetricsBundle());
    }

    public static final class Builder
    {
        private Exception mException;
        private int mErrorCode = ERROR_UNKNOWN;
        private int mSubErrorCode;
        private long mTimeSinceCreatedMillis = -1;
        private Bundle mMetricsBundle = new Bundle();

        public Builder()
        {
        }

        public PlaybackErrorEvent build()
        {
            String stack = null;

            if (mException != null)
            {
                StringWriter writer = new StringWriter();
                mException.printStackTrace(new PrintWriter(writer));
                stack = writer.toString();
            }

            return new PlaybackErrorEvent(stack, mErrorCode, mSubErrorCode, mTimeSinceCreatedMillis, mMetricsBundle);
        }

        public Builder setErrorCode(int errorCode)
        {
            mErrorCode = errorCode;
            return this;
        }

        public Builder setException(Exception exception)
        {
            mException = exception;
            return this;
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setSubErrorCode(int subErrorCode)
        {
            mSubErrorCode = subErrorCode;
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

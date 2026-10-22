package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PlaybackMetrics implements Parcelable
{
    public static final int CONTENT_TYPE_AD = 2;
    public static final int CONTENT_TYPE_MAIN = 1;
    public static final int CONTENT_TYPE_OTHER = 3;
    public static final int CONTENT_TYPE_UNKNOWN = 0;
    public static final int DRM_TYPE_CLEARKEY = 6;
    public static final int DRM_TYPE_NONE = 0;
    public static final int DRM_TYPE_OTHER = 1;
    public static final int DRM_TYPE_PLAY_READY = 2;
    public static final int DRM_TYPE_WIDEVINE_L1 = 3;
    public static final int DRM_TYPE_WIDEVINE_L3 = 4;
    public static final int DRM_TYPE_WV_L3_FALLBACK = 5;
    public static final int PLAYBACK_TYPE_LIVE = 2;
    public static final int PLAYBACK_TYPE_OTHER = 3;
    public static final int PLAYBACK_TYPE_UNKNOWN = 0;
    public static final int PLAYBACK_TYPE_VOD = 1;
    public static final int STREAM_SOURCE_DEVICE = 2;
    public static final int STREAM_SOURCE_MIXED = 3;
    public static final int STREAM_SOURCE_NETWORK = 1;
    public static final int STREAM_SOURCE_UNKNOWN = 0;
    public static final int STREAM_TYPE_DASH = 3;
    public static final int STREAM_TYPE_HLS = 4;
    public static final int STREAM_TYPE_OTHER = 1;
    public static final int STREAM_TYPE_PROGRESSIVE = 2;
    public static final int STREAM_TYPE_SS = 5;
    public static final int STREAM_TYPE_UNKNOWN = 0;

    public static final Parcelable.Creator<PlaybackMetrics> CREATOR = new Parcelable.Creator<PlaybackMetrics>()
    {
        @Override
        public PlaybackMetrics createFromParcel(Parcel source)
        {
            Builder builder = new Builder();
            builder.mMediaDurationMillis = source.readLong();
            builder.mStreamSource = source.readInt();
            builder.mStreamType = source.readInt();
            builder.mPlaybackType = source.readInt();
            builder.mDrmType = source.readInt();
            builder.mContentType = source.readInt();
            builder.mPlayerName = source.readString();
            builder.mPlayerVersion = source.readString();

            for (long experimentId : source.createLongArray())
            {
                builder.mExperimentIds.add(experimentId);
            }

            builder.mVideoFramesPlayed = source.readInt();
            builder.mVideoFramesDropped = source.readInt();
            builder.mAudioUnderrunCount = source.readInt();
            builder.mNetworkBytesRead = source.readLong();
            builder.mLocalBytesRead = source.readLong();
            builder.mNetworkTransferDurationMillis = source.readLong();
            builder.mDrmSessionId = source.createByteArray();
            builder.mMetricsBundle = source.readBundle();
            return builder.build();
        }

        @Override
        public PlaybackMetrics[] newArray(int size)
        {
            return new PlaybackMetrics[size];
        }
    };

    private final long mMediaDurationMillis;
    private final int mStreamSource;
    private final int mStreamType;
    private final int mPlaybackType;
    private final int mDrmType;
    private final int mContentType;
    private final String mPlayerName;
    private final String mPlayerVersion;
    private final long[] mExperimentIds;
    private final int mVideoFramesPlayed;
    private final int mVideoFramesDropped;
    private final int mAudioUnderrunCount;
    private final long mNetworkBytesRead;
    private final long mLocalBytesRead;
    private final long mNetworkTransferDurationMillis;
    private final byte[] mDrmSessionId;
    private final Bundle mMetricsBundle;

    private PlaybackMetrics(Builder builder)
    {
        mMediaDurationMillis = builder.mMediaDurationMillis;
        mStreamSource = builder.mStreamSource;
        mStreamType = builder.mStreamType;
        mPlaybackType = builder.mPlaybackType;
        mDrmType = builder.mDrmType;
        mContentType = builder.mContentType;
        mPlayerName = builder.mPlayerName;
        mPlayerVersion = builder.mPlayerVersion;
        mExperimentIds = new long[builder.mExperimentIds.size()];

        for (int index = 0; index < mExperimentIds.length; index++)
        {
            mExperimentIds[index] = builder.mExperimentIds.get(index);
        }

        mVideoFramesPlayed = builder.mVideoFramesPlayed;
        mVideoFramesDropped = builder.mVideoFramesDropped;
        mAudioUnderrunCount = builder.mAudioUnderrunCount;
        mNetworkBytesRead = builder.mNetworkBytesRead;
        mLocalBytesRead = builder.mLocalBytesRead;
        mNetworkTransferDurationMillis = builder.mNetworkTransferDurationMillis;
        mDrmSessionId = builder.mDrmSessionId == null ? new byte[0] : builder.mDrmSessionId.clone();
        mMetricsBundle = builder.mMetricsBundle == null ? new Bundle() : builder.mMetricsBundle;
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
        if (!(object instanceof PlaybackMetrics)) return false;

        PlaybackMetrics other = (PlaybackMetrics)object;
        return mMediaDurationMillis == other.mMediaDurationMillis && mStreamSource == other.mStreamSource
            && mStreamType == other.mStreamType && mPlaybackType == other.mPlaybackType && mDrmType == other.mDrmType
            && mContentType == other.mContentType && Objects.equals(mPlayerName, other.mPlayerName)
            && Objects.equals(mPlayerVersion, other.mPlayerVersion) && Arrays.equals(mExperimentIds, other.mExperimentIds)
            && mVideoFramesPlayed == other.mVideoFramesPlayed && mVideoFramesDropped == other.mVideoFramesDropped
            && mAudioUnderrunCount == other.mAudioUnderrunCount && mNetworkBytesRead == other.mNetworkBytesRead
            && mLocalBytesRead == other.mLocalBytesRead && mNetworkTransferDurationMillis == other.mNetworkTransferDurationMillis
            && Arrays.equals(mDrmSessionId, other.mDrmSessionId);
    }

    public int getAudioUnderrunCount()
    {
        return mAudioUnderrunCount;
    }

    public int getContentType()
    {
        return mContentType;
    }

    public byte[] getDrmSessionId()
    {
        return mDrmSessionId;
    }

    public int getDrmType()
    {
        return mDrmType;
    }

    public long[] getExperimentIds()
    {
        return Arrays.copyOf(mExperimentIds, mExperimentIds.length);
    }

    public long getLocalBytesRead()
    {
        return mLocalBytesRead;
    }

    public long getMediaDurationMillis()
    {
        return mMediaDurationMillis;
    }

    public Bundle getMetricsBundle()
    {
        return mMetricsBundle;
    }

    public long getNetworkBytesRead()
    {
        return mNetworkBytesRead;
    }

    public long getNetworkTransferDurationMillis()
    {
        return mNetworkTransferDurationMillis;
    }

    public int getPlaybackType()
    {
        return mPlaybackType;
    }

    public String getPlayerName()
    {
        return mPlayerName;
    }

    public String getPlayerVersion()
    {
        return mPlayerVersion;
    }

    public int getStreamSource()
    {
        return mStreamSource;
    }

    public int getStreamType()
    {
        return mStreamType;
    }

    public int getVideoFramesDropped()
    {
        return mVideoFramesDropped;
    }

    public int getVideoFramesPlayed()
    {
        return mVideoFramesPlayed;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mMediaDurationMillis, mStreamSource, mStreamType, mPlaybackType, mDrmType, mContentType, mPlayerName,
            mPlayerVersion, Arrays.hashCode(mExperimentIds), mVideoFramesPlayed, mVideoFramesDropped, mAudioUnderrunCount,
            mNetworkBytesRead, mLocalBytesRead, mNetworkTransferDurationMillis, Arrays.hashCode(mDrmSessionId));
    }

    @Override
    public String toString()
    {
        return "PlaybackMetrics { mediaDurationMillis = " + mMediaDurationMillis + ", streamSource = " + mStreamSource
            + ", streamType = " + mStreamType + ", playbackType = " + mPlaybackType + ", drmType = " + mDrmType
            + ", contentType = " + mContentType + ", playerName = " + mPlayerName + ", playerVersion = " + mPlayerVersion
            + ", experimentIds = " + Arrays.toString(mExperimentIds) + ", videoFramesPlayed = " + mVideoFramesPlayed
            + ", videoFramesDropped = " + mVideoFramesDropped + ", audioUnderrunCount = " + mAudioUnderrunCount
            + ", networkBytesRead = " + mNetworkBytesRead + ", localBytesRead = " + mLocalBytesRead
            + ", networkTransferDurationMillis = " + mNetworkTransferDurationMillis + ", drmSessionId = "
            + Arrays.toString(mDrmSessionId) + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeLong(mMediaDurationMillis);
        destination.writeInt(mStreamSource);
        destination.writeInt(mStreamType);
        destination.writeInt(mPlaybackType);
        destination.writeInt(mDrmType);
        destination.writeInt(mContentType);
        destination.writeString(mPlayerName);
        destination.writeString(mPlayerVersion);
        destination.writeLongArray(mExperimentIds);
        destination.writeInt(mVideoFramesPlayed);
        destination.writeInt(mVideoFramesDropped);
        destination.writeInt(mAudioUnderrunCount);
        destination.writeLong(mNetworkBytesRead);
        destination.writeLong(mLocalBytesRead);
        destination.writeLong(mNetworkTransferDurationMillis);
        destination.writeByteArray(mDrmSessionId);
        destination.writeBundle(mMetricsBundle);
    }

    public static final class Builder
    {
        private long mMediaDurationMillis = -1;
        private int mStreamSource = STREAM_SOURCE_UNKNOWN;
        private int mStreamType = STREAM_TYPE_UNKNOWN;
        private int mPlaybackType = PLAYBACK_TYPE_UNKNOWN;
        private int mDrmType = DRM_TYPE_NONE;
        private int mContentType = CONTENT_TYPE_UNKNOWN;
        private String mPlayerName;
        private String mPlayerVersion;
        private final List<Long> mExperimentIds = new ArrayList<>();
        private int mVideoFramesPlayed = -1;
        private int mVideoFramesDropped = -1;
        private int mAudioUnderrunCount = -1;
        private long mNetworkBytesRead = -1;
        private long mLocalBytesRead = -1;
        private long mNetworkTransferDurationMillis = -1;
        private byte[] mDrmSessionId = new byte[0];
        private Bundle mMetricsBundle = new Bundle();

        public Builder()
        {
        }

        public Builder addExperimentId(long experimentId)
        {
            mExperimentIds.add(experimentId);
            return this;
        }

        public PlaybackMetrics build()
        {
            return new PlaybackMetrics(this);
        }

        public Builder setAudioUnderrunCount(int audioUnderrunCount)
        {
            requireNonNegative(audioUnderrunCount);
            mAudioUnderrunCount = audioUnderrunCount;
            return this;
        }

        public Builder setContentType(int contentType)
        {
            mContentType = contentType;
            return this;
        }

        public Builder setDrmSessionId(byte[] drmSessionId)
        {
            mDrmSessionId = Objects.requireNonNull(drmSessionId);
            return this;
        }

        public Builder setDrmType(int drmType)
        {
            mDrmType = drmType;
            return this;
        }

        public Builder setLocalBytesRead(long localBytesRead)
        {
            requireNonNegative(localBytesRead);
            mLocalBytesRead = localBytesRead;
            return this;
        }

        public Builder setMediaDurationMillis(long mediaDurationMillis)
        {
            requireNonNegative(mediaDurationMillis);
            mMediaDurationMillis = mediaDurationMillis;
            return this;
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setNetworkBytesRead(long networkBytesRead)
        {
            requireNonNegative(networkBytesRead);
            mNetworkBytesRead = networkBytesRead;
            return this;
        }

        public Builder setNetworkTransferDurationMillis(long networkTransferDurationMillis)
        {
            requireNonNegative(networkTransferDurationMillis);
            mNetworkTransferDurationMillis = networkTransferDurationMillis;
            return this;
        }

        public Builder setPlaybackType(int playbackType)
        {
            mPlaybackType = playbackType;
            return this;
        }

        public Builder setPlayerName(String playerName)
        {
            mPlayerName = Objects.requireNonNull(playerName);
            return this;
        }

        public Builder setPlayerVersion(String playerVersion)
        {
            mPlayerVersion = Objects.requireNonNull(playerVersion);
            return this;
        }

        public Builder setStreamSource(int streamSource)
        {
            mStreamSource = streamSource;
            return this;
        }

        public Builder setStreamType(int streamType)
        {
            mStreamType = streamType;
            return this;
        }

        public Builder setVideoFramesDropped(int videoFramesDropped)
        {
            requireNonNegative(videoFramesDropped);
            mVideoFramesDropped = videoFramesDropped;
            return this;
        }

        public Builder setVideoFramesPlayed(int videoFramesPlayed)
        {
            requireNonNegative(videoFramesPlayed);
            mVideoFramesPlayed = videoFramesPlayed;
            return this;
        }

        private static void requireNonNegative(long value)
        {
            if (value < 0) throw new IllegalArgumentException("Negative value: " + value);
        }
    }
}

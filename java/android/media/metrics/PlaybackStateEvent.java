package android.media.metrics;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class PlaybackStateEvent extends Event implements Parcelable
{
    public static final int STATE_ABANDONED = 15;
    public static final int STATE_BUFFERING = 6;
    public static final int STATE_ENDED = 11;
    public static final int STATE_FAILED = 13;
    public static final int STATE_INTERRUPTED_BY_AD = 14;
    public static final int STATE_JOINING_BACKGROUND = 1;
    public static final int STATE_JOINING_FOREGROUND = 2;
    public static final int STATE_NOT_STARTED = 0;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PAUSED_BUFFERING = 7;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_SEEKING = 5;
    public static final int STATE_STOPPED = 12;
    public static final int STATE_SUPPRESSED = 9;
    public static final int STATE_SUPPRESSED_BUFFERING = 10;

    public static final Parcelable.Creator<PlaybackStateEvent> CREATOR = new Parcelable.Creator<PlaybackStateEvent>()
    {
        @Override
        public PlaybackStateEvent createFromParcel(Parcel source)
        {
            int state = source.readInt();
            long timeSinceCreatedMillis = source.readLong();
            Bundle metricsBundle = source.readBundle();
            return new PlaybackStateEvent(state, timeSinceCreatedMillis, metricsBundle);
        }

        @Override
        public PlaybackStateEvent[] newArray(int size)
        {
            return new PlaybackStateEvent[size];
        }
    };

    private final int mState;

    PlaybackStateEvent(int state, long timeSinceCreatedMillis, Bundle metricsBundle)
    {
        super(timeSinceCreatedMillis, metricsBundle);
        mState = state;
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
        if (!(object instanceof PlaybackStateEvent)) return false;

        PlaybackStateEvent other = (PlaybackStateEvent)object;
        return mState == other.mState && getTimeSinceCreatedMillis() == other.getTimeSinceCreatedMillis();
    }

    @Override
    public Bundle getMetricsBundle()
    {
        return super.getMetricsBundle();
    }

    public int getState()
    {
        return mState;
    }

    @Override
    public long getTimeSinceCreatedMillis()
    {
        return super.getTimeSinceCreatedMillis();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mState, getTimeSinceCreatedMillis());
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mState);
        destination.writeLong(getTimeSinceCreatedMillis());
        destination.writeBundle(getMetricsBundle());
    }

    public static final class Builder
    {
        private int mState = STATE_NOT_STARTED;
        private long mTimeSinceCreatedMillis = -1;
        private Bundle mMetricsBundle = new Bundle();

        public Builder()
        {
        }

        public PlaybackStateEvent build()
        {
            return new PlaybackStateEvent(mState, mTimeSinceCreatedMillis, mMetricsBundle);
        }

        public Builder setMetricsBundle(Bundle metricsBundle)
        {
            mMetricsBundle = metricsBundle;
            return this;
        }

        public Builder setState(int state)
        {
            mState = state;
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

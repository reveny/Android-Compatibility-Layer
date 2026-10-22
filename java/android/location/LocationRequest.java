package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class LocationRequest implements Parcelable
{
    public static final long PASSIVE_INTERVAL = Long.MAX_VALUE;
    public static final int QUALITY_HIGH_ACCURACY = 100;
    public static final int QUALITY_BALANCED_POWER_ACCURACY = 102;
    public static final int QUALITY_LOW_POWER = 104;

    private static final long IMPLICIT_MIN_UPDATE_INTERVAL = -1;
    private static final double IMPLICIT_MIN_UPDATE_INTERVAL_FACTOR = 1D / 6D;

    private final int mQuality;
    private final long mIntervalMillis;
    private final long mDurationMillis;
    private final int mMaxUpdates;
    private final long mMinUpdateIntervalMillis;
    private final float mMinUpdateDistanceMeters;
    private final long mMaxUpdateDelayMillis;

    private LocationRequest(int quality, long intervalMillis, long durationMillis, int maxUpdates, long minUpdateIntervalMillis,
                            float minUpdateDistanceMeters, long maxUpdateDelayMillis)
    {
        mQuality = quality;
        mIntervalMillis = intervalMillis;
        mDurationMillis = durationMillis;
        mMaxUpdates = maxUpdates;
        mMinUpdateIntervalMillis = minUpdateIntervalMillis;
        mMinUpdateDistanceMeters = minUpdateDistanceMeters;
        mMaxUpdateDelayMillis = maxUpdateDelayMillis;
    }

    public int getQuality()
    {
        return mQuality;
    }

    public long getIntervalMillis()
    {
        return mIntervalMillis;
    }

    public long getDurationMillis()
    {
        return mDurationMillis;
    }

    public int getMaxUpdates()
    {
        return mMaxUpdates;
    }

    public long getMinUpdateIntervalMillis()
    {
        if (mMinUpdateIntervalMillis == IMPLICIT_MIN_UPDATE_INTERVAL)
        {
            return (long)(mIntervalMillis * IMPLICIT_MIN_UPDATE_INTERVAL_FACTOR);
        }

        return Math.min(mMinUpdateIntervalMillis, mIntervalMillis);
    }

    public float getMinUpdateDistanceMeters()
    {
        return mMinUpdateDistanceMeters;
    }

    public long getMaxUpdateDelayMillis()
    {
        return mMaxUpdateDelayMillis;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mQuality);
        parcel.writeLong(mIntervalMillis);
        parcel.writeLong(mDurationMillis);
        parcel.writeInt(mMaxUpdates);
        parcel.writeLong(mMinUpdateIntervalMillis);
        parcel.writeFloat(mMinUpdateDistanceMeters);
        parcel.writeLong(mMaxUpdateDelayMillis);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LocationRequest)) return false;

        LocationRequest request = (LocationRequest)other;
        return mQuality == request.mQuality && mIntervalMillis == request.mIntervalMillis && mDurationMillis == request.mDurationMillis
            && mMaxUpdates == request.mMaxUpdates && mMinUpdateIntervalMillis == request.mMinUpdateIntervalMillis
            && Float.compare(request.mMinUpdateDistanceMeters, mMinUpdateDistanceMeters) == 0
            && mMaxUpdateDelayMillis == request.mMaxUpdateDelayMillis;
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mQuality, mIntervalMillis, mMinUpdateIntervalMillis);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("Request[");
        builder.append(mIntervalMillis == PASSIVE_INTERVAL ? "PASSIVE" : "@" + mIntervalMillis + "ms");
        builder.append(" quality=").append(mQuality);
        if (mDurationMillis != Long.MAX_VALUE) builder.append(", duration=").append(mDurationMillis).append("ms");
        if (mMaxUpdates != Integer.MAX_VALUE) builder.append(", maxUpdates=").append(mMaxUpdates);
        if (mMinUpdateDistanceMeters > 0) builder.append(", minUpdateDistance=").append(mMinUpdateDistanceMeters);
        builder.append(']');
        return builder.toString();
    }

    public static final Creator<LocationRequest> CREATOR = new Creator<LocationRequest>()
    {
        @Override
        public LocationRequest createFromParcel(Parcel parcel)
        {
            return new LocationRequest(parcel.readInt(), parcel.readLong(), parcel.readLong(), parcel.readInt(), parcel.readLong(),
                                       parcel.readFloat(), parcel.readLong());
        }

        @Override
        public LocationRequest[] newArray(int size)
        {
            return new LocationRequest[size];
        }
    };

    public static final class Builder
    {
        private long mIntervalMillis;
        private int mQuality;
        private long mDurationMillis;
        private int mMaxUpdates;
        private long mMinUpdateIntervalMillis;
        private float mMinUpdateDistanceMeters;
        private long mMaxUpdateDelayMillis;

        public Builder(long intervalMillis)
        {
            setIntervalMillis(intervalMillis);
            mQuality = QUALITY_BALANCED_POWER_ACCURACY;
            mDurationMillis = Long.MAX_VALUE;
            mMaxUpdates = Integer.MAX_VALUE;
            mMinUpdateIntervalMillis = IMPLICIT_MIN_UPDATE_INTERVAL;
            mMinUpdateDistanceMeters = 0;
            mMaxUpdateDelayMillis = 0;
        }

        public Builder(LocationRequest request)
        {
            mIntervalMillis = request.mIntervalMillis;
            mQuality = request.mQuality;
            mDurationMillis = request.mDurationMillis;
            mMaxUpdates = request.mMaxUpdates;
            mMinUpdateIntervalMillis = request.mMinUpdateIntervalMillis;
            mMinUpdateDistanceMeters = request.mMinUpdateDistanceMeters;
            mMaxUpdateDelayMillis = request.mMaxUpdateDelayMillis;
        }

        public Builder setIntervalMillis(long intervalMillis)
        {
            if (intervalMillis < 0) throw new IllegalArgumentException("intervalMillis must be greater than or equal to 0");

            mIntervalMillis = intervalMillis;
            return this;
        }

        public Builder setQuality(int quality)
        {
            if (quality != QUALITY_HIGH_ACCURACY && quality != QUALITY_BALANCED_POWER_ACCURACY && quality != QUALITY_LOW_POWER)
            {
                throw new IllegalArgumentException("quality must be a defined QUALITY constant, not " + quality);
            }

            mQuality = quality;
            return this;
        }

        public Builder setDurationMillis(long durationMillis)
        {
            if (durationMillis <= 0) throw new IllegalArgumentException("durationMillis must be greater than 0");

            mDurationMillis = durationMillis;
            return this;
        }

        public Builder setMaxUpdates(int maxUpdates)
        {
            if (maxUpdates <= 0) throw new IllegalArgumentException("maxUpdates must be greater than 0");

            mMaxUpdates = maxUpdates;
            return this;
        }

        public Builder setMinUpdateIntervalMillis(long minUpdateIntervalMillis)
        {
            if (minUpdateIntervalMillis < 0) throw new IllegalArgumentException("minUpdateIntervalMillis must be at least 0");

            mMinUpdateIntervalMillis = minUpdateIntervalMillis;
            return this;
        }

        public Builder clearMinUpdateIntervalMillis()
        {
            mMinUpdateIntervalMillis = IMPLICIT_MIN_UPDATE_INTERVAL;
            return this;
        }

        public Builder setMinUpdateDistanceMeters(float minUpdateDistanceMeters)
        {
            if (minUpdateDistanceMeters < 0) throw new IllegalArgumentException("minUpdateDistanceMeters must be at least 0");

            mMinUpdateDistanceMeters = minUpdateDistanceMeters;
            return this;
        }

        public Builder setMaxUpdateDelayMillis(long maxUpdateDelayMillis)
        {
            if (maxUpdateDelayMillis < 0) throw new IllegalArgumentException("maxUpdateDelayMillis must be greater than or equal to 0");

            mMaxUpdateDelayMillis = maxUpdateDelayMillis;
            return this;
        }

        public LocationRequest build()
        {
            if (mIntervalMillis == PASSIVE_INTERVAL && mMinUpdateIntervalMillis == IMPLICIT_MIN_UPDATE_INTERVAL)
            {
                throw new IllegalStateException("passive location requests must have an explicit minimum update interval");
            }

            return new LocationRequest(mQuality, mIntervalMillis, mDurationMillis, mMaxUpdates, mMinUpdateIntervalMillis,
                                       mMinUpdateDistanceMeters, mMaxUpdateDelayMillis);
        }
    }
}

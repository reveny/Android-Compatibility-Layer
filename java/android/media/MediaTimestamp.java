package android.media;

public final class MediaTimestamp
{
    public static final MediaTimestamp TIMESTAMP_UNKNOWN = new MediaTimestamp(-1, -1, 0.0f);

    private final long mMediaTimeUs;
    private final long mNanoTime;
    private final float mClockRate;

    public MediaTimestamp(long mediaTimeUs, long nanoTime, float clockRate)
    {
        mMediaTimeUs = mediaTimeUs;
        mNanoTime = nanoTime;
        mClockRate = clockRate;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof MediaTimestamp)) return false;

        MediaTimestamp other = (MediaTimestamp)object;
        return mMediaTimeUs == other.mMediaTimeUs && mNanoTime == other.mNanoTime && mClockRate == other.mClockRate;
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mMediaTimeUs, mNanoTime, mClockRate);
    }

    public long getAnchorMediaTimeUs()
    {
        return mMediaTimeUs;
    }

    public long getAnchorSystemNanoTime()
    {
        return mNanoTime;
    }

    public long getAnchorSytemNanoTime()
    {
        return getAnchorSystemNanoTime();
    }

    public float getMediaClockRate()
    {
        return mClockRate;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "{AnchorMediaTimeUs=" + mMediaTimeUs + " AnchorSystemNanoTime=" + mNanoTime
            + " clockRate=" + mClockRate + "}";
    }
}

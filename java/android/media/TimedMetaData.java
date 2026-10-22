package android.media;

public final class TimedMetaData
{
    private final long mTimestampUs;
    private final byte[] mMetaData;

    public TimedMetaData(long timestampUs, byte[] metaData)
    {
        if (metaData == null) throw new IllegalArgumentException("null metaData is not allowed");
        mTimestampUs = timestampUs;
        mMetaData = metaData;
    }

    public byte[] getMetaData()
    {
        return mMetaData;
    }

    public long getTimestamp()
    {
        return mTimestampUs;
    }
}

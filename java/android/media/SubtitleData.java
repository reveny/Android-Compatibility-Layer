package android.media;

public final class SubtitleData
{
    private final int mTrackIndex;
    private final long mStartTimeUs;
    private final long mDurationUs;
    private final byte[] mData;

    public SubtitleData(int trackIndex, long startTimeUs, long durationUs, byte[] data)
    {
        if (data == null) throw new IllegalArgumentException("null data is not allowed");
        mTrackIndex = trackIndex;
        mStartTimeUs = startTimeUs;
        mDurationUs = durationUs;
        mData = data;
    }

    public byte[] getData()
    {
        return mData;
    }

    public long getDurationUs()
    {
        return mDurationUs;
    }

    public long getStartTimeUs()
    {
        return mStartTimeUs;
    }

    public int getTrackIndex()
    {
        return mTrackIndex;
    }
}

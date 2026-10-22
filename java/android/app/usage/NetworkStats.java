package android.app.usage;

public final class NetworkStats implements AutoCloseable
{
    private boolean mClosed;

    NetworkStats()
    {
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public boolean getNextBucket(Bucket bucketOut)
    {
        return false;
    }

    public boolean hasNextBucket()
    {
        return false;
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    public static class Bucket
    {
        public static final int STATE_ALL = -1;
        public static final int STATE_DEFAULT = 1;
        public static final int STATE_FOREGROUND = 2;

        public static final int UID_ALL = -1;
        public static final int UID_REMOVED = -4;
        public static final int UID_TETHERING = -5;

        public static final int TAG_NONE = 0;

        public static final int METERED_ALL = -1;
        public static final int METERED_NO = 1;
        public static final int METERED_YES = 2;

        public static final int ROAMING_ALL = -1;
        public static final int ROAMING_NO = 1;
        public static final int ROAMING_YES = 2;

        public static final int DEFAULT_NETWORK_ALL = -1;
        public static final int DEFAULT_NETWORK_NO = 1;
        public static final int DEFAULT_NETWORK_YES = 2;

        private int mState = STATE_ALL;
        private int mUid = UID_ALL;
        private int mTag = TAG_NONE;
        private int mMetered = METERED_ALL;
        private int mRoaming = ROAMING_ALL;
        private int mDefaultNetworkStatus = DEFAULT_NETWORK_ALL;
        private long mBeginTimeStamp;
        private long mEndTimeStamp;
        private long mRxBytes;
        private long mTxBytes;
        private long mRxPackets;
        private long mTxPackets;

        public Bucket()
        {
        }

        static Bucket empty(int uid, long startTime, long endTime)
        {
            Bucket bucket = new Bucket();
            bucket.mUid = uid;
            bucket.mBeginTimeStamp = startTime;
            bucket.mEndTimeStamp = endTime;
            return bucket;
        }

        public int getUid()
        {
            return mUid;
        }

        public int getTag()
        {
            return mTag;
        }

        public int getState()
        {
            return mState;
        }

        public int getMetered()
        {
            return mMetered;
        }

        public int getRoaming()
        {
            return mRoaming;
        }

        public int getDefaultNetworkStatus()
        {
            return mDefaultNetworkStatus;
        }

        public long getStartTimeStamp()
        {
            return mBeginTimeStamp;
        }

        public long getEndTimeStamp()
        {
            return mEndTimeStamp;
        }

        public long getRxBytes()
        {
            return mRxBytes;
        }

        public long getTxBytes()
        {
            return mTxBytes;
        }

        public long getRxPackets()
        {
            return mRxPackets;
        }

        public long getTxPackets()
        {
            return mTxPackets;
        }
    }
}

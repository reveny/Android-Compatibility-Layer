package android.os;

import java.io.Closeable;

public final class PerformanceHintManager
{
    private static final long PREFERRED_UPDATE_RATE_NANOS = 16666666L;

    public PerformanceHintManager(android.content.Context context)
    {
    }

    public Session createHintSession(int[] tids, long initialTargetWorkDurationNanos)
    {
        if (tids == null) throw new NullPointerException("tids cannot be null");
        if (tids.length == 0) throw new IllegalArgumentException("thread id list can't be empty.");
        if (initialTargetWorkDurationNanos <= 0) throw new IllegalArgumentException("the hint target duration should be positive.");

        return new Session(tids, initialTargetWorkDurationNanos);
    }

    public long getPreferredUpdateRateNanos()
    {
        return PREFERRED_UPDATE_RATE_NANOS;
    }

    public static class Session implements Closeable
    {
        private int[] mThreads;
        private long mTargetDurationNanos;
        private long mLastActualDurationNanos;
        private boolean mClosed;

        Session(int[] threads, long targetDurationNanos)
        {
            mThreads = threads.clone();
            mTargetDurationNanos = targetDurationNanos;
        }

        public void updateTargetWorkDuration(long targetDurationNanos)
        {
            if (targetDurationNanos <= 0) throw new IllegalArgumentException("the hint target duration should be positive.");
            mTargetDurationNanos = targetDurationNanos;
        }

        public void reportActualWorkDuration(long actualDurationNanos)
        {
            if (actualDurationNanos <= 0) throw new IllegalArgumentException("the actual duration should be positive.");
            mLastActualDurationNanos = actualDurationNanos;
        }

        public void close()
        {
            mClosed = true;
        }

        public void setThreads(int[] tids)
        {
            if (tids == null) throw new NullPointerException("tids cannot be null");
            if (tids.length == 0) throw new IllegalArgumentException("Thread id list can't be empty.");
            mThreads = tids.clone();
        }
    }
}

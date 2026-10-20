package android.app.job;

import android.content.ComponentName;

public class JobInfo
{
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_ANY = 1;
    public static final int NETWORK_TYPE_UNMETERED = 2;
    public static final int NETWORK_TYPE_NOT_ROAMING = 3;
    public static final int NETWORK_TYPE_CELLULAR = 4;

    private final int mId;
    private final ComponentName mService;

    private long mMinimumLatencyMillis;
    private long mOverrideDeadlineMillis = -1;
    private long mIntervalMillis;
    private boolean mPeriodic;
    private boolean mPersisted;
    private int mNetworkType = NETWORK_TYPE_NONE;
    private boolean mRequiresCharging;
    private boolean mRequiresDeviceIdle;
    private android.os.Bundle mExtras = new android.os.Bundle();

    private JobInfo(int id, ComponentName service)
    {
        mId = id;
        mService = service;
    }

    public int getId()
    {
        return mId;
    }

    public ComponentName getService()
    {
        return mService;
    }

    public long getMinLatencyMillis()
    {
        return mMinimumLatencyMillis;
    }

    public long getMaxExecutionDelayMillis()
    {
        return mOverrideDeadlineMillis;
    }

    public long getIntervalMillis()
    {
        return mIntervalMillis;
    }

    public boolean isPeriodic()
    {
        return mPeriodic;
    }

    public boolean isPersisted()
    {
        return mPersisted;
    }

    public int getNetworkType()
    {
        return mNetworkType;
    }

    public boolean isRequireCharging()
    {
        return mRequiresCharging;
    }

    public boolean isRequireDeviceIdle()
    {
        return mRequiresDeviceIdle;
    }

    public android.os.Bundle getTransientExtras()
    {
        return mExtras;
    }

    public static final class Builder
    {
        private final JobInfo mJob;

        public Builder(int jobId, ComponentName jobService)
        {
            mJob = new JobInfo(jobId, jobService);
        }

        public Builder setMinimumLatency(long minLatencyMillis)
        {
            mJob.mMinimumLatencyMillis = minLatencyMillis;
            return this;
        }

        public Builder setOverrideDeadline(long maxExecutionDelayMillis)
        {
            mJob.mOverrideDeadlineMillis = maxExecutionDelayMillis;
            return this;
        }

        public Builder setPeriodic(long intervalMillis)
        {
            mJob.mPeriodic = true;
            mJob.mIntervalMillis = intervalMillis;
            return this;
        }

        public Builder setRequiredNetworkType(int networkType)
        {
            mJob.mNetworkType = networkType;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging)
        {
            mJob.mRequiresCharging = requiresCharging;
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle)
        {
            mJob.mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Builder setPersisted(boolean persisted)
        {
            mJob.mPersisted = persisted;
            return this;
        }

        public Builder setTransientExtras(android.os.Bundle extras)
        {
            if (extras != null) mJob.mExtras = extras;
            return this;
        }

        public JobInfo build()
        {
            return mJob;
        }
    }
}

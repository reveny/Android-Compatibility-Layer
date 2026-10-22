package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class UsageStats implements Parcelable
{
    public static final Parcelable.Creator<UsageStats> CREATOR = new Parcelable.Creator<UsageStats>()
    {
        @Override
        public UsageStats createFromParcel(Parcel source)
        {
            UsageStats stats = new UsageStats();
            stats.mPackageName = source.readString();
            stats.mBeginTimeStamp = source.readLong();
            stats.mEndTimeStamp = source.readLong();
            stats.mLastTimeUsed = source.readLong();
            stats.mLastTimeVisible = source.readLong();
            stats.mLastTimeForegroundServiceUsed = source.readLong();
            stats.mTotalTimeInForeground = source.readLong();
            stats.mTotalTimeVisible = source.readLong();
            stats.mTotalTimeForegroundServiceUsed = source.readLong();
            return stats;
        }

        @Override
        public UsageStats[] newArray(int size)
        {
            return new UsageStats[size];
        }
    };

    private String mPackageName;
    private long mBeginTimeStamp;
    private long mEndTimeStamp;
    private long mLastTimeUsed;
    private long mLastTimeVisible;
    private long mLastTimeForegroundServiceUsed;
    private long mTotalTimeInForeground;
    private long mTotalTimeVisible;
    private long mTotalTimeForegroundServiceUsed;

    UsageStats()
    {
    }

    public UsageStats(UsageStats stats)
    {
        mPackageName = stats.mPackageName;
        mBeginTimeStamp = stats.mBeginTimeStamp;
        mEndTimeStamp = stats.mEndTimeStamp;
        mLastTimeUsed = stats.mLastTimeUsed;
        mLastTimeVisible = stats.mLastTimeVisible;
        mLastTimeForegroundServiceUsed = stats.mLastTimeForegroundServiceUsed;
        mTotalTimeInForeground = stats.mTotalTimeInForeground;
        mTotalTimeVisible = stats.mTotalTimeVisible;
        mTotalTimeForegroundServiceUsed = stats.mTotalTimeForegroundServiceUsed;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public long getFirstTimeStamp()
    {
        return mBeginTimeStamp;
    }

    public long getLastTimeStamp()
    {
        return mEndTimeStamp;
    }

    public long getLastTimeUsed()
    {
        return mLastTimeUsed;
    }

    public long getLastTimeVisible()
    {
        return mLastTimeVisible;
    }

    public long getTotalTimeInForeground()
    {
        return mTotalTimeInForeground;
    }

    public long getTotalTimeVisible()
    {
        return mTotalTimeVisible;
    }

    public long getLastTimeForegroundServiceUsed()
    {
        return mLastTimeForegroundServiceUsed;
    }

    public long getTotalTimeForegroundServiceUsed()
    {
        return mTotalTimeForegroundServiceUsed;
    }

    public void add(UsageStats right)
    {
        if (mPackageName == null || !mPackageName.equals(right.mPackageName))
        {
            throw new IllegalArgumentException("Can't merge UsageStats for package '" + mPackageName + "' with UsageStats for package '"
                                               + right.mPackageName + "'.");
        }

        if (right.mBeginTimeStamp > mBeginTimeStamp)
        {
            mLastTimeUsed = Math.max(mLastTimeUsed, right.mLastTimeUsed);
            mLastTimeVisible = Math.max(mLastTimeVisible, right.mLastTimeVisible);
            mLastTimeForegroundServiceUsed = Math.max(mLastTimeForegroundServiceUsed, right.mLastTimeForegroundServiceUsed);
        }

        mBeginTimeStamp = Math.min(mBeginTimeStamp, right.mBeginTimeStamp);
        mEndTimeStamp = Math.max(mEndTimeStamp, right.mEndTimeStamp);
        mTotalTimeInForeground += right.mTotalTimeInForeground;
        mTotalTimeVisible += right.mTotalTimeVisible;
        mTotalTimeForegroundServiceUsed += right.mTotalTimeForegroundServiceUsed;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mPackageName);
        dest.writeLong(mBeginTimeStamp);
        dest.writeLong(mEndTimeStamp);
        dest.writeLong(mLastTimeUsed);
        dest.writeLong(mLastTimeVisible);
        dest.writeLong(mLastTimeForegroundServiceUsed);
        dest.writeLong(mTotalTimeInForeground);
        dest.writeLong(mTotalTimeVisible);
        dest.writeLong(mTotalTimeForegroundServiceUsed);
    }
}

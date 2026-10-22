package android.app.usage;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;

public final class ConfigurationStats implements Parcelable
{
    public static final Parcelable.Creator<ConfigurationStats> CREATOR = new Parcelable.Creator<ConfigurationStats>()
    {
        @Override
        public ConfigurationStats createFromParcel(Parcel source)
        {
            ConfigurationStats stats = new ConfigurationStats();
            stats.mBeginTimeStamp = source.readLong();
            stats.mEndTimeStamp = source.readLong();
            stats.mLastTimeActive = source.readLong();
            stats.mTotalTimeActive = source.readLong();
            stats.mActivationCount = source.readInt();
            return stats;
        }

        @Override
        public ConfigurationStats[] newArray(int size)
        {
            return new ConfigurationStats[size];
        }
    };

    private Configuration mConfiguration;
    private long mBeginTimeStamp;
    private long mEndTimeStamp;
    private long mLastTimeActive;
    private long mTotalTimeActive;
    private int mActivationCount;

    ConfigurationStats()
    {
    }

    public ConfigurationStats(ConfigurationStats stats)
    {
        mConfiguration = stats.mConfiguration;
        mBeginTimeStamp = stats.mBeginTimeStamp;
        mEndTimeStamp = stats.mEndTimeStamp;
        mLastTimeActive = stats.mLastTimeActive;
        mTotalTimeActive = stats.mTotalTimeActive;
        mActivationCount = stats.mActivationCount;
    }

    public Configuration getConfiguration()
    {
        return mConfiguration;
    }

    public long getFirstTimeStamp()
    {
        return mBeginTimeStamp;
    }

    public long getLastTimeStamp()
    {
        return mEndTimeStamp;
    }

    public long getLastTimeActive()
    {
        return mLastTimeActive;
    }

    public long getTotalTimeActive()
    {
        return mTotalTimeActive;
    }

    public int getActivationCount()
    {
        return mActivationCount;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mBeginTimeStamp);
        dest.writeLong(mEndTimeStamp);
        dest.writeLong(mLastTimeActive);
        dest.writeLong(mTotalTimeActive);
        dest.writeInt(mActivationCount);
    }
}

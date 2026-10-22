package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class EventStats implements Parcelable
{
    public static final Parcelable.Creator<EventStats> CREATOR = new Parcelable.Creator<EventStats>()
    {
        @Override
        public EventStats createFromParcel(Parcel source)
        {
            EventStats stats = new EventStats();
            stats.mEventType = source.readInt();
            stats.mBeginTimeStamp = source.readLong();
            stats.mEndTimeStamp = source.readLong();
            stats.mLastEventTime = source.readLong();
            stats.mTotalTime = source.readLong();
            stats.mCount = source.readInt();
            return stats;
        }

        @Override
        public EventStats[] newArray(int size)
        {
            return new EventStats[size];
        }
    };

    private int mEventType;
    private long mBeginTimeStamp;
    private long mEndTimeStamp;
    private long mLastEventTime;
    private long mTotalTime;
    private int mCount;

    EventStats()
    {
    }

    public EventStats(EventStats stats)
    {
        mEventType = stats.mEventType;
        mBeginTimeStamp = stats.mBeginTimeStamp;
        mEndTimeStamp = stats.mEndTimeStamp;
        mLastEventTime = stats.mLastEventTime;
        mTotalTime = stats.mTotalTime;
        mCount = stats.mCount;
    }

    public int getEventType()
    {
        return mEventType;
    }

    public long getFirstTimeStamp()
    {
        return mBeginTimeStamp;
    }

    public long getLastTimeStamp()
    {
        return mEndTimeStamp;
    }

    public long getLastEventTime()
    {
        return mLastEventTime;
    }

    public int getCount()
    {
        return mCount;
    }

    public long getTotalTime()
    {
        return mTotalTime;
    }

    public void add(EventStats right)
    {
        if (mEventType != right.mEventType)
        {
            throw new IllegalArgumentException("Can't merge EventStats for event #" + mEventType + " with EventStats for event #"
                                               + right.mEventType);
        }

        if (right.mBeginTimeStamp > mBeginTimeStamp) mLastEventTime = Math.max(mLastEventTime, right.mLastEventTime);

        mBeginTimeStamp = Math.min(mBeginTimeStamp, right.mBeginTimeStamp);
        mEndTimeStamp = Math.max(mEndTimeStamp, right.mEndTimeStamp);
        mTotalTime += right.mTotalTime;
        mCount += right.mCount;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mEventType);
        dest.writeLong(mBeginTimeStamp);
        dest.writeLong(mEndTimeStamp);
        dest.writeLong(mLastEventTime);
        dest.writeLong(mTotalTime);
        dest.writeInt(mCount);
    }
}

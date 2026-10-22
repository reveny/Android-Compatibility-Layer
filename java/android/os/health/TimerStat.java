package android.os.health;

import android.os.Parcel;
import android.os.Parcelable;

public final class TimerStat implements Parcelable
{
    public static final Creator<TimerStat> CREATOR = new Creator<TimerStat>()
    {
        @Override
        public TimerStat createFromParcel(Parcel parcel)
        {
            return new TimerStat(parcel);
        }

        @Override
        public TimerStat[] newArray(int size)
        {
            return new TimerStat[size];
        }
    };

    private int mCount;
    private long mTime;

    public TimerStat()
    {
    }

    public TimerStat(int count, long time)
    {
        mCount = count;
        mTime = time;
    }

    public TimerStat(Parcel in)
    {
        mCount = in.readInt();
        mTime = in.readLong();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(mCount);
        out.writeLong(mTime);
    }

    public void setCount(int count)
    {
        mCount = count;
    }

    public int getCount()
    {
        return mCount;
    }

    public void setTime(long time)
    {
        mTime = time;
    }

    public long getTime()
    {
        return mTime;
    }
}

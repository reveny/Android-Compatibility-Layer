package android.hardware;

import android.os.Parcel;
import android.os.Parcelable;

import java.time.Duration;

public final class SyncFence implements AutoCloseable, Parcelable
{
    public static final long SIGNAL_TIME_INVALID = -1L;
    public static final long SIGNAL_TIME_PENDING = Long.MAX_VALUE;

    public static final Parcelable.Creator<SyncFence> CREATOR = new Parcelable.Creator<SyncFence>()
    {
        @Override
        public SyncFence createFromParcel(Parcel source)
        {
            boolean valid = source.readBoolean();
            long signalTime = source.readLong();
            return new SyncFence(valid, signalTime);
        }

        @Override
        public SyncFence[] newArray(int size)
        {
            return new SyncFence[size];
        }
    };

    private final Object mLock = new Object();
    private boolean mValid;
    private long mSignalTime;

    SyncFence(boolean valid, long signalTime)
    {
        mValid = valid;
        mSignalTime = signalTime;
    }

    public SyncFence(SyncFence other)
    {
        synchronized (other.mLock)
        {
            mValid = other.mValid;
            mSignalTime = other.mSignalTime;
            other.mValid = false;
        }
    }

    public boolean isValid()
    {
        synchronized (mLock)
        {
            return mValid;
        }
    }

    public boolean await(Duration timeout)
    {
        if (timeout == null) throw new NullPointerException("timeout");
        if (timeout.isNegative()) throw new IllegalArgumentException("timeout must not be negative");

        return true;
    }

    public boolean awaitForever()
    {
        return true;
    }

    public long getSignalTime()
    {
        synchronized (mLock)
        {
            return mValid ? mSignalTime : SIGNAL_TIME_INVALID;
        }
    }

    @Override
    public void close()
    {
        synchronized (mLock)
        {
            mValid = false;
        }
    }

    @Override
    public int describeContents()
    {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        synchronized (mLock)
        {
            dest.writeBoolean(mValid);
            dest.writeLong(mSignalTime);
        }
    }
}

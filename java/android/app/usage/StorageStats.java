package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class StorageStats implements Parcelable
{
    public static final Parcelable.Creator<StorageStats> CREATOR = new Parcelable.Creator<StorageStats>()
    {
        @Override
        public StorageStats createFromParcel(Parcel source)
        {
            return new StorageStats(source.readLong(), source.readLong(), source.readLong(), source.readLong());
        }

        @Override
        public StorageStats[] newArray(int size)
        {
            return new StorageStats[size];
        }
    };

    private final long mCodeBytes;
    private final long mDataBytes;
    private final long mCacheBytes;
    private final long mExternalCacheBytes;

    StorageStats(long codeBytes, long dataBytes, long cacheBytes, long externalCacheBytes)
    {
        mCodeBytes = codeBytes;
        mDataBytes = dataBytes;
        mCacheBytes = cacheBytes;
        mExternalCacheBytes = externalCacheBytes;
    }

    public long getAppBytes()
    {
        return mCodeBytes;
    }

    public long getDataBytes()
    {
        return mDataBytes;
    }

    public long getCacheBytes()
    {
        return mCacheBytes;
    }

    public long getExternalCacheBytes()
    {
        return mExternalCacheBytes;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mCodeBytes);
        dest.writeLong(mDataBytes);
        dest.writeLong(mCacheBytes);
        dest.writeLong(mExternalCacheBytes);
    }
}

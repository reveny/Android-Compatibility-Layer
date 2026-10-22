package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class ExternalStorageStats implements Parcelable
{
    public static final Parcelable.Creator<ExternalStorageStats> CREATOR = new Parcelable.Creator<ExternalStorageStats>()
    {
        @Override
        public ExternalStorageStats createFromParcel(Parcel source)
        {
            return new ExternalStorageStats(source.readLong(), source.readLong(), source.readLong(), source.readLong(), source.readLong());
        }

        @Override
        public ExternalStorageStats[] newArray(int size)
        {
            return new ExternalStorageStats[size];
        }
    };

    private final long mTotalBytes;
    private final long mAudioBytes;
    private final long mVideoBytes;
    private final long mImageBytes;
    private final long mAppBytes;

    ExternalStorageStats(long totalBytes, long audioBytes, long videoBytes, long imageBytes, long appBytes)
    {
        mTotalBytes = totalBytes;
        mAudioBytes = audioBytes;
        mVideoBytes = videoBytes;
        mImageBytes = imageBytes;
        mAppBytes = appBytes;
    }

    public long getTotalBytes()
    {
        return mTotalBytes;
    }

    public long getAudioBytes()
    {
        return mAudioBytes;
    }

    public long getVideoBytes()
    {
        return mVideoBytes;
    }

    public long getImageBytes()
    {
        return mImageBytes;
    }

    public long getAppBytes()
    {
        return mAppBytes;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mTotalBytes);
        dest.writeLong(mAudioBytes);
        dest.writeLong(mVideoBytes);
        dest.writeLong(mImageBytes);
        dest.writeLong(mAppBytes);
    }
}

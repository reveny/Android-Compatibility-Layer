package android.app.job;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

public final class JobWorkItem implements Parcelable
{
    public static final Parcelable.Creator<JobWorkItem> CREATOR = new Parcelable.Creator<JobWorkItem>()
    {
        @Override
        public JobWorkItem createFromParcel(Parcel source)
        {
            Intent intent = source.readInt() != 0 ? Intent.CREATOR.createFromParcel(source) : null;
            JobWorkItem item = new JobWorkItem(intent, source.readLong(), source.readLong(), source.readLong());
            item.mExtras = source.readPersistableBundle();
            item.mDeliveryCount = source.readInt();
            item.mWorkId = source.readInt();
            return item;
        }

        @Override
        public JobWorkItem[] newArray(int size)
        {
            return new JobWorkItem[size];
        }
    };

    private final Intent mIntent;
    private final long mNetworkDownloadBytes;
    private final long mNetworkUploadBytes;
    private final long mMinimumChunkBytes;
    private PersistableBundle mExtras = PersistableBundle.EMPTY;
    private int mDeliveryCount;
    private int mWorkId;

    public JobWorkItem(Intent intent)
    {
        this(intent, JobInfo.NETWORK_BYTES_UNKNOWN, JobInfo.NETWORK_BYTES_UNKNOWN, JobInfo.NETWORK_BYTES_UNKNOWN);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes)
    {
        this(intent, downloadBytes, uploadBytes, JobInfo.NETWORK_BYTES_UNKNOWN);
    }

    public JobWorkItem(Intent intent, long downloadBytes, long uploadBytes, long minimumChunkBytes)
    {
        mIntent = intent;
        mNetworkDownloadBytes = downloadBytes;
        mNetworkUploadBytes = uploadBytes;
        mMinimumChunkBytes = minimumChunkBytes;
    }

    public Intent getIntent()
    {
        return mIntent;
    }

    public PersistableBundle getExtras()
    {
        return mExtras;
    }

    public long getEstimatedNetworkDownloadBytes()
    {
        return mNetworkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes()
    {
        return mNetworkUploadBytes;
    }

    public long getMinimumNetworkChunkBytes()
    {
        return mMinimumChunkBytes;
    }

    public int getDeliveryCount()
    {
        return mDeliveryCount;
    }

    public void bumpDeliveryCount()
    {
        mDeliveryCount++;
    }

    public int getWorkId()
    {
        return mWorkId;
    }

    public void setWorkId(int workId)
    {
        mWorkId = workId;
    }

    @Override
    public String toString()
    {
        return "JobWorkItem{id=" + mWorkId + " intent=" + mIntent + " dcount=" + mDeliveryCount + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        if (mIntent == null)
        {
            destination.writeInt(0);
        }
        else
        {
            destination.writeInt(1);
            mIntent.writeToParcel(destination, flags);
        }
        destination.writeLong(mNetworkDownloadBytes);
        destination.writeLong(mNetworkUploadBytes);
        destination.writeLong(mMinimumChunkBytes);
        destination.writePersistableBundle(mExtras);
        destination.writeInt(mDeliveryCount);
        destination.writeInt(mWorkId);
    }

    public static final class Builder
    {
        private Intent mIntent;
        private long mDownloadBytes = JobInfo.NETWORK_BYTES_UNKNOWN;
        private long mUploadBytes = JobInfo.NETWORK_BYTES_UNKNOWN;
        private long mMinimumChunkBytes = JobInfo.NETWORK_BYTES_UNKNOWN;
        private PersistableBundle mExtras = PersistableBundle.EMPTY;

        public Builder()
        {
        }

        public Builder setIntent(Intent intent)
        {
            mIntent = intent;
            return this;
        }

        public Builder setExtras(PersistableBundle extras)
        {
            if (extras == null) throw new IllegalArgumentException("extras cannot be null");
            mExtras = extras;
            return this;
        }

        public Builder setEstimatedNetworkBytes(long downloadBytes, long uploadBytes)
        {
            mDownloadBytes = downloadBytes;
            mUploadBytes = uploadBytes;
            return this;
        }

        public Builder setMinimumNetworkChunkBytes(long chunkSizeBytes)
        {
            mMinimumChunkBytes = chunkSizeBytes;
            return this;
        }

        public JobWorkItem build()
        {
            JobWorkItem item = new JobWorkItem(mIntent, mDownloadBytes, mUploadBytes, mMinimumChunkBytes);
            item.mExtras = mExtras;
            return item;
        }
    }
}

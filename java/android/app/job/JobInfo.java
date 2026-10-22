package android.app.job;

import android.content.ClipData;
import android.content.ComponentName;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class JobInfo implements Parcelable
{
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_ANY = 1;
    public static final int NETWORK_TYPE_UNMETERED = 2;
    public static final int NETWORK_TYPE_NOT_ROAMING = 3;
    public static final int NETWORK_TYPE_CELLULAR = 4;
    public static final int NETWORK_TYPE_METERED = 4;
    public static final int NETWORK_BYTES_UNKNOWN = -1;

    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000L;
    public static final long MAX_BACKOFF_DELAY_MILLIS = 18000000L;

    public static final int BACKOFF_POLICY_LINEAR = 0;
    public static final int BACKOFF_POLICY_EXPONENTIAL = 1;

    public static final int PRIORITY_MIN = 100;
    public static final int PRIORITY_LOW = 200;
    public static final int PRIORITY_DEFAULT = 300;
    public static final int PRIORITY_HIGH = 400;
    public static final int PRIORITY_MAX = 500;

    private static final long MIN_PERIOD_MILLIS = 15L * 60L * 1000L;
    private static final long MIN_FLEX_MILLIS = 5L * 60L * 1000L;

    public static final Parcelable.Creator<JobInfo> CREATOR = new Parcelable.Creator<JobInfo>()
    {
        @Override
        public JobInfo createFromParcel(Parcel source)
        {
            JobInfo job = new JobInfo(source.readInt(), ComponentName.readFromParcel(source));
            job.mExtras = source.readPersistableBundle();
            job.mTransientExtras = source.readBundle();
            job.mMinimumLatencyMillis = source.readLong();
            job.mOverrideDeadlineMillis = source.readLong();
            job.mIntervalMillis = source.readLong();
            job.mFlexMillis = source.readLong();
            job.mPeriodic = source.readInt() != 0;
            job.mPersisted = source.readInt() != 0;
            job.mNetworkType = source.readInt();
            job.mConstraintFlags = source.readInt();
            job.mInitialBackoffMillis = source.readLong();
            job.mBackoffPolicy = source.readInt();
            job.mPriority = source.readInt();
            job.mFlags = source.readInt();
            job.mTriggerContentUpdateDelay = source.readLong();
            job.mTriggerContentMaxDelay = source.readLong();
            job.mEstimatedDownloadBytes = source.readLong();
            job.mEstimatedUploadBytes = source.readLong();
            job.mMinimumChunkBytes = source.readLong();
            job.mTraceTag = source.readString();
            int count = source.readInt();
            if (count >= 0)
            {
                job.mTriggerContentUris = new TriggerContentUri[count];
                for (int index = 0; index < count; index++)
                {
                    job.mTriggerContentUris[index] = TriggerContentUri.CREATOR.createFromParcel(source);
                }
            }
            if (source.readInt() != 0)
            {
                job.mClipData = ClipData.CREATOR.createFromParcel(source);
                job.mClipGrantFlags = source.readInt();
            }
            return job;
        }

        @Override
        public JobInfo[] newArray(int size)
        {
            return new JobInfo[size];
        }
    };

    private static final int CONSTRAINT_CHARGING = 1;
    private static final int CONSTRAINT_IDLE = 1 << 1;
    private static final int CONSTRAINT_BATTERY_NOT_LOW = 1 << 2;
    private static final int CONSTRAINT_STORAGE_NOT_LOW = 1 << 3;

    private static final int FLAG_IMPORTANT_WHILE_FOREGROUND = 1;
    private static final int FLAG_PREFETCH = 1 << 1;
    private static final int FLAG_EXPEDITED = 1 << 2;
    private static final int FLAG_USER_INITIATED = 1 << 3;

    private final int mId;
    private final ComponentName mService;

    private PersistableBundle mExtras = PersistableBundle.EMPTY;
    private Bundle mTransientExtras = Bundle.EMPTY;
    private ClipData mClipData;
    private int mClipGrantFlags;
    private long mMinimumLatencyMillis;
    private long mOverrideDeadlineMillis = -1;
    private long mIntervalMillis;
    private long mFlexMillis;
    private boolean mPeriodic;
    private boolean mPersisted;
    private int mNetworkType = NETWORK_TYPE_NONE;
    private NetworkRequest mNetworkRequest;
    private int mConstraintFlags;
    private long mInitialBackoffMillis = DEFAULT_INITIAL_BACKOFF_MILLIS;
    private int mBackoffPolicy = BACKOFF_POLICY_EXPONENTIAL;
    private int mPriority = PRIORITY_DEFAULT;
    private int mFlags;
    private TriggerContentUri[] mTriggerContentUris;
    private long mTriggerContentUpdateDelay = -1;
    private long mTriggerContentMaxDelay = -1;
    private long mEstimatedDownloadBytes = NETWORK_BYTES_UNKNOWN;
    private long mEstimatedUploadBytes = NETWORK_BYTES_UNKNOWN;
    private long mMinimumChunkBytes = NETWORK_BYTES_UNKNOWN;
    private String mTraceTag;

    JobInfo(int id, ComponentName service)
    {
        mId = id;
        mService = service;
    }

    public static final long getMinPeriodMillis()
    {
        return MIN_PERIOD_MILLIS;
    }

    public static final long getMinFlexMillis()
    {
        return MIN_FLEX_MILLIS;
    }

    public int getId()
    {
        return mId;
    }

    public PersistableBundle getExtras()
    {
        return mExtras;
    }

    public Bundle getTransientExtras()
    {
        return mTransientExtras;
    }

    public ClipData getClipData()
    {
        return mClipData;
    }

    public int getClipGrantFlags()
    {
        return mClipGrantFlags;
    }

    public ComponentName getService()
    {
        return mService;
    }

    public int getPriority()
    {
        return mPriority;
    }

    public boolean isRequireCharging()
    {
        return (mConstraintFlags & CONSTRAINT_CHARGING) != 0;
    }

    public boolean isRequireBatteryNotLow()
    {
        return (mConstraintFlags & CONSTRAINT_BATTERY_NOT_LOW) != 0;
    }

    public boolean isRequireDeviceIdle()
    {
        return (mConstraintFlags & CONSTRAINT_IDLE) != 0;
    }

    public boolean isRequireStorageNotLow()
    {
        return (mConstraintFlags & CONSTRAINT_STORAGE_NOT_LOW) != 0;
    }

    public TriggerContentUri[] getTriggerContentUris()
    {
        return mTriggerContentUris;
    }

    public long getTriggerContentUpdateDelay()
    {
        return mTriggerContentUpdateDelay;
    }

    public long getTriggerContentMaxDelay()
    {
        return mTriggerContentMaxDelay;
    }

    public int getNetworkType()
    {
        return mNetworkType;
    }

    public NetworkRequest getRequiredNetwork()
    {
        return mNetworkRequest;
    }

    public long getEstimatedNetworkDownloadBytes()
    {
        return mEstimatedDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes()
    {
        return mEstimatedUploadBytes;
    }

    public long getMinimumNetworkChunkBytes()
    {
        return mMinimumChunkBytes;
    }

    public long getMinLatencyMillis()
    {
        return mMinimumLatencyMillis;
    }

    public long getMaxExecutionDelayMillis()
    {
        return mOverrideDeadlineMillis;
    }

    public boolean isPeriodic()
    {
        return mPeriodic;
    }

    public boolean isPersisted()
    {
        return mPersisted;
    }

    public long getIntervalMillis()
    {
        return mIntervalMillis;
    }

    public long getFlexMillis()
    {
        return mFlexMillis;
    }

    public long getInitialBackoffMillis()
    {
        return mInitialBackoffMillis;
    }

    public int getBackoffPolicy()
    {
        return mBackoffPolicy;
    }

    public boolean isExpedited()
    {
        return (mFlags & FLAG_EXPEDITED) != 0;
    }

    public boolean isUserInitiated()
    {
        return (mFlags & FLAG_USER_INITIATED) != 0;
    }

    public boolean isImportantWhileForeground()
    {
        return (mFlags & FLAG_IMPORTANT_WHILE_FOREGROUND) != 0;
    }

    public boolean isPrefetch()
    {
        return (mFlags & FLAG_PREFETCH) != 0;
    }

    public String getTraceTag()
    {
        return mTraceTag;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof JobInfo)) return false;

        JobInfo job = (JobInfo)other;
        return mId == job.mId && Objects.equals(mService, job.mService) && mMinimumLatencyMillis == job.mMinimumLatencyMillis
               && mOverrideDeadlineMillis == job.mOverrideDeadlineMillis && mIntervalMillis == job.mIntervalMillis
               && mFlexMillis == job.mFlexMillis && mPeriodic == job.mPeriodic && mPersisted == job.mPersisted
               && mNetworkType == job.mNetworkType && mConstraintFlags == job.mConstraintFlags
               && mInitialBackoffMillis == job.mInitialBackoffMillis && mBackoffPolicy == job.mBackoffPolicy
               && mPriority == job.mPriority && mFlags == job.mFlags && Arrays.equals(mTriggerContentUris, job.mTriggerContentUris)
               && mTriggerContentUpdateDelay == job.mTriggerContentUpdateDelay
               && mTriggerContentMaxDelay == job.mTriggerContentMaxDelay && Objects.equals(mTraceTag, job.mTraceTag);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mId, mService, mMinimumLatencyMillis, mOverrideDeadlineMillis, mIntervalMillis, mPeriodic, mNetworkType,
                            mConstraintFlags, mFlags, mPriority);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mId);
        ComponentName.writeToParcel(mService, destination);
        destination.writePersistableBundle(mExtras);
        destination.writeBundle(mTransientExtras);
        destination.writeLong(mMinimumLatencyMillis);
        destination.writeLong(mOverrideDeadlineMillis);
        destination.writeLong(mIntervalMillis);
        destination.writeLong(mFlexMillis);
        destination.writeInt(mPeriodic ? 1 : 0);
        destination.writeInt(mPersisted ? 1 : 0);
        destination.writeInt(mNetworkType);
        destination.writeInt(mConstraintFlags);
        destination.writeLong(mInitialBackoffMillis);
        destination.writeInt(mBackoffPolicy);
        destination.writeInt(mPriority);
        destination.writeInt(mFlags);
        destination.writeLong(mTriggerContentUpdateDelay);
        destination.writeLong(mTriggerContentMaxDelay);
        destination.writeLong(mEstimatedDownloadBytes);
        destination.writeLong(mEstimatedUploadBytes);
        destination.writeLong(mMinimumChunkBytes);
        destination.writeString(mTraceTag);
        if (mTriggerContentUris == null)
        {
            destination.writeInt(-1);
        }
        else
        {
            destination.writeInt(mTriggerContentUris.length);
            for (TriggerContentUri uri : mTriggerContentUris) uri.writeToParcel(destination, flags);
        }
        if (mClipData == null)
        {
            destination.writeInt(0);
        }
        else
        {
            destination.writeInt(1);
            mClipData.writeToParcel(destination, flags);
            destination.writeInt(mClipGrantFlags);
        }
    }

    @Override
    public String toString()
    {
        return "(job:" + mId + "/" + (mService == null ? "null" : mService.flattenToShortString()) + ")";
    }

    public static final class TriggerContentUri implements Parcelable
    {
        public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1;

        public static final Parcelable.Creator<TriggerContentUri> CREATOR = new Parcelable.Creator<TriggerContentUri>()
        {
            @Override
            public TriggerContentUri createFromParcel(Parcel source)
            {
                Uri uri = Uri.CREATOR.createFromParcel(source);
                return new TriggerContentUri(uri, source.readInt());
            }

            @Override
            public TriggerContentUri[] newArray(int size)
            {
                return new TriggerContentUri[size];
            }
        };

        private final Uri mUri;
        private final int mFlags;

        public TriggerContentUri(Uri uri, int flags)
        {
            mUri = Objects.requireNonNull(uri);
            mFlags = flags;
        }

        public Uri getUri()
        {
            return mUri;
        }

        public int getFlags()
        {
            return mFlags;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof TriggerContentUri)) return false;

            TriggerContentUri uri = (TriggerContentUri)other;
            return mUri.equals(uri.mUri) && mFlags == uri.mFlags;
        }

        @Override
        public int hashCode()
        {
            return mUri.hashCode() ^ mFlags;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags)
        {
            mUri.writeToParcel(destination, flags);
            destination.writeInt(mFlags);
        }
    }

    public static final class Builder
    {
        private final JobInfo mJob;
        private final ArrayList<TriggerContentUri> mTriggerContentUris = new ArrayList<>();
        private boolean mBackoffCriteriaSet;

        public Builder(int jobId, ComponentName jobService)
        {
            mJob = new JobInfo(jobId, jobService);
        }

        public Builder setPriority(int priority)
        {
            if (priority < PRIORITY_MIN || priority > PRIORITY_MAX) throw new IllegalArgumentException("Invalid priority value");
            mJob.mPriority = priority;
            return this;
        }

        public Builder setExtras(PersistableBundle extras)
        {
            mJob.mExtras = extras == null ? PersistableBundle.EMPTY : extras;
            return this;
        }

        public Builder setTransientExtras(Bundle extras)
        {
            mJob.mTransientExtras = extras == null ? Bundle.EMPTY : extras;
            return this;
        }

        public Builder setClipData(ClipData clip, int grantFlags)
        {
            mJob.mClipData = clip;
            mJob.mClipGrantFlags = grantFlags;
            return this;
        }

        public Builder setRequiredNetworkType(int networkType)
        {
            mJob.mNetworkType = networkType;
            mJob.mNetworkRequest = null;
            return this;
        }

        public Builder setRequiredNetwork(NetworkRequest networkRequest)
        {
            mJob.mNetworkRequest = networkRequest;
            mJob.mNetworkType = networkRequest == null ? NETWORK_TYPE_NONE : NETWORK_TYPE_ANY;
            return this;
        }

        public Builder setEstimatedNetworkBytes(long downloadBytes, long uploadBytes)
        {
            mJob.mEstimatedDownloadBytes = downloadBytes;
            mJob.mEstimatedUploadBytes = uploadBytes;
            return this;
        }

        public Builder setMinimumNetworkChunkBytes(long chunkSizeBytes)
        {
            if (chunkSizeBytes != NETWORK_BYTES_UNKNOWN && chunkSizeBytes <= 0)
            {
                throw new IllegalArgumentException("Minimum chunk size must be positive");
            }
            mJob.mMinimumChunkBytes = chunkSizeBytes;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging)
        {
            setConstraint(CONSTRAINT_CHARGING, requiresCharging);
            return this;
        }

        public Builder setRequiresBatteryNotLow(boolean batteryNotLow)
        {
            setConstraint(CONSTRAINT_BATTERY_NOT_LOW, batteryNotLow);
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle)
        {
            setConstraint(CONSTRAINT_IDLE, requiresDeviceIdle);
            return this;
        }

        public Builder setRequiresStorageNotLow(boolean storageNotLow)
        {
            setConstraint(CONSTRAINT_STORAGE_NOT_LOW, storageNotLow);
            return this;
        }

        public Builder addTriggerContentUri(TriggerContentUri uri)
        {
            mTriggerContentUris.add(Objects.requireNonNull(uri));
            return this;
        }

        public Builder setTriggerContentUpdateDelay(long durationMs)
        {
            mJob.mTriggerContentUpdateDelay = durationMs;
            return this;
        }

        public Builder setTriggerContentMaxDelay(long durationMs)
        {
            mJob.mTriggerContentMaxDelay = durationMs;
            return this;
        }

        public Builder setPeriodic(long intervalMillis)
        {
            return setPeriodic(intervalMillis, intervalMillis);
        }

        public Builder setPeriodic(long intervalMillis, long flexMillis)
        {
            long interval = Math.max(intervalMillis, MIN_PERIOD_MILLIS);
            long flex = Math.min(Math.max(flexMillis, Math.max(MIN_FLEX_MILLIS, interval / 20)), interval);
            mJob.mPeriodic = true;
            mJob.mIntervalMillis = interval;
            mJob.mFlexMillis = flex;
            return this;
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

        public Builder setBackoffCriteria(long initialBackoffMillis, int backoffPolicy)
        {
            mBackoffCriteriaSet = true;
            mJob.mInitialBackoffMillis = Math.min(Math.max(initialBackoffMillis, 10000L), MAX_BACKOFF_DELAY_MILLIS);
            mJob.mBackoffPolicy = backoffPolicy;
            return this;
        }

        public Builder setExpedited(boolean expedited)
        {
            setFlag(FLAG_EXPEDITED, expedited);
            return this;
        }

        public Builder setUserInitiated(boolean userInitiated)
        {
            setFlag(FLAG_USER_INITIATED, userInitiated);
            return this;
        }

        public Builder setImportantWhileForeground(boolean importantWhileForeground)
        {
            setFlag(FLAG_IMPORTANT_WHILE_FOREGROUND, importantWhileForeground);
            return this;
        }

        public Builder setPrefetch(boolean prefetch)
        {
            setFlag(FLAG_PREFETCH, prefetch);
            return this;
        }

        public Builder setPersisted(boolean isPersisted)
        {
            mJob.mPersisted = isPersisted;
            return this;
        }

        public Builder setTraceTag(String traceTag)
        {
            mJob.mTraceTag = traceTag;
            return this;
        }

        public JobInfo build()
        {
            if (mJob.mPeriodic && (mJob.mMinimumLatencyMillis > 0 || mJob.mOverrideDeadlineMillis >= 0))
            {
                throw new IllegalArgumentException("Can't call setMinimumLatency() or setOverrideDeadline() on a periodic job.");
            }
            if (mJob.mPeriodic && !mTriggerContentUris.isEmpty())
            {
                throw new IllegalArgumentException("Can't call addTriggerContentUri() on a periodic job");
            }
            if (mJob.isExpedited() && (mJob.mPeriodic || mJob.mMinimumLatencyMillis > 0 || mJob.isRequireDeviceIdle()
                                       || mJob.isRequireCharging() || mJob.isRequireBatteryNotLow()
                                       || mJob.isRequireStorageNotLow() || !mTriggerContentUris.isEmpty()))
            {
                throw new IllegalArgumentException("An expedited job can only have network and storage-not-low constraints");
            }
            if (mBackoffCriteriaSet && mJob.isRequireDeviceIdle())
            {
                throw new IllegalArgumentException("An idle mode job will not respect any back-off policy, so calling "
                                                   + "setBackoffCriteria with setRequiresDeviceIdle is an error.");
            }

            JobInfo job = new JobInfo(mJob.mId, mJob.mService);
            job.mExtras = mJob.mExtras;
            job.mTransientExtras = mJob.mTransientExtras;
            job.mClipData = mJob.mClipData;
            job.mClipGrantFlags = mJob.mClipGrantFlags;
            job.mMinimumLatencyMillis = mJob.mMinimumLatencyMillis;
            job.mOverrideDeadlineMillis = mJob.mOverrideDeadlineMillis;
            job.mIntervalMillis = mJob.mIntervalMillis;
            job.mFlexMillis = mJob.mFlexMillis;
            job.mPeriodic = mJob.mPeriodic;
            job.mPersisted = mJob.mPersisted;
            job.mNetworkType = mJob.mNetworkType;
            job.mNetworkRequest = mJob.mNetworkRequest;
            job.mConstraintFlags = mJob.mConstraintFlags;
            job.mInitialBackoffMillis = mJob.mInitialBackoffMillis;
            job.mBackoffPolicy = mJob.mBackoffPolicy;
            job.mPriority = mJob.mPriority;
            job.mFlags = mJob.mFlags;
            job.mTriggerContentUris =
                    mTriggerContentUris.isEmpty() ? null : mTriggerContentUris.toArray(new TriggerContentUri[0]);
            job.mTriggerContentUpdateDelay = mJob.mTriggerContentUpdateDelay;
            job.mTriggerContentMaxDelay = mJob.mTriggerContentMaxDelay;
            job.mEstimatedDownloadBytes = mJob.mEstimatedDownloadBytes;
            job.mEstimatedUploadBytes = mJob.mEstimatedUploadBytes;
            job.mMinimumChunkBytes = mJob.mMinimumChunkBytes;
            job.mTraceTag = mJob.mTraceTag;
            return job;
        }

        private void setConstraint(int flag, boolean enabled)
        {
            mJob.mConstraintFlags = enabled ? mJob.mConstraintFlags | flag : mJob.mConstraintFlags & ~flag;
        }

        private void setFlag(int flag, boolean enabled)
        {
            mJob.mFlags = enabled ? mJob.mFlags | flag : mJob.mFlags & ~flag;
        }
    }
}

package android.app.job;

import android.content.ClipData;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

public class JobParameters implements Parcelable
{
    public static final int STOP_REASON_UNDEFINED = 0;
    public static final int STOP_REASON_CANCELLED_BY_APP = 1;
    public static final int STOP_REASON_PREEMPT = 2;
    public static final int STOP_REASON_TIMEOUT = 3;
    public static final int STOP_REASON_DEVICE_STATE = 4;
    public static final int STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW = 5;
    public static final int STOP_REASON_CONSTRAINT_CHARGING = 6;
    public static final int STOP_REASON_CONSTRAINT_CONNECTIVITY = 7;
    public static final int STOP_REASON_CONSTRAINT_DEVICE_IDLE = 8;
    public static final int STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW = 9;
    public static final int STOP_REASON_QUOTA = 10;
    public static final int STOP_REASON_BACKGROUND_RESTRICTION = 11;
    public static final int STOP_REASON_APP_STANDBY = 12;
    public static final int STOP_REASON_USER = 13;
    public static final int STOP_REASON_SYSTEM_PROCESSING = 14;
    public static final int STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED = 15;

    public static final Parcelable.Creator<JobParameters> CREATOR = new Parcelable.Creator<JobParameters>()
    {
        @Override
        public JobParameters createFromParcel(Parcel source)
        {
            JobParameters parameters = new JobParameters(source.readInt(), source.readString(), source.readPersistableBundle(),
                                                         source.readBundle(), null, null, null, false, false, false);
            parameters.mStopReason = source.readInt();
            parameters.mStopped = source.readInt() != 0;
            return parameters;
        }

        @Override
        public JobParameters[] newArray(int size)
        {
            return new JobParameters[size];
        }
    };

    private final int mJobId;
    private final String mJobNamespace;
    private final PersistableBundle mExtras;
    private final Bundle mTransientExtras;
    private final ClipData mClipData;
    private final int mClipGrantFlags;
    private final Uri[] mTriggeredContentUris;
    private final String[] mTriggeredContentAuthorities;
    private final Network mNetwork;
    private final boolean mOverrideDeadlineExpired;
    private final boolean mExpedited;
    private final boolean mUserInitiated;
    private int mStopReason = STOP_REASON_UNDEFINED;
    private boolean mStopped;

    public JobParameters(int jobId, Bundle transientExtras)
    {
        this(jobId, null, PersistableBundle.EMPTY, transientExtras, null, null, null, false, false, false);
    }

    public JobParameters(int jobId, String jobNamespace, PersistableBundle extras, Bundle transientExtras, Uri[] triggeredContentUris,
                         String[] triggeredContentAuthorities, Network network, boolean overrideDeadlineExpired, boolean expedited,
                         boolean userInitiated)
    {
        this(jobId, jobNamespace, extras, transientExtras, null, 0, triggeredContentUris, triggeredContentAuthorities, network,
             overrideDeadlineExpired, expedited, userInitiated);
    }

    public JobParameters(int jobId, String jobNamespace, PersistableBundle extras, Bundle transientExtras, ClipData clipData,
                         int clipGrantFlags, Uri[] triggeredContentUris, String[] triggeredContentAuthorities, Network network,
                         boolean overrideDeadlineExpired, boolean expedited, boolean userInitiated)
    {
        mJobId = jobId;
        mJobNamespace = jobNamespace;
        mExtras = extras == null ? PersistableBundle.EMPTY : extras;
        mTransientExtras = transientExtras == null ? Bundle.EMPTY : transientExtras;
        mClipData = clipData;
        mClipGrantFlags = clipGrantFlags;
        mTriggeredContentUris = triggeredContentUris;
        mTriggeredContentAuthorities = triggeredContentAuthorities;
        mNetwork = network;
        mOverrideDeadlineExpired = overrideDeadlineExpired;
        mExpedited = expedited;
        mUserInitiated = userInitiated;
    }

    public int getJobId()
    {
        return mJobId;
    }

    public String getJobNamespace()
    {
        return mJobNamespace;
    }

    public int getStopReason()
    {
        return mStopReason;
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

    public boolean isExpeditedJob()
    {
        return mExpedited;
    }

    public boolean isUserInitiatedJob()
    {
        return mUserInitiated;
    }

    public boolean isOverrideDeadlineExpired()
    {
        return mOverrideDeadlineExpired;
    }

    public Uri[] getTriggeredContentUris()
    {
        return mTriggeredContentUris;
    }

    public String[] getTriggeredContentAuthorities()
    {
        return mTriggeredContentAuthorities;
    }

    public Network getNetwork()
    {
        return mNetwork;
    }

    public JobWorkItem dequeueWork()
    {
        return runtime.Jobs.dequeueWork(this);
    }

    public void completeWork(JobWorkItem work)
    {
        if (!runtime.Jobs.completeWork(this, work))
        {
            throw new IllegalArgumentException("Given work is not active: " + work);
        }
    }

    public boolean isStopped()
    {
        return mStopped;
    }

    public void markStopped(int stopReason)
    {
        mStopped = true;
        mStopReason = stopReason;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mJobId);
        destination.writeString(mJobNamespace);
        destination.writePersistableBundle(mExtras);
        destination.writeBundle(mTransientExtras);
        destination.writeInt(mStopReason);
        destination.writeInt(mStopped ? 1 : 0);
    }
}

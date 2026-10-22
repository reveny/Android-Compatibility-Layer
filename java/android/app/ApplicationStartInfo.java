package android.app;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class ApplicationStartInfo implements Parcelable
{
    public static final int STARTUP_STATE_STARTED = 0;
    public static final int STARTUP_STATE_ERROR = 1;
    public static final int STARTUP_STATE_FIRST_FRAME_DRAWN = 2;

    public static final int START_REASON_ALARM = 0;
    public static final int START_REASON_BACKUP = 1;
    public static final int START_REASON_BOOT_COMPLETE = 2;
    public static final int START_REASON_BROADCAST = 3;
    public static final int START_REASON_CONTENT_PROVIDER = 4;
    public static final int START_REASON_JOB = 5;
    public static final int START_REASON_LAUNCHER = 6;
    public static final int START_REASON_LAUNCHER_RECENTS = 7;
    public static final int START_REASON_OTHER = 8;
    public static final int START_REASON_PUSH = 9;
    public static final int START_REASON_SERVICE = 10;
    public static final int START_REASON_START_ACTIVITY = 11;

    public static final int START_TYPE_UNSET = 0;
    public static final int START_TYPE_COLD = 1;
    public static final int START_TYPE_WARM = 2;
    public static final int START_TYPE_HOT = 3;

    public static final int LAUNCH_MODE_STANDARD = 0;
    public static final int LAUNCH_MODE_SINGLE_TOP = 1;
    public static final int LAUNCH_MODE_SINGLE_INSTANCE = 2;
    public static final int LAUNCH_MODE_SINGLE_TASK = 3;
    public static final int LAUNCH_MODE_SINGLE_INSTANCE_PER_TASK = 4;

    public static final int START_COMPONENT_ACTIVITY = 1;
    public static final int START_COMPONENT_BROADCAST = 2;
    public static final int START_COMPONENT_CONTENT_PROVIDER = 3;
    public static final int START_COMPONENT_SERVICE = 4;
    public static final int START_COMPONENT_OTHER = 5;

    public static final int START_TIMESTAMP_LAUNCH = 0;
    public static final int START_TIMESTAMP_FORK = 1;
    public static final int START_TIMESTAMP_APPLICATION_ONCREATE = 2;
    public static final int START_TIMESTAMP_BIND_APPLICATION = 3;
    public static final int START_TIMESTAMP_FIRST_FRAME = 4;
    public static final int START_TIMESTAMP_FULLY_DRAWN = 5;
    public static final int START_TIMESTAMP_INITIAL_RENDERTHREAD_FRAME = 6;
    public static final int START_TIMESTAMP_SURFACEFLINGER_COMPOSITION_COMPLETE = 7;
    public static final int START_TIMESTAMP_RESERVED_RANGE_SYSTEM = 20;
    public static final int START_TIMESTAMP_RESERVED_RANGE_DEVELOPER_START = 21;
    public static final int START_TIMESTAMP_RESERVED_RANGE_DEVELOPER = 30;

    public static final Creator<ApplicationStartInfo> CREATOR = new Creator<ApplicationStartInfo>()
    {
        @Override
        public ApplicationStartInfo createFromParcel(Parcel parcel)
        {
            ApplicationStartInfo info = new ApplicationStartInfo();

            info.mStartupState = parcel.readInt();
            info.mPid = parcel.readInt();
            info.mRealUid = parcel.readInt();
            info.mPackageUid = parcel.readInt();
            info.mDefiningUid = parcel.readInt();
            info.mProcessName = parcel.readString();
            info.mReason = parcel.readInt();
            info.mStartType = parcel.readInt();
            info.mLaunchMode = parcel.readInt();
            info.mStartComponent = parcel.readInt();
            info.mWasForceStopped = parcel.readBoolean();

            int count = parcel.readInt();
            for (int index = 0; index < count; index++)
            {
                int key = parcel.readInt();
                info.mTimestamps.put(key, parcel.readLong());
            }

            if (parcel.readBoolean()) info.mIntent = Intent.CREATOR.createFromParcel(parcel);

            return info;
        }

        @Override
        public ApplicationStartInfo[] newArray(int size)
        {
            return new ApplicationStartInfo[size];
        }
    };

    private static ApplicationStartInfo sCurrent;

    private int mStartupState = STARTUP_STATE_STARTED;
    private int mPid;
    private int mRealUid;
    private int mPackageUid;
    private int mDefiningUid;
    private String mProcessName;
    private int mReason = START_REASON_LAUNCHER;
    private int mStartType = START_TYPE_COLD;
    private int mLaunchMode = LAUNCH_MODE_STANDARD;
    private int mStartComponent = START_COMPONENT_ACTIVITY;
    private boolean mWasForceStopped;
    private Intent mIntent;
    private final Map<Integer, Long> mTimestamps = new TreeMap<>();

    ApplicationStartInfo()
    {
    }

    static ApplicationStartInfo current()
    {
        synchronized (ApplicationStartInfo.class)
        {
            if (sCurrent == null)
            {
                ApplicationStartInfo info = new ApplicationStartInfo();
                info.mPid = android.os.Process.myPid();
                info.mRealUid = android.os.Process.myUid();
                info.mPackageUid = info.mRealUid;
                info.mDefiningUid = info.mRealUid;
                info.mProcessName = android.os.Process.myProcessName();
                info.mTimestamps.put(START_TIMESTAMP_LAUNCH, android.os.Process.getStartRequestedElapsedRealtime() * 1000000L);
                info.mTimestamps.put(START_TIMESTAMP_FORK, android.os.Process.getStartElapsedRealtime() * 1000000L);
                sCurrent = info;
            }

            return sCurrent;
        }
    }

    static void recordTimestamp(int key, long timestampNanos)
    {
        ApplicationStartInfo info = current();

        synchronized (ApplicationStartInfo.class)
        {
            info.mTimestamps.put(key, timestampNanos);
            if (key == START_TIMESTAMP_FIRST_FRAME) info.mStartupState = STARTUP_STATE_FIRST_FRAME_DRAWN;
        }
    }

    static void recordIntent(Intent intent)
    {
        ApplicationStartInfo info = current();

        synchronized (ApplicationStartInfo.class)
        {
            info.mIntent = intent;
        }
    }

    public int getStartupState()
    {
        return mStartupState;
    }

    public int getPid()
    {
        return mPid;
    }

    public int getRealUid()
    {
        return mRealUid;
    }

    public int getPackageUid()
    {
        return mPackageUid;
    }

    public int getDefiningUid()
    {
        return mDefiningUid;
    }

    public String getProcessName()
    {
        return mProcessName;
    }

    public int getReason()
    {
        return mReason;
    }

    public int getStartType()
    {
        return mStartType;
    }

    public int getLaunchMode()
    {
        return mLaunchMode;
    }

    public int getStartComponent()
    {
        return mStartComponent;
    }

    public boolean wasForceStopped()
    {
        return mWasForceStopped;
    }

    public Intent getIntent()
    {
        return mIntent;
    }

    public Map<Integer, Long> getStartupTimestamps()
    {
        synchronized (ApplicationStartInfo.class)
        {
            return Collections.unmodifiableMap(new TreeMap<>(mTimestamps));
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mStartupState);
        destination.writeInt(mPid);
        destination.writeInt(mRealUid);
        destination.writeInt(mPackageUid);
        destination.writeInt(mDefiningUid);
        destination.writeString(mProcessName);
        destination.writeInt(mReason);
        destination.writeInt(mStartType);
        destination.writeInt(mLaunchMode);
        destination.writeInt(mStartComponent);
        destination.writeBoolean(mWasForceStopped);

        Map<Integer, Long> timestamps = getStartupTimestamps();
        destination.writeInt(timestamps.size());
        for (Map.Entry<Integer, Long> entry : timestamps.entrySet())
        {
            destination.writeInt(entry.getKey());
            destination.writeLong(entry.getValue());
        }

        destination.writeBoolean(mIntent != null);
        if (mIntent != null) mIntent.writeToParcel(destination, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ApplicationStartInfo)) return false;

        ApplicationStartInfo info = (ApplicationStartInfo)other;
        return mPid == info.mPid && mRealUid == info.mRealUid && mPackageUid == info.mPackageUid
               && mDefiningUid == info.mDefiningUid && mReason == info.mReason && mStartupState == info.mStartupState
               && mStartType == info.mStartType && mLaunchMode == info.mLaunchMode && mStartComponent == info.mStartComponent
               && mWasForceStopped == info.mWasForceStopped && Objects.equals(mProcessName, info.mProcessName)
               && getStartupTimestamps().equals(info.getStartupTimestamps());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPid, mRealUid, mPackageUid, mDefiningUid, mReason, mStartupState, mStartType, mLaunchMode,
                            mProcessName, mStartComponent);
    }

    @Override
    public String toString()
    {
        return "ApplicationStartInfo(pid=" + mPid + " realUid=" + mRealUid + " packageUid=" + mPackageUid + " definingUid="
               + mDefiningUid + " process=" + mProcessName + " startupState=" + mStartupState + " reason=" + mReason
               + " startType=" + mStartType + " launchMode=" + mLaunchMode + " startComponent=" + mStartComponent
               + " timestamps=" + getStartupTimestamps() + ")";
    }
}

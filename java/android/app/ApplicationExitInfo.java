package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Arrays;
import java.util.Objects;

public final class ApplicationExitInfo implements Parcelable
{
    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_EXIT_SELF = 1;
    public static final int REASON_SIGNALED = 2;
    public static final int REASON_LOW_MEMORY = 3;
    public static final int REASON_CRASH = 4;
    public static final int REASON_CRASH_NATIVE = 5;
    public static final int REASON_ANR = 6;
    public static final int REASON_INITIALIZATION_FAILURE = 7;
    public static final int REASON_PERMISSION_CHANGE = 8;
    public static final int REASON_EXCESSIVE_RESOURCE_USAGE = 9;
    public static final int REASON_USER_REQUESTED = 10;
    public static final int REASON_USER_STOPPED = 11;
    public static final int REASON_DEPENDENCY_DIED = 12;
    public static final int REASON_OTHER = 13;
    public static final int REASON_FREEZER = 14;
    public static final int REASON_PACKAGE_STATE_CHANGE = 15;
    public static final int REASON_PACKAGE_UPDATED = 16;

    public static final Creator<ApplicationExitInfo> CREATOR = new Creator<ApplicationExitInfo>()
    {
        @Override
        public ApplicationExitInfo createFromParcel(Parcel parcel)
        {
            ApplicationExitInfo info = new ApplicationExitInfo();

            info.mPid = parcel.readInt();
            info.mRealUid = parcel.readInt();
            info.mPackageUid = parcel.readInt();
            info.mDefiningUid = parcel.readInt();
            info.mProcessName = parcel.readString();
            info.mReason = parcel.readInt();
            info.mStatus = parcel.readInt();
            info.mImportance = parcel.readInt();
            info.mPss = parcel.readLong();
            info.mRss = parcel.readLong();
            info.mTimestamp = parcel.readLong();
            info.mDescription = parcel.readString();
            info.mState = parcel.createByteArray();

            return info;
        }

        @Override
        public ApplicationExitInfo[] newArray(int size)
        {
            return new ApplicationExitInfo[size];
        }
    };

    private int mPid;
    private int mRealUid;
    private int mPackageUid;
    private int mDefiningUid;
    private String mProcessName;
    private int mReason;
    private int mStatus;
    private int mImportance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE;
    private long mPss;
    private long mRss;
    private long mTimestamp;
    private String mDescription;
    private byte[] mState;

    ApplicationExitInfo()
    {
    }

    ApplicationExitInfo(int reason, int status, long timestamp, String description, String processName)
    {
        mReason = reason;
        mStatus = status;
        mTimestamp = timestamp;
        mDescription = description;
        mProcessName = processName;
    }

    public int getReason()
    {
        return mReason;
    }

    public int getStatus()
    {
        return mStatus;
    }

    public long getTimestamp()
    {
        return mTimestamp;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public String getProcessName()
    {
        return mProcessName;
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

    public int getImportance()
    {
        return mImportance;
    }

    public long getPss()
    {
        return mPss;
    }

    public long getRss()
    {
        return mRss;
    }

    public byte[] getProcessStateSummary()
    {
        return mState;
    }

    public UserHandle getUserHandle()
    {
        return UserHandle.getUserHandleForUid(mRealUid);
    }

    public java.io.InputStream getTraceInputStream() throws java.io.IOException
    {
        return null;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mPid);
        destination.writeInt(mRealUid);
        destination.writeInt(mPackageUid);
        destination.writeInt(mDefiningUid);
        destination.writeString(mProcessName);
        destination.writeInt(mReason);
        destination.writeInt(mStatus);
        destination.writeInt(mImportance);
        destination.writeLong(mPss);
        destination.writeLong(mRss);
        destination.writeLong(mTimestamp);
        destination.writeString(mDescription);
        destination.writeByteArray(mState);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ApplicationExitInfo)) return false;

        ApplicationExitInfo info = (ApplicationExitInfo)other;
        return mPid == info.mPid && mRealUid == info.mRealUid && mPackageUid == info.mPackageUid
               && mDefiningUid == info.mDefiningUid && mReason == info.mReason && mStatus == info.mStatus
               && mImportance == info.mImportance && mPss == info.mPss && mRss == info.mRss && mTimestamp == info.mTimestamp
               && Objects.equals(mProcessName, info.mProcessName) && Objects.equals(mDescription, info.mDescription)
               && Arrays.equals(mState, info.mState);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPid, mRealUid, mProcessName, mReason, mStatus, mTimestamp, mDescription);
    }

    @Override
    public String toString()
    {
        return "ApplicationExitInfo(timestamp=" + mTimestamp + " pid=" + mPid + " realUid=" + mRealUid + " packageUid="
               + mPackageUid + " definingUid=" + mDefiningUid + " user=" + getUserHandle().getIdentifier() + " process="
               + mProcessName + " reason=" + mReason + " status=" + mStatus + " importance=" + mImportance + " pss=" + mPss
               + " rss=" + mRss + " description=" + mDescription + " state=" + (mState == null ? 0 : mState.length) + "B)";
    }
}

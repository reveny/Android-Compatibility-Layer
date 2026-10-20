package android.app;

public final class ApplicationExitInfo
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

    private final int mReason;
    private final int mStatus;
    private final long mTimestamp;
    private final String mDescription;
    private final String mProcessName;

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
        return 0;
    }

    public int getRealUid()
    {
        return 0;
    }

    public int getImportance()
    {
        return ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE;
    }

    public java.io.InputStream getTraceInputStream()
    {
        return null;
    }
}

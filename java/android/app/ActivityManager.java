package android.app;

import java.util.ArrayList;
import java.util.List;

public class ActivityManager
{
    public static final int RECENT_WITH_EXCLUDED = 0x0001;

    private static byte[] sProcessStateSummary;

    private final android.content.Context mContext;

    public ActivityManager(android.content.Context context)
    {
        mContext = context;
    }

    public List<RunningAppProcessInfo> getRunningAppProcesses()
    {
        RunningAppProcessInfo running = new RunningAppProcessInfo();
        running.pid = android.os.Process.myPid();
        running.uid = android.os.Process.myUid();
        running.processName = mContext == null ? "" : mContext.getPackageName();
        running.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        running.pkgList = new String[] {running.processName};

        List<RunningAppProcessInfo> processes = new ArrayList<>();
        processes.add(running);
        return processes;
    }

    public List<ApplicationExitInfo> getHistoricalProcessExitReasons(String packageName, int pid, int maxNum)
    {
        return new ArrayList<>();
    }

    public List<ApplicationStartInfo> getHistoricalProcessStartReasons(int maxNum)
    {
        List<ApplicationStartInfo> reasons = new ArrayList<>();
        if (maxNum >= 0) reasons.add(ApplicationStartInfo.current());
        return reasons;
    }

    public void addStartInfoTimestamp(int key, long timestampNs)
    {
        if (key < ApplicationStartInfo.START_TIMESTAMP_RESERVED_RANGE_DEVELOPER_START
            || key > ApplicationStartInfo.START_TIMESTAMP_RESERVED_RANGE_DEVELOPER)
        {
            throw new IllegalArgumentException("key " + key + " is outside the developer timestamp range");
        }

        ApplicationStartInfo.recordTimestamp(key, timestampNs);
    }

    public void addApplicationStartInfoCompletionListener(java.util.concurrent.Executor executor,
                                                          final java.util.function.Consumer<ApplicationStartInfo> listener)
    {
        if (executor == null || listener == null) throw new NullPointerException();

        final ApplicationStartInfo info = ApplicationStartInfo.current();
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.accept(info);
            }
        });
    }

    public void removeApplicationStartInfoCompletionListener(java.util.function.Consumer<ApplicationStartInfo> listener)
    {
    }

    public void setProcessStateSummary(byte[] state)
    {
        if (state != null && state.length > 128) throw new IllegalArgumentException("the process state summary is limited to 128 bytes");

        synchronized (ActivityManager.class)
        {
            sProcessStateSummary = state == null ? null : state.clone();
        }
    }

    public List<ProcessErrorStateInfo> getProcessesInErrorState()
    {
        return null;
    }

    public void getMemoryInfo(MemoryInfo out)
    {
        if (out == null) return;

        out.availMem = Runtime.getRuntime().freeMemory();
        out.totalMem = Runtime.getRuntime().maxMemory();
        out.advertisedMem = out.totalMem;
        out.threshold = out.totalMem / 16;
        out.lowMemory = false;
    }

    public android.os.Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids)
    {
        int count = pids == null ? 0 : pids.length;
        android.os.Debug.MemoryInfo[] infos = new android.os.Debug.MemoryInfo[count];

        for (int index = 0; index < count; index++)
        {
            infos[index] = new android.os.Debug.MemoryInfo();
            if (pids[index] == android.os.Process.myPid()) android.os.Debug.getMemoryInfo(infos[index]);
        }

        return infos;
    }

    @Deprecated
    public List<RunningServiceInfo> getRunningServices(int maxNum)
    {
        return new ArrayList<>();
    }

    @Deprecated
    public List<RunningTaskInfo> getRunningTasks(int maxNum)
    {
        return new ArrayList<>();
    }

    public List<AppTask> getAppTasks()
    {
        return new ArrayList<>();
    }

    public boolean isBackgroundRestricted()
    {
        return false;
    }

    public int getLauncherLargeIconSize()
    {
        return getLauncherLargeIconDensity() * 48 / 160;
    }

    public int getLauncherLargeIconDensity()
    {
        if (mContext == null) return 480;

        int density = mContext.getResources().getDisplayMetrics().densityDpi;
        return density < 480 ? 480 : 640;
    }

    public boolean clearApplicationUserData()
    {
        return false;
    }

    public void killBackgroundProcesses(String packageName)
    {
    }

    public void moveTaskToFront(int taskId, int flags)
    {
    }

    public static boolean isLowMemoryKillReportSupported()
    {
        return false;
    }

    public android.content.pm.ConfigurationInfo getDeviceConfigurationInfo()
    {
        android.content.pm.ConfigurationInfo info = new android.content.pm.ConfigurationInfo();
        info.reqGlEsVersion = packVersion(runtime.PlatformConfig.GL_ES_VERSION);
        return info;
    }

    private static int packVersion(String version)
    {
        int dot = version == null ? -1 : version.indexOf('.');
        if (dot <= 0) return android.content.pm.ConfigurationInfo.GL_ES_VERSION_UNDEFINED;

        try
        {
            int major = Integer.parseInt(version.substring(0, dot));
            int minor = Integer.parseInt(version.substring(dot + 1));
            return (major << 16) | (minor & 0xffff);
        }
        catch (NumberFormatException malformed)
        {
            return android.content.pm.ConfigurationInfo.GL_ES_VERSION_UNDEFINED;
        }
    }

    public int getMemoryClass()
    {
        return (int)(Runtime.getRuntime().maxMemory() >> 20);
    }

    public int getLargeMemoryClass()
    {
        return getMemoryClass();
    }

    public boolean isLowRamDevice()
    {
        return false;
    }

    public static boolean isRunningInTestHarness()
    {
        return false;
    }

    public static boolean isUserAMonkey()
    {
        return false;
    }

    public static void getMyMemoryState(RunningAppProcessInfo out)
    {
        if (out == null) return;

        out.pid = android.os.Process.myPid();
        out.uid = android.os.Process.myUid();
        out.importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

    public static class RunningAppProcessInfo
    {
        public static final int IMPORTANCE_FOREGROUND = 100;
        public static final int IMPORTANCE_VISIBLE = 200;
        public static final int IMPORTANCE_SERVICE = 300;
        public static final int IMPORTANCE_CACHED = 400;
        public static final int IMPORTANCE_GONE = 1000;

        public String processName;
        public int pid;
        public int uid;
        public String[] pkgList;
        public int importance;
        public int lru;
        public int importanceReasonCode;
        public int lastTrimLevel;
        public android.content.ComponentName importanceReasonComponent;
        public int importanceReasonPid;

        public RunningAppProcessInfo()
        {
        }

        public RunningAppProcessInfo(String processName, int pid, String[] pkgList)
        {
            this.processName = processName;
            this.pid = pid;
            this.pkgList = pkgList;
        }
    }

    public static class MemoryInfo
    {
        public long advertisedMem;
        public long availMem;
        public long totalMem;
        public long threshold;
        public boolean lowMemory;
    }

    public static class RunningServiceInfo
    {
        public android.content.ComponentName service;
        public int pid;
        public int uid;
        public String process;
        public boolean foreground;
        public long activeSince;
        public boolean started;
        public int clientCount;
        public int crashCount;
        public long lastActivityTime;
        public long restarting;
        public int flags;
        public String clientPackage;
        public int clientLabel;
    }

    public static class TaskDescription
    {
        private final String mLabel;
        private final int mPrimaryColor;

        public TaskDescription()
        {
            this(null, 0);
        }

        public TaskDescription(String label)
        {
            this(label, 0);
        }

        public TaskDescription(String label, int iconRes, int colorPrimary)
        {
            this(label, colorPrimary);
        }

        public TaskDescription(String label, android.graphics.Bitmap icon, int colorPrimary)
        {
            this(label, colorPrimary);
        }

        public TaskDescription(String label, android.graphics.Bitmap icon)
        {
            this(label, 0);
        }

        private TaskDescription(String label, int colorPrimary)
        {
            mLabel = label;
            mPrimaryColor = colorPrimary;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public int getPrimaryColor()
        {
            return mPrimaryColor;
        }

        public android.graphics.Bitmap getIcon()
        {
            return null;
        }
    }

    public static class RunningTaskInfo
    {
        public int id;
        public int taskId;
        public android.content.ComponentName baseActivity;
        public android.content.ComponentName topActivity;
        public int numActivities;
        public int numRunning;
        public android.content.Intent baseIntent;
        public TaskDescription taskDescription;
    }

    public static class RecentTaskInfo extends RunningTaskInfo
    {
        public int persistentId;
        public android.content.ComponentName origActivity;
    }

    public static class ProcessErrorStateInfo implements android.os.Parcelable
    {
        public static final int NO_ERROR = 0;
        public static final int CRASHED = 1;
        public static final int NOT_RESPONDING = 2;

        public static final Creator<ProcessErrorStateInfo> CREATOR = new Creator<ProcessErrorStateInfo>()
        {
            @Override
            public ProcessErrorStateInfo createFromParcel(android.os.Parcel parcel)
            {
                ProcessErrorStateInfo info = new ProcessErrorStateInfo();
                info.readFromParcel(parcel);
                return info;
            }

            @Override
            public ProcessErrorStateInfo[] newArray(int size)
            {
                return new ProcessErrorStateInfo[size];
            }
        };

        public int condition;
        public String processName;
        public int pid;
        public int uid;
        public String tag;
        public String shortMsg;
        public String longMsg;
        public String stackTrace;
        public byte[] crashData;

        public ProcessErrorStateInfo()
        {
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(android.os.Parcel destination, int flags)
        {
            destination.writeInt(condition);
            destination.writeString(processName);
            destination.writeInt(pid);
            destination.writeInt(uid);
            destination.writeString(tag);
            destination.writeString(shortMsg);
            destination.writeString(longMsg);
            destination.writeString(stackTrace);
        }

        public void readFromParcel(android.os.Parcel source)
        {
            condition = source.readInt();
            processName = source.readString();
            pid = source.readInt();
            uid = source.readInt();
            tag = source.readString();
            shortMsg = source.readString();
            longMsg = source.readString();
            stackTrace = source.readString();
        }
    }

    public static class AppTask
    {
        public void finishAndRemoveTask()
        {
        }

        public RecentTaskInfo getTaskInfo()
        {
            return new RecentTaskInfo();
        }

        public void moveToFront()
        {
        }

        public void setExcludeFromRecents(boolean exclude)
        {
        }

        public void startActivity(android.content.Context context, android.content.Intent intent, android.os.Bundle options)
        {
            if (context != null) context.startActivity(intent, options);
        }
    }
}

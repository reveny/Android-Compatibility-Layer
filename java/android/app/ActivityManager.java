package android.app;

import java.util.ArrayList;
import java.util.List;

public class ActivityManager
{
    public static final int RECENT_WITH_EXCLUDED = 0x0001;


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

    public void getMemoryInfo(MemoryInfo out)
    {
        if (out == null) return;

        out.availMem = Runtime.getRuntime().freeMemory();
        out.totalMem = Runtime.getRuntime().maxMemory();
        out.threshold = out.totalMem / 16;
        out.lowMemory = false;
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

        public RunningAppProcessInfo()
        {
        }
    }

    public static class MemoryInfo
    {
        public long availMem;
        public long totalMem;
        public long threshold;
        public boolean lowMemory;
    }
}

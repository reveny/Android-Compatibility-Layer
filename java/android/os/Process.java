package android.os;

public class Process
{
    public static final int THREAD_PRIORITY_DEFAULT = 0;
    public static final int THREAD_PRIORITY_LOWEST = 19;
    public static final int THREAD_PRIORITY_BACKGROUND = 10;
    public static final int THREAD_PRIORITY_FOREGROUND = -2;
    public static final int THREAD_PRIORITY_DISPLAY = -4;
    public static final int THREAD_PRIORITY_URGENT_DISPLAY = -8;
    public static final int THREAD_PRIORITY_VIDEO = -10;
    public static final int THREAD_PRIORITY_AUDIO = -16;
    public static final int THREAD_PRIORITY_URGENT_AUDIO = -19;
    public static final int THREAD_PRIORITY_MORE_FAVORABLE = -1;
    public static final int THREAD_PRIORITY_LESS_FAVORABLE = 1;

    public static final int SIGNAL_QUIT = 3;
    public static final int SIGNAL_KILL = 9;
    public static final int SIGNAL_USR1 = 10;

    public static final int INVALID_UID = -1;
    public static final int FIRST_APPLICATION_UID = 10000;

    private static final int LOWEST_JAVA_PRIORITY_BOUNDARY = 10;

    private Process()
    {
    }

    public static int myPid()
    {
        return nativeProcessId();
    }

    public static int myTid()
    {
        return nativeThreadId();
    }

    public static int myUid()
    {
        return nativeUserId();
    }

    public static String myProcessName()
    {
        String name = android.app.Application.getProcessName();

        return name == null ? "acl" : name;
    }

    public static UserHandle myUserHandle()
    {
        return new UserHandle(myUid() / 100000);
    }

    public static boolean is64Bit()
    {
        return true;
    }

    public static void setThreadPriority(int priority)
    {
        Thread.currentThread().setPriority(toJavaPriority(priority));
    }

    public static void setThreadPriority(int tid, int priority)
    {
        if (tid == myTid()) setThreadPriority(priority);
    }

    public static int getThreadPriority(int tid)
    {
        if (tid != myTid()) return THREAD_PRIORITY_DEFAULT;

        int java = Thread.currentThread().getPriority();
        if (java >= Thread.MAX_PRIORITY) return THREAD_PRIORITY_URGENT_DISPLAY;
        if (java <= Thread.MIN_PRIORITY) return THREAD_PRIORITY_LOWEST;

        return THREAD_PRIORITY_DEFAULT;
    }

    public static void killProcess(int pid)
    {
        if (pid == myPid()) Looper.quitMainLooper();
    }

    public static void sendSignal(int pid, int signal)
    {
    }

    public static long getElapsedCpuTime()
    {
        return SystemClock.uptimeMillis();
    }

    public static final int ROOT_UID = 0;
    public static final int SYSTEM_UID = 1000;
    public static final int PHONE_UID = 1001;
    public static final int SHELL_UID = 2000;
    public static final int BLUETOOTH_UID = 1002;
    public static final int WIFI_UID = 1010;
    public static final int LAST_APPLICATION_UID = 19999;

    private static final long START_UPTIME_MILLIS = SystemClock.uptimeMillis();
    private static final long START_ELAPSED_REALTIME = SystemClock.elapsedRealtime();

    public static long getStartElapsedRealtime()
    {
        return START_ELAPSED_REALTIME;
    }

    public static long getStartUptimeMillis()
    {
        return START_UPTIME_MILLIS;
    }

    public static long getStartRequestedElapsedRealtime()
    {
        return START_ELAPSED_REALTIME;
    }

    public static long getStartRequestedUptimeMillis()
    {
        return START_UPTIME_MILLIS;
    }

    public static boolean isApplicationUid(int uid)
    {
        int appId = uid % 100000;
        return appId >= FIRST_APPLICATION_UID && appId <= LAST_APPLICATION_UID;
    }

    public static final boolean isIsolated()
    {
        return false;
    }

    public static final boolean isSdkSandbox()
    {
        return false;
    }

    public static final int getUidForName(String name)
    {
        return -1;
    }

    public static final int getGidForName(String name)
    {
        return -1;
    }

    public static final int[] getExclusiveCores()
    {
        return new int[0];
    }

    public static final void setThreadPriority(int tid, int priority, boolean ignored)
    {
        setThreadPriority(tid, priority);
    }

    private static native int nativeProcessId();

    private static native int nativeThreadId();

    private static native int nativeUserId();

    private static int toJavaPriority(int priority)
    {
        if (priority <= THREAD_PRIORITY_URGENT_DISPLAY) return Thread.MAX_PRIORITY;
        if (priority >= LOWEST_JAVA_PRIORITY_BOUNDARY) return Thread.MIN_PRIORITY;

        return Thread.NORM_PRIORITY;
    }
}

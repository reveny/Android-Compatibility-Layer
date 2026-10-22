package android.util;

import android.content.Context;

public class BoostFramework
{
    public static final int VENDOR_HINT_SCROLL_BOOST = 0x00001080;
    public static final int VENDOR_HINT_FIRST_LAUNCH_BOOST = 0x00001081;
    public static final int VENDOR_HINT_SUBSEQ_LAUNCH_BOOST = 0x00001082;
    public static final int VENDOR_HINT_ANIM_BOOST = 0x00001083;
    public static final int VENDOR_HINT_ACTIVITY_BOOST = 0x00001084;
    public static final int VENDOR_HINT_TOUCH_BOOST = 0x00001085;
    public static final int VENDOR_HINT_MTP_BOOST = 0x00001086;
    public static final int VENDOR_HINT_DRAG_BOOST = 0x00001087;
    public static final int VENDOR_HINT_PACKAGE_INSTALL_BOOST = 0x00001088;
    public static final int VENDOR_HINT_ROTATION_LATENCY_BOOST = 0x00001089;
    public static final int VENDOR_HINT_ROTATION_ANIM_BOOST = 0x00001090;
    public static final int VENDOR_HINT_PERFORMANCE_MODE = 0x00001091;
    public static final int VENDOR_HINT_APP_UPDATE = 0x00001092;
    public static final int VENDOR_HINT_KILL = 0x00001093;
    public static final int VENDOR_HINT_BOOST_RENDERTHREAD = 0x00001096;
    public static final int VENDOR_HINT_FIRST_DRAW = 0x00001042;
    public static final int VENDOR_HINT_TAP_EVENT = 0x00001043;
    public static final int VENDOR_HINT_DISPLAY_OFF = 0x00001040;
    public static final int VENDOR_HINT_DISPLAY_ON = 0x00001041;

    public static final int VENDOR_FEEDBACK_WORKLOAD_TYPE = 0x00001601;
    public static final int VENDOR_FEEDBACK_LAUNCH_END_POINT = 0x00001602;
    public static final int VENDOR_FEEDBACK_PA_FW = 0x00001603;

    public static final int UXE_TRIGGER = 1;
    public static final int UXE_EVENT_BINDAPP = 2;
    public static final int UXE_EVENT_DISPLAYED_ACT = 3;
    public static final int UXE_EVENT_KILL = 4;
    public static final int UXE_EVENT_GAME = 5;
    public static final int UXE_EVENT_SUB_LAUNCH = 6;
    public static final int UXE_EVENT_PKG_UNINSTALL = 7;
    public static final int UXE_EVENT_PKG_INSTALL = 8;

    public static final int MPCTLV3_GPU_IS_APP_FG = 0x42820000;
    public static final int MPCTLV3_GPU_IS_APP_BG = 0x42824000;

    private static final int FAILURE = -1;

    public BoostFramework()
    {
    }

    public BoostFramework(Context context)
    {
    }

    public BoostFramework(boolean isUntrustedDomain)
    {
    }

    public BoostFramework(Context context, boolean isTrusted)
    {
    }

    public int perfLockAcquire(int duration, int... list)
    {
        return FAILURE;
    }

    public int perfLockRelease()
    {
        return FAILURE;
    }

    public int perfLockReleaseHandler(int handle)
    {
        return FAILURE;
    }

    public int perfHint(int hint, String userDataString)
    {
        return perfHint(hint, userDataString, -1, -1);
    }

    public int perfHint(int hint, String userDataString, int userData)
    {
        return perfHint(hint, userDataString, userData, -1);
    }

    public int perfHint(int hint, String userDataString, int userData1, int userData2)
    {
        return FAILURE;
    }

    public double getPerfHalVersion()
    {
        return 0;
    }

    public int perfGetFeedback(int request, String packageName)
    {
        return FAILURE;
    }

    public int perfGetFeedbackExtn(int request, String packageName, int numArgs, int... list)
    {
        return FAILURE;
    }

    public int perfIOPrefetchStart(int pid, String packageName, String codePath)
    {
        return FAILURE;
    }

    public int perfIOPrefetchStop()
    {
        return FAILURE;
    }

    public int perfUXEngine_events(int opcode, int pid, String packageName, int latency)
    {
        return perfUXEngine_events(opcode, pid, packageName, latency, null);
    }

    public int perfUXEngine_events(int opcode, int pid, String packageName, int latency, String codePath)
    {
        return FAILURE;
    }

    public String perfUXEngine_trigger(int opcode)
    {
        return null;
    }

    public String perfSyncRequest(int opcode)
    {
        return null;
    }

    public String perfGetProp(String propertyName, String defaultValue)
    {
        return defaultValue;
    }

    public int perfLockAcqAndRelease(int handle, int duration, int numArgs, int reserveNumArgs, int... list)
    {
        return FAILURE;
    }

    public void perfEvent(int eventId, String packageName)
    {
        perfEvent(eventId, packageName, 0);
    }

    public void perfEvent(int eventId, String packageName, int numArgs, int... list)
    {
    }

    public boolean perfIsGameApp(String packageName)
    {
        return false;
    }

    public static final class ScrollOptimizer
    {
        public static final int FLING_START = 1;
        public static final int FLING_END = 0;

        private ScrollOptimizer()
        {
        }

        public static void setFrameInterval(long frameIntervalNanos)
        {
        }

        public static void setBLASTBufferQueue(Object blastBufferQueue)
        {
        }

        public static void setMotionType(int eventType)
        {
        }

        public static void setVsyncTime(long vsyncTimeNanos)
        {
        }

        public static void setUITaskStatus(boolean running)
        {
        }

        public static void setFlingFlag(int flag)
        {
        }

        public static boolean shouldUseVsync(boolean defaultValue)
        {
            return defaultValue;
        }

        public static long getAdjustedAnimationClock(long frameTimeNanos)
        {
            return frameTimeNanos;
        }

        public static void disableOptimizer(boolean disabled)
        {
        }
    }
}

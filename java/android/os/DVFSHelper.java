package android.os;

import android.content.Context;

public class DVFSHelper
{
    public static final int TYPE_CPU_MIN = 12;
    public static final int TYPE_CPU_MAX = 13;
    public static final int TYPE_GPU_MIN = 16;
    public static final int TYPE_GPU_MAX = 17;
    public static final int TYPE_BUS_MIN = 19;
    public static final int TYPE_BUS_MAX = 20;
    public static final int TYPE_CPU_CORE_NUM_MIN = 14;
    public static final int TYPE_CPU_CORE_NUM_MAX = 15;

    private final String mTag;
    private final int mType;
    private boolean mAcquired;

    public DVFSHelper(Context context, int type)
    {
        this(context, null, type, 0L);
    }

    public DVFSHelper(Context context, String tag, int type, long value)
    {
        mTag = tag;
        mType = type;
    }

    public static void onScrollEvent(boolean isScrolling)
    {
    }

    public static void onSmoothScrollEvent(boolean isScrolling)
    {
    }

    public void acquire()
    {
        mAcquired = true;
    }

    public void acquire(int durationMillis)
    {
        mAcquired = true;
    }

    public void release()
    {
        mAcquired = false;
    }

    public boolean isAcquired()
    {
        return mAcquired;
    }

    public int[] getSupportedCPUFrequency()
    {
        return new int[0];
    }

    public int[] getSupportedGPUFrequency()
    {
        return new int[0];
    }

    public int[] getSupportedCPUCoreNum()
    {
        return new int[] {Runtime.getRuntime().availableProcessors()};
    }

    @Override
    public String toString()
    {
        return "DVFSHelper{tag=" + mTag + ", type=" + mType + ", acquired=" + mAcquired + "}";
    }
}

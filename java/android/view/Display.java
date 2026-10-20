package android.view;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public final class Display
{
    public static final int DEFAULT_DISPLAY = 0;

    public static final float REFRESH_RATE = 60.0f;

    private static final int PIXEL_FORMAT_RGBA_8888 = 1;

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_OFF = 1;
    public static final int STATE_ON = 2;

    private final int mDisplayId;
    private final String mName;
    private final int mWidth;
    private final int mHeight;
    private final int mDensityDpi;

    public Display(int displayId, String name, int width, int height, int densityDpi)
    {
        mDisplayId = displayId;
        mName = name;
        mWidth = width;
        mHeight = height;
        mDensityDpi = densityDpi;
    }

    public int getDisplayId()
    {
        return mDisplayId;
    }

    public String getName()
    {
        return mName;
    }

    public int getState()
    {
        return STATE_ON;
    }

    public boolean isValid()
    {
        return true;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getRotation()
    {
        return mWidth > mHeight ? Surface.ROTATION_90 : Surface.ROTATION_0;
    }

    public float getRefreshRate()
    {
        return runtime.WindowBridge.refreshRate();
    }

    public Mode getMode()
    {
        return new Mode(1, mWidth, mHeight, getRefreshRate());
    }

    public Mode[] getSupportedModes()
    {
        return new Mode[] {getMode()};
    }

    public float[] getSupportedRefreshRates()
    {
        return new float[] {getRefreshRate()};
    }

    public long getPresentationDeadlineNanos()
    {
        return (long)(1000000000.0f / REFRESH_RATE);
    }

    public long getAppVsyncOffsetNanos()
    {
        return 0;
    }

    public int getFlags()
    {
        return 0;
    }

    public int getPixelFormat()
    {
        return PIXEL_FORMAT_RGBA_8888;
    }

    public boolean isHdr()
    {
        return false;
    }

    public boolean isWideColorGamut()
    {
        return false;
    }

    public boolean isMinimalPostProcessingSupported()
    {
        return false;
    }

    public static final class Mode
    {
        private final int mModeId;
        private final int mWidth;
        private final int mHeight;
        private final float mRefreshRate;

        public Mode(int modeId, int width, int height, float refreshRate)
        {
            mModeId = modeId;
            mWidth = width;
            mHeight = height;
            mRefreshRate = refreshRate;
        }

        public int getModeId()
        {
            return mModeId;
        }

        public int getPhysicalWidth()
        {
            return mWidth;
        }

        public int getPhysicalHeight()
        {
            return mHeight;
        }

        public float getRefreshRate()
        {
            return mRefreshRate;
        }

        public float[] getAlternativeRefreshRates()
        {
            return new float[0];
        }

        @Override
        public String toString()
        {
            return "Mode{id=" + mModeId + ", size=" + mWidth + "x" + mHeight + ", fps=" + mRefreshRate + "}";
        }
    }

    public void getSize(Point outSize)
    {
        if (outSize == null) return;
        outSize.set(mWidth, mHeight);
    }

    public void getRectSize(Rect outSize)
    {
        if (outSize == null) return;
        outSize.set(0, 0, mWidth, mHeight);
    }

    public void getCurrentSizeRange(Point outSmallestSize, Point outLargestSize)
    {
        int shorter = Math.min(mWidth, mHeight);
        int longer = Math.max(mWidth, mHeight);

        if (outSmallestSize != null) outSmallestSize.set(shorter, shorter);
        if (outLargestSize != null) outLargestSize.set(longer, longer);
    }

    public void getMetrics(DisplayMetrics outMetrics)
    {
        if (outMetrics == null) return;

        outMetrics.widthPixels = mWidth;
        outMetrics.heightPixels = mHeight;
        outMetrics.densityDpi = mDensityDpi;
        outMetrics.density = mDensityDpi / (float)DisplayMetrics.DENSITY_DEFAULT;
        outMetrics.scaledDensity = outMetrics.density;
        outMetrics.xdpi = mDensityDpi;
        outMetrics.ydpi = mDensityDpi;
    }

    public void getRealMetrics(DisplayMetrics outMetrics)
    {
        getMetrics(outMetrics);
    }

    public void getRealSize(Point outSize)
    {
        getSize(outSize);
    }
}

package android.view;

import android.graphics.ColorSpace;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class Display
{
    public static final int DEFAULT_DISPLAY = 0;
    public static final int INVALID_DISPLAY = -1;

    public static final float REFRESH_RATE = 60.0f;

    private static final int PIXEL_FORMAT_RGBA_8888 = 1;

    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 1;
    public static final int FLAG_SECURE = 2;
    public static final int FLAG_PRIVATE = 4;
    public static final int FLAG_PRESENTATION = 8;
    public static final int FLAG_ROUND = 16;

    public static final int FRAME_RATE_CATEGORY_NORMAL = 0;
    public static final int FRAME_RATE_CATEGORY_HIGH = 1;

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_OFF = 1;
    public static final int STATE_ON = 2;
    public static final int STATE_DOZE = 3;
    public static final int STATE_DOZE_SUSPEND = 4;
    public static final int STATE_VR = 5;
    public static final int STATE_ON_SUSPEND = 6;

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

    @Deprecated
    public int getOrientation()
    {
        return getRotation();
    }

    public float getRefreshRate()
    {
        return runtime.WindowBridge.refreshRate();
    }

    public float getSuggestedFrameRate(int category)
    {
        if (category != FRAME_RATE_CATEGORY_NORMAL && category != FRAME_RATE_CATEGORY_HIGH)
        {
            throw new IllegalArgumentException("Invalid FrameRateCategory provided");
        }

        return getRefreshRate();
    }

    public boolean hasArrSupport()
    {
        return false;
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
        return (long)(1000000000.0f / getRefreshRate());
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

    public DisplayShape getShape()
    {
        return DisplayShape.rectangle(mWidth, mHeight);
    }

    public DisplayCutout getCutout()
    {
        return null;
    }

    public RoundedCorner getRoundedCorner(int position)
    {
        if (position < RoundedCorner.POSITION_TOP_LEFT || position > RoundedCorner.POSITION_BOTTOM_LEFT)
        {
            throw new IllegalArgumentException("invalid position " + position);
        }

        return null;
    }

    public boolean isHdr()
    {
        return false;
    }

    public HdrCapabilities getHdrCapabilities()
    {
        return new HdrCapabilities(new int[0], HdrCapabilities.INVALID_LUMINANCE, HdrCapabilities.INVALID_LUMINANCE,
                                   HdrCapabilities.INVALID_LUMINANCE);
    }

    public boolean isHdrSdrRatioAvailable()
    {
        return false;
    }

    public float getHdrSdrRatio()
    {
        return 1.0f;
    }

    public float getHighestHdrSdrRatio()
    {
        return 1.0f;
    }

    public void registerHdrSdrRatioChangedListener(Executor executor, Consumer<Display> listener)
    {
        if (!isHdrSdrRatioAvailable()) throw new IllegalStateException("HDR/SDR ratio changed not available");
    }

    public void unregisterHdrSdrRatioChangedListener(Consumer<Display> listener)
    {
    }

    public boolean isWideColorGamut()
    {
        return false;
    }

    public ColorSpace getPreferredWideGamutColorSpace()
    {
        return isWideColorGamut() ? ColorSpace.get(ColorSpace.Named.DISPLAY_P3) : null;
    }

    public boolean isMinimalPostProcessingSupported()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "Display id " + mDisplayId + ": DisplayInfo{\"" + mName + "\", " + mWidth + " x " + mHeight + ", "
               + getRefreshRate() + " fps, density " + mDensityDpi + "}";
    }

    public static final class Mode implements Parcelable
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

        public int[] getSupportedHdrTypes()
        {
            return new int[0];
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Mode)) return false;

            Mode mode = (Mode)other;
            return mModeId == mode.mModeId && mWidth == mode.mWidth && mHeight == mode.mHeight
                   && Float.floatToIntBits(mRefreshRate) == Float.floatToIntBits(mode.mRefreshRate);
        }

        @Override
        public int hashCode()
        {
            int hash = 1;
            hash = hash * 17 + mModeId;
            hash = hash * 17 + mWidth;
            hash = hash * 17 + mHeight;
            hash = hash * 17 + Float.floatToIntBits(mRefreshRate);
            return hash;
        }

        @Override
        public String toString()
        {
            return "{id=" + mModeId + ", width=" + mWidth + ", height=" + mHeight + ", fps=" + mRefreshRate + "}";
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeInt(mModeId);
            out.writeInt(mWidth);
            out.writeInt(mHeight);
            out.writeFloat(mRefreshRate);
        }

        public static final Parcelable.Creator<Mode> CREATOR = new Parcelable.Creator<Mode>()
        {
            @Override
            public Mode createFromParcel(Parcel source)
            {
                return new Mode(source.readInt(), source.readInt(), source.readInt(), source.readFloat());
            }

            @Override
            public Mode[] newArray(int size)
            {
                return new Mode[size];
            }
        };
    }

    public static final class HdrCapabilities implements Parcelable
    {
        public static final float INVALID_LUMINANCE = -1.0f;

        public static final int HDR_TYPE_INVALID = -1;
        public static final int HDR_TYPE_DOLBY_VISION = 1;
        public static final int HDR_TYPE_HDR10 = 2;
        public static final int HDR_TYPE_HLG = 3;
        public static final int HDR_TYPE_HDR10_PLUS = 4;

        private final int[] mSupportedHdrTypes;
        private final float mMaxLuminance;
        private final float mMaxAverageLuminance;
        private final float mMinLuminance;

        HdrCapabilities(int[] supportedHdrTypes, float maxLuminance, float maxAverageLuminance, float minLuminance)
        {
            mSupportedHdrTypes = supportedHdrTypes;
            mMaxLuminance = maxLuminance;
            mMaxAverageLuminance = maxAverageLuminance;
            mMinLuminance = minLuminance;
        }

        @Deprecated
        public int[] getSupportedHdrTypes()
        {
            return mSupportedHdrTypes.clone();
        }

        public float getDesiredMaxLuminance()
        {
            return mMaxLuminance;
        }

        public float getDesiredMaxAverageLuminance()
        {
            return mMaxAverageLuminance;
        }

        public float getDesiredMinLuminance()
        {
            return mMinLuminance;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof HdrCapabilities)) return false;

            HdrCapabilities capabilities = (HdrCapabilities)other;
            return Arrays.equals(mSupportedHdrTypes, capabilities.mSupportedHdrTypes) && mMaxLuminance == capabilities.mMaxLuminance
                   && mMaxAverageLuminance == capabilities.mMaxAverageLuminance && mMinLuminance == capabilities.mMinLuminance;
        }

        @Override
        public int hashCode()
        {
            int hash = 23;
            hash = hash * 17 + Arrays.hashCode(mSupportedHdrTypes);
            hash = hash * 17 + Float.floatToIntBits(mMaxLuminance);
            hash = hash * 17 + Float.floatToIntBits(mMaxAverageLuminance);
            hash = hash * 17 + Float.floatToIntBits(mMinLuminance);
            return hash;
        }

        @Override
        public String toString()
        {
            return "HdrCapabilities{mSupportedHdrTypes=" + Arrays.toString(mSupportedHdrTypes) + ", mMaxLuminance=" + mMaxLuminance
                   + ", mMaxAverageLuminance=" + mMaxAverageLuminance + ", mMinLuminance=" + mMinLuminance + "}";
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeIntArray(mSupportedHdrTypes);
            out.writeFloat(mMaxLuminance);
            out.writeFloat(mMaxAverageLuminance);
            out.writeFloat(mMinLuminance);
        }

        public static final Parcelable.Creator<HdrCapabilities> CREATOR = new Parcelable.Creator<HdrCapabilities>()
        {
            @Override
            public HdrCapabilities createFromParcel(Parcel source)
            {
                int[] types = source.createIntArray();
                return new HdrCapabilities(types == null ? new int[0] : types, source.readFloat(), source.readFloat(), source.readFloat());
            }

            @Override
            public HdrCapabilities[] newArray(int size)
            {
                return new HdrCapabilities[size];
            }
        };
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

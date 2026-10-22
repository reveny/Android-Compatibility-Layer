package android.hardware;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.concurrent.atomic.AtomicLong;

public final class HardwareBuffer implements AutoCloseable, Parcelable
{
    public static final int RGBA_8888 = 1;
    public static final int RGBX_8888 = 2;
    public static final int RGB_888 = 3;
    public static final int RGB_565 = 4;
    public static final int RGBA_FP16 = 22;
    public static final int BLOB = 33;
    public static final int YCBCR_420_888 = 35;
    public static final int RGBA_1010102 = 43;
    public static final int D_16 = 48;
    public static final int D_24 = 49;
    public static final int DS_24UI8 = 50;
    public static final int D_FP32 = 51;
    public static final int DS_FP32UI8 = 52;
    public static final int S_UI8 = 53;
    public static final int YCBCR_P010 = 54;
    public static final int R_8 = 56;
    public static final int R_16 = 57;
    public static final int RG_1616 = 58;
    public static final int RGBA_10101010 = 59;
    public static final int YCBCR_P210 = 60;

    public static final long USAGE_CPU_READ_RARELY = 2L;
    public static final long USAGE_CPU_READ_OFTEN = 3L;
    public static final long USAGE_CPU_WRITE_RARELY = 32L;
    public static final long USAGE_CPU_WRITE_OFTEN = 48L;
    public static final long USAGE_GPU_SAMPLED_IMAGE = 256L;
    public static final long USAGE_GPU_COLOR_OUTPUT = 512L;
    public static final long USAGE_COMPOSER_OVERLAY = 2048L;
    public static final long USAGE_PROTECTED_CONTENT = 16384L;
    public static final long USAGE_VIDEO_ENCODE = 65536L;
    public static final long USAGE_SENSOR_DIRECT_DATA = 8388608L;
    public static final long USAGE_GPU_DATA_BUFFER = 16777216L;
    public static final long USAGE_GPU_CUBE_MAP = 33554432L;
    public static final long USAGE_GPU_MIPMAP_COMPLETE = 67108864L;
    public static final long USAGE_FRONT_BUFFER = 4294967296L;

    public static final Parcelable.Creator<HardwareBuffer> CREATOR = new Parcelable.Creator<HardwareBuffer>()
    {
        @Override
        public HardwareBuffer createFromParcel(Parcel source)
        {
            long id = source.readLong();
            int width = source.readInt();
            int height = source.readInt();
            int format = source.readInt();
            int layers = source.readInt();
            long usage = source.readLong();
            return new HardwareBuffer(id, width, height, format, layers, usage);
        }

        @Override
        public HardwareBuffer[] newArray(int size)
        {
            return new HardwareBuffer[size];
        }
    };

    private static final AtomicLong sNextId = new AtomicLong(1);

    private final long mId;
    private final int mWidth;
    private final int mHeight;
    private final int mFormat;
    private final int mLayers;
    private final long mUsage;
    private boolean mClosed;

    private HardwareBuffer(long id, int width, int height, int format, int layers, long usage)
    {
        mId = id;
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mLayers = layers;
        mUsage = usage;
    }

    public static HardwareBuffer create(int width, int height, int format, int layers, long usage)
    {
        if (!isSupportedFormat(format)) throw new IllegalArgumentException("Invalid pixel format " + format);
        if (width <= 0) throw new IllegalArgumentException("Invalid width " + width);
        if (height <= 0) throw new IllegalArgumentException("Invalid height " + height);
        if (layers <= 0) throw new IllegalArgumentException("Invalid layer count " + layers);
        if (format == BLOB && height != 1) throw new IllegalArgumentException("Height must be 1 when using the BLOB format");

        return new HardwareBuffer(sNextId.getAndIncrement(), width, height, format, layers, usage);
    }

    public static boolean isSupported(int width, int height, int format, int layers, long usage)
    {
        if (!isSupportedFormat(format)) throw new IllegalArgumentException("Invalid pixel format " + format);
        if (width <= 0) throw new IllegalArgumentException("Invalid width " + width);
        if (height <= 0) throw new IllegalArgumentException("Invalid height " + height);
        if (layers <= 0) throw new IllegalArgumentException("Invalid layer count " + layers);
        if (format == BLOB && height != 1) throw new IllegalArgumentException("Height must be 1 when using the BLOB format");

        return (usage & USAGE_PROTECTED_CONTENT) == 0 && (usage & USAGE_FRONT_BUFFER) == 0;
    }

    private static boolean isSupportedFormat(int format)
    {
        switch (format)
        {
            case RGBA_8888:
            case RGBX_8888:
            case RGB_888:
            case RGB_565:
            case RGBA_FP16:
            case BLOB:
            case YCBCR_420_888:
            case RGBA_1010102:
            case D_16:
            case D_24:
            case DS_24UI8:
            case D_FP32:
            case DS_FP32UI8:
            case S_UI8:
            case YCBCR_P010:
            case R_8:
            case R_16:
            case RG_1616:
            case RGBA_10101010:
            case YCBCR_P210:
                return true;
            default:
                return false;
        }
    }

    public int getWidth()
    {
        checkClosed("width");
        return mWidth;
    }

    public int getHeight()
    {
        checkClosed("height");
        return mHeight;
    }

    public int getFormat()
    {
        checkClosed("format");
        return mFormat;
    }

    public int getLayers()
    {
        checkClosed("layer count");
        return mLayers;
    }

    public long getUsage()
    {
        checkClosed("usage");
        return mUsage;
    }

    public long getId()
    {
        checkClosed("id");
        return mId;
    }

    private void checkClosed(String name)
    {
        if (mClosed) throw new IllegalStateException("This HardwareBuffer has been closed and its " + name + " cannot be obtained.");
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    public boolean isClosed()
    {
        return mClosed;
    }

    @Override
    public int describeContents()
    {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (mClosed) throw new IllegalStateException("This HardwareBuffer has been closed and cannot be written to a parcel.");

        dest.writeLong(mId);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeInt(mFormat);
        dest.writeInt(mLayers);
        dest.writeLong(mUsage);
    }
}

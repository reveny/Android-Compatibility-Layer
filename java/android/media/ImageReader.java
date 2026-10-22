package android.media;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

public class ImageReader implements AutoCloseable
{
    private static final int DATASPACE_SRGB = 142671872;
    private static final long USAGE_CPU_READ_OFTEN = 3L;
    private static final long USAGE_GPU_SAMPLED_IMAGE = 256L;

    private final int mWidth;
    private final int mHeight;
    private final int mFormat;
    private final int mHardwareBufferFormat;
    private final int mDataSpace;
    private final int mMaxImages;
    private final long mUsage;
    private Surface mSurface;
    private OnImageAvailableListener mListener;
    private Handler mListenerHandler;
    private boolean mClosed;

    ImageReader()
    {
        this(1, 1, PixelFormat.RGBA_8888, PixelFormat.RGBA_8888, DATASPACE_SRGB, 1, USAGE_CPU_READ_OFTEN);
    }

    private ImageReader(int width, int height, int format, int hardwareBufferFormat, int dataSpace, int maxImages, long usage)
    {
        if (width < 1 || height < 1) throw new IllegalArgumentException("The image dimensions must be positive");
        if (maxImages < 1) throw new IllegalArgumentException("Maximum outstanding image count must be at least 1");
        if (format == ImageFormat.NV21) throw new IllegalArgumentException("NV21 format is not supported");

        mWidth = width;
        mHeight = height;
        mFormat = format;
        mHardwareBufferFormat = hardwareBufferFormat;
        mDataSpace = dataSpace;
        mMaxImages = maxImages;
        mUsage = usage;
        mSurface = new Surface();
    }

    public static ImageReader newInstance(int width, int height, int format, int maxImages)
    {
        long usage = format == ImageFormat.PRIVATE ? USAGE_GPU_SAMPLED_IMAGE : USAGE_CPU_READ_OFTEN;

        return new ImageReader(width, height, format, format, DATASPACE_SRGB, maxImages, usage);
    }

    public static ImageReader newInstance(int width, int height, int format, int maxImages, long usage)
    {
        return new ImageReader(width, height, format, format, DATASPACE_SRGB, maxImages, usage);
    }

    public Image acquireLatestImage()
    {
        throwIfClosed();
        return null;
    }

    public Image acquireNextImage()
    {
        throwIfClosed();
        return null;
    }

    public void close()
    {
        if (mClosed) return;

        mClosed = true;
        mListener = null;
        mListenerHandler = null;

        if (mSurface != null) mSurface.release();

        mSurface = null;
    }

    public void discardFreeBuffers()
    {
        throwIfClosed();
    }

    public int getDataSpace()
    {
        return mDataSpace;
    }

    public int getHardwareBufferFormat()
    {
        return mHardwareBufferFormat;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getImageFormat()
    {
        return mFormat;
    }

    public int getMaxImages()
    {
        return mMaxImages;
    }

    public Surface getSurface()
    {
        return mSurface;
    }

    public long getUsage()
    {
        return mUsage;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public void setOnImageAvailableListener(ImageReader.OnImageAvailableListener listener, Handler handler)
    {
        throwIfClosed();

        if (listener == null)
        {
            mListener = null;
            mListenerHandler = null;
            return;
        }

        Looper looper = handler != null ? handler.getLooper() : Looper.myLooper();

        if (looper == null) throw new IllegalArgumentException("handler is null but the current thread is not a looper");

        mListener = listener;
        mListenerHandler = handler != null ? handler : new Handler(looper);
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            close();
        }
        finally
        {
            super.finalize();
        }
    }

    private void throwIfClosed()
    {
        if (mClosed) throw new IllegalStateException("ImageReader is closed");
    }

    public interface OnImageAvailableListener
    {
        void onImageAvailable(ImageReader reader);
    }

    public static final class Builder
    {
        private final int mWidth;
        private final int mHeight;
        private int mImageFormat = PixelFormat.RGBA_8888;
        private int mHardwareBufferFormat = PixelFormat.RGBA_8888;
        private int mDataSpace = DATASPACE_SRGB;
        private int mMaxImages = 1;
        private long mUsage = USAGE_CPU_READ_OFTEN;

        public Builder(int width, int height)
        {
            mWidth = width;
            mHeight = height;
        }

        public ImageReader build()
        {
            return new ImageReader(mWidth, mHeight, mImageFormat, mHardwareBufferFormat, mDataSpace, mMaxImages, mUsage);
        }

        public ImageReader.Builder setDefaultDataSpace(int dataSpace)
        {
            mDataSpace = dataSpace;
            return this;
        }

        public ImageReader.Builder setDefaultHardwareBufferFormat(int hardwareBufferFormat)
        {
            mHardwareBufferFormat = hardwareBufferFormat;
            return this;
        }

        public ImageReader.Builder setImageFormat(int imageFormat)
        {
            mImageFormat = imageFormat;
            mHardwareBufferFormat = imageFormat;
            return this;
        }

        public ImageReader.Builder setMaxImages(int maxImages)
        {
            mMaxImages = maxImages;
            return this;
        }

        public ImageReader.Builder setUsage(long usage)
        {
            mUsage = usage;
            return this;
        }
    }
}

package android.media;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class ImageWriter implements AutoCloseable
{
    private static final int DATASPACE_SRGB = 142671872;
    private static final long USAGE_CPU_WRITE_OFTEN = 48L;
    private static final int DEFAULT_SIZE = 1;

    private final Surface mSurface;
    private final int mMaxImages;
    private final int mFormat;
    private final int mHardwareBufferFormat;
    private final int mDataSpace;
    private final long mUsage;
    private final int mWidth;
    private final int mHeight;
    private final List<CpuImage> mDequeuedImages = new ArrayList<>();
    private final CpuImage.Owner mOwner = new CpuImage.Owner()
    {
        @Override
        public void onImageClosed(CpuImage image)
        {
            synchronized (mDequeuedImages)
            {
                mDequeuedImages.remove(image);
            }
        }
    };
    private OnImageReleasedListener mListener;
    private Handler mListenerHandler;
    private boolean mClosed;

    ImageWriter()
    {
        this(new Surface(), 1, PixelFormat.RGBA_8888, PixelFormat.RGBA_8888, DATASPACE_SRGB, USAGE_CPU_WRITE_OFTEN, DEFAULT_SIZE,
            DEFAULT_SIZE);
    }

    private ImageWriter(Surface surface, int maxImages, int format, int hardwareBufferFormat, int dataSpace, long usage, int width,
        int height)
    {
        if (surface == null) throw new IllegalArgumentException("Surface must not be null");
        if (maxImages < 1) throw new IllegalArgumentException("Maximum outstanding image count must be at least 1");
        if (format == ImageFormat.NV21) throw new IllegalArgumentException("NV21 format is not supported");

        mSurface = surface;
        mMaxImages = maxImages;
        mFormat = format;
        mHardwareBufferFormat = hardwareBufferFormat;
        mDataSpace = dataSpace;
        mUsage = usage;
        mWidth = Math.max(width, DEFAULT_SIZE);
        mHeight = Math.max(height, DEFAULT_SIZE);
    }

    public static ImageWriter newInstance(Surface surface, int maxImages)
    {
        return new ImageWriter(surface, maxImages, PixelFormat.RGBA_8888, PixelFormat.RGBA_8888, DATASPACE_SRGB, USAGE_CPU_WRITE_OFTEN,
            DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public static ImageWriter newInstance(Surface surface, int maxImages, int format)
    {
        if (!ImageFormat.isPublicFormat(format) && !CpuImage.isSupportedFormat(format))
        {
            throw new IllegalArgumentException("Invalid format is specified: " + format);
        }

        return new ImageWriter(surface, maxImages, format, format, DATASPACE_SRGB, USAGE_CPU_WRITE_OFTEN, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public void close()
    {
        if (mClosed) return;

        mClosed = true;
        mListener = null;
        mListenerHandler = null;

        List<CpuImage> outstanding;

        synchronized (mDequeuedImages)
        {
            outstanding = new ArrayList<>(mDequeuedImages);
            mDequeuedImages.clear();
        }

        for (CpuImage image : outstanding)
        {
            image.close();
        }
    }

    public Image dequeueInputImage()
    {
        throwIfClosed();

        synchronized (mDequeuedImages)
        {
            if (mDequeuedImages.size() >= mMaxImages)
            {
                throw new IllegalStateException("Already dequeued max number of Images " + mMaxImages);
            }

            CpuImage image = new CpuImage(mFormat, mWidth, mHeight, mOwner);
            image.setDataSpace(mDataSpace);
            mDequeuedImages.add(image);

            return image;
        }
    }

    public int getDataSpace()
    {
        return mDataSpace;
    }

    public int getFormat()
    {
        return mFormat;
    }

    public int getHardwareBufferFormat()
    {
        return mHardwareBufferFormat;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getMaxImages()
    {
        return mMaxImages;
    }

    public long getUsage()
    {
        return mUsage;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public void queueInputImage(Image image)
    {
        throwIfClosed();

        if (image == null) throw new IllegalArgumentException("image shouldn't be null");

        image.close();

        final OnImageReleasedListener listener = mListener;
        Handler handler = mListenerHandler;

        if (listener == null || handler == null) return;

        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (!mClosed) listener.onImageReleased(ImageWriter.this);
            }
        });
    }

    public void setOnImageReleasedListener(ImageWriter.OnImageReleasedListener listener, Handler handler)
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
        if (mClosed) throw new IllegalStateException("ImageWriter is closed");
    }

    public interface OnImageReleasedListener
    {
        void onImageReleased(ImageWriter writer);
    }

    public static final class Builder
    {
        private final Surface mSurface;
        private int mWidth = DEFAULT_SIZE;
        private int mHeight = DEFAULT_SIZE;
        private int mMaxImages = 1;
        private int mImageFormat = PixelFormat.RGBA_8888;
        private int mHardwareBufferFormat = PixelFormat.RGBA_8888;
        private int mDataSpace = DATASPACE_SRGB;
        private long mUsage = USAGE_CPU_WRITE_OFTEN;

        public Builder(Surface surface)
        {
            mSurface = surface;
        }

        public ImageWriter build()
        {
            return new ImageWriter(mSurface, mMaxImages, mImageFormat, mHardwareBufferFormat, mDataSpace, mUsage, mWidth, mHeight);
        }

        public ImageWriter.Builder setDataSpace(int dataSpace)
        {
            mDataSpace = dataSpace;
            return this;
        }

        public ImageWriter.Builder setHardwareBufferFormat(int hardwareBufferFormat)
        {
            mHardwareBufferFormat = hardwareBufferFormat;
            return this;
        }

        public ImageWriter.Builder setImageFormat(int imageFormat)
        {
            mImageFormat = imageFormat;
            mHardwareBufferFormat = imageFormat;
            return this;
        }

        public ImageWriter.Builder setMaxImages(int maxImages)
        {
            mMaxImages = maxImages;
            return this;
        }

        public ImageWriter.Builder setUsage(long usage)
        {
            mUsage = usage;
            return this;
        }

        public ImageWriter.Builder setWidthAndHeight(int width, int height)
        {
            mWidth = width;
            mHeight = height;
            return this;
        }
    }
}

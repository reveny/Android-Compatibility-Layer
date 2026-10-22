package android.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Parcel;
import android.os.Parcelable;

public class Surface implements Parcelable
{
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;

    public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0;
    public static final int FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1;
    public static final int FRAME_RATE_COMPATIBILITY_AT_LEAST = 2;

    public static final int CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS = 0;
    public static final int CHANGE_FRAME_RATE_ALWAYS = 1;

    public static final Parcelable.Creator<Surface> CREATOR = new Parcelable.Creator<Surface>()
    {
        @Override
        public Surface createFromParcel(Parcel source)
        {
            Surface surface = new Surface();
            surface.readFromParcel(source);
            return surface;
        }

        @Override
        public Surface[] newArray(int size)
        {
            return new Surface[size];
        }
    };

    private final Object mLock = new Object();

    private long mNativeWindow;
    private boolean mValid;
    private boolean mReleased;
    private String mName;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceControl mSurfaceControl;
    private BufferProducer mProducer;
    private Canvas mLockedCanvas;
    private Bitmap mLockedBuffer;
    private BufferProducer mLockedProducer;
    private float mFrameRate;
    private int mFrameRateCompatibility;
    private int mChangeFrameRateStrategy;

    public Surface()
    {
        mNativeWindow = 0;
        mValid = false;
    }

    public Surface(SurfaceTexture surfaceTexture)
    {
        if (surfaceTexture == null) throw new IllegalArgumentException("surfaceTexture must not be null");

        mSurfaceTexture = surfaceTexture;
        mName = surfaceTexture.toString();
        mValid = true;
    }

    public Surface(SurfaceControl from)
    {
        if (from == null) throw new IllegalArgumentException("from must not be null");
        if (!from.isValid()) throw new IllegalArgumentException("the SurfaceControl has already been released");

        mSurfaceControl = from;
        mName = from.getName();
        mValid = true;
        mProducer = from.bufferProducer();
    }

    Surface(long nativeWindow)
    {
        mNativeWindow = nativeWindow;
        mValid = nativeWindow != 0;
    }

    abstract static class BufferProducer
    {
        abstract Bitmap lockBuffer();

        abstract void unlockAndPost(Bitmap buffer);

        Bitmap peekFrontBuffer()
        {
            return null;
        }
    }

    Bitmap peekFrontBuffer()
    {
        BufferProducer producer;

        synchronized (mLock)
        {
            if (!isValid()) return null;
            producer = producerLocked();
        }

        return producer == null ? null : producer.peekFrontBuffer();
    }

    void setBufferProducer(BufferProducer producer)
    {
        synchronized (mLock)
        {
            mProducer = producer;
        }
    }

    public boolean isValid()
    {
        synchronized (mLock)
        {
            if (mReleased) return false;
            if (mSurfaceTexture != null) return !mSurfaceTexture.isReleased();
            if (mSurfaceControl != null) return mSurfaceControl.isValid();

            return mValid && mNativeWindow != 0;
        }
    }

    public void release()
    {
        synchronized (mLock)
        {
            mNativeWindow = 0;
            mValid = false;
            mReleased = mSurfaceTexture != null || mSurfaceControl != null;
            mLockedCanvas = null;
            mLockedBuffer = null;
            mLockedProducer = null;
        }
    }

    private BufferProducer producerLocked()
    {
        if (mProducer != null) return mProducer;
        if (mSurfaceTexture == null) return null;

        BufferProducer consumer = TextureView.producerFor(mSurfaceTexture);
        if (consumer != null) return consumer;

        if (mTextureProducer == null) mTextureProducer = new TextureBufferProducer(mSurfaceTexture);
        return mTextureProducer;
    }

    private TextureBufferProducer mTextureProducer;

    private static final class TextureBufferProducer extends BufferProducer
    {
        private final SurfaceTexture mTexture;
        private Bitmap mBuffer;
        private boolean mPosted;

        TextureBufferProducer(SurfaceTexture texture)
        {
            mTexture = texture;
        }

        private int defaultSize(String field)
        {
            try
            {
                java.lang.reflect.Field size = SurfaceTexture.class.getDeclaredField(field);
                size.setAccessible(true);
                return size.getInt(mTexture);
            }
            catch (ReflectiveOperationException | RuntimeException unavailable)
            {
                return 0;
            }
        }

        @Override
        synchronized Bitmap lockBuffer()
        {
            int width = defaultSize("mDefaultWidth");
            int height = defaultSize("mDefaultHeight");
            if (width <= 0 || height <= 0) return null;

            if (mBuffer == null || mBuffer.getWidth() != width || mBuffer.getHeight() != height)
            {
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }

            return mBuffer;
        }

        @Override
        void unlockAndPost(Bitmap buffer)
        {
            synchronized (this)
            {
                mPosted = true;
            }

            try
            {
                java.lang.reflect.Method notify = SurfaceTexture.class.getDeclaredMethod("postFrameAvailable");
                notify.setAccessible(true);
                notify.invoke(mTexture);
            }
            catch (ReflectiveOperationException | RuntimeException unavailable)
            {
                android.util.Log.w("Surface", "the SurfaceTexture could not be told about a new frame", unavailable);
            }
        }

        @Override
        synchronized Bitmap peekFrontBuffer()
        {
            return mPosted ? mBuffer : null;
        }
    }

    public Canvas lockCanvas(Rect inOutDirty) throws OutOfResourcesException, IllegalArgumentException
    {
        synchronized (mLock)
        {
            if (!isValid()) throw new IllegalStateException("Surface has already been released.");
            if (mLockedCanvas != null) throw new IllegalArgumentException("Surface was already locked");

            BufferProducer producer = producerLocked();
            if (producer == null) throw new OutOfResourcesException("this surface has no consumer that accepts software buffers");

            Bitmap buffer = producer.lockBuffer();
            if (buffer == null) throw new OutOfResourcesException("no buffer is available for this surface");

            Canvas canvas = new Canvas(buffer);

            if (inOutDirty != null)
            {
                if (!inOutDirty.intersect(0, 0, buffer.getWidth(), buffer.getHeight())) inOutDirty.setEmpty();
                canvas.clipRect(inOutDirty);
            }

            mLockedCanvas = canvas;
            mLockedBuffer = buffer;
            mLockedProducer = producer;
            return canvas;
        }
    }

    public Canvas lockHardwareCanvas()
    {
        return lockCanvas(null);
    }

    public void unlockCanvasAndPost(Canvas canvas)
    {
        BufferProducer producer;
        Bitmap buffer;

        synchronized (mLock)
        {
            if (mLockedCanvas == null) throw new IllegalStateException("Surface was not locked");
            if (canvas != mLockedCanvas)
            {
                throw new IllegalArgumentException("canvas object must be the same instance that was previously returned by lockCanvas");
            }

            producer = mLockedProducer;
            buffer = mLockedBuffer;
            mLockedCanvas = null;
            mLockedBuffer = null;
            mLockedProducer = null;
        }

        if (producer != null) producer.unlockAndPost(buffer);
    }

    @Deprecated
    public void unlockCanvas(Canvas canvas)
    {
        throw new UnsupportedOperationException();
    }

    public void setFrameRate(float frameRate, int compatibility)
    {
        setFrameRate(frameRate, compatibility, CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
    }

    public void setFrameRate(float frameRate, int compatibility, int changeFrameRateStrategy)
    {
        if (Float.isNaN(frameRate) || Float.isInfinite(frameRate) || frameRate < 0)
        {
            throw new IllegalArgumentException("invalid frame rate " + frameRate);
        }

        if (compatibility < FRAME_RATE_COMPATIBILITY_DEFAULT || compatibility > FRAME_RATE_COMPATIBILITY_AT_LEAST)
        {
            throw new IllegalArgumentException("invalid compatibility " + compatibility);
        }

        if (changeFrameRateStrategy != CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS && changeFrameRateStrategy != CHANGE_FRAME_RATE_ALWAYS)
        {
            throw new IllegalArgumentException("invalid change frame rate strategy " + changeFrameRateStrategy);
        }

        synchronized (mLock)
        {
            if (!isValid()) throw new IllegalStateException("Surface has already been released.");

            mFrameRate = frameRate;
            mFrameRateCompatibility = compatibility;
            mChangeFrameRateStrategy = changeFrameRateStrategy;
        }
    }

    public void clearFrameRate()
    {
        synchronized (mLock)
        {
            if (!isValid()) throw new IllegalStateException("Surface has already been released.");

            mFrameRate = 0;
            mFrameRateCompatibility = FRAME_RATE_COMPATIBILITY_DEFAULT;
            mChangeFrameRateStrategy = CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS;
        }
    }

    long getNativeWindow()
    {
        return mNativeWindow;
    }

    void attach(long nativeWindow)
    {
        synchronized (mLock)
        {
            mNativeWindow = nativeWindow;
            mValid = nativeWindow != 0;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        if (out == null) throw new IllegalArgumentException("dest must not be null");

        synchronized (mLock)
        {
            out.writeString(mName);
            out.writeLong(mNativeWindow);
            out.writeInt(mValid ? 1 : 0);
        }

        if ((flags & PARCELABLE_WRITE_RETURN_VALUE) != 0) release();
    }

    public void readFromParcel(Parcel source)
    {
        if (source == null) throw new IllegalArgumentException("source must not be null");

        synchronized (mLock)
        {
            mName = source.readString();
            mNativeWindow = source.readLong();
            mValid = source.readInt() != 0;
            mReleased = false;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            release();
        }
        finally
        {
            super.finalize();
        }
    }

    @Override
    public String toString()
    {
        return "Surface(name=" + mName + ")/@0x" + Integer.toHexString(System.identityHashCode(this));
    }

    public static class OutOfResourcesException extends RuntimeException
    {
        public OutOfResourcesException()
        {
        }

        public OutOfResourcesException(String name)
        {
            super(name);
        }
    }
}

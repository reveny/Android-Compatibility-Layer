package android.graphics;

public class SurfaceTexture
{
    private final boolean mSingleBufferMode;

    private OnFrameAvailableListener mListener;
    private android.os.Handler mHandler;
    private int mTextureName;
    private boolean mAttached;
    private boolean mReleased;
    private int mDefaultWidth;
    private int mDefaultHeight;
    private long mTimestamp;

    public SurfaceTexture(int textureName)
    {
        this(textureName, false);
    }

    public SurfaceTexture(int textureName, boolean singleBufferMode)
    {
        mTextureName = textureName;
        mSingleBufferMode = singleBufferMode;
        mAttached = true;
    }

    public SurfaceTexture(boolean singleBufferMode)
    {
        mSingleBufferMode = singleBufferMode;
        mAttached = false;
    }

    public void setOnFrameAvailableListener(OnFrameAvailableListener listener)
    {
        setOnFrameAvailableListener(listener, null);
    }

    public void setOnFrameAvailableListener(OnFrameAvailableListener listener, android.os.Handler handler)
    {
        mListener = listener;
        mHandler = handler;
    }

    public void setDefaultBufferSize(int width, int height)
    {
        mDefaultWidth = width;
        mDefaultHeight = height;
    }

    public void updateTexImage()
    {
        if (mReleased) throw new IllegalStateException("SurfaceTexture has been released");
        if (!mAttached) throw new IllegalStateException("Unable to update texture contents (see logcat for details)");

        mTimestamp = System.nanoTime();
    }

    public void releaseTexImage()
    {
        if (mReleased) throw new IllegalStateException("SurfaceTexture has been released");
    }

    public void detachFromGLContext()
    {
        if (!mAttached) throw new RuntimeException("Error during detachFromGLContext (see logcat for details)");

        mAttached = false;
    }

    public void attachToGLContext(int textureName)
    {
        if (mAttached) throw new RuntimeException("Error during attachToGLContext (see logcat for details)");

        mTextureName = textureName;
        mAttached = true;
    }

    public void getTransformMatrix(float[] matrix)
    {
        if (matrix.length != 16) throw new IllegalArgumentException();

        java.util.Arrays.fill(matrix, 0);
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
    }

    public long getTimestamp()
    {
        return mTimestamp;
    }

    public int getDataSpace()
    {
        return 0;
    }

    public void release()
    {
        mReleased = true;
        mListener = null;
        mHandler = null;
    }

    public boolean isReleased()
    {
        return mReleased;
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

    void postFrameAvailable()
    {
        final OnFrameAvailableListener listener = mListener;
        if (listener == null) return;

        if (mHandler == null)
        {
            listener.onFrameAvailable(this);
            return;
        }

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onFrameAvailable(SurfaceTexture.this);
            }
        });
    }

    public interface OnFrameAvailableListener
    {
        void onFrameAvailable(SurfaceTexture surfaceTexture);
    }

    public static class OutOfResourcesException extends Exception
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

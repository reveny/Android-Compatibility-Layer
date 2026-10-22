package android.media;

import android.graphics.Rect;

import java.nio.ByteBuffer;

public abstract class Image implements AutoCloseable
{
    private Rect mCropRect;
    private int mDataSpace;
    private long mTimestamp;

    Image()
    {
    }

    public abstract void close();

    public Rect getCropRect()
    {
        throwIfImageIsInvalid();

        if (mCropRect == null) return new Rect(0, 0, getWidth(), getHeight());

        return new Rect(mCropRect);
    }

    public int getDataSpace()
    {
        throwIfImageIsInvalid();
        return mDataSpace;
    }

    public abstract int getFormat();

    public abstract int getHeight();

    public abstract Image.Plane[] getPlanes();

    public abstract long getTimestamp();

    public abstract int getWidth();

    public void setCropRect(Rect cropRect)
    {
        throwIfImageIsInvalid();

        if (cropRect == null)
        {
            mCropRect = null;
            return;
        }

        Rect bounded = new Rect(cropRect);

        if (!bounded.intersect(0, 0, getWidth(), getHeight())) bounded.setEmpty();

        mCropRect = bounded;
    }

    public void setDataSpace(int dataSpace)
    {
        throwIfImageIsInvalid();
        mDataSpace = dataSpace;
    }

    public void setTimestamp(long timestamp)
    {
        throwIfImageIsInvalid();
        mTimestamp = timestamp;
    }

    long getStoredTimestamp()
    {
        return mTimestamp;
    }

    boolean isImageValid()
    {
        return true;
    }

    void throwIfImageIsInvalid()
    {
        if (!isImageValid()) throw new IllegalStateException("Image is already closed");
    }

    public static abstract class Plane
    {
        Plane()
        {
        }

        public abstract ByteBuffer getBuffer();

        public abstract int getPixelStride();

        public abstract int getRowStride();
    }
}

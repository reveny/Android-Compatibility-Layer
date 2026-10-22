package android.graphics;

import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BitmapRegionDecoder
{
    private final Object mLock = new Object();
    private final byte[] mData;
    private final int mWidth;
    private final int mHeight;
    private Bitmap mDecoded;
    private boolean mRecycled;

    private BitmapRegionDecoder(byte[] data) throws IOException
    {
        int[] size = new int[2];
        if (data == null || data.length == 0 || !Bitmap.measureBytes(data, 0, data.length, size))
            throw new IOException("Image format not supported");

        mData = data;
        mWidth = size[0];
        mHeight = size[1];
    }

    public static BitmapRegionDecoder newInstance(byte[] data, int offset, int length, boolean isShareable) throws IOException
    {
        return newInstance(data, offset, length);
    }

    public static BitmapRegionDecoder newInstance(byte[] data, int offset, int length) throws IOException
    {
        if ((offset | length) < 0 || data.length < offset + length) throw new ArrayIndexOutOfBoundsException();

        byte[] copy = new byte[length];
        System.arraycopy(data, offset, copy, 0, length);
        return new BitmapRegionDecoder(copy);
    }

    public static BitmapRegionDecoder newInstance(FileDescriptor descriptor, boolean isShareable) throws IOException
    {
        return newInstance(new FileInputStream(descriptor));
    }

    public static BitmapRegionDecoder newInstance(ParcelFileDescriptor descriptor) throws IOException
    {
        return newInstance(descriptor.getFileDescriptor(), false);
    }

    public static BitmapRegionDecoder newInstance(InputStream stream, boolean isShareable) throws IOException
    {
        return newInstance(stream);
    }

    public static BitmapRegionDecoder newInstance(InputStream stream) throws IOException
    {
        if (stream == null) throw new NullPointerException("stream");

        byte[] data = BitmapFactory.readFully(stream, null);
        if (data == null) throw new IOException("cannot read the image stream");

        return new BitmapRegionDecoder(data);
    }

    public static BitmapRegionDecoder newInstance(String pathName, boolean isShareable) throws IOException
    {
        return newInstance(pathName);
    }

    public static BitmapRegionDecoder newInstance(String pathName) throws IOException
    {
        try (InputStream stream = new FileInputStream(pathName))
        {
            return newInstance(stream);
        }
    }

    public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options)
    {
        synchronized (mLock)
        {
            checkRecycled("decodeRegion called on recycled region decoder");

            if (rect.right <= 0 || rect.bottom <= 0 || rect.left >= getWidth() || rect.top >= getHeight())
                throw new IllegalArgumentException("rectangle is outside the image");

            if (options != null && options.mCancel) return null;

            if (mDecoded == null)
            {
                mDecoded = Bitmap.adopt(Bitmap.decodeBytes(mData, 0, mData.length));
                if (mDecoded == null) return null;
            }

            int left = Math.max(0, rect.left);
            int top = Math.max(0, rect.top);
            int right = Math.min(mDecoded.getWidth(), rect.right);
            int bottom = Math.min(mDecoded.getHeight(), rect.bottom);
            int sampleSize = options == null || options.inSampleSize <= 1 ? 1 : options.inSampleSize;

            int width = Math.max(1, rect.width() / sampleSize);
            int height = Math.max(1, rect.height() / sampleSize);

            if (options != null)
            {
                options.outWidth = width;
                options.outHeight = height;
                options.outMimeType = null;
                options.outConfig = Bitmap.Config.ARGB_8888;
                options.outColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
                if (options.inJustDecodeBounds) return null;
            }

            Bitmap region = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(region);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);

            float scaleX = width / (float)rect.width();
            float scaleY = height / (float)rect.height();
            RectF destination = new RectF((left - rect.left) * scaleX, (top - rect.top) * scaleY, (right - rect.left) * scaleX,
                                          (bottom - rect.top) * scaleY);
            canvas.drawBitmap(mDecoded, new Rect(left, top, right, bottom), destination, paint);

            if (options != null)
            {
                region.setConfigQuietly(options.inPreferredConfig == Bitmap.Config.RGB_565 && !mDecoded.hasAlpha()
                                            ? Bitmap.Config.RGB_565
                                            : Bitmap.Config.ARGB_8888);
                options.outConfig = region.getConfig();
                if (!options.inPremultiplied) region.setPremultiplied(false);
                region.setMutable(options.inMutable || options.inBitmap != null);
            }

            return region;
        }
    }

    public int getWidth()
    {
        synchronized (mLock)
        {
            checkRecycled("getWidth called on recycled region decoder");
            return mWidth;
        }
    }

    public int getHeight()
    {
        synchronized (mLock)
        {
            checkRecycled("getHeight called on recycled region decoder");
            return mHeight;
        }
    }

    public void recycle()
    {
        synchronized (mLock)
        {
            if (mRecycled) return;

            if (mDecoded != null) mDecoded.recycle();
            mDecoded = null;
            mRecycled = true;
        }
    }

    public boolean isRecycled()
    {
        return mRecycled;
    }

    private void checkRecycled(String message)
    {
        if (mRecycled) throw new IllegalStateException(message);
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            recycle();
        }
        finally
        {
            super.finalize();
        }
    }
}

package android.graphics;

public final class Bitmap
{
    private long mNativeBitmap;
    private boolean mRecycled;
    private Config mConfig = Config.ARGB_8888;
    private int mDensity = 160;

    private Bitmap(long nativeBitmap)
    {
        mNativeBitmap = nativeBitmap;
    }

    static Bitmap adopt(long nativeBitmap)
    {
        return nativeBitmap == 0 ? null : new Bitmap(nativeBitmap);
    }

    public static Bitmap createBitmap(int width, int height, Config config)
    {
        long handle = nativeCreate(width, height);
        if (handle == 0) throw new IllegalArgumentException("cannot make a " + width + "x" + height + " bitmap");

        Bitmap bitmap = new Bitmap(handle);
        bitmap.mConfig = config == null ? Config.ARGB_8888 : config;

        return bitmap;
    }

    public static Bitmap createBitmap(Picture source)
    {
        if (source == null) throw new IllegalArgumentException("the picture is required");

        Bitmap recorded = source.recorded();

        if (recorded != null) return recorded;

        return createBitmap(Math.max(1, source.getWidth()), Math.max(1, source.getHeight()), Config.ARGB_8888);
    }

    public static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha)
    {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha, ColorSpace colorSpace)
    {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(android.util.DisplayMetrics metrics, int width, int height, Config config)
    {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(android.util.DisplayMetrics metrics, int width, int height, Config config,
                                      boolean hasAlpha)
    {
        return createBitmap(width, height, config);
    }

    public static Bitmap createBitmap(android.util.DisplayMetrics metrics, int width, int height, Config config,
                                      boolean hasAlpha, ColorSpace colorSpace)
    {
        return createBitmap(width, height, config);
    }

    public ColorSpace getColorSpace()
    {
        return ColorSpace.get(ColorSpace.Named.SRGB);
    }

    public void setColorSpace(ColorSpace colorSpace)
    {
    }

    public void setHasAlpha(boolean hasAlpha)
    {
    }

    public boolean isPremultiplied()
    {
        return true;
    }

    public void setPremultiplied(boolean premultiplied)
    {
    }

    public void prepareToDraw()
    {
    }

    public Bitmap copy(Config config, boolean mutable)
    {
        return createScaledBitmap(this, getWidth(), getHeight(), false);
    }

    public static Bitmap createScaledBitmap(Bitmap source, int width, int height, boolean filter)
    {
        if (source == null) return null;

        Bitmap scaled = createBitmap(width, height, source.mConfig);
        Canvas canvas = new Canvas(scaled);

        Paint paint = new Paint();
        paint.setFilterBitmap(filter);

        canvas.drawBitmap(source, new Rect(0, 0, source.getWidth(), source.getHeight()),
                          new Rect(0, 0, width, height), paint);

        return scaled;
    }

    long nativeHandle()
    {
        return mNativeBitmap;
    }

    public int getWidth()
    {
        return nativeWidth(mNativeBitmap);
    }

    public int getHeight()
    {
        return nativeHeight(mNativeBitmap);
    }

    public boolean hasAlpha()
    {
        return nativeHasAlpha(mNativeBitmap);
    }

    public Config getConfig()
    {
        return mConfig;
    }

    public int getDensity()
    {
        return mDensity;
    }

    public void setDensity(int density)
    {
        mDensity = density;
    }

    public int getRowBytes()
    {
        return getWidth() * 4;
    }

    public int getByteCount()
    {
        return getRowBytes() * getHeight();
    }

    public int getAllocationByteCount()
    {
        return getByteCount();
    }

    public boolean isRecycled()
    {
        return mRecycled;
    }

    public boolean isMutable()
    {
        return true;
    }

    public int getPixel(int x, int y)
    {
        checkAlive();
        return nativePixel(mNativeBitmap, x, y);
    }

    public void setPixel(int x, int y, int color)
    {
        checkAlive();
        nativeSetPixel(mNativeBitmap, x, y, color);
    }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height)
    {
        checkAlive();
        nativeReadPixels(mNativeBitmap, pixels, offset, stride, x, y, width, height);
    }

    public void eraseColor(int color)
    {
        checkAlive();
        nativeErase(mNativeBitmap, color);
    }

    public void recycle()
    {
        if (mRecycled || mNativeBitmap == 0) return;

        nativeDestroy(mNativeBitmap);
        mNativeBitmap = 0;
        mRecycled = true;
    }

    private void checkAlive()
    {
        if (mRecycled || mNativeBitmap == 0) throw new IllegalStateException("this bitmap was recycled");
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

    private static native long nativeDecode(byte[] data, int offset, int length);

    private static native boolean nativeMeasure(byte[] data, int offset, int length, int[] into);

    private static native long nativeCreate(int width, int height);

    private static native void nativeDestroy(long bitmap);

    private static native int nativeWidth(long bitmap);

    private static native int nativeHeight(long bitmap);

    private static native boolean nativeHasAlpha(long bitmap);

    private static native int nativePixel(long bitmap, int x, int y);

    private static native void nativeSetPixel(long bitmap, int x, int y, int color);

    private static native void nativeReadPixels(long bitmap, int[] into, int offset, int stride, int x, int y, int width,
                                                int height);

    private static native void nativeErase(long bitmap, int color);

    private static native long nativeOpenCanvas(long bitmap);

    private static native void nativeCloseCanvas(long canvas);

    long openCanvas()
    {
        checkAlive();
        return nativeOpenCanvas(mNativeBitmap);
    }

    static void closeCanvas(long canvas)
    {
        if (canvas != 0) nativeCloseCanvas(canvas);
    }

    static long decodeBytes(byte[] data, int offset, int length)
    {
        return nativeDecode(data, offset, length);
    }

    static boolean measureBytes(byte[] data, int offset, int length, int[] into)
    {
        return nativeMeasure(data, offset, length, into);
    }

    public enum Config
    {
        ALPHA_8,
        RGB_565,
        ARGB_4444,
        ARGB_8888,
        RGBA_F16,
        HARDWARE
    }
}

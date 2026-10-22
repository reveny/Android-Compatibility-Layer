package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class Bitmap implements Parcelable
{
    public static final int DENSITY_NONE = 0;

    public static final Parcelable.Creator<Bitmap> CREATOR = new Parcelable.Creator<Bitmap>()
    {
        @Override
        public Bitmap createFromParcel(Parcel source)
        {
            boolean mutable = source.readBoolean();
            int width = source.readInt();
            int height = source.readInt();
            int config = source.readInt();
            int density = source.readInt();
            boolean premultiplied = source.readBoolean();
            boolean hasAlpha = source.readBoolean();
            byte[] chunk = source.createByteArray();
            int[] pixels = source.createIntArray();
            Gainmap gainmap = source.readTypedObject(Gainmap.CREATOR);

            Config[] configs = Config.values();
            Bitmap bitmap = createBitmap(width, height, config >= 0 && config < configs.length ? configs[config] : Config.ARGB_8888);
            if (pixels != null && pixels.length >= width * height) bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            bitmap.mMutable = mutable;
            bitmap.mDensity = density;
            bitmap.mPremultiplied = premultiplied;
            bitmap.mHasAlpha = hasAlpha;
            bitmap.mNinePatchChunk = chunk;
            bitmap.mGainmap = gainmap;
            return bitmap;
        }

        @Override
        public Bitmap[] newArray(int size)
        {
            return new Bitmap[size];
        }
    };

    private static final AtomicInteger sGenerationIds = new AtomicInteger(1);

    private long mNativeBitmap;
    private boolean mRecycled;
    private Config mConfig = Config.ARGB_8888;
    private int mDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
    private boolean mMutable = true;
    private boolean mPremultiplied = true;
    private boolean mHasAlpha = true;
    private boolean mHasMipMap;
    private byte[] mNinePatchChunk;
    private Gainmap mGainmap;
    private ColorSpace mColorSpace;
    private int mGenerationId = sGenerationIds.getAndIncrement();
    private int mAllocationByteCount = -1;
    private List<Long> mRetiredBitmaps;
    private android.hardware.HardwareBuffer mHardwareBuffer;

    private Bitmap(long nativeBitmap)
    {
        mNativeBitmap = nativeBitmap;
        mHasAlpha = nativeHasAlpha(nativeBitmap);
    }

    static Bitmap adopt(long nativeBitmap)
    {
        return nativeBitmap == 0 ? null : new Bitmap(nativeBitmap);
    }

    public static Bitmap createBitmap(int width, int height, Config config)
    {
        return createBitmap(width, height, config, true);
    }

    public static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha)
    {
        return createBitmap(width, height, config, hasAlpha, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public static Bitmap createBitmap(int width, int height, Config config, boolean hasAlpha, ColorSpace colorSpace)
    {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width and height must be > 0");
        if (config == Config.HARDWARE) throw new IllegalArgumentException("can't create mutable bitmap with Config.HARDWARE");
        if (colorSpace == null && config != Config.ALPHA_8) throw new IllegalArgumentException("can't create bitmap without a color space");

        long handle = nativeCreate(width, height);
        if (handle == 0) throw new IllegalArgumentException("cannot make a " + width + "x" + height + " bitmap");

        Bitmap bitmap = new Bitmap(handle);
        bitmap.mConfig = config == null ? Config.ARGB_8888 : config;
        bitmap.mColorSpace = colorSpace;
        bitmap.mHasAlpha = true;

        if (!hasAlpha && config != Config.ALPHA_8)
        {
            bitmap.eraseColor(0xFF000000);
            bitmap.mHasAlpha = false;
        }

        return bitmap;
    }

    public static Bitmap createBitmap(DisplayMetrics display, int width, int height, Config config)
    {
        return createBitmap(display, width, height, config, true);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int width, int height, Config config, boolean hasAlpha)
    {
        return createBitmap(display, width, height, config, hasAlpha, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public static Bitmap createBitmap(DisplayMetrics display, int width, int height, Config config, boolean hasAlpha,
                                      ColorSpace colorSpace)
    {
        Bitmap bitmap = createBitmap(width, height, config, hasAlpha, colorSpace);
        if (display != null) bitmap.mDensity = display.densityDpi;
        return bitmap;
    }

    public static Bitmap createBitmap(Bitmap source)
    {
        return createBitmap(source, 0, 0, source.getWidth(), source.getHeight());
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height)
    {
        return createBitmap(source, x, y, width, height, null, false);
    }

    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter)
    {
        checkXYSign(x, y);
        checkWidthHeight(width, height);
        if (x + width > source.getWidth()) throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        if (y + height > source.getHeight()) throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        if (source.isRecycled()) throw new IllegalArgumentException("cannot use a recycled source in createBitmap");

        if (!source.isMutable() && x == 0 && y == 0 && width == source.getWidth() && height == source.getHeight()
            && (m == null || m.isIdentity()))
        {
            return source;
        }

        Config config = source.mConfig == Config.HARDWARE ? Config.ARGB_8888 : source.mConfig;
        Bitmap bitmap;

        if (m == null || m.isIdentity())
        {
            bitmap = createBitmap(width, height, config, source.mHasAlpha, source.getColorSpace());

            int[] pixels = new int[width * height];
            source.getPixels(pixels, 0, width, x, y, width, height);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }
        else
        {
            RectF bounds = new RectF(0, 0, width, height);
            m.mapRect(bounds);

            int targetWidth = Math.max(1, Math.round(bounds.width()));
            int targetHeight = Math.max(1, Math.round(bounds.height()));

            bitmap = createBitmap(targetWidth, targetHeight, config, true, source.getColorSpace());

            Paint paint = new Paint();
            paint.setFilterBitmap(filter);
            if (!m.rectStaysRect()) paint.setAntiAlias(true);

            Canvas canvas = new Canvas(bitmap);
            canvas.translate(-bounds.left, -bounds.top);
            canvas.concat(m);
            canvas.drawBitmap(source, new Rect(x, y, x + width, y + height), new RectF(0, 0, width, height), paint);

            bitmap.mHasAlpha = source.mHasAlpha || !m.rectStaysRect();
        }

        bitmap.mDensity = source.mDensity;
        bitmap.mPremultiplied = source.mPremultiplied;
        bitmap.mMutable = false;
        return bitmap;
    }

    public static Bitmap createBitmap(int[] colors, int offset, int stride, int width, int height, Config config)
    {
        return createBitmap(null, colors, offset, stride, width, height, config);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int[] colors, int offset, int stride, int width, int height,
                                      Config config)
    {
        checkWidthHeight(width, height);
        if (Math.abs(stride) < width) throw new IllegalArgumentException("abs(stride) must be >= width");

        int lastScanline = offset + (height - 1) * stride;
        int length = colors.length;
        if (offset < 0 || (offset + width > length) || lastScanline < 0 || (lastScanline + width > length))
            throw new ArrayIndexOutOfBoundsException();

        Bitmap bitmap = createBitmap(display, width, height, config);
        bitmap.setPixels(colors, offset, stride, 0, 0, width, height);
        bitmap.mMutable = false;
        return bitmap;
    }

    public static Bitmap createBitmap(int[] colors, int width, int height, Config config)
    {
        return createBitmap(null, colors, 0, width, width, height, config);
    }

    public static Bitmap createBitmap(DisplayMetrics display, int[] colors, int width, int height, Config config)
    {
        return createBitmap(display, colors, 0, width, width, height, config);
    }

    public static Bitmap createBitmap(Picture source)
    {
        if (source == null) throw new IllegalArgumentException("the picture is required");

        Bitmap recorded = source.recorded();

        if (recorded != null) return recorded;

        return createBitmap(Math.max(1, source.getWidth()), Math.max(1, source.getHeight()), Config.ARGB_8888);
    }

    public static Bitmap createBitmap(Picture source, int width, int height, Config config)
    {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width & height must be > 0");
        if (config == null) throw new IllegalArgumentException("Config must not be null");

        Bitmap bitmap = createBitmap(width, height, config == Config.HARDWARE ? Config.ARGB_8888 : config);
        Canvas canvas = new Canvas(bitmap);

        if (source.getWidth() != width || source.getHeight() != height)
        {
            canvas.scale(width / (float)Math.max(1, source.getWidth()), height / (float)Math.max(1, source.getHeight()));
        }

        source.draw(canvas);
        bitmap.mConfig = config;
        return bitmap;
    }

    public static Bitmap createScaledBitmap(Bitmap source, int width, int height, boolean filter)
    {
        if (source == null) return null;
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width and height must be > 0");

        Bitmap scaled = createBitmap(width, height, source.mConfig == Config.HARDWARE ? Config.ARGB_8888 : source.mConfig);
        Canvas canvas = new Canvas(scaled);

        Paint paint = new Paint();
        paint.setFilterBitmap(filter);

        canvas.drawBitmap(source, new Rect(0, 0, source.getWidth(), source.getHeight()),
                          new Rect(0, 0, width, height), paint);

        scaled.mDensity = source.mDensity;
        scaled.mHasAlpha = source.mHasAlpha;
        scaled.mPremultiplied = source.mPremultiplied;
        return scaled;
    }

    public static Bitmap wrapHardwareBuffer(android.hardware.HardwareBuffer hardwareBuffer, ColorSpace colorSpace)
    {
        if ((hardwareBuffer.getUsage() & android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE) == 0)
            throw new IllegalArgumentException("usage flags must contain USAGE_GPU_SAMPLED_IMAGE.");

        switch (hardwareBuffer.getFormat())
        {
            case android.hardware.HardwareBuffer.RGBA_8888:
            case android.hardware.HardwareBuffer.RGBX_8888:
            case android.hardware.HardwareBuffer.RGB_888:
            case android.hardware.HardwareBuffer.RGB_565:
            case android.hardware.HardwareBuffer.RGBA_FP16:
            case android.hardware.HardwareBuffer.RGBA_1010102:
                break;
            default:
                return null;
        }

        Bitmap bitmap = createBitmap(hardwareBuffer.getWidth(), hardwareBuffer.getHeight(), Config.ARGB_8888, true,
                                     colorSpace == null ? ColorSpace.get(ColorSpace.Named.SRGB) : colorSpace);
        bitmap.mConfig = Config.HARDWARE;
        bitmap.mMutable = false;
        bitmap.mHardwareBuffer = hardwareBuffer;
        return bitmap;
    }

    public android.hardware.HardwareBuffer getHardwareBuffer()
    {
        checkRecycled("Can't getHardwareBuffer from a recycled bitmap");
        if (mConfig != Config.HARDWARE) throw new IllegalStateException("This bitmap must have Config.HARDWARE");

        if (mHardwareBuffer == null || mHardwareBuffer.isClosed())
        {
            mHardwareBuffer = android.hardware.HardwareBuffer.create(getWidth(), getHeight(), android.hardware.HardwareBuffer.RGBA_8888,
                                                                     1, android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE);
        }

        return mHardwareBuffer;
    }

    private static void checkXYSign(int x, int y)
    {
        if (x < 0) throw new IllegalArgumentException("x must be >= 0");
        if (y < 0) throw new IllegalArgumentException("y must be >= 0");
    }

    private static void checkWidthHeight(int width, int height)
    {
        if (width <= 0) throw new IllegalArgumentException("width must be > 0");
        if (height <= 0) throw new IllegalArgumentException("height must be > 0");
    }

    public Bitmap asShared()
    {
        return this;
    }

    public ColorSpace getColorSpace()
    {
        if (mConfig == Config.ALPHA_8) return null;

        return mColorSpace == null ? ColorSpace.get(ColorSpace.Named.SRGB) : mColorSpace;
    }

    public void setColorSpace(ColorSpace colorSpace)
    {
        checkRecycled("setColorSpace called on a recycled bitmap");
        if (colorSpace == null) throw new IllegalArgumentException("The colorSpace cannot be set to null");
        if (mConfig == Config.ALPHA_8) throw new IllegalArgumentException("Cannot set a ColorSpace on ALPHA_8");

        mColorSpace = colorSpace;
    }

    public void setHasAlpha(boolean hasAlpha)
    {
        checkRecycled("setHasAlpha called on a recycled bitmap");
        mHasAlpha = hasAlpha;
    }

    public boolean hasAlpha()
    {
        checkRecycled("hasAlpha called on a recycled bitmap");
        return mHasAlpha;
    }

    public boolean isPremultiplied()
    {
        checkRecycled("isPremultiplied called on a recycled bitmap");
        return mPremultiplied && mConfig != Config.RGB_565 && mConfig != Config.ALPHA_8 && mHasAlpha;
    }

    public void setPremultiplied(boolean premultiplied)
    {
        checkRecycled("setPremultiplied called on a recycled bitmap");
        mPremultiplied = premultiplied;
    }

    public boolean hasMipMap()
    {
        checkRecycled("hasMipMap called on a recycled bitmap");
        return mHasMipMap;
    }

    public void setHasMipMap(boolean hasMipMap)
    {
        checkRecycled("setHasMipMap called on a recycled bitmap");
        mHasMipMap = hasMipMap;
    }

    public void prepareToDraw()
    {
    }

    public byte[] getNinePatchChunk()
    {
        return mNinePatchChunk;
    }

    void setNinePatchChunk(byte[] chunk)
    {
        mNinePatchChunk = chunk;
    }

    void setMutable(boolean mutable)
    {
        mMutable = mutable;
    }

    void setConfigQuietly(Config config)
    {
        if (config != null) mConfig = config;
    }

    public Gainmap getGainmap()
    {
        checkRecycled("Bitmap is recycled");
        return mGainmap;
    }

    public void setGainmap(Gainmap gainmap)
    {
        checkRecycled("Bitmap is recycled");
        mGainmap = gainmap;
    }

    public boolean hasGainmap()
    {
        checkRecycled("Bitmap is recycled");
        return mGainmap != null;
    }

    public int getGenerationId()
    {
        if (mRecycled) return 0;

        return mGenerationId;
    }

    private void touch()
    {
        mGenerationId = sGenerationIds.getAndIncrement();
    }

    public Bitmap copy(Config config, boolean isMutable)
    {
        checkRecycled("Can't copy a recycled bitmap");
        if (config == Config.HARDWARE && isMutable) throw new IllegalArgumentException("Hardware bitmaps are always immutable");

        int width = getWidth();
        int height = getHeight();

        Bitmap copy = createBitmap(width, height, config == Config.HARDWARE || config == null ? Config.ARGB_8888 : config);

        int[] pixels = new int[width * height];
        getPixels(pixels, 0, width, 0, 0, width, height);
        if (config == Config.ALPHA_8)
        {
            for (int index = 0; index < pixels.length; index++) pixels[index] &= 0xFF000000;
        }
        else if (config == Config.RGB_565)
        {
            for (int index = 0; index < pixels.length; index++) pixels[index] = premultiply(pixels[index]) | 0xFF000000;
        }

        copy.setPixels(pixels, 0, width, 0, 0, width, height);

        copy.mConfig = config == null ? mConfig : config;
        copy.mDensity = mDensity;
        copy.mHasAlpha = config == Config.RGB_565 ? false : mHasAlpha;
        copy.mPremultiplied = mPremultiplied;
        copy.mColorSpace = mColorSpace;
        copy.mMutable = isMutable;
        return copy;
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

    public void setWidth(int width)
    {
        reconfigure(width, getHeight(), getConfig());
    }

    public void setHeight(int height)
    {
        reconfigure(getWidth(), height, getConfig());
    }

    public void setConfig(Config config)
    {
        reconfigure(getWidth(), getHeight(), config);
    }

    public void reconfigure(int width, int height, Config config)
    {
        checkRecycled("Can't call reconfigure() on a recycled bitmap");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width and height must be > 0");
        if (!isMutable()) throw new IllegalStateException("only mutable bitmaps may be reconfigured");
        if (config == null) config = mConfig;

        int allocation = getAllocationByteCount();
        if ((long)width * height * bytesPerPixel(config) > allocation)
            throw new IllegalArgumentException("Bitmap not large enough to support new configuration");

        if (width != getWidth() || height != getHeight())
        {
            long replacement = nativeCreate(width, height);
            if (replacement == 0) throw new IllegalArgumentException("cannot make a " + width + "x" + height + " bitmap");

            if (mRetiredBitmaps == null) mRetiredBitmaps = new ArrayList<>();
            mRetiredBitmaps.add(mNativeBitmap);
            mNativeBitmap = replacement;
        }

        mAllocationByteCount = allocation;
        mConfig = config;
        mHasAlpha = true;
        mNinePatchChunk = null;
        touch();
    }

    public Config getConfig()
    {
        return mConfig;
    }

    public int getDensity()
    {
        if (mRecycled) android.util.Log.w("Bitmap", "Called getDensity() on a recycle()'d bitmap! This is undefined behavior!");
        return mDensity;
    }

    public void setDensity(int density)
    {
        mDensity = density;
    }

    public int getScaledWidth(Canvas canvas)
    {
        return scaleFromDensity(getWidth(), mDensity, canvas.getDensity());
    }

    public int getScaledHeight(Canvas canvas)
    {
        return scaleFromDensity(getHeight(), mDensity, canvas.getDensity());
    }

    public int getScaledWidth(DisplayMetrics metrics)
    {
        return scaleFromDensity(getWidth(), mDensity, metrics.densityDpi);
    }

    public int getScaledHeight(DisplayMetrics metrics)
    {
        return scaleFromDensity(getHeight(), mDensity, metrics.densityDpi);
    }

    public int getScaledWidth(int targetDensity)
    {
        return scaleFromDensity(getWidth(), mDensity, targetDensity);
    }

    public int getScaledHeight(int targetDensity)
    {
        return scaleFromDensity(getHeight(), mDensity, targetDensity);
    }

    static int scaleFromDensity(int size, int sourceDensity, int targetDensity)
    {
        if (sourceDensity == DENSITY_NONE || targetDensity == DENSITY_NONE || sourceDensity == targetDensity) return size;

        return ((size * targetDensity) + (sourceDensity >> 1)) / sourceDensity;
    }

    private static int bytesPerPixel(Config config)
    {
        if (config == null) return 4;

        switch (config)
        {
            case ALPHA_8:
                return 1;
            case RGB_565:
            case ARGB_4444:
                return 2;
            case RGBA_F16:
                return 8;
            default:
                return 4;
        }
    }

    public int getRowBytes()
    {
        if (mRecycled) android.util.Log.w("Bitmap", "Called getRowBytes() on a recycle()'d bitmap! This is undefined behavior!");
        return getWidth() * bytesPerPixel(mConfig);
    }

    public int getByteCount()
    {
        if (mRecycled) return 0;

        return getRowBytes() * getHeight();
    }

    public int getAllocationByteCount()
    {
        if (mRecycled) return 0;

        int current = getByteCount();
        if (mAllocationByteCount < current) mAllocationByteCount = current;
        return mAllocationByteCount;
    }

    public boolean isRecycled()
    {
        return mRecycled;
    }

    public boolean isMutable()
    {
        return mMutable;
    }

    public int getPixel(int x, int y)
    {
        checkRecycled("Can't call getPixel() on a recycled bitmap");
        checkPixelAccess(x, y);
        return nativePixel(mNativeBitmap, x, y);
    }

    public Color getColor(int x, int y)
    {
        checkRecycled("Can't call getColor() on a recycled bitmap");
        checkPixelAccess(x, y);
        return Color.valueOf(nativePixel(mNativeBitmap, x, y));
    }

    private void checkPixelAccess(int x, int y)
    {
        if (x < 0) throw new IllegalArgumentException("x must be >= 0");
        if (y < 0) throw new IllegalArgumentException("y must be >= 0");
        if (x >= getWidth()) throw new IllegalArgumentException("x must be < bitmap.width()");
        if (y >= getHeight()) throw new IllegalArgumentException("y must be < bitmap.height()");
    }

    public void setPixel(int x, int y, int color)
    {
        checkRecycled("Can't call setPixel() on a recycled bitmap");
        checkPixelAccess(x, y);
        nativeSetPixel(mNativeBitmap, x, y, color);
        touch();
    }

    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height)
    {
        checkRecycled("Can't call getPixels() on a recycled bitmap");
        if (width == 0 || height == 0) return;
        checkPixelsAccess(x, y, width, height, offset, stride, pixels);
        nativeReadPixels(mNativeBitmap, pixels, offset, stride, x, y, width, height);
    }

    public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height)
    {
        checkRecycled("Can't call setPixels() on a recycled bitmap");
        if (pixels == null) throw new NullPointerException("pixels");
        if (width == 0 || height == 0) return;
        checkPixelsAccess(x, y, width, height, offset, stride, pixels);

        nativeWritePixels(mNativeBitmap, pixels, offset, stride, x, y, width, height);
        touch();
    }

    private void checkPixelsAccess(int x, int y, int width, int height, int offset, int stride, int[] pixels)
    {
        checkXYSign(x, y);
        if (width < 0) throw new IllegalArgumentException("width must be >= 0");
        if (height < 0) throw new IllegalArgumentException("height must be >= 0");
        if (x + width > getWidth()) throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        if (y + height > getHeight()) throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        if (Math.abs(stride) < width) throw new IllegalArgumentException("abs(stride) must be >= width");

        int lastScanline = offset + (height - 1) * stride;
        int length = pixels.length;
        if (offset < 0 || (offset + width > length) || lastScanline < 0 || (lastScanline + width > length))
            throw new ArrayIndexOutOfBoundsException();
    }

    public void eraseColor(int color)
    {
        checkRecycled("Can't erase a recycled bitmap");
        nativeErase(mNativeBitmap, premultiply(color));
        touch();
    }

    public void eraseColor(long color)
    {
        eraseColor(Color.toArgb(color));
    }

    public Bitmap extractAlpha()
    {
        return extractAlpha(null, null);
    }

    public Bitmap extractAlpha(Paint paint, int[] offsetXY)
    {
        checkRecycled("Can't extractAlpha on a recycled bitmap");

        int width = getWidth();
        int height = getHeight();
        int[] pixels = new int[width * height];
        getPixels(pixels, 0, width, 0, 0, width, height);

        for (int index = 0; index < pixels.length; index++) pixels[index] &= 0xFF000000;

        Bitmap alpha = createBitmap(width, height, Config.ALPHA_8);
        alpha.setPixels(pixels, 0, width, 0, 0, width, height);
        alpha.mDensity = mDensity;

        if (offsetXY != null && offsetXY.length >= 2)
        {
            offsetXY[0] = 0;
            offsetXY[1] = 0;
        }

        return alpha;
    }

    public boolean sameAs(Bitmap other)
    {
        checkRecycled("Can't call sameAs on a recycled bitmap!");
        if (this == other) return true;
        if (other == null) return false;
        if (other.isRecycled()) throw new IllegalArgumentException("Can't compare to a recycled bitmap!");

        int width = getWidth();
        int height = getHeight();
        if (width != other.getWidth() || height != other.getHeight() || mConfig != other.mConfig) return false;

        int[] mine = new int[width];
        int[] theirs = new int[width];

        for (int row = 0; row < height; row++)
        {
            getPixels(mine, 0, width, 0, row, width, 1);
            other.getPixels(theirs, 0, width, 0, row, width, 1);
            if (!java.util.Arrays.equals(mine, theirs)) return false;
        }

        return true;
    }

    public void copyPixelsToBuffer(Buffer destination)
    {
        checkRecycled("copyPixelsToBuffer called on recycled bitmap");

        int elements = destination.remaining();
        int shift = elementShift(destination);
        long bufferSize = (long)elements << shift;
        int byteCount = getByteCount();

        if (bufferSize < byteCount) throw new RuntimeException("Buffer not large enough for pixels");

        byte[] raw = encodeRaw();

        if (destination instanceof ByteBuffer)
        {
            ((ByteBuffer)destination).put(raw, 0, byteCount);
        }
        else if (destination instanceof ShortBuffer)
        {
            ShortBuffer shorts = (ShortBuffer)destination;
            for (int index = 0; index < byteCount; index += 2) shorts.put((short)((raw[index] & 0xFF) | ((raw[index + 1] & 0xFF) << 8)));
        }
        else
        {
            IntBuffer ints = (IntBuffer)destination;
            for (int index = 0; index < byteCount; index += 4) ints.put(readLittleInt(raw, index));
        }
    }

    public void copyPixelsFromBuffer(Buffer source)
    {
        checkRecycled("copyPixelsFromBuffer called on recycled bitmap");

        int elements = source.remaining();
        int shift = elementShift(source);
        long bufferBytes = (long)elements << shift;
        int bitmapBytes = getByteCount();

        if (bufferBytes < bitmapBytes) throw new RuntimeException("Buffer not large enough for pixels");

        byte[] raw = new byte[bitmapBytes];

        if (source instanceof ByteBuffer)
        {
            ((ByteBuffer)source).get(raw);
        }
        else if (source instanceof ShortBuffer)
        {
            ShortBuffer shorts = (ShortBuffer)source;
            for (int index = 0; index < bitmapBytes; index += 2)
            {
                short value = shorts.get();
                raw[index] = (byte)value;
                raw[index + 1] = (byte)(value >> 8);
            }
        }
        else
        {
            IntBuffer ints = (IntBuffer)source;
            for (int index = 0; index < bitmapBytes; index += 4) writeLittleInt(raw, index, ints.get());
        }

        decodeRaw(raw);
        touch();
    }

    private static int elementShift(Buffer buffer)
    {
        if (buffer instanceof ByteBuffer) return 0;
        if (buffer instanceof ShortBuffer) return 1;
        if (buffer instanceof IntBuffer) return 2;

        throw new RuntimeException("unsupported Buffer subclass");
    }

    private byte[] encodeRaw()
    {
        int width = getWidth();
        int height = getHeight();
        int bytes = bytesPerPixel(mConfig);
        int[] pixels = new int[width * height];
        getPixels(pixels, 0, width, 0, 0, width, height);

        boolean premultiplied = isPremultiplied();
        byte[] raw = new byte[pixels.length * bytes];

        for (int index = 0; index < pixels.length; index++)
        {
            int color = premultiplied ? premultiply(pixels[index]) : pixels[index];
            int alpha = color >>> 24;
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            int at = index * bytes;

            switch (mConfig)
            {
                case ALPHA_8:
                    raw[at] = (byte)alpha;
                    break;
                case RGB_565:
                {
                    int packed = ((red >> 3) << 11) | ((green >> 2) << 5) | (blue >> 3);
                    raw[at] = (byte)packed;
                    raw[at + 1] = (byte)(packed >> 8);
                    break;
                }
                case ARGB_4444:
                {
                    int packed = ((red >> 4) << 12) | ((green >> 4) << 8) | ((blue >> 4) << 4) | (alpha >> 4);
                    raw[at] = (byte)packed;
                    raw[at + 1] = (byte)(packed >> 8);
                    break;
                }
                case RGBA_F16:
                {
                    writeHalf(raw, at, red / 255.0f);
                    writeHalf(raw, at + 2, green / 255.0f);
                    writeHalf(raw, at + 4, blue / 255.0f);
                    writeHalf(raw, at + 6, alpha / 255.0f);
                    break;
                }
                case RGBA_1010102:
                {
                    int packed = ((red * 1023 + 127) / 255) | (((green * 1023 + 127) / 255) << 10) | (((blue * 1023 + 127) / 255) << 20)
                                 | (((alpha * 3 + 127) / 255) << 30);
                    writeLittleInt(raw, at, packed);
                    break;
                }
                default:
                    raw[at] = (byte)red;
                    raw[at + 1] = (byte)green;
                    raw[at + 2] = (byte)blue;
                    raw[at + 3] = (byte)alpha;
                    break;
            }
        }

        return raw;
    }

    private void decodeRaw(byte[] raw)
    {
        int width = getWidth();
        int height = getHeight();
        int bytes = bytesPerPixel(mConfig);
        int[] pixels = new int[width * height];
        boolean premultiplied = isPremultiplied();

        for (int index = 0; index < pixels.length; index++)
        {
            int at = index * bytes;
            int alpha;
            int red;
            int green;
            int blue;

            switch (mConfig)
            {
                case ALPHA_8:
                    alpha = raw[at] & 0xFF;
                    red = 0;
                    green = 0;
                    blue = 0;
                    break;
                case RGB_565:
                {
                    int packed = (raw[at] & 0xFF) | ((raw[at + 1] & 0xFF) << 8);
                    red = ((packed >> 11) & 0x1F) * 255 / 31;
                    green = ((packed >> 5) & 0x3F) * 255 / 63;
                    blue = (packed & 0x1F) * 255 / 31;
                    alpha = 0xFF;
                    break;
                }
                case ARGB_4444:
                {
                    int packed = (raw[at] & 0xFF) | ((raw[at + 1] & 0xFF) << 8);
                    red = ((packed >> 12) & 0xF) * 17;
                    green = ((packed >> 8) & 0xF) * 17;
                    blue = ((packed >> 4) & 0xF) * 17;
                    alpha = (packed & 0xF) * 17;
                    break;
                }
                case RGBA_F16:
                    red = unitToByte(readHalf(raw, at));
                    green = unitToByte(readHalf(raw, at + 2));
                    blue = unitToByte(readHalf(raw, at + 4));
                    alpha = unitToByte(readHalf(raw, at + 6));
                    break;
                case RGBA_1010102:
                {
                    int packed = readLittleInt(raw, at);
                    red = (packed & 0x3FF) * 255 / 1023;
                    green = ((packed >> 10) & 0x3FF) * 255 / 1023;
                    blue = ((packed >> 20) & 0x3FF) * 255 / 1023;
                    alpha = ((packed >>> 30) & 0x3) * 85;
                    break;
                }
                default:
                    red = raw[at] & 0xFF;
                    green = raw[at + 1] & 0xFF;
                    blue = raw[at + 2] & 0xFF;
                    alpha = raw[at + 3] & 0xFF;
                    break;
            }

            int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
            pixels[index] = premultiplied ? unpremultiply(color) : color;
        }

        nativeWritePixels(mNativeBitmap, pixels, 0, width, 0, 0, width, height);
    }

    private static int unitToByte(float value)
    {
        int scaled = Math.round(value * 255.0f);
        return scaled < 0 ? 0 : scaled > 255 ? 255 : scaled;
    }

    private static void writeHalf(byte[] raw, int at, float value)
    {
        int bits = Float.floatToIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exponent = ((bits >>> 23) & 0xFF) - 127 + 15;
        int mantissa = bits & 0x7FFFFF;
        int half;

        if (exponent <= 0)
            half = sign;
        else if (exponent >= 31)
            half = sign | 0x7C00;
        else
            half = sign | (exponent << 10) | (mantissa >> 13);

        raw[at] = (byte)half;
        raw[at + 1] = (byte)(half >> 8);
    }

    private static float readHalf(byte[] raw, int at)
    {
        int half = (raw[at] & 0xFF) | ((raw[at + 1] & 0xFF) << 8);
        int sign = (half & 0x8000) << 16;
        int exponent = (half >> 10) & 0x1F;
        int mantissa = half & 0x3FF;

        if (exponent == 0) return (sign != 0 ? -1 : 1) * mantissa / 16777216.0f;
        if (exponent == 31) return Float.intBitsToFloat(sign | 0x7F800000 | (mantissa << 13));

        return Float.intBitsToFloat(sign | ((exponent - 15 + 127) << 23) | (mantissa << 13));
    }

    private static int readLittleInt(byte[] raw, int at)
    {
        return (raw[at] & 0xFF) | ((raw[at + 1] & 0xFF) << 8) | ((raw[at + 2] & 0xFF) << 16) | ((raw[at + 3] & 0xFF) << 24);
    }

    private static void writeLittleInt(byte[] raw, int at, int value)
    {
        raw[at] = (byte)value;
        raw[at + 1] = (byte)(value >> 8);
        raw[at + 2] = (byte)(value >> 16);
        raw[at + 3] = (byte)(value >> 24);
    }

    static int premultiply(int color)
    {
        int alpha = color >>> 24;
        if (alpha == 0xFF) return color;
        if (alpha == 0) return 0;

        int red = (((color >> 16) & 0xFF) * alpha + 127) / 255;
        int green = (((color >> 8) & 0xFF) * alpha + 127) / 255;
        int blue = ((color & 0xFF) * alpha + 127) / 255;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int unpremultiply(int color)
    {
        int alpha = color >>> 24;
        if (alpha == 0xFF || alpha == 0) return alpha == 0 ? 0 : color;

        int red = Math.min(255, (((color >> 16) & 0xFF) * 255 + alpha / 2) / alpha);
        int green = Math.min(255, (((color >> 8) & 0xFF) * 255 + alpha / 2) / alpha);
        int blue = Math.min(255, ((color & 0xFF) * 255 + alpha / 2) / alpha);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public boolean compress(CompressFormat format, int quality, OutputStream stream)
    {
        checkRecycled("Can't compress a recycled bitmap");
        if (stream == null) throw new NullPointerException();
        if (quality < 0 || quality > 100) throw new IllegalArgumentException("quality must be 0..100");
        if (format == null) throw new NullPointerException("format");

        int width = getWidth();
        int height = getHeight();
        int[] pixels = new int[width * height];
        getPixels(pixels, 0, width, 0, 0, width, height);

        try
        {
            if (format == CompressFormat.JPEG || (format == CompressFormat.WEBP_LOSSY && !mHasAlpha))
                JpegEncoder.encode(pixels, width, height, quality, stream);
            else
                PngEncoder.encode(pixels, width, height, mHasAlpha, quality, stream);

            stream.flush();
            return true;
        }
        catch (IOException failure)
        {
            return false;
        }
    }

    public void recycle()
    {
        if (mRecycled || mNativeBitmap == 0) return;

        nativeDestroy(mNativeBitmap);
        mNativeBitmap = 0;
        mRecycled = true;
        mNinePatchChunk = null;

        if (mRetiredBitmaps != null)
        {
            for (long retired : mRetiredBitmaps) nativeDestroy(retired);
            mRetiredBitmaps = null;
        }
    }

    private void checkAlive()
    {
        if (mRecycled || mNativeBitmap == 0) throw new IllegalStateException("this bitmap was recycled");
    }

    private void checkRecycled(String message)
    {
        if (mRecycled || mNativeBitmap == 0) throw new IllegalStateException(message);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        checkRecycled("Can't parcel a recycled bitmap");

        int width = getWidth();
        int height = getHeight();
        int[] pixels = new int[width * height];
        getPixels(pixels, 0, width, 0, 0, width, height);

        out.writeBoolean(mMutable);
        out.writeInt(width);
        out.writeInt(height);
        out.writeInt(mConfig.ordinal());
        out.writeInt(mDensity);
        out.writeBoolean(mPremultiplied);
        out.writeBoolean(mHasAlpha);
        out.writeByteArray(mNinePatchChunk);
        out.writeIntArray(pixels);
        out.writeTypedObject(mGainmap, flags);
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

    private static native void nativeWritePixels(long bitmap, int[] from, int offset, int stride, int x, int y, int width,
                                                 int height);

    private static native void nativeErase(long bitmap, int color);

    private static native long nativeOpenCanvas(long bitmap);

    private static native void nativeCloseCanvas(long canvas);

    long openCanvas()
    {
        checkAlive();
        touch();
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
        HARDWARE,
        RGBA_1010102
    }

    public enum CompressFormat
    {
        JPEG,
        PNG,
        WEBP,
        WEBP_LOSSY,
        WEBP_LOSSLESS
    }

    private static final class PngEncoder
    {
        private static final byte[] SIGNATURE = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

        static void encode(int[] pixels, int width, int height, boolean alpha, int quality, OutputStream stream) throws IOException
        {
            stream.write(SIGNATURE);

            ByteArrayOutputStream header = new ByteArrayOutputStream();
            writeInt(header, width);
            writeInt(header, height);
            header.write(8);
            header.write(alpha ? 6 : 2);
            header.write(0);
            header.write(0);
            header.write(0);
            writeChunk(stream, "IHDR", header.toByteArray());

            int channels = alpha ? 4 : 3;
            int rowBytes = width * channels;
            byte[] previous = new byte[rowBytes];
            byte[] current = new byte[rowBytes];
            byte[] filtered = new byte[rowBytes + 1];

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(quality >= 90 ? Deflater.BEST_COMPRESSION : Deflater.DEFAULT_COMPRESSION);

            try (DeflaterOutputStream deflated = new DeflaterOutputStream(compressed, deflater, 65536))
            {
                for (int row = 0; row < height; row++)
                {
                    int base = row * width;

                    for (int column = 0; column < width; column++)
                    {
                        int color = pixels[base + column];
                        int at = column * channels;
                        current[at] = (byte)(color >> 16);
                        current[at + 1] = (byte)(color >> 8);
                        current[at + 2] = (byte)color;
                        if (alpha) current[at + 3] = (byte)(color >>> 24);
                    }

                    filtered[0] = 4;
                    for (int index = 0; index < rowBytes; index++)
                    {
                        int left = index >= channels ? current[index - channels] & 0xFF : 0;
                        int up = previous[index] & 0xFF;
                        int upLeft = index >= channels ? previous[index - channels] & 0xFF : 0;
                        filtered[index + 1] = (byte)((current[index] & 0xFF) - paeth(left, up, upLeft));
                    }

                    deflated.write(filtered);

                    byte[] swap = previous;
                    previous = current;
                    current = swap;
                }
            }
            finally
            {
                deflater.end();
            }

            writeChunk(stream, "IDAT", compressed.toByteArray());
            writeChunk(stream, "IEND", new byte[0]);
        }

        private static int paeth(int left, int up, int upLeft)
        {
            int estimate = left + up - upLeft;
            int distanceLeft = Math.abs(estimate - left);
            int distanceUp = Math.abs(estimate - up);
            int distanceUpLeft = Math.abs(estimate - upLeft);

            if (distanceLeft <= distanceUp && distanceLeft <= distanceUpLeft) return left;
            if (distanceUp <= distanceUpLeft) return up;
            return upLeft;
        }

        private static void writeChunk(OutputStream stream, String type, byte[] data) throws IOException
        {
            byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

            ByteArrayOutputStream length = new ByteArrayOutputStream(4);
            writeInt(length, data.length);
            stream.write(length.toByteArray());
            stream.write(typeBytes);
            stream.write(data);

            CRC32 crc = new CRC32();
            crc.update(typeBytes);
            crc.update(data);

            ByteArrayOutputStream checksum = new ByteArrayOutputStream(4);
            writeInt(checksum, (int)crc.getValue());
            stream.write(checksum.toByteArray());
        }

        private static void writeInt(ByteArrayOutputStream stream, int value)
        {
            stream.write(value >>> 24);
            stream.write(value >>> 16);
            stream.write(value >>> 8);
            stream.write(value);
        }
    }

    private static final class JpegEncoder
    {
        private static final int[] ZIGZAG = {0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27,
                                             20, 13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51,
                                             58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63};

        private static final int[] LUMINANCE_QUANTIZATION = {16, 11, 10, 16, 24, 40, 51, 61, 12, 12, 14, 19, 26, 58, 60, 55, 14, 13, 16,
                                                             24, 40, 57, 69, 56, 14, 17, 22, 29, 51, 87, 80, 62, 18, 22, 37, 56, 68,
                                                             109, 103, 77, 24, 35, 55, 64, 81, 104, 113, 92, 49, 64, 78, 87, 103, 121,
                                                             120, 101, 72, 92, 95, 98, 112, 100, 103, 99};

        private static final int[] CHROMINANCE_QUANTIZATION = {17, 18, 24, 47, 99, 99, 99, 99, 18, 21, 26, 66, 99, 99, 99, 99, 24, 26,
                                                               56, 99, 99, 99, 99, 99, 47, 66, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
                                                               99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
                                                               99, 99, 99, 99, 99, 99, 99, 99, 99, 99};

        private static final int[] DC_LUMINANCE_BITS = {0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0};
        private static final int[] DC_LUMINANCE_VALUES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        private static final int[] DC_CHROMINANCE_BITS = {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        private static final int[] DC_CHROMINANCE_VALUES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

        private static final int[] AC_LUMINANCE_BITS = {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d};
        private static final int[] AC_LUMINANCE_VALUES = {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, 0x81,
            0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0, 0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18,
            0x19, 0x1a, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x73, 0x74, 0x75,
            0x76, 0x77, 0x78, 0x79, 0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99,
            0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
            0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2, 0xe3, 0xe4, 0xe5,
            0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa};

        private static final int[] AC_CHROMINANCE_BITS = {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77};
        private static final int[] AC_CHROMINANCE_VALUES = {
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22, 0x32, 0x81, 0x08,
            0x14, 0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0, 0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25,
            0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47,
            0x48, 0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x73, 0x74,
            0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97,
            0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba,
            0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe2, 0xe3, 0xe4,
            0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa};

        private static final double[][] COSINES = cosines();

        private final OutputStream mStream;
        private int mBitBuffer;
        private int mBitCount;

        private JpegEncoder(OutputStream stream)
        {
            mStream = stream;
        }

        private static double[][] cosines()
        {
            double[][] table = new double[8][8];

            for (int frequency = 0; frequency < 8; frequency++)
            {
                double scale = frequency == 0 ? Math.sqrt(0.125) : 0.5;
                for (int position = 0; position < 8; position++)
                {
                    table[frequency][position] = scale * Math.cos((2 * position + 1) * frequency * Math.PI / 16.0);
                }
            }

            return table;
        }

        static void encode(int[] pixels, int width, int height, int quality, OutputStream stream) throws IOException
        {
            new JpegEncoder(stream).write(pixels, width, height, quality);
        }

        private static int[] scaleTable(int[] base, int quality)
        {
            int clamped = Math.max(1, Math.min(100, quality));
            int scale = clamped < 50 ? 5000 / clamped : 200 - clamped * 2;
            int[] table = new int[64];

            for (int index = 0; index < 64; index++) table[index] = Math.max(1, Math.min(255, (base[index] * scale + 50) / 100));

            return table;
        }

        private static int[][] buildCodes(int[] bits, int[] values)
        {
            int[] codes = new int[256];
            int[] lengths = new int[256];
            int code = 0;
            int at = 0;

            for (int length = 1; length <= 16; length++)
            {
                for (int count = 0; count < bits[length - 1]; count++)
                {
                    codes[values[at]] = code;
                    lengths[values[at]] = length;
                    at++;
                    code++;
                }

                code <<= 1;
            }

            return new int[][] {codes, lengths};
        }

        private void write(int[] pixels, int width, int height, int quality) throws IOException
        {
            int[] luminance = scaleTable(LUMINANCE_QUANTIZATION, quality);
            int[] chrominance = scaleTable(CHROMINANCE_QUANTIZATION, quality);

            writeMarker(0xD8);

            writeMarker(0xE0);
            writeShort(16);
            mStream.write(new byte[] {'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1, 0, 1, 0, 0});

            writeMarker(0xDB);
            writeShort(2 + 2 * 65);
            mStream.write(0);
            for (int index = 0; index < 64; index++) mStream.write(luminance[ZIGZAG[index]]);
            mStream.write(1);
            for (int index = 0; index < 64; index++) mStream.write(chrominance[ZIGZAG[index]]);

            writeMarker(0xC0);
            writeShort(17);
            mStream.write(8);
            writeShort(height);
            writeShort(width);
            mStream.write(3);
            mStream.write(new byte[] {1, 0x11, 0, 2, 0x11, 1, 3, 0x11, 1});

            writeMarker(0xC4);
            writeShort(2 + (1 + 16 + 12) * 2 + (1 + 16 + 162) * 2);
            writeHuffmanTable(0x00, DC_LUMINANCE_BITS, DC_LUMINANCE_VALUES);
            writeHuffmanTable(0x10, AC_LUMINANCE_BITS, AC_LUMINANCE_VALUES);
            writeHuffmanTable(0x01, DC_CHROMINANCE_BITS, DC_CHROMINANCE_VALUES);
            writeHuffmanTable(0x11, AC_CHROMINANCE_BITS, AC_CHROMINANCE_VALUES);

            writeMarker(0xDA);
            writeShort(12);
            mStream.write(3);
            mStream.write(new byte[] {1, 0x00, 2, 0x11, 3, 0x11, 0, 63, 0});

            int[][] dcLuminance = buildCodes(DC_LUMINANCE_BITS, DC_LUMINANCE_VALUES);
            int[][] acLuminance = buildCodes(AC_LUMINANCE_BITS, AC_LUMINANCE_VALUES);
            int[][] dcChrominance = buildCodes(DC_CHROMINANCE_BITS, DC_CHROMINANCE_VALUES);
            int[][] acChrominance = buildCodes(AC_CHROMINANCE_BITS, AC_CHROMINANCE_VALUES);

            float[] blockY = new float[64];
            float[] blockCb = new float[64];
            float[] blockCr = new float[64];
            int previousY = 0;
            int previousCb = 0;
            int previousCr = 0;

            for (int blockTop = 0; blockTop < height; blockTop += 8)
            {
                for (int blockLeft = 0; blockLeft < width; blockLeft += 8)
                {
                    for (int row = 0; row < 8; row++)
                    {
                        int y = Math.min(height - 1, blockTop + row);

                        for (int column = 0; column < 8; column++)
                        {
                            int x = Math.min(width - 1, blockLeft + column);
                            int color = premultiply(pixels[y * width + x]);
                            float red = (color >> 16) & 0xFF;
                            float green = (color >> 8) & 0xFF;
                            float blue = color & 0xFF;
                            int at = row * 8 + column;

                            blockY[at] = 0.299f * red + 0.587f * green + 0.114f * blue - 128.0f;
                            blockCb[at] = -0.168736f * red - 0.331264f * green + 0.5f * blue;
                            blockCr[at] = 0.5f * red - 0.418688f * green - 0.081312f * blue;
                        }
                    }

                    previousY = encodeBlock(blockY, luminance, previousY, dcLuminance, acLuminance);
                    previousCb = encodeBlock(blockCb, chrominance, previousCb, dcChrominance, acChrominance);
                    previousCr = encodeBlock(blockCr, chrominance, previousCr, dcChrominance, acChrominance);
                }
            }

            flushBits();
            writeMarker(0xD9);
        }

        private int encodeBlock(float[] block, int[] quantization, int previousDc, int[][] dcCodes, int[][] acCodes) throws IOException
        {
            double[] intermediate = new double[64];

            for (int row = 0; row < 8; row++)
            {
                for (int frequency = 0; frequency < 8; frequency++)
                {
                    double sum = 0;
                    for (int column = 0; column < 8; column++) sum += COSINES[frequency][column] * block[row * 8 + column];
                    intermediate[row * 8 + frequency] = sum;
                }
            }

            int[] quantized = new int[64];

            for (int frequencyY = 0; frequencyY < 8; frequencyY++)
            {
                for (int frequencyX = 0; frequencyX < 8; frequencyX++)
                {
                    double sum = 0;
                    for (int row = 0; row < 8; row++) sum += COSINES[frequencyY][row] * intermediate[row * 8 + frequencyX];

                    int natural = frequencyY * 8 + frequencyX;
                    quantized[natural] = (int)Math.round(sum / quantization[natural]);
                }
            }

            int dc = quantized[0];
            int difference = dc - previousDc;
            int category = categoryOf(difference);
            writeBits(dcCodes[0][category], dcCodes[1][category]);
            if (category > 0) writeBits(difference < 0 ? difference - 1 : difference, category);

            int run = 0;

            for (int index = 1; index < 64; index++)
            {
                int value = quantized[ZIGZAG[index]];

                if (value == 0)
                {
                    run++;
                    continue;
                }

                while (run > 15)
                {
                    writeBits(acCodes[0][0xF0], acCodes[1][0xF0]);
                    run -= 16;
                }

                int size = categoryOf(value);
                int symbol = (run << 4) | size;
                writeBits(acCodes[0][symbol], acCodes[1][symbol]);
                writeBits(value < 0 ? value - 1 : value, size);
                run = 0;
            }

            if (run > 0) writeBits(acCodes[0][0x00], acCodes[1][0x00]);

            return dc;
        }

        private static int categoryOf(int value)
        {
            int magnitude = Math.abs(value);
            int category = 0;

            while (magnitude > 0)
            {
                category++;
                magnitude >>= 1;
            }

            return category;
        }

        private void writeBits(int value, int count) throws IOException
        {
            for (int bit = count - 1; bit >= 0; bit--)
            {
                mBitBuffer = (mBitBuffer << 1) | ((value >> bit) & 1);
                mBitCount++;

                if (mBitCount == 8)
                {
                    mStream.write(mBitBuffer);
                    if (mBitBuffer == 0xFF) mStream.write(0);
                    mBitBuffer = 0;
                    mBitCount = 0;
                }
            }
        }

        private void flushBits() throws IOException
        {
            if (mBitCount > 0) writeBits(0x7F, 8 - mBitCount);
        }

        private void writeHuffmanTable(int classAndId, int[] bits, int[] values) throws IOException
        {
            mStream.write(classAndId);
            for (int count : bits) mStream.write(count);
            for (int value : values) mStream.write(value);
        }

        private void writeMarker(int marker) throws IOException
        {
            mStream.write(0xFF);
            mStream.write(marker);
        }

        private void writeShort(int value) throws IOException
        {
            mStream.write((value >> 8) & 0xFF);
            mStream.write(value & 0xFF);
        }
    }
}

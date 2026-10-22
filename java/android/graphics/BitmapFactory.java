package android.graphics;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapFactory
{
    private static final int NINE_PATCH_HEADER_SIZE = 32;

    public BitmapFactory()
    {
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length)
    {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options options)
    {
        if ((offset | length) < 0 || data == null || data.length < offset + length) throw new ArrayIndexOutOfBoundsException();

        validate(options);
        return decode(data, offset, length, null, options);
    }

    public static Bitmap decodeStream(InputStream stream)
    {
        return decodeStream(stream, null, null);
    }

    public static Bitmap decodeStream(InputStream stream, Rect padding, Options options)
    {
        if (stream == null) return null;

        validate(options);

        byte[] data = readFully(stream, options == null ? null : options.inTempStorage);
        if (data == null) return null;

        return decode(data, 0, data.length, padding, options);
    }

    public static Bitmap decodeFile(String path)
    {
        return decodeFile(path, null);
    }

    public static Bitmap decodeFile(String path, Options options)
    {
        validate(options);
        if (path == null) return null;

        try (InputStream stream = new FileInputStream(path))
        {
            return decodeStream(stream, null, options);
        }
        catch (IOException failure)
        {
            android.util.Log.e("BitmapFactory", "Unable to decode stream: " + failure);
            return null;
        }
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor descriptor)
    {
        return decodeFileDescriptor(descriptor, null, null);
    }

    public static Bitmap decodeFileDescriptor(FileDescriptor descriptor, Rect padding, Options options)
    {
        validate(options);
        if (descriptor == null) return null;

        byte[] data = readFully(new FileInputStream(descriptor), options == null ? null : options.inTempStorage);
        if (data == null) return null;

        return decode(data, 0, data.length, padding, options);
    }

    public static Bitmap decodeResource(Resources resources, int id)
    {
        return decodeResource(resources, id, null);
    }

    public static Bitmap decodeResource(Resources resources, int id, Options options)
    {
        validate(options);

        Bitmap bitmap = null;
        InputStream stream = null;

        try
        {
            TypedValue value = new TypedValue();
            stream = resources.openRawResource(id, value);
            bitmap = decodeResourceStream(resources, value, stream, null, options);
        }
        catch (Exception failure)
        {
            bitmap = null;
        }
        finally
        {
            try
            {
                if (stream != null) stream.close();
            }
            catch (IOException ignored)
            {
            }
        }

        if (bitmap == null && options != null && options.inBitmap != null)
            throw new IllegalArgumentException("Problem decoding into existing bitmap");

        return bitmap;
    }

    public static Bitmap decodeResourceStream(Resources resources, TypedValue value, InputStream stream, Rect padding, Options options)
    {
        validate(options);
        if (options == null) options = new Options();

        if (options.inDensity == 0 && value != null)
        {
            int density = value.density;
            if (density == TypedValue.DENSITY_DEFAULT) density = densityFromPath(value.string);

            if (density == TypedValue.DENSITY_DEFAULT)
                options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            else if (density != TypedValue.DENSITY_NONE)
                options.inDensity = density;
        }

        if (options.inTargetDensity == 0 && resources != null) options.inTargetDensity = resources.getDisplayMetrics().densityDpi;

        return decodeStream(stream, padding, options);
    }

    private static int densityFromPath(CharSequence path)
    {
        if (path == null) return TypedValue.DENSITY_DEFAULT;

        String text = path.toString();
        int slash = text.lastIndexOf('/');
        if (slash <= 0) return TypedValue.DENSITY_DEFAULT;

        int previous = text.lastIndexOf('/', slash - 1);
        String directory = text.substring(previous + 1, slash);

        for (String qualifier : directory.split("-"))
        {
            switch (qualifier)
            {
                case "ldpi":
                    return DisplayMetrics.DENSITY_LOW;
                case "mdpi":
                    return DisplayMetrics.DENSITY_MEDIUM;
                case "tvdpi":
                    return 213;
                case "hdpi":
                    return DisplayMetrics.DENSITY_HIGH;
                case "xhdpi":
                    return DisplayMetrics.DENSITY_XHIGH;
                case "xxhdpi":
                    return DisplayMetrics.DENSITY_XXHIGH;
                case "xxxhdpi":
                    return DisplayMetrics.DENSITY_XXXHIGH;
                case "nodpi":
                case "anydpi":
                    return TypedValue.DENSITY_NONE;
                default:
                    if (qualifier.endsWith("dpi"))
                    {
                        try
                        {
                            return Integer.parseInt(qualifier.substring(0, qualifier.length() - 3));
                        }
                        catch (NumberFormatException ignored)
                        {
                        }
                    }
                    break;
            }
        }

        return TypedValue.DENSITY_DEFAULT;
    }

    private static void validate(Options options)
    {
        if (options != null && options.inMutable && options.inPreferredConfig == Bitmap.Config.HARDWARE)
            throw new IllegalArgumentException("Bitmaps with Config.HARDWARE are always immutable");
    }

    private static Bitmap decode(byte[] data, int offset, int length, Rect padding, Options options)
    {
        if (options != null && options.mCancel) return null;

        int[] size = new int[2];
        boolean measured = length > 0 && Bitmap.measureBytes(data, offset, length, size);

        byte[] chunk = measured ? ninePatchChunk(data, offset, length) : null;

        if (padding != null)
        {
            if (chunk != null)
                padding.set(readLittleInt(chunk, 12), readLittleInt(chunk, 20), readLittleInt(chunk, 16), readLittleInt(chunk, 24));
            else
                padding.set(-1, -1, -1, -1);
        }

        int sampleSize = options == null || options.inSampleSize <= 1 ? 1 : options.inSampleSize;
        float scale = 1.0f;

        if (options != null && options.inScaled && options.inDensity != 0 && options.inTargetDensity != 0
            && options.inDensity != options.inScreenDensity)
        {
            scale = options.inTargetDensity / (float)options.inDensity;
        }

        if (options != null)
        {
            options.outMimeType = measured ? mimeTypeOf(data, offset, length) : null;

            if (measured)
            {
                int sampledWidth = Math.max(1, size[0] / sampleSize);
                int sampledHeight = Math.max(1, size[1] / sampleSize);

                options.outWidth = scale != 1.0f ? (int)(sampledWidth * scale + 0.5f) : sampledWidth;
                options.outHeight = scale != 1.0f ? (int)(sampledHeight * scale + 0.5f) : sampledHeight;
                options.outConfig = Bitmap.Config.ARGB_8888;
                options.outColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            }
            else
            {
                options.outWidth = -1;
                options.outHeight = -1;
                options.outConfig = null;
                options.outColorSpace = null;
            }

            if (options.inJustDecodeBounds) return null;
        }

        if (!measured) return null;

        Bitmap decoded = Bitmap.adopt(Bitmap.decodeBytes(data, offset, length));
        if (decoded == null) return null;
        if (options != null && options.mCancel) return null;

        int targetWidth = Math.max(1, decoded.getWidth() / sampleSize);
        int targetHeight = Math.max(1, decoded.getHeight() / sampleSize);

        if (scale != 1.0f)
        {
            targetWidth = Math.max(1, (int)(targetWidth * scale + 0.5f));
            targetHeight = Math.max(1, (int)(targetHeight * scale + 0.5f));
        }

        Bitmap result = decoded;

        if (targetWidth != decoded.getWidth() || targetHeight != decoded.getHeight())
        {
            result = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true);
            decoded.recycle();
        }

        if (chunk != null && scale != 1.0f)
        {
            chunk = scaleNinePatchChunk(chunk, scale, targetWidth, targetHeight);
            if (padding != null)
                padding.set(readLittleInt(chunk, 12), readLittleInt(chunk, 20), readLittleInt(chunk, 16), readLittleInt(chunk, 24));
        }

        result.setNinePatchChunk(chunk);

        if (options != null)
        {
            Bitmap.Config config = chooseConfig(options.inPreferredConfig, result.hasAlpha());
            result.setConfigQuietly(config);
            options.outConfig = config;

            if (options.inPreferredColorSpace != null && config != Bitmap.Config.ALPHA_8)
            {
                result.setColorSpace(options.inPreferredColorSpace);
                options.outColorSpace = options.inPreferredColorSpace;
            }

            if (!options.inPremultiplied) result.setPremultiplied(false);

            if (scale != 1.0f)
                result.setDensity(options.inTargetDensity);
            else if (options.inDensity != 0)
                result.setDensity(options.inDensity);

            result.setMutable(options.inMutable || options.inBitmap != null);

            if (options.inBitmap != null) result = reuse(options.inBitmap, result);
        }
        else
        {
            result.setMutable(false);
        }

        return result;
    }

    private static Bitmap reuse(Bitmap target, Bitmap decoded)
    {
        if (target.isRecycled()) throw new IllegalArgumentException("Cannot reuse a recycled Bitmap");
        if (!target.isMutable()) throw new IllegalArgumentException("Bitmaps must be mutable to be reused");

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        if ((long)width * height * 4 > target.getAllocationByteCount()) return decoded;

        target.reconfigure(width, height, decoded.getConfig());

        int[] pixels = new int[width * height];
        decoded.getPixels(pixels, 0, width, 0, 0, width, height);
        target.setPixels(pixels, 0, width, 0, 0, width, height);
        target.setDensity(decoded.getDensity());
        target.setHasAlpha(decoded.hasAlpha());
        target.setNinePatchChunk(decoded.getNinePatchChunk());
        decoded.recycle();

        return target;
    }

    private static Bitmap.Config chooseConfig(Bitmap.Config requested, boolean hasAlpha)
    {
        if (requested == null) return Bitmap.Config.ARGB_8888;

        switch (requested)
        {
            case RGB_565:
                return hasAlpha ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            case ALPHA_8:
            case ARGB_4444:
                return Bitmap.Config.ARGB_8888;
            default:
                return requested;
        }
    }

    private static byte[] ninePatchChunk(byte[] data, int offset, int length)
    {
        if (length < 8 || (data[offset] & 0xFF) != 0x89 || data[offset + 1] != 'P' || data[offset + 2] != 'N' || data[offset + 3] != 'G')
            return null;

        int at = offset + 8;
        int end = offset + length;

        while (at + 8 <= end)
        {
            int chunkLength = readBigInt(data, at);
            if (chunkLength < 0 || at + 12 + (long)chunkLength > end) return null;

            int type = at + 4;
            if (data[type] == 'n' && data[type + 1] == 'p' && data[type + 2] == 'T' && data[type + 3] == 'c')
            {
                byte[] chunk = new byte[chunkLength];
                System.arraycopy(data, at + 8, chunk, 0, chunkLength);
                return toDeviceOrder(chunk);
            }

            if (data[type] == 'I' && data[type + 1] == 'E' && data[type + 2] == 'N' && data[type + 3] == 'D') return null;

            at += 12 + chunkLength;
        }

        return null;
    }

    private static byte[] toDeviceOrder(byte[] chunk)
    {
        if (chunk.length < NINE_PATCH_HEADER_SIZE) return null;

        int horizontalCount = chunk[1] & 0xFF;
        int verticalCount = chunk[2] & 0xFF;
        int colorCount = chunk[3] & 0xFF;
        int needed = NINE_PATCH_HEADER_SIZE + (horizontalCount + verticalCount + colorCount) * 4;
        if (chunk.length < needed) return null;

        byte[] device = chunk.clone();
        device[0] = 1;

        int xDivsOffset = NINE_PATCH_HEADER_SIZE;
        int yDivsOffset = xDivsOffset + horizontalCount * 4;
        int colorsOffset = yDivsOffset + verticalCount * 4;

        writeLittleInt(device, 4, xDivsOffset);
        writeLittleInt(device, 8, yDivsOffset);
        writeLittleInt(device, 28, colorsOffset);

        for (int at = 12; at < 28; at += 4) writeLittleInt(device, at, readBigInt(chunk, at));
        for (int at = NINE_PATCH_HEADER_SIZE; at < needed; at += 4) writeLittleInt(device, at, readBigInt(chunk, at));

        return device;
    }

    private static byte[] scaleNinePatchChunk(byte[] chunk, float scale, int scaledWidth, int scaledHeight)
    {
        byte[] scaled = chunk.clone();
        int horizontalCount = chunk[1] & 0xFF;
        int verticalCount = chunk[2] & 0xFF;

        for (int at = 12; at < 28; at += 4) writeLittleInt(scaled, at, (int)(readLittleInt(chunk, at) * scale + 0.5f));

        scaleDivs(scaled, NINE_PATCH_HEADER_SIZE, horizontalCount, scale, scaledWidth);
        scaleDivs(scaled, NINE_PATCH_HEADER_SIZE + horizontalCount * 4, verticalCount, scale, scaledHeight);

        return scaled;
    }

    private static void scaleDivs(byte[] chunk, int start, int count, float scale, int maxValue)
    {
        int previous = 0;

        for (int index = 0; index < count; index++)
        {
            int at = start + index * 4;
            int value = (int)(readLittleInt(chunk, at) * scale + 0.5f);
            if (index > 0 && value <= previous) value = previous + 1;
            if (value > maxValue) value = maxValue;
            writeLittleInt(chunk, at, value);
            previous = value;
        }
    }

    private static int readBigInt(byte[] data, int at)
    {
        return ((data[at] & 0xFF) << 24) | ((data[at + 1] & 0xFF) << 16) | ((data[at + 2] & 0xFF) << 8) | (data[at + 3] & 0xFF);
    }

    private static int readLittleInt(byte[] data, int at)
    {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8) | ((data[at + 2] & 0xFF) << 16) | ((data[at + 3] & 0xFF) << 24);
    }

    private static void writeLittleInt(byte[] data, int at, int value)
    {
        data[at] = (byte)value;
        data[at + 1] = (byte)(value >> 8);
        data[at + 2] = (byte)(value >> 16);
        data[at + 3] = (byte)(value >> 24);
    }

    static byte[] readFully(InputStream stream, byte[] storage)
    {
        if (stream == null) return null;

        try
        {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = storage != null && storage.length > 0 ? storage : new byte[16384];

            int read;
            while ((read = stream.read(chunk)) > 0) collected.write(chunk, 0, read);

            return collected.toByteArray();
        }
        catch (IOException failure)
        {
            return null;
        }
    }

    private static String mimeTypeOf(byte[] data, int offset, int length)
    {
        if (length >= 8 && (data[offset] & 0xFF) == 0x89 && data[offset + 1] == 'P') return "image/png";
        if (length >= 3 && (data[offset] & 0xFF) == 0xFF && (data[offset + 1] & 0xFF) == 0xD8) return "image/jpeg";
        if (length >= 12 && data[offset] == 'R' && data[offset + 1] == 'I') return "image/webp";
        if (length >= 6 && data[offset] == 'G' && data[offset + 1] == 'I' && data[offset + 2] == 'F') return "image/gif";
        if (length >= 2 && data[offset] == 'B' && data[offset + 1] == 'M') return "image/bmp";
        if (length >= 12 && data[offset + 4] == 'f' && data[offset + 5] == 't' && data[offset + 6] == 'y' && data[offset + 7] == 'p')
            return "image/heif";

        return null;
    }

    public static class Options
    {
        public Bitmap inBitmap;
        public boolean inMutable;
        public boolean inJustDecodeBounds;
        public int inSampleSize;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public ColorSpace inPreferredColorSpace;
        public boolean inPremultiplied = true;
        public boolean inDither;
        public int inDensity;
        public int inTargetDensity;
        public int inScreenDensity;
        public boolean inScaled = true;
        public boolean inPurgeable;
        public boolean inInputShareable;
        public boolean inPreferQualityOverSpeed;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
        public Bitmap.Config outConfig;
        public ColorSpace outColorSpace;
        public byte[] inTempStorage;
        public boolean mCancel;

        public Options()
        {
        }

        public void requestCancelDecode()
        {
            mCancel = true;
        }
    }
}

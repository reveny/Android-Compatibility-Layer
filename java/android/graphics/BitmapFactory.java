package android.graphics;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapFactory
{
    public static Bitmap decodeByteArray(byte[] data, int offset, int length)
    {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options options)
    {
        if (data == null || length <= 0) return null;

        if (options != null)
        {
            int[] size = new int[2];
            if (Bitmap.measureBytes(data, offset, length, size))
            {
                options.outWidth = size[0];
                options.outHeight = size[1];
                options.outMimeType = mimeTypeOf(data, offset, length);
            }
            else
            {
                options.outWidth = -1;
                options.outHeight = -1;
            }

            if (options.inJustDecodeBounds) return null;
        }

        return Bitmap.adopt(Bitmap.decodeBytes(data, offset, length));
    }

    public static Bitmap decodeStream(InputStream stream)
    {
        return decodeStream(stream, null, null);
    }

    public static Bitmap decodeStream(InputStream stream, Rect padding, Options options)
    {
        byte[] data = readFully(stream);
        if (data == null) return null;

        return decodeByteArray(data, 0, data.length, options);
    }

    public static Bitmap decodeFile(String path)
    {
        return decodeFile(path, null);
    }

    public static Bitmap decodeFile(String path, Options options)
    {
        if (path == null) return null;

        try (InputStream stream = new FileInputStream(path))
        {
            return decodeStream(stream, null, options);
        }
        catch (IOException failure)
        {
            return null;
        }
    }

    public static Bitmap decodeResource(Object resources, int id)
    {
        return null;
    }

    private static byte[] readFully(InputStream stream)
    {
        if (stream == null) return null;

        try
        {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];

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

        return null;
    }

    public static class Options
    {
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public boolean inMutable;
        public boolean inScaled = true;
        public int inDensity;
        public int inTargetDensity;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
    }
}

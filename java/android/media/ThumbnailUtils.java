package android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.CancellationSignal;
import android.util.Size;

import java.io.File;
import java.io.IOException;

public class ThumbnailUtils
{
    public static final int OPTIONS_RECYCLE_INPUT = 2;

    private static final int MINI_KIND = 1;
    private static final int MICRO_KIND = 3;
    private static final int MINI_WIDTH = 512;
    private static final int MINI_HEIGHT = 384;
    private static final int MICRO_SIZE = 96;
    private static final String[] FOLDER_ART_NAMES = {"albumart.jpg", "albumart.png", "cover.jpg", "cover.png", "folder.jpg",
        "folder.png", "front.jpg", "front.png"};

    public ThumbnailUtils()
    {
    }

    public static Bitmap createAudioThumbnail(File file, Size size, CancellationSignal signal) throws IOException
    {
        checkCanceled(signal);

        if (file == null) throw new NullPointerException("file");
        if (size == null) throw new NullPointerException("size");

        File directory = file.getParentFile();
        File[] siblings = directory == null ? null : directory.listFiles();

        if (siblings != null)
        {
            for (String name : FOLDER_ART_NAMES)
            {
                for (File sibling : siblings)
                {
                    checkCanceled(signal);

                    if (sibling.isFile() && sibling.getName().equalsIgnoreCase(name))
                    {
                        Bitmap art = decodeScaled(sibling.getAbsolutePath(), size.getWidth(), size.getHeight());

                        if (art != null) return art;
                    }
                }
            }
        }

        throw new IOException("No album art found");
    }

    public static Bitmap createAudioThumbnail(String filePath, int kind)
    {
        try
        {
            return createAudioThumbnail(new File(filePath), sizeForKind(kind), null);
        }
        catch (IOException | RuntimeException failure)
        {
            return null;
        }
    }

    public static Bitmap createImageThumbnail(File file, Size size, CancellationSignal signal) throws IOException
    {
        checkCanceled(signal);

        if (file == null) throw new NullPointerException("file");
        if (size == null) throw new NullPointerException("size");
        if (!file.isFile()) throw new IOException("File not found: " + file);

        Bitmap bitmap = decodeScaled(file.getAbsolutePath(), size.getWidth(), size.getHeight());

        checkCanceled(signal);

        if (bitmap == null) throw new IOException("Failed to decode image: " + file);

        int rotation = 0;

        try
        {
            rotation = new ExifInterface(file).getRotationDegrees();
        }
        catch (IOException | RuntimeException ignored)
        {
            rotation = 0;
        }

        if (rotation != 0)
        {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            if (rotated != null && rotated != bitmap)
            {
                bitmap.recycle();
                bitmap = rotated;
            }
        }

        return bitmap;
    }

    public static Bitmap createImageThumbnail(String filePath, int kind)
    {
        if (filePath == null) return null;

        try
        {
            Bitmap bitmap = createImageThumbnail(new File(filePath), sizeForKind(kind), null);

            if (kind == MICRO_KIND) return extractThumbnail(bitmap, MICRO_SIZE, MICRO_SIZE, OPTIONS_RECYCLE_INPUT);

            return bitmap;
        }
        catch (IOException | RuntimeException failure)
        {
            return null;
        }
    }

    public static Bitmap createVideoThumbnail(File file, Size size, CancellationSignal signal) throws IOException
    {
        checkCanceled(signal);

        if (file == null) throw new NullPointerException("file");
        if (size == null) throw new NullPointerException("size");

        throw new IOException("No video decoder is available for " + file);
    }

    public static Bitmap createVideoThumbnail(String filePath, int kind)
    {
        return null;
    }

    public static Bitmap extractThumbnail(Bitmap source, int width, int height)
    {
        return extractThumbnail(source, width, height, 0);
    }

    public static Bitmap extractThumbnail(Bitmap source, int width, int height, int options)
    {
        if (source == null) return null;
        if (width <= 0 || height <= 0) return null;

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        if (sourceWidth <= 0 || sourceHeight <= 0) return null;

        float scale = Math.max((float)width / sourceWidth, (float)height / sourceHeight);
        int scaledWidth = Math.max(width, Math.round(sourceWidth * scale));
        int scaledHeight = Math.max(height, Math.round(sourceHeight * scale));

        Bitmap scaled = scaledWidth == sourceWidth && scaledHeight == sourceHeight
            ? source : Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);

        int left = (scaledWidth - width) / 2;
        int top = (scaledHeight - height) / 2;
        Bitmap result = left == 0 && top == 0 && scaledWidth == width && scaledHeight == height
            ? scaled : Bitmap.createBitmap(scaled, left, top, width, height);

        if (scaled != source && scaled != result) scaled.recycle();

        if ((options & OPTIONS_RECYCLE_INPUT) != 0 && result != source) source.recycle();

        return result;
    }

    private static Size sizeForKind(int kind)
    {
        if (kind == MICRO_KIND) return new Size(MICRO_SIZE, MICRO_SIZE);

        return new Size(MINI_WIDTH, MINI_HEIGHT);
    }

    private static void checkCanceled(CancellationSignal signal)
    {
        if (signal != null) signal.throwIfCanceled();
    }

    private static Bitmap decodeScaled(String path, int maxWidth, int maxHeight)
    {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int sampleSize = 1;

        while (bounds.outWidth / (sampleSize * 2) >= maxWidth && bounds.outHeight / (sampleSize * 2) >= maxHeight)
        {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap decoded = BitmapFactory.decodeFile(path, options);

        if (decoded == null) return null;

        int width = decoded.getWidth();
        int height = decoded.getHeight();

        if (width <= maxWidth && height <= maxHeight) return decoded;

        float scale = Math.min((float)maxWidth / width, (float)maxHeight / height);
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true);

        if (scaled != decoded) decoded.recycle();

        return scaled;
    }
}

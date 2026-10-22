package android.graphics;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Movie
{
    private final Bitmap mFrame;
    private int mTime;

    private Movie(Bitmap frame)
    {
        mFrame = frame;
    }

    public int width()
    {
        return mFrame.getWidth();
    }

    public int height()
    {
        return mFrame.getHeight();
    }

    public boolean isOpaque()
    {
        return !mFrame.hasAlpha();
    }

    public int duration()
    {
        return 0;
    }

    public boolean setTime(int relativeMilliseconds)
    {
        mTime = relativeMilliseconds;
        return true;
    }

    public void draw(Canvas canvas, float x, float y, Paint paint)
    {
        if (canvas != null) canvas.drawBitmap(mFrame, x, y, paint);
    }

    public void draw(Canvas canvas, float x, float y)
    {
        draw(canvas, x, y, null);
    }

    public static Movie decodeStream(InputStream stream)
    {
        if (stream == null) return null;

        try
        {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];

            int read;
            while ((read = stream.read(chunk)) > 0) collected.write(chunk, 0, read);

            byte[] data = collected.toByteArray();
            return decodeByteArray(data, 0, data.length);
        }
        catch (IOException failure)
        {
            return null;
        }
    }

    public static Movie decodeByteArray(byte[] data, int offset, int length)
    {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset + length > data.length) throw new ArrayIndexOutOfBoundsException();

        Bitmap frame = BitmapFactory.decodeByteArray(data, offset, length);
        return frame == null ? null : new Movie(frame);
    }

    public static Movie decodeFile(String path)
    {
        try (InputStream stream = new FileInputStream(path))
        {
            return decodeStream(stream);
        }
        catch (IOException failure)
        {
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }
}

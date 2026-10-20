package android.graphics;

public class Picture
{
    private Bitmap mRecorded;
    private Canvas mRecording;
    private int mWidth;
    private int mHeight;

    public Picture()
    {
    }

    public Picture(Picture source)
    {
        if (source == null) return;

        mWidth = source.mWidth;
        mHeight = source.mHeight;
        mRecorded = source.mRecorded;
    }

    public Canvas beginRecording(int width, int height)
    {
        mWidth = Math.max(0, width);
        mHeight = Math.max(0, height);

        mRecorded = Bitmap.createBitmap(Math.max(1, mWidth), Math.max(1, mHeight), Bitmap.Config.ARGB_8888);
        mRecording = new Canvas(mRecorded);

        return mRecording;
    }

    public void endRecording()
    {
        mRecording = null;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public boolean requiresHardwareAcceleration()
    {
        return false;
    }

    public void draw(Canvas canvas)
    {
        if (canvas == null || mRecorded == null) return;

        canvas.drawBitmap(mRecorded, 0.0f, 0.0f, null);
    }

    Bitmap recorded()
    {
        return mRecorded;
    }
}

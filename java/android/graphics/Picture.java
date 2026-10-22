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

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
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

    @Deprecated
    public void writeToStream(java.io.OutputStream stream)
    {
        if (stream == null) throw new NullPointerException();

        try
        {
            java.io.DataOutputStream output = new java.io.DataOutputStream(stream);
            output.writeInt(STREAM_MAGIC);
            output.writeInt(mWidth);
            output.writeInt(mHeight);
            output.writeBoolean(mRecorded != null);
            output.flush();

            if (mRecorded != null && !mRecorded.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new RuntimeException();
        }
        catch (java.io.IOException failure)
        {
            throw new RuntimeException(failure);
        }
    }

    @Deprecated
    public static Picture createFromStream(java.io.InputStream stream)
    {
        try
        {
            java.io.DataInputStream input = new java.io.DataInputStream(stream);
            if (input.readInt() != STREAM_MAGIC) return new Picture();

            Picture picture = new Picture();
            picture.mWidth = input.readInt();
            picture.mHeight = input.readInt();
            if (input.readBoolean()) picture.mRecorded = BitmapFactory.decodeStream(input, null, null);

            return picture;
        }
        catch (java.io.IOException failure)
        {
            return new Picture();
        }
    }

    private static final int STREAM_MAGIC = 0x41434C50;
}

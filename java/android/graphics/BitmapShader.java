package android.graphics;

public class BitmapShader extends Shader
{
    public static final int FILTER_MODE_DEFAULT = 0;
    public static final int FILTER_MODE_NEAREST = 1;
    public static final int FILTER_MODE_LINEAR = 2;

    private final Bitmap mBitmap;
    private final int mTileX;
    private final int mTileY;
    private int mFilterMode = FILTER_MODE_DEFAULT;
    private int mMaxAnisotropy;

    public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY)
    {
        if (bitmap == null) throw new IllegalArgumentException("a bitmap shader needs a bitmap");

        mBitmap = bitmap;
        mTileX = tileModeId(tileX);
        mTileY = tileModeId(tileY);
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    public int getFilterMode()
    {
        return mFilterMode;
    }

    public void setFilterMode(int mode)
    {
        if (mode < FILTER_MODE_DEFAULT || mode > FILTER_MODE_LINEAR) throw new IllegalArgumentException("Invalid filter mode " + mode);

        mFilterMode = mode;
        mMaxAnisotropy = 0;
    }

    public void setMaxAnisotropy(int maxAnisotropy)
    {
        if (maxAnisotropy < 0) throw new IllegalArgumentException("maxAnisotropy must be >= 0");

        mMaxAnisotropy = maxAnisotropy;
        mFilterMode = FILTER_MODE_DEFAULT;
    }

    public int getMaxAnisotropy()
    {
        return mMaxAnisotropy;
    }

    @Override
    int kind()
    {
        return KIND_BITMAP;
    }

    @Override
    int tileModeId()
    {
        return mTileX;
    }

    @Override
    long bitmapHandle()
    {
        return mBitmap.nativeHandle();
    }
}

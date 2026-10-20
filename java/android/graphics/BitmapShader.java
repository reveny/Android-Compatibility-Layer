package android.graphics;

public class BitmapShader extends Shader
{
    private final Bitmap mBitmap;
    private final int mTileX;
    private final int mTileY;

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

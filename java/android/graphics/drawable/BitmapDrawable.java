package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class BitmapDrawable extends Drawable
{
    private static final String[] TILE_NAMES = {"clamp", "repeat", "mirror"};

    private final Paint mPaint = new Paint();

    private Bitmap mBitmap;
    private Shader.TileMode mTileX;
    private Shader.TileMode mTileY;
    private int mGravity = 119;

    public BitmapDrawable()
    {
    }

    public BitmapDrawable(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public BitmapDrawable(Resources resources, Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public BitmapDrawable(Resources resources, String path)
    {
        mBitmap = BitmapFactory.decodeFile(path);
    }

    public BitmapDrawable(Resources resources, java.io.InputStream stream)
    {
        mBitmap = BitmapFactory.decodeStream(stream);
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public Paint getPaint()
    {
        return mPaint;
    }

    public void setGravity(int gravity)
    {
        mGravity = gravity;
    }

    public int getGravity()
    {
        return mGravity;
    }

    public void setTileModeX(Shader.TileMode mode)
    {
        mTileX = mode;
    }

    public void setTileModeY(Shader.TileMode mode)
    {
        mTileY = mode;
    }

    public void setTileModeXY(Shader.TileMode x, Shader.TileMode y)
    {
        mTileX = x;
        mTileY = y;
    }

    public void setAntiAlias(boolean enabled)
    {
        mPaint.setAntiAlias(enabled);
    }

    public void setFilterBitmap(boolean enabled)
    {
        mPaint.setFilterBitmap(enabled);
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mBitmap == null ? -1 : mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mBitmap == null ? -1 : mBitmap.getHeight();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        int source = DrawableInflater.reference(attributes, "src", 0);
        if (source != 0) mBitmap = resources.getBitmap(source);

        int tileMode = DrawableInflater.enumeration(attributes, "tileMode", TILE_NAMES, -1);

        if (tileMode == 0) setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        else if (tileMode == 1) setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        else if (tileMode == 2) setTileModeXY(Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);

        mPaint.setFilterBitmap(DrawableInflater.flag(attributes, "filter", true));
        mPaint.setAntiAlias(DrawableInflater.flag(attributes, "antialias", true));
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mBitmap == null) return;

        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        mPaint.setAlpha(getAlpha());

        if (mTileX != null || mTileY != null)
        {
            Shader.TileMode x = mTileX == null ? Shader.TileMode.CLAMP : mTileX;
            Shader.TileMode y = mTileY == null ? Shader.TileMode.CLAMP : mTileY;

            mPaint.setShader(new BitmapShader(mBitmap, x, y));
            mPaint.setStyle(Paint.Style.FILL);

            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
            mPaint.setShader(null);
            return;
        }

        canvas.drawBitmap(mBitmap, null, new RectF(bounds.left, bounds.top, bounds.right, bounds.bottom), mPaint);
    }
}

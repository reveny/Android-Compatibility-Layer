package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Picture;
import android.graphics.Rect;

public class PictureDrawable extends Drawable
{
    private Picture mPicture;

    public PictureDrawable(Picture picture)
    {
        mPicture = picture;
    }

    public Picture getPicture()
    {
        return mPicture;
    }

    public void setPicture(Picture picture)
    {
        mPicture = picture;
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mPicture == null) return;

        Rect bounds = getBounds();
        int saveCount = canvas.save();

        canvas.clipRect(bounds);
        canvas.translate(bounds.left, bounds.top);
        canvas.drawPicture(mPicture);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mPicture != null ? mPicture.getWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mPicture != null ? mPicture.getHeight() : -1;
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
    }

    @Override
    public void setAlpha(int alpha)
    {
    }
}

package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class ColorDrawable extends Drawable
{
    private final Paint mPaint = new Paint();
    private int mColor;

    public ColorDrawable()
    {
        this(0);
    }

    public ColorDrawable(int color)
    {
        mColor = color;
    }

    public int getColor()
    {
        return mColor;
    }

    public void setColor(int color)
    {
        mColor = color;
    }

    @Override
    public void draw(Canvas canvas)
    {
        if ((mColor >>> 24) == 0) return;

        Rect bounds = getBounds();
        mPaint.setColor(mColor);
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
    }

    @Override
    public int getOpacity()
    {
        return (mColor >>> 24) == 0xFF ? 0xFF : 0;
    }
}

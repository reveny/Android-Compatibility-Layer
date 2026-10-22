package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;

public class RectShape extends Shape
{
    private RectF mRect = new RectF();

    public RectShape()
    {
    }

    @Override
    public void draw(Canvas canvas, Paint paint)
    {
        canvas.drawRect(mRect, paint);
    }

    @Override
    public void getOutline(Outline outline)
    {
        RectF rect = rect();
        outline.setRect((int)Math.ceil(rect.left), (int)Math.ceil(rect.top), (int)Math.floor(rect.right), (int)Math.floor(rect.bottom));
    }

    @Override
    protected void onResize(float width, float height)
    {
        mRect.set(0, 0, width, height);
    }

    protected final RectF rect()
    {
        return mRect;
    }

    @Override
    public RectShape clone() throws CloneNotSupportedException
    {
        RectShape shape = (RectShape) super.clone();
        shape.mRect = new RectF(mRect);
        return shape;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other)) return false;

        return mRect.equals(((RectShape) other).mRect);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + mRect.hashCode();
    }
}

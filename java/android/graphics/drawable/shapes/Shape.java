package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;

public abstract class Shape implements Cloneable
{
    private float mWidth;
    private float mHeight;

    public Shape()
    {
    }

    public final float getWidth()
    {
        return mWidth;
    }

    public final float getHeight()
    {
        return mHeight;
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public final void resize(float width, float height)
    {
        float clampedWidth = width < 0 ? 0 : width;
        float clampedHeight = height < 0 ? 0 : height;

        if (mWidth == clampedWidth && mHeight == clampedHeight) return;

        mWidth = clampedWidth;
        mHeight = clampedHeight;
        onResize(clampedWidth, clampedHeight);
    }

    public boolean hasAlpha()
    {
        return true;
    }

    protected void onResize(float width, float height)
    {
    }

    public void getOutline(Outline outline)
    {
    }

    @Override
    public Shape clone() throws CloneNotSupportedException
    {
        return (Shape) super.clone();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        Shape shape = (Shape) other;
        return Float.compare(shape.mWidth, mWidth) == 0 && Float.compare(shape.mHeight, mHeight) == 0;
    }

    @Override
    public int hashCode()
    {
        return 31 * Float.floatToIntBits(mWidth) + Float.floatToIntBits(mHeight);
    }
}

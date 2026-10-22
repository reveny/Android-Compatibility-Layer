package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;

public class ArcShape extends RectShape
{
    private final float mStartAngle;
    private final float mSweepAngle;

    public ArcShape(float startAngle, float sweepAngle)
    {
        mStartAngle = startAngle;
        mSweepAngle = sweepAngle;
    }

    public final float getStartAngle()
    {
        return mStartAngle;
    }

    public final float getSweepAngle()
    {
        return mSweepAngle;
    }

    @Override
    public void draw(Canvas canvas, Paint paint)
    {
        canvas.drawArc(rect(), mStartAngle, mSweepAngle, true, paint);
    }

    @Override
    public void getOutline(Outline outline)
    {
    }

    @Override
    public ArcShape clone() throws CloneNotSupportedException
    {
        return (ArcShape) super.clone();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other)) return false;

        ArcShape shape = (ArcShape) other;
        return Float.compare(shape.mStartAngle, mStartAngle) == 0 && Float.compare(shape.mSweepAngle, mSweepAngle) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Float.floatToIntBits(mStartAngle);
        return 31 * result + Float.floatToIntBits(mSweepAngle);
    }
}

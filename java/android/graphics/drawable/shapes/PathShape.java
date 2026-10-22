package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class PathShape extends Shape
{
    private final float mStandardWidth;
    private final float mStandardHeight;

    private Path mPath;
    private float mScaleX = 1;
    private float mScaleY = 1;

    public PathShape(Path path, float stdWidth, float stdHeight)
    {
        mPath = path;
        mStandardWidth = stdWidth;
        mStandardHeight = stdHeight;
    }

    @Override
    public void draw(Canvas canvas, Paint paint)
    {
        int saveCount = canvas.save();
        canvas.scale(mScaleX, mScaleY);
        canvas.drawPath(mPath, paint);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onResize(float width, float height)
    {
        mScaleX = mStandardWidth == 0 ? 1 : width / mStandardWidth;
        mScaleY = mStandardHeight == 0 ? 1 : height / mStandardHeight;
    }

    @Override
    public PathShape clone() throws CloneNotSupportedException
    {
        PathShape shape = (PathShape) super.clone();
        shape.mPath = new Path(mPath);
        return shape;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other)) return false;

        PathShape shape = (PathShape) other;
        return Float.compare(shape.mStandardWidth, mStandardWidth) == 0 && Float.compare(shape.mStandardHeight, mStandardHeight) == 0
               && Float.compare(shape.mScaleX, mScaleX) == 0 && Float.compare(shape.mScaleY, mScaleY) == 0
               && mPath == shape.mPath;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Float.floatToIntBits(mStandardWidth);
        result = 31 * result + Float.floatToIntBits(mStandardHeight);
        result = 31 * result + Float.floatToIntBits(mScaleX);
        return 31 * result + Float.floatToIntBits(mScaleY);
    }
}

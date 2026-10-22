package android.graphics.drawable.shapes;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Arrays;

public class RoundRectShape extends RectShape
{
    private float[] mOuterRadii;
    private RectF mInset;
    private float[] mInnerRadii;

    private RectF mInnerRect;
    private Path mPath;

    public RoundRectShape(float[] outerRadii, RectF inset, float[] innerRadii)
    {
        if (outerRadii != null && outerRadii.length < 8) throw new ArrayIndexOutOfBoundsException("outer radii must have >= 8 values");
        if (innerRadii != null && innerRadii.length < 8) throw new ArrayIndexOutOfBoundsException("inner radii must have >= 8 values");

        mOuterRadii = outerRadii;
        mInset = inset;
        mInnerRadii = innerRadii;

        if (inset != null) mInnerRect = new RectF();

        mPath = new Path();
    }

    @Override
    public void draw(Canvas canvas, Paint paint)
    {
        canvas.drawPath(mPath, paint);
    }

    @Override
    public void getOutline(Outline outline)
    {
        if (mInnerRect != null) return;

        float radius = 0;

        if (mOuterRadii != null)
        {
            radius = mOuterRadii[0];

            for (int index = 1; index < 8; index++)
            {
                if (mOuterRadii[index] != radius)
                {
                    outline.setConvexPath(mPath);
                    return;
                }
            }
        }

        RectF rect = rect();
        outline.setRoundRect((int)Math.ceil(rect.left), (int)Math.ceil(rect.top), (int)Math.floor(rect.right),
                             (int)Math.floor(rect.bottom), radius);
    }

    @Override
    protected void onResize(float width, float height)
    {
        super.onResize(width, height);

        RectF rect = rect();
        mPath.reset();

        if (mOuterRadii != null) mPath.addRoundRect(rect, mOuterRadii, Path.Direction.CW);
        else mPath.addRect(rect, Path.Direction.CW);

        if (mInnerRect == null) return;

        mInnerRect.set(rect.left + mInset.left, rect.top + mInset.top, rect.right - mInset.right, rect.bottom - mInset.bottom);

        if (mInnerRect.width() < width && mInnerRect.height() < height)
        {
            if (mInnerRadii != null) mPath.addRoundRect(mInnerRect, mInnerRadii, Path.Direction.CCW);
            else mPath.addRect(mInnerRect, Path.Direction.CCW);

            mPath.setFillType(Path.FillType.EVEN_ODD);
        }
    }

    @Override
    public RoundRectShape clone() throws CloneNotSupportedException
    {
        RoundRectShape shape = (RoundRectShape) super.clone();
        shape.mOuterRadii = mOuterRadii == null ? null : mOuterRadii.clone();
        shape.mInnerRadii = mInnerRadii == null ? null : mInnerRadii.clone();
        shape.mInset = mInset == null ? null : new RectF(mInset);
        shape.mInnerRect = mInnerRect == null ? null : new RectF(mInnerRect);
        shape.mPath = new Path(mPath);
        return shape;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other)) return false;

        RoundRectShape shape = (RoundRectShape) other;
        return Arrays.equals(mOuterRadii, shape.mOuterRadii) && Arrays.equals(mInnerRadii, shape.mInnerRadii)
               && (mInset == null ? shape.mInset == null : mInset.equals(shape.mInset));
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(mOuterRadii);
        result = 31 * result + Arrays.hashCode(mInnerRadii);
        return 31 * result + (mInset == null ? 0 : mInset.hashCode());
    }
}

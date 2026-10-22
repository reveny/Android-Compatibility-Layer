package android.view.animation;

import android.graphics.Matrix;
import android.graphics.Rect;

public class Transformation
{
    public static final int TYPE_IDENTITY = 0x0;
    public static final int TYPE_ALPHA = 0x1;
    public static final int TYPE_MATRIX = 0x2;
    public static final int TYPE_BOTH = TYPE_ALPHA | TYPE_MATRIX;

    protected Matrix mMatrix;
    protected float mAlpha;
    protected int mTransformationType;

    private boolean mHasClipRect;
    private final Rect mClipRect = new Rect();

    public Transformation()
    {
        clear();
    }

    public void clear()
    {
        if (mMatrix == null) mMatrix = new Matrix();
        else mMatrix.reset();

        mClipRect.setEmpty();
        mHasClipRect = false;
        mAlpha = 1.0f;
        mTransformationType = TYPE_BOTH;
    }

    public int getTransformationType()
    {
        return mTransformationType;
    }

    public void setTransformationType(int transformationType)
    {
        mTransformationType = transformationType;
    }

    public void set(Transformation transformation)
    {
        mAlpha = transformation.getAlpha();
        mMatrix.set(transformation.getMatrix());

        if (transformation.mHasClipRect)
        {
            setClipRect(transformation.getClipRect());
        }
        else
        {
            mHasClipRect = false;
            mClipRect.setEmpty();
        }

        mTransformationType = transformation.getTransformationType();
    }

    public void compose(Transformation transformation)
    {
        mAlpha *= transformation.getAlpha();
        mMatrix.preConcat(transformation.getMatrix());

        if (transformation.mHasClipRect)
        {
            Rect bounds = transformation.getClipRect();
            if (mHasClipRect)
            {
                setClipRect(mClipRect.left + bounds.left, mClipRect.top + bounds.top, mClipRect.right + bounds.right,
                            mClipRect.bottom + bounds.bottom);
            }
            else
            {
                setClipRect(bounds);
            }
        }
    }

    public void postCompose(Transformation transformation)
    {
        mAlpha *= transformation.getAlpha();
        mMatrix.postConcat(transformation.getMatrix());

        if (transformation.mHasClipRect)
        {
            Rect bounds = transformation.getClipRect();
            if (mHasClipRect)
            {
                setClipRect(mClipRect.left + bounds.left, mClipRect.top + bounds.top, mClipRect.right + bounds.right,
                            mClipRect.bottom + bounds.bottom);
            }
            else
            {
                setClipRect(bounds);
            }
        }
    }

    public Matrix getMatrix()
    {
        return mMatrix;
    }

    public void setAlpha(float alpha)
    {
        mAlpha = alpha;
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    public void setClipRect(Rect rect)
    {
        setClipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setClipRect(int left, int top, int right, int bottom)
    {
        mClipRect.set(left, top, right, bottom);
        mHasClipRect = true;
    }

    public Rect getClipRect()
    {
        return mClipRect;
    }

    public boolean hasClipRect()
    {
        return mHasClipRect;
    }

    @Override
    public String toString()
    {
        return "Transformation" + toShortString();
    }

    public String toShortString()
    {
        return "{alpha=" + mAlpha + " matrix=" + mMatrix.toShortString() + "}";
    }
}

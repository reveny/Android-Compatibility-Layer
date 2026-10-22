package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class RotateDrawable extends DrawableWrapper
{
    private static final float MAX_LEVEL = 10000.0f;

    private float mFromDegrees = 0.0f;
    private float mToDegrees = 360.0f;
    private float mCurrentDegrees = 0.0f;
    private float mPivotX = 0.5f;
    private float mPivotY = 0.5f;
    private boolean mPivotXRelative = true;
    private boolean mPivotYRelative = true;

    public RotateDrawable()
    {
    }

    @Override
    public void draw(Canvas canvas)
    {
        Drawable inner = getDrawable();
        if (inner == null) return;

        Rect bounds = inner.getBounds();
        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;

        float pivotX = mPivotXRelative ? width * mPivotX : mPivotX;
        float pivotY = mPivotYRelative ? height * mPivotY : mPivotY;

        int saveCount = canvas.save();
        canvas.rotate(mCurrentDegrees, pivotX + bounds.left, pivotY + bounds.top);
        inner.setAlpha(getAlpha());
        inner.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    DrawableWrapper copyWrapper()
    {
        RotateDrawable copy = new RotateDrawable();
        copy.mFromDegrees = mFromDegrees;
        copy.mToDegrees = mToDegrees;
        copy.mCurrentDegrees = mCurrentDegrees;
        copy.mPivotX = mPivotX;
        copy.mPivotY = mPivotY;
        copy.mPivotXRelative = mPivotXRelative;
        copy.mPivotYRelative = mPivotYRelative;
        return copy;
    }

    public void setFromDegrees(float fromDegrees)
    {
        if (mFromDegrees == fromDegrees) return;

        mFromDegrees = fromDegrees;
        invalidateSelf();
    }

    public float getFromDegrees()
    {
        return mFromDegrees;
    }

    public void setToDegrees(float toDegrees)
    {
        if (mToDegrees == toDegrees) return;

        mToDegrees = toDegrees;
        invalidateSelf();
    }

    public float getToDegrees()
    {
        return mToDegrees;
    }

    public void setPivotX(float pivotX)
    {
        if (mPivotX == pivotX) return;

        mPivotX = pivotX;
        invalidateSelf();
    }

    public float getPivotX()
    {
        return mPivotX;
    }

    public void setPivotXRelative(boolean relative)
    {
        if (mPivotXRelative == relative) return;

        mPivotXRelative = relative;
        invalidateSelf();
    }

    public boolean isPivotXRelative()
    {
        return mPivotXRelative;
    }

    public void setPivotY(float pivotY)
    {
        if (mPivotY == pivotY) return;

        mPivotY = pivotY;
        invalidateSelf();
    }

    public float getPivotY()
    {
        return mPivotY;
    }

    public void setPivotYRelative(boolean relative)
    {
        if (mPivotYRelative == relative) return;

        mPivotYRelative = relative;
        invalidateSelf();
    }

    public boolean isPivotYRelative()
    {
        return mPivotYRelative;
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        Drawable inner = getDrawable();
        if (inner != null) inner.setLevel(level);

        float value = level / MAX_LEVEL;
        mCurrentDegrees = mFromDegrees + (mToDegrees - mFromDegrees) * value;

        invalidateSelf();
        return true;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        if (DrawableInflater.text(attributes, "pivotX") != null)
        {
            mPivotXRelative = DrawableInflater.isFraction(attributes, "pivotX");
            mPivotX = mPivotXRelative ? DrawableInflater.fraction(attributes, "pivotX", mPivotX)
                                      : DrawableInflater.dimensionOrFloat(resources, attributes, "pivotX", mPivotX);
        }

        if (DrawableInflater.text(attributes, "pivotY") != null)
        {
            mPivotYRelative = DrawableInflater.isFraction(attributes, "pivotY");
            mPivotY = mPivotYRelative ? DrawableInflater.fraction(attributes, "pivotY", mPivotY)
                                      : DrawableInflater.dimensionOrFloat(resources, attributes, "pivotY", mPivotY);
        }

        mFromDegrees = DrawableInflater.dimensionOrFloat(resources, attributes, "fromDegrees", mFromDegrees);
        mToDegrees = DrawableInflater.dimensionOrFloat(resources, attributes, "toDegrees", mToDegrees);
        mCurrentDegrees = mFromDegrees;

        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0) setDrawable(resources.getDrawable(reference));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
            if (child != null) setDrawable(child);
        }

        onLevelChange(getLevel());
    }
}

package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class RotateAnimation extends Animation
{
    private float mFromDegrees;
    private float mToDegrees;

    private int mPivotXType = ABSOLUTE;
    private int mPivotYType = ABSOLUTE;
    private float mPivotXValue = 0.0f;
    private float mPivotYValue = 0.0f;

    private float mPivotX;
    private float mPivotY;

    public RotateAnimation(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);
        mFromDegrees = values.getFloat("fromDegrees", 0.0f);
        mToDegrees = values.getFloat("toDegrees", 0.0f);

        Description description = values.getDescription("pivotX");
        mPivotXType = description.type;
        mPivotXValue = description.value;

        description = values.getDescription("pivotY");
        mPivotYType = description.type;
        mPivotYValue = description.value;

        initializePivotPoint();
    }

    public RotateAnimation(float fromDegrees, float toDegrees)
    {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mPivotX = 0.0f;
        mPivotY = 0.0f;
    }

    public RotateAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY)
    {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;

        mPivotXType = ABSOLUTE;
        mPivotYType = ABSOLUTE;
        mPivotXValue = pivotX;
        mPivotYValue = pivotY;
        initializePivotPoint();
    }

    public RotateAnimation(float fromDegrees, float toDegrees, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue)
    {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;

        mPivotXValue = pivotXValue;
        mPivotXType = pivotXType;
        mPivotYValue = pivotYValue;
        mPivotYType = pivotYType;
        initializePivotPoint();
    }

    private void initializePivotPoint()
    {
        if (mPivotXType == ABSOLUTE) mPivotX = mPivotXValue;
        if (mPivotYType == ABSOLUTE) mPivotY = mPivotYValue;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
        float degrees = mFromDegrees + ((mToDegrees - mFromDegrees) * interpolatedTime);
        float scale = getScaleFactor();

        if (mPivotX == 0.0f && mPivotY == 0.0f) transformation.getMatrix().setRotate(degrees);
        else transformation.getMatrix().setRotate(degrees, mPivotX * scale, mPivotY * scale);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);

        mPivotX = resolveSize(mPivotXType, mPivotXValue, width, parentWidth);
        mPivotY = resolveSize(mPivotYType, mPivotYValue, height, parentHeight);
    }
}

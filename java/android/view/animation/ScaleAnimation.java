package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class ScaleAnimation extends Animation
{
    private float mFromX;
    private float mToX;
    private float mFromY;
    private float mToY;

    private int mFromXType = ABSOLUTE;
    private int mToXType = ABSOLUTE;
    private int mFromYType = ABSOLUTE;
    private int mToYType = ABSOLUTE;

    private float mFromXValue;
    private float mToXValue;
    private float mFromYValue;
    private float mToYValue;

    private int mPivotXType = ABSOLUTE;
    private int mPivotYType = ABSOLUTE;
    private float mPivotXValue = 0.0f;
    private float mPivotYValue = 0.0f;

    private float mPivotX;
    private float mPivotY;

    public ScaleAnimation(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);

        Description description = scaleDescription(values, "fromXScale");
        mFromXType = description.type;
        mFromXValue = description.value;
        mFromX = description.value;

        description = scaleDescription(values, "toXScale");
        mToXType = description.type;
        mToXValue = description.value;
        mToX = description.value;

        description = scaleDescription(values, "fromYScale");
        mFromYType = description.type;
        mFromYValue = description.value;
        mFromY = description.value;

        description = scaleDescription(values, "toYScale");
        mToYType = description.type;
        mToYValue = description.value;
        mToY = description.value;

        description = values.getDescription("pivotX");
        mPivotXType = description.type;
        mPivotXValue = description.value;

        description = values.getDescription("pivotY");
        mPivotYType = description.type;
        mPivotYValue = description.value;

        initializePivotPoint();
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY)
    {
        mFromX = fromX;
        mToX = toX;
        mFromY = fromY;
        mToY = toY;
        mFromXValue = fromX;
        mToXValue = toX;
        mFromYValue = fromY;
        mToYValue = toY;
        mPivotX = 0;
        mPivotY = 0;
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY, float pivotX, float pivotY)
    {
        this(fromX, toX, fromY, toY);

        mPivotXType = ABSOLUTE;
        mPivotYType = ABSOLUTE;
        mPivotXValue = pivotX;
        mPivotYValue = pivotY;
        initializePivotPoint();
    }

    public ScaleAnimation(float fromX, float toX, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType,
                          float pivotYValue)
    {
        this(fromX, toX, fromY, toY);

        mPivotXType = pivotXType;
        mPivotYType = pivotYType;
        mPivotXValue = pivotXValue;
        mPivotYValue = pivotYValue;
        initializePivotPoint();
    }

    private static Description scaleDescription(AnimationAttributes values, String name)
    {
        Description description = values.getDescription(name);

        if (!values.has(name))
        {
            description.type = ABSOLUTE;
            description.value = 1.0f;
        }

        return description;
    }

    private void initializePivotPoint()
    {
        if (mPivotXType == ABSOLUTE) mPivotX = mPivotXValue;
        if (mPivotYType == ABSOLUTE) mPivotY = mPivotYValue;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float scale = getScaleFactor();

        if (mFromX != 1.0f || mToX != 1.0f) scaleX = mFromX + ((mToX - mFromX) * interpolatedTime);
        if (mFromY != 1.0f || mToY != 1.0f) scaleY = mFromY + ((mToY - mFromY) * interpolatedTime);

        if (mPivotX == 0 && mPivotY == 0) transformation.getMatrix().setScale(scaleX, scaleY);
        else transformation.getMatrix().setScale(scaleX, scaleY, scale * mPivotX, scale * mPivotY);
    }

    private static float resolveScale(int type, float value, int size, int parentSize)
    {
        if (type == ABSOLUTE) return value;
        if (size == 0) return 1.0f;

        float target = type == RELATIVE_TO_PARENT ? value * parentSize : value * size;
        return target / (float)size;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);

        mFromX = resolveScale(mFromXType, mFromXValue, width, parentWidth);
        mToX = resolveScale(mToXType, mToXValue, width, parentWidth);
        mFromY = resolveScale(mFromYType, mFromYValue, height, parentHeight);
        mToY = resolveScale(mToYType, mToYValue, height, parentHeight);

        mPivotX = resolveSize(mPivotXType, mPivotXValue, width, parentWidth);
        mPivotY = resolveSize(mPivotYType, mPivotYValue, height, parentHeight);
    }
}

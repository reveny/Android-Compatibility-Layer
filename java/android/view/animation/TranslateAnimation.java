package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class TranslateAnimation extends Animation
{
    private int mFromXType = ABSOLUTE;
    private int mToXType = ABSOLUTE;
    private int mFromYType = ABSOLUTE;
    private int mToYType = ABSOLUTE;

    private float mFromXValue = 0.0f;
    private float mToXValue = 0.0f;
    private float mFromYValue = 0.0f;
    private float mToYValue = 0.0f;

    private float mFromXDelta;
    private float mToXDelta;
    private float mFromYDelta;
    private float mToYDelta;

    public TranslateAnimation(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);

        Description description = values.getDescription("fromXDelta");
        mFromXType = description.type;
        mFromXValue = description.value;

        description = values.getDescription("toXDelta");
        mToXType = description.type;
        mToXValue = description.value;

        description = values.getDescription("fromYDelta");
        mFromYType = description.type;
        mFromYValue = description.value;

        description = values.getDescription("toYDelta");
        mToYType = description.type;
        mToYValue = description.value;
    }

    public TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta)
    {
        mFromXValue = fromXDelta;
        mToXValue = toXDelta;
        mFromYValue = fromYDelta;
        mToYValue = toYDelta;

        mFromXType = ABSOLUTE;
        mToXType = ABSOLUTE;
        mFromYType = ABSOLUTE;
        mToYType = ABSOLUTE;
    }

    public TranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue, int fromYType, float fromYValue, int toYType,
                              float toYValue)
    {
        mFromXValue = fromXValue;
        mToXValue = toXValue;
        mFromYValue = fromYValue;
        mToYValue = toYValue;

        mFromXType = fromXType;
        mToXType = toXType;
        mFromYType = fromYType;
        mToYType = toYType;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
        float deltaX = mFromXDelta;
        float deltaY = mFromYDelta;

        if (mFromXDelta != mToXDelta) deltaX = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime);
        if (mFromYDelta != mToYDelta) deltaY = mFromYDelta + ((mToYDelta - mFromYDelta) * interpolatedTime);

        transformation.getMatrix().setTranslate(deltaX, deltaY);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);

        mFromXDelta = resolveSize(mFromXType, mFromXValue, width, parentWidth);
        mToXDelta = resolveSize(mToXType, mToXValue, width, parentWidth);
        mFromYDelta = resolveSize(mFromYType, mFromYValue, height, parentHeight);
        mToYDelta = resolveSize(mToYType, mToYValue, height, parentHeight);
    }
}

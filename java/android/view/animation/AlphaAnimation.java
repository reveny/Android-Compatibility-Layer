package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AlphaAnimation extends Animation
{
    private float mFromAlpha;
    private float mToAlpha;

    public AlphaAnimation(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);
        mFromAlpha = values.getFloat("fromAlpha", 1.0f);
        mToAlpha = values.getFloat("toAlpha", 1.0f);
    }

    public AlphaAnimation(float fromAlpha, float toAlpha)
    {
        mFromAlpha = fromAlpha;
        mToAlpha = toAlpha;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
        float alpha = mFromAlpha;
        transformation.setAlpha(alpha + ((mToAlpha - alpha) * interpolatedTime));
    }

    @Override
    public boolean willChangeTransformationMatrix()
    {
        return false;
    }

    @Override
    public boolean willChangeBounds()
    {
        return false;
    }

    @Override
    public boolean hasAlpha()
    {
        return true;
    }
}

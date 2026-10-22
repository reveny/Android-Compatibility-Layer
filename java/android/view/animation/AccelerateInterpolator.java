package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AccelerateInterpolator extends BaseInterpolator
{
    private final float mFactor;
    private final double mDoubledFactor;

    public AccelerateInterpolator()
    {
        this(1.0f);
    }

    public AccelerateInterpolator(float factor)
    {
        mFactor = factor;
        mDoubledFactor = 2.0 * factor;
    }

    public AccelerateInterpolator(Context context, AttributeSet attributes)
    {
        this(new AnimationAttributes(context, attributes).getFloat("factor", 1.0f));
    }

    @Override
    public float getInterpolation(float input)
    {
        if (mFactor == 1.0f) return input * input;

        return (float)Math.pow(input, mDoubledFactor);
    }
}

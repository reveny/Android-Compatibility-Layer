package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class CycleInterpolator extends BaseInterpolator
{
    private final float mCycles;

    public CycleInterpolator(float cycles)
    {
        mCycles = cycles;
    }

    public CycleInterpolator(Context context, AttributeSet attributes)
    {
        mCycles = new AnimationAttributes(context, attributes).getFloat("cycles", 1.0f);
    }

    @Override
    public float getInterpolation(float input)
    {
        return (float)(Math.sin(2 * mCycles * Math.PI * input));
    }
}

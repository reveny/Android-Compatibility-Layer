package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AnticipateOvershootInterpolator extends BaseInterpolator
{
    private final float mTension;

    public AnticipateOvershootInterpolator()
    {
        mTension = 2.0f * 1.5f;
    }

    public AnticipateOvershootInterpolator(float tension)
    {
        mTension = tension * 1.5f;
    }

    public AnticipateOvershootInterpolator(float tension, float extraTension)
    {
        mTension = tension * extraTension;
    }

    public AnticipateOvershootInterpolator(Context context, AttributeSet attributes)
    {
        AnimationAttributes values = new AnimationAttributes(context, attributes);
        mTension = values.getFloat("tension", 2.0f) * values.getFloat("extraTension", 1.5f);
    }

    private static float anticipate(float time, float tension)
    {
        return time * time * ((tension + 1) * time - tension);
    }

    private static float overshoot(float time, float tension)
    {
        return time * time * ((tension + 1) * time + tension);
    }

    @Override
    public float getInterpolation(float input)
    {
        if (input < 0.5f) return 0.5f * anticipate(input * 2.0f, mTension);

        return 0.5f * (overshoot(input * 2.0f - 2.0f, mTension) + 2.0f);
    }
}

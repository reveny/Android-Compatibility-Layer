package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class AnticipateInterpolator extends BaseInterpolator
{
    private final float mTension;

    public AnticipateInterpolator()
    {
        this(2.0f);
    }

    public AnticipateInterpolator(float tension)
    {
        mTension = tension;
    }

    public AnticipateInterpolator(Context context, AttributeSet attributes)
    {
        this(new AnimationAttributes(context, attributes).getFloat("tension", 2.0f));
    }

    @Override
    public float getInterpolation(float input)
    {
        return input * input * ((mTension + 1.0f) * input - mTension);
    }
}

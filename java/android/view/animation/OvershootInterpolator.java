package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class OvershootInterpolator extends BaseInterpolator
{
    private final float mTension;

    public OvershootInterpolator()
    {
        this(2.0f);
    }

    public OvershootInterpolator(float tension)
    {
        mTension = tension;
    }

    public OvershootInterpolator(Context context, AttributeSet attributes)
    {
        this(new AnimationAttributes(context, attributes).getFloat("tension", 2.0f));
    }

    @Override
    public float getInterpolation(float input)
    {
        float shifted = input - 1.0f;

        return shifted * shifted * ((mTension + 1.0f) * shifted + mTension) + 1.0f;
    }
}

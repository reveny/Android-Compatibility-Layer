package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class LinearInterpolator extends BaseInterpolator
{
    public LinearInterpolator()
    {
    }

    public LinearInterpolator(Context context, AttributeSet attributes)
    {
    }

    @Override
    public float getInterpolation(float input)
    {
        return input;
    }
}

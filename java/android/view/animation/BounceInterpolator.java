package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;

public class BounceInterpolator extends BaseInterpolator
{
    public BounceInterpolator()
    {
    }

    public BounceInterpolator(Context context, AttributeSet attributes)
    {
    }

    private static float bounce(float time)
    {
        return time * time * 8.0f;
    }

    @Override
    public float getInterpolation(float input)
    {
        float time = input * 1.1226f;

        if (time < 0.3535f) return bounce(time);
        if (time < 0.7408f) return bounce(time - 0.54719f) + 0.7f;
        if (time < 0.9644f) return bounce(time - 0.8526f) + 0.9f;

        return bounce(time - 1.0435f) + 0.95f;
    }
}

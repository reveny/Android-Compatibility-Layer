package android.view.animation;

public class DecelerateInterpolator implements Interpolator
{
    private final float mFactor;

    public DecelerateInterpolator()
    {
        this(1.0f);
    }

    public DecelerateInterpolator(float factor)
    {
        mFactor = factor;
    }

    public DecelerateInterpolator(android.content.Context context, android.util.AttributeSet attributes)
    {
        this(1.0f);
    }

    @Override
    public float getInterpolation(float input)
    {
        if (mFactor == 1.0f) return 1.0f - (1.0f - input) * (1.0f - input);

        return (float)(1.0 - Math.pow(1.0 - input, 2.0 * mFactor));
    }
}

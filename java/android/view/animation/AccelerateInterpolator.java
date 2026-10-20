package android.view.animation;

public class AccelerateInterpolator implements Interpolator
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

    public AccelerateInterpolator(android.content.Context context, android.util.AttributeSet attributes)
    {
        this(1.0f);
    }

    @Override
    public float getInterpolation(float input)
    {
        if (mFactor == 1.0f) return input * input;

        return (float)Math.pow(input, mDoubledFactor);
    }
}

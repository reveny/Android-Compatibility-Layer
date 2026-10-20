package android.view.animation;

public class OvershootInterpolator implements Interpolator
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

    public OvershootInterpolator(android.content.Context context, android.util.AttributeSet attributes)
    {
        this(2.0f);
    }

    @Override
    public float getInterpolation(float input)
    {
        float shifted = input - 1.0f;

        return shifted * shifted * ((mTension + 1.0f) * shifted + mTension) + 1.0f;
    }
}

package android.view.animation;

public class AnticipateInterpolator implements Interpolator
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

    public AnticipateInterpolator(android.content.Context context, android.util.AttributeSet attributes)
    {
        this(2.0f);
    }

    @Override
    public float getInterpolation(float input)
    {
        return input * input * ((mTension + 1.0f) * input - mTension);
    }
}

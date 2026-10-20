package android.view.animation;

public class LinearInterpolator implements Interpolator
{
    @Override
    public float getInterpolation(float input)
    {
        return input;
    }
}

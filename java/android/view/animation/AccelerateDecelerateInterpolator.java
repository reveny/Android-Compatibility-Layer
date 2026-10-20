package android.view.animation;

public class AccelerateDecelerateInterpolator implements Interpolator
{
    @Override
    public float getInterpolation(float input)
    {
        return (float)(Math.cos((input+1)*Math.PI)/2.0f)+0.5f;
    }
}

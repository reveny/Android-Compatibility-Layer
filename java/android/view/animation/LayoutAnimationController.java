package android.view.animation;

public class LayoutAnimationController
{
    private Animation mAnimation;
    private float mDelay = 0.5f;

    public LayoutAnimationController()
    {
    }

    public LayoutAnimationController(Animation animation)
    {
        mAnimation = animation;
    }

    public LayoutAnimationController(Animation animation, float delay)
    {
        mAnimation = animation;
        mDelay = delay;
    }

    public Animation getAnimation()
    {
        return mAnimation;
    }

    public void setAnimation(Animation animation)
    {
        mAnimation = animation;
    }

    public float getDelay()
    {
        return mDelay;
    }

    public void setDelay(float delay)
    {
        mDelay = delay;
    }
}

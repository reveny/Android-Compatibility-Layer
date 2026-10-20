package android.view.animation;

public class Animation
{
    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;

    private long mDuration;
    private long mStartOffset;
    private int mRepeatCount;
    private Interpolator mInterpolator = new LinearInterpolator();
    private AnimationListener mListener;

    public void setDuration(long duration)
    {
        mDuration = duration;
    }

    public long getDuration()
    {
        return mDuration;
    }

    public void setStartOffset(long offset)
    {
        mStartOffset = offset;
    }

    public long getStartOffset()
    {
        return mStartOffset;
    }

    public void setRepeatCount(int count)
    {
        mRepeatCount = count;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    public void setInterpolator(Interpolator interpolator)
    {
        if (interpolator != null) mInterpolator = interpolator;
    }

    public Interpolator getInterpolator()
    {
        return mInterpolator;
    }

    public void setAnimationListener(AnimationListener listener)
    {
        mListener = listener;
    }

    public AnimationListener getAnimationListener()
    {
        return mListener;
    }

    public void reset()
    {
    }

    public void cancel()
    {
    }

    public boolean hasStarted()
    {
        return false;
    }

    public boolean hasEnded()
    {
        return true;
    }

    public interface AnimationListener
    {
        void onAnimationStart(Animation animation);

        void onAnimationEnd(Animation animation);

        void onAnimationRepeat(Animation animation);
    }
}

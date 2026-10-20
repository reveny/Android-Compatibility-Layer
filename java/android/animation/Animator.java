package android.animation;

import java.util.ArrayList;

public abstract class Animator
{
    private final ArrayList<AnimatorListener> mListeners = new ArrayList<>();

    public abstract void start();

    public abstract void cancel();

    public abstract void end();

    public abstract boolean isRunning();

    public boolean isStarted()
    {
        return isRunning();
    }

    public abstract Animator setDuration(long duration);

    public abstract long getDuration();

    public abstract void setStartDelay(long delay);

    public abstract long getStartDelay();

    public abstract void setInterpolator(TimeInterpolator interpolator);

    public void addListener(AnimatorListener listener)
    {
        if (listener != null) mListeners.add(listener);
    }

    public void removeListener(AnimatorListener listener)
    {
        mListeners.remove(listener);
    }

    public void removeAllListeners()
    {
        mListeners.clear();
    }

    private Object mTarget;

    public void setTarget(Object target)
    {
        mTarget = target;
    }

    public Object getAnimatorTarget()
    {
        return mTarget;
    }

    public void setupStartValues()
    {
    }

    public void setupEndValues()
    {
    }

    public boolean isPaused()
    {
        return false;
    }

    public void pause()
    {
    }

    public void resume()
    {
    }

    public long getTotalDuration()
    {
        return getDuration();
    }

    public TimeInterpolator getInterpolator()
    {
        return null;
    }

    public java.util.List<AnimatorListener> getListeners()
    {
        return mListeners;
    }

    protected void notifyStart()
    {
        for (int index = 0; index < mListeners.size(); index++) mListeners.get(index).onAnimationStart(this);
    }

    protected void notifyEnd()
    {
        for (int index = 0; index < mListeners.size(); index++) mListeners.get(index).onAnimationEnd(this);
    }

    protected void notifyCancel()
    {
        for (int index = 0; index < mListeners.size(); index++) mListeners.get(index).onAnimationCancel(this);
    }

    protected void notifyRepeat()
    {
        for (int index = 0; index < mListeners.size(); index++) mListeners.get(index).onAnimationRepeat(this);
    }

    public interface AnimatorListener
    {
        void onAnimationStart(Animator animation);

        void onAnimationEnd(Animator animation);

        void onAnimationCancel(Animator animation);

        void onAnimationRepeat(Animator animation);
    }

}

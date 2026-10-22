package android.animation;

import java.util.ArrayList;

public abstract class Animator implements Cloneable
{
    public static final long DURATION_INFINITE = -1;

    private ArrayList<AnimatorListener> mListeners = new ArrayList<>();
    private ArrayList<AnimatorPauseListener> mPauseListeners = new ArrayList<>();
    private boolean mPaused;
    private int mChangingConfigurations;

    public void start()
    {
    }

    public void cancel()
    {
    }

    public void end()
    {
    }

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

    public TimeInterpolator getInterpolator()
    {
        return null;
    }

    public long getTotalDuration()
    {
        long duration = getDuration();
        if (duration == DURATION_INFINITE) return DURATION_INFINITE;

        return getStartDelay() + duration;
    }

    public void addListener(AnimatorListener listener)
    {
        if (listener != null) mListeners.add(listener);
    }

    public void removeListener(AnimatorListener listener)
    {
        mListeners.remove(listener);
    }

    public ArrayList<AnimatorListener> getListeners()
    {
        return mListeners;
    }

    public void addPauseListener(AnimatorPauseListener listener)
    {
        if (listener != null) mPauseListeners.add(listener);
    }

    public void removePauseListener(AnimatorPauseListener listener)
    {
        mPauseListeners.remove(listener);
    }

    public void removeAllListeners()
    {
        mListeners.clear();
        mPauseListeners.clear();
    }

    public void setTarget(Object target)
    {
    }

    public void setupStartValues()
    {
    }

    public void setupEndValues()
    {
    }

    public boolean isPaused()
    {
        return mPaused;
    }

    public void pause()
    {
        if (!isStarted() || mPaused) return;

        mPaused = true;
        notifyPause();
    }

    public void resume()
    {
        if (!mPaused) return;

        mPaused = false;
        notifyResume();
    }

    boolean isPausedInternal()
    {
        return mPaused;
    }

    void clearPaused()
    {
        mPaused = false;
    }

    public int getChangingConfigurations()
    {
        return mChangingConfigurations;
    }

    public void setChangingConfigurations(int configs)
    {
        mChangingConfigurations = configs;
    }

    public void appendChangingConfigurations(int configs)
    {
        mChangingConfigurations |= configs;
    }

    public boolean canReverse()
    {
        return false;
    }

    public void reverse()
    {
        throw new IllegalStateException("Reverse is not supported");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Animator clone()
    {
        try
        {
            Animator copy = (Animator)super.clone();
            copy.mListeners = (ArrayList<AnimatorListener>)mListeners.clone();
            copy.mPauseListeners = (ArrayList<AnimatorPauseListener>)mPauseListeners.clone();
            copy.mPaused = false;

            return copy;
        }
        catch (CloneNotSupportedException failure)
        {
            throw new AssertionError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private ArrayList<AnimatorListener> listenerSnapshot()
    {
        return mListeners.isEmpty() ? null : (ArrayList<AnimatorListener>)mListeners.clone();
    }

    protected void notifyStart()
    {
        notifyStart(false);
    }

    void notifyStart(boolean isReverse)
    {
        ArrayList<AnimatorListener> listeners = listenerSnapshot();
        if (listeners == null) return;

        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationStart(this, isReverse);
    }

    protected void notifyEnd()
    {
        notifyEnd(false);
    }

    void notifyEnd(boolean isReverse)
    {
        ArrayList<AnimatorListener> listeners = listenerSnapshot();
        if (listeners == null) return;

        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationEnd(this, isReverse);
    }

    protected void notifyCancel()
    {
        ArrayList<AnimatorListener> listeners = listenerSnapshot();
        if (listeners == null) return;

        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationCancel(this);
    }

    protected void notifyRepeat()
    {
        ArrayList<AnimatorListener> listeners = listenerSnapshot();
        if (listeners == null) return;

        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationRepeat(this);
    }

    @SuppressWarnings("unchecked")
    void notifyPause()
    {
        if (mPauseListeners.isEmpty()) return;

        ArrayList<AnimatorPauseListener> listeners = (ArrayList<AnimatorPauseListener>)mPauseListeners.clone();
        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationPause(this);
    }

    @SuppressWarnings("unchecked")
    void notifyResume()
    {
        if (mPauseListeners.isEmpty()) return;

        ArrayList<AnimatorPauseListener> listeners = (ArrayList<AnimatorPauseListener>)mPauseListeners.clone();
        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationResume(this);
    }

    public interface AnimatorListener
    {
        default void onAnimationStart(Animator animation, boolean isReverse)
        {
            onAnimationStart(animation);
        }

        default void onAnimationEnd(Animator animation, boolean isReverse)
        {
            onAnimationEnd(animation);
        }

        void onAnimationStart(Animator animation);

        void onAnimationEnd(Animator animation);

        void onAnimationCancel(Animator animation);

        void onAnimationRepeat(Animator animation);
    }

    public interface AnimatorPauseListener
    {
        void onAnimationPause(Animator animation);

        void onAnimationResume(Animator animation);
    }
}

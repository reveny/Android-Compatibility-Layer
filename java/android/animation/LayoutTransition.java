package android.animation;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class LayoutTransition
{
    public static final int CHANGE_APPEARING = 0;
    public static final int CHANGE_DISAPPEARING = 1;
    public static final int APPEARING = 2;
    public static final int DISAPPEARING = 3;
    public static final int CHANGING = 4;

    private final List<TransitionListener> mListeners = new ArrayList<>();
    private final boolean[] mEnabled = {true, true, true, true, false};

    private long mDuration = 300;
    private long mStartDelay;

    public void setDuration(long duration)
    {
        mDuration = duration;
    }

    public void setDuration(int transitionType, long duration)
    {
        mDuration = duration;
    }

    public long getDuration(int transitionType)
    {
        return mDuration;
    }

    public void setStartDelay(int transitionType, long delay)
    {
        mStartDelay = delay;
    }

    public long getStartDelay(int transitionType)
    {
        return mStartDelay;
    }

    public void enableTransitionType(int transitionType)
    {
        if (transitionType >= 0 && transitionType < mEnabled.length) mEnabled[transitionType] = true;
    }

    public void disableTransitionType(int transitionType)
    {
        if (transitionType >= 0 && transitionType < mEnabled.length) mEnabled[transitionType] = false;
    }

    public boolean isTransitionTypeEnabled(int transitionType)
    {
        return transitionType >= 0 && transitionType < mEnabled.length && mEnabled[transitionType];
    }

    public void setAnimator(int transitionType, Animator animator)
    {
    }

    public Animator getAnimator(int transitionType)
    {
        return null;
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator)
    {
    }

    public TimeInterpolator getInterpolator(int transitionType)
    {
        return null;
    }

    public void setStagger(int transitionType, long duration)
    {
    }

    public long getStagger(int transitionType)
    {
        return 0;
    }

    public boolean isRunning()
    {
        return false;
    }

    public boolean isChangingLayout()
    {
        return false;
    }

    public void showChild(ViewGroup parent, View child)
    {
    }

    public void showChild(ViewGroup parent, View child, int oldVisibility)
    {
    }

    public void hideChild(ViewGroup parent, View child)
    {
    }

    public void hideChild(ViewGroup parent, View child, int newVisibility)
    {
    }

    public void addChild(ViewGroup parent, View child)
    {
    }

    public void removeChild(ViewGroup parent, View child)
    {
    }

    public void addTransitionListener(TransitionListener listener)
    {
        if (listener != null) mListeners.add(listener);
    }

    public void removeTransitionListener(TransitionListener listener)
    {
        mListeners.remove(listener);
    }

    public List<TransitionListener> getTransitionListeners()
    {
        return mListeners;
    }

    public interface TransitionListener
    {
        void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);

        void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);
    }
}

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

    private static final int TRANSITION_TYPE_COUNT = 5;
    private static final long DEFAULT_DURATION = 300;

    private final List<TransitionListener> mListeners = new ArrayList<>();
    private final boolean[] mEnabled = {true, true, true, true, false};
    private final long[] mDurations = {DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION, DEFAULT_DURATION};
    private final long[] mStartDelays = {0, DEFAULT_DURATION, DEFAULT_DURATION, 0, 0};
    private final long[] mStaggers = {0, 0, 0, 0, 0};
    private final Animator[] mAnimators = new Animator[TRANSITION_TYPE_COUNT];
    private final TimeInterpolator[] mInterpolators = new TimeInterpolator[TRANSITION_TYPE_COUNT];

    private boolean mAnimateParentHierarchy = true;

    public LayoutTransition()
    {
    }

    private static boolean isValid(int transitionType)
    {
        return transitionType >= 0 && transitionType < TRANSITION_TYPE_COUNT;
    }

    public void setDuration(long duration)
    {
        for (int index = 0; index < TRANSITION_TYPE_COUNT; index++) mDurations[index] = duration;
    }

    public void setDuration(int transitionType, long duration)
    {
        if (isValid(transitionType)) mDurations[transitionType] = duration;
    }

    public long getDuration(int transitionType)
    {
        return isValid(transitionType) ? mDurations[transitionType] : 0;
    }

    public void setStartDelay(int transitionType, long delay)
    {
        if (isValid(transitionType)) mStartDelays[transitionType] = delay;
    }

    public long getStartDelay(int transitionType)
    {
        return isValid(transitionType) ? mStartDelays[transitionType] : 0;
    }

    public void enableTransitionType(int transitionType)
    {
        if (isValid(transitionType)) mEnabled[transitionType] = true;
    }

    public void disableTransitionType(int transitionType)
    {
        if (isValid(transitionType)) mEnabled[transitionType] = false;
    }

    public boolean isTransitionTypeEnabled(int transitionType)
    {
        return isValid(transitionType) && mEnabled[transitionType];
    }

    public void setAnimator(int transitionType, Animator animator)
    {
        if (isValid(transitionType)) mAnimators[transitionType] = animator;
    }

    public Animator getAnimator(int transitionType)
    {
        return isValid(transitionType) ? mAnimators[transitionType] : null;
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator)
    {
        if (isValid(transitionType)) mInterpolators[transitionType] = interpolator;
    }

    public TimeInterpolator getInterpolator(int transitionType)
    {
        return isValid(transitionType) ? mInterpolators[transitionType] : null;
    }

    public void setStagger(int transitionType, long duration)
    {
        if (isValid(transitionType)) mStaggers[transitionType] = duration;
    }

    public long getStagger(int transitionType)
    {
        return isValid(transitionType) ? mStaggers[transitionType] : 0;
    }

    public void setAnimateParentHierarchy(boolean animateParentHierarchy)
    {
        mAnimateParentHierarchy = animateParentHierarchy;
    }

    boolean isAnimateParentHierarchy()
    {
        return mAnimateParentHierarchy;
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
        showChild(parent, child, View.GONE);
    }

    public void showChild(ViewGroup parent, View child, int oldVisibility)
    {
        notifyInstant(parent, child, APPEARING);
    }

    public void hideChild(ViewGroup parent, View child)
    {
        hideChild(parent, child, View.GONE);
    }

    public void hideChild(ViewGroup parent, View child, int newVisibility)
    {
        notifyInstant(parent, child, DISAPPEARING);
    }

    public void addChild(ViewGroup parent, View child)
    {
        notifyInstant(parent, child, APPEARING);
    }

    public void removeChild(ViewGroup parent, View child)
    {
        notifyInstant(parent, child, DISAPPEARING);
    }

    private void notifyInstant(ViewGroup parent, View child, int transitionType)
    {
        if (!isTransitionTypeEnabled(transitionType) || mListeners.isEmpty()) return;

        ArrayList<TransitionListener> listeners = new ArrayList<>(mListeners);

        for (TransitionListener listener : listeners) listener.startTransition(this, parent, child, transitionType);
        for (TransitionListener listener : listeners) listener.endTransition(this, parent, child, transitionType);
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

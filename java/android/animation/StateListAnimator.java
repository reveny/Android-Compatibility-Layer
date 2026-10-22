package android.animation;

import android.util.StateSet;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class StateListAnimator implements Cloneable
{
    private ArrayList<Tuple> mTuples = new ArrayList<>();
    private Tuple mLastMatch;
    private Animator mRunningAnimator;
    private WeakReference<View> mViewRef;
    private int mChangingConfigurations;

    private Animator.AnimatorListener mAnimatorListener = createListener();

    public StateListAnimator()
    {
    }

    private Animator.AnimatorListener createListener()
    {
        return new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                animation.setTarget(null);
                if (mRunningAnimator == animation) mRunningAnimator = null;
            }
        };
    }

    public void addState(int[] specs, Animator animator)
    {
        Tuple tuple = new Tuple(specs, animator);
        animator.addListener(mAnimatorListener);
        mTuples.add(tuple);
    }

    public Animator getRunningAnimator()
    {
        return mRunningAnimator;
    }

    public View getTarget()
    {
        return mViewRef == null ? null : mViewRef.get();
    }

    public void setTarget(View view)
    {
        View current = getTarget();
        if (current == view) return;

        if (current != null) clearTarget();
        if (view != null) mViewRef = new WeakReference<>(view);
    }

    private void clearTarget()
    {
        for (int index = 0; index < mTuples.size(); index++)
        {
            mTuples.get(index).mAnimator.setTarget(null);
        }

        mViewRef = null;
        mLastMatch = null;
        mRunningAnimator = null;
    }

    public void setState(int[] state)
    {
        Tuple match = null;

        for (int index = 0; index < mTuples.size(); index++)
        {
            Tuple tuple = mTuples.get(index);
            if (StateSet.stateSetMatches(tuple.mSpecs, state))
            {
                match = tuple;
                break;
            }
        }

        if (match == mLastMatch) return;

        if (mLastMatch != null) cancel();

        mLastMatch = match;

        if (match != null) start(match);
    }

    private void start(Tuple match)
    {
        match.mAnimator.setTarget(getTarget());
        mRunningAnimator = match.mAnimator;
        mRunningAnimator.start();
    }

    private void cancel()
    {
        if (mRunningAnimator == null) return;

        mRunningAnimator.cancel();
        mRunningAnimator = null;
    }

    public void jumpToCurrentState()
    {
        if (mRunningAnimator != null) mRunningAnimator.end();
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

    @Override
    public StateListAnimator clone()
    {
        try
        {
            StateListAnimator clone = (StateListAnimator)super.clone();
            clone.mTuples = new ArrayList<>(mTuples.size());
            clone.mLastMatch = null;
            clone.mRunningAnimator = null;
            clone.mViewRef = null;
            clone.mAnimatorListener = clone.createListener();

            for (int index = 0; index < mTuples.size(); index++)
            {
                Tuple tuple = mTuples.get(index);
                clone.addState(tuple.mSpecs, tuple.mAnimator);
            }

            return clone;
        }
        catch (CloneNotSupportedException exception)
        {
            throw new AssertionError("cannot clone state list animator", exception);
        }
    }

    public ArrayList<Tuple> getTuples()
    {
        return mTuples;
    }

    public static class Tuple
    {
        final int[] mSpecs;
        final Animator mAnimator;

        Tuple(int[] specs, Animator animator)
        {
            mSpecs = specs;
            mAnimator = animator;
        }

        public int[] getSpecs()
        {
            return mSpecs;
        }

        public Animator getAnimator()
        {
            return mAnimator;
        }
    }
}

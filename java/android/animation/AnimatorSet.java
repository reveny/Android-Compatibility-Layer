package android.animation;

import java.util.ArrayList;
import java.util.List;

public final class AnimatorSet extends Animator
{
    private final ArrayList<Animator> mTogether = new ArrayList<>();
    private final ArrayList<Animator> mSequence = new ArrayList<>();

    private long mDuration = -1;
    private long mStartDelay;
    private TimeInterpolator mInterpolator;

    private boolean mRunning;
    private int mPending;
    private int mSequenceIndex;

    private final AnimatorListener mChildListener = new AnimatorListener()
    {
        @Override
        public void onAnimationStart(Animator animation)
        {
        }

        @Override
        public void onAnimationEnd(Animator animation)
        {
            childFinished();
        }

        @Override
        public void onAnimationCancel(Animator animation)
        {
        }

        @Override
        public void onAnimationRepeat(Animator animation)
        {
        }
    };

    public void playTogether(Animator... animators)
    {
        if (animators == null) return;

        for (Animator animator : animators)
        {
            if (animator != null) mTogether.add(animator);
        }
    }

    public void playTogether(java.util.Collection<Animator> animators)
    {
        if (animators == null) return;

        for (Animator animator : animators)
        {
            if (animator != null) mTogether.add(animator);
        }
    }

    public void playSequentially(Animator... animators)
    {
        if (animators == null) return;

        for (Animator animator : animators)
        {
            if (animator != null) mSequence.add(animator);
        }
    }

    public void playSequentially(List<Animator> animators)
    {
        if (animators == null) return;

        for (Animator animator : animators)
        {
            if (animator != null) mSequence.add(animator);
        }
    }

    public Builder play(Animator animator)
    {
        if (animator != null) mTogether.add(animator);

        return new Builder(animator);
    }

    public List<Animator> getChildAnimations()
    {
        ArrayList<Animator> all = new ArrayList<>(mTogether);
        all.addAll(mSequence);

        return all;
    }

    @Override
    public void start()
    {
        if (mRunning) return;

        mRunning = true;
        mSequenceIndex = 0;
        mPending = 0;

        notifyStart();

        for (Animator animator : mTogether) prepare(animator);
        for (Animator animator : mSequence) prepare(animator);

        mPending = mTogether.size() + (mSequence.isEmpty() ? 0 : 1);

        if (mPending == 0)
        {
            mRunning = false;
            notifyEnd();

            return;
        }

        for (Animator animator : mTogether) animator.start();

        startNextInSequence();
    }

    private void prepare(Animator animator)
    {
        if (mDuration >= 0) animator.setDuration(mDuration);
        if (mInterpolator != null) animator.setInterpolator(mInterpolator);

        animator.removeListener(mChildListener);
        animator.addListener(mChildListener);
    }

    private void startNextInSequence()
    {
        if (mSequenceIndex >= mSequence.size()) return;

        mSequence.get(mSequenceIndex++).start();
    }

    private void childFinished()
    {
        if (!mRunning) return;

        if (mSequenceIndex > 0 && mSequenceIndex <= mSequence.size() && mSequenceIndex < mSequence.size())
        {
            startNextInSequence();
            return;
        }

        mPending--;

        if (mPending > 0) return;

        mRunning = false;
        notifyEnd();
    }

    @Override
    public void cancel()
    {
        if (!mRunning) return;

        mRunning = false;

        for (Animator animator : mTogether) animator.cancel();
        for (Animator animator : mSequence) animator.cancel();

        notifyCancel();
        notifyEnd();
    }

    @Override
    public void end()
    {
        if (!mRunning) return;

        mRunning = false;

        for (Animator animator : mTogether) animator.end();
        for (Animator animator : mSequence) animator.end();

        notifyEnd();
    }

    @Override
    public boolean isRunning()
    {
        return mRunning;
    }

    @Override
    public AnimatorSet setDuration(long duration)
    {
        mDuration = duration;
        return this;
    }

    @Override
    public long getDuration()
    {
        return mDuration;
    }

    @Override
    public void setStartDelay(long delay)
    {
        mStartDelay = delay;
    }

    @Override
    public long getStartDelay()
    {
        return mStartDelay;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
    }

    public final class Builder
    {
        private final Animator mAnchor;

        Builder(Animator anchor)
        {
            mAnchor = anchor;
        }

        public Builder with(Animator animator)
        {
            if (animator != null) mTogether.add(animator);

            return this;
        }

        public Builder before(Animator animator)
        {
            if (animator != null) mSequence.add(animator);

            return this;
        }

        public Builder after(Animator animator)
        {
            if (animator != null) mTogether.add(animator);

            return this;
        }

        public Builder after(long delay)
        {
            if (mAnchor != null) mAnchor.setStartDelay(delay);

            return this;
        }
    }
}

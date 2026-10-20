package android.view;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;

import java.util.ArrayList;

public class ViewPropertyAnimator
{
    private static final int ALPHA = 0;
    private static final int TRANSLATION_X = 1;
    private static final int TRANSLATION_Y = 2;
    private static final int SCALE_X = 3;
    private static final int SCALE_Y = 4;
    private static final int ROTATION = 5;
    private static final int X = 6;
    private static final int Y = 7;

    private static final class Change
    {
        final int property;
        final float target;
        final boolean relative;

        Change(int property, float target, boolean relative)
        {
            this.property = property;
            this.target = target;
            this.relative = relative;
        }
    }

    private final View mView;
    private final ArrayList<Change> mChanges = new ArrayList<>();

    private long mDuration = 300;
    private long mStartDelay;
    private boolean mDurationSet;
    private TimeInterpolator mInterpolator;
    private Runnable mStartAction;
    private Runnable mEndAction;
    private Animator.AnimatorListener mListener;
    private ValueAnimator mAnimator;
    private boolean mPending;

    ViewPropertyAnimator(View view)
    {
        mView = view;
    }

    public ViewPropertyAnimator setDuration(long duration)
    {
        mDuration = duration;
        mDurationSet = true;
        return this;
    }

    public long getDuration()
    {
        return mDuration;
    }

    public ViewPropertyAnimator setStartDelay(long delay)
    {
        mStartDelay = delay;
        return this;
    }

    public long getStartDelay()
    {
        return mStartDelay;
    }

    public ViewPropertyAnimator setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public ViewPropertyAnimator setListener(Animator.AnimatorListener listener)
    {
        mListener = listener;
        return this;
    }

    public ViewPropertyAnimator setUpdateListener(ValueAnimator.AnimatorUpdateListener listener)
    {
        return this;
    }

    public ViewPropertyAnimator withStartAction(Runnable action)
    {
        mStartAction = action;
        return this;
    }

    public ViewPropertyAnimator withEndAction(Runnable action)
    {
        mEndAction = action;
        return this;
    }

    public ViewPropertyAnimator withLayer()
    {
        return this;
    }

    public ViewPropertyAnimator alpha(float value)
    {
        return add(ALPHA, value, false);
    }

    public ViewPropertyAnimator alphaBy(float value)
    {
        return add(ALPHA, value, true);
    }

    public ViewPropertyAnimator translationX(float value)
    {
        return add(TRANSLATION_X, value, false);
    }

    public ViewPropertyAnimator translationXBy(float value)
    {
        return add(TRANSLATION_X, value, true);
    }

    public ViewPropertyAnimator translationY(float value)
    {
        return add(TRANSLATION_Y, value, false);
    }

    public ViewPropertyAnimator translationYBy(float value)
    {
        return add(TRANSLATION_Y, value, true);
    }

    public ViewPropertyAnimator scaleX(float value)
    {
        return add(SCALE_X, value, false);
    }

    public ViewPropertyAnimator scaleXBy(float value)
    {
        return add(SCALE_X, value, true);
    }

    public ViewPropertyAnimator scaleY(float value)
    {
        return add(SCALE_Y, value, false);
    }

    public ViewPropertyAnimator scaleYBy(float value)
    {
        return add(SCALE_Y, value, true);
    }

    public ViewPropertyAnimator rotation(float value)
    {
        return add(ROTATION, value, false);
    }

    public ViewPropertyAnimator rotationBy(float value)
    {
        return add(ROTATION, value, true);
    }

    public ViewPropertyAnimator x(float value)
    {
        return add(X, value, false);
    }

    public ViewPropertyAnimator y(float value)
    {
        return add(Y, value, false);
    }

    private ViewPropertyAnimator add(int property, float target, boolean relative)
    {
        mChanges.add(new Change(property, target, relative));
        schedule();

        return this;
    }

    private void schedule()
    {
        if (mPending) return;

        mPending = true;

        mView.post(new Runnable()
        {
            @Override
            public void run()
            {
                mPending = false;
                start();
            }
        });
    }

    public void start()
    {
        if (mChanges.isEmpty()) return;

        cancel();

        final float[] from = new float[mChanges.size()];
        final float[] to = new float[mChanges.size()];

        for (int index = 0; index < mChanges.size(); index++)
        {
            Change change = mChanges.get(index);

            from[index] = read(change.property);
            to[index] = change.relative ? from[index] + change.target : change.target;
        }

        final ArrayList<Change> changes = new ArrayList<>(mChanges);
        mChanges.clear();

        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);

        animator.setDuration(mDuration);
        animator.setStartDelay(mStartDelay);

        if (mInterpolator != null) animator.setInterpolator(mInterpolator);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float fraction = ((Float)animation.getAnimatedValue()).floatValue();

                for (int index = 0; index < changes.size(); index++)
                {
                    write(changes.get(index).property, from[index] + (to[index] - from[index]) * fraction);
                }
            }
        });

        final Runnable startAction = mStartAction;
        final Runnable endAction = mEndAction;
        final Animator.AnimatorListener listener = mListener;

        animator.addListener(new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                if (startAction != null) startAction.run();
                if (listener != null) listener.onAnimationStart(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                for (int index = 0; index < changes.size(); index++) write(changes.get(index).property, to[index]);

                if (endAction != null) endAction.run();
                if (listener != null) listener.onAnimationEnd(animation);

                mAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {
                if (listener != null) listener.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationRepeat(Animator animation)
            {
                if (listener != null) listener.onAnimationRepeat(animation);
            }
        });

        mAnimator = animator;
        animator.start();
    }

    public void cancel()
    {
        if (mAnimator == null) return;

        ValueAnimator running = mAnimator;
        mAnimator = null;
        running.cancel();
    }

    private float read(int property)
    {
        if (property == ALPHA) return mView.getAlpha();
        if (property == TRANSLATION_X) return mView.getTranslationX();
        if (property == TRANSLATION_Y) return mView.getTranslationY();
        if (property == SCALE_X) return mView.getScaleX();
        if (property == SCALE_Y) return mView.getScaleY();
        if (property == ROTATION) return mView.getRotation();
        if (property == X) return mView.getX();
        if (property == Y) return mView.getY();

        return 0;
    }

    private void write(int property, float value)
    {
        if (property == ALPHA) mView.setAlpha(value);
        else if (property == TRANSLATION_X) mView.setTranslationX(value);
        else if (property == TRANSLATION_Y) mView.setTranslationY(value);
        else if (property == SCALE_X) mView.setScaleX(value);
        else if (property == SCALE_Y) mView.setScaleY(value);
        else if (property == ROTATION) mView.setRotation(value);
        else if (property == X) mView.setX(value);
        else if (property == Y) mView.setY(value);
    }
}

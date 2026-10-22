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
    private static final int TRANSLATION_Z = 3;
    private static final int SCALE_X = 4;
    private static final int SCALE_Y = 5;
    private static final int ROTATION = 6;
    private static final int ROTATION_X = 7;
    private static final int ROTATION_Y = 8;
    private static final int X = 9;
    private static final int Y = 10;
    private static final int Z = 11;

    private static final class Change
    {
        final int property;
        final float target;
        final boolean relative;
        float from;
        float to;

        Change(int property, float target, boolean relative)
        {
            this.property = property;
            this.target = target;
            this.relative = relative;
        }
    }

    private final class Running
    {
        final ValueAnimator animator;
        final ArrayList<Change> changes;
        boolean canceled;

        Running(ValueAnimator animator, ArrayList<Change> changes)
        {
            this.animator = animator;
            this.changes = changes;
        }
    }

    private final View mView;
    private final ArrayList<Change> mChanges = new ArrayList<>();
    private final ArrayList<Running> mRunning = new ArrayList<>();

    private long mDuration;
    private boolean mDurationSet;
    private long mStartDelay;
    private TimeInterpolator mInterpolator;
    private boolean mInterpolatorSet;
    private Runnable mStartAction;
    private Runnable mEndAction;
    private boolean mWithLayer;
    private Animator.AnimatorListener mListener;
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private boolean mPending;

    private final Runnable mStarter = new Runnable()
    {
        @Override
        public void run()
        {
            mPending = false;
            startAnimation();
        }
    };

    ViewPropertyAnimator(View view)
    {
        mView = view;
    }

    public ViewPropertyAnimator setDuration(long duration)
    {
        if (duration < 0) throw new IllegalArgumentException("Animators cannot have negative duration: " + duration);

        mDuration = duration;
        mDurationSet = true;
        return this;
    }

    public long getDuration()
    {
        if (mDurationSet) return mDuration;

        return new ValueAnimator().getDuration();
    }

    public ViewPropertyAnimator setStartDelay(long startDelay)
    {
        if (startDelay < 0) throw new IllegalArgumentException("Animators cannot have negative start delay: " + startDelay);

        mStartDelay = startDelay;
        return this;
    }

    public long getStartDelay()
    {
        return mStartDelay;
    }

    public ViewPropertyAnimator setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
        mInterpolatorSet = true;
        return this;
    }

    public TimeInterpolator getInterpolator()
    {
        if (mInterpolatorSet) return mInterpolator;

        return new ValueAnimator().getInterpolator();
    }

    public ViewPropertyAnimator setListener(Animator.AnimatorListener listener)
    {
        mListener = listener;
        return this;
    }

    public ViewPropertyAnimator setUpdateListener(ValueAnimator.AnimatorUpdateListener listener)
    {
        mUpdateListener = listener;
        return this;
    }

    public ViewPropertyAnimator withStartAction(Runnable runnable)
    {
        mStartAction = runnable;
        return this;
    }

    public ViewPropertyAnimator withEndAction(Runnable runnable)
    {
        mEndAction = runnable;
        return this;
    }

    public ViewPropertyAnimator withLayer()
    {
        mWithLayer = true;
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

    public ViewPropertyAnimator translationZ(float value)
    {
        return add(TRANSLATION_Z, value, false);
    }

    public ViewPropertyAnimator translationZBy(float value)
    {
        return add(TRANSLATION_Z, value, true);
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

    public ViewPropertyAnimator rotationX(float value)
    {
        return add(ROTATION_X, value, false);
    }

    public ViewPropertyAnimator rotationXBy(float value)
    {
        return add(ROTATION_X, value, true);
    }

    public ViewPropertyAnimator rotationY(float value)
    {
        return add(ROTATION_Y, value, false);
    }

    public ViewPropertyAnimator rotationYBy(float value)
    {
        return add(ROTATION_Y, value, true);
    }

    public ViewPropertyAnimator x(float value)
    {
        return add(X, value, false);
    }

    public ViewPropertyAnimator xBy(float value)
    {
        return add(X, value, true);
    }

    public ViewPropertyAnimator y(float value)
    {
        return add(Y, value, false);
    }

    public ViewPropertyAnimator yBy(float value)
    {
        return add(Y, value, true);
    }

    public ViewPropertyAnimator z(float value)
    {
        return add(Z, value, false);
    }

    public ViewPropertyAnimator zBy(float value)
    {
        return add(Z, value, true);
    }

    private ViewPropertyAnimator add(int property, float target, boolean relative)
    {
        claimFromRunning(property);

        for (int index = mChanges.size() - 1; index >= 0; index--)
        {
            if (mChanges.get(index).property == property) mChanges.remove(index);
        }

        Change change = new Change(property, target, relative);
        change.from = read(property);
        change.to = relative ? change.from + target : target;

        mChanges.add(change);
        schedule();

        return this;
    }

    private void claimFromRunning(int property)
    {
        for (int index = mRunning.size() - 1; index >= 0; index--)
        {
            Running running = mRunning.get(index);

            for (int change = running.changes.size() - 1; change >= 0; change--)
            {
                if (running.changes.get(change).property == property) running.changes.remove(change);
            }

            if (running.changes.isEmpty())
            {
                running.canceled = true;
                mRunning.remove(index);
                running.animator.cancel();
            }
        }
    }

    private void schedule()
    {
        if (mPending) return;

        mPending = true;
        mView.removeCallbacks(mStarter);
        mView.postOnAnimation(mStarter);
    }

    public void start()
    {
        mView.removeCallbacks(mStarter);
        mPending = false;
        startAnimation();
    }

    private void startAnimation()
    {
        if (mChanges.isEmpty() && mStartAction == null && mEndAction == null) return;

        final ArrayList<Change> changes = new ArrayList<>(mChanges);
        mChanges.clear();

        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        if (mDurationSet) animator.setDuration(mDuration);
        if (mInterpolatorSet) animator.setInterpolator(mInterpolator);
        animator.setStartDelay(mStartDelay);

        final Running running = new Running(animator, changes);
        final Runnable startAction = mStartAction;
        final Runnable endAction = mEndAction;
        final boolean withLayer = mWithLayer;
        final int previousLayerType = mView.getLayerType();
        final Animator.AnimatorListener listener = mListener;
        final ValueAnimator.AnimatorUpdateListener updateListener = mUpdateListener;

        mStartAction = null;
        mEndAction = null;
        mWithLayer = false;

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float fraction = animation.getAnimatedFraction();

                for (int index = 0; index < running.changes.size(); index++)
                {
                    Change change = running.changes.get(index);
                    write(change.property, change.from + (change.to - change.from) * fraction);
                }

                mView.invalidate();
                if (updateListener != null) updateListener.onAnimationUpdate(animation);
            }
        });

        animator.addListener(new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                if (withLayer) mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                if (startAction != null) startAction.run();
                if (listener != null) listener.onAnimationStart(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                mRunning.remove(running);

                if (!running.canceled)
                {
                    for (Change change : running.changes) write(change.property, change.to);
                }

                if (withLayer) mView.setLayerType(previousLayerType, null);
                if (listener != null) listener.onAnimationEnd(animation);
                if (!running.canceled && endAction != null) endAction.run();
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {
                running.canceled = true;
                if (listener != null) listener.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationRepeat(Animator animation)
            {
                if (listener != null) listener.onAnimationRepeat(animation);
            }
        });

        mRunning.add(running);
        animator.start();
    }

    public void cancel()
    {
        mView.removeCallbacks(mStarter);
        mPending = false;
        mChanges.clear();

        ArrayList<Running> running = new ArrayList<>(mRunning);
        mRunning.clear();

        for (Running entry : running)
        {
            entry.canceled = true;
            entry.animator.cancel();
        }
    }

    private float read(int property)
    {
        switch (property)
        {
            case ALPHA:
                return mView.getAlpha();
            case TRANSLATION_X:
                return mView.getTranslationX();
            case TRANSLATION_Y:
                return mView.getTranslationY();
            case TRANSLATION_Z:
                return mView.getTranslationZ();
            case SCALE_X:
                return mView.getScaleX();
            case SCALE_Y:
                return mView.getScaleY();
            case ROTATION:
                return mView.getRotation();
            case ROTATION_X:
                return mView.getRotationX();
            case ROTATION_Y:
                return mView.getRotationY();
            case X:
                return mView.getX();
            case Y:
                return mView.getY();
            case Z:
                return mView.getZ();
            default:
                return 0;
        }
    }

    private void write(int property, float value)
    {
        switch (property)
        {
            case ALPHA:
                mView.setAlpha(value);
                break;
            case TRANSLATION_X:
                mView.setTranslationX(value);
                break;
            case TRANSLATION_Y:
                mView.setTranslationY(value);
                break;
            case TRANSLATION_Z:
                mView.setTranslationZ(value);
                break;
            case SCALE_X:
                mView.setScaleX(value);
                break;
            case SCALE_Y:
                mView.setScaleY(value);
                break;
            case ROTATION:
                mView.setRotation(value);
                break;
            case ROTATION_X:
                mView.setRotationX(value);
                break;
            case ROTATION_Y:
                mView.setRotationY(value);
                break;
            case X:
                mView.setX(value);
                break;
            case Y:
                mView.setY(value);
                break;
            case Z:
                mView.setZ(value);
                break;
            default:
                break;
        }
    }
}

package android.animation;

import android.view.Choreographer;

import java.util.ArrayList;

public class ValueAnimator extends Animator implements Choreographer.FrameCallback
{
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int INFINITE = -1;

    private static float sDurationScale = 1.0f;

    private final ArrayList<AnimatorUpdateListener> mUpdateListeners = new ArrayList<>();

    private long mDuration = 300;
    private long mStartDelay;
    private long mStartTimeNanos;
    private boolean mRunning;
    private boolean mStarted;
    private int mRepeatCount;
    private int mRepeatMode = RESTART;
    private int mRepeatsDone;
    private float mFraction;
    private float mFrom;
    private float mTo = 1.0f;
    private float[] mKeyframes;
    private boolean mIntegral;
    private TimeInterpolator mInterpolator = new LinearInterpolator();
    private Object mAnimatedValue = Float.valueOf(0);

    public static ValueAnimator ofFloat(float... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(values);

        return animator;
    }

    public static ValueAnimator ofInt(int... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(values);

        return animator;
    }

    public static float getDurationScale()
    {
        return sDurationScale;
    }

    public static void setDurationScale(float scale)
    {
        sDurationScale = scale;
    }

    public static boolean areAnimatorsEnabled()
    {
        return sDurationScale != 0;
    }

    public void setFloatValues(float... values)
    {
        mIntegral = false;

        if (values == null || values.length == 0) return;

        if (values.length == 1)
        {
            mKeyframes = new float[] {0, values[0]};
        }
        else
        {
            mKeyframes = values.clone();
        }

        mFrom = mKeyframes[0];
        mTo = mKeyframes[mKeyframes.length - 1];
    }

    public void setIntValues(int... values)
    {
        mIntegral = true;

        if (values == null || values.length == 0) return;

        if (values.length == 1)
        {
            mKeyframes = new float[] {0, values[0]};
        }
        else
        {
            mKeyframes = new float[values.length];
            for (int index = 0; index < values.length; index++) mKeyframes[index] = values[index];
        }

        mFrom = mKeyframes[0];
        mTo = mKeyframes[mKeyframes.length - 1];
    }

    protected float valueAt(float eased)
    {
        if (mKeyframes == null || mKeyframes.length < 2) return mFrom + (mTo - mFrom) * eased;

        int segments = mKeyframes.length - 1;
        float scaled = eased * segments;

        int segment = (int)scaled;
        if (segment >= segments) segment = segments - 1;
        if (segment < 0) segment = 0;

        float local = scaled - segment;

        return mKeyframes[segment] + (mKeyframes[segment + 1] - mKeyframes[segment]) * local;
    }

    @Override
    public ValueAnimator setDuration(long duration)
    {
        mDuration = Math.max(0, duration);
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
        mStartDelay = Math.max(0, delay);
    }

    @Override
    public long getStartDelay()
    {
        return mStartDelay;
    }

    public void setRepeatCount(int count)
    {
        mRepeatCount = count;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    public void setRepeatMode(int mode)
    {
        mRepeatMode = mode;
    }

    public int getRepeatMode()
    {
        return mRepeatMode;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator == null ? new LinearInterpolator() : interpolator;
    }

    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public float getAnimatedFraction()
    {
        return mFraction;
    }

    public Object getAnimatedValue()
    {
        return mAnimatedValue;
    }

    public void addUpdateListener(AnimatorUpdateListener listener)
    {
        if (listener != null) mUpdateListeners.add(listener);
    }

    public void removeUpdateListener(AnimatorUpdateListener listener)
    {
        mUpdateListeners.remove(listener);
    }

    public void removeAllUpdateListeners()
    {
        mUpdateListeners.clear();
    }

    @Override
    public boolean isRunning()
    {
        return mRunning;
    }

    @Override
    public boolean isStarted()
    {
        return mStarted;
    }

    @Override
    public void start()
    {
        if (mStarted) return;

        mStarted = true;
        mRunning = true;
        mRepeatsDone = 0;
        mFraction = 0;
        mStartTimeNanos = 0;

        notifyStart();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void cancel()
    {
        if (!mStarted) return;

        finish(true);
    }

    @Override
    public void end()
    {
        if (!mStarted) return;

        apply(1.0f);
        finish(false);
    }

    private void finish(boolean cancelled)
    {
        mStarted = false;
        mRunning = false;

        Choreographer.getInstance().removeFrameCallback(this);

        if (cancelled) notifyCancel();
        notifyEnd();
    }

    @Override
    public void doFrame(long frameTimeNanos)
    {
        if (!mRunning) return;

        if (mStartTimeNanos == 0) mStartTimeNanos = frameTimeNanos;

        long elapsed = (frameTimeNanos - mStartTimeNanos) / 1000000L - mStartDelay;

        if (elapsed < 0)
        {
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        long scaled = (long)(mDuration * sDurationScale);

        float fraction = scaled <= 0 ? 1.0f : Math.min(1.0f, elapsed / (float)scaled);
        boolean finished = fraction >= 1.0f;

        if (finished && (mRepeatCount == INFINITE || mRepeatsDone < mRepeatCount))
        {
            mRepeatsDone++;
            mStartTimeNanos = frameTimeNanos;
            notifyRepeat();

            apply(mRepeatMode == REVERSE && mRepeatsDone % 2 == 1 ? 0.0f : 1.0f);
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        if (mRepeatMode == REVERSE && mRepeatsDone % 2 == 1) fraction = 1.0f - fraction;

        apply(fraction);

        if (finished)
        {
            finish(false);
            return;
        }

        Choreographer.getInstance().postFrameCallback(this);
    }

    protected void apply(float fraction)
    {
        mFraction = fraction;

        float eased = mInterpolator.getInterpolation(fraction);
        float value = valueAt(eased);

        mAnimatedValue = mIntegral ? (Object)Integer.valueOf(Math.round(value)) : (Object)Float.valueOf(value);

        onAnimatedValue(value);

        for (int index = 0; index < mUpdateListeners.size(); index++)
        {
            mUpdateListeners.get(index).onAnimationUpdate(this);
        }
    }

    protected void onAnimatedValue(float value)
    {
    }

    public interface AnimatorUpdateListener
    {
        void onAnimationUpdate(ValueAnimator animation);
    }
}

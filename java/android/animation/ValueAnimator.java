package android.animation;

import android.view.Choreographer;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashMap;

public class ValueAnimator extends Animator
{
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int INFINITE = -1;

    private static final TimeInterpolator sDefaultInterpolator = new AccelerateDecelerateInterpolator();
    private static final ArrayList<DurationScaleChangeListener> sDurationScaleListeners = new ArrayList<>();

    private static float sDurationScale = 1.0f;
    private static long sFrameDelay = 10;

    PropertyValuesHolder[] mValues;
    HashMap<String, PropertyValuesHolder> mValuesMap;
    boolean mInitialized;

    private ArrayList<AnimatorUpdateListener> mUpdateListeners = new ArrayList<>();
    private Choreographer.FrameCallback mFrameCallback = createFrameCallback();
    private Choreographer mChoreographer;

    private long mDuration = 300;
    private long mStartDelay;
    private int mRepeatCount;
    private int mRepeatMode = RESTART;
    private TimeInterpolator mInterpolator = sDefaultInterpolator;

    private long mStartTime = -1;
    private long mLastFrameTime = -1;
    private long mPauseNanos = -1;
    private float mSeekFraction = -1;
    private float mOverallFraction;
    private float mCurrentFraction;
    private boolean mStarted;
    private boolean mRunning;
    private boolean mReversing;
    private boolean mCallbackPosted;

    public ValueAnimator()
    {
    }

    public static ValueAnimator ofInt(int... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(values);

        return animator;
    }

    public static ValueAnimator ofArgb(int... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setIntValues(values);
        animator.setEvaluator(ArgbEvaluator.getInstance());

        return animator;
    }

    public static ValueAnimator ofFloat(float... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setFloatValues(values);

        return animator;
    }

    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setValues(values);

        return animator;
    }

    public static ValueAnimator ofObject(TypeEvaluator evaluator, Object... values)
    {
        ValueAnimator animator = new ValueAnimator();
        animator.setObjectValues(values);
        animator.setEvaluator(evaluator);

        return animator;
    }

    public static float getDurationScale()
    {
        return sDurationScale;
    }

    public static void setDurationScale(float scale)
    {
        sDurationScale = scale;

        ArrayList<DurationScaleChangeListener> listeners;

        synchronized (sDurationScaleListeners)
        {
            listeners = new ArrayList<>(sDurationScaleListeners);
        }

        for (DurationScaleChangeListener listener : listeners) listener.onChanged(scale);
    }

    public static boolean registerDurationScaleChangeListener(DurationScaleChangeListener listener)
    {
        if (listener == null) return false;

        synchronized (sDurationScaleListeners)
        {
            if (sDurationScaleListeners.contains(listener)) return false;

            return sDurationScaleListeners.add(listener);
        }
    }

    public static boolean unregisterDurationScaleChangeListener(DurationScaleChangeListener listener)
    {
        synchronized (sDurationScaleListeners)
        {
            return sDurationScaleListeners.remove(listener);
        }
    }

    public static boolean areAnimatorsEnabled()
    {
        return !(sDurationScale == 0);
    }

    public static long getFrameDelay()
    {
        return sFrameDelay;
    }

    public static void setFrameDelay(long frameDelay)
    {
        sFrameDelay = frameDelay;
    }

    public void setIntValues(int... values)
    {
        if (values == null || values.length == 0) return;

        if (mValues == null || mValues.length == 0) setValues(PropertyValuesHolder.ofInt("", values));
        else mValues[0].setIntValues(values);

        mInitialized = false;
    }

    public void setFloatValues(float... values)
    {
        if (values == null || values.length == 0) return;

        if (mValues == null || mValues.length == 0) setValues(PropertyValuesHolder.ofFloat("", values));
        else mValues[0].setFloatValues(values);

        mInitialized = false;
    }

    public void setObjectValues(Object... values)
    {
        if (values == null || values.length == 0) return;

        if (mValues == null || mValues.length == 0) setValues(PropertyValuesHolder.ofObject("", (TypeEvaluator)null, values));
        else mValues[0].setObjectValues(values);

        mInitialized = false;
    }

    public void setValues(PropertyValuesHolder... values)
    {
        int count = values == null ? 0 : values.length;
        mValues = values;
        mValuesMap = new HashMap<>(count);

        for (int index = 0; index < count; index++)
        {
            PropertyValuesHolder holder = values[index];
            mValuesMap.put(holder.getPropertyName(), holder);
        }

        mInitialized = false;
    }

    public PropertyValuesHolder[] getValues()
    {
        return mValues;
    }

    void initAnimation()
    {
        if (mInitialized) return;

        if (mValues != null)
        {
            for (int index = 0; index < mValues.length; index++) mValues[index].init();
        }

        mInitialized = true;
    }

    @Override
    public ValueAnimator setDuration(long duration)
    {
        if (duration < 0) throw new IllegalArgumentException("Animators cannot have negative duration: " + duration);

        mDuration = duration;
        return this;
    }

    private long getScaledDuration()
    {
        return (long)(mDuration * sDurationScale);
    }

    @Override
    public long getDuration()
    {
        return mDuration;
    }

    @Override
    public long getTotalDuration()
    {
        if (mRepeatCount == INFINITE) return DURATION_INFINITE;

        return mStartDelay + (mDuration * (mRepeatCount + 1));
    }

    public void setCurrentPlayTime(long playTime)
    {
        float fraction = mDuration > 0 ? (float)playTime / mDuration : 1;
        setCurrentFraction(fraction);
    }

    public void setCurrentFraction(float fraction)
    {
        initAnimation();
        fraction = clampFraction(fraction);

        if (mLastFrameTime >= 0)
        {
            long seekTime = (long)(getScaledDuration() * fraction);
            mStartTime = mLastFrameTime - seekTime;
            mSeekFraction = -1;
        }
        else
        {
            mSeekFraction = fraction;
        }

        mOverallFraction = fraction;
        float currentIterationFraction = getCurrentIterationFraction(fraction, mReversing);
        animateValue(currentIterationFraction);
    }

    private int getCurrentIteration(float fraction)
    {
        fraction = clampFraction(fraction);
        double iteration = Math.floor(fraction);
        if (fraction == iteration && fraction > 0) iteration--;

        return (int)iteration;
    }

    private float getCurrentIterationFraction(float fraction, boolean inReverse)
    {
        fraction = clampFraction(fraction);
        int iteration = getCurrentIteration(fraction);
        float currentFraction = fraction - iteration;

        return shouldPlayBackward(iteration, inReverse) ? 1.0f - currentFraction : currentFraction;
    }

    private float clampFraction(float fraction)
    {
        if (fraction < 0) return 0;
        if (mRepeatCount != INFINITE) return Math.min(fraction, mRepeatCount + 1);

        return fraction;
    }

    private boolean shouldPlayBackward(int iteration, boolean inReverse)
    {
        if (iteration > 0 && mRepeatMode == REVERSE && (iteration < (mRepeatCount + 1) || mRepeatCount == INFINITE))
        {
            if (inReverse) return (iteration % 2) == 0;

            return (iteration % 2) != 0;
        }

        return inReverse;
    }

    public long getCurrentPlayTime()
    {
        if (!mInitialized || (!mStarted && mSeekFraction < 0)) return 0;
        if (mSeekFraction >= 0) return (long)(mDuration * mSeekFraction);
        if (mStartTime < 0 || mLastFrameTime < 0) return 0;

        float durationScale = sDurationScale == 0 ? 1 : sDurationScale;
        return (long)((mLastFrameTime - mStartTime) / durationScale);
    }

    @Override
    public long getStartDelay()
    {
        return mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay)
    {
        if (startDelay < 0)
        {
            android.util.Log.w("ValueAnimator", "Start delay should always be non-negative");
            startDelay = 0;
        }

        mStartDelay = startDelay;
    }

    public Object getAnimatedValue()
    {
        if (mValues != null && mValues.length > 0) return mValues[0].getAnimatedValue();

        return null;
    }

    public Object getAnimatedValue(String propertyName)
    {
        PropertyValuesHolder holder = mValuesMap == null ? null : mValuesMap.get(propertyName);

        return holder != null ? holder.getAnimatedValue() : null;
    }

    public void setRepeatCount(int value)
    {
        mRepeatCount = value;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    public void setRepeatMode(int value)
    {
        mRepeatMode = value;
    }

    public int getRepeatMode()
    {
        return mRepeatMode;
    }

    public void addUpdateListener(AnimatorUpdateListener listener)
    {
        if (listener != null) mUpdateListeners.add(listener);
    }

    public void removeAllUpdateListeners()
    {
        mUpdateListeners.clear();
    }

    public void removeUpdateListener(AnimatorUpdateListener listener)
    {
        mUpdateListeners.remove(listener);
    }

    @Override
    public void setInterpolator(TimeInterpolator value)
    {
        mInterpolator = value != null ? value : new LinearInterpolator();
    }

    @Override
    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public void setEvaluator(TypeEvaluator value)
    {
        if (value != null && mValues != null && mValues.length > 0) mValues[0].setEvaluator(value);
    }

    @Override
    public void start()
    {
        start(false);
    }

    private void start(boolean playBackwards)
    {
        mReversing = playBackwards;

        if (playBackwards && mSeekFraction != -1 && mSeekFraction != 0)
        {
            if (mRepeatCount == INFINITE)
            {
                float fraction = (float)(mSeekFraction - Math.floor(mSeekFraction));
                mSeekFraction = 1 - fraction;
            }
            else
            {
                mSeekFraction = 1 + mRepeatCount - mSeekFraction;
            }
        }

        mStarted = true;
        clearPaused();
        mRunning = false;
        mStartTime = -1;
        mLastFrameTime = -1;
        mPauseNanos = -1;

        postFrame();

        if (mStartDelay == 0 || mSeekFraction >= 0 || mReversing)
        {
            startAnimation();

            if (mSeekFraction == -1) setCurrentPlayTime(0);
            else setCurrentFraction(mSeekFraction);
        }
    }

    @Override
    public void cancel()
    {
        if (!mStarted && !mRunning) return;

        if (!mRunning) notifyStart(mReversing);
        notifyCancel();

        endAnimation();
    }

    @Override
    public void end()
    {
        if (!mRunning)
        {
            startAnimation();
            mStarted = true;
        }
        else if (!mInitialized)
        {
            initAnimation();
        }

        animateValue(shouldPlayBackward(mRepeatCount, mReversing) ? 0.0f : 1.0f);
        endAnimation();
    }

    @Override
    public void resume()
    {
        if (isPaused())
        {
            if (mPauseNanos >= 0 && mStartTime >= 0)
            {
                mStartTime += (System.nanoTime() - mPauseNanos) / 1000000L;
            }

            mPauseNanos = -1;
            postFrame();
        }

        super.resume();
    }

    @Override
    public void pause()
    {
        boolean previouslyPaused = isPaused();
        super.pause();

        if (!previouslyPaused && isPaused())
        {
            mPauseNanos = System.nanoTime();
            removeFrame();
        }
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
    public void reverse()
    {
        if (mStarted && mLastFrameTime >= 0)
        {
            long currentTime = mLastFrameTime;
            long currentPlayTime = currentTime - mStartTime;
            long timeLeft = getScaledDuration() - currentPlayTime;
            mStartTime = currentTime - timeLeft;
            mReversing = !mReversing;
        }
        else if (mStarted)
        {
            mReversing = !mReversing;
            end();
        }
        else
        {
            start(true);
        }
    }

    @Override
    public boolean canReverse()
    {
        return true;
    }

    private void endAnimation()
    {
        removeFrame();

        boolean notify = mStarted || mRunning;

        mRunning = false;
        mStarted = false;
        mStartTime = -1;
        mLastFrameTime = -1;
        mPauseNanos = -1;
        mSeekFraction = -1;
        clearPaused();

        if (notify) notifyEnd(mReversing);

        mReversing = false;
    }

    private void startAnimation()
    {
        initAnimation();
        mRunning = true;

        if (mSeekFraction >= 0) mOverallFraction = mSeekFraction;
        else mOverallFraction = 0.0f;

        notifyStart(mReversing);
    }

    private void postFrame()
    {
        if (mCallbackPosted) return;

        if (mChoreographer == null) mChoreographer = Choreographer.getInstance();

        mCallbackPosted = true;
        mChoreographer.postFrameCallback(mFrameCallback);
    }

    private void removeFrame()
    {
        if (!mCallbackPosted) return;

        mCallbackPosted = false;
        if (mChoreographer != null) mChoreographer.removeFrameCallback(mFrameCallback);
    }

    private Choreographer.FrameCallback createFrameCallback()
    {
        return new Choreographer.FrameCallback()
        {
            @Override
            public void doFrame(long frameTimeNanos)
            {
                mCallbackPosted = false;
                doAnimationFrame(frameTimeNanos / 1000000L);
            }
        };
    }

    private void doAnimationFrame(long frameTime)
    {
        if (!mStarted || isPaused()) return;

        if (mStartTime < 0)
        {
            mStartTime = mReversing ? frameTime : frameTime + (long)(mStartDelay * sDurationScale);
        }

        if (!mRunning)
        {
            if (mStartTime > frameTime && mSeekFraction == -1)
            {
                postFrame();
                return;
            }

            startAnimation();
        }

        mLastFrameTime = frameTime;

        if (mSeekFraction >= 0)
        {
            mStartTime = frameTime - (long)(getScaledDuration() * mSeekFraction);
            mSeekFraction = -1;
        }

        long currentTime = Math.max(frameTime, mStartTime);
        boolean finished = animateBasedOnTime(currentTime);

        if (finished)
        {
            endAnimation();
            return;
        }

        if (mStarted) postFrame();
    }

    private boolean animateBasedOnTime(long currentTime)
    {
        boolean done = false;

        if (mRunning)
        {
            long scaledDuration = getScaledDuration();
            float fraction = scaledDuration > 0 ? (float)(currentTime - mStartTime) / scaledDuration : 1.0f;
            float lastFraction = mOverallFraction;
            boolean newIteration = (int)fraction > (int)lastFraction;
            boolean lastIterationFinished = (fraction >= mRepeatCount + 1) && (mRepeatCount != INFINITE);

            if (scaledDuration == 0)
            {
                done = true;
            }
            else if (newIteration && !lastIterationFinished)
            {
                notifyRepeat();
            }
            else if (lastIterationFinished)
            {
                done = true;
            }

            mOverallFraction = clampFraction(fraction);
            float currentIterationFraction = getCurrentIterationFraction(mOverallFraction, mReversing);
            animateValue(currentIterationFraction);
        }

        return done;
    }

    public float getAnimatedFraction()
    {
        return mCurrentFraction;
    }

    @SuppressWarnings("unchecked")
    void animateValue(float fraction)
    {
        fraction = mInterpolator.getInterpolation(fraction);
        mCurrentFraction = fraction;

        if (mValues != null)
        {
            for (int index = 0; index < mValues.length; index++) mValues[index].calculateValue(fraction);
        }

        if (mUpdateListeners.isEmpty()) return;

        ArrayList<AnimatorUpdateListener> listeners = (ArrayList<AnimatorUpdateListener>)mUpdateListeners.clone();
        for (int index = 0; index < listeners.size(); index++) listeners.get(index).onAnimationUpdate(this);
    }

    @Override
    public ValueAnimator clone()
    {
        ValueAnimator copy = (ValueAnimator)super.clone();
        copy.mUpdateListeners = new ArrayList<>(mUpdateListeners);
        copy.mFrameCallback = copy.createFrameCallback();
        copy.mChoreographer = null;
        copy.mCallbackPosted = false;
        copy.mSeekFraction = -1;
        copy.mReversing = false;
        copy.mInitialized = false;
        copy.mStarted = false;
        copy.mRunning = false;
        copy.mStartTime = -1;
        copy.mLastFrameTime = -1;
        copy.mPauseNanos = -1;
        copy.mOverallFraction = 0;
        copy.mCurrentFraction = 0;

        PropertyValuesHolder[] values = mValues;

        if (values != null)
        {
            int count = values.length;
            copy.mValues = new PropertyValuesHolder[count];
            copy.mValuesMap = new HashMap<>(count);

            for (int index = 0; index < count; index++)
            {
                PropertyValuesHolder holder = values[index].clone();
                copy.mValues[index] = holder;
                copy.mValuesMap.put(holder.getPropertyName(), holder);
            }
        }

        return copy;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("ValueAnimator@" + Integer.toHexString(hashCode()));

        if (mValues != null)
        {
            for (int index = 0; index < mValues.length; index++) builder.append("\n    ").append(mValues[index].toString());
        }

        return builder.toString();
    }

    public interface AnimatorUpdateListener
    {
        void onAnimationUpdate(ValueAnimator animation);
    }

    public interface DurationScaleChangeListener
    {
        void onChanged(float scale);
    }
}

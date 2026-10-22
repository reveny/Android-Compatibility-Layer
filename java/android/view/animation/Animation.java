package android.view.animation;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;

public abstract class Animation implements Cloneable
{
    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int START_ON_FIRST_FRAME = -1;
    public static final int ABSOLUTE = 0;
    public static final int RELATIVE_TO_SELF = 1;
    public static final int RELATIVE_TO_PARENT = 2;
    public static final int ZORDER_NORMAL = 0;
    public static final int ZORDER_TOP = 1;
    public static final int ZORDER_BOTTOM = -1;

    private static final String[] REPEAT_MODE_NAMES = {"restart", "reverse"};
    private static final int[] REPEAT_MODE_VALUES = {RESTART, REVERSE};
    private static final String[] Z_ADJUSTMENT_NAMES = {"normal", "top", "bottom"};
    private static final int[] Z_ADJUSTMENT_VALUES = {ZORDER_NORMAL, ZORDER_TOP, ZORDER_BOTTOM};

    boolean mEnded;
    boolean mStarted;
    boolean mCycleFlip;
    boolean mInitialized;
    boolean mFillBefore = true;
    boolean mFillAfter;
    boolean mFillEnabled;
    long mStartTime = -1;
    long mStartOffset;
    long mDuration;
    int mRepeatCount;
    int mRepeated;
    int mRepeatMode = RESTART;
    Interpolator mInterpolator;
    AnimationListener mListener;

    private int mZAdjustment;
    private int mBackgroundColor;
    private float mScaleFactor = 1.0f;
    private boolean mDetachWallpaper;
    private boolean mShowBackdrop;
    private int mBackdropColor;
    private boolean mMore = true;
    private boolean mOneMoreTime = true;

    RectF mPreviousRegion = new RectF();
    RectF mRegion = new RectF();
    Transformation mTransformation = new Transformation();
    Transformation mPreviousTransformation = new Transformation();

    public Animation()
    {
        ensureInterpolator();
    }

    public Animation(Context context, AttributeSet attributes)
    {
        AnimationAttributes values = new AnimationAttributes(context, attributes);

        setDuration(values.getInt("duration", 0));
        setStartOffset(values.getInt("startOffset", 0));
        setFillEnabled(values.getBoolean("fillEnabled", mFillEnabled));
        setFillBefore(values.getBoolean("fillBefore", mFillBefore));
        setFillAfter(values.getBoolean("fillAfter", mFillAfter));
        setRepeatCount(values.getInt("repeatCount", mRepeatCount));
        setRepeatMode(values.getEnum("repeatMode", REPEAT_MODE_NAMES, REPEAT_MODE_VALUES, RESTART));
        setZAdjustment(values.getEnum("zAdjustment", Z_ADJUSTMENT_NAMES, Z_ADJUSTMENT_VALUES, ZORDER_NORMAL));
        setBackgroundColor(values.getColor("background", 0));
        setDetachWallpaper(values.getBoolean("detachWallpaper", false));
        setShowBackdrop(values.getBoolean("showBackdrop", false));
        setBackdropColor(values.getColor("backdropColor", 0));

        int interpolator = values.getResourceId("interpolator", 0);
        if (interpolator > 0) setInterpolator(context, interpolator);

        ensureInterpolator();
    }

    @Override
    protected Animation clone() throws CloneNotSupportedException
    {
        Animation animation = (Animation)super.clone();
        animation.mPreviousRegion = new RectF();
        animation.mRegion = new RectF();
        animation.mTransformation = new Transformation();
        animation.mPreviousTransformation = new Transformation();
        return animation;
    }

    public void reset()
    {
        mPreviousRegion.setEmpty();
        mPreviousTransformation.clear();
        mInitialized = false;
        mCycleFlip = false;
        mRepeated = 0;
        mMore = true;
        mOneMoreTime = true;
    }

    public void cancel()
    {
        if (mStarted && !mEnded)
        {
            fireAnimationEnd();
            mEnded = true;
        }

        mStartTime = Long.MIN_VALUE;
        mMore = false;
        mOneMoreTime = false;
    }

    public void detach()
    {
        if (mStarted && !mEnded)
        {
            mEnded = true;
            fireAnimationEnd();
        }
    }

    public boolean isInitialized()
    {
        return mInitialized;
    }

    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        reset();
        mInitialized = true;
    }

    public void setInterpolator(Context context, int resourceId)
    {
        setInterpolator(AnimationUtils.loadInterpolator(context, resourceId));
    }

    public void setInterpolator(Interpolator interpolator)
    {
        mInterpolator = interpolator;
    }

    public void setStartOffset(long startOffset)
    {
        mStartOffset = startOffset;
    }

    public void setDuration(long durationMillis)
    {
        if (durationMillis < 0) throw new IllegalArgumentException("Animation duration cannot be negative");

        mDuration = durationMillis;
    }

    public void restrictDuration(long durationMillis)
    {
        if (mStartOffset > durationMillis)
        {
            mStartOffset = durationMillis;
            mDuration = 0;
            mRepeatCount = 0;
            return;
        }

        long total = mDuration + mStartOffset;
        if (total > durationMillis)
        {
            mDuration = durationMillis - mStartOffset;
            total = durationMillis;
        }

        if (mDuration <= 0)
        {
            mDuration = 0;
            mRepeatCount = 0;
            return;
        }

        if (mRepeatCount < 0 || mRepeatCount > durationMillis || (total * mRepeatCount) > durationMillis)
        {
            mRepeatCount = (int)(durationMillis / total) - 1;
            if (mRepeatCount < 0) mRepeatCount = 0;
        }
    }

    public void scaleCurrentDuration(float scale)
    {
        mDuration = (long)(mDuration * scale);
        mStartOffset = (long)(mStartOffset * scale);
    }

    public void setStartTime(long startTimeMillis)
    {
        mStartTime = startTimeMillis;
        mStarted = false;
        mEnded = false;
        mCycleFlip = false;
        mRepeated = 0;
        mMore = true;
    }

    public void start()
    {
        setStartTime(-1);
    }

    public void startNow()
    {
        setStartTime(AnimationUtils.currentAnimationTimeMillis());
    }

    public void setRepeatMode(int repeatMode)
    {
        mRepeatMode = repeatMode;
    }

    public void setRepeatCount(int repeatCount)
    {
        if (repeatCount < 0) repeatCount = INFINITE;

        mRepeatCount = repeatCount;
    }

    public boolean isFillEnabled()
    {
        return mFillEnabled;
    }

    public void setFillEnabled(boolean fillEnabled)
    {
        mFillEnabled = fillEnabled;
    }

    public void setFillBefore(boolean fillBefore)
    {
        mFillBefore = fillBefore;
    }

    public void setFillAfter(boolean fillAfter)
    {
        mFillAfter = fillAfter;
    }

    public void setZAdjustment(int zAdjustment)
    {
        mZAdjustment = zAdjustment;
    }

    public void setBackgroundColor(int backgroundColor)
    {
        mBackgroundColor = backgroundColor;
    }

    protected float getScaleFactor()
    {
        return mScaleFactor;
    }

    public void setDetachWallpaper(boolean detachWallpaper)
    {
        mDetachWallpaper = detachWallpaper;
    }

    public void setShowBackdrop(boolean showBackdrop)
    {
        mShowBackdrop = showBackdrop;
    }

    public void setBackdropColor(int backdropColor)
    {
        mBackdropColor = backdropColor;
    }

    public Interpolator getInterpolator()
    {
        return mInterpolator;
    }

    public long getStartTime()
    {
        return mStartTime;
    }

    public long getDuration()
    {
        return mDuration;
    }

    public long getStartOffset()
    {
        return mStartOffset;
    }

    public int getRepeatMode()
    {
        return mRepeatMode;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    public boolean getFillBefore()
    {
        return mFillBefore;
    }

    public boolean getFillAfter()
    {
        return mFillAfter;
    }

    public int getZAdjustment()
    {
        return mZAdjustment;
    }

    public int getBackgroundColor()
    {
        return mBackgroundColor;
    }

    public boolean getDetachWallpaper()
    {
        return mDetachWallpaper;
    }

    public boolean getShowBackdrop()
    {
        return mShowBackdrop;
    }

    public int getBackdropColor()
    {
        return mBackdropColor;
    }

    public boolean willChangeTransformationMatrix()
    {
        return true;
    }

    public boolean willChangeBounds()
    {
        return true;
    }

    public boolean hasAlpha()
    {
        return false;
    }

    public void setAnimationListener(AnimationListener listener)
    {
        mListener = listener;
    }

    public AnimationListener getAnimationListener()
    {
        return mListener;
    }

    protected void ensureInterpolator()
    {
        if (mInterpolator == null) mInterpolator = new AccelerateDecelerateInterpolator();
    }

    public long computeDurationHint()
    {
        return (getStartOffset() + getDuration()) * (getRepeatCount() + 1);
    }

    private boolean isCanceled()
    {
        return mStartTime == Long.MIN_VALUE;
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation)
    {
        if (mStartTime == -1) mStartTime = currentTime;

        long startOffset = getStartOffset();
        long duration = mDuration;
        float normalizedTime;

        if (duration != 0) normalizedTime = ((float)(currentTime - (mStartTime + startOffset))) / (float)duration;
        else normalizedTime = currentTime < mStartTime ? 0.0f : 1.0f;

        boolean expired = normalizedTime >= 1.0f || isCanceled();
        mMore = !expired;

        if (!mFillEnabled) normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), 0.0f);

        if ((normalizedTime >= 0.0f || mFillBefore) && (normalizedTime <= 1.0f || mFillAfter))
        {
            if (!mStarted)
            {
                fireAnimationStart();
                mStarted = true;
            }

            if (mFillEnabled) normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), 0.0f);
            if (mCycleFlip) normalizedTime = 1.0f - normalizedTime;

            getTransformationAt(normalizedTime, outTransformation);
        }

        if (expired)
        {
            if (mRepeatCount == mRepeated || isCanceled())
            {
                if (!mEnded)
                {
                    mEnded = true;
                    fireAnimationEnd();
                }
            }
            else
            {
                if (mRepeatCount > 0) mRepeated++;
                if (mRepeatMode == REVERSE) mCycleFlip = !mCycleFlip;

                mStartTime = -1;
                mMore = true;

                fireAnimationRepeat();
            }
        }

        if (!mMore && mOneMoreTime)
        {
            mOneMoreTime = false;
            return true;
        }

        return mMore;
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation, float scale)
    {
        mScaleFactor = scale;
        return getTransformation(currentTime, outTransformation);
    }

    public void getTransformationAt(float normalizedTime, Transformation outTransformation)
    {
        ensureInterpolator();

        float interpolatedTime = mInterpolator.getInterpolation(normalizedTime);
        applyTransformation(interpolatedTime, outTransformation);
    }

    void fireAnimationStart()
    {
        if (mListener != null) mListener.onAnimationStart(this);
    }

    void fireAnimationRepeat()
    {
        if (mListener != null) mListener.onAnimationRepeat(this);
    }

    void fireAnimationEnd()
    {
        if (mListener != null) mListener.onAnimationEnd(this);
    }

    public boolean hasStarted()
    {
        return mStarted;
    }

    public boolean hasEnded()
    {
        return mEnded;
    }

    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
    }

    protected float resolveSize(int type, float value, int size, int parentSize)
    {
        switch (type)
        {
            case ABSOLUTE:
                return value;
            case RELATIVE_TO_SELF:
                return size * value;
            case RELATIVE_TO_PARENT:
                return parentSize * value;
            default:
                return value;
        }
    }

    public void getInvalidateRegion(int left, int top, int right, int bottom, RectF invalidate, Transformation transformation)
    {
        RectF tempRegion = mRegion;
        RectF previousRegion = mPreviousRegion;

        invalidate.set(left, top, right, bottom);
        transformation.getMatrix().mapRect(invalidate);
        invalidate.inset(-1.0f, -1.0f);
        tempRegion.set(invalidate);
        invalidate.union(previousRegion);

        previousRegion.set(tempRegion);

        Transformation tempTransformation = mTransformation;
        Transformation previousTransformation = mPreviousTransformation;

        tempTransformation.set(transformation);
        transformation.set(previousTransformation);
        previousTransformation.set(tempTransformation);
    }

    public void initializeInvalidateRegion(int left, int top, int right, int bottom)
    {
        RectF region = mPreviousRegion;
        region.set(left, top, right, bottom);
        region.inset(-1.0f, -1.0f);

        if (mFillBefore)
        {
            Transformation previousTransformation = mPreviousTransformation;
            applyTransformation(mInterpolator.getInterpolation(0.0f), previousTransformation);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public static class Description
    {
        public int type;
        public float value;

        protected Description()
        {
        }
    }

    public interface AnimationListener
    {
        void onAnimationStart(Animation animation);

        void onAnimationEnd(Animation animation);

        void onAnimationRepeat(Animation animation);
    }
}

package android.widget;

import android.content.Context;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

public class Scroller
{
    private static final int DEFAULT_DURATION = 250;
    private static final float INFLEXION = 0.35f;

    private final Interpolator mInterpolator;
    private final float mDeceleration;
    private final float mPhysicalCoefficient;

    private int mMode;
    private static final int MODE_SCROLL = 0;
    private static final int MODE_FLING = 1;

    private int mStartX;
    private int mStartY;
    private int mFinalX;
    private int mFinalY;
    private int mCurrentX;
    private int mCurrentY;
    private int mDeltaX;
    private int mDeltaY;

    private long mStartTime;
    private int mDuration;
    private boolean mFinished = true;

    private float mVelocity;
    private float mDirectionX;
    private float mDirectionY;

    public Scroller(Context context)
    {
        this(context, null);
    }

    public Scroller(Context context, Interpolator interpolator)
    {
        this(context, interpolator, true);
    }

    public Scroller(Context context, Interpolator interpolator, boolean flywheel)
    {
        mInterpolator = interpolator;

        float density = context == null || context.getResources() == null
                            ? 1.0f
                            : context.getResources().getDisplayMetrics().density;

        mDeceleration = 386.0878f * 160.0f * density * 0.84f;
        mPhysicalCoefficient = 9.80665f * 39.37f * 160.0f * density * 0.84f;
    }

    public final boolean isFinished()
    {
        return mFinished;
    }

    public final void forceFinished(boolean finished)
    {
        mFinished = finished;
    }

    public final int getDuration()
    {
        return mDuration;
    }

    public final int getCurrX()
    {
        return mCurrentX;
    }

    public final int getCurrY()
    {
        return mCurrentY;
    }

    public final int getStartX()
    {
        return mStartX;
    }

    public final int getStartY()
    {
        return mStartY;
    }

    public final int getFinalX()
    {
        return mFinalX;
    }

    public final int getFinalY()
    {
        return mFinalY;
    }

    public void setFinalX(int x)
    {
        mFinalX = x;
        mDeltaX = mFinalX - mStartX;
        mFinished = false;
    }

    public void setFinalY(int y)
    {
        mFinalY = y;
        mDeltaY = mFinalY - mStartY;
        mFinished = false;
    }

    public float getCurrVelocity()
    {
        if (mMode != MODE_FLING) return 0;

        long elapsed = android.os.SystemClock.uptimeMillis() - mStartTime;
        float remaining = mVelocity - mDeceleration * (elapsed / 1000.0f);

        return remaining < 0 ? 0 : remaining;
    }

    public boolean computeScrollOffset()
    {
        if (mFinished) return false;

        long elapsed = android.os.SystemClock.uptimeMillis() - mStartTime;

        if (elapsed >= mDuration)
        {
            mCurrentX = mFinalX;
            mCurrentY = mFinalY;
            mFinished = true;

            return true;
        }

        float fraction = (float)elapsed / (float)mDuration;
        float eased = mInterpolator == null ? viscous(fraction) : mInterpolator.getInterpolation(fraction);

        mCurrentX = mStartX + Math.round(eased * mDeltaX);
        mCurrentY = mStartY + Math.round(eased * mDeltaY);

        return true;
    }

    private static float viscous(float fraction)
    {
        float inverse = 1.0f - fraction;

        return 1.0f - inverse * inverse * inverse;
    }

    public void startScroll(int startX, int startY, int deltaX, int deltaY)
    {
        startScroll(startX, startY, deltaX, deltaY, DEFAULT_DURATION);
    }

    public void startScroll(int startX, int startY, int deltaX, int deltaY, int duration)
    {
        mMode = MODE_SCROLL;
        mFinished = false;
        mDuration = duration <= 0 ? 1 : duration;
        mStartTime = android.os.SystemClock.uptimeMillis();

        mStartX = startX;
        mStartY = startY;
        mCurrentX = startX;
        mCurrentY = startY;
        mFinalX = startX + deltaX;
        mFinalY = startY + deltaY;
        mDeltaX = deltaX;
        mDeltaY = deltaY;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY)
    {
        mMode = MODE_FLING;
        mFinished = false;

        float velocity = (float)Math.hypot(velocityX, velocityY);
        mVelocity = velocity;

        if (velocity < 1)
        {
            mDuration = 1;
            mStartX = mCurrentX = mFinalX = startX;
            mStartY = mCurrentY = mFinalY = startY;
            mDeltaX = 0;
            mDeltaY = 0;
            mStartTime = android.os.SystemClock.uptimeMillis();

            return;
        }

        mDuration = Math.max(1, (int)(1000.0f * velocity / mDeceleration));
        mStartTime = android.os.SystemClock.uptimeMillis();

        mDirectionX = velocityX / velocity;
        mDirectionY = velocityY / velocity;

        double distance = velocity * velocity / (2.0 * mDeceleration);

        mStartX = startX;
        mStartY = startY;
        mCurrentX = startX;
        mCurrentY = startY;

        mFinalX = clamp(startX + (int)Math.round(distance * mDirectionX), minX, maxX);
        mFinalY = clamp(startY + (int)Math.round(distance * mDirectionY), minY, maxY);

        mDeltaX = mFinalX - mStartX;
        mDeltaY = mFinalY - mStartY;
    }

    static int clamp(int value, int low, int high)
    {
        if (value < low) return low;
        if (value > high) return high;

        return value;
    }

    public void abortAnimation()
    {
        mCurrentX = mFinalX;
        mCurrentY = mFinalY;
        mFinished = true;
    }

    public void extendDuration(int extra)
    {
        long elapsed = android.os.SystemClock.uptimeMillis() - mStartTime;

        mDuration = (int)elapsed + extra;
        mFinished = false;
    }

    public int timePassed()
    {
        return (int)(android.os.SystemClock.uptimeMillis() - mStartTime);
    }
}

package android.graphics.drawable;

import android.graphics.Canvas;
import android.os.SystemClock;

public class TransitionDrawable extends LayerDrawable implements Drawable.Callback
{
    private static final int TRANSITION_STARTING = 0;
    private static final int TRANSITION_RUNNING = 1;
    private static final int TRANSITION_NONE = 2;

    private int mTransitionState = TRANSITION_NONE;
    private boolean mReverse;
    private long mStartTimeMillis;
    private int mFrom;
    private int mTo;
    private int mDuration;
    private int mOriginalDuration;
    private int mAlpha = 0;
    private boolean mCrossFade;

    TransitionDrawable()
    {
    }

    public TransitionDrawable(Drawable[] layers)
    {
        super(layers);
    }

    @Override
    LayerDrawable createCopy()
    {
        return new TransitionDrawable();
    }

    @Override
    void copyConfigurationTo(LayerDrawable copy)
    {
        super.copyConfigurationTo(copy);
        if (copy instanceof TransitionDrawable) ((TransitionDrawable) copy).mCrossFade = mCrossFade;
    }

    public void startTransition(int durationMillis)
    {
        mFrom = 0;
        mTo = 255;
        mAlpha = 0;
        mDuration = mOriginalDuration = durationMillis;
        mReverse = false;
        mTransitionState = TRANSITION_STARTING;
        invalidateSelf();
    }

    public void resetTransition()
    {
        mAlpha = 0;
        mTransitionState = TRANSITION_NONE;
        invalidateSelf();
    }

    public void reverseTransition(int duration)
    {
        long time = SystemClock.uptimeMillis();

        if (time - mStartTimeMillis > mDuration)
        {
            if (mTo == 0)
            {
                mFrom = 0;
                mTo = 255;
                mAlpha = 0;
                mReverse = false;
            }
            else
            {
                mFrom = 255;
                mTo = 0;
                mAlpha = 255;
                mReverse = true;
            }

            mDuration = mOriginalDuration = duration;
            mTransitionState = TRANSITION_STARTING;
            invalidateSelf();
            return;
        }

        mReverse = !mReverse;
        mFrom = mAlpha;
        mTo = mReverse ? 0 : 255;
        mDuration = (int)(mReverse ? time - mStartTimeMillis : mOriginalDuration - (time - mStartTimeMillis));
        mTransitionState = TRANSITION_STARTING;
    }

    public void setCrossFadeEnabled(boolean enabled)
    {
        mCrossFade = enabled;
    }

    public boolean isCrossFadeEnabled()
    {
        return mCrossFade;
    }

    @Override
    public void draw(Canvas canvas)
    {
        boolean done = true;

        switch (mTransitionState)
        {
            case TRANSITION_STARTING:
                mStartTimeMillis = SystemClock.uptimeMillis();
                done = false;
                mTransitionState = TRANSITION_RUNNING;
                break;

            case TRANSITION_RUNNING:
                if (mStartTimeMillis >= 0)
                {
                    float normalized = mDuration <= 0 ? 1.0f : (float)(SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                    done = normalized >= 1.0f;
                    normalized = Math.min(normalized, 1.0f);
                    mAlpha = (int)(mFrom + (mTo - mFrom) * normalized);
                }
                break;

            default:
                break;
        }

        layoutLayers();

        Drawable first = getDrawable(0);
        Drawable second = getDrawable(1);
        int alpha = mAlpha;

        if (done)
        {
            if (first != null && (!mCrossFade || alpha == 0)) first.draw(canvas);
            if (second != null && alpha == 0xFF) second.draw(canvas);
            return;
        }

        if (first != null)
        {
            if (mCrossFade) first.setAlpha(255 - alpha);
            first.draw(canvas);
            if (mCrossFade) first.setAlpha(0xFF);
        }

        if (second != null && alpha > 0)
        {
            second.setAlpha(alpha);
            second.draw(canvas);
            second.setAlpha(0xFF);
        }

        invalidateSelf();
    }
}

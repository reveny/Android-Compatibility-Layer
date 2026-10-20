package android.widget;

import android.content.Context;
import android.view.animation.Interpolator;

public class OverScroller
{
    private final Scroller mScroller;

    private int mOverX;
    private int mOverY;

    public OverScroller(Context context)
    {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator)
    {
        mScroller = new Scroller(context, interpolator);
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceX, float bounceY)
    {
        this(context, interpolator);
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceX, float bounceY, boolean flywheel)
    {
        this(context, interpolator);
    }

    public final boolean isFinished()
    {
        return mScroller.isFinished();
    }

    public final void forceFinished(boolean finished)
    {
        mScroller.forceFinished(finished);
    }

    public final int getCurrX()
    {
        return mScroller.getCurrX();
    }

    public final int getCurrY()
    {
        return mScroller.getCurrY();
    }

    public final int getStartX()
    {
        return mScroller.getStartX();
    }

    public final int getStartY()
    {
        return mScroller.getStartY();
    }

    public final int getFinalX()
    {
        return mScroller.getFinalX();
    }

    public final int getFinalY()
    {
        return mScroller.getFinalY();
    }

    public final int getDuration()
    {
        return mScroller.getDuration();
    }

    public float getCurrVelocity()
    {
        return mScroller.getCurrVelocity();
    }

    public boolean computeScrollOffset()
    {
        return mScroller.computeScrollOffset();
    }

    public void startScroll(int startX, int startY, int deltaX, int deltaY)
    {
        mScroller.startScroll(startX, startY, deltaX, deltaY);
    }

    public void startScroll(int startX, int startY, int deltaX, int deltaY, int duration)
    {
        mScroller.startScroll(startX, startY, deltaX, deltaY, duration);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY)
    {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY,
                      int overX, int overY)
    {
        mOverX = overX;
        mOverY = overY;

        mScroller.fling(startX, startY, velocityX, velocityY, minX - overX, maxX + overX, minY - overY, maxY + overY);
    }

    public void springBack(int startX, int startY, int minX, int maxX, int minY, int maxY)
    {
        int targetX = Scroller.clamp(startX, minX, maxX);
        int targetY = Scroller.clamp(startY, minY, maxY);

        if (targetX == startX && targetY == startY)
        {
            mScroller.forceFinished(true);
            return;
        }

        mScroller.startScroll(startX, startY, targetX - startX, targetY - startY);
    }

    public void notifyHorizontalEdgeReached(int start, int end, int overX)
    {
        mOverX = overX;
        springBack(start, getCurrY(), end, end, getCurrY(), getCurrY());
    }

    public void notifyVerticalEdgeReached(int start, int end, int overY)
    {
        mOverY = overY;
        springBack(getCurrX(), start, getCurrX(), getCurrX(), end, end);
    }

    public boolean isOverScrolled()
    {
        return false;
    }

    public void abortAnimation()
    {
        mScroller.abortAnimation();
    }

    public void extendDuration(int extra)
    {
        mScroller.extendDuration(extra);
    }

    public int timePassed()
    {
        return mScroller.timePassed();
    }
}

package android.view;

import android.graphics.Point;
import android.graphics.Rect;

public final class ScrollCaptureTarget
{
    private final View mContainingView;
    private final ScrollCaptureCallback mCallback;
    private final Rect mLocalVisibleRect;
    private final Point mPositionInWindow;
    private final int mHint;
    private Rect mScrollBounds;
    private final int[] mTmpIntArray = new int[2];

    public ScrollCaptureTarget(View scrollTarget, Rect localVisibleRect, Point positionInWindow, ScrollCaptureCallback callback)
    {
        if (scrollTarget == null) throw new NullPointerException("scrollTarget must not be null");
        if (localVisibleRect == null) throw new NullPointerException("localVisibleRect must not be null");
        if (positionInWindow == null) throw new NullPointerException("positionInWindow must not be null");
        if (callback == null) throw new NullPointerException("callback must not be null");

        mContainingView = scrollTarget;
        mHint = scrollTarget.getScrollCaptureHint();
        mCallback = callback;
        mLocalVisibleRect = localVisibleRect;
        mPositionInWindow = positionInWindow;
    }

    public int getHint()
    {
        return mHint;
    }

    public ScrollCaptureCallback getCallback()
    {
        return mCallback;
    }

    public View getContainingView()
    {
        return mContainingView;
    }

    public Rect getLocalVisibleRect()
    {
        return mLocalVisibleRect;
    }

    public Point getPositionInWindow()
    {
        return mPositionInWindow;
    }

    public Rect getScrollBounds()
    {
        return mScrollBounds;
    }

    public void setScrollBounds(Rect scrollBounds)
    {
        mScrollBounds = scrollBounds == null ? null : new Rect(scrollBounds);
        if (mScrollBounds != null && !mScrollBounds.intersect(0, 0, mContainingView.getWidth(), mContainingView.getHeight()))
        {
            mScrollBounds.setEmpty();
        }
    }

    public void updatePositionInWindow()
    {
        mContainingView.getLocationInWindow(mTmpIntArray);
        mPositionInWindow.x = mTmpIntArray[0];
        mPositionInWindow.y = mTmpIntArray[1];
    }

    @Override
    public String toString()
    {
        return "ScrollCaptureTarget{view=" + mContainingView + ", rect=" + mLocalVisibleRect + ", window=" + mPositionInWindow + ", hint="
               + mHint + "}";
    }
}

package android.view;

import android.graphics.Point;
import android.graphics.Rect;

public class ScrollCaptureSession
{
    private final Surface mSurface;
    private final Rect mScrollBounds;
    private final Point mPositionInWindow;

    public ScrollCaptureSession(Surface surface, Rect scrollBounds, Point positionInWindow)
    {
        if (surface == null) throw new NullPointerException("surface must not be null");
        if (scrollBounds == null) throw new NullPointerException("scrollBounds must not be null");
        if (positionInWindow == null) throw new NullPointerException("positionInWindow must not be null");

        mSurface = surface;
        mScrollBounds = scrollBounds;
        mPositionInWindow = positionInWindow;
    }

    public Surface getSurface()
    {
        return mSurface;
    }

    public Rect getScrollBounds()
    {
        return mScrollBounds;
    }

    public Point getPositionInWindow()
    {
        return mPositionInWindow;
    }
}

package android.view;

import android.graphics.Rect;

public final class WindowMetrics
{
    private final Rect mBounds;
    private final WindowInsets mWindowInsets;
    private final float mDensity;

    public WindowMetrics(Rect bounds, WindowInsets windowInsets)
    {
        this(bounds, windowInsets, 1.0f);
    }

    public WindowMetrics(Rect bounds, WindowInsets windowInsets, float density)
    {
        mBounds = bounds;
        mWindowInsets = windowInsets;
        mDensity = density;
    }

    public Rect getBounds()
    {
        return mBounds;
    }

    public WindowInsets getWindowInsets()
    {
        return mWindowInsets;
    }

    public float getDensity()
    {
        return mDensity;
    }
}

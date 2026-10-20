package android.view;

import android.graphics.Insets;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public final class DisplayCutout
{
    private final Insets mSafeInsets;
    private final List<Rect> mBoundingRects;

    public DisplayCutout(Insets safeInsets, List<Rect> boundingRects)
    {
        mSafeInsets = safeInsets == null ? Insets.NONE : safeInsets;
        mBoundingRects = boundingRects == null ? new ArrayList<Rect>() : boundingRects;
    }

    public DisplayCutout(Rect safeInsets, List<Rect> boundingRects)
    {
        this(safeInsets == null ? Insets.NONE : Insets.of(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom),
             boundingRects);
    }

    public int getSafeInsetLeft()
    {
        return mSafeInsets.left;
    }

    public int getSafeInsetTop()
    {
        return mSafeInsets.top;
    }

    public int getSafeInsetRight()
    {
        return mSafeInsets.right;
    }

    public int getSafeInsetBottom()
    {
        return mSafeInsets.bottom;
    }

    public Insets getWaterfallInsets()
    {
        return Insets.NONE;
    }

    public List<Rect> getBoundingRects()
    {
        return new ArrayList<Rect>(mBoundingRects);
    }

    public Rect getBoundingRectLeft()
    {
        return new Rect();
    }

    public Rect getBoundingRectTop()
    {
        return new Rect();
    }

    public Rect getBoundingRectRight()
    {
        return new Rect();
    }

    public Rect getBoundingRectBottom()
    {
        return new Rect();
    }
}

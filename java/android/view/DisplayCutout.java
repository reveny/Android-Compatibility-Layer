package android.view;

import android.graphics.Insets;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public final class DisplayCutout
{
    private static final int BOUNDS_POSITION_LEFT = 0;
    private static final int BOUNDS_POSITION_TOP = 1;
    private static final int BOUNDS_POSITION_RIGHT = 2;
    private static final int BOUNDS_POSITION_BOTTOM = 3;

    private final Insets mSafeInsets;
    private final Insets mWaterfallInsets;
    private final Rect[] mBounds = {new Rect(), new Rect(), new Rect(), new Rect()};
    private final List<Rect> mExtraRects = new ArrayList<>();

    public DisplayCutout(Insets safeInsets, Rect boundLeft, Rect boundTop, Rect boundRight, Rect boundBottom)
    {
        this(safeInsets, boundLeft, boundTop, boundRight, boundBottom, null);
    }

    public DisplayCutout(Insets safeInsets, Rect boundLeft, Rect boundTop, Rect boundRight, Rect boundBottom, Insets waterfallInsets)
    {
        mSafeInsets = safeInsets == null ? Insets.NONE : safeInsets;
        mWaterfallInsets = waterfallInsets == null ? Insets.NONE : waterfallInsets;

        assign(BOUNDS_POSITION_LEFT, boundLeft);
        assign(BOUNDS_POSITION_TOP, boundTop);
        assign(BOUNDS_POSITION_RIGHT, boundRight);
        assign(BOUNDS_POSITION_BOTTOM, boundBottom);
    }

    public DisplayCutout(Insets safeInsets, List<Rect> boundingRects)
    {
        mSafeInsets = safeInsets == null ? Insets.NONE : safeInsets;
        mWaterfallInsets = Insets.NONE;

        if (boundingRects == null) return;

        for (Rect rect : boundingRects)
        {
            if (rect == null || rect.isEmpty()) continue;

            int position = sideOf(rect);
            if (position >= 0 && mBounds[position].isEmpty()) mBounds[position].set(rect);
            else mExtraRects.add(new Rect(rect));
        }
    }

    @Deprecated
    public DisplayCutout(Rect safeInsets, List<Rect> boundingRects)
    {
        this(safeInsets == null ? Insets.NONE : Insets.of(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom),
             boundingRects);
    }

    private void assign(int position, Rect rect)
    {
        if (rect != null) mBounds[position].set(rect);
    }

    private int sideOf(Rect rect)
    {
        if (mSafeInsets.top > 0 && rect.top <= 0) return BOUNDS_POSITION_TOP;
        if (mSafeInsets.left > 0 && rect.left <= 0) return BOUNDS_POSITION_LEFT;
        if (mSafeInsets.bottom > 0) return BOUNDS_POSITION_BOTTOM;
        if (mSafeInsets.right > 0) return BOUNDS_POSITION_RIGHT;
        return -1;
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
        return mWaterfallInsets;
    }

    public List<Rect> getBoundingRects()
    {
        List<Rect> rects = new ArrayList<>();

        for (Rect rect : mBounds)
        {
            if (!rect.isEmpty()) rects.add(new Rect(rect));
        }

        for (Rect rect : mExtraRects)
        {
            rects.add(new Rect(rect));
        }

        return rects;
    }

    public Rect getBoundingRectLeft()
    {
        return new Rect(mBounds[BOUNDS_POSITION_LEFT]);
    }

    public Rect getBoundingRectTop()
    {
        return new Rect(mBounds[BOUNDS_POSITION_TOP]);
    }

    public Rect getBoundingRectRight()
    {
        return new Rect(mBounds[BOUNDS_POSITION_RIGHT]);
    }

    public Rect getBoundingRectBottom()
    {
        return new Rect(mBounds[BOUNDS_POSITION_BOTTOM]);
    }

    public Path getCutoutPath()
    {
        List<Rect> rects = getBoundingRects();
        if (rects.isEmpty()) return null;

        Path path = new Path();
        for (Rect rect : rects)
        {
            path.addRect(rect.left, rect.top, rect.right, rect.bottom, Path.Direction.CW);
        }

        return path;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof DisplayCutout)) return false;

        DisplayCutout cutout = (DisplayCutout)other;
        return mSafeInsets.equals(cutout.mSafeInsets) && mWaterfallInsets.equals(cutout.mWaterfallInsets)
               && getBoundingRects().equals(cutout.getBoundingRects());
    }

    @Override
    public int hashCode()
    {
        int result = mSafeInsets.hashCode();
        result = result * 48271 + mWaterfallInsets.hashCode();
        result = result * 48271 + getBoundingRects().hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "DisplayCutout{insets=" + mSafeInsets + " waterfall=" + mWaterfallInsets + " boundingRect=" + getBoundingRects() + "}";
    }
}

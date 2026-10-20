package android.view;

import android.graphics.Rect;

public interface ViewParent
{
    void requestLayout();

    void invalidateChild(View child, Rect dirty);

    default void onDescendantInvalidated(View child, View target)
    {
        invalidateChild(child, null);
    }

    default void requestDisallowInterceptTouchEvent(boolean disallow)
    {
    }

    ViewParent getParent();
}

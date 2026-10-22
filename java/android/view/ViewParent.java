package android.view;

import android.graphics.Point;
import android.graphics.Rect;

public interface ViewParent
{
    void requestLayout();

    default boolean isLayoutRequested()
    {
        return false;
    }

    default void requestTransparentRegion(View child)
    {
    }

    void invalidateChild(View child, Rect dirty);

    default void onDescendantInvalidated(View child, View target)
    {
        invalidateChild(child, null);
    }

    default ViewParent invalidateChildInParent(int[] location, Rect dirty)
    {
        return null;
    }

    default void requestDisallowInterceptTouchEvent(boolean disallow)
    {
    }

    ViewParent getParent();

    default void requestChildFocus(View child, View focused)
    {
    }

    default void recomputeViewAttributes(View child)
    {
    }

    default void clearChildFocus(View child)
    {
    }

    default boolean getChildVisibleRect(View child, Rect rect, Point offset)
    {
        return true;
    }

    default View focusSearch(View view, int direction)
    {
        return null;
    }

    default View keyboardNavigationClusterSearch(View currentCluster, int direction)
    {
        return null;
    }

    default void bringChildToFront(View child)
    {
    }

    default void focusableViewAvailable(View view)
    {
    }

    default boolean showContextMenuForChild(View originalView)
    {
        return false;
    }

    default boolean showContextMenuForChild(View originalView, float x, float y)
    {
        return false;
    }

    default void createContextMenu(ContextMenu menu)
    {
    }

    default ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback)
    {
        return null;
    }

    default ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type)
    {
        return null;
    }

    default void childDrawableStateChanged(View child)
    {
    }

    default boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate)
    {
        return false;
    }

    default boolean requestSendAccessibilityEvent(View child, android.view.accessibility.AccessibilityEvent event)
    {
        return false;
    }

    default void childHasTransientStateChanged(View child, boolean hasTransientState)
    {
    }

    default void requestFitSystemWindows()
    {
    }

    default ViewParent getParentForAccessibility()
    {
        return null;
    }

    default void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType)
    {
    }

    default boolean canResolveLayoutDirection()
    {
        return true;
    }

    default boolean isLayoutDirectionResolved()
    {
        return true;
    }

    default int getLayoutDirection()
    {
        return View.LAYOUT_DIRECTION_LTR;
    }

    default boolean canResolveTextDirection()
    {
        return true;
    }

    default boolean isTextDirectionResolved()
    {
        return true;
    }

    default int getTextDirection()
    {
        return View.TEXT_DIRECTION_FIRST_STRONG;
    }

    default boolean canResolveTextAlignment()
    {
        return true;
    }

    default boolean isTextAlignmentResolved()
    {
        return true;
    }

    default int getTextAlignment()
    {
        return View.TEXT_ALIGNMENT_GRAVITY;
    }

    default boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return false;
    }

    default void onNestedScrollAccepted(View child, View target, int nestedScrollAxes)
    {
    }

    default void onStopNestedScroll(View target)
    {
    }

    default void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    {
    }

    default void onNestedPreScroll(View target, int dx, int dy, int[] consumed)
    {
    }

    default boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        return false;
    }

    default boolean onNestedPreFling(View target, float velocityX, float velocityY)
    {
        return false;
    }

    default boolean onNestedPrePerformAccessibilityAction(View target, int action, android.os.Bundle arguments)
    {
        return false;
    }

    default android.window.OnBackInvokedDispatcher findOnBackInvokedDispatcherForChild(View child, View requester)
    {
        return null;
    }
}

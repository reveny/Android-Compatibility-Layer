package android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;

public class ViewOverlay
{
    final OverlayViewGroup mOverlayViewGroup;

    ViewOverlay(Context context, View hostView)
    {
        mOverlayViewGroup = new OverlayViewGroup(context, hostView);
    }

    ViewGroup getOverlayView()
    {
        return mOverlayViewGroup;
    }

    public void add(Drawable drawable)
    {
        mOverlayViewGroup.add(drawable);
    }

    public void remove(Drawable drawable)
    {
        mOverlayViewGroup.remove(drawable);
    }

    public void clear()
    {
        mOverlayViewGroup.clear();
    }

    boolean isEmpty()
    {
        return mOverlayViewGroup.isEmpty();
    }

    static class OverlayViewGroup extends ViewGroup
    {
        final View mHostView;
        private final ArrayList<Drawable> mDrawables = new ArrayList<>();

        OverlayViewGroup(Context context, View hostView)
        {
            super(context);
            mHostView = hostView;
            setRight(hostView.getWidth());
            setBottom(hostView.getHeight());
            assignParent(new HostParent(hostView));
        }

        void add(Drawable drawable)
        {
            if (drawable == null) throw new IllegalArgumentException("drawable must be non-null");
            if (mDrawables.contains(drawable)) return;

            mDrawables.add(drawable);
            invalidate(drawable.getBounds());
            drawable.setCallback(this);
        }

        void remove(Drawable drawable)
        {
            if (drawable == null) throw new IllegalArgumentException("drawable must be non-null");
            if (!mDrawables.remove(drawable)) return;

            invalidate(drawable.getBounds());
            drawable.setCallback(null);
        }

        void add(View child)
        {
            if (child == null) throw new IllegalArgumentException("view must be non-null");

            ViewParent parent = child.getParent();
            if (parent instanceof ViewGroup)
            {
                ViewGroup group = (ViewGroup)parent;
                if (group != mHostView && group.getParent() != null && group.isAttachedToWindow())
                {
                    int[] parentLocation = new int[2];
                    int[] hostLocation = new int[2];
                    group.getLocationOnScreen(parentLocation);
                    mHostView.getLocationOnScreen(hostLocation);
                    child.offsetLeftAndRight(parentLocation[0] - hostLocation[0]);
                    child.offsetTopAndBottom(parentLocation[1] - hostLocation[1]);
                }

                group.removeView(child);
            }
            else if (parent != null)
            {
                return;
            }

            addViewKeepingBounds(child);
        }

        private void addViewKeepingBounds(View child)
        {
            int left = child.getLeft();
            int top = child.getTop();
            int right = child.getRight();
            int bottom = child.getBottom();

            ViewGroup.LayoutParams params = child.getLayoutParams();
            addViewInLayout(child, -1, params != null ? params : generateDefaultLayoutParams(), true);
            child.setLeftTopRightBottom(left, top, right, bottom);
            invalidate();
        }

        void remove(View child)
        {
            if (child == null) throw new IllegalArgumentException("view must be non-null");
            removeViewInLayout(child);
            invalidate();
        }

        void clear()
        {
            removeAllViewsInLayout();

            for (Drawable drawable : mDrawables) drawable.setCallback(null);
            mDrawables.clear();
            invalidate();
        }

        boolean isEmpty()
        {
            return getChildCount() == 0 && mDrawables.isEmpty();
        }

        @Override
        public void invalidateDrawable(Drawable drawable)
        {
            invalidate();
        }

        @Override
        public boolean verifyDrawable(Drawable who)
        {
            return mDrawables.contains(who);
        }

        @Override
        public void invalidate()
        {
            mHostView.invalidate();
        }

        @Override
        public void invalidate(Rect dirty)
        {
            mHostView.invalidate();
        }

        @Override
        public void invalidateChild(View child, Rect dirty)
        {
            mHostView.invalidate();
        }

        @Override
        public void requestLayout()
        {
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom)
        {
        }

        @Override
        protected void dispatchDraw(Canvas canvas)
        {
            for (int index = 0; index < mDrawables.size(); index++)
            {
                mDrawables.get(index).draw(canvas);
            }

            super.dispatchDraw(canvas);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event)
        {
            return false;
        }
    }

    static final class HostParent implements ViewParent
    {
        private final View mHost;

        HostParent(View host)
        {
            mHost = host;
        }

        @Override
        public void requestLayout()
        {
        }

        @Override
        public void invalidateChild(View child, Rect dirty)
        {
            mHost.invalidate();
        }

        @Override
        public void onDescendantInvalidated(View child, View target)
        {
            mHost.invalidate();
        }

        @Override
        public ViewParent getParent()
        {
            return mHost.getParent();
        }
    }
}

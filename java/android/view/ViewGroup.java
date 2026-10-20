package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import java.util.ArrayList;

public class ViewGroup extends View implements ViewParent
{
    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;

    public static final int PERSISTENT_NO_CACHE = 0x0;
    public static final int PERSISTENT_ANIMATION_CACHE = 0x1;
    public static final int PERSISTENT_SCROLLING_CACHE = 0x2;
    public static final int PERSISTENT_ALL_CACHES = 0x3;

    private final ArrayList<View> mChildren = new ArrayList<>();
    private View mTouchTarget;
    private boolean mDisallowIntercept;
    private boolean mIntercepting;

    private boolean mClipChildren = true;
    private boolean mClipToPadding = true;
    private int mDescendantFocusability = FOCUS_BEFORE_DESCENDANTS;
    private boolean mMotionEventSplitting = true;
    private boolean mTransitionGroup;

    public void setClipChildren(boolean clipChildren)
    {
        mClipChildren = clipChildren;
    }

    public boolean getClipChildren()
    {
        return mClipChildren;
    }

    public void setClipToPadding(boolean clipToPadding)
    {
        mClipToPadding = clipToPadding;
    }

    public boolean getClipToPadding()
    {
        return mClipToPadding;
    }

    public void setDescendantFocusability(int focusability)
    {
        mDescendantFocusability = focusability;
    }

    public int getDescendantFocusability()
    {
        return mDescendantFocusability;
    }

    public void setMotionEventSplittingEnabled(boolean split)
    {
        mMotionEventSplitting = split;
    }

    public boolean isMotionEventSplittingEnabled()
    {
        return mMotionEventSplitting;
    }

    public void setTransitionGroup(boolean transitionGroup)
    {
        mTransitionGroup = transitionGroup;
    }

    public boolean isTransitionGroup()
    {
        return mTransitionGroup;
    }

    public void setAddStatesFromChildren(boolean addStates)
    {
    }

    public void setAlwaysDrawnWithCacheEnabled(boolean always)
    {
    }

    public void setAnimationCacheEnabled(boolean enabled)
    {
    }

    public void setPersistentDrawingCache(int drawingCacheToKeep)
    {
    }

    public void setLayoutTransition(Object transition)
    {
    }


    public ViewGroup(Context context)
    {
        super(context);
    }

    public ViewGroup(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public ViewGroup(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public ViewGroup(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    @Override
    public void resolveLayoutParams()
    {
        super.resolveLayoutParams();

        for (View child : mChildren)
        {
            child.resolveLayoutParams();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    protected LayoutParams generateLayoutParams(LayoutParams source)
    {
        return source;
    }

    protected boolean checkLayoutParams(LayoutParams layoutParams)
    {
        return layoutParams != null;
    }

    public int getChildCount()
    {
        return mChildren.size();
    }

    public View getChildAt(int index)
    {
        if (index < 0 || index >= mChildren.size()) return null;
        return mChildren.get(index);
    }

    public void addView(View child)
    {
        addView(child, mChildren.size(), child.getLayoutParams());
    }

    public void addView(View child, int index)
    {
        addView(child, index, child.getLayoutParams());
    }

    public void addView(View child, LayoutParams layoutParams)
    {
        addView(child, mChildren.size(), layoutParams);
    }

    public void addView(View child, int index, LayoutParams layoutParams)
    {
        insertChild(child, index, layoutParams);

        requestLayout();
        invalidate();
    }

    private void insertChild(View child, int index, LayoutParams layoutParams)
    {
        if (child == null) throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        if (child.getParent() != null)
        {
            throw new IllegalStateException("The specified child already has a parent. Call removeView() on the child's parent first.");
        }

        LayoutParams resolved = layoutParams != null ? layoutParams : generateDefaultLayoutParams();
        if (!checkLayoutParams(resolved)) resolved = generateLayoutParams(resolved);
        child.setLayoutParams(resolved);
        child.assignParent(this);

        int position = index < 0 || index > mChildren.size() ? mChildren.size() : index;
        mChildren.add(position, child);

        if (isAttachedToWindow()) child.dispatchAttachedToWindow();
    }

    public int indexOfChild(View child)
    {
        return mChildren.indexOf(child);
    }

    public void removeViewAt(int index)
    {
        View child = getChildAt(index);
        if (child != null) removeView(child);
    }

    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed)
    {
        LayoutParams layoutParams = child.getLayoutParams();
        if (layoutParams == null) layoutParams = generateDefaultLayoutParams();

        int horizontalMargins = 0;
        int verticalMargins = 0;
        if (layoutParams instanceof MarginLayoutParams)
        {
            MarginLayoutParams margins = (MarginLayoutParams)layoutParams;
            horizontalMargins = margins.leftMargin + margins.rightMargin;
            verticalMargins = margins.topMargin + margins.bottomMargin;
        }

        int width = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight() + horizontalMargins + widthUsed,
                                       layoutParams.width);
        int height = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom() + verticalMargins + heightUsed,
                                        layoutParams.height);
        child.measure(width, height);
    }

    public void removeView(View child)
    {
        if (!mChildren.remove(child)) return;

        if (mTouchTarget == child) mTouchTarget = null;

        child.dispatchDetachedFromWindow();
        child.assignParent(null);
        requestLayout();
        invalidate();
    }

    public void removeAllViews()
    {
        mTouchTarget = null;

        for (View child : mChildren)
        {
            child.assignParent(null);
        }
        mChildren.clear();
        requestLayout();
        invalidate();
    }

    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                             getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec)
    {
        for (View child : mChildren)
        {
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec)
    {
        LayoutParams layoutParams = child.getLayoutParams();
        if (layoutParams == null) layoutParams = generateDefaultLayoutParams();

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        int width = getChildMeasureSpec(parentWidthMeasureSpec, horizontalPadding, layoutParams.width);
        int height = getChildMeasureSpec(parentHeightMeasureSpec, verticalPadding, layoutParams.height);
        child.measure(width, height);
    }

    public static int getChildMeasureSpec(int spec, int padding, int childDimension)
    {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        int available = Math.max(0, specSize - padding);

        int resultSize = 0;
        int resultMode = MeasureSpec.UNSPECIFIED;

        if (specMode == MeasureSpec.EXACTLY || specMode == MeasureSpec.AT_MOST)
        {
            if (childDimension >= 0)
            {
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            }
            else if (childDimension == LayoutParams.MATCH_PARENT)
            {
                resultSize = available;
                resultMode = specMode == MeasureSpec.EXACTLY ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            }
            else
            {
                resultSize = available;
                resultMode = MeasureSpec.AT_MOST;
            }

            return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
        }

        if (childDimension >= 0)
        {
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        }
        else
        {
            resultSize = available;
            resultMode = MeasureSpec.UNSPECIFIED;
        }

        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        for (View child : mChildren)
        {
            if (child.getVisibility() == GONE) continue;
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow)
    {
        if (mDisallowIntercept == disallow) return;

        mDisallowIntercept = disallow;

        ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(disallow);
    }

    private void cancelTouchTarget(MotionEvent event)
    {
        View target = mTouchTarget;
        mTouchTarget = null;

        if (target == null) return;

        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, event.getX(), event.getY(), 0);

        try
        {
            dispatchToChild(target, cancel);
        }
        finally
        {
            cancel.recycle();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            mTouchTarget = null;
            mDisallowIntercept = false;
            mIntercepting = false;
        }

        if (!mDisallowIntercept && !mIntercepting && onInterceptTouchEvent(event))
        {
            mIntercepting = true;
            cancelTouchTarget(event);
        }

        if (mIntercepting)
        {
            boolean handled = super.dispatchTouchEvent(event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) mIntercepting = false;
            return handled;
        }

        if (mTouchTarget != null)
        {
            boolean handled = dispatchToChild(mTouchTarget, event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) mTouchTarget = null;
            return handled;
        }

        if (action == MotionEvent.ACTION_DOWN)
        {
            float scrolledX = event.getX() + getScrollX();
            float scrolledY = event.getY() + getScrollY();

            for (int index = getChildCount() - 1; index >= 0; index--)
            {
                View child = getChildAt(index);
                if (child == null || child.getVisibility() != VISIBLE) continue;

                float localX = scrolledX - child.getLeft();
                float localY = scrolledY - child.getTop();

                if (!child.containsLocal(localX, localY)) continue;
                if (!dispatchToChild(child, event)) continue;

                mTouchTarget = child;
                return true;
            }
        }

        return super.dispatchTouchEvent(event);
    }

    private boolean dispatchToChild(View child, MotionEvent event)
    {
        float offsetX = getScrollX() - child.getLeft();
        float offsetY = getScrollY() - child.getTop();

        event.offsetLocation(offsetX, offsetY);

        boolean handled;
        try
        {
            handled = child.dispatchTouchEvent(event);
        }
        finally
        {
            event.offsetLocation(-offsetX, -offsetY);
        }

        if (TRACE_INPUT)
        {
            android.util.Log.i("ViewGroup", child.getClass().getName() + ".dispatchTouchEvent(" + event.getActionMasked()
                                                + ") returned " + handled);
        }

        return handled;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event)
    {
        for (int index = getChildCount() - 1; index >= 0; index--)
        {
            View child = getChildAt(index);
            if (child == null || child.getVisibility() != VISIBLE) continue;

            if (!child.containsLocal(event.getX() - child.getLeft(), event.getY() - child.getTop())) continue;

            float offsetX = -child.getLeft();
            float offsetY = -child.getTop();

            event.offsetLocation(offsetX, offsetY);

            boolean handled;
            try
            {
                handled = child.dispatchGenericMotionEvent(event);
            }
            finally
            {
                event.offsetLocation(-offsetX, -offsetY);
            }

            if (handled) return true;
        }

        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        for (int index = getChildCount() - 1; index >= 0; index--)
        {
            View child = getChildAt(index);
            if (child != null && child.dispatchKeyEvent(event)) return true;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets)
    {
        WindowInsets applied = super.dispatchApplyWindowInsets(insets);
        if (applied == null || applied == WindowInsets.CONSUMED) return WindowInsets.CONSUMED;

        for (View child : mChildren)
        {
            if (child != null) child.dispatchApplyWindowInsets(applied);
        }

        return applied;
    }

    private View mFocusedChild;

    public View getFocusedChild()
    {
        return mFocusedChild;
    }

    public void requestChildFocus(View child, View focused)
    {
        mFocusedChild = child;

        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).requestChildFocus(this, focused);
    }

    public void clearChildFocus(View child)
    {
        if (mFocusedChild == child) mFocusedChild = null;

        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).clearChildFocus(this);
    }

    private android.animation.LayoutTransition mLayoutTransition;

    public void setLayoutTransition(android.animation.LayoutTransition transition)
    {
        mLayoutTransition = transition;
    }

    public android.animation.LayoutTransition getLayoutTransition()
    {
        return mLayoutTransition;
    }

    private boolean mChildrenDrawingOrderEnabled;

    protected void setChildrenDrawingOrderEnabled(boolean enabled)
    {
        mChildrenDrawingOrderEnabled = enabled;
    }

    protected boolean isChildrenDrawingOrderEnabled()
    {
        return mChildrenDrawingOrderEnabled;
    }

    protected int getChildDrawingOrder(int childCount, int drawingPosition)
    {
        return drawingPosition;
    }

    public int getChildDrawingOrder(int drawingPosition)
    {
        return getChildDrawingOrder(getChildCount(), drawingPosition);
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        long drawingTime = getDrawingTime();
        int count = getChildCount();

        for (int position = 0; position < count; position++)
        {
            int index = mChildrenDrawingOrderEnabled ? getChildDrawingOrder(count, position) : position;

            if (index < 0 || index >= mChildren.size()) continue;

            View child = mChildren.get(index);

            if (child == null || child.getVisibility() != VISIBLE) continue;

            drawChild(canvas, child, drawingTime);
        }
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime)
    {
        float alpha = child.getAlpha();

        if (alpha <= 0) return false;

        int saveCount = canvas.save();

        canvas.translate(child.getLeft(), child.getTop());

        if (!child.hasIdentityMatrix()) canvas.concat(child.getMatrix());

        if (mClipChildren) canvas.clipRect(0, 0, child.getWidth(), child.getHeight());

        if (alpha < 1)
        {
            canvas.saveLayerAlpha(0, 0, child.getWidth(), child.getHeight(), Math.round(alpha * 255));
        }

        child.draw(canvas);
        canvas.restoreToCount(saveCount);
        return false;
    }

    @Override
    public View findViewById(int id)
    {
        View self = super.findViewById(id);
        if (self != null) return self;

        for (View child : mChildren)
        {
            View found = child.findViewById(id);
            if (found != null) return found;
        }

        return null;
    }

    @Override
    public void invalidateChild(View child, android.graphics.Rect dirty)
    {
        invalidate();
    }

    public boolean shouldDelayChildPressedState()
    {
        return false;
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params)
    {
        return addViewInLayout(child, index, params, false);
    }

    protected boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout)
    {
        if (child == null) return false;

        insertChild(child, index, params);
        if (!preventRequestLayout) requestLayout();

        invalidate();
        return true;
    }

    protected void attachViewToParent(View child, int index, LayoutParams params)
    {
        insertChild(child, index, params);
    }

    protected void detachViewFromParent(View child)
    {
        removeView(child);
    }

    protected void detachViewFromParent(int index)
    {
        removeView(getChildAt(index));
    }

    protected void detachAllViewsFromParent()
    {
        removeAllViews();
    }

    protected void removeDetachedView(View child, boolean animate)
    {
    }

    @Override
    public void onDescendantInvalidated(View child, View target)
    {
        ViewParent parent = getParent();

        if (parent != null) parent.onDescendantInvalidated(this, target);
        else invalidate();
    }

    @Override
    void dispatchAttachedToWindow()
    {
        super.dispatchAttachedToWindow();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child != null) child.dispatchAttachedToWindow();
        }
    }

    @Override
    void dispatchDetachedFromWindow()
    {
        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child != null) child.dispatchDetachedFromWindow();
        }

        super.dispatchDetachedFromWindow();
    }

    public static class LayoutParams
    {
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;

        private static StyleableSet sStyleable;

        public int width;
        public int height;

        public LayoutParams(int width, int height)
        {
            this.width = width;
            this.height = height;
        }

        public LayoutParams(LayoutParams source)
        {
            width = source.width;
            height = source.height;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            width = WRAP_CONTENT;
            height = WRAP_CONTENT;
            if (attributes != null) readDimensions(context, attributes);
        }

        public void resolveLayoutDirection(int layoutDirection)
        {
        }

        private void readDimensions(Context context, AttributeSet attributes)
        {
            StyleableSet styleable = layoutStyleable(context);
            if (styleable.isEmpty()) return;

            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), 0, 0);

            int widthIndex = styleable.indexOf("layout_width");
            int heightIndex = styleable.indexOf("layout_height");

            if (widthIndex >= 0) width = array.getLayoutDimension(widthIndex, WRAP_CONTENT);
            if (heightIndex >= 0) height = array.getLayoutDimension(heightIndex, WRAP_CONTENT);

            array.recycle();
        }

        static StyleableSet layoutStyleable(Context context)
        {
            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "layout_width", "layout_height");
            return sStyleable;
        }
    }

    public static class MarginLayoutParams extends LayoutParams
    {
        public static final int DEFAULT_MARGIN_RELATIVE = Integer.MIN_VALUE;

        private static StyleableSet sMarginStyleable;

        public int leftMargin;
        public int topMargin;
        public int rightMargin;
        public int bottomMargin;

        private int mStartMargin = DEFAULT_MARGIN_RELATIVE;
        private int mEndMargin = DEFAULT_MARGIN_RELATIVE;
        private int mLayoutDirection = View.LAYOUT_DIRECTION_LTR;

        public MarginLayoutParams(int width, int height)
        {
            super(width, height);
        }

        public MarginLayoutParams(LayoutParams source)
        {
            super(source);
        }

        public MarginLayoutParams(MarginLayoutParams source)
        {
            super(source);
            leftMargin = source.leftMargin;
            topMargin = source.topMargin;
            rightMargin = source.rightMargin;
            bottomMargin = source.bottomMargin;
            mStartMargin = source.mStartMargin;
            mEndMargin = source.mEndMargin;
            mLayoutDirection = source.mLayoutDirection;
        }

        public MarginLayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
            if (attributes != null) readMargins(context, attributes);
        }

        private void readMargins(Context context, AttributeSet attributes)
        {
            StyleableSet styleable = marginStyleable(context);
            if (styleable.isEmpty()) return;

            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), 0, 0);

            int all = read(array, styleable, "layout_margin", -1);
            if (all >= 0) setMargins(all, all, all, all);

            leftMargin = read(array, styleable, "layout_marginLeft", leftMargin);
            topMargin = read(array, styleable, "layout_marginTop", topMargin);
            rightMargin = read(array, styleable, "layout_marginRight", rightMargin);
            bottomMargin = read(array, styleable, "layout_marginBottom", bottomMargin);

            mStartMargin = read(array, styleable, "layout_marginStart", DEFAULT_MARGIN_RELATIVE);
            mEndMargin = read(array, styleable, "layout_marginEnd", DEFAULT_MARGIN_RELATIVE);

            array.recycle();
        }

        private static int read(TypedArray array, StyleableSet styleable, String name, int fallback)
        {
            int index = styleable.indexOf(name);
            return index < 0 ? fallback : array.getDimensionPixelSize(index, fallback);
        }

        private static StyleableSet marginStyleable(Context context)
        {
            if (sMarginStyleable == null)
            {
                sMarginStyleable = StyleableSet.of(context.getResources(), "layout_margin", "layout_marginLeft", "layout_marginTop",
                                                   "layout_marginRight", "layout_marginBottom", "layout_marginStart", "layout_marginEnd");
            }
            return sMarginStyleable;
        }

        public void setMargins(int left, int top, int right, int bottom)
        {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }

        public void setMarginStart(int start)
        {
            mStartMargin = start;
        }

        public void setMarginEnd(int end)
        {
            mEndMargin = end;
        }

        public int getMarginStart()
        {
            if (mStartMargin != DEFAULT_MARGIN_RELATIVE) return mStartMargin;
            return mLayoutDirection == View.LAYOUT_DIRECTION_RTL ? rightMargin : leftMargin;
        }

        public int getMarginEnd()
        {
            if (mEndMargin != DEFAULT_MARGIN_RELATIVE) return mEndMargin;
            return mLayoutDirection == View.LAYOUT_DIRECTION_RTL ? leftMargin : rightMargin;
        }

        public boolean isMarginRelative()
        {
            return mStartMargin != DEFAULT_MARGIN_RELATIVE || mEndMargin != DEFAULT_MARGIN_RELATIVE;
        }

        public void setLayoutDirection(int layoutDirection)
        {
            if (layoutDirection != View.LAYOUT_DIRECTION_LTR && layoutDirection != View.LAYOUT_DIRECTION_RTL) return;
            mLayoutDirection = layoutDirection;
        }

        public int getLayoutDirection()
        {
            return mLayoutDirection;
        }

        @Override
        public void resolveLayoutDirection(int layoutDirection)
        {
            setLayoutDirection(layoutDirection);

            if (!isMarginRelative()) return;

            if (layoutDirection == View.LAYOUT_DIRECTION_RTL)
            {
                if (mStartMargin != DEFAULT_MARGIN_RELATIVE) rightMargin = mStartMargin;
                if (mEndMargin != DEFAULT_MARGIN_RELATIVE) leftMargin = mEndMargin;
                return;
            }

            if (mStartMargin != DEFAULT_MARGIN_RELATIVE) leftMargin = mStartMargin;
            if (mEndMargin != DEFAULT_MARGIN_RELATIVE) rightMargin = mEndMargin;
        }
    }
}

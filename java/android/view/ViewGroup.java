package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import java.util.ArrayList;

public class ViewGroup extends View implements ViewParent, ViewManager
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

    public void setAddStatesFromChildren(boolean addsStates)
    {
        mAddStatesFromChildren = addsStates;
        refreshDrawableState();
    }

    public boolean addStatesFromChildren()
    {
        return mAddStatesFromChildren;
    }

    public void setAlwaysDrawnWithCacheEnabled(boolean always)
    {
        mAlwaysDrawnWithCache = always;
    }

    public boolean isAlwaysDrawnWithCacheEnabled()
    {
        return mAlwaysDrawnWithCache;
    }

    public void setAnimationCacheEnabled(boolean enabled)
    {
        mAnimationCacheEnabled = enabled;
    }

    public boolean isAnimationCacheEnabled()
    {
        return mAnimationCacheEnabled;
    }

    public void setPersistentDrawingCache(int drawingCacheToKeep)
    {
        mPersistentDrawingCache = drawingCacheToKeep & PERSISTENT_ALL_CACHES;
    }

    public int getPersistentDrawingCache()
    {
        return mPersistentDrawingCache;
    }

    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0;
    public static final int LAYOUT_MODE_OPTICAL_BOUNDS = 1;

    private boolean mAddStatesFromChildren;
    private boolean mAlwaysDrawnWithCache = true;
    private boolean mAnimationCacheEnabled = true;
    private int mPersistentDrawingCache = PERSISTENT_SCROLLING_CACHE;
    private boolean mTouchscreenBlocksFocus;
    private int mLayoutMode = LAYOUT_MODE_CLIP_BOUNDS;
    private boolean mSuppressLayout;
    private boolean mLayoutCalledWhileSuppressed;
    private int mNestedScrollAxes;
    private int mChildCountWithTransientState;
    private final ArrayList<View> mDisappearingChildren = new ArrayList<>();
    private final android.view.animation.Transformation mChildTransformation = new android.view.animation.Transformation();
    private android.view.animation.LayoutAnimationController mLayoutAnimationController;
    private android.view.animation.Animation.AnimationListener mAnimationListener;
    private boolean mLayoutAnimationPending;
    private boolean mLayoutAnimationRunning;


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
            throw new IllegalStateException("The specified child already has a parent. Call removeView() on the child's parent first. "
                                            + "The child is a " + child.getClass().getName() + ", its parent a "
                                            + child.getParent().getClass().getName() + ", and this is a " + getClass().getName());
        }

        LayoutParams resolved = layoutParams != null ? layoutParams : generateDefaultLayoutParams();
        if (!checkLayoutParams(resolved)) resolved = generateLayoutParams(resolved);
        child.setLayoutParams(resolved);
        child.assignParent(this);

        mDisappearingChildren.remove(child);

        if (mLayoutTransition != null) mLayoutTransition.addChild(this, child);

        int position = index < 0 || index > mChildren.size() ? mChildren.size() : index;
        mChildren.add(position, child);

        if (isAttachedToWindow()) child.dispatchAttachedToWindow();

        if (child.hasTransientState()) childHasTransientStateChanged(child, true);

        onViewAdded(child);
        if (mHierarchyListener != null) mHierarchyListener.onChildViewAdded(this, child);
    }

    private OnHierarchyChangeListener mHierarchyListener;
    private final java.util.ArrayList<View> mTransitioningViews = new java.util.ArrayList<>();

    public interface OnHierarchyChangeListener
    {
        void onChildViewAdded(View parent, View child);

        void onChildViewRemoved(View parent, View child);
    }

    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener)
    {
        mHierarchyListener = listener;
    }

    public void onViewAdded(View child)
    {
    }

    public void onViewRemoved(View child)
    {
    }

    private boolean removeChildInternal(View child)
    {
        if (child == null) return false;

        int index = mChildren.indexOf(child);
        if (index < 0) return false;

        if (mTouchTarget == child) mTouchTarget = null;

        if (child.hasFocus())
        {
            View focused = child.findFocus();
            if (focused != null) focused.clearFocus();
            if (mFocusedChild == child) mFocusedChild = null;
        }

        if (mLayoutTransition != null) mLayoutTransition.removeChild(this, child);

        mChildren.remove(index);

        android.view.animation.Animation animation = child.getAnimation();
        boolean animating = animation != null && !animation.hasEnded();

        if (animating)
        {
            if (!mDisappearingChildren.contains(child)) mDisappearingChildren.add(child);
        }
        else if (!mTransitioningViews.contains(child))
        {
            child.dispatchDetachedFromWindow();
        }

        if (child.hasTransientState()) childHasTransientStateChanged(child, false);

        child.assignParent(null);

        onViewRemoved(child);
        if (mHierarchyListener != null) mHierarchyListener.onChildViewRemoved(this, child);
        return true;
    }

    public void removeViewInLayout(View child)
    {
        removeChildInternal(child);
    }

    public void removeViewsInLayout(int start, int count)
    {
        int end = Math.min(mChildren.size(), start + count);
        for (int index = end - 1; index >= start && index >= 0; index--) removeChildInternal(mChildren.get(index));
    }

    public void removeViews(int start, int count)
    {
        removeViewsInLayout(start, count);
        requestLayout();
        invalidate();
    }

    public void removeAllViewsInLayout()
    {
        for (int index = mChildren.size() - 1; index >= 0; index--) removeChildInternal(mChildren.get(index));
    }

    protected void removeDetachedView(View child, boolean animate)
    {
        if (child == null) return;
        if (mTouchTarget == child) mTouchTarget = null;

        child.dispatchDetachedFromWindow();
        if (child.getParent() == this) child.assignParent(null);

        onViewRemoved(child);
        if (mHierarchyListener != null) mHierarchyListener.onChildViewRemoved(this, child);
    }

    protected void detachViewsFromParent(int start, int count)
    {
        int end = Math.min(mChildren.size(), start + count);
        for (int index = end - 1; index >= start && index >= 0; index--) mChildren.remove(index);
    }

    protected void detachAllViewsFromParent()
    {
        mChildren.clear();
    }

    protected void cleanupLayoutState(View child)
    {
    }

    public void startViewTransition(View view)
    {
        if (view != null && view.getParent() == this && !mTransitioningViews.contains(view)) mTransitioningViews.add(view);
    }

    public void endViewTransition(View view)
    {
        if (!mTransitioningViews.remove(view)) return;

        if (view.getParent() == null || !mChildren.contains(view)) view.dispatchDetachedFromWindow();
    }

    public void bringChildToFront(View child)
    {
        int index = mChildren.indexOf(child);
        if (index < 0) return;

        mChildren.remove(index);
        mChildren.add(child);
        requestLayout();
        invalidate();
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
        if (!removeChildInternal(child)) return;

        requestLayout();
        invalidate();
    }

    public void removeAllViews()
    {
        removeAllViewsInLayout();
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

        boolean mayIntercept = action == MotionEvent.ACTION_DOWN || mTouchTarget != null;

        if (mayIntercept && !mDisallowIntercept && !mIntercepting && onInterceptTouchEvent(event))
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
        if (applied == null || applied.isConsumed()) return applied == null ? WindowInsets.CONSUMED : applied;

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

        if (mLayoutAnimationPending && mLayoutAnimationController != null) startLayoutAnimationOnChildren();

        int saveCount = -1;
        if (mClipToPadding)
        {
            saveCount = canvas.save();
            canvas.clipRect(getScrollX() + getPaddingLeft(), getScrollY() + getPaddingTop(),
                            getScrollX() + getWidth() - getPaddingRight(), getScrollY() + getHeight() - getPaddingBottom());
        }

        for (int position = 0; position < count; position++)
        {
            int index = mChildrenDrawingOrderEnabled ? getChildDrawingOrder(count, position) : position;

            if (index < 0 || index >= mChildren.size()) continue;

            View child = mChildren.get(index);

            if (child == null) continue;
            if (child.getVisibility() != VISIBLE && child.getAnimation() == null) continue;

            drawChild(canvas, child, drawingTime);
        }

        if (!mDisappearingChildren.isEmpty())
        {
            ArrayList<View> disappearing = new ArrayList<>(mDisappearingChildren);
            for (int index = disappearing.size() - 1; index >= 0; index--)
            {
                drawChild(canvas, disappearing.get(index), drawingTime);
            }
        }

        if (saveCount >= 0) canvas.restoreToCount(saveCount);

        if (mLayoutAnimationRunning && mLayoutAnimationController != null && mLayoutAnimationController.isDone()) finishLayoutAnimation();
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime)
    {
        android.view.animation.Animation animation = child.getAnimation();
        android.view.animation.Transformation transformation = null;
        boolean more = false;

        if (animation != null)
        {
            transformation = mChildTransformation;
            transformation.clear();
            more = child.applyLegacyAnimation(this, drawingTime, animation, transformation);
        }

        float alpha = child.getAlpha() * child.getTransitionAlpha();
        if (transformation != null && (transformation.getTransformationType() & android.view.animation.Transformation.TYPE_ALPHA) != 0)
        {
            alpha *= transformation.getAlpha();
        }

        if (alpha > 0 && (child.getVisibility() == VISIBLE || animation != null))
        {
            int saveCount = canvas.save();

            canvas.translate(child.getLeft(), child.getTop());

            if (transformation != null && (transformation.getTransformationType() & android.view.animation.Transformation.TYPE_MATRIX) != 0)
            {
                canvas.concat(transformation.getMatrix());
            }

            android.graphics.Matrix animationMatrix = child.getAnimationMatrix();
            if (animationMatrix != null) canvas.concat(animationMatrix);

            if (!child.hasIdentityMatrix()) canvas.concat(child.getMatrix());

            if (mClipChildren) canvas.clipRect(0, 0, child.getWidth(), child.getHeight());

            if (alpha < 1)
            {
                canvas.saveLayerAlpha(0, 0, child.getWidth(), child.getHeight(), Math.round(Math.min(1.0f, alpha) * 255));
            }

            child.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        if (animation != null && !more && animation.hasEnded()) finishAnimatingView(child, animation);

        return more;
    }

    void finishAnimatingView(View view, android.view.animation.Animation animation)
    {
        if (mDisappearingChildren.remove(view))
        {
            if (!mTransitioningViews.contains(view)) view.dispatchDetachedFromWindow();
            view.clearAnimation();
            invalidate();
        }

        if (animation != null && !animation.getFillAfter() && view.getAnimation() == animation) view.clearAnimation();

        if (view.consumeAnimationStarted()) view.onAnimationEnd();
    }

    public void clearDisappearingChildren()
    {
        ArrayList<View> disappearing = new ArrayList<>(mDisappearingChildren);
        mDisappearingChildren.clear();

        for (View view : disappearing)
        {
            if (!mTransitioningViews.contains(view)) view.dispatchDetachedFromWindow();
            view.clearAnimation();
        }

        invalidate();
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
        if (child == null) return;

        if (params != null) child.setLayoutParams(checkLayoutParams(params) ? params : generateLayoutParams(params));
        child.assignParent(this);

        int position = index < 0 || index > mChildren.size() ? mChildren.size() : index;
        mChildren.add(position, child);

        if (isAttachedToWindow()) child.dispatchAttachedToWindow();
    }

    protected void detachViewFromParent(View child)
    {
        if (child == null) return;
        if (mTouchTarget == child) mTouchTarget = null;

        mChildren.remove(child);
    }

    protected void detachViewFromParent(int index)
    {
        detachViewFromParent(getChildAt(index));
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
        if (!mDisappearingChildren.isEmpty())
        {
            ArrayList<View> disappearing = new ArrayList<>(mDisappearingChildren);
            mDisappearingChildren.clear();

            for (View view : disappearing)
            {
                if (!mTransitioningViews.contains(view)) view.dispatchDetachedFromWindow();
                view.clearAnimation();
            }
        }

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child != null) child.dispatchDetachedFromWindow();
        }

        super.dispatchDetachedFromWindow();
    }

    @Override
    public void layout(int left, int top, int right, int bottom)
    {
        if (mSuppressLayout)
        {
            mLayoutCalledWhileSuppressed = true;
            return;
        }

        super.layout(left, top, right, bottom);
    }

    public void suppressLayout(boolean suppress)
    {
        mSuppressLayout = suppress;

        if (!suppress && mLayoutCalledWhileSuppressed)
        {
            requestLayout();
            mLayoutCalledWhileSuppressed = false;
        }
    }

    public boolean isLayoutSuppressed()
    {
        return mSuppressLayout;
    }

    public int getLayoutMode()
    {
        return mLayoutMode;
    }

    public void setLayoutMode(int layoutMode)
    {
        if (mLayoutMode == layoutMode) return;

        mLayoutMode = layoutMode;
        requestLayout();
    }

    public void addView(View child, int width, int height)
    {
        LayoutParams params = generateDefaultLayoutParams();
        params.width = width;
        params.height = height;
        addView(child, -1, params);
    }

    @Override
    public void updateViewLayout(View view, LayoutParams params)
    {
        if (!checkLayoutParams(params)) throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        if (view.getParent() != this) throw new IllegalArgumentException("Given view not a child of " + this);

        view.setLayoutParams(params);
    }

    @Override
    public boolean hasFocus()
    {
        return isFocused() || mFocusedChild != null;
    }

    @Override
    public View findFocus()
    {
        if (isFocused()) return this;
        if (mFocusedChild != null) return mFocusedChild.findFocus();

        return null;
    }

    @Override
    public boolean hasFocusable()
    {
        if (getVisibility() != VISIBLE) return false;
        if (isFocusable()) return true;
        if (mDescendantFocusability == FOCUS_BLOCK_DESCENDANTS) return false;

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.hasFocusable()) return true;
        }

        return false;
    }

    @Override
    public boolean hasExplicitFocusable()
    {
        return hasFocusable();
    }

    @Override
    public boolean requestFocus(int direction, android.graphics.Rect previouslyFocusedRect)
    {
        switch (mDescendantFocusability)
        {
            case FOCUS_BLOCK_DESCENDANTS:
                return super.requestFocus(direction, previouslyFocusedRect);
            case FOCUS_AFTER_DESCENDANTS:
            {
                boolean took = onRequestFocusInDescendants(direction, previouslyFocusedRect);
                return took || super.requestFocus(direction, previouslyFocusedRect);
            }
            default:
            {
                boolean took = super.requestFocus(direction, previouslyFocusedRect);
                return took || onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
        }
    }

    protected boolean onRequestFocusInDescendants(int direction, android.graphics.Rect previouslyFocusedRect)
    {
        int count = mChildren.size();
        int start;
        int increment;
        int end;

        if ((direction & FOCUS_FORWARD) != 0)
        {
            start = 0;
            increment = 1;
            end = count;
        }
        else
        {
            start = count - 1;
            increment = -1;
            end = -1;
        }

        for (int index = start; index != end; index += increment)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE && child.requestFocus(direction, previouslyFocusedRect)) return true;
        }

        return false;
    }

    @Override
    public boolean restoreDefaultFocus()
    {
        View focusedByDefault = findDefaultFocus(this);
        if (focusedByDefault != null && focusedByDefault != this && focusedByDefault.requestFocus()) return true;

        return super.restoreDefaultFocus();
    }

    private static View findDefaultFocus(View view)
    {
        if (view.isFocusedByDefault() && view.getVisibility() == VISIBLE && view.isFocusable()) return view;
        if (!(view instanceof ViewGroup)) return null;

        ViewGroup group = (ViewGroup)view;
        for (int index = 0; index < group.mChildren.size(); index++)
        {
            View found = findDefaultFocus(group.mChildren.get(index));
            if (found != null) return found;
        }

        return null;
    }

    @Override
    boolean hasDefaultFocus()
    {
        return findDefaultFocus(this) != null;
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode)
    {
        int focusableCount = views.size();
        boolean blockFocusForTouchscreen = shouldBlockFocusForTouchscreen();
        boolean focusSelf = isFocusableInTouchMode() || !blockFocusForTouchscreen;

        if (mDescendantFocusability == FOCUS_BLOCK_DESCENDANTS)
        {
            if (focusSelf) super.addFocusables(views, direction, focusableMode);
            return;
        }

        if (blockFocusForTouchscreen) focusableMode |= FOCUSABLES_TOUCH_MODE;

        if (mDescendantFocusability == FOCUS_BEFORE_DESCENDANTS && focusSelf) super.addFocusables(views, direction, focusableMode);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.addFocusables(views, direction, focusableMode);
        }

        if (mDescendantFocusability == FOCUS_AFTER_DESCENDANTS && focusSelf && focusableCount == views.size())
        {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    @Override
    public void addKeyboardNavigationClusters(java.util.Collection<View> views, int direction)
    {
        int focusedCount = views.size();

        super.addKeyboardNavigationClusters(views, direction);

        if (focusedCount != views.size()) return;
        if (mDescendantFocusability == FOCUS_BLOCK_DESCENDANTS) return;

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.addKeyboardNavigationClusters(views, direction);
        }
    }

    public void setTouchscreenBlocksFocus(boolean touchscreenBlocksFocus)
    {
        mTouchscreenBlocksFocus = touchscreenBlocksFocus;
    }

    public boolean getTouchscreenBlocksFocus()
    {
        return mTouchscreenBlocksFocus;
    }

    boolean shouldBlockFocusForTouchscreen()
    {
        return mTouchscreenBlocksFocus && !(isKeyboardNavigationCluster() && hasFocus());
    }

    @Override
    public View focusSearch(View focused, int direction)
    {
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) return parent.focusSearch(focused, direction);

        return FocusFinder.getInstance().findNextFocus(this, focused, direction);
    }

    @Override
    public void focusableViewAvailable(View view)
    {
        ViewParent parent = getParent();
        if (parent == null) return;
        if (mDescendantFocusability == FOCUS_BLOCK_DESCENDANTS) return;
        if (isFocused() && mDescendantFocusability != FOCUS_AFTER_DESCENDANTS) return;

        parent.focusableViewAvailable(view);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction)
    {
        return mFocusedChild != null && mFocusedChild.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean showContextMenuForChild(View originalView)
    {
        ViewParent parent = getParent();
        return parent != null && parent.showContextMenuForChild(originalView);
    }

    @Override
    public boolean showContextMenuForChild(View originalView, float x, float y)
    {
        ViewParent parent = getParent();
        return parent != null && parent.showContextMenuForChild(originalView, x, y);
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback)
    {
        return startActionModeForChild(originalView, callback, ActionMode.TYPE_PRIMARY);
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type)
    {
        ViewParent parent = getParent();
        if (parent == null) return null;

        return parent.startActionModeForChild(originalView, callback, type);
    }

    @Override
    public android.window.OnBackInvokedDispatcher findOnBackInvokedDispatcherForChild(View child, View requester)
    {
        ViewParent parent = getParent();
        if (parent == null) return null;

        return parent.findOnBackInvokedDispatcherForChild(this, requester);
    }

    @Override
    public void childDrawableStateChanged(View child)
    {
        if (mAddStatesFromChildren) refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        if (!mAddStatesFromChildren) return super.onCreateDrawableState(extraSpace);

        int need = 0;
        for (int index = 0; index < mChildren.size(); index++)
        {
            int[] childState = mChildren.get(index).getDrawableState();
            if (childState != null) need += childState.length;
        }

        int[] state = super.onCreateDrawableState(extraSpace + need);

        for (int index = 0; index < mChildren.size(); index++)
        {
            int[] childState = mChildren.get(index).getDrawableState();
            if (childState != null) state = mergeDrawableStates(state, childState);
        }

        return state;
    }

    @Override
    public void dispatchSetSelected(boolean selected)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).setSelected(selected);
        }
    }

    @Override
    public void dispatchSetActivated(boolean activated)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).setActivated(activated);
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (!pressed || (!child.isClickable() && !child.isLongClickable())) child.setPressed(pressed);
        }
    }

    @Override
    public void dispatchDrawableHotspotChanged(float x, float y)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            boolean nonActionable = !child.isClickable() && !child.isLongClickable();
            boolean duplicatesState = child.isDuplicateParentStateEnabled();

            if (nonActionable || duplicatesState)
            {
                float[] point = {x, y};
                transformPointToViewLocal(point, child);
                child.drawableHotspotChanged(point[0], point[1]);
            }
        }
    }

    void transformPointToViewLocal(float[] point, View child)
    {
        point[0] += getScrollX() - child.getLeft();
        point[1] += getScrollY() - child.getTop();

        if (!child.hasIdentityMatrix())
        {
            android.graphics.Matrix inverse = new android.graphics.Matrix();
            if (child.getMatrix().invert(inverse)) inverse.mapPoints(point);
        }
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).jumpDrawablesToCurrentState();
        }
    }

    protected void setChildrenDrawingCacheEnabled(boolean enabled)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).setDrawingCacheEnabled(enabled);
        }
    }

    protected void setChildrenDrawnWithCacheEnabled(boolean enabled)
    {
    }

    @Override
    ViewOverlay createOverlay()
    {
        return new ViewGroupOverlay(getContext(), this);
    }

    @Override
    public ViewGroupOverlay getOverlay()
    {
        return (ViewGroupOverlay)super.getOverlay();
    }

    @Override
    public ViewParent invalidateChildInParent(int[] location, android.graphics.Rect dirty)
    {
        invalidate();
        return getParent();
    }

    @Override
    public boolean getChildVisibleRect(View child, android.graphics.Rect rect, android.graphics.Point offset)
    {
        android.graphics.RectF bounds = new android.graphics.RectF(rect);

        if (!child.hasIdentityMatrix()) child.getMatrix().mapRect(bounds);

        int dx = child.getLeft() - getScrollX();
        int dy = child.getTop() - getScrollY();
        bounds.offset(dx, dy);

        if (offset != null)
        {
            if (!child.hasIdentityMatrix())
            {
                float[] position = {offset.x, offset.y};
                child.getMatrix().mapPoints(position);
                offset.x = Math.round(position[0]);
                offset.y = Math.round(position[1]);
            }

            offset.x += dx;
            offset.y += dy;
        }

        int width = getRight() - getLeft();
        int height = getBottom() - getTop();
        boolean rectIsVisible = true;

        if (!(getParent() instanceof ViewGroup) || ((ViewGroup)getParent()).getClipChildren())
        {
            rectIsVisible = bounds.intersect(0, 0, width, height);
        }

        if (rectIsVisible && mClipToPadding)
        {
            rectIsVisible = bounds.intersect(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
        }

        rect.set((int)Math.floor(bounds.left), (int)Math.floor(bounds.top), (int)Math.ceil(bounds.right), (int)Math.ceil(bounds.bottom));

        if (rectIsVisible && getParent() instanceof ViewGroup) rectIsVisible = getParent().getChildVisibleRect(this, rect, offset);

        return rectIsVisible;
    }

    public final void offsetDescendantRectToMyCoords(View descendant, android.graphics.Rect rect)
    {
        offsetRectBetweenParentAndChild(descendant, rect, true, false);
    }

    public final void offsetRectIntoDescendantCoords(View descendant, android.graphics.Rect rect)
    {
        offsetRectBetweenParentAndChild(descendant, rect, false, false);
    }

    void offsetRectBetweenParentAndChild(View descendant, android.graphics.Rect rect, boolean offsetFromChildToParent, boolean clipToBounds)
    {
        if (descendant == this) return;

        ViewParent theParent = descendant.getParent();

        while (theParent instanceof View && theParent != this)
        {
            if (offsetFromChildToParent)
            {
                rect.offset(descendant.getLeft() - descendant.getScrollX(), descendant.getTop() - descendant.getScrollY());
                if (clipToBounds)
                {
                    View parentView = (View)theParent;
                    boolean intersected = rect.intersect(0, 0, parentView.getRight() - parentView.getLeft(),
                                                         parentView.getBottom() - parentView.getTop());
                    if (!intersected) rect.setEmpty();
                }
            }
            else
            {
                if (clipToBounds)
                {
                    View parentView = (View)theParent;
                    boolean intersected = rect.intersect(0, 0, parentView.getRight() - parentView.getLeft(),
                                                         parentView.getBottom() - parentView.getTop());
                    if (!intersected) rect.setEmpty();
                }
                rect.offset(descendant.getScrollX() - descendant.getLeft(), descendant.getScrollY() - descendant.getTop());
            }

            descendant = (View)theParent;
            theParent = descendant.getParent();
        }

        if (theParent != this) throw new IllegalArgumentException("parameter must be a descendant of this view");

        if (offsetFromChildToParent)
        {
            rect.offset(descendant.getLeft() - descendant.getScrollX(), descendant.getTop() - descendant.getScrollY());
        }
        else
        {
            rect.offset(descendant.getScrollX() - descendant.getLeft(), descendant.getScrollY() - descendant.getTop());
        }
    }

    public void offsetChildrenTopAndBottom(int offset)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).offsetTopAndBottom(offset);
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, android.graphics.Rect rectangle, boolean immediate)
    {
        return false;
    }

    @Override
    public boolean requestSendAccessibilityEvent(View child, android.view.accessibility.AccessibilityEvent event)
    {
        ViewParent parent = getParent();
        if (parent == null) return false;

        boolean propagate = onRequestSendAccessibilityEvent(child, event);
        if (!propagate) return false;

        return parent.requestSendAccessibilityEvent(this, event);
    }

    public boolean onRequestSendAccessibilityEvent(View child, android.view.accessibility.AccessibilityEvent event)
    {
        AccessibilityDelegate delegate = getAccessibilityDelegate();
        if (delegate != null) return delegate.onRequestSendAccessibilityEvent(this, child, event);

        return onRequestSendAccessibilityEventInternal(child, event);
    }

    boolean onRequestSendAccessibilityEventInternal(View child, android.view.accessibility.AccessibilityEvent event)
    {
        return true;
    }

    @Override
    public void childHasTransientStateChanged(View child, boolean childHasTransientState)
    {
        boolean oldHasTransientState = hasTransientState();

        if (childHasTransientState) mChildCountWithTransientState++;
        else mChildCountWithTransientState = Math.max(0, mChildCountWithTransientState - 1);

        boolean newHasTransientState = hasTransientState();
        ViewParent parent = getParent();

        if (parent == null || oldHasTransientState == newHasTransientState) return;

        parent.childHasTransientStateChanged(this, newHasTransientState);
    }

    @Override
    public boolean hasTransientState()
    {
        return mChildCountWithTransientState > 0 || super.hasTransientState();
    }

    @Override
    public void recomputeViewAttributes(View child)
    {
        ViewParent parent = getParent();
        if (parent != null) parent.recomputeViewAttributes(this);
    }

    @Override
    public void requestTransparentRegion(View child)
    {
        ViewParent parent = getParent();
        if (parent != null) parent.requestTransparentRegion(this);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event)
    {
        if (isFocused()) return super.dispatchKeyEventPreIme(event);
        if (mFocusedChild != null) return mFocusedChild.dispatchKeyEventPreIme(event);

        return false;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event)
    {
        if (isFocused()) return super.dispatchKeyShortcutEvent(event);
        if (mFocusedChild != null) return mFocusedChild.dispatchKeyShortcutEvent(event);

        return false;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        for (int index = mChildren.size() - 1; index >= 0; index--)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() != VISIBLE) continue;

            float localX = x + getScrollX() - child.getLeft();
            float localY = y + getScrollY() - child.getTop();
            if (!child.containsLocal(localX, localY)) continue;

            event.offsetLocation(localX - x, localY - y);

            try
            {
                PointerIcon icon = child.onResolvePointerIcon(event, pointerIndex);
                if (icon != null) return icon;
            }
            finally
            {
                event.offsetLocation(x - localX, y - localY);
            }

            break;
        }

        return super.onResolvePointerIcon(event, pointerIndex);
    }

    public boolean onInterceptHoverEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public void addChildrenForAccessibility(ArrayList<View> outChildren)
    {
        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() != VISIBLE) continue;

            if (child.includeForAccessibility()) outChildren.add(child);
            else child.addChildrenForAccessibility(outChildren);
        }
    }

    @Override
    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType)
    {
        ViewParent parent = getParent();
        if (parent != null) parent.notifySubtreeAccessibilityStateChanged(this, source, changeType);
    }

    @Override
    public boolean onNestedPrePerformAccessibilityAction(View target, int action, android.os.Bundle arguments)
    {
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ViewGroup.class.getName();
    }

    @Override
    public void dispatchProvideStructure(ViewStructure structure)
    {
        super.dispatchProvideStructure(structure);

        if (structure.getChildCount() != 0) return;

        int count = mChildren.size();
        if (count <= 0) return;

        structure.setChildCount(count);
        for (int index = 0; index < count; index++)
        {
            ViewStructure childStructure = structure.newChild(index);
            mChildren.get(index).dispatchProvideStructure(childStructure);
        }
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags)
    {
        super.dispatchProvideAutofillStructure(structure, flags);

        if (structure.getChildCount() != 0) return;

        int count = mChildren.size();
        if (count <= 0) return;

        structure.setChildCount(count);
        for (int index = 0; index < count; index++)
        {
            ViewStructure childStructure = structure.newChild(index);
            mChildren.get(index).dispatchProvideAutofillStructure(childStructure, flags);
        }
    }

    @Override
    protected void dispatchSaveInstanceState(android.util.SparseArray<android.os.Parcelable> container)
    {
        super.dispatchSaveInstanceState(container);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.isSaveFromParentEnabled()) child.dispatchSaveInstanceState(container);
        }
    }

    protected void dispatchFreezeSelfOnly(android.util.SparseArray<android.os.Parcelable> container)
    {
        super.dispatchSaveInstanceState(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> container)
    {
        super.dispatchRestoreInstanceState(container);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.isSaveFromParentEnabled()) child.dispatchRestoreInstanceState(container);
        }
    }

    protected void dispatchThawSelfOnly(android.util.SparseArray<android.os.Parcelable> container)
    {
        super.dispatchRestoreInstanceState(container);
    }

    @Override
    protected void dispatchVisibilityChanged(View changedView, int visibility)
    {
        super.dispatchVisibilityChanged(changedView, visibility);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchVisibilityChanged(changedView, visibility);
        }
    }

    @Override
    boolean dispatchVisibilityAggregated(boolean isVisible)
    {
        isVisible = super.dispatchVisibilityAggregated(isVisible);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.dispatchVisibilityAggregated(isVisible);
        }

        return isVisible;
    }

    void onChildVisibilityChanged(View child, int oldVisibility, int newVisibility)
    {
        if (mLayoutTransition != null)
        {
            if (newVisibility == VISIBLE) mLayoutTransition.showChild(this, child, oldVisibility);
            else mLayoutTransition.hideChild(this, child, newVisibility);
        }

        if (newVisibility != VISIBLE && child.getAnimation() != null && !mDisappearingChildren.contains(child)
            && mChildren.contains(child))
        {
            invalidate();
        }
    }

    @Override
    void dispatchDragEventToTree(DragEvent event)
    {
        super.dispatchDragEventToTree(event);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.dispatchDragEventToTree(event);
        }
    }

    @Override
    public void dispatchConfigurationChanged(android.content.res.Configuration newConfig)
    {
        super.dispatchConfigurationChanged(newConfig);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchConfigurationChanged(newConfig);
        }
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus)
    {
        super.dispatchWindowFocusChanged(hasFocus);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void dispatchWindowVisibilityChanged(int visibility)
    {
        super.dispatchWindowVisibilityChanged(visibility);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchWindowVisibilityChanged(visibility);
        }
    }

    @Override
    public void dispatchDisplayHint(int hint)
    {
        super.dispatchDisplayHint(hint);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchDisplayHint(hint);
        }
    }

    @Override
    public void dispatchSystemUiVisibilityChanged(int visibility)
    {
        super.dispatchSystemUiVisibilityChanged(visibility);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchSystemUiVisibilityChanged(visibility);
        }
    }

    @Override
    public void dispatchWindowSystemUiVisiblityChanged(int visible)
    {
        super.dispatchWindowSystemUiVisiblityChanged(visible);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchWindowSystemUiVisiblityChanged(visible);
        }
    }

    @Override
    public void dispatchStartTemporaryDetach()
    {
        super.dispatchStartTemporaryDetach();

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchStartTemporaryDetach();
        }
    }

    @Override
    public void dispatchFinishTemporaryDetach()
    {
        super.dispatchFinishTemporaryDetach();

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchFinishTemporaryDetach();
        }
    }

    @Override
    public void dispatchPointerCaptureChanged(boolean hasCapture)
    {
        super.dispatchPointerCaptureChanged(hasCapture);

        for (int index = 0; index < mChildren.size(); index++)
        {
            mChildren.get(index).dispatchPointerCaptureChanged(hasCapture);
        }
    }

    @Override
    View findViewWithTagTraversal(Object tag)
    {
        View self = super.findViewWithTagTraversal(tag);
        if (self != null) return self;

        for (int index = 0; index < mChildren.size(); index++)
        {
            View found = mChildren.get(index).findViewWithTagTraversal(tag);
            if (found != null) return found;
        }

        return null;
    }

    @Override
    public void findViewsWithText(ArrayList<View> outViews, CharSequence text, int flags)
    {
        super.findViewsWithText(outViews, text, flags);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.findViewsWithText(outViews, text, flags);
        }
    }

    @Override
    public void addTouchables(ArrayList<View> views)
    {
        super.addTouchables(views);

        for (int index = 0; index < mChildren.size(); index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() == VISIBLE) child.addTouchables(views);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes)
    {
        mNestedScrollAxes = axes;
    }

    @Override
    public void onStopNestedScroll(View child)
    {
        stopNestedScroll();
        mNestedScrollAxes = 0;
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed)
    {
        dispatchNestedPreScroll(dx, dy, consumed, null);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY)
    {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    public int getNestedScrollAxes()
    {
        return mNestedScrollAxes;
    }

    public void setLayoutAnimation(android.view.animation.LayoutAnimationController controller)
    {
        mLayoutAnimationController = controller;
        if (controller != null) mLayoutAnimationPending = true;
    }

    public android.view.animation.LayoutAnimationController getLayoutAnimation()
    {
        return mLayoutAnimationController;
    }

    protected static final int CLIP_TO_PADDING_MASK = 0x22;

    private boolean mStaticTransformationsEnabled;

    protected boolean canAnimate()
    {
        return mLayoutAnimationController != null;
    }

    protected void setStaticTransformationsEnabled(boolean enabled)
    {
        mStaticTransformationsEnabled = enabled;
    }

    protected boolean getChildStaticTransformation(View child, android.view.animation.Transformation transformation)
    {
        return false;
    }

    @Deprecated
    protected boolean isChildrenDrawnWithCacheEnabled()
    {
        return false;
    }

    protected void debug(int depth)
    {
        StringBuilder indent = new StringBuilder();
        for (int level = 0; level < depth; level++) indent.append("  ");

        android.util.Log.d("View", indent + getClass().getName() + " {" + getLeft() + ", " + getTop() + ", " + getRight() + ", "
                                       + getBottom() + "}");

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child instanceof ViewGroup) ((ViewGroup)child).debug(depth + 1);
            else
            {
                android.util.Log.d("View", indent + "  " + child.getClass().getName() + " {" + child.getLeft() + ", " + child.getTop()
                                               + ", " + child.getRight() + ", " + child.getBottom() + "}");
            }
        }
    }

    public void propagateRequestedFrameRate(float frameRate, boolean forceOverride)
    {
        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child instanceof ViewGroup) ((ViewGroup)child).propagateRequestedFrameRate(frameRate, forceOverride);
            else if (forceOverride || Float.isNaN(child.getRequestedFrameRate())) child.setRequestedFrameRate(frameRate);
        }
    }

    private boolean continuesInsetsAnimation()
    {
        WindowInsetsAnimation.Callback callback = getWindowInsetsAnimationCallback();
        return callback == null || callback.getDispatchMode() == WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE;
    }

    @Override
    public void dispatchWindowInsetsAnimationPrepare(WindowInsetsAnimation animation)
    {
        super.dispatchWindowInsetsAnimationPrepare(animation);
        if (!continuesInsetsAnimation()) return;

        for (int index = 0; index < getChildCount(); index++)
        {
            getChildAt(index).dispatchWindowInsetsAnimationPrepare(animation);
        }
    }

    @Override
    public WindowInsetsAnimation.Bounds dispatchWindowInsetsAnimationStart(WindowInsetsAnimation animation,
                                                                          WindowInsetsAnimation.Bounds bounds)
    {
        WindowInsetsAnimation.Bounds result = super.dispatchWindowInsetsAnimationStart(animation, bounds);
        if (!continuesInsetsAnimation()) return result;

        for (int index = 0; index < getChildCount(); index++)
        {
            getChildAt(index).dispatchWindowInsetsAnimationStart(animation, result);
        }

        return result;
    }

    @Override
    public WindowInsets dispatchWindowInsetsAnimationProgress(WindowInsets insets, java.util.List<WindowInsetsAnimation> runningAnimations)
    {
        WindowInsets result = super.dispatchWindowInsetsAnimationProgress(insets, runningAnimations);
        if (!continuesInsetsAnimation()) return result;

        for (int index = 0; index < getChildCount(); index++)
        {
            getChildAt(index).dispatchWindowInsetsAnimationProgress(result, runningAnimations);
        }

        return result;
    }

    @Override
    public void dispatchWindowInsetsAnimationEnd(WindowInsetsAnimation animation)
    {
        super.dispatchWindowInsetsAnimationEnd(animation);
        if (!continuesInsetsAnimation()) return;

        for (int index = 0; index < getChildCount(); index++)
        {
            getChildAt(index).dispatchWindowInsetsAnimationEnd(animation);
        }
    }

    public void scheduleLayoutAnimation()
    {
        mLayoutAnimationPending = mLayoutAnimationController != null;
    }

    public void startLayoutAnimation()
    {
        if (mLayoutAnimationController == null) return;

        mLayoutAnimationPending = true;
        requestLayout();
    }

    public void setLayoutAnimationListener(android.view.animation.Animation.AnimationListener animationListener)
    {
        mAnimationListener = animationListener;
    }

    public android.view.animation.Animation.AnimationListener getLayoutAnimationListener()
    {
        return mAnimationListener;
    }

    protected boolean isAnimationCacheEnabledForChildren()
    {
        return mAnimationCacheEnabled;
    }

    protected void attachLayoutAnimationParameters(View child, LayoutParams params, int index, int count)
    {
        android.view.animation.LayoutAnimationController.AnimationParameters animationParameters = params.layoutAnimationParameters;
        if (animationParameters == null)
        {
            animationParameters = new android.view.animation.LayoutAnimationController.AnimationParameters();
            params.layoutAnimationParameters = animationParameters;
        }

        animationParameters.count = count;
        animationParameters.index = index;
    }

    private void startLayoutAnimationOnChildren()
    {
        mLayoutAnimationPending = false;

        int count = mChildren.size();
        for (int index = 0; index < count; index++)
        {
            View child = mChildren.get(index);
            if (child.getVisibility() != VISIBLE) continue;

            LayoutParams params = child.getLayoutParams();
            if (params == null) continue;

            attachLayoutAnimationParameters(child, params, index, count);
            child.setAnimation(mLayoutAnimationController.getAnimationForView(child));
        }

        mLayoutAnimationController.start();
        mLayoutAnimationRunning = true;

        if (mAnimationListener != null) mAnimationListener.onAnimationStart(mLayoutAnimationController.getAnimation());
    }

    private void finishLayoutAnimation()
    {
        mLayoutAnimationRunning = false;

        if (mAnimationListener != null) mAnimationListener.onAnimationEnd(mLayoutAnimationController.getAnimation());
    }

    public static class LayoutParams
    {
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;

        @Deprecated
        public static final int FILL_PARENT = -1;

        private static StyleableSet sStyleable;

        public int width;
        public int height;
        public android.view.animation.LayoutAnimationController.AnimationParameters layoutAnimationParameters;

        protected void setBaseAttributes(TypedArray array, int widthAttr, int heightAttr)
        {
            width = array.getLayoutDimension(widthAttr, "layout_width");
            height = array.getLayoutDimension(heightAttr, "layout_height");
        }

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

package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ScrollView extends FrameLayout
{
    private static final float MAX_SCROLL_FACTOR = 0.5f;

    private static StyleableSet sStyleable;

    private boolean mFillViewport;
    private boolean mSmoothScrollingEnabled = true;

    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;
    private VelocityTracker mVelocityTracker;

    private boolean mDragging;
    private float mLastMotionY;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mVerticalScrollFactor;
    private int mSavedScrollPosition = -1;
    private int mNestedYOffset;

    public ScrollView(Context context)
    {
        this(context, null);
    }

    public ScrollView(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "scrollViewStyle"));
    }

    public ScrollView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ScrollView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mScroller = new OverScroller(context);
        mEdgeGlowTop = new EdgeEffect(context);
        mEdgeGlowBottom = new EdgeEffect(context);

        ViewConfiguration configuration = ViewConfiguration.get(context);

        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mVerticalScrollFactor = configuration.getScaledVerticalScrollFactor();

        setFocusable(true);
        setWillNotDraw(false);

        if (attributes == null) return;

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "fillViewport");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);
        mFillViewport = array.getBoolean(0, false);
        array.recycle();
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return true;
    }

    @Override
    protected float getTopFadingEdgeStrength()
    {
        if (getChildCount() == 0) return 0f;

        int length = getVerticalFadingEdgeLength();
        if (length <= 0) return 0f;

        return getScrollY() < length ? getScrollY() / (float)length : 1f;
    }

    @Override
    protected float getBottomFadingEdgeStrength()
    {
        if (getChildCount() == 0) return 0f;

        int length = getVerticalFadingEdgeLength();
        if (length <= 0) return 0f;

        int span = getChildAt(0).getBottom() - getScrollY() - (getHeight() - getPaddingBottom());
        return span < length ? span / (float)length : 1f;
    }

    public void setEdgeEffectColor(int color)
    {
        setTopEdgeEffectColor(color);
        setBottomEdgeEffectColor(color);
    }

    public void setBottomEdgeEffectColor(int color)
    {
        mEdgeGlowBottom.setColor(color);
    }

    public void setTopEdgeEffectColor(int color)
    {
        mEdgeGlowTop.setColor(color);
    }

    public int getTopEdgeEffectColor()
    {
        return mEdgeGlowTop.getColor();
    }

    public int getBottomEdgeEffectColor()
    {
        return mEdgeGlowBottom.getColor();
    }

    public int getMaxScrollAmount()
    {
        return (int)(MAX_SCROLL_FACTOR * (getBottom() - getTop()));
    }

    private void ensureSingleChild()
    {
        if (getChildCount() > 0) throw new IllegalStateException("ScrollView can host only one direct child");
    }

    @Override
    public void addView(View child)
    {
        ensureSingleChild();
        super.addView(child);
    }

    @Override
    public void addView(View child, int index)
    {
        ensureSingleChild();
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params)
    {
        ensureSingleChild();
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        ensureSingleChild();
        super.addView(child, index, params);
    }

    private int getScrollRange()
    {
        if (getChildCount() == 0) return 0;

        View child = getChildAt(0);
        int available = getHeight() - getPaddingTop() - getPaddingBottom();

        return Math.max(0, child.getHeight() - available);
    }

    public boolean isFillViewport()
    {
        return mFillViewport;
    }

    public void setFillViewport(boolean fillViewport)
    {
        if (mFillViewport == fillViewport) return;

        mFillViewport = fillViewport;
        requestLayout();
    }

    public boolean isSmoothScrollingEnabled()
    {
        return mSmoothScrollingEnabled;
    }

    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled)
    {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) return;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) return;
        if (getChildCount() == 0) return;

        View child = getChildAt(0);
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        ViewGroup.LayoutParams params = child.getLayoutParams();
        int horizontalMargins = 0;
        int verticalMargins = 0;

        if (params instanceof MarginLayoutParams)
        {
            MarginLayoutParams margins = (MarginLayoutParams)params;
            horizontalMargins = margins.leftMargin + margins.rightMargin;
            verticalMargins = margins.topMargin + margins.bottomMargin;
        }

        height -= verticalMargins;
        if (child.getMeasuredHeight() >= height) return;

        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + horizontalMargins,
                                                 params == null ? LayoutParams.MATCH_PARENT : params.width);

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(KeyEvent event)
    {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (getScrollRange() == 0) return false;

        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return event.isAltPressed() ? fullScroll(FOCUS_UP) : arrowScroll(FOCUS_UP);
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return event.isAltPressed() ? fullScroll(FOCUS_DOWN) : arrowScroll(FOCUS_DOWN);
        if (keyCode == KeyEvent.KEYCODE_SPACE) return pageScroll(event.isShiftPressed() ? FOCUS_UP : FOCUS_DOWN);
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) return pageScroll(FOCUS_UP);
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) return pageScroll(FOCUS_DOWN);
        if (keyCode == KeyEvent.KEYCODE_MOVE_HOME) return fullScroll(FOCUS_UP);
        if (keyCode == KeyEvent.KEYCODE_MOVE_END) return fullScroll(FOCUS_DOWN);

        return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        if (disallowIntercept) recycleVelocityTracker();

        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_MOVE && mDragging) return true;

        if (action == MotionEvent.ACTION_DOWN)
        {
            mLastMotionY = event.getY();
            mDragging = !mScroller.isFinished();

            startNestedScroll(SCROLL_AXIS_VERTICAL);

            return mDragging;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            if (Math.abs(event.getY() - mLastMotionY) > mTouchSlop && (getNestedScrollAxes() & SCROLL_AXIS_VERTICAL) == 0)
            {
                mDragging = true;
                mLastMotionY = event.getY();
                mNestedYOffset = 0;

                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);

                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
        {
            mDragging = false;

            springBack();
            stopNestedScroll();
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            if (getChildCount() == 0) return false;

            mLastMotionY = event.getY();
            mNestedYOffset = 0;

            if (!mScroller.isFinished()) mScroller.abortAnimation();

            startNestedScroll(SCROLL_AXIS_VERTICAL);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            float y = event.getY();
            int delta = (int)(mLastMotionY - y);

            if (!mDragging && Math.abs(delta) > mTouchSlop)
            {
                mDragging = true;
                delta = delta > 0 ? delta - mTouchSlop : delta + mTouchSlop;

                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
            }

            if (!mDragging) return true;

            int[] consumed = new int[2];
            int[] offset = new int[2];

            if (dispatchNestedPreScroll(0, delta, consumed, offset))
            {
                delta -= consumed[1];
                mNestedYOffset += offset[1];
            }

            mLastMotionY = y - offset[1];

            int oldY = getScrollY();
            int range = getScrollRange();
            int requested = oldY + delta;
            int target = Math.max(0, Math.min(range, requested));

            if (target != oldY) scrollTo(getScrollX(), target);

            int scrolled = getScrollY() - oldY;
            int unconsumed = delta - scrolled;

            if (dispatchNestedScroll(0, scrolled, 0, unconsumed, offset))
            {
                mLastMotionY -= offset[1];
                mNestedYOffset += offset[1];
                return true;
            }

            int overScrollMode = getOverScrollMode();
            boolean canOverScroll = overScrollMode == OVER_SCROLL_ALWAYS || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

            if (canOverScroll && getHeight() > 0)
            {
                float horizontal = event.getX() / Math.max(1, getWidth());

                if (requested < 0)
                {
                    mEdgeGlowTop.onPull((float)(-requested) / getHeight(), horizontal);
                    if (!mEdgeGlowBottom.isFinished()) mEdgeGlowBottom.onRelease();
                }
                else if (requested > range)
                {
                    mEdgeGlowBottom.onPull((float)(requested - range) / getHeight(), 1f - horizontal);
                    if (!mEdgeGlowTop.isFinished()) mEdgeGlowTop.onRelease();
                }

                if (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()) invalidate();
            }

            return true;
        }

        if (action == MotionEvent.ACTION_UP)
        {
            if (mDragging)
            {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                int velocity = (int)mVelocityTracker.getYVelocity();

                if (Math.abs(velocity) > mMinimumVelocity) flingWithNestedDispatch(-velocity);
                else springBack();
            }

            endDrag();
            return true;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            if (mDragging) springBack();

            endDrag();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void flingWithNestedDispatch(int velocityY)
    {
        int scrollY = getScrollY();
        boolean canFling = (scrollY > 0 || velocityY > 0) && (scrollY < getScrollRange() || velocityY < 0);

        if (dispatchNestedPreFling(0, velocityY)) return;

        dispatchNestedFling(0, velocityY, canFling);
        if (canFling) fling(velocityY);
    }

    private void springBack()
    {
        mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange());

        if (!mScroller.isFinished()) invalidate();
    }

    private void endDrag()
    {
        mDragging = false;

        recycleVelocityTracker();
        stopNestedScroll();

        mEdgeGlowTop.onRelease();
        mEdgeGlowBottom.onRelease();
    }

    private void recycleVelocityTracker()
    {
        if (mVelocityTracker == null) return;

        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER))
        {
            float axis = event.getAxisValue(MotionEvent.AXIS_VSCROLL);

            if (axis != 0)
            {
                int delta = Math.round(axis * mVerticalScrollFactor);
                int range = getScrollRange();
                int target = Math.max(0, Math.min(range, getScrollY() - delta));

                if (target != getScrollY())
                {
                    super.scrollTo(getScrollX(), target);
                    return true;
                }
            }
        }

        return super.onGenericMotionEvent(event);
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
        if (!mScroller.isFinished())
        {
            super.scrollTo(scrollX, scrollY);

            if (clampedY) mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, getScrollRange());
            return;
        }

        super.scrollTo(scrollX, scrollY);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ScrollView.class.getName();
    }

    public boolean pageScroll(int direction)
    {
        int height = getHeight();
        int current = getScrollY();
        int target = direction == FOCUS_DOWN ? current + height : current - height;

        target = Math.max(0, Math.min(getScrollRange(), target));
        if (target == current) return false;

        smoothScrollTo(getScrollX(), target);
        return true;
    }

    public boolean fullScroll(int direction)
    {
        int current = getScrollY();
        int target = direction == FOCUS_DOWN ? getScrollRange() : 0;
        if (target == current) return false;

        smoothScrollTo(getScrollX(), target);
        return true;
    }

    public boolean arrowScroll(int direction)
    {
        int amount = getMaxScrollAmount();
        int current = getScrollY();
        int target = direction == FOCUS_DOWN ? current + amount : current - amount;

        target = Math.max(0, Math.min(getScrollRange(), target));
        if (target == current) return false;

        if (mSmoothScrollingEnabled) smoothScrollBy(0, target - current);
        else scrollTo(getScrollX(), target);

        return true;
    }

    public final void smoothScrollBy(int dx, int dy)
    {
        if (getChildCount() == 0) return;

        int target = Math.max(0, Math.min(getScrollRange(), getScrollY() + dy));

        if (!mSmoothScrollingEnabled)
        {
            if (!mScroller.isFinished()) mScroller.abortAnimation();

            scrollTo(getScrollX(), target);
            return;
        }

        mScroller.startScroll(getScrollX(), getScrollY(), 0, target - getScrollY());
        invalidate();
    }

    public final void smoothScrollTo(int x, int y)
    {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if (getChildCount() == 0) return contentHeight;

        int scrollRange = getChildAt(0).getBottom();
        int scrollY = getScrollY();
        int overscrollBottom = Math.max(0, scrollRange - contentHeight);

        if (scrollY < 0) scrollRange -= scrollY;
        else if (scrollY > overscrollBottom) scrollRange += scrollY - overscrollBottom;

        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        return Math.max(0, getScrollY());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) params = generateDefaultLayoutParams();

        int childWidthSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), params.width);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(
            Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - getPaddingTop() - getPaddingBottom()), MeasureSpec.UNSPECIFIED);

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec,
                                           int heightUsed)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) params = generateDefaultLayoutParams();

        int horizontalMargins = 0;
        int verticalMargins = 0;

        if (params instanceof MarginLayoutParams)
        {
            MarginLayoutParams margins = (MarginLayoutParams)params;
            horizontalMargins = margins.leftMargin + margins.rightMargin;
            verticalMargins = margins.topMargin + margins.bottomMargin;
        }

        int horizontalUsed = getPaddingLeft() + getPaddingRight() + horizontalMargins + widthUsed;
        int childWidthSpec = getChildMeasureSpec(parentWidthMeasureSpec, horizontalUsed, params.width);
        int usedTotal = getPaddingTop() + getPaddingBottom() + verticalMargins + heightUsed;
        int childHeightSpec = MeasureSpec.makeMeasureSpec(Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal),
                                                          MeasureSpec.UNSPECIFIED);

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void computeScroll()
    {
        if (mScroller == null || !mScroller.computeScrollOffset()) return;

        int range = getScrollRange();
        int y = mScroller.getCurrY();
        int clamped = Math.max(0, Math.min(range, y));

        if (y != clamped && !mScroller.isFinished())
        {
            if (y < 0 && mEdgeGlowTop.isFinished()) mEdgeGlowTop.onAbsorb((int)mScroller.getCurrVelocity());
            else if (y > range && mEdgeGlowBottom.isFinished()) mEdgeGlowBottom.onAbsorb((int)mScroller.getCurrVelocity());
        }

        if (clamped != getScrollY()) onOverScrolled(getScrollX(), clamped, false, y != clamped);

        invalidate();
    }

    public void scrollToDescendant(View child)
    {
        if (child == null || isInLayout()) return;

        Rect rect = new Rect(0, 0, child.getWidth(), child.getHeight());
        rectInMyCoordinates(child, rect);
        rect.offset(getScrollX(), getScrollY());

        int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        if (delta != 0) scrollBy(0, delta);
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect)
    {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        int fadingEdge = getVerticalFadingEdgeLength();
        View child = getChildAt(0);

        if (rect.top > 0) screenTop += fadingEdge;
        if (rect.bottom < child.getHeight()) screenBottom -= fadingEdge;

        int delta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop)
        {
            if (rect.height() > height) delta += rect.top - screenTop;
            else delta += rect.bottom - screenBottom;

            delta = Math.min(delta, child.getBottom() - screenBottom);
        }
        else if (rect.top < screenTop && rect.bottom < screenBottom)
        {
            if (rect.height() > height) delta -= screenBottom - rect.bottom;
            else delta -= screenTop - rect.top;

            delta = Math.max(delta, -getScrollY());
        }

        return delta;
    }

    private boolean scrollToChildRect(Rect rect, boolean immediate)
    {
        int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        if (delta == 0) return false;

        if (immediate) scrollBy(0, delta);
        else smoothScrollBy(0, delta);

        return true;
    }

    private void rectInMyCoordinates(View descendant, Rect rect)
    {
        View current = descendant;

        while (current != null && current != this)
        {
            rect.offset(current.getLeft() - current.getScrollX(), current.getTop() - current.getScrollY());

            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View)parent : null;
        }
    }

    @Override
    public void requestChildFocus(View child, View focused)
    {
        if (focused != null && !isInLayout())
        {
            Rect rect = new Rect(0, 0, focused.getWidth(), focused.getHeight());
            rectInMyCoordinates(focused, rect);
            rect.offset(getScrollX(), getScrollY());
            scrollToChildRect(rect, false);
        }

        super.requestChildFocus(child, focused);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect)
    {
        if (getChildCount() == 0) return false;

        return getChildAt(0).requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate)
    {
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout()
    {
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if (mSavedScrollPosition >= 0)
        {
            int position = mSavedScrollPosition;
            mSavedScrollPosition = -1;
            super.scrollTo(getScrollX(), Math.max(0, Math.min(getScrollRange(), position)));
            return;
        }

        int clamped = Math.max(0, Math.min(getScrollRange(), getScrollY()));
        if (clamped != getScrollY()) super.scrollTo(getScrollX(), clamped);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if (!mScroller.isFinished()) mScroller.abortAnimation();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        int clamped = Math.max(0, Math.min(getScrollRange(), getScrollY()));
        if (clamped != getScrollY()) super.scrollTo(getScrollX(), clamped);
    }

    public void fling(int velocityY)
    {
        if (getChildCount() == 0) return;

        int height = getHeight() - getPaddingBottom() - getPaddingTop();
        int bottom = getChildAt(0).getHeight();

        mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, Math.max(0, bottom - height), 0, height / 2);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y)
    {
        if (getChildCount() > 0)
        {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
        }

        if (x != getScrollX() || y != getScrollY()) super.scrollTo(x, y);
    }

    private static int clamp(int position, int viewport, int content)
    {
        if (viewport >= content || position < 0) return 0;
        if (viewport + position > content) return content - viewport;

        return position;
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return (nestedScrollAxes & SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes)
    {
        super.onNestedScrollAccepted(child, target, axes);

        startNestedScroll(SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View target)
    {
        super.onStopNestedScroll(target);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    {
        int oldScrollY = getScrollY();
        scrollBy(0, dyUnconsumed);

        int consumed = getScrollY() - oldScrollY;
        dispatchNestedScroll(0, consumed, 0, dyUnconsumed - consumed, null);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        if (consumed) return false;

        flingWithNestedDispatch((int)velocityY);
        return true;
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        int scrollY = getScrollY();

        if (!mEdgeGlowTop.isFinished())
        {
            int saveCount = canvas.save();
            int width = getWidth() - getPaddingLeft() - getPaddingRight();

            canvas.translate(getPaddingLeft(), Math.min(0, scrollY));
            mEdgeGlowTop.setSize(width, getHeight());
            if (mEdgeGlowTop.draw(canvas)) invalidate();
            canvas.restoreToCount(saveCount);
        }

        if (!mEdgeGlowBottom.isFinished())
        {
            int saveCount = canvas.save();
            int width = getWidth() - getPaddingLeft() - getPaddingRight();
            int height = getHeight();

            canvas.translate(getPaddingLeft() - width, Math.max(getScrollRange(), scrollY) + height);
            canvas.rotate(180, width, 0);
            mEdgeGlowBottom.setSize(width, height);
            if (mEdgeGlowBottom.draw(canvas)) invalidate();
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (!(state instanceof SavedState))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState saved = (SavedState)state;
        super.onRestoreInstanceState(saved.getSuperState());

        mSavedScrollPosition = saved.mScrollPosition;
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mScrollPosition = getScrollY();
        return state;
    }

    static class SavedState extends BaseSavedState
    {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel source)
            {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };

        int mScrollPosition;

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            mScrollPosition = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(mScrollPosition);
        }
    }
}

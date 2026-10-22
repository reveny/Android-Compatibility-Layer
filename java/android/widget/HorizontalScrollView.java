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

public class HorizontalScrollView extends FrameLayout
{
    private static final float MAX_SCROLL_FACTOR = 0.5f;

    private static StyleableSet sStyleable;

    private boolean mFillViewport;
    private boolean mSmoothScrollingEnabled = true;

    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowLeft;
    private EdgeEffect mEdgeGlowRight;
    private VelocityTracker mVelocityTracker;

    private boolean mDragging;
    private float mLastMotionX;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private float mHorizontalScrollFactor;
    private int mSavedScrollPosition = -1;

    public HorizontalScrollView(Context context)
    {
        this(context, null);
    }

    public HorizontalScrollView(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "horizontalScrollViewStyle"));
    }

    public HorizontalScrollView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public HorizontalScrollView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mScroller = new OverScroller(context);
        mEdgeGlowLeft = new EdgeEffect(context);
        mEdgeGlowRight = new EdgeEffect(context);

        ViewConfiguration configuration = ViewConfiguration.get(context);

        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mHorizontalScrollFactor = configuration.getScaledHorizontalScrollFactor();

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
    protected float getLeftFadingEdgeStrength()
    {
        if (getChildCount() == 0) return 0f;

        int length = getHorizontalFadingEdgeLength();
        if (length <= 0) return 0f;

        return getScrollX() < length ? getScrollX() / (float)length : 1f;
    }

    @Override
    protected float getRightFadingEdgeStrength()
    {
        if (getChildCount() == 0) return 0f;

        int length = getHorizontalFadingEdgeLength();
        if (length <= 0) return 0f;

        int span = getChildAt(0).getRight() - getScrollX() - (getWidth() - getPaddingRight());
        return span < length ? span / (float)length : 1f;
    }

    public void setEdgeEffectColor(int color)
    {
        setLeftEdgeEffectColor(color);
        setRightEdgeEffectColor(color);
    }

    public void setRightEdgeEffectColor(int color)
    {
        mEdgeGlowRight.setColor(color);
    }

    public void setLeftEdgeEffectColor(int color)
    {
        mEdgeGlowLeft.setColor(color);
    }

    public int getLeftEdgeEffectColor()
    {
        return mEdgeGlowLeft.getColor();
    }

    public int getRightEdgeEffectColor()
    {
        return mEdgeGlowRight.getColor();
    }

    public int getMaxScrollAmount()
    {
        return (int)(MAX_SCROLL_FACTOR * (getRight() - getLeft()));
    }

    private void ensureSingleChild()
    {
        if (getChildCount() > 0) throw new IllegalStateException("HorizontalScrollView can host only one direct child");
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
        int available = getWidth() - getPaddingLeft() - getPaddingRight();

        return Math.max(0, child.getWidth() - available);
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
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) return;
        if (getChildCount() == 0) return;

        View child = getChildAt(0);
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();

        ViewGroup.LayoutParams params = child.getLayoutParams();
        int horizontalMargins = 0;
        int verticalMargins = 0;

        if (params instanceof MarginLayoutParams)
        {
            MarginLayoutParams margins = (MarginLayoutParams)params;
            horizontalMargins = margins.leftMargin + margins.rightMargin;
            verticalMargins = margins.topMargin + margins.bottomMargin;
        }

        width -= horizontalMargins;
        if (child.getMeasuredWidth() >= width) return;

        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + verticalMargins,
                                                  params == null ? LayoutParams.MATCH_PARENT : params.height);

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

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return event.isAltPressed() ? fullScroll(FOCUS_LEFT) : arrowScroll(FOCUS_LEFT);
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return event.isAltPressed() ? fullScroll(FOCUS_RIGHT) : arrowScroll(FOCUS_RIGHT);
        if (keyCode == KeyEvent.KEYCODE_SPACE) return pageScroll(event.isShiftPressed() ? FOCUS_LEFT : FOCUS_RIGHT);
        if (keyCode == KeyEvent.KEYCODE_MOVE_HOME) return fullScroll(FOCUS_LEFT);
        if (keyCode == KeyEvent.KEYCODE_MOVE_END) return fullScroll(FOCUS_RIGHT);

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
            mLastMotionX = event.getX();
            mDragging = !mScroller.isFinished();

            return mDragging;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            if (Math.abs(event.getX() - mLastMotionX) > mTouchSlop)
            {
                mDragging = true;
                mLastMotionX = event.getX();

                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);

                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
        {
            mDragging = false;

            springBack();
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

            mLastMotionX = event.getX();

            if (!mScroller.isFinished()) mScroller.abortAnimation();

            return true;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            float x = event.getX();
            int delta = (int)(mLastMotionX - x);

            if (!mDragging && Math.abs(delta) > mTouchSlop)
            {
                mDragging = true;
                delta = delta > 0 ? delta - mTouchSlop : delta + mTouchSlop;

                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
            }

            if (!mDragging) return true;

            mLastMotionX = x;

            int range = getScrollRange();
            int requested = getScrollX() + delta;
            int target = Math.max(0, Math.min(range, requested));

            int overScrollMode = getOverScrollMode();
            boolean canOverScroll = overScrollMode == OVER_SCROLL_ALWAYS || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > 0);

            if (canOverScroll && getWidth() > 0)
            {
                float vertical = 1f - event.getY() / Math.max(1, getHeight());

                if (requested < 0)
                {
                    mEdgeGlowLeft.onPull((float)(requested) / getWidth(), vertical);
                    if (!mEdgeGlowRight.isFinished()) mEdgeGlowRight.onRelease();
                }
                else if (requested > range)
                {
                    mEdgeGlowRight.onPull((float)(requested - range) / getWidth(), 1f - vertical);
                    if (!mEdgeGlowLeft.isFinished()) mEdgeGlowLeft.onRelease();
                }

                if (!mEdgeGlowLeft.isFinished() || !mEdgeGlowRight.isFinished()) invalidate();
            }

            if (target != getScrollX()) scrollTo(target, getScrollY());

            return true;
        }

        if (action == MotionEvent.ACTION_UP)
        {
            if (mDragging)
            {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                int velocity = (int)mVelocityTracker.getXVelocity();

                if (Math.abs(velocity) > mMinimumVelocity) fling(-velocity);
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

    private void springBack()
    {
        mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0);

        if (!mScroller.isFinished()) invalidate();
    }

    private void endDrag()
    {
        mDragging = false;

        recycleVelocityTracker();

        mEdgeGlowLeft.onRelease();
        mEdgeGlowRight.onRelease();
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
            float axis = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            if (axis == 0) axis = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);

            if (axis != 0)
            {
                int delta = Math.round(axis * mHorizontalScrollFactor);
                int range = getScrollRange();
                int target = Math.max(0, Math.min(range, getScrollX() + delta));

                if (target != getScrollX())
                {
                    super.scrollTo(target, getScrollY());
                    return true;
                }
            }
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return true;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
        if (!mScroller.isFinished())
        {
            super.scrollTo(scrollX, scrollY);

            if (clampedX) mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0);
            return;
        }

        super.scrollTo(scrollX, scrollY);
    }

    public CharSequence getAccessibilityClassName()
    {
        return HorizontalScrollView.class.getName();
    }

    public boolean pageScroll(int direction)
    {
        int width = getWidth();
        int current = getScrollX();
        int target = direction == FOCUS_RIGHT ? current + width : current - width;

        target = Math.max(0, Math.min(getScrollRange(), target));
        if (target == current) return false;

        smoothScrollTo(target, getScrollY());
        return true;
    }

    public boolean fullScroll(int direction)
    {
        int current = getScrollX();
        int target = direction == FOCUS_RIGHT ? getScrollRange() : 0;
        if (target == current) return false;

        smoothScrollTo(target, getScrollY());
        return true;
    }

    public boolean arrowScroll(int direction)
    {
        int amount = getMaxScrollAmount();
        int current = getScrollX();
        int target = direction == FOCUS_RIGHT ? current + amount : current - amount;

        target = Math.max(0, Math.min(getScrollRange(), target));
        if (target == current) return false;

        if (mSmoothScrollingEnabled) smoothScrollBy(target - current, 0);
        else scrollTo(target, getScrollY());

        return true;
    }

    public final void smoothScrollBy(int dx, int dy)
    {
        if (getChildCount() == 0) return;

        int target = Math.max(0, Math.min(getScrollRange(), getScrollX() + dx));

        if (!mSmoothScrollingEnabled)
        {
            if (!mScroller.isFinished()) mScroller.abortAnimation();

            scrollTo(target, getScrollY());
            return;
        }

        mScroller.startScroll(getScrollX(), getScrollY(), target - getScrollX(), 0);
        invalidate();
    }

    public final void smoothScrollTo(int x, int y)
    {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    @Override
    protected int computeHorizontalScrollRange()
    {
        int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (getChildCount() == 0) return contentWidth;

        int scrollRange = getChildAt(0).getRight();
        int scrollX = getScrollX();
        int overscrollRight = Math.max(0, scrollRange - contentWidth);

        if (scrollX < 0) scrollRange -= scrollX;
        else if (scrollX > overscrollRight) scrollRange += scrollX - overscrollRight;

        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollOffset()
    {
        return Math.max(0, getScrollX());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) params = generateDefaultLayoutParams();

        int childHeightSpec = getChildMeasureSpec(parentHeightMeasureSpec, getPaddingTop() + getPaddingBottom(), params.height);
        int childWidthSpec = MeasureSpec.makeMeasureSpec(
            Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - getPaddingLeft() - getPaddingRight()), MeasureSpec.UNSPECIFIED);

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

        int verticalUsed = getPaddingTop() + getPaddingBottom() + verticalMargins + heightUsed;
        int childHeightSpec = getChildMeasureSpec(parentHeightMeasureSpec, verticalUsed, params.height);
        int usedTotal = getPaddingLeft() + getPaddingRight() + horizontalMargins + widthUsed;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - usedTotal),
                                                         MeasureSpec.UNSPECIFIED);

        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void computeScroll()
    {
        if (mScroller == null || !mScroller.computeScrollOffset()) return;

        int range = getScrollRange();
        int x = mScroller.getCurrX();
        int clamped = Math.max(0, Math.min(range, x));

        if (x != clamped && !mScroller.isFinished())
        {
            if (x < 0 && mEdgeGlowLeft.isFinished()) mEdgeGlowLeft.onAbsorb((int)mScroller.getCurrVelocity());
            else if (x > range && mEdgeGlowRight.isFinished()) mEdgeGlowRight.onAbsorb((int)mScroller.getCurrVelocity());
        }

        if (clamped != getScrollX()) onOverScrolled(clamped, getScrollY(), x != clamped, false);

        invalidate();
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect)
    {
        if (getChildCount() == 0) return 0;

        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;
        int fadingEdge = getHorizontalFadingEdgeLength();
        View child = getChildAt(0);

        if (rect.left > 0) screenLeft += fadingEdge;
        if (rect.right < child.getWidth()) screenRight -= fadingEdge;

        int delta = 0;

        if (rect.right > screenRight && rect.left > screenLeft)
        {
            if (rect.width() > width) delta += rect.left - screenLeft;
            else delta += rect.right - screenRight;

            delta = Math.min(delta, child.getRight() - screenRight);
        }
        else if (rect.left < screenLeft && rect.right < screenRight)
        {
            if (rect.width() > width) delta -= screenRight - rect.right;
            else delta -= screenLeft - rect.left;

            delta = Math.max(delta, -getScrollX());
        }

        return delta;
    }

    private boolean scrollToChildRect(Rect rect, boolean immediate)
    {
        int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        if (delta == 0) return false;

        if (immediate) scrollBy(delta, 0);
        else smoothScrollBy(delta, 0);

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

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect)
    {
        if (getChildCount() == 0) return false;

        return getChildAt(0).requestFocus(direction, previouslyFocusedRect);
    }

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
            super.scrollTo(Math.max(0, Math.min(getScrollRange(), position)), getScrollY());
            return;
        }

        int clamped = Math.max(0, Math.min(getScrollRange(), getScrollX()));
        if (clamped != getScrollX()) super.scrollTo(clamped, getScrollY());
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        int clamped = Math.max(0, Math.min(getScrollRange(), getScrollX()));
        if (clamped != getScrollX()) super.scrollTo(clamped, getScrollY());
    }

    public void fling(int velocityX)
    {
        if (getChildCount() == 0) return;

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int right = getChildAt(0).getWidth();

        mScroller.fling(getScrollX(), getScrollY(), velocityX, 0, 0, Math.max(0, right - width), 0, 0, width / 2, 0);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y)
    {
        if (getChildCount() > 0)
        {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingLeft() - getPaddingRight(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingTop() - getPaddingBottom(), child.getHeight());
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
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        if (!mEdgeGlowLeft.isFinished())
        {
            int saveCount = canvas.save();
            int height = getHeight() - getPaddingTop() - getPaddingBottom();

            canvas.rotate(270);
            canvas.translate(-height - getPaddingTop(), 0);
            mEdgeGlowLeft.setSize(height, getWidth());
            if (mEdgeGlowLeft.draw(canvas)) invalidate();
            canvas.restoreToCount(saveCount);
        }

        if (!mEdgeGlowRight.isFinished())
        {
            int saveCount = canvas.save();
            int width = getWidth();
            int height = getHeight() - getPaddingTop() - getPaddingBottom();

            canvas.rotate(90);
            canvas.translate(getPaddingTop(), -width);
            mEdgeGlowRight.setSize(height, width);
            if (mEdgeGlowRight.draw(canvas)) invalidate();
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
        state.mScrollPosition = getScrollX();
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

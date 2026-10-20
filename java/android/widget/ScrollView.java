package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class ScrollView extends FrameLayout
{
    public static final int FOCUS_UP = View.FOCUS_UP;
    public static final int FOCUS_DOWN = View.FOCUS_DOWN;

    private boolean mFillViewport;
    private boolean mSmoothScrollingEnabled = true;

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;

    private boolean mDragging;
    private float mLastMotionY;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    public ScrollView(Context context)
    {
        super(context);
        initialise(context);
    }

    public ScrollView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        initialise(context);
    }

    public ScrollView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
        initialise(context);
    }

    public ScrollView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
        initialise(context);
    }

    private void initialise(Context context)
    {
        mScroller = new OverScroller(context);

        ViewConfiguration configuration = ViewConfiguration.get(context);

        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public int getScrollRange()
    {
        if (getChildCount() == 0) return 0;

        View child = getChildAt(0);
        int available = getHeight() - getPaddingTop() - getPaddingBottom();

        return Math.max(0, child.getHeight() - available);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            mLastMotionY = event.getY();
            mDragging = !mScroller.isFinished();

            return mDragging;
        }

        if (action == MotionEvent.ACTION_MOVE && !mDragging)
        {
            if (Math.abs(event.getY() - mLastMotionY) > mTouchSlop)
            {
                mDragging = true;
                mLastMotionY = event.getY();

                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) mDragging = false;

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

            if (!mScroller.isFinished()) mScroller.abortAnimation();

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
            }

            if (!mDragging) return true;

            mLastMotionY = y;

            int range = getScrollRange();
            int target = Math.max(0, Math.min(range, getScrollY() + delta));

            if (target != getScrollY()) scrollTo(getScrollX(), target);

            return true;
        }

        if (action == MotionEvent.ACTION_UP)
        {
            if (mDragging)
            {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                int velocity = (int)mVelocityTracker.getYVelocity();

                if (Math.abs(velocity) > mMinimumVelocity) fling(-velocity);
            }

            endDrag();
            return true;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            endDrag();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void endDrag()
    {
        mDragging = false;

        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public void fling(int velocityY)
    {
        if (getChildCount() == 0) return;

        mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, getScrollRange());
        invalidate();
    }

    @Override
    public void computeScroll()
    {
        if (mScroller == null || !mScroller.computeScrollOffset()) return;

        int range = getScrollRange();
        int y = Math.max(0, Math.min(range, mScroller.getCurrY()));

        if (y != getScrollY()) scrollTo(getScrollX(), y);

        invalidate();
    }

    public void setFillViewport(boolean fill)
    {
        if (mFillViewport == fill) return;

        mFillViewport = fill;
        requestLayout();
    }

    public boolean isFillViewport()
    {
        return mFillViewport;
    }

    public void setSmoothScrollingEnabled(boolean enabled)
    {
        mSmoothScrollingEnabled = enabled;
    }

    public boolean isSmoothScrollingEnabled()
    {
        return mSmoothScrollingEnabled;
    }

    public void smoothScrollTo(int x, int y)
    {
        if (!mSmoothScrollingEnabled)
        {
            scrollTo(x, Math.max(0, Math.min(getScrollRange(), y)));
            return;
        }

        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    public void smoothScrollBy(int x, int y)
    {
        if (getChildCount() == 0) return;

        int target = Math.max(0, Math.min(getScrollRange(), getScrollY() + y));

        mScroller.startScroll(getScrollX(), getScrollY(), 0, target - getScrollY());
        invalidate();
    }

    public void fullScroll(int direction)
    {
        smoothScrollTo(getScrollX(), direction == FOCUS_DOWN ? getScrollRange() : 0);
    }

    public boolean pageScroll(int direction)
    {
        return false;
    }

    public boolean arrowScroll(int direction)
    {
        return false;
    }

    public int getMaxScrollAmount()
    {
        return getHeight() / 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, childHeightSpec, 0);
        }

        int widest = 0;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            widest = Math.max(widest, child.getMeasuredWidth());
        }

        widest = Math.max(widest + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSize(widest, widthMeasureSpec),
                             resolveSize(Math.max(getSuggestedMinimumHeight(), MeasureSpec.getSize(heightMeasureSpec)),
                                         heightMeasureSpec));
    }
}

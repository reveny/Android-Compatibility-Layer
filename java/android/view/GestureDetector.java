package android.view;

import android.content.Context;
import android.os.Handler;

public class GestureDetector
{
    public interface OnGestureListener
    {
        boolean onDown(MotionEvent event);

        void onShowPress(MotionEvent event);

        boolean onSingleTapUp(MotionEvent event);

        boolean onScroll(MotionEvent down, MotionEvent move, float distanceX, float distanceY);

        void onLongPress(MotionEvent event);

        boolean onFling(MotionEvent down, MotionEvent move, float velocityX, float velocityY);
    }

    public interface OnDoubleTapListener
    {
        boolean onSingleTapConfirmed(MotionEvent event);

        boolean onDoubleTap(MotionEvent event);

        boolean onDoubleTapEvent(MotionEvent event);
    }

    public interface OnContextClickListener
    {
        boolean onContextClick(MotionEvent event);
    }

    public static class SimpleOnGestureListener implements OnGestureListener, OnDoubleTapListener, OnContextClickListener
    {
        @Override
        public boolean onDown(MotionEvent event)
        {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent event)
        {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event)
        {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent down, MotionEvent move, float distanceX, float distanceY)
        {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent event)
        {
        }

        @Override
        public boolean onFling(MotionEvent down, MotionEvent move, float velocityX, float velocityY)
        {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event)
        {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent event)
        {
            return false;
        }

        @Override
        public boolean onContextClick(MotionEvent event)
        {
            return false;
        }
    }

    private final OnGestureListener mListener;
    private OnDoubleTapListener mDoubleTapListener;
    private OnContextClickListener mContextClickListener;
    private boolean mInContextClick;
    private final Handler mHandler;

    private final int mTouchSlop;
    private final int mMinimumFlingVelocity;
    private final int mMaximumFlingVelocity;
    private final int mDoubleTapSlop;

    private VelocityTracker mVelocityTracker;

    private MotionEvent mDownEvent;
    private MotionEvent mPreviousUp;

    private float mLastFocusX;
    private float mLastFocusY;
    private float mDownFocusX;
    private float mDownFocusY;

    private boolean mInLongPress;
    private boolean mStillWithinTapRegion;
    private boolean mIsDoubleTapping;
    private boolean mLongpressEnabled = true;

    private final Runnable mLongPressRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mDownEvent == null || !mStillWithinTapRegion) return;

            mInLongPress = true;
            mListener.onLongPress(mDownEvent);
        }
    };

    @Deprecated
    public GestureDetector(OnGestureListener listener, Handler handler)
    {
        this(null, listener, handler);
    }

    @Deprecated
    public GestureDetector(OnGestureListener listener)
    {
        this(null, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener)
    {
        this(context, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler, boolean unused)
    {
        this(context, listener, handler);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler)
    {
        if (listener == null) throw new NullPointerException("OnGestureListener must not be null");

        mListener = listener;

        android.os.Looper looper = handler != null ? handler.getLooper() : android.os.Looper.myLooper();
        if (looper == null) looper = android.os.Looper.getMainLooper();
        mHandler = new Handler(looper);

        if (listener instanceof OnDoubleTapListener) mDoubleTapListener = (OnDoubleTapListener)listener;
        if (listener instanceof OnContextClickListener) mContextClickListener = (OnContextClickListener)listener;

        ViewConfiguration configuration = ViewConfiguration.get(context);

        mTouchSlop = configuration.getScaledTouchSlop();
        mDoubleTapSlop = configuration.getScaledDoubleTapSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setOnDoubleTapListener(OnDoubleTapListener listener)
    {
        mDoubleTapListener = listener;
    }

    public void setContextClickListener(OnContextClickListener onContextClickListener)
    {
        mContextClickListener = onContextClickListener;
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (event == null) return false;

        int actionButton = event.getActionButton();

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_BUTTON_PRESS:
                if (mContextClickListener != null && !mInContextClick && !mInLongPress
                    && (actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY || actionButton == MotionEvent.BUTTON_SECONDARY))
                {
                    if (mContextClickListener.onContextClick(event))
                    {
                        mInContextClick = true;
                        mHandler.removeCallbacks(mLongPressRunnable);
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_BUTTON_RELEASE:
                if (mInContextClick && (actionButton == MotionEvent.BUTTON_STYLUS_PRIMARY || actionButton == MotionEvent.BUTTON_SECONDARY))
                {
                    mInContextClick = false;
                }
                break;

            default:
                break;
        }

        return false;
    }

    public void setIsLongpressEnabled(boolean enabled)
    {
        mLongpressEnabled = enabled;
    }

    public boolean isLongpressEnabled()
    {
        return mLongpressEnabled;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (event == null) return false;

        int action = event.getActionMasked();

        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        boolean handled = false;

        if (action == MotionEvent.ACTION_DOWN)
        {
            handled = handleDown(event);
        }
        else if (action == MotionEvent.ACTION_MOVE)
        {
            handled = handleMove(event);
        }
        else if (action == MotionEvent.ACTION_UP)
        {
            handled = handleUp(event);
        }
        else if (action == MotionEvent.ACTION_CANCEL)
        {
            cancel();
        }

        return handled;
    }

    private boolean handleDown(MotionEvent event)
    {
        boolean handled = false;

        if (mDoubleTapListener != null && isDoubleTap(event))
        {
            mIsDoubleTapping = true;
            handled = mDoubleTapListener.onDoubleTap(event) | mDoubleTapListener.onDoubleTapEvent(event);
        }

        mDownFocusX = event.getX();
        mDownFocusY = event.getY();
        mLastFocusX = mDownFocusX;
        mLastFocusY = mDownFocusY;

        if (mDownEvent != null) mDownEvent.recycle();
        mDownEvent = MotionEvent.obtain(event);

        mStillWithinTapRegion = true;
        mInLongPress = false;

        if (mLongpressEnabled)
        {
            mHandler.removeCallbacks(mLongPressRunnable);
            mHandler.postDelayed(mLongPressRunnable,
                                 ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout());
        }

        return mListener.onDown(event) || handled;
    }

    private boolean handleMove(MotionEvent event)
    {
        if (mInLongPress) return false;

        float x = event.getX();
        float y = event.getY();
        float scrollX = mLastFocusX - x;
        float scrollY = mLastFocusY - y;

        if (mIsDoubleTapping && mDoubleTapListener != null) return mDoubleTapListener.onDoubleTapEvent(event);

        if (mStillWithinTapRegion)
        {
            float deltaX = x - mDownFocusX;
            float deltaY = y - mDownFocusY;

            if (deltaX * deltaX + deltaY * deltaY <= mTouchSlop * mTouchSlop) return false;

            mStillWithinTapRegion = false;
            mHandler.removeCallbacks(mLongPressRunnable);

            mLastFocusX = x;
            mLastFocusY = y;

            return mListener.onScroll(mDownEvent, event, scrollX, scrollY);
        }

        if (Math.abs(scrollX) < 1 && Math.abs(scrollY) < 1) return false;

        mLastFocusX = x;
        mLastFocusY = y;

        return mListener.onScroll(mDownEvent, event, scrollX, scrollY);
    }

    private boolean handleUp(MotionEvent event)
    {
        mHandler.removeCallbacks(mLongPressRunnable);

        boolean handled = false;

        if (mIsDoubleTapping)
        {
            if (mDoubleTapListener != null) handled = mDoubleTapListener.onDoubleTapEvent(event);
        }
        else if (mInLongPress)
        {
            mInLongPress = false;
        }
        else if (mStillWithinTapRegion)
        {
            handled = mListener.onSingleTapUp(event);

            if (mDoubleTapListener != null) handled |= mDoubleTapListener.onSingleTapConfirmed(event);

            if (mPreviousUp != null) mPreviousUp.recycle();
            mPreviousUp = MotionEvent.obtain(event);
        }
        else
        {
            mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);

            float velocityX = mVelocityTracker.getXVelocity();
            float velocityY = mVelocityTracker.getYVelocity();

            if (Math.abs(velocityX) > mMinimumFlingVelocity || Math.abs(velocityY) > mMinimumFlingVelocity)
            {
                handled = mListener.onFling(mDownEvent, event, velocityX, velocityY);
            }
        }

        mIsDoubleTapping = false;
        mStillWithinTapRegion = false;

        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }

        return handled;
    }

    private void cancel()
    {
        mHandler.removeCallbacks(mLongPressRunnable);

        mIsDoubleTapping = false;
        mStillWithinTapRegion = false;
        mInLongPress = false;

        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private boolean isDoubleTap(MotionEvent event)
    {
        if (mPreviousUp == null || mDownEvent == null) return false;

        long interval = event.getEventTime() - mPreviousUp.getEventTime();
        if (interval > ViewConfiguration.getDoubleTapTimeout()) return false;

        float deltaX = mDownEvent.getX() - event.getX();
        float deltaY = mDownEvent.getY() - event.getY();

        return deltaX * deltaX + deltaY * deltaY < mDoubleTapSlop * mDoubleTapSlop;
    }
}

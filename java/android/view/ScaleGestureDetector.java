package android.view;

import android.content.Context;
import android.os.Handler;

public class ScaleGestureDetector
{
    private static final float SCALE_FACTOR = 0.5f;
    private static final int ANCHORED_SCALE_MODE_NONE = 0;
    private static final int ANCHORED_SCALE_MODE_DOUBLE_TAP = 1;
    private static final int ANCHORED_SCALE_MODE_STYLUS = 2;
    private static final int BUTTON_STYLUS_PRIMARY = 32;

    public interface OnScaleGestureListener
    {
        boolean onScale(ScaleGestureDetector detector);

        boolean onScaleBegin(ScaleGestureDetector detector);

        void onScaleEnd(ScaleGestureDetector detector);
    }

    public static class SimpleOnScaleGestureListener implements OnScaleGestureListener
    {
        public SimpleOnScaleGestureListener()
        {
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector)
        {
        }
    }

    private final Context mContext;
    private final OnScaleGestureListener mListener;
    private final Handler mHandler;

    private float mFocusX;
    private float mFocusY;
    private boolean mQuickScaleEnabled;
    private boolean mStylusScaleEnabled;
    private float mCurrSpan;
    private float mPrevSpan;
    private float mInitialSpan;
    private float mCurrSpanX;
    private float mCurrSpanY;
    private float mPrevSpanX;
    private float mPrevSpanY;
    private long mCurrTime;
    private long mPrevTime;
    private boolean mInProgress;
    private final int mSpanSlop;
    private final int mMinSpan;
    private float mAnchoredScaleStartX;
    private float mAnchoredScaleStartY;
    private int mAnchoredScaleMode = ANCHORED_SCALE_MODE_NONE;
    private boolean mEventBeforeOrAboveStartingGestureEvent;
    private GestureDetector mGestureDetector;

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener)
    {
        this(context, listener, null);
    }

    public ScaleGestureDetector(Context context, OnScaleGestureListener listener, Handler handler)
    {
        mContext = context;
        mListener = listener;
        mHandler = handler;

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mSpanSlop = configuration.getScaledTouchSlop() * 2;
        mMinSpan = configuration.getScaledMinimumScalingSpan();

        int targetSdk = context != null && context.getApplicationInfo() != null ? context.getApplicationInfo().targetSdkVersion : 34;
        if (targetSdk > 18) setQuickScaleEnabled(true);
        if (targetSdk > 22) setStylusScaleEnabled(true);
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        mCurrTime = event.getEventTime();

        int action = event.getActionMasked();

        if (mQuickScaleEnabled && mGestureDetector != null) mGestureDetector.onTouchEvent(event);

        int count = event.getPointerCount();
        boolean isStylusButtonDown = (event.getButtonState() & BUTTON_STYLUS_PRIMARY) != 0;

        boolean anchoredScaleCancelled = mAnchoredScaleMode == ANCHORED_SCALE_MODE_STYLUS && !isStylusButtonDown;
        boolean streamComplete = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || anchoredScaleCancelled;

        if (action == MotionEvent.ACTION_DOWN || streamComplete)
        {
            if (mInProgress)
            {
                mListener.onScaleEnd(this);
                mInProgress = false;
                mInitialSpan = 0;
                mAnchoredScaleMode = ANCHORED_SCALE_MODE_NONE;
            }
            else if (inAnchoredScaleMode() && streamComplete)
            {
                mInProgress = false;
                mInitialSpan = 0;
                mAnchoredScaleMode = ANCHORED_SCALE_MODE_NONE;
            }

            if (streamComplete) return true;
        }

        if (!mInProgress && mStylusScaleEnabled && !inAnchoredScaleMode() && !streamComplete && isStylusButtonDown)
        {
            mAnchoredScaleStartX = event.getX();
            mAnchoredScaleStartY = event.getY();
            mAnchoredScaleMode = ANCHORED_SCALE_MODE_STYLUS;
            mInitialSpan = 0;
        }

        boolean configChanged = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP
                                || action == MotionEvent.ACTION_POINTER_DOWN || anchoredScaleCancelled;

        boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
        int skipIndex = pointerUp ? event.getActionIndex() : -1;

        float sumX = 0;
        float sumY = 0;
        int divisor = pointerUp ? count - 1 : count;
        float focusX;
        float focusY;

        if (inAnchoredScaleMode())
        {
            focusX = mAnchoredScaleStartX;
            focusY = mAnchoredScaleStartY;
            mEventBeforeOrAboveStartingGestureEvent = event.getY() < focusY;
        }
        else
        {
            for (int index = 0; index < count; index++)
            {
                if (skipIndex == index) continue;

                sumX += event.getX(index);
                sumY += event.getY(index);
            }

            focusX = divisor > 0 ? sumX / divisor : 0;
            focusY = divisor > 0 ? sumY / divisor : 0;
        }

        float deviationSumX = 0;
        float deviationSumY = 0;

        for (int index = 0; index < count; index++)
        {
            if (skipIndex == index) continue;

            deviationSumX += Math.abs(event.getX(index) - focusX);
            deviationSumY += Math.abs(event.getY(index) - focusY);
        }

        float deviationX = divisor > 0 ? deviationSumX / divisor : 0;
        float deviationY = divisor > 0 ? deviationSumY / divisor : 0;

        float spanX = deviationX * 2;
        float spanY = deviationY * 2;
        float span = inAnchoredScaleMode() ? spanY : (float)Math.hypot(spanX, spanY);

        boolean wasInProgress = mInProgress;
        mFocusX = focusX;
        mFocusY = focusY;

        if (!inAnchoredScaleMode() && mInProgress && (span < mMinSpan || configChanged))
        {
            mListener.onScaleEnd(this);
            mInProgress = false;
            mInitialSpan = span;
        }

        if (configChanged)
        {
            mPrevSpanX = spanX;
            mCurrSpanX = spanX;
            mPrevSpanY = spanY;
            mCurrSpanY = spanY;
            mInitialSpan = span;
            mPrevSpan = span;
            mCurrSpan = span;
        }

        int minSpan = inAnchoredScaleMode() ? mSpanSlop : mMinSpan;

        if (!mInProgress && span >= minSpan && (wasInProgress || Math.abs(span - mInitialSpan) > mSpanSlop))
        {
            mPrevSpanX = spanX;
            mCurrSpanX = spanX;
            mPrevSpanY = spanY;
            mCurrSpanY = spanY;
            mPrevSpan = span;
            mCurrSpan = span;
            mPrevTime = mCurrTime;
            mInProgress = mListener.onScaleBegin(this);
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            mCurrSpanX = spanX;
            mCurrSpanY = spanY;
            mCurrSpan = span;

            boolean updatePrevious = true;
            if (mInProgress) updatePrevious = mListener.onScale(this);

            if (updatePrevious)
            {
                mPrevSpanX = mCurrSpanX;
                mPrevSpanY = mCurrSpanY;
                mPrevSpan = mCurrSpan;
                mPrevTime = mCurrTime;
            }
        }

        return true;
    }

    private boolean inAnchoredScaleMode()
    {
        return mAnchoredScaleMode != ANCHORED_SCALE_MODE_NONE;
    }

    public void setQuickScaleEnabled(boolean scales)
    {
        mQuickScaleEnabled = scales;

        if (mQuickScaleEnabled && mGestureDetector == null)
        {
            GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener()
            {
                @Override
                public boolean onDoubleTap(MotionEvent event)
                {
                    mAnchoredScaleStartX = event.getX();
                    mAnchoredScaleStartY = event.getY();
                    mAnchoredScaleMode = ANCHORED_SCALE_MODE_DOUBLE_TAP;
                    return true;
                }
            };

            mGestureDetector = new GestureDetector(mContext, gestureListener, mHandler);
        }
    }

    public boolean isQuickScaleEnabled()
    {
        return mQuickScaleEnabled;
    }

    public void setStylusScaleEnabled(boolean scales)
    {
        mStylusScaleEnabled = scales;
    }

    public boolean isStylusScaleEnabled()
    {
        return mStylusScaleEnabled;
    }

    public boolean isInProgress()
    {
        return mInProgress;
    }

    public float getFocusX()
    {
        return mFocusX;
    }

    public float getFocusY()
    {
        return mFocusY;
    }

    public float getCurrentSpan()
    {
        return mCurrSpan;
    }

    public float getCurrentSpanX()
    {
        return mCurrSpanX;
    }

    public float getCurrentSpanY()
    {
        return mCurrSpanY;
    }

    public float getPreviousSpan()
    {
        return mPrevSpan;
    }

    public float getPreviousSpanX()
    {
        return mPrevSpanX;
    }

    public float getPreviousSpanY()
    {
        return mPrevSpanY;
    }

    public float getScaleFactor()
    {
        if (inAnchoredScaleMode())
        {
            boolean scaleUp = (mEventBeforeOrAboveStartingGestureEvent && mCurrSpan < mPrevSpan)
                              || (!mEventBeforeOrAboveStartingGestureEvent && mCurrSpan > mPrevSpan);
            float spanDifference = Math.abs(1 - (mCurrSpan / mPrevSpan)) * SCALE_FACTOR;

            if (mPrevSpan <= 0) return 1;
            return scaleUp ? 1 + spanDifference : 1 - spanDifference;
        }

        return mPrevSpan > 0 ? mCurrSpan / mPrevSpan : 1;
    }

    public long getTimeDelta()
    {
        return mCurrTime - mPrevTime;
    }

    public long getEventTime()
    {
        return mCurrTime;
    }
}

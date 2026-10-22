package android.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class GestureOverlayView extends FrameLayout
{
    public static final int GESTURE_STROKE_TYPE_SINGLE = 0;
    public static final int GESTURE_STROKE_TYPE_MULTIPLE = 1;

    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final int FADE_ANIMATION_RATE = 16;
    private static final boolean GESTURE_RENDERING_ANTIALIAS = true;
    private static final boolean DITHER_FLAG = true;
    private static final float TOUCH_TOLERANCE = 3;

    private final Paint mGesturePaint = new Paint();
    private final Path mPath = new Path();
    private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList<>(100);
    private final ArrayList<OnGestureListener> mOnGestureListeners = new ArrayList<>();
    private final ArrayList<OnGesturePerformedListener> mOnGesturePerformedListeners = new ArrayList<>();
    private final ArrayList<OnGesturingListener> mOnGesturingListeners = new ArrayList<>();
    private final FadeOutRunnable mFadingOut = new FadeOutRunnable();

    private long mFadeDuration = 150;
    private long mFadeOffset = 420;
    private long mFadingStart;
    private boolean mFadingHasStarted;
    private boolean mFadeEnabled = true;

    private int mCurrentColor;
    private int mCertainGestureColor = 0xFFFFFF00;
    private int mUncertainGestureColor = 0x48FFFF00;
    private float mGestureStrokeWidth = 12.0f;

    private int mGestureStrokeType = GESTURE_STROKE_TYPE_SINGLE;
    private float mGestureStrokeLengthThreshold = 50.0f;
    private float mGestureStrokeSquarenessTreshold = 0.275f;
    private float mGestureStrokeAngleThreshold = 40.0f;

    private int mOrientation = ORIENTATION_VERTICAL;

    private boolean mGestureVisible = true;

    private float mX;
    private float mY;
    private float mTotalLength;
    private boolean mIsGesturing;
    private boolean mPreviousWasGesturing;
    private boolean mInterceptEvents = true;
    private boolean mIsListeningForGestures;
    private boolean mResetGesture;

    private Gesture mCurrentGesture;
    private boolean mHandleGestureActions;
    private boolean mIsFadingOut;
    private float mFadingAlpha = 1.0f;

    public GestureOverlayView(Context context)
    {
        super(context);
        init(null);
    }

    public GestureOverlayView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public GestureOverlayView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public GestureOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        if (attrs != null)
        {
            mCertainGestureColor = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "gestureColor", mCertainGestureColor);
            mUncertainGestureColor = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "uncertainGestureColor", mUncertainGestureColor);
            mFadeDuration = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "fadeDuration", (int)mFadeDuration);
            mFadeOffset = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "fadeOffset", (int)mFadeOffset);
            mGestureStrokeType = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "gestureStrokeType", mGestureStrokeType);
            mGestureStrokeLengthThreshold =
                attrs.getAttributeFloatValue(ANDROID_NAMESPACE, "gestureStrokeLengthThreshold", mGestureStrokeLengthThreshold);
            mGestureStrokeAngleThreshold =
                attrs.getAttributeFloatValue(ANDROID_NAMESPACE, "gestureStrokeAngleThreshold", mGestureStrokeAngleThreshold);
            mGestureStrokeSquarenessTreshold =
                attrs.getAttributeFloatValue(ANDROID_NAMESPACE, "gestureStrokeSquarenessThreshold", mGestureStrokeSquarenessTreshold);
            mInterceptEvents = attrs.getAttributeBooleanValue(ANDROID_NAMESPACE, "eventsInterceptionEnabled", mInterceptEvents);
            mFadeEnabled = attrs.getAttributeBooleanValue(ANDROID_NAMESPACE, "fadeEnabled", mFadeEnabled);
            mOrientation = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "orientation", mOrientation);
        }

        setWillNotDraw(false);

        mGesturePaint.setAntiAlias(GESTURE_RENDERING_ANTIALIAS);
        mGesturePaint.setColor(mCertainGestureColor);
        mGesturePaint.setStyle(Paint.Style.STROKE);
        mGesturePaint.setStrokeJoin(Paint.Join.ROUND);
        mGesturePaint.setStrokeCap(Paint.Cap.ROUND);
        mGesturePaint.setStrokeWidth(mGestureStrokeWidth);
        mGesturePaint.setDither(DITHER_FLAG);

        mCurrentColor = mCertainGestureColor;
        setPaintAlpha(255);
    }

    public ArrayList<GesturePoint> getCurrentStroke()
    {
        return mStrokeBuffer;
    }

    public int getOrientation()
    {
        return mOrientation;
    }

    public void setOrientation(int orientation)
    {
        mOrientation = orientation;
    }

    public void setGestureColor(int color)
    {
        mCertainGestureColor = color;
    }

    public void setUncertainGestureColor(int color)
    {
        mUncertainGestureColor = color;
    }

    public int getUncertainGestureColor()
    {
        return mUncertainGestureColor;
    }

    public int getGestureColor()
    {
        return mCertainGestureColor;
    }

    public float getGestureStrokeWidth()
    {
        return mGestureStrokeWidth;
    }

    public void setGestureStrokeWidth(float gestureStrokeWidth)
    {
        mGestureStrokeWidth = gestureStrokeWidth;
        mGesturePaint.setStrokeWidth(gestureStrokeWidth);
    }

    public int getGestureStrokeType()
    {
        return mGestureStrokeType;
    }

    public void setGestureStrokeType(int gestureStrokeType)
    {
        mGestureStrokeType = gestureStrokeType;
    }

    public float getGestureStrokeLengthThreshold()
    {
        return mGestureStrokeLengthThreshold;
    }

    public void setGestureStrokeLengthThreshold(float gestureStrokeLengthThreshold)
    {
        mGestureStrokeLengthThreshold = gestureStrokeLengthThreshold;
    }

    public float getGestureStrokeSquarenessTreshold()
    {
        return mGestureStrokeSquarenessTreshold;
    }

    public void setGestureStrokeSquarenessTreshold(float gestureStrokeSquarenessTreshold)
    {
        mGestureStrokeSquarenessTreshold = gestureStrokeSquarenessTreshold;
    }

    public float getGestureStrokeAngleThreshold()
    {
        return mGestureStrokeAngleThreshold;
    }

    public void setGestureStrokeAngleThreshold(float gestureStrokeAngleThreshold)
    {
        mGestureStrokeAngleThreshold = gestureStrokeAngleThreshold;
    }

    public boolean isEventsInterceptionEnabled()
    {
        return mInterceptEvents;
    }

    public void setEventsInterceptionEnabled(boolean enabled)
    {
        mInterceptEvents = enabled;
    }

    public boolean isFadeEnabled()
    {
        return mFadeEnabled;
    }

    public void setFadeEnabled(boolean fadeEnabled)
    {
        mFadeEnabled = fadeEnabled;
    }

    public Gesture getGesture()
    {
        return mCurrentGesture;
    }

    public void setGesture(Gesture gesture)
    {
        if (mCurrentGesture != null) clear(false);

        setCurrentColor(mCertainGestureColor);
        mCurrentGesture = gesture;

        Path path = mCurrentGesture.toPath();
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        mPath.rewind();
        mPath.addPath(path, -bounds.left + (getWidth() - bounds.width()) / 2.0f, -bounds.top + (getHeight() - bounds.height()) / 2.0f);

        mResetGesture = true;
        invalidate();
    }

    public Path getGesturePath()
    {
        return mPath;
    }

    public Path getGesturePath(Path path)
    {
        path.set(mPath);
        return path;
    }

    public boolean isGestureVisible()
    {
        return mGestureVisible;
    }

    public void setGestureVisible(boolean visible)
    {
        mGestureVisible = visible;
    }

    public long getFadeOffset()
    {
        return mFadeOffset;
    }

    public void setFadeOffset(long fadeOffset)
    {
        mFadeOffset = fadeOffset;
    }

    public void addOnGestureListener(OnGestureListener listener)
    {
        mOnGestureListeners.add(listener);
    }

    public void removeOnGestureListener(OnGestureListener listener)
    {
        mOnGestureListeners.remove(listener);
    }

    public void removeAllOnGestureListeners()
    {
        mOnGestureListeners.clear();
    }

    public void addOnGesturePerformedListener(OnGesturePerformedListener listener)
    {
        mOnGesturePerformedListeners.add(listener);
        if (mOnGesturePerformedListeners.size() > 0) mHandleGestureActions = true;
    }

    public void removeOnGesturePerformedListener(OnGesturePerformedListener listener)
    {
        mOnGesturePerformedListeners.remove(listener);
        if (mOnGesturePerformedListeners.size() <= 0) mHandleGestureActions = false;
    }

    public void removeAllOnGesturePerformedListeners()
    {
        mOnGesturePerformedListeners.clear();
        mHandleGestureActions = false;
    }

    public void addOnGesturingListener(OnGesturingListener listener)
    {
        mOnGesturingListeners.add(listener);
    }

    public void removeOnGesturingListener(OnGesturingListener listener)
    {
        mOnGesturingListeners.remove(listener);
    }

    public void removeAllOnGesturingListeners()
    {
        mOnGesturingListeners.clear();
    }

    public boolean isGesturing()
    {
        return mIsGesturing;
    }

    private void setCurrentColor(int color)
    {
        mCurrentColor = color;
        if (mFadingHasStarted) setPaintAlpha((int)(255 * mFadingAlpha));
        else setPaintAlpha(255);
        invalidate();
    }

    public Paint getGesturePaint()
    {
        return mGesturePaint;
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        if (mCurrentGesture != null && mGestureVisible) canvas.drawPath(mPath, mGesturePaint);
    }

    private void setPaintAlpha(int alpha)
    {
        alpha += alpha >> 7;
        int baseAlpha = mCurrentColor >>> 24;
        int useAlpha = baseAlpha * alpha >> 8;
        mGesturePaint.setColor((mCurrentColor << 8 >>> 8) | (useAlpha << 24));
    }

    public void clear(boolean animated)
    {
        clear(animated, false, true);
    }

    private void clear(boolean animated, boolean fireActionPerformed, boolean immediate)
    {
        setPaintAlpha(255);
        removeCallbacks(mFadingOut);
        mResetGesture = false;
        mFadingOut.mFireActionPerformed = fireActionPerformed;
        mFadingOut.mResetMultipleStrokes = false;

        if (animated && mCurrentGesture != null)
        {
            mFadingAlpha = 1.0f;
            mIsFadingOut = true;
            mFadingHasStarted = false;
            mFadingStart = SystemClock.uptimeMillis() + mFadeOffset;

            postDelayed(mFadingOut, mFadeOffset);
            return;
        }

        mFadingAlpha = 1.0f;
        mIsFadingOut = false;
        mFadingHasStarted = false;

        if (immediate)
        {
            mCurrentGesture = null;
            mPath.rewind();
            invalidate();
        }
        else if (fireActionPerformed)
        {
            postDelayed(mFadingOut, mFadeOffset);
        }
        else if (mGestureStrokeType == GESTURE_STROKE_TYPE_MULTIPLE)
        {
            mFadingOut.mResetMultipleStrokes = true;
            postDelayed(mFadingOut, mFadeOffset);
        }
        else
        {
            mCurrentGesture = null;
            mPath.rewind();
            invalidate();
        }
    }

    public void cancelClearAnimation()
    {
        setPaintAlpha(255);
        mIsFadingOut = false;
        mFadingHasStarted = false;
        removeCallbacks(mFadingOut);
        mPath.rewind();
        mCurrentGesture = null;
    }

    public void cancelGesture()
    {
        mIsListeningForGestures = false;

        if (mCurrentGesture != null) mCurrentGesture.addStroke(new GestureStroke(mStrokeBuffer));

        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);

        ArrayList<OnGestureListener> listeners = new ArrayList<>(mOnGestureListeners);
        for (OnGestureListener listener : listeners) listener.onGestureCancelled(this, event);

        event.recycle();

        clear(false);
        mIsGesturing = false;
        mPreviousWasGesturing = false;
        mStrokeBuffer.clear();

        ArrayList<OnGesturingListener> gesturing = new ArrayList<>(mOnGesturingListeners);
        for (OnGesturingListener listener : gesturing) listener.onGesturingEnded(this);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        cancelClearAnimation();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (!isEnabled()) return super.dispatchTouchEvent(event);

        boolean cancelDispatch = (mIsGesturing || (mCurrentGesture != null && mCurrentGesture.getStrokesCount() > 0
                                                   && mPreviousWasGesturing))
            && mInterceptEvents;

        processEvent(event);

        if (cancelDispatch) event.setAction(MotionEvent.ACTION_CANCEL);

        super.dispatchTouchEvent(event);
        return true;
    }

    private boolean processEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mIsListeningForGestures) return false;
                touchMove(event);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!mIsListeningForGestures) return false;
                touchUp(event, false);
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (!mIsListeningForGestures) return false;
                touchUp(event, true);
                invalidate();
                return true;
            default:
                return false;
        }
    }

    private void touchDown(MotionEvent event)
    {
        mIsListeningForGestures = true;

        float x = event.getX();
        float y = event.getY();

        mX = x;
        mY = y;

        mTotalLength = 0;
        mIsGesturing = false;

        if (mGestureStrokeType == GESTURE_STROKE_TYPE_SINGLE || mResetGesture)
        {
            if (mHandleGestureActions) setCurrentColor(mUncertainGestureColor);
            mResetGesture = false;
            mCurrentGesture = null;
            mPath.rewind();
        }
        else if (mCurrentGesture == null || mCurrentGesture.getStrokesCount() == 0)
        {
            if (mHandleGestureActions) setCurrentColor(mUncertainGestureColor);
        }

        if (mFadingHasStarted)
        {
            cancelClearAnimation();
        }
        else if (mIsFadingOut)
        {
            setPaintAlpha(255);
            mIsFadingOut = false;
            mFadingHasStarted = false;
            removeCallbacks(mFadingOut);
        }

        if (mCurrentGesture == null) mCurrentGesture = new Gesture();

        mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));
        mPath.moveTo(x, y);

        ArrayList<OnGestureListener> listeners = new ArrayList<>(mOnGestureListeners);
        for (OnGestureListener listener : listeners) listener.onGestureStarted(this, event);
    }

    private void touchMove(MotionEvent event)
    {
        float x = event.getX();
        float y = event.getY();

        float previousX = mX;
        float previousY = mY;

        float dx = Math.abs(x - previousX);
        float dy = Math.abs(y - previousY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
        {
            float controlX = (x + previousX) / 2;
            float controlY = (y + previousY) / 2;
            mPath.quadTo(previousX, previousY, controlX, controlY);

            mX = x;
            mY = y;

            mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));

            if (mHandleGestureActions && !mIsGesturing)
            {
                mTotalLength += (float)Math.hypot(dx, dy);

                if (mTotalLength > mGestureStrokeLengthThreshold)
                {
                    float[] coordinates = new float[mStrokeBuffer.size() * 2];
                    for (int index = 0; index < mStrokeBuffer.size(); index++)
                    {
                        coordinates[index * 2] = mStrokeBuffer.get(index).x;
                        coordinates[index * 2 + 1] = mStrokeBuffer.get(index).y;
                    }

                    OrientedBoundingBox box = GestureStroke.computeOrientedBoundingBox(coordinates);

                    float angle = Math.abs(box.orientation);
                    if (angle > 90) angle = 180 - angle;

                    boolean oriented = mOrientation == ORIENTATION_VERTICAL ? angle < mGestureStrokeAngleThreshold
                                                                            : angle > mGestureStrokeAngleThreshold;

                    if (box.squareness > mGestureStrokeSquarenessTreshold || oriented)
                    {
                        mIsGesturing = true;
                        setCurrentColor(mCertainGestureColor);

                        ArrayList<OnGesturingListener> gesturing = new ArrayList<>(mOnGesturingListeners);
                        for (OnGesturingListener listener : gesturing) listener.onGesturingStarted(this);
                    }
                }
            }

            ArrayList<OnGestureListener> listeners = new ArrayList<>(mOnGestureListeners);
            for (OnGestureListener listener : listeners) listener.onGesture(this, event);
        }
    }

    private void touchUp(MotionEvent event, boolean cancel)
    {
        mIsListeningForGestures = false;

        if (mCurrentGesture != null)
        {
            mCurrentGesture.addStroke(new GestureStroke(mStrokeBuffer));

            if (!cancel)
            {
                ArrayList<OnGestureListener> listeners = new ArrayList<>(mOnGestureListeners);
                for (OnGestureListener listener : listeners) listener.onGestureEnded(this, event);

                clear(mHandleGestureActions && mFadeEnabled, mHandleGestureActions && mIsGesturing, false);
            }
            else
            {
                cancelGesture(event);
            }
        }
        else
        {
            cancelGesture(event);
        }

        mStrokeBuffer.clear();
        mPreviousWasGesturing = mIsGesturing;
        mIsGesturing = false;

        ArrayList<OnGesturingListener> gesturing = new ArrayList<>(mOnGesturingListeners);
        for (OnGesturingListener listener : gesturing) listener.onGesturingEnded(this);
    }

    private void cancelGesture(MotionEvent event)
    {
        ArrayList<OnGestureListener> listeners = new ArrayList<>(mOnGestureListeners);
        for (OnGestureListener listener : listeners) listener.onGestureCancelled(this, event);

        clear(false);
    }

    private void fireOnGesturePerformed()
    {
        Gesture gesture = mCurrentGesture;
        ArrayList<OnGesturePerformedListener> listeners = new ArrayList<>(mOnGesturePerformedListeners);
        for (OnGesturePerformedListener listener : listeners) listener.onGesturePerformed(this, gesture);
    }

    private static float accelerateDecelerate(float input)
    {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0) + 0.5f;
    }

    private final class FadeOutRunnable implements Runnable
    {
        boolean mFireActionPerformed;
        boolean mResetMultipleStrokes;

        @Override
        public void run()
        {
            if (mIsFadingOut)
            {
                long now = SystemClock.uptimeMillis();
                long duration = now - mFadingStart;

                if (duration > mFadeDuration)
                {
                    if (mFireActionPerformed) fireOnGesturePerformed();

                    mPreviousWasGesturing = false;
                    mIsFadingOut = false;
                    mFadingHasStarted = false;
                    mPath.rewind();
                    mCurrentGesture = null;
                    setPaintAlpha(255);
                }
                else
                {
                    mFadingHasStarted = true;
                    float interpolatedTime = Math.max(0.0f, Math.min(1.0f, duration / (float)mFadeDuration));
                    mFadingAlpha = 1.0f - accelerateDecelerate(interpolatedTime);
                    setPaintAlpha((int)(255 * mFadingAlpha));
                    postDelayed(this, FADE_ANIMATION_RATE);
                }
            }
            else if (mResetMultipleStrokes)
            {
                mResetGesture = true;
            }
            else
            {
                fireOnGesturePerformed();

                mFadingHasStarted = false;
                mPath.rewind();
                mCurrentGesture = null;
                mPreviousWasGesturing = false;
                setPaintAlpha(255);
            }

            invalidate();
        }
    }

    public interface OnGesturingListener
    {
        void onGesturingStarted(GestureOverlayView overlay);

        void onGesturingEnded(GestureOverlayView overlay);
    }

    public interface OnGestureListener
    {
        void onGestureStarted(GestureOverlayView overlay, MotionEvent event);

        void onGesture(GestureOverlayView overlay, MotionEvent event);

        void onGestureEnded(GestureOverlayView overlay, MotionEvent event);

        void onGestureCancelled(GestureOverlayView overlay, MotionEvent event);
    }

    public interface OnGesturePerformedListener
    {
        void onGesturePerformed(GestureOverlayView overlay, Gesture gesture);
    }
}

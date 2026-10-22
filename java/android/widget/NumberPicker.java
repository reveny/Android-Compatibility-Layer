package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.Locale;

public class NumberPicker extends LinearLayout
{
    public interface OnValueChangeListener
    {
        void onValueChange(NumberPicker picker, int oldVal, int newVal);
    }

    public interface OnScrollListener
    {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChange(NumberPicker view, int scrollState);
    }

    public interface Formatter
    {
        String format(int value);
    }

    private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;
    private static final int SNAP_SCROLL_DURATION = 300;
    private static final float FADING_EDGE_STRENGTH = 0.9f;
    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300;

    private static StyleableSet sStyleable;

    private int mMinValue;
    private int mMaxValue;
    private int mValue;
    private String[] mDisplayedValues;
    private Formatter mFormatter;
    private boolean mWrapSelectorWheelPreferred = true;
    private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

    private OnValueChangeListener mOnValueChangeListener;
    private OnScrollListener mOnScrollListener;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDividerPaint = new Paint();
    private int mTextColor;
    private int mSelectionDividerHeight;
    private int mSolidColor;
    private final float mDensity;

    private int mElementHeight;
    private float mScrollOffset;
    private final Scroller mFlingScroller;
    private final Scroller mAdjustScroller;
    private int mPreviousScrollerY;
    private VelocityTracker mVelocityTracker;
    private float mDownY;
    private float mLastY;
    private boolean mMoved;
    private final int mTouchSlop;
    private final int mMinimumFlingVelocity;
    private final int mMaximumFlingVelocity;

    public NumberPicker(Context context)
    {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "numberPickerStyle"));
    }

    public NumberPicker(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public NumberPicker(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mDensity = context.getResources().getDisplayMetrics().density;
        mTextColor = ProgressBar.resolveThemeColor(context, 0xDE000000, "textColorPrimary");
        mSelectionDividerHeight = (int)(2 * mDensity + 0.5f);
        mTextPaint.setTextSize(20 * context.getResources().getDisplayMetrics().scaledDensity);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mDividerPaint.setColor(ProgressBar.resolveThemeColor(context, 0xFF009688, "colorControlNormal", "colorAccent"));

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / 8;

        mFlingScroller = new Scroller(context, null, true);
        mAdjustScroller = new Scroller(context, new android.view.animation.DecelerateInterpolator(2.5f));

        setOrientation(VERTICAL);
        setWillNotDraw(false);
        setFocusable(true);
        setVerticalFadingEdgeEnabled(true);

        StyleableSet styleable = styleable(context);
        if (styleable.isEmpty() || (attributes == null && defStyleAttr == 0 && defStyleRes == 0)) return;

        TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

        int index = styleable.indexOf("solidColor");
        if (index >= 0) mSolidColor = array.getColor(index, 0);

        index = styleable.indexOf("selectionDividerHeight");
        if (index >= 0) mSelectionDividerHeight = array.getDimensionPixelSize(index, mSelectionDividerHeight);

        index = styleable.indexOf("textColor");
        if (index >= 0 && array.hasValue(index)) mTextColor = array.getColor(index, mTextColor);

        index = styleable.indexOf("textSize");
        if (index >= 0 && array.hasValue(index)) mTextPaint.setTextSize(array.getDimension(index, mTextPaint.getTextSize()));

        array.recycle();
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "solidColor", "selectionDividerHeight", "textColor", "textSize");
        }
        return sStyleable;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        mElementHeight = Math.max(1, (bottom - top - getPaddingTop() - getPaddingBottom()) / SELECTOR_WHEEL_ITEM_COUNT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widest = 0;

        if (mDisplayedValues != null)
        {
            for (String value : mDisplayedValues) widest = Math.max(widest, measureLabel(value));
        }
        else
        {
            widest = Math.max(measureLabel(formatNumber(mMinValue)), measureLabel(formatNumber(mMaxValue)));
        }

        int width = Math.max((int)(64 * mDensity), widest + (int)(32 * mDensity)) + getPaddingLeft() + getPaddingRight();
        int height = (int)(180 * mDensity) + getPaddingTop() + getPaddingBottom();

        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    private int measureLabel(String label)
    {
        return (int)Math.ceil(mTextPaint.measureText(label == null ? "" : label));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        return isEnabled() && event.getActionMasked() == MotionEvent.ACTION_DOWN;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!isEnabled()) return false;

        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            mDownY = event.getY();
            mLastY = mDownY;
            mMoved = false;

            if (!mFlingScroller.isFinished()) mFlingScroller.forceFinished(true);
            if (!mAdjustScroller.isFinished()) mAdjustScroller.forceFinished(true);

            ViewParent parent = getParent();
            if (parent != null) parent.requestDisallowInterceptTouchEvent(true);

            return true;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            float y = event.getY();

            if (!mMoved && Math.abs(y - mDownY) > mTouchSlop)
            {
                mMoved = true;
                onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            if (mMoved)
            {
                scrollBy(0, (int)(y - mLastY));
                invalidate();
            }

            mLastY = y;
            return true;
        }

        if (action == MotionEvent.ACTION_UP)
        {
            mVelocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
            int velocity = (int)mVelocityTracker.getYVelocity();

            if (mMoved && Math.abs(velocity) > mMinimumFlingVelocity)
            {
                fling(velocity);
                onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
            }
            else if (!mMoved)
            {
                float y = event.getY();
                int third = getHeight() / SELECTOR_WHEEL_ITEM_COUNT;

                if (y < third) changeValueByOne(false);
                else if (y > getHeight() - third) changeValueByOne(true);
                else performClick();

                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }
            else
            {
                ensureScrollWheelAdjusted();
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }

            recycleVelocityTracker();
            return true;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            ensureScrollWheelAdjusted();
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            recycleVelocityTracker();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void recycleVelocityTracker()
    {
        if (mVelocityTracker == null) return;

        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
        {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;

            boolean increment = keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
            boolean canMove = getWrapSelectorWheel() || (increment ? mValue < mMaxValue : mValue > mMinValue);

            if (canMove)
            {
                changeValueByOne(increment);
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public boolean dispatchTrackballEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event)
    {
        return super.dispatchHoverEvent(event);
    }

    @Override
    public void computeScroll()
    {
        Scroller scroller = !mFlingScroller.isFinished() ? mFlingScroller : mAdjustScroller;
        if (scroller.isFinished()) return;

        scroller.computeScrollOffset();

        int currentY = scroller.getCurrY();

        scrollBy(0, currentY - mPreviousScrollerY);
        mPreviousScrollerY = currentY;

        if (scroller.isFinished())
        {
            mPreviousScrollerY = 0;

            if (scroller == mFlingScroller)
            {
                if (!ensureScrollWheelAdjusted()) onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }
            else
            {
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            }
        }

        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        invalidate();
    }

    @Override
    public void scrollBy(int x, int y)
    {
        if (mElementHeight <= 0 || y == 0) return;

        boolean wrap = getWrapSelectorWheel();

        if (!wrap && y > 0 && mValue <= mMinValue && mScrollOffset + y > 0)
        {
            mScrollOffset = 0;
            return;
        }

        if (!wrap && y < 0 && mValue >= mMaxValue && mScrollOffset + y < 0)
        {
            mScrollOffset = 0;
            return;
        }

        mScrollOffset += y;

        while (mScrollOffset > mElementHeight / 2f)
        {
            if (!wrap && mValue <= mMinValue)
            {
                mScrollOffset = 0;
                break;
            }

            mScrollOffset -= mElementHeight;
            setValueInternal(mValue - 1, true);
        }

        while (mScrollOffset < -mElementHeight / 2f)
        {
            if (!wrap && mValue >= mMaxValue)
            {
                mScrollOffset = 0;
                break;
            }

            mScrollOffset += mElementHeight;
            setValueInternal(mValue + 1, true);
        }

        invalidate();
    }

    private void fling(int velocityY)
    {
        if (velocityY > 0) mFlingScroller.fling(0, 0, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        else mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);

        mPreviousScrollerY = mFlingScroller.getStartY();
        invalidate();
    }

    private boolean ensureScrollWheelAdjusted()
    {
        int delta = (int)-mScrollOffset;
        if (delta == 0) return false;

        mAdjustScroller.startScroll(0, 0, 0, delta, SNAP_SCROLL_DURATION);
        mPreviousScrollerY = mAdjustScroller.getStartY();
        invalidate();

        return true;
    }

    private void changeValueByOne(boolean increment)
    {
        if (!mFlingScroller.isFinished()) mFlingScroller.forceFinished(true);
        if (!mAdjustScroller.isFinished()) mAdjustScroller.forceFinished(true);

        mScrollOffset = 0;
        setValueInternal(increment ? mValue + 1 : mValue - 1, true);
        invalidate();
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        return (int)((mValue - mMinValue) * Math.max(1, mElementHeight) - mScrollOffset);
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        return (mMaxValue - mMinValue + 1) * Math.max(1, mElementHeight);
    }

    @Override
    protected int computeVerticalScrollExtent()
    {
        return getHeight();
    }

    public int getSolidColor()
    {
        return mSolidColor;
    }

    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener)
    {
        mOnValueChangeListener = onValueChangedListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener)
    {
        mOnScrollListener = onScrollListener;
    }

    private void onScrollStateChange(int scrollState)
    {
        if (mScrollState == scrollState) return;

        mScrollState = scrollState;
        if (mOnScrollListener != null) mOnScrollListener.onScrollStateChange(this, scrollState);
    }

    public void setFormatter(Formatter formatter)
    {
        if (formatter == mFormatter) return;

        mFormatter = formatter;
        requestLayout();
        invalidate();
    }

    public void setValue(int value)
    {
        setValueInternal(value, false);
    }

    private void setValueInternal(int current, boolean notifyChange)
    {
        if (mValue == current) return;

        if (getWrapSelectorWheel())
        {
            current = wrapValue(current);
        }
        else
        {
            current = Math.max(current, mMinValue);
            current = Math.min(current, mMaxValue);
        }

        int previous = mValue;
        mValue = current;

        boolean changed = previous != current;
        if (notifyChange && changed && mOnValueChangeListener != null) mOnValueChangeListener.onValueChange(this, previous, current);

        invalidate();
    }

    private int wrapValue(int value)
    {
        if (value > mMaxValue) return mMinValue + (value - mMaxValue) % (mMaxValue - mMinValue + 1) - 1;
        if (value < mMinValue) return mMaxValue - (mMinValue - value) % (mMaxValue - mMinValue + 1) + 1;

        return value;
    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        return true;
    }

    @Override
    public boolean performLongClick()
    {
        super.performLongClick();
        return true;
    }

    public boolean getWrapSelectorWheel()
    {
        return mWrapSelectorWheelPreferred && mMaxValue - mMinValue >= SELECTOR_WHEEL_ITEM_COUNT;
    }

    public void setWrapSelectorWheel(boolean wrapSelectorWheel)
    {
        mWrapSelectorWheelPreferred = wrapSelectorWheel;
        invalidate();
    }

    public void setOnLongPressUpdateInterval(long intervalMillis)
    {
        mLongPressUpdateInterval = intervalMillis;
    }

    public int getValue()
    {
        return mValue;
    }

    public int getMinValue()
    {
        return mMinValue;
    }

    public void setMinValue(int minValue)
    {
        if (mMinValue == minValue) return;
        if (minValue < 0) throw new IllegalArgumentException("minValue must be >= 0");

        mMinValue = minValue;
        if (mMinValue > mValue) mValue = mMinValue;

        requestLayout();
        invalidate();
    }

    public int getMaxValue()
    {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue)
    {
        if (mMaxValue == maxValue) return;
        if (maxValue < 0) throw new IllegalArgumentException("maxValue must be >= 0");

        mMaxValue = maxValue;
        if (mMaxValue < mValue) mValue = mMaxValue;

        requestLayout();
        invalidate();
    }

    public String[] getDisplayedValues()
    {
        return mDisplayedValues;
    }

    public void setDisplayedValues(String[] displayedValues)
    {
        if (mDisplayedValues == displayedValues) return;

        mDisplayedValues = displayedValues;
        requestLayout();
        invalidate();
    }

    public void setSelectionDividerHeight(int height)
    {
        mSelectionDividerHeight = height;
        invalidate();
    }

    public int getSelectionDividerHeight()
    {
        return mSelectionDividerHeight;
    }

    @Override
    protected float getTopFadingEdgeStrength()
    {
        return FADING_EDGE_STRENGTH;
    }

    @Override
    protected float getBottomFadingEdgeStrength()
    {
        return FADING_EDGE_STRENGTH;
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        mFlingScroller.forceFinished(true);
        mAdjustScroller.forceFinished(true);
        recycleVelocityTracker();
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (!mAdjustScroller.isFinished())
        {
            mAdjustScroller.forceFinished(true);
            mScrollOffset = 0;
        }
    }

    private String formatNumber(int value)
    {
        if (mFormatter != null) return mFormatter.format(value);

        return String.format(Locale.getDefault(), "%d", value);
    }

    private String labelFor(int value)
    {
        if (mDisplayedValues != null)
        {
            int index = value - mMinValue;
            if (index < 0 || index >= mDisplayedValues.length) return "";

            String label = mDisplayedValues[index];
            return label == null ? "" : label;
        }

        return formatNumber(value);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int innerHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (mElementHeight <= 0) mElementHeight = Math.max(1, innerHeight / SELECTOR_WHEEL_ITEM_COUNT);

        float centreX = (getPaddingLeft() + getWidth() - getPaddingRight()) / 2f;
        float centreY = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2f;
        Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
        float baselineOffset = -(metrics.ascent + metrics.descent) / 2f;
        boolean wrap = getWrapSelectorWheel();

        for (int slot = -2; slot <= 2; slot++)
        {
            int value = mValue + slot;

            if (wrap) value = wrapValue(value);
            else if (value < mMinValue || value > mMaxValue) continue;

            float y = centreY + slot * mElementHeight + mScrollOffset;
            float distance = Math.min(1f, Math.abs(y - centreY) / mElementHeight);
            int alpha = (int)(255 * (1f - 0.6f * distance));

            if (!isEnabled()) alpha = alpha * 38 / 100;

            mTextPaint.setColor((mTextColor & 0x00FFFFFF) | ((((mTextColor >>> 24) * alpha) / 255) << 24));
            canvas.drawText(labelFor(value), centreX, y + baselineOffset, mTextPaint);
        }

        if (mSelectionDividerHeight <= 0) return;

        float half = mElementHeight / 2f;
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();

        canvas.drawRect(left, centreY - half - mSelectionDividerHeight, right, centreY - half, mDividerPaint);
        canvas.drawRect(left, centreY + half, right, centreY + half + mSelectionDividerHeight, mDividerPaint);
    }

    public AccessibilityNodeProvider getAccessibilityNodeProvider()
    {
        return null;
    }

    public void setTextColor(int color)
    {
        mTextColor = color;
        invalidate();
    }

    public int getTextColor()
    {
        return mTextColor;
    }

    public void setTextSize(float size)
    {
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
    }

    public float getTextSize()
    {
        return mTextPaint.getTextSize();
    }

    public CharSequence getAccessibilityClassName()
    {
        return NumberPicker.class.getName();
    }
}

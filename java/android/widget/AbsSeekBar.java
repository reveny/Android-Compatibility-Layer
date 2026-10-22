package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.ViewParent;

public abstract class AbsSeekBar extends ProgressBar
{
    private static final int NO_ALPHA = 0xFF;

    private static StyleableSet sStyleable;

    private Drawable mThumb;
    private Drawable mTickMark;
    private ColorStateList mThumbTintList;
    private ColorStateList mTickMarkTintList;
    private PorterDuff.Mode mThumbTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuff.Mode mTickMarkTintMode = PorterDuff.Mode.SRC_IN;
    private BlendMode mThumbTintBlendMode = BlendMode.SRC_IN;
    private BlendMode mTickMarkTintBlendMode = BlendMode.SRC_IN;
    private int mThumbOffset;
    private boolean mThumbOffsetSet;
    private boolean mSplitTrack;
    private int mKeyProgressIncrement = 1;
    private float mDisabledAlpha = 0.5f;
    private boolean mDragging;
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mThumbBounds = new Rect();

    boolean mIsUserSeekable = true;
    float mTouchProgressOffset;

    public AbsSeekBar(Context context)
    {
        this(context, null);
    }

    public AbsSeekBar(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AbsSeekBar(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        setFocusable(true);
        updateKeyProgressIncrement();

        StyleableSet styleable = styleable(context);
        if (styleable.isEmpty() || (attributes == null && defStyleAttr == 0 && defStyleRes == 0)) return;

        TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

        int index = styleable.indexOf("thumb");
        if (index >= 0 && array.hasValue(index)) setThumb(loadDrawable(array, index));

        index = styleable.indexOf("tickMark");
        if (index >= 0 && array.hasValue(index)) setTickMark(loadDrawable(array, index));

        index = styleable.indexOf("thumbOffset");
        if (index >= 0 && array.hasValue(index)) setThumbOffset(array.getDimensionPixelOffset(index, mThumbOffset));

        index = styleable.indexOf("splitTrack");
        if (index >= 0) mSplitTrack = array.getBoolean(index, false);

        index = styleable.indexOf("disabledAlpha");
        if (index >= 0) mDisabledAlpha = array.getFloat(index, mDisabledAlpha);

        index = styleable.indexOf("thumbTint");
        if (index >= 0 && array.hasValue(index)) mThumbTintList = readColors(array, index);

        index = styleable.indexOf("tickMarkTint");
        if (index >= 0 && array.hasValue(index)) mTickMarkTintList = readColors(array, index);

        array.recycle();

        applyTint(mThumb, mThumbTintList, getDrawableState());
        applyTint(mTickMark, mTickMarkTintList, getDrawableState());
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "thumb", "tickMark", "thumbOffset", "splitTrack", "disabledAlpha",
                                         "thumbTint", "tickMarkTint");
        }
        return sStyleable;
    }

    private static Drawable loadDrawable(TypedArray array, int index)
    {
        try
        {
            return array.getDrawable(index);
        }
        catch (Throwable unsupported)
        {
            return null;
        }
    }

    private static ColorStateList readColors(TypedArray array, int index)
    {
        try
        {
            return array.getColorStateList(index);
        }
        catch (RuntimeException unresolved)
        {
            return null;
        }
    }

    public void setThumb(Drawable thumb)
    {
        if (mThumb == thumb) return;

        if (mThumb != null) mThumb.setCallback(null);

        mThumb = thumb;

        if (thumb != null)
        {
            thumb.setCallback(this);
            if (thumb.isStateful()) thumb.setState(getDrawableState());
            if (!mThumbOffsetSet) mThumbOffset = thumb.getIntrinsicWidth() / 2;
        }

        applyTint(thumb, mThumbTintList, getDrawableState());
        requestLayout();
        invalidate();
    }

    public Drawable getThumb()
    {
        return mThumb;
    }

    public void setThumbTintList(ColorStateList tint)
    {
        mThumbTintList = tint;
        applyTint(mThumb, tint, getDrawableState());
        invalidate();
    }

    public ColorStateList getThumbTintList()
    {
        return mThumbTintList;
    }

    public void setThumbTintMode(PorterDuff.Mode tintMode)
    {
        mThumbTintMode = tintMode;
    }

    public void setThumbTintBlendMode(BlendMode blendMode)
    {
        mThumbTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getThumbTintMode()
    {
        return mThumbTintMode;
    }

    public BlendMode getThumbTintBlendMode()
    {
        return mThumbTintBlendMode;
    }

    public int getThumbOffset()
    {
        return mThumbOffset;
    }

    public void setThumbOffset(int thumbOffset)
    {
        mThumbOffset = thumbOffset;
        mThumbOffsetSet = true;
        invalidate();
    }

    public void setSplitTrack(boolean splitTrack)
    {
        mSplitTrack = splitTrack;
        invalidate();
    }

    public boolean getSplitTrack()
    {
        return mSplitTrack;
    }

    public void setTickMark(Drawable tickMark)
    {
        if (mTickMark == tickMark) return;

        if (mTickMark != null) mTickMark.setCallback(null);

        mTickMark = tickMark;

        if (tickMark != null)
        {
            tickMark.setCallback(this);
            if (tickMark.isStateful()) tickMark.setState(getDrawableState());
        }

        applyTint(tickMark, mTickMarkTintList, getDrawableState());
        invalidate();
    }

    public Drawable getTickMark()
    {
        return mTickMark;
    }

    public void setTickMarkTintList(ColorStateList tint)
    {
        mTickMarkTintList = tint;
        applyTint(mTickMark, tint, getDrawableState());
        invalidate();
    }

    public ColorStateList getTickMarkTintList()
    {
        return mTickMarkTintList;
    }

    public void setTickMarkTintMode(PorterDuff.Mode tintMode)
    {
        mTickMarkTintMode = tintMode;
    }

    public void setTickMarkTintBlendMode(BlendMode blendMode)
    {
        mTickMarkTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getTickMarkTintMode()
    {
        return mTickMarkTintMode;
    }

    public BlendMode getTickMarkTintBlendMode()
    {
        return mTickMarkTintBlendMode;
    }

    public void setKeyProgressIncrement(int increment)
    {
        mKeyProgressIncrement = increment < 0 ? -increment : increment;
    }

    public int getKeyProgressIncrement()
    {
        return mKeyProgressIncrement;
    }

    @Override
    public synchronized void setMin(int min)
    {
        super.setMin(min);
        updateKeyProgressIncrement();
    }

    @Override
    public synchronized void setMax(int max)
    {
        super.setMax(max);
        updateKeyProgressIncrement();
    }

    private void updateKeyProgressIncrement()
    {
        int range = getMax() - getMin();

        if (mKeyProgressIncrement == 0 || range / mKeyProgressIncrement > 20) setKeyProgressIncrement(Math.max(1, Math.round(range / 20f)));
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return who == mThumb || who == mTickMark || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (mThumb != null) mThumb.jumpToCurrentState();
        if (mTickMark != null) mTickMark.jumpToCurrentState();
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        int[] state = getDrawableState();

        if (mThumb != null && mThumb.isStateful()) mThumb.setState(state);
        if (mTickMark != null && mTickMark.isStateful()) mTickMark.setState(state);
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        if (mThumb != null) mThumb.setHotspot(x, y);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    public void setSystemGestureExclusionRects(java.util.List<Rect> rects)
    {
        super.setSystemGestureExclusionRects(rects);
    }

    boolean drawsDefaultThumb()
    {
        return true;
    }

    int thumbWidth()
    {
        if (mThumb != null) return Math.max(0, mThumb.getIntrinsicWidth());
        if (!drawsDefaultThumb()) return 0;

        return (int)(20 * density());
    }

    int thumbHeight()
    {
        if (mThumb != null) return Math.max(0, mThumb.getIntrinsicHeight());
        if (!drawsDefaultThumb()) return 0;

        return (int)(20 * density());
    }

    private int effectiveThumbOffset()
    {
        if (mThumbOffsetSet || mThumb != null) return mThumbOffset;

        return thumbWidth() / 2;
    }

    @Override
    protected synchronized void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (isIndeterminate()) return;

        drawTickMarks(canvas);
        drawThumb(canvas);
    }

    private void drawTickMarks(Canvas canvas)
    {
        if (mTickMark == null) return;

        int count = getMax() - getMin();
        if (count <= 1) return;

        int halfWidth = Math.max(1, mTickMark.getIntrinsicWidth() / 2);
        int halfHeight = Math.max(1, mTickMark.getIntrinsicHeight() / 2);
        float spacing = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float)count;
        float centreY = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2f;

        mTickMark.setBounds(-halfWidth, (int)centreY - halfHeight, halfWidth, (int)centreY + halfHeight);

        int saveCount = canvas.save();
        canvas.translate(getPaddingLeft(), 0);

        for (int index = 0; index <= count; index++)
        {
            mTickMark.draw(canvas);
            canvas.translate(spacing, 0);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawThumb(Canvas canvas)
    {
        int width = thumbWidth();
        int height = thumbHeight();
        if (width <= 0 || height <= 0) return;

        int offset = effectiveThumbOffset();
        int available = getWidth() - getPaddingLeft() - getPaddingRight() - width + offset * 2;
        float scale = getScale(getProgress());

        int position = (int)(scale * available + 0.5f);
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) position = available - position;

        int left = getPaddingLeft() - offset + position;
        int top = (getPaddingTop() + getHeight() - getPaddingBottom() - height) / 2;

        mThumbBounds.set(left, top, left + width, top + height);

        if (mThumb != null)
        {
            mThumb.setBounds(mThumbBounds);
            mThumb.draw(canvas);
            return;
        }

        int colour = tintColor(mThumbTintList, getDrawableState(), accentColor());
        if (!isEnabled()) colour = (colour & 0x00FFFFFF) | ((int)(NO_ALPHA * mDisabledAlpha) << 24);

        float radius = Math.min(width, height) / 2f * (mDragging ? 1f : 0.75f);

        mThumbPaint.setStyle(Paint.Style.FILL);
        mThumbPaint.setColor(colour);
        canvas.drawCircle(mThumbBounds.exactCenterX(), mThumbBounds.exactCenterY(), radius, mThumbPaint);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int contentWidth = Math.max(getMinWidth(), Math.min(getMaxWidth(), intrinsicContentWidth()));
        int contentHeight = Math.max(getMinHeight(), Math.min(getMaxHeight(), intrinsicContentHeight()));
        contentHeight = Math.max(contentHeight, thumbHeight());

        int width = contentWidth + getPaddingLeft() + getPaddingRight();
        int height = contentHeight + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!mIsUserSeekable || !isEnabled() || isIndeterminate()) return false;

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            startDrag(event);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            if (mDragging) trackTouchEvent(event);
            else startDrag(event);

            return true;
        }

        if (action == MotionEvent.ACTION_UP)
        {
            if (mDragging)
            {
                trackTouchEvent(event);
                onStopTrackingTouch();
                setPressed(false);
            }
            else
            {
                onStartTrackingTouch();
                trackTouchEvent(event);
                onStopTrackingTouch();
            }

            invalidate();
            return true;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            if (mDragging)
            {
                onStopTrackingTouch();
                setPressed(false);
            }

            invalidate();
            return true;
        }

        return true;
    }

    private void startDrag(MotionEvent event)
    {
        setPressed(true);
        onStartTrackingTouch();
        trackTouchEvent(event);

        ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
    }

    private void trackTouchEvent(MotionEvent event)
    {
        int x = Math.round(event.getX());
        int width = getWidth();
        int available = width - getPaddingLeft() - getPaddingRight();

        float scale;
        float progress = 0f;

        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL)
        {
            if (x > width - getPaddingRight()) scale = 0f;
            else if (x < getPaddingLeft()) scale = 1f;
            else scale = (available - x + getPaddingLeft()) / (float)Math.max(1, available);
        }
        else
        {
            if (x < getPaddingLeft()) scale = 0f;
            else if (x > width - getPaddingRight()) scale = 1f;
            else scale = (x - getPaddingLeft()) / (float)Math.max(1, available);

            progress = mTouchProgressOffset;
        }

        int range = getMax() - getMin();
        progress += scale * range + getMin();

        drawableHotspotChanged(x, event.getY());
        setProgressInternal(Math.round(progress), true, false);
    }

    void onStartTrackingTouch()
    {
        mDragging = true;
    }

    void onStopTrackingTouch()
    {
        mDragging = false;
    }

    void onKeyChange()
    {
    }

    boolean isDragging()
    {
        return mDragging;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (isEnabled() && mIsUserSeekable && !isIndeterminate())
        {
            int increment = mKeyProgressIncrement;
            boolean handled = true;

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MINUS || keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT)
            {
                increment = -increment;
            }
            else if (keyCode != KeyEvent.KEYCODE_DPAD_RIGHT && keyCode != KeyEvent.KEYCODE_PLUS && keyCode != KeyEvent.KEYCODE_EQUALS
                     && keyCode != KeyEvent.KEYCODE_NUMPAD_ADD)
            {
                handled = false;
            }

            if (handled)
            {
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) increment = -increment;

                if (setProgressInternal(getProgress() + increment, true, true)) onKeyChange();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return AbsSeekBar.class.getName();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);
        invalidate();
    }
}

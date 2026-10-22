package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class Switch extends CompoundButton
{
    private static final int THUMB_ANIMATION_DURATION = 250;
    private static final int FRAME_DELAY = 16;
    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;

    private static StyleableSet sStyleable;
    private static StyleableSet sAppearanceStyleable;

    private Drawable mThumbDrawable;
    private Drawable mTrackDrawable;
    private ColorStateList mThumbTintList;
    private ColorStateList mTrackTintList;
    private PorterDuff.Mode mThumbTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuff.Mode mTrackTintMode = PorterDuff.Mode.SRC_IN;
    private BlendMode mThumbTintBlendMode = BlendMode.SRC_IN;
    private BlendMode mTrackTintBlendMode = BlendMode.SRC_IN;

    private CharSequence mTextOn = "ON";
    private CharSequence mTextOff = "OFF";
    private boolean mShowText;
    private boolean mSplitTrack;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private int mThumbTextPadding;

    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private ColorStateList mTextColors;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Rect mThumbBounds = new Rect();
    private final int mAccentColor;
    private final float mDensity;

    private int mSwitchWidth;
    private int mSwitchHeight;
    private int mThumbWidth;
    private int mSwitchLeft;
    private int mSwitchTop;
    private int mSwitchRight;
    private int mSwitchBottom;

    private float mThumbPosition;
    private float mAnimationFrom;
    private float mAnimationTo;
    private long mAnimationStart;
    private boolean mAnimating;
    private final Runnable mAnimationTick = new Runnable()
    {
        @Override
        public void run()
        {
            stepAnimation();
        }
    };

    private int mTouchMode = TOUCH_MODE_IDLE;
    private final int mTouchSlop;
    private final int mMinimumFlingVelocity;
    private float mTouchX;
    private float mTouchY;
    private VelocityTracker mVelocityTracker;

    public Switch(Context context)
    {
        this(context, null);
    }

    public Switch(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "switchStyle"));
    }

    public Switch(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public Switch(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mDensity = context.getResources().getDisplayMetrics().density;
        mAccentColor = ProgressBar.resolveThemeColor(context, 0xFF009688, "colorControlActivated", "colorAccent");
        mTextPaint.density = mDensity;
        mTextPaint.setTextSize(14 * mDensity);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mSwitchPadding = (int)(16 * mDensity);
        mThumbTextPadding = (int)(4 * mDensity);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();

        setClickable(true);
        setFocusable(true);

        StyleableSet styleable = styleable(context);
        if (!styleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

            int index = styleable.indexOf("thumb");
            if (index >= 0 && array.hasValue(index)) setThumbDrawable(loadDrawable(array, index));

            index = styleable.indexOf("track");
            if (index >= 0 && array.hasValue(index)) setTrackDrawable(loadDrawable(array, index));

            index = styleable.indexOf("textOn");
            if (index >= 0)
            {
                CharSequence text = array.getText(index);
                if (text != null) mTextOn = text;
            }

            index = styleable.indexOf("textOff");
            if (index >= 0)
            {
                CharSequence text = array.getText(index);
                if (text != null) mTextOff = text;
            }

            index = styleable.indexOf("showText");
            if (index >= 0) mShowText = array.getBoolean(index, false);

            index = styleable.indexOf("thumbTextPadding");
            if (index >= 0) mThumbTextPadding = array.getDimensionPixelSize(index, mThumbTextPadding);

            index = styleable.indexOf("switchMinWidth");
            if (index >= 0) mSwitchMinWidth = array.getDimensionPixelSize(index, 0);

            index = styleable.indexOf("switchPadding");
            if (index >= 0) mSwitchPadding = array.getDimensionPixelSize(index, mSwitchPadding);

            index = styleable.indexOf("splitTrack");
            if (index >= 0) mSplitTrack = array.getBoolean(index, false);

            index = styleable.indexOf("thumbTint");
            if (index >= 0 && array.hasValue(index)) mThumbTintList = readColors(array, index);

            index = styleable.indexOf("trackTint");
            if (index >= 0 && array.hasValue(index)) mTrackTintList = readColors(array, index);

            index = styleable.indexOf("switchTextAppearance");
            if (index >= 0)
            {
                int appearance = array.getResourceId(index, 0);
                if (appearance != 0) setSwitchTextAppearance(context, appearance);
            }

            array.recycle();
        }

        ProgressBar.applyTint(mThumbDrawable, mThumbTintList, getDrawableState());
        ProgressBar.applyTint(mTrackDrawable, mTrackTintList, getDrawableState());

        mThumbPosition = isChecked() ? 1f : 0f;
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "thumb", "track", "textOn", "textOff", "showText", "thumbTextPadding",
                                         "switchMinWidth", "switchPadding", "splitTrack", "thumbTint", "trackTint", "switchTextAppearance");
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

    public void setSwitchTextAppearance(Context context, int resourceId)
    {
        if (sAppearanceStyleable == null)
        {
            sAppearanceStyleable = StyleableSet.of(context.getResources(), "textSize", "textColor", "textStyle", "textAllCaps");
        }
        if (sAppearanceStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(resourceId, sAppearanceStyleable.attributes());

        int index = sAppearanceStyleable.indexOf("textColor");
        if (index >= 0 && array.hasValue(index))
        {
            ColorStateList colors = readColors(array, index);
            mTextColors = colors != null ? colors : ColorStateList.valueOf(array.getColor(index, 0xFF000000));
        }

        index = sAppearanceStyleable.indexOf("textSize");
        if (index >= 0)
        {
            int size = array.getDimensionPixelSize(index, 0);
            if (size > 0 && size != mTextPaint.getTextSize()) mTextPaint.setTextSize(size);
        }

        index = sAppearanceStyleable.indexOf("textStyle");
        if (index >= 0 && array.hasValue(index)) setSwitchTypeface(null, array.getInt(index, Typeface.NORMAL));

        array.recycle();

        requestLayout();
        invalidate();
    }

    public void setSwitchTypeface(Typeface typeface, int style)
    {
        if (style > 0)
        {
            Typeface styled = typeface == null ? Typeface.defaultFromStyle(style) : Typeface.create(typeface, style);
            setSwitchTypeface(styled);

            int missing = style & ~(styled == null ? 0 : styled.getStyle());
            mTextPaint.setFakeBoldText((missing & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((missing & Typeface.ITALIC) != 0 ? -0.25f : 0);
            return;
        }

        mTextPaint.setFakeBoldText(false);
        mTextPaint.setTextSkewX(0);
        setSwitchTypeface(typeface);
    }

    public void setSwitchTypeface(Typeface typeface)
    {
        if (mTextPaint.getTypeface() == typeface) return;

        mTextPaint.setTypeface(typeface);
        requestLayout();
        invalidate();
    }

    public void setSwitchPadding(int pixels)
    {
        mSwitchPadding = pixels;
        requestLayout();
    }

    public int getSwitchPadding()
    {
        return mSwitchPadding;
    }

    public void setSwitchMinWidth(int pixels)
    {
        mSwitchMinWidth = pixels;
        requestLayout();
    }

    public int getSwitchMinWidth()
    {
        return mSwitchMinWidth;
    }

    public void setThumbTextPadding(int pixels)
    {
        mThumbTextPadding = pixels;
        requestLayout();
    }

    public int getThumbTextPadding()
    {
        return mThumbTextPadding;
    }

    public void setTrackDrawable(Drawable track)
    {
        if (mTrackDrawable != null) mTrackDrawable.setCallback(null);

        mTrackDrawable = track;

        if (track != null)
        {
            track.setCallback(this);
            if (track.isStateful()) track.setState(getDrawableState());
        }

        ProgressBar.applyTint(track, mTrackTintList, getDrawableState());
        requestLayout();
    }

    public void setTrackResource(int resourceId)
    {
        setTrackDrawable(getContext().getResources().getDrawable(resourceId));
    }

    public Drawable getTrackDrawable()
    {
        return mTrackDrawable;
    }

    public void setTrackIcon(Icon icon)
    {
        setTrackDrawable(icon == null ? null : icon.loadDrawable(getContext()));
    }

    public void setTrackTintList(ColorStateList tint)
    {
        mTrackTintList = tint;
        ProgressBar.applyTint(mTrackDrawable, tint, getDrawableState());
        invalidate();
    }

    public ColorStateList getTrackTintList()
    {
        return mTrackTintList;
    }

    public void setTrackTintMode(PorterDuff.Mode tintMode)
    {
        mTrackTintMode = tintMode;
    }

    public void setTrackTintBlendMode(BlendMode blendMode)
    {
        mTrackTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getTrackTintMode()
    {
        return mTrackTintMode;
    }

    public BlendMode getTrackTintBlendMode()
    {
        return mTrackTintBlendMode;
    }

    public void setThumbDrawable(Drawable thumb)
    {
        if (mThumbDrawable != null) mThumbDrawable.setCallback(null);

        mThumbDrawable = thumb;

        if (thumb != null)
        {
            thumb.setCallback(this);
            if (thumb.isStateful()) thumb.setState(getDrawableState());
        }

        ProgressBar.applyTint(thumb, mThumbTintList, getDrawableState());
        requestLayout();
    }

    public void setThumbResource(int resourceId)
    {
        setThumbDrawable(getContext().getResources().getDrawable(resourceId));
    }

    public Drawable getThumbDrawable()
    {
        return mThumbDrawable;
    }

    public void setThumbIcon(Icon icon)
    {
        setThumbDrawable(icon == null ? null : icon.loadDrawable(getContext()));
    }

    public void setThumbTintList(ColorStateList tint)
    {
        mThumbTintList = tint;
        ProgressBar.applyTint(mThumbDrawable, tint, getDrawableState());
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

    public void setSplitTrack(boolean splitTrack)
    {
        mSplitTrack = splitTrack;
        invalidate();
    }

    public boolean getSplitTrack()
    {
        return mSplitTrack;
    }

    public CharSequence getTextOn()
    {
        return mTextOn;
    }

    public void setTextOn(CharSequence textOn)
    {
        mTextOn = textOn;
        requestLayout();
    }

    public CharSequence getTextOff()
    {
        return mTextOff;
    }

    public void setTextOff(CharSequence textOff)
    {
        mTextOff = textOff;
        requestLayout();
    }

    public void setShowText(boolean showText)
    {
        if (mShowText == showText) return;

        mShowText = showText;
        requestLayout();
    }

    public boolean getShowText()
    {
        return mShowText;
    }

    private static boolean usable(Drawable drawable)
    {
        return drawable != null && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0;
    }

    private int measureLabel(CharSequence text)
    {
        if (text == null || text.length() == 0) return 0;

        return (int)Math.ceil(mTextPaint.measureText(text, 0, text.length()));
    }

    private boolean hasLabel()
    {
        CharSequence text = getText();
        return text != null && text.length() > 0;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int maxTextWidth = mShowText ? Math.max(measureLabel(mTextOn), measureLabel(mTextOff)) + mThumbTextPadding * 2 : 0;

        int thumbWidth = usable(mThumbDrawable) ? mThumbDrawable.getIntrinsicWidth() : (int)(20 * mDensity);
        int thumbHeight = usable(mThumbDrawable) ? mThumbDrawable.getIntrinsicHeight() : (int)(20 * mDensity);
        int trackHeight = usable(mTrackDrawable) ? mTrackDrawable.getIntrinsicHeight() : (int)(14 * mDensity);

        mThumbWidth = Math.max(maxTextWidth, thumbWidth);

        int minimumTrackWidth = usable(mTrackDrawable) ? mTrackDrawable.getIntrinsicWidth() : (int)(36 * mDensity);
        mSwitchWidth = Math.max(Math.max(mSwitchMinWidth, mThumbWidth * 2), minimumTrackWidth);
        mSwitchHeight = Math.max(trackHeight, thumbHeight);

        int reserved = mSwitchWidth + (hasLabel() ? mSwitchPadding : 0);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int innerWidth = Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - reserved);
        int innerSpec = widthMode == MeasureSpec.UNSPECIFIED ? widthMeasureSpec : MeasureSpec.makeMeasureSpec(innerWidth, widthMode);

        super.onMeasure(innerSpec, heightMeasureSpec);

        int width = getMeasuredWidth() + reserved;
        if (!hasLabel()) width = Math.max(getSuggestedMinimumWidth(), mSwitchWidth + getPaddingLeft() + getPaddingRight());

        int height = Math.max(getMeasuredHeight(), mSwitchHeight + getPaddingTop() + getPaddingBottom());
        if (!hasLabel()) height = Math.max(getSuggestedMinimumHeight(), mSwitchHeight + getPaddingTop() + getPaddingBottom());

        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        positionSwitch();
    }

    private void positionSwitch()
    {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL)
        {
            mSwitchLeft = getPaddingLeft();
            mSwitchRight = mSwitchLeft + mSwitchWidth;
        }
        else
        {
            mSwitchRight = getWidth() - getPaddingRight();
            mSwitchLeft = mSwitchRight - mSwitchWidth;
        }

        int innerTop = getPaddingTop();
        int innerBottom = getHeight() - getPaddingBottom();

        mSwitchTop = innerTop + (innerBottom - innerTop - mSwitchHeight) / 2;
        mSwitchBottom = mSwitchTop + mSwitchHeight;
    }

    private int thumbScrollRange()
    {
        return Math.max(0, mSwitchWidth - mThumbWidth);
    }

    private int thumbOffset()
    {
        float position = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? 1f - mThumbPosition : mThumbPosition;
        return (int)(position * thumbScrollRange() + 0.5f);
    }

    private boolean hitThumb(float x, float y)
    {
        int slop = mTouchSlop;
        int thumbLeft = mSwitchLeft + thumbOffset() - slop;
        int thumbRight = mSwitchLeft + thumbOffset() + mThumbWidth + slop;

        return x > thumbLeft && x < thumbRight && y > mSwitchTop - slop && y < mSwitchBottom + slop;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN)
        {
            float x = event.getX();
            float y = event.getY();

            if (isEnabled() && hitThumb(x, y))
            {
                mTouchMode = TOUCH_MODE_DOWN;
                mTouchX = x;
                mTouchY = y;
            }
        }
        else if (action == MotionEvent.ACTION_MOVE)
        {
            if (mTouchMode == TOUCH_MODE_DOWN)
            {
                float x = event.getX();
                float y = event.getY();

                if (Math.abs(x - mTouchX) > mTouchSlop || Math.abs(y - mTouchY) > mTouchSlop)
                {
                    mTouchMode = TOUCH_MODE_DRAGGING;
                    mTouchX = x;
                    mTouchY = y;

                    cancelLongPress();
                    setPressed(false);

                    ViewParent parent = getParent();
                    if (parent != null) parent.requestDisallowInterceptTouchEvent(true);

                    return true;
                }
            }
            else if (mTouchMode == TOUCH_MODE_DRAGGING)
            {
                float x = event.getX();
                int range = thumbScrollRange();
                float delta = range > 0 ? (x - mTouchX) / range : (x > mTouchX ? 1f : -1f);
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) delta = -delta;

                float position = Math.max(0f, Math.min(1f, mThumbPosition + delta));
                if (position != mThumbPosition)
                {
                    mTouchX = x;
                    setThumbPosition(position);
                }

                return true;
            }
        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
        {
            if (mTouchMode == TOUCH_MODE_DRAGGING)
            {
                stopDrag(event);
                return true;
            }

            mTouchMode = TOUCH_MODE_IDLE;
            recycleVelocityTracker();
        }

        return super.onTouchEvent(event);
    }

    private void stopDrag(MotionEvent event)
    {
        mTouchMode = TOUCH_MODE_IDLE;

        boolean commit = event.getActionMasked() == MotionEvent.ACTION_UP && isEnabled();
        boolean oldState = isChecked();
        boolean newState = oldState;

        if (commit)
        {
            mVelocityTracker.computeCurrentVelocity(1000);
            float velocity = mVelocityTracker.getXVelocity();

            if (Math.abs(velocity) > mMinimumFlingVelocity)
            {
                newState = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? velocity < 0 : velocity > 0;
            }
            else
            {
                newState = mThumbPosition > 0.5f;
            }
        }

        recycleVelocityTracker();

        if (newState != oldState) playSoundEffect(0);

        setChecked(newState);
        animateThumbTo(newState ? 1f : 0f);
    }

    private void recycleVelocityTracker()
    {
        if (mVelocityTracker == null) return;

        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    private void setThumbPosition(float position)
    {
        mThumbPosition = position;
        invalidate();
    }

    private void animateThumbTo(float target)
    {
        if (!isAttachedToWindow() || getWidth() == 0)
        {
            mAnimating = false;
            removeCallbacks(mAnimationTick);
            setThumbPosition(target);
            return;
        }

        mAnimationFrom = mThumbPosition;
        mAnimationTo = target;
        mAnimationStart = SystemClock.uptimeMillis();

        if (!mAnimating)
        {
            mAnimating = true;
            postDelayed(mAnimationTick, FRAME_DELAY);
        }
    }

    private void stepAnimation()
    {
        if (!mAnimating) return;

        float fraction = Math.min(1f, (SystemClock.uptimeMillis() - mAnimationStart) / (float)THUMB_ANIMATION_DURATION);
        float eased = (float)(Math.cos((fraction + 1) * Math.PI) / 2.0f) + 0.5f;

        setThumbPosition(mAnimationFrom + (mAnimationTo - mAnimationFrom) * eased);

        if (fraction >= 1f)
        {
            mAnimating = false;
            return;
        }

        postDelayed(mAnimationTick, FRAME_DELAY);
    }

    @Override
    public void toggle()
    {
        setChecked(!isChecked());
    }

    @Override
    public void setChecked(boolean checked)
    {
        super.setChecked(checked);

        boolean current = isChecked();

        if (mTouchMode != TOUCH_MODE_DRAGGING) animateThumbTo(current ? 1f : 0f);

        refreshDrawableState();
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (mSwitchWidth == 0) return;
        if (mSwitchRight - mSwitchLeft != mSwitchWidth) positionSwitch();

        int[] state = getDrawableState();
        boolean enabled = isEnabled();
        float alpha = enabled ? 1f : 0.38f;

        int thumbLeft = mSwitchLeft + thumbOffset();
        int thumbTop = usable(mThumbDrawable) ? mSwitchTop + (mSwitchHeight - mThumbDrawable.getIntrinsicHeight()) / 2 : mSwitchTop;
        int thumbBottom = usable(mThumbDrawable) ? thumbTop + mThumbDrawable.getIntrinsicHeight() : mSwitchBottom;
        mThumbBounds.set(thumbLeft, thumbTop, thumbLeft + mThumbWidth, thumbBottom);

        float position = mThumbPosition;

        if (usable(mTrackDrawable))
        {
            int trackTop = mSwitchTop + (mSwitchHeight - mTrackDrawable.getIntrinsicHeight()) / 2;
            mTrackDrawable.setBounds(mSwitchLeft, trackTop, mSwitchRight, trackTop + mTrackDrawable.getIntrinsicHeight());
            mTrackDrawable.draw(canvas);
        }
        else
        {
            int off = ProgressBar.tintColor(mTrackTintList, state, 0x4D000000);
            int on = (mAccentColor & 0x00FFFFFF) | 0x80000000;
            int colour = blend(off, on, position);

            float trackHeight = 14 * mDensity;
            float centreY = (mSwitchTop + mSwitchBottom) / 2f;

            mRect.set(mSwitchLeft, centreY - trackHeight / 2f, mSwitchRight, centreY + trackHeight / 2f);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(withAlpha(colour, alpha));
            canvas.drawRoundRect(mRect, trackHeight / 2f, trackHeight / 2f, mPaint);
        }

        if (usable(mThumbDrawable))
        {
            mThumbDrawable.setBounds(mThumbBounds);
            mThumbDrawable.draw(canvas);
        }
        else
        {
            int off = 0xFFF1F1F1;
            int on = ProgressBar.tintColor(mThumbTintList, state, mAccentColor);
            int colour = blend(off, on, position);

            float radius = Math.min(mThumbBounds.width(), mThumbBounds.height()) / 2f;

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(withAlpha(0x33000000, alpha));
            canvas.drawCircle(mThumbBounds.exactCenterX(), mThumbBounds.exactCenterY() + mDensity, radius, mPaint);

            mPaint.setColor(withAlpha(colour, alpha));
            canvas.drawCircle(mThumbBounds.exactCenterX(), mThumbBounds.exactCenterY(), radius, mPaint);
        }

        if (!mShowText) return;

        CharSequence label = position > 0.5f ? mTextOn : mTextOff;
        if (label == null || label.length() == 0) return;

        int textColour = mTextColors != null ? mTextColors.getColorForState(state, mTextColors.getDefaultColor()) : getCurrentTextColor();
        mTextPaint.setColor(textColour);
        mTextPaint.drawableState = state;

        String text = label.toString();
        float textWidth = mTextPaint.measureText(text);
        Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
        float baseline = mThumbBounds.exactCenterY() - (metrics.ascent + metrics.descent) / 2f;

        canvas.drawText(text, mThumbBounds.exactCenterX() - textWidth / 2f, baseline, mTextPaint);
    }

    private static int blend(int from, int to, float fraction)
    {
        float inverse = 1f - fraction;

        int alpha = (int)(((from >>> 24) & 0xFF) * inverse + ((to >>> 24) & 0xFF) * fraction);
        int red = (int)(((from >> 16) & 0xFF) * inverse + ((to >> 16) & 0xFF) * fraction);
        int green = (int)(((from >> 8) & 0xFF) * inverse + ((to >> 8) & 0xFF) * fraction);
        int blue = (int)((from & 0xFF) * inverse + (to & 0xFF) * fraction);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int colour, float alpha)
    {
        int channel = (int)(((colour >>> 24) & 0xFF) * alpha);
        return (colour & 0x00FFFFFF) | (channel << 24);
    }

    public int getCompoundPaddingLeft()
    {
        if (getLayoutDirection() != LAYOUT_DIRECTION_RTL) return getPaddingLeft();

        return getPaddingLeft() + mSwitchWidth + (hasLabel() ? mSwitchPadding : 0);
    }

    public int getCompoundPaddingRight()
    {
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) return getPaddingRight();

        return getPaddingRight() + mSwitchWidth + (hasLabel() ? mSwitchPadding : 0);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        return super.onCreateDrawableState(extraSpace);
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        int[] state = getDrawableState();

        if (mThumbDrawable != null && mThumbDrawable.isStateful()) mThumbDrawable.setState(state);
        if (mTrackDrawable != null && mTrackDrawable.isStateful()) mTrackDrawable.setState(state);

        invalidate();
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        if (mThumbDrawable != null) mThumbDrawable.setHotspot(x, y);
        if (mTrackDrawable != null) mTrackDrawable.setHotspot(x, y);
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return who == mThumbDrawable || who == mTrackDrawable || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (mThumbDrawable != null) mThumbDrawable.jumpToCurrentState();
        if (mTrackDrawable != null) mTrackDrawable.jumpToCurrentState();

        if (mAnimating)
        {
            mAnimating = false;
            removeCallbacks(mAnimationTick);
            setThumbPosition(isChecked() ? 1f : 0f);
        }
    }

    public CharSequence getAccessibilityClassName()
    {
        return Switch.class.getName();
    }
}

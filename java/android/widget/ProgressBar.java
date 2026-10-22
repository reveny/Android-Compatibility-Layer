package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class ProgressBar extends View
{
    static final int LAYER_BACKGROUND = 0x01020000;
    static final int LAYER_PROGRESS = 0x0102000d;
    static final int LAYER_SECONDARY_PROGRESS = 0x0102000f;

    private static final int FRAME_DELAY = 16;
    private static final int ROTATION_DURATION = 1333;
    private static final int SLIDE_DURATION = 2000;
    private static final int DEFAULT_ACCENT = 0xFF009688;

    private static StyleableSet sStyleable;

    private int mMin;
    private int mMax = 100;
    private int mProgress;
    private int mSecondaryProgress;
    private boolean mIndeterminate;
    private boolean mOnlyIndeterminate;
    private boolean mMinInitialised;
    private boolean mMaxInitialised;

    private int mMinWidth;
    private int mMaxWidth;
    private int mMinHeight;
    private int mMaxHeight;

    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    private Drawable mCurrentDrawable;

    private ColorStateList mProgressTintList;
    private ColorStateList mProgressBackgroundTintList;
    private ColorStateList mSecondaryProgressTintList;
    private ColorStateList mIndeterminateTintList;
    private PorterDuff.Mode mProgressTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuff.Mode mProgressBackgroundTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuff.Mode mSecondaryProgressTintMode = PorterDuff.Mode.SRC_IN;
    private PorterDuff.Mode mIndeterminateTintMode = PorterDuff.Mode.SRC_IN;
    private BlendMode mProgressTintBlendMode = BlendMode.SRC_IN;
    private BlendMode mProgressBackgroundTintBlendMode = BlendMode.SRC_IN;
    private BlendMode mSecondaryProgressTintBlendMode = BlendMode.SRC_IN;
    private BlendMode mIndeterminateTintBlendMode = BlendMode.SRC_IN;

    private Interpolator mInterpolator;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mArcBounds = new RectF();
    private final int mAccentColor;
    private final float mDensity;

    private boolean mAnimating;
    private boolean mAggregatedVisible = true;
    private long mAnimationStart;
    private final Runnable mAnimationTick = new Runnable()
    {
        @Override
        public void run()
        {
            if (!mAnimating) return;

            invalidate();
            postDelayed(this, FRAME_DELAY);
        }
    };

    public ProgressBar(Context context)
    {
        this(context, null);
    }

    public ProgressBar(Context context, AttributeSet attributes)
    {
        this(context, attributes, attributeIdentifier(context, "progressBarStyle"));
    }

    public ProgressBar(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ProgressBar(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mDensity = context.getResources().getDisplayMetrics().density;
        mAccentColor = resolveThemeColor(context, DEFAULT_ACCENT, "colorControlActivated", "colorAccent");

        mMinWidth = (int)(24 * mDensity);
        mMaxWidth = (int)(48 * mDensity);
        mMinHeight = (int)(24 * mDensity);
        mMaxHeight = (int)(48 * mDensity);

        StyleableSet styleable = styleable(context);
        if (!styleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);
            readAttributes(array, styleable);
            array.recycle();
        }

        if (mInterpolator == null) mInterpolator = new LinearInterpolator();
        if (mOnlyIndeterminate) mIndeterminate = true;

        attachDrawable(mProgressDrawable);
        attachDrawable(mIndeterminateDrawable);
        applyProgressTints();
        applyIndeterminateTint();

        mCurrentDrawable = mIndeterminate ? mIndeterminateDrawable : mProgressDrawable;
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "min", "max", "progress", "secondaryProgress", "indeterminate",
                                         "indeterminateOnly", "minWidth", "maxWidth", "minHeight", "maxHeight", "progressDrawable",
                                         "indeterminateDrawable", "progressTint", "progressBackgroundTint", "secondaryProgressTint",
                                         "indeterminateTint", "interpolator");
        }
        return sStyleable;
    }

    private void readAttributes(TypedArray array, StyleableSet styleable)
    {
        int index = styleable.indexOf("progressDrawable");
        if (index >= 0 && array.hasValue(index)) mProgressDrawable = loadDrawable(array, index);

        index = styleable.indexOf("indeterminateDrawable");
        if (index >= 0 && array.hasValue(index)) mIndeterminateDrawable = loadDrawable(array, index);

        index = styleable.indexOf("minWidth");
        if (index >= 0) mMinWidth = array.getDimensionPixelSize(index, mMinWidth);

        index = styleable.indexOf("maxWidth");
        if (index >= 0) mMaxWidth = array.getDimensionPixelSize(index, mMaxWidth);

        index = styleable.indexOf("minHeight");
        if (index >= 0) mMinHeight = array.getDimensionPixelSize(index, mMinHeight);

        index = styleable.indexOf("maxHeight");
        if (index >= 0) mMaxHeight = array.getDimensionPixelSize(index, mMaxHeight);

        index = styleable.indexOf("interpolator");
        if (index >= 0)
        {
            int resource = array.getResourceId(index, 0);
            if (resource != 0) setInterpolator(getContext(), resource);
        }

        index = styleable.indexOf("min");
        if (index >= 0 && array.hasValue(index)) setMin(array.getInt(index, mMin));

        index = styleable.indexOf("max");
        if (index >= 0 && array.hasValue(index)) setMax(array.getInt(index, mMax));

        index = styleable.indexOf("progress");
        if (index >= 0) setProgress(array.getInt(index, mProgress));

        index = styleable.indexOf("secondaryProgress");
        if (index >= 0) setSecondaryProgress(array.getInt(index, mSecondaryProgress));

        index = styleable.indexOf("indeterminateOnly");
        if (index >= 0) mOnlyIndeterminate = array.getBoolean(index, false);

        index = styleable.indexOf("indeterminate");
        if (index >= 0) mIndeterminate = array.getBoolean(index, mIndeterminate);

        mProgressTintList = readTint(array, styleable, "progressTint", mProgressTintList);
        mProgressBackgroundTintList = readTint(array, styleable, "progressBackgroundTint", mProgressBackgroundTintList);
        mSecondaryProgressTintList = readTint(array, styleable, "secondaryProgressTint", mSecondaryProgressTintList);
        mIndeterminateTintList = readTint(array, styleable, "indeterminateTint", mIndeterminateTintList);
    }

    private static ColorStateList readTint(TypedArray array, StyleableSet styleable, String name, ColorStateList fallback)
    {
        int index = styleable.indexOf(name);
        if (index < 0 || !array.hasValue(index)) return fallback;

        try
        {
            ColorStateList list = array.getColorStateList(index);
            return list != null ? list : fallback;
        }
        catch (RuntimeException unresolved)
        {
            return fallback;
        }
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

    static int attributeIdentifier(Context context, String name)
    {
        if (context == null) return 0;

        return context.getResources().getIdentifier(name, "attr", "android");
    }

    static int resolveThemeColor(Context context, int fallback, String... names)
    {
        for (String name : names)
        {
            int identifier = attributeIdentifier(context, name);
            if (identifier == 0) continue;

            try
            {
                TypedArray array = context.obtainStyledAttributes(new int[] {identifier});
                boolean present = array.hasValue(0);
                int color = array.getColor(0, fallback);
                array.recycle();

                if (present) return color;
            }
            catch (RuntimeException unresolved)
            {
                continue;
            }
        }

        return fallback;
    }

    float density()
    {
        return mDensity;
    }

    int accentColor()
    {
        return mAccentColor;
    }

    static int tintColor(ColorStateList list, int[] state, int fallback)
    {
        if (list == null) return fallback;

        return list.getColorForState(state, list.getDefaultColor());
    }

    public void setMinWidth(int minWidth)
    {
        mMinWidth = minWidth;
        requestLayout();
    }

    public int getMinWidth()
    {
        return mMinWidth;
    }

    public void setMaxWidth(int maxWidth)
    {
        mMaxWidth = maxWidth;
        requestLayout();
    }

    public int getMaxWidth()
    {
        return mMaxWidth;
    }

    public void setMinHeight(int minHeight)
    {
        mMinHeight = minHeight;
        requestLayout();
    }

    public int getMinHeight()
    {
        return mMinHeight;
    }

    public void setMaxHeight(int maxHeight)
    {
        mMaxHeight = maxHeight;
        requestLayout();
    }

    public int getMaxHeight()
    {
        return mMaxHeight;
    }

    public synchronized boolean isIndeterminate()
    {
        return mIndeterminate;
    }

    public synchronized void setIndeterminate(boolean indeterminate)
    {
        if (mOnlyIndeterminate && !indeterminate) return;
        if (mIndeterminate == indeterminate) return;

        mIndeterminate = indeterminate;

        if (indeterminate)
        {
            mCurrentDrawable = mIndeterminateDrawable;
            startAnimation();
        }
        else
        {
            mCurrentDrawable = mProgressDrawable;
            stopAnimation();
        }

        invalidate();
    }

    public Drawable getIndeterminateDrawable()
    {
        return mIndeterminateDrawable;
    }

    public void setIndeterminateDrawable(Drawable drawable)
    {
        if (mIndeterminateDrawable == drawable) return;

        boolean animating = mAnimating;
        if (animating) stopAnimation();

        if (mIndeterminateDrawable != null) mIndeterminateDrawable.setCallback(null);

        mIndeterminateDrawable = drawable;
        attachDrawable(drawable);
        applyIndeterminateTint();

        if (mIndeterminate) mCurrentDrawable = drawable;
        if (animating) startAnimation();

        requestLayout();
        invalidate();
    }

    private void attachDrawable(Drawable drawable)
    {
        if (drawable == null) return;

        drawable.setCallback(this);
        if (drawable.isStateful()) drawable.setState(getDrawableState());
    }

    public void setIndeterminateTintList(ColorStateList tint)
    {
        mIndeterminateTintList = tint;
        applyIndeterminateTint();
        invalidate();
    }

    public ColorStateList getIndeterminateTintList()
    {
        return mIndeterminateTintList;
    }

    public void setIndeterminateTintMode(PorterDuff.Mode tintMode)
    {
        mIndeterminateTintMode = tintMode;
    }

    public void setIndeterminateTintBlendMode(BlendMode blendMode)
    {
        mIndeterminateTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getIndeterminateTintMode()
    {
        return mIndeterminateTintMode;
    }

    public BlendMode getIndeterminateTintBlendMode()
    {
        return mIndeterminateTintBlendMode;
    }

    public void setIndeterminateDrawableTiled(Drawable drawable)
    {
        setIndeterminateDrawable(drawable);
    }

    public Drawable getProgressDrawable()
    {
        return mProgressDrawable;
    }

    public void setProgressDrawable(Drawable drawable)
    {
        if (mProgressDrawable == drawable) return;

        if (mProgressDrawable != null) mProgressDrawable.setCallback(null);

        mProgressDrawable = drawable;
        attachDrawable(drawable);
        applyProgressTints();

        if (drawable != null)
        {
            int drawableHeight = drawable.getMinimumHeight();
            if (mMaxHeight < drawableHeight) mMaxHeight = drawableHeight;
        }

        if (!mIndeterminate) mCurrentDrawable = drawable;

        requestLayout();
        invalidate();
    }

    public void setProgressTintList(ColorStateList tint)
    {
        mProgressTintList = tint;
        applyProgressTints();
        invalidate();
    }

    public ColorStateList getProgressTintList()
    {
        return mProgressTintList;
    }

    public void setProgressTintMode(PorterDuff.Mode tintMode)
    {
        mProgressTintMode = tintMode;
    }

    public void setProgressTintBlendMode(BlendMode blendMode)
    {
        mProgressTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getProgressTintMode()
    {
        return mProgressTintMode;
    }

    public BlendMode getProgressTintBlendMode()
    {
        return mProgressTintBlendMode;
    }

    public void setProgressBackgroundTintList(ColorStateList tint)
    {
        mProgressBackgroundTintList = tint;
        applyProgressTints();
        invalidate();
    }

    public ColorStateList getProgressBackgroundTintList()
    {
        return mProgressBackgroundTintList;
    }

    public void setProgressBackgroundTintMode(PorterDuff.Mode tintMode)
    {
        mProgressBackgroundTintMode = tintMode;
    }

    public void setProgressBackgroundTintBlendMode(BlendMode blendMode)
    {
        mProgressBackgroundTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getProgressBackgroundTintMode()
    {
        return mProgressBackgroundTintMode;
    }

    public BlendMode getProgressBackgroundTintBlendMode()
    {
        return mProgressBackgroundTintBlendMode;
    }

    public void setSecondaryProgressTintList(ColorStateList tint)
    {
        mSecondaryProgressTintList = tint;
        applyProgressTints();
        invalidate();
    }

    public ColorStateList getSecondaryProgressTintList()
    {
        return mSecondaryProgressTintList;
    }

    public void setSecondaryProgressTintMode(PorterDuff.Mode tintMode)
    {
        mSecondaryProgressTintMode = tintMode;
    }

    public void setSecondaryProgressTintBlendMode(BlendMode blendMode)
    {
        mSecondaryProgressTintBlendMode = blendMode;
    }

    public PorterDuff.Mode getSecondaryProgressTintMode()
    {
        return mSecondaryProgressTintMode;
    }

    public BlendMode getSecondaryProgressTintBlendMode()
    {
        return mSecondaryProgressTintBlendMode;
    }

    public void setProgressDrawableTiled(Drawable drawable)
    {
        setProgressDrawable(drawable);
    }

    public Drawable getCurrentDrawable()
    {
        return mCurrentDrawable;
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return who == mProgressDrawable || who == mIndeterminateDrawable || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (mProgressDrawable != null) mProgressDrawable.jumpToCurrentState();
        if (mIndeterminateDrawable != null) mIndeterminateDrawable.jumpToCurrentState();
    }

    @Override
    public void postInvalidate()
    {
        super.postInvalidate();
    }

    @Override
    public void setStateDescription(CharSequence stateDescription)
    {
        super.setStateDescription(stateDescription);
    }

    public synchronized void setProgress(int progress)
    {
        setProgressInternal(progress, false, false);
    }

    public void setProgress(int progress, boolean animate)
    {
        setProgressInternal(progress, false, animate);
    }

    synchronized boolean setProgressInternal(int progress, boolean fromUser, boolean animate)
    {
        if (mIndeterminate) return false;

        progress = Math.max(mMin, Math.min(mMax, progress));
        if (progress == mProgress) return false;

        mProgress = progress;
        invalidate();
        onProgressRefresh(getScale(progress), fromUser, progress);

        return true;
    }

    void onProgressRefresh(float scale, boolean fromUser, int progress)
    {
    }

    float getScale(int progress)
    {
        int range = mMax - mMin;
        return range > 0 ? (progress - mMin) / (float)range : 0f;
    }

    public synchronized void setSecondaryProgress(int secondaryProgress)
    {
        if (mIndeterminate) return;

        secondaryProgress = Math.max(mMin, Math.min(mMax, secondaryProgress));
        if (secondaryProgress == mSecondaryProgress) return;

        mSecondaryProgress = secondaryProgress;
        invalidate();
    }

    public synchronized int getProgress()
    {
        return mIndeterminate ? 0 : mProgress;
    }

    public synchronized int getSecondaryProgress()
    {
        return mIndeterminate ? 0 : mSecondaryProgress;
    }

    public synchronized int getMin()
    {
        return mMin;
    }

    public synchronized int getMax()
    {
        return mMax;
    }

    public synchronized void setMin(int min)
    {
        if (mMaxInitialised && min > mMax) min = mMax;

        mMinInitialised = true;
        if (mMin == min) return;

        mMin = min;
        if (mProgress < min) mProgress = min;
        if (mSecondaryProgress < min) mSecondaryProgress = min;

        invalidate();
        onProgressRefresh(getScale(mProgress), false, mProgress);
    }

    public synchronized void setMax(int max)
    {
        if (mMinInitialised && max < mMin) max = mMin;

        mMaxInitialised = true;
        if (mMax == max) return;

        mMax = max;
        if (mProgress > max) mProgress = max;
        if (mSecondaryProgress > max) mSecondaryProgress = max;

        invalidate();
        onProgressRefresh(getScale(mProgress), false, mProgress);
    }

    public final synchronized void incrementProgressBy(int diff)
    {
        setProgress(mProgress + diff);
    }

    public final synchronized void incrementSecondaryProgressBy(int diff)
    {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    public void setInterpolator(Context context, int resourceId)
    {
        try
        {
            setInterpolator(AnimationUtils.loadInterpolator(context, resourceId));
        }
        catch (RuntimeException unsupported)
        {
            setInterpolator(new LinearInterpolator());
        }
    }

    public void setInterpolator(Interpolator interpolator)
    {
        mInterpolator = interpolator == null ? new LinearInterpolator() : interpolator;
    }

    public Interpolator getInterpolator()
    {
        return mInterpolator;
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible)
    {
        super.onVisibilityAggregated(isVisible);

        mAggregatedVisible = isVisible;

        if (mIndeterminate && mAggregatedVisible && isAttachedToWindow()) startAnimation();
        else stopAnimation();
    }

    void startAnimation()
    {
        if (!mIndeterminate || mAnimating || getVisibility() != VISIBLE) return;

        mAnimating = true;
        mAnimationStart = SystemClock.uptimeMillis();

        removeCallbacks(mAnimationTick);

        if (mIndeterminateDrawable instanceof Animatable)
        {
            ((Animatable)mIndeterminateDrawable).start();
            return;
        }

        postDelayed(mAnimationTick, FRAME_DELAY);
    }

    void stopAnimation()
    {
        if (!mAnimating) return;

        mAnimating = false;
        removeCallbacks(mAnimationTick);

        if (mIndeterminateDrawable instanceof Animatable) ((Animatable)mIndeterminateDrawable).stop();
    }

    @Override
    public void invalidateDrawable(Drawable drawable)
    {
        if (!verifyDrawable(drawable))
        {
            super.invalidateDrawable(drawable);
            return;
        }

        android.graphics.Rect dirty = drawable.getBounds();
        int scrollX = getScrollX() + getPaddingLeft();
        int scrollY = getScrollY() + getPaddingTop();

        invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int bottom = getHeight() - getPaddingBottom();

        if (right <= left || bottom <= top) return;

        if (mIndeterminate) drawIndeterminate(canvas, left, top, right, bottom);
        else drawTrack(canvas, left, top, right, bottom);

        if (mIndeterminate && !mAnimating && isAttachedToWindow()) startAnimation();
    }

    void drawTrack(Canvas canvas, int left, int top, int right, int bottom)
    {
        float primary = getScale(mProgress);
        float secondary = getScale(mSecondaryProgress);

        if (mProgressDrawable != null)
        {
            applyVisualProgress(LAYER_SECONDARY_PROGRESS, secondary);
            applyVisualProgress(LAYER_PROGRESS, primary);

            mProgressDrawable.setBounds(left, top, right, bottom);
            mProgressDrawable.draw(canvas);
            return;
        }

        int[] state = getDrawableState();
        int trackColor = tintColor(mProgressBackgroundTintList, state, (mAccentColor & 0x00FFFFFF) | 0x4C000000);
        int secondaryColor = tintColor(mSecondaryProgressTintList, state, (mAccentColor & 0x00FFFFFF) | 0x80000000);
        int progressColor = tintColor(mProgressTintList, state, mAccentColor);

        drawBar(canvas, left, top, right, bottom, 1f, trackColor);
        drawBar(canvas, left, top, right, bottom, secondary, secondaryColor);
        drawBar(canvas, left, top, right, bottom, primary, progressColor);
    }

    private void applyVisualProgress(int layerId, float scale)
    {
        Drawable target = mProgressDrawable;

        if (target instanceof LayerDrawable)
        {
            Drawable layer = ((LayerDrawable)target).findDrawableByLayerId(layerId);
            target = layer != null ? layer : layerId == LAYER_PROGRESS ? target : null;
        }
        else if (layerId != LAYER_PROGRESS)
        {
            target = null;
        }

        if (target != null) target.setLevel((int)(scale * 10000));
    }

    private void applyProgressTints()
    {
        if (mProgressDrawable == null) return;

        if (mProgressTintList != null) tintLayer(LAYER_PROGRESS, mProgressTintList, true);
        if (mProgressBackgroundTintList != null) tintLayer(LAYER_BACKGROUND, mProgressBackgroundTintList, false);
        if (mSecondaryProgressTintList != null) tintLayer(LAYER_SECONDARY_PROGRESS, mSecondaryProgressTintList, false);
    }

    private void tintLayer(int layerId, ColorStateList tint, boolean fallbackToRoot)
    {
        Drawable target = null;

        if (mProgressDrawable instanceof LayerDrawable) target = ((LayerDrawable)mProgressDrawable).findDrawableByLayerId(layerId);
        if (target == null && fallbackToRoot) target = mProgressDrawable;

        applyTint(target, tint, getDrawableState());
    }

    private void applyIndeterminateTint()
    {
        applyTint(mIndeterminateDrawable, mIndeterminateTintList, getDrawableState());
    }

    static void applyTint(Drawable drawable, ColorStateList tint, int[] state)
    {
        if (drawable == null || tint == null) return;

        drawable.mutate();
        drawable.setTintList(tint);
        if (drawable.isStateful()) drawable.setState(state);
    }

    private void drawBar(Canvas canvas, int left, int top, int right, int bottom, float scale, int color)
    {
        if (scale <= 0f) return;

        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        float extent = (right - left) * Math.min(1f, scale);
        float clipLeft = rightToLeft ? right - extent : left;
        float clipRight = rightToLeft ? right : left + extent;

        float thickness = Math.min(bottom - top, 4 * mDensity);
        float centre = (top + bottom) / 2f;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
        canvas.drawRect(clipLeft, centre - thickness / 2f, clipRight, centre + thickness / 2f, mPaint);
    }

    private void drawIndeterminate(Canvas canvas, int left, int top, int right, int bottom)
    {
        long elapsed = SystemClock.uptimeMillis() - mAnimationStart;
        int width = right - left;
        int height = bottom - top;

        if (mIndeterminateDrawable != null)
        {
            Drawable drawable = mIndeterminateDrawable;
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();

            if (mOnlyIndeterminate && intrinsicWidth > 0 && intrinsicHeight > 0)
            {
                float intrinsicAspect = (float)intrinsicWidth / intrinsicHeight;
                float boundAspect = (float)width / height;

                if (intrinsicAspect > boundAspect)
                {
                    int scaledHeight = (int)(width / intrinsicAspect);
                    top += (height - scaledHeight) / 2;
                    bottom = top + scaledHeight;
                }
                else if (intrinsicAspect < boundAspect)
                {
                    int scaledWidth = (int)(height * intrinsicAspect);
                    left += (width - scaledWidth) / 2;
                    right = left + scaledWidth;
                }
            }

            if (!(drawable instanceof Animatable)) drawable.setLevel((int)((elapsed % SLIDE_DURATION) * 10000 / SLIDE_DURATION));

            drawable.setBounds(left, top, right, bottom);
            drawable.draw(canvas);
            return;
        }

        int color = tintColor(mIndeterminateTintList, getDrawableState(), mAccentColor);

        if (width > height * 3)
        {
            float phase = mInterpolator.getInterpolation((elapsed % SLIDE_DURATION) / (float)SLIDE_DURATION);
            float thickness = Math.min(height, 4 * mDensity);
            float centre = (top + bottom) / 2f;
            float segment = width * 0.4f;
            float start = left - segment + (width + segment) * phase;

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor((color & 0x00FFFFFF) | 0x4C000000);
            canvas.drawRect(left, centre - thickness / 2f, right, centre + thickness / 2f, mPaint);

            mPaint.setColor(color);
            canvas.drawRect(Math.max(left, start), centre - thickness / 2f, Math.min(right, start + segment), centre + thickness / 2f,
                            mPaint);
            return;
        }

        float rotation = (elapsed % ROTATION_DURATION) * 360f / ROTATION_DURATION;
        int size = Math.min(width, height);
        float centreX = (left + right) / 2f;
        float centreY = (top + bottom) / 2f;

        float stroke = Math.max(2f, size / 12f);
        float radius = size / 2f - stroke;
        float cycle = (elapsed % (ROTATION_DURATION * 2)) / (float)ROTATION_DURATION;
        float sweep = cycle < 1f ? 20f + 250f * cycle : 270f - 250f * (cycle - 1f);
        float start = rotation * 2f + (cycle < 1f ? 0f : 250f * (cycle - 1f));

        mArcBounds.set(centreX - radius, centreY - radius, centreX + radius, centreY + radius);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(stroke);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(color);
        canvas.drawArc(mArcBounds, start - 90f, sweep, false, mPaint);
    }

    int intrinsicContentWidth()
    {
        Drawable drawable = mCurrentDrawable;
        if (drawable != null && drawable.getIntrinsicWidth() > 0) return drawable.getIntrinsicWidth();

        return mIndeterminate ? (int)(48 * mDensity) : 0;
    }

    int intrinsicContentHeight()
    {
        Drawable drawable = mCurrentDrawable;
        if (drawable != null && drawable.getIntrinsicHeight() > 0) return drawable.getIntrinsicHeight();

        return mIndeterminate ? (int)(48 * mDensity) : (int)(4 * mDensity);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int contentWidth = Math.max(mMinWidth, Math.min(mMaxWidth, intrinsicContentWidth()));
        int contentHeight = Math.max(mMinHeight, Math.min(mMaxHeight, intrinsicContentHeight()));

        int width = contentWidth + getPaddingLeft() + getPaddingRight();
        int height = contentHeight + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        int[] state = getDrawableState();

        if (mProgressDrawable != null && mProgressDrawable.isStateful()) mProgressDrawable.setState(state);
        if (mIndeterminateDrawable != null && mIndeterminateDrawable.isStateful()) mIndeterminateDrawable.setState(state);

        invalidate();
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        if (mProgressDrawable != null) mProgressDrawable.setHotspot(x, y);
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mProgress = mProgress;
        state.mSecondaryProgress = mSecondaryProgress;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        if (!(state instanceof SavedState))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState saved = (SavedState)state;
        super.onRestoreInstanceState(saved.getSuperState());

        setProgress(saved.mProgress);
        setSecondaryProgress(saved.mSecondaryProgress);
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

        int mProgress;
        int mSecondaryProgress;

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            mProgress = source.readInt();
            mSecondaryProgress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(mProgress);
            out.writeInt(mSecondaryProgress);
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mIndeterminate) startAnimation();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        stopAnimation();

        super.onDetachedFromWindow();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ProgressBar.class.getName();
    }

    public boolean isAnimating()
    {
        return mAnimating && isIndeterminate() && getWindowVisibility() == VISIBLE && isShown();
    }
}

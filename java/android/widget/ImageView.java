package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;

public class ImageView extends View
{
    public enum ScaleType
    {
        MATRIX,
        FIT_XY,
        FIT_START,
        FIT_CENTER,
        FIT_END,
        CENTER,
        CENTER_CROP,
        CENTER_INSIDE
    }

    private static final ScaleType[] SCALE_TYPES = ScaleType.values();

    private static StyleableSet sStyleable;

    private Drawable mDrawable;
    private int mResource;
    private Uri mUri;
    private int mDrawableWidth = -1;
    private int mDrawableHeight = -1;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;
    private boolean mAdjustViewBounds;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    private final Matrix mMatrix = new Matrix();
    private Matrix mDrawMatrix;
    private final RectF mTempSource = new RectF();
    private final RectF mTempDestination = new RectF();
    private boolean mHaveFrame;
    private boolean mCropToPadding;
    private int mBaseline = -1;
    private boolean mBaselineAlignBottom;
    private int mLevel;
    private int[] mState;
    private boolean mMergeState;
    private ColorFilter mColorFilter;
    private boolean mHasColorFilter;
    private int mAlpha = 255;
    private boolean mHasAlpha;
    private ColorStateList mDrawableTintList;
    private boolean mHasDrawableTint;
    private BlendMode mDrawableBlendMode;
    private PorterDuff.Mode mDrawableTintMode;
    private boolean mHasDrawableBlendMode;

    public ImageView(Context context)
    {
        this(context, null);
    }

    public ImageView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public ImageView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ImageView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes == null && defStyleAttr == 0 && defStyleRes == 0) return;

        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "src", "scaleType", "adjustViewBounds", "maxWidth", "maxHeight", "baseline",
                                         "baselineAlignBottom", "cropToPadding", "tint", "tintMode", "drawableAlpha");
        }

        if (!sStyleable.isEmpty()) readAttributes(context, attributes, defStyleAttr, defStyleRes);

        if (mDrawable == null && attributes != null) readCompatSource(context, attributes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int sourceIndex = sStyleable.indexOf("src");
        if (sourceIndex >= 0)
        {
            int resource = array.getResourceId(sourceIndex, 0);
            if (resource != 0) setImageDrawable(resolveDrawable(resource));
        }

        int index = sStyleable.indexOf("baselineAlignBottom");
        if (index >= 0) mBaselineAlignBottom = array.getBoolean(index, false);

        index = sStyleable.indexOf("baseline");
        if (index >= 0) mBaseline = array.getDimensionPixelSize(index, -1);

        index = sStyleable.indexOf("adjustViewBounds");
        if (index >= 0) setAdjustViewBounds(array.getBoolean(index, false));

        index = sStyleable.indexOf("maxWidth");
        if (index >= 0) setMaxWidth(array.getDimensionPixelSize(index, Integer.MAX_VALUE));

        index = sStyleable.indexOf("maxHeight");
        if (index >= 0) setMaxHeight(array.getDimensionPixelSize(index, Integer.MAX_VALUE));

        index = sStyleable.indexOf("scaleType");
        if (index >= 0)
        {
            int scaleType = array.getInt(index, -1);
            if (scaleType >= 0 && scaleType < SCALE_TYPES.length) setScaleType(SCALE_TYPES[scaleType]);
        }

        index = sStyleable.indexOf("tint");
        if (index >= 0 && array.hasValue(index))
        {
            mDrawableTintList = array.getColorStateList(index);
            mHasDrawableTint = true;
            mDrawableTintMode = PorterDuff.Mode.SRC_ATOP;
            mHasDrawableBlendMode = true;
        }

        index = sStyleable.indexOf("tintMode");
        if (index >= 0 && array.hasValue(index))
        {
            PorterDuff.Mode mode = parseTintMode(array.getInt(index, -1));
            if (mode != null)
            {
                mDrawableTintMode = mode;
                mHasDrawableBlendMode = true;
            }
        }

        applyImageTint();

        index = sStyleable.indexOf("drawableAlpha");
        if (index >= 0 && array.hasValue(index)) setImageAlpha(array.getInt(index, 255));

        index = sStyleable.indexOf("cropToPadding");
        if (index >= 0) mCropToPadding = array.getBoolean(index, false);

        array.recycle();
    }

    private static PorterDuff.Mode parseTintMode(int value)
    {
        switch (value)
        {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return null;
        }
    }

    private void readCompatSource(Context context, AttributeSet attributes)
    {
        int attribute = context.getResources().getIdentifier("srcCompat", "attr", context.getPackageName());
        if (attribute == 0) return;

        TypedArray array = context.obtainStyledAttributes(attributes, new int[] {attribute}, 0, 0);
        int resource = array.getResourceId(0, 0);
        array.recycle();

        if (resource != 0) setImageDrawable(resolveDrawable(resource));
    }

    private Drawable resolveDrawable(int resourceId)
    {
        try
        {
            return getContext().getResources().getDrawable(resourceId, getContext().getTheme());
        }
        catch (Throwable missing)
        {
            android.util.Log.w("ImageView", "drawable 0x" + Integer.toHexString(resourceId) + " did not load", missing);
            return null;
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return mDrawable == who || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (mDrawable != null) mDrawable.jumpToCurrentState();
    }

    @Override
    public void invalidateDrawable(Drawable who)
    {
        if (who != mDrawable)
        {
            super.invalidateDrawable(who);
            return;
        }

        if (who != null)
        {
            int width = who.getIntrinsicWidth();
            int height = who.getIntrinsicHeight();

            if (width != mDrawableWidth || height != mDrawableHeight)
            {
                mDrawableWidth = width;
                mDrawableHeight = height;
                configureBounds();
            }
        }

        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering()
    {
        Drawable background = getBackground();
        return background != null && background.getCurrent() != null;
    }

    public boolean getAdjustViewBounds()
    {
        return mAdjustViewBounds;
    }

    public void setAdjustViewBounds(boolean adjustViewBounds)
    {
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) setScaleType(ScaleType.FIT_CENTER);
        requestLayout();
    }

    public int getMaxWidth()
    {
        return mMaxWidth;
    }

    public void setMaxWidth(int maxWidth)
    {
        mMaxWidth = maxWidth;
    }

    public int getMaxHeight()
    {
        return mMaxHeight;
    }

    public void setMaxHeight(int maxHeight)
    {
        mMaxHeight = maxHeight;
    }

    public Drawable getDrawable()
    {
        if (mDrawable == null && mResource != 0) resolveUri();

        return mDrawable;
    }

    public void setImageResource(int resId)
    {
        int oldWidth = mDrawableWidth;
        int oldHeight = mDrawableHeight;

        updateDrawable(null);
        mResource = resId;
        mUri = null;

        resolveUri();

        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) requestLayout();
        invalidate();
    }

    public void setImageURI(Uri uri)
    {
        if (mResource == 0 && (mUri == uri || (uri != null && uri.equals(mUri)))) return;

        updateDrawable(null);
        mResource = 0;
        mUri = uri;

        int oldWidth = mDrawableWidth;
        int oldHeight = mDrawableHeight;

        resolveUri();

        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) requestLayout();
        invalidate();
    }

    public void setImageDrawable(Drawable drawable)
    {
        if (mDrawable == drawable) return;

        mResource = 0;
        mUri = null;

        int oldWidth = mDrawableWidth;
        int oldHeight = mDrawableHeight;

        updateDrawable(drawable);

        if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) requestLayout();
        invalidate();
    }

    public void setImageIcon(Icon icon)
    {
        setImageDrawable(icon == null ? null : icon.loadDrawable(getContext()));
    }

    public void setImageTintList(ColorStateList tint)
    {
        mDrawableTintList = tint;
        mHasDrawableTint = true;
        applyImageTint();
    }

    public ColorStateList getImageTintList()
    {
        return mDrawableTintList;
    }

    public void setImageTintMode(PorterDuff.Mode tintMode)
    {
        mDrawableTintMode = tintMode;
        mDrawableBlendMode = null;
        mHasDrawableBlendMode = true;
        applyImageTint();
    }

    public void setImageTintBlendMode(BlendMode blendMode)
    {
        mDrawableBlendMode = blendMode;
        mDrawableTintMode = null;
        mHasDrawableBlendMode = true;
        applyImageTint();
    }

    public PorterDuff.Mode getImageTintMode()
    {
        return mDrawableTintMode;
    }

    public BlendMode getImageTintBlendMode()
    {
        return mDrawableBlendMode;
    }

    private void applyImageTint()
    {
        if (mDrawable == null || (!mHasDrawableTint && !mHasDrawableBlendMode)) return;

        replaceWithMutated();

        if (mHasDrawableTint) mDrawable.setTintList(mDrawableTintList);
        if (mHasDrawableBlendMode && mDrawableBlendMode != null) mDrawable.setTintBlendMode(mDrawableBlendMode);
        else if (mHasDrawableBlendMode) mDrawable.setTintMode(mDrawableTintMode);
        if (mDrawable.isStateful()) mDrawable.setState(getDrawableState());
    }

    private void replaceWithMutated()
    {
        Drawable mutated = mDrawable.mutate();
        if (mutated == mDrawable) return;

        mDrawable.setCallback(null);
        mDrawable = mutated;
        mutated.setCallback(this);
    }

    public void setImageBitmap(Bitmap bitmap)
    {
        setImageDrawable(bitmap == null ? null : new BitmapDrawable(getContext().getResources(), bitmap));
    }

    public void setImageState(int[] state, boolean merge)
    {
        mState = state;
        mMergeState = merge;

        if (mDrawable == null) return;

        refreshDrawableState();
        resizeFromDrawable();
    }

    @Override
    public void setSelected(boolean selected)
    {
        super.setSelected(selected);

        resizeFromDrawable();
    }

    public void setImageLevel(int level)
    {
        mLevel = level;

        if (mDrawable == null) return;

        mDrawable.setLevel(level);
        resizeFromDrawable();
    }

    public void setScaleType(ScaleType scaleType)
    {
        if (scaleType == null) throw new NullPointerException();
        if (mScaleType == scaleType) return;

        mScaleType = scaleType;
        requestLayout();
        invalidate();
    }

    public ScaleType getScaleType()
    {
        return mScaleType;
    }

    public Matrix getImageMatrix()
    {
        if (mDrawMatrix == null) return new Matrix(Matrix.IDENTITY_MATRIX);

        return mDrawMatrix;
    }

    public void setImageMatrix(Matrix matrix)
    {
        if (matrix != null && matrix.isIdentity()) matrix = null;

        if ((matrix == null && !mMatrix.isIdentity()) || (matrix != null && !mMatrix.equals(matrix)))
        {
            if (matrix == null) mMatrix.reset();
            else mMatrix.set(matrix);

            configureBounds();
            invalidate();
        }
    }

    public boolean getCropToPadding()
    {
        return mCropToPadding;
    }

    public void setCropToPadding(boolean cropToPadding)
    {
        if (mCropToPadding == cropToPadding) return;

        mCropToPadding = cropToPadding;
        requestLayout();
        invalidate();
    }

    private void resolveUri()
    {
        if (mDrawable != null) return;
        if (getResources() == null) return;

        Drawable drawable = null;

        if (mResource != 0)
        {
            drawable = resolveDrawable(mResource);
            if (drawable == null) mUri = null;
        }
        else if (mUri != null)
        {
            drawable = drawableFromUri(mUri);
            if (drawable == null) mUri = null;
        }
        else
        {
            return;
        }

        updateDrawable(drawable);
    }

    private Drawable drawableFromUri(Uri uri)
    {
        String scheme = uri.getScheme();

        try
        {
            if (android.content.ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme))
            {
                int identifier = resourceIdentifier(uri);
                return identifier == 0 ? null : resolveDrawable(identifier);
            }

            if (android.content.ContentResolver.SCHEME_CONTENT.equals(scheme)
                || android.content.ContentResolver.SCHEME_FILE.equals(scheme))
            {
                try (java.io.InputStream stream = getContext().getContentResolver().openInputStream(uri))
                {
                    return Drawable.createFromStream(stream, null);
                }
            }

            return Drawable.createFromPath(uri.toString());
        }
        catch (Exception failure)
        {
            android.util.Log.w("ImageView", "unable to open content: " + uri, failure);
            return null;
        }
    }

    private int resourceIdentifier(Uri uri)
    {
        java.util.List<String> segments = uri.getPathSegments();
        String authority = uri.getAuthority();
        if (segments == null || segments.isEmpty()) return 0;

        if (segments.size() == 1)
        {
            try
            {
                return Integer.parseInt(segments.get(0));
            }
            catch (NumberFormatException malformed)
            {
                return 0;
            }
        }

        return getResources().getIdentifier(segments.get(1), segments.get(0), authority);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace)
    {
        if (mState == null) return super.onCreateDrawableState(extraSpace);
        if (!mMergeState) return mState;

        return mergeDrawableStates(super.onCreateDrawableState(extraSpace + mState.length), mState);
    }

    private void updateDrawable(Drawable drawable)
    {
        boolean sameDrawable = false;

        if (mDrawable != null)
        {
            sameDrawable = mDrawable == drawable;
            mDrawable.setCallback(null);
            unscheduleDrawable(mDrawable);
            if (!sameDrawable && isAttachedToWindow()) mDrawable.setVisible(false, false);
        }

        mDrawable = drawable;

        if (drawable == null)
        {
            mDrawableWidth = -1;
            mDrawableHeight = -1;
            return;
        }

        drawable.setCallback(this);
        drawable.setLayoutDirection(getLayoutDirection());
        if (drawable.isStateful()) drawable.setState(getDrawableState());

        if (!sameDrawable)
        {
            boolean visible = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
            drawable.setVisible(visible, true);
        }

        drawable.setLevel(mLevel);
        mDrawableWidth = drawable.getIntrinsicWidth();
        mDrawableHeight = drawable.getIntrinsicHeight();

        applyImageTint();
        applyColorFilter();
        applyAlpha();

        configureBounds();
    }

    private void resizeFromDrawable()
    {
        Drawable drawable = mDrawable;
        if (drawable == null) return;

        int width = drawable.getIntrinsicWidth();
        if (width < 0) width = mDrawableWidth;
        int height = drawable.getIntrinsicHeight();
        if (height < 0) height = mDrawableHeight;

        if (width == mDrawableWidth && height == mDrawableHeight) return;

        mDrawableWidth = width;
        mDrawableHeight = height;
        configureBounds();
        requestLayout();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mDrawable != null) mDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        resolveUri();

        int width;
        int height;
        float desiredAspect = 0.0f;
        boolean resizeWidth = false;
        boolean resizeHeight = false;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (mDrawable == null)
        {
            mDrawableWidth = -1;
            mDrawableHeight = -1;
            width = 0;
            height = 0;
        }
        else
        {
            width = Math.max(mDrawableWidth, 1);
            height = Math.max(mDrawableHeight, 1);

            if (mAdjustViewBounds)
            {
                resizeWidth = widthMode != MeasureSpec.EXACTLY;
                resizeHeight = heightMode != MeasureSpec.EXACTLY;
                desiredAspect = (float)width / (float)height;
            }
        }

        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight)
        {
            widthSize = resolveAdjustedSize(width + paddingLeft + paddingRight, mMaxWidth, widthMeasureSpec);
            heightSize = resolveAdjustedSize(height + paddingTop + paddingBottom, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f)
            {
                float actualAspect = (float)(widthSize - paddingLeft - paddingRight) / (heightSize - paddingTop - paddingBottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001)
                {
                    boolean done = false;

                    if (resizeWidth)
                    {
                        int newWidth = (int)(desiredAspect * (heightSize - paddingTop - paddingBottom)) + paddingLeft + paddingRight;
                        if (!resizeHeight) widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec);

                        if (newWidth <= widthSize)
                        {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    if (!done && resizeHeight)
                    {
                        int newHeight = (int)((widthSize - paddingLeft - paddingRight) / desiredAspect) + paddingTop + paddingBottom;
                        if (!resizeWidth) heightSize = resolveAdjustedSize(newHeight, mMaxHeight, heightMeasureSpec);

                        if (newHeight <= heightSize) heightSize = newHeight;
                    }
                }
            }
        }
        else
        {
            width = Math.max(width + paddingLeft + paddingRight, getSuggestedMinimumWidth());
            height = Math.max(height + paddingTop + paddingBottom, getSuggestedMinimumHeight());

            widthSize = resolveSizeAndState(width, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(height, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int maxSize, int measureSpec)
    {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) return Math.min(desiredSize, maxSize);
        if (specMode == MeasureSpec.AT_MOST) return Math.min(Math.min(desiredSize, specSize), maxSize);

        return specSize;
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom)
    {
        boolean changed = super.setFrame(left, top, right, bottom);
        mHaveFrame = true;
        configureBounds();
        return changed;
    }

    private void configureBounds()
    {
        if (mDrawable == null || !mHaveFrame) return;

        int drawableWidth = mDrawableWidth;
        int drawableHeight = mDrawableHeight;

        int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        boolean fits = (drawableWidth < 0 || viewWidth == drawableWidth) && (drawableHeight < 0 || viewHeight == drawableHeight);

        if (drawableWidth <= 0 || drawableHeight <= 0 || mScaleType == ScaleType.FIT_XY)
        {
            mDrawable.setBounds(0, 0, viewWidth, viewHeight);
            mDrawMatrix = null;
            return;
        }

        mDrawable.setBounds(0, 0, drawableWidth, drawableHeight);

        if (mScaleType == ScaleType.MATRIX)
        {
            mDrawMatrix = mMatrix.isIdentity() ? null : mMatrix;
        }
        else if (fits)
        {
            mDrawMatrix = null;
        }
        else if (mScaleType == ScaleType.CENTER)
        {
            mDrawMatrix = mMatrix;
            mDrawMatrix.setTranslate(Math.round((viewWidth - drawableWidth) * 0.5f), Math.round((viewHeight - drawableHeight) * 0.5f));
        }
        else if (mScaleType == ScaleType.CENTER_CROP)
        {
            mDrawMatrix = mMatrix;

            float scale;
            float offsetX = 0;
            float offsetY = 0;

            if (drawableWidth * viewHeight > viewWidth * drawableHeight)
            {
                scale = (float)viewHeight / (float)drawableHeight;
                offsetX = (viewWidth - drawableWidth * scale) * 0.5f;
            }
            else
            {
                scale = (float)viewWidth / (float)drawableWidth;
                offsetY = (viewHeight - drawableHeight * scale) * 0.5f;
            }

            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(Math.round(offsetX), Math.round(offsetY));
        }
        else if (mScaleType == ScaleType.CENTER_INSIDE)
        {
            mDrawMatrix = mMatrix;

            float scale = 1.0f;
            if (drawableWidth > viewWidth || drawableHeight > viewHeight)
            {
                scale = Math.min((float)viewWidth / (float)drawableWidth, (float)viewHeight / (float)drawableHeight);
            }

            float offsetX = Math.round((viewWidth - drawableWidth * scale) * 0.5f);
            float offsetY = Math.round((viewHeight - drawableHeight * scale) * 0.5f);

            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(offsetX, offsetY);
        }
        else
        {
            mTempSource.set(0, 0, drawableWidth, drawableHeight);
            mTempDestination.set(0, 0, viewWidth, viewHeight);

            mDrawMatrix = mMatrix;
            mDrawMatrix.setRectToRect(mTempSource, mTempDestination, scaleToFit(mScaleType));
        }
    }

    private static Matrix.ScaleToFit scaleToFit(ScaleType scaleType)
    {
        switch (scaleType)
        {
            case FIT_START:
                return Matrix.ScaleToFit.START;
            case FIT_END:
                return Matrix.ScaleToFit.END;
            case FIT_XY:
                return Matrix.ScaleToFit.FILL;
            default:
                return Matrix.ScaleToFit.CENTER;
        }
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        Drawable drawable = mDrawable;
        if (drawable != null && drawable.isStateful() && drawable.setState(getDrawableState())) invalidateDrawable(drawable);
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        if (mDrawable != null) mDrawable.setHotspot(x, y);
    }

    public void animateTransform(Matrix matrix)
    {
        if (mDrawable == null) return;

        if (matrix == null)
        {
            int viewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
            mDrawable.setBounds(0, 0, viewWidth, viewHeight);
            mDrawMatrix = null;
        }
        else
        {
            mDrawable.setBounds(0, 0, mDrawableWidth, mDrawableHeight);
            if (mDrawMatrix == null) mDrawMatrix = new Matrix();
            mDrawMatrix.set(matrix);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (mDrawable == null) return;
        if (mDrawableWidth == 0 || mDrawableHeight == 0) return;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mDrawMatrix == null && paddingLeft == 0 && paddingTop == 0 && !mCropToPadding)
        {
            mDrawable.draw(canvas);
            return;
        }

        int saveCount = canvas.getSaveCount();
        canvas.save();

        if (mCropToPadding)
        {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            canvas.clipRect(scrollX + paddingLeft, scrollY + paddingTop, scrollX + getWidth() - getPaddingRight(),
                            scrollY + getHeight() - getPaddingBottom());
        }

        canvas.translate(paddingLeft, paddingTop);
        if (mDrawMatrix != null) canvas.concat(mDrawMatrix);

        mDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getBaseline()
    {
        return mBaselineAlignBottom ? getMeasuredHeight() : mBaseline;
    }

    public void setBaseline(int baseline)
    {
        if (mBaseline == baseline) return;

        mBaseline = baseline;
        requestLayout();
    }

    public void setBaselineAlignBottom(boolean aligned)
    {
        if (mBaselineAlignBottom == aligned) return;

        mBaselineAlignBottom = aligned;
        requestLayout();
    }

    public boolean getBaselineAlignBottom()
    {
        return mBaselineAlignBottom;
    }

    public final void setColorFilter(int color, PorterDuff.Mode mode)
    {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public final void setColorFilter(int color)
    {
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public final void clearColorFilter()
    {
        setColorFilter(null);
    }

    public ColorFilter getColorFilter()
    {
        return mColorFilter;
    }

    public void setColorFilter(ColorFilter colorFilter)
    {
        if (mColorFilter == colorFilter && mHasColorFilter) return;

        mColorFilter = colorFilter;
        mHasColorFilter = true;
        applyColorFilter();
        invalidate();
    }

    private void applyColorFilter()
    {
        if (mDrawable == null || !mHasColorFilter) return;

        replaceWithMutated();
        mDrawable.setColorFilter(mColorFilter);
    }

    public int getImageAlpha()
    {
        return mAlpha;
    }

    public void setImageAlpha(int alpha)
    {
        setAlpha(alpha);
    }

    @Deprecated
    public void setAlpha(int alpha)
    {
        alpha &= 0xFF;
        if (mAlpha == alpha) return;

        mAlpha = alpha;
        mHasAlpha = true;
        applyAlpha();
        invalidate();
    }

    private void applyAlpha()
    {
        if (mDrawable == null || !mHasAlpha) return;

        replaceWithMutated();
        mDrawable.setAlpha(mAlpha);
    }

    @Override
    public boolean isOpaque()
    {
        if (super.isOpaque()) return true;
        if (mDrawable == null || mAlpha != 255 || getAlpha() < 1.0f) return false;
        if (mDrawable.getOpacity() != PixelFormat.OPAQUE) return false;

        return isFilledByImage();
    }

    private boolean isFilledByImage()
    {
        if (mDrawable == null) return false;

        android.graphics.Rect bounds = mDrawable.getBounds();
        Matrix matrix = mDrawMatrix;

        if (matrix == null)
        {
            return bounds.left <= 0 && bounds.top <= 0 && bounds.right >= getWidth() && bounds.bottom >= getHeight();
        }

        RectF mapped = new RectF(bounds);
        matrix.mapRect(mapped);
        return mapped.left <= 0 && mapped.top <= 0 && mapped.right >= getWidth() && mapped.bottom >= getHeight();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible)
    {
        super.onVisibilityAggregated(isVisible);

        if (mDrawable != null && mDrawable.isVisible() != isVisible) mDrawable.setVisible(isVisible, false);
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if (mDrawable != null) mDrawable.setVisible(false, false);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ImageView.class.getName();
    }
}

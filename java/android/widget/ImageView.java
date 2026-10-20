package android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

    private static StyleableSet sStyleable;

    private Drawable mDrawable;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;
    private boolean mAdjustViewBounds;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    private int mTint;
    private boolean mTinted;

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

        if (attributes == null) return;

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "src", "scaleType", "adjustViewBounds");
        if (sStyleable.isEmpty()) return;

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int sourceIndex = sStyleable.indexOf("src");
        if (sourceIndex >= 0)
        {
            int resource = array.getResourceId(sourceIndex, 0);
            if (resource != 0) applyDrawable(resolveDrawable(resource));
        }

        int boundsIndex = sStyleable.indexOf("adjustViewBounds");
        if (boundsIndex >= 0) mAdjustViewBounds = array.getBoolean(boundsIndex, false);

        array.recycle();

        if (mDrawable == null) readCompatSource(context, attributes);
    }

    private void readCompatSource(Context context, AttributeSet attributes)
    {
        int attribute = context.getResources().getIdentifier("srcCompat", "attr", context.getPackageName());
        if (attribute == 0) return;

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, new int[] {attribute}, 0, 0);
        int resource = array.getResourceId(0, 0);
        array.recycle();

        if (resource != 0) applyDrawable(resolveDrawable(resource));
    }

    public void setImageDrawable(Drawable drawable)
    {
        applyDrawable(drawable);
    }

    private void applyDrawable(Drawable drawable)
    {
        mDrawable = drawable;
        requestLayout();
        invalidate();
    }

    private Drawable resolveDrawable(int resourceId)
    {
        try
        {
            return getContext().getResources().getDrawable(resourceId);
        }
        catch (Throwable missing)
        {
            android.util.Log.w("ImageView", "drawable 0x" + Integer.toHexString(resourceId) + " did not load", missing);
            return null;
        }
    }

    public Drawable getDrawable()
    {
        return mDrawable;
    }

    public void setImageResource(int resourceId)
    {
        applyDrawable(resolveDrawable(resourceId));
    }

    public void setImageBitmap(Bitmap bitmap)
    {
        setImageDrawable(bitmap == null ? null : new BitmapDrawable(getContext().getResources(), bitmap));
    }

    public void setImageURI(android.net.Uri uri)
    {
        setImageDrawable(null);
    }

    public void setScaleType(ScaleType scaleType)
    {
        if (scaleType == null || mScaleType == scaleType) return;

        mScaleType = scaleType;
        requestLayout();
        invalidate();
    }

    public ScaleType getScaleType()
    {
        return mScaleType;
    }

    public void setAdjustViewBounds(boolean adjust)
    {
        mAdjustViewBounds = adjust;
        requestLayout();
    }

    public void setMaxWidth(int maxWidth)
    {
        mMaxWidth = maxWidth;
    }

    public void setMaxHeight(int maxHeight)
    {
        mMaxHeight = maxHeight;
    }

    public void setColorFilter(int colour)
    {
        mTint = colour;
        mTinted = true;
        invalidate();
    }

    public void setColorFilter(android.graphics.ColorFilter filter)
    {
        invalidate();
    }

    public void clearColorFilter()
    {
        mTinted = false;
        invalidate();
    }

    public void setImageLevel(int level)
    {
    }

    public void setImageState(int[] state, boolean merge)
    {
    }

    public void setImageMatrix(android.graphics.Matrix matrix)
    {
    }

    public void setCropToPadding(boolean crop)
    {
    }

    public void setBaselineAlignBottom(boolean aligned)
    {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int contentWidth = mDrawable == null ? 0 : Math.max(mDrawable.getIntrinsicWidth(), 0);
        int contentHeight = mDrawable == null ? 0 : Math.max(mDrawable.getIntrinsicHeight(), 0);

        contentWidth = Math.min(contentWidth, mMaxWidth);
        contentHeight = Math.min(contentHeight, mMaxHeight);

        int width = Math.max(contentWidth + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());
        int height = Math.max(contentHeight + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (mDrawable == null) return;

        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getWidth() - getPaddingRight();
        int bottom = getHeight() - getPaddingBottom();

        if (right <= left || bottom <= top) return;

        Rect bounds = new Rect(left, top, right, bottom);

        if (mScaleType != ScaleType.FIT_XY)
        {
            int intrinsicWidth = Math.max(mDrawable.getIntrinsicWidth(), 1);
            int intrinsicHeight = Math.max(mDrawable.getIntrinsicHeight(), 1);

            float scale = Math.min((float)(right - left) / intrinsicWidth, (float)(bottom - top) / intrinsicHeight);

            if (mScaleType == ScaleType.CENTER) scale = 1.0f;
            else if (mScaleType == ScaleType.CENTER_CROP)
                scale = Math.max((float)(right - left) / intrinsicWidth, (float)(bottom - top) / intrinsicHeight);

            int scaledWidth = Math.round(intrinsicWidth * scale);
            int scaledHeight = Math.round(intrinsicHeight * scale);

            int offsetX = left + ((right - left) - scaledWidth) / 2;
            int offsetY = top + ((bottom - top) - scaledHeight) / 2;

            bounds = new Rect(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight);
        }

        mDrawable.setBounds(bounds);
        mDrawable.draw(canvas);
    }
}

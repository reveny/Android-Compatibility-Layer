package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public abstract class DynamicDrawableSpan extends ReplacementSpan
{
    public static final int ALIGN_BOTTOM = 0;
    public static final int ALIGN_BASELINE = 1;
    public static final int ALIGN_CENTER = 2;

    protected final int mVerticalAlignment;

    protected DynamicDrawableSpan()
    {
        this(ALIGN_BOTTOM);
    }

    protected DynamicDrawableSpan(int verticalAlignment)
    {
        mVerticalAlignment = verticalAlignment;
    }

    public int getVerticalAlignment()
    {
        return mVerticalAlignment;
    }

    public abstract Drawable getDrawable();

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetrics)
    {
        Drawable drawable = getDrawable();

        if (drawable == null) return 0;

        Rect bounds = drawable.getBounds();

        if (fontMetrics != null)
        {
            fontMetrics.ascent = -bounds.bottom;
            fontMetrics.descent = 0;
            fontMetrics.top = fontMetrics.ascent;
            fontMetrics.bottom = 0;
        }

        return bounds.right;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
    {
        Drawable drawable = getDrawable();

        if (drawable == null || canvas == null) return;

        int saveCount = canvas.save();

        int translateY = bottom - drawable.getBounds().bottom;

        if (mVerticalAlignment == ALIGN_BASELINE) translateY = y - drawable.getBounds().bottom;
        else if (mVerticalAlignment == ALIGN_CENTER) translateY = top + (bottom - top) / 2 - drawable.getBounds().height() / 2;

        canvas.translate(x, translateY);
        drawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}

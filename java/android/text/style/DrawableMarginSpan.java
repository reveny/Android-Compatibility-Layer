package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spanned;

public class DrawableMarginSpan implements LeadingMarginSpan, LineHeightSpan
{
    private static final int STANDARD_PAD_WIDTH = 0;

    private final Drawable mDrawable;
    private final int mPad;

    public DrawableMarginSpan(Drawable drawable)
    {
        this(drawable, STANDARD_PAD_WIDTH);
    }

    public DrawableMarginSpan(Drawable drawable, int pad)
    {
        mDrawable = drawable;
        mPad = pad;
    }

    @Override
    public int getLeadingMargin(boolean first)
    {
        return mDrawable.getIntrinsicWidth() + mPad;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout)
    {
        int spanStart = ((Spanned)text).getSpanStart(this);
        int itemTop = layout.getLineTop(layout.getLineForOffset(spanStart));

        int width = mDrawable.getIntrinsicWidth();
        int height = mDrawable.getIntrinsicHeight();

        mDrawable.setBounds(x, itemTop, x + width, itemTop + height);
        mDrawable.draw(canvas);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fontMetrics)
    {
        if (end != ((Spanned)text).getSpanEnd(this)) return;

        int height = mDrawable.getIntrinsicHeight();
        int need = height - (spanstartv + fontMetrics.descent - fontMetrics.ascent - lineHeight);
        if (need > 0) fontMetrics.descent += need;

        need = height - (spanstartv + fontMetrics.bottom - fontMetrics.top - lineHeight);
        if (need > 0) fontMetrics.bottom += need;
    }

    @Override
    public String toString()
    {
        return "DrawableMarginSpan{drawable=" + mDrawable + ", padding=" + mPad + "}";
    }

    public Drawable getDrawable()
    {
        return mDrawable;
    }

    public int getPadding()
    {
        return mPad;
    }
}

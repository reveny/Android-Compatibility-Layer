package android.text.style;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;

public class IconMarginSpan implements LeadingMarginSpan, LineHeightSpan
{
    private final Bitmap mBitmap;
    private final int mPad;

    public IconMarginSpan(Bitmap bitmap)
    {
        this(bitmap, 0);
    }

    public IconMarginSpan(Bitmap bitmap, int pad)
    {
        mBitmap = bitmap;
        mPad = pad;
    }

    @Override
    public int getLeadingMargin(boolean first)
    {
        return mBitmap.getWidth() + mPad;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout)
    {
        int spanStart = ((Spanned)text).getSpanStart(this);
        int itemTop = layout.getLineTop(layout.getLineForOffset(spanStart));

        if (direction < 0) x -= mBitmap.getWidth();

        canvas.drawBitmap(mBitmap, x, itemTop, paint);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fontMetrics)
    {
        if (end != ((Spanned)text).getSpanEnd(this)) return;

        int height = mBitmap.getHeight();
        int need = height - (spanstartv + fontMetrics.descent - fontMetrics.ascent - lineHeight);
        if (need > 0) fontMetrics.descent += need;

        need = height - (spanstartv + fontMetrics.bottom - fontMetrics.top - lineHeight);
        if (need > 0) fontMetrics.bottom += need;
    }

    @Override
    public String toString()
    {
        return "IconMarginSpan{bitmap=" + getBitmap() + ", padding=" + getPadding() + "}";
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    public int getPadding()
    {
        return mPad;
    }
}

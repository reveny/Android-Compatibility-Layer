package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class ReplacementSpan extends MetricAffectingSpan
{
    private CharSequence mContentDescription;

    public abstract int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetrics);

    public abstract void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
                              Paint paint);

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence description)
    {
        mContentDescription = description;
    }

    @Override
    public void updateDrawState(android.text.TextPaint paint)
    {
    }

    @Override
    public void updateMeasureState(android.text.TextPaint paint)
    {
    }
}

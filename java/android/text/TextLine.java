package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;

final class TextLine
{
    private static final int TAB_INCREMENT = 20;

    private final TextPaint mWorkPaint = new TextPaint();
    private final Paint.FontMetricsInt mWorkMetrics = new Paint.FontMetricsInt();

    private TextLine()
    {
    }

    static TextLine obtain()
    {
        return new TextLine();
    }

    static float measure(TextPaint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics)
    {
        return obtain().run(null, paint, text, start, end, 0f, 0, 0, 0, metrics, -1);
    }

    static float measureTo(TextPaint paint, CharSequence text, int start, int end, int offset)
    {
        return obtain().run(null, paint, text, start, end, 0f, 0, 0, 0, null, offset);
    }

    static void draw(Canvas canvas, TextPaint paint, CharSequence text, int start, int end, float x, int top, int baseline, int bottom)
    {
        obtain().run(canvas, paint, text, start, end, x, top, baseline, bottom, null, -1);
    }

    static int offsetForAdvance(TextPaint paint, CharSequence text, int start, int end, float advance)
    {
        if (advance <= 0) return start;

        TextLine line = obtain();
        float previous = 0f;

        for (int index = start; index < end; )
        {
            int next = TextUtils.getOffsetAfter(text, index);
            if (next > end) next = end;

            float width = line.run(null, paint, text, start, end, 0f, 0, 0, 0, null, next);

            if (width >= advance) return advance - previous < width - advance ? index : next;

            previous = width;
            index = next;
        }

        return end;
    }

    private static boolean hasTab(CharSequence text, int start, int end)
    {
        for (int index = start; index < end; index++)
        {
            if (text.charAt(index) == '\t') return true;
        }

        return false;
    }

    private float nextTab(CharSequence text, int start, int end, float horizontal)
    {
        if (text instanceof Spanned)
        {
            TabStopSpan[] stops = ((Spanned)text).getSpans(start, end, TabStopSpan.class);
            float best = Float.MAX_VALUE;

            for (TabStopSpan stop : stops)
            {
                int where = stop.getTabStop();
                if (where > horizontal && where < best) best = where;
            }

            if (best != Float.MAX_VALUE) return best;
        }

        return ((int)((horizontal + TAB_INCREMENT) / TAB_INCREMENT)) * TAB_INCREMENT;
    }

    private float run(Canvas canvas, TextPaint base, CharSequence text, int start, int end, float x, int top, int baseline, int bottom,
                      Paint.FontMetricsInt metrics, int stopAt)
    {
        if (base == null || text == null) return 0f;

        int limit = stopAt >= 0 ? Math.min(stopAt, end) : end;
        float horizontal = 0f;

        if (metrics != null) mergeMetrics(base, metrics, true);

        boolean tabs = hasTab(text, start, limit);
        Spanned spanned = text instanceof Spanned ? (Spanned)text : null;

        int segmentStart = start;
        while (segmentStart < limit)
        {
            int segmentEnd = limit;

            if (tabs)
            {
                int tab = TextUtils.indexOf(text, '\t', segmentStart, limit);
                if (tab >= 0) segmentEnd = tab;
            }

            if (spanned == null)
            {
                horizontal += drawRun(canvas, base, text, segmentStart, segmentEnd, x + horizontal, top, baseline, bottom, metrics);
            }
            else
            {
                horizontal += runSpans(canvas, base, spanned, segmentStart, segmentEnd, x + horizontal, top, baseline, bottom, metrics,
                                       start, end);
            }

            if (segmentEnd < limit)
            {
                horizontal = nextTab(text, start, end, horizontal);
                segmentStart = segmentEnd + 1;
            }
            else
            {
                segmentStart = segmentEnd;
            }
        }

        return horizontal;
    }

    private float runSpans(Canvas canvas, TextPaint base, Spanned text, int start, int end, float x, int top, int baseline, int bottom,
                           Paint.FontMetricsInt metrics, int lineStart, int lineEnd)
    {
        float horizontal = 0f;

        for (int index = start; index < end; )
        {
            int next = text.nextSpanTransition(index, end, CharacterStyle.class);

            CharacterStyle[] styles = text.getSpans(index, next, CharacterStyle.class);
            ReplacementSpan replacement = null;

            mWorkPaint.set(base);
            mWorkPaint.baselineShift = 0;
            mWorkPaint.bgColor = 0;

            for (CharacterStyle style : styles)
            {
                int spanStart = text.getSpanStart(style);
                int spanEnd = text.getSpanEnd(style);
                if (spanStart > index || spanEnd < next || spanStart == spanEnd) continue;

                if (style instanceof ReplacementSpan)
                {
                    replacement = (ReplacementSpan)style;
                    continue;
                }

                if (canvas != null) style.updateDrawState(mWorkPaint);
                else if (style instanceof MetricAffectingSpan) ((MetricAffectingSpan)style).updateMeasureState(mWorkPaint);
            }

            if (replacement != null)
            {
                int spanStart = Math.max(text.getSpanStart(replacement), lineStart);
                int spanEnd = Math.min(text.getSpanEnd(replacement), lineEnd);

                if (spanStart == index)
                {
                    Paint.FontMetricsInt replacementMetrics = metrics == null ? null : mWorkMetrics;
                    if (replacementMetrics != null) base.getFontMetricsInt(replacementMetrics);

                    float width = replacement.getSize(mWorkPaint, text, spanStart, spanEnd, replacementMetrics);

                    if (replacementMetrics != null) mergeMetricsFrom(replacementMetrics, metrics);
                    if (canvas != null)
                    {
                        replacement.draw(canvas, text, spanStart, spanEnd, x + horizontal, top, baseline, bottom, mWorkPaint);
                    }

                    horizontal += width;
                }

                index = Math.max(next, Math.min(spanEnd, end));
                continue;
            }

            horizontal += drawRun(canvas, mWorkPaint, text, index, next, x + horizontal, top, baseline, bottom, metrics);
            index = next;
        }

        return horizontal;
    }

    private float drawRun(Canvas canvas, TextPaint paint, CharSequence text, int start, int end, float x, int top, int baseline,
                          int bottom, Paint.FontMetricsInt metrics)
    {
        if (end <= start) return 0f;

        float width = paint.measureText(text, start, end);

        if (metrics != null) mergeMetrics(paint, metrics, false);

        if (canvas == null) return width;

        if (paint.bgColor != 0)
        {
            int color = paint.getColor();
            Paint.Style style = paint.getStyle();

            paint.setColor(paint.bgColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x, top, x + width, bottom, paint);

            paint.setStyle(style);
            paint.setColor(color);
        }

        int y = baseline + paint.baselineShift;
        canvas.drawText(text, start, end, x, y, paint);

        float size = paint.getTextSize();

        if (paint.isUnderlineText() || paint.underlineColor != 0)
        {
            float thickness = paint.underlineThickness > 0 ? paint.underlineThickness : Math.max(1f, size / 18f);
            float position = y + size / 9f;
            int color = paint.getColor();

            if (paint.underlineColor != 0 && !paint.isUnderlineText()) paint.setColor(paint.underlineColor);
            canvas.drawRect(x, position, x + width, position + thickness, paint);
            paint.setColor(color);
        }

        if (paint.isStrikeThruText())
        {
            float thickness = Math.max(1f, size / 18f);
            float position = y - size * 0.3f;
            canvas.drawRect(x, position, x + width, position + thickness, paint);
        }

        return width;
    }

    private void mergeMetrics(Paint paint, Paint.FontMetricsInt metrics, boolean reset)
    {
        paint.getFontMetricsInt(mWorkMetrics);

        int shift = paint instanceof TextPaint ? ((TextPaint)paint).baselineShift : 0;
        mWorkMetrics.top += Math.min(0, shift);
        mWorkMetrics.ascent += Math.min(0, shift);
        mWorkMetrics.descent += Math.max(0, shift);
        mWorkMetrics.bottom += Math.max(0, shift);

        if (reset)
        {
            metrics.top = mWorkMetrics.top;
            metrics.ascent = mWorkMetrics.ascent;
            metrics.descent = mWorkMetrics.descent;
            metrics.bottom = mWorkMetrics.bottom;
            metrics.leading = mWorkMetrics.leading;
            return;
        }

        mergeMetricsFrom(mWorkMetrics, metrics);
    }

    private static void mergeMetricsFrom(Paint.FontMetricsInt source, Paint.FontMetricsInt metrics)
    {
        metrics.top = Math.min(metrics.top, source.top);
        metrics.ascent = Math.min(metrics.ascent, source.ascent);
        metrics.descent = Math.max(metrics.descent, source.descent);
        metrics.bottom = Math.max(metrics.bottom, source.bottom);
        metrics.leading = Math.max(metrics.leading, source.leading);
    }
}

package android.text.style;

import android.graphics.Paint;

public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan
{
    void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fontMetrics);

    interface WithDensity extends LineHeightSpan
    {
        void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
                          Paint.FontMetricsInt fontMetrics, android.text.TextPaint paint);
    }
}

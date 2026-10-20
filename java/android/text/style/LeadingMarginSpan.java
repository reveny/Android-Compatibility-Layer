package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;

public interface LeadingMarginSpan extends ParagraphStyle
{
    int getLeadingMargin(boolean first);

    void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                           CharSequence text, int start, int end, boolean first, Layout layout);

    interface LeadingMarginSpan2 extends LeadingMarginSpan, WrapTogetherSpan
    {
        int getLeadingMarginLineCount();
    }
}

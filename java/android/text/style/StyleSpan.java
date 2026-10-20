package android.text.style;

import android.text.TextPaint;

public class StyleSpan extends MetricAffectingSpan
{
    private final int mStyle;

    public StyleSpan(int style)
    {
        mStyle = style;
    }

    public int getStyle()
    {
        return mStyle;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
    }
}

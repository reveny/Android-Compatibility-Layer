package android.text.style;

import android.text.TextPaint;

public class RelativeSizeSpan extends MetricAffectingSpan
{
    private final float mProportion;

    public RelativeSizeSpan(float proportion)
    {
        mProportion = proportion;
    }

    public float getSizeChange()
    {
        return mProportion;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setTextSize(paint.getTextSize() * mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextSize(paint.getTextSize() * mProportion);
    }
}

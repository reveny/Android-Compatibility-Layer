package android.text.style;

import android.text.TextPaint;

public class ScaleXSpan extends MetricAffectingSpan
{
    private final float mProportion;

    public ScaleXSpan(float proportion)
    {
        mProportion = proportion;
    }

    public float getScaleX()
    {
        return mProportion;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setTextScaleX(paint.getTextScaleX() * mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextScaleX(paint.getTextScaleX() * mProportion);
    }
}

package android.text.style;

import android.graphics.Typeface;
import android.text.TextPaint;

public class TypefaceSpan extends MetricAffectingSpan
{
    private final String mFamily;
    private final Typeface mTypeface;

    public TypefaceSpan(String family)
    {
        mFamily = family;
        mTypeface = null;
    }

    public TypefaceSpan(Typeface typeface)
    {
        mFamily = null;
        mTypeface = typeface;
    }

    public String getFamily()
    {
        return mFamily;
    }

    public Typeface getTypeface()
    {
        return mTypeface;
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

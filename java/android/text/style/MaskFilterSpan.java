package android.text.style;

import android.graphics.MaskFilter;
import android.text.TextPaint;

public class MaskFilterSpan extends CharacterStyle implements UpdateAppearance
{
    private final MaskFilter mFilter;

    public MaskFilterSpan(MaskFilter filter)
    {
        mFilter = filter;
    }

    public MaskFilter getMaskFilter()
    {
        return mFilter;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setMaskFilter(mFilter);
    }

    @Override
    public String toString()
    {
        return "MaskFilterSpan{filter=" + mFilter + "}";
    }
}

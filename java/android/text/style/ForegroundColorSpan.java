package android.text.style;

import android.text.TextPaint;

public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance
{
    private final int mColor;

    public ForegroundColorSpan(int color)
    {
        mColor = color;
    }

    public int getForegroundColor()
    {
        return mColor;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setColor(mColor);
    }
}

package android.text.style;

import android.text.TextPaint;

public class BackgroundColorSpan extends CharacterStyle implements UpdateAppearance
{
    private final int mColor;

    public BackgroundColorSpan(int color)
    {
        mColor = color;
    }

    public int getBackgroundColor()
    {
        return mColor;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
    }
}

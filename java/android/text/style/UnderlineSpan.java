package android.text.style;

import android.text.TextPaint;

public class UnderlineSpan extends CharacterStyle implements UpdateAppearance
{
    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setUnderlineText(true);
    }
}

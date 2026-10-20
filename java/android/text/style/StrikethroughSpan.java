package android.text.style;

import android.text.TextPaint;

public class StrikethroughSpan extends CharacterStyle implements UpdateAppearance
{
    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setStrikeThruText(true);
    }
}

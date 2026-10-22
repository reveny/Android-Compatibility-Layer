package android.text.style;

import android.text.TextPaint;
import android.view.View;

public abstract class ClickableSpan extends CharacterStyle implements UpdateAppearance
{
    private static int sIdCounter = 0;

    private final int mId = sIdCounter++;

    public ClickableSpan()
    {
    }

    public abstract void onClick(View widget);

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint == null) return;

        paint.setColor(paint.linkColor);
        paint.setUnderlineText(true);
    }

    public int getId()
    {
        return mId;
    }

    @Override
    public String toString()
    {
        return "ClickableSpan{}";
    }
}

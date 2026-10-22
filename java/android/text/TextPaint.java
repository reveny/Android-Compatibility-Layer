package android.text;

import android.graphics.Paint;

public class TextPaint extends Paint
{
    public int bgColor;
    public int baselineShift;
    public int linkColor;
    public int[] drawableState;
    public float density = 1.0f;
    public int underlineColor;
    public float underlineThickness;

    public TextPaint()
    {
    }

    public TextPaint(int flags)
    {
        super(flags);
    }

    public TextPaint(Paint from)
    {
        super(from);
    }

    public void set(TextPaint from)
    {
        super.set(from);

        bgColor = from.bgColor;
        baselineShift = from.baselineShift;
        linkColor = from.linkColor;
        drawableState = from.drawableState;
        density = from.density;
        underlineColor = from.underlineColor;
        underlineThickness = from.underlineThickness;
    }

    public float getUnderlineThickness()
    {
        if (underlineThickness > 0) return underlineThickness;

        return Math.max(1f, getTextSize() / 18f);
    }
}

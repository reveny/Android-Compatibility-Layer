package runtime;

import android.graphics.Paint;
import android.graphics.Typeface;

public final class NativeTextMeasurer implements TextMeasurer
{
    @Override
    public int resolveFamily(String familyName, int weight, boolean italic)
    {
        return Typeface.nativeResolveFamily(familyName, weight, italic);
    }

    @Override
    public int internStyle(int font, float textSize, float scaleX, float skewX, float letterSpacing, int flags)
    {
        return Paint.nativeInternStyle(font, textSize, scaleX, skewX, letterSpacing, flags);
    }

    @Override
    public float measureText(int style, char[] text, int index, int count)
    {
        return Paint.nativeMeasureText(style, text, index, count);
    }

    @Override
    public int textWidths(int style, char[] text, int index, int count, float[] outWidths)
    {
        return Paint.nativeGetTextWidths(style, text, index, count, outWidths);
    }

    @Override
    public void fontMetrics(int style, float[] outMetrics)
    {
        Paint.nativeGetFontMetrics(style, outMetrics);
    }

    @Override
    public void textBounds(int style, char[] text, int index, int count, int[] outBounds)
    {
        Paint.nativeGetTextBounds(style, text, index, count, outBounds);
    }

    @Override
    public int breakText(int style, char[] text, int index, int count, float maxWidth, boolean forwards, float[] outMeasured)
    {
        return Paint.nativeBreakText(style, text, index, count, maxWidth, forwards, outMeasured);
    }

    @Override
    public boolean hasGlyph(int style, String text)
    {
        return Paint.nativeHasGlyph(style, text);
    }
}

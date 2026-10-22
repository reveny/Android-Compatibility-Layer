package android.graphics.text;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontStyle;

import java.util.ArrayList;

public class TextRunShaper
{
    private TextRunShaper()
    {
    }

    public static PositionedGlyphs shapeTextRun(char[] text, int start, int count, int contextStart, int contextCount, float xOffset,
                                                float yOffset, boolean isRtl, Paint paint)
    {
        if (text == null) throw new NullPointerException("text");
        if (paint == null) throw new NullPointerException("paint");
        if (start < 0 || count < 0 || start + count > text.length) throw new IndexOutOfBoundsException("start or count out of range");

        float[] widths = new float[count];
        if (count > 0) paint.getTextWidths(text, start, count, widths);

        ArrayList<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < count; )
        {
            offsets.add(index);
            index += Character.charCount(Character.codePointAt(text, start + index, start + count));
        }

        int glyphCount = offsets.size();
        int[] glyphIds = new int[glyphCount];
        float[] positions = new float[glyphCount * 2];
        Font[] fonts = new Font[glyphCount];
        Font font = fontFor(paint);

        float x = xOffset;
        for (int position = 0; position < glyphCount; position++)
        {
            int offset = offsets.get(isRtl ? glyphCount - 1 - position : position);
            int next = offset + Character.charCount(Character.codePointAt(text, start + offset, start + count));

            float advance = 0;
            for (int index = offset; index < next; index++)
            {
                advance += widths[index];
            }

            glyphIds[position] = Character.codePointAt(text, start + offset, start + count);
            positions[position * 2] = x;
            positions[position * 2 + 1] = yOffset;
            fonts[position] = font;
            x += advance;
        }

        return new PositionedGlyphs(glyphIds, positions, fonts, x - xOffset, paint.ascent(), paint.descent(), xOffset, yOffset);
    }

    public static PositionedGlyphs shapeTextRun(CharSequence text, int start, int count, int contextStart, int contextCount,
                                                float xOffset, float yOffset, boolean isRtl, Paint paint)
    {
        if (text == null) throw new NullPointerException("text");

        char[] characters = text.toString().toCharArray();
        return shapeTextRun(characters, start, count, contextStart, contextCount, xOffset, yOffset, isRtl, paint);
    }

    private static Font fontFor(Paint paint)
    {
        Typeface typeface = paint.getTypeface();
        String family = typeface == null ? "sans-serif" : typeface.getSystemFontFamilyName();
        int weight = typeface == null ? FontStyle.FONT_WEIGHT_NORMAL : typeface.getWeight();
        boolean italic = typeface != null && typeface.isItalic();

        try
        {
            return new Font.Builder(new java.io.File(family)).setWeight(Math.max(FontStyle.FONT_WEIGHT_MIN, weight))
                .setSlant(italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT).build();
        }
        catch (java.io.IOException failure)
        {
            return null;
        }
    }
}

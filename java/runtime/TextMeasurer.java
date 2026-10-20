package runtime;

public interface TextMeasurer
{
    int internStyle(int font, float textSize, float scaleX, float skewX, float letterSpacing, int flags);

    float measureText(int style, char[] text, int index, int count);

    int textWidths(int style, char[] text, int index, int count, float[] outWidths);

    void fontMetrics(int style, float[] outMetrics);

    void textBounds(int style, char[] text, int index, int count, int[] outBounds);

    int breakText(int style, char[] text, int index, int count, float maxWidth, boolean forwards, float[] outMeasured);

    boolean hasGlyph(int style, String text);

    int resolveFamily(String familyName, int weight, boolean italic);
}

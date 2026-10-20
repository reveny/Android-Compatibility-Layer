package runtime;

public final class FixedAdvanceTextMeasurer implements TextMeasurer
{
    public static final float ADVANCE_RATIO = 0.5f;
    public static final float ASCENT_RATIO = -0.928f;
    public static final float DESCENT_RATIO = 0.244f;

    private static final int MAX_STYLE_COUNT = 4096;

    private final float[] mSizes = new float[MAX_STYLE_COUNT];
    private int mStyleCount;

    @Override
    public int resolveFamily(String familyName, int weight, boolean italic)
    {
        return 1;
    }

    @Override
    public int internStyle(int font, float textSize, float scaleX, float skewX, float letterSpacing, int flags)
    {
        for (int index = 0; index < mStyleCount; index++)
        {
            if (mSizes[index] == textSize) return index + 1;
        }

        if (mStyleCount >= MAX_STYLE_COUNT) return 0;

        mSizes[mStyleCount] = textSize;
        mStyleCount++;
        return mStyleCount;
    }

    private float sizeOf(int style)
    {
        if (style < 1 || style > mStyleCount) return 0f;
        return mSizes[style - 1];
    }

    private float advanceOf(int style)
    {
        return Math.max(1f, Math.round(sizeOf(style) * ADVANCE_RATIO));
    }

    @Override
    public float measureText(int style, char[] text, int index, int count)
    {
        return count * advanceOf(style);
    }

    @Override
    public int textWidths(int style, char[] text, int index, int count, float[] outWidths)
    {
        float advance = advanceOf(style);
        for (int position = 0; position < count; position++)
        {
            outWidths[position] = advance;
        }
        return count;
    }

    @Override
    public void fontMetrics(int style, float[] outMetrics)
    {
        float size = sizeOf(style);
        outMetrics[0] = size * ASCENT_RATIO - 1f;
        outMetrics[1] = size * ASCENT_RATIO;
        outMetrics[2] = size * DESCENT_RATIO;
        outMetrics[3] = size * DESCENT_RATIO + 1f;
        outMetrics[4] = 0f;
    }

    @Override
    public void textBounds(int style, char[] text, int index, int count, int[] outBounds)
    {
        float size = sizeOf(style);
        outBounds[0] = 0;
        outBounds[1] = Math.round(size * ASCENT_RATIO);
        outBounds[2] = Math.round(count * advanceOf(style));
        outBounds[3] = 0;
    }

    @Override
    public int breakText(int style, char[] text, int index, int count, float maxWidth, boolean forwards, float[] outMeasured)
    {
        float advance = advanceOf(style);
        int fits = advance <= 0f ? count : (int)Math.floor(maxWidth / advance);
        if (fits > count) fits = count;
        if (fits < 0) fits = 0;

        if (outMeasured != null && outMeasured.length > 0) outMeasured[0] = fits * advance;
        return fits;
    }

    @Override
    public boolean hasGlyph(int style, String text)
    {
        return text != null && text.length() == 1;
    }
}

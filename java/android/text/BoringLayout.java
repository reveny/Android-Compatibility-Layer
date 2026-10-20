package android.text;

import android.graphics.Paint;

public class BoringLayout extends Layout
{
    private final Metrics mMetrics;

    private BoringLayout(CharSequence text, TextPaint paint, int width, Alignment alignment, float spacingMultiplier,
                         float spacingAdd, Metrics metrics)
    {
        super(text, paint, width, alignment, spacingMultiplier, spacingAdd);
        mMetrics = metrics;
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint)
    {
        return isBoring(text, paint, null);
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, Metrics metrics)
    {
        return null;
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic direction, Metrics metrics)
    {
        return null;
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic direction,
                                   boolean useFallbackLineSpacing, Metrics metrics)
    {
        return null;
    }

    public static BoringLayout make(CharSequence text, TextPaint paint, int width, Alignment alignment,
                                    float spacingMultiplier, float spacingAdd, Metrics metrics, boolean includePad)
    {
        return new BoringLayout(text, paint, width, alignment, spacingMultiplier, spacingAdd, metrics);
    }

    public static BoringLayout make(CharSequence text, TextPaint paint, int width, Alignment alignment,
                                    float spacingMultiplier, float spacingAdd, Metrics metrics, boolean includePad,
                                    TextUtils.TruncateAt ellipsize, int ellipsizedWidth)
    {
        return new BoringLayout(text, paint, width, alignment, spacingMultiplier, spacingAdd, metrics);
    }

    public BoringLayout replaceOrMake(CharSequence text, TextPaint paint, int width, Alignment alignment,
                                      float spacingMultiplier, float spacingAdd, Metrics metrics, boolean includePad)
    {
        return make(text, paint, width, alignment, spacingMultiplier, spacingAdd, metrics, includePad);
    }

    @Override
    public int getLineCount()
    {
        return 1;
    }

    @Override
    public int getLineTop(int line)
    {
        if (mMetrics == null) return line == 0 ? 0 : 0;

        return line == 0 ? 0 : mMetrics.bottom - mMetrics.top;
    }

    @Override
    public int getLineStart(int line)
    {
        return line == 0 ? 0 : getText().length();
    }

    @Override
    public int getLineDescent(int line)
    {
        return mMetrics == null ? 0 : mMetrics.descent;
    }

    @Override
    public int getEllipsisCount(int line)
    {
        return 0;
    }

    public static class Metrics extends Paint.FontMetricsInt
    {
        public int width;

        @Override
        public String toString()
        {
            return "BoringLayout.Metrics{width=" + width + "}";
        }
    }
}

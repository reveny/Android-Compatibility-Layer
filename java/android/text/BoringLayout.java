package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class BoringLayout extends Layout implements TextUtils.EllipsizeCallback
{
    private int mBottom;
    private int mDescent;
    private int mTopPadding;
    private int mBottomPadding;
    private float mMax;
    private int mEllipsizedWidth;
    private int mEllipsizedStart;
    private int mEllipsizedCount;
    private boolean mUseFallbackLineSpacing;

    public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                        float spacingAdd, Metrics metrics, boolean includePad)
    {
        super(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd);

        mEllipsizedWidth = outerWidth;
        init(source, paint, alignment, metrics, includePad, true, false);
    }

    public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                        float spacingAdd, Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth)
    {
        this(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd, metrics, includePad, ellipsize, ellipsizedWidth, false);
    }

    public BoringLayout(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                        float spacingAdd, Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth,
                        boolean useFallbackLineSpacing)
    {
        super(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd);

        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE)
        {
            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        }
        else
        {
            TextUtils.ellipsize(source, paint, ellipsizedWidth, ellipsize, true, this);
            mEllipsizedWidth = ellipsizedWidth;
            trust = false;
        }

        init(getText(), paint, alignment, metrics, includePad, trust, useFallbackLineSpacing);
    }

    public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                                    float spacingAdd, Metrics metrics, boolean includePad)
    {
        return new BoringLayout(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd, metrics, includePad);
    }

    public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                                    float spacingAdd, Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize,
                                    int ellipsizedWidth)
    {
        return new BoringLayout(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd, metrics, includePad, ellipsize,
                                ellipsizedWidth);
    }

    public static BoringLayout make(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, Metrics metrics,
                                    boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth,
                                    boolean useFallbackLineSpacing)
    {
        return new BoringLayout(source, paint, outerWidth, alignment, 1f, 0f, metrics, includePad, ellipsize, ellipsizedWidth,
                                useFallbackLineSpacing);
    }

    public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                                      float spacingAdd, Metrics metrics, boolean includePad)
    {
        replaceWith(source, paint, outerWidth, alignment, spacingMultiplier, spacingAdd);

        mEllipsizedWidth = outerWidth;
        mEllipsizedStart = 0;
        mEllipsizedCount = 0;

        init(source, paint, alignment, metrics, includePad, true, false);
        return this;
    }

    public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, Metrics metrics,
                                      boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth,
                                      boolean useFallbackLineSpacing)
    {
        boolean trust;

        if (ellipsize == null || ellipsize == TextUtils.TruncateAt.MARQUEE)
        {
            replaceWith(source, paint, outerWidth, alignment, 1f, 0f);
            mEllipsizedWidth = outerWidth;
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            trust = true;
        }
        else
        {
            replaceWith(source, paint, ellipsizedWidth, alignment, 1f, 0f);
            mEllipsizedStart = 0;
            mEllipsizedCount = 0;
            TextUtils.ellipsize(source, paint, ellipsizedWidth, ellipsize, true, this);
            mEllipsizedWidth = ellipsizedWidth;
            trust = false;
        }

        init(getText(), paint, alignment, metrics, includePad, trust, useFallbackLineSpacing);
        return this;
    }

    public BoringLayout replaceOrMake(CharSequence source, TextPaint paint, int outerWidth, Alignment alignment, float spacingMultiplier,
                                      float spacingAdd, Metrics metrics, boolean includePad, TextUtils.TruncateAt ellipsize,
                                      int ellipsizedWidth)
    {
        return replaceOrMake(source, paint, outerWidth, alignment, metrics, includePad, ellipsize, ellipsizedWidth, false);
    }

    private void init(CharSequence source, TextPaint paint, Alignment alignment, Metrics metrics, boolean includePad, boolean trustWidth,
                      boolean useFallbackLineSpacing)
    {
        if (metrics == null) metrics = isBoring(source, paint, null);
        if (metrics == null) metrics = measure(source, paint, new Metrics());

        mUseFallbackLineSpacing = useFallbackLineSpacing;

        int spacing;
        if (includePad)
        {
            spacing = metrics.bottom - metrics.top;
            mDescent = metrics.bottom;
        }
        else
        {
            spacing = metrics.descent - metrics.ascent;
            mDescent = metrics.descent;
        }

        mBottom = spacing;

        if (includePad)
        {
            mTopPadding = metrics.top - metrics.ascent;
            mBottomPadding = metrics.bottom - metrics.descent;
        }
        else
        {
            mTopPadding = 0;
            mBottomPadding = 0;
        }

        if (trustWidth) mMax = metrics.width;
        else mMax = paint == null ? 0f : TextLine.measure(paint, source, 0, source.length(), null) - ellipsisSavings(paint, source);
    }

    private float ellipsisSavings(TextPaint paint, CharSequence source)
    {
        if (mEllipsizedCount <= 0) return 0f;

        int end = Math.min(source.length(), mEllipsizedStart + mEllipsizedCount);
        return TextLine.measure(paint, source, mEllipsizedStart, end, null) - paint.measureText(ELLIPSIS);
    }

    private static Metrics measure(CharSequence text, TextPaint paint, Metrics metrics)
    {
        if (paint == null)
        {
            metrics.width = 0;
            return metrics;
        }

        paint.getFontMetricsInt(metrics);
        metrics.width = (int)Math.ceil(TextLine.measure(paint, text, 0, text.length(), metrics));
        return metrics;
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint)
    {
        return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, false, null);
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, Metrics metrics)
    {
        return isBoring(text, paint, TextDirectionHeuristics.FIRSTSTRONG_LTR, false, metrics);
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic direction, Metrics metrics)
    {
        return isBoring(text, paint, direction, false, metrics);
    }

    public static Metrics isBoring(CharSequence text, TextPaint paint, TextDirectionHeuristic direction, boolean useFallbackLineSpacing,
                                   Metrics metrics)
    {
        if (text == null) return null;

        int length = text.length();

        for (int index = 0; index < length; index++)
        {
            char character = text.charAt(index);

            if (character == '\n' || character == '\t') return null;
            if (character >= 0x0590 && character <= 0x08FF) return null;
            if (character >= 0xD800 && character <= 0xDFFF) return null;
            if (character >= 0xFB1D && character <= 0xFDFF) return null;
            if (character >= 0xFE70 && character <= 0xFEFE) return null;
            if (character >= 0x202A && character <= 0x202E) return null;
            if (character >= 0x2066 && character <= 0x2069) return null;
        }

        if (hasParagraphStyle(text)) return null;

        Metrics result = metrics == null ? new Metrics() : metrics;
        result.reset();

        return measure(text, paint, result);
    }

    @Override
    public int getHeight()
    {
        return mBottom;
    }

    @Override
    public int getLineCount()
    {
        return 1;
    }

    @Override
    public int getLineTop(int line)
    {
        return line == 0 ? 0 : mBottom;
    }

    @Override
    public int getLineDescent(int line)
    {
        return mDescent;
    }

    @Override
    public int getLineStart(int line)
    {
        return line == 0 ? 0 : getText().length();
    }

    @Override
    public int getParagraphDirection(int line)
    {
        return DIR_LEFT_TO_RIGHT;
    }

    @Override
    public boolean getLineContainsTab(int line)
    {
        return false;
    }

    @Override
    public float getLineMax(int line)
    {
        return mMax;
    }

    @Override
    public float getLineWidth(int line)
    {
        return line == 0 ? mMax : 0f;
    }

    @Override
    public final Directions getLineDirections(int line)
    {
        return DIRS_ALL_LEFT_TO_RIGHT;
    }

    @Override
    public int getTopPadding()
    {
        return mTopPadding;
    }

    @Override
    public int getBottomPadding()
    {
        return mBottomPadding;
    }

    @Override
    public int getEllipsisCount(int line)
    {
        return mEllipsizedCount;
    }

    @Override
    public int getEllipsisStart(int line)
    {
        return mEllipsizedStart;
    }

    @Override
    public int getEllipsizedWidth()
    {
        return mEllipsizedWidth;
    }

    @Override
    public boolean isFallbackLineSpacingEnabled()
    {
        return mUseFallbackLineSpacing;
    }

    @Override
    public void draw(Canvas canvas, Path highlight, Paint highlightPaint, int cursorOffset)
    {
        super.draw(canvas, highlight, highlightPaint, cursorOffset);
    }

    @Override
    public void ellipsized(int start, int end)
    {
        mEllipsizedStart = start;
        mEllipsizedCount = end - start;
    }

    public static class Metrics extends Paint.FontMetricsInt
    {
        public int width;

        public Metrics()
        {
        }

        void reset()
        {
            top = 0;
            bottom = 0;
            ascent = 0;
            descent = 0;
            width = 0;
            leading = 0;
        }

        @Override
        public String toString()
        {
            return "BoringLayout.Metrics{top=" + top + ", ascent=" + ascent + ", descent=" + descent + ", bottom=" + bottom
                   + ", leading=" + leading + ", width=" + width + "}";
        }
    }
}

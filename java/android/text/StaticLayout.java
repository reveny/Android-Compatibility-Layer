package android.text;

import java.util.ArrayList;

public class StaticLayout extends Layout
{
    private final int[] mLineStarts;
    private final int[] mLineTops;
    private final int mLineHeight;
    private final int mDescent;

    public StaticLayout(CharSequence source, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd,
                        boolean includePad)
    {
        this(source, 0, source == null ? 0 : source.length(), paint, width, alignment, spacingMultiplier, spacingAdd, includePad);
    }

    public StaticLayout(CharSequence source, int bufferStart, int bufferEnd, TextPaint paint, int outerWidth, Alignment alignment,
                        float spacingMultiplier, float spacingAdd, boolean includePad)
    {
        super(source == null ? "" : source.subSequence(bufferStart, bufferEnd), paint, outerWidth, alignment, spacingMultiplier, spacingAdd);

        CharSequence text = getText();

        ArrayList<Integer> starts = new ArrayList<>();
        starts.add(0);
        breakLines(text, paint, outerWidth, starts);

        mLineStarts = new int[starts.size() + 1];
        for (int line = 0; line < starts.size(); line++)
        {
            mLineStarts[line] = starts.get(line);
        }
        mLineStarts[starts.size()] = text.length();

        int spacing = paint == null ? 0 : paint.getFontMetricsInt(null);
        mLineHeight = Math.max(1, Math.round(spacing * spacingMultiplier + spacingAdd));
        mDescent = paint == null ? 0 : Math.max(0, Math.round(paint.descent()));

        mLineTops = new int[mLineStarts.length];
        for (int line = 0; line < mLineTops.length; line++)
        {
            mLineTops[line] = line * mLineHeight;
        }
    }

    private static void breakLines(CharSequence text, TextPaint paint, int outerWidth, ArrayList<Integer> starts)
    {
        int length = text.length();
        if (length == 0 || paint == null) return;

        java.text.BreakIterator breaker = java.text.BreakIterator.getLineInstance(java.util.Locale.getDefault());
        breaker.setText(text.toString());

        int lineStart = 0;
        int lastFitting = -1;
        int candidate = breaker.first();

        while (candidate != java.text.BreakIterator.DONE)
        {
            if (candidate <= lineStart)
            {
                candidate = breaker.next();
                continue;
            }

            int hardBreak = indexOfNewline(text, lineStart, candidate);
            if (hardBreak >= 0)
            {
                starts.add(hardBreak + 1);
                lineStart = hardBreak + 1;
                lastFitting = -1;
                breaker.following(lineStart);
                candidate = breaker.current();
                continue;
            }

            float width = paint.measureText(text, lineStart, trimmedEnd(text, lineStart, candidate));
            if (width <= outerWidth)
            {
                lastFitting = candidate;
                candidate = breaker.next();
                continue;
            }

            int wrapAt = lastFitting > lineStart ? lastFitting : hardWrap(text, paint, lineStart, candidate, outerWidth);
            if (wrapAt <= lineStart) wrapAt = candidate;
            if (wrapAt >= length) break;

            starts.add(wrapAt);
            lineStart = wrapAt;
            lastFitting = -1;
            breaker.following(lineStart);
            candidate = breaker.current();
        }
    }

    private static int trimmedEnd(CharSequence text, int start, int end)
    {
        int trimmed = end;
        while (trimmed > start && text.charAt(trimmed - 1) == ' ')
        {
            trimmed--;
        }
        return trimmed;
    }

    private static int hardWrap(CharSequence text, TextPaint paint, int lineStart, int limit, int outerWidth)
    {
        int fits = lineStart;
        for (int index = lineStart + 1; index <= limit; index++)
        {
            if (paint.measureText(text, lineStart, index) > outerWidth) break;
            fits = index;
        }

        return fits > lineStart ? fits : lineStart + 1;
    }

    private static int indexOfNewline(CharSequence text, int start, int end)
    {
        for (int index = start; index < end; index++)
        {
            if (text.charAt(index) == '\n') return index;
        }
        return -1;
    }

    @Override
    public int getLineCount()
    {
        return mLineStarts.length - 1;
    }

    @Override
    public int getLineStart(int line)
    {
        if (line < 0) return 0;
        if (line >= mLineStarts.length) return mLineStarts[mLineStarts.length - 1];
        return mLineStarts[line];
    }

    @Override
    public int getLineTop(int line)
    {
        if (line < 0) return 0;
        if (line >= mLineTops.length) return mLineTops[mLineTops.length - 1];
        return mLineTops[line];
    }

    @Override
    public int getLineDescent(int line)
    {
        return mDescent;
    }

    @Override
    public int getEllipsisCount(int line)
    {
        return 0;
    }

    public boolean isFallbackLineSpacingEnabled()
    {
        return false;
    }

    public int getEllipsisStart(int line)
    {
        return 0;
    }

    public int getEllipsizedWidth()
    {
        return getWidth();
    }

    public static final class Builder
    {
        public static Builder obtain(CharSequence source, int start, int end, TextPaint paint, int width)
        {
            return new Builder(source, start, end, paint, width);
        }

        private final CharSequence mSource;
        private final int mStart;
        private final int mEnd;
        private final TextPaint mPaint;
        private final int mWidth;

        private Alignment mAlignment = Alignment.ALIGN_NORMAL;
        private float mSpacingMultiplier = 1.0f;
        private float mSpacingAdd = 0.0f;
        private boolean mIncludePad = true;

        Builder(CharSequence source, int start, int end, TextPaint paint, int width)
        {
            mSource = source;
            mStart = start;
            mEnd = end;
            mPaint = paint;
            mWidth = width;
        }

        public Builder setAlignment(Alignment alignment)
        {
            mAlignment = alignment;
            return this;
        }

        public Builder setLineSpacing(float spacingAdd, float spacingMultiplier)
        {
            mSpacingAdd = spacingAdd;
            mSpacingMultiplier = spacingMultiplier;
            return this;
        }

        public Builder setIncludePad(boolean includePad)
        {
            mIncludePad = includePad;
            return this;
        }

        public Builder setMaxLines(int maxLines)
        {
            return this;
        }

        public Builder setEllipsize(TextUtils.TruncateAt where)
        {
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic direction)
        {
            return this;
        }

        public Builder setEllipsizedWidth(int width)
        {
            return this;
        }

        public Builder setBreakStrategy(int strategy)
        {
            return this;
        }

        public Builder setHyphenationFrequency(int frequency)
        {
            return this;
        }

        public Builder setJustificationMode(int mode)
        {
            return this;
        }

        public Builder setUseLineSpacingFromFallbacks(boolean use)
        {
            return this;
        }

        public Builder setIndents(int[] leftIndents, int[] rightIndents)
        {
            return this;
        }

        public Builder setLineBreakConfig(android.graphics.text.LineBreakConfig config)
        {
            return this;
        }

        public StaticLayout build()
        {
            return new StaticLayout(mSource, mStart, mEnd, mPaint, mWidth, mAlignment, mSpacingMultiplier, mSpacingAdd, mIncludePad);
        }
    }
}

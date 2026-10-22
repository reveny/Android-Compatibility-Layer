package android.text;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.text.LineBreakConfig;
import java.util.ArrayList;
import java.util.Objects;

public class PrecomputedText implements Spannable
{
    private final SpannableString mText;
    private final int mStart;
    private final int mEnd;
    private final Params mParams;
    private final int[] mParagraphEnds;

    private PrecomputedText(CharSequence text, int start, int end, Params params, int[] paragraphEnds)
    {
        mText = new SpannableString(text, true);
        mStart = start;
        mEnd = end;
        mParams = params;
        mParagraphEnds = paragraphEnds;
    }

    public static PrecomputedText create(CharSequence text, Params params)
    {
        if (text instanceof PrecomputedText)
        {
            PrecomputedText existing = (PrecomputedText)text;
            if (existing.getParams().equals(params)) return existing;
        }

        ArrayList<Integer> ends = new ArrayList<>();
        int length = text.length();

        for (int index = 0; index < length; )
        {
            int newline = TextUtils.indexOf(text, '\n', index, length);
            int end = newline < 0 ? length : newline + 1;

            ends.add(end);
            index = end;
        }

        int[] paragraphEnds = new int[ends.size()];
        for (int index = 0; index < paragraphEnds.length; index++)
        {
            paragraphEnds[index] = ends.get(index);
        }

        return new PrecomputedText(text, 0, length, params, paragraphEnds);
    }

    public CharSequence getText()
    {
        return mText;
    }

    public int getStart()
    {
        return mStart;
    }

    public int getEnd()
    {
        return mEnd;
    }

    public Params getParams()
    {
        return mParams;
    }

    public int getParagraphCount()
    {
        return mParagraphEnds.length;
    }

    public int getParagraphStart(int paragraphIndex)
    {
        Objects.checkIndex(paragraphIndex, getParagraphCount());

        return paragraphIndex == 0 ? mStart : getParagraphEnd(paragraphIndex - 1);
    }

    public int getParagraphEnd(int paragraphIndex)
    {
        Objects.checkIndex(paragraphIndex, getParagraphCount());

        return mParagraphEnds[paragraphIndex];
    }

    private void checkRange(int start, int end)
    {
        if (start < 0 || end > length() || start > end) throw new IllegalArgumentException("Invalid range: " + start + ", " + end);
    }

    public float getWidth(int start, int end)
    {
        checkRange(start, end);

        float width = 0f;
        int paragraphStart = start;

        while (paragraphStart < end)
        {
            int newline = TextUtils.indexOf(mText, '\n', paragraphStart, end);
            int paragraphEnd = newline < 0 ? end : newline;

            width += TextLine.measure(mParams.getTextPaint(), mText, paragraphStart, paragraphEnd, null);
            paragraphStart = newline < 0 ? end : newline + 1;
        }

        return width;
    }

    public void getBounds(int start, int end, Rect bounds)
    {
        checkRange(start, end);
        Objects.requireNonNull(bounds);

        Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
        float width = start == end ? 0f : TextLine.measure(mParams.getTextPaint(), mText, start, end, metrics);

        bounds.set(0, metrics.ascent, (int)Math.ceil(width), metrics.descent);
    }

    public void getFontMetricsInt(int start, int end, Paint.FontMetricsInt outMetrics)
    {
        checkRange(start, end);
        Objects.requireNonNull(outMetrics);

        if (start == end)
        {
            mParams.getTextPaint().getFontMetricsInt(outMetrics);
            return;
        }

        TextLine.measure(mParams.getTextPaint(), mText, start, end, outMetrics);
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags)
    {
        if (what instanceof android.text.style.MetricAffectingSpan)
        {
            throw new IllegalArgumentException("MetricAffectingSpan can not be set to PrecomputedText.");
        }

        mText.setSpan(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what)
    {
        if (what instanceof android.text.style.MetricAffectingSpan)
        {
            throw new IllegalArgumentException("MetricAffectingSpan can not be removed from PrecomputedText.");
        }

        mText.removeSpan(what);
    }

    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type)
    {
        return mText.getSpans(start, end, type);
    }

    @Override
    public int getSpanStart(Object span)
    {
        return mText.getSpanStart(span);
    }

    @Override
    public int getSpanEnd(Object span)
    {
        return mText.getSpanEnd(span);
    }

    @Override
    public int getSpanFlags(Object span)
    {
        return mText.getSpanFlags(span);
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type)
    {
        return mText.nextSpanTransition(start, limit, type);
    }

    @Override
    public int length()
    {
        return mText.length();
    }

    @Override
    public char charAt(int index)
    {
        return mText.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return PrecomputedText.create(mText.subSequence(start, end), mParams);
    }

    @Override
    public String toString()
    {
        return mText.toString();
    }

    public static final class Params
    {
        private final TextPaint mPaint;
        private final TextDirectionHeuristic mTextDirection;
        private final int mBreakStrategy;
        private final int mHyphenationFrequency;
        private final LineBreakConfig mLineBreakConfig;

        private Params(TextPaint paint, LineBreakConfig lineBreakConfig, TextDirectionHeuristic textDirection, int breakStrategy,
                       int hyphenationFrequency)
        {
            mPaint = paint;
            mTextDirection = textDirection;
            mBreakStrategy = breakStrategy;
            mHyphenationFrequency = hyphenationFrequency;
            mLineBreakConfig = lineBreakConfig;
        }

        public TextPaint getTextPaint()
        {
            return mPaint;
        }

        public TextDirectionHeuristic getTextDirection()
        {
            return mTextDirection;
        }

        public int getBreakStrategy()
        {
            return mBreakStrategy;
        }

        public int getHyphenationFrequency()
        {
            return mHyphenationFrequency;
        }

        public LineBreakConfig getLineBreakConfig()
        {
            return mLineBreakConfig;
        }

        public boolean sameTextMetrics(Params other)
        {
            if (other == null) return false;

            return mBreakStrategy == other.mBreakStrategy && mHyphenationFrequency == other.mHyphenationFrequency
                   && mTextDirection == other.mTextDirection && samePaint(mPaint, other.mPaint);
        }

        private static boolean samePaint(TextPaint first, TextPaint second)
        {
            if (first == second) return true;
            if (first == null || second == null) return false;

            return first.getTextSize() == second.getTextSize() && first.getTextScaleX() == second.getTextScaleX()
                   && first.getTextSkewX() == second.getTextSkewX() && first.getLetterSpacing() == second.getLetterSpacing()
                   && first.getFlags() == second.getFlags() && Objects.equals(first.getTypeface(), second.getTypeface())
                   && Objects.equals(first.getTextLocales(), second.getTextLocales())
                   && Objects.equals(first.getFontFeatureSettings(), second.getFontFeatureSettings());
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == this) return true;
            if (!(other instanceof Params)) return false;

            return sameTextMetrics((Params)other);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mPaint == null ? 0f : mPaint.getTextSize(), mPaint == null ? 0f : mPaint.getTextScaleX(),
                                mPaint == null ? 0f : mPaint.getTextSkewX(), mPaint == null ? null : mPaint.getTypeface(),
                                mTextDirection, mBreakStrategy, mHyphenationFrequency);
        }

        @Override
        public String toString()
        {
            return "{textSize=" + (mPaint == null ? 0f : mPaint.getTextSize()) + ", textScaleX="
                   + (mPaint == null ? 0f : mPaint.getTextScaleX()) + ", textSkewX=" + (mPaint == null ? 0f : mPaint.getTextSkewX())
                   + ", letterSpacing=" + (mPaint == null ? 0f : mPaint.getLetterSpacing()) + ", textDir=" + mTextDirection
                   + ", breakStrategy=" + mBreakStrategy + ", hyphenationFrequency=" + mHyphenationFrequency + "}";
        }

        public static class Builder
        {
            private final TextPaint mPaint;
            private TextDirectionHeuristic mTextDirection = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            private int mBreakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY;
            private int mHyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NORMAL;
            private LineBreakConfig mLineBreakConfig = new LineBreakConfig.Builder().build();

            public Builder(TextPaint paint)
            {
                mPaint = paint;
            }

            public Builder(Params params)
            {
                mPaint = params.mPaint;
                mTextDirection = params.mTextDirection;
                mBreakStrategy = params.mBreakStrategy;
                mHyphenationFrequency = params.mHyphenationFrequency;
                mLineBreakConfig = params.mLineBreakConfig;
            }

            public Builder setBreakStrategy(int strategy)
            {
                mBreakStrategy = strategy;
                return this;
            }

            public Builder setHyphenationFrequency(int frequency)
            {
                mHyphenationFrequency = frequency;
                return this;
            }

            public Builder setTextDirection(TextDirectionHeuristic textDirection)
            {
                mTextDirection = textDirection;
                return this;
            }

            public Builder setLineBreakConfig(LineBreakConfig lineBreakConfig)
            {
                mLineBreakConfig = lineBreakConfig;
                return this;
            }

            public Params build()
            {
                return new Params(mPaint, mLineBreakConfig, mTextDirection, mBreakStrategy, mHyphenationFrequency);
            }
        }
    }
}

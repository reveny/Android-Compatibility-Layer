package android.text;

import android.graphics.Paint;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;

public class StaticLayout extends Layout
{
    private int mLineCount;
    private int[] mLineStarts;
    private int[] mLineTops;
    private int[] mLineDescents;
    private int[] mEllipsisStarts;
    private int[] mEllipsisCounts;
    private boolean[] mLineTabs;
    private int mTopPadding;
    private int mBottomPadding;
    private int mEllipsizedWidth;
    private int mMaximumLines;
    private boolean mFallbackLineSpacing;
    private int[] mLeftIndents;
    private int[] mRightIndents;

    public StaticLayout(CharSequence source, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd,
                        boolean includePad)
    {
        this(source, 0, source == null ? 0 : source.length(), paint, width, alignment, spacingMultiplier, spacingAdd, includePad);
    }

    public StaticLayout(CharSequence source, int bufferStart, int bufferEnd, TextPaint paint, int outerWidth, Alignment alignment,
                        float spacingMultiplier, float spacingAdd, boolean includePad)
    {
        this(source, bufferStart, bufferEnd, paint, outerWidth, alignment, spacingMultiplier, spacingAdd, includePad, null, 0);
    }

    public StaticLayout(CharSequence source, int bufferStart, int bufferEnd, TextPaint paint, int outerWidth, Alignment alignment,
                        float spacingMultiplier, float spacingAdd, boolean includePad, TextUtils.TruncateAt ellipsize, int ellipsizedWidth)
    {
        this(Builder.obtain(source == null ? "" : source, bufferStart, bufferEnd, paint, outerWidth).setAlignment(alignment)
                 .setLineSpacing(spacingAdd, spacingMultiplier).setIncludePad(includePad).setEllipsize(ellipsize)
                 .setEllipsizedWidth(ellipsizedWidth).setMaxLines(Integer.MAX_VALUE));
    }

    public StaticLayout(CharSequence source, int bufferStart, int bufferEnd, TextPaint paint, int outerWidth, Alignment alignment,
                        TextDirectionHeuristic direction, float spacingMultiplier, float spacingAdd, boolean includePad,
                        TextUtils.TruncateAt ellipsize, int ellipsizedWidth, int maxLines)
    {
        this(Builder.obtain(source == null ? "" : source, bufferStart, bufferEnd, paint, outerWidth).setAlignment(alignment)
                 .setTextDirection(direction).setLineSpacing(spacingAdd, spacingMultiplier).setIncludePad(includePad)
                 .setEllipsize(ellipsize).setEllipsizedWidth(ellipsizedWidth).setMaxLines(maxLines));
    }

    private StaticLayout(Builder builder)
    {
        super(builder.mText, builder.mPaint, builder.mWidth, builder.mAlignment, builder.mSpacingMultiplier, builder.mSpacingAdd);

        mEllipsizedWidth = builder.mEllipsize != null ? builder.mEllipsizedWidth : builder.mWidth;
        mMaximumLines = builder.mMaxLines;
        mFallbackLineSpacing = builder.mFallbackLineSpacing;
        mLeftIndents = builder.mLeftIndents;
        mRightIndents = builder.mRightIndents;

        generate(builder);
    }

    private void ensureCapacity(int lines)
    {
        if (mLineStarts != null && mLineStarts.length > lines + 1) return;

        int capacity = Math.max(8, lines * 2 + 2);
        mLineStarts = grow(mLineStarts, capacity);
        mLineTops = grow(mLineTops, capacity);
        mLineDescents = grow(mLineDescents, capacity);
        mEllipsisStarts = grow(mEllipsisStarts, capacity);
        mEllipsisCounts = grow(mEllipsisCounts, capacity);

        boolean[] tabs = new boolean[capacity];
        if (mLineTabs != null) System.arraycopy(mLineTabs, 0, tabs, 0, mLineTabs.length);
        mLineTabs = tabs;
    }

    private static int[] grow(int[] array, int capacity)
    {
        int[] grown = new int[capacity];
        if (array != null) System.arraycopy(array, 0, grown, 0, array.length);
        return grown;
    }

    private int indentWidth(int line)
    {
        int indent = 0;

        if (mLeftIndents != null && mLeftIndents.length > 0) indent += mLeftIndents[Math.min(line, mLeftIndents.length - 1)];
        if (mRightIndents != null && mRightIndents.length > 0) indent += mRightIndents[Math.min(line, mRightIndents.length - 1)];

        return indent;
    }

    private void generate(Builder builder)
    {
        CharSequence text = builder.mText;
        int start = builder.mStart;
        int end = builder.mEnd;
        TextPaint paint = builder.mPaint;
        int width = builder.mWidth;
        boolean includePad = builder.mIncludePad;
        TextUtils.TruncateAt ellipsize = builder.mEllipsize;

        boolean ellipsisMayBeApplied = ellipsize != null
                                       && (ellipsize == TextUtils.TruncateAt.END
                                           || (mMaximumLines == 1 && ellipsize != TextUtils.TruncateAt.MARQUEE));

        ensureCapacity(1);
        mLineCount = 0;
        mLineStarts[0] = start;
        mLineTops[0] = 0;

        Spanned spanned = text instanceof Spanned ? (Spanned)text : null;
        Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();

        java.text.BreakIterator breaker = java.text.BreakIterator.getLineInstance(java.util.Locale.getDefault());
        String string = text.toString();
        breaker.setText(string);

        int paragraphStart = start;
        int top = 0;

        while (paragraphStart <= end)
        {
            int newline = TextUtils.indexOf(text, '\n', paragraphStart, end);
            int paragraphEnd = newline < 0 ? end : newline + 1;

            int firstWidth = width;
            int restWidth = width;
            int firstWidthLineCount = 1;
            LineHeightSpan[] chooseHeight = null;

            if (spanned != null)
            {
                LeadingMarginSpan[] margins = getParagraphSpans(spanned, paragraphStart, paragraphEnd, LeadingMarginSpan.class);

                for (LeadingMarginSpan margin : margins)
                {
                    firstWidth -= margin.getLeadingMargin(true);
                    restWidth -= margin.getLeadingMargin(false);

                    if (margin instanceof LeadingMarginSpan.LeadingMarginSpan2)
                    {
                        int spanLine = getLineForOffsetInternal(spanned.getSpanStart(margin));
                        int count = ((LeadingMarginSpan.LeadingMarginSpan2)margin).getLeadingMarginLineCount();
                        firstWidthLineCount = Math.max(firstWidthLineCount, spanLine + count - mLineCount);
                    }
                }

                chooseHeight = getParagraphSpans(spanned, paragraphStart, paragraphEnd, LineHeightSpan.class);
                if (chooseHeight.length == 0) chooseHeight = null;
            }

            int lineStart = paragraphStart;
            int paragraphLine = 0;

            while (true)
            {
                int available = (paragraphLine < firstWidthLineCount ? firstWidth : restWidth) - indentWidth(mLineCount);
                boolean lastAllowedLine = mLineCount + 1 >= mMaximumLines;

                int lineEnd;
                if (lastAllowedLine && ellipsisMayBeApplied) lineEnd = paragraphEnd;
                else lineEnd = breakLine(text, string, breaker, paint, lineStart, paragraphEnd, Math.max(1, available));

                boolean moreChars = lineEnd < end || paragraphEnd < end;
                boolean lastLine = lineEnd >= end || lastAllowedLine;

                top = addLine(text, paint, lineStart, lineEnd, top, metrics, chooseHeight, spanned, includePad, mLineCount == 0,
                              lastLine && !(lineEnd >= end && end > start && text.charAt(end - 1) == '\n'));

                if (ellipsize != null)
                {
                    boolean forceEllipsis = moreChars && lastAllowedLine;
                    boolean firstLine = mLineCount == 1;
                    boolean doEllipsis = (((mMaximumLines == 1 && moreChars) || (firstLine && !moreChars))
                                          && ellipsize != TextUtils.TruncateAt.MARQUEE)
                                         || (!firstLine && (lastAllowedLine || !moreChars) && ellipsize == TextUtils.TruncateAt.END);

                    if (doEllipsis)
                    {
                        calculateEllipsis(text, paint, lineStart, lineEnd, mLineCount - 1,
                                          mEllipsizedWidth - (width - (paragraphLine < firstWidthLineCount ? firstWidth : restWidth)),
                                          ellipsize, forceEllipsis);
                    }
                }

                paragraphLine++;
                lineStart = lineEnd;

                if (mLineCount >= mMaximumLines) break;
                if (lineStart >= paragraphEnd) break;
            }

            if (mLineCount >= mMaximumLines) break;

            if (paragraphEnd >= end)
            {
                if (end > start && text.charAt(end - 1) == '\n' && mLineCount < mMaximumLines)
                {
                    top = addLine(text, paint, end, end, top, metrics, null, null, includePad, mLineCount == 0, true);
                }
                break;
            }

            paragraphStart = paragraphEnd;
        }

        if (mLineCount == 0) top = addLine(text, paint, start, end, top, metrics, null, null, includePad, true, true);
    }

    private int getLineForOffsetInternal(int offset)
    {
        for (int line = mLineCount - 1; line >= 0; line--)
        {
            if (mLineStarts[line] <= offset) return line;
        }

        return 0;
    }

    private static float measure(CharSequence text, TextPaint paint, int start, int end)
    {
        if (end <= start) return 0f;

        if (!(text instanceof Spanned)) return paint.measureText(text, start, end);

        return TextLine.measure(paint, text, start, end, null);
    }

    private static int trimmedEnd(CharSequence text, int start, int end)
    {
        int trimmed = end;
        while (trimmed > start)
        {
            char character = text.charAt(trimmed - 1);
            if (character != ' ' && character != '\n' && character != '\t') break;
            trimmed--;
        }
        return trimmed;
    }

    private static int breakLine(CharSequence text, String string, java.text.BreakIterator breaker, TextPaint paint, int lineStart,
                                 int paragraphEnd, int available)
    {
        if (paint == null) return paragraphEnd;

        int visibleEnd = trimmedEnd(text, lineStart, paragraphEnd);
        if (measure(text, paint, lineStart, visibleEnd) <= available) return paragraphEnd;

        int lastFitting = -1;
        int candidate = breaker.following(lineStart);

        while (candidate != java.text.BreakIterator.DONE && candidate <= paragraphEnd)
        {
            float width = measure(text, paint, lineStart, trimmedEnd(text, lineStart, candidate));

            if (width > available) break;

            lastFitting = candidate;
            if (candidate >= paragraphEnd) break;

            candidate = breaker.next();
        }

        if (lastFitting > lineStart) return lastFitting;

        int limit = candidate == java.text.BreakIterator.DONE ? paragraphEnd : Math.min(candidate, paragraphEnd);
        return hardWrap(text, paint, lineStart, limit, available);
    }

    private static int hardWrap(CharSequence text, TextPaint paint, int lineStart, int limit, int available)
    {
        int low = lineStart + 1;
        int high = limit;
        int fits = lineStart;

        while (low <= high)
        {
            int middle = (low + high) >>> 1;

            if (measure(text, paint, lineStart, middle) <= available)
            {
                fits = middle;
                low = middle + 1;
            }
            else
            {
                high = middle - 1;
            }
        }

        if (fits <= lineStart) fits = TextUtils.getOffsetAfter(text, lineStart);
        if (fits > lineStart && fits < text.length() && Character.isLowSurrogate(text.charAt(fits))
            && Character.isHighSurrogate(text.charAt(fits - 1)))
        {
            fits = fits - 1 > lineStart ? fits - 1 : fits + 1;
        }

        return Math.min(fits, limit);
    }

    private int addLine(CharSequence text, TextPaint paint, int start, int end, int top, Paint.FontMetricsInt metrics,
                        LineHeightSpan[] chooseHeight, Spanned spanned, boolean includePad, boolean firstLine, boolean lastLine)
    {
        ensureCapacity(mLineCount + 1);

        int line = mLineCount;

        if (paint != null)
        {
            int visibleEnd = trimmedEnd(text, start, end);
            if (visibleEnd > start && text instanceof Spanned) TextLine.measure(paint, text, start, visibleEnd, metrics);
            else paint.getFontMetricsInt(metrics);
        }
        else
        {
            metrics.top = metrics.ascent = metrics.descent = metrics.bottom = metrics.leading = 0;
        }

        if (chooseHeight != null)
        {
            for (LineHeightSpan span : chooseHeight)
            {
                int spanStart = spanned.getSpanStart(span);
                int spanTop = spanStart < start ? mLineTops[getLineForOffsetInternal(spanStart)] : top;

                if (span instanceof LineHeightSpan.WithDensity)
                {
                    ((LineHeightSpan.WithDensity)span).chooseHeight(text, start, end, spanTop, top, metrics, paint);
                }
                else
                {
                    span.chooseHeight(text, start, end, spanTop, top, metrics);
                }
            }
        }

        int above = metrics.ascent;
        int below = metrics.descent;

        if (includePad && firstLine)
        {
            mTopPadding = metrics.top - metrics.ascent;
            above = metrics.top;
        }

        if (includePad && lastLine)
        {
            mBottomPadding = metrics.bottom - metrics.descent;
            below = metrics.bottom;
        }

        int extra = 0;
        float multiplier = getSpacingMultiplier();
        float add = getSpacingAdd();

        if ((multiplier != 1f || add != 0f) && !lastLine)
        {
            double extraSpace = (below - above) * (multiplier - 1) + add;
            extra = extraSpace >= 0 ? (int)(extraSpace + 0.5) : -(int)(-extraSpace + 0.5);
        }

        mLineStarts[line] = start;
        mLineTops[line] = top;
        mLineDescents[line] = below + extra;
        mEllipsisStarts[line] = 0;
        mEllipsisCounts[line] = 0;
        mLineTabs[line] = TextUtils.indexOf(text, '\t', start, end) >= 0;

        int bottom = top + (below - above) + extra;

        mLineCount++;
        mLineStarts[mLineCount] = end;
        mLineTops[mLineCount] = bottom;

        return bottom;
    }

    private void calculateEllipsis(CharSequence text, TextPaint paint, int lineStart, int lineEnd, int line, float available,
                                   TextUtils.TruncateAt where, boolean forceEllipsis)
    {
        if (paint == null) return;

        int visibleEnd = trimmedEnd(text, lineStart, lineEnd);
        float textWidth = measure(text, paint, lineStart, visibleEnd);

        if (textWidth <= available && !forceEllipsis)
        {
            mEllipsisStarts[line] = 0;
            mEllipsisCounts[line] = 0;
            return;
        }

        float ellipsisWidth = paint.measureText(ELLIPSIS);
        int length = visibleEnd - lineStart;
        float[] widths = new float[length];

        for (int index = 0; index < length; index++)
        {
            widths[index] = measure(text, paint, lineStart + index, lineStart + index + 1);
        }

        int ellipsisStart = 0;
        int ellipsisCount = 0;

        if (where == TextUtils.TruncateAt.START)
        {
            if (mMaximumLines == 1)
            {
                float sum = 0;
                int index = length;

                for (; index > 0; index--)
                {
                    float width = widths[index - 1];
                    if (width + sum + ellipsisWidth > available) break;
                    sum += width;
                }

                ellipsisStart = 0;
                ellipsisCount = index;
            }
        }
        else if (where == TextUtils.TruncateAt.END || where == TextUtils.TruncateAt.MARQUEE)
        {
            float sum = 0;
            int index = 0;

            for (; index < length; index++)
            {
                float width = widths[index];
                if (width + sum + ellipsisWidth > available) break;
                sum += width;
            }

            ellipsisStart = index;
            ellipsisCount = length - index;

            if (forceEllipsis && ellipsisCount == 0 && length > 0)
            {
                ellipsisStart = length - 1;
                ellipsisCount = 1;
            }
        }
        else if (mMaximumLines == 1)
        {
            float leftSum = 0;
            float rightSum = 0;
            int left = 0;
            int right = length;

            float rightAvailable = (available - ellipsisWidth) / 2;
            for (; right > 0; right--)
            {
                float width = widths[right - 1];
                if (width + rightSum > rightAvailable) break;
                rightSum += width;
            }

            float leftAvailable = available - ellipsisWidth - rightSum;
            for (; left < right; left++)
            {
                float width = widths[left];
                if (width + leftSum > leftAvailable) break;
                leftSum += width;
            }

            ellipsisStart = left;
            ellipsisCount = right - left;
        }

        mEllipsisStarts[line] = ellipsisStart;
        mEllipsisCounts[line] = ellipsisCount;
    }

    @Override
    public int getLineForVertical(int vertical)
    {
        int high = mLineCount;
        int low = -1;

        while (high - low > 1)
        {
            int guess = (high + low) >> 1;

            if (mLineTops[guess] > vertical) high = guess;
            else low = guess;
        }

        return Math.max(0, low);
    }

    @Override
    public int getLineCount()
    {
        return mLineCount;
    }

    @Override
    public int getLineTop(int line)
    {
        if (line < 0) return 0;
        if (line > mLineCount) return mLineTops[mLineCount];

        return mLineTops[line];
    }

    @Override
    public int getLineDescent(int line)
    {
        if (line < 0 || line >= mLineCount) return 0;

        return mLineDescents[line];
    }

    @Override
    public int getLineStart(int line)
    {
        if (line < 0) return mLineStarts[0];
        if (line > mLineCount) return mLineStarts[mLineCount];

        return mLineStarts[line];
    }

    @Override
    public int getParagraphDirection(int line)
    {
        return DIR_LEFT_TO_RIGHT;
    }

    @Override
    public boolean getLineContainsTab(int line)
    {
        return line >= 0 && line < mLineCount && mLineTabs[line];
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
        if (line < 0 || line >= mLineCount) return 0;

        return mEllipsisCounts[line];
    }

    @Override
    public int getEllipsisStart(int line)
    {
        if (line < 0 || line >= mLineCount) return 0;

        return mEllipsisStarts[line];
    }

    @Override
    public int getEllipsizedWidth()
    {
        return mEllipsizedWidth;
    }

    @Override
    public boolean isFallbackLineSpacingEnabled()
    {
        return mFallbackLineSpacing;
    }

    public static final class Builder
    {
        private CharSequence mText;
        private int mStart;
        private int mEnd;
        private TextPaint mPaint;
        private int mWidth;
        private Alignment mAlignment = Alignment.ALIGN_NORMAL;
        private TextDirectionHeuristic mTextDirection = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        private float mSpacingMultiplier = 1.0f;
        private float mSpacingAdd = 0.0f;
        private boolean mIncludePad = true;
        private boolean mFallbackLineSpacing;
        private int mEllipsizedWidth;
        private TextUtils.TruncateAt mEllipsize;
        private int mMaxLines = Integer.MAX_VALUE;
        private int mBreakStrategy;
        private int mHyphenationFrequency;
        private int[] mLeftIndents;
        private int[] mRightIndents;
        private int mJustificationMode;
        private android.graphics.text.LineBreakConfig mLineBreakConfig;
        private boolean mUseBoundsForWidth;

        private Builder()
        {
        }

        public static Builder obtain(CharSequence source, int start, int end, TextPaint paint, int width)
        {
            Builder builder = new Builder();
            builder.mText = source;
            builder.mStart = start;
            builder.mEnd = end;
            builder.mPaint = paint;
            builder.mWidth = width;
            builder.mEllipsizedWidth = width;
            return builder;
        }

        public Builder setText(CharSequence source)
        {
            return setText(source, 0, source.length());
        }

        public Builder setText(CharSequence source, int start, int end)
        {
            mText = source;
            mStart = start;
            mEnd = end;
            return this;
        }

        public Builder setPaint(TextPaint paint)
        {
            mPaint = paint;
            return this;
        }

        public Builder setWidth(int width)
        {
            mWidth = width;
            if (mEllipsize == null) mEllipsizedWidth = width;
            return this;
        }

        public Builder setAlignment(Alignment alignment)
        {
            mAlignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic direction)
        {
            mTextDirection = direction;
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

        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks)
        {
            mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        public Builder setEllipsizedWidth(int ellipsizedWidth)
        {
            mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        public Builder setEllipsize(TextUtils.TruncateAt ellipsize)
        {
            mEllipsize = ellipsize;
            return this;
        }

        public Builder setMaxLines(int maxLines)
        {
            mMaxLines = maxLines;
            return this;
        }

        public Builder setBreakStrategy(int breakStrategy)
        {
            mBreakStrategy = breakStrategy;
            return this;
        }

        public Builder setHyphenationFrequency(int hyphenationFrequency)
        {
            mHyphenationFrequency = hyphenationFrequency;
            return this;
        }

        public Builder setIndents(int[] leftIndents, int[] rightIndents)
        {
            mLeftIndents = leftIndents;
            mRightIndents = rightIndents;
            return this;
        }

        public Builder setJustificationMode(int justificationMode)
        {
            mJustificationMode = justificationMode;
            return this;
        }

        public Builder setLineBreakConfig(android.graphics.text.LineBreakConfig lineBreakConfig)
        {
            mLineBreakConfig = lineBreakConfig;
            return this;
        }

        public Builder setUseBoundsForWidth(boolean useBoundsForWidth)
        {
            mUseBoundsForWidth = useBoundsForWidth;
            return this;
        }

        public StaticLayout build()
        {
            if (mText == null) mText = "";
            if (mMaxLines <= 0) mMaxLines = Integer.MAX_VALUE;
            if (mWidth < 0) mWidth = 0;
            if (mStart < 0) mStart = 0;
            if (mEnd > mText.length()) mEnd = mText.length();
            if (mEnd < mStart) mEnd = mStart;

            return new StaticLayout(this);
        }
    }
}

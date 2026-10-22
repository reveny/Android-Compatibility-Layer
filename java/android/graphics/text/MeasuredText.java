package android.graphics.text;

import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;

public class MeasuredText
{
    private final char[] mText;
    private final float[] mWidths;
    private final ArrayList<Run> mRuns;
    private final boolean mComputeHyphenation;
    private final boolean mComputeLayout;

    private MeasuredText(char[] text, float[] widths, ArrayList<Run> runs, boolean computeHyphenation, boolean computeLayout)
    {
        mText = text;
        mWidths = widths;
        mRuns = runs;
        mComputeHyphenation = computeHyphenation;
        mComputeLayout = computeLayout;
    }

    public char[] getChars()
    {
        return mText;
    }

    public float getWidth(int start, int end)
    {
        checkRange(start, end);

        float width = 0;
        for (int index = start; index < end; index++)
        {
            width += mWidths[index];
        }
        return width;
    }

    public void getBounds(int start, int end, Rect rect)
    {
        checkRange(start, end);
        if (rect == null) throw new NullPointerException("rect");

        rect.set(0, 0, 0, 0);

        float offset = 0;
        boolean first = true;
        Rect runBounds = new Rect();

        for (Run run : mRuns)
        {
            int from = Math.max(start, run.mStart);
            int to = Math.min(end, run.mEnd);
            if (from >= to) continue;

            if (run.mPaint == null)
            {
                runBounds.set(0, 0, (int)Math.ceil(run.mReplacementWidth), 0);
            }
            else
            {
                run.mPaint.getTextBounds(mText, from, to - from, runBounds);
            }

            int shift = (int)offset;
            runBounds.offset(shift, 0);

            if (first) rect.set(runBounds);
            else rect.union(runBounds);

            first = false;
            offset += getWidth(from, to);
        }
    }

    public void getFontMetricsInt(int start, int end, Paint.FontMetricsInt outMetrics)
    {
        checkRange(start, end);
        if (outMetrics == null) throw new NullPointerException("outMetrics");

        outMetrics.top = 0;
        outMetrics.ascent = 0;
        outMetrics.descent = 0;
        outMetrics.bottom = 0;
        outMetrics.leading = 0;

        Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
        for (Run run : mRuns)
        {
            if (run.mPaint == null || Math.max(start, run.mStart) >= Math.min(end, run.mEnd)) continue;

            run.mPaint.getFontMetricsInt(metrics);
            outMetrics.top = Math.min(outMetrics.top, metrics.top);
            outMetrics.ascent = Math.min(outMetrics.ascent, metrics.ascent);
            outMetrics.descent = Math.max(outMetrics.descent, metrics.descent);
            outMetrics.bottom = Math.max(outMetrics.bottom, metrics.bottom);
            outMetrics.leading = Math.max(outMetrics.leading, metrics.leading);
        }
    }

    public float getCharWidthAt(int offset)
    {
        if (offset < 0 || offset >= mText.length)
        {
            throw new IllegalArgumentException("offset(" + offset + ") is out of range [0, " + mText.length + ")");
        }

        return mWidths[offset];
    }

    private void checkRange(int start, int end)
    {
        if (start < 0 || end > mText.length || start > end)
        {
            throw new IllegalArgumentException("Invalid range [" + start + ", " + end + ") for text of length " + mText.length);
        }
    }

    private static final class Run
    {
        final int mStart;
        final int mEnd;
        final Paint mPaint;
        final float mReplacementWidth;

        Run(int start, int end, Paint paint, float replacementWidth)
        {
            mStart = start;
            mEnd = end;
            mPaint = paint;
            mReplacementWidth = replacementWidth;
        }
    }

    public static final class Builder
    {
        public static final int HYPHENATION_MODE_NONE = 0;
        public static final int HYPHENATION_MODE_NORMAL = 1;
        public static final int HYPHENATION_MODE_FAST = 2;

        private final char[] mText;
        private final float[] mWidths;
        private final ArrayList<Run> mRuns = new ArrayList<>();

        private int mCurrentOffset;
        private int mHyphenationMode = HYPHENATION_MODE_NONE;
        private boolean mComputeLayout = true;
        private boolean mBuilt;

        public Builder(char[] text)
        {
            if (text == null) throw new NullPointerException("text");

            mText = text;
            mWidths = new float[text.length];
        }

        public Builder(MeasuredText text)
        {
            if (text == null) throw new NullPointerException("text");
            if (!text.mComputeLayout)
            {
                throw new IllegalArgumentException("The input MeasuredText must not be created with setComputeLayout(false).");
            }

            mText = text.mText;
            mWidths = new float[mText.length];
        }

        public Builder appendStyleRun(Paint paint, int length, boolean isRtl)
        {
            return appendStyleRun(paint, null, length, isRtl);
        }

        public Builder appendStyleRun(Paint paint, LineBreakConfig lineBreakConfig, int length, boolean isRtl)
        {
            if (paint == null) throw new NullPointerException("paint");
            checkLength(length);

            int end = mCurrentOffset + length;
            if (length > 0)
            {
                float[] widths = new float[length];
                paint.getTextWidths(mText, mCurrentOffset, length, widths);
                System.arraycopy(widths, 0, mWidths, mCurrentOffset, length);
            }

            mRuns.add(new Run(mCurrentOffset, end, new Paint(paint), 0));
            mCurrentOffset = end;
            return this;
        }

        public Builder appendReplacementRun(Paint paint, int length, float width)
        {
            if (paint == null) throw new NullPointerException("paint");
            if (width < 0) throw new IllegalArgumentException("width must not be negative");
            checkLength(length);

            if (length > 0)
            {
                mWidths[mCurrentOffset] = width;
                for (int index = mCurrentOffset + 1; index < mCurrentOffset + length; index++)
                {
                    mWidths[index] = 0;
                }
            }

            mRuns.add(new Run(mCurrentOffset, mCurrentOffset + length, null, width));
            mCurrentOffset += length;
            return this;
        }

        public Builder setComputeHyphenation(boolean computeHyphenation)
        {
            return setComputeHyphenation(computeHyphenation ? HYPHENATION_MODE_NORMAL : HYPHENATION_MODE_NONE);
        }

        public Builder setComputeHyphenation(int mode)
        {
            if (mode != HYPHENATION_MODE_NONE && mode != HYPHENATION_MODE_NORMAL && mode != HYPHENATION_MODE_FAST)
            {
                throw new IllegalArgumentException("Unknown hyphenation mode: " + mode);
            }

            mHyphenationMode = mode;
            return this;
        }

        public Builder setComputeLayout(boolean computeLayout)
        {
            mComputeLayout = computeLayout;
            return this;
        }

        public MeasuredText build()
        {
            if (mBuilt) throw new IllegalStateException("Builder can not be reused.");
            if (mCurrentOffset != mText.length) throw new IllegalStateException("Style info has not been provided for all text.");

            mBuilt = true;
            return new MeasuredText(mText, mWidths, new ArrayList<>(mRuns), mHyphenationMode != HYPHENATION_MODE_NONE, mComputeLayout);
        }

        private void checkLength(int length)
        {
            if (mBuilt) throw new IllegalStateException("Builder can not be reused.");
            if (length < 0) throw new IllegalArgumentException("length can not be negative");
            if (mCurrentOffset + length > mText.length) throw new IllegalArgumentException("Style exceeds the text length");
        }
    }
}

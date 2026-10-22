package android.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.ParagraphStyle;
import java.util.List;

public abstract class Layout
{
    public static final int BREAK_STRATEGY_SIMPLE = 0;
    public static final int BREAK_STRATEGY_HIGH_QUALITY = 1;
    public static final int BREAK_STRATEGY_BALANCED = 2;

    public static final int HYPHENATION_FREQUENCY_NONE = 0;
    public static final int HYPHENATION_FREQUENCY_NORMAL = 1;
    public static final int HYPHENATION_FREQUENCY_FULL = 2;
    public static final int HYPHENATION_FREQUENCY_NORMAL_FAST = 3;
    public static final int HYPHENATION_FREQUENCY_FULL_FAST = 4;

    public static final int JUSTIFICATION_MODE_NONE = 0;
    public static final int JUSTIFICATION_MODE_INTER_WORD = 1;

    public static final int DIR_LEFT_TO_RIGHT = 1;
    public static final int DIR_RIGHT_TO_LEFT = -1;

    public static final float DEFAULT_LINESPACING_MULTIPLIER = 1.0f;
    public static final float DEFAULT_LINESPACING_ADDITION = 0.0f;

    public static final TextInclusionStrategy INCLUSION_STRATEGY_ANY_OVERLAP = RectF::intersects;
    public static final TextInclusionStrategy INCLUSION_STRATEGY_CONTAINS_CENTER =
        (segmentBounds, area) -> area.contains(segmentBounds.centerX(), segmentBounds.centerY());
    public static final TextInclusionStrategy INCLUSION_STRATEGY_CONTAINS_ALL = (segmentBounds, area) -> area.contains(segmentBounds);

    static final String ELLIPSIS = "…";
    static final Directions DIRS_ALL_LEFT_TO_RIGHT = new Directions(new int[] {0, 0x03FFFFFF});
    static final Directions DIRS_ALL_RIGHT_TO_LEFT = new Directions(new int[] {0, 0x03FFFFFF | 0x04000000});

    public enum Alignment
    {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    public interface TextInclusionStrategy
    {
        boolean isSegmentInside(RectF segmentBounds, RectF area);
    }

    public static class Directions
    {
        final int[] mDirections;

        Directions(int[] directions)
        {
            mDirections = directions;
        }
    }

    private CharSequence mText;
    private TextPaint mPaint;
    private int mWidth;
    private Alignment mAlignment;
    private float mSpacingMultiplier;
    private float mSpacingAdd;
    private boolean mSpannedText;

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd)
    {
        if (width < 0) throw new IllegalArgumentException("Layout: " + width + " < 0");

        mText = text == null ? "" : text;
        mPaint = paint;
        mWidth = width;
        mAlignment = alignment == null ? Alignment.ALIGN_NORMAL : alignment;
        mSpacingMultiplier = spacingMultiplier;
        mSpacingAdd = spacingAdd;
        mSpannedText = mText instanceof Spanned;
    }

    void replaceWith(CharSequence text, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd)
    {
        if (width < 0) throw new IllegalArgumentException("Layout: " + width + " < 0");

        mText = text == null ? "" : text;
        mPaint = paint;
        mWidth = width;
        mAlignment = alignment == null ? Alignment.ALIGN_NORMAL : alignment;
        mSpacingMultiplier = spacingMultiplier;
        mSpacingAdd = spacingAdd;
        mSpannedText = mText instanceof Spanned;
    }

    public static float getDesiredWidth(CharSequence text, TextPaint paint)
    {
        if (text == null || paint == null) return 0f;

        return getDesiredWidth(text, 0, text.length(), paint);
    }

    public static float getDesiredWidth(CharSequence text, int start, int end, TextPaint paint)
    {
        if (text == null || paint == null) return 0f;

        float widest = 0f;
        int lineStart = start;

        for (int index = start; index <= end; index++)
        {
            if (index != end && text.charAt(index) != '\n') continue;

            widest = Math.max(widest, TextLine.measure(paint, text, lineStart, index, null));
            lineStart = index + 1;
        }

        return widest;
    }

    public void draw(Canvas canvas)
    {
        draw(canvas, (Path)null, (Paint)null, 0);
    }

    public void draw(Canvas canvas, Path selectionHighlight, Paint selectionHighlightPaint, int cursorOffsetVertical)
    {
        drawBackground(canvas);
        drawHighlight(canvas, selectionHighlight, selectionHighlightPaint, cursorOffsetVertical);
        drawText(canvas);
    }

    public void draw(Canvas canvas, List<Path> highlightPaths, List<Paint> highlightPaints, Path selectionPath, Paint selectionPaint,
                     int cursorOffsetVertical)
    {
        drawBackground(canvas);

        if (highlightPaths != null && highlightPaints != null)
        {
            for (int index = 0; index < highlightPaths.size() && index < highlightPaints.size(); index++)
            {
                drawHighlight(canvas, highlightPaths.get(index), highlightPaints.get(index), cursorOffsetVertical);
            }
        }

        drawHighlight(canvas, selectionPath, selectionPaint, cursorOffsetVertical);
        drawText(canvas);
    }

    private static void drawHighlight(Canvas canvas, Path path, Paint paint, int cursorOffsetVertical)
    {
        if (canvas == null || path == null || paint == null) return;

        if (cursorOffsetVertical != 0) canvas.translate(0, cursorOffsetVertical);
        canvas.drawPath(path, paint);
        if (cursorOffsetVertical != 0) canvas.translate(0, -cursorOffsetVertical);
    }

    public void drawBackground(Canvas canvas)
    {
        if (canvas == null || !mSpannedText) return;

        Spanned spanned = (Spanned)mText;
        LineBackgroundSpan[] spans = spanned.getSpans(0, mText.length(), LineBackgroundSpan.class);
        if (spans.length == 0) return;

        int count = getLineCount();
        for (int line = 0; line < count; line++)
        {
            int start = getLineStart(line);
            int end = getLineEnd(line);

            for (LineBackgroundSpan span : spans)
            {
                int spanStart = spanned.getSpanStart(span);
                int spanEnd = spanned.getSpanEnd(span);
                if (spanStart >= end || spanEnd <= start) continue;

                span.drawBackground(canvas, mPaint, 0, mWidth, getLineTop(line), getLineBaseline(line), getLineBottom(line), mText,
                                    start, end, line);
            }
        }
    }

    public void drawText(Canvas canvas)
    {
        if (canvas == null || mPaint == null) return;

        int count = getLineCount();
        for (int line = 0; line < count; line++)
        {
            int start = getLineStart(line);
            int end = getLineVisibleEnd(line);

            int top = getLineTop(line);
            int bottom = getLineBottom(line);
            int baseline = getLineBaseline(line);

            int margin = 0;

            if (mSpannedText)
            {
                Spanned spanned = (Spanned)mText;
                int paragraphEnd = getLineEnd(line);
                LeadingMarginSpan[] margins = getParagraphSpans(spanned, start, paragraphEnd, LeadingMarginSpan.class);
                boolean first = isFirstLineOfParagraph(line);

                for (LeadingMarginSpan span : margins)
                {
                    span.drawLeadingMargin(canvas, mPaint, margin, DIR_LEFT_TO_RIGHT, top, baseline, bottom, mText, start, paragraphEnd,
                                           first, this);
                    margin += span.getLeadingMargin(isFirstLeadingMarginLine(spanned, span, line));
                }
            }

            if (end <= start) continue;

            float left = getLineLeft(line);
            int ellipsisCount = getEllipsisCount(line);

            if (ellipsisCount > 0)
            {
                int ellipsisStart = start + getEllipsisStart(line);
                int ellipsisEnd = Math.min(ellipsisStart + ellipsisCount, getLineEnd(line));

                float x = left;
                TextLine.draw(canvas, mPaint, mText, start, ellipsisStart, x, top, baseline, bottom);
                x += TextLine.measure(mPaint, mText, start, ellipsisStart, null);

                canvas.drawText(ELLIPSIS, x, baseline, mPaint);
                x += mPaint.measureText(ELLIPSIS);

                if (ellipsisEnd < end) TextLine.draw(canvas, mPaint, mText, ellipsisEnd, end, x, top, baseline, bottom);
                continue;
            }

            TextLine.draw(canvas, mPaint, mText, start, end, left, top, baseline, bottom);
        }
    }

    private boolean isFirstLineOfParagraph(int line)
    {
        int start = getLineStart(line);
        return start == 0 || mText.charAt(start - 1) == '\n';
    }

    private boolean isFirstLeadingMarginLine(Spanned spanned, LeadingMarginSpan span, int line)
    {
        int spanLine = getLineForOffset(spanned.getSpanStart(span));
        int lineCount = 1;

        if (span instanceof LeadingMarginSpan.LeadingMarginSpan2)
        {
            lineCount = ((LeadingMarginSpan.LeadingMarginSpan2)span).getLeadingMarginLineCount();
        }

        return line < spanLine + lineCount;
    }

    static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type)
    {
        if (start == end && start > 0) return text.getSpans(start - 1, start, type);

        return text.getSpans(start, end, type);
    }

    public final CharSequence getText()
    {
        return mText;
    }

    public final TextPaint getPaint()
    {
        return mPaint;
    }

    public final int getWidth()
    {
        return mWidth;
    }

    public int getEllipsizedWidth()
    {
        return mWidth;
    }

    public final void increaseWidthTo(int width)
    {
        if (width < mWidth) throw new RuntimeException("attempted to reduce Layout width");

        mWidth = width;
    }

    public int getHeight()
    {
        return getLineTop(getLineCount());
    }

    public final Alignment getAlignment()
    {
        return mAlignment;
    }

    public final float getSpacingMultiplier()
    {
        return mSpacingMultiplier;
    }

    public final float getSpacingAdd()
    {
        return mSpacingAdd;
    }

    public abstract int getLineCount();

    public int getLineBounds(int line, Rect bounds)
    {
        if (bounds != null)
        {
            bounds.left = 0;
            bounds.top = getLineTop(line);
            bounds.right = mWidth;
            bounds.bottom = getLineTop(line + 1);
        }

        return getLineBaseline(line);
    }

    public abstract int getLineTop(int line);

    public abstract int getLineDescent(int line);

    public abstract int getLineStart(int line);

    public abstract int getParagraphDirection(int line);

    public abstract boolean getLineContainsTab(int line);

    public abstract Directions getLineDirections(int line);

    public abstract int getTopPadding();

    public abstract int getBottomPadding();

    public boolean isFallbackLineSpacingEnabled()
    {
        return false;
    }

    public boolean isRtlCharAt(int offset)
    {
        if (offset < 0 || offset >= mText.length()) return false;

        byte direction = Character.getDirectionality(mText.charAt(offset));
        return direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public float getPrimaryHorizontal(int offset)
    {
        int line = getLineForOffset(offset);
        return getHorizontal(offset, line);
    }

    public float getSecondaryHorizontal(int offset)
    {
        return getPrimaryHorizontal(offset);
    }

    private float getHorizontal(int offset, int line)
    {
        int start = getLineStart(line);
        int end = getLineEnd(line);
        int clamped = Math.max(start, Math.min(offset, end));

        return getLineLeft(line) + measureLine(line, start, clamped);
    }

    private float measureLine(int line, int start, int offset)
    {
        if (mPaint == null) return 0f;

        int visibleEnd = getLineVisibleEnd(line);
        int limit = Math.min(offset, visibleEnd);
        int ellipsisCount = getEllipsisCount(line);

        if (ellipsisCount > 0)
        {
            int ellipsisStart = start + getEllipsisStart(line);
            int ellipsisEnd = ellipsisStart + ellipsisCount;

            if (limit <= ellipsisStart) return TextLine.measureTo(mPaint, mText, start, visibleEnd, limit);

            float width = TextLine.measure(mPaint, mText, start, ellipsisStart, null) + mPaint.measureText(ELLIPSIS);
            if (limit > ellipsisEnd) width += TextLine.measure(mPaint, mText, ellipsisEnd, limit, null);

            return width;
        }

        float width = TextLine.measureTo(mPaint, mText, start, visibleEnd, limit);
        if (offset > visibleEnd) width += mPaint.measureText(mText, visibleEnd, Math.min(offset, getLineEnd(line)));

        return width;
    }

    public void fillCharacterBounds(int start, int end, float[] bounds, int boundsStart)
    {
        if (start < 0 || end < start || end > mText.length()) throw new IndexOutOfBoundsException();
        if (bounds == null) throw new IllegalArgumentException("bounds can't be null.");
        if (boundsStart < 0 || bounds.length - boundsStart < 4 * (end - start)) throw new IndexOutOfBoundsException();

        for (int offset = start; offset < end; offset++)
        {
            int line = getLineForOffset(offset);
            float left = getHorizontal(offset, line);
            float right = getHorizontal(offset + 1, line);
            int index = boundsStart + 4 * (offset - start);

            bounds[index] = left;
            bounds[index + 1] = getLineTop(line);
            bounds[index + 2] = Math.max(left, right);
            bounds[index + 3] = getLineBottom(line);
        }
    }

    public float getLineLeft(int line)
    {
        Alignment alignment = getParagraphAlignment(line);
        int margin = getParagraphLeadingMargin(line);
        float width = getLineMax(line) - margin;

        if (alignment == Alignment.ALIGN_CENTER) return margin + (float)Math.floor((mWidth - margin - width) / 2f);
        if (alignment == Alignment.ALIGN_RIGHT) return mWidth - width;

        if (alignment == Alignment.ALIGN_OPPOSITE)
        {
            return getParagraphDirection(line) == DIR_RIGHT_TO_LEFT ? margin : mWidth - width;
        }

        if (alignment == Alignment.ALIGN_NORMAL && getParagraphDirection(line) == DIR_RIGHT_TO_LEFT) return mWidth - width;

        return margin;
    }

    public float getLineRight(int line)
    {
        return getLineLeft(line) + getLineMax(line) - getParagraphLeadingMargin(line);
    }

    public float getLineMax(int line)
    {
        return getParagraphLeadingMargin(line) + getLineWidthInternal(line, false);
    }

    public float getLineWidth(int line)
    {
        return getParagraphLeadingMargin(line) + getLineWidthInternal(line, true);
    }

    private float getLineWidthInternal(int line, boolean full)
    {
        int start = getLineStart(line);
        int end = full ? getLineEnd(line) : getLineVisibleEnd(line);
        if (end > 0 && end <= mText.length() && end > start && mText.charAt(end - 1) == '\n') end--;

        return measureLine(line, start, end);
    }

    public int getLineForVertical(int vertical)
    {
        int high = getLineCount();
        int low = -1;

        while (high - low > 1)
        {
            int guess = (high + low) / 2;

            if (getLineTop(guess) > vertical) high = guess;
            else low = guess;
        }

        return Math.max(0, low);
    }

    public int getLineForOffset(int offset)
    {
        int high = getLineCount();
        int low = -1;

        while (high - low > 1)
        {
            int guess = (high + low) / 2;

            if (getLineStart(guess) > offset) high = guess;
            else low = guess;
        }

        return Math.max(0, Math.min(low, getLineCount() - 1));
    }

    public int getOffsetForHorizontal(int line, float horizontal)
    {
        int start = getLineStart(line);
        int end = getLineVisibleEnd(line);
        if (mPaint == null) return start;

        float left = getLineLeft(line);
        int offset = TextLine.offsetForAdvance(mPaint, mText, start, end, horizontal - left);

        int lineEnd = getLineEnd(line);
        if (offset == end && line < getLineCount() - 1 && end == lineEnd) offset = TextUtils.getOffsetBefore(mText, lineEnd);

        return offset;
    }

    public int[] getRangeForRect(RectF area, SegmentFinder segmentFinder, TextInclusionStrategy inclusionStrategy)
    {
        int startLine = getLineForVertical((int)area.top);
        int endLine = getLineForVertical((int)area.bottom);

        int rangeStart = -1;
        int rangeEnd = -1;
        RectF bounds = new RectF();

        for (int line = startLine; line <= endLine; line++)
        {
            int lineStart = getLineStart(line);
            int lineEnd = getLineEnd(line);

            int segmentStart = segmentFinder.nextStartBoundary(lineStart - 1);
            if (segmentStart == SegmentFinder.DONE) segmentStart = lineStart;

            while (segmentStart != SegmentFinder.DONE && segmentStart < lineEnd)
            {
                int segmentEnd = segmentFinder.nextEndBoundary(segmentStart);
                if (segmentEnd == SegmentFinder.DONE) break;

                int clampedEnd = Math.min(segmentEnd, lineEnd);
                bounds.set(getHorizontal(segmentStart, line), getLineTop(line), getHorizontal(clampedEnd, line), getLineBottom(line));

                if (inclusionStrategy.isSegmentInside(bounds, area))
                {
                    if (rangeStart < 0) rangeStart = segmentStart;
                    rangeEnd = Math.max(rangeEnd, segmentEnd);
                }

                segmentStart = segmentFinder.nextStartBoundary(segmentStart);
            }
        }

        if (rangeStart < 0) return null;

        return new int[] {rangeStart, rangeEnd};
    }

    public final int getLineEnd(int line)
    {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line)
    {
        int start = getLineStart(line);
        int end = getLineEnd(line);

        if (line == getLineCount() - 1) end = Math.min(end, mText.length());

        while (end > start)
        {
            char character = mText.charAt(end - 1);
            if (character == '\n') return end - 1;
            if (character != ' ' && character != '\t') break;

            end--;
        }

        return end;
    }

    public final int getLineBottom(int line)
    {
        return getLineTop(line + 1);
    }

    public int getLineBottom(int line, boolean includeLineSpacing)
    {
        return getLineTop(line + 1);
    }

    public final int getLineBaseline(int line)
    {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineAscent(int line)
    {
        return getLineTop(line) - (getLineTop(line + 1) - getLineDescent(line));
    }

    public int getOffsetToLeftOf(int offset)
    {
        return Math.max(0, TextUtils.getOffsetBefore(mText, offset));
    }

    public int getOffsetToRightOf(int offset)
    {
        return Math.min(mText.length(), TextUtils.getOffsetAfter(mText, offset));
    }

    public void getCursorPath(int point, Path destination, CharSequence editingBuffer)
    {
        destination.reset();

        int line = getLineForOffset(point);
        int top = getLineTop(line);
        int bottom = getLineBottom(line);
        float horizontal = getPrimaryHorizontal(point) - 0.5f;

        destination.moveTo(horizontal, top);
        destination.lineTo(horizontal, bottom);
    }

    public void getSelectionPath(int start, int end, Path destination)
    {
        destination.reset();
        if (start == end) return;

        if (end < start)
        {
            int swap = end;
            end = start;
            start = swap;
        }

        int startLine = getLineForOffset(start);
        int endLine = getLineForOffset(end);

        for (int line = startLine; line <= endLine; line++)
        {
            int top = getLineTop(line);
            int bottom = getLineBottom(line);

            float left = line == startLine ? getPrimaryHorizontal(start) : getLineLeft(line);
            float right = line == endLine ? getPrimaryHorizontal(end) : (line == startLine ? mWidth : getLineRight(line));

            if (line != startLine && line != endLine) right = mWidth;
            if (line != startLine) left = 0;

            destination.addRect(left, top, Math.max(left, right), bottom, Path.Direction.CW);
        }
    }

    public final Alignment getParagraphAlignment(int line)
    {
        if (!mSpannedText) return mAlignment;

        Spanned spanned = (Spanned)mText;
        AlignmentSpan[] spans = getParagraphSpans(spanned, getLineStart(line), getLineEnd(line), AlignmentSpan.class);

        if (spans.length > 0) return spans[spans.length - 1].getAlignment();

        return mAlignment;
    }

    public final int getParagraphLeft(int line)
    {
        if (getParagraphDirection(line) == DIR_RIGHT_TO_LEFT || !mSpannedText) return 0;

        return getParagraphLeadingMargin(line);
    }

    public final int getParagraphRight(int line)
    {
        if (getParagraphDirection(line) == DIR_LEFT_TO_RIGHT || !mSpannedText) return mWidth;

        return mWidth - getParagraphLeadingMargin(line);
    }

    int getParagraphLeadingMargin(int line)
    {
        if (!mSpannedText) return 0;

        Spanned spanned = (Spanned)mText;
        int start = getLineStart(line);
        int end = getLineEnd(line);

        int next = spanned.nextSpanTransition(start, end, LeadingMarginSpan.class);
        LeadingMarginSpan[] spans = getParagraphSpans(spanned, start, next, LeadingMarginSpan.class);
        if (spans.length == 0) return 0;

        int margin = 0;
        for (LeadingMarginSpan span : spans)
        {
            margin += span.getLeadingMargin(isFirstLeadingMarginLine(spanned, span, line));
        }

        return margin;
    }

    protected final boolean isSpanned()
    {
        return mSpannedText;
    }

    static boolean hasParagraphStyle(CharSequence text)
    {
        if (!(text instanceof Spanned)) return false;

        return ((Spanned)text).getSpans(0, text.length(), ParagraphStyle.class).length > 0;
    }

    public abstract int getEllipsisStart(int line);

    public abstract int getEllipsisCount(int line);
}

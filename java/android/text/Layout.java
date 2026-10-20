package android.text;

import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class Layout
{
    public enum Alignment
    {
        ALIGN_NORMAL,
        ALIGN_OPPOSITE,
        ALIGN_CENTER,
        ALIGN_LEFT,
        ALIGN_RIGHT
    }

    private final CharSequence mText;
    private final TextPaint mPaint;
    private final int mWidth;
    private final Alignment mAlignment;
    private final float mSpacingMultiplier;
    private final float mSpacingAdd;

    protected Layout(CharSequence text, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd)
    {
        if (width < 0) throw new IllegalArgumentException("Layout: " + width + " < 0");

        mText = text == null ? "" : text;
        mPaint = paint;
        mWidth = width;
        mAlignment = alignment == null ? Alignment.ALIGN_NORMAL : alignment;
        mSpacingMultiplier = spacingMultiplier;
        mSpacingAdd = spacingAdd;
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

    public abstract int getLineTop(int line);

    public abstract int getLineStart(int line);

    public abstract int getLineDescent(int line);

    public abstract int getEllipsisCount(int line);

    public final int getHeight()
    {
        return getLineTop(getLineCount());
    }

    public final int getLineBottom(int line)
    {
        return getLineTop(line + 1);
    }

    public int getLineAscent(int line)
    {
        return getLineTop(line) - getLineBaseline(line);
    }

    public int getTopPadding()
    {
        return 0;
    }

    public int getBottomPadding()
    {
        return 0;
    }

    public int getParagraphDirection(int line)
    {
        return 1;
    }

    public boolean isRtlCharAt(int offset)
    {
        return false;
    }

    public int getOffsetForHorizontal(int line, float horizontal)
    {
        return getLineStart(line);
    }

    public float getSecondaryHorizontal(int offset)
    {
        return getPrimaryHorizontal(offset);
    }

    public final int getLineBaseline(int line)
    {
        return getLineTop(line + 1) - getLineDescent(line);
    }

    public final int getLineEnd(int line)
    {
        return getLineStart(line + 1);
    }

    public int getLineVisibleEnd(int line)
    {
        int end = getLineEnd(line);
        int start = getLineStart(line);

        while (end > start)
        {
            char character = mText.charAt(end - 1);
            if (character != '\n' && character != '\r' && character != ' ') break;
            end--;
        }

        return end;
    }

    public float getLineWidth(int line)
    {
        return mPaint == null ? 0f : mPaint.measureText(mText, getLineStart(line), getLineVisibleEnd(line));
    }

    public float getLineMax(int line)
    {
        return getLineWidth(line);
    }

    public int getLineForOffset(int offset)
    {
        int count = getLineCount();
        for (int line = 0; line < count; line++)
        {
            if (offset < getLineEnd(line)) return line;
        }
        return Math.max(0, count - 1);
    }

    public int getLineForVertical(int vertical)
    {
        int count = getLineCount();
        for (int line = 0; line < count; line++)
        {
            if (vertical < getLineBottom(line)) return line;
        }
        return Math.max(0, count - 1);
    }

    public float getPrimaryHorizontal(int offset)
    {
        int line = getLineForOffset(offset);
        return getLineLeft(line) + (mPaint == null ? 0f : mPaint.measureText(mText, getLineStart(line), offset));
    }

    public float getLineLeft(int line)
    {
        if (mAlignment == Alignment.ALIGN_CENTER) return (mWidth - getLineWidth(line)) / 2f;
        if (mAlignment == Alignment.ALIGN_RIGHT || mAlignment == Alignment.ALIGN_OPPOSITE) return mWidth - getLineWidth(line);
        return 0f;
    }

    public float getLineRight(int line)
    {
        return getLineLeft(line) + getLineWidth(line);
    }

    public int getLineBounds(int line, Rect bounds)
    {
        if (bounds != null)
        {
            bounds.left = 0;
            bounds.top = getLineTop(line);
            bounds.right = mWidth;
            bounds.bottom = getLineBottom(line);
        }

        return getLineBaseline(line);
    }

    public void draw(Canvas canvas)
    {
        if (canvas == null || mPaint == null) return;

        int count = getLineCount();
        for (int line = 0; line < count; line++)
        {
            int start = getLineStart(line);
            int end = getLineVisibleEnd(line);
            if (end <= start) continue;

            CharSequence slice = mText.subSequence(start, end);
            canvas.drawText(slice.toString(), getLineLeft(line), getLineBaseline(line), mPaint);
        }
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

            widest = Math.max(widest, paint.measureText(text, lineStart, index));
            lineStart = index + 1;
        }

        return widest;
    }
}

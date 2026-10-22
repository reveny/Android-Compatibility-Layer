package android.text;

import java.text.BreakIterator;

public class GraphemeClusterSegmentFinder extends SegmentFinder
{
    private final CharSequence mText;
    private final BreakIterator mBreaker;

    public GraphemeClusterSegmentFinder(CharSequence text, TextPaint textPaint)
    {
        mText = text;
        mBreaker = BreakIterator.getCharacterInstance();
        mBreaker.setText(text.toString());
    }

    @Override
    public int previousStartBoundary(int offset)
    {
        if (offset <= 0) return DONE;

        int boundary = mBreaker.preceding(Math.min(offset, mText.length()));
        return boundary == BreakIterator.DONE ? DONE : boundary;
    }

    @Override
    public int previousEndBoundary(int offset)
    {
        int boundary = previousStartBoundary(offset);
        if (boundary == DONE || boundary == 0) return DONE;

        return boundary;
    }

    @Override
    public int nextStartBoundary(int offset)
    {
        if (offset >= mText.length()) return DONE;
        if (offset < 0) return mText.length() == 0 ? DONE : 0;

        int boundary = mBreaker.following(offset);
        if (boundary == BreakIterator.DONE || boundary >= mText.length()) return DONE;

        return boundary;
    }

    @Override
    public int nextEndBoundary(int offset)
    {
        if (offset >= mText.length()) return DONE;

        int boundary = mBreaker.following(Math.max(0, offset));
        return boundary == BreakIterator.DONE ? DONE : boundary;
    }
}

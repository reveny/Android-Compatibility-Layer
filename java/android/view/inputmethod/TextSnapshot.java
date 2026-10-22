package android.view.inputmethod;

public final class TextSnapshot
{
    private final SurroundingText mSurroundingText;
    private final int mCompositionStart;
    private final int mCompositionEnd;
    private final int mCursorCapsMode;

    public TextSnapshot(SurroundingText surroundingText, int compositionStart, int compositionEnd, int cursorCapsMode)
    {
        if (surroundingText == null) throw new NullPointerException("surroundingText");
        if (compositionStart < -1) throw new IllegalArgumentException("compositionStart must be -1 or higher but was " + compositionStart);
        if (compositionEnd < -1) throw new IllegalArgumentException("compositionEnd must be -1 or higher but was " + compositionEnd);

        if (compositionStart == -1 && compositionEnd != -1)
        {
            throw new IllegalArgumentException("compositionEnd must be -1 if compositionStart is -1 but was " + compositionEnd);
        }

        if (compositionStart != -1 && compositionEnd == -1)
        {
            throw new IllegalArgumentException("compositionStart must be -1 if compositionEnd is -1 but was " + compositionStart);
        }

        if (compositionStart > compositionEnd)
        {
            throw new IllegalArgumentException("compositionStart=" + compositionStart + " must be equal to or greater than compositionEnd="
                                               + compositionEnd);
        }

        mSurroundingText = surroundingText;
        mCompositionStart = compositionStart;
        mCompositionEnd = compositionEnd;
        mCursorCapsMode = cursorCapsMode;
    }

    public SurroundingText getSurroundingText()
    {
        return mSurroundingText;
    }

    public int getSelectionStart()
    {
        return mSurroundingText.getOffset() < 0 ? -1 : mSurroundingText.getSelectionStart() + mSurroundingText.getOffset();
    }

    public int getSelectionEnd()
    {
        return mSurroundingText.getOffset() < 0 ? -1 : mSurroundingText.getSelectionEnd() + mSurroundingText.getOffset();
    }

    public int getCompositionStart()
    {
        return mCompositionStart;
    }

    public int getCompositionEnd()
    {
        return mCompositionEnd;
    }

    public int getCursorCapsMode()
    {
        return mCursorCapsMode;
    }
}

package android.view.inputmethod;

public class CorrectionInfo
{
    private final int mOffset;
    private final CharSequence mOldText;
    private final CharSequence mNewText;

    public CorrectionInfo(int offset, CharSequence oldText, CharSequence newText)
    {
        mOffset = offset;
        mOldText = oldText;
        mNewText = newText;
    }

    public int getOffset()
    {
        return mOffset;
    }

    public CharSequence getOldText()
    {
        return mOldText;
    }

    public CharSequence getNewText()
    {
        return mNewText;
    }
}

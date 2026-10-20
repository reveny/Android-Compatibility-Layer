package android.view.inputmethod;

public class CompletionInfo
{
    private final long mId;
    private final int mPosition;
    private final CharSequence mText;
    private final CharSequence mLabel;

    public CompletionInfo(long id, int index, CharSequence text)
    {
        this(id, index, text, null);
    }

    public CompletionInfo(long id, int index, CharSequence text, CharSequence label)
    {
        mId = id;
        mPosition = index;
        mText = text;
        mLabel = label;
    }

    public long getId()
    {
        return mId;
    }

    public int getPosition()
    {
        return mPosition;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }
}

package android.view.inputmethod;

public final class TextBoundsInfoResult
{
    public static final int CODE_UNSUPPORTED = 0;
    public static final int CODE_SUCCESS = 1;
    public static final int CODE_FAILED = 2;
    public static final int CODE_CANCELLED = 3;

    private final int mResultCode;
    private final TextBoundsInfo mTextBoundsInfo;

    public TextBoundsInfoResult(int resultCode)
    {
        this(resultCode, null);
    }

    public TextBoundsInfoResult(int resultCode, TextBoundsInfo textBoundsInfo)
    {
        if (resultCode == CODE_SUCCESS && textBoundsInfo == null)
        {
            throw new IllegalStateException("TextBoundsInfo must be provided when the resultCode is CODE_SUCCESS.");
        }

        mResultCode = resultCode;
        mTextBoundsInfo = textBoundsInfo;
    }

    public int getResultCode()
    {
        return mResultCode;
    }

    public TextBoundsInfo getTextBoundsInfo()
    {
        return mTextBoundsInfo;
    }
}

package android.webkit;

public class ConsoleMessage
{
    private final MessageLevel mLevel;
    private final String mMessage;
    private final String mSourceId;
    private final int mLineNumber;

    public ConsoleMessage(String message, String sourceId, int lineNumber, MessageLevel messageLevel)
    {
        mMessage = message;
        mSourceId = sourceId;
        mLineNumber = lineNumber;
        mLevel = messageLevel;
    }

    public MessageLevel messageLevel()
    {
        return mLevel;
    }

    public String message()
    {
        return mMessage;
    }

    public String sourceId()
    {
        return mSourceId;
    }

    public int lineNumber()
    {
        return mLineNumber;
    }

    public enum MessageLevel
    {
        TIP,
        LOG,
        WARNING,
        ERROR,
        DEBUG
    }
}

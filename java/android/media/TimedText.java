package android.media;

import android.graphics.Rect;

public final class TimedText
{
    private final String mText;
    private final Rect mBounds;

    TimedText(String text, Rect bounds)
    {
        mText = text;
        mBounds = bounds;
    }

    public Rect getBounds()
    {
        return mBounds;
    }

    public String getText()
    {
        return mText;
    }
}

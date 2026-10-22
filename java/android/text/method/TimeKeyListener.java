package android.text.method;

import android.text.InputType;

import java.util.Locale;

public class TimeKeyListener extends NumberKeyListener
{
    public static final char[] CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'm', 'p', ':'};

    private static TimeKeyListener sInstance;

    private final char[] mCharacters;

    public TimeKeyListener()
    {
        this(null);
    }

    public TimeKeyListener(Locale locale)
    {
        mCharacters = CHARACTERS;
    }

    @Override
    public int getInputType()
    {
        return InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME;
    }

    @Override
    protected char[] getAcceptedChars()
    {
        return mCharacters;
    }

    public static TimeKeyListener getInstance()
    {
        if (sInstance == null) sInstance = new TimeKeyListener();

        return sInstance;
    }

    public static TimeKeyListener getInstance(Locale locale)
    {
        return getInstance();
    }
}

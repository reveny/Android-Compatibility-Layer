package android.text.method;

import android.text.InputType;

import java.util.Locale;

public class DateTimeKeyListener extends NumberKeyListener
{
    public static final char[] CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'm', 'p', ':', '/', '-', ' '};

    private static DateTimeKeyListener sInstance;

    private final char[] mCharacters;

    public DateTimeKeyListener()
    {
        this(null);
    }

    public DateTimeKeyListener(Locale locale)
    {
        mCharacters = CHARACTERS;
    }

    @Override
    public int getInputType()
    {
        return InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_NORMAL;
    }

    @Override
    protected char[] getAcceptedChars()
    {
        return mCharacters;
    }

    public static DateTimeKeyListener getInstance()
    {
        if (sInstance == null) sInstance = new DateTimeKeyListener();

        return sInstance;
    }

    public static DateTimeKeyListener getInstance(Locale locale)
    {
        return getInstance();
    }
}

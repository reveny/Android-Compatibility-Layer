package android.text.method;

import android.text.InputType;

import java.util.Locale;

public class DateKeyListener extends NumberKeyListener
{
    public static final char[] CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '/', '-', '.'};

    private static DateKeyListener sInstance;

    private final char[] mCharacters;

    public DateKeyListener()
    {
        this(null);
    }

    public DateKeyListener(Locale locale)
    {
        mCharacters = CHARACTERS;
    }

    @Override
    public int getInputType()
    {
        return InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE;
    }

    @Override
    protected char[] getAcceptedChars()
    {
        return mCharacters;
    }

    public static DateKeyListener getInstance()
    {
        if (sInstance == null) sInstance = new DateKeyListener();

        return sInstance;
    }

    public static DateKeyListener getInstance(Locale locale)
    {
        return getInstance();
    }
}

package android.text.method;

import android.text.Editable;
import android.text.Spannable;

public class TextKeyListener
{
    public enum Capitalize
    {
        NONE,
        SENTENCES,
        WORDS,
        CHARACTERS
    }

    private static final TextKeyListener INSTANCE = new TextKeyListener(Capitalize.NONE, false);

    private final Capitalize mCapitalize;
    private final boolean mAutoText;

    public TextKeyListener(Capitalize capitalize, boolean autoText)
    {
        mCapitalize = capitalize;
        mAutoText = autoText;
    }

    public static TextKeyListener getInstance()
    {
        return INSTANCE;
    }

    public static TextKeyListener getInstance(boolean autoText, Capitalize capitalize)
    {
        return new TextKeyListener(capitalize, autoText);
    }

    public Capitalize getCapitalize()
    {
        return mCapitalize;
    }

    public boolean getAutoText()
    {
        return mAutoText;
    }

    public static void clear(Editable text)
    {
        if (text != null) text.clear();
    }

    public int getInputType()
    {
        return 0;
    }

    public void release()
    {
    }
}

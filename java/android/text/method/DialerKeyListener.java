package android.text.method;

import android.text.InputType;
import android.text.Spannable;
import android.view.KeyEvent;

public class DialerKeyListener extends NumberKeyListener
{
    public static final char[] CHARACTERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '#', '*', '+', '-', '(', ')', ',', '/', 'N',
                                             '.', ' ', ';'};

    private static DialerKeyListener sInstance;

    public DialerKeyListener()
    {
    }

    @Override
    protected char[] getAcceptedChars()
    {
        return CHARACTERS;
    }

    public static DialerKeyListener getInstance()
    {
        if (sInstance == null) sInstance = new DialerKeyListener();

        return sInstance;
    }

    @Override
    public int getInputType()
    {
        return InputType.TYPE_CLASS_PHONE;
    }

    @Override
    protected int lookup(KeyEvent event, Spannable content)
    {
        int meta = event.getMetaState() | getMetaState(content);
        char number = event.getNumber();

        if ((meta & (MetaKeyKeyListener.META_ALT_ON | MetaKeyKeyListener.META_SHIFT_ON)) == 0 && number != 0) return number;

        int match = super.lookup(event, content);
        if (match != 0) return match;

        if (meta != 0)
        {
            int unicode = event.getUnicodeChar(0);
            if (unicode != 0 && ok(getAcceptedChars(), (char)unicode)) return unicode;
        }

        return 0;
    }
}

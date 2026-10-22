package android.text.method;

import android.text.Editable;
import android.text.InputType;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

public class QwertyKeyListener extends BaseKeyListener
{
    private static final QwertyKeyListener[] sInstance = new QwertyKeyListener[TextKeyListener.Capitalize.values().length * 2];
    private static QwertyKeyListener sFullKeyboardInstance;

    private final TextKeyListener.Capitalize mAutoCap;
    private final boolean mAutoText;
    private final boolean mFullKeyboard;

    private QwertyKeyListener(TextKeyListener.Capitalize capitalize, boolean autoText, boolean fullKeyboard)
    {
        mAutoCap = capitalize;
        mAutoText = autoText;
        mFullKeyboard = fullKeyboard;
    }

    public QwertyKeyListener(TextKeyListener.Capitalize capitalize, boolean autoText)
    {
        this(capitalize, autoText, false);
    }

    public static QwertyKeyListener getInstance(boolean autoText, TextKeyListener.Capitalize capitalize)
    {
        int index = capitalize.ordinal() * 2 + (autoText ? 1 : 0);

        synchronized (sInstance)
        {
            if (sInstance[index] == null) sInstance[index] = new QwertyKeyListener(capitalize, autoText);

            return sInstance[index];
        }
    }

    public static QwertyKeyListener getInstanceForFullKeyboard()
    {
        if (sFullKeyboardInstance == null)
        {
            sFullKeyboardInstance = new QwertyKeyListener(TextKeyListener.Capitalize.NONE, false, true);
        }

        return sFullKeyboardInstance;
    }

    @Override
    public int getInputType()
    {
        int type = InputType.TYPE_CLASS_TEXT;

        if (mAutoCap == TextKeyListener.Capitalize.CHARACTERS) type |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
        else if (mAutoCap == TextKeyListener.Capitalize.WORDS) type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        else if (mAutoCap == TextKeyListener.Capitalize.SENTENCES) type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

        if (mAutoText) type |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        return type;
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL)
        {
            return super.onKeyDown(view, content, keyCode, event);
        }

        if (event == null) return false;

        int unicode = event.getUnicodeChar();
        if (unicode == 0 || (unicode & KeyCharacterMap.COMBINING_ACCENT) != 0)
        {
            return super.onKeyDown(view, content, keyCode, event);
        }

        int first = Selection.getSelectionStart(content);
        int second = Selection.getSelectionEnd(content);
        if (first < 0 || second < 0) first = second = content.length();

        int selectionStart = Math.min(first, second);
        int selectionEnd = Math.max(first, second);

        if (!mFullKeyboard && TextKeyListener.shouldCap(mAutoCap, content, selectionStart)) unicode = Character.toUpperCase(unicode);

        content.replace(selectionStart, selectionEnd, new String(Character.toChars(unicode)));
        Selection.setSelection(content, Math.min(selectionStart + Character.charCount(unicode), content.length()));
        adjustMetaAfterKeypress(content);
        return true;
    }

    public static void markAsReplaced(Spannable content, int start, int end, String original)
    {
        Replaced[] replaced = content.getSpans(0, content.length(), Replaced.class);
        for (Replaced span : replaced) content.removeSpan(span);

        int length = original.length();
        char[] text = new char[length];
        original.getChars(0, length, text, 0);

        content.setSpan(new Replaced(text), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static class Replaced implements NoCopySpan
    {
        private final char[] mText;

        Replaced(char[] text)
        {
            mText = text;
        }

        char[] getText()
        {
            return mText;
        }
    }
}

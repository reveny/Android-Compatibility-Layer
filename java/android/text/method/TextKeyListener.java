package android.text.method;

import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.View;

public class TextKeyListener extends BaseKeyListener implements SpanWatcher
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

    public static boolean shouldCap(Capitalize capitalize, CharSequence content, int offset)
    {
        if (capitalize == Capitalize.NONE) return false;
        if (capitalize == Capitalize.CHARACTERS) return true;

        int index = offset - 1;
        while (index >= 0 && Character.isWhitespace(content.charAt(index))) index--;
        if (index < 0) return true;

        if (capitalize == Capitalize.WORDS) return index < offset - 1;

        char previous = content.charAt(index);
        return previous == '.' || previous == '!' || previous == '?';
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

    @Override
    public int getInputType()
    {
        int type = InputType.TYPE_CLASS_TEXT;

        if (mCapitalize == Capitalize.CHARACTERS) type |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
        else if (mCapitalize == Capitalize.WORDS) type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        else if (mCapitalize == Capitalize.SENTENCES) type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

        if (mAutoText) type |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        return type;
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event)
    {
        if (super.onKeyDown(view, content, keyCode, event)) return true;
        if (event == null) return false;

        int unicode = event.getUnicodeChar();
        if (unicode == 0) return false;

        int start = Selection.getSelectionStart(content);
        int end = Selection.getSelectionEnd(content);
        if (start < 0 || end < 0) start = end = content.length();

        int from = Math.min(start, end);
        int to = Math.max(start, end);

        if (shouldCap(mCapitalize, content, from)) unicode = Character.toUpperCase(unicode);

        content.replace(from, to, new String(Character.toChars(unicode)));
        Selection.setSelection(content, from + Character.charCount(unicode));
        return true;
    }

    @Override
    public boolean onKeyUp(View view, Editable content, int keyCode, KeyEvent event)
    {
        return false;
    }

    @Override
    public boolean onKeyOther(View view, Editable content, KeyEvent event)
    {
        if (event == null || event.getAction() != KeyEvent.ACTION_MULTIPLE || event.getKeyCode() != KeyEvent.KEYCODE_UNKNOWN) return false;

        String characters = event.getCharacters();
        if (characters == null) return false;

        int start = Selection.getSelectionStart(content);
        int end = Selection.getSelectionEnd(content);
        if (start < 0 || end < 0) start = end = content.length();

        int from = Math.min(start, end);
        int to = Math.max(start, end);

        content.replace(from, to, characters);
        Selection.setSelection(content, from + characters.length());
        return true;
    }

    public void release()
    {
    }

    @Override
    public void onSpanAdded(Spannable text, Object what, int start, int end)
    {
    }

    @Override
    public void onSpanRemoved(Spannable text, Object what, int start, int end)
    {
    }

    @Override
    public void onSpanChanged(Spannable text, Object what, int start, int end, int startOld, int endOld)
    {
    }
}

package android.text.method;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;

public abstract class NumberKeyListener extends BaseKeyListener implements InputFilter
{
    public NumberKeyListener()
    {
    }

    protected abstract char[] getAcceptedChars();

    protected int lookup(KeyEvent event, Spannable content)
    {
        return event.getMatch(getAcceptedChars(), event.getMetaState() | getMetaState(content));
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd)
    {
        char[] accepted = getAcceptedChars();

        int index;
        for (index = start; index < end; index++)
        {
            if (!ok(accepted, source.charAt(index))) break;
        }

        if (index == end) return null;
        if (end - start == 1) return "";

        SpannableStringBuilder filtered = new SpannableStringBuilder(source, start, end);
        int first = index - start;

        for (int position = end - start - 1; position >= first; position--)
        {
            if (!ok(accepted, filtered.charAt(position))) filtered.delete(position, position + 1);
        }

        return filtered;
    }

    protected static boolean ok(char[] accept, char character)
    {
        for (int index = accept.length - 1; index >= 0; index--)
        {
            if (accept[index] == character) return true;
        }

        return false;
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event)
    {
        int first = Selection.getSelectionStart(content);
        int second = Selection.getSelectionEnd(content);

        int selectionStart = Math.min(first, second);
        int selectionEnd = Math.max(first, second);

        if (selectionStart < 0 || selectionEnd < 0)
        {
            selectionStart = selectionEnd = 0;
            Selection.setSelection(content, 0);
        }

        int character = event != null ? lookup(event, content) : 0;
        int repeatCount = event != null ? event.getRepeatCount() : 0;

        if (repeatCount == 0)
        {
            if (character != 0)
            {
                if (selectionStart != selectionEnd) Selection.setSelection(content, selectionEnd);

                content.replace(selectionStart, selectionEnd, String.valueOf((char)character));
                Selection.setSelection(content, Math.min(selectionStart + 1, content.length()));
                adjustMetaAfterKeypress(content);
                return true;
            }
        }
        else if (character == '0' && repeatCount == 1)
        {
            if (selectionStart == selectionEnd && selectionEnd > 0 && content.charAt(selectionStart - 1) == '0')
            {
                content.replace(selectionStart - 1, selectionEnd, String.valueOf('+'));
                Selection.setSelection(content, Math.min(selectionStart, content.length()));
                adjustMetaAfterKeypress(content);
                return true;
            }
        }

        adjustMetaAfterKeypress(content);
        return super.onKeyDown(view, content, keyCode, event);
    }
}

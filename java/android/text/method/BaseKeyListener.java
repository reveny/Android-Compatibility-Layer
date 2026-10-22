package android.text.method;

import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.View;

public abstract class BaseKeyListener extends MetaKeyKeyListener implements KeyListener
{
    public BaseKeyListener()
    {
    }

    public boolean backspace(View view, Editable content, int keyCode, KeyEvent event)
    {
        return deleteAround(content, false);
    }

    public boolean forwardDelete(View view, Editable content, int keyCode, KeyEvent event)
    {
        return deleteAround(content, true);
    }

    private static boolean deleteAround(Editable content, boolean forward)
    {
        int start = Selection.getSelectionStart(content);
        int end = Selection.getSelectionEnd(content);
        if (start < 0 || end < 0) return false;

        if (start != end)
        {
            content.delete(Math.min(start, end), Math.max(start, end));
            return true;
        }

        if (forward)
        {
            if (end >= content.length()) return false;
            content.delete(end, Character.isHighSurrogate(content.charAt(end)) && end + 1 < content.length() ? end + 2 : end + 1);
            return true;
        }

        if (start == 0) return false;

        int from = start - 1;
        if (from > 0 && Character.isLowSurrogate(content.charAt(from)) && Character.isHighSurrogate(content.charAt(from - 1))) from--;

        content.delete(from, start);
        return true;
    }

    @Override
    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DEL) return backspace(view, content, keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) return forwardDelete(view, content, keyCode, event);

        return super.onKeyDown(view, content, keyCode, event);
    }

    @Override
    public boolean onKeyOther(View view, Editable content, KeyEvent event)
    {
        return false;
    }
}

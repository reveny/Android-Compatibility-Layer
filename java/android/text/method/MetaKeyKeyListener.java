package android.text.method;

import android.text.Editable;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.View;

public abstract class MetaKeyKeyListener
{
    public static final int META_SHIFT_ON = KeyEvent.META_SHIFT_ON;
    public static final int META_ALT_ON = KeyEvent.META_ALT_ON;
    public static final int META_SYM_ON = 0x4;
    public static final int META_CAP_LOCKED = 0x100;
    public static final int META_ALT_LOCKED = 0x200;
    public static final int META_SYM_LOCKED = 0x400;

    public MetaKeyKeyListener()
    {
    }

    public static void resetMetaState(Spannable text)
    {
    }

    public static final int getMetaState(CharSequence text)
    {
        return 0;
    }

    public static final int getMetaState(CharSequence text, int meta)
    {
        return 0;
    }

    public static final int getMetaState(CharSequence text, KeyEvent event)
    {
        int metaState = event == null ? 0 : event.getMetaState();
        return metaState | getMetaState(text);
    }

    public static final int getMetaState(CharSequence text, int meta, KeyEvent event)
    {
        int metaState = event == null ? 0 : event.getMetaState();
        if ((metaState & meta) != 0) return 1;

        return getMetaState(text, meta);
    }

    public static final int getMetaState(long state)
    {
        return (int)(state & 0xFFFFFFFFL);
    }

    public static final int getMetaState(long state, int meta)
    {
        return (getMetaState(state) & meta) != 0 ? 1 : 0;
    }

    public static void adjustMetaAfterKeypress(Spannable content)
    {
    }

    public static boolean isMetaTracker(CharSequence text, Object what)
    {
        return false;
    }

    public static boolean isSelectingMetaTracker(CharSequence text, Object what)
    {
        return false;
    }

    protected static void resetLockedMeta(Spannable content)
    {
    }

    public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean onKeyUp(View view, Editable content, int keyCode, KeyEvent event)
    {
        return false;
    }

    public void clearMetaKeyState(View view, Editable content, int states)
    {
    }

    public static void clearMetaKeyState(Editable content, int states)
    {
    }

    public static long resetLockedMeta(long state)
    {
        return state & ~(long)(META_CAP_LOCKED | META_ALT_LOCKED | META_SYM_LOCKED);
    }

    public static long adjustMetaAfterKeypress(long state)
    {
        return state;
    }

    public static long handleKeyDown(long state, int keyCode, KeyEvent event)
    {
        return event == null ? state : state | (event.getMetaState() & 0xFFFFFFFFL);
    }

    public static long handleKeyUp(long state, int keyCode, KeyEvent event)
    {
        return event == null ? state : state & ~(event.getMetaState() & 0xFFFFFFFFL);
    }

    public long clearMetaKeyState(long state, int which)
    {
        return state & ~(long)which;
    }
}

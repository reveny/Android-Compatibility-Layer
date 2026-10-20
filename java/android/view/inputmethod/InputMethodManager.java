package android.view.inputmethod;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class InputMethodManager
{
    public static final int SHOW_IMPLICIT = 0x0001;
    public static final int SHOW_FORCED = 0x0002;
    public static final int HIDE_IMPLICIT_ONLY = 0x0001;
    public static final int HIDE_NOT_ALWAYS = 0x0002;
    public static final int RESULT_UNCHANGED_SHOWN = 0;
    public static final int RESULT_UNCHANGED_HIDDEN = 1;
    public static final int RESULT_SHOWN = 2;
    public static final int RESULT_HIDDEN = 3;

    private static InputMethodManager sInstance;

    private boolean mVisible;
    private View mServedView;
    private InputConnection mConnection;
    private EditorInfo mEditorInfo;

    private InputMethodManager()
    {
    }

    public static InputMethodManager getInstance()
    {
        if (sInstance == null) sInstance = new InputMethodManager();

        return sInstance;
    }

    public boolean showSoftInput(View view, int flags)
    {
        startInput(view);
        mVisible = true;

        return true;
    }

    public boolean hideSoftInputFromWindow(android.os.IBinder windowToken, int flags)
    {
        boolean wasVisible = mVisible;
        mVisible = false;

        return wasVisible;
    }

    public boolean isSoftInputVisible()
    {
        return mVisible;
    }

    public void restartInput(View view)
    {
        if (view == mServedView) startInput(view);
    }

    public void startInput(View view)
    {
        if (view == null || !view.onCheckIsTextEditor())
        {
            finishInput();
            return;
        }

        EditorInfo editorInfo = new EditorInfo();
        InputConnection connection = view.onCreateInputConnection(editorInfo);

        if (connection == null)
        {
            finishInput();
            return;
        }

        mServedView = view;
        mConnection = connection;
        mEditorInfo = editorInfo;
    }

    public void finishInput()
    {
        mServedView = null;
        mConnection = null;
        mEditorInfo = null;
        mVisible = false;
    }

    public View getServedView()
    {
        return mServedView;
    }

    public InputConnection getCurrentInputConnection()
    {
        return mConnection;
    }

    public EditorInfo getCurrentEditorInfo()
    {
        return mEditorInfo;
    }

    public boolean dispatchKeyEvent(android.view.KeyEvent event)
    {
        if (mConnection == null || event == null) return false;
        if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;

        int keyCode = event.getKeyCode();

        if (keyCode == android.view.KeyEvent.KEYCODE_DEL)
        {
            return mConnection.deleteSurroundingText(1, 0);
        }

        if (keyCode == android.view.KeyEvent.KEYCODE_ENTER)
        {
            if (mEditorInfo != null && (mEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE)
            {
                return mConnection.performEditorAction(mEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
            }

            return mConnection.commitText("\n", 1);
        }

        int character = event.getUnicodeChar();
        if (character == 0) return false;

        return mConnection.commitText(String.valueOf((char)character), 1);
    }

    public void displayCompletions(View view, CompletionInfo[] completions)
    {
    }

    public void updateExtractedText(View view, int token, ExtractedText text)
    {
    }

    public void updateSelection(View view, int selStart, int selEnd, int candidatesStart, int candidatesEnd)
    {
    }

    public boolean isActive()
    {
        return mVisible;
    }

    public boolean isActive(View view)
    {
        return mVisible;
    }

    public boolean isAcceptingText()
    {
        return mVisible;
    }

    public void toggleSoftInput(int showFlags, int hideFlags)
    {
        mVisible = !mVisible;
    }

    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(InputMethodInfo imi, boolean allowsImplicitlyEnabledSubtypes)
    {
        return new ArrayList<>();
    }

    public InputMethodSubtype getCurrentInputMethodSubtype()
    {
        return null;
    }
}

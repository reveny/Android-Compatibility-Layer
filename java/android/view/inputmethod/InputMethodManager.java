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
    private CursorAnchorInfo mCursorAnchorInfo;

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

        if (keyCode == android.view.KeyEvent.KEYCODE_DEL || keyCode == android.view.KeyEvent.KEYCODE_FORWARD_DEL)
        {
            CharSequence selected = mConnection.getSelectedText(0);
            if (selected != null && selected.length() > 0) return mConnection.commitText("", 1);

            if (keyCode == android.view.KeyEvent.KEYCODE_FORWARD_DEL) return mConnection.deleteSurroundingText(0, 1);
            return mConnection.deleteSurroundingText(1, 0);
        }

        if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)
        {
            int action = enterAction(mEditorInfo);
            if (action == EditorInfo.IME_ACTION_NONE) return mConnection.commitText("\n", 1);

            return mConnection.performEditorAction(action);
        }

        int character = event.getUnicodeChar();
        if (character == 0) return false;

        return mConnection.commitText(String.valueOf((char)character), 1);
    }

    private static int enterAction(EditorInfo info)
    {
        if (info == null) return EditorInfo.IME_ACTION_NONE;
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return EditorInfo.IME_ACTION_NONE;
        if (info.actionLabel != null) return info.actionId;

        return info.imeOptions & EditorInfo.IME_MASK_ACTION;
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

    public List<InputMethodInfo> getInputMethodList()
    {
        return new ArrayList<>();
    }

    public List<InputMethodInfo> getEnabledInputMethodList()
    {
        return new ArrayList<>();
    }

    public InputMethodInfo getCurrentInputMethodInfo()
    {
        return null;
    }

    public boolean isStylusHandwritingAvailable()
    {
        return false;
    }

    public void startStylusHandwriting(View view)
    {
    }

    public void prepareStylusHandwritingDelegation(View delegatorView)
    {
    }

    public void prepareStylusHandwritingDelegation(View delegatorView, String delegatePackageName)
    {
    }

    public boolean acceptStylusHandwritingDelegation(View delegateView)
    {
        return false;
    }

    public boolean acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName)
    {
        return false;
    }

    public void updateCursorAnchorInfo(View view, CursorAnchorInfo cursorAnchorInfo)
    {
        if (view == mServedView) mCursorAnchorInfo = cursorAnchorInfo;
    }

    CursorAnchorInfo getCursorAnchorInfo()
    {
        return mCursorAnchorInfo;
    }

    public void updateCursor(View view, int left, int top, int right, int bottom)
    {
    }

    public boolean isWatchingCursor(View view)
    {
        return false;
    }

    public void viewClicked(View view)
    {
    }

    public void invalidateInput(View view)
    {
        restartInput(view);
    }

    public void sendAppPrivateCommand(View view, String action, android.os.Bundle data)
    {
        if (view == mServedView && mConnection != null) mConnection.performPrivateCommand(action, data);
    }

    public void dispatchKeyEventFromInputMethod(View targetView, android.view.KeyEvent event)
    {
        View target = targetView != null ? targetView : mServedView;
        if (target == null || event == null) return;

        target.dispatchKeyEvent(event);
    }

    public boolean showSoftInput(View view, int flags, android.os.ResultReceiver resultReceiver)
    {
        boolean wasVisible = mVisible;
        boolean shown = showSoftInput(view, flags);

        if (resultReceiver != null) resultReceiver.send(wasVisible ? RESULT_UNCHANGED_SHOWN : RESULT_SHOWN, null);

        return shown;
    }

    public boolean hideSoftInputFromWindow(android.os.IBinder windowToken, int flags, android.os.ResultReceiver resultReceiver)
    {
        boolean hidden = hideSoftInputFromWindow(windowToken, flags);

        if (resultReceiver != null) resultReceiver.send(hidden ? RESULT_HIDDEN : RESULT_UNCHANGED_HIDDEN, null);

        return hidden;
    }

    public void toggleSoftInputFromWindow(android.os.IBinder windowToken, int showFlags, int hideFlags)
    {
        toggleSoftInput(showFlags, hideFlags);
    }

    public boolean isFullscreenMode()
    {
        return false;
    }

    public boolean isInputMethodSuppressingSpellChecker()
    {
        return false;
    }

    public void showInputMethodPicker()
    {
    }

    public void showInputMethodAndSubtypeEnabler(String imiId)
    {
    }

    public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype)
    {
        return false;
    }

    public InputMethodSubtype getLastInputMethodSubtype()
    {
        return null;
    }

    public java.util.Map<InputMethodInfo, List<InputMethodSubtype>> getShortcutInputMethodsAndSubtypes()
    {
        return new java.util.HashMap<>();
    }

    public boolean switchToLastInputMethod(android.os.IBinder imeToken)
    {
        return false;
    }

    public boolean switchToNextInputMethod(android.os.IBinder imeToken, boolean onlyCurrentIme)
    {
        return false;
    }

    public boolean shouldOfferSwitchingToNextInputMethod(android.os.IBinder imeToken)
    {
        return false;
    }

    public void showStatusIcon(android.os.IBinder imeToken, String packageName, int iconId)
    {
    }

    public void hideStatusIcon(android.os.IBinder imeToken)
    {
    }

    public static final int HANDWRITING_DELEGATE_FLAG_HOME_DELEGATOR_ALLOWED = 0x0001;

    public void acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName, java.util.concurrent.Executor executor,
                                                  java.util.function.Consumer<Boolean> callback)
    {
        acceptStylusHandwritingDelegation(delegateView, delegatorPackageName, 0, executor, callback);
    }

    public void acceptStylusHandwritingDelegation(View delegateView, String delegatorPackageName, int flags,
                                                  java.util.concurrent.Executor executor, java.util.function.Consumer<Boolean> callback)
    {
        if (executor == null || callback == null) throw new NullPointerException();

        boolean accepted = acceptStylusHandwritingDelegation(delegateView, delegatorPackageName);
        executor.execute(() -> callback.accept(accepted));
    }

    public boolean isConnectionlessStylusHandwritingAvailable()
    {
        return false;
    }

    public void startConnectionlessStylusHandwriting(View view, CursorAnchorInfo cursorAnchorInfo, java.util.concurrent.Executor executor,
                                                     ConnectionlessHandwritingCallback callback)
    {
        reportConnectionlessUnsupported(executor, callback);
    }

    public void startConnectionlessStylusHandwritingForDelegation(View view, CursorAnchorInfo cursorAnchorInfo,
                                                                  java.util.concurrent.Executor executor,
                                                                  ConnectionlessHandwritingCallback callback)
    {
        reportConnectionlessUnsupported(executor, callback);
    }

    public void startConnectionlessStylusHandwritingForDelegation(View view, CursorAnchorInfo cursorAnchorInfo,
                                                                  String delegatorPackageName, java.util.concurrent.Executor executor,
                                                                  ConnectionlessHandwritingCallback callback)
    {
        reportConnectionlessUnsupported(executor, callback);
    }

    private static void reportConnectionlessUnsupported(java.util.concurrent.Executor executor, ConnectionlessHandwritingCallback callback)
    {
        if (executor == null || callback == null) throw new NullPointerException();

        executor.execute(() -> callback.onError(ConnectionlessHandwritingCallback.CONNECTIONLESS_HANDWRITING_ERROR_UNSUPPORTED));
    }

    @Deprecated
    public void showSoftInputFromInputMethod(android.os.IBinder token, int flags)
    {
    }

    @Deprecated
    public void hideSoftInputFromInputMethod(android.os.IBinder token, int flags)
    {
    }

    @Deprecated
    public void setInputMethod(android.os.IBinder token, String id)
    {
    }

    @Deprecated
    public void setInputMethodAndSubtype(android.os.IBinder token, String id, InputMethodSubtype subtype)
    {
    }

    @Deprecated
    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes)
    {
    }

    public void setExplicitlyEnabledInputMethodSubtypes(String imiId, int[] subtypeHashCodes)
    {
    }
}

package android.view.inputmethod;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

public interface InputMethodSession
{
    void finishInput();

    void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd);

    void viewClicked(boolean focusChanged);

    void updateCursor(Rect newCursor);

    void displayCompletions(CompletionInfo[] completions);

    void updateExtractedText(int token, ExtractedText text);

    void dispatchKeyEvent(int seq, KeyEvent event, EventCallback callback);

    void dispatchTrackballEvent(int seq, MotionEvent event, EventCallback callback);

    void dispatchGenericMotionEvent(int seq, MotionEvent event, EventCallback callback);

    void appPrivateCommand(String action, Bundle data);

    void toggleSoftInput(int showFlags, int hideFlags);

    void updateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo);

    interface EventCallback
    {
        void finishedEvent(int seq, boolean handled);
    }
}

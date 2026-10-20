package android.view.inputmethod;

import android.os.Bundle;
import android.view.KeyEvent;

public interface InputConnection
{
    int GET_TEXT_WITH_STYLES = 1;
    int GET_EXTRACTED_TEXT_MONITOR = 1;
    int CURSOR_UPDATE_IMMEDIATE = 1;
    int CURSOR_UPDATE_MONITOR = 2;
    int INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 1;

    CharSequence getTextBeforeCursor(int length, int flags);

    CharSequence getTextAfterCursor(int length, int flags);

    CharSequence getSelectedText(int flags);

    int getCursorCapsMode(int reqModes);

    ExtractedText getExtractedText(ExtractedTextRequest request, int flags);

    boolean deleteSurroundingText(int beforeLength, int afterLength);

    boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength);

    boolean setComposingText(CharSequence text, int newCursorPosition);

    boolean setComposingRegion(int start, int end);

    boolean finishComposingText();

    boolean commitText(CharSequence text, int newCursorPosition);

    boolean commitCompletion(CompletionInfo text);

    boolean commitCorrection(CorrectionInfo correctionInfo);

    boolean setSelection(int start, int end);

    boolean performEditorAction(int editorAction);

    boolean performContextMenuAction(int id);

    boolean beginBatchEdit();

    boolean endBatchEdit();

    boolean sendKeyEvent(KeyEvent event);

    boolean clearMetaKeyStates(int states);

    boolean reportFullscreenMode(boolean enabled);

    boolean performPrivateCommand(String action, Bundle data);

    boolean requestCursorUpdates(int cursorUpdateMode);

    void closeConnection();

    boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts);
}

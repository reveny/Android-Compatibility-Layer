package android.view.inputmethod;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.view.KeyEvent;

import java.util.concurrent.Executor;
import java.util.function.IntConsumer;

public interface InputConnection
{
    int GET_TEXT_WITH_STYLES = 1;
    int GET_EXTRACTED_TEXT_MONITOR = 1;
    int INPUT_CONTENT_GRANT_READ_URI_PERMISSION = 1;

    int CURSOR_UPDATE_IMMEDIATE = 1;
    int CURSOR_UPDATE_MONITOR = 2;
    int CURSOR_UPDATE_FILTER_EDITOR_BOUNDS = 4;
    int CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS = 8;
    int CURSOR_UPDATE_FILTER_INSERTION_MARKER = 16;
    int CURSOR_UPDATE_FILTER_VISIBLE_LINE_BOUNDS = 32;
    int CURSOR_UPDATE_FILTER_TEXT_APPEARANCE = 64;

    int HANDWRITING_GESTURE_RESULT_UNKNOWN = 0;
    int HANDWRITING_GESTURE_RESULT_SUCCESS = 1;
    int HANDWRITING_GESTURE_RESULT_UNSUPPORTED = 2;
    int HANDWRITING_GESTURE_RESULT_FAILED = 3;
    int HANDWRITING_GESTURE_RESULT_CANCELLED = 4;
    int HANDWRITING_GESTURE_RESULT_FALLBACK = 5;

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

    default boolean performSpellCheck()
    {
        return false;
    }

    boolean performPrivateCommand(String action, Bundle data);

    default void performHandwritingGesture(HandwritingGesture gesture, Executor executor, IntConsumer consumer)
    {
        if (executor == null || consumer == null) return;

        executor.execute(() -> consumer.accept(HANDWRITING_GESTURE_RESULT_UNSUPPORTED));
    }

    default boolean previewHandwritingGesture(PreviewableHandwritingGesture gesture, CancellationSignal cancellationSignal)
    {
        return false;
    }

    boolean requestCursorUpdates(int cursorUpdateMode);

    default boolean requestCursorUpdates(int cursorUpdateMode, int cursorUpdateFilter)
    {
        if (cursorUpdateFilter == 0) return requestCursorUpdates(cursorUpdateMode);

        return false;
    }

    default Handler getHandler()
    {
        return null;
    }

    void closeConnection();

    boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts);

    default boolean setImeConsumesInput(boolean imeConsumesInput)
    {
        return false;
    }

    default SurroundingText getSurroundingText(int beforeLength, int afterLength, int flags)
    {
        if (beforeLength < 0) throw new IllegalArgumentException("beforeLength must be non-negative");
        if (afterLength < 0) throw new IllegalArgumentException("afterLength must be non-negative");

        CharSequence textBeforeCursor = getTextBeforeCursor(beforeLength, flags);
        if (textBeforeCursor == null) return null;

        CharSequence textAfterCursor = getTextAfterCursor(afterLength, flags);
        if (textAfterCursor == null) return null;

        CharSequence selectedText = getSelectedText(flags);
        if (selectedText == null) selectedText = "";

        CharSequence surroundingText = android.text.TextUtils.concat(textBeforeCursor, selectedText, textAfterCursor);
        return new SurroundingText(surroundingText, textBeforeCursor.length(), textBeforeCursor.length() + selectedText.length(), -1);
    }

    default boolean setComposingText(CharSequence text, int newCursorPosition, TextAttribute textAttribute)
    {
        return setComposingText(text, newCursorPosition);
    }

    default boolean setComposingRegion(int start, int end, TextAttribute textAttribute)
    {
        return setComposingRegion(start, end);
    }

    default boolean commitText(CharSequence text, int newCursorPosition, TextAttribute textAttribute)
    {
        return commitText(text, newCursorPosition);
    }

    default boolean replaceText(int start, int end, CharSequence text, int newCursorPosition, TextAttribute textAttribute)
    {
        if (start < 0) throw new IllegalArgumentException("start must be non-negative");
        if (end < 0) throw new IllegalArgumentException("end must be non-negative");

        beginBatchEdit();
        finishComposingText();
        setSelection(start, end);
        commitText(text, newCursorPosition);
        endBatchEdit();
        return true;
    }

    default TextSnapshot takeSnapshot()
    {
        return null;
    }

    default void requestTextBoundsInfo(android.graphics.RectF bounds, Executor executor,
                                       java.util.function.Consumer<TextBoundsInfoResult> consumer)
    {
        if (executor == null || consumer == null) throw new NullPointerException();

        executor.execute(() -> consumer.accept(new TextBoundsInfoResult(TextBoundsInfoResult.CODE_UNSUPPORTED)));
    }
}

package android.view.inputmethod;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public class BaseInputConnection implements InputConnection
{
    private final StringBuilder mText = new StringBuilder();
    private final View mTargetView;
    private final boolean mFullEditor;

    private int mSelectionStart;
    private int mSelectionEnd;
    private int mComposingStart = -1;
    private int mComposingEnd = -1;
    private int mBatchDepth;

    public BaseInputConnection(View targetView, boolean fullEditor)
    {
        mTargetView = targetView;
        mFullEditor = fullEditor;
    }

    public View getTargetView()
    {
        return mTargetView;
    }

    public boolean isFullEditor()
    {
        return mFullEditor;
    }

    public android.text.Editable getEditable()
    {
        return null;
    }

    @Override
    public android.os.Handler getHandler()
    {
        return null;
    }

    public static int getComposingSpanStart(android.text.Spannable text)
    {
        return -1;
    }

    public static int getComposingSpanEnd(android.text.Spannable text)
    {
        return -1;
    }

    public static void removeComposingSpans(android.text.Spannable text)
    {
    }

    public static void setComposingSpans(android.text.Spannable text)
    {
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags)
    {
        int start = Math.max(0, mSelectionStart - length);
        if (start >= mSelectionStart) return "";

        return mText.substring(start, mSelectionStart);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags)
    {
        int end = Math.min(mText.length(), mSelectionEnd + length);
        if (mSelectionEnd >= end) return "";

        return mText.substring(mSelectionEnd, end);
    }

    @Override
    public CharSequence getSelectedText(int flags)
    {
        if (mSelectionStart >= mSelectionEnd) return null;

        return mText.substring(mSelectionStart, mSelectionEnd);
    }

    @Override
    public int getCursorCapsMode(int reqModes)
    {
        return 0;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags)
    {
        ExtractedText extracted = new ExtractedText();
        extracted.text = mText.toString();
        extracted.startOffset = 0;
        extracted.selectionStart = mSelectionStart;
        extracted.selectionEnd = mSelectionEnd;
        extracted.partialStartOffset = -1;
        extracted.partialEndOffset = -1;
        return extracted;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength)
    {
        int start = Math.max(0, mSelectionStart - Math.max(0, beforeLength));
        int end = Math.min(mText.length(), mSelectionEnd + Math.max(0, afterLength));

        mText.delete(mSelectionEnd, end);
        mText.delete(start, mSelectionStart);

        mSelectionStart = start;
        mSelectionEnd = start;
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength)
    {
        return deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition)
    {
        replaceSelection(text == null ? "" : text.toString());

        mComposingStart = mSelectionStart - (text == null ? 0 : text.length());
        mComposingEnd = mSelectionEnd;
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end)
    {
        mComposingStart = Math.max(0, Math.min(start, mText.length()));
        mComposingEnd = Math.max(mComposingStart, Math.min(end, mText.length()));
        return true;
    }

    @Override
    public boolean finishComposingText()
    {
        mComposingStart = -1;
        mComposingEnd = -1;
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition)
    {
        replaceSelection(text == null ? "" : text.toString());
        return finishComposingText();
    }

    @Override
    public boolean commitCompletion(CompletionInfo text)
    {
        return false;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo)
    {
        return false;
    }

    @Override
    public boolean setSelection(int start, int end)
    {
        mSelectionStart = Math.max(0, Math.min(start, mText.length()));
        mSelectionEnd = Math.max(mSelectionStart, Math.min(end, mText.length()));
        return true;
    }

    @Override
    public boolean performEditorAction(int editorAction)
    {
        return false;
    }

    @Override
    public boolean performContextMenuAction(int id)
    {
        return false;
    }

    @Override
    public boolean beginBatchEdit()
    {
        mBatchDepth += 1;
        return true;
    }

    @Override
    public boolean endBatchEdit()
    {
        if (mBatchDepth == 0) return false;

        mBatchDepth -= 1;
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event)
    {
        return false;
    }

    @Override
    public boolean clearMetaKeyStates(int states)
    {
        return false;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled)
    {
        return false;
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data)
    {
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode)
    {
        return false;
    }

    @Override
    public void closeConnection()
    {
        finishComposingText();
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts)
    {
        return false;
    }

    @Override
    public SurroundingText getSurroundingText(int beforeLength, int afterLength, int flags)
    {
        if (beforeLength < 0) throw new IllegalArgumentException("beforeLength must be non-negative");
        if (afterLength < 0) throw new IllegalArgumentException("afterLength must be non-negative");

        int selectionStart = Math.max(0, Math.min(mSelectionStart, mText.length()));
        int selectionEnd = Math.max(selectionStart, Math.min(mSelectionEnd, mText.length()));
        int start = (int)Math.max(0, (long)selectionStart - beforeLength);
        int end = (int)Math.min(mText.length(), (long)selectionEnd + afterLength);

        return new SurroundingText(mText.substring(start, end), selectionStart - start, selectionEnd - start, start);
    }

    @Override
    public TextSnapshot takeSnapshot()
    {
        SurroundingText surroundingText = getSurroundingText(2048, 2048, 0);
        int compositionStart = mComposingStart >= 0 && mComposingEnd >= mComposingStart ? mComposingStart : -1;
        int compositionEnd = compositionStart < 0 ? -1 : mComposingEnd;

        return new TextSnapshot(surroundingText, compositionStart, compositionEnd, getCursorCapsMode(0));
    }

    @Override
    public boolean replaceText(int start, int end, CharSequence text, int newCursorPosition, TextAttribute textAttribute)
    {
        if (start < 0) throw new IllegalArgumentException("start must be non-negative");
        if (end < 0) throw new IllegalArgumentException("end must be non-negative");

        beginBatchEdit();
        finishComposingText();
        setSelection(Math.min(start, end), Math.max(start, end));
        commitText(text, newCursorPosition);
        endBatchEdit();
        return true;
    }

    private void replaceSelection(String text)
    {
        int start = Math.max(0, Math.min(mSelectionStart, mText.length()));
        int end = Math.max(start, Math.min(mSelectionEnd, mText.length()));

        mText.replace(start, end, text);
        mSelectionStart = start + text.length();
        mSelectionEnd = mSelectionStart;
    }
}

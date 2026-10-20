package android.view.inputmethod;

import android.os.Bundle;
import android.view.KeyEvent;

public class InputConnectionWrapper implements InputConnection
{
    private InputConnection mTarget;
    private final boolean mMutable;

    public InputConnectionWrapper(InputConnection target, boolean mutable)
    {
        mTarget = target;
        mMutable = mutable;
    }

    public void setTarget(InputConnection target)
    {
        if (mTarget != null && !mMutable) throw new SecurityException("not mutable");

        mTarget = target;
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags)
    {
        return mTarget == null ? null : mTarget.getTextBeforeCursor(length, flags);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags)
    {
        return mTarget == null ? null : mTarget.getTextAfterCursor(length, flags);
    }

    @Override
    public CharSequence getSelectedText(int flags)
    {
        return mTarget == null ? null : mTarget.getSelectedText(flags);
    }

    @Override
    public int getCursorCapsMode(int reqModes)
    {
        return mTarget == null ? 0 : mTarget.getCursorCapsMode(reqModes);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags)
    {
        return mTarget == null ? null : mTarget.getExtractedText(request, flags);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength)
    {
        return mTarget != null && mTarget.deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength)
    {
        return mTarget != null && mTarget.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition)
    {
        return mTarget != null && mTarget.setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingRegion(int start, int end)
    {
        return mTarget != null && mTarget.setComposingRegion(start, end);
    }

    @Override
    public boolean finishComposingText()
    {
        return mTarget != null && mTarget.finishComposingText();
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition)
    {
        return mTarget != null && mTarget.commitText(text, newCursorPosition);
    }

    @Override
    public boolean commitCompletion(CompletionInfo text)
    {
        return mTarget != null && mTarget.commitCompletion(text);
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo)
    {
        return mTarget != null && mTarget.commitCorrection(correctionInfo);
    }

    @Override
    public boolean setSelection(int start, int end)
    {
        return mTarget != null && mTarget.setSelection(start, end);
    }

    @Override
    public boolean performEditorAction(int editorAction)
    {
        return mTarget != null && mTarget.performEditorAction(editorAction);
    }

    @Override
    public boolean performContextMenuAction(int id)
    {
        return mTarget != null && mTarget.performContextMenuAction(id);
    }

    @Override
    public boolean beginBatchEdit()
    {
        return mTarget != null && mTarget.beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit()
    {
        return mTarget != null && mTarget.endBatchEdit();
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event)
    {
        return mTarget != null && mTarget.sendKeyEvent(event);
    }

    @Override
    public boolean clearMetaKeyStates(int states)
    {
        return mTarget != null && mTarget.clearMetaKeyStates(states);
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled)
    {
        return mTarget != null && mTarget.reportFullscreenMode(enabled);
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data)
    {
        return mTarget != null && mTarget.performPrivateCommand(action, data);
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode)
    {
        return mTarget != null && mTarget.requestCursorUpdates(cursorUpdateMode);
    }

    @Override
    public void closeConnection()
    {
        if (mTarget != null) mTarget.closeConnection();
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts)
    {
        return mTarget != null && mTarget.commitContent(inputContentInfo, flags, opts);
    }
}

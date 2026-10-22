package android.text.method;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class ArrowKeyMovementMethod extends BaseMovementMethod implements MovementMethod
{
    private static ArrowKeyMovementMethod sInstance;

    private boolean mSelecting;

    public ArrowKeyMovementMethod()
    {
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event)
    {
        mSelecting = event != null && event.isShiftPressed();

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && KeyEvent.metaStateHasNoModifiers(movementMetaState))
        {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0 && mSelecting)
            {
                return widget.showContextMenu();
            }
        }

        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    private static int clamp(Spannable buffer, int offset)
    {
        return Math.max(0, Math.min(offset, buffer.length()));
    }

    private boolean moveTo(Spannable buffer, int offset)
    {
        int target = clamp(buffer, offset);
        int start = Selection.getSelectionStart(buffer);
        int end = Selection.getSelectionEnd(buffer);

        if (mSelecting)
        {
            if (end == target) return false;

            Selection.extendSelection(buffer, target);
            return true;
        }

        if (start == target && end == target) return false;

        Selection.setSelection(buffer, target);
        return true;
    }

    private boolean moveHorizontal(Spannable buffer, int direction)
    {
        int start = Selection.getSelectionStart(buffer);
        int end = Selection.getSelectionEnd(buffer);

        if (!mSelecting && start != end)
        {
            Selection.setSelection(buffer, direction < 0 ? Math.min(start, end) : Math.max(start, end));
            return true;
        }

        if (end < 0) end = 0;

        int target = end + direction;

        if (direction < 0 && target > 0 && Character.isLowSurrogate(buffer.charAt(target))
            && Character.isHighSurrogate(buffer.charAt(target - 1)))
        {
            target--;
        }
        else if (direction > 0 && target < buffer.length() && Character.isLowSurrogate(buffer.charAt(target)))
        {
            target++;
        }

        if (target < 0 || target > buffer.length()) return false;

        return moveTo(buffer, target);
    }

    private boolean moveVertical(TextView widget, Spannable buffer, int lines)
    {
        Layout layout = widget.getLayout();
        int end = Math.max(0, Selection.getSelectionEnd(buffer));

        if (layout == null) return moveTo(buffer, lines < 0 ? 0 : buffer.length());

        int line = layout.getLineForOffset(end);
        int target = line + lines;

        if (target < 0) return moveTo(buffer, 0);
        if (target >= layout.getLineCount()) return moveTo(buffer, buffer.length());

        float horizontal = layout.getPrimaryHorizontal(end);
        return moveTo(buffer, layout.getOffsetForHorizontal(target, horizontal));
    }

    private int getPageLines(TextView widget)
    {
        Layout layout = widget.getLayout();
        if (layout == null || layout.getLineCount() == 0) return 1;

        int lineHeight = Math.max(1, layout.getLineBottom(0) - layout.getLineTop(0));
        return Math.max(1, getInnerHeight(widget) / lineHeight);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer)
    {
        return moveHorizontal(buffer, -1);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer)
    {
        return moveHorizontal(buffer, 1);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer)
    {
        return moveVertical(widget, buffer, -1);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer)
    {
        return moveVertical(widget, buffer, 1);
    }

    @Override
    protected boolean pageUp(TextView widget, Spannable buffer)
    {
        return moveVertical(widget, buffer, -getPageLines(widget));
    }

    @Override
    protected boolean pageDown(TextView widget, Spannable buffer)
    {
        return moveVertical(widget, buffer, getPageLines(widget));
    }

    @Override
    protected boolean top(TextView widget, Spannable buffer)
    {
        return moveTo(buffer, 0);
    }

    @Override
    protected boolean bottom(TextView widget, Spannable buffer)
    {
        return moveTo(buffer, buffer.length());
    }

    @Override
    protected boolean lineStart(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        int end = Math.max(0, Selection.getSelectionEnd(buffer));

        if (layout == null)
        {
            int index = end;
            while (index > 0 && buffer.charAt(index - 1) != '\n') index--;
            return moveTo(buffer, index);
        }

        return moveTo(buffer, layout.getLineStart(layout.getLineForOffset(end)));
    }

    @Override
    protected boolean lineEnd(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        int end = Math.max(0, Selection.getSelectionEnd(buffer));

        if (layout == null)
        {
            int index = end;
            while (index < buffer.length() && buffer.charAt(index) != '\n') index++;
            return moveTo(buffer, index);
        }

        return moveTo(buffer, layout.getLineVisibleEnd(layout.getLineForOffset(end)));
    }

    @Override
    boolean leftWord(TextView widget, Spannable buffer)
    {
        int end = Math.max(0, Selection.getSelectionEnd(buffer));
        int index = end;

        while (index > 0 && !Character.isLetterOrDigit(buffer.charAt(index - 1))) index--;
        while (index > 0 && Character.isLetterOrDigit(buffer.charAt(index - 1))) index--;

        return moveTo(buffer, index);
    }

    @Override
    boolean rightWord(TextView widget, Spannable buffer)
    {
        int end = Math.max(0, Selection.getSelectionEnd(buffer));
        int index = end;
        int length = buffer.length();

        while (index < length && !Character.isLetterOrDigit(buffer.charAt(index))) index++;
        while (index < length && Character.isLetterOrDigit(buffer.charAt(index))) index++;

        return moveTo(buffer, index);
    }

    @Override
    protected boolean home(TextView widget, Spannable buffer)
    {
        return lineStart(widget, buffer);
    }

    @Override
    protected boolean end(TextView widget, Spannable buffer)
    {
        return lineEnd(widget, buffer);
    }

    @Override
    public boolean previousParagraph(TextView widget, Spannable buffer)
    {
        int index = Math.max(0, Selection.getSelectionEnd(buffer));

        if (index > 0) index--;
        while (index > 0 && buffer.charAt(index - 1) != '\n') index--;

        return moveTo(buffer, index);
    }

    @Override
    public boolean nextParagraph(TextView widget, Spannable buffer)
    {
        int index = Math.max(0, Selection.getSelectionEnd(buffer));
        int length = buffer.length();

        while (index < length && buffer.charAt(index) != '\n') index++;
        if (index < length) index++;

        return moveTo(buffer, index);
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        int initialScrollX = -1;
        int initialScrollY = -1;
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_UP)
        {
            initialScrollX = Touch.getInitialScrollX(widget, buffer);
            initialScrollY = Touch.getInitialScrollY(widget, buffer);
        }

        boolean handled = Touch.onTouchEvent(widget, buffer, event);

        if (action != MotionEvent.ACTION_UP) return handled;

        if ((initialScrollY >= 0 && initialScrollY != widget.getScrollY())
            || (initialScrollX >= 0 && initialScrollX != widget.getScrollX()))
        {
            return true;
        }

        int offset = getOffsetForPosition(widget, event.getX(), event.getY());
        if (offset < 0) return handled;

        boolean selecting = (event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0;

        if (selecting) Selection.extendSelection(buffer, clamp(buffer, offset));
        else Selection.setSelection(buffer, clamp(buffer, offset));

        MetaKeyKeyListener.adjustMetaAfterKeypress(buffer);
        MetaKeyKeyListener.resetLockedMeta(buffer);
        return true;
    }

    @Override
    public boolean canSelectArbitrarily()
    {
        return true;
    }

    @Override
    public void initialize(TextView widget, Spannable text)
    {
        Selection.setSelection(text, 0);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int direction)
    {
        if ((direction & (View.FOCUS_FORWARD | View.FOCUS_DOWN)) != 0)
        {
            if (view.getLayout() == null) Selection.setSelection(text, text.length());
        }
        else
        {
            Selection.setSelection(text, text.length());
        }
    }

    public static MovementMethod getInstance()
    {
        if (sInstance == null) sInstance = new ArrowKeyMovementMethod();

        return sInstance;
    }
}

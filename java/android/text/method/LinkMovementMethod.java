package android.text.method;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class LinkMovementMethod extends ScrollingMovementMethod
{
    private static final int CLICK = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;

    private static final Object FROM_BELOW = new NoCopySpan.Concrete();

    private static LinkMovementMethod sInstance;

    public LinkMovementMethod()
    {
    }

    @Override
    public boolean canSelectArbitrarily()
    {
        return true;
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
        {
            if (KeyEvent.metaStateHasNoModifiers(movementMetaState) && event != null && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0 && action(CLICK, widget, buffer))
            {
                return true;
            }
        }

        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer)
    {
        if (action(UP, widget, buffer)) return true;

        return super.up(widget, buffer);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer)
    {
        if (action(DOWN, widget, buffer)) return true;

        return super.down(widget, buffer);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer)
    {
        if (action(UP, widget, buffer)) return true;

        return super.left(widget, buffer);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer)
    {
        if (action(DOWN, widget, buffer)) return true;

        return super.right(widget, buffer);
    }

    private boolean action(int what, TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
        int areaTop = widget.getScrollY();
        int areaBottom = areaTop + widget.getHeight() - padding;

        int lineTop = layout.getLineForVertical(areaTop);
        int lineBottom = layout.getLineForVertical(areaBottom);

        int first = layout.getLineStart(lineTop);
        int last = layout.getLineEnd(lineBottom);

        ClickableSpan[] candidates = buffer.getSpans(first, last, ClickableSpan.class);

        int selectionA = Selection.getSelectionStart(buffer);
        int selectionB = Selection.getSelectionEnd(buffer);

        int selectionStart = Math.min(selectionA, selectionB);
        int selectionEnd = Math.max(selectionA, selectionB);

        if (selectionStart < 0 && buffer.getSpanStart(FROM_BELOW) >= 0) selectionStart = selectionEnd = buffer.length();

        if (selectionStart > last) selectionStart = selectionEnd = Integer.MAX_VALUE;
        if (selectionEnd < first) selectionStart = selectionEnd = -1;

        switch (what)
        {
            case CLICK:
            {
                if (selectionStart == selectionEnd) return false;

                ClickableSpan[] links = buffer.getSpans(selectionStart, selectionEnd, ClickableSpan.class);
                if (links.length != 1) return false;

                links[0].onClick(widget);
                break;
            }
            case UP:
            {
                int bestStart = -1;
                int bestEnd = -1;

                for (ClickableSpan candidate : candidates)
                {
                    int end = buffer.getSpanEnd(candidate);

                    if ((end < selectionEnd || selectionStart == selectionEnd) && end > bestEnd)
                    {
                        bestStart = buffer.getSpanStart(candidate);
                        bestEnd = end;
                    }
                }

                if (bestStart >= 0)
                {
                    Selection.setSelection(buffer, bestEnd, bestStart);
                    return true;
                }

                break;
            }
            case DOWN:
            {
                int bestStart = Integer.MAX_VALUE;
                int bestEnd = Integer.MAX_VALUE;

                for (ClickableSpan candidate : candidates)
                {
                    int start = buffer.getSpanStart(candidate);

                    if ((start > selectionStart || selectionStart == selectionEnd) && start < bestStart)
                    {
                        bestStart = start;
                        bestEnd = buffer.getSpanEnd(candidate);
                    }
                }

                if (bestEnd < Integer.MAX_VALUE)
                {
                    Selection.setSelection(buffer, bestStart, bestEnd);
                    return true;
                }

                break;
            }
            default:
                break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN)
        {
            int offset = getOffsetForPosition(widget, event.getX(), event.getY());

            if (offset >= 0)
            {
                ClickableSpan[] links = buffer.getSpans(offset, offset, ClickableSpan.class);

                if (links.length != 0)
                {
                    ClickableSpan link = links[0];

                    if (action == MotionEvent.ACTION_UP) link.onClick(widget);
                    else Selection.setSelection(buffer, buffer.getSpanStart(link), buffer.getSpanEnd(link));

                    return true;
                }

                Selection.removeSelection(buffer);
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    @Override
    public void initialize(TextView widget, Spannable text)
    {
        Selection.removeSelection(text);
        text.removeSpan(FROM_BELOW);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int direction)
    {
        Selection.removeSelection(text);

        if ((direction & View.FOCUS_BACKWARD) != 0) text.setSpan(FROM_BELOW, 0, 0, Spanned.SPAN_POINT_POINT);
        else text.removeSpan(FROM_BELOW);
    }

    public static MovementMethod getInstance()
    {
        if (sInstance == null) sInstance = new LinkMovementMethod();

        return sInstance;
    }
}

package android.text.method;

import android.text.Layout;
import android.text.Spannable;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class BaseMovementMethod implements MovementMethod
{
    public BaseMovementMethod()
    {
    }

    @Override
    public boolean canSelectArbitrarily()
    {
        return false;
    }

    @Override
    public void initialize(TextView widget, Spannable text)
    {
    }

    @Override
    public boolean onKeyDown(TextView widget, Spannable text, int keyCode, KeyEvent event)
    {
        int movementMetaState = getMovementMetaState(text, event);
        boolean handled = handleMovementKey(widget, text, keyCode, movementMetaState, event);

        if (handled)
        {
            MetaKeyKeyListener.adjustMetaAfterKeypress(text);
            MetaKeyKeyListener.resetLockedMeta(text);
        }

        return handled;
    }

    @Override
    public boolean onKeyOther(TextView widget, Spannable text, KeyEvent event)
    {
        if (event == null) return false;

        int movementMetaState = getMovementMetaState(text, event);
        int keyCode = event.getKeyCode();

        if (keyCode != KeyEvent.KEYCODE_UNKNOWN && event.getAction() == KeyEvent.ACTION_MULTIPLE)
        {
            int repeat = event.getRepeatCount();
            boolean handled = false;

            for (int index = 0; index < repeat; index++)
            {
                if (!handleMovementKey(widget, text, keyCode, movementMetaState, event)) break;
                handled = true;
            }

            if (handled)
            {
                MetaKeyKeyListener.adjustMetaAfterKeypress(text);
                MetaKeyKeyListener.resetLockedMeta(text);
            }

            return handled;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(TextView widget, Spannable text, int keyCode, KeyEvent event)
    {
        return false;
    }

    @Override
    public void onTakeFocus(TextView widget, Spannable text, int direction)
    {
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable text, MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean onTrackballEvent(TextView widget, Spannable text, MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(TextView widget, Spannable text, MotionEvent event)
    {
        if (event == null) return false;
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) return false;
        if (event.getAction() != MotionEvent.ACTION_SCROLL) return false;

        float verticalScroll;
        float horizontalScroll;

        if ((event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0)
        {
            verticalScroll = 0;
            horizontalScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        }
        else
        {
            verticalScroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            horizontalScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
        }

        boolean handled = false;

        if (horizontalScroll < 0) handled |= scrollLeft(widget, text, (int)Math.ceil(-horizontalScroll));
        else if (horizontalScroll > 0) handled |= scrollRight(widget, text, (int)Math.ceil(horizontalScroll));

        if (verticalScroll < 0) handled |= scrollUp(widget, text, (int)Math.ceil(-verticalScroll));
        else if (verticalScroll > 0) handled |= scrollDown(widget, text, (int)Math.ceil(verticalScroll));

        return handled;
    }

    protected int getMovementMetaState(Spannable buffer, KeyEvent event)
    {
        int metaState = MetaKeyKeyListener.getMetaState(buffer) | (event == null ? 0 : event.getMetaState());
        metaState &= ~(MetaKeyKeyListener.META_ALT_LOCKED | MetaKeyKeyListener.META_SYM_LOCKED);

        return KeyEvent.normalizeMetaState(metaState) & ~KeyEvent.META_SHIFT_MASK;
    }

    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return left(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_CTRL_ON)) return leftWord(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return lineStart(widget, buffer);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return right(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_CTRL_ON)) return rightWord(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return lineEnd(widget, buffer);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return up(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return top(widget, buffer);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return down(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return bottom(widget, buffer);
                break;
            case KeyEvent.KEYCODE_PAGE_UP:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return pageUp(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return top(widget, buffer);
                break;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return pageDown(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_ALT_ON)) return bottom(widget, buffer);
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return home(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_CTRL_ON)) return top(widget, buffer);
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) return end(widget, buffer);
                if (KeyEvent.metaStateHasModifiers(movementMetaState, KeyEvent.META_CTRL_ON)) return bottom(widget, buffer);
                break;
            default:
                break;
        }

        return false;
    }

    protected boolean left(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean right(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean up(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean down(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean pageUp(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean pageDown(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean top(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean bottom(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean lineStart(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean lineEnd(TextView widget, Spannable buffer)
    {
        return false;
    }

    boolean leftWord(TextView widget, Spannable buffer)
    {
        return false;
    }

    boolean rightWord(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean home(TextView widget, Spannable buffer)
    {
        return false;
    }

    protected boolean end(TextView widget, Spannable buffer)
    {
        return false;
    }

    public boolean previousParagraph(TextView widget, Spannable buffer)
    {
        return false;
    }

    public boolean nextParagraph(TextView widget, Spannable buffer)
    {
        return false;
    }

    static int getOffsetForPosition(TextView widget, float x, float y)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return -1;

        int horizontal = (int)x - widget.getTotalPaddingLeft() + widget.getScrollX();
        int vertical = (int)y - widget.getTotalPaddingTop() + widget.getScrollY();

        int line = layout.getLineForVertical(vertical);
        return layout.getOffsetForHorizontal(line, horizontal);
    }

    private static int getTopLine(TextView widget)
    {
        return widget.getLayout().getLineForVertical(widget.getScrollY());
    }

    private static int getBottomLine(TextView widget)
    {
        return widget.getLayout().getLineForVertical(widget.getScrollY() + getInnerHeight(widget));
    }

    static int getInnerWidth(TextView widget)
    {
        return widget.getWidth() - widget.getTotalPaddingLeft() - widget.getTotalPaddingRight();
    }

    static int getInnerHeight(TextView widget)
    {
        return widget.getHeight() - widget.getTotalPaddingTop() - widget.getTotalPaddingBottom();
    }

    private static int getCharacterWidth(TextView widget)
    {
        return (int)Math.ceil(widget.getPaint().getFontSpacing());
    }

    private static int getScrollBoundsLeft(TextView widget)
    {
        Layout layout = widget.getLayout();
        int topLine = getTopLine(widget);
        int bottomLine = getBottomLine(widget);
        if (topLine > bottomLine) return 0;

        int left = Integer.MAX_VALUE;
        for (int line = topLine; line <= bottomLine; line++)
        {
            int lineLeft = (int)Math.floor(layout.getLineLeft(line));
            if (lineLeft < left) left = lineLeft;
        }

        return left;
    }

    private static int getScrollBoundsRight(TextView widget)
    {
        Layout layout = widget.getLayout();
        int topLine = getTopLine(widget);
        int bottomLine = getBottomLine(widget);
        if (topLine > bottomLine) return 0;

        int right = Integer.MIN_VALUE;
        for (int line = topLine; line <= bottomLine; line++)
        {
            int lineRight = (int)Math.ceil(layout.getLineRight(line));
            if (lineRight > right) right = lineRight;
        }

        return right;
    }

    boolean scrollLeft(TextView widget, Spannable buffer, int amount)
    {
        if (widget.getLayout() == null) return false;

        int minimumScrollX = getScrollBoundsLeft(widget);
        int scrollX = widget.getScrollX();

        if (scrollX <= minimumScrollX) return false;

        scrollX = Math.max(scrollX - getCharacterWidth(widget) * amount, minimumScrollX);
        widget.scrollTo(scrollX, widget.getScrollY());
        return true;
    }

    boolean scrollRight(TextView widget, Spannable buffer, int amount)
    {
        if (widget.getLayout() == null) return false;

        int maximumScrollX = getScrollBoundsRight(widget) - getInnerWidth(widget);
        int scrollX = widget.getScrollX();

        if (scrollX >= maximumScrollX) return false;

        scrollX = Math.min(scrollX + getCharacterWidth(widget) * amount, maximumScrollX);
        widget.scrollTo(scrollX, widget.getScrollY());
        return true;
    }

    boolean scrollUp(TextView widget, Spannable buffer, int amount)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int top = widget.getScrollY();
        int topLine = layout.getLineForVertical(top);
        if (layout.getLineTop(topLine) == top) topLine -= 1;

        if (topLine < 0) return false;

        topLine = Math.max(topLine - amount + 1, 0);
        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(topLine));
        return true;
    }

    boolean scrollDown(TextView widget, Spannable buffer, int amount)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int innerHeight = getInnerHeight(widget);
        int bottom = widget.getScrollY() + innerHeight;
        int bottomLine = layout.getLineForVertical(bottom);
        if (layout.getLineTop(bottomLine + 1) < bottom + 1) bottomLine += 1;

        int limit = layout.getLineCount() - 1;
        if (bottomLine > limit) return false;

        bottomLine = Math.min(bottomLine + amount - 1, limit);
        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(bottomLine + 1) - innerHeight);
        return true;
    }

    boolean scrollPageUp(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int top = widget.getScrollY() - getInnerHeight(widget);
        int topLine = layout.getLineForVertical(top);

        if (topLine < 0) return false;

        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(topLine));
        return true;
    }

    boolean scrollPageDown(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int innerHeight = getInnerHeight(widget);
        int bottom = widget.getScrollY() + innerHeight + innerHeight;
        int bottomLine = layout.getLineForVertical(bottom);

        if (bottomLine > layout.getLineCount() - 1) return false;

        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(bottomLine + 1) - innerHeight);
        return true;
    }

    boolean scrollTop(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;
        if (getTopLine(widget) < 0) return false;

        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(0));
        return true;
    }

    boolean scrollBottom(TextView widget, Spannable buffer)
    {
        Layout layout = widget.getLayout();
        if (layout == null) return false;

        int lineCount = layout.getLineCount();
        if (getBottomLine(widget) > lineCount - 1) return false;

        Touch.scrollTo(widget, layout, widget.getScrollX(), layout.getLineTop(lineCount) - getInnerHeight(widget));
        return true;
    }

    boolean scrollLineStart(TextView widget, Spannable buffer)
    {
        if (widget.getLayout() == null) return false;

        int minimumScrollX = getScrollBoundsLeft(widget);
        int scrollX = widget.getScrollX();

        if (scrollX <= minimumScrollX) return false;

        widget.scrollTo(minimumScrollX, widget.getScrollY());
        return true;
    }

    boolean scrollLineEnd(TextView widget, Spannable buffer)
    {
        if (widget.getLayout() == null) return false;

        int maximumScrollX = getScrollBoundsRight(widget) - getInnerWidth(widget);
        int scrollX = widget.getScrollX();

        if (scrollX >= maximumScrollX) return false;

        widget.scrollTo(maximumScrollX, widget.getScrollY());
        return true;
    }
}

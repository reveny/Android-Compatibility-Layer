package android.text.method;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class Touch
{
    private Touch()
    {
    }

    public static void scrollTo(TextView widget, Layout layout, int x, int y)
    {
        int horizontalPadding = widget.getTotalPaddingLeft() + widget.getTotalPaddingRight();
        int availableWidth = widget.getWidth() - horizontalPadding;

        int top = layout.getLineForVertical(y);
        int bottom = layout.getLineForVertical(y + widget.getHeight() - widget.getTotalPaddingTop() - widget.getTotalPaddingBottom());

        int left = Integer.MAX_VALUE;
        int right = 0;
        Layout.Alignment alignment = layout.getAlignment();
        boolean leftToRight = layout.getParagraphDirection(top) > 0;

        for (int line = top; line <= bottom; line++)
        {
            left = (int)Math.min(left, layout.getLineLeft(line));
            right = (int)Math.max(right, layout.getLineRight(line));
        }

        int actualWidth = right - left;

        if (actualWidth < availableWidth)
        {
            if (alignment == Layout.Alignment.ALIGN_CENTER)
            {
                x = left - ((availableWidth - actualWidth) / 2);
            }
            else if ((leftToRight && alignment == Layout.Alignment.ALIGN_OPPOSITE)
                     || (!leftToRight && alignment == Layout.Alignment.ALIGN_NORMAL) || alignment == Layout.Alignment.ALIGN_RIGHT)
            {
                x = left - (availableWidth - actualWidth);
            }
            else
            {
                x = left;
            }
        }
        else
        {
            x = Math.min(x, right - availableWidth);
            x = Math.max(x, left);
        }

        widget.scrollTo(x, y);
    }

    public static boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
    {
        DragState[] states;

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                states = buffer.getSpans(0, buffer.length(), DragState.class);
                for (DragState state : states) buffer.removeSpan(state);

                buffer.setSpan(new DragState(event.getX(), event.getY(), widget.getScrollX(), widget.getScrollY()), 0, 0,
                               Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                return true;
            case MotionEvent.ACTION_UP:
                states = buffer.getSpans(0, buffer.length(), DragState.class);
                for (DragState state : states) buffer.removeSpan(state);

                return states.length > 0 && states[0].mUsed;
            case MotionEvent.ACTION_MOVE:
                states = buffer.getSpans(0, buffer.length(), DragState.class);
                if (states.length == 0) return false;

                DragState state = states[0];

                if (!state.mFarEnough)
                {
                    int slop = ViewConfiguration.get(widget.getContext()).getScaledTouchSlop();
                    if (Math.abs(event.getX() - state.mX) >= slop || Math.abs(event.getY() - state.mY) >= slop) state.mFarEnough = true;
                }

                if (!state.mFarEnough) return false;

                Layout layout = widget.getLayout();
                if (layout == null) return false;

                state.mUsed = true;

                boolean capturing = (event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0;

                float deltaX;
                float deltaY;

                if (capturing)
                {
                    deltaX = event.getX() - state.mX;
                    deltaY = event.getY() - state.mY;
                }
                else
                {
                    deltaX = state.mX - event.getX();
                    deltaY = state.mY - event.getY();
                }

                state.mX = event.getX();
                state.mY = event.getY();

                int newX = widget.getScrollX() + (int)deltaX;
                int newY = widget.getScrollY() + (int)deltaY;

                int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
                newY = Math.min(newY, layout.getHeight() - (widget.getHeight() - padding));
                newY = Math.max(newY, 0);

                int oldX = widget.getScrollX();
                int oldY = widget.getScrollY();

                scrollTo(widget, layout, newX, newY);

                if (oldX != widget.getScrollX() || oldY != widget.getScrollY()) widget.cancelLongPress();
                return true;
            default:
                return false;
        }
    }

    public static int getInitialScrollX(TextView widget, Spannable buffer)
    {
        DragState[] states = buffer.getSpans(0, buffer.length(), DragState.class);
        return states.length > 0 ? states[0].mScrollX : -1;
    }

    public static int getInitialScrollY(TextView widget, Spannable buffer)
    {
        DragState[] states = buffer.getSpans(0, buffer.length(), DragState.class);
        return states.length > 0 ? states[0].mScrollY : -1;
    }

    private static class DragState implements NoCopySpan
    {
        public float mX;
        public float mY;
        public int mScrollX;
        public int mScrollY;
        public boolean mFarEnough;
        public boolean mUsed;

        DragState(float x, float y, int scrollX, int scrollY)
        {
            mX = x;
            mY = y;
            mScrollX = scrollX;
            mScrollY = scrollY;
        }
    }
}

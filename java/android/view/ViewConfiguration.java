package android.view;

import android.content.Context;

public class ViewConfiguration
{
    private static final int SCROLL_BAR_SIZE = 10;
    private static final int TOUCH_SLOP = 8;
    private static final int PAGING_TOUCH_SLOP = TOUCH_SLOP * 2;
    private static final int DOUBLE_TAP_SLOP = 100;
    private static final int MINIMUM_FLING_VELOCITY = 50;
    private static final int MAXIMUM_FLING_VELOCITY = 8000;
    private static final int TAP_TIMEOUT = 100;
    private static final int LONG_PRESS_TIMEOUT = 500;
    private static final int DOUBLE_TAP_TIMEOUT = 300;
    private static final int PRESSED_STATE_DURATION = 64;
    private static final int HOVER_TAP_TIMEOUT = 150;
    private static final int SCROLL_FRICTION_HUNDREDTHS = 15;

    private final float mDensity;

    private ViewConfiguration(Context context)
    {
        float density = 1.0f;

        if (context != null && context.getResources() != null && context.getResources().getDisplayMetrics() != null)
        {
            density = context.getResources().getDisplayMetrics().density;
        }

        mDensity = density <= 0 ? 1.0f : density;
    }

    public static ViewConfiguration get(Context context)
    {
        return new ViewConfiguration(context);
    }

    public int getScaledScrollBarSize()
    {
        return scaled(SCROLL_BAR_SIZE);
    }

    public int getScaledTouchSlop()
    {
        return scaled(TOUCH_SLOP);
    }

    public int getScaledPagingTouchSlop()
    {
        return scaled(PAGING_TOUCH_SLOP);
    }

    public int getScaledDoubleTapSlop()
    {
        return scaled(DOUBLE_TAP_SLOP);
    }

    public int getScaledMinimumFlingVelocity()
    {
        return scaled(MINIMUM_FLING_VELOCITY);
    }

    public int getScaledMaximumFlingVelocity()
    {
        return scaled(MAXIMUM_FLING_VELOCITY);
    }

    public int getScaledEdgeSlop()
    {
        return scaled(TOUCH_SLOP);
    }

    public int getScaledWindowTouchSlop()
    {
        return scaled(TOUCH_SLOP * 2);
    }

    public int getScaledHoverSlop()
    {
        return scaled(TOUCH_SLOP / 2);
    }

    public int getScaledOverscrollDistance()
    {
        return 0;
    }

    public int getScaledOverflingDistance()
    {
        return scaled(6);
    }

    public float getScaledHorizontalScrollFactor()
    {
        return scaled(64);
    }

    public float getScaledVerticalScrollFactor()
    {
        return scaled(64);
    }

    public int getScaledMinimumScalingSpan()
    {
        return scaled(27);
    }

    public static int getScrollBarFadeDuration()
    {
        return 250;
    }

    public static int getScrollDefaultDelay()
    {
        return 300;
    }

    public static int getTapTimeout()
    {
        return TAP_TIMEOUT;
    }

    public static int getLongPressTimeout()
    {
        return LONG_PRESS_TIMEOUT;
    }

    public static int getDoubleTapTimeout()
    {
        return DOUBLE_TAP_TIMEOUT;
    }

    public static int getPressedStateDuration()
    {
        return PRESSED_STATE_DURATION;
    }

    public static int getJumpTapTimeout()
    {
        return LONG_PRESS_TIMEOUT;
    }

    public static int getKeyRepeatTimeout()
    {
        return LONG_PRESS_TIMEOUT;
    }

    public static int getKeyRepeatDelay()
    {
        return 50;
    }

    public static int getHoverTapTimeout()
    {
        return HOVER_TAP_TIMEOUT;
    }

    public static int getHoverTapSlop()
    {
        return 20;
    }

    public static float getScrollFriction()
    {
        return SCROLL_FRICTION_HUNDREDTHS / 100.0f;
    }

    public static long getDefaultActionModeHideDuration()
    {
        return 2000;
    }

    public boolean hasPermanentMenuKey()
    {
        return false;
    }

    public boolean shouldShowMenuShortcutsWhenKeyboardPresent()
    {
        return false;
    }

    private int scaled(int value)
    {
        return (int)(value * mDensity + 0.5f);
    }
}

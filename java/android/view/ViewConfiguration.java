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

    private static final int FADING_EDGE_LENGTH = 12;
    private static final int EDGE_SLOP = 12;
    private static final int WINDOW_TOUCH_SLOP = 16;
    private static final int HANDWRITING_SLOP = 2;
    private static final int HANDWRITING_GESTURE_LINE_MARGIN = 16;
    private static final int MAXIMUM_DRAWING_CACHE_SIZE = 480 * 800 * 4;
    private static final int MULTI_PRESS_TIMEOUT = 300;
    private static final long ZOOM_CONTROLS_TIMEOUT = 3000;
    private static final long GLOBAL_ACTIONS_KEY_TIMEOUT = 500;
    private static final float AMBIGUOUS_GESTURE_MULTIPLIER = 2.0f;
    private static final int AXIS_SCROLL = 26;
    private static final int NO_FLING_MINIMUM_VELOCITY = Integer.MAX_VALUE;
    private static final int NO_FLING_MAXIMUM_VELOCITY = Integer.MIN_VALUE;

    private final float mDensity;

    public ViewConfiguration()
    {
        mDensity = 1.0f;
    }

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

    public static int getScrollBarSize()
    {
        return SCROLL_BAR_SIZE;
    }

    public static int getFadingEdgeLength()
    {
        return FADING_EDGE_LENGTH;
    }

    public int getScaledFadingEdgeLength()
    {
        return scaled(FADING_EDGE_LENGTH);
    }

    public static int getMultiPressTimeout()
    {
        return MULTI_PRESS_TIMEOUT;
    }

    public static int getEdgeSlop()
    {
        return EDGE_SLOP;
    }

    public static int getTouchSlop()
    {
        return TOUCH_SLOP;
    }

    public int getScaledHandwritingSlop()
    {
        return scaled(HANDWRITING_SLOP);
    }

    public int getScaledHandwritingGestureLineMargin()
    {
        return scaled(HANDWRITING_GESTURE_LINE_MARGIN);
    }

    public static int getWindowTouchSlop()
    {
        return WINDOW_TOUCH_SLOP;
    }

    public static int getMinimumFlingVelocity()
    {
        return MINIMUM_FLING_VELOCITY;
    }

    public static int getMaximumFlingVelocity()
    {
        return MAXIMUM_FLING_VELOCITY;
    }

    public static int getMaximumDrawingCacheSize()
    {
        return MAXIMUM_DRAWING_CACHE_SIZE;
    }

    public int getScaledMaximumDrawingCacheSize()
    {
        return MAXIMUM_DRAWING_CACHE_SIZE;
    }

    public static long getZoomControlsTimeout()
    {
        return ZOOM_CONTROLS_TIMEOUT;
    }

    public static long getGlobalActionKeyTimeout()
    {
        return GLOBAL_ACTIONS_KEY_TIMEOUT;
    }

    public static float getAmbiguousGestureMultiplier()
    {
        return AMBIGUOUS_GESTURE_MULTIPLIER;
    }

    public float getScaledAmbiguousGestureMultiplier()
    {
        return AMBIGUOUS_GESTURE_MULTIPLIER;
    }

    public int getScaledMinimumFlingVelocity(int inputDeviceId, int axis, int source)
    {
        if (!isFlingSupported(axis)) return NO_FLING_MINIMUM_VELOCITY;
        return getScaledMinimumFlingVelocity();
    }

    public int getScaledMaximumFlingVelocity(int inputDeviceId, int axis, int source)
    {
        if (!isFlingSupported(axis)) return NO_FLING_MAXIMUM_VELOCITY;
        return getScaledMaximumFlingVelocity();
    }

    private static boolean isFlingSupported(int axis)
    {
        return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y || axis == AXIS_SCROLL;
    }

    private int scaled(int value)
    {
        return (int)(value * mDensity + 0.5f);
    }
}

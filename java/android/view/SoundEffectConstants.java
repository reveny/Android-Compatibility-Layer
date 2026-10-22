package android.view;

public class SoundEffectConstants
{
    public static final int CLICK = 0;
    public static final int NAVIGATION_LEFT = 1;
    public static final int NAVIGATION_UP = 2;
    public static final int NAVIGATION_RIGHT = 3;
    public static final int NAVIGATION_DOWN = 4;
    public static final int NAVIGATION_REPEAT_LEFT = 5;
    public static final int NAVIGATION_REPEAT_UP = 6;
    public static final int NAVIGATION_REPEAT_RIGHT = 7;
    public static final int NAVIGATION_REPEAT_DOWN = 8;

    private SoundEffectConstants()
    {
    }

    public static int getContantForFocusDirection(int direction)
    {
        switch (direction)
        {
            case View.FOCUS_RIGHT:
                return NAVIGATION_RIGHT;
            case View.FOCUS_FORWARD:
            case View.FOCUS_DOWN:
                return NAVIGATION_DOWN;
            case View.FOCUS_LEFT:
                return NAVIGATION_LEFT;
            case View.FOCUS_BACKWARD:
            case View.FOCUS_UP:
                return NAVIGATION_UP;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
                                                   + "FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }

    public static int getConstantForFocusDirection(int direction, boolean repeating)
    {
        if (!repeating) return getContantForFocusDirection(direction);

        switch (direction)
        {
            case View.FOCUS_RIGHT:
                return NAVIGATION_REPEAT_RIGHT;
            case View.FOCUS_FORWARD:
            case View.FOCUS_DOWN:
                return NAVIGATION_REPEAT_DOWN;
            case View.FOCUS_LEFT:
                return NAVIGATION_REPEAT_LEFT;
            case View.FOCUS_BACKWARD:
            case View.FOCUS_UP:
                return NAVIGATION_REPEAT_UP;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
                                                   + "FOCUS_FORWARD, FOCUS_BACKWARD}.");
        }
    }
}

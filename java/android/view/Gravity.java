package android.view;

import android.graphics.Rect;

public class Gravity
{
    public static final int NO_GRAVITY = 0x0000;

    public static final int AXIS_SPECIFIED = 0x0001;
    public static final int AXIS_PULL_BEFORE = 0x0002;
    public static final int AXIS_PULL_AFTER = 0x0004;
    public static final int AXIS_CLIP = 0x0008;

    public static final int AXIS_X_SHIFT = 0;
    public static final int AXIS_Y_SHIFT = 4;

    public static final int TOP = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    public static final int BOTTOM = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_Y_SHIFT;
    public static final int LEFT = (AXIS_PULL_BEFORE | AXIS_SPECIFIED) << AXIS_X_SHIFT;
    public static final int RIGHT = (AXIS_PULL_AFTER | AXIS_SPECIFIED) << AXIS_X_SHIFT;

    public static final int CENTER_VERTICAL = AXIS_SPECIFIED << AXIS_Y_SHIFT;
    public static final int CENTER_HORIZONTAL = AXIS_SPECIFIED << AXIS_X_SHIFT;
    public static final int CENTER = CENTER_VERTICAL | CENTER_HORIZONTAL;

    public static final int FILL_VERTICAL = TOP | BOTTOM;
    public static final int FILL_HORIZONTAL = LEFT | RIGHT;
    public static final int FILL = FILL_VERTICAL | FILL_HORIZONTAL;

    public static final int CLIP_VERTICAL = AXIS_CLIP << AXIS_Y_SHIFT;
    public static final int CLIP_HORIZONTAL = AXIS_CLIP << AXIS_X_SHIFT;

    public static final int VERTICAL_GRAVITY_MASK = (AXIS_SPECIFIED | AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_Y_SHIFT;
    public static final int HORIZONTAL_GRAVITY_MASK = (AXIS_SPECIFIED | AXIS_PULL_BEFORE | AXIS_PULL_AFTER) << AXIS_X_SHIFT;

    public static final int RELATIVE_LAYOUT_DIRECTION = 0x00800000;
    public static final int START = RELATIVE_LAYOUT_DIRECTION | LEFT;
    public static final int END = RELATIVE_LAYOUT_DIRECTION | RIGHT;
    public static final int RELATIVE_HORIZONTAL_GRAVITY_MASK = START | END;

    public static final int DISPLAY_CLIP_VERTICAL = 0x10000000;
    public static final int DISPLAY_CLIP_HORIZONTAL = 0x01000000;

    private Gravity()
    {
    }

    public static boolean isHorizontal(int gravity)
    {
        return (gravity & HORIZONTAL_GRAVITY_MASK) != 0;
    }

    public static boolean isVertical(int gravity)
    {
        return (gravity & VERTICAL_GRAVITY_MASK) != 0;
    }

    public static int getAbsoluteGravity(int gravity, int layoutDirection)
    {
        int absolute = gravity & ~RELATIVE_LAYOUT_DIRECTION;
        if ((gravity & RELATIVE_LAYOUT_DIRECTION) == 0) return absolute;

        boolean rightToLeft = layoutDirection == 1;
        if ((gravity & START) == START) return (absolute & ~HORIZONTAL_GRAVITY_MASK) | (rightToLeft ? RIGHT : LEFT);
        if ((gravity & END) == END) return (absolute & ~HORIZONTAL_GRAVITY_MASK) | (rightToLeft ? LEFT : RIGHT);

        return absolute;
    }

    public static void apply(int gravity, int width, int height, Rect container, Rect out)
    {
        applyHorizontal(gravity, width, container, out);
        applyVertical(gravity, height, container, out);
    }

    private static void applyHorizontal(int gravity, int width, Rect container, Rect out)
    {
        int horizontal = gravity & HORIZONTAL_GRAVITY_MASK;

        if (horizontal == FILL_HORIZONTAL)
        {
            out.left = container.left;
            out.right = container.right;
            return;
        }

        if (horizontal == RIGHT) out.left = container.right - width;
        else if (horizontal == CENTER_HORIZONTAL) out.left = container.left + (container.width() - width) / 2;
        else out.left = container.left;

        out.right = out.left + width;
    }

    private static void applyVertical(int gravity, int height, Rect container, Rect out)
    {
        int vertical = gravity & VERTICAL_GRAVITY_MASK;

        if (vertical == FILL_VERTICAL)
        {
            out.top = container.top;
            out.bottom = container.bottom;
            return;
        }

        if (vertical == BOTTOM) out.top = container.bottom - height;
        else if (vertical == CENTER_VERTICAL) out.top = container.top + (container.height() - height) / 2;
        else out.top = container.top;

        out.bottom = out.top + height;
    }
}

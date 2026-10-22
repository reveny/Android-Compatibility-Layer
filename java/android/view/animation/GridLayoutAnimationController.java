package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class GridLayoutAnimationController extends LayoutAnimationController
{
    public static final int DIRECTION_LEFT_TO_RIGHT = 0x0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 0x1;
    public static final int DIRECTION_TOP_TO_BOTTOM = 0x0;
    public static final int DIRECTION_BOTTOM_TO_TOP = 0x2;
    public static final int DIRECTION_HORIZONTAL_MASK = 0x1;
    public static final int DIRECTION_VERTICAL_MASK = 0x2;
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_COLUMN = 1;
    public static final int PRIORITY_ROW = 2;

    private static final String[] PRIORITY_NAMES = {"none", "column", "row"};
    private static final int[] PRIORITY_VALUES = {PRIORITY_NONE, PRIORITY_COLUMN, PRIORITY_ROW};

    private float mColumnDelay;
    private float mRowDelay;
    private int mDirection;
    private int mDirectionPriority;

    public GridLayoutAnimationController(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);
        mColumnDelay = values.getDescription("columnDelay").value;
        mRowDelay = values.getDescription("rowDelay").value;
        mDirection = parseDirection(attributes, values);
        mDirectionPriority = values.getEnum("directionPriority", PRIORITY_NAMES, PRIORITY_VALUES, PRIORITY_NONE);
    }

    public GridLayoutAnimationController(Animation animation)
    {
        this(animation, 0.5f, 0.5f);
    }

    public GridLayoutAnimationController(Animation animation, float columnDelay, float rowDelay)
    {
        super(animation);
        mColumnDelay = columnDelay;
        mRowDelay = rowDelay;
    }

    private static int parseDirection(AttributeSet attributes, AnimationAttributes values)
    {
        if (!values.has("direction")) return DIRECTION_LEFT_TO_RIGHT | DIRECTION_TOP_TO_BOTTOM;

        int numeric = values.getInt("direction", -1);
        if (numeric >= 0) return numeric;

        String text = attributes.getAttributeValue(null, "direction");
        if (text == null) return DIRECTION_LEFT_TO_RIGHT | DIRECTION_TOP_TO_BOTTOM;

        int direction = 0;
        for (String part : text.split("\\|"))
        {
            String flag = part.trim();
            if (flag.equals("right_to_left")) direction |= DIRECTION_RIGHT_TO_LEFT;
            else if (flag.equals("bottom_to_top")) direction |= DIRECTION_BOTTOM_TO_TOP;
        }

        return direction;
    }

    public float getColumnDelay()
    {
        return mColumnDelay;
    }

    public void setColumnDelay(float columnDelay)
    {
        mColumnDelay = columnDelay;
    }

    public float getRowDelay()
    {
        return mRowDelay;
    }

    public void setRowDelay(float rowDelay)
    {
        mRowDelay = rowDelay;
    }

    public int getDirection()
    {
        return mDirection;
    }

    public void setDirection(int direction)
    {
        mDirection = direction;
    }

    public int getDirectionPriority()
    {
        return mDirectionPriority;
    }

    public void setDirectionPriority(int directionPriority)
    {
        mDirectionPriority = directionPriority;
    }

    @Override
    public boolean willOverlap()
    {
        return mColumnDelay < 1.0f || mRowDelay < 1.0f;
    }

    @Override
    protected long getDelayForView(View view)
    {
        LayoutAnimationController.AnimationParameters base = parametersOf(view);
        if (!(base instanceof AnimationParameters) || mAnimation == null) return 0;

        AnimationParameters parameters = (AnimationParameters)base;

        int column = getTransformedColumnIndex(parameters);
        int row = getTransformedRowIndex(parameters);

        int rowsCount = parameters.rowsCount;
        int columnsCount = parameters.columnsCount;

        long duration = mAnimation.getDuration();
        float columnDelay = mColumnDelay * duration;
        float rowDelay = mRowDelay * duration;

        float totalDelay;
        long viewDelay;

        if (mInterpolator == null) mInterpolator = new LinearInterpolator();

        switch (mDirectionPriority)
        {
            case PRIORITY_COLUMN:
                viewDelay = (long)(row * rowDelay + column * rowsCount * rowDelay);
                totalDelay = rowsCount * rowDelay + columnsCount * rowsCount * rowDelay;
                break;
            case PRIORITY_ROW:
                viewDelay = (long)(column * columnDelay + row * columnsCount * columnDelay);
                totalDelay = columnsCount * columnDelay + rowsCount * columnsCount * columnDelay;
                break;
            case PRIORITY_NONE:
            default:
                viewDelay = (long)(column * columnDelay + row * rowDelay);
                totalDelay = columnsCount * columnDelay + rowsCount * rowDelay;
                break;
        }

        if (totalDelay == 0) return 0;

        float normalizedDelay = viewDelay / totalDelay;
        normalizedDelay = mInterpolator.getInterpolation(normalizedDelay);

        return (long)(normalizedDelay * totalDelay);
    }

    private int getTransformedColumnIndex(AnimationParameters parameters)
    {
        int index;

        switch (getOrder())
        {
            case ORDER_REVERSE:
                index = parameters.columnsCount - 1 - parameters.column;
                break;
            case ORDER_RANDOM:
                if (mRandomizer == null) mRandomizer = new Random();
                index = (int)(parameters.columnsCount * mRandomizer.nextFloat());
                break;
            case ORDER_NORMAL:
            default:
                index = parameters.column;
                break;
        }

        if ((mDirection & DIRECTION_HORIZONTAL_MASK) == DIRECTION_RIGHT_TO_LEFT) index = parameters.columnsCount - 1 - index;

        return index;
    }

    private int getTransformedRowIndex(AnimationParameters parameters)
    {
        int index;

        switch (getOrder())
        {
            case ORDER_REVERSE:
                index = parameters.rowsCount - 1 - parameters.row;
                break;
            case ORDER_RANDOM:
                if (mRandomizer == null) mRandomizer = new Random();
                index = (int)(parameters.rowsCount * mRandomizer.nextFloat());
                break;
            case ORDER_NORMAL:
            default:
                index = parameters.row;
                break;
        }

        if ((mDirection & DIRECTION_VERTICAL_MASK) == DIRECTION_BOTTOM_TO_TOP) index = parameters.rowsCount - 1 - index;

        return index;
    }

    public static class AnimationParameters extends LayoutAnimationController.AnimationParameters
    {
        public int column;
        public int row;
        public int columnsCount;
        public int rowsCount;

        public AnimationParameters()
        {
        }
    }
}

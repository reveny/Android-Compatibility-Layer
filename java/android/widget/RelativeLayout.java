package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class RelativeLayout extends ViewGroup
{
    public static final int TRUE = -1;

    public static final int LEFT_OF = 0;
    public static final int RIGHT_OF = 1;
    public static final int ABOVE = 2;
    public static final int BELOW = 3;
    public static final int ALIGN_BASELINE = 4;
    public static final int ALIGN_LEFT = 5;
    public static final int ALIGN_TOP = 6;
    public static final int ALIGN_RIGHT = 7;
    public static final int ALIGN_BOTTOM = 8;
    public static final int ALIGN_PARENT_LEFT = 9;
    public static final int ALIGN_PARENT_TOP = 10;
    public static final int ALIGN_PARENT_RIGHT = 11;
    public static final int ALIGN_PARENT_BOTTOM = 12;
    public static final int CENTER_IN_PARENT = 13;
    public static final int CENTER_HORIZONTAL = 14;
    public static final int CENTER_VERTICAL = 15;
    public static final int START_OF = 16;
    public static final int END_OF = 17;
    public static final int ALIGN_START = 18;
    public static final int ALIGN_END = 19;
    public static final int ALIGN_PARENT_START = 20;
    public static final int ALIGN_PARENT_END = 21;

    private static final int RULE_COUNT = 22;
    private static final int UNRESOLVED = Integer.MIN_VALUE;

    private int mGravity = Gravity.START | Gravity.TOP;

    public RelativeLayout(Context context)
    {
        super(context);
    }

    public RelativeLayout(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public RelativeLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public RelativeLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public void setGravity(int gravity)
    {
        if (mGravity == gravity) return;

        mGravity = gravity;
        requestLayout();
    }

    public void setHorizontalGravity(int gravity)
    {
        setGravity((mGravity & ~Gravity.HORIZONTAL_GRAVITY_MASK) | (gravity & Gravity.HORIZONTAL_GRAVITY_MASK));
    }

    public void setVerticalGravity(int gravity)
    {
        setGravity((mGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | (gravity & Gravity.VERTICAL_GRAVITY_MASK));
    }

    public void setIgnoreGravity(int viewId)
    {
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof LayoutParams) return (LayoutParams)source;
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);
        return new LayoutParams(source);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof LayoutParams;
    }

    private LayoutParams paramsFor(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = generateLayoutParams(params == null ? generateDefaultLayoutParams() : params);
        child.setLayoutParams(converted);

        return converted;
    }

    private View childWithId(int id)
    {
        if (id == View.NO_ID) return null;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getId() == id) return child;
        }

        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int innerWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        final int innerHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            LayoutParams params = paramsFor(child);

            params.left = UNRESOLVED;
            params.top = UNRESOLVED;
            params.right = UNRESOLVED;
            params.bottom = UNRESOLVED;

            if (child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }

        for (int pass = 0; pass < 2; pass++)
        {
            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;

                place(child, paramsFor(child), innerWidth, innerHeight, widthMode, heightMode);
            }
        }

        int widest = 0;
        int tallest = 0;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);
            widest = Math.max(widest, params.right + params.rightMargin);
            tallest = Math.max(tallest, params.bottom + params.bottomMargin);
        }

        widest = Math.max(widest + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());
        tallest = Math.max(tallest + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSize(widest, widthMeasureSpec), resolveSize(tallest, heightMeasureSpec));
    }

    private void place(View child, LayoutParams params, int innerWidth, int innerHeight, int widthMode, int heightMode)
    {
        final int[] rules = params.getRules();
        final boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        int left = UNRESOLVED;
        int top = UNRESOLVED;
        int right = UNRESOLVED;
        int bottom = UNRESOLVED;

        int width = child.getMeasuredWidth();
        int height = child.getMeasuredHeight();

        if (rules[ALIGN_PARENT_LEFT] == TRUE || (rules[ALIGN_PARENT_START] == TRUE && !rightToLeft)
            || (rules[ALIGN_PARENT_END] == TRUE && rightToLeft))
        {
            left = params.leftMargin;
        }

        if (rules[ALIGN_PARENT_RIGHT] == TRUE || (rules[ALIGN_PARENT_END] == TRUE && !rightToLeft)
            || (rules[ALIGN_PARENT_START] == TRUE && rightToLeft))
        {
            right = innerWidth - params.rightMargin;
        }

        if (rules[ALIGN_PARENT_TOP] == TRUE) top = params.topMargin;
        if (rules[ALIGN_PARENT_BOTTOM] == TRUE) bottom = innerHeight - params.bottomMargin;

        left = fromSibling(rules[rightToLeft ? END_OF : START_OF] != 0 ? rules[rightToLeft ? END_OF : START_OF] : rules[RIGHT_OF],
                           Edge.RIGHT, params.leftMargin, left);
        right = fromSibling(rules[rightToLeft ? START_OF : END_OF] != 0 ? rules[rightToLeft ? START_OF : END_OF] : rules[LEFT_OF],
                            Edge.LEFT, -params.rightMargin, right);
        top = fromSibling(rules[BELOW], Edge.BOTTOM, params.topMargin, top);
        bottom = fromSibling(rules[ABOVE], Edge.TOP, -params.bottomMargin, bottom);

        left = fromSibling(rules[ALIGN_LEFT] != 0 ? rules[ALIGN_LEFT] : rules[ALIGN_START], Edge.LEFT, params.leftMargin, left);
        right = fromSibling(rules[ALIGN_RIGHT] != 0 ? rules[ALIGN_RIGHT] : rules[ALIGN_END], Edge.RIGHT, -params.rightMargin, right);
        top = fromSibling(rules[ALIGN_TOP], Edge.TOP, params.topMargin, top);
        bottom = fromSibling(rules[ALIGN_BOTTOM], Edge.BOTTOM, -params.bottomMargin, bottom);

        if (left != UNRESOLVED && right != UNRESOLVED && right - left != width && right > left)
        {
            width = right - left;
            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(height, height > 0 ? MeasureSpec.EXACTLY : MeasureSpec.UNSPECIFIED));
            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
        }

        if (top != UNRESOLVED && bottom != UNRESOLVED && bottom - top != height && bottom > top)
        {
            height = bottom - top;
            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
        }

        final boolean centreHorizontally = rules[CENTER_IN_PARENT] == TRUE || rules[CENTER_HORIZONTAL] == TRUE;
        final boolean centreVertically = rules[CENTER_IN_PARENT] == TRUE || rules[CENTER_VERTICAL] == TRUE;

        if (centreHorizontally && left == UNRESOLVED && right == UNRESOLVED)
        {
            left = (innerWidth - width) / 2 + params.leftMargin - params.rightMargin;
        }

        if (centreVertically && top == UNRESOLVED && bottom == UNRESOLVED)
        {
            top = (innerHeight - height) / 2 + params.topMargin - params.bottomMargin;
        }

        if (left == UNRESOLVED && right != UNRESOLVED) left = right - width;
        if (top == UNRESOLVED && bottom != UNRESOLVED) top = bottom - height;

        if (left == UNRESOLVED) left = params.leftMargin;
        if (top == UNRESOLVED) top = params.topMargin;

        params.left = left;
        params.top = top;
        params.right = left + width;
        params.bottom = top + height;
    }

    private enum Edge
    {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }

    private int fromSibling(int siblingId, Edge edge, int offset, int current)
    {
        if (current != UNRESOLVED || siblingId == 0 || siblingId == TRUE) return current;

        View sibling = childWithId(siblingId);
        if (sibling == null || sibling.getVisibility() == GONE) return current;

        LayoutParams params = paramsFor(sibling);
        int anchor;

        switch (edge)
        {
            case LEFT: anchor = params.left; break;
            case TOP: anchor = params.top; break;
            case RIGHT: anchor = params.right; break;
            default: anchor = params.bottom; break;
        }

        if (anchor == UNRESOLVED) return current;

        return anchor + offset;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        final int offsetLeft = getPaddingLeft();
        final int offsetTop = getPaddingTop();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);

            child.layout(offsetLeft + params.left, offsetTop + params.top, offsetLeft + params.right, offsetTop + params.bottom);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        private static StyleableSet sStyleable;

        private static final String[] RULE_ATTRIBUTES = {
            "layout_toLeftOf",        "layout_toRightOf",       "layout_above",           "layout_below",
            "layout_alignBaseline",   "layout_alignLeft",       "layout_alignTop",        "layout_alignRight",
            "layout_alignBottom",     "layout_alignParentLeft", "layout_alignParentTop",  "layout_alignParentRight",
            "layout_alignParentBottom", "layout_centerInParent", "layout_centerHorizontal", "layout_centerVertical",
            "layout_toStartOf",       "layout_toEndOf",         "layout_alignStart",      "layout_alignEnd",
            "layout_alignParentStart", "layout_alignParentEnd",
        };

        private final int[] mRules = new int[RULE_COUNT];

        int left;
        int top;
        int right;
        int bottom;

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);

            if (attributes == null) return;

            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), RULE_ATTRIBUTES);
            if (sStyleable.isEmpty()) return;

            android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

            for (int rule = 0; rule < RULE_ATTRIBUTES.length; rule++)
            {
                int index = sStyleable.indexOf(RULE_ATTRIBUTES[rule]);
                if (index < 0) continue;

                if (rule >= ALIGN_PARENT_LEFT && rule <= CENTER_VERTICAL)
                {
                    if (array.getBoolean(index, false)) mRules[rule] = TRUE;
                }
                else if (rule == ALIGN_PARENT_START || rule == ALIGN_PARENT_END)
                {
                    if (array.getBoolean(index, false)) mRules[rule] = TRUE;
                }
                else
                {
                    mRules[rule] = array.getResourceId(index, 0);
                }
            }

            array.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source)
        {
            super(source);
        }

        public void addRule(int verb)
        {
            mRules[verb] = TRUE;
        }

        public void addRule(int verb, int anchor)
        {
            mRules[verb] = anchor;
        }

        public void removeRule(int verb)
        {
            mRules[verb] = 0;
        }

        public int getRule(int verb)
        {
            return mRules[verb];
        }

        public int[] getRules()
        {
            return mRules;
        }
    }
}

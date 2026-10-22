package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.StyleableSet;
import android.view.ViewGroup;

public class FrameLayout extends ViewGroup
{
    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

    private static StyleableSet sStyleable;

    private boolean mMeasureAllChildren;
    private final java.util.ArrayList<View> mMatchParentChildren = new java.util.ArrayList<>(1);

    public FrameLayout(Context context)
    {
        super(context);
    }

    public FrameLayout(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        readAttributes(context, attributes, 0, 0);
    }

    public FrameLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
        readAttributes(context, attributes, defStyleAttr, 0);
    }

    public FrameLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
        readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (attributes == null) return;

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "measureAllChildren");
        if (sStyleable.isEmpty()) return;

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);
        mMeasureAllChildren = array.getBoolean(0, false);
        array.recycle();
    }

    @Override
    public void setForegroundGravity(int gravity)
    {
        super.setForegroundGravity(gravity);
    }

    public void setMeasureAllChildren(boolean measureAll)
    {
        mMeasureAllChildren = measureAll;
    }

    @Deprecated
    public boolean getConsiderGoneChildrenWhenMeasuring()
    {
        return getMeasureAllChildren();
    }

    public boolean getMeasureAllChildren()
    {
        return mMeasureAllChildren;
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return FrameLayout.class.getName();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        boolean measureMatchParentChildren = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY
            || MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();

        int widestChild = 0;
        int tallestChild = 0;
        int childState = 0;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (!mMeasureAllChildren && child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            LayoutParams params = paramsFor(child);
            widestChild = Math.max(widestChild, child.getMeasuredWidth() + params.leftMargin + params.rightMargin);
            tallestChild = Math.max(tallestChild, child.getMeasuredHeight() + params.topMargin + params.bottomMargin);
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            if (measureMatchParentChildren && (params.width == LayoutParams.MATCH_PARENT || params.height == LayoutParams.MATCH_PARENT))
            {
                mMatchParentChildren.add(child);
            }
        }

        widestChild = Math.max(widestChild + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());
        tallestChild = Math.max(tallestChild + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());

        android.graphics.drawable.Drawable foreground = getForeground();
        if (foreground != null)
        {
            widestChild = Math.max(widestChild, foreground.getMinimumWidth());
            tallestChild = Math.max(tallestChild, foreground.getMinimumHeight());
        }

        setMeasuredDimension(resolveSizeAndState(widestChild, widthMeasureSpec, childState),
                             resolveSizeAndState(tallestChild, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));

        if (mMatchParentChildren.size() <= 1) return;

        for (int index = 0; index < mMatchParentChildren.size(); index++)
        {
            View child = mMatchParentChildren.get(index);
            LayoutParams params = paramsFor(child);

            int childWidthMeasureSpec;
            if (params.width == LayoutParams.MATCH_PARENT)
            {
                int width = Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - params.leftMargin - params.rightMargin);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            }
            else
            {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight()
                                                            + params.leftMargin + params.rightMargin, params.width);
            }

            int childHeightMeasureSpec;
            if (params.height == LayoutParams.MATCH_PARENT)
            {
                int height = Math.max(0, getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - params.topMargin - params.bottomMargin);
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            }
            else
            {
                childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom()
                                                             + params.topMargin + params.bottomMargin, params.height);
            }

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    private LayoutParams paramsFor(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = generateLayoutParams(params == null ? generateDefaultLayoutParams() : params);
        child.setLayoutParams(converted);
        return converted;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        int innerLeft = getPaddingLeft();
        int innerTop = getPaddingTop();
        int innerRight = right - left - getPaddingRight();
        int innerBottom = bottom - top - getPaddingBottom();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            LayoutParams params = paramsFor(child);

            int gravity = params.gravity;
            if (gravity == LayoutParams.UNSPECIFIED_GRAVITY) gravity = DEFAULT_CHILD_GRAVITY;
            gravity = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());

            int horizontal = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;

            int childLeft;
            int childTop;

            if (horizontal == Gravity.CENTER_HORIZONTAL)
            {
                childLeft = innerLeft + (innerRight - innerLeft - childWidth) / 2 + params.leftMargin - params.rightMargin;
            }
            else if (horizontal == Gravity.RIGHT)
            {
                childLeft = innerRight - childWidth - params.rightMargin;
            }
            else
            {
                childLeft = innerLeft + params.leftMargin;
            }

            if (vertical == Gravity.CENTER_VERTICAL)
            {
                childTop = innerTop + (innerBottom - innerTop - childHeight) / 2 + params.topMargin - params.bottomMargin;
            }
            else if (vertical == Gravity.BOTTOM)
            {
                childTop = innerBottom - childHeight - params.bottomMargin;
            }
            else
            {
                childTop = innerTop + params.topMargin;
            }

            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        public static final int UNSPECIFIED_GRAVITY = -1;

        private static StyleableSet sGravityStyleable;

        public int gravity = UNSPECIFIED_GRAVITY;

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity)
        {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
            if (attributes != null) readGravity(context, attributes);
        }

        private void readGravity(Context context, AttributeSet attributes)
        {
            if (sGravityStyleable == null) sGravityStyleable = StyleableSet.of(context.getResources(), "layout_gravity");
            if (sGravityStyleable.isEmpty()) return;

            android.content.res.TypedArray array =
                context.obtainStyledAttributes(attributes, sGravityStyleable.attributes(), 0, 0);
            gravity = array.getInt(0, UNSPECIFIED_GRAVITY);
            array.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source)
        {
            super(source);
        }

        public LayoutParams(LayoutParams source)
        {
            super(source);
            this.gravity = source.gravity;
        }
    }

}

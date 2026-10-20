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

    public FrameLayout(Context context)
    {
        super(context);
    }

    public FrameLayout(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public FrameLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public FrameLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public void setForeground(android.graphics.drawable.Drawable foreground)
    {
    }

    public void setForegroundGravity(int gravity)
    {
    }

    public void setMeasureAllChildren(boolean measureAll)
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
        int widestChild = 0;
        int tallestChild = 0;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            LayoutParams params = paramsFor(child);
            widestChild = Math.max(widestChild, child.getMeasuredWidth() + params.leftMargin + params.rightMargin);
            tallestChild = Math.max(tallestChild, child.getMeasuredHeight() + params.topMargin + params.bottomMargin);
        }

        widestChild = Math.max(widestChild + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth());
        tallestChild = Math.max(tallestChild + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSize(widestChild, widthMeasureSpec), resolveSize(tallestChild, heightMeasureSpec));
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
    }

}

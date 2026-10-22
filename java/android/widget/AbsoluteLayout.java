package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

@Deprecated
public class AbsoluteLayout extends ViewGroup
{
    public AbsoluteLayout(Context context)
    {
        this(context, null);
    }

    public AbsoluteLayout(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AbsoluteLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AbsoluteLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);

            maxWidth = Math.max(maxWidth, params.x + child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, params.y + child.getMeasuredHeight());
        }

        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0), resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    private LayoutParams paramsFor(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = (LayoutParams)generateLayoutParams(params == null ? generateDefaultLayoutParams() : params);
        child.setLayoutParams(converted);
        return converted;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        int count = getChildCount();

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);

            int childLeft = getPaddingLeft() + params.x;
            int childTop = getPaddingTop() + params.y;
            child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        return new LayoutParams(source);
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return false;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams
    {
        private static StyleableSet sStyleable;

        public int x;
        public int y;

        public LayoutParams(int width, int height, int x, int y)
        {
            super(width, height);
            this.x = x;
            this.y = y;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);

            if (attributes == null) return;

            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "layout_x", "layout_y");
            if (sStyleable.isEmpty()) return;

            android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

            int xIndex = sStyleable.indexOf("layout_x");
            int yIndex = sStyleable.indexOf("layout_y");

            if (xIndex >= 0) x = array.getDimensionPixelOffset(xIndex, 0);
            if (yIndex >= 0) y = array.getDimensionPixelOffset(yIndex, 0);

            array.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public String debug(String output)
        {
            return output + "Absolute.LayoutParams={width=" + LinearLayout.LayoutParams.sizeToString(width) + ", height="
                + LinearLayout.LayoutParams.sizeToString(height) + " x=" + x + " y=" + y + "}";
        }
    }
}

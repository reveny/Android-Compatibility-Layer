package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class LinearLayout extends ViewGroup
{
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private int mOrientation = HORIZONTAL;

    public LinearLayout(Context context)
    {
        super(context);
    }

    public LinearLayout(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0, 0);
    }

    public LinearLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public LinearLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes == null) return;

        int identifier = context.getResources().getIdentifier("orientation", "attr", "android");
        if (identifier == 0) return;

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, new int[] {identifier}, defStyleAttr, defStyleRes);
        mOrientation = array.getInt(0, HORIZONTAL);
        array.recycle();
    }

    public int getOrientation()
    {
        return mOrientation;
    }

    public void setOrientation(int orientation)
    {
        if (mOrientation == orientation) return;

        mOrientation = orientation;
        requestLayout();
    }

    public void setGravity(int gravity)
    {
    }

    public void setBaselineAligned(boolean aligned)
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
        if (mOrientation == VERTICAL) return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (mOrientation == VERTICAL) measureVertical(widthMeasureSpec, heightMeasureSpec);
        else measureHorizontal(widthMeasureSpec, heightMeasureSpec);
    }

    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec)
    {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int usedHeight = getPaddingTop() + getPaddingBottom();
        int widestChild = 0;
        float totalWeight = 0f;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);
            totalWeight += params.weight;

            if (params.weight > 0f && heightMode == MeasureSpec.EXACTLY) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, usedHeight);
            usedHeight += child.getMeasuredHeight() + params.topMargin + params.bottomMargin;
            widestChild = Math.max(widestChild, child.getMeasuredWidth() + params.leftMargin + params.rightMargin);
        }

        int height = resolveSize(usedHeight, heightMeasureSpec);

        if (totalWeight > 0f && heightMode == MeasureSpec.EXACTLY)
        {
            int remaining = Math.max(0, height - usedHeight);
            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;

                LayoutParams params = paramsFor(child);
                if (params.weight <= 0f) continue;

                int share = (int)(remaining * (params.weight / totalWeight));
                int childWidthSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight()
                                                         + params.leftMargin + params.rightMargin, params.width);
                child.measure(childWidthSpec, MeasureSpec.makeMeasureSpec(share, MeasureSpec.EXACTLY));
                usedHeight += child.getMeasuredHeight() + params.topMargin + params.bottomMargin;
                widestChild = Math.max(widestChild, child.getMeasuredWidth() + params.leftMargin + params.rightMargin);
            }
        }

        int width = resolveSize(widestChild + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int usedWidth = getPaddingLeft() + getPaddingRight();
        int tallestChild = 0;
        float totalWeight = 0f;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);
            totalWeight += params.weight;

            if (params.weight > 0f && widthMode == MeasureSpec.EXACTLY) continue;

            measureChildWithMargins(child, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);
            usedWidth += child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
            tallestChild = Math.max(tallestChild, child.getMeasuredHeight() + params.topMargin + params.bottomMargin);
        }

        int width = resolveSize(usedWidth, widthMeasureSpec);

        if (totalWeight > 0f && widthMode == MeasureSpec.EXACTLY)
        {
            int remaining = Math.max(0, width - usedWidth);
            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;

                LayoutParams params = paramsFor(child);
                if (params.weight <= 0f) continue;

                int share = (int)(remaining * (params.weight / totalWeight));
                int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom()
                                                          + params.topMargin + params.bottomMargin, params.height);
                child.measure(MeasureSpec.makeMeasureSpec(share, MeasureSpec.EXACTLY), childHeightSpec);
                usedWidth += child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
                tallestChild = Math.max(tallestChild, child.getMeasuredHeight() + params.topMargin + params.bottomMargin);
            }
        }

        int height = resolveSize(tallestChild + getPaddingTop() + getPaddingBottom(), heightMeasureSpec);
        setMeasuredDimension(width, height);
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
        int cursor = mOrientation == VERTICAL ? getPaddingTop() : getPaddingLeft();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            LayoutParams params = paramsFor(child);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (mOrientation == VERTICAL)
            {
                int childLeft = getPaddingLeft() + params.leftMargin;
                cursor += params.topMargin;
                child.layout(childLeft, cursor, childLeft + childWidth, cursor + childHeight);
                cursor += childHeight + params.bottomMargin;
                continue;
            }

            int childTop = getPaddingTop() + params.topMargin;
            cursor += params.leftMargin;
            child.layout(cursor, childTop, cursor + childWidth, childTop + childHeight);
            cursor += childWidth + params.rightMargin;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        private static StyleableSet sChildStyleable;

        public float weight;
        public int gravity = -1;

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight)
        {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
            if (attributes != null) readChildAttributes(context, attributes);
        }

        private void readChildAttributes(Context context, AttributeSet attributes)
        {
            if (sChildStyleable == null)
            {
                sChildStyleable = StyleableSet.of(context.getResources(), "layout_weight", "layout_gravity");
            }
            if (sChildStyleable.isEmpty()) return;

            android.content.res.TypedArray array =
                context.obtainStyledAttributes(attributes, sChildStyleable.attributes(), 0, 0);

            int weightIndex = sChildStyleable.indexOf("layout_weight");
            int gravityIndex = sChildStyleable.indexOf("layout_gravity");

            if (weightIndex >= 0) weight = array.getFloat(weightIndex, weight);
            if (gravityIndex >= 0) gravity = array.getInt(gravityIndex, gravity);

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

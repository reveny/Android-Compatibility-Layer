package android.view;

import android.content.Context;
import android.graphics.Canvas;

public class DecorView extends ViewGroup
{
    private final ViewGroup mContentParent;

    public DecorView(Context context)
    {
        super(context);

        mContentParent = new ContentFrame(context);
        mContentParent.setId(Window.ID_ANDROID_CONTENT);
        addView(mContentParent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public ViewGroup getContentParent()
    {
        return mContentParent;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets)
    {
        if (insets == null) return WindowInsets.empty();

        android.graphics.Insets bars = insets.getSystemWindowInsets();
        mContentParent.setPadding(bars.left, bars.top, bars.right, bars.bottom);

        mContentParent.dispatchApplyWindowInsets(insets);

        return WindowInsets.CONSUMED;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;

            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        int width = right - left;
        int height = bottom - top;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;
            child.layout(0, 0, width, height);
        }
    }

    private static final class ContentFrame extends ViewGroup
    {
        ContentFrame(Context context)
        {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            int horizontal = getPaddingLeft() + getPaddingRight();
            int vertical = getPaddingTop() + getPaddingBottom();

            int innerWidth = Math.max(width - horizontal, 0);
            int innerHeight = Math.max(height - vertical, 0);

            measureChildren(MeasureSpec.makeMeasureSpec(innerWidth, MeasureSpec.getMode(widthMeasureSpec)),
                            MeasureSpec.makeMeasureSpec(innerHeight, MeasureSpec.getMode(heightMeasureSpec)));

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom)
        {
            int innerLeft = getPaddingLeft();
            int innerTop = getPaddingTop();
            int width = Math.max(right - left - innerLeft - getPaddingRight(), 0);
            int height = Math.max(bottom - top - innerTop - getPaddingBottom(), 0);

            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                if (childWidth <= 0) childWidth = width;
                if (childHeight <= 0) childHeight = height;

                child.layout(innerLeft, innerTop, innerLeft + childWidth, innerTop + childHeight);
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas)
        {
            super.dispatchDraw(canvas);
        }
    }
}

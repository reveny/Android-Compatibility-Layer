package android.view;

import android.content.Context;
import android.graphics.Canvas;

public class DecorView extends ViewGroup
{
    private final ViewGroup mContentParent;
    private Window mWindow;
    private View mActionBarView;
    private android.graphics.Insets mBarInsets = android.graphics.Insets.NONE;

    public DecorView(Context context)
    {
        super(context);

        mContentParent = new ContentFrame(context);
        mContentParent.setId(Window.ID_ANDROID_CONTENT);
        addView(mContentParent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void attachWindow(Window window)
    {
        mWindow = window;
    }

    public Window getWindow()
    {
        return mWindow;
    }

    public ViewGroup getContentParent()
    {
        return mContentParent;
    }

    public void setActionBarView(View view)
    {
        if (mActionBarView == view) return;
        if (mActionBarView != null) removeView(mActionBarView);

        mActionBarView = view;
        if (view != null)
        {
            if (view.getParent() instanceof ViewGroup) ((ViewGroup)view.getParent()).removeView(view);
            addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        applyContentPadding();
        requestLayout();
    }

    public View getActionBarView()
    {
        return mActionBarView;
    }

    private boolean hasActionBar()
    {
        return mActionBarView != null && mActionBarView.getVisibility() != GONE;
    }

    private void applyContentPadding()
    {
        int top = hasActionBar() ? 0 : mBarInsets.top;
        mContentParent.setPadding(mBarInsets.left, top, mBarInsets.right, mBarInsets.bottom);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets)
    {
        if (insets == null) return WindowInsets.empty();

        mBarInsets = insets.getSystemWindowInsets();
        applyContentPadding();
        if (mActionBarView != null) mActionBarView.setPadding(mBarInsets.left, 0, mBarInsets.right, 0);

        mContentParent.dispatchApplyWindowInsets(insets);

        return WindowInsets.CONSUMED;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        boolean exactWidth = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY;
        boolean exactHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;

        int minimumWidth = Math.min(getMinimumWidth(), width);
        int measuredWidth = exactWidth ? width : 0;
        int measuredHeight = exactHeight ? height : 0;
        int barHeight = 0;

        if (hasActionBar())
        {
            int barWidthSpec = exactWidth ? MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY) : widthMeasureSpec;
            mActionBarView.measure(barWidthSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            barHeight = mBarInsets.top + mActionBarView.getMeasuredHeight();
        }

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE || child == mActionBarView) continue;

            int contentHeight = Math.max(height - barHeight, 0);
            int childWidthSpec = exactWidth ? MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY) : widthMeasureSpec;
            int childHeightSpec = exactHeight ? MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
                                              : MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.getMode(heightMeasureSpec));

            child.measure(childWidthSpec, childHeightSpec);

            if (!exactWidth && child.getMeasuredWidth() < minimumWidth)
            {
                child.measure(MeasureSpec.makeMeasureSpec(minimumWidth, MeasureSpec.EXACTLY), childHeightSpec);
            }

            if (!exactWidth) measuredWidth = Math.max(measuredWidth, child.getMeasuredWidth());
            if (!exactHeight) measuredHeight = Math.max(measuredHeight, child.getMeasuredHeight() + barHeight);
        }

        if (!exactWidth) measuredWidth = Math.max(measuredWidth, minimumWidth);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        int width = right - left;
        int height = bottom - top;
        int contentTop = 0;

        if (hasActionBar())
        {
            int barTop = mBarInsets.top;
            int barBottom = Math.min(barTop + mActionBarView.getMeasuredHeight(), height);
            mActionBarView.layout(0, barTop, width, barBottom);
            contentTop = barBottom;
        }

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE || child == mActionBarView) continue;
            child.layout(0, contentTop, width, height);
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
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            int horizontal = getPaddingLeft() + getPaddingRight();
            int vertical = getPaddingTop() + getPaddingBottom();

            int innerWidth = Math.max(width - horizontal, 0);
            int innerHeight = Math.max(height - vertical, 0);

            measureChildren(MeasureSpec.makeMeasureSpec(innerWidth, widthMode), MeasureSpec.makeMeasureSpec(innerHeight, heightMode));

            int widest = 0;
            int tallest = 0;

            for (int index = 0; index < getChildCount(); index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == GONE) continue;

                widest = Math.max(widest, child.getMeasuredWidth());
                tallest = Math.max(tallest, child.getMeasuredHeight());
            }

            int measuredWidth = widthMode == MeasureSpec.EXACTLY ? width : widest + horizontal;
            int measuredHeight = heightMode == MeasureSpec.EXACTLY ? height : tallest + vertical;

            if (widthMode == MeasureSpec.AT_MOST) measuredWidth = Math.min(measuredWidth, width);
            if (heightMode == MeasureSpec.AT_MOST) measuredHeight = Math.min(measuredHeight, height);

            setMeasuredDimension(measuredWidth, measuredHeight);
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

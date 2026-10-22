package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class TableRow extends LinearLayout
{
    private int mNumColumns;
    private int[] mColumnWidths;
    private int[] mConstrainedColumnWidths;
    private SparseIntArray mColumnToChildIndex;
    private ChildrenTracker mChildrenTracker;

    public TableRow(Context context)
    {
        super(context);
        initTableRow();
    }

    public TableRow(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        initTableRow();
    }

    private void initTableRow()
    {
        mChildrenTracker = new ChildrenTracker();
        super.setOnHierarchyChangeListener(mChildrenTracker);
    }

    @Override
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener)
    {
        mChildrenTracker.setOnHierarchyChangeListener(listener);
    }

    void setColumnCollapsed(int columnIndex, boolean collapsed)
    {
        View child = getVirtualChildAt(columnIndex);
        if (child != null) child.setVisibility(collapsed ? GONE : VISIBLE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        measureHorizontal(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        layoutHorizontal(left, top, right, bottom);
    }

    @Override
    public View getVirtualChildAt(int index)
    {
        if (mColumnToChildIndex == null) mapIndexAndColumns();

        int deflectedIndex = mColumnToChildIndex.get(index, -1);
        if (deflectedIndex != -1) return getChildAt(deflectedIndex);

        return null;
    }

    @Override
    public int getVirtualChildCount()
    {
        if (mColumnToChildIndex == null) mapIndexAndColumns();

        return mNumColumns;
    }

    private void mapIndexAndColumns()
    {
        if (mColumnToChildIndex != null) return;

        int virtualCount = 0;
        int count = getChildCount();

        mColumnToChildIndex = new SparseIntArray();
        SparseIntArray columnToChild = mColumnToChildIndex;

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);
            LayoutParams params = cellParams(child);

            if (params.column >= virtualCount) virtualCount = params.column;

            for (int span = 0; span < params.span; span++)
            {
                columnToChild.put(virtualCount++, index);
            }
        }

        mNumColumns = virtualCount;
    }

    private LayoutParams cellParams(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = params == null ? new LayoutParams() : new LayoutParams(params);
        child.setLayoutParams(converted);
        return converted;
    }

    @Override
    int measureNullChild(int childIndex)
    {
        if (mConstrainedColumnWidths == null || childIndex >= mConstrainedColumnWidths.length) return 0;

        return mConstrainedColumnWidths[childIndex];
    }

    @Override
    void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec,
                                  int totalHeight)
    {
        if (mConstrainedColumnWidths == null)
        {
            super.measureChildBeforeLayout(child, childIndex, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
            return;
        }

        LayoutParams params = cellParams(child);

        int measureMode = MeasureSpec.EXACTLY;
        int columnWidth = 0;

        int span = params.span;
        int[] constrainedColumnWidths = mConstrainedColumnWidths;

        for (int index = 0; index < span; index++)
        {
            columnWidth += constrainedColumnWidths[childIndex + index];
        }

        int gravity = params.gravity;
        boolean isHorizontalGravity = gravity > 0 && (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != 0;

        if (isHorizontalGravity) measureMode = MeasureSpec.AT_MOST;

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, columnWidth - params.leftMargin - params.rightMargin),
                                                                measureMode);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + params.topMargin
                                                         + params.bottomMargin + totalHeight, params.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        if (!isHorizontalGravity)
        {
            params.mOffset[LayoutParams.LOCATION] = 0;
            params.mOffset[LayoutParams.LOCATION_NEXT] = 0;
            return;
        }

        int childWidth = child.getMeasuredWidth();
        params.mOffset[LayoutParams.LOCATION_NEXT] = columnWidth - childWidth;

        int absoluteGravity = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());
        int horizontal = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;

        if (horizontal == Gravity.RIGHT) params.mOffset[LayoutParams.LOCATION] = params.mOffset[LayoutParams.LOCATION_NEXT];
        else if (horizontal == Gravity.CENTER_HORIZONTAL) params.mOffset[LayoutParams.LOCATION] = params.mOffset[LayoutParams.LOCATION_NEXT] / 2;
        else params.mOffset[LayoutParams.LOCATION] = 0;
    }

    @Override
    int getChildrenSkipCount(View child, int index)
    {
        return cellParams(child).span - 1;
    }

    @Override
    int getLocationOffset(View child)
    {
        return cellParams(child).mOffset[LayoutParams.LOCATION];
    }

    @Override
    int getNextLocationOffset(View child)
    {
        return cellParams(child).mOffset[LayoutParams.LOCATION_NEXT];
    }

    int[] getColumnsWidths(int widthMeasureSpec, int heightMeasureSpec)
    {
        int numColumns = getVirtualChildCount();
        if (mColumnWidths == null || numColumns != mColumnWidths.length) mColumnWidths = new int[numColumns];

        int[] columnWidths = mColumnWidths;

        for (int index = 0; index < numColumns; index++)
        {
            View child = getVirtualChildAt(index);

            if (child == null || child.getVisibility() == GONE)
            {
                columnWidths[index] = 0;
                continue;
            }

            LayoutParams params = cellParams(child);

            if (params.span != 1)
            {
                columnWidths[index] = 0;
                continue;
            }

            int spec;

            if (params.width == LayoutParams.WRAP_CONTENT)
            {
                spec = getChildMeasureSpec(widthMeasureSpec, 0, LayoutParams.WRAP_CONTENT);
            }
            else if (params.width == LayoutParams.MATCH_PARENT)
            {
                spec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.UNSPECIFIED);
            }
            else
            {
                spec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
            }

            child.measure(spec, spec);

            columnWidths[index] = child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
        }

        return columnWidths;
    }

    void setColumnsWidthConstraints(int[] columnWidths)
    {
        if (columnWidths == null || columnWidths.length < getVirtualChildCount())
        {
            throw new IllegalArgumentException("columnWidths should be >= getVirtualChildCount()");
        }

        mConstrainedColumnWidths = columnWidths;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new TableRow.LayoutParams(getContext(), attributes);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof TableRow.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        return new LayoutParams(source);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TableRow.class.getName();
    }

    public static class LayoutParams extends LinearLayout.LayoutParams
    {
        private static final int LOCATION = 0;
        private static final int LOCATION_NEXT = 1;

        private static StyleableSet sStyleable;

        public int column = -1;
        public int span = 1;

        private final int[] mOffset = new int[2];

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);

            if (attributes == null)
            {
                width = MATCH_PARENT;
                return;
            }

            if (sStyleable == null)
            {
                sStyleable = StyleableSet.of(context.getResources(), "layout_width", "layout_height", "layout_column", "layout_span");
            }
            if (sStyleable.isEmpty()) return;

            TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

            int widthIndex = sStyleable.indexOf("layout_width");
            int heightIndex = sStyleable.indexOf("layout_height");
            int columnIndex = sStyleable.indexOf("layout_column");
            int spanIndex = sStyleable.indexOf("layout_span");

            if (widthIndex >= 0 && !array.hasValue(widthIndex)) width = MATCH_PARENT;
            if (heightIndex >= 0 && !array.hasValue(heightIndex)) height = WRAP_CONTENT;
            if (columnIndex >= 0) column = array.getInt(columnIndex, -1);
            if (spanIndex >= 0) span = array.getInt(spanIndex, 1);
            if (span <= 1) span = 1;

            array.recycle();
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, float initWeight)
        {
            super(width, height, initWeight);
        }

        public LayoutParams()
        {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(int column)
        {
            this();
            this.column = column;
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source)
        {
            super(source);
        }

        @Override
        protected void setBaseAttributes(TypedArray array, int widthAttr, int heightAttr)
        {
            width = array.hasValue(widthAttr) ? array.getLayoutDimension(widthAttr, "layout_width") : MATCH_PARENT;
            height = array.hasValue(heightAttr) ? array.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }
    }

    private class ChildrenTracker implements OnHierarchyChangeListener
    {
        private OnHierarchyChangeListener mListener;

        private void setOnHierarchyChangeListener(OnHierarchyChangeListener listener)
        {
            mListener = listener;
        }

        @Override
        public void onChildViewAdded(View parent, View child)
        {
            mColumnToChildIndex = null;

            if (mListener != null) mListener.onChildViewAdded(parent, child);
        }

        @Override
        public void onChildViewRemoved(View parent, View child)
        {
            mColumnToChildIndex = null;

            if (mListener != null) mListener.onChildViewRemoved(parent, child);
        }
    }
}

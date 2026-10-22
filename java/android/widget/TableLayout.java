package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class TableLayout extends LinearLayout
{
    private static StyleableSet sStyleable;

    private int[] mMaxWidths;
    private SparseBooleanArray mCollapsedColumns;
    private SparseBooleanArray mShrinkableColumns;
    private SparseBooleanArray mStretchableColumns;
    private boolean mShrinkAllColumns;
    private boolean mStretchAllColumns;
    private PassThroughHierarchyChangeListener mPassThroughListener;
    private boolean mInitialized;

    public TableLayout(Context context)
    {
        super(context);
        initTableLayout();
    }

    public TableLayout(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        if (attributes != null)
        {
            if (sStyleable == null)
            {
                sStyleable = StyleableSet.of(context.getResources(), "stretchColumns", "shrinkColumns", "collapseColumns");
            }

            if (!sStyleable.isEmpty())
            {
                TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

                String stretched = readString(array, sStyleable.indexOf("stretchColumns"));
                if (stretched != null && !stretched.isEmpty())
                {
                    if (stretched.charAt(0) == '*') mStretchAllColumns = true;
                    else mStretchableColumns = parseColumns(stretched);
                }

                String shrunk = readString(array, sStyleable.indexOf("shrinkColumns"));
                if (shrunk != null && !shrunk.isEmpty())
                {
                    if (shrunk.charAt(0) == '*') mShrinkAllColumns = true;
                    else mShrinkableColumns = parseColumns(shrunk);
                }

                String collapsed = readString(array, sStyleable.indexOf("collapseColumns"));
                if (collapsed != null && !collapsed.isEmpty()) mCollapsedColumns = parseColumns(collapsed);

                array.recycle();
            }
        }

        initTableLayout();
    }

    private static String readString(TypedArray array, int index)
    {
        if (index < 0) return null;

        CharSequence text = array.getText(index);
        if (text != null) return text.toString();

        if (array.hasValue(index)) return String.valueOf(array.getInt(index, -1));

        return null;
    }

    private static SparseBooleanArray parseColumns(String sequence)
    {
        SparseBooleanArray columns = new SparseBooleanArray();
        String[] columnDefinitions = sequence.split("\\s*,\\s*");

        for (String columnIdentifier : columnDefinitions)
        {
            try
            {
                int columnIndex = Integer.parseInt(columnIdentifier.trim());
                if (columnIndex >= 0) columns.put(columnIndex, true);
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        return columns;
    }

    private void initTableLayout()
    {
        if (mCollapsedColumns == null) mCollapsedColumns = new SparseBooleanArray();
        if (mStretchableColumns == null) mStretchableColumns = new SparseBooleanArray();
        if (mShrinkableColumns == null) mShrinkableColumns = new SparseBooleanArray();

        setOrientation(LinearLayout.VERTICAL);

        mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(mPassThroughListener);

        mInitialized = true;
    }

    @Override
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener)
    {
        mPassThroughListener.mOnHierarchyChangeListener = listener;
    }

    private void requestRowsLayout()
    {
        if (!mInitialized) return;

        int count = getChildCount();
        for (int index = 0; index < count; index++)
        {
            getChildAt(index).requestLayout();
        }
    }

    @Override
    public void requestLayout()
    {
        if (mInitialized)
        {
            int count = getChildCount();
            for (int index = 0; index < count; index++)
            {
                getChildAt(index).forceLayout();
            }
        }

        super.requestLayout();
    }

    public boolean isShrinkAllColumns()
    {
        return mShrinkAllColumns;
    }

    public void setShrinkAllColumns(boolean shrinkAllColumns)
    {
        mShrinkAllColumns = shrinkAllColumns;
    }

    public boolean isStretchAllColumns()
    {
        return mStretchAllColumns;
    }

    public void setStretchAllColumns(boolean stretchAllColumns)
    {
        mStretchAllColumns = stretchAllColumns;
    }

    public void setColumnCollapsed(int columnIndex, boolean isCollapsed)
    {
        mCollapsedColumns.put(columnIndex, isCollapsed);

        int count = getChildCount();
        for (int index = 0; index < count; index++)
        {
            View view = getChildAt(index);
            if (view instanceof TableRow) ((TableRow)view).setColumnCollapsed(columnIndex, isCollapsed);
        }

        requestRowsLayout();
    }

    public boolean isColumnCollapsed(int columnIndex)
    {
        return mCollapsedColumns.get(columnIndex);
    }

    public void setColumnStretchable(int columnIndex, boolean isStretchable)
    {
        mStretchableColumns.put(columnIndex, isStretchable);
        requestRowsLayout();
    }

    public boolean isColumnStretchable(int columnIndex)
    {
        return mStretchAllColumns || mStretchableColumns.get(columnIndex);
    }

    public void setColumnShrinkable(int columnIndex, boolean isShrinkable)
    {
        mShrinkableColumns.put(columnIndex, isShrinkable);
        requestRowsLayout();
    }

    public boolean isColumnShrinkable(int columnIndex)
    {
        return mShrinkAllColumns || mShrinkableColumns.get(columnIndex);
    }

    private void trackCollapsedColumns(View child)
    {
        if (!(child instanceof TableRow)) return;

        TableRow row = (TableRow)child;
        SparseBooleanArray collapsedColumns = mCollapsedColumns;
        int count = collapsedColumns.size();

        for (int index = 0; index < count; index++)
        {
            int columnIndex = collapsedColumns.keyAt(index);
            boolean isCollapsed = collapsedColumns.valueAt(index);

            if (isCollapsed) row.setColumnCollapsed(columnIndex, true);
        }
    }

    @Override
    public void addView(View child)
    {
        super.addView(child);
        requestRowsLayout();
    }

    @Override
    public void addView(View child, int index)
    {
        super.addView(child, index);
        requestRowsLayout();
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params)
    {
        super.addView(child, params);
        requestRowsLayout();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        super.addView(child, index, params);
        requestRowsLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        layoutVertical(left, top, right, bottom);
    }

    @Override
    void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec,
                                  int totalHeight)
    {
        if (child instanceof TableRow) ((TableRow)child).setColumnsWidthConstraints(mMaxWidths);

        super.measureChildBeforeLayout(child, childIndex, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    @Override
    void measureVertical(int widthMeasureSpec, int heightMeasureSpec)
    {
        findLargestCells(widthMeasureSpec, heightMeasureSpec);
        shrinkAndStretchColumns(widthMeasureSpec);

        super.measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    private void findLargestCells(int widthMeasureSpec, int heightMeasureSpec)
    {
        boolean firstRow = true;
        int count = getChildCount();

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == GONE) continue;
            if (!(child instanceof TableRow)) continue;

            TableRow row = (TableRow)child;
            ViewGroup.LayoutParams params = row.getLayoutParams();
            if (params != null) params.height = LayoutParams.WRAP_CONTENT;

            int[] widths = row.getColumnsWidths(widthMeasureSpec, heightMeasureSpec);
            int newColumnCount = widths.length;

            if (firstRow)
            {
                if (mMaxWidths == null || mMaxWidths.length != newColumnCount) mMaxWidths = new int[newColumnCount];

                System.arraycopy(widths, 0, mMaxWidths, 0, newColumnCount);
                firstRow = false;
                continue;
            }

            int length = mMaxWidths.length;
            int difference = newColumnCount - length;

            if (difference > 0)
            {
                int[] oldMaxWidths = mMaxWidths;
                mMaxWidths = new int[newColumnCount];
                System.arraycopy(oldMaxWidths, 0, mMaxWidths, 0, oldMaxWidths.length);
                System.arraycopy(widths, oldMaxWidths.length, mMaxWidths, oldMaxWidths.length, difference);
            }

            int[] maxWidths = mMaxWidths;
            length = Math.min(length, newColumnCount);

            for (int column = 0; column < length; column++)
            {
                maxWidths[column] = Math.max(maxWidths[column], widths[column]);
            }
        }
    }

    private void shrinkAndStretchColumns(int widthMeasureSpec)
    {
        if (mMaxWidths == null) return;

        int totalWidth = 0;
        for (int width : mMaxWidths)
        {
            totalWidth += width;
        }

        int size = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();

        if (totalWidth > size && (mShrinkAllColumns || mShrinkableColumns.size() > 0))
        {
            mutateColumnsWidth(mShrinkableColumns, mShrinkAllColumns, size, totalWidth);
        }
        else if (totalWidth < size && (mStretchAllColumns || mStretchableColumns.size() > 0))
        {
            mutateColumnsWidth(mStretchableColumns, mStretchAllColumns, size, totalWidth);
        }
    }

    private void mutateColumnsWidth(SparseBooleanArray columns, boolean allColumns, int size, int totalWidth)
    {
        int skipped = 0;
        int[] maxWidths = mMaxWidths;
        int length = maxWidths.length;
        int count = allColumns ? length : columns.size();
        if (count == 0) return;

        int totalExtraSpace = size - totalWidth;
        int extraSpace = totalExtraSpace / count;

        int children = getChildCount();
        for (int index = 0; index < children; index++)
        {
            View child = getChildAt(index);
            if (child instanceof TableRow) child.forceLayout();
        }

        if (allColumns)
        {
            for (int index = 0; index < count; index++)
            {
                maxWidths[index] += extraSpace;
            }

            return;
        }

        for (int index = 0; index < count; index++)
        {
            int column = columns.keyAt(index);

            if (columns.valueAt(index))
            {
                if (column < length) maxWidths[column] += extraSpace;
                else skipped++;
            }
        }

        if (skipped > 0 && skipped < count)
        {
            extraSpace = skipped * extraSpace / (count - skipped);

            for (int index = 0; index < count; index++)
            {
                int column = columns.keyAt(index);

                if (columns.valueAt(index) && column < length)
                {
                    if (extraSpace > maxWidths[column]) maxWidths[column] = 0;
                    else maxWidths[column] += extraSpace;
                }
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new TableLayout.LayoutParams(getContext(), attributes);
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof TableLayout.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);
        return new LayoutParams(source);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TableLayout.class.getName();
    }

    public static class LayoutParams extends LinearLayout.LayoutParams
    {
        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
            width = MATCH_PARENT;
        }

        public LayoutParams(int width, int height)
        {
            super(MATCH_PARENT, height);
        }

        public LayoutParams(int width, int height, float initWeight)
        {
            super(MATCH_PARENT, height, initWeight);
        }

        public LayoutParams()
        {
            super(MATCH_PARENT, WRAP_CONTENT);
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
            width = MATCH_PARENT;
        }

        public LayoutParams(MarginLayoutParams source)
        {
            super(source);
            width = MATCH_PARENT;

            if (source instanceof LinearLayout.LayoutParams)
            {
                weight = ((LinearLayout.LayoutParams)source).weight;
                gravity = ((LinearLayout.LayoutParams)source).gravity;
            }
        }

        @Override
        protected void setBaseAttributes(TypedArray array, int widthAttr, int heightAttr)
        {
            width = MATCH_PARENT;
            height = array.hasValue(heightAttr) ? array.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }
    }

    private class PassThroughHierarchyChangeListener implements OnHierarchyChangeListener
    {
        private OnHierarchyChangeListener mOnHierarchyChangeListener;

        @Override
        public void onChildViewAdded(View parent, View child)
        {
            trackCollapsedColumns(child);

            if (mOnHierarchyChangeListener != null) mOnHierarchyChangeListener.onChildViewAdded(parent, child);
        }

        @Override
        public void onChildViewRemoved(View parent, View child)
        {
            if (mOnHierarchyChangeListener != null) mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
        }
    }
}

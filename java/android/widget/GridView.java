package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

public class GridView extends AbsListView
{
    public static final int NO_STRETCH = 0;
    public static final int STRETCH_SPACING = 1;
    public static final int STRETCH_COLUMN_WIDTH = 2;
    public static final int STRETCH_SPACING_UNIFORM = 3;
    public static final int AUTO_FIT = -1;

    private static StyleableSet sStyleable;

    private int mNumColumns = AUTO_FIT;
    private int mRequestedNumColumns;
    private int mHorizontalSpacing;
    private int mRequestedHorizontalSpacing;
    private int mVerticalSpacing;
    private int mStretchMode = STRETCH_COLUMN_WIDTH;
    private int mColumnWidth;
    private int mRequestedColumnWidth;
    private int mGravity = Gravity.START;

    public GridView(Context context)
    {
        this(context, null);
    }

    public GridView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public GridView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public GridView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mRequestedNumColumns = 1;
        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "horizontalSpacing", "verticalSpacing", "stretchMode", "columnWidth",
                                         "numColumns", "gravity");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int index = sStyleable.indexOf("horizontalSpacing");
        if (index >= 0) setHorizontalSpacing(array.getDimensionPixelOffset(index, 0));

        index = sStyleable.indexOf("verticalSpacing");
        if (index >= 0) setVerticalSpacing(array.getDimensionPixelOffset(index, 0));

        index = sStyleable.indexOf("stretchMode");
        if (index >= 0) setStretchMode(array.getInt(index, STRETCH_COLUMN_WIDTH));

        index = sStyleable.indexOf("columnWidth");
        if (index >= 0)
        {
            int columnWidth = array.getDimensionPixelOffset(index, -1);
            if (columnWidth > 0) setColumnWidth(columnWidth);
        }

        index = sStyleable.indexOf("numColumns");
        if (index >= 0) setNumColumns(array.getInt(index, 1));

        index = sStyleable.indexOf("gravity");
        if (index >= 0)
        {
            int gravity = array.getInt(index, -1);
            if (gravity >= 0) setGravity(gravity);
        }

        array.recycle();
    }

    @Override
    public ListAdapter getAdapter()
    {
        return mAdapter;
    }

    @Override
    public void setRemoteViewsAdapter(Intent intent)
    {
        super.setRemoteViewsAdapter(intent);
    }

    @Override
    public void setAdapter(ListAdapter adapter)
    {
        if (mAdapter != null && mDataSetObserver != null) mAdapter.unregisterDataSetObserver(mDataSetObserver);
        mDataSetObserver = null;

        resetList();
        mRecycler.clear();
        mAdapter = adapter;

        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        super.setAdapter(adapter);

        if (mAdapter != null)
        {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            mDataChanged = true;
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(Math.max(1, mAdapter.getViewTypeCount()));
        }
        else
        {
            mItemCount = 0;
            checkFocus();
        }

        checkSelectionChanged();
        requestLayout();
    }

    @Override
    int columnCount()
    {
        return Math.max(1, mNumColumns);
    }

    @Override
    int rowSpacing()
    {
        return mVerticalSpacing;
    }

    @Override
    void measureItem(View child, int column)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        int childWidthSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, params.width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, params.height);
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    int itemLeft(int column, View child)
    {
        int columns = columnCount();
        int available = getWidth() - mListPadding.left - mListPadding.right;
        int used = columns * mColumnWidth + (columns - 1) * mHorizontalSpacing;
        int start = mListPadding.left + (mStretchMode == STRETCH_SPACING_UNIFORM ? mHorizontalSpacing : 0);

        int gravity = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (gravity == Gravity.CENTER_HORIZONTAL) start = mListPadding.left + (available - used) / 2;
        else if (gravity == Gravity.RIGHT) start = mListPadding.left + available - used;

        int cellLeft = start + column * (mColumnWidth + mHorizontalSpacing);
        int childWidth = child.getMeasuredWidth();
        if (childWidth >= mColumnWidth) return cellLeft;

        if (gravity == Gravity.CENTER_HORIZONTAL) return cellLeft + (mColumnWidth - childWidth) / 2;
        if (gravity == Gravity.RIGHT) return cellLeft + mColumnWidth - childWidth;
        return cellLeft;
    }

    @Override
    public void smoothScrollToPosition(int position)
    {
        super.smoothScrollToPosition(position);
    }

    public void smoothScrollByOffset(int offset)
    {
        if (mItemCount == 0) return;

        int base = offset < 0 ? getFirstVisiblePosition() : getLastVisiblePosition();
        int target = Math.max(0, Math.min(mItemCount - 1, base + offset * columnCount()));
        smoothScrollToPosition(target);
    }

    private boolean determineColumns(int availableSpace)
    {
        int requestedHorizontalSpacing = mRequestedHorizontalSpacing;
        int stretchMode = mStretchMode;
        int requestedColumnWidth = mRequestedColumnWidth;
        boolean didNotInitiallyFit = false;

        if (mRequestedNumColumns == AUTO_FIT)
        {
            int cell = requestedColumnWidth + requestedHorizontalSpacing;
            mNumColumns = requestedColumnWidth > 0 && cell > 0 ? (availableSpace + requestedHorizontalSpacing) / cell : 2;
        }
        else
        {
            mNumColumns = mRequestedNumColumns;
        }

        if (mNumColumns <= 0) mNumColumns = 1;

        if (stretchMode == NO_STRETCH)
        {
            mColumnWidth = requestedColumnWidth;
            mHorizontalSpacing = requestedHorizontalSpacing;
            return false;
        }

        int spaceLeftOver = availableSpace - (mNumColumns * requestedColumnWidth) - ((mNumColumns - 1) * requestedHorizontalSpacing);
        if (spaceLeftOver < 0) didNotInitiallyFit = true;

        if (stretchMode == STRETCH_COLUMN_WIDTH)
        {
            mColumnWidth = requestedColumnWidth + spaceLeftOver / mNumColumns;
            mHorizontalSpacing = requestedHorizontalSpacing;
        }
        else if (stretchMode == STRETCH_SPACING)
        {
            mColumnWidth = requestedColumnWidth;
            if (mNumColumns > 1) mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver / (mNumColumns - 1);
            else mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
        }
        else if (stretchMode == STRETCH_SPACING_UNIFORM)
        {
            mColumnWidth = requestedColumnWidth;
            if (mNumColumns > 1) mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver / (mNumColumns + 1);
            else mHorizontalSpacing = requestedHorizontalSpacing + spaceLeftOver;
        }

        if (mColumnWidth < 0) mColumnWidth = 0;
        return didNotInitiallyFit;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED)
        {
            if (mColumnWidth > 0) widthSize = mColumnWidth + mListPadding.left + mListPadding.right;
            else widthSize = mListPadding.left + mListPadding.right;
            widthSize += getVerticalScrollbarWidth();
        }

        int previousColumns = mNumColumns;
        int childWidth = widthSize - mListPadding.left - mListPadding.right;
        boolean didNotInitiallyFit = determineColumns(childWidth);

        if (previousColumns != mNumColumns && getChildCount() > 0 && mSyncPosition < 0)
        {
            mSyncPosition = mFirstPosition;
            mSpecificTop = getChildAt(0).getTop() - mListPadding.top;
        }

        int childHeight = 0;
        int count = mAdapter == null ? 0 : mAdapter.getCount();
        mItemCount = count;

        if (count > 0 && (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST))
        {
            View child = measuredItemView(0);
            childHeight = child.getMeasuredHeight();
            releaseMeasuredItemView(child, 0);
        }

        if (heightMode == MeasureSpec.UNSPECIFIED)
        {
            heightSize = mListPadding.top + mListPadding.bottom + childHeight;
        }

        if (heightMode == MeasureSpec.AT_MOST)
        {
            int ourSize = mListPadding.top + mListPadding.bottom;
            int columns = columnCount();

            for (int position = 0; position < count; position += columns)
            {
                int rowHeight = 0;
                for (int column = 0; column < columns && position + column < count; column++)
                {
                    View child = measuredItemView(position + column);
                    rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
                    releaseMeasuredItemView(child, position + column);
                }

                ourSize += rowHeight;
                if (position + columns < count) ourSize += mVerticalSpacing;

                if (ourSize >= heightSize)
                {
                    ourSize = heightSize;
                    break;
                }
            }

            heightSize = ourSize;
        }

        if (widthMode == MeasureSpec.AT_MOST && mRequestedNumColumns != AUTO_FIT)
        {
            int ourSize = (mRequestedNumColumns * mColumnWidth) + ((mRequestedNumColumns - 1) * mHorizontalSpacing) + mListPadding.left
                          + mListPadding.right;
            if (ourSize > widthSize || didNotInitiallyFit) widthSize |= MEASURED_STATE_TOO_SMALL;
        }

        setMeasuredDimension(widthSize, heightSize);
        mWidthMeasureSpec = widthMeasureSpec;
    }

    private View measuredItemView(int position)
    {
        int index = position - mFirstPosition;
        View child = index >= 0 && index < getChildCount() ? getChildAt(index) : null;

        if (child == null) child = obtainView(position);

        listParamsFor(child);
        measureItem(child, 0);
        return child;
    }

    private void releaseMeasuredItemView(View child, int position)
    {
        if (child.getParent() == this && indexOfChild(child) >= 0) return;

        mRecycler.addScrapView(child, position);
    }

    protected void attachLayoutAnimationParameters(View child, ViewGroup.LayoutParams params, int index, int count)
    {
    }

    @Override
    protected void layoutChildren()
    {
        layoutRows();
    }

    @Override
    public void setSelection(int position)
    {
        setSelectionFromTop(position, 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event)
    {
        boolean handled = false;
        for (int index = 0; index < Math.max(1, repeatCount); index++) handled |= onKeyDown(keyCode, event);
        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    public void setGravity(int gravity)
    {
        if (mGravity == gravity) return;

        mGravity = gravity;
        requestLayoutIfNecessary();
    }

    public int getGravity()
    {
        return mGravity;
    }

    public void setHorizontalSpacing(int horizontalSpacing)
    {
        if (horizontalSpacing == mRequestedHorizontalSpacing) return;

        mRequestedHorizontalSpacing = horizontalSpacing;
        requestLayoutIfNecessary();
    }

    public int getHorizontalSpacing()
    {
        return mHorizontalSpacing;
    }

    public int getRequestedHorizontalSpacing()
    {
        return mRequestedHorizontalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing)
    {
        if (verticalSpacing == mVerticalSpacing) return;

        mVerticalSpacing = verticalSpacing;
        requestLayoutIfNecessary();
    }

    public int getVerticalSpacing()
    {
        return mVerticalSpacing;
    }

    public void setStretchMode(int stretchMode)
    {
        if (stretchMode == mStretchMode) return;

        mStretchMode = stretchMode;
        requestLayoutIfNecessary();
    }

    public int getStretchMode()
    {
        return mStretchMode;
    }

    public void setColumnWidth(int columnWidth)
    {
        if (columnWidth == mRequestedColumnWidth) return;

        mRequestedColumnWidth = columnWidth;
        requestLayoutIfNecessary();
    }

    public int getColumnWidth()
    {
        return mColumnWidth;
    }

    public int getRequestedColumnWidth()
    {
        return mRequestedColumnWidth;
    }

    public void setNumColumns(int numColumns)
    {
        if (numColumns == mRequestedNumColumns) return;

        mRequestedNumColumns = numColumns;
        requestLayoutIfNecessary();
    }

    public int getNumColumns()
    {
        return mNumColumns;
    }

    @Override
    protected int computeVerticalScrollExtent()
    {
        return super.computeVerticalScrollExtent();
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        return super.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        return super.computeVerticalScrollRange();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return GridView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info)
    {
        super.onInitializeAccessibilityNodeInfoForItem(view, position, info);
    }
}

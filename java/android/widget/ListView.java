package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class ListView extends AbsListView
{
    static final int NO_POSITION = -1;

    private static final float MAX_SCROLL_FACTOR = 0.33f;
    private static final int DEFAULT_SMOOTH_SCROLL_DURATION = 250;

    private static StyleableSet sStyleable;

    private final ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<FixedViewInfo>();
    private final ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<FixedViewInfo>();

    private Drawable mDivider;
    private int mDividerHeight;
    private boolean mHeaderDividersEnabled = true;
    private boolean mFooterDividersEnabled = true;
    private Drawable mOverScrollHeader;
    private Drawable mOverScrollFooter;
    private boolean mItemsCanFocus;
    private boolean mAreAllItemsSelectable = true;
    private final Rect mTempRect = new Rect();

    public ListView(Context context)
    {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public ListView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ListView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "entries", "divider", "dividerHeight", "headerDividersEnabled",
                                         "footerDividersEnabled", "overScrollHeader", "overScrollFooter");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int entriesIndex = sStyleable.indexOf("entries");
        if (entriesIndex >= 0 && array.hasValue(entriesIndex))
        {
            CharSequence[] entries = array.getTextArray(entriesIndex);
            if (entries != null)
            {
                int layout = context.getResources().getIdentifier("simple_list_item_1", "layout", "android");
                setAdapter(new ArrayAdapter<CharSequence>(context, layout, entries));
            }
        }

        int dividerIndex = sStyleable.indexOf("divider");
        if (dividerIndex >= 0 && array.hasValue(dividerIndex))
        {
            Drawable divider = readDrawable(array, dividerIndex);
            setDivider(divider);
        }

        mOverScrollHeader = readDrawable(array, sStyleable.indexOf("overScrollHeader"));
        mOverScrollFooter = readDrawable(array, sStyleable.indexOf("overScrollFooter"));

        int heightIndex = sStyleable.indexOf("dividerHeight");
        if (heightIndex >= 0 && array.hasValue(heightIndex))
        {
            int height = array.getDimensionPixelSize(heightIndex, 0);
            if (height != 0) setDividerHeight(height);
        }

        int headerIndex = sStyleable.indexOf("headerDividersEnabled");
        if (headerIndex >= 0) mHeaderDividersEnabled = array.getBoolean(headerIndex, true);

        int footerIndex = sStyleable.indexOf("footerDividersEnabled");
        if (footerIndex >= 0) mFooterDividersEnabled = array.getBoolean(footerIndex, true);

        array.recycle();
    }

    private static Drawable readDrawable(TypedArray array, int index)
    {
        if (index < 0 || !array.hasValue(index)) return null;

        try
        {
            return array.getDrawable(index);
        }
        catch (RuntimeException unsupported)
        {
            return null;
        }
    }

    public int getMaxScrollAmount()
    {
        return (int)(MAX_SCROLL_FACTOR * getHeight());
    }

    public void addHeaderView(View view, Object data, boolean isSelectable)
    {
        if (view.getParent() != null && view.getParent() != this)
        {
            android.util.Log.w("ListView", "The specified child already has a parent. "
                                           + "You must call removeView() on the child's parent first.");
        }

        FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);
        mAreAllItemsSelectable &= isSelectable;

        onFixedViewsChanged();
    }

    public void addHeaderView(View view)
    {
        addHeaderView(view, null, true);
    }

    public int getHeaderViewsCount()
    {
        return mHeaderViewInfos.size();
    }

    public boolean removeHeaderView(View view)
    {
        if (mHeaderViewInfos.isEmpty()) return false;

        boolean result = false;
        if (mAdapter instanceof HeaderViewListAdapter && ((HeaderViewListAdapter)mAdapter).removeHeader(view))
        {
            if (mDataSetObserver != null) mDataSetObserver.onChanged();
            result = true;
        }

        removeFixedViewInfo(view, mHeaderViewInfos);
        return result;
    }

    public void addFooterView(View view, Object data, boolean isSelectable)
    {
        if (view.getParent() != null && view.getParent() != this)
        {
            android.util.Log.w("ListView", "The specified child already has a parent. "
                                           + "You must call removeView() on the child's parent first.");
        }

        FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);
        mAreAllItemsSelectable &= isSelectable;

        onFixedViewsChanged();
    }

    public void addFooterView(View view)
    {
        addFooterView(view, null, true);
    }

    public int getFooterViewsCount()
    {
        return mFooterViewInfos.size();
    }

    public boolean removeFooterView(View view)
    {
        if (mFooterViewInfos.isEmpty()) return false;

        boolean result = false;
        if (mAdapter instanceof HeaderViewListAdapter && ((HeaderViewListAdapter)mAdapter).removeFooter(view))
        {
            if (mDataSetObserver != null) mDataSetObserver.onChanged();
            result = true;
        }

        removeFixedViewInfo(view, mFooterViewInfos);
        return result;
    }

    private void onFixedViewsChanged()
    {
        if (mAdapter == null) return;

        if (!(mAdapter instanceof HeaderViewListAdapter)) wrapHeaderListAdapterInternal();
        if (mDataSetObserver != null) mDataSetObserver.onChanged();
    }

    private void wrapHeaderListAdapterInternal()
    {
        ListAdapter wrapped = mAdapter;
        if (wrapped != null && mDataSetObserver != null) wrapped.unregisterDataSetObserver(mDataSetObserver);

        mAdapter = new HeaderViewListAdapter(mHeaderViewInfos, mFooterViewInfos, wrapped);
        if (mDataSetObserver != null) mAdapter.registerDataSetObserver(mDataSetObserver);

        mRecycler.setViewTypeCount(Math.max(1, mAdapter.getViewTypeCount()));
        resetChildrenForNewAdapter();
    }

    private void resetChildrenForNewAdapter()
    {
        removeAllViewsInLayout();
        mDataChanged = true;
        mOldItemCount = mItemCount;
        mItemCount = mAdapter == null ? 0 : mAdapter.getCount();
    }

    private void removeFixedViewInfo(View view, ArrayList<FixedViewInfo> where)
    {
        for (int index = 0; index < where.size(); index++)
        {
            if (where.get(index).view != view) continue;

            where.remove(index);
            break;
        }

        if (view.getParent() == this)
        {
            if (indexOfChild(view) >= 0) removeViewInLayout(view);
            else removeDetachedView(view, false);
        }
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

        boolean hasFixedViews = !mHeaderViewInfos.isEmpty() || !mFooterViewInfos.isEmpty();
        mAdapter = hasFixedViews ? new HeaderViewListAdapter(mHeaderViewInfos, mFooterViewInfos, adapter) : adapter;

        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        super.setAdapter(adapter);

        if (mAdapter != null)
        {
            mAreAllItemsSelectable = mAdapter.areAllItemsEnabled();
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(Math.max(1, mAdapter.getViewTypeCount()));
        }
        else
        {
            mAreAllItemsSelectable = true;
            mItemCount = 0;
            checkFocus();
        }

        checkSelectionChanged();
        requestLayout();
    }

    @Override
    void resetList()
    {
        super.resetList();
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate)
    {
        int listTop = mListPadding.top;
        int listBottom = getHeight() - mListPadding.bottom;

        mTempRect.set(rect);
        mTempRect.offset(child.getLeft(), child.getTop());

        int delta = 0;
        if (mTempRect.bottom > listBottom) delta = Math.min(mTempRect.bottom - listBottom, mTempRect.top - listTop);
        else if (mTempRect.top < listTop) delta = mTempRect.top - listTop;

        if (delta == 0) return false;

        if (immediate) trackMotionScroll(-delta);
        else smoothScrollBy(delta, DEFAULT_SMOOTH_SCROLL_DURATION);

        return true;
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
        int target = Math.max(0, Math.min(mItemCount - 1, base + offset));
        smoothScrollToPosition(target);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int childWidth = 0;
        int childHeight = 0;

        int count = mAdapter == null ? 0 : mAdapter.getCount();
        if (count > 0 && (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED))
        {
            View child = measuredItemView(0);
            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();
            releaseMeasuredItemView(child, 0);
        }

        if (widthMode == MeasureSpec.UNSPECIFIED)
        {
            widthSize = mListPadding.left + mListPadding.right + childWidth + getVerticalScrollbarWidth();
        }

        if (heightMode == MeasureSpec.UNSPECIFIED)
        {
            heightSize = mListPadding.top + mListPadding.bottom + childHeight;
        }

        if (heightMode == MeasureSpec.AT_MOST)
        {
            heightSize = measureHeightOfChildren(widthMeasureSpec, 0, NO_POSITION, heightSize, -1);
        }

        setMeasuredDimension(Math.max(widthSize, getSuggestedMinimumWidth()), Math.max(heightSize, getSuggestedMinimumHeight()));
        mWidthMeasureSpec = widthMeasureSpec;
    }

    private View measuredItemView(int position)
    {
        int index = position - mFirstPosition;
        View child = index >= 0 && index < getChildCount() ? getChildAt(index) : null;

        if (child == null) child = obtainView(position);

        measureItem(child, 0);
        return child;
    }

    private void releaseMeasuredItemView(View child, int position)
    {
        if (child.getParent() == this && indexOfChild(child) >= 0) return;

        mRecycler.addScrapView(child, position);
    }

    final int measureHeightOfChildren(int widthMeasureSpec, int startPosition, int endPosition, int maxHeight,
                                      int disallowPartialChildPosition)
    {
        ListAdapter adapter = mAdapter;
        updateListPadding();
        if (adapter == null) return mListPadding.top + mListPadding.bottom;

        mWidthMeasureSpec = widthMeasureSpec;
        int returnedHeight = mListPadding.top + mListPadding.bottom;
        int dividerHeight = dividerSpacing();
        int prevHeightWithoutPartialChild = 0;

        int end = endPosition == NO_POSITION ? adapter.getCount() - 1 : endPosition;

        for (int position = startPosition; position <= end; position++)
        {
            View child = measuredItemView(position);
            if (position > 0) returnedHeight += dividerHeight;

            returnedHeight += child.getMeasuredHeight();
            releaseMeasuredItemView(child, position);

            if (returnedHeight >= maxHeight)
            {
                return disallowPartialChildPosition >= 0 && position > disallowPartialChildPosition && prevHeightWithoutPartialChild > 0
                       && returnedHeight != maxHeight ? prevHeightWithoutPartialChild : maxHeight;
            }

            boolean fullyCounted = disallowPartialChildPosition >= 0 && position >= disallowPartialChildPosition;
            if (fullyCounted) prevHeightWithoutPartialChild = returnedHeight;
        }

        return returnedHeight;
    }

    private int dividerSpacing()
    {
        return mDivider != null && mDividerHeight > 0 ? mDividerHeight : 0;
    }

    @Override
    int rowSpacing()
    {
        return dividerSpacing();
    }

    @Override
    protected void layoutChildren()
    {
        layoutRows();
    }

    @Override
    protected boolean canAnimate()
    {
        return super.canAnimate() && mItemCount > 0;
    }

    @Override
    public void setSelection(int position)
    {
        setSelectionFromTop(position, 0);
    }

    public void setSelectionAfterHeaderView()
    {
        int count = getHeaderViewsCount();
        if (count > 0)
        {
            setSelectionFromTop(count, 0);
            return;
        }

        if (mAdapter != null) setSelection(count);
        else mResurrectToPosition = count;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        return super.dispatchKeyEvent(event);
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

    public void setItemsCanFocus(boolean itemsCanFocus)
    {
        mItemsCanFocus = itemsCanFocus;
        if (!itemsCanFocus) setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    public boolean getItemsCanFocus()
    {
        return mItemsCanFocus;
    }

    public boolean isOpaque()
    {
        return (getCacheColorHint() >>> 24) == 0xFF && getChildCount() > 0;
    }

    @Override
    public void setCacheColorHint(int color)
    {
        super.setCacheColorHint(color);
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);

        int dividerHeight = dividerSpacing();
        if (dividerHeight <= 0 || mDivider == null) return;

        int childCount = getChildCount();
        int headerCount = getHeaderViewsCount();
        int footerLimit = mItemCount - mFooterViewInfos.size();
        int left = mListPadding.left;
        int right = getWidth() - mListPadding.right;

        int saveCount = canvas.save();
        if (getClipToPadding())
        {
            canvas.clipRect(mListPadding.left, mListPadding.top, getWidth() - mListPadding.right, getHeight() - mListPadding.bottom);
        }

        for (int index = 0; index < childCount; index++)
        {
            int position = mFirstPosition + index;
            if (position >= mItemCount - 1) break;

            boolean isHeader = position < headerCount;
            boolean nextIsFooter = position + 1 >= footerLimit;
            if (!mHeaderDividersEnabled && isHeader) continue;
            if (!mFooterDividersEnabled && (nextIsFooter || position >= footerLimit)) continue;
            boolean bothEnabled = mAdapter != null && mAdapter.isEnabled(position) && mAdapter.isEnabled(position + 1);
            if (!mAreAllItemsSelectable && !bothEnabled) continue;

            int bottom = getChildAt(index).getBottom();
            mTempRect.set(left, bottom, right, bottom + dividerHeight);
            mDivider.setBounds(mTempRect);
            mDivider.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime)
    {
        return super.drawChild(canvas, child, drawingTime);
    }

    public Drawable getDivider()
    {
        return mDivider;
    }

    public void setDivider(Drawable divider)
    {
        mDividerHeight = divider != null ? Math.max(0, divider.getIntrinsicHeight()) : 0;
        if (divider != null && mDividerHeight == 0) mDividerHeight = 1;

        mDivider = divider;
        requestLayout();
        invalidate();
    }

    public int getDividerHeight()
    {
        return mDividerHeight;
    }

    public void setDividerHeight(int height)
    {
        mDividerHeight = height;
        requestLayout();
        invalidate();
    }

    public void setHeaderDividersEnabled(boolean headerDividersEnabled)
    {
        mHeaderDividersEnabled = headerDividersEnabled;
        invalidate();
    }

    public boolean areHeaderDividersEnabled()
    {
        return mHeaderDividersEnabled;
    }

    public void setFooterDividersEnabled(boolean footerDividersEnabled)
    {
        mFooterDividersEnabled = footerDividersEnabled;
        invalidate();
    }

    public boolean areFooterDividersEnabled()
    {
        return mFooterDividersEnabled;
    }

    public void setOverscrollHeader(Drawable header)
    {
        mOverScrollHeader = header;
    }

    public Drawable getOverscrollHeader()
    {
        return mOverScrollHeader;
    }

    public void setOverscrollFooter(Drawable footer)
    {
        mOverScrollFooter = footer;
    }

    public Drawable getOverscrollFooter()
    {
        return mOverScrollFooter;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        int count = getChildCount();
        if (count == 0) return;

        View[] children = new View[count];
        for (int index = 0; index < count; index++) children[index] = getChildAt(index);

        removeAllViewsInLayout();
        for (View child : children) addHeaderView(child);
    }

    @Deprecated
    public long[] getCheckItemIds()
    {
        if (mAdapter != null && mAdapter.hasStableIds()) return getCheckedItemIds();

        if (mChoiceMode == CHOICE_MODE_NONE || mCheckStates == null || mAdapter == null) return new long[0];

        long[] ids = new long[mCheckStates.size()];
        int found = 0;
        for (int index = 0; index < mCheckStates.size(); index++)
        {
            if (mCheckStates.valueAt(index)) ids[found++] = mAdapter.getItemId(mCheckStates.keyAt(index));
        }

        if (found == ids.length) return ids;

        long[] trimmed = new long[found];
        System.arraycopy(ids, 0, trimmed, 0, found);
        return trimmed;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ListView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info)
    {
        super.onInitializeAccessibilityNodeInfoForItem(view, position, info);
    }

    public class FixedViewInfo
    {
        public View view;
        public Object data;
        public boolean isSelectable;

        public FixedViewInfo()
        {
        }
    }
}

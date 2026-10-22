package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.SoundEffectConstants;
import android.view.StyleableSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

public class ExpandableListView extends ListView
{
    public static final int PACKED_POSITION_TYPE_GROUP = 0;
    public static final int PACKED_POSITION_TYPE_CHILD = 1;
    public static final int PACKED_POSITION_TYPE_NULL = 2;
    public static final long PACKED_POSITION_VALUE_NULL = 0x00000000FFFFFFFFL;
    public static final int CHILD_INDICATOR_INHERIT = -1;

    private static final long PACKED_POSITION_MASK_CHILD = 0x00000000FFFFFFFFL;
    private static final long PACKED_POSITION_MASK_GROUP = 0x7FFFFFFF00000000L;
    private static final long PACKED_POSITION_MASK_TYPE = 0x8000000000000000L;
    private static final long PACKED_POSITION_SHIFT_GROUP = 32;
    private static final long PACKED_POSITION_SHIFT_TYPE = 63;
    private static final long PACKED_POSITION_INT_MASK = 0xFFFFFFFFL;

    private static final int STATE_EXPANDED = 0x010100a8;
    private static final int STATE_EMPTY = 0x010100a9;
    private static final int STATE_LAST = 0x010100a6;
    private static final int[] EMPTY_STATE_SET = {};
    private static final int[] GROUP_EXPANDED_STATE_SET = {STATE_EXPANDED};
    private static final int[] GROUP_EMPTY_STATE_SET = {STATE_EMPTY};
    private static final int[] GROUP_EXPANDED_EMPTY_STATE_SET = {STATE_EXPANDED, STATE_EMPTY};
    private static final int[] CHILD_LAST_STATE_SET = {STATE_LAST};

    private static final int INDICATOR_UNDEFINED = -2;

    private static StyleableSet sStyleable;

    private ExpandableListConnector mConnector;
    private ExpandableListAdapter mAdapter;
    private int mIndicatorLeft;
    private int mIndicatorRight;
    private int mIndicatorStart = INDICATOR_UNDEFINED;
    private int mIndicatorEnd = INDICATOR_UNDEFINED;
    private int mChildIndicatorLeft = CHILD_INDICATOR_INHERIT;
    private int mChildIndicatorRight = CHILD_INDICATOR_INHERIT;
    private int mChildIndicatorStart = INDICATOR_UNDEFINED;
    private int mChildIndicatorEnd = INDICATOR_UNDEFINED;
    private Drawable mGroupIndicator;
    private Drawable mChildIndicator;
    private Drawable mChildDivider;
    private OnGroupCollapseListener mOnGroupCollapseListener;
    private OnGroupExpandListener mOnGroupExpandListener;
    private OnGroupClickListener mOnGroupClickListener;
    private OnChildClickListener mOnChildClickListener;
    private int[] mPendingExpandedGroups;
    private long[] mPendingExpandedIds;

    public ExpandableListView(Context context)
    {
        this(context, null);
    }

    public ExpandableListView(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "expandableListViewStyle"));
    }

    public ExpandableListView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ExpandableListView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null || defStyleAttr != 0 || defStyleRes != 0) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "groupIndicator", "childIndicator", "indicatorLeft", "indicatorRight",
                                         "childIndicatorLeft", "childIndicatorRight", "childDivider", "indicatorStart", "indicatorEnd",
                                         "childIndicatorStart", "childIndicatorEnd");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        mGroupIndicator = readDrawable(array, sStyleable.indexOf("groupIndicator"));
        mChildIndicator = readDrawable(array, sStyleable.indexOf("childIndicator"));
        mChildDivider = readDrawable(array, sStyleable.indexOf("childDivider"));
        mIndicatorLeft = readPixels(array, "indicatorLeft", 0);
        mIndicatorRight = readPixels(array, "indicatorRight", 0);
        mChildIndicatorLeft = readPixels(array, "childIndicatorLeft", CHILD_INDICATOR_INHERIT);
        mChildIndicatorRight = readPixels(array, "childIndicatorRight", CHILD_INDICATOR_INHERIT);
        mIndicatorStart = readPixels(array, "indicatorStart", INDICATOR_UNDEFINED);
        mIndicatorEnd = readPixels(array, "indicatorEnd", INDICATOR_UNDEFINED);
        mChildIndicatorStart = readPixels(array, "childIndicatorStart", INDICATOR_UNDEFINED);
        mChildIndicatorEnd = readPixels(array, "childIndicatorEnd", INDICATOR_UNDEFINED);

        array.recycle();

        if (mGroupIndicator != null) mGroupIndicator.setCallback(this);
        if (mChildIndicator != null) mChildIndicator.setCallback(this);
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

    private static int readPixels(TypedArray array, String name, int fallback)
    {
        int index = sStyleable.indexOf(name);
        if (index < 0 || !array.hasValue(index)) return fallback;

        return array.getDimensionPixelSize(index, fallback);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);

        invalidate();
    }

    private int indicatorLeft(boolean child)
    {
        boolean rtl = isLayoutRtl();
        int start = child ? mChildIndicatorStart : mIndicatorStart;
        int end = child ? mChildIndicatorEnd : mIndicatorEnd;
        int left = child ? mChildIndicatorLeft : mIndicatorLeft;

        if (child && left == CHILD_INDICATOR_INHERIT && start == INDICATOR_UNDEFINED) return indicatorLeft(false);

        if (start != INDICATOR_UNDEFINED || end != INDICATOR_UNDEFINED)
        {
            if (rtl) return getWidth() - (end == INDICATOR_UNDEFINED ? 0 : end);
            return start == INDICATOR_UNDEFINED ? 0 : start;
        }

        return left;
    }

    private int indicatorRight(boolean child)
    {
        boolean rtl = isLayoutRtl();
        int start = child ? mChildIndicatorStart : mIndicatorStart;
        int end = child ? mChildIndicatorEnd : mIndicatorEnd;
        int right = child ? mChildIndicatorRight : mIndicatorRight;

        if (child && right == CHILD_INDICATOR_INHERIT && end == INDICATOR_UNDEFINED) return indicatorRight(false);

        Drawable indicator = child ? mChildIndicator : mGroupIndicator;

        if (start != INDICATOR_UNDEFINED || end != INDICATOR_UNDEFINED)
        {
            if (rtl) return getWidth() - (start == INDICATOR_UNDEFINED ? 0 : start);
            if (end != INDICATOR_UNDEFINED) return end;
        }
        else if (right != 0)
        {
            return right;
        }

        int base = indicatorLeft(child);
        int width = indicator == null ? 0 : Math.max(0, indicator.getIntrinsicWidth());
        return base + width;
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);

        if (mConnector == null) return;
        if (mGroupIndicator == null && mChildIndicator == null && mChildDivider == null) return;

        int headerCount = getHeaderViewsCount();
        int itemCount = mConnector.getCount();
        int childCount = getChildCount();
        int dividerHeight = getDividerHeight();
        int scrollX = getScrollX();

        for (int index = 0; index < childCount; index++)
        {
            int flat = mFirstPosition + index - headerCount;
            if (flat < 0 || flat >= itemCount) continue;

            View item = getChildAt(index);
            if (item.getVisibility() != VISIBLE) continue;

            int group = mConnector.groupAt(flat);
            int child = mConnector.childAt(flat);
            boolean isChild = child >= 0;

            if (isChild && mChildDivider != null && dividerHeight > 0 && flat + 1 < itemCount && mConnector.childAt(flat + 1) >= 0)
            {
                mChildDivider.setBounds(item.getLeft(), item.getBottom(), item.getRight(), item.getBottom() + dividerHeight);
                mChildDivider.draw(canvas);
            }

            Drawable indicator = isChild ? mChildIndicator : mGroupIndicator;
            if (indicator == null) continue;

            int[] state;
            if (isChild)
            {
                state = child == mAdapter.getChildrenCount(group) - 1 ? CHILD_LAST_STATE_SET : EMPTY_STATE_SET;
            }
            else
            {
                boolean expanded = mConnector.isGroupExpanded(group);
                boolean empty = mAdapter.getChildrenCount(group) == 0;
                if (expanded) state = empty ? GROUP_EXPANDED_EMPTY_STATE_SET : GROUP_EXPANDED_STATE_SET;
                else state = empty ? GROUP_EMPTY_STATE_SET : EMPTY_STATE_SET;
            }

            if (indicator.isStateful()) indicator.setState(state);

            int left = indicatorLeft(isChild) + scrollX;
            int right = indicatorRight(isChild) + scrollX;
            if (right <= left) continue;

            int top = item.getTop();
            int bottom = item.getBottom();
            int height = indicator.getIntrinsicHeight();
            if (height > 0 && height < bottom - top)
            {
                top += (bottom - top - height) / 2;
                bottom = top + height;
            }

            indicator.setBounds(left, top, right, bottom);
            indicator.draw(canvas);
        }
    }

    public void setChildDivider(Drawable childDivider)
    {
        mChildDivider = childDivider;
        invalidate();
    }

    @Override
    public void setAdapter(ListAdapter adapter)
    {
        throw new RuntimeException("For ExpandableListView, use setAdapter(ExpandableListAdapter) instead of setAdapter(ListAdapter)");
    }

    @Override
    public ListAdapter getAdapter()
    {
        return super.getAdapter();
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        super.setOnItemClickListener(listener);
    }

    public void setAdapter(ExpandableListAdapter adapter)
    {
        mAdapter = adapter;

        if (adapter == null)
        {
            mConnector = null;
            super.setAdapter((ListAdapter)null);
            return;
        }

        mConnector = new ExpandableListConnector(adapter);

        if (mPendingExpandedGroups != null)
        {
            mConnector.setExpandedGroups(mPendingExpandedGroups, mPendingExpandedIds);
            mPendingExpandedGroups = null;
            mPendingExpandedIds = null;
        }

        super.setAdapter(mConnector);
    }

    public ExpandableListAdapter getExpandableListAdapter()
    {
        return mAdapter;
    }

    private boolean isHeaderOrFooterPosition(int position)
    {
        int footerStart = mItemCount - getFooterViewsCount();
        return position < getHeaderViewsCount() || position >= footerStart;
    }

    private int getFlatPositionForConnector(int flatListPosition)
    {
        return flatListPosition - getHeaderViewsCount();
    }

    private int getAbsoluteFlatPosition(int flatListPosition)
    {
        return flatListPosition + getHeaderViewsCount();
    }

    @Override
    public boolean performItemClick(View view, int position, long id)
    {
        if (mConnector == null || isHeaderOrFooterPosition(position)) return super.performItemClick(view, position, id);

        return handleItemClick(view, getFlatPositionForConnector(position), id);
    }

    boolean handleItemClick(View view, int flatPosition, long id)
    {
        int group = mConnector.groupAt(flatPosition);
        if (group < 0) return false;

        int child = mConnector.childAt(flatPosition);

        if (child < 0)
        {
            long groupId = mAdapter.getGroupId(group);

            if (mOnGroupClickListener != null && mOnGroupClickListener.onGroupClick(this, view, group, groupId))
            {
                return true;
            }

            if (mConnector.isGroupExpanded(group))
            {
                mConnector.collapseGroup(group);
                playSoundEffect(SoundEffectConstants.CLICK);
                if (mOnGroupCollapseListener != null) mOnGroupCollapseListener.onGroupCollapse(group);
            }
            else
            {
                mConnector.expandGroup(group);
                playSoundEffect(SoundEffectConstants.CLICK);
                if (mOnGroupExpandListener != null) mOnGroupExpandListener.onGroupExpand(group);

                int groupFlat = getAbsoluteFlatPosition(mConnector.flatPositionOfGroup(group));
                int lastChildFlat = groupFlat + mAdapter.getChildrenCount(group);
                smoothScrollToPosition(lastChildFlat, groupFlat);
            }

            return true;
        }

        if (mOnChildClickListener == null) return false;

        playSoundEffect(SoundEffectConstants.CLICK);
        return mOnChildClickListener.onChildClick(this, view, group, child, mAdapter.getChildId(group, child));
    }

    public boolean expandGroup(int groupPos)
    {
        return expandGroup(groupPos, false);
    }

    public boolean expandGroup(int groupPos, boolean animate)
    {
        if (mConnector == null) return false;

        boolean expanded = mConnector.expandGroup(groupPos);
        if (mOnGroupExpandListener != null) mOnGroupExpandListener.onGroupExpand(groupPos);

        if (animate && expanded)
        {
            int groupFlat = getAbsoluteFlatPosition(mConnector.flatPositionOfGroup(groupPos));
            smoothScrollToPosition(groupFlat + mAdapter.getChildrenCount(groupPos), groupFlat);
        }

        return expanded;
    }

    public boolean collapseGroup(int groupPos)
    {
        if (mConnector == null) return false;

        boolean collapsed = mConnector.collapseGroup(groupPos);
        if (mOnGroupCollapseListener != null) mOnGroupCollapseListener.onGroupCollapse(groupPos);

        return collapsed;
    }

    public void setOnGroupCollapseListener(OnGroupCollapseListener onGroupCollapseListener)
    {
        mOnGroupCollapseListener = onGroupCollapseListener;
    }

    public void setOnGroupExpandListener(OnGroupExpandListener onGroupExpandListener)
    {
        mOnGroupExpandListener = onGroupExpandListener;
    }

    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener)
    {
        mOnGroupClickListener = onGroupClickListener;
    }

    public void setOnChildClickListener(OnChildClickListener onChildClickListener)
    {
        mOnChildClickListener = onChildClickListener;
    }

    public long getExpandableListPosition(int flatListPosition)
    {
        if (mConnector == null || isHeaderOrFooterPosition(flatListPosition)) return PACKED_POSITION_VALUE_NULL;

        int adjusted = getFlatPositionForConnector(flatListPosition);
        int group = mConnector.groupAt(adjusted);
        if (group < 0) return PACKED_POSITION_VALUE_NULL;

        int child = mConnector.childAt(adjusted);
        return child < 0 ? getPackedPositionForGroup(group) : getPackedPositionForChild(group, child);
    }

    public int getFlatListPosition(long packedPosition)
    {
        if (mConnector == null) return INVALID_POSITION;

        int type = getPackedPositionType(packedPosition);
        if (type == PACKED_POSITION_TYPE_NULL) return INVALID_POSITION;

        int group = getPackedPositionGroup(packedPosition);
        int flat = type == PACKED_POSITION_TYPE_GROUP ? mConnector.flatPositionOfGroup(group)
                                                      : mConnector.flatPositionOfChild(group, getPackedPositionChild(packedPosition));
        return flat < 0 ? INVALID_POSITION : getAbsoluteFlatPosition(flat);
    }

    public long getSelectedPosition()
    {
        int selected = getSelectedItemPosition();
        if (selected == INVALID_POSITION) return PACKED_POSITION_VALUE_NULL;

        return getExpandableListPosition(selected);
    }

    public long getSelectedId()
    {
        long packed = getSelectedPosition();
        if (packed == PACKED_POSITION_VALUE_NULL || mAdapter == null) return -1;

        int group = getPackedPositionGroup(packed);
        if (getPackedPositionType(packed) == PACKED_POSITION_TYPE_GROUP) return mAdapter.getGroupId(group);

        return mAdapter.getChildId(group, getPackedPositionChild(packed));
    }

    public void setSelectedGroup(int groupPosition)
    {
        int flat = getFlatListPosition(getPackedPositionForGroup(groupPosition));
        if (flat != INVALID_POSITION) super.setSelection(flat);
    }

    public boolean setSelectedChild(int groupPosition, int childPosition, boolean shouldExpandGroup)
    {
        long packed = getPackedPositionForChild(groupPosition, childPosition);
        int flat = getFlatListPosition(packed);

        if (flat == INVALID_POSITION)
        {
            if (!shouldExpandGroup) return false;

            expandGroup(groupPosition);
            flat = getFlatListPosition(packed);
            if (flat == INVALID_POSITION) throw new IllegalStateException("Could not find child");
        }

        super.setSelection(flat);
        return true;
    }

    public boolean isGroupExpanded(int groupPosition)
    {
        return mConnector != null && mConnector.isGroupExpanded(groupPosition);
    }

    public static int getPackedPositionType(long packedPosition)
    {
        if (packedPosition == PACKED_POSITION_VALUE_NULL) return PACKED_POSITION_TYPE_NULL;

        return (packedPosition & PACKED_POSITION_MASK_TYPE) == PACKED_POSITION_MASK_TYPE ? PACKED_POSITION_TYPE_CHILD
                                                                                         : PACKED_POSITION_TYPE_GROUP;
    }

    public static int getPackedPositionGroup(long packedPosition)
    {
        if (packedPosition == PACKED_POSITION_VALUE_NULL) return -1;

        return (int)((packedPosition & PACKED_POSITION_MASK_GROUP) >> PACKED_POSITION_SHIFT_GROUP);
    }

    public static int getPackedPositionChild(long packedPosition)
    {
        if (packedPosition == PACKED_POSITION_VALUE_NULL) return -1;
        if ((packedPosition & PACKED_POSITION_MASK_TYPE) != PACKED_POSITION_MASK_TYPE) return -1;

        return (int)(packedPosition & PACKED_POSITION_MASK_CHILD);
    }

    public static long getPackedPositionForChild(int groupPosition, int childPosition)
    {
        return ((long)PACKED_POSITION_TYPE_CHILD << PACKED_POSITION_SHIFT_TYPE)
               | ((groupPosition & PACKED_POSITION_INT_MASK) << PACKED_POSITION_SHIFT_GROUP)
               | (childPosition & PACKED_POSITION_INT_MASK);
    }

    public static long getPackedPositionForGroup(int groupPosition)
    {
        return (groupPosition & PACKED_POSITION_INT_MASK) << PACKED_POSITION_SHIFT_GROUP;
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo()
    {
        ContextMenu.ContextMenuInfo info = super.getContextMenuInfo();
        if (!(info instanceof AdapterContextMenuInfo)) return info;

        AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo)info;
        int position = adapterInfo.position;
        if (mConnector == null || isHeaderOrFooterPosition(position)) return info;

        long packed = getExpandableListPosition(position);
        int group = getPackedPositionGroup(packed);
        long id;

        if (getPackedPositionType(packed) == PACKED_POSITION_TYPE_CHILD) id = mAdapter.getChildId(group, getPackedPositionChild(packed));
        else if (group >= 0) id = mAdapter.getGroupId(group);
        else id = -1;

        return new ExpandableListContextMenuInfo(adapterInfo.targetView, packed, id);
    }

    @Override
    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info)
    {
        super.onInitializeAccessibilityNodeInfoForItem(view, position, info);

        if (mConnector == null || isHeaderOrFooterPosition(position)) return;

        int flat = getFlatPositionForConnector(position);
        if (mConnector.childAt(flat) >= 0) return;

        int group = mConnector.groupAt(flat);
        if (group < 0) return;

        if (mConnector.isGroupExpanded(group)) info.addAction(AccessibilityNodeInfo.ACTION_COLLAPSE);
        else info.addAction(AccessibilityNodeInfo.ACTION_EXPAND);
    }

    public void setChildIndicator(Drawable childIndicator)
    {
        mChildIndicator = childIndicator;
        if (childIndicator != null) childIndicator.setCallback(this);
        invalidate();
    }

    public void setChildIndicatorBounds(int left, int right)
    {
        mChildIndicatorLeft = left;
        mChildIndicatorRight = right;
        invalidate();
    }

    public void setChildIndicatorBoundsRelative(int start, int end)
    {
        mChildIndicatorStart = start;
        mChildIndicatorEnd = end;
        invalidate();
    }

    public void setGroupIndicator(Drawable groupIndicator)
    {
        mGroupIndicator = groupIndicator;
        if (groupIndicator != null) groupIndicator.setCallback(this);
        if (mIndicatorRight == 0 && mGroupIndicator != null) mIndicatorRight = mIndicatorLeft + mGroupIndicator.getIntrinsicWidth();
        invalidate();
    }

    public void setIndicatorBounds(int left, int right)
    {
        mIndicatorLeft = left;
        mIndicatorRight = right;
        invalidate();
    }

    public void setIndicatorBoundsRelative(int start, int end)
    {
        mIndicatorStart = start;
        mIndicatorEnd = end;
        invalidate();
    }

    @Override
    public boolean verifyDrawable(Drawable who)
    {
        return who == mGroupIndicator || who == mChildIndicator || super.verifyDrawable(who);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ExpandableListView.class.getName();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        if (mConnector != null)
        {
            state.expandedGroups = mConnector.getExpandedGroups();
            state.expandedIds = mConnector.getExpandedGroupIds();
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable)
    {
        if (!(parcelable instanceof SavedState))
        {
            super.onRestoreInstanceState(parcelable);
            return;
        }

        SavedState state = (SavedState)parcelable;
        super.onRestoreInstanceState(state.getSuperState());

        if (state.expandedGroups == null) return;

        if (mConnector != null)
        {
            mConnector.setExpandedGroups(state.expandedGroups, state.expandedIds);
        }
        else
        {
            mPendingExpandedGroups = state.expandedGroups;
            mPendingExpandedIds = state.expandedIds;
        }
    }

    static class SavedState extends BaseSavedState
    {
        int[] expandedGroups;
        long[] expandedIds;

        SavedState(Parcelable superState)
        {
            super(superState == null ? android.view.AbsSavedState.EMPTY_STATE : superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            expandedGroups = source.createIntArray();
            expandedIds = source.createLongArray();
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeIntArray(expandedGroups);
            out.writeLongArray(expandedIds);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }

    public interface OnGroupCollapseListener
    {
        void onGroupCollapse(int groupPosition);
    }

    public interface OnGroupExpandListener
    {
        void onGroupExpand(int groupPosition);
    }

    public interface OnGroupClickListener
    {
        boolean onGroupClick(ExpandableListView parent, View view, int groupPosition, long id);
    }

    public interface OnChildClickListener
    {
        boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id);
    }

    public static class ExpandableListContextMenuInfo implements ContextMenu.ContextMenuInfo
    {
        public View targetView;
        public long packedPosition;
        public long id;

        public ExpandableListContextMenuInfo(View targetView, long packedPosition, long id)
        {
            this.targetView = targetView;
            this.packedPosition = packedPosition;
            this.id = id;
        }
    }
}

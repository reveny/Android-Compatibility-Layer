package android.widget;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

public abstract class BaseExpandableListAdapter implements ExpandableListAdapter, HeterogeneousExpandableList
{
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    public BaseExpandableListAdapter()
    {
    }

    public void registerDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.unregisterObserver(observer);
    }

    public void notifyDataSetInvalidated()
    {
        mDataSetObservable.notifyInvalidated();
    }

    public void notifyDataSetChanged()
    {
        mDataSetObservable.notifyChanged();
    }

    public boolean areAllItemsEnabled()
    {
        return true;
    }

    public void onGroupCollapsed(int groupPosition)
    {
    }

    public void onGroupExpanded(int groupPosition)
    {
    }

    public long getCombinedChildId(long groupId, long childId)
    {
        return 0x8000000000000000L | ((groupId & 0x7FFFFFFFL) << 32) | (childId & 0xFFFFFFFFL);
    }

    public long getCombinedGroupId(long groupId)
    {
        return (groupId & 0x7FFFFFFFL) << 32;
    }

    public boolean isEmpty()
    {
        return getGroupCount() == 0;
    }

    public int getChildType(int groupPosition, int childPosition)
    {
        return 0;
    }

    public int getChildTypeCount()
    {
        return 1;
    }

    public int getGroupType(int groupPosition)
    {
        return 0;
    }

    public int getGroupTypeCount()
    {
        return 1;
    }
}

package android.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

class ExpandableListConnector extends BaseAdapter implements Filterable
{
    private ExpandableListAdapter mExpandableListAdapter;
    private final ArrayList<Integer> mExpandedGroups = new ArrayList<>();
    private final ArrayList<Long> mExpandedIds = new ArrayList<>();
    private int[] mFlatGroups = new int[0];
    private int[] mFlatChildren = new int[0];
    private int[] mGroupFlatPositions = new int[0];
    private int mMaxExpandedGroupCount = Integer.MAX_VALUE;
    private final DataSetObserver mDataSetObserver = new MyDataSetObserver();

    ExpandableListConnector(ExpandableListAdapter expandableListAdapter)
    {
        setExpandableListAdapter(expandableListAdapter);
    }

    void setExpandableListAdapter(ExpandableListAdapter expandableListAdapter)
    {
        if (mExpandableListAdapter != null) mExpandableListAdapter.unregisterDataSetObserver(mDataSetObserver);

        mExpandableListAdapter = expandableListAdapter;
        mExpandedGroups.clear();
        mExpandedIds.clear();

        if (expandableListAdapter != null) expandableListAdapter.registerDataSetObserver(mDataSetObserver);

        rebuild();
    }

    ExpandableListAdapter getAdapter()
    {
        return mExpandableListAdapter;
    }

    void setMaxExpGroupCount(int maxExpandedGroupCount)
    {
        mMaxExpandedGroupCount = maxExpandedGroupCount;
    }

    private void rebuild()
    {
        ExpandableListAdapter adapter = mExpandableListAdapter;
        int groupCount = adapter == null ? 0 : adapter.getGroupCount();

        int total = groupCount;
        for (int index = 0; index < mExpandedGroups.size(); index++)
        {
            int group = mExpandedGroups.get(index);
            if (group < groupCount) total += adapter.getChildrenCount(group);
        }

        int[] groups = new int[total];
        int[] children = new int[total];
        int[] groupPositions = new int[groupCount];
        int flat = 0;

        for (int group = 0; group < groupCount; group++)
        {
            groupPositions[group] = flat;
            groups[flat] = group;
            children[flat] = -1;
            flat++;

            if (!mExpandedGroups.contains(group)) continue;

            int childCount = adapter.getChildrenCount(group);
            for (int child = 0; child < childCount; child++)
            {
                groups[flat] = group;
                children[flat] = child;
                flat++;
            }
        }

        mFlatGroups = groups;
        mFlatChildren = children;
        mGroupFlatPositions = groupPositions;
    }

    private void refreshExpandedGroups()
    {
        ExpandableListAdapter adapter = mExpandableListAdapter;
        int groupCount = adapter == null ? 0 : adapter.getGroupCount();

        if (adapter != null && adapter.hasStableIds())
        {
            for (int index = mExpandedGroups.size() - 1; index >= 0; index--)
            {
                long id = mExpandedIds.get(index);
                int position = mExpandedGroups.get(index);

                if (position < groupCount && adapter.getGroupId(position) == id) continue;

                int found = -1;
                for (int group = 0; group < groupCount; group++)
                {
                    if (adapter.getGroupId(group) != id) continue;

                    found = group;
                    break;
                }

                if (found < 0)
                {
                    mExpandedGroups.remove(index);
                    mExpandedIds.remove(index);
                }
                else
                {
                    mExpandedGroups.set(index, found);
                }
            }
        }
        else
        {
            for (int index = mExpandedGroups.size() - 1; index >= 0; index--)
            {
                if (mExpandedGroups.get(index) < groupCount) continue;

                mExpandedGroups.remove(index);
                mExpandedIds.remove(index);
            }
        }

        rebuild();
    }

    boolean isGroupExpanded(int groupPosition)
    {
        return mExpandedGroups.contains(groupPosition);
    }

    boolean expandGroup(int groupPosition)
    {
        ExpandableListAdapter adapter = mExpandableListAdapter;
        if (adapter == null || groupPosition < 0 || groupPosition >= adapter.getGroupCount()) return false;
        if (mExpandedGroups.contains(groupPosition)) return false;

        if (mExpandedGroups.size() >= mMaxExpandedGroupCount && !mExpandedGroups.isEmpty())
        {
            int oldest = mExpandedGroups.get(0);
            collapseGroup(oldest);
        }

        mExpandedGroups.add(groupPosition);
        mExpandedIds.add(adapter.getGroupId(groupPosition));

        adapter.onGroupExpanded(groupPosition);
        rebuild();
        notifyDataSetChanged();
        return true;
    }

    boolean collapseGroup(int groupPosition)
    {
        int index = mExpandedGroups.indexOf(groupPosition);
        if (index < 0) return false;

        mExpandedGroups.remove(index);
        mExpandedIds.remove(index);

        if (mExpandableListAdapter != null) mExpandableListAdapter.onGroupCollapsed(groupPosition);
        rebuild();
        notifyDataSetChanged();
        return true;
    }

    int[] getExpandedGroups()
    {
        int[] result = new int[mExpandedGroups.size()];
        for (int index = 0; index < result.length; index++) result[index] = mExpandedGroups.get(index);
        return result;
    }

    long[] getExpandedGroupIds()
    {
        long[] result = new long[mExpandedIds.size()];
        for (int index = 0; index < result.length; index++) result[index] = mExpandedIds.get(index);
        return result;
    }

    void setExpandedGroups(int[] groups, long[] ids)
    {
        mExpandedGroups.clear();
        mExpandedIds.clear();

        if (groups != null && mExpandableListAdapter != null)
        {
            int groupCount = mExpandableListAdapter.getGroupCount();
            for (int index = 0; index < groups.length; index++)
            {
                if (groups[index] < 0 || groups[index] >= groupCount) continue;

                mExpandedGroups.add(groups[index]);
                mExpandedIds.add(ids != null && index < ids.length ? ids[index] : mExpandableListAdapter.getGroupId(groups[index]));
            }
        }

        refreshExpandedGroups();
        notifyDataSetChanged();
    }

    int groupAt(int flatPosition)
    {
        return flatPosition < 0 || flatPosition >= mFlatGroups.length ? -1 : mFlatGroups[flatPosition];
    }

    int childAt(int flatPosition)
    {
        return flatPosition < 0 || flatPosition >= mFlatChildren.length ? -1 : mFlatChildren[flatPosition];
    }

    int flatPositionOfGroup(int groupPosition)
    {
        return groupPosition < 0 || groupPosition >= mGroupFlatPositions.length ? -1 : mGroupFlatPositions[groupPosition];
    }

    int flatPositionOfChild(int groupPosition, int childPosition)
    {
        if (!isGroupExpanded(groupPosition)) return -1;

        int base = flatPositionOfGroup(groupPosition);
        if (base < 0 || childPosition < 0 || childPosition >= mExpandableListAdapter.getChildrenCount(groupPosition)) return -1;

        return base + 1 + childPosition;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return mExpandableListAdapter == null || mExpandableListAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int flatPosition)
    {
        int child = childAt(flatPosition);
        if (child < 0) return true;

        return mExpandableListAdapter.isChildSelectable(groupAt(flatPosition), child);
    }

    @Override
    public int getCount()
    {
        return mFlatGroups.length;
    }

    @Override
    public Object getItem(int flatPosition)
    {
        int group = groupAt(flatPosition);
        if (group < 0) return null;

        int child = childAt(flatPosition);
        return child < 0 ? mExpandableListAdapter.getGroup(group) : mExpandableListAdapter.getChild(group, child);
    }

    @Override
    public long getItemId(int flatPosition)
    {
        int group = groupAt(flatPosition);
        if (group < 0) return 0;

        long groupId = mExpandableListAdapter.getGroupId(group);
        int child = childAt(flatPosition);
        if (child < 0) return mExpandableListAdapter.getCombinedGroupId(groupId);

        return mExpandableListAdapter.getCombinedChildId(groupId, mExpandableListAdapter.getChildId(group, child));
    }

    @Override
    public View getView(int flatPosition, View convertView, ViewGroup parent)
    {
        int group = groupAt(flatPosition);
        if (group < 0) throw new IndexOutOfBoundsException("flat list position " + flatPosition + " is out of range");

        int child = childAt(flatPosition);
        if (child < 0) return mExpandableListAdapter.getGroupView(group, isGroupExpanded(group), convertView, parent);

        boolean isLastChild = child == mExpandableListAdapter.getChildrenCount(group) - 1;
        return mExpandableListAdapter.getChildView(group, child, isLastChild, convertView, parent);
    }

    @Override
    public int getItemViewType(int flatPosition)
    {
        int group = groupAt(flatPosition);
        int child = childAt(flatPosition);

        if (mExpandableListAdapter instanceof HeterogeneousExpandableList)
        {
            HeterogeneousExpandableList list = (HeterogeneousExpandableList)mExpandableListAdapter;
            if (child < 0) return list.getGroupType(group);

            return list.getChildType(group, child) + list.getGroupTypeCount();
        }

        return child < 0 ? 0 : 1;
    }

    @Override
    public int getViewTypeCount()
    {
        if (mExpandableListAdapter instanceof HeterogeneousExpandableList)
        {
            HeterogeneousExpandableList list = (HeterogeneousExpandableList)mExpandableListAdapter;
            return list.getGroupTypeCount() + list.getChildTypeCount();
        }

        return 2;
    }

    @Override
    public boolean hasStableIds()
    {
        return mExpandableListAdapter != null && mExpandableListAdapter.hasStableIds();
    }

    @Override
    public boolean isEmpty()
    {
        return mExpandableListAdapter == null || mExpandableListAdapter.isEmpty();
    }

    @Override
    public Filter getFilter()
    {
        ExpandableListAdapter adapter = mExpandableListAdapter;
        return adapter instanceof Filterable ? ((Filterable)adapter).getFilter() : null;
    }

    private class MyDataSetObserver extends DataSetObserver
    {
        @Override
        public void onChanged()
        {
            refreshExpandedGroups();
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated()
        {
            refreshExpandedGroups();
            notifyDataSetInvalidated();
        }
    }
}

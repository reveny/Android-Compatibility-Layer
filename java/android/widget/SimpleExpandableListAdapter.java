package android.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Map;

public class SimpleExpandableListAdapter extends BaseExpandableListAdapter
{
    private final List<? extends Map<String, ?>> mGroupData;
    private final int mExpandedGroupLayout;
    private final int mCollapsedGroupLayout;
    private final String[] mGroupFrom;
    private final int[] mGroupTo;
    private final List<? extends List<? extends Map<String, ?>>> mChildData;
    private final int mChildLayout;
    private final int mLastChildLayout;
    private final String[] mChildFrom;
    private final int[] mChildTo;
    private final LayoutInflater mInflater;

    public SimpleExpandableListAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom,
                                       int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout,
                                       String[] childFrom, int[] childTo)
    {
        this(context, groupData, groupLayout, groupLayout, groupFrom, groupTo, childData, childLayout, childLayout, childFrom, childTo);
    }

    public SimpleExpandableListAdapter(Context context, List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom,
                                       int[] childTo)
    {
        this(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom, groupTo, childData, childLayout, childLayout,
             childFrom, childTo);
    }

    public SimpleExpandableListAdapter(Context context, List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                       int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                       List<? extends List<? extends Map<String, ?>>> childData, int childLayout, int lastChildLayout,
                                       String[] childFrom, int[] childTo)
    {
        mGroupData = groupData;
        mExpandedGroupLayout = expandedGroupLayout;
        mCollapsedGroupLayout = collapsedGroupLayout;
        mGroupFrom = groupFrom;
        mGroupTo = groupTo;
        mChildData = childData;
        mChildLayout = childLayout;
        mLastChildLayout = lastChildLayout;
        mChildFrom = childFrom;
        mChildTo = childTo;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Object getChild(int groupPosition, int childPosition)
    {
        return mChildData.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        View view = convertView == null ? newChildView(isLastChild, parent) : convertView;
        bindView(view, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
        return view;
    }

    public View newChildView(boolean isLastChild, ViewGroup parent)
    {
        return mInflater.inflate(isLastChild ? mLastChildLayout : mChildLayout, parent, false);
    }

    private void bindView(View view, Map<String, ?> data, String[] from, int[] to)
    {
        if (from == null || to == null) return;

        int count = Math.min(from.length, to.length);

        for (int index = 0; index < count; index++)
        {
            View target = view.findViewById(to[index]);
            if (!(target instanceof TextView)) continue;

            Object value = data.get(from[index]);
            ((TextView)target).setText(value == null ? "" : value.toString());
        }
    }

    public int getChildrenCount(int groupPosition)
    {
        return mChildData.get(groupPosition).size();
    }

    public Object getGroup(int groupPosition)
    {
        return mGroupData.get(groupPosition);
    }

    public int getGroupCount()
    {
        return mGroupData.size();
    }

    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        View view = convertView == null ? newGroupView(isExpanded, parent) : convertView;
        bindView(view, mGroupData.get(groupPosition), mGroupFrom, mGroupTo);
        return view;
    }

    public View newGroupView(boolean isExpanded, ViewGroup parent)
    {
        return mInflater.inflate(isExpanded ? mExpandedGroupLayout : mCollapsedGroupLayout, parent, false);
    }

    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

    public boolean hasStableIds()
    {
        return true;
    }
}

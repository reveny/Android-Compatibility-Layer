package android.widget;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class HeaderViewListAdapter implements WrapperListAdapter, Filterable
{
    private static final ArrayList<ListView.FixedViewInfo> EMPTY_INFO_LIST = new ArrayList<ListView.FixedViewInfo>();

    private final ListAdapter mAdapter;
    private final boolean mIsFilterable;
    private ArrayList<ListView.FixedViewInfo> mHeaderViewInfos;
    private ArrayList<ListView.FixedViewInfo> mFooterViewInfos;
    private boolean mAreAllFixedViewsSelectable;

    public HeaderViewListAdapter(ArrayList<ListView.FixedViewInfo> headerViewInfos, ArrayList<ListView.FixedViewInfo> footerViewInfos,
                                 ListAdapter adapter)
    {
        mAdapter = adapter;
        mIsFilterable = adapter instanceof Filterable;
        mHeaderViewInfos = headerViewInfos == null ? EMPTY_INFO_LIST : headerViewInfos;
        mFooterViewInfos = footerViewInfos == null ? EMPTY_INFO_LIST : footerViewInfos;
        mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos);
    }

    public int getHeadersCount()
    {
        return mHeaderViewInfos.size();
    }

    public int getFootersCount()
    {
        return mFooterViewInfos.size();
    }

    @Override
    public boolean isEmpty()
    {
        return mAdapter == null || mAdapter.isEmpty();
    }

    private static boolean areAllListInfosSelectable(ArrayList<ListView.FixedViewInfo> infos)
    {
        if (infos == null) return true;

        for (ListView.FixedViewInfo info : infos)
        {
            if (!info.isSelectable) return false;
        }

        return true;
    }

    public boolean removeHeader(View view)
    {
        return removeInfo(mHeaderViewInfos, view);
    }

    public boolean removeFooter(View view)
    {
        return removeInfo(mFooterViewInfos, view);
    }

    private boolean removeInfo(ArrayList<ListView.FixedViewInfo> infos, View view)
    {
        for (int index = 0; index < infos.size(); index++)
        {
            if (infos.get(index).view != view) continue;

            infos.remove(index);
            mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos);
            return true;
        }

        return false;
    }

    @Override
    public int getCount()
    {
        int fixed = getFootersCount() + getHeadersCount();
        return mAdapter == null ? fixed : fixed + mAdapter.getCount();
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        if (mAdapter == null) return true;
        return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position)
    {
        int headers = getHeadersCount();
        if (position < headers) return mHeaderViewInfos.get(position).isSelectable;

        int adjusted = position - headers;
        int adapterCount = 0;
        if (mAdapter != null)
        {
            adapterCount = mAdapter.getCount();
            if (adjusted < adapterCount) return mAdapter.isEnabled(adjusted);
        }

        return mFooterViewInfos.get(adjusted - adapterCount).isSelectable;
    }

    @Override
    public Object getItem(int position)
    {
        int headers = getHeadersCount();
        if (position < headers) return mHeaderViewInfos.get(position).data;

        int adjusted = position - headers;
        int adapterCount = 0;
        if (mAdapter != null)
        {
            adapterCount = mAdapter.getCount();
            if (adjusted < adapterCount) return mAdapter.getItem(adjusted);
        }

        return mFooterViewInfos.get(adjusted - adapterCount).data;
    }

    @Override
    public long getItemId(int position)
    {
        int headers = getHeadersCount();
        if (mAdapter != null && position >= headers)
        {
            int adjusted = position - headers;
            if (adjusted < mAdapter.getCount()) return mAdapter.getItemId(adjusted);
        }

        return -1;
    }

    @Override
    public boolean hasStableIds()
    {
        return mAdapter != null && mAdapter.hasStableIds();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        int headers = getHeadersCount();
        if (position < headers) return mHeaderViewInfos.get(position).view;

        int adjusted = position - headers;
        int adapterCount = 0;
        if (mAdapter != null)
        {
            adapterCount = mAdapter.getCount();
            if (adjusted < adapterCount) return mAdapter.getView(adjusted, convertView, parent);
        }

        return mFooterViewInfos.get(adjusted - adapterCount).view;
    }

    @Override
    public int getItemViewType(int position)
    {
        int headers = getHeadersCount();
        if (mAdapter != null && position >= headers)
        {
            int adjusted = position - headers;
            if (adjusted < mAdapter.getCount()) return mAdapter.getItemViewType(adjusted);
        }

        return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    @Override
    public int getViewTypeCount()
    {
        return mAdapter == null ? 1 : mAdapter.getViewTypeCount();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer)
    {
        if (mAdapter != null) mAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        if (mAdapter != null) mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public Filter getFilter()
    {
        if (mIsFilterable) return ((Filterable)mAdapter).getFilter();
        return null;
    }

    @Override
    public ListAdapter getWrappedAdapter()
    {
        return mAdapter;
    }
}

package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SimpleAdapter extends BaseAdapter implements Filterable, ThemedSpinnerAdapter
{
    private final LayoutInflater mInflater;
    private final int[] mTo;
    private final String[] mFrom;
    private ViewBinder mViewBinder;
    private List<? extends Map<String, ?>> mData;
    private final int mResource;
    private int mDropDownResource;
    private LayoutInflater mDropDownInflater;
    private SimpleFilter mFilter;
    private ArrayList<Map<String, ?>> mUnfilteredData;

    public SimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to)
    {
        mData = data;
        mResource = resource;
        mDropDownResource = resource;
        mFrom = from;
        mTo = to;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        return mData.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    private View createViewFromResource(LayoutInflater inflater, int position, View convertView, ViewGroup parent, int resource)
    {
        View view = convertView == null ? inflater.inflate(resource, parent, false) : convertView;

        bindView(position, view);
        return view;
    }

    public void setDropDownViewResource(int resource)
    {
        mDropDownResource = resource;
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme)
    {
        if (theme == null) mDropDownInflater = null;
        else if (theme == mInflater.getContext().getTheme()) mDropDownInflater = mInflater;
        else mDropDownInflater = LayoutInflater.from(new ContextThemeWrapper(mInflater.getContext(), theme));
    }

    @Override
    public Resources.Theme getDropDownViewTheme()
    {
        return mDropDownInflater == null ? null : mDropDownInflater.getContext().getTheme();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = mDropDownInflater == null ? mInflater : mDropDownInflater;
        return createViewFromResource(inflater, position, convertView, parent, mDropDownResource);
    }

    private void bindView(int position, View view)
    {
        Map<String, ?> data = mData.get(position);
        if (data == null) return;

        ViewBinder binder = mViewBinder;
        int count = Math.min(mTo.length, mFrom.length);

        for (int index = 0; index < count; index++)
        {
            View target = view.findViewById(mTo[index]);
            if (target == null) continue;

            Object value = data.get(mFrom[index]);
            String text = value == null ? "" : value.toString();

            boolean bound = binder != null && binder.setViewValue(target, value, text);
            if (bound) continue;

            if (target instanceof Checkable)
            {
                if (value instanceof Boolean) ((Checkable)target).setChecked((Boolean)value);
                else if (target instanceof TextView) setViewText((TextView)target, text);
                else throw new IllegalStateException(target.getClass().getName() + " should be bound to a Boolean, not a "
                                                     + (value == null ? "<unknown type>" : value.getClass().getName()));
            }
            else if (target instanceof TextView)
            {
                setViewText((TextView)target, text);
            }
            else if (target instanceof ImageView)
            {
                if (value instanceof Integer) setViewImage((ImageView)target, (Integer)value);
                else setViewImage((ImageView)target, text);
            }
            else
            {
                throw new IllegalStateException(target.getClass().getName() + " is not a view that can be bounds by this SimpleAdapter");
            }
        }
    }

    public ViewBinder getViewBinder()
    {
        return mViewBinder;
    }

    public void setViewBinder(ViewBinder viewBinder)
    {
        mViewBinder = viewBinder;
    }

    public void setViewImage(ImageView view, int value)
    {
        view.setImageResource(value);
    }

    public void setViewImage(ImageView view, String value)
    {
        try
        {
            view.setImageResource(Integer.parseInt(value));
        }
        catch (NumberFormatException notResource)
        {
            view.setImageURI(Uri.parse(value));
        }
    }

    public void setViewText(TextView view, String text)
    {
        view.setText(text);
    }

    @Override
    public Filter getFilter()
    {
        if (mFilter == null) mFilter = new SimpleFilter();
        return mFilter;
    }

    public interface ViewBinder
    {
        boolean setViewValue(View view, Object data, String textRepresentation);
    }

    private class SimpleFilter extends Filter
    {
        @Override
        protected FilterResults performFiltering(CharSequence prefix)
        {
            FilterResults results = new FilterResults();

            if (mUnfilteredData == null) mUnfilteredData = new ArrayList<Map<String, ?>>(mData);

            if (prefix == null || prefix.length() == 0)
            {
                ArrayList<Map<String, ?>> list = new ArrayList<Map<String, ?>>(mUnfilteredData);
                results.values = list;
                results.count = list.size();
                return results;
            }

            String prefixString = prefix.toString().toLowerCase(Locale.getDefault());
            ArrayList<Map<String, ?>> matches = new ArrayList<Map<String, ?>>();

            for (Map<String, ?> entry : mUnfilteredData)
            {
                if (entry == null) continue;

                for (String key : mFrom)
                {
                    Object value = entry.get(key);
                    if (value == null) continue;

                    if (matchesPrefix(value.toString().toLowerCase(Locale.getDefault()), prefixString))
                    {
                        matches.add(entry);
                        break;
                    }
                }
            }

            results.values = matches;
            results.count = matches.size();
            return results;
        }

        private boolean matchesPrefix(String value, String prefix)
        {
            for (String word : value.split(" "))
            {
                if (word.startsWith(prefix)) return true;
            }

            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            mData = results.values == null ? new ArrayList<Map<String, ?>>() : (List<Map<String, ?>>)results.values;

            if (results.count > 0) notifyDataSetChanged();
            else notifyDataSetInvalidated();
        }
    }
}

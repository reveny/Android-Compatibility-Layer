package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ArrayAdapter<T> extends BaseAdapter implements Filterable, ThemedSpinnerAdapter
{
    private final Object mLock = new Object();
    private final Context mContext;
    private final LayoutInflater mInflater;
    private LayoutInflater mDropDownInflater;
    private final int mResource;
    private int mDropDownResource;
    private int mFieldId;
    private List<T> mObjects;
    private final boolean mObjectsFromResources;
    private boolean mNotifyOnChange = true;
    private ArrayList<T> mOriginalValues;
    private ArrayFilter mFilter;

    public ArrayAdapter(Context context, int resource)
    {
        this(context, resource, 0, new ArrayList<T>());
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId)
    {
        this(context, resource, textViewResourceId, new ArrayList<T>());
    }

    public ArrayAdapter(Context context, int resource, T[] objects)
    {
        this(context, resource, 0, Arrays.asList(objects));
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects)
    {
        this(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    public ArrayAdapter(Context context, int resource, List<T> objects)
    {
        this(context, resource, 0, objects);
    }

    public ArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects)
    {
        this(context, resource, textViewResourceId, objects, false);
    }

    private ArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, boolean objectsFromResources)
    {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        mDropDownResource = resource;
        mObjects = objects;
        mObjectsFromResources = objectsFromResources;
        mFieldId = textViewResourceId;
    }

    public void add(T object)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) mOriginalValues.add(object);
            else mObjects.add(object);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void addAll(Collection<? extends T> collection)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) mOriginalValues.addAll(collection);
            else mObjects.addAll(collection);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    public void addAll(T... items)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) Collections.addAll(mOriginalValues, items);
            else Collections.addAll(mObjects, items);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void insert(T object, int index)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) mOriginalValues.add(index, object);
            else mObjects.add(index, object);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void remove(T object)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) mOriginalValues.remove(object);
            else mObjects.remove(object);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void clear()
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) mOriginalValues.clear();
            else mObjects.clear();
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    public void sort(Comparator<? super T> comparator)
    {
        synchronized (mLock)
        {
            if (mOriginalValues != null) Collections.sort(mOriginalValues, comparator);
            else Collections.sort(mObjects, comparator);
        }

        if (mNotifyOnChange) notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged()
    {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    public void setNotifyOnChange(boolean notifyOnChange)
    {
        mNotifyOnChange = notifyOnChange;
    }

    public Context getContext()
    {
        return mContext;
    }

    @Override
    public int getCount()
    {
        return mObjects.size();
    }

    @Override
    public T getItem(int position)
    {
        return mObjects.get(position);
    }

    public int getPosition(T item)
    {
        return mObjects.indexOf(item);
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
        View view = convertView != null ? convertView : inflateItem(inflater, resource, parent);

        TextView text;
        try
        {
            if (mFieldId == 0) text = (TextView)view;
            else text = (TextView)view.findViewById(mFieldId);

            if (text == null)
            {
                throw new RuntimeException("Failed to find view with ID " + mContext.getResources().getResourceName(mFieldId)
                                           + " in item layout");
            }
        }
        catch (ClassCastException failure)
        {
            throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", failure);
        }

        T item = getItem(position);
        if (item instanceof CharSequence) text.setText((CharSequence)item);
        else text.setText(item == null ? "null" : item.toString());

        return view;
    }

    private View inflateItem(LayoutInflater inflater, int resource, ViewGroup parent)
    {
        try
        {
            return inflater.inflate(resource, parent, false);
        }
        catch (RuntimeException failure)
        {
            if (mFieldId != 0) throw failure;

            android.util.Log.w("ArrayAdapter", "item layout 0x" + Integer.toHexString(resource)
                                               + " could not be inflated, so a plain TextView is used", failure);
            return fallbackItemView(inflater.getContext());
        }
    }

    static TextView fallbackItemView(Context context)
    {
        float density = context.getResources().getDisplayMetrics().density;
        int horizontal = (int)(16 * density);
        int vertical = (int)(12 * density);

        TextView text = new TextView(context);
        text.setPadding(horizontal, vertical, horizontal, vertical);
        text.setMinimumHeight((int)(48 * density));
        text.setTextSize(16);
        return text;
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
        else mDropDownInflater = LayoutInflater.from(new ContextThemeWrapper(mContext, theme));
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

    public static ArrayAdapter<CharSequence> createFromResource(Context context, int textArrayResId, int textViewResId)
    {
        CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new ArrayAdapter<CharSequence>(context, textViewResId, 0, Arrays.asList(strings), true);
    }

    @Override
    public Filter getFilter()
    {
        if (mFilter == null) mFilter = new ArrayFilter();
        return mFilter;
    }

    @Override
    public CharSequence[] getAutofillOptions()
    {
        CharSequence[] explicit = super.getAutofillOptions();
        if (explicit != null) return explicit;

        if (!mObjectsFromResources || mObjects == null || mObjects.isEmpty()) return null;

        CharSequence[] options = new CharSequence[mObjects.size()];
        mObjects.toArray(options);
        return options;
    }

    private class ArrayFilter extends Filter
    {
        @Override
        protected FilterResults performFiltering(CharSequence prefix)
        {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null)
            {
                synchronized (mLock)
                {
                    mOriginalValues = new ArrayList<>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0)
            {
                ArrayList<T> list;
                synchronized (mLock)
                {
                    list = new ArrayList<>(mOriginalValues);
                }

                results.values = list;
                results.count = list.size();
                return results;
            }

            String prefixString = prefix.toString().toLowerCase(Locale.getDefault());

            ArrayList<T> values;
            synchronized (mLock)
            {
                values = new ArrayList<>(mOriginalValues);
            }

            ArrayList<T> matches = new ArrayList<>();
            for (T value : values)
            {
                String valueText = String.valueOf(value).toLowerCase(Locale.getDefault());

                if (valueText.startsWith(prefixString))
                {
                    matches.add(value);
                    continue;
                }

                String[] words = valueText.split(" ");
                for (String word : words)
                {
                    if (!word.startsWith(prefixString)) continue;

                    matches.add(value);
                    break;
                }
            }

            results.values = matches;
            results.count = matches.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            mObjects = results.values == null ? new ArrayList<T>() : (List<T>)results.values;

            if (results.count > 0) notifyDataSetChanged();
            else notifyDataSetInvalidated();
        }
    }
}

package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

public abstract class CursorAdapter extends BaseAdapter implements Filterable, ThemedSpinnerAdapter
{
    public static final int FLAG_AUTO_REQUERY = 0x01;
    public static final int FLAG_REGISTER_CONTENT_OBSERVER = 0x02;

    private static final String ROW_ID_COLUMN = "_id";

    private boolean mDataValid;
    private boolean mAutoRequery;
    private Cursor mCursor;
    private Context mContext;
    private Context mDropDownContext;
    private int mRowIdColumn;
    private ChangeObserver mChangeObserver;
    private DataSetObserver mDataSetObserver;
    private CursorFilter mCursorFilter;
    private FilterQueryProvider mFilterQueryProvider;

    public CursorAdapter(Context context, Cursor cursor)
    {
        init(context, cursor, FLAG_AUTO_REQUERY);
    }

    public CursorAdapter(Context context, Cursor cursor, boolean autoRequery)
    {
        init(context, cursor, autoRequery ? FLAG_AUTO_REQUERY : FLAG_REGISTER_CONTENT_OBSERVER);
    }

    public CursorAdapter(Context context, Cursor cursor, int flags)
    {
        init(context, cursor, flags);
    }

    protected void init(Context context, Cursor cursor, boolean autoRequery)
    {
        init(context, cursor, autoRequery ? FLAG_AUTO_REQUERY : FLAG_REGISTER_CONTENT_OBSERVER);
    }

    void init(Context context, Cursor cursor, int flags)
    {
        if ((flags & FLAG_AUTO_REQUERY) == FLAG_AUTO_REQUERY)
        {
            flags |= FLAG_REGISTER_CONTENT_OBSERVER;
            mAutoRequery = true;
        }
        else
        {
            mAutoRequery = false;
        }

        boolean cursorPresent = cursor != null;
        mCursor = cursor;
        mDataValid = cursorPresent;
        mContext = context;
        mRowIdColumn = cursorPresent ? cursor.getColumnIndexOrThrow(ROW_ID_COLUMN) : -1;

        if ((flags & FLAG_REGISTER_CONTENT_OBSERVER) == FLAG_REGISTER_CONTENT_OBSERVER)
        {
            mChangeObserver = new ChangeObserver();
            mDataSetObserver = new MyDataSetObserver();
        }
        else
        {
            mChangeObserver = null;
            mDataSetObserver = null;
        }

        if (!cursorPresent) return;

        if (mChangeObserver != null) cursor.registerContentObserver(mChangeObserver);
        if (mDataSetObserver != null) cursor.registerDataSetObserver(mDataSetObserver);
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme)
    {
        if (theme == null) mDropDownContext = null;
        else if (theme == mContext.getTheme()) mDropDownContext = mContext;
        else mDropDownContext = new ContextThemeWrapper(mContext, theme);
    }

    @Override
    public Resources.Theme getDropDownViewTheme()
    {
        return mDropDownContext == null ? null : mDropDownContext.getTheme();
    }

    public Cursor getCursor()
    {
        return mCursor;
    }

    @Override
    public int getCount()
    {
        if (mDataValid && mCursor != null) return mCursor.getCount();
        return 0;
    }

    @Override
    public Object getItem(int position)
    {
        if (!mDataValid || mCursor == null) return null;

        mCursor.moveToPosition(position);
        return mCursor;
    }

    @Override
    public long getItemId(int position)
    {
        if (!mDataValid || mCursor == null) return 0;
        if (!mCursor.moveToPosition(position)) return 0;

        return mCursor.getLong(mRowIdColumn);
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (!mDataValid) throw new IllegalStateException("this should only be called when the cursor is valid");
        if (!mCursor.moveToPosition(position)) throw new IllegalStateException("couldn't move cursor to position " + position);

        View view = convertView == null ? newView(mContext, mCursor, parent) : convertView;
        bindView(view, mContext, mCursor);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        if (!mDataValid) return null;

        Context context = mDropDownContext == null ? mContext : mDropDownContext;
        mCursor.moveToPosition(position);

        View view = convertView == null ? newDropDownView(context, mCursor, parent) : convertView;
        bindView(view, context, mCursor);
        return view;
    }

    public abstract View newView(Context context, Cursor cursor, ViewGroup parent);

    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent)
    {
        return newView(context, cursor, parent);
    }

    public abstract void bindView(View view, Context context, Cursor cursor);

    public void changeCursor(Cursor cursor)
    {
        Cursor old = swapCursor(cursor);
        if (old != null) old.close();
    }

    public Cursor swapCursor(Cursor newCursor)
    {
        if (newCursor == mCursor) return null;

        Cursor old = mCursor;
        if (old != null)
        {
            if (mChangeObserver != null) old.unregisterContentObserver(mChangeObserver);
            if (mDataSetObserver != null) old.unregisterDataSetObserver(mDataSetObserver);
        }

        mCursor = newCursor;

        if (newCursor == null)
        {
            mRowIdColumn = -1;
            mDataValid = false;
            notifyDataSetInvalidated();
            return old;
        }

        if (mChangeObserver != null) newCursor.registerContentObserver(mChangeObserver);
        if (mDataSetObserver != null) newCursor.registerDataSetObserver(mDataSetObserver);

        mRowIdColumn = newCursor.getColumnIndexOrThrow(ROW_ID_COLUMN);
        mDataValid = true;
        notifyDataSetChanged();
        return old;
    }

    public CharSequence convertToString(Cursor cursor)
    {
        return cursor == null ? "" : cursor.toString();
    }

    public Cursor runQueryOnBackgroundThread(CharSequence constraint)
    {
        if (mFilterQueryProvider != null) return mFilterQueryProvider.runQuery(constraint);
        return mCursor;
    }

    @Override
    public Filter getFilter()
    {
        if (mCursorFilter == null) mCursorFilter = new CursorFilter();
        return mCursorFilter;
    }

    public FilterQueryProvider getFilterQueryProvider()
    {
        return mFilterQueryProvider;
    }

    public void setFilterQueryProvider(FilterQueryProvider filterQueryProvider)
    {
        mFilterQueryProvider = filterQueryProvider;
    }

    protected void onContentChanged()
    {
        if (!mAutoRequery || mCursor == null || mCursor.isClosed()) return;

        mDataValid = mCursor.requery();
    }

    private class ChangeObserver extends ContentObserver
    {
        ChangeObserver()
        {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications()
        {
            return true;
        }

        @Override
        public void onChange(boolean selfChange)
        {
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver
    {
        @Override
        public void onChanged()
        {
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated()
        {
            mDataValid = false;
            notifyDataSetInvalidated();
        }
    }

    private class CursorFilter extends Filter
    {
        @Override
        public CharSequence convertResultToString(Object resultValue)
        {
            return convertToString((Cursor)resultValue);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            Cursor cursor = runQueryOnBackgroundThread(constraint);

            FilterResults results = new FilterResults();
            if (cursor != null)
            {
                results.count = cursor.getCount();
                results.values = cursor;
            }
            else
            {
                results.count = 0;
                results.values = null;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            Cursor old = mCursor;
            if (results.values != null && results.values != old) changeCursor((Cursor)results.values);
        }
    }
}

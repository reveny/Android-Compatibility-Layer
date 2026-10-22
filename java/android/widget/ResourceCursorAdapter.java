package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class ResourceCursorAdapter extends CursorAdapter
{
    private int mLayout;
    private int mDropDownLayout;
    private LayoutInflater mInflater;
    private LayoutInflater mDropDownInflater;

    public ResourceCursorAdapter(Context context, int layout, Cursor cursor)
    {
        super(context, cursor);
        setup(context, layout);
    }

    public ResourceCursorAdapter(Context context, int layout, Cursor cursor, boolean autoRequery)
    {
        super(context, cursor, autoRequery);
        setup(context, layout);
    }

    public ResourceCursorAdapter(Context context, int layout, Cursor cursor, int flags)
    {
        super(context, cursor, flags);
        setup(context, layout);
    }

    private void setup(Context context, int layout)
    {
        mLayout = layout;
        mDropDownLayout = layout;
        mInflater = LayoutInflater.from(context);
        mDropDownInflater = mInflater;
    }

    @Override
    public void setDropDownViewTheme(Resources.Theme theme)
    {
        super.setDropDownViewTheme(theme);

        if (theme == null) mDropDownInflater = null;
        else if (theme == mInflater.getContext().getTheme()) mDropDownInflater = mInflater;
        else mDropDownInflater = LayoutInflater.from(new ContextThemeWrapper(mInflater.getContext(), theme));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        return mInflater.inflate(mLayout, parent, false);
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent)
    {
        LayoutInflater inflater = mDropDownInflater == null ? mInflater : mDropDownInflater;
        return inflater.inflate(mDropDownLayout, parent, false);
    }

    public void setViewResource(int layout)
    {
        mLayout = layout;
    }

    public void setDropDownViewResource(int dropDownLayout)
    {
        mDropDownLayout = dropDownLayout;
    }
}

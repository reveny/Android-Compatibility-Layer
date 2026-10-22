package android.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;

public class SimpleCursorAdapter extends ResourceCursorAdapter
{
    protected int[] mFrom;
    protected int[] mTo;

    private int mStringConversionColumn = -1;
    private CursorToStringConverter mCursorToStringConverter;
    private ViewBinder mViewBinder;
    private String[] mOriginalFrom;

    @Deprecated
    public SimpleCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to)
    {
        super(context, layout, cursor);
        mTo = to;
        mOriginalFrom = from;
        findColumns(cursor, from);
    }

    public SimpleCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags)
    {
        super(context, layout, cursor, flags);
        mTo = to;
        mOriginalFrom = from;
        findColumns(cursor, from);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        ViewBinder binder = mViewBinder;
        int[] from = mFrom;
        int[] to = mTo;
        if (from == null || to == null) return;

        int count = Math.min(from.length, to.length);

        for (int index = 0; index < count; index++)
        {
            View target = view.findViewById(to[index]);
            if (target == null) continue;

            boolean bound = binder != null && binder.setViewValue(target, cursor, from[index]);
            if (bound) continue;

            String text = cursor.getString(from[index]);
            if (text == null) text = "";

            if (target instanceof TextView)
            {
                setViewText((TextView)target, text);
            }
            else if (target instanceof ImageView)
            {
                setViewImage((ImageView)target, text);
            }
            else
            {
                throw new IllegalStateException(target.getClass().getName() + " is not a view that can be bound by this SimpleCursorAdapter");
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

    public int getStringConversionColumn()
    {
        return mStringConversionColumn;
    }

    public void setStringConversionColumn(int stringConversionColumn)
    {
        mStringConversionColumn = stringConversionColumn;
    }

    public CursorToStringConverter getCursorToStringConverter()
    {
        return mCursorToStringConverter;
    }

    public void setCursorToStringConverter(CursorToStringConverter cursorToStringConverter)
    {
        mCursorToStringConverter = cursorToStringConverter;
    }

    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        if (mCursorToStringConverter != null) return mCursorToStringConverter.convertToString(cursor);
        if (mStringConversionColumn > -1) return cursor.getString(mStringConversionColumn);

        return super.convertToString(cursor);
    }

    private void findColumns(Cursor cursor, String[] from)
    {
        if (cursor == null || from == null)
        {
            mFrom = null;
            return;
        }

        int count = from.length;
        if (mFrom == null || mFrom.length != count) mFrom = new int[count];

        for (int index = 0; index < count; index++) mFrom[index] = cursor.getColumnIndexOrThrow(from[index]);
    }

    @Override
    public Cursor swapCursor(Cursor cursor)
    {
        findColumns(cursor, mOriginalFrom);
        return super.swapCursor(cursor);
    }

    public void changeCursorAndColumns(Cursor cursor, String[] from, int[] to)
    {
        mOriginalFrom = from;
        mTo = to;
        findColumns(cursor, mOriginalFrom);
        super.changeCursor(cursor);
    }

    public interface ViewBinder
    {
        boolean setViewValue(View view, Cursor cursor, int columnIndex);
    }

    public interface CursorToStringConverter
    {
        CharSequence convertToString(Cursor cursor);
    }
}

package android.view;

import android.content.Context;

public class RuntimeWindow extends Window
{
    private final DecorView mDecor;

    public RuntimeWindow(Context context)
    {
        super(context);
        mDecor = new DecorView(context);
        applyWindowBackground(context);
    }

    private void applyWindowBackground(Context context)
    {
        int attribute = context.getResources().getIdentifier("windowBackground", "attr", "android");
        if (attribute == 0) return;

        android.util.TypedValue value = new android.util.TypedValue();
        if (!context.getTheme().resolveAttribute(attribute, value, true)) return;

        if (value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT)
        {
            mDecor.setBackgroundColor(value.data);
            return;
        }

        if (value.resourceId != 0)
        {
            android.graphics.drawable.Drawable background = context.getResources().getDrawable(value.resourceId);
            if (background instanceof android.graphics.drawable.ColorDrawable)
            {
                mDecor.setBackgroundColor(((android.graphics.drawable.ColorDrawable)background).getColor());
            }
        }
    }

    @Override
    public void setContentView(View view)
    {
        setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams)
    {
        ViewGroup content = mDecor.getContentParent();
        content.removeAllViews();
        content.addView(view, layoutParams);

        Callback callback = getCallback();
        if (callback != null) callback.onContentChanged();
    }

    @Override
    public View getDecorView()
    {
        return mDecor;
    }

    @Override
    public View peekDecorView()
    {
        return mDecor;
    }

    @Override
    public View findViewById(int id)
    {
        return mDecor.findViewById(id);
    }
}

package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

public class ContextThemeWrapper extends ContextWrapper
{
    private int mThemeResource;
    private Resources.Theme mTheme;
    private LayoutInflater mInflater;

    public ContextThemeWrapper()
    {
        super(null);
    }

    public ContextThemeWrapper(Context base, int themeResourceId)
    {
        super(base);
        mThemeResource = themeResourceId;
    }

    public ContextThemeWrapper(Context base, Resources.Theme theme)
    {
        super(base);
        mTheme = theme;
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
    }

    public int getThemeResId()
    {
        return mThemeResource;
    }

    @Override
    public void setTheme(int resourceId)
    {
        if (mThemeResource == resourceId) return;

        mThemeResource = resourceId;

        if (mTheme != null)
        {
            mTheme.release();
            mTheme = null;
        }
    }

    @Override
    public Resources.Theme getTheme()
    {
        if (mTheme != null) return mTheme;

        if (mThemeResource == 0) return getBaseContext().getTheme();

        mTheme = getResources().newTheme();

        Resources.Theme base = getBaseContext().getTheme();
        if (base != null) mTheme.setTo(base);

        onApplyThemeResource(mTheme, mThemeResource, true);
        return mTheme;
    }

    @Override
    public Object getSystemService(String name)
    {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name))
        {
            if (mInflater == null)
            {
                LayoutInflater base = (LayoutInflater)getBaseContext().getSystemService(name);
                mInflater = base != null ? base.cloneInContext(this) : new RuntimeLayoutInflater(this);
            }
            return mInflater;
        }
        return getBaseContext().getSystemService(name);
    }

    protected void onApplyThemeResource(Resources.Theme theme, int resourceId, boolean first)
    {
        theme.applyStyle(resourceId, true);
    }
}

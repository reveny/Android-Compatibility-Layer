package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;

public class ContextThemeWrapper extends ContextWrapper
{
    private int mThemeResource;
    private Resources.Theme mTheme;
    private LayoutInflater mInflater;
    private Configuration mOverrideConfiguration;
    private Resources mResources;
    private boolean mResourcesRequested;

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

    public void applyOverrideConfiguration(Configuration overrideConfiguration)
    {
        if (mResources != null || mResourcesRequested)
        {
            throw new IllegalStateException("getResources() or getAssets() has already been called");
        }
        if (mOverrideConfiguration != null) throw new IllegalStateException("Override configuration has already been set");

        mOverrideConfiguration = new Configuration(overrideConfiguration);
    }

    public Configuration getOverrideConfiguration()
    {
        return mOverrideConfiguration;
    }

    @Override
    public AssetManager getAssets()
    {
        return getResourcesInternal().getAssets();
    }

    @Override
    public Resources getResources()
    {
        return getResourcesInternal();
    }

    private Resources getResourcesInternal()
    {
        if (mResources != null) return mResources;

        if (mOverrideConfiguration == null)
        {
            Resources resources = super.getResources();
            if (resources != null) mResourcesRequested = true;
            return resources;
        }

        mResources = createConfigurationContext(mOverrideConfiguration).getResources();
        return mResources;
    }

    public int getThemeResId()
    {
        return mThemeResource;
    }

    public void setTheme(Resources.Theme theme)
    {
        mTheme = theme;
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

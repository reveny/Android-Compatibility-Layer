package android.content.res;

import android.util.DisplayMetrics;

public class ResourcesImpl
{
    final AssetManager mAssets;
    private final DisplayMetrics mMetrics;
    private final Configuration mConfiguration;

    public ResourcesImpl(AssetManager assets, DisplayMetrics metrics, Configuration configuration)
    {
        mAssets = assets;
        mMetrics = metrics == null ? new DisplayMetrics() : metrics;
        mConfiguration = configuration == null ? new Configuration() : configuration;
    }

    public AssetManager getAssets()
    {
        return mAssets;
    }

    DisplayMetrics getDisplayMetrics()
    {
        return mMetrics;
    }

    Configuration getConfiguration()
    {
        return mConfiguration;
    }

    public void updateConfiguration(Configuration configuration, DisplayMetrics metrics)
    {
        if (configuration != null) mConfiguration.setTo(configuration);
        if (metrics != null) mMetrics.setTo(metrics);
    }

    public void flushLayoutCache()
    {
    }
}

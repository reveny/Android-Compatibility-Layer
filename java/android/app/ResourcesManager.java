package android.app;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ResourcesManager
{
    private static final String TAG = "ResourcesManager";

    private static ResourcesManager sResourcesManager;

    private Resources mResources;
    private final List<String> mLibraryAssets = new ArrayList<>();

    public static ResourcesManager getInstance()
    {
        synchronized (ResourcesManager.class)
        {
            if (sResourcesManager == null) sResourcesManager = new ResourcesManager();
            return sResourcesManager;
        }
    }

    synchronized void attach(Resources resources)
    {
        mResources = resources;
    }

    public synchronized Configuration getConfiguration()
    {
        return mResources == null ? Resources.getSystem().getConfiguration() : mResources.getConfiguration();
    }

    public synchronized DisplayMetrics getDisplayMetrics()
    {
        return mResources == null ? Resources.getSystem().getDisplayMetrics() : mResources.getDisplayMetrics();
    }

    public synchronized boolean applyConfigurationToResources(Configuration configuration, Object compatibility)
    {
        if (configuration == null || mResources == null) return false;

        mResources.updateConfiguration(configuration, mResources.getDisplayMetrics());
        return true;
    }

    public void appendLibAssetForMainAssetPath(String assetPath, String libraryAsset)
    {
        appendLibAssetsForMainAssetPath(assetPath, new String[] {libraryAsset});
    }

    public synchronized void appendLibAssetsForMainAssetPath(String assetPath, String[] libraryAssets)
    {
        if (libraryAssets == null) return;

        for (String asset : libraryAssets)
        {
            if (asset == null || mLibraryAssets.contains(asset)) continue;

            mLibraryAssets.add(asset);
            Log.i(TAG, "shared library asset " + asset + " noted for " + assetPath + "; its resources are not loaded into this package");
        }
    }

    public synchronized void invalidatePath(String path)
    {
        mLibraryAssets.remove(path);
    }

    public boolean isSameResourcesOverrideConfig(android.os.IBinder activityToken, Configuration overrideConfiguration)
    {
        return overrideConfiguration == null || overrideConfiguration.equals(getConfiguration());
    }
}

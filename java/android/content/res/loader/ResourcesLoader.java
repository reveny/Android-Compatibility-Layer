package android.content.res.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourcesLoader
{
    private final Object mLock = new Object();
    private final ArrayList<ResourcesProvider> mProviders = new ArrayList<>();

    public ResourcesLoader()
    {
    }

    public List<ResourcesProvider> getProviders()
    {
        synchronized (mLock)
        {
            return Collections.unmodifiableList(new ArrayList<>(mProviders));
        }
    }

    public void addProvider(ResourcesProvider resourcesProvider)
    {
        synchronized (mLock)
        {
            if (resourcesProvider == null || mProviders.contains(resourcesProvider)) return;
            mProviders.add(resourcesProvider);
        }
    }

    public void removeProvider(ResourcesProvider resourcesProvider)
    {
        synchronized (mLock)
        {
            mProviders.remove(resourcesProvider);
        }
    }

    public void setProviders(List<ResourcesProvider> resourcesProviders)
    {
        synchronized (mLock)
        {
            mProviders.clear();
            if (resourcesProviders == null) return;

            for (ResourcesProvider provider : resourcesProviders)
            {
                if (provider != null && !mProviders.contains(provider)) mProviders.add(provider);
            }
        }
    }

    public void clearProviders()
    {
        synchronized (mLock)
        {
            mProviders.clear();
        }
    }
}

package android.webkit;

import android.content.Context;

public final class CookieSyncManager implements Runnable
{
    private static CookieSyncManager sInstance;

    private CookieSyncManager()
    {
    }

    public static synchronized CookieSyncManager getInstance()
    {
        if (sInstance == null)
        {
            throw new IllegalStateException("CookieSyncManager::createInstance() needs to be called before "
                + "CookieSyncManager::getInstance()");
        }
        return sInstance;
    }

    public static synchronized CookieSyncManager createInstance(Context context)
    {
        if (context == null) throw new IllegalArgumentException("Invalid context argument");
        if (sInstance == null) sInstance = new CookieSyncManager();
        return sInstance;
    }

    public void sync()
    {
        CookieManager.getInstance().flush();
    }

    public void resetSync()
    {
    }

    public void startSync()
    {
    }

    public void stopSync()
    {
    }

    @Override
    public void run()
    {
        sync();
    }

    protected void syncFromRamToFlash()
    {
        CookieManager.getInstance().flush();
    }
}

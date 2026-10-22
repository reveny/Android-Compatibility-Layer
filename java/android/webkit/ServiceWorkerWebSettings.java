package android.webkit;

public abstract class ServiceWorkerWebSettings
{
    public ServiceWorkerWebSettings()
    {
    }

    public abstract void setCacheMode(int mode);

    public abstract int getCacheMode();

    public abstract void setAllowContentAccess(boolean allow);

    public abstract boolean getAllowContentAccess();

    public abstract void setAllowFileAccess(boolean allow);

    public abstract boolean getAllowFileAccess();

    public abstract void setBlockNetworkLoads(boolean flag);

    public abstract boolean getBlockNetworkLoads();
}

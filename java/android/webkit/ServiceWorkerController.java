package android.webkit;

public abstract class ServiceWorkerController
{
    public ServiceWorkerController()
    {
    }

    public static ServiceWorkerController getInstance()
    {
        return LocalServiceWorkerController.INSTANCE;
    }

    public abstract ServiceWorkerWebSettings getServiceWorkerWebSettings();

    public abstract void setServiceWorkerClient(ServiceWorkerClient client);

    private static final class LocalServiceWorkerController extends ServiceWorkerController
    {
        static final LocalServiceWorkerController INSTANCE = new LocalServiceWorkerController();

        private final LocalServiceWorkerWebSettings mSettings = new LocalServiceWorkerWebSettings();
        private ServiceWorkerClient mClient;

        @Override
        public ServiceWorkerWebSettings getServiceWorkerWebSettings()
        {
            return mSettings;
        }

        @Override
        public synchronized void setServiceWorkerClient(ServiceWorkerClient client)
        {
            mClient = client;
        }
    }

    private static final class LocalServiceWorkerWebSettings extends ServiceWorkerWebSettings
    {
        private int mCacheMode = WebSettings.LOAD_DEFAULT;
        private boolean mAllowContentAccess = true;
        private boolean mAllowFileAccess;
        private boolean mBlockNetworkLoads;

        @Override
        public synchronized void setCacheMode(int mode)
        {
            mCacheMode = mode;
        }

        @Override
        public synchronized int getCacheMode()
        {
            return mCacheMode;
        }

        @Override
        public synchronized void setAllowContentAccess(boolean allow)
        {
            mAllowContentAccess = allow;
        }

        @Override
        public synchronized boolean getAllowContentAccess()
        {
            return mAllowContentAccess;
        }

        @Override
        public synchronized void setAllowFileAccess(boolean allow)
        {
            mAllowFileAccess = allow;
        }

        @Override
        public synchronized boolean getAllowFileAccess()
        {
            return mAllowFileAccess;
        }

        @Override
        public synchronized void setBlockNetworkLoads(boolean flag)
        {
            mBlockNetworkLoads = flag;
        }

        @Override
        public synchronized boolean getBlockNetworkLoads()
        {
            return mBlockNetworkLoads;
        }
    }
}

package android.net;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectivityManager
{
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE_MMS = 2;
    public static final int TYPE_MOBILE_SUPL = 3;
    public static final int TYPE_MOBILE_DUN = 4;
    public static final int TYPE_MOBILE_HIPRI = 5;
    public static final int TYPE_WIMAX = 6;
    public static final int TYPE_BLUETOOTH = 7;
    public static final int TYPE_DUMMY = 8;
    public static final int TYPE_ETHERNET = 9;
    public static final int TYPE_VPN = 17;

    private static final int MAX_NETWORK_TYPE = 18;

    public static final int DEFAULT_NETWORK_PREFERENCE = TYPE_WIFI;

    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED = "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";
    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";
    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED = "android.net.conn.RESTRICT_BACKGROUND_CHANGED";

    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";
    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";
    public static final String EXTRA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    public static final String EXTRA_REASON = "reason";

    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1;
    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 2;
    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 4;

    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

    private static Network sProcessDefaultNetwork;

    private final Map<NetworkCallback, Runnable> mCallbacks = new HashMap<>();
    private final List<OnNetworkActiveListener> mNetworkActiveListeners = new ArrayList<>();
    private Handler mMainHandler;

    public interface OnNetworkActiveListener
    {
        void onNetworkActive();
    }

    public NetworkInfo getActiveNetworkInfo()
    {
        return null;
    }

    public Network getActiveNetwork()
    {
        return null;
    }

    public Network[] getAllNetworks()
    {
        return new Network[0];
    }

    @Deprecated
    public NetworkInfo[] getAllNetworkInfo()
    {
        return new NetworkInfo[0];
    }

    @Deprecated
    public NetworkInfo getNetworkInfo(int networkType)
    {
        return null;
    }

    public NetworkInfo getNetworkInfo(Network network)
    {
        return null;
    }

    public NetworkCapabilities getNetworkCapabilities(Network network)
    {
        return null;
    }

    public LinkProperties getLinkProperties(Network network)
    {
        return null;
    }

    @Deprecated
    public static boolean isNetworkTypeValid(int networkType)
    {
        return networkType >= 0 && networkType <= MAX_NETWORK_TYPE;
    }

    @Deprecated
    public void setNetworkPreference(int preference)
    {
    }

    @Deprecated
    public int getNetworkPreference()
    {
        return DEFAULT_NETWORK_PREFERENCE;
    }

    @Deprecated
    public boolean getBackgroundDataSetting()
    {
        return true;
    }

    public ProxyInfo getDefaultProxy()
    {
        return null;
    }

    public boolean isActiveNetworkMetered()
    {
        return false;
    }

    public boolean isDefaultNetworkActive()
    {
        return false;
    }

    public void addDefaultNetworkActiveListener(OnNetworkActiveListener listener)
    {
        synchronized (mNetworkActiveListeners)
        {
            if (listener != null && !mNetworkActiveListeners.contains(listener)) mNetworkActiveListeners.add(listener);
        }
    }

    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener listener)
    {
        synchronized (mNetworkActiveListeners)
        {
            if (!mNetworkActiveListeners.remove(listener)) throw new IllegalArgumentException("Listener was not registered.");
        }
    }

    public int getRestrictBackgroundStatus()
    {
        return RESTRICT_BACKGROUND_STATUS_DISABLED;
    }

    public int getMultipathPreference(Network network)
    {
        return 0;
    }

    public int getConnectionOwnerUid(int protocol, InetSocketAddress local, InetSocketAddress remote)
    {
        return -1;
    }

    public byte[] getNetworkWatchlistConfigHash()
    {
        return null;
    }

    public void reportBadNetwork(Network network)
    {
    }

    public void reportNetworkConnectivity(Network network, boolean hasConnectivity)
    {
    }

    public boolean requestBandwidthUpdate(Network network)
    {
        return false;
    }

    public boolean bindProcessToNetwork(Network network)
    {
        if (network != null) return false;

        synchronized (ConnectivityManager.class)
        {
            sProcessDefaultNetwork = null;
        }

        return true;
    }

    public Network getBoundNetworkForProcess()
    {
        synchronized (ConnectivityManager.class)
        {
            return sProcessDefaultNetwork;
        }
    }

    @Deprecated
    public static boolean setProcessDefaultNetwork(Network network)
    {
        if (network != null) return false;

        synchronized (ConnectivityManager.class)
        {
            sProcessDefaultNetwork = null;
        }

        return true;
    }

    @Deprecated
    public static Network getProcessDefaultNetwork()
    {
        synchronized (ConnectivityManager.class)
        {
            return sProcessDefaultNetwork;
        }
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback)
    {
        registerCallback(networkCallback, null, 0, null);
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback, Handler handler)
    {
        registerCallback(networkCallback, handler, 0, null);
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback)
    {
        registerCallback(networkCallback, null, 0, request);
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback, Handler handler)
    {
        registerCallback(networkCallback, handler, 0, request);
    }

    public void registerNetworkCallback(NetworkRequest request, PendingIntent operation)
    {
        checkPendingIntent(operation);
    }

    public void registerBestMatchingNetworkCallback(NetworkRequest request, NetworkCallback networkCallback, Handler handler)
    {
        registerCallback(networkCallback, handler, 0, request);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback)
    {
        registerCallback(networkCallback, null, 0, request);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler)
    {
        registerCallback(networkCallback, handler, 0, request);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, int timeoutMs)
    {
        checkTimeout(timeoutMs);
        registerCallback(networkCallback, null, timeoutMs, request);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, Handler handler, int timeoutMs)
    {
        checkTimeout(timeoutMs);
        registerCallback(networkCallback, handler, timeoutMs, request);
    }

    public void requestNetwork(NetworkRequest request, PendingIntent operation)
    {
        checkPendingIntent(operation);
    }

    public void reserveNetwork(NetworkRequest request, Handler handler, NetworkCallback networkCallback)
    {
        registerCallback(networkCallback, handler, 0, request);
    }

    public void releaseNetworkRequest(PendingIntent operation)
    {
        checkPendingIntent(operation);
    }

    public void unregisterNetworkCallback(PendingIntent operation)
    {
        checkPendingIntent(operation);
    }

    public void unregisterNetworkCallback(NetworkCallback networkCallback)
    {
        if (networkCallback == null) throw new IllegalArgumentException("null NetworkCallback");

        Runnable timeout;
        synchronized (mCallbacks)
        {
            if (!mCallbacks.containsKey(networkCallback)) throw new IllegalArgumentException("NetworkCallback was not registered");

            timeout = mCallbacks.remove(networkCallback);
        }

        if (timeout != null) mainHandler().removeCallbacks(timeout);
    }

    private void registerCallback(NetworkCallback networkCallback, Handler handler, int timeoutMs, NetworkRequest request)
    {
        if (networkCallback == null) throw new IllegalArgumentException("null NetworkCallback");

        synchronized (mCallbacks)
        {
            if (mCallbacks.containsKey(networkCallback)) throw new IllegalArgumentException("NetworkCallback was already registered");

            Runnable timeout = null;
            if (timeoutMs > 0)
            {
                final Handler target = handler == null ? mainHandler() : handler;
                timeout = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        synchronized (mCallbacks)
                        {
                            if (mCallbacks.get(networkCallback) != this) return;
                            mCallbacks.remove(networkCallback);
                        }

                        networkCallback.onUnavailable();
                    }
                };

                target.postDelayed(timeout, timeoutMs);
            }

            mCallbacks.put(networkCallback, timeout);
        }
    }

    private Handler mainHandler()
    {
        synchronized (mCallbacks)
        {
            if (mMainHandler == null) mMainHandler = new Handler(Looper.getMainLooper());
            return mMainHandler;
        }
    }

    private static void checkTimeout(int timeoutMs)
    {
        if (timeoutMs <= 0) throw new IllegalArgumentException("timeoutMs must be strictly positive.");
    }

    private static void checkPendingIntent(PendingIntent intent)
    {
        if (intent == null) throw new IllegalArgumentException("PendingIntent cannot be null.");
    }

    public static class NetworkCallback
    {
        public static final int FLAG_INCLUDE_LOCATION_INFO = 1;

        public NetworkCallback()
        {
            this(0);
        }

        public NetworkCallback(int flags)
        {
            if ((flags & ~FLAG_INCLUDE_LOCATION_INFO) != 0) throw new IllegalArgumentException("Invalid flags");
        }

        public void onAvailable(Network network)
        {
        }

        public void onLosing(Network network, int maxMsToLive)
        {
        }

        public void onLost(Network network)
        {
        }

        public void onUnavailable()
        {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities)
        {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties)
        {
        }

        public void onBlockedStatusChanged(Network network, boolean blocked)
        {
        }

        public void onReserved(NetworkCapabilities networkCapabilities)
        {
        }
    }
}

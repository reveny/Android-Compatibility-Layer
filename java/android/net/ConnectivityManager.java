package android.net;

import android.os.Handler;

public class ConnectivityManager
{
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_ETHERNET = 9;
    public static final int TYPE_VPN = 17;

    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;

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

    public boolean isActiveNetworkMetered()
    {
        return false;
    }

    public int getRestrictBackgroundStatus()
    {
        return RESTRICT_BACKGROUND_STATUS_DISABLED;
    }

    public void registerDefaultNetworkCallback(NetworkCallback callback)
    {
        if (callback != null) callback.onUnavailable();
    }

    public void registerDefaultNetworkCallback(NetworkCallback callback, Handler handler)
    {
        registerDefaultNetworkCallback(callback);
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback)
    {
        if (callback != null) callback.onUnavailable();
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback callback, Handler handler)
    {
        registerNetworkCallback(request, callback);
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback callback)
    {
        if (callback != null) callback.onUnavailable();
    }

    public void unregisterNetworkCallback(NetworkCallback callback)
    {
    }

    public static class NetworkCallback
    {
        public void onAvailable(Network network)
        {
        }

        public void onLost(Network network)
        {
        }

        public void onUnavailable()
        {
        }

        public void onLosing(Network network, int maxMsToLive)
        {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities)
        {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties properties)
        {
        }

        public void onBlockedStatusChanged(Network network, boolean blocked)
        {
        }
    }
}

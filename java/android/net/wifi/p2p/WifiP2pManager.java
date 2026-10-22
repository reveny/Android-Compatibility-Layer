package android.net.wifi.p2p;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class WifiP2pManager
{
    public static final String ACTION_WIFI_P2P_LISTEN_STATE_CHANGED = "android.net.wifi.p2p.action.WIFI_P2P_LISTEN_STATE_CHANGED";
    public static final String ACTION_WIFI_P2P_REQUEST_RESPONSE_CHANGED = "android.net.wifi.p2p.action.WIFI_P2P_REQUEST_RESPONSE_CHANGED";
    public static final int ERROR = 0;
    public static final int P2P_UNSUPPORTED = 1;
    public static final int BUSY = 2;
    public static final int NO_SERVICE_REQUESTS = 3;
    public static final int CONNECTION_REQUEST_ACCEPT = 0;
    public static final int CONNECTION_REQUEST_REJECT = 1;
    public static final int CONNECTION_REQUEST_DEFER_TO_SERVICE = 2;
    public static final int CONNECTION_REQUEST_DEFER_SHOW_PIN_TO_SERVICE = 3;
    public static final String EXTRA_DISCOVERY_STATE = "discoveryState";
    public static final String EXTRA_LISTEN_STATE = "android.net.wifi.p2p.extra.LISTEN_STATE";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_P2P_DEVICE_LIST = "wifiP2pDeviceList";
    public static final String EXTRA_REQUEST_CONFIG = "android.net.wifi.p2p.extra.REQUEST_CONFIG";
    public static final String EXTRA_REQUEST_RESPONSE = "android.net.wifi.p2p.extra.REQUEST_RESPONSE";
    public static final String EXTRA_WIFI_P2P_DEVICE = "wifiP2pDevice";
    public static final String EXTRA_WIFI_P2P_GROUP = "p2pGroupInfo";
    public static final String EXTRA_WIFI_P2P_INFO = "wifiP2pInfo";
    public static final String EXTRA_WIFI_STATE = "wifi_p2p_state";
    public static final String WIFI_P2P_CONNECTION_CHANGED_ACTION = "android.net.wifi.p2p.CONNECTION_STATE_CHANGE";
    public static final String WIFI_P2P_DISCOVERY_CHANGED_ACTION = "android.net.wifi.p2p.DISCOVERY_STATE_CHANGE";
    public static final int WIFI_P2P_DISCOVERY_STOPPED = 1;
    public static final int WIFI_P2P_DISCOVERY_STARTED = 2;
    public static final int WIFI_P2P_LISTEN_STOPPED = 1;
    public static final int WIFI_P2P_LISTEN_STARTED = 2;
    public static final String WIFI_P2P_PEERS_CHANGED_ACTION = "android.net.wifi.p2p.PEERS_CHANGED";
    public static final String WIFI_P2P_STATE_CHANGED_ACTION = "android.net.wifi.p2p.STATE_CHANGED";
    public static final int WIFI_P2P_STATE_DISABLED = 1;
    public static final int WIFI_P2P_STATE_ENABLED = 2;
    public static final String WIFI_P2P_THIS_DEVICE_CHANGED_ACTION = "android.net.wifi.p2p.THIS_DEVICE_CHANGED";

    private final Context mContext;

    public WifiP2pManager(Context context)
    {
        mContext = context;
    }

    public interface ChannelListener
    {
        void onChannelDisconnected();
    }

    public interface ActionListener
    {
        void onSuccess();

        void onFailure(int reason);
    }

    public interface GroupInfoListener
    {
        void onGroupInfoAvailable(WifiP2pGroup group);
    }

    public static class Channel implements AutoCloseable
    {
        private final Handler mHandler;
        private final ChannelListener mChannelListener;

        Channel(Looper looper, ChannelListener channelListener)
        {
            mHandler = new Handler(looper);
            mChannelListener = channelListener;
        }

        @Override
        public void close()
        {
        }

        void post(Runnable runnable)
        {
            mHandler.post(runnable);
        }
    }

    public Channel initialize(Context sourceContext, Looper sourceLooper, ChannelListener listener)
    {
        if (sourceLooper == null) throw new IllegalArgumentException("the looper must not be null");

        return new Channel(sourceLooper, listener);
    }

    public void discoverPeers(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void discoverPeersOnSocialChannels(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void discoverPeersOnSpecificFrequency(Channel channel, int frequencyMhz, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void stopPeerDiscovery(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void cancelConnect(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void createGroup(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void removeGroup(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void startListening(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void stopListening(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void clearLocalServices(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void discoverServices(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void clearServiceRequests(Channel channel, ActionListener listener)
    {
        unsupported(channel, listener);
    }

    public void requestGroupInfo(Channel channel, final GroupInfoListener listener)
    {
        if (channel == null) throw new IllegalArgumentException("Channel needs to be initialized");
        if (listener == null) return;

        channel.post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onGroupInfoAvailable(null);
            }
        });
    }

    public boolean isSetVendorElementsSupported()
    {
        return false;
    }

    public boolean isChannelConstrainedDiscoverySupported()
    {
        return false;
    }

    public boolean isGroupClientRemovalSupported()
    {
        return false;
    }

    public boolean isGroupOwnerIPv6LinkLocalAddressProvided()
    {
        return false;
    }

    public void getListenState(Channel channel, Executor executor, final Consumer<Integer> resultsCallback)
    {
        if (channel == null) throw new IllegalArgumentException("Channel needs to be initialized");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                resultsCallback.accept(WIFI_P2P_LISTEN_STOPPED);
            }
        });
    }

    public static int getP2pMaxAllowedVendorElementsLengthBytes()
    {
        return 0;
    }

    private static void unsupported(Channel channel, final ActionListener listener)
    {
        if (channel == null) throw new IllegalArgumentException("Channel needs to be initialized");
        if (listener == null) return;

        channel.post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onFailure(P2P_UNSUPPORTED);
            }
        });
    }
}

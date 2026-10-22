package android.net.wifi;

import android.content.Context;
import android.net.DhcpInfo;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class WifiManager
{
    public static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
    public static final int ACTION_REMOVE_SUGGESTION_DISCONNECT = 2;
    public static final int ACTION_REMOVE_SUGGESTION_LINGER = 1;
    public static final String ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE = "android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE";
    public static final String ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION =
        "android.net.wifi.action.WIFI_NETWORK_SUGGESTION_POST_CONNECTION";
    public static final String ACTION_WIFI_SCAN_AVAILABILITY_CHANGED = "android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED";
    public static final String CHANNEL_DATA_KEY_FREQUENCY_MHZ = "CHANNEL_DATA_KEY_FREQUENCY_MHZ";
    public static final String CHANNEL_DATA_KEY_NUM_AP = "CHANNEL_DATA_KEY_NUM_AP";
    public static final int ERROR_AUTHENTICATING = 1;
    public static final String EXTRA_BSSID = "bssid";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NETWORK_SUGGESTION = "android.net.wifi.extra.NETWORK_SUGGESTION";
    public static final String EXTRA_NEW_RSSI = "newRssi";
    public static final String EXTRA_NEW_STATE = "newState";
    public static final String EXTRA_PREVIOUS_WIFI_STATE = "previous_wifi_state";
    public static final String EXTRA_RESULTS_UPDATED = "resultsUpdated";
    public static final String EXTRA_SCAN_AVAILABLE = "android.net.wifi.extra.SCAN_AVAILABLE";
    public static final String EXTRA_SUPPLICANT_CONNECTED = "connected";
    public static final String EXTRA_SUPPLICANT_ERROR = "supplicantError";
    public static final String EXTRA_WIFI_INFO = "wifiInfo";
    public static final String EXTRA_WIFI_STATE = "wifi_state";
    public static final String NETWORK_IDS_CHANGED_ACTION = "android.net.wifi.NETWORK_IDS_CHANGED";
    public static final String NETWORK_STATE_CHANGED_ACTION = "android.net.wifi.STATE_CHANGE";
    public static final String RSSI_CHANGED_ACTION = "android.net.wifi.RSSI_CHANGED";
    public static final String SCAN_RESULTS_AVAILABLE_ACTION = "android.net.wifi.SCAN_RESULTS";
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_UNKNOWN = 0;
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_ASSOCIATION = 1;
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION = 2;
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_IP_PROVISIONING = 3;
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NOT_FOUND = 4;
    public static final int STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NO_RESPONSE = 5;
    public static final int STATUS_NETWORK_SUGGESTIONS_SUCCESS = 0;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL = 1;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED = 2;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE = 3;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP = 4;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID = 5;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED = 6;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID = 7;
    public static final int STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN = 8;
    public static final int STATUS_SUGGESTION_APPROVAL_UNKNOWN = 0;
    public static final int STATUS_SUGGESTION_APPROVAL_PENDING = 1;
    public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_USER = 2;
    public static final int STATUS_SUGGESTION_APPROVAL_REJECTED_BY_USER = 3;
    public static final int STATUS_SUGGESTION_APPROVAL_APPROVED_BY_CARRIER_PRIVILEGE = 4;
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_UNKNOWN = 0;
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_ASSOCIATION = 1;
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_AUTHENTICATION = 2;
    public static final int STATUS_SUGGESTION_CONNECTION_FAILURE_IP_PROVISIONING = 3;
    public static final String SUPPLICANT_CONNECTION_CHANGE_ACTION = "android.net.wifi.supplicant.CONNECTION_CHANGE";
    public static final String SUPPLICANT_STATE_CHANGED_ACTION = "android.net.wifi.supplicant.STATE_CHANGE";
    public static final String UNKNOWN_SSID = "<unknown ssid>";
    public static final int WIFI_INTERFACE_TYPE_STA = 0;
    public static final int WIFI_INTERFACE_TYPE_AP = 1;
    public static final int WIFI_INTERFACE_TYPE_AWARE = 2;
    public static final int WIFI_INTERFACE_TYPE_DIRECT = 3;
    public static final int WIFI_MODE_FULL = 1;
    public static final int WIFI_MODE_SCAN_ONLY = 2;
    public static final int WIFI_MODE_FULL_HIGH_PERF = 3;
    public static final int WIFI_MODE_FULL_LOW_LATENCY = 4;
    public static final int WIFI_MULTI_INTERNET_MODE_DISABLED = 0;
    public static final int WIFI_MULTI_INTERNET_MODE_DBS_AP = 1;
    public static final int WIFI_MULTI_INTERNET_MODE_MULTI_AP = 2;
    public static final String WIFI_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_STATE_CHANGED";
    public static final int WIFI_STATE_DISABLING = 0;
    public static final int WIFI_STATE_DISABLED = 1;
    public static final int WIFI_STATE_ENABLING = 2;
    public static final int WIFI_STATE_ENABLED = 3;
    public static final int WIFI_STATE_UNKNOWN = 4;
    public static final int WPS_OVERLAP_ERROR = 3;
    public static final int WPS_WEP_PROHIBITED = 4;
    public static final int WPS_TKIP_ONLY_PROHIBITED = 5;
    public static final int WPS_AUTH_FAILURE = 6;
    public static final int WPS_TIMED_OUT = 7;

    private static final String TAG = "WifiManager";
    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;
    private static final int MAX_SIGNAL_LEVEL = 4;

    private final Context mContext;

    public WifiManager(Context context)
    {
        mContext = context;
    }

    public boolean removeNetwork(int netId)
    {
        return false;
    }

    public boolean removeNonCallerConfiguredNetworks()
    {
        return false;
    }

    public boolean enableNetwork(int netId, boolean attemptConnect)
    {
        return false;
    }

    public boolean disableNetwork(int netId)
    {
        return false;
    }

    public boolean disconnect()
    {
        return false;
    }

    public boolean reconnect()
    {
        return false;
    }

    public boolean reassociate()
    {
        return false;
    }

    public boolean pingSupplicant()
    {
        return false;
    }

    public boolean isP2pSupported()
    {
        return false;
    }

    public boolean isStaApConcurrencySupported()
    {
        return false;
    }

    public boolean isStaConcurrencyForLocalOnlyConnectionsSupported()
    {
        return false;
    }

    public boolean isMakeBeforeBreakWifiSwitchingSupported()
    {
        return false;
    }

    public boolean isStaConcurrencyForMultiInternetSupported()
    {
        return false;
    }

    public boolean isDeviceToApRttSupported()
    {
        return false;
    }

    public boolean isPreferredNetworkOffloadSupported()
    {
        return false;
    }

    public boolean isTdlsSupported()
    {
        return false;
    }

    public boolean isEnhancedPowerReportingSupported()
    {
        return false;
    }

    public boolean is24GHzBandSupported()
    {
        return false;
    }

    public boolean is5GHzBandSupported()
    {
        return false;
    }

    public boolean is60GHzBandSupported()
    {
        return false;
    }

    public boolean is6GHzBandSupported()
    {
        return false;
    }

    public boolean isWifiStandardSupported(int standard)
    {
        return false;
    }

    public boolean isStaBridgedApConcurrencySupported()
    {
        return false;
    }

    public boolean isBridgedApConcurrencySupported()
    {
        return false;
    }

    @Deprecated
    public boolean startScan()
    {
        return false;
    }

    public WifiInfo getConnectionInfo()
    {
        return new WifiInfo();
    }

    public List<ScanResult> getScanResults()
    {
        return new ArrayList<>();
    }

    public boolean isScanAlwaysAvailable()
    {
        return false;
    }

    public void getChannelData(Executor executor, final Consumer<List<Bundle>> resultsCallback)
    {
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                resultsCallback.accept(new ArrayList<Bundle>());
            }
        });
    }

    @Deprecated
    public boolean saveConfiguration()
    {
        return false;
    }

    public DhcpInfo getDhcpInfo()
    {
        return new DhcpInfo();
    }

    @Deprecated
    public boolean setWifiEnabled(boolean enabled)
    {
        Log.i(TAG, "this device has no Wi-Fi hardware, so Wi-Fi cannot be " + (enabled ? "enabled" : "disabled"));
        return false;
    }

    public int getWifiState()
    {
        return WIFI_STATE_DISABLED;
    }

    public boolean isWifiEnabled()
    {
        return false;
    }

    @Deprecated
    public static int calculateSignalLevel(int rssi, int numLevels)
    {
        if (rssi <= MIN_RSSI) return 0;
        if (rssi >= MAX_RSSI) return numLevels - 1;

        float inputRange = MAX_RSSI - MIN_RSSI;
        float outputRange = numLevels - 1;
        return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
    }

    public int calculateSignalLevel(int rssi)
    {
        return calculateSignalLevel(rssi, MAX_SIGNAL_LEVEL + 1);
    }

    public int getMaxSignalLevel()
    {
        return MAX_SIGNAL_LEVEL;
    }

    public static int compareSignalLevel(int rssiA, int rssiB)
    {
        return rssiA - rssiB;
    }

    public void setTdlsEnabled(InetAddress remoteIPAddress, boolean enable)
    {
    }

    public void setTdlsEnabled(InetAddress remoteIPAddress, boolean enable, Executor executor, Consumer<Boolean> resultsCallback)
    {
        deliver(executor, resultsCallback, Boolean.FALSE);
    }

    public void setTdlsEnabledWithMacAddress(String remoteMacAddress, boolean enable)
    {
    }

    public void setTdlsEnabledWithMacAddress(String remoteMacAddress, boolean enable, Executor executor,
                                             Consumer<Boolean> resultsCallback)
    {
        deliver(executor, resultsCallback, Boolean.FALSE);
    }

    public void isTdlsOperationCurrentlyAvailable(Executor executor, Consumer<Boolean> resultsCallback)
    {
        deliver(executor, resultsCallback, Boolean.FALSE);
    }

    public void getMaxSupportedConcurrentTdlsSessions(Executor executor, Consumer<Integer> resultsCallback)
    {
        deliver(executor, resultsCallback, 0);
    }

    public void getNumberOfEnabledTdlsSessions(Executor executor, Consumer<Integer> resultsCallback)
    {
        deliver(executor, resultsCallback, 0);
    }

    public void allowAutojoinGlobal(boolean allowAutojoin)
    {
    }

    public void queryAutojoinGlobal(Executor executor, Consumer<Boolean> resultsCallback)
    {
        deliver(executor, resultsCallback, Boolean.FALSE);
    }

    private static <T> void deliver(Executor executor, final Consumer<T> callback, final T value)
    {
        if (executor == null || callback == null) throw new IllegalArgumentException("executor and callback must not be null");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.accept(value);
            }
        });
    }

    public WifiLock createWifiLock(int lockType, String tag)
    {
        return new WifiLock(lockType, tag);
    }

    @Deprecated
    public WifiLock createWifiLock(String tag)
    {
        return new WifiLock(WIFI_MODE_FULL, tag);
    }

    public MulticastLock createMulticastLock(String tag)
    {
        return new MulticastLock(tag);
    }

    public boolean isWpa3SaeSupported()
    {
        return false;
    }

    public boolean isWpa3SuiteBSupported()
    {
        return false;
    }

    public boolean isEnhancedOpenSupported()
    {
        return false;
    }

    public boolean isEasyConnectSupported()
    {
        return false;
    }

    public boolean isEasyConnectEnrolleeResponderModeSupported()
    {
        return false;
    }

    public boolean isWapiSupported()
    {
        return false;
    }

    public boolean isWpa3SaePublicKeySupported()
    {
        return false;
    }

    public boolean isPasspointTermsAndConditionsSupported()
    {
        return false;
    }

    public boolean isWpa3SaeH2eSupported()
    {
        return false;
    }

    public boolean isWifiDisplayR2Supported()
    {
        return false;
    }

    public boolean isDecoratedIdentitySupported()
    {
        return false;
    }

    public boolean isTrustOnFirstUseSupported()
    {
        return false;
    }

    public boolean isEasyConnectDppAkmSupported()
    {
        return false;
    }

    public boolean isTlsMinimumVersionSupported()
    {
        return false;
    }

    public boolean isTlsV13Supported()
    {
        return false;
    }

    public boolean isDualBandSimultaneousSupported()
    {
        return false;
    }

    public boolean isTidToLinkMappingNegotiationSupported()
    {
        return false;
    }

    public boolean isScanThrottleEnabled()
    {
        return true;
    }

    public boolean isAutoWakeupEnabled()
    {
        return false;
    }

    public boolean isCarrierNetworkOffloadEnabled(int subscriptionId, boolean merged)
    {
        return false;
    }

    public void flushPasspointAnqpCache()
    {
    }

    public boolean isWifiPasspointEnabled()
    {
        return false;
    }

    public int getStaConcurrencyForMultiInternetMode()
    {
        return WIFI_MULTI_INTERNET_MODE_DISABLED;
    }

    public int getMaxNumberOfChannelsPerNetworkSpecifierRequest()
    {
        return 0;
    }

    public class WifiLock
    {
        private final String mTag;
        private final int mLockType;
        private int mRefCount;
        private boolean mRefCounted = true;
        private boolean mHeld;
        private WorkSource mWorkSource;

        private WifiLock(int lockType, String tag)
        {
            mTag = tag;
            mLockType = lockType;
        }

        public void acquire()
        {
            synchronized (this)
            {
                if (mRefCounted ? (++mRefCount == 1) : (!mHeld)) mHeld = true;
            }
        }

        public void release()
        {
            synchronized (this)
            {
                if (mRefCounted ? (--mRefCount == 0) : (mHeld))
                {
                    mHeld = false;
                }

                if (mRefCount < 0) throw new RuntimeException("WifiLock under-locked " + mTag);
            }
        }

        public void setReferenceCounted(boolean refCounted)
        {
            mRefCounted = refCounted;
        }

        public boolean isHeld()
        {
            synchronized (this)
            {
                return mHeld;
            }
        }

        public void setWorkSource(WorkSource workSource)
        {
            synchronized (this)
            {
                mWorkSource = workSource;
            }
        }

        @Override
        public String toString()
        {
            synchronized (this)
            {
                return "WifiLock{ " + mTag + " type=" + mLockType + " refcounted=" + mRefCounted + " held=" + mHeld + " refcount="
                    + mRefCount + " }";
            }
        }
    }

    public class MulticastLock
    {
        private final String mTag;
        private int mRefCount;
        private boolean mRefCounted = true;
        private boolean mHeld;

        private MulticastLock(String tag)
        {
            mTag = tag;
        }

        public void acquire()
        {
            synchronized (this)
            {
                if (mRefCounted ? (++mRefCount == 1) : (!mHeld)) mHeld = true;
            }
        }

        public void release()
        {
            synchronized (this)
            {
                if (mRefCounted ? (--mRefCount == 0) : (mHeld))
                {
                    mHeld = false;
                }

                if (mRefCount < 0) throw new RuntimeException("MulticastLock under-locked " + mTag);
            }
        }

        public void setReferenceCounted(boolean refCounted)
        {
            mRefCounted = refCounted;
        }

        public boolean isHeld()
        {
            synchronized (this)
            {
                return mHeld;
            }
        }

        @Override
        public String toString()
        {
            synchronized (this)
            {
                return "MulticastLock{ " + mTag + "; refcounted=" + mRefCounted + "; held=" + mHeld + "; refcount=" + mRefCount + " }";
            }
        }
    }
}

package android.net.wifi;

import android.net.NetworkInfo;
import android.net.TransportInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WifiInfo implements TransportInfo, Parcelable
{
    public static final String FREQUENCY_UNITS = "MHz";
    public static final String LINK_SPEED_UNITS = "Mbps";
    public static final int LINK_SPEED_UNKNOWN = -1;
    public static final int SECURITY_TYPE_UNKNOWN = -1;
    public static final int SECURITY_TYPE_OPEN = 0;
    public static final int SECURITY_TYPE_WEP = 1;
    public static final int SECURITY_TYPE_PSK = 2;
    public static final int SECURITY_TYPE_EAP = 3;
    public static final int SECURITY_TYPE_SAE = 4;
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT = 5;
    public static final int SECURITY_TYPE_OWE = 6;
    public static final int SECURITY_TYPE_WAPI_PSK = 7;
    public static final int SECURITY_TYPE_WAPI_CERT = 8;
    public static final int SECURITY_TYPE_EAP_WPA3_ENTERPRISE = 9;
    public static final int SECURITY_TYPE_OSEN = 10;
    public static final int SECURITY_TYPE_PASSPOINT_R1_R2 = 11;
    public static final int SECURITY_TYPE_PASSPOINT_R3 = 12;
    public static final int SECURITY_TYPE_DPP = 13;

    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final int INVALID_RSSI = -127;

    private SupplicantState mSupplicantState = SupplicantState.DISCONNECTED;
    private int mRssi = INVALID_RSSI;
    private int mNetworkId = -1;

    WifiInfo()
    {
    }

    public String getSSID()
    {
        return WifiManager.UNKNOWN_SSID;
    }

    public String getBSSID()
    {
        return null;
    }

    public int getApMloLinkId()
    {
        return 0;
    }

    public int getRssi()
    {
        return mRssi;
    }

    public int getWifiStandard()
    {
        return ScanResult.WIFI_STANDARD_UNKNOWN;
    }

    public int getLinkSpeed()
    {
        return LINK_SPEED_UNKNOWN;
    }

    public int getTxLinkSpeedMbps()
    {
        return LINK_SPEED_UNKNOWN;
    }

    public int getMaxSupportedTxLinkSpeedMbps()
    {
        return LINK_SPEED_UNKNOWN;
    }

    public int getRxLinkSpeedMbps()
    {
        return LINK_SPEED_UNKNOWN;
    }

    public int getMaxSupportedRxLinkSpeedMbps()
    {
        return LINK_SPEED_UNKNOWN;
    }

    public int getFrequency()
    {
        return -1;
    }

    public String getMacAddress()
    {
        return DEFAULT_MAC_ADDRESS;
    }

    public boolean isRestricted()
    {
        return false;
    }

    public String getPasspointFqdn()
    {
        return null;
    }

    public String getPasspointProviderFriendlyName()
    {
        return null;
    }

    public int getSubscriptionId()
    {
        return -1;
    }

    public int getNetworkId()
    {
        return mNetworkId;
    }

    public SupplicantState getSupplicantState()
    {
        return mSupplicantState;
    }

    public int getIpAddress()
    {
        return 0;
    }

    public boolean getHiddenSSID()
    {
        return false;
    }

    public static NetworkInfo.DetailedState getDetailedStateOf(SupplicantState supplicantState)
    {
        switch (supplicantState)
        {
            case SCANNING:
                return NetworkInfo.DetailedState.SCANNING;
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                return NetworkInfo.DetailedState.CONNECTING;
            case COMPLETED:
                return NetworkInfo.DetailedState.OBTAINING_IPADDR;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case DORMANT:
                return NetworkInfo.DetailedState.DISCONNECTED;
            case INACTIVE:
            case UNINITIALIZED:
                return NetworkInfo.DetailedState.IDLE;
            case INVALID:
                return NetworkInfo.DetailedState.FAILED;
            default:
                return null;
        }
    }

    @Override
    public String toString()
    {
        return "SSID: " + getSSID() + ", BSSID: <none>, MAC: " + getMacAddress() + ", Supplicant state: " + mSupplicantState + ", RSSI: "
            + mRssi + ", Link speed: " + LINK_SPEED_UNKNOWN + LINK_SPEED_UNITS + ", Frequency: -1" + FREQUENCY_UNITS + ", Net ID: "
            + mNetworkId;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mSupplicantState.name());
        dest.writeInt(mRssi);
        dest.writeInt(mNetworkId);
    }

    public List<ScanResult.InformationElement> getInformationElements()
    {
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof WifiInfo)) return false;

        WifiInfo info = (WifiInfo)other;
        return mSupplicantState == info.mSupplicantState && mRssi == info.mRssi && mNetworkId == info.mNetworkId;
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * mSupplicantState.hashCode() + mRssi) + mNetworkId;
    }

    @Override
    public WifiInfo makeCopy(long redactions)
    {
        WifiInfo copy = new WifiInfo();
        copy.mSupplicantState = mSupplicantState;
        copy.mRssi = mRssi;
        copy.mNetworkId = mNetworkId;
        return copy;
    }

    @Override
    public long getApplicableRedactions()
    {
        return 0;
    }

    public int getCurrentSecurityType()
    {
        return SECURITY_TYPE_UNKNOWN;
    }

    public static final Creator<WifiInfo> CREATOR = new Creator<WifiInfo>()
    {
        @Override
        public WifiInfo createFromParcel(Parcel source)
        {
            WifiInfo info = new WifiInfo();
            info.mSupplicantState = SupplicantState.valueOf(source.readString());
            info.mRssi = source.readInt();
            info.mNetworkId = source.readInt();
            return info;
        }

        @Override
        public WifiInfo[] newArray(int size)
        {
            return new WifiInfo[size];
        }
    };
}

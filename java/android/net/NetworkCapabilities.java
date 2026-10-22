package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class NetworkCapabilities implements Parcelable
{
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI_AWARE = 5;
    public static final int TRANSPORT_LOWPAN = 6;
    public static final int TRANSPORT_USB = 8;
    public static final int TRANSPORT_THREAD = 9;
    public static final int TRANSPORT_SATELLITE = 10;

    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_XCAP = 9;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_NOT_VPN = 15;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
    public static final int NET_CAPABILITY_NOT_ROAMING = 18;
    public static final int NET_CAPABILITY_FOREGROUND = 19;
    public static final int NET_CAPABILITY_NOT_CONGESTED = 20;
    public static final int NET_CAPABILITY_NOT_SUSPENDED = 21;
    public static final int NET_CAPABILITY_MCX = 23;
    public static final int NET_CAPABILITY_TEMPORARILY_NOT_METERED = 25;
    public static final int NET_CAPABILITY_ENTERPRISE = 29;
    public static final int NET_CAPABILITY_HEAD_UNIT = 32;
    public static final int NET_CAPABILITY_MMTEL = 33;
    public static final int NET_CAPABILITY_PRIORITIZE_LATENCY = 34;
    public static final int NET_CAPABILITY_PRIORITIZE_BANDWIDTH = 35;
    public static final int NET_CAPABILITY_LOCAL_NETWORK = 36;
    public static final int NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED = 37;

    public static final int NET_ENTERPRISE_ID_1 = 1;
    public static final int NET_ENTERPRISE_ID_2 = 2;
    public static final int NET_ENTERPRISE_ID_3 = 3;
    public static final int NET_ENTERPRISE_ID_4 = 4;
    public static final int NET_ENTERPRISE_ID_5 = 5;

    public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;

    private static final int MAX_CAPABILITY = NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED;
    private static final int MAX_TRANSPORT = TRANSPORT_SATELLITE;

    private static final long DEFAULT_CAPABILITIES = (1L << NET_CAPABILITY_NOT_RESTRICTED) | (1L << NET_CAPABILITY_TRUSTED)
        | (1L << NET_CAPABILITY_NOT_VPN);

    private long mTransports;
    private long mCapabilities;
    private int mLinkUpBandwidthKbps;
    private int mLinkDownBandwidthKbps;
    private int mSignalStrength = SIGNAL_STRENGTH_UNSPECIFIED;
    private int mOwnerUid = -1;
    private NetworkSpecifier mNetworkSpecifier;
    private TransportInfo mTransportInfo;
    private Set<Integer> mSubscriptionIds = new HashSet<>();
    private int[] mEnterpriseIds = new int[0];

    public NetworkCapabilities()
    {
        mCapabilities = DEFAULT_CAPABILITIES;
    }

    public NetworkCapabilities(NetworkCapabilities source)
    {
        if (source == null) return;

        mTransports = source.mTransports;
        mCapabilities = source.mCapabilities;
        mLinkUpBandwidthKbps = source.mLinkUpBandwidthKbps;
        mLinkDownBandwidthKbps = source.mLinkDownBandwidthKbps;
        mSignalStrength = source.mSignalStrength;
        mOwnerUid = source.mOwnerUid;
        mNetworkSpecifier = source.mNetworkSpecifier;
        mTransportInfo = source.mTransportInfo;
        mSubscriptionIds = new HashSet<>(source.mSubscriptionIds);
        mEnterpriseIds = source.mEnterpriseIds.clone();
    }

    NetworkCapabilities(int transports, int capabilities)
    {
        mTransports = transports & 0xffffffffL;
        mCapabilities = capabilities & 0xffffffffL;
    }

    public boolean hasTransport(int transport)
    {
        return transport >= 0 && transport <= MAX_TRANSPORT && (mTransports & (1L << transport)) != 0;
    }

    public boolean hasCapability(int capability)
    {
        return capability >= 0 && capability <= MAX_CAPABILITY && (mCapabilities & (1L << capability)) != 0;
    }

    public int[] getCapabilities()
    {
        return bitsOf(mCapabilities);
    }

    int[] getTransportTypes()
    {
        return bitsOf(mTransports);
    }

    public boolean hasEnterpriseId(int enterpriseId)
    {
        for (int candidate : mEnterpriseIds)
        {
            if (candidate == enterpriseId) return true;
        }

        return false;
    }

    public int[] getEnterpriseIds()
    {
        return mEnterpriseIds.clone();
    }

    public int getLinkDownstreamBandwidthKbps()
    {
        return mLinkDownBandwidthKbps;
    }

    public int getLinkUpstreamBandwidthKbps()
    {
        return mLinkUpBandwidthKbps;
    }

    public NetworkSpecifier getNetworkSpecifier()
    {
        return mNetworkSpecifier;
    }

    public TransportInfo getTransportInfo()
    {
        return mTransportInfo;
    }

    public int getSignalStrength()
    {
        return mSignalStrength;
    }

    public int getOwnerUid()
    {
        return mOwnerUid;
    }

    public Set<Integer> getSubscriptionIds()
    {
        return Collections.unmodifiableSet(new HashSet<>(mSubscriptionIds));
    }

    NetworkCapabilities addTransportType(int transport)
    {
        checkTransport(transport);
        mTransports |= 1L << transport;
        return this;
    }

    NetworkCapabilities removeTransportType(int transport)
    {
        checkTransport(transport);
        mTransports &= ~(1L << transport);
        return this;
    }

    NetworkCapabilities addCapability(int capability)
    {
        checkCapability(capability);
        mCapabilities |= 1L << capability;
        return this;
    }

    NetworkCapabilities removeCapability(int capability)
    {
        checkCapability(capability);
        mCapabilities &= ~(1L << capability);
        return this;
    }

    void clearCapabilities()
    {
        mCapabilities = 0;
    }

    void setNetworkSpecifier(NetworkSpecifier networkSpecifier)
    {
        mNetworkSpecifier = networkSpecifier;
    }

    void setSubscriptionIds(Set<Integer> subscriptionIds)
    {
        mSubscriptionIds = new HashSet<>(subscriptionIds);
    }

    boolean satisfiedByNetworkCapabilities(NetworkCapabilities other)
    {
        if (other == null) return false;
        if ((other.mCapabilities & mCapabilities) != mCapabilities) return false;
        if (mTransports != 0 && (other.mTransports & mTransports) == 0) return false;
        if (mSignalStrength != SIGNAL_STRENGTH_UNSPECIFIED && other.mSignalStrength < mSignalStrength) return false;
        if (!mSubscriptionIds.isEmpty() && !other.mSubscriptionIds.containsAll(mSubscriptionIds)) return false;

        if (mNetworkSpecifier != null)
        {
            if (other.mNetworkSpecifier == null) return false;
            if (!mNetworkSpecifier.equals(other.mNetworkSpecifier) && !other.mNetworkSpecifier.canBeSatisfiedBy(mNetworkSpecifier))
            {
                return false;
            }
        }

        return true;
    }

    private static void checkTransport(int transport)
    {
        if (transport < 0 || transport > MAX_TRANSPORT) throw new IllegalArgumentException("Invalid TransportType " + transport);
    }

    private static void checkCapability(int capability)
    {
        if (capability < 0 || capability > MAX_CAPABILITY)
        {
            throw new IllegalArgumentException("NetworkCapability " + capability + " out of range");
        }
    }

    private static int[] bitsOf(long mask)
    {
        int[] bits = new int[Long.bitCount(mask)];
        int count = 0;

        for (int bit = 0; bit < 64; bit++)
        {
            if ((mask & (1L << bit)) != 0) bits[count++] = bit;
        }

        return bits;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof NetworkCapabilities)) return false;

        NetworkCapabilities that = (NetworkCapabilities)other;
        return mTransports == that.mTransports && mCapabilities == that.mCapabilities
            && mLinkUpBandwidthKbps == that.mLinkUpBandwidthKbps && mLinkDownBandwidthKbps == that.mLinkDownBandwidthKbps
            && mSignalStrength == that.mSignalStrength && mOwnerUid == that.mOwnerUid
            && Objects.equals(mNetworkSpecifier, that.mNetworkSpecifier) && Objects.equals(mTransportInfo, that.mTransportInfo)
            && mSubscriptionIds.equals(that.mSubscriptionIds) && java.util.Arrays.equals(mEnterpriseIds, that.mEnterpriseIds);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTransports, mCapabilities, mLinkUpBandwidthKbps, mLinkDownBandwidthKbps, mSignalStrength, mOwnerUid,
            mNetworkSpecifier, mTransportInfo, mSubscriptionIds) * 31 + java.util.Arrays.hashCode(mEnterpriseIds);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("[");

        int[] transports = getTransportTypes();
        if (transports.length > 0)
        {
            builder.append(" Transports: ");
            for (int index = 0; index < transports.length; index++)
            {
                if (index != 0) builder.append('|');
                builder.append(transportName(transports[index]));
            }
        }

        int[] capabilities = getCapabilities();
        if (capabilities.length > 0)
        {
            builder.append(" Capabilities: ");
            for (int index = 0; index < capabilities.length; index++)
            {
                if (index != 0) builder.append('&');
                builder.append(capabilityName(capabilities[index]));
            }
        }

        if (mLinkUpBandwidthKbps > 0) builder.append(" LinkUpBandwidth>=").append(mLinkUpBandwidthKbps).append("Kbps");
        if (mLinkDownBandwidthKbps > 0) builder.append(" LinkDnBandwidth>=").append(mLinkDownBandwidthKbps).append("Kbps");
        if (mNetworkSpecifier != null) builder.append(" Specifier: <").append(mNetworkSpecifier).append('>');
        if (mTransportInfo != null) builder.append(" TransportInfo: <").append(mTransportInfo).append('>');
        if (mSignalStrength != SIGNAL_STRENGTH_UNSPECIFIED) builder.append(" SignalStrength: ").append(mSignalStrength);
        if (mOwnerUid != -1) builder.append(" OwnerUid: ").append(mOwnerUid);
        if (!mSubscriptionIds.isEmpty()) builder.append(" SubscriptionIds: ").append(mSubscriptionIds);

        return builder.append(']').toString();
    }

    private static String transportName(int transport)
    {
        switch (transport)
        {
            case TRANSPORT_CELLULAR: return "CELLULAR";
            case TRANSPORT_WIFI: return "WIFI";
            case TRANSPORT_BLUETOOTH: return "BLUETOOTH";
            case TRANSPORT_ETHERNET: return "ETHERNET";
            case TRANSPORT_VPN: return "VPN";
            case TRANSPORT_WIFI_AWARE: return "WIFI_AWARE";
            case TRANSPORT_LOWPAN: return "LOWPAN";
            case 7: return "TEST";
            case TRANSPORT_USB: return "USB";
            case TRANSPORT_THREAD: return "THREAD";
            case TRANSPORT_SATELLITE: return "SATELLITE";
            default: return Integer.toString(transport);
        }
    }

    private static String capabilityName(int capability)
    {
        switch (capability)
        {
            case NET_CAPABILITY_MMS: return "MMS";
            case NET_CAPABILITY_SUPL: return "SUPL";
            case NET_CAPABILITY_DUN: return "DUN";
            case NET_CAPABILITY_FOTA: return "FOTA";
            case NET_CAPABILITY_IMS: return "IMS";
            case NET_CAPABILITY_CBS: return "CBS";
            case NET_CAPABILITY_WIFI_P2P: return "WIFI_P2P";
            case NET_CAPABILITY_IA: return "IA";
            case NET_CAPABILITY_RCS: return "RCS";
            case NET_CAPABILITY_XCAP: return "XCAP";
            case NET_CAPABILITY_EIMS: return "EIMS";
            case NET_CAPABILITY_NOT_METERED: return "NOT_METERED";
            case NET_CAPABILITY_INTERNET: return "INTERNET";
            case NET_CAPABILITY_NOT_RESTRICTED: return "NOT_RESTRICTED";
            case NET_CAPABILITY_TRUSTED: return "TRUSTED";
            case NET_CAPABILITY_NOT_VPN: return "NOT_VPN";
            case NET_CAPABILITY_VALIDATED: return "VALIDATED";
            case NET_CAPABILITY_CAPTIVE_PORTAL: return "CAPTIVE_PORTAL";
            case NET_CAPABILITY_NOT_ROAMING: return "NOT_ROAMING";
            case NET_CAPABILITY_FOREGROUND: return "FOREGROUND";
            case NET_CAPABILITY_NOT_CONGESTED: return "NOT_CONGESTED";
            case NET_CAPABILITY_NOT_SUSPENDED: return "NOT_SUSPENDED";
            case NET_CAPABILITY_MCX: return "MCX";
            case NET_CAPABILITY_TEMPORARILY_NOT_METERED: return "TEMPORARILY_NOT_METERED";
            case NET_CAPABILITY_ENTERPRISE: return "ENTERPRISE";
            case NET_CAPABILITY_HEAD_UNIT: return "HEAD_UNIT";
            case NET_CAPABILITY_MMTEL: return "MMTEL";
            case NET_CAPABILITY_PRIORITIZE_LATENCY: return "PRIORITIZE_LATENCY";
            case NET_CAPABILITY_PRIORITIZE_BANDWIDTH: return "PRIORITIZE_BANDWIDTH";
            case NET_CAPABILITY_LOCAL_NETWORK: return "LOCAL_NETWORK";
            case NET_CAPABILITY_NOT_BANDWIDTH_CONSTRAINED: return "NOT_BANDWIDTH_CONSTRAINED";
            default: return Integer.toString(capability);
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mTransports);
        dest.writeLong(mCapabilities);
        dest.writeInt(mLinkUpBandwidthKbps);
        dest.writeInt(mLinkDownBandwidthKbps);
        dest.writeInt(mSignalStrength);
        dest.writeInt(mOwnerUid);

        int[] subscriptionIds = new int[mSubscriptionIds.size()];
        int index = 0;
        for (Integer subscriptionId : mSubscriptionIds) subscriptionIds[index++] = subscriptionId;

        dest.writeIntArray(subscriptionIds);
        dest.writeIntArray(mEnterpriseIds);
    }

    public static final Creator<NetworkCapabilities> CREATOR = new Creator<NetworkCapabilities>()
    {
        @Override
        public NetworkCapabilities createFromParcel(Parcel source)
        {
            NetworkCapabilities capabilities = new NetworkCapabilities();
            capabilities.mTransports = source.readLong();
            capabilities.mCapabilities = source.readLong();
            capabilities.mLinkUpBandwidthKbps = source.readInt();
            capabilities.mLinkDownBandwidthKbps = source.readInt();
            capabilities.mSignalStrength = source.readInt();
            capabilities.mOwnerUid = source.readInt();

            int[] subscriptionIds = source.createIntArray();
            if (subscriptionIds != null)
            {
                for (int subscriptionId : subscriptionIds) capabilities.mSubscriptionIds.add(subscriptionId);
            }

            int[] enterpriseIds = source.createIntArray();
            capabilities.mEnterpriseIds = enterpriseIds == null ? new int[0] : enterpriseIds;

            return capabilities;
        }

        @Override
        public NetworkCapabilities[] newArray(int size)
        {
            return new NetworkCapabilities[size];
        }
    };
}

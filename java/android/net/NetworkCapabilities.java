package android.net;

public final class NetworkCapabilities
{
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI_AWARE = 5;
    public static final int TRANSPORT_LOWPAN = 6;
    public static final int TRANSPORT_USB = 8;

    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;

    private final int mTransports;
    private final int mCapabilities;

    public NetworkCapabilities()
    {
        this(0, 0);
    }

    NetworkCapabilities(int transports, int capabilities)
    {
        mTransports = transports;
        mCapabilities = capabilities;
    }

    public boolean hasTransport(int transport)
    {
        return transport >= 0 && transport < 32 && (mTransports & (1 << transport)) != 0;
    }

    public boolean hasCapability(int capability)
    {
        return capability >= 0 && capability < 32 && (mCapabilities & (1 << capability)) != 0;
    }

    public int getLinkDownstreamBandwidthKbps()
    {
        return 0;
    }

    public int getLinkUpstreamBandwidthKbps()
    {
        return 0;
    }
}
